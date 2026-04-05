package to.joe.ftp.ftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPCmd;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import to.joe.ftp.DownloadThread;
import to.joe.ftp.config.FTPHost;
import to.joe.ftp.config.Fetcher;

public class FTPDownloadThread extends DownloadThread {
	
	private FTPClient ftpClient;
	
	private Logger logger = LogManager.getLogger(FTPDownloadThread.class.getName());
	
	public FTPDownloadThread(FTPHost config) throws SQLException, IOException {
		super(config);
		
		getFTPClient();
	}
	
	private FTPClient getFTPClient() throws IOException {
		if (ftpClient != null && ftpClient.isConnected()) { // If we are already connected, we return what we have.
			return ftpClient;
		}
		
		logger.info("Establishing FTP connection to {}:{}", getConfig().getHost(), getConfig().getPort());
		
		if (getConfig().getFTPS()) { // If we are using FTPS, we use our modified FTPS client instead of the standard one.
			FTPSClientSSLSessionReuse ftps = new FTPSClientSSLSessionReuse();
			ftps.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
			ftpClient = ftps;
		} else {
			ftpClient = new FTPClient();
		}
		
		int replyCode;
		
		ftpClient.connect(getConfig().getHost(), getConfig().getPort());
		printLog(ftpClient);
		
		replyCode = ftpClient.getReplyCode();
		
		if (!FTPReply.isPositiveCompletion(replyCode)) {
			ftpClient.disconnect();
			logger.error("FTP server refused connection.");
		}
		
		logger.info("Connected to {}", getConfig().getHost());
		
		ftpClient.login(getConfig().getUsername(), getConfig().getPassword()); // TODO Test what happens if we put in wrong info, and then log it.
		printLog(ftpClient);
		
		if (ftpClient instanceof FTPSClient) {
			FTPSClient ftpsClient = (FTPSClient) ftpClient;
			ftpsClient.execPBSZ(0);
			ftpsClient.execPROT("P");
		}
		
		ftpClient.enterLocalPassiveMode();
		ftpClient.features();
		printLog(ftpClient);
		
		if (!ftpClient.hasFeature(FTPCmd.MLSD) && !ftpClient.hasFeature(FTPCmd.MDTM)) {
			logger.warn("Server does not support accurate timestamps (MLSD or MDTM commands), risk of issues at new year's.");
		}
		
		return ftpClient;
	}

	private void printLog(FTPClient client) {
		String[] log = client.getReplyStrings();
		for (String string : log) {
			logger.debug(string);
		}
	}

	@Override
	protected FTPHost getConfig() {
		return (FTPHost) super.getConfig();
	}

	@Override
	protected List<QueuedFile> getInterestedFiles(Fetcher fetcher) throws IOException {
		List<QueuedFile> interestedFiles = new ArrayList<QueuedFile>();
		
		getFTPClient().changeWorkingDirectory(fetcher.getSourcePath());
		printLog(ftpClient);
		
		FTPFile[] files;
		if (getFTPClient().hasFeature(FTPCmd.MLSD)) { // If possible, we try to use MLSD to list the directory. If not, we fall back to regular LIST.
			files = getFTPClient().mlistDir();
			printLog(ftpClient);
		} else {
			files = getFTPClient().listFiles();
			printLog(ftpClient);
		}
		
		Pattern pattern = Pattern.compile(fetcher.getSourcePattern());
		
		for (FTPFile file : files) {
			if (file.isFile()) {
				Matcher matcher = pattern.matcher(file.getName());
				if (matcher.matches()) {
					Instant timestamp;
					if (!getFTPClient().hasFeature(FTPCmd.MLSD) && getFTPClient().hasFeature(FTPCmd.MDTM)) {
						printLog(ftpClient);
						timestamp = getFTPClient().mdtmInstant(file.getName());
						printLog(ftpClient);
					} else {
						timestamp = file.getTimestampInstant();
					}
					interestedFiles.add(new QueuedFile(file.getName(), timestamp, file.getSize(), matcher));
				}
			}
		}
		
		return interestedFiles;
	}

	@Override
	protected void downloadFile(Fetcher fetcher, QueuedFile pendingFile, File tempFile) throws IOException {
		FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
		
		Instant start = Instant.now();
		
		logger.info("Downloading file to {}", tempFile.getAbsolutePath());
		getFTPClient().retrieveFile(pendingFile.fileName, fileOutputStream);
		printLog(ftpClient);
		
		Instant end = Instant.now();
		Duration difference = Duration.between(start, end);
		
		fileOutputStream.close();
		
		logger.info("Downloaded {} bytes in {}.{} seconds", pendingFile.fileSize, difference.toSeconds(), difference.toMillisPart());
	}

	@Override
	protected void deleteSourceFile(Fetcher fetcherm, QueuedFile pendingFile) throws IOException {
		getFTPClient().deleteFile(pendingFile.fileName);
		printLog(ftpClient);
		
		logger.info("Deleted {} from remote host", pendingFile.fileName);
	}

	@Override
	protected void cleanup() {
		try {
			ftpClient.logout();
			printLog(ftpClient);
			logger.info("Logged out of {}", getConfig().getHost());
		} catch (FTPConnectionClosedException e) {
			// Pass, we are closing the connection anyway.
		} catch (IOException e) {
			logger.error("", e);
		} finally {
			try {
				ftpClient.disconnect();
			} catch (IOException e) {
				// Pass
			} finally {
				logger.info("Disconnected from {}", getConfig().getHost());
			}
		}
	}

}
