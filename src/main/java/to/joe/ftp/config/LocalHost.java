package to.joe.ftp.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the local file system which will be scanned...
 */
public class LocalHost implements CommonConfig { // TODO Set all fields to private
	
	public int scanDelay = 30;
	
	public String action = "copy";
	
	private boolean wetRun = false;
	
	public List<Fetcher> fetchers = new ArrayList<Fetcher>();
	
	public LocalHost() {
		fetchers.add(new Fetcher());
	}

	@Override
	public int getScanDelay() {
		return scanDelay;
	}

	@Override
	public String getAction() {
		return action;
	}

	@Override
	public boolean wetRun() {
		return wetRun;
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
