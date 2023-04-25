package xbrl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

public class GAAPTaxonomyParser {

	public String localPath = "c:/grabber/fasb/";

	/*
	 * public static void ugt2012Concepts(String file) throws IOException,
	 * ParseException, SAXException {
	 * 
	 * BufferedReader rdr = null;
	 * 
	 * try { rdr = new BufferedReader(new FileReader(file)); } catch
	 * (FileNotFoundException e1) { // TODO Auto-generated catch block
	 * e1.printStackTrace(); }
	 * 
	 * @SuppressWarnings("unused") String line ;
	 * 
	 * try { while ((line = rdr.readLine()) != null) { //
	 * System.out.println(line); //String[] items = line.split("\t"); //
	 * System.out.println("[0]: " + items[0]); } } catch (IOException e) {
	 * e.printStackTrace(System.out); } finally {
	 * 
	 * if (null != rdr) { rdr.close();
	 * 
	 * String query = "TRUNCATE UGT2012Concepts;"; String query1 =
	 * "LOAD DATA INFILE '" + file.replaceAll("\\\\", "/") +
	 * "' ignore INTO TABLE UGT2012Concepts FIELDS TERMINATED BY '\t' " +
	 * "(`#`,`prefix`,`name`,`type`,`enumerations`,`substitutionGroup`," +
	 * "`balance`,`periodType`,`abstract`,`label`,`documentation`,`deprecatedLabel`"
	 * + ",`deprecatedDateLabel`,`changeLabel2012`);"; try {
	 * MysqlConnUtils.executeQuery(query + query1); } catch (SQLException e) {
	 * e.printStackTrace(System.out); } } } }
	 * 
	 * public static void ugt2013Concepts(String file) throws IOException,
	 * ParseException, SAXException { // this method there is no manipulation of
	 * data - it works fine.
	 * 
	 * BufferedReader rdr = null;
	 * 
	 * try { rdr = new BufferedReader(new FileReader(file)); } catch
	 * (FileNotFoundException e1) { // TODO Auto-generated catch block
	 * e1.printStackTrace(); }
	 * 
	 * @SuppressWarnings("unused") String line;
	 * 
	 * try { while ((line = rdr.readLine()) != null) { //
	 * System.out.println(line); //String[] items = line.split("\t"); //
	 * System.out.println("[0]: " + items[0]); } } catch (IOException e) {
	 * e.printStackTrace(System.out); } finally {
	 * 
	 * if (null != rdr) { rdr.close();
	 * 
	 * String query = "TRUNCATE UGT2013Concepts;"; String query1 =
	 * "LOAD DATA INFILE '" + file.replaceAll("\\\\", "/") +
	 * "' ignore INTO TABLE UGT2013Concepts FIELDS TERMINATED BY '\t' " +
	 * "(`#`,`prefix`,`name`,`type`,`enumerations`,`substitutionGroup`," +
	 * "`balance`,`periodType`,`abstract`,`label`,`documentation`,`deprecatedLabel`"
	 * + ",`deprecatedDateLabel`,`changeLabel2012`,`changeLabel2013`);"; try {
	 * MysqlConnUtils.executeQuery(query + query1); } catch (SQLException e) {
	 * e.printStackTrace(System.out); } } } }
	 * 
	 * public static void ugtLinks(String file, String fields) throws
	 * IOException, ParseException, SAXException { // this bufferedReader I need
	 * to manipulate the data before I pass it to // mysql. BufferedReader rdr =
	 * null; List<String> smallerRows = new ArrayList<String>(); try { rdr = new
	 * BufferedReader(new FileReader(file)); } catch (FileNotFoundException e1)
	 * { // TODO Auto-generated catch block e1.printStackTrace(); } String line;
	 * String roleFileName = file.substring(0, file.lastIndexOf("/") + 1) +
	 * "temp.txt"; PrintWriter preLinkPw = new PrintWriter(roleFileName); try
	 * {// would like to separate fasb but only when it occurrs String
	 * httpLinkRole = ""; String roleName = ""; String[] items;
	 * 
	 * while ((line = rdr.readLine()) != null) { // System.out.println(line);
	 * items = line.split("\t"); if (line.startsWith("LinkRole")) { httpLinkRole
	 * = items[1]; roleName = httpLinkRole.substring(httpLinkRole
	 * .lastIndexOf("/") + 1); continue; } if (items.length < 10) continue; //
	 * ignore this line.. and go back to the while // loop (next line) if
	 * (line.startsWith("prefix")) { // ignore continue; } else { if
	 * (items.length < 3) { smallerRows.add(line); continue; } // save it here..
	 * preLinkPw.print(httpLinkRole + "\t" + roleName); for (int i = 0; i <
	 * items.length; i++) preLinkPw.print("\t" + items[i]); preLinkPw.println();
	 * // prints blank new line } } System.out.println("Smaller rows: " +
	 * Arrays.toString(smallerRows.toArray()));
	 * 
	 * } catch (IOException e) { e.printStackTrace(System.out); } finally {
	 * 
	 * if (null != preLinkPw) { preLinkPw.close(); }
	 * 
	 * if (null != rdr) { rdr.close();
	 * 
	 * String tableName = file.substring(file.lastIndexOf("/") + 1,
	 * file.lastIndexOf(".")); String query = "Truncate " + tableName + ";";
	 * String query1 =
	 * "LOAD DATA INFILE 'c:/grabber/fasb/temp.txt' ignore INTO TABLE " +
	 * tableName + "  FIELDS TERMINATED BY '\\t' " + fields + ";";
	 * 
	 * System.out.println(query + query1);
	 * 
	 * try { MysqlConnUtils.executeQuery(query + query1); } catch (SQLException
	 * e) { e.printStackTrace(System.out); } } } }
	 */

