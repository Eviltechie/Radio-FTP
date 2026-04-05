package to.joe.ftp.config;

import java.util.List;

public interface CommonConfig {
	
	/**
	 * Re-scan delay, in seconds.
	 * @return
	 */
	public int getScanDelay();
	
	/**
	 * Action to perform. A "copy" will download the files only, while "move" will attempt to delete them from the source afterwards.
	 */
	public String getAction();
	
	/**
	 * If <code>true</code>:<br>
	 * When set to copy, files which match will be marked as processed, but will not actually be copied to the destination.<br>
	 * When set to move, files which match will be marked as processed and deleted from the source, but will not actually be moved to the destination.<br><br>
	 * This is intended to be used for a "first run" where you may not want old or existing files to be re-downloaded.<br>
	 * @return
	 */
	public boolean wetRun(); // TODO Implement this
	
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
