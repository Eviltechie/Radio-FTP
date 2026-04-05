package to.joe.ftp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import to.joe.ftp.config.CommonConfig;
import to.joe.ftp.config.Fetcher;
import to.joe.ftp.ftp.QueuedFile;

public abstract class DownloadThread extends Thread {
	
	private Logger logger = LogManager.getLogger(DownloadThread.class.getName());
	
	private CommonConfig config;
	
	private Connection connection;
	protected PreparedStatement psUpsert;
	protected PreparedStatement psSelect;
	
	public DownloadThread(CommonConfig config) throws SQLException, IOException {
		this.config = config;
		
		setupSQLiteConnection(config.getHost());
	}
	
	/**
	 * Returns the {@link CommonConfig} associated with this thread.
	 * @return
	 */
	protected CommonConfig getConfig() {
		return config;
	}
	
	/**
	 * Creates the SQLite connection, initializes the database (if necessary), and sets up the upsert and select prepared statements.
	 * @param host Used for the database name as well as the host column in the table.
	 * @throws SQLException
	 * @throws IOException
	 */
	private void setupSQLiteConnection(String host) throws SQLException, IOException {
		connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s.db", host));
		Statement statement = connection.createStatement();
		
		// Read the ftp.sql file from the jar file and execute it. 
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
		
		// Setup the upsert and select prepared statements.
		psUpsert = connection.prepareStatement("INSERT INTO files(host, fetcher, file, size, modified) VALUES(?, ?, ?, ?, ?) ON CONFLICT (host, fetcher, file) DO UPDATE SET size=excluded.size, modified=excluded.modified");
		psUpsert.setString(1, host);
		
		psSelect = connection.prepareStatement("SELECT size, modified FROM files WHERE host = ? AND fetcher = ? AND file = ?");
		psSelect.setString(1, host);
	}
	
	/**
	 * Get a {@link List} of {@link QueuedFile} where the file name matches the source pattern in the provided fetcher.
	 * @param fetcher
	 * @return
	 */
	protected abstract List<QueuedFile> getInterestedFiles(Fetcher fetcher) throws Exception;
	
	/**
	 * Iterates through each fetcher, getting files which match the source pattern, and then checks to see if we need to queue them for download.
	 * @throws Exception 
	 */
	private void firstCheck() throws Exception {
		for (Fetcher fetcher : getConfig().getFetchers()) { // We loop through each fetcher in turn.
			fetcher.pendingFiles = new ArrayList<QueuedFile>(); // Each fetcher has a transient variable for storing interested files. This lets us compare when we re-check in 5 seconds.
			
			logger.info("Searching directory {} with pattern {}", fetcher.getSourcePath(), fetcher.getSourcePattern());
			
			psSelect.setString(2, fetcher.getName()); // In a moment we will check to see if we have a record of this file. If we do and all the info is the same we can skip processing it.
			
			List<QueuedFile> interestedFiles = getInterestedFiles(fetcher);
			
			/**
			 * Loop through the interested files and check for a previous record of them.
			 * If no record, it's new and we'll queue it up.
			 * If a record exists, we will check last modified timestamp and size. If either is different we'll queue it up.
			 */
			for (QueuedFile interestedFile : interestedFiles) {
				psSelect.setString(3, interestedFile.fileName);
				ResultSet rs = psSelect.executeQuery();
				
				if (rs.next()) { // If a row exists, we have a record and we'll check timestamp/size.
					long storedSize = rs.getLong(1);
					Instant storedTime = Instant.parse(rs.getString(2));
					if (interestedFile.timeStamp.equals(storedTime) && interestedFile.fileSize == storedSize) { // All attributes are the same, ignore.
						logger.info("Ignorning file {} last modified at {} with size {}", interestedFile.fileName, interestedFile.timeStamp, interestedFile.fileSize);
					} else { // Something changed, queue it up.
						logger.info("Changed file {} last modified at {} with size {}", interestedFile.fileName, interestedFile.timeStamp, interestedFile.fileSize);
						fetcher.pendingFiles.add(interestedFile);
					}
				} else { // Otherwise it's new.
					logger.info("New file {} last modified at {} with size {}", interestedFile.fileName, interestedFile.timeStamp, interestedFile.fileSize);
					fetcher.pendingFiles.add(interestedFile);
				}
				
				if (isInterrupted()) {
					logger.trace("Interrupt signal received, returning first check of interested files");
					return;
				}
			}
		}
		
		if (isInterrupted()) {
			logger.trace("Interrupt signal received, returning from first check of fetchers");
			return;
		}
	}
	