	public static void getGaapDefLinkTaxonomies(String filename, String year)
			throws IOException, SQLException {

		// reformat so that rolename is associated with each line.

		System.out.println("filename=" + filename);

		String text = Utils.readTextFromFile(filename), cell = "", line = "", rolename = "" 
//				,lineItem = ""
				;
		String[] textLines = text.split("[\r\n]");
		StringBuilder sb = new StringBuilder();
//		StringBuilder sbLineItems = new StringBuilder();
		int depth=0, order=0; 
		for (int i = 0; i < textLines.length; i++) {
			line = textLines[i];
			// System.out.println("initial line="+line+"initialLineEnd");
			if (line.toLowerCase().contains("prefix")) {
				continue;
			}
			if (line.toLowerCase().contains("linkrole")) {
				rolename = line.split("\t")[1];
				// System.out.println("rolename="+rolename+"||END");
				continue;
			}
			if (line.replaceAll("[\r\n\t]", "").length() < 1)
				continue;
			// System.out.println("line="+line + "\t" + rolename + "ENDline\r");

			String[] cells = line.split("\t");
			int a = 0;
			for (; a < cells.length; a++) {
				if(cells.length>3){
					depth = Integer.parseInt(cells[3]);
					order = Integer.parseInt(cells[4]);
				}

				cell = "";
				cell = cells[a];
				if (a == 6) {
					// us-gaap:
					cell = cell.replaceAll("us-gaap:", "");
				}
				if (a == 7) {
					cell = cell.substring(cell.lastIndexOf("/") + 1);
				}
				if (a > 0)
					sb.append("\t" + cell);
				else
					sb.append(cell);
			}

			if (a == 12) {
				sb.append(rolename.substring(rolename.lastIndexOf("/") + 1)
						+ "\r");
			} else {
				for (int c = 0; c < (13 - a); c++) {
					sb.append("\t");
				}
			}
			sb.append("\t" + rolename.substring(rolename.lastIndexOf("/") + 1)
					+ "\r");

		}

		// us-gaap-2009-def_link.txt
		File file = new File("c:/grabber/fasb/us-gaap-"+year+"-def_link.txt");
		PrintWriter pw = new PrintWriter(file);
		pw.println(sb.toString());
		pw.close();

		System.out.println("file.size=" + file.length() + " file.getAbpath"
				+ file.getAbsolutePath());

		String query = "LOAD DATA INFILE '"
				+ file.getAbsolutePath().replace("\\", "\\/")
				+ "' Ignore INTO TABLE ugt" + year + "def_link\r"
				+ "FIELDS TERMINATED BY '\\t'" + "LINES TERMINATED BY '\r'";

		MysqlConnUtils.executeQuery(query);
		// System.out.println("sb.toString="+sb.toString());
	}

