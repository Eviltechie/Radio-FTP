package to.joe.ftp.config;

import java.util.List;

public interface CommonConfig {
	
	/**
	 * Re-scan delay, in seconds.
	 * @return
	 */
	public int getScanDelay();
	
	/**
	 * A list of {@link Fetcher}s associated with this host.
	 * @return
	 */
	public List<Fetcher> getFetchers();
	
	/**
	 * Returns the "host" for this config. This can be an actual hostname/IP, or another unique designator.
	 * @return
	 */
	public String getHost();

}
