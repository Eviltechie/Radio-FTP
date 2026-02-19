package to.joe.ftp;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import to.joe.ftp.config.Config;

public class Main {

	public static void main(String[] args) throws JsonSyntaxException, JsonIOException, IOException {
		
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
	}

}
