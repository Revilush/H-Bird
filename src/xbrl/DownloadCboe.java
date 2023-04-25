package xbrl;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

//TODO: Need to copy over 8k bsnWire data to local drive c:/backtest/8k/2014/QTR1 ... QTR3 and reparse locally using BusinessWire

public class DownloadCboe {

	public static void PutCallEquity(String newsWireUrl) throws IOException {

		String localPath = "c:/backtest/cboe/cboeURLs.txt";
		System.out.println("localPath="+localPath);
		
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
	
	public static void downloadCBOE() throws IOException, SQLException {
		

		PutCallEquity("http://www.cboe.com/publish/ScheduledTask/MktData/datahouse/equityPC.csv");

		String query = "\rLOAD DATA INFILE 'c:/backtest/cboe/cboeURLs.txt' "
				+ "\rignore INTO TABLE o_cboe_putcall_equity"
				+ "\rFIELDS TERMINATED BY ','"
				+ "\rLINES TERMINATED BY '\n' \rIGNORE 3 LINES "
				+ "\r(@date,callvolume,putvolume,totalvolume,putcallratio) "
				+ "\rset date = STR_TO_DATE(@Date, '%m/%d/%Y');\r"
				+ "\rcall o_wk_cboe;";
		
		MysqlConnUtils.executeQuery(query);

		
	}

	public static void main(String[] args) throws ParseException, IOException,
			SQLException {
		
		downloadCBOE();

		// below will retrieve the 2 archived put call data files from cboe.

		// file#1 (still equity data and in same format - so use same query)
		// PutCallEquity("http://www.cboe.com/publish/ScheduledTask/MktData/datahouse/equityPCarchive.csv");
		// MysqlConnUtils.executeQuery(query);
		//
		// file#2: (total putCall data and just ratio info)
		// PutCallEquity("http://www.cboe.com/publish/ScheduledTask/MktData/datahouse/pcratioarchive.csv");
		//
		// query = "\rLOAD DATA INFILE 'c:/backtest/cboe/cboeURLs.txt' "
		// + "\rignore INTO TABLE o_cboe_putcall_equity"
		// + "\rFIELDS TERMINATED BY ','"
		// + "\rLINES TERMINATED BY '\n'\rIGNORE 3 LINES "
		// + "\r(@date,putcallratio) "
		// + "\rset date = STR_TO_DATE(@Date, '%m/%d/%Y');\r";
		//
		// MysqlConnUtils.executeQuery(query);

	}
}
