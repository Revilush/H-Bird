package xbrl;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class YahooPriceDownloader {

	private static String DummyTicker = "^GSPC";

	private int maxNoOfThreads = 4;
	private int queueSize = 100000;

	public YahooPriceDownloader() {
	}

	public YahooPriceDownloader(int maxNoOfThreads, int queueSize) {
		this.maxNoOfThreads = maxNoOfThreads;
		this.queueSize = queueSize;
	}

	public void addNewSymbol(String tickers) {
		String[] itsPty = tickers.split("[,\t:| ]");
		// 2013-12-18,2013-11-06,AAPL,550.77,3.05
		for (int i = 0; i < itsPty.length; i++) {
			itsPty[i] = "1970-01-01,1970-01-01," + itsPty[i].trim() + ",-1,-1";
		}
		updateTickers(itsPty);
	}

	public void downloadYahooPriceDivData() throws SQLException, IOException {

		File file = new File("c:/backtest/price/tmp_downloadProperties.txt");
		FileSystemUtils.createFoldersIfReqd("c:/backtest/price");
		if (file.exists()) {
			file.delete();
		}
		// this procedure creates the properties file the program uses to update
		String query = "CALL downloadProperties();";

		MysqlConnUtils.executeQuery(query);

		String[] itsPty = Utils.readTextFromFile(file.getAbsolutePath()).split(
				"\n");
		// 2013-12-18,2013-11-06,AAPL,550.77,3.05

		updateTickers(itsPty);
	}

	private void updateTickers(String[] itsPty) {
		RejectedExecutionHandlerImpl rejectionHandler = new RejectedExecutionHandlerImpl();
		ThreadPoolExecutor executorPool = new ThreadPoolExecutor(4,
				maxNoOfThreads, 10, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(queueSize),
				Executors.defaultThreadFactory(), rejectionHandler);
		String[] itsPtyItems;

		@SuppressWarnings("unused")
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		String lastDate = getLastDate4DummyTicker();
		System.out.println(lastDate);
		for (String itsLine : itsPty) {
			itsPtyItems = itsLine.split(",");
			// lastDatePrice,lastDateDiv,its,adjClose,t2.dividend
			// (, , double adjCloseOnStartDate, double dividendOnStartDate)
			YahooPriceDownloadWorker worker;
			try {
				worker = new YahooPriceDownloadWorker(itsPtyItems[2],
						itsPtyItems[0], itsPtyItems[1], itsPtyItems[4],
						itsPtyItems[3], lastDate);
				executorPool.execute(worker);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		executorPool.shutdown();
		try {
			executorPool.awaitTermination(10, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			// System.out.println("executorPool was terminated while waiting.."
			// + e.getMessage());
		}
	}

	public String getLastDate4DummyTicker() {
		// DummyTicker
		Calendar startDt = Calendar.getInstance();
		// this sets the startDt to 5 days prior to today's date.
		startDt.set(startDt.get(Calendar.YEAR), startDt.get(Calendar.MONTH),
				startDt.get(Calendar.DAY_OF_MONTH) - 5);
		Calendar endDate = Calendar.getInstance();
		// System.out.println("refreshing price data for " + ticker
		// + " for dates from:" + startDt.getTime() + " till:"
		// + endDate.getTime());

		@SuppressWarnings("unused")
		File dataFile = new File(YahooPriceDownloadWorker.TempDataFilePath,
				DummyTicker + ".csv");
		String url = YahooPriceDownloadWorker.getYahooHistoryUrl(
				YahooPriceDownloadWorker.YahooBaseUrl, DummyTicker, startDt,
				endDate, "DailyPrice");
		EasyHttpClient httpClient = new EasyHttpClient(false);
		String html = null;
		try {
			html = httpClient.makeHttpRequest("Get", url, null, -1, null, null);
			String[] csvLines = YahooPriceDownloadWorker
					.getCSVRecord4MaxDate(html);
			if (null != csvLines)
				return csvLines[0];
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	static class RejectedExecutionHandlerImpl implements
			RejectedExecutionHandler {
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			System.out.println("**** job " + r.toString() + " is rejected");
		}
	}

	public static void main(String[] arg) throws SQLException, IOException {
		/*
		 * Button: Price, dividend and split data =>update or add new symbols -
		 * if you click add new symbol - box to type in symbols.
		 */
		YahooPriceDownloader ypd = new YahooPriceDownloader();
		ypd.downloadYahooPriceDivData();
		// downloadYahooPrice fetches from mysql a download
		// properties file which is used to determine lastDate/last
		// adjClose for each ITS in order to know for each ITS which
		// date to update from and if last adjClose previously
		// downloaded does not match with adjclose of same date
		// retrieved from yahoo server to redownload entire history (for all:
		// price/div/split).
	}
}
