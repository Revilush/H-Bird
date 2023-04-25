package contracts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrServerException;

import xbrl.MysqlConnUtils;
import xbrl.NLP;
import xbrl.Utils;

public class DisplayNames {

	public static void create_mysql_table_of_displaynames(int yr, int q) throws FileNotFoundException, SQLException {

		// This method creates contractTypes and fetches contract names and validates
		// them
		// any changes/cleanup to displayname must happen at the tmp_displayname or
		// displayname_Final, but not after. displayname_final is master list of
		// displaynames for contracts (those whose names are validated as homogenous and
		// for example not company specific contract names such as 'Air Products Inc
		// Lease"

		String startDate = yr + "1001", endDate = yr + "1231";
		if (q == 1) {
			startDate = yr + "0101";
			endDate = yr + "0331";

		}
		if (q == 2) {
			startDate = yr + "0401";
			endDate = yr + "0630";
		}
		if (q == 3) {
			startDate = yr + "0701";
			endDate = yr + "0930";
		}

		// see saved procedure at C:\MySQL_queries\final\CONTRACT_NAMES_CLEANED
		// procedure.sql -- procedure is also pasted below as well:

		String query = "\r\ncall contract_names_cleaned(\'" + startDate + "\','" + endDate + "');";//must have date encased in apostrophe 
		MysqlConnUtils.executeQuery(query);

	}

