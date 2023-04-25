package xbrl;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.http.conn.HttpHostConnectException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
//import java.sql.ResultSetMetaData;
//import java.sql.SQLException;
//import java.sql.Statement;
//import java.util.HashMap;
//import java.util.Map;

public class YahooPriceDownloadWorker implements Runnable {

	EasyHttpClient httpClient = new EasyHttpClient(false);

	// /http://ichart.finance.yahoo.com/table.csv?s=AAPL&a=09&b=2&c=2012&d=11&e=15&f=2013&g=d&ignore=.csv
	// /http://ichart.finance.yahoo.com/table.csv?s=YHOO&d=12&e=14&f=2013&g=d&a=3&b=12&c=1996&ignore=.csv

//	public static String YahooBaseUrl = "http://ichart.finance.yahoo.com/table.csv?ignore=.csv";

	public static String YahooQuoteBaseUrl = "https://finance.yahoo.com/quote/@ticker@/history?p=@ticker@";
	
	
	public static String YahooBaseUrl = "http://real-chart.finance.yahoo.com/table.csv?ignore=.csv";
	public static String YahooBaseSplitUrl = "http://finance.yahoo.com/q/hp?&g=v"; 
	// s=aapl&a=03&b=12&c=1996&d=11&e=20&f=2013";

	public static String csvDateFormat = "yyyy-MM-dd";
	public static String TempDataFilePath = "c:/BACKTEST/PRICE/";

	public static String MysqlPriceTableName = "stockdata";
	public static String MysqlDividendTableName = "stockdividend";
	public static String MysqlDividendSplitTableName = "stocksplit";

	private String ticker = null;
	private Date startDate4Price = null;
	private Date startDate4Dividend = null;
	private double adjCloseOnStartDate = -1;
	private double dividendOnStartDate = -1;
	private Date lastDate = null;
	private File dataFile = null;

	public YahooPriceDownloadWorker(){}	// for testing
	
