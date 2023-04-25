package xbrl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

//NOTES TO LEAVE: executeInMysql is getting just 1 row name for total sales/revenues
//executeComboInMysql is getting 2 or more row names and aggregating them to equal total revenues

//See chartUI of BLK (blackrock) - missed revenue because it uses subcomponents not captured here.

public class FinancialStatement {

	public static String cashFlowStatement = "(t2.pre_link_role_name like '%cashflow%' and t2.pre_link_role_name not like '%table%' and t2.pre_link_role_name not like '%details%' and t2.pre_link_role_name not like '%information%' and t2.pre_link_role_name not like '%component%'and t2.pre_link_role_name not like '%parenthetical%' and t2.pre_link_role_name not like '%disclosure%' and t2.pre_link_role_name not like '%note%')";
	public static String incomeStatement = 
							"(pre_link_role_name like '%revenue%' \r"
						+ " OR pre_link_role_name like '%operation%' \r"
						+ " OR pre_link_role_name like '%income%' \r"
						+ " OR pre_link_role_name like '%earning%'\r"
						+ " or pre_link_role_name rlike '%result%operation%' \r"
						+ " OR pre_link_role_name like '%Statement%loss%' \r"
						+ " OR pre_link_role_name ='SupplementalFinancialStatementInformationEffectsOnNetIncomeOfAmountsReclassifiedFromAociDetails' \r"
						+ " OR pre_link_role_name ='SupplementaryFinancialInformation' \r"
						+ ")\r\r"
						+ " AND \r\r( pre_link_role_name not like '%table%' \r"
						+ " and pre_link_role_name not like '%Earning%Share%' and pre_link_role_name not like '%detail%' \r"
						+ " and pre_link_role_name not like '%component%' and pre_link_role_name not like '%information%' \r"
						+ " and pre_link_role_name not like '%parenthetical%' and pre_link_role_name not like '%disclosure%' \r"
						+ " and pre_link_role_name not like '%accumulate%' and pre_link_role_name not like '%discontinue%' \r"
						+ " and pre_link_role_name not like '%derivative%' and pre_link_role_name not like '%tax%' \r"
						+ " and pre_link_role_name not like '%note%' and pre_link_role_name not like '%presentation%' \r"
						+ " and pre_link_role_name not like '%netincome%' and pre_link_role_name not like '%expense%'"
						+ " and pre_link_role_name not like '%royalty%' and pre_link_role_name not like '%Impairments%' \r"
						+ " and pre_link_role_name not like '%financialposition%' and pre_link_role_name not like '%cashflow%' \r"
						+ " and pre_link_role_name not like '%detail%' and pre_link_role_name not like '%pershare%' and \r"
						+ " pre_link_role_name not like '%financialposition%')"
						+ "\r\r";


	public static String salesNames = "(name = 'Revenues' or name = 'sales' or \r"
			+ "name = 'salesRevenueGoodsNet' or name = 'salesRevenueNet' or \r"
			+ "name = 'salesRevenueServicesNet' or name = 'UtilityRevenue' or \r"
			+ "name = 'OilAndGasRevenue' or name = 'salesRevenueGoodsGross' or \r"
			+ "name = 'RealEstateRevenueNet' or name = 'ElectricDomesticRegulatedRevenue' or \r"
			+ "name = 'ContractsRevenue' or name = 'RoyaltyRevenue' or \r"
			+ "name = 'salesReturnsAndAllowancesGoods' or name = 'foodAndBeverageRevenue' or \r"
			+ "name = 'salesRevenueServicesGross' or name = 'OperatingLeasesIncomeStatementLeaseRevenue' or \r"
			+ "name = 'OperationsRevenueFromRealEstatePropertyOperations' or name = 'TotalRevenuesAndOtherIncome' or \r"
			+ "name = 'RevenuesOperatingAndNonoperating' or name = 'RevenueFromLeasedAndOwnedHotels' or \r"
			+ "name = 'FinancialServicesRevenue' or name = 'AggregateRevenue' or \r"
			+ "name = 'NetRevenues' or name = 'RevenuesExcludingInterestAndDividends' or \r"
			+ "name = 'ElectricUtilityRevenue' or name = 'RevenuesNet' or \r"
			+ "name = 'RegulatedAndUnregulatedOperatingRevenue' or name = 'OthersalesRevenueNet' or \r"
			+ "name = 'UnregulatedOperatingRevenue' or name = 'TotalOperatingRevenue' or \r"
			+ "name = 'GrossRevenue' or name = 'TotalRevenuesAndIncomeFromEquityMethodInvestments' or \r"
			+ "name = 'PremiumAndServiceRevenues' or name = 'RegulatedOperatingRevenue' or \r"
			+ "name = 'HealthCareOrganizationRevenue' or name = 'NetRevenue' or \r"
			+ "name = 'HotelAndCasinoRevenueGross' or name = 'SoftwareAndRelatedServicesRevenue' or \r"
			+ "name = 'TotalFinancingRevenueAndOtherInterestIncome' or name = 'GrossRevenues' or \r"
			+ "name = 'AgrregateGrossRevenue' or name = 'RevenueMineralsales' or \r"
			+ "name = 'NetFinancingRevenue' or name = 'Storerevenues' or \r"
			+ "name = 'RealEstateRevenueandOther' or name = 'RevenueFromLeasedAndOwnedHotelsGross' or \r"
			+ "name = 'RetailRevenue' or name = 'OperatingRevenues' or \r"
			+ "name = 'InsuranceServicesRevenue' or name = 'OperatingAndCapitalLeasesIncomeStatementLeaseRevenue' or \r"
			+ "name = 'salesRevenueGross' or name = 'RevenuesAndOtherIncome' or \r"
			+ "name = 'RevenuesAndOtherIncomeNet' or name = 'NetFinancingAndOtherRevenues' or \r"
			+ "name = 'LeasesIncomeStatementLeaseRevenue' or name = 'NaturalGasMidstreamRevenue' or \r"
			+ "name = 'RevenuesGross' or name = 'GasGatheringTransportationMarketingAndProcessingRevenue' or \r"
			+ "name = 'RecurringRevenues' or name = 'SoftwareRelatedRevenue' or \r"
			+ "name = 'LicensingAndRoyaltyRevenue' or name = 'salesRevenueAutomotive' or \r"
			+ "name = 'Totallicenseserviceandmaintenancerevenue' or name = 'LicensesRevenue' \r"
			+ "  \r\ror \r"
			+ "name = 'NoninterestIncome' or name = 'InvestmentIncomeInterest' or \r"
			+ "name = 'InterestAndDividendIncomeOperating' or name ='RevenuesNetOfInterestExpense' or name = 'NetRevenuesIncludingNetInterestIncome' \r"
			+ "or name = 'InterestIncomeOperating' or name = 'TotalFeeRevenue'\r"
			+ "or name = 'RevenueFromContractWithCustomerIncludingAssessedTax' \r"
			+ "or name = 'RevenueFromContractWithCustomerExcludingAssessedTax')\r";

	// public static String InterestDividendOtherIncome =
	// "(name = 'NoninterestIncome' or name = 'InvestmentIncomeInterest' or name = 'InterestAndDividendIncomeOperating')";
	public static String netIncNames = "(name = 'NetIncomeLossAvailableToCommonStockholdersBasic' or name = 'NetIncomeLossAvailableToCommonStockholdersDiluted' or name = 'netincomeloss' or name = 'profitLoss' or name= 'IncomeLossAttributableToParent' or name = 'NetIncomeLossAttributableToParentDiluted' or name = 'NetOperatingIncomeLoss' or name = 'IncomeLossFromOperatingActivities')";
	public static String cf_opsNames = "(name = 'NetCashProvidedByUsedInOperatingActivities' or "
			+ "name = 'NetCashProvidedByUsedInOperatingActivitiesContinuingOperations' or "
			+ "name = 'NetCashProvidedByUsedInContinuingOperations' or "
			+ "name = 'NetCashUsedInOperatingActivities')";

