package xbrl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

public class XbrlTickers {

	public static String folder = "c:/backtest/xbrl/";

	public static void getXbrlTickerAndsAcceptedDate(String yr, String q)
			throws IOException, SQLException {

		int year = Integer.parseInt(yr);
		int qtr = Integer.parseInt(q);
		String accno, acceptedDate = "", its = "", textFile;
		StringBuilder sb = new StringBuilder();
		File files = new File(folder + year + "/QTR" + q + "/");
		System.out.println("files folder=" + files.getAbsolutePath());
		File[] listOfFiles = files.listFiles();
		for (File file : listOfFiles) {
			accno = "";
			acceptedDate = "";
			its = "";
			textFile = "";
			if (file.getAbsolutePath().contains(".txt")) {
				accno = file.getName()
						.substring(0, file.getName().indexOf("."));
				textFile = Utils.readTextFromFile(file.getAbsolutePath());
				if (textFile.split("\\|\\|").length > 0) {
					acceptedDate = textFile.split("\\|\\|")[0].replaceAll(
							"[\r\n]", "");
				}

				if (textFile.split("\\|\\|").length > 1) {
					its = textFile.split("\\|\\|")[1].replaceAll("[\r\n]", "");
				}

				if (accno != null && accno.length() > 19) {
					
					sb.append(accno + "||" + acceptedDate + "||" + its + "\n");
					
				}
				// System.out.println("accno="+accno+" its="+its+" acceptedDate="+acceptedDate);
			}
		}

		File file = new File("c:/backtest/xbrl/" + year + "/QTR" + qtr
				+ "/xbrl" + year + "q" + qtr + "_its.csv");

		PrintWriter pw = new PrintWriter(file);
		pw.println(sb.toString());
		pw.close();

		String query = "LOAD Data INFILE '"
				+ file.getAbsolutePath().replaceAll("\\\\", "/")
				+ "' ignore INTO TABLE " + "xbrl_its"
				+ " FIELDS TERMINATED BY '||' lines terminated by '\\n';";

		MysqlConnUtils.executeQuery(query);

	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws SQLException
	 */

	public static void main(String[] args) throws IOException, SQLException {

		@SuppressWarnings("resource")
		Scanner Scan = new Scanner(System.in);
		System.out.println("Enter year");
		String yr = Scan.nextLine();

		System.out.println("Enter quarter");
		String q = Scan.nextLine();
		getXbrlTickerAndsAcceptedDate(yr, q);
		
	}
}
