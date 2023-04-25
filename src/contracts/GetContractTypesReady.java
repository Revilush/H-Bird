package contracts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

import xbrl.MysqlConnUtils;
import xbrl.NLP;
import xbrl.QuarterlyMasterIdx;
import xbrl.Utils;

public class GetContractTypesReady {

	public static String mySQL_contract_folder = "E:\\getContracts\\MySQL_contract_type_tables\\";
	public static String mySQL_parent_group_folder = "E:\\getContracts\\MySQL_parent_group_tables\\";
	public static String mySQL_masterIdx_new = "E:/getContracts/masterIdx_new/";
	public static String masterIdxKsPath = "e:/getContracts/masterIdx_Ks/";
	public static boolean regenerate_metadata_json_all = false;

	public static void import_sector_industry_ticker_CIK(String filename_sector_industry, String filename_ticker_CIK)
			throws FileNotFoundException, SQLException {

		String query = "\r\n" + "DROP TABLE IF EXISTS COMPANY_NAME_TICKER_INDUSTRY_SECTOR;\r\n"
				+ "CREATE TABLE `company_name_ticker_industry_sector` (\r\n"
				+ "  `company_name` varchar(155) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '0',\r\n"
				+ "  `ticker` varchar(25) NOT NULL DEFAULT '',\r\n"
				+ "  `industry` varchar(155) NOT NULL DEFAULT '',\r\n"
				+ "  `sector` varchar(155) NOT NULL DEFAULT '',\r\n" + "  PRIMARY KEY (ticker),\r\n"
				+ "  key (company_name)\r\n" + ") ENGINE=InnoDB ;\r\n" + "\r\n" + "\r\n"
				+ "/*make the file a variable. Leave a note to remove quotes and to ensure columns are tab separated*/\r\n"
				+ "LOAD DATA INFILE '" + filename_sector_industry + "' \r\n"
				+ "IGNORE INTO TABLE contracts.company_name_ticker_industry_sector \r\n" + "CHARACTER SET latin1\r\n"
				+ "FIELDS TERMINATED BY '\\t' \r\n" + ";\r\n" + "\r\n" + "\r\n" + "DROP TABLE IF EXISTS TICKER_CIK;\r\n"
				+ "CREATE TABLE `ticker_CIK` (\r\n" + "  `ticker` varchar(25) NOT NULL DEFAULT '',\r\n"
				+ "  `CIK` INT(10) NOT NULL DEFAULT 0,\r\n" + "  PRIMARY KEY (ticker),\r\n" + "  key (cik)\r\n"
				+ ") ENGINE=InnoDB ;\r\n" + "\r\n" + "\r\n" + "/*make the file a variable.*/ \r\n"
				+ "LOAD DATA INFILE '" + filename_ticker_CIK + "' \r\n" + "IGNORE INTO TABLE contracts.ticker_CIK \r\n"
				+ "CHARACTER SET latin1\r\n" + "FIELDS TERMINATED BY '\\t' ;\r\n"
				+ "UPDATE LOW_PRIORITY IGNORE contracts.company_name_ticker_industry_sector\r\n" + " SET \r\n"
				+ " company_name = REGEXP_REPLACE(company_name,'(?ism)((?= )[IV]{1,4})?( CORPORATION| CORP\\\\.?| INCORPORATION\\\\.| Company|S\\\\.A\\\\.| P\\\\.?L\\\\.?C\\\\.?|( [IV]{1,4})? (Co\\\\.?|CORP\\\\.|INC\\\\.?|l\\\\.?t\\\\.?d\\\\.?)| CO\\\\.?( |$)?| L\\\\.?L\\\\.?C\\\\.?| L?\\\\.?L\\\\.?P\\\\.?| L\\\\.?T\\\\.?D\\\\.?| ?/ ?[A-Z]{2,3}/?|\\\\.| (TX|NY|CA|MI|FL|DE)$|LIMITED| AG($| )?| ?& ?Co\\\\.?$|, | ?&| AND )+( [IV]{1,4}$)?','');\r\n"
				+ "\r\n" + "\r\n" + "DROP TABLE IF EXISTS company_name_ticker_industry_sector_cik;\r\n"
				+ "CREATE TABLE company_name_ticker_industry_sector_cik\r\n"
				+ "select t1.*,t2.cik from COMPANY_NAME_TICKER_INDUSTRY_SECTOR t1 \r\n"
				+ "left join ticker_CIK t2 on t1.ticker=t2.ticker;\r\n"
				+ "ALTER TABLE company_name_ticker_industry_sector_cik ADD KEY (CIK), add key(company_name);\r\n";
		MysqlConnUtils.executeQueryDB(query, "contracts");

	}

