package xbrl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

//this class will establish connection to ftp site, get the list of files and download files. 
//This main method is not being called anywhere.  EastFTPFileFilter is used to filter the list of files.

public class EasyFTPClient {

	private FTPClient ftp;

	@SuppressWarnings("unused")
	private String userName;
	@SuppressWarnings("unused")
	private String password;
	@SuppressWarnings("unused")
	private String ftpSite;

	public EasyFTPClient(String ftpSite, int port, String username,
			String password, boolean useSSL) throws SocketException,
			IOException {
		System.out.println("ftpSite="+ftpSite);
		this.ftpSite = ftpSite;
		this.ftpSite = username;
		this.password = password;
		if (useSSL)
			ftp = new FTPSClient(true);
		else
			ftp = new FTPClient();
		// picks class based on whether or not connection is secure. FTPSClient
		// is if secure else FTPClient (not secure)

		// suppress login details
		// ftp.addProtocolCommandListener(new PrintCommandListener(new
		// PrintWriter(System.out), true));
		try {
			int reply;
			if (port > 0) {
				ftp.connect(ftpSite, port);
			} else {
				ftp.connect(ftpSite);
			}
			System.out.println("Connected to " + ftpSite + " on "
					+ (port > 0 ? port : ftp.getDefaultPort()));
			//ftp.enterLocalPassiveMode();
			// After connection attempt, you should check the reply code to
			// verify success.
			reply = ftp.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				throw new IOException("FTP server refused connection.");
			}
		} catch (IOException e) {
			if (ftp.isConnected()) {
				try {
					ftp.disconnect();
				} catch (IOException f) {
					// do nothing
				}
			}
			System.err.println("Could not connect to server.");
			throw new IOException("Could not connect to server.", e);
		}
		if (null != username)
			if (!ftp.login(username, password)) {
				ftp.logout();
				throw new IOException("Could not authenticate/login to server.");
			}
		// Use passive mode as default because most of us are behind firewalls
		// these days. else make it active: ftp.enterLocalActiveMode();
		ftp.enterLocalPassiveMode();
		ftp.setUseEPSVwithIPv4(false);
		System.out.println("ftp client object is ready...");
	}

	public void copyToLocal(String remoteFilePath, String localFilePath,
			boolean isAsciiFile) throws IOException {
		System.out.println("copying remote file:" + remoteFilePath
				+ "  to local path:" + localFilePath);
		if (isAsciiFile)
			ftp.setFileType(FTP.ASCII_FILE_TYPE);
		else
			ftp.setFileType(FTP.BINARY_FILE_TYPE);
		OutputStream output = new FileOutputStream(localFilePath);
		boolean fileRetrieved = ftp.retrieveFile(remoteFilePath, output);
		// <=here the file is retrieved from ftp site (downloaded).
		output.close();
		if (!fileRetrieved) {
			System.out.println("No such file exists:" + remoteFilePath);
			File f = new File(localFilePath);
			if (f.exists() && f.length() == 0)
				f.delete();
		}
	}

	public FTPFile[] listFiles(String remoteFile, FTPFileFilter fileFilter)
			throws IOException {
		boolean isLenient = true;
		if (isLenient) {
			FTPClientConfig config = new FTPClientConfig();
			config.setLenientFutureDates(true);
			ftp.configure(config);
		}
		FTPFile[] files = null;
		if (null != fileFilter)
			files = ftp.listFiles(remoteFile, fileFilter);
		else
			files = ftp.listFiles(remoteFile);
		
		  for (FTPFile f : files) { System.out.println(f.getRawListing());
		  System.out.println(f.toFormattedString()); }
		 
		return files;
	}

	public void close() {
		if (null != ftp && ftp.isConnected()) {
			try {
				ftp.disconnect();
			} catch (IOException f) {
				// do nothing
			}
		}
	}

	protected void finalize() {
		close();
	}

	public static void main(String[] arg) throws SocketException, IOException {
		// ftp.sec.gov
		// replaced by: https://www.sec.gov 
		EasyFTPClient ftp = new EasyFTPClient("https://www.sec.gov", 0, "anonymous",
				"", false);
		//ftp.copyToLocal("/edgar/daily-index/2013/index.htm", "c:/index.html",
		//		true);
		// these are just dummy entries to test b/c main methods are not being
		// called. 
		System.out.println(ftp.listFiles("/edgar/Feed/2014/QTR4/", null));
		ftp.close();
		System.out.println("done copying");
	}
}
