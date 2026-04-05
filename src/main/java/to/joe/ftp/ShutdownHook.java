package to.joe.ftp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ShutdownHook extends Thread {
	
	private Logger logger = LogManager.getLogger(ShutdownHook.class.getName());
	private Watchdog watchdogThread;
	
	public ShutdownHook(Watchdog watchdogThread) {
		this.watchdogThread = watchdogThread;
		setName("Shutdown Hook");
	}
	
	@Override
	public void run() {
		logger.info("Shutdown signal received");
		watchdogThread.interrupt();
		try {
			watchdogThread.join();
			logger.info("All threads terminated, goodbye");
			LogManager.shutdown();
		} catch (InterruptedException e) {
			// Pass
		}
	}

}