	public static String export_metadata_as_masterIdx(int yr, int qtr, boolean exportMasterIdx,
			List<String> listParentGroupToParse, String modifiedFilter) throws IOException, SQLException {

		if (regenerate_metadata_json_all) {
			MysqlConnUtils.executeQuery("call CREATE_TABLE_metadata_json_all();\r\n");

		}
		// don't export master idx if all I want is to regenerate metadata_json_all tbl.
		// This is the table that reflects what was culled based on what is in
		// metadata/metadata_href of all files sought to create jsons for. So if a file
		// is not in ES but in this table determine the reason for its failure to parse.

		// System.out.println("minSize=" + minSize);

		String query_update = "UPDATE METADATA_JSON_ALL\r\n" + "SET CONTRACTNAMEOK=-1 \r\n"
				+ "WHERE CONTRACTNAMEALGO RLIKE '^(reference)$' and contractType rlike 'indenture' and contractnameok=1;\r\n"
				+ "\r\n" + "UPDATE METADATA_JSON_ALL\r\n" + "SET CONTRACTNAMEOK=1 \r\n"
				+ "WHERE CONTRACTNAMEALGO RLIKE '^(CLO Indenture|Contingent Capital Securities Indenture|Contingent Convertible Capital Securities Indenture|Convertible Indenture|Convertible Senior Notes Indenture|Coupon Bonds Indenture|Credit Enhanced Bonds Indenture|Guarantee Indenture|Junior Secured Notes Indenture|Junior Subordinated Indenture|Mortgage Indenture|Omnibus Indenture|Omnibus Master Indenture|PIK Notes Indenture|Senior Secured Stub Notes Indenture|Special Warrant Indenture|Subordinate Indenture|Subordinate Voting Share Purchase Warrant Indenture|Unsecured Senior Notes Indenture|Unsubordinated Indenture)$';\r\n"
				+ "\r\n" + "UPDATE METADATA_JSON_ALL\r\n" + "SET CONTRACTNAMEALGO = \r\n" + "case\r\n" + "\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Indenture$' then 'Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Senior Indenture$' then 'Senior Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Subordinated Indenture$' then 'Subordinated Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Senior Notes Indenture$' then 'Senior Notes Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Master Indenture$' then 'Master Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Trust Indenture$' then 'Trust Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Senior Subordinated Indenture$' then 'Senior Subordinated Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Subordinated Debt Indenture$' then 'Subordinated Debt Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Senior Debt Indenture$' then 'Senior Debt Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Base Indenture$' then 'Base Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Junior Subordinated Indenture$' then 'Junior Subordinated Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Warrant Indenture$' then 'Warrant Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Guarantee Indenture$' then 'Guarantee Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Senior Secured Indenture$' then 'Senior Secured Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Convertible Indenture$' then 'Convertible Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Special Warrant Indenture$' then 'Special Warrant Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Omnibus Indenture$' then 'Omnibus Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Convertible Senior Notes Indenture$' then 'Convertible Senior Notes Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Contingent Capital Securities Indenture$' then 'Contingent Capital Securities Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Subordinate Indenture$' then 'Subordinate Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Mortgage Indenture$' then 'Mortgage Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^PIK Notes Indenture$' then 'PIK Notes Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Omnibus Master Indenture$' then 'Omnibus Master Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Subordinated Debt Securities Indenture$' then 'Subordinated Debt Securities Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Senior Debt Securities Indenture$' then 'Senior Debt Securities Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Debt Securities Indenture$' then 'Debt Securities Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Debt Indenture$' then 'Debt Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Senior Secured Stub Notes Indenture$' then 'Senior Secured Stub Notes Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Junior Secured Notes Indenture$' then 'Junior Secured Notes Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^CLO Indenture$' then 'CLO Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Unsubordinated Indenture$' then 'Unsubordinated Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Unsecured Senior Notes Indenture$' then 'Unsecured Senior Notes Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Contingent Convertible Capital Securities Indenture$' then 'Contingent Convertible Capital Securities Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Credit Enhanced Bonds Indenture$' then 'Credit Enhanced Bonds Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Coupon Bonds Indenture$' then 'Coupon Bonds Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Subordinate Voting Share Purchase Warrant Indenture$' then 'Subordinate Voting Share Purchase Warrant Indenture'\r\n"
				+ "else CONTRACTNAMEALGO\r\n" + "end\r\n"
				+ "WHERE CONTRACTNAMEALGO RLIKE '^(INDENTURE|Senior Indenture|Subordinated Indenture|Senior Notes Indenture|MASTER INDENTURE|Trust Indenture|SENIOR SUBORDINATED INDENTURE|SUBORDINATED DEBT INDENTURE|Senior Debt Indenture|BASE INDENTURE|Junior Subordinated Indenture|Warrant Indenture|Guarantee Indenture|Senior Secured Indenture|convertible indenture|Special Warrant Indenture|Omnibus Indenture|Convertible Senior Notes Indenture|Contingent Capital Securities Indenture|Subordinate Indenture|Mortgage Indenture|PIK Notes Indenture|Omnibus Master Indenture|Subordinated Debt Securities Indenture|Senior Debt Securities Indenture|Debt Securities Indenture|Debt Indenture|Senior Secured Stub Notes Indenture|Junior Secured Notes Indenture|CLO Indenture|UNSUBORDINATED INDENTURE|Reference is made to that certain Indenture|Contingent Convertible Capital Securities Indenture|Credit Enhanced Bonds Indenture|Coupon Bonds Indenture|Subordinate Voting share Purchase Warrant Indenture)$';\r\n"
				+ "\r\n" + "\r\n" + "UPDATE METADATA_JSON_ALL\r\n" + "SET contractType='Indentures'\r\n"
				+ "WHERE CONTRACTNAMEALGO RLIKE '^Master Indenture$';\r\n" 

				+ "\r\n\r\nDROP TABLE IF EXISTS tmp_metadata_json_all_initial_cap;\r\n"
				+ "CREATE TABLE tmp_metadata_json_all_initial_cap\r\n"
				+ "select left(kid,25) kid, case when BINARY contractnamealgo =  BINARY UPPER(contractnamealgo) then 'AC' else 'lc' end aclc, left(contractnamealgo,255) contractnamealgo from metadata_json_all wh\r\n"
				+ "order by CONTRACTNAMEALGO, aclc desc;\r\n" + "\r\n" + "set @priorKn='abc';\r\n"
				+ "drop table if exists tmp_metadata_json_all_initial_cap2;\r\n"
				+ "create table tmp_metadata_json_all_initial_cap2\r\n"
				+ "select left(KNA,255) kna,left(KID,25) kid from (\r\n" + "SELECT kid,\r\n"
				+ "@priorKn:=CASE WHEN ACLC = 'lc' then contractnamealgo when @priorKn=contractnamealgo then @priorKn else contractnamealgo end kna,aclc,contractnamealgo contractnamealgo\r\n"
				+ "\r\n"
				+ "FROM tmp_metadata_json_all_initial_cap ) t1 where aclc='ac' and BINARY kna !=  BINARY (contractnamealgo) ;\r\n"
				+ "ALTER TABLE tmp_metadata_json_all_initial_cap2 ADD KEY(KID),ADD KEY(KNA);\r\n" + "\r\n"
				+ "update metadata_json_all t1 inner join tmp_metadata_json_all_initial_cap2 t2\r\n"
				+ " ON t1.kid = T2.kid\r\n" + "SET t1.CONTRACTNAMEALGO = T2.kna;\r\n" + "\r\n"
				+ "DROP TABLE IF EXISTS tmp_metadata_json_all_initial_cap;\r\n"
				+ "drop table if exists tmp_metadata_json_all_initial_cap2;\r\n" + "\r\n\r\n"
				+ "\r\nupdate metadata_json_all\r\n" + "set contractnamealgo =\r\n" + " trim(\r\n"
				+ "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n"
				+ "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n"
				+ "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n"
				+ "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n"
				+ "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n"
				+ "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n"
				+ "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n"
				+ "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n"
				+ "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n"
				+ "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n" + "regexp_replace(\r\n"
				+ "contractnamealgo,\r\n" + "'(?<=(^| |-)[A-Z]{1,100})a','a'),\r\n"
				+ "'(?<=(^| |-)[A-Z]{1,100})b','b'),\r\n" + "'(?<=(^| |-)[A-Z]{1,100})c','c'),\r\n"
				+ "'(?<=(^| |-)[A-Z]{1,100})d','d'),\r\n" + "'(?<=(^| |-)[A-Z]{1,100})e','e'),\r\n"
				+ "'(?<=(^| |-)[A-Z]{1,100})f','f'),\r\n" + "'(?<=(^| |-)[A-Z]{1,100})g','g'),\r\n"
				+ "'(?<=(^| |-)[A-Z]{1,100})h','h'),\r\n" + "'(?<=(^| |-)[A-Z]{1,100})i','i'),\r\n"
				+ "'(?<=(^| |-)[A-Z]{1,100})j','j'),\r\n" + "'(?<=(^| |-)[A-Z]{1,100})k','k'),\r\n"
				+ "'(?<=(^| |-)[A-Z]{1,100})l','l'),\r\n" + "'(?<=(^| |-)[A-Z]{1,100})m','m'),\r\n"
				+ "'(?<=(^| |-)[A-Z]{1,100})n','n'),\r\n" + "'(?<=(^| |-)[A-Z]{1,100})o','o'),\r\n"
				+ "'(?<=(^| |-)[A-Z]{1,100})p','p'),\r\n" + "'(?<=(^| |-)[A-Z]{1,100})q','q'),\r\n"
				+ "'(?<=(^| |-)[A-Z]{1,100})r','r'),\r\n" + "'(?<=(^| |-)[A-Z]{1,100})s','s'),\r\n"
				+ "'(?<=(^| |-)[A-Z]{1,100})t','t'),\r\n" + "'(?<=(^| |-)[A-Z]{1,100})u','u'),\r\n"
				+ "'(?<=(^| |-)[A-Z]{1,100})v','v'),\r\n" + "'(?<=(^| |-)[A-Z]{1,100})w','w'),\r\n"
				+ "'(?<=(^| |-)[A-Z]{1,100})x','x'),\r\n" + "'(?<=(^| |-)[A-Z]{1,100})y','y'),\r\n"
				+ "'(?<=(^| |-)[A-Z]{1,100})z','z'),\r\n" + "'(?<=(^| ))PSU(?= |$)','PSU'),\r\n"
				+ "'(?<=(^| ))RSU(?= |$)','RSU'),\r\n" + "' TO ',' to '),\r\n" + "' of ',' of '),\r\n"
				+ "'(?<=(^| ))Llp(?= |$)','LLP'),\r\n" + "' and ',' and '),\r\n" + "'^The? ',' '),\r\n"
				+ "'(^| )VRDP($| )','$1VRDP$2'),\r\n" + "'(^| )VIE($| )','$1VIE$2'),\r\n"
				+ "'(^| )NCIT($| )','$1NCIT$2'),\r\n" + "'(^| )GDSVF&H($| )','$1GDSVF&H$2'),\r\n"
				+ "'(^| )VMTP($| )','$1VMTP$2'),\r\n" + "'(^| )IV($| )','$1IV$2'),\r\n"
				+ "'(?<=(^| ))Llc(?= |$)','LLC')\r\n" + "\r\n" + ")\r\n" + "\r\n" + "WHERE \r\n"
				+ "(BINARY CONTRACTNAMEALGO =  BINARY UPPER(CONTRACTNAMEALGO) AND CONTRACTNAMEOK=1) OR \r\n"
				+ "(BINARY CONTRACTNAMEALGO =  BINARY UPPER(CONTRACTNAMEALGO)\r\n"
				+ "AND (CONTRACTNAMEALGO NOT RLIKE '^[A-Z]{3} |^[A-Z]+&' OR CONTRACTNAMEALGO RLIKE '^(JPM|KEY|LAW|NEW|LOC|SUB|NON|GAS|NOT|NUT|ONE|OUT|PRE|REG|TAX|WEB) '\r\n"
				+ "AND CONTRACTNAMEOK!=1 ) ) \r\n" + "OR\r\n" + "CONTRACTNAMEALGO rlike '^the? ';\r\n\r\n"
				+ "UPDATE displayname_final_okay\r\n" + "SET CONTRACTNAMEOK=-1 \r\n"
				+ "WHERE DISPLAYNAME RLIKE '^(reference)$' and contractType rlike 'indenture' and contractnameok=1;\r\n"
				+ "\r\n" + "\r\n" + "UPDATE displayname_final_okay\r\n" + "SET CONTRACTNAMEOK=1 \r\n"
				+ "WHERE DISPLAYNAME RLIKE '^(CLO Indenture|Contingent Capital Securities Indenture|Contingent Convertible Capital Securities Indenture|Convertible Indenture|Convertible Senior Notes Indenture|Coupon Bonds Indenture|Credit Enhanced Bonds Indenture|Guarantee Indenture|Junior Secured Notes Indenture|Junior Subordinated Indenture|Mortgage Indenture|Omnibus Indenture|Omnibus Master Indenture|PIK Notes Indenture|Senior Secured Stub Notes Indenture|Special Warrant Indenture|Subordinate Indenture|Subordinate Voting Share Purchase Warrant Indenture|Unsecured Senior Notes Indenture|Unsubordinated Indenture)$';\r\n"
				+ "\r\n" + "UPDATE displayname_final_okay\r\n" + "SET DISPLAYNAME = \r\n" + "case\r\n" + "\r\n"
				+ "when DISPLAYNAME rlike '^Indenture$' then 'Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Indenture$' then 'Senior Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinated Indenture$' then 'Subordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Notes Indenture$' then 'Senior Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Master Indenture$' then 'Master Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Trust Indenture$' then 'Trust Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Subordinated Indenture$' then 'Senior Subordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinated Debt Indenture$' then 'Subordinated Debt Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Debt Indenture$' then 'Senior Debt Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Base Indenture$' then 'Base Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Junior Subordinated Indenture$' then 'Junior Subordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Warrant Indenture$' then 'Warrant Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Guarantee Indenture$' then 'Guarantee Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Secured Indenture$' then 'Senior Secured Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Convertible Indenture$' then 'Convertible Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Special Warrant Indenture$' then 'Special Warrant Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Omnibus Indenture$' then 'Omnibus Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Convertible Senior Notes Indenture$' then 'Convertible Senior Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Contingent Capital Securities Indenture$' then 'Contingent Capital Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinate Indenture$' then 'Subordinate Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Mortgage Indenture$' then 'Mortgage Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^PIK Notes Indenture$' then 'PIK Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Omnibus Master Indenture$' then 'Omnibus Master Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinated Debt Securities Indenture$' then 'Subordinated Debt Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Debt Securities Indenture$' then 'Senior Debt Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Debt Securities Indenture$' then 'Debt Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Debt Indenture$' then 'Debt Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Secured Stub Notes Indenture$' then 'Senior Secured Stub Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Junior Secured Notes Indenture$' then 'Junior Secured Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^CLO Indenture$' then 'CLO Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Unsubordinated Indenture$' then 'Unsubordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Unsecured Senior Notes Indenture$' then 'Unsecured Senior Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Contingent Convertible Capital Securities Indenture$' then 'Contingent Convertible Capital Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Credit Enhanced Bonds Indenture$' then 'Credit Enhanced Bonds Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Coupon Bonds Indenture$' then 'Coupon Bonds Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinate Voting Share Purchase Warrant Indenture$' then 'Subordinate Voting Share Purchase Warrant Indenture'\r\n"
				+ "else DISPLAYNAME\r\n" + "end\r\n"
				+ "WHERE DISPLAYNAME RLIKE '^(INDENTURE|Senior Indenture|Subordinated Indenture|Senior Notes Indenture|MASTER INDENTURE|Trust Indenture|SENIOR SUBORDINATED INDENTURE|SUBORDINATED DEBT INDENTURE|Senior Debt Indenture|BASE INDENTURE|Junior Subordinated Indenture|Warrant Indenture|Guarantee Indenture|Senior Secured Indenture|convertible indenture|Special Warrant Indenture|Omnibus Indenture|Convertible Senior Notes Indenture|Contingent Capital Securities Indenture|Subordinate Indenture|Mortgage Indenture|PIK Notes Indenture|Omnibus Master Indenture|Subordinated Debt Securities Indenture|Senior Debt Securities Indenture|Debt Securities Indenture|Debt Indenture|Senior Secured Stub Notes Indenture|Junior Secured Notes Indenture|CLO Indenture|UNSUBORDINATED INDENTURE|Reference is made to that certain Indenture|Contingent Convertible Capital Securities Indenture|Credit Enhanced Bonds Indenture|Coupon Bonds Indenture|Subordinate Voting share Purchase Warrant Indenture)$';\r\n"
				+ "\r\n" + "\r\n" + "UPDATE displayname_final_okay\r\n" + "SET contractType='Indentures'\r\n"
				+ "WHERE DISPLAYNAME RLIKE '^Master Indenture$';\r\n" + "\r\n" + "UPDATE displayname_final\r\n"
				+ "SET DISPLAYNAME = \r\n" + "case\r\n" + "\r\n"
				+ "when DISPLAYNAME rlike '^Indenture$' then 'Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Indenture$' then 'Senior Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinated Indenture$' then 'Subordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Notes Indenture$' then 'Senior Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Master Indenture$' then 'Master Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Trust Indenture$' then 'Trust Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Subordinated Indenture$' then 'Senior Subordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinated Debt Indenture$' then 'Subordinated Debt Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Debt Indenture$' then 'Senior Debt Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Base Indenture$' then 'Base Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Junior Subordinated Indenture$' then 'Junior Subordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Warrant Indenture$' then 'Warrant Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Guarantee Indenture$' then 'Guarantee Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Secured Indenture$' then 'Senior Secured Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Convertible Indenture$' then 'Convertible Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Special Warrant Indenture$' then 'Special Warrant Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Omnibus Indenture$' then 'Omnibus Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Convertible Senior Notes Indenture$' then 'Convertible Senior Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Contingent Capital Securities Indenture$' then 'Contingent Capital Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinate Indenture$' then 'Subordinate Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Mortgage Indenture$' then 'Mortgage Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^PIK Notes Indenture$' then 'PIK Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Omnibus Master Indenture$' then 'Omnibus Master Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinated Debt Securities Indenture$' then 'Subordinated Debt Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Debt Securities Indenture$' then 'Senior Debt Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Debt Securities Indenture$' then 'Debt Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Debt Indenture$' then 'Debt Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Secured Stub Notes Indenture$' then 'Senior Secured Stub Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Junior Secured Notes Indenture$' then 'Junior Secured Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^CLO Indenture$' then 'CLO Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Unsubordinated Indenture$' then 'Unsubordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Unsecured Senior Notes Indenture$' then 'Unsecured Senior Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Contingent Convertible Capital Securities Indenture$' then 'Contingent Convertible Capital Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Credit Enhanced Bonds Indenture$' then 'Credit Enhanced Bonds Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Coupon Bonds Indenture$' then 'Coupon Bonds Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinate Voting Share Purchase Warrant Indenture$' then 'Subordinate Voting Share Purchase Warrant Indenture'\r\n"
				+ "else DISPLAYNAME\r\n" + "end\r\n"
				+ "WHERE DISPLAYNAME RLIKE '^(INDENTURE|Senior Indenture|Subordinated Indenture|Senior Notes Indenture|MASTER INDENTURE|Trust Indenture|SENIOR SUBORDINATED INDENTURE|SUBORDINATED DEBT INDENTURE|Senior Debt Indenture|BASE INDENTURE|Junior Subordinated Indenture|Warrant Indenture|Guarantee Indenture|Senior Secured Indenture|convertible indenture|Special Warrant Indenture|Omnibus Indenture|Convertible Senior Notes Indenture|Contingent Capital Securities Indenture|Subordinate Indenture|Mortgage Indenture|PIK Notes Indenture|Omnibus Master Indenture|Subordinated Debt Securities Indenture|Senior Debt Securities Indenture|Debt Securities Indenture|Debt Indenture|Senior Secured Stub Notes Indenture|Junior Secured Notes Indenture|CLO Indenture|UNSUBORDINATED INDENTURE|Reference is made to that certain Indenture|Contingent Convertible Capital Securities Indenture|Credit Enhanced Bonds Indenture|Coupon Bonds Indenture|Subordinate Voting share Purchase Warrant Indenture)$';\r\n"
				+ "\r\n" + "\r\n" + "UPDATE displayname_final\r\n" + "SET contractType='Indentures'\r\n"
				+ "WHERE DISPLAYNAME RLIKE '^Master Indenture$';\r\n" + "\r\n"
				+ "UPDATE DISPLAY_NAME_TABLE_dnu_href_all\r\n" + "SET DISPLAYNAME = \r\n" + "case\r\n" + "\r\n"
				+ "when DISPLAYNAME rlike '^Indenture$' then 'Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Indenture$' then 'Senior Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinated Indenture$' then 'Subordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Notes Indenture$' then 'Senior Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Master Indenture$' then 'Master Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Trust Indenture$' then 'Trust Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Subordinated Indenture$' then 'Senior Subordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinated Debt Indenture$' then 'Subordinated Debt Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Debt Indenture$' then 'Senior Debt Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Base Indenture$' then 'Base Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Junior Subordinated Indenture$' then 'Junior Subordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Warrant Indenture$' then 'Warrant Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Guarantee Indenture$' then 'Guarantee Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Secured Indenture$' then 'Senior Secured Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Convertible Indenture$' then 'Convertible Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Special Warrant Indenture$' then 'Special Warrant Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Omnibus Indenture$' then 'Omnibus Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Convertible Senior Notes Indenture$' then 'Convertible Senior Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Contingent Capital Securities Indenture$' then 'Contingent Capital Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinate Indenture$' then 'Subordinate Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Mortgage Indenture$' then 'Mortgage Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^PIK Notes Indenture$' then 'PIK Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Omnibus Master Indenture$' then 'Omnibus Master Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinated Debt Securities Indenture$' then 'Subordinated Debt Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Debt Securities Indenture$' then 'Senior Debt Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Debt Securities Indenture$' then 'Debt Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Debt Indenture$' then 'Debt Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Secured Stub Notes Indenture$' then 'Senior Secured Stub Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Junior Secured Notes Indenture$' then 'Junior Secured Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^CLO Indenture$' then 'CLO Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Unsubordinated Indenture$' then 'Unsubordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Unsecured Senior Notes Indenture$' then 'Unsecured Senior Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Contingent Convertible Capital Securities Indenture$' then 'Contingent Convertible Capital Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Credit Enhanced Bonds Indenture$' then 'Credit Enhanced Bonds Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Coupon Bonds Indenture$' then 'Coupon Bonds Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinate Voting Share Purchase Warrant Indenture$' then 'Subordinate Voting Share Purchase Warrant Indenture'\r\n"
				+ "else DISPLAYNAME\r\n" + "end\r\n"
				+ "WHERE DISPLAYNAME RLIKE '^(INDENTURE|Senior Indenture|Subordinated Indenture|Senior Notes Indenture|MASTER INDENTURE|Trust Indenture|SENIOR SUBORDINATED INDENTURE|SUBORDINATED DEBT INDENTURE|Senior Debt Indenture|BASE INDENTURE|Junior Subordinated Indenture|Warrant Indenture|Guarantee Indenture|Senior Secured Indenture|convertible indenture|Special Warrant Indenture|Omnibus Indenture|Convertible Senior Notes Indenture|Contingent Capital Securities Indenture|Subordinate Indenture|Mortgage Indenture|PIK Notes Indenture|Omnibus Master Indenture|Subordinated Debt Securities Indenture|Senior Debt Securities Indenture|Debt Securities Indenture|Debt Indenture|Senior Secured Stub Notes Indenture|Junior Secured Notes Indenture|CLO Indenture|UNSUBORDINATED INDENTURE|Reference is made to that certain Indenture|Contingent Convertible Capital Securities Indenture|Credit Enhanced Bonds Indenture|Coupon Bonds Indenture|Subordinate Voting share Purchase Warrant Indenture)$';\r\n"
				+ "\r\n" + "\r\n" + "UPDATE DISPLAY_NAME_TABLE_dnu_href_all\r\n" + "SET contractType='Indentures'\r\n"
				+ "WHERE DISPLAYNAME RLIKE '^Master Indenture$';\r\n" + "\r\n" + "\r\n" + "\r\n"
				+ "UPDATE DISPLAY_NAME_TABLE_dnu_href_2\r\n" + "SET DISPLAYNAME = \r\n" + "case\r\n" + "\r\n"
				+ "when DISPLAYNAME rlike '^Indenture$' then 'Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Indenture$' then 'Senior Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinated Indenture$' then 'Subordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Notes Indenture$' then 'Senior Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Master Indenture$' then 'Master Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Trust Indenture$' then 'Trust Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Subordinated Indenture$' then 'Senior Subordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinated Debt Indenture$' then 'Subordinated Debt Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Debt Indenture$' then 'Senior Debt Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Base Indenture$' then 'Base Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Junior Subordinated Indenture$' then 'Junior Subordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Warrant Indenture$' then 'Warrant Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Guarantee Indenture$' then 'Guarantee Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Secured Indenture$' then 'Senior Secured Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Convertible Indenture$' then 'Convertible Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Special Warrant Indenture$' then 'Special Warrant Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Omnibus Indenture$' then 'Omnibus Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Convertible Senior Notes Indenture$' then 'Convertible Senior Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Contingent Capital Securities Indenture$' then 'Contingent Capital Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinate Indenture$' then 'Subordinate Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Mortgage Indenture$' then 'Mortgage Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^PIK Notes Indenture$' then 'PIK Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Omnibus Master Indenture$' then 'Omnibus Master Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinated Debt Securities Indenture$' then 'Subordinated Debt Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Debt Securities Indenture$' then 'Senior Debt Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Debt Securities Indenture$' then 'Debt Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Debt Indenture$' then 'Debt Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Secured Stub Notes Indenture$' then 'Senior Secured Stub Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Junior Secured Notes Indenture$' then 'Junior Secured Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^CLO Indenture$' then 'CLO Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Unsubordinated Indenture$' then 'Unsubordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Unsecured Senior Notes Indenture$' then 'Unsecured Senior Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Contingent Convertible Capital Securities Indenture$' then 'Contingent Convertible Capital Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Credit Enhanced Bonds Indenture$' then 'Credit Enhanced Bonds Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Coupon Bonds Indenture$' then 'Coupon Bonds Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinate Voting Share Purchase Warrant Indenture$' then 'Subordinate Voting Share Purchase Warrant Indenture'\r\n"
				+ "else DISPLAYNAME\r\n" + "end\r\n"
				+ "WHERE DISPLAYNAME RLIKE '^(INDENTURE|Senior Indenture|Subordinated Indenture|Senior Notes Indenture|MASTER INDENTURE|Trust Indenture|SENIOR SUBORDINATED INDENTURE|SUBORDINATED DEBT INDENTURE|Senior Debt Indenture|BASE INDENTURE|Junior Subordinated Indenture|Warrant Indenture|Guarantee Indenture|Senior Secured Indenture|convertible indenture|Special Warrant Indenture|Omnibus Indenture|Convertible Senior Notes Indenture|Contingent Capital Securities Indenture|Subordinate Indenture|Mortgage Indenture|PIK Notes Indenture|Omnibus Master Indenture|Subordinated Debt Securities Indenture|Senior Debt Securities Indenture|Debt Securities Indenture|Debt Indenture|Senior Secured Stub Notes Indenture|Junior Secured Notes Indenture|CLO Indenture|UNSUBORDINATED INDENTURE|Reference is made to that certain Indenture|Contingent Convertible Capital Securities Indenture|Credit Enhanced Bonds Indenture|Coupon Bonds Indenture|Subordinate Voting share Purchase Warrant Indenture)$';\r\n"
				+ "\r\n" + "\r\n" + "UPDATE DISPLAY_NAME_TABLE_dnu_href_2\r\n" + "SET contractType='Indentures'\r\n"
				+ "WHERE DISPLAYNAME RLIKE '^Master Indenture$';\r\n" + "\r\n"
				+ "UPDATE DISPLAY_NAME_TABLE_dnu_href_3\r\n" + "SET DISPLAYNAME = \r\n" + "case\r\n" + "\r\n"
				+ "when DISPLAYNAME rlike '^Indenture$' then 'Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Indenture$' then 'Senior Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinated Indenture$' then 'Subordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Notes Indenture$' then 'Senior Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Master Indenture$' then 'Master Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Trust Indenture$' then 'Trust Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Subordinated Indenture$' then 'Senior Subordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinated Debt Indenture$' then 'Subordinated Debt Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Debt Indenture$' then 'Senior Debt Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Base Indenture$' then 'Base Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Junior Subordinated Indenture$' then 'Junior Subordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Warrant Indenture$' then 'Warrant Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Guarantee Indenture$' then 'Guarantee Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Secured Indenture$' then 'Senior Secured Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Convertible Indenture$' then 'Convertible Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Special Warrant Indenture$' then 'Special Warrant Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Omnibus Indenture$' then 'Omnibus Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Convertible Senior Notes Indenture$' then 'Convertible Senior Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Contingent Capital Securities Indenture$' then 'Contingent Capital Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinate Indenture$' then 'Subordinate Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Mortgage Indenture$' then 'Mortgage Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^PIK Notes Indenture$' then 'PIK Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Omnibus Master Indenture$' then 'Omnibus Master Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinated Debt Securities Indenture$' then 'Subordinated Debt Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Debt Securities Indenture$' then 'Senior Debt Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Debt Securities Indenture$' then 'Debt Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Debt Indenture$' then 'Debt Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Senior Secured Stub Notes Indenture$' then 'Senior Secured Stub Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Junior Secured Notes Indenture$' then 'Junior Secured Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^CLO Indenture$' then 'CLO Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Unsubordinated Indenture$' then 'Unsubordinated Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Unsecured Senior Notes Indenture$' then 'Unsecured Senior Notes Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Contingent Convertible Capital Securities Indenture$' then 'Contingent Convertible Capital Securities Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Credit Enhanced Bonds Indenture$' then 'Credit Enhanced Bonds Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Coupon Bonds Indenture$' then 'Coupon Bonds Indenture'\r\n"
				+ "when DISPLAYNAME rlike '^Subordinate Voting Share Purchase Warrant Indenture$' then 'Subordinate Voting Share Purchase Warrant Indenture'\r\n"
				+ "else DISPLAYNAME\r\n" + "end\r\n"
				+ "WHERE DISPLAYNAME RLIKE '^(INDENTURE|Senior Indenture|Subordinated Indenture|Senior Notes Indenture|MASTER INDENTURE|Trust Indenture|SENIOR SUBORDINATED INDENTURE|SUBORDINATED DEBT INDENTURE|Senior Debt Indenture|BASE INDENTURE|Junior Subordinated Indenture|Warrant Indenture|Guarantee Indenture|Senior Secured Indenture|convertible indenture|Special Warrant Indenture|Omnibus Indenture|Convertible Senior Notes Indenture|Contingent Capital Securities Indenture|Subordinate Indenture|Mortgage Indenture|PIK Notes Indenture|Omnibus Master Indenture|Subordinated Debt Securities Indenture|Senior Debt Securities Indenture|Debt Securities Indenture|Debt Indenture|Senior Secured Stub Notes Indenture|Junior Secured Notes Indenture|CLO Indenture|UNSUBORDINATED INDENTURE|Reference is made to that certain Indenture|Contingent Convertible Capital Securities Indenture|Credit Enhanced Bonds Indenture|Coupon Bonds Indenture|Subordinate Voting share Purchase Warrant Indenture)$';\r\n"
				+ "\r\n" + "UPDATE DISPLAY_NAME_TABLE_dnu_href_3\r\n" + "SET contractType='Indentures'\r\n"
				+ "WHERE DISPLAYNAME RLIKE '^Master Indenture$';\r\n" + "\r\n";

		MysqlConnUtils.executeQuery(query_update);

		int startdate = Integer.parseInt(yr + "0101");
		int enddate = Integer.parseInt(yr + "0331");
		int sDateLess2 = Integer.parseInt((yr - 2) + "0101");
		int eDatePlus2 = Integer.parseInt((yr + 2) + "0331");

//		System.out.println("qtr=" + qtr);
		if (qtr == 2) {
			startdate = Integer.parseInt(yr + "0401");
			enddate = Integer.parseInt(yr + "0630");
			sDateLess2 = Integer.parseInt((yr - 2) + "0401");
			eDatePlus2 = Integer.parseInt((yr + 2) + "0630");
		}
		if (qtr == 3) {
			startdate = Integer.parseInt(yr + "0701");
			enddate = Integer.parseInt(yr + "0930");
			sDateLess2 = Integer.parseInt((yr - 2) + "0701");
			eDatePlus2 = Integer.parseInt((yr + 2) + "0930");
		}
		if (qtr == 4) {
			startdate = Integer.parseInt(yr + "1001");
			enddate = Integer.parseInt(yr + "1231");
			sDateLess2 = Integer.parseInt((yr - 2) + "1001");
			eDatePlus2 = Integer.parseInt((yr + 2) + "1231");
		}

		String filename = "";
		StringBuilder sb = new StringBuilder();
		StringBuilder sb_json_all = new StringBuilder();

		String query = "DROP TABLE IF EXISTS tmp_" + yr + "_" + qtr + "_metadata_to_use_for_displaynames;\r\n"
				+ "CREATE TABLE `tmp_" + yr + "_" + qtr + "_metadata_to_use_for_displaynames` (\r\n"
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
				+ " PARTITION p29 VALUES LESS THAN MAXVALUE ENGINE = InnoDB) */;\r\n" +

				"insert ignore into tmp_" + yr + "_" + qtr + "_metadata_to_use_for_displaynames\r\n"
				+ " select t1.* from metadata t1 inner join displayname_final t2\r\n" + " on t1.kid=t2.kid\r\n"
				+ " where t1.fdate>='" + sDateLess2 + "' and t1.fdate<='" + eDatePlus2 + "'; \r\n" + " \r\n"
				+ "set @cik=0; set @cnt=0; set @displayname ='abc';\r\n" + "DROP TABLE IF EXISTS TMP_" + yr + "_" + qtr
				+ "_Correct_cnt;\r\n" + "create table TMP_" + yr + "_" + qtr + "_Correct_cnt\r\n"
				+ "select ck,displayname from (\r\n" + " select score, @cnt:=case \r\n"
				+ " when @displayname!=trim(displayname) then 1 \r\n"
				+ " when @cik=t1.cik then @cnt when @cik!=t1.cik and @displayname=trim(displayname) then @cnt+1 else @cnt end ck,@cik:=t1.cik cik2,@displayname:=trim(displayname) dn\r\n"
				+ " , t1.* from displayname_final t1 inner join tmp_" + yr + "_" + qtr
				+ "_metadata_to_use_for_displaynames t2 on t1.kid=t2.kid\r\n" + " where t1.fdate between '" + sDateLess2
				+ "' and '" + eDatePlus2
				+ "' and length(t1.displayName)>3 order by trim(displayname),t1.cik ) t1 where (ck>1 and score>4) or (score>2 and ck>2) or ck>5  group by dn;\r\n"
				+ "alter table TMP_" + yr + "_" + qtr + "_Correct_cnt add key(displayname);\r\n" + "\r\n"
				+ "DROP TABLE IF EXISTS tmp_" + yr + "_" + qtr + "_displayname_cnts_join_metadata;\r\n"
				+ "CREATE TABLE tmp_" + yr + "_" + qtr + "_displayname_cnts_join_metadata\r\n"
				+ "select count(*) cnt2,score,displayname,contracttype,cnt from DISPLAY_NAME_TABLE_dnu_href_3 t1 inner join tmp_"
				+ yr + "_" + qtr + "_metadata_to_use_for_displaynames t2 \r\n"
				+ "on t1.kid = t2.kId where length(t1.displayname)>3 \r\n" + "group by displayname;\r\n"
				+ "ALTER TABLE tmp_" + yr + "_" + qtr
				+ "_displayname_cnts_join_metadata change displayname displayname varchar(255), ADD KEY (DISPLAYNAME), change cnt cnt int(10), change cnt2 cnt2 int(10);\r\n"
				+ "\r\n" + " update tmp_" + yr + "_" + qtr + "_displayname_cnts_join_metadata t1\r\n"
				+ " inner join TMP_" + yr + "_" + qtr + "_Correct_cnt t2\r\n" + " set t1.cnt=t2.ck\r\n"
				+ " where t1.displayname=t2.displayname;\r\n" + "\r\n" +

				"DROP TABLE IF EXISTS TMP_" + yr + "_" + qtr + "_CIK_CK;\r\n" + "CREATE TABLE TMP_" + yr + "_" + qtr
				+ "_CIK_CK\r\n" + "select cik,t2.displayName,contractnameok from TMP_" + yr + "_" + qtr
				+ "_metadata_to_use_for_displaynames t1 right join displayname_final_okay t2\r\n"
				+ "on t1.contractnamealgo=t2.displayname \r\n" + "where contractnameok=-1\r\n"
				+ "and t2.contractType not rlike 'other agreement' \r\n" + "and  fdate>='" + sDateLess2 + "' and fdate<='"
				+ eDatePlus2 + "';" + "\r\n" + "\r\n" + "set @cn='abc'; set @cik=123; set @cnt=0;\r\n"

				+ "DROP TABLE IF EXISTS TMP_" + yr + "_" + qtr + "_CNT_CIK2;\r\n" + "CREATE TABLE TMP_" + yr + "_" + qtr
				+ "_CNT_CIK2\r\n" + "select dn,max(cnt) cnt from (\r\n"
				+ "select case when binary left(displayname,1) = binary lower(left(displayname,1)) then 0 else 1 end ck,\r\n"
				+ "@cnt:=case when @cik!=cik and @cn=displayname then 1+@cnt else 0 end cnt,\r\n"
				+ "@cik:=cik,@cn:=displayName dn from TMP_" + yr + "_" + qtr
				+ "_CIK_CK order by displayName ) t1 where cnt>1 and ck=1\r\n"
				+ "and dn not rlike '^([a-z]{1,2}|[^a-z]|) |[^a-z\\-,& ]|^Agreement$' and length(dn)<55 \r\n"
				+ "group by dn;\r\n" + "ALTER TABLE TMP_" + yr + "_" + qtr
				+ "_CNT_CIK2 ADD KEY(DN), change cnt cnt int(10);\r\n" + "\r\n"

				+ " update tmp_" + yr + "_" + qtr + "_displayname_cnts_join_metadata t1\r\n" + " inner join TMP_" + yr
				+ "_" + qtr + "_CNT_CIK2 t2\r\n" + " set t1.cnt=t2.cnt\r\n"
				+ " where t1.displayname=t2.dn and t1.cnt<t2.cnt;\r\n"

				+ "DROP TABLE IF EXISTS " + yr + "_" + qtr + "_displayname_final_okay;\r\n" + "CREATE TABLE " + yr + "_"
				+ qtr + "_displayname_final_okay\r\n" + "select kid,href_description,\r\n" + "case\r\n"
				+ "when (cnt>5 or (cnt2>15 and cnt>2) or (score>2 and cnt>2) or (score>3 and cnt>1 ) or (t1.contractType rlike 'token|future equit') OR regexp_replace(t2.contractType,'s$','' )=trim(t2.displayname) \r\n"
				+ "or (cnt2>10 and length(t2.displayname)<55) and cnt>1 )\r\n" + "and t1.displayname not rlike\r\n"
				+ "'^( any |class|contract|000,|additional|agreement|attorney)$|original|execution|million|billion|thousand|signature|page|cambridge|mplx|csg|mccann|pimco|thereto|company of |attached| hereto|^Number t|joing|(^| )(cure |DWS  )|codeofeth'\r\n"
				+ "then 1 else -1 end contractNameOk, t1.displayname,parent,t1.contracttype,t2.modify\r\n" + "from tmp_"
				+ yr + "_" + qtr + "_displayname_cnts_join_metadata T1 \r\n"
				+ "inner join displayname_final t2 on T1.displayname = t2.displayName \r\n"
				+ "where (t1.displayname not rlike '^other') ;\r\n" + "ALTER TABLE " + yr + "_" + qtr
				+ "_displayname_final_okay ADD PRIMARY KEY (KID);\r\n" + "\r\n"
				+ "/*this is what is exported to csv file.*/\r\n" + "\r\n" + "DROP TABLE IF EXISTS TMP_" + yr + "_"
				+ qtr + "_meta_export_table;\r\n" + "create table TMP_" + yr + "_" + qtr + "_meta_export_table\r\n"
				+ "select\r\n"
				+ "		LEFT(regexp_replace(left(acc,10),'^[0]+',''),10) cik,companyName,formType,fdate,t1.kid,'5',contracttype contract_type,fsize,acc,acc_link_filename,edgarLink,contractLongName,type,legalEntitiesInOpeningParagraph\r\n"
				+ "    ,numberOfLegalEntities,\r\n"
				+ "    case when length(href_description)>3 and length(openingParagraph)>3 and href_description != openingParagraph then regexp_replace(concat(trim(href_description),'zxz',trim(openingParagraph)),'[\r\n]+','') \r\n"
				+ "    when length(href_description)>3 then href_description when length(openingParagraph)>3 then openingParagraph else href_description end openingParagraph\r\n"
				+ "    ,openParaContractName,displayname contractNameAlgo,legalEntities,score,'solrCore','pattern',governingLaw,parent parent_group,contracttype contract_type2\r\n"
				+ "    ,contractNameOk,modify\r\n" + "\r\n" + "FROM " + yr + "_" + qtr
				+ "_displayname_final_okay T1 \r\n" + "INNER JOIN tmp_" + yr + "_" + qtr
				+ "_metadata_to_use_for_displaynames T2\r\n" + "ON T1.KID=T2.KID WHERE FDATE >= '" + startdate
				+ "' AND FDATE<='" + enddate + "' ;\r\n" + "\r\n" + "ALTER TABLE TMP_" + yr + "_" + qtr
				+ "_meta_export_table ADD KEY(CIK), add key(companyname);\r\n" + "\r\n" + "\r\n"
				+ "DROP TABLE IF EXISTS metadata_json_" + yr + "Q" + qtr // + "_fSize_" + minSize + "_to_" + maxSize
				+ ";\r\n" + "create table metadata_json_" + yr + "Q" + qtr// + "_fSize_" + minSize + "_to_" + maxSize

				+ " (\r\n" + "  `cik` VARCHAR(25) CHARACTER SET latin1,\r\n"
				+ "  `companyName` VARCHAR(255) CHARACTER SET latin1,\r\n"
				+ "  `formType` VARCHAR(25) CHARACTER SET latin1,\r\n" + "  `fdate` VARCHAR(25),\r\n"
				+ "  `kid` VARCHAR(25) CHARACTER SET latin1,\r\n" + "  `5` VARCHAR(25),\r\n"
				+ "  `contractType` VARCHAR(255),\r\n" + "  `fsize` INT(10),\r\n"
				+ "  `acc` VARCHAR(255) CHARACTER SET latin1,\r\n"
				+ "  `acc_link_filename` VARCHAR(255) CHARACTER SET latin1,\r\n"
				+ "  `edgarLink` VARCHAR(255) CHARACTER SET latin1,\r\n"
				+ "  `contractLongName` VARCHAR(255) CHARACTER SET latin1,\r\n"
				+ "  `type` VARCHAR(25) CHARACTER SET latin1,\r\n"
				+ "  `legalEntitiesInOpeningParagraph` VARCHAR(255) CHARACTER SET latin1,\r\n"
				+ "  `numberOfLegalEntities` VARCHAR(255),\r\n" + "  `openingParagraph` TEXT,\r\n"
				+ "  `openParaContractName` VARCHAR(255) CHARACTER SET latin1,\r\n"
				+ "  `contractNameAlgo` VARCHAR(255),\r\n" + "  `legalEntities` VARCHAR(255) CHARACTER SET latin1,\r\n"
				+ "  `score` VARCHAR(255),\r\n" + "  `solrCore` VARCHAR(20),\r\n" + "  `pattern` VARCHAR(255),\r\n"
				+ "  `gLaw` VARCHAR(255) CHARACTER SET latin1,\r\n" + "  `parent` VARCHAR(255),\r\n"
				+ "  `kty2` VARCHAR(255),\r\n" + "  `ticker` VARCHAR(255),\r\n" + "  `sector` VARCHAR(255),\r\n"
				+ "  `industry` VARCHAR(255),\r\n" + "  `contractnameok` VARCHAR(25),\r\n"
				+ "  `modify` VARCHAR(25)\r\n"
				+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;\r\n" + "\r\n"
				+ "INSERT IGNORE INTO metadata_json_" + yr + "Q" + qtr + "\r\n" +

				"\r\n" + "SELECT   \r\n" + "TRIM(regexp_replace(t1.cik,'[\r\n]+','')) cik,\r\n"
				+ "TRIM(regexp_replace(companyName,'[\r\n]+','')) companyName,\r\n"
				+ "TRIM(regexp_replace(formType,'[\r\n]+','')) formType,\r\n"
				+ "TRIM(regexp_replace(fdate,'[\r\n]+','')) fdate,\r\n"
				+ "TRIM(regexp_replace(kid,'[\r\n]+','')) kid,\r\n" + "TRIM(regexp_replace('5','[\r\n]+','')) '5',\r\n"
				+ "TRIM(regexp_replace( contract_type,'[\r\n]+','')) contractType,\r\n"
				+ "TRIM(regexp_replace(fsize,'[\r\n]+','')) fsize,\r\n"
				+ "TRIM(regexp_replace(acc,'[\r\n]+','')) acc,\r\n"
				+ "TRIM(regexp_replace(acc_link_filename,'[\r\n]+','')) acc_link_filename,\r\n"
				+ "TRIM(regexp_replace(edgarLink,'[\r\n]+','')) edgarLink,\r\n"
				+ "TRIM(regexp_replace(contractLongName,'[\r\n]+','')) contractLongName,\r\n"
				+ "TRIM(regexp_replace(type,'[\r\n]+','')) type,\r\n"
				+ "TRIM(regexp_replace(legalEntitiesInOpeningParagraph,'[\r\n]+','')) legalEntitiesInOpeningParagraph,\r\n"
				+ "TRIM(regexp_replace(numberOfLegalEntities,'[\r\n]+','')) numberOfLegalEntities,\r\n"
				+ "TRIM(regexp_replace(openingParagraph,'[\r\n]+','')) openingParagraph,\r\n"
				+ "TRIM(regexp_replace(openParaContractName,'[\r\n]+','')) openParaContractName,\r\n"
				+ "TRIM(regexp_replace( contractNameAlgo,'[\r\n]+','')) contractNameAlgo, \r\n"
				+ "TRIM(regexp_replace(legalEntities,'[\r\n]+','')) legalEntities,\r\n"
				+ "TRIM(regexp_replace(score,'[\r\n]+','')) score,\r\n"
				+ "TRIM(regexp_replace('solrCore','[\r\n]+','')) solrCore,\r\n"
				+ "TRIM(regexp_replace('pattern','[\r\n]+','')) pattern,\r\n"
				+ "case when T1.governingLaw rlike 'not found' or length(T1.governingLaw)<2 then '-1' else TRIM(regexp_replace(governingLaw,'[\r\n]+','')) end gLaw,\r\n"
				+ "TRIM(regexp_replace( parent_group,'[\r\n]+','')) parent,\r\n"
				+ "TRIM(regexp_replace( contract_type2,'[\r\n]+','')) kty2, \r\n"
				+ "case when ticker is null then '-1' else TRIM(regexp_replace(ticker,'[\r\n"
				+ "]+','')) end ticker,\r\n"
				+ "case when sector is null then '-1' else TRIM(regexp_replace(sector,'[\r\n"
				+ "]+','')) end sector,\r\n"
				+ "case when industry is null then '-1' else TRIM(regexp_replace(industry,'[\r\n"
				+ "]+','')) end industry,\r\n" + "TRIM(regexp_replace(contractNameOk,'[\r\n]+','')) contractnameok,\r\n"
				+ "case when length(modify)<1 then '-1' else TRIM(regexp_replace(modify,'[\r\n]+','')) end modify  \r\n"
				+ "FROM TMP_" + yr + "_" + qtr
				+ "_meta_export_table T1 left JOIN company_name_ticker_industry_sector_cik t2\r\n"
				+ "    on t1.cik=t2.cik WHERE\r\n" + "t1.type!='425' AND t1.type!='DEF 14A' AND t1.type!='DEF 14C'\r\n"
				+ "AND t1.type!='10-K' AND t1.type!='PRE 14A' AND t1.type!='DEFA14A' AND t1.type!='497' AND t1.type!='10-Q' AND t1.type!='S-4/A' AND t1.type!='DEFM14A' AND t1.type!='S-4' AND t1.type!='SC 13G/A' \r\n"
				+ "AND t1.type!='PREM14A' AND t1.type!='SC 14D9' AND t1.type!='497K' AND t1.type!='485BXT' AND t1.type!='PRE 14C' AND t1.type!='PRER14A' AND t1.type!='SC 13D' AND t1.type!='40-17G' AND t1.type!='F-4/A' \r\n"
				+ "AND t1.type!='SC 13G' AND t1.type!='SC 13D/A' AND t1.type!='424B4' AND t1.type!='F-4' AND t1.type!='DEFR14A' AND t1.type!='10-Q/A' AND t1.type!='DEFM14C' AND t1.type!='PREM14C' AND t1.type!='10-K/A'\r\n"
				+ "AND t1.type!='N-2/A' AND t1.type!='N-1A/A' AND t1.type!='253G3' AND t1.type!='253G2' AND t1.type!='40-APP' AND t1.type!='40-17G/A' AND t1.type!='N-14' AND t1.type!='S-1/A' AND t1.type!='SC 14F1' AND t1.type!='1-SA'\r\n"
				+ "AND t1.type!='F-1/A' AND t1.type!='253G1' AND t1.type!='DEFC14A' AND t1.type!='PREC14A' AND t1.type!='F-1' AND t1.type!='N-14 8C/A' AND t1.type!='SF-3/A' AND t1.type!='SF-3' AND t1.type!='497VPI'\r\n"
				+ "AND t1.type!='N-2MEF' AND t1.type!='497J' AND t1.type!='20-F' AND t1.type!='S-3' AND t1.type!='SC 13E3' AND t1.type!='40-F' AND t1.type!='40-APP/A' AND t1.type!='N-6' AND t1.type!='DFAN14A' AND t1.type!='N-2' \r\n"
				+ "AND t1.type!='DEFR14C' AND t1.type!='SC 14D9/A' AND t1.type!='N-14/A' AND t1.type!='487' AND t1.type!='N-Q' AND t1.type!='N-Q/A' AND t1.type!='T-3' AND t1.type!='T-3/A' AND t1.type!='486BPOS' AND t1.type!='SC TO-I' AND t1.type!='NSAR-U'\r\n"
				+ "AND t1.type!='SC 13E3/A' AND t1.type!='SC TO-C'group by kid;\r\n" + "\r\n"

				+ "UPDATE metadata_json_" + yr + "Q" + qtr + "\r\n" + "set contracttype =\r\n"
				+ "case when contracttype='Indentures' and parent = 'Ancillary Transaction Docs' and openingparagraph rlike 'Officer.{1,4} Certificate' then 'Officer Certificates'\r\n"
				+ "when contracttype='Description of Securities' and openingparagraph rlike 'note|bond|debentur|%' then 'Description of Notes'\r\n"
				+ "when contracttype='Letter of Agreements' then 'Letter Agreements'\r\n"
				+ "when contracttype rlike 'other notices' and contractnamealgo rlike 'notice' and CONTRACTNAMEALGO rlike ' grant' then 'Notices of Grant'\r\n"
				+ "when contracttype rlike 'other notices' and contractnamealgo rlike 'change of trustee' then 'Notices of Change of Trustee'\r\n"
				+ "when contracttype rlike 'other notices' and contractnamealgo rlike 'settlement' then 'Notices of Settlement'\r\n"
				+ "when contracttype rlike 'other policies' and contractnamealgo rlike 'Whistleblower Policy' then 'Whistleblower Policies'\r\n"
				+ "when contracttype rlike 'other policies' and contractnamealgo rlike 'Foreign Corrupt Practice Act|FCPA' then 'Foreign Corrupt Practice Act Policies'\r\n"
				+ "when contracttype rlike 'other policies' and contractnamealgo rlike 'Document Retention' then 'Document Retention Policies'\r\n"
				+ "when contracttype rlike '^indenture$' and parent not rlike 'financial contract' then 'Other'\r\n"
				+ "else contracttype end;\r\n" + "\r\n" + "UPDATE metadata_json_" + yr + "Q" + qtr + "\r\n"
				+ "set contractnamealgo =\r\n"
				+ "case when contractnamealgo='Indenture' and parent = 'Ancillary Transaction Docs' and  contracttype='Officer Certificates' then 'Officer Certificates'\r\n"
				+ "else contractnamealgo end;\r\n" + "\r\n" + "UPDATE metadata_json_" + yr + "Q" + qtr + "\r\n"
				+ "set parent =\r\n" + "case \r\n"
				+ "when parent = 'Letters & Offers' then 'Ancillary Transaction Docs'\r\n"
				+ "when parent = 'HR - Letters & Offers'  then 'Ancillary Transaction Docs'\r\n"
//				+ "when parent = 'Policies'  then 'Formation'\r\n"
				+ "when parent = 'Plans - Funds' then 'Financial Contracts'\r\n"
				+ "when parent='HR Plans' and contracttype = 'Retirement Agreements' then 'HR Contracts'\r\n"
				+ "else parent end;\r\n" +

				"UPDATE METADATA_JSON_" + yr + "Q" + qtr + "\r\n" + "SET CONTRACTNAMEOK=-1 \r\n"
				+ "WHERE CONTRACTNAMEALGO RLIKE '^(reference)$' and contractType rlike 'indenture' and contractnameok=1;\r\n"
				+ "\r\n" + "UPDATE METADATA_JSON_" + yr + "Q" + qtr + "\r\n" + "SET CONTRACTNAMEOK=1 \r\n"
				+ "WHERE CONTRACTNAMEALGO RLIKE '^(CLO Indenture|Contingent Capital Securities Indenture|Contingent Convertible Capital Securities Indenture|Convertible Indenture|Convertible Senior Notes Indenture|Coupon Bonds Indenture|Credit Enhanced Bonds Indenture|Guarantee Indenture|Junior Secured Notes Indenture|Junior Subordinated Indenture|Mortgage Indenture|Omnibus Indenture|Omnibus Master Indenture|PIK Notes Indenture|Senior Secured Stub Notes Indenture|Special Warrant Indenture|Subordinate Indenture|Subordinate Voting Share Purchase Warrant Indenture|Unsecured Senior Notes Indenture|Unsubordinated Indenture)$';\r\n"
				+ "\r\n" + "UPDATE METADATA_JSON_" + yr + "Q" + qtr + "\r\n" + "SET CONTRACTNAMEALGO = \r\n"
				+ "case\r\n" + "\r\n" + "when CONTRACTNAMEALGO rlike '^Indenture$' then 'Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Senior Indenture$' then 'Senior Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Subordinated Indenture$' then 'Subordinated Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Senior Notes Indenture$' then 'Senior Notes Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Master Indenture$' then 'Master Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Trust Indenture$' then 'Trust Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Senior Subordinated Indenture$' then 'Senior Subordinated Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Subordinated Debt Indenture$' then 'Subordinated Debt Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Senior Debt Indenture$' then 'Senior Debt Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Base Indenture$' then 'Base Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Junior Subordinated Indenture$' then 'Junior Subordinated Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Warrant Indenture$' then 'Warrant Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Guarantee Indenture$' then 'Guarantee Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Senior Secured Indenture$' then 'Senior Secured Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Convertible Indenture$' then 'Convertible Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Special Warrant Indenture$' then 'Special Warrant Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Omnibus Indenture$' then 'Omnibus Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Convertible Senior Notes Indenture$' then 'Convertible Senior Notes Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Contingent Capital Securities Indenture$' then 'Contingent Capital Securities Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Subordinate Indenture$' then 'Subordinate Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Mortgage Indenture$' then 'Mortgage Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^PIK Notes Indenture$' then 'PIK Notes Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Omnibus Master Indenture$' then 'Omnibus Master Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Subordinated Debt Securities Indenture$' then 'Subordinated Debt Securities Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Senior Debt Securities Indenture$' then 'Senior Debt Securities Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Debt Securities Indenture$' then 'Debt Securities Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Debt Indenture$' then 'Debt Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Senior Secured Stub Notes Indenture$' then 'Senior Secured Stub Notes Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Junior Secured Notes Indenture$' then 'Junior Secured Notes Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^CLO Indenture$' then 'CLO Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Unsubordinated Indenture$' then 'Unsubordinated Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Unsecured Senior Notes Indenture$' then 'Unsecured Senior Notes Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Contingent Convertible Capital Securities Indenture$' then 'Contingent Convertible Capital Securities Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Credit Enhanced Bonds Indenture$' then 'Credit Enhanced Bonds Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Coupon Bonds Indenture$' then 'Coupon Bonds Indenture'\r\n"
				+ "when CONTRACTNAMEALGO rlike '^Subordinate Voting Share Purchase Warrant Indenture$' then 'Subordinate Voting Share Purchase Warrant Indenture'\r\n"
				+ "else CONTRACTNAMEALGO\r\n" + "end\r\n"
				+ "WHERE CONTRACTNAMEALGO RLIKE '^(INDENTURE|Senior Indenture|Subordinated Indenture|Senior Notes Indenture|MASTER INDENTURE|Trust Indenture|SENIOR SUBORDINATED INDENTURE|SUBORDINATED DEBT INDENTURE|Senior Debt Indenture|BASE INDENTURE|Junior Subordinated Indenture|Warrant Indenture|Guarantee Indenture|Senior Secured Indenture|convertible indenture|Special Warrant Indenture|Omnibus Indenture|Convertible Senior Notes Indenture|Contingent Capital Securities Indenture|Subordinate Indenture|Mortgage Indenture|PIK Notes Indenture|Omnibus Master Indenture|Subordinated Debt Securities Indenture|Senior Debt Securities Indenture|Debt Securities Indenture|Debt Indenture|Senior Secured Stub Notes Indenture|Junior Secured Notes Indenture|CLO Indenture|UNSUBORDINATED INDENTURE|Reference is made to that certain Indenture|Contingent Convertible Capital Securities Indenture|Credit Enhanced Bonds Indenture|Coupon Bonds Indenture|Subordinate Voting share Purchase Warrant Indenture)$';\r\n"
				+ "\r\n" + "\r\n" + "UPDATE METADATA_JSON_" + yr + "Q" + qtr + "\r\n"
				+ "SET contractType='Indentures'\r\n" + "WHERE CONTRACTNAMEALGO RLIKE '^Master Indenture$';\r\n"
				
+ "UPDATE metadata_json_" + yr + "Q" + qtr
+ "\r\n" + "set contracttype =\r\n"
+ "case when contracttype='Indentures' and parent = 'Ancillary Transaction Docs' and openingparagraph rlike 'Officer.{1,4} Certificate' then 'Officer Certificates'\r\n"
+ "when contracttype='Description of Securities' and openingparagraph rlike 'note|bond|debentur|%' then 'Description of Notes'\r\n"
+ "when contracttype='Letter of Agreements' then 'Letter Agreements'\r\n"
+ "when contracttype rlike 'other notices' and contractnamealgo rlike 'notice' and CONTRACTNAMEALGO rlike ' grant' then 'Notices of Grant'\r\n"
+ "when contracttype rlike 'other notices' and contractnamealgo rlike 'change of trustee' then 'Notices of Change of Trustee'\r\n"
+ "when contracttype rlike 'other notices' and contractnamealgo rlike 'settlement' then 'Notices of Settlement'\r\n"
+ "when contracttype rlike 'other policies' and contractnamealgo rlike 'Whistleblower Policy' then 'Whistleblower Policies'\r\n"
+ "when contracttype rlike 'other policies' and contractnamealgo rlike 'Foreign Corrupt Practice Act|FCPA' then 'Foreign Corrupt Practice Act Policies'\r\n"
+ "when contracttype rlike 'other policies' and contractnamealgo rlike 'Document Retention' then 'Document Retention Policies'\r\n"
+ "when contracttype rlike '^indenture$' and parent not rlike 'financial contract' then 'Other'\r\n"
+ "else contracttype end;\r\n" + "\r\n" + "UPDATE metadata_json_" + yr + "Q" + qtr + "\r\n"
+ "set contractnamealgo =\r\n"
+ "case when contractnamealgo='Indenture' and parent = 'Ancillary Transaction Docs' and  contracttype='Officer Certificates' then 'Officer Certificates'\r\n"
+ "else contractnamealgo end;\r\n" + "\r\n" + "UPDATE metadata_json_" + yr + "Q" + qtr + "\r\n"
+ "set parent =\r\n" + "case \r\n"
+ "when parent = 'Letters & Offers' then 'Ancillary Transaction Docs'\r\n"
+ "when parent = 'HR - Letters & Offers'  then 'Ancillary Transaction Docs'\r\n"
//+ "when parent = 'Policies'  then 'Formation'\r\n"
+ "when parent = 'Plans - Funds' then 'Financial Contracts'\r\n"
+ "when parent='HR Plans' and contracttype = 'Retirement Agreements' then 'HR Contracts'\r\n"
+ "else parent end;\r\n"
				
				;
		String query2 = "", query2b = "", query3 = "", query_json_all = "";
		if (exportMasterIdx) {
			filename = mySQL_masterIdx_new + "masterIdx" + yr + "QTR" + qtr + ".csv";
			Utils.createFoldersIfReqd(mySQL_masterIdx_new);
			File file = new File(filename);
			if (file.exists()) {
				file.delete();
				System.out.println("deleted file =" + file.getAbsolutePath());
			}
			System.out.println("file==" + file.getAbsolutePath());

			boolean added = false;
			int cnt = 0;
			for (int i = 0; i < listParentGroupToParse.size(); i++) {
				added = false;
				if (listParentGroupToParse.get(i).contains("contracts_and_opinions")) {// when parsing just contracts
					// and opinions.
					String fromStatement = " from (\r\n" + "select case\r\n"
							+ "when fsize>8000 and fsize<=20000 and (glaw!=-1 or modify rlike 'amended|form' or contractlongname rlike 'agreement|(supplemental )?indenture|lease|guarant|promissory|warrant|Declaration of Trust'\r\n"
							+ "or left(openingparagraph, 45) rlike 'this (amendment)?.{1,10}(agreement|(supplemental )?indenture|lease|guarant|promissory|warrant|Declaration of Trust)' )\r\n"
							+ "and parent rlike 'contracts' then 1\r\n"
							+ "when fsize>=1000000 and glaw!=-1 and parent rlike 'contracts' then 1 \r\n"
							+ "when fsize>=300000 and glaw=-1 and (CONTRACTNAMEALGO rlike 'Servicing Agreement|^indenture|prospectus|certificate|statement' \r\n"
							+ "or CONTRACTLONGNAME rlike 'certificate|statement|report|prospectus|memorandum|pricing'\r\n"
							+ "or openingParagraph rlike 'certificate|statement|report|prospectus|memorandum|pricing') and parent rlike 'contracts' then 0 \r\n"
							+ "when fsize>2500 and parent rlike 'opinion' then 1\r\n"
							+ "when fsize>15000 and fsize<1000000 and parent rlike 'contracts' then 1\r\n"
							+ "when parent not rlike 'contract|opinion' and fsize>1000 then 1\r\n"
							+ "else 0 end keep, t1.*\r\n" + "from metadata_json_" + yr + "Q" + qtr + " t1\r\n"
							+ "order by fdate ) t1 \r\n" + "where keep=1  ";

					if (modifiedFilter.length() > 0) {
						// b/c data takes so long to parse - sometimes I modify filter to eliminate very
						// large files and very small files
						fromStatement = fromStatement + modifiedFilter;
					}

					query2 = "\r\n"
							+ "select cik, companyName, formType, fdate, kid, `5`, contractType, fsize, acc, acc_link_filename, edgarLink, contractLongName, type, legalEntitiesInOpeningParagraph, "
							+ "numberOfLegalEntities, openingParagraph, openParaContractName, contractNameAlgo, legalEntities, score, solrCore, pattern, gLaw, parent, kty2, ticker, sector, industry, contractnameok, modify "
							+ "\r\n" + fromStatement;

					query_json_all = "\r\nselect CIK,COMPANYNAME,FORMTYPE,TYPE,FDATE,KID,FSIZE,ACC,ACC_LINK_FILENAME,CONTRACTNAMEALGO,CONTRACTTYPE,PARENT,CONTRACTLONGNAME,GLAW,TICKER,SECTOR,INDUSTRY,CONTRACTNAMEOK,MODIFY,"
							+ "EDGARLINK,OPENINGPARAGRAPH,SCORE " + fromStatement;
					cnt++;
					added = true;
				}

				if (added && cnt > 1) {
					sb.append("\r\nUNION\r\n" + query2);
					sb_json_all.append("\r\nUNION\r\n" + query_json_all);
//					System.out.println("added union query2.=" + query2);
				}
				if (added && cnt <= 1) {
//					System.out.println("added query2.=" + query2);
					sb.append(query2);
					sb_json_all.append(query_json_all);
				}
			}
		}

		File f = new File(filename);
		if (f.exists())
			f.delete();

		query3 = "\r\n\r\ninsert ignore into metadata_json_all\r\n" + sb_json_all.toString() + ";\r\n"
				+ "\r\n\r\ninsert ignore into metadata_json_all_backup\r\n" + sb_json_all.toString() + ";\r\n";
		query2b = " into outfile '" + filename + "'\r\n" + "fields terminated by '||' \r\n"
				+ "lines terminated by '\r\n'" + ";\r\n\r\n";

		MysqlConnUtils.executeQueryDB(query + sb.toString() + query2b + query3, "contracts");

//		MysqlConnUtils.executeQueryDB(query + query2 + query2b + query3, "contracts");
		String text = "";

		if (exportMasterIdx) {
			text = Utils.readTextFromFile(filename);
		}
		return text;

	}

