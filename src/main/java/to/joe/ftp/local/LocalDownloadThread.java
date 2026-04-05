package to.joe.ftp.local;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import to.joe.ftp.DownloadThread;
import to.joe.ftp.config.Fetcher;
import to.joe.ftp.config.LocalHost;
import to.joe.ftp.ftp.QueuedFile;

public class LocalDownloadThread extends DownloadThread {
	
	private Logger logger = LogManager.getLogger(LocalDownloadThread.class.getName());
	
	public LocalDownloadThread(LocalHost config) throws SQLException, IOException {
		super(config);
	}
	
	@Override
	protected LocalHost getConfig() {
		return (LocalHost) super.getConfig();
	}

	@Override
	protected List<QueuedFile> getInterestedFiles(Fetcher fetcher) throws IOException {
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

	@Override
	protected void downloadFile(Fetcher fetcher, QueuedFile pendingFile, File tempFile) throws IOException {
		FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
		
		Instant start = Instant.now();
		
		logger.info("Copying file to {}", tempFile.getAbsolutePath());
		Files.copy(Paths.get(fetcher.sourcePath, pendingFile.fileName), fileOutputStream);
		
		Instant end = Instant.now();
		Duration difference = Duration.between(start, end);
		
		fileOutputStream.close();
		
		logger.info("Copied {} bytes in {} seconds", pendingFile.fileSize, difference.toSeconds());
	}

	@Override
	protected void deleteSourceFile(Fetcher fetcher, QueuedFile pendingFile) throws IOException {
		Files.delete(Paths.get(fetcher.sourcePath, pendingFile.fileName));
		
		logger.info("Deleted {} from source folder", pendingFile.fileName);
	}

	@Override
	protected void cleanup() {
		// Pass
	}

}
