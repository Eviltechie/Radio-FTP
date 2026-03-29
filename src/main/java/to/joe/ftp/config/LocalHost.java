package to.joe.ftp.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the local file system which will be scanned...
 */
public class LocalHost {
	
	/**
	 * Re-scan delay, in seconds.
	 */
	public int scanDelay = 30;
	
	/**
	 * Action to perform. A "copy" will download the files only, while "move" will attempt to delete them from the source afterwards.
	 */
	public String action = "copy";
	
	/**
	 * A list of fetchers associated with this local host.
	 */
	public List<Fetcher> fetchers = new ArrayList<Fetcher>();
	
	/**
	 * Constructor creates default configuration when not being created through deserialization.
	 */
	public LocalHost() {
		fetchers.add(new Fetcher());
	}

}
