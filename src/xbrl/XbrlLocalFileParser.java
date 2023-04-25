package xbrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipFile;

public class XbrlLocalFileParser {

	public static String baseFolder = "c:/backtest/xbrl/";
	public static String qQtr = "";
	public static String yYear = "";

	public static void dateRangeQuarters(Calendar startDate, Calendar endDate)
			throws SocketException, IOException, SQLException {

		int startYear = startDate.get(Calendar.YEAR);
		int endYear = endDate.get(Calendar.YEAR);
		int startQtr = XbrlDownloader.getQuarter(startDate);
		int endQtr = XbrlDownloader.getQuarter(endDate);
		String localPath = baseFolder + "/" + endYear + "/QTR" + endQtr;

		int iYear = (endYear - startYear) * 4;
		int iQtr = (endQtr - startQtr) + 1;
		int totalQtrs = iYear + iQtr;
		iYear = startYear;
		iQtr = startQtr;
		Calendar cal = Calendar.getInstance();

		for (int i = 1; i <= totalQtrs; i++) {
			yYear = iYear + "";
			qQtr = iQtr + "";
			
			XbrlDownloader.moveToXbrlPermanentTable(iYear + "", iQtr + "");
			XbrlDownloader.dropTmpMySqlTablestoParseXML(iYear + "", iQtr + "");
			XbrlLocalFileParser.createXbrlTmp_Tables(iYear + "", iQtr + "");
			
			String query = XbrlDownloader.createTmpXbrlMySqlTablestoParseXML(
					iYear + "", iQtr + "");
			
			try {
				MysqlConnUtils.executeQuery(query);
			} catch (SQLException e) {
				e.printStackTrace(System.out);
			}

			cal.set(Calendar.YEAR, iYear);
			cal.set(Calendar.MONTH, (iQtr * 3) - 1);
			localPath = baseFolder + "/" + iYear + "/QTR" + iQtr;
			String mysqlLocalPath = baseFolder + "/" + iYear + "/QTR" + iQtr
					+ "/xbrl.csv";
			File f = new File(mysqlLocalPath);
			if (f.exists()) {
				f.delete();
				createMysqlCsv(mysqlLocalPath, iYear, iQtr);
			} else if (!f.exists()) {
				createMysqlCsv(mysqlLocalPath, iYear, iQtr);
			}

			String zipFilePath = localPath + "/" + iYear + "/Qtr" + iQtr
					+ ".zip";
			File file = new File(zipFilePath);
			if (file.exists()) {
				ZipUtils.deflateZipFile(zipFilePath, localPath);
			}
			if (!file.exists()) {
				Utils.createFoldersIfReqd("c:/backtest/master/" + iYear
						+ "/QTR" + iQtr + "/");
				HttpDownloadUtility.downloadFile(
						"https://www.sec.gov/Archives/edgar/full-index/"
								+ iYear + "/QTR" + iQtr + "/xbrl.idx",
						"c:/backtest/master/" + iYear + "/QTR" + iQtr + "/");

			}
//			System.out.println("localPath::" + localPath);

			try {
				MysqlConnUtils.executeQuery(query);
			} catch (SQLException e) {
				e.printStackTrace(System.out);
			}

			parseXbrlZipFiling(localPath, iYear + "", iQtr + "");
			XbrlDownloader.moveToXbrlPermanentTable(iYear + "", iQtr + "");
			XbrlDownloader.dropTmpMySqlTablestoParseXML(iYear + "", iQtr + "");
			XbrlDownloader.dropTmpMySqlTablestoParseXML(iYear + "", iQtr + "");
			FinancialStatement.updateXbrlFilerInfo(localPath);
			//FinancialStatement.getFinancials(localPath);

			//uncomment
MysqlConnUtils.executeQuery("DROP TABLE IF EXISTS `tmp_" + iYear
					+ "q" + iQtr + "xbrl_cal_arc`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + iYear + "q" + iQtr
					+ "xbrl_cal_link`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ iYear + "q" + iQtr + "xbrl_cal_loc`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + iYear + "q" + iQtr
					+ "xbrl_cal_roleref`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ iYear + "q" + iQtr + "xbrl_def_arc`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + iYear + "q" + iQtr
					+ "xbrl_def_arcrole`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ iYear + "q" + iQtr + "xbrl_def_link`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + iYear + "q" + iQtr
					+ "xbrl_def_loc`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ iYear + "q" + iQtr + "xbrl_def_roleref`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + iYear + "q" + iQtr
					+ "xbrl_ins_context`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ iYear + "q" + iQtr + "xbrl_ins_data`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + iYear + "q" + iQtr
					+ "xbrl_ins_qatr`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ iYear + "q" + iQtr + "xbrl_ins_text`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + iYear + "q" + iQtr
					+ "xbrl_ins_unit`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ iYear + "q" + iQtr + "xbrl_lab_arc`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + iYear + "q" + iQtr
					+ "xbrl_lab_lab`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ iYear + "q" + iQtr + "xbrl_lab_link`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + iYear + "q" + iQtr
					+ "xbrl_lab_loc`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ iYear + "q" + iQtr + "xbrl_lab_roleref`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + iYear + "q" + iQtr
					+ "xbrl_pre_arc`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ iYear + "q" + iQtr + "xbrl_pre_link`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + iYear + "q" + iQtr
					+ "xbrl_pre_loc`;\r" + " DROP TABLE IF EXISTS `tmp_"
					+ iYear + "q" + iQtr + "xbrl_pre_roleref`;\r"
					+ " DROP TABLE IF EXISTS `tmp_" + iYear + "q" + iQtr
					+ "xbrl_xsd_roletype`;\r");

			iQtr++;
			if (iQtr > 4) {
				iYear++;
				iQtr = 1;
			}
		}
	}

