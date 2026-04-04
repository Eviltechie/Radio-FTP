package to.joe.ftp.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import to.joe.ftp.Main;
import to.joe.ftp.config.Fetcher;
import to.joe.ftp.config.LocalHost;
import to.joe.ftp.ftp.QueuedFile;

public class Local extends Thread {
	
	private LocalHost config;
	private Logger logger = LogManager.getLogger(Local.class.getName());
	private Connection connection;
	
	public List<QueuedFile> getInterestedFiles(Fetcher fetcher) throws IOException {
		List<QueuedFile> interestedFiles = new ArrayList<QueuedFile>();
		
		Set<Path> files = new HashSet<Path>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(fetcher.sourcePath))) {
			for (Path path : stream) {
				if (!Files.isDirectory(path)) {
					files.add(path);
				}
			}
		} catch (IOException e) {
			logger.error("", e);
		}
		
		Pattern pattern = Pattern.compile(fetcher.sourcePattern);
		
		for (Path path : files) {
			Matcher matcher = pattern.matcher(path.getFileName().toString());
			if (matcher.matches()) {
				interestedFiles.add(new QueuedFile(path.getFileName().toString(), Files.getLastModifiedTime(path).toInstant(), Files.size(path), matcher));
			}
		}
		
		return interestedFiles;
	}
	
	public Local(LocalHost config) throws SQLException, IOException {
		this.config = config;
		
		// Connect to the sqlite database for this ftp host, and create the table if it does not already exist.
		connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s.db", "local"));
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
	}
	
	@Override
	public void run() {
		try {
			PreparedStatement psUpsert = connection.prepareStatement("INSERT INTO files(host, fetcher, file, size, modified) VALUES(?, ?, ?, ?, ?) ON CONFLICT (host, fetcher, file) DO UPDATE SET size=excluded.size, modified=excluded.modified");
			psUpsert.setString(1, "local");
			
			PreparedStatement psSelect = connection.prepareStatement("SELECT size, modified FROM files WHERE host = ? AND fetcher = ? AND file = ?");
			psSelect.setString(1, "local");
			
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
								logger.info("Copying file to {}", tempDestination.getAbsolutePath());
								Files.copy(Paths.get(fetcher.sourcePath, pendingFile.fileName), outputStream); // TODO Catch exception here // TODO Calculate data rate here
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
		}
	}

}