	public static List<String[]> getFSList() {

		List<String[]> listIncomeStatmentAndLineItemTypesAndMysqlTableType = new ArrayList<String[]>();

		// ary=fs,names
		// cycle thru i/s

		String[] ary = { incomeStatement, salesNames, "sales" };
		listIncomeStatmentAndLineItemTypesAndMysqlTableType.add(ary);
		String[] ary2 = { incomeStatement, netIncNames, "ni" };
		listIncomeStatmentAndLineItemTypesAndMysqlTableType.add(ary2);
		String[] ary3 = { cashFlowStatement, cf_opsNames, "cf_ops" };
		listIncomeStatmentAndLineItemTypesAndMysqlTableType.add(ary3);

		return listIncomeStatmentAndLineItemTypesAndMysqlTableType;
	}

	public static void getFinancials(int startYr, int endYr, int qtr, boolean regenerate) throws SQLException, FileNotFoundException {
		// cycle through each financial statement in list. lineItemTypes are
		// then fetched (all at once) - e.g., sales, revenue, etc. Some types
		// need to be summed - method that sums lineItemTypes below this method.

		/*
		 * method works where highest value is value sought. But for some name
		 * types I need to sum them first - for examples if it is of
		 * revenue/sales category I don't need to. But if it is name =
		 * 'NoninterestIncome' or name = 'InvestmentIncomeInterest' or name =
		 * 'InterestAndDividendIncomeOperating' - I do. See WFC which reports
		 * both revenue category 'NoninterestIncome' and
		 * 'InterestAndDividendIncomeOperating' categories. Revenue category is
		 * a f/p and that is rectified after suming the interest caterogies.
		 * This should be conditioned on name type - e.g., for NI or Cash Flow I
		 * might not need to
		 */

		System.out.println("get list of f/s");
		
		List<String[]> listIncomeStatmentAndLineItemTypesAndMysqlTableType = getFSList();
//		System.out.println("will gather these f/s");
//		for(int i=0; i<listIncomeStatmentAndLineItemTypesAndMysqlTableType.size(); i++) {
//			System.out.println("f/s:" + Arrays.toString(listIncomeStatmentAndLineItemTypesAndMysqlTableType.get(i)));
//		}
		
		if (regenerate) {
			// just drops/creates tables
			startYr = 2009;
			qtr = 1;
			String query = "";
			System.out.println("regenerating and startYr="+startYr);
			for (int i = 0; i < listIncomeStatmentAndLineItemTypesAndMysqlTableType
					.size(); i++) {

				query = "DROP PROCEDURE IF EXISTS xbrl_all_start_"
						+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
								.get(i)[2]
						+ "_procedure;\r"
						+ "CREATE PROCEDURE xbrl_all_start_"
						+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
								.get(i)[2]
						+ "_procedure()\r"
						+ "\r"
						+ "BEGIN\r"
						+ "\r"
						+ "DROP TABLE IF EXISTS xbrl_all_start_"
						+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
								.get(i)[2]
						+ ";\r"
						+ "CREATE TABLE xbrl_all_start_"
						+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
								.get(i)[2]
						+ " (\r"
						+ "  `accno` varchar(20) NOT NULL DEFAULT '',\r"
						+ "  `acceptedDate` datetime DEFAULT NULL,\r"
						+ "  `ITS` varchar(20) DEFAULT NULL,\r"
						+ "  `formType` varchar(20) DEFAULT '0',\r"
						+ "  `prefix` varchar(15) NOT NULL DEFAULT '',\r"
						+ "  `name` varchar(150) NOT NULL DEFAULT '',\r"
						+ "  `value` double NOT NULL DEFAULT 0,\r"
						+ "  `Period` int(4) NOT NULL DEFAULT 0,\r"
						+ "  `startDate` date NOT NULL DEFAULT '0000-00-00',\r"
						+ "  `endDate` date NOT NULL DEFAULT '0000-00-00',\r"
						+ "  `unitRef` varchar(25) DEFAULT NULL,\r"
						+ "  `ROLE_NAME` varchar(255) DEFAULT NULL,\r"
						+ "  `segment` double NOT NULL DEFAULT -9,\r"
						+ "  `dimension` varchar(150) NOT NULL DEFAULT '',\r"
						+ "  `dimensionValue` varchar(150) NOT NULL DEFAULT '',\r"
						+ "  PRIMARY KEY (ACCNO,ENDDATE,PERIOD,VALUE,NAME,SEGMENT),\r"
						+ "  KEY `acceptedDate` (`acceptedDate`),\r"
						+ "  KEY `ITS` (`ITS`),\r"
						+ "  KEY `Period` (`Period`),\r"
						+ "  KEY `endDate` (`endDate`),\r"
						+ "  KEY `startDate` (`startDate`),\r"
						+ "  KEY `ROLE_NAME` (`ROLE_NAME`),\r"
						+ "  KEY `prefix` (`prefix`),\r"
						+ "    KEY `accno` (`accno`)\r"
						+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
						+ "\r" + "END;\r";

				MysqlConnUtils.executeQuery(query
						+ "\rcall xbrl_all_start_"
						+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
								.get(i)[2] + "_procedure;\r");
				query = "DROP PROCEDURE IF EXISTS xbrl_"
						+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
								.get(i)[2]
						+ "_procedure;\r"
						+ "CREATE PROCEDURE xbrl_"
						+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
								.get(i)[2]
						+ "_procedure()\r"
						+ "\r"
						+ "BEGIN\r"
						+ "\r"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`xbrl_"
						+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
								.get(i)[2]
						+ "`;\r"
						+ "CREATE TABLE xbrl_"
						+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
								.get(i)[2]
						+ " (\r"
						+ "  `rowNameKey` int(11) NOT NULL DEFAULT -1 COMMENT 'unique F/S lineItem name key. singe key if 2 names are same for same ITS',\r"
						+ "  `accno` varchar(20) NOT NULL DEFAULT '',\r"
						+ "  `acceptedDate` datetime NOT NULL DEFAULT '1901-01-01',\r"
						+ "  `ITS` varchar(20) NOT NULL DEFAULT '',\r"
						+ "  `CIK` int(11) NOT NULL DEFAULT 0,\r"
						+ "  `formType` varchar(20) DEFAULT '0',\r"
						+ "  `fye` varchar(10) DEFAULT '0',  \r"
						+ "  `prefix` varchar(15) NOT NULL DEFAULT '',\r"
						+ "  `name` varchar(150) NOT NULL DEFAULT '',\r"
						+ "  `value` double NOT NULL DEFAULT 0,\r"
						+ "  `Period` tinyint(3) NOT NULL DEFAULT 0,\r"
						+ "  `startDate` VARCHAR(10) NOT NULL DEFAULT '0000-00-00',\r"
						+ "  `endDate` VARCHAR(10) NOT NULL DEFAULT '0000-00-00',\r"
						+ "  `ROLE_NAME` varchar(255) NOT NULL DEFAULT '',\r"
						+ "  `segment` tinyint(2) NOT NULL DEFAULT -9,\r"
						+ "  `unitref` varchar(50) NOT NULL DEFAULT '',\r"
						+ "  \r"
						+ "  PRIMARY KEY (ACCNO,ENDDATE,PERIOD,ROWNAMEKEY,VALUE,SEGMENT),\r"
						+ "  KEY `RNK` (ACCNO,PERIOD,ROWNAMEKEY),\r"
						+ "  KEY `Period` (`Period`),\r"
						+ "  KEY `endDate` (`endDate`),\r"
						+ "  KEY `formType` (`ROLE_NAME`),\r"
						+ "  KEY `cik` (`cik`),\r"
						+ "  KEY `accno` (`accno`)\r"
						+ "  ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r" + "\r"
						+ "END;\r";
				MysqlConnUtils.executeQuery(query
						+ "\rcall xbrl_"
						+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
								.get(i)[2] + "_procedure;\r");
			}
		}

		// cycle thru to get those rolenames not in pre_loc table but in
		// cal_loc.
		String[] roleTable = { "pre_", "cal_" };
		// for each type(sales,NI, etc)
		int cnt = 0, reStartStartYr = startYr, reStartQtr = qtr;
		String localPath = "c:/BACKTEST/XBRL//";
		// cycle through each FS type (1 for sales, 1 for NI, 1 etc). Can't
		// combine name type (e.g., sales and NI) that I'm getting - although
		// both I/S.

//		System.out
//				.println("listIncomeStatmentAndLineItemTypesAndMysqlTableType.size="
//						+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
//								.size());
		for (int g = 0; g < listIncomeStatmentAndLineItemTypesAndMysqlTableType
				.size(); g++) {
			startYr = reStartStartYr;
			qtr = reStartQtr;

			for (; startYr <= endYr; startYr++) {
				cnt++;
				if (startYr == 2009 && qtr<2)
					qtr = 2;
				for (; qtr <= 4; qtr++) {
					localPath = localPath + startYr + "/QTR" + qtr + "/";

					for (int z = 0; z < roleTable.length; z++) {
						StringBuilder sb = new StringBuilder();
						
						sb.append("/*SET SQL_MODE = 'NO_ZERO_DATE';*/\r"
								+ "\r" + "DROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "AccNo1 ; \r"
								+ "CREATE TABLE TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "AccNo1 ENGINE=MYISAM\r"
								+ " SELECT accNo FROM xbrl_all_start_"
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "\rt1 /*where (t1.its = 'JPM' OR t1.ITS='GOOG' OR  t1.ITS='BK' OR  t1.ITS='BAC' OR  t1.ITS='AAPL' OR  t1.ITS='C' \r"
								+ "OR  t1.ITS='F' )*/  GROUP BY accNo; \r"
								+ "alter table TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "AccNo1 ADD KEY (accNo);\r"
								+ " \r"
								+ "DROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "AccNo2; \r"
								+ "CREATE TABLE TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "AccNo2 ENGINE=MYISAM\r"
								+ " SELECT accNo FROM "
								+ startYr
								+ "Q"
								+ qtr
								+ "xbrl_pre_link \r"
								+ "\r t1 /*where (t1.its = 'JPM' OR t1.ITS='GOOG' OR  t1.ITS='BK' OR  t1.ITS='BAC' OR  t1.ITS='AAPL' OR  t1.ITS='C' \r"
								+ "OR  t1.ITS='F' )*/   \r"
								+ " GROUP BY accNo; \r"
								+ "alter table TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "AccNo2 ADD PRIMARY KEY (accNo);  \r"
								+ "INSERT IGNORE INTO TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "AccNo2 \r"
								+ "SELECT accNo FROM "
								+ startYr
								+ "Q"
								+ qtr
								+ "xbrl_CAL_ARC   \r"
								+ "GROUP BY accNo; \r"
								+ "\r"
								+ "\r"
								+ "DROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "AccNo; \r"
								+ "CREATE TABLE TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "AccNo ENGINE=MYISAM\r"
								+ "  SELECT T1.accNo FROM (SELECT T1.accNo X,T2.accNo FROM TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "AccNo1 T1 \r"
								+ " RIGHT JOIN TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "AccNo2 T2  ON T1.accNo=T2.accNo) T1 WHERE X IS NULL;\r"
								+ "alter table TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "AccNo ADD PRIMARY KEY (accNo); \r"
								+ "\r"
								+ "DROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "XBRL_INS_DATA_ONLY; \r"
								+ "CREATE TABLE TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "XBRL_INS_DATA_ONLY ENGINE=MYISAM\r"
								+ " SELECT t1.accNo,t1.acceptedDate, t1.prefix, t1.name, t1.value, t1.contextRef, t1.unitRef \r"
								+ " FROM "
								+ startYr
								+ "Q"
								+ qtr
								+ "xbrl_ins_data t1 "
								+ " WHERE (\r"
								+ "\r"
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[1]
								+ ");\r"
								+ "\r"
								+ "alter table TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "XBRL_INS_DATA_ONLY ADD KEY (accNo); \r"

								+ "\rDROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "INS_DATA;\r"
								+ "CREATE TABLE TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "INS_DATA ENGINE=MYISAM"
								+ "\rSELECT T1.* FROM TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "XBRL_INS_DATA_ONLY T1 INNER JOIN TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "AccNo T2 ON T1.ACCNO=T2.ACCNO"
								+ "\r\r"+
								" WHERE trim(T1.VALUE) RLIKE '^-?[0-9]{1,100}$' \r" + 
								"OR\r\n" + 
								"trim(T1.VALUE)  RLIKE '^-?[0-9]{0,50}\\\\.[0-9]{1,50}';\r" + 
								"\r\r" 
								+ "alter table TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "INS_DATA ADD KEY (accNo), "
								+ "ADD KEY (contextref), ADD KEY (accNo,contextref),CHANGE prefix prefix VARCHAR(15) NOT NULL DEFAULT '',\r"
								+ "CHANGE VALUE VALUE DOUBLE NOT NULL DEFAULT 0, CHANGE NAME NAME VARCHAR(150) NOT NULL DEFAULT '';\r"

								+ "DROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ "A"
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA;\r"
								+ "CREATE TABLE TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ "A"
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA ENGINE=MYISAM\r"
								+ "select accno,ID, segment, startDate,endDate,instant"
								+ ", dimension,dimensionValue from "
								+ startYr
								+ "q"
								+ qtr
								+ "xbrl_ins_context;"
								+ "\rALTER TABLE TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ "A"
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA ADD KEY(ACCNO,ID);\r"

								+ "\rDROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA; \r"
								+ "CREATE TABLE TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA ENGINE=MYISAM\r"
								+ " SELECT t1.accNo,t1.acceptedDate,t1.prefix,t1.name,t1.value,t2.segment, t2.startDate,t2.endDate,t2.instant\r"
								+ " , t2.dimension,t2.dimensionValue,t1.unitRef\r"
								+ " FROM TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "Ins_data t1 inner join \r "
								+ "TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ "A"
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA t2 ON t1.accNo=t2.accNo AND t1.contextRef = t2.id \r"
								+ ";  \r"
								+ "\r"
								+ "alter table TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA ADD KEY(ACCNO,NAME),\r"
								+ "ADD KEY (accNo), ADD KEY(NAME),CHANGE prefix prefix VARCHAR(15) NOT NULL DEFAULT '', \r"
								+ " CHANGE VALUE VALUE DOUBLE NOT NULL DEFAULT 0, CHANGE SEGMENT SEGMENT DOUBLE NOT NULL DEFAULT -9,\r"
								+ " CHANGE NAME NAME VARCHAR(150) NOT NULL DEFAULT ''; \r"
								+ "\r"
								+ "DROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA_CHILD; \r"
								+ "CREATE TABLE TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA_CHILD ENGINE=MYISAM\r"
								+ " SELECT t1.accNo,t1.acceptedDate,t1.prefix,t1.name,t1.value,t1.segment,\r"
								+ "t1.startDate,t1.endDate,t1.instant,t1.unitRef,T1.dimension,t1.dimensionValue  \r"
								+ ",T2."
								+ roleTable[z]
								+ "LINK_ROLE_NAME PRE_LINK_ROLE_NAME\r"
								+ "FROM TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA T1 inner join "
								+ startYr
								+ "Q"
								+ qtr
								+ "xbrl_"
								+ roleTable[z]
								+ "LOC T2 \r"
								+ "ON (T1.accNo=T2.accNo AND T1.NAME=T2."
								+ roleTable[z]
								+ "LOC_HREF_NAME )\r");
								
								if(!roleTable[z].contains("cal_")) {
								sb.append(" OR (T1.accNo=T2.accNo AND T1.NAME=T2.PRE_LINK_ROLE_NAME )");
								}
								
								sb.append("\r"
								+ "WHERE ("
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[0].replaceAll("pre_",
										roleTable[z])
								+ ")\r"
								+ "\r"
								+ ";  \r"
								+ "alter table TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA_CHILD ADD KEY (accNo), ADD KEY(PRE_LINK_ROLE_NAME), \r"
								+ "ADD KEY(ACCNO,PRE_LINK_ROLE_NAME),\r"
								+ " CHANGE prefix prefix VARCHAR(15) NOT NULL DEFAULT '',  CHANGE VALUE VALUE DOUBLE NOT NULL DEFAULT 0,\r"
								+ " CHANGE SEGMENT SEGMENT DOUBLE NOT NULL DEFAULT -9, CHANGE NAME NAME VARCHAR(150) NOT NULL DEFAULT ''; \r"
								+ "\r"
								+ "\r"
								+ "DROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "XBRL_HOLD; \r"
								+ "CREATE TABLE TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "XBRL_HOLD ENGINE=MYISAM\r"
								+ " SELECT t1.accNo,t1.acceptedDate,t2.ITS,t2.formType,t1.prefix,t1.name,t1.value,\r"
								+ "t1.segment,t1.startDate,t1.endDate,t1.instant,t1.unitRef,dimension,dimensionvalue,T2.pre_link_role_name \r"
								+ "FROM TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA_CHILD T1 inner join "
								+ startYr
								+ "Q"
								+ qtr
								+ "xbrl_PRE_LINK T2 \r"
								+ "ON T1.accNo=T2.accNo AND T1.PRE_LINK_ROLE_NAME=T2.PRE_LINK_ROLE_NAME\r"
								+ ";\r"
								+ "ALTER TABLE TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "XBRL_HOLD ADD KEY(SEGMENT);\r"
								+ "\r"
								+ "\r"
								+ "\r"
								+ "insert ignore INTO xbrl_all_start_"
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ " \r"
								+ "SELECT accno,accepteddate,its,formType,prefix,\r"
								+ "case when pre_link_role_name rlike 'SupplementaryFinancialInformation' then concat('s_',name) else name end name\r"
								+ ",value\r"
								+ ",case when (DATEDIFF(enddate,startdate)/30) between 2.4 and 3.6 then 3 \r"
								+ "when (DATEDIFF(enddate,startdate)/30) between 5.4 and 6.6 then 6 \r"
								+ "when (DATEDIFF(enddate,startdate)/30) between 8.4 and 9.6 then 9 \r"
								+ "when (DATEDIFF(enddate,startdate)/30) between 11.4 and 12.6 then 12 \r"
								+ "else (DATEDIFF(enddate,startdate)/30) end Period\r"
								+ ",startDate, case when instant < 1 then endDate else \r"
								+ "endDate end endDate,unitRef,pre_link_role_name,segment,dimension,dimensionValue\r"
								+ "\r"
								+ "FROM TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "XBRL_HOLD where segment = 0;\r"
								+ "DROP TABLE IF EXISTS tmp"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA_ACCNO_ONLY;\r"
								+ "CREATE TABLE tmp"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA_ACCNO_ONLY ENGINE=MYISAM\r"
								+ "select accno from TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA t1 group by accno; \r"
								+ "\rALTER TABLE tmp"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA_ACCNO_ONLY ADD KEY(ACCNO);\r"
								+ "\r"
								+ "DROP TABLE IF EXISTS tmpxbrl_all_start_"
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ startYr
								+ "q"
								+ qtr
								+ "_ACCNO_ONLY;\r"
								+ "CREATE TABLE tmpxbrl_all_start_"
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ startYr
								+ "q"
								+ qtr
								+ "_ACCNO_ONLY ENGINE=MYISAM\r"
								+ "select accno from xbrl_all_start_"
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ " t1 group by accno;\r"
								+ "\rALTER TABLE tmpxbrl_all_start_"
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ startYr
								+ "q"
								+ qtr
								+ "_ACCNO_ONLY add key(accno);\r"

								+ "DROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "Q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "XBRL_SEG0 ;\r"
								+ "CREATE TABLE TMP_"
								+ startYr
								+ "Q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "XBRL_SEG0  ENGINE=MYISAM\r"
								+ "select t2.accno from tmpxbrl_all_start_"
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ startYr
								+ "q"
								+ qtr
								+ "_ACCNO_ONLY"
								+ " t1 right join \r"
								+ " tmp"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA_ACCNO_ONLY "
								+ " t2 on t1.accno=t2.accno\r"
								+ "where t1.accno is null;\r"

								+ "ALTER TABLE TMP_"
								+ startYr
								+ "Q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "XBRL_SEG0 ADD KEY(ACCNO);\r"
								+ "\r"
								+ "insert ignore INTO xbrl_all_start_"
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ " \r"
								+ "SELECT T1.accno,accepteddate,its,formType,prefix,\r"
								+ "case when pre_link_role_name rlike 'SupplementaryFinancialInformation' then concat('s_',name) else name end name\r"
								+ ",value\r"
								+ ",case when (DATEDIFF(enddate,startdate)/30) between 2.4 and 3.6 then 3 \r"
								+ "when (DATEDIFF(enddate,startdate)/30) between 5.4 and 6.6 then 6 \r"
								+ "when (DATEDIFF(enddate,startdate)/30) between 8.4 and 9.6 then 9 \r"
								+ "when (DATEDIFF(enddate,startdate)/30) between 11.4 and 12.6 then 12 \r"
								+ "else (DATEDIFF(enddate,startdate)/30) end Period\r"
								+ ",startDate, case when instant < 1 then endDate else \r"
								+ "endDate end endDate,unitRef,pre_link_role_name,segment,dimension,dimensionValue\r"
								+ "\r"
								+ "FROM TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "XBRL_HOLD T1 INNER JOIN TMP_"
								+ startYr
								+ "Q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "XBRL_SEG0 T2\r"
								+ "ON T1.ACCNO=T2.ACCNO\r"
								+ "where segment = 1;\r\r"
								+ "DROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "accNo; \r"
								+ "DROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "accNo1; \r"
								+ "DROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "accNo2; \r"
								+ "DROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA; \r"
								+ "DROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "DATA_CHILD; \r"
								+ "DROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "INS_DATA; \r"
								+ "DROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "XBRL_HOLD; \r"
								+ "DROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "XBRL_SEG0 ;\r"
								+ "DROP TABLE IF EXISTS TMP_"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ "XBRL_INS_DATA_ONLY;"
								+ "\rDROP TABLE IF EXISTS tmpxbrl_all_start_"
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2]
								+ startYr
								+ "q"
								+ qtr
								+ "_ACCNO_ONLY;\r"

								+ "DROP TABLE IF EXISTS tmp"
								+ startYr
								+ "q"
								+ qtr
								+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
										.get(g)[2] + "DATA_ACCNO_ONLY;\r");

						MysqlConnUtils.executeQuery(sb.toString());
						sb.delete(0, sb.toString().length());
						
					}
					
					String query = "/*SET SQL_MODE = 'NO_ZERO_DATE';*/\r"
							+ "\r" + "DROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "AccNo1 ; \r"
							+ "CREATE TABLE TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "AccNo1 ENGINE=MYISAM\r"
							+ " SELECT accNo FROM xbrl_all_start_"
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "\rt1 /*where (t1.its = 'JPM' OR t1.ITS='GOOG' OR  t1.ITS='BK' OR  t1.ITS='BAC' OR  t1.ITS='AAPL' OR  t1.ITS='C' \r"
							+ "OR  t1.ITS='F' )*/  GROUP BY accNo; \r"
							+ "alter table TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "AccNo1 ADD KEY (accNo);\r"
							+ " \r"
							+ "DROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "AccNo2; \r"
							+ "CREATE TABLE TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "AccNo2 ENGINE=MYISAM\r"
							+ " SELECT accNo FROM "
							+ startYr
							+ "Q"
							+ qtr
							+ "xbrl_pre_link \r"
							+ "\r t1 /*where (t1.its = 'JPM' OR t1.ITS='GOOG' OR  t1.ITS='BK' OR  t1.ITS='BAC' OR  t1.ITS='AAPL' OR  t1.ITS='C' \r"
							+ "OR  t1.ITS='F' )*/   \r"
							+ " GROUP BY accNo; \r"
							+ "alter table TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "AccNo2 ADD PRIMARY KEY (accNo);  \r"
							+ "INSERT IGNORE INTO TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "AccNo2 \r"
							+ "SELECT accNo FROM "
							+ startYr
							+ "Q"
							+ qtr
							+ "xbrl_CAL_ARC   \r"
							+ "GROUP BY accNo; \r"
							+ "\r"
							+ "\r"
							+ "DROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "AccNo; \r"
							+ "CREATE TABLE TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "AccNo ENGINE=MYISAM\r"
							+ "  SELECT T1.accNo FROM (SELECT T1.accNo X,T2.accNo FROM TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "AccNo1 T1 \r"
							+ " RIGHT JOIN TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "AccNo2 T2  ON T1.accNo=T2.accNo) T1 WHERE X IS NULL;\r"
							+ "alter table TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "AccNo ADD PRIMARY KEY (accNo); \r"
							+ "\r"
							+ "DROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "XBRL_INS_DATA_ONLY; \r"
							+ "CREATE TABLE TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "XBRL_INS_DATA_ONLY ENGINE=MYISAM\r"
							+ " SELECT t1.accNo,t1.acceptedDate, t1.prefix, t1.name, t1.value, t1.contextRef, t1.unitRef \r"
							+ " FROM "
							+ startYr
							+ "Q"
							+ qtr
							+ "xbrl_ins_data t1 "
							+ " WHERE (\r"
							+ "\r"
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[1]
							+ ");\r"
							+ "\r"
							+ "alter table TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "XBRL_INS_DATA_ONLY ADD KEY (accNo); \r"

							+ "\rDROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "INS_DATA;\r"
							+ "CREATE TABLE TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "INS_DATA ENGINE=MYISAM"
							+ "\rSELECT T1.* FROM TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "XBRL_INS_DATA_ONLY T1 INNER JOIN TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "AccNo T2 ON T1.ACCNO=T2.ACCNO"
							+ "\r\r"+
							" WHERE trim(T1.VALUE) RLIKE '^-?[0-9]{1,100}$' \r" + 
							"OR\r\n" + 
							"trim(T1.VALUE)  RLIKE '^-?[0-9]{0,50}\\\\.[0-9]{1,50}';\r" + 
							"\r\r" 
							+ "alter table TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "INS_DATA ADD KEY (accNo), "
							+ "ADD KEY (contextref), ADD KEY (accNo,contextref),CHANGE prefix prefix VARCHAR(15) NOT NULL DEFAULT '',\r"
							+ "CHANGE VALUE VALUE DOUBLE NOT NULL DEFAULT 0, CHANGE NAME NAME VARCHAR(150) NOT NULL DEFAULT '';\r"

							+ "DROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ "A"
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA;\r"
							+ "CREATE TABLE TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ "A"
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA ENGINE=MYISAM\r"
							+ "select accno,ID, segment, startDate,endDate,instant"
							+ ", dimension,dimensionValue from "
							+ startYr
							+ "q"
							+ qtr
							+ "xbrl_ins_context;"
							+ "\rALTER TABLE TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ "A"
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA ADD KEY(ACCNO,ID);\r"

							+ "\rDROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA; \r"
							+ "CREATE TABLE TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA ENGINE=MYISAM\r"
							+ " SELECT t1.accNo,t1.acceptedDate,t1.prefix,t1.name,t1.value,t2.segment, t2.startDate,t2.endDate,t2.instant\r"
							+ " , t2.dimension,t2.dimensionValue,t1.unitRef\r"
							+ " FROM TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "Ins_data t1 inner join \r "
							+ "TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ "A"
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA t2 ON t1.accNo=t2.accNo AND t1.contextRef = t2.id \r"
							+ ";  \r"
							+ "\r"
							+ "alter table TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA ADD KEY(ACCNO,NAME),\r"
							+ "ADD KEY (accNo), ADD KEY(NAME),CHANGE prefix prefix VARCHAR(15) NOT NULL DEFAULT '', \r"
							+ " CHANGE VALUE VALUE DOUBLE NOT NULL DEFAULT 0, CHANGE SEGMENT SEGMENT DOUBLE NOT NULL DEFAULT -9,\r"
							+ " CHANGE NAME NAME VARCHAR(150) NOT NULL DEFAULT ''; \r"
							+ "\r"
							+ "DROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA_CHILD; \r"
							+ "CREATE TABLE TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA_CHILD ENGINE=MYISAM\r"
							+ " SELECT t1.accNo,t1.acceptedDate,t1.prefix,t1.name,t1.value,t1.segment,\r"
							+ "t1.startDate,t1.endDate,t1.instant,t1.unitRef,T1.dimension,t1.dimensionValue  \r"
							+ ",T2."
							+ "lab_"
							+ "loc_href_name lab_LOC_HREF_NAME\r"
							+ "FROM TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA T1 inner join "
							+ startYr
							+ "Q"
							+ qtr
							+ "xbrl_"
							+ "lab_"
							+ "LOC T2 \r"
							+ "ON (T1.accNo=T2.accNo AND T1.NAME=T2."
							+ "lab_"
							+ "LOC_HREF_NAME )\r"
//							+" OR (T1.accNo=T2.accNo AND T1.NAME=T2.PRE_LINK_ROLE_NAME )"
							+ "\r"
							+ "WHERE ("
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[0].replaceAll("pre_link_role_name", "lab_LOC_HREF_NAME")
							+ ")\r"
							+ "\r"
							+ ";  \r"
							+ "alter table TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA_CHILD ADD KEY (accNo), ADD KEY(lab_LOC_HREF_NAME), \r"
							+ "ADD KEY(ACCNO,lab_LOC_HREF_NAME),\r"
							+ " CHANGE prefix prefix VARCHAR(15) NOT NULL DEFAULT '',  CHANGE VALUE VALUE DOUBLE NOT NULL DEFAULT 0,\r"
							+ " CHANGE SEGMENT SEGMENT DOUBLE NOT NULL DEFAULT -9, CHANGE NAME NAME VARCHAR(150) NOT NULL DEFAULT ''; \r"
							+ "\r"
							+ "\r"
							+ "DROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "XBRL_HOLD; \r"
							+ "CREATE TABLE TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "XBRL_HOLD ENGINE=MYISAM\r"
							+ " SELECT t1.accNo,t1.acceptedDate,'' ITS,'' formType,t1.prefix,t1.name,t1.value,\r"
							+ "t1.segment,t1.startDate,t1.endDate,t1.instant,t1.unitRef,dimension,dimensionvalue,T2.lab_LOC_HREF_NAME \r"
							+ "FROM TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA_CHILD T1 inner join "
							+ startYr
							+ "Q"
							+ qtr
							+ "xbrl_lab_loc T2 \r"
							+ "ON T1.accNo=T2.accNo AND T1.lab_LOC_HREF_NAME=T2.lab_LOC_HREF_NAME\r"
							+ ";\r"
							+ "ALTER TABLE TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "XBRL_HOLD ADD KEY(SEGMENT), ADD KEY(ACCNO);\r"
							+ "\r"
							+ "\r"
							+ "\r"
							+ "insert ignore INTO xbrl_all_start_"
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ " \r"
							+ "SELECT t1.accno,t1.accepteddate,t2.its,t2.formType,t1.prefix,\r"
							+ "case when lab_LOC_HREF_NAME rlike 'SupplementaryFinancialInformation' then concat('s_',name) else name end name\r"
							+ ",value\r"
							+ ",case when (DATEDIFF(enddate,startdate)/30) between 2.4 and 3.6 then 3 \r"
							+ "when (DATEDIFF(enddate,startdate)/30) between 5.4 and 6.6 then 6 \r"
							+ "when (DATEDIFF(enddate,startdate)/30) between 8.4 and 9.6 then 9 \r"
							+ "when (DATEDIFF(enddate,startdate)/30) between 11.4 and 12.6 then 12 \r"
							+ "else (DATEDIFF(enddate,startdate)/30) end Period\r"
							+ ",startDate, case when instant < 1 then endDate else \r"
							+ "endDate end endDate,unitRef,lab_LOC_HREF_NAME,segment,dimension,dimensionValue\r"
							+ "\r"
							+ "FROM TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "XBRL_HOLD t1 \r"
							
							+"inner join "+ 
							 startYr
								+ "q"
								+ qtr
								+"xbrl_pre_link t2 on t1.accno=t2.accno"
							+ "\rwhere segment = 0;\r"
							+ "\r\rDROP TABLE IF EXISTS tmp"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA_ACCNO_ONLY;\r"
							+ "CREATE TABLE tmp"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA_ACCNO_ONLY ENGINE=MYISAM\r"
							+ "select accno from TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA t1 group by accno; \r"
							+ "\rALTER TABLE tmp"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA_ACCNO_ONLY ADD KEY(ACCNO);\r"
							+ "\r"
							+ "DROP TABLE IF EXISTS tmpxbrl_all_start_"
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ startYr
							+ "q"
							+ qtr
							+ "_ACCNO_ONLY;\r"
							+ "CREATE TABLE tmpxbrl_all_start_"
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ startYr
							+ "q"
							+ qtr
							+ "_ACCNO_ONLY ENGINE=MYISAM\r"
							+ "select accno from xbrl_all_start_"
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ " t1 group by accno;\r"
							+ "\rALTER TABLE tmpxbrl_all_start_"
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ startYr
							+ "q"
							+ qtr
							+ "_ACCNO_ONLY add key(accno);\r"

							+ "DROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "Q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "XBRL_SEG0 ;\r"
							+ "CREATE TABLE TMP_"
							+ startYr
							+ "Q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "XBRL_SEG0  ENGINE=MYISAM\r"
							+ "select t2.accno from tmpxbrl_all_start_"
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ startYr
							+ "q"
							+ qtr
							+ "_ACCNO_ONLY"
							+ " t1 right join \r"
							+ " tmp"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA_ACCNO_ONLY "
							+ " t2 on t1.accno=t2.accno\r"
							+ "where t1.accno is null;\r"

							+ "ALTER TABLE TMP_"
							+ startYr
							+ "Q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "XBRL_SEG0 ADD KEY(ACCNO);\r"
							+ "\r"
							
							+
							"DROP TABLE IF EXISTS TMP_"+ startYr
							+ "Q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]+"XBRL_HOLD_A \r;"
							+"CREATE TABLE TMP_"+ startYr
							+ "Q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
											+"XBRL_HOLD_A  ENGINE=MYISAM \r"
							+" select t1.accNo, t1.acceptedDate, t2.its, t2.formType, t1.prefix, t1.name, t1.value, t1.segment, \r"
							+ "t1.startDate, t1.endDate, t1.instant, t1.unitRef, t1.dimension, t1.dimensionvalue, t1.lab_LOC_HREF_NAME "
							+" from  TMP_"+ startYr
							+ "Q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]+
											"XBRL_HOLD t1 inner join "+ startYr
											+ "Q"
											+ qtr
															+
															"xbrl_pre_link t2 \r"
							+"on t1.accno=t2.accno;\r\r"
							
							+ "insert ignore INTO xbrl_all_start_"
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ " \r"
							+ " SELECT T1.accno,accepteddate,its,formType,prefix,\r"
							+ " case when lab_LOC_HREF_NAME rlike 'SupplementaryFinancialInformation' then concat('s_',name) else name end name\r"
							+ ",value\r"
							+ ",case when (DATEDIFF(enddate,startdate)/30) between 2.4 and 3.6 then 3 \r"
							+ " when (DATEDIFF(enddate,startdate)/30) between 5.4 and 6.6 then 6 \r"
							+ " when (DATEDIFF(enddate,startdate)/30) between 8.4 and 9.6 then 9 \r"
							+ " when (DATEDIFF(enddate,startdate)/30) between 11.4 and 12.6 then 12 \r"
							+ " else (DATEDIFF(enddate,startdate)/30) end Period\r"
							+ ",startDate, case when instant < 1 then endDate else \r"
							+ "endDate end endDate,unitRef,lab_LOC_HREF_NAME,segment,dimension,dimensionValue\r"
							+ "\r"
							+ "FROM TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
							.get(g)[2]
							+ "XBRL_HOLD_A T1 INNER JOIN TMP_"
							+ startYr
							+ "Q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "XBRL_SEG0 T2\r"
							+ "ON T1.ACCNO=T2.ACCNO\r"
							+ "where segment = 1;\r\r"
							
							+ "\rDROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "accNo; \r"
							+ "DROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "accNo1; \r"
							+ "DROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "accNo2; \r"
							+ "DROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA; \r"
							+ "DROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "DATA_CHILD; \r"
							+ "DROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "INS_DATA; \r"
							+ "DROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "XBRL_HOLD; \r"
							+ "DROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "XBRL_SEG0 ;\r"
							+ "DROP TABLE IF EXISTS TMP_"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ "XBRL_INS_DATA_ONLY;"
							+ "\rDROP TABLE IF EXISTS tmpxbrl_all_start_"
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2]
							+ startYr
							+ "q"
							+ qtr
							+ "_ACCNO_ONLY;\r"

