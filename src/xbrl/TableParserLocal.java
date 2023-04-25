package xbrl;

//core methods are tableParserHTML and tableParserText (if html or raw text), and tableParser getGeneric method for those failed.

//TODO: SEE = REMOVE COMMENTS WHEN FINISHED. WANT PROGRAM TO DROP THESE TABLES.
//tableParserLocal: this will parse SecZipFiles
//1. dateRangeQuarters (startDt,endDt,form) get yr/qtr then calls
//2. getMasterIdx (downlaods qtryl masteridx) 
//3.loadMasterIndexIntoMysql
//4. extracts applicable secZipFile to folder
//5. determines which files are already in mysql and creates
//acc list of those not yet parsed.
//6. calls tableParser.getAccFromNCFile --- after this point
//7. downloadFilingDetails(localPath) -- goes to sec.gov site (but only if not in .nc file) 
//- downloads index-htm site (asigns formType which
//can be 8k or 10q/k) and then calls
//8. getFilingDetails(fileHtmlStr)
//- parses key info from file (index-htm file)
//and then calls
//9. downloadExhibit99 if formtype contains 8 else calls downloadCompleteSubmission
//10. both will call: downloadEx99
//11. downloadEx99 grabs the file from sec.gov to be parsed
//12. parseTable
//13. getGenericTableText -- which calls removeAttachments -- which calls stripHtml
//13. is now: tableParserHTML or tableParserText is called in lieu of getGenericText (if html or raw text).

//14. When program is finished it then calls TableParser and goes online and gets missing accno.
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;

public class TableParserLocal {

	public Document document;
	public static String baseFolder = "c:/backtest/tableParser/";
	public static String secZipFolder = "e:/SECZipFiles/";
	public static String masterIndexFolder = "c:/backtest/masterIndex/";
	public static boolean getFromSec = true;

