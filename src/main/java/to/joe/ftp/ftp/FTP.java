package to.joe.ftp.ftp;

import java.io.IOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import to.joe.ftp.Main;
import to.joe.ftp.config.FTPHost;
import to.joe.ftp.config.Fetcher;

public class FTP extends Thread {
	
	private FTPHost config;
	private FTPClient client = new FTPClient();
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
			client.enterLocalPassiveMode();
			printLog(client);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
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
			e.printStackTrace();
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
	public void run() {
		try {
			PreparedStatement ps = connection.prepareStatement("INSERT INTO files(host, fetcher, file, size, modified) VALUES(?, ?, ?, ?, ?) ON CONFLICT (host, fetcher, file) DO UPDATE SET size=excluded.size, modified=excluded.modified");
			ps.setString(1, config.host);
			while (!interrupted()) {
				for (Fetcher fetcher : config.fetchers) {
					getFTPClient().changeWorkingDirectory(fetcher.sourcePath);
					FTPFile[] files = getFTPClient().listFiles();
					System.out.println(String.format("Directory: %s", fetcher.sourcePath));
					ps.setString(2, fetcher.sourcePath);
					for (FTPFile file : files) {
						if (file.isFile()) {
							ps.setString(3, file.getName());
							ps.setLong(4, file.getSize());
							ps.setString(5, file.getTimestampInstant().toString());
							System.out.println(file.getTimestampInstant() + " " + file.getName() + " " + file.getSize());
							ps.executeUpdate();
						}
					}
					System.out.println();
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
	}

}