	private void secondCheck() throws Exception {
		for (Fetcher fetcher : getConfig().getFetchers()) { // We loop through each fetcher in turn.
			psUpsert.setString(2, fetcher.getName());
			
			List<QueuedFile> interestedFiles = getInterestedFiles(fetcher); // It's now five seconds later, so we get interested files again to see if they have changed. (Indicating they are currently being written to.)
			
			for (QueuedFile pendingFile : fetcher.pendingFiles) { // We now loop through the files 
				if (interestedFiles.contains(pendingFile)) { // If our most recent list of files contains the previous file we are looking at, then we will try to process it.
					logger.info("File {} attributes unchanged after re-check, attempting to process", pendingFile.fileName);
					
					psUpsert.setString(3, pendingFile.fileName);
					psUpsert.setLong(4, pendingFile.fileSize);
					psUpsert.setString(5, pendingFile.timeStamp.toString());
					
					File tempFolder = new File(System.getProperty("java.io.tmpdir"), String.format("%s%s%s%s", "radio-ftp", File.separator, fetcher.getName(), File.separator)); // Create a temp folder to download the file to.
					if (!tempFolder.exists()) {
						tempFolder.mkdirs();
					}
					
					File tempFile = new File(tempFolder, pendingFile.fileName); // Create a destination file to download to.
					
					if (fetcher.isWetRun()) {
						logger.info("Wet run enabled, skipping processing for {}", pendingFile.fileName);
					} else {
						downloadFile(fetcher, pendingFile, tempFile); // Pass off the downloading to an abstract method for the implementation specific download.
						
						File destinationFolder = new File(fetcher.getDestinationPath()); // Create our destination folder if it doesn't exist.
						if (!destinationFolder.exists()) {
							destinationFolder.mkdirs();
						}
						
						String destinationName = pendingFile.matcher.replaceFirst(fetcher.getDestinationPattern());
						File destination = new File(destinationFolder, destinationName); // Create a destination file with the final file name with the regex from the fetcher.
						
						logger.info("Moving file from temp directory to {}", destination.getAbsolutePath());
						
						Files.move(tempFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING); // Move temp file, replacing existing file if needed.
					}
					
					if (fetcher.getAction().equalsIgnoreCase("move")) {
						deleteSourceFile(fetcher, pendingFile);
					}
					
					psUpsert.executeUpdate(); // After all other items succeed, we can run the upsert to record the file as processed.
				} else {
					logger.info("File {} attributes changed or file missing after re-check, skipping", pendingFile.fileName);
				}
				
				if (isInterrupted()) {
					logger.trace("Interrupt signal received, returning second check of interested files");
					return;
				}
			}
			
			if (isInterrupted()) {
				logger.trace("Interrupt signal received, returning from second check of fetchers");
				return;
			}
		}
	}
	
	protected abstract void downloadFile(Fetcher fetcher, QueuedFile pendingFile, File tempFile) throws Exception;
	
	protected abstract void deleteSourceFile(Fetcher fetcher, QueuedFile pendingFile) throws Exception;
	
	/**
	 * Cleanup method. Runs in a finally block at the end of the thread loop.
	 */
	protected abstract void cleanup();
	
	@Override
	public void run() {
		try {
			while (!isInterrupted()) {
				
				if (!isInterrupted()) {
					firstCheck();
				}
				
				if (!isInterrupted()) {
					try {
						logger.info("Sleeping for 5 seconds before re-checking queued files.");
						Thread.sleep(Duration.ofSeconds(5));
					} catch (InterruptedException e) {
						logger.trace("Interrupt signal received, breaking out of short sleep");
						interrupt();
						break;
					}
				}
				
				if (!isInterrupted()) {
					secondCheck();
				}
				
				if (!isInterrupted()) {
					try {
						logger.info("Sleeping for {} seconds", getConfig().getScanDelay());
						Thread.sleep(Duration.ofSeconds(getConfig().getScanDelay()));
					} catch (InterruptedException e) {
						logger.trace("Interrupt signal received, breaking out of long sleep");
						interrupt();
						break;
					}
				}
			}
		} catch (Exception e) {
			logger.error("", e);
		} finally {
			cleanup();
		}
	}

}