	public static void drop_and_delete_displayName_tables() throws FileNotFoundException, SQLException {
		String query = "/*recreate these tables to regenerate*/\r\n" + "\r\n"
				+ "DROP TABLE IF EXISTS DISPLAYNAME_final;\r\n" + "CREATE TABLE DISPLAYNAME_final\r\n" + "(\r\n"
				+ "`cik` int(10) NOT NULL DEFAULT 0,\r\n" + "`fdate` date NOT NULL DEFAULT '1901-01-01',\r\n"
				+ "`kid` varchar(25) CHARACTER SET latin1 NOT NULL DEFAULT '0',\r\n"
				+ "`ACC` varchar(24) CHARACTER SET latin1 NOT NULL DEFAULT '0',\r\n"
				+ "`filename` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "`displayName` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "`contractnamealgo` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "`contractType` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "`parent` varchar(255) NOT NULL DEFAULT '',\r\n" + "`modify` varchar(255) NOT NULL DEFAULT '', \r\n"
				+ "href_description text ,\r\n" + "source varchar(15) NOT NULL DEFAULT '',\r\n"
				+ "Primary Key (kid),\r\n" + "key(acc),\r\n" + "key(filename),\r\n"
				+ "key(fdate), \r\nkey(displayName),\r\n" + "key (acc,fdate)\r\n"
				+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;\r\n" + "\r\n"
				+ "/*once tmp_DISPLAYNAME_CIK_DNAME is final insert all from tmp*/\r\n" + "\r\n"
				+ "DROP TABLE IF EXISTS DISPLAYNAME_CIK_DNAME;\r\n" + "CREATE TABLE DISPLAYNAME_CIK_DNAME\r\n" + "(\r\n"
				+ "`cik` varchar(10) NOT NULL DEFAULT '0',\r\n"
				+ "`ACC` varchar(24) CHARACTER SET latin1 NOT NULL DEFAULT '0',\r\n"
				+ "`kid` varchar(25) CHARACTER SET latin1 NOT NULL DEFAULT '0',\r\n"
				+ "`fdate` date NOT NULL DEFAULT '1901-01-01',\r\n" + "`filename` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "href_description text,\r\n" + "`displayName` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "`contractnamealgo` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "`contractType` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "`parent` varchar(255) NOT NULL DEFAULT '',\r\n" + "`modify` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "source varchar(15) NOT NULL DEFAULT '',\r\n" + "primary key (cik,displayname),\r\n"
				+ "unique key (kid),\r\n" + "key(acc),\r\n" + "key(kid),\r\n" + "key(filename),\r\n" + "key(fdate),\r\n"
				+ "key (acc,fdate)\r\n" + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;\r\n"
				+ "\r\n" + "\r\n" + "DROP TABLE IF EXISTS DISPLAY_NAME_TABLE_dnu_href_all; \r\n"
				+ "CREATE TABLE `DISPLAY_NAME_TABLE_dnu_href_all` (\r\n" + "  `cik` int NOT NULL DEFAULT '0',\r\n"
				+ "  `kid` varchar(25) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '0',\r\n"
				+ "  `displayname` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "  `contracttype` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "  `parent` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "  `modify` varchar(255) NOT NULL DEFAULT '',\r\n" + "  `source` varchar(15) NOT NULL DEFAULT '',\r\n"
				+ "  `cnt` int(4) NOT NULL DEFAULT '0',\r\n" + "  `href_description` varchar(255),\r\n"
				+ "  primary key(kid)\r\n" + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;\r\n"
				+ "\r\n" + " DROP TABLE IF EXISTS DISPLAY_NAME_TABLE_dnu_href; \r\n"
				+ "CREATE TABLE `DISPLAY_NAME_TABLE_dnu_href` (\r\n" + "  `cik` int NOT NULL DEFAULT '0',\r\n"
				+ "  `kid` varchar(25) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '0',\r\n"
				+ "  `displayname` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "  `contracttype` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "  `parent` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "  `modify` varchar(255) NOT NULL DEFAULT '',\r\n" + "  `source` varchar(15) NOT NULL DEFAULT '',\r\n"
				+ "  `cnt` int(4) NOT NULL DEFAULT '0',\r\n" + "  `href_description` varchar(255),\r\n"
				+ "  primary key(kid)\r\n" + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;\r\n"
				+ "\r\n" + "\r\n" + "DROP TABLE IF EXISTS DISPLAY_NAME_TABLE_dnu_href_2; \r\n"
				+ "CREATE TABLE `DISPLAY_NAME_TABLE_dnu_href_2` (\r\n" + "  `cik` int NOT NULL DEFAULT '0',\r\n"
				+ "  `kid` varchar(25) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '0',\r\n"
				+ "  `displayname` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "  `contracttype` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "  `parent` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "  `modify` varchar(255) NOT NULL DEFAULT '',\r\n" + "  `source` varchar(15) NOT NULL DEFAULT '',\r\n"
				+ "  `cnt` int(4) NOT NULL DEFAULT '0',\r\n" + "  `href_description` varchar(255),\r\n"
				+ "  primary key(kid)\r\n" + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;\r\n"
				+ "\r\n" + "DROP TABLE IF EXISTS DISPLAY_NAME_TABLE_dnu_href_3; \r\n"
				+ "CREATE TABLE `DISPLAY_NAME_TABLE_dnu_href_3` (\r\n" + "  `cik` int NOT NULL DEFAULT '0',\r\n"
				+ "  `kid` varchar(25) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '0',\r\n"
				+ "  `displayname` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "  `contracttype` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "  `parent` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "  `modify` varchar(255) NOT NULL DEFAULT '',\r\n" + "  `source` varchar(15) NOT NULL DEFAULT '',\r\n"
				+ "  `cnt` int(4) NOT NULL DEFAULT '0',\r\n" + "  `href_description` varchar(255),\r\n"
				+ "key (displayname),\r\n" + "key (kid)," + "  primary key(kid)\r\n"
				+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;\r\n" + "";

		MysqlConnUtils.executeQuery(query);
	}

