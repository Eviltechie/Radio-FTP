package to.joe.ftp.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the root of the program configuration. Serialized to JSON.
 */
public class Config {
	
	private List<FTPHost> ftpHosts = new ArrayList<FTPHost>();
	private LocalHost localHost = new LocalHost();
	
	public List<FTPHost> getFTPHosts() {
		return ftpHosts;
	}
	
	public LocalHost getLocalHost() {
		return localHost;
	}
	
	/**
	 * Constructor creates default configuration when not being created through deserialization.
	 */
	public Config() {
		ftpHosts.add(new FTPHost());
	}

}
