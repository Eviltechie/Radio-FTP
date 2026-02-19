package to.joe.ftp.config;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a FTP host that will be scanned at an interval for files to download.
 */
public class FTPHost {
	
	public String host = "localhost";
	public int port = 21;
	
	/**
	 * Re-scan delay, in seconds.
	 */
	public int scanDelay = 30;
	
	public List<Fetcher> fetchers = new ArrayList<Fetcher>();
	
	public FTPHost() {
		fetchers.add(new Fetcher());
	}

}