	public static void validateContractDisplayNames(int yr, int qtr) throws FileNotFoundException, SQLException {
		// Note that I grab a long history of displaynames to measure validity. However,
		// I need to look forward and backward if I am regenerating the data so I always
		// look 2 years ahead and 2 behind. Which in 2018 gives me 4 years.

		// this table is created each time and fetches five year of metadata to run
		// displayname analysis.

		String query = "drop table if exists tmp_metadata_to_use_for_displaynames;\r\n"
				+ "CREATE TABLE `tmp_metadata_to_use_for_displaynames` (\r\n"
				+ "  `kId` varchar(24) NOT NULL DEFAULT '0',\r\n" + "  `fDate` date NOT NULL DEFAULT '1901-01-01',\r\n"
				+ "  `fSize` int DEFAULT NULL,\r\n" + "  `acc` varchar(20) DEFAULT NULL,\r\n"
				+ "  `cik` int DEFAULT NULL,\r\n" + "  `formType` varchar(50) DEFAULT NULL,\r\n"
				+ "  `companyName` varchar(155) DEFAULT NULL,\r\n"
				+ "  `acc_link_filename` varchar(100) DEFAULT NULL,\r\n"
				+ "  `edgarLink` varchar(250) DEFAULT NULL,\r\n" + "  `contractLongName` varchar(250) DEFAULT NULL,\r\n"
				+ "  `type` varchar(50) DEFAULT NULL,\r\n" + "  `legalEntitiesInOpeningParagraph` text,\r\n"
				+ "  `numberOfLegalEntities` int DEFAULT NULL,\r\n" + "  `openingParagraph` text,\r\n"
				+ "  `openParaContractName` varchar(255) DEFAULT NULL,\r\n"
				+ "  `contractNameAlgo` varchar(255) DEFAULT NULL,\r\n" + "  `score` int DEFAULT NULL,\r\n"
				+ "  `legalEntities` text,\r\n" + "  `governinglaw` varchar(155) DEFAULT NULL,\r\n"
				+ "  PRIMARY KEY (`fDate`,`kId`),\r\n" + "  KEY `kId` (`kId`),\r\n" + "  KEY `fDate` (`fDate`),\r\n"
				+ "  KEY `acc` (`acc`),\r\n" + "  KEY `CIK` (`cik`),\r\n" + "  KEY `type` (`type`),\r\n"
				+ "  KEY `companyname` (`companyName`)\r\n" + ") ENGINE=InnoDB DEFAULT CHARSET=latin1\r\n"
				+ "/*!50100 PARTITION BY RANGE (year(`fDate`))\r\n"
				+ "(PARTITION p1 VALUES LESS THAN (1982) ENGINE = InnoDB,\r\n"
				+ " PARTITION p2 VALUES LESS THAN (1984) ENGINE = InnoDB,\r\n"
				+ " PARTITION p3 VALUES LESS THAN (1986) ENGINE = InnoDB,\r\n"
				+ " PARTITION p4 VALUES LESS THAN (1988) ENGINE = InnoDB,\r\n"
				+ " PARTITION p5 VALUES LESS THAN (1990) ENGINE = InnoDB,\r\n"
				+ " PARTITION p6 VALUES LESS THAN (1992) ENGINE = InnoDB,\r\n"
				+ " PARTITION p7 VALUES LESS THAN (1994) ENGINE = InnoDB,\r\n"
				+ " PARTITION p8 VALUES LESS THAN (1996) ENGINE = InnoDB,\r\n"
				+ " PARTITION p9 VALUES LESS THAN (1998) ENGINE = InnoDB,\r\n"
				+ " PARTITION p10 VALUES LESS THAN (2000) ENGINE = InnoDB,\r\n"
				+ " PARTITION p11 VALUES LESS THAN (2002) ENGINE = InnoDB,\r\n"
				+ " PARTITION p12 VALUES LESS THAN (2004) ENGINE = InnoDB,\r\n"
				+ " PARTITION p13 VALUES LESS THAN (2006) ENGINE = InnoDB,\r\n"
				+ " PARTITION p14 VALUES LESS THAN (2008) ENGINE = InnoDB,\r\n"
				+ " PARTITION p15 VALUES LESS THAN (2009) ENGINE = InnoDB,\r\n"
				+ " PARTITION p16 VALUES LESS THAN (2010) ENGINE = InnoDB,\r\n"
				+ " PARTITION p17 VALUES LESS THAN (2011) ENGINE = InnoDB,\r\n"
				+ " PARTITION p18 VALUES LESS THAN (2012) ENGINE = InnoDB,\r\n"
				+ " PARTITION p19 VALUES LESS THAN (2013) ENGINE = InnoDB,\r\n"
				+ " PARTITION p20 VALUES LESS THAN (2014) ENGINE = InnoDB,\r\n"
				+ " PARTITION p21 VALUES LESS THAN (2015) ENGINE = InnoDB,\r\n"
				+ " PARTITION p22 VALUES LESS THAN (2016) ENGINE = InnoDB,\r\n"
				+ " PARTITION p23 VALUES LESS THAN (2017) ENGINE = InnoDB,\r\n"
				+ " PARTITION p24 VALUES LESS THAN (2018) ENGINE = InnoDB,\r\n"
				+ " PARTITION p25 VALUES LESS THAN (2019) ENGINE = InnoDB,\r\n"
				+ " PARTITION p26 VALUES LESS THAN (2020) ENGINE = InnoDB,\r\n"
				+ " PARTITION p27 VALUES LESS THAN (2021) ENGINE = InnoDB,\r\n"
				+ " PARTITION p28 VALUES LESS THAN (2022) ENGINE = InnoDB,\r\n"
				+ " PARTITION p29 VALUES LESS THAN MAXVALUE ENGINE = InnoDB) */;\r\n" + "\r\n";
		MysqlConnUtils.executeQuery(query);

//		cycles through five years of history preceding startyear
		String dayStrStart = "01", dayStrEnd = "30";
		String moStrStart = "01", moStrEnd = "03";
		int sQ = qtr;
		if (qtr == 1) {
			moStrStart = "01";
			moStrEnd = "03";
			dayStrEnd = "31";
		}
		if (qtr == 2) {
			moStrStart = "04";
			moStrEnd = "06";
		}

		if (qtr == 3) {
			moStrStart = "07";
			moStrEnd = "09";
		}
		if (qtr == 3) {
			moStrStart = "10";
			moStrEnd = "12";
			dayStrEnd = "31";
		}

		int endYear = yr + 2;// 2 years after current year. if 2018, then 2019 and 2020.
		int startYear = yr;
		for (; startYear < endYear; startYear++) {

			for (; startYear < endYear; startYear++) {
				for (; sQ < 5; sQ++) {
					dayStrEnd = "30";
					if (sQ == 1) {
						dayStrEnd = "31";
						moStrStart = "01";
						moStrEnd = "03";
					}
					if (sQ == 2) {
						moStrStart = "04";
						moStrEnd = "06";
					}

					if (sQ == 3) {
						moStrStart = "07";
						moStrEnd = "09";
					}
					if (sQ == 4) {
						dayStrEnd = "31";
						moStrStart = "10";
						moStrEnd = "12";
					}

					query = "insert ignore into tmp_metadata_to_use_for_displaynames\r\n"
							+ " select * from metadata where fdate>='" + (startYear + moStrStart + dayStrStart)
							+ "' and fdate<='" + (startYear + moStrEnd + dayStrEnd) + "';\r\n";
					MysqlConnUtils.executeQuery(query);
				}
				sQ = 1;
			}

			MysqlConnUtils.executeQuery(query);
		}

		endYear = yr;
		startYear = yr - 2;// 2 years before current year - eg. if 2018, then 2017 and 2016.
		sQ = qtr;

		for (; startYear < endYear; startYear++) {
			for (; sQ < 5; sQ++) {
				dayStrEnd = "30";
				if (sQ == 1) {
					dayStrEnd = "31";
					moStrStart = "01";
					moStrEnd = "03";
				}
				if (sQ == 2) {
					moStrStart = "04";
					moStrEnd = "06";
				}

				if (sQ == 3) {
					moStrStart = "07";
					moStrEnd = "09";
				}
				if (sQ == 4) {
					dayStrEnd = "31";
					moStrStart = "10";
					moStrEnd = "12";
				}

				query = "insert ignore into tmp_metadata_to_use_for_displaynames\r\n"
						+ " select * from metadata where fdate>='" + (startYear + moStrStart + dayStrStart)
						+ "' and fdate<='" + (startYear + moStrEnd + dayStrEnd) + "';\r\n";
				MysqlConnUtils.executeQuery(query);
			}
			sQ = 1;
		}

		query = "\r\n" + "DROP TABLE IF EXISTS MASTER_DISPLAYNAMES;\r\n" + "CREATE TABLE `master_displaynames` (\r\n"
				+ "  `contractNameOk` int NOT NULL DEFAULT '0',\r\n"
				+ "  `displayname` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "  `parent` varchar(255) NOT NULL DEFAULT '',\r\n"
				+ "  `contracttype` varchar(255) NOT NULL DEFAULT '',\r\n" + "  KEY `displayname` (`displayname`)\r\n"
				+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;\r\n" + "\r\n"
				+ "/*tmp_metadata_to_use_for_displaynames houses up to 5 years of data*/\r\n\r\n"
				+ "insert ignore into master_displaynames\r\n" + "select\r\n" + "case\r\n"
				+ "when (cnt>5 or (cnt2>15 and cnt>2) or (score>2 and cnt>3) or (score>3 and cnt>1) or (t1.contractType rlike 'token|future equit') ) \r\n"
				+ "and t1.displayname not rlike '^( any |class|contract|additional|additional agreement|agreement)$|original|execution|signature|page|cambridge|mplx|csg|mccann|pimco'\r\n"
				+ "then 1 else -1 end contractNameOk, t1.displayname,parent,t1.contracttype\r\n"
				+ "from (select count(*) cnt2,score,displayname,contracttype,cnt from DISPLAY_NAME_TABLE_dnu_href_3 t1 inner join tmp_metadata_to_use_for_displaynames t2 \r\n"
				+ "on t1.kid = t2.kId where length(t1.displayname)>3 \r\n" + "group by displayname) T1 \r\n"
				+ "inner join displayname_final t2 on T1.displayname = t2.displayName \r\n" + "group by displayname ;";

		MysqlConnUtils.executeQuery(query);
	}

