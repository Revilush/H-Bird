package xbrl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.File;
import java.io.FileWriter;
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
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

//NOTES: 
//A. Will download: 
//(1) Complete submission if: 
//(a) fileSize<100k or (b) items keywords are present (e.g.: results of ops) && fileSize>100k<600k. 
//(2) 99Exhibits if
//(a) fileSize >600k<1mb and items keyword met
//(3) 8k if: 99Exhibit>1mb 
//This ensures 1 unique parsing source for each AccNo which is assumptive of mysql procedures.

public class EightK {

	public static String baseFolder = "c:/backtest/8-K/";

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

	public static boolean isCurrentQuarter(Calendar date) {

		int todayQtr = getQuarter(Calendar.getInstance());
		int qtr = getQuarter(date);
		int todayYear = Calendar.getInstance().get(Calendar.YEAR);
		int year = date.get(Calendar.YEAR);
		return (todayQtr == qtr && todayYear == year);
	}

	public static void dateRangeQuarters(Calendar startDate, Calendar endDate)
			throws SocketException, IOException {

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
			getMasterIdx(startYr, iQtr, cal);
			String localPath = baseFolder + "/" + startYr + "/QTR" + iQtr + "/";
			File file = new File(localPath + "/master.idx");
			if (!file.exists()) {
				ZipUtils.deflateZipFile(localPath + "/master.zip", localPath);
			}
			String zipFilePath = localPath + "/" + startYr + "Qtr" + iQtr
					+ ".zip";
			file = new File(zipFilePath);
			if (file.exists()) {
				ZipUtils.deflateZipFile(zipFilePath, localPath);
			}
			downloadFilingDetails(localPath);
			iQtr++;
			if (iQtr > 4) {
				startYr++;
				iQtr = 1;
			}
		}
	}

	public static void downloadFilingDetails(String localPath) {

		String file = localPath + "/master.idx";
		BufferedReader rdr = null;
		try {
			rdr = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		String line;

		try {
			while ((line = rdr.readLine()) != null) {
				String[] items = line.split("\\|");
				if (items.length < 5)
					continue;
				if (items[4].contains("edgar") && items[2].contains("8-K")) {
					String cik = items[0];
					String formType = items[2];
					if (!items[2].equals("8-K")) {
						formType = "8KA";
					}

					// <=added substring. would not work with 10-K
					String fileDate = items[3];
					String secFile = items[4];
					String acc = secFile.substring(
							secFile.lastIndexOf("/") + 1,
							secFile.lastIndexOf("."));
					// System.out.println("accNO:: " + acc);
					String accHtm = acc + "-index.htm";
					String accNoHyphens = acc.replace("-", "");
					File fileHtm = new File(localPath + "/htm/" + accHtm);

					String suffix = secFile.substring(0,
							secFile.lastIndexOf("/"));

					String wwwSecGovPath = (suffix + "/" + accNoHyphens + "/" + accHtm);
					String wwwSecGovPathHtm = (suffix + "/" + accNoHyphens
							+ "/" + acc + "-index.htm");

					Utils.createFoldersIfReqd(localPath + "/htm");
					String acceptedDate = "";

					if (!fileHtm.exists()) {
						Xbrl.download(wwwSecGovPath, localPath + "/htm", accHtm);
						XBRLInfoSecGov info = new XBRLInfoSecGov(
								"https://www.sec.gov/Archives/"
										+ wwwSecGovPathHtm);
						System.out.println("https://www.sec.gov/Archives/"
								+ wwwSecGovPathHtm);
						acceptedDate = (info.acceptedDate);

						// catches errors where acceptedDate would otherwise go
						// to the wrong folder b/c fileDate and doesn't match
						
						if (acceptedDate.substring(0, 7) != fileDate.substring(
								0, 7)) {
							acceptedDate = fileDate;
						}

						String fye = (info.fye);
						String formItems = (info.formItems);
						String formStr = (info.formStr);

						// System.out.println("items::" + formItems);
						// System.out.println("items::" + formStr);

						File file2 = new File(localPath + "/" + acc
								+ "_itemInfo");

						if (file2.exists()) {
							file2.delete();

						} else
							file2.createNewFile();

						if (formItems != null || formStr != null) {
							try {
								String content = (cik + "||" + acc + "||"
										+ acceptedDate + "||" + formType + "||"
										+ fye + "||" + formStr);

								FileWriter fw = new FileWriter(
										file2.getAbsoluteFile());
								BufferedWriter bw = new BufferedWriter(fw);
								bw.write(content);
								bw.close();

								String filename = file2.getPath();

								String query = "LOAD DATA INFILE '"
										+ filename.replaceAll("\\\\", "/")
										+ "' ignore INTO TABLE 99_8k FIELDS TERMINATED BY '||' "
										+ "(CIK,accNo,acceptedDate,formType,FYE,Items); ";
								try {
									MysqlConnUtils.executeQuery(query);
									file2.delete();
								} catch (SQLException e) {
									e.printStackTrace(System.out);
								}

							} catch (IOException e) {
								e.printStackTrace();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						String fileHtmStr = fileHtm.getAbsolutePath();
						fileHtmStr = fileHtmStr.replaceAll("\\\\", "/");

						getFilingDetails(fileHtmStr, acc);
					}
				}
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			try {
				rdr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void downloadExhibit99(String fileHtmStr,
			String acceptedDate, String acc, String items, String itemsStr,
			Document document) throws FileNotFoundException {

		// System.out.println("itemsStr" + itemsStr);
		// System.out.println("items" + items);

		if (items != null) {

			// && (items.contains("Operation") || items.contains("operation")
			// || items.contains("FD") || items.contains("statements")
			// || items.contains("Statements")
			// || items.contains("other") || items.contains("Other"))) {
			// above not necessary b/c this is a condition precedent in earlier
			// method that calls this.
			Pattern ptrn = Pattern.compile("(?<=((?i)EX-))99[\\.\\d]{2}");

			// gets elements with pattern match EX-99
			Elements ptrnEle = document.getElementsMatchingOwnText(ptrn);

			/*
			 * create pattern=>match/capture pattern=>get parent el related for
			 * match and then find info w/n parent. Here we captured a pattern
			 * that is in a cell. We then identified within the row the href
			 * cell by Id of the child
			 * 
			 * /* <tr> <td scope="row">1</td> <td scope="row">FORM 10-K</td> <td
			 * scope="row"><a href= "/Archives/edgar/dat....</a></td> <td
			 * scope="row">10-K</td> <td scope="row">1451696</td> </tr>
			 */

			// gets parent element of pattern matched above

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
				// Ids child of parent = to fileSize (b/c size equals last child
				// which is located at rowEle.Size -1
				Element td = rowEle.child(rowEle.children().size() - 1);

				// System.out.println("last column: "
				// + td.text());

				int fileSize = Integer.parseInt(td.text());

				if (fileSize < 1000000) {

					String divUrl = "https://www.sec.gov"
							+ rowEle.getElementsByAttribute("href").get(0)
									.attr("href");

					if (divUrl.endsWith(".txt") || divUrl.endsWith(".htm")
							|| divUrl.endsWith(".html")) {
						Element tdType = rowEle
								.child(rowEle.children().size() - 2);

						String extension = null;

						extension = tdType.text();
						System.out.println("99 extension::" + extension);

						// saves fileSize to 99Size table.
						saveFileSize(acceptedDate, acc, extension, fileSize);

						if (extension.contains("99.1")) {
							extension = "99_1";
							System.out.println("x99_1::" + extension);
						}
						if (extension.contains("99.2")) {
							extension = "99_2";
							System.out.println("x99_2::" + extension);
						}
						if (!extension.contains("99_1")
								&& !extension.contains("99_2")) {
							System.out.println("x99_0::" + extension);
							extension = "99_0";
						}

						try {
							DivParser.downloadEx99(divUrl, acceptedDate, acc,
									extension);
						} catch (IOException e) {
							e.printStackTrace();
						} catch (ParseException e) {
							e.printStackTrace();
						}
						// if (i == 2)
						// consider first 2 items only
					}
				}

				if (fileSize > 1000000) {
					downloadForm8k(fileHtmStr, acceptedDate, acc, items,
							itemsStr, document);
				}
			}

		}
	}

	public static void saveFileSize(String acceptedDate, String acc,
			String extension, int fileSize) throws FileNotFoundException {
		File file = new File(getFolderForAcceptedDate(acceptedDate) + "/" + acc
				+ "_Size.txt");

		if (file.exists()) {
			file.delete();
		}
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		PrintWriter out;
		try {
			out = new PrintWriter(
					new BufferedWriter(new FileWriter(file, true)));
			out.append(acc + "||" + fileSize + "||" + extension);
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		String filename = file.getAbsolutePath().replace("\\", "/");

		DividendSentenceExtractor.loadIntoMysql(filename, "99Size");

	}

	public static void downloadForm8k(String fileHtmStr, String acceptedDate,
			String acc, String items, String itemsStr, Document document) throws FileNotFoundException {

		// System.out.println("items::" + items);
		// System.out.println("formStr::" + itemsStr);

		if (items != null) {
			// && (items.contains("Operation") || items.contains("operation")
			// || items.contains("FD") || items.contains("statements")
			// || items.contains("Statements")
			// || items.contains("other") || items.contains("Other"))
			// above is not necessary b/c condition precedent in previous method

			Pattern ptrn = Pattern.compile("(?i)8-k");
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

				// System.out.println("last column: "
				// + td.text());

				int fileSize = Integer.parseInt(td.text());
				if (fileSize < 1000000) {

					String divUrl = "https://www.sec.gov"
							+ rowEle.getElementsByAttribute("href").get(0)
									.attr("href");

					if (divUrl.endsWith(".txt") || divUrl.endsWith(".htm")
							|| divUrl.endsWith(".html")) {
						Element tdType = rowEle
								.child(rowEle.children().size() - 2);

						String extension = null;

						extension = tdType.text();
						System.out.println("extension:: " + extension);
						if (!extension.equals("8-K")) {
							extension = "8KA";
							saveFileSize(acceptedDate, acc, extension, fileSize);
						}

						System.out.println("tdType=8k::" + extension);

						try {
							DivParser.downloadEx99(divUrl, acceptedDate, acc,
									extension);
						} catch (IOException e) {
							e.printStackTrace();
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	public static void downloadCompleteSubmission(String fileHtmStr,
			String acceptedDate, String acc, String items, String itemsStr,
			Document document) throws FileNotFoundException {

		// System.out.println("items::" + items);
		// System.out.println("formStr::" + itemsStr);

		Pattern ptrn = Pattern.compile("(?i)Complete submission");
		Elements ptrnEle = document.getElementsMatchingOwnText(ptrn);

		int fileSize = 0;

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
			// System.out.println("last column: "
			// + td.text());

			fileSize = Integer.parseInt(td.text());
		}

		if (fileSize > 100000) {
			System.out.println("fileSize +100k:: " + fileSize);
			System.out.println("items +100k fileSize:: " + items);
		}

		if (fileSize <= 100000) {
			saveFileSize(acceptedDate, acc, "Complete", fileSize);
			System.out.println("fileSize:: <100k " + fileSize);
			String divUrl = "https://www.sec.gov"
					+ rowEle.getElementsByAttribute("href").get(0).attr("href");
			try {
				DivParser.downloadEx99(divUrl, acceptedDate, acc, "Complete");
				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if ((fileSize > 100000 && fileSize < 600000)
				&& (items.toLowerCase().contains("operation") || items.toLowerCase().contains("fd")
						|| items.toLowerCase().contains("fair disclosure")
						|| items.toLowerCase().contains("statement")
						|| items.toLowerCase().contains("other") )) {
			saveFileSize(acceptedDate, acc, "Complete", fileSize);

			String divUrl = "https://www.sec.gov"
					+ rowEle.getElementsByAttribute("href").get(0).attr("href");
			try {
				DivParser.downloadEx99(divUrl, acceptedDate, acc, "Complete");
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		if (fileSize > 600000
				&& (items.toLowerCase().contains("operation") || items.toLowerCase().contains("fd")
						|| items.toLowerCase().contains("statement")
						|| items.toLowerCase().contains("other") )) {
			System.out.println("going to downloadExhibit99 method....");
			downloadExhibit99(fileHtmStr, acceptedDate, acc, items, itemsStr,
					document);
		}
	}

	public static void getFilingDetails(String pageUrl, String acc)
			throws Exception {
		String itemsStr = "";
		String items = "";

		String html = Utils.readTextFromFileWithSpaceSeparator(pageUrl);
		Document document = Jsoup.parse(html, pageUrl);
		Pattern ptrnA = Pattern
				.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
		Elements dateElement = document.getElementsMatchingOwnText(ptrnA);
		String acceptedDate = dateElement.get(0).text();
		// System.out.println("acceptedDate::" + acceptedDate);

		// Pattern ptrnI = Pattern.compile("formGrouping");
		// Results of Operations
		Elements formGroups = document.getElementsByAttributeValue("class",
				"formGrouping");
		// html source is [<div class="formGrouping">]. class is attribute of
		// div el and "formGrouping" is value. See tech notes for
		// getElementsByAttributeValue

		for (Element ele : formGroups) {
			// loop thru each element of formGroups
			Elements children = ele.getElementsByTag("div");
			// where element name/tag is "div" provide all elements
			boolean rightGroupFound = false;
			for (Element child : children) {
				// System.out.println(JSoupXMLParser.getAttributeValue(child,"class")
				// + "::" + child.text());
				if ("infoHead".equalsIgnoreCase(JSoupXMLParser
						.getAttributeValue(child, "class"))
						&& "Items".equalsIgnoreCase(child.text())) {
					// sub routine then checks for attribute "class" to see if
					// it has value "infoHead" and to see if
					// child element value is equal to "Items"
					rightGroupFound = true;
				}
				if (rightGroupFound
						&& "info".equalsIgnoreCase(JSoupXMLParser
								.getAttributeValue(child, "class"))
						&& child.text().startsWith("Item")) {
					// the div that hold the items value...
					items = child.text();
					// System.out.println("child.text::" + items);
					String[] item = items.split("Item \\d[\\.\\d:]{0,4}");
					itemsStr = Arrays.toString(item);
					System.out.println("itemStr::" + itemsStr);

					items = itemsStr.substring(itemsStr.indexOf(",") + 2,
							itemsStr.lastIndexOf("]"));

					// System.out.println("i[]: " + Arrays.toString(item));
					// System.out.printSln("itemsCsv: " + itemsCsv);
				}
			}
		}

		downloadCompleteSubmission(pageUrl, acceptedDate, acc, items, itemsStr,
				document);

	}

	public static void getMasterIdx(int year, int qtr, Calendar endDate)
			throws SocketException, IOException {

		String localPath = baseFolder + "/" + year + "/QTR" + qtr + "/";
		FileSystemUtils.createFoldersIfReqd(localPath);
		// create folder to save files to
		File f = new File(localPath + "/master.idx");
		// identify if file 'master.zip' is in local path.
		if (f.exists() && !isCurrentQuarter(endDate)) {
			return;
		}
		if (f.exists() && isCurrentQuarter(endDate)) {
			System.out.println("localPath:" + localPath);
			f.delete();
			QuarterlyMasterIdx.download(year, qtr, localPath, "master.zip");
//			ZipUtils.deflateZipFile(localPath + "/master.zip", localPath);
		}
		if (!f.exists()) {
			QuarterlyMasterIdx.download(year, qtr, localPath, "master.zip");
//			ZipUtils.deflateZipFile(localPath + "/master.zip", localPath);
		}
	}

	public static void createTmpMySqlTablestoParseXML() {

		String query = "call createTmpMySqlTablestoParseXML();";

		try {
			MysqlConnUtils.executeUpdateQuery(query);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	public static void main(String[] args) throws ParseException,
			SocketException, IOException {

		@SuppressWarnings("resource")
		Scanner Scan = new Scanner(System.in);
		System.out
				.println("Enter start date for time period to check for dividend data (yyyymmdd)");
		String startDateStr = Scan.nextLine();

		System.out
				.println("Enter start date for time period to check for dividend data (yyyymmdd)");
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
		String earliestDateStr = "19980101";
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
		dateRangeQuarters(startDate, endDate);
		String query = "call update99();";
		try {
			MysqlConnUtils.executeQuery(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
