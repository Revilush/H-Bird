package xbrl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class FinancialStatementsTPRaw {

	public void fixRownamesAndTNtype(String table, boolean stripSubStt)
			throws SQLException, FileNotFoundException {

		// This performs a series of utility like functions to
		// bac_tp_rawYYYYQtr# table. Joins two rownames, fixes htmlTxt where
		// null, re-numbers trow and row so they are always consecutive (both
		// due to numbering being off - but not table- from java and due to
		// rowname merge. It also inserts the rnh for blank rowname (eg. if
		// rownameHeader is 'Total Revenues' and there's a blank rowname that
		// sums to all sub-components immediately under RNH - I then put the RNH
		// at the blank row location. Lastly - this method will recharacterize
		// the table type (if tn incorrectly labeled 'bs' and should be 'is' it
		// will set tn='is')

		table = table.toLowerCase();
		String yr = table.substring(table.indexOf("tp_raw") + 6,
				table.indexOf("tp_raw") + 10);
		String q = table.substring(table.indexOf("tp_raw") + 13,
				table.indexOf("tp_raw") + 14);

		/*
		 * First gets rid of 'sub','net','ttl','stt' markers in rowname. Then
		 * runs query against rowname to join two or more rownames and then
		 * deletes earlier rowname that was joined. These are rownames that
		 * should have been on one line but wrapped and java captured as two
		 * separate rows. This corrects that.
		 */

		/* removes ;sub/;st/;tl/;net and rnh/rnh set from rowname */

		StringBuilder sb = new StringBuilder();

		if (stripSubStt) {

			sb.append("drop table if exists tmp_bac_tp_raw"
					+ yr
					+ "qtr"
					+ q
					+ ";\r"
					+ "CREATE TABLE tmp_bac_tp_raw"
					+ yr
					+ "qtr"
					+ q
					+ " (\r"
					+ "  `AccNo` varchar(20) NOT NULL DEFAULT '-1',\r"
					+ "  `fileDate` datetime DEFAULT NULL,\r"
					+ "  `cik` int(11) DEFAULT NULL,\r"
					+ "  `tn` varchar(10) DEFAULT NULL,\r"
					+ "  `row` int(5) NOT NULL DEFAULT '-1' COMMENT 'table row',\r"
					+ "  `col` tinyint(2) DEFAULT NULL COMMENT 'data col number in financial table',\r"
					+ "  `tRow` tinyint(2) DEFAULT NULL COMMENT 'row number in financial table',\r"
					+ "  `tno` int(5) NOT NULL,\r"
					+ "  `rowName` varchar(125) DEFAULT NULL,\r"
					+ "  `value` double(23,5) DEFAULT NULL,\r"
					+ "  `ttl` int(4) DEFAULT NULL,\r"
					+ "  `stt` int(4) DEFAULT NULL,\r"
					+ "  `net` int(4) DEFAULT NULL,\r"
					+ "  `sub` int(4) DEFAULT NULL,\r"
					+ "  `p1` int(3) DEFAULT NULL COMMENT 'if html - per1 parsed from cell, if txt per1 parsed based on col hdg ratio matching',\r"
					+ "  `edt1` varchar(11) DEFAULT NULL COMMENT 'same as per1',\r"
					+ "  `p2` int(3) DEFAULT NULL COMMENT 'if html - per2 based on col hdg ratio matching, if txt based on idx alignments of each match',\r"
					+ "  `edt2` varchar(11) DEFAULT NULL COMMENT ' same as per2',\r"
					+ "  `tc` tinyint(3) DEFAULT NULL COMMENT 'total number of data cols',\r"
					+ "  `tableName` varchar(255) DEFAULT '',\r"
					+ "  `coMatch` tinyint(1) DEFAULT NULL COMMENT '1 means company name is in tableheading',\r"
					+ "  `companyNameMatched` varchar(100) DEFAULT '',\r"
					+ "  `dec` int(11) DEFAULT NULL,\r"
					+ "  `tsShort` varchar(20) DEFAULT NULL COMMENT 'Yr mo per in order found in tablesentence. This pattern can then be used to grab data in TSLong',\r"
					+ "  `ColumnText` varchar(255) DEFAULT NULL COMMENT 'shows this col #s text used for edt2. ',\r"
					+ "  `ColumnPattern` varchar(255) DEFAULT NULL,\r"
					+ "  `allColText` varchar(255) DEFAULT NULL COMMENT 'shows by Line each Column based on words being separated by two spaces.',\r"
					+ "  `ended` varchar(50) DEFAULT NULL,\r"
					+ "  `yr` varchar(10) DEFAULT NULL,\r"
					+ "  `mo` varchar(25) DEFAULT NULL,\r"
					+ "  `htmlTxt` varchar(15) DEFAULT NULL COMMENT 'if txt it has loc end idx of far right data col, else it will say html or generic to show which parser used',\r"
					+ "  `form` varchar(15) DEFAULT NULL COMMENT 'this will equal rowratioBeforeColumnUtil if generic in htmlTxt field',\r"
					+ "  `TSlong` varchar(200) DEFAULT NULL\r"
					+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r"
					+ "insert ignore into tmp_bac_tp_raw"
					+ yr
					+ "qtr"
					+ q
					+ "\r"
					+ "select \r"
					+ "t.AccNo, t.fileDate, t.cik, t.tn, t.row, t.col, t.tRow,\r"
					+ " t.tno, trim(replace(replace(replace(replace(replace(replace(substring_index(substring_index(substring_index("
					+ "substring_index(upper(rowname),';SUB',1),';NET',1),';ST',1),';TL',1),':',' '),';',''),',',''),'  ',' '),"
					+ "'RNH SET',''),'RNH','')) rowname, \r"
					+ "t.value, t.ttl, t.stt, t.net, t.sub, t.p1, t.edt1, t.p2, t.edt2, t.tc, t.tableName, t.coMatch,"
					+ " t.companyNameMatched, t.`dec`, t.tsShort, \r"
					+ "t.ColumnText, t.ColumnPattern, t.allColText, t.ended, t.yr, t.mo, t.htmlTxt, t.form, t.TSlong\r"
					+ "from \r" + "bac_tp_raw" + yr + "qtr" + q + " t;\r"
					+ "truncate bac_tp_raw" + yr + "qtr" + q + ";\r"
					+ "insert ignore into bac_tp_raw" + yr + "qtr" + q + "\r"
					+ "select * from tmp_bac_tp_raw" + yr + "qtr" + q + ";\r"
					+ "drop table if exists tmp_bac_tp_raw" + yr + "qtr" + q
					+ ";\r");
			MysqlConnUtils.executeQuery(sb.toString());

		}
		sb = new StringBuilder();

		sb.append("\r"
				+ "\r\rupdate bac_tp_raw"
				+ yr
				+ "QTR"
				+ q
				+ " set rowname=left(rowname,length(rowname)-1) where rowname rlike '[A-Za-z][0-9]$';\r\r\r");
		MysqlConnUtils.executeQuery(sb.toString());
		sb = new StringBuilder();

		sb.append("\r"
				+ "/*joins two rownames and then deletes earlier rowname that was joined.\r"
				+ "These are rownames that should have been on one line but wrapped and\r"
				+ "java captured as two separate rows. This corrects that.*/\r"
				+ "\r"
				+ "set @pRn = ''; set @f=''; set @row=0;\r"
				+ "set @col:=-1; set @tno=0; set @acc='1x'; set @rw=0;\r"
				+ "\r"
				+ "/*this gets dones first - then I number rows/trows. This will join two or more rowmanes when the case when conditions are met.\r"
				+ "The @rw value records the number of rows merged. If two prior rows merged - then I would delete rows trow-1 and trow-2 of merged\r"
				+ "trow.  Once this is complete - tRows and rows will have gaps. So run before 'trow' and 'row' utility*/\r"
				+ "\r" + "DROP TABLE IF EXISTS TMP_"
				+ yr
				+ "qtr"
				+ q
				+ "_JOIN_ROWNAMES;\r"
				+ "CREATE TABLE TMP_"
				+ yr
				+ "qtr"
				+ q
				+ "_JOIN_ROWNAMES ENGINE=MYISAM\r"
				+ "\r"
				+ "select \r"
				+ "@f:=case when  @tno=tno and @acc=accno and @col=0 and row-@row=1 and length(@pRn)>1 and \r"
				+ "/*prior row must have null value which always true when col=0. In addition, the next row must be consecutive (row-@row=1)*/\r"
				+ "( rowname rlike '^\\\\$|^([0-9]{1})|^(at|from|and|\\\\&|ended|in|per|of) ' OR @pRn rlike ' (of|and|in|possible|per|except|post)$|,$'\r"
				+ "or\r"
				+ "(@pRn rlike '(stock|shares)$' and rowname rlike '^(issue|outstand)') or (rowname rlike '^\\\\(' and rowname not rlike '^\\\\([a-zA-Z]{1,2}\\\\)' and @pRn not rlike 'expense|cost|charge|debit|credit')\r"
				+ "or \r"
				+ "(@pRn rlike 'common$' and rowname rlike '^equival') or (@pRn rlike 'continuing' and rowname rlike 'operation')\r"
				+ "or\r"
				+ "(@pRn rlike  '(used (for|in|by\\\\)?)| FROM) (FINANCING|INVESTING|OPERATING|INVESTMENT|FINANCE)$' and rowname rlike 'ACTIVITIES:?$') \r"
				+ "or\r"
				+ " ((right(@pRn,8) = 'used in)' or right(@pRn,8) = 'used by)' or right(@pRn,9) = 'used for)') and rowname rlike '^(operating|financ|invest)') \r"
				+ "or\r"
				+ "( @pRn rlike ' (general and)$' and rowname rlike '^(admin)' )\r"
				+ "or\r"
				+ "( @pRn rlike ' (applicable)$' and rowname rlike '^(to )' )\r"
				+ "or\r"
				+ "( @pRn rlike ' ( plant)$' and left(rowname,1) = '&' )\r"
				+ "or\r"
				+ "( right(@pRn,4) = ' for' and left(rowname,10) = 'income tax' )\r"
				+ "or\r"
				+ "( @pRn rlike ' (held)$' and rowname rlike '^(for) ' )\r"
				+ "or\r"
				+ "( @pRn rlike ' (work)$' and rowname rlike '^(in progresss) ' )\r"
				+ "or\r"
				+ "(@pRn rlike 'income$' and rowname rlike 'tax.{1,3}$')\r"
				+ "or\r"
				+ "(@pRn rlike 'investing|financing|operating' and rowname rlike '^activities.{1,3}$')\r"
				+ "or\r"
				+ "( @pRn rlike ' (less)$' and rowname rlike '^(accumulated.{1,3}depreciat)' )\r"
				+ "or\r"
				+ "( @pRn rlike ' (par value:?)$' and rowname rlike '^(of)) ' )\r"
				+ "or\r"
				+ "( @pRn rlike '(accounting)$' and rowname rlike '^(princip)' )\r"
				+ "or\r"
				+ "( @pRn rlike '(per share)$' and rowname rlike '^(amounts\\\\)?)' )\r"
				+ "or\r"
				+ "( @pRn rlike '( for)$' and rowname rlike '^(under )' )\r"
				+ "or\r"
				+ "( @pRn rlike '( vested)$' and rowname rlike '^(benefit)' )\r"
				+ "or\r"
				+ "( @pRn rlike '([0-9]){1}$' and rowname rlike '^(million|thousand)' )\r"

				+ "or\r"
				+ "(@pRn rlike 'USED (IN|FOR)\\) (OPERATING|FINANCING|INVESTING)$'\r"
				+ "and rowname rlike '^ACTIVITIES') or\r"
				+ "(@pRn rlike 'AVAILABLE FOR SALE$'\r"
				+ "and rowname rlike '^SECURITIES') or\r"
				+ "(@pRn rlike '(INCOME|LOSS|EARNINGS) FROM OPERATIONS BEFORE$'\r"
				+ "and rowname rlike '^EXTRAORDINARY LOSS ON EARLY') or\r"
				+ "(@pRn rlike 'AMORTIZATION OF DEFERRED ACQUISITION$'\r"
				+ "and rowname rlike '^COSTS') or\r"
				+ "(@pRn rlike 'SELLING GENERAL AND ADMINISTRATIVE$'\r"
				+ "and rowname rlike '^EXPENSES') or\r"
				+ "(@pRn rlike 'TOTAL LIABILITIES AND STOCKHOLDERS$'\r"
				+ "and rowname rlike '^EQUITY') or\r"
				+ "(@pRn rlike 'ACCUMULATED OTHER COMPREHENSIVE$'\r"
				+ "and rowname rlike '^INCOME') or\r"
				+ "(@pRn rlike 'RECONCILE NET (EARNINGS|cash|loss|income)$'\r"
				+ "and rowname rlike '^TO NET CASH PROVIDED') or\r"
				+ "(@pRn rlike 'RECONCILE NET (EARNINGS|cash|loss|income) TO$'\r"
				+ "and rowname rlike '^NET CASH PROVIDED BY') or\r"
				+ "(@pRn rlike 'RECONCILE NET (EARNINGS|cash|loss|income) TO (NET( CASH)?)$'\r"
				+ "and rowname rlike '^PROVIDED BY ') or\r"
				+ "(@pRn rlike 'PROVIDED BY$'\r"
				+ "and rowname rlike '^(FINANCING|OPERATING|OPERATING) ACTIVITIES') or\r"
				+ "(@pRn rlike 'reconcile to net cash$'\r"
				+ "and rowname rlike '^provided') or\r"
				+ "(@pRn rlike 'EXTRAORDINARY LOSS ON EARLY$'\r"
				+ "and rowname rlike '^EXTINGUISHMENT OF DEBT') or\r"
				+ "(@pRn rlike 'provided$'\r"
				+ "and rowname rlike '^by') \r"

				+ "or\r"
				+ "( @pRn rlike '(redemption)$' and rowname rlike '^(require)' )\r"
				+ " or\r"
				+ "\r"
				+ "(@pRn rlike '(related)$' and rowname rlike '^(part[iesy]{1,3})') or\r"
				+ "(@pRn rlike '([0-9]{1})$' and rowname rlike '^(and|in) ') or \r"
				+ "(@pRn rlike '(a-z)$' and rowname rlike '^(\\\\(Notes|\\\\(including) ') or\r"
				+ "(@pRn rlike '([0-9]{1}|,|[A-Z])$' and rowname rlike '^\\\\$ ' )\r"
				+ ") and right(@pRn,1)!=':' then 'f' else '' end 'ftch'\r"
				+ "\r"
				+ ",@pRn:=case when  @tno=tno and @acc=accno and @col=0 and row-@row=1 and length(@pRn)>1  and \r"
				+ "( rowname rlike '^\\\\$|^([0-9]{1})|^(at|from|and|\\\\&|ended|in|per|of) ' OR @pRn rlike ' (of|and|in|possible|per|except|post)$|,$'\r"
				+ "or\r"
				+ "(@pRn rlike '(stock|shares)$' and rowname rlike '^(issue|outstand)') or (rowname rlike '^\\\\(' and rowname not rlike '^\\\\([[a-zA-Z]{1,2}\\\\)' and @pRn not rlike 'expense|cost|charge|debit|credit')\r"
				+ "or \r"
				+ "(@pRn rlike 'common$' and rowname rlike '^equival') or (@pRn rlike 'continuing' and rowname rlike 'operation')\r"
				+ "or\r"
				+ "(@pRn rlike  '(used (for|in|by\\\\)?)| FROM) (FINANCING|INVESTING|OPERATING|INVESTMENT|FINANCE)$' and rowname rlike 'ACTIVITIES:?$') \r"
				+ " or\r"
				+ "(@pRn rlike ' (\\\\(?used in\\\\)?|by|from)$' and rowname rlike '^(operating|financ|invest)') \r"
				+ "or\r"
				+ " ((right(@pRn,8) = 'used in)' or right(@pRn,8) = 'used by)' or right(@pRn,9) = 'used for)') and rowname rlike '^(operating|financ|invest)') \r"
				+ "or\r"
				+ "( @pRn rlike ' (general and)$' and rowname rlike '^(admin)' )\r"
				+ "or\r"
				+ "( @pRn rlike ' (applicable)$' and rowname rlike '^(to )' )\r"
				+ "or\r"
				+ "( @pRn rlike ' ( plant)$' and left(rowname,1) = '&' )\r"
				+ "or\r"
				+ "( right(@pRn,4) = ' for' and left(rowname,10) = 'income tax' )\r"
				+ "or\r"
				+ "( @pRn rlike ' (held)$' and rowname rlike '^(for) ' )\r"
				+ "or\r"
				+ "( @pRn rlike ' (work)$' and rowname rlike '^(in progresss) ' )\r"
				+ "or\r"
				+ "(@pRn rlike 'income$' and rowname rlike 'tax.{1,3}$')\r"
				+ "or\r"
				+ "(@pRn rlike 'investing|financing|operating' and rowname rlike '^activities.{1,3}$')\r"
				+ "or\r"
				+ "( @pRn rlike ' (less)$' and rowname rlike '^(accumulated.{1,3}depreciat)' )\r"
				+ "or\r"
				+ "( @pRn rlike ' (par value:?)$' and rowname rlike '^(of)) ' )\r"
				+ "or\r"
				+ "( @pRn rlike '(accounting)$' and rowname rlike '^(princip)' )\r"
				+ "or\r"
				+ "( @pRn rlike '(per share)$' and rowname rlike '^(amounts\\\\)?)' )\r"
				+ "or\r"
				+ "( @pRn rlike '( for)$' and rowname rlike '^(under )' )\r"
				+ "or\r"
				+ "( @pRn rlike '( vested)$' and rowname rlike '^(benefit)' )\r"
				+ "or\r"
				+ "( @pRn rlike '([0-9]){1}$' and rowname rlike '^(million|thousand)' )\r"

				+ "or\r"
				+ "(@pRn rlike 'USED (IN|FOR)\\) (OPERATING|FINANCING|INVESTING)$'\r"
				+ "and rowname rlike '^ACTIVITIES') or\r"
				+ "(@pRn rlike 'AVAILABLE FOR SALE$'\r"
				+ "and rowname rlike '^SECURITIES') or\r"
				+ "(@pRn rlike '(INCOME|LOSS|EARNINGS) FROM OPERATIONS BEFORE$'\r"
				+ "and rowname rlike '^EXTRAORDINARY LOSS ON EARLY') or\r"
				+ "(@pRn rlike 'AMORTIZATION OF DEFERRED ACQUISITION$'\r"
				+ "and rowname rlike '^COSTS') or\r"
				+ "(@pRn rlike 'SELLING GENERAL AND ADMINISTRATIVE$'\r"
				+ "and rowname rlike '^EXPENSES') or\r"
				+ "(@pRn rlike 'TOTAL LIABILITIES AND STOCKHOLDERS$'\r"
				+ "and rowname rlike '^EQUITY') or\r"
				+ "(@pRn rlike 'ACCUMULATED OTHER COMPREHENSIVE$'\r"
				+ "and rowname rlike '^INCOME') or\r"
				+ "(@pRn rlike 'RECONCILE NET (EARNINGS|cash|loss|income)$'\r"
				+ "and rowname rlike '^TO NET CASH PROVIDED') or\r"
				+ "(@pRn rlike 'RECONCILE NET (EARNINGS|cash|loss|income) TO$'\r"
				+ "and rowname rlike '^NET CASH PROVIDED BY') or\r"
				+ "(@pRn rlike 'RECONCILE NET (EARNINGS|cash|loss|income) TO (NET( CASH)?)'\r"
				+ "and rowname rlike '^PROVIDED BY ') or\r"
				+ "(@pRn rlike 'PROVIDED BY$'\r"
				+ "and rowname rlike '^(FINANCING|OPERATING|OPERATING) ACTIVITIES') or\r"
				+ "(@pRn rlike 'reconcile to net cash$'\r"
				+ "and rowname rlike '^provided') or\r"
				+ "(@pRn rlike 'EXTRAORDINARY LOSS ON EARLY$'\r"
				+ "and rowname rlike '^EXTINGUISHMENT OF DEBT') or\r"
				+ "(@pRn rlike 'provided$'\r"
				+ "and rowname rlike '^by') \r"

				+ "or\r"
				+ "( @pRn rlike '(redemption)$' and rowname rlike '^(require)' )\r"
				+ "\r"
				+ " or (@pRn rlike '(related)$' and rowname rlike '^(part[iesy]{1,3})') or\r"
				+ "(@pRn rlike '([0-9]{1})$' and rowname rlike '^(and|in) ') or \r"
				+ "(@pRn rlike '(a-z)$' and rowname rlike '^(\\\\(Notes|\\\\(including) ') or\r"
				+ "(@pRn rlike '([0-9]{1}|,|[A-Z])$' and rowname rlike '^\\\\$ ' )\r"
				+ ") and @pRn not rlike 'holder.{1,3}(equity)$' and right(@pRn,1)!=':' then concat(trim(@pRn),' ',trim(rowname)) else trim(rowname) end jnRowname\r"
				+ " ,@rw:=case when  @f='f' then @rw+1 else 0 end rw\r"
				+ " /*rowname `actual Rowname`,value,right(@pRn,8) rt8, left(rowname,8) lf4\r,*/\r"
				+ ",@col:=col col,@tno:=tno tno, @acc:=accno accno,trow,@row:=row row,@v:=value val\r"
				+ "from bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " t1 where \r"
				+ "col<2 \r"
				+ "order by accno,tno,row ;\r"
				+ "alter table TMP_"
				+ yr
				+ "qtr"
				+ q
				+ "_JOIN_ROWNAMES add key(accno), add key(tno), add key(trow), add key(ftch);\r");

		MysqlConnUtils.executeQuery(sb.toString());
		sb = new StringBuilder();

		sb.append("\r"
				+ "/*ONLY join where max rw<3. Then I update just at the joined row as below\r"
				+ "d2 if marked 1 will indicate max rw of 2 (joins 3 rows). if d2=0 there are 4 or more rows joined.\r"
				+ "d3 marks the high rw value which is the final joined row.*/\r"
				+ "\r"
				+ "set @r=0; set @row=-1; set @trow=-1; set @acc='1x'; set @d2=-1; set @d=-1; set @tno=-1;\r"
				+ "DROP TABLE IF EXISTS TMP_"
				+ yr
				+ "qtr"
				+ q
				+ "_JOIN_ROWNAMES2;\r"
				+ "CREATE TABLE TMP_"
				+ yr
				+ "qtr"
				+ q
				+ "_JOIN_ROWNAMES2 ENGINE=MYISAM\r"
				+ "select @d2:=case when ( (@d=0 or @d2=0) and @trow-trow=1 and @tno=tno and @acc=accno) or rw>2 then 0 else 1 end d2\r"
				+ ",@d:=case when rw>2 then 0 else 1 end d\r"
				+ ",case when @acc!=accno or @tno!=tno or @trow-trow!=1 then 1 else 0 end d3\r"
				+ ",jnRowname,rw,col,@tno:=tno tno,@acc:=accno accno,@trow:=trow trow,@row:=row row ,ftch\r"
				+ "from TMP_"
				+ yr
				+ "qtr"
				+ q
				+ "_JOIN_ROWNAMES t1 where ftch='f' order by accno,tno,trow desc,row desc,rw desc;\r"
				+ "alter table TMP_"
				+ yr
				+ "qtr"
				+ q
				+ "_JOIN_ROWNAMES2 add key(accno), add key(tno), add key(trow), add key(ftch);\r"
				+ "\r");

		MysqlConnUtils.executeQuery(sb.toString());
		sb = new StringBuilder();

		sb.append("UPDATE IGNORE \r" + "bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " t1 inner join TMP_"
				+ yr
				+ "qtr"
				+ q
				+ "_JOIN_ROWNAMES2 t2 on t1.accno=t2.accno and t1.tno=t2.tno and t1.trow=t2.trow\r"
				+ " set rowname=left(jnRowname,255)\r"
				+ "where ftch='f' and d2=1 and d3=1 ;\r"
				+ "\r"
				+ "delete t1 from bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " t1 inner join TMP_"
				+ yr
				+ "qtr"
				+ q
				+ "_JOIN_ROWNAMES2 t2 on t1.accno=t2.accno and t1.tno=t2.tno \r"
				+ "where ftch='f' and d2=1 and d=1\r"
				+ "and (   (t1.trow=t2.trow-1) or (rw=2 and t1.trow=t2.trow-2) ) ;\r"
				+ "/*based on number of rows joined and final joined trow - I delete the prior trows*/\r"
				+ "\r" + "/*correctly numbers trow and rows.*/\r" + "\r"
				+ "delete from bac_tp_raw" + yr + "qtr" + q
				+ " where trim(rowname)='' and value is null ;\r" + "\r");

		MysqlConnUtils.executeQuery(sb.toString());
		sb = new StringBuilder();

		sb.append("set @trow=0;	set @acc='1x'; set @tno=0; set @rw=0; set @rn='hello'; set @tRw=0; set @html='1x'; set @col=99; set @rw=0;\r"
				+ "drop table if exists tmp_bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ ";\r"
				+ "create table tmp_bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " engine=myisam\r"
				+ "/*if col=0 it is a new trow, if @col=tc it is a new trow, if @col>col it is new trow, or if prior rn!=rowname it is a new trow.\r"
				+ "if prior rn=rn and prior @col<col it is same trow*/\r"
				+ "\r"
				+ "select \r"
				+ "t1.fileDate, t1.cik, t1.tn \r"
				+ ",@rw:=case when @acc=accno and @tno=tno then @rw+1 else 0 end rw\r"
				+ ",@tRw:=case when @acc=accno and @tno=tno and (@rn!=rowname or col=0 or @col=tc or @col>col or @tRow!=trow) then @tRw+1 \r"
				+ " when @acc=accno and @tno=tno and @rn=rowname and @col<col then @tRw \r"
				+ " when @acc!=accno or @tno!=tno then 0 else -1 end tRow\r"
				+ ", @col:=t1.col col,@acc:=t1.AccNo accno\r"
				+ ",@tno:=t1.tNo tNo\r"
				+ ", @rn:=rowname  rowName\r"
				+ ", @tRow:=trow trowDelete\r"
				+ ", t1.value, t1.ttl, t1.stt, t1.net, t1.sub, t1.p1, t1.edt1, t1.p2, t1.edt2, t1.tc,t1.tablename, t1.`dec`, t1.tsShort,t1.companyNameMatched,\r"
				+ "t1.ColumnText\r"
				+ ", t1.ColumnPattern, t1.allColText, t1.ended, t1.yr, t1.mo, t1.coMatch, @html:=case when htmltxt is null then @html else t1.htmlTxt end htmlTxt\r"
				+ ", t1.form, t1.TSlong \r"
				+ "from \r"
				+ "\r"
				+ "bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " \r"
				+ "\r"
				+ "t1 order by accno,tno,row;\r"
				+ "alter table tmp_bac_tp_raw"
				+ yr + "qtr" + q + " drop column trowDelete;\r");

		MysqlConnUtils.executeQuery(sb.toString());
		sb = new StringBuilder();

		sb.append("\r" + "truncate bac_tp_raw" + yr + "qtr" + q + " ;\r" + "\r");

		MysqlConnUtils.executeQuery(sb.toString());
		sb = new StringBuilder();

		sb.append("insert ignore into bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ "\r"
				+ "(FILEDATE,CIK,TN,ROW,TROW,COL,ACCNO,TNO,rowName\r"
				+ "				, value, ttl, stt, net, sub, p1, edt1, p2, edt2, tc,tablename, `dec`, tsShort,companyNameMatched, ColumnText\r"
				+ "				, ColumnPattern, allColText, ended, yr, mo, coMatch, htmlTxt\r"
				+ "				, form, TSlong )\r"
				+ "        \r"
				+ "select FILEDATE,CIK,TN,RW,TROW,COL,ACCNO,TNO,rowName\r"
				+ "				, value, ttl, stt, net, sub, p1, edt1, p2, edt2, tc,tablename, `dec`, tsShort ,companyNameMatched,ColumnText\r"
				+ "				, ColumnPattern, allColText, ended, yr, mo, coMatch, htmlTxt\r"
				+ "				, form, TSlong  from tmp_bac_tp_raw" + yr + "qtr" + q
				+ " ;\r" + "\r" + "\r");

		MysqlConnUtils.executeQuery(sb.toString());
		sb = new StringBuilder();

		sb.append("DROP TABLE IF EXISTS TMP_bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ ";\r"
				+ "\r"
				+ "DROP TABLE IF EXISTS TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN;\r"
				+ "CREATE TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN ENGINE=MYISAM\r"
				+ "SELECT accno,tno FROM bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " where rowname rlike 'net cash (provid|used)|cash flows from operat' and tn!='cf' group by accno,tno;\r"
				+ "alter table TMP_" + yr + "QTR" + q
				+ "_WRONG_TN add key(accno), add key(tno);\r" + "\r");

		MysqlConnUtils.executeQuery(sb.toString());
		sb = new StringBuilder();

		sb.append("UPDATE IGNORE bac_tp_raw" + yr + "qtr" + q
				+ " t1 inner join TMP_" + yr + "QTR" + q
				+ "_WRONG_TN t2 on t1.accno=t2.accno and t1.tno=t2.tno\r" +
				"set t1.tn='cf';\r\r");

		MysqlConnUtils.executeQuery(sb.toString());
		sb = new StringBuilder();

		sb.append("\r" + "DROP TABLE IF EXISTS TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN2;\r"
				+ "CREATE TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN2 ENGINE=MYISAM\r"
				+ "select accno,tno from bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ "\r"
				+ "where (rowname rlike '(net|total).{1,5}(revenu|sale)|(^sales|^revenu)($| ;[a-z]{3})|^rnh (sales|revenu)|basic|diluted'\r"
				+ "or ( rowname rlike 'net.{1,3}(income|loss)' and tn!='cf')) and tn!='is'\r"
				+ "and length(rowname)<40 \r"
				+ "group by accno,tno;\r"
				+ "alter table TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN2 add key(accno), add key(tno);\r"
				+ "DROP TABLE IF EXISTS TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN3;\r"
				+ "CREATE TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN3 ENGINE=MYISAM\r"
				+ "select t1.accno,t1.tno,t1.row,t1.rowname,T1.TN from bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " t1 inner join TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN2 t2 on t1.accno=t2.accno and t1.tno=t2.tno;\r"
				+ "ALTER TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN3 ADD KEY(ACCNO),ADD KEY(TNO), ADD KEY(rowName);\r"
				+ "\r"
				+ "DROP TABLE IF EXISTS TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN4;\r"
				+ "CREATE TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN4 ENGINE=MYISAM\r"
				+ "select t1.* from TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN3 t1 where rowname rlike \r"
				+ "'Cash.{1,2}and.{1,2}cash.{1,2}equiv|current.{1,2}(asset|liabili)|total.{1,2}(asset|liabiliti)|net cash (provid|used)|cash flow.{1,3}|retained earn|Adjustment.{1,3}to.{1,3}reconcile|assets.{1,3}beginning.{1,3}of.{1,3}year|Balance.{1,3}(begin|endin)|(begin|end).{1,6}year|(share|stock)holder.{1,5}equit'\r"
				+ "group by accno,tno;\r" + "ALTER TABLE TMP_" + yr + "QTR" + q
				+ "_WRONG_TN4 ADD KEY(ACCNO),ADD KEY(TNO);\r");

		MysqlConnUtils.executeQuery(sb.toString());
		sb = new StringBuilder();

		sb.append("\r" + "DELETE T1 FROM TMP_" + yr + "QTR" + q
				+ "_WRONG_TN3 T1 INNER JOIN TMP_" + yr + "QTR" + q
				+ "_WRONG_TN4 T2 ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO;\r"
				+ "\r");

		MysqlConnUtils.executeQuery(sb.toString());
		sb = new StringBuilder();

		sb.append("UPDATE IGNORE bac_tp_raw" + yr + "qtr" + q
				+ " t1 inner join TMP_" + yr + "QTR" + q
				+ "_WRONG_TN3 t2 on t1.accno=t2.accno and t1.tno=t2.tno\r"
				+ "set t1.tn='is';\r" + "\r");

		MysqlConnUtils.executeQuery(sb.toString());
		sb = new StringBuilder();

		sb.append("DROP TABLE IF EXISTS TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN2;\r"
				+ "CREATE TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN2 ENGINE=MYISAM\r"
				+ "select accno,tno from  bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " \r"
				+ "where rowname rlike '(total|current).{1,3}(asset|liabilit)'\r"
				+ "and tn!='bs' and trow<8 and (p2=0 or length(p2)<1 or p2 is null) and tn!='cf' \r"
				+ "group by accno,tno;\r"
				+ "alter table TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN2 add key(accno), add key(tno);\r"
				+ "\r"
				+ "DROP TABLE IF EXISTS TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN3;\r"
				+ "CREATE TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN3 ENGINE=MYISAM\r"
				+ "select t1.accno,t1.tno,t1.row,t1.rowname,T1.TN from  bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ "  t1 inner join TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN2 t2 on t1.accno=t2.accno and t1.tno=t2.tno;\r"
				+ "ALTER TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN3 ADD KEY(ACCNO),ADD KEY(TNO), ADD KEY(rowName);\r"
				+ "\r"
				+ "DROP TABLE IF EXISTS TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN4;\r"
				+ "CREATE TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN4 ENGINE=MYISAM\r"
				+ "SELECT * FROM TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_WRONG_TN3 WHERE \r"
				+ "rowname rlike\r"
				+ "'cash.{0,5} (flow|provided|used|from|begin|end)|net.{1,4}(income|loss)|Increas|Decreas|beginning.{1,3}of.{1,3}year|Balance.{1,3}(begin|endin)|(begin|end).{1,6}year'\r"
				+ "GROUP BY ACCNO,TNO;\r" + "ALTER TABLE TMP_" + yr + "QTR" + q
				+ "_WRONG_TN4 ADD KEY(ACCNO),ADD KEY(TNO), ADD KEY(rowName);\r");

		MysqlConnUtils.executeQuery(sb.toString());
		sb = new StringBuilder();

		sb.append("\r" + "DELETE T1 FROM TMP_" + yr + "QTR" + q
				+ "_WRONG_TN3 T1 INNER JOIN TMP_" + yr + "QTR" + q
				+ "_WRONG_TN4 T2 ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO;\r"
				+ "\r");

		MysqlConnUtils.executeQuery(sb.toString());
		sb = new StringBuilder();

		sb.append("UPDATE IGNORE  bac_tp_raw" + yr + "qtr" + q
				+ "  t1 inner join TMP_" + yr + "QTR" + q
				+ "_WRONG_TN3 t2 on t1.accno=t2.accno and t1.tno=t2.tno\r"
				+ "set t1.tn='bs';\r\r");

		MysqlConnUtils.executeQuery(sb.toString());

		sb = new StringBuilder();
		sb.append("/*This finds row name headers (RNH) by: getting a potential rnh (value is null) that is immediately followed by a new stt or sub value.\r"
				+ "First I find the minimum row for each accno,tno and stt - and next table I do that for sub. This gets the first row that has a unique\r"
				+ "accno,tno,stt then sub.  The minimim row less is the potential RNH. If the RNH prior is null it should be joined.*/\r"
				+ "\r"
				+ "\r"
				+ "/*fetch rnh. rnh= row bef min row of each unique stt*/\r"
				+ "\r" + "DROP TABLE IF EXISTS TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_stt_RNH; \r"
				+ "CREATE TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_stt_RNH ENGINE=MYISAM\r"
				+ "select min(row)-1 row,min(row)-2 rw2,accno,tno,stt from bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " t1 \r"
				+ "where stt>0 and value is not null \r"
				+ "group by accno,tno,stt;\r"
				+ "ALTER TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_stt_RNH ADD KEY(ACCNO),ADD KEY(TNO), ADD KEY(ROW), ADD KEY(STT);\r"
				+ "\r"
				+ "/*fetch rnh. rnh= row bef min row of each unique sub*/\r"
				+ "\r"
				+ "DROP TABLE IF EXISTS TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_sub_RNH; \r"
				+ "CREATE TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_sub_RNH ENGINE=MYISAM\r"
				+ "select min(row)-1 row,min(row)-2 rw2,accno,tno,sub from bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " t1 \r"
				+ "where sub>0 and value is not null \r"
				+ "group by accno,tno,sub;\r"
				+ "ALTER TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_SUB_RNH ADD KEY(ACCNO),ADD KEY(TNO), ADD KEY(ROW), ADD KEY(SUB);\r"
				+ "\r"
				+ "\r"
				+ "/*fetch rnh based on min(row-1) provided rnh must have value equal to null (rnh have no reported value )*/\r"
				+ "DROP TABLE IF EXISTS TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_RNH; \r"
				+ "CREATE TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_RNH ENGINE=MYISAM\r"
				+ "SELECT t1.accno,t1.tno,t1.rowname,t2.stt TY,0 tyC,t2.rw2 FROM BAC_TP_RAW"
				+ yr
				+ "QTR"
				+ q
				+ " T1 INNER JOIN TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_stt_RNH T2\r"
				+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.ROW=T2.ROW\r"
				+ "WHERE T1.STT IS NULL and value is null;\r"
				+ "\r"
				+ "/*fetch rnh based on min(row-1) provided rnh must have value equal to null (rnh have no reported value )*/\r"
				+ "INSERT IGNORE INTO TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_RNH\r"
				+ "SELECT t1.accno,t1.tno,t1.rowname,t2.sub,1,t2.rw2 FROM BAC_TP_RAW"
				+ yr
				+ "QTR"
				+ q
				+ " T1 INNER JOIN TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_sub_RNH T2\r"
				+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.ROW=T2.ROW\r"
				+ "WHERE T1.SUB IS NULL and value is null;\r"
				+ "\r"
				+ "ALTER TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_RNH ADD KEY(ACCNO),ADD KEY(TNO), ADD KEY(TY), add key(tyc);\r"
				+ "\r"
				+ "\r"
				+ "/*get append rnh to associated total row - if such row is blank or only contains 'total'.*/\r"
				+ "\r"
				+ "update \r"
				+ "/*select T1.TTL,T1.NET,T2.TY,TYC,t1.rowname,t2.rowname ,t1.*\r"
				+ ",concat('https://www.sec.gov/Archives/edgar/data/',cik,'/',t1.accno,'-index.htm') from */\r"
				+ "bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " t1 inner join TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_RNH t2 \r"
				+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO \r"
				+ "set t1.rowname=trim(concat(trim(t1.rowname),' ',trim(t2.rowname))) \r"
				+ "where \r"
				+ "( (T1.ttl=T2.TY AND tyC=0)\r"
				+ "or\r"
				+ "(t1.net=t2.ty and tyC=1) )\r"
				+ "and\r"
				+ "(trim(t1.rowname) = 'TOTAL' or length(trim(t1.rowname))=0)\r"
				+ "and trim(t2.rowname) rlike 'expense|cost|(gross|net)sales|^sales$|revenue|income|^fees$|^current|^long|^assets$|^liabil|^property|sharehold|stockhold'\r"
				+ "and t2.rowname not rlike 'tax|account|accru|payab|benefi|work|progre| to | less ';\r");

		MysqlConnUtils.executeQuery(sb.toString());

	}

	public void markBadTables(String table) throws SQLException, FileNotFoundException {

		String yr = table.substring(table.indexOf("tp_raw") + 6,
				table.indexOf("tp_raw") + 10);
		String q = table.substring(table.indexOf("tp_raw") + 13,
				table.indexOf("tp_raw") + 14);

		StringBuffer sb = new StringBuffer(
				"DROP PROCEDURE IF EXISTS markBadTables" + yr + "QTR" + q
						+ ";\n" + "CREATE PROCEDURE markBadTables" + yr + "QTR"
						+ q + "()\n\n \n" + "\n\nbegin \n");

		sb.append("\n /*this finds good tables where columns did not sum in JAVA but did sum in mysql query below.\n"
				+ " I then mark each ttl,stt,net,sub with 0. When I mark a table bad because it has no subtotals \n"
				+ "I exclude tables where ttl, stt, sub, net are >= 0. This will also delete tables where there are "
				+ " fewer than 7 trows or less than 11 rows*/"
				+ "\nDROP TABLE IF EXISTS tmp_get_more_totals;\n"
				+ "CREATE TABLE `tmp_get_more_totals` (\n"
				+ "  `ck` int(1) NOT NULL DEFAULT '0',\n"
				+ "  `v8` double DEFAULT NULL,\n"
				+ "  `v7` double DEFAULT NULL,\n"
				+ "  `v6` double DEFAULT NULL,\n"
				+ "  `v5` double DEFAULT NULL,\n"
				+ "  `v4` double DEFAULT NULL,\n"
				+ "  `v3` double DEFAULT NULL,\n"
				+ "  `v2` double DEFAULT NULL,\n"
				+ "  `value` double(23,5) DEFAULT NULL,\n"
				+ "  `acc8` VARCHAR(20) CHARACTER SET utf8,\n"
				+ "  `acc7` VARCHAR(20) CHARACTER SET utf8,\n"
				+ "  `acc6` VARCHAR(20) CHARACTER SET utf8,\n"
				+ "  `acc5` VARCHAR(20) CHARACTER SET utf8,\n"
				+ "  `acc4` VARCHAR(20) CHARACTER SET utf8,\n"
				+ "  `acc3` VARCHAR(20) CHARACTER SET utf8,\n"
				+ "  `acc2` VARCHAR(20) CHARACTER SET utf8,\n"
				+ "  `accno` varchar(20) NOT NULL DEFAULT '',\n"
				+ "  `tno8` int(2) DEFAULT NULL,\n"
				+ "  `tno7` int(2) DEFAULT NULL,\n"
				+ "  `tno6` int(2) DEFAULT NULL,\n"
				+ "  `tno5` int(2) DEFAULT NULL,\n"
				+ "  `tno4` int(2) DEFAULT NULL,\n"
				+ "  `tno3` int(2) DEFAULT NULL,\n"
				+ "  `tno2` int(2) DEFAULT NULL,\n"
				+ "  `tno` int(2) NOT NULL DEFAULT '0',\n"
				+ "  KEY `ck` (`ck`),\n"
				+ "  KEY TNO (TNO),\n"
				+ "  KEY ACCNO (ACCNO)\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n"
				+ "\n"
				+ "set @v1=0; set @v2=0; set @v3=0; set @v4=0; set @v5=0; set @v6=0; set @v7=0; set @v8=0;\n"
				+ "set @acc1='x1'; set @acc2='x1'; set @acc3='x1'; set @acc4='x1'; set @acc5='x1'; set @acc6='x1'; set @acc7='x1'; set @acc8='x1';\n"
				+ "set @tno1=0; set @tno2=0; set @tno3=0; set @tno4=0; set @tno5=0; set @tno6=0; set @tno7=0; set @tno8=0;\n"
				+ "\n"
				+ "INSERT IGNORE INTO TMP_GET_MORE_TOTALS\n"
				+ "select case \n"
				+ "when @acc2=accno and @tno2=tno and (round(@v2-@v1)=round(value) or round(@v2+@v1)=round(value)) and value>9 and ttl=-1 then 1.2\n"
				+ "when @acc2=accno and @tno2=tno and (round(abs(@v2-@v1))/round(value) between .99999 and 1.00001 or round(@v2+@v1)/round(value) between .99999 and 1.00001  ) and value>99 and ttl=-1 then 1.21\n"
				+ "when @acc3=accno and @tno3=tno and (round(@v3-@v2-@v1)=round(value) or round(@v3+@v2+@v1)=round(value)) and value>9 and ttl=-1 then 1.3\n"
				+ "when @acc2=accno and @tno2=tno and (round(abs(@v3-@v2-@v1))/round(value) between .99999 and 1.00001 or round(@v3+@v2+@v1)/round(value) between .99999 and 1.00001  ) and value>99 and ttl=-1 then 1.31\n"
				+ "when @acc4=accno and @tno4=tno and (round(@v4-@v3-@v2-@v1)=round(value) or round(@v4+@v3+@v2+@v1)=round(value)) and value>9 and ttl=-1 then 1.4\n"
				+ "when @acc2=accno and @tno2=tno and (round(abs(@v4-@v3-@v2-@v1))/round(value) between .99999 and 1.00001 or round(@v4+@v3+@v2+@v1)/round(value) between .99999 and 1.00001  ) and value>99 and ttl=-1 then 1.41\n"
				+ "when @acc5=accno and @tno5=tno and (round(@v5-@v4-@v3-@v2-@v1)=round(value) or round(@v5+@v4+@v3+@v2+@v1)=round(value)) and value>9 and ttl=-1 then  1.5\n"
				+ "when @acc2=accno and @tno2=tno and (round(abs(@v5-@v4-@v3-@v2-@v1))/round(value) between .99999 and 1.00001 or round(@v5+@v4+@v3+@v2+@v1)/round(value) between .99999 and 1.00001  ) and value>99 and ttl=-1 then 1.51\n"
				+ "when @acc6=accno and @tno6=tno and (round(@v6-@v5-@v4-@v3-@v2-@v1)=round(value) or round(@v6+@v5+@v4+@v3+@v2+@v1)=round(value)) and value>9 and ttl=-1 then 1.6\n"
				+ "when @acc2=accno and @tno2=tno and (round(abs(@v6-@v5-@v4-@v3-@v2-@v1))/round(value) between .99999 and 1.00001 or round(@v6+@v5+@v4+@v3+@v2+@v1)/round(value) between .99999 and 1.00001  ) and value>99 and ttl=-1 then 1.61\n"
				+ "when @acc7=accno and @tno7=tno and (round(@v7-@v6-@v5-@v4-@v3-@v2-@v1)=round(value) or round(@v7+@v6+@v5+@v4+@v3+@v2+@v1)=round(value)) and value>9 and ttl=-1 then 1.7\n"
				+ "when @acc2=accno and @tno2=tno and (round(abs(@v7-@v6-@v5-@v4-@v3-@v2-@v1))/round(value) between .99999 and 1.00001 or round(@v7+@v6+@v5+@v4+@v3+@v2+@v1)/round(value) between .99999 and 1.00001  ) and value>99 and ttl=-1 then 1.71\n"
				+ "when @acc8=accno and @tno8=tno and (round(@v8+@v7-@v6-@v5-@v4-@v3-@v2-@v1)=round(value) or round(@v8-@v7+@v6+@v5+@v4+@v3+@v2+@v1)=round(value)) and value>9 and ttl=-1 then 1.8\n"
				+ "when @acc2=accno and @tno2=tno and (round(abs(@v8-@v7-@v6-@v5-@v4-@v3-@v2-@v1))/round(value) between .99999 and 1.00001 or round(@v8+@v7+@v6+@v5+@v4+@v3+@v2+@v1)/round(value) between .99999 and 1.00001  ) and value>99 and ttl=-1 then 1.81\n"
				+ "when @acc8=accno and @tno8=tno and round(@v8-@v1)=round(value) and value>9 and ttl=-1 then 2.8\n"
				+ "when @acc8=accno and @tno8=tno and round(abs(@v8-@v1))/round(value) between .99999 and 1.00001 and value>99 and ttl=-1 then 2.81\n"
				+ "when @acc7=accno and @tno7=tno and round(@v7-@v1)=round(value) and value>9 and ttl=-1 then 2.7\n"
				+ "when @acc8=accno and @tno8=tno and round(abs(@v7-@v1))/round(value) between .99999 and 1.00001 and value>99 and ttl=-1 then 2.71\n"
				+ "when @acc6=accno and @tno6=tno and round(@v6-@v1)=round(value) and value>9 and ttl=-1 then 2.6 \n"
				+ "when @acc8=accno and @tno8=tno and round(abs(@v6-@v1))/round(value) between .99999 and 1.00001 and value>99 and ttl=-1 then 2.61\n"
				+ "when @acc5=accno and @tno5=tno and round(@v5-@v1)=round(value) and value>9 and ttl=-1 then 2.5 \n"
				+ "when @acc8=accno and @tno8=tno and round(abs(@v5-@v1))/round(value) between .99999 and 1.00001 and value>99 and ttl=-1 then 2.51\n"
				+ "when @acc4=accno and @tno4=tno and round(@v4-@v1)=round(value) and value>9 and ttl=-1 then 2.4\n"
				+ "when @acc8=accno and @tno8=tno and round(abs(@v4-@v1))/round(value) between .99999 and 1.00001 and value>99 and ttl=-1 then 2.41\n"
				+ "when @acc3=accno and @tno3=tno and round(@v3-@v1)=round(value) and value>9 and ttl=-1 then 2.3 \n"
				+ "when @acc8=accno and @tno8=tno and round(abs(@v3-@v1))/round(value) between .99999 and 1.00001 and value>99 and ttl=-1 then 2.31\n"
				+ "else 0 end ck\n"
				+ ",@v8:=@v7 v8,@v7:=@v6 v7,@v6:=@v5 v6,@v5:=@v4 v5, @v4:=@v3 v4,@v3:=@v2 v3,@v2:=@v1 v2, @v1:=value value\n"
				+ ",@acc8:=@acc7 acc8,@acc7:=@acc6 acc7,@acc6:=@acc5 acc6,@acc5:=@acc4 acc5, @acc4:=@acc3 acc4,@acc3:=@acc2 acc3,@acc2:=@acc1 acc2, @acc1:=accno accno\n"
				+ ",@tno8:=@tno7 tno8,@tno7:=@tno6 tno7,@tno6:=@tno5 tno6,@tno5:=@tno4 tno5, @tno4:=@tno3 tno4,@tno3:=@tno2 tno3,@tno2:=@tno1 tno2, @tno1:=tno tno\n"
				+ "from bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " where (col=1 or col=tc) and tc>1 and ttl=-1 order by accno,tno,col,row;\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS tmp2_get_more_totals;\n"
				+ "CREATE TABLE tmp2_get_more_totals ENGINE=MYISAM\n"
				+ "SELECT accno,tno FROM tmp_get_more_totals WHERE CK>0 group by accno,tno;\n"
				+ "ALTER TABLE tmp2_get_more_totals ADD KEY(ACCNO),ADD KEY(TNO);\n"
				+ "\n"
				+ "update bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " T1 INNER JOIN tmp2_get_more_totals T2\n"
				+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO\n"
				+ "set t1.ttl=0, net=0,stt=0,sub=0\n"
				+ "WHERE (TTL=-1 OR NET=-1 OR STT=-1 OR SUB=-1);\n" + "\n");

		sb.append("/*delete any tables where there's no subtotal. 1st step finds tables that have subtotal (>0)*/"
				+ "\nDROP TABLE IF EXISTS TMP1"
				+ yr
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP1"
				+ yr
				+ q
				+ " ENGINE=myisam\n"
				+ "select count(*) cnt, t1.* from  "
				+ table
				+ "  t1 where col=1 and (ttl>=0 or stt>=0 or sub>=0 or net>=0) group by accno,tno;\n"
				+ "ALTER TABLE TMP1"
				+ yr
				+ q
				+ " ADD KEY(ACCNO), ADD KEY(TNO);\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP2"
				+ yr
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP2"
				+ yr
				+ q
				+ " ENGINE=myisam\n"
				+ "select count(*) cnt, t1.* from  "
				+ table
				+ "  t1 where col=1  group by accno,tno;\n"
				+ "ALTER TABLE TMP2"
				+ yr
				+ q
				+ " ADD KEY(ACCNO), ADD KEY(TNO);\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP3"
				+ yr
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP3"
				+ yr
				+ q
				+ " ENGINE=myisam\n"
				+ "SELECT ACCNO,TNO FROM (\n"
				+ "SELECT T2.ACCNO ACC2, T2.TNO TNO2,T1.* FROM TMP2"
				+ yr
				+ q
				+ " T1 LEFT JOIN TMP1"
				+ yr
				+ q
				+ " T2 ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO) T1\n"
				+ "WHERE ACC2 IS NULL ;\n"
				+ "ALTER TABLE TMP3"
				+ yr
				+ q
				+ " ADD KEY(ACCNO), ADD KEY(TNO);\n"
				+ "\n"
				+ "UPDATE IGNORE  "
				+ table
				+ "  T1 INNER JOIN (SELECT ACCNO, TNO FROM TMP3"
				+ yr
				+ q
				+ ") T2 \n"
				+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO\n"
				+ "SET T1.TTL=-1, T1.STT=-1, T1.NET=-1,T1.SUB=-1, yr='bad';\n\n"
				+ "\nDROP TABLE IF EXISTS TMP1"
				+ yr
				+ q
				+ ";\nDROP TABLE IF EXISTS TMP2"
				+ yr
				+ q
				+ ";\n"
				+ "DROP TABLE IF EXISTS TMP3" + yr + q + ";");

		sb.append("DROP TABLE IF EXISTS TMP_DELETE_"
				+ yr
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP_DELETE_"
				+ yr
				+ q
				+ " ENGINE=MYISAM\n"
				+ "select t1.*\n"
				+ "from (\n"
				+ "select count(distinct(trow)) tRowCnt,count(*) rowCnt,FILEDATE, accno,tno from "
				+ table
				+ " \n"
				+ "where abs(value)>=0\n"
				+ "group by accno,tno) t1 \n"
				+ "where (trowCnt<5 or rowCnt<8) ;\n"
				+ "ALTER TABLE TMP_DELETE_"
				+ yr
				+ q
				+ " ADD KEY (FILEDATE), ADD KEY(ACCNO),ADD KEY(TNO);\n"
				+ "DELETE T1 FROM "
				+ table
				+ " T1 INNER JOIN TMP_DELETE_"
				+ yr
				+ q
				+ " T2\n"
				+ "ON T1.FILEDATE=T2.FILEDATE AND T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO;\n"
				+ "DROP TABLE IF EXISTS TMP_DELETE_"
				+ yr
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP_DELETE_"
				+ yr
				+ q
				+ " ENGINE=MYISAM\n"
				+ "select t1.*\n"
				+ "from (\n"
				+ "select count(distinct(trow)) tRowCnt,count(*) rowCnt,FILEDATE, accno,tno from "
				+ table
				+ " \n"
				+ "group by accno,tno) t1 \n"
				+ "where tRowCnt<7 or rowCnt<12;\n"
				+ "ALTER TABLE TMP_DELETE_"
				+ yr
				+ q
				+ " ADD KEY (FILEDATE), ADD KEY(ACCNO),ADD KEY(TNO);\n"
				+ "DELETE T1 FROM "
				+ table
				+ " T1 INNER JOIN TMP_DELETE_"
				+ yr
				+ q
				+ " T2\n"
				+ "ON T1.FILEDATE=T2.FILEDATE AND T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO;\n"
				+ "DROP TABLE IF EXISTS TMP_DELETE_" + yr + ";\n");

		sb.append("END;");

		MysqlConnUtils.executeQuery(sb.toString());
		MysqlConnUtils.executeQuery("call markBadTables" + yr + "QTR" + q
				+ "();\n");
		sb.delete(0, sb.toString().length());

	}

	public void renameTables(String table) throws SQLException, FileNotFoundException {

		String yr = table.substring(table.indexOf("tp_raw") + 6,
				table.indexOf("tp_raw") + 10);
		String q = table.substring(table.indexOf("tp_raw") + 13,
				table.indexOf("tp_raw") + 14);

		// finds tablenames that are mislabeled as CF or BS or '' and relables
		// them as IS

		StringBuffer sb = new StringBuffer(
				"DROP PROCEDURE IF EXISTS renameTables"
						+ yr
						+ "QTR"
						+ q
						+ ";\n"
						+ "CREATE PROCEDURE renameTables"
						+ yr
						+ "QTR"
						+ q
						+ "()\n"
						+ "\n\nbegin\n\n"
						+ "DROP TABLE IF EXISTS TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "1;\n"
						+ "CREATE TABLE TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "1 ENGINE = MYISAM\n"
						+ "select ACCNO,TNO,'good' from  "
						+ table
						+ "  where \n"
						+ " (ROWNAME rlike 'sale|revenu|total interest income' ) AND rowname not rlike \n"
						+ " 'admin|average|excess|benefit|acquisit|due |rebate|value|estimat|eliminat|recogn|outstand"
						+ "|estate|provision|doubt|policy|payment|prepaid|sales? of|effect|deposit|promotion|percent|financ"
						+ "|book|notes|payabl|expendit|cash|common|stock|equival|inventory|%|property|maturit|debt|loss"
						+ "|advance|affiliate|amortiz|depreciat|license|discon|adjust|gain|chang|cost|leas|provid"
						+ "|non.{1,3}gaap|memb|margin|ratio |geograph|segment|impairment|unearn|non.{0,3}perform|decreas|increas"
						+ "|deferr|purchas|loss.{1,12}sale|accrue|receivabl|unbille|addition|(held|available).{1,2}(for|to)"
						+ "|investmen|subsidiar|proceed|marketing|whole|resale|misc|saleof|bond|mortgag|expens|internal' \n"
						+ " and tn!='is' and trow between 0 and 6 \n"
						+ "AND (columnText not like '%pro%forma%' or columnText is null)  \r"
						+ " and  (htmltxt='html' or (htmltxt!='html' and allColText not like '%pro%forma%' \n"
						+ "and ColumnPattern not like '%pro%forma%')) and (yr!='bad' or yr is null) \n"
						+ " group by accno,tno;\n"
						+ " ALTER TABLE TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "1 ADD KEY(ACCNO), ADD KEY(TNO);\n"
						+ "\n"
						+ "/*potential mislabeled -- tables that should be labled I/S i.e., tn='is'*/\n"
						+ "\n"
						+ " DROP TABLE IF EXISTS TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "2;\n"
						+ "CREATE TABLE TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "2 ENGINE = MYISAM\n"
						+ " select t1.accno,t1.tno,'good' from  "
						+ table
						+ "  t1 inner join tmp_rename_"
						+ yr
						+ "_"
						+ q
						+ "1 t2 on t1.accno=t2.accno and t1.tno=t2.tno\n"
						+ " where t1.p2 between 3 and 12 and (rowname rlike \n"
						+ "'gross margin|gross profit|cost.{1,5}expense|oustanding.{1,5}(share|stock)|cost.{1,5}(product|good)' or\n"
						+ " (rowname rlike 'operating income|Total interest income|net interest income' and rowname not rlike 'tax|distribu|adjustm|chang|decreas|increas|reconcil') )"
						+ "\ngroup by t1.accno,t1.tno;\n"
						+ " ALTER TABLE TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "2 ADD KEY(ACCNO), ADD KEY(TNO);\n"
						+ "\n"
						+ " DROP TABLE IF EXISTS TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "3;\n"
						+ "CREATE TABLE TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "3 ENGINE = MYISAM\n"
						+ " select t1.accno,t1.tno, 'bad' from  "
						+ table
						+ "  t1 inner join tmp_rename_"
						+ yr
						+ "_"
						+ q
						+ "1 t2 on t1.accno=t2.accno and t1.tno=t2.tno\n"
						+ " where rowname rlike 'Account.{1,3}(pay|rec)|Accrued	Accumulated|Additional P|assets|Cash and cas|Contingen|Construction|current asse|Current Liab|Current matu|Current port|Deferred|INTANGIBLE|Less Accumul|liabilities|long.{1,3}term debt|Marketable sec|Notes (pay|rec)|Preferred Stoc|Prepaid expe|Property.{1,5}(plant|equip)|shareholder|Short.{1,3}term (inv|debt)|stockholders|Treasury st' "
						+ "and rowname not rlike 'allowance|interest' group by t1.accno,t1.tno;\n"
						+ " ALTER TABLE TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "3 ADD KEY(ACCNO), ADD KEY(TNO);\n"
						+ "\n"
						+ " DROP TABLE IF EXISTS TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "4;\n"
						+ "CREATE TABLE TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "4 ENGINE = MYISAM\n"
						+ " select * from (\n"
						+ " select t1.accno,t1.tno,t2.accno acc2, t2.tno tno2 \n"
						+ " from tmp_rename_"
						+ yr
						+ "_"
						+ q
						+ "2 t1 left join tmp_rename_"
						+ yr
						+ "_"
						+ q
						+ "3 t2 on t1.accno=t2.accno and t1.tno=t2.tno) t1\n"
						+ " where acc2 is null;\n"
						+ " ALTER TABLE TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "4 ADD KEY(ACCNO), ADD KEY(TNO);\n"
						+ "\n"
						+ "UPDATE IGNORE  "
						+ table
						+ "  t1 inner join TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "4 t2 on t1.accno=t2.accno and t1.tno=t2.tno set tn='is';\n"
						+ "\n");

		sb.append("\nSET @A='1X'; SET @TNO=0; SET @P2=0; SET @E='1901-01-01'; SET @TR=0;\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "DROP TABLE IF EXISTS TMP_SAME_EDT"
				+ yr
				+ "qtr"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP_SAME_EDT"
				+ yr
				+ "qtr"
				+ q
				+ " \n"
				+ "/*SAME EDT,SAME P FOR SAME TNO AND SAME ACCNO. THEREFORE THEY ARE BAD*/"
				+ "\nSELECT \n"
				+ "row,CASE when @A=accno and @TNO=tNo and p2=@P2 and @TR=trow \n"
				+ "and datediff(greatest(@E,edt2),least(@E,edt2))<45 and @P2 between 3 and 12 and p2 between 3 and 12 THEN 1\n"
				+ "else 0 end bad,\n"
				+ "@A:=T1.AccNo AccNo, @TR:=T1.tRow tRow, @TNO:=T1.tNo tNo, @P2:=T1.p2 P2, @E:=T1.edt2 EDT2\n"
				+ "FROM  "
				+ table
				+ "  T1\n"
				+ "WHERE TC>1 and tn!='bs' and (tn='is' or tn='cf') ORDER BY ACCNO,TNO,ROW;\n"
				+ "ALTER TABLE TMP_SAME_EDT"
				+ yr
				+ "qtr"
				+ q
				+ " ADD KEY(BAD);\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP_SAME_EDT2"
				+ yr
				+ "qtr"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP_SAME_EDT2"
				+ yr
				+ "qtr"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "SELECT * FROM TMP_SAME_EDT"
				+ yr
				+ "qtr"
				+ q
				+ " WHERE BAD=1 ;\n"
				+ "ALTER TABLE TMP_SAME_EDT2"
				+ yr
				+ "qtr"
				+ q
				+ " ADD KEY(ROW),ADD KEY(ACCNO), ADD KEY(TNO), ADD KEY(TROW);\n"
				+ "\n"
				+ "UPDATE IGNORE  "
				+ table
				+ " T1 INNER JOIN TMP_SAME_EDT2"
				+ yr
				+ "qtr"
				+ q
				+ " T2 ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.TROW=T2.TROW SET YR='BAD' WHERE T1.ROW=T2.ROW;\n\n"
				+ "UPDATE IGNORE  "
				+ table
				+ " T1 INNER JOIN TMP_SAME_EDT2"
				+ yr
				+ "qtr"
				+ q
				+ " T2 ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.TROW=T2.TROW SET YR='BAD' WHERE T1.ROW=T2.ROW-1 ;\n"
				+ "\nEND;");

		MysqlConnUtils.executeQuery(sb.toString());
		MysqlConnUtils.executeQuery("call renameTables" + yr + "QTR" + q
				+ "();\n");

		StringBuffer sb2 = new StringBuffer(
				"set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "/*these go after rename table method because of tn filter*/\n"
						+ "DROP  TABLE  IF EXISTS TMP_"
						+ yr
						+ "_"
						+ q
						+ "_P2_ALLCOL;\n"
						+ "CREATE  TABLE TMP_"
						+ yr
						+ "_"
						+ q
						+ "_P2_ALLCOL ENGINE=MYISAM\n"
						+ "select accno,tno,row,p2,columnPattern,allcolText,tsLong,columnText from  bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ "  t1 where\n"
						+ "columnpattern rlike 'pCntD:1'\n"
						+ "and p2=0 and length(trim(edt2))=10 and (tn='is' or tn='cf')   AND allcolText NOT rlike '<FISCAL|<PERIOD'\n"
						+ "and yr!='bad';\n"
						+ "\n"
						+ "ALTER TABLE TMP_"
						+ yr
						+ "_"
						+ q
						+ "_P2_ALLCOL add key(columnPattern),ADD KEY(allcoltext),ADD KEY(tslong),ADD KEY(columnText);\n"
						+ "\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "DROP  TABLE  IF EXISTS TMP2_"
						+ yr
						+ "_"
						+ q
						+ "_P2_ALLCOL;\n"
						+ "CREATE TABLE TMP2_"
						+ yr
						+ "_"
						+ q
						+ "_P2_ALLCOL ENGINE=MYISAM\n"
						+ "select accno,tno,row,\n"
						+ "case \n"
						+ "when columnPattern rlike 'year|twelve|:12|52|fifty' then 12\n"
						+ "when columnPattern rlike 'nine|:9 |:3 quarter|thirty|three quarter' then 9\n"
						+ "when columnPattern rlike 'six |:6 |twenty' then 6\n"
						+ "when columnPattern rlike ':3 m|three|:quarter|:3rd|third|second|first|fourth|thirteen' then 3 \n"
						+ "else 0 end p3 from  TMP_"
						+ yr
						+ "_"
						+ q
						+ "_P2_ALLCOL  t1 where\n"
						+ "((columnPattern REGEXP 'year|twelve|:12|52|fifty')\n"
						+ "+ (columnPattern REGEXP 'nine|:9 |:3 quarter|thirty|three quarter')+ (columnPattern REGEXP 'six|:6 |twenty')\n"
						+ "+ (columnPattern REGEXP ':3 m|three|:quarter|:3rd|third|second|first|fourth|thirteen') \n"
						+ "+ (columnPattern REGEXP        ':ONE|:TWO|:FOUR|:FIVE|:SEVEN|:EIGH|:TEN|:FIFTEEN|:SIXTEEN|:SEVENTEEN|:EIGHTEEN|:NINETEEN|:10 |:15 |:1[7-9]{1} |:2[0-4]{1} |:29 |:3[0-5]{1} |:4[1-8]{1} ') \n"
						+ "+ (allColtext REGEXP        ':ONE|:TWO|:FOUR|:FIVE|:SEVEN|:EIGH|:TEN|:FIFTEEN|:SIXTEEN|:SEVENTEEN|:EIGHTEEN|:NINETEEN|:10 |:15 |:1[7-9]{1} |:2[0-4]{1} |:29 |:3[0-5]{1} |:4[1-8]{1} ') \n"
						+ "+ (tslong REGEXP        ':ONE|:TWO|:FOUR|:FIVE|:SEVEN|:EIGH|:TEN|:FIFTEEN|:SIXTEEN|:SEVENTEEN|:EIGHTEEN|:NINETEEN|:10 |:15 |:1[7-9]{1} |:2[0-4]{1} |:29 |:3[0-5]{1} |:4[1-8]{1} ') \n"
						+ "+ (columnText REGEXP    ':ONE|:TWO|:FOUR|:FIVE|:SEVEN|:EIGH|:TEN|:FIFTEEN|:SIXTEEN|:SEVENTEEN|:EIGHTEEN|:NINETEEN|:10 |:15 |:1[7-9]{1} |:2[0-4]{1} |:29 |:3[0-5]{1} |:4[1-8]{1} ') \n"
						+ ") = 1 and\n"
						+ "((columnPattern REGEXP 'MO')+ (columnPattern REGEXP 'WEEK') + (columnPattern REGEXP 'QUARTER')\n"
						+ "+ (columnPattern REGEXP 'YEAR') <=1) ;\n"
						+ "\n"
						+ "ALTER TABLE TMP2_"
						+ yr
						+ "_"
						+ q
						+ "_P2_ALLCOL add key(p3),ADD KEY(ACCNO),ADD KEY(TNO),ADD KEY(ROW);\n"
						+ "\n"
						+ "UPDATE IGNORE bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ " t1 inner join tmp2_"
						+ yr
						+ "_"
						+ q
						+ "_p2_allcol t2 on t1.AccNo=t2.accno and t1.tno=t2.tno \n"
						+ "and t1.row=t2.row\n"
						+ "SET p2=p3, mo='p2'\n"
						+ "where p2!=0;\n"
						+ "\n\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "UPDATE IGNORE bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ " set p2=12,mo='P2'\n"
						+ "where \n"
						+ "form rlike '10-k' and p2!=12 and p2!=3 and p2!=6 and p2!=9\n"
						+ "and (\n"
						+ "(tc=5 and columnPattern rlike 'yCntD:5') or\n"
						+ "(tc=4 and columnPattern rlike 'yCntD:4') or\n"
						+ "(tc=3 and columnPattern rlike 'yCntD:3') or\n"
						+ "(tc=2 and columnPattern rlike 'yCntD:2') \n"
						+ ") and (tn='is' or tn='cf') \n"
						+ "and (columnPattern rlike 'pCntD:1') and (p1=0 or p1=12)\n"
						+ "and columnPattern rlike 'year|5[0-4]{1} w|12 m';\n"
						+ "\n");

		String dropProc = "DROP PROCEDURE IF EXISTS updateP2FromColumnPattern"
				+ yr + "QTR" + q + ";\n"
				+ "CREATE PROCEDURE updateP2FromColumnPattern" + yr + "QTR" + q
				+ "()\n\n begin\n\n";
		String endProc = "\n\nend;";

		// ADD HERE DELETION OF LAST BATCH OF BAD TABLES
		String str = "\r\rinsert ignore into tp_raw_revised\r"
				+ "select * from " + table + ";";

		MysqlConnUtils.executeQuery(str);

		MysqlConnUtils.executeQuery(dropProc + sb2.toString() + endProc);
		sb.delete(0, sb.toString().length());
		// MysqlConnUtils.executeQuery("call updateP2FromColumnPattern" + yr
		// + "QTR" + q + "();\n");

	}

	public void prep_TP_Id(String table, int cikStart, int cikEnd, int cnt)
			throws SQLException, FileNotFoundException {

		/*
		 * NOTE: this is run after everything other than tp_sales_to_scrub has
		 * been run and loops through each bac_tp_rawYYYYQtrNo table. This
		 * populates prep_tp_Id from across all quarters so that later I can
		 * sort by cik,rowname,value and create unique ID across filings to
		 * fetch edts. This doesn't include tn='bs'.
		 */

		String yr = table.substring(table.indexOf("tp_raw") + 6,
				table.indexOf("tp_raw") + 10);
		String q = table.substring(table.indexOf("tp_raw") + 13,
				table.indexOf("tp_raw") + 14);

		StringBuffer sb = new StringBuffer();

		if (cnt == 0) {

			sb.append("DROP TABLE IF EXISTS prep_tp_Id;\n"
					+ "CREATE TABLE `prep_tp_Id` (\n"
					+ "  `AccNo` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `fileDate` date NOT NULL DEFAULT '1901-01-1',\n"
					+ "  `cik` int(11) DEFAULT NULL,\n"
					+ "  `tNo` int(5) NOT NULL DEFAULT '-1',\n"
					+ "  `row` tinyint(3) NOT NULL DEFAULT '-1',\n"
					+ "  `tn` varchar(6) DEFAULT NULL,\n"
					+ "  `col` TINYINT(3) DEFAULT NULL,\n"
					+ "  `tc` TINYINT(3) DEFAULT NULL,\n"
					+ "  `rowName` varchar(30) DEFAULT NULL,\n"
					+ "  `edt1` varchar(11) DEFAULT NULL COMMENT ' same as per2',\n"
					+ "  `p1` int(3) DEFAULT NULL COMMENT 'if html - per2 based on col hdg ratio matching, if txt based on idx alignments of each match',\n"
					+ "  `edt2` varchar(11) DEFAULT NULL COMMENT ' same as per2',\n"
					+ "  `p2` int(3) DEFAULT NULL COMMENT 'if html - per2 based on col hdg ratio matching, if txt based on idx alignments of each match',\n"
					+ "  `value` double(12,0) DEFAULT NULL,\n"
					+ "  `form` varchar(30) DEFAULT NULL,\n"
					+ "  `edt2_ck` tinyint(1) DEFAULT NULL,\n"
					+ "  `p2_ck` tinyint(1) DEFAULT NULL,\n"
					+ "   PRIMARY KEY `id` (ACCNO,TNO,ROW),\n"
					+ "  KEY (cik,value,rowname)\n"
					+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n" + "\n");
		}

		sb.append("\r\r/*Match across all historical filings of any cik. If there is a match of accno or cik \n"
				+ "(if different tables) AND value AND rowname are identical\n"
				+ "and there is a multiple account of good rowname type I can update a bad edt2 or missing p2 with the other table edt2/p2."
				+ " Step 1: \n"
				+ "is to put all filings into one table where yr!=bad. Mark if good edt (yyyy-mm-dd) or invalid \n"
				+ "(no value, just yyyy, etc) or p2=12,9,6 or 3 (good) or invalid p=0 then create Ids based \n"
				+ "on rowname,value and cik,rowname,value at createTp_id. These Ids can then be used to identify \n"
				+ "matching tables across time spans/accnos.*/\r\r"
				+ "DROP TABLE IF EXISTS TMPgetEdtFromAllTbls"
				+ yr
				+ "Qtr"
				+ q
				+ ";\n\n"
				+ "\n\nCREATE TABLE TMPgetEdtFromAllTbls"
				+ yr
				+ "Qtr"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "SELECT COUNT(DISTINCT(TROW)) trCnt,accno,tno FROM bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " WHERE COL>0 and abs(value)>101 AND LENGTH(ROWNAME)>2 and yr!='bad'"
				+ "/* AND length(cik)<6*/ and TN!='se' AND tn!='bs' AND cik between "
				+ cikStart
				+ " and "
				+ cikEnd
				+ "\n"
				+ " GROUP BY ACCNO,TNO;\n"
				+ "ALTER TABLE TMPgetEdtFromAllTbls"
				+ yr
				+ "Qtr"
				+ q
				+ " change trcnt trcnt int(5), change tno tno int(5),ADD KEY(ACCNO),ADD KEY(TNO),ADD KEY(trCnt);\n"
				+ "\n"
				+ "\n"
				+ "/*ck=0 means bad edt2 and bad p2, ck=1 means bad edt2, ck=2 means bad p2*/\n"

				+ "DROP TABLE IF EXISTS TMP_PREP_TP_ID"
				+ yr
				+ "_"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP_PREP_TP_ID"
				+ yr
				+ "_"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "SELECT t1.accno,t1.filedate,t1.cik,t1.tno,t1.row,t1.tn,t1.col,t1.tc,left(t1.rowname,30) rowname,t1.trow,T1.EDT1,T1.P1,t1.edt2,t1.p2\n"
				+ ",case when abs(t1.value)< 9999 then t1.value else round(left(abs(t1.value),5)) end value,form,\n"
				+ "case \n"
				+ "when t1.edt2 not rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})' then 0\n else 1 end edt2_ck"
				+ ",case \n"
				+ "when t1.p2<3 or t1.p2>12 then 0 else 1 end p2_ck \n"
				+ "/*t1.*/ FROM bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " t1 inner join TMPgetEdtFromAllTbls"
				+ yr
				+ "Qtr"
				+ q
				+ " t2\n"
				+ "on t1.accno=t2.accno and t1.tno=t2.tno\n"
				+ "WHERE col>0 and /*to get IS*/\n"
				+ " abs(t1.value)>101 and length(t1.rowname) >2 and trcnt>6 and t1.yr!='bad' "
				+ ";\n\n"
				+ " ALTER TABLE TMP_PREP_TP_ID"
				+ yr
				+ "_"
				+ q
				+ " ADD KEY(ROWNAME);\n"
				+ "\nINSERT IGNORE INTO prep_tp_Id\n\n"
				+ "select AccNo, fileDate, cik, tNo, row, tn, col, tc, rowName, edt1, p1, edt2, p2, value,form, edt2_ck, p2_ck \n"
				+ "from tmp_prep_tp_id"
				+ yr
				+ "_"
				+ q
				+ " t1 where (t1.rowname not rlike \n"
				+ "'capital|prefer|stock|issu|author|insur|unusual|availab|sale of|from sale|terminat|one.{1,3}time|cancel|loss on|gain on|number|addition"
				+ "|adjust|recur|class |distribut|redempt|earl"
				+ "|extra|litigat|patent|units|benefit|primary|balance|begin|period|end of|workin|lawsuit|gain|amortiz|accret|accrue|weight|outstand"
				+ "|equit|asset|merger|joint|originat|treasur"
				+ "|restruct|impairm|acqui|goodwil|short|share|reserv|special|intangib|write|recover|reimburs|divid|discontinu|consolid|redeem|dispos|closure"
				+ "|foreign|exchange|reorg|basic|dilute|debt|securit|bond|stock|derivativ|matur|long|current|payab|receiva|restat|borrow|funds|liabilit"
				+ "|retain|cash|deposi|loan|compensat|salar|cumulat|series|deferr|less|subsid|future|billion|million|thousand'\n )"
				+ " or ((ROWNAME RLIKE '(TOTAL) (NET |OPERATING )?(SALES|REVEN|cost|expense)' or ROWNAME RLIKE '(TOTAL|NET) (INTEREST|INVESTMENT) "
				+ "(INCOME|EXPENSE)'  OR ROWNAME RLIKE 'NET.{1,3}(INCOME|LOSS)' OR TRIM(ROWNAME) ='REVENUES' OR TRIM(ROWNAME) = 'REVENUE' "
				+ "OR TRIM(ROWNAME) = 'REVENU' OR TRIM(ROWNAME) ='REVENUES:' OR TRIM(ROWNAME) = 'REVENUE:'   OR TRIM(ROWNAME) ='SALES' OR TRIM(ROWNAME) ='SALES:' OR ROWNAME RLIKE 'NET( PRODUCT| SERVICE)? (SALES|REVEN)' OR ROWNAME RLIKE 'OPERATING REVEN'   or ROWNAME RLIKE 'COGS|COST.{1,2}OF (GOODS|SALES|REVEN)' or ROWNAME RLIKE 'COST.{0,2} AND EXPEN'    OR ( (length(rowname)<2 OR rowname rlike 'revenu|premium') and trow<6) ) )"
				+ ";\n");

		String dropProc = "DROP PROCEDURE IF EXISTS prep_tp_Id" + yr + "QTR"
				+ q + ";\n" + "CREATE PROCEDURE prep_tp_Id" + yr + "QTR" + q
				+ "()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
		sb.delete(0, sb.toString().length());
		MysqlConnUtils.executeQuery("call prep_tp_Id" + yr + "QTR" + q
				+ "();\n");
	}

	public void createTP_Id() throws SQLException, FileNotFoundException {

		StringBuffer sb = new StringBuffer();

		/*
		 * Runs after prep_tp_Id if finished and all bac_tp_rawYYYYQtrNo have
		 * been cycled through. Puts all data into tp_all_is_tables and each ID
		 * represents cik,rownam,value. Can be used later to pair across
		 * filings.
		 */

		sb.append("DROP TABLE IF EXISTS tp_Id;\n"
				+ "CREATE TABLE `tp_Id` (\n"
				+ "  `id` bigint(13) DEFAULT NULL COMMENT 'if same id the cik,value and rowname are the same',\n"
				+ "  `AccNo` varchar(20) NOT NULL DEFAULT '-1',\n"
				+ "  `filedate` varchar(10) CHARACTER SET utf8 DEFAULT NULL,\n"
				+ "  `cik` int(11) DEFAULT NULL,\n"
				+ "  `tNo` int(5) NOT NULL DEFAULT '-1',\n"
				+ "  `row` int(5) NOT NULL DEFAULT '-1',\n"
				+ "  `tn` varchar(6) DEFAULT NULL,\n"
				+ "  `tc` TINYINT(3) DEFAULT NULL COMMENT 'total number of data cols ',\n"
				+ "  `col` TINYINT(3) DEFAULT NULL COMMENT 'data col number in financial table',\n"
				+ "  `rowname` varchar(20) DEFAULT NULL,\n"
				+ "  `edt1` varchar(11) DEFAULT NULL COMMENT ' same as per2',\n"
				+ "  `p1` int(3) DEFAULT NULL COMMENT 'if html - per2 based on col hdg ratio matching, if txt based on idx alignments of each match',\n"
				+ "  `edt2` varchar(11) DEFAULT NULL COMMENT ' same as per2',\n"
				+ "  `p2` int(3) DEFAULT NULL COMMENT 'if html - per2 based on col hdg ratio matching, if txt based on idx alignments of each match',\n"
				+ "  `value` double(12,0) DEFAULT NULL,\n"
				+ "  `form` varchar(30) DEFAULT NULL,\n"
				+ "  `edt2_ck` tinyint(1) DEFAULT NULL,\n"
				+ "  `p2_ck` tinyint(1) DEFAULT NULL,\n"
				+ "  `cntEdt` tinyint(3) DEFAULT NULL,\n"
				+ "  `cntP` tinyint(3) DEFAULT NULL,\n"
				+ "  PRIMARY KEY (accno,tno,row),\n"
				+ "  KEY acc_tno_col (accno,tno,col),\n"
				+ "  KEY `accno` (`accno`),\n"
				+ "  KEY `cik` (`cik`),\n"
				+ "  KEY `id` (`id`),\n"
				+ "  KEY `edt2_ck` (`edt2_ck`),\n"
				+ "  KEY `tn` (`tn`),\n"
				+ "  KEY `edt2` (`edt2`),\n"
				+ "  KEY `p2` (`p2`),\n"
				+ "  KEY `p2_ck` (`p2_ck`),\n"
				+ "  KEY `col` (`col`),\n"
				+ "  KEY `tno` (`tno`)\n"
				+ "   COMMENT 'Id Allows FAST joins across accno to identify tables that are same (high no of same Ids)'\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n\n");

		sb.append("set @c:=0; set @rn='abc'; set @v=0; set @id:=0;\n"
				+ "drop table if exists tmp_tp_id;\n"
				+ "create table tmp_tp_id engine=myisam \n"
				+ "select \n"
				+ "@id:=case when @c=cik and @rn=rowname and @v=value then @id else @id+1 end id,\n"
				+ " t1.AccNo, left(t1.fileDate,10) filedate, @c:=t1.cik cik, t1.tNo,t1.row, t1.tn,t1.tc, t1.col, @rn:=t1.rowName rowname,T1.EDT1,T1.P1,t1.edt2,t1.p2\n"
				+ ", @v:=t1.value value, t1.form,t1.edt2_ck,t1.p2_ck\n"
				+ "from prep_tp_Id t1 where tn!='se' \n"
				+ "order by value,rowname,cik;\n"
				+ "ALTER TABLE TMP_TP_ID ADD KEY(ID), ADD KEY(EDT2), add key(p2);"
				+ "\n");

		sb.append("\nDROP TABLE IF EXISTS TMP_TP_ID_edt;\n"
				+ "CREATE TABLE TMP_TP_ID_edt ENGINE=MYISAM\n"
				+ "select count(*) cntEdt,t1.* from tmp_tp_id t1 group by id,edt2;\n"
				+ "ALTER TABLE TMP_TP_ID_edt ADD KEY(ID), ADD KEY(EDT2);\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP_TP_ID_p;\n"
				+ "CREATE TABLE TMP_TP_ID_p ENGINE=MYISAM\n"
				+ "select count(*) cntP,t1.* from tmp_tp_id t1 group by id,p2;\n"
				+ "ALTER TABLE TMP_TP_ID_p ADD KEY(ID), ADD KEY(P2);\n"
				+ "\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP2_TP_ID_edt;\n"
				+ "CREATE TABLE TMP2_TP_ID_edt ENGINE=MYISAM\n"
				+ "SELECT T1.*,T2.cntEdt FROM tmp_tp_id T1 INNER JOIN TMP_TP_ID_EDT T2 ON T1.ID=T2.ID AND T1.EDT2=T2.EDT2;\n"
				+ "ALTER TABLE TMP2_TP_ID_edt ADD KEY(ID), ADD KEY(P2);\n"
				+ "\n"
				+ "insert ignore into tp_id\n"
				+ "SELECT T1.*,T2.cntP FROM TMP2_TP_ID_edt T1 INNER JOIN TMP_TP_ID_P T2 ON T1.ID=T2.ID AND T1.P2=T2.P2;\n"
				+ "\n");

		String dropProc = "DROP PROCEDURE IF EXISTS createTP_Id;\n"
				+ "CREATE PROCEDURE createTP_Id()\n\n" + " begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
		sb.delete(0, sb.toString().length());

		MysqlConnUtils.executeQuery("call createTP_Id();\n");

	}

	public void getPeriodsConformEnddate(int startYr, int endYr, int startQ,
			int endQ) throws SQLException, FileNotFoundException {

		StringBuffer sb = new StringBuffer();

		sb.append("\n"
				+ "/*Below conform enddates and fixes a large percentage of enddates \n"
				+ "because in part I assume 30/28 day when no day value. \n"
				+ "After enddates are conformed the query will find missing periods \n"
				+ "- and finds approximately 30%. End resuls is 2.7 percent msg P c"
				+ "and 1.7p ercent msg Edt. Msg P found by seeing where p2=0 and p1!=0"
				+ "and then subtracting p1 val from another p1 or p2 value and see if result"
				+ "matchs another p1 or p2 val. If so - all p1 can be set to p2*/\n"
				+ "\n"
				+ "set @rw =0; set @rn='xzc'; set @cik=0; set @id=0; set @v=0;\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "DROP TABLE IF EXISTS TMP_CONFORM_ENDDATES;\n"
				+ "CREATE TABLE TMP_CONFORM_ENDDATES ENGINE=MYISAM\n"
				+ "SELECT @rw:=case when @cik=cik and @rn=left(trim(rowname),20) then @rw else @rw+1 end rw,accno,left(filedate,10) filedate ,@cik:=cik cik\n"
				+ ",tno,tn,tc,col,@rn:=left(trim(rowname),20) rowname\n"
				+ ",@v:=value value,p1,p2\n"
				+ "/*,CASE WHEN `DEC`=0 OR `DEC` IS NULL OR `DEC`='' THEN 1 WHEN `DEC`=-3 THEN 1000 WHEN `DEC` = -6 THEN 1000000 \n"
				+ "WHEN `DEC`=-9 THEN 1000000000 ELSE `DEC` END `DEC`*/\n"
				+ ",@mo:=case when length(trim(edt2))=10 then left(right(edt2,5),2) \n"
				+ "when length(replace(edt2,'-',''))=6  and right(replace(edt2,'-',''),2) between 1 and 12 then right(replace(edt2,'-',''),2) \n"
				+ "else 'xx' end mo\n"
				+ ",@dy:=case when length(trim(edt2))=10 then right(edt2,2) when @mo!=2 then '30' when @mo=2 then '28' else 'xx' end dy\n"
				+ ",@fDay:=case when @mo!=2 and (@dy>=15 or right(edt2,2)='00') then 30 when @mo=2 and (@dy>=15 or right(edt2,2)='00') then 28\n"
				+ "when @mo!=3 and @dy<15 and @mo!='xx' and @dy!='xx' then 30 \n"
				+ "when @mo=3 and @dy<15 and @mo!='xx' and @dy!='xx' then 28 else 'xx' end fDay\n"
				+ ",@fMo:=case when @dy='xx' then @mo when @dy>=15 or right(edt2,2)='00' then @mo \n"
				+ "when @dy!='xx' and @mo!='xx' and @dy<15 and @mo>1 and right(edt2,2)!='00' then @mo-1\n"
				+ "when @dy!='xx' and @mo!='xx' and @dy<15 and @mo<2 and right(edt2,2)!='00' then 12 else 'xx' end fMo\n"
				+ ",@fnlMo:=case when @fMo!='xx' and length(@fMo)=1 then concat('0',@fMo) else @fMo end fnlMo\n"
				+ ",@fnlDay:=case when @fDay!='xx' and length(@fDay)=1 then concat('0',@fDay) else @fDay end fnlDay\n"
				+ ",LEFT(concat(@fnlMo,'-',@fnlDay),5) q_end\n"
				+ ",@yr:=case when @dy!='xx' and @mo!='xx' and @mo<2 and @dy<15 then left(edt2,4)-1 else left(edt2,4) end yr\n"
				+ ",LEFT(CONCAT(@yr,'-',LEFT(concat(@fnlMo,'-',@fnlDay),5)),10) edt2,edt1,edt2_ck,p2_ck,cntEdt,cntP\n"
				+ "FROM TP_ID \n"
				+ "\n"
				+ "/*where cik between 780000 and 800000*/\n"
				+ "\n"
				+ "order by cik,rowname,value;\n"
				+ "\n"
				+ "/*I only need rw and key=cik,value,rowname*/\n"
				+ "ALTER TABLE TMP_CONFORM_ENDDATES \n"
				+ "DROP COLUMN FNLDAY, DROP COLUMN FNLMO, DROP COLUMN MO, DROP COLUMN DY, DROP COLUMN FMO, DROP COLUMN FDAY,DROP COLUMN Q_END,\n"
				+ "change edt2 edt2 varchar(10),change rw rw double,change value value double,change cik cik int(11),change rowname rowname varchar(20)\n"
				+ ",CHANGE P2 p2 TINYINT(3),CHANGE P1 p1 TINYINT(3), CHANGE FILEDATE filedate date, CHANGE edt1 edt1 VARCHAR(10)\n"
				+ ",add key(rw),add key(cik,value,rowname),add key(accno,tno,col),add key(cik),add key(p1),add key(p2),add key(edt2); \n"
				+ "/*do I need all these keys?*/\n"
				+ "\n"
				+ "\n"
				+ "/*2nd pass to update tmp_conform_enddates: this finds multiple instances where edt1 has same month by finding the same data values and rownames "
				+ "in 2 or more tables. The result is \n"
				+ "the edt1 where I can use the month value where it is missing in edt2*/\n"
				+ "\n"
				+ "set @dy:='00'; set @cik:=0; set @acc:='1x'; set @e:='1901-01-01'; set @tno:=0; set @v=0;\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP_GET_MONTH;\n"
				+ "CREATE TABLE TMP_GET_MONTH ENGINE=MYISAM\n"
				+ "select abs(right(edt1,2)-right(edt2,2)) df,\n"
				+ "case \n"
				+ "when @v=value and @acc!=accno and @cik=cik and left(@e,7)=left(edt1,7) then 1 \n"
				+ "when @v=value and (@tno!=tno or @acc!=accno) and @cik=cik and left(@e,7)!=left(edt1,7) then 2 else 0 end ck,\n"
				+ "\n"
				+ "/*@l:=concat('file:///c://backtest/tableparser/',year(t1.filedate),'/qtr',quarter(t1.filedate),'/tables/',t1.accno,'_',t1.tno,'.txt') l1,\n"
				+ "concat('select * from tp_raw_r.evised',right(left(@l,37),4),right(left(@l,42),4),' where accno=\'',accno,'\' and tno=',tno,' and abs(value)=',value,';') sl\n"
				+ ",*/\n@acc:=accno accno, @cik:=cik cik, @dy:=right(edt2,2) dy, @e:=edt1 edt1,edt2,@tno:=tno tno, @v:=value value,col,tc,p1,p2,rowname\n"
				+ "from TMP_CONFORM_ENDDATES t1 \n"
				+ "where \n"
				+ "(edt2 rlike '[0-9]{4}-xx-[0-9]{2}' or right(left(replace(edt2,'-',''),6),2) >12 \n"
				+ "or right(left(replace(edt2,'-',''),6),2) rlike '[a-z]') and (length(edt1)=10\n"
				+ "or edt1 rlike '[12]{1}[09]{1}[0-9]{2}-(0[0-9]{1}|1[12]{1})') and edt1 not rlike '[a-z]' \n"
				+ "and left(edt1,4)=left(edt2,4) and right(left(replace(edt1,'-',''),6),2) between 1 and 12\n"
				+ " order by cik,value,rowname ;\n"
				+ "ALTER TABLE TMP_GET_MONTH ADD KEY(ACCNO,TNO,COL);\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP2_GET_MONTH;\n"
				+ "CREATE TABLE TMP2_GET_MONTH ENGINE=MYISAM\n"
				+ "select max(ck) maxCk,t1.* from TMP_GET_MONTH \n"
				+ " t1 where ck>0 and (col=1 or col=tc) and (df<5 or df>27)\n"
				+ " group by accno,tno,col;\n"
				+ "ALTER TABLE TMP2_GET_MONTH ADD KEY(ACCNO,TNO,COL);\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP3_GET_MONTH;\n"
				+ "CREATE TABLE TMP3_GET_MONTH ENGINE=MYISAM\n"
				+ " SELECT T1.* FROM TMP_GET_MONTH T1 INNER JOIN TMP2_GET_MONTH T2 \n"
				+ " ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.COL=T2.COL\n"
				+ " WHERE t2.MAXCK=1;\n"
				+ "\n"
				+ "set @rw =0; set @rn='xzc'; set @cik=0; set @id=0; set @v=0;\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "DROP TABLE IF EXISTS TMP4_GET_MONTH ;\n"
				+ "CREATE TABLE TMP4_GET_MONTH  ENGINE=MYISAM\n"
				+ "SELECT @rw:=case when @cik=cik and @rn=left(trim(rowname),20) then @rw else @rw+1 end rw,accno,@cik:=cik cik\n"
				+ ",tno,tc,col,@rn:=left(trim(rowname),20) rowname\n"
				+ ",@v:=value value,p1,p2\n"
				+ "/*,CASE WHEN `DEC`=0 OR `DEC` IS NULL OR `DEC`='' THEN 1 WHEN `DEC`=-3 THEN 1000 WHEN `DEC` = -6 THEN 1000000 \n"
				+ "WHEN `DEC`=-9 THEN 1000000000 ELSE `DEC` END `DEC`*/\n"
				+ ",@mo:=case when right(left(edt1,7),2) between 1 and 12 then right(left(edt1,7),2)\n"
				+ "else 'xx' end mo\n"
				+ ",@dy:=case when length(trim(edt1))=10 then right(edt1,2) when @mo!=2 then '30' when @mo=2 then '28' else 'xx' end dy\n"
				+ ",@fDay:=case when @mo!=2 and (@dy>=15 or right(edt1,2)='00') then 30 when @mo=2 and (@dy>=15 or right(edt1,2)='00') then 28\n"
				+ "when @mo!=3 and @dy<15 and @mo!='xx' and @dy!='xx' then 30 \n"
				+ "when @mo=3 and @dy<15 and @mo!='xx' and @dy!='xx' then 28 else 'xx' end fDay\n"
				+ ",@fMo:=case when @dy='xx' then @mo when @dy>=15 or right(edt1,2)='00' then @mo \n"
				+ "when @dy!='xx' and @mo!='xx' and @dy<15 and @mo>1 and right(edt1,2)!='00' then @mo-1\n"
				+ "when @dy!='xx' and @mo!='xx' and @dy<15 and @mo<2 and right(edt1,2)!='00' then 12 else 'xx' end fMo\n"
				+ ",@fnlMo:=case when @fMo!='xx' and length(@fMo)=1 then concat('0',@fMo) else @fMo end fnlMo\n"
				+ ",@fnlDay:=case when @fDay!='xx' and length(@fDay)=1 then concat('0',@fDay) else @fDay end fnlDay\n"
				+ ",LEFT(concat(@fnlMo,'-',@fnlDay),5) q_end\n"
				+ ",@yr:=case when @dy!='xx' and @mo!='xx' and @mo<2 and @dy<15 then left(edt1,4)-1 else left(edt1,4) end yr\n"
				+ ",LEFT(CONCAT(@yr,'-',LEFT(concat(@fnlMo,'-',@fnlDay),5)),10) edt2,edt1\n"
				+ "FROM TMP3_GET_MONTH \n"
				+ "order by cik,rowname,value;\n"
				+ " alter table TMP4_GET_MONTH add key(accno,tno,col);\n"
				+ "\n"
				+ "/*requires two different accno and if same val and same rowname and cik and both edt1 are same - use mo for edt2.*/\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP5_GET_MONTH;\n"
				+ "CREATE TABLE TMP5_GET_MONTH ENGINE=MYISAM\n"
				+ "SELECT * FROM TMP4_GET_MONTH GROUP BY ACCNO,TNO,COL;\n"
				+ "ALTER TABLE TMP5_GET_MONTH ADD KEY(ACCNO,TNO,COL);\n"
				+ "\n"
				+ "update \n"
				+ "/*select t2.edt2,t1.* from*/\n"
				+ "tmp_conform_enddates t1 inner join  TMP5_GET_MONTH t2\n"
				+ "on t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col\n"
				+ "set t1.edt2=t2.edt2;\n"
				+ "\n"
				+ "/*2nd pass to update tmp_conform_enddates END*/\n"
				+ "\n"
				+ "/*Where p1=3|6|9|12 and p2=0 see if p1 is correct by subtracting it from p1 or p2 = 6|9|12.  If value can then be matched we know p1=p2.\n"
				+ "A. Matches where enddates are same and B. then where enddates are separated. In subsequent tables I filter out errors where edt and p\n"
				+ "do not match and where there aren't at least 3 matches.\n"
				+ "Mark accno,tno,col with c (confirmed) - so that if in next query there is a match - I join bac_tp_raw_yyyy\n"
				+ "Step 1: conform tp_id enddates and create cik rowname id, step 2: create calc query calculates other values.\n"
				+ "step 3: find matches to that value to confirm missing p2 -- expand*/\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP_confirm_PERIODS;\n"
				+ "CREATE TABLE TMP_confirm_PERIODS ENGINE=MYISAM\n"
				+ "select T1.ACCNO cAcc,T1.CIK/*,T1.FILEDATE,*/,T1.TNO cTno,T1.COL cCol,datediff(t1.edt2,t2.edt2) df,t1.p2 cP2,t1.p1 cP1,t1.edt2 cEdt2_1\n"
				+ ",t1.value cVal1,t2.value val2,t2.edt2 edt2_2,t2.p2 p2_2,t2.p1 p1_2,t2.accno acc_2,t2.tno tno_2,t2.col col_2,/*cVal1- val2=calc value to match in \n"
				+ "next query*/\n"
				+ "case \n"
				+ "when t1.edt2=t2.edt2 and t1.p1=12 and (t2.p2=9 or t2.p1=9) then date_sub(t1.edt2, interval 9 month)\n"
				+ "when t1.edt2=t2.edt2 and t1.p1=12 and (t2.p2=6 or t2.p1=6) then date_sub(t1.edt2, interval 6 month)\n"
				+ "when t1.edt2=t2.edt2 and t1.p1=12 and (t2.p2=3 or t2.p1=3) then date_sub(t1.edt2, interval 3 month)\n"
				+ "when t1.edt2=t2.edt2 and t1.p1=9 and (t2.p2=6 or t2.p1=6) then date_sub(t1.edt2, interval 6 month)\n"
				+ "when t1.edt2=t2.edt2 and t1.p1=9 and (t2.p2=3 or t2.p1=3) then date_sub(t1.edt2, interval 3 month)\n"
				+ "when t1.edt2=t2.edt2 and t1.p1=6 and (t2.p2=3 or t2.p1=3) then date_sub(t1.edt2, interval 3 month)\n"
				+ "when datediff(t1.edt2,t2.edt2) between 80 and 283 then binary t1.edt2\n"
				+ "/*gets where t1.p1=12 and t2.p2=3 and are 3 months apart*/\n"
				+ "else ''  end edtCalc,t1.value-t2.value valCalc,\n"
				+ "case when t2.p2!=0 then t1.p1-t2.p2 else t1.p1-t2.p1 end pCalc,t1.rowname\n"
				+ ",concat('file:///c://backtest/tableparser/',year(t1.filedate),'/qtr',quarter(t1.filedate),'/tables/',t1.accno,'_',t1.tno,'.txt') l1\n"
				+ ",concat('file:///c://backtest/tableparser/',year(t2.filedate),'/qtr',quarter(t2.filedate),'/tables/',t2.accno,'_',t2.tno,'.txt') l2\n"
				+ " from TMP_CONFORM_ENDDATES t1 inner join tmp_conform_endDates t2\n"
				+ "on t1.rw=t2.rw\n"
				+ "where\n"
				+ "\n"
				+ " t1.value>t2.value and\n"
				+ "t1.p2=0 and ( \n"
				+ "(( \n"
				+ "(t1.p1=12 and (t2.p2=9 or t2.p1=9)) or\n"
				+ "(t1.p1=12 and (t2.p2=6 or t2.p1=6)) or\n"
				+ "(t1.p1=12 and (t2.p2=3 or t2.p1=6)) or\n"
				+ "(t1.p1=9 and (t2.p2=6 or t2.p1=6)) or\n"
				+ "(t1.p1=9 and (t2.p2=3 or t2.p1=3)) or\n"
				+ "(t1.p1=6 and (t2.p2=3 or t2.p1=3)) \n"
				+ ") and t1.edt2=t2.edt2  )\n"
				+ "\n"
				+ "or\n"
				+ "\n"
				+ "((\n"
				+ "\n"
				+ "(t1.p1=12 and (t2.p2=9 or t2.p1=9)) or\n"
				+ "(t1.p1=9 and (t2.p2=6 or t2.p1=6)) or\n"
				+ "(t1.p1=6 and (t2.p2=3 or t2.p1=3)) \n"
				+ ") and datediff(t1.edt2,t2.edt2) between 81 and 101  )\n"
				+ "\n"
				+ "or\n"
				+ "\n"
				+ "((\n"
				+ "\n"
				+ "(t1.p1=12 and (t2.p2=6 or t2.p1=6)) or\n"
				+ "(t1.p1=9 and (t2.p2=3 or t2.p1=3)) \n"
				+ ") and datediff(t1.edt2,t2.edt2) between 170 and 194  )\n"
				+ "\n"
				+ "or\n"
				+ "\n"
				+ "((\n"
				+ "\n"
				+ "(t1.p1=12 and (t2.p2=3 or t2.p1=3)) \n"
				+ ") and datediff(t1.edt2,t2.edt2) between 263 and 283  )\n"
				+ ");\n"
				+ "\n"
				+ "ALTER TABLE TMP_confirm_PERIODS ADD KEY(CIK,VALCALC,ROWNAME),ADD KEY(cAcc),add key(cCol), add key(cTno)\n"
				+ ", CHANGE edtCalc edtCalc VARCHAR(10), ADD KEY(CIK), ADD KEY(ROWNAME), ADD KEY(valCalc), ADD KEY(edtCalc);\n"
				+ "\n"
				+ "/*NOTE: I assumed a day value of 30 where it was absent in original edt2 - so if when I match the edtCalc != t1.edt2\n"
				+ "then I know the cEdt2 should be adjusted by the same number of days that are between edtCalc and t1.edt2.\n"
				+ "'where ok=1 update -- cAcc,cTno,CCol p2 value in associated bac_tp_rawYYYYQtr table with pCalc.\n"
				+ "where no pEr or eEr and p2_2=0 then update acc_2,tno_2,col_2 by setting its p2 to to its p1 in its bac_tp_rawYYYYQtr.\n"
				+ "where no pEr or eEr and t1.p2=0 then update accCalc,tnoCalc and colCalc by setting its p2 to its p1 in its bac_tp_rawYYYYQtr. \n"
				+ "these will largely be duplicative - so insert each (after making case when similar to 'ok' into a table w/ primary key\n"
				+ "=acc,tno,col. Then cycle through to update - and I can use that as a way to ck against one linke - simpler.\n"
				+ "KEY:CHG TO 1=P1 ERROR,2=P2 ERROR,3=EDT ERROR*/\n"
				+ "\n"
				+ "set @acc='1x'; set @col=0; set @tno=-1; set @cnt=0;\n"
				+ "DROP TABLE IF EXISTS TMP_P2_UPD;\n"
				+ "CREATE TABLE TMP_P2_UPD ENGINE=MYISAM\n"
				+ "SELECT FILEDATE,\n"
				+ "@cnt:=case when @acc=cAcc and @tno=cTno and @col=cCol then 1+@cnt else 0 end cnt/*counts number of acc,tno,col matched*/\n"
				+ ",case when @acc!=cAcc or @tno != cTno or @col!=cCol then 1 else 0 end nw\n"
				+ ",case when @acc!=cAcc or @tno != cTno or @col!=cCol and @pR=0 and @eR=0 then 1 else 0 end ok\n"
				+ "/*1=P1 ERROR,2=P2 ERROR,3=EDT ERROR. */\n"
				+ ",@eR:=CASE WHEN DATEDIFF(GREATEST(EDTCALC,EDT2),LEAST(EDTCALC,EDT2))>53  THEN 3 ELSE 0 END eR\n"
				+ ",@pR:=case when t1.p2!=0 and pCalc!=t1.p2 then 2 when t1.p1!=0 and pCalc!=t1.p1 then 1 else 0 end pR\n"
				+ ",left(t1.rowname,10) rn\n"
				+ ",@acc:=cAcc cAcc,t1.cik,@tno:=cTno cTno,@col:=cCol cCol,cP2,cP1,cEdt2_1,cVal1,val2,edt2_2,p2_2,P1_2,valCalc,edtCalc,t1.edt2,t1.p2,t1.p1,pCalc\n"
				+ ",l1,l2\n"
				+ ",concat('file:///c://backtest/tableparser/',year(t1.filedate),'/qtr',quarter(t1.filedate),'/tables/',t1.accno,'_',t1.tno,'.txt') l3\n"
				+ ",acc_2,tno_2,col_2,t1.accno accCalc,t1.tno tnoCalc,t1.col colCalc,df\n"
				+ "FROM TMP_CONFORM_ENDDATES T1 INNER JOIN TMP_confirm_PERIODS T2\n"
				+ "ON T1.CIK=T2.CIK AND T1.ROWNAME=T2.ROWNAME and t1.value=t2.ValCalc \n"
				+ "/*incorporate dec val adjustment?*/\n"
				+ "WHERE (T1.P2!=0 or t1.p1!=0) AND DATEDIFF(GREATEST(EDTCALC,EDT2),LEAST(EDTCALC,EDT2))<600\n"
				+ "order by cAcc,cTno,cCol;\n"
				+ "\n"
				+ "set @rw=0;\n"
				+ "DROP TABLE IF EXISTS TMP2_P2_UPD;\n"
				+ "CREATE TABLE TMP2_P2_UPD ENGINE=MYISAM\n"
				+ "SELECT @rw:=@rw+1 rw,CACC ACCNO,CTNO TNO, CCOL COL, CEDT2_1 EDT2,CP1 P2,CVAL1 VALUE,pr,er,l1 link,'L1' FROM TMP_P2_UPD \n"
				+ "WHERE pR=0 and eR=0 ;\n"
				+ "\n"
				+ "set @rw=@rw+1000;\n"
				+ "INSERT IGNORE INTO TMP2_P2_UPD\n"
				+ "SELECT @rw:=@rw+1,ACC_2,TNO_2, COL_2, EDT2_2,p1_2,val2,pr,er,l2,'L2' FROM TMP_P2_UPD\n"
				+ "WHERE pR=0 and eR=0\n"
				+ "and p2_2=0;\n"
				+ "\n"
				+ "set @rw=@rw+1000;\n"
				+ "\n"
				+ "INSERT IGNORE INTO TMP2_P2_UPD\n"
				+ "SELECT @rw:=@rw+1,ACCcalc,TNOcalc, COLcalc, EDTcalc,pCalc,valCalc,pr,er,l3, 'L3' FROM TMP_P2_UPD\n"
				+ "WHERE pR=0 and eR=0 and p2=0 ;\n"
				+ "\n"
				+ "ALTER TABLE TMP2_P2_UPD ADD KEY(ACCNO,TNO,COL);\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP3_P2_UPD;\n"
				+ "CREATE TABLE TMP3_P2_UPD ENGINE=MYISAM\n"
				+ "SELECT count(*) cnt,T1.*\n"
				+ " FROM tmp2_p2_upd T1 group by accno,tno,col;\n"
				+ " ALTER TABLE TMP3_P2_UPD ADD KEY(CNT);\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP4_P2_UPD;\n"
				+ "CREATE TABLE TMP4_P2_UPD ENGINE=MYISAM\n"
				+ "SELECT T1.*\n"
				+ " FROM tmp3_p2_upd T1  WHERE CNT>2 ;\n"
				+ "ALTER TABLE TMP4_P2_UPD ADD PRIMARY KEY(ACCNO,TNO,COL);\n"
				+ "\n"
				+ "/*sum two tables. find all p6,p9 and p12 values by summing two p values (2 tbls)\n"
				+ "p6=p3+p3. p9: p3+p6, p6+p3. p12: 6+6,9+3,3+9*/\n"
				+ "\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "\n"
				+ "/*valCalc is matched in next query and that verifies t1.p1 (cP1) */\n"
				+ "DROP TABLE IF EXISTS TMP_confirm_PERIODS;\n"
				+ "CREATE TABLE TMP_confirm_PERIODS ENGINE=MYISAM\n"
				+ "select T1.ACCNO cAcc,T1.CIK/*,T1.FILEDATE,*/,T1.TNO cTno,T1.COL cCol,datediff(t1.edt2,t2.edt2) df,t1.p2 cP2,t1.p1 cP1,t1.edt2 cEdt2_1\n"
				+ ",t1.value cVal1,t2.value val2,t2.edt2 edt2_2,t2.p2 p2_2,t2.p1 p1_2,t2.accno acc_2,t2.tno tno_2,t2.col col_2\n"
				+ ",case \n"
				+ "when t1.p1=3 and (t2.p2=3 or t2.p1=3) then 6\n"
				+ "\n"
				+ "when t1.p1=3 and (t2.p2=6 or t2.p1=6) then 9\n"
				+ "when t1.p1=6 and (t2.p2=3 or t2.p1=3) then 9\n"
				+ "\n"
				+ "when t1.p1=3 and (t2.p2=9 or t2.p1=9) then 12\n"
				+ "when t1.p1=9 and (t2.p2=3 or t2.p1=3) then 12\n"
				+ "when t1.p1=6 and (t2.p2=6 or t2.p1=6) then 12\n"
				+ "else 0 end pCalc\n"
				+ ", t1.edt2 edtCalc\n"
				+ ",t1.value+t2.value valCalc\n"
				+ ",t1.rowname,concat('file:///c://backtest/tableparser/',year(t1.filedate),'/qtr',quarter(t1.filedate),'/tables/',t1.accno,'_',t1.tno,'.txt') l1\n"
				+ ",concat('file:///c://backtest/tableparser/',year(t2.filedate),'/qtr',quarter(t2.filedate),'/tables/',t2.accno,'_',t2.tno,'.txt') l2\n"
				+ " from TMP_CONFORM_ENDDATES t1 , tmp_conform_endDates t2 \n"
				+ "\n"
				+ "where t1.rw=t2.rw \n"
				+ "and t1.p2=0 and (\n"
				+ "   (/*p3+p3=calcP6*/t1.p1=3 and (t2.p2=3 or t2.p1=3) and datediff(t1.edt2,t2.edt2) between 80 and 102 )\n"
				+ "or (/*p3+p6=calcP9*/t1.p1=3 and (t2.p2=6 or t2.p1=6) and datediff(t1.edt2,t2.edt2) between 80 and 102 )\n"
				+ "or (/*p3+p6=calcP9*/t1.p1=6 and (t2.p2=3 or t2.p1=3) and datediff(t1.edt2,t2.edt2) between 170 and 194 )\n"
				+ "\n"
				+ "or (/*p3+p9=calcP12*/t1.p1=3 and (t2.p2=9 or t2.p1=9) and datediff(t1.edt2,t2.edt2) between 80 and 102 )\n"
				+ "or (/*p3+p9=calcP12*/t1.p1=9 and (t2.p2=3 or t2.p1=3) and datediff(t1.edt2,t2.edt2) between 260 and 286 )\n"
				+ "or (/*p6+p6=calcP12*/t1.p1=6 and (t2.p2=6 or t2.p1=6) and datediff(t1.edt2,t2.edt2) between 170 and 194 )\n"
				+ ") ;\n"
				+ "\n"
				+ "ALTER TABLE TMP_confirm_PERIODS ADD KEY(CIK,VALCALC,ROWNAME),ADD KEY(cAcc),add key(cCol), add key(cTno)\n"
				+ ", CHANGE edtCalc edtCalc VARCHAR(10), ADD KEY(CIK), ADD KEY(ROWNAME), ADD KEY(valCalc), ADD KEY(edtCalc);\n"
				+ "\n"
				+ "\n"
				+ "set @acc='1x'; set @col=0; set @tno=-1; set @cnt=0;\n"
				+ "DROP TABLE IF EXISTS TMP_P2_UPD;\n"
				+ "CREATE TABLE TMP_P2_UPD ENGINE=MYISAM\n"
				+ "SELECT FILEDATE,\n"
				+ "@cnt:=case when @acc=cAcc and @tno=cTno and @col=cCol then 1+@cnt else 0 end cnt/*counts number of acc,tno,col matched*/\n"
				+ ",case when @acc!=cAcc or @tno != cTno or @col!=cCol then 1 else 0 end nw\n"
				+ ",case when @acc!=cAcc or @tno != cTno or @col!=cCol and @pR=0 and @eR=0 then 1 else 0 end ok\n"
				+ "/*1=P1 ERROR,2=P2 ERROR,3=EDT ERROR*/\n"
				+ ",@eR:=CASE WHEN DATEDIFF(GREATEST(EDTCALC,EDT2),LEAST(EDTCALC,EDT2))>53  THEN 3 ELSE 0 END eR\n"
				+ ",@pR:=case when t1.p2!=0 and pCalc!=t1.p2 then 2 when t1.p1!=0 and pCalc!=t1.p1 then 1 else 0 end pR\n"
				+ ",left(t1.rowname,10) rn\n"
				+ ",@acc:=cAcc cAcc,t1.cik,@tno:=cTno cTno,@col:=cCol cCol,cP2,cP1,cEdt2_1,cVal1,val2,edt2_2,p2_2,P1_2,valCalc,edtCalc,t1.edt2,t1.p2,t1.p1,pCalc\n"
				+ ",l1,l2\n"
				+ ",concat('file:///c://backtest/tableparser/',year(t1.filedate),'/qtr',quarter(t1.filedate),'/tables/',t1.accno,'_',t1.tno,'.txt') l3\n"
				+ ",acc_2,tno_2,col_2,t1.accno accCalc,t1.tno tnoCalc,t1.col colCalc,df\n"
				+ "FROM TMP_CONFORM_ENDDATES T1 INNER JOIN TMP_confirm_PERIODS T2\n"
				+ "ON T1.CIK=T2.CIK AND T1.ROWNAME=T2.ROWNAME and t1.value=t2.ValCalc \n"
				+ "WHERE (T1.P2!=0 or t1.p1!=0) AND DATEDIFF(GREATEST(EDTCALC,EDT2),LEAST(EDTCALC,EDT2))<600\n"
				+ "order by cAcc,cTno,cCol;\n"
				+ "\n"
				+ "set @rw=0;\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP2_P2_UPD;\n"
				+ "CREATE TABLE TMP2_P2_UPD ENGINE=MYISAM\n"
				+ "SELECT @rw:=@rw+1, CACC ACCNO,CTNO TNO, CCOL COL, CEDT2_1 EDT2,CP1 P2,CVAL1 VALUE,pr,er,l1 link,'L1' FROM TMP_P2_UPD WHERE pR=0 and eR=0;\n"
				+ "\n"
				+ "set @rw=@rw+1000;\n"
				+ "INSERT IGNORE INTO TMP2_P2_UPD\n"
				+ "SELECT @rw:=@rw+1,ACC_2,TNO_2, COL_2, EDT2_2,p1_2,val2,pr,er,l2,'L2' FROM TMP_P2_UPD\n"
				+ "WHERE pR=0 and eR=0\n"
				+ "and p2_2=0;\n"
				+ "\n"
				+ "set @rw=@rw+1000;\n"
				+ "\n"
				+ "INSERT IGNORE INTO TMP2_P2_UPD\n"
				+ "SELECT @rw:=@rw+1,ACCcalc,TNOcalc, COLcalc, EDTcalc,pCalc,valCalc,pr,er,l3, 'L3' FROM TMP_P2_UPD\n"
				+ "WHERE pR=0 and eR=0\n"
				+ "and p2=0;\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP3_P2_UPD;\n"
				+ "CREATE TABLE TMP3_P2_UPD ENGINE=MYISAM\n"
				+ "SELECT count(*) cnt,T1.*\n"
				+ " FROM tmp2_p2_upd T1 group by accno,tno,col;\n"
				+ " ALTER TABLE TMP3_P2_UPD ADD KEY(CNT);\n"
				+ "\n"
				+ "insert ignore into TMP4_P2_UPD\n"
				+ "SELECT T1.*\n"
				+ " FROM tmp3_p2_upd T1  WHERE CNT>2 ;\n"
				+ "\n"
				+ "/*sum 3 tables (join 3tbls but 2 at a time): p9=p3+p3+3, p12=6+3+3|3+6+3|3+3+6*/\n"
				+ "\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "\n"
				+ "/*valCalc is matched in next query and that verifies t1.p1 (cP1) join 3tbls: p9=p3+p3+3, p12=6+3+3|3+6+3|3+3+6*/\n"
				+ "\n"
				+ "/*because I am adding current p value to confirm to other p values the enddate is that p value's enddate. I need to carry each\n"
				+ "v1, v2 and later v3 value in order to ck. Initial join is 2 tbls - p1 and p2 (p1 is being confirmed). I use the sum of those\n"
				+ "two values and add that to the value in the table I join next.*/\n"
				+ "DROP TABLE IF EXISTS TMP1_confirm_PERIODS;\n"
				+ "CREATE TABLE TMP1_confirm_PERIODS ENGINE=MYISAM\n"
				+ "select t1.rw,T1.CIK/*,T1.FILEDATE,*/\n"
				+ ",T1.ACCNO Acc_t1,T1.TNO TNO_t1,T1.COL Col_t1,t1.edt2 edt2_t1,t1.p2 p2_t1,t1.p1 p1_t1,t1.value value_t1\n"
				+ ",t2.ACCNO Acc_t2,t2.TNO TNO_t2,t2.COL Col_t2,t2.edt2 edt2_t2,t2.p2 p2_t2,t2.p1 p1_t2,t2.value value_t2\n"
				+ ",case \n"
				+ "when t1.p1=3 and (t2.p2=3 or t2.p1=3) then 9\n"
				+ "/*can also be that in tbl where I do next join t2.p2=6 even though placeholder is p9*/\n"
				+ "when t1.p1=6 and (t2.p2=3 or t2.p1=3) then 12\n"
				+ "when t1.p1=3 and (t2.p2=6 or t2.p1=6) then 12\n"
				+ "/*when t1.p1=3 and (t2.p2=3 or t2.p1=3) then 12 - this is duplicative*/\n"
				+ "else 0 end tmp_pCalc\n"
				+ ",t1.edt2 edtCalc,\n"
				+ " (t1.value+t2.value) tmp_valCalc\n"
				+ ",t1.rowname,concat('file:///c://backtest/tableparser/',year(t1.filedate),'/qtr',quarter(t1.filedate),'/tables/',t1.accno,'_',t1.tno,'.txt') l1\n"
				+ ",concat('file:///c://backtest/tableparser/',year(t2.filedate),'/qtr',quarter(t2.filedate),'/tables/',t2.accno,'_',t2.tno,'.txt') l2\n"
				+ "from TMP_CONFORM_ENDDATES t1 inner join tmp_conform_endDates t2 \n"
				+ "on t1.rw=t2.rw  \n"
				+ "and t1.p2=0 and (\n"
				+ "\n"
				+ "   (/*p3+p3+3=calcP9*/t1.p1=3 and  (t2.p2=3 or t2.p1=3) and datediff(t1.edt2,t2.edt2) between 80 and 102 ) or   \n"
				+ "   (/*p6+p3+p3=calcP12*/t1.p1=6 and (t2.p2=3 or t2.p1=3) and datediff(t1.edt2,t2.edt2) between 170 and 184) or   \n"
				+ "   (/*p3+p6+p3=calcP12*/t1.p1=3 and (t2.p2=6 or t2.p1=6) and datediff(t1.edt2,t2.edt2) between 80 and 102) ) ;\n"
				+ "\n"
				+ "ALTER TABLE TMP1_confirm_PERIODS ADD KEY(RW), ADD KEY(TMP_PCALC),ADD KEY(edt2_t2);\n"
				+ "\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP_confirm_PERIODS;\n"
				+ "CREATE TABLE TMP_confirm_PERIODS ENGINE=MYISAM\n"
				+ "select t1.*,t2.ACCNO Acc_t3,t2.TNO TNO_t3,t2.COL Col_t3,t2.edt2 edt2_t3,t2.p2 p2_t3,t2.p1 p1_t3,t2.value value_t3,\n"
				+ "/*<these are the 3 values used to get valCalc*/\n"
				+ "case \n"
				+ "when t1.tmp_pCalc=9 and  (t2.p1=3 or t2.p2=3) and datediff(t1.edt2_t2,t2.edt2) between 80 and 102 then 9\n"
				+ "when t1.tmp_pCalc=9 and  (t2.p1=6 or t2.p2=6) and datediff(t1.edt2_t2,t2.edt2) between 80 and 102 then 12\n"
				+ "when t1.tmp_pCalc=12 and (t2.p1=3 or t2.p2=3) and datediff(t1.edt2_t2,t2.edt2) between 80 and 102 then 12 \n"
				+ "when t1.tmp_pCalc=12 and (t2.p1=3 or t2.p2=3) and datediff(t1.edt2_t2,t2.edt2) between 170 and 184 then 12\n"
				+ "when t1.tmp_pCalc=12 and (t2.p1=6 or t2.p2=6) and datediff(t1.edt2_t2,t2.edt2) between 80 and 102 then 12 else '' end pCalc,\n"
				+ "t1.tmp_valCalc+t2.value valCalc/*t1.valCalc equals the sum of the first two p values from the tbl abv*/\n"
				+ ",concat('file:///c://backtest/tableparser/',year(t2.filedate),'/qtr',quarter(t2.filedate),'/tables/',t2.accno,'_',t2.tno,'.txt') l3\n"
				+ " from TMP1_confirm_PERIODS t1 inner join TMP_CONFORM_ENDDATES t2 on t1.rw=t2.rw where \n"
				+ "(t1.tmp_pCalc=9  and (t2.p1=3 or t2.p2=3) and datediff(t1.edt2_t2,t2.edt2) between 80 and 102) or\n"
				+ "(t1.tmp_pCalc=12 and (t2.p1=3 or t2.p2=3) and datediff(t1.edt2_t2,t2.edt2) between 80 and 102) or\n"
				+ "(t1.tmp_pCalc=12 and (t2.p1=3 or t2.p2=3) and datediff(t1.edt2_t2,t2.edt2) between 170 and 184) or\n"
				+ "(t1.tmp_pCalc=12 and (t2.p1=6 or t2.p2=6) and datediff(t1.edt2_t2,t2.edt2) between 80 and 102) ;\n"
				+ "ALTER TABLE TMP_confirm_PERIODS ADD KEY(CIK,VALCALC,ROWNAME),ADD KEY(acc_t1),add key(col_t1), add key(tno_t1)\n"
				+ ", CHANGE edtCalc edtCalc VARCHAR(10);\n"
				+ "\n"
				+ "set @acc='1x'; set @col=0; set @tno=-1; set @cnt=0;\n"
				+ "\n"
				+ "/*pCalc=p? and edtCalc=edt_t4*/\n"
				+ "DROP TABLE IF EXISTS TMP_P2_UPD;\n"
				+ "CREATE TABLE TMP_P2_UPD ENGINE=MYISAM\n"
				+ "SELECT /*FILEDATE,*/@cnt:=case when @acc=acc_t3 and @tno=tno_t3 and @col=col_t3 then 1+@cnt else 0 end cnt/*counts number of acc,tno,col matched*/\n"
				+ ",case when @acc!=acc_t3 or @tno != tno_t3 or @col!=col_t3 then 1 else 0 end nw\n"
				+ ",case when @acc!=acc_t3 or @tno != tno_t3 or @col!=col_t3 and @pR=0 and @eR=0 then 1 else 0 end ok\n"
				+ ",@eR:=CASE WHEN DATEDIFF(GREATEST(EDTCALC,EDT2),LEAST(EDTCALC,EDT2))>53  THEN 3 ELSE 0 END eR\n"
				+ ",@pR:=case when t1.p2!=0 and pCalc!=t1.p2 then 2 when t1.p1!=0 and pCalc!=t1.p1 then 1 else 0 end pR\n"
				+ ",left(t1.rowname,10) rn\n"
				+ ",@acc:=acc_t3 acc_t3,t1.cik,@tno:=tno_t3 tno_t3,@col:=col_t3 col_t3, edt2_t3,p2_t3,p1_t3,value_t3\n"
				+ ",valCalc,edtCalc,pCalc\n"
				+ ",Acc_t1,TNO_t1,Col_t1,edt2_t1,p2_t1,p1_t1,value_t1\n"
				+ ",Acc_t2,TNO_t2,Col_t2,edt2_t2,p2_t2,p1_t2,value_t2\n"
				+ ",accno Acc_t4,tno TNO_t4,col Col_t4,edt2 edt2_t4,p2 p2_t4,p1 p1_t4,value value_t4,\n"
				+ "l1,l2,l3,concat('file:///c://backtest/tableparser/',year(t1.filedate),'/qtr',quarter(t1.filedate),'/tables/',t1.accno,'_',t1.tno,'.txt') l4\n"
				+ "FROM TMP_CONFORM_ENDDATES T1 INNER JOIN TMP_confirm_PERIODS T2\n"
				+ "ON T1.CIK=T2.CIK AND T1.ROWNAME=T2.ROWNAME and t1.value=t2.ValCalc \n"
				+ "WHERE (T1.P2!=0 or t1.p1!=0) \n"
				+ "order by acc_t3,tno_t3,col_t3;\n"
				+ "\n"
				+ "set @rw=0;\n"
				+ "DROP TABLE IF EXISTS TMP2_P2_UPD;\n"
				+ "CREATE TABLE TMP2_P2_UPD ENGINE=MYISAM\n"
				+ "SELECT @rw:=@rw+1 rw,ACC_t1 accno,TNO_t1 tno, COL_t1 COL, EDT2_t1 EDT2,P1_t1 P2,VALUE_t1 value,pr,er,l1 link,'L1' FROM TMP_P2_UPD \n"
				+ "WHERE pR=0 and eR=0 ;\n"
				+ "\n"
				+ "set @rw=@rw+1000;\n"
				+ "INSERT IGNORE INTO TMP2_P2_UPD\n"
				+ "SELECT @rw:=@rw+1 rw,ACC_t2,TNO_t2, COL_t2, EDT2_t2 ,P1_t2,VALUE_t2,pr,er,l2 link,'L2' FROM TMP_P2_UPD \n"
				+ "WHERE pR=0 and eR=0 and p2_t2=0;\n"
				+ "\n"
				+ "set @rw=@rw+1000;\n"
				+ "INSERT IGNORE INTO TMP2_P2_UPD\n"
				+ "SELECT @rw:=@rw+1 rw,ACC_t3,TNO_t3, COL_t3, EDT2_t3 ,P1_t3,VALUE_t3,pr,er,l3 link,'L3' FROM TMP_P2_UPD \n"
				+ "WHERE pR=0 and eR=0 and p2_t3=0;\n"
				+ "\n"
				+ "set @rw=@rw+1000;\n"
				+ "\n"
				+ "INSERT IGNORE INTO TMP2_P2_UPD\n"
				+ "SELECT @rw:=@rw+1 rw,ACC_t4,TNO_t4, COL_t4, edt2_t4 ,p1_t4,VALUE_t4,pr,er,l4 link,'L4' FROM TMP_P2_UPD \n"
				+ "WHERE pR=0 and eR=0 and p2_t4=0;\n"
				+ "\n"
				+ "ALTER TABLE TMP2_P2_UPD ADD KEY(ACCNO,TNO,COL);\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP3_P2_UPD;\n"
				+ "CREATE TABLE TMP3_P2_UPD ENGINE=MYISAM\n"
				+ "SELECT count(*) cnt,T1.*\n"
				+ " FROM tmp2_p2_upd T1 group by accno,tno,col;\n"
				+ " ALTER TABLE TMP3_P2_UPD ADD KEY(CNT);\n"
				+ "\n"
				+ "insert ignore into TMP4_P2_UPD\n"
				+ "SELECT T1.*\n"
				+ " FROM tmp3_p2_upd T1  WHERE CNT>2 ;\n"
				+ "\n"
				+ "\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "\n"
				+ "/*valCalc is matched in next query and that verifies t1.p1 (cP1) join 3tbls: p9=p3+p3+3, p12=6+3+3|3+6+3|3+3+6*/\n"
				+ "\n"
				+ "/*because I am adding current p value to confirm to other p values the enddate is that p value's enddate. I need to carry each\n"
				+ "v1, v2 and later v3 value in order to ck. Initial join is 2 tbls - p1 and p2 (p1 is being confirmed). I use the sum of those\n"
				+ "two values and add that to the value in the table I join next.*/\n"
				+ "DROP TABLE IF EXISTS TMP1_confirm_PERIODS;\n"
				+ "CREATE TABLE TMP1_confirm_PERIODS ENGINE=MYISAM\n"
				+ "select t1.rw,T1.CIK/*,T1.FILEDATE,*/\n"
				+ ",T1.ACCNO Acc_t1,T1.TNO TNO_t1,T1.COL Col_t1,t1.edt2 edt2_t1,t1.p2 p2_t1,t1.p1 p1_t1,t1.value value_t1\n"
				+ ",t2.ACCNO Acc_t2,t2.TNO TNO_t2,t2.COL Col_t2,t2.edt2 edt2_t2,t2.p2 p2_t2,t2.p1 p1_t2,t2.value value_t2\n"
				+ ",case \n"
				+ "when t1.p1=3 and (t2.p2=3 or t2.p1=3) then 12\n"
				+ "else 0 end tmp_pCalc\n"
				+ ",t1.edt2 edtCalc,\n"
				+ " (t1.value+t2.value) tmp_valCalc\n"
				+ ",t1.rowname,concat('file:///c://backtest/tableparser/',year(t1.filedate),'/qtr',quarter(t1.filedate),'/tables/',t1.accno,'_',t1.tno,'.txt') l1\n"
				+ ",concat('file:///c://backtest/tableparser/',year(t2.filedate),'/qtr',quarter(t2.filedate),'/tables/',t2.accno,'_',t2.tno,'.txt') l2\n"
				+ "from TMP_CONFORM_ENDDATES t1 inner join tmp_conform_endDates t2 \n"
				+ "on t1.rw=t2.rw  \n"
				+ "and t1.p2=0 and (\n"
				+ "   (/*p3+p3+p3Pp3=calcP12*/t1.p1=3 and  (t2.p2=3 or t2.p1=3) and datediff(t1.edt2,t2.edt2) between 80 and 102 )   ) ;\n"
				+ "\n"
				+ "ALTER TABLE TMP1_confirm_PERIODS ADD KEY(RW), ADD KEY(TMP_PCALC),ADD KEY(edt2_t2);\n"
				+ "\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP2_confirm_PERIODS;\n"
				+ "CREATE TABLE TMP2_confirm_PERIODS ENGINE=MYISAM\n"
				+ "select t1.*,t2.ACCNO Acc_t3,t2.TNO TNO_t3,t2.COL Col_t3,t2.edt2 edt2_t3,t2.p2 p2_t3,t2.p1 p1_t3,t2.value value_t3,\n"
				+ "/*these are the 3 values used to get valCalc*/\n"
				+ "case \n"
				+ "when t1.tmp_pCalc=12 and (t2.p1=3 or t2.p2=3) then 12\n"
				+ "else 0 end tmp_pCalc2,\n"
				+ "t1.tmp_valCalc+t2.value tmp_valCalc2/*t1.valCalc equals the sum of the first two p values from the tbl abv*/\n"
				+ ",concat('file:///c://backtest/tableparser/',year(t2.filedate),'/qtr',quarter(t2.filedate),'/tables/',t2.accno,'_',t2.tno,'.txt') l3\n"
				+ " from TMP1_confirm_PERIODS t1 inner join TMP_CONFORM_ENDDATES t2 on t1.rw=t2.rw where \n"
				+ "(t1.tmp_pCalc=12  and (t2.p1=3 or t2.p2=3) and datediff(t1.edt2_t2,t2.edt2) between 80 and 102) ;\n"
				+ "ALTER TABLE TMP2_confirm_PERIODS ADD KEY(RW), ADD KEY(TMP_PCALC2),ADD KEY(edt2_t3);\n"
				+ "\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP_confirm_PERIODS;\n"
				+ "CREATE TABLE TMP_confirm_PERIODS ENGINE=MYISAM\n"
				+ "select  t1.*,t2.ACCNO Acc_t4,t2.TNO TNO_t4,t2.COL Col_t4,t2.edt2 edt2_t4,t2.p2 p2_t4,t2.p1 p1_t4,t2.value value_t4,\n"
				+ "/*<these are the 3 values used to get valCalc*/\n"
				+ "case \n"
				+ "when t1.tmp_pCalc2=12 and (t2.p1=3 or t2.p2=3) then 12\n"
				+ "else 0 end pCalc,\n"
				+ "t1.tmp_valCalc2+t2.value valCalc/*t1.valCalc equals the sum of the first two p values from the tbl abv*/\n"
				+ ",concat('file:///c://backtest/tableparser/',year(t2.filedate),'/qtr',quarter(t2.filedate),'/tables/',t2.accno,'_',t2.tno,'.txt') l4\n"
				+ " from TMP2_confirm_PERIODS t1 inner join TMP_CONFORM_ENDDATES t2 on t1.rw=t2.rw where \n"
				+ "(t1.tmp_pCalc2=12  and (t2.p1=3 or t2.p2=3) and datediff(t1.edt2_t3,t2.edt2) between 80 and 102) ;\n"
				+ "ALTER TABLE TMP_confirm_PERIODS ADD KEY(CIK,VALCALC,ROWNAME),ADD KEY(acc_t1),add key(col_t1), add key(tno_t1)\n"
				+ ", CHANGE edtCalc edtCalc VARCHAR(10);\n"
				+ "\n"
				+ "set @acc='1x'; set @col=0; set @tno=-1; set @cnt=0;\n"
				+ "\n"
				+ "/*pCalc=p? and edtCalc=edt_t4*/\n"
				+ "DROP TABLE IF EXISTS TMP_P2_UPD;\n"
				+ "CREATE TABLE TMP_P2_UPD ENGINE=MYISAM\n"
				+ "SELECT /*FILEDATE,*/@cnt:=case when @acc=acc_t4 and @tno=tno_t4 and @col=col_t4 then 1+@cnt else 0 end cnt/*counts number of acc,tno,col matched*/\n"
				+ ",case when @acc!=acc_t4 or @tno != tno_t4 or @col!=col_t4 then 1 else 0 end nw\n"
				+ ",case when @acc!=acc_t4 or @tno != tno_t4 or @col!=col_t4 and @pR=0 and @eR=0 then 1 else 0 end ok\n"
				+ ",@eR:=CASE WHEN DATEDIFF(GREATEST(EDTCALC,EDT2),LEAST(EDTCALC,EDT2))>53  THEN 3 ELSE 0 END eR\n"
				+ ",@pR:=case when t1.p2!=0 and pCalc!=t1.p2 then 2 when t1.p1!=0 and pCalc!=t1.p1 then 1 else 0 end pR\n"
				+ ",left(t1.rowname,10) rn\n"
				+ ",@acc:=acc_t4 acc_t4,t1.cik,@tno:=tno_t4 tno_t4,@col:=col_t4 col_t4, edt2_t4,p2_t4,p1_t4,value_t4\n"
				+ ",valCalc,edtCalc,pCalc\n"
				+ ",Acc_t1,TNO_t1,Col_t1,edt2_t1,p2_t1,p1_t1,value_t1\n"
				+ ",Acc_t2,TNO_t2,Col_t2,edt2_t2,p2_t2,p1_t2,value_t2\n"
				+ ",Acc_t3,TNO_t3,Col_t3,edt2_t3,p2_t3,p1_t3,value_t3\n"
				+ ",accno Acc_t5,tno TNO_t5,col Col_t5,edt2 edt2_t5,p2 p2_t5,p1 p1_t5,value value_t5\n"
				+ ",l1,l2,l3,l4,concat('file:///c://backtest/tableparser/',year(t1.filedate),'/qtr',quarter(t1.filedate),'/tables/',t1.accno,'_',t1.tno,'.txt') l5\n"
				+ "FROM TMP_CONFORM_ENDDATES T1 INNER JOIN TMP_confirm_PERIODS T2\n"
				+ "ON T1.CIK=T2.CIK AND T1.ROWNAME=T2.ROWNAME and t1.value=t2.ValCalc \n"
				+ "WHERE (T1.P2!=0 or t1.p1!=0) \n"
				+ "order by acc_t4,tno_t4,col_t4;\n"
				+ "\n"
				+ "set @rw=0;\n"
				+ "DROP TABLE IF EXISTS TMP2_P2_UPD;\n"
				+ "CREATE TABLE TMP2_P2_UPD ENGINE=MYISAM\n"
				+ "SELECT @rw:=@rw+1 rw,ACC_t1 accno,TNO_t1 tno, COL_t1 COL, EDT2_t1 EDT2,P1_t1 P2,VALUE_t1 value,pr,er,l1 link,'L1' FROM TMP_P2_UPD \n"
				+ "WHERE pR=0 and eR=0;\n"
				+ "\n"
				+ "set @rw=@rw+1000;\n"
				+ "INSERT IGNORE INTO TMP2_P2_UPD\n"
				+ "SELECT @rw:=@rw+1 rw,ACC_t2,TNO_t2, COL_t2, EDT2_t2 ,P1_t2,VALUE_t2,pr,er,l2 link,'L2' FROM TMP_P2_UPD \n"
				+ "WHERE pR=0 and eR=0 and p2_t2=0;\n"
				+ "\n"
				+ "set @rw=@rw+1000;\n"
				+ "INSERT IGNORE INTO TMP2_P2_UPD\n"
				+ "SELECT @rw:=@rw+1 rw,ACC_t3,TNO_t3, COL_t3, EDT2_t3 ,P1_t3,VALUE_t3,pr,er,l3 link,'L3' FROM TMP_P2_UPD \n"
				+ "WHERE pR=0 and eR=0 and p2_t3=0;\n"
				+ "\n"
				+ "set @rw=@rw+1000;\n"
				+ "\n"
				+ "INSERT IGNORE INTO TMP2_P2_UPD\n"
				+ "SELECT @rw:=@rw+1 rw,ACC_t4,TNO_t4, COL_t4, edt2_t4 ,p1_t4,VALUE_t4,pr,er,l4 link,'L4' FROM TMP_P2_UPD \n"
				+ "WHERE pR=0 and eR=0 and p2_t4=0;\n"
				+ "\n"
				+ "INSERT IGNORE INTO TMP2_P2_UPD\n"
				+ "SELECT @rw:=@rw+1 rw,ACC_t5,TNO_t5, COL_t5, edt2_t5 ,p1_t5,VALUE_t5,pr,er,l5 link,'L5' FROM TMP_P2_UPD \n"
				+ "WHERE pR=0 and eR=0 and p2_t4=0;\n"
				+ "\n"
				+ "ALTER TABLE TMP2_P2_UPD ADD KEY(ACCNO,TNO,COL);\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP3_P2_UPD;\n"
				+ "CREATE TABLE TMP3_P2_UPD ENGINE=MYISAM\n"
				+ "SELECT count(*) cnt,T1.*\n"
				+ " FROM tmp2_p2_upd T1 group by accno,tno,col;\n"
				+ " ALTER TABLE TMP3_P2_UPD ADD KEY(CNT);\n"
				+ "\n"
				+ "insert ignore into TMP4_P2_UPD\n"
				+ "SELECT T1.*\n"
				+ " FROM tmp3_p2_upd T1  WHERE CNT>2 ;\n"
				+ "\n"
				+ "\n"
				+ "UPDATE \n"
				+ "/*SELECT T1.P2,T2.P2,T1.* FROM */\n"
				+ "TMP_CONFORM_ENDDATES T1 INNER JOIN TMP4_P2_UPD T2 ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.COL=T2.COL\n"
				+ "SET T1.P2=T2.P2;\n"
				+ "\n"
				+ "\n"
				+ "\nDROP TABLE IF EXISTS TMP_CONFORMED_ENDDATES_GRP;\n"
				+ "CREATE TABLE TMP_CONFORMED_ENDDATES_GRP ENGINE=MYISAM\n"
				+ "select * from tmp_conform_enddates group by accno,tno,col;\n"
				+ "ALTER TABLE TMP_CONFORMED_ENDDATES_GRP ADD KEY(ACCNO,TNO,COL);\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP_TP_ID;\n"
				+ "CREATE TABLE TMP_TP_ID\n"
				+ "select T1.id, T1.AccNo, T1.filedate, T1.cik, T1.tNo, T1.row, T1.tn, T1.tc, T1.col, T1.rowname, T1.edt1, T1.p1, t2.edt2, t2.p2, T1.value, T1.form, T1.edt2_ck, T1.p2_ck, T1.cntEdt, T1.cntP\n"
				+ "from tp_id t1 inner join TMP_CONFORMED_ENDDATES_GRP T2 ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.COL=T2.COL;\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TP_ID;\n"
				+ "CREATE TABLE `tp_id` (\n"
				+ "  `id` bigint(13) DEFAULT NULL COMMENT 'if same id the cik,value and rowname are the same',\n"
				+ "  `AccNo` varchar(20) NOT NULL DEFAULT '-1',\n"
				+ "  `filedate` varchar(10) CHARACTER SET utf8 DEFAULT NULL,\n"
				+ "  `cik` int(11) DEFAULT NULL,\n"
				+ "  `tNo` int(5) NOT NULL DEFAULT '-1',\n"
				+ "  `row` int(5) NOT NULL DEFAULT '-1',\n"
				+ "  `tn` varchar(6) DEFAULT NULL,\n"
				+ "  `tc` tinyint(3) DEFAULT NULL COMMENT 'total number of data cols ',\n"
				+ "  `col` tinyint(3) DEFAULT NULL COMMENT 'data col number in financial table',\n"
				+ "  `rowname` varchar(20) DEFAULT NULL,\n"
				+ "  `edt1` varchar(11) DEFAULT NULL COMMENT ' same as per2',\n"
				+ "  `p1` int(3) DEFAULT NULL COMMENT 'if html - per2 based on col hdg ratio matching, if txt based on idx alignments of each match',\n"
				+ "  `edt2` varchar(11) DEFAULT NULL COMMENT ' same as per2',\n"
				+ "  `p2` int(3) DEFAULT NULL COMMENT 'if html - per2 based on col hdg ratio matching, if txt based on idx alignments of each match',\n"
				+ "  `value` double(12,0) DEFAULT NULL,\n"
				+ "  `form` varchar(30) DEFAULT NULL,\n"
				+ "  `edt2_ck` tinyint(1) DEFAULT NULL,\n"
				+ "  `p2_ck` tinyint(1) DEFAULT NULL,\n"
				+ "  `cntEdt` tinyint(3) DEFAULT NULL,\n"
				+ "  `cntP` tinyint(3) DEFAULT NULL,\n"
				+ "  PRIMARY KEY (`AccNo`,`tNo`,`row`),\n"
				+ "  KEY `atc` (AccNo,tno,col),\n"
				+ "  KEY `accno` (`AccNo`),\n"
				+ "  KEY `cik` (`cik`),\n"
				+ "  KEY `id` (`id`),\n"
				+ "  KEY `edt2_ck` (`edt2_ck`),\n"
				+ "  KEY `tn` (`tn`),\n"
				+ "  KEY `edt2` (`edt2`),\n"
				+ "  KEY `p2` (`p2`),\n"
				+ "  KEY `p2_ck` (`p2_ck`),\n"
				+ "  KEY `col` (`col`),\n"
				+ "  KEY `tno` (`tNo`) COMMENT 'Id Allows FAST joins across accno to identify tables that are same (high no of same Ids)'\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n" + "\n"
				+ "insert ignore into tp_id\n" + "select * from tmp_tp_id;\n");

		String dropProc = "DROP PROCEDURE IF EXISTS getPeriodsConformEnddate"
				+ startYr + "_" + startQ + ";\n"
				+ "CREATE PROCEDURE getPeriodsConformEnddate" + startYr + "_"
				+ startQ + "()\n\n" + " begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
		sb.delete(0, sb.toString().length());

		MysqlConnUtils.executeQuery("call getPeriodsConformEnddate" + startYr
				+ "_" + startQ + "();\n");

		String table = "";
		int qtr = startQ;
		int q = startQ;
		for (int yr = startYr; yr <= endYr; yr++) {
			if (yr == 1993 && qtr < 3) {
				qtr = 3;
			}
			for (q = qtr; q <= endQ; q++) {
				table = "bac_tp_raw" + yr + "qtr" + q;

				sb.append(" update "
						+ table
						+ " t1 INNER JOIN TMP4_P2_UPD T2 \n"
						+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.COL=T2.COL\n"
						+ "SET T1.P2=T2.P2;\n");

				sb.append("update "
						+ table
						+ " t1 INNER JOIN TMP3_GET_MONTH T2 \n"
						+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.COL=T2.COL\n"
						+ "SET T1.EDT2=T2.EDT1;\n");

				dropProc = "DROP PROCEDURE IF EXISTS getPeriodsConformEnddate2"
						+ yr + "_" + q + ";\n"
						+ "CREATE PROCEDURE getPeriodsConformEnddate2" + yr
						+ "_" + q + "()\n\n" + " begin\n\n";
				endProc = "\n\nend;";

				MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
				sb.delete(0, sb.toString().length());

				MysqlConnUtils.executeQuery("call getPeriodsConformEnddate2"
						+ yr + "_" + q + "();\n");
				sb.delete(0, sb.toString().length());

			}
			qtr = 1;
		}
	}

	public void tp_sales_to_scrub(String table, int cikStart, int cikEnd,
			String tp_sales_to_scrub, int cnt) throws SQLException,
			FileNotFoundException {
		/*
		 * READ THIS!!!!!! IN ORDER TO RETAIN AS MUCH ACCURACY AS POSSIBLE:
		 * TP_SALES WILL ONLY GRAB THE HIGHEST VALUE GROUP BY ACCNO. CANNOT GET
		 * HHV FOR EACH tn W/O CORRUPTING DUE MULTIPLE CO F/S IN SAME FILING!!!
		 * IN ORDER TO BE ABLE TRACK WHAT WAS CHANGED LATER I CAN'T CHANGE
		 * VALUES -I ONLY CHANGE p2/edt2 !!!
		 */

		String yr = table.substring(table.indexOf("tp_raw") + 6,
				table.indexOf("tp_raw") + 10);
		String q = table.substring(table.indexOf("tp_raw") + 13,
				table.indexOf("tp_raw") + 14);

		String qry = "\n";

		if (cnt == 0) { // will regenerate if cnt==0
			qry = "DROP TABLE IF EXISTS `stockanalyser`.`tp_sales_to_scrub_hold`;\n"
					+ "CREATE TABLE `tp_sales_to_scrub_hold` (\n"
					+ "  `accno` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `fileDate` datetime NOT NULL DEFAULT '1901-01-01 00:00:00',\n"
					+ "  `cik` int(11) DEFAULT NULL,\n"
					+ "  `tn` varchar(6) DEFAULT NULL,\n"
					+ "  `trow` TINYINT(3) DEFAULT NULL COMMENT 'table line number in financial table',\n"
					+ "  `row` int(5) NOT NULL DEFAULT '-1' COMMENT 'row count in mysql ',\n"
					+ "  `col` TINYINT(3) NOT NULL DEFAULT '0' COMMENT 'data col number in financial table',\n"
					+ "  `tno` int(5) NOT NULL DEFAULT '-1',\n"
					+ "  `rowname` varchar(125) DEFAULT NULL,\n"
					+ "  `value` double(23,5) DEFAULT NULL,\n"
					+ "  `p2` int(3) NOT NULL DEFAULT '0' COMMENT 'if html - per2 parsed from cell, if txt per2 parsed based on col hdg ratio matching',\n"
					+ "  `edt2` varchar(10) NOT NULL DEFAULT '1901-01-1',\n"
					+ "  `DEC` int(11) DEFAULT NULL,\n"
					+ "  `columnText` varchar(200) CHARACTER SET utf8 DEFAULT NULL,\n"
					+ "  `form` varchar(15) DEFAULT NULL COMMENT 'this will equal rowratioBeforeColumnUtil if generic in htmlTxt field',\n"
					+ "  PRIMARY KEY (`accno`,`tno`,`fileDate`,`edt2`,`p2`),\n"
					+ "  KEY `fileDate` (`fileDate`)\n"
					+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1\n"
					+ "/*!50100 PARTITION BY RANGE ( YEAR(filedate))\n"
					+ "(PARTITION p0 VALUES LESS THAN (1994) ENGINE = MyISAM,\n"
					+ " PARTITION p1 VALUES LESS THAN (1995) ENGINE = MyISAM,\n"
					+ " PARTITION p2 VALUES LESS THAN (1996) ENGINE = MyISAM,\n"
					+ " PARTITION p3 VALUES LESS THAN (1997) ENGINE = MyISAM,\n"
					+ " PARTITION p4 VALUES LESS THAN (1998) ENGINE = MyISAM,\n"
					+ " PARTITION p5 VALUES LESS THAN (1999) ENGINE = MyISAM,\n"
					+ " PARTITION p6 VALUES LESS THAN (2000) ENGINE = MyISAM,\n"
					+ " PARTITION p7 VALUES LESS THAN (2001) ENGINE = MyISAM,\n"
					+ " PARTITION p8 VALUES LESS THAN (2002) ENGINE = MyISAM,\n"
					+ " PARTITION p9 VALUES LESS THAN (2003) ENGINE = MyISAM,\n"
					+ " PARTITION p10 VALUES LESS THAN (2004) ENGINE = MyISAM,\n"
					+ " PARTITION p11 VALUES LESS THAN (2005) ENGINE = MyISAM,\n"
					+ " PARTITION p12 VALUES LESS THAN (2006) ENGINE = MyISAM,\n"
					+ " PARTITION p13 VALUES LESS THAN (2007) ENGINE = MyISAM,\n"
					+ " PARTITION p14 VALUES LESS THAN (2008) ENGINE = MyISAM,\n"
					+ " PARTITION p15 VALUES LESS THAN (2009) ENGINE = MyISAM,\n"
					+ " PARTITION p16 VALUES LESS THAN (2010) ENGINE = MyISAM,\n"
					+ " PARTITION p17 VALUES LESS THAN (2011) ENGINE = MyISAM,\n"
					+ " PARTITION p18 VALUES LESS THAN (2012) ENGINE = MyISAM,\n"
					+ " PARTITION p19 VALUES LESS THAN (2013) ENGINE = MyISAM,\n"
					+ " PARTITION p20 VALUES LESS THAN (2014) ENGINE = MyISAM,\n"
					+ " PARTITION p21 VALUES LESS THAN (2015) ENGINE = MyISAM,\n"
					+ " PARTITION p22 VALUES LESS THAN (2016) ENGINE = MyISAM,\n"
					+ " PARTITION p23 VALUES LESS THAN (2017) ENGINE = MyISAM,\n"
					+ " PARTITION p24 VALUES LESS THAN (2018) ENGINE = MyISAM,\n"
					+ " PARTITION p25 VALUES LESS THAN (2019) ENGINE = MyISAM,\n"
					+ " PARTITION p26 VALUES LESS THAN (2020) ENGINE = MyISAM,\n"
					+ " PARTITION p27 VALUES LESS THAN MAXVALUE ENGINE = MyISAM) */;\n"
					+ "\n"
					+ "DROP TABLE IF EXISTS `stockanalyser`.`tp_sales_to_scrub2`;\n"
					+ "CREATE TABLE `tp_sales_to_scrub2` (\n"
					+ "  `accno` varchar(20) CHARACTER SET latin1 NOT NULL DEFAULT '-1',\n"
					+ "  `fileDate` datetime DEFAULT NULL,\n"
					+ "  `cik` int(11) DEFAULT NULL,\n"
					+ "  `tn` varchar(6) CHARACTER SET latin1 DEFAULT NULL,\n"
					+ "  `trow` TINYINT(3) DEFAULT NULL COMMENT 'table row number in financial table',\n"
					+ "  `row` int(5) NOT NULL DEFAULT '-1' COMMENT 'row count in mysql',\n"
					+ "  `col` TINYINT(3) NOT NULL DEFAULT '0' COMMENT 'data col number in financial table',\n"
					+ "  `tno` int(5) NOT NULL DEFAULT '-1',\n"
					+ "  `rowname` varchar(255) CHARACTER SET latin1 DEFAULT NULL,\n"
					+ "  `value` double(23,5) DEFAULT NULL,\n"
					+ "  `p2` int(3) NOT NULL DEFAULT '0' COMMENT 'if html - per2 parsed from cell, if txt per2 parsed based on col hdg ratio matching',\n"
					+ "  `edt2` varchar(10) CHARACTER SET latin1 NOT NULL DEFAULT '1901-01-1',\n"
					+ "  `DEC` int(10) DEFAULT NULL,\n"
					+ "  `columnText` varchar(200) DEFAULT NULL,\n"
					+ "  `form` varchar(15) CHARACTER SET latin1 DEFAULT NULL,\n"
					+ "  `revised` varchar(25) DEFAULT NULL COMMENT 'notes if it is numbered reported at time of filing (actual) or one that is later revised',\n"
					+ "  PRIMARY KEY (`accno`,`tno`,`edt2`,`p2`),\n"
					+ "  UNIQUE KEY `accno_2` (`accno`,`tno`,`row`),\n"
					+ "  KEY `accno` (`accno`),\n"
					+ "  KEY `tno` (`tno`),\n"
					+ "  KEY `trow` (`trow`),\n"
					+ "  KEY `row` (`row`),\n"
					+ "  KEY `p2` (`p2`),\n"
					+ "  KEY `edt2` (`edt2`),\n"
					+ "  KEY `col` (`col`),\n"
					+ "  KEY `cik` (`cik`),\n"
					+ "  KEY `value` (`value`),\n"
					+ "  KEY `fileDate` (`fileDate`)\n"
					+ ") ENGINE=MyISAM DEFAULT CHARSET=utf8;\n"
					+ "\n"
					+ "DROP TABLE IF EXISTS `stockanalyser`.`tp_sales_to_scrub`;\n"
					+ "CREATE TABLE `tp_sales_to_scrub` (\n"
					+ "  `accno` varchar(20) CHARACTER SET latin1 NOT NULL DEFAULT '-1',\n"
					+ "  `fileDate` datetime DEFAULT NULL,\n"
					+ "  `cik` int(11) DEFAULT NULL,\n"
					+ "  `tn` varchar(6) CHARACTER SET latin1 DEFAULT NULL,\n"
					+ "  `trow` TINYINT(3) DEFAULT NULL COMMENT 'table line number in financial table',\n"
					+ "  `row` int(5) NOT NULL DEFAULT '-1' COMMENT 'row count in mysql ',\n"
					+ "  `col` TINYINT(3) NOT NULL DEFAULT '0' COMMENT 'data col number in financial table',\n"
					+ "  `tno` int(5) NOT NULL DEFAULT '-1',\n"
					+ "  `rowname` varchar(125) CHARACTER SET latin1 DEFAULT NULL,\n"
					+ "  `value` double(23,5) DEFAULT NULL,\n"
					+ "  `p2` int(3) NOT NULL DEFAULT '0' COMMENT 'if html - per2 parsed from cell, if txt per2 parsed based on col hdg ratio matching',\n"
					+ "  `edt2` varchar(10) CHARACTER SET latin1 NOT NULL DEFAULT '1901-01-1',\n"
					+ "  `DEC` int(10) DEFAULT NULL,\n"
					+ "  `columnText` varchar(200) DEFAULT NULL,\n"
					+ "  `form` varchar(15) CHARACTER SET latin1 DEFAULT NULL COMMENT 'this will equal rowratioBeforeColumnUtil if generic in htmlTxt field',\n"
					+ "  PRIMARY KEY (`accno`,`tno`,`edt2`,`p2`) COMMENT 'need to get all possible enddates (edt2) and periods.',\n"
					+ "  UNIQUE KEY `accno_2` (`accno`,`tno`,`row`),\n"
					+ "  KEY `accno` (`accno`),\n"
					+ "  KEY `tno` (`tno`),\n"
					+ "  KEY `trow` (`trow`),\n"
					+ "  KEY `row` (`row`),\n"
					+ "  KEY `p2` (`p2`),\n"
					+ "  KEY `edt2` (`edt2`),\n"
					+ "  KEY `col` (`col`),\n"
					+ "  KEY `cik` (`cik`),\n"
					+ "  KEY `fileDate` (`fileDate`),\n"
					+ "  KEY `value` (`value`)\n"
					+ ") ENGINE=MyISAM DEFAULT CHARSET=utf8;\n";
			// MysqlConnUtils.executeQuery(qry);
		}

		/*
		 * 1st rownameWhereFilter runs on all table=IS to find common
		 * sales/revenue rownames. 2nd where filter looks for rownames like
		 * '%fee%' and runs against only accno not found in 1st and -
		 * callAfterLoop1 and rightJoinAfterLoop1 isolate accno where
		 * sales/revenue not found yet (after each rowname filter is run). 3rd
		 * runs on BS/CF against accno not found in 1st with greater filter on
		 * f/p rownames.
		 */
		boolean p3 = false;
		if (table.substring(0, 2).equals("p3"))
			p3 = true;

		String dropProc = "DROP PROCEDURE IF EXISTS queryGetSalesTable" + yr
				+ "QTR" + q + ";\n" + "CREATE PROCEDURE queryGetSalesTable"
				+ yr + "QTR" + q + "()\n" + "\n\nbegin\n\n";
		String queryGetSalesTable = "DROP TABLE IF EXISTS TMP_CIKLENGTH_"
				+ yr
				+ "_"
				+ q
				+ ";"
				+ "\nCREATE TABLE TMP_CIKLENGTH_"
				+ yr
				+ "_"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "SELECT * FROM "
				+ table
				+ " WHERE cik between "
				+ cikStart
				+ " and "
				+ cikEnd
				+ " AND TN='IS' and (yr!='bad' or yr is null);\n"
				+ "alter table TMP_CIKLENGTH_"
				+ yr
				+ "_"
				+ q
				+ " add key(accno),add key(tno), add key(trow)"
				+ ", ADD KEY(COLUMNTEXT),ADD KEY(ROWNAME), ADD KEY(VALUE);\n\n"
				+

				"DROP TABLE IF EXISTS TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_MIN_COST_ROW;\n"
				+ "CREATE TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_MIN_COST_ROW ENGINE=MYISAM\n"
				+ "SELECT  MIN(TROW)+1 TROW,ACCNO,TNO/*,FILEDATE,ROWNAME*/ FROM TMP_CIKLENGTH_"
				+ yr
				+ "_"
				+ q
				+ " \n"
				+ "WHERE (ROWNAME RLIKE '(TOTAL|GROSS) (NET |OPERATING |INTEREST )?(SALES|REVENU|INCOME)' \n"
				+ "OR TRIM(ROWNAME) ='REVENUES' OR TRIM(ROWNAME) = 'REVENUE' OR TRIM(ROWNAME) = 'REVENU'\n"
				+ "OR TRIM(ROWNAME) ='REVENUES:' OR TRIM(ROWNAME) = 'REVENUE:' \n"
				+ "OR TRIM(ROWNAME) ='SALES' OR TRIM(ROWNAME) ='SALES:' \n"
				+ "OR TRIM(ROWNAME) = 'NET SALES' OR TRIM(ROWNAME) = 'NET SALES:' \n"
				+ "OR TRIM(ROWNAME) ='OPERATING REVENUES' OR TRIM(ROWNAME) = 'OPERATING REVENUE'  \n"
				+ "OR TRIM(ROWNAME) ='OPERATING REVENUES:' OR TRIM(ROWNAME) = 'OPERATING REVENUE:' \n"
				+ "OR TRIM(ROWNAME) = 'OPERATING REVENU') AND (VALUE>99 AND VALUE IS NOT NULL ) and tn='is'"
				+ "and left(trim(rowname),16)!= 'total income tax' and left(trim(rowname),6)!='state '\n"
				+ "GROUP BY ACCNO,TNO;\n\n"
				// "ALTER TABLE TMP_"+year+"QTR"+qtr+"_MIN_COST_ROW ADD PRIMARY KEY(ACCNO,TNO);\n"

				+ "insert ignore into TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_MIN_COST_ROW \n"
				+ "select min(trow) tRow,accno,tno from "
				+ "TMP_CIKLENGTH_"
				+ yr
				+ "_"
				+ q
				+ "\n"
				+ "where \n"
				+ "(\n"
				+ "(rowname rlike 'cost.{1,3}and.{1,3}expense|^Expenses|COST.{1,2}OF|COST.{1,2}(AND|\\\\&).{1,2}EXPENSE|expenses:"
				+ "|COGS|(advertising|benefit|claims and|communication|control|corporate|costs|debt|fuel|general|operating|other"
				+ "|processing|related|selling|tax|utilities).{0,3}expens|depreciat|amortiz|research|administrat|(operating|"
				+ "production|transport(ation)?).{0,3}cost|net.{1,2}income|(shares|stock).{1,5}outstand|(earning|income).{0,3}(before|after)"
				+ "( provision.{1,3}for)?.{0,3}income.{0,3}tax|segment|(direct|contract) cost|(direct|routine|administrative|general"
				+ "|income.{1,3}tax|non.{0,1}financial|operating) expense'\n "
				+ "and trow<40 and rowname not rlike 'revenu' ) or (value<-10 and trow>15) or \n"
				+ "/*using often there's a line item called other income (expense) and or gain loss - so means it could be plus/minus\n"
				+ "probably need tok keep trow filter .*/"
				+ "(value<-10 and trow<6 and rowname not rlike 'income.{1,4}expense|gain.{1,4}loss' and value is not null) or rowname rlike "
				+ "'cost.{1,2}of.{1,3}(revenu|concrete|fuel|sales)' or "
				+ "rowname rlike 'costs?.{1,2}of.{1,3}(contracting|contract|goods|grain|operating|earned|service|other|product) revenu'\n"
				+ "or rowname rlike 'costs? (applicable|related) to' or"
				+ " rowname rlike 'costs? and (other deduct|deduct|expens)' or rowname rlike '(GROSS|OPERATING).{0,2}(PROFIT|MARGIN)'\n"
				+ ") \ngroup by accno,tno;\n"
				+ "alter table TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_MIN_COST_ROW add key(accno),add key(tno), add key(trow);\n\n"
				+

				"DROP TABLE IF EXISTS TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_MIN_COST_ROW2;\n"
				+ "CREATE TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_MIN_COST_ROW2 ENGINE=MYISAM\n"
				+ "SELECT max(trow) trow,accno,tno FROM TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_MIN_COST_ROW GROUP BY ACCNO,TNO;\n"
				+ "ALTER TABLE TMP_"
				+ yr
				+ "QTR"
				+ q
				+ "_MIN_COST_ROW2 ADD KEY(ACCNO), ADD KEY(TNO), ADD KEY(TROW),ADD PRIMARY KEY(ACCNO,TNO);\n"
				+

				"drop table if exists TMP_ABOVE_EXPENSE_"
				+ yr
				+ "qtr"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP_ABOVE_EXPENSE_"
				+ yr
				+ "qtr"
				+ q
				+ " (\n"
				+ "  `AccNo` varchar(20) NOT NULL DEFAULT '-1',\n"
				+ "  `fileDate` datetime DEFAULT NULL,\n"
				+ "  `cik` int(11) DEFAULT NULL,\n"
				+ "  `tn` varchar(6) DEFAULT NULL,\n"
				+ "  `row` int(5) NOT NULL DEFAULT '-1' COMMENT 'table row',\n"
				+ "  `col` TINYINT(3) DEFAULT NULL COMMENT 'data col number in financial table',\n"
				+ "  `tRow` TINYINT(3) DEFAULT NULL COMMENT 'row number in financial table',\n"
				+ "  `tno` int(5) NOT NULL DEFAULT '-1',\n"
				+ "  `rowName` varchar(255) DEFAULT NULL,\n"
				+ "  `value` double(23,5) DEFAULT NULL,\n"
				+ "  `ttl` int(4) DEFAULT NULL,\n"
				+ "  `stt` int(4) DEFAULT NULL,\n"
				+ "  `net` int(4) DEFAULT NULL,\n"
				+ "  `sub` int(4) DEFAULT NULL,\n"
				+ "  `p1` int(3) DEFAULT NULL COMMENT 'if html - per1 parsed from cell, if txt per1 parsed based on col hdg ratio matching',\n"
				+ "  `edt1` varchar(11) DEFAULT NULL COMMENT 'same as per1',\n"
				+ "  `p2` int(3) DEFAULT NULL COMMENT 'if html - per2 based on col hdg ratio matching, if txt based on idx alignments of each match',\n"
				+ "  `edt2` varchar(11) DEFAULT NULL COMMENT ' same as per2',\n"
				+ "  `tc` tinyint(3) DEFAULT NULL COMMENT 'total number of data cols',\n"
				+ "  `tableName` varchar(255) DEFAULT '',\r"
				+ "  `coMatch` tinyint(1) DEFAULT NULL COMMENT '1 means company name is in tableheading',\r"
				+ "  `companyNameMatched` varchar(100) DEFAULT '',\r"
				+ "  `dec` int(11) DEFAULT NULL,\n"
				+ "  `tsShort` varchar(20) DEFAULT NULL COMMENT 'Yr mo per in order found in tablesentence. This pattern can then be used to grab data in TSLong',\n"
				+ "  `ColumnText` varchar(255) DEFAULT NULL COMMENT 'shows this col nos text used for edt2. ',\n"
				+ "  `ColumnPattern` varchar(255) DEFAULT NULL COMMENT 'Shows pattern of all c/hdgs pattern matched (not CH lines) and counts distinct m,y,p (versus total y,m,p) on ALL CH.\n"
				+ " For txt it numbers each CH by reading across cols (the line) and for html by paired CHs.',\n"
				+ "  `allColText` varchar(255) DEFAULT NULL COMMENT 'shows by Line each Column based on words being separated by two spaces.',\n"
				+ "  `ended` varchar(50) DEFAULT NULL,\n"
				+ "  `yr` varchar(10) DEFAULT NULL,\n"
				+ "  `mo` varchar(25) DEFAULT NULL,\n"
				+ "  `htmlTxt` varchar(15) DEFAULT NULL COMMENT 'if txt it has loc end idx of far right data col, else it will say html or generic to show which parser used',\n"
				+ "  `form` varchar(15) DEFAULT NULL COMMENT 'this will equal rowratioBeforeColumnUtil if generic in htmlTxt field',\n"
				+ "  `TSlong` varchar(200) DEFAULT NULL COMMENT 'tablesentence Year, Month and Period formatted: |yyyy|mmdd|p.pp| - b/c each is 4 width can recreate edt/per based on tsShort pattern',\n"
				+ "  PRIMARY KEY (`AccNo`,`tNo`,`row`),\n"
				+ "  KEY `edt2` (`edt2`),\n" + "  KEY `per2` (`p2`),\n"
				+ "  KEY `AccNo` (`AccNo`),\n"
				+ "  KEY `fileDate` (`fileDate`),\n" + "  KEY `tNo` (`tNo`),\n"
				+ "  KEY `row` (`row`),\n" + "  KEY `rowname` (`rowName`),\n"
				+ "  KEY `tn` (`tn`),\n" + "  KEY `value` (`value`),\n"
				+ "  KEY `cik` (`cik`),\n" + "  KEY `col` (`col`),\n"
				+ "  KEY `totcol` (`tc`),\n" + "  KEY `year` (`yr`),\n"
				+ "  KEY `columnPattern` (`ColumnPattern`),\n"
				+ "  KEY `tRow` (`tRow`)\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n";

		if (!p3) {
			queryGetSalesTable = queryGetSalesTable
					+ "\n"
					+ "INSERT IGNORE INTO TMP_ABOVE_EXPENSE_"
					+ yr
					+ "qtr"
					+ q
					+ "\nSELECT t1.* FROM "
					+ table
					+ " T1 INNER JOIN TMP_"
					+ yr
					+ "QTR"
					+ q
					+ "_MIN_COST_ROW2 T2 ON T1.ACCNO=t2.accno and T1.TNO=T2.TNO WHERE T1.TROW<T2.TROW"
					+ " and t1.cik between " + cikStart + " and " + cikEnd
					+ " and (t1.yr is null or t1.yr!='bad');";
		}

		if (p3) {
			queryGetSalesTable = queryGetSalesTable + "\n"
					+ "INSERT IGNORE INTO TMP_ABOVE_EXPENSE_" + yr + "qtr" + q
					+ "\nSELECT t1.* FROM " + table + " T1 WHERE \n"
					+ " t1.cik between " + cikStart + " and " + cikEnd
					+ " and (t1.yr is null or t1.yr!='bad');";
		}

		MysqlConnUtils.executeQuery(dropProc + qry + "\n\n"
				+ queryGetSalesTable + "\n end;\n\n");
		MysqlConnUtils.executeQuery("\ncall queryGetSalesTable" + yr + "QTR"
				+ q + "();\n");

		// added premium|income after sales|revenue -- may cause false positive
		// inclusions. will show up in rowname of final tp_sales_to_scrub2

		String[] rowNameWhereFilter = {
				"/*don't look for 'income' and other less likely sales/rev rownames when I look for rownames like sales/rev*/\n"
						+ " (ROWNAME rlike 'sale|revenu|gaming|TOTAL.{1,2}INTEREST.{1,2}INCOME' ) "
						+ "AND (rowname not rlike 'ADMIN|discon|adjust|gain on|write.{1,3}(down|off)|chang|cost|leas|provid"
						+ "|non.{1,3}gaap|memb|margin"
						+ "|discon|ratio |geograph|segment|impairment|unearn|non.{0,3}perform|decreas|increas|deferr|purchas"
						+ "|loss.{1,12}sale|accrue|"
						+ "receivabl|unbille|addition|(held|available).{1,2}(to|for)|investmen|subsidiar|proceed|marketing"
						+ "|whole|resale|misc|sales? of|"
						+ "bond|mortgag|expens|internal' or \n(\n"
						+ "ROWNAME RLIKE 'sales? of|loss|gains' and ROWNAME RLIKE \n"
						+ "'(net|gross|total) sale|(sales? of )(cement|commission|concentra|develop|fine|fruit|good|gold"
						+ "|health|homes|insuranc"
						+ "|medical|communica|natural|oil|product|recycl|servic|sugar|turnkey|used car|graphic|gas)'))"
						+ " and tn='is' \n"
						+ " and trow<21 and value>0 and (ttl>=0 or ttl is null) AND rowname NOT rlike 'rnh.{1,2}rnh' "
						+ " and yr!='bad' and p2 between 3 and 12 and length(edt2)=10 "
						+ " AND columnText not like '%pro%forma%' and  (htmltxt='html' or (htmltxt!='html' and allColText not like '%pro%forma%' \n"
						+ " and ColumnPattern not like '%pro%forma%')) \n"
						+ " AND COLUMNTEXT NOT RLIKE \n"
						+ " 'subsidiar|mortgag|eliminat|guarantor|historical|portfolio|segments|annuity|health|investment"
						+ "|other|under|closed|percent|increas|decreas';\n"

				/*
				 * array2: expands rowname to include fee, income and premium.
				 */

				,
				"((((rowname like '%fee%' and trow<5 or (rowname like '%premiu%' and (cik=100320 or cik=5016 or cik=1985 or cik=5272) and trow<12) ) \n"
						+ "or (rowname like '%incom%' and rowname not like '%net income%' and trow<12 and rowname not like '%tax%' \n"
						+ "and rowname not like '%weight%' and rowname not like '%share%' and rowname not like '%loss%')  \n"
						+ "or (rowname like '%total%fee%' and trow<9) \n"
						+ "or (rowname like '%total%incom%' ))\n"
						+ "AND ROWNAME NOT LIKE '%ADMIN%'  and ROWNAME not like '%discon%' and ROWNAME not like '%adjust%' and ROWNAME not like \n"
						+ "'%gain%'  and ROWNAME not like '%chang%' and ROWNAME not like '%cost%' and ROWNAME not like '%leas%' and ROWNAME not like '%provid%' and ROWNAME not \n"
						+ "like '%non-gaap%' and ROWNAME not like '%memb%' and ROWNAME not like '%margin%' and ROWNAME not like '%discon%'  and ROWNAME not like \n"
						+ "'% ratio%' and ROWNAME not like '%geograph%' and ROWNAME not like '%segment%' and ROWNAME not like '%impairment%' \n"
						+ " and ROWNAME not like '%discon%' and ROWNAME not like '%non%perform%' and ROWNAME not like '%\\\\%%'  and ROWNAME not like '%unearn%' \n"
						+ " and ROWNAME not like '%decreas%' and ROWNAME not like '%increas%' and ROWNAME not like '%deferr%' and ROWNAME not like '%purchas%' \n"
						+ " and ROWNAME not like '%loss%sale%' and ROWNAME not like '%accrue%' and ROWNAME not like '%receivabl%' \n"
						+ " and ROWNAME not like '%unbille%' and ROWNAME not like '%addition%' and ROWNAME not like '%held%for%' and ROWNAME not like '%availabl%' \n"
						+ " and ROWNAME not like '%investmen%' and ROWNAME not like '%subsidiar%' and ROWNAME not like '%proceed%' and ROWNAME not like '%marketing%' \n"
						+ " and ROWNAME not like '%whole%' and ROWNAME not like '%resale%' and ROWNAME not like '%misc%' and ROWNAME not like '%sale of%' and ROWNAME not like '%sales of%' \n"
						+ " and ROWNAME not like '%bond%' and ROWNAME not like '%mortgag%' and ROWNAME not like '%mortgag%' and ROWNAME not like '%taxe%' \n"
						+ " and ROWNAME not like '%expens%') or (rowname like '%revenu%earn%as%' and trow<12)) and tn='is' \n"
						+ " and value>0  and (ttl>=0 or ttl is null) and yr!='bad' AND rowname not rlike 'rnh.{1,2}rnh' "
						+ " and p2 between 3 and 12 and length(edt2)=10 "
						+ " AND columnText not like '%pro%forma%' and  (htmltxt='html' or (htmltxt!='html' and allColText not like '%pro%forma%' \n"
						+ " and ColumnPattern not like '%pro%forma%')) \n"
						+ " AND COLUMNTEXT NOT RLIKE "
						+ "'subsidiar|mortgag|eliminat|guarantor|historical|portfolio|segments|annuity|health|investment|other|under|parent|wholly|previous|closed|percent|increas|decreas';\n" };

		String callAfterLoop1 = "DROP TABLE IF EXISTS TMP_MISSING_ACCNO_"
				+ yr
				+ q
				+ " ;\nset sql_mode = ALLOW_INVALID_DATES;\n CREATE TABLE TMP_MISSING_ACCNO_"
				+ yr
				+ q
				+ " ENGINE=MYISAM \n"
				+ "\nSELECT ACCNO FROM "
				+ tp_sales_to_scrub
				+ " T1 WHERE YEAR(FILEDATE)="
				+ yr
				+ " AND QUARTER(FILEDATE)= "
				+ q
				+ " GROUP BY ACCNO; "
				+ "\nALTER TABLE TMP_MISSING_ACCNO_"
				+ yr
				+ q
				+ " ADD KEY(ACCNO); "
				+ "\n\n"
				+ "INSERT IGNORE INTO TMP_MISSING_ACCNO_"
				+ yr
				+ q
				+ " "
				+ "\nSELECT ACCNO FROM "
				+ " TMP_ABOVE_EXPENSE_"
				+ yr
				+ "qtr"
				+ q
				+ " GROUP BY ACCNO; "
				+ "\n\nDROP TABLE IF EXISTS TMP_MISSING_ACCNO2_"
				+ yr
				+ q
				+ " ;\n"
				+ "CREATE TABLE TMP_MISSING_ACCNO2_"
				+ yr
				+ q
				+ " ENGINE=MYISAM\n"
				+ "SELECT ACCNO FROM (SELECT COUNT(*) CNT, ACCNO FROM TMP_MISSING_ACCNO_"
				+ yr + q + " T1 GROUP BY ACCNO) T1 WHERE CNT=1;\n"
				+ "ALTER TABLE TMP_MISSING_ACCNO2_" + yr + q
				+ " ADD KEY(ACCNO);\n";

		String rightJoinAfterLoop1 = " RIGHT JOIN TMP_MISSING_ACCNO2_" + yr + q
				+ " T2 ON T1.ACCNO=T2.ACCNO ";

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < rowNameWhereFilter.length; i++) {

			if (i > 0) {
				sb.append(callAfterLoop1);
				rightJoinAfterLoop1 = " RIGHT JOIN TMP_MISSING_ACCNO2_" + yr
						+ q + " T2 ON T1.ACCNO=T2.ACCNO ";
			}
			if (i < 1) {
				rightJoinAfterLoop1 = " ";
			}
			// oddly - can't lead w/ comment.
			sb.append("\nDROP TABLE IF EXISTS TMP_TP_RAW"
					+ yr
					+ q
					+ ";\nset sql_mode = ALLOW_INVALID_DATES;\n"
					+ "CREATE TABLE TMP_TP_RAW"
					+ yr
					+ q
					+ " ENGINE=MYISAM\n"
					+ "SELECT T1.accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value, t1.p2"
					+ ", LEFT(edt2,10) edt2, CASE WHEN `DEC`=0 OR `DEC` IS NULL OR `DEC`='' THEN 1 WHEN `DEC`=-3 THEN 1000 WHEN `DEC` = -6 "
					+ "THEN 1000000 WHEN `DEC`=-9 THEN 1000000000 ELSE `DEC` END `DEC`, columnText ,form,t1.tc FROM \n"
					+ " TMP_ABOVE_EXPENSE_"
					+ yr
					+ "qtr"
					+ q
					+ " T1 "
					+ rightJoinAfterLoop1
					+ "\n WHERE "
					+ rowNameWhereFilter[i]
					+ " ALTER TABLE TMP_TP_RAW"
					+ yr
					+ q
					+ " ADD PRIMARY KEY (ACCNO,tno,ROW), ADD KEY(p2);\n"
					+ " \n"
					+ "\n"
					+ "/*BELOW MARKS THE HIGHEST VALUES OF EACH ACCNO THEN GRABS ALL COLUMNS RELATED TO THAT VALUE ON THE SAME ROW "
					+ "INTO TP_SALES_PRELIM.*/\n"
					+ "\n"
					+ "\n\nset @pAcc = '1x'; set @rw=0; set @hhv=0; set @tno=-1;\n"
					+ "DROP TABLE IF EXISTS tmp_getIt"
					+ yr
					+ q
					+ ";\n"
					+ "CREATE TABLE tmp_getIt"
					+ yr
					+ q
					+ " engine=myisam\n"
					+ " select case when @rw=0 or @pAcc!=accno or (@hhv=value and @pAcc=accno and @tno!=tno) then 1 else 0 end getIt\n"
					+ " ,@hhv:=case when @pAcc!=accno or @rw=0 then value else @hhv end hhv\n"
					+ " ,@rw:=@rw+1 rw,@pAcc:=t1.AccNo accno,\n"
					+ " t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, @tno:=t1.tno tno, t1.rowname,t1.value, t1.p2"
					+ ", LEFT(edt2,10) edt2, `DEC`, columnText,form\n"
					+ "from TMP_TP_RAW"
					+ yr
					+ q
					+ " t1 WHERE length(edt2)=10 AND p2 between 3 and 12 \n"
					+ "order by accno,value*`DEC` desc,tno,trow;\n"
					+ "ALTER TABLE tmp_getIt"
					+ yr
					+ q
					+ " add key(getIt);"
					+ "\nDROP TABLE IF EXISTS TMP_tp_sales_To_Scrub"
					+ yr
					+ q
					+ " ;\nset sql_mode = ALLOW_INVALID_DATES;\n"
					+ "CREATE TABLE TMP_tp_sales_To_Scrub"
					+ yr
					+ q
					+ " ENGINE=MYISAM\n"
					+ " SELECT T1.accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value, t1.p2,"
					+ " LEFT(edt2,10) edt2, `DEC`, columnText,form "
					+ "from tmp_getIt"
					+ yr
					+ q
					+ " T1 \n"
					+ "WHERE getIt=1 ;\n"
					+ "ALTER TABLE TMP_tp_sales_To_Scrub"
					+ yr
					+ q
					+ " ADD PRIMARY KEY(accno,tno),ADD KEY(ACCNO),ADD KEY(tn),ADD KEY(trow);\n\n"
					+ "/*don't have p2 as a condition*/\n"
					+ "\n\nset @pAcc = '1x'; set @rw=0; set @hhv=0; set @tno=-1;\n"
					+ "insert ignore into TMP_tp_sales_To_Scrub"
					+ yr
					+ q
					+ " \n"
					+ "SELECT accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value, t1.p2, LEFT(edt2,10) edt2, `DEC`"
					+ ", columnText,form \n"
					+ "from (\n"
					+ "select case when @rw=0 or @pAcc!=accno or (@hhv=value and @pAcc=accno and @tno!=tno) then 1 else 0 end getIt ,@hhv:=case when @pAcc!=accno or @rw=0 then value else @hhv end hhv,@pAcc:=t1.AccNo accno, @rw:=@rw+1 rw, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col,"
					+ " @tno:=t1.tno tno, t1.rowname, t1.value, t1.p2, LEFT(edt2,10) edt2, `DEC`, columnText,form \n"
					+ "from TMP_TP_RAW"
					+ yr
					+ q
					+ " t1 WHERE edt2 rlike '^[1,2]{1}[09]{1}[0-9]{2}' order by accno,tno,value*`DEC` desc,trow) T1 \n"
					+ "WHERE getIt=1 ;\n"
					+ "\n"
					+ "/*don't have edt2 as a condition*/\n"
					+ "\n\nset @pAcc = '1x'; set @rw=0; set @hhv=0; set @tno=-1;\n"
					+ "insert ignore into TMP_tp_sales_To_Scrub"
					+ yr
					+ q
					+ " \n"
					+ " SELECT accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value, t1.p2, LEFT(edt2,10) edt2, `DEC`"
					+ ", columnText,form \n"
					+ " from (\n"
					+ " select case when @rw=0 or @pAcc!=accno or (@hhv=value and @pAcc=accno and @tno!=tno) then 1 else 0 end getIt ,@hhv:=case when @pAcc!=accno or @rw=0 then value else @hhv end hhv,@pAcc:=t1.AccNo accno, @rw:=@rw+1 rw, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col"
					+ ",@tno:= t1.tno tno, t1.rowname, t1.value, t1.p2, LEFT(edt2,10) edt2, `DEC`, columnText,form \n"
					+ " from TMP_TP_RAW"
					+ yr
					+ q
					+ " t1 WHERE edt2 not rlike '^[1,2]{1}[09]{1}[0-9]{2}' AND p2>2 order by accno,tno,value*`DEC` desc,trow) T1 \n"
					+ "WHERE getIt=1 ;\n"
					+ "\n"
					+ "/*don't have either edt2 or period as a condition*/\n"
					+ "\n\nset @pAcc = '1x'; set @rw=0; set @hhv=0; set @tno=-1;\n"
					+ "insert ignore into TMP_tp_sales_To_Scrub"
					+ yr
					+ q
					+ " \n"
					+ "SELECT accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value, t1.p2, LEFT(edt2,10) edt2, `DEC`"
					+ ", columnText,form \n"
					+ "from (\n"
					+ "select case when @rw=0 or @pAcc!=accno or (@hhv=value and @pAcc=accno and @tno!=tno) then 1 else 0 end getIt ,@hhv:=case when @pAcc!=accno or @rw=0 then value else @hhv end hhv,@pAcc:=t1.AccNo accno, @rw:=@rw+1 rw, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col"
					+ ", @tno:=t1.tno tno, t1.rowname, t1.value, t1.p2, LEFT(edt2,10) edt2, `DEC`, columnText,form \n"
					+ "from TMP_TP_RAW"
					+ yr
					+ q
					+ " t1 WHERE edt2 not rlike '^[1,2]{1}[09]{1}[0-9]{2}' AND p2<3 order by accno,tno,value*`DEC` desc,trow) T1 \n"
					+ "WHERE getIt=1 ;\n"
					+ "\n"
					+ "/*used primary key (accno,tno,trow) initially to prevent re-entry of identical rows. Dropped it in order to allow joinder of like trows \n"
					+ "but first inserted back overall primary key of accno,tno,row*/\n"
					+ "\n"
					+ "ALTER TABLE TMP_TP_SALES_TO_SCRUB"
					+ yr
					+ q
					+ " DROP PRIMARY KEY, ADD PRIMARY KEY(ACCNO,tno,ROW);\n"
					+ "\n"
					+ "\n"
					+ "INSERT IGNORE INTO TMP_TP_SALES_TO_SCRUB"
					+ yr
					+ q
					+ " \n"
					+ "SELECT T2.accno, t2.fileDate, t2.cik, t2.tno, t2.trow, t2.row, t2.col, t2.tn, t2.rowname, t2.value, t2.p2, t2.edt2, t2.`DEC`, t2.columnText, t2.form FROM TMP_TP_SALES_TO_SCRUB"
					+ yr
					+ q
					+ " T1 INNER JOIN TMP_TP_RAW"
					+ yr
					+ q
					+ " T2\n"
					+ "ON T1.ACCNO=T2.ACCNO AND T1.tno=T2.tno AND T1.trow=T2.trow ;\n"
					+ "\n"
					+ "\n"
					+ "UPDATE IGNORE TMP_TP_RAW"
					+ yr
					+ q
					+ "\n set p2=0\n"
					+ "WHERE (COLUMNTEXT RLIKE 'NINE.{1,3}WEEK' AND COLUMNTEXT NOT RLIKE 'THIRTY') OR\n"
					+ " (COLUMNTEXT RLIKE 'SIX.{1,3}WEEK' AND COLUMNTEXT NOT RLIKE 'THIRTY|TWENTY')"
					+ " or (COLUMNTEXT not RLIKE 'hundred' and columntext rlike '^ONE | ONE ');\n"

					+ "\nUPDATE IGNORE TMP_TP_RAW"
					+ yr
					+ q
					+ " set p2=case \n"
					+ "when columntext rlike 'one.{1,2}hundred' and columntext rlike 'twenty|fifty|forty|thirty' then 3\n"
					+ "when columntext rlike 'three.{1,2}hundred' and columntext rlike 'fifty|sixty|forty' then 12\n"
					+ "when columntext rlike 'three.{1,2}hundred' then 0\n"
					+ "else p2 end \n"
					+ "where columntext rlike 'hundred' ;\n\n"

					+ "INSERT IGNORE INTO "
					+ tp_sales_to_scrub
					+ "\n"
					+ "SELECT t1.accno,t1.filedate,t1.cik,t1.tn,t1.trow,t1.row,t1.col,t1.tno,t1.rowname,t1.value,t1.p2,t1.edt2,t1.`dec`,t1.columntext,t1.form "
					+ " FROM TMP_TP_RAW"
					+ yr
					+ q
					+ " T1 INNER JOIN TMP_tp_sales_To_Scrub"
					+ yr
					+ q
					+ " T2 ON T1.ACCNO=T2.ACCNO AND T1.tno=T2.tno AND T1.trow=T2.trow "
					+ "\n where t1.p2 between 3 and 12;\n" + " \n");

			sb.append("DROP TABLE IF EXISTS TMP_SAME_EDT_VAL_DIF_P"
					+ yr
					+ "qtr"
					+ q
					+ ";"
					+ "/*can only run this on tp_sales b/c many values can be equal to each other \n"
					+ "in bs and other line items in a f/s. I also then need to update yr='bad' after items identified\n"
					+ "SAME EDT, SAME VALUE BUT DIF P2 - THEREFORE THEY ARE BAD*/\n"
					+ "CREATE TABLE TMP_SAME_EDT_VAL_DIF_P"
					+ yr
					+ "qtr"
					+ q
					+ " ENGINE=MYISAM\n"

					+ "SELECT T1.ACCNO,T1.TNO,T1.ROW FROM "
					+ tp_sales_to_scrub
					+ " T1 inner join "
					+ tp_sales_to_scrub
					+ " t2 \n"
					+ "on t1.cik=t2.cik and t1.edt2=t2.edt2 and t1.value=t2.value and t1.p2!=t2.p2 where t1.value>999;\n"
					+ "ALTER TABLE TMP_SAME_EDT_VAL_DIF_P"
					+ yr
					+ "qtr"
					+ q
					+ " ADD KEY(ACCNO), ADD KEY(TNO), ADD KEY(ROW);\n"
					+ "\n /*TODO: COME BACK TO FIGURE OUT WHAT TO DELETE -- RUN METHOD AND SEE WHAT THIS DELETES AND HOW TO TIGHTEN AND TRY TO LEAVE TILL VERY LAST PASS!-- XXXX*/"
					+ "DELETE T1 FROM "
					+ tp_sales_to_scrub
					+ " T1 INNER JOIN TMP_SAME_EDT_VAL_DIF_P"
					+ yr
					+ "qtr"
					+ q
					+ " T2 ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.ROW=T2.ROW;\n"
					+ "\n"
					+ "UPDATE IGNORE "
					+ table
					+ " t1 INNER JOIN TMP_SAME_EDT_VAL_DIF_P"
					+ yr
					+ "qtr"
					+ q
					+ " T2 ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.ROW=T2.ROW\n"
					+ "SET YR='bad';\n");

			dropProc = "DROP PROCEDURE IF EXISTS tp_sales_to_scrub" + yr
					+ "QTR" + q + "_" + i + ";\n"
					+ "CREATE PROCEDURE tp_sales_to_scrub" + yr + "QTR" + q
					+ "_" + i + "()\n\n begin\n\n";
			String endProc = "\n\nend;";

			MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
			MysqlConnUtils.executeQuery("call tp_sales_to_scrub" + yr + "QTR"
					+ q + "_" + i + "();\n");
			sb.delete(0, sb.toString().length());
		}

		StringBuffer sb2 = new StringBuffer();

		// ck tp_sales_to_scrub for duplicates - delete those and mark
		// tp_raw_revised table bad. Then rerun sales cycle above - which then
		// skips
		// bad tables.
		sb2.append("/*after tp_sales_to_scrub is run - find those tables that have duplicate entries */\n"
				+ "\n" + " \n" + "drop table if exists TMP_TPS_"
				+ yr
				+ q
				+ "; \n"
				+ "create table TMP_TPS_"
				+ yr
				+ q
				+ " ENGINE=myisam\n"
				+ " SELECT * FROM (\n"
				+ " SELECT ACCNO,TNO,EDT2,P2,COUNT(*) C FROM "
				+ tp_sales_to_scrub
				+ " T1 WHERE YEAR(FILEDATE)="
				+ yr
				+ " AND QUARTER(FILEDATE)= "
				+ q
				+ " GROUP BY ACCNO,TNO,EDT2,P2) T1\n"
				+ " WHERE C>1; \n"
				+ "alter table TMP_TPS_"
				+ yr
				+ q
				+ " add key (accno), add key(tno), ADD KEY(P2), ADD KEY(EDT2);\n"
				+ "\n"
				+ "/*DELETE THOSE W/ MORE THAN 1 CNT FOR SAME EDT2/P2 WHERE LIKE 'JOINT|WHOLLY ETC'.*/\n"
				+ "/*TODO: COME BACK TO FIGURE OUT WHAT TO DELETE -- RUN METHOD AND SEE WHAT THIS DELETES AND HOW TO TIGHTEN AND TRY TO LEAVE TILL VERY LAST PASS!-- XXXX*/\n"
				+ "DELETE T1 FROM "
				+ tp_sales_to_scrub
				+ " T1, TMP_TPS_"
				+ yr
				+ q
				+ " T2 WHERE\n"
				+ "T1.TNO=T2.TNO AND T1.ACCNO=T2.ACCNO AND T1.P2=T2.P2 AND T1.EDT2=T2.EDT2\n"
				+ "AND (columntext rlike 'joint|wholly|previou') ;\n");

		dropProc = "DROP PROCEDURE IF EXISTS tp_sales_to_scrub2" + yr + "QTR"
				+ q + ";\n" + "CREATE PROCEDURE tp_sales_to_scrub2" + yr
				+ "QTR" + q + "()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb2.toString() + endProc);
		sb2.deleteCharAt(0);
		MysqlConnUtils.executeQuery("call tp_sales_to_scrub2" + yr + "QTR" + q
				+ "();\n");

		StringBuffer sb3 = new StringBuffer();

		// only need to call missing accs - so skip i=0
		for (int i = 0; i < rowNameWhereFilter.length; i++) {
			if (i == 0)
				continue;
			if (i > 0) {
				sb3.append(callAfterLoop1);
				rightJoinAfterLoop1 = " RIGHT JOIN TMP_MISSING_ACCNO2_" + yr
						+ q + " T2 ON T1.ACCNO=T2.ACCNO ";
			}
			sb3.append("  DROP TABLE IF EXISTS TMP_TP_RAW"
					+ yr
					+ q
					+ ";\nset sql_mode = ALLOW_INVALID_DATES;\n"
					+ "CREATE TABLE TMP_TP_RAW"
					+ yr
					+ q
					+ " ENGINE=MYISAM\n"
					+ "SELECT T1.accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value, t1.p2, LEFT(edt2,10) edt2, CASE WHEN `DEC`=0 OR `DEC` IS NULL OR `DEC`='' THEN 1 WHEN `DEC`=-3 THEN 1000 WHEN `DEC` = -6 THEN 1000000 WHEN `DEC`=-9 THEN 1000000000 ELSE `DEC` END `DEC`, columnText ,form FROM \n"
					+ " TMP_ABOVE_EXPENSE_"
					+ yr
					+ "qtr"
					+ q
					+ " T1 "
					+ rightJoinAfterLoop1
					+ "\n WHERE "
					+ rowNameWhereFilter[i]
					+ " ALTER TABLE TMP_TP_RAW"
					+ yr
					+ q
					+ " ADD PRIMARY KEY (ACCNO,tno,ROW);\n"
					+ " \n"
					+ "\n"
					+ "/*BELOW MARKS THE HIGHEST VALUES OF EACH ACCNO THEN GRABS ALL COLUMNS RELATED TO THAT VALUE ON THE SAME ROW INTO TP_SALES_PRELIM.*/\n"
					+ "\n"
					+ "\n\nset @pAcc = '1x'; set @rw=0; set @hhv=0; set @tno=-1;\n"
					+ "DROP TABLE IF EXISTS TMP_tp_sales_To_Scrub"
					+ yr
					+ q
					+ " ;\nset sql_mode = ALLOW_INVALID_DATES;\n"
					+ "CREATE TABLE TMP_tp_sales_To_Scrub"
					+ yr
					+ q
					+ " ENGINE=MYISAM\n"
					+ " SELECT T1.accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value, t1.p2, LEFT(edt2,10) edt2, `DEC`, "
					+ "columnText,form "
					+ "from (\n"
					+ "select case when @rw=0 or @pAcc!=accno or (@hhv=value and @pAcc=accno and @tno!=tno) then 1 else 0 end getIt ,@hhv:=case when @pAcc!=accno or @rw=0 then value else @hhv end hhv,@pAcc:=t1.AccNo accno, @rw:=@rw+1 rw, t1.fileDate, t1.cik, t1.tn, t1.trow"
					+ ", t1.row, t1.col, @tno:=t1.tno tno, t1.rowname, t1.value, t1.p2, LEFT(edt2,10) edt2, `DEC`, columnText,form \n"
					+ "from TMP_TP_RAW"
					+ yr
					+ q
					+ " t1 WHERE edt2 rlike '^[1,2]{1}[09]{1}[0-9]{2}' and p2 between 3 and 12 \n"
					+ "order by accno,value*`DEC` desc,trow desc) T1 \n"
					+ "WHERE getIt=1 ;\n"
					+ "ALTER TABLE TMP_tp_sales_To_Scrub"
					+ yr
					+ q
					+ " ADD PRIMARY KEY(accno),ADD KEY(ACCNO),ADD KEY(tn),ADD KEY(trow);\n"
					+ "\n"
					+ "/*don't have p2 as a condition*/\n"
					+ "\n\nset @pAcc = '1x'; set @rw=0; set @hhv=0; set @tno=-1;\n"
					+ "insert ignore into TMP_tp_sales_To_Scrub"
					+ yr
					+ q
					+ " \n"
					+ "SELECT accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value, t1.p2, LEFT(edt2,10) edt2, `DEC`"
					+ ", columnText,form \n"
					+ "from (\n"
					+ "select case when @pAcc!=accno or @rw=0 then 1 else 0 end getIt,@pAcc:=t1.AccNo accno, @rw:=@rw+1 rw,t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col"
					+ ", t1.tno, t1.rowname, t1.value, t1.p2, LEFT(edt2,10) edt2, `DEC`, columnText,form \n"
					+ "from TMP_TP_RAW"
					+ yr
					+ q
					+ " t1 WHERE edt2 rlike '^[1,2]{1}[09]{1}[0-9]{2}' AND p2<3 order by accno,tno,value*`DEC` desc,trow) T1 \n"
					+ "WHERE getIt=1 ;\n"
					+ "\n"
					+ "/*don't have edt2 as a condition*/\n"
					+ "\n\nset @pAcc = '1x'; set @rw=0; set @hhv=0; set @tno=-1;\n"
					+ "insert ignore into TMP_tp_sales_To_Scrub"
					+ yr
					+ q
					+ " \n"
					+ " SELECT accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value, t1.p2, LEFT(edt2,10) edt2, `DEC`, "
					+ "columnText,form \n"
					+ " from (\n"
					+ " select case when @rw=0 or @pAcc!=accno or (@hhv=value and @pAcc=accno and @tno!=tno) then 1 else 0 end getIt ,@hhv:=case when @pAcc!=accno or @rw=0 then value else @hhv end hhv,@pAcc:=t1.AccNo accno, @rw:=@rw+1 rw,t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col,"
					+ " @tno:=t1.tno tno, t1.rowname, t1.value, t1.p2, LEFT(edt2,10) edt2, `DEC`, columnText,form \n"
					+ " from TMP_TP_RAW"
					+ yr
					+ q
					+ " t1 WHERE edt2 not rlike '^[1,2]{1}[09]{1}[0-9]{2}' AND p2>2 order by accno,tno,value*`DEC` desc,trow) T1 \n"
					+ "WHERE getIt=1 ;\n"
					+ "\n"
					+ "/*don't have either edt2 or period as a condition*/\n"
					+ "\n\nset @pAcc = '1x'; set @rw=0; set @hhv=0; set @tno=-1;\n"
					+ "insert ignore into TMP_tp_sales_To_Scrub"
					+ yr
					+ q
					+ " \n"
					+ "SELECT accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value, t1.p2, LEFT(edt2,10) edt2, `DEC`"
					+ ", columnText,form \n"
					+ "from (\n"
					+ "select case when @rw=0 or @pAcc!=accno or (@hhv=value and @pAcc=accno and @tno!=tno) then 1 else 0 end getIt ,@hhv:=case when @pAcc!=accno or @rw=0 then value else @hhv end hhv,@pAcc:=t1.AccNo accno, @rw:=@rw+1 rw, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col,"
					+ " @tno:=t1.tno tno, t1.rowname, t1.value, t1.p2, LEFT(edt2,10) edt2, `DEC`, columnText,form \n"
					+ "from TMP_TP_RAW"
					+ yr
					+ q
					+ " t1 WHERE edt2 not rlike '^[1,2]{1}[09]{1}[0-9]{2}' AND p2<3 order by accno,tno,value*`DEC` desc,trow) T1 \n"
					+ "WHERE getIt=1 ;\n"
					+ "\n"
					+ "/*used primary key (accno,tno,trow) initially to prevent re-entry of identical rows. Dropped it inorder to allow joinder of like trows \n"
					+ "but first inserted back overall primary key of accno,tno,row*/\n"
					+ "\n"
					+ "ALTER TABLE TMP_TP_SALES_TO_SCRUB"
					+ yr
					+ q
					+ " DROP PRIMARY KEY;\n"
					+ "ALTER TABLE TMP_TP_SALES_TO_SCRUB"
					+ yr
					+ q
					+ " ADD PRIMARY KEY(ACCNO,tno,ROW);\n"
					+ "\n"
					+ "\n"
					+ "INSERT IGNORE INTO TMP_TP_SALES_TO_SCRUB"
					+ yr
					+ q
					+ " \n"
					+ "SELECT T2.accno, t2.fileDate, t2.cik, t2.tno, t2.trow, t2.row, t2.col, t2.tn, t2.rowname, t2.value, t2.p2, t2.edt2, t2.`DEC`"
					+ ", t2.columnText, t2.form FROM TMP_TP_SALES_TO_SCRUB"
					+ yr
					+ q
					+ " T1 INNER JOIN TMP_TP_RAW"
					+ yr
					+ q
					+ " T2\n"
					+ "ON T1.ACCNO=T2.ACCNO AND T1.tno=T2.tno AND T1.trow=T2.trow ;\n"
					+ "\n"
					+ "\n"

					+ "UPDATE IGNORE TMP_TP_RAW"
					+ yr
					+ q
					+ "\n set p2=0\n"
					+ "WHERE (COLUMNTEXT RLIKE 'NINE.{1,3}WEEK' AND COLUMNTEXT NOT RLIKE 'THIRTY') OR\n"
					+ " (COLUMNTEXT RLIKE 'SIX.{1,3}WEEK' AND COLUMNTEXT NOT RLIKE 'THIRTY|TWENTY')"
					+ " or (COLUMNTEXT not RLIKE 'hundred' and columntext rlike '^ONE | ONE ');\n"

					+ "\nUPDATE IGNORE TMP_TP_RAW"
					+ yr
					+ q
					+ " set p2=case \n"
					+ "when columntext rlike 'one.{1,2}hundred' and columntext rlike 'twenty|fifty|forty|thirty' then 3\n"
					+ "when columntext rlike 'three.{1,2}hundred' and columntext rlike 'fifty|sixty|forty' then 12\n"
					+ "when columntext rlike 'three.{1,2}hundred' then 0\n"
					+ "else p2 end \n"
					+ "where columntext rlike 'hundred'; \n"

					+ "INSERT IGNORE INTO "
					+ tp_sales_to_scrub
					+ "\n"
					+ "SELECT t1.* FROM TMP_TP_RAW"
					+ yr
					+ q
					+ " T1 INNER JOIN TMP_tp_sales_To_Scrub"
					+ yr
					+ q
					+ " T2 ON T1.ACCNO=T2.ACCNO AND T1.tno=T2.tno AND T1.trow=T2.trow where t1.p2 between 3 and 12;\n"
					+ " \n");

			sb3.append("DROP TABLE IF EXISTS TMP_SAME_EDT_VAL_DIF_P"
					+ yr
					+ "qtr"
					+ q
					+ ";"
					+ "/*can only run this on tp_sales b/c many values can be equal to each other \n"
					+ "in bs and other line items in a f/s. I also then need to update yr='bad' after items identified\n"
					+ "SAME EDT, SAME VALUE BUT DIF P2 - THEREFORE THEY ARE BAD*/\n"
					+ "CREATE TABLE TMP_SAME_EDT_VAL_DIF_P"
					+ yr
					+ "qtr"
					+ q
					+ " ENGINE=MYISAM\n"

					+ "SELECT T1.ACCNO,T1.TNO,T1.ROW FROM "
					+ tp_sales_to_scrub
					+ " T1 inner join "
					+ tp_sales_to_scrub
					+ " t2 \n"
					+ "on t1.cik=t2.cik and t1.edt2=t2.edt2 and t1.value=t2.value and t1.p2!=t2.p2 where t1.value>999;\n"
					+ "ALTER TABLE TMP_SAME_EDT_VAL_DIF_P"
					+ yr
					+ "qtr"
					+ q
					+ " ADD KEY(ACCNO), ADD KEY(TNO), ADD KEY(ROW);\n"
					+ "\n/*TODO: COME BACK TO FIGURE OUT WHAT TO DELETE -- RUN METHOD AND SEE WHAT THIS DELETES AND HOW TO TIGHTEN AND TRY TO LEAVE TILL VERY LAST PASS!-- XXXX*/"
					+ "DELETE T1 FROM "
					+ tp_sales_to_scrub
					+ " T1 INNER JOIN TMP_SAME_EDT_VAL_DIF_P"
					+ yr
					+ "qtr"
					+ q
					+ " T2 ON T1.ACCNO=T2.ACCNO AND "
					+ "T1.TNO=T2.TNO AND T1.ROW=T2.ROW;\n"
					+ "\n"
					+ "UPDATE IGNORE "
					+ table
					+ " t1 INNER JOIN TMP_SAME_EDT_VAL_DIF_P"
					+ yr
					+ "qtr"
					+ q
					+ " T2 ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.ROW=T2.ROW\n"
					+ "SET YR='bad';\n");

			sb3.append("   DROP TABLE IF EXISTS TMP_TP_RAW" + yr + q + ";\n"
					+ "DROP TABLE IF EXISTS TMP_TP_SALES_TO_SCRUB" + yr + q
					+ ";\nDROP TABLE IF EXISTS TMP_MISSING_ACCNO_" + yr + q
					+ ";\n DROP TABLE IF EXISTS TMP_MISSING_ACCNO2_" + yr + q
					+ ";\n DROP TABLE IF EXISTS TMP_SWITCH_" + yr + q + ";\n"
					+ "DROP TABLE IF EXISTS TMP_SAME_EDT_VAL_DIF_P" + yr
					+ "qtr" + q + ";\n");
		}

		dropProc = "DROP PROCEDURE IF EXISTS tp_sales_to_scrub3" + yr + "QTR"
				+ q + ";\n" + "CREATE PROCEDURE tp_sales_to_scrub3" + yr
				+ "QTR" + q + "()\n\n begin\n\n";
		endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb3.toString() + endProc);
		sb3.deleteCharAt(0);
		MysqlConnUtils.executeQuery("call tp_sales_to_scrub3" + yr + "QTR" + q
				+ "();\n");

		StringBuffer sb4 = new StringBuffer();

		/* shouldn't I just mark this as 'bad */
		sb4.append("/*after tp_sales_to_scrub is run - find those tables that have duplicate entries but also have:\n"
				+ " COLUMNTEXT  RLIKE 'joint|merg etc*/\n"
				+ "\n"
				+ " \n"
				+ "drop table if exists TMP_TPS_"
				+ yr
				+ q
				+ "; \n"
				+ "create table TMP_TPS_"
				+ yr
				+ q
				+ " ENGINE=myisam\n"
				+ " SELECT * FROM (\n"
				+ " SELECT ACCNO,TNO,EDT2,P2,COUNT(*) C FROM "
				+ tp_sales_to_scrub
				+ " T1 WHERE YEAR(FILEDATE)="
				+ yr
				+ " and quarter(filedate)="
				+ q
				+ " GROUP BY ACCNO,TNO,EDT2,P2) T1\n"
				+ " WHERE C>1; \n"
				+ "alter table TMP_TPS_"
				+ yr
				+ q
				+ " add key (accno), add key(tno), ADD KEY(P2), ADD KEY(EDT2);\n"
				+ "\n"

				+ "/*DELETE THOSE W/ MORE THAN 1 CNT FOR SAME EDT2/P2 WHERE NOT LIKE 'CONSOLIDATED ETC'.*/\n"
				+ "\n/*TODO: COME BACK TO FIGURE OUT WHAT TO DELETE -- RUN METHOD AND SEE WHAT THIS DELETES AND HOW TO TIGHTEN AND TRY TO LEAVE TILL VERY LAST PASS!-- XXXX*/"
				+ "DELETE T1 FROM "
				+ tp_sales_to_scrub
				+ " T1, TMP_TPS_"
				+ yr
				+ q
				+ " T2 WHERE\n"
				+ "T1.ACCNO=T2.ACCNO  AND T1.TNO=T2.TNO ; /*\n\nleave out?==>AND T1.P2=T2.P2 AND T1.EDT2=T2.EDT2\n*/\n"
				+ "\n/*<<=delete all b/c these are marked as 'bad' tables. Prior instances were not 'bad' tbls.*/");

		dropProc = "DROP PROCEDURE IF EXISTS tp_sales_to_scrub4" + yr + "QTR"
				+ q + ";\n" + "CREATE PROCEDURE tp_sales_to_scrub4" + yr
				+ "QTR" + q + "()\n\n begin\n\n";
		endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb4.toString() + endProc);
		sb4.deleteCharAt(0);
		MysqlConnUtils.executeQuery("call tp_sales_to_scrub4" + yr + "QTR" + q
				+ "();\n");

		StringBuffer sb5 = new StringBuffer(
				"DROP TABLE IF EXISTS TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "1;\n"
						+ " DROP TABLE IF EXISTS TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "2;\n"
						+ " DROP TABLE IF EXISTS TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "3;\n"
						+ " DROP TABLE IF EXISTS TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "4;\n"
						+ "DROP TABLE IF EXISTS TMP_MISSING_ACCNO_"
						+ yr
						+ q
						+ " ;\n"
						+ "\n\nDROP TABLE IF EXISTS TMP_MISSING_ACCNO2_"
						+ yr
						+ q
						+ " ;\n"
						+ "DROP TABLE IF EXISTS TMP_TP_RAW"
						+ yr
						+ q
						+ ";"
						+ "DROP TABLE IF EXISTS tmp_getIt"
						+ yr
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS TMP_tp_sales_To_Scrub"
						+ yr
						+ q
						+ " ;\n"

						+ "  DROP TABLE IF EXISTS TMP_TP_RAW"
						+ yr
						+ q
						+ ";\n"
						+ "  DROP TABLE IF EXISTS TMP_TP_SALES_TO_SCRUB"
						+ yr
						+ q
						+ ";\nDROP TABLE IF EXISTS TMP_MISSING_ACCNO_"
						+ yr
						+ q
						+ ";\n DROP TABLE IF EXISTS TMP_MISSING_ACCNO2_"
						+ yr
						+ q
						+ ";\n DROP TABLE IF EXISTS TMP_SWITCH_"
						+ yr
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp1;\n DROP TABLE IF EXISTS tmp2;\n DROP TABLE IF EXISTS tmp3; \n"
						+ "DROP TABLE IF EXISTS tmp1;\n DROP TABLE IF EXISTS tmp2;\n DROP TABLE IF EXISTS tmp3; \n"
						+ "  DROP TABLE IF EXISTS TMP_TP_RAW"
						+ yr
						+ q
						+ ";\n"
						+ "  DROP TABLE IF EXISTS TMP_TP_SALES_TO_SCRUB"
						+ yr
						+ q
						+ ";\nDROP TABLE IF EXISTS TMP_MISSING_ACCNO_"
						+ yr
						+ q
						+ ";\n DROP TABLE IF EXISTS TMP_MISSING_ACCNO2_"
						+ yr
						+ q
						+ ";\n DROP TABLE IF EXISTS TMP_SWITCH_"
						+ yr
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp1;\n DROP TABLE IF EXISTS tmp2;\n DROP TABLE IF EXISTS tmp3; \n");

		dropProc = "DROP PROCEDURE IF EXISTS tp_sales_to_scrub5" + yr + "QTR"
				+ q + ";\n" + "CREATE PROCEDURE tp_sales_to_scrub5" + yr
				+ "QTR" + q + "()\n\n begin\n\n";
		endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb5.toString() + endProc);
		sb5.deleteCharAt(0);
		MysqlConnUtils.executeQuery("call tp_sales_to_scrub5" + yr + "QTR" + q
				+ "();\n");

		sb.delete(0, sb.toString().length());
		sb5.deleteCharAt(0);

	}

	public void getSalesFromBlankRows(String table, int cikStart, int cikEnd,
			String tp_sales_to_scrub) throws SQLException, FileNotFoundException {

		/*
		 * This finds a higher sales value in a table row that is early in the
		 * IS table and then runs various checks to see if preceding rows are
		 * costs / expense like rows. Passing that - it then grabs the highest
		 * high value for any early row that is blank and see if that value for
		 * that accno is higher than the highest high value for the same accno
		 * in tp_sales_to_scrub. If it is higher - delete the accno from
		 * tp_sales_to_scrub and insert the found one
		 */

		String yr = table.substring(table.indexOf("tp_raw") + 6,
				table.indexOf("tp_raw") + 10);
		String q = table.substring(table.indexOf("tp_raw") + 13,
				table.indexOf("tp_raw") + 14);
		int yrInt = Integer.parseInt(yr);

		String p = "", whereP = "";
		if (table.substring(0, 2).equals("p3")) {
			p = "=3 ";
			whereP = " where p2=3 ";
		} else {
			p = " between 3 and 12 ";
			whereP = " where p2 between 3 and 12 ";
		}

		StringBuffer sb = new StringBuffer(
				"DROP TABLE IF EXISTS TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "a;\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "CREATE TABLE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "a ENGINE=MYISAM\n"
						+ "SELECT T1.accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value, t1.p2, LEFT(edt2,10) edt2\n"
						+ ", CASE WHEN `DEC`=0 OR `DEC` IS NULL OR `DEC`='' THEN 1 WHEN `DEC`=-3 THEN 1000 WHEN `DEC` = -6 THEN 1000000 WHEN `DEC`=-9 THEN 1000000000 ELSE `DEC` END `DEC`, columnText ,form FROM \n"
						+ " TMP_ABOVE_EXPENSE_"
						+ yr
						+ "qtr"
						+ q
						+ " T1  \n"
						+ "WHERE ((trow<18 AND \n"
						+ " (ROWNAME RLIKE '(TOTAL|GROSS) (NET |OPERATING |INTEREST )?(SALES|REVENU|INCOME)'\n"
						+ " OR TRIM(ROWNAME) ='REVENUES' OR TRIM(ROWNAME) = 'REVENUE' OR TRIM(ROWNAME) ='REVENUES:' OR TRIM(ROWNAME) = 'REVENUE:' OR TRIM(ROWNAME) = 'REVENU' \n"
						+ " OR TRIM(ROWNAME) ='SALES' OR TRIM(ROWNAME) ='SALES:' OR TRIM(ROWNAME) = 'NET SALES' OR TRIM(ROWNAME) = 'NET SALES:' \n"
						+ " OR TRIM(ROWNAME) ='OPERATING REVENUES' OR TRIM(ROWNAME) = 'OPERATING REVENUE' OR TRIM(ROWNAME) ='OPERATING REVENUES:'\n"
						+ " OR TRIM(ROWNAME) = 'OPERATING REVENUE:' OR TRIM(ROWNAME) ='OPERATING REVENU' )) OR (TROW<9) )\n"

						+ "and tn='is' and value>999 and\n"
						+ "value !="
						+ yr
						+ " and value!="
						+ (yrInt - 1)
						+ " and value!= "
						+ (yrInt - 2)
						+ " and value!= "
						+ (yrInt - 3)
						+ " and value!="
						+ (yrInt - 4)
						+ " and value!="
						+ (yrInt - 5)
						+ "\n"
						+ "AND columnText not like '%pro%forma%' and  (htmltxt='html' or (htmltxt!='html' and "
						+ "allColText not like '%pro%forma%' and \n"
						+ "ColumnPattern not like '%pro%forma%')) \n"
						+ "AND tno<7 AND (yr!='bad' or yr is null) \n"
						+ "AND COLUMNTEXT NOT RLIKE 'subsidiar|mortgag|eliminat|guarantor|historical|portfolio|segments|annuity|health|investment"
						+ "|other|under|closed|percent|increas|decreas'\n"
						+ " AND ( (rowname not rlike \n"
						+ "'non.{0,1}gaap|bond|(held|available).{1,2}(for|to)|defer|taxe|expense|unbill|unearn|proceeds|"
						+ "marketi|member|segment|addition|impair|admin|gain.{1,3}on.{1,3}|write.{1,3}(down|off)|realiz|"
						+ "decrea|increas|change|(account|finance).{1,3}receivabl|extraordinary|adjustm|reconil|accrued|"
						+ "disposed|discontin|cost|depreciat|amortizat|loss|sales? of|gains'\n"
						+ " or (rowname rlike 'net of expense' )  or (rowname rlike 'revenu|net of|net (sale|revenu)|excis'  ) \n"
						+ ")\n"
						+ " or (ROWNAME RLIKE 'sales? of|loss|gains' and ROWNAME RLIKE \n"
						+ "'(net|gross|total) sale|(sales? of )(cement|commission|concentra|develop|fine|fruit|good|gold|health|homes|insuranc"
						+ "|medical|communica|natural|oil|product|recycl|servic|sugar|turnkey|used car|graphic|gas)')\n"
						+ ") \n"
						+ " and (ttl>=0 or ttl is null)  and p2"
						+ p
						+ " and length(edt2)=10 and value>0 "
						+ "and cik between "
						+ cikStart
						+ " and "
						+ cikEnd
						+ ";\n"

						+ "alter table TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "a add key(accno), add key(tno), add key(value), add key(trow), add key(edt2), add key(p2), change `dec` `dec` int(10);\n"
						+ "\n/*<<==above get all possilbe tables subject to various filters. This then become universe of potential \nadditional tables to see if we don't have correct sales value\n*/"
						+ "\n\nset @pAcc = '1x'; set @rw=0;\n"
						+ "DROP TABLE IF EXISTS TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "b ;\n"
						+ "CREATE TABLE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "b ENGINE=MYISAM\n"
						+ " select case when @rw=0 or @pAcc!=accno then 1 else 0 end getIt,@rw:=@rw+1 rw,@pAcc:=t1.AccNo accno, t1.fileDate, t1.cik, t1.tn, t1.trow\n"
						+ ", t1.row, t1.col, t1.tno, t1.rowname, t1.value, t1.p2, LEFT(edt2,10) edt2, `DEC`, columnText,form \n"
						+ "from  TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "a t1 WHERE length(edt2)=10 AND p2"
						+ p
						+ " \n"
						+ "order by accno,value*`DEC` desc,trow desc;\n"
						+ "ALTER TABLE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "b add key (getit), change `dec` `dec` int(10);\n"
						+ "/*<<==above gets highest high value and marks it as 1. This is the potential table row I need*/"
						+ "\n\nDROP TABLE IF EXISTS TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_2 ;\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "CREATE TABLE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_2 ENGINE=MYISAM\n"
						+ " SELECT T1.accno,t1.trow, t1.tno from TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "b T1 \n"
						+ "WHERE getIt=1 and p2"
						+ p
						+ "/*TOOK OUT -- NOT NECESSARY- AND MISSES SOME :==>and (rowname like ';%' or length(rowname)<2)*/ ;\n"
						+ "ALTER TABLE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_2 ADD KEY(ACCNO), ADD KEY(TNO), ADD KEY(TROW);\n"
						+ "\n"
						+ "\n"
						+ "/*now check if any of the tables below have rows with values thta include cost or expenses*/\n"
						+ "\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_3;\n"
						+ "CREATE TABLE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_3 ENGINE=MYISAM\n"
						+ "SELECT T2.* FROM TMP_ABOVE_EXPENSE_"
						+ yr
						+ "qtr"
						+ q
						+ " T1 INNER JOIN TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_2 T2\n"
						+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO\n"
						+ "WHERE T1.TROW<=T2.TROW AND ROWNAME RLIKE "
						+ " 'COST.{1,2}OF|COST.{1,2}AND.{1,2}EXPENSE|COGS|GROSS.{0,2}(PROFIT|MARGIN)|(advertising|general|debt|benefit|selling|other|claims and|control|communication|corporate|costs|processing|utilities|tax|fuel|related).{1,3}expense|depreciat|amortiz|research|administrat'\n"
						+ "and p2"
						+ p
						+ ";\n"

						// NEED TO DESIGN BETTER MOUSE TRAP WHEN ADDING BLANK
						// ROWS IN REPLACE OF REVENUE IDENTIFIED TOP Line.
						// I MAY WANT TO PLACE IN SEPARATE CONTAINER TO HOLD THE
						// BLANK ROW NOS AND AFTERWARDS GO BACK TO
						// TP_SALES_TO_SCRUB
						// AND DO ANALYSIS OF ROWNAMES AND VALUES TO SEE WHICH
						// ROW IS CORRECT AND THEN ALWAYS USE THAT ROW. AT THE
						// MOMENT
						// I'M GETTING TOO MUCH VARIANCE AND THAT'S CAUSING
						// Y-O-Y ERRORS.

						+ "ALTER TABLE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_3 ADD KEY(ACCNO), ADD KEY(TNO), ADD KEY(TROW);\n"
						+ "\n"
						+ "DELETE T1 FROM TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_2 T1, TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_3 T2 \n"
						+ "WHERE T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO  ;\n"
						+ "\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_4;\n"
						+ "CREATE TABLE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_4 ENGINE=MYISAM\n"
						+ "SELECT t1.* FROM TMP_ABOVE_EXPENSE_"
						+ yr
						+ "qtr"
						+ q
						+ " T1 INNER JOIN\n"
						+ "TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_2 T2 ON T1.ACCNO=T2.ACCNO and t1.tno=t2.tno\n"
						+ "WHERE T1.TROW<=T2.TROW and p2"
						+ p
						+ ";\n"
						+ "ALTER TABLE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_4 ADD KEY(ACCNO), ADD KEY(TNO), ADD KEY(TROW), change `dec` `dec` int(10);\n"
						+ "\n"
						+ "/*grab max row of each accno,tno -- that is row w/ max value. then see if that value is > than value in tp_sales_to_scrub.\n"
						+ "If so -delete and replace with this. B/c costs|expense occuring prior to max row will kick out the tno -- highly unlikely \n"
						+ "any of this is wrong AND value is greater than what was parsed into tp_sales_to_scrub. Need to limit it to 1 tno - so now I get highest high value - and I need to include other exclusion, i.e,. yr=bad, pro format etc\n"
						+ "- basically all the same processes as tp_sales_to_scrub except w/o rowname filter.*/\n"
						+ "\n"
						+ "SET @tno=-1; SET @acc='0'; SET @row=0;\n"
						+ "DROP TABLE IF EXISTS TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_5;\n"
						+ "CREATE TABLE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_5 engine=myisam\n"
						+ "SELECT @row:=@row+1 row,case when @acc!=accno or @tno!=tno or @row=1 then 1 else 0 end getIt, @acc:=accno accno, @tno:=tno tno,trow,value,'blank' blnk\n"
						+ " FROM TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_4 t1 "
						+ whereP
						+ " order by accno,tno,value desc,trow desc;\n"
						+ "ALTER TABLE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_5 ADD KEY(GETIT), ADD KEY(ACCNO), ADD KEY(TROW), ADD KEY(TNO);\n"
						+ "\n\n"
						+ "delete from TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_5 where getit!=1;"
						+ "\n\n"

						+ "DROP TABLE IF EXISTS TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_6a;\n"
						+ "CREATE TABLE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_6a engine=myisam\n"
						+ "SELECT \n t1.accno,t1.filedate,t1.cik,t1.tn,t1.trow,t1.row,t1.col,t1.tno,t1.rowname,t1.value,t1.p2,t1.edt2,t1.`dec`,t1.columntext,t1.form\n"
						+ ",1 BLANK FROM TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_5 T2 INNER JOIN "
						+ " TMP_ABOVE_EXPENSE_"
						+ yr
						+ "qtr"
						+ q
						+ " T1\n"
						+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO  "
						+ whereP
						+ " ;\n"
						+ "/*delete join on trow*/\n"
						+ "ALTER TABLE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_6a ADD KEY(ACCNO), ADD KEY(TROW), ADD KEY(TNO), change `dec` `dec` int(10);\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_6;\n"
						+ "CREATE TABLE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_6 engine=myisam\n"
						+ "SELECT t1.accno,t1.filedate,t1.cik,t1.tn,t1.trow,t1.row,t1.col,t1.tno,t1.rowname,t1.value,t1.p2,t1.edt2,t1.`dec`,t1.columntext,t1.form\n"
						+ ",1 BLANK FROM TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_6a T1 INNER JOIN TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_5 T2\n"
						+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO and t1.trow=t2.trow "
						+ whereP
						+ " ;\n"
						+ "ALTER TABLE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_6 ADD KEY(ACCNO), ADD KEY(TROW), ADD KEY(TNO), change `dec` `dec` int(10);"

						+ "/*label blanks as 1 and tp_sales as 0 and if when I order by accno,tno,edt,p2 desc,val the 1 is largest - then delete that same accno from \n"
						+ "tp_sales_to_scrub*/\n"
						+ "\n"
						+ "INSERT IGNORE INTO TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_6\n"
						+ "SELECT accno,filedate,cik,tn,trow,row,col,tno,rowname,value,p2,edt2,`dec`,columntext,form,0 blank from "
						+ tp_sales_to_scrub
						+ " t1"
						+ "\n"
						+ whereP
						+ " \n;\n\n"
						+ "set @acc = '1x'; set @rw=0;\n"
						+ "DROP TABLE IF EXISTS TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_7;\n"
						+ "CREATE TABLE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_7 engine=myisam\n"
						+ "select case when @acc!=accno or @rw=0 then blank else 'xx' end getIt,@rw:=@rw+1 rw,\n"
						+ "@acc:=accno accno,filedate,cik,tn,trow,row,col,tno,rowname,value,p2,edt2,`dec`,columntext,form,blank \n"
						+ "from (select * from TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_6 "
						+ whereP
						+ " order by accno,value desc) t1;\n"
						+ "ALTER TABLE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_7  ADD KEY(ACCNO), ADD KEY(TROW), ADD KEY(TNO), add key(getIt), change `dec` `dec` int(10);\n"
						+ "\n"
						+ "/*above identifies those accno/tno that have the wrong row in a tno -- those are ones where getIt=1.\n"
						+ "So I delete those accno from tp_sales_to_scrub and then insert same accno but with correct row (correct value)*/\n"
						+ "\n"
						+ "\n/*TODO: COME BACK TO FIGURE OUT WHAT TO DELETE -- RUN METHOD AND SEE WHAT THIS DELETES AND HOW TO TIGHTEN AND TRY TO LEAVE TILL VERY LAST PASS!-- XXXX*/"
						+ "DELETE T1 FROM "
						+ tp_sales_to_scrub
						+ " T1 INNER JOIN TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_7 T2\n"
						+ "ON T1.ACCNO=T2.ACCNO WHERE GETIT=1 and t1.p2"
						+ p
						+ ";\n"
						+ "\n"

						+ "UPDATE IGNORE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_7"
						+ "\n set p2=0\n"
						+ "WHERE (COLUMNTEXT RLIKE 'NINE.{1,3}WEEK' AND COLUMNTEXT NOT RLIKE 'THIRTY') OR\n"
						+ " (COLUMNTEXT RLIKE 'SIX.{1,3}WEEK' AND COLUMNTEXT NOT RLIKE 'THIRTY|TWENTY')"
						+ " or (COLUMNTEXT not RLIKE 'hundred' and columntext rlike '^ONE | ONE ');\n"

						+ "\nUPDATE IGNORE TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_7"
						+ " set p2=case \n"
						+ "when columntext rlike 'one.{1,2}hundred' and columntext rlike 'twenty|fifty|forty|thirty' then 3\n"
						+ "when columntext rlike 'three.{1,2}hundred' and columntext rlike 'fifty|sixty|forty' then 12\n"
						+ "when columntext rlike 'three.{1,2}hundred' then 0\n"
						+ "else p2 end \n"
						+ "where columntext rlike 'hundred'; \n\n"

						+ "INSERT IGNORE INTO "
						+ tp_sales_to_scrub
						+ "\n"
						+ "SELECT T2.ACCNO,T2.FILEDATE,T2.CIK,T2.TN,T2.TROW,T2.ROW,T2.COL,T2.TNO,T2.ROWNAME,T2.VALUE,T2.P2,T2.EDT2\n"
						+ ",T2.`DEC`,T2.COLUMNTEXT,T2.FORM"
						+ " FROM TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_7 T1 \n"
						+ "INNER JOIN TMP_BLANKROW_"
						+ yr
						+ "qtr"
						+ q
						+ "_7 T2 ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.TROW=T2.TROW\n"
						+ "WHERE T1.GETIT=1 and t2.p2 " + p + " and t1.p2 " + p
						+ " ;\n");

		String dropProc = "DROP PROCEDURE IF EXISTS getSalesFromBlankRows" + yr
				+ "QTR" + q + ";\n" + "CREATE PROCEDURE getSalesFromBlankRows"
				+ yr + "QTR" + q + "()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
		sb.delete(0, sb.toString().length());
		
		MysqlConnUtils.executeQuery("call getSalesFromBlankRows" + yr + "QTR"
				+ q + "();\n");
		sb.delete(0, sb.toString().length());

	}

	public void updateEdt2p2(String table) throws SQLException, FileNotFoundException {

		String yr = table.substring(table.indexOf("tp_raw") + 6,
				table.indexOf("tp_raw") + 10);
		String q = table.substring(table.indexOf("tp_raw") + 13,
				table.indexOf("tp_raw") + 14);

		StringBuffer sb = new StringBuffer();

		sb.append("DROP TABLE IF EXISTS TMP1"
				+ yr
				+ "qtr"
				+ q
				+ ";\n\n"

				+ "/*this will pair when edt2 only has year value with mo/day from TS pattern IF year in edt2 matchs TS's "
				+ "(except when tsShort pattern=just 1 M and no Y). In addition I use col number and tc pairing (eg PMYY"
				+ " must be two cols and last Y goes with col2). B/c TS is not 100% reliable I require year in edt2 to match"
				+ " year in TS.*/\n"
				+ "\nDROP TABLE IF EXISTS TMP1"
				+ yr
				+ "qtr"
				+ q
				+ ";\n"
				+ "CREATE TABLE `tmp1"
				+ yr
				+ "qtr"
				+ q
				+ "` (\n"
				+ "  `accno` varchar(20) NOT NULL DEFAULT '-1',\n"
				+ "  `tno` int(5) NOT NULL DEFAULT '-1',\n"
				+ "  `row` int(5) NOT NULL DEFAULT '-1' COMMENT 'table row',\n"
				+ "  `y` varchar(10) DEFAULT NULL,\n"
				+ "  `EDT3` varchar(25) DEFAULT NULL,\n"
				+ "  KEY `accno` (`accno`,`tno`,`row`),\n"
				+ "  KEY `accno_2` (`accno`),\n"
				+ "  KEY `tno` (`tno`),\n"
				+ "  KEY `row` (`row`)\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n\n"

				+ "insert ignore into tmp1"
				+ yr
				+ "qtr"
				+ q
				+ "\n"
				+ "select accno,tno,row,@y:=replace(edt2,'-','') y,\n"
				+ "case \n"
				+ "when tsShort='M' then concat(@y,'-',right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort = 'PM' then concat(@y,'-',right(left(tsLong,8),2),'-',right(left(tsLong,10),2))\n"
				+ "when tsShort = 'PPM' then concat(@y,'-',right(left(tsLong,13),2),'-',right(left(tsLong,15),2))\n"
				+ "when tsShort='PMY' and @y=right(left(tslong,15),4) and tc=1 then concat(right(left(tslong,15),4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2)) \n"
				+ "when tsShort='PMYPMY' and @y=right(left(tslong,15),4) and tc=2 and col=1 then concat(right(left(tslong,15),4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2)) \n"
				+ "when tsShort='PMYPMY' and @y=right(left(tslong,30),4) and tc=2 and col=2 then concat(right(left(tslong,30),4),'-', right(left(tsLong,23),2),'-',right(left(tsLong,25),2)) \n"
				+ "when tsShort='MYY' and @y=right(left(tslong,10),4) and tc=2 and col=1 then concat(right(left(tslong,10),4),'-', right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort='MYY' and @y=right(left(tslong,15),4) and tc=2 and col=2 then concat(right(left(tslong,15),4),'-', right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort='PMYY' and @y=right(left(tslong,15),4) and tc=2 and col=1 then concat(right(left(tslong,15),4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2))\n"
				+ "when tsShort='PMYY' and @y=right(left(tslong,20),4) and tc=2 and col=2 then concat(right(left(tslong,20),4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2))\n"
				+ "when tsShort='PMYYY' and @y=right(left(tslong,15),4) and tc=3 and col=1 then concat(right(left(tslong,15),4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2))\n"
				+ "when tsShort='PMYYY' and @y=right(left(tslong,20),4) and tc=3 and col=2 then concat(right(left(tslong,20),4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2))\n"
				+ "when tsShort='PMYYY' and @y=right(left(tslong,25),4) and tc=3 and col=3 then concat(right(left(tslong,25),4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2))\n"
				+ "when tsShort='MYYY' and @y=right(left(tslong,10),4) and tc=3 and col=1 then concat(right(left(tslong,10),4),'-', right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort='MYYY' and @y=right(left(tslong,15),4) and tc=3 and col=2 then concat(right(left(tslong,15),4),'-', right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort='MYYY' and @y=right(left(tslong,20),4) and tc=3 and col=3 then concat(right(left(tslong,20),4),'-', right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort='MYMY' and @y=right(left(tslong,10),4) and tc=2 and col=1 then concat(right(left(tslong,10),4),'-', right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort='MYMY' and @y=right(left(tslong,20),4) and tc=2 and col=2 then concat(right(left(tslong,20),4),'-', right(left(tsLong,13),2),'-',right(left(tsLong,15),2))\n"
				+ "when tsShort='PMYMY' and @y=right(left(tslong,15),4) and tc=2 and col=1 then concat(right(left(tslong,15),4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2))\n"
				+ "when tsShort='PMYMY' and @y=right(left(tslong,25),4) and tc=2 and col=2 then concat(right(left(tslong,25),4),'-', right(left(tsLong,18),2),'-',right(left(tsLong,20),2))\n"
				+ "when tsShort='PMYMYMY' and @y=right(left(tslong,15),4) and tc=3 and col=1 then concat(right(left(tslong,15),4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2))\n"
				+ "when tsShort='PMYMYMY' and @y=right(left(tslong,25),4) and tc=3 and col=2 then concat(right(left(tslong,25),4),'-', right(left(tsLong,18),2),'-',right(left(tsLong,20),2))\n"
				+ "when tsShort='PMYMYMY' and @y=right(left(tslong,35),4) and tc=3 and col=3 then concat(right(left(tslong,35),4),'-', right(left(tsLong,28),2),'-',right(left(tsLong,30),2))\n"
				+ "when tsShort='PPMY' and @y=right(left(tslong,20),4) and tc=2 then concat(right(left(tslong,20),4),'-', right(left(tsLong,13),2),'-',right(left(tsLong,16),2))\n"
				+ "when tsShort='MYYMY' and @y=right(left(tslong,10),4) and tc=3 and col=1 then concat(right(left(tslong,10),4),'-', right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort='MYYMY' and @y=right(left(tslong,15),4) and tc=3 and col=2 then concat(right(left(tslong,15),4),'-', right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort='MYYMY' and @y=right(left(tslong,25),4) and tc=3 and col=3 then concat(right(left(tslong,25),4),'-', right(left(tsLong,18),2),'-',right(left(tsLong,20),2))\n"
				+ "else 'xx' end 'edt3'\n"
				+ "/*@xtn:= case when htmlTxt='html' then '.htm' else '.txt' end xtn\n"
				+ ", @qtr:= left(round(((month(filedate)-1)/3),2),1)+1 qtr,\n"
				+ "concat('file:///c:/backtest/tableparser/',left(filedate,4),'/qtr',@qtr,'/tables/',accno,'_',tno,@xtn) link,*/\n"
				+ "/*,tno,left(rowname,15),value,col,tc,edt1,edt2,p1,p2,tsShort,tsLong,htmlTxt,mo,yr,ended,columnPattern,allcolText,filedate,ts*/\n"
				+ "from "
				+ table
				+ " \n"
				+ "where length(edt2)!=10 \n"
				+ "and tsShort!='-1' and trim(tsShort)!=''\n"
				+ "and edt2 rlike '[1,2]{1}[0,9]{1}[0-9]{2}'\n"
				+ "and edt1 rlike '[1,2]{1}[0,9]{1}[0-9]{2}'\n"
				+ "and trim(length(replace(edt2,'-','')))=4\n"
				+ "order by accno,tno,row;\n"
				+ "ALTER TABLE TMP1"
				+ yr
				+ "qtr"
				+ q
				+ " ADD KEY (ACCNO,tno,ROW), ADD KEY(ACCNO),ADD KEY(tno),ADD KEY(ROW), "
				+ "CHANGE EDT3 EDT3 VARCHAR(25);\n"

				+ "\nset sql_mode = ALLOW_INVALID_DATES;\n\nUPDATE IGNORE "
				+ table
				+ "  T1 \n"
				+ "INNER JOIN TMP1"
				+ yr
				+ "qtr"
				+ q
				+ " T2\n"
				+ "ON T1.ACCNO=T2.ACCNO AND T1.tno=T2.tno AND T1.ROW=T2.ROW\n"
				+ "set ENDED='EDT2', edt2=edt3\n"
				+ "WHERE EDT3!='XX';\n\n"

				+ "DROP TABLE IF EXISTS TMP1"
				+ yr
				+ "qtr"
				+ q
				+ ";\n"
				+ "CREATE TABLE `tmp1"
				+ yr
				+ "qtr"
				+ q
				+ "` (\n"
				+ "  `accno` varchar(20) NOT NULL DEFAULT '-1',\n"
				+ "  `tno` int(5) NOT NULL DEFAULT '-1',\n"
				+ "  `row` int(5) NOT NULL DEFAULT '-1' COMMENT 'table row',\n"
				+ "  `per3` varchar(25) DEFAULT NULL,\n"
				+ "  KEY `accno` (`accno`,`tno`,`row`),\n"
				+ "  KEY `accno_2` (`accno`),\n"
				+ "  KEY `tno` (`tno`),\n"
				+ "  KEY `row` (`row`)\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n\n"

				+ "insert ignore into tmp1"
				+ yr
				+ "qtr"
				+ q
				+ "\n"
				+ "select accno,tno,row,\n"
				+ "case \n"
				+ "when (tsShort = 'P' or tsShort='PM') \n"
				+ "and (substring_index(right(left(tsLong,5),4),'.',1)=p1 or p1=0) \n"
				+ "then substring_index(right(left(tsLong,5),4),'.',1)\n"
				+ "when tsShort = 'PMYPMY' and tc=2 and col=1 \n"
				+ "and (substring_index(right(left(tsLong,5),4),'.',1)=p1 or p1=0)\n"
				+ "then substring_index(right(left(tsLong,5),4),'.',1)\n"
				+ "when tsShort = 'PMYPMY' and tc=2 and col=2\n"
				+ "AND (substring_index(right(left(tsLong,10),4),'.',1) = p1 or p1=0)\n"
				+ "then substring_index(right(left(tsLong,10),4),'.',1)\n"
				+ "else 'xx' end 'per3'\n"
				+ "/*,@xtn:= case when htmlTxt='html' then '.htm' else '.txt' end xtn\n"
				+ ", @qtr:= left(round(((month(filedate)-1)/3),2),1)+1 qtr,\n"
				+ "concat('file:///c:/backtest/tableparser/',left(filedate,4),'/qtr',@qtr,'/tables/',accno,'_',tno,@xtn) link,tno,tn,row\n"
				+ ",left(rowname,15),value,col,tc,edt1,edt2,p1,p2,tsShort,tsLong,mo,yr,ended\n"
				+ ",columnPattern,allcolText,filedate,ts*/\n"
				+ "from  "
				+ table
				+ " \n"
				+ "where tsShort!='-1' and trim(tsShort)!=''\n"
				+ "and p2 not rlike '(3|6|9|12)'\n"
				+ "and col!=0 and tn!='bs'\n"
				+ "order by accno,tno,row;\n"
				+ "\n\n"
				+ "\nset sql_mode = ALLOW_INVALID_DATES;\nUPDATE IGNORE  "
				+ table
				+ "  T1 \n"
				+ "INNER JOIN TMP1"
				+ yr
				+ "qtr"
				+ q
				+ " T2\n"
				+ "ON T1.ACCNO=T2.ACCNO AND T1.tno=T2.tno AND T1.ROW=T2.ROW\n"
				+ "set mo='p2' , p2=per3\n"
				+ "WHERE per3!='XX' and per3>0;\n"

				+ "DROP TABLE IF EXISTS TMP1"
				+ yr
				+ "qtr"
				+ q
				+ ";\n"

				+ "\n/*same as earlier technique that uses columnPattern except here I found instances where there "
				+ "is only 1 Month and then put it with each edt2 year.*/\n"

				+ "DROP TABLE IF EXISTS TMP1"
				+ yr
				+ "qtr"
				+ q
				+ ";\n"

				+ "\n\n/*when edt2 isn't complete and has year - gets month/day from tablesentence (tsLong) when there is ony 1 unique month.*/\n"
				+ "DROP TABLE IF EXISTS TMP1"
				+ yr
				+ "qtr"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP1"
				+ yr
				+ "qtr"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "select accno,tno,row, case \n"
				+ "when tsShort rlike '^M' and LENGTH(TSSHORT)>2 then concat(left(edt2,4),'-',left(right(left(tslong,5),4),2),'-',right(right(left(tslong,5),4),2))\n"
				+ "when tsShort rlike '^[PY]{1}M' then concat(left(edt2,4),'-',left(right(left(tslong,10),4),2),'-',right(right(left(tslong,10),4),2))\n"
				+ "when tsShort rlike '^[PY]{2}M' then concat(left(edt2,4),'-',left(right(left(tslong,15),4),2),'-',right(right(left(tslong,15),4),2))\n"
				+ "when tsShort rlike '^[PY]{3}M' then concat(left(edt2,4),'-',left(right(left(tslong,20),4),2),'-',right(right(left(tslong,20),4),2))\n"
				+ "else 'xx' end edt3/*,right(TSlong,7),right(columnPattern,7),\n"
				+ "@xtn:= case when htmlTxt='html' then '.htm' else '.txt' end xtn\n"
				+ ", @qtr:= left(round(((month(filedate)-1)/3),2),1)+1 qtr,\n"
				+ "concat('file:///c:/backtest/tableparser/',left(filedate,4),'/qtr',@qtr,'/tables/',accno,'_',tno,@xtn) link\n"
				+ ",tno,left(rowname,15),value,col,tc,edt1,edt2,p1,p2,tsShort,tsLong,htmlTxt,mo,yr,ended\n"
				+ ",columnPattern,allcolText,filedate,ts,accno,concat('https://www.sec.gov/Archives/edgar/data/',cik,'/',accno,'-index.htm')*/\n"
				+ " from  "
				+ table
				+ "  where col!=0 \n"
				+ "and length(edt2)<10 and edt2 rlike '[12]{1}[09]{1}[0-9]{2}'\n"
				+ "and right(columnPattern,7) rlike 'mCntD:(1|0)' and right(tsLong,7) rlike 'mCntD:1';\n"

				+ "ALTER TABLE TMP1"
				+ yr
				+ "qtr"
				+ q
				+ " ADD KEY (ACCNO,tno,ROW), ADD KEY(ACCNO),ADD KEY(tno),ADD KEY(ROW)"
				+ ", CHANGE EDT3 EDT3 VARCHAR(25);\n"

				+ "\nset sql_mode = ALLOW_INVALID_DATES;\n\nUPDATE IGNORE  "
				+ table
				+ "  T1 \n"
				+ "INNER JOIN TMP1"
				+ yr
				+ "qtr"
				+ q
				+ " T2\n"
				+ "ON T1.ACCNO=T2.ACCNO AND T1.tno=T2.tno AND T1.ROW=T2.ROW\n"
				+ "set ENDED='EDT2' , edt2=edt3\n"
				+ "WHERE EDT3!='XX';\n"
				+ "\n\n/*gets edt2 if tc=2 or 4 and edt2 yr edt1 yr and edt1 has yyyy-m-dd*/"
				+ "\nset sql_mode = ALLOW_INVALID_DATES;"
				+

				"\n/* "
				+ "UPDATE IGNORE "
				+ table
				+ "\n"
				+ "set edt2=edt1, ended='EDT2'\n"
				+ "where col!=0 and tc<5\n"
				+ "and length(edt2)!=10 \n"
				+ "and left(edt2,4)=left(edt1,4) and length(trim(edt1))=10 and edt1 rlike '[12]{1}[09]{1}[0-9]{2}-[0-9]{2}-[0-9]{2}'\n"
				+ "and (tc=4 or tc=2) ;"
				+ "*/\n"
				+

				"/*gets period from allcolText/columnPattern and TSLong when pCntD=1*/\n\n"
				+ "DROP TABLE IF EXISTS TMP1"
				+ yr
				+ "qtr"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP1"
				+ yr
				+ "qtr"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "select accno,tno,row, case \n"
				+ "when tsShort rlike '^P' then replace(right(left(tslong,5),4),'.00','')\n"
				+ "when tsShort rlike '^[MY]{1}P' then replace(right(left(tslong,10),4),'.00','')\n"
				+ "when tsShort rlike '^[MY]{2}P' then replace(right(left(tslong,15),4),'.00','')\n"
				+ "when tsShort rlike '^[MY]{3}P' then replace(right(left(tslong,20),4),'.00','')\n"
				+ "else 'xx' end per3\n"
				+ "/*,@xtn:= case when htmlTxt='html' then '.htm' else '.txt' end xtn\n"
				+ ", @qtr:= left(round(((month(filedate)-1)/3),2),1)+1 qtr,\n"
				+ "concat('file:///c:/backtest/tableparser/',left(filedate,4),'/qtr',@qtr,'/tables/',accno,'_',tno,@xtn) link\n"
				+ ",yr,tno,left(rowname,15),value,col,tc,edt1,edt2,p1,p2,tsShort,tsLong,htmlTxt,mo,ended\n"
				+ ",columnPattern,allcolText,filedate,ts,accno,concat('https://www.sec.gov/Archives/edgar/data/',cik,'/',accno,'-index.htm')*/\n"
				+ " from "
				+ table
				+ " where col!=0 \n"
				+ "and p2!=3 and p2!=6 and p2!=9 and p2!=12 \n"
				+ "and ( (columnPattern rlike 'pCntD:(1|0)' and tsLong rlike 'pCntD:1')\n"
				+ "or (columnPattern rlike 'pCntD:1' and tsLong rlike 'pCntD:(1|0)') );\n"
				+ "\n"
				+ "ALTER TABLE TMP1"
				+ yr
				+ "qtr"
				+ q
				+ " ADD KEY (ACCNO,tno,ROW), ADD KEY(ACCNO),ADD KEY(tno),ADD KEY(ROW), CHANGE per3 per3 VARCHAR(10);\n"
				+ "\nset sql_mode = ALLOW_INVALID_DATES;\n\n"
				+ "UPDATE IGNORE "
				+ table
				+ " T1 \n"
				+ "INNER JOIN TMP1"
				+ yr
				+ "qtr"
				+ q
				+ " T2\n"
				+ "ON T1.ACCNO=T2.ACCNO AND T1.tno=T2.tno AND T1.ROW=T2.ROW\n"
				+ "set mo='p2' , p2=per3\n"
				+ "WHERE per3!='xx';\n"
				+ "/*this can work by grabbing p1*/"
				+ "\nDROP TABLE IF EXISTS TMP1_"
				+ yr
				+ "_"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP1_"
				+ yr
				+ "_"
				+ q
				+ " engine=myisam\n"
				+ "select t1.accno,t1.tno,t1.row,\n"
				+ "case\n"
				+ "\n"
				+ "when columnText like '%three % nine%' and (tc=4 or tc=2) and col=1 then 3\n"
				+ "when columnText rlike '(the|first|second|third).{1,3}Quarter.{1,25}( 6 | 9 | 12 ).{0,3}Mo'"
				+ " and (tc=4 or tc=2) and col=1 then 3\n"
				+ "when columnText rlike '(the|first|second|third).{1,3}Quarter.{1,25} 6 .{0,3}Mo'"
				+ " and (tc=4 or tc=2) and col=2 then 6\n"

				+ "when columnText like '%three % nine%' and tc=4 and col=2 then 3\n"
				+ "when columnText like '%three % nine%' and tc=2 and col=2 then 9\n"
				+ "when columnText like '%three % nine%' and tc=4 and (col=3 or col=4) then 9\n"
				+ "\n"
				+ "when columnText like '%nine % three%' and (tc=4 or tc=2) and col=1 then 9\n"
				+ "when columnText like '%nine % three%' and tc=4 and col=2 then 9\n"
				+ "when columnText like '%nine % three%' and tc=2 and col=2 then 3\n"
				+ "when columnText like '%nine % three%' and tc=4 and (col=3 or col=4) then 3\n"
				+ "\n"
				+ "when columnText like '%three % six%' and columntext not rlike 'week' and (tc=4 or tc=2) and col=1 then 3\n"
				+ "when columnText like '%three % six%' and columntext not rlike 'week' and tc=4 and col=2 then 3\n"
				+ "when columnText like '%three % six%' and columntext not rlike 'week' and tc=2 and col=2 then 6\n"
				+ "when columnText like '%three % six%' and columntext not rlike 'week' and tc=4 and (col=3 or col=4) then 6\n"

				+ "when columnText like '%twenty%six%thirteen%' and columntext rlike 'week' and (tc=4 or tc=2) and col=1 then 3\n"
				+ "when columnText like '%twenty%six%thirteen%' and columntext rlike 'week' and tc=4 and col=2 then 3\n"
				+ "when columnText like '%twenty%six%thirteen%' and columntext rlike 'week' and tc=2 and col=2 then 6\n"
				+ "when columnText like '%twenty%six%thirteen%' and columntext rlike 'week' and tc=4 and (col=3 or col=4) then 6\n"
				+ "\n"
				+ "when columnText like '%six % three%' and (tc=4 or tc=2) and col=1 then 6\n"
				+ "when columnText like '%six % three%' and tc=4 and col=2 then 6\n"
				+ "when columnText like '%six % three%' and tc=2 and col=2 then 3\n"
				+ "when columnText like '%six % three%' and tc=4 and (col=3 or col=4) then 3\n"
				+ "\n"
				+ "when columnText like '%three % twelve%' and (tc=4 or tc=2) and col=1 then 3\n"
				+ "when columnText like '%three % twelve%' and tc=4 and col=2 then 3\n"
				+ "when columnText like '%three % twelve%' and tc=2 and col=2 then 12\n"
				+ "when columnText like '%three % twelve%' and tc=4 and (col=3 or col=4) then 12\n"
				+ "\n"
				+ "when columnText like '%twelve% three%' and (tc=4 or tc=2) and col=1 then 12\n"
				+ "when columnText like '%twelve% three%' and tc=4 and col=2 then 12\n"
				+ "when columnText like '%twelve% three%' and tc=2 and col=2 then 3\n"
				+ "when columnText like '%twelve% three%' and tc=4 and (col=3 or col=4) then 3\n"
				+ "end p3\n"
				+ "/*,t1.col,t1.tc,t1.trow\n"
				+ ",left(t1.rowname,18),t1.value,t1.ended,t1.mo,t1.p1,t1.p2,t1.edt2\n"
				+ ",t1.columntext,t1.columnpattern,allcoltext,htmlTxt html,tsshort, concat('https://edgar.sec.gov/Archives/edgar/data/',t1.cik,'/',t1.accno,'-index.html') link*/\n"
				+ " from  "
				+ table
				+ "  t1 where \n"
				+ "    columnText like '%month%' and (tc=2 or tc=4)\n"
				+ "and columnText not like '%three%three' and columnText not like '%six%six'\n"
				+ "and columnText not like '%nine%nine' and columnText not like '%twelve%twelve' \n"
				+ "and (columnText like '%three % nine%' \n"
				+ " or columnText like '%nine % three%' \n"
				+ " or columnText like '%three % six%' \n"
				+ " or columnText like '%six % three%' \n"
				+ " or columnText like '%three % twelve%' \n"
				+ " or columnText like '%twelve% three%' \n"
				+ " or columnText like '%twelve% three%' );\n\n"
				+ "alter table TMP1_"
				+ yr
				+ "_"
				+ q
				+ " add key (accno), add key (tno),add key(row);\n"
				+ "\nset sql_mode = ALLOW_INVALID_DATES;\n\n"
				+ "UPDATE IGNORE  "
				+ table
				+ "  t1\n"
				+ "inner join TMP1_"
				+ yr
				+ "_"
				+ q
				+ " t2 on t1.accno=t2.accno and t1.tno=t2.tno and t1.row=t2.row\n"
				+ "set p2=p3, mo='p2';\n"

				+ "\nDROP TABLE IF EXISTS TMP_TSSHORT_"
				+ yr
				+ "_"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP_TSSHORT_"
				+ yr
				+ "_"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "select accno,tno,row,@y:=replace(edt2,'-','') y,\n"
				+ "case \n"
				+ "when tsShort='M' then concat(@y,'-',right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort = 'PM' then concat(@y,'-',right(left(tsLong,8),2),'-',right(left(tsLong,10),2))\n"
				+ "when tsShort = 'PPM' then concat(@y,'-',right(left(tsLong,13),2),'-',right(left(tsLong,15),2))\n"
				+ "when tsShort='PMY' and @y=left(edt1,4) and tc=1 then concat(left(edt1,4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2)) \n"
				+ "when tsShort='PMYPMY' and @y=left(edt1,4) and tc=2 and col=1 then concat(left(edt1,4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2)) \n"
				+ "when tsShort='PMYPMY' and @y=left(edt1,4) and tc=2 and col=2 then concat(left(edt1,4),'-', right(left(tsLong,23),2),'-',right(left(tsLong,25),2)) \n"
				+ "when tsShort='MYY' and @y=left(edt1,4) and tc=2 and col=1 then concat(left(edt1,4),'-', right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort='MYY' and @y=left(edt1,4) and tc=2 and col=2 then concat(left(edt1,4),'-', right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort='PMYY' and @y=left(edt1,4) and tc=2 and col=1 then concat(left(edt1,4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2))\n"
				+ "when tsShort='PMYY' and @y=left(edt1,4) and tc=2 and col=2 then concat(left(edt1,4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2))\n"
				+ "when tsShort='PMYYY' and @y=left(edt1,4) and tc=3 and col=1 then concat(left(edt1,4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2))\n"
				+ "when tsShort='PMYYY' and @y=left(edt1,4) and tc=3 and col=2 then concat(left(edt1,4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2))\n"
				+ "when tsShort='PMYYY' and @y=left(edt1,4) and tc=3 and col=3 then concat(left(edt1,4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2))\n"
				+ "when tsShort='MYYY' and @y=left(edt1,4) and tc=3 and col=1 then concat(left(edt1,4),'-', right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort='MYYY' and @y=left(edt1,4) and tc=3 and col=2 then concat(left(edt1,4),'-', right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort='MYYY' and @y=left(edt1,4) and tc=3 and col=3 then concat(left(edt1,4),'-', right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort='MYMY' and @y=left(edt1,4) and tc=2 and col=1 then concat(left(edt1,4),'-', right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort='MYMY' and @y=left(edt1,4) and tc=2 and col=2 then concat(left(edt1,4),'-', right(left(tsLong,13),2),'-',right(left(tsLong,15),2))\n"
				+ "when tsShort='PMYMY' and @y=left(edt1,4) and tc=2 and col=1 then concat(left(edt1,4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2))\n"
				+ "when tsShort='PMYMY' and @y=left(edt1,4) and tc=2 and col=2 then concat(left(edt1,4),'-', right(left(tsLong,18),2),'-',right(left(tsLong,20),2))\n"
				+ "when tsShort='PMYMYMY' and @y=left(edt1,4) and tc=3 and col=1 then concat(left(edt1,4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2))\n"
				+ "when tsShort='PMYMYMY' and @y=left(edt1,4) and tc=3 and col=2 then concat(left(edt1,4),'-', right(left(tsLong,18),2),'-',right(left(tsLong,20),2))\n"
				+ "when tsShort='PMYMYMY' and @y=left(edt1,4) and tc=3 and col=3 then concat(left(edt1,4),'-', right(left(tsLong,28),2),'-',right(left(tsLong,30),2))\n"
				+ "when tsShort='PPMY' and @y=left(edt1,4) and tc=2 then concat(left(edt1,4),'-', right(left(tsLong,13),2),'-',right(left(tsLong,16),2))\n"
				+ "when tsShort='MYYMY' and @y=left(edt1,4) and tc=3 and col=1 then concat(left(edt1,4),'-', right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort='MYYMY' and @y=left(edt1,4) and tc=3 and col=2 then concat(left(edt1,4),'-', right(left(tsLong,3),2),'-',right(left(tsLong,5),2))\n"
				+ "when tsShort='MYYMY' and @y=left(edt1,4) and tc=3 and col=3 then concat(left(edt1,4),'-', right(left(tsLong,18),2),'-',right(left(tsLong,20),2))\n"
				+ "else 'xx' end 'edt3' /*,tsshort,@y,col,edt2,edt1,TSLONG\n"
				+ "/*@xtn:= case when htmlTxt='html' then '.htm' else '.txt' end xtn\n"
				+ ", @qtr:= left(round(((month(filedate)-1)/3),2),1)+1 qtr,\n"
				+ "concat('file:///c:/backtest/tableparser/',left(filedate,4),'/qtr',@qtr,'/tables/',accno,'_',tno,@xtn) link,\n"
				+ ",tno,left(rowname,15),value,col,tc,edt1,edt2,p1,p2,tsShort,tsLong,htmlTxt,mo,yr,ended,columnPattern,allcolText,filedate,ts*/\n"
				+ "from " + table + " \n" + "where length(edt2)!=10 \n"
				+ "and tsShort!='-1' and trim(tsShort)!=''\n"
				+ "and edt2 rlike '[12]{1}[09]{1}[0-9]{2}'\n"
				+ "and edt1 rlike '[12]{1}[09]{1}[0-9]{2}'\n"
				+ "and trim(length(replace(edt2,'-','')))<10\n"
				+ "order by accno,tno,row;\n" + "ALTER TABLE TMP_TSSHORT_" + yr
				+ "_" + q + " ADD KEY(ACCNO), ADD KEY(TNO), ADD KEY(ROW);\n"
				+ "\n" + "UPDATE IGNORE " + table
				+ " t1 inner join TMP_TSSHORT_" + yr + "_" + q + " t2\n"
				+ " ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.ROW=T2.ROW\n"
				+ "set edt2=edt3, ended='EDT2'\n" + "where edt3!='xx';\n\n");

		String dropProc = "DROP PROCEDURE IF EXISTS updateEdt2p2" + yr + "QTR"
				+ q + ";\n" + "CREATE PROCEDURE updateEdt2p2" + yr + "QTR" + q
				+ "()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
		sb.delete(0, sb.toString().length());
		MysqlConnUtils.executeQuery("call updateEdt2p2" + yr + "QTR" + q
				+ "();\n");

		sb.delete(0, sb.toString().length());

		String qry = "UPDATE IGNORE "
				+ table
				+ " SET EDT2=CASE \n"
				+ "WHEN tsshort = 'MYY' THEN CONCAT(LEFT(edt2,5),RIGHT(LEFT(tslong,3),2),'-',RIGHT(LEFT(tslong,5),2))\n"
				+ "WHEN tsshort = 'PPMYY' THEN CONCAT(LEFT(edt2,5),RIGHT(LEFT(tslong,13),2),'-',RIGHT(LEFT(tslong,13),2))\n"
				+ "WHEN columntext rlike '([12]{1}[3-9]{1}|3[12]{1})/(0[1-9]{1}|1[12]{1}) [12]{1}[09]{1}[0-9]{2}' then \n"
				+ "concat(left(edt2,5),right(edt2,2),'-',right(left(edt2,7),2))\n"
				+ "end,\n"
				+ " ended=CASE \n"
				+ "WHEN tsshort = 'MYY' THEN 'EDT2'\n"
				+ "WHEN tsshort = 'PPMYY' THEN 'EDT2'\n"
				+ "WHEN columntext rlike '([12]{1}[3-9]{1}|3[12]{1})/(0[1-9]{1}|1[12]{1}) [12]{1}[09]{1}[0-9]{2}' then 'EDT2'\n"
				+ "end\n"
				+ "where right(left(edt2,7),2)>12 AND LENGTH(EDT2)>9;\n";

		dropProc = "DROP PROCEDURE IF EXISTS updateEdt2p2_2" + yr + "QTR" + q
				+ ";\n" + "CREATE PROCEDURE updateEdt2p2_2" + yr + "QTR" + q
				+ "()\n\n begin\n\n";
		endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + qry + endProc);
		sb.delete(0, sb.toString().length());
		MysqlConnUtils.executeQuery("call updateEdt2p2_2" + yr + "QTR" + q
				+ "();\n");

		qry = null;

	}

	public void updatePeriodFromColumnPattern(String table) throws SQLException, FileNotFoundException {

		String yr = table.substring(table.indexOf("tp_raw") + 6,
				table.indexOf("tp_raw") + 10);
		String q = table.substring(table.indexOf("tp_raw") + 13,
				table.indexOf("tp_raw") + 14);

		StringBuffer sb = new StringBuffer();

		sb.append("DROP TABLE IF EXISTS TMP_UPD_EDT_"
				+ yr
				+ "qtr"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP_UPD_EDT_"
				+ yr
				+ "qtr"
				+ q
				+ "\n"
				+ "SELECT EDT2,COLUMNTEXT,ACCNO,TNO,COL FROM BAC_TP_RAW"
				+ yr
				+ "qtr"
				+ q
				+ " GROUP BY ACCNO,TNO,COL;\n"
				+ "ALTER TABLE TMP_UPD_EDT_"
				+ yr
				+ "qtr"
				+ q
				+ " ADD KEY(ACCNO),ADD KEY(TNO),ADD KEY(COL);\n"
				+ "\n"
				+ "\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "update ignore bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " t1 inner join TMP_UPD_EDT_"
				+ yr
				+ "qtr"
				+ q
				+ " t2 on t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col\n"
				+ "/*select */\n"
				+ "set t1.edt2=\n"
				+ "case \n"
				+ " when month(t1.edt2)!=1 and t1.columntext rlike 'jan' THEN CONCAT(LEFT(t1.edt2,5),'01-',RIGHT(t1.edt2,2))\n"
				+ " when month(t1.edt2)!=2 and t1.columntext rlike 'feb' THEN CONCAT(LEFT(t1.edt2,5),'02-',RIGHT(t1.edt2,2))\n"
				+ " when month(t1.edt2)!=3 and t1.columntext rlike 'mar' THEN CONCAT(LEFT(t1.edt2,5),'03-',RIGHT(t1.edt2,2))\n"
				+ " when month(t1.edt2)!=4 and t1.columntext rlike 'apr' THEN CONCAT(LEFT(t1.edt2,5),'04-',RIGHT(t1.edt2,2))\n"
				+ " when month(t1.edt2)!=5 and t1.columntext rlike 'may ' THEN CONCAT(LEFT(t1.edt2,5),'05-',RIGHT(t1.edt2,2))\n"
				+ " when month(t1.edt2)!=6 and t1.columntext rlike 'jun' THEN CONCAT(LEFT(t1.edt2,5),'06-',RIGHT(t1.edt2,2))\n"
				+ " when month(t1.edt2)!=7 and t1.columntext rlike 'jul' THEN CONCAT(LEFT(t1.edt2,5),'07-',RIGHT(t1.edt2,2))\n"
				+ " when month(t1.edt2)!=8 and t1.columntext rlike 'aug' THEN CONCAT(LEFT(t1.edt2,5),'08-',RIGHT(t1.edt2,2))\n"
				+ " when month(t1.edt2)!=9 and t1.columntext rlike 'sep' THEN CONCAT(LEFT(t1.edt2,5),'09-',RIGHT(t1.edt2,2))\n"
				+ " when month(t1.edt2)!=10 and t1.columntext rlike 'oct' THEN CONCAT(LEFT(t1.edt2,5),'10-',RIGHT(t1.edt2,2))\n"
				+ " when month(t1.edt2)!=11 and t1.columntext rlike 'nov' THEN CONCAT(LEFT(t1.edt2,5),'11-',RIGHT(t1.edt2,2))\n"
				+ " when month(t1.edt2)!=12 and t1.columntext rlike 'dec' THEN CONCAT(LEFT(t1.edt2,5),'12-',RIGHT(t1.edt2,2))\n"
				+ "ELSE t1.edt2 end,ended='EDT2' \n"
				+ "\n"
				+ "/*from bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " t1 inner join TMP_UPD_EDT_"
				+ yr
				+ "qtr"
				+ q
				+ " t2 on t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col*/\n"
				+ " where \n"
				+ " t2.columntext not rlike '(jan|feb|march|apr|may |jun|jul|aug|sep|oct|nov|dec).*(jan|feb|march|apr|may |jun|jul|aug|sep|oct|nov|dec)|decrea|predeces'\n"
				+ " and (\n"
				+ " month(t2.edt2)!=1 and t2.columntext rlike 'jan' or\n"
				+ " month(t2.edt2)!=2 and t2.columntext rlike 'feb' or\n"
				+ " month(t2.edt2)!=3 and t2.columntext rlike 'mar' or\n"
				+ " month(t2.edt2)!=4 and t2.columntext rlike 'apr' or\n"
				+ " month(t2.edt2)!=5 and t2.columntext rlike 'may ' or\n"
				+ " month(t2.edt2)!=6 and t2.columntext rlike 'jun' or\n"
				+ " month(t2.edt2)!=7 and t2.columntext rlike 'jul' or\n"
				+ " month(t2.edt2)!=8 and t2.columntext rlike 'aug' or\n"
				+ " month(t2.edt2)!=9 and t2.columntext rlike 'sep' or\n"
				+ " month(t2.edt2)!=10 and t2.columntext rlike 'oct' or\n"
				+ " month(t2.edt2)!=11 and t2.columntext rlike 'nov' or\n"
				+ " month(t2.edt2)!=12 and t2.columntext rlike 'dec' \n"
				+ " ) ;\n");

		sb.append("\nupdate ignore bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " t1\n"
				+ "inner join TMP_UPD_EDT_"
				+ yr
				+ "qtr"
				+ q
				+ " t2 on t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col\n"
				+ "set t1.edt2=case \n"
				+ "when t1.columnText rlike 'jan' then concat(left(t1.edt2,4),'-01')\n"
				+ "when t1.columnText rlike 'feb' then concat(left(t1.edt2,4),'-02')\n"
				+ "when t1.columnText rlike 'mar' then concat(left(t1.edt2,4),'-03')\n"
				+ "when t1.columntext rlike 'apr' then concat(left(t1.edt2,4),'-04')\n"
				+ "when t1.columntext rlike 'may ' then concat(left(t1.edt2,4),'-05')\n"
				+ "when t1.columntext rlike 'jun' then concat(left(t1.edt2,4),'-06')\n"
				+ "when t1.columntext rlike 'jul' then concat(left(t1.edt2,4),'-07')\n"
				+ "when t1.columntext rlike 'aug' then concat(left(t1.edt2,4),'-08')\n"
				+ "when t1.columntext rlike 'sep' then concat(left(t1.edt2,4),'-09')\n"
				+ "when t1.columntext rlike 'oct' then concat(left(t1.edt2,4),'-10')\n"
				+ "when t1.columntext rlike 'nov' then concat(left(t1.edt2,4),'-11')\n"
				+ "when t1.columntext rlike 'dec' then concat(left(t1.edt2,4),'-12')\n"
				+ "else t1.edt2 end /*edt3,t1.* from bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ "*/ \n"
				+ "\n"
				+ "where length(replace(t2.edt2,'-',''))=4 and replace(t2.edt2,'-','') between 1990 and 2050\n"
				+ "and t2.columntext rlike 'jan|feb|mar|apr|may |jun|jul|aug|sep|oct|nov|dec' \n"
				+ "and \n"
				+ "(\n"
				+ "(t2.columntext regexp 'jan') + (t2.columntext regexp 'feb')+ (t2.columntext regexp 'mar')+  \n"
				+ "(t2.columntext regexp 'apr') + (t2.columntext regexp 'may ')+ (t2.columntext regexp 'jun')+  \n"
				+ "(t2.columntext regexp 'jul') + (t2.columntext regexp 'aug')+ (t2.columntext regexp 'sep')+  \n"
				+ "(t2.columntext regexp 'oct') + (t2.columntext regexp 'nov')+ (t2.columntext regexp 'dec')  \n"
				+ ")=1  ;\n");

		String dropProc = "DROP PROCEDURE IF EXISTS updatePeriodFromColumnPattern"
				+ yr
				+ "QTR"
				+ q
				+ ";\n"
				+ "CREATE PROCEDURE updatePeriodFromColumnPattern"
				+ yr
				+ "QTR"
				+ q + "()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
		sb.delete(0, sb.toString().length());
		MysqlConnUtils.executeQuery("call updatePeriodFromColumnPattern" + yr
				+ "QTR" + q + "();\n");

		String qry = "DROP TABLE IF EXISTS TMP_UPD_p2_"
				+ yr
				+ "qtr"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP_UPD_p2_"
				+ yr
				+ "qtr"
				+ q
				+ "\n"
				+ "SELECT P2 p_2,columnPattern CP,TC,ACCNO,TNO,COL FROM BAC_TP_RAW"
				+ yr
				+ "qtr"
				+ q
				+ " GROUP BY ACCNO,TNO,COL;\n"
				+ "ALTER TABLE TMP_UPD_p2_"
				+ yr
				+ "qtr"
				+ q
				+ " ADD KEY(ACCNO),ADD KEY(TNO),ADD KEY(COL), ADD KEY(P_2);\n"
				+ "\n"
				+ "\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "UPDATE IGNORE bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " t1 inner join TMP_UPD_p2_"
				+ yr
				+ "qtr"
				+ q
				+ " t2 on t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col\n"
				+ "/*select p_2,t1.p2,t2.CP,t1.p1, ct,*/\n"
				+ "set p2=\n"
				+ "case when (columnPattern rlike '(twelve|thirteen|fourteen|(: )(12|13|14))[ -]{1,3}w|(:| )(3|three)[ -]{1,3}mo' \n"
				+ "or (columnPattern rlike 'quarter|qtr' and columnPattern not rlike 'three')\n"
				+ "or (columnPattern rlike 'quarter|qtr' and columnPattern not rlike 'three'))\n"
				+ "and columnPattern not rlike 'one |two |four |five |seven |eight |(:| )ten|twent|thirt|fort|fift|eleven|years|six|nine|twelve'\n"
				+ " then 3 \n"
				+ "\n when columnText rlike 'sixteen w' then 0\n"
				+ "when columnPattern rlike '((twenty.{1}(five|six |seven))|25|26|27)[ -]{1,3}w|(:| )(6|six)[ -]{1,3}|(:2 |:two | two )(fiscal )?quarter|qtr' \n"
				+ "and columnPattern not rlike ':seven|one |two |four |eight |nine |(:| )ten |thirt|fort|fift|eleven|year|three|nine|twelve|teen '\n"
				+ " then 6\n"
				+ "when columnPattern rlike '((thirty.{1}(eight|nine))|38|39|40)[ -]{1,3}w|(:| )(9|nine)[ -]{1,3}mo|(:3 |:three | three )(fiscal )?quarter|qtr' \n"
				+ "and columnPattern not rlike 'one|two|four|five|seven|(:| )ten|eleven|twent|fift|year|three|six|twelve|teen '\n"
				+ " then 9\n"
				+ "when columnPattern rlike\n"
				+ "'((fifty.{1}(one|two|three))|51|52|53)[ -]{1,3}w|(:| )(12|twelve)[ -]{1,3}mo|(:4 |:four | four )(fiscal )?quarter|qtr|years? end' \n"
				+ "and columnPattern not rlike 'one |two |four |five |six |seven |eight | nine| eleven|ten|eleven|thirt|twent|three|six|nine|teen '\n"
				+ " then 12\n"
				+ "else p2 end,mo='p2'\n"
				+ "/*from bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " t1 inner join TMP_UPD_p2_"
				+ yr
				+ "qtr"
				+ q
				+ " t2 on t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col*/\n"
				+ "where\n"
				+ "(t2.p_2=0 \n"
				+ "and t2.CP not rlike 'days'\n"
				+ "and t2.CP rlike 'pCntD:1' and t2.CP rlike 'pCnt:1'\n"
				+ "and ( (t2.CP rlike 'yCnt:1' and t2.tc=1 ) or (t2.CP rlike 'yCntD:2' and t2.tc=2) ))\n"
				+ "and\n"
				+ "(\n"
				+ "(\n"
				+ "(cp rlike '(twelve|thirteen|fourteen|(: )(12|13|14))[ -]{1,3}w|(:| )(3|three)[ -]{1,3}mo' \n"
				+ "or (cp rlike 'quarter|qtr' and cp not rlike 'three')\n"
				+ "or (cp rlike 'quarter|qtr' and cp not rlike 'three'))\n"
				+ "and cp not rlike 'one |two |four |five |seven |eight |(:| )ten|twent|thirt|fort|fift|eleven|years|six|nine|twelve')\n"
				+ "\n"
				+ "or\n"
				+ "\n"
				+ "(\n"
				+ "cp rlike '((twenty.{1}(five|six |seven))|25|26|27)[ -]{1,3}w|(:| )(6|six)[ -]{1,3}|(:2 |:two | two )(fiscal )?quarter|qtr' \n"
				+ "and cp not rlike ':seven|one |two |four |eight |nine |(:| )ten |thirt|fort|fift|eleven|year|three|nine|twelve|teen '\n"
				+ ")\n"
				+ "\n"
				+ "or\n"
				+ "\n"
				+ "(\n"
				+ "cp rlike '((thirty.{1}(eight|nine))|38|39|40)[ -]{1,3}w|(:| )(9|nine)[ -]{1,3}mo|(:3 |:three | three )(fiscal )?quarter|qtr' \n"
				+ "and cp not rlike 'one|two|four|five|seven|(:| )ten|eleven|twent|fift|year|three|six|twelve|teen '\n"
				+ ")\n"
				+ "\n"
				+ "or\n"
				+ "\n"
				+ "( \n"
				+ "cp rlike\n"
				+ "'((fifty.{1}(one|two|three))|51|52|53)[ -]{1,3}w|(:| )(12|twelve)[ -]{1,3}mo|(:4 |:four | four )(fiscal )?quarter|qtr|years? end' \n"
				+ "and cp not rlike 'one |two |four |five |six |seven |eight | nine| eleven|ten|eleven|thirt|twent|three|six|nine|teen '\n"
				+ ")\n"
				+ ")\n"
				+ ";\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS tmpA_getEdt_oneMotwoYr_"
				+ yr
				+ "qtr"
				+ q
				+ ";\n"
				+ "create table tmpA_getEdt_oneMotwoYr_"
				+ yr
				+ "qtr"
				+ q
				+ " engine=myisam\n"
				+ "SELECT ACCNO,TNO,COL,COLUMNPATTERN CP,allcoltext,EDT2 E2,TC TC2 FROM bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " where tc=2 group by accno,tno,col;\n"
				+ "ALTER TABLE tmpA_getEdt_oneMotwoYr_"
				+ yr
				+ "qtr"
				+ q
				+ " ADD KEY(ACCNO),ADD KEY(TNO), ADD KEY(COL);\n"
				+ "\n"
				+ " set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "drop table if exists tmp_getEdt_oneMotwoYr_"
				+ yr
				+ "qtr"
				+ q
				+ ";\n"
				+ "create table tmp_getEdt_oneMotwoYr_"
				+ yr
				+ "qtr"
				+ q
				+ " engine=myisam\n"
				+ "select T1.accno,T1.tno,T1.COL,@yr:=case \n"
				+ "when T1.col=1 then substring_index(substring_index(t1.cp,'1Y:',-1),'|',1) \n"
				+ "when T1.col=t1.tc2 then substring_index(substring_index(t1.cp,'2Y:',-1),'|',1) end yr\n"
				+ ",@mo:=substring_index(substring_index(t1.cp,'1M:',-1),'|',1) mo \n"
				+ ",@mo2:=trim((substring_index(@mo,' ',1))) mo2\n"
				+ ",case when length(date_format(concat(@yr,'-',month(str_to_date(@mo2,'%M')),'-',right(@mo,2)),'%Y-%m-%d'))=10 then \n"
				+ "date_format(concat(@yr,'-',month(str_to_date(@mo2,'%M')),'-',right(@mo,2)),'%Y-%m-%d') else 'xx' end edt3\n"
				+ "/*,concat('file:///c:/backtest/tableParser/',year(filedate),'/qtr',quarter(filedate),'/tables/',accno,'_',tno,'.txt') link*/\n"
				+ "from tmpA_getEdt_oneMotwoYr_"
				+ yr
				+ "qtr"
				+ q
				+ " T1 INNER JOIN tmpA_getEdt_oneMotwoYr_"
				+ yr
				+ "qtr"
				+ q
				+ " T2 ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.COL=T2.COL\n"
				+ "where t2.tc2=2 and t2.CP rlike 'mCntD:1' and t2.CP  rlike 'yCntD:2'\n"
				+ "and t2.e2 not rlike '[12]{1}[09]{1}[0-9]{1}[0-9]{1}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})'\n"
				+ "and t2.CP not rlike '/' and (\n"
				+ "(t1.allcoltext regexp 'jan') + (t1.allcoltext regexp 'feb')+ (t1.allcoltext regexp 'mar')+\n"
				+ "(t1.allcoltext regexp 'apr') + (t1.allcoltext regexp 'may ')+ (t1.allcoltext regexp 'jun')+\n"
				+ "(t1.allcoltext regexp 'jul') + (t1.allcoltext regexp 'aug')+ (t1.allcoltext regexp 'sep')+\n"
				+ "(t1.allcoltext regexp 'oct') + (t1.allcoltext regexp 'nov')+ (t1.allcoltext regexp 'dec')\n"
				+ ")=1\n"
				+ ";\n"

				+ "alter table tmp_getEdt_oneMotwoYr_"
				+ yr
				+ "qtr"
				+ q
				+ " add key (accno),add key(tno),add key(COL), ADD KEY(EDT3);\n"
				+ "\n" + "\n" + "\n" + "UPDATE IGNORE bac_tp_raw" + yr + "qtr"
				+ q + " t1 inner join tmp_getEdt_oneMotwoYr_" + yr + "qtr" + q
				+ " t2 \n"
				+ "on t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col \n"
				+ "set t1.edt2=t2.edt3, ended='edt2'\n" + "where edt3!='xx';\n"
				+ "\n";

		dropProc = "DROP PROCEDURE IF EXISTS updatePeriodFromColumnPattern_2"
				+ yr + "QTR" + q + ";\n"
				+ "CREATE PROCEDURE updatePeriodFromColumnPattern_2" + yr
				+ "QTR" + q + "()\n\n begin\n\n";
		endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + qry + endProc);
		sb.delete(0, sb.toString().length());
		MysqlConnUtils.executeQuery("call updatePeriodFromColumnPattern_2" + yr
				+ "QTR" + q + "();\n");

		qry = "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "SET @lEdt = '1901-01-01';\n" + "SET @fEdt = '1901-01-01';\n"
				+ "\n" + "DROP TABLE IF EXISTS TMP_TO_FROM_"
				+ yr
				+ "QTR"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP_TO_FROM_"
				+ yr
				+ "QTR"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "SELECT ACCNO,TNO,COL, COLUMNTEXT,EDT2,P2\n"
				+ ",@l:=length(substring_index(columnText,'/',-4)) l\n"
				+ ",@str:=replace(replace(replace(replace(replace(replace(replace(trim(right(columnText,@l+3)),' THROUGH ',' ')"
				+ ",'(',''),')',''),' from ',' '),' to ',' '),'-',''),'  ',' ') str2\n"
				+ ",@lEdt:=SUBSTRING_INDEX(@str, ' ', -1) lEdt\n"
				+ ",@fEdt:=SUBSTRING_INDEX(@str, ' ', 1) fEdt\n"
				+ ",@lEdt2:=CASE \n"
				+ "WHEN RIGHT(@lEdt,2) BETWEEN 80 AND 99 THEN CONCAT('19',RIGHT(@lEdt,2),'-',SUBSTRING_INDEX(@lEdt, '/', 1),'-',substring_index(\n"
				+ "SUBSTRING_INDEX(@lEdt, '/', -2),'/',1))\n"
				+ "WHEN RIGHT(@lEdt,2) BETWEEN 0 and 10  THEN CONCAT('20',RIGHT(@lEdt,2),'-',SUBSTRING_INDEX(@lEdt, '/', 1),'-',substring_index(\n"
				+ "SUBSTRING_INDEX(@lEdt, '/', -2),'/',1)) END lEdtConf\n"
				+ ",@fEdt2:=CASE \n"
				+ "WHEN RIGHT(@fEdt,2) BETWEEN 80 AND 99 THEN CONCAT('19',RIGHT(@fEdt,2),'-',SUBSTRING_INDEX(@fEdt, '/', 1),'-',substring_index(\n"
				+ "SUBSTRING_INDEX(@fEdt, '/', -2),'/',1))\n"
				+ "WHEN RIGHT(@lEdt,2) BETWEEN 0 and 10  THEN CONCAT('20',RIGHT(@fEdt,2),'-',SUBSTRING_INDEX(@fEdt, '/', 1),'-',substring_index(\n"
				+ "SUBSTRING_INDEX(@fEdt, '/', -2),'/',1)) END fEdtConf\n"
				+ ", @edt3:=CASE WHEN DATEDIFF(@lEdt2,EDT2)>0 THEN date_format(@lEdt2,'%Y-%m-%d') ELSE 'xx' END EDT3 \n"
				+ ",@p3:=round((datediff(@lEdt2,@fEdt2)/30),1) tmpP3\n"
				+ ",case \n"
				+ "when @p3 between .91*12 and 1.08*12 and p2!=12 then 12 \n"
				+ "when @p3 between .91*9 and 1.08*9   and p2!=9 then 9 \n"
				+ "when @p3 between .91*6 and 1.08*6   and p2!=6 then 6 \n"
				+ "when @p3 between .91*3 and 1.08*3   and p2!=3 then 3\n"
				+ "when p2!=0 then 0 \n"
				+ "else 'xx' end p3\n"
				+ "/*,rowname,value*/\n"
				+ "\n"
				+ "FROM BAC_TP_RAW"
				+ yr
				+ "QTR"
				+ q
				+ " WHERE \n"
				+ "(\n"
				+ "COLUMNTEXT RLIKE '(0?[1-9]{1}|1[12]{1})-[0-9]{1,2}-[0-9]{2,4}.{1,30}(0?[1-9]{1}|1[12]{1})-[0-9]{1,2}-[0-9]{2,4}'\n"
				+ "or\n"
				+ "COLUMNTEXT RLIKE '(0?[1-9]{1}|1[12]{1})/[0-9]{1,2}/[0-9]{2,4}.{1,30}(0?[1-9]{1}|1[12]{1})/[0-9]{1,2}/[0-9]{2,4}'\n"
				+ ") and col>0 \n"
				+ "GROUP BY accno,tno,col;\n"
				+ "ALTER TABLE TMP_TO_FROM_"
				+ yr
				+ "QTR"
				+ q
				+ " ADD KEY(ACCNO),ADD KEY(TNO),ADD KEY(COL);\n"
				+ "\n"
				+ "\n"
				+ "UPDATE IGNORE bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " t1 inner join TMP_TO_FROM_"
				+ yr
				+ "QTR"
				+ q
				+ " t2 on t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col\n"
				+ "set t1.edt2=edt3\n"
				+ "where edt3!='xx';\n"
				+ "\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "UPDATE IGNORE bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " t1 inner join TMP_TO_FROM_"
				+ yr
				+ "QTR"
				+ q
				+ " t2 on t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col\n"
				+ "set t1.p2=p3\n"
				+ "where p3!='xx';\n\n\n"
				+ "UPDATE IGNORE bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ "\n"
				+ "set edt2=concat(right(left(tslong,15),4),'-', right(left(tsLong,8),2),'-',right(left(tsLong,10),2)), ended='EDT2'\n"
				+ "/*select * from bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ "*/\n"
				+ "where tsshort rlike '^pmy'\n"
				+ "and\n"
				+ "length(edt2)!=10 \n"
				+ "and tsShort!='-1' and trim(tsShort)!=''\n"
				+ "and edt2 rlike '[1,2]{1}[0,9]{1}[0-9]{2}'\n"
				+ "and edt1 rlike '[1,2]{1}[0,9]{1}[0-9]{2}'\n"
				+ "and trim(length(replace(edt2,'-','')))=4\n"
				+ "and left(edt2,4)=right(left(tslong,15),4) and col=1;\n"
				+ "\n"

				+ "update bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ "\n"
				+ "set edt2=edt1\n"
				+ "where \n"
				+ "/*select t1.*,\n"
				+ "concat('file:///c://backtest/tableparser/',year(filedate),'/qtr',quarter(filedate),'/tables/',accno,'_',tno,'.txt') link\n"
				+ "from ... t1*/\n"
				+ " year(edt2)!=year(edt1) and year(edt1)=yr\n"
				+ "and columntext rlike year(edt1);\n" + "\n";

		dropProc = "DROP PROCEDURE IF EXISTS updatePeriodFromColumnPattern_6"
				+ yr + "QTR" + q + ";\n"
				+ "CREATE PROCEDURE updatePeriodFromColumnPattern_6" + yr
				+ "QTR" + q + "()\n\n begin\n\n";
		endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + qry + endProc);
		MysqlConnUtils.executeQuery("call updatePeriodFromColumnPattern_6" + yr
				+ "QTR" + q + "();\n");

	}

	public void updateEdt2(String table) throws SQLException, FileNotFoundException {
		String yr = table.substring(table.indexOf("tp_raw") + 6,
				table.indexOf("tp_raw") + 10);
		String q = table.substring(table.indexOf("tp_raw") + 13,
				table.indexOf("tp_raw") + 14);

		/*
		 * This finds instances where edt2 has no month and/or day value. But
		 * has year value. If there is only 1 distinct monthin allColText or
		 * columnPattern (and neither has a distinct month count>1) then I can
		 * supplement edt2 w/ month day found in allColText or columnPattern.
		 */

		StringBuffer sb = new StringBuffer(
				"DROP TABLE IF EXISTS TMP_MOPATTERN_"
						+ yr
						+ "_QTR"
						+ q
						+ ";\n"
						+ "CREATE TABLE TMP_MOPATTERN_"
						+ yr
						+ "_QTR"
						+ q
						+ " ENGINE=MYISAM\n"
						+ "SELECT accno,tno,row,edt2,columnPattern\n"
						+ ",@cp_mCnt:=0 /*seems to help to reset b/c it was erroneously showing cnts of 2 notwithstanding where clause filters those out*/\n"
						+ ",@cp_mCnt:=((columnPattern REGEXP 'jan') + (columnPattern REGEXP 'feb')\n"
						+ "+ (columnPattern REGEXP 'mar')+ (columnPattern REGEXP 'apr')\n"
						+ "+ (columnPattern REGEXP 'may')+ (columnPattern REGEXP 'jun')\n"
						+ "+ (columnPattern REGEXP 'jul')+ (columnPattern REGEXP 'aug')\n"
						+ "+ (columnPattern REGEXP 'sep')+ (columnPattern REGEXP 'oct')\n"
						+ "+ (columnPattern REGEXP 'nov')+ (columnPattern REGEXP 'dec')\n"
						+ ") cp_mCnt\n"
						+ ",@moDay:=substring_index(SUBSTRING(columnpattern,LOCATE('1M:',columnpattern)+3),'|',1) moDay\n"
						+ "\n"
						+ "/*find which month is present and if there's a day value - then append both to edt2 year.\n"
						+ "if just month value and edt2 is just year append just month*/,case \n"
						+ "when @cp_mCnt=1 and @moDay like '%jan%' and right(trim(@moDay),2) rlike '[0-9]{2}' \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}jan')\n"
						+ "then concat(left(edt2,4),'-','01','-',right(trim(@moDay),2))\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%jan%' and right(trim(@moDay),2) not rlike '[0-9]{2}' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}jan')\n"
						+ "then concat(left(edt2,4),'-','01','-')\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%feb%' and right(trim(@moDay),2) rlike '[0-9]{2}' \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}feb')\n"
						+ "then concat(left(edt2,4),'-','02','-',right(trim(@moDay),2))\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%feb%' and right(trim(@moDay),2) not rlike '[0-9]{2}' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}feb')\n"
						+ "then concat(left(edt2,4),'-','02','-')\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%mar%' and right(trim(@moDay),2) rlike '[0-9]{2}' \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}mar')\n"
						+ "then concat(left(edt2,4),'-','03','-',right(trim(@moDay),2))\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%mar%' and right(trim(@moDay),2) not rlike '[0-9]{2}' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}mar')\n"
						+ "then concat(left(edt2,4),'-','03','-')\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%apr%' and right(trim(@moDay),2) rlike '[0-9]{2}' \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}apr')\n"
						+ "then concat(left(edt2,4),'-','04','-',right(trim(@moDay),2))\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%apr%' and right(trim(@moDay),2) not rlike '[0-9]{2}' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}apr')\n"
						+ "then concat(left(edt2,4),'-','04','-')\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%may %' and right(trim(@moDay),2) rlike '[0-9]{2}' \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}may ')\n"
						+ "then concat(left(edt2,4),'-','05','-',right(trim(@moDay),2))\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%may %' and right(trim(@moDay),2) not rlike '[0-9]{2}' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}may ')\n"
						+ "then concat(left(edt2,4),'-','05','-')\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%jun%' and right(trim(@moDay),2) rlike '[0-9]{2}' \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}jun')\n"
						+ "then concat(left(edt2,4),'-','06','-',right(trim(@moDay),2))\n"
						+ "\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%jun%' and right(trim(@moDay),2) not rlike '[0-9]{2}' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}jun')\n"
						+ "then concat(left(edt2,4),'-','06','-')\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%jul%' and right(trim(@moDay),2) rlike '[0-9]{2}'\n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}jul')\n"
						+ "then concat(left(edt2,4),'-','07','-',right(trim(@moDay),2))\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%jul%' and right(trim(@moDay),2) not rlike '[0-9]{2}' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}jul')\n"
						+ "then concat(left(edt2,4),'-','07','-')\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%aug%' and right(trim(@moDay),2) rlike '[0-9]{2}' \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}aug')\n"
						+ "then concat(left(edt2,4),'-','08','-',right(trim(@moDay),2))\n"
						+ "when @cp_mCnt=1 and @moDay like '%aug%' and right(trim(@moDay),2) not rlike '[0-9]{2}' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}aug')\n"
						+ "then concat(left(edt2,4),'-','08','-')\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%sep%' and right(trim(@moDay),2) rlike '[0-9]{2}' \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}sep')\n"
						+ "then concat(left(edt2,4),'-','09','-',right(trim(@moDay),2))\n"
						+ "when @cp_mCnt=1 and @moDay like '%sep%' and right(trim(@moDay),2) not rlike '[0-9]{2}' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}sep')\n"
						+ "then concat(left(edt2,4),'-','09','-')\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%oct%' and right(trim(@moDay),2) rlike '[0-9]{2}' \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}oct')\n"
						+ "then concat(left(edt2,4),'-','10','-',right(trim(@moDay),2))\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%oct%' and right(trim(@moDay),2) not rlike '[0-9]{2}' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}oct')\n"
						+ "then concat(left(edt2,4),'-','19','-')\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%nov%' and right(trim(@moDay),2) rlike '[0-9]{2}'\n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}nov')\n"
						+ "then concat(left(edt2,4),'-','11','-',right(trim(@moDay),2))\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%nov%' and right(trim(@moDay),2) not rlike '[0-9]{2}' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}nov')\n"
						+ "then concat(left(edt2,4),'-','11','-')\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%dec%' and right(trim(@moDay),2) rlike '[0-9]{2}' \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}dec')\n"
						+ "then concat(left(edt2,4),'-','12','-',right(trim(@moDay),2))\n"
						+ "\n"
						+ "when @cp_mCnt=1 and @moDay like '%dec%' and right(trim(@moDay),2) not rlike '[0-9]{2}' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}dec')\n"
						+ "then concat(left(edt2,4),'-','12','-')\n"
						+ "\n"
						+ "WHEN allcoltext like '%jan%' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}jan')\n"
						+ "then concat(left(edt2,4),'-','01','-')\n"
						+ "\n"
						+ "WHEN allcoltext like '%feb%' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}feb')\n"
						+ "then concat(left(edt2,4),'-','02','-')\n"
						+ "\n"
						+ "WHEN (allcoltext like '%marc%' or allcoltext like'%mar.%' or allcoltext like '%mar %') and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}mar')\n"
						+ "then concat(left(edt2,4),'-','03','-')\n"
						+ "\n"
						+ "WHEN allcoltext like '%apr%' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}apr')\n"
						+ "then concat(left(edt2,4),'-','04','-')\n"
						+ "\n"
						+ "WHEN allcoltext like '%may ' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}may ')\n"
						+ "then concat(left(edt2,4),'-','05','-')\n"
						+ "\n"
						+ "WHEN (allcoltext like '%june%' or allcoltext like'%jun.%' or allcoltext like '%jun %') and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}jun')\n"
						+ "then concat(left(edt2,4),'-','06','-')\n"
						+ "\n"
						+ "WHEN allcoltext like '%jul%' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}jul')\n"
						+ "then concat(left(edt2,4),'-','07','-')\n"
						+ "\n"
						+ "WHEN allcoltext like '%aug%' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}aug')\n"
						+ "then concat(left(edt2,4),'-','08','-')\n"
						+ "\n"
						+ "WHEN allcoltext like '%sep%' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}sep')\n"
						+ "then concat(left(edt2,4),'-','09','-')\n"
						+ "\n"
						+ "WHEN allcoltext like '%oct%' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}oct')\n"
						+ "then concat(left(edt2,4),'-','10','-')\n"
						+ "\n"
						+ "WHEN allcoltext like '%nov%' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}nov')\n"
						+ "then concat(left(edt2,4),'-','11','-')\n"
						+ "\n"
						+ "WHEN allcoltext like '%dec%' and length(replace(edt2,'-',''))=4 \n"
						+ "and allColText rlike concat('c',col,':[\\\\(\\\\$\\\\)A-Z0-09;,]{0,30}dec')\n"
						+ "then concat(left(edt2,4),'-','12','-')\n"
						+ "\n"
						+ "else 'xx' end edt3 \n"
						+ " from bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ " t1 where length(edt2)<10 and length(edt2)>=4\n"
						+ " AND EDT2 NOT LIKE '-%' AND (EDT2 LIKE '1%' OR EDT2 LIKE '2%')\n"
						+ " AND ( \n"
						+ " \n"
						+ " ((columnPattern REGEXP 'jan') + (columnPattern REGEXP 'feb')\n"
						+ "+ (columnPattern REGEXP 'mar')+ (columnPattern REGEXP 'apr')\n"
						+ "+ (columnPattern REGEXP 'may')+ (columnPattern REGEXP 'jun')\n"
						+ "+ (columnPattern REGEXP 'jul')+ (columnPattern REGEXP 'aug')\n"
						+ "+ (columnPattern REGEXP 'sep')+ (columnPattern REGEXP 'oct')\n"
						+ "+ (columnPattern REGEXP 'nov')+ (columnPattern REGEXP 'dec')\n"
						+ ")=1\n"
						+ "\n"
						+ "OR\n"
						+ "\n"
						+ "((allcoltext REGEXP 'jan') + (allcoltext REGEXP 'feb')\n"
						+ "+ (allcoltext REGEXP 'mar')+ (allcoltext REGEXP 'apr')\n"
						+ "+ (allcoltext REGEXP 'may')+ (allcoltext REGEXP 'jun')\n"
						+ "+ (allcoltext REGEXP 'jul')+ (allcoltext REGEXP 'aug')\n"
						+ "+ (allcoltext REGEXP 'sep')+ (allcoltext REGEXP 'oct')\n"
						+ "+ (allcoltext REGEXP 'nov')+ (allcoltext REGEXP 'dec')\n"
						+ ") = 1\n"
						+ "\n"
						+ ")\n"
						+ "AND  \n"
						+ " \n"
						+ " ((columnPattern REGEXP 'jan') + (columnPattern REGEXP 'feb')\n"
						+ "+ (columnPattern REGEXP 'mar')+ (columnPattern REGEXP 'apr')\n"
						+ "+ (columnPattern REGEXP 'may')+ (columnPattern REGEXP 'jun')\n"
						+ "+ (columnPattern REGEXP 'jul')+ (columnPattern REGEXP 'aug')\n"
						+ "+ (columnPattern REGEXP 'sep')+ (columnPattern REGEXP 'oct')\n"
						+ "+ (columnPattern REGEXP 'nov')+ (columnPattern REGEXP 'dec')\n"
						+ ")<=1\n"
						+ "\n"
						+ "AND\n"
						+ "\n"
						+ "((allcoltext REGEXP 'jan') + (allcoltext REGEXP 'feb')\n"
						+ "+ (allcoltext REGEXP 'mar')+ (allcoltext REGEXP 'apr')\n"
						+ "+ (allcoltext REGEXP 'may')+ (allcoltext REGEXP 'jun')\n"
						+ "+ (allcoltext REGEXP 'jul')+ (allcoltext REGEXP 'aug')\n"
						+ "+ (allcoltext REGEXP 'sep')+ (allcoltext REGEXP 'oct')\n"
						+ "+ (allcoltext REGEXP 'nov')+ (allcoltext REGEXP 'dec')\n"
						+ ") <= 1\n"
						+ ";\n\n"
						+ "\n"
						+ "ALTER TABLE TMP_MOPATTERN_"
						+ yr
						+ "_QTR"
						+ q
						+ " add key(edt3), ADD KEY(ACCNO), ADD KEY(TNO), ADD KEY(ROW);\n"
						+ "\n"
						+ "UPDATE IGNORE bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ " T1 INNER JOIN TMP_MOPATTERN_"
						+ yr
						+ "_QTR"
						+ q
						+ " T2\n"
						+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.ROW=T2.ROW\n"
						+ "SET T1.EDT2=T2.EDT3, t1.ended='EDT2'\n"
						+ "where edt3!='xx';\n");

		/*
		 * year in edt2 but no month day. Find month day in allColText that
		 * corresponds to same month day in allColtext and same column in
		 * allcoltext. I limit matches to either first or last col
		 */

		sb.append("\n" + "DROP TABLE IF EXISTS TMP_"
				+ yr
				+ "_"
				+ q
				+ "_MODAY;\n"
				+ "CREATE TABLE TMP_"
				+ yr
				+ "_"
				+ q
				+ "_MODAY ENGINE=MYISAM\n"
				+ "SELECT \n"
				+ "@moday:=case \n"
				+ "when ALLCOLTEXT rlike concat('C',col,':Jan') then\n"
				+ "replace(substring_index(substring(allColText,LOCATE(concat('C',col,':Jan'), allColText), 10000),'|',1),concat('C',col,':'),'')\n"
				+ "\n"
				+ "when ALLCOLTEXT rlike concat('C',col,':Feb') then\n"
				+ "replace(substring_index(substring(allColText,LOCATE(concat('C',col,':Feb'), allColText), 10000),'|',1),concat('C',col,':'),'')\n"
				+ "\n"
				+ "when ALLCOLTEXT rlike concat('C',col,':Mar') then\n"
				+ "replace(substring_index(substring(allColText,LOCATE(concat('C',col,':Mar'), allColText), 10000),'|',1),concat('C',col,':'),'')\n"
				+ "\n"
				+ "when ALLCOLTEXT rlike concat('C',col,':Apr') then\n"
				+ "replace(substring_index(substring(allColText,LOCATE(concat('C',col,':Apr'), allColText), 10000),'|',1),concat('C',col,':'),'')\n"
				+ "\n"
				+ "when ALLCOLTEXT rlike concat('C',col,':May') then\n"
				+ "replace(substring_index(substring(allColText,LOCATE(concat('C',col,':May'), allColText), 10000),'|',1),concat('C',col,':'),'')\n"
				+ "\n"
				+ "when ALLCOLTEXT rlike concat('C',col,':Jun') then\n"
				+ "replace(substring_index(substring(allColText,LOCATE(concat('C',col,':Jun'), allColText), 10000),'|',1),concat('C',col,':'),'')\n"
				+ "\n"
				+ "when ALLCOLTEXT rlike concat('C',col,':Jul') then\n"
				+ "replace(substring_index(substring(allColText,LOCATE(concat('C',col,':Jul'), allColText), 10000),'|',1),concat('C',col,':'),'')\n"
				+ "\n"
				+ "when ALLCOLTEXT rlike concat('C',col,':Aug') then\n"
				+ "replace(substring_index(substring(allColText,LOCATE(concat('C',col,':Aug'), allColText), 10000),'|',1),concat('C',col,':'),'')\n"
				+ "\n"
				+ "when ALLCOLTEXT rlike concat('C',col,':Sep') then\n"
				+ "replace(substring_index(substring(allColText,LOCATE(concat('C',col,':Sep'), allColText), 10000),'|',1),concat('C',col,':'),'')\n"
				+ "\n"
				+ "when ALLCOLTEXT rlike concat('C',col,':Oct') then\n"
				+ "replace(substring_index(substring(allColText,LOCATE(concat('C',col,':Oct'), allColText), 10000),'|',1),concat('C',col,':'),'')\n"
				+ "\n"
				+ "when ALLCOLTEXT rlike concat('C',col,':Nov') then\n"
				+ "replace(substring_index(substring(allColText,LOCATE(concat('C',col,':Nov'), allColText), 10000),'|',1),concat('C',col,':'),'')\n"
				+ "\n"
				+ "when ALLCOLTEXT rlike concat('C',col,':Dec') then\n"
				+ "replace(substring_index(substring(allColText,LOCATE(concat('C',col,':Dec'), allColText), 10000),'|',1),concat('C',col,':'),'')\n"
				+ "\n"
				+ "else '' end ss,\n"
				+ "@mo:=case when @moday rlike 'Jan' and @moday not rlike 'feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' then '01' \n"
				+ "when @moday rlike 'Feb' and @moday not rlike 'jan|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' then '02' \n"
				+ "when @moday rlike 'Mar' and @moday not rlike 'jan|feb|apr|may|jun|jul|aug|sep|oct|nov|dec' then '03' \n"
				+ "when @moday rlike 'Apr' and @moday not rlike 'jan|feb|mar|may|jun|jul|aug|sep|oct|nov|dec' then '04' \n"
				+ "when @moday rlike 'May' and @moday not rlike 'jan|feb|mar|apr|jun|jul|aug|sep|oct|nov|dec' then '05' \n"
				+ "when @moday rlike 'Jun' and @moday not rlike 'jan|feb|mar|apr|may|jul|aug|sep|oct|nov|dec' then '06' \n"
				+ "when @moday rlike 'Jul' and @moday not rlike 'jan|feb|mar|apr|may|jun|sep|oct|nov|dec' then '07' \n"
				+ "when @moday rlike 'Aug' and @moday not rlike 'jan|feb|mar|apr|may|jun|jul|sep|oct|nov|dec' then '08' \n"
				+ "when @moday rlike 'Sep' and @moday not rlike 'jan|feb|mar|apr|may|jun|jul|aug|oct|nov|dec' then '09' \n"
				+ "when @moday rlike 'Oct' and @moday not rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|nov|dec' then '10' \n"
				+ "when @moday rlike 'Nov' and @moday not rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|dec' then '11' \n"
				+ "when @moday rlike 'Dec' and @moday not rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov' then '12' \n"
				+ "else '' end mo,@moday,\n"
				+ "@dy:=case when right(trim(replace(@moday,',','')),2) rlike '[0-9]{1,2}'\n"
				+ "and  right(trim(replace(@moday,',','')),5) not RLIKE '[12]{1}[90]{1}[0-9]{1,2}' and @mo between 1 and 12\n"
				+ "then right(trim(replace(@moday,',','')),2)  else '' end dy,\n"
				+ "case when @mo between 1 and 12 and @dy between 10 and 31 then concat(left(edt2,4),'-',@mo,'-',trim(@dy))\n"
				+ "when @mo between 1 and 12 and @dy between 1 and 9  then concat(left(edt2,4),'-',@mo,'-0',trim(@dy))\n"
				+ "else 'xx' end edt3,accno,tno,row /*,EDT2,EDT1,P1,P2,TSSHORT,COLUMNTEXT*/\n"
				+ "from "
				+ table
				+ " t1\n"
				+ "\n"
				+ "WHERE \n"
				+ "(REPLACE(EDT2,'-','')  RLIKE '^20[0-9]{2}$'  or REPLACE(EDT2,'-','')  RLIKE '^19[0-9]{2}$' \n"
				+ ")\n"
				+ "AND ALLCOLTEXT\n"
				+ "rlike concat('C',col,':(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-zA-Z,]{0,10}[ ]{0,2}[0-9]{1,2}')\n"
				+ "and (col=1 or tc=col) AND ALLCOLTEXT\n"
				+ "not rlike concat('C',tc+1,':(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-zA-Z,]{0,10}[ ]{0,2}[0-9]{1,2}')\n"
				+ "AND ALLCOLTEXT\n"
				+ "rlike concat('C',tc,':(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-zA-Z,]{0,10}[ ]{0,2}[0-9]{1,2}');\n"
				+ "\n"
				+ "ALTER TABLE TMP_"
				+ yr
				+ "_"
				+ q
				+ "_MODAY change edt3 edt3 varchar(20),ADD KEY(EDT3), ADD KEY(ACCNO),ADD KEY(TNO),ADD KEY(ROW);\n"
				+ "\n" + "UPDATE IGNORE " + table + " T1 INNER JOIN TMP_" + yr
				+ "_" + q + "_MODAY T2\n"
				+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.ROW=T2.ROW\n"
				+ "SET EDT2=left(EDT3,10), ENDED='EDT2'\n"
				+ "WHERE EDT3!='XX';\n");

		/*
		 * THESE UPDATES FIND WHERE EDT2 IS INCOMPLETE - E.G.: 1995-04- BUT IN
		 * COLUMNTEXT THERE IS A DAY VALUE. THIS FINDS DAY IN COLUMN TEXT AND IF
		 * DAY VALUE IS 20 OR HIGHER ASSIGNS DAY VALUE OF 30 UNLESS MONTH IS FEB
		 * THEN 28. IF <15 IT ASSIGNS DAY VALUE W/N 5 OF QUERIED DAY. I USE SAME
		 * CONDITIONS TO SET ENDED='EDT2' AND SAME WHERE FILTER CONDITION AS IN
		 * CASE WHEN
		 */

		sb.append("\nUPDATE IGNORE "
				+ table
				+ " \n"
				+ "set edt2=\n"
				+ "case when left(edt2,8) rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-' and length(replace(edt2,'-',''))<7\n"
				+ "and right(replace(edt2,'-',''),1)!=2\n"
				+ "and (columntext rlike '3[01]{1} | 3[01]{1}' \n"
				+ "or (trim(columntext) rlike '2[0-9]{1} | 2[0-9]{1}' and columntext not rlike '2[0-9]{1}.{1,5}week')\n"
				+ "or (columntext rlike ' 2[0-9]{1},' and columntext rlike '2[0-9]{1}.{1,5}week')\n"
				+ "or columntext rlike '( |\\\\.)[23]{1}[0-9]{1},')\n"
				+ "then concat(left(edt2,8),'30') \n"
				+ "when left(edt2,8) rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-' and length(replace(edt2,'-',''))<7\n"
				+ "and right(replace(edt2,'-',''),1)=2\n"
				+ "and (columntext rlike '3[01]{1} | 3[01]{1}' \n"
				+ "or (columntext rlike '2[1-9]{1} | 2[0-9]{1}' and columntext not rlike '2[0-9]{1}.{1,5}week')\n"
				+ "or (columntext rlike ' 2[0-9]{1},' and columntext rlike '2[0-9]{1}.{1,5}week')\n"
				+ "or columntext rlike '( |\\\\.)[23]{1}[0-9]{1},')\n"
				+ "then concat(left(edt2,8),'28') \n"
				+ "when columntext rlike '( |\\\\.)[1-4]{1},'\n"
				+ "and left(edt2,8) rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-' and length(replace(edt2,'-',''))<7\n"
				+ "then concat(left(edt2,8),'04') \n"
				+ "\n"
				+ "when columntext rlike '( |\\\\.)[5-9]{1},'\n"
				+ "and left(edt2,8) rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-' and length(replace(edt2,'-',''))<7\n"
				+ "then concat(left(edt2,8),'09')\n"
				+ "when columntext rlike '( |\\\\.)(10|11|12|13|14),' \n"
				+ "and left(edt2,8) rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-' and length(replace(edt2,'-',''))<7\n"
				+ "then concat(left(edt2,8),'14')\n"
				+ "WHEN COLUMNTEXT RLIKE '(RY|CH|IL|AY|NE|LY|ST|BER)[,]{1} ?[0-9]{1}[, ]{1}' and LENGTH(trim(EDT2))<9 THEN concat(left(edt2,8),'04')\n"
				+ "WHEN COLUMNTEXT RLIKE '(RY|CH|IL|AY|NE|LY|ST|BER)[,]{1} ?1[0-9]{1}[, ]{1}' and LENGTH(trim(EDT2))<9 THEN concat(left(edt2,8),'15')"
				+ "WHEN COLUMNTEXT RLIKE '(RY|CH|IL|AY|NE|LY|ST|BER)[,]{1} ?2[0-9]{1}[, ]{1}' and LENGTH(trim(EDT2))<9 THEN concat(left(edt2,8),'25')\n"
				+ "WHEN COLUMNTEXT RLIKE '(RY|CH|IL|AY|NE|LY|ST|BER)[,]{1} ?3[01]{1}[, ]{1}' and LENGTH(trim(EDT2))<9 THEN concat(left(edt2,8),'30')\n"
				+ "else edt2 end ,\n"
				+ "ended =\n"
				+ "case when left(edt2,8) rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-' and length(replace(edt2,'-',''))<7\n"
				+ "and right(replace(edt2,'-',''),1)!=2\n"
				+ "and (columntext rlike '3[01]{1} | 3[01]{1}' \n"
				+ "or (trim(columntext) rlike '2[0-9]{1} | 2[0-9]{1}' and columntext not rlike '2[0-9]{1}.{1,5}week')\n"
				+ "or (columntext rlike ' 2[0-9]{1},' and columntext rlike '2[0-9]{1}.{1,5}week')\n"
				+ "or columntext rlike '( |\\\\.)[23]{1}[0-9]{1},')\n"
				+ "then 'EDT2' \n"
				+ "when left(edt2,8) rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-' and length(replace(edt2,'-',''))<7\n"
				+ "and right(replace(edt2,'-',''),1)=2\n"
				+ "and (columntext rlike '3[01]{1} | 3[01]{1}' \n"
				+ "or (trim(columntext) rlike '2[0-9]{1} | 2[0-9]{1}' and columntext not rlike '2[0-9]{1}.{1,5}week')\n"
				+ "or (columntext rlike ' 2[0-9]{1},' and columntext rlike '2[0-9]{1}.{1,5}week')\n"
				+ "or columntext rlike '( |\\\\.)[23]{1}[0-9]{1},')\n"
				+ "then 'EDT2' \n"
				+ "when columntext rlike '( |\\\\.)[1-4]{1},'\n"
				+ "and left(edt2,8) rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-' and length(replace(edt2,'-',''))<7\n"
				+ "then 'EDT2' \n"
				+ "when columntext rlike '( |\\\\.)[5-9]{1},'\n"
				+ "and left(edt2,8) rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-' and length(replace(edt2,'-',''))<7\n"
				+ "then 'EDT2'\n"
				+ "when columntext rlike '( |\\\\.)(10|11|12|13|14),' \n"
				+ "and left(edt2,8) rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-' and length(replace(edt2,'-',''))<7\n"
				+ "then 'EDT2'\n"
				+ "else ended end\n"
				+ "where ((\n"
				+ "(left(edt2,8) rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-' and length(replace(edt2,'-',''))<7\n"
				+ "and right(replace(edt2,'-',''),1)!=2\n"
				+ "and (columntext rlike '3[01]{1} | 3[01]{1}' \n"
				+ "or (trim(columntext) rlike '2[0-9]{1} | 2[0-9]{1}' and columntext not rlike '2[0-9]{1}.{1,5}week')\n"
				+ "or (columntext rlike ' 2[0-9]{1},' and columntext rlike '2[0-9]{1}.{1,5}week')\n"
				+ "or columntext rlike '( |\\\\.)[23]{1}[0-9]{1},')) or\n"
				+ "(left(edt2,8) rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-' and length(replace(edt2,'-',''))<7\n"
				+ "and right(replace(edt2,'-',''),1)=2\n"
				+ "and (columntext rlike '3[01]{1} | 3[01]{1}' \n"
				+ "or (trim(columntext) rlike '2[0-9]{1} | 2[0-9]{1}' and columntext not rlike '2[0-9]{1}.{1,5}week')\n"
				+ "or (columntext rlike ' 2[0-9]{1},' and columntext rlike '2[0-9]{1}.{1,5}week')\n"
				+ "or columntext rlike '( |\\\\.)[23]{1}[0-9]{1},')\n"
				+ ")\n"
				+ "or\n"
				+ "(columntext rlike '( |\\\\.)[1-4]{1},'\n"
				+ "and left(edt2,8) rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-' and length(replace(edt2,'-',''))<7)\n"
				+ "or\n"
				+ "( columntext rlike '( |\\\\.)[5-9]{1},'\n"
				+ "and left(edt2,8) rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-' and length(replace(edt2,'-',''))<7)\n"
				+ "or\n"
				+ "(columntext rlike '( |\\\\.)(10|11|12|13|14),' \n"
				+ "and left(edt2,8) rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-' and length(replace(edt2,'-',''))<7\n"
				+ ")))\n"
				+ " or (COLUMNTEXT RLIKE '(RY|CH|IL|AY|NE|LY|ST|BER)[,]{1} ?([0-9]{1}|3[01]{1}|2[0-9]|1[0-9]{1})[, ]{1}' AND LENGTH(trim(EDT2))<9)"
				+ " ;\n\n");

		sb.append("UPDATE IGNORE "
				+ table
				+ " set edt2=\n"
				+ "CASE \n"
				+ "WHEN COLUMNTEXT RLIKE 'SEP\\\\.MBER [0-4]{1}[, ]{1,2}[12]{1}[09]{1}[0-9]{2}' and right(edt2,5) not rlike '09-[0-9]{2}' THEN CONCAT(LEFT(EDT2,5),'09-02') \n"
				+ "WHEN COLUMNTEXT RLIKE 'SEP\\\\.MBER [5-9]{1}[, ]{1,2}[12]{1}[09]{1}[0-9]{2}' and right(edt2,5) not rlike '09-[0-9]{2}'THEN CONCAT(LEFT(EDT2,5),'09-07') \n"
				+ "WHEN COLUMNTEXT RLIKE 'SEP\\\\.MBER 1[0-3]{1}[, ]{1,2}[12]{1}[09]{1}[0-9]{2}' and right(edt2,5) not rlike '09-[0-9]{2}'THEN CONCAT(LEFT(EDT2,5),'09-12') \n"
				+ "ELSE EDT2 END,ended='EDT2' \n"
				+ "WHERE \n"
				+ "DAY(EDT2)>10\n"
				+ "AND (COLUMNTEXT RLIKE 'SEP\\\\.MBER [0-9]{1}[, ]{1,2}[12]{1}[09]{1}[0-9]{2}'\n"
				+ "OR\n"
				+ "COLUMNTEXT RLIKE 'SEP\\\\.MBER 1[0-3]{1}[, ]{1,2}[12]{1}[09]{1}[0-9]{2}')\n"
				+ " and right(edt2,5) not rlike '09-[0-9]{2}';\n");

		sb.append("\n"
				+ "/*IF mCntD is 1 and month missing from edt2 then use mCntD from columnPattern. This has a very small error rate but gathers\n"
				+ "a very large number of missing enddates.  I check that there's only 1 month in both allColText and columnPattern. I also check\n"
				+ "that yr in ColumnPattern matches yr in edt1 or edt2 as well as other check.*/\n"
				+ "\n" + "DROP TABLE IF EXISTS TMP_MCNTD_"
				+ yr
				+ "QTR"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP_MCNTD_"
				+ yr
				+ "QTR"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "select ACCNO,TNO,COL\n"
				+ "/*t1.rowname,t1.value,t1.col,t1.edt2,t1.edt1,tsshort,*/\n"
				+ ",@yr:=case \n"
				+ "when T1.col=1 then substring_index(substring_index(t1.columnpattern,'1Y:',-1),'|',1) \n"
				+ "when T1.col=2 then substring_index(substring_index(t1.columnpattern,'2Y:',-1),'|',1) \n"
				+ "when T1.col=3 then substring_index(substring_index(t1.columnpattern,'3Y:',-1),'|',1) \n"
				+ "when T1.col=4 then substring_index(substring_index(t1.columnpattern,'3Y:',-1),'|',1) \n"
				+ "when T1.col=t1.tc then substring_index(substring_index(t1.columnpattern,concat(tc,'Y:'),-1),'|',1) end yr\n"
				+ ",@nwM:=case \n"
				+ "when (columnPattern rlike 'decem|dec |dec\\\\.' or allcolText rlike 'decem|dec |dec\\\\.' )\n"
				+ "and columnPattern not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov'\n"
				+ "and allcolText    not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov' then '12'\n"
				+ "\n"
				+ "when (columnPattern rlike 'nov |nov\\\\.|nov' or allcolText rlike 'nov |nov\\\\.|nov' )\n"
				+ "and columnPattern not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|decem|dec |dec\\\\.'\n"
				+ "and allcolText    not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|decem|dec |dec\\\\.' then '11'\n"
				+ "\n"
				+ "when (columnPattern rlike 'oct |oct\\\\.|oct' or allcolText rlike 'oct |oct\\\\.|oct' )\n"
				+ "and columnPattern not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|decem|dec |dec\\\\.|nov |nov\\\\.|nov'\n"
				+ "and allcolText    not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|decem|dec |dec\\\\.|nov |nov\\\\.|nov' then '10'\n"
				+ "\n"
				+ "when (columnPattern rlike 'sep |sep\\\\.|sept|sepe' or allcolText rlike 'sep |sep\\\\.|sept|sepe' )\n"
				+ "and columnPattern not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|decem|dec |dec\\\\.|oct |oct\\\\.|oct|nov |nov\\\\.|nov'\n"
				+ "and allcolText    not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|decem|dec |dec\\\\.|oct |oct\\\\.|oct|nov |nov\\\\.|nov' then '09'\n"
				+ "\n"
				+ "when (columnPattern rlike 'aug |aug\\\\.|augu' or allcolText rlike 'aug |aug\\\\.|augu' )\n"
				+ "and columnPattern not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|jul |jul\\\\.|july|decem|dec |dec\\\\.|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov'\n"
				+ "and allcolText    not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|jul |jul\\\\.|july|decem|dec |dec\\\\.|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov' then '08'\n"
				+ "\n"
				+ "when (columnPattern rlike 'jul |jul\\\\.|july' or allcolText rlike 'jul |jul\\\\.|july' )\n"
				+ "and columnPattern not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|decem|dec |dec\\\\.|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov'\n"
				+ "and allcolText    not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|decem|dec |dec\\\\.|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov' then '07'\n"
				+ "\n"
				+ "when (columnPattern rlike 'jun |jun\\\\.|june' or allcolText rlike 'jun |jun\\\\.|june' )\n"
				+ "and columnPattern not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |decem|dec |dec\\\\.|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov'\n"
				+ "and allcolText    not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |decem|dec |dec\\\\.|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov' then '06'\n"
				+ "\n"
				+ "when (columnPattern rlike 'may ' or allcolText rlike 'may ' )\n"
				+ "and columnPattern not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|decem|dec |dec\\\\.|jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov'\n"
				+ "and allcolText    not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|decem|dec |dec\\\\.|jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov' then '05'\n"
				+ "\n"
				+ "when (columnPattern rlike 'apr |apr\\\\.|apri' or allcolText rlike 'apr |apr\\\\.|apri' )\n"
				+ "and columnPattern not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|decem|dec |dec\\\\.|may |jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov'\n"
				+ "and allcolText    not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|decem|dec |dec\\\\.|may |jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov' then '04'\n"
				+ "\n"
				+ "when (columnPattern rlike 'mar |mar\\\\.|marc' or allcolText rlike 'mar |mar\\\\.|marc' )\n"
				+ "and columnPattern not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|decem|dec |dec\\\\.|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov'\n"
				+ "and allcolText    not rlike 'janu|jan |jan\\\\.|feb |feb\\\\.|febr|decem|dec |dec\\\\.|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov' then '03'\n"
				+ "\n"
				+ "when (columnPattern rlike 'feb |feb\\\\.|febr' or allcolText rlike 'feb |feb\\\\.|febr' )\n"
				+ "and columnPattern not rlike 'janu|jan |jan\\\\.|decem|dec |dec\\\\.|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov'\n"
				+ "and allcolText    not rlike 'janu|jan |jan\\\\.|decem|dec |dec\\\\.|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov' then '02'\n"
				+ "\n"
				+ "when (columnPattern rlike 'janu|jan |jan\\\\.' or allcolText rlike 'jan' )\n"
				+ "and columnPattern not rlike 'decem|dec |dec\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov'\n"
				+ "and allcolText    not rlike 'decem|dec |dec\\\\.|feb |feb\\\\.|febr|mar |mar\\\\.|marc|apr |apr\\\\.|apri|may |jun |jun\\\\.|june|jul |jul\\\\.|july|aug |aug\\\\.|augu|sep |sep\\\\.|sept|sepe|oct |oct\\\\.|oct|nov |nov\\\\.|nov' then '01'\n"
				+ "else '' end nw\n"
				+ "\n"
				+ ",@mo:=substring_index(substring_index(t1.columnpattern,'1M:',-1),'|',1) mo \n"
				+ "/*,@mo2:=trim((substring_index(@mo,' ',1))) mo2\n"
				+ ",@moI:=case when month(str_to_date(@mo2,'%M'))<10 then concat(0,month(str_to_date(@mo2,'%M'))) else month(str_to_date(@mo2,'%M')) end moI\n"
				+ ",@nwM-@moI er*/\n"
				+ ",@dy:=case when right(@mo,2) between 1 and 9 then concat(0,right(@mo,1)) when right(@mo,2) between 10 and 31 then right(@mo,2) \n"
				+ "when left(edt2,4)=left(edt1,4) and length(edt1)=10 and edt1 rlike '[12]{1}[09]{1}[0-9]{1}[0-9]{1}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})' then right(edt1,2)\n"
				+ "else '' end dy\n"
				+ ",case \n"
				+ "when left(edt2,7) rlike  '[12]{1}[09]{1}[0-9]{2}-(0[0-9]{1}|1[0-2]{1})' and @nwM between 1 and 12 then  concat(left(edt2,7),'-',@dy)\n"
				+ "when left(edt2,4) rlike  '[12]{1}[09]{1}[0-9]{2}' and @nwM between 1 and 12 then concat(left(edt2,4),'-',@nwM,'-',@dy)\n"
				+ "when left(edt1,10) rlike '[12]{1}[09]{1}[0-9]{2}-(0[0-9]{1}|1[0-2]{1})' and @nwM=right(left(edt1,7),2) and left(edt1,4)=@yr \n"
				+ "and right(edt1,2) between 0 and 31 and @nwM between 1 and 12\n"
				+ "then left(edt1,10) \n"
				+ "when left(edt1,7)  rlike '[12]{1}[09]{1}[0-9]{2}-(0[0-9]{1}|1[0-2]{1})' and @nwM=right(left(edt1,7),2) and left(edt1,4)=@yr and @nwM between 1 and 12\n"
				+ "then concat(left(edt1,7),'-',@dy) \n"
				+ "when left(edt1,4) rlike '[12]{1}[09]{1}[0-9]{2}' and @nwM between 1 and 12 \n"
				+ "then concat(left(edt1,4),'-',@nwM,'-',@dy)\n"
				+ "else 'xx' end EDT3\n"
				+ "/*,case when left(edt1,4) rlike '[12]{1}[09]{1}[0-9]{2}' and left(edt1,4)!=@yr  then 'noYr' else '' end noYr\n"
				+ ",case when length(date_format(concat(@yr,'-',month(str_to_date(@mo2,'%M')),'-',right(@mo,2)),'%Y-%m-%d'))=10 then \n"
				+ "date_format(concat(@yr,'-',month(str_to_date(@mo2,'%M')),'-',right(@mo,2)),'%Y-%m-%d') else 'xx' end xx\n"
				+ ",concat('file:///c:/backtest/tableParser/',year(filedate),'/qtr',quarter(filedate),'/tables/',accno,'_',tno,'.txt') link,columnPattern,allcoltext\n"
				+ "*/\n"
				+ "\n"
				+ "from bac_tp_raw"
				+ yr
				+ "QTR"
				+ q
				+ " T1 \n"
				+ "where \n"
				+ "columnPattern rlike 'mCntD:1'\n"
				+ "and t1.edt2 not rlike '[12]{1}[09]{1}[0-9]{1}[0-9]{1}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})'\n"
				+ "and t1.columnPattern not rlike '/' \n"
				+ "group by accno,tno,col;\n"
				+ "\n"
				+ "ALTER TABLE TMP_MCNTD_"
				+ yr
				+ "QTR"
				+ q
				+ " CHANGE EDT3 EDT3 VARCHAR(11),ADD KEY(ACCNO),ADD KEY(TNO),ADD KEY(COL), ADD KEY(EDT3);\n"
				+ "\n"
				+ "/*SELECT T1.EDT2,T2.EDT3,T1.ROWNAME,T1.VALUE\n"
				+ ",concat('file:///c:/backtest/tableParser/',year(T1.filedate),'/qtr',quarter(T1.filedate),'/tables/',T1.accno,'_',T1.tno,'.txt') link\n"
				+ "FROM */\n"
				+ "UPDATE BAC_TP_RAW"
				+ yr
				+ "QTR"
				+ q
				+ " T1 INNER JOIN TMP_MCNTD_"
				+ yr
				+ "QTR"
				+ q
				+ " T2 \n"
				+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.COL=T2.COL\n"
				+ "SET T1.EDT2=T2.EDT3\n" + "WHERE EDT3!='XX';\n" + "\n");

		/*
		 * combined with 2nd prior above: sb.append("UPDATE IGNORE " + table +
		 * " set edt2=" +
		 * "CASE WHEN COLUMNTEXT RLIKE '(RY|CH|IL|AY|NE|LY|ST|BER)[,]{1} ?[0-9]{1}[, ]{1}' \n"
		 * + "AND DAY(EDT2)>14 THEN concat(left(edt2,8),'04') else edt2 end \n"
		 * + ",ended='EDT2'" +
		 * "WHERE COLUMNTEXT RLIKE '(RY|CH|IL|AY|NE|LY|ST|BER)[,]{1} ?[0-9]{1}[, ]{1}'\n"
		 * + "AND DAY(EDT2)>14\n" + "AND LENGTH(EDT2)=10;\n");
		 */
		String dropProc = "DROP PROCEDURE IF EXISTS updateEdt2" + yr + "QTR"
				+ q + ";\n" + "CREATE PROCEDURE updateEdt2" + yr + "QTR" + q
				+ "()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
		MysqlConnUtils.executeQuery("call updateEdt2" + yr + "QTR" + q
				+ "();\n");
		sb.delete(0, sb.toString().length());

	}

	public void tp_sales_to_scrubDeleteDuplicates(String tp_sales_to_scrub2)
			throws SQLException, FileNotFoundException {

		/*
		 * only need to run once (it runs against entire tp_sales_to_scrub) -
		 * deletes duplicate TNOs by dropping the the TNO that has the smaller
		 * maximum value
		 */

		StringBuffer sb = new StringBuffer(
				"\nDROP TABLE IF EXISTS TMP_TP_SALES_TO_SCRUB_TWO_TNO_A;\n"
						+ "CREATE TABLE TMP_TP_SALES_TO_SCRUB_TWO_TNO_A ENGINE=MYISAM\n"
						+ "select count(distinct(value)) cntVal, count(distinct(tno)) cntTno, t1.* from "
						+ tp_sales_to_scrub2
						+ " t1 \n"
						+ "group by accno,cik,edt2,p2;\n"
						+ "ALTER TABLE TMP_TP_SALES_TO_SCRUB_TWO_TNO_A ADD KEY(CNTTNO);\n\n"
						+ "DROP TABLE IF EXISTS TMP_TP_SALES_TO_SCRUB_TWO_TNO;\n"
						+ "CREATE TABLE TMP_TP_SALES_TO_SCRUB_TWO_TNO ENGINE=MYISAM\n"
						+ "SELECT * FROM TMP_TP_SALES_TO_SCRUB_TWO_TNO_A T1 WHERE cntTno>1 ;\n"
						+ "ALTER TABLE TMP_TP_SALES_TO_SCRUB_TWO_TNO ADD KEY(ACCNO),ADD KEY(EDT2),ADD KEY(P2);\n"
						+ "\n"
						+ "/*FOR ALL ACCNO W/ 2 OR MORE TNOs ORDER BY VALUE DESC AND MARK 1ST AS '1' AND REST AS 0.*/\n"
						+ "\n"
						+ "set @acc='0000000000-00-000000'; set @rw=0;\n"
						+ "\n\nDROP TABLE IF EXISTS TMP_TP_SALES_TO_SCRUB_TWO_TNO2_A;"
						+ "\nCREATE TABLE TMP_TP_SALES_TO_SCRUB_TWO_TNO2_A ENGINE=MYISAM"
						+ "\nselect \nt1.accno accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value , t1.p2, t1.edt2\n"
						+ ", t1.`DEC`, t1.columnText, t1.form\n"
						+ "from "
						+ tp_sales_to_scrub2
						+ " t1 inner join TMP_TP_SALES_TO_SCRUB_TWO_TNO t2\n"
						+ "on t1.accno=t2.accno and t1.edt2=t2.edt2 and t1.p2=t2.p2 order by t1.accno,t1.value desc;\n"
						+ "ALTER TABLE TMP_TP_SALES_TO_SCRUB_TWO_TNO2_A add key(accno), add key(value);"

						+ "\n\nDROP TABLE IF EXISTS TMP_TP_SALES_TO_SCRUB_TWO_TNO2;\n"
						+ "CREATE TABLE TMP_TP_SALES_TO_SCRUB_TWO_TNO2 ENGINE=MYISAM\n"
						+ "select case when @acc!=t1.accno or @rw=0 then 1 else 0 end getIt,@rw:=@rw+1 rw,\n"
						+ "@acc:=t1.accno accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value , t1.p2, t1.edt2\n"
						+ ", t1.`DEC`, t1.columnText, t1.form from TMP_TP_SALES_TO_SCRUB_TWO_TNO2_A t1 ORDER BY t1.ACCNO,t1.VALUE;\n"
						+ "ALTER TABLE TMP_TP_SALES_TO_SCRUB_TWO_TNO2 ADD KEY(getit), DROP COLUMN RW;\n"
						+ "\n"
						+ "/*THESE ARE ALL THE TNOs AND EACH TNOs TROW/ROWS TO KEEP VIA LEFT JOIN BACK TO TP_SALES_TO_SCRUB2 OF MAX VALUE TNO*/\n"
						+ "DROP TABLE IF EXISTS TMP_TP_SALES_TO_SCRUB_TWO_TNO3;\n"
						+ "CREATE TABLE TMP_TP_SALES_TO_SCRUB_TWO_TNO3 ENGINE=MYISAM\n"
						+ "select 1 getIt, t2.* from (SELECT * FROM TMP_TP_SALES_TO_SCRUB_TWO_TNO2 where getit=1 ) t1 \n"
						+ "left join "
						+ tp_sales_to_scrub2
						+ " t2 on t1.accno=t2.accno and t1.tno=t2.tno ;\n"
						+ "ALTER TABLE TMP_TP_SALES_TO_SCRUB_TWO_TNO3 ADD PRIMARY KEY(ACCNO,TNO,ROW);\n"
						+ "/*BY ADDING PRIMARY KEY ACCNO,TNO,ROW KNOWING THIS TABLE HAS ALL CORRECT TNOS - I CAN THEN BELOW INSERT ALL TNOs (GETIT=0).\n"
						+ "RESULTING TALBE WILL THEN HAVE TNOs AND ROWS MARKED WITH 0 THAT NEED TO BE DELETED FROM TP_SALES_TO_SCRUB2. PRIMARY KEY RETAINS\n"
						+ "GOOD TABLES WITH GETIT=1 MARK*/\n"
						+ "\n"
						+ "INSERT IGNORE INTO TMP_TP_SALES_TO_SCRUB_TWO_TNO3\n"
						+ "SELECT t1.*,'' FROM TMP_TP_SALES_TO_SCRUB_TWO_TNO2 t1;\n"
						+ "ALTER TABLE TMP_TP_SALES_TO_SCRUB_TWO_TNO3 ADD KEY(ACCNO),ADD KEY(TNO), ADD KEY(GETIT);\n"
						+ "\n"
						+ "DELETE T1 FROM "
						+ tp_sales_to_scrub2
						+ " T1 INNER JOIN TMP_TP_SALES_TO_SCRUB_TWO_TNO3 T2\n"
						+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO WHERE T2.GETIT=0;\n"
						+
						/*
						 * "\nDELETE FROM TP_SALES_TO_SCRUB2 \n" +
						 * "WHERE COLUMNTEXT RLIKE \n" +
						 * "'^ONE | ONE |^TWO | TWO |^FOUR | FOUR |^FIVE | FIVE |^SEVEN | SEVEN |^EIGHT | EIGHT|FIFTEEN|SIXTEEN|SEVENTEEN|EIGHTEEN|NINETEEN|HUNDRED|(^1 | 1 |^2 | 2 |^4 | 4 |^5 | 5 |^7 | 7 |^8 | 8 |^15 | 15 |^16 | 16 |^17 | 17 |^18 | 18 |^19 | 19 ).{0,1}(MO|WK)'"
						 * + "AND COLUMNTEXT NOT RLIKE \n" +
						 * "'QUARTER|QTR|TWENTY.{1,2}(FOUR|FIVE|SEVEN|EIGHT|THIRTY.{1,2}NINE)|FIFTY|(^3 | 3 |^9 | 9 |^6 | 6 |^12 | 12 ).{1,3}MO|^NINE | NINE |^THREE | THREE |^SIX | SIX |^TWELVE | TWELVE '\n\n;"
						 * +
						 * "DELETE FROM TP_SALES_TO_SCRUB2 where p2=0 or p2>12 or p2<3 or length(p2)<1;\n"
						 * +
						 */
						"DROP TABLE IF EXISTS TMP_TP_SALES_TO_SCRUB_TWO_TNO;\n"
						+ "DROP TABLE IF EXISTS TMP_TP_SALES_TO_SCRUB_TWO_TNO2;\n"
						+ "DROP TABLE IF EXISTS TMP_TP_SALES_TO_SCRUB_TWO_TNO3;\n");

		MysqlConnUtils.executeQuery(sb.toString());

		String dropProc = "DROP PROCEDURE IF EXISTS tp_sales_to_scrubDeleteDuplicates;\n"
				+ "CREATE PROCEDURE tp_sales_to_scrubDeleteDuplicates()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
		MysqlConnUtils
				.executeQuery("call tp_sales_to_scrubDeleteDuplicates();\n");

		sb.delete(0, sb.toString().length());
	}

	public void dropProcedures(int yr, int q) throws SQLException, FileNotFoundException {

		StringBuffer sb = new StringBuffer(
				"DROP PROCEDURE IF EXISTS getPeriodsConformEnddate"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP PROCEDURE IF EXISTS getPeriodsConformEnddate2"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+

						"DROP PROCEDURE IF EXISTS regenerateBac_TP_RawYYYYQtrNo"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP PROCEDURE IF EXISTS repairEnddatePeriod"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+

						"drop procedure  if exists fixBlankRownames"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "drop procedure  if exists fixRownamesAndTNtype"
						+ yr
						+ "QTR"
						+ q
						+ ";\n"
						+ "drop procedure  if exists fixTableSentenceEnddates"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "drop procedure  if exists getAndmarkAllBadEndDatesAndPeriodsAcrossYears"
						+ yr
						+ "QTR"
						+ q
						+ ";\n"
						+ "drop procedure  if exists getMissingEndDatesAcrossYears"
						+ yr
						+ "QTR"
						+ q
						+ ";\n"
						+ "drop procedure  if exists getSalesFromBlankRows"
						+ yr
						+ "QTR"
						+ q
						+ ";\n"
						+ "drop procedure  if exists markBadTblsAndRepairEdtPer"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "drop procedure  if exists prep_tp_Id"
						+ yr
						+ "QTR"
						+ q
						+ ";\n"
						+ "drop procedure  if exists updateP2FromColumnPattern"
						+ yr
						+ "QTR"
						+ q
						+ ";\n"
						+ "drop procedure  if exists tp_sales_to_scrub"
						+ yr
						+ "QTR"
						+ q
						+ "_"
						+ q
						+ ";\n"
						+ "drop procedure  if exists tp_sales_to_scrub2"
						+ yr
						+ "QTR"
						+ q
						+ "; \n"
						+ "DROP PROCEDURE IF EXISTS fixRownamesAndTablename"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP PROCEDURE IF EXISTS markBadTables"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP PROCEDURE IF EXISTS fixRownamesAndTNtype"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP PROCEDURE IF EXISTS updatePeriodFromEnded"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP PROCEDURE IF EXISTS updateEdt2p2"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP PROCEDURE IF EXISTS updateEdt2p2_2"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP PROCEDURE IF EXISTS updateEdt2"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP PROCEDURE IF EXISTS updatePeriodFromColumnPattern"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP PROCEDURE IF EXISTS updatePeriodFromColumnPattern_2"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP PROCEDURE IF EXISTS updatePeriodFromColumnPattern_3"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP PROCEDURE IF EXISTS updatePeriodFromColumnPattern_4"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP PROCEDURE IF EXISTS updatePeriodFromColumnPattern_5"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP PROCEDURE IF EXISTS updatePeriodFromColumnPattern_6"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP PROCEDURE IF EXISTS renameTables"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP PROCEDURE IF EXISTS getEndDatesAndPeriodsFromOtherTablesAcrossOneQuarter"
						+ yr + "qtr" + q + ";\n"
						+ "DROP PROCEDURE IF EXISTS prep_tp_is_Id" + yr + "qtr"
						+ q + ";\n" + "DROP PROCEDURE IF EXISTS prep_tp_is_Id"
						+ yr + "qtr" + q + ";\n"
						+ "DROP PROCEDURE IF EXISTS tp_Sales_fixEndDates;\n"
						+ "DROP PROCEDURE IF EXISTS queryGetSalesTable" + yr
						+ "qtr" + q + ";\n"
						+ "DROP PROCEDURE IF EXISTS tp_sales_to_scrub" + yr
						+ "qtr" + q + "_0;\n"
						+ "DROP PROCEDURE IF EXISTS tp_sales_to_scrub" + yr
						+ "qtr" + q + "_1;\n"
						+ "DROP PROCEDURE IF EXISTS tp_sales_to_scrub2" + yr
						+ "qtr" + q + ";\n"
						+ "DROP PROCEDURE IF EXISTS tp_sales_to_scrub3" + yr
						+ "qtr" + q + ";\n"
						+ "DROP PROCEDURE IF EXISTS tp_sales_to_scrub4" + yr
						+ "qtr" + q + ";\n"
						+ "DROP PROCEDURE IF EXISTS tp_sales_to_scrub5" + yr
						+ "qtr" + q + ";\n"
						+ "DROP PROCEDURE IF EXISTS getSalesFromBlankRows" + yr
						+ "qtr" + q + ";\n"
						+ "DROP PROCEDURE IF EXISTS tp_p3Prep" + yr + "_" + q
						+ ";\n" + "DROP PROCEDURE IF EXISTS queryGetSalesTable"
						+ yr + "qtr" + q + ";\n"
						+ "DROP PROCEDURE IF EXISTS tp_sales_to_scrub" + yr
						+ "qtr" + q + "_0;\n"
						+ "DROP PROCEDURE IF EXISTS tp_sales_to_scrub" + yr
						+ "qtr" + q + "_1;\n"
						+ "DROP PROCEDURE IF EXISTS tp_sales_to_scrub2" + yr
						+ "qtr" + q + ";\n"
						+ "DROP PROCEDURE IF EXISTS tp_sales_to_scrub3" + yr
						+ "qtr" + q + ";\n"
						+ "DROP PROCEDURE IF EXISTS tp_sales_to_scrub4" + yr
						+ "qtr" + q + ";\n"
						+ "DROP PROCEDURE IF EXISTS tp_sales_to_scrub5" + yr
						+ "qtr" + q + ";\n"
						+ "DROP PROCEDURE IF EXISTS getSalesFromBlankRows" + yr
						+ "qtr" + q + ";\n");

		MysqlConnUtils.executeQuery(sb.toString());

	}

	public void dropTablesIfExists(String table) throws SQLException, FileNotFoundException {

		// TODO: ADD DROP PROCEDURES

		String yr = table.substring(table.indexOf("tp_raw") + 6,
				table.indexOf("tp_raw") + 10);
		String q = table.substring(table.indexOf("tp_raw") + 13,
				table.indexOf("tp_raw") + 14);

		StringBuffer sb = new StringBuffer(
				"drop table if exists tmpa_repair_p_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ " drop table if exists tmpa_repair_edt_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ " drop table if exists tmpa_mismatch_edt_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ " drop table if exists tmpa_mismatch_p_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ " drop table if exists tmpb_mismatch_edt_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ " drop table if exists tmpb_mismatch_p_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ " drop table if exists tmpb_repair_p_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ " drop table if exists tmpb_repair_edt_"
						+ yr
						+ "qtr"
						+ q
						+ "; \n"
						+ "drop table if exists tmp_"
						+ yr
						+ "Qtr"
						+ q
						+ "_updateHtml;\n"
						+ "\nDROP TABLE IF EXISTS tmp_ttlrows_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "\nDROP TABLE IF EXISTS tmp_getrows_p2edt_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "\nDROP TABLE IF EXISTS tmp2_getrows_p2edt_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "\ndrop table if exists TMP2_MISMATCH_EDT;\n"
						+ "\ndrop table if exists TMP2_MISMATCH_p;\n"
						+ "\ndrop table if exists TMP_MISMATCH_EDT;\n"
						+ "\ndrop table if exists TMP_MISMATCH_p;\n"
						+ "\ndrop table if exists tmp1_rpr_period;\n"
						+ "\ndrop table if exists tmp1_rpr_enddate;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp2_rpr_enddate_bd`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp2_tp_id_edt`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp3_rpr_enddate_bd`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp3_rpr_period_bd`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp4_rpr_enddate_bd`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp5_rpr_enddate_bd`;\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMP_MCNTD_"
						+ yr
						+ "QTR"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp2_rpr_period_bd`;\n"
						+ "\nDROP TABLE IF EXISTS tmpb_mismatch_edt_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "\nDROP TABLE IF EXISTS tmpb_mismatch_p_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "\nDROP TABLE IF EXISTS tmpb_repair_edt_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "\nDROP TABLE IF EXISTS tmpb_repair_p_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "\nDROP TABLE IF EXISTS tmpa_mismatch_edt_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "\nDROP TABLE IF EXISTS tmpa_mismatch_p_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "\nDROP TABLE IF EXISTS tmpa_repair_edt_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "\nDROP TABLE IF EXISTS tmpa_repair_p_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "\nDROP TABLE IF EXISTS TMP_"
						+ yr
						+ "qtr"
						+ q
						+ "_JOIN_ROWNAMES2;"
						+ "\nDROP TABLE IF EXISTS TMP_"
						+ yr
						+ "qtr"
						+ q
						+ "_JOIN_ROWNAMES;"
						+ "\nDROP TABLE IF EXISTS p3_bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ ";"
						+ "\nDROP TABLE IF EXISTS TMP_"
						+ yr
						+ "qtr"
						+ q
						+ "1_ROW_TROW;"
						+ "\nDROP TABLE IF EXISTS tmp_ciklength_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "\nDROP TABLE IF EXISTS TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "1;"
						+ "\nDROP TABLE IF EXISTS TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "2;"
						+ "\nDROP TABLE IF EXISTS TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "3;"
						+ "\nDROP TABLE IF EXISTS TMP_RENAME_"
						+ yr
						+ "_"
						+ q
						+ "4;"
						+ "\ndrop table if exists tmp_missing_accno2_"
						+ yr
						+ q
						+ ";"
						+ "\nDROP TABLE IF EXISTS tmp_filter;"
						+ "\nDROP TABLE IF EXISTS tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_min_cost_row;"
						+ "\nDROP TABLE IF EXISTS tmp_above_expense"
						+ yr
						+ "qtr"
						+ q
						+ ";\n DROP TABLE IF EXISTS tmp1_"
						+ yr
						+ "_"
						+ q
						+ "; \n"
						+ "DROP TABLE IF EXISTS tmp_tsshort_"
						+ yr
						+ "_"
						+ q
						+ "; \n"
						+ "DROP TABLE IF EXISTS  tmp_tps_"
						+ yr
						+ q
						+ "; \n"
						+ "DROP TABLE IF EXISTS  tmp_no_p3_"
						+ yr
						+ "_"
						+ q
						+ "; \n"
						+ "DROP TABLE IF EXISTS  tmp_no_p3_"
						+ yr
						+ q
						+ "_2; \n"
						+ "DROP TABLE IF EXISTS  tmp_mopattern_"
						+ yr
						+ "_qtr"
						+ q
						+ "_2; \n"
						+ "DROP TABLE IF EXISTS  tmp_mopattern_"
						+ yr
						+ "_qtr"
						+ q
						+ "; \n"
						+ "DROP TABLE IF EXISTS  tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_7; \n"
						+ "DROP TABLE IF EXISTS  tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_6; \n"
						+ "DROP TABLE IF EXISTS  tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_5; \n"
						+ "DROP TABLE IF EXISTS  tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_4; \n"
						+ "DROP TABLE IF EXISTS  tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_3; \n"
						+ "DROP TABLE IF EXISTS  tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_2; \n"
						+ "DROP TABLE IF EXISTS  tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "b; \n"
						+ "DROP TABLE IF EXISTS  tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "a; \n"
						+ "DROP TABLE IF EXISTS  tmp_"
						+ yr
						+ "_"
						+ q
						+ "_moday; \n"
						+ "DROP TABLE IF EXISTS  tmp_"
						+ yr
						+ q
						+ "; \n"
						+ "DROP TABLE IF EXISTS  tmp1_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS TMP_"
						+ yr
						+ "QTR"
						+ q
						+ "_WRONG_TN;\n"
						+ "DROP TABLE IF EXISTS TMP_"
						+ yr
						+ "QTR"
						+ q
						+ "_WRONG_TN2;\n"
						+ "DROP TABLE IF EXISTS TMP_"
						+ yr
						+ "QTR"
						+ q
						+ "_WRONG_TN3;\n"
						+ "DROP TABLE IF EXISTS TMP_"
						+ yr
						+ "QTR"
						+ q
						+ "_WRONG_TN4;\n"
						+ "DROP TABLE IF EXISTS tmp_"
						+ yr
						+ "_"
						+ q
						+ "_moday;\n"
						+ "DROP TABLE IF EXISTS tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_wrong_tn;\n"
						+ "DROP TABLE IF EXISTS tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_wrong_tn2;\n"
						+ "DROP TABLE IF EXISTS tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_wrong_tn3;\n"
						+ "DROP TABLE IF EXISTS tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_wrong_tn4;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "a;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "b;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_2;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_3;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_4;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_5;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_6;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_7;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "a;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "b;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_2;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_3;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_4;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_5;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_6;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_7;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "a;\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "b;\n"
						+ "DROP TABLE IF EXISTS tmp_calc_p_edt;\n"
						+ "DROP TABLE IF EXISTS tmp_calc_p_edt2;\n"
						+ "DROP TABLE IF EXISTS tmp_calc_p_edt3;\n"
						+ "DROP TABLE IF EXISTS tmp_calc_p_edt4;\n"
						+ "DROP TABLE IF EXISTS tmp_calc_p_edt5;\n"
						+ "DROP TABLE IF EXISTS tmp_calc_p_enddate;\n"
						+ "DROP TABLE IF EXISTS tmp_calc5_12_9;\n"
						+ "DROP TABLE IF EXISTS tmp_calc5_6_3;\n"
						+ "DROP TABLE IF EXISTS tmp_calc5_9_6;\n"
						+ "DROP TABLE IF EXISTS tmp_gap;\n"
						+ "DROP TABLE IF EXISTS tmp_gap1;\n"
						+ "DROP TABLE IF EXISTS tmp_gap2;\n"
						+ "DROP TABLE IF EXISTS tmp_getit"
						+ yr
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_its;\n"
						+ "DROP TABLE IF EXISTS tmp_missing_acc19623;\n"
						+ "DROP TABLE IF EXISTS tmp_missing_accno;\n"
						+ "DROP TABLE IF EXISTS tmp_mopattern_"
						+ yr
						+ "_qtr"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_mopattern_"
						+ yr
						+ "_qtr"
						+ q
						+ "_2;\n"
						+ "DROP TABLE IF EXISTS tmp_mopattern_"
						+ yr
						+ "_qtr"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_mopattern_"
						+ yr
						+ "_qtr"
						+ q
						+ "_2;\n"
						+ "DROP TABLE IF EXISTS tmp_mopattern_"
						+ yr
						+ "_qtr"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_mopattern_"
						+ yr
						+ "_qtr"
						+ q
						+ "_2;\n"
						+ "DROP TABLE IF EXISTS tmp_no_p3_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_no_p3_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_no_p3_"
						+ yr
						+ "_"
						+ q
						+ "_2;\n"
						+ "DROP TABLE IF EXISTS tmp_p3_calc;\n"
						+ "DROP TABLE IF EXISTS tmp_p3_calc_2;\n"
						+ "DROP TABLE IF EXISTS tmp_p3_calc_edt;\n"
						+ "DROP TABLE IF EXISTS tmp_p3_calc_edt_2;\n"
						+ "DROP TABLE IF EXISTS tmp_p3_calc3;\n"
						+ "DROP TABLE IF EXISTS tmp_q_cik;\n"
						+ "DROP TABLE IF EXISTS tmp_q_cik_to_fix;\n"
						+ "DROP TABLE IF EXISTS tmp_qd_cik;\n"
						+ "DROP TABLE IF EXISTS tmp_scr;\n"
						+ "DROP TABLE IF EXISTS tmp_scrub;\n"
						+ "DROP TABLE IF EXISTS tmp_start1_scrub;\n"
						+ "DROP TABLE IF EXISTS tmp_tp_q_sales;\n"
						+ "DROP TABLE IF EXISTS tmp_tp_q_sales2;\n"
						+ "DROP TABLE IF EXISTS tmp_tp_q_sales3;\n"
						+ "DROP TABLE IF EXISTS tmp_tp_raw"
						+ yr
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_tp_sales;\n"
						+ "DROP TABLE IF EXISTS tmp_tp_sales_max_edtp3;\n"
						+ "DROP TABLE IF EXISTS tmp_tp_sales_p3_remove;\n"
						+ "DROP TABLE IF EXISTS tmp_tp_sales_to_scrub"
						+ yr
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_tp_sales_to_scrub_two_tno_a;\n"
						+ "DROP TABLE IF EXISTS tmp_tp_sales_to_scrub_two_tno2_a;\n"
						+ "DROP TABLE IF EXISTS tmp_tp_sales_to_scrub2;\n"
						+ "DROP TABLE IF EXISTS tmp_tps_"
						+ yr
						+ "1;\n"
						+ "DROP TABLE IF EXISTS tmp_tradingsymbol;\n"
						+ "DROP TABLE IF EXISTS tmp_tsshort_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp1;\n"
						+ "DROP TABLE IF EXISTS tmp1_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp1_msg;\n"
						+ "DROP TABLE IF EXISTS tmp1919;\n"
						+ "DROP TABLE IF EXISTS tmp_"
						+ yr
						+ "_"
						+ q
						+ "_moday;\n"
						+ "DROP TABLE IF EXISTS tmp_"
						+ yr
						+ "_"
						+ q
						+ "_p2_allcol;\n"
						+ "DROP TABLE IF EXISTS tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_wrong_tn;\n"
						+ "DROP TABLE IF EXISTS tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_wrong_tn2;\n"
						+ "DROP TABLE IF EXISTS tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_wrong_tn3;\n"
						+ "DROP TABLE IF EXISTS tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_wrong_tn4;\n"
						+ "DROP TABLE IF EXISTS tmp_"
						+ yr
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_"
						+ yr
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_blankrow_"
						+ yr
						+ "qtr"
						+ q
						+ "_6a;\n"
						+ "DROP TABLE IF EXISTS tmp_no_p3_"
						+ yr
						+ q
						+ "_2;\n"
						+ "DROP TABLE IF EXISTS tmp_tps_"
						+ yr
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_tsshort_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp1_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_above_expense_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp;\n"
						+ "DROP TABLE IF EXISTS TMP_MULTIPE_CIKs;\n"
						+ "DROP TABLE IF EXISTS TMP_MULTIPE_CIKs1;\n"
						+ "DROP TABLE IF EXISTS TMP_MULTIPE_CIKs2;\n"
						+ "DROP TABLE IF EXISTS TMP_MULTIPE_CIKs3;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`bac_tp_raw_tmp`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`p3_tmp1919`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp2_msg`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp3_msg`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmpdate`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_fix_filings`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_fix_same_filing3`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_fix_same_filing_two_accno`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_fix_same_filing_two_accno2`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_fix_same_filing_two_accno3`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_fix_same_filing_two_accno4`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_fix_same_filing_two_accno5`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_gap_adj`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_int_inc_"
						+ yr
						+ "qtr"
						+ q
						+ "`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_multiple_ciks3`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_qend`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_qend1`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_qend2`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_qend3`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_qend3_1`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_qend3_2`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_qend4`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_qend_ttl_cnt`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_q_sales`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_q_sales2`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_q_sales3`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_q_sales4`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_q_sales4a`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_q_sales5`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_q_sales6`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_q_sales6_a`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_sales`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_sales_max_edtp3`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_sales_p12`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_sales_p9`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_sales_yearlyg`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_sales_yearlyg2`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_tpidx`;\n"
						+ "DROP TABLE IF EXISTS TMP_MANY_CIKS_2;\n"
						+ "DROP TABLE IF EXISTS TMP_MANY_CIKS_3;\n"
						+ "DROP TABLE IF EXISTS TMP_MANY_CIKS;\n"
						+ "DROP TABLE IF EXISTS TMP_SAME_EDT"
						+ yr
						+ "qtr"
						+ q
						+ ";\n\n"
						+ "DROP TABLE IF EXISTS TMP_SAME_EDT2"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS TMP_SAME_EDT_VAL_DIF_P"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "\nDROP TABLE IF EXISTS  `stockanalyser`.`tmp_fiscal_yrqtr_"
						+ yr
						+ "qtr"
						+ q
						+ "`;\n"
						+ "DROP TABLE IF EXISTS  `stockanalyser`.`tmp_fiscal_yrqtr_"
						+ yr
						+ "qtr"
						+ q
						+ "_2`;\n"
						+ "DROP TABLE IF EXISTS  `stockanalyser`.`tmp_fiscal_yrqtr_"
						+ yr
						+ "qtr"
						+ q
						+ "_3`;\n"
						+ "drop table if exists tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_min_cost_row2;\n"
						+ "drop table if exists tmp_get_edt_from_another_table_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "drop table if exists tmp_sum100_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_qend`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_qend1`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_qend2`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_qend3`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_qend3_1`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_qend3_2`;\n"
						+ "DROP TABLE IF EXISTS `stockanalyser`.`tmp_qend4`;\n"
						+ "DROP TABLE IF EXISTS tmp1"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp"
						+ yr
						+ "qtr"
						+ q
						+ "_get_rowname_for_net;\n"
						+ "DROP TABLE IF EXISTS tmp"
						+ yr
						+ "qtr"
						+ q
						+ "_get_rowname_for_net2;\n"
						+ "DROP TABLE IF EXISTS tmp"
						+ yr
						+ "qtr"
						+ q
						+ "_get_rowname_for_ttl;\n"
						+ "DROP TABLE IF EXISTS tmp"
						+ yr
						+ "qtr"
						+ q
						+ "_get_rowname_for_ttl2;\n"
						+ "DROP TABLE IF EXISTS tmp"
						+ yr
						+ "qtr"
						+ q
						+ "_get_rowname_for_ttl3;\n"
						+ "DROP TABLE IF EXISTS tmp"
						+ yr
						+ "qtr"
						+ q
						+ "_get_rowname_for_ttl4;\n"
						+ "DROP TABLE IF EXISTS tmp"
						+ yr
						+ "qtr"
						+ q
						+ "_get_rowname_for_ttl5;\n"
						+ "DROP TABLE IF EXISTS tmp"
						+ yr
						+ "qtr"
						+ q
						+ "_get_rowname_mntr;\n"
						+ "DROP TABLE IF EXISTS tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_row_trow;\n"
						+ "DROP TABLE IF EXISTS tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_sub;\n"
						+ "DROP TABLE IF EXISTS tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_row_trow;\n"
						+ "DROP TABLE IF EXISTS tmp_addcolumns_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_same_filing_"
						+ yr
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_same_filings2_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_same_filings_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_sumallrowsbycol_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmpgetedtfromalltbls"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmpgetedtfromothertables_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "drop table if exists tmp_"
						+ yr
						+ "_"
						+ q
						+ "_moday;\n"
						+ "drop table if exists tmp_"
						+ yr
						+ "_"
						+ q
						+ "_p2_allcol;\n"
						+ "drop table if exists tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_join_rownames;\n"
						+ "drop table if exists tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_join_rownames2;\n"
						+ "drop table if exists tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_row_trow;\n"
						+ "drop table if exists tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_updatehtml;\n"
						+ "drop table if exists tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_wrong_tn;\n"
						+ "drop table if exists tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_wrong_tn2;\n"
						+ "drop table if exists tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_wrong_tn3;\n"
						+ "drop table if exists tmp_"
						+ yr
						+ "qtr"
						+ q
						+ "_wrong_tn4;\n"
						+ "drop table if exists tmp_"
						+ yr
						+ q
						+ ";\n"
						+ "drop table if exists tmp_mismatch_edt_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "drop table if exists tmp_mismatch_p_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "drop table if exists tmp_net_net_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "drop table if exists tmp_rename_"
						+ yr
						+ "_"
						+ q
						+ "1;\n"
						+ "drop table if exists tmp_rename_"
						+ yr
						+ "_"
						+ q
						+ "2;\n"
						+ "drop table if exists tmp_rename_"
						+ yr
						+ "_"
						+ q
						+ "3;\n"
						+ "drop table if exists tmp_rename_"
						+ yr
						+ "_"
						+ q
						+ "4;\n"
						+ "drop table if exists tmp_repair_edt_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "drop table if exists tmp_repair_p_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "drop table if exists tmp_same_edt"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "drop table if exists tmp_same_edt2"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "drop table if exists tmp_sum100_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "drop table if exists tmp_to_from_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "drop table if exists tmp_tsshort_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "drop table if exists tmp_ttl_net_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "drop table if exists tmp1"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "drop table if exists tmp1_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "drop table if exists tmp2_net_net_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "drop table if exists tmp2_ttl_net_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "drop table if exists tmp4_net_net_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "drop table if exists tmp4_ttl_net_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "drop table if exists tmp5_net_net_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "drop table if exists tmp5_ttl_net_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "drop table if exists tmpgetedtfromalltbls"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "drop table if exists tmp_getedt_onemotwoyr_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp2_"
						+ yr
						+ "_"
						+ q
						+ "_p2_allcol;\n"
						+ "DROP TABLE IF EXISTS tmp2_allcol_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_acctnocol"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_allcol_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_edt_p_yr_ts_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_mis_ts_"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_missing_accno_"
						+ yr
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_prep_tp_id"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_upd_edt_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_upd_p2_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmp_wrong_month_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tmpa_getedt_onemotwoyr_"
						+ yr
						+ "qtr"
						+ q
						+ "; \n"
						+ "DROP TABLE IF EXISTS fixBlankRownames"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS fixTableSentenceEnddates"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS getAndmarkAllBadEndDatesAndPeriodsAcrossYears"
						+ yr
						+ "QTR"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS getMissingEndDatesAcrossYears"
						+ yr
						+ "QTR"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS markBadTblsAndRepairEdtPer"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS prep_tp_Id"
						+ yr
						+ "QTR"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS updateP2FromColumnPattern"
						+ yr
						+ "QTR"
						+ q
						+ ";\n"
						+ "DROP TABLE IF EXISTS tp_sales_to_scrubDeleteDuplicates;\n"
						+ "DROP TABLE IF EXISTS TMP_" + yr + "QTR" + q + ";\n"
						+ "DROP TABLE IF EXISTS TMP_JN_ROWNAMES_" + yr + "QTR"
						+ q + ";\n"
						+ "DROP TABLE IF EXISTS prepTP_ALL_IS_Tables" + yr
						+ "QTR" + q + ";\n");

		String createProc = "DROP PROCEDURE IF EXISTS dropTablesIfExists" + yr
				+ "QTR" + q + ";\n" + "CREATE PROCEDURE dropTablesIfExists"
				+ yr + "QTR" + q + "()\n\n begin\n\n";
		String endProc = "\n\nend;";
		String dropProc = "\rDROP PROCEDURE IF EXISTS dropTablesIfExists" + yr
				+ "QTR" + q + ";\r\r";

		MysqlConnUtils.executeQuery(createProc + sb.toString() + endProc);
		sb.delete(0, sb.toString().length());
		MysqlConnUtils.executeQuery("call dropTablesIfExists" + yr + "QTR" + q
				+ "();\n" + dropProc);

		sb.delete(0, sb.toString().length());
	}

	public void addRevisedColumn(String tp_sales_to_scrub,
			String tp_sales_to_scrub2) throws SQLException, FileNotFoundException {
		String query = "\nINSERT IGNORE INTO "
				+ tp_sales_to_scrub
				+ "\n"
				+ "select * from tp_sales_to_scrub_hold;\n\n"
				+ "INSERT IGNORE INTO "
				+ tp_sales_to_scrub2
				+ "\n"
				/*
				 * below xx to xx can be uncommented after commenting out select
				 * block below this once bac_tp_raw is populated
				 */

				+ "select t1.*,case when \n"
				+ "(allColText like '%revise%' or t1.ColumnText like '%revise%' or ColumnPattern like '%revise%'\n"
				+ "or allColText like '%restate%' or t1.ColumnText like '%restate%' or"
				+ " ColumnPattern like '%restate%'\n"
				+ "or allColText like '%adjust%' or t1.ColumnText like '%adjust%' "
				+ "or ColumnPattern like '%adjust%') then 'restated'\n"
				+ "when (htmltxt='html' and t1.ColumnText like '%pro%forma%')"
				+ " or ((allColText like '%pro%forma%' or ColumnPattern like '%pro%forma%' or\n"
				+ "t1.ColumnText like '%pro%forma%') and htmlTxt!='html') then 'pro forma'\n"
				+ "else 'actual' end revised \n"

				// + "select t1.*,case when \n"
				// + "(t1.ColumnText like '%revise%' \n"
				// + "or t1.ColumnText like '%restate%'"
				// + "or t1.ColumnText like '%adjust%' ) then 'restated'\n"
				// + "when (t1.ColumnText like '%pro%forma%')" +
				// " then 'pro forma'\n"
				// + "else 'actual' end revised \n"
				+ "from "
				+ tp_sales_to_scrub
				+ " t1"
				+ " inner join BAC_TP_RAW"
				+ " t2 on t1.accno=t2.accno and t1.filedate=t2.filedate and t1.tno=t2.tno and t1.row=t2.row"
				+ ";\n" + "\n";

		String dropProc = "DROP PROCEDURE IF EXISTS addRevisedColumn;\n"
				+ "CREATE PROCEDURE addRevisedColumn()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + query + endProc);
		MysqlConnUtils.executeQuery("call addRevisedColumn();\n");
		query = "";

	}

	public static void missingAccnos(String table) throws SQLException, FileNotFoundException {

		String yr = table.substring(table.indexOf("tp_raw") + 6,
				table.indexOf("tp_raw") + 10);
		String q = table.substring(table.indexOf("tp_raw") + 13,
				table.indexOf("tp_raw") + 14);

		String query = "DROP TABLE IF EXISTS TMP1_msg"
				+ yr
				+ "_"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP1_msgTMP1"
				+ yr
				+ "qtr"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "select t1.cik,t1.`Company Name`,t1.`Date Filed`,t1.`form type`, @acc:=left(right(filename,25),20) acc ,\n"
				+ "concat('https://www.sec.gov/Archives/edgar/data/',cik,'/',@acc,'-index.htm') link\n"
				+ "from tpidx t1 where year(`date filed`)="
				+ yr
				+ " and quarter(t1.`Date Filed`)="
				+ q
				+ "\n"
				+ "and (`Form Type` rlike '10-q' or t1.`Form Type` rlike '10-k')  and\n"
				+ "(`COMPANY NAME` NOT RLIKE '[1-2]{1}[0-9]{1}[0-9]{2}|SERIES|SECURITIZAT|FUND|receivabl|abs|mbs|backed|mortg|master|special' "
				+ "or (`COMPANY NAME` RLIKE 'trust' and `COMPANY NAME` rlike 'bank|company' ));\n"
				+ "ALTER TABLE TMP1_msg"
				+ yr
				+ "_"
				+ q
				+ " ADD KEY(ACC);\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP2_msg"
				+ yr
				+ "_"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP2_msg"
				+ yr
				+ "_"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "select accno from "
				+ table
				+ "\n"
				+ "group by accno;\n"
				+ "ALTER TABLE TMP2_msg"
				+ yr
				+ "_"
				+ q
				+ " ADD KEY(ACCNO);\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP3_msg"
				+ yr
				+ "_"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP3_msg"
				+ yr
				+ "_"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "select t2.accno, t1.cik,t1.`Company Name`,t1.`Date Filed`,acc,link\n"
				+ "from tmp1_msg"
				+ yr
				+ "_"
				+ q
				+ " t1 left join tmp2_msg"
				+ yr
				+ "_"
				+ q
				+ " t2 on\n"
				+ "acc=t2.accno where t2.accno is null order by t1.`Date Filed`;\n"
				+ "\n"
				+ "insert ignore into MSG_ACCNOs\n"
				+ "select acc,CIK, `Company Name` Company, `Date Filed` FILEDATE, link from TMP3_msg"
				+ yr + "_" + q + " group by acc;\n";

		String dropProc = "DROP PROCEDURE IF EXISTS missingAccnos;\n"
				+ "CREATE PROCEDURE missingAccnos()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + query + endProc);
		MysqlConnUtils.executeQuery("call missingAccnos();\n");

	}

	public void tp_p3Prep(String table, String tp_sales_to_scrub)
			throws SQLException, FileNotFoundException {

		String yr = table.substring(table.indexOf("tp_raw") + 6,
				table.indexOf("tp_raw") + 10);
		String q = table.substring(table.indexOf("tp_raw") + 13,
				table.indexOf("tp_raw") + 14);

		String query = "DROP TABLE IF EXISTS TMP_NO_P3_"
				+ yr
				+ "_"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP_NO_P3_"
				+ yr
				+ "_"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "select ACCNO from "
				+ tp_sales_to_scrub
				+ " where p2=3\n"
				+ "and YEAR(FILEDATE)="
				+ yr
				+ " AND QUARTER(FILEDATE)= "
				+ q
				+ "\n"
				+ "GROUP BY ACCNO;\n"
				+ "ALTER TABLE TMP_NO_P3_"
				+ yr
				+ "_"
				+ q
				+ " ADD KEY(ACCNO);\n"
				+ "/*THIS WILL FIND FILINGS THAT HAVE A SEPARATE TABLE FOR 3 MONTH PERIOD VALUES AND AS A RESULT AREN'T INITIALLY CAPTURED IN TP_SALES_TO_SCRUB.\n"
				+ "IT CHECKS TO SEE WHAT ACCNOs ARE NOT IN TP_SALES_TO_SCRUB THAT DOES NOT THAT HAVE P3 VALUE. INITIAL QUERY I RUN PICKS-UP HIGHEST VALUE WHICH INHERENTLY \n"
				+ "RELATES TO 12, 9, AND 6 MONTH PERIODS*/\n"
				+ "/*FIND ALL ACCNOs WHERE THERE IS A PERIOD=3 IN TP_SALES_TO_SCRUB*/\n"
				+ "/*INSERT ALL P3 VALUES FROM BAC_TP TABLE.*/\n"
				+ "INSERT IGNORE INTO TMP_NO_P3_"
				+ yr
				+ "_"
				+ q
				+ "\n"
				+ "SELECT ACCNO FROM TMP_ABOVE_EXPENSE_"
				+ yr
				+ "qtr"
				+ q
				+ " WHERE TN='IS' AND P2=3 GROUP BY ACCNO;\n"
				+ "\n"
				+ "/*THOSE WITH ONLY 1 P3 ARE THE ACCNO WHERE NO P3 WAS FOUND. THESE ARE THE ACCNOs TO LOOK JUST FOR P3 MAX VALUE*/\n"
				+ "\n" + "DROP TABLE IF EXISTS TMP_NO_P3_" + yr + q + "_2;\n"
				+ "CREATE TABLE TMP_NO_P3_" + yr + q + "_2 ENGINE=MYISAM\n"
				+ "SELECT * FROM (\n"
				+ "SELECT COUNT(*) C,T1.* FROM TMP_NO_P3_" + yr + "_" + q
				+ " T1 GROUP BY ACCNO ) T1 WHERE C=1;\n"
				+ "ALTER TABLE TMP_NO_P3_" + yr + q + "_2 ADD KEY(ACCNO);\n"
				+ "\n" + "DROP TABLE IF EXISTS P3_" + table + ";\n"
				+ "CREATE TABLE P3_" + table + " ENGINE=MYISAM\n"
				+ "SELECT T1.* FROM TMP_ABOVE_EXPENSE_" + yr + "qtr" + q
				+ " T1 INNER JOIN TMP_NO_P3_" + yr + q
				+ "_2 T2 ON T1.ACCNO=T2.ACCNO\n"
				+ "WHERE TN='IS' and (p2=3 or col=0);\n" + "\n"
				+ "ALTER TABLE P3_" + table + " ADD KEY (edt2), ADD\n"
				+ "  KEY `per2` (`p2`), ADD\n"
				+ "  KEY `AccNo` (`AccNo`),ADD\n"
				+ "  KEY `fileDate` (`fileDate`),ADD \n"
				+ "  KEY `tNo` (`tNo`),ADD\n" + "  KEY `row` (`row`),ADD\n"
				+ "  KEY `rowname` (`rowName`),ADD\n"
				+ "  KEY `tn` (`tn`),ADD\n" + "  KEY `value` (`value`),ADD\n"
				+ "  KEY `cik` (`cik`),ADD\n" + "  KEY `col` (`col`),ADD\n"
				+ "  KEY `totcol` (`tc`),ADD\n"
				+ "  KEY `columnPattern` (`ColumnPattern`),ADD\n"
				+ "  KEY `tRow` (`tRow`);\n";

		String dropProc = "DROP PROCEDURE IF EXISTS tp_p3Prep" + yr + "_" + q
				+ ";\n" + "CREATE PROCEDURE tp_p3Prep" + yr + "_" + q
				+ "()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + query + endProc);
		MysqlConnUtils.executeQuery("call tp_p3Prep" + yr + "_" + q + "();\n");
		query = "";

	}

	public void tp_Sales_fixEndDates(String table, boolean isBacTPRaw)
			throws SQLException, FileNotFoundException {

		String yr = table.substring(table.indexOf("tp_raw") + 6,
				table.indexOf("tp_raw") + 10);
		String q = table.substring(table.indexOf("tp_raw") + 13,
				table.indexOf("tp_raw") + 14);

		StringBuffer sb = new StringBuffer();
		/*
		 * sb.append( "update ignore " + table + " set p2= \n" + "case \n" +
		 * "when columntext rlike '(january|jan[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' \n"
		 * +
		 * "and columntext rlike '(march|mar[\\\\.]{0,1})[ ]{1,2}[23]{1}[0-9]{1}[, ]{1}|(april|apr[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' then 3\n"
		 * + "\n" +
		 * "when columntext rlike '(february|feb[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' \n"
		 * +
		 * "and columntext rlike '(april|apr[\\\\.]{0,1})[ ]{1,2}[23]{1}[0-9]{1}[, ]{1}|(may)[ ]{1,2}[0-9]{1}[, ]{1}' then 3\n"
		 * + "\n" +
		 * "when columntext rlike '(march|mar[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' \n"
		 * +
		 * "and columntext rlike '(may)[ ]{1,2}[23]{1}[0-9]{1}[, ]{1}|(june|jun[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' then 3\n"
		 * + "\n" +
		 * "when columntext rlike '(april|apr[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' \n"
		 * +
		 * "and columntext rlike '(june|jun[\\\\.]{0,1})[ ]{1,2}[23]{1}[0-9]{1}[, ]{1}|(july|jul[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' then 3\n"
		 * + "\n" + "when columntext rlike '(may)[ ]{1,2}[0-9]{1}[, ]{1}' \n" +
		 * "and columntext rlike '(july|jul[\\\\.]{0,1})[ ]{1,2}[23]{1}[0-9]{1}[, ]{1}|(august|aug[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' then 3\n"
		 * + "\n" +
		 * "when columntext rlike '(june|jun[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' \n"
		 * +
		 * "and columntext rlike '(august|aug[\\\\.]{0,1})[ ]{1,2}[23]{1}[0-9]{1}[, ]{1}|(september|sep[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' then 3\n"
		 * + "\n" +
		 * "when columntext rlike '(july|jul[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}|(june|jun[\\\\.]{0,1})[ ]{1,2}[23]{1}[0-9]{1}[, ]{1}' \n"
		 * +
		 * "and columntext rlike '(september|sep[\\\\.]{0,1})[ ]{1,2}[23]{1}[0-9]{1}[, ]{1}|(october|oct[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' then 3\n"
		 * + "\n" +
		 * "when columntext rlike '(august|aug[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' \n"
		 * +
		 * "and columntext rlike '(october|oct[\\\\.]{0,1})[ ]{1,2}[23]{1}[0-9]{1}[, ]{1}|(november|nov[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' then 3\n"
		 * + "\n" +
		 * "when columntext rlike '(september|sep[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' \n"
		 * +
		 * "and columntext rlike '(november|nov[\\\\.]{0,1})[ ]{1,2}[23]{1}[0-9]{1}[, ]{1}|(december|dec[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' then 3\n"
		 * + "\n" +
		 * "when columntext rlike '(october|oct[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' \n"
		 * +
		 * "and columntext rlike '(december|dec[\\\\.]{0,1})[ ]{1,2}[23]{1}[0-9]{1}[, ]{1}|(january|jan[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' then 3\n"
		 * + "\n" +
		 * "when columntext rlike '(november|nov[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' \n"
		 * +
		 * "and columntext rlike '(january|jan[\\\\.]{0,1})[ ]{1,2}[23]{1}[0-9]{1}[, ]{1}|(february|feb[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' then 3\n"
		 * + "\n" +
		 * "when columntext rlike '(december|dec[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' \n"
		 * +
		 * "and columntext rlike '(february|feb[\\\\.]{0,1})[ ]{1,2}[23]{1}[0-9]{1}[, ]{1}|(march|mar[\\\\.]{0,1})[ ]{1,2}[0-9]{1}[, ]{1}' then 3\n"
		 * + "else p2\n" + "end\n");
		 * 
		 * if (isBacTPRaw) { sb.append(",mo='P2',yr='to'\n"); }
		 * 
		 * sb.append(
		 * "where columntext rlike 'from| to|to |through| [0-9]{4} - [A-Z]{3}' \n"
		 * + "and p2!=3 and tn not rlike 'bs|se'\n" + "and\n" + "(\n" +
		 * "(columntext rlike 'jan' and columntext rlike 'mar|apr')  or\n" +
		 * " (columntext rlike 'feb' and columntext rlike 'apr|may' )  or\n" +
		 * " (columntext rlike 'marc' and columntext rlike 'may |jun' )  or\n" +
		 * " (columntext rlike 'apr' and columntext rlike 'jun|jul' )  or \n" +
		 * " (columntext rlike 'may ' and columntext rlike 'jul|aug' )  or \n" +
		 * " (columntext rlike 'jun' and columntext rlike 'aug|sep' )  or \n" +
		 * " (columntext rlike 'jul' and columntext rlike 'sep|oct' )  or \n" +
		 * " (columntext rlike 'aug' and columntext rlike 'oct|nov' )  or \n" +
		 * " (columntext rlike 'sep' and columntext rlike 'nov|dec' )  or \n" +
		 * " (columntext rlike 'oct' and columntext rlike 'dec|jan' )  or \n" +
		 * " (columntext rlike 'nov' and columntext rlike 'jan|feb' )  or\n" +
		 * "  (columntext rlike 'dec' and columntext rlike 'feb|marc' )  );\n");
		 */

		if (isBacTPRaw) {

			sb.append("\nDROP TABLE IF EXISTS TMP_WRONG_MONTH_"
					+ yr
					+ "QTR"
					+ q
					+ ";\n"
					+ "CREATE TABLE TMP_WRONG_MONTH_"
					+ yr
					+ "QTR"
					+ q
					+ " ENGINE=MYISAM\n"
					+ "SELECT ACCNO,TNO,ROW,COLUMNTEXT FROM \n"
					+ "bac_tp_raw"
					+ yr
					+ "qtr"
					+ q
					+ " WHERE columntext not rlike \n"
					+ "'quarter| mo| week| wk| to date|statement|year' AND yr!='to' and yr!='bad';\n"
					+ "ALTER TABLE TMP_WRONG_MONTH_"
					+ yr
					+ "QTR"
					+ q
					+ " ADD KEY(ACCNO),ADD KEY(TNO), ADD KEY(ROW),ADD KEY(COLUMNTEXT);\n"
					+ "\n"
					+ "UPDATE bac_tp_raw"
					+ yr
					+ "qtr"
					+ q
					+ " T1 INNER JOIN TMP_WRONG_MONTH_"
					+ yr
					+ "QTR"
					+ q
					+ " T2\n"
					+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.ROW=T2.ROW\n"
					+ "set yr='bad'\n"
					+ "where T1.columntext rlike \n"
					+ "'(jan|feb|march|apr|may |jun|jul|aug|sep|oct|nov|dec).{1,35}(jan|feb|march|apr|may |jun|jul|aug|sep|oct|nov|dec)';\n"
					+ "DROP TABLE IF EXISTS TMP_WRONG_MONTH_" + yr + "QTR" + q
					+ ";\n\n");

		}

		/*
		 * sb.append("UPDATE IGNORE " + table + " \n" + "set edt2=case \n" +
		 * "when ( right(left(edt2,7),2)='02' ) \n" +
		 * "and (columntext rlike '3[01]{1} | 3[01]{1}' \n" +
		 * "or (trim(columntext) rlike '2[0-9]{1} | 2[0-9]{1}' and columntext not rlike '2[0-9]{1}.{1,5}week')\n"
		 * +
		 * "or (columntext rlike ' 2[0-9]{1},' and columntext rlike '2[0-9]{1}.{1,5}week')\n"
		 * + "or columntext rlike '( |\\\\.)[23]{1}[0-9]{1},')\n" +
		 * "then concat(left(edt2,8),'28') \n" +
		 * "when ( right(left(edt2,7),2)!='02') \n" +
		 * "and (columntext rlike '3[01]{1} | 3[01]{1}' \n" +
		 * "or (trim(columntext) rlike '2[0-9]{1} | 2[0-9]{1}' and columntext not rlike '2[0-9]{1}.{1,5}week')\n"
		 * +
		 * "or (columntext rlike ' 2[0-9]{1},' and columntext rlike '2[0-9]{1}.{1,5}week')\n"
		 * + "or columntext rlike '( |\\\\.)[23]{1}[0-9]{1},')\n" +
		 * "then concat(left(edt2,8),'30') \n" +
		 * "when columntext rlike '( |\\\\.)[1-4]{1},'\n" +
		 * "then concat(left(edt2,8),'04') \n" +
		 * "when columntext rlike '( |\\\\.)[5-9]{1},'\n" +
		 * "then concat(left(edt2,8),'09') \n" +
		 * "when right(edt2,2) rlike '[a-z]' then concat(left(edt2,8),'15')\n" +
		 * "else edt2 end \n" +
		 * "where  (columnText not rlike '15' and right(edt2,2)=15) or (edt2 rlike '[a-z]');\n"
		 * );
		 * 
		 * sb.append("update ignore " + table +
		 * " set edt2=concat(left(edt2,2),left(right(edt2,5),2),'-',right(edt2,2),'-',right(left(edt2,4),2)) "
		 * +
		 * "where columntext rlike '[12]{1}[09]{1}[019]{1}[0-9]{1}-(0[0-9]{1}|1[0-2]{1})-[0-9]{2}' and edt2 not rlike  "
		 * +
		 * "'[12]{1}[09]{1}[0-9]{1}[0-9]{1}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})';\n "
		 * );
		 */
		sb.append("update ignore "
				+ table
				+ " \n"
				+ "set edt2= case \n"
				+ "when columntext rlike ' 2000|2000 ' then concat('2000',right(edt2,6))\n"
				+ "when columntext rlike ' 2001|2001 ' then concat('2001',right(edt2,6))\n"
				+ "when columntext rlike ' 2002|2002 ' then concat('2002',right(edt2,6))\n"
				+ "when columntext rlike ' 2003|2003 ' then concat('2003',right(edt2,6))\n"
				+ "when columntext rlike ' 2004|2004 ' then concat('2004',right(edt2,6))\n"
				+ "when columntext rlike ' 2005|2005 ' then concat('2005',right(edt2,6))\n"
				+ "when columntext rlike ' 2006|2006 ' then concat('2006',right(edt2,6))\n"
				+ "when columntext rlike ' 2007|2007 ' then concat('2007',right(edt2,6))\n"
				+ "when columntext rlike ' 2008|2008 ' then concat('2008',right(edt2,6))\n"
				+ "when columntext rlike ' 2009|2008 ' then concat('2009',right(edt2,6))\n"
				+ "else edt2 end \n" + "where edt2 rlike '^00|,';\n" + "\n"
				+ "delete from " + table + " where edt2 rlike '^00|,'\n"
				+ "or columntext rlike '%|change';\n");

		sb.append("\nUPDATE IGNORE  " + table + " set p2=0 ");

		if (isBacTPRaw) {
			sb.append(",mo='p2',yr='bad'\n");
		}

		sb.append(" WHERE (COLUMNTEXT RLIKE 'NINE.{1,3}WEEK' AND COLUMNTEXT NOT RLIKE 'THIRTY') OR\n"
				+ " (COLUMNTEXT RLIKE 'SIX.{1,3}WEEK' AND COLUMNTEXT NOT RLIKE 'THIRTY|TWENTY')"
				+ " or (COLUMNTEXT not RLIKE 'hundred' and columntext rlike '^ONE | ONE ');\n");

		sb.append("\nUPDATE IGNORE  "
				+ table
				+ " set p2=case \n"
				+ "when columntext rlike 'one.{1,2}hundred' and columntext rlike 'twenty|fifty|forty|thirty' then 3\n"
				+ "when columntext rlike 'three.{1,2}hundred' and columntext rlike 'fifty|sixty|forty' then 12\n"
				+ "when columntext rlike 'three.{1,2}hundred' then 0\n"
				+ "else p2 end \n");
		if (isBacTPRaw) {
			sb.append(",mo='p2'\n");
		}
		sb.append(" where columntext rlike 'hundred'; \n");

		/*
		 * sb.append("UPDATE IGNORE \n" + table + "\n" + "set edt2=case \n" +
		 * "when ( right(left(edt2,7),2)='02' ) \n" +
		 * "and (trim(columntext) rlike '3[01]{1} | 3[01]{1}' \n" +
		 * "or (columntext rlike '2[0-9]{1} | 2[0-9]{1}' and columntext not rlike '2[0-9]{1}.{1,5}week')\n"
		 * +
		 * "or (columntext rlike ' 2[0-9]{1},' and columntext rlike '2[0-9]{1}.{1,5}week')\n"
		 * + "or columntext rlike '( |\\\\.)[23]{1}[0-9]{1},')\n" +
		 * "then concat(left(edt2,8),'28') \n" +
		 * "when ( right(left(edt2,7),2)!='02') \n" +
		 * "and (columntext rlike '3[01]{1} | 3[01]{1}' \n" +
		 * "or (trim(columntext) rlike '2[0-9]{1} | 2[0-9]{1}' and columntext not rlike '2[0-9]{1}.{1,5}week')\n"
		 * +
		 * "or (columntext rlike ' 2[0-9]{1},' and columntext rlike '2[0-9]{1}.{1,5}week')\n"
		 * + "or columntext rlike '( |\\\\.)[23]{1}[0-9]{1},')\n" +
		 * "then concat(left(edt2,8),'30') \n" +
		 * "when columntext rlike '( |\\\\.)[1-4]{1},'\n" +
		 * "then concat(left(edt2,8),'04') \n" +
		 * "when columntext rlike '( |\\\\.)[5-9]{1},'\n" +
		 * "then concat(left(edt2,8),'09') \n" +
		 * "when right(edt2,2) rlike '[a-z]|00' then concat(left(edt2,8),'15')\n"
		 * +
		 * "when length(edt2)=10 and right(edt2,3) rlike '- [0-9]{1}' then concat(left(edt2,8),0,right(edt2,1)) \n"
		 * + "else edt2 end \n" + "where edt2 not rlike \n" +
		 * " '[12]{1}[09]{1}[0-9]{1}[0-9]{1}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})'\n"
		 * + " and left(edt2,4) rlike '[12]{1}[09]{1}[0-9]{2}';\n");
		 * 
		 * sb.append("UPDATE IGNORE " + table + " set edt2=case \n" +
		 * "when year(edt2)=2099 then concat('1999-',right(edt2,5))\n" +
		 * "when year(edt2)=2098 then concat('1998-',right(edt2,5))\n" +
		 * "when year(edt2)=2097 then concat('1997-',right(edt2,5))\n" +
		 * "when year(edt2)=2096 then concat('1996-',right(edt2,5))\n" +
		 * "when year(edt2)=2095 then concat('1995-',right(edt2,5))\n" +
		 * "when year(edt2)=2094 then concat('1994-',right(edt2,5))\n" +
		 * "when year(edt2)=2093 then concat('1993-',right(edt2,5))\n" +
		 * "when year(edt2)=2092 then concat('1992-',right(edt2,5))\n" +
		 * "when year(edt2)=2091 then concat('1991-',right(edt2,5))\n" +
		 * "when year(edt2)=2090 then concat('1990-',right(edt2,5)) else edt2 end\n"
		 * );
		 * 
		 * if (isBacTPRaw) { sb.append(",ended='EDT2' \n"); }
		 * 
		 * sb.append("\n WHERE year(edt2)>year(curdate());\n");
		 */

		if (!isBacTPRaw) {
			// only applies to tp_sales_to_scrub2
			sb.append("\ndelete ignore from "
					+ table
					+ "\n WHERE year(edt2)>year(curdate());\n\n"
					+ "drop table if exists tmp1TPSales"
					+ yr
					+ "_"
					+ q
					+ ";\n"
					+ "create table tmp1TPSales"
					+ yr
					+ "_"
					+ q
					+ " engine=myisam\n"
					+ "ignore select "
					// datediff(t1.filedate,t1.edt2) df,\n"
					// +
					// "(right(left(tslong,3),2)) tsM,tslong,day(t1.filedate) dFd,day(t1.edt2) dEdt2,\n"
					+ "t1.accno,t1.tno from "
					+ table
					+ " t1 inner join tp_raw_revised t2 on t1.accno=t2.accno and t1.tno=t2.tno and t1.filedate=t2.filedate\n"
					+ "where t1.row=t2.row AND datediff(t1.filedate,t1.edt2)<5  \n"
					+ "GROUP BY ACCNO,TNO;\n" + "alter table tmp1TPSales" + yr
					+ "_" + q + " add key(accno), add key(tno);\n" + "\n"
					+ "delete t1 from " + table + " t1 inner join tmp1TPSales"
					+ yr + "_" + q
					+ " t2 on t1.accno=t2.accno and t1.tno=t2.tno;\n" + "\n");
		}

		if (isBacTPRaw) {

			/*
			 * sb.append("DROP TABLE IF EXISTS TMP_GET_EDT_FROM_ANOTHER_TABLE_"
			 * + yr + "_" + q + ";\n" +
			 * "CREATE TABLE TMP_GET_EDT_FROM_ANOTHER_TABLE_" + yr + "_" + q +
			 * " ENGINE=MYISAM\n" +
			 * "SELECT t2.edt2 T2EDT2,t2.p2 t2p2,t2.p1 t2p1,t1.p1 t1p1,t1.p2 t1p2,\n"
			 * + "t1.accno T1ACC,T1.COL T1COL FROM " + table + " T1\n" +
			 * "INNER JOIN " + table + " T2 ON T1.ACCNO=T2.ACCNO  \n" +
			 * "WHERE T1.TN!='BS' AND T2.TN!='BS' AND T1.TN!='SE' AND T2.TN!='SE' and T1.tNo != T2.tNo\n"
			 * + "AND ABS(T1.VALUE)>999 AND ABS(T2.VALUE)>999\n" +
			 * "AND (T1.TC=2 OR T1.TC=4 ) AND (T2.TC=2 OR T2.TC=4 ) \n" +
			 * "AND LENGTH(T1.EDT2)<10 AND LENGTH(TRIM(T2.EDT2))=10 \n" +
			 * "AND t1.VALUE=t2.VALUE\n" +
			 * "and left(trim(t1.rowname),5)=left(trim(t2.rowname),5)\n" +
			 * "and t1.yr!='bad' and t2.yr!='bad'\n" +
			 * "and t1.rowname rlike 'sales|revenue|(gross|net|total)( interest)? income|net.{1,4}(income|earning|loss)';\n"
			 * + "ALTER TABLE TMP_GET_EDT_FROM_ANOTHER_TABLE_" + yr + "_" + q +
			 * " ADD KEY(T1ACC),ADD KEY(T1TNO),ADD KEY(T1COL);\n" + "\n" +
			 * "UPDATE IGNORE " + table +
			 * " T1 INNER JOIN TMP_GET_EDT_FROM_ANOTHER_TABLE_" + yr + "_" + q +
			 * " T2\n" +
			 * "ON T1.ACCNO=T2.T1ACC AND T1.TNO=T2.T1TNO AND T1.COL=T2.T1COL\n"
			 * + "SET EDT2=T2EDT2, ENDED='EDT2'\n" + "WHERE\n" +
			 * "t2p2 between 3 and 12 and t2p1 between 3 and 12 \n" +
			 * "AND ((T1P2=T2P2 OR T1P2=0) AND (T1P1=T2P1 OR T1P1=0));\n" +
			 * "\n");
			 */

			/*
			 * sb.append("\n\nupdate ignore " + table + " set edt2=\n" +
			 * "case \n" +
			 * "when columnText rlike ' Jan' then concat(left(edt2,8),right(trim(substring_index(replace(ColumnText,'Jan','Jan'),' Jan',1)),2))\n"
			 * +
			 * "when columnText rlike ' Feb' then concat(left(edt2,8),right(trim(substring_index(replace(ColumnText,'Feb','Feb'),' Feb',1)),2))\n"
			 * +
			 * "when columnText rlike ' Mar' then concat(left(edt2,8),right(trim(substring_index(replace(ColumnText,'Mar','Mar'),' Mar',1)),2))\n"
			 * +
			 * "when columnText rlike ' Apr' then concat(left(edt2,8),right(trim(substring_index(replace(ColumnText,'Apr','Apr'),' Apr',1)),2))\n"
			 * +
			 * "when columnText rlike ' May' then concat(left(edt2,8),right(trim(substring_index(replace(ColumnText,'May','May'),' May',1)),2))\n"
			 * +
			 * "when columnText rlike ' Jun' then concat(left(edt2,8),right(trim(substring_index(replace(ColumnText,'Jun','Jun'),' Jun',1)),2))\n"
			 * +
			 * "when columnText rlike ' Jul' then concat(left(edt2,8),right(trim(substring_index(replace(ColumnText,'Jul','Jul'),' Jul',1)),2))\n"
			 * +
			 * "when columnText rlike ' Aug' then concat(left(edt2,8),right(trim(substring_index(replace(ColumnText,'Aug','Aug'),' Aug',1)),2))\n"
			 * +
			 * "when columnText rlike ' Sep' then concat(left(edt2,8),right(trim(substring_index(replace(ColumnText,'Sep','Sep'),' Sep',1)),2))\n"
			 * +
			 * "when columnText rlike ' Oct' then concat(left(edt2,8),right(trim(substring_index(replace(ColumnText,'Oct','Oct'),' Oct',1)),2))\n"
			 * +
			 * "when columnText rlike ' Nov' then concat(left(edt2,8),right(trim(substring_index(replace(ColumnText,'Nov','Nov'),' Nov',1)),2))\n"
			 * +
			 * "when columnText rlike ' Dec' then concat(left(edt2,8),right(trim(substring_index(replace(ColumnText,'Dec','Dec'),' Dec',1)),2))\n"
			 * + "else edt2 end \n" + ", ended= 'edt2'\n" + "where \n" +
			 * "columnText rlike '(^[0-9]{1,2}| [0-9]{1,2}) (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)' \n"
			 * // + "<<==columnText may have a european day format\n" +
			 * "and (\n" +
			 * "(ColumnText regexp 'jan')+(ColumnText regexp 'feb')+(ColumnText regexp '^marc| mar')+(ColumnText regexp 'apr')+\n"
			 * +
			 * "(ColumnText regexp '^may| may')+(ColumnText regexp 'jun')+(ColumnText regexp 'jul')+(ColumnText regexp 'aug')+\n"
			 * +
			 * "(ColumnText regexp 'sep')+(ColumnText regexp 'oct')+(ColumnText regexp 'nov')+(ColumnText regexp 'dec')  ) =1\n"
			 * + "and\n" + "(\n" +
			 * "(ColumnText regexp '1990')+ (ColumnText regexp '1991')+ (ColumnText regexp '1992')+ (ColumnText regexp '1993')+ (ColumnText regexp '1994')+\n"
			 * +
			 * "(ColumnText regexp '1995')+ (ColumnText regexp '1996')+ (ColumnText regexp '1997')+ (ColumnText regexp '1998')+ (ColumnText regexp '1999')+ \n"
			 * +
			 * "(ColumnText regexp '2000')+ (ColumnText regexp '2001')+ (ColumnText regexp '2002')+ (ColumnText regexp '2003')+ (ColumnText regexp '2004')+ \n"
			 * +
			 * "(ColumnText regexp '2005')+ (ColumnText regexp '2006')+ (ColumnText regexp '2007')+ (ColumnText regexp '2008')+ (ColumnText regexp '2009')+ \n"
			 * +
			 * "(ColumnText regexp '2010')+ (ColumnText regexp '2011')+ (ColumnText regexp '2012')+ (ColumnText regexp '2013')+ (ColumnText regexp '2014')+ \n"
			 * +
			 * "(ColumnText regexp '2015')+ (ColumnText regexp '2016')+ (ColumnText regexp '2017')+ (ColumnText regexp '2018')+ (ColumnText regexp '2019')+ \n"
			 * +
			 * "(ColumnText regexp '2020')+ (ColumnText regexp '2021')+ (ColumnText regexp '2022')+ (ColumnText regexp '2023')+ (ColumnText regexp '2024')\n"
			 * + ") =1\n" // +
			 * "<<==filter out false positives - can only have 1 year and 1 month value\n"
			 * +
			 * "and columntext not rlike '(Mar|Apr|May|Jun|Jul).{1,4}[0-9]{1,2}($|[, ]{1})' \n"
			 * +
			 * "and columntext not rlike '(Jan|Feb|Aug|Sep|Oct|Nov|Dec){1,7}.{1,4}[0-9]{1,2}($|[, ]{1})' \n"
			 * + "and columnText not rlike concat(right(edt2,2),' ')\n" // +
			 * "<<==filter out false positives - cannot be non-european date format\n"
			 * +
			 * "and columnText not rlike concat(right(edt2,2),',') and length(edt2)=10\n"
			 * // + "edt2 has a day not matching a day that is in columnText
			 * ;\n");
			 */
		}

		String dropProc = "DROP PROCEDURE IF EXISTS tp_Sales_fixEndDates;\n"
				+ "CREATE PROCEDURE tp_Sales_fixEndDates()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
		MysqlConnUtils.executeQuery("call tp_Sales_fixEndDates();\n");

	}

	public void generateTP_Sales(int[] periods, String table, int cikStart,
			int cikEnd, int cnt) throws SQLException, FileNotFoundException {
		StringBuffer sb = new StringBuffer();

		/*
		 * This method retrieves from tp_sales_to_scrub2 table (or direct output
		 * of it if I feed segments of tp_sales_to_scrub2 b/c that file may be
		 * too large and generates period=3 values). This methods takes p12, p9,
		 * p6 and p3 values as reported and then performs calculations to
		 * determine additional p3 values. It will subtract p9 from p12 where
		 * both have same enddate and a reportDate separated by 3 months (does
		 * the same for 6 and 9 and 3 and 6). For 6 and 9 and 3 and 6 it will
		 * also compare for same enddate to get 3 month period that is start of
		 * longer period provided they are the same filing (accno). In addition,
		 * whenever I am comparing two accno of same cik I ensure they represent
		 * consecutive filedates - else comparison across to far a span will
		 * cause more errors with very little benefit in filing in missing
		 * enddates.
		 */
		if (cnt == 0) {
			cnt++;
			String qry = "DROP TABLE IF EXISTS TP_SALES_P3;\n"
					+ "CREATE TABLE `tp_sales_p3` (\n"
					+ "  `accno` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `reportDate` datetime DEFAULT NULL,\n"
					+ "  `cik` int(11) DEFAULT NULL,\n"
					+ "  `tno` int(5) NOT NULL DEFAULT '-1',\n"
					+ "  `row` int(5) NOT NULL DEFAULT '-1' COMMENT 'table row',\n"
					+ "  `rowname` varchar(150) DEFAULT NULL,\n"
					+ "  `enddate` varchar(10) NOT NULL DEFAULT '1901-01-01',\n"
					+ "  `period` int(3) DEFAULT NULL COMMENT 'if html - per1 parsed from cell, if txt per1 parsed based on col hdg ratio matching',\n"
					+ "  `value` double(18,1) NOT NULL DEFAULT '0.0',\n"
					+ "  `dec` int(11) DEFAULT NULL,\n"
					+ "  `dif` int(7) NOT NULL DEFAULT '0',\n"
					+ "  `rep` varchar(4) CHARACTER SET utf8 NOT NULL DEFAULT '',\n"
					+ "  `vCalc1` double(18,1) DEFAULT NULL,\n"
					+ "  `vCalc2` double(18,1) DEFAULT NULL,\n"
					+ "  `eCalc1` varchar(10) DEFAULT NULL,\n"
					+ "  `eCalc2` varchar(10) DEFAULT NULL,\n"
					+ "  `pCalc1` TINYINT(3) DEFAULT NULL,\n"
					+ "  `pCalc2` TINYINT(3) DEFAULT NULL,\n"
					+ "  `rnCalc1` varchar(150) DEFAULT NULL,\n"
					+ "  `rnCalc2` varchar(150) DEFAULT NULL,\n"
					+ "  `decCalc1` int(9) DEFAULT NULL,\n"
					+ "  `decCalc2` int(9) DEFAULT NULL,\n"
					+ "  `repCalc1` varchar(20) DEFAULT NULL,\n"
					+ "  `repCalc2` varchar(20) DEFAULT NULL,\n"
					+ "  `accCalc1` varchar(20) DEFAULT NULL,\n"
					+ "  `accCalc2` varchar(20) DEFAULT NULL,\n"
					+ "  PRIMARY KEY (`accno`,`tno`,`enddate`,`value`,`dif`),\n"
					+ "  KEY `accno` (`accno`),\n"
					+ "  KEY `enddate` (`enddate`),\n"
					+ "  KEY `cik` (`cik`),\n"
					+ "  KEY `rowname` (`rowname`)\n"
					+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n";
			MysqlConnUtils.executeQuery(qry);

		}

		sb.append("DROP TABLE IF EXISTS TMP_SCRUB_SALES;\n"
				+ "CREATE TABLE TMP_SCRUB_SALES ENGINE=MYISAM\n"
				+ "SELECT accno,form,filedate ,cik,tno,row,\n"
				+ "trim(replace(replace(replace(replace(substring_index(substring_index(substring_index(substring_index(rowname,';SUB',1),';NET',1),';ST',1),';TL',1),':',' '),';',''),',',''),'  ',' ')) rowname\n"
				+ ",value,p2 ,edt2 \n"
				+ ",CASE WHEN `DEC`=0 OR `DEC` IS NULL OR `DEC`='' THEN 1 WHEN `DEC`=-3 THEN 1000 WHEN `DEC` = -6 THEN 1000000 \n"
				+ "WHEN `DEC`=-9 THEN 1000000000 ELSE `DEC` END `DEC`\n"
				+ ",revised\n"
				+ ",@mo:=case when edt2 rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})' then right(left(edt2,7),2) else '' end mo\n"
				+ ",@dy:=case when edt2 rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})' then right(edt2,2) else '' end dy\n"
				+ ",@fDay:=case \n"
				+ "when @mo>0 and @mo!=2 and (@dy>=15 or right(edt2,2)='00') and @dy!='' and @mo!='' then 30\n"
				+ "when @mo=2 and (@dy>=15 or right(edt2,2)='00') and @dy!='' and @mo!='' then 28\n"
				+ "when @mo!=3 and @dy<15 and @dy!='' and @mo!='' then 30 \n"
				+ "when @mo=3 and @dy<15 and @dy!='' and @mo!='' then 28 else '' end fDay\n"
				+ ",@fMo:=case \n"
				+ "when @dy>=15 or right(edt2,2)='00' or @dy='' then @mo\n"
				+ "when @dy<15 and @mo>1 then @mo-1\n"
				+ "when @dy<15 and @mo<2 then 12 else @mo end fMo\n"
				+ ",@fnlMo:=case when length(@fMo)=1 then concat('0',@fMo) when @fMo between 1 and 12 then @fMo else @mo end fnlMo\n"
				+ ",@fnlDay:=case when length(@fDay)=1 then concat('0',@fDay) else @fDay end fnlDay\n"
				+ ",LEFT(concat(@fnlMo,'-',@fnlDay),5) q_end\n"
				+ ",@yr:=case when @mo<2 and @dy<15  and @mo!='' and @dy!='' then left(edt2,4)-1 else left(edt2,4) end yr\n"
				+ ",LEFT(CONCAT(@yr,'-',LEFT(concat(@fnlMo,'-',@fnlDay),5)),10) enddate\n"
				+ " FROM "
				+ table
				+ " \n"
				+ " where cik between "
				+ cikStart
				+ " and "
				+ cikEnd
				+ "\n"
				+ " and\n"
				+ " value>0 and p2 between 3 and 12\n"
				+ " and (left(replace(columnText,'for the ',''),9) not rlike '(one|two|four|five|ten|10) Mo|seven|eight|five w' and\n"
				+ " columnText not rlike '(fifty|thirty|three hundred).{1,8}day' ) order by cik,edt2;\n"
				+ "\n"
				+ "ALTER TABLE TMP_SCRUB_SALES ADD KEY(ACCNO), CHANGE enddate ENDDATE VARCHAR(10),\n"
				+ "CHANGE P2 PERIOD TINYINT(3), CHANGE FILEDATE REPORTDATE DATETIME,\n"
				+ "ADD KEY(ENDDATE), ADD KEY(PERIOD),DROP COLUMN Q_END,\n"
				+ "DROP COLUMN FNLDAY, DROP COLUMN FNLMO, DROP COLUMN MO, DROP COLUMN DY, DROP COLUMN FMO, DROP COLUMN FDAY, DROP COLUMN EDT2; \n"
				+ "/*if value=100 this is likely a percentage col. This works with top line of income statement (sales) but not other IS line items.*/\n");

		int[] per = { 3, 6, 9, 12 };

		for (int i = 0; i < per.length; i++) {
			sb.append("DROP TABLE IF EXISTS tmp_tp_sales_p"
					+ per[i]
					+ ";\n"
					+ "CREATE TABLE `tmp_tp_sales_p"
					+ per[i]
					+ "` (\n"
					+ "  `accno` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `reportDate` datetime DEFAULT NULL,\n"
					+ "  `cik` int(11) DEFAULT NULL,\n"
					+ "  `tno` int(5) NOT NULL DEFAULT '-1',\n"
					+ "  `row` int(5) NOT NULL DEFAULT '-1' COMMENT 'table row',\n"
					+ "  `rowname` varchar(255) DEFAULT NULL,\n"
					+ "  `enddate` varchar(10) DEFAULT NULL,\n"
					+ "  `period` int(3) DEFAULT NULL COMMENT 'if html - per1 parsed from cell, if txt per1 parsed based on col hdg ratio matching',\n"
					+ "  `value` double(18,1) DEFAULT NULL,\n"
					+ "  `dec` int(11) DEFAULT NULL,\n"
					+ "  `dif` int(7) DEFAULT NULL,\n"
					+ "  `rep` varchar(10) CHARACTER SET utf8 NOT NULL DEFAULT '',\n"
					+ "  `vCalc1` double(18,1) DEFAULT NULL,\n"
					+ "  `vCalc2` double(18,1) DEFAULT NULL,\n"
					+ "  `eCalc1` varchar(10) DEFAULT NULL,\n"
					+ "  `eCalc2` varchar(10) DEFAULT NULL,\n"
					+ "  `pCalc1` TINYINT(3) DEFAULT NULL,\n"
					+ "  `pCalc2` TINYINT(3) DEFAULT NULL,\n"
					+ "  `rnCalc1` varchar(255) DEFAULT NULL,\n"
					+ "  `rnCalc2` varchar(255) DEFAULT NULL,\n"
					+ "  `decCalc1` int(9) DEFAULT NULL,\n"
					+ "  `decCalc2` int(9) DEFAULT NULL,\n"
					+ "  `repCalc1` varchar(20) DEFAULT NULL,\n"
					+ "  `repCalc2` varchar(20) DEFAULT NULL,\n"
					+ "  `accCalc1` varchar(20) DEFAULT NULL,\n"
					+ "  `accCalc2` varchar(20) DEFAULT NULL,\n"
					+ "KEY (CIK),\n" + "KEY (Rowname),\n" + "KEY (Accno),\n"
					+ "KEY (enddate)\n"
					+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n" + "\n");

			// get all 6, 9 and 12 month reported periods and put into
			// respective tmp_tp_sales_p tables.
			sb.append("\nset sql_mode = ALLOW_INVALID_DATES;"
					+ "\nINSERT IGNORE INTO TMP_tp_sales_p"
					+ per[i]
					+ "\n"
					+ "SELECT \n"
					+ "accno,reportDate,cik,tno,row,rowname, enddate, period, value,`dec`,datediff(reportDate,t1.ENDDATE) df, 'rep' rep\n"
					+ ",null vCalc1, null vCalc2, null eCalc1, null eCalc2, null pCalc1, null pCalc2, null rnCalc1, null rnCalc2, null decCalc1\n"
					+ ", null decCalc2, null repCalc1, null repCalc2, null accCalc1, null accCalc2\n"
					+ " FROM TMP_SCRUB_SALES T1 \n"
					+ "WHERE t1.PERIOD="
					+ per[i]
					+ ";\n\n"
					+ "/*Get p6, p9 and p12 data. Will use to calc p3 values*/\n");
		}

		int[] dateRange = { 0, -18 };
		String sameAcc = "";
		String enddate2 = "";
		for (int c = 0; c < dateRange.length; c++) {
			for (int i = 3; i > 0; i--) {
				if (c == 0) {
					enddate2 = "T1.ENDDATE";
				} else
					continue;
				sb.append("\nDROP TABLE IF EXISTS tmp_tp_sales_P3_1;\n"
						+ "CREATE TABLE tmp_tp_sales_P3_1 engine=myisam\n"
						+ "SELECT T2.ACCNO,T2.reportDate,T1.CIK,T2.TNO,T2.ROW,T2.ROWNAME,/*\n\nfor calc values - the reportDate (and related accno) is the reportDate of the oldest value - which will always be t2 given the"
						+ "\nrequirement that t1 reportdate be newer than t2 reportdate\n\n*/"
						+ enddate2
						+ ",T1.PERIOD-T2.PERIOD PERIOD\n"
						+ ",@val1Dec:=case when t1.value/(t2.value*t1.period/t2.period)<.005 then t1.value*(greatest(t1.`dec`,1000)) else t1.value end val1Dec\n"
						+ ",@val2Dec:=case when t1.value/(t2.value*t1.period/t2.period) >250 then t2.value*(greatest(t1.`dec`,1000)) else t2.value end val2Dec\n"
						+ ",@dec:=case when t1.value/t2.value>250 then t2.`dec` when t1.value/t2.value<.005 then t1.`dec` else 1 end `dec`\n"
						+ ",case when @val1Dec>@val2Dec then round((@val1Dec-@val2Dec)/@dec) else null end value,"
						+ "3 dif"
						+ ", 'calc' rep, t1.value vCalc1, t2.value vCalc2, t1.enddate eCalc1"
						+ ", t2.enddate eCalc2, t1.period pCalc1, t2.period pCalc2,"
						+ "\n case when length(t1.rowname)<2 and length(t2.rowname)<2 then 'same'\n"
						+ " when (left(t1.rowname,10)!=left(t2.rowname,10) or t1.rowname = 'different') then 'different' else 'same' end rnCalc1"
						+ "\n,t2.rowname rnCalc2"
						+ ", t1.`dec` decCalc1, t2.`dec` decCalc2, t1.reportDate repCalc1, t2.reportDate repCalc2, t1.accno accCalc1, t2.accno accCalc2 "
						+ "FROM \n\ntmp_tp_sales_p"
						+ periods[i]
						+ " T1\n\n"
						+ "INNER JOIN \n\ntmp_tp_sales_p"
						+ (periods[i] - 3)
						+ " t2\n\n on t1.cik=t2.cik "
						+ sameAcc
						+ "\n\n"
						+ "WHERE datediff(t1.enddate,t2.enddate) between ");
				if (c == 0) {
					sb.append("(t1.period-t2.period)/12*365-18 and (t1.period-t2.period)/12*365+18 \n"
							+ "and datediff(t1.reportDate,t2.reportDate) between 0 and 200;\n");
				}
				/*
				 * else {sb.append(dateRange[c] + " and " + (dateRange[c] + 36)
				 * + ";\n");}
				 */
				sb.append("ALTER TABLE tmp_tp_sales_p3_1 drop column val1dec, drop column val2dec, ADD KEY(CIK,ENDDATE,dif);\n"
						+ "\n"
						+ "SET @dif=0; SET @E='1901-01-01'; SET @CIK=0; SET @V=0.0; SET @rw = 0;\n"
						+ "\nDROP TABLE IF EXISTS tmp_tp_sales_P3_2;\n"
						+ "CREATE TABLE tmp_tp_sales_P3_2 engine=myisam\n"
						+ " SELECT CASE WHEN @rw=0 THEN 1 WHEN @CIK=CIK AND @E=ENDDATE AND @V=VALUE AND @dif=dif AND rep rlike 'calc' THEN 0 ELSE 1 END getIt,\n"
						+ " @rw:=@rw+1 rw,ACCNO,reportDate,@CIK:=CIK CIK,TNO,ROW,ROWNAME,@E:=ENDDATE ENDDATE,PERIOD,@V:=VALUE VALUE,`DEC`,@dif:=dif dif,REP,\n"
						+ " vCalc1, vCalc2, eCalc1, eCalc2, pCalc1, pCalc2, rnCalc1, rnCalc2,\n"
						+ " decCalc1, decCalc2, repCalc1, repCalc2, accCalc1, accCalc2\n"
						+ " FROM tmp_tp_sales_p3_1 ORDER BY CIK,ENDDATE,dif;\n"
						+ "ALTER TABLE tmp_tp_sales_P3_2 ADD KEY (GETIT), DROP COLUMN RW;\n"
						+ "\n"
						/* <<==HERE I BELIEVE I CAN DELETE GETIT function */
						+ "\nset sql_mode = ALLOW_INVALID_DATES;"
						+ "\nINSERT IGNORE INTO tmp_tp_sales_p3\n"
						+ "select ACCNO,reportDate,CIK,TNO,ROW,ROWNAME,ENDDATE,PERIOD,VALUE,`DEC`,dif,REP,\n"
						+ "vCalc1, vCalc2, eCalc1, eCalc2, pCalc1, pCalc2, rnCalc1, rnCalc2, decCalc1, decCalc2, repCalc1, repCalc2, accCalc1, accCalc2 \n"
						+ "FROM tmp_tp_sales_p3_2\n" + "WHERE GETIT=1;\n");
			}
			sb.append(removeDuplicatePeriod(3));
		}

		// gets 6v3 mo with same enddate -so if 6mo edt=0630 and 3mo edt=0630 -
		// calc p3 edt=0330. This becomes prone to error when going back more
		// than 6 months

		sb.append("set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "DROP TABLE IF EXISTS TMP_TP_SALES_P3_6V3;\n"
				+ "create table tmp_tp_sales_p3_6v3 engine=myisam\n"
				+ "SELECT T1.ACCNO,T1.reportDate,T1.CIK,T1.TNO,T1.ROW,T1.ROWNAME,date_sub(t1.enddate, \n"
				+ "interval t2.period month) enddate,T1.PERIOD-T2.PERIOD PERIOD\n"
				+ ",@val1Dec:=case when t1.value/(t2.value*t1.period/t2.period)<.005 then t1.value*(greatest(t1.`dec`,1000)) else t1.value end val1Dec\n"
				+ ",@val2Dec:=case when t1.value/(t2.value*t1.period/t2.period) >250 then t2.value*(greatest(t1.`dec`,1000)) else t2.value end val2Dec\n"
				+ ",@dec:=case when t1.value/t2.value>250 then t2.`dec` when t1.value/t2.value<.005 then t1.`dec` else 1 end `dec`\n"
				+ ",case when @val1Dec>@val2Dec then round((@val1Dec-@val2Dec)/@dec) else null end value\n"
				+ ",0 dif,\n\n/*The only calc value I may 're-use' is p3 values. If a p3 value was calculate and used to calculate another"
				+ " p3 value it is labeled 'calc2'. This\nallows me to then discard it in favor of another calc for same edt that is not a calc2. "
				+ "It also can be used to help identify revisions by Cos.*/"
				+ "\n\ncase when t2.rep='calc' then 'calc2' else 'calc' end calc,t1.value vCalc1,t2.value vCalc2,t1.enddate eCalc1,t2.enddate eCalc2,t1.period pCalc1,t2.period pCalc2,\n"
				+ "t1.rowname rnCalc1"
				+ "\n,t2.rowname rnCalc2"
				+ ",t1.`dec` decCalc1,t2.`dec` decCalc2,t1.reportDate repCalc1,t2.reportDate repCalc2,t1.accno accCalc1,t2.accno accCalc2  FROM \n"
				+ "\ntmp_tp_sales_p6 T1\n\n"
				+ "INNER JOIN \n\ntmp_tp_sales_p3 t2 \n\non t1.cik=t2.cik and \n"
				+ "t1.accno=t2.accno\n"
				+ "WHERE datediff(t1.enddate,t2.enddate) between -18 and 18;\n"
				+ "ALTER TABLE TMP_TP_SALES_P3_6V3 drop column val1dec, drop column val2dec, ADD KEY(CIK,ENDDATE,dif);\n"
				+ "\nINSERT IGNORE INTO tmp_tp_sales_p3\n"
				+ "SELECT ACCNO,reportDate,CIK,TNO,ROW,ROWNAME,ENDDATE,PERIOD,VALUE,`DEC`,dif,CALC,\n"
				+ "vCalc1, vCalc2, eCalc1, eCalc2, pCalc1, pCalc2, rnCalc1, rnCalc2, decCalc1\n"
				+ ", decCalc2, repCalc1, repCalc2, accCalc1, accCalc2  FROM TMP_TP_SALES_P3_6V3;\n"
				+ "DROP TABLE IF EXISTS TMP_TP_SALES_P3_6V3;\n\n");

		// 9v6 - gets 1st 3mo of 9mo (same as above but 9v6).
		sb.append("set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "DROP TABLE IF EXISTS TMP_TP_SALES_P3_9V6;\n"
				+ "create table TMP_TP_SALES_P3_9V6 engine=myisam\n"
				+ "SELECT T1.ACCNO,T1.reportDate,T1.CIK,T1.TNO,T1.ROW,T1.ROWNAME,date_sub(t1.enddate, \n"
				+ "interval t2.period month) enddate,T1.PERIOD-T2.PERIOD PERIOD\n"
				+ ",@val1Dec:=case when t1.value/(t2.value*t1.period/t2.period)<.005 then t1.value*(greatest(t1.`dec`,1000)) else t1.value end val1Dec\n"
				+ ",@val2Dec:=case when t1.value/(t2.value*t1.period/t2.period) >250 then t2.value*(greatest(t1.`dec`,1000)) else t2.value end val2Dec\n"
				+ ",@dec:=case when t1.value/t2.value>250 then t2.`dec` when t1.value/t2.value<.005 then t1.`dec` else 1 end `dec`\n"
				+ ",case when @val1Dec>@val2Dec then round((@val1Dec-@val2Dec)/@dec) else null end value\n"
				+ ",0 dif,\n\n/*The only calc value I may 're-use' is p3 values. If a p3 value was calculate and used to calculate another"
				+ " p3 value it is labeled 'calc2'. This\nallows me to then discard it in favor of another calc for same edt that is not a calc2. "
				+ "It also can be used to help identify revisions by Cos.*/"
				+ "\n\ncase when t2.rep='calc' then 'calc2' else 'calc' end calc,t1.value vCalc1,t2.value vCalc2,t1.enddate eCalc1,t2.enddate eCalc2,t1.period pCalc1,t2.period pCalc2,\n"
				+ "t1.rowname rnCalc1"
				+ "\n,t2.rowname rnCalc2"
				+ ",t1.`dec` decCalc1,t2.`dec` decCalc2,t1.reportDate repCalc1,t2.reportDate repCalc2,t1.accno accCalc1,t2.accno accCalc2  FROM \n"
				+ "\ntmp_tp_sales_p9 T1\n\n"
				+ "INNER JOIN \n\ntmp_tp_sales_p6 t2 \n\n on t1.cik=t2.cik and \n"
				+ "t1.accno=t2.accno\n"
				+ "WHERE datediff(t1.enddate,t2.enddate) between -18 and 18;\n"
				+ "ALTER TABLE TMP_TP_SALES_P3_9V6 drop column val1dec, drop column val2dec, ADD KEY(CIK,ENDDATE,dif);\n"
				+ "INSERT IGNORE INTO tmp_tp_sales_p3\n"
				+ "SELECT ACCNO,reportDate,CIK,TNO,ROW,ROWNAME,ENDDATE,PERIOD,VALUE,`DEC`,dif,CALC,\n"
				+ "vCalc1, vCalc2, eCalc1, eCalc2, pCalc1, pCalc2, rnCalc1, rnCalc2, decCalc1\n"
				+ ", decCalc2, repCalc1, repCalc2, accCalc1, accCalc2  FROM TMP_TP_SALES_P3_9V6;\n"
				+ "DROP TABLE IF EXISTS TMP_TP_SALES_P3_9V6;\n\n");

		sb.append("SET @D =0; SET @V=0.0; SET @E='1901-01-01'; SET @C=0; SET @rw = 0;\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP_TP_SALES_P3_REMOVE;\n"
				+ "CREATE TABLE TMP_TP_SALES_P3_REMOVE ENGINE=MYISAM\n"
				+ "SELECT CASE WHEN @rw=0 THEN 1 WHEN @C=CIK AND @E=ENDDATE AND @V=Value AND rep rlike 'calc' THEN 0 ELSE 1 END GETIT,\n"
				+ "@rw:=@rw+1 rw,T1.accno, T1.reportDate, T1.cik, T1.tno, T1.row, T1.rowname, T1.enddate, T1.period, T1.value, T1.`dec`, T1.dif, T1.rep, T1.vCalc1, T1.vCalc2, T1.eCalc1, T1.eCalc2, T1.pCalc1, T1.pCalc2, T1.rnCalc1, T1.rnCalc2, T1.decCalc1, T1.decCalc2, T1.repCalc1, T1.repCalc2, T1.accCalc1, T1.accCalc2\n"
				+ ",@D:=DIF D, @V:=VALUE V, @E:=ENDDATE E,@C:=CIK C\n"
				+ "FROM tmp_tp_sales_p3 T1 ORDER BY CIK,ENDDATE,DIF,VALUE ;\n"
				+ "ALTER TABLE TMP_TP_SALES_P3_REMOVE ADD KEY(GETIT), DROP COLUMN RW;\n"
				+ "\nset sql_mode = ALLOW_INVALID_DATES;\n"
				+ "\nINSERT IGNORE INTO TP_SALES_P3\n"
				+ "SELECT T1.accno, T1.reportDate, T1.cik, T1.tno, T1.row, T1.rowname, T1.enddate, T1.period, T1.value, T1.`dec`, T1.dif, T1.rep, T1.vCalc1, T1.vCalc2, T1.eCalc1, T1.eCalc2, T1.pCalc1, T1.pCalc2, T1.rnCalc1, T1.rnCalc2, T1.decCalc1, T1.decCalc2, T1.repCalc1, T1.repCalc2, T1.accCalc1, T1.accCalc2\n"
				+ "FROM TMP_TP_SALES_P3_REMOVE T1 WHERE GETIT=1;\n");

		sb.append("\ndrop table if exists tmp_tp_sales_p3_2;\n"
				+ "drop table if exists tmp_tp_sales_p3_1;\n"
				+ " drop table if exists TMP_SCRUB_SALES; \n"
				+ "drop table if exists tmp_tp_sales_P9_1;\n"
				+ "drop table if exists tmp_tp_sales_P9_2;\n"
				+ "drop table if exists tmp_tp_sales_P6_1;\n"
				+ "drop table if exists tmp_tp_sales_P6_2;\n"
				+ "drop table if exists tmp_tp_sales_P12;\n"
				+ "drop table if exists tmp_tp_sales_P9;\n"
				+ "drop table if exists tmp_tp_sales_P6;\n"
				+ "drop table if exists tmp_tp_sales_P3;\n");

		String dropProc = "DROP PROCEDURE IF EXISTS generateTP_Sales;\n"
				+ "CREATE PROCEDURE generateTP_Sales()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
		MysqlConnUtils.executeQuery("call generateTP_Sales();\n");

		sb.delete(0, sb.toString().length());
	}

	public static String removeDuplicatePeriod(int period) {
		// NOTE SUBROUTINE: used as subroutine - returns String FOR OTHER OTHER
		// METHODS.

		/*
		 * run for all of p3,p6,p9 and p12 prior to re-use. Remove
		 * duplicatePeriod cik,edt2,dif,val
		 */

		StringBuffer sb = new StringBuffer(
				"SET @dif=0; SET @E='1901-01-01'; SET @CIK=0; SET @V=0.0; SET @rw = 0;\n"
						+ "DROP TABLE IF EXISTS tmp_tp_sales_P_1;\n"
						+ "CREATE TABLE tmp_tp_sales_P_1 engine=myisam\n"
						+ "SELECT CASE WHEN @rw=0 THEN 1 WHEN @CIK=CIK AND @E=ENDDATE AND @V=VALUE AND @dif=dif AND rep rlike 'calc' THEN 0 ELSE 1 END getIt,\n"
						+ "@rw:=@rw+1 rw, @CIK:=CIK C,@V:=VALUE V,@E:=ENDDATE E,@dif:=dif d,\n"
						+ "t1.* FROM tmp_tp_sales_p"
						+ period
						+ " t1 ORDER BY Cik,enddate,dif,value;\n"
						+ "ALTER TABLE tmp_tp_sales_P_1 ADD KEY (GETIT),DROP COLUMN RW, DROP COLUMN C, DROP COLUMN E, DROP COLUMN V, DROP COLUMN D;\n\n");

		sb.append(dropCreateTable(period));
		sb.append("set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "INSERT IGNORE INTO tmp_tp_sales_p"
				+ period
				+ "\n"
				+ "SELECT accno,reportdate,cik,tno,row,rowname,enddate,period,value,`dec`,dif,rep,vcalc1,vcalc2,ecalc1,ecalc2,pcalc1,pcalc2,rncalc1,rncalc2"
				+ ",deccalc1,deccalc2,repcalc1,repcalc2,acccalc1,acccalc2 FROM tmp_tp_sales_p_1 t1 where getIt=1\n;\n"
				+ "DROP TABLE IF EXISTS tmp_tp_sales_P_1;\n\n");

		return sb.toString();
	}

	public static String dropCreateTable(int period) {
		// acts as a sub-method- returns String - don't drop/create procedure.

		StringBuffer sb = new StringBuffer();
		sb.append("DROP TABLE IF EXISTS tmp_tp_sales_p"
				+ period
				+ ";\n"
				+ "CREATE TABLE `tmp_tp_sales_p"
				+ period
				+ "` (\n"
				+ "  `accno` varchar(20) NOT NULL DEFAULT '-1',\n"
				+ "  `reportDate` datetime DEFAULT NULL,\n"
				+ "  `cik` int(11) DEFAULT NULL,\n"
				+ "  `tno` int(5) NOT NULL DEFAULT '-1',\n"
				+ "  `row` int(5) NOT NULL DEFAULT '-1' COMMENT 'table row',\n"
				+ "  `rowname` varchar(255) DEFAULT NULL,\n"
				+ "  `enddate` varchar(10) DEFAULT NULL,\n"
				+ "  `period` int(3) DEFAULT NULL COMMENT 'if html - per1 parsed from cell, if txt per1 parsed based on col hdg ratio matching',\n"
				+ "  `value` double(18,1) DEFAULT NULL,\n"
				+ "  `dec` int(11) DEFAULT NULL,\n"
				+ "  `dif` int(7) DEFAULT NULL,\n"
				+ "  `rep` varchar(10) CHARACTER SET utf8 NOT NULL DEFAULT '',\n"
				+ "  `vCalc1` double(18,1) DEFAULT NULL,\n"
				+ "  `vCalc2` double(18,1) DEFAULT NULL,\n"
				+ "  `eCalc1` varchar(10) DEFAULT NULL,\n"
				+ "  `eCalc2` varchar(10) DEFAULT NULL,\n"
				+ "  `pCalc1` TINYINT(3) DEFAULT NULL,\n"
				+ "  `pCalc2` TINYINT(3) DEFAULT NULL,\n"
				+ "  `rnCalc1` varchar(255) DEFAULT NULL,\n"
				+ "  `rnCalc2` varchar(255) DEFAULT NULL,\n"
				+ "  `decCalc1` int(9) DEFAULT NULL,\n"
				+ "  `decCalc2` int(9) DEFAULT NULL,\n"
				+ "  `repCalc1` varchar(20) DEFAULT NULL,\n"
				+ "  `repCalc2` varchar(20) DEFAULT NULL,\n"
				+ "  `accCalc1` varchar(20) DEFAULT NULL,\n"
				+ "  `accCalc2` varchar(20) DEFAULT NULL,\n" + "KEY (CIK),\n"
				+ "KEY (Rowname),\n" + "KEY (Accno)," + "KEY (enddate)\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n" + "\n");

		return sb.toString();

	}

	public static void conformMultipleCIKs() throws SQLException, FileNotFoundException {

		// DESIGNED TO RUN AGAINST AND UPDATE BAC_TP_RAW.

		StringBuffer sb = new StringBuffer(
				"\n"
						+ "\n"
						+ "/*This conforms CIKs (multi CIKs for same accno OR multi accnos with many CIKs that are same filing). \n"
						+ "NOTE: This is only run once against BAC_TP_RAW. Generates and keep tables: sumAllTables and MultipleCIKs. \n"
						+ "SumAllTables is run agains tp_raw_revised and MultipleCIKs against tpIdx. Both then update bac_tp_raw.\n"
						+ "STEP 1. where 2 or more CIKs w/ single accno - update CIK for that AccNo w/ cik that has latest use period (latest filing date): \n"
						+ "this helps create continuity by having combined cik have long date period. \n"
						+ "Step 1 MUST BE RUN BEFORE STEP 2.  After step 1 is complete - all accNo are updated in tp_raw_revised to CIK with latest date range.\n"
						+ "\n"
						+ "STEP 2. Find multiple accnos with identical tables (two or more accno's tables data sum to identical value). \n"
						+ "Then assign cik with lastest date range (latest fileDate) to the other(s) that have the earlier last date filed.\n"
						+ "TpIdx has original cik for all accnos - so can alway revert.\n"
						+ "Because STEP 2 sums all tables it won't match accno across multiple filing periods \n"
						+ "(if I sum by col it would - but that is prone to high likelihood of error given many filings may reference another table in another accno).\n"
						+ "\n"
						+ "STEP 2 uses CIKS_date_range from step 1 in order to assign cik with latest date range*/\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMP_MULTIPE_CIKs;\n"
						+ "/*only run once b/c it runs against entire tpIdx*/\n"
						+ "CREATE TABLE TMP_MULTIPE_CIKs ENGINE=MYISAM\n"
						+ "SELECT CIK, `Company Name` company, `Form Type` form, RIGHT(`Date Filed`,10) filedate, left(right(Filename,25),20) accno FROM TPIDX \n"
						+ "where `Form Type` rlike '10-(k|q)';\n"
						+ "ALTER TABLE TMP_MULTIPE_CIKs ADD KEY(CIK), ADD KEY(ACCNO);\n"
						+ "\n"
						+ "/*SOME ACCNOs HAVE MULTIPLE CIKS. THIS WILL ASSIGN THE HIGHEST CIK ALWAYS TO THAT ACCNO.*/\n"
						+ "/*COUNT DISTINCT(CIKs) FOR EACH ACCNO. Sometimes multiple filings for same accno.*/\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMP_MULTIPE_CIKs2;\n"
						+ "CREATE TABLE TMP_MULTIPE_CIKs2 ENGINE=MYISAM\n"
						+ "SELECT COUNT(DISTINCT(CIK)) Cnt, ACCNO FROM TMP_MULTIPE_CIKs GROUP BY ACCNO;\n"
						+ "ALTER TABLE TMP_MULTIPE_CIKs2 change cnt cnt TINYINT(3), ADD KEY(Cnt), ADD KEY(ACCNO);\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS CIKS_date_range;\n"
						+ "CREATE TABLE CIKS_date_range ENGINE=MYISAM\n"
						+ "SELECT MIN(filedate) startdate,MAX(filedate) enddate,T1.CIK FROM TMP_MULTIPE_CIKs T1\n"
						+ "GROUP BY T1.CIK;\n"
						+ "ALTER TABLE CIKS_date_range ADD KEY(CIK);\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMP_MULTIPLE_CIKS2B;\n"
						+ "CREATE TABLE TMP_MULTIPLE_CIKS2B ENGINE=MYISAM\n"
						+ "SELECT t1.cnt,t2.accno,t2.cik,t2.company,t2.form,t2.filedate FROM TMP_MULTIPE_CIKs2 t1 inner join TMP_MULTIPE_CIKs t2 on t1.accno=t2.accno \n"
						+ "WHERE CNT>1;\n"
						+ "ALTER TABLE TMP_MULTIPLE_CIKS2B ADD KEY(CIK);\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMP_MULTIPLE_CIKS2C;\n"
						+ "CREATE TABLE TMP_MULTIPLE_CIKS2C ENGINE=MYISAM\n"
						+ "SELECT STARTDATE,ENDDATE,T1.* FROM TMP_MULTIPLE_CIKS2b T1 INNER JOIN CIKS_date_range T2 ON T1.CIK=T2.CIK;\n"
						+ "ALTER TABLE TMP_MULTIPLE_CIKS2C ADD KEY(ACCNO),ADD KEY(ENDDATE),ADD KEY(CIK);\n"
						+ "\n"
						+ "set @acc='1x'; \n"
						+ "DROP TABLE IF EXISTS TMP_MULTIPLE_CIKS2D;\n"
						+ "CREATE TABLE TMP_MULTIPLE_CIKS2D ENGINE=MYISAM\n"
						+ "SELECT case when @acc!=accno then 1 else 0 end getIt, startDate,endDate,cnt,@acc:=accno accno,cik,company,form,filedate FROM TMP_MULTIPLE_CIKS2C \n"
						+ "ORDER BY ACCNO,ENDDATE  DESC,CIK DESC;\n"
						+ "ALTER TABLE TMP_MULTIPLE_CIKS2D add key(getIt);\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMP_MULTIPLE_CIKS2E;\n"
						+ "CREATE TABLE TMP_MULTIPLE_CIKS2E ENGINE=MYISAM\n"
						+ "select * from TMP_MULTIPLE_CIKS2D where getIt=1;\n"
						+ "ALTER TABLE TMP_MULTIPLE_CIKS2E ADD KEY(ACCNO);\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS MULTIPLE_CIKS;\n"
						+ "RENAME TABLE TMP_MULTIPLE_CIKS2E TO MULTIPLE_CIKS;\n"
						+ "\n"
						+ "UPDATE ignore \n"
						+ "\n"
						+ " tp_raw_revised T1 \n"
						+ "\n"
						+ "INNER JOIN MULTIPLE_CIKS T2 ON T1.ACCNO=T2.ACCNO \n"
						+ "SET T1.CIK=T2.CIK \n"
						+ "WHERE T1.CIK!=T2.CIK;\n"
						+ "\n"
						+ "\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS sumAllTables;\n"
						+ "create table sumAllTables engine=myisam\n"
						+ "SELECT SUM(ABS(VALUE)) sumAllTables,COUNT(*) CNT,count(distinct(concat(col,tno))) cntCol,CIK,ACCNO,filedate FROM \n"
						+ "\n"
						+ "/*counts total number so I can determine how many rows per col in table.*/\n"
						+ "\n"
						+ " BAC_TP_RAW\n"
						+ "\n"
						+ "WHERE\n"
						+ "ABS(VALUE)>101 /*AND YR!='BAD' AND TN='IS'*/\n"
						+ "and col>0 \n"
						+ "\n"
						+ "GROUP BY ACCNO;\n"
						+ "ALTER TABLE sumAllTables add key(cik), add key(accno), ADD KEY(sumAllTables);\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMP2_sumAllTables;\n"
						+ "create table TMP2_sumAllTables engine=myisam\n"
						+ "select enddate, t1.* from sumAllTables t1 inner join CIKS_date_range t2 on t1.cik=t2.cik;\n"
						+ "ALTER TABLE TMP2_sumAllTables add key(cik), add key(enddate), ADD KEY(sumAllTables);\n"
						+ "\n"
						+ "set @cik=0; set @cnt=0; set @acc='1x'; set @sumAllTables=0.0; set @hhvCik=0; set @fd:='1901-01-01'; set @acc2:='1x';\n"
						+ "set @cik2:=0; set @ck=-1; set @conformToCik=-1;\n"
						+ "\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMP3_sumAllTables;\n"
						+ "CREATE TABLE TMP3_sumAllTables ENGINE=MYISAM\n"
						+ "select accno, enddate,cntcol, @ck:=case when @sumAllTables=sumAllTables and @cnt=cnt \n"
						+ "and (round(cnt/cntCol)>8 or cnt>40) then @ck+1 else 0 end ck,\n"
						+ "@conformToCik:=case when @sumAllTables!=sumAllTables then cik else @conformToCik end conformToCik\n"
						+ "/*Either at least 8 rows w/ abs(value)>101 per col or min of 40 total instances of abs(val)>101 given many small garbage tbls*/\n"
						+ ",case when @ck>=1 then @conformToCik else 0 end conformToUseCik, \n"
						+ "@sumAllTables:=sumAllTables sumAllTables,@cnt:=cnt cnt,@cik:=cik cik\n"
						+ "from TMP2_sumAllTables t1 \n"
						+ "order by sumAllTables,enddate desc,cik desc;\n"
						+ "ALTER TABLE TMP3_sumAllTables add key(ck), add key(accno), add key(cik), add key(conformToUseCik);\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMP4_sumAllTables;\n"
						+ "CREATE TABLE TMP4_sumAllTables ENGINE=MYISAM\n"
						+ "SELECT * FROM TMP3_sumAllTables WHERE CIK!=CONFORMTOUSECIK and ck>0;\n"
						+ "ALTER TABLE TMP4_sumAllTables add key(ck), add key(accno), add key(cik), add key(conformToUseCik);\n"
						+ "\n"
						+ "update ignore\n"
						+ "/*select t1.cik,t2.cik,conformToUseCik,t1.* from*/\n"
						+ "\n" + "tp_raw_revised t1 \n" + "\n"
						+ "inner join TMP4_sumAllTables t2\n"
						+ "on t1.accno=t2.accno \n"
						+ "set t1.cik=conformToUseCik ;\n" + "\n" + "\n"
						+ "DROP TABLE IF EXISTS TMP_MULTIPE_CIKs;\n"
						+ "DROP TABLE IF EXISTS TMP_MULTIPE_CIKs2;\n"
						+ "DROP TABLE IF EXISTS TMP_MULTIPE_CIKs2A;\n"
						+ "DROP TABLE IF EXISTS TMP_MULTIPE_CIKs2B;\n"
						+ "DROP TABLE IF EXISTS TMP_MULTIPE_CIKs2C;\n"
						+ "DROP TABLE IF EXISTS TMP_MULTIPE_CIKs2D;\n"
						+ "DROP TABLE IF EXISTS TMP_MULTIPE_CIKs2E;\n" + "\n"
						+ "DROP TABLE IF EXISTS TMP4_sumAllTables;\n"
						+ "DROP TABLE IF EXISTS TMP3_sumAllTables;\n"
						+ "DROP TABLE IF EXISTS TMP2_sumAllTables;\n"
						+ "DROP TABLE IF EXISTS TMP_sumAllTables;\n" + "\n");

		String dropProc = "DROP PROCEDURE IF EXISTS conformMultipleCIKs;\n"
				+ "CREATE PROCEDURE conformMultipleCIKs()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
		MysqlConnUtils.executeQuery("call conformMultipleCIKs();\n");

	}

	public void conformEnddatesAndRownames(String table, int startYr,
			int endYr, int startQ, int endQ) throws SQLException, FileNotFoundException {

		/*
		 * created for tp_sales_to_scrub2 - runs based on same rowname types (eg
		 * Sales or NI or Cash Flow from Ops) all in one place - based same
		 * rowname types all in one place I sort by cik, edt, p and run queries
		 * to conform rownames (also of course first conforms edts). See fuller
		 * note directly below.
		 */

		int q = 0;
		int yr = 1993;
		StringBuffer sb = new StringBuffer(
				"DROP TABLE IF EXISTS TMP_CONFORM_ENDDATES;\n"
						+ "/*NOTE: This will conform enddates to month end eg: 0630 or 0228 etc. That's done\n"
						+ "entire in the first table - TMP_CONFORM_ENDDATES\n"
						+ "This allows other screens to run where enddates need to match. Such as\n"
						+ "comparing when two values are same on same edt but rownames are different.\n"
						+ "There I know different rownames are interchangeable b/c same values \n"
						+ "on same edt for same period. This method also takes these interchangeable\n"
						+ "rownames and renames the rownames so they are identical. I use the rowame\n"
						+ "that is most often found prior to conforming rownames.*/\n"
						+ "CREATE TABLE TMP_CONFORM_ENDDATES ENGINE=MYISAM\n"
						+ "SELECT accno,form,filedate ,cik,tno,row,\n"
						+ "trim(replace(replace(replace(replace(replace(replace(replace(substring_index(substring_index(substring_index(substring_index(upper(rowname),';SUB',1),';NET',1),';ST',1),';TL',1),';',''),',',''),'  ',' '),'REVENU ','REVENUE'),'TOTAL ',''),'GROSS ',''),'NET ','')) rowname\n"
						+ ",value,p2 ,edt2 ,tn,trow,col\n"
						+ ",CASE WHEN `DEC`=0 OR `DEC` IS NULL OR `DEC`='' THEN 1 WHEN `DEC`=-3 THEN 1000 WHEN `DEC` = -6 THEN 1000000 \n"
						+ "WHEN `DEC`=-9 THEN 1000000000 ELSE `DEC` END `DEC`\n"
						+ "/*,revised*/\n"
						+ ",@mo:=case when edt2 rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})' then right(left(edt2,7),2) else '' end mo\n"
						+ ",@dy:=case when edt2 rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})' then right(edt2,2) else '' end dy\n"
						+ ",@fDay:=case \n"
						+ "when @mo>0 and @mo!=2 and (@dy>=15 or right(edt2,2)='00') and @dy!='' and @mo!='' then 30\n"
						+ "when @mo=2 and (@dy>=15 or right(edt2,2)='00') and @dy!='' and @mo!='' then 28\n"
						+ "when @mo!=3 and @dy<15 and @dy!='' and @mo!='' then 30 \n"
						+ "when @mo=3 and @dy<15 and @dy!='' and @mo!='' then 28 else '' end fDay\n"
						+ ",@fMo:=case \n"
						+ "when @dy>=15 or right(edt2,2)='00' or @dy='' then @mo\n"
						+ "when @dy<15 and @mo>1 then @mo-1\n"
						+ "when @dy<15 and @mo<2 then 12 else @mo end fMo\n"
						+ ",@fnlMo:=case when length(@fMo)=1 then concat('0',@fMo) when @fMo between 1 and 12 then @fMo else @mo end fnlMo\n"
						+ ",@fnlDay:=case when length(@fDay)=1 then concat('0',@fDay) else @fDay end fnlDay\n"
						+ ",LEFT(concat(@fnlMo,'-',@fnlDay),5) q_end\n"
						+ ",@yr:=case when @mo<2 and @dy<15 and @mo!='' and @dy!='' then left(edt2,4)-1 else left(edt2,4) end yr\n"
						+ ",LEFT(CONCAT(@yr,'-',LEFT(concat(@fnlMo,'-',@fnlDay),5)),10) enddate\n"
						+ ",edt2 eOrig\n" + " FROM "
						+ table
						+ " where \n"
						+ " value>0 and p2 between 3 and 12 \n"
						+ " order by cik,edt2;\n"
						+ "ALTER TABLE TMP_CONFORM_ENDDATES ADD KEY(ACCNO), \n"
						+ "DROP COLUMN FNLDAY, DROP COLUMN FNLMO, DROP COLUMN MO, DROP COLUMN DY, DROP COLUMN FMO, DROP COLUMN FDAY, DROP COLUMN EDT2,\n"
						+ "CHANGE P2 p2 TINYINT(3), CHANGE FILEDATE filedate DATETIME,CHANGE enddate edt2 VARCHAR(12), change eOrig eOrig varchar(10),\n"
						+ "ADD KEY(edt2), ADD KEY(p2),DROP COLUMN Q_END;\n"
						+ "\n"
						+ "SET @cnt=0; set @cik=0; set @rn='x'; set @tno=0; set @acc='1x'; set @rnCnt=0;\n"
						+ "\n"
						+ "/*this counts (cnt) so that the last value is the max value for each rowname\n"
						+ " type - rnCnt is the current count of the unique left(rowname,5). \n"
						+ "@cnt is the count of that unique rowname. this way I can take max value for\n"
						+ " each cik, rnCnt.*/\n"
						+ "\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES;\n"
						+ "CREATE TABLE TP_SALES_ODD_ROWNAMES ENGINE=MYISAM\n"
						+ "SELECT @cnt:=case when @cik!=cik then 1 when left(trim(rowname),7)!=@rn then 1 when @tno!=tno or @acc!=accno then\n"
						+ "@cnt+1 else @cnt end cnt\n"
						+ ",@rnCnt:=case when @cik=cik and left(trim(rowname),7)!=@rn then @rnCnt+1 when @cik!=cik then 1 else @rnCnt end rnCnt\n"
						+ "/*rnCt reflect each unique rowname. If I take max cnt for each rnct for each CIK I can get total count.*/\n"
						+ ",@cik:=cik cik,@rn:=left(trim(replace(rowname,'revenu ','revenue')),7) rn7,\n"
						+ "/*if I change how much of rowname to match - search ',*/\n"
						+ "case when ROWNAME RLIKE '(TOTAL |GROSS |NET |OPERATING )?(SALES|REVENU)|^revenu|^sales' AND rowname not rlike 'real|contract' then 'R'\n"
						+ "when TRIM(rowname) rlike '^income:?$|^INCOME.{1,3}NOTE|^income on|gross income|(TOTAL |GROSS )?(INTEREST |INVESTMENT )(AND OTHER |AND FEE )?INCOME|total income (on |from )|^total ? (income:$|income$)|^total interest|interest and (related |dividend )income' then 'I' \n"
						+ "when trim(rowname) rlike 'dividend income' then 'D'\n"
						+ "when length(trim(rowname))<=1 then 'B'\n"
						+ "when trim(rowname) rlike 'premium' then 'P'\n"
						+ "when trim(rowname) = 'total'  or rowname rlike 'total.{1,3}note' then 'T'\n"
						+ "when  rowname rlike '^\\\\$|^\\\\(|^[0-9]|^(from|and|operations|ended|in) ' and rowname not rlike 'revenu|sale|income' then 'F' end R \n"
						+ ",@tno:=tno tno,@acc:=accno accno, filedate,tn,trow,row,col,rowname,value,p2,edt2,`dec`/*,columntext,form,revised*/ ,eOrig\n"
						+ "\n"
						+ "FROM TMP_CONFORM_ENDDATES t1\n"
						+ "\n"
						+ "ORDER BY CIK,LEFT(TRIM(ROWNAME),7),filedate ;\n"
						+ "ALTER TABLE TP_SALES_ODD_ROWNAMES ADD KEY(CIK), ADD KEY(rnCnt);\n"
						+ "\n"
						+ "\n"
						+ "/*get max cnt for each rowname type*/\n"
						+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES2;\n"
						+ "CREATE TABLE TP_SALES_ODD_ROWNAMES2 ENGINE=MYISAM\n"
						+ "SELECT MAX(CNT) mxRnC,max(trow) mxTr,min(trow) mnTr,rnCnt,rn7,r,cik FROM TP_SALES_ODD_ROWNAMES T1 GROUP BY CIK,RNCNT;\n"
						+ "ALTER TABLE TP_SALES_ODD_ROWNAMES2 ADD KEY(CIK),ADD KEY(rnCnt);\n"
						+ "\n"
						+ "/*joins max cnt for each rn type to each row of original tp_sales_to_scrub2 table reformatted as tp_sales_odd_rownames3.*/\n"
						+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES3;\n"
						+ "CREATE TABLE TP_SALES_ODD_ROWNAMES3 ENGINE=MYISAM\n"
						+ "SELECT T2.mxRnC,t2.mxtr,t2.mntr,\n"
						+ "t1.*\n"
						+ "FROM TP_SALES_ODD_ROWNAMES T1 INNER JOIN TP_SALES_ODD_ROWNAMES2 T2 ON T1.CIK=T2.CIK AND T1.RNCNT=T2.RNCNT;\n"
						+ "ALTER TABLE TP_SALES_ODD_ROWNAMES3 ADD KEY(CIK),ADD KEY(rnCnt), add key(mxRnC);\n"
						+ "\n"
						+ "/*join max rn count of the cik to each row. if there are 3 rowname types - this find the rn type with highest count and marks it mxCikC.\n"
						+ "This compared to mxRnC shows which are odd balls rownames (rn).*/\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES4;\n"
						+ "CREATE TABLE TP_SALES_ODD_ROWNAMES4 ENGINE=MYISAM\n"
						+ "select max(mxRnC) mxCikC, cik from TP_SALES_ODD_ROWNAMES3 t1 group by cik;\n"
						+ "ALTER TABLE TP_SALES_ODD_ROWNAMES4 add key(cik);\n"
						+ "\n"
						+ "\n"
						+ "/*joins odd rows to original tp_sales_to_scrub data from its reformatted table tp_sales_odd_rownames3 and calcs odd row percentage*/\n"
						+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES5;\n"
						+ "CREATE TABLE TP_SALES_ODD_ROWNAMES5 ENGINE=MYISAM\n"
						+ "SELECT ROUND((T1.mxRnC/T2.mxCikC),2) oP,t2.mxCikC,t1.* FROM TP_SALES_ODD_ROWNAMES3 T1 INNER JOIN TP_SALES_ODD_ROWNAMES4 T2 ON T1.CIK=T2.CIK ;\n"
						+ "ALTER TABLE TP_SALES_ODD_ROWNAMES5 ADD KEY(CIK), add key(tno), add key(accno);\n"
						+ "\n"
						+ "\n"
						+ "set @c:=0; set @p=0; set @e:='1901-01-01'; set @eOrig:='1901-01-01'; set @mxCikC = 0; set @mxRnC=0; set @v=0; set @rn='x'; set @rn2='x'; set @op=0;\n"
						+ "/*when ck=2 - it denotes an odd rowname and a value that is not equal to the same value for the same edt2/p2 for a non-odd rowname. the fV, vP2, fEdt2 are the correct values from the prior filing which should also be in the filing with odd rowname but on a different row. So next query\n"
						+ "will use odd rowname accno to find in same tno the trow that has the matching fP2,fV, fEdt2. That is then used to replace the odd rowname \n"
						+ "value in this instance (delete that accno's value and replace with found).*/\n"
						+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES6;\n"
						+ "CREATE TABLE TP_SALES_ODD_ROWNAMES6 ENGINE=MYISAM\n"
						+ "select \n"
						+ "case when op<1 and @e=edt2 and @p=p2 and \n"
						+ "(@v=value or round(greatest(@v,value)/least(@v,value))=1000 or round(greatest(@v,value)/least(@v,value))=1000000 ) and @mxCikC=@mxRnC then 1 else 0 end mt,\n"
						+ "@f:=case when op!=1 and @e=edt2 and @p=p2 and \n"
						+ "(@v=value or round(greatest(@v,value)/least(@v,value))=1000 or round(greatest(@v,value)/least(@v,value))=1000000 ) and @mxRnC=@mxCikC then 1 when op!=1 and @e=edt2 and @p=p2 and @v!=value and @mxRnC=@mxCikC then 2 \n"
						+ "else 0 end ck\n"
						+ ",case when @f=2 then @v else 0 end fV\n"
						+ ",case when @f=2 then @p else 0 end fP2\n"
						+ ",case when @f=2 then @eOrig else 0 end fEdt2\n"
						+ ",case when @f=2 then @rn else 0 end fRn\n"
						+ "/*if 2 - then values don't match for different rowname types. these I'll have\n"
						+ " to try and fetch from table by matching value*/\n"
						+ ", @rn2:=CASE when op!=1 and @e=edt2 and @p=p2 and\n"
						+ " (@v=value or round(greatest(@v,value)/least(@v,value))=1000 or round(greatest(@v,value)/least(@v,value))=1000000 ) \n"
						+ "and @op=1 then @rn2 else rowname end rowname\n"
						+ "/*if ck=1 then current rowname is odd rowname and prior is non-odd. this grabs non-odd rowname and replaces odd. In next query\n"
						+ "update rowname by joining where ck=1 and rnCnt=rnCnt.*/\n"
						+ "\n"
						+ ",@op:=t1.oP op, @mxCikC:=t1.mxCikC mxCikC, @mxRnC:=t1.mxRnC mxRnC, t1.mxtr, t1.mntr, t1.cnt, t1.rnCnt, @cik:=t1.cik cik, t1.rn7, t1.R\n"
						+ ", t1.tno, t1.accno, t1.filedate, t1.tn, t1.trow, t1.row,t1.col, @v:=t1.value value,@rn:=rowname rn, @p:=t1.p2 p2,@e:=t1.edt2 edt2\n"
						+ ",@eOrig:=eOrig eOrig\n"
						+ ", t1.`dec`/*, t1.revised*/ from TP_SALES_ODD_ROWNAMES5 t1 \n"
						+ "order by cik,edt2,p2,OP desc;\n"
						+ "\n"
						+ "ALTER TABLE TP_SALES_ODD_ROWNAMES6 ADD KEY(CIK), add key(tno), add key(accno), change fv fv double(23,2), change fedt2 fedt2 varchar(11),\n"
						+ "change fRn fRn varchar(255), change op op double(8,2),change mxcikc mxcikc double,change mxrnc mxrnc double, change cnt cnt double\n"
						+ ", change rncnt rncnt double,change fp2 fp2 tinyint(3) , add key(fp2), add key(fv), add key(fedt2), change rowname rowname varchar(255);\n"
						+ "\n"
						+ "/*select t1.ck, t1.fv, t1.fp2, t1.fedt2, t1.fRn, t1.rowname, t1.op, t1.tno,\n"
						+ " t1.accno, t1.filedate, t1.tn, t1.trow, t1.row, t1.col, t1.value, t1.rn,\n"
						+ " t1.p2, t1.edt2, t1.eOrig, t1.`dec`, t1.revised from TP_SALES_ODD_ROWNAMES6\n"
						+ " t1 order by edt2,p2;*/\n"
						+ "\n"
						+ "/*THIS GETS EACH INSTANCE OF A ODD ROWNAME (RNCNT) THAT HAD IT ROWNAME\n"
						+ " CHANGED TO A NON-ODD BALL ROWNAME ABOVE (CK=1).*/\n"
						+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES6A;\n"
						+ "CREATE TABLE TP_SALES_ODD_ROWNAMES6A ENGINE=MYISAM\n"
						+ "select CIK,RNCNT,ROWNAME from TP_SALES_ODD_ROWNAMES6 t1 where ck=1 group by cik,rncnt;\n"
						+ "ALTER TABLE TP_SALES_ODD_ROWNAMES6A ADD KEY(CIK), ADD KEY(RNCNT);\n"
						+ "\n"
						+ "\n"
						+ "/*UPDATES ALL RNCNT EQUAL TO THE ONES THAT WERE CHANGED ABOVE FOR EACH CIK - ALL ROWNAMES OF SAME TYPE UPDATED*/\n"
						+ "UPDATE IGNORE TP_SALES_ODD_ROWNAMES6 T1 INNER JOIN TP_SALES_ODD_ROWNAMES6A t2 on t1.cik=t2.cik and t1.rncnt=t2.rncnt\n"
						+ "SET T1.ROWNAME=T2.ROWNAME\n"
						+ "where t1.ck!=1;\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES6B;\n"
						+ "CREATE TABLE TP_SALES_ODD_ROWNAMES6B ENGINE=MYISAM\n"
						+ "SELECT T1.ACCNO,T1.TNO,T1.ROW,T2.ROWNAME FROM TP_SALES_ODD_ROWNAMES6 T1 INNER JOIN TP_SALES_ODD_ROWNAMES6A t2 on t1.cik=t2.cik and t1.rncnt=t2.rncnt\n"
						+ "where t1.ck!=1;\n"
						+ "ALTER TABLE TP_SALES_ODD_ROWNAMES6B ADD KEY(ACCNO), ADD KEY(TNO), ADD KEY(ROW);\n"
						+ "\n"
						+ "/*NOW I HAVE TO UPDATE ROWNAME IN TP_SALES_TO_SCRUB2 or table passed */\n"
						+ "UPDATE IGNORE "
						+ table
						+ " T1 INNER JOIN TP_SALES_ODD_ROWNAMES6B t2 on T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T2.ROW=T2.ROW\n"
						+ "SET T1.ROWNAME=T2.ROWNAME;\n"
						+ "\n"
						+ "\n"
						+ "/*THIS GETS ALL CORRECT ROWS BASED ON NON-ODD ROWNAME. HOWEVER I ONLY NEED THE TROW - SO THAT LATER I FETCH ALL ROWS FOR THAT TROW IF\n"
						+ "EDT2/P2 VALUES ARE GOOD*/\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES7;\n"
						+ "CREATE TABLE `tp_sales_odd_rownames7` (\n"
						+ "  `accno` varchar(20) NOT NULL DEFAULT '-1',\n"
						+ "  `tno` int(5) NOT NULL DEFAULT '-1',\n"
						+ "  `TROW` tinyint(2) DEFAULT NULL COMMENT 'row number in financial table',\n"
						+ "  `ROWNAME` varchar(255) DEFAULT NULL,\n"
						+ "  KEY `accno` (`accno`),\n"
						+ "  KEY `TNO` (`TNO`),\n"
						+ "  KEY `TROW` (`TROW`)\n"
						+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES8;\n"
						+ "CREATE TABLE `tp_sales_odd_rownames8` (\n"
						+ "  `accno` varchar(20) NOT NULL DEFAULT '-1',\n"
						+ "  `fileDate` datetime DEFAULT NULL,\n"
						+ "  `cik` int(11) DEFAULT NULL,\n"
						+ "  `tn` varchar(6) DEFAULT NULL,\n"
						+ "  `trow` tinyint(2) DEFAULT NULL COMMENT 'row number in financial table',\n"
						+ "  `row` int(5) NOT NULL DEFAULT '-1' COMMENT 'table row',\n"
						+ "  `col` tinyint(2) DEFAULT NULL COMMENT 'data col number in financial table',\n"
						+ "  `tno` int(5) NOT NULL DEFAULT '-1',\n"
						+ "  `rowname` varchar(255) DEFAULT NULL,\n"
						+ "  `value` double(23,5) DEFAULT NULL,\n"
						+ "  `p2` int(3) DEFAULT NULL COMMENT 'if html - per2 based on col hdg ratio matching, if txt based on idx alignments of each match',\n"
						+ "  `edt2` varchar(11) DEFAULT NULL COMMENT ' same as per2',\n"
						+ "  `DEC` int(11) DEFAULT NULL,\n"
						+ "  `columnText` varchar(255) DEFAULT NULL COMMENT 'shows this col nos text used for edt2. ',\n"
						+ "  `form` varchar(15) DEFAULT NULL COMMENT 'this will equal rowratioBeforeColumnUtil if generic in htmlTxt field',\n"
						/*
						 * +
						 * "  `revised` varchar(9) CHARACTER SET utf8 NOT NULL DEFAULT '',\n"
						 */
						+ "  KEY `accno` (`accno`),\n"
						+ "  KEY `tno` (`tno`),\n"
						+ "  KEY `trow` (`trow`)\n"
						+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n"
						+ "\n\n\n");

		int qtr = startQ;
		q = startQ;
		for (yr = startYr; yr <= endYr; yr++) {
			if (yr == 1993 && qtr < 3) {
				qtr = 3;
			}
			for (q = qtr; q <= endQ; q++) {
				sb.append("\ninsert ignore into TP_SALES_ODD_ROWNAMES7 \n"
						+ "SELECT T2.accno, T2.TNO,T2.TROW,T2.ROWNAME \n"
						+ "FROM TP_SALES_ODD_ROWNAMES6 T1 INNER JOIN \n" + "\n"
						+ "bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ "\n"
						+ "T2 ON T1.ACCNO=T2.ACCNO \n"
						+ "AND T1.fV=t2.value \n"
						+ "and (t1.fp2=t2.P2 or t1.fp2=t2.p1) /*p1 confirmed by fp2*/\n"
						+ "and\n"
						+ "(left(t1.fEdt2,7)=left(t2.Edt2,7) or left(t1.fEdt2,7)=left(t2.Edt1,7)) /*fedt2 confirms edt1*/\n"
						+ "WHERE CK=2 \n"
						+ "\n"
						+ "GROUP BY ACCNO,TNO,TROW;\n"
						+ "\n"
						+ "\n"
						+ "/*THIS GETS ENTIRE CORRECT ROW OF CORRECT ROWNAME. THESE WILL BE INSERT INTO TP_SALES_TO_SCRUB2 AS IS AFTER I DELETE THE IDENTICAL ACCNO,TNO\n"
						+ "CURRENT IN TP_SALES_TO_SCRUB2*/\n"
						+ "\n"
						+ "insert ignore into TP_SALES_ODD_ROWNAMES8 \n"
						+ "SELECT T2.accno, fileDate, cik, tn, T2.trow, row, col, T2.tno, T2.rowname, value, p2, edt2, `DEC`, columnText, form\n"
						+ "/*,case when (allColText like '%revise%' or t1.ColumnText like '%revise%' or ColumnPattern like '%revise%' or allColText like '%restate%' or t1.ColumnText like '%restate%' or ColumnPattern like '%restate%' or allColText like '%adjust%' or t1.ColumnText like '%adjust%' or ColumnPattern like '%adjust%') then 'restated' when (htmltxt='html' and t1.ColumnText like '%pro%forma%') or ((allColText like '%pro%forma%' or ColumnPattern like '%pro%forma%' or t1.ColumnText like '%pro%forma%') and htmlTxt!='html') then 'pro forma' else 'actual' end revised*/ FROM \n"
						+ "bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ " t1 \n"
						+ "\n"
						+ "INNER JOIN TP_SALES_ODD_ROWNAMES7 T2 ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.TROW=T2.TROW\n"
						+ "where t1.value>0 and t1.yr!='bad' and length(edt2)=10 and (p2=3 or p2=6 or p2=9 or p2=12 ) \n"
						+ "and edt2 rlike '[12]{1}[09]{1}[0-9]{2}-(1[012]{1}|0[0-9]{1})'\n"
						+ ";\n");
			}
			qtr = 1;
		}
		sb.append("\n\n\n"
				+ "\n"
				+ "/*now I delete those accno where I found a match from tp_sales_to_scrub2 and insert all from the trow where match was found*/\n"
				+ "DELETE T1 FROM "
				+ table
				+ " T1 INNER JOIN TP_SALES_ODD_ROWNAMES8 T2 ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO;\n"
				+ "\n"
				+ "INSERT IGNORE INTO "
				+ table
				+ "\n"
				+ "SELECT t1.*,'' FROM TP_SALES_ODD_ROWNAMES8 t1;\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES7;\n"
				+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES8;\n"
				+ "\n"
				+ "\n"
				+ " /*this 2nd loop gets finds two rownames that are different and both are odd\n"
				+ " but have the same value and them homogenizes them by using one rowname for\n"
				+ " all. This helps later when I run tp_sales y-o-y comparison when restriction\n"
				+ " to same Rowname when I join across multiple accno*/\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP_CONFORM_ENDDATES;\n"
				+ "/*NOTE: This will conform enddates to month end eg: 0630 or 0228 etc. That's done\n"
				+ "entire in the first table - TMP_CONFORM_ENDDATES\n"
				+ "This allows other screens to run where enddates need to match. Such as\n"
				+ "comparing when two values are same on same edt but rownames are different.\n"
				+ "There I know different rownames are interchangeable b/c same values \n"
				+ "on same edt for same period. This method also takes these interchangeable\n"
				+ "rownames and renames the rownames so they are identical. I use the rowame\n"
				+ "that is most often found prior to conforming rownames.*/\n"
				+ "CREATE TABLE TMP_CONFORM_ENDDATES ENGINE=MYISAM\n"
				+ "SELECT accno,form,filedate ,cik,tno,row,\n"
				+ "trim(replace(replace(replace(replace(replace(replace(replace(substring_index(substring_index(substring_index(substring_index(upper(rowname),';SUB',1),';NET',1),';ST',1),';TL',1),';',''),',',''),'  ',' '),'REVENU ','REVENUE'),'TOTAL ',''),'GROSS ',''),'NET ','')) rowname \n"
				+ ",value,p2 ,edt2 ,tn,trow,col\n"
				+ ",CASE WHEN `DEC`=0 OR `DEC` IS NULL OR `DEC`='' THEN 1 WHEN `DEC`=-3 THEN 1000 WHEN `DEC` = -6 THEN 1000000 \n"
				+ "WHEN `DEC`=-9 THEN 1000000000 ELSE `DEC` END `DEC`\n"
				+ "/*,revised\n*/"
				+ ",@mo:=case when edt2 rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})' then right(left(edt2,7),2) else '' end mo\n"
				+ ",@dy:=case when edt2 rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})' then right(edt2,2) else '' end dy\n"
				+ ",@fDay:=case \n"
				+ "when @mo>0 and @mo!=2 and (@dy>=15 or right(edt2,2)='00') and @dy!='' and @mo!='' then 30\n"
				+ "when @mo=2 and (@dy>=15 or right(edt2,2)='00') and @dy!='' and @mo!='' then 28\n"
				+ "when @mo!=3 and @dy<15 and @dy!='' and @mo!='' then 30 \n"
				+ "when @mo=3 and @dy<15 and @dy!='' and @mo!='' then 28 else '' end fDay\n"
				+ ",@fMo:=case \n"
				+ "when @dy>=15 or right(edt2,2)='00' or @dy='' then @mo\n"
				+ "when @dy<15 and @mo>1 then @mo-1\n"
				+ "when @dy<15 and @mo<2 then 12 else @mo end fMo\n"
				+ ",@fnlMo:=case when length(@fMo)=1 then concat('0',@fMo) when @fMo between 1 and 12 then @fMo else @mo end fnlMo\n"
				+ ",@fnlDay:=case when length(@fDay)=1 then concat('0',@fDay) else @fDay end fnlDay\n"
				+ ",LEFT(concat(@fnlMo,'-',@fnlDay),5) q_end\n"
				+ ",@yr:=case when @mo<2 and @dy<15 and @mo!='' and @dy!='' then left(edt2,4)-1 else left(edt2,4) end yr\n"
				+ ",LEFT(CONCAT(@yr,'-',LEFT(concat(@fnlMo,'-',@fnlDay),5)),10) enddate\n"
				+ ",edt2 eOrig\n"
				+ " FROM "
				+ table
				+ " where \n"
				+ " value>0 and p2 between 3 and 12 \n"
				+ " order by cik,edt2;\n"
				+ "ALTER TABLE TMP_CONFORM_ENDDATES ADD KEY(ACCNO), \n"
				+ "DROP COLUMN FNLDAY, DROP COLUMN FNLMO, DROP COLUMN MO, DROP COLUMN DY, DROP COLUMN FMO, DROP COLUMN FDAY, DROP COLUMN EDT2,\n"
				+ "CHANGE P2 p2 TINYINT(3), CHANGE FILEDATE filedate DATETIME,CHANGE enddate edt2 VARCHAR(12), change eOrig eOrig varchar(10),\n"
				+ "ADD KEY(edt2), ADD KEY(p2),DROP COLUMN Q_END;\n"
				+ "\n"
				+ "\n"
				+ "SET @cnt=0; set @cik=0; set @rn='x'; set @tno=0; set @acc='1x'; set @rnCnt=0;\n"
				+ "\n"
				+ "/*this counts (cnt) so that the last value is the max value for each rowname\n"
				+ " type - rnCnt is the current count of the unique left(rowname,5). \n"
				+ "@cnt is the count of that unique rowname. this way I can take max value for\n"
				+ " each cik, rnCnt.*/\n"
				+ "\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES;\n"
				+ "CREATE TABLE TP_SALES_ODD_ROWNAMES ENGINE=MYISAM\n"
				+ "SELECT @cnt:=case when @cik!=cik then 1 when left(trim(rowname),7)!=@rn then 1 when @tno!=tno or @acc!=accno then\n"
				+ "@cnt+1 else @cnt end cnt\n"
				+ ",@rnCnt:=case when @cik=cik and left(trim(rowname),7)!=@rn then @rnCnt+1 when @cik!=cik then 1 else @rnCnt end rnCnt\n"
				+ "/*rnCt reflect each unique rowname. If I take max cnt for each rnct for each CIK I can get total count.*/\n"
				+ ",@cik:=cik cik,@rn:=left(trim(replace(rowname,'revenu ','revenue')),7) rn7,\n"
				+ "/*if I change how much of rowname to match - search ',*/\n"
				+ "case when ROWNAME RLIKE '(TOTAL |GROSS |NET |OPERATING )?(SALES|REVENU)|^revenu|^sales' AND rowname not rlike 'real|contract' then 'R'\n"
				+ "when TRIM(rowname) rlike '^income:?$|^INCOME.{1,3}NOTE|^income on|gross income|(TOTAL |GROSS )?(INTEREST |INVESTMENT )(AND OTHER |AND FEE )?INCOME|total income (on |from )|^total ? (income:$|income$)|^total interest|interest and (related |dividend )income' then 'I' \n"
				+ "when trim(rowname) rlike 'dividend income' then 'D'\n"
				+ "when length(trim(rowname))<=1 then 'B'\n"
				+ "when trim(rowname) rlike 'premium' then 'P'\n"
				+ "when trim(rowname) = 'total'  or rowname rlike 'total.{1,3}note' then 'T'\n"
				+ "when  rowname rlike '^\\\\$|^\\\\(|^[0-9]|^(from|and|operations|ended|in) ' and rowname not rlike 'revenu|sale|income' then 'F' end R \n"
				+ ",@tno:=tno tno,@acc:=accno accno, filedate,tn,trow,row,col,rowname,value,p2,edt2,`dec`/*,columntext,form,revised*/ ,eOrig\n"
				+ "\n"
				+ "FROM TMP_CONFORM_ENDDATES t1\n"
				+ "\n"
				+ "ORDER BY CIK,LEFT(TRIM(ROWNAME),7),filedate ;\n"
				+ "ALTER TABLE TP_SALES_ODD_ROWNAMES ADD KEY(CIK), ADD KEY(rnCnt);\n"
				+ "\n"
				+ "\n"
				+ "/*get max cnt for each rowname type*/\n"
				+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES2;\n"
				+ "CREATE TABLE TP_SALES_ODD_ROWNAMES2 ENGINE=MYISAM\n"
				+ "SELECT MAX(CNT) mxRnC,max(trow) mxTr,min(trow) mnTr,rnCnt,rn7,r,cik FROM TP_SALES_ODD_ROWNAMES T1 GROUP BY CIK,RNCNT;\n"
				+ "ALTER TABLE TP_SALES_ODD_ROWNAMES2 ADD KEY(CIK),ADD KEY(rnCnt);\n"
				+ "\n"
				+ "/*joins max cnt for each rn type to each row of original tp_sales_to_scrub2 table reformatted as tp_sales_odd_rownames3.*/\n"
				+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES3;\n"
				+ "CREATE TABLE TP_SALES_ODD_ROWNAMES3 ENGINE=MYISAM\n"
				+ "SELECT T2.mxRnC,t2.mxtr,t2.mntr,\n"
				+ "t1.*\n"
				+ "FROM TP_SALES_ODD_ROWNAMES T1 INNER JOIN TP_SALES_ODD_ROWNAMES2 T2 ON T1.CIK=T2.CIK AND T1.RNCNT=T2.RNCNT;\n"
				+ "ALTER TABLE TP_SALES_ODD_ROWNAMES3 ADD KEY(CIK),ADD KEY(rnCnt), add key(mxRnC);\n"
				+ "\n"
				+ "/*join max rn count of the cik to each row. if there are 3 rowname types - this find the rn type with highest count and marks it mxCikC.\n"
				+ "This compared to mxRnC shows which are odd balls rownames (rn).*/\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES4;\n"
				+ "CREATE TABLE TP_SALES_ODD_ROWNAMES4 ENGINE=MYISAM\n"
				+ "select max(mxRnC) mxCikC, cik from TP_SALES_ODD_ROWNAMES3 t1 group by cik;\n"
				+ "ALTER TABLE TP_SALES_ODD_ROWNAMES4 add key(cik);\n"
				+ "\n"
				+ "/*joins odd rows to original tp_sales_to_scrub data from its reformatted table tp_sales_odd_rownames3 and calcs odd row percentage*/\n"
				+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES5;\n"
				+ "CREATE TABLE TP_SALES_ODD_ROWNAMES5 ENGINE=MYISAM\n"
				+ "SELECT ROUND((T1.mxRnC/T2.mxCikC),2) oP,t2.mxCikC,t1.* FROM TP_SALES_ODD_ROWNAMES3 T1 INNER JOIN TP_SALES_ODD_ROWNAMES4 T2 ON T1.CIK=T2.CIK ;\n"
				+ "ALTER TABLE TP_SALES_ODD_ROWNAMES5 ADD KEY(CIK), add key(tno), add key(accno);\n"
				+ "\n"
				+ "set @c:=0; set @p=0; set @e:='1901-01-01'; set @eOrig:='1901-01-01'; set @mxCikC = 0; set @mxRnC=0; set @v=0; set @rn='x'; set @rn2='x'; set @op=0;\n"
				+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES6;\n"
				+ "CREATE TABLE TP_SALES_ODD_ROWNAMES6 ENGINE=MYISAM\n"
				+ "select \n"
				+ "@f:=case when op!=1 and @e=edt2 and @p=p2\n"
				+ "and (@v=value or round(greatest(@v,value)/least(@v,value))=1000 or round(greatest(@v,value)/least(@v,value))=1000000 ) and @mxCikC!=@mxRnC \n"
				+ "and @rn!=rowname then 3 \n"
				+ "else 0 end ck\n"
				+ ",case when @f=3 then @rn else 0 end fRn\n"
				+ "/*if ck = 3 -  we have two oddballs rowanems that are same value. I homogenize the 2 rownames by keeping the one that occurs most frequently\n"
				+ "(sort by op desc). In next query I join where ck=3 and rnCnt=rnCnt and set rowname=fRn (fRn=rowname of prior row that matched).*/\n"
				+ ",@op:=t1.oP op, @mxCikC:=t1.mxCikC mxCikC, @mxRnC:=t1.mxRnC mxRnC, t1.mxtr, t1.mntr, t1.cnt, t1.rnCnt, @cik:=t1.cik cik, t1.rn7, t1.R\n"
				+ ", t1.tno, t1.accno, t1.filedate, t1.tn, t1.trow, t1.row,t1.col, @v:=t1.value value,@rn:=rowname rowname, @p:=t1.p2 p2,@e:=t1.edt2 edt2\n"
				+ ",@eOrig:=eOrig eOrig\n"
				+ ", t1.`dec`/*, t1.revised*/ from TP_SALES_ODD_ROWNAMES5 t1 \n"
				+ "order by cik,edt2,p2,OP desc;\n"
				+ "\n"
				+ "ALTER TABLE TP_SALES_ODD_ROWNAMES6 ADD KEY(CIK), add key(tno), add key(accno), change fRn fRn varchar(255), change op op double(8,2),change mxcikc mxcikc double,change mxrnc mxrnc double, change cnt cnt double, change rncnt rncnt double;\n"
				+ "\n"
				+ "/*THESE ARE ROWNAMES THAT NEED TO BE CHANGED.*/\n"
				+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES6A;\n"
				+ "CREATE TABLE TP_SALES_ODD_ROWNAMES6A ENGINE=MYISAM\n"
				+ "select CIK,RNCNT,fRn rowname from TP_SALES_ODD_ROWNAMES6 t1 where ck=3 group by cik,rncnt;\n"
				+ "ALTER TABLE TP_SALES_ODD_ROWNAMES6A ADD KEY(CIK), ADD KEY(RNCNT);\n"
				+ "\n"
				+ "/*UPDATES ALL RNCNT EQUAL TO THE ONES THAT WERE CHANGED ABOVE FOR EACH CIK - ALL ROWNAMES OF SAME TYPE UPDATED*/\n"
				+ "UPDATE IGNORE TP_SALES_ODD_ROWNAMES6 T1 INNER JOIN TP_SALES_ODD_ROWNAMES6A t2 on t1.cik=t2.cik and t1.rncnt=t2.rncnt\n"
				+ "SET T1.rowname=T2.ROWNAME;\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TP_SALES_ODD_ROWNAMES6B;\n"
				+ "CREATE TABLE TP_SALES_ODD_ROWNAMES6B ENGINE=MYISAM\n"
				+ "SELECT T1.ACCNO,T1.TNO,T1.ROW,T2.ROWNAME FROM TP_SALES_ODD_ROWNAMES6 T1 INNER JOIN TP_SALES_ODD_ROWNAMES6A t2 on t1.cik=t2.cik and t1.rncnt=t2.rncnt\n"
				+ "where t1.ck!=1;\n"
				+ "ALTER TABLE TP_SALES_ODD_ROWNAMES6B ADD KEY(ACCNO), ADD KEY(TNO), ADD KEY(ROW);\n"
				+ "\n"
				+ "/*NOW I HAVE TO UPDATE ROWNAME IN TP_SALES_TO_SCRUB2 or table passed */\n"
				+ "UPDATE IGNORE "
				+ table
				+ " T1 INNER JOIN TP_SALES_ODD_ROWNAMES6B t2 on T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T2.ROW=T2.ROW\n"
				+ "SET T1.ROWNAME=T2.ROWNAME;\n");

		String dropProc = "DROP PROCEDURE IF EXISTS conformEnddatesAndRownames;\n"
				+ "CREATE PROCEDURE conformEnddatesAndRownames()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
		MysqlConnUtils.executeQuery("call conformEnddatesAndRownames();\n");
		// System.out.println(dropProc + sb.toString() +
		// endProcy";\n\ncall conformEnddatesAndRownames();\n");

	}

	public void regenerateBac_TP_RawYYYYQtrNo(String table, int cikStart,
			int cikEnd) throws SQLException, FileNotFoundException {

		String yr = table.substring(table.indexOf("tp_raw") + 6,
				table.indexOf("tp_raw") + 10);
		String qq = table.substring(table.indexOf("tp_raw") + 13,
				table.indexOf("tp_raw") + 14);

		int q = Integer.parseInt(qq);

//		TableParser tp = new TableParser();
		String moS = "", dyS = "01", moE = "", dyE = "";

		if (q == 1) {
			moS = "01";
			moE = "03";
			dyE = "31";
		}
		if (q == 2) {
			moS = "04";
			moE = "06";
			dyE = "30";
		}
		if (q == 3) {
			moS = "07";
			moE = "09";
			dyE = "30";
		}
		if (q == 4) {
			moS = "10";
			moE = "12";
			dyE = "31";
		}

		String qry = "insert ignore into bac_tp_raw\r" + "select * from "
				+ table + " where year(filedate)=" + yr + ";";

		MysqlConnUtils.executeQuery(qry);

		NLP.createTPrawTable(table);

		qry = "DROP TABLE IF EXISTS TMP_accTnoCol"
				+ yr
				+ "qtr"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP_accTnoCol"
				+ yr
				+ "qtr"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "SELECT ACCNO,TNO,COL,max(abs(value)) SUMVALUE,EDT1,EDT2,P1,P2,YR,ENDED,TC,COLUMNPATTERN,COLUMNTEXT,ALLCOLTEXT,HTMLTXT,FORM\n"
				+ ",TSSHORT,TSLONG FROM bac_tp_raw t1 \n"
				+ "where filedate between \n"
				+ "'"
				+ yr
				+ "-"
				+ moS
				+ "-"
				+ dyS
				+ "' and '"
				+ yr
				+ "-"
				+ moE
				+ "-"
				+ dyE
				+ "' and cik between "
				+ cikStart
				+ " and "
				+ cikEnd
				+ " \n"
				+ "group by accno,tno,col;\n"
				+ "\n"
				+ "set @rw=0;\n"
				+ "DROP TABLE IF EXISTS TMP_EDT_P_YR_TS_"
				+ yr
				+ "QTR"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP_EDT_P_YR_TS_"
				+ yr
				+ "QTR"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "select @rw:=1+@rw rw,t1.AccNo, /*t1.fileDate, t1.cik, t1.tn, t1.row,*/ t1.col,/* t1.tRow, */t1.tNo,\n"
				+ "/*t1.rowname,t1.value, t1.ttl, t1.stt, t1.net, t1.sub, t1.p1, t1.edt1,*/ case when (COLUMNTEXT RLIKE '^ONE | ONE |^TWO | TWO "
				+ "|^FOUR | FOUR |^FIVE | FIVE "
				+ "|^SEVEN | SEVEN |^EIGHT | EIGHT"
				+ "|^NINE.{1,3}WEEK|FIFTEEN|SIXTEEN|SEVENTEEN|EIGHTEEN|NINETEEN|HUNDRED|(^1 | 1 |^2 | 2 |^4 | 4 |^5 | 5 |^7 | 7 |^9.{1,3}WEEK|^8 "
				+ "| 8 |^15 | 15 |^16 | 16 |^17 | 17 |^18 | 18 |^19 | 19 ).{0,1}(MO|WK)' AND COLUMNTEXT NOT RLIKE  'QUARTER|QTR|TWENTY.{1,2}(FOUR"
				+ "|FIVE|SEVEN|EIGHT|THIRTY.{1,2}NINE)|FIFTY|(^3 | 3 |^9 | 9 |^6 | 6 |^12 | 12 ).{1,3}MO|^NINE | NINE |^THREE | THREE |^SIX | SIX "
				+ "|^TWELVE | TWELVE ' and p2 between 3 and 12)\n"
				+ "or ( (COLUMNTEXT RLIKE 'NINE.{1,3}WEEK' AND COLUMNTEXT NOT RLIKE 'THIRTY') OR (COLUMNTEXT RLIKE 'SIX.{1,3}WEEK' "
				+ "AND COLUMNTEXT NOT RLIKE 'THIRTY|TWENTY') \n"
				+ "or (COLUMNTEXT not RLIKE 'hundred' and columntext rlike '^ONE | ONE ') ) "
				+ "then 0 when columntext rlike 'one.{1,2}hundred' and columntext rlike 'twenty|fifty|forty|thirty' then 3"
				+ " when columntext rlike 'three.{1,2}hundred' and columntext rlike 'fifty|sixty|forty' then 12\n"
				+ " when columntext rlike 'three.{1,2}hundred' then 0 when  \n"
				+ "\n"
				+ "  ((columnText like '%quarter%' and p2!=3 and columntext not rlike 'two |three')\n"
				+ " or (columnText like '%qtr%' and columnText not like '%qtrs%' and p2!=3 and columntext not rlike 'two |three')\n"
				+ " or (columnText like '%for%three%' and columnText not like '%year%' and p2!=3 and columntext not rlike 'quarter|qtr' )\n"
				+ " or (columnText like '%3 %mo%' and columnText not like '%1%' and p2!=3 )\n"
				+ " or (COLUMNTEXT RLIKE 'HREE.{1,2}MONTH|THRE.{1,5}MONTH|THEE.{1,2}MON|THIRTEEN.{1,3}W' AND P2!=3)\n"
				+ " or (columnText like '%three%mo%' and p2!=3 )\n"
				+ " or (columnText rlike '(^12 | 12 |^13 | 13 |^14 | 14 |^twelve | twelve | thirteen |^thirteen ).{0,2}w|(^three | three |^3 | 3 ).{0,2}mo' \n"
				+ " and p2!=3 )\n"
				+ " ) AND COLUMNTEXT NOT RLIKE '(SIX|NINE|TWELVE).{1,2}mo' \n"
				+ " then 3 \n"
				+ "  when ((columnText like '%six%' and columnText not like '%thirt%' and columnText not like '%w%k%' and p2!=6)\n"
				+ " or (columnText like '%6 mo%' and p2!=6)\n"
				+ " or (columnText like '%two quarters%' and p2!=6)\n"
				+ " or (columnText like '%two%fiscal%quarters%' and p2!=6)\n"
				+ " or (columnText like '%24 %wk%' and p2!=6) \n"
				+ "or ( REPLACE(COLUMNTEXT,'1/2','') RLIKE '26.{1,3}WEEK|^IX MON'  AND P2!=6) \n"
				+ " or (columnText like '%25 %wk%' and p2!=6)\n"
				+ " or ( (columnText like '%26 %wk%' OR ((COLUMNTEXT RLIKE '2 QUARTERS' OR ENDED RLIKE '(^2| 2) QUARTER') and p1=6 ) ) and p2!=6)\n"
				+ " or (columnText like '%27 %wk%' and p2!=6)\n"
				+ " or (columnText like '%twenty%' and p2!=6)) AND COLUMNTEXT NOT RLIKE '(THREE|NINE|TWELVE).{1,2}mo' \n"
				+ "then 6\n"
				+ " when columntext rlike 'half.{1,2}year' and p2!=6 then 6\n"
				+ "when (( (columnText like '%nine ' or columnText like 'nine-' or columnText like 'nine ' ) \n"
				+ "and columnText not like '%twent%' and columnText not like '%w%k%' and p2!=9 )\n"
				+ " or ((columnText like '%three quarters%' or columnText rlike 'three quarter' or \n"
				+ "columnText like '%three%fiscal%quarters%' or columnText like 'three%fiscal%quarters%' ) and p2!=9 )\n"
				+ " or (columnText like '%9 mo%' and p2!=9 )\n"
				+ " or (columnText like '%FOR THE NINE%' and p2!=9 )\n"
				+ " or (columnText like '%36%' and ColumnText not rlike '[0-9]{3},' and p2!=9 )\n"
				+ " or (COLUMNTEXT RLIKE 'NINE.{1,2}MONTH|THREE.{1,3}FISCAL.{1,3}Q|THREE.{1,3}QUA|THIRTY.{1,2}NINE.{1,2}W|39WEEK|^INE.{1,2}MO' \n"
				+ " AND P2!=9)\n"
				+ " or (columnText like '%37%' and ColumnText not rlike '[0-9]{3},' and p2!=9 )\n"
				+ " or (columnText like '%38%' and ColumnText not rlike '[0-9]{3},' and p2!=9 )\n"
				+ " or (columnText like '%39%' and ColumnText not rlike '[0-9]{3},' and p2!=9 )\n"
				+ " or (columnText like '%40%' and ColumnText not rlike '[0-9]{3},' and p2!=9 )\n"
				+ " or (columnText like '%41%' and ColumnText not rlike '[0-9]{3},' and p2!=9 )\n"
				+ " or (columnText like '%42%' and ColumnText not rlike '[0-9]{3},' and p2!=9 )\n"
				+ " or (columnText like '%three%fiscal%quarters%' and p2!=9)\n"
				+ " or (columnText like '%thirty%' and p2!=9 )\n"
				+ " or (columnText like '%forty%' and p2!=9 )) AND COLUMNTEXT NOT RLIKE '(THREE|SIX|TWELVE).{1,2}mo' \n"
				+ "then 9\n"
				+ " when columntext rlike '(^3 | 3 |three).{1,3}QUARTER' AND p2!=9 and columnText not rlike 'six |twelve ' then 9 \n"
				+ "when  ((columnText like '%twelv%mo%' and p2!=12 ) \n"
				+ " or (columnText like '%12%mo%' and p2!=12 ) \n"
				+ " or (columnText like '%50 %' and p2!=12 ) \n"
				+ " or (columnText like '%50-%' and p2!=12 ) \n"
				+ " or (columnText like '%51 %' and p2!=12 ) \n"
				+ " or (columnText like '%51-%' and p2!=12 ) \n"
				+ " or (columnText like '%52 %' and p2!=12 ) \n"
				+ " or (columnText like '%52-%' and p2!=12 ) \n"
				+ " or (columnText like '%53 %' and p2!=12 ) \n"
				+ " or (columnText like '%53-%' and p2!=12 ) \n"
				+ " or (columnText like '%54 %' and p2!=12 ) \n"
				+ " or (columnText like '%54-%' and p2!=12 ) \n"
				+ " or (columnText like '%fifty%' and p2!=12 ) \n"
				+ " or (columnText like '%year%end%' and columnText not like '%date%' \n"
				+ " and form like '%10-K%' \n"
				+ " and p2!=12 ) \n"
				+ " or (columnText like '%years%' \n"
				+ " and form like '%10-K%' \n"
				+ " and p2!=12 )) AND COLUMNTEXT NOT RLIKE '(THREE|SIX|NINE).{1,2}mo' \n"
				+ "then 12\n"
				+ "when (tc=2 and (allcoltext rlike 'two year' or columnpattern rlike 'two year' or columntext rlike 'two year'))\n"
				+ "or\n"
				+ "(tc=3 and (allcoltext rlike 'three year' or columnpattern rlike 'three year' or columntext rlike 'three year'))\n"
				+ "or\n"
				+ "(tc=4 and (allcoltext rlike 'four year' or columnpattern rlike 'four year' or columntext rlike 'four year'))\n"
				+ "or\n"
				+ "(tc=5 and (allcoltext rlike 'five year' or columnpattern rlike 'five year' or columntext rlike 'five year'))\n"
				+ "and p2!=12 and form rlike '10-K' then 12\n"
				+ "when tsshort rlike '^p' and p2=0 and col=1 and right(left(tsLong,5),4)>0 then p2=round(right(left(tsLong,5),4)) else p2 end p2\n"
				+ ",case when left(edt2,3) rlike '9[0-9]{1}-' and length(edt2)<9 then concat('19',edt2) \n"
				+ "when left(edt2,3) rlike '[0-3]{1}[0-9]{1}-' and length(edt2)<9 then concat('20',edt2) \n"
				+ "when ((edt2 not rlike '[12]{1}[09]{1}[0-9]{1}[0-9]{1}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})' and length(edt2)>8) \n"
				+ "  or ( edt2 rlike '[a-z]' and length(edt2)=10)) and \n"
				+ "  ((left(edt2,7)=left(edt1,7) and length(edt2)>8 \n"
				+ "  and (right(edt2,1)= '\\\\|') or edt2 rlike '[12]{1}[0-9]{3}-[01]{1}[0-9]{1}- [0-9]{1}') and edt1 rlike '[12]{1}[0-9]{3}-[01]{1}[0-9]{1}-[0-3]{1}[0-9]{1}')\n"
				+ "  then edt1 \n"
				+ "  when ((edt2 not rlike '[12]{1}[09]{1}[0-9]{1}[0-9]{1}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})' and length(edt2)>8) \n"
				+ "  or ( edt2 rlike '[a-z]' and length(edt2)=10)) and\n"
				+ "  (right(left(edt2,7),2) > 12 and left(edt1,4)=left(edt2,4) and right(edt2,2)=right(edt1,2) and length(edt2)>8 and edt1 rlike '[12]{1}[0-9]{3}-(0[0-9]{1}|1[1-2]{1})-[0-3]{1}[0-9]{1}')\n"
				+ "  then edt1\n"
				+ "  when ((edt2 not rlike '[12]{1}[09]{1}[0-9]{1}[0-9]{1}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})' and length(edt2)>8) \n"
				+ "  or ( edt2 rlike '[a-z]' and length(edt2)=10)) and \n"
				+ "  (edt2 rlike '[a-z]' and length(edt2)=10 and left(edt2,7)=left(edt1,7) and edt1 rlike '[12]{1}[0-9]{3}-(0[0-9]{1}|1[1-2]{1})-[0-3]{1}[0-9]{1}')\n"
				+ "  then edt1 \n"
				+ "  when ((edt2 not rlike '[12]{1}[09]{1}[0-9]{1}[0-9]{1}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})' and length(edt2)>8) \n"
				+ "  or ( edt2 rlike '[a-z]' and length(edt2)=10)) and \n"
				+ "  (edt2 rlike '[a-z]' and length(edt2)=10 and edt1 not rlike '[12]{1}[0-9]{3}-(0[0-9]{1}|1[1-2]{1})-[0-3]{1}[0-9]{1}')\n"
				+ "  then left(edt2,7)\n"
				+ "    when ((edt2 not rlike '[12]{1}[09]{1}[0-9]{1}[0-9]{1}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})' and length(edt2)>8) \n"
				+ "  or ( edt2 rlike '[a-z]' and length(edt2)=10)) and\n"
				+ "  (right(left(edt2,7),2) > 12 and  left(edt2,4) rlike '[12]{1}[0-9]{3}')\n"
				+ "    then left(edt2,4)  \n"
				+ "  when ((edt2 not rlike '[12]{1}[09]{1}[0-9]{1}[0-9]{1}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})' and length(edt2)>8) \n"
				+ "  or ( edt2 rlike '[a-z]' and length(edt2)=10)) and \n"
				+ "  (edt2 not rlike '[12]{1}[0-9]{3}-[0-9]{1}' or edt2 rlike '[12]{1}[0-9]{3}[12]{1}[0-9]{3}' or edt1 rlike '[12]{1}[0-9]{3}[12]{1}[0-9]{3}' or edt1 not rlike '[12]{1}[0-9]{3}-[01]{1}[0-9]{1}-[0-3]{1}[0-9]{1}' )\n"
				+ "  then edt2 \n"
				+ " when columntext rlike \n"
				+ "'[12]{1}[09]{1}[019]{1}[0-9]{1}-(0[0-9]{1}|1[0-2]{1})-[0-9]{2}' and edt2 not rlike \n"
				+ "'[12]{1}[09]{1}[0-9]{1}[0-9]{1}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})' and left(edt2,4) rlike \n"
				+ "'[12]{1}[09]{1}[0-9]{2}' \n"
				+ " then concat(left(edt2,2),left(right(edt2,5),2),'-',right(edt2,2),'-',right(left(edt2,4),2)) \n"
				+ "when (EDT2 = 'null' or edt2 is null or length(edt2)<1) AND EDT1 \n"
				+ "RLIKE '^[12]{1}[90]{1}[0-9]{2}-' AND ALLCOLTEXT RLIKE \n"
				+ "CONCAT('C',COL,':',LEFT(EDT1,4))\n"
				+ "then LEFT(EDT1,4)\n"
				+ "when  htmlTxt='html' and length(edt2)!=10 and col!=0 and left(substring_index(columnPattern,'yCnt:',-1),1) =tc and columnPattern rlike concat(col,'L1M:')\n"
				+ "and columnPattern rlike concat(col,'L1Y:') and \n"
				+ "left(STR_TO_DATE(concat(substring_index(substring_index(columnPattern,concat(col,'L1M:'),-1),'|',1) ,', ',substring_index(substring_index(columnPattern,concat(col,'L1Y:'),-1),'|',1)),'%M %d,%Y'),10) is not null\n"
				+ "then left(STR_TO_DATE(concat(substring_index(substring_index(columnPattern,concat(col,'L1M:'),-1),'|',1) ,', ',substring_index(substring_index(columnPattern,concat(col,'L1Y:'),-1),'|',1)),'%M %d,%Y'),10) \n"
				+ "when htmlTxt='html' and col!=0 \n"
				+ "and (length(edt2)!=10 ) and (columnPattern rlike 'mCntD:1' ) and (tslong rlike 'mCntD:1' or tslong rlike 'mCnt:0')\n"
				+ "and left(STR_TO_DATE(concat(substring_index(substring_index(columnPattern,'L1M:',-1),'|',1) ,', ',\n"
				+ "left(edt2,4)),'%M %d,%Y'),10) is not null\n"
				+ "then left(STR_TO_DATE(concat(substring_index(substring_index(columnPattern,'L1M:',-1),'|',1) ,', ',\n"
				+ "left(edt2,4)),'%M %d,%Y'),10)\n"
				+ "else edt2 end edt2,\n"
				+ "/* t1.tc, t1.`dec`, t1.tsShort, t1.ColumnText, t1.ColumnPattern,t1.allColText,t1.ended, t1.mo, t1.coMatch, t1.htmlTxt, t1.form, t1.TSlong,*/ \n"
				+ "case when \n"
				+ "(( columntext rlike 'succesor|predecesor|guarant|inception|transition|cumulat|develop|histor|reorg|cumulat|joint|wholly|<FISCAL|FISCAL-YEAR-END|<PERIOD' \n"
				+ "or allcoltext rlike \n"
				+ "'succesor|predecesor|inception|transition|cumulat|develop|histor|reorg|cumulat|joint|wholly|<FISCAL|FISCAL-YEAR-END|<PERIOD' or \n"
				+ "(columntext rlike 'merg' and columntext not rlike 'post.{1,2}merger') )) or\n"
				+ "( (COLUMNTEXT RLIKE 'NINE.{1,3}WEEK' AND COLUMNTEXT NOT RLIKE 'THIRTY') OR (COLUMNTEXT RLIKE 'SIX.{1,3}WEEK' AND COLUMNTEXT NOT RLIKE 'THIRTY|TWENTY') or (COLUMNTEXT not RLIKE 'hundred' and columntext rlike '^ONE | ONE ') ) or SUMVALUE<101 \n"
				+ "then 'bad' else yr end yr\r from\n"
				+ "TMP_accTnoCol"
				+ yr
				+ "qtr"
				+ q
				+ " t1 order by accno,tno,col;\n"
				+ "ALTER TABLE TMP_EDT_P_YR_TS_"
				+ yr
				+ "QTR"
				+ q
				+ " ADD KEY(rw);\n"
				+ "\n"
				+ "set @acc='1x'; set @tno=-1; set @col=-1; set @rw=0;\n"
				+ "DROP TABLE IF EXISTS tmp_bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ ";\n"
				+ "create table tmp_bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " engine=myisam\n"
				+ "select @rw:=case when @acc!=accno or @tno!=tno or @col!=col then @rw+1 else @rw end rw\n"
				+ ",@acc:=t1.AccNo accno, t1.fileDate, t1.cik, t1.tn, t1.row,@col:=t1.col col\n"
				+ ", t1.tRow, @tno:=t1.tNo tno, t1.rowName, t1.value, t1.ttl, t1.stt, t1.net, t1.sub, t1.p1, t1.edt1\n"
				+ ", t1.p2, t1.edt2, t1.tc, t1.`dec`, t1.tsShort, t1.ColumnText, t1.ColumnPattern, t1.allColText, t1.ended\n"
				+ ", t1.yr, t1.mo, t1.coMatch,t1.tableName,t1.companyNameMatched, t1.htmlTxt, t1.form, t1.TSlong\n"
				+ "from bac_tp_raw t1 \n"
				+ "where filedate between \n"
				+ "'"
				+ yr
				+ "-"
				+ moS
				+ "-"
				+ dyS
				+ "' and '"
				+ yr
				+ "-"
				+ moE
				+ "-"
				+ dyE
				+ "' and cik between "
				+ cikStart
				+ " and "
				+ cikEnd
				+ " \n"
				+ "order by accno,tno,col;\n"
				+ "alter table tmp_bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ " add key(rw);\n"
				+ "\n"
				+ "INSERT IGNORE INTO bac_tp_raw"
				+ yr
				+ "qtr"
				+ q
				+ "\n"
				+ "select t1.accno,t1.filedate,t1.cik,t1.tn,t1.row,t1.col,t1.trow,t1.tno,\n"
				+ "trim(replace(replace(replace(substring_index(substring_index(substring_index(substring_index(rowname,';ST',1),';NET',1),';SUB',1),';TL',1),'RNH set',''),'RNH',''),'?','-')) ROWNAME\n"
				+ ",t1.value,t1.ttl,t1.stt,t1.net,t1.sub\n"
				+ ",t1.p1,t1.edt1,t2.p2,t2.edt2,t1.tc,t1.tableName,t1.coMatch,t1.companyNameMatched,t1.`dec`,t1.tsShort,t1.columnText,t1.columnPattern\n"
				+ ",t1.allColText,t1.ended,t2.yr,t1.mo,t1.htmlTxt,t1.form,t1.tsLong\n"
				+ "from tmp_bac_tp_raw" + yr + "qtr" + q
				+ " t1 inner join TMP_EDT_P_YR_TS_" + yr + "QTR" + q + " t2\n"
				+ "on t1.rw=t2.rw;\n";

		String dropProc = "DROP PROCEDURE IF EXISTS regenerateBac_TP_RawYYYYQtrNo"
				+ yr
				+ "_"
				+ q
				+ ";\n"
				+ "CREATE PROCEDURE regenerateBac_TP_RawYYYYQtrNo"
				+ yr
				+ "_"
				+ q + "()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + qry + endProc);
		MysqlConnUtils.executeQuery("call regenerateBac_TP_RawYYYYQtrNo" + yr
				+ "_" + q + "();\n");

	}

	public void prepRepairMismatch(int startYr, int endYr, int startQ,
			int endQ, boolean regenerateBac_TP_RawYYYYQtr) throws SQLException, FileNotFoundException {

		// This will cycle through all bac_tp_rawYYYYQtr in start/endYr.
		// NOTE: this uses Id from tp_id - id is equal to cik,value and
		// rowname. This will count how times one table's Id matches another. If
		// high - that means they are same table. Next I see where edt/per don't
		// match and mark the corresponding bac_tp_rawYYYYQtrNo yr='bad' - this
		// ensures that column's data won't be used. Next I fetch instances
		// where there's a missing edt or per and find instances where is not
		// missing and the two tables share a lot of the same Id. Based on that
		// I update the missing edt/per.

		StringBuffer sb = new StringBuffer();

		if (regenerateBac_TP_RawYYYYQtr) {

			sb.append("DROP TABLE IF EXISTS TP_MISMATCH_EDT;\n"
					+ "CREATE TABLE `tp_mismatch_edt` (\n"
					+ "  `ttlRw1` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'equals no of rows in tbl1 available to match - that have Id.',\n"
					+ "  `ttlRw2` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'equals no of rows in tbl2 available to match - that have Id.',\n"
					+ "  `CntEdt2_1` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'counts how many tables have the exact same enddate for that rowname,value,cik',\n"
					+ "  `CntP2_1` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'same as above but for p2',\n"
					+ "  `CntEdt2_2` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'same as above but for enddate mismatched (2nd enddate)',\n"
					+ "  `CntP2_2` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'same as above but period mismatched',\n"
					+ "  `CNTId` int(5) NOT NULL DEFAULT '0' COMMENT 'cntId equals the no of rows (rowname,value) that match between the two tables columns',\n"
					+ "  `ID` bigint(13) DEFAULT NULL COMMENT 'for each unique rowname,value,cik an Id is assigned - from tp_id',\n"
					+ "  `CIK` int(11) DEFAULT NULL,\n"
					+ "  `edt2_1` varchar(10) NOT NULL DEFAULT '-1' comment 'edt2 for acc1',\n"
					+ "  `p2_1` tinyint(3) DEFAULT NULL,\n"
					+ "  `ACC1` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `TNO1` tinyint(3) NOT NULL DEFAULT '-1',\n"
					+ "  `COL1` TINYINT(3) NOT NULL COMMENT 'data col number in financial table',\n"
					+ "  `edt2_2` varchar(10) NOT NULL DEFAULT '-1'  comment 'edt2 for acc2',\n"
					+ "  `p2_2` tinyint(3) DEFAULT NULL,\n"
					+ "  `ACC2` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `TNO2` tinyint(3) NOT NULL DEFAULT '-1',\n"
					+ "  `COL2` TINYINT(3) NOT NULL COMMENT 'data col number in financial table',\n"
					+ "  `TN1` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `TN2` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `fd1` varchar(10) CHARACTER SET utf8 DEFAULT NULL,\n"
					+ "  `fd2` varchar(10) CHARACTER SET utf8 DEFAULT NULL,\n"
					+ "  `V1` double(12,0) DEFAULT NULL,\n"
					+ "  `RN1` varchar(20) DEFAULT NULL,\n"
					+ "  key (acc1),\n"
					+ "  key(acc2),\n"
					+ "  key(tno1),\n"
					+ "  key(tno2),\n"
					+ "  key(col1),\n"
					+ "  key(col2),\n"
					+ "  PRIMARY KEY (`ACC1`,`TNO1`,`COL1`,`ACC2`,`TNO2`,`COL2`) \n"
					+ "  COMMENT 'Mismatch is shown by comparing edt2_1 v edt2_2'\n"
					+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n"
					+ "\n"
					+ "DROP TABLE IF EXISTS TP_MISMATCH_P;\n"
					+ "CREATE TABLE `TP_MISMATCH_P` (\n"
					+ "  `ttlRw1` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'equals no of rows in tbl1 available to match - that have Id.',\n"
					+ "  `ttlRw2` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'equals no of rows in tbl2 available to match - that have Id.',\n"
					+ "  `CntEdt2_1` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'counts how many tables have the exact same enddate for that rowname,value,cik',\n"
					+ "  `CntP2_1` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'same as above but for p2',\n"
					+ "  `CntEdt2_2` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'same as above but for enddate mismatched (2nd enddate)',\n"
					+ "  `CntP2_2` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'same as above but period mismatched',\n"
					+ "  `CNTId` int(5) NOT NULL DEFAULT '0' COMMENT 'cntId equals the no of rows (rowname,value) that match between the two tables columns',\n"
					+ "  `ID` bigint(13) DEFAULT NULL COMMENT 'for each unique rowname,value,cik an Id is assigned - from tp_id',\n"
					+ "  `CIK` int(11) DEFAULT NULL,\n"
					+ "  `edt2_1` varchar(10) NOT NULL DEFAULT '-1' comment 'edt2 for acc1',\n"
					+ "  `p2_1` tinyint(3) DEFAULT NULL,\n"
					+ "  `ACC1` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `TNO1` tinyint(3) NOT NULL DEFAULT '-1',\n"
					+ "  `COL1` TINYINT(3) NOT NULL COMMENT 'data col number in financial table',\n"
					+ "  `edt2_2` varchar(10) NOT NULL DEFAULT '-1'  comment 'edt2 for acc2',\n"
					+ "  `p2_2` tinyint(3) DEFAULT NULL,\n"
					+ "  `ACC2` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `TNO2` tinyint(3) NOT NULL DEFAULT '-1',\n"
					+ "  `COL2` TINYINT(3) NOT NULL COMMENT 'data col number in financial table',\n"
					+ "  `TN1` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `TN2` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `fd1` varchar(10) CHARACTER SET utf8 DEFAULT NULL,\n"
					+ "  `fd2` varchar(10) CHARACTER SET utf8 DEFAULT NULL,\n"
					+ "  `V1` double(12,0) DEFAULT NULL,\n"
					+ "  `RN1` varchar(20) DEFAULT NULL,\n"
					+ "  key (acc1),\n"
					+ "  key(acc2),\n"
					+ "  key(tno1),\n"
					+ "  key(tno2),\n"
					+ "  key(col1),\n"
					+ "  key(col2),\n"
					+ "  PRIMARY KEY (`ACC1`,`TNO1`,`COL1`,`ACC2`,`TNO2`,`COL2`)\n"
					+ "  COMMENT 'Mismatch is shown by comparing p2_1 v p2_2'\n"
					+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n"
					+ "\n"
					+ "DROP TABLE IF EXISTS TP_repair_EDT;\n"
					+ "CREATE TABLE `tp_repair_edt` (\n"
					+ "  `ttlRw1` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'equals no of rows in tbl1 available to match - that have Id.',\n"
					+ "  `ttlRw2` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'equals no of rows in tbl2 available to match - that have Id.',\n"
					+ "  `CntEdt2_1` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'counts how many tables have the exact same enddate for that rowname,value,cik',\n"
					+ "  `CntP2_1` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'same as above but for p2',\n"
					+ "  `CntEdt2_2` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'same as above but for enddate mismatched (2nd enddate)',\n"
					+ "  `CntP2_2` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'same as above but period mismatched',\n"
					+ "  `CNTId` int(5) NOT NULL DEFAULT '0' COMMENT 'cntId equals the no of rows (rowname,value) that match between the two tables columns',\n"
					+ "  `ID` bigint(13) DEFAULT NULL COMMENT 'for each unique rowname,value,cik an Id is assigned - from tp_id',\n"
					+ "  `CIK` int(11) DEFAULT NULL,\n"
					+ "  `edt2_1` varchar(10) NOT NULL DEFAULT '-1' comment 'edt2 for acc1',\n"
					+ "  `p2_1` tinyint(3) DEFAULT NULL,\n"
					+ "  `ACC1` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `TNO1` tinyint(3) NOT NULL DEFAULT '-1',\n"
					+ "  `COL1` TINYINT(3) NOT NULL COMMENT 'data col number in financial table',\n"
					+ "  `edt2_2` varchar(10) NOT NULL DEFAULT '-1'  comment 'edt2 for acc2',\n"
					+ "  `p2_2` tinyint(3) DEFAULT NULL,\n"
					+ "  `ACC2` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `TNO2` tinyint(3) NOT NULL DEFAULT '-1',\n"
					+ "  `COL2` TINYINT(3) NOT NULL COMMENT 'data col number in financial table',\n"
					+ "  `TN1` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `TN2` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `fd1` varchar(10) CHARACTER SET utf8 DEFAULT NULL,\n"
					+ "  `fd2` varchar(10) CHARACTER SET utf8 DEFAULT NULL,\n"
					+ "  `V1` double(12,0) DEFAULT NULL,\n"
					+ "  `RN1` varchar(20) DEFAULT NULL,\n"
					+ "  key (acc1),\n"
					+ "  key(acc2),\n"
					+ "  key(tno1),\n"
					+ "  key(tno2),\n"
					+ "  key(col1),\n"
					+ "  key(col2),\n"
					+ "  PRIMARY KEY (`ACC1`,`TNO1`,`COL1`,`ACC2`,`TNO2`,`COL2`) \n"
					+ "  COMMENT 'repair is shown by comparing edt2_1 v edt2_2'\n"
					+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n"
					+ "\n"
					+ "DROP TABLE IF EXISTS TP_repair_P;\n"
					+ "CREATE TABLE `TP_repair_P` (\n"
					+ "  `ttlRw1` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'equals no of rows in tbl1 available to match - that have Id.',\n"
					+ "  `ttlRw2` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'equals no of rows in tbl2 available to match - that have Id.',\n"
					+ "  `CntEdt2_1` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'counts how many tables have the exact same enddate for that rowname,value,cik',\n"
					+ "  `CntP2_1` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'same as above but for p2',\n"
					+ "  `CntEdt2_2` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'same as above but for enddate mismatched (2nd enddate)',\n"
					+ "  `CntP2_2` tinyint(3) NOT NULL DEFAULT '0' COMMENT 'same as above but period mismatched',\n"
					+ "  `CNTId` int(5) NOT NULL DEFAULT '0' COMMENT 'cntId equals the no of rows (rowname,value) that match between the two tables columns',\n"
					+ "  `ID` bigint(13) DEFAULT NULL COMMENT 'for each unique rowname,value,cik an Id is assigned - from tp_id',\n"
					+ "  `CIK` int(11) DEFAULT NULL,\n"
					+ "  `edt2_1` varchar(10) NOT NULL DEFAULT '-1' comment 'edt2 for acc1',\n"
					+ "  `p2_1` tinyint(3) DEFAULT NULL,\n"
					+ "  `ACC1` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `TNO1` tinyint(3) NOT NULL DEFAULT '-1',\n"
					+ "  `COL1` TINYINT(3) NOT NULL COMMENT 'data col number in financial table',\n"
					+ "  `edt2_2` varchar(10) NOT NULL DEFAULT '-1'  comment 'edt2 for acc2',\n"
					+ "  `p2_2` tinyint(3) DEFAULT NULL,\n"
					+ "  `ACC2` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `TNO2` tinyint(3) NOT NULL DEFAULT '-1',\n"
					+ "  `COL2` TINYINT(3) NOT NULL COMMENT 'data col number in financial table',\n"
					+ "  `TN1` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `TN2` varchar(20) NOT NULL DEFAULT '-1',\n"
					+ "  `fd1` varchar(10) CHARACTER SET utf8 DEFAULT NULL,\n"
					+ "  `fd2` varchar(10) CHARACTER SET utf8 DEFAULT NULL,\n"
					+ "  `V1` double(12,0) DEFAULT NULL,\n"
					+ "  `RN1` varchar(20) DEFAULT NULL,\n"
					+ "  key (acc1),\n"
					+ "  key(acc2),\n"
					+ "  key(tno1),\n"
					+ "  key(tno2),\n"
					+ "  key(col1),\n"
					+ "  key(col2),\n"
					+ "  PRIMARY KEY (`ACC1`,`TNO1`,`COL1`,`ACC2`,`TNO2`,`COL2`)\n"
					+ "  COMMENT 'repair is shown by comparing p2_1 v p2_2'\n"
					+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n");

			sb.append("set sql_mode = ALLOW_INVALID_DATES;\n"
					+ "DROP TABLE IF EXISTS TTLROWS;\n"
					+ "CREATE TABLE TTLROWS ENGINE=MYISAM\n"
					+ "select count(*) ttlRw, accno,tno,col from tp_id group by accno,tno,col;\n"
					+ "ALTER TABLE TTLROWS ADD KEY(accno),ADD KEY(tno),ADD KEY(col);\n");

		}

		int qtr = startQ;
		int q = qtr;

		for (int yr = startYr; yr <= endYr; yr++) {
			if (yr == 1993 && qtr < 3) {
				qtr = 3;
			}
			for (q = qtr; q <= endQ; q++) {

				String moS = "", dyS = "01", moE = "", dyE = "";

				if (q == 1) {
					moS = "01";
					moE = "03";
					dyE = "31";
				}
				if (q == 2) {
					moS = "04";
					moE = "06";
					dyE = "30";
				}
				if (q == 3) {
					moS = "07";
					moE = "09";
					dyE = "30";
				}
				if (q == 4) {
					moS = "10";
					moE = "12";
					dyE = "31";
				}

				sb.append("\n"
						+ "/*when edt2_ck=0 edt2 is invalid, if 1 valid. Same for p2_ck. Same Id share same cik,rowname,value.\n"
						+ "At this point there's no way to repair mismatched enddates. The mismatching occurs in most instances due to errors in the actual filing. \n"
						+ "Typically two separate filings will have same exact data for a particular column but will erroneoulsy label enddates. Each bad table is\n"
						+ "inserted into tp_mismatch_edt and tp_misMatch_p. Even as I exclude bad tables from a repair - the newly repaired can create a mismatch\n"
						+ "if a 2nd pass is done.*/\n" + "\n" + "\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n" + "\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "DROP TABLE IF EXISTS tmpA_mismatch_edt_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "CREATE TABLE tmpA_mismatch_edt_"
						+ yr
						+ "qtr"
						+ q
						+ " ENGINE=MYISAM\n"
						+ "select t1.cntEdt cntEdt2_1,t1.cntP cntP2_1,t2.cntEdt cntEdt2_2,t2.cntP cntP2_2,\n"
						+ "count(distinct(t1.id)) cnt,t1.cik\n"
						+ ",t1.id id1,t1.edt2 edt2_1,t1.p2 p2_1\n"
						+ ",t2.edt2 edt2_2,t2.p2 p2_2\n"
						+ ",t1.accno acc1,t1.tno tno1,t1.col col1\n"
						+ ",t2.accno acc2,t2.tno tno2,t2.col col2\n"
						+ ",T1.ROWNAME rn1,T1.VALUE v1,T1.FILEDATE FD1,T2.FILEDATE FD2,t1.tn tn1,t2.tn tn2\n"
						+ "/*,concat('https://www.sec.gov/Archives/edgar/data/',t1.cik,'/',t1.accno,'.txt') link\n"
						+ ",concat('https://www.sec.gov/Archives/edgar/data/',t2.cik,'/',t2.accno,'.txt') link2*/\n"
						+ " from tp_id t1 left join tp_id t2 \n"
						+ "on t1.id=t2.id and t1.edt2!=t2.edt2\n"
						+ "where ((t1.accno!=t2.accno ) or (t1.accno=t2.accno and t1.tno!=t2.tno) )\n"
						+ "and t1.edt2_ck=1 and t2.edt2_ck=1 \n"
						+ "and t1.filedate between '"
						+ yr
						+ "-"
						+ moS
						+ "-"
						+ dyS
						+ "' and '"
						+ yr
						+ "-"
						+ moE
						+ "-"
						+ dyE
						+ "' \n"
						+ "and t2.filedate between date_sub('"
						+ yr
						+ "-"
						+ moS
						+ "-"
						+ dyS
						+ "', interval 4 year) and date_add('"
						+ yr
						+ "-"
						+ moE
						+ "-"
						+ dyE
						+ "',interval 4 year) \n"
						+ "and datediff(greatest(t1.filedate,t2.filedate),least(t1.filedate,t2.filedate))<2000\n"
						+ "and datediff(greatest(t1.edt2,t2.edt2),least(t1.edt2,t2.edt2))>35\n"
						+ "group by t1.accno,t1.tno,t1.col,t2.accno,t2.tno,t2.col;\n"
						+ "ALTER TABLE tmpA_mismatch_edt_"
						+ yr
						+ "qtr"
						+ q
						+ " ADD KEY(acc2),ADD KEY(tno2),ADD KEY(col2),ADD KEY(cnt);\n"
						+ "/*because I span 5 years before and after each quarter I find all possible mismatched enddates for each filing in that quarter.\n"
						+ "I then store it in tp_misMatch_EDT table. Both acc1,tno1,col1 and acc2,tno2,col2 are misMatch enddate - edt2_1 v ed2_2 demonstrate\n"
						+ "the mismatch.*/\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMPB_mismatch_edt_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "CREATE TABLE TMPB_mismatch_edt_"
						+ yr
						+ "qtr"
						+ q
						+ " ENGINE=MYISAM\n"
						+ "SELECT TTLRW TTLRW2,T1.* FROM tmpA_mismatch_edt_"
						+ yr
						+ "qtr"
						+ q
						+ " T1 INNER JOIN ttlRows T2 \n"
						+ "ON T1.ACC2=T2.ACCNO AND T1.TNO2=T2.TNO AND T1.COL2=T2.COL;\n"
						+ "ALTER TABLE TMPB_mismatch_edt_"
						+ yr
						+ "qtr"
						+ q
						+ " ADD KEY(acc1),ADD KEY(tno1),ADD KEY(col1);\n"
						+ "\n"
						+ "INSERT IGNORE INTO TP_MISMATCH_EDT \n"
						+ "SELECT TTLRW TTLRW1,TTLRW2,cntEdt2_1, cntP2_1, cntEdt2_2, cntP2_2, cnt,ID1 ID,CIK,edt2_1,p2_1,ACC1,TNO1,COL1,edt2_2,p2_2\n"
						+ ",ACC2,TNO2,COL2,tn1,tn2,fd1,fd2,V1,RN1 FROM TMPB_mismatch_edt_"
						+ yr
						+ "qtr"
						+ q
						+ " T1 INNER JOIN ttlRows T2 \n"
						+ "ON T1.ACC1=T2.ACCNO AND T1.TNO1=T2.TNO AND T1.COL1=T2.COL;\n"
						+ "/*<<Mismatch is shown by comparing EDT2_1 v EDT2_2*/\n"
						+ "\n"
						+ "\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "DROP TABLE IF EXISTS tmpA_mismatch_p_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "CREATE TABLE tmpA_mismatch_p_"
						+ yr
						+ "qtr"
						+ q
						+ " ENGINE=MYISAM\n"
						+ "select t1.cntEdt cntEdt2_1,t1.cntP cntP2_1,t2.cntEdt cntEdt2_2,t2.cntP cntP2_2,\n"
						+ "count(distinct(t1.id)) cnt,\n"
						+ "t1.cik\n"
						+ ",t1.id id1,t1.edt2 edt2_1,t1.p2 p2_1\n"
						+ ",t2.edt2 edt2_2,t2.p2 p2_2\n"
						+ ",t1.accno acc1,t1.tno tno1,t1.col col1\n"
						+ ",t2.accno acc2,t2.tno tno2,t2.col col2\n"
						+ ",T1.ROWNAME rn1,T1.VALUE v1,T1.FILEDATE FD1,T2.FILEDATE FD2,t1.tn tn1,t2.tn tn2\n"
						+ "/*,t1.p2_ck,t2.p2_ck,concat('https://www.sec.gov/Archives/edgar/data/',t1.cik,'/',t1.accno,'.txt') link\n"
						+ ",concat('https://www.sec.gov/Archives/edgar/data/',t2.cik,'/',t2.accno,'.txt') link2*/\n"
						+ " from tp_id t1 inner join tp_id t2 \n"
						+ "on t1.id=t2.id and t1.p2!=t2.p2\n"
						+ "where ((t1.accno!=t2.accno ) or (t1.accno=t2.accno and t1.tno!=t2.tno) )\n"
						+ "and t1.filedate between '"
						+ yr
						+ "-"
						+ moS
						+ "-"
						+ dyS
						+ "' and '"
						+ yr
						+ "-"
						+ moE
						+ "-"
						+ dyE
						+ "' \n"
						+ "and t2.filedate between date_sub('"
						+ yr
						+ "-"
						+ moS
						+ "-"
						+ dyS
						+ "', interval 4 year) and date_add('"
						+ yr
						+ "-"
						+ moE
						+ "-"
						+ dyE
						+ "',interval 4 year) \n"
						+ "and datediff(greatest(t1.filedate,t2.filedate),least(t1.filedate,t2.filedate))<2000\n"
						+ "and t1.p2_ck=1 and t2.p2_ck=1 \n"
						+ "group by t1.accno,t1.tno,t1.col,t2.accno,t2.tno,t2.col;\n"
						+ "ALTER TABLE tmpA_mismatch_p_"
						+ yr
						+ "qtr"
						+ q
						+ " ADD KEY(acc2),ADD KEY(tno2),ADD KEY(col2),ADD KEY(cnt);\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMPB_mismatch_P_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "CREATE TABLE TMPB_mismatch_P_"
						+ yr
						+ "qtr"
						+ q
						+ " ENGINE=MYISAM\n"
						+ "SELECT TTLRW TTLRW2,T1.* FROM tmpA_mismatch_P_"
						+ yr
						+ "qtr"
						+ q
						+ " T1 INNER JOIN ttlRows T2 \n"
						+ "ON T1.ACC2=T2.ACCNO AND T1.TNO2=T2.TNO AND T1.COL2=T2.COL;\n"
						+ "ALTER TABLE TMPB_mismatch_P_"
						+ yr
						+ "qtr"
						+ q
						+ " ADD KEY(acc1),ADD KEY(tno1),ADD KEY(col1);\n"
						+ "\n"
						+ "INSERT IGNORE INTO TP_MISMATCH_P \n"
						+ "SELECT TTLRW TTLRW1,TTLRW2,cntEdt2_1, cntP2_1, cntEdt2_2, cntP2_2, cnt,ID1 ID,CIK,edt2_1,p2_1,ACC1,TNO1,COL1,edt2_2,p2_2\n"
						+ ",ACC2,TNO2,COL2,tn1,tn2,fd1,fd2,V1,RN1 FROM TMPB_mismatch_P_"
						+ yr
						+ "qtr"
						+ q
						+ " T1 INNER JOIN ttlRows T2 \n"
						+ "ON T1.ACC1=T2.ACCNO AND T1.TNO1=T2.TNO AND T1.COL1=T2.COL;\n"
						+ "/*<<Mismatch is shown by comparing p2_1 v p2_2*/\n"
						+ "\n"
						+ "/*repair phase START*/\n"
						+ "/*runs after bad enddates were marked. Ensures I don't utilize bad enddate accno in a repair by deleting them from repair table.*/\n"
						+ "\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "DROP TABLE IF EXISTS tmpA_repair_edt_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "CREATE TABLE tmpA_repair_edt_"
						+ yr
						+ "qtr"
						+ q
						+ " ENGINE=MYISAM\n"
						+ "select t1.cntEdt cntEdt2_1,t1.cntP cntP2_1,t2.cntEdt cntEdt2_2,t2.cntP cntP2_2,\n"
						+ "count(distinct(t1.id)) cnt,t1.cik\n"
						+ ",t1.id id1,t1.edt2 edt2_1,t1.p2 p2_1\n"
						+ ",t2.edt2 edt2_2,t2.p2 p2_2\n"
						+ ",t1.accno acc1,t1.tno tno1,t1.col col1\n"
						+ ",t2.accno acc2,t2.tno tno2,t2.col col2\n"
						+ ",T1.ROWNAME rn1,T1.VALUE v1,T1.FILEDATE FD1,T2.FILEDATE FD2,t1.tn tn1,t2.tn tn2\n"
						+ "/*,concat('https://www.sec.gov/Archives/edgar/data/',t1.cik,'/',t1.accno,'.txt') link\n"
						+ ",concat('https://www.sec.gov/Archives/edgar/data/',t2.cik,'/',t2.accno,'.txt') link2*/\n"
						+ " from tp_id t1 inner join tp_id t2 \n"
						+ "on t1.id=t2.id \n"
						+ "where ((t1.accno!=t2.accno ) or (t1.accno=t2.accno and t1.tno!=t2.tno) )\n"
						+ "and t1.edt2_ck=0 and t2.edt2_ck=1/*if 0 - incomplete enddate, if 1 complete. Find incomplete*/ \n"
						+ "and t1.filedate between '"
						+ yr
						+ "-"
						+ moS
						+ "-"
						+ dyS
						+ "' and '"
						+ yr
						+ "-"
						+ moE
						+ "-"
						+ dyE
						+ "' \n"
						+ "and t2.filedate between date_sub('"
						+ yr
						+ "-"
						+ moS
						+ "-"
						+ dyS
						+ "', interval 4 year) and date_add('"
						+ yr
						+ "-"
						+ moE
						+ "-"
						+ dyE
						+ "',interval 4 year) \n"
						+ "and datediff(greatest(t1.filedate,t2.filedate),least(t1.filedate,t2.filedate))<2000\n"
						+ "group by t1.accno,t1.tno,t1.col,t2.accno,t2.tno,t2.col;\n"
						+ "ALTER TABLE tmpA_repair_edt_"
						+ yr
						+ "qtr"
						+ q
						+ " ADD KEY(acc2),ADD KEY(tno2),ADD KEY(col2),ADD KEY(cnt);\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMPB_repair_edt_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "CREATE TABLE TMPB_repair_edt_"
						+ yr
						+ "qtr"
						+ q
						+ " ENGINE=MYISAM\n"
						+ "SELECT TTLRW TTLRW2,T1.* FROM tmpA_repair_edt_"
						+ yr
						+ "qtr"
						+ q
						+ " T1 INNER JOIN ttlRows T2 \n"
						+ "ON T1.ACC2=T2.ACCNO AND T1.TNO2=T2.TNO AND T1.COL2=T2.COL;\n"
						+ "ALTER TABLE TMPB_repair_edt_"
						+ yr
						+ "qtr"
						+ q
						+ " ADD KEY(acc1),ADD KEY(tno1),ADD KEY(col1);\n"
						+ "\n"
						+ "INSERT IGNORE INTO TP_REPAIR_EDT \n"
						+ "SELECT TTLRW TTLRW1,TTLRW2,cntEdt2_1, cntP2_1, cntEdt2_2, cntP2_2, cnt,ID1 ID,CIK,edt2_1,p2_1,ACC1,TNO1,COL1,edt2_2,p2_2\n"
						+ ",ACC2,TNO2,COL2,tn1,tn2,fd1,fd2,V1,RN1 FROM TMPB_repair_edt_"
						+ yr
						+ "qtr"
						+ q
						+ " T1 INNER JOIN ttlRows T2 \n"
						+ "ON T1.ACC1=T2.ACCNO AND T1.TNO1=T2.TNO AND T1.COL1=T2.COL;\n"
						+ "/*<<Mismatch is shown by comparing p2_1 v p2_2*/\n"
						+ "\n"
						+ "delete t1 from tp_repair_edt t1 inner join tp_mismatch_edt t2 on \n"
						+ "(t1.acc1=t2.acc1 and t1.tno1=t2.tno1 and t1.col1=t2.col1) or\n"
						+ "(t1.acc1=t2.acc2 and t1.tno1=t2.tno2 and t1.col1=t2.col2) or\n"
						+ "(t1.acc2=t2.acc1 and t1.tno2=t2.tno1 and t1.col2=t2.col1) or\n"
						+ "(t1.acc2=t2.acc2 and t1.tno2=t2.tno2 and t1.col2=t2.col2) ;\n"
						+ "\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "DROP TABLE IF EXISTS tmpA_repair_p_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "CREATE TABLE tmpA_repair_p_"
						+ yr
						+ "qtr"
						+ q
						+ " ENGINE=MYISAM\n"
						+ "select t1.cntEdt cntEdt2_1,t1.cntP cntP2_1,t2.cntEdt cntEdt2_2,t2.cntP cntP2_2,\n"
						+ "count(distinct(t1.id)) cnt,t1.cik\n"
						+ ",t1.id id1,t1.edt2 edt2_1,t1.p2 p2_1\n"
						+ ",t2.edt2 edt2_2,t2.p2 p2_2\n"
						+ ",t1.accno acc1,t1.tno tno1,t1.col col1\n"
						+ ",t2.accno acc2,t2.tno tno2,t2.col col2\n"
						+ ",T1.ROWNAME rn1,T1.VALUE v1,T1.FILEDATE FD1,T2.FILEDATE FD2,t1.tn tn1,t2.tn tn2\n"
						+ "/*,concat('https://www.sec.gov/Archives/edgar/data/',t1.cik,'/',t1.accno,'.txt') link\n"
						+ ",concat('httpshttps://www.sec.gov/Archives/edgar/data/',t2.cik,'/',t2.accno,'.txt') link2*/\n"
						+ " from tp_id t1 inner join tp_id t2 \n"
						+ "on t1.id=t2.id \n"
						+ "where ((t1.accno!=t2.accno ) or (t1.accno=t2.accno and t1.tno!=t2.tno) )\n"
						+ "and t1.p2_ck=0 and t2.p2_ck=1/*if 0 - incomplete enddate, if 1 complete. Find incomplete*/ \n"
						+ "and t1.filedate between '"
						+ yr
						+ "-"
						+ moS
						+ "-"
						+ dyS
						+ "' and '"
						+ yr
						+ "-"
						+ moE
						+ "-"
						+ dyE
						+ "' \n"
						+ "and t2.filedate between date_sub('"
						+ yr
						+ "-"
						+ moS
						+ "-"
						+ dyS
						+ "', interval 4 year) and date_add('"
						+ yr
						+ "-"
						+ moE
						+ "-"
						+ dyE
						+ "',interval 4 year) \n"
						+ "and datediff(greatest(t1.filedate,t2.filedate),least(t1.filedate,t2.filedate))<2000\n"
						+ "group by t1.accno,t1.tno,t1.col,t2.accno,t2.tno,t2.col;\n"
						+ "ALTER TABLE tmpA_repair_P_"
						+ yr
						+ "qtr"
						+ q
						+ " ADD KEY(acc2),ADD KEY(tno2),ADD KEY(col2),ADD KEY(cnt);\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMPB_repair_p_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "CREATE TABLE TMPB_repair_p_"
						+ yr
						+ "qtr"
						+ q
						+ " ENGINE=MYISAM\n"
						+ "SELECT TTLRW TTLRW2,T1.* FROM tmpA_repair_p_"
						+ yr
						+ "qtr"
						+ q
						+ " T1 INNER JOIN ttlRows T2 \n"
						+ "ON T1.ACC2=T2.ACCNO AND T1.TNO2=T2.TNO AND T1.COL2=T2.COL;\n"
						+ "ALTER TABLE TMPB_repair_p_"
						+ yr
						+ "qtr"
						+ q
						+ " ADD KEY(acc1),ADD KEY(tno1),ADD KEY(col1);\n"
						+ "\n"
						+ "INSERT IGNORE INTO TP_REPAIR_p \n"
						+ "SELECT TTLRW TTLRW1,TTLRW2,cntEdt2_1, cntP2_1, cntEdt2_2, cntP2_2, cnt,ID1 ID,CIK,edt2_1,p2_1,ACC1,TNO1,COL1,edt2_2,p2_2\n"
						+ ",ACC2,TNO2,COL2,tn1,tn2,fd1,fd2,V1,RN1 FROM TMPB_repair_p_"
						+ yr
						+ "qtr"
						+ q
						+ " T1 INNER JOIN ttlRows T2 \n"
						+ "ON T1.ACC1=T2.ACCNO AND T1.TNO1=T2.TNO AND T1.COL1=T2.COL;\n"
						+ "/*<<Mismatch is shown by comparing p2_1 v p2_2*/\n"
						+ "\n"
						+ "delete t1 from tp_repair_P t1 inner join tp_mismatch_P t2 on \n"
						+ "(t1.acc1=t2.acc1 and t1.tno1=t2.tno1 and t1.col1=t2.col1) or\n"
						+ "(t1.acc1=t2.acc2 and t1.tno1=t2.tno2 and t1.col1=t2.col2) or\n"
						+ "(t1.acc2=t2.acc1 and t1.tno2=t2.tno1 and t1.col2=t2.col1) or\n"
						+ "(t1.acc2=t2.acc2 and t1.tno2=t2.tno2 and t1.col2=t2.col2) ;\n"
						+ "\n" + "\n");

				String dropProc = "DROP PROCEDURE IF EXISTS markBadTblsAndRepairEdtPer"
						+ yr
						+ "_"
						+ q
						+ ";\n"
						+ "CREATE PROCEDURE markBadTblsAndRepairEdtPer"
						+ yr
						+ "_" + q + "()\n\n begin\n\n";
				String endProc = "\n\nend;";

				MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
				MysqlConnUtils.executeQuery("call markBadTblsAndRepairEdtPer"
						+ yr + "_" + q + "();\n");
				sb.delete(0, sb.toString().length());

			}
			qtr = 1;
		}
	}

	public void repairEnddatePeriod(int startYr, int endYr, int startQ, int endQ)
			throws SQLException, FileNotFoundException {
		/*
		 * This will take the tp_repair_edt and tp_repair_p tables and determine
		 * which enddates and periods can be repaired and then update each
		 * bac_tp_rawYYYYQtr table.
		 */

		StringBuffer sb = new StringBuffer();

		sb.append("\n"
				+ "DROP TABLE IF EXISTS tp_edt;\n"
				+ "CREATE TABLE `tp_edt` (\n"
				+ "  `ACCNO` varchar(20) NOT NULL DEFAULT '-1',\n"
				+ "  `tno` int(5) NOT NULL DEFAULT -1,\n"
				+ "  `COL` TINYINT(3) NOT NULL COMMENT 'data col number in financial table',\n"
				+ "  `EDT2` varchar(10) NOT NULL DEFAULT '1901-01-01',\n"
				+ "  PRIMARY KEY(ACCNO,TNO,COL),\n" + "    KEY(ACCNO),\n"
				+ "  KEY (TNO),\n" + "  KEY(COL)\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n\n");

		sb.append("\n"
				+ "DROP TABLE IF EXISTS tp_p;\n"
				+ "CREATE TABLE `tp_p` (\n"
				+ "  `ACCNO` varchar(20) NOT NULL DEFAULT '-1',\n"
				+ "  `tno` int(5) NOT NULL DEFAULT -1,\n"
				+ "  `COL` TINYINT(3) NOT NULL DEFAULT -1 COMMENT 'data col number in financial table',\n"
				+ "  `p2` varchar(10) NOT NULL DEFAULT '1901-01-01',\n"
				+ "  PRIMARY KEY(ACCNO,TNO,COL),\n" + "  KEY(ACCNO),\n"
				+ "  KEY (TNO),\n" + "  KEY(COL)\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n\n");

		sb.append("DROP TABLE IF EXISTS tp_edt_bad;\n"
				+ "CREATE TABLE `tp_edt_bad` (\n"
				+ "  `ACCNO` varchar(20) NOT NULL DEFAULT '-1',\n"
				+ "  `tno` int(5) NOT NULL DEFAULT '-1',\n"
				+ "  `COL` TINYINT(3) NOT NULL COMMENT 'data col number in financial table',\n"
				+ "  `EDT2` varchar(10) NOT NULL DEFAULT '1901-01-01',\n"
				+ "  PRIMARY KEY (`ACCNO`,`TNO`,`COL`),\n"
				+ "  KEY `ACCNO` (`ACCNO`),\n"
				+ "  KEY `TNO` (`TNO`),\n"
				+ "  KEY `COL` (`COL`)\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TP_P_BAD;\n"
				+ "CREATE TABLE `tp_p_bad` (\n"
				+ "  `ACCNO` varchar(20) NOT NULL DEFAULT '-1',\n"
				+ "  `tno` int(5) NOT NULL DEFAULT '-1',\n"
				+ "  `COL` TINYINT(3) NOT NULL COMMENT 'data col number in financial table',\n"
				+ "  `p2` varchar(10) NOT NULL DEFAULT '1901-01-01',\n"
				+ "  PRIMARY KEY (`ACCNO`,`TNO`,`COL`),\n"
				+ "  KEY `ACCNO` (`ACCNO`),\n" + "  KEY `TNO` (`TNO`),\n"
				+ "  KEY `COL` (`COL`)\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n" + "\n");

		sb.append("\n"
				+ "/*If high cntId that means the two tables matched had that many rows,values and cik that were identical. I may have cases where just 1 row\n"
				+ "matches between 2 tables, but there are 3 or more tables that also match the same table and each have the same enddate. That is highly \n"
				+ "reliable. If I order by acc1,tn1,col1 each row is a match to another table and if all have same enddates I've confirmed enddate.\n"
				+ "1. find where two tables confirm enddate and nothing bad and insert into tp_edt.if two or more tables have same enddate - repair to \n"
				+ "that enddate (eg: two or more rows of acc1,tno1,col1 have same edt2_2) provided there are no other tables that have another enddate. */\n"
				+ "\n"
				+ "set @acc1='1x'; set @TNO1=0; set @col1=0; set @edt2='1901-01-01'; set @cI:=0; set @ck='1x';\n"
				+ "DROP TABLE IF EXISTS TMP1_RPR_ENDDATE;\n"
				+ "CREATE TABLE TMP1_RPR_ENDDATE ENGINE=MYISAM\n"
				+ "select \n"
				+ "case when @ck=edt2_2 and @acc1=acc1 and @tno1=tno1 and @col1=col1 then edt2_2 else '' end edt3,\n"
				+ "@ck:=case \n"
				+ "when @acc1=acc1 and @tno1=tno1 and @col1=col1 and datediff(greatest(@edt2,edt2_2),least(@edt2,edt2_2))<35 then edt2_2 \n"
				+ "when @acc1=acc1 and @tno1=tno1 and @col1=col1 and datediff(greatest(@edt2,edt2_2),least(@edt2,edt2_2))>=35  then 'bad' \n"
				+ "else edt2_2 end ck,\n"
				+ "@cI:=CNTid cI,ttlrw1 tr1,ttlrw2 tr2,round((cntId/least(ttlrw1,ttlrw2)),1) pMtc,tn1 ,tn2 \n"
				+ ",CIK,V1,RN1,edt2_1,@EDT2:=edt2_2 edt2_2,@ACC1:=ACC1 acc1,@TNO1:=TNO1 tno1,@COL1:=COL1 col1,ACC2,TNO2,COL2,fd1,fd2,ID,p2_1 p1,p2_2 p2\n"
				+ "/*,concat('file:///c://backtest/tableparser/',year(fd1),'/qtr',quarter(fd1),'/tables/',acc1,'_',tno1,'.htm') link\n"
				+ ",concat('file:///c://backtest/tableparser/',year(fd2),'/qtr',quarter(fd2),'/tables/',acc2,'_',tno2,'.htm') link2*/\n"
				+ "from tp_repair_edt t1\n"
				+ "where ( abs( left(edt2_1,4)-left(edt2_2,4))<=1 or left(edt2_1,4) not rlike '[12]{1}[09]{1}[0-9]{2}')\n"
				+ "/*require yr edt2_1 and edt_2 abs.diff be <=1. */\n"
				+ "order by acc1,tno1,col1,edt2_2,acc2,tno2,col2 ;\n"
				+ "\n"
				+ "ALTER TABLE TMP1_RPR_ENDDATE ADD KEY(ACC1),ADD KEY(TNO1),ADD KEY(COL1);\n"
				+ "/*find acc1 that have two enddates match it.*/\n"
				+ "DROP TABLE IF EXISTS TMP2_RPR_ENDDATE_BD;\n"
				+ "CREATE TABLE TMP2_RPR_ENDDATE_BD ENGINE=MYISAM\n"
				+ "SELECT * FROM TMP1_RPR_ENDDATE where ck='bad' GROUP BY ACC1,TNO1,COL1;\n"
				+ "ALTER TABLE TMP2_RPR_ENDDATE_BD ADD KEY(ACC1),ADD KEY(TNO1),ADD KEY(COL1);\n"
				+ "\n"
				+ "/*rejoin all acc1 and add col that shows this is an acc1 that has 2 or more enddates that match it - ie, is potentially bad.*/\n"
				+ "DROP TABLE IF EXISTS TMP3_RPR_ENDDATE_BD;\n"
				+ "CREATE TABLE TMP3_RPR_ENDDATE_BD ENGINE=MYISAM\n"
				+ "SELECT T1.*,T2.CK 'BAD' FROM TMP1_RPR_ENDDATE T1 LEFT JOIN TMP2_RPR_ENDDATE_BD T2 ON T1.ACC1=T2.ACC1 AND T1.TNO1=T2.TNO1 AND T1.COL1=T2.COL1;\n"
				+ "ALTER TABLE TMP3_RPR_ENDDATE_BD ADD KEY(ACC1),ADD KEY(TNO1),ADD KEY(COL1);\n"
				+ "\n"
				+ "/*where bad is null it means if 1 or more tables found match table to repair there was only 1 enddate (which is good).?*/\n"
				+ "INSERT IGNORE INTO TP_EDT\n"
				+ "SELECT ACC1,TNO1,COL1,EDT3 FROM TMP3_RPR_ENDDATE_BD t1 where bad IS NULL\n"
				+ "AND LENGTH(EDT3)=10 and ( abs( left(edt2_1,4)-left(edt2_2,4))<=1 or left(edt2_1,4) not rlike '[12]{1}[09]{1}[0-9]{2}');\n"
				+ "\n"
				+ "/*2. find where 3 or more tables confirm enddate and ONLY 1 is bad (1 enddate doesn't confomr)\n"
				+ "and nothing bad and insert into tp_edt (if 2 are bad - skip).*/\n"
				+ "\n"
				+ "/*number of tables matched for each edt2*/\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP4_RPR_ENDDATE_BD;\n"
				+ "CREATE TABLE TMP4_RPR_ENDDATE_BD ENGINE=MYISAM\n"
				+ "SELECT \n"
				+ "count(*) eCnt,acc1,tno1,col1,edt2_2\n"
				+ "FROM TMP3_RPR_ENDDATE_BD t1 where bad='bad' group by acc1,tno1,col1,edt2_2;\n"
				+ "ALTER TABLE TMP4_RPR_ENDDATE_BD ADD KEY(ACC1),ADD KEY(TNO1), ADD KEY(COL1), ADD KEY(eCnt);\n"
				+ "/*if 1 table has 2 eCnt or greater and only 1 other match - then good*/\n"
				+ "\n"
				+ "set @acc1='1x'; set @TNO1=0; set @col1=0; set @eCnt:=0; set @edt2='1901-01-01';\n"
				+ "DROP TABLE IF EXISTS TMP5_RPR_ENDDATE_BD;\n"
				+ "CREATE TABLE TMP5_RPR_ENDDATE_BD ENGINE=MYISAM\n"
				+ "SELECT case when @acc1=acc1 and @TNO1=tno1 and @col1=col1 and @eCnt>2 and eCnt=1 then left(@edt2,10) else 0 end edt3,\n"
				+ "@eCnt:=eCnt eCn,@acc1:=acc1 acc1,@TNO1:=tno1 tno1,@col1:=col1 col1,@edt2:=edt2_2 edt2_2 FROM TMP4_RPR_ENDDATE_BD\n"
				+ "ORDER BY ACC1,TNO1,COL1,ECNT DESC;\n"
				+ "ALTER TABLE TMP5_RPR_ENDDATE_BD add key(edt3);\n"
				+ "\n"
				+ "insert ignore into tp_edt\n"
				+ "select acc1,tno1,col1,edt3 from TMP5_RPR_ENDDATE_BD where edt3!=0;\n"
				+ "\n"
				+ "/*INSERT ANY WHERE cntID>5 - even two enddates match 1 table. I've not seen a case where both have cntId>5 or where cntId>5 is wrong.*/\n"
				+ "insert ignore into tp_edt\n"
				+ "select t1.acc1,t1.tno1,t1.col1,t1.edt2_2 from TMP1_RPR_ENDDATE t1\n"
				+ " where cI>5\n"
				+ "and (abs( left(edt2_1,4)-left(edt2_2,4))<=1 or left(edt2_1,4) not rlike '[12]{1}[09]{1}[0-9]{2}')\n"
				+ "order by acc1,tno1,col1;\n");

		sb.append("set @acc1='1x'; set @TNO1=0; set @col1=0; set @p2=-1; set @ck='1x';\n"
				+ "DROP TABLE IF EXISTS TMP1_RPR_period;\n"
				+ "CREATE TABLE TMP1_RPR_period ENGINE=MYISAM\n"
				+ "select \n"
				+ "case when @ck=p2_2 and @acc1=acc1 and @tno1=tno1 and @col1=col1 then p2_2 else '' end p3,\n"
				+ "@ck:=case \n"
				+ "when @acc1=acc1 and @tno1=tno1 and @col1=col1 and @p2!=p2_2  then 'bad' \n"
				+ "when @acc1=acc1 and @tno1=tno1 and @col1=col1 then p2_2 \n"
				+ "else p2_2 end ck,\n"
				+ "CNTid cI,ttlrw1 tr1,ttlrw2 tr2,round((cntId/least(ttlrw1,ttlrw2)),1) pMtc,tn1 ,tn2 \n"
				+ ",CIK,V1,RN1,p2_1,@p2:=p2_2 p2_2,edt2_2,@ACC1:=ACC1 acc1,@TNO1:=TNO1 tno1,@COL1:=COL1 col1,ACC2,TNO2,COL2,fd1,fd2,ID,p2_1 p1,p2_2 p2\n"
				+ ",concat('file:///c://backtest/tableparser/',year(fd1),'/qtr',quarter(fd1),'/tables/',acc1,'_',tno1,'.htm') link\n"
				+ ",concat('file:///c://backtest/tableparser/',year(fd2),'/qtr',quarter(fd2),'/tables/',acc2,'_',tno2,'.htm') link2\n"
				+ "from tp_repair_p t1\n"
				+ "order by \n"
				+ "acc1,tno1,col1,p2_2,acc2,tno2,col2 ;\n"
				+ "/*if bad - exclude*/\n"
				+ "\n"
				+ "ALTER TABLE TMP1_RPR_period ADD KEY(ACC1),ADD KEY(TNO1),ADD KEY(COL1);\n"
				+ "/*find acc1 that have two periods match it.*/\n"
				+ "DROP TABLE IF EXISTS TMP2_RPR_period_BD;\n"
				+ "CREATE TABLE TMP2_RPR_period_BD ENGINE=MYISAM\n"
				+ "SELECT * FROM TMP1_RPR_period where ck='bad' GROUP BY ACC1,TNO1,COL1;\n"
				+ "ALTER TABLE TMP2_RPR_period_BD ADD KEY(ACC1),ADD KEY(TNO1),ADD KEY(COL1);\n"
				+ "\n"
				+ "/*rejoin all acc1 and add col that shows this is an acc1 that has 2 or more periods that match it - ie, is potentially bad.*/\n"
				+ "DROP TABLE IF EXISTS TMP3_RPR_period_BD;\n"
				+ "CREATE TABLE TMP3_RPR_period_BD ENGINE=MYISAM\n"
				+ "SELECT T1.*,T2.CK 'BAD' FROM TMP1_RPR_period T1 LEFT JOIN TMP2_RPR_period_BD T2 ON T1.ACC1=T2.ACC1 AND T1.TNO1=T2.TNO1 AND T1.COL1=T2.COL1;\n"
				+ "ALTER TABLE TMP3_RPR_period_BD ADD KEY(ACC1),ADD KEY(TNO1),ADD KEY(COL1);\n"
				+ "\n"
				+ "/*insert all where cntId>1 or meets certain rowname type or there is a p3 value (p3 means 2 or more tbls found w/ same p value )*/\n"
				+ "INSERT IGNORE INTO TP_p\n"
				+ "SELECT \n"
				+ "ACC1,TNO1,COL1,p2 \n"
				+ "FROM TMP3_RPR_period_BD t1 where bad IS NULL \n"
				+ "and (cI>1 or p3 between 3 and 12 or rn1 rlike \n"
				+ "'net |earnin|interes|commiss|servic|expens|costs|fuel|equipm|salar|administr|deprec|sellin|marketi|inventor|income|operat|adverti');\n"
				+ "\n");

		// MysqlConnUtils.executeQuery(sb.toString());

		String dropProc = "DROP PROCEDURE IF EXISTS repairEnddatePeriod"
				+ startYr + "_" + startQ + ";\n"
				+ "CREATE PROCEDURE repairEnddatePeriod" + startYr + "_"
				+ startQ + "()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
		MysqlConnUtils.executeQuery("call repairEnddatePeriod" + startYr + "_"
				+ startQ + "();\n");
		sb.delete(0, sb.toString().length());

		// Calendar cal = Calendar.getInstance();
		// cal.setTime(cal.getTime());
		// int endYr = cal.get(Calendar.YEAR);
		String query = "";
		int qtr = startQ;
		int q = qtr;

		for (int yr = startYr; yr <= endYr; yr++) {
			if (yr == 1993 && qtr < 3) {
				qtr = 3;
			}
			for (q = qtr; q <= endQ; q++) {

				query = "update ignore bac_tp_raw"
						+ yr
						+ "qtr"
						+ startQ
						+ " t1 inner join tp_edt t2 on \n"
						+ "t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col \n"
						+ "set t1.edt2=t2.edt2/*,ended=concat('edt2',ended)*/ \n"
						+ "where t1.edt2!=t2.edt2;\n";

				MysqlConnUtils.executeQuery(query);

				query = "update ignore bac_tp_raw"
						+ yr
						+ "qtr"
						+ startQ
						+ " t1 inner join tp_p t2 on \n"
						+ "t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col \n"
						+ "set t1.p2=t2.p2/*,ended=concat('p2',ended)*/ \n"
						+ "where t1.p2!=t2.p2;\n";

				MysqlConnUtils.executeQuery(query);
			}
			qtr = 1;
		}

		sb.delete(0, sb.toString().length());

		// MARK BAD TABLES
		sb.append("\n"
				+ "/*In tp_misMatch_edt and tp_repair_p (same for period): cntId equals the no of rows (rowname,value) that match between the two tables columns. If there aren't at least 4 or 5 - there's low reliability. CntEdt2_ : counts how many tables have the exact same enddate for that Id (same enddate for in many tables for same cik,value and rowname). High vales for all cnt types is good. ts_used means tablesentence was used to build enddate.\n"
				+ "\n"
				+ "ATTEMPTED MANY FILTERS TO ATTEMPT TO SALVAGE ENDDATES AND PERIODS. \n"
				+ "THIS IS THE BEST YOU CAN DO WITHOUT SIMPLY RANDOM FIXES. THIS ERRS \n"
				+ "IN MISSING ERROS VERSUS OVERINCLUSION AND THEREBY MARKING BAD GOOD \n"
				+ "ENDDATES/PERIODS.\n"
				+ "*/\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP_MISMATCH_EDT;\n"
				+ "CREATE TABLE TMP_MISMATCH_EDT ENGINE=MYISAM\n"
				+ "select CNTid cI,ttlrw1 tr1,ttlrw2 tr2,round((cntId/least(ttlrw1,ttlrw2)),1) pMtc ,round((cntEdt2_1/cntEdt2_2),2) cEr,cntEdt2_1 cE1,cntP2_1 cP1,cntedt2_2 cE2,cntP2_2 cP2,tn1,tn2,case\n"
				+ "when (cntEdt2_1/cntEdt2_2)>=3 /*and tsUsed_1=0 */then edt2_1 /*'1-a1Gd'*/ /*take enddate that is 3 times more frequent*/\n"
				+ "when (cntEdt2_2/cntEdt2_1)>=3 /*and tsUsed_2=0 */then edt2_2 /*'1-a2Gd'*/\n"
				+ "when fd1<=date_add(edt2_1,interval 7 day) or datediff(fd1,edt2_1)>3650 then 'a1Bd' \n"
				+ "when fd2<=date_add(edt2_2,interval 7 day) or datediff(fd2,edt2_2)>3650 then 'a2Bd' \n"
				+ "else '' end ck\n"
				+ ",CIK,V1,RN1,edt2_1,edt2_2/*,fd1,fd2,ID*/,p2_1 p1,ACC1,TNO1,COL1,p2_2,ACC2,TNO2,COL2 \n"
				+ "/*,concat('file:///c://backtest/tableparser/',year(fd1),'/qtr',quarter(fd1),'/tables/',acc1,'_',tno1,'.htm') link\n"
				+ ",concat('file:///c://backtest/tableparser/',year(fd2),'/qtr',quarter(fd2),'/tables/',acc2,'_',tno2,'.htm') link2*/\n"
				+ "from tp_misMatch_edt t1 \n"
				+ "/*for now this focuses just on tn='is'. However the where conditions below can be modified and or\n"
				+ "rowname filter in prep_tp_id to hunt for misMatch enddates for CF and BS*/\n"
				+ "where\n"
				+ "(cntId>3 and tn1='is') or (cntId>4 and tn1='cf');\n"
				+ "ALTER TABLE TMP_MISMATCH_EDT ADD KEY(ACC1),ADD KEY(TNO1),ADD KEY(COL1);\n"
				+ "\n"
				+ "drop table if exists TMP2_MISMATCH_EDT;\n"
				+ "create table TMP2_MISMATCH_EDT engine=myisam\n"
				+ "select COUNT(DISTINCT(CK)) cEdt,acc1,tno1,col1 FROM TMP_MISMATCH_EDT T1 \n"
				+ "WHERE LENGTH(CK)=10\n"
				+ "GROUP BY ACC1,TNO1,COL1;\n"
				+ "ALTER TABLE TMP2_MISMATCH_EDT ADD KEY(ACC1),ADD KEY(TNO1),ADD KEY(COL1), ADD KEY(CEDT);\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP_TP_EDT;\n"
				+ "CREATE TABLE TMP_TP_EDT ENGINE=MYISAM\n"
				+ "select T1.ACC1 accno,T1.TNO1 tno,T1.COL1 col,T1.CK edt2 from TMP_MISMATCH_EDT t1 left join TMP2_MISMATCH_EDT t2\n"
				+ "on t1.acc1=t2.acc1 and t1.tno1=t2.tno1 and t1.col1=t2.col1\n"
				+ "where cEdt=1 and length(ck)=10 group by t1.acc1,t1.tno1,t1.col1;\n"
				+ "ALTER TABLE TMP_TP_EDT ADD KEY(ACCNO),ADD KEY(TNO),ADD KEY(COL);\n"
				+ "\n"
				+ "INSERT IGNORE INTO TP_EDT\n"
				+ "select * from TMP_TP_EDT;\n"
				+ "\n"
				+ "/*THIS IS IT - THESE ARE THE BAD ONES==>>>*/\n"
				+ "INSERT IGNORE INTO TP_EDT_BAD\n"
				+ "select T1.ACC1,T1.TNO1,T1.COL1,T1.CK from TMP_MISMATCH_EDT t1 left join TMP2_MISMATCH_EDT t2\n"
				+ "on t1.acc1=t2.acc1 and t1.tno1=t2.tno1 and t1.col1=t2.col1\n"
				+ "where cEdt IS NULL group by t1.acc1,t1.tno1,t1.col1;\n"
				+ "\n");

		sb.append("set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "DROP TABLE IF EXISTS TMP_MISMATCH_p;\n"
				+ "CREATE TABLE TMP_MISMATCH_p ENGINE=MYISAM\n"
				+ "select /*tsUsed_1 ts1,tsUsed_2 ts2,*/CNTid cI,/*ttlrw1 tr1,ttlrw2 tr2,round((cntId/least(ttlrw1,ttlrw2)),1) pMtc ,*/round((cntp2_1/cntp2_2),2) cEr,cntP2_1 cP1,cntP2_2 cP2,/*tn1,tn2,*/case\n"
				+ "when (cntp2_1/cntp2_2)>=3 /*and tsUsed_1=0*/ then p2_1 /*'1-a1Gd'*/ /*take enddate that is 3 times more frequent*/\n"
				+ "when (cntp2_2/cntp2_1)>=3 /*and tsUsed_2=0*/ then p2_2 /*'1-a2Gd'*/\n"
				+ "when fd1<=date_add(p2_1,interval 7 day) or datediff(fd1,p2_1)>3650 then 'a1Bd' \n"
				+ "when fd2<=date_add(p2_2,interval 7 day) or datediff(fd2,p2_2)>3650 then 'a2Bd' \n"
				+ "else '' end ck\n"
				+ ",CIK,V1,RN1,p2_1,p2_2/*,fd1,fd2,ID*/,ACC1,TNO1,COL1,ACC2,TNO2,COL2 \n"
				+ ",concat('file:///c://backtest/tableparser/',year(fd1),'/qtr',quarter(fd1),'/tables/',acc1,'_',tno1,'.txt') link\n"
				+ ",concat('file:///c://backtest/tableparser/',year(fd2),'/qtr',quarter(fd2),'/tables/',acc2,'_',tno2,'.txt') link2\n"
				+ "from tp_misMatch_p t1 \n"
				+ "/*for now this focuses just on tn='is'. However the where conditions below can be modified and or\n"
				+ "rowname filter in prep_tp_id to hunt for misMatch enddates for CF and BS*/\n"
				+ "where\n"
				+ "(cntId>3 and tn1='is') or (cntId>5 and tn1='cf');\n"
				+ "ALTER TABLE TMP_MISMATCH_p ADD KEY(ACC1),ADD KEY(TNO1),ADD KEY(COL1);\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP2_MISMATCH_P;\n"
				+ "CREATE TABLE TMP2_MISMATCH_P ENGINE=MYISAM\n"
				+ "SELECT COUNT(DISTINCT(CK)) CP,ACC1,TNO1,COL1,ck FROM TMP_MISMATCH_P T1 \n"
				+ "GROUP BY ACC1,TNO1,COL1;\n"
				+ "ALTER TABLE TMP2_MISMATCH_P ADD KEY(ACC1),ADD KEY(TNO1),ADD KEY(COL1), ADD KEY(CP);\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS TMP_TP_p;\n"
				+ "CREATE TABLE TMP_TP_p ENGINE=MYISAM\n"
				+ "select T1.ACC1 accno,T1.TNO1 tno,T1.COL1 col,T1.CK p2 from TMP_MISMATCH_p t1 left join TMP2_MISMATCH_p t2\n"
				+ "on t1.acc1=t2.acc1 and t1.tno1=t2.tno1 and t1.col1=t2.col1\n"
				+ "where cp=1 and t1.ck between 3 and 12 group by t1.acc1,t1.tno1,t1.col1;\n"
				+ "ALTER TABLE TMP_TP_p ADD KEY(ACCNO),ADD KEY(TNO),ADD KEY(COL);\n"
				+ "\n"
				+ "INSERT IGNORE INTO TP_p\n"
				+ "select * from TMP_TP_p where p2 between 3 and 12;\n"
				+ "\n"
				+ "/*THIS IS IT - THESE ARE THE BAD ONES==>>>*/\n"
				+ "INSERT IGNORE INTO TP_p_BAD\n"
				+ "select T1.ACC1,T1.TNO1,T1.COL1,T1.CK from TMP_MISMATCH_p t1 left join TMP2_MISMATCH_p t2\n"
				+ "on t1.acc1=t2.acc1 and t1.tno1=t2.tno1 and t1.col1=t2.col1\n"
				+ "where t2.ck='' group by t1.acc1,t1.tno1,t1.col1;\n" + "\n");

		MysqlConnUtils.executeQuery(sb.toString());
		sb.delete(0, sb.toString().length());

		qtr = startQ;
		for (int yr = startYr; yr <= endYr; yr++) {
			if (yr == 1993 && qtr < 3) {
				qtr = 3;
			}
			for (q = qtr; q <= endQ; q++) {

				query = "update bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ " t1 inner join TP_EDT_BAD t2 on \n"
						+ "t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col \n"
						+ "set t1.yr='bad';/*,ended=concat('edt2',ended)*/ \n";
				MysqlConnUtils.executeQuery(query);

				query = "update bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ " t1 inner join tmp_tp_edt t2 on \n"
						+ "t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col \n"
						+ "set t1.edt2=t2.edt2/*,ended=concat('p2',ended)*/ \n"
						+ "where t1.edt2!=t2.edt2;\n";

				MysqlConnUtils.executeQuery(query);

				query = "update bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ " t1 inner join TP_P_BAD t2 on \n"
						+ "t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col \n"
						+ "set t1.yr='bad';/*,ended=concat('p2',ended)*/ \n";
				MysqlConnUtils.executeQuery(query);
				query = "";

				query = "update bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ " t1 inner join tmp_tp_p t2 on \n"
						+ "t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col \n"
						+ "set t1.p2=t2.p2/*,ended=concat('p2',ended)*/ \n"
						+ "where t1.p2!=t2.p2;\n";

				MysqlConnUtils.executeQuery(query);
				query = "";
			}
			qtr = 1;
		}
	}

	public void fixBlankRownames(String table) throws SQLException, FileNotFoundException {

		String yr = table.substring(table.indexOf("tp_raw") + 6,
				table.indexOf("tp_raw") + 10);
		String q = table.substring(table.indexOf("tp_raw") + 13,
				table.indexOf("tp_raw") + 14);

		StringBuffer sb = new StringBuffer();

		sb.append("\n"
				+ "/*GET EACH BLANK ROW WHERE THERE IS A TTL OR NET. THEN FOR THAT TNO GET THE mnTR THAT IMMEDIATLEY PRECEDES THE FIRST STT=TTL*/\n"
				+ "DROP TABLE IF EXISTS TMP_TTL_NET_"
				+ yr
				+ "_"
				+ q
				+ ";\n"
				+ "create TABLE TMP_TTL_NET_"
				+ yr
				+ "_"
				+ q
				+ " engine=myisam\n"
				+ "SELECT accno,tno,trow,ttl FROM BAC_TP_RAW"
				+ yr
				+ "QTR"
				+ q
				+ " WHERE LENGTH(ROWNAME)<2 AND TTL>0 and col=1;\n"
				+ "ALTER TABLE tmp_ttl_net_"
				+ yr
				+ "_"
				+ q
				+ " ADD KEY(ACCNO), ADD KEY(TNO),ADD KEY(ttl);\n"
				+ "/*these are all the blank rows*/\n"
				+ "\n"
				+ "\n"
				+ "/*get min trow for each stt. Header row should be be right before it*/\n"
				+ "DROP TABLE IF EXISTS TMP2_TTL_NET_"
				+ yr
				+ "_"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP2_TTL_NET_"
				+ yr
				+ "_"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "SELECT accno,tno,min(trow) mnTr,stt from BAC_TP_RAW"
				+ yr
				+ "QTR"
				+ q
				+ " where stt>0 and col>0 group by accno,tno,stt;\n"
				+ "ALTER TABLE tmp2_ttl_net_"
				+ yr
				+ "_"
				+ q
				+ " ADD KEY(ACCNO), ADD KEY(TNO),ADD KEY(stt), add key(mnTr);\n"
				+ "\n"
				+ "\n"
				+ "/*now get header rows (1 before first stt/sub)*/\n"
				+ "DROP TABLE IF EXISTS TMP4_TTL_NET_"
				+ yr
				+ "_"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP4_TTL_NET_"
				+ yr
				+ "_"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "select mntr,t1.accno,t1.tno,t2.stt,rowname headerrow from bac_tp_raw"
				+ yr
				+ "QTR"
				+ q
				+ " t1 \n"
				+ "inner join TMP2_TTL_NET_"
				+ yr
				+ "_"
				+ q
				+ " t2 on t1.accno=t2.accno and t1.tno=t2.tno\n"
				+ "where t1.trow=t2.mntr-1 and length(t1.rowname)>2 and col=0;\n"
				+ "ALTER TABLE tmp4_ttl_net_"
				+ yr
				+ "_"
				+ q
				+ " ADD KEY(ACCNO), ADD KEY(TNO),ADD KEY(stt);\n"
				+ "\n"
				+ "\n"
				+ "/*now join each header row to ttl blank row*/\n"
				+ "DROP TABLE IF EXISTS TMP5_TTL_NET_"
				+ yr
				+ "_"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP5_TTL_NET_"
				+ yr
				+ "_"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "SELECT t2.trow,t2.ttl,t2.accno,t2.tno,t1.headerrow FROM TMP4_TTL_NET_"
				+ yr
				+ "_"
				+ q
				+ " T1 inner join TMP_TTL_NET_"
				+ yr
				+ "_"
				+ q
				+ " t2\n"
				+ "on t1.accno=t2.accno and t1.tno=t2.tno and t1.stt=t2.ttl;\n"
				+ "ALTER TABLE tmp5_ttl_net_"
				+ yr
				+ "_"
				+ q
				+ " ADD KEY(ACCNO), ADD KEY(TNO),ADD KEY(ttl), ADD KEY(TROW);\n"
				+ "\n"
				+ "UPDATE IGNORE BAC_TP_RAW"
				+ yr
				+ "QTR"
				+ q
				+ " \n"
				+ "/*select HEADERROW, T1.* FROM  BAC_TP_RAW"
				+ yr
				+ "QTR"
				+ q
				+ "*/ T1 INNER JOIN TMP5_TTL_NET_"
				+ yr
				+ "_"
				+ q
				+ " T2 ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO\n"
				+ "AND (T1.TROW=T2.TROW)\n"
				+ "SET T1.ROWNAME=T2.HEADERROW\n"
				+ "WHERE HEADERROW NOT RLIKE 'IN (THOUSAND|MILLION|OUTSTAND|SHARES|ISSUE)';\n"
				+ "\n"
				+ "\n"
				+ "\n"
				+ "/*GET EACH BLANK ROW WHERE THERE IS A NET OR NET. THEN FOR THAT TNO GET THE mnTR THAT IMMEDIATLEY PRECEDES THE FIRST SUB=NET*/\n"
				+ "DROP TABLE IF EXISTS TMP_NET_NET_"
				+ yr
				+ "_"
				+ q
				+ ";\n"
				+ "create TABLE TMP_NET_NET_"
				+ yr
				+ "_"
				+ q
				+ " engine=myisam\n"
				+ "SELECT accno,tno,trow,NET FROM BAC_TP_RAW"
				+ yr
				+ "QTR"
				+ q
				+ " WHERE LENGTH(ROWNAME)<2 AND NET>0 and col=1;\n"
				+ "ALTER TABLE tmp_NET_net_"
				+ yr
				+ "_"
				+ q
				+ " ADD KEY(ACCNO), ADD KEY(TNO),ADD KEY(NET);\n"
				+ "/*these are all the blank rows*/\n"
				+ "\n"
				+ "\n"
				+ "/*get min trow for each SUB. Header row should be be right before it*/\n"
				+ "DROP TABLE IF EXISTS TMP2_NET_NET_"
				+ yr
				+ "_"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP2_NET_NET_"
				+ yr
				+ "_"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "SELECT accno,tno,min(trow) mnTr,SUB from BAC_TP_RAW"
				+ yr
				+ "QTR"
				+ q
				+ " where SUB>0 and col>0 group by accno,tno,SUB;\n"
				+ "ALTER TABLE tmp2_NET_net_"
				+ yr
				+ "_"
				+ q
				+ " ADD KEY(ACCNO), ADD KEY(TNO),ADD KEY(SUB), add key(mnTr);\n"
				+ "\n"
				+ "\n"
				+ "/*now get header rows (1 before first SUB/sub)*/\n"
				+ "DROP TABLE IF EXISTS TMP4_NET_NET_"
				+ yr
				+ "_"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP4_NET_NET_"
				+ yr
				+ "_"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "select mntr,t1.accno,t1.tno,t2.SUB,rowname headerrow from bac_tp_raw"
				+ yr
				+ "QTR"
				+ q
				+ " t1 \n"
				+ "inner join TMP2_NET_NET_"
				+ yr
				+ "_"
				+ q
				+ " t2 on t1.accno=t2.accno and t1.tno=t2.tno\n"
				+ "where t1.trow=t2.mntr-1 and length(t1.rowname)>2 and col=0;\n"
				+ "ALTER TABLE tmp4_NET_net_"
				+ yr
				+ "_"
				+ q
				+ " ADD KEY(ACCNO), ADD KEY(TNO),ADD KEY(SUB);\n"
				+ "\n"
				+ "\n"
				+ "/*now join each header row to NET blank row*/\n"
				+ "DROP TABLE IF EXISTS TMP5_NET_NET_"
				+ yr
				+ "_"
				+ q
				+ ";\n"
				+ "CREATE TABLE TMP5_NET_NET_"
				+ yr
				+ "_"
				+ q
				+ " ENGINE=MYISAM\n"
				+ "SELECT t2.trow,t2.NET,t2.accno,t2.tno,t1.headerrow FROM TMP4_NET_NET_"
				+ yr
				+ "_"
				+ q
				+ " T1 inner join TMP_NET_NET_"
				+ yr
				+ "_"
				+ q
				+ " t2\n"
				+ "on t1.accno=t2.accno and t1.tno=t2.tno and t1.SUB=t2.NET;\n"
				+ "ALTER TABLE tmp5_NET_net_"
				+ yr
				+ "_"
				+ q
				+ " ADD KEY(ACCNO), ADD KEY(TNO),ADD KEY(NET), ADD KEY(TROW);\n"
				+ "\n"
				+ "UPDATE IGNORE BAC_TP_RAW"
				+ yr
				+ "QTR"
				+ q
				+ " \n"
				+ "/*select HEADERROW, T1.* FROM BAC_TP_RAW"
				+ yr
				+ "QTR"
				+ q
				+ "*/ T1 INNER JOIN TMP5_NET_NET_"
				+ yr
				+ "_"
				+ q
				+ " T2 ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO\n"
				+ "AND (T1.TROW=T2.TROW) \n"
				+ "SET T1.ROWNAME=T2.HEADERROW\n"
				+ "WHERE HEADERROW NOT RLIKE 'IN (THOUSAND|MILLION|OUTSTAND|SHARES|ISSUE)';\n");

		String dropProc = "DROP PROCEDURE IF EXISTS fixBlankRownames" + yr
				+ "_" + q + ";\n" + "CREATE PROCEDURE fixBlankRownames" + yr
				+ "_" + q + "()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
		MysqlConnUtils.executeQuery("call fixBlankRownames" + yr + "_" + q
				+ "();\n");
		sb.delete(0, sb.toString().length());

	}

	public void fixTableSentenceEnddates(String table) throws SQLException, FileNotFoundException {

		String yr = table.substring(table.indexOf("tp_raw") + 6,
				table.indexOf("tp_raw") + 10);
		String q = table.substring(table.indexOf("tp_raw") + 13,
				table.indexOf("tp_raw") + 14);

		StringBuffer sb = new StringBuffer(
				"\n"
						+ "/*at this point I've identified tables that have 1 distinct month value in CH (columnPattern mCntD:1) and there's no month value in \n"
						+ "colunmText. So month value had to have come from TS. Next query will determine if month in allColText is the same as in EDT. If not*/\n"
						+ "\n" + "\nset sql_mode = ALLOW_INVALID_DATES;\n"
						+ "DROP TABLE IF EXISTS tmp_ALLCOL_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "CREATE TABLE tmp_ALLCOL_"
						+ yr
						+ "qtr"
						+ q
						+ " ENGINE=MYISAM\n"
						+ "select \n"
						+ "case\n"
						+ "when substring_index(substring_index(allColText,'L5C1:',-1),'|',1) rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' \n"
						+ "then substring_index(substring_index(allColText,'L5C1:',-1),'|',1)\n"
						+ "when substring_index(substring_index(allColText,'L4C1:',-1),'|',1) rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' \n"
						+ "then substring_index(substring_index(allColText,'L4C1:',-1),'|',1)\n"
						+ "when substring_index(substring_index(allColText,'L3C1:',-1),'|',1) rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' \n"
						+ "then substring_index(substring_index(allColText,'L3C1:',-1),'|',1)\n"
						+ "when substring_index(substring_index(allColText,'L2C1:',-1),'|',1) rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' \n"
						+ "then substring_index(substring_index(allColText,'L2C1:',-1),'|',1)\n"
						+ "when substring_index(substring_index(allColText,'L1C1:',-1),'|',1) rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' \n"
						+ "then substring_index(substring_index(allColText,'L1C1:',-1),'|',1) else '' end\n"
						+ "c1,\n"
						+ "case\n"
						+ "when substring_index(substring_index(allColText,'L5C2:',-1),'|',1) rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' \n"
						+ "then substring_index(substring_index(allColText,'L5C2:',-1),'|',1)\n"
						+ "when substring_index(substring_index(allColText,'L4C2:',-1),'|',1) rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' \n"
						+ "then substring_index(substring_index(allColText,'L4C2:',-1),'|',1)\n"
						+ "when substring_index(substring_index(allColText,'L3C2:',-1),'|',1) rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' \n"
						+ "then substring_index(substring_index(allColText,'L3C2:',-1),'|',1)\n"
						+ "when substring_index(substring_index(allColText,'L2C2:',-1),'|',1) rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' \n"
						+ "then substring_index(substring_index(allColText,'L2C2:',-1),'|',1)\n"
						+ "when substring_index(substring_index(allColText,'L1C2:',-1),'|',1) rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' \n"
						+ "then substring_index(substring_index(allColText,'L1C2:',-1),'|',1) else '' end\n"
						+ "c2,\n"
						+ "case\n"
						+ "when substring_index(substring_index(allColText,'L5C3:',-1),'|',1) rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' \n"
						+ "then substring_index(substring_index(allColText,'L5C3:',-1),'|',1)\n"
						+ "when substring_index(substring_index(allColText,'L4C3:',-1),'|',1) rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' \n"
						+ "then substring_index(substring_index(allColText,'L4C3:',-1),'|',1)\n"
						+ "when substring_index(substring_index(allColText,'L3C3:',-1),'|',1) rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' \n"
						+ "then substring_index(substring_index(allColText,'L3C3:',-1),'|',1)\n"
						+ "when substring_index(substring_index(allColText,'L2C3:',-1),'|',1) rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' \n"
						+ "then substring_index(substring_index(allColText,'L2C3:',-1),'|',1)\n"
						+ "when substring_index(substring_index(allColText,'L1C3:',-1),'|',1) rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' \n"
						+ "then substring_index(substring_index(allColText,'L1C3:',-1),'|',1) else '' end\n"
						+ "C3,edt2,\n"
						+ "ACCNO,tno,col/*,tn,row,rowname,value,edt1,p1,p2,tc,columnText,columnPattern,allColText,tsShort,tsLong\n"
						+ ",concat('file:///c://backtest/tableparser/',year(filedate),'/qtr',quarter(filedate),'/tables/',accno,'_',tno,'.txt') link*/\n"
						+ "from bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ " where allcoltext rlike '(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)'\n"
						+ "and \n"
						+ "columntext not rlike 'jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec' \n"
						+ "and columnPattern rlike 'mCntD:1'\n"
						+ "and \n"
						+ "tsShort rlike 'm'\n"
						+ "and trow between 1 and 5 and col>0 group by accno,tno,col ;\n"
						+ "\nset sql_mode = ALLOW_INVALID_DATES;\n"
						+ "DROP TABLE IF EXISTS tmp2_ALLCOL_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "CREATE TABLE TMP2_ALLCOL_"
						+ yr
						+ "qtr"
						+ q
						+ " ENGINE=MYISAM\n"
						+ "\n"
						+ "SELECT \n"
						+ "case \n"
						+ "when c1 rlike 'Dec' and month(edt2)!=12\n"
						+ "and c1 rlike 'Dec.{1,7}[23]{1}[0-9]{1}'\n"
						+ "then concat(left(edt2,4),'-12-30')\n"
						+ "\n"
						+ "when c1 rlike 'Nov' and month(edt2)!=11 \n"
						+ "and c1 rlike 'Nov.{1,7}[23]{1}[0-9]{1}'\n"
						+ "then concat(left(edt2,4),'-11-30')\n"
						+ "\n"
						+ "when c1 rlike 'Oct' and month(edt2)!=10 \n"
						+ "and c1 rlike 'Oct.{1,6}[23]{1}[0-9]{1}'\n"
						+ "then concat(left(edt2,4),'-10-30')\n"
						+ "\n"
						+ "when c1 rlike 'Sep' and month(edt2)!=9 \n"
						+ "and c1 rlike 'Sep.{1,8}[23]{1}[0-9]{1}'\n"
						+ "then concat(left(edt2,4),'-09-30')\n"
						+ "\n"
						+ "when c1 rlike 'Aug' and month(edt2)!=8 \n"
						+ "and c1 rlike 'Aug.{1,5}[23]{1}[0-9]{1}'\n"
						+ "then concat(left(edt2,4),'-08-30')\n"
						+ "\n"
						+ "when c1 rlike 'Jul' and month(edt2)!=7 \n"
						+ "and c1 rlike 'Jul.{1,4}[23]{1}[0-9]{1}'\n"
						+ "then concat(left(edt2,4),'-07-30')\n"
						+ "\n"
						+ "when c1 rlike 'Jun' and month(edt2)!=6 \n"
						+ "and c1 rlike 'Jun.{1,3}[23]{1}[0-9]{1}'\n"
						+ "then concat(left(edt2,4),'-06-30')\n"
						+ "\n"
						+ "when c1 rlike 'May ' and month(edt2)!=5 \n"
						+ "and c1 rlike 'May [23]{1}[0-9]{1}'\n"
						+ "then concat(left(edt2,4),'-05-30')\n"
						+ "\n"
						+ "when c1 rlike 'Apr' and month(edt2)!=4 \n"
						+ "and c1 rlike 'Apr.{1,4}[23]{1}[0-9]{1}'\n"
						+ "then concat(left(edt2,4),'-04-30')\n"
						+ "\n"
						+ "when c1 rlike 'Mar' and month(edt2)!=3 \n"
						+ "and c1 rlike 'Mar.{1,4}[23]{1}[0-9]{1}'\n"
						+ "then concat(left(edt2,4),'-03-30')\n"
						+ "\n"
						+ "when c1 rlike 'Feb' and month(edt2)!=2 \n"
						+ "and c1 rlike 'Feb.{1,7}[23]{1}[0-9]{1}'\n"
						+ "then concat(left(edt2,4),'-02-28')\n"
						+ "\n"
						+ "when c1 rlike 'Jan' and month(edt2)!=1 \n"
						+ "and c1 rlike 'Jan.{1,6}[23]{1}[0-9]{1}'\n"
						+ "then concat(left(edt2,4),'-01-30')\n"
						+ "\n"
						+ "else 0 end EDT3,\n"
						+ "case when length(c1)>0 then c1 when length(c2)>0 then c2 when length(c3)>0 then c3 end ctxt\n"
						+ ",T1.accno,tno,col\n"
						+ "\n"
						+ "/*,link*/ FROM tmp_ALLCOL_"
						+ yr
						+ "qtr"
						+ q
						+ " t1;\n"
						+ "\n"
						+ "ALTER TABLE tmp2_ALLCOL_"
						+ yr
						+ "qtr"
						+ q
						+ " ADD KEY(ACCNO),ADD KEY(TNO),ADD KEY(COL),ADD KEY(EDT3);\n"
						+ "\n\nset sql_mode = ALLOW_INVALID_DATES;\n"
						+ "\n"
						+ "update ignore \n"
						+ "/*SELECT EDT3,EDT2,LINK, T1.* FROM */\n"
						+ "bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ " t1 inner join tmp2_ALLCOL_"
						+ yr
						+ "qtr"
						+ q
						+ " T2 ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.COL=T2.COL\n"
						+ "SET T1.EDT2=T2.EDT3 \n"
						+ "WHERE EDT3!=0\n"
						+ "/*group by accno,tno,col*/\n" + ";\n");

		String dropProc = "DROP PROCEDURE IF EXISTS fixTableSentenceEnddates"
				+ yr + "_" + q + ";\n"
				+ "CREATE PROCEDURE fixTableSentenceEnddates" + yr + "_" + q
				+ "()\n\n begin\n\n";
		String endProc = "\n\nend;";

		MysqlConnUtils.executeQuery(dropProc + sb.toString() + endProc);
		MysqlConnUtils.executeQuery("call fixTableSentenceEnddates" + yr + "_"
				+ q + "();\n");
		sb.delete(0, sb.toString().length());

	}

	public static void fetchCik(int startYr, int endYr, int cik,
			String rownameRlike) throws SQLException, FileNotFoundException {

		int yr, qtr = 1, q = 1, cnt = 0;
		String table;
		StringBuffer sb = new StringBuffer();

		for (yr = startYr; yr <= endYr; yr++) {
			if (yr == 1993 && qtr < 3) {
				qtr = 3;
			}

			for (q = qtr; q <= 4; q++) {
				table = "bac_tp_raw" + yr + "qtr" + q;
				if (cnt == 0) {
					cnt++;
					sb.append("drop table if exists tmp_bac_tp_rawYrs;\n"
							+ "create table tmp_bac_tp_rawYrs engine=myisam\n"
							+ "select * from " + table + " where cik=" + cik
							+ " and rowname rlike '" + rownameRlike + "';\n");
				}
				sb.append("insert ignore into tmp_bac_tp_rawYrs\n"
						+ "select * from " + table + " where cik=" + cik
						+ " and rowname rlike '" + rownameRlike + "';\n");

			}
		}
		
		MysqlConnUtils.executeQuery(sb.toString());

	}

	public void getEnddateOrPeriodSmartMatch(int startYr, int endYr,
			int startQ, int endQ) throws SQLException, FileNotFoundException {

		StringBuffer sb = new StringBuffer(
				"\n"
						+ "/*READ THIS: First I create procedures: tmp_tp_sales,\n"
						+ "insertIntoTMP_TP_Sales_subtractSameAcc,....sameRn PROCEDURES. \n"
						+ "Then these procs get called. tmp_tp_sales is run based on tp_sales_to_scrub. ). */\n"
						+ "DROP PROCEDURE IF EXISTS tmp_tp_sales;\n"
						+ "CREATE PROCEDURE tmp_tp_sales()\n"
						+ "\n"
						+ "BEGIN\n"
						+ "\n"
						+ "\r\rset sql_mode = ALLOW_INVALID_DATES;\r\r"
						+ "DROP TABLE IF EXISTS tmp_tp_sales;\n"
						+ "CREATE TABLE `tmp_tp_sales` (\n"
						+ "  `accno` varchar(20) NOT NULL DEFAULT '-1',\n"
						+ "  `filedate` varchar(12) NOT NULL DEFAULT '',\n"
						+ "  `cik` int(11) NOT NULL DEFAULT 0,\n"
						+ "  `tno` int(5) NOT NULL DEFAULT '-1',\n"
						+ "  `trow` int(5) NOT NULL DEFAULT '-1' COMMENT 'table row',\n"
						+ "  `row` int(5) NOT NULL DEFAULT '-1',\n"
						+ "  `rowname` varchar(150) NOT NULL DEFAULT '',\n"
						+ "  `origRowname` varchar(255) NOT NULL DEFAULT '',\n"
						+ "  `edt2` varchar(12) NOT NULL DEFAULT '-1',\n"
						+ "  `edtId` int(5) NOT NULL DEFAULT 0 COMMENT 'if html - per1 parsed from cell, if txt per1 parsed based on col hdg ratio matching',\n"
						+ "  `p2` int(3) NOT NULL DEFAULT 0 COMMENT 'if html - per1 parsed from cell, if txt per1 parsed based on col hdg ratio matching',\n"
						+ "  `value` double(18,2) NOT NULL DEFAULT 0,\n"
						+ "  `dec` int(11) NOT NULL DEFAULT 0,\n"
						+ "  `calc` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'if value obtained from two accno - 1 eles reported is 0',\n"
						+ "  `dif` double NOT NULL DEFAULT 0 COMMENT 'max filedate less least filedate',\n"
						+ "  `rnId` int(6) NOT NULL DEFAULT 0 COMMENT 'this is Id assigned to each unique left(rowname,5)',\n"
						+ "  PRIMARY KEY(CIK,EDTID,P2,value,rnId), \n"
						+ "KEY (CIK,EDTID,P2,RNID),\n"
						+ "KEY (ACCNO),\n"
						+ "KEY (VALUE),\n"
						+ "KEY (CIK),\n"
						+ "KEY (rnId),\n"
						+ "KEY (edtId),\n"
						+ "KEY (p2),\n"
						+ "KEY (edtId),\n"
						+ "KEY (edt2),\n"
						+ "KEY (filedate),\n"
						+ "KEY (calc),\n"
						+ "KEY (dif),\n"
						+ "KEY (CIK,EDTID,P2)\n"
						+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n"
						+ "\n"
						+ "\n"
						+ "END;\n"
						+ "\n"
						+ "\n"
						+ "DROP PROCEDURE IF EXISTS insertIntoTMP_TP_Sales_subtractSameAcc;\n"
						+ "CREATE PROCEDURE insertIntoTMP_TP_Sales_subtractSameAcc()\n"
						+ "\n"
						+ "BEGIN\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "INSERT IGNORE INTO TMP_TP_SALES\n"
						+ "SELECT \n"
						+ "\n"
						+ "/*round((t1.value/(t2.value*t1.p2/t2.p2)),1) Vr,round((T2.VALUE/(T1.VALUE*T1.P2/T2.P2)),1) Vr2,\n"
						+ "t1.`dec` t1d,t2.`dec` t2d,t1.edt2 t1edt2,t2.edt2 t2edt2,t1.p2 t1p2,t2.p2 t2p2,t1.value t1v,t2.value t2v,left(t2.rowname,5) t1rn5,*/\n"
						+ "\n"
						+ "t1.accno,greatest(t1.filedate,t2.filedate) filedate,t1.cik,t1.tno,t1.trow,t1.row,t1.rowname,t1.origrowname,\n"
						+ "case when t1.edt2=t2.edt2 then cast(date_sub(t1.edt2,interval t2.p2 month) as binary) else t1.edt2 end edt2\n"
						+ ",case when t1.edtId=t2.edtId then t1.edtId-t2.p2 else t1.edtId end edtId\n"
						+ ",t1.p2-t2.p2 p2\n"
						+ ",case \n"
						+ "when t1.value/(t2.value*t1.p2/t2.p2) BETWEEN 1/120     AND 1/80 then t1.value*100 - t2.value \n"
						+ "when t1.value/(t2.value*t1.p2/t2.p2) BETWEEN 1/2500    AND 1/250 then t1.value*1000 - t2.value \n"
						+ "when t1.value/(t2.value*t1.p2/t2.p2) BETWEEN 1/2500000 AND 1/250000 then t1.value*1000000 - t2.value \n"
						+ "when t1.value/(t2.value*t1.p2/t2.p2) BETWEEN 80 AND 120 then t1.value - t2.value*100 \n"
						+ "when t1.value/(t2.value*t1.p2/t2.p2) BETWEEN 250 AND 2500 then t1.value - t2.value*1000 \n"
						+ "when t1.value/(t2.value*t1.p2/t2.p2) BETWEEN 250000 AND 2500000 then t1.value - t2.value*1000000 \n"
						+ "else t1.value-t2.value end val,t1.`dec`,\n"
						+ "case when t1.accno=t2.accno and t1.tno=t2.tno then 0+t1.calc+t2.calc when \n"
						+ "t1.rnId=t2.rnId then 1+t1.calc+t2.calc else 2 end 'calc'\n"
						+ ",greatest(datediff(greatest(t1.filedate,t2.filedate),least(t1.filedate,t2.filedate)),t1.dif,t2.dif) dif,t1.rnId  \n"
						+ "/*,concat('file:///c://backtest/tableparser/',year(t1.filedate),'/qtr',quarter(t1.filedate),'/tables/',t1.accno,'_',t1.tno,'.txt') l1\n"
						+ ",concat('file:///c://backtest/tableparser/',year(t2.filedate),'/qtr',quarter(t2.filedate),'/tables/',t2.accno,'_',t2.tno,'.txt') l2\n"
						+ ",concat('select * from bac_tp_raw',year(t1.filedate),'qtr',quarter(t1.filedate),' where accno=\'',t1.accno,'\' and tno=',t1.tno,' and value=',t1.value,';') s1\n"
						+ ",concat('select * from bac_tp_raw',year(t2.filedate),'qtr',quarter(t2.filedate),' where accno=\'',t2.accno,'\' and tno=',t2.tno,' and value=',t2.value,';') s2*/\n"
						+ "from tmp_tp_sales t1 inner join tmp_tp_sales t2\n"
						+ "on t1.accno=t2.accno /*and (  t1.edtId=t2.edtId or t1.edtId=t2.edtId+(t1.p2-t2.p2) )*/\n"
						+ "where ((t1.edt2=t2.edt2 or datediff(greatest(t1.edt2,t2.edt2),least(t1.edt2,t2.edt2) between 0 and 43) ) or (datediff(t1.edt2,t2.edt2) between ((t1.p2-t2.p2)/12)*365-43 and ((t1.p2-t2.p2)/12)*365+43))\n"
						+ "AND t1.p2>t2.p2 and datediff(greatest(t1.filedate,t2.filedate),least(t1.filedate,t2.filedate))<850 \n"
						+ "/*and left(t1.rowname,5)=left(t2.rowname,5)*/\n"
						+ ";\n"
						+ "\n"
						+ "end;\n"
						+ "\n"
						+ "\n"
						+ "DROP PROCEDURE IF EXISTS insertIntoTMP_TP_Sales_subtractSameRn;\n"
						+ "CREATE PROCEDURE insertIntoTMP_TP_Sales_subtractSameRn()\n"
						+ "\n"
						+ "BEGIN\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "INSERT IGNORE INTO TMP_TP_SALES\n"
						+ "SELECT \n"
						+ "\n"
						+ "/*round((t1.value/(t2.value*t1.p2/t2.p2)),1) Vr,round((T2.VALUE/(T1.VALUE*T1.P2/T2.P2)),1) Vr2,\n"
						+ "t1.`dec` t1d,t2.`dec` t2d,t1.edt2 t1edt2,t2.edt2 t2edt2,t1.p2 t1p2,t2.p2 t2p2,t1.value t1v,t2.value t2v,left(t2.rowname,5) t1rn5,*/\n"
						+ "\n"
						+ "t1.accno,greatest(t1.filedate,t2.filedate) filedate,t1.cik,t1.tno,t1.trow,t1.row,t1.rowname,t1.origrowname,\n"
						+ "case when t1.edt2=t2.edt2 then cast(date_sub(t1.edt2,interval t2.p2 month) as binary) else t1.edt2 end edt2\n"
						+ ",case when t1.edtId=t2.edtId then t1.edtId-t2.p2 else t1.edtId end edtId\n"
						+ ",t1.p2-t2.p2 p2\n"
						+ ",case \n"
						+ "when t1.value/(t2.value*t1.p2/t2.p2) BETWEEN 1/120     AND 1/80 then t1.value*100 - t2.value \n"
						+ "when t1.value/(t2.value*t1.p2/t2.p2) BETWEEN 1/2500    AND 1/250 then t1.value*1000 - t2.value \n"
						+ "when t1.value/(t2.value*t1.p2/t2.p2) BETWEEN 1/2500000 AND 1/250000 then t1.value*1000000 - t2.value \n"
						+ "when t1.value/(t2.value*t1.p2/t2.p2) BETWEEN 80 AND 120 then t1.value - t2.value*100 \n"
						+ "when t1.value/(t2.value*t1.p2/t2.p2) BETWEEN 250 AND 2500 then t1.value - t2.value*1000 \n"
						+ "when t1.value/(t2.value*t1.p2/t2.p2) BETWEEN 250000 AND 2500000 then t1.value - t2.value*1000000 \n"
						+ "else t1.value-t2.value end val,t1.`dec`,\n"
						+ "case when t1.accno=t2.accno and t1.tno=t2.tno then 0+t1.calc+t2.calc when \n"
						+ "( t1.rnId=t2.rnId or (LEFT(TRIM(t1.rowname),4) rlike 'sale|reve|[ ]{0,2}' and LEFT(TRIM(t2.rowname),4) rlike 'sale|reve|[ ]{0,2}') ) then 1+t1.calc+t2.calc else 2 end 'calc'\n"
						+ ",greatest(datediff(greatest(t1.filedate,t2.filedate),least(t1.filedate,t2.filedate)),t1.dif,t2.dif) dif,t1.rnId \n"
						+ "\n"

						+ "/*,concat('file:///c://backtest/tableparser/',year(t1.filedate),'/qtr',quarter(t1.filedate),'/tables/',t1.accno,'_',t1.tno,'.txt') l1\n"
						+ ",concat('file:///c://backtest/tableparser/',year(t2.filedate),'/qtr',quarter(t2.filedate),'/tables/',t2.accno,'_',t2.tno,'.txt') l2\n"
						+ ",concat('select * from bac_tp_raw',year(t1.filedate),'qtr',quarter(t1.filedate),' where accno=\'',t1.accno,'\' and tno=',t1.tno,' and value=',t1.value,';') s1\n"
						+ ",concat('select * from bac_tp_raw',year(t2.filedate),'qtr',quarter(t2.filedate),' where accno=\'',t2.accno,'\' and tno=',t2.tno,' and value=',t2.value,';') s2*/\n"
						+ "from tmp_tp_sales t1 inner join tmp_tp_sales t2\n"
						+ "on t1.cik=t2.cik /*and t1.rnId=t2.rnId and (  t1.edtId=t2.edtId or t1.edtId=t2.edtId+(t1.p2-t2.p2) )*/\n"
						+ "where ((t1.edt2=t2.edt2 or datediff(greatest(t1.edt2,t2.edt2),least(t1.edt2,t2.edt2) between 0 and 43)) or (datediff(t1.edt2,t2.edt2) between ((t1.p2-t2.p2)/12)*365-43 and ((t1.p2-t2.p2)/12)*365+43))\n"
						+ "AND t1.p2>t2.p2 \n"
						+ "and  datediff(greatest(t1.filedate,t2.filedate),least(t1.filedate,t2.filedate))<850 \n"
						+ "and ( t1.rnId=t2.rnId or (LEFT(TRIM(t1.rowname),4) rlike 'sale|reve|[ ]{0,2}' and LEFT(TRIM(t2.rowname),4) rlike 'sale|reve|[ ]{0,2}') )\n"
						+ ";\n"
						+ "\n"
						+ "end;\n"
						+ "\n"
						+ "\n"
						+ "DELETE FROM TP_SALES_TO_SCRUB WHERE ROWNAME RLIKE 'BEGINNING OF PERIOD|END OF PERIOD';\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMP_SCRUB_SALES_A;\n"
						+ "CREATE TABLE TMP_SCRUB_SALES_A ENGINE=MYISAM\n"
						+ "\n"
						+ "\n"
						+ "select accno,left(filedate,10) filedate ,cik,tno,row,trim(replace(\n"
						+ "replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(upper(trim(ROWNAME)),'TOTAL',''),'NET',''),'GROSS',''),'\\(',''),'\\)',''),'MILLIONS',''),'OF DOLLARS',''),'EXCEPT ',''),'PER SHARE ',''),'DATA ',''),'THOUSANDS',''),'EPS',''),'OPERATING','')\n"
						+ ") rowname, rowname origRowname\n"
						+ ",value,p2 ,edt2 \n"
						+ ",CASE WHEN `DEC`=0 OR `DEC` IS NULL OR `DEC`='' THEN 1 WHEN `DEC`=-3 THEN 1000 WHEN `DEC` = -6 THEN 1000000 \n"
						+ "WHEN `DEC`=-9 THEN 1000000000 ELSE `DEC` END `DEC`,trow\n"
						+ ",@mo:=case when edt2 rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})' then right(left(edt2,7),2) else '' end mo\n"
						+ ",@dy:=case when edt2 rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})' then right(edt2,2) else '' end dy\n"
						+ ",@fDay:=case \n"
						+ "when @mo>0 and @mo!=2 and (@dy>=15 or right(edt2,2)='00') and @dy!='' and @mo!='' then 30\n"
						+ "when @mo=2 and (@dy>=15 or right(edt2,2)='00') and @dy!='' and @mo!='' then 28\n"
						+ "when @mo!=3 and @dy<15 and @dy!='' and @mo!='' then 30 \n"
						+ "when @mo=3 and @dy<15 and @dy!='' and @mo!='' then 28 else '' end fDay\n"
						+ ",@fMo:=case \n"
						+ "when @dy>=15 or right(edt2,2)='00' or @dy='' then @mo\n"
						+ "when @dy<15 and @mo>1 then @mo-1\n"
						+ "when @dy<15 and @mo<2 then 12 else @mo end fMo\n"
						+ ",@fnlMo:=case when length(@fMo)=1 then concat('0',@fMo) when @fMo between 1 and 12 then @fMo else @mo end fnlMo\n"
						+ ",@fnlDay:=case when length(@fDay)=1 then concat('0',@fDay) else @fDay end fnlDay\n"
						+ ",LEFT(concat(@fnlMo,'-',@fnlDay),5) q_end\n"
						+ ",@yr:=case when @mo<2 and @dy<15 and @mo!='' and @dy!='' then left(edt2,4)-1 else left(edt2,4) end yr\n"
						+ ",LEFT(CONCAT(@yr,'-',LEFT(concat(@fnlMo,'-',@fnlDay),5)),10) enddate\n"
						+ "FROM TP_SALES_TO_SCRUB  where p2 between 3 and 12 and value!=100;\n"
						+ "\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "ALTER TABLE TMP_SCRUB_SALES_A DROP COLUMN EDT2, DROP COLUMN FMO, DROP COLUMN MO,DROP COLUMN DY\n"
						+ ",DROP COLUMN FDAY,DROP COLUMN FNLMO,DROP COLUMN FNLDAY,DROP COLUMN Q_END,DROP COLUMN YR\n"
						+ ",CHANGE ENDDATE EDT2 VARCHAR(12), ADD KEY(ROWNAME), CHANGE FILEDATE FILEDATE VARCHAR(10); \n"
						+ "\n"
						+ "/*@gp sets differential between two different rownames with same edt2 and p2 where if it their values are less than this \n"
						+ "differential the two rownames are set to equal each other. This will benefit when I do y-o-y comparisons.*/\n"
						+ "SET @GP=0.05; set @rn='1x'; set @rw=0; set @p2=0; set @edt2='1901-01-01'; set @v=0;\n"
						+ "DROP TABLE IF EXISTS TMP_HOMOG_ODDROWNAME;\n"
						+ "CREATE TABLE TMP_HOMOG_ODDROWNAME ENGINE=MYISAM\n"
						+ "select @rw:=@rw+1 rw,origRowname,\n"
						+ "@rn2:=case when left(rowname,4) rlike 'sale|reve' or trim(rowname) ='' then 'REVEN' else left(rowname,5) end rn5\n"
						+ "/*set to same rn5 so they have the same rnId later - and as a result treated as same rowname*/\n"
						+ ",case\n"
						+ "when left(@rn,5)!=left(@rn2,5) and @edt2=edt2 and @p2=p2 and ABS(@v-VALUE)/value<@GP then 1 \n"
						+ "/*greater @gp value means more odd ball rownames will be merged*/\n"
						+ "when left(@rn,5)!=left(@rn2,5) and @edt2=edt2 and @p2=p2 AND ABS(@v-VALUE)/value>=@GP then 2 else 0 end Homog, \n"
						+ "case\n"
						+ "when left(@rn,5)!=left(@rn2,5) and @edt2=edt2 and @p2=p2 AND ABS(@v-VALUE)/value<@GP then 1 \n"
						+ "when left(@rn,5)!=left(@rn2,5) and @edt2=edt2 and @p2=p2 and @edt2=edt2 and @p2=p2 then round((abs(@v-value)/value),3)*100 else 0 end dfr, \n"
						+ "accno,filedate,cik,tno,row,@rn:=case when left(rowname,4) rlike 'sale|reve' or trim(rowname) ='' then 'REVEN' else left(rowname,5) end rnTMP\n"
						+ ", rowname\n"
						+ ",@edt2:=edt2 edt2,@p2:=p2 p2,@v:=value value,`dec`,TROW/*,TN,COL*/\n"
						+ "from TMP_SCRUB_SALES_A t1 ORDER BY CIK,edt2,p2,rowname,VALUE ;\n"
						+ "ALTER TABLE TMP_HOMOG_ODDROWNAME ADD KEY(CIK), ADD KEY(RN5), ADD KEY(RW), ADD KEY(HOMOG);\n"
						+ "\n"
						+ "/*if homog=1 - the rowname at 1 and prior can be homogenized depending on which is the odd rowname (least number of instances of that rowname \n"
						+ "for that cik*/\n"
						+ "set @cik=0;\n"
						+ "DROP TABLE IF EXISTS TMP2_HOMOG_ODDROWNAME;\n"
						+ "CREATE TABLE TMP2_HOMOG_ODDROWNAME ENGINE=MYISAM\n"
						+ "select \n"
						+ "case when @cik!=cik then 1 else 0 end gIt,cnt,@cik:=cik cik,RN5 from ( \n"
						+ "select count(*) cnt,cik cik,case when left(rowname,4) rlike 'sale|reve' or trim(rowname) ='' then 'REVEN' else left(rowname,5) end rn5 from TMP_SCRUB_SALES_A group by cik,rn5 order by cik, cnt desc\n"
						+ ") t1 order by cik,cnt desc;\n"
						+ "ALTER TABLE TMP2_HOMOG_ODDROWNAME ADD KEY(CIK), ADD KEY(RN5);\n"
						+ "\n"
						+ "/*these are all the potential rows that can be homogenized -the row where homog=1 can be homogenized or the prior row depending on which is \n"
						+ "the odd rowname. Once these two rows are captured I join with tmp2 table to find one with higher count and lower count. Lower count is \n"
						+ "odd and gets homog. I homog by updating rowname*/\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMP3_HOMOG_ODDROWNAME;\n"
						+ "CREATE TABLE TMP3_HOMOG_ODDROWNAME ENGINE=MYISAM\n"
						+ "SELECT T1.*,CNT FROM TMP_HOMOG_ODDROWNAME T1 INNER JOIN TMP2_HOMOG_ODDROWNAME T2\n"
						+ "ON T1.CIK=T2.CIK AND T1.RN5=T2.RN5;\n"
						+ "ALTER TABLE TMP3_HOMOG_ODDROWNAME ADD KEY(RW),ADD KEY(CIK), ADD KEY(RN5),ADD KEY(HOMOG);\n"
						+ "\n"
						+ "DROP TABLE IF EXISTS TMP4_HOMOG_ODDROWNAME;\n"
						+ "CREATE TABLE TMP4_HOMOG_ODDROWNAME ENGINE=MYISAM\n"
						+ "SELECT T1.CIK,T1.RN5,T1.Rowname,T2.RN5 RN5_2\n"
						+ "/*,T1.CNT\n"
						+ ",T2.CNT \n"
						+ ",T1.HOMOG*/\n"
						+ "FROM TMP3_HOMOG_ODDROWNAME T1 \n"
						+ "INNER JOIN TMP3_HOMOG_ODDROWNAME T2\n"
						+ "ON T1.RW=T2.RW+1\n"
						+ "WHERE T1.HOMOg=1;\n"
						+ "ALTER TABLE TMP4_HOMOG_ODDROWNAME ADD KEY(CIK),ADD KEY(RN5_2);\n"
						+ "\n"
						+ "UPDATE TMP_HOMOG_ODDROWNAME T1 INNER JOIN TMP4_HOMOG_ODDROWNAME T2\n"
						+ "ON T1.CIK=T2.CIK AND T1.RN5=T2.RN5_2\n"
						+ "SET T1.RN5=T2.RN5, T1.ROWNAME=T2.ROWNAME;\n"
						+ "\n"
						+ "ALTER TABLE TMP_HOMOG_ODDROWNAME DROP COLUMN rw,DROP COLUMN homog,DROP COLUMN DFR, ADD KEY(CIK), ADD KEY(rowname);\n"
						+ "\n"
						+ "/*assign same rnId if sale|reve|[ ]{0,2}|total|gross|net - although total|gross|net shoulnd't be present at all.*/\n"
						+ "SET @r=0; SET @rn5='xzxz';\n"
						+ "DROP TABLE IF EXISTS TMP_SCRUB_SALES;\n"
						+ "CREATE TABLE TMP_SCRUB_SALES ENGINE=MYISAM\n"
						+ " SELECT @r:=case when left(@rn5,5)=RN5 then @r else @r+1 end rnId\n"
						+ " ,round(datediff(edt2,'1990-01-01')/30.4375) edtId\n"
						+ " ,ACCNO,FILEDATE,CIK,TNO,ROW,@rn5:=rn5 rn5, rowname,origRowname,VALUE,P2,`DEC`,EDT2,trow FROM TMP_HOMOG_ODDROWNAME ORDER BY rn5;\n"
						+ " \n"
						+ "\n"
						+ "/*first gather all p3,p6,p9,p12 values. Primary key for tmp_tp_sales is cik,edt2,p2,rnId,value - helps ensure most cur rep data only inserted*/\n"
						+ "call tmp_tp_sales();\n"
						+ "\n"
						+ "/*need to homog values by assignmnet of vId in order to filter out duplicates later*/\n"
						+ "set @cik=0; set @edtId:=0; set @p2=0; set @value=0; set @vId=0; \n"
						+ "DROP TABLE IF EXISTS TMPA_TP_SALES_ALL;\n"
						+ "CREATE TABLE TMPA_TP_SALES_ALL ENGINE=MYISAM\n"
						+ "SELECT @val:=case \n"
						+ "when value/(@value*p2/@p2) BETWEEN 1/120     AND 1/80 then value*100 \n"
						+ "when value/(@value*p2/@p2) BETWEEN 1/2500    AND 1/250 then value*1000\n"
						+ "when value/(@value*p2/@p2) BETWEEN 1/2500000 AND 1/250000 then value*1000000\n"
						+ "when value/(@value*p2/@p2) BETWEEN 80 AND 120 then value/100 \n"
						+ "when value/(@value*p2/@p2) BETWEEN 250 AND 2500 then value/1000 \n"
						+ "when value/(@value*p2/@p2) BETWEEN 250000 AND 2500000 then value/1000000 \n"
						+ "else value end val,\n"
						+ "@vId:=case when @cik=cik and @edtId=edtId and @p2=p2 and abs(@value-@val)/@val<.0002 then @vId else @vId+1 end vId,\n"
						+ "/*filter out very close vol val - within one tenth of one percent*/\n"
						+ "accno, filedate, @cik:=cik cik, tno, trow,row, rowname, origRowname,edt2,@edtId:=edtId edtId, @p2:=p2 p2, @value:=value value, `dec`, rnId\n"
						+ "FROM TMP_SCRUB_SALES t1 WHERE VALUE>0 ORDER BY CIK,value,edtId,p2,filedate;\n"
						+ "ALTER TABLE TMPA_TP_SALES_ALL add key(CIK,edtId,P2,rnId,vId);\n"
						+ "/*1st pass it is tmp_scrub_sales -- next is tmp_tp_sales*/\n"
						+ "\n"
						+ "set @cik=0; set @edtId:=0 and @p2=0; set @vId=0; set @rnId=0;\n"
						+ "DROP TABLE IF EXISTS TMP_TP_SALES_ALL;\n"
						+ "CREATE TABLE TMP_TP_SALES_ALL ENGINE=MYISAM\n"
						+ "SELECT \n"
						+ "case when @cik=cik and @edtId=edtId and @p2=p2 and @vId=vId AND @rnId=rnId then 0 else 1 end getIt,\n"
						+ "accno, filedate, @cik:=cik cik, tno,trow, row, rowname, origRowname,edt2,@edtId:=edtId edtId, @p2:=p2 p2, value, `dec`\n"
						+ ",@vId:=vId vId, @rnId:=rnId rnId\n"
						+ "FROM TMPA_TP_SALES_ALL t1 WHERE VALUE>0 ORDER BY CIK,edtId desc,P2,rnId,vId,filedate;\n"
						+ "ALTER TABLE TMP_TP_SALES_ALL ADD KEY(GETIT);\n"
						+ "\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "INSERT IGNORE INTO TMP_tp_sales\n"
						+ "SELECT accno,filedate,cik,tno,trow,row,rowname,origRowname,edt2,edtId,p2,value,`dec`,0,0,rnId FROM TMP_TP_SALES_ALL T1 where getIt=1;\n"
						+ "\n"
						+ "call insertIntoTMP_TP_Sales_subtractSameAcc();\n"
						+ "\n"
						+ "/*need to homog values by assignmnet of vId in order to filter out duplicates later*/\n"
						+ "set @cik=0; set @edtId:=0; set @p2=0; set @value=0; set @vId=0; \n"
						+ "DROP TABLE IF EXISTS TMPA_TP_SALES_ALL;\n"
						+ "CREATE TABLE TMPA_TP_SALES_ALL ENGINE=MYISAM\n"
						+ "SELECT @val:=case \n"
						+ "when value/(@value*p2/@p2) BETWEEN 1/120     AND 1/80 then value*100 \n"
						+ "when value/(@value*p2/@p2) BETWEEN 1/2500    AND 1/250 then value*1000\n"
						+ "when value/(@value*p2/@p2) BETWEEN 1/2500000 AND 1/250000 then value*1000000\n"
						+ "when value/(@value*p2/@p2) BETWEEN 80 AND 120 then value/100 \n"
						+ "when value/(@value*p2/@p2) BETWEEN 250 AND 2500 then value/1000 \n"
						+ "when value/(@value*p2/@p2) BETWEEN 250000 AND 2500000 then value/1000000 \n"
						+ "else value end val,\n"
						+ "@vId:=case when @cik=cik and @edtId=edtId and @p2=p2 and abs(@value-@val)/@val<.0002 then @vId else @vId+1 end vId,\n"
						+ "/*filter out very close vol val - within one tenth of one percent*/\n"
						+ "accno, filedate, @cik:=cik cik, tno, trow,row, rowname, origRowname,edt2,@edtId:=edtId edtId, @p2:=p2 p2, @value:=value value, `dec`, rnId,calc,dif\n"
						+ "FROM tmp_tp_sales t1 WHERE VALUE>0 ORDER BY CIK,value,edtId,p2,filedate;\n"
						+ "ALTER TABLE TMPA_TP_SALES_ALL add key(CIK,edtId,P2,rnId,vId);\n"
						+ "\n"
						+ "set @cik=0; set @edtId:=0; set @p2=0; set @vId=0; set @rnId=0;\n"
						+ "DROP TABLE IF EXISTS TMP_TP_SALES_ALL;\n"
						+ "CREATE TABLE TMP_TP_SALES_ALL ENGINE=MYISAM\n"
						+ "SELECT \n"
						+ "case when @cik=cik and @edtId=edtId and @p2=p2 and @vId=vId AND @rnId=rnId then 0 else 1 end getIt,\n"
						+ "accno, filedate, @cik:=cik cik, tno, trow,row, rowname,origRowname,edt2,@edtId:=edtId edtId, @p2:=p2 p2, value, `dec`\n"
						+ ",@vId:=vId vId, calc,dif,@rnId:=rnId rnId\n"
						+ "FROM TMPA_TP_SALES_ALL t1 WHERE VALUE>0 ORDER BY CIK,edtId desc,P2,rnId,vId,filedate;\n"
						+ "ALTER TABLE TMP_TP_SALES_ALL ADD KEY(GETIT);\n"
						+ "\n"
						+ "CALL tmp_tp_sales();\n"
						+ "\n"
						+ "INSERT IGNORE INTO TMP_TP_SALES\n"
						+ "SELECT accno,filedate,cik,tno,trow,row,rowname,origRowname,edt2,edtId,p2,value,`dec`,CALC,DIF,rnId FROM TMP_TP_SALES_ALL\n"
						+ "WHERE GETIT=1;\n"
						+ "/*select 'loop 1',t1.* from  TMP_TP_SALES t1; */\n"
						+ "\n"
						+ "/*2: rerun with new numbers - 2nd loop I limit it to to same accno. Later to ge just missing periods - run again??*/\n"
						+ "call insertIntoTMP_TP_Sales_subtractSameRn();\n"
						+ "/*need to homog values by assignmnet of vId in order to filter out duplicates later*/\n"
						+ "set @cik=0; set @edtId:=0; set @p2=0; set @value=0; set @vId=0; \n"
						+ "DROP TABLE IF EXISTS TMPA_TP_SALES_ALL;\n"
						+ "CREATE TABLE TMPA_TP_SALES_ALL ENGINE=MYISAM\n"
						+ "SELECT @val:=case \n"
						+ "when value/(@value*p2/@p2) BETWEEN 1/120     AND 1/80 then value*100 \n"
						+ "when value/(@value*p2/@p2) BETWEEN 1/2500    AND 1/250 then value*1000\n"
						+ "when value/(@value*p2/@p2) BETWEEN 1/2500000 AND 1/250000 then value*1000000\n"
						+ "when value/(@value*p2/@p2) BETWEEN 80 AND 120 then value/100 \n"
						+ "when value/(@value*p2/@p2) BETWEEN 250 AND 2500 then value/1000 \n"
						+ "when value/(@value*p2/@p2) BETWEEN 250000 AND 2500000 then value/1000000 \n"
						+ "else value end val,\n"
						+ "@vId:=case when @cik=cik and @edtId=edtId and @p2=p2 and abs(@value-@val)/@val<.0002 then @vId else @vId+1 end vId,\n"
						+ "/*filter out very close vol val - within one tenth of one percent*/\n"
						+ "accno, filedate, @cik:=cik cik, tno,trow, row, rowname, origRowname,edt2,@edtId:=edtId edtId, @p2:=p2 p2, @value:=value value, `dec`, rnId,calc,dif\n"
						+ "FROM tmp_tp_sales t1 WHERE VALUE>0 ORDER BY CIK,value,edtId,p2,filedate;\n"
						+ "ALTER TABLE TMPA_TP_SALES_ALL add key(CIK,edtId,P2,rnId,vId);\n"
						+ "\n"
						+ "set @cik=0; set @edtId:=0; set @p2=0; set @vId=0; set @rnId=0;\n"
						+ "DROP TABLE IF EXISTS TMP_TP_SALES_ALL;\n"
						+ "CREATE TABLE TMP_TP_SALES_ALL ENGINE=MYISAM\n"
						+ "SELECT \n"
						+ "case when @cik=cik and @edtId=edtId and @p2=p2 and @vId=vId AND @rnId=rnId then 0 else 1 end getIt,\n"
						+ "accno, filedate, @cik:=cik cik, tno,trow, row, rowname,origRowname,edt2,@edtId:=edtId edtId, @p2:=p2 p2, value, `dec`\n"
						+ ",@vId:=vId vId, @rnId:=rnId rnId,calc,dif\n"
						+ "FROM TMPA_TP_SALES_ALL t1 WHERE VALUE>0 ORDER BY CIK,edtId desc,P2,rnId,vId,filedate;\n"
						+ "ALTER TABLE TMP_TP_SALES_ALL ADD KEY(GETIT);\n"
						+ "\n"
						+ "CALL tmp_tp_sales();\n"
						+ "\n"
						+ "INSERT IGNORE INTO TMP_TP_SALES\n"
						+ "SELECT accno,filedate,cik,tno,trow,row,rowname,origRowname,edt2,edtId,p2,value,`dec`,CALC,DIF,rnId FROM TMP_TP_SALES_ALL\n"
						+ "WHERE GETIT=1;\n");

		String generateTmp_tp_sales = sb.toString();
		MysqlConnUtils.executeQuery(generateTmp_tp_sales);
		sb.delete(0, sb.toString().length());
		// xxxx --- I uncommneted below.

		int qtr = startQ, q = startQ;
		int yr = startYr;

		int cnt = 0;
		for (yr = startYr; yr <= endYr; yr++) {
			if (yr == 1993 && qtr < 3) {
				qtr = 3;
			}
			for (q = qtr; q <= endQ; q++) {
				if (cnt == 0) {
					sb.append("/*Last attempt to fix edt2/p2. This matches all rows in bac_tp_rawYYYYQtr tables with cik and either\n"
							+ "rowname or value where edt2/p2 needs to be repaired or YR='BAD' with tmp_tp_sales cik and rowname or value. \n"
							+ "Below gets unique rownames for each CIK so I can join with each YYYYQtr table based on cik and rowname.\n"
							+ "Run once-where cnt=0*/\n"
							+ "\n"
							+ "set sql_mode = ALLOW_INVALID_DATES;\n"
							+ "DROP TABLE IF EXISTS TMP_tp_sales_OrigRowname_cik;\n"
							+ "CREATE TABLE TMP_tp_sales_OrigRowname_cik ENGINE=MYISAM\n"
							+ "select origRowname,cik from tmp_tp_sales t1 group by cik,origRowname;\n"
							+ "ALTER TABLE TMP_tp_sales_OrigRowname_cik ADD KEY(CIK),ADD KEY(origROWNAME);\n");
				}

				sb.append("\n" + "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "DROP TABLE IF EXISTS TMP_REPAIR_SAMERN_"
						+ yr
						+ "QTR"
						+ q
						+ ";\n"
						+ "CREATE TABLE TMP_REPAIR_SAMERN_"
						+ yr
						+ "QTR"
						+ q
						+ " ENGINE=MYISAM\n"
						+ "select 1 getIt,t1.accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value,T1.P1, t1.p2,T1.EDT1\n"
						+ ",case when length(t1.edt2)=10 then round(datediff(t1.edt2,'1990-01-01')/30.4375) else -1 end edtId, t1.edt2,t1.yr, t1.`DEC`\n"
						+ ", t1.columnText, t1.form from bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ " t1 inner join TMP_tp_sales_OrigRowname_cik  t2\n"
						+ "on t1.cik=t2.cik\n"
						+ "where \n"
						+ "left(t1.rowname,20)=left(t2.origrowname,20)\n"
						+ "/*rowname from tmp_tp_sales reflects trim(replace query ... - so only need to conform t1 rowname */\n"
						+ "and length(t1.rowname)>2 and tn='is' \n"
						+ "and (t1.p2=0 or t1.edt2 not rlike "
						+ "'[12]{1}[09]{1}[0-9]{2}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})'\n"
						+ " or yr='bad')\n"
						+ "and t1.value>100 GROUP BY ACCNO,TNO,ROW;\n"
						+ "ALTER TABLE TMP_REPAIR_SAMERN_"
						+ yr
						+ "QTR"
						+ q
						+ " ADD KEY(ACCNO,TNO,ROW), ADD KEY(ACCNO),ADD KEY(TNO),ADD KEY(ROW);\n"
						+ "\n");

				if (cnt == 0) {

					sb.append("/*Run once-where cnt=0 else insert */\n"
							+ "set sql_mode = ALLOW_INVALID_DATES;\n"
							+ "DROP TABLE IF EXISTS TP_REPAIR_FROM_TP_SALES;\n"
							+ "CREATE TABLE TP_REPAIR_FROM_TP_SALES ENGINE=MYISAM\n"
							+ "select * from TMP_REPAIR_SAMERN_"
							+ yr
							+ "QTR"
							+ q
							+ ";\n"
							+ "ALTER TABLE TP_REPAIR_FROM_TP_SALES ADD PRIMARY KEY(ACCNO,TNO,ROW)\n"
							+ ", change tno tno int(10), change row row int(10), change value value double(18,2),change `dec` `dec` int(11),\n"
							+ "add key(value), add key(edtId), add key(cik);\n"
							+ "\n");
				}

				if (cnt != 0) {
					sb.append("set sql_mode = ALLOW_INVALID_DATES;\n"
							+ "\nINSERT IGNORE INTO TP_REPAIR_FROM_TP_SALES\n"
							+ "select * from TMP_REPAIR_SAMERN_" + yr + "QTR"
							+ q + ";\n" + "\n");
				}

				sb.append("/*these are other rows associated with edt2/p2 trow to repair but don't need to be repaired - hence 0. Used to repair where 1.*/\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "INSERT IGNORE INTO TP_REPAIR_FROM_TP_SALES\n"
						+ "select 0,t1.accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value,T1.P1, t1.p2\n"
						+ ",T1.EDT1,case when length(t1.edt2)=10 then round(datediff(t1.edt2,'1990-01-01')/30.4375) else -1 end edtId\n"
						+ ", t1.edt2,t1.yr, t1.`DEC`, t1.columnText, t1.form from bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ " t1 inner join TMP_REPAIR_SAMERN_"
						+ yr
						+ "QTR"
						+ q
						+ " t2\n"
						+ "on T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.TROW=T2.TROW;\n"
						+ "\n"
						+ "/*find incomplete edt2/p2 by matching values from tmp_tp_sales*/\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "INSERT IGNORE INTO TP_REPAIR_FROM_TP_SALES\n"
						+ "select 1, t1.accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value,T1.P1, t1.p2,T1.EDT1\n"
						+ ",case when length(t1.edt2)=10 then round(datediff(t1.edt2,'1990-01-01')/30.4375) else -1 end edtId, t1.edt2,t1.yr, t1.`DEC`\n"
						+ ", t1.columnText, t1.form from bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ " t1 inner join tmp_tp_sales t2\n"
						+ "on t1.cik=t2.cik and t1.value=t2.value\n"
						+ "where (t1.p2=0 or t1.edt2 not rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})'\n"
						+ " OR yr='bad')\n"
						+ " and t1.value>100 and tn='is' ;\n"
						+ "\n"
						+ "/*done once - at end. Have to assign unique values to tno and row so that pkey doesn't include them*/\n"
						+ "set @rw=99999;\n");

				if (cnt == 0) {
					sb.append(""
							+ "set sql_mode = ALLOW_INVALID_DATES;\n"
							+ "INSERT IGNORE INTO TP_REPAIR_FROM_TP_SALES\n"
							+ "select 0, t1.accno, t1.fileDate, t1.cik, t1.tn, t1.trow, row, col, tno, t1.rowname, t1.value,'', t1.p2,'',\n"
							+ "round(datediff(edt2,'1990-01-01')/30.4375) edtId, t1.edt2,'', t1.`DEC`,columnText, form\n"
							+ "from tp_sales_to_scrub t1 ;");

					sb.append(""
							+ "set sql_mode = ALLOW_INVALID_DATES;\n"
							+ "INSERT IGNORE INTO TP_REPAIR_FROM_TP_SALES\n"
							+ "select 0, t1.accno, t1.fileDate, t1.cik, 'is', t1.trow, @rw:=@rw+1, '', @rw rw, t1.origRowname, t1.value,'', t1.p2,'',\n"
							+ "round(datediff(edt2,'1990-01-01')/30.4375) edtId, t1.edt2,'', t1.`DEC`,'', ''\n"
							+ "from tmp_tp_sales t1 ;\n");
				}
				cnt++;

				MysqlConnUtils.executeQuery(sb.toString());
				sb.delete(0, sb.toString().length());
			}
			qtr = 1;
		}

		sb.append(""
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "DROP TABLE IF EXISTS TMP_TP_REPAIR_FROM_TP_SALES;\n"
				+ "/*this preps for enddates - see where clause*/"
				+ "CREATE TABLE TMP_TP_REPAIR_FROM_TP_SALES ENGINE=MYISAM\n"
				+ "select getIt, accno, fileDate, cik, tn, trow, row, col, tno, rowname, value, P1, p2, EDT1, edtId, edt2, yr, `DEC`, columnText, form from TP_REPAIR_FROM_TP_SALES \n"
				+ "where edt2 not rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})'\n"
				+ "GROUP BY CIK ;\n"
				+ "ALTER TABLE TMP_TP_REPAIR_FROM_TP_SALES ADD KEY(CIK);\n"
				+ "\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "DROP TABLE IF EXISTS TMP2_TP_REPAIR_FROM_TP_SALES;\n"
				+ "SET @rw=0;"
				+ "CREATE TABLE TMP2_TP_REPAIR_FROM_TP_SALES ENGINE=MYISAM\n"
				+ "select @rw:=@rw+1 rw,left(T1.value,5) v5,t1.* from TP_REPAIR_FROM_TP_SALES t1 \n"
				+ "inner join TMP_TP_REPAIR_FROM_TP_SALES t2\n"
				+ "on t1.cik=t2.cik;\n"
				+ "ALTER TABLE TMP2_TP_REPAIR_FROM_TP_SALES ADD KEY(CIK), ADD KEY(RW),ADD KEY(V5),\n"
				+ "ADD KEY (EDTID),ADD KEY(ROW),ADD KEY(YR);\n");

		sb.append(""
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "set @edtId=0; set @value=0; set @cik=0;\n"
				+ "DROP TABLE IF EXISTS TMP_TP_EDT_P_UPDATE;\n"
				+ "CREATE TABLE TMP_TP_EDT_P_UPDATE ENGINE=MYISAM\n"
				+ "select t1.accno,t1.filedate,t1.tn,t1.trow,t1.row,t1.col,t1.tno,t1.rowname,t1.`dec`,t1.columntext,t1.form,\n"
				+ "@edt3:=case when t1.edt2 rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})' and t1.yr!='bad' \n"
				+ "and left(t1.edt2,7)!=left(t2.edt2,7) and datediff(greatest(t2.edt2,concat(left(t1.edt2,7),'-30')),least(t2.edt2,concat(left(t1.edt2,7),'-30')))>45\n"
				+ "then 'xx'\n"
				+ "when\n"
				+ "( \n"
				+ "greatest(left(t2.edt2,4),left(t1.edt2,4)) - least(left(t2.edt2,4),left(t1.edt2,4)) < 3 and t1.edt2 rlike '[12]{1}[09]{1}[0-9]{2}' \n"
				+ "\n"
				+ "or greatest(left(t2.edt2,4),left(t1.edt1,4)) - least(left(t2.edt2,4),left(t1.edt1,4)) < 3 and t1.edt1 rlike '[12]{1}[09]{1}[0-9]{2}' \n"
				+ "\n"
				+ "or (t1.edt2 not rlike '[12]{1}[09]{1}[0-9]{2}' and t1.edt1 not rlike '[12]{1}[09]{1}[0-9]{2}' )\n"
				+ ") \n"
				+ "and (left(trim(t1.rowname),8)= left(trim(t2.rowname),8) or \n"
				+ "(t1.rowname rlike 'revenue|sale|interest|income|royalt' and t2.rowname rlike 'revenue|sale|interest|income|royalt')\n"
				+ "or (t1.rowname rlike 'revenue|sale|interest|income|royalt' and length(t2.rowname)<2)\n"
				+ "or (t2.rowname rlike 'revenue|sale|interest|income|royalt' and length(t1.rowname)<2)\n"
				+ ")\n"
				+ "and (abs(t1.value-t2.value)/t2.value<.001 or greatest(t1.value,t2.value)/least(t1.value,t2.value) between 997 and 1003 \n"
				+ "or greatest(t1.value,t2.value)/least(t1.value,t2.value) between 999900 and 1000100 )\n"
				+ "/*compares yrs of edt1/edt2 of t1/t2 - must be w/n 2 yrs and values must be very close or thous or mil multp*/\n"
				+ "then t2.edt2\n"
				+ "when (abs(t1.value-t2.value)/t2.value<.001 or greatest(t1.value,t2.value)/least(t1.value,t2.value) between 997 and 1003 \n"
				+ "or greatest(t1.value,t2.value)/least(t1.value,t2.value) between 999900 and 1000100 )\n"
				+ "and ( greatest(left(t2.edt2,4),left(t1.edt2,4)) - least(left(t2.edt2,4),left(t1.edt2,4)) < 3 and t1.edt2 rlike '[12]{1}[09]{1}[0-9]{2}' \n"
				+ "or greatest(left(t2.edt2,4),left(t1.edt1,4)) - least(left(t2.edt2,4),left(t1.edt1,4)) < 3 and t1.edt1 rlike '[12]{1}[09]{1}[0-9]{2}' \n"
				+ "or (t1.edt2 not rlike '[12]{1}[09]{1}[0-9]{2}' and t1.edt1 not rlike '[12]{1}[09]{1}[0-9]{2}' )\n"
				+ ") and t1.value>9999\n"
				+ "/*no t1.edt1/edt2 yr or rowname compares but values to compare >9999*/\n"
				+ "then t2.edt2 /*put in: t2.edt2*/\n"
				+ "else 'xx' end edt3,\n"
				+ "case when @cik=t1.cik and @value=t1.value and greatest(t2.edtId,@edtId)-least(t2.edtId,@edtId)<1 then round(greatest(t2.edtId,@edtId)) else t2.edtId end edtId3,\n"
				+ "@edtId:=t2.edtId edtId,\n"
				+ "@p3:=case when t2.p2 between 3 and 12 then t2.p2 when t1.p1=t2.p1 and t1.p1 between 3 and 12 then t2.p1 when t1.yr!='bad' and t1.p2 between 3 and 12 then t1.p2 else 0 end p3,\n"
				+ "case when @p3 between 3 and 12 and @edt3!='xx' then 'gd' else '' end gdYr,\n"
				+ "@cik:=t1.cik cik,@value:=t1.value value\n"
				+ "/*,case when t1.value!=t2.value and length(t1.value)>5 and length(t2.value)>5 then 1 else 0 end ckV,t2.value,t1.edt1,t1.edt2,t2.edt1,t2.edt2,t1.p2,t2.p2,t1.p1,t2.p1,left(t1.filedate,10) fd1,left(t2.filedate,10) fd2\n"
				+ ",t1.rowname,t2.rowname,t1.yr,t2.yr,\n"
				+ "t1.*\n"
				+ ",concat('file:///c://backtest/tableparser/',year(t1.filedate),'/qtr',quarter(t1.filedate),'/tables/',t1.accno,'_',t1.tno,'.htm') l1 \n"
				+ ",concat('file:///c://backtest/tableparser/',year(t2.filedate),'/qtr',quarter(t2.filedate),'/tables/',t2.accno,'_',t2.tno,'.htm') l2 */\n"
				+ "from TMP2_TP_REPAIR_FROM_TP_SALES t1 inner join TMP2_TP_REPAIR_FROM_TP_SALES t2\n"
				+ "on t1.cik=t2.cik and t1.v5=t2.v5\n"
				+ "where /*fix t1 row - original rows have row number less than 100000.*/\n"
				+ "(t1.edtID<0 or T1.edtId is null) and t1.row<100000 and t2.row<100000\n"
				+ "/*when t1.yr='bad' but t1.edt2 valid that edt2 is stil highly likely correct*/\n"
				+ "and t1.rw!=t2.rw\n"
				+ "and t2.edt2 rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})' and t2.yr!='bad'\n"
				+ "and datediff(greatest(t1.filedate,t2.filedate),least(t1.filedate,t2.filedate))<1200\n"
				+ "and t1.value>100 and t1.rowname not rlike 'expense|cost|administrativ|selling|net incom|retained earn|estate tax'\n"
				+ "and t1.columnText not rlike '^ two month' and t2.columntext not rlike '^ two month'\n"
				+ "order by t1.cik,t1.value;\n"
				+ "alter table TMP_TP_EDT_P_UPDATE add key(cik), add key(value), add key(edtId3);\n"
				+ "\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "DROP TABLE IF EXISTS TMP2_TP_EDT_P_UPDATE;\n"
				+ "CREATE TABLE TMP2_TP_EDT_P_UPDATE ENGINE=MYISAM\n"
				+ "select count(distinct(edtId3)) cntEdtId, t1.* from TMP_TP_EDT_P_UPDATE t1 group by t1.cik,t1.value;\n"
				+ "alter table TMP2_TP_EDT_P_UPDATE add key(cik), add key(value), add key(cntEdtId);\n"
				+ "\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "insert ignore into tp_sales_to_scrub\n"
				+ "select t1.accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value, t1.p3, t1.edt3, t1.`DEC`, t1.columnText, t1.form from TMP_TP_EDT_P_UPDATE t1 inner join TMP2_TP_EDT_P_UPDATE t2 on t1.cik=t2.cik and t1.value=t2.value\n"
				+ "where t2.cntEdtId=1 \n"
				+ "and t1.columnText not rlike '^ two month' and t2.columntext not rlike '^ two month'\n"
				+ "and t1.gdYr='gd' and t1.edt3!='xx';\n"
				+ "\n"
				+ "DROP TABLE IF EXISTS tp2_edt_p;\n"
				+ "CREATE TABLE tp2_edt_p ENGINE=MYISAM\n"
				+ "select t1.accno, t1.fileDate, t1.tno,t1.col,t1.edt3 edt2,t1.p3 p2 from TMP_TP_EDT_P_UPDATE t1 inner join TMP2_TP_EDT_P_UPDATE t2 on t1.cik=t2.cik and t1.value=t2.value\n"
				+ "where t2.cntEdtId=1 \n"
				+ "and t1.columnText not rlike '^ two month' and t2.columntext not rlike '^ two month' GROUP BY T1.ACCNO,T1.TNO,T1.COL;\n"
				+ "\n"
				+ "\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "set @edtId=0; set @value=0; set @cik=0;\n"
				+ "insert ignore into TMP_TP_EDT_P_UPDATE\n"
				+ "select t1.accno,t1.filedate,t1.tn,t1.trow,t1.row,t1.col,t1.tno,t1.rowname,t1.`dec`,t1.columntext,t1.form,\n"
				+ "@edt3:=case when t1.edt2 rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})' and t1.yr!='bad' \n"
				+ "and left(t1.edt2,7)!=left(t2.edt2,7) and datediff(greatest(t2.edt2,concat(left(t1.edt2,7),'-30')),least(t2.edt2,concat(left(t1.edt2,7),'-30')))>45\n"
				+ "then 'xx'\n"
				+ "when\n"
				+ "( \n"
				+ "greatest(left(t2.edt2,4),left(t1.edt2,4)) - least(left(t2.edt2,4),left(t1.edt2,4)) < 3 and t1.edt2 rlike '[12]{1}[09]{1}[0-9]{2}' \n"
				+ "\n"
				+ "or greatest(left(t2.edt2,4),left(t1.edt1,4)) - least(left(t2.edt2,4),left(t1.edt1,4)) < 3 and t1.edt1 rlike '[12]{1}[09]{1}[0-9]{2}' \n"
				+ "\n"
				+ "or (t1.edt2 not rlike '[12]{1}[09]{1}[0-9]{2}' and t1.edt1 not rlike '[12]{1}[09]{1}[0-9]{2}' )\n"
				+ ") \n"
				+ "and (left(trim(t1.rowname),8)= left(trim(t2.rowname),8) or \n"
				+ "(t1.rowname rlike 'revenue|sale|interest|income|royalt' and t2.rowname rlike 'revenue|sale|interest|income|royalt')\n"
				+ "or (t1.rowname rlike 'revenue|sale|interest|income|royalt' and length(t2.rowname)<2)\n"
				+ "or (t2.rowname rlike 'revenue|sale|interest|income|royalt' and length(t1.rowname)<2)\n"
				+ ")\n"
				+ "and (abs(t1.value-t2.value)/t2.value<.001 or greatest(t1.value,t2.value)/least(t1.value,t2.value) between 997 and 1003 \n"
				+ "or greatest(t1.value,t2.value)/least(t1.value,t2.value) between 999900 and 1000100 )\n"
				+ "/*compares yrs of edt1/edt2 of t1/t2 - must be w/n 2 yrs and values must be very close or thous or mil multp*/\n"
				+ "then t2.edt2\n"
				+ "when (abs(t1.value-t2.value)/t2.value<.001 or greatest(t1.value,t2.value)/least(t1.value,t2.value) between 997 and 1003 \n"
				+ "or greatest(t1.value,t2.value)/least(t1.value,t2.value) between 999900 and 1000100 )\n"
				+ "and ( greatest(left(t2.edt2,4),left(t1.edt2,4)) - least(left(t2.edt2,4),left(t1.edt2,4)) < 3 and t1.edt2 rlike '[12]{1}[09]{1}[0-9]{2}' \n"
				+ "or greatest(left(t2.edt2,4),left(t1.edt1,4)) - least(left(t2.edt2,4),left(t1.edt1,4)) < 3 and t1.edt1 rlike '[12]{1}[09]{1}[0-9]{2}' \n"
				+ "or (t1.edt2 not rlike '[12]{1}[09]{1}[0-9]{2}' and t1.edt1 not rlike '[12]{1}[09]{1}[0-9]{2}' )\n"
				+ ") and t1.value>9999\n"
				+ "/*no t1.edt1/edt2 yr or rowname compares but values to compare >9999*/\n"
				+ "then t2.edt2 /*put in: t2.edt2*/\n"
				+ "else 'xx' end edt3,\n"
				+ "case when @cik=t1.cik and @value=t1.value and greatest(t2.edtId,@edtId)-least(t2.edtId,@edtId)<1 then greatest(t2.edtId,@edtId) else t2.edtId end edtId3,\n"
				+ "@edtId:=t2.edtId edtId,\n"
				+ "@p3:=case when t2.p2 between 3 and 12 then t2.p2 when t1.p1=t2.p1 and t1.p1 between 3 and 12 then t2.p1 when t1.yr!='bad' and t1.p2 between 3 and 12 then t1.p2 else 0 end p3,\n"
				+ "case when @p3 between 3 and 12 and @edt3!='xx' then 'gd' else '' end gdYr,\n"
				+ "@cik:=t1.cik cik,@value:=t1.value value\n"
				+ "/*,case when t1.value!=t2.value and length(t1.value)>5 and length(t2.value)>5 then 1 else 0 end ckV,t2.value,t1.edt1,t1.edt2,t2.edt1,t2.edt2,t1.p2,t2.p2,t1.p1,t2.p1,left(t1.filedate,10) fd1,left(t2.filedate,10) fd2\n"
				+ ",t1.rowname,t2.rowname,t1.yr,t2.yr,\n"
				+ "t1.*\n"
				+ ",concat('file:///c://backtest/tableparser/',year(t1.filedate),'/qtr',quarter(t1.filedate),'/tables/',t1.accno,'_',t1.tno,'.htm') l1 \n"
				+ ",concat('file:///c://backtest/tableparser/',year(t2.filedate),'/qtr',quarter(t2.filedate),'/tables/',t2.accno,'_',t2.tno,'.htm') l2 */\n"
				+ "from TMP2_TP_REPAIR_FROM_TP_SALES t1 inner join TMP2_TP_REPAIR_FROM_TP_SALES t2\n"
				+ "on t1.cik=t2.cik and t1.v5=t2.v5\n"
				+ "where /*fix t1 row - original rows have row number less than 100000.*/\n"
				+ "(t1.edtID<0 or T1.edtId is null) and t1.row<100000 and t2.row>100000\n"
				+ "/*when t1.yr='bad' but t1.edt2 valid that edt2 is stil highly likely correct*/\n"
				+ "and t1.rw!=t2.rw\n"
				+ "and t2.edt2 rlike '[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})' and t2.yr!='bad'\n"
				+ "and datediff(greatest(t1.filedate,t2.filedate),least(t1.filedate,t2.filedate))<1200\n"
				+ "and t1.value>100 and t1.rowname not rlike 'expense|cost|administrativ|selling|net incom|retained earn|estate tax'\n"
				+ "and t1.columnText not rlike '^ two month' and t2.columntext not rlike '^ two month'\n"
				+ "order by t1.cik,t1.value;\n"
				+ "alter table TMP_TP_EDT_P_UPDATE add key(cik), add key(value), add key(edtId3);\n"
				+ "\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "DROP TABLE IF EXISTS TMP2_TP_EDT_P_UPDATE;\n"
				+ "CREATE TABLE TMP2_TP_EDT_P_UPDATE ENGINE=MYISAM\n"
				+ "select count(distinct(edtId3)) cntEdtId, t1.* from TMP_TP_EDT_P_UPDATE t1 group by t1.cik,t1.value;\n"
				+ "alter table TMP2_TP_EDT_P_UPDATE add key(cik), add key(value), add key(cntEdtId);\n"
				+ "\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "insert ignore into tp_sales_to_scrub\n"
				+ "select t1.accno, t1.fileDate, t1.cik, t1.tn, t1.trow, t1.row, t1.col, t1.tno, t1.rowname, t1.value, t1.p3, t1.edt3, t1.`DEC`, t1.columnText, t1.form from TMP_TP_EDT_P_UPDATE t1 inner join TMP2_TP_EDT_P_UPDATE t2 on t1.cik=t2.cik and t1.value=t2.value\n"
				+ "where t2.cntEdtId=1 \n"
				+ "and t1.columnText not rlike '^ two month' and t2.columntext not rlike '^ two month'\n"
				+ "and t1.gdYr='gd' and t1.edt3!='xx';\n"
				+ "\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "insert ignore into tp2_edt_p\n"
				+ "select t1.accno, t1.fileDate, t1.tno,t1.col,t1.edt3 edt2,t1.p3 p2 \n"
				+ "from TMP_TP_EDT_P_UPDATE t1 inner join TMP2_TP_EDT_P_UPDATE t2 on t1.cik=t2.cik and t1.value=t2.value\n"
				+ "where t2.cntEdtId=1 \n"
				+ "and t1.columnText not rlike '^ two month' and t2.columntext not rlike '^ two month' GROUP BY T1.ACCNO,T1.TNO,T1.COL;\n"
				+ "ALTER TABLE tp2_edt_p ADD KEY(ACCNO), ADD KEY(TNO), ADD KEY(COL);\n"
				+ "\n");

		MysqlConnUtils.executeQuery(sb.toString());
		sb.delete(0, sb.toString().length());

		qtr = startQ;
		q = startQ;
		yr = startYr;

		for (yr = startYr; yr <= endYr; yr++) {
			if (yr == 1993 && qtr < 3) {
				qtr = 3;
			}
			for (q = qtr; q <= endQ; q++) {

				sb.append("UPDATE \n"
						+ "/*SELECT T2.EDT2,T1.EDT2,T1.P2,T2.P2, T1.* FROM*/ BAC_TP_RAW"
						+ yr
						+ "qtr"
						+ q
						+ " T1 INNER JOIN tp2_edt_p T2\n"
						+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.COL=T2.COL\n"
						+ "SET T1.EDT2=T2.EDT2\n"
						+ "WHERE T2.EDT2!='XX';\n"
						+ "\n"
						+ "UPDATE \n"
						+ "/*SELECT T2.EDT2,T1.EDT2,T1.P2,T2.P2, T1.* FROM*/ BAC_TP_RAW"
						+ yr
						+ "qtr"
						+ q
						+ " T1 INNER JOIN tp2_edt_p T2\n"
						+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.COL=T2.COL\n"
						+ "SET T1.P2=T2.P2\n" + "WHERE T2.P2!=0 AND T1.P2=0;\n");
				MysqlConnUtils.executeQuery(sb.toString());
				sb.delete(0, sb.toString().length());
			}
			qtr = 1;
		}

		// generateTmp_tp_sales rerun==>uses further populated
		// tp_sales_to_scrub
		// to regenerate tmp_tp_sales to there are less gaps!
		MysqlConnUtils.executeQuery(generateTmp_tp_sales);
		sb.delete(0, sb.toString().length());

		sb.append("\n\nset sql_mode = ALLOW_INVALID_DATES;\n"
				+ "/*get cnt of each Q (conformed mm-dd). tmp_scrub_sales is created as first step when tmp_tp_sales is\n"
				+ " created and conforms enddates so they are in \n"
				+ "mm-30 format (28 days for Feb)*/\n"
				+ "\n"
				+ "/*start process of counting number of instances each month day occurs - qEnd*/\n"
				+ "DROP TABLE IF EXISTS TMP_SCRUB_SALES_QEND;\n"
				+ "CREATE TABLE TMP_SCRUB_SALES_QEND ENGINE=MYISAM\n"
				+ "SELECT COUNT(*) CNT,CIK,min(filedate) mnFd, MAX(FILEDATE) MxFd,right(EDT2,5) qEnd,p2 \n"
				+ "FROM TMP_SCRUB_SALES where p2 > 3 AND \n"
				+ "EDT2 RLIKE '[12]{1}[09]{1}[0-9]{2}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})'\n"
				+ "GROUP BY CIK,right(EDT2,5),P2 order by cik,p2 DESC;\n"
				+ "ALTER TABLE TMP_SCRUB_SALES_QEND;\n"
				+ "\n"
				+ "/*get Q1 count only*/\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "DROP TABLE IF EXISTS TMP2_SCRUB_SALES_QEND;\n"
				+ "CREATE TABLE TMP2_SCRUB_SALES_QEND ENGINE=MYISAM\n"
				+ "SELECT @q:=left(qEnd,2) q, @qp3:=CASE \n"
				+ "WHEN p2=12 AND @q>9 then @q-9 \n"
				+ "WHEN p2=12 AND @q<=9 then @q-9+12\n"
				+ "WHEN p2=12 AND @q>9 then @q-9 \n"
				+ "WHEN p2=12 AND @q<=9 then @q-9+12\n"
				+ "\n"
				+ "WHEN p2=9 AND @q>6 then @q-6 \n"
				+ "WHEN p2=9 AND @q<=6 then @q-6+12\n"
				+ "WHEN p2=9 AND @q>6 then @q-6 \n"
				+ "WHEN p2=9 AND @q<=6 then @q-6+12\n"
				+ "\n"
				+ "WHEN p2=6 AND @q>3 then @q-3 \n"
				+ "WHEN p2=6 AND @q<=3 then @q-3+12\n"
				+ "WHEN p2=6 AND @q>3 then @q-3 \n"
				+ "WHEN p2=6 AND @q<=3 then @q-3+12\n"
				+ "else 'xx' end tmpQ1,\n"
				+ "LEFT(case when length(@qP3)<2 AND @qP3!=2 then concat(0,@qp3,'-30')\n"
				+ "when length(@qP3)<2 AND @qP3=2 then concat(0,@qp3,'-28')\n"
				+ "else concat(@qp3,'-30') end,5) q1,\n"
				+ "CNT,@cik:=CIK CIK,mnFD,mxFD,qEnd qEnd,@p2:=p2 p2 FROM TMP_SCRUB_SALES_QEND;\n"
				+ "ALTER TABLE TMP2_SCRUB_SALES_QEND ADD KEY(CIK), ADD KEY(q1);\n"
				+ "\n"
				+ "/*cnt how many times same q1 occurs for each cik*/\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "DROP TABLE IF EXISTS TMP3_SCRUB_SALES_QEND;\n"
				+ "CREATE TABLE TMP3_SCRUB_SALES_QEND ENGINE=MYISAM\n"
				+ "SELECT SUM(CNT) CNT,CIK,min(mnFd) mnFd,max(mxFd) mxFd,q1 FROM TMP2_SCRUB_SALES_QEND group by cik,q1;\n"
				+ "ALTER TABLE TMP3_SCRUB_SALES_QEND ADD KEY(CIK),ADD KEY(mnFd), add key(mxFd);\n"
				+ "\n"
				+ "/*discard false positive Q1s*/\n"

				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "set @cnt=0; set @cik=0; set @mnFd='1901-01-01'; set @mxFd='1901-01-01'; \n"
				+ "DROP TABLE IF EXISTS TMP4_SCRUB_SALES_QEND;\n"
				+ "CREATE TABLE TMP4_SCRUB_SALES_QEND ENGINE=MYISAM\n"
				+ "SELECT \n"
				+ "case when (@cik=cik and @cnt/cnt>4 and mxFd between @mnFd and @mxFd)\n"
				+ "or (@cik=cik and cnt<4)\n"
				+ "then 1 else 0 end ckIt,\n"
				+ "@cnt:=case when @cik!=cik then cnt else @cnt end cntC,cnt\n"
				+ ",@mnFd:=case when @cik!=cik then mnFd else @mnFd end mnFdc\n"
				+ ",@mxFd:=case when @cik!=cik then mxFd else @mxFd end mxFdc\n"
				+ ",@cik:=cik cik, mnFd,mxFd,q1 FROM TMP3_SCRUB_SALES_QEND \n"
				+ "ORDER BY CIK,CNT DESC;\n"
				+ "ALTER TABLE TMP4_SCRUB_SALES_QEND ADD KEY(CKIT);\n" + "\n"
				+ "/*get just q1 not false positive.*/\n"
				+ "set sql_mode = ALLOW_INVALID_DATES;\n"
				+ "DROP TABLE IF EXISTS TMP5_SCRUB_SALES_QEND;\n"
				+ "CREATE TABLE TMP5_SCRUB_SALES_QEND ENGINE=MYISAM\n"
				+ "SELECT * FROM TMP4_SCRUB_SALES_QEND WHERE CKIT!=1;\n"
				+ "ALTER TABLE TMP5_SCRUB_SALES_QEND ADD KEY(CIK);\n" + "\n");

		sb.append("/*get unique rownames for each cik to use as a tool to join later*/\n"
				+ "\n\nset sql_mode = ALLOW_INVALID_DATES;\n"
				+ "DROP TABLE IF EXISTS tmp_tp_sale_rowname;\n"
				+ "CREATE TABLE tmp_tp_sale_rowname ENGINE=MYISAM\n"
				+ "SELECT T1.CIK,T1.ORIGROWNAME FROM TMP_TP_SALES T1 GROUP BY CIK,ORIGROWNAME;\n"
				+ "ALTER TABLE tmp_tp_sale_rowname ADD KEY(CIK), ADD KEY(ORIGROWNAME);\n"
				+ "\n");

		MysqlConnUtils.executeQuery(sb.toString());
		sb.delete(0, sb.toString().length());

		qtr = startQ;
		q = startQ;
		yr = startYr;

		for (yr = startYr; yr <= endYr; yr++) {
			if (yr == 1993 && qtr < 3) {
				qtr = 3;
			}
			for (q = qtr; q <= endQ; q++) {

				sb.append("\nset sql_mode = ALLOW_INVALID_DATES;\n"
						+ "/*join where same rowname - but group by accno,tno,trow so that I don't get duplicates.*/\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "DROP TABLE IF EXISTS TMP_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "CREATE TABLE TMP_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "qtr"
						+ q
						+ " ENGINE=MYISAM\n"
						+ "SELECT t1.accno,t1.tno,trow\n"
						+ "FROM BAC_TP_RAW"
						+ yr
						+ "qtr"
						+ q
						+ " T1 INNER JOIN tmp_tp_sale_rowname T2\n"
						+ "ON T1.CIK=T2.CIK AND T1.ROWNAME=T2.ORIGROWNAME\n"
						+ "WHERE (T1.P2=0 OR (EDT2 NOT RLIKE '[12]{1}[09]{1}[0-9]{2}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})'\n"
						+ "AND (EDT2 RLIKE '[12]{1}[09]{1}[0-9]{2}' OR EDT1 RLIKE '[12]{1}[09]{1}[0-9]{2}')) ) AND \n"
						+ "LENGTH(T1.ROWNAME)>5 AND T1.VALUE>100  AND TN='is'\n"
						+ "group by t1.accno,t1.tno,t1.trow; \n"
						+ "ALTER TABLE TMP_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "qtr"
						+ q
						+ " ADD KEY(ACCNO), ADD KEY(TNO), ADD KEY(TROW);\n"
						+ "\n"
						+ "/*get all rows on the trow - some may be okay.*/\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "DROP TABLE IF EXISTS TMP2_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "CREATE TABLE TMP2_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "qtr"
						+ q
						+ " ENGINE=MYISAM\n"
						+ "SELECT round(datediff(t1.edt2,'1990-01-01')/30.4375) edtid,t1.filedate,\n"
						+ "t1.accno,t1.cik,t1.tn,t1.row,t1.col,t1.tno,t1.rowname,t1.value,t1.p1,t1.p2,t1.edt1,t1.edt2\n"
						+ ",t1.tsshort,\n"
						+ "t1.yr,t1.columnPattern,t1.allColtext,t1.columnText,t1.tsLong,t1.form\n"
						+ "FROM BAC_TP_RAW"
						+ yr
						+ "qtr"
						+ q
						+ " T1 INNER JOIN TMP_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "qtr"
						+ q
						+ " T2\n"
						+ "ON T1.ACCNO=T2.ACCNO AND T1.TNO=T2.TNO AND T1.TROW=T2.TROW;\n"
						+ "ALTER TABLE TMP2_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "qtr"
						+ q
						+ " add key(cik), add key(edtid);\n"
						+ "\n"
						+ "/*IF 2 OR MORE MO VALUES IN tsShort AND ONE SEEMS RANDOM AND IT IS THE FIRST INSTANCE*/\n"
						+ "\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "DROP TABLE IF EXISTS TMP3_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "CREATE TABLE TMP3_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "qtr"
						+ q
						+ " ENGINE=MYISAM\n"
						+ "SELECT T1.*,Q1,mnFd,mxFd,cnt FROM TMP2_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "qtr"
						+ q
						+ " T1 LEFT JOIN TMP5_SCRUB_SALES_QEND T2\n"
						+ "ON T1.CIK=T2.CIK ;\n"
						+ "ALTER TABLE TMP3_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "qtr"
						+ q
						+ " ADD KEY(CIK), ADD KEY(EDTID);\n"
						+ "\n"
						+ "/*estimate p3 value - e.g., between 10.1 and 15 = 12 and 2.5 to 4 = 3 and 5 to 7 = 6 and 8 to 10 = 9*/\n"
						+ "set sql_mode = ALLOW_INVALID_DATES;\n"
						+ "DROP TABLE IF EXISTS TMP4_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "qtr"
						+ q
						+ ";\n"
						+ "CREATE TABLE TMP4_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "qtr"
						+ q
						+ " ENGINE=MYISAM\n"
						+ "select t1.accno,t1.tno,t1.col,/*Q1,mnFd,mxFd, t1.edtid,t2.edtid,t1.cik,t1.tn,t1.row,t1.col,t1.tno,t1.p1,t1.p2,t2.p2,left(t1.filedate,10) fd1,t1.edt1,t1.edt2,t2.edt2 edtValid,t1.value,t2.value,*/\n"
						+ "@v:=case when t1.value/t2.value BETWEEN 1/150     AND 1/60 then t1.value*100 \n"
						+ "when t1.value/t2.value BETWEEN 1/5000    AND 1/250 then t1.value*1000  when t1.value/t2.value BETWEEN 1/5000000 AND 1/250000 then t1.value*1000000 \n"
						+ "when t1.value/t2.value BETWEEN 60 AND 500 then t1.value/100 when t1.value/t2.value BETWEEN 150 AND 5000 then t1.value/1000 \n"
						+ "when t1.value/t2.value BETWEEN 200000 AND 5000000 then t1.value/1000000 \n"
						+ "else t1.value end val\n"
						+ "\n"
						+ ",@p3:=case when t1.p2=0 and (@v/t2.value)*t2.p2 between 10.1 and 15 then 12 \n"
						+ "when t1.p2=0 and (@v/t2.value)*t2.p2 between 2.5 and 4 then 3 \n"
						+ "when t1.p2=0 and (@v/t2.value)*t2.p2 between 5 and 7 then 6 \n"
						+ "when t1.p2=0 and (@v/t2.value)*t2.p2 between 8 and 10 then 9 \n"
						+ "when t1.p2 between 3 and 12 then t1.p2 else 'xx' end p3\n"
						+ ",@cnf:=case \n"
						+ "when @p3=12 and left(q1,2)<4 then left(q1,2)+9\n"
						+ "when @p3=12 and left(q1,2)>=4 then left(q1,2)+9-12\n"
						+ "\n"
						+ "when @p3=9 and left(q1,2)<7 then left(q1,2)+6\n"
						+ "when @p3=9 and left(q1,2)>=7 then left(q1,2)+6-12\n"
						+ "\n"
						+ "when @p3=6 and left(q1,2)<10 then left(q1,2)+3\n"
						+ "when @p3=6 and left(q1,2)>=10 then left(q1,2)+3-12\n"
						+ "end cnf,\n"
						+ "@dy:=case when @cnf!=2 then '30' else '-28' end dy,\n"
						+ "case when t1.edt2 rlike '[12]{1}[09]{1}[0-9]{2}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})'\n"
						+ "then 'xx' \n"
						+ "\n"
						+ "when @cnf<10 and @p3=12 and t1.edtId is null and concat(left(t1.filedate,4),'-',@cnf,'-',@dy) \n"
						+ "between date_sub(t1.filedate,interval 3 month) and date_sub(t1.filedate,interval 10 day) \n"
						+ "then concat(left(t1.edt2,4),'-0',@cnf,'-',@dy) \n"
						+ "\n"
						+ "when @cnf>=10 and @p3=12 and t1.edtId is null and concat(left(t1.filedate,4),'-',@cnf,'-',@dy) \n"
						+ "between date_sub(t1.filedate,interval 3 month) and date_sub(t1.filedate,interval 10 day) \n"
						+ "then concat(left(t1.edt2,4),'-',@cnf,'-',@dy) \n"
						+ "\n"
						+ "when @cnf<10 and @p3!=12 and t1.edtId is null and concat(left(t1.filedate,4),'-',@cnf,'-',@dy) \n"
						+ "between date_sub(t1.filedate,interval 2 month) and date_sub(t1.filedate,interval 10 day) \n"
						+ "then concat(left(t1.edt2,4),'-0',@cnf,'-',@dy) \n"
						+ "\n"
						+ "when @cnf>=10 and @p3!=12 and t1.edtId is null and concat(left(t1.filedate,4),'-',@cnf,'-',@dy) \n"
						+ "between date_sub(t1.filedate,interval 2 month) and date_sub(t1.filedate,interval 10 day) \n"
						+ "then concat(left(t1.edt2,4),'-',@cnf,'-',@dy) \n"
						+ " end edt3\n"
						+ " ,case when (concat(left(t1.filedate,4),'-',@cnf,'-',@dy) between date_sub(t1.filedate,interval 110 day) and  date_sub(t1.filedate,interval 15 day) \n"
						+ " ) or form rlike '/A' or (@cnf is null and @p3=3 ) then 'true' else 'false' end tst\n"
						+ "/*this estimates edt3 -- cnf is the estimated month based on period. If p6 and Q1=0330 then p6 edt month should be 06*/\n"
						+ "/*,t1.rowname,t2.origRowname,t1.yr,t1.columntext,t1.form\n"
						+ ",concat('file:///c://backtest/tableparser/',year(t1.filedate),'/qtr',quarter(t1.filedate),'/tables/',t1.accno,'_',t1.tno,'.htm') l1 */\n"
						+ "from TMP3_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "qtr"
						+ q
						+ " t1 inner join tmp_tp_sales t2 on t1.cik=t2.cik where ((t1.edtId-t2.edtId) between 0 and 3 \n"
						+ "or (left(t1.edt2,4)=left(t2.edt2,4) and t1.edt2 not rlike '[12]{1}[09]{1}[0-9]{2}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})') )\n"
						+ "AND  (T1.P2=0 OR (t1.EDT2 NOT RLIKE '[12]{1}[09]{1}[0-9]{2}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})'\n"
						+ "AND (t1.EDT2 RLIKE '[12]{1}[09]{1}[0-9]{2}' OR t1.EDT1 RLIKE '[12]{1}[09]{1}[0-9]{2}')) ) AND T1.VALUE>100 and t1.yr!='bad';\n"
						+ "ALTER TABLE TMP4_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "qtr"
						+ q
						+ " ADD KEY(accno), ADD KEY(tno), add key(col);\n"
						+ "\n\n"
						+ "INSERT IGNORE INTO TP_SALES_TO_SCRUB\n"
						+ "select T1.accno, T1.fileDate, T1.cik, T1.tn, T1.trow, T1.row, T1.col, T1.tno, T1.rowname, T1.value,\n"
						+ "CASE WHEN T1.P2=0 AND T2.p3 between 3 and 12 then t2.p3 else t1.p2 end p2,\n"
						+ "CASE WHEN T1.edt2 not rlike '[12]{1}[09]{1}[0-9]{2}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})'\n"
						+ " AND T2.edt3 rlike '[12]{1}[09]{1}[0-9]{2}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})'\n"
						+ " then t2.edt3 else t1.edt2 end edt2, T1.`DEC`, T1.columnText, T1.form\n"
						+ "from bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ " t1 inner join TMP4_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "QTR"
						+ q
						+ " t2\n"
						+ "on t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col \n"
						+ "where tst='true';\n"
						+ "\n"
						+ "update \n"
						+ "/*select t1.p2,t2.p3 from */ bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ " t1 inner join TMP4_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "QTR"
						+ q
						+ " t2\n"
						+ "on t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col\n"
						+ "set t1.p2=t2.p3\n"
						+ "where tst='true' and t2.p3!='xx' and t1.p2=0 and t2.p3!=0;\n"
						+ "\n"
						+ "update \n"
						+ "/*select t1.edt2,t2.edt3 from*/ bac_tp_raw"
						+ yr
						+ "qtr"
						+ q
						+ " t1 inner join TMP4_TP_SALE_ROWNAME_MATCH_"
						+ yr
						+ "QTR"
						+ q
						+ " t2\n"
						+ "on t1.accno=t2.accno and t1.tno=t2.tno and t1.col=t2.col\n"
						+ "set t1.edt2=t2.edt3\n"
						+ "where tst='true' and t2.edt3!='xx' and \n"
						+ "t1.edt2 not rlike '[12]{1}[09]{1}[0-9]{2}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})'\n"
						+ "and t2.edt3 rlike '[12]{1}[09]{1}[0-9]{2}-(0[0-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})';\n");

				MysqlConnUtils.executeQuery(sb.toString());
				sb.delete(0, sb.toString().length());
			}
			qtr = 1;
		}

		// generateTmp_tp_sales rerun==>uses further populatedtp_sales_to_scrub
		// to regenerate tmp_tp_sales to there are less gaps!
		MysqlConnUtils.executeQuery(generateTmp_tp_sales);
		sb.delete(0, sb.toString().length());
	}

	public static void putBadInTP_Sales_To_Scrub_Table(int startYr, int endYr,
			boolean regenerate) throws SQLException, FileNotFoundException {

		String query = "";

		if (regenerate) {

			query = "DROP TABLE IF EXISTS `stockanalyser`.`tp_sales_to_scrub_yr`;\r"
					+ "CREATE TABLE `tp_sales_to_scrub_yr` (\r"
					+ "  `yr` varchar(20) CHARACTER SET latin1 NOT NULL DEFAULT '-1',\r"
					+ "  `accno` varchar(20) CHARACTER SET latin1 NOT NULL DEFAULT '-1',\r"
					+ "  `fileDate` datetime DEFAULT NULL,\r"
					+ "  `cik` int(11) DEFAULT NULL,\r"
					+ "  `tn` varchar(6) CHARACTER SET latin1 DEFAULT NULL,\r"
					+ "  `trow` TINYINT(3) DEFAULT NULL COMMENT 'table line number in financial table',\r"
					+ "  `row` int(5) NOT NULL DEFAULT '-1' COMMENT 'row count in mysql ',\r"
					+ "  `col` TINYINT(3) NOT NULL DEFAULT '0' COMMENT 'data col number in financial table',\r"
					+ "  `tno` int(5) NOT NULL DEFAULT '-1',\r"
					+ "  `rowname` varchar(125) CHARACTER SET latin1 DEFAULT NULL,\r"
					+ "  `value` double(23,5) DEFAULT NULL,\r"
					+ "  `p2` int(3) NOT NULL DEFAULT '0' COMMENT 'if html - per2 parsed from cell, if txt per2 parsed based on col hdg ratio matching',\r"
					+ "  `edt2` varchar(10) CHARACTER SET latin1 NOT NULL DEFAULT '1901-01-1',\r"
					+ "  `DEC` int(10) DEFAULT NULL,\r"
					+ "  `columnText` varchar(200) DEFAULT NULL,\r"
					+ "  `form` varchar(15) CHARACTER SET latin1 DEFAULT NULL COMMENT 'this will equal rowratioBeforeColumnUtil if generic in htmlTxt field',\r"
					+ "  PRIMARY KEY (`accno`,`tno`,row) COMMENT 'need to get all possible enddates (edt2) and periods.',\r"
					+ "  KEY `accno` (`accno`),\r"
					+ "  KEY `tno` (`tno`),\r"
					+ "  KEY `trow` (`trow`),\r"
					+ "  KEY `row` (`row`),\r"
					+ "  KEY `p2` (`p2`),\r"
					+ "  KEY `edt2` (`edt2`),\r"
					+ "  KEY `col` (`col`),\r"
					+ "  KEY `cik` (`cik`),\r"
					+ "  KEY `fileDate` (`fileDate`),\r"
					+ "  KEY `value` (`value`)\r"
					+ ") ENGINE=MyISAM DEFAULT CHARSET=utf8;\r";

			MysqlConnUtils.executeQuery(query);

		}

		int yr = startYr, qtr = 1, endQ = 4, q = 1;
		String table = "";
		for (yr = startYr; yr <= endYr; yr++) {
			if (yr == 1993 && qtr < 3) {
				qtr = 3;
			} else
				qtr = 1;

			for (q = qtr; q <= endQ; q++) {
				table = "bac_tp_raw" + yr + "qtr" + q;

				query = "  insert ignore into tp_sales_to_scrub_yr  \r"
						+ "  select t1.yr,t1.accno,t1.fileDate,t1.cik,t1.tn,t1.trow,t1.row,"
						+ "t1.col,t1.tno,t1.rowname,t1.value,t1.p2,t1.edt2,t1.`DEC`,t1.columnText,t1.form\r"
						+ "  from tp_sales_to_scrub t2 inner join "
						+ table
						+ " t1 \r"
						+ "  on t1.accno=t2.accno and t1.tno=t2.tno and t1.trow=t2.trow\r"
						+ "  where t1.p2=12 or ((t1.p2=3 or t1.p2=6 or t1.p2=9 ) /*and tc=4 <--put back?*/);\r";

				MysqlConnUtils.executeQuery(query);
			}
			qtr = 1;
		}

		query = "insert ignore into tp_sales_to_scrub_yr "
				+ "\r select 'good',t1.* from tp_sales_to_scrub t1; ";
		MysqlConnUtils.executeQuery(query);
	}

	public static void fetchForCIKallRownames(int startYr, int endYr, int cik) throws SQLException, FileNotFoundException {

		// rowname (@rowname) is equal to whatever I set @rowname in mysql to be
		int qtr = 1, q = 1, endQ = 4, yr = startYr, cnt = 0;
		String table = "", query = "";

		StringBuilder sb = new StringBuilder();
		List<String> list = new ArrayList<String>();

		Connection conn = MysqlConnUtils.getConnection();
		Statement stmt = conn.createStatement();
		query = "select rn from (select count(*) cnt, cik,tno,trim(rowname) rn \r"
				+ " from tp_sales_to_scrub t1 where cik="
				+ cik
				+ " and "
				+ "rowname not rlike 'miles' group by replace(rowname,'.','') \r"
				+ "order by cnt desc) t1 /*where cnt>9*/\r;";

		System.out.println("query=" + query);

		ResultSet rs = stmt.executeQuery(query);

		while (rs.next()) {
			list.add(rs.getString(1));
		}
		rs.close();

		String orClause = "";

		System.out.println("list.size=" + list.size());
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).length() > 10	)
				orClause = " or ( t1.rowname rlike  '" + list.get(i).trim()
						+ "' and length(t1.rowname)>8 )  ";
			else
				orClause = "";

			for (yr = startYr; yr <= endYr; yr++) {
				if (yr == 1993 && qtr < 3) {
					qtr = 3;
				} else
					qtr = 1;

				for (q = qtr; q <= endQ; q++) {
					cnt++;
					table = "bac_tp_raw" + yr + "qtr" + q;
					if (cnt == 1) {
						sb.append("\r\rdrop table if exists tmp_fetch_bac_tp_raw_cik_"
								+ cik
								+ ";\r"
								+ "create table tmp_fetch_bac_tp_raw_cik_"
								+ cik
								+ " engine=myisam\r"
								+ "select * from "
								+ table
								+ " t1 where cik="
								+ cik
								+ " and tn='is' and (trim(rowname) = '"
								+ list.get(i).trim()
								+ "' \r"
								+ orClause
								+ " ) and yr!='bad' and value is not null;\r"
								+ "alter table tmp_fetch_bac_tp_raw_cik_"
								+ cik
								+ " add primary key(accno,tno,row), add key(cik), add key(accno),add key(edt2)\r"
								+ ", add key(p2), add key(value);\r\r");

						sb.append("insert ignore into  tmp_fetch_bac_tp_raw_cik_"
								+ cik
								+ "\rselect t1.* from "
								+ table
								+ " t1 inner join (\r" +
								"SELECT t1.accno,t1.tno,t1.trow FROM "
								+ table
								+ " t1 inner join tp_sales_to_scrub t2 \r" +
								" on t1.cik=t2.cik and t1.value=t2.value and left(t1.edt2,4)=left(t2.edt2,4)"
								+ " where t1.cik="+cik+" and t1.value>1000 and t1.tn='is' and t1.yr!='bad' "
								+ " and "
								+ "(t1.rowname rlike 'sales|revenue' or length(t1.rowname)=0 or trim(t1.rowname) = '"
								+ list.get(i).trim()
								+ "' \r"
								+ orClause+")) t2 on t1.accno=t2.accno and t1.tno=t2.tno and t1.trow=t2.trow;");

					} else {
						sb.append("\rinsert ignore into tmp_fetch_bac_tp_raw_cik_"
								+ cik
								+ "\r"
								+ "select * from "
								+ table
								+ " t1 where cik="
								+ cik
								+ " and tn='is' and (trim(rowname) = '"
								+ list.get(i).trim()
								+ "' \r"
								+ orClause
								+ ") and yr!='bad' and value is not null;\r");
						
						sb.append("insert ignore into  tmp_fetch_bac_tp_raw_cik_"
								+ cik
								+ "\rselect t1.* from "
								+ table
								+ " t1 inner join (\r" +
								"SELECT t1.accno,t1.tno,t1.trow FROM "
								+ table
								+ " t1 inner join tp_sales_to_scrub t2 \r" +
								" on t1.cik=t2.cik and t1.value=t2.value and left(t1.edt2,4)=left(t2.edt2,4)"
								+ " where t1.cik="+cik+" and t1.value>1000 and t1.tn='is' and t1.yr!='bad' "
								+ " and "
								+ "(t1.rowname rlike 'sales|revenue' or length(t1.rowname)=0 or trim(t1.rowname) = '"
								+ list.get(i).trim()
								+ "' \r"
								+ orClause+")) t2 on t1.accno=t2.accno and t1.tno=t2.tno and t1.trow=t2.trow;");

					}
				}
				qtr = 1;
			}
		}
		
		MysqlConnUtils.executeQuery(sb.toString());

	}
	

	public static void main(String[] args) throws SQLException, IOException,
			ParseException {

		// TODO: use 'tn' to filter out 'invali' tablenames. And as a last ditch
		// effort attack for missing values after. I should also save the long
		// tablename!

		/*
		 * TODO: USE NLP.getEnddatePeriodFromTablesentence on toc is of value as
		 * it routinely lists enddates and periods. Use tablesentence parser
		 * developed to parse from toc - just capture toc tablename, period and
		 * enddates and dump entire toc into mysql field.
		 */

		// TODO: Get 8-Ks historically for tp AND conform all above NOT TO
		// EXCLUDE 8-Ks (some only apply to 10k/q).

		// TODO: 1. auto update each day for 8-K! not for tp b/c xbrl).

		// TODO: identify missing 10-Q/Ks time gaps in tp_sales for each CIK and
		// then find accnos in tpIdx not parsed and reparse.

		// TODO: Need to get desktop that has independent drive that can be auto
		// backed-up to.

		// TODO: Categorize rownames where 2 or more rows equal net/ttl (net=sub
		// rows or ttl=stt rows). If several rows have stt=7 then ttl=7 is
		// likely a broad category type. After I find broad category types I can
		// then use that to homogenize across tables.

		// FinancialStatementsTPRaw.putBadInTP_Sales_To_Scrub_Table(1993, 2003,
		// true);

		// FinancialStatementsTPRaw.fetchCIK(1993, 2009, "7286", "", true);
		FinancialStatementsTPRaw fs = new FinancialStatementsTPRaw();
		fs.fixRownamesAndTNtype("bac_tp_raw2000qtr1", true);

		// need to add drop table for each tmp cik table sent through fetchCIK

	}

}
