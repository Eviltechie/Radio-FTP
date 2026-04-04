package to.joe.ftp.config;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a FTP host that will be scanned at an interval for files to download.
 */
public class FTPHost {
	
	public String host = "localhost";
	public int port = 21;
	
	public boolean ftps = false;
	
	public String username = "anonymous";
	public String password = "";
	
	/**
	 * Re-scan delay, in seconds.
	 */
	public int scanDelay = 30;
	
	/**
	 * Action to perform. A "copy" will download the files only, while "move" will attempt to delete them from the source afterwards.
	 */
	public String action = "copy"; // TODO Implement this
	
	/**
	 * A list of fetchers associated with this FTP host.
	 */
	public List<Fetcher> fetchers = new ArrayList<Fetcher>();
	
	/**
	 * Constructor creates default configuration when not being created through deserialization.
	 */
	public FTPHost() {
		fetchers.add(new Fetcher());
	}

}
