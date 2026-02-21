package to.joe.ftp.config;

/**
 * Represents a "job" of files to download.
 */
public class Fetcher {
	
	/**
	 * Name used to identify this fetcher in the database.
	 * Important in case two fetchers have the same source directory. 
	 */
	public String name = "Job Name";
	
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
	public String sourcePattern = ".+\\.wav$";
	/**
	 * Downloaded files will be renamed using this pattern.
	 */
	public String destinationPattern = "$&";

}