	public static String makeContractNamesInitialCaps(String contractNames) throws IOException {

		NLP nlp = new NLP();
		Pattern patternStopWords = Pattern.compile("(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" + ""
				+ "(in case)|(a)|(about)|(above)|(after)|(again)|(all)|(am)|(an)|(any)|(and)|(are)|(as)|(at)|(be)"
				+ "|(because)|(been)|(before)|(being)|(below)|(between)|(both)|(by)|(can)|(day)|(did)|(do)|(does)|(doing)|(down)"
				+ "|(during)|(each)|(either)|(few)|(for)|(from)|(further)|(had)|(has)|(have)|(having)|(he)|(her)|(here)|(hers)"
				+ "|(herself)|(him)|(himself)|(his)|(how)|(I)|(if)|(in)|(into)|(is)|(it)|(its)|(it\'s)|(itself)|(just)|(me)"
				+ "|(might)|(more)|(most)|(must)|(my)|(myself)|(need)|(now)|(of)|(off)|(on)|(once)|(only)|(or)|(other)|(otherwise)"
				+ "|(our)|(ours)|(ourselves)|(out)|(over)|(own)|(same)|(she)|(she\'s)|(should)|(so)|(some)|(such)|(than)"
				+ "|(that)|(the)|(their)|(theirs)|(them)|(themselves)|(then)|(there)|(these)|(they)|(this)|(those)|(through)"
				+ "|(to)|(too)|(under)|(until)|(up)|(very)|(very)|(was)|(we)|(were)|(what)|(when)|(where)|(whether)|(which)|(while)|(who)"
				+ "|(whom)|(why)|(will)|(with)|(won)|(would)|(you)|(your)|(yours)|(yourself)|(yourselves)" + ""
				+ ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

		StringBuilder sb = new StringBuilder();
		String[] contractNamesSplit = contractNames.split("\r\n");
		String contractName = "";
		String word = "", hdgInitialCaps = "";
		for (int n = 0; n < contractNamesSplit.length; n++) {
			contractName = contractNamesSplit[n];
			contractName = contractName.replaceAll("(?<=(^| )(Lock|Non))( )(?=([A-Za-z]{1}))", "-");
			hdgInitialCaps = "";
			String[] words = contractName.split(" ");

			for (int i = 0; i < words.length; i++) {
				word = words[i];
				if (word.trim().length() < 1)
					continue;
				if (nlp.getAllMatchedGroups(word.toLowerCase(), patternStopWords).size() > 0 && i > 0)
					word = word.toLowerCase();
				if (
						word.trim().length() > 3 || nlp
						.getAllIndexEndLocations(word, Pattern.compile("(?ism)(^| )(Tax|Fee|Day|Nut|Net|Co|New)($| )"))
						.size() > 0)
					
					word = word.substring(0, 1).toUpperCase() + word.substring(1, word.length()).toLowerCase();
				if (nlp.getAllIndexEndLocations(word, Pattern.compile("(?ism)[A-Z]-[a-z]")).size() > 0) {
					word = word.replaceAll("-", "xx");
					String wordHypJoin = "";
					String[] wordHyp = word.split("xx");
					for (int c = 0; c < wordHyp.length; c++) {
						if(wordHyp[c].length()<1)
							continue;
//						System.out.println("wordHyp="+wordHyp[c]);
						wordHypJoin = wordHypJoin + "-" + wordHyp[c].substring(0, 1).toUpperCase()
								+ wordHyp[c].substring(1, wordHyp[c].length()).toLowerCase();
					}
					wordHypJoin = wordHypJoin.replaceAll("xx", "-");
					word = wordHypJoin.replaceAll("^-", "");

				}

				if (nlp.getAllIndexEndLocations(word, Pattern.compile("(?ism)(^| )(ltip|vmtp|Vrdp|vzmt|rsu|safe)($| )"))
						.size() > 0)
					word = word.toUpperCase();

				word = word.replaceAll("(?sm)(^| )(Nonqual)", "$1Non-Qual")
						.replaceAll("(?sm)(^| )(Nonempl)", "$1Non-Empl").replaceAll("(^| )Nonstat", "$1Non-Stat")
						.replaceAll("(^| )Subadv", "$1Sub-Adv").replaceAll("(^| )Multifam", "$1Multi-Fam")
						.replaceAll("(^| )Time Based", "$1Time-Based");
				word = word.replaceAll("(Mezzanine|Series|Class) a ", "$1 A ").replaceAll("U\\.s\\.", "U\\.S\\.");
				hdgInitialCaps = hdgInitialCaps + " " + word;
			}
			sb.append(hdgInitialCaps.trim() + "\r\n");
		}
		return sb.toString();
	}

	public static void createListOfHeadingAndDefinitionDispolaynames(String type)
			throws FileNotFoundException, SQLException {
		// either allDefs, allHdgs, allQuotedTerms, or allExhs
		String folder = "e:/getContracts/solrIngestion/DisplayNames/";
		String filename = folder + type + ".csv";
		Utils.createFoldersIfReqd(folder);
		File file = new File(filename);
		if (file.exists())
			file.delete();

		// NOTE: allHdgs/allDefs etc have both the original heading and the heading
		// split by ';'. If the heading reads: Servicer Termination Event; and Master
		// Servicing. Then in the allHdgs table there are 3 rows associated with this
		// hdg. 1. Servicer Termination Event; and Master Servicing. 2. Servicer
		// Termination Event and 3. Master Servicing. 2 and 3 are marked with a
		// preceding "split_" in the MySQL table. Once the below is run all split
		// headings (2 and 3) are filtered out using the 'where allhdgs not rlike
		// '^split' condition. But if I want to change it to include split headings I
		// remove the where clause (the allhdgs already replace split_). Then if I want
		// to not include the original heading filter out using where clause if rlike
		// ';'
		

		String query ="DROP TABLE IF EXISTS "+type+"__TMP1;\r\n"+
				"CREATE TABLE "+type+"__TMP1\r\n"+
				"(\r\n"+
				"  `aclc` varchar(5) NOT NULL DEFAULT '0',\r\n"+
				"  `"+type+"` varchar(155),\r\n"+
				"  `kid` varchar(25) NOT NULL DEFAULT '0',\r\n"+
				"  `cik` int(10) DEFAULT NULL,\r\n"+
				"  `yr` int(5) DEFAULT NULL,\r\n"+
				"  `qtr` int(2) DEFAULT NULL,\r\n"+
				"  `parentgroup` varchar(155) NOT NULL DEFAULT '',\r\n"+
				"  `contracttype` varchar(155) NOT NULL DEFAULT '',\r\n"+
				"  `contractnamealgo` varchar(155) NOT NULL DEFAULT '',\r\n"+
				"  key(yr),\r\n"+
				"  key(qtr),\r\n"+
				"  key(cik)\r\n"+
				") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;\r\n"+
				"\r\n"+
				"INSERT IGNORE INTO "+type+"__TMP1\r\n"+
				"select case when BINARY "+type+" =  BINARY UPPER("+type+") then 'AC' else 'lc' end aclc,\r\n"+
				"LEFT(TRIM(regexp_replace("+type+",'^(the) | ?, ?$|^split_','')),155) \r\n"+
				""+type+" ,kid,LEFT(KID,10) CIK,year(fDate) yr,quarter(fdate) qtr,parentGroup,contractType,contractnamealgo\r\n"+
				"from\r\n"+
				""+type+" T1 GROUP BY year(fdate),quarter(fdate),LEFT(KID,10),parentgroup,ACLC,"+type+" ;\r\n"+
				"/*this establishes max of 1 hdg per cik*/\r\n"+
				"\r\n"+
				"DROP TABLE IF EXISTS "+type+"_TMP1;\r\n"+
				"CREATE TABLE "+type+"_TMP1 (\r\n"+
				"  `aclc` varchar(5) NOT NULL DEFAULT '0',\r\n"+
				"  `"+type+"` varchar(155),\r\n"+
				"  `kid` varchar(25) NOT NULL DEFAULT '0',\r\n"+
				"  `cik` int(10) DEFAULT NULL,\r\n"+
				"  `yr` int(5) DEFAULT NULL,\r\n"+
				"  `qtr` int(2) DEFAULT NULL,\r\n"+
				"  `parentgroup` varchar(155) NOT NULL DEFAULT '',\r\n"+
				"  `contracttype` varchar(155) NOT NULL DEFAULT '',\r\n"+
				"  `contractnamealgo` varchar(155) NOT NULL DEFAULT ''\r\n"+
				") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;\r\n"+
				"\r\n"+
				"\r\n"+
				"insert ignore into "+type+"_tmp1\r\n"+
				"select case when BINARY "+type+" =  BINARY UPPER("+type+") then 'AC' else 'lc' end aclc,\r\n"+
				"LEFT(TRIM(regexp_replace("+type+",'^(the) | ?, ?$|^split_','')),155) \r\n"+
				""+type+" ,kid,cik, yr, qtr,parentGroup,contractType,contractnamealgo\r\n"+
				"from "+type+"__TMP1 t1 where "+type+" not rlike '^split_' \r\n"+
				"group by yr,qtr,cik,parentgroup,ACLC,"+type+" ;\r\n"+
				"ALTER TABLE "+type+"_TMP1 ADD KEY (yr), ADD KEY (Qtr), ADD KEY ("+type+");\r\n"+
				"\r\n"+
				"DROP TABLE IF EXISTS "+type+"_TMP2;\r\n"+
				"CREATE TABLE `"+type+"_TMP2` (\r\n"+
				"  `c` int(10) NOT NULL DEFAULT '0',\r\n"+
				"  `ck` varchar(4) NOT NULL DEFAULT '',\r\n"+
				"  `aclc` varchar(2) NOT NULL DEFAULT '',\r\n"+
				"  `"+type+"` varchar(255),\r\n"+
				"  `kid` varchar(25) NOT NULL,\r\n"+
				"  `cik` int NOT NULL,\r\n"+
				"  `yr` int DEFAULT NULL,\r\n"+
				"  `qtr` int DEFAULT NULL,\r\n"+
				"  `parentGroup` varchar(155) NOT NULL,\r\n"+
				"  `contractType` varchar(155) NOT NULL,\r\n"+
				"  `contractnamealgo` varchar(155) NOT NULL\r\n"+
				") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;\r\n"+
				"\r\n"+
				"\r\n"+
				"set @hdgPrior='abc';\r\n"+
				"set @acPrior='abcde';\r\n"+
				"\r\n"+
				"insert ignore into "+type+"_TMP2\r\n"+
				"select count(*) c,\r\n"+
				"case when aclc='AC' and @acPrior='lc' and @hdgPrior="+type+" then 'omit' else 'keep' end ck,\r\n"+
				"@acPrior:=aclc aclc,\r\n"+
				"@hdgPrior:= "+type+",kid,cik,yr,qtr,parentGroup,contractType,contractnamealgo\r\n"+
				"from "+type+"_tmp1\r\n"+
				" t1 where  "+type+" not rlike '^[a-z\\\\d]{1} |^(%|-)' group by yr,qtr, parentGroup,ACLC,"+type+" ;\r\n"+
				" \r\n"+
				"\r\n"+
				"DROP TABLE IF EXISTS "+type+"_DISPLAYNAMES;\r\n"+
				"CREATE TABLE `"+type+"_displaynames` (\r\n"+
				"  `kid` VARCHAR(25),\r\n"+
				"  `"+type+"Final` VARCHAR(255),\r\n"+
				"  `parentGroup` varchar(155) NOT NULL,\r\n"+
				"  `cnt` int(10) NOT NULL,\r\n"+
				"  `aclc` varchar(4) NOT NULL,\r\n"+
				"  key("+type+"final),\r\n"+
				"  key(parentgroup)\r\n"+
				") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;\r\n"+
				"\r\n"+
				"\r\n"+
				"SET @acPrior='lc'; SET @hdgPrior = 'abc'; SET @fcCnt=9, @hrCnt=4;SET  @"+type+":='abcd';\r\n"+
				"INSERT IGNORE INTO "+type+"_DISPLAYNAMES\r\n"+
				"select kid,"+type+"Final,parentGroup,c,aclc from (\r\n"+
				"select kid,\r\n"+
				"case when aclc='AC' and @"+type+"="+type+" then @"+type+" else "+type+" end "+type+"Final,\r\n"+
				"@"+type+":="+type+" "+type+"\r\n"+
				",parentgroup,c,aclc from (\r\n"+
				"select c,aclc, "+type+",ck,\r\n"+
				"kid,cik, yr, qtr,parentgroup,contracttype,contractnamealgo from "+type+"_TMP2 t1 where\r\n"+
				"( (c>@fcCnt and parentGroup rlike 'financial contracts') or (c>@hrCnt and parentGroup not rlike 'financial contracts') ) \r\n"+
				"and "+type+" not rlike '^(and)$|^\\\\(' and ck='keep' AND "+type+" not rlike '(exhibit|annex|schedule|appendix) [\\\\dA-Za-z]{1,4} ?$|BYE-LAW'\r\n"+
				"group by parentGroup,"+type+",aclc order by "+type+",aclc desc\r\n"+
				") t1 \r\n"+
				"group by parentgroup,aclc,"+type+" order by parentgroup,"+type+",aclc desc ) t1\r\n"+
				"where length("+type+"final)>2 and "+type+"final not rlike '^S\\\\. '\r\n"+
				"group by parentgroup,"+type+" ;\r\n"
				+"SELECT "+type+"Final,parentGroup FROM "+type+"_DISPLAYNAMES\r\n"
				+" \r\n\r\n" + "INTO OUTFILE '" + filename + "';\r\n";

		MysqlConnUtils.executeQuery(query);

	}

	public static void main(String[] args) throws IOException, SQLException, SolrServerException {

//		createListOfHeadingAndDefinitionDispolaynames("");
		
		int sY = 2017, eY = 2022, sQ = 1, eQ = 4,sYr=sY,eYr=eY,sQr=sQ,eQr=eQ;

		boolean regenerate = false;// if true then cycle through full history. otherwise just insert new.
		if (regenerate) {
			DisplayNames.drop_and_delete_displayName_tables();// only if I regenerate create_mysql_table_of_displaynames
		}
//
		for (; sY <= eY; sY++) {
			for (; sQ <= eQ; sQ++) {
				DisplayNames.create_mysql_table_of_displaynames(sY, sQ); // this runs immediately after metadata is in
			}
			sQ = 1;
		}

		sY = sYr;
		eY = eYr;
		sQ = sQr;
		eQ = eQr;

		for (; sY <= eY; sY++) {
			for (; sQ <= eQ; sQ++) {
				DisplayNames.validateContractDisplayNames(sY, sQ);
			}
			sQ = 1;
		}

	}
}
