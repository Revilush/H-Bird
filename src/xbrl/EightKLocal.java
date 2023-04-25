package xbrl;

//local parser will reparse complete filing or just the text block
//TODO: ADD a drop table mysql procedure you call from java when RSS and local program are complete
//TODO: Integrate localParsing into downloader (including just text block choice). Low priority.

//EightK parser-have eightK call tableParser
//current EightK process:

//EightKLocal:
//1. dateRangeQuarters
//2. if I say Yes it will call:
//a. parseLocalTextBlock or
//b. parse99Link(localPath)
//3. assuming b: reads masterIdx then looks for index-html
//file to get accepteddate then calls 
//4. DivParser.getTextSimple and then 
//5. DividendSentenceExtractor.getTextBlock. This creates
//a textBlock of dividend data.
//6. above then calls patternsToMatch and that in
//turn calls
//7. matchPattern for each field type - rec/paydt fre etc
//and loads into mysql


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EightKLocal {

	public static String baseFolder = "c:/backtest/8-K/";

	// b/c public static this variable can be used

	public static String getFolderForDate(Calendar date) {
		return baseFolder + date.get(Calendar.YEAR) + "/QTR" + getQuarter(date);
	}

	public static String getFolderForAcceptedDate(String acceptedDate) {

		int year = Integer.parseInt(acceptedDate.substring(0, 4));
		int month = Integer.parseInt(acceptedDate.substring(5, 7));
		// System.out.println("m::" + month);
		int qtr = ((month - 1) / 3) + 1;
		// System.out.println(baseFolder + year + "/QTR" + qtr);
		return baseFolder + year + "/QTR" + qtr;
	}

	// prior to 1998 format is masterYYMMDD.idx. Thereafter masterYYYYMMDD.idx
	// ftp://ftp.sec.gov/edgar/daily-index/1998/QTR4/

	public static int getQuarter(Calendar date) {
		return ((date.get(Calendar.MONTH) / 3) + 1);
	}

	public static void dateRangeQuarters(Calendar startDate, Calendar endDate,
			String choice) throws SocketException, IOException, ParseException {

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

		for (int i = 1; i <= totalQtrs; i++) {
			cal.set(Calendar.YEAR, startYr);
			cal.set(Calendar.MONTH, (iQtr * 3) - 1);

			String localPath = baseFolder + "/" + startYr + "/QTR" + iQtr;
			System.out.println("localPath:" + localPath);
			if (choice.contains("Y") || choice.contains("y")) {
				System.out.println("localPathTextBlock::" + localPath);
				parseLocalTextBlock(localPath);
			} else {
				parse99Link(localPath);
				System.out.println("localPathComplete:" + localPath);
			}
			iQtr++;
			if (iQtr > 4) {
				startYr++;
				iQtr = 1;
			}
		}
	}

	public static void parse99Link(String localPath) throws IOException {
		String MasterIdx = localPath + "/master.idx";
		File f3 = new File(MasterIdx);
		System.out.println("f3::" + f3);

		if (!f3.exists()) {
			System.out
					.println("master.idx file DOES NOT EXIST - it must be downloaded");
			return;
		}
		BufferedReader rdrMasterIdx = null;
		try {
			rdrMasterIdx = new BufferedReader(new FileReader(MasterIdx));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		String masterIdxLine;
		try {
			while ((masterIdxLine = rdrMasterIdx.readLine()) != null) {
				String[] items = masterIdxLine.split("\\|");
				if (items.length < 5)
					continue;
				if (items[4].contains("edgar") && items[2].contains("8-K")) {
					String dateFiled = items[3];
					// System.out.println("dateFiled:: " + dateFiled);
					String fileName = items[4];
					String acc = fileName.substring(
							fileName.lastIndexOf("/") + 1,
							fileName.lastIndexOf("."));

					String pageUrl = localPath + "/htm/" + acc + "-index.htm";
					// String pageUrl =
					// "c:/backtest/8-K/2013/QTR1/htm/0001061393-13-000023-index.htm";
					File file = new File(pageUrl);
					// System.out.println("-index.htm: " + pageUrl);

					if (file.exists()) {
						String html = Utils
								.readTextFromFileWithSpaceSeparator(pageUrl);
						Document document = Jsoup.parse(html, pageUrl);
						Pattern ptrnA = Pattern
								.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
						Elements dateElement = document
								.getElementsMatchingOwnText(ptrnA);
						String acceptedDate = dateElement.get(0).text();
						// System.out.println("acceptedDate:: " + acceptedDate);

						if (acceptedDate.substring(0, 7) == dateFiled
								.substring(0, 7)) {
							dateFiled = acceptedDate;
						}

						// System.out.println("dateFiledFinal:: " + dateFiled);

						File accLinkPath = new File(localPath + "/" + acc
								+ "_8k.html");
						Pattern ptrn = Pattern.compile("(?i)8-K");
						getSize(acceptedDate, acc, ptrn, document, "8k");

						File accLinkPath2 = new File(localPath + "/" + acc
								+ "_99_0.html");
						ptrn = Pattern.compile("(?i)EX-99");
						getSize(acceptedDate, acc, ptrn, document, "99");

						File accLinkPath3 = new File(localPath + "/" + acc
								+ "_Complete.txt");
						ptrn = Pattern.compile("(?i)Complete submission");
						getSize(acceptedDate, acc, ptrn, document, "Complete");

						// System.out.println(accLinkPath.getAbsolutePath());

						if (accLinkPath.exists()) {
							String htmlText = Utils
									.readTextFromFileWithSpaceSeparator(accLinkPath
											.getAbsolutePath());
							htmlText = htmlText
									.replaceAll(
											"(?<=[ \\w\\.]{1})(\\r\\n)(?=[ \\w \\.]{1})",
											" ");
							String simpleText = DivParser.getTextSimple(
									htmlText, "8k");
							DividendSentenceExtractor.getTextBlock(simpleText,
									dateFiled, acc, "8k");
						}

						if (accLinkPath2.exists()) {
							String htmlText = Utils
									.readTextFromFileWithSpaceSeparator(accLinkPath2
											.getAbsolutePath());
							htmlText = htmlText
									.replaceAll(
											"(?<=[ \\w\\.]{1})(\\r\\n)(?=[ \\w \\.]{1})",
											" ");
							String simpleText = DivParser.getTextSimple(
									htmlText, "99_0");
							DividendSentenceExtractor.getTextBlock(simpleText,
									dateFiled, acc, "99_0");
						}

						if (accLinkPath3.exists()) {
							String htmlText = Utils
									.readTextFromFileWithSpaceSeparator(accLinkPath3
											.getAbsolutePath());
							htmlText = htmlText
									.replaceAll(
											"(?<=[ \\w\\.]{1})(\\r\\n)(?=[ \\w \\.]{1})",
											" ");
							String simpleText = DivParser.getTextSimple(
									htmlText, "Complete");
							DividendSentenceExtractor.getTextBlock(simpleText,
									dateFiled, acc, "Complete");
						}
					}
				}
			}
			rdrMasterIdx.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void getSize(String acceptedDate, String acc, Pattern ptrn,
			Document document, String fileType) throws FileNotFoundException {
		Elements ptrnEle = document.getElementsMatchingOwnText(ptrn);
		Element rowEle = null;
		for (int i = 0; i < ptrnEle.size(); i++) {
			Elements parentEle = ptrnEle.parents();
			for (Element eleFind : parentEle) {
				if (eleFind.tagName().equalsIgnoreCase("tr")) {
					rowEle = eleFind;
					break;
				}
			}
			if (null == rowEle)
				continue;
			Element td = rowEle.child(rowEle.children().size() - 1);
			// System.out.println("last column: " + td.text());

			int fileSize = Integer.parseInt(td.text());

			EightK.saveFileSize(acceptedDate, acc, fileType, fileSize);
		}
	}

	public static void parseLocalTextBlock(String folder)
			throws ParseException, IOException {
		System.out.println("folder::" + folder);
		File[] files = new File(folder + "/Text").listFiles();

		for (File file : files) {
			if (file.isFile()) {
				String filePath = file.getAbsolutePath();
				System.out.println("filePath::" + filePath);
				filePath = filePath.replaceAll("\\\\", "\\/");
				try {
					System.out.println("filePath::" + filePath);
					String ex99html = Utils
							.readTextFromFileWithSpaceSeparator(filePath);
					String[] text99 = ex99html.split("\\|\\|");
					String acceptedDate = text99[0];
					String acc = text99[1];
					ex99html = text99[2];
					String exhibit8kor99 = "_" + text99[3].trim();
					DividendSentenceExtractor.loadIntoMysql(filePath,
							"99text");
					DividendSentenceExtractor.patternsToMatch(ex99html,
							acceptedDate, acc, exhibit8kor99);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) throws ParseException,
			SocketException, IOException {
		
		String htmlText = Utils.readTextFromFile("c:/temp/tmpD.htm");
		
		htmlText = htmlText
				.replaceAll(
						"(?<=[ \\w\\.]{1})(\\r\\n)(?=[ \\w \\.]{1})",
						" ");
		String simpleText = DivParser.getTextSimple(
				htmlText, "8k");
		DividendSentenceExtractor.getTextBlock(simpleText,
				"2015-09-18","0", "8k");
	}

/*		@SuppressWarnings("resource")
		Scanner Scan = new Scanner(System.in);
		System.out
				.println("Enter start date for time period to re-parse dividends from local drive (yyyymmdd)");
		String startDateStr = Scan.nextLine();

		System.out
				.println("Enter start date time for period to re-parse dividends from local drive (yyyymmdd)");
		String endDateStr = Scan.nextLine();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date sDate = new Date();
		sDate = sdf.parse(startDateStr);
		Calendar startDate = Calendar.getInstance();
		startDate.setTime(sDate);
		Date eDate = new Date();
		eDate = sdf.parse(endDateStr);
		Calendar endDate = Calendar.getInstance();
		endDate.setTime(eDate);

		if (endDate.before(startDate)) {
			System.out
					.println("End date must be later than start date. Please re-enter.");
			return;
		}
		System.out
				.println("To parse just text block enter 'Y' else 'N' to parse all filings?");
		String choice = Scan.nextLine();
		dateRangeQuarters(startDate, endDate, choice);
		String query = "call update99();";
		try {
			MysqlConnUtils.executeQuery(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}*/
}
