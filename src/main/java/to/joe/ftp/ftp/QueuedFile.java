package to.joe.ftp.ftp;

import java.time.Instant;
import java.util.regex.Matcher;

public class QueuedFile {
	
	public String fileName;
	public Instant timeStamp;
	public long fileSize;
	public Matcher matcher;
	
	public QueuedFile(String fileName, Instant timeStamp, long fileSize, Matcher matcher) {
		this.fileName = fileName;
		this.timeStamp = timeStamp;
		this.fileSize = fileSize;
		this.matcher = matcher;
	}
	
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
	
	@Override
	public String toString() {
		return String.format("%s %s %s", timeStamp, fileName, fileSize);
	}

}