	public static void getTpIdx(int yr, int q, boolean justInMysql) throws IOException, SQLException {
		
		int ifLessThanThisManyRowsReparse=20;
		Utils.createFoldersIfReqd(baseFolder+"/"+yr+"/QTR"+q+ "/");
		File tpIdxFile = new File(baseFolder+"/"+yr+"/QTR"+q
				+ "/tpidx.csv");
		
		if (tpIdxFile.exists())
			tpIdxFile.delete();
		String query = "";
		
		if(justInMysql) {
			query = 
				"LOAD DATA INFILE '"+baseFolder+"/"+yr+"/qtr"+q+"/master.idx'"+ 
				"\rIgnore INTO TABLE tpidx"+
				"\rFIELDS TERMINATED BY '|'"+
				"\rLINES TERMINATED BY '\n'"+
				"\rIGNORE 10 LINES ;\r\r"+
				
				"\nDROP TABLE IF EXISTS TMP_TPIDX_ACCNO" + yr + q + " ; \r"
		+ "\nCREATE TABLE TMP_TPIDX_ACCNO"
				+ yr + q + " ENGINE=MYISAM \r" 
		+ "\nSELECT left(right(filename,25),20) ACCNO from tpidx where "
				+ "year(`Date Filed`)=" + yr + " and quarter(`date filed`)=" + q
				+ " and `form type` rlike '10-(q|k)' and `form type` not rlike 'NT' and \r"
				+ "( `Company Name` not rlike 'trust|mort|fund|[0-9]{4}|portfo|managed|futures|series"
				+ "|receiv| abs[$ ]{1}| mbs[$ ]{1}"
				+ "|heloc|securiti[sz]|prefer|structur|asset[ -]back|asset sec|auto rec' "
				+ "\r or `Company Name` rlike 'royalty trust' )\r" + " group by left (right(filename,25),20); \r\r"

				+ "\nINSERT IGNORE INTO TMP_TPIDX_ACCNO" + yr + q
				+ "\n select accno from (select accno,count(*) cnt from bac_tp_raw" + yr + "QTR" + q
				+ " where year(fileDate)=" + yr + " and quarter(filedate)=" + q 
						+ " AND edt2 rlike '^[12]{1}[09]{1}[0-9]{2}-[012]{1,2}[0-9]{1}-[0123]{1}[0-9]' "
						+ " and length(edt2)=10 AND p2 rlike '3|6|9|12' "
						+ "group by accno ) t1 where cnt>"
				+ ifLessThanThisManyRowsReparse + "; \r\r"

				+ "\nDROP TABLE IF EXISTS TMP_TPIDX_ACC" + yr + q + ";\r" 
				+ "\nCREATE TABLE TMP_TPIDX_ACC" + yr + q+ " ENGINE=MYISAM " 
				+ "\n SELECT cik,`company name`,`form type`,`date filed`,`accno` "
				+ " FROM (SELECT COUNT(*) CNT, ACCNO " + " FROM TMP_TPIDX_ACCNO" + yr + q
				+ " GROUP BY ACCNO) T1 inner join tpidx t2 on t1.accno=left(right(filename,25),20)"
				+ " WHERE CNT=1 and year(`Date Filed`)=" + yr + " and quarter(`date filed`)=" + q + ";"
				+ "\nSELECT * INTO OUTFILE " + "'" + tpIdxFile.getAbsolutePath().replaceAll("\\\\", "//")
				+ "' FIELDS TERMINATED BY '||' " + "ESCAPED BY '\\\\' LINES TERMINATED BY '\\n' \r"
				+ " FROM TMP_TPIDX_ACC" + yr + q
				// + " where cik between 780000 and 800000"
				+ ";\r\r"
				+ "ALTER TABLE TMP_TPIDX_ACC" + yr + q + " ADD KEY (ACCNO);"
//				+ "\r\rdelete t1 from bac_tp_raw" + yr + "qtr" + q + " t1, TMP_TPIDX_ACC" + yr + q + " t2"
//				+ " where t1.accno=t2.accno;\r\r"				 

				+ "DROP TABLE IF EXISTS TMP_TPIDX_ACC" + yr + q + ";" + "DROP TABLE IF EXISTS TMP_TPIDX_ACCNO" + yr + q
				+ " ;";
		}
		else {
			if (tpIdxFile.exists())
				tpIdxFile.delete();
			query = "LOAD DATA INFILE '" + baseFolder + "/" + yr + "/qtr" + q + "/master.idx'"
					+ "\rIgnore INTO TABLE tpidx" + "\rFIELDS TERMINATED BY '|'" + "\rLINES TERMINATED BY '\n'"
					+ "\rIGNORE 10 LINES ;\r\r"

					+ "SELECT left(right(filename,25),20) ,cik,`form type`,`date filed`" + ",`company name` "
					+ " INTO OUTFILE '" + tpIdxFile.getAbsolutePath().replaceAll("\\\\", "//")
					+ "' FIELDS TERMINATED BY '||' ESCAPED BY '\\\\' LINES TERMINATED BY '\\n'\r" + "from tpidx \r"
					+ " where year(`Date Filed`)=" + yr + " and quarter(`date filed`)=" + q
					+ " and `form type` rlike '10-(q|k)' and `form type` not rlike 'NT'\r"
					+ " group by left(right(filename,25),20);\r";

		}

		MysqlConnUtils.executeQuery(query);
		
	}

	
	public void dateRangeQuarters(Calendar startDate, Calendar endDate, boolean getFromSec, boolean parseEntireHtml,
			boolean justFilesNotInMysql) throws SocketException, IOException, SQLException, ParseException {

		TableParser tpr = new TableParser();
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
		// System.xut.println("nextYr:" + nextYr);
		iQtr = startQtr;
		System.out.println("iQtr:" + iQtr);
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

//			if (mysql && !tp.isCurrentQuarter(endDate)) {
//				System.out.println("getTpRawAcc for year=" + nextYr + " qtr=" + iQtr);
//				getTpIdx(nextYr, iQtr);
//			}

			if (!file.exists() && !tp.isCurrentQuarter(endDate)) {
				tp.getMasterIdx(nextYr, iQtr, cal);
			}

			getTpIdx(nextYr, iQtr,justFilesNotInMysql);


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
			for (iF = 0; iF < listOfFiles.length; iF++) {

				if (listOfFiles[iF].isFile()) {
					fileName = listOfFiles[iF].getName().substring(0, 6);
					if (fileName.compareTo(minDate) >= 0 && fileName.compareTo(maxDate) <= 0) {
						String fileDate = listOfFiles[iF].getName().substring(0, 8);
						String extractedNCfolder = folder + "/" + fileDate;
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
						tp.getAccFromNCfile(nextYr,iQtr,extractedNCfolder, parseEntireHtml);
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
				}
			}
			

			if (getFromSec) {

				String startDateStr = minDate + "01";
				String endDateStr = maxDate + "30";
				System.out.println("startDateStr==" + startDateStr);
				if (iQtr == 1 || iQtr == 4) {
					endDateStr = maxDate + "31";
				}

				System.out.println("endDateStr==" + endDateStr);

				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
				Date sDate = new Date();
				sDate = sdf.parse(startDateStr);
				Calendar startDate2 = Calendar.getInstance();
				startDate2.setTime(sDate);
				Date eDate = new Date();
				eDate = sdf.parse(endDateStr);
				Calendar endDate2 = Calendar.getInstance();
				endDate2.setTime(eDate);
				System.out.println("get from SEC.GOV");
				tpr.dateRangeQuarters(startDate2, endDate2, true, parseEntireHtml);
			}
			

			iQtr++;
			if (iQtr > 4) {
				nextYr++;
				iQtr = 1;
			}
			System.out.println("going to next qtr=" + iQtr + " for year=" + nextYr);
		}
	}

