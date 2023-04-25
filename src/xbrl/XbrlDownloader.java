package xbrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;

//THIS PROGRAM DOWNLOADS XBRRL FILINGS AND PARSES THEM INTO MYSQL TABLES.
//THIS PARSER DOES NOT PARSE TEXT BLOCKS OR INS_DATA ELEMENT VALUES THAT ARE HTML

public class XbrlDownloader {

	public static String qQtr = "";
	public static String yYear = "";

	public static String baseFolder = "c:/backtest/xbrl/";
	public static boolean generateFinancials = true;

	// b/c public static this variable can be used

	public static String getFolderForDate(Calendar date) {
		return baseFolder + "/" + date.get(Calendar.YEAR) + "/QTR"
				+ getQuarter(date);
	}

	public static int getQuarter(Calendar date) {
		return ((date.get(Calendar.MONTH) / 3) + 1);
	}

	public static String getPageHtml(String pageUrl) throws IOException {
		EasyHttpClient httpClient = new EasyHttpClient(false);
		return httpClient.makeHttpRequest("Get", pageUrl, null, -1, null, null);
	}

	public static boolean isCurrentQuarter(Calendar date) {
		// determines today's qtr/ year.
		int todayQtr = getQuarter(Calendar.getInstance());
		int qtr = getQuarter(date);
		int todayYear = Calendar.getInstance().get(Calendar.YEAR);
		int year = date.get(Calendar.YEAR);

		return (todayQtr == qtr && todayYear == year);
	}