	public static void createXbrlTmp_Tables(String year, String qtr)
			throws SQLException, FileNotFoundException {
		// createXbrlTmpTables(String iYear, String iQtr);

		String query = "DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_cal_arc`;\r" + "CREATE TABLE `tmp_" + year + "q" + qtr
				+ "xbrl_cal_arc`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `cal_link_role_name` varchar(255) NOT NULL,\r"
				+ " `cal_arc_Parent` varchar(255) DEFAULT NULL,\r"
				+ " `cal_arc_Child` varchar(255) DEFAULT NULL,\r"
				+ " `cal_arc_order` double DEFAULT NULL,\r"
				+ " `cal_arc_Weight` double DEFAULT NULL,\r"
				+ " `cal_arc_Priority` varchar(255) DEFAULT NULL,\r"
				+ " `cal_arc_Use` varchar(255) DEFAULT NULL,\r"
				+ " `cal_arc_type` varchar(255) DEFAULT NULL,\r"
				+ " `cal_arc_arcRole` varchar(255) DEFAULT NULL,\r"
				+ " `cal_arc_title` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_cal_link`;\r" + "CREATE TABLE `tmp_" + year + "q" + qtr
				+ "xbrl_cal_link`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `cal_link_role` varchar(255) DEFAULT NULL,\r"
				+ " `cal_link_role_name` varchar(255) DEFAULT NULL,\r"
				+ " `cal_link_type` varchar(255) DEFAULT NULL,\r"
				+ " `cal_link_title` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_cal_loc`;\r" + "CREATE TABLE `tmp_" + year + "q" + qtr
				+ "xbrl_cal_loc`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `cal_link_role_name` varchar(255) NOT NULL,\r"
				+ " `cal_loc_label` varchar(255) DEFAULT NULL,\r"
				+ " `cal_loc_href` varchar(255) DEFAULT NULL,\r"
				+ " `cal_loc_href_prefix` varchar(255) DEFAULT NULL,\r"
				+ " `cal_loc_href_name` varchar(255) DEFAULT NULL,\r"
				+ " `cal_loc_type` varchar(255) DEFAULT NULL,\r"
				+ " `cal_loc_title` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_cal_roleref`;\r" + "CREATE TABLE `tmp_" + year + "q"
				+ qtr + "xbrl_cal_roleref`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `cal_rr_roleURI` varchar(255) DEFAULT NULL,\r"
				+ " `cal_rr_roleURI_Name` varchar(255) DEFAULT NULL,\r"
				+ " `cal_rr_href` varchar(255) DEFAULT NULL,\r"
				+ " `cal_rr_type` varchar(255) DEFAULT NULL,\r"
				+ " `cal_rr_title` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_def_arc`;\r" + "CREATE TABLE `tmp_" + year + "q" + qtr
				+ "xbrl_def_arc`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `def_link_role_name` varchar(255) DEFAULT NULL,\r"
				+ " `def_arc_Parent` varchar(255) DEFAULT NULL,\r"
				+ " `def_arc_Child` varchar(255) DEFAULT NULL,\r"
				+ " `def_arc_order` double DEFAULT NULL,\r"
				+ " `def_arc_type` varchar(255) DEFAULT NULL,\r"
				+ " `def_arc_arcRole` varchar(255) DEFAULT NULL,\r"
				+ " `def_arc_title` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_def_arcrole`;\r" + "CREATE TABLE `tmp_" + year + "q"
				+ qtr + "xbrl_def_arcrole`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `def_arcrr_roleURI` varchar(255) DEFAULT NULL,\r"
				+ " `def_arcrr_roleURI_Name` varchar(255) DEFAULT NULL,\r"
				+ " `def_arcrr_href` varchar(255) DEFAULT NULL,\r"
				+ " `def_arcrr_href_name` varchar(255) DEFAULT NULL,\r"
				+ " `def_arcrr_type` varchar(255) DEFAULT NULL,\r"
				+ " `def_arcrr_title` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_def_link`;\r" + "CREATE TABLE `tmp_" + year + "q" + qtr
				+ "xbrl_def_link`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `def_link_role` varchar(255) DEFAULT NULL,\r"
				+ " `def_link_role_name` varchar(255) DEFAULT NULL,\r"
				+ " `def_link_type` varchar(255) DEFAULT NULL,\r"
				+ " `def_link_title` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_def_loc`;\r" + "CREATE TABLE `tmp_" + year + "q" + qtr
				+ "xbrl_def_loc`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `def_link_Role_Name` varchar(255) NOT NULL,\r"
				+ " `def_loc_label` varchar(255) DEFAULT NULL,\r"
				+ " `def_loc_href` varchar(255) DEFAULT NULL,\r"
				+ " `def_loc_href_prefix` varchar(255) DEFAULT NULL,\r"
				+ " `def_loc_href_name` varchar(255) DEFAULT NULL,\r"
				+ " `def_loc_type` varchar(255) DEFAULT NULL,\r"
				+ " `def_loc_title` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_def_roleref`;\r" + "CREATE TABLE `tmp_" + year + "q"
				+ qtr + "xbrl_def_roleref`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `def_rr_roleURI` varchar(255) DEFAULT NULL,\r"
				+ " `def_rr_roleURI_Name` varchar(255) DEFAULT NULL,\r"
				+ " `def_rr_href` varchar(255) DEFAULT NULL,\r"
				+ " `def_rr_type` varchar(255) DEFAULT NULL,\r"
				+ " `def_rr_title` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_ins_context`;\r" + "CREATE TABLE `tmp_" + year + "q"
				+ qtr + "xbrl_ins_context`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `id` varchar(255) NOT NULL,\r"
				+ " `startDate` date DEFAULT '1900-01-01',\r"
				+ " `endDate` date DEFAULT '1900-01-01',\r"
				+ " `instant` date DEFAULT '1900-01-01',\r"
				+ " `segment` varchar(255) DEFAULT NULL,\r"
				+ " `dimension` varchar(255) DEFAULT NULL,\r"
				+ " `dimensionValue` varchar(255) DEFAULT NULL,\r"
				+ " `CIK` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_ins_data`;\r" + "CREATE TABLE `tmp_" + year + "q" + qtr
				+ "xbrl_ins_data`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `prefix` varchar(255) NOT NULL,\r"
				+ " `Name` varchar(255) DEFAULT NULL,\r"
				+ " `value` varchar(255) DEFAULT NULL,\r"
				+ " `contextRef` varchar(255) DEFAULT NULL,\r"
				+ " `unitRef` varchar(255) DEFAULT NULL,\r"
				+ " `id` varchar(255) DEFAULT NULL,\r"
				+ " `decimals` double DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_ins_qatr`;\r" + "CREATE TABLE `tmp_" + year + "q" + qtr
				+ "xbrl_ins_qatr`\r" + " ( `rowno` double DEFAULT '0',\r"
				+ " `AN_Id` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) DEFAULT NULL,\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `attribute` varchar(255) DEFAULT NULL,\r"
				+ " `localName` varchar(255) DEFAULT NULL,\r"
				+ " `qName` varchar(255) DEFAULT NULL,\r"
				+ " `URI` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_ins_text`;\r" + "CREATE TABLE `tmp_" + year + "q" + qtr
				+ "xbrl_ins_text`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `prefix` varchar(255) NOT NULL,\r"
				+ " `Name` varchar(255) DEFAULT NULL,\r" + " `value` text,\r"
				+ " `contextRef` varchar(255) DEFAULT NULL,\r"
				+ " `unitRef` varchar(255) DEFAULT NULL,\r"
				+ " `id` varchar(255) DEFAULT NULL,\r"
				+ " `decimals` double DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_ins_unit`;\r" + "CREATE TABLE `tmp_" + year + "q" + qtr
				+ "xbrl_ins_unit`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `id` varchar(255) NOT NULL,\r"
				+ " `measure` varchar(255) DEFAULT NULL,\r"
				+ " `divide` varchar(255) DEFAULT NULL,\r"
				+ " `Denominator` varchar(255) DEFAULT NULL,\r"
				+ " `Numerator` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_lab_arc`;\r" + "CREATE TABLE `tmp_" + year + "q" + qtr
				+ "xbrl_lab_arc`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `lab_link_Role_Name` varchar(255) NOT NULL,\r"
				+ " `lab_arc_Parent` varchar(255) DEFAULT NULL,\r"
				+ " `lab_arc_Child` varchar(255) DEFAULT NULL,\r"
				+ " `lab_arc_type` varchar(255) DEFAULT NULL,\r"
				+ " `lab_arc_arcRole` varchar(255) DEFAULT NULL,\r"
				+ " `lab_arc_order` double DEFAULT '0',\r"
				+ " `lab_arc_title` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_lab_lab`;\r" + "CREATE TABLE `tmp_" + year + "q" + qtr
				+ "xbrl_lab_lab`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `lab_link_Role_Name` varchar(255) NOT NULL,\r"
				+ " `lab_lab_role` varchar(255) DEFAULT NULL,\r"
				+ " `lab_lab_value` varchar(255) DEFAULT NULL,\r"
				+ " `lab_lab_label` varchar(255) DEFAULT NULL,\r"
				+ " `lab_lab_lang` varchar(255) DEFAULT NULL,\r"
				+ " `lab_lab_id` varchar(255) DEFAULT NULL,\r"
				+ " `lab_lab_type` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_lab_link`;\r" + "CREATE TABLE `tmp_" + year + "q" + qtr
				+ "xbrl_lab_link`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `lab_link_role` varchar(255) DEFAULT NULL,\r"
				+ " `lab_link_role_name` varchar(255) DEFAULT NULL,\r"
				+ " `lab_Link_type` varchar(255) DEFAULT NULL,\r"
				+ " `lab_Link_title` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_lab_loc`;\r" + "CREATE TABLE `tmp_" + year + "q" + qtr
				+ "xbrl_lab_loc`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `lab_link_Role_Name` varchar(255) NOT NULL,\r"
				+ " `lab_loc_label` varchar(255) DEFAULT NULL,\r"
				+ " `lab_loc_href` varchar(255) DEFAULT NULL,\r"
				+ " `lab_loc_href_prefix` varchar(255) DEFAULT NULL,\r"
				+ " `lab_loc_href_name` varchar(255) DEFAULT NULL,\r"
				+ " `lab_loc_type` varchar(255) DEFAULT NULL,\r"
				+ " `lab_loc_title` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_lab_roleref`;\r" + "CREATE TABLE `tmp_" + year + "q"
				+ qtr + "xbrl_lab_roleref`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `lab_rr_roleURI` varchar(255) DEFAULT NULL,\r"
				+ " `lab_rr_roleURI_Name` varchar(255) DEFAULT NULL,\r"
				+ " `lab_rr_href` varchar(255) DEFAULT NULL,\r"
				+ " `lab_rr_type` varchar(255) DEFAULT NULL,\r"
				+ " `lab_rr_title` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_pre_arc`;\r" + "CREATE TABLE `tmp_" + year + "q" + qtr
				+ "xbrl_pre_arc`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `pre_link_role_name` varchar(255) DEFAULT NULL,\r"
				+ " `pre_arc_Parent` varchar(255) DEFAULT NULL,\r"
				+ " `pre_arc_Child` varchar(255) DEFAULT NULL,\r"
				+ " `pre_arc_preferredLabel` varchar(255) DEFAULT NULL,\r"
				+ " `pre_arc_pLabel` varchar(255) DEFAULT NULL,\r"
				+ " `pre_arc_order` double DEFAULT NULL,\r"
				+ " `pre_arc_type` varchar(255) DEFAULT NULL,\r"
				+ " `pre_arc_arcRole` varchar(255) DEFAULT NULL,\r"
				+ " `pre_arc_title` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_pre_link`;\r" + "CREATE TABLE `tmp_" + year + "q" + qtr
				+ "xbrl_pre_link`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `formType` varchar(20) DEFAULT '0',\r"
				+ " `ITS` varchar(20) DEFAULT NULL,\r"
				+ " `pre_link_role` varchar(255) DEFAULT NULL,\r"
				+ " `pre_link_role_name` varchar(255) DEFAULT NULL,\r"
				+ " `pre_link_type` varchar(255) DEFAULT NULL,\r"
				+ " `pre_link_title` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_pre_loc`;\r" + "CREATE TABLE `tmp_" + year + "q" + qtr
				+ "xbrl_pre_loc`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `pre_link_role_name` varchar(255) DEFAULT NULL,\r"
				+ " `pre_Loc_label` varchar(255) DEFAULT NULL,\r"
				+ " `pre_Loc_href` varchar(255) DEFAULT NULL,\r"
				+ " `pre_Loc_href_prefix` varchar(255) DEFAULT NULL,\r"
				+ " `pre_Loc_href_name` varchar(255) DEFAULT NULL,\r"
				+ " `pre_Loc_type` varchar(255) DEFAULT NULL,\r"
				+ " `pre_Loc_title` varchar(255) DEFAULT NULL\r" + "\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_pre_roleref`;\r" + "CREATE TABLE `tmp_" + year + "q"
				+ qtr + "xbrl_pre_roleref`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `pre_rr_roleURI` varchar(255) DEFAULT NULL,\r"
				+ " `pre_rr_roleURI_Name` varchar(255) DEFAULT NULL,\r"
				+ " `pre_rr_href` varchar(255) DEFAULT NULL,\r"
				+ " `pre_rr_type` varchar(255) DEFAULT NULL,\r"
				+ " `pre_rr_title` varchar(255) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
				+ " DROP TABLE IF EXISTS `tmp_" + year + "q" + qtr
				+ "xbrl_xsd_roletype`;\r" + "CREATE TABLE `tmp_" + year + "q"
				+ qtr + "xbrl_xsd_roletype`\r"
				+ " ( `rowno` double NOT NULL DEFAULT '0',\r"
				+ " `accNo` varchar(20) NOT NULL DEFAULT '',\r"
				+ " `acceptedDate` datetime DEFAULT NULL,\r"
				+ " `xsd_rt_roleURI` varchar(255) NOT NULL,\r"
				+ " `roleURI_Role_Name` varchar(255) NOT NULL,\r"
				+ " `xsd_rt_id` varchar(255) NOT NULL,\r"
				+ " `xsd_rt_definition` varchar(255) DEFAULT NULL,\r"
				+ " `xsd_rt_usedOnPre` varchar(70) DEFAULT NULL,\r"
				+ " `xsd_rt_usedOnCal` varchar(70) DEFAULT NULL,\r"
				+ " `xsd_rt_usedOnDef` varchar(70) DEFAULT NULL\r"
				+ " ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r";

		MysqlConnUtils.executeQuery(query);

	}

