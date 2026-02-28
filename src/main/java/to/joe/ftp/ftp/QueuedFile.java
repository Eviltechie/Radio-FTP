package to.joe.ftp.ftp;

import java.time.Instant;
import java.util.regex.Matcher;

import to.joe.ftp.config.Fetcher;

/**
 * Represents a file that is or may be queued for download.
 */
public class QueuedFile {
	
	/**
	 * File name, as provided by the FTP server.
	 */
	public final String fileName;
	/**
	 * Last modified timestamp, as {@link Instant}, as provided by the FTP server.
	 */
	public final Instant timeStamp;
	/**
	 * File size in bytes, as provided by the FTP server.
	 */
	public final long fileSize;
	/**
	 * We store the {@link Fetcher#sourcePattern} so that we can use capture groups later on.
	 */
	public final Matcher matcher;
	
	public QueuedFile(String fileName, Instant timeStamp, long fileSize, Matcher matcher) {
		this.fileName = fileName;
		this.timeStamp = timeStamp;
		this.fileSize = fileSize;
		this.matcher = matcher;
	}
	
	/**
	 * We override equals since we only want to compare the name, timestamp, and size, while ignoring the matcher.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof QueuedFile) {
			QueuedFile other = (QueuedFile) obj;
			if (fileName.equals(other.fileName) && timeStamp.equals(other.timeStamp) && fileSize == other.fileSize) {
				return true;
			}
		}
		return super.equals(obj);
	}
	
	/**
	 * Returns the name, timestamp, and size for convenience.
	 */
	@Override
	public String toString() {
		return String.format("%s %s %s", timeStamp, fileName, fileSize);
	}

}
