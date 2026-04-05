package to.joe.ftp.config;

import java.util.List;

import to.joe.ftp.ftp.QueuedFile;

/**
 * Represents a "job" of files to download.
 */
public class Fetcher {
	
	private String name = "Job Name";
	
	private String action = "copy";
	
	private boolean wetRun = false;
	
	private String sourcePath = "/";
	private String destinationPath = "/";
	
	private String sourcePattern = ".+\\.wav$";
	private String destinationPattern = "$0";
	
	/**
	 * Name used to identify this fetcher in the database.
	 * Important in case two fetchers have the same source directory.
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Action to perform. A "copy" will download the files only, while "move" will attempt to delete them from the source afterwards.
	 * @return
	 */
	public String getAction() {
		return action;
	}
	
	/**
	 * If <code>true</code>:<br>
	 * When set to copy, files which match will be marked as processed, but will not actually be copied to the destination.<br>
	 * When set to move, files which match will be marked as processed and deleted from the source, but will not actually be moved to the destination.<br><br>
	 * This is intended to be used for a "first run" where you may not want old or existing files to be re-downloaded.<br>
	 * @return
	 */
	public boolean isWetRun() {
		return wetRun;
	}
	
	/**
	 * Files will be downloaded from this directory on the remote site.
	 * @return
	 */
	public String getSourcePath() {
		return sourcePath;
	}
	
	/**
	 * Files will be placed in this directory on the local site.
	 * @return
	 */
	public String getDestinationPath() {
		return destinationPath;
	}
	
	/**
	 * Files matching this regular expression pattern will be processed.
	 * @return
	 */
	public String getSourcePattern() {
		return sourcePattern;
	}
	
	/**
	 * Downloaded files will be renamed using this regex replacement pattern.
	 * @return
	 */
	public String getDestinationPattern() {
		return destinationPattern;
	}
	
	/**
	 * List of pending files to be re-checked before transferring.
	 * TODO This should probably be a Map in the download thread...
	 */
	public transient List<QueuedFile> pendingFiles;

}
