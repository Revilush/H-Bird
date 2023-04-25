package xbrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

public class InsiderParserLocal {

	// private String list = null;
	public static String baseFolder = "c:/backtest/insider/";

	public static int getQuarter(Calendar date) {
		return ((date.get(Calendar.MONTH) / 3) + 1);
	}

	public static void dateRangeQuarters(Calendar startDate, Calendar endDate)
			throws SocketException, IOException {

		int startYear = startDate.get(Calendar.YEAR);
		int endYear = endDate.get(Calendar.YEAR);
		int startQtr = getQuarter(startDate);
		int endQtr = getQuarter(endDate);
		String localPath = baseFolder + "/" + endYear + "/QTR" + endQtr;

		int iYear = (endYear - startYear) * 4;
		int iQtr = (endQtr - startQtr) + 1;
		int totalQtrs = iYear + iQtr;
		iYear = startYear;
		iQtr = startQtr;
		Calendar cal = Calendar.getInstance();

		for (int i = 1; i <= totalQtrs; i++) {
			cal.set(Calendar.YEAR, iYear);
			cal.set(Calendar.MONTH, (iQtr * 3) - 1);
			String zipFile = baseFolder + "/" + iYear + "/QTR" + iQtr+"/"+iYear+"QTR"+iQtr+".zip";
			File zFile = new File(zipFile);
			String zipFilePath = baseFolder + "/" + iYear + "/QTR" + iQtr;
			if(zFile.exists()){
				ZipUtils.deflateZipFile(zipFile, zipFilePath);	
			}
			localPath = baseFolder + "/" + iYear + "/QTR" + iQtr;
			String mysqlLocalPath = baseFolder + "/" + iYear + "/QTR" + iQtr
					+ "/insider.csv";
			File f = new File(mysqlLocalPath);
			if (f.exists()) {
				f.delete();
				System.out.println("create mysql.scv");
				createMysqlCsv(mysqlLocalPath, iYear, iQtr);
			} else if (!f.exists()) {
				createMysqlCsv(mysqlLocalPath, iYear, iQtr);
			}
			 System.out.println("localPath::" + localPath);
			parseLocalInsiderFiling(localPath);
			iQtr++;
			if (iQtr > 4) {
				iYear++;
				iQtr = 1;
			}
		}
	}

