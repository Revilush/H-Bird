package xbrl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ParseCsiPortfolios {

	public static void importAdjustedCsiData(String[] sAry) throws IOException, SQLException {

		/*
		 * grabs from csi data folders each tickers csv data history and imports it into
		 * the csi_ table in mysql.
		 */

		// NOTE: DON'T USE THIS IF CSI IS NOT UPDATING EACH TICKER IN EACH OF THE CSI
		// DATA PORTFOLIO DIRECTORIES - THIS UPDATES MYSQL DATA BY IMPORTING EACH TICKER
		// FILE

		String yrMoBacup = Calendar.getInstance().get(Calendar.YEAR) + "_"
				+ (Calendar.getInstance().get(Calendar.MONTH) + 1);

		for (int i = 0; i < sAry.length; i++) {
			if (sAry[i].toLowerCase().contains("unadjusted"))
				continue;

			MysqlConnUtils.executeQuery("DROP TABLE IF EXISTS BAC_" + yrMoBacup + "_csi_" + sAry[i] + ";\r"
					+ "CREATE TABLE BAC_" + yrMoBacup + "_csi_" + sAry[i] + "\rSELECT * FROM csi_" + sAry[i] + ";\r");

			MysqlConnUtils.executeQuery("\r" + "call create_table_csi_" + sAry[i] + "_procedure();\r");

//			if (sAry[i].contains("index")) {
//				MysqlConnUtils.executeQuery("call HISTORICAL_TREASURY_YIELDS_INTO_CSI_DATA();");
//			}

//			StringBuilder sb = new StringBuilder();

			File folder = new File("c:/backtest/csidata/" + sAry[i]);
			File[] listOfFiles = folder.listFiles();

			System.out.println("folder=" + folder);
			if (listOfFiles == null)
				continue;
			
//			System.out.println("file.len=" + sb.toString().length());
			File f = new File("c:/backtest/export/csi_" + sAry[i] + ".txt");
			if(f.exists())
				f.delete();
			
			PrintWriter pw = new PrintWriter(f);

			for (File file : listOfFiles) {
				pw.append(Utils.readTextFromFile(file.getAbsolutePath()).replaceAll("[ ]+", ""));
			}
			pw.close();

//			System.out.println("file.len=" + sb.toString().length());
//			PrintWriter pw = new PrintWriter(new File("c:/backtest/export/csi_" + sAry[i] + ".txt"));
//			pw.append(sb.toString().replaceAll("[ ]+", ""));
//			pw.close();
//			sb.delete(0, sb.toString().length());

			String query = "LOAD DATA INFILE 'c:/backtest/export/csi_" + sAry[i] + ".txt'" + "\r ignore INTO TABLE csi_"
					+ sAry[i] + "\r" + "FIELDS TERMINATED BY ','\r" + "LINES TERMINATED BY '\r\n'\r"
					+ "(its,date,open,high,low,close,volume,v2);";

			MysqlConnUtils.executeQuery(query);
		}
	}

	public static void importUnadjustedCsiData(String[] sAry) throws SQLException, IOException {

		String yrMoBacup = Calendar.getInstance().get(Calendar.YEAR) + "_"
				+ (Calendar.getInstance().get(Calendar.MONTH) + 1);

		for (int i = 0; i < sAry.length; i++) {

			if (!sAry[i].toLowerCase().contains("unadjusted"))
				continue;

			MysqlConnUtils.executeQuery("DROP TABLE IF EXISTS BAC_" + yrMoBacup + "_csi_" + sAry[i] + ";\r"
					+ "CREATE TABLE BAC_" + yrMoBacup + "_csi_" + sAry[i] + 
					"\rSELECT * FROM csi_" + sAry[i] + ";\r");

			File folder = new File("c:/backtest/csidata/" + sAry[i] + "/");
			File[] listOfFiles = folder.listFiles();

			System.out.println("folder=" + folder);
			if (listOfFiles == null)
				continue;

			StringBuilder sb = new StringBuilder();

			File file = new File("c:/backtest/export/" + sAry[i] + ".txt");
			if (file.exists())
				file.delete();

			PrintWriter pw = new PrintWriter(file);
			for (File f : listOfFiles) {
				pw.append(Utils.readTextFromFile(f.getAbsolutePath()).replaceAll("[ ]+", "") + "\r\n");
				// sb.append(Utils.readTextFromFile(file.getAbsolutePath()) + "\r\n");
			}

//			System.out.println("file.len=" + sb.toString().length());
//			File file = new File("c:/backtest/export/" + sAry[i] + ".txt");
//			if (file.exists())
//				file.delete();
//			PrintWriter pw = new PrintWriter(file);
//			String text = sb.toString().replaceAll("[ ]+", "");
//			pw.append(text.substring(0, text.length() - 2));
			pw.close();
//			sb.delete(0, sb.toString().length());

			// TODO: don't truncate - chg to drop/create table
			String query = "call create_table_csi_" + sAry[i] + "_procedure();\r"
					+ "LOAD DATA INFILE 'c:/backtest/export/" + sAry[i] + ".txt'\r" + " ignore INTO TABLE csi_"
					+ sAry[i] + " \r" + "FIELDS TERMINATED BY ','\r" + "LINES TERMINATED BY '\r\n'\r"
					+ "(its,date,@o,@h,@l,@c,@v,@v2,@unadjClose)\r" + "SET open=@unadjClose/@c*@o, \r"
					+ "high=@unadjClose/@c*@h,\r" + "low=@unadjClose/@c*@l,\r" + "close=@unadjClose,\r"
					+ "unadjustedClose = @unadjClose,\r" + "volume=@v,\r" + "v2=@v2;\r";

			MysqlConnUtils.executeQuery(query);

		}
	}

	public static void getCsiDividends(String[] sAry) throws SQLException, FileNotFoundException {

		StringBuilder sb = new StringBuilder();

		List<String[]> listTables = new ArrayList<String[]>();

		// align each csi unadjusted and adj tables into a single array for each item in
		// list
		for (int i = 0; i < sAry.length; i++) {
			String[] ary = { sAry[i], sAry[i + 1] };
			listTables.add(ary);
			i++;
		}

		// runs once to get tmp tables used in each of sAry
		sb.append("DROP PROCEDURE IF EXISTS tmp_getCsiDivAndInsertIntoStockDividendTable_Procedure; \r"
				+ "CREATE PROCEDURE tmp_getCsiDivAndInsertIntoStockDividendTable_Procedure()\r" + "\r" + "BEGIN\r"
				+ "/*this procedure retrieves the dividend value based on the adjusted and unadusted stock prices exported from \r"
				+ "CSI in each case not adjusted for stocksplit. These are 2 separate portfios - 1 adjusted for div and the other not. \r"
				+ "After the dividends are retrieves I make sure they do not duplicate any in stockdividend and \r"
				+ "I add the those that do not. I only need to retrieve CEFs, ETFs, preferred, warrants from csi \r"
				+ "b/c rest of dividends are on nasdaq.com*/\r" + "\r" + "DROP TABLE IF EXISTS TMP_CSI_DIVIDENDS;\r"
				+ "CREATE TABLE `tmp_csi_dividends` (\r" + "  `divd` double(12,5) DEFAULT NULL,\r"
				+ "  `dif` int(7) DEFAULT NULL,\r" + "  `its` varchar(15) NOT NULL DEFAULT '',\r"
				+ "  `date` date NOT NULL DEFAULT '0000-00-00',\r" + "  `adjClose` double(15,3) NOT NULL DEFAULT '0',\r"
				+ "  `unadjClose` double(15,3) NOT NULL DEFAULT '0',\r" + "  key(date),\r" + "  key(its),\r"
				+ "  key(divd)\r" + ") ENGINE=myisam DEFAULT CHARSET=latin1;\r" + "\r\rend;\r\r"
				+ "call tmp_getCsiDivAndInsertIntoStockDividendTable_Procedure();\r\r"
				+ "DROP PROCEDURE IF EXISTS tmp_getCsiDivAndInsertIntoStockDividendTable_Procedure;\r\r");

		for (int i = 0; i < listTables.size(); i++) {
			sb.append("DROP PROCEDURE IF EXISTS tmp2_getCsiDivAndInsertIntoStockDividendTable_Procedure; \r"
					+ "CREATE PROCEDURE tmp2_getCsiDivAndInsertIntoStockDividendTable_Procedure()\r" + "\r" + "begin"
					+ "\r\r/*join csi price data that is unadjusted for dividends and splits "
					+ "with table that has price adjusted for dividends but not for splits*/\r\r"
					+ "set @its='1x'; set @d=0; set @div = 0;\r" + "insert ignore into tmp_csi_dividends\r"
					+ "select \r" + "@div:=case when t1.its!=@its then 0 when @its=t1.its and "
					+ "@d!= round((unadjClose-adjClose),6) "
					+ "then round((unadjClose-adjClose),6) - @d else 0 end divd,\r"
					+ "@d:=case when @its=its and round(adjClose,6)+@d!=round((unadjClose+@d),6) "
					+ "then round((unadjClose-adjClose),6) when @its!=its then 0 else @d end dif,\r"
					+ "@its:=its its,date,adjClose, unadjClose\r" + "from (\r" + "\r"
					+ "select t1.its,t1.date,t1.close adjClose,t2.close unadjClose\r" + "from " + "\r" + "csi_"
					+ listTables.get(i)[1] + "  t1 inner join csi_" + listTables.get(i)[0]
					+ " t2 on t1.its=t2.its and t1.date=t2.date\r" + " order by t1.its,t1.date desc ) t1;\r" + "end;\r"
					+ "\r" + "call tmp2_getCsiDivAndInsertIntoStockDividendTable_Procedure();\r\r\r"
					+ "DROP PROCEDURE IF EXISTS tmp2_getCsiDivAndInsertIntoStockDividendTable_Procedure;\r\r");
		}

		sb.append("DROP PROCEDURE IF EXISTS tmp3_getCsiDivAndInsertIntoStockDividendTable_Procedure; \r"
				+ "CREATE PROCEDURE tmp3_getCsiDivAndInsertIntoStockDividendTable_Procedure()\r" + "\r\rBEGIN"
				+ "/*find duplicates before adding csi dividends. Generally i have all dividends from nasdaq.com other than CEFs, ETFs and Preferred stock.*/"
				+ "\r" + "DROP TABLE IF EXISTS TMP_STOCKDIVIDENDS ;\r"
				+ "CREATE TABLE TMP_STOCKDIVIDENDS ENGINE=MYISAM\r"
				+ "select ITS,EXDIVDT DATE,dividend,'sd' type from stockdividend \r" + "where its !=''\r"
				+ "order by its,date;\r\r\r" + "" + "insert ignore into TMP_STOCKDIVIDENDS \r"
				+ "select replace(its,'+','-') its,date,divd,'cs' from TMP_CSI_DIVIDENDS t1\r" + "where divd!=0;\r"
				+ "alter table tmp_stockdividends add key (date), add key(its),add key(its,date);\r" + "\r"
				+ "set @its='1x'; set @dt='1901-01-01'; set @div = 0.0;\r"
				+ "DROP TABLE IF EXISTS TMP_STOCKDIVIDENDS2 ;\r" + "CREATE TABLE TMP_STOCKDIVIDENDS2 ENGINE=MYISAM\r"
				+ "select type,\r" + "case when @its=its and (datediff(GREATEST(@dt,date),LEAST(@dt,date))<5\r"
				+ "or (abs(@div-dividend)<.0125 and datediff(GREATEST(@dt,date),LEAST(@dt,date))<9 ))\r"
				+ "then 1 else 0 end ck,\r"
				+ "@its:=its its,@dt:=date date,@div:=dividend dividend  from TMP_STOCKDIVIDENDS T1 \r"
				+ "order by its,date desc;\r"
				+ "alter table TMP_STOCKDIVIDENDS2 change ck ck tinyint(2), add key (ck), change type type varchar(3), add key (type);\r"
				+ "/*FIND WHERE DIF IS NEGATIVE (ROUNDING ERRORS IN COMPARISON OF CSI_UNADJ AND CSI_ADJ - AND THE MAX ROUNDING ERROR SETS OUTSIDE BOUNDARY \r"
				+ "OF WHAT TO EXCLUDE IN NEXT QUERY*/\r" + "DROP TABLE IF EXISTS TMP_STOCKDIVIDENDS3;\r"
				+ "CREATE TABLE TMP_STOCKDIVIDENDS3 ENGINE=MYISAM\r"
				+ "select max(abs(dividend)) divErr,ITS,CK,TYPE from TMP_STOCKDIVIDENDS2 where ck!=1  and type!='sd'\r"
				+ "AND dividend<0 group by its;\r"
				+ "alter table TMP_STOCKDIVIDENDS3 add primary key (its),change ck ck tinyint(2), add key (ck), change type type varchar(3), add key (type);\r"
				+ "\r" + "INSERT IGNORE INTO TMP_STOCKDIVIDENDS3\r"
				+ "SELECT 0,ITS,0,'cs' from TMP_STOCKDIVIDENDS2 group by its;\r" + "\r"
				+ "/*if type is sd - it already is in - if it is 1 - it is a duplicate*/\r"
				+ "INSERT IGNORE INTO STOCKDIVIDEND\r"
				+ "select T1.DATE,T1.DIVIDEND,T1.ITS,'','','','' from TMP_STOCKDIVIDENDS2 T1\r"
				+ "INNER JOIN TMP_STOCKDIVIDENDS3 T2 ON T1.ITS=T2.ITS\r" + "where T1.ck!=1 and T1.type!='sd'\r"
				+ "AND (T1.DIVIDEND>1.25*diverr or divErr=0)\r" + "and dividend>.01;\r" + "\r" + "\r"
				+ "set @dt='1901-01-01'; set @its='1x'; set @div=0; set @type='any';\r"
				+ "drop table if exists tmp_fix;\r" + "create table tmp_fix engine=myisam\r" + "select case when \r"
				+ "@its=its and (@type = 'cash' or type = 'cash') and \r"
				+ "datediff(greatest(@dt,exdivdt),least(@dt,exdivdt))<8   \r" + "/*and abs(@div-dividend) <.012 and \r"
				+ "month(exdivdt)!=12 and month(@dt)!=12 and month(exdivdt)!=1 and month(@dt)!=1 */\r"
				+ "then 1 else 0 end ck,\r" + "@dt:=exdivdt exdivdt, @its:=its its, @div:=dividend dividend\r"
				+ ",@type:=type type\r" + "from stockdividend order by its,exdivdt;\r"
				+ "alter table tmp_fix add key(ck),add key(its), add key(exdivdt);\r" + "\r"
				+ "delete t1 from stockdividend t1, tmp_fix t2 where t1.its=t2.its and t1.exDivDt=t2.exdivdt and t2.ck=1;\r"
				+ "\rend;\r\r" + "\r" + "\r" + "call tmp3_getCsiDivAndInsertIntoStockDividendTable_Procedure();\r\r\r"
				+ "DROP PROCEDURE IF EXISTS tmp3_getCsiDivAndInsertIntoStockDividendTable_Procedure;\r\r");

		MysqlConnUtils.executeQuery(sb.toString());

	}

	public static void getCsiSplits(String[] sAry) throws SQLException, FileNotFoundException {

		for (int i = 0; i < sAry.length; i++) {

			if ((sAry[i].toLowerCase().contains("sp500") || sAry[i].toLowerCase().contains("russel"))
					&& sAry[i].toLowerCase().contains("unadjusted")) {

				StringBuilder sb = new StringBuilder(
						"set @dt = '1901-01-01'; set @sp = 0.0; set @its = '1x'; set @r =0; set @s = 0.0; SET @remain=0.00; SET @remain2=0.00;\r"
								+ "insert ignore into stocksplit\r"
								+ "select its,prevDate date,split num,1 den from (\r"
								+ "select @r:=case when @its!=its then 1 else @r+1 end r,\r"
								+ "@s:=case when @its!=its then round((volume/v2),2) else round((volume/v2)/@sp,2) end sp1,\r"
								+ "@remain:=right(@s,3) rmn,\r" + "@remain2:=substring_index(@s,'.',1) rmn2,\r"
								+ "case \r"
								+ "when abs(@remain-0.00) between 0.01 and 0.02 then concat(@remain2,'.00') \r"
								+ "when abs(@remain-0.10) between 0.01 and 0.02 then concat(@remain2,'.10')\r"
								+ "when abs(@remain-0.20) between 0.01 and 0.02 then concat(@remain2,'.20')\r"
								+ "when abs(@remain-0.25) between 0.01 and 0.02 then concat(@remain2,'.25')\r"
								+ "when abs(@remain-0.30) between 0.01 and 0.02 then concat(@remain2,'.30')\r"
								+ "when abs(@remain-0.33) between 0.01 and 0.02 then concat(@remain2,'.33')\r"
								+ "when abs(@remain-0.40) between 0.01 and 0.02 then concat(@remain2,'.40')\r"
								+ "when abs(@remain-0.50) between 0.01 and 0.02 then concat(@remain2,'.50')\r"
								+ "when abs(@remain-0.60) between 0.01 and 0.02 then concat(@remain2,'.60')\r"
								+ "when abs(@remain-0.67) between 0.01 and 0.02 then concat(@remain2,'.67')\r"
								+ "when abs(@remain-0.70) between 0.01 and 0.02 then concat(@remain2,'.70')\r"
								+ "when abs(@remain-0.80) between 0.01 and 0.02 then concat(@remain2,'.80')\r"
								+ "when abs(@remain-0.90) between 0.01 and 0.02 then concat(@remain2,'.90')\r"
								+ "else @s \r" + "end split\r" + ",@sp:=round((volume/v2),2) sp\r"
								+ ",@its:=its its,@dt prevDate,@dt:=date date,open,high,low,close,volume,v2,unadjustedClose\r"
								+ "from csi_" + sAry[i] + " t1 \r" + "\r" + "order by its,date \r" + "desc \r" + "\r"
								+ ") t1 where split!=1;\r");

				MysqlConnUtils.executeQuery(sb.toString());

			}
		}
	}
	
	public static void getStockdataPropietaryUnadjusted(String [] sAry, String startDate) throws SQLException, FileNotFoundException {

		MysqlConnUtils.executeQuery("CALL csi_stockdata_unadjusted_create_table();");

		for (int i = 0; i < sAry.length; i++) {
			if (sAry[i].toLowerCase().contains("unadjusted")) {
			
		StringBuilder sb = new StringBuilder(
				"call TMP_csi_STOCKDATA_SPLIT_AND_DIV_create_table();\r"
				+ "set @endDate = (select max(date) from csi_"+sAry[i]+");\r"
				+"set @startDate = "+startDate+";\r"
				+"\r"
				+"/*regenerates in this order --->csi_STOCKDATA_UNADJUSTED, wk_bp, wk_calc.\r"
				+"Using closing price or adjClose distorts price movements - this show return assuming \r"
				+"investment of 100% of cash dividend. I liimit to symbols of less than 5 characters\r"
				+"\r"
				+"See 'formula' below. EXPLANATION: The non-adjusted closing price (the original closing price) is adjusted by all splits that occur after it (this is necessary to develop a closing price up to present original closing price). e.g., if I own 1 share of aapl stock - and I receive 2:1 (2) - I now have 2 for all dates prior to that split if there's also a 3:1 after the start date - I now have 6 shares. In order to determine historical price I I multiply the then closing price by this 'multiplier' which if a 2:1 and 3:1 split occur after the closing price date is 6 (2x3). So if date is 2/1/2014 and splits were 3/1/2015 and 5/1/2017 then for purposes of closing price I multiply by 6 shares - so I adjust from dates prior to  2/1/2014 by 6 and at 3/1/2015 by 2 and after 5/1/2017 by 1. This creates a new price that is unadjusted to current using this unqiue method. In addition on each dividend date I can buy additional shares based on the then original closing price - based on the unadjusted dividend paid on that date - provided the dividend is not split adjusted by the is unadjusted so that I can buy at the unadjusted close on that date. So if closing price is 50 and dividend is 1 - I have increased split multiplier by 1/50 or (1+1/50) - e.g., (1/50+1)*6. This multiplier then reflects the reinvested dividends and split. I checked this manually by accounting for dividends and splits since my purchase and this method calculates EXACTLY the gain/loss when reinvesting for dividends!*/\r"
				+"\r"
				+"DROP TABLE IF EXISTS stocksplits_dividends;\r"
				+"\r"
				+"CREATE TABLE stocksplits_dividends ENGINE=MYISAM\r"
				+"select T1.*, 0.000000000000000000 DIVIDEND \r"
				+"\r"
				+"FROM STOCKSPLIT T1\r"
				+"\r"
				+"WHERE\r"
				+"\r"
				+"t1.DATE between @startDate and @endDate ;\r"
				+"\r"
				+"ALTER TABLE stocksplits_dividends CHANGE DIVIDEND DIVIDEND DOUBLE, CHANGE DATE DATE DATE, CHANGE ITS ITS VARCHAR(10)\r"
				+", CHANGE NUM NUM DOUBLE, CHANGE DEN DEN DOUBLE;\r"
				+"\r"
				+"/*insert all stockdividends*/\r"
				+"INSERT IGNORE INTO stocksplits_dividends\r"
				+"SELECT T1.EXDIVDT,T1.ITS,0,0,DIVIDEND\r"
				+"FROM STOCKDIVIDEND T1\r"
				+"\r"
				+"WHERE\r"
				+"\r"
				+"t1.EXDIVDT between @startDate and @endDate;\r"
				+"\r"
				+"ALTER TABLE stocksplits_dividends ADD KEY(ITS,DATE), ADD KEY(ITS), ADD KEY(DATE),ADD KEY(DATE,ITS);\r"
				+"\r"
				+"\r"
				+"\r"
				+"insert ignore into tmp_csi_stockdata_split_and_div\r"
				+"SELECT \r"
				+"case when num=0 then 1 else ROUND((NUM/DEN),6) end SPLIT\r"
				+", case when dividend=0 then 1 else ROUND(dividend,6) end DIVIDEND\r"
				+",LEFT(T1.DATE,10) DATE,LEFT(T1.ITS,10) ITS,0.0000 OPEN,0.0000 HIGH,0.0000 LOW,0.0000 CLOSE,0 VOLUME\r"
				+"FROM csi_"+sAry[i]+" T1 INNER JOIN csi_stocksplits_dividends T2 ON T1.ITS=T2.ITS AND T1.DATE=T2.DATE\r"
				+"WHERE t1.DATE between @startDate and @endDate ;\r"
				+"\r"
				+"INSERT IGNORE INTO TMP_csi_STOCKDATA_SPLIT_AND_DIV\r"
				+"SELECT 1,1,LEFT(T1.DATE,10) DATE,T1.ITS\r"
				+",T1.OPEN,T1.HIGH,T1.LOW,T1.CLOSE close,T1.VOLUME\r"
				+"FROM csi_"+sAry[i]+" T1 \r"
				+"WHERE T1.DATE BETWEEN @startDate and @endDate;\r"
				+"\r"
				+"set @m=1.000000; set @its='1x'; set @unAdjClose = 0.0000;set @s=1.00000;\r"
				+"/*this is the formula*/\r"
				+"\r"
				+"insert ignore into tmp_csi_stockdata_unadjusted\r"
				+"select \r"
				+"case when open=0 and high=0 and low=0 and volume = 0 and close = 0 then 1 else 0 end ck,\r"
				+"@m:=case \r"
				+"when its=@its and split!=1 and dividend=1 then round((split*@m),6)\r"
				+"when its=@its and split=1 and dividend!=1 then round((((@m*dividend)/@unAdjClose)+@m ),6)\r"
				+"when its=@its and split!=1 and dividend!=1 then round((split*(((@m*dividend)/@unAdjClose)+@m)),6)\r"
				+"when its=@its and split=1 and dividend=1 then @m\r"
				+"when its!=@its and split!=1 and dividend=1 then round((split*@m),6)\r"
				+"when its!=@its and split=1 and dividend!=1 then round((((@m*dividend)/@unAdjClose)+@m ),6)\r"
				+"when its!=@its and split!=1 and dividend!=1 then round(split*(((@m*dividend)/@unAdjClose)+@m),6)\r"
				+"when its!=@its and split=1 and dividend =1 then 1\r"
				+"else round(@m,6) end Multiplier\r"
				+",@s:=case when @its!=its then round(split,2) when split!=1 and @its=its then ROUND(@s*split,6) else round(@s,6) end s\r"
				+",date,@its:=its its,round((@m*open),4) open,round((@m*high),4) high,round((@m*low),4) low,round((@m*close),4) cl, round(volume/@s) volume\r"
				+",@unAdjClose:=case when close=0 then @unAdjClose else close end unAdjClose\r"
				+",split,dividend from TMP_csi_STOCKDATA_SPLIT_AND_DIV t1 \r"
				+"ORDER BY ITS,DATE,close; \r"
				+"/*<---need to include close in order by to align split multiplier to correct date.*/\r"
				+"\r"
				+"insert ignore into csi_STOCKDATA_UNADJUSTED\r"
				+"select multiplier,date,its,open,high,low,close,volume,origClose from tmp_csi_stockdata_unadjusted t1 where ck!=1;\r"
				+"\r"
				+"DROP TABLE IF EXISTS tmp_csi_stockdata_unadjusted; \r"
				+"DROP TABLE IF EXISTS TMP_CSI_STOCKDATA_SPLIT_AND_DIV; \r"
				+"DROP TABLE IF EXISTS stocksplits_dividends;\r");
		
			MysqlConnUtils.executeQuery(sb.toString());
			
			}
		}
	}

	public static void main(String[] args) throws IOException, SQLException {

		String[] sAry = { /*"bonds_unadjusted", "bonds_adjDivUnAdjSplit", "indexes_unadjusted",
				"indexes_adjDivUnAdjSplit", "preferred_unadjusted", "preferred_adjDivUnAdjSplit", "ETFs_unadjusted",
				"ETFs_adjDivUnAdjSplit", "CEFs_unadjusted", "CEFs_adjDivUnAdjSplit", "sp500_unadjusted",
				"sp500_adjDivUnAdjSplit", */"russell2000_unadjusted", "russell2000_adjDivUnAdjSplit" };
		ParseCsiPortfolios.importUnadjustedCsiData(sAry);
		ParseCsiPortfolios.importAdjustedCsiData(sAry);
		
	}
}