	public static void createUGTtables() throws SQLException, FileNotFoundException {

		String query = "DROP TABLE IF EXISTS ugt2009def_link;\r"
				+ "CREATE TABLE `ugt2009def_link` (\r"
				+ "  `prefix` varchar(15) DEFAULT NULL,\r"
				+ "  `name` varchar(255) NOT NULL DEFAULT '',\r"
				+ "  `label` text,\r"
				+ "  `depth` double NOT NULL,\r"
				+ "  `order` double NOT NULL,\r"
				+ "  `priority` double DEFAULT NULL,\r"
				+ "  `parent` varchar(255) NOT NULL DEFAULT '',\r"
				+ "  `arcrole` varchar(255) DEFAULT NULL,\r"
				+ "  `targetRole` varchar(255) DEFAULT NULL,\r"
				+ "  `usable` varchar(60) DEFAULT NULL,\r"
				+ "  `closed` varchar(60) DEFAULT NULL,\r"
				+ "  `contextElement` varchar(60) DEFAULT NULL,\r"
				+ "  `systemid` varchar(60) DEFAULT NULL,\r"
				+ "  `rolename` varchar(255) NOT NULL DEFAULT '',\r"
				+ "  PRIMARY KEY(rolename,name,parent),\r"
				+ "  KEY `name` (`name`),\r"
				+ "  KEY `rolename` (`rolename`)\r"
				+ ") ENGINE=MyISAM AUTO_INCREMENT=29868 DEFAULT CHARSET=latin1;"
				+ "DROP TABLE IF EXISTS ugt2012def_link;\r"
				+ "CREATE TABLE `ugt2012def_link` (\r"
				+ "  `prefix` varchar(15) DEFAULT NULL,\r"
				+ "  `name` varchar(255) NOT NULL DEFAULT '',\r"
				+ "  `label` text,\r"
				+ "  `depth` double NOT NULL,\r"
				+ "  `order` double NOT NULL,\r"
				+ "  `priority` double DEFAULT NULL,\r"
				+ "  `parent` varchar(255) NOT NULL DEFAULT '',\r"
				+ "  `arcrole` varchar(255) DEFAULT NULL,\r"
				+ "  `targetRole` varchar(255) DEFAULT NULL,\r"
				+ "  `usable` varchar(60) DEFAULT NULL,\r"
				+ "  `closed` varchar(60) DEFAULT NULL,\r"
				+ "  `contextElement` varchar(60) DEFAULT NULL,\r"
				+ "  `systemid` varchar(60) DEFAULT NULL,\r"
				+ "  `rolename` varchar(255) NOT NULL DEFAULT '',\r"
				+ "  PRIMARY KEY(rolename,name,parent),\r"
				+ "  KEY `name` (`name`),\r"
				+ "  KEY `rolename` (`rolename`)\r"
				+ ") ENGINE=MyISAM AUTO_INCREMENT=29868 DEFAULT CHARSET=latin1;\r";

		MysqlConnUtils.executeQuery(query);

	}
	
	public static void getLineItemsGroup() throws IOException{
		
		Utils.readTextFromFile("c:/grabber/fasb/");
		
		for(int i=0; i<15; i++){
			
		}
		
	}

	public static void main(String[] args) throws IOException, SQLException {

		
		createUGTtables();//run once
		getGaapDefLinkTaxonomies("c:/grabber/fasb/UGT2009Def_Link.txt",
				"2009");

		getGaapDefLinkTaxonomies("c:/grabber/fasb/UGT2012Def_Link.txt",
				"2012");
		// TODO Auto-generated method stub
		/*
		 * try {
		 * SaxParserUGT.ugt2012Concepts("c:/grabber/fasb/UGT2012Concepts.txt");
		 * SaxParserUGT.ugt2013Concepts("c:/grabber/fasb/UGT2013Concepts.txt");
		 * String localPath = "c:/grabber/fasb/"; String path2012Cal = localPath
		 * + "UGT2012Cal_link.txt"; String path2013Cal = localPath +
		 * "UGT2013Cal_link.txt"; String path2012Pre = localPath +
		 * "UGT2012Pre_link.txt"; String path2013Pre = localPath +
		 * "UGT2013Pre_link.txt"; String path2012Def = localPath +
		 * "UGT2012Def_link.txt"; String path2013Def = localPath +
		 * "UGT2013Def_link.txt"; String calFields =
		 * "(linkrole,rolename,prefix,name,label,depth,`order`,priority," +
		 * "parent,arcrole,systemid)"; String preFields =
		 * "(linkrole,rolename,prefix,name,label,depth,`order`,priority," +
		 * "parent,arcrole,preferredLabel)"; String defFields =
		 * "(linkrole,rolename,prefix,name,label,depth,`order`,priority,parent,arcrole,"
		 * + "targetRole,usable,closed,contextElement,systemid)";
		 * 
		 * SaxParserUGT.ugtLinks(path2012Cal, calFields);
		 * SaxParserUGT.ugtLinks(path2013Cal, calFields);
		 * SaxParserUGT.ugtLinks(path2012Pre, preFields);
		 * SaxParserUGT.ugtLinks(path2013Pre, preFields);
		 * SaxParserUGT.ugtLinks(path2012Def, defFields);
		 * SaxParserUGT.ugtLinks(path2013Def, defFields);
		 * 
		 * } catch (IOException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } catch (ParseException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); } catch (SAXException
		 * e) { // TODO Auto-generated catch block e.printStackTrace(); }
		 */
	}

}