	public YahooPriceDownloadWorker(String its, String startDate4Price,
			String startDate4Dividend, String adjCloseOnStartDate,
			String dividendOnStartDate, String lastDate) {
		/*
		 * "this.ticker" references to the current object instance of the class
		 * YahooPriceDownloadWorker. Whenever Java creates a new instance of the
		 * class java it calls the constructor. The constructor has the same
		 * name as the class. "This.ticker" refers to the current object
		 * instance of that variable (i.e., the actual ticker that is passed by
		 * the caller (caller is the method in the location this is being
		 * called). See for example "new YahooPriceDownloadWorker" in the
		 * YahooPriceDownloader class.
		 */
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		this.ticker = its;
		try {
			if (null != startDate4Dividend && startDate4Dividend.length() > 0)
				this.startDate4Dividend = sdf.parse(startDate4Dividend);
			if (null != startDate4Price && startDate4Price.length() > 0)
				this.startDate4Price = sdf.parse(startDate4Price);
			if (null != lastDate && lastDate.length() > 0)
				this.lastDate = sdf.parse(lastDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		if (null != adjCloseOnStartDate && adjCloseOnStartDate.length() > 0)
			this.adjCloseOnStartDate = Double.parseDouble(adjCloseOnStartDate);
		if (null != dividendOnStartDate && dividendOnStartDate.length() > 0)
			this.dividendOnStartDate = Double.parseDouble(dividendOnStartDate);
	}

//	@SuppressWarnings("deprecation")
//	@Override
	public void run() {
		// System.out.println("Running Yahoo history downloader for ticker:"
		// + ticker);
		try {
			// don't download if startDate4Price is yesterday or today and
			// today's time is before 4:00 p.m. EST
			if (null != startDate4Price) {
				if (startDate4Price.before(lastDate)) {
					System.out.println(ticker + "::"
							+ Thread.currentThread().getId());
					refreshPriceHistory(ticker);
				}

				/*
				 * Date today = new Date(); int lastDay = 1; // (0 = Sunday, 1 =
				 * Monday, 2 = Tuesday, 3 = Wednesday, 4 = // Thursday, 5 =
				 * Friday, 6 = Saturday) // ^GSPC - ticker used to determine
				 * what the last date is // if ^GSPC last date > my ticker last
				 * date - download if ((startDate4Price.getDate() <
				 * (today.getDate() - lastDay)) || (startDate4Price.getDate() >=
				 * (today.getDate() - 1) && today .getHours() > 16)) {
				 * System.out.println(ticker + "::" +
				 * Thread.currentThread().getId()); refreshPriceHistory(ticker);
				 * // refreshDividendHistory(ticker); // PRAVEEN: I also skip
				 * re-downloading Div if price // history // is current (not
				 * ideal but works and it will always // double // check in any
				 * event when price updates because it // always // goes back to
				 * last update dividend date. }
				 */
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != dataFile)
				;
			// dataFile.delete();
		}
	}

	public void refreshPriceHistory(String ticker) throws Exception {
		/*
		 * String query = "SELECT Max(Date) as date from " + MysqlPriceTableName
		 * + " where its='" + ticker + "'"; Map<String, Object> result =
		 * getRecordFromMySql(query); Calendar startDate = null; if (null !=
		 * result && result.size() > 0 && null != result.get("date")) {
		 * startDate = Calendar.getInstance(); startDate.setTime((Date)
		 * result.get("date")); }
		 */

		Calendar startDateCal = null;
		if (null != startDate4Price) {
			startDateCal = Calendar.getInstance();
			startDateCal.setTime(startDate4Price);
		}
		refreshPriceHistory(ticker, startDateCal, null);
	}

	public void refreshPriceHistory(String ticker, Calendar startDate,
			Calendar endDate) throws Exception {
		Calendar startDt = startDate;
		if (null == startDate) {
			startDt = Calendar.getInstance();
			startDt.set(1970, 1, 1);
			// if startDate is null set it it 1970.
		}
		if (null == endDate)
			endDate = Calendar.getInstance();
		// System.out.println("refreshing price data for " + ticker
		// + " for dates from:" + startDt.getTime() + " till:"
		// + endDate.getTime());
		dataFile = new File(TempDataFilePath, ticker + "_price.csv");
		String html = saveYahooInfo2File(ticker, startDt, endDate,
				"DailyPrice", dataFile);
		if (null == html)
			return;
		@SuppressWarnings("unused")
		SimpleDateFormat sdf = new SimpleDateFormat(csvDateFormat);
		// get adjClose value from csv, for minimum date. MinDate may not be
		// equal to startDate..
		String[] csvDate_AdjClose = getCSVRecord4MinDate(html);
		// get [Date,adjClose] for minDate in csv records

		// String truncateQuery;
		if (null != csvDate_AdjClose && null != startDate) {
			@SuppressWarnings("unused")
			String minDate = csvDate_AdjClose[0];
			String csvAdjClose = csvDate_AdjClose[csvDate_AdjClose.length - 1];
			// last item is adjClose..
			// String mysqlAdjClose = getAdjcloseFromMySql(ticker,
			// sdf.parse(minDate));
			double mysqlAdjClose = adjCloseOnStartDate;
			// Double is a class. mysqlAdjClose variable is a string.
			// Double.parse... converts it to #.
			if (mysqlAdjClose != Double.parseDouble(csvAdjClose)) {
				// System.out.println("Price data mis-match: ticker=" + ticker
				// + ", mysqlAdjClose=" + mysqlAdjClose + ", csvAdjClose="
				// + csvAdjClose + ", minDate=" + minDate);
				// adjClose in csv has been changed - truncate sql table and
				// reload full history..
				// truncateQuery = "Delete from " + MysqlPriceTableName
				// + " WHERE its='" + ticker + "'";
				// MysqlConnUtils.executeQuery(truncateQuery);
				// reload the history, with start/end dates passed as null :-
				// from very first date possible till today..

				// only time we update stockdividend is if we have re-download
				// entire history of stockdata for that ITS.
				System.out.println("div:" + ticker);
				refreshDividendHistory(ticker);
				System.out.println("d'loading full:" + ticker);
				refreshPriceHistory(ticker, null, null);
				return;
			}
		}
		// truncateQuery = "Delete from " + MysqlPriceTableName + " WHERE its='"
		// + ticker + "' AND date >= str_to_date('"
		// + sdf.format(startDt.getTime())
		// + "', '%Y-%m-%d') AND date <= str_to_date('"
		// + sdf.format(endDate.getTime()) + "', '%Y-%m-%d')";
		// MysqlConnUtils.executeQuery(truncateQuery);
		// <=PRAVEEN: I don't think we need above.

		loadPriceDividendFileIntoMysql(dataFile.getAbsolutePath(),
				MysqlPriceTableName, ticker);
	}

	public void refreshDividendHistory(String ticker) throws Exception {
		/*
		 * String query = "SELECT Max(Date) as date from " +
		 * MysqlDividendTableName + " where its='" + ticker + "'"; Map<String,
		 * Object> result = getRecordFromMySql(query); Calendar startDate =
		 * null; if (null != result && result.size() > 0 && null !=
		 * result.get("date")) { startDate = Calendar.getInstance();
		 * startDate.setTime((Date) result.get("date")); }
		 * refreshDividendHistory(ticker, startDate, null);
		 */

		Calendar startDateCal = null;
		if (null != startDate4Dividend) {
			startDateCal = Calendar.getInstance();
			startDateCal.setTime(startDate4Dividend);
		}
		refreshDividendHistory(ticker, startDateCal, null);
	}

	public void refreshDividendHistory(String ticker, Calendar startDate,
			Calendar endDate) throws Exception {
		Calendar startDt = startDate;
		if (null == startDate) {
			startDt = Calendar.getInstance();
			startDt.set(1970, 1, 1);
		}
		if (null == endDate)
			endDate = Calendar.getInstance();
		dataFile = new File(TempDataFilePath, ticker + "_div.csv");
		String html = saveYahooInfo2File(ticker, startDt, endDate, "Dividend",
				dataFile);
		if (null == html)
			return;
		SimpleDateFormat sdf = new SimpleDateFormat(csvDateFormat);
		// get adjClose value from csv, for minimum date. MinDate may not be
		// equal to startDate..
		String[] csvDate_Dividend = getCSVRecord4MinDate(html);
		// get [Date,Dividend] for minDate in csv records
		if (null != csvDate_Dividend && null != startDate) {
			@SuppressWarnings("unused")
			String minDate = csvDate_Dividend[0];
			String csvDividend = csvDate_Dividend[1];
			// String mysqlDividend = getDividendFromMySql(ticker,
			// sdf.parse(minDate));

			if (dividendOnStartDate != Double.parseDouble(csvDividend)) {
				// Dividend in csv has been changed - truncate sql table and
				// reload full dividend history..
				// System.out.println("Dividend data mis-match: ticker=" +
				// ticker
				// + ", mysqlDividend=" + dividendOnStartDate
				// + ", csvDividend=" + csvDividend + ", minDate="
				// + minDate);
				// truncateQuery = "Delete from " + MysqlDividendTableName
				// + " WHERE its='" + ticker + "'";
				// MysqlConnUtils.executeQuery(truncateQuery);
				// reload the history, with start/end dates passed as null :-
				// from very first date possible till today..
				refreshDividendHistory(ticker, null, null);
				return;
			}
		}
		// truncateQuery = "Delete from " + MysqlDividendTableName
		// + " WHERE its='" + ticker + "' AND date >= str_to_date('"
		// + sdf.format(startDt.getTime())
		// + "', '%Y-%m-%d') AND date <= str_to_date('"
		// + sdf.format(endDate.getTime()) + "', '%Y-%m-%d')";
		// MysqlConnUtils.executeQuery(truncateQuery);
		// load the data file into mysql now..
		loadPriceDividendFileIntoMysql(dataFile.getAbsolutePath(),
				MysqlDividendTableName, ticker);

		// / load split data as well..
		// getYahooHistoryUrl(YahooBaseSplitUrl, ticker, startDate, endDate,
		// "Dividend");
		dataFile = new File(TempDataFilePath, ticker + "_split.csv");
		html = saveYahooInfo2File(ticker, startDt, endDate, "DividendSplit",
				dataFile);
		if (null == html)
			return;
		Document doc = Jsoup.parse(html);
		Elements TDs = doc.getElementsContainingOwnText("Stock Split");
		Element dtEle;
		StringBuffer sb = new StringBuffer();
		SimpleDateFormat splitSdf = new SimpleDateFormat("MMM dd, yyyy");
		// Feb
		// 23,
		// 1998
		for (Element td : TDs) {
			dtEle = td.parent().children().first();
			String dateStr = sdf.format(splitSdf.parse(dtEle.text()));
			String[] splitStr = td.text().replaceAll("Stock Split", "")
					.replaceAll("[\r\n]", "").split(":");
			sb.append(dateStr).append(",").append(ticker).append(",")
					.append(splitStr[0].trim()).append(",")
					.append(splitStr[1].trim()).append(Utils.LineSeparator);
			// /System.out.println("dateStr:" + dateStr +"::splitStr"+
			// Arrays.toString(splitStr));
		}
		Utils.writeTextToFile(dataFile, sb.toString());
		loadPriceDividendFileIntoMysql(dataFile.getAbsolutePath(),
				MysqlDividendSplitTableName, ticker);
	}

	/**
	 * InfoType: 'DailyPrice' for daily price type url, 'Dividend' for dividend
	 * info
	 * 
	 * @param ticker
	 * @param startDate
	 * @param endDate
	 * @param infoType
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static String getYahooHistoryUrl(String yahooBaseUrl, String ticker,
			Calendar startDate, Calendar endDate, String infoType) {
		StringBuffer sb = new StringBuffer(yahooBaseUrl);
		// s=YHOO&d=12&e=14&f=2013&g=d&a=3&b=12&c=1996&";
		// s=AAPL&a=00&b=2&c=1962&d=11&e=16&f=2013&g=v
		sb.append("&s=").append(URLEncoder.encode(ticker));
		sb.append("&a=").append(startDate.get(Calendar.MONTH));
		sb.append("&b=").append(startDate.get(Calendar.DAY_OF_MONTH));
		sb.append("&c=").append(startDate.get(Calendar.YEAR));
		sb.append("&d=").append(endDate.get(Calendar.MONTH));
		sb.append("&e=").append(endDate.get(Calendar.DAY_OF_MONTH));
		sb.append("&f=").append(endDate.get(Calendar.YEAR));
		if (infoType.equalsIgnoreCase("DailyPrice"))
			sb.append("&g=d"); // daily price type
		else if (infoType.equalsIgnoreCase("Dividend"))
			sb.append("&g=v"); // dividend
		// System.out.println("url:" + sb.toString());
		return sb.toString();
	}

	public static String getAdjcloseFromMySql(String ticker, Date date)
			throws Exception {
		String query = "SELECT adjClose from " + MysqlPriceTableName
				+ " where date=? and its=?";
		Connection conn = MysqlConnUtils.getConnection();
		PreparedStatement stmt = conn.prepareStatement(query);
		stmt.setDate(1, new java.sql.Date(date.getTime()));
		stmt.setString(2, ticker);
		ResultSet rs = stmt.executeQuery();
		String adjClose = null;
		if (rs.next()) {
			adjClose = rs.getString("adjClose");
		}
		rs.close();
		stmt.close();
		conn.close();
		return adjClose;
	}

	public static String getDividendFromMySql(String ticker, Date date)
			throws Exception {
		String query = "SELECT Dividend from " + MysqlDividendTableName
				+ " where date=? and its=?";
		Connection conn = MysqlConnUtils.getConnection();
		PreparedStatement stmt = conn.prepareStatement(query);
		stmt.setDate(1, new java.sql.Date(date.getTime()));
		stmt.setString(2, ticker);
		ResultSet rs = stmt.executeQuery();
		String dividend = null;
		if (rs.next()) {
			dividend = rs.getString("Dividend");
		}
		rs.close();
		stmt.close();
		conn.close();
		return dividend;
	}

	public static String[] getCSVRecord4MinDate(String csvData) {
		String[] recFirst = getCSVRecord(csvData, 1);
		String[] recLast = getCSVRecord(csvData, -1);
		if (null != recFirst) { // if first record is not null, then last record
								// will also be not null, since atleaset the csv
								// has a record which can be treated as last
								// record..
			int comp = recFirst[0].compareTo(recLast[0]);
			if (comp < 0)
				return recFirst;
			else
				return recLast;
		}
		return null;
	}

	public static String[] getCSVRecord4MaxDate(String csvData) {
		String[] recFirst = getCSVRecord(csvData, 1);
		String[] recLast = getCSVRecord(csvData, -1);
		if (null != recFirst) { // if first record is not null, then last record
								// will also be not null, since atleaset the csv
								// has a record which can be treated as last
								// record..
			int comp = recFirst[0].compareTo(recLast[0]);
			if (comp > 0)
				return recFirst;
			else
				return recLast;
		}
		return null;
	}

	private static String[] getCSVRecord(String csvData, int lineNo) {
		String[] record = null;
		if (lineNo == -1) { // last record
			int last1Idx = csvData.lastIndexOf(Utils.LineSeparator);
			if (last1Idx > 0) {
				int last2Idx = csvData.lastIndexOf(Utils.LineSeparator,
						last1Idx - 1);
				if (last2Idx >= 0 && last2Idx < last1Idx)
					record = new String(csvData.substring(last2Idx, last1Idx))
							.replaceAll(Utils.LineSeparator, "").split(",");
			}
		} else if (lineNo > 0) {
			int idx1 = csvData.indexOf(Utils.LineSeparator);
			for (int i = 1; i < lineNo && idx1 >= 0; i++)
				idx1 = csvData.indexOf(Utils.LineSeparator, idx1 + 1);
			if (idx1 >= 0) {
				int idx2 = csvData.indexOf(Utils.LineSeparator, idx1 + 1);
				if (idx2 >= 0 && idx2 > idx1)
					record = new String(csvData.substring(idx1, idx2))
							.replaceAll(Utils.LineSeparator, "").split(",");
			}
		}
		return record;
	}

	private String getPageHtml(String pageUrl) throws IOException {
		String html = null;
		try {
			html = httpClient.makeHttpRequest("Get", pageUrl, null, -1, null,
					null);
			if (httpClient.getResponseCode() == 404) // page not found..
				html = null;
		} catch (HttpHostConnectException e) { // if connection reset error,
												// retry once again..
			try {
				System.out.println("http err, wait 1 sec::"
						+ Thread.currentThread().getId());
				Thread.sleep(1000); // wait for 1 sec
			} catch (InterruptedException e1) {
			}
			// retry now..
			html = httpClient.makeHttpRequest("Get", pageUrl, null, -1, null,
					null);
		}
		return html;
	}

	private String saveYahooInfo2File(String ticker, Calendar startDate,
			Calendar endDate, String infoType, File dataFile) throws Exception {
		String url = getYahooHistoryUrl(YahooBaseUrl, ticker, startDate,
				endDate, infoType);
		if ("DividendSplit".equalsIgnoreCase(infoType)) {
			url = getYahooHistoryUrl(YahooBaseSplitUrl, ticker, startDate,
					endDate, infoType);
		} else
			url = getYahooHistoryUrl(YahooBaseUrl, ticker, startDate, endDate,
					infoType);
		String html = getPageHtml(url);
		if (null == html)
			return null;
		html = html.replaceAll("Adj Close", "AdjClose");
		// Date Open High Low Close Volume AdjClose Change ITS Time
		String[] itsLines = html.split(Utils.LineSeparator);
		itsLines[0] = itsLines[0] + ",Change,ITS,Time";
		StringBuffer sb = new StringBuffer();
		if ("DividendSplit".equalsIgnoreCase(infoType)) {
			sb.append(html);
		} else if ("Dividend".equalsIgnoreCase(infoType)) {
			for (int i = 1; i < itsLines.length; i++) {
				sb.append(itsLines[i]).append(",").append(ticker).append("\n");
						//.append(Utils.LineSeparator);
			}
		} else {
			for (int i = 1; i < itsLines.length; i++) {
				sb.append(itsLines[i]).append(",,").append(ticker).append(",,").append("\n");
						//.append(Utils.LineSeparator);
			}
		}
		Utils.writeTextToFile(dataFile, sb.toString());
		return html;
	}

	private void loadPriceDividendFileIntoMysql(String filePath,
			String tableName, String ticker) throws Exception {
		StringBuffer sb = new StringBuffer(
				"set sql_mode='NO_ENGINE_SUBSTITUTION';");
		sb.append("LOAD DATA INFILE '");
		sb.append(filePath.replaceAll("\\\\", "/"))
				.append("' REPLACE INTO TABLE  ").append(tableName);
		sb.append(" FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"' LINES TERMINATED BY '\n' ;");
		// sb.append(" (@date,Open,High,Low,Close,Volume,AdjClose) set date = STR_TO_DATE(@Date, '%Y-%m-%d'), its='"
		// + ticker + "'; ");
		if (ticker.equalsIgnoreCase("BWA"))
		System.out.println(sb.toString());
		MysqlConnUtils.executeQuery(sb.toString());
	}
	
	
	public String getStockDownloadUrl(String ticker, Date from, Date to) throws IOException {
		String pageUrl = YahooQuoteBaseUrl.replaceAll("@ticker@", ticker);
		String html = getPageHtml(pageUrl);
		FileSystemUtils.writeToAsciiFile("c:/temp/yahoo_bk.html", html);
		Document doc = Jsoup.parse(html, YahooQuoteBaseUrl);
		Elements anchor = doc.select("a[download=" +ticker+ ".csv]");
		if (null != anchor  &&  anchor.size() > 0) {
			Element a = anchor.get(0);
			String url = a.attr("href");
			// TODO: replace periods
			return url;
		}
		return null;
	}

	public static void main(String[] arg) throws Exception {
		YahooPriceDownloadWorker yp = new YahooPriceDownloadWorker();
		System.out.println(yp.getStockDownloadUrl("BK", new Date(), new Date()));
		// File f = new File(TempDataFilePath);
		// System.out.println(Arrays.toString(getAdjcloseFromCsv(f, true,
		// false)));
		// loadFileIntoMysql(TempDataFilePath, MysqlTmpPriceTableName, "AAPL");
		/*
		 * Calendar startDate = Calendar.getInstance(); startDate.set(2012, 0,
		 * 1); Calendar endDate = Calendar.getInstance(); endDate.set(2012, 3,
		 * 31); refreshPriceHistory("AAPL", startDate, endDate);
		 */
		// refreshPriceHistory("AAPL");
		// refreshDividendHistory("AAPL");

		// String csvData = Utils.readTextFromFile(TempDataFilePath);
		// System.out.println(Arrays.toString(getCSVRecord(csvData, -1)));

		/*
		 * Thread t = new Thread(new YahooPriceDownloadWorker("AAPL", null,
		 * null, null, null)); t.start(); t.join();
		 * System.out.println("done..");
		 */
	}
}
