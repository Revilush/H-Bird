package xbrl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

import org.apache.http.conn.HttpHostConnectException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DownloadDividendNasdaq {

	public static final String DividendBaseUrl = "http://www.nasdaq.com/symbol/@ticker@/dividend-history";
	
	EasyHttpClient httpClient = new EasyHttpClient(false);
	
	public DownloadDividendNasdaq() {
		// TODO Auto-generated constructor stub
	}

	
	private String getPageHtml(String pageUrl) throws IOException {
		String html = null;
		try {
			html = httpClient.makeHttpRequest("Get", pageUrl, null, -1, null,
					null);
			if (httpClient.getResponseCode() == 404) // page not found..
				html = null;
		} catch (HttpHostConnectException e) { // if connection reset error,
												// retry once again..
			try {
				System.out.println("http err, wait 1 sec::"
						+ Thread.currentThread().getId());
				Thread.sleep(1000); // wait for 1 sec
			} catch (InterruptedException e1) {
			}
			// retry now..
			html = httpClient.makeHttpRequest("Get", pageUrl, null, -1, null,
					null);
		}
		return html;
	}
	
	
	public String getDividendData(String ticker) throws IOException {
		String url = DividendBaseUrl.replaceAll("@ticker@", ticker);
		System.out.println(url);
		String html = getPageHtml(url);
		Document doc = Jsoup.parse(html);

		Elements tbl = doc.select("table#quotes_content_left_dividendhistoryGrid");
		if (null != tbl  &&  tbl.size() > 0) {
			Elements rows = tbl.get(0).select("tbody tr");
			//System.out.println(table.text());
			Elements tds;
			StringBuilder sb = new StringBuilder();
			if (null != rows  &&  rows.size() > 0) {
				for (Element row : rows) {
					// date,$amt.cent,ticker,type,decDt,recDt,payDt
					// can be tab or comma separated - each record on its own line
					tds = row.select("td");
					sb.append(tds.get(0).text().replaceAll("", "") + "," + tds.get(2).text() + ","
							+ ticker + "," + tds.get(1).text() + ","
							+ tds.get(3).text() + "," + tds.get(4).text() + ","
							+ tds.get(4).text()+"\n");
				}
			}
			return sb.toString();
		}
		//pw.close();
		
		return "";
	}
	
	public static void dividends(String[] symbolRange) throws SQLException, IOException{
		
		String ticker = "";
		
		File file = new File("c:/backtest/dividends/dividendSymbols_"+symbolRange[0]+"_"+symbolRange[1]+".txt");
		if(file.exists())
			file.delete();
		
		String qry = "set @startDate = (select maxDate from (\r"+
				"select max(date) maxDate,its from csi_stockdata_unadjusted where its='a' or its='aa' or its='bk' or its='wfc' \r" +
				"or its='jpm' or its='ibm'" +
				"or its='aapl' or its = 'xom' or its = 'amzn' or its = 'fb' group by its) t1\r"+
				"group by maxDate order by maxDate desc limit 1);\r"
				+"\r"+
				"set @startDt = date_sub(@startDate, interval 1 month);\r"+
				"\r"+
				"set @dt = (select date from (select count(*) cnt, date from " +
				"\rcsi_stockdata_unadjusted where date>=@startDt and date<date_add(@startDt,interval 4 day) and close>1 group by date\r"+
				"order by cnt desc, date asc limit 1) t1);" +
				"\r\rSELECT REPLACE((replace(its,'-','.')),' ','.')" +
				"\r INTO OUTFILE 'c:/backtest/dividends/dividendSymbols_"+symbolRange[0]+"_"+symbolRange[1]+".txt'\r"+
				"FIELDS TERMINATED BY ','\r"+
				"ESCAPED BY '\'\r"+
				"LINES TERMINATED BY '\\n'\r"+
				"FROM DIV_GET where its rlike '^["+symbolRange[0]+"-"+symbolRange[1]+"]' and its not rlike '[-+]'" +
				";\r";
		
		MysqlConnUtils.executeQuery(qry);
		
		String tickerStr = Utils.readTextFromFile(file.getAbsolutePath());
		
		String[] tickers = tickerStr.split("\n");
		
		PrintWriter pw = new PrintWriter(new File("c:/backtest/dividends/dividends_"+symbolRange[0]+"_"+symbolRange[1]+".txt"));
		for(int i = 0; i<tickers.length; i++){
			ticker = tickers[i].trim();
			System.out.println("ticker="+ticker);
			DownloadDividendNasdaq dp = new DownloadDividendNasdaq();
			pw.append(dp.getDividendData(ticker));
			System.out.println("got ticker");
		}
		
		System.out.println("all tickers done");
		
		pw.close();
		System.out.println("closed pw done");

		String query = "\rLOAD DATA INFILE 'c:/backtest/dividends/dividends_"+symbolRange[0]+"_"+symbolRange[1]+".txt' \r"+
				"Ignore INTO TABLE stockdividend\r"+
				"FIELDS TERMINATED BY ','\r"+
				"LINES TERMINATED BY '\n'\r"+
				"(@var1,dividend,@varITS,type,@var2,@var3,@var4)\r"+
				"SET exDivDt = str_to_date(@var1, '%m/%d/%Y')\r"+
				",ITS = replace(@varITS,'.','-')"+
				",decDt  = str_to_date(@var2, '%m/%d/%Y')\r"+
				",recDt  = str_to_date(@var3, '%m/%d/%Y')\r"+
				",payDt  = str_to_date(@var4, '%m/%d/%Y');\r";

		System.out.println("load data infile query");

		MysqlConnUtils.executeQuery(query);
		System.out.println("load data infile query executed");
		
	}
	
	public static void callDividend() throws SQLException, IOException{

		
		String query = "set @startDt = (date_sub((select max(date) from csi_stockdata_unadjusted), interval 1 month));\r"+
				"\r"+
				"set @dt=(select max(enddate) from csi_symbols);\r"+
				"DROP TABLE IF EXISTS TMP_DIV_GET;\r"+
				"CREATE TABLE TMP_DIV_GET ENGINE=MYISAM\r"+
				"select round(avg(close)*avg(volume)) dave,its,close from csi_stockdata_unadjusted where date>@startDt group by its ;\r"+
				"ALTER TABLE TMP_DIV_GET ADD KEY(ITS),ADD KEY(CLOSE), ADD KEY(DAVE);\r"+
				"\r"+
				"\r"+
				"DROP TABLE IF EXISTS DIV_GET;\r"+
				"CREATE TABLE DIV_GET\r"+
				"select t1.its from TMP_DIV_GET t1 inner join csi_symbols t2\r"+
				"on t1.its=t2.its where exchange rlike 'nyse|nasdaq|amex' \r"+
				"and enddate=@dt and (name not rlike \r"+
				"'etf|ipath|indx|ish|ishare|isha|powersh|nuveen|blackrock|schwa|vangu|vane ec|veloc|virtus|first t|prosh\r"+
				"		|db x|direx|lyxor|guggen|amundi|wisdom|global x|spdr|eaton|msci|xtracker|ssga'\r"+
				"or (t1.its='blk' or t1.its='SCHW') )\r"+
				"AND\r"+
				"dave>1000000 and close>5 and T1.its not rlike '[-+]' \r"+
				"GROUP BY ITS;\r"+
				"ALTER TABLE DIV_GET change its its varchar(15),ADD PRIMARY KEY(ITS);\r\r" +
				"SET @YR=YEAR(DATE_SUB((SELECT MAX(DATE) FROM csi_stockdata_unadjusted), INTERVAL 365 DAY));\r"+
				"SET @DT = (SELECT MAX(DATE) FROM csi_stockdata_unadjusted);\r"+
				"\r"+
				"insert ignore into div_get\r"+
				"select t1.its from (select * from (\r"+
				"select count(*) c,its,year(exdivdt) yr from stockdividend \r"+
				"where year(exdivdt)=@YR\r"+
				"group by its) t1 where c between 10 and 99 ) t1 \r"+
				"inner join  (select its,dt from(\r"+
				"select max(exDivDt) dt,its from stockdividend group by its ) t1 ) t2\r"+
				"on t1.its=t2.its\r"+
				"where datediff(@DT,dt) between 24 and 38 ;\r"+
				"\r"+
				"\r"+
				"\r"+
				"INSERT IGNORE INTO DIV_GET \r"+
				"select t1.its from (select * from (\r"+
				"select count(*) c,its,year(exdivdt) yr from stockdividend \r"+
				"where year(exdivdt)=@YR\r"+
				"group by its) t1 where c between 6 and 9 ) t1 \r"+
				"inner join  (select its,dt from(\r"+
				"select max(exDivDt) dt,its from stockdividend group by its ) t1 ) t2\r"+
				"on t1.its=t2.its\r"+
				"where datediff(@DT,dt) between 35 and 61 ;\r"+
				"\r"+
				"INSERT IGNORE INTO DIV_GET \r"+
				"select t1.its from (select * from (\r"+
				"select count(*) c,its,year(exdivdt) yr from stockdividend \r"+
				"where year(exdivdt)=@YR\r"+
				"group by its) t1 where c =3 ) t1 \r"+
				"inner join  (select its,dt from(\r"+
				"select max(exDivDt) dt,its from stockdividend group by its ) t1 ) t2\r"+
				"on t1.its=t2.its\r"+
				"where datediff(@DT,dt) between 120 and 140 ;\r"+
				"\r"+
				"INSERT IGNORE INTO DIV_GET \r"+
				"select t1.its from (select * from (\r"+
				"select count(*) c,its,year(exdivdt) yr from stockdividend \r"+
				"where year(exdivdt)=@YR\r"+
				"group by its) t1 where c between 4 and 5 ) t1 \r"+
				"inner join  (select its,dt from(\r"+
				"select max(exDivDt) dt,its from stockdividend group by its ) t1 ) t2\r"+
				"on t1.its=t2.its\r"+
				"where datediff(@DT,dt) between 80 and  100 ;\r"+
				"\r"+
				"\r"+
				"INSERT IGNORE INTO DIV_GET \r"+
				"select t1.its from (select * from (\r"+
				"select count(*) c,its,year(exdivdt) yr from stockdividend \r"+
				"where year(exdivdt)=@YR\r"+
				"group by its) t1 where c = 2 ) t1 \r"+
				"inner join  (select its,dt from(\r"+
				"select max(exDivDt) dt,its from stockdividend group by its ) t1 ) t2\r"+
				"on t1.its=t2.its\r"+
				"where datediff(@DT,dt) between 140 and 220 ;\r"+
				"\r"+
				"INSERT IGNORE INTO DIV_GET \r"+
				"select t1.its from (select * from (\r"+
				"select count(*) c,its,year(exdivdt) yr from stockdividend \r"+
				"where year(exdivdt)=@YR\r"+
				"group by its) t1 where c = 1 ) t1 \r"+
				"inner join  (select its,dt from(\r"+
				"select max(exDivDt) dt,its from stockdividend group by its ) t1 ) t2\r"+
				"on t1.its=t2.its\r"+
				"where datediff(@DT,dt) between 300 and 420 ;\r"+
				"\r"+
				"\r"+
				"\r"+
				"DELETE T1 FROM DIV_GET T1, (SELECT avg(close) close,ROUND(AVG(CLOSE)*AVG(VOLUME)) DAVE,ITS FROM csi_stockdata_unadjusted \r"+
				"WHERE DATE>=DATE_SUB(@DT,INTERVAL 1 week) GROUP BY ITS) T2 WHERE T1.ITS=T2.ITS\r"+
				"AND DAVE<70000 and close<5;\r";

		
		MysqlConnUtils.executeQuery(query);

		
		String [] symbolRangeA= {"A","A"};
		String [] symbolRangeB= {"B","B"};
		String [] symbolRangeC= {"C","C"};
		String [] symbolRangeD= {"D","D"};
		String [] symbolRangeE= {"E","E"};
		String [] symbolRangeF= {"F","F"};
		String [] symbolRangeG= {"G","G"};
		String [] symbolRangeH= {"H","H"};
		String [] symbolRangeI= {"I","I"};
		String [] symbolRangeJ= {"J","J"};
		String [] symbolRangeK= {"K","K"};
		String [] symbolRangeL= {"L","L"};
		String [] symbolRangeM= {"M","M"};
		String [] symbolRangeN= {"N","N"};
		String [] symbolRangeO= {"O","O"};
		String [] symbolRangeP= {"P","P"};
		String [] symbolRangeQ= {"Q","Q"};
		String [] symbolRangeR= {"R","R"};
		String [] symbolRangeS= {"S","S"};
		String [] symbolRangeT= {"T","T"};
		String [] symbolRangeU= {"U","U"};
		String [] symbolRangeV= {"V","V"};
		String [] symbolRangeW= {"W","W"};
		String [] symbolRangeX= {"X","X"};
		String [] symbolRangeY= {"Y","Y"};
		String [] symbolRangeZ= {"Z","Z"};

		// updated last on 7.23.2017
		dividends(symbolRangeA);
		dividends(symbolRangeB);
		dividends(symbolRangeC);
		dividends(symbolRangeD);
		dividends(symbolRangeE);
		dividends(symbolRangeF);
		dividends(symbolRangeG);
		dividends(symbolRangeH);
		dividends(symbolRangeI);
		dividends(symbolRangeJ);
		dividends(symbolRangeK);
		dividends(symbolRangeL);
		dividends(symbolRangeM);
		dividends(symbolRangeN);
		dividends(symbolRangeO);
		dividends(symbolRangeP);
		dividends(symbolRangeQ);
		dividends(symbolRangeR);
		dividends(symbolRangeS);
		dividends(symbolRangeT);
		dividends(symbolRangeU);
		dividends(symbolRangeV);
		dividends(symbolRangeW);
		dividends(symbolRangeX);
		dividends(symbolRangeY);
		dividends(symbolRangeZ);


		query = "DROP TABLE IF EXISTS DIV_GET;\r";
		MysqlConnUtils.executeQuery(query);

	}
	
	
	
	public static void main(String[] args) throws IOException, SQLException {
		
		//TODO: record retrieval date.

		// NOTE: access will be FORBIDDEN to their entire site if I try to run
		// in parallel from same IP address. Break into small pieces in case it
		// gets interrupted at least I got some of it

		 callDividend();
		// the list of symbols to download dividends was from stockdata which is
		// outdated. So I changed it to the csi_stockdata_unadjusted. However - this is
		// now redundant given I get dividends from CSI using formula. But this may be a
		// nice check - or cause errors in my data.
		
		//see -->>>> https://www.dividendchannel.com/symbol/acp/
	}

}
