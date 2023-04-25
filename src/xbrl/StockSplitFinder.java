package xbrl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

public class StockSplitFinder {

	public static void StockSplitFind(String year, String year2) throws SQLException, FileNotFoundException {

		String query = "/*Due to source data from vendors the stocksplit table is incomplete. The below is much better and finds where splits occurred. Below method will miss small splits (11:10 for example). Or where BK Mellon merger which was 994 to 1000. Use 8-Ks to grab stuff in future.  Only need to run once - then grab since last time.   DO THE BELOW in yearly segments - execute in loop in java.  GRAB BACKED-UP stocksplit table - and regenerate.*/\r"
				+ "\r" + "SET @sDate='" + year + "-12-01'; SET @eDate = '" + year2 + "-12-31';\r"
				+ "SET @prevClose = 0.0; SET @prevITS = '1x'; SET @prevDate = '1901-01-01';\r"
				+ "SET @startDate = '1901-01-01'; set @v5=0; set @v4=0; set @v3=0; set @v2=0; set @prevVolume = 0;\r"
				+ "\r"
				+ "/*search for stocksplits based on change from prior day's close to open of current date provided the date dif is not more than a week. I also measure the day's vol per chg on the day stock trades at split, the range of the stock price on the split date and the dollar volue. All 3 are used to filter out potential false positives. */\r"
				+ "\r" + "DROP TABLE IF EXISTS splitList;\r" + "CREATE TABLE splitList engine=MYISAM\r" + "\r"
				+ "SELECT\r" + "dayofweek(date) dy\r"
				+ ", @dyDiff:=case when dayofweek(@prevDate)<6 then dayofweek(date)-dayofweek(@prevDate) else dayofweek(date)+5-dayofweek(@prevDate) end dyDiff\r"
				+ ",@split:=case \r" + "\r"
				+ "when round(((@prevClose+.01)/(open+.01)),3) between 18 and 22 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose>20 THEN 20 \r"
				+ "WHEN round(((@prevClose+.01)/(open+.01)),3) between 16.01 and 17.9 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose>20 THEN 17 \r"
				+ "WHEN round(((@prevClose+.01)/(open+.01)),3) between 14 and 16 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose>20 THEN 15 \r"
				+ "WHEN round(((@prevClose+.01)/(open+.01)),3) between 11.56 and 13.9 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose>20 THEN 12\r"
				+ "WHEN round(((@prevClose+.01)/(open+.01)),3) between 10.45 and 11.55 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose>20 THEN 11 \r"
				+ "WHEN round(((@prevClose+.01)/(open+.01)),3) between 9.5 and 10.5 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose>20 THEN 10 \r"
				+ "WHEN round(((@prevClose+.01)/(open+.01)),3) between 8.55 and 9.55 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose>20 THEN 9\r"
				+ "WHEN round(((@prevClose+.01)/(open+.01)),3) between 7.6 and 8.4 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose>20 THEN 8 \r"
				+ "WHEN round(((@prevClose+.01)/(open+.01)),3) between 6.65 and 7.35 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose>20 THEN 7 \r"
				+ "WHEN round(((@prevClose+.01)/(open+.01)),3) between 5.7 and 6.3 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose>20 THEN 6 \r"
				+ "WHEN round(((@prevClose+.01)/(open+.01)),3) between 4.5 and 5.5 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose>20 THEN 5 \r"
				+ "WHEN round(((@prevClose+.01)/(open+.01)),3) between 3.6 and 4.4 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose>20 THEN 4 \r"
				+ "WHEN round(((@prevClose+.01)/(open+.01)),3) between 2.7 and 3.3 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose>20 THEN 3 \r"
				+ "WHEN round(((@prevClose+.01)/(open+.01)),3) between 1.8 and 2.2 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose>20 THEN 2 \r"
				+ "WHEN round(((@prevClose+.01)/(open+.01)),3) between 2.3 and 2.7 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose>20 THEN 2.5\r"
				+ "WHEN round(((@prevClose+.01)/(open+.01)),3) between 1.55 and 1.78 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose>20 THEN 1.67\r"
				+ "WHEN round(((@prevClose+.01)/(open+.01)),3) between 1.35 and 1.55 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose>20 THEN 1.5\r"
				+ "\r" + "\r"
				+ "WHEN round(((open/(@prevClose+.01)+.01)),3) between 10.45 and 11.55 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose<15 THEN .091 /*1:11 */\r"
				+ "WHEN round(((open/(@prevClose+.01)+.01)),3) between 9.5 and 10.5 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose<15 THEN .1  /*1:10*/\r"
				+ "WHEN round(((open/(@prevClose+.01)+.01)),3) between 8.55 and 9.55 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose<15 THEN .111 /*1:9*/\r"
				+ "WHEN round(((open/(@prevClose+.01)+.01)),3) between 7.6 and 8.4 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose<15 THEN .125  /*1:8*/\r"
				+ "WHEN round(((open/(@prevClose+.01)+.01)),3) between 6.65 and 7.35 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose<15 THEN .143 /*1:7 */\r"
				+ "WHEN round(((open/(@prevClose+.01)+.01)),3) between 5.7 and 6.3 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose<15 THEN .167 /*1:6*/\r"
				+ "WHEN round(((open/(@prevClose+.01)+.01)),3) between 4.5 and 5.5 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose<15 THEN .2 /*1:5*/\r"
				+ "WHEN round(((open/(@prevClose+.01)+.01)),3) between 3.6 and 4.4 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose<15 THEN .25 /*1:4*/\r"
				+ "WHEN round(((open/(@prevClose+.01)+.01)),3) between 2.7 and 3.3 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose<15 THEN .33 /*1:3*/\r"
				+ "WHEN round(((open/(@prevClose+.01)+.01)),3) between 1.8 and 2.2 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose<15 THEN .5 /*1:2*/\r"
				+ "WHEN round(((open/(@prevClose+.01)+.01)),3) between 2.3 and 2.7 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose<15 THEN .4 /*.4*/\r"
				+ "WHEN round(((open/(@prevClose+.01)+.01)),3) between 1.55 and 1.78 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose<15 THEN .6 /*3:5*/\r"
				+ "WHEN round(((open/(@prevClose+.01)+.01)),3) between 1.35 and 1.55 and @prevITS=its and @dyDiff<3 and @dyDiff>0 AND @prevClose<15 THEN .67 /*2:3*/\r"
				+ "else 1 end split\r" + ",round((@prevVolume+@v2+@v3+@v4+@v5+.01)/5*@prevClose) DAVE\r"
				+ ",(round(abs(high-low)/(low*.5+high*.5),2))*100 RngPChg\r"
				+ ",@prevDate prevDate,@prevDate:=date date,open,high,low\r"
				+ ",CASE WHEN @sp>0 THEN round(round(((volume/@split+.01)/((@prevVolume+@v2+@v3+@v4+@v5+.01)/5)),3) *100)\r"
				+ "ELSE round(round(((volume)/((@prevVolume+@v2+@v3+@v4+@v5+.01)/5)),3) *100) END VolPerChg, round(@prevClose,4) prevClose, @prevClose:=close close\r"
				+ ",@v5:=@v4 v5,@v4:=@v3 v4,@v3:=@v2 v3,@v2:=@prevVolume v2,@prevVolume prevVol,@prevVolume:=volume volume\r"
				+ ",@prevITS:=its its\r" + "FROM STOCKDATA WHERE \r" + "date between @sDate and @eDate\r"
				+ "order by its,date;\r" + "ALTER TABLE SPLITLIST ADD KEY (SPLIT);\r" + "\r"
				+ "/*grab just instances of when there is calculated split*/\r" + "DROP TABLE IF EXISTS splitCheck;\r"
				+ "CREATE TABLE splitCheck engine=myisam\r"
				+ "Select prevDate,date,its,split,round(open,4) open,round(high,4) high,round(low,4) low,round(close,4) close\r"
				+ ",round(prevClose,4) prevClose ,volume,volPerChg,DAVE,RngPChg from splitList \r"
				+ "where split!=1 ;\r" + "ALTER TABLE SPLITCHECK CHANGE SPLIT SPLIT DOUBLE;\r" + "\r" + "\r"
				+ "/*formats stocksplit to den=1 and num=2,3,4 etcc or den=1 and num=10,5,3 etc*/\r"
				+ "DROP TABLE IF EXISTS TMP_STOCKSPLIT;\r" + "CREATE TABLE TMP_STOCKSPLIT ENGINE=MYISAM\r"
				+ "SELECT CLOSE,\r" + "\r"
				+ "CASE WHEN SPLIT >0 THEN LEFT(DATE,10) ELSE LEFT (PREVDATE,10) END DATE,ITS,\r" + "\r"
				+ "/*CASE WHEN FORMATTED SO DEN IS 1 OR IF REVERSE SPLIT NUM IS 1. */\r" + "\r" + "CASE \r"
				+ "WHen SPLIT = 20 THEN 20\r" + "WHen SPLIT = 17 THEN 17\r" + "WHen SPLIT = 15 THEN 15\r"
				+ "WHen SPLIT = 12 THEN 12\r" + "WHen SPLIT = 10 THEN 10\r" + "WHen SPLIT = 9 THEN 9\r"
				+ "WHen SPLIT = 8 THEN 8\r" + "WHen SPLIT = 7 THEN 7\r" + "WHen SPLIT = 6 THEN 6\r"
				+ "WHen SPLIT = 5 THEN 5\r" + "WHen SPLIT = 4 THEN 4\r" + "WHen SPLIT = 3 THEN 3\r"
				+ "WHen SPLIT = 2 THEN 2\r" + "WHen SPLIT = 2.5 THEN 2.5\r" + "WHen SPLIT = 1.67 THEN 1.67\r"
				+ "WHen SPLIT = 1.5 THEN 1.5\r" + "WHen SPLIT = 1.2 THEN 1.2\r" + "ELSE 1 END NUM\r" + "\r" + ",CASE \r"
				+ "WHEN  split =  0.091 /*1:11 */  THEN 11\r" + "WHEN  split =  0.1 /*1:10*/  THEN 10\r"
				+ "WHEN  split =  0.111 /*1:9*/  THEN 9\r" + "WHEN  split =  0.125 /*1:8*/  THEN 8\r"
				+ "WHEN  split =  0.143 /*1:7 */  THEN 7\r" + "WHEN  split =  0.167 /*1:6*/  THEN 6\r"
				+ "WHEN  split =  0.2 /*1:5*/  THEN 5\r" + "WHEN  split =  0.25 /*1:4*/  THEN 4\r"
				+ "WHEN  split =  0.33 /*1:3*/  THEN 3\r" + "WHEN  split =  0.5 /*1:2*/  THEN 2\r"
				+ "WHEN  split =  0.4 /*.4*/  THEN 2.5\r" + "WHEN  split =  0.6 /*3:5*/  THEN 1.669\r"
				+ "WHEN  split =  0.67 /*2:3*/  THEN 1.5\r"
				+ "ELSE 1 END DEN, Split,prevClose,prevDate,volPerChg,rngPChg,dave\r"
				+ " FROM splitCheck where SPLIT>0  ;\r"
				+ " ALTER TABLE TMP_STOCKSPLIT CHANGE DATE DATE DATE,ADD KEY(DATE), ADD KEY(ITS)\r"
				+ " , CHANGE PREVDATE PREVDATE DATE,change close close double, change prevClose prevClose double;\r"
				+ "\r" + "\r" + "INSERT IGNORE INTO STOCKSPLIT\r"
				+ "SELECT DATE,ITS,NUM,DEN FROM TMP_STOCKSPLIT WHERE VOLPERCHG<300 AND RNGPCHG<8 AND DAVE>500000 ;\r"
				+ "\r" + "SET @date = '1901-01-01'; SET @its = 'a1'; SET @n=0; SET @d=0;\r" + "\r"
				+ "DROP TABLE IF EXISTS TMP_STOCKSPLIT2;\r" + "CREATE TABLE TMP_STOCKSPLIT2 ENGINE=MYISAM\r"
				+ "SELECT\r"
				+ "case when @date!=date and @its=its and datediff(date,@date)<7 and datediff(date,@date)>0 and ABS(@n/@d-num/den)<.01 \r"
				+ "then 1 else 0 end ck\r" + ",@date:=date date,@its:=its its,@n:=num num,@d:=den den\r"
				+ "FROM STOCKSPLIT ORDER BY ITS,date;\r" + "ALTER TABLE TMP_STOCKSPLIT2 ADD KEY(CK);\r"
				+ "/*delete these from stocksplit*/\r" + "\r" + "DROP TABLE IF EXISTS TMP_DEL_STOCKSPLIT_DUPLICATES;\r"
				+ "CREATE TABLE TMP_DEL_STOCKSPLIT_DUPLICATES ENGINE=MYISAM\r"
				+ "SELECT * FROM TMP_STOCKSPLIT2 WHERE CK=1;\r"
				+ "ALTER TABLE TMP_DEL_STOCKSPLIT_DUPLICATES  ADD KEY(ITS), ADD KEY(DATE);\r" + "\r"
				+ "DELETE T1 FROM STOCKSPLIT T1, TMP_DEL_STOCKSPLIT_DUPLICATES T2 WHERE T1.ITS=T2.ITS AND T1.DATE=T2.DATE;\r"
				+ "\r"

				+ "\r"
				+ "/*1st find errors in stockdata. Where there appears to be split - or large price gap down. Then a couple or several days later it reverses and there is a large gap up. If this is true - note the gap - and paste in price prior to the gap. */\r"
				+ "\r" + "/*FIND POSSIBLE SPLITS OF ANY KIND - AND THEN SEE IF THEY ARE IN ERROR*/\r"
				+ "set @date = '1901-01-01'; set @pC = 0; set @ro =0;\r" + "DROP TABLE IF EXISTS TMP_PRICE_ERRORS;\r"
				+ "CREATE TABLE TMP_PRICE_ERRORS ENGINE=MYISAM\r" + "select @ro:=@ro+1 ro,\r"
				+ "case when @its=its and (greatest(@pC,close)-least(@pC,close)) / @pC > .25 then 1 else 0 end ck,\r"
				+ "@date:=t1.date date,@its:=t1.its its,@pC:=t1.close close from stockdata t1 where\r"
				+ "date between @sDate and @eDate\r" + "order by its,date;\r"
				+ "ALTER TABLE TMP_PRICE_ERRORS ADD KEY (ro),ADD KEY (CK),ADD KEY(DATE),ADD KEY(ITS);\r" + "\r"
				+ "/*SEE HOW MANY DAYS APART SPLITS OCCUR (IF CLOSE - ERROR)*/\r" + "SET @prevRo=0; SET @its = '1x';\r"
				+ "DROP TABLE IF EXISTS TMP_PRICE_ERRORS2;\r" + "CREATE TABLE TMP_PRICE_ERRORS2 ENGINE=MYISAM\r"
				+ "SELECT case when @its=its then ro-@prevRo else 0 end df,\r"
				+ "@prevRo:=t1.ro ro,t1.ck,t1.date,@its:=t1.its its,t1.close FROM TMP_PRICE_ERRORS t1 WHERE CK=1 order by its,date;\r"
				+ "ALTER TABLE TMP_PRICE_ERRORS2 ADD KEY(ro), add key(ck),ADD KEY(DATE),ADD KEY(ITS);\r" + "\r"
				+ "SET @r=0; SET @its = '1x';\r" + "DROP TABLE IF EXISTS TMP_PRICE_ERRORS3;\r"
				+ "CREATE TABLE TMP_PRICE_ERRORS3 ENGINE=MYISAM\r"
				+ "SELECT case when df=0 AND @its=its then @r-ro else df end df2,@r:=ro row,ck,date,@its:=its its,close \r"
				+ "FROM TMP_PRICE_ERRORS2 t1 order by its,date desc;\r"
				+ "ALTER TABLE TMP_PRICE_ERRORS3 ADD KEY(df2),ADD KEY(DATE),ADD KEY(ITS);\r" + "\r"
				+ "delete from TMP_PRICE_ERRORS3 where df2>50 OR DF2=0;\r" + "\r"

				+ "DELETE T1 FROM STOCKSPLIT T1 INNER JOIN (\r"
				+ "SELECT T2.* FROM TMP_PRICE_ERRORS3 T1 INNER JOIN STOCKSPLIT T2\r" + "ON T1.ITS=T2.ITS \r"
				+ "WHERE DATEDIFF(GREATEST(T1.DATE,T2.DATE),LEAST(T1.DATE,T2.DATE))<6) T2\r"
				+ "ON T1.ITS=T2.ITS AND T1.DATE=T2.DATE;\r";

		MysqlConnUtils.executeQuery(query);

	}


	public static void main(String[] args) throws Exception {

	}
}
