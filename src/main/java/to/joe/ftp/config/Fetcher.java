package to.joe.ftp.config;

/**
 * Represents a "job" of files to download.
 */
public class Fetcher {
	
	/**
	 * Files will be downloaded from this directory on the remote site.
	 */
	public String sourcePath = "/";
	/**
	 * Files will be placed in this directory on the local site.
	 */
	public String destinationPath = "/";
	
	/**
	 * Files matching this regular expression pattern will be downloaded.
	 */
	public String sourcePattern = "(\\d{5})\\.wav$";
	/**
	 * Downloaded files will be renamed using this pattern.
	 */
	public String destinationPattern = "SH$1.wav";

}
