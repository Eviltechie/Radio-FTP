package to.joe.ftp;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import to.joe.ftp.config.Config;
import to.joe.ftp.config.FTPHost;
import to.joe.ftp.ftp.FTP;
import to.joe.ftp.local.Local;

public class Watchdog extends Thread {
	
	private Logger logger = LogManager.getLogger(FTP.class.getName());
	private Config config;
	private Map<FTPHost, FTP> ftpThreads = new HashMap<FTPHost, FTP>();
	private Local localThread = null;
	
	public Watchdog(Config config) {
		setName("Watchdog");
		this.config = config;
	}
	
	@Override
	public void run() {
		if (config.ftpHosts.size() > 0) { // Start any FTP threads for the first time
			logger.info("Starting FTP connections");
		} else {
			logger.info("No FTP configurations present");
		}
		for (FTPHost ftpHost : config.ftpHosts) {
			try {
				FTP ftpThread = new FTP(ftpHost);
				ftpThreads.put(ftpHost, ftpThread);
				ftpThread.setName(ftpHost.host);
				ftpThread.start();
			} catch (Exception e) {
				logger.error("Error starting FTP thread", e);
			}
		}
		
		try { // Start local thread for the first time
			if (config.localHost.fetchers.size() > 0) {
				logger.info("Starting local connection");
				localThread = new Local(config.localHost);
				localThread.setName("local");
				localThread.start();
			} else {
				logger.info("No local configuration present");
			}
		} catch (Exception e) {
			logger.error("Error starting local thread", e);
		}
		
		while (!isInterrupted()) { // Every 30 seconds, loop over all threads and make sure they are running, re-starting if needed.
			for (Map.Entry<FTPHost, FTP> entry : ftpThreads.entrySet()) {
				if (!entry.getValue().isAlive()) {
					logger.warn("FTP thread {} terminated, attempting to re-establish FTP connection", entry.getValue().getName());
					try {
						FTP ftpThread = new FTP(entry.getKey());
						ftpThread.setName(entry.getKey().host);
						ftpThread.start();
						entry.setValue(ftpThread);
					} catch (Exception e) {
						logger.error("Error when re-establishing FTP connection", e);
					}
				}
			}
			
			if (localThread != null && !localThread.isAlive()) {
				logger.warn("Local thread terminated, attempting to restart");
				try {
					localThread = new Local(config.localHost);
					localThread.setName("local");
					localThread.start();
				} catch (Exception e) {
					logger.error("Error restarting local thread", e);
				}
			}
			
			try {
				Thread.sleep(Duration.ofSeconds(30));
			} catch (InterruptedException e) {
				logger.info("Watchdog interrupted, beginning shutdown sequence");
			}
		}
		
		for (FTP ftpThread : ftpThreads.values()) { // When interrupted, interrupt all other threads.
			logger.info("Stopping {} FTP thread", ftpThread.getName());
			ftpThread.interrupt();
			try {
				ftpThread.join();
				logger.info("Stopped {} FTP thread", ftpThread.getName());
			} catch (InterruptedException e) {
				// Pass
			}
		}
		
		if (localThread != null) {
			localThread.interrupt();
			logger.info("Stopping local thread");
			try {
				localThread.join();
				logger.info("Stopped local thread");
			} catch (InterruptedException e) {
				// Pass
			}
		}
		
		logger.info("All download threads stopped");
	}

}
