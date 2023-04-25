package xbrl;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TempTP_raw_FS {

	public static void main(String[] arg) throws IOException, SQLException,
			ParseException {

		FinancialStatementsTPRaw fs = new FinancialStatementsTPRaw();

		// current bac_tp_rawYYYYQtr# is limited to CIKs b/w 0 and 10000
		int cikStart = 0, cikEnd = 10000;
		int cnt = 0;

		int startYr = 1993, endYr = 2009;
		String table = "";
		int yr = startYr;
		int startQ = 1, endQ = 4, q = startQ;
		int qtr = startQ;
		boolean regenerateBacTPRawYYYYQtr = false, 
				truncateEverything = true, 
				copyToBacTpRaw = true;
		
		if(truncateEverything){
			// TODO: right now I'm at 98% success putting sales values into
			// tp_sales_to_scrub. What next?

			 MysqlConnUtils.executeQuery("truncate tp_sales_to_scrub;"
			 + "\n truncate tp_sales_to_scrub2;"
			 + "\n truncate tp_sales_to_scrub_hold;"
			 + "\n truncate TP_RAW_REVISED;" + 
			"\ntruncate tp_id;");

		}
		
		// bac_tp_raw is a backup of all bac_tp_rawYYYYQTR#

		for (yr = startYr; yr <= endYr; yr++) {
			if (yr == 1993 && qtr < 3) {
				qtr = 3;
			}
			else qtr=1;

			for (q = qtr; q <= endQ; q++) {
				table = "bac_tp_raw" + yr + "qtr" + q;
				if(copyToBacTpRaw ) {
					MysqlConnUtils
					 .executeQuery("insert ignore into bac_tp_raw \r"
					 		+ "select * from "+table+";");
				}

				
				if (regenerateBacTPRawYYYYQtr) {

					MysqlConnUtils
					 .executeQuery("truncate tp_sales_to_scrub;"
					 + "\n truncate tp_sales_to_scrub2;"
					 + "\n truncate tp_sales_to_scrub_hold;\n"
//					 +
//					 " truncate bac_tp_raw2018qtr1;\n"
//					 +"insert ignore into bac_tp_raw2018qtr1\n"
//					 +"select * from bac_tp_raw where accno='0000950137-96-000821' ;\n"
//					 +"update bac_tp_raw2018qtr1 set filedate='2018-03-01', accno='1';\n"
//					 +"delete from bac_tp_raw where accno='1';\n" +
//					 "insert ignore into tp_raw_revised\n" +
//					 "select * from bac_tp_raw2018qtr1;\n"
					 );
					 
					// tmp.regenerateBAC_TP_Raw_YYYYQtrTester(table,
					// "'0000820067-05-000021'", "0");
					// <<-for testing single accno/cik. This will truncate each
					// loop - so remove when running for time periods.
//					fs.regenerateBac_TP_RawYYYYQtrNo(table, cikStart, cikEnd);
				}
				
				 fs.fixRownamesAndTNtype(table,true);// only need to run if I
				// regenerate bac_tp_rawYYYYqtr# table.

				 fs.markBadTables(table);// only need to run if I regenerate
				// bac_tp_rawYYYYqtr# table.

				fs.renameTables(table); 
				
				/*
				 * renameTables only needs to run if I regenerate
				 * bac_tp_rawYYYYqtr# table. At renameTables - bac_tp_rawYYYYQtr
				 * is transferred to tp_raw_revised -- duplicate of bac_tp_raw
				 * but with revisions to tables where bac_tp_raw is original
				 * parsing.
				 */
				
				 fs.tp_Sales_fixEndDates(table, true); // only need to run if
				// I regenerate bac_tp_rawYYYYqtr# table.
				
				 fs.prep_TP_Id(table, cikStart, cikEnd, cnt);
				// prep_tp_Id -- this puts into prep_tp_id

				cnt++;
		}
			qtr = 1;
		}
		
		

		 fs.createTP_Id();
		// Only needs 3 yrs bef / aft filedates to ck. Tbl will get very large
		
		 fs.getPeriodsConformEnddate(startYr, endYr, startQ, endQ);//<<keep!
		// Very good!
		// goes after createTP_id. uses +++++++++++++++++++++++++++++++++++++++tp_id to get Ps then drops/creates tp_ip
		// with conformed enddates AND updates each bac_tp_rawYYYYQtr tbl with
		// non-conformed EDTs and found Periods

		// TODO: will regenerate tp_mismatch_edt each time if set to true;
		 fs.prepRepairMismatch(startYr, endYr, startQ, endQ, true);
		 fs.repairEnddatePeriod(startYr, endYr, startQ, endQ);

		// this uses entire tp_id - this is a large
		// TODO NOTE: I believe I can run getPeriodsConformEnddate and then
		// repair 1 more time and it will get more. have all put in one method
		// that cycles through and calls each.

		cnt = 0;
		qtr = startQ;
		for (yr = startYr; yr <= endYr; yr++) {
			if (yr == 1993 && qtr < 3) {
				qtr = 3;
			}
			
			
			for (q = qtr; q <= endQ; q++) {
				table = "bac_tp_raw" + yr + "qtr" + q;
				fs.tp_sales_to_scrub(table, cikStart, cikEnd,
						"tp_sales_to_scrub", cnt);
				// if cnt=0 this will drop and create tp_sales_to_scrub and
				// tp_sales_to_scrub2
				cnt++;
				fs.getSalesFromBlankRows(table, cikStart, cikEnd,
						"tp_sales_to_scrub");
				fs.tp_p3Prep(table, "tp_sales_to_scrub");
				// <<== gets period=3 max values in case if separate table.
				fs.tp_sales_to_scrub("p3_" + table, cikStart, cikEnd,
						"tp_sales_to_scrub", 1);// <<always set cnt=1 -
				fs.getSalesFromBlankRows("p3_" + table, cikStart, cikEnd,
						"tp_sales_to_scrub");
//				MysqlConnUtils.executeQuery("\ndrop table if exists p3" + table
//						+ ";\n");
				// can't drop p3 table until this point
				// don't run either of these - run as separate cleanup
			}
			qtr = 1;
		}

		 fs.getEnddateOrPeriodSmartMatch(startYr, endYr, startQ, endQ);
		// see getEnddateOrPeriodSmartMatch - need to uncomment 2nd half of
		// method - else it does nothing. 

		 fs.addRevisedColumn("tp_sales_to_scrub", "tp_sales_to_scrub2");
		// addRevisedColumn -- revert to old commented out code -- but requires
		// 'bac_tp_raw' table be populated.
		 
//		 tp_Sales_fixEndDates==> chgd - tp_sales_to_scrub2 to tp_sales_to_scrub
		fs.tp_Sales_fixEndDates("tp_sales_to_scrub2", false);
		 //conformEnddatesAndRownames==> chgd - tp_sales_to_scrub2 to tp_sales_to_scrub
		fs.conformEnddatesAndRownames("tp_sales_to_scrub2", startYr, endYr,
				startQ, endQ);
		// conformEnddatesAndRownames==>hunts for same rownames in each
		// bac_tp_rawYYYY tbl if it is missing in some cases but that rowname is
		// otherwise frequent in tp_sales_to_scrub2. I search only for the most
		// frequent (not odd ball- I may want to search that also b/c if I'm
		// stuck with a y-o-y odd ball I may not have prior y). MUST be run
		// before running final generate tp_sales
		// y-o-y G/L.

//		if (table.length() > 10)
//			fs.dropTablesIfExists(table);
		int[] periods = { 3, 6, 9, 12 };
		cnt = 0;
//		fs.generateTP_Sales(periods, "tp_sales_to_scrub2", cikStart, cikEnd,
//				cnt);
		cnt++;
//		if (table.length() > 10)
//			fs.dropTablesIfExists(table);

	}




	// chg bac_tp_raw - so it rownames don't have 'ttl/stt/sub/net' by doing

	// TODO: when do I conformMultipleCIKs? See method in fs
	
	// }

}