	public static void createMysqlCsv(String localPath, int iYear, int iQtr) throws IOException {
		// String query =
		// "INSERT IGNORE INTO xbrl_accno_id(accNo_id,ACCNO,ACCEPTEDDATE) SELECT 0,0,'1900-01-01';";

		String masterIdxLocalPath = localPath.substring(0,
				localPath.lastIndexOf("/") + 1)
				+ "master.idx";
		System.out.println("masterIdxLocalPath="+masterIdxLocalPath);
		
		File file = new File(masterIdxLocalPath);
		
		if(file.exists()){
			String str = Utils.readTextFromFile(file.getAbsolutePath())
					.replaceAll("\\\\", " ");
			File f = new File(masterIdxLocalPath + 2);
			PrintWriter pw = new PrintWriter(f);
			pw.println(str);
			pw.close();
			file.delete();
			f.renameTo(file);
		}
		
		
		String masterCsv = masterIdxLocalPath.replaceAll(".idx", ".csv");
		masterCsv = masterCsv.replaceAll("\\\\", " ");
		file = new File(masterCsv);
		System.out.println("file="+file.getAbsolutePath());
		if(file.exists()){
			System.out.println("file being deleted");
			file.delete();
		}
		
		String day= "31";
		if(iQtr * 3==9 || iQtr*3==6)
			day="30";
		

		String query = "\r\rDROP TABLE IF EXISTS tmp_masterIdx; \r"
				+ "CREATE TABLE `tmp_masteridx` ( `CIK` int(11) DEFAULT NULL, \r"
				+ "`Company Name` varchar(255) DEFAULT NULL,\r"
				+ " `Form Type` varchar(50) DEFAULT NULL, \r"
				+ " `Date Filed` date DEFAULT NULL,\r"
				+ " `Filename` varchar(255) NOT NULL DEFAULT ''\r,"
				+ " PRIMARY KEY (`Filename`), \r"
				+ "KEY `Form Type` (`Form Type`)\r "
				+ ") ENGINE=myisam DEFAULT CHARSET=latin1;\r"
				+ " LOAD DATA INFILE \r'"
				+ masterIdxLocalPath
				+ "' IGNORE INTO TABLE TMP_MASTERIDX \rFIELDS TERMINATED BY '|' OPTIONALLY ENCLOSED BY '\\\\' \r" +
				"LINES TERMINATED BY '\\n' IGNORE 11 LINES; "
				+ "\r\rDROP TABLE IF EXISTS TMP_IDX ; "
				+ "\rCREATE TABLE TMP_IDX ENGINE=MYISAM "
				+ "\rselect left(right(trim(filename),25),20) ACCNO, `Date Filed` "
				+ "\rfrom TMP_MASTERIDX where (`Form Type`= '4' or `Form Type`='3' or `Form Type`='5' or `Form Type`= '4/A' "
				+ "\ror `Form Type`='3/A' or `Form Type`='5/A') group by left(right(trim(filename),25),20);"
				+ "\r ALTER TABLE TMP_IDX ADD KEY  (ACCNO); \r\rDROP TABLE IF EXISTS TMP_IDX2 ;"
				+ "\r CREATE TABLE TMP_IDX2 ENGINE=MYISAM "
				+ "\rselect * from (select t1.accno idx_accno,`DATE FILED`,t2.accno schema_accno from TMP_IDX t1 LEFT "
				+ "\rjoin i_issuer t2 on t1.accno=t2.accno) t1 where schema_accno is null ; " +
				"\rALTER TABLE TMP_IDX2 ADD KEY (idx_ACCNO), add key (schema_accno); " +
				"\rSELECT T2.* FROM TMP_IDX2 T1 INNER JOIN TMP_MASTERIDX T2 ON T1.idx_accno = left(right(trim(filename),25),20) "
				+ "\rWHERE T1.`DATE FILED` >= '"
				+ +iYear
				+ "-"
				+ ((iQtr * 3) - 2)
				+ "-01' and\r\r t1.`Date Filed`<='"
				+ iYear
				+ "-"
				+ iQtr * 3
				+ "-"+day+"' GROUP BY left(right(trim(filename),25),20) INTO OUTFILE '"
				+ masterCsv
				+ "'   FIELDS TERMINATED BY '||' ;\r\r DROP TABLE IF EXISTS tmp_masterIdx;";
		
		try {
			MysqlConnUtils.executeQuery(query);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	public static void parseLocalInsiderFiling(String localPath)
			throws IOException {

		String masterIdx = localPath + "/master.csv";
		File f3 = new File(masterIdx);

		if (!f3.exists()) {
			System.out
					.println("master.idx file DOES NOT EXIST - it must be downloaded");
			return;
		}

		BufferedReader rdrInsider = null;
		// BufferedReader rdrMysql = null;
		try {
			rdrInsider = new BufferedReader(new FileReader(masterIdx));
			// rdrMysql = new BufferedReader(new FileReader(mysqlCsv));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		String insiderLine;
		try {
			while ((insiderLine = rdrInsider.readLine()) != null) {
				String[] items = insiderLine.split("\\|\\|");
				System.out.println("items="+Arrays.toString(items));
				if (items.length < 5)
					continue;
				if (items[4].contains("edgar") && items[2].contains("3")
						|| items[4].contains("edgar") && items[2].contains("4")
						|| items[4].contains("edgar") && items[2].contains("5")) {
					// String formType = items[2];
					String fileName = items[4];
					String acc = fileName.substring(
							fileName.lastIndexOf("/") + 1,
							fileName.lastIndexOf("."));

					String accXml = acc + ".xml";
					// String accNoNoHyphens = accNo.replace("-", "");
					File accXmlPath = new File(localPath + "/" + accXml);

					if (accXmlPath.exists()) {
						// System.out.println("acc::" + acc);

						String accXmlPathStr = accXmlPath.toString();
						InsiderParser parser = new InsiderParser();
						// "yes" is b/c parsing locally
						parser.getInsider(accXmlPathStr, acc, "yes");
						InsiderParser.loadIntoMysql();

					}
				}
			}
			rdrInsider.close();
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			try {
				rdrInsider.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws SocketException, IOException,
			ParseException {

		// try {
		// InsiderParser parser = new InsiderParser();
		// parser.getInsider("c:/backtest/insider/2007/QTR4/0000002969-07-000149.XML",
		// "0000002969-07-000149", "no");
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
//		 XbrlDownloader.createTmpMySqlTablestoParseXML();
		
		@SuppressWarnings("resource")
		Scanner Scan = new Scanner(System.in);
		System.out
				.println("Enter start date of insider filing to re-parse from local drive in yyyymmdd format");

		String startDateStr = Scan.nextLine();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date sDate = new Date();
		sDate = sdf.parse(startDateStr);
		Calendar startDate = Calendar.getInstance();
		startDate.setTime(sDate);

		System.out
				.println("Enter end date of insider filing to re-parse from local drive in yyyymmdd format");
		String endDateStr = Scan.nextLine();
		Date eDate = new Date();
		eDate = sdf.parse(endDateStr);
		Calendar endDate = Calendar.getInstance();
		endDate.setTime(eDate);
		String earliestDateStr = "20030401";
		Date firstDate = sdf.parse(earliestDateStr);
		Calendar badDate = Calendar.getInstance();
		badDate.setTime(firstDate);

		if (endDate.before(startDate)) {
			System.out
					.println("End date must be later than start date. Please re-enter.");
			return;
		}
		if (endDate.after(Calendar.getInstance())) {
			System.out
					.println("End date cannot be later than today. Please re-enter.");
			return;
		}
		if (endDate.before(badDate) || startDate.before(badDate)) {
			System.out
					.println("End date and start date must be later than 20030601. Please re-enter.");
			return;
		}
		dateRangeQuarters(startDate, endDate);
	}

}
