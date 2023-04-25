package xbrl;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Calendar;

public class DownloadFSDataSetsAndMasterXbrlIIdxFiles {
	
	public static String fsDataSetFolder = "c:/backtest/FSDataSets/";

	public static int endYear = Calendar.getInstance().get(Calendar.YEAR);

	public static void DownloadFSDataMaster(int startYr, int startQtr) throws SQLException {
		String[] saveDir = { fsDataSetFolder/*, "c:/backtest/master/",
				"c:/backtest/master/" */};
//		https://www.sec.gov/files/dera/data/financial-statement-data-sets/2017q3.zip

		String[] sites = {
				"https://www.sec.gov/files/dera/data/financial-statement-data-sets/" };
		// "https://www.sec.gov/Archives/edgar/full-index/2016/QTR4/"
		String site = "";
		String[] filenameExtension = { ".zip", "master.zip", "xbrl.zip" };
//		int startYr = 2016, startQtr = 1;

		// search for file - and if exists - skip.
		File file = new File("");

		System.out.println("endYear=" + endYear);

		for (int i = 0; i < sites.length; i++) {

			for (int yr = startYr; yr <= endYear; yr++) {
				for (int q = startQtr; q < 5; q++) {

					try {
						if (i == 0) {
							// add validator
							String path = saveDir[i] + "/" + yr + "/QTR" + q
									+ "/";
							FileSystemUtils.createFoldersIfReqd(path);
							file = new File(path + yr + "q" + q
									+ filenameExtension[i]);
							site = sites[i] + yr + "q" + q
									+ filenameExtension[i];
							System.out.println("file=" + file
									+ "\rdownload file site=" + site);
							System.out.println("is valid?="
									+ ZipUtils.isTarGzipFileValid(file
											.getAbsolutePath()));
							if ((file.exists() && ZipUtils
									.isTarGzipFileValid(file.getAbsolutePath()))
									|| yr < 2009) {
								System.out.println("skip this download");
								if( (yr==2009 && q>1) || yr>2009)
								continue;
							} else {
								HttpDownloadUtility.downloadFile(site, path);
								if( (yr==2009 && q>1) || yr>2009) {
									ZipUtils.deflateZipFile(
											fsDataSetFolder + yr + "/qtr" + q + "/" + yr + "q" + q + ".zip",
											fsDataSetFolder + yr + "/qtr" + q + "/");
								loadIntoMysqlFSDataSets(yr,q);
								}
							}
						}
						if (i > 0) {
							String path = saveDir[i] + "/" + yr + "/QTR" + q
									+ "/";
							System.out.println("valid master/xbrl zip?="
									+ ZipUtils.isTarGzipFileValid(file
											.getAbsolutePath()));
							FileSystemUtils.createFoldersIfReqd(path);
							site = sites[i] + "/" + yr + "/QTR" + q + "/"
									+ filenameExtension[i];
							file = new File(path + filenameExtension[i]);
							System.out.println("file=" + file
									+ "\rdownload file site=" + site);
							if (file.exists()
									&& ZipUtils.isTarGzipFileValid(file
											.getAbsolutePath())) {
								System.out.println("skip this download");
								continue;
							} else {
								HttpDownloadUtility.downloadFile(site, path);
								insertIntoMysqMasterIdx();
							}
						}

					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
		}
	}

	public static void insertIntoMysqMasterIdx() throws IOException,
			SQLException {

		int startYr = 1993, startQtr = 1;
		int endYear = Calendar.getInstance().get(Calendar.YEAR);

		String localPath = "c:/backtest/master", zipFilePath = "";
		
		MysqlConnUtils.executeQuery("call TMP_masteridx();\r");
		
		for (int yr = startYr; yr < endYear; yr++) {
			for (int q = startQtr; q < 5; q++) {
				zipFilePath = localPath + "/" + yr + "/QTR" + q + "/";
				File file = new File(zipFilePath + "/" + "master.zip");
				System.out.println("a file=" + file.getAbsolutePath());
				if (!file.exists()) {
					System.out.println("file does not exist");
					continue;

				}

				if (file.exists()
						&& ZipUtils.isTarGzipFileValid(file.getAbsolutePath())) {
					ZipUtils.deflateZipFile(zipFilePath + "/"+file.getName(),
							zipFilePath);
					File f = new File(file.getAbsolutePath().replaceAll("\\.zip", "\\.idx"));
					
					String query = "LOAD DATA INFILE '"
							+ f.getAbsolutePath().replaceAll("\\\\", "/")
							+ "' IGNORE INTO TABLE tmp_masterIdx \n"
							+ "FIELDS TERMINATED BY '|' OPTIONALLY ENCLOSED BY '\\\\' "
							+ "LINES TERMINATED BY '\\n' IGNORE 11 LINES;\n";
					ZipUtils.zipAFile(file);
					
					MysqlConnUtils.executeQuery(query);
				}
			}
		}
		
		String query = "insert ignore into masterIdx "
				+ "select left(right(filename,25),20) accno,cik,`company name`"
				+ ",`form type`,`date filed`,filename from tmp_masterIdx ;";
		MysqlConnUtils.executeQuery(query);
		
	}
	
	public static void loadIntoMysqlFSDataSets(int yr, int qtr) throws IOException, SQLException{
		
		// deflate zip file.
		
		String query = "drop table if exists "+yr+"q"+qtr+"FSData_sub;\r"+
				"CREATE TABLE `"+yr+"q"+qtr+"FSData_sub` (\r"+
				"  `accno` varchar(20) DEFAULT '0',\r"+
				"  `cik` int(11) DEFAULT '0',\r"+
				"  `name` varchar(255) DEFAULT '0',\r"+
				"  `sic` int(11) DEFAULT '0',\r"+
				"  `countryba` varchar(150) DEFAULT '0',\r"+
				"  `stprba` varchar(150) DEFAULT '0',\r"+
				"  `cityba` varchar(150) DEFAULT '0',\r"+
				"  `zipba` varchar(150) DEFAULT '0',\r"+
				"  `bas1` varchar(150) DEFAULT '0',\r"+
				"  `bas2` varchar(150) DEFAULT '0',\r"+
				"  `baph` varchar(150) DEFAULT '0',\r"+
				"  `countryma` varchar(150) DEFAULT '0',\r"+
				"  `stprma` varchar(150) DEFAULT '0',\r"+
				"  `cityma` varchar(150) DEFAULT '0',\r"+
				"  `zipma` varchar(150) DEFAULT '0',\r"+
				"  `mas1` varchar(150) DEFAULT '0',\r"+
				"  `mas2` varchar(150) DEFAULT '0',\r"+
				"  `countryinc` varchar(150) DEFAULT '0',\r"+
				"  `stprinc` varchar(150) DEFAULT '0',\r"+
				"  `ein` int(11) DEFAULT 0,\r"+
				"  `former` varchar(150) DEFAULT '0',\r"+
				"  `changed` varchar(150) DEFAULT '0',\r"+
				"  `afs` varchar(150) DEFAULT '0',\r"+
				"  `wksi` int(11) DEFAULT 0,\r"+
				"  `fye` int(11) DEFAULT 0,\r"+
				"  `form` varchar(50) DEFAULT '0',\r"+
				"  `period` int(11) DEFAULT 0,\r"+
				"  `fy` int(11) DEFAULT 0,\r"+
				"  `fp` varchar(50) DEFAULT 0,\r"+
				"  `filed` int(11) DEFAULT 0,\r"+
				"  `accepted` datetime DEFAULT '1901-01-01',\r"+
				"  `prevrpt` int(11) DEFAULT '0',\r"+
				"  `detail` int(11) DEFAULT '0',\r"+
				"  `instance` varchar(56) DEFAULT '0',\r"+
				"  `nciks` int(11) DEFAULT '0',\r"+
				"  `aciks` varchar(69) DEFAULT '0'  ,\r"+
				"  key(accno)\r"+
				") ENGINE=myisam DEFAULT CHARSET=latin1;\r"+
				"\r"+
				"DROP TABLE IF EXISTS "+yr+"Q"+qtr+"FSData;\r"+
				"CREATE TABLE `"+yr+"Q"+qtr+"FSData` (\r"+
				"  `accno` varchar(20) NOT NULL DEFAULT '0',\r"+
				"  `tag` varchar(255) NOT NULL DEFAULT '0',\r"+
				"  `ver` varchar(100) NOT NULL DEFAULT '0',\r"+
				"  `co_reg` varchar(255) NOT NULL DEFAULT '0',\r"+
				"  `date` int(11) NOT NULL DEFAULT 0,\r"+
				"  `qtrs` tinyint(3) NOT NULL DEFAULT 0,\r"+
				"  `uom` varchar(50) NOT NULL DEFAULT '0',\r"+
				"  `value` double DEFAULT NULL,\r"+
				"  `footnote` varchar(150) NOT NULL DEFAULT '0',\r"+
				"  key(accno,tag)\r"+
				"  ) ENGINE=myisam DEFAULT CHARSET=latin1;\r"+
				"\r"+
				"DROP TABLE IF EXISTS "+yr+"Q"+qtr+"FSData_PRE;\r"+
				"CREATE TABLE `"+yr+"Q"+qtr+"FSData_PRE` (\r"+
				"  `accno` varchar(20) NOT NULL DEFAULT '0',\r"+
				"  `report` int(11) NOT NULL DEFAULT 0,\r"+
				"  `line` int(11) NOT NULL DEFAULT 0,\r"+
				"  `stmt` varchar(50) NOT NULL DEFAULT '0',\r"+
				"  `inpth` int(11) NOT NULL DEFAULT 0,\r"+
				"  `rfile` varchar(50) NOT NULL DEFAULT '0',\r"+
				"  `tag` varchar(255) NOT NULL DEFAULT '0',\r"+
				"  `ver` varchar(150) NOT NULL DEFAULT '0',\r"+
				"  `plabel` varchar(50) NOT NULL DEFAULT '0',\r"+
				"  `negating` int(11) NOT NULL DEFAULT 0,\r"+
				"  key(accno,tag)\r"+
				"  ) ENGINE=myisam DEFAULT CHARSET=latin1;\r"+
				"\r"+
				"\r"+
				"LOAD DATA INFILE 'c:/backtest/fsDataSets/"+yr+"/qtr"+qtr+"/num.txt' \r"+
				"ignore INTO TABLE "+yr+"Q"+qtr+"FSData\r"+
				"FIELDS TERMINATED BY '\\t'\r"+
				"LINES TERMINATED BY '\\n'\r"+
				"IGNORE 1 LINES\r"+
				"(accno,tag,@v,co_reg,date,qtrs,uom,value,footnote)\r"+
				"SET `ver`=replace(@v,'/','_') ;\r"+
				"\r"+
				"LOAD DATA INFILE 'c:/backtest/fsDataSets/"+yr+"/qtr"+qtr+"/pre.txt' \r"+
				"ignore INTO TABLE "+yr+"Q"+qtr+"FSData_pre\r"+
				"FIELDS TERMINATED BY '\\t'\r"+
				"LINES TERMINATED BY '\\n'\r"+
				"IGNORE 1 LINES\r"+
				"(accno,report,line,stmt,inpth,rfile,tag,@v,plabel,negating)\r"+
				"SET `ver`=replace(@v,'/','_') ;\r"+
				"\r"+
				"\r"+
				"LOAD DATA INFILE 'c:/backtest/fsDataSets/"+yr+"/qtr"+qtr+"/sub.txt' \r"+
				"ignore INTO TABLE "+yr+"q"+qtr+"FSData_sub\r"+
				"FIELDS TERMINATED BY '\\t'\r"+
				"LINES TERMINATED BY '\\n'\r"+
				"IGNORE 1 LINES\r"+
				";\r"+
				"\r"+
				"DROP TABLE IF EXISTS "+yr+"q"+qtr+"fsdata_is;\r"+
				"CREATE TABLE `"+yr+"q"+qtr+"fsdata_is` (\r"+
				"  `accepteddate` datetime DEFAULT '1901-01-01',\r"+
				"  `cik` int(11) DEFAULT 0,\r"+
				"coName varchar(250) DEFAULT '0',\r"+
				"    `filerType` varchar(150) DEFAULT '0',\r"+
				"  `wksi` int(11) DEFAULT 0,\r"+
				"  `fye` int(11) DEFAULT 0,\r"+
				"  `form` varchar(50) DEFAULT '0',\r"+
				"  `fy` int(11) DEFAULT 0,\r"+
				"  `fp` varchar(50) DEFAULT 0,\r"+
				"its varchar(50) DEFAULT 0,\r"+
				"xml_file_name  varchar(250) DEFAULT 0,\r"+
				"  `accno` varchar(20) DEFAULT NULL,\r"+
				"  `tag` varchar(255) DEFAULT NULL,\r"+
				"  `ver` varchar(50) DEFAULT NULL,\r"+
				"  `co_reg` varchar(50) DEFAULT NULL,\r"+
				"  `date` int(11) DEFAULT NULL,\r"+
				"  `qtrs` int(11) DEFAULT NULL,\r"+
				"  `uom` varchar(50) DEFAULT NULL,\r"+
				"  `value` double DEFAULT NULL,\r"+
				"  `footnote` varchar(50) DEFAULT NULL,\r"+
				"  `report` int(11) DEFAULT NULL,\r"+
				"  `line` int(11) DEFAULT NULL,\r"+
				"  `pLabel` varchar(59) DEFAULT NULL,\r"+
				"  `negating` int(11) DEFAULT NULL,\r"+
				"  `stmt` varchar(50) DEFAULT NULL,\r"+
				"  `inpth` int(11) DEFAULT NULL,\r"+
				"  `rfile` varchar(50) DEFAULT NULL, key(accno),key(tag)\r"+
				") ENGINE=MYISAM DEFAULT CHARSET=latin1;\r"+
				"\r"+
				"insert ignore into "+yr+"q"+qtr+"fsdata_is\r"+
				"select\r"+
				"accepted,cik\r"+
				",name\r"+
				",afs\r"+
				",wksi\r"+
				",fye\r"+
				",form\r"+
				",fy\r"+
				",fp\r"+
				",case \r"+
				"when left(instance,7)  rlike '-' then substring_index(instance,'-',1)\r"+
				"when right(left(instance,2),1) rlike '[0-9]' then left(instance,1)\r"+
				"when right(left(instance,3),1) rlike '[0-9]' then left(instance,2)\r"+
				"when right(left(instance,4),1) rlike '[0-9]' then left(instance,3)\r"+
				"when right(left(instance,5),1) rlike '[0-9]' then left(instance,4)\r"+
				"when right(left(instance,6),1) rlike '[0-9]' then left(instance,5)\r"+
				"else \r"+
				"instance end its,instance,\r"+
				"t1.*,t2.report,t2.line,t2.pLabel,t2.negating,t2.stmt,t2.inpth,t2.rfile from "+yr+"Q"+qtr+"FSData\r"+
				"t1,"+yr+"q"+qtr+"FSData_sub t3, "+yr+"Q"+qtr+"FSData_pre t2\r"+
				"\r"+
				"where stmt = 'IS' and co_reg =0 \r"+
				"and t3.accno=t2.accno and\r"+
				" t1.accno=t2.accno and t1.tag = t2.tag \r"+
				";\r"+
				"\r"+
				"DROP TABLE IF EXISTS "+yr+"q"+qtr+"fsdata_cf;\r"+
				"CREATE TABLE `"+yr+"q"+qtr+"fsdata_cf` (\r"+
				"  `accepteddate` datetime DEFAULT '1901-01-01',\r"+
				"  `cik` int(11) DEFAULT 0,\r"+
				"coName varchar(250) DEFAULT '0',\r"+
				"    `filerType` varchar(150) DEFAULT '0',\r"+
				"  `wksi` int(11) DEFAULT 0,\r"+
				"  `fye` int(11) DEFAULT 0,\r"+
				"  `form` varchar(50) DEFAULT '0',\r"+
				"  `fy` int(11) DEFAULT 0,\r"+
				"  `fp` varchar(50) DEFAULT 0,\r"+
				"its varchar(50) DEFAULT 0,\r"+
				"xml_file_name  varchar(250) DEFAULT 0,\r"+
				"  `accno` varchar(20) DEFAULT NULL,\r"+
				"  `tag` varchar(255) DEFAULT NULL,\r"+
				"  `ver` varchar(50) DEFAULT NULL,\r"+
				"  `co_reg` varchar(50) DEFAULT NULL,\r"+
				"  `date` int(11) DEFAULT NULL,\r"+
				"  `qtrs` int(11) DEFAULT NULL,\r"+
				"  `uom` varchar(50) DEFAULT NULL,\r"+
				"  `value` double DEFAULT NULL,\r"+
				"  `footnote` varchar(50) DEFAULT NULL,\r"+
				"  `report` int(11) DEFAULT NULL,\r"+
				"  `line` int(11) DEFAULT NULL,\r"+
				"  `pLabel` varchar(59) DEFAULT NULL,\r"+
				"  `negating` int(11) DEFAULT NULL,\r"+
				"  `stmt` varchar(50) DEFAULT NULL,\r"+
				"  `inpth` int(11) DEFAULT NULL,\r"+
				"  `rfile` varchar(50) DEFAULT NULL, key(accno),key(tag)\r"+
				") ENGINE=MYISAM DEFAULT CHARSET=latin1;\r"+
				"\r"+
				"insert ignore into "+yr+"q"+qtr+"fsdata_cf\r"+
				"select\r"+
				"accepted,cik\r"+
				",name\r"+
				",afs\r"+
				",wksi\r"+
				",fye\r"+
				",form\r"+
				",fy\r"+
				",fp\r"+
				",case \r"+
				"when left(instance,7)  rlike '-' then substring_index(instance,'-',1)\r"+
				"when right(left(instance,2),1) rlike '[0-9]' then left(instance,1)\r"+
				"when right(left(instance,3),1) rlike '[0-9]' then left(instance,2)\r"+
				"when right(left(instance,4),1) rlike '[0-9]' then left(instance,3)\r"+
				"when right(left(instance,5),1) rlike '[0-9]' then left(instance,4)\r"+
				"when right(left(instance,6),1) rlike '[0-9]' then left(instance,5)\r"+
				"else \r"+
				"instance end its,instance,\r"+
				"t1.*,t2.report,t2.line,t2.pLabel,t2.negating,t2.stmt,t2.inpth,t2.rfile from "+yr+"Q"+qtr+"FSData\r"+
				"t1,"+yr+"q"+qtr+"FSData_sub t3, "+yr+"Q"+qtr+"FSData_pre t2\r"+
				"\r"+
				"where stmt = 'CF' and co_reg =0 \r"+
				"and t3.accno=t2.accno and\r"+
				" t1.accno=t2.accno and t1.tag = t2.tag \r"+
				";\r"+
				"\r"+
				"\r"+
				"DROP TABLE IF EXISTS "+yr+"q"+qtr+"fsdata_bs;\r"+
				"CREATE TABLE `"+yr+"q"+qtr+"fsdata_bs` (\r"+
				"  `accepteddate` datetime DEFAULT '1901-01-01',\r"+
				"  `cik` int(11) DEFAULT 0,\r"+
				"coName varchar(250) DEFAULT '0',\r"+
				"    `filerType` varchar(150) DEFAULT '0',\r"+
				"  `wksi` int(11) DEFAULT 0,\r"+
				"  `fye` int(11) DEFAULT 0,\r"+
				"  `form` varchar(50) DEFAULT '0',\r"+
				"  `fy` int(11) DEFAULT 0,\r"+
				"  `fp` varchar(50) DEFAULT 0,\r"+
				"its varchar(50) DEFAULT 0,\r"+
				"xml_file_name  varchar(250) DEFAULT 0,\r"+
				"  `accno` varchar(20) DEFAULT NULL,\r"+
				"  `tag` varchar(255) DEFAULT NULL,\r"+
				"  `ver` varchar(50) DEFAULT NULL,\r"+
				"  `co_reg` varchar(50) DEFAULT NULL,\r"+
				"  `date` int(11) DEFAULT NULL,\r"+
				"  `qtrs` int(11) DEFAULT NULL,\r"+
				"  `uom` varchar(50) DEFAULT NULL,\r"+
				"  `value` double DEFAULT NULL,\r"+
				"  `footnote` varchar(50) DEFAULT NULL,\r"+
				"  `report` int(11) DEFAULT NULL,\r"+
				"  `line` int(11) DEFAULT NULL,\r"+
				"  `pLabel` varchar(59) DEFAULT NULL,\r"+
				"  `negating` int(11) DEFAULT NULL,\r"+
				"  `stmt` varchar(50) DEFAULT NULL,\r"+
				"  `inpth` int(11) DEFAULT NULL,\r"+
				"  `rfile` varchar(50) DEFAULT NULL, key(accno),key(tag)\r"+
				") ENGINE=MYISAM DEFAULT CHARSET=latin1;\r"+
				"\r"+
				"insert ignore into "+yr+"q"+qtr+"fsdata_bs\r"+
				"select\r"+
				"accepted,cik\r"+
				",name\r"+
				",afs\r"+
				",wksi\r"+
				",fye\r"+
				",form\r"+
				",fy\r"+
				",fp\r"+
				",case \r"+
				"when left(instance,7)  rlike '-' then substring_index(instance,'-',1)\r"+
				"when right(left(instance,2),1) rlike '[0-9]' then left(instance,1)\r"+
				"when right(left(instance,3),1) rlike '[0-9]' then left(instance,2)\r"+
				"when right(left(instance,4),1) rlike '[0-9]' then left(instance,3)\r"+
				"when right(left(instance,5),1) rlike '[0-9]' then left(instance,4)\r"+
				"when right(left(instance,6),1) rlike '[0-9]' then left(instance,5)\r"+
				"else \r"+
				"instance end its,instance,\r"+
				"t1.*,t2.report,t2.line,t2.pLabel,t2.negating,t2.stmt,t2.inpth,t2.rfile from "+yr+"Q"+qtr+"FSData\r"+
				"t1,"+yr+"q"+qtr+"FSData_sub t3, "+yr+"Q"+qtr+"FSData_pre t2\r"+
				"\r"+
				"where stmt = 'BS' and co_reg =0 \r"+
				"and t3.accno=t2.accno and\r"+
				" t1.accno=t2.accno and t1.tag = t2.tag \r"+
				";\r"+
				"\r"+
				"DROP TABLE IF EXISTS "+yr+"q"+qtr+"fsdata_eq;\r"+
				"CREATE TABLE `"+yr+"q"+qtr+"fsdata_eq` (\r"+
				"  `accepteddate` datetime DEFAULT '1901-01-01',\r"+
				"  `cik` int(11) DEFAULT 0,\r"+
				"coName varchar(250) DEFAULT '0',\r"+
				"    `filerType` varchar(150) DEFAULT '0',\r"+
				"  `wksi` int(11) DEFAULT 0,\r"+
				"  `fye` int(11) DEFAULT 0,\r"+
				"  `form` varchar(50) DEFAULT '0',\r"+
				"  `fy` int(11) DEFAULT 0,\r"+
				"  `fp` varchar(50) DEFAULT 0,\r"+
				"its varchar(50) DEFAULT 0,\r"+
				"xml_file_name  varchar(250) DEFAULT 0,\r"+
				"  `accno` varchar(20) DEFAULT NULL,\r"+
				"  `tag` varchar(255) DEFAULT NULL,\r"+
				"  `ver` varchar(50) DEFAULT NULL,\r"+
				"  `co_reg` varchar(50) DEFAULT NULL,\r"+
				"  `date` int(11) DEFAULT NULL,\r"+
				"  `qtrs` int(11) DEFAULT NULL,\r"+
				"  `uom` varchar(50) DEFAULT NULL,\r"+
				"  `value` double DEFAULT NULL,\r"+
				"  `footnote` varchar(50) DEFAULT NULL,\r"+
				"  `report` int(11) DEFAULT NULL,\r"+
				"  `line` int(11) DEFAULT NULL,\r"+
				"  `pLabel` varchar(59) DEFAULT NULL,\r"+
				"  `negating` int(11) DEFAULT NULL,\r"+
				"  `stmt` varchar(50) DEFAULT NULL,\r"+
				"  `inpth` int(11) DEFAULT NULL,\r"+
				"  `rfile` varchar(50) DEFAULT NULL, key(accno),key(tag)\r"+
				") ENGINE=MYISAM DEFAULT CHARSET=latin1;\r"+
				"\r"+
				"insert ignore into "+yr+"q"+qtr+"fsdata_eq\r"+
				"select\r"+
				"accepted,cik\r"+
				",name\r"+
				",afs\r"+
				",wksi\r"+
				",fye\r"+
				",form\r"+
				",fy\r"+
				",fp\r"+
				",case \r"+
				"when left(instance,7)  rlike '-' then substring_index(instance,'-',1)\r"+
				"when right(left(instance,2),1) rlike '[0-9]' then left(instance,1)\r"+
				"when right(left(instance,3),1) rlike '[0-9]' then left(instance,2)\r"+
				"when right(left(instance,4),1) rlike '[0-9]' then left(instance,3)\r"+
				"when right(left(instance,5),1) rlike '[0-9]' then left(instance,4)\r"+
				"when right(left(instance,6),1) rlike '[0-9]' then left(instance,5)\r"+
				"else \r"+
				"instance end its,instance,\r"+
				"t1.*,t2.report,t2.line,t2.pLabel,t2.negating,t2.stmt,t2.inpth,t2.rfile from "+yr+"Q"+qtr+"FSData\r"+
				"t1,"+yr+"q"+qtr+"FSData_sub t3, "+yr+"Q"+qtr+"FSData_pre t2\r"+
				"\r"+
				"where stmt = 'EQ' and co_reg =0 \r"+
				"and t3.accno=t2.accno and\r"+
				" t1.accno=t2.accno and t1.tag = t2.tag \r"+
				";\r"+
				"\r"+
				"DROP TABLE IF EXISTS "+yr+"q"+qtr+"fsdata_ci;\r"+
				"CREATE TABLE `"+yr+"q"+qtr+"fsdata_ci` (\r"+
				"  `accepteddate` datetime DEFAULT '1901-01-01',\r"+
				"  `cik` int(11) DEFAULT 0,\r"+
				"coName varchar(250) DEFAULT '0',\r"+
				"    `filerType` varchar(150) DEFAULT '0',\r"+
				"  `wksi` int(11) DEFAULT 0,\r"+
				"  `fye` int(11) DEFAULT 0,\r"+
				"  `form` varchar(50) DEFAULT '0',\r"+
				"  `fy` int(11) DEFAULT 0,\r"+
				"  `fp` varchar(50) DEFAULT 0,\r"+
				"its varchar(50) DEFAULT 0,\r"+
				"xml_file_name  varchar(250) DEFAULT 0,\r"+
				"  `accno` varchar(20) DEFAULT NULL,\r"+
				"  `tag` varchar(255) DEFAULT NULL,\r"+
				"  `ver` varchar(50) DEFAULT NULL,\r"+
				"  `co_reg` varchar(50) DEFAULT NULL,\r"+
				"  `date` int(11) DEFAULT NULL,\r"+
				"  `qtrs` int(11) DEFAULT NULL,\r"+
				"  `uom` varchar(50) DEFAULT NULL,\r"+
				"  `value` double DEFAULT NULL,\r"+
				"  `footnote` varchar(50) DEFAULT NULL,\r"+
				"  `report` int(11) DEFAULT NULL,\r"+
				"  `line` int(11) DEFAULT NULL,\r"+
				"  `pLabel` varchar(59) DEFAULT NULL,\r"+
				"  `negating` int(11) DEFAULT NULL,\r"+
				"  `stmt` varchar(50) DEFAULT NULL,\r"+
				"  `inpth` int(11) DEFAULT NULL,\r"+
				"  `rfile` varchar(50) DEFAULT NULL, key(accno),key(tag)\r"+
				") ENGINE=MYISAM DEFAULT CHARSET=latin1;\r"+
				"\r"+
				"insert ignore into "+yr+"q"+qtr+"fsdata_ci\r"+
				"select\r"+
				"accepted,cik\r"+
				",name\r"+
				",afs\r"+
				",wksi\r"+
				",fye\r"+
				",form\r"+
				",fy\r"+
				",fp\r"+
				",case \r"+
				"when left(instance,7)  rlike '-' then substring_index(instance,'-',1)\r"+
				"when right(left(instance,2),1) rlike '[0-9]' then left(instance,1)\r"+
				"when right(left(instance,3),1) rlike '[0-9]' then left(instance,2)\r"+
				"when right(left(instance,4),1) rlike '[0-9]' then left(instance,3)\r"+
				"when right(left(instance,5),1) rlike '[0-9]' then left(instance,4)\r"+
				"when right(left(instance,6),1) rlike '[0-9]' then left(instance,5)\r"+
				"else \r"+
				"instance end its,instance,\r"+
				"t1.*,t2.report,t2.line,t2.pLabel,t2.negating,t2.stmt,t2.inpth,t2.rfile from "+yr+"Q"+qtr+"FSData\r"+
				"t1,"+yr+"q"+qtr+"FSData_sub t3, "+yr+"Q"+qtr+"FSData_pre t2\r"+
				"\r"+
				"where stmt = 'CI' and co_reg =0 \r"+
				"and t3.accno=t2.accno and\r"+
				" t1.accno=t2.accno and t1.tag = t2.tag \r"+
				";\r"+
				"\r"+
				"\r"+
				"DROP TABLE IF EXISTS "+yr+"q"+qtr+"fsdata_cp;\r"+
				"CREATE TABLE `"+yr+"q"+qtr+"fsdata_cp` (\r"+
				"  `accepteddate` datetime DEFAULT '1901-01-01',\r"+
				"  `cik` int(11) DEFAULT 0,\r"+
				"coName varchar(250) DEFAULT '0',\r"+
				"    `filerType` varchar(150) DEFAULT '0',\r"+
				"  `wksi` int(11) DEFAULT 0,\r"+
				"  `fye` int(11) DEFAULT 0,\r"+
				"  `form` varchar(50) DEFAULT '0',\r"+
				"  `fy` int(11) DEFAULT 0,\r"+
				"  `fp` varchar(50) DEFAULT 0,\r"+
				"its varchar(50) DEFAULT 0,\r"+
				"xml_file_name  varchar(250) DEFAULT 0,\r"+
				"  `accno` varchar(20) DEFAULT NULL,\r"+
				"  `tag` varchar(255) DEFAULT NULL,\r"+
				"  `ver` varchar(50) DEFAULT NULL,\r"+
				"  `co_reg` varchar(50) DEFAULT NULL,\r"+
				"  `date` int(11) DEFAULT NULL,\r"+
				"  `qtrs` int(11) DEFAULT NULL,\r"+
				"  `uom` varchar(50) DEFAULT NULL,\r"+
				"  `value` double DEFAULT NULL,\r"+
				"  `footnote` varchar(50) DEFAULT NULL,\r"+
				"  `report` int(11) DEFAULT NULL,\r"+
				"  `line` int(11) DEFAULT NULL,\r"+
				"  `pLabel` varchar(59) DEFAULT NULL,\r"+
				"  `negating` int(11) DEFAULT NULL,\r"+
				"  `stmt` varchar(50) DEFAULT NULL,\r"+
				"  `inpth` int(11) DEFAULT NULL,\r"+
				"  `rfile` varchar(50) DEFAULT NULL, key(accno),key(tag)\r"+
				") ENGINE=MYISAM DEFAULT CHARSET=latin1;\r"+
				"\r"+
				"insert ignore into "+yr+"q"+qtr+"fsdata_cp\r"+
				"select\r"+
				"accepted,cik\r"+
				",name\r"+
				",afs\r"+
				",wksi\r"+
				",fye\r"+
				",form\r"+
				",fy\r"+
				",fp\r"+
				",case \r"+
				"when left(instance,7)  rlike '-' then substring_index(instance,'-',1)\r"+
				"when right(left(instance,2),1) rlike '[0-9]' then left(instance,1)\r"+
				"when right(left(instance,3),1) rlike '[0-9]' then left(instance,2)\r"+
				"when right(left(instance,4),1) rlike '[0-9]' then left(instance,3)\r"+
				"when right(left(instance,5),1) rlike '[0-9]' then left(instance,4)\r"+
				"when right(left(instance,6),1) rlike '[0-9]' then left(instance,5)\r"+
				"else \r"+
				"instance end its,instance,\r"+
				"t1.*,t2.report,t2.line,t2.pLabel,t2.negating,t2.stmt,t2.inpth,t2.rfile from "+yr+"Q"+qtr+"FSData\r"+
				"t1,"+yr+"q"+qtr+"FSData_sub t3, "+yr+"Q"+qtr+"FSData_pre t2\r"+
				"\r"+
				"where stmt = 'CP' and co_reg =0 \r"+
				"and t3.accno=t2.accno and\r"+
				" t1.accno=t2.accno and t1.tag = t2.tag \r"+
				";\r"+
				"\r"+
				"\r"+
				"DROP TABLE IF EXISTS "+yr+"q"+qtr+"fsdata_un;\r"+
				"CREATE TABLE `"+yr+"q"+qtr+"fsdata_un` (\r"+
				"  `accepteddate` datetime DEFAULT '1901-01-01',\r"+
				"  `cik` int(11) DEFAULT 0,\r"+
				"coName varchar(250) DEFAULT '0',\r"+
				"    `filerType` varchar(150) DEFAULT '0',\r"+
				"  `wksi` int(11) DEFAULT 0,\r"+
				"  `fye` int(11) DEFAULT 0,\r"+
				"  `form` varchar(50) DEFAULT '0',\r"+
				"  `fy` int(11) DEFAULT 0,\r"+
				"  `fp` varchar(50) DEFAULT 0,\r"+
				"its varchar(50) DEFAULT 0,\r"+
				"xml_file_name  varchar(250) DEFAULT 0,\r"+
				"  `accno` varchar(20) DEFAULT NULL,\r"+
				"  `tag` varchar(255) DEFAULT NULL,\r"+
				"  `ver` varchar(50) DEFAULT NULL,\r"+
				"  `co_reg` varchar(50) DEFAULT NULL,\r"+
				"  `date` int(11) DEFAULT NULL,\r"+
				"  `qtrs` int(11) DEFAULT NULL,\r"+
				"  `uom` varchar(50) DEFAULT NULL,\r"+
				"  `value` double DEFAULT NULL,\r"+
				"  `footnote` varchar(50) DEFAULT NULL,\r"+
				"  `report` int(11) DEFAULT NULL,\r"+
				"  `line` int(11) DEFAULT NULL,\r"+
				"  `pLabel` varchar(59) DEFAULT NULL,\r"+
				"  `negating` int(11) DEFAULT NULL,\r"+
				"  `stmt` varchar(50) DEFAULT NULL,\r"+
				"  `inpth` int(11) DEFAULT NULL,\r"+
				"  `rfile` varchar(50) DEFAULT NULL, key(accno),key(tag)\r"+
				") ENGINE=MYISAM DEFAULT CHARSET=latin1;\r"+
				"\r"+
				"insert ignore into "+yr+"q"+qtr+"fsdata_un\r"+
				"select\r"+
				"accepted,cik\r"+
				",name\r"+
				",afs\r"+
				",wksi\r"+
				",fye\r"+
				",form\r"+
				",fy\r"+
				",fp\r"+
				",case \r"+
				"when left(instance,7)  rlike '-' then substring_index(instance,'-',1)\r"+
				"when right(left(instance,2),1) rlike '[0-9]' then left(instance,1)\r"+
				"when right(left(instance,3),1) rlike '[0-9]' then left(instance,2)\r"+
				"when right(left(instance,4),1) rlike '[0-9]' then left(instance,3)\r"+
				"when right(left(instance,5),1) rlike '[0-9]' then left(instance,4)\r"+
				"when right(left(instance,6),1) rlike '[0-9]' then left(instance,5)\r"+
				"else \r"+
				"instance end its,instance,\r"+
				"t1.*,t2.report,t2.line,t2.pLabel,t2.negating,t2.stmt,t2.inpth,t2.rfile from "+yr+"Q"+qtr+"FSData\r"+
				"t1,"+yr+"q"+qtr+"FSData_sub t3, "+yr+"Q"+qtr+"FSData_pre t2\r"+
				"\r"+
				"where stmt = 'UN' and co_reg =0 \r"+
				"and t3.accno=t2.accno and\r"+
				" t1.accno=t2.accno and t1.tag = t2.tag \r"+
				";\r";
		
		MysqlConnUtils.executeQuery(query);
		
	}

	public static void main(String[] args) throws ParseException, IOException,
			SQLException {

		// THIS WILL DOWNLOAD ALL FINANCIAL DATA SETS (XBRL FILES IN PLAIN CSV
		// FORMAT). THIS WILL AUTOMATICALLY BYPASS ALREADY DOWNLOADED FILES AS
		// LONG AS
		// THEY ARE NOT INVALID (THIS CHECKS IF FILE EXISTS AND IS VALID AND IF
		// NEITHER IS TRUE WILL DOWNLOAD).

		// this can be run each day - will only do anything if new quarterly
		// file -
		// whereupon it downloads quarterly FSData set and parses each of the
		// statement types into the corresponding quarterly table (eg
		// 2017q3FSData_IS has income statement data)
		endYear = Calendar.getInstance().get(Calendar.YEAR);

		int startYr = 2018;
		int endYr = startYr;
		int startQ = 1;
		int endQ = startQ;
		
		 DownloadFSDataMaster(startYr,startQ);
		
		String[] statements = { "IS", "Sales", FinancialStatement.salesNames, "IS", "NI",
				FinancialStatement.netIncNames, "CF", "cf_ops", FinancialStatement.cf_opsNames };

		for (; startYr <= endYr; startYr++) {
			for (; startQ <= endQ; startQ++) {
				FinancialStatement.getFinancialsFromFSDataSets(statements, startYr, endYr, startQ, endQ
						, true);
			}
		}
	}
}