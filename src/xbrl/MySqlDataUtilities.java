package xbrl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

import xbrl.MysqlConnUtils;

public class MySqlDataUtilities {

	public static void getAndCleanupInsiderSymbols() throws SQLException, FileNotFoundException {

		Connection conn = MysqlConnUtils.getConnection();
		Statement stmt = conn.createStatement();
		String query;

		query = "select * from i_symbols_cleanup;";

		ResultSet rs = stmt.executeQuery(query);

		StringBuffer sb = new StringBuffer();

		while (rs.next()) {

			for (int i = 1; i <= 25; i++) {
					sb.append(rs.getString(i));
//				System.out.println("sb.toString each column="+sb.toString());
			}
		}
		MysqlConnUtils.executeQuery(sb.toString());
		rs.close();
	}
	
	public static void getMissingAccnos(int startYr, int endYr, String tablePrefix) throws SQLException, FileNotFoundException {
		
		String table = "", query ="";
		int yr = startYr;
		int startQ = 1, endQ = 4, q = startQ;
		int qtr = startQ;
		
		MysqlConnUtils.executeQuery("truncate tmp_msgAccTpRaw;\r");
		
		for (yr = startYr; yr <= endYr; yr++) {
			if (yr == 1993 && qtr < 3) {
				qtr = 3;
			}
			else qtr=1;

			for (q = qtr; q <= endQ; q++) {
				
				table = tablePrefix + yr + "qtr" + q;
				
				query = "insert ignore into tmp_msgAccTpRaw\r"
						+ "select t1.`date filed`,t1.accno,t1.form from (\r\n" + 
						"select t1.`date filed`,t1.accno,t2.acc2,t1.form "
						+ "from tpidx2 t1 left join \r"
						+ "(select accno acc2 from "+table+" group by accno) t2\r\n" + 
						"on t1.accno=t2.acc2\r\n" + 
						"where year(t1.`date filed`) = "+yr+" and "
								+ "quarter(t1.`date filed`)="+q+" ) t1 \r" + 
						"where acc2 is null and form not rlike '/A';\r\n" ;

					MysqlConnUtils
					 .executeQuery(query);
					
			}
		}
		
		System.out.println("check table - tmp_msgAccTpRaw for missing accno\r"
				+ "select count(*) cnt, year(t1.`date filed`) year, quarter(t1.`date filed`) q \r"
				+ "from tmp_msgAccTpRaw t1\r"
				+ "group by year(`date filed`),quarter(t1.`date filed`);\r" + "");
	}
	
	public static void exportMysqlTableToCsv(String table, String whereStatement){
		

		// fetch the data from mysql..
		String query = "select T1.* from "+table+" t1 "+whereStatement+";";
		System.out.println(query);
		try {

			Connection conn = MysqlConnUtils.getConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			StringBuilder sb = new StringBuilder();
			
			while (rs.next()) {
				for (int col = 1; col <= rsmd.getColumnCount(); col++) {
					if(rs.getString(rsmd.getColumnLabel(col))==null) {
						sb.append(",");
						continue;
					}
					sb.append((rs.getString(rsmd.getColumnLabel(col))).replaceAll("[\r\n]", ""));
					if(col+1<rsmd.getColumnCount()){
						sb.append(",");
					}
				}
				sb.append("\r");
			}
			rs.close();
			stmt.close();
			conn.close();
			String folder = "c:/backtest/export/";
			Utils.createFoldersIfReqd(folder);
			PrintWriter pw = new PrintWriter(new File(folder+table+".csv"));
			pw.append(sb.toString());
			pw.close();

		}catch(Exception e) {
			e.printStackTrace(System.out);
		}
	}
	
	public static void importAllFilesInFolderIntoMysql(String path)
			throws SQLException, IOException {
		// filename must be same as tablename (w/o the .txt extension). File
		// must end in .txt

		File folder = new File(path);

		File[] listOfFiles = folder.listFiles();
		
		System.out.println("listOfFiles.len=");

		if (listOfFiles == null)
			return;

		NLP nlp = new NLP();
		for (File file : listOfFiles) {
			if(nlp.getAllMatchedGroups(file.getName(), Pattern.compile(".txt")).size()<1)
				continue;
			
			String query = "truncate "+file.getName().replace(".txt", "")+";\r" +
					"LOAD DATA INFILE '" + path + file.getName()
					+ "'\r ignore INTO TABLE "
					+ file.getName().replace(".txt", "") + "\r"
					+ "FIELDS TERMINATED BY ','\r"
					+ "LINES TERMINATED BY '\r\n';\r";

			MysqlConnUtils.executeQuery(query);

		}
	}
	

	public static void main(String[] args) throws SQLException, IOException {
		// getAndCleanupInsiderSymbols();
		// getMissingAccnos(2005,2005,"bac_tp_raw");

		importAllFilesInFolderIntoMysql("c:/backtest/export/");
		
	}
}
