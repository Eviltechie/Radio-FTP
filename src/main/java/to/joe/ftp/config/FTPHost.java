package to.joe.ftp.config;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a FTP host that will be scanned at an interval for files to download.
 */
public class FTPHost implements CommonConfig {
	
	private String host = "localhost";
	private int port = 21;
	
	private boolean ftps = false;
	
	private String username = "anonymous";
	private String password = "";
	
	private int scanDelay = 30;
	
	private List<Fetcher> fetchers = new ArrayList<Fetcher>();
	
	public FTPHost() {
		fetchers.add(new Fetcher());
	}
	
	@Override
	public String getHost() {
		return host;
	}
	
	public int getPort() {
		return port;
	}
	
	public boolean getFTPS() {
		return ftps;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}

	@Override
	public int getScanDelay() {
		return scanDelay;
	}

	@Override
	public List<Fetcher> getFetchers() {
		return fetchers;
	}

}
