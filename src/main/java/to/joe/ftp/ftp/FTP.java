package to.joe.ftp.ftp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import to.joe.ftp.Main;
import to.joe.ftp.config.FTPHost;
import to.joe.ftp.config.Fetcher;

public class FTP extends Thread {
	
	private FTPHost config;
	private FTPClient client = new FTPClient();
	private Connection connection;
	private Logger logger = LogManager.getLogger(FTP.class.getName());
	
	/**
	 * Attempts to return an active {@link FTPClient} ready for use.
	 * @return
	 * @throws IOException 
	 */
	private FTPClient getFTPClient() throws IOException {
		if (client.isConnected()) {
			return client;
		}
		
		logger.info("Establishing FTP connection to {}:{}", config.host, config.port);
		
		if (config.ftps) { // If we're doing FTPS, we'll swap to a FTPS client class instead.
			FTPSClientSSLSessionReuse ftps = new FTPSClientSSLSessionReuse();
			ftps.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
			client = ftps;
		}
		
		int reply;
		
		client.connect(config.host, config.port);
		logger.info("Connected to {}", config.host);
		printLog(client);
		
		reply = client.getReplyCode();
		
		if (!FTPReply.isPositiveCompletion(reply)) {
			client.disconnect();
			logger.error("FTP server refused connection.");
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
		if (!getFTPClient().hasFeature(FTPCmd.MLSD) && !getFTPClient().hasFeature(FTPCmd.MDTM)) {
			logger.warn("Server does not support accurate timestamps (MLSD or MDTM commands), risk of issues at new year's.");
		}
		
		return client;
	}
	
	private void ftpLogoutAndDisconnect() {
		try {
			client.logout();
			logger.info("Logged out of {}", config.host);
		} catch (FTPConnectionClosedException e) {
			// Pass, we are closing the connection anyway.
		} catch (IOException e) {
			logger.error("", e);
		} finally {
			try {
				client.disconnect();
			} catch (IOException e) {
				// Pass
			} finally {
				logger.info("Disconnected from {}", config.host);
			}
		}
	}
	
	private void printLog(FTPClient ftp) {
		String[] log = ftp.getReplyStrings();
		for (String string : log) {
			logger.debug(string);
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
					if (!getFTPClient().hasFeature(FTPCmd.MLSD) && getFTPClient().hasFeature(FTPCmd.MDTM)) {
						timestamp = getFTPClient().mdtmInstant(file.getName());
					} else {
						timestamp = file.getTimestampInstant();
					}
					interestedFiles.add(new QueuedFile(file.getName(), timestamp, file.getSize(), matcher));
				}
			}
		}
		return interestedFiles;
	}
	
	public FTP(FTPHost config) throws SQLException, URISyntaxException, IOException {
		this.config = config;
		
		// Connect to the sqlite database for this ftp host, and create the table if it does not already exist.
		connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s.db", config.host));
		Statement statement = connection.createStatement();
		
		InputStream is = Main.class.getClassLoader().getResourceAsStream("ftp.sql");
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		List<String> queries = br.readAllLines();
		br.close();
		isr.close();
		is.close();
		
		for (String query : queries) {
			statement.executeUpdate(query);
		}
		
		statement.close();
		
		getFTPClient();

	}
	
	@Override
	public void run() {
		try {
			PreparedStatement psUpsert = connection.prepareStatement("INSERT INTO files(host, fetcher, file, size, modified) VALUES(?, ?, ?, ?, ?) ON CONFLICT (host, fetcher, file) DO UPDATE SET size=excluded.size, modified=excluded.modified");
			psUpsert.setString(1, config.host);
			
			PreparedStatement psSelect = connection.prepareStatement("SELECT size, modified FROM files WHERE host = ? AND fetcher = ? AND file = ?");
			psSelect.setString(1, config.host);
			
			while (!isInterrupted()) {
				for (Fetcher fetcher : config.fetchers) {
					fetcher.pendingFiles = new ArrayList<QueuedFile>();
					
					logger.info("Searching directory {} with pattern {}", fetcher.sourcePath, fetcher.sourcePattern);
					
					psSelect.setString(2, fetcher.name);
					
					List<QueuedFile> interestedFiles = getInterestedFiles(fetcher);
					
					for (QueuedFile interestedFile : interestedFiles) {
						psSelect.setString(3, interestedFile.fileName);
						ResultSet rs = psSelect.executeQuery();
						
						if (rs.next()) { // If row exists, we have a record of this file and we compare time stamp/size with what we saw last. If not we assume it's new and queue for download.
							long storedSize = rs.getLong(1);
							Instant storedTime = Instant.parse(rs.getString(2));
							if (interestedFile.timeStamp.equals(storedTime) && interestedFile.fileSize == storedSize) {
								logger.info("Ignorning file {} last modified at {} with size {}", interestedFile.fileName, interestedFile.timeStamp, interestedFile.fileSize);
							} else {
								logger.info("Changed file {} last modified at {} with size {}", interestedFile.fileName, interestedFile.timeStamp, interestedFile.fileSize);
								fetcher.pendingFiles.add(interestedFile);
							}
						} else {
							logger.info("New file {} last modified at {} with size {}", interestedFile.fileName, interestedFile.timeStamp, interestedFile.fileSize);
							fetcher.pendingFiles.add(interestedFile);
						}
						if (isInterrupted()) {
							logger.trace("Interrupt signal received, breaking out of interested file loop 1");
							break;
						}
					}
					if (isInterrupted()) {
						logger.trace("Interrupt signal received, breaking out of interested fetcher loop 1");
						break;
					}
				}
				
				if (!isInterrupted()) {
					try {
						logger.info("Sleeping for 5 seconds before re-checking queued files.");
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						logger.trace("Interrupt signal received, breaking out of sleep 1");
						interrupt();
						break;
					}
				}
				
				if (!isInterrupted()) {
					for (Fetcher fetcher : config.fetchers) {
						psUpsert.setString(2, fetcher.name);
						List<QueuedFile> interestedFiles = getInterestedFiles(fetcher);
						
						for (QueuedFile pendingFile : fetcher.pendingFiles) {
							if (interestedFiles.contains(pendingFile)) {
								logger.info("File {} attributes unchanged after re-check", pendingFile.fileName);
								
								psUpsert.setString(3, pendingFile.fileName);
								psUpsert.setLong(4, pendingFile.fileSize);
								psUpsert.setString(5, pendingFile.timeStamp.toString());
								
								File tempFolder = new File(System.getProperty("java.io.tmpdir"), String.format("%s%s%s%s", "radio-ftp", File.separator, fetcher.name, File.separator));
								tempFolder.mkdirs();
								File tempDestination = new File(tempFolder, pendingFile.fileName);
								FileOutputStream outputStream = new FileOutputStream(tempDestination);
								logger.info("Downloading file to {}", tempDestination.getAbsolutePath());
								getFTPClient().retrieveFile(pendingFile.fileName, outputStream);
								outputStream.close();
								
								File destinationFolder = new File(fetcher.destinationPath);
								if (!destinationFolder.exists()) {
									destinationFolder.mkdirs();
								}
								String destinationName = pendingFile.matcher.replaceFirst(fetcher.destinationPattern);
								File destination = new File(destinationFolder, destinationName);
								
								logger.info("Moving file to {}", destination.getAbsolutePath());
								
								Files.move(tempDestination.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
								
								psUpsert.executeUpdate();
							} else {
								logger.info("File {} attributes changed after re-check, skipping", pendingFile.fileName);
							}
							if (isInterrupted()) {
								logger.trace("Interrupt signal received, breaking out of interested file loop 2");
								break;
							}
						}
						if (isInterrupted()) {
							logger.trace("Interrupt signal received, breaking out of interested fetcher loop 1");
							break;
						}
					}
				}
				
				if (!isInterrupted()) {
					try {
						logger.info("Sleeping for {} seconds", config.scanDelay);
						Thread.sleep(config.scanDelay * 1000);
					} catch (InterruptedException e) {
						logger.trace("Interrupt signal received, breaking out of sleep 2");
						interrupt();
						break;
					}
				}
			}
			connection.close();
		} catch (IOException e) {
			logger.error("", e);
		} catch (SQLException e) {
			logger.error("", e);
		} finally {
			ftpLogoutAndDisconnect();
		}
	}

}
