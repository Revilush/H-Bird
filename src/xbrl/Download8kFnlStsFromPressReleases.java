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
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class Download8kFnlStsFromPressReleases {

	public int tableCount = 0;
	public String fileDate = null; // "2099-01-01";
	public String companyName = "";// from master.idx
	public String coName = ""; // matched.
	public String headerPortionOfFile = "";
	public String cik = "0";
	public String acceptedDate = null;
	public String periodOfReport = null, qtrEnd = null;
	public String colHeadingPatternAtEachLine = "";
	public static String tableSaved = "";

	public String acc = null;// "0000020232-01-500022";
	public String formType = null;// "10-K";
	public String fye = null;// "1231";
	public String formStr = null;
	public String formItems = null;
	public String sicCode = null;

	public static String baseFolder = "c:/backtest/tableParser/";
	public static String secZipFolder = "c:/SECZipFiles/";
	public static String masterIndexFolder = "c:/backtest/master/";

	public void dateRangeQuarters(Calendar startDate, Calendar endDate, boolean justFilesNotInMysql)
			throws SocketException, IOException, SQLException, ParseException {

		// if -- getFromSec = false - it won't attempt to download from
		// edgar/archives each complete.txt file. Instead will stop if not found
		// in SecZipFile

		int initialYear = startDate.get(Calendar.YEAR);
		int endYear = endDate.get(Calendar.YEAR);
		int startQtr = TableParser.getQuarter(startDate);
		int endQtr = TableParser.getQuarter(endDate);
		// total # of loops=totalQtrs.

		String minDate = "";
		String maxDate = "";
		// int cnt = 0;
		int QtrYrs = (endYear - initialYear) * 4;

		System.out.println("endQtr=" + endQtr + " startQtr=" + startQtr + " endDate=" + endDate.get(Calendar.MONTH)
				+ " startDate=" + startDate.get(Calendar.MONTH));

		int iQtr = (endQtr - startQtr) + 1;
		int totalQtrs = QtrYrs + iQtr;
		int nextYr = initialYear;
		iQtr = startQtr;
		Calendar cal = Calendar.getInstance();
		for (int i = 1; i <= totalQtrs; i++) {
			System.out.println("totalQtrs=" + totalQtrs + ",i=" + i);
			cal.set(Calendar.YEAR, nextYr);
			cal.set(Calendar.MONTH, (iQtr * 3) - 1);

			// will retrieve masterIdx if not in folder or current
			String localPath = baseFolder + "/" + nextYr + "/QTR" + iQtr + "/";
			System.out.println("localPath1=" + localPath);
			File file = new File(localPath + "/master.idx");
			FileSystemUtils.createFoldersIfReqd(localPath);
			TableParser tp = new TableParser();

			if (!file.exists() || !tp.isCurrentQuarter(endDate)) {
				System.out.println("getting masterIdx");
				tp.getMasterIdx(nextYr, iQtr, cal);
			}

			System.out.println("creating csv file of 8-Ks not parse");
			getTpIdx(nextYr, iQtr, justFilesNotInMysql);

			String fileName;
			File folder = new File(secZipFolder);
			File[] listOfFiles = folder.listFiles();

			// only first time it will pass here
			// if (cnt == 0) {
			minDate = nextYr + "";
			maxDate = nextYr + "";
			if (iQtr == 1) {
				minDate += "01";
				maxDate += "03";
			} else if (iQtr == 2) {
				minDate += "04";
				maxDate += "06";
			} else if (iQtr == 3) {
				minDate += "07";
				maxDate += "09";
			} else if (iQtr == 4) {
				minDate += "10";
				maxDate += "12";
			}

			int iF = 0;
			System.out.println(
					"minDate=" + minDate + ", maxDate=" + maxDate + ",iQtr=" + iQtr + ",startYear=" + initialYear);
			System.out.println("listOfFiles.len=" + listOfFiles.length);
			for (iF = 0; iF < listOfFiles.length; iF++) {
				if (listOfFiles[iF].isFile()) {
					fileName = listOfFiles[iF].getName().substring(0, 6);
					if (fileName.compareTo(minDate) >= 0 && fileName.compareTo(maxDate) <= 0) {
						String fileDate = listOfFiles[iF].getName().substring(0, 8);
						String extractedNCfolder = folder + "/" + fileDate;
						System.out.println("extractedNCfolder=" + extractedNCfolder);
						FileSystemUtils.createFoldersIfReqd(extractedNCfolder);

						if (!ZipUtils.isTarGzipFileValid(listOfFiles[iF].getAbsolutePath())
								|| listOfFiles[iF].getName().contains(".old")
								|| listOfFiles[iF].getName().contains(".bad")) {

							Utils.createFoldersIfReqd(secZipFolder + "/bad/");
							listOfFiles[iF].renameTo(new File(secZipFolder + "/bad/" + listOfFiles[iF].getName()));
							continue;
						}

						if (listOfFiles[iF].getAbsolutePath().contains("nc.tar.gz")) {
							ZipUtils.deflateTarGzipFile(listOfFiles[iF].getAbsolutePath(), extractedNCfolder);
						}

						else {
							String outputFn = ZipUtils.deflateGzipFile(listOfFiles[iF].getAbsolutePath(),
									extractedNCfolder);

							// below extracts from yyyymmdd.gz file (this file
							// contains all submissions for that day in 1 file)
							// a single filing by capturing from start to end
							// submission marker and saves each filing as
							// accno.nc so that existing 'architechture' can be
							// run to parse it.

							Pattern ACCPattern = Pattern.compile("(?<=(<ACCESSION-NUMBER>)).*\r\n");

							BufferedReader br = new BufferedReader(new FileReader(outputFn));
							StringBuilder sb = new StringBuilder();
							String line, terminator = "</SUBMISSION>";
							while (true) {
								line = br.readLine();
								if (null == line || line.contains(terminator)) {
									String accNo = "";
									Matcher matcherACC = ACCPattern.matcher(sb.toString());
									if (matcherACC.find()) {
										accNo = extractedNCfolder + "/" + matcherACC.group().trim() + ".nc";
										FileSystemUtils.writeToAsciiFile(accNo, sb.toString());
									}
									if (null != line) {
										int idx = line.indexOf(terminator) + terminator.length();
										sb.append(line.substring(0, idx));
										line = line.substring(idx);
									} else
										// we are at EOF
										break;
									sb = new StringBuilder();
								}
								sb.append(line).append("\r\n");
							}
							br.close();
						}

						System.out.println("get accFile iQtr=" + iQtr + " extractToFolder=" + extractedNCfolder);
						// getAccFromNCFile goes to filePath of unzipped nc file
						// which has all accs for given nc filedate. at
						// getAccFromNCfile it pull tpIdx.csv file that has only
						// the missing 10 and 8 files (those not in bac_tp_raw)

						// if I'm parsing 8-Ks though I should also check for
						// dividends

						System.out.println("getEightKAccFromNCfile");
						getEightKAccFromNCfile(nextYr, iQtr, extractedNCfolder);
						File tempDir = new File(extractedNCfolder);
						Utils.deleteDirectory(tempDir);
						FileSystemUtils.createFoldersIfReqd(folder + "/parsed/");
						File parseF = new File(folder + "/parsed/" + listOfFiles[iF].getName());
						// moves zip file to parsed folder after it has parsed
						// all.

						if (parseF.exists())
							parseF.delete();
						listOfFiles[iF].renameTo(parseF);

						System.out.println("deleted extractToFolder:: " + tempDir.getAbsolutePath());
						// file.delete();
					}
					// import here and save csv with date name of NC file.
					File f = new File("C:/backtest/tableParser/tmp");
					if (f.exists())
						insertIntoMysql(nextYr, iQtr, fileDate, f);
				}
			}

			iQtr++;
			if (iQtr > 4) {
				nextYr++;
				iQtr = 1;
			}
			System.out.println("going to next qtr=" + iQtr + " for year=" + nextYr);
		}
	}

	public static void insertIntoMysql(int yr, int q, String fileName, File file) throws IOException, SQLException {

		File f = new File("c:/backtest/tableparser/" + yr + "/" + fileName + "_final_8Ks_to_import_into_mysql.csv");
		PrintWriter pwAddedHardReturnsPriortoAccno = new PrintWriter(f);
		String textAddedHardReturnsPriortoAccno = Utils.readTextFromFile(file.getAbsolutePath());

		textAddedHardReturnsPriortoAccno = textAddedHardReturnsPriortoAccno
				.replaceAll("(?ism)([\\d]{10}-[\\d]{2}-[\\d]{6})", "\r$1").replaceAll("[\n\r]+", "\r");
		pwAddedHardReturnsPriortoAccno.append(textAddedHardReturnsPriortoAccno);
		pwAddedHardReturnsPriortoAccno.close();

		String query = "LOAD Data LOCAL INFILE \r'" + f.getAbsolutePath().replaceAll("\\\\", "//")
				+ "' \rignore INTO TABLE bac_tp_raw" + yr + "qtr" + q + "" + "\r FIELDS TERMINATED BY ',' "
				+ "\rLINES TERMINATED BY '\\r';";

		MysqlConnUtils.executeQuery(query);

	}

	public static void getTpIdx(int yr, int q, boolean justFilesNotInMysql) throws IOException, SQLException {

		Utils.createFoldersIfReqd(baseFolder + "/" + yr + "/QTR" + q + "/");
		File tpIdxFile = new File(baseFolder + "/" + yr + "/QTR" + q + "/tpidx.csv");
		File masterIdxFile = new File("baseFolder + /" + yr + "/qtr" + q + "/master.idx'");

		String minDate = "";
		String maxDate = "";

		if (q == 1) {
			minDate += "01";
			maxDate += "03";
		} else if (q == 2) {
			minDate += "04";
			maxDate += "06";
		} else if (q == 3) {
			minDate += "07";
			maxDate += "09";
		} else if (q == 4) {
			minDate += "10";
			maxDate += "12";
		}

		if (!masterIdxFile.exists())

			if (tpIdxFile.exists())
				tpIdxFile.delete();
		String query = "";

		if (justFilesNotInMysql) {

			System.out.println("outfile=" + tpIdxFile.getAbsolutePath().replaceAll("\\\\", "//"));
			query = "LOAD DATA INFILE '" + baseFolder + "/" + yr + "/qtr" + q + "/master.idx'"
					+ "\rIgnore INTO TABLE tpidx" + "\rFIELDS TERMINATED BY '|'" + "\rLINES TERMINATED BY '\n'"
					+ "\rIGNORE 10 LINES ;\r\r" +

					"set @startDate = (select max(fileDate) from bac_tp_raw" + yr + "qtr" + q + " \r"
					+ " where form rlike '8-K');\r" + " set @startDate =(select case when @startDate is null then '"
					+ yr + "-" + minDate + "-01' else @startDate end dt);\r\n"
					+ "\nDROP TABLE IF EXISTS TMP_TPIDX8k_ACCNO" + yr + q + " ;" + " \r"
					+ "\nCREATE TABLE TMP_TPIDX8k_ACCNO" + yr + q + " ENGINE=MYISAM \r"
					+ "\nSELECT left(right(replace(filename,'\\r',''),24),20) ACCNO\r"
					+ ",cik,`form type`,`date filed`,`company name` from tpidx where " + "\ryear(`Date Filed`)=" + yr
					+ " and quarter(`date filed`)=" + q + "\r and `form type` rlike '8-K' and \r"
					+ "\r( `Company Name` not rlike 'trust|mort|fund|[0-9]{4}|portfo|managed"
					+ "\r|futures|series|receiv| abs[$ ]{1}| mbs[$ ]{1}|heloc|securiti[sz]|prefer"
					+ "\r|structur|asset[ -]back|asset sec|auto rec' " + "\r or `Company Name` rlike 'royalty trust' )"
					+ "\r and `Date Filed`>=@startDate \r"
					+ "\r group by left(right(replace(filename,'\\r',''),24),20); \r\r"

					+ "\r\rSELECT * INTO OUTFILE " + "'" + tpIdxFile.getAbsolutePath().replaceAll("\\\\", "//")
					+ "' \rFIELDS TERMINATED BY '||' " + "ESCAPED BY '\\\\' LINES TERMINATED BY '\\n' \r"
					+ "\r FROM TMP_TPIDX8k_ACCNO" + yr + q + ";\r\r" + "\rALTER TABLE TMP_TPIDX8k_ACCNO" + yr + q
					+ " ADD KEY (ACCNO);\r"

					+ "DROP TABLE IF EXISTS TMP_TPIDX8k_ACCNO" + yr + q + " ;";

		} else {
			if (tpIdxFile.exists())
				tpIdxFile.delete();
			System.out.println("outfile=" + tpIdxFile.getAbsolutePath().replaceAll("\\\\", "//"));

			query = "LOAD DATA INFILE '" + baseFolder + "/" + yr + "/qtr" + q + "/master.idx'"
					+ "\rIgnore INTO TABLE tpidx" + "\rFIELDS TERMINATED BY '|'" + "\rLINES TERMINATED BY '\n'"
					+ "\rIGNORE 10 LINES ;\r\r"

					+ "SELECT left(right(replace(filename,'\\r',''),24),20) ,cik,`form type`,`date filed`,`company name` "
					+ "\r INTO OUTFILE '" + tpIdxFile.getAbsolutePath().replaceAll("\\\\", "//")
					+ "'\r FIELDS TERMINATED BY '||' ESCAPED BY '\\\\' LINES TERMINATED BY '\\n'\r" + "from tpidx \r"
					+ " \rwhere year(`Date Filed`)=" + yr + " and quarter(`date filed`)=" + q
					+ " \rand `form type` rlike '8-K' \r"
					+ " \rgroup by left(right(replace(filename,'\\r',''),24),20);\r";

		}

		MysqlConnUtils.executeQuery(query);

	}

	public void getEightKAccFromNCfile(int year, int quarter, String localPath)
			throws IOException, ParseException, SQLException {

		boolean isSecZipFile = true;

		String folder = baseFolder + "/" + year + "/QTR" + quarter + "/";
		String tpIdx = folder + "tpidx.csv";
		System.out.println("getAcc localPath: " + localPath);
		File f3 = new File(tpIdx);
		System.out.println("tpIdx f3::" + f3);

		if (!f3.exists()) {
			System.out.println("mysql exported file of accno to parse DOES NOT EXIST");
			return;
		}

		BufferedReader tpIdxBR = null;
		try {
			tpIdxBR = new BufferedReader(new FileReader(tpIdx));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		String line;
		File fileToLoadIntoMysql = new File(baseFolder + "/tmp");

		TableParser.pwYyyyQtr = new PrintWriter(fileToLoadIntoMysql);

		try {

			while ((line = tpIdxBR.readLine()) != null) {
				// TODO: failed to export all fields!!!

				acc = line.split("\\|\\|")[0];
				cik = line.split("\\|\\|")[1];
				formType = line.split("\\|\\|")[2];
				companyName = line.split("\\|\\|")[4];
				System.out.println("getAcc:: accno==" + acc + " cik: " + cik + " formType: " + formType + " fileDate: "
						+ fileDate + " companyName: " + companyName);

				String filePath = localPath + "/" + acc + ".nc";
				filePath = filePath.replaceAll("\\\\", "//");
				System.out.println("filePath=" + filePath);
				File file = new File(filePath);

				// if file size>5mb it goes to the next file.
				String ex99s = "";

				if (file.exists()) {
					System.out.println("nc file exists");

					fileDate = line.split("\\|\\|")[3];

					headerPortionOfFile = Utils.readSecHeaderPortionOnlyFromFile(filePath);

					// when pulling from .nc file items are in this format
					if (!formType.toUpperCase().contains("8-K")) {
						System.out.println("form is not an 8-k");
						continue;
					}

					if (!headerPortionOfFile.contains("<ITEMS>2") && !headerPortionOfFile.contains("<ITEMS>5")
							&& !headerPortionOfFile.contains("<ITEMS>7") && !headerPortionOfFile.contains("<ITEMS>9")
							// && !headerPortionOfFile.toLowerCase().contains("financial")
							&& !headerPortionOfFile.toLowerCase().contains("result")
							&& !headerPortionOfFile.toLowerCase().contains("dividend")
							&& !headerPortionOfFile.toLowerCase().contains("fd")
							&& !headerPortionOfFile.toLowerCase().contains("fair disclosure")
					// && !headerPortionOfFile.toLowerCase().contains("other")

					) {
						System.out
								.println("form 8k does not contain items 2, items 9 or text financial/result/dividend");
						continue;
					}

					ex99s = Utils.readTextFromFile(filePath);
					System.out.println("8-K entireHtml.len=" + ex99s.length() + " accno=" + acc);

					if (ex99s.indexOf("<TYPE>EX-99") > 0) {

//						ex99s = NLP.removeGobblyGook(NLP.getEx99s(ex99s));
						System.out.println("8k -- ex99s.len=" + ex99s.length());

					}

					tableCount = 0;
					TableParserHTML tpHtml = new TableParserHTML(acc, fileDate, cik, tableCount, fye, formType,
							companyName);

					TableTextParser ttp = new TableTextParser(acc, fileDate, cik, tableCount, fye, formType,
							companyName);

					int idx = StringUtils.indexOfIgnoreCase(ex99s, "</tr>");
					int idxTd = StringUtils.indexOfIgnoreCase(ex99s, "</td>");

					if (idx >= 0 && idxTd >= 0 && ex99s.length() > 0) {

						System.out.println("sending to tpHtml.getTablesFromFiling");
						tpHtml.getTablesFromFiling(ex99s, false, true);

						// tempPw.append(html);
						// tempPw.close();

						// if not html -- no <html in entireHtml (complete
						// filing)
					}

					if ((idx < 0 || idxTd < 0) && ex99s.length() > 0) {
						tableCount = 100;

						// PrintWriter tempPw2 = new PrintWriter(new File(
						// "c:/backtest/temp2/"
						// + acc + "htmlJustEx13_.txt"));
						// tempPw2.append(strEx13);
						// tempPw2.close();
						ttp.tableTextParser(ex99s, false, true, "1");

					}

					ex99s = "";

				}
			}

			tpIdxBR.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		TableParser.pwYyyyQtr.close();

		TableParser.loadIntoMysqlNCparsings(year, quarter, isSecZipFile);

	}

	public static void main(String[] arg) throws IOException, ParseException, SQLException {

		// THIS WILL PARSE 8-Ks FROM SECZIPFILES -8-K FINANCIAL PRESS RELEASES

		// STEP 1: UNZIP SECZIPFILE -- METHOD _________
		// STEP 2: METHOD THAT FILTERS TO GET JUST 8-K FINANCIAL PRESS RELEASES
		// STEP 3: RUNS FINANCIALS PARSER (method tableParserHTML,tableTextParser and
		// dividend parser)
		
		// STEP 4 TODO:
		// from bac_tp_rawYYYYqtr find those 8-Ks that pre-date 10-Q/K and
		// extract revenues, NI, cf etc and use to update sales rank etc after
		// confirmation it is correct.

		Download8kFnlStsFromPressReleases fnl8k = new Download8kFnlStsFromPressReleases();
		boolean justFilesNotInMysql = false;
		// generally leave as false and just parse new SECZipFiles.

		String startDateStr = "20190101", endDateStr = "20190131";

		/*
		 * @SuppressWarnings("resource") Scanner Scan = new Scanner(System.in);
		 * System.out.println(
		 * "Enter start date of time period to check for 8-K filings to parse from SecZipFiles (yyyymmdd)"
		 * ); String startDateStr = Scan.nextLine();
		 * 
		 * System.out.println("Enter end date (yyyymmdd)"); String endDateStr =
		 * Scan.nextLine();
		 * 
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
		String earliestDateStr = "19930101";
		Date firstDate = sdf.parse(earliestDateStr);
		Calendar badDate = Calendar.getInstance();
		badDate.setTime(firstDate);

		/*
		 * if (endDate.before(startDate)) {
		 * System.out.println("End date must be later than start date. Please re-enter."
		 * ); return; }
		 * 
		 * System.out.println("parse ONLY 8-Ks after last date in mysql (Y/N)");
		 * 
		 * String notInMysqlStr = Scan.nextLine(); if
		 * (notInMysqlStr.toLowerCase().contains("n")) justFilesNotInMysql = false;
		 */
		// currently runs off of SECZipFiles - so need to download most recent first.
		fnl8k.dateRangeQuarters(startDate, endDate, justFilesNotInMysql);

	}
}
