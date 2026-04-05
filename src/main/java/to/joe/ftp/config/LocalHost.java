package to.joe.ftp.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the local file system which will be scanned...
 */
public class LocalHost implements CommonConfig { // TODO Set all fields to private
	
	public int scanDelay = 30;
	
	public String action = "copy";
	
	public List<Fetcher> fetchers = new ArrayList<Fetcher>();
	
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