	public static void main(String[] arg) throws IOException, ParseException, SQLException {

		@SuppressWarnings("resource")
		Scanner Scan = new Scanner(System.in);
		System.out.println("Enter start date of time period to check for tables to parse(yyyymmdd)");
		String startDateStr = Scan.nextLine();

		System.out.println("Enter end date of time period to check for tables to parse (yyyymmdd)");
		String endDateStr = Scan.nextLine();

		/*
		 * System.out .println(
		 * "Enter 1 to grab financial statements from quartelry and annual reports. \n"
		 * + "Enter 2 to grab financial statements from press releases." +
		 * "  Enter 3 to grab both[TODO] ");
		 * 
		 * String form = Scan.nextLine();
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

		if (endDate.before(startDate)) {
			System.out.println("End date must be later than start date. Please re-enter.");
			return;
		}

		TableParserLocal tpL = new TableParserLocal();
		getFromSec = false;

		// if false - it won't attempt to download from
		// edgar/archives each complete.txt file. Instead
		// will stop if not found in SecZipFile

		System.out.println("if local parsing of .nc file fails - do you want to download from sec.gov? (Y/N)");
		if (Scan.nextLine().toLowerCase().contains("y")) {
			getFromSec = true;
		}

//		System.out.println("Do you want to parse just files not in mysql (Y/N)?");
//		boolean mysql = false;
//		if (Scan.nextLine().toLowerCase().contains("y"))

		boolean parseEnitreHtml = false;
		System.out.println(
				"TODO: RESET public static String skipAmend to no value ==> \"\" - \rsee TableParser method.\r\r\r"
						+ "To parse just 10-Q/K & EX-13 (hit enter). To parse 99s /entire filing type: 2. \r"
						+ "- It takes a long time to parse entire filing.\r\r\r");

		if (Scan.nextLine().contains("2")) {
			parseEnitreHtml = true;
		}

		// TODO: RESET public static String skipAmend to ==> "" - see
		// TableParser.

		// TODO: if "2" then run query to create idx of just the missing accnos
		// -- broaden query so it gets accnos that were parsed but don't have
		// IS/BS with at least 16 rows.

		//TODO: This only works by getting all acc in .nc file
		tpL.dateRangeQuarters(startDate, endDate, getFromSec, parseEnitreHtml, false);

		// NOTE: in TableTextParser - replacement of text is slow if fileSize is
		// large - I have set it to skip those replacements (a small subset)
		// that take a longtime if fileSize is greater than a certain size.
		// Existing default is 200k or .2mb.

		/*
		 * NOTE: tableParserLocal PARSEs FROM .NC FILES (see getAccFromNCfile) ALL
		 * 10-Qs/Ks. getAccFromNCfile calls TableParser which then calls calls
		 * TableParserText and TableParserHtml class tableParsers.
		 * 
		 * TODO: Once 10Q/K works - employ same process to parse 8K tables (IS, BS, CF).
		 * In addition, while parsing 8Ks also call DIVIDEND PARSER to apply to txt
		 * captured from 8k news release. Also employ tpHtml and tpTxt class parsers
		 * against newswires to get IS, BS, CF -- both end of day and RSS. Same for
		 * sec.gov RSS
		 */

		// ensure local and auto update each night is grabbing 10K/Qs, 8K IS, BS
		// and CF. And 8K dividends. Run against nc and sec.gov. Also get RSS to
		// work. Laslty, tableParser (IS,BS,CF) to be called for businessWire,
		// marketWire both RSS and auto update end of day

		// TODO: acc and fileDate must be set to null
		// TODO: COMMENT OUT SYSTEM.OUT.PRINTLN -- RUNS MUCH MUCH FASTER

		// zip files downloaded from SEC

	}
}
