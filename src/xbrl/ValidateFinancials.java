package xbrl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ValidateFinancials {

	public static void validateSales() throws SQLException {
		
		// TODO: these may end up being method parameters.
		String sDt = "1993-07-01", eDt = "1999-01-01", acc1, 
				acc2,fd1,fd2,fd_dif,edt1,edt2,endDate="",prevEdt="",rowname1,rowname2;
		int period = 3, cik = 0,prevCik=0,p1,p2,dec,sec;
		double value,val1,val2;
		
		String query = "/*when you have restated sales - you can only calculate restated versus restated and non-restated versus non-restated.\r"
				+ "restated value is the later fileDate. Any filings prior to restated fileDate can't be compared to revised values on that\r"
				+ "fileDate*/\r"
				+ "set @cik = 0; set @val=0.0; set @edt = '1900-01-01'; set @def = 0.00; set @p=0;\r"
				+ "select * from (\r"
				+ "select case when @cik=cik and @edt=enddate and @p=period and (@val=value*`dec` or abs(@val-value*`dec`)/(@val*.5+value*`dec`*.5)<.01)\r"
				+ "then 0 else 1 end kp,\r"
				+ "@restated:=case when @cik=cik and @edt=enddate and @p=period and @val!=value*`dec` and abs(@val-value*`dec`)/(@val*.5+value*`dec`*.5)>.02\r"
				+ "then round((abs(@val-value*`dec`)/(@val*.5+value*`dec`*.5)),2) else '' end restate,\r"
				+ "datediff(enddate,@edt) ddif,\r"
				+ "accno,@cik:=cik cik,tbl,row,tblrow,colno,rowname,@val:=value*`dec` value,@p:=period period,@edt:=enddate enddate\r"
				+ ",left(filedate,10) filedate,`dec`,form from tp_sales_to_scrub where cik=858877 and \r"
				+ "year(enddate) between 1990 and 2020 and period between 3 and 12\r"
				+ "order by cik,period,enddate,filedate ) t1 where kp=1 order by cik,period,enddate,filedate ;\r";

		Connection conn = MysqlConnUtils.getConnection();
		Statement stmt = conn.createStatement();
		System.out.println(query+"\r");
		ResultSet rs = stmt.executeQuery(query);

		List<String[]> cikList = new ArrayList<String[]>();
		int i = 0;
		
		while (rs.next()) {
			i++;
			cik = rs.getInt("cik");
			endDate = rs.getString("enddate");
			value = rs.getDouble("value");
			sec = rs.getInt("sec");
			dec = rs.getInt("dec");
			fd1 = rs.getString("fd1");

			if (prevCik == cik) {
				// System.out.println("cik=" + cik +
				// ", prevCik="+prevCik+", endDate=" + endDate+
				// ", value="+value);
				String[] rec = { cik + "", endDate, fd1, value + "",
						sec + "", dec + "" };
				cikList.add(rec);
//				System.out.println("rec="+Arrays.toString(rec));
			} else {
				if (i > 1) {
					// TODO: perform ruleset to figure out which value is
					// associated with eDt. Call methods.
//					System.out.println("cikList.size()=" + cikList.size()
//							+ ", cik processed=" + prevCik);
					getValueForEnddate(cikList);
				}

				cikList.clear();
				String[] rec = { cik + "", endDate, fd1, value + "",
						sec + "", dec + "" };
				cikList.add(rec);
//				System.out.println("rec="+Arrays.toString(rec));
			}
			prevCik = cik;
		}
		
		
	rs.close();
	stmt.close();
	conn.close();
	
}
	
	public static void getValueForEnddate(List<String[]> list) {
//		list=acc1,cik, endDate,fd1,value,sec,dec
		
		String acc1, firstEdt="0", prevEdt="1", edt="2"
				,firstFd1, prevFd1,fd1,cik, prevCik = "0";
		double firstEdtVal=0,prevEdtVal = 1,edtVal=2, dif;
		for (int i = 0; i < list.size(); i++) {
//			System.out.println("list="+Arrays.toString(list.get(i)));
			acc1 = list.get(i)[0];
			cik=list.get(i)[1];
			edt=list.get(i)[2];
			fd1=list.get(i)[3];
			edtVal=Double.parseDouble(list.get(i)[4]);
//			System.out.println("edtVal=" + list.get(i)[4]);
			if(prevEdt.equals(edt)){
//				System.out.println("endDate="+edt+ ", endDateValue="+edtVal);
				dif = Math.abs(edtVal-prevEdtVal)/(edtVal*.5+prevEdtVal*.5);
				if(prevEdtVal!=edtVal && dif>.15){
					System.out.println("same edt different values. edtVal="+edtVal+", " +
							"prevEdtVal="+prevEdtVal + "  %dif="+dif  );
					
				}
			}
			if(!prevEdt.equals(edt) || !prevCik.equals(cik)){
				firstEdt=edt;
				firstEdtVal= edtVal;
				firstFd1=fd1;
//				System.out.println("firstEdt="+firstEdt+ ", firstEdtValue="+edtVal);
			}
			prevEdt=edt;
			prevEdtVal=edtVal;
			prevCik = cik;
			prevFd1 = fd1;

//			System.out.println("prevEdt="+prevEdt+ " prevEdtValue="+prevEdtVal);
		}
	}
	
	public static void getSalesForCik(String cik) throws SQLException{
	
		Connection conn = MysqlConnUtils.getConnection();
		Statement stmt = conn.createStatement();
		String query = "select * from tp_sales_to_scrub where cik=1750 and enddate>'1994-01-01' " +
				" and period = 3 order by enddate,period;";
		System.out.println(query+"\r");
		ResultSet rs = stmt.executeQuery(query);
		while(rs.next()){
			
		}

		rs.close();
		stmt.close();
		conn.close();
		
		
	}

	public static void main(String[] args) throws SQLException {
		validateSales();
//		getSalesForCik("1750");
		
	}

}
