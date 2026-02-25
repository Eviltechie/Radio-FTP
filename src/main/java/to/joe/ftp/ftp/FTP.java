package to.joe.ftp.ftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPCmd;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;

import to.joe.ftp.Main;
import to.joe.ftp.config.FTPHost;
import to.joe.ftp.config.Fetcher;

public class FTP extends Thread {
	
	private FTPHost config;
	private FTPClient client = new FTPClient(); // FIXME Need to try FTPSClient too...
	private Connection connection;
	
	/**
	 * Attempts to return an active {@link FTPClient} ready for use.
	 * @return
	 */
	private FTPClient getFTPClient() {
		if (client.isConnected()) {
			return client;
		}
		
		try {
			if (config.ftps) {
				FTPSClientSSLSessionReuse ftps = new FTPSClientSSLSessionReuse();
				ftps.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
				client = ftps;
			}
			
			int reply;
			
			client.connect(config.host, config.port);
			System.out.println(String.format("Connected to %s:%s", config.host, config.port));
			printLog(client);
			
			reply = client.getReplyCode();
			
			if (!FTPReply.isPositiveCompletion(reply)) {
				client.disconnect();
				System.err.println("FTP server refused connection.");
			}
			
			client.login(config.username, config.password);
			if (client instanceof FTPSClient) {
				FTPSClient ftps = (FTPSClient) client;
				ftps.execPBSZ(0);
				ftps.execPROT("P");
			}
			client.enterLocalPassiveMode();
			client.features();
			printLog(client);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return client;
	}
	
	private void closeFTPClient() {
		try {
			client.logout();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} finally {
			if (client.isConnected()) {
				try {
					client.disconnect();
				} catch (IOException e) {
					// Pass
				}
			}
		}
	}
	
	private void printLog(FTPClient ftp) {
		String[] log = ftp.getReplyStrings();
		for (String string : log) {
			System.out.println(string);
		}
	}
	
	/**
	 * Gets files on the server which match our selected source pattern.
	 * @param fetcher
	 * @throws IOException
	 */
	private List<QueuedFile> getInterestedFiles(Fetcher fetcher) throws IOException {
		List<QueuedFile> interestedFiles = new ArrayList<QueuedFile>();
		
		getFTPClient().changeWorkingDirectory(fetcher.sourcePath);
		
		FTPFile[] files;
		if (getFTPClient().hasFeature(FTPCmd.MLSD)) { // If possible, we try to use MLSD to list the directory. If not, we fall back to regular LIST.
			files = getFTPClient().mlistDir();
		} else {
			files = getFTPClient().listFiles();
		}
		
		Pattern pattern = Pattern.compile(fetcher.sourcePattern);
		
		for (FTPFile file : files) {
			if (file.isFile()) {
				Matcher matcher = pattern.matcher(file.getName());
				if (matcher.matches()) {
					Instant timestamp;
					if (!getFTPClient().hasFeature(FTPCmd.MLSD) && getFTPClient().hasFeature(FTPCmd.MDTM)) { // TODO Warn user if server doesn't support MDTM or another way to get proper timestamp
						timestamp = getFTPClient().mdtmInstant(file.getName());
					} else {
						timestamp = file.getTimestampInstant();
					}
					interestedFiles.add(new QueuedFile(file.getName(), timestamp, file.getSize()));
				}
			}
		}
		return interestedFiles;
	}
	
	public FTP(FTPHost config) {
		this.config = config;
		
		// Connect to the sqlite database for this ftp host, and create the table if it does not already exist.
		try {
			connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s.db", config.host));
			Statement statement = connection.createStatement();
			
			Path resourcePath = Paths.get(Main.class.getClassLoader().getResource("ftp.sql").toURI());
			List<String> queries = Files.readAllLines(resourcePath);
			
			for (String query : queries) {
				statement.executeUpdate(query);
			}
			
			statement.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	@Override
	public void run() { // TODO More checking for interrupted.
		try {
			PreparedStatement psUpsert = connection.prepareStatement("INSERT INTO files(host, fetcher, file, size, modified) VALUES(?, ?, ?, ?, ?) ON CONFLICT (host, fetcher, file) DO UPDATE SET size=excluded.size, modified=excluded.modified");
			psUpsert.setString(1, config.host);
			
			PreparedStatement psSelect = connection.prepareStatement("SELECT size, modified FROM files WHERE host = ? AND fetcher = ? AND file = ?");
			psSelect.setString(1, config.host);
			
			while (!interrupted()) {
				for (Fetcher fetcher : config.fetchers) {
					fetcher.pendingFiles = new ArrayList<QueuedFile>();
					
					System.out.println(String.format("Directory: %s Pattern: %s", fetcher.sourcePath, fetcher.sourcePattern));
					
					psSelect.setString(2, fetcher.name);
					
					List<QueuedFile> interestedFiles = getInterestedFiles(fetcher);
					
					for (QueuedFile interestedFile : interestedFiles) {
						psSelect.setString(3, interestedFile.fileName);
						ResultSet rs = psSelect.executeQuery();
						
						System.out.print(interestedFile.timeStamp + " " + interestedFile.fileName + " " + interestedFile.fileSize + " ");
						
						if (rs.next()) { // If row exists, we have a record of this file and we compare time stamp/size with what we saw last. If not we assume it's new and queue for download.
							long storedSize = rs.getLong(1);
							Instant storedTime = Instant.parse(rs.getString(2));
							if (interestedFile.timeStamp.equals(storedTime) && interestedFile.fileSize == storedSize) {
								System.out.println("Same");
							} else {
								System.out.println("Changed!");
								fetcher.pendingFiles.add(interestedFile);
							}
						} else {
							System.out.println("New!");
							fetcher.pendingFiles.add(interestedFile);
						}
					}
					System.out.println();
				}
				
				try {
					System.out.println("Sleeping for 5 seconds before re-checking queued files.");
					Thread.sleep(10000); // FIXME
				} catch (InterruptedException e) {
					break;
				}
				
				for (Fetcher fetcher : config.fetchers) {
					psUpsert.setString(2, fetcher.name);
					List<QueuedFile> interestedFiles = getInterestedFiles(fetcher);
					
					for (QueuedFile file : fetcher.pendingFiles) {
						if (interestedFiles.contains(file)) {
							System.out.println("Matched " + file.fileName);
							
							psUpsert.setString(3, file.fileName);
							psUpsert.setLong(4, file.fileSize);
							psUpsert.setString(5, file.timeStamp.toString());
							
							File f = new File(System.getProperty("java.io.tmpdir"), String.format("%s%s%s%s", "radio-ftp", File.separator, fetcher.name, File.separator));
							f.mkdirs();
							f = new File(f, file.fileName);
							FileOutputStream outputStream = new FileOutputStream(f);
							getFTPClient().retrieveFile(file.fileName, outputStream); // TODO Catch exception here
							outputStream.close();
							
							psUpsert.executeUpdate();
						}
					}
				}
				
				try {
					System.out.println(String.format("Sleeping for %s seconds", config.scanDelay));
					Thread.sleep(config.scanDelay * 1000);
				} catch (InterruptedException e) {
					// Pass
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		closeFTPClient();
		// TODO connection.close() ?
	}

}