	public static void createMysqlCsv(String localPath, int iYear, int iQtr)
			throws IOException {

		String xbrlIdxLocalPath = localPath.substring(0,
				localPath.lastIndexOf("/") + 1)
				+ "xbrl.idx";
//		System.out.println("localPath=" + localPath);
//		File f = new File(localPath.replaceAll("\\.csv", "\\.idx"));
//		if (!f.exists()) {
			HttpDownloadUtility.downloadFile(
					"https://www.sec.gov/Archives/edgar/full-index/" + iYear
							+ "/QTR" + iQtr + "/" + "xbrl.idx",
					localPath.replaceAll("xbrl.csv", ""));
//		}
		
//		System.out.println("printing"+xbrlIdxLocalPath);

		// DON'T CHANGE BELOW -- ALWAYS CREATE XBLR IDX TABLE WHERE YOU LOAD THE
		// XBRL.IDX FILE FROM SEC.GOV

			
		int q = iQtr * 3 - 2, day = 31;
		if (q == 2) {
			day = 29;
		}

		if (q == 4 || q == 6 || q == 9 || q == 11) {
			day = 30;
		}

		String query = "DROP TABLE IF EXISTS `TMP_xbrlIdx"
				+ iYear
				+ "Q"
				+ iQtr
				+ "`;\r"
				+ "CREATE TABLE `TMP_xbrlIdx"
				+ iYear
				+ "Q"
				+ iQtr
				+ "` (\r"
				+ "  `ACCNO` VARCHAR(20) NOT NULL DEFAULT '0',\r"
				+ "  `CIK` int(11) DEFAULT NULL,\r"
				+ "  `Company Name` varchar(255) DEFAULT NULL,\r"
				+ "  `Form Type` varchar(50) DEFAULT NULL,\r"
				+ "  `Date Filed` date DEFAULT NULL,\r"
				+ "  `Filename` varchar(255) NOT NULL DEFAULT '',\r"
				+ "  PRIMARY KEY (`accno`),\r"
				+ "  KEY `Form Type` (`Form Type`)\r"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r"
				+ "\r"
				+ "DROP TABLE IF EXISTS tmp_XbrlIDX"
				+ iYear
				+ "Q"
				+ iQtr
				+ "_2;\r"
				+ " CREATE TABLE `tmp_XbrlIDX"
				+ iYear
				+ "Q"
				+ iQtr
				+ "_2` (\r"
				+ "`CIK` int(11) DEFAULT NULL, \r"
				+ " `Company Name` varchar(255) DEFAULT NULL, \r"
				+ "`Form Type` varchar(50) DEFAULT NULL, `Date Filed` date DEFAULT NULL,\r"
				+ " `Filename` varchar(255) NOT NULL DEFAULT '', \r"
				+ " PRIMARY KEY (`Filename`), KEY `Form Type` (`Form Type`)\r"
				+ " ) ENGINE=myisam DEFAULT CHARSET=latin1;\r\r";
		
		String queryA = "LOAD DATA INFILE 'c:/backtest/xbrl//"
				+ iYear
				+ "/QTR"
				+ iQtr
				+ "/xbrl.idx' \r"
				+ "IGNORE INTO TABLE TMP_xbrlIdx"
				+ iYear
				+ "Q"
				+ iQtr
				+ "_2\r"
				+ "FIELDS TERMINATED BY '|'  LINES TERMINATED BY '\\n' IGNORE 11 LINES;\r";
		
		String queryA2 = "\r"
				+ "INSERT IGNORE INTO tmp_XbrlIDX"
				+ iYear
				+ "Q"
				+ iQtr
				+ " \r"
				+ "SELECT substring_index(substring_index(filename,'/',-1),'.',1) acc,t1.* FROM TMP_xbrlIdx"
				+ iYear
				+ "Q"
				+ iQtr
				+ "_2 t1\r"
				+ "WHERE `form Type`='10-K' or `form Type`='10-Q' or `form Type`='10-Q/A' or\r"
				+ " `form Type`='10-K/A' or `form Type`='10-KT' or `form Type`='10-QT'" +
				" or `form Type`='10-KT/A' or `form Type`='10-QT/A'\r group by substring_index(substring_index(filename,'/',-1),'.',1);\r"
				+ " \r"
				+ "DROP TABLE IF EXISTS tmp_"
				+ iYear
				+ "q"
				+ iQtr
				+ "_idx_ins_data;\r"
				+ "CREATE TABLE tmp_"
				+ iYear
				+ "q"
				+ iQtr
				+ "_idx_ins_data ENGINE=MYISAM\r"
				+ "SELECT ACCNO FROM "
				+ iYear
				+ "Q"
				+ iQtr
				+ "XBRL_INS_DATA GROUP BY ACCNO;\r"
				+ "ALTER TABLE TMP_"
				+ iYear
				+ "Q"
				+ iQtr
				+ "_IDX_INS_DATA ADD KEY (ACCNO);\r";
		
		File file = new File("c:/backtest/xbrl/" + iYear + "/QTR" + iQtr + "/xbrl.csv");
		if(file.exists())
			file.delete();
		
		String query2 = "select   t1.`CIK` ,t1.`Company Name` , t1.`Form Type`,t1.`Date Filed` ,t1.`Filename` from tmp_XbrlIDX"
				+ iYear
				+ "Q"
				+ iQtr
				+ " t1 left join tmp_"
				+ iYear
				+ "q"
				+ iQtr
				+ "_idx_ins_data t2\r"
				+ " on t1.accno=t2.accno \r"
				+ "where `Date Filed`>='"
				+ iYear
				+ "-"
				+ ((iQtr * 3) - 2)
				+ "-01' and `Date Filed`<='"
				+ iYear
				+ "-"
				+ iQtr
				* 3
				+ "-"+day+"' and\r"
				+ "(`form Type`='10-K' or `form Type`='10-Q' or `form Type`='10-Q/A' or `form Type`='10-K/A'\r" +
				" or `form Type`='10-KT' or `form Type`='10-QT' or `form Type`='10-KT/A' or `form Type`='10-QT/A') \r"
				+ " and t2.accno is null \r"
				+ "INTO OUTFILE 'c:/backtest/xbrl//" + iYear + "/QTR" + iQtr
				+ "/xbrl.csv'   FIELDS TERMINATED BY '||' ;\r";
				
				String query3 =  "DROP TABLE IF EXISTS tmp_XbrlIDX" + iYear + "Q" + iQtr
				+ ";\r" + "DROP TABLE IF EXISTS tmp_XbrlIDX" + iYear + "Q"
				+ iQtr + "_2;\r" + "DROP TABLE IF EXISTS tmp_" + iYear + "q"
				+ iQtr + "_idx_ins_data;\r";

		try {
			MysqlConnUtils.executeQuery(query+queryA+queryA2+query2+query3);
//			System.out.println("end printing"+xbrlIdxLocalPath);
//			MysqlConnUtils.executeQuery(queryA);
//			System.out.println("end printingA"+xbrlIdxLocalPath);
//			MysqlConnUtils.executeQuery(queryA2+query2+query3);
//			System.out.println("end printingA2"+xbrlIdxLocalPath);
//			MysqlConnUtils.executeQuery(query2);
//			System.out.println("end printing2"+xbrlIdxLocalPath);
//			MysqlConnUtils.executeQuery(query3);
//			System.out.println("end printing3"+xbrlIdxLocalPath);

			// MysqlConnUtils.executeQuery(query1);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	public static void deleteFile(String localPath, String file2parse) {

		File file = new File(localPath + "/" + file2parse.replace("\\", "/"));
//		System.out.println("method delteFile -- deleting="
//				+ file.getAbsolutePath());
		file.delete();

	}

	public static void parseXbrlZipFiling(String localPath, String year,
			String quarter) throws IOException {
		System.out.println("starting parseXbrlZipFiling");
		String xbrlIdx = localPath + "/xbrl.csv";
//		System.out.println("xbrlIdx=" + xbrlIdx);
		// String mysqlCsv = localPath + "/mysql.csv";
		File f3 = new File(xbrlIdx);

		String acceptedDate = null;
		if (!f3.exists()) {
//			System.out
//					.println("xbrl.idx file DOES NOT EXIST - it must be downloaded");
			return;
		}

		BufferedReader rdrXbrl = null;
		// BufferedReader rdrMysql = null;
		try {
			rdrXbrl = new BufferedReader(new FileReader(xbrlIdx));
			// rdrMysql = new BufferedReader(new FileReader(mysqlCsv));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		String xbrlIdxLine;
		try {
			while ((xbrlIdxLine = rdrXbrl.readLine()) != null) {

				String[] items = xbrlIdxLine.split("\\|\\|");
//				System.out.println("printing items" + Arrays.toString(items));
//				System.out.println("items.len=" + items.length);

				if (items.length < 5) {
//					System.out
//							.println("printing items and continuing to next line--items"
//									+ Arrays.toString(items));
					continue;
				}

//				System.out.println("items[2]=" + items[2] + " items[4]="
//						+ items[4]);

				if (items[4].contains("edgar") && items[2].contains("10-")) {
//					System.out.println("has 10Q/K");
					String formType = items[2];
					String fileName = items[4];
					String acc = fileName.substring(
							fileName.lastIndexOf("/") + 1,
							fileName.lastIndexOf("."));

					String accZip = acc + "-xbrl.zip";
					// String accNoNoHyphens = accNo.replace("-", "");
					File accZipPath = new File(localPath + "/" + accZip);
					if (!accZipPath.exists()) {
//						System.out.println("1. continue");
						continue;
					}

					if (!ZipUtilsUnZip.isValid(accZipPath)) {
//						System.out.println("2. continue");
//						System.out.println("2. deleting--"
//								+ accZipPath.getAbsolutePath());
						accZipPath.delete();
						continue;
					}

					if (accZipPath.length() < 1000) {
//						System.out.println("3. continue");
//						System.out.println("3. deleting--"
//								+ accZipPath.getAbsolutePath());
						// accZipPath.delete();
						continue;
					}

					PrintWriter pwAcc = new PrintWriter(localPath + "/" + acc
							+ ".txt");
					List<String> files2Parse = new ArrayList<>();

					File txtPath = new File(localPath + "/" + acc + ".txt");
					String txtPathStr = "";
					if (txtPath.exists()) {
//						System.out.println("4 read txt from tile");
						txtPathStr = Utils.readTextFromFile(localPath + "/"
								+ acc + ".txt");
					}

					String ticker = "";
					if (txtPathStr.length() < 18) {
//						System.out.println("defalting zip file. zipFile="
//								+ localPath + "/" + accZip + " localPath="
//								+ localPath + " acc=" + acc);

						files2Parse = ZipUtils.deflateZipFilewithAccNo(
								localPath + "/" + accZip, localPath + "/", acc);

//						System.out.println("list of zip files size="
//								+ files2Parse.size());

						for (String file2parse : files2Parse) {
							if (file2parse.endsWith("_pre.xml")) {

//								System.out
//										.println("getting ticker from xbrl unzipped file name");

								File f2 = new File(localPath + "/" + file2parse);
								String path = f2.getPath();
								ticker = path.substring(
										path.lastIndexOf("\\") + 22,
										path.lastIndexOf("-"));
							}

							deleteFile(localPath, file2parse);

						}
					}

//					System.out.println("ticker === " + ticker
//							+ " txtPathStr.len=" + txtPathStr.length()
//							+ " accZipPath.exists=" + accZipPath.exists());
					boolean getAccTxt = false;
					if (!txtPath.exists()) {
						getAccTxt = true;
					}

					if (txtPathStr.length() < 18) {
						txtPath.delete();
						getAccTxt = true;
					}

					System.out.println("getAccTxt=" + getAccTxt);

					if (getAccTxt && accZipPath.exists()) {
//						System.out
//								.println("downloading accno.txt - accepteddate");
						String suffix = fileName.substring(0,
								fileName.lastIndexOf("/"));
						String wwwSecGovPathHtm = (suffix + "/"
								+ acc.replaceAll("-", "") + "/" + acc + "-index.htm");
						// this downloads acceptedDate etc.
						XBRLInfoSecGov info = new XBRLInfoSecGov(
								"https://www.sec.gov/Archives/"
										+ wwwSecGovPathHtm);
						acceptedDate = (info.acceptedDate);
//						System.out.println("acceptedDate downloaded="
//								+ acceptedDate + " acc=" + acc + " ticker="
//								+ ticker);

						pwAcc.println(acceptedDate + "||" + ticker);
						pwAcc.close();

					}

					System.out.println("acceptedDate=" + acceptedDate);
					txtPathStr = Utils.readTextFromFile(localPath + "/" + acc
							+ ".txt");

					if (txtPathStr.length() > 18 && accZipPath.exists()) {

						acceptedDate = txtPathStr.split("\\|\\|")[0];
						ticker = txtPathStr.split("\\|\\|")[1];
//						System.out.println("1. ticker=" + ticker
//								+ " acceptedDate=" + acceptedDate);

						files2Parse = ZipUtils.deflateZipFilewithAccNo(
								localPath + "/" + accZip, localPath + "/", acc);

						// String year = acceptedDate.substring(0,4);
						// int qtr =
						// (Integer.parseInt(acceptedDate.substring(4,6).replaceAll("^0",
						// ""))+2)/3;
						// String q = qtr+"";

						MysqlConnUtils.executeQuery(XbrlDownloader.createTmpXbrlMySqlTablestoParseXML(year,
								quarter));
						for (String file2parse : files2Parse) {

							if (file2parse.endsWith("_pre.xml")
									&& file2parse.contains(acc)) {
								@SuppressWarnings("unused")
								SAXParserPre sxp = new SAXParserPre(localPath
										+ "/" + file2parse.replace("\\", "/"),
										acceptedDate, formType, ticker, yYear,
										qQtr);
								deleteFile(localPath, file2parse);

							} else if ((!file2parse.endsWith("_pre.xml")
									&& !file2parse.endsWith("_cal.xml")
									&& !file2parse.endsWith("_def.xml")
									&& !file2parse.endsWith("_lab.xml")
									&& !file2parse.endsWith(".xsd")
									&& !file2parse.endsWith("ref.xml") && !file2parse
										.contains("defnref"))

									&& (file2parse.endsWith(".xml") || file2parse
											.endsWith(".txt"))) {

								@SuppressWarnings("unused")
								SAXParserIns sxc = new SAXParserIns(localPath
										+ "/" + file2parse.replace("\\", "/"),
										acceptedDate, yYear, qQtr);
								deleteFile(localPath, file2parse);

							} else if (file2parse.endsWith("_cal.xml")
									&& file2parse.contains(acc)) {
								@SuppressWarnings("unused")
								SAXParserCal sxc = new SAXParserCal(

										localPath + "/"
												+ file2parse.replace("\\", "/"),
										acceptedDate, yYear, qQtr);

								deleteFile(localPath, file2parse);

							} else if (file2parse.endsWith("_lab.xml")
									&& file2parse.contains(acc)) {
								@SuppressWarnings("unused")
								SAXParserLab sxc = new SAXParserLab(localPath
										+ "/" + file2parse.replace("\\", "/"),
										acceptedDate, yYear, qQtr);

								deleteFile(localPath, file2parse);

							} else if (file2parse.endsWith("_def.xml")
									&& file2parse.contains(acc)) {
								@SuppressWarnings("unused")
								SAXParserDef sxc = new SAXParserDef(localPath
										+ "/" + file2parse.replace("\\", "/"),
										acceptedDate, yYear, qQtr);

								deleteFile(localPath, file2parse);

							} else if (file2parse.endsWith(".xsd")
									&& file2parse.contains(acc)) {
								@SuppressWarnings("unused")
								// XSD must be final SAXParser
								SAXParserXSD sxc = new SAXParserXSD(localPath
										+ "/" + file2parse.replace("\\", "/"),
										acceptedDate, yYear, qQtr);
								deleteFile(localPath, file2parse);
							}
						}

						// System.out.println("a/d getXbrlTable::" +
						// acceptedDate);
						XbrlDownloader.getXbrlTable(acceptedDate);
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		// rdrMysql.close();
		rdrXbrl.close();
	}

	public static void main(String[] args) throws SocketException, IOException,
			ParseException, SQLException {


		@SuppressWarnings("resource")
		Scanner Scan = new Scanner(System.in);
		/*
		 * System.out.println(
		 * "Do you want to only parse files missing in mysql table (y/n)");
		 * String onlyInMySql = Scan.nextLine(); boolean onlyNotInMysql = false;
		 * if(onlyInMySql.toLowerCase().contains("y")){ onlyNotInMysql = true; }
		 */
		System.out.println("this will only parse what's not in MySql");
		System.out
				.println("Enter start date of xbrl to re-parse from local drive in yyyymmdd format");

		String startDateStr = Scan.nextLine();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date sDate = new Date();
		sDate = sdf.parse(startDateStr);
		Calendar startDate = Calendar.getInstance();
		startDate.setTime(sDate);

		System.out
				.println("Enter end date of xbrl to re-parse from local drive in yyyymmdd format");
		String endDateStr = Scan.nextLine();
		Date eDate = new Date();
		eDate = sdf.parse(endDateStr);
		Calendar endDate = Calendar.getInstance();
		endDate.setTime(eDate);
		String earliestDateStr = "20090401";
		Date firstDate = sdf.parse(earliestDateStr);
		Calendar badDate = Calendar.getInstance();
		badDate.setTime(firstDate);

		if (endDate.before(startDate)) {
			System.out
					.println("End date must be later than start date. Please re-enter.");
			return;
		}
		
		if (endDate.after(Calendar.getInstance())) {
			System.out
					.println("End date cannot be later than today. Please re-enter.");
			return;
		}

		if (endDate.before(badDate) || startDate.before(badDate)) {
			System.out
					.println("End date and start date must be later than 20090401. Please re-enter.");
			return;
		}
		// HttpDownloadUtility
		// .downloadFile(
		// "https://www.sec.gov/Archives/edgar/full-index/2014/QTR2/xbrl.idx",
		// "c:/backtest/master/2014/QTR2/");

		dateRangeQuarters(startDate, endDate);

		// If I regenerate - truncate xbrl_filer;
		// XbrlDownloader.dropTmpMySqlTablestoParseXML();

		// TODO: Search xxxxxx in XbrlLocalFileParser and remove commented
		// coded!

	}
}
