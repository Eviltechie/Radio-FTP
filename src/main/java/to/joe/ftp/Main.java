package to.joe.ftp;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import to.joe.ftp.config.Config;
import to.joe.ftp.config.FTPHost;
import to.joe.ftp.ftp.FTP;

public class Main {

	public static void main(String[] args) throws JsonSyntaxException, JsonIOException, IOException, URISyntaxException {
		
		/*
		 * --enable-native-access=ALL-UNNAMED
		 * --add-opens=java.base/sun.security.util=ALL-UNNAMED
		 * --add-opens=java.base/sun.security.ssl=ALL-UNNAMED
		 */
		
		System.setProperty("jdk.tls.client.enableSessionTicketExtension", String.valueOf(false));
		System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
		System.setProperty("jdk.tls.allowLegacyResumption", String.valueOf(true));
		System.setProperty("jdk.tls.useExtendedMasterSecret", String.valueOf(false));
		
		/*
		 * Check to see if the config file exists.
		 * If it doesn't create a default one and exit.
		 * If it does, then read it in.
		 */
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Config config;
		File configFile = new File("config.json");
		
		if (configFile.exists()) {
			config = gson.fromJson(new FileReader(configFile), Config.class);
		} else {
			config = new Config();
			FileWriter writer = new FileWriter(configFile);
			gson.toJson(config, writer);
			writer.close();
			System.out.println("No configuration found. Writing default configuration and exiting.");
			System.exit(0);
		}
		
		// Start our FTP threads.
		List<FTP> ftpThreads = new ArrayList<FTP>();
		
		for (FTPHost ftpHost : config.ftpHosts) { // TODO Make sure we don't have duplicate FTP servers.
			try {
				FTP ftpThread = new FTP(ftpHost);
				ftpThreads.add(ftpThread);
				ftpThread.start();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// TODO Start our local thread(s)
		
		final Thread mainThread = Thread.currentThread();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("Shutdown signal received.");
				for (FTP ftpThread : ftpThreads) {
					ftpThread.interrupt();
					try {
						ftpThread.join();
					} catch (InterruptedException e) {
						// Pass
					}
				}
				try {
					mainThread.join();
					System.out.println("All threads terminated.");
				} catch (InterruptedException e) {
					// Pass
				}
			}
		});
	}

}
