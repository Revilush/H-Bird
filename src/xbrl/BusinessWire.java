package xbrl;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

//TODO: Need to copy over 8k bsnWire data to local drive c:/backtest/8k/2014/QTR1 ... QTR3 and reparse locally using BusinessWire

public class BusinessWire {
	
	public static void localBusinessWireFiles(String folder)
			throws ParseException {

		File file2 = new File(folder);
		if (!file2.exists()) {
			file2.mkdirs();
		}

		File[] files = new File(folder).listFiles();

		for (File file : files) {
			if (file.isFile() && (file.getName().endsWith("bsnWire.html"))) {
				String filePath = file.getAbsolutePath();
				filePath = filePath.replaceAll("\\\\", "\\/");
				String acc = filePath.substring(filePath.lastIndexOf("/") + 1,
						filePath.lastIndexOf("_"));
				String acceptedDate = acc.substring(0, 4) + "-"
						+ acc.substring(4, 6) + "-" + acc.substring(6, 8);
				// System.out.println("acceptedDate::" + acceptedDate);
				String address = acc;
				String fileType = "bsnWire";
				String extn = ".html";
				// System.out.println("acc: " + acc);
				try {
					// System.out.println("address:: " + address);
					DivParser.parseHtml(acceptedDate, acc, address, fileType,
							extn);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	
	public static void EarningsNews(String newsWireUrl, String epsOrDiv) throws IOException, SQLException {
		
		NLP nlp = new NLP();
		
		String localPath = "c:/backtest/NewsWire/epsUrls_bsnWire.txt";

		if (epsOrDiv.equals("div")) {
			localPath = "c:/backtest/NewsWire/divUrls_bsnWire.txt";
		}

		File file = new File(localPath);
		if (file.exists()) {
			file.delete();
		} else {
			System.err.println("I cannot find '" + file + "' ('"
					+ file.getAbsolutePath() + "')");
		}

		file.createNewFile();
		// define here in order to create folder in case it doesn't exist
		File fileUrls = new File(localPath);
		 System.out.println(localPath.substring(0,
		 localPath.lastIndexOf("/") + 1));
		// if folder doesn't exists - it creates
		Utils.createFoldersIfReqd(localPath.substring(0,
				localPath.lastIndexOf("/") + 1));

		// businessWire Url with links to capture and file to save to
		
		System.out.println("newsWireUrl="+newsWireUrl);
		
		Rss.download(newsWireUrl, localPath);
		System.out.println("main url download complete");

		// reads file now with all the links
		String readFrom = Utils.readTextFromFile(localPath);
		System.out.println("main url localPath:: " + localPath);
		String newsWireUrlStr = fileUrls.getAbsolutePath();
		System.out
				.println("fileUrls localPath:: " + fileUrls.getAbsolutePath());

		// get filePath as string for Jsoup
		newsWireUrlStr = newsWireUrlStr.replaceAll("\\\\", "/");
		System.out.println("newsWireUrlStr:: " + newsWireUrlStr);
		Document document = Jsoup.parse(readFrom, newsWireUrlStr);

		// href links are in this class
		Elements titleLinks = document.getElementsByAttributeValue("class",
				"bwTitleLink");
		// this loop is same as short hand loop ele : titleLinks..
		// for (int i=0 ; i <titleLinks.size(); i++ ) {
		// Element ele = titleLinks.get(i);
		// String url = ele.attr("href");

		int cnt=0;
		String priorYr="";
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 1);
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
		String formatted = format1.format(cal.getTime());
		
		if (epsOrDiv.equals("eps")) {
			String path = EightK.getFolderForAcceptedDate(formatted).replaceAll("8-K", "tableParser");
			System.out.println("path="+path);
			FileSystemUtils.createFoldersIfReqd(path);
			TableParser.pwYyyyQtr = new PrintWriter(new File(path + "/" + formatted));
		}
		
		for (Element ele : titleLinks) {
			String url = ele.attr("href");
			System.out.println("url: " + url);
			String acc = null;
			try {
				acc = url.substring(url.lastIndexOf("home") + 5, url.lastIndexOf("home") + 19);
				System.out.println("ur===" + url);
			} catch (IndexOutOfBoundsException e) {
			}

			String acceptedDate = acc.substring(0, 4) + "-"
					+ acc.substring(4, 6) + "-" + acc.substring(6, 8);
			System.out.println("acceptedDate::" + acceptedDate);
			
			cnt++;
			String yr = acceptedDate.substring(0, 4);
			if(cnt<2 || !yr.equals(priorYr)){
			
			String q = (Integer.parseInt(acceptedDate.substring(5,7))-1)/3+1+""; 

			String table = "tp_raw" + yr + "QTR" + q;
			
			Connection con = MysqlConnUtils.getConnection();
			java.sql.DatabaseMetaData dbm = con.getMetaData();
			// check if table exists
			ResultSet tables = dbm.getTables(null, null, table, null);

			if (tables.next()) {
				String qry = "set sql_mode = ALLOW_INVALID_DATES;\rinsert ignore into "
						+ "bac_"
						+ table
						+ "\rselect * from "
						+ table
						+ ";\n DROP TABLE IF EXISTS " + table + ";";
				MysqlConnUtils.executeQuery(qry);
			}

			tables = dbm.getTables(null, null, table, null);
			if (tables.next()) {
				String qry = "set sql_mode = ALLOW_INVALID_DATES;\rinsert ignore into "
						+ "tp_co "
						+ "\rselect * from tmp_tp_co;\n"
						+ "DROP TABLE IF EXISTS TMP_TP_CO;\n"
						+ "CREATE TABLE `TMP_tp_co` (\n"
						+ "  `accno` varchar(20) NOT NULL DEFAULT '-1',\n"
						+ "  `filedate` datetime NOT NULL DEFAULT '1901-01-01 00:00:00',\n"
						+ "  `cik` int(11) DEFAULT NULL,\n"
						+ "  `tno` int(5) NOT NULL DEFAULT '-1',\n"
						+ "  `tablename` varchar(255) CHARACTER SET utf8 NOT NULL DEFAULT '',\n"
						+ "  `tn` varchar(15) CHARACTER SET utf8 NOT NULL DEFAULT '',\n"
						+ "  `conameMatched` varchar(255) CHARACTER SET utf8 NOT NULL DEFAULT '',\n"
						+ " `conameOnPriorLine` varchar(255) CHARACTER SET utf8 NOT NULL DEFAULT '',\n"
						+ "  primary key(accno,tno)\n"
						+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n";
				MysqlConnUtils.executeQuery(qry);

			}
			
			NLP.createTPrawTable(table);			
			}
			
			priorYr = acceptedDate.substring(0,4);
			
			System.out.println("bsnWire acc::" + acc);

			url = "https://www.businesswire.com/" + url;
			System.out.println("final:: " + url);
			TableParser tp = new TableParser();
			String folder = tp.getFolderForAcceptedDate(acceptedDate);
			
			if(epsOrDiv.equals("div")){
				folder = EightK.getFolderForAcceptedDate(acceptedDate);
			}
			
			Utils.createFoldersIfReqd(folder);
			System.out.println("folder:: " + folder);
			
			try {
				File fileOrig = new File(
						tp.getFolderForAcceptedDate(acceptedDate)
								+ "/" + acc + "_bsnWire.html");
				if(epsOrDiv.equals("div")){
					fileOrig = new File(
							EightK.getFolderForAcceptedDate(acceptedDate)
									+ "/" + acc + "_bsnWire.html");
				}
					
				// if (fileOrig.exists())
				// return;
				
				if(epsOrDiv.equals("div")){
					System.out.println("DivParser.downloadEx99");
					DivParser.downloadEx99(url, acceptedDate, acc, "bsnWire");
				}
					
				else {
					System.out.println("tp.downloadEx99. url="+url);
					tp.downloadEx99(url, acceptedDate, acc, "bsnWire");
				}
				
			} catch (ParseException e) {
				e.printStackTrace();
			}
			// System.out.println("http://www.businesswire.com/" + url);
		}

		Elements nextLinks = document.getElementsMatchingOwnText("Next »");
		if (null != nextLinks && nextLinks.size() > 0) {
			
			System.out.println("epsOrDiv="+epsOrDiv);
			
			if(epsOrDiv.equals("eps")){

				System.out.println("loadIntoMysqlNCparsings="+epsOrDiv);

				TableParser.pwYyyyQtr.close();
				TableParser.loadIntoMysqlNCparsingsBsnWire(Integer.parseInt(formatted.substring(0, 4)),
						TableParser.getQuarter(cal));
				
			}

			String nextUrl = "https://www.businesswire.com/"
					+ nextLinks.get(0).attr("href");
			 System.out.println("Going to next page..." + nextUrl);
			EarningsNews(nextUrl, epsOrDiv);
			
		}
		
		if (epsOrDiv.equals("div"))
			MysqlConnUtils.executeQuery("call update99()");
	}


	public static void main(String[] args) throws ParseException, SQLException, IOException {
		
		/*
		 * TO DO conform to other local parsers - ask for date range - got to
		 * folders and parse. Can only parse historical business wire on local
		 * drive. businessWire is RSS. So we add a RSS button - and when we
		 * click RSS: choose SEC; NewsWire
		 */
		@SuppressWarnings("resource")
		Scanner Scan = new Scanner(System.in);

//		System.out
//				.println("If you want to parse new files type 'Y', else 'N' for local files?");
//		String yesNo = Scan.nextLine();
//
//		if (yesNo.equalsIgnoreCase("N")) {
//
//			System.out
//					.println("Enter start date for time period to parse local newswire files (yyyymmdd)");
//			String startDateStr = Scan.nextLine();
//
//			System.out
//					.println("Enter start date for time period to parse local newswire files (yyyymmdd)");
//			String endDateStr = Scan.nextLine();
//
//			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
//			Date sDate = new Date();
//			sDate = sdf.parse(startDateStr);
//			Calendar startDate = Calendar.getInstance();
//			startDate.setTime(sDate);
//			Date eDate = new Date();
//			eDate = sdf.parse(endDateStr);
//			Calendar endDate = Calendar.getInstance();
//			endDate.setTime(eDate);
//
//			int startYear = startDate.get(Calendar.YEAR);
//			int endYear = endDate.get(Calendar.YEAR);
//			int startQtr = EightKLocal.getQuarter(startDate);
//			int endQtr = EightKLocal.getQuarter(endDate);
//
//			int QtrYrs = (endYear - startYear) * 4;
//			int iQtr = (endQtr - startQtr) + 1;
//			int totalQtrs = QtrYrs + iQtr;
//			int startYr = startYear;
//			iQtr = startQtr;
//			Calendar cal = Calendar.getInstance();
//
//			for (int i = 1; i <= totalQtrs; i++) {
//				cal.set(Calendar.YEAR, startYr);
//				cal.set(Calendar.MONTH, (iQtr * 3) - 1);
//
//				String folder = EightKLocal.baseFolder + "/" + startYr + "/QTR"
//						+ iQtr;
//				localBusinessWireFiles(folder);
//				iQtr++;
//				if (iQtr > 4) {
//					startYr++;
//					iQtr = 1;
//				}
//			}
//		}

//		if (yesNo.equalsIgnoreCase("Y")) {
			// div files are saved to 8-k/yyyy/Qtr# folder
			
			// eps files are saved tableParser/yyyy/Qtr#
		BusinessWire.EarningsNews("https://www.businesswire.com/portal/site/home/news/subject/?vnsId=31350", "eps");

		BusinessWire.EarningsNews("https://www.businesswire.com/portal/site/home/news/subject/?vnsId=31348", "div");

//		}
	}
}
