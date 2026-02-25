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
	
	public String action = "copy"; // Copy or move.
	
	public List<Fetcher> fetchers = new ArrayList<Fetcher>();
	
	public FTPHost() {
		fetchers.add(new Fetcher());
	}

}
