package to.joe.ftp.config;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a FTP host that will be scanned at an interval for files to download.
 */
public class FTPHost implements CommonConfig { // TODO Set all fields to private
	
	public String host = "localhost";
	public int port = 21;
	
	public boolean ftps = false;
	
	public String username = "anonymous";
	public String password = "";
	
	public int scanDelay = 30;
	
	public String action = "copy";
	
	private boolean wetRun = false;
	
	public List<Fetcher> fetchers = new ArrayList<Fetcher>();
	
	public FTPHost() {
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
		return host;
	}

}
