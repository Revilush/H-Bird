package xbrl;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Xbrl {

	public static String server = "https://www.sec.gov/Archives/";

	public static void download(String siteLocation, String localPath,
			String fileName) throws Exception {
		System.out.println("Connecting to FTP server...");
		// pass year and qtr# and localPath
		// Connection String
		
		System.out.println("site:: " + server+siteLocation);

		if(siteLocation.trim().length()<1)
			return;
		
		
		URL url = null;
		try {
			url = new URL(server + siteLocation.trim());
			System.out.println("server+xmlStub:: "+url);
		} catch (MalformedURLException e) {
			System.out.println("new url");
			e.printStackTrace();
		}

		URLConnection con = null;
		try {
			System.out.println("open conection");
			con = url.openConnection();
			 if(con==null){
			 return;
			 }
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(con.getInputStream());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		System.out.println("Downloading file.");

		FileOutputStream out = null;
		try {
			FileSystemUtils.createFoldersIfReqd(localPath);
			out = new FileOutputStream(localPath + "/" + fileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		int i = 0;
		byte[] bytesIn = new byte[1024];
		try {
			while ((i = in.read(bytesIn)) >= 0) {// while the input stream is
													// "reading" bytesIn>0
				out.write(bytesIn, 0, i);// then write.
			}
		} catch (IOException e) {

			e.printStackTrace();
		}
		try {
			out.close();
		} catch (IOException e) {

			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("File downloaded.");

	}

	public static void main(String[] args) throws Exception {

		download(
				"edgar/data/98338/0001213900-16-017477.txt",
				"c:/temp2/", "0001213900-16-017477.txt");
		
/*		System.out.println("Connecting to FTP server...");

		// Connection String
		URL url = new URL(
				"https://www.sec.gov/Archives/edgar/data/875316/0000875316-98-000020.txt");
		// need to pass year and qtr #.xx
		URLConnection con = url.openConnection();

		BufferedInputStream in = new BufferedInputStream(con.getInputStream());

		System.out.println("Downloading file.");

		// Downloads the selected file to the C drive
		FileOutputStream out = new FileOutputStream(
				"c://backtest//text.txt");
		// need to pass localPath and year and qtr#.xx
		int i = 0;
		byte[] bytesIn = new byte[1024];
		while ((i = in.read(bytesIn)) >= 0) {
			out.write(bytesIn, 0, i);
		}

		out.close();
		in.close();

		System.out.println("File downloaded.");*/
		
	}
}
