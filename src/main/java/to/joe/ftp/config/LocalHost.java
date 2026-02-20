package to.joe.ftp.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the local file system which will be scanned...
 */
public class LocalHost {
	
	public String action = "copy"; // Copy or move.
	
	public List<Fetcher> fetchers = new ArrayList<Fetcher>();
	
	public LocalHost() {
		fetchers.add(new Fetcher());
	}

}
