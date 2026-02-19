package to.joe.ftp.config;

import java.util.ArrayList;
import java.util.List;

public class Config {
	
	public List<FTPHost> ftpHosts = new ArrayList<FTPHost>();
	public LocalHost localHost = new LocalHost();
	
	public Config() {
		ftpHosts.add(new FTPHost());
	}

}
