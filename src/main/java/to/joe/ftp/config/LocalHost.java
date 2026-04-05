package to.joe.ftp.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the local file system which will be scanned...
 */
public class LocalHost implements CommonConfig {
	
	private int scanDelay = 30;
	
	private List<Fetcher> fetchers = new ArrayList<Fetcher>();
	
	public LocalHost() {
		fetchers.add(new Fetcher());
	}

	@Override
	public int getScanDelay() {
		return scanDelay;
	}

	@Override
	public List<Fetcher> getFetchers() {
		return fetchers;
	}

	@Override
	public String getHost() {
		return "local";
	}

}
