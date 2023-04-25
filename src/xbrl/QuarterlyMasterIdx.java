package xbrl;

import java.io.*;
import java.net.*;
import java.sql.SQLException;

public class QuarterlyMasterIdx {
	// was=> ftp://ftp.sec.gov/edgar/full-index/
	public static String ftpAddress = "https://www.sec.gov/Archives/edgar/full-index/";
	// was=> ftp://ftp.sec.gov/edgar/Feed/
	public static String ftpSecZipAddress = "https://www.sec.gov/Archives/edgar/Feed/";

	public static String basePath = "c:/backtest/master/";
	public static String fileName = "/master.zip";

	public static void download(int year, int qtr, String localPath, String zipFile) throws IOException {
		String localFileName = localPath + zipFile;
		
		File file = new File(localFileName);
		System.out.println("download file=" + file.getAbsolutePath());

//		if(file.exists() ){
//			return;
//		}

		if (file.exists())
			file.delete();

		FileSystemUtils.createFoldersIfReqd(localPath);
		String secZipAddress = ftpSecZipAddress + year + "/QTR" + qtr + "/" + zipFile;
		String address = ftpAddress + year + "/QTR" + qtr + "/" + zipFile;
		System.out.println("sec.gov:: " + address);
		OutputStream out = null;
		URLConnection conn = null;
		InputStream in = null;
		
		HttpDownloadUtility.downloadFile(address, localPath);
//
//		try {
//			URL url = new URL(address);
//			out = new BufferedOutputStream(new FileOutputStream(localFileName));
//			conn = url.openConnection();
//			in = conn.getInputStream();
//			byte[] buffer = new byte[1024];
//
//			int numRead;
//			@SuppressWarnings("unused")
//			long numWritten = 0;
//
//			while ((numRead = in.read(buffer)) != -1) {
//
//				out.write(buffer, 0, numRead);
//				numWritten += numRead;
//				// this just keeps track of # of char read.
//			}
//
//		} catch (Exception exception) {
//			exception.printStackTrace();
//		} finally {
//			try {
//				if (in != null) {
//					in.close();
//				}
//				if (out != null) {
//					out.close();
//				}
//			} catch (IOException ioe) {
//			}
//		}

		System.out.println("2 file.len=" + file.length() + " file=" + file.getAbsolutePath());

		if (file.exists()) {
			ZipUtils.deflateZipFile(localPath + "/master.zip", localPath + "/");
			System.out.println("unzipped master.zip: localPath ==" + localPath);
			File f1 = new File(localPath + "/master.idx");
			System.out.println("f1");
			String str = Utils.readTextFromFile(f1.getAbsolutePath()).replaceAll("\\\\", " ");
			System.out.println("str");
			File f = new File(localPath + "/master.idx2");
			System.out.println("f");
			PrintWriter pw = new PrintWriter(f);
			pw.println(str);
			System.out.println("pw");
			pw.close();
			f1.delete();
			System.out.println("f1.del");
			f.renameTo(f1);
			System.out.println("f.ren");
		}
	}

	public static void getQuarterlyMasterIdx(int startYr, int endYr, int sQtr, int eQtr)
			throws IOException, SQLException {

		String str = "";
		File file = new File("");
		File f = new File("");
		for (int yr = startYr; yr <= endYr; yr++) {
			for (int q = sQtr; q <= eQtr; q++) {
				download(yr, q, basePath + yr + "/QTR" + q + "/", "master.zip");
				String secZipAddress = ftpSecZipAddress + yr + "/QTR" + q + "/" + "master.zip";
				HttpDownloadUtility.downloadFile(secZipAddress, basePath + yr + "/QTR" + q + "/master.idx");
				file = new File(basePath + yr + "/QTR" + q + "/master.idx");
//				saved c:/backtest/master/yr/qtr/

				if (!file.exists()) {
					ZipUtils.deflateZipFile(basePath + yr + "/QTR" + q + "/master.zip",
							basePath + yr + "/QTR" + q + "/");
				}

				if (file.exists()) {
					str = Utils.readTextFromFile(file.getAbsolutePath()).replaceAll("\\\\", " ");
					f = new File(basePath + yr + "/QTR" + q + "/master2.idx");
					PrintWriter pw = new PrintWriter(f);
					pw.println(str);
					pw.close();
				}

				if (file.exists())
					file.delete();

				f.renameTo(file);

				String query = "\r\rDROP TABLE IF EXISTS tmp_masterIdx; \r" + "CREATE TABLE `tmp_masteridx` ( "
						+ "`CIK` int(11) DEFAULT NULL, \r" + "`Company Name` varchar(255) DEFAULT NULL,\r"
						+ " `Form Type` varchar(50) DEFAULT NULL, \r" + " `Date Filed` date DEFAULT NULL,\r"
						+ " `Filename` varchar(255) NOT NULL DEFAULT ''\r," + " PRIMARY KEY (`Filename`), \r"
						+ "KEY `Form Type` (`Form Type`)\r " + ") ENGINE=myisam DEFAULT CHARSET=latin1;\r"
						+ " LOAD DATA INFILE \r" + "'" + basePath + yr + "/QTR" + q
						+ "/master.idx' IGNORE INTO TABLE TMP_MASTERIDX \rFIELDS TERMINATED BY '|' OPTIONALLY ENCLOSED BY '\\\\' \r"
						+ "LINES TERMINATED BY '\\n' IGNORE 11 LINES; " + "\r\rinsert ignore into masterIdx_all \r "
						+ "\rselect left(right(replace(filename,'\\r',''),24),20) ACCNO,cik,`company name`,`form type`, `Date Filed`,`filename` "
						+ "\rfrom TMP_MASTERIDX ;";

				System.out.println("getQuarterlyMasterIdx: query= " + query);
				MysqlConnUtils.executeQueryDB(query, "contracts");
//				MysqlConnUtils.executeQuery(query);

			}
			sQtr = 1;
		}
	}

	public static void main(String[] args) throws IOException, SQLException {

		int startYr = 2022, endYr = 2022, qtr = 3;
		getQuarterlyMasterIdx(startYr, endYr, qtr, qtr);
	}
}
