package xbrl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

//Program will automatically grab last date from mysql and next date as startDate

//TODO: have mysql produce missing dates and have it retrieve those dates.

public class DownloadStockPriceAndOptions {

	private EasyFTPClient ftpClient = null;

	public DownloadStockPriceAndOptions() throws SocketException, IOException {
		ftpClient = new EasyFTPClient("ftp.tbsp.com", -1, "71988FIN",
				"71988FIN", false);
	}

	public void downloadDailyOption(Calendar startCal, Calendar endCal,
			String folder) throws FileNotFoundException {
		String folderOptions = "c:/backtest/options/";
		FileSystemUtils.createFoldersIfReqd(folderOptions);
		
		String OptionsUrl = "Quotes_Daily/daily_options_BA/";
		// 00_index.txt
		if (null == endCal)
			endCal = Calendar.getInstance();
		if (null == startCal)
			startCal = Calendar.getInstance();
		Calendar startDate = Calendar.getInstance();
		startDate.set(startCal.get(Calendar.YEAR),
				startCal.get(Calendar.MONTH), startCal.get(Calendar.DATE));
		Calendar endDate = Calendar.getInstance();
		endDate.set(endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH),
				endCal.get(Calendar.DATE));
		String filename;
		if (!folder.endsWith("/"))
			folder += "/";
		SimpleDateFormat sdf = new SimpleDateFormat("MMddyy");
		File tgtFile;
		while (startDate.before(endDate) || startDate.equals(endDate)) {
			// filename convention: oa<mmddyy>.zip for ex:"oa010214.zip"
			filename = "oa" + sdf.format(startDate.getTime()) + ".zip";
			tgtFile = new File(folder, filename);
			if (!tgtFile.exists()) {
				try {
					ftpClient.copyToLocal(OptionsUrl + filename,
							tgtFile.getAbsolutePath(), false);

					if (tgtFile.exists()) {
						ZipUtils.deflateZipFile(tgtFile.getAbsolutePath(),
								folder);
					}

					String fileName = tgtFile.getAbsolutePath().substring(0,
							tgtFile.getAbsolutePath().lastIndexOf("."))
							+ ".csv";
					File f = new File(fileName);
					System.out.println(f);
					if (f.exists()) {
						fileName = fileName.replace("\\", "//");

						 System.out.println(fileName);

						String query = "LOAD DATA INFILE '"
								+ folderOptions
								+ f.getName()
								+ "' ignore INTO TABLE NEWOD3 FIELDS TERMINATED BY ',';  call o_its_update();";
						
						try {
							MysqlConnUtils.executeQuery(query);
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}

				} catch (IOException e) {
					System.out
							.println("Error while downloading Options file for date:"
									+ startDate.getTime());
					e.printStackTrace(System.out);
				}
			} else {
				if (tgtFile.exists()) {
					try {
						ZipUtils.deflateZipFile(tgtFile.getAbsolutePath(),
								folder);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				System.out.println("option file already exists, reloading into mysql:"
						+ tgtFile.getAbsolutePath());
				String fileName = tgtFile.getAbsolutePath().substring(0,
						tgtFile.getAbsolutePath().lastIndexOf("."))
						+ ".csv";
				fileName = fileName.replace("\\", "//");
				String query = "LOAD DATA INFILE '"
						+ fileName
						+ "' ignore INTO TABLE NEWOD3 FIELDS TERMINATED BY ',';  call o_its_update();";
				try {
					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

			startDate.add(Calendar.DATE, 1);
		}
		System.out.println("options download done..");
	}
	
	public void downloadDailyStockPrice(Calendar startCal, Calendar endCal,
			String folder) throws FileNotFoundException {
		String folderStockPrices = "c:/backtest/stockprices/";
		FileSystemUtils.createFoldersIfReqd(folderStockPrices);
		
		String OptionsUrl = "Quotes_Daily/daily_stocks/";
		// 00_index.txt
		if (null == endCal)
			endCal = Calendar.getInstance();
		if (null == startCal)
			startCal = Calendar.getInstance();
		Calendar startDate = Calendar.getInstance();
		startDate.set(startCal.get(Calendar.YEAR),
				startCal.get(Calendar.MONTH), startCal.get(Calendar.DATE));
		Calendar endDate = Calendar.getInstance();
		endDate.set(endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH),
				endCal.get(Calendar.DATE));
		String filename;
		if (!folder.endsWith("/"))
			folder += "/";
		SimpleDateFormat sdf = new SimpleDateFormat("MMddyy");
		File tgtFile;
		while (startDate.before(endDate) || startDate.equals(endDate)) {
			// filename convention: oa<mmddyy>.zip for ex:"oa010214.zip"
			filename = "s" + sdf.format(startDate.getTime()) + ".zip";
			System.out.println("tbsp file="+filename);
			tgtFile = new File(folder, filename);
			if (!tgtFile.exists()) {
				try {
					ftpClient.copyToLocal(OptionsUrl + filename,
							tgtFile.getAbsolutePath(), false);

					if (tgtFile.exists()) {
						ZipUtils.deflateZipFile(tgtFile.getAbsolutePath(),
								folder);
					}

					String fileName = tgtFile.getAbsolutePath().substring(0,
							tgtFile.getAbsolutePath().lastIndexOf("."))
							+ ".csv";
					File f = new File(fileName);
					System.out.println(f);
					if (f.exists()) {
						fileName = fileName.replace("\\", "//");
						
						 System.out.println(fileName);

						String query = "DROP TABLE IF EXISTS TMP_STOCKDATA;\r"
								+ "CREATE TABLE `tmp_stockdata` (\r"
								+ "  `ITS` varchar(25) NOT NULL DEFAULT '',\r"
								+ "  `Date` date NOT NULL DEFAULT '0000-00-00',\r"
								+ "  `Open` double NOT NULL DEFAULT '0',\r"
								+ "  `High` double NOT NULL DEFAULT '0',\r"
								+ "  `Low` double NOT NULL DEFAULT '0',\r"
								+ "  `Close` double NOT NULL DEFAULT '0',\r"
								+ "  `Volume` double NOT NULL DEFAULT '0'\r"
								+ "  ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\r"
								+ "LOAD DATA INFILE '"
								+ folderStockPrices
								+ f.getName()
								+ "' ignore INTO TABLE TMP_STOCKDATA \r"
								+ "FIELDS TERMINATED BY ','\r"
								+ "(@var1,@var2,open,high,low,close,volume)\r"
								+ "SET its=replace(replace(replace(replace(@var1,'&',''),' A','.a'),' B','.b'),' ','-'),\r"
								+ "date = str_to_date(@var2, '%m/%d/%Y');"
								+ "\rinsert ignore into stockdata \r"
								+ "select date,open,high,low,close,volume,'',''," +
								"case when right(its,1)='-' then replace(its,'-','+A') when its rlike '-' and right(its,2)!='-W' then replace(its,'-','+') else its end its" +
								",'' from tmp_stockdata \r" +
								"where its not rlike '[/\\$@]';\r"
								+ "\rDROP TABLE IF EXISTS TMP_STOCKDATA;\r";

						try {
							MysqlConnUtils.executeQuery(query);
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}

				} catch (IOException e) {
					System.out
							.println("Error while downloading stock prices file for date:"
									+ startDate.getTime());
					e.printStackTrace(System.out);
				}
			} else {
				if (tgtFile.exists()) {
					try {
						ZipUtils.deflateZipFile(tgtFile.getAbsolutePath(),
								folder);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				System.out.println("file already exists, reloading into mysql:"
						+ tgtFile.getAbsolutePath());
				String fileName = tgtFile.getAbsolutePath().substring(0,
						tgtFile.getAbsolutePath().lastIndexOf("."))
						+ ".csv";
				fileName = fileName.replace("\\", "//");
				
				String query = "DROP TABLE IF EXISTS TMP_STOCKDATA;\r"
						+ "CREATE TABLE `tmp_stockdata` (\r"
						+ "  `ITS` varchar(25) NOT NULL DEFAULT '',\r"
						+ "  `Date` date NOT NULL DEFAULT '0000-00-00',\r"
						+ "  `Open` double NOT NULL DEFAULT '0',\r"
						+ "  `High` double NOT NULL DEFAULT '0',\r"
						+ "  `Low` double NOT NULL DEFAULT '0',\r"
						+ "  `Close` double NOT NULL DEFAULT '0',\r"
						+ "  `Volume` double NOT NULL DEFAULT '0'\r"
						+ "  ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\r" +
						"LOAD DATA INFILE '"
						+ folderStockPrices
						+ fileName
						+ "' ignore INTO TABLE TMP_STOCKDATA \r"
						+ "FIELDS TERMINATED BY ','\r"
						+ "(@var1,@var2,open,high,low,close,volume)\r"
						+ "SET its=replace(replace(replace(replace(@var1,'&',''),' A','.a'),' B','.b'),' ','-'),\r"
						+ "date = str_to_date(@var2, '%m/%d/%Y');"
						+ "\rinsert ignore into stockdata \r"
						+ "select date,open,high,low,close,volume,'',''," +
						"case when right(its,1)='-' then replace(its,'-','+A') when its rlike '-' and right(its,2)!='-W' then replace(its,'-','+') else its end its" +
						",'' from tmp_stockdata \r" +
						"where its not rlike '[/\\$@]';\r"
						+ "\rDROP TABLE IF EXISTS TMP_STOCKDATA;\r";
				
				try {
					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

			startDate.add(Calendar.DATE, 1);
		}
		System.out.println("stock prices download done..");
	}
	
	public static void getStockAndOptionFromTBSP() throws SQLException, SocketException, IOException {
		
		String qry = "DROP TABLE IF EXISTS TMP_STOCKDATA;\r"
				+ "CREATE TABLE `tmp_stockdata` (\r"
				+ "  `ITS` varchar(25) NOT NULL DEFAULT '',\r"
				+ "  `Date` date NOT NULL DEFAULT '0000-00-00',\r"
				+ "  `Open` double NOT NULL DEFAULT '0',\r"
				+ "  `High` double NOT NULL DEFAULT '0',\r"
				+ "  `Low` double NOT NULL DEFAULT '0',\r"
				+ "  `Close` double NOT NULL DEFAULT '0',\r"
				+ "  `Volume` double NOT NULL DEFAULT '0'\r"
				+ "  ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\r"
				+ "LOAD DATA INFILE '"
//				+ folderStockPrices
//				+ f.getName()
				+ "' ignore INTO TABLE TMP_STOCKDATA \r"
				+ "FIELDS TERMINATED BY ','\r"
				+ "(@var1,@var2,open,high,low,close,volume)\r"
				+ "SET its=replace(replace(replace(replace(@var1,'&',''),' A','.a'),' B','.b'),' ','-'),\r"
				+ "date = str_to_date(@var2, '%m/%d/%Y');"
				+ "\rinsert ignore into stockdata \r"
				+ "select date,open,high,low,close,volume,'',''" +
				",case when right(its,1)='-' then replace(its,'-','+A') when its rlike '-' and right(its,2)!='-W' then replace(its,'-','+') else its end its" +
				",'' from tmp_stockdata \r" +
				"where its not rlike '[/\\$@]';\r"
				+ "DROP TABLE IF EXISTS TMP_STOCKDATA;\r";

		// csi is bf.a / bf.b (only for '.a/.b') tbsp is 'bf a' / 'bf b'. tbsp
		// format in tbsp is 'aig w'. csi's format is 'aig-w'. aig w ==> aig-w.
		// convert when importing tbsp ' a' and ' b' to '.a' and '.b'
		// convert when importing tbsp ' [C-Z]' CONVERT TO [+[C-Z]
		
		System.out.println(qry);
		Calendar st = Calendar.getInstance();
		DownloadStockPriceAndOptions od = new DownloadStockPriceAndOptions();

		String query = 	"";
		query = 
				"SELECT DATE_add(MAX(DATE), INTERVAL 1 DAY) startDate FROM stockdata;";
		System.out.println(query);
		st.setTime(AutoHistoryUpdator.getStartDate(query));
		od.downloadDailyStockPrice(st, null, "c:/backtest/stockprices/");

		query = "SELECT DATE_add(MAX(DATE), INTERVAL 1 DAY) startDate FROM NEWOD ;";
		System.out.println(query);
		st.setTime(AutoHistoryUpdator.getStartDate(query));
		od.downloadDailyOption(st, null, "c:/backtest/options");
		MysqlConnUtils.executeQuery("call o_wk_calc;");

		
		File folder = new File("c:/backtest/options/");
		File[] listOfFiles = folder.listFiles();
		for (File f : listOfFiles) {
			query = "LOAD DATA INFILE '"+"c:/backtest/options/"
					+ f.getName()
					+ "' ignore INTO TABLE NEWOD3 FIELDS TERMINATED BY ',';  call o_its_update();";
			
			MysqlConnUtils.executeQuery(query);

			
		}

	}

	public static void main(String[] arg) throws SocketException, IOException, SQLException {
		
//		ftp://ftp.tbsp.com/Programs_For_All/Programs/splits.txt

		getStockAndOptionFromTBSP();

	}
}