							+ "DROP TABLE IF EXISTS tmp"
							+ startYr
							+ "q"
							+ qtr
							+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
									.get(g)[2] + "DATA_ACCNO_ONLY;\r";

					MysqlConnUtils.executeQuery(query);

				}
				qtr = 1;
			}
		}
	}

	public static void getFinancialRanks() throws SQLException, FileNotFoundException {

		List<String[]> listIncomeStatmentAndLineItemTypesAndMysqlTableType = getFSList();

		
		for (int g = 0; g < listIncomeStatmentAndLineItemTypesAndMysqlTableType
				.size(); g++) {
			

			MysqlConnUtils.executeQuery("call conformCIK_"
					+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
							.get(g)[2] + "();"); // 3
			MysqlConnUtils.executeQuery("call homogenizeRoNames_"
					+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
							.get(g)[2] + "();"); // 3
			MysqlConnUtils.executeQuery("call getRestatements_"
					+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
							.get(g)[2] + "_procedure();"); // 4
			MysqlConnUtils.executeQuery("call QUARTERLY_RANK_"
					+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
							.get(g)[2] + "();");// 5
			MysqlConnUtils.executeQuery("call QUARTERLY_RANK_"
					+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
							.get(g)[2] + "2();");// 6
			MysqlConnUtils.executeQuery("call QUARTERLY_RANK_"
					+ listIncomeStatmentAndLineItemTypesAndMysqlTableType
							.get(g)[2] + "3();");// 7

		}
	}

	public static void updateXbrlFilerInfo(String localPath) throws FileNotFoundException {

		localPath = localPath.toLowerCase();

		String year = localPath.substring(localPath.lastIndexOf("xbrl/") + 6,
				localPath.lastIndexOf("xbrl/") + 10);
		System.out.println("year:: " + year);
		String qtr = localPath.substring(localPath.lastIndexOf("qtr") + 3,
				localPath.lastIndexOf("qtr") + 4);
		System.out.println("qtr:: " + qtr);
		String xbrlPrefix = year + "Q" + qtr + "xbrl_";
		System.out.println("xbrlPrefix:: " + xbrlPrefix);

		String query = "/*SET SQL_MODE = 'NO_ZERO_DATE';*/\r\rDROP TABLE IF EXISTS TMP_ITS; "
				+ "\rCREATE TABLE TMP_ITS ENGINE=MYISAM\r"
				+ " SELECT ACCNO,ACCEPTEDDATE,ITS FROM "
				+ xbrlPrefix
				+ "PRE_LINK GROUP BY ACCNO;\r"
				+ "ALTER TABLE TMP_ITS ADD KEY(ACCNO);";
		String query2 = "\r\rDROP TABLE IF EXISTS TMP_MISSING_ACCNO; "
				+ "\rCREATE TABLE TMP_MISSING_ACCNO ENGINE=MYISAM"
				+ "\r SELECT T1.ACCNO ACC FROM ( SELECT T1.ACCNO,T2.ACCNO AN2 "
				+ "FROM TMP_ITS T1 LEFT JOIN xbrl_filer_info T2  ON T1.ACCNO=T2.ACCNO) T1 WHERE AN2 IS NULL;"
				+ " \ralter table TMP_MISSING_ACCNO ADD KEY (ACC);";
		String query3 = "\r\rDROP TABLE IF EXISTS tmp_ins_data; "
				+ "\rCREATE TABLE tmp_ins_data ENGINE=MYISAM\r"
				+ "SELECT t1.* FROM "
				+ xbrlPrefix
				+ "ins_data T1 inner join\r TMP_MISSING_ACCNO T2 ON T1.ACCNO=T2.ACC \r"
				+ "WHERE PREFIX='DEI';\r"
				+ "\r\rDROP TABLE IF EXISTS tmp_ins_context; \r"
				+ "CREATE TABLE tmp_ins_context ENGINE=MYISAM\r"
				+ "SELECT accno,cik FROM "+xbrlPrefix+"ins_context T1 inner join\r"
				+ "TMP_MISSING_ACCNO T2 ON T1.ACCNO=T2.ACC \rgroup by accno;";

		String query4 = "\r\rcall XBRL_Filer();\r\r";
		String query5 =  "\r\n call confirm_xbrl_filer_info_its_procedure()";
		
		try {
			MysqlConnUtils.executeQuery(query);
			MysqlConnUtils.executeQuery(query2);
			MysqlConnUtils.executeQuery(query3);
			MysqlConnUtils.executeQuery(query4);
			MysqlConnUtils.executeQuery(query5);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}
	
	public static void getFinancialsFromFSDataSets(String lineItemType[],
			int startYr, int endYr, int qtr, int endQ, boolean regenerate)
			throws SQLException, FileNotFoundException {

		if (regenerate) {
			startYr = 2009;

			if (regenerate) {
				// just drops/creates tables
				String query = "";
				for (int i = 0; i < lineItemType.length; i++) {
					System.out.println("lineItemType.len=" +lineItemType.length);
					
					query = "DROP PROCEDURE IF EXISTS xbrl_all_start_fsDataSet_"
							+ lineItemType[i+1]

							+ "_procedure;\r"
							+ "CREATE PROCEDURE xbrl_all_start_fsDataSet_"
							+ lineItemType[i+1]
							+ "_procedure()\r"
							+ "\r"
							+ "BEGIN\r"
							+ "\r"
							+ "DROP TABLE IF EXISTS xbrl_all_start_fsDataSet_"
							+ lineItemType[i+1]
							+ ";\r"
							+ "CREATE TABLE xbrl_all_start_fsDataSet_"
							+ lineItemType[i+1]
							+ " (\r"
							+ "  `accno` varchar(20) NOT NULL DEFAULT '',\r"
							+ "  `acceptedDate` datetime DEFAULT NULL,\r"
							+ "  `ITS` varchar(20) DEFAULT NULL,\r"
							+ "  `formType` varchar(20) DEFAULT '0',\r"
							+ "  `prefix` varchar(15) NOT NULL DEFAULT '',\r"
							+ "  `name` varchar(150) NOT NULL DEFAULT '',\r"
							+ "  `value` double NOT NULL DEFAULT 0,\r"
							+ "  `Period` int(4) NOT NULL DEFAULT 0,\r"
							+ "  `startDate` date NOT NULL DEFAULT '0000-00-00',\r"
							+ "  `endDate` date NOT NULL DEFAULT '0000-00-00',\r"
							+ "  `unitRef` varchar(25) DEFAULT NULL,\r"
							+ "  `ROLE_NAME` varchar(255) DEFAULT NULL,\r"
							+ "  `segment` double NOT NULL DEFAULT -9,\r"
							+ "  `dimension` varchar(150) NOT NULL DEFAULT '',\r"
							+ "  `dimensionValue` varchar(150) NOT NULL DEFAULT '',\r"
							+ "  PRIMARY KEY (ACCNO,ENDDATE,PERIOD,VALUE,NAME,SEGMENT),\r"
							+ "  KEY `acceptedDate` (`acceptedDate`),\r"
							+ "  KEY `ITS` (`ITS`),\r"
							+ "  KEY `Period` (`Period`),\r"
							+ "  KEY `endDate` (`endDate`),\r"
							+ "  KEY `startDate` (`startDate`),\r"
							+ "  KEY `ROLE_NAME` (`ROLE_NAME`),\r"
							+ "  KEY `prefix` (`prefix`),\r"
							+ "    KEY `accno` (`accno`)\r"
							+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r"
							+ "\r" + "\r" + "END;\r";

					
					MysqlConnUtils.executeQuery(query
							+ "\rcall xbrl_all_start_fsDataSet_"
							+ lineItemType[i+1] + "_procedure;\r");

			i++;
			i++;}
			}
		}

		String query = "";
		int cnt = 0, origStartYr = startYr, endQtr=1;
		for (int i = 0; i < lineItemType.length; i++) {
			startYr = origStartYr; cnt=0; endQtr =4;
			for (; startYr <= endYr; startYr++) {
				cnt++;
				if (startYr == 2009 && cnt == 1)
					qtr = 2;
				if(startYr==endYr)
					endQtr = endQ;
				for (; qtr <= endQtr; qtr++) {
					query = "insert ignore into xbrl_all_start_fsDataSet_"
							+ lineItemType[i + 1]
							+ ""
							+ "\rselect accno,accepteddate," +
							"\r case when co_reg!='' then concat('rel_to_',its) else its end its" +
							",form,ver,tag,value\r"
							+ ",qtrs*3,date_sub(date, interval 3 month) startdate,date_sub(date, interval 0 month) enddate\r"
							+ ",uom,stmt,0,0,0 \r"
							+ "from "
							+ startYr
							+ "q"
							+ qtr
							+ "fsdata_"
							+ lineItemType[i]
							+ " where "
							+ lineItemType[i + 2]
									.replaceAll("name ?=", "tag =") + ";";
					MysqlConnUtils.executeQuery(query);
				}
				qtr = 1;
			}
		i++;
		i++;}
	}
	
	public static void getCIKfromXBRL_ins_context(int qtr, int startYr, int endYr, boolean regenerate)
			throws SQLException, FileNotFoundException {
		
		System.out.println("startYr="+startYr+" endYr="+ endYr+" qtr="+qtr);
		int cnt = 0, yr=startYr;
		
		//don't need to regenerate necessary.
		
		if (regenerate) {
			MysqlConnUtils
					.executeQuery("call create_table_xbrl_accno_cik_and_its_tables_procedure();");
		}
		
		for (; yr <= endYr; yr++) {
			cnt++;
			if (yr == 2009 && cnt == 1)
				qtr = 2;
			for (; qtr <= 4; qtr++) {

				MysqlConnUtils
						.executeQuery("/*INSERT ALL CIK BASED ON CIK ASSOCIATED WITH THE INS_CONTEXT FILE. "
								+ "\rTHERE ARE VERY RARE CASES WHERE 1 IS NOT ASSOCIATED. \r"
								+ "IN THAT CASE I INSERT BASED ON NAME IN INS_DATA. \r"
								+ " ACCNO IS PRIMARY KEY*/"
								+ "\r\rDROP TABLE IF EXISTS TMP_"
								+ yr
								+ "Q"
								+ qtr
								+ "_XBRL_ACCNO_CIKs;\r"
								+ "CREATE TABLE TMP_"
								+ yr
								+ "Q"
								+ qtr
								+ "_XBRL_ACCNO_CIKs ENGINE=MYISAM\r"
								+ "select accepteddate,accno,cik from "
								+ yr
								+ "Q"
								+ qtr
								+ "xbrl_ins_context group by accno;\r"
								+ "INSERT IGNORE INTO XBRL_ACCNO_CIK\r"
								+ "SELECT * FROM TMP_"
								+ yr
								+ "Q"
								+ qtr
								+ "_XBRL_ACCNO_CIKs;\r\r"
								+ "INSERT IGNORE INTO XBRL_ACCNO_CIK\r"
								+ "select t1.accepteddate,t1.accno,t1.value from "
								+ yr
								+ "Q"
								+ qtr
								+ "xbrl_ins_data t1\r"
								+ "inner join "
								+ yr
								+ "Q"
								+ qtr
								+ "xbrl_ins_context t2 on t1.accno=t2.accno and t1.contextref=t2.id\r"
								+ "where name = 'entitycentralindexkey' order by segment,dimension;");				

			}
			qtr = 1;
		}
		

		MysqlConnUtils
				.executeQuery("UPDATE XBRL_FILER_INFO T1 INNER JOIN xbrl_accno_cik t2 \r"
						+ "on t1.accno=t2.accno\r"
						+ "SET T1.CIK= T2.CIK\r"
						+ "WHERE t1.cik!=t2.cik ");
		
		MysqlConnUtils.executeQuery("\r\n call confirm_xbrl_filer_info_its_procedure();\r\n");

	}

	public static void getSharesOutstanding (int yr, int qtr) throws SQLException, FileNotFoundException {

		int yr2 = yr - 1;

		int qtr2 = qtr - 1;
		if (qtr == 1)
			qtr2 = 4;

		String str = "DROP TABLE IF EXISTS XBRL_SHARES_OUTSTANDING;\r"
				+ "CREATE TABLE XBRL_SHARES_OUTSTANDING ENGINE=MYISAM \r"
				+ "select t1.*,t2.accepteddate,prefix,name,value,contextref,unitref from (select its, accno from \r"
				+ "\r" + "" + yr + "q" + qtr + "xbrl_pre_link t1 \r" + "\r" + "group by accno) t1\r" + "inner join \r"
				+ "\r" + "" + yr + "q" + qtr + "xbrl_ins_data t2 \r" + "\r" + "on t1.accno=t2.accno\r" + "where \r"
				+ "name rlike 'float|outs|publ' and name not rlike 'comp|excess'\r"
				+ "AND (unitref='shares' or unitref rlike '_shares$' ) AND VALUE>0\r" + 
				"\r" + "order by its,value desc;\r" + "\r"
				+ "insert ignore into XBRL_SHARES_OUTSTANDING\r"
				+ "select t1.*,t2.accepteddate,prefix,name,value,contextref,unitref from (select its, accno \r"
				+ "from \r" + "\r" + "" + yr2 + "q" + qtr2 + "xbrl_pre_link t1 \r" + "\r" + "group by accno) t1\r"
				+ "inner join \r" + "\r" + "" + yr2 + "q" + qtr2 + "xbrl_ins_data t2 \r" + "\r"
				+ "on t1.accno=t2.accno\r" + "where \r"
				+ "name rlike 'float|outs|publ' and name not rlike 'comp|excess'\r"
				+ "AND (unitref='shares' or unitref rlike '_shares$' ) AND VALUE>0\r\r"
				+ "order by its,value desc;\r\r\r"
				+ "ALTER TABLE XBRL_SHARES_OUTSTANDING \r" + "ADD KEY(ITS),ADD KEY(ACCNO),ADD KEY(CONTEXTREF),\r"
				+ "CHANGE VALUE VALUE BIGINT;\r";		
		
		
		MysqlConnUtils.executeQuery(str);
		
	}

	public static void main(String[] args) throws SQLException, IOException {
		
		

		// getFinnacials gets quarterly Sales,NI and CF from Ops (values and
		// rank). It iterates over each ary in list - ary1=Sales,NI and then
		// CF_ops.

		// int startYr = 2009, endYr = Calendar.getInstance().get(Calendar.YEAR), qtr =
		// ((Calendar
		// .getInstance().get(Calendar.MONTH) / 3) + 1);
		// boolean regenerate = false;

		FinancialStatement.getFinancials(2018, 2018, 4, false);

		/*
		 * getFinancialsFromFSDataSets - add to statements str ary - statement
		 * source (IS has sales and ni) of FSDataSet table - eg
		 * 2017Q3_FSDataSet_IS then pair w/ corresponding names to filter by
		 * (salesName or netIncNames) and goes into
		 * xbrl_all_start_fsDataset_sales. This mirrors exactly getFinancials
		 * salesNames,netIncNames,cf_opsNames. In case of net income data- get
		 * from 2017Q3_FSDataSet_IS (IS) source table but put in to
		 * xbrl_all_start_fsDataset_sales_ni (NI) and filter with netIncNames
		 * (netIncNames)
		 */
		
		//make this run on demand OR when new quarterly file available.
		String [] statements = {/*"IS","Sales",salesNames,*/
				"IS","NI",netIncNames,
				"CF","cf_ops", cf_opsNames};
//		int endQ = 4;//in last year last quarter available 
//		getFinancialsFromFSDataSets(statements,2017,2017,1,endQ,false);
		
//		FinancialStatement.getSharesOutstanding(2018, 1);
		
		//then insert all from xbrl_all_start_fsDataSet_sales -- into xbrl_all_start_sales
	
		// MysqlConnUtils
		// .executeQuery("/*RUN BEFORE getFinancialRanks*/CALL confirm_xbrl_filer_info_its_procedure;");
		// // 3
//		 getFinancialRanks();
		
		// TODO: regeneration using FSData sets is fast. Next step is now
		// that I have all data set in sales/NI/Cf xbrl_all_start_fs... - move
		// into xbrl_all_start . .. . and then run quarterly_ ... . Then see how
		// to inc into wk_regenerate. ADD BELOW INSERT WHENEVER I REGENERATE
		// EITHER getFinancials (xbrl of FS) AND whenever I get new FS Quarter.
		// Input appropriate place <<<<========TODO=========>>:
		// INSERT IGNORE INTO xbrl_all_start_sales
		// select * from xbrl_all_start_fsDataSet_sales ;
		// INSERT IGNORE INTO xbrl_all_start_NI
		// select * from xbrl_all_start_fsDataSet_NI ;
		// INSERT IGNORE INTO xbrl_all_start_CF_OPs
		// select * from xbrl_all_start_fsDataSet_CF_OPS ;
		
	}
}
