package to.joe.ftp;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import to.joe.ftp.config.Config;
import to.joe.ftp.config.FTPHost;

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
		
		Logger logger = LogManager.getLogger(Main.class.getName());
		logger.info("Starting up radio-ftp");
		
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
			logger.fatal("No configuration found. Writing default configuration and exiting.");
			System.exit(1);
		}
		
		{
			// Check for unique FTP hosts.
			Set<String> hosts = new HashSet<String>();
			for (FTPHost ftpHost : config.getFTPHosts()) {
				hosts.add(String.format("%s%s", ftpHost.getHost().toLowerCase(), ftpHost.getPort()));
			}
			if (hosts.size() != config.getFTPHosts().size()) {
				logger.fatal("Duplicate FTP host detected, exiting.");
				System.exit(1);
			}
		}
		
		final Watchdog watchdogThread = new Watchdog(config);
		watchdogThread.start();
		
		Runtime.getRuntime().addShutdownHook(new ShutdownHook(watchdogThread));
	}

}
