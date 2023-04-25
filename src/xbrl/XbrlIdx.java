package xbrl;

import java.io.*;
import java.net.*;

public class XbrlIdx {

	// was: ftp://ftp.sec.gov/edgar/full-index/

	public static String ftpAddress = "https://www.sec.gov/Archives/edgar/full-index/";
	public static String basePath = "c:/backtest/xbrl/";
	public static String fileName = "/xbrl.zip";

	public static void download(int year, int qtr, String localPath) {
		String localFileName = localPath + fileName;
		String address = ftpAddress + year + "/QTR" + qtr + "/xbrl.zip";
		System.out.println("sec.gov:: " + address);
		OutputStream out = null;
		URLConnection conn = null;
		InputStream in = null;

		try {
			URL url = new URL(address);
			/*
			 * creating a string url which is an instance of the class URL. The
			 * class URL takes the string address and parses it and understand
			 * http/https schemes server name, folder, files, etc. It has
			 * inherent built in functionality to understand certain protocols
			 * related to URLs?
			 */
			out = new BufferedOutputStream(new FileOutputStream(localFileName));
			// instantiating an instance of a new output stream to be written to
			// a local file.
			conn = url.openConnection();
			// this is to make the connection to the URL.
			in = conn.getInputStream();
			// this is the input stream you want to read from the URL
			// connection. This way we can read what the server/address is
			// giving me back
			byte[] buffer = new byte[1024];
			// byte array

			int numRead;
			@SuppressWarnings("unused")
			long numWritten = 0;

			while ((numRead = in.read(buffer)) != -1) {
				// reading (read) from the input stream (in) into the buffer
				// array. This is just reading whether or not buffer has bytes
				// and it is also checking number of bytes read. If no char to
				// read it will be -1.

				out.write(buffer, 0, numRead);
				// writing to output file (see how out was instantiated) and
				// putting into out what is read. The source being written from
				// is buffer, the starting point is 0 and end point is numRead.
				// see help details re 'write' etc.
				numWritten += numRead;
				// this just keeps track of # of char read.
			}

			// System.out.println(localFileName + "\t" + numWritten);
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException ioe) {
			}
		}
	}

	public static void main(String[] args) {

	}
}
