package xbrl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

public class IBDList {

	public static void joinIBDColumns(String dateOfIBDList, String folder)
			throws IOException {
		// to convert IBD 197 group booklet to text -scan each column 1 by 1.
		// 4 scans per page! Convert to excel in able2extract and save as txt
		// file in excel. Save format is pg#c# -this will then put into one file
		// all columns. then review and cleanup the file by number each row so
		// as to not loose order and fixing symbols manually

		String text = "", indsName = "", line = "", cell1 = "", cell2 = "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 21; i++) {
			for (int c = 0; c < 4; c++) {
				String filename = "c:/backtest/ibd/20170201/txt/pg" + (i + 1)
						+ "c" + (c + 1) + ".txt";
				File file = new File(filename);

				if (!file.exists())
					continue;

				text = Utils.readTextFromFile(filename.toString());
				System.out.println("text=" + text);
				String[] textSplit = text.split("\r");
				for (int n = 0; n < textSplit.length; n++) {
					line = textSplit[n].replaceAll("[\r\n]", "");
					if (line.replaceAll("[\t ]", "").length() < 1)
						continue;
					sb.append(line + "\r\n");
				}
			}
		}

		PrintWriter pw = new PrintWriter(new File(folder + dateOfIBDList
				+ "ibdList.txt"));
		pw.append(sb.toString());
		pw.close();

	}

	public static void finalizeIBD(String dateOfIBDList, String folder,
			String quarter) throws IOException, SQLException {

		// final pass of manually prepared list -attaches inds to each symbols
		String text = Utils.readTextFromFile(folder + dateOfIBDList
				+ "list.txt");
		String[] textSplit = text.split("\r");
		String line = "", inds = null;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < textSplit.length; i++) {
			line = textSplit[i].replaceAll("[\r\n]", "");
			// System.out.println("line="+line);
			if (line.replaceAll("[\r\n\t ]", "").length() < 1)
				continue;
			// System.out.println("line.split len="+line.split("\t").length);
			if (line.split("\t").length == 1) {
				inds = line;
				continue;
			}
			sb.append(line + "\t" + inds + "\r\n");
		}
		System.out.println(sb.toString());
		PrintWriter pw = new PrintWriter(new File(folder + dateOfIBDList
				+ "final.txt"));
		pw.println(sb.toString());
		pw.close();

		String year = dateOfIBDList.substring(0, 4);
		String query = "DROP TABLE IF EXISTS ibd" + year + "q" + quarter
				+ ";\r" + "CREATE TABLE ibd" + year + "q" + quarter + " (\r"
				+ "`name` varchar(20) NOT NULL DEFAULT '',\r"
				+ "`ITS` varchar(20) NOT NULL DEFAULT '',\r"
				+ "`industry` varchar(255) NOT NULL DEFAULT '',\r"
				+ "PRIMARY KEY (`ITS`)"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r"
				+ "LOAD DATA INFILE '" + folder + dateOfIBDList
				+ "final.txt' \r" + "Ignore INTO TABLE IBD" + year + "q"
				+ quarter + "\rFIELDS TERMINATED BY '\t'\r"
				+ "LINES TERMINATED BY '\n'\r";

		MysqlConnUtils.executeQuery(query);

	}

	public static void insertIntoStockDataMissingIBDsymbols(
			String dateOfIBDList, String quarter, int month)
			throws SQLException, FileNotFoundException {

		String year = dateOfIBDList.substring(0, 4);
		// finds those ITS not in stockdata based on last price a month ago
		String query = "DROP TABLE IF EXISTS TMP_NEW_IBD_SYMBOLS;\r"
				+ "CREATE TABLE TMP_NEW_IBD_SYMBOLS ENGINE=MYISAM\r"
				+ "select its from stockdata where DATE>'"
				+ year
				+ "-"
				+ (month - 1)
				+ "-01' group by its;\r"
				+ "ALTER TABLE TMP_NEW_IBD_SYMBOLS ADD KEY (ITS);\r"
				+ "\r"
				+ "DROP TABLE IF EXISTS TMP_MISSING_IBD_SYMBOLS;\r"
				+ "CREATE TABLE TMP_MISSING_IBD_SYMBOLS ENGINE=MYISAM\r"
				+ "SELECT ITS FROM (\r"
				+ "select T1.ITS,T2.ITS ITS2 from ibd"
				+ year
				+ "q"
				+ quarter
				+ " "
				+ "t1 left join tmp_new_ibd_symbols t2 on t1.its=t2.its \r"
				+ ") T1 WHERE ITS2 IS NULL AND LENGTH(ITS)>0;\r"
				+ "\r"
				+ "INSERT IGNORE INTO STOCKDATA \r"
				+ "SELECT '"
				+ year
				+ "-"
				+ month
				+ "-01','','','','','','','',ITS,'' FROM TMP_MISSING_IBD_SYMBOLS;\r"
				+ "DROP TABLE IF EXISTS TMP_NEW_IBD_SYMBOLS;\r"
				+ "DROP TABLE IF EXISTS TMP_MISSING_IBD_SYMBOLS;\r";

		MysqlConnUtils.executeQuery(query);

	}

	public static void main(String[] args) throws IOException, SQLException {

		String dateOfIBDList = "20170201";
		String folder = "c:/backtest/ibd/" + dateOfIBDList + "/";
		//quarter date of IBD list is in.
		String quarter = "1";

		// to convert IBD 197 group booklet to text -scan each column 1 by 1.
		// 4 scans per page! Convert to excel in able2extract and save as txt
		// file in excel. Save format is pg#c# -this will then put into one file
		// all columns.Then run joinIBDColumns. able2Extract needs to be
		// purchased (bought at the time a 1 month subscr)
		joinIBDColumns(dateOfIBDList, folder);
		
		// then review and cleanup the file (number each row so
		// as to not loose order) and fix symbols manually

		// once list is cleanedup and at inds name is followed by name, symbol
		// column finalizeIBD will append industry to each name symbol
		finalizeIBD(dateOfIBDList, folder, quarter);
		// if date of IBD list is 2017-02-01 - quarter = 1 (q1).
		int month = 3; // <==today's month (month below is run)
		// insertIntoSto... puts new IBD symbols into stockdata so prices can be
		// downloaded
		insertIntoStockDataMissingIBDsymbols(dateOfIBDList, quarter, month);
		//
	}
}
