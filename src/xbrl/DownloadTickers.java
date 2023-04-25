package xbrl;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;

public class DownloadTickers {

	public static void downloadCSIsymbols(String server, String exchange)
			throws IOException {

		System.out.println("Connecting to FTP server...");

		System.out.println("site:: " + server);
		String localPath = "c:/backtest/symbols/";

		URL url = null;
		try {
			url = new URL(server);
			System.out.println("url:: " + url);
		} catch (MalformedURLException e) {
			System.out.println("new url");
			e.printStackTrace();
		}

		URLConnection con = null;
		try {
			System.out.println("open conection");
			con = url.openConnection();
			if (con == null) {
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(con.getInputStream());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		System.out.println("Downloading file.");

		FileOutputStream out = null;
		try {
			FileSystemUtils.createFoldersIfReqd(localPath);
			out = new FileOutputStream(localPath + "/" + exchange);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		int i = 0;
		byte[] bytesIn = new byte[1024];
		try {
			while ((i = in.read(bytesIn)) >= 0) {// while the input stream is
													// "reading" bytesIn>0
				out.write(bytesIn, 0, i);// then write.
			}
		} catch (IOException e) {

			e.printStackTrace();
		}
		try {
			out.close();
		} catch (IOException e) {

			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("File downloaded.");

	}
	
	public static void downloadTickersCSI() throws IOException, SQLException {
		
		/*
		 * NASDAQ & OTC:
		 * http://www.csidata.com/factsheets.php?type=stock&format=csv&exchangeid=88
		 * NYSE Mkt:
		 * http://www.csidata.com/factsheets.php?type=stock&format=csv&exchangeid=80
		 * NYSE:
		 * http://www.csidata.com/factsheets.php?type=stock&format=csv&exchangeid=79
		 * LSE:
		 * http://www.csidata.com/factsheets.php?type=stock&format=csv&exchangeid=84
		 * Toronto Stock Exchange
		 * http://www.csidata.com/factsheets.php?type=stock&format=csv&exchangeid=82
		 * Toronto venture exchange
		 * http://www.csidata.com/factsheets.php?type=stock&format=csv&exchangeid=83
		 */
		
		String[] exchanges = { "NASDAQ", "NYSE", "NYSE_MKT", "LSE", "TSE",
				"TVE","indexes","ETFs","Notes" };
		String[] servers = {
				"http://www.csidata.com/factsheets.php?type=stock&format=csv&exchangeid=NYSE",
				"http://www.csidata.com/factsheets.php?type=stock&format=csv&exchangeid=AMEX",
				"http://www.csidata.com/factsheets.php?type=stock&format=csv&exchangeid=NASDAQ"/*,
				"http://www.csidata.com/factsheets.php?type=stock&format=csv&exchangeid=84",
				"http://www.csidata.com/factsheets.php?type=stock&format=csv&exchangeid=82",
				"http://www.csidata.com/factsheets.php?type=stock&format=csv&exchangeid=83",
				"http://www.csidata.com/factsheets.php?type=stock&format=csv&exchangeid=81",
				"http://www.csidata.com/factsheets.php?type=stock&format=csv&isetf=1",
				"http://www.csidata.com/factsheets.php?type=stock&format=csv&isetn=1"*/
				};
		
		String query= "DROP TABLE IF EXISTS `bac_csi_symbols` ;\r"+
				"CREATE TABLE `bac_csi_symbols` (\r"+
				"  `CsiNumber` int(11) DEFAULT 0,\r"+
				"  `ITS` varchar(50) DEFAULT '',\r"+
				"  `Name` varchar(70) DEFAULT NULL,\r"+
				"  `Exchange` varchar(50) DEFAULT NULL,\r"+
				"  `IsActive` int(11) DEFAULT NULL,\r"+
				"  `StartDate` date DEFAULT NULL,\r"+
				"  `EndDate` date DEFAULT '1901-01-01',\r"+
				"  `ConversionFactor` int(11) DEFAULT NULL,\r"+
				"  `SwitchCfDate` VARCHAR(50) DEFAULT NULL,\r"+
				"  `PreSwitchCf` varchar(50) DEFAULT NULL,\r"+
				"  `SubExchange` varchar(50) DEFAULT NULL,\r"+
				"  PRIMARY KEY (CsiNumber),\r"+
				"  KEY (ITS),\r"+
				"  KEY (ENDDATE)\r"+
				") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r"+
				"\r"+
				"insert ignore into bac_csi_symbols\r"+
				"select * from csi_symbols;\r"+
				"\r"+
				"DROP TABLE IF EXISTS `csi_symbols` ;\r"+
				"CREATE TABLE `csi_symbols` (\r"+
				"  `CsiNumber` int(11) DEFAULT 0,\r"+
				"  `ITS` varchar(50) DEFAULT '',\r"+
				"  `Name` varchar(70) DEFAULT NULL,\r"+
				"  `Exchange` varchar(50) DEFAULT NULL,\r"+
				"  `IsActive` int(11) DEFAULT NULL,\r"+
				"  `StartDate` date DEFAULT NULL,\r"+
				"  `EndDate` date DEFAULT '1901-01-01',\r"+
				"  `ConversionFactor` int(11) DEFAULT NULL,\r"+
				"  `SwitchCfDate` VARCHAR(50) DEFAULT NULL,\r"+
				"  `PreSwitchCf` varchar(50) DEFAULT NULL,\r"+
				"  `SubExchange` varchar(50) DEFAULT NULL,\r"+
				"  PRIMARY KEY (CsiNumber),\r"+
				"  KEY (ITS),\r"+
				"  KEY (ENDDATE)\r"+
				") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r";

		MysqlConnUtils.executeQuery(query);

		for(int i=0; i<servers.length; i++){
			System.out.println("server="+servers[i]);
		downloadCSIsymbols(servers[i], exchanges[i]);
		
			query = "\r\rLOAD DATA INFILE 'c:/backtest/symbols/" + exchanges[i]
					+ "' " + "\rIgnore INTO TABLE csi_symbols "
					+ "\rFIELDS TERMINATED BY ','"
					+ "\rLINES TERMINATED BY '\n'" + "\r IGNORE 1 LINES ;";

		MysqlConnUtils.executeQuery(query);

		}
		
		MysqlConnUtils
				.executeQuery("UPDATE csi_symbols SET NAME=REPLACE(NAME,'\\\"',\"\");");

		
	}

	public static void main(String[] args) throws IOException, SQLException {
		
		downloadTickersCSI();
		
	}

}