	public static void main(String[] args) throws Exception {

		int sY = 2017, eY = 2022, sQ = 1, eQ = 4, sYr = sY, eYr = sY, sQr = sQ, eQr = eQ;
		List<String> listParentsToParse = new ArrayList<String>();
		listParentsToParse.add("contracts_and_opinions");

		boolean regenerate = false;// if true then cycle through full history. otherwise just insert new.
		if (regenerate) {
			DisplayNames.drop_and_delete_displayName_tables();// only if I regenerate create_mysql_table_of_displaynames
		}

		for (; sY <= eY; sY++) {
			for (; sQ <= eQ; sQ++) {
				DisplayNames.create_mysql_table_of_displaynames(sY, sQ);// this runs immediately after metadata is in
			}
			sQ = 1;
		}

		sY = sYr;
		eY = eYr;
		sQ = sQr;
		eQ = eQr;

		for (; sY <= eY; sY++) {
			for (; sQ <= eQ; sQ++) {
				DisplayNames.validateContractDisplayNames(sY, sQ);// update displaynames for new data.
			}
			sQ = 1;
		}

		sY = sYr;
		eY = eYr;
		sQ = sQr;
		eQ = eQr;

		for (; sY <= eY; sY++) {
			for (; sQ <= eQ; sQ++) {
				GetContractTypesReady.export_metadata_as_masterIdx(sY, sQ, true, listParentsToParse, "");
			}
		}
	}
}
