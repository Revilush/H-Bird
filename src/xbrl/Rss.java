package xbrl;

import java.io.*;
import java.net.*;

public class Rss {

	public static void download(String address, String localFileName) {
		
		System.out.println("site address="+address+"\r localFilename="+localFileName);
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

			 System.out.println("writing file"+localFileName + "\t" + numWritten);
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

	
	public static void download(String address) {
		int lastSlashIndex = address.lastIndexOf('/');
		if (lastSlashIndex >= 0 && lastSlashIndex < address.length() - 1) {
			download(address, address.substring(lastSlashIndex + 1));
		} else {
			System.err.println("Could not figure out local file name for "
					+ address);
		}
	}
	
	public static void testURL() throws Exception {
	    String strUrl = "https://www.sec.gov/Archives/edgar/data/1046050/000093905716001117/0000939057-16-001117-xbrl.zip";

	    try {
	        URL url = new URL(strUrl);
	        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
	        urlConn.connect();
//	        assertEquals(HttpURLConnection.HTTP_OK, urlConn.getResponseCode());
	        
	    } catch (IOException e) {
	        System.err.println("Error creating HTTP connection");
	        e.printStackTrace();
	        throw e;
	    }
	}

	public static void main(String[] args) throws Exception {
		// download("https://www.sec.gov/Archives/edgar/xbrlrss.all.xml",
		// "c:/backtest/xbrl/rss/allxbrlrss.xml");

		// download(
		// "https://www.sec.gov/Archives/edgar/data/1001258/000119312512268182/d365769dex991.htm",
		// "c:/backtest/8-k/d365769dex991.htm");

//		testURL();
//		download(
//				"https://www.sec.gov/Archives/edgar/data/1046050/000093905716001117/0000939057-16-001117-xbrl.zip",
//				"c://backtest/test.zip");
		// download("http://www.businesswire.com/portal/site/home/news/subject/?vnsId=31350",
		// "c:/backtest/newswire/test3.xml");
		
		String str = "/Archives/edgar/data/12208/000001220817000041/a10q63017_htm.xml";
		System.out.println("str="+str.substring(str.lastIndexOf("/")+1));
	}
}