	public static void dateRangeQuarters(Calendar startDate, Calendar endDate)
			throws SocketException, IOException, SQLException {

		int startYear = startDate.get(Calendar.YEAR);
		int endYear = endDate.get(Calendar.YEAR);
		int startQtr = getQuarter(startDate);
		int endQtr = getQuarter(endDate);
		// total # of loops=totalQtrs.
		int QtrYrs = (endYear - startYear) * 4;
		int iQtr = (endQtr - startQtr) + 1;
		int totalQtrs = QtrYrs + iQtr;
		int startYr = startYear;
		iQtr = startQtr;
		Calendar cal = Calendar.getInstance();

		XbrlDownloader.moveToXbrlPermanentTable(startYr + "", iQtr + "");
		dropTmpMySqlTablestoParseXML(startYr + "", iQtr + "");
		XbrlLocalFileParser.createXbrlTmp_Tables(startYr + "", iQtr + "");

		for (int i = 1; i <= totalQtrs; i++) {

			cal.set(Calendar.YEAR, startYr);
			cal.set(Calendar.MONTH, (iQtr * 3) - 1);

			XbrlLocalFileParser.createXbrlTmp_Tables(startYr + "", iQtr + "");
			String query = createTmpXbrlMySqlTablestoParseXML(startYr + "",
					iQtr + "");

			try {
				MysqlConnUtils.executeQuery(query);
			} catch (SQLException e) {
				e.printStackTrace(System.out);
			}

			downloadXbrlIdx(startYr, iQtr, cal);
			String localPath = baseFolder + "/" + startYr + "/QTR" + iQtr;

			String mysqlLocalPath = localPath + "/xbrl.csv";
			File f = new File(mysqlLocalPath);
			if (f.exists()) {
				f.delete();
				XbrlLocalFileParser.createMysqlCsv(mysqlLocalPath, startYr,
						iQtr);
			} else if (!f.exists()) {
				XbrlLocalFileParser.createMysqlCsv(mysqlLocalPath, startYr,
						iQtr);
			}

			yYear = startYr + "";
			qQtr = iQtr + "";
			System.out.println("download xbrl zip filing!!");
			System.out.println("localPath=" + localPath);
			downloadXbrlZipFiling(localPath);
			// BELOW CREATES THE LINE ITEM F/S
			XbrlDownloader.moveToXbrlPermanentTable(startYr + "", iQtr + "");
			dropTmpMySqlTablestoParseXML(startYr + "", iQtr + "");
			
			FinancialStatement.updateXbrlFilerInfo(localPath);
			if (generateFinancials) {
//				FinancialStatement.getFinancials(localPath);
			}
			
			MysqlConnUtils.executeQuery("DROP TABLE IF EXISTS `tmp_" + startYr
					+ "q" + iQtr + "xbrl_cal_arc`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + startYr + "q" + iQtr
					+ "xbrl_cal_link`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ startYr + "q" + iQtr + "xbrl_cal_loc`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + startYr + "q" + iQtr
					+ "xbrl_cal_roleref`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ startYr + "q" + iQtr + "xbrl_def_arc`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + startYr + "q" + iQtr
					+ "xbrl_def_arcrole`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ startYr + "q" + iQtr + "xbrl_def_link`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + startYr + "q" + iQtr
					+ "xbrl_def_loc`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ startYr + "q" + iQtr + "xbrl_def_roleref`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + startYr + "q" + iQtr
					+ "xbrl_ins_context`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ startYr + "q" + iQtr + "xbrl_ins_data`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + startYr + "q" + iQtr
					+ "xbrl_ins_qatr`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ startYr + "q" + iQtr + "xbrl_ins_text`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + startYr + "q" + iQtr
					+ "xbrl_ins_unit`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ startYr + "q" + iQtr + "xbrl_lab_arc`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + startYr + "q" + iQtr
					+ "xbrl_lab_lab`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ startYr + "q" + iQtr + "xbrl_lab_link`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + startYr + "q" + iQtr
					+ "xbrl_lab_loc`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ startYr + "q" + iQtr + "xbrl_lab_roleref`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + startYr + "q" + iQtr
					+ "xbrl_pre_arc`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ startYr + "q" + iQtr + "xbrl_pre_link`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + startYr + "q" + iQtr
					+ "xbrl_pre_loc`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ startYr + "q" + iQtr + "xbrl_pre_roleref`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + startYr + "q" + iQtr
					+ "xbrl_xsd_roletype`;\r");
			iQtr++;
			if (iQtr > 4) {
				startYr++;
				iQtr = 1;
			}
		}
	}

	public static void rssZip(String rssLocalPath) throws SQLException, FileNotFoundException {

		new SAXParserRSS(rssLocalPath + "/allxbrlrss.xml");
		String file = rssLocalPath + "/allxbrlrss.txt";
		// System.out.println(file);
		Calendar acceptedDate1 = Calendar.getInstance();
		String acceptedDate = "";
		BufferedReader rdr = null;

		try {
			rdr = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		String line;
		try {
			while ((line = rdr.readLine()) != null) {
				String[] items = line.split("\\|\\|");
				if (items[0].startsWith("http")
						&& (items[2].contains("10-Q") || items[2]
								.contains("10-K"))) {
					String fileName = items[0];
					acceptedDate = items[1].substring(0, 8);
					String acceptedDateStr = items[1].substring(0, 14);

					String formType = items[2];
					String accessionNumberZipFileName = fileName.substring(
							fileName.lastIndexOf("/") + 1, fileName.length());
					String accessionNumber = accessionNumberZipFileName
							.substring(0,
									accessionNumberZipFileName.lastIndexOf("-"));
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

					Date aDate = new Date();
					aDate = sdf.parse(acceptedDate);
					// Calendar acceptedDate1 = Calendar.getInstance();
					acceptedDate1.setTime(aDate);

					String localPath = getFolderForDate(acceptedDate1);
					FileSystemUtils.createFoldersIfReqd(localPath);

					File f = new File(localPath + "/"
							+ accessionNumberZipFileName);
					String wwwSecGovPath = fileName.substring(28,
							fileName.length());
					// System.out.println("wwwSecGovPath: " + wwwSecGovPath);
					String fn = accessionNumber + ".txt";

					if (!f.exists()
							|| !ZipUtils.isTarGzipFileValid(localPath + "/"
									+ accessionNumberZipFileName)) {
						PrintWriter writer = new PrintWriter(localPath + "/"
								+ fn);

						try {
							Xbrl.download(wwwSecGovPath, localPath,
									accessionNumberZipFileName);
						} catch (Exception e1) {
							e1.printStackTrace();
							continue;
						}

						try {
							List<String> files2Parse = ZipUtils
									.deflateZipFilewithAccNo(localPath + "/"
											+ accessionNumberZipFileName,
											localPath, accessionNumber);

							String year = acceptedDate.substring(0, 4);
							int qtr = (Integer.parseInt(acceptedDate.substring(
									4, 6).replaceAll("^0", "")) + 2) / 3;
							String q = qtr + "";

							System.out.println("create tmpXbrl. year=" + year
									+ " q=" + q);

							MysqlConnUtils.executeQuery(XbrlDownloader.createTmpXbrlMySqlTablestoParseXML(
									year, q));

							for (String file2parse : files2Parse) {
								if (file2parse.endsWith("_pre.xml")
										&& file2parse.contains(accessionNumber)) {
									File f2 = new File(localPath + "/"
											+ file2parse);
									String path = f2.getPath();
									String ticker = path.substring(
											path.lastIndexOf("\\") + 22,
											path.lastIndexOf("-"));
									writer.println(acceptedDateStr + "||"
											+ ticker);
									writer.close();
									@SuppressWarnings("unused")
									SAXParserPre sxp = new SAXParserPre(
											localPath
													+ "/"
													+ file2parse.replace("\\",
															"/"),
											acceptedDateStr, formType, ticker,
											yYear, qQtr);

									XbrlLocalFileParser.deleteFile(localPath,
											file2parse);

								} else if (!file2parse.endsWith("_pre.xml")
										&& !file2parse.endsWith("_cal.xml")
										&& !file2parse.endsWith("_def.xml")
										&& !file2parse.endsWith("_lab.xml")
										&& !file2parse.endsWith(".xsd")
										&& !file2parse.endsWith("ref.xml")
										&& !file2parse.contains("defnref")
										&& file2parse.endsWith(".xml")) {

									System.out
											.println("calling SAXParserIns. acceptedDate="
													+ acceptedDate);
									SAXParserIns sxc = new SAXParserIns(
											localPath
													+ "/"
													+ file2parse.replace("\\",
															"/"),
											acceptedDateStr, yYear, qQtr);
									XbrlLocalFileParser.deleteFile(localPath,
											file2parse);

								} else if (file2parse.endsWith("_cal.xml")
										&& file2parse.contains(accessionNumber)) {
									@SuppressWarnings("unused")
									SAXParserCal sxc = new SAXParserCal(
											localPath
													+ "/"
													+ file2parse.replace("\\",
															"/"),
											acceptedDateStr, yYear, qQtr);
									XbrlLocalFileParser.deleteFile(localPath,
											file2parse);
								} else if (file2parse.endsWith("_lab.xml")
										&& file2parse.contains(accessionNumber)) {
									@SuppressWarnings("unused")
									SAXParserLab sxc = new SAXParserLab(
											localPath
													+ "/"
													+ file2parse.replace("\\",
															"/"),
											acceptedDateStr, yYear, qQtr);
									XbrlLocalFileParser.deleteFile(localPath,
											file2parse);
								} else if (file2parse.endsWith("_def.xml")
										&& file2parse.contains(accessionNumber)) {
									@SuppressWarnings("unused")
									SAXParserDef sxc = new SAXParserDef(
											localPath
													+ "/"
													+ file2parse.replace("\\",
															"/"),
											acceptedDateStr, yYear, qQtr);
									XbrlLocalFileParser.deleteFile(localPath,
											file2parse);
								} else if (file2parse.endsWith(".xsd")) {
									@SuppressWarnings("unused")
									// XSD must be final SAXParser
									SAXParserXSD sxc = new SAXParserXSD(
											localPath
													+ "/"
													+ file2parse.replace("\\",
															"/"),
											acceptedDateStr, yYear, qQtr);
									XbrlLocalFileParser.deleteFile(localPath,
											file2parse);
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
						getXbrlTable(acceptedDate);
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			rdr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String dateStr = acceptedDate.replace("-", "");
		String yr = dateStr.substring(0, 4);
		// System.out.println("yr:: " + year);
		String month = dateStr.substring(4, 6);
		int mo = Integer.parseInt(month);
		int quarter = ((mo - 1) / 3) + 1;

		XbrlDownloader.moveToXbrlPermanentTable(yr + "", quarter + "");
		String localPath = getFolderForDate(acceptedDate1);
		FinancialStatement.updateXbrlFilerInfo(localPath);
//		FinancialStatement.getFinancials(localPath);
	}

	public static void moveToXbrlPermanentTable(String year, String qtr)
			throws SQLException, FileNotFoundException {
		System.out.println("move to perm xbrl table");

		Connection conn = MysqlConnUtils.getConnection();
		DatabaseMetaData metadata = conn.getMetaData();
		ResultSet rs;
		System.out.println("tmp_" + year + "Q" + qtr
				+ "xbrl_CAL_ARC");
		rs = metadata.getTables(null, null, "tmp_"+year+"Q"+qtr+"xbrl_ins_data", null);

		// if table does not exists (there is no rs.next) return true.
		// if last date of tableToCheck not equal to sourceTable return
		// true.
		
		// System.out.println("rs.next="+rs.next());
		// rs.next cannot be used to print! else it goes to the next table which does
		// not exist!

		if (rs.next()) {
			System.out.println("moving to perm xbrl files");
			String qry = "\rINSERT IGNORE INTO " + year + "Q" + qtr
					+ "XBRL_CAL_ARC \rselect * from tmp_" + year + "Q" + qtr
					+ "xbrl_CAL_ARC t1; \rINSERT IGNORE INTO " + year + "Q"
					+ qtr + "XBRL_cal_Link \rselect * from tmp_" + year + "Q"
					+ qtr + "xbrl_cal_Link t1; \rINSERT IGNORE INTO " + year
					+ "Q" + qtr + "XBRL_cal_Loc \rselect * from tmp_" + year
					+ "Q" + qtr + "xbrl_cal_Loc t1; \rINSERT IGNORE INTO "
					+ year + "Q" + qtr
					+ "XBRL_cal_RoleRef \rselect * from tmp_" + year + "Q"
					+ qtr + "xbrl_cal_RoleRef t1; \rINSERT IGNORE INTO " + year
					+ "Q" + qtr + "XBRL_def_arc \rselect * from tmp_" + year
					+ "Q" + qtr + "xbrl_def_arc t1; \rINSERT IGNORE INTO "
					+ year + "Q" + qtr
					+ "XBRL_def_arcrole \rselect * from tmp_" + year + "Q"
					+ qtr + "xbrl_def_arcrole t1; \rINSERT IGNORE INTO " + year
					+ "Q" + qtr + "XBRL_def_Link \rselect * from tmp_" + year
					+ "Q" + qtr + "xbrl_def_Link t1; \rINSERT IGNORE INTO "
					+ year + "Q" + qtr + "XBRL_def_Loc \rselect * from tmp_"
					+ year + "Q" + qtr
					+ "xbrl_def_Loc t1; \rINSERT IGNORE INTO " + year + "Q"
					+ qtr + "XBRL_def_RoleRef \rselect * from tmp_" + year
					+ "Q" + qtr + "xbrl_def_RoleRef t1; \rINSERT IGNORE INTO "
					+ year + "Q" + qtr
					+ "XBRL_ins_context \rselect * from tmp_" + year + "Q"
					+ qtr + "xbrl_ins_context t1; \rINSERT IGNORE INTO " + year
					+ "Q" + qtr + "XBRL_ins_data \rselect * from tmp_" + year
					+ "Q" + qtr + "xbrl_ins_data t1; \rINSERT IGNORE INTO "
					+ year + "Q" + qtr + "XBRL_LAB_arc \rselect * from tmp_"
					+ year + "Q" + qtr
					+ "xbrl_LAB_arc t1; \rINSERT IGNORE INTO " + year + "Q"
					+ qtr + "XBRL_lab_lab \rselect * from tmp_" + year + "Q"
					+ qtr + "xbrl_lab_lab t1; \rINSERT IGNORE INTO " + year
					+ "Q" + qtr + "XBRL_lab_Link \rselect * from tmp_" + year
					+ "Q" + qtr + "xbrl_lab_Link t1; \rINSERT IGNORE INTO "
					+ year + "Q" + qtr + "XBRL_lab_Loc \rselect * from tmp_"
					+ year + "Q" + qtr
					+ "xbrl_lab_Loc t1; \rINSERT IGNORE INTO " + year + "Q"
					+ qtr + "XBRL_lab_RoleRef \rselect * from tmp_" + year
					+ "Q" + qtr + "xbrl_lab_RoleRef t1; \rINSERT IGNORE INTO "
					+ year + "Q" + qtr + "XBRL_pre_arc \rselect * from tmp_"
					+ year + "Q" + qtr
					+ "xbrl_pre_arc t1; \rINSERT IGNORE INTO " + year + "Q"
					+ qtr + "XBRL_pre_Link \rselect * from tmp_" + year + "Q"
					+ qtr + "xbrl_pre_Link t1; \rINSERT IGNORE INTO " + year
					+ "Q" + qtr + "XBRL_pre_Loc \rselect * from tmp_" + year
					+ "Q" + qtr + "xbrl_pre_Loc t1; \rINSERT IGNORE INTO "
					+ year + "Q" + qtr
					+ "XBRL_pre_RoleRef \rselect * from tmp_" + year + "Q"
					+ qtr + "xbrl_pre_RoleRef t1; \rINSERT IGNORE INTO " + year
					+ "Q" + qtr + "XBRL_xsd_roletype \rselect * from tmp_"
					+ year + "Q" + qtr
					+ "xbrl_xsd_roletype t1; \rINSERT IGNORE INTO " + year
					+ "Q" + qtr + "XBRL_ins_unit \rselect * from tmp_" + year
					+ "Q" + qtr + "xbrl_ins_unit t1; ";

			try {
				MysqlConnUtils.executeQuery(qry);
			} catch (SQLException e) {
				e.printStackTrace(System.out);
			}
		}

		 String qry = "DROP TABLE IF EXISTS tmp_" + year + "Q" + qtr
				+ "xbrl_CAL_ARC; \r\n" + "DROP TABLE IF EXISTS tmp_" + year
				+ "Q" + qtr + "xbrl_cal_Link; \r\n"
				+ "DROP TABLE IF EXISTS tmp_" + year + "Q" + qtr
				+ "xbrl_cal_Loc; \r\n" + "DROP TABLE IF EXISTS tmp_" + year
				+ "Q" + qtr + "xbrl_cal_RoleRef; \r\n"
				+ "DROP TABLE IF EXISTS tmp_" + year + "Q" + qtr
				+ "xbrl_def_arc; \r\n" + "DROP TABLE IF EXISTS tmp_" + year
				+ "Q" + qtr + "xbrl_def_arcrole; \r\n"
				+ "DROP TABLE IF EXISTS tmp_" + year + "Q" + qtr
				+ "xbrl_def_Link; \r\n" + "DROP TABLE IF EXISTS tmp_" + year
				+ "Q" + qtr + "xbrl_def_Loc; \r\n"
				+ "DROP TABLE IF EXISTS tmp_" + year + "Q" + qtr
				+ "xbrl_def_RoleRef; \r\n" + "DROP TABLE IF EXISTS tmp_" + year
				+ "Q" + qtr + "xbrl_ins_context; \r\n"
				+ "DROP TABLE IF EXISTS tmp_" + year + "Q" + qtr
				+ "xbrl_ins_data; \r\n" + "DROP TABLE IF EXISTS tmp_" + year
				+ "Q" + qtr + "xbrl_LAB_arc; \r\n"
				+ "DROP TABLE IF EXISTS tmp_" + year + "Q" + qtr
				+ "xbrl_lab_lab; \r\n" + "DROP TABLE IF EXISTS tmp_" + year
				+ "Q" + qtr + "xbrl_lab_Link; \r\n"
				+ "DROP TABLE IF EXISTS tmp_" + year + "Q" + qtr
				+ "xbrl_lab_Loc; \r\n" + "DROP TABLE IF EXISTS tmp_" + year
				+ "Q" + qtr + "xbrl_lab_RoleRef; \r\n"
				+ "DROP TABLE IF EXISTS tmp_" + year + "Q" + qtr
				+ "xbrl_pre_arc; \r\n" + "DROP TABLE IF EXISTS tmp_" + year
				+ "Q" + qtr + "xbrl_pre_Link; \r\n"
				+ "DROP TABLE IF EXISTS tmp_" + year + "Q" + qtr
				+ "xbrl_pre_Loc; \r\n" + "DROP TABLE IF EXISTS tmp_" + year
				+ "Q" + qtr + "xbrl_pre_RoleRef; \r\n"
				+ "DROP TABLE IF EXISTS tmp_" + year + "Q" + qtr
				+ "xbrl_xsd_roletype; \r\n" + "DROP TABLE IF EXISTS tmp_"
				+ year + "Q" + qtr + "xbrl_ins_unit; \r\n";
	}

	public static void getXbrlTable(String acceptedDate) throws FileNotFoundException {
		 System.out.println("a/d::" + acceptedDate);
		 String dateStr = acceptedDate.replace("-", "");
		 String year = dateStr.substring(0, 4);
		 System.out.println("yr:: " + year);
		 String month = dateStr.substring(4, 6);
		 int mo = Integer.parseInt(month);
		 int qtr = ((mo - 1) / 3) + 1;
		 System.out.println("year: " + year + " qtr: " + qtr);

		if (yYear.length() < 4) {
			yYear = year;
			qQtr = qtr + "";
		}

		String query = "\rINSERT IGNORE INTO TMP_" + yYear + "Q" + qQtr
				+ "XBRL_CAL_ARC \rselect * from TMPxbrl_" + yYear + "Q" + qQtr
				+ "_CAL_ARC t1; \rINSERT IGNORE INTO TMP_" + yYear + "Q" + qQtr
				+ "XBRL_cal_Link \rselect * from TMPxbrl_" + yYear + "Q" + qQtr
				+ "_cal_Link t1; \rINSERT IGNORE INTO TMP_" + yYear + "Q" + qQtr
				+ "XBRL_cal_Loc \rselect * from TMPxbrl_" + yYear + "Q" + qQtr
				+ "_cal_Loc t1; \rINSERT IGNORE INTO TMP_" + yYear + "Q" + qQtr
				+ "XBRL_cal_RoleRef \rselect * from TMPxbrl_" + yYear + "Q"
				+ qQtr + "_cal_RoleRef t1; \rINSERT IGNORE INTO TMP_" + yYear
				+ "Q" + qQtr + "XBRL_def_arc \rselect * from TMPxbrl_" + yYear
				+ "Q" + qQtr + "_def_arc t1; \rINSERT IGNORE INTO TMP_" + yYear
				+ "Q" + qQtr + "XBRL_def_arcrole \rselect * from TMPxbrl_"
				+ yYear + "Q" + qQtr
				+ "_def_arcrole t1; \rINSERT IGNORE INTO TMP_" + yYear + "Q"
				+ qQtr + "XBRL_def_Link \rselect * from TMPxbrl_" + yYear + "Q"
				+ qQtr + "_def_Link t1; \rINSERT IGNORE INTO TMP_" + yYear + "Q"
				+ qQtr + "XBRL_def_Loc \rselect * from TMPxbrl_" + yYear + "Q"
				+ qQtr + "_def_Loc t1; \rINSERT IGNORE INTO TMP_" + yYear + "Q"
				+ qQtr + "XBRL_def_RoleRef \rselect * from TMPxbrl_" + yYear
				+ "Q" + qQtr + "_def_RoleRef t1; \rINSERT IGNORE INTO TMP_"
				+ yYear + "Q" + qQtr
				+ "XBRL_ins_context \rselect * from TMPxbrl_" + yYear + "Q"
				+ qQtr + "_ins_context t1; \rINSERT IGNORE INTO TMP_" + yYear
				+ "Q" + qQtr + "XBRL_ins_data \rselect * from TMPxbrl_" + yYear
				+ "Q" + qQtr + "_ins_data t1; \rINSERT IGNORE INTO TMP_" + yYear
				+ "Q" + qQtr + "XBRL_LAB_arc \rselect * from TMPxbrl_" + yYear
				+ "Q" + qQtr + "_LAB_arc t1; \rINSERT IGNORE INTO TMP_" + yYear
				+ "Q" + qQtr + "XBRL_lab_lab \rselect * from TMPxbrl_" + yYear
				+ "Q" + qQtr + "_lab_lab t1; \rINSERT IGNORE INTO TMP_" + yYear
				+ "Q" + qQtr + "XBRL_lab_Link \rselect * from TMPxbrl_" + yYear
				+ "Q" + qQtr + "_lab_Link t1; \rINSERT IGNORE INTO TMP_" + yYear
				+ "Q" + qQtr + "XBRL_lab_Loc \rselect * from TMPxbrl_" + yYear
				+ "Q" + qQtr + "_lab_Loc t1; \rINSERT IGNORE INTO TMP_" + yYear
				+ "Q" + qQtr + "XBRL_lab_RoleRef \rselect * from TMPxbrl_"
				+ yYear + "Q" + qQtr
				+ "_lab_RoleRef t1; \rINSERT IGNORE INTO TMP_" + yYear + "Q"
				+ qQtr + "XBRL_pre_arc \rselect * from TMPxbrl_" + yYear + "Q"
				+ qQtr + "_pre_arc t1; \rINSERT IGNORE INTO TMP_" + yYear + "Q"
				+ qQtr + "XBRL_pre_Link \rselect * from TMPxbrl_" + yYear + "Q"
				+ qQtr + "_pre_Link t1; \rINSERT IGNORE INTO TMP_" + yYear + "Q"
				+ qQtr + "XBRL_pre_Loc \rselect * from TMPxbrl_" + yYear + "Q"
				+ qQtr + "_pre_Loc t1; \rINSERT IGNORE INTO TMP_" + yYear + "Q"
				+ qQtr + "XBRL_pre_RoleRef \rselect * from TMPxbrl_" + yYear
				+ "Q" + qQtr + "_pre_RoleRef t1; \rINSERT IGNORE INTO TMP_"
				+ yYear + "Q" + qQtr
				+ "XBRL_xsd_roletype \rselect * from TMPxbrl_" + yYear + "Q"
				+ qQtr + "_xsd_roletype t1; \rINSERT IGNORE INTO TMP_" + yYear
				+ "Q" + qQtr + "XBRL_ins_unit \rselect * from TMPxbrl_" + yYear
				+ "Q" + qQtr + "_ins_unit t1; ";

		String query2 = createTmpXbrlMySqlTablestoParseXML(yYear, qQtr + "");

		try {
			MysqlConnUtils.executeQuery(query);
			MysqlConnUtils.executeQuery(query2);

			System.out.println("xbrl -- finished. download next");
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	public static String createTmpXbrlMySqlTablestoParseXML(String yr,
			String qtr) {

		String query = "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr
				+ "Q" + qtr + "_xsd_roletype`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_pre_roleref`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_pre_loc`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_pre_link`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_pre_arc`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_lab_loc`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_lab_roleref`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_lab_link`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_lab_lab`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_lab_arc`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_ins_context`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_ins_data`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_ins_text`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_ins_unit`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_ins_qatr`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_def_arc`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_def_arcrole`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_def_link`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_def_loc`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_def_roleref`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_cal_arc`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_cal_link`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_cal_loc`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_cal_roleref`;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr
				+ "_xsd_roletype` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `xsd_rt_roleURI` varchar(255) NOT NULL,\r\n"
				+ "  `roleURI_Role_Name` varchar(255) NOT NULL,\r\n"
				+ "  `xsd_rt_id` varchar(255) NOT NULL,\r\n"
				+ "  `xsd_rt_definition` varchar(255) DEFAULT NULL,\r\n"
				+ "  `xsd_rt_usedOnPre` varchar(70) DEFAULT NULL,\r\n"
				+ "  `xsd_rt_usedOnCal` varchar(70) DEFAULT NULL,\r\n"
				+ "  `xsd_rt_usedOnDef` varchar(70) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr
				+ "_pre_roleref` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `pre_rr_roleURI` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_rr_roleURI_Name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_rr_href` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_rr_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_rr_title` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr + "_pre_loc` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `pre_link_role_name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_Loc_label` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_Loc_href` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_Loc_href_prefix` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_Loc_href_name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_Loc_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_Loc_title` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr
				+ "_pre_link` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT NULL,\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `formType` varchar(20) DEFAULT '0',\r\n"
				+ "  `ITS` varchar(100) DEFAULT NULL,\r\n"
				+ "  `pre_link_role` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_link_role_name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_link_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_link_title` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr + "_pre_arc` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `pre_link_role_name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_arc_Parent` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_arc_Child` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_arc_preferredLabel` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_arc_pLabel` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_arc_order` double DEFAULT NULL,\r\n"
				+ "  `pre_arc_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_arc_arcRole` varchar(255) DEFAULT NULL,\r\n"
				+ "  `pre_arc_title` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr + "_lab_loc` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `lab_link_Role_Name` varchar(255) NOT NULL,\r\n"
				+ "  `lab_loc_label` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_loc_href` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_loc_href_prefix` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_loc_href_name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_loc_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_loc_title` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr
				+ "_lab_roleref` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `lab_rr_roleURI` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_rr_roleURI_Name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_rr_href` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_rr_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_rr_title` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr
				+ "_lab_link` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `lab_link_role` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_link_role_name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_Link_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_Link_title` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr + "_lab_lab` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `lab_link_Role_Name` varchar(255) NOT NULL,\r\n"
				+ "  `lab_lab_role` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_lab_value` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_lab_label` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_lab_lang` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_lab_id` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_lab_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr + "_lab_arc` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `lab_link_Role_Name` varchar(255) NOT NULL,\r\n"
				+ "  `lab_arc_Parent` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_arc_Child` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_arc_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_arc_arcRole` varchar(255) DEFAULT NULL,\r\n"
				+ "  `lab_arc_order` double DEFAULT '0',\r\n"
				+ "  `lab_arc_title` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr
				+ "_ins_context` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `id` varchar(255) NOT NULL,\r\n"
				+ "  `startDate` date DEFAULT '1901-01-01',\r\n"
				+ "  `endDate` date DEFAULT '1901-01-01',\r\n"
				+ "  `instant` date DEFAULT '1901-01-01',\r\n"
				+ "  `segment` varchar(255) DEFAULT NULL,\r\n"
				+ "  `dimension` varchar(255) DEFAULT NULL,\r\n"
				+ "  `dimensionValue` varchar(255) DEFAULT NULL,\r\n"
				+ "  `CIK` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr
				+ "_ins_data` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `prefix` varchar(255) NOT NULL,\r\n"
				+ "  `Name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `value` varchar(255) DEFAULT NULL,\r\n"
				+ "  `contextRef` varchar(255) DEFAULT NULL,\r\n"
				+ "  `unitRef` varchar(255) DEFAULT NULL,\r\n"
				+ "  `id` varchar(255) DEFAULT NULL,\r\n"
				+ "  `decimals` double DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr
				+ "_ins_text` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `prefix` varchar(255) NOT NULL,\r\n"
				+ "  `Name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `value` varchar(255) DEFAULT NULL,\r\n"
				+ "  `contextRef` varchar(255) DEFAULT NULL,\r\n"
				+ "  `unitRef` varchar(255) DEFAULT NULL,\r\n"
				+ "  `id` varchar(255) DEFAULT NULL,\r\n"
				+ "  `decimals` double DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr
				+ "_ins_unit` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `id` varchar(255) NOT NULL,\r\n"
				+ "  `measure` varchar(255) DEFAULT NULL,\r\n"
				+ "  `divide` varchar(255) DEFAULT NULL,\r\n"
				+ "  `Denominator` varchar(255) DEFAULT NULL,\r\n"
				+ "  `Numerator` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr
				+ "_ins_qatr` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `attribute` varchar(255) DEFAULT NULL,\r\n"
				+ "  `localName` varchar(255) DEFAULT NULL,\r\n"
				+ "  `qName` varchar(255) DEFAULT NULL,\r\n"
				+ "  `URI` varchar(255) DEFAULT NULL\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr + "_def_arc` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `def_link_role_name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_arc_Parent` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_arc_Child` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_arc_order` double DEFAULT NULL,\r\n"
				+ "  `def_arc_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_arc_arcRole` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_arc_title` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr
				+ "_def_arcrole` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `def_arcrr_roleURI` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_arcrr_roleURI_Name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_arcrr_href` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_arcrr_href_name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_arcrr_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_arcrr_title` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr
				+ "_def_link` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `def_link_role` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_link_role_name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_link_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_link_title` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr + "_def_loc` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `def_link_Role_Name` varchar(255) NOT NULL,\r\n"
				+ "  `def_loc_label` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_loc_href` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_loc_href_prefix` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_loc_href_name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_loc_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_loc_title` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr
				+ "_def_roleref` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `def_rr_roleURI` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_rr_roleURI_Name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_rr_href` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_rr_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  `def_rr_title` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr + "_cal_arc` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `cal_link_role_name` varchar(255) NOT NULL,\r\n"
				+ "  `cal_arc_Parent` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_arc_Child` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_arc_order` double DEFAULT NULL,\r\n"
				+ "  `cal_arc_Weight` double DEFAULT NULL,\r\n"
				+ "  `cal_arc_Priority` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_arc_Use` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_arc_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_arc_arcRole` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_arc_title` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr
				+ "_cal_link` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `cal_link_role` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_link_role_name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_link_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_link_title` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr + "_cal_loc` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `cal_link_role_name` varchar(255) NOT NULL,\r\n"
				+ "  `cal_loc_label` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_loc_href` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_loc_href_prefix` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_loc_href_name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_loc_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_loc_title` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "CREATE TABLE `TMPxbrl_" + yr + "Q" + qtr
				+ "_cal_roleref` (\r\n"
				+ "  `rowNo` double NOT NULL DEFAULT '0',\r\n"
				+ "  `accNo` varchar(20) DEFAULT '0',\r\n"
				+ "  `acceptedDate` datetime DEFAULT NULL,\r\n"
				+ "  `cal_rr_roleURI` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_rr_roleURI_Name` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_rr_href` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_rr_type` varchar(255) DEFAULT NULL,\r\n"
				+ "  `cal_rr_title` varchar(255) DEFAULT NULL,\r\n"
				+ "  KEY `accNo` (`accNo`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;";

		return query;

	}

	public static void downloadXbrlZipFiling(String localPath) {

		String file = localPath + "/xbrl.csv";
		System.out.println("reading csv file=" + file);
		BufferedReader rdr = null;
		try {
			rdr = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		String line;
		try {
			while ((line = rdr.readLine()) != null) {
				String[] items = line.split("\\|\\|");
				if (items.length < 5)
					continue;
				if (items[4].contains("edgar") && items[2].contains("10-")) {
					String formType = items[2];
					String fileDate = items[3];

					String year = fileDate.substring(0, 4);
					int qtr = (Integer.parseInt(fileDate.replaceAll("-", "")
							.substring(4, 6).replaceAll("^0", "")) + 2) / 3;
					String q = qtr + "";

					String fileName = items[4];
					String accNo = fileName.substring(
							fileName.lastIndexOf("/") + 1,
							fileName.lastIndexOf("."));
					String accNoZipFileName = accNo + "-xbrl.zip";
					String accNoNoHyphens = accNo.replace("-", "");
					File f = new File(localPath + "/" + accNoZipFileName);
					String suffix = fileName.substring(0,
							fileName.lastIndexOf("/"));
					String wwwSecGovPath = (suffix + "/" + accNoNoHyphens + "/" + accNoZipFileName);
					String wwwSecGovPathHtm = (suffix + "/" + accNoNoHyphens
							+ "/" + accNo + "-index.htm");
					System.out.println("wwwSecGovPath=" + wwwSecGovPath
							+ " localPath=" + localPath + " accNoZipFileName="
							+ accNoZipFileName);

					System.out.println("xbrl zip file=" + f.getAbsolutePath()
							+ " xbrl zip file.size=" + f.length());
					// if (!f.exists() || f.length()<1100) {
					System.out.println("downloading");
					// replaced below with HttpDownloadUtility - not sure if it
					// works
					// Xbrl.download(wwwSecGovPath, localPath,
					// accNoZipFileName);
					HttpDownloadUtility.downloadFile(
							"https://www.sec.gov/Archives/" + wwwSecGovPath,
							localPath);
					// this downloads acceptedDate etc.
					if (!f.exists())
						continue;
					XBRLInfoSecGov info = new XBRLInfoSecGov(
							"https://www.sec.gov/Archives/" + wwwSecGovPathHtm);
					String acceptedDate = (info.acceptedDate);
					String fn = accNo + ".txt";
					System.out.println("info=" + wwwSecGovPathHtm);
					boolean foundInstanceFile = false;
					try {
						
						MysqlConnUtils.executeQuery(createTmpXbrlMySqlTablestoParseXML(yYear, qQtr));
						
						List<String> files2Parse = ZipUtils
								.deflateZipFilewithAccNo(localPath + "/"
										+ accNoZipFileName, localPath, accNo);

						for (String file2parse : files2Parse) {
							if (file2parse.endsWith("_pre.xml")) {
								System.out
										.println("getting ticker from xbrl unzipped file name");
								PrintWriter writer = new PrintWriter(localPath
										+ "/" + fn);
								File f2 = new File(localPath + "/" + file2parse);
								String path = f2.getPath();
								String ticker = path.substring(
										path.lastIndexOf("\\") + 22,
										path.lastIndexOf("-"));
								writer.println(acceptedDate + "||" + ticker);
								writer.close();

								@SuppressWarnings("unused")
								SAXParserPre sxp = new SAXParserPre(localPath
										+ "/" + file2parse.replace("\\", "/"),
										acceptedDate, formType, ticker, yYear,
										qQtr);

								XbrlLocalFileParser.deleteFile(localPath,
										file2parse);

							} else if (!file2parse.endsWith("_pre.xml")
									&& !file2parse.endsWith("_cal.xml")
									&& !file2parse.endsWith("_def.xml")
									&& !file2parse.endsWith("_lab.xml")
									&& !file2parse.endsWith(".xsd")
									&& !file2parse.endsWith("ref.xml")
									&& !file2parse.contains("defnref")
									&& file2parse.endsWith(".xml")) {
								
								foundInstanceFile = true;

								@SuppressWarnings("unused")
								SAXParserIns sxc = new SAXParserIns(localPath
										+ "/" + file2parse.replace("\\", "/"),
										acceptedDate, yYear, qQtr);

								XbrlLocalFileParser.deleteFile(localPath,
										file2parse);
							} else if (file2parse.endsWith("_cal.xml")) {
								@SuppressWarnings("unused")
								SAXParserCal sxc = new SAXParserCal(

										localPath + "/"
												+ file2parse.replace("\\", "/"),
										acceptedDate, yYear, qQtr);

								XbrlLocalFileParser.deleteFile(localPath,
										file2parse);
							} else if (file2parse.endsWith("_lab.xml")) {
								@SuppressWarnings("unused")
								SAXParserLab sxc = new SAXParserLab(

										localPath + "/"
												+ file2parse.replace("\\", "/"),
										acceptedDate, year, q);

								File f2 = new File(localPath + "/" + file2parse);
								f2.delete();

							} else if (file2parse.endsWith("_def.xml")) {
								@SuppressWarnings("unused")
								SAXParserDef sxc = new SAXParserDef(localPath
										+ "/" + file2parse.replace("\\", "/"),
										acceptedDate, yYear, qQtr);

								XbrlLocalFileParser.deleteFile(localPath,
										file2parse);

							} else if (file2parse.endsWith(".xsd")) {
								@SuppressWarnings("unused")
								// XSD must be final SAXParser
								SAXParserXSD sxc = new SAXParserXSD(localPath
										+ "/" + file2parse.replace("\\", "/"),
										acceptedDate, yYear, qQtr);

								XbrlLocalFileParser.deleteFile(localPath,
										file2parse);
							}
						}
						
						
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					if(!foundInstanceFile){
						System.out.println("did not find instance file in acc xbrl zip file ");
						// download extracted instance from sec site
						// rename instance doc to be accno_filename
						// parse with saxParserIns
						// 1st go to :
						// https://www.sec.gov/Archives/edgar/data/78003/000007800317000039/0000078003-17-000039-index.htm
						// then download link at tab after
						// "XBRL INSTANCE DOCUMENT" text
						// https://www.sec.gov/Archives/edgar/data/78003/000007800317000039/pfe-07022017x10q_htm.xml
						

						System.out.println("rssDownload");
						Rss.download(
								"https://www.sec.gov/Archives/edgar/data/78003/000007800317000039/"
										+ accNo + "-index.htm", localPath+"/"+"secGov"+accNo);
						System.out.println("main url download complete");

						// reads file now with all the links
						String readFrom = Utils.readTextFromFile(localPath
								+ "/" + "secGov" + accNo);
						Pattern patternExtractedInstanceLink = Pattern
								.compile("(?sm)(?<=INSTANCE DOCUMENT.{1,150})/Archives/edgar/data/.{1,125}\\.xml(?=\")");
						Matcher match = patternExtractedInstanceLink.matcher(readFrom);
						if (match.find()) {
							System.out.println("match.grp=" + match.group()
									+ " accNo=" + accNo);
							String filenm = localPath
									+ "/"
									+ accNo
									+ "_"
									+ match.group().substring(
											match.group().lastIndexOf("/") + 1);
							System.out.println("filenm=" + filenm);

							//							localFilename=c:/backtest/xbrl//2017/QTR3/45
							Rss.download("https://www.sec.gov"+match.group(), filenm);

							SAXParserIns sxc = new SAXParserIns(filenm,
									acceptedDate, yYear, qQtr);

						}
					}
					
					getXbrlTable(acceptedDate);
				}
			}
		} catch (IOException e) {

			e.printStackTrace();
		} catch (Exception e1) {

			e1.printStackTrace();
		}
		try {
			rdr.close();
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	public static void downloadXbrlIdx(int year, int qtr, Calendar endDate)
			throws SocketException, IOException {

		String localPath = baseFolder + "/" + year + "/QTR" + qtr, str = "";
		FileSystemUtils.createFoldersIfReqd(localPath);
		File f = new File(localPath + "/xbrl.idx");
		if (f.exists() // && isCurrentQuarter(endDate)
		)

		{
			f.delete();
			XbrlIdx.download(year, qtr, localPath);
			ZipUtils.deflateZipFile(localPath + "/xbrl.zip", localPath);
		}
		
		if (!f.exists()) {
			XbrlIdx.download(year, qtr, localPath);
			ZipUtils.deflateZipFile(localPath + "/xbrl.zip", localPath);
		}
		
		if(f.exists()){
			str = Utils.readTextFromFile(f.getAbsolutePath()).replaceAll("\\\\", " ");
			File f2 = new File(localPath +"/xbrl2.idx");
			PrintWriter pw = new PrintWriter(f2);
			pw.println(str);
			pw.close();
			f.delete();
			f2.renameTo(f);
		}
	}

	/*
	 * public static void createTmpMySqlTablestoParseXML() {
	 * 
	 * String query =
	 * "call dropTmpMySqlTablestoParseXML();\rcall createTmpMySqlTablestoParseXML();"
	 * ; try { MysqlConnUtils.executeQuery(query); } catch (SQLException e) {
	 * e.printStackTrace(System.out); } }
	 */
	
	public static void dropTmpMySqlTablestoParseXML(String yr, String qtr) throws FileNotFoundException {

		String query = "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr
				+ "Q" + qtr + "_xsd_roletype`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_pre_roleref`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_pre_loc`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_pre_link`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_pre_arc`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_lab_loc`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_lab_roleref`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_lab_link`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_lab_lab`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_lab_arc`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_ins_context`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_ins_data`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_ins_text`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_ins_unit`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_ins_qatr`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_def_arc`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_def_arcrole`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_def_link`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_def_loc`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_def_roleref`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_cal_arc`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_cal_link`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_cal_loc`;\r\n"
				+ "DROP TABLE IF EXISTS`stockanalyser`.`TMPxbrl_" + yr + "Q"
				+ qtr + "_cal_roleref`;\r\n" + "\r\n";

		try {
			MysqlConnUtils.executeQuery(query);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	public static void main(String[] args) throws ParseException,
			SocketException, IOException, SQLException {
		

		int qtr = 2;//chg Q also**
		int startYr = 2018, endYr = startYr;
		String startDateStr = startYr+"0"+((qtr*3)+"01");
		// System.out.println(startDateStr.length());
		if(startDateStr.length()==9)
			startDateStr =startYr+((qtr*3)+"01");
		String endDateStr = startDateStr;
		System.out.println("startDate=" + startDateStr + " endDate=" + endDateStr);

		generateFinancials = true;
		Scanner Scan = new Scanner(System.in);
		
		/*
		//#1

		@SuppressWarnings("resource")
		
		System.out.println("Do you want to update XBRL via RSS (y/n)? Enter N for to enter specific time period");
		String rssYN = Scan.nextLine();
		

		if (rssYN.equalsIgnoreCase("y")) {

			Calendar st = Calendar.getInstance();
			String localPath = getFolderForDate(st);
			System.out.println("LOCALPATH:: " + localPath);
			String year = localPath.substring(localPath.toLowerCase()
					.lastIndexOf("xbrl/") + 6, localPath.toLowerCase()
					.lastIndexOf("xbrl/") + 10);
			String qtr = localPath.toLowerCase().substring(
					localPath.toLowerCase().lastIndexOf("qtr") + 3,
					localPath.toLowerCase().lastIndexOf("qtr") + 4);

			String query = createTmpXbrlMySqlTablestoParseXML(year, qtr);
			MysqlConnUtils.executeQuery(query);
			XbrlLocalFileParser.createXbrlTmp_Tables(year + "", qtr + "");

			System.out.println("YN: " + rssYN);
			Rss.download("https://www.sec.gov/Archives/edgar/xbrlrss.all.xml",
					"c:/backtest/xbrl/rss/allxbrlrss.xml");
			rssZip("c:/backtest/xbrl/rss");

			XbrlDownloader.moveToXbrlPermanentTable(year, qtr);
//			FinancialStatement.updateXbrlFilerInfo(localPath);
//			FinancialStatement.getFinancials(localPath);
			// dropTmpMySqlTablestoParseXML();

		} else if (rssYN.equalsIgnoreCase("n")) {
			System.out
					.println("Enter start date of xbrl to download in yyyymmdd format or 'ENTER' for current period");

			startDateStr = Scan.nextLine();
			if(startDateStr.length()<1){
				// startDateStr = (Calendar.getInstance().get(Calendar.YEAR) +
				// ""
				// + (Calendar.getInstance().get(Calendar.MONTH) + 1) + "" +
				// Calendar
				// .getInstance().get(Calendar.DAY_OF_MONTH)) + "";
			}

		System.out
				.println("Enter end date of xbrl to download in yyyymmdd format");
		endDateStr = Scan.nextLine();
*/			


			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			Date sDate = new Date();
			sDate = sdf.parse(startDateStr);
			Calendar startDate = Calendar.getInstance();
			startDate.setTime(sDate);

			Date eDate = new Date();
			eDate = sdf.parse(endDateStr);
			Calendar endDate = Calendar.getInstance();
			endDate.setTime(eDate);
			String earliestDateStr = "20090401";
			Date firstDate = sdf.parse(earliestDateStr);
			Calendar badDate = Calendar.getInstance();
			badDate.setTime(firstDate);

			/*
			//#2
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
						.println("End date and start date must be later than 20090401. Please re-enter.");
				return;
			}


			System.out
					.println("after xbrl are parsed do you want to generate the financials? Y/N. \r" +
							"If N - you will need to regenerate them later because it will skip these quarters.\r" +
							"To regenerate them later call method regenerateAllFinancialsForAllYears");

			generateFinancials = true;
			String financials = Scan.nextLine();

			if(financials.toLowerCase().equals("n")){
				generateFinancials = false;
			}
*/

		System.out.println("startDate=" + startDateStr + " endDate=" + endDateStr);
		
		dateRangeQuarters(startDate, endDate);

		FinancialStatement.getCIKfromXBRL_ins_context(qtr, startYr, endYr, false);
		MysqlConnUtils.executeQuery("/*RUN BEFORE getFinancialRanks*/"
				+ "CALL confirm_xbrl_filer_info_its_procedure;");
		FinancialStatement.getFinancials(startYr, endYr, qtr, true);
//		FinancialStatement.getFinancials(startYr, endYr, qtr, false);
		FinancialStatement.getSharesOutstanding(endYr, qtr);
		FinancialStatement.getFinancialRanks();
		dropTmpMySqlTablestoParseXML(startYr + "", qtr + "");
		// below run to parse those local files that failed.
		// XbrlLocalFileParser.dateRangeQuarters(startDate, endDate);
		// }
	}
}
