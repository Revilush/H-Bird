package xbrl;


import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

//TODO: Need to copy over 8k bsnWire data to local drive c:/backtest/8k/2014/QTR1 ... QTR3 and reparse locally using BusinessWire

public class DownloadAdvDecl {

	public static void AdvanceDecline(String newsWireUrl, String exchange,
			String type) throws IOException {

		String localPath = "c:/backtest/AdvanceDecline/" + exchange + "_"
				+ type + ".csv";
		System.out.println("localPath:" + localPath);
		
		File file = new File(localPath);
		if (file.exists()) {
			file.delete();
		} else {
			System.err.println("I cannot find '" + file + "' ('"
					+ file.getAbsolutePath() + "')");
		}

		file.createNewFile();
		Utils.createFoldersIfReqd(localPath.substring(0,
				localPath.lastIndexOf("/") + 1));

		// businessWire Url with links to capture and file to save to
		Rss.download(newsWireUrl, localPath);
	}
	
	public static void downloadAdvDecline() throws SQLException, IOException {
		
		// below retrieve entire history for all adv
		// (vol&count)/decl(vol&count)/unch(vol&count)/newHi/newLo for nyse,amex
		// and nasdaq (24 datapoints). Entire history is brief/small - primary
		// key is date in tables and only new data is inserted.

		// ad_calc procedure (advance decline calc table) inserts new data into
		// tables ad_calc_nyse, ad_calc_nasdaq and ad_calc_amex and pre-calc
		// various ratios

		System.out.println("start");
		AdvanceDecline("http://unicorn.us.com/advdec/NASDAQ_advn.csv",
				"NASDAQ", "Advancers");
		AdvanceDecline("http://unicorn.us.com/advdec/NASDAQ_advv.csv",
				"NASDAQ", "AdvancersVolume");
		AdvanceDecline("http://unicorn.us.com/advdec/NASDAQ_decln.csv",
				"NASDAQ", "Decliners");
		AdvanceDecline("http://unicorn.us.com/advdec/NASDAQ_declv.csv",
				"NASDAQ", "DeclinersVolume");
		AdvanceDecline("http://unicorn.us.com/advdec/NASDAQ_newhi.csv",
				"NASDAQ", "newHi");
		AdvanceDecline("http://unicorn.us.com/advdec/NASDAQ_newlo.csv",
				"NASDAQ", "newLo");
		AdvanceDecline("http://unicorn.us.com/advdec/NASDAQ_unchn.csv",
				"NASDAQ", "Unchanged");
		AdvanceDecline("http://unicorn.us.com/advdec/NASDAQ_unchv.csv",
				"NASDAQ", "UnchangedVolume");
		AdvanceDecline("http://unicorn.us.com/advdec/AMEX_advn.csv", "AMEX",
				"Advancers");
		AdvanceDecline("http://unicorn.us.com/advdec/AMEX_advv.csv", "AMEX",
				"AdvancersVolume");
		AdvanceDecline("http://unicorn.us.com/advdec/AMEX_decln.csv", "AMEX",
				"Decliners");
		AdvanceDecline("http://unicorn.us.com/advdec/AMEX_declv.csv", "AMEX",
				"DeclinersVolume");
		AdvanceDecline("http://unicorn.us.com/advdec/AMEX_newhi.csv", "AMEX",
				"newHi");
		AdvanceDecline("http://unicorn.us.com/advdec/AMEX_newlo.csv", "AMEX",
				"newLo");
		AdvanceDecline("http://unicorn.us.com/advdec/AMEX_unchn.csv", "AMEX",
				"Unchanged");
		AdvanceDecline("http://unicorn.us.com/advdec/AMEX_unchv.csv", "AMEX",
				"UnchangedVolume");

		AdvanceDecline("http://unicorn.us.com/advdec/NYSE_advn.csv", "NYSE",
				"Advancers");
		AdvanceDecline("http://unicorn.us.com/advdec/NYSE_advv.csv", "NYSE",
				"AdvancersVolume");
		AdvanceDecline("http://unicorn.us.com/advdec/NYSE_decln.csv", "NYSE",
				"Decliners");
		AdvanceDecline("http://unicorn.us.com/advdec/NYSE_declv.csv", "NYSE",
				"DeclinersVolume");
		AdvanceDecline("http://unicorn.us.com/advdec/NYSE_newhi.csv", "NYSE",
				"newHi");
		AdvanceDecline("http://unicorn.us.com/advdec/NYSE_newlo.csv", "NYSE",
				"newLo");
		AdvanceDecline("http://unicorn.us.com/advdec/NYSE_unchn.csv", "NYSE",
				"Unchanged");
		AdvanceDecline("http://unicorn.us.com/advdec/NYSE_unchv.csv", "NYSE",
				"UnchangedVolume");

		String query = "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/NASDAQ_Advancers.csv' ignore INTO TABLE advDecl_NASDAQ_Advancers FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r"
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/NASDAQ_AdvancersVolume.csv' ignore INTO TABLE ADVDECL_NASDAQ_Advancers_Volume FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/NASDAQ_Decliners.csv' ignore INTO TABLE ADVDECL_NASDAQ_Decliners FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/NASDAQ_DeclinersVolume.csv' ignore INTO TABLE ADVDECL_NASDAQ_Decliners_Volume FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/NASDAQ_Unchanged.csv' ignore INTO TABLE ADVDECL_NASDAQ_Unchanged FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/NASDAQ_UnchangedVolume.csv' ignore INTO TABLE ADVDECL_NASDAQ_Unchanged_Volume FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r  "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/NASDAQ_newHi.csv' ignore INTO TABLE newHi_NASDAQ FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r  "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/NASDAQ_newLo.csv' ignore INTO TABLE newLo_NASDAQ FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r  "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/AMEX_Advancers.csv' ignore INTO TABLE advDecl_AMEX_Advancers FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r"
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/AMEX_AdvancersVolume.csv' ignore INTO TABLE ADVDECL_AMEX_Advancers_Volume FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/AMEX_Decliners.csv' ignore INTO TABLE ADVDECL_AMEX_Decliners FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/AMEX_DeclinersVolume.csv' ignore INTO TABLE ADVDECL_AMEX_Decliners_Volume FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/AMEX_Unchanged.csv' ignore INTO TABLE ADVDECL_AMEX_Unchanged FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/AMEX_UnchangedVolume.csv' ignore INTO TABLE ADVDECL_AMEX_Unchanged_Volume FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r  "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/AMEX_newHi.csv' ignore INTO TABLE newHi_amex FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r  "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/AMEX_newLo.csv' ignore INTO TABLE newLo_amex FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r  "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/NYSE_Advancers.csv' ignore INTO TABLE advDecl_NYSE_Advancers FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r"
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/NYSE_AdvancersVolume.csv' ignore INTO TABLE ADVDECL_NYSE_Advancers_Volume FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/NYSE_Decliners.csv' ignore INTO TABLE ADVDECL_NYSE_Decliners FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/NYSE_DeclinersVolume.csv' ignore INTO TABLE ADVDECL_NYSE_Decliners_Volume FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/NYSE_Unchanged.csv' ignore INTO TABLE ADVDECL_NYSE_Unchanged FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/NYSE_UnchangedVolume.csv' ignore INTO TABLE ADVDECL_NYSE_Unchanged_Volume FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r  "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/NYSE_newHi.csv' ignore INTO TABLE newHi_nyse FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r  "
				+ "LOAD DATA INFILE 'c:/backtest/AdvanceDecline/NYSE_newLo.csv' ignore INTO TABLE newLo_nyse FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n';\r "
				+ "\rCALL ad_calc();";

		MysqlConnUtils.executeQuery(query);

	}

	public static void main(String[] args) throws ParseException, IOException,
			SQLException {
		
		downloadAdvDecline();
		
	}
}
