package to.joe.ftp.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the root of the program configuration. Serialized to JSON.
 */
public class Config {
	
	// TODO Add option for working directory other than %TEMP%
	public List<FTPHost> ftpHosts = new ArrayList<FTPHost>();
	public LocalHost localHost = new LocalHost();
	
	/**
	 * Constructor creates default configuration when not being created through deserialization.
	 */
	public Config() {
		ftpHosts.add(new FTPHost());
	}

}
