package contracts;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.regex.Pattern;

import xbrl.FileSystemUtils;
import xbrl.MysqlConnUtils;
import xbrl.NLP;
import xbrl.Utils;

public class GetContractsOnline {

	public static void exportRoyaltyTrusts(String outputfolder, String filename, String exportFromThisDate)
			throws SQLException, FileNotFoundException {

		// currently the CIK is hardcoded below based on my own research as to which
		// CIKs are part of the royalty trust group. The end product will allow the user
		// to select those companies using the ticker symbol, CIK or filer name. Each
		// of these are metadata items in the doc0. Currently CIK is not in each
		// document. But the others are. A user will inherently use the sector and
		// industry to select peers. We of course have associated the ticker with cik of
		// those. once the user has identified the companies we gather all
		// the relevant filings based on the filing types we designate.
		// Currently these are the 10-K/Q and the 8-K. This can be expanded later.
		// currently as well, I have used a description filter as noted in the main
		// method.this only relates to the form 8-K. unfortunately, this will require
		// homogenization and work on the backend I myself and others because I don't
		// think we can leave it to the user. But we can determine more later.

		Utils.createFoldersIfReqd(outputfolder);
		File file = new File(outputfolder + filename);
		if (file.exists())
			file.delete();

		// patterns can't contain certain regex- such as '|'.
		String query = "DROP PROCEDURE IF EXISTS royalty_trusts;\r\n" + "CREATE PROCEDURE royalty_trusts()\r\n"
				+ "begin\r\n" + "\r\n" + "\r\n"
				+ "select t1.ACCNO, t1.CIK, t1.`Form Type` formType, t1.`Date Filed` fDate, t1.`Company Name` companyName, year(t1.`Date Filed`) yr, quarter(t1.`Date Filed`) qtr\r\n"
				+ ",'royaltyTrusts' contractType\r\n"
				+ ", concat('https://www.sec.gov/Archives/edgar/data/',cik,'/',replace(accno,'-',''),'/',accno,'.txt') edgarLink\r\n"
				+ "from `stockanalyser`.`masterIdx_all` t1 where \r\n" + "\r\n"
				+ "(cik='850033' or cik='881787' or cik='923680' or cik='895474' or cik='1520048' or cik='862022' or cik='721765' or cik='62362' \r\n"
				+ "or cik='313364' or cik='65172' or cik='1538822' or cik='1088166' or cik='319654' \r\n"
				+ "or cik='710752' or cik='319655' or cik='1521168' or cik='893486' or cik='912030' or cik='1505413' or cik='895007')\r\n"
				+ "\r\n" + "and  t1.`Form Type` rlike '(8|10)-(k|q)' \r\n" + "\r\n" + "and t1.`Date Filed`>"
				+ exportFromThisDate + "\r\n" + "\r\n" + "order by `date filed` desc,cik,accno\r\n" + "INTO OUTFILE '"
				+ outputfolder + filename + "' FIELDS TERMINATED BY '||' LINES TERMINATED BY '\\n';\r\n" + "\r\n"
				+ "\r\n" + "end;\r\n" + "\r\n" + "\r\n" + "call royalty_trusts();\r\n" + "\r\n"
				+ "DROP PROCEDURE IF EXISTS royalty_trusts;\r\n" + "";

		MysqlConnUtils.executeQuery(query);

	}

	public static void downLoadAcc(String acc, String address, String path, String metadata) throws Exception {
		System.out.println("Connecting to FTP server...");

		System.out.println("site:: " + address);
		String localPath = path;

		if (acc.trim().length() < 1)
			return;

		URL url = null;
		try {
			url = new URL(address);
			// System.out.println("server+xmlStub:: " + url);
		} catch (MalformedURLException e) {
			// System.out.println("new url");
			e.printStackTrace();
		}

		URLConnection con = null;
		try {
			System.out.println("open conection");
			con = url.openConnection();
			if (con == null) {
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
			out = new FileOutputStream(localPath + "/" + acc + ".txt");
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

		String text = Utils.readTextFromFile(localPath + "/" + acc + ".txt");
		File file = new File(localPath + "/" + acc + ".txt");
		if (file.exists())
			file.delete();

		PrintWriter pw = new PrintWriter(file);
		pw.append(metadata + "\r\n" + text);
		pw.close();
		System.out.println("File downloaded.");

	}

	public static void main(String[] args) throws Exception {
		NLP nlp = new NLP();

		// update masterIdx table in MySQL first, which updates
		// `stockanalyser`.`masterIdx_all` table
//		QuarterlyMasterIdx.getQuarterlyMasterIdx(startYr, endYr, qtr);

		// create MySQL query to select ACCs to export to a file - such as all
		// RoyaltyTrusts. Once exported to that file I run downloadAcc which downloads
		// to a folder each of those ACCs and appends the masterIdx metadata. Then I run
		// main method parseFromCompleteTextFilingFromEdgarOnline
		String path = "E:/getContracts/solrIngestion/edgarOnline/", filename = "royaltyTrustsAccNos.txt";
		exportRoyaltyTrusts(path, filename, "19950901");
		String txt = Utils.readTextFromFile(path + filename);
		String[] txtSplit = txt.split("\n");
		String line = "", url = "", acc = "";
		System.out.println("txtSplit.len=" + txtSplit.length);

		// download each acc exported from method above.
		String parentFolder = "E:/getContracts/solrIngestion/edgarOnline/", subfolder = "royaltyTrusts";
		for (int i = 0; i < txtSplit.length; i++) {
			line = txtSplit[i];
			String[] fields = line.split("\\|\\|");
			url = fields[fields.length - 1];
			acc = fields[0];
			try {
				downLoadAcc(acc, url, parentFolder + subfolder, line);//
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// read from file the doc and then add metadata
		}

		Pattern patternFormType = Pattern.compile("(8|10)-(Q|K)");
		// parses if formType and patternDescr is found.
		Pattern patternDescr = Pattern.compile("(EX-99|Press|PRESS|Release|RELEASE)");

		/* after this export my parser picks it up.
		 * Exclude graphics and other gobbly gook exhibits from main method; don't grab
		 * 8-K if its stripped size is less than 3k
		 */

		File files = new File(parentFolder + subfolder);
		System.out.println("folder=" + parentFolder + subfolder + "/");
		File[] listOfFiles = files.listFiles();
		System.out.println("listOfFiles.len=" + listOfFiles.length);
	

		for (int i = 0; i < listOfFiles.length; i++) {

			System.out.println("file=" + listOfFiles[i].getAbsolutePath());

//			GetContracts.parseFromCompleteTextFilingFromEdgarOnline(listOfFiles[i].getAbsolutePath(), true,
//					patternFormType, patternDescr);

		}
	}
}
