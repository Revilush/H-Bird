package xbrl;

//bac_tp_rawyyyyQtr# table is used to determine which acc are fetched and thus missing against tpidx.
// load infile into mysql is the most efficient method - this  involves writing to disk in java and then reading it into mysql. 
//One of the benefits is it does not require exact column match - just lines up based on separator

//TODO: two types of TablePaser downloads from sec.gov.  1. using masterIndex and gets both 10-Q/K
//(need to ck how it gets 10K/10Q when it is xbrl filing) and 2. get secZipFile. Can only get masterIdx and secZipFile day after 
//- so always download secZipFile and then call TableParserLocal. 3. TableParser is also used by BWireEarnings
//pre-2003 it grabs complete txt file.
//Note post 1997 everything is in secZipFiles which can be parsed by running TableParserLocal

//see 'REMOVE' to reparse .txt files on local drive (use TableParserLocal for secZipFiles).

//tableParser Outline of process:
//1. dateRangeQuarters (startDt,endDt) get yr/qtr then calls
//2. downloadFilingDetails(localPath) 
//- downloads index-htm site (asigns formType which
//can be 8k or 10q/k) and then calls
//3. getFilingDetails(fileHtmlStr)
//- parses key info from file (index-htm file)
//and then calls
//4. downloadExhibit99 if formtype contains 8 else calls downloadCompleteSubmission
//5.both will call: downloadEx99
//6. downloadEx99 grabs the file from sec.gov to be parsed
//7. parseTable
//8. getGenericTableText 
//which calls removeAttachments
//which calls stripHtml

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import contracts.GetContracts;

public class TableParser {

	// static is a constant - such as the folder location below. If I chg the
	// value of the constant then the same value of that variable will remain
	// available for all instances (those that are running of this class). Think
	// of 'static' as 'read only' (not being changed, i.e.,. written to).

	public static int maxFileSize = 5000000;
	public static int fileSizeToLoad = 20000000;

	public Document document;
	public static String skipAmend = "/* and cik between 0 and 10000 */";

	public static String adjustIdxLoc = "";

	public static String baseTableParserFolder = "c:/backtest/tableParser/";
	public static String baseSecZipDownloadedFolder = "c:/secZipFiles/Downloaded/";

	public static PrintWriter pwYyyyQtr = null;

	// public static String tpchFilename = "c:/BACKTEST/CH/TPCH";
	// public static String tpchFoundFilename = "c:/BACKTEST/CH/TP_CH";
	// over 3 and you merge tables and end up loosing ColumnHeadings
	public String fileDate = null; // "2099-01-01";
	public String companyName = "";// from master.idx
	public String coName = ""; // matched.
	public String headerPortionOfFile = "";
	public String cik = "0";
	public String acceptedDate = null;
	public String periodOfReport = null, qtrEnd = null;
	public String colHeadingPatternAtEachLine = "";
	public static String tableSaved = "";

	public String acc = null;// "0000020232-01-500022";
	public String formType = null;// "10-K";
	public String fye = null;// "1231";
	public String formStr = null;
	public String formItems = null;
	public String sicCode = null;
	public String toc2 = "";
	public String goodHeadingSection = null;
	public String tableSentence = null;

	public String tp_sentLine1 = "", tp_sentLine2 = "";

	public int startYr;
	public int iQtr;
	public int row = 0;
	public int tocNameMatch;

	@SuppressWarnings("unused")
	private boolean isHtml = false;
	public String heading = "good";

	public int tableCount;

	public List<String> sentenceEndedStr = null;
	public List<String> sentenceMonthStr = null;
	public List<String> sentenceYearStr = null;

	// final String is string that will always be used (not change).

	public static Pattern patternMonthDayYY = Pattern
			.compile("(?i)(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC).{1,10}[,\\d]{1,3}.{1,2}\\'\\d\\d");

	public static final String LineSeparator = System.getProperty("line.separator", "\r\n");

	public static String[] MonthNamesArray = new String[] { "January", "February", "March", "April", "May", "June",
			"July", "August", "September", "October", "November", "December" };

	public static Pattern MonthNamesLongPattern = Pattern.compile(" January | February | March | April | May | June"
			+ " | July | August | September | October | November | December ");

	public static Pattern AuditedRestatedNotFollowedByTabPattern = Pattern.compile(
			"(?i)(   [\\(un]audited|   [\\(]restated|   [\\(]reported|   [\\(]adjust|   [\\(]revised|   change|   \\%"
					+ "|   increase|   Dollar[s]|   DOLLAR[S]|   percent" + "|   [\\(]decrease|   restated)(?!\t)");
	// change can come up in tablename - so require min of 3 ws.
	public static Pattern AuditedRestatedNoParenFollowedByTabPattern = Pattern
			.compile("(?i)(audited|revised|restated|reported)(?!\t)");
	// can't have audited not followed by tab b/c that includes (unaudited). So
	// 2 patterns each must be met

	public static Pattern BalanceSheetInitialCapsPattern = Pattern
			.compile("(?sm)(" + "(Condens.{0,10})?(Consolidat.{0,10})?(Combin.{0,10})?"
					+ "((?i)([\\(]{0,1}[un]{0,2}audited[\\)]{0,1}.{0,10}))?"
					+ "(Subsidiar.{0,8})?(Balance.{0,3}Sheet\\b(?!\\.)|Balance.{0,3}Sheets\\b(?!\\.)|"
					+ "Statement.{1,6}Asset.{1,7}Liabilities(.{1,6}Partner.{1,5}Deficit\\b(?!\\.))?|"
					+ "Statement.{1,6}Net.{1,3}Asset.{1,5}Liquidation\\b(?!\\.)|"
					+ "Statement.{1,6}(Consolidat.{1,5})?(Financial)?(.{0,3}Condition|.{0,3}Position\\b(?!\\.)))"
					+ "((?i)(.{0,8}[\\(]{0,1}[un]{0,2}[aA]{1}udited[\\)]{0,1}))?)");
	// match=Statements of Balance Sheet (unaudited)

	public static Pattern BalanceSheetAllCapsPattern = Pattern.compile("(?sm)("
			+ "(CONDENS.{0,10})?(CONSOLIDAT.{0,10})?(COMBIN.{0,10})?" + "([\\(]{0,1}[UN]{0,2}AUDITED[\\)]{0,1}.{0,10})?"
			+ "(SUBSIDIAR.{0,10})?(BALANCE[ \t\r\n]{1,8}SHEET[S]{0,2}|"
			+ "STATEMENT.{1,6}ASSET.{1,7}LIABILITIES(.{1,6}PARTNER.{1,5}DEFICIT)?|"
			+ "STATEMENT.{1,6}NET.{1,3}ASSET.{1,5}LIQUIDATION|"
			+ "STATEMENT.{1,6}(CONSOLIDAT.{1,5})?(FINANCIAL)?(.{0,3}CONDITION|.{0,3}POSITION))"
			+ "(.{0,8}[\\(]{0,1}[UN]{0,2}AUDITED[\\)]{0,1})?)|BALANCE SHEETS?|LIABILITIES AND STOCKHOLDER.{1,4}EQUITY");

	public static Pattern IncomeInitialCapsPattern = Pattern.compile("(?sm)("
			+ "(Condens.{0,5})?(Consolidat.{0,5})?(Combin.{0,5})?(Comprehensiv.{0,4})?(Subsidiar.{0,5})?"
			+ "((?i)(.{0,5}audited.{0,5}))?"
			+ "(Statement.{0,4}(f.{0,3})?(Results.{0,3}f.{0,3})?(Operation\\b(?!\\.)|Operations\\b(?!\\.)).{0,1}"
			// statement of operation
			+ "|(Consolidat.{1,5})?Results.{1,6}Operation[s]{0,1}"
			+ "|Consolidat.{1,5}Earning[s]|Statement.{0,4}f.{0,3}(Net.{0,3})?(Retained.{0,3})?(Earnings\\b(?!\\.)"
			+ "|Earning\\b(?!\\.)).{0,1}(.{0,3}(?i)Loss.{0,1})?"
			+ "|Statement.{0,4}f.{0,3}Cons.{0,11}(Earnings\\b(?!\\.)|Earning\\b(?!\\.)).{0,1}"
			// statement of earnings (net or retained)
			+ "|Statement.{0,4}f.{0,3}(Net.{0,3})?(Cons.{0,11})?Income\\b(?!\\.)(.{0,3}(?i)loss.{0,1})?"
			+ "|Statements? of [\\(]{0,1}Loss"
			// +"|Statement of \\(Loss\\) Income"
			// statements? of income
			+ "|Income.{0,2}(Statement\\b(?!\\.)|Statements\\b(?!\\.)).{0,1}"
			+ "|Statement.{1,6}Current.{1,3}nd.{1,3}Retained.{1,3}(Earnings\\b(?!\\.)|Earning\\b(?!\\.))"
			+ "|Consolidated Income\\b(?!\\.)" + "|Statement.{0,4}f.{0,3}(Revenue\\b(?!\\.)|Revenues\\b(?!\\.))"
			+ "|Statement.{0,4}f.{0,3}(Loss\\b(?!\\.)|Losses\\b(?!\\.))"
			+ "|Statement.{0,4}f.{0,3}Distributable.{0,3}Income(?!\\.)"
			+ "|Consolidated (Operation\\b(?!\\.)|Operations\\b(?!\\.))"
			// + "|((?i)statements? of INCOME)"
			+ "|Summary.{0,4}f.{0,3}Earnings\\b(?!\\.)|Summary.{0,4}f.{0,3}Earning\\b(?!\\.)|"
			+ "Summary.{0,4}f.{0,3}(Operation\\b(?!\\.)|Operations\\b(?!\\.))"
			// (?!\\.) works if Operations. but not if Operation.
			+ "|(Consolidat|Condense).{1,5}Results.{0,4}f.{0,3}(Operation\\b(?!\\.)|Operations\\b(?!\\.))"
			+ "|Statement.{0,4}f.{0,3}(Other.{0,3})?Comprehensive.{0,3}(Income|(Retained.{0,3})?Earnings\\b(?!\\.)))"
			// statement of comprehensive income|earnings
			+ "(.{0,6}nd.{0,3}Retained.{0,3}Earnings\\b(?!\\.))?"
			+ "(.{0,6}(nd|,|\\&).{0,3}(Other.{0,3})?Comprehensive.{0,2}"
			+ "(.{0,3}Income.{0,1}|.{0,3}Earnings\\b(?!\\.)|.{0,3}Loss.{0,1}))?(.{0,3}Loss.{0,1})?"
			+ "(((?i).{0,5}AUDITED.{0,2}[\\) ]{1,2}))?)");

	// don't use: [oOfF\t\r\n ]{1,9} when it is not all caps. same in b/s and
	// c/f.

	// public static Pattern tmp =
	// Pattern.compile("STATEMENTS.{1,3}OF.{1,3}OPERATIONS");

	public static Pattern IncomeAllCapsPattern = Pattern.compile(
			"(?sm)(" + "(CONDENS.{0,5})?(CONSOLIDAT.{0,5})?(COMBIN.{0,5})?(COMPREHENSIV.{0,4})?(SUBSIDIAR.{0,5})?"
					+ "((?i)(.{0,5}AUDITED.{0,5}))?"
					+ "(STATEMENT.{0,4}([Ff].{0,3})?(RESULTS.{0,5}[oOFf].{0,5})?OPERATION[S]{0,2}"
					+ "|(CONSOLIDAT.{1,5})?RESULTS.{1,6}OPERATION[S]{0,1}"
					+ "|STATEMENT.{0,4}[Ff].{0,3}(NET.{0,3})?(RETAINED.{0,3})?EARNING[S]{0,1}(.{0,3}LOSS.{0,1})?"
					+ "|STATEMENT.{0,4}[Ff].{0,3}CONSOLIDAT.{0,5}EARNING[S].{0,1}"
					+ "|CONSOLIDATED EARNING[S]|STATEMENT[S].{1,5}[Ff].{1,2}EARNING[S]"
					+ "|STATEMENT.{0,4}[Ff].{0,3}(CONS.{0,11})?(NET.{0,3})?INCOME{0,1}(.{0,3}LOSS.{0,1})?"
					+ "|STATEMENTS OF INCOME" + "|INCOME.{0,2}STATEMENT[S]{0,1}"
					+ "|STATEMENT.{1,6}CURRENT.{1,3}ND.{1,3}RETAINED.{1,3}EARNING[S]" + "|CONSOLIDATED INCOME"
					+ "|STATEMENT.{0,4}[Ff].{0,3}REVENUES? ?(AND EXPENSES?)?" + "|STATEMENT[S][oOfF\t\r\n ]{1,10}LOSS"
					+ "|STATEMENTS? OF [\\(]{0,1}LOSS" + "|STATEMENT[S][oOfF\t\r\n ]{1,10}DISTRIBUTABLE.{0,3}INCOME"
					+ "|CONSOLIDATED OPERATION|STATEMENT[S][oOfF\t\r\n ]{1,10}EARNING[S]"
					+ "|SUMMARY[oOfF\t\r\n ]{1,9}OPERATION[S]" + "|SUMMARY[oOfF\t\r\n ]{1,9}EARNING[S]"
					+ "|(CONSOLIDAT|CONDENSE).{1,5}RESULTS[S][oOfF\t\r\n ]{1,10}OPERATION[S]"
					+ "|STATEMENT.{0,4}[Ff].{0,5}(OTHER.{0,3})?COMPREHENSIVE.{0,3}(INCOME|(RETAINED.{0,3})?EARNINGS))"
					// STATEMENT OF COMPREHENSIVE INCOME|EARNINGS
					+ "(.{0,6}ND.{0,3}RETAINED.{0,3}EARNINGS.{0,1})?"
					+ "(.{0,6}(ND|,|\\&).{0,3}(OTHER.{0,3})?COMPREHENSIVE.{0,2}"
					+ "(.{0,3}INCOME.{0,1}|.{0,3}EARNINGS.{0,1}|.{0,3}LOSS.{0,1}))?(.{0,3}LOSS.{0,1})?"
					+ "(((?i).{0,5}AUDITED.{0,2}[\\) ]{1,2}))?)");

	public static Pattern StockHoldersEquityInitialCapsPattern = Pattern
			.compile("(?s)((Condens.{0,12})?(Consolidat.{0,12})?(Combin.{0,12})?(Comprehensive.{0,12})?"
					+ "((?i)(.{0,10}[\\(]{0,1}[un]{0,2}audited[\\)]{0,1}))?"
					+ "(Statement.{0,12}|Liabil.{0,16})(Subsidiar.{0,12})?((Partner.{0,4}|holder.{0,4})(Equit.{0,2}|Def[ei]cit.{0,2}))"
					+ "((?i)[\\(]{0,1}[un]{0,2}[Aa]{1}udited[\\)]{0,1}.{0,10})?)");

	public static Pattern StockHoldersEquityAllCapsPattern = Pattern
			.compile("(?s)((CONDENS.{0,12})?(CONSOLIDAT.{0,12})?(COMBIN.{0,12})?(COMPREHENSIVE.{0,12})?"
					+ "((?i)(.{0,10}[\\(]{0,1}[UN]{0,2}AUDITED[\\)]{0,1}))?"
					+ "(STATEMENT.{0,12}|LIABILIT.{0,16})(SUBSIDIAR.{0,12})?((PARTNER.{0,4}|HOLDER.{0,4})(EQUIT.{0,2}"
					+ "|DEF[EI]CIT.{0,2}))" + "((?i)[\\(]{0,1}[UN]{0,2}AUDITED[\\)]{0,1}.{0,10})?)");

	public static Pattern CashFlowInitialCapsPattern = Pattern.compile("(?sm)(("
			+ "(Condens.{0,12})?(Consolidat.{0,12})?(Combin.{0,12})?"
			+ "((?i)(.{0,10}[\\(]{0,1}[un]{0,2}audited[\\)]{0,12}))?" + "(Statement.{0,12})?(Subsidiar.{0,10})?"
			+ "(Cash.{0,3}([Ff]low\\b(?!\\.)|[Ff]lows\\b(?!\\.)).{0,1}"
			+ "|Change.{1,3}in.{1,3}Net.{1,3}Assets\\b(?!\\.)"
			+ "|Cashflow|CashFlow|Change.{1,6}Financial.{1,2}Position\\b(?!\\.))"
			+ "((?i)(.{0,10}[\\(]{0,1}[un]{0,2}[Aa]{1}udited[\\)]{0,1}))?)(?!.{0,12}(used )))(?!.{1,30}(operations|:))");

	public static Pattern CashFlowAllCapsPattern = Pattern
			.compile("(?sm)(" + "(CONDENS.{0,12})?(CONSOLIDAT.{0,12})?(COMBIN.{0,12})?"
					+ "((?i)(.{0,10}[\\(]{0,1}[UN]{0,2}AUDITED[\\)]{0,12}))?" + "(STATEMENT.{0,12})?(SUBSIDIAR.{0,10})?"
					+ "(CASH[ \t\r\n]{1,8}FLOWS?|CASHFLOW|CHANGE.{1,6}FINANCIAL.{1,2}POSITION)"
					+ "((?i)(.{0,10}[\\(]{0,1}[UN]{0,2}AUDITED[\\)]{0,1}))?)(?!.{1,30}(operations|:))");
	// NEGATIVE LOOK AHEAD ISN'T NECESSARY - SEE E.G., (?![S FROMUSED]{5,8}
	// ISN'T NECESSARY
	// CONDENSED CONSOLIDATED CASH FLOW STATEMENTS
	public static Pattern OtherAllCapsFinancialStatements = Pattern.compile("(?sm)("
			+ "(CONDENS|CONSOLIDAT|COMBIN).{1,5}(FINANCIAL STATEMENTS\\b(?!\\.)|"
			+ "STATEMENTS?.{1,2}OF.{1,2}(CHANGES?.{1,2}IN.{1,2})?NET.{1,2}ASSETS(.{1,4}(Liquidation.{1,2}Basis|LIQUIDATION BASIS))?)"
			+ "([\\(]{0,1}[UN]{0,2}AUDITED[\\)]{0,1}.{0,10})?" + "(.{0,8}[\\(]{0,1}[UN]{0,2}AUDITED[\\)]{0,1})?)");

	public static Pattern Other2AllCaps = Pattern.compile("CAPITALIZATION( AND LIABILITIES)?");

	public static Pattern OtherInitialCapsFinancialStatements = Pattern.compile("((?sm)("
			+ "(Condens|Consolidat|Combin).{1,5}(Financial Statements\\b(?!\\.)|"
			+ "STATEMENTS?.{1,2}OF.{1,2}(CHANGES?.{1,2}IN.{1,2})?NET.{1,2}ASSETS(.{1,4}(Liquidation.{1,2}Basis|LIQUIDATION BASIS))?)"
			+ "([\\(]{0,1}[un]{0,2}Audited[\\)]{0,1}.{0,10})?"
			+ "(.{0,8}[\\(]{0,1}[Un]{0,2}[Aa]{1}udited[\\)]{0,1})?))");

	public static Pattern MoreTabeleNamePatterns = Pattern
			.compile("(?i)Liabilities.{1,2}and.{1,2}Stockholder['s]{0,2}.{1,2}Equity");

	public static Pattern TableNamePattern = Pattern
			.compile(BalanceSheetAllCapsPattern.pattern() + "|" + BalanceSheetInitialCapsPattern.pattern() + "|"
					+ IncomeInitialCapsPattern.pattern() + "|" + IncomeAllCapsPattern.pattern() + "|"
					+ StockHoldersEquityInitialCapsPattern.pattern() + "|" + StockHoldersEquityAllCapsPattern.pattern()
					+ "|" + CashFlowAllCapsPattern.pattern() + "|" + CashFlowInitialCapsPattern.pattern() + "|"
					+ OtherAllCapsFinancialStatements.pattern() + "|" + Other2AllCaps.pattern() + "|"
					+ OtherInitialCapsFinancialStatements.pattern() + "|" + MoreTabeleNamePatterns.pattern());

	public static Pattern MonthPattern = Pattern
			// 30-Jun
			.compile("[\\xA0 \t-]*(Jan[\\.\t ]{1}|January[ ]{0,1}|Feb[\\.\t ]{1}|February| Mar[\\.\t ]{1}|"
					+ "March|Apr[\\.\t ]{1}|April|May[\\.\t ]{1}(?!([Nn]{1}ot|Dep|Limit))|Jun[\\.\t ]{1}|June|Jul[\\.\t ]{1}|July|Aug[\\.\t ]{1}"
					+ "|August|Sep[\\.\t ]{1}|Sept[\\.\t ]{1}|Septemb[er]{0,2}|Oct[\\.\t ]{1}|October|Nov[\\.\t ]{1}"
					+ "|November|Dec[\\.\t ]{1}|December|JAN[\\.\t ]{1}|JANUARY|FEB[\\.\t ]{1}"
					+ "|FEBRUARY| MAR[\\.\t ]{1}|MARCH|APR[\\.\t ]{1}|APRIL|MAY[\\.\t ]{1}(?!(NOT|DEP|LIMIT))|JUN[\\.\t ]{1}"
					+ "|JUNE|JUL[\\.\t ]{1}|JULY|AUG[\\.\t ]{1}|AUGUST|SEP[\\.\t \r\n]"
					+ "{1}|SEPT[\\.\t ]{1}|SEPTEMBER|OCT[\\.\t ]{1}|OCTOBER|NOV[\\.\t ]{1}|NOVEMBER"
					+ "|DEC[\\.\t ]{1}|DECEMBER),? ?([\\d]{0,2}[, \t\\xA0])[\\xA0 \t$]*");
	// ?[\\d]{0,2}[\\xA0 \t]
	public static Pattern MonthPatternSimple = Pattern
			.compile("(Jan[!\\.\t ]{1}|January|Feb[|\\.\t ]{1}|February| Mar[!\\.\t ]{1}|"
					+ "March|Apr[!\\.\t ]{1}|April|May[!\\.\t ]{1}(?!([Nn]{1}ot|Dep|Limit))|Jun[!\\.\t ]{1}|June|Jul[!\\.\t ]{1}|July|Aug"
					+ "[!\\.\t ]{1}|August|Sep[!\\.\t ]{1}|Sept[!\\.\t ]{1}|September|Oct[!\\.\t ]{1}|October|Nov"
					+ "[!\\.\t ]{1}|November|Dec[!\\.\t ]{1}|December|JAN[!\\.\t ]{1}|JANUARY|FEB"
					+ "[!\\.\t ]{1}|FEBRUARY| MAR[!\\.\t ]{1}|MARCH|APR[!\\.\t ]{1}|APRIL|MAY[!\\.\t ]{1}(?!(NOT|DEP|LIMIT))|JUN"
					+ "[!\\.\t ]{1}|JUNE|JUL[!\\.\t ]{1}|JULY|AUG[!\\.\t ]{1}|AUGUST|SEP[!\\.\t ]"
					+ "{1}|SEPT[!\\.\t ]{1}|SEPTEMBER|OCT[!\\.\t ]{1}|OCTOBER|NOV[!\\.\t ]{1}|NOVEMBER|DEC[!\\.\t ]"
					+ "{1}|DECEMBER) ?[\\d]{0,2}(?!\\d)");

	public static Pattern YearOrMoDayYrPattern = Pattern.compile("(?=[\\xA0 \t\r\n]*)((19[\\d]{2}|20[\\d]{2})" + "|("
			+ "([\\d]{1,2}[\\/-]{1}[\\d]{1,2}[\\/-]{1}([\\d]{2,4}" + "[\t \\s\r\n]{1}|[\\d]{4}[\t \\s\r\n]{1}))"
			+ "|[1-3]{1}\\d-(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)-[90]{1}\\d)" + ")[\\xA0 \t\r\n]*");
	// rethink if i want the two digit year pattern (need to test further and
	// |([ ,]{1,2}9[\\d]{1}[\r\n ]{1}|[ ,]{1,2}0[\\d]{1})[\r\n ]{1}
	// above captures , 00 or 99.

	public static Pattern YearOrMoDayYrPattern2 = Pattern
			.compile("(?i)(?<=[ ]{35})[\\d]{1,2}[\\/-]{1}[\\d]{1,2}[\\/-]{1}[\\d]{2,4}(?=( ?$|\r|\n))");
	// |[ \t]{3,}[\\d]{1,2}[\\/-]{1}[\\d]{1,2}[\\/-]{1}[\\d]{2,4}( ?$|[ \t]{3})
	public static Pattern MoDayYrSimplePattern = Pattern.compile("(\\d{1,2}[\\/|-]{1}\\d{1,2}[\\/|-]{1}\\d{2,4})");

	// https://www.sec.gov/Archives/edgar/data/914156/0000912057-00-036325-index.htm
	// 30-JUN-00 31-DEC-99
	// [1-3]{1}\\d-(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)-[90]{1}\\D

	public Pattern YearPatternOrMoDayYrSimplePattern = Pattern.compile(
			"[\\xA0 \t]*((19\\d{2}|20\\d{2})" + "|((\\d{1,2}[\\/|-]{1}\\d{1,2}[\\/|-]{1}(\\d{2,4}" + "))))[\\xA0 \t]*");
	// [\t \\s\r\n]{1}|\\d{4}[\t \\s\r\n]{1}

	public static Pattern YearSimple = Pattern
			.compile("199[\\d]{1}|20[0-5]{1}[\\d]{1}|\\d{1,2}[\\/-]{1}\\d{1,2}[\\/-]{1}\\d{2,4}" + "[\t \\s\r\n]{1}");

	// negative look back to prevent fifty-one or twenty seven
	public static Pattern OddBallPeriodPattern = Pattern
			.compile("[a-zA-Z]{0,5}(?i)(?s)(((?<!(twenty[- ]{1,3}|thirty[- ]{1,3}|fifty[- ]{1,3}))"
					+ "(one|two|four|five|seven|eight|fifteen|sixteen))[ ]{1,4}month[s ]"
					+ "(ending|ended|end)?|year[s]{0,1}[- ]{1,3}to[ -]{1,3}date|for the (one|two|four|five|seven|eight|fifteen|sixteen))");

	public static Pattern EndedHeadingPatternGood = Pattern.compile("([a-zA-Z]{0,5}(?is)"
			+ "((for )?(the )?(three|six|nine|twelve)[\t -]{1,4}months? ?(periods? )?([ ]{0,3}end(ed|ing))?"
			+ "|half.{1,3}year|first[ ]{1,2}half|[s]ix[\t ]{1,4}months? ?"
			+ "([ ]{0,3}end(ed|ing))?|nine[\t ]{1,4}months? ?([ ]{0,3}end(ed|ing))?|twelve[\t ]{1,4}months? ?"
			+ "([ ]{0,3}end(ed|ing))?|(three|two)(.{1,2}fiscal)?.{1,2}quarters.{1,2}end(ed|ing)|(first|second|third|fourth|1st|2nd"
			+ "|3rd|4th)[- \t]{1,3}Q[uarters]{0,7}|quarters? ?{1,3}end(ed|ing)|three quarters"
			+ "|(two|three|four|five).{1,3}years|year[s]{0,1}(?![- ]{1,3}to[ -]{1,3}date)([ ]{1,3}(ended|ending))"
			+ "|fiscal.{1,2}year(.{1,2}end(ed|ing))?|(periods?|year)[\r\t ]{1,3}(ended|ending)?"
			+ "|(3|6|9|12|three| six|nine|twelve)[- \t]{1,3}(mo[ $]{1}|mo\\.|mos\\.?|months? ?)[ ]{0,2}(end(ed|ing))?"
			+ "|(12|twelve|13|thirteen|14|fourteen|15|fifteen|sixteen|16|seventeen|17|24|twenty.{1,3}four|25|twenty.{1,3}five|26|twenty.{1,3}six"
			+ "|27|twenty.{1,3}seven|28|twenty.{1,3}eight|36|thirty.{1,3}six|37|thirty.{1,3}seven|"
			+ "38|thirty.{1,3}eight|39|thirty.{1,3}nine|40|forty|51"
			+ "|fifty.{0,2}two|52|53)[\t- ]{1,3}w.{0,2}k[s](.{0,3}end(ed|ing))?|current (qtr|quarter)"
			+ "|(thirteen|twenty.{1,3}six|(for )?the( three)( years| months)?|for the quarters?( ended)?|for[ ]{1,2}the[ ]{1,2}six( months)?"
			+ "|twelve|thirty.{1,3}nine|for.{1,3}the.{1,3}nine( months)?|for.{1,2}the.{1,2}years.{1,2}end(ed|ing)))|fifty.{0,3}(one|two|three)|for the qtr)"
			// below will find three,six,nine or twelve sandwiched
			// between tab/hard return eg: Three<tab>Six<tab>Twelve.
			+ "" +
			// "|([ ]{3}|\t)quarter( ?$|[ ]{3}| ?\t| ?[\r\n]{1})" +
			"|((?i)(?m)[\r\t]{1} ?three ?(?=([\r\t]{1}))|[\r\t]{1} ?six ?(?=([\r\t]{1}))|[\r\t]{1} ?nine ?(?=([\r\t]{1}))"
			+ "|[\r\t]{1} ?twelve ?(?=([\r\t]{1}))|[\r\t]{1} ?quarter ?(?=([\r\t]{1}))|(\t|[ ]{3})quarter(\t|[ ]{3}| ?$)"
			+ "|^ ?three|^ ?six|^ ?nine|^ ?twelve|[ \t]{2,}three ?$|[ \t]{2,}six ?$|[ \t]{2,}nine ?$|[ \t]{2,}twelve ?$|"
			+ "^ ?quarter[ ending]{0,6}|two quarters[ ending]{0,6}|[1,2,3,4]{1}.{1,3}quarter)"
			+ "|(?i)(  three(  | ?[\r]{1,})|  six(  | ?[\r]{1,})|  nine(  | ?[\r\n]{1,})|  twelve(  | ?[\r]{1,})"
			+ "|for the year" + ")");

	public static Pattern EndedTabSimplePattern = Pattern
			.compile("(?i)((ended|ending)[ ]{0,1}\t|\t[ ]{0,1}(ended|ending))");

	public static Pattern OddColumnPattern = Pattern.compile("(?i)("
			+ "percent|decrease|  US\\$  |  HK\\$  |for (the )?period|[ ]{2,}MONTHS[ ]{2,}|[\\t ]{2}\\% (of|over)|[\\t ]{2}\\(?under\\)?|"
			+ "[\t ]{2}[\\(](as )?restated|[\t ]{2}[\\(](as )?adjusted|[\t ]{2}\\(?(as )?reported|[\t ]{2}[\\(](as )?revised|"
			+ "[\t ]{2}[\\(]?pro.{1,2}forma|[\t ]{2}through[\t ]{2}|[\t ]{2}from[\t ]{2}|"
			+ "[\t ]{2}inception\\)?[\t ]{2}|[\t ]{2}\\(?Note ?\\d?\\)?|  USD  |  RMB  |"
			+ "[A-Z,a-z]{2}[\t ]{4,}inception|[\t ]{2}(as )?adjusted|[\t ]{2}(as )?previously|"
			+ "[\t ]{2}Six[\t ]{2}|[\t ]{2}Days[\t ]{2}|[\t ]{2}Months End[eding]{2,3}|"
			+ "[\t ]{2}ended[\t ]{2}|[\t ]{2}[\\(]unaudited|[\t ]{2}[\\(]audited|"
			+ "[\t ]{2}inception|(, |   )Year($|  )" + ")");
	// have to be precide w/ Year as oddball

	public static Pattern OddColumnLimitedPattern = Pattern
			.compile("(?i)([\t ]{2}through[\t ]{2}|[\t ]{2}from[\t ]{2}|[\t ]{2}Six[\t ]{2}"
					+ "|[\t ]{2}Months End[eding]{2,3}|[\t ]{2}ended|[\t ]{2}to[\t ]{1}|[\t ]{2}inception|)");

	public static Pattern patternThroughColumn = Pattern.compile("(?i)(THROUGH)");

	public static Pattern WordTabNumberTaborHardReturnPattern = Pattern.compile(
			"([A-Za-z -,;\\$\\(\\)[\\d]{4}]{5,35}" + "\t[ ]{0,2}[\\$\\(\\_\\-\\.]{0,4}[\\d]{0,3}[,\\d].{1,20})[ ]{0,3}"
					+ "((\t[ ]{0,2}[\\$\\(\\_\\-\\.]{0,4}[\\d]{0,3}[,\\d].{1,20}[ ]{0,3})|\r\n)");

	public static Pattern WordSpaceNumberSpaceorHardReturnPattern = Pattern
			.compile("([A-Za-z -,;\\$\\(\\)[\\d]{4}]{5,35}"
					+ "[\t \\s]{1,30}[\\$\\(\\_\\-\\.]{0,4}[\\d]{0,3}[,\\d].{1,20})[ ]{0,3}"
					+ "(([\t \\s]{1,30}[\\$\\(\\_\\-\\.]{0,4}[\\d]{0,3}[,\\d].{1,20}[ ]{0,3})|\r\n)");

	public static Pattern tabNumberTabOrHardReturn = Pattern
			.compile("\t.{0,1}[\\$\\(]{0,2}\\d{1}[\\d,]{1,15}\\d[\\) ]{0,2}"
					// <tab>1,234 (has to end with a #)
					+ "|\\([\\d]{1,3}\\)"
					// <tab>123
					+ "|\t.{0,1}[-â€”_]{1,3}.{0,1}\t.{0,1}[-â€”_]{1,3}.{0,1}[\t\r\n]" +
					// <tab>-<tab>-
					"|\t.{0,1}[\\$\\(]{0,2}[\\d]{0,3}\\.[\\d]{1,4}\\)?");
	// <tab>123.3456

	public static Pattern tabNumbertabNumber = Pattern.compile("\t ?[\\d\\_-â€”]{1,2} ?\t ?[\\d\\_-â€”]{1,2} ?[\t\r\n]{1}");
	// this pattern distinguishes from tabNumberTabOrHardReturn by picking up
	// instances where it is only 1 or 2 consecutive #s (eg;
	// <tab>1<tab>10<tab|hardreturn>. tabNumberTabOrHardReturn picks up (2).

	public static Pattern numberCloseParenOpenParenNumber = Pattern.compile("(?<=\\d\\))[ ]{0,2}(?=\\(\\$?\\.?\\d)");

	public static Pattern numberDollarNumber = Pattern.compile("(?<=\\d\\)?) ?(?=\\$\\(?\\d)");

	public static Pattern hardReturnNumberTabNumber = Pattern
			.compile("[\r\n]{1}(?=([[\\$]{0,1}[\\(]{0,1}[\\.]{0,1}\\d[\\)]{0,1}]{0,15} ?\t"
					+ "[[ ]{0,1}[\\$]{0,1}[\\(]{0,1}[\\.]{0,1}\\d[\\)]{0,1}]{0,15}))");

	public static Pattern hardReturnNumberTabNumber2 = Pattern.compile(
			"\r\n(?=([\\(\\$\\.]{0,3}[,\\d]{1,15}[\\)]{0,1})[\t\r\n ]([\\(\\$\\.]{0,3}[,\\d]{1,15}[\\)]{0,1}))");
	// this picks up where it is also a comma and separator b/w #s is also space

	public static Pattern tabNumberNoYear = Pattern
			.compile("\t ?([\\$\\(]{0,2}[\\d]{3}[ \t\r\n]|" + "[\\$\\(]{1,2}[\\d,]{2,3}" + "|[\\d]{1,2},)");
	// can be: ###<tab|hardreturn>

	public static Pattern spaceNumberNoYear = Pattern
			.compile("\t ?([\\$\\(]{0,2}[\\d]{3}[ \t\r\n]|" + "[\\$\\(]{1,2}[\\d,]{2,3}" + "|[\\d]{1,2},)");

	public static Pattern DecimalPattern = Pattern.compile("(?i)[ \t]*((" + "(in |\\$|\\()thousands?"
			+ "|[\\( ]{0,1}000[\\' \\)]{0,2}s|in millions?|inmillions|million[s] of|\\(.{1,2}000 omit"
			+ "|in billions?|billion[s] of)|(thousands?|millions?|billions?).{1,3}except)[ \t]*");

	public Pattern ColumnHeadingDecimalPattern = Pattern.compile(EndedHeadingPattern.pattern() + "|"
			+ MonthPattern.pattern() + "|" + YearOrMoDayYrPattern.pattern() + DecimalPattern.pattern() + "|");

	public static Pattern BottomTagsPattern = Pattern.compile("(?is)(<ARTICLE>.{1,8000}</TABLE>)");

	public static String EmptyLineRegex = "^([a-zA-Z:\\p{Blank}\\(\\)'\\-\"]+)$";

	// lines having only chars/space/tab/(/)/'/" etc. - lines don't have any
	// amount, like group headings

	public static Pattern TOCTableNamePattern = Pattern.compile("(?is)(" + "(" + "(.{1,350}balance.{0,3}sheet)|"
			+ "statement.{1,6}(financial)?(.{0,3}condition|.{0,3}position))" + ".{1,350}(cash.{0,3}flow).{1,350}"
			+ "|(.{1,350}cash.{0,3}flow)" + ".{1,350}("
			+ "(balance.{0,3}sheet)|statement{1,6}(financial)?(.{0,3}condition|.{0,3}position))" + ".{1,350}" + ")");

	public static Pattern Period3MonthsPattern = Pattern.compile(
			"[a-zA-Z]{0,5}(?i)(?s)((three[- ]{1,4}(and.{1,2})?((six|nine|twelve).{1,2})?month([ ]{0,3}(periods?[ ]{0,3})?end(ed|ing))?"
					+ "|(first|second|third|fourth|1st|2nd|3rd|4th)[ ]{1,3}Q[uarters]{0,7}|[\r\t]{1,2} ?quarters? ?{1,3}end(ed|ing)"
					+ "|(3|three)[- ]{1,3}(mo\\.|mos\\.?|months?)[ ]{0,2}(end(ed|ing))?|(12|twelve|13|thirteen|Second Quarter \\(?12 weeks\\)? ended|"
					+ "14|fourteen)[- ]{1,3}w.{0,2}k(.{0,3}end(ed|ing))?|(thirteen|for the three(?! years)|for the qtr"
					+ "|[\r\t ]{0,2}(?<!-)Three[\r\t ]{1,2})))" +
					// will get Three sandwiched between hard reeturn or where
					// starts line==>>
					"|(?i)(?m)([\r\n\t]{1}three(?=([\r\n\t]{1}))|^( ?(?<!-)three ?)|^( ?quarter)|[\r\n\t]{1} ?twelve ?(?=([\r\n\t]{1}))"
					+ "|[\r\n\t]{1} ?for the fiscal quarters? ?(?=([\r\n\t]{1}))|^( ?for the quarter)|1.{1,3}quarter)");
	// twelve by itself is probably twelve months but could be twelve weeks.
	// "|((?i)(?m)[\r\t]{1}three(?=([\r\t]{1}))|[\r\t]{1}six(?=([\r\t]{1}))|[\r\t]{1}nine(?=([\r\t]{1}))|[\r\t]{1}twelve(?=([\r\t]{1}))"
	// +
	// "|^three|^six|^nine|^twelve

	public static Pattern Period6MonthsPattern = Pattern.compile(
			"[a-zA-Z]{0,5}(?i)(?s)([s]ix[- ]{1,4}month([ ]{0,3}(periods?[ ]{0,3})?end(ed|ing))?|half.{1,3}year|first[ ]{1,2}half|(6)[- ]{1,3}(mo\\.|"
					+ "mos\\.?|months)[ ]{0,2}(end(ed|ing))?"
					+ "|(24|twenty.{1,3}four|25|twenty.{1,3}five|26|twenty.{1,3}six|27|twenty.{1,3}seven|28|twenty.{1,3}eight)[- ]{1,3}"
					+ "w.{0,2}k(.{0,3}end(ed|ing))?|(twenty.{1,3}six|for[ ]{1,2}the[ ]{1,2}six))" +
					// will get Three sandwiched between hard reeturn or where
					// starts line==>>
					"|Year\\-to\\-date \\(?28 weeks?\\)? Ended|(?i)(?m)([\r\t]{1,} ?six ?(?=([\r\t]{1,}))|[\r\t]{1}six|^( ?six ?)(?=([\r\t]{1}))|two.{1,3}quarters|2.{1,3}quarters)");

	public static Pattern Period9MonthsPattern = Pattern
			.compile("[a-zA-Z]{0,5}(?i)(?s)(nine[- ]{1,4}month([ ]{0,3}(periods?[ ]{0,3})?end(ed|ing))?|(9)[- ]{1,3}"
					+ "(mo\\.|mos\\.?|months)[ ]{0,2}(end(ed|ing))?|"
					+ "(36|thirty.{1,3}six|37|thirty.{1,3}seven|38|thirty.{1,3}eight|39|thirty.{1,3}nine|40|forty|for.{1,3}the.{1,3}nine)"
					+ "[- ]{1,3}w.{0,2}k(.{0,3}end(ed|ing))?|(thirty.{1,3}nine|three.{1,3}quarters))" +
					// will get Three sandwiched between hard reeturn or where
					// starts line==>>
					"|(?i)(?m)([\r\t]{1} ?nine ?(?=([\r\t]{1}))|^( ?nine)|for the nine[ months]|3.{1,3}quarters)");

	// ?! negative lookahead of to-date (don't want year-to-date
	public static Pattern Period12MonthsPattern = Pattern
			.compile("[a-zA-Z]{0,5}(?i)(?s)(twelve[- ]{1,4}month([ ]{0,3}(periods?[ ]{0,3})?end(ed|ing))?"
					+ "|(two|three|four|five).{1,3}year[s]{0,1}(?![- ]{1,3}to[ -]{1,3}date)([ ]{1,3}(period )?(ended|ending))"
					+ "|(two|three|four|five).{1,3}fiscal.{1,2}year(.{1,2}end(ed|ing))?"
					+ "|(two|three|four|five).{1,3}years?[\r\t ]{1,3}(ended|ending)?"
					+ "|(12)[- ]{1,3}(mo[\\. ]|mos[\\. ]|months)[ ]{0,2}(end(ed|ing))?|(51|fifty.{0,2}two|52|four.{1,3}|quarters|"
					+ "fifty.{0,2}three|53)[- ]{1,3}w.{0,2}k(.{0,3}end(ed|ing))?|fifty.{0,3}(one|two|three))" +
					// will get Three sandwiched between hard reeturn or where
					// starts line==>>
					"|(?i)(?m)([\r\t]{1} ?twelve ?(?=([\r\t]{1}))|^( ?twelve)|4.{1,3}quarters)");

	public static Pattern EndedHeadingPattern = Pattern.compile(EndedHeadingPatternGood.pattern() + "|"
			+ OddBallPeriodPattern.pattern() + "|" + Period12MonthsPattern.pattern());

	// public static Pattern EndedHeadingTablesentencePattern = Pattern
	// .compile(EndedHeadingPatternGood.pattern() + "|" +
	// OddBallPeriodPattern.pattern() + "|" +
	// Period3MonthsPattern.pattern() + "|" + Period6MonthsPattern.pattern()
	// +"|" + Period9MonthsPattern.pattern() + "|" +
	// Period12MonthsPattern.pattern());

	// make a series of tablesentence ended parsers based on pattern - e.g., if:
	// PPMYY: FOR THE THREE AND SIX MONTHS ENDED DECEMBER 31, 2001 AND 2002.
	// Based on that pattern call a loop that then pairs each P w/ each Y and M
	// for 4 cols

	// For each of the years in the three-year period
	public Pattern EndedOtherTablesentence = Pattern.compile("(?i)SIX AND|THE THREE[ -]{1,3}YEAR PERIOD ENDED");
	public Pattern EndedHeadingTablesentencePattern = Pattern.compile(EndedHeadingPatternGood.pattern() + "|"
			+ OddBallPeriodPattern.pattern() + "|" + Period12MonthsPattern.pattern() + "|"
			+ Period9MonthsPattern.pattern() + "|" + Period6MonthsPattern.pattern() + "|"
			+ EndedOtherTablesentence.pattern() + "|" + Period3MonthsPattern.pattern());

	public Pattern periodsAll = Pattern.compile(Period3MonthsPattern.pattern() + "|" + Period6MonthsPattern.pattern()
			+ "|" + Period9MonthsPattern.pattern() + "|" + Period12MonthsPattern.pattern());

	public static Pattern ColumnHeadingPattern = Pattern
			.compile(EndedHeadingPattern.pattern() + "|" + MonthPattern.pattern() + "|" + patternMonthDayYY.pattern()
					+ "|" + YearOrMoDayYrPattern.pattern() + "|" + patternThroughColumn.pattern());

	public Pattern AllColumnHeadingPattern = Pattern
			.compile(ColumnHeadingPattern.pattern() + "|" + OddColumnPattern.pattern());

	public static Pattern YearPrecededByNumberFalsePositive = Pattern.compile("" + "((" + // begin
																							// can't
																							// be
																							// preceded
																							// by
			"[\\d,]{4}[\\d]{3}[\\); -]{1,4})" +
			// can't be preceded by: ###,###[) -;]
			"|[\\$\\( ]{1,3}[\\d]{1,3}[\\); -]{1,4}"
			// can't be preceded by: [$( ]#to###[); -]
			+ ")"// end of can't be preceded by
			+ "[\t \r\n](199[\\d]{1}|20[0-5]{1}[\\d]{1})");
	// yr replcd"[\\d]{4}"

	public static Pattern YearFollowedByNumberFalsePositive = Pattern
			.compile("([\t \r\n](199[\\d]{1}|20[0-5]{1}[\\d]{1}))" +
	// yr replaced "[\\d]{4}"
					"((" + // begin can't be followed by
					"[-:\\(\\$ ]{1,5}[\\.\\d]{3})" +
					// can't be followed by:[ $)-]#to##|.###]
					")"// end of can't be followed by
	);
	// yr 19\\d{2}|20\\d{2} (199[\\d]{1}|20[0-5]{1}[\\d]{1})

	public static Pattern YearFalsePositives = Pattern
			.compile(YearPrecededByNumberFalsePositive.pattern() + "|" + YearFollowedByNumberFalsePositive.pattern());

	public static Pattern ShortLineFollowedByShortLine = Pattern.compile("(?<=\r\n[ a-z;:,\\']{18,40})\r\n");

	public static Pattern oneHyphenGroups = Pattern.compile("\r\n[-]{1,10} ?\r\n");

	public static Pattern twoHyphenGroups = Pattern.compile("\r\n[-]{1,10} ?\t ?[-]{1,10} ?\r\n");

	public static Pattern threeHyphenGroups = Pattern.compile("\r\n[-]{1,10} ?\t ?[-]{1,10} ?\t ?[-]{1,10} ?\r\n");

	public static Pattern fourHyphenGroups = Pattern
			.compile("\r\n[-]{1,10} ?\t ?[-]{1,10} ?\t ?[-]{1,10} ?\t ?[-]{1,10} ?\r\n");

	public static Pattern DevelopmentStageCompanyPattern = Pattern
			.compile("(?i)(inception|percent|\\%|change|development stage)");

	public static Pattern footNotePattern = Pattern.compile("(?<=[\\d]{2,4}[ ]{1}|[A-Za-z\\) ]{4})\\([\\da-z]{1}\\)");

	// methods would need to be non-static if non-static variables are used. but
	// can be non-static method and use static variables.
	public static String insertMissingTab(String tableText, Pattern pattern) {
		tableText = tableText.replaceAll("[_]{1,10}(?=([\\-\\d\\(\\$]{1}))", " ");
		// <<=replaces rare instances of ___95 with 95 (underscores are replaced
		// with a space to keep index same.

		String[] lines = tableText.split("\r\n");
		tableText = "";
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].indexOf("\t") > 0) {
				String[] lineCols = lines[i].split("\t");
				String dataLine = lines[i].substring(lines[i].indexOf("\t"));
				dataLine = pattern.matcher(dataLine).replaceAll("\t");
				// System.Xut.println("dataLine:" + dataLine);
				lines[i] = lineCols[0] + "\t" + dataLine + "\r\n";
				// System.Xut.println("lines[i]:" + lines[i]);
				tableText = tableText + lines[i];
			} else
				tableText = tableText + lines[i] + "\r\n";
		}
		return tableText;
	}

	// we cretae a map of:
	// #OfCols = RowCount
	public static Map<Integer, Integer> getColumnCountMapForAllRows(String tableText) {
		Map<Integer, Integer> map = new TreeMap<Integer, Integer>();
		// maps columns and rows. Key is 1st integer and its count the 2nd

		String[] lines = tableText.split("\r\n");
		int tabCount;
		for (String line : lines) {
			tabCount = StringUtils.countMatches(line, "\t");
			if (tabCount == 0 || line.contains("?")) // RowNm or line has a ?
				continue;
			if (map.containsKey(tabCount))
				map.put(tabCount, map.get(tabCount) + 1);
			// maps: puts for key (tabCount is key) the value of 1 plus prior
			// value
			else
				map.put(tabCount, 1);
		}
		// when map is finished looking at a 5 column table for example it will
		// look like this (assuming 10 rows):
		// 5=8
		// 6=1
		// 4=1
		// the first integer represents the #ofColumns and the 2nd is the # of
		// rows it occured on with that many columns.

		return map;
	}

	public int[] getDataColumnCount(Map<Integer, Integer> colCount2RowsMap) {
		int columnsInRows = -1, rowsWithMaximumColumnsRows = -1;
		int totalRows = 0;

		for (Integer cols : colCount2RowsMap.keySet()) {
			// for each key, col=key
			if (rowsWithMaximumColumnsRows < colCount2RowsMap.get(cols)) {
				// get value for key
				columnsInRows = cols;
				rowsWithMaximumColumnsRows = colCount2RowsMap.get(cols);
			}
			totalRows += colCount2RowsMap.get(cols);
		}

		// System.Xut.println("columns: " + columnsInRows
		// + "| #ofRowsWithMaxColumns: " + rowsWithMaximumColumnsRows +
		// "| totalRows: " + totalRows);
		return new int[] { columnsInRows, rowsWithMaximumColumnsRows, totalRows };
	}

	public String getTableNameShort(String tableName) {

		String tableNameShort = "";

		if (tableName.toLowerCase().contains("balance") || (tableName.toLowerCase().contains("financial")
				&& (tableName.toLowerCase().contains("condition") || tableName.toLowerCase().contains("position")))
				|| (tableName.toLowerCase().contains("of condition") && tableName.toLowerCase().contains("statement"))
				|| ((tableName.toLowerCase().contains("asset") || tableName.toLowerCase().contains("liabilities"))
						&& !tableName.toLowerCase().contains("change"))) {
			tableNameShort = "bs";
			return tableNameShort;
		}

		if (tableName.toLowerCase().contains("cash") || tableName.toLowerCase().contains("flows")
				|| tableName.toLowerCase().contains("change")) {
			tableNameShort = "cf";
			return tableNameShort;
			// System.Xut.println("cf tablename =" + tableName);
		}
		if (tableName.toLowerCase().contains("incom") || tableName.toLowerCase().contains("operati")
				|| tableName.toLowerCase().contains("earning") || tableName.toLowerCase().contains("loss")
				|| tableName.toLowerCase().contains("revenue")) {
			tableNameShort = "is";
			return tableNameShort;
		}
		if ((tableName.toLowerCase().contains("equit") || tableName.toLowerCase().contains("holder"))
				&& !tableName.toLowerCase().contains("liabilities")) {
			tableNameShort = "se";
			return tableNameShort;
		}
		return tableNameShort;
	}

	public void getTocTableNames(String text) throws FileNotFoundException, SQLException {
		// System.Xut.println("toc sample text:: " + text.substring(0, 300));

		Matcher matchOfToc = TOCTableNamePattern.matcher(text);
		String tocBlock = "";
		String toc = "";

		while (matchOfToc.find()) {
			tocBlock = matchOfToc.group();
			Matcher matchTableName = TableNamePattern.matcher(tocBlock);
			int rowToc = 0;
			while (matchTableName.find()) {
				rowToc++;
				toc = (toc + acc + "||" + rowToc + "||" + matchTableName.group() + "\r\n");
			}
		}
	}

	public void getGenericTableText(String pageText, String fileType, String acceptedDate, String acc)
			throws IOException, ParseException, SQLException {
		// fileType (bsnWire?),

		NLP nlp = new NLP();
		if (acceptedDate != null)
			fileDate = acceptedDate;
		tableCount = 0;
		int endStartPatternIdx = 0, startIdx = 0;
		// below gets min and hours of acceptedDate for bsnWire html file

		if (fileType.equalsIgnoreCase("bsnWire") && acceptedDate == null) {
			try {
				// System.xut.println("I have acceptedDate?: " + acceptedDate);
				acceptedDate = getAcceptedDateForBusinessWire(pageText, acceptedDate);
				// System.Xut.println("acceptedDate from pageText bsnWire"
				// + acceptedDate);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// System.Xut.println("bsnWire aDate::" + acceptedDate);
		}

		// right before here - if file is ascii file (and not html) - then we
		// may want to mark location of CHs based on line length
		isHtml = false;
		if (pageText.indexOf("</tr") >= 0 || pageText.indexOf("</TR") >= 0) {
			isHtml = true;
			// System.xut.println("isHtml=" + isHtml);
		}

		// PrintWriter tempPw97 = new PrintWriter(new File("c:/backtest/"
		// + getTableNamePrefixYearQtr(fileDate) + "temp97.txt"));
		// tempPw97.append(pageText);
		// tempPw97.close();

		String text = nlp.removeExtraBlanks(stripHtmlTags(pageText));
		if (text.length() < 1) {
			return;
		}
		// PrintWriter tempPw98 = new PrintWriter(new File("c:/backtest/"
		// + getTableNamePrefixYearQtr(fileDate) + "temp98a.txt"));
		// tempPw98.append(text);
		// tempPw98.close();

		// System.xut.println("stripped html");
		if (formType == null)
			formType = "xx";
		// if (formType.contains("10-K")) {
		// getTocTableNames(text.substring(Math.max(1, text.length() - 10000),
		// text.length()));
		// // gets end for just 10-K
		// }
		// if (formType.contains("10-")) {
		// getTocTableNames(text.substring(0, Math.min(10000, text.length())));
		// // System.Xut.println("getting TOC::");
		// // gets all beginning 10-
		// }

		// System.xut.println("fileDate=" + fileDate);
		// PrintWriter tempPw = new PrintWriter(new File("c:/backtest/"
		// + getTableNamePrefixYearQtr(fileDate) + "temp99.txt"));
		// tempPw.append(text);
		// tempPw.close();

		/*
		 * TODO: ADD AN IF CONDITION IF COMPLETE.TXT OR HAVE 2 PATTERNS CHECK EACH
		 * PARSING: COMPANY CONFORMED NAME: KENTUCKY UTILITIES CO STANDARD INDUSTRIAL
		 * CLASSIFICATION: ELECTRIC SERVICES [4911] FILED AS OF DATE: 20051114
		 * <ACCEPTANCE-DATETIME>20051114095807 FISCAL YEAR END: 1229 FORM TYPE: 10-Q/A
		 */

		Pattern SICCodePattern = Pattern.compile("(?<=(<ASSIGNED-SIC>)).*\r\n");
		// FyePattern picks up both <FISCAL-YEAR-END>1231 and FISCAL YEAR
		// END:1231. (.NC AND complete.txt file formats)
		Pattern FyePattern = Pattern.compile("(?i)(?<=(([<]{0,1}FISCAL[- ]{1}YEAR[- ]{1}END)).{1,15})[\\d]{4}");

		Pattern PeriodOfReportPattern = Pattern.compile("(?<=(<PERIOD>)).*\r\n");

		Matcher matcherSicCode = SICCodePattern.matcher(headerPortionOfFile);
		while (matcherSicCode.find() && !fileType.equalsIgnoreCase("bsnWire")) {
			// && sicCode == null
			sicCode = matcherSicCode.group().trim();
		}

		Matcher matcherPeriodOfReport = PeriodOfReportPattern.matcher(headerPortionOfFile);
		String day = "";
		// high error rate in source field of periodOfReport. Don't parse
		while (matcherPeriodOfReport.find() && !fileType.equalsIgnoreCase("bsnWire")) {
			// && sicCode == null
			periodOfReport = matcherPeriodOfReport.group().trim();
			qtrEnd = periodOfReport.substring(4);
			if (matcherPeriodOfReport.group().length() > 7)
				day = periodOfReport.substring(6);
			// System.Xut.println("periodOfReport from nc file: " +
			// periodOfReport
			// + " qtrEnd: " + qtrEnd + " day: " + day);
		}

		Matcher matcherFye = FyePattern.matcher(headerPortionOfFile);
		// System.Xut.println("filingDetails matcherFye applied to - 1st 200 of
		// 5000:="+headerPortionOfFile.substring(0,200));

		while (matcherFye.find() && !fileType.equalsIgnoreCase("bsnWire")) {
			// && fye == null
			fye = matcherFye.group().trim().replaceAll("([ ]{1,}|[\t]{1,})", "");
			qtrEnd = fye;
			// System.Xut.println("matcherFye="+fye);
			if (matcherFye.group().length() > 3)
				day = fye.substring(2);
			// System.xut
			// .println("fyePattern matcher (txt and .nc file formats) fye="
			// + fye
			// + " qtrEnd (should be same as fye)="
			// + qtrEnd
			// + " day=" + day);
		}

		File localPath = new File("");
		if (acceptedDate != null) {
			localPath = new File(getFolderForAcceptedDate(acceptedDate));
			// System.xut.println("getFolderForAcceptedDate(acceptedDate)="
			// + localPath);
			// } else if (fileDate == null) {
			// System.Xut.println("fileDateLocal2:" + fileDateLocal);
			// localPath = new File(
			// getFolderForFileDateNoHyphensInDate(fileDateLocal));
			// fileDate = fileDateLocal;
		} else {
			// System.Xut.println("fileDate::" + fileDate);
			localPath = new File(getFolderForFileDateNoHyphensInDate(fileDate));
		}

		Utils.createFoldersIfReqd(localPath + "/csv/");
		String fileName = localPath + "/csv/" + acc + ".csv";
		// System.Xut.println("localPath - path::" + fileName);

		// PrintWriter tablePw = new PrintWriter(fileName);
		StringBuffer sb = new StringBuffer();

		// text = ShortLineFollowedByShortLine.matcher(text).replaceAll(" ");
		if (cik.contains("881788")) {
			text = text.replaceAll("ICON CASH FLOW|ICON Cash Flow", "ICON");

		}

		String startGrp = null, coNameParsed = null, tableName = null, tableNameShort = null;
		// prevLine = null, previousLine = null,

		@SuppressWarnings("unused")
		int endTableTextIdx = 0, inception = 0, oddColumn = 0, endTblNmIdx = 0, subtractFromStartIdx = 0, coMatch = 0;
		boolean tableFound = false;
		while (true) {
			tableFound = false;
			System.out.print("accno: " + acc + "\r\n");
			// first time thru startIdx=0 - so false.
			// loops stops when end of text length
			// System.Xut.println(":startidx1:" + startIdx);
			if (startIdx > 0 && startIdx + 301 < text.length() && tableCount < 30)
				text = text.substring(startIdx);
			// if true it 'breaks' and comes out of while loop
			else if (startIdx + 301 >= text.length() || tableCount > 29)
				break;
			Matcher matcher = TableNamePattern.matcher(text);
			// System.Xut.println("tableNamePattern applied to this text||" +
			// text
			// + "||text");
			// make it null only at instance of tableNamePattern then if
			// getSentence is called twice variable value will remain as long as
			// 2nd sentence doesn't have another match
			sentenceEndedStr = null;
			sentenceMonthStr = null;
			sentenceYearStr = null;

			// Matcher matcher = StartPattern.matcher(text);
			// System.Xut.println(":startIdx2a:" + startIdx);

			if (matcher.find() && (matcher.start() + 500 < text.length())) {
				// System.Xut.println(":startIdx2b:" + startIdx);
				// +500 is minimum size of table. need to not cause endless loop
				// after 1st match found - startIdx=[i] and break
				for (int i = 1; i <= matcher.groupCount(); i++) {
					if (matcher.start(i) >= 0) {
						startIdx = matcher.start(i);
						// String priorToStartGrp = text.substring(Math.max(0,
						// startIdx-250),startIdx);
						// System.Xut.println("priorToStartGrp="+priorToStartGrp);
						endTblNmIdx = matcher.end(i);
						tableName = text.substring(startIdx, endTblNmIdx).replaceAll("\r\n|[ ]{2,}| ?\t", " ");
						int z = (endTblNmIdx - startIdx) - tableName.length();
						// "[\r\n]| [ ]{2,}|\t", " ");
						// System.xut.println("tableName1: " + tableName);
						endStartPatternIdx = Math.min(matcher.end(i) + 475, text.length());
						tableNameShort = getTableNameShort(tableName);
						// endStartPatternIdx needs to be same as distance in
						// toomanytables. Start after tablename
						startGrp = text.substring(matcher.end(i) - z, Math.min(text.length(), endStartPatternIdx));
						int startGrpLengthBeforeReplacmentIdx = startGrp.length();

						// System.Xut.println("startGrp1st:" + startGrp +
						// "|End");

						startGrp = startGrp.replaceAll("l99", "199").replaceAll("-J", " J").replaceAll("-F", " F")
								.replaceAll("-M", " M").replaceAll("-A", " A").replaceAll("-M", " M")
								.replaceAll("-S", " S").replaceAll("-O", " O").replaceAll("-N", " N")
								.replaceAll("-D", " D").replaceAll("-(?i)week", " week").replaceAll("-(?i)m", " m")
								.replaceAll("2 0 0 ", "200").replaceAll("1 9 9 ", "199")
								.replaceAll("Septembe ", "September").replaceAll("December r", "December").toUpperCase()
								.replaceAll("JANAURY", "JANUARY");

						// System.Xut.println("startGrp dec/sep: " + startGrp);

						// TODO: move below to separate generic method. Combine
						// with above

						if (startGrp.contains("/") || startGrp.contains("-")) {
							Pattern slashHyphenYY1990s = Pattern.compile(
									"(?<=[\\d]{1,2}[\\/-]{1}[\\d]{1,2})[\\/-]{1}" + "(?=(9[\\d]{1})[ \t\r\n]{1})");

							Pattern slashHyphenYY2000s = Pattern.compile(
									"(?<=[\\d]{1,2}[\\/-]{1}[\\d]{1,2})[\\/-]{1}" + "(?=(0[\\d]{1})[ \t\r\n]{1})");

							Pattern slashHyphenYYYY1990s = Pattern.compile(
									"(?<=[\\d]{1,2}[\\/-]{1}[\\d]{1,2})[\\/-]{1}" + "(?=(19[\\d]{2})[ \t\r\n]{1})");

							Pattern slashHyphenYYYY2000s = Pattern.compile(
									"(?<=[\\d]{1,2}[\\/-]{1}[\\d]{1,2})[\\/-]{1}" + "(?=(20[\\d]{2})[ \t\r\n]{1})");

							Pattern slashHyphenYY00s = Pattern.compile("([\\/-](?=[0]{1}\\d[\t\r\n]))");
							// replace hyphen with " 20"

							Pattern slashHyphenYY90s = Pattern.compile("([\\/-](?=[9]{1}\\d[\t\r\n]))");
							// replace hyphen with " 19"

							Pattern mo12SlashHyphen = Pattern.compile("(?<=[ \r\n\t]{1})12[\\/\\-]{1}(?=[\\d]{1,2})");
							Pattern mo11SlashHyphen = Pattern.compile("(?<=[ \r\n\t]{1})11[\\/\\-]{1}(?=[\\d]{1,2})");
							Pattern mo10SlashHyphen = Pattern.compile("(?<=[ \r\n\t]{1})10[\\/\\-]{1}(?=[\\d]{1,2})");
							Pattern mo9SlashHyphen = Pattern
									.compile("(?<=[ \r\n\t]{1})(9|09)[\\/\\-]{1}(?=[\\d]{1,2})");
							Pattern mo8SlashHyphen = Pattern
									.compile("(?<=[ \r\n\t]{1})(8|08)[\\/\\-]{1}(?=[\\d]{1,2})");
							Pattern mo7SlashHyphen = Pattern
									.compile("(?<=[ \r\n\t]{1})(7|07)[\\/\\-]{1}(?=[\\d]{1,2})");
							Pattern mo6SlashHyphen = Pattern.compile("(?<=[ \r\n\t]{1})(6|06)[\\/\\-]{1}(?=[\\d]{2})");
							Pattern mo5SlashHyphen = Pattern
									.compile("(?<=[ \r\n\t]{1})(5|05)[\\/\\-]{1}(?=[\\d]{1,2})");
							Pattern mo4SlashHyphen = Pattern
									.compile("(?<=[ \r\n\t]{1})(4|04)[\\/\\-]{1}(?=[\\d]{1,2})");
							Pattern mo3SlashHyphen = Pattern
									.compile("(?<=[ \r\n\t]{1})(3|03)[\\/\\-]{1}(?=[\\d]{1,2})");
							Pattern mo2SlashHyphen = Pattern
									.compile("(?<=[ \t\r\n]{1})(02|2)[\\/\\-]{1}(?=[\\d]{1,2})");
							Pattern mo1SlashHyphen = Pattern
									.compile("(?<=[ \t\r\n]{1})(01|1)[\\/\\-]{1}(?=[\\d]{1,2})");

							startGrp = slashHyphenYYYY1990s.matcher(startGrp).replaceAll(" ");

							// <tab> b/c pattern match to find end limitStart
							// pivot off \t

							startGrp = slashHyphenYYYY2000s.matcher(startGrp).replaceAll(" ");

							// slashHyphenYYYY must go before below (YY).

							startGrp = slashHyphenYY1990s.matcher(startGrp).replaceAll(" 19");
							startGrp = slashHyphenYY2000s.matcher(startGrp).replaceAll(" 20");

							startGrp = slashHyphenYY00s.matcher(startGrp).replaceAll(" 20");
							// replace hyphen with " 20"

							startGrp = slashHyphenYY90s.matcher(startGrp).replaceAll(" 19");
							// replace hyphen with " 19"

							startGrp = mo12SlashHyphen.matcher(startGrp).replaceAll(" December ");
							startGrp = mo11SlashHyphen.matcher(startGrp).replaceAll(" November ");
							startGrp = mo10SlashHyphen.matcher(startGrp).replaceAll(" October ");
							startGrp = mo9SlashHyphen.matcher(startGrp).replaceAll(" September ");
							startGrp = mo8SlashHyphen.matcher(startGrp).replaceAll(" August ");
							startGrp = mo7SlashHyphen.matcher(startGrp).replaceAll(" July ");
							startGrp = mo6SlashHyphen.matcher(startGrp).replaceAll(" June ");
							startGrp = mo5SlashHyphen.matcher(startGrp).replaceAll(" May ");
							startGrp = mo4SlashHyphen.matcher(startGrp).replaceAll(" April ");
							startGrp = mo3SlashHyphen.matcher(startGrp).replaceAll(" March ");
							startGrp = mo2SlashHyphen.matcher(startGrp).replaceAll(" February ");
							startGrp = mo1SlashHyphen.matcher(startGrp).replaceAll(" January ");
						}
						companyName = companyName.replaceAll("[\r\n]", "");

						// System.Xut.println("startGrp2nd=" + startGrp);

						int startGrpLengthAfterReplacmentIdx = startGrp.length();
						subtractFromStartIdx = startGrpLengthAfterReplacmentIdx - startGrpLengthBeforeReplacmentIdx;
						// System.Xut
						// .println("change in startGrp.length after
						// replacments: "
						// + subtractFromStartIdx);

						if (fileType.equalsIgnoreCase("bsnWire")) {
							coNameParsed = getPreviousLines(text, startIdx, 1).replaceAll("[\r\n]", "");
						}
						String startGrp2 = "";
						if (!fileType.equalsIgnoreCase("bsnWire")) {
							int endStart2 = Math.min(matcher.end(i) + 250, text.length());
							startGrp2 = text.substring(Math.max(startIdx - 90, 0), Math.min(text.length(), endStart2));
							coNameParsed = nlp.getCompanyNameConfirmed(startGrp2,
									companyName.replaceAll("INC.|CORP.|INCORPORATED|CORPORATION|L.P.|L.L.C.", "")
											.replaceAll("INC|CORP|LLC|[\r\n]", ""))
									.replaceAll("[\r\n]", "");
						}

						Matcher matchMo = MonthPattern.matcher(startGrp);
						Matcher matchYr = YearOrMoDayYrPattern.matcher(startGrp);
						Matcher matchInception = DevelopmentStageCompanyPattern.matcher(startGrp2);
						Matcher matchOddColumn = OddColumnPattern.matcher(startGrp2);

						Matcher matchLineEnding = WordSpaceNumberSpaceorHardReturnPattern.matcher(startGrp);
						inception = 0;
						oddColumn = 0;
						if (matchMo.find() || matchYr.find() || matchLineEnding.find()) {
							tableFound = true;
							tableCount++;
							if (matchInception.find())
								inception = 1;
							if (matchOddColumn.find())
								oddColumn = 1;
							// finds match - and its out of loop
							break;
						}
					}
				}
				// System.Xut.println("STARTGRP 2ND::"+startGrp+
				// " tablename is still:::"+tableName);
				if (!tableFound) {
					startIdx = Math.min(text.length(), endTblNmIdx);
					// System.xut.println("table not found!!! at index="
					// + startIdx);
					continue;
				}

				// if > 2 tabs or <tab>### it is not a TOC tableNm. If it is we
				// then go to end of first tblnm instance in loop

				// System.Xut.println("startGrp1:" + startGrp);
				String[] startGrpRows = startGrp.split("\r\n");
				int count = 0;
				int end = 0;
				for (int s = 0; s < startGrpRows.length; s++) {
					count++;
					if (startGrpRows[s].toLowerCase().contains("notes to")
							&& startGrpRows[s].toLowerCase().contains("financial ") && count > 2) {
						// notes to financial statements but be at least 1 row
						// below tablename.
						end = 0;

						break;
					}

					Matcher mTabNumber = spaceNumberNoYear.matcher(startGrpRows[s]);
					if (count < 14 && mTabNumber.find()) {
						end = 1;
						// System.Xut.println("mTabNumber: " +
						// mTabNumber.group()
						// + " end=" + end);
						// System.Xut
						// .println("if it finds
						// tabNumberNoYear\rtabNumberNoYear prior to line 15 it
						// is parsed"
						// + count);
						break;
					}
				}
				// System.Xut.println("tab found if end=1. end==" + end);

				if (end != 1) {
					startIdx = Math.min(text.length(), endTblNmIdx);
					continue;
					// goes to the next iteration/step in the loop
				}

				int startGrpLengthBeforeReplacmentIdx = startGrp.length();
				@SuppressWarnings("unused")
				String dataColsLine = "";
				int dataCols = 0;

				startGrp = startGrp.replaceAll("[\t ]{2,}", "\t").replaceAll("\t[ ]{1,}|[ ]{1,}\t", "\t")
						.replaceAll("[\t]{2,}", "\t");

				// System.Xut.println("startGrp3rd=" + startGrp);

				String[] startGrpRow = startGrp.split("\r\n");
				for (int a = 0; a < startGrpRow.length; a++) {
					if (a < 4) {
						// System.Xut.println("first 4 rows of startGrp: "
						// + startGrpRow[a]);
					}
					if (startGrpRow[a].split("\t").length > dataCols) {
						dataCols = startGrpRow[a].split("\t").length;
						dataColsLine = startGrpRow[a];
						// finds # of dataCols set to line w/ max # of tabs
					}
				}

				// System.xut.println("number of data columns: " + dataCols
				// + ": dataColsLine: " + dataColsLine);

				if (dataCols == 0) {
					continue;
				}

				dataCols = dataCols - 1;

				// TODO: include in start group method where all other replaces
				// are
				startGrp = footNotePattern.matcher(startGrp).replaceAll("");
				int startGrpLengthAfterReplacmentIdx = startGrp.length();
				subtractFromStartIdx = subtractFromStartIdx
						+ (startGrpLengthAfterReplacmentIdx - startGrpLengthBeforeReplacmentIdx);
				// above finds all(\\d) and gets rid of them.

				startGrp = limitStartGrp(startGrp, dataCols);
				// System.Xut.println("limit startgrp="+startGrp);
				String[] startGrpRows2 = startGrp.split("\r\n");
				// System.xut.println("LimitstartGrp|" + startGrp + "|END");

				if (startGrp.length() < 5 || startGrpRows2.length > 9) {
					// < 5 has means nothing - so move to end of tblnm pattern
					// System.Xut
					// .println("ended here b/c startGrp.length() < 5 ||
					// startGrpRows2.length > 9");
					// System.Xut.println("startGrp.length()=" +
					// startGrp.length()
					// + " startGrpRows2.length=" + startGrpRows2.length);
					startIdx = endTblNmIdx;
					continue;
				}
				// System.Xut.println("limitStartGrp=" + startGrp);
				startIdx = startIdx - subtractFromStartIdx - 1;
				// startIdx is adjusted b/c its length has been impacted by
				// replacements.

				endTableTextIdx = getEndOfTableLocation(text, startIdx + startGrp.length() + 100);
				// by adding 100 I extend by approx 2 lines if it is long
				// rnh - eg: [CASH FLOWS FROM OPERATING ACTIVITIES:] is 39
				// chars. if no RNH it will go at most 4 rows with very
				// abbreviated RNs and only 2 cols data

				int tableStart = Math.max(Math.min(text.length(), startIdx), 0);
				// System.Xut.println("tableStart: " + tableStart);
				// int tableStart = Math.min(text.length(),
				// startIdx + startGrp.length());
				String tableText = (text.substring(tableStart, Math.min(endTableTextIdx, text.length())));
				// String tableTextHold = (text.substring(tableStart,
				// Math.min(endTableTextIdx, text.length())));

				// System.Xut.println("tableText before additional
				// formatting="+tableText.substring(0,1000));
				startIdx = Math.min(text.length(), Math.max((endTableTextIdx), startIdx));

				// decimalText to check against StartPattern string.

				matcher = DecimalPattern.matcher(startGrp);
				// System.Xut.println("decimal pattern applied to this text
				// start|||"+startGrp+"|||end");
				// System.Xut.println("start
				// tableText|||"+tableText.substring(0,200)+"|||end tableText");
				String decimal = "";
				int dec = 0;
				if (matcher.find()) {
					// System.Xut.println("Decimal pattern found at:"
					// + matcher.start() + "::" + matcher.group());
					decimal = matcher.group();
					// System.Xut.println("decimal found ");
				}

				matcher = DecimalPattern.matcher(tableText.substring(0, Math.min(200, tableText.length())));
				if (decimal == null || decimal.length() < 2 || decimal == "") {
					// System.Xut.println("decimal not yet found");
					if (matcher.find()) {
						// System.Xut
						// .println("Decimal pattern found on 2nd attempt at:"
						// + matcher.start()
						// + "::"
						// + matcher.group());
						decimal = matcher.group();
					}
				}
				if (decimal.toLowerCase().contains("million")) {
					dec = -6;

				}
				if (decimal.toLowerCase().contains("billion")) {
					dec = -9;
				}

				if (decimal.toLowerCase().contains("thousand") || decimal.toLowerCase().contains("000")) {
					dec = -3;
				}

				if (decimal.toLowerCase().contains("hundred")
						|| (decimal.toLowerCase().contains("00") && !decimal.toLowerCase().contains("000"))) {
					dec = -2;
				}

				tableNameShort = "";

				tableNameShort = getTableNameShort(tableName).replaceAll("\r|\n", "");

				String[] finalHeadings = null;

				// try {

				Pattern YearSpaceMonthPattern = Pattern
						.compile("(?<=[\\d]{4}) (?=(?i)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dev))");

				// below shouldn't be necessary b/c it is removed in
				// getColumnHeadings method. Unclear why I replaced two spaces
				// with a tab? due to merged headings?

				// this use to be right after limitStartGrp - i don't think it
				// needs to move to location where limitStartGrp is
				StringBuffer sbuf = new StringBuffer();
				String startGrpLines[] = startGrp.split("\r\n");
				for (int i = 0; i < startGrpLines.length; i++) {
					if (!startGrpLines[i].toLowerCase().contains(" and")
							|| !startGrpLines[i].toLowerCase().contains("and ")) {
						// limits search and replace to non date sentence (those
						// w/o and)
						sbuf.append(startGrpLines[i].replaceAll("  ", " \t").replaceAll("[\t]{2,5}", "\t"));
						sbuf.append("\r\n");
					} else {
						sbuf.append(startGrpLines[i]);
						sbuf.append("\r\n");
					}
				}

				// System.Xut.println("startGrp #2a" + startGrp);
				// startGrp = sbuf.toString();
				startGrp = YearSpaceMonthPattern.matcher(startGrp).replaceAll("\t");

				// System.Xut.println("startGrp passed thru 1st finalheadings"
				// + startGrp);
				// System.Xut.println(":startidx8:" + startIdx);

				// System.Xut.println("startGrp to get finalHeadings
				// start|"+startGrp+"|end\r dataCols="+dataCols);
				finalHeadings = getColumnsHeadings(startGrp, dataCols, tableCount);
				// System.Xut.println("finalHeadings=="
				// + Arrays.toString(finalHeadings));

				// System.Xut
				// .println("after getColumnHeading
				// colHeadingPatternAtEachLine="
				// + colHeadingPatternAtEachLine);
				if (heading == "reversCH") {
					// see where heading is determined to be "reversCH"
					String[] finalHeadings2 = { finalHeadings[1], finalHeadings[0] };
					finalHeadings = finalHeadings2;
				}

				try {
					// System.Xut.println("2 finalHeadings==="
					// + Arrays.toString(finalHeadings));
				} catch (Exception e) {
					// System.Xut
					// .println("merged heading error exception...." + e);
				}
				// we dont have col headings..
				if (null == finalHeadings || finalHeadings.length == 0) {
					startIdx = Math.min(text.length(), endTblNmIdx);
					// System.Xut.println("new start of text for next loop:"
					// + text.substring(startIdx,
					// Math.min(text.length(), startIdx + 30)));
					continue;
				}

				startGrp = YearSpaceMonthPattern.matcher(startGrp).replaceAll("\t");

				// System.Xut.println(":startidx10:" + startIdx);
				startIdx = Math.min(endTableTextIdx, text.length());
				// text = text.substring(startIdx);
				// System.Xut.println(":startidx11:" + startIdx);

				// FinalHeading = [2003|Jan 30], [...], ...
				int endedPatternIndexInColHeading = -1, yearPatternIndexInColHeading = -1,
						monthPatternIndexInColHeading = -1;
				// the DoesPatternMatch tells us if we don't have and assigns
				// -1.
				String[] headingPatterns = finalHeadings[0].split("\\|");
				for (int p = 0; p < headingPatterns.length; p++) {
					// p is either 0,1 or 2 b/c it is either ended/mo/yr and we
					// split each pattern by |.
					// System.xut.println("headingPatterns[p]="
					// + headingPatterns[p]);

					if (nlp.doesPatternMatch(headingPatterns[p], EndedHeadingPattern)) {
						endedPatternIndexInColHeading = p;
						// System.xut.println("p=" + p
						// + " EndedHeadingPattern[p]="
						// + headingPatterns[p]);
					}

					else if (nlp.doesPatternMatch(headingPatterns[p], MonthPattern)) {
						monthPatternIndexInColHeading = p;
						// System.Xut.println("monthPattern[p]="
						// + headingPatterns[p]);
					}

					else if (nlp.doesPatternMatch(headingPatterns[p], YearPatternOrMoDayYrSimplePattern)) {
						yearPatternIndexInColHeading = p;
						// System.Xut.println("yearPattern[p]="
						// + headingPatterns[p]);
					}

					// the simple pattern has to be used b/c it is checking
					// only against the finalHeading after it has been split
					// by tab. so it may not have any additional data other
					// than the match.

				}
				// System.xut.println("colheadindexLocs: "
				// + endedPatternIndexInColHeading + ":"
				// + monthPatternIndexInColHeading + ":"
				// + yearPatternIndexInColHeading + "::"
				// + finalHeadings[0]);

				// endDate=yyyymmdd (merges mo and yr)
				// period=3,6,9 or 12. Two fields. (this ended.)

				String[][] endDatePeriodColHeading = getEndDatePeriodColHeading(finalHeadings,
						endedPatternIndexInColHeading, monthPatternIndexInColHeading, yearPatternIndexInColHeading,
						fileType);

				// for (int c = 0; c < endDatePeriodColHeading.length; c++) {
				// System.xut.println("per edt from finalHeadings="
				// + Arrays.toString(endDatePeriodColHeading[c]));
				// }

				int startGrpEndIdx = startGrp.length();
				// System.Xut.println("startGrpEndIdx" + startGrpEndIdx);
				// if ((startGrpEndIdx + 450) > tableText.length()) {
				// startIdx = Math.min(text.length(),
				// Math.max((endIdx + 10), startIdx + 10));
				// // text = text.substring(startIdx);
				// continue;
				// }
				// unclear if we need this - b/c startIdx is same whether if
				// condition is met.
				startIdx = Math.min(text.length(), endTableTextIdx);
				// text = text.substring(startIdx);
				// System.Xut.println(":startidx12: " + startIdx);

				tableText = tableText.substring(startGrpEndIdx);
				tableText = tableText.replaceAll("\t[A-Z]{1}\t", "\t");
				// gets rid of footnotes that are put in their own columns
				// (extremely rare).

				// System.Xut.println("tableText2="+tableText);
				// prestfPw.append(tableText);

				// PrintWriter tempPw2 = new PrintWriter(new File("c:/backtest/"
				// + getTableNamePrefixYearQtr(fileDate) + "temp17.txt"));
				// tempPw2.append(text);
				// tempPw2.close();

				// System.Xut.println("tableText used to get DataCol
				// count="+tableText);
				int[] colCountAndTotalRows = getDataColumnCount(getColumnCountMapForAllRows(tableText));
				// System.Xut
				// .println("tableText to get columnCount: " + tableText);
				int columns = colCountAndTotalRows[0];
				// System.Xut.println("# of columns--:" + columns);
				double headingRatioBeforeColUtil = (columns * 1.0 / finalHeadings.length);
				if (headingRatioBeforeColUtil == 2) {
					// System.Xut.println("headingRatioBeforeColUtil=2: "
					// + headingRatioBeforeColUtil);
					Pattern NumberTabDecimalNumber = Pattern.compile("(?<=[^\\.][\\d]{1,3}) ?\t(?=\\.\\d{1,3})");
					tableText = NumberTabDecimalNumber.matcher(tableText).replaceAll("").replaceAll("\\(\t", "\\(");
					// recorded as: 52tab.90tab60
					// should be: 52.9tab60.01
					colCountAndTotalRows = getDataColumnCount(getColumnCountMapForAllRows(tableText));
					columns = colCountAndTotalRows[0];
					// System.xut.println("# of columns=" + columns);
				}

				List<Integer> percentCols = new ArrayList<Integer>();
				headingRatioBeforeColUtil = (columns * 1.0 / finalHeadings.length);
				// System.Xut.println("headingRatio before utilities: "
				// + headingRatioBeforeColUtil);
				int numberOfRowWithMaxCols = colCountAndTotalRows[1];
				// numberOfRowWithMaxCols = counts instances of rows with
				// maximum number of columns
				int totalRows = colCountAndTotalRows[2];
				double rowRatioBeforeColUtil = (numberOfRowWithMaxCols * 1.0 / totalRows);
				// ignore ? when run after colUtility is run

				tableText = fourHyphenGroups.matcher(tableText).replaceAll("\r\n");
				tableText = threeHyphenGroups.matcher(tableText).replaceAll("\r\n");
				tableText = twoHyphenGroups.matcher(tableText).replaceAll("\r\n");
				tableText = oneHyphenGroups.matcher(tableText).replaceAll("\r\n");
				//
				// System.Xut
				// .println("1st 50 characters of SubTotalFinder tableText"
				// + tableText.substring(0, 50));

				SubTotalFinder stf = new SubTotalFinder();
				tableText = stf.multilpleRowNameMergeUtility(tableText);
				// rowUtilstfPw.append(tableText);

				// System.Xut
				// .println("columns per row: " + columns
				// + " # of finalHeadings columns "
				// + finalHeadings.length);

				if (totalRows < 0 || columns < 0) {
					startIdx = endTblNmIdx;
					continue;
				}

				// when table columns and ColHeading counts are not equal -
				// possibly look for 1/more Percent Columns

				// System.Xut
				// .println("goodHeadingSection right before col#!=fh.length="
				// + goodHeadingSection);

				// System.xut.println("# of columns=" + columns
				// + " finalHeading.len=" + finalHeadings.length);
				if (columns != finalHeadings.length) {
					// if (columns != finalHeadings.length) {

					startGrpLines = startGrp.split("\r\n");
					List<String> groups;
					// System.xut.println("final head count != columns");

					for (int i = 0; i < startGrpLines.length; i++) {

						// match the line against colHeadPattern, if found,
						// match against percent pattern also.. if found,
						// capture that
						if (nlp.doesPatternMatch(startGrpLines[i], OddColumnPattern)
								&& nlp.doesPatternMatch(startGrpLines[i], ColumnHeadingPattern)) {
							/*
							 * below ensures we only check lines that a CH over each dataCol (fh.len=#CHs on
							 * line). This way we know the 'oddCol' location is correct. This ensures we
							 * remove (later) the correct dataCols with ref to this idxs loc.
							 */
							if (nlp.getAllMatchedGroups(startGrpLines[i], ColumnHeadingPattern)
									.size() != finalHeadings.length)
								continue;
							groups = nlp.getAllMatchedGroups(startGrpLines[i], AllColumnHeadingPattern);
							// System.xut
							// .println("line found with percent and CH:"
							// + groups.size() + ":"
							// + startGrpLines[i]);
							for (int g = 0; g < groups.size(); g++) {
								if (nlp.doesPatternMatch(groups.get(g), OddColumnPattern)) { //
									percentCols.add(g);
									// .add(g) saves location to the
									// List<Integer>
									// System.xut
									// .println("Column is percent type: "
									// + g);
								} else
									System.out.println("");
								// System.xut
								// .println("group not matching oddPattern:"
								// + groups.get(g));
							}
							if (columns - percentCols.size() != finalHeadings.length) {
								// System.xut
								// .println("length doesn't match - percentCols.size()="
								// + percentCols.size());
								percentCols.clear();
							}
							break;
						}
					}
				}
				// }

				// System.Xut.println("rowCountforColumns: "
				// + numberOfRowWithMaxCols);
				// System.Xut.println("totalRows: " + totalRows);

				tableText = hardReturnNumberTabNumber2.matcher(tableText).replaceAll("\r\nBLANK ROW\t");
				tableText = hardReturnNumberTabNumber.matcher(tableText).replaceAll("\r\nBLANK ROW\t");
				// System.Xut.println("tableText: " +tableText);

				tableText = insertMissingTab(tableText, numberCloseParenOpenParenNumber);
				tableText = insertMissingTab(tableText, numberDollarNumber);
				// above insertMissingTab can insert duplicate tabs - hence
				// replacement below. May need to revise
				tableText = tableText.replaceAll("[\t ]{2,10}", "\t");

				// System.Xut.println("no of columns as per
				// finalHeadings.length:"
				// + finalHeadings.length);

				if (percentCols.size() > 0) {
					// columns is not fh.len - columns is counting tabs for each
					// data row.
					tableText = SubTotalFinder.columnUtility(tableText, columns);
					tableText = removePerecentCols(tableText, percentCols);
					percentCols.clear();
					// TODO: recount columns/rows etc..
				}

				// System.Xut.println("colsWithMaxRows=" +
				// numberOfRowWithMaxCols);
				if (finalHeadings.length == columns && tableText.split("\r\n").length > 10) {
					// if no percentCols it will run this (which is most often
					// the case). Final instance of running columnUtility
					tableText = SubTotalFinder.columnUtility(tableText, finalHeadings.length);
					// TODO: Can run location based CH parser
				} else {
					// System.Xut.println("finalHeadings.length: "
					// + finalHeadings.length + " # of columns: "
					// + columns);
					// save garabage tableText to session txt file:
					// acc||cik<hardreturn>tableText
					// table bad b/c finalHeadings.length != columns
					System.out.println("2 fileDate=" + fileDate);
					String fileName1 = "c:/backtest/" + getTableNamePrefixYearQtr(fileDate) + "garbageTable.txt";
					// PrintWriter pw = new PrintWriter(fileName1);
					String[] lines = tableText.split("\r\n");
					int cnt = 0;
					for (String line : lines) {
						cnt++;
						// pw.write(acc + "||" + cik + "||" + tableCount + "||"
						// + cnt + "||" + line.replaceAll("\t", "[]")
						// + "\r\n");
					}
					// pw.close();
					continue;
				}

				tableText = stf.removeGarbageLines(tableText);

				// colUtilstfPw.append(tableText);
				// colUtilstfPw.close();

				tableText = stf.replaceSubTotals(tableText, tableName);
				// System.Xut.println("tableText after additional
				// reformatting="+tableText.substring(0,1000));
				// TODO: this is where tableText is most formatted.

				// System.Xut
				// .println("writing 1st 70 characters of subTotal tableText: "
				// + tableText.substring(0, 70));
				// subTotalPw.append(tableText);

				colCountAndTotalRows = getDataColumnCount(getColumnCountMapForAllRows(tableText));
				columns = colCountAndTotalRows[0];
				numberOfRowWithMaxCols = colCountAndTotalRows[1];
				totalRows = colCountAndTotalRows[2];
				double headingRatioAfterColUtil = (columns * 1.0 / finalHeadings.length);
				double rowRatioAfterColUtil = (numberOfRowWithMaxCols * 1.0 / totalRows);
				// System.Xut
				// .println("after all utilities are run
				// (columnUtility,rowMerge, percentCols and subTotal).
				// HeadingRatio="
				// + headingRatioAfterColUtil
				// + " rowRatio="
				// + rowRatioAfterColUtil);

				String[] tableContentLines = tableText.split(LineSeparator);

				String[] contentLineParts;
				int tableRowCount = 0;
				// System.Xut.println("acceptedDate - bsnwire y/n?: "
				// + acceptedDate);
				if (fileType.equalsIgnoreCase("bsnWire")) {
					fileDate = acceptedDate;
					// System.Xut.println("fileDate bsnWire::" + fileDate);
				}

				// print tablename

				// System.Xut.println("apostrophe catastrophe="
				// + tableName.replaceAll(" ", " ").replaceAll(" ", " ")
				// .replaceAll("'", "\'").trim());

				coMatch = 0;
				if (coNameParsed.length() > 1) {
					coMatch = 1;
				}

				if (null != tableSentence && tableSentence.length() > 175)
					tableSentence = "";

				if (null != tableSentence && tableSentence.length() > 2)
					tableSentence.trim();

				String tableSentencePattern = nlp.getTableSentencePatterns(tableSentence, "").trim();
				// System.xut.println("tableSentencePattern="
				// + tableSentencePattern);

				Pattern pCnt = Pattern.compile("\\dP:");
				Pattern yCnt = Pattern.compile("\\dY:");
				Pattern mCnt = Pattern.compile("\\dM:");
				String pCnts, yCnts, mCnts;

				int pI = 0, yI = 0, mI = 0;

				if (null != nlp.getAllIndexEndLocations(tableSentencePattern, pCnt)
						&& nlp.getAllIndexEndLocations(tableSentencePattern, pCnt).size() > 0)
					pI = nlp.getAllIndexEndLocations(tableSentencePattern, pCnt).size();

				if (null != nlp.getAllIndexEndLocations(tableSentencePattern, yCnt)
						&& nlp.getAllIndexEndLocations(tableSentencePattern, yCnt).size() > 0)
					yI = nlp.getAllIndexEndLocations(tableSentencePattern, yCnt).size();

				if (null != nlp.getAllIndexEndLocations(tableSentencePattern, mCnt)
						&& nlp.getAllIndexEndLocations(tableSentencePattern, mCnt).size() > 0)
					mI = nlp.getAllIndexEndLocations(tableSentencePattern, mCnt).size();

				pCnts = "pCnt:" + pI;
				yCnts = "yCnt:" + yI;
				mCnts = "mCnt:" + mI;

				tableSentencePattern = tableSentencePattern + pCnts + "|" + yCnts + "|" + mCnts;

				int tRow = 0;
				// START OF INDIVIDUAL TABLE PARSING
				for (String line : tableContentLines) {
					tRow++;
					// System.Xut.println("contentLindParts=="+line);
					contentLineParts = line.split("\t");
					if (contentLineParts.length == 1) {
						// if only RN: only 1 column - only this loop is done
						if (contentLineParts[0] != null) {
							tableRowCount++;
							sb.append(acc + "||" + fileDate + "||" + cik + "||" + tableNameShort + "||" + tableRowCount
									+ "||'||" + tableCount + "||" + tRow + "||");
							sb.append(contentLineParts[0].replaceAll("\r|\n", "") + "||''"
									+ "||''||''||''||''||''||''||''||''||''||''||''||''||''||''||''||''||''||''||''||"
									+ "\r\n");

							// System.Xut.print(acc + "||" + fileDate + "||" +
							// cik
							// + "||" + tableNameShort + "||"
							// + tableRowCount + "||" + tableCount +
							// "||"+tRow+"||");
							// System.Xut.println(contentLineParts[0] + "||''"
							// +
							// "||''||''||''||''||''||''||''||''||''||''||''||''||''||''||''||''||''||''||''||"
							// + "\r\n");

						}
					}

					int fhIdx = -1;

					for (int p = 1; p < contentLineParts.length; p++) {
						fhIdx = p - 1;

						// if multiple columns - then this loop is done.
						tableRowCount++;
						sb.append(acc + "||" + fileDate + "||" + cik + "||" + tableNameShort + "||" + tableRowCount
								+ "||" + (fhIdx + 1) + "||" + tableCount + "||" + tRow + "||");
						sb.append(contentLineParts[0].replaceAll("\\\\", "").replaceAll("\r|\n", "") + "||");

						double amt = SubTotalFinder.parseToAmount(contentLineParts[p].replaceAll("\r\n", ""));
						String amtS = amt + "";
						// Ensures years are not inadvertantly parsed as values.
						// '\\\\N' is null when imported into mysql
						sb.append(amtS.replaceAll("3.3333", "\\\\N").replaceAll("\r|\n", "") + "||");

						// System.Xut.print(acc + "||" + fileDate + "||" + cik
						// + "||" + tableNameShort + "||" + tableRowCount
						// + "||" + tableCount + "||"+tRow+ "||" + (fhIdx +
						// 1)+"||"
						// + contentLineParts[0] + "||"
						// + amtS.replaceAll("3.3333", "null") + "||"
						// + (fhIdx + 1) + "||");

						if (finalHeadings.length > fhIdx) {
							// System.Xut.println("fhIdx=" + fhIdx + "
							// finalHeadings.len=" + finalHeadings.length
							// + " finalHeadings=" +
							// Arrays.toString(finalHeadings));
							headingPatterns = finalHeadings[fhIdx].replaceAll("\r|\n", "").split("\\|");

							// if(headingPatterns.length<=endedPatternIndexInColHeading)
							// continue;
							// period[0] and endDate[1]==>
							sb.append(endDatePeriodColHeading[fhIdx][0] + "||");
							sb.append(endDatePeriodColHeading[fhIdx][1] + "||");

							// System.Xut.print(endDatePeriodColHeading[fhIdx][0]
							// + "||");
							// System.Xut.print(endDatePeriodColHeading[fhIdx][1]
							// + "||");
							// sb.append(finalHeadings[fhIdx] + "||");
							sb.append(dec + "||");
							// System.Xut.print(dec + "||");

							if (endedPatternIndexInColHeading >= 0
									&& headingPatterns.length > endedPatternIndexInColHeading) {
								sb.append(headingPatterns[endedPatternIndexInColHeading].replaceAll("\r|\n", " "));
								// System.Xut
								// .print(headingPatterns[endedPatternIndexInColHeading]
								// .replaceAll("\r\n", " "));
							}

							// System.Xut.println("endedPatternIndexInColHeading
							// col#?="+endedPatternIndexInColHeading);

							sb.append("||");
							// System.Xut.print("||");
							if (yearPatternIndexInColHeading >= 0
									&& headingPatterns.length > yearPatternIndexInColHeading) {
								sb.append(headingPatterns[yearPatternIndexInColHeading].replaceAll("\r|\n", " "));
								// System.Xut
								// .print(headingPatterns[yearPatternIndexInColHeading]
								// .replaceAll("\r\n", " "));
							}

							sb.append("||");
							// System.Xut.print("||");
							// if(headingPatterns.length<=monthPatternIndexInColHeading)
							// continue;

							if (monthPatternIndexInColHeading >= 0
									&& headingPatterns.length > monthPatternIndexInColHeading) {
								sb.append(headingPatterns[monthPatternIndexInColHeading].replaceAll("\r|\n", " "));
								// System.Xut
								// .print(headingPatterns[monthPatternIndexInColHeading]
								// .replaceAll("\r\n", " "));
							}

							// m/ended/yr pattern.
							// TotCols=contentLineParts.length
							// TODO: insert mpDist at -1//htmlText?//

							sb.append("||'" + colHeadingPatternAtEachLine + "||" + coMatch + "||" + coNameParsed + "||"
									+ companyName + "||" + "generic" + "||" + rowRatioBeforeColUtil + "||'"
									+ tableSentencePattern + "'||" + tableSentence + "||" + rowRatioAfterColUtil
									+ "\n");

							// System.Xut.print("||'" +
							// colHeadingPatternAtEachLine
							// + "||" + coMatch + "||" + coNameParsed + "||"
							// + companyName + "||" + "generic" + "||"
							// + rowRatioBeforeColUtil + "||'"
							// + tableSentencePattern + "'||"
							// + tableSentence + "||"
							// + rowRatioAfterColUtil + "\n");

						}
					}
				}

				String mysqlTableText = sb.toString();

				String finalTable = getMapData(mysqlTableText, tableNameShort);
				// System.xut.println("finalTable start|");
				// System.xut.println(sb.toString() + "\rfinalTable end");
				// w/n this bracket is loop that grabs each table
				// tablePw.append(finalTable);
				sb.delete(0, sb.toString().length());

			} else
				break;
		}
		// put here so that it goes through entire file and saves all tables
		// above via append then loads them once below. May even want to do this
		// for entire quarter.
		// tablePw.close();

		// TODO
		// ensure I pass far right col b/c it subtotals all other rows will.
		// add stringbuffer in lieu of tablePw - then set below string to
		// sb.tostring

		// append finalTable via tablePw
		// TODO: add tRow at col7 to tp_generic_all table
		System.out.println("4 fileDate=" + fileDate);
		int year = Integer.parseInt(fileDate.replaceAll("-", "").substring(0, 4));
		int month = Integer.parseInt(fileDate.replaceAll("-", "").substring(4, 6));
		int qtr = ((month - 1) / 3) + 1;
		String tblAppend = year + "QTR" + qtr;

		Utils.loadIntoMysql2(fileName.replaceAll("\\\\", "/"), "tp_generic" + tblAppend);
		text = text.substring(Math.min(text.length(), endTableTextIdx));
		startIdx = 1;
		// subTotalPw.close();
		// prestfPw.close();
		// rowUtilstfPw.close();
		// colUtilstfPw.close();
	}

	public String getMapData(String tableStr, String tableNameShort) throws ParseException {
		// System.Xut.println("tableStr="+tableStr+"end tableStr");
		NLP nlp = new NLP();

		String line = "";
		String[] lineField = null;
		String[] prepSubTotalTable = tableStr.split("[\r\n]");
		String rownm = "", colStr = "";
		Integer pTR = -2, tbRw = -3;
		String data;
		// this preps map so it can be passed to getSubtotals method
		Map<Integer, List<String[]>> mapData = new TreeMap<Integer, List<String[]>>();
		List<String[]> listRow = new ArrayList<String[]>();

		for (int i = 0; i < prepSubTotalTable.length; i++) {
			line = prepSubTotalTable[i];
			lineField = line.split("\\|\\|");
			// System.Xut.println("1. line==" + line);
			if (line.replaceAll("[\r \\-=]", "").length() < 1 || lineField.length < 10
			/* || lineField[7].replaceAll("[\']{1,}", "").length() < 1 */)
				continue;
			// System.Xut.println("line=="+line+"
			// lineField.len"+lineField.length);
			rownm = lineField[8];
			colStr = lineField[5];
			// System.xut.println("2. line==" + line);
			tbRw = Integer.parseInt(lineField[7].replaceAll("[\']{1,}", ""));
			data = lineField[9];
			// get RNH and put into tbl.
			// System.Xut.println("colStr=" + colStr + " data=" + data
			// + " prevTblRow=" + pTR + " tblRow=" + tbRw);

			if (tbRw != pTR) {

				// add dummy ary[2] to pass getSubtotal method
				String[] ary = { rownm.length() + "", rownm.replaceAll("[\']{1,}", "") + " ", 1 + "" };
				listRow.add(ary);
				// System.Xut.println("added ary="+Arrays.toString(ary));
			}

			if (colStr.replaceAll("\',", "").equals("0") /* || pTR==-2 */) {
				// System.Xut.println("RNH colStr="+colStr+" pTR="+pTR+"
				// data="+data+" rownm="+rownm);
				// System.Xut.println("RNH - added to mapData. Key="+tbRw+"
				// listRow=");
				// nlp.printListOfStringArray(listRow);
				mapData.put(tbRw, listRow);
				// System.Xut.println(" map after adding RNH=");
				// nlp.printMapIntListOfStringAry(mapData);
				listRow = new ArrayList<String[]>();
				pTR = tbRw;
				continue;
			}

			if (tbRw != pTR) {
				// System.Xut.println("B tblrow="+tbRw+" prevTblRow="+pTR+"
				// data="+data+" rownm="+rownm);
				// System.Xut.println("before adding ary2. listRow=");
				// nlp.printListOfStringArray(listRow);
				colStr = colStr.replaceAll("\'", "");
				data = data.replaceAll("\'", "");
				String[] ary = { colStr, data, 1 + "" };
				// System.xut.println("B ary2 -- colStr=" + colStr + " data="
				// + data);
				listRow.add(ary);
				// System.xut.println("key=" + tbRw + " B added to map listRow=");
				// nlp.printListOfStringArray(listRow);
				// mapData.put(tbRw, listRow);
				// System.Xut.println("B map after adding listRow=");
				// nlp.printMapIntListOfStringAry(mapData);
				listRow = new ArrayList<String[]>();
			}

			pTR = tbRw;
		}

		// System.Xut.println("print mapData before getSubtotal=");
		// nlp.printMapIntListOfStringAry(mapData);

		mapData = NLP.getSubtotals(mapData, 1, true, tableNameShort, false);

		// System.Xut.println("mapdata subtotal finished - print==");
		// nlp.printMapIntListOfStringAry(mapData);

		String rownameMap = "";
		// need to fix so it is
		StringBuffer sb2 = new StringBuffer();

		// This rejoins the subtotaled map with entire lines of table - key in
		// map fetched by tblrow in each line.
		int linesInTable = prepSubTotalTable.length;
		boolean hasSubTotal = false;
		for (int i = 0; i < linesInTable; i++) {
			// get key at each line equal to tblrow
			line = prepSubTotalTable[i];
			lineField = line.split("\\|\\|");
			if (lineField.length < 9)
				continue;
			if (lineField[8].replaceAll("[\' ]", "").length() < 1)
				continue;
			if (line.replaceAll("[\r \\-=]", "").length() < 1)
				continue;

			// System.xut.println("line=" + line + " lineField.len="
			// + lineField.length);
			rownm = lineField[8];

			tbRw = Integer.parseInt(lineField[7].replaceAll("[\']{1,}", ""));
			if (null != mapData.get(tbRw))
				rownameMap = mapData.get(tbRw).get(0)[1].replaceAll("[ ]{2,}", " ").trim().replaceAll("','", "||");
			// System.Xut.println("rownameMap="+rownameMap);

			sb2.append("\r");
			for (int c = 0; c < lineField.length; c++) {
				if (c == 8) {
					// System.Xut.println("c==7"+" rownameMap="+rownameMap);

					String tl = "";
					if (rownameMap.contains(";TL")) {
						tl = rownameMap.substring(rownameMap.indexOf(";TL") + 3).trim();
						// System.Xut.println("1 tl="+tl);
						tl = tl.substring(0, tl.indexOf(";")).trim();
						// System.Xut.println("2 tl="+tl);
						hasSubTotal = true;
					}

					if (!rownameMap.contains(";TL")) {
						tl = "\\N";
					}

					String st = "";
					if (rownameMap.contains(";ST")) {
						st = rownameMap.substring(rownameMap.indexOf(";ST") + 3).trim();
						// System.Xut.println("1 st="+st);
						st = st.substring(0, st.indexOf(";")).trim();
						// System.Xut.println("2 st="+st);
						hasSubTotal = true;
					}

					if (!rownameMap.contains(";ST")) {
						st = "\\N";
					}

					String net = "";
					if (rownameMap.contains(";NET")) {
						net = rownameMap.substring(rownameMap.indexOf(";NET") + 4).trim();
						net = net.substring(0, net.indexOf(";")).trim();
						hasSubTotal = true;
					}

					if (!rownameMap.contains(";NET")) {
						net = "\\N";
					}

					String sub = "";
					if (rownameMap.contains(";SUB")) {
						sub = rownameMap.substring(rownameMap.indexOf(";SUB") + 4).trim();
						sub = sub.substring(0, sub.indexOf(";")).trim();
						hasSubTotal = true;
					}

					if (!rownameMap.contains(";SUB")) {
						sub = "\\N";
					}
					// System.Xut.println("line==="+line+"
					// lineField.len"+lineField.length);
					sb2.append(
							rownameMap + "||" + lineField[9] + "||" + tl + "||" + st + "||" + net + "||" + sub + "||");
				}
				if (c != 8 && c != 9) {
					if (c < (lineField.length - 1)) {
						sb2.append(lineField[c] + "||");
					} else
						sb2.append(lineField[c]);
				}
			}
		}
		return sb2.toString();
	}

	public String stripHtmlTags(String text) throws FileNotFoundException {

		// System.Xut.println("stripHtmlTags text200 characters="
		// + text.substring(0, 200));

		TableParser tp = new TableParser();

		if (tp.acceptedDate != null)
			tp.fileDate = tp.acceptedDate;

		if (fileDate == null)
			fileDate = "1901-01-01";

		text = text.replaceAll("<PRE>|</PRE>|<PAGE>|</PAGE>", "");
		Pattern startTr = Pattern.compile("(?i)(<tr[^>]*>)");
		Pattern endTr = Pattern.compile("(?i)(</(tr|table)[^>]*>)");
		Pattern startTd = Pattern.compile("(?i)(<td[^>]*>)");
		Pattern endTd = Pattern.compile("(?i)(</td[^>]*>)");
		Pattern startTh = Pattern.compile("(?i)(<th[^>]*>)");
		Pattern endTh = Pattern.compile("(?i)(</th[^>]*>)");

		try {
			text = keepCellsInSameRow(text, startTd, endTd);
		} catch (Throwable t) {
			t.printStackTrace(System.out);
		}

		// PrintWriter tempPw2 = new PrintWriter(new File("c:/backtest/"
		// + getTableNamePrefixYearQtr(fileDate) + "temp3.txt"));
		// tempPw2.append(text);
		// tempPw2.close();

		try {
			text = keepCellsInSameRow(text, startTh, endTh);
		} catch (Throwable t) {
			t.printStackTrace(System.out);
		}

		try {
			text = keepCellsInSameRow(text, startTr, endTr);
		} catch (Throwable t) {
			t.printStackTrace(System.out);
		}

		// PrintWriter tempPw3 = new PrintWriter(new File("c:/backtest/"
		// + getTableNamePrefixYearQtr(fileDate) + "temp4.txt"));
		// tempPw3.append(text);
		// tempPw3.close();

		try {
			text = text.replaceAll(NLP.TRPattern.toString(), LineSeparator);
			// PrintWriter tempPw4 = new PrintWriter(new File(""
			// + getTableNamePrefixYearQtr(fileDate) + "temp5.txt"));
			// tempPw4.append(text);
			// tempPw4.close();
			text = text.replaceAll("\\&nbsp\r\n;|\\&nbsp;\r\n", " ");
			text = text.replaceAll("\\&nbsp;?|\\xA0", " ");
			// theory is you don't want a hard return after a hard space
			text = text.replaceAll("\\&#151;|\\&mdash", "_");
			text = text.replaceAll("\\&#8211;", "-");
			text = text.replaceAll("\\&amp;", "\\&");
			text = text.replaceAll("Ã¢â‚¬â€�", "-");
			text = text.replaceAll("Ã¢â‚¬Å“|Ã¢â‚¬?", "\"");
			text = text.replaceAll("\\&lt;", "<");
			text = text.replaceAll("\\&gt;", ">");
			// PrintWriter tempPw5 = new PrintWriter(new File("c:/backtest/"
			// + getTableNamePrefixYearQtr(fileDate) + "temp6.txt"));
			// tempPw5.append(text);
			// tempPw5.close();
			text = text.replaceAll("\\&#146;", "'");
			// PrintWriter tempPw6 = new PrintWriter(new File("c:/backtest/"
			// + getTableNamePrefixYearQtr(fileDate) + "temp7.txt"));
			// tempPw6.append(text);
			// tempPw6.close();
			text = text.replaceAll("\\&#[\\d]*;", "");
			// <=necessary?
			// deleted .replaceAll(SpacesWithinTDs.toString(), " ")
			// PrintWriter tempPw7 = new PrintWriter(new File("c:/backtest/"
			// + getTableNamePrefixYearQtr(fileDate) + "temp8.txt"));
			// tempPw7.append(text);
			// tempPw7.close();
			text = text.replaceAll(NLP.TDWithColspanPattern.toString(), "\t\t");
			// PrintWriter tempPw8 = new PrintWriter(new File("c:/backtest/"
			// + getTableNamePrefixYearQtr(fileDate) + "temp9.txt"));
			// tempPw8.append(text);
			// tempPw8.close();
			text = text.replaceAll(NLP.TDPattern.toString(), "\t");
			// PrintWriter tempPw9 = new PrintWriter(new File("c:/backtest/"
			// + getTableNamePrefixYearQtr(fileDate) + "temp10.txt"));
			// tempPw9.append(text);
			// tempPw9.close();
			text = text.replaceAll("(?i)(<SUP[^>]*>[\\(0-9\\) ]*</SUP[^>]*>)", "");
			// all superscripts [i.e. (1) (2)...] with blank
			// PrintWriter tempPw10 = new PrintWriter(new File("c:/backtest/"
			// + getTableNamePrefixYearQtr(fileDate) + "temp11.txt"));
			// tempPw10.append(text);
			// tempPw10.close();
			text = text.replaceAll("</U>|<U>|</u>|<U>", "");
			// above is removed b/c it interfers with below <BR>
			text = text.replaceAll("(?i)<BR> ?\r\n ?<BR>", "\r\n");
			// if 2 consecutive BRs-likely meant hard return
			// if BR after a number - likely end of a row.
			text = NLP.numberBR.matcher(text).replaceAll("\r\n");
			text = text.replaceAll("(?i)(<BR>\r\n|\r\n<BR>)", " ");

			// PrintWriter tempPw10a = new PrintWriter(new File("c:/backtest/"
			// + getTableNamePrefixYearQtr(fileDate) + "temp11b.txt"));
			// tempPw10a.append(text);
			// tempPw10a.close();

			text = text.replaceAll("(?i)<table[^>]*>", " xx ");
			text = text.replaceAll("(?i)</table[^>]*>", " xx ");
			// need 'xx' b/c it will be picked up as shortline to count against
			// endofTable (if too many rows not meeting criteria it is
			// endOfTable - and 'xx' is one such row). Often </table if followed
			// by <table - so keep both - as those are 2 rows to count towards
			// likely endofT. Need space bef/aft xx or it corrupts tablenName

			// PrintWriter tempPw11 = new PrintWriter(new File("c:/backtest/"
			// + getTableNamePrefixYearQtr(fileDate) + "temp12.txt"));
			// tempPw11.append(text);
			// tempPw11.close();

			text = text.replaceAll("(?i)(</strong>|<strong>|</small>|<small>)", "");

			// PrintWriter tempPw12b = new PrintWriter(new File("c:/backtest/"
			// + getTableNamePrefixYearQtr(fileDate) + "temp12b.txt"));
			// tempPw12b.append(text);
			// tempPw12b.close();

			text = text.replaceAll("(?i)<BR>[ ]{1,10}", "<BR>").replaceAll(NLP.ExtraHtmlPattern.toString(), "")
					.replaceAll(NLP.ExtraHtmlPattern2.toString(), " ");
			// previously I replaced all ExtraHtmlPatterns with a "~" and then
			// replaced all them with just 1 ws. This causes problems.
			// Ultimately if I need to remove problematic html code. Focus on
			// removing html code that only formats and replace with nothing and
			// for that which is a hard return or tab or row replace with a
			// space or hard return or tab.

			// System.Xut.println("11 fileDate=" + fileDate);
			// PrintWriter tempPw12c = new PrintWriter(new File("c:/backtest/"
			// + getTableNamePrefixYearQtr(fileDate) + "temp12c.txt"));
			// tempPw12c.append(text);
			// tempPw12c.close();

		} catch (Throwable t) {
			t.printStackTrace(System.out);
		}
		return text;
	}

	public String keepCellsInSameRow(String html, Pattern startPattern, Pattern endPattern)
			throws FileNotFoundException {
		NLP nlp = new NLP();
		// <td[^>]*
		// this simply removes all hard returns within start and end pattern

		Pattern tmp = Pattern.compile("[\r\n]");
		int howMany = nlp.getAllIndexEndLocations(html, tmp).size();
		if (howMany > 10)
			return html;

		StringBuffer sb = new StringBuffer();
		// sb.delete(0, sb.toString().length());
		int start = 0, htmlLen = html.length();
		List<Integer> idxStartTrs = nlp.getAllIndexStartLocations(html, startPattern);
		List<Integer> idxEndTrs = nlp.getAllIndexStartLocations(html, endPattern);
		if (idxStartTrs.size() == 0 || idxEndTrs.size() == 0) {
			// System.Xut.println("no TR found..");
			return html;
		}
		int endTrI = 0, endTrLoc = 0;
		for (Integer idxStartTr : idxStartTrs) {
			if (start > idxStartTr)
				continue;
			sb.append(new String(html.substring(start, idxStartTr)));
			// above is identifying JUST the start of the pattern - so we do NOT
			// want to replace anything here!!

			for (Integer eTr = endTrI; eTr < idxEndTrs.size(); eTr++) {
				endTrI++;
				endTrLoc = idxEndTrs.get(eTr);
				if (endTrLoc <= idxStartTr)
					continue;
				else {
					String htmlTemp = new String(html.substring(idxStartTr, endTrLoc));
					htmlTemp = htmlTemp.replaceAll("[\r\n]{1,}|[\\s]{2,}", " ");
					if (startPattern.equals("startTd") || startPattern.equals("startTh")) {
						htmlTemp = nlp.htmlPara.matcher(htmlTemp).replaceAll(" ");
						// if <td > <p>hello</p>world</td> it removes the <p
						// (same for <div and <br)
					}
					sb.append(new String(htmlTemp));
					break;
				}
			}
			start = endTrLoc;
		}
		String keepCellsTextTogether = (html.substring(start, htmlLen));
		// sb.append(new String(html.substring(start, htmlLen)))
		sb.append(new String(keepCellsTextTogether));
		String temp = sb.toString();
		sb.delete(0, sb.toString().length());
		return temp;
	}

	public String removePerecentCols(String tableText, List<Integer> percentCols) {
		NLP nlp = new NLP();
		String[] lines = tableText.split("\r\n"), lineCols;
		for (int i = 0; i < lines.length; i++) {
			lineCols = lines[i].split("\t");
			if (lineCols.length < 2) // if only rowname skip
				continue;
			else {
				for (int c = percentCols.size() - 1; c >= 0; c--) {
					// use '.size()' to go reverse direction (right<==left).
					// This way the remaining columns to check location don't
					// chg b/c col removed is after the next col to check
					// System.xut.println("removing percols:lineCols.length="
					// + lineCols.length + ",percentCols.get(c)="
					// + percentCols.get(c) + ":" + lines[i]
					// + " percentCols.size=" + percentCols.size());
					if (lineCols != null && percentCols.get(c) != null && percentCols.get(c) + 1 < lineCols.length) {
						lineCols = (String[]) ArrayUtils.remove(lineCols, percentCols.get(c) + 1);
					}
					// add 1 to accommodate for rowName as 1st col..
				}
				lines[i] = StringUtils.join(lineCols, "\t");
			}
		}
		return StringUtils.join(lines, "\r\n");
	}

	public int getDayFromFYE(String year, int monthNo) {
		// if monthNo (starts at zero) is greater than the qtrEnd month we
		// default to first day of that month. For example if 0630 is QtrEnd but
		// month parsed is July - we default to July 1 or 0701. Currently this
		// doesn't contemplate day parsed as day is not currently parsed. It
		// assumes the day equal to endDate of qtrEnd for the related month.
		NLP nlp = new NLP();
		if (fye == null || fye.trim().length() < 4)
			return -1;

		fye = fye.trim();
		if (!fye.substring(0, 2).contains("0") && !fye.substring(0, 2).contains("1")
				&& !fye.substring(0, 2).contains("2") && !fye.substring(0, 2).contains("3")
				&& !fye.substring(0, 2).contains("4") && !fye.substring(0, 2).contains("5")
				&& !fye.substring(0, 2).contains("6") && !fye.substring(0, 2).contains("7")
				&& !fye.substring(0, 2).contains("8") && !fye.substring(0, 2).contains("9"))
			return 0;
		if (monthNo == Integer.parseInt(fye.substring(0, 2)))
			return 1;
		int lastDayOfQtrMonth = getMonthLastDate(Integer.parseInt(fye.substring(0, 2)) - 1, Integer.parseInt(year));

		// System.Xut.println("getDayFromFYE fye=" + fye +
		// ", lastDayOfQtrMonth="
		// + lastDayOfQtrMonth);
		int diffDays = Integer.parseInt(fye.substring(2)) - lastDayOfQtrMonth;

		lastDayOfQtrMonth = getMonthLastDate(monthNo, Integer.parseInt(year));
		// System.Xut.println("lastDayOfQtrMonth of our mo=" + lastDayOfQtrMonth
		// + ", diff=" + diffDays);
		return (lastDayOfQtrMonth - Math.abs(diffDays));
	}

	// cks the dd captured in the pattern matcher against the calcuated date for
	// that month based on the fye. dd calc based on fye is to find # of days
	// from end of month as indicated in fye and apply to month captured in
	// pattern. Also check if month captured in pattern is a month that
	// correspond to a quarter that relates to fye

	public String[][] getEndDatePeriodColHeading(String[] finalHeadings, int endedPatternIndexInColHeading,
			int monthPatternIndexInColHeading, int yearPatternIndexInColHeading, String fileType) {
		// the int values are the healding[p] value (so value is -1 if not
		// occurring and 0,1 or 2 for its location
		NLP nlp = new NLP();
		String[][] endDatePeriodColHeadings = new String[finalHeadings.length][2];
		// each dim array will now contain up to 2 part - endDate and period
		if (endedPatternIndexInColHeading >= 0) {
			for (int c = 0; c < finalHeadings.length; c++) {
				endDatePeriodColHeadings[c] = new String[] { "-1", "" };
				// {"-1",""} is the initialization of the array (2D and first
				// value="-1". So if none of the patterns match below endDate
				// will be equal to -1.

				// this is the initialization of the 1st D of the array. and C
				// is the col#. finalHeadings[c]=[2003|ended|Jan 30]
				String colHeadingEndedPart = finalHeadings[c].split("\\|")[endedPatternIndexInColHeading];
				// this is capturing the exact ending pattern from
				// [2003|ended|Jan 30] by saying take the instance of the split
				// that has the index loc = to endedPatternIndexInColHeading

				// System.xut.println("fh used to get endDate and Period: "
				// + finalHeadings[c]);
				if (nlp.doesPatternMatch(colHeadingEndedPart, OddBallPeriodPattern))
					endDatePeriodColHeadings[c][0] = "22";
				else if (nlp.doesPatternMatch(colHeadingEndedPart, Period3MonthsPattern))
					endDatePeriodColHeadings[c][0] = "3";
				else if (nlp.doesPatternMatch(colHeadingEndedPart, Period6MonthsPattern))
					endDatePeriodColHeadings[c][0] = "6";
				else if (nlp.doesPatternMatch(colHeadingEndedPart, Period9MonthsPattern))
					endDatePeriodColHeadings[c][0] = "9";
				else if (nlp.doesPatternMatch(colHeadingEndedPart, Period12MonthsPattern))
					endDatePeriodColHeadings[c][0] = "12";

				else {
					endDatePeriodColHeadings[c][0] = "0";
				}

			}
		}

		// System.xut
		// .println("at this point we have each of year, month and ended for
		// finalHeadings. finalHeadings="
		// + Arrays.toString(finalHeadings));

		// AT THIS POINT WE HAVE EACH OF: year, month and ended.
		// first check if yr=mm/dd/yy - if so - finished.
		// merge month and year. and that's 1 string. sometimes we only have
		// convert monthStr to mo and day (assuming always month is avalble).

		Pattern DayPattern = Pattern.compile("[\\d]{1,}");
		if (yearPatternIndexInColHeading >= 0) {
			for (int c = 0; c < finalHeadings.length; c++) {
				if (null == endDatePeriodColHeadings[c])
					// we should check since it mught have already been assigned
					// an array while looking for ended period..

					endDatePeriodColHeadings[c] = new String[] { "-1", "" };
				// if year matches mm/dd/yy - done..
				String year = finalHeadings[c].split("\\|")[yearPatternIndexInColHeading];
				if (nlp.doesPatternMatch(year, MoDayYrSimplePattern)) {
					endDatePeriodColHeadings[c][1] = year;
					// System.xut.println("year is sufficient:" + year);
					continue; // for next column..
				} else if (monthPatternIndexInColHeading >= 0) {
					String month = finalHeadings[c].split("\\|")[monthPatternIndexInColHeading];
					// int dayIdx = StringUtils.indexOfAny(month, "0123456789");
					List<String> chDay;
					try {
						chDay = nlp.getAllMatchedGroups(month, MonthPatternSimple);
						if (null == chDay || chDay.size() == 0)
							continue;
						String chMonthName = getCorrectMonthName(chDay.get(0));
						chDay = nlp.getAllMatchedGroups(month, DayPattern);
						if (null == chDay || chDay.size() == 0)
							continue;
						// System.Xut.println("chDay day pattern: " + chDay);
						int chMonthNo = ArrayUtils.indexOf(MonthNamesArray, chMonthName);
						// System.xut.println("monthNo=" + chMonthNo);
						// if bsnWire there is no fye - so we use month/day
						String mn = "", dy = "";
						// System.Xut.println("fileType=="+fileType);

						if (String.valueOf(chMonthNo).length() < 2) {
							mn = "0" + String.valueOf(chMonthNo + 1);
							// System.Xut.println("mn :" + mn);
						}
						if (String.valueOf(chMonthNo).length() >= 2) {
							mn = String.valueOf(chMonthNo + 1);
							// System.xut.println("mn :" + mn);
						}

						if (String.valueOf(chDay.get(0)).length() < 2) {
							dy = "0" + String.valueOf(chDay.get(0));
							// System.xut.println("dy :" + dy);
						}
						if (String.valueOf(chDay.get(0)).length() >= 2) {
							dy = String.valueOf(chDay.get(0));
							// System.xut.println("dy :" + dy);
						}

						fye = (mn + dy.replaceAll("/[|/]", "")).replaceAll(" ", "");

						// System.xut.println("fye where bsnwire:" + fye);

						System.out.println("endDateXX:" + year + "-" + (chMonthNo + 1) + "-" + dy);
						if (dy.trim().length() < 2)
							dy = "0" + dy;
						if (chMonthNo + 1 < 10) {
							endDatePeriodColHeadings[c][1] = year + "-" + "0" + (chMonthNo + 1) + "-" + dy;

						} else {
							endDatePeriodColHeadings[c][1] = year + "-" + (chMonthNo + 1) + "-" + dy;

						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return endDatePeriodColHeadings;
	}

	public int getQMonthFromFYE(String fye, int chMonthNo) {
		if (fye == null || fye.length() < 4) {

			int qnull = 0;
			return qnull;
		}
		fye = fye.replaceAll(" ", "");

		// System.xut.println("fye2=" + fye + "\r chMonthNo=" + chMonthNo);
		if (fye.length() > 3 && !fye.substring(0, 2).contains("0") && !fye.substring(0, 2).contains("1")
				&& !fye.substring(0, 2).contains("2") && !fye.substring(0, 2).contains("3")
				&& !fye.substring(0, 2).contains("4") && !fye.substring(0, 2).contains("5")
				&& !fye.substring(0, 2).contains("6") && !fye.substring(0, 2).contains("7")
				&& !fye.substring(0, 2).contains("8") && !fye.substring(0, 2).contains("9"))
			return 0;

		int q1MoFromFYE = Integer.parseInt(fye.substring(0, 2)) + 2;
		int q2MoFromFYE = Integer.parseInt(fye.substring(0, 2)) + 5;
		int q3MoFromFYE = Integer.parseInt(fye.substring(0, 2)) + 8;
		int q4MoFromFYE = Integer.parseInt(fye.substring(0, 2)) - 1;
		int qtr = -1;

		// eg fye=1130. q1=10+2
		if (q1MoFromFYE > 11)
			q1MoFromFYE = q1MoFromFYE - 12;
		if (q2MoFromFYE > 11)
			q2MoFromFYE = q2MoFromFYE - 12;
		if (q3MoFromFYE > 11)
			q3MoFromFYE = q3MoFromFYE - 12;
		if (q4MoFromFYE > 11)
			q4MoFromFYE = q4MoFromFYE - 12;

		if (Math.abs(q1MoFromFYE - chMonthNo) <= 1 || Math.abs(q1MoFromFYE - chMonthNo) == 11) {
			return qtr = q1MoFromFYE;
		} else if (Math.abs(q2MoFromFYE - chMonthNo) <= 1 || Math.abs(q2MoFromFYE - chMonthNo) == 11) {
			return qtr = q2MoFromFYE;
		} else if (Math.abs(q3MoFromFYE - chMonthNo) <= 1 || Math.abs(q3MoFromFYE - chMonthNo) == 11) {
			return qtr = q3MoFromFYE;
		} else if (Math.abs(q4MoFromFYE - chMonthNo) <= 1 || Math.abs(q4MoFromFYE - chMonthNo) == 11) {
			return qtr = q4MoFromFYE;
		}
		return qtr;
	}

	public String getPreviousLines(String text, int indexFrom, int lineCount) {
		int nlIdx = text.lastIndexOf("\r\n", indexFrom); // from the 'indexFrom'
															// location, look
															// for a hard-return
															// - backwards.
		if (nlIdx >= 0) { // if found,
			for (int i = 0; i < lineCount; i++) { // go back so many lines as
													// required (lineCount)
				if (nlIdx >= 0)
					nlIdx = text.lastIndexOf("\r\n", nlIdx - 2);
				else
					break;
				// System.Xut.println("nlIdx=" + nlIdx);
			}
		}
		// System.Xut.println("nlIdx=" + nlIdx + ":");
		if (nlIdx >= 0) // if we have some good position, cut the text from that
						// location till where we started from - to get previous
						// lines..
			return text.substring(nlIdx, indexFrom);
		// System.Xut.println("previous line not found..");
		// this returns the text that is equal to the # of lines prior to the
		// beginning of the startIdx
		return "";

	}

	public int getMonthLastDate(int month, int year) {
		switch (month) {
		case Calendar.JANUARY:
		case Calendar.MARCH:
		case Calendar.MAY:
		case Calendar.JULY:
		case Calendar.AUGUST:
		case Calendar.OCTOBER:
		case Calendar.DECEMBER:
			return 31;
		case Calendar.APRIL:
		case Calendar.JUNE:
		case Calendar.SEPTEMBER:
		case Calendar.NOVEMBER:
			return 30;
		default: // Calendar.FEBRUARY
			return year % 4 == 0 ? 29 : 28;
		}
	}

	public String getCorrectMonthName(String monthName) {
		monthName = monthName.toLowerCase();
		if (monthName.startsWith("jan"))
			return "January";
		else if (monthName.startsWith("feb"))
			return "February";
		else if (monthName.startsWith("mar"))
			return "March";
		else if (monthName.startsWith("apr"))
			return "April";
		else if (monthName.startsWith("may"))
			return "May";
		else if (monthName.startsWith("jun"))
			return "June";
		else if (monthName.startsWith("jul"))
			return "July";
		else if (monthName.startsWith("aug"))
			return "August";
		else if (monthName.startsWith("sep"))
			return "September";
		else if (monthName.startsWith("oct"))
			return "October";
		else if (monthName.startsWith("nov"))
			return "November";
		else if (monthName.startsWith("dec"))
			return "December";
		return null;
	}

	public String limitStartGrp(String startGrp, int dataCols) throws IOException {
		NLP nlp = new NLP();
		// System.Xut.println("startGrp before limit==" + startGrp);
		// System.Xut.println("limit startgrp" + startGrp + "|limitStartGrp");
		String[] lines = startGrp.split("\r\n");
		int l = 0;
		List<String> tmpEndedList = null, tmpMonthList = null, tmpYearList = null, tmpDecimalList = null,
				tmpEndedSimpleList = null, tmpNumberList = null;
		for (; l < lines.length; l++) {
			tmpEndedList = null;
			tmpMonthList = null;
			tmpYearList = null;
			tmpDecimalList = null;
			tmpEndedSimpleList = null;
			tmpNumberList = null;

			if (l == 0) {
				tmpNumberList = nlp.getAllMatchedGroups(lines[0], tabNumberTabOrHardReturn);
			}

			if (!lines[l].contains("\t") && dataCols > 1) {
				// System.Xut
				// .println("startgrp3 line: No tab found and there are 2 data
				// columns: "
				// + lines[l]);
				tmpYearList = nlp.getAllMatchedGroups(lines[l], YearSimple);
				continue;
				// if no tab - it must be part of startGrp. It may however not
				// contain any CH patterns. But that is okay. This will catch
				// where CH is in 1st field
			}

			if (!lines[l].contains("\t") && dataCols == 1) {
				// System.xut
				// .println("startgrp3 line: No tab found and there is 1 data column: "
				// + lines[l]);
				// b/c 1 data column it is possible only CH data is in this line
				// so we set CH patterns.

				tmpEndedList = nlp.getAllMatchedGroups(lines[l], EndedHeadingPattern);
				tmpMonthList = nlp.getAllMatchedGroups(lines[l], MonthPattern);
				tmpYearList = nlp.getAllMatchedGroups(lines[l], YearSimple);
				tmpDecimalList = nlp.getAllMatchedGroups(lines[l], DecimalPattern);
				tmpEndedSimpleList = nlp.getAllMatchedGroups(lines[l], EndedTabSimplePattern);
				tmpNumberList = nlp.getAllMatchedGroups(lines[l], tabNumberTabOrHardReturn);
			}

			if (lines[l].contains("\t")) {
				// if tab - field after tab would contain CH, but not prior.
				// System.Xut.println("startgrp3 lines contains tab: " +
				// lines[l]);

				tmpNumberList = nlp.getAllMatchedGroups(lines[l], tabNumberTabOrHardReturn);
				// or just last 2 or 3 digits on line is a # preceded by tab
				// want to run above on entire line to catch tab prior to Number
				// System.Xut.println("dataCol="
				// + lines[l].substring(lines[l].indexOf("\t")));
				// System.Xut.println("lines[l].substr of tab"
				// + lines[l].substring(lines[l].indexOf("\t")));
				tmpEndedList = nlp.getAllMatchedGroups(lines[l].substring(lines[l].indexOf("\t")), EndedHeadingPattern);
				tmpMonthList = nlp.getAllMatchedGroups(lines[l].substring(lines[l].indexOf("\t")), MonthPattern);
				tmpYearList = nlp.getAllMatchedGroups(lines[l].substring(lines[l].indexOf("\t")), YearSimple);
				tmpDecimalList = nlp.getAllMatchedGroups(lines[l].substring(lines[l].indexOf("\t")), DecimalPattern);
				tmpEndedSimpleList = nlp.getAllMatchedGroups(lines[l].substring(lines[l].indexOf("\t")),
						EndedTabSimplePattern);
				// System.Xut.println("if tab numberList.size is:"
				// + tmpNumberList.size());
				// System.Xut.println("if tab tmpYearList.size is:"
				// + tmpYearList.size());
				// System.Xut.println("if tab tmpMonthList.size is:"
				// + tmpMonthList.size());
			}

			if (tmpEndedList.size() > 0 || tmpYearList.size() > 0 || tmpMonthList.size() > 0
					|| tmpDecimalList.size() > 0 || tmpEndedSimpleList.size() > 0 || tmpNumberList.size() < 1
			// CH can be
			// as of<tab>as of<tab>
			// June<tab>June<tab>
			// so tmpNumberList<0 needed otherwise loop stops b/c no match.
			) {
				// System.Xut.println("numberList.size:" +
				// tmpNumberList.size());
				// System.Xut.println("tmpYearList.size:" + tmpYearList.size());
				// System.Xut.println("tmpMonthList.size:" +
				// tmpMonthList.size());
				// System.Xut.println("startgrp limited lines[l]:" + lines[l]);
				continue;
			}

			// cks if CH pattern is present whenever there is a tab and if so
			// continues OR if there is no tmpNumber pattern to continue
			{
				// System.Xut.println("not part of startGrp: " + lines[l]);
				l--;
				break;
			}
		}

		if (l < lines.length) {
			for (; l >= 0; l--) {

				tmpEndedList = null;
				tmpMonthList = null;
				tmpYearList = null;
				tmpDecimalList = null;
				tmpEndedList = nlp.getAllMatchedGroups(lines[l], EndedHeadingPattern);
				tmpMonthList = nlp.getAllMatchedGroups(lines[l], MonthPattern);
				tmpYearList = nlp.getAllMatchedGroups(lines[l], YearSimple);
				tmpDecimalList = nlp.getAllMatchedGroups(lines[l], DecimalPattern);

				// System.Xut.println("find end of startgrp");
				if (//
				((tmpEndedList.size() >= 1 || tmpMonthList.size() >= 1 || tmpYearList.size() >= 1
						|| tmpDecimalList.size() >= 1) && lines[l].indexOf("\t") > 0)
						// won't catch where no tab
						|| (tmpMonthList.size() + tmpYearList.size() + tmpEndedList.size() >= 4)) {
					// if 2 or more of any 2 will catch if no tab (and add but
					// no #,#)?
					// System.Xut.println("line bef CH Match:" + lines[l]);
					l++;
					// System.Xut.println("line after CH match: " + lines[l]);
					break;
				}

				// below picks up where just 1 data column
				if ((tmpYearList.size() > 0 || tmpMonthList.size() > 0 || tmpEndedList.size() > 0) && dataCols == 1
				// && lines[l].length() < 5
				) {
					// if year and no tab and length is 4 or less
					// System.Xut.println("line bef CH Match:" + lines[l]);
					l++; // go to the next line..
					// System.xut.println("line after CH match: " + lines[l]);
					break;
				}
			}
		}

		lines = (String[]) ArrayUtils.subarray(lines, 0, l);
		startGrp = StringUtils.join(lines, "\r\n");
		// System.Xut.println("Corrected startGrp=" + startGrp
		// + "::end Corrected startGrp");
		return startGrp;
	}

	public String[] trimElements(String[] array) {
		// this is removing extra spaces at the beginning and end of element.
		for (int i = 0; i < array.length; i++)
			array[i] = array[i].replaceAll("^\\s+|\\s+$", "").replaceAll("\t", " ");
		return array;
	}

	public void getSentence(String line, int lineNo, List<String> tmpEndedStr, List<String> tmpMonthStr,
			List<String> tmpYearStr, int tableCount) throws SQLException {
		NLP nlp = new NLP();
		// if you reset the variables here then if there are multiple sentences
		// in 1 table it will reset the variables at each sentence found which
		// if 1st sentence for example had the period (three months) but the 2nd
		// did not - you loose the 1st as part of the finalheadings. I have for
		// now only reset these 3 variables when it is a new table. I could if
		// it becomes an issue add a 2nd set of variables for the 2nd sentence
		// (where tableCount is still same - sentenceCount doesn't seem to wrk).

		// sentenceEndedStr = null;
		// sentenceMonthStr = null;
		// sentenceYearStr = null;

		// System.xut.println("Ended size=" + tmpEndedStr.size() + " month size="
		// + tmpMonthStr.size() + " yr size=" + tmpYearStr.size() + ":"
		// + line.indexOf("\t"));

		// System.xut.println("line that is not part of column heading: " + line);

		// acc,tbl,sentCou,sent (added 1 b/c otherwise it starts at 0).
		int sentenceCount = lineNo + 1;

		line = line.replaceAll("ix", "six").replaceAll("IX", "SIX").replaceAll("ssix", "six").replaceAll("SSIX", "SIX");

		// ColumnHeadingForTextFiles ch = new ColumnHeadingForTextFiles(acc);
		// ch.getColumnHeadingFromTableSentence(line.trim(), tableCount);

		if (sentenceCount == 1) {
			tp_sentLine1 = line;
			tp_sentLine2 = "";
		}
		if (sentenceCount == 2) {

			tp_sentLine2 = tp_sentLine1 + " " + line;
			// System.xut.println("two lines merged of sentenceCount"
			// + tp_sentLine2 + " tp_sentLine1=" + tp_sentLine1);

			tp_sentLine1 = "";

			try {

				tmpEndedStr = nlp.getAllMatchedGroups(tp_sentLine2, EndedHeadingPattern);
				// for (int i = 0; i < tmpEndedStr.size(); i++) {
				// System.xut.println("tp_sent ended pattern match:"
				// + tmpEndedStr.get(i));
				//
				// }
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		if (tp_sentLine2.length() > 3) {
			tableSentence = tp_sentLine2;
		} else
			tableSentence = tp_sentLine1;

		// try {
		// MysqlConnUtils.executeQuery(qry);
		// } catch (SQLException e) {
		// e.printStackTrace();
		// }

		String yearMoOrEnd = "";
		if (tmpEndedStr.size() > 0) {
			sentenceEndedStr = tmpEndedStr;
			// System.xut.println("sentenceEndedStr=" + tmpEndedStr.get(0));
			for (int iE = 0; iE < Math.min(tmpEndedStr.size(), 1); iE++) {
				Matcher match = EndedHeadingPattern.matcher(line);
				yearMoOrEnd = null;

				@SuppressWarnings("unused")
				int row = 0;
				while (match.find()) {
					row++;
					yearMoOrEnd = match.group();

					// System.xut.println("6 fileDate=" + fileDate);
					iE++;
				}
			}
		}

		if (tmpYearStr.size() > 0) {
			sentenceYearStr = tmpYearStr;
			for (int iE = 0; iE < Math.min(tmpYearStr.size(), 1); iE++) {
				Matcher match = YearSimple.matcher(line);
				yearMoOrEnd = null;
				@SuppressWarnings("unused")
				int row = 0;
				// System.xut.println("7 fileDate=" + fileDate);
				while (match.find()) {
					row++;
					yearMoOrEnd = match.group();
					iE++;
				}
			}
		}

		if (tmpMonthStr.size() > 0) {
			sentenceMonthStr = tmpMonthStr;
			for (int iE = 0; iE < Math.min(tmpMonthStr.size(), 1); iE++) {
				Matcher match = MonthPatternSimple.matcher(line);
				// System.xut.println("line at tp_mo:" + line);
				yearMoOrEnd = null;
				int row = 0;
				// System.xut.println("8 fileDate=" + fileDate);
				while (match.find()) {
					row++;
					yearMoOrEnd = match.group();
					iE++;
				}
			}
		}
	}

	public String[] getColumnsHeadings(String headingSection, int dataCols, int tableCount)
			throws IOException, SQLException {

		NLP nlp = new NLP();

		// for each headingsection I want to pair headings across rows that are
		// the same column.

		String[] headLines = headingSection.split("\r\n");

		// System.Xut.println("headingSection (each line is headLines[i])="
		// + headingSection);
		// headLines = limitStartGrp

		// below is 2D array. 1st dimension is each heading pattern
		// (yr/mo/ended). The next dimension is the number of occurences of
		// each. We know there are 3 patterns -so we set the 1st dimens to [3]
		String[][] headingLinePortions = new String[3][]; // [headLines.length][];

		@SuppressWarnings("unused")
		List<String> tmpEndedStr = null, tmpMonthStr, tmpYearStr, tmpYearFalsePositives = null, tmpEndedColStr = null,
				tmpMonthColStr = null, tmpYearColStr = null, tmpMonthNamesLong = null, tmpMoDayYrSimpleColStr = null,
				tmpEndedStr2 = null, tmpMonthStr2, tmpYearStr2, tmpYearFalsePositives2 = null, tmpEndedColStr2 = null,
				tmpMonthColStr2 = null, tmpYearColStr2 = null, tmpMonthNamesLong2 = null,
				tmpMoDayYrSimpleColStr2 = null, tmpDecimalStr2 = null;

		// int maxCols = 0;
		// xx1: do pattern match from bottom of heading section.
		StringBuffer sbGoodHeadingText = new StringBuffer();
		for (int i = 0; i < headLines.length; i++) {
			// headlines is limit startGrp
			String line = headLines[i];
			String line2;
			if (i + 1 < headLines.length) {
				line2 = headLines[i + 1];
				tmpEndedStr2 = nlp.getAllMatchedGroups(line2, EndedHeadingPattern);
				tmpYearStr2 = nlp.getAllMatchedGroups(line2, YearOrMoDayYrPattern);
				tmpMonthStr2 = nlp.getAllMatchedGroups(line2, MonthPattern);
				tmpMonthNamesLong2 = nlp.getAllMatchedGroups(line2, MonthNamesLongPattern);
				tmpMoDayYrSimpleColStr2 = nlp.getAllMatchedGroups(line2, MoDayYrSimplePattern);
				tmpDecimalStr2 = nlp.getAllMatchedGroups(line2, DecimalPattern);

			} else {
				line2 = null;
				tmpEndedStr2 = null;
				tmpYearStr2 = null;
				tmpMonthStr2 = null;
				tmpMonthNamesLong2 = null;
				tmpMoDayYrSimpleColStr2 = null;
			}

			// System.Xut.println("line==: " + line + " lineNo: " + "\r\n" + i);
			// now for each line get pattern
			tmpEndedStr = nlp.getAllMatchedGroups(line, EndedHeadingPattern);
			// TODO: can add 'Percent or %' as a CH type here.
			// System.Xut.println("after line== tmpEndedStr: "
			// + tmpEndedStr.toString());

			tmpYearStr = nlp.getAllMatchedGroups(line, YearOrMoDayYrPattern);
			tmpMonthStr = nlp.getAllMatchedGroups(line, MonthPattern);
			tmpMonthNamesLong = nlp.getAllMatchedGroups(line, MonthNamesLongPattern);
			tmpMoDayYrSimpleColStr = nlp.getAllMatchedGroups(line, MoDayYrSimplePattern);
			String[] lineCols = null;
			// get matched grps for lineCols[0]
			if (line.indexOf("\t") > 0) {
				lineCols = line.split("\t");
				tmpEndedColStr = nlp.getAllMatchedGroups(lineCols[0], EndedHeadingPattern);
				tmpYearColStr = nlp.getAllMatchedGroups(lineCols[0], YearOrMoDayYrPattern);
				tmpMonthColStr = nlp.getAllMatchedGroups(lineCols[0], MonthPattern);

			}

			// ck lineStub==>
			if (lineCols != null && (//
			(tmpYearColStr.size() > 1 && tmpMonthColStr.size() > 0 && dataCols % tmpYearColStr.size() == 0
					&& dataCols % tmpMonthColStr.size() == 0 && dataCols != tmpYearStr.size()
					&& dataCols != tmpMonthStr.size()) || //
					(tmpYearColStr.size() > 0 && tmpMonthColStr.size() > 1 && dataCols % tmpYearColStr.size() == 0
							&& dataCols % tmpMonthColStr.size() == 0 && dataCols != tmpYearStr.size()
							&& dataCols != tmpMonthStr.size())
			//
			))

			// only remove matches from stub (lineCol[0]) if after removal CH
			// patterns are divisible into # dataCols and prior to removal the #
			// of year or mo matches is not equal to # of dataCols (you can
			// remove a stub that has 2 and leave 2 and have 4 data cols - hence
			// both checks)

			{
				line = line.substring(line.indexOf("\t"));
				tmpEndedColStr = nlp.getAllMatchedGroups(line, EndedHeadingPattern);
				tmpYearColStr = nlp.getAllMatchedGroups(line, YearOrMoDayYrPattern);
				tmpMonthColStr = nlp.getAllMatchedGroups(line, MonthPattern);
				tmpMonthNamesLong = nlp.getAllMatchedGroups(line, MonthNamesLongPattern);
				tmpEndedStr = tmpEndedColStr;
				tmpYearStr = tmpYearColStr;
				tmpMonthStr = tmpMonthColStr;

			}
			// System.Xut.println("tmpmonthStr=" + tmpMonthStr.toString());

			tmpYearFalsePositives = nlp.getAllMatchedGroups(line, YearFalsePositives);

			// constraint for line (if condition met line is removed)
			// System.Xut.println("line at CH: " + line + " headLines.length: "
			// + headLines.length + " line#: " + i);

			if ((tmpEndedStr.size() > 0 || tmpYearStr.size() > 0 || tmpMonthStr.size() > 0) && (
			// start:

			/*
			 * TODO: by combining analysis of line2 while on line1 - the sentence parser
			 * will look to line 1 more than it needs to but should end up without any
			 * results b/c no patterns will have matched. This may later result in a
			 * refinement where you break line2 out from the mix and still use it as a
			 * condition to parse line1 but also add some conditions to line1 (that are
			 * weaker than normal) that can weed out f/p parsing into tp_end/mo/yr.
			 */

			((line.toLowerCase().contains("and ") || (line2 != null && line2.toLowerCase().contains("and "))
					|| line.toLowerCase().contains(" and") || (line2 != null && line2.toLowerCase().contains(" and"))
					|| line.toLowerCase().contains("for the ")
					|| (line2 != null && line2.toLowerCase().contains("for the ")))
					&& (headLines.length - i > 3 && line.indexOf("\t") < 3)) || //
					(line.indexOf("\t") < 0 && dataCols > 2 && line.length() > 15
							&& (line.substring(0, 4).toLowerCase().contains("and ")
									|| line.substring(0, 4).toLowerCase().contains("and ")))

					|| (line.indexOf("\t") < 1 && i < 3 && dataCols > 1 && line.length() > 20
							&& ((tmpYearStr.size() > 1 && tmpMonthStr.size() > 0)
									|| (tmpMonthStr.size() > 1 && tmpYearStr.size() > 0)))
					// end: (i=0=tblnm, i=1=tp_sent)
					// 2 rows dataCo & < 4 CH match

					// line2=>>>xx
					|| (line2 != null && line2.indexOf("\t") < 1 && i < 2 && dataCols > 1 && line2.length() > 20
							&& ((tmpMonthStr2 != null && tmpYearStr2 != null && tmpYearStr2.size() > 1
									&& tmpMonthStr2.size() > 0)
									|| (tmpMonthStr2 != null && tmpYearStr2 != null && tmpMonthStr2.size() > 1
											&& tmpYearStr2.size() > 0)))

					|| (tmpYearFalsePositives.size() > 0 && headLines.length - i > 3)// end
					// OR fp w/n 2 rows of tblname
					// line2
					|| (tmpYearFalsePositives2 != null && tmpYearFalsePositives2.size() > 0 && headLines.length - i > 2)
					|| (tmpYearStr.size() >= 2 && line.indexOf("\t") < 1 && tmpMonthStr.size() >= 1
							&& headLines.length - i > 3)
					// end: no tab and more than 2yr and at least 1mo
					// (odd to have in 1 line 2 year and 1 mo)
					// line2
					|| (tmpYearStr2 != null && tmpMonthStr2 != null && tmpYearStr2.size() >= 2
							&& line2.indexOf("\t") < 1 && tmpMonthStr2.size() >= 1 && headLines.length - i > 3)

					// line2
					|| (tmpMonthNamesLong2 != null && tmpYearStr2 != null && line2 != null
							&& tmpMonthNamesLong2.size() > 0 && tmpYearStr2.size() > 0 && line2.indexOf("\t") < 1
							&& headLines.length - i > 3 && dataCols > 1)

					|| (tmpMonthNamesLong.size() > 0 && tmpYearStr.size() > 0 && line.indexOf("\t") < 1
							&& headLines.length - i > 3 && dataCols > 1)
					// end: 1 month long name, at least 1yr and no tab.
					// Add
					// line.length constraint?
					// line2
					|| (line2 != null && tmpYearStr2 != null && tmpMonthStr2 != null && tmpEndedStr2 != null
							&& line2.length() > 35 && line2.indexOf("\t") < 0 && dataCols > 1
							&& ((tmpYearStr2.size() + tmpMonthStr2.size() + tmpEndedStr2.size()) > 1
									&& tmpYearStr2.size() < 3 && tmpMonthStr2.size() < 3 && tmpEndedStr2.size() < 3))//
					|| (tmpDecimalStr2 != null && tmpDecimalStr2.size() > 0 && i < 2 && line.length() < 5
							&& headLines.length - i > 3) // <<==next line (Str2)
															// is dec
															// pattern and line
															// prior is
					// either line 1 or 2 and there are at least 3 more
					// lines left in heading pattern. Then it is f/p. Or
					// if 2
					// dataCols line length is less than 7 characters
					|| (line.length() < 5 && i < 2 && headLines.length - i > 4)
					// picksup stranded year on line1/2 and where there are at
					// least 4 mo CH lines to be able to pick it up. Ideally I'd
					// want to identify if it was a stranded year and if on
					// subsequent rows there are 2 or more years.
					|| (line.length() > 35 && line.indexOf("\t") < 0 && dataCols > 1
							&& ((tmpYearStr.size() + tmpMonthStr.size() + tmpEndedStr.size()) > 1
									&& tmpYearStr.size() < 3 && tmpMonthStr.size() < 3 && tmpEndedStr.size() < 3))))
			// OR if > 2 year ptrns; at least 1 mo and
			// no tabs - not part of CH
			// if full length MonthName and no tab and
			// year - discard eg:
			// CONDENSED BALANCE SHEET, unaudited, at
			// December 31, 1999:

			// TODO: ideally you'd run discard check after you get the end of
			// the limitStartGrp. I'd then work up from bottom and stop based on
			// filter constraint b/c where [i] is a row we can measure against
			// last row in limitStartGrp length. That will tell us with
			// exactness CHs closest to dataCol and my filters may work better

			{
				// this saves str for potential later use as next line that is
				// passed will reset these to zero and we may need to use this
				// CH data should CH data not be captured in primary way
				// System.xut.println("this is not part of CH: " + line
				// + ": tableCount: " + tableCount);
				// System.xut.println("headLines.length: " + headLines.length
				// + " lineNo:" + i);
				// System.xut.println("tmpMonthStr parsed to getSentence="
				// + tmpMonthStr);
				if (null != tableSentence && tableSentence.length() > 175)
					continue;
				getSentence(line, i, tmpEndedStr, tmpMonthStr, tmpYearStr, tableCount);
			} else
				// if constraint above not met - we keep the line and append
				sbGoodHeadingText.append(new String(line)).append("\r\n");
		}
		// System.Xut
		// .println("sbGoodHeadingText: " + sbGoodHeadingText.toString());
		// System.Xut.println("after sbGoodHeadingText tmpEndedStr: "
		// + tmpEndedStr.toString());
		// out of for loop (JUST lines with CH patterns).
		goodHeadingSection = sbGoodHeadingText.toString().replaceAll("(?i)TWLEVE", "TWELVE")
				.replaceAll("(?i)for the fiscal quarter.{0,3}\t", "Three Month\t")
				.replaceAll("(?i)(?<=(six|three|nine))months?", " Months")
				.replaceAll("(?i)1.{1,3}Quarter", "Three Months").replaceAll("(?i)3.{1,3}Quarters", "Nine Months")
				.replaceAll("(?i)2.{1,3}Quarters", "Six Months").replaceAll("(?i)4.{1,3}Quarters", "Twelve Month");
		if (!goodHeadingSection.toLowerCase().contains("month") && !goodHeadingSection.toLowerCase().contains("week")) {
			goodHeadingSection = goodHeadingSection.replaceAll("(?i)(two|three|four|five).{1,3}years?", " Year");
		}

		if (!goodHeadingSection.toLowerCase().contains("week")) {

			goodHeadingSection = goodHeadingSection.replaceAll("(?i)[\r\n]{1} ?THREE ?[\t]{1}", "\rThree Month\t")
					.replaceAll("(?i)[\r\n]{1} ?Six ?[\t]{1}", "\rSix Month\t")
					.replaceAll("(?i)[\r\n]{1} ?Nine ?[\t]{1}", "\rNine Month\t")
					.replaceAll("(?i)[\r\n]{1} ?Twelve ?[\t]{1}", "\rTwelve Month\t")

					.replaceAll("(?i)[\t]{1} ?THREE ?[\t]{1}", "\tThree Month\t")
					.replaceAll("(?i)[\t]{1} ?Six ?[\t]{1}", "\tSix Month\t")
					.replaceAll("(?i)[\t]{1} ?Nine ?[\t]{1}", "\tNine Month\t")
					.replaceAll("(?i)[\t]{1} ?Twelve ?[\t]{1}", "\tTwelve Month\t")

					.replaceAll("(?i)[\t]{1} ?THREE ?[\r\n]{1}", "\tThree Month\r")
					.replaceAll("(?i)[\t]{1} ?Six ?[\r\n]{1}", "\tSix Month\r")
					.replaceAll("(?i)[\t]{1} ?Nine ?[\r\n]{1}", "\tNine Month\r")
					.replaceAll("(?i)[\t]{1} ?Twelve ?[\r\n]{1}", "\tTwelve Month\r");
			// System.Xut
			// .println("replacing three,six,nine,twelve sandwiched between
			// tab/hard return with three month, six month etc.");
		}

		goodHeadingSection = goodHeadingSection
				.replaceAll("(?i)three(.{1,2}fiscal)?.{1,2}quarters(.{1,2}end[eding]{2,3})?", "Nine Months Ended)")
				.replaceAll("(?i)two(.{1,2}fiscal)?.{1,2}quarters(.{1,2}end[eding]{2,3})?", "Six Months Ended)")
				.replaceAll("(?i)four(.{1,2}fiscal)?.{1,2}quarters(.{1,2}end[eding]{2,3})?", "twelve months ended");

		// System.Xut.println("after text replacements goodHeadingSection="
		// + goodHeadingSection);
		// if not isHtml, and
		heading = "good";
		// [when you have year pattern on two consecutive rows and
		// where for at least 1 of these rows you also have a month pattern]

		// if (!isHtml) {
		// remove if !isHtml condition b/c
		// https://www.sec.gov/Archives/edgar/data/1335190/000095012310030964/c98648e10vk.htm
		// was html where table structure results in same problem - ie., each
		// cell had just 1 line of data versus letting it wrap in the cell
		// System.Xut.println("# of dataCols: " + dataCols);
		String[] ghLines = goodHeadingSection.split("\r\n");
		for (int i = 1; i + 1 < ghLines.length; i++) {
			// start at 1 - which is row2
			// System.Xut.println("ghLines:" + ghLines[i]);
			if (nlp.doesPatternMatch(ghLines[i], YearSimple) && nlp.doesPatternMatch(ghLines[i + 1], YearSimple)
					&& (nlp.doesPatternMatch(ghLines[i + 1], AuditedRestatedNotFollowedByTabPattern)
							|| nlp.doesPatternMatch(ghLines[i + 1], AuditedRestatedNoParenFollowedByTabPattern))
					&& ghLines[i + 1].indexOf("\t") > 0 && ghLines[i - 1].length() < 17
					&& ghLines[i - 1].indexOf("\t") < 0
					// if yr on row1&row2&; no tab on row1 (if tab in row1
					// CH
					// ok) and (audited/restated) at end of line row2 it=bad
					// row1: June 30,
					// row2: September 30, 2000
					// row3: 1999 (unaudited)
					// in temp99.txt there is no tab on row1
					// not sure I need below to be captured. Need to test.
					&& (nlp.doesPatternMatch(ghLines[i - 1], MonthPatternSimple)
							|| nlp.doesPatternMatch(ghLines[i], MonthPatternSimple)
							|| nlp.doesPatternMatch(ghLines[i + 1], MonthPatternSimple))

			) {
				// System.Xut.println("bad lines: " + ghLines[i - 1] + "\r\n"
				// + ghLines[i] + "\r\n" + ghLines[i + 1]);
				// table heading has problematic pattern potentially.
				heading = "bad";
			}
			// System.Xut.println("dataCols: " + dataCols);
			if (dataCols > 2) {
				Pattern tabPattern = Pattern.compile("\t");
				List<String> tmpYrlist, tmpYrlist2, tmpTab, tmpTab2;
				tmpYrlist = nlp.getAllMatchedGroups(ghLines[i], YearSimple);
				tmpYrlist2 = nlp.getAllMatchedGroups(ghLines[i + 1], YearSimple);
				tmpTab = nlp.getAllMatchedGroups(ghLines[i], tabPattern);
				tmpTab2 = nlp.getAllMatchedGroups(ghLines[i + 1], tabPattern);

				if (tmpYrlist.size() > 1 && tmpYrlist2.size() > 1 && tmpTab.size() > 1 && tmpTab2.size() > 1) {
					// System.Xut.println("heading bad datacols>2");
					// System.Xut.println("line: " + ghLines[i]);
					// System.Xut.println("line2: " + ghLines[i + 1]);
					heading = "bad";
				}
			}

			if (heading == "bad" && dataCols == 2) {
				heading = "reversCH";
			}
		}
		// }

		// nlp.getAllMatchedGroups will return the multiple (list) of strings
		// that
		// match the pattern passed from goodHeadingSection string (e.g., 3
		// months ended). The ".toArray is converting this list (technically not
		// an array) to an array so it can the be assigned to the array
		// headLinesPortions[]. This does it for all 3 and each is done
		// independently. None are joined yet.

		String[] ghsLines = goodHeadingSection.split("\r");
		// 2D ary (1st is each CH type: mo/yr/ended and 2nd is
		// instance of each. This loop will fetch each pattern in
		// reverse order (from bot to top of goodheading)
		for (int i = ghsLines.length - 1; i >= 0; i--) {
			// the .addAll appends the next array to the existing (ary1,ary2)
			headingLinePortions[0] = (String[]) ArrayUtils.addAll(headingLinePortions[0],
					trimElements(nlp.getAllMatchedGroups(ghsLines[i], EndedHeadingPattern).toArray(new String[0])));
			// System.Xut.println("hlPortions ended prior to trimming="
			// + Arrays.toString(headingLinePortions[0]));

			headingLinePortions[1] = (String[]) ArrayUtils.addAll(headingLinePortions[1],
					trimElements(nlp.getAllMatchedGroups(ghsLines[i], MonthPattern).toArray(new String[0])));
			// System.Xut.println("hlPortions mo prior to trimming="
			// + Arrays.toString(headingLinePortions[1]));

			headingLinePortions[2] = (String[]) ArrayUtils.addAll(headingLinePortions[2],
					trimElements(nlp.getAllMatchedGroups(ghsLines[i], YearOrMoDayYrPattern).toArray(new String[0])));
			// System.Xut.println("hlPortions yr prior to trimming="
			// + Arrays.toString(headingLinePortions[2]));
		}

		StringBuffer sb = new StringBuffer();
		// Above has retrieved all patterns across all goodheading
		// lines in order of lines closest to table (bottom of
		// goodSection heading) first. Any mo/ended/yr patterns in
		// excess of data cols are discarded.

		colHeadingPatternAtEachLine = "|";
		int pCnt = 0;
		for (int i = 0; i < headingLinePortions.length; i++) {
			// for each 1d ary (each CH pattern type) get 2d ary
			// from 0 to max # of cols or if less # of 2d ary
			headingLinePortions[i] = (String[]) ArrayUtils.subarray(headingLinePortions[i], 0,
					Math.min(dataCols, headingLinePortions[i].length));
			// System.Xut.println("(mo/yr/end) trimmed to # of data cols:"
			// + Arrays.toString(headingLinePortions[i]));
			sb.append(StringUtils.join(headingLinePortions[i], "\t\r\n")).append("\t\r\n");

			if (headingLinePortions[i].length > 0) {
				pCnt++;
			}

			colHeadingPatternAtEachLine = colHeadingPatternAtEachLine
					// +"L"+ (i+1)
					// + ":"
					+ nlp.getTableSentencePatterns(Arrays.toString(headingLinePortions[i]), (pCnt) + "");

			colHeadingPatternAtEachLine = colHeadingPatternAtEachLine.replaceAll("[\\|]{2,}", "\\|");
		}

		Pattern pKnt = Pattern.compile("\\dP:");
		Pattern yCnt = Pattern.compile("\\dY:");
		Pattern mCnt = Pattern.compile("\\dM:");
		String pCnts, yCnts, mCnts;

		int pI = 0, yI = 0, mI = 0;

		if (null != nlp.getAllIndexEndLocations(colHeadingPatternAtEachLine, pKnt)
				&& nlp.getAllIndexEndLocations(colHeadingPatternAtEachLine, pKnt).size() > 0)
			pI = nlp.getAllIndexEndLocations(colHeadingPatternAtEachLine, pKnt).size();

		if (null != nlp.getAllIndexEndLocations(colHeadingPatternAtEachLine, yCnt)
				&& nlp.getAllIndexEndLocations(colHeadingPatternAtEachLine, yCnt).size() > 0)
			yI = nlp.getAllIndexEndLocations(colHeadingPatternAtEachLine, yCnt).size();

		if (null != nlp.getAllIndexEndLocations(colHeadingPatternAtEachLine, mCnt)
				&& nlp.getAllIndexEndLocations(colHeadingPatternAtEachLine, mCnt).size() > 0)
			mI = nlp.getAllIndexEndLocations(colHeadingPatternAtEachLine, mCnt).size();

		pCnts = "pCnt:" + pI;
		yCnts = "yCnt:" + yI;
		mCnts = "mCnt:" + mI;

		colHeadingPatternAtEachLine = nlp.getTableSentencePatterns(colHeadingPatternAtEachLine, "") + pCnts + "|"
				+ yCnts + "|" + mCnts;

		// System.Xut.println("colHeadingPatternAtEachLine=="
		// + colHeadingPatternAtEachLine);

		// now we have stored each of the 3 patterns in 1st dimension of the 2
		// dimensional array as [0],[1],[2]. In each of array [0],[1] and [2]
		// are the instances of those matched patterns (the 2nd dimensional
		// array).

		// System.Xut.println("line portions matched heading
		// pattern:"+tmpHeadLocs.size()+", str="+
		// Arrays.toString(headingLinePortions[i]));

		// pk: FROM THIS POINT ONWARD, THE HEADING MECHANISM REMAIN SAME

		int maxPortions = 0;
		// remove empty line portions from headingLinePortions
		// create a List of type String called headingLinePortionsList
		List<String[]> headingLinePortionsList = new ArrayList<String[]>();
		for (String[] lineParts : headingLinePortions) {
			if (null != lineParts && lineParts.length > 0) {
				headingLinePortionsList.add(lineParts);
				if (maxPortions < lineParts.length)
					maxPortions = lineParts.length;
				// As it loops through it compares length of each line - one
				// with most 2nd dimensional arrays will have greatest length
				// and maxPortions will equal that integer value.
				// if linePart.length <0 (no match) so only matched patterns are
				// added. All patterns matched are now in
				// headingLinePortionsList

			}
		}
		// size() returns the number of elements in the List<String>. Each
		// element in the list is equal to matched patterns for each pattern (up
		// to 3 elements). The length of each element is the # of matches.

		for (int l = 0; l < headingLinePortionsList.size();) {
			// the for loop will check each array (l).
			if ((maxPortions % headingLinePortionsList.get(l).length) > 0) {
				// the if condition say if maxP/l has a remainder remove that
				// element from the List - see headingLinePortionsList.remove(l)
				// at bottom
				String[] ary = headingLinePortionsList.get(l);
				// returns the element of the List at the l position
				int b = l + 1;
				// System.xut.println("tp_sent:" + acc + "||" + tableCount + "||"
				// + b + "||" + "remainder=-1: " + Arrays.toString(ary));
				// System.xut.println("9 fileDate=" + fileDate);
				// System.xut.println("10 fileDate=" + fileDate);
				@SuppressWarnings("unused")
				String tableName = "tp_mo" + getTableNamePrefixYearQtr(fileDate);
				if (nlp.doesPatternMatch(ary[0], YearOrMoDayYrPattern))
					tableName = "tp_yr" + getTableNamePrefixYearQtr(fileDate);
				else if (nlp.doesPatternMatch(ary[0], EndedHeadingPattern))
					tableName = "tp_end" + getTableNamePrefixYearQtr(fileDate);
				for (int hp = 0; hp < ary.length; hp++) {
				}

				// instead of removing the remainder portions we insert the
				// first and last elements and put a dummy holder for any
				// inbetween (eg, where you
				// have 3 Columns but have found a CH pattern that has only 2
				// instances and therefore can't be paired logically with each
				// of the 3 columns. but we know the first and last columns go
				// with first and last of the 2 remainder pattern matched).
				String[] newAry = new String[maxPortions];
				// maxPortions is # of CHs. new String[#] is specifying the size
				// of the array. We need to specify the size of the newAry so
				// that the last (and first) element will align later when we
				// merge them. if 2 remainders and 5CHs - we know first and last
				// of each ary.
				// endDate if incomplete should equal - '0000-00-00'
				newAry[0] = ary[0];
				newAry[newAry.length - 1] = ary[ary.length - 1];
				for (int m = 1; m < newAry.length - 1; m++)
					newAry[m] = "0000";
				headingLinePortionsList.remove(l);
				headingLinePortionsList.add(l, newAry);
			} else
				l++;
		}
		// once loop is complete heading pattern List is complete. Contains just
		// pattern matches. Constraint requires each pattern's match count be
		// divisible into pattern's highest match count. This remove some goood
		// matches: eg, where 3 data columns - row 1 CH1#1 applies to data Col1
		// and row 1 row 2 CH1#2 applies to Col2 and Col3. CH1 #1/#2 are 2
		// matches of same pattern (say month pattern). And CH2 occurs 3 times
		// and is year pattern. Ch3 = maxMatches = 3. CH2 = 2 matches. 3/2 is
		// remainder so 2 discarded. This is rare. Otherwise the above appears
		// not to capture f/p but exclude positives (has f/n).

		// sort by array by element size, desc order - so biggest array is
		// first (eacharray contains a number of items - each item is 1 group -
		// (array length is the count of pattern matches)

		// this could be [0], [1] and [2] or just [0],[1] or [0]...
		if (headingLinePortionsList.size() == 0 || headingLinePortionsList.get(0).length == 0) {
			// System.Xut.println("this is null2");
			return null;
			// we don't have correct heading - so goes back starting of
			// loop. break goes out of the loop.
		}

		// here you convert the list back to the array.
		headingLinePortions = headingLinePortionsList.toArray(new String[0][]);

		// for (String[] ary : headingLinePortions)
		// System.Xut.println("headingLinePortions:" + Arrays.toString(ary));
		// determine if headings portions we got are multiples of each
		// other so can be merged
		// else discard the portion

		String[] finalHeadings = headingLinePortions[0];
		// start w/ [0] b/c that is the pattern with most matches.

		try {
			for (int i = 1; i < headingLinePortions.length; i++) {
				// System.Xut.println("headingLinePortions[i]"
				// + ArrayUtils.toString(headingLinePortions[i]));

				finalHeadings = getMergedHeadings(finalHeadings, StringUtils.join(headingLinePortions[i], "\t"));
				// System.Xut
				// .println("finalHeadnigs after 'getMergedHeadings' method="
				// + Arrays.toString(finalHeadings));

				// starting at next instance of headingLinePortions array [1]
				// merge to [0]. StringUtils.join is creating a string from the
				// instance of the array with each instance of 2nd dimensional
				// array separated by tab
			}
		} catch (Exception e) {
			// System.xut.println("getMergedHeadings error exception...." + e);
		}

		// System.Xut.println("Final merged headings: "
		// + Arrays.toString(finalHeadings));
		if (!nlp.doesPatternMatch(finalHeadings[0], EndedHeadingPattern)
				// if finalHeadings doesn't have ended pattern
				&& null != sentenceEndedStr && sentenceEndedStr.size() > 0
				// and we have ended pattern in sentence
				&& (finalHeadings.length % sentenceEndedStr.size()) == 0) {
			// System.xut.println("merge sentenceEndedStr="
			// + sentenceEndedStr.get(0));
			// and sentence ended patterns size has no remainder on final
			// heading count

			// System.xut
			// .println("sentenceEndedStr prior to finalHeadings determination="
			// + sentenceEndedStr);
			finalHeadings = getMergedHeadings(finalHeadings,
					StringUtils.join(sentenceEndedStr.toArray(new String[0]), "\t"));
			// System.Xut
			// .println("finalHeadings AFTER merging sentence Ended pattern:"
			// + Arrays.toString(finalHeadings));
		}

		if (!nlp.doesPatternMatch(finalHeadings[0], MonthPattern) && null != sentenceMonthStr
				&& sentenceMonthStr.size() > 0 && (finalHeadings.length % sentenceMonthStr.size()) == 0) {
			// System.Xut.println("sentenceMonthStr="
			// + Arrays.toString(sentenceMonthStr.toArray()));
			finalHeadings = getMergedHeadings(finalHeadings,
					StringUtils.join(sentenceMonthStr.toArray(new String[0]), "\t"));
			// System.Xut
			// .println("finalHeadings AFTER merging sentence Ended pattern:"
			// + Arrays.toString(finalHeadings));
		}

		if (!nlp.doesPatternMatch(finalHeadings[0], YearSimple)
				// if finalHeadings doesn't have ended pattern
				&& null != sentenceYearStr && sentenceYearStr.size() > 0
				// and we have ended pattern in sentence
				&& (finalHeadings.length % sentenceYearStr.size()) == 0) {
			// and no remainder on final heading count
			finalHeadings = getMergedHeadings(finalHeadings,
					StringUtils.join(sentenceYearStr.toArray(new String[0]), "\t"));
			// System.Xut
			// .println("finalHeadings AFTER merging sentence Ended pattern:"
			// + Arrays.toString(finalHeadings));
		}

		return finalHeadings;
	}

	public String[] getMergedHeadings(String[] firstLineHeads, String headingLine2) {
		// System.Xut.println("headingLine2: " + headingLine2);

		String[] secondLineHeads = headingLine2.split("\t");
		int noOfColGroups = secondLineHeads.length / firstLineHeads.length;
		if (secondLineHeads.length < firstLineHeads.length)
			noOfColGroups = firstLineHeads.length / secondLineHeads.length;
		if (noOfColGroups == 0) {
			// System.Xut.println("noOfColGroups=0 -- returns firstLineHeads:"
			// + Arrays.toString(firstLineHeads));
			return firstLineHeads;
		}

		// System.Xut.println("noOfColGroups: " + noOfColGroups);
		// System.Xut.println("secondLineHeads.length: " +
		// secondLineHeads.length);
		// System.Xut.println("firstLineHeads.length: " +
		// firstLineHeads.length);
		// System.Xut
		// .println("firstLineHeads: " + Arrays.toString(firstLineHeads));
		// System.Xut.println("secondLineHeads: "
		// + Arrays.toString(secondLineHeads));

		// <==firstLineHeads is pattern that has most matches so shoud always be
		// 1st/2nd

		if (secondLineHeads.length >= firstLineHeads.length) {
			// could be 2nd=1st. if so merge each respective array of each
			for (int i1 = 0, i2 = 0; i2 < secondLineHeads.length; i2++) {
				// System.Xut.println("1.sLH: " + secondLineHeads[i2]);
				// System.Xut.println("1.fLH: " + firstLineHeads[i1]);
				secondLineHeads[i2] = firstLineHeads[i1].replaceAll("  ", " ") + "|"
						+ secondLineHeads[i2].replaceAll("  ", " ");
				if (((i2 + 1) % noOfColGroups) == 0)
					// this is just a check to increment i1 so long as i2+1
					// returns remainder b/c when it does it is 1 over and gone
					// too far whereby i2 is not <.. and then it exits loop and
					i1++;
			}
			return secondLineHeads;
		} else if (firstLineHeads.length > secondLineHeads.length && firstLineHeads.length > 0
				&& secondLineHeads.length > 0) {
			// System.Xut.println("FLH.length:" + firstLineHeads.length);
			for (int i1 = 0, i2 = 0; i1 < firstLineHeads.length; i1++) {
				firstLineHeads[i1] = "|" + secondLineHeads[i2].replaceAll("  ", " ") + "|"
						+ firstLineHeads[i1].replaceAll("  ", " ");
				// System.Xut.println("2.sLH: " + secondLineHeads[i2]);
				// System.Xut.println("2.fLH: " + firstLineHeads[i1]);
				if (((i1 + 1) % noOfColGroups) == 0)
					i2++;
			}
		}
		return firstLineHeads;
	}

	public int getEndOfTableLocation(String text, int startIdx) throws IOException {

		NLP nlp = new NLP();

		int maxEmptyLinesToMarkEndTable = 4;
		// int maxEmptyLinesToMarkEndTable2 = 6;
		// 2nd is needed to reset
		// end of heading is where we want to start.
		int endOfTableLoc = Math.min(startIdx + 1, text.length()), linesRead = 0, count = 0;
		String line;
		String lines[] = null;
		lines = text.split("\r\n");
		int nextRowStart = text.indexOf("\n", startIdx);
		while (nextRowStart >= 0 && (nextRowStart + 20) < text.length()) {
			count++; // lineNo
			linesRead++; // increasing 1
			line = text.substring(startIdx, nextRowStart);
			// System.Xut.println("line::" + line);

			List<String> tmpEndedList = null, tmpMonthList = null, tmpYearList = null, tmpDecimalList = null,
					tmpEndedSimpleList = null//
					, tmpNumberList = null, tmptabNumbertabNumberList = null, tmpTabNumberNoYearList = null;

			if (line.indexOf("\t") > 0) {

				tmpMonthList = nlp.getAllMatchedGroups(line.substring(line.indexOf("\t")), MonthPattern);
				tmpYearList = nlp.getAllMatchedGroups(line.substring(line.indexOf("\t")), YearSimple);
				tmpEndedList = nlp.getAllMatchedGroups(line.substring(line.indexOf("\t")), EndedHeadingPattern);
				tmpEndedSimpleList = nlp.getAllMatchedGroups(line.substring(line.indexOf("\t")), EndedTabSimplePattern);
				tmpTabNumberNoYearList = nlp.getAllMatchedGroups(line.substring(line.indexOf("\t")), tabNumberNoYear);
			}

			// dataCol=lines[l].substring(lines[l].indexOf("\t"))
			tmpNumberList = nlp.getAllMatchedGroups(line, tabNumberTabOrHardReturn);
			tmptabNumbertabNumberList = nlp.getAllMatchedGroups(line, tabNumbertabNumber);
			// above needs to run on entire line b/c tab prior to number
			tmpDecimalList = nlp.getAllMatchedGroups(line, DecimalPattern);
			// above must be run on entire line
			Matcher tableNameMatch = TableNamePattern.matcher(line);

			maxEmptyLinesToMarkEndTable = 4;
			if (count < 10)
				maxEmptyLinesToMarkEndTable = 6;
			if (tmpNumberList.size() > 0 || tmptabNumbertabNumberList.size() > 0
					|| (line.substring(Math.max(line.length() - 2, 0), line.length()).contains(":")
							&& line.indexOf("\t") < 1)) {
				linesRead = 0;
				endOfTableLoc = nextRowStart;
				// if rn= "Current liabilities:" it is rnh - so reset lineRead=0
			}
			startIdx = nextRowStart + 1;
			if (linesRead >= maxEmptyLinesToMarkEndTable
					&& !line.substring(Math.max(line.length() - 2, 0), line.length()).contains(":")) {
				// if line ends with ":" - it is likely rnh
				// System.Xut.println("endOfTable line maxrow count met::" +
				// line);
				break;
			}

			if (line.indexOf("\t") > 0 && ((tmpYearList.size() > 0 || tmpMonthList.size() > 0
					|| tmpEndedSimpleList.size() > 0 || tmpEndedList.size() > 0))
					&& tmpTabNumberNoYearList.size() < 1) {
				// System.xut.println("endOfTablex met (year/mo/ended found)::"
				// + line);
				break;
			}

			if ((tableNameMatch.find() && (!line.toUpperCase().contains("LIABILIT")
					&& !line.toUpperCase().contains("EQUITY") && line.toUpperCase().contains("STATEMENT") && count > 5))
					// above OR below
					|| (tableNameMatch.find() && (lines[Math.min(lines.length - 1, count + 1)].toLowerCase()
							.contains("in million|in thousand")
							|| lines[Math.min(lines.length - 1, count + 2)].toLowerCase()
									.contains("in million|in thousand")
							|| lines[Math.min(lines.length - 1, count + 3)].toLowerCase()
									.contains("in million|in thousand")
							|| lines[Math.min(lines.length - 1, count + 4)].toLowerCase()
									.contains("in million|in thousand")

							|| lines[Math.min(lines.length - 1, count + 1)].contains("audited")
							|| lines[Math.min(lines.length - 1, count + 2)].toLowerCase().contains("audited")
							|| lines[Math.min(lines.length - 1, count + 3)].toLowerCase().contains("audited")
							|| lines[Math.min(lines.length - 1, count + 4)].toLowerCase().contains("audited"))))
			// above can look ahead if tableName match found to see if decimal
			// pattern is present
			{
				// System.xut.println("endOfTable line w/ tblNm::" + line);
				break;
			}

			if (linesRead > 2 && line.length() < 4 && !line.contains("199") && !line.contains("20")) {
				// System.xut
				// .println("endOfTable line: line.length<4 and linesRead>2::"
				// + line);
				// page # has line length<4 - don't want to inadverantly pickup
				// - 2000/199*
				break;
			}

			if (tmpDecimalList.size() > 0 && tmpNumberList.size() < 1 && tmptabNumbertabNumberList.size() < 1
					&& !line.toLowerCase().contains("shares") && !line.toLowerCase().contains("outstand")) {
				// System.xut
				// .println("endOfTable line: decimal found but not tabNumber::"
				// + line);
				break;
			}

			if (line.indexOf("\t") < 1) {
				tmpEndedList = nlp.getAllMatchedGroups(line, EndedHeadingPattern);
				tmpEndedSimpleList = nlp.getAllMatchedGroups(line, EndedTabSimplePattern);
				// no tab just: Three Month Ended June 30 Six Month Ended June30
				// need to reset above variables and run constraint below:
			}

			nextRowStart = text.indexOf("\n", startIdx);
			// System.Xut.println("endOfTable idx:" + nextRowStart);
		}
		// once loop is finished - the endOfTableLoc returned
		return endOfTableLoc;
	}

	public int getEndOfTableLocation222(String text, int startIdx) throws IOException {

		NLP nlp = new NLP();

		// System.xut.println("tableText to start===" + text);
		int maxEmptyLinesToMarkEndTable = 4;
		// int maxEmptyLinesToMarkEndTable2 = 6;
		// 2nd is needed to reset
		// end of heading is where we want to start.
		int endOfTableLoc = Math.min(startIdx + 1, text.length()), linesRead = 0, count = 0;
		String line;
		String lines[] = null;
		lines = text.split("\r\n");
		int nextRowStart = text.indexOf("\n", startIdx);
		while (nextRowStart >= 0 && (nextRowStart + 20) < text.length()) {
			count++; // lineNo
			linesRead++; // increasing 1
			line = text.substring(startIdx, nextRowStart);
			// System.xut.println("line::" + line);

			List<String> tmpEndedList = null, tmpMonthList = null, tmpYearList = null, tmpDecimalList = null,
					tmpEndedSimpleList = null//
					, tmpNumberList = null, tmptabNumbertabNumberList = null, tmpTabNumberNoYearList = null;

			if (line.indexOf("  ") > 0 || line.indexOf("\t") > 0) {

				tmpMonthList = nlp.getAllMatchedGroups(line.substring(line.indexOf("  ")), MonthPattern);
				tmpYearList = nlp.getAllMatchedGroups(line.substring(line.indexOf("\t")), YearSimple);
				tmpEndedList = nlp.getAllMatchedGroups(line.substring(line.indexOf("\t")), EndedHeadingPattern);
				tmpEndedSimpleList = nlp.getAllMatchedGroups(line.substring(line.indexOf("\t")), EndedTabSimplePattern);
				tmpTabNumberNoYearList = nlp.getAllMatchedGroups(line.substring(line.indexOf("\t")), tabNumberNoYear);
			}

			// dataCol=lines[l].substring(lines[l].indexOf("\t"))
			tmpNumberList = nlp.getAllMatchedGroups(line, tabNumberTabOrHardReturn);
			tmptabNumbertabNumberList = nlp.getAllMatchedGroups(line, tabNumbertabNumber);
			// above needs to run on entire line b/c tab prior to number
			tmpDecimalList = nlp.getAllMatchedGroups(line, DecimalPattern);
			// above must be run on entire line
			Matcher tableNameMatch = TableNamePattern.matcher(line);

			maxEmptyLinesToMarkEndTable = 4;
			if (count < 10)
				maxEmptyLinesToMarkEndTable = 6;
			if (tmpNumberList.size() > 0 || tmptabNumbertabNumberList.size() > 0
					|| (line.substring(Math.max(line.length() - 2, 0), line.length()).contains(":")
							&& line.indexOf("\t") < 1)) {
				linesRead = 0;
				endOfTableLoc = nextRowStart;
				// if rn= "Current liabilities:" it is rnh - so reset lineRead=0
			}
			startIdx = nextRowStart + 1;
			if (linesRead >= maxEmptyLinesToMarkEndTable
					&& !line.substring(Math.max(line.length() - 2, 0), line.length()).contains(":")) {
				// if line ends with ":" - it is likely rnh
				// System.xut.println("endOfTable line maxrow count met::" + line);
				break;
			}

			if (line.indexOf("\t") > 0 && ((tmpYearList.size() > 0 || tmpMonthList.size() > 0
					|| tmpEndedSimpleList.size() > 0 || tmpEndedList.size() > 0))
					&& tmpTabNumberNoYearList.size() < 1) {
				// System.xut.println("endOfTablex met (year/mo/ended found)::"
				// + line);
				break;
			}

			if ((tableNameMatch.find() && (!line.toUpperCase().contains("LIABILIT")
					&& !line.toUpperCase().contains("EQUITY") && line.toUpperCase().contains("STATEMENT") && count > 5))
					// above OR below
					|| (tableNameMatch.find() && (lines[count + 1].toLowerCase().contains("in million|in thousand")
							|| lines[count + 2].toLowerCase().contains("in million|in thousand")
							|| lines[count + 3].toLowerCase().contains("in million|in thousand")
							|| lines[count + 4].toLowerCase().contains("in million|in thousand")

							|| lines[count + 1].contains("audited")
							|| lines[count + 2].toLowerCase().contains("audited")
							|| lines[count + 3].toLowerCase().contains("audited")
							|| lines[count + 4].toLowerCase().contains("audited"))))
			// above can look ahead if tableName match found to see if decimal
			// pattern is present
			{
				// System.xut.println("endOfTable line w/ tblNm::" + line);
				break;
			}

			if (linesRead > 2 && line.length() < 4 && !line.contains("199") && !line.contains("20")) {
				// System.xut
				// .println("endOfTable line: line.length<4 and linesRead>2::"
				// + line);
				// page # has line length<4 - don't want to inadverantly pickup
				// - 2000/199*
				break;
			}

			if (tmpDecimalList.size() > 0 && tmpNumberList.size() < 1 && tmptabNumbertabNumberList.size() < 1
					&& !line.toLowerCase().contains("shares") && !line.toLowerCase().contains("outstand")) {
				// System.xut
				// .println("endOfTable line: decimal found but not tabNumber::"
				// + line);
				break;
			}

			if (line.indexOf("\t") < 1) {
				tmpEndedList = nlp.getAllMatchedGroups(line, EndedHeadingPattern);
				tmpEndedSimpleList = nlp.getAllMatchedGroups(line, EndedTabSimplePattern);
				// no tab just: Three Month Ended June 30 Six Month Ended June30
				// need to reset above variables and run constraint below:
			}

			nextRowStart = text.indexOf("\n", startIdx);
			// System.Xut.println("endOfTable idx:" + nextRowStart);
		}
		// once loop is finished - the endOfTableLoc returned
		return endOfTableLoc;
	}

	public String getPage(String url) throws IOException {
		EasyHttpClient httpClient = new EasyHttpClient(true);
		String html = httpClient.makeHttpRequest("Get", url, null, -1, null, null);
		return html;
	}

	public String getFolderForDate(Calendar date) {
		return baseTableParserFolder + date.get(Calendar.YEAR) + "/QTR" + getQuarter(date);
	}

	public String getFolderForAcceptedDate(String fileDate) {

		if (acceptedDate != null)
			fileDate = acceptedDate;
		// System.xut.println("fileDate=" + fileDate);

		int year = Integer.parseInt(fileDate.substring(0, 4));
		int month = Integer.parseInt(fileDate.substring(5, 7));
		// System.Xut.println("m::" + month);
		int qtr = ((month - 1) / 3) + 1;
		// System.xut.println(baseFolder + year + "/QTR" + qtr);
		return baseTableParserFolder + year + "/QTR" + qtr;
	}

	public String getFolderForFileDateNoHyphensInDate(String fileDate) {

		int year = Integer.parseInt(fileDate.substring(0, 4));
		int month = Integer.parseInt(fileDate.substring(4, 6));
		// System.Xut.println("m::" + month);
		// System.Xut.println("yr::" + year);
		// (11-1)/3 = 3.6+1 = 4.6 = 4
		int qtr = ((month - 1) / 3) + 1;
		// System.Xut.println("getFolderForFileDate results::" + baseFolder +
		// year
		// + "/QTR" + qtr);
		return baseTableParserFolder + year + "/QTR" + qtr;
	}

	public static int getQuarter(Calendar date) {
		return ((date.get(Calendar.MONTH) / 3) + 1);
	}

	public boolean isCurrentQuarter(Calendar date) {

		int todayQtr = getQuarter(Calendar.getInstance());
		int qtr = getQuarter(date);
		int todayYear = Calendar.getInstance().get(Calendar.YEAR);
		int year = date.get(Calendar.YEAR);
		return (todayQtr == qtr && todayYear == year);
	}

	public String getTableNamePrefixYearQtr(String fileDate) {

		// System.Xut.println("2. filedate=" + fileDate + " acceptedDate" +
		// acceptedDate);

		if (fileDate == null)
			fileDate = acceptedDate;
		int year = Integer.parseInt(fileDate.replaceAll("-", "").substring(0, 4));
		int month = Integer.parseInt(fileDate.replaceAll("-", "").substring(4, 6));
		int qtr = ((month - 1) / 3) + 1;
		return year + "QTR" + qtr;
	}

	public void downloadFilingDetails(Calendar endDate, int yr, int qtr, String localPath, boolean parseEntireHtml,
			boolean justFilesNotInMysql) throws IOException, SQLException {
		// HERE WE CHOOSE JUST MISSING ACCNO AND GET FROM SEC.GOV
		// called when going online to sec.gov
		// this gets called from EightK
		// this gets called from TabelParser dateRangeQuarters

		String file = localPath + "/tpidx.csv";
		File fl2 = new File(file);
//		System.out.println("at downloadFilingDetails");
		if (!fl2.exists()) {
			getMasterIdx(yr, qtr, endDate);
			TableParserLocal.getTpIdx(yr, qtr, justFilesNotInMysql);
		}

		BufferedReader rdr = null;
		try {
			rdr = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		String line;

		String entireHtml = "";
		String html = "";
		String str10QK = "";
		String strEx13 = "";
		String str8k = "";
		String strEx99 = "";
		int cnt = 0;
		File fl = new File(baseTableParserFolder + "tmp");
		if (fl.exists())
			fl.delete();

		String origFd = "";

		pwYyyyQtr = new PrintWriter(fl);
		NLP nlp = new NLP();
		try {

			while ((line = rdr.readLine()) != null) {

				String[] items = line.split("\\|\\|");
				String[] items2 = line.split(",");
				// System.out.println("reading lines. line=" + Arrays.toString(items) + "
				// items2.len=" + items2.length
				// + " items.len=" + items.length);

				if (items.length < 5 && items2.length < 5)
					continue;

				if (items2.length > items.length) {
					items = items2;
				}

				fileDate = items[3];

				if (nlp.getAllIndexEndLocations(fileDate, Pattern.compile("[\\d]{4}-[\\d]{2}-[\\d]{2}")).size() < 1) {
					continue;
				}

//				System.out.println("filedate=" + fileDate);

				if (cnt == 0 || fl.length() > fileSizeToLoad) {
					if (cnt == 0) {
						origFd = fileDate;
					}
					pwYyyyQtr.close();

//					System.out.println("origFd=" + origFd);
					
					if(cnt>0)
					loadIntoMysqlNCparsings(yr, qtr,false);
					
					fl = new File(localPath + origFd.substring(0, 10) + "_" + cnt);

//					System.out.println("filename=" + fl.getAbsolutePath());

					if (fl.exists())
						fl.delete();

					pwYyyyQtr = new PrintWriter(fl);
					cnt++;
				}

				if (items[2].toLowerCase().contains("8-k") || items[2].contains("10-K") 
						|| items[2].contains("10-Q")) {
					cik = items[0];
					companyName = items[1];

					formType = items[2];
//					System.out.println("formType from masterIdx=" + items[2]);

					// System.Xut.println("filedate 1a=" + fileDate);
					if (fileDate != null && acceptedDate == null) {
						acceptedDate = fileDate;
					}

					if (fileDate != null
							&& (fileDate.substring(0, 4) != acceptedDate.substring(0, 4) || acceptedDate == null)) {
						acceptedDate = fileDate;
					}

					if (items[4].length() == 20) {
						acc = items[4];
					}

					else {
						acc = items[4].substring(items[4].length() - 24, items[4].length()).replaceAll(".txt", "");
					}
					int year = Integer.parseInt(fileDate.substring(0, 4));
					// System.Xut.println("fileDate1b=" + fileDate);
//					System.out.println("acc=" + acc);

					File f = new File(localPath + "/" + acc + ".txt");
					if (!f.exists()) {

						// System.out.println("xbrl.download items[4]=" + items[4] + localPath + acc +
						// ".txt");
						// System.out.println("items[4]=" + items[4] + " localPath=" + localPath + "
						// acc=" + acc + ".txt");
						Xbrl.download("edgar/data/" + cik + "/" + acc + ".txt", localPath, acc + ".txt");

					}

					String filePath = localPath + "/" + acc + ".txt";
					headerPortionOfFile = Utils.readSecHeaderPortionOnlyFromFile(filePath);

					// some files are downloaded but corrupted - those will not
					// have typical SEC headers - such as accession and as a
					// result are redownloaded
					if (f.exists() && !headerPortionOfFile.contains("ACCESSION")) {
						f.delete();

						Xbrl.download("edgar/data/" + cik + "/" + acc + ".txt", localPath, acc + ".txt");

						filePath = localPath + "/" + acc + ".txt";
						headerPortionOfFile = Utils.readSecHeaderPortionOnlyFromFile(filePath);

					}

					// when pulling from .nc file items are in this format
					if (formType.contains("8") && !headerPortionOfFile.contains("<ITEMS>2")
							&& !headerPortionOfFile.contains("<ITEMS>5") && !headerPortionOfFile.contains("<ITEMS>7")
							&& !headerPortionOfFile.contains("<ITEMS>9")
							&& !headerPortionOfFile.toLowerCase().contains("financial")
							&& !headerPortionOfFile.toLowerCase().contains("result")
							&& !headerPortionOfFile.toLowerCase().contains("dividend")

							&& !headerPortionOfFile.toLowerCase().contains("other")) {
						// System.xut
						// .println("form 8k does not contain items 2, items 9 or text
						// financial/result/dividend");
						continue;
					}

					entireHtml = Utils.readTextFromFile(filePath);
					html = "";
					str10QK = "";
					strEx13 = "";
					str8k = "";
					strEx99 = "";

					// whether its 10Q/K or 8k - set html to the text to parse
					// (10-Q+ex13 or ex99 if 8-k, entire file ). Then tpHtml or
					// tpTxt parser fails - run against entire filing

					if (formType.contains("8-K") && entireHtml.indexOf("<TYPE>EX-99") > 0) {
						str8k = NLP.removeGobblyGookGetContracts(NLP.getEx99s(entireHtml));
						html = str8k;
					}

					// set html to just 10-Q and ex13 if !parseEntireHtml.
					// Doesn't call ttp or tpHtml.
					// if parseEntireHtml -- after removal of gobbly gook is
					// less than maxFileSize - use entireHtml w/o gobbly gook
					if (parseEntireHtml && !formType.contains("8-K")) {
						html = NLP.removeGobblyGookGetContracts(entireHtml);
						// don't need to recheck removeGobblyGook!
					}

					if ((!parseEntireHtml || (parseEntireHtml && html.length() >= maxFileSize))
							&& ((formType.contains("10-Q") || formType.contains("10-K"))
									&& entireHtml.indexOf("<TYPE>10-") >= 0)
					// <==this must always must be

					) {

						if (parseEntireHtml && html.indexOf("<TYPE>EX-99") > 0) {
							// means fileSize>maxFileSize - so also include ex99
							// but only if parsing entire html
							strEx99 = NLP.getEx99s(html);
						}

//						System.out.println("formType is 10-Q/K");
						str10QK = entireHtml.substring(entireHtml.indexOf("<TYPE>10-"));
						if (str10QK.indexOf("</TEXT>") >= 0) {
							str10QK = NLP.removeGobblyGookGetContracts(str10QK.substring(0, str10QK.indexOf("</TEXT>")));
							// System.xut
							// .println("is 10-Q/K - and have 10K/Q string");
							html = str10QK;
						}

						if (entireHtml.indexOf("<TYPE>EX-13") >= 0) {
							strEx13 = entireHtml.substring(entireHtml.indexOf("<TYPE>EX-13"));
							if (strEx13.indexOf("</TEXT>") >= 0) {
								strEx13 = NLP.removeGobblyGookGetContracts(strEx13.substring(0, strEx13.indexOf("</TEXT>")));
							}
							// ex99 will be blank or have text if parsing entire
							// html and it is too large.
						}
						html = str10QK + " \r\r" + strEx13 + "\r\r" + strEx99;
					}

					int idx = StringUtils.indexOfIgnoreCase(html, "</tr>");
					int idxTd = StringUtils.indexOfIgnoreCase(html, "</td>");

					int idxEx13 = StringUtils.indexOfIgnoreCase(strEx13, "</tr>");
					int idxTdEx13 = StringUtils.indexOfIgnoreCase(strEx13, "</td>");

					// html is either entire html or just 10k/ex-13 based on
					// whether I asked to parse entire html when calling class
					// or not. Now either ttp or tpHtml is called which is
					// agnostic as to what is in html/text passed.
					// PrintWriter tempPw = new PrintWriter(new File(
					// "c:/backtest/temp2/"
					// + acc + "__.txt"));

					if ((formType.contains("10") || formType.contains("8")) && html.length() > 2) {

						if (idx >= 0 && idxTd >= 0) {

							TableParserHTML tpHtml = new TableParserHTML(acc, fileDate, cik, tableCount, fye, formType,
									companyName);

							tpHtml.getTablesFromFiling(html, false, parseEntireHtml);

							// tempPw.append(html);
							// tempPw.close();

							if (idxEx13 < 0 || idxTdEx13 < 0) {
								TableTextParser ttp = new TableTextParser(acc, fileDate, cik, tableCount, fye, formType,
										companyName);
								tableCount = 100;

								// PrintWriter tempPw2 = new PrintWriter(new File(
								// "c:/backtest/temp2/"
								// + acc + "htmlJustEx13_.txt"));
								// tempPw2.append(strEx13);
								// tempPw2.close();
								ttp.tableTextParser(strEx13, false, parseEntireHtml, adjustIdxLoc);

							}
							// if not html -- no <html in entireHtml (complete
							// filing)
						} else {
							TableTextParser ttp = new TableTextParser(acc, fileDate, cik, tableCount, fye, formType,
									companyName);
							// tempPw.append(html);
							// tempPw.close();
							// System.xut.println("adjustIdxLoc="+adjustIdxLoc);
							ttp.tableTextParser(html, false, parseEntireHtml, adjustIdxLoc);

						}
					}

					str10QK = "";
					strEx13 = "";
					html = "";
					entireHtml = "";
					str8k = "";
					strEx99 = "";

					String secFile = items[4];
					if (acc.length() > 20)
						acc = secFile.substring(secFile.lastIndexOf("/") + 1, secFile.lastIndexOf("."));
					// System.Xut.println("accNO:: " + acc);
					String accHtm = acc + "-index.htm";
					// String accNoHyphens = acc.replace("-", "");
					File fileHtm = new File(localPath + "/htm/" + accHtm);

					// String suffix = secFile.substring(0, secFile.lastIndexOf("/"));
					// String wwwSecGovPath = (suffix + "/" + accNoHyphens + "/" + accHtm);
					// String wwwSecGovPathHtm = (suffix + "/" + accNoHyphens + "/" + acc +
					// "-index.htm");

					Utils.createFoldersIfReqd(localPath + "/htm");

					if (!fileHtm.exists()) {

						Xbrl.download("edgar/data/" + cik + "/" + acc + "-index.htm", localPath + "/htm", accHtm);
						// XBRLInfoSecGov info = new XBRLInfoSecGov(
						// "https://www.sec.gov/Archives/"
						// + wwwSecGovPathHtm);
						// System.Xut.println("fileName:"
						// + fileHtm.getAbsolutePath());

						entireHtml = Utils.readTextFromFile(fileHtm.getAbsolutePath());
						// once this method is called all the public string
						// variables are assigned that are in this method
						// (acceptedDate,fye,formItems,etc.)
						extractIndexHtmlInformation(entireHtml, "");

						// System.Xut.println("formItems::" + formItems);
						// System.Xut.println("formStr::" + formStr);
						// System.Xut.println("fye::" + fye);
						// System.Xut.println("acceptedDate::" + acceptedDate);
						// System.Xut.println("fileDate2::" + fileDate);

						// System.xut.println("index-html="
						// + "https://www.sec.gov/Archives/"
						// + wwwSecGovPathHtm);

						// catches errors where fileDate!=aDate (ensure correct
						// localPath)

						acceptedDate = acceptedDate.replace("-", "");

						if (acceptedDate.substring(0, 7) != fileDate.substring(0, 7) && fileDate != null) {
							acceptedDate = fileDate;
						}

						// going forward from here always use fileDate

						// File fileIndexHtmData = new File(localPath + "/" +
						// acc
						// + "_itemInfo");

						// if (fileIndexHtmData.exists()) {
						// fileIndexHtmData.delete();
						//
						// } else
						// fileIndexHtmData.createNewFile();

						String fileHtmStr = fileHtm.getAbsolutePath();
						fileHtmStr = fileHtmStr.replaceAll("\\\\", "/");

						// b/c if year<2003 we grab complete.txt and run
						// getGeneric...
						if (year > 2003) {
							getFilingDetails(fileHtmStr);
						} else
							continue;
					}
				}
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			try {
				rdr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		pwYyyyQtr.close();

		loadIntoMysqlNCparsings(yr, qtr,false);
		
	}

	public void getFilingDetails(String htmlFile) throws Exception {
		String itemsStr = "";
		String items = "";
		periodOfReport = "";
		// see method extractIndexHtmlInformation(html, ""); period of report
		// and other tools that extract from -index.html file should be put into
		// one method (remove this or streamline with
		// extractIndexHtmlInformation method)

		String html = Utils.readTextFromFileWithSpaceSeparator(htmlFile);
		Document document = Jsoup.parse(html, htmlFile);
		Pattern ptrnA = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
		Elements dateElement = document.getElementsMatchingOwnText(ptrnA);
		acceptedDate = dateElement.get(0).text();
		// System.Xut.println("acceptedDate::" + acceptedDate);

		// Pattern ptrnI = Pattern.compile("formGrouping");
		// Results of Operations
		Elements formGroups = document.getElementsByAttributeValue("class", "formGrouping");
		// html source is [<div class="formGrouping">]. class is attribute of
		// div el and "formGrouping" is value. See tech notes for
		// getElementsByAttributeValue

		for (Element ele : formGroups) {
			// loop thru each element of formGroups
			Elements children = ele.getElementsByTag("div");
			// where element name/tag is "div" provide all elements
			boolean rightGroupFound = false;
			boolean periodOfReportFound = false;
			for (Element child : children) {
				// System.Xut.println(JSoupXMLParser.getAttributeValue(child,"class")
				// + "::" + child.text());
				if ("infoHead".equalsIgnoreCase(JSoupXMLParser.getAttributeValue(child, "class"))
						&& "Period of Report".equalsIgnoreCase(child.text())) {
					periodOfReportFound = true;
				}

				if ("infoHead".equalsIgnoreCase(JSoupXMLParser.getAttributeValue(child, "class"))
						&& "Items".equalsIgnoreCase(child.text())) {
					// sub routine then checks for attribute "class" to see if
					// it has value "infoHead" and to see if
					// child element value is equal to "Items"
					rightGroupFound = true;

				}
				if (periodOfReportFound && "info".equalsIgnoreCase(JSoupXMLParser.getAttributeValue(child, "class"))) {
					periodOfReport = child.text().replaceAll("-", "");
					periodOfReportFound = false;
					// we use false b/c can't break or continue loop due to
					// other fields being captured that rely on the loop
					// independent of this
					// System.xut.println("periodOfReport: " + periodOfReport);
				}

				if (rightGroupFound && "info".equalsIgnoreCase(JSoupXMLParser.getAttributeValue(child, "class"))
						&& child.text().startsWith("Item")) {
					// the div that hold the items value...
					items = child.text();
					// System.Xut.println("child.text::" + items);
					String[] item = items.split("Item \\d[\\.\\d:]{0,4}");
					itemsStr = Arrays.toString(item);
					// System.Xut.println("itemStr::" + itemsStr);

					items = itemsStr.substring(itemsStr.indexOf(",") + 2, itemsStr.lastIndexOf("]"));

					// System.Xut.println("i[]: " + Arrays.toString(item));
					// System.Xut.println("itemsCsv: " + itemsCsv);
				}
			}
		}
		// System.Xut.println("htmlFile:: " + htmlFile);
		// System.Xut.println("itemsStr:: " + itemsStr);
		// System.Xut.println("items:: " + items);

		// System.xut.println("formType=" + formType);
		if (formType.contains("8")) {

			downloadExhibit99(htmlFile, acceptedDate, acc, items, itemsStr, document);
		} else
			downloadCompleteSubmission(htmlFile, items, itemsStr, document);
		// System.xut.println("downloadCompleteSubmission. htmlFile=" + htmlFile
		// + " items=" + items + " itemsStr=" + itemsStr);

	}

	public void downloadCompleteSubmission(String fileHtmStr, String items, String itemsStr, Document document)
			throws SQLException {

		// System.Xut.println("items::" + items);
		// System.Xut.println("formStr::" + itemsStr);

		Pattern ptrn = Pattern.compile("(?i)Complete submission");
		Elements ptrnEle = document.getElementsMatchingOwnText(ptrn);

		@SuppressWarnings("unused")
		int fileSize = 0;

		Element rowEle = null;
		for (int i = 0; i < ptrnEle.size(); i++) {
			Elements parentEle = ptrnEle.parents();
			for (Element eleFind : parentEle) {
				if (eleFind.tagName().equalsIgnoreCase("tr")) {
					rowEle = eleFind;
					break;
				}
			}
			if (null == rowEle)
				continue;
			Element td = rowEle.child(rowEle.children().size() - 1);
			// System.Xut.println("last column: "
			// + td.text());

			fileSize = Integer.parseInt(td.text());
		}
		String yr = fileDate.substring(0, 4);
		int y = Integer.parseInt(yr);
		// TODO: don't parse 10-Q/K if yr is after 2013.

		if (formType.contains("10") && y < 2014) {
			String divUrl = "https://www.sec.gov" + rowEle.getElementsByAttribute("href").get(0).attr("href");
			// System.xut.println("10k/q divUrl=" + divUrl);
			try {
				downloadEx99(divUrl, acceptedDate, acc, "Complete");
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}

	public void downloadExhibit99(String fileHtmStr, String acceptedDate, String acc, String items, String itemsStr,
			Document document) throws SQLException {

		// System.Xut.println("itemsStr" + itemsStr);
		// System.Xut.println("items" + items);

		if (items != null) {

			// && (items.contains("Operation") || items.contains("operation")
			// || items.contains("FD") || items.contains("statements")
			// || items.contains("Statements")
			// || items.contains("other") || items.contains("Other"))) {
			// above not necessary b/c this is a condition precedent in earlier
			// method that calls this.
			Pattern ptrn = Pattern.compile("(?<=((?i)EX-))99[\\.\\d]{2}");

			// gets elements with pattern match EX-99
			Elements ptrnEle = document.getElementsMatchingOwnText(ptrn);

			/*
			 * create pattern=>match/capture pattern=>get parent el related for match and
			 * then find info w/n parent. Here we captured a pattern that is in a cell. We
			 * then identified within the row the href cell by Id of the child
			 * 
			 * /* <tr> <td scope="row">1</td> <td scope="row">FORM 10-K</td> <td
			 * scope="row"><a href= "/Archives/edgar/dat....</a></td> <td
			 * scope="row">10-K</td> <td scope="row">1451696</td> </tr>
			 */

			// gets parent element of pattern matched above

			Element rowEle = null;
			for (int i = 0; i < ptrnEle.size(); i++) {
				Elements parentEle = ptrnEle.parents();
				for (Element eleFind : parentEle) {
					if (eleFind.tagName().equalsIgnoreCase("tr")) {
						rowEle = eleFind;
						break;
					}
				}
				if (null == rowEle)
					continue;
				// Ids child of parent = to fileSize (b/c size equals last child
				// which is located at rowEle.Size -1
				Element td = rowEle.child(rowEle.children().size() - 1);

				// System.Xut.println("last column: "
				// + td.text());

				@SuppressWarnings("unused")
				int fileSize = Integer.parseInt(td.text());

				String divUrl = "https://www.sec.gov" + rowEle.getElementsByAttribute("href").get(0).attr("href");

				if (divUrl.endsWith(".txt") || divUrl.endsWith(".htm") || divUrl.endsWith(".html")) {
					Element tdType = rowEle.child(rowEle.children().size() - 2);

					String extension = null;

					extension = tdType.text();
//					System.out.println("99 extension::" + extension);

					// saves fileSize to 99Size table.
					// saveFileSize(acceptedDate, acc, extension, fileSize);

					if (extension.contains("99.1")) {
						extension = "99_1";
//						System.out.println("x99_1::" + extension);
					}
					if (extension.contains("99.2")) {
						extension = "99_2";
//						System.out.println("x99_2::" + extension);
					}
					if (!extension.contains("99_1") && !extension.contains("99_2")) {
//						System.out.println("x99_0::" + extension);
						extension = "99_0";
					}

					try {
						downloadEx99(divUrl, acceptedDate, acc, extension);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ParseException e) {
						e.printStackTrace();
					}
					// if (i == 2)
					// consider first 2 items only
				}

				// if (fileSize > 1000000) {
				// downloadForm8k(fileHtmStr, acceptedDate, acc, items,
				// itemsStr, document);
				// }
			}

		}
	}

	public void downloadEx99(String divUrl, String acceptedDate, String acc, String fileType)
			throws IOException, ParseException, SQLException {
		
		System.out.println("acceptedDate at downloadEx99=" + acceptedDate + "\r URL=" + divUrl);
		if (fileDate == null && acceptedDate != null) {
			fileDate = acceptedDate;
		}
		
		OutputStream out = null;
		URLConnection conn = null;
		InputStream in = null;
		String extn = ".html";
		File fileOrig = new File("");
		String localFileName = null;
		if (fileType.contains("Complete")) {
			fileOrig = new File(getFolderForFileDateNoHyphensInDate(fileDate.replaceAll("-", "")) + "/" + acc + "_"
					+ fileType + ".htm");
			// System.Xut.println("Complete fileOrig: "
			// + fileOrig.getAbsolutePath());
			if (!fileOrig.exists()) {
				localFileName = getFolderForFileDateNoHyphensInDate(fileDate.replaceAll("-", "")) + "/" + acc + "_"
						+ fileType + extn;
			}
		}
		if (fileType.contains("bsnWire")) {
			fileOrig = new File(getFolderForAcceptedDate(acceptedDate) + "/" + acc + "_" + fileType + ".htm");
			// System.Xut.println("bsnWire fileOrig: "
			// + fileOrig.getAbsolutePath());
			if (!fileOrig.exists()) {
				localFileName = getFolderForAcceptedDate(acceptedDate) + "/" + acc + "_" + fileType + ".htm";
			}
		}

		if (fileType.contains("99")) {
			// System.Xut.println("getFolderForAcceptedDate(acceptedDate) 2="
			// + acceptedDate);
			fileOrig = new File(getFolderForAcceptedDate(acceptedDate) + "/" + acc + "_" + fileType + ".htm");
			// System.Xut.println("bsnWire fileOrig: "
			// + fileOrig.getAbsolutePath());
			if (!fileOrig.exists()) {
				localFileName = getFolderForAcceptedDate(acceptedDate) + "/" + acc + "_" + fileType + ".htm";
			}
		}

		// System.Xut.println("localFileName::" + localFileName);
		if (!fileOrig.exists()) {
			// this will prevent redownloading and reparsing -
			// parseTable(.....). In theory - you can put the parseTable outside
			// the loop to parse the local file. But this would cause reparsing
			// when intent is to downloading new.

			try {
				URL url = new URL(divUrl);
				out = new BufferedOutputStream(new FileOutputStream(localFileName));
				conn = url.openConnection();
				in = conn.getInputStream();
				byte[] buffer = new byte[1024];
				int numRead;
				@SuppressWarnings("unused")
				long numWritten = 0;
				while ((numRead = in.read(buffer)) != -1) {
					out.write(buffer, 0, numRead);
					numWritten += numRead;
				}

				// System.Xut.println("downloading" + localFileName + "\t"
				// + numWritten);
			} catch (Exception exception) {
				exception.printStackTrace();
			} finally {
				try {
					if (in != null) {
						in.close();
					}
					if (out != null) {
						out.close();
					}
				} catch (IOException ioe) {
				}
			}
			System.out.println("done downloading.." + divUrl);
			parseTable(acceptedDate, acc, divUrl, fileType, ".html");
		}
	}

	// System.Xut.println("parseHtml");

	@SuppressWarnings("deprecation")
	public String getAcceptedDateForBusinessWire(String pageText, String acceptedDate) throws ParseException {
		// gets minutes and hours of acceptedDate for businessWire html file

		Document doc = Jsoup.parse(pageText);
		Element time = null;
		// Element time = doc.getElementsByTag("time").get(0);
		System.out.println("acc at getAD for bsnwire: " + acc);
		if (acceptedDate != null) {
			acceptedDate = acceptedDate.replaceAll("-", "");
			return acceptedDate = acceptedDate.replaceAll("-", "");
		} else if (doc.getElementsByTag("time").hasText()

		// && doc.getElementsByTag("time").size() > 0
		) {
			time = doc.getElementsByTag("time").get(0);

			// some date formats in bsnWire are yyyy:mm:dd hh:mm
			String date = time.text();
			if (date.substring(0, 3).contains("201")) {
				date = date.replaceAll("[^\\p{ASCII}|[ ]]", "-").replaceAll("---", " ");
				acceptedDate = date.substring(0, 16) + ":00";
				// System.Xut.println("acceptedDate if non-ascii::"+
				// acceptedDate);
			} else {
				// December 13, 2013 07:00 PM Eastern Standard Time
				SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm a z");
				Date dt = null;
				try {
					dt = sdf.parse(date);
					// TODO: HOW TO DEAL WITH FOREIGN DATE ERRORS?
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return acceptedDate;
				}
				if (date.contains(" PM "))
					dt.setHours(dt.getHours() + 12);
				SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				// System.Xut.println("date (sdf2.format(dt)::"
				// + sdf2.format(dt) + ":00");
				acceptedDate = sdf2.format(dt) + ":00";
			}
		}
		// System.Xut.println("acceptedDate at start of parseTable:: "
		// + acceptedDate);
		return acceptedDate;
	}

	public void parseTable(String acceptedDate, String acc, String address, String fileType, String extn)
			throws IOException, ParseException, SQLException {

		System.out.println("acceptedDate=" + acceptedDate + "\r acc=" + acc + "\r address=" + address + "\r fileType="
				+ fileType + "\r extn=" + extn);

		System.out
				.println("file to parse" + getFolderForAcceptedDate(acceptedDate) + "/" + acc + "_" + fileType + extn);

		String fileOriginal = null;
		if (acceptedDate != null)
			fileDate = acceptedDate;

		if (address.contains("Text")) {
			fileOriginal = address;
			// System.Xut.println("contains text. file=" + fileOriginal);

		}
		if (fileType.equalsIgnoreCase("bsnWire")) {
			fileOriginal = getFolderForAcceptedDate(acceptedDate) + "/" + acc + "_" + fileType + extn;
			System.out.println("fileType=bsnWire. fileOriginal=" + fileOriginal);
		}

		else {
			fileOriginal = getFolderForFileDateNoHyphensInDate(fileDate.replaceAll("-", "")) + "/" + acc + "_"
					+ fileType + extn;
			System.out.println("else file=" + fileOriginal);
		}

		// System.Xut.println("parse table fileOriginal2:: " + fileOriginal);
		String html = "";

		// fix ==> only need 1.
		File f = new File(fileOriginal);
		if (f.exists()) {
//			System.out.println("1. readTextFromFile=======");
			html = Utils.readTextFromFile(fileOriginal);
		}

		if (!f.exists() && fileOriginal.toLowerCase().endsWith(".html")) {
//			System.out.println("2. readTextFromFile=======");
			html = Utils.readTextFromFile(fileOriginal.replaceAll(".html", ".htm"));

			// TODO: HERE I CAN READ FILE AND GET TICKER SYMBOL - OR RUN GET
			// TICKER SYMBOL FROM BUSINESSWIRE HTML

		}

		if (!f.exists() && fileOriginal.toLowerCase().endsWith("htm")) {
//			System.out.println("3. readTextFromFile=======");
			html = Utils.readTextFromFile(fileOriginal.replaceAll("html", "htm"));

			// TODO: HERE I CAN READ FILE AND GET TICKER SYMBOL - OR RUN GET
			// TICKER SYMBOL FROM BUSINESSWIRE HTML

		}

		System.out.println("fileOriginal=" + fileOriginal + " f=" + f.getAbsolutePath());
		String ticker = "";
		if(fileType.equals("bsnWire")) {
			ticker = NLP.getTicker(html);
			System.out.println("ticker="+ticker);
			acc = acc.substring(0, 14)+ticker.trim();
		}

		// this has sent the acc/adate to this class so it will be available in
		// that class

		String entireHtml = html;
		String str10QK = "";
		String strEx13 = "";
		String str8k = "";
		String strEx99 = "";
		boolean parseEntireHtml = true;

		if (null != formType && formType.contains("8-K") && entireHtml.indexOf("<TYPE>EX-99") > 0) {
			str8k = NLP.removeGobblyGookGetContracts(NLP.getEx99s(entireHtml));
			html = str8k;
		}

		// set html to just 10-Q and ex13 if !parseEntireHtml.
		// Doesn't call ttp or tpHtml.
		// if parseEntireHtml -- after removal of gobbly gook is
		// less than maxFileSize - use entireHtml w/o gobbly gook
		if (null != formType && parseEntireHtml && !formType.contains("8-K")) {
			html = NLP.removeGobblyGookGetContracts(entireHtml);
			// don't need to recheck removeGobblyGook!
		}

		if ((!parseEntireHtml || (parseEntireHtml && html.length() >= maxFileSize))
				&& ((formType != null && formType.contains("10-Q") || formType.contains("10-K"))
						&& entireHtml.indexOf("<TYPE>10-") >= 0)

		) {

			if (parseEntireHtml && html.indexOf("<TYPE>EX-99") > 0) {
				// means fileSize>maxFileSize - so also include ex99
				// but only if parsing entire html
				strEx99 = NLP.getEx99s(html);
			}

			System.out.println("formType is 10-Q/K");
			str10QK = entireHtml.substring(entireHtml.indexOf("<TYPE>10-"));
			if (str10QK.indexOf("</TEXT>") >= 0) {
				str10QK = NLP.removeGobblyGookGetContracts(str10QK.substring(0, str10QK.indexOf("</TEXT>")));
				System.out.println("is 10-Q/K - and have 10K/Q string");
				html = str10QK;
			}

			if (entireHtml.indexOf("<TYPE>EX-13") >= 0) {
				strEx13 = entireHtml.substring(entireHtml.indexOf("<TYPE>EX-13"));
				if (strEx13.indexOf("</TEXT>") >= 0) {
					strEx13 = NLP.removeGobblyGookGetContracts(strEx13.substring(0, strEx13.indexOf("</TEXT>")));
				}
				// ex99 will be blank or have text if parsing entire
				// html and it is too large.
			}
			html = str10QK + " \r\r" + strEx13 + "\r\r" + strEx99;
		}

		// System.Xut.println(html.length());

		TableParserHTML tpHtml = new TableParserHTML(acc, fileDate, cik, tableCount, fye, formType, companyName);

		TableTextParser ttp = new TableTextParser(acc, fileDate, cik, tableCount, fye, formType, companyName);

		// System.Xut.println("2. html or txt parser" + " html.length()="
		// + html.length() + "\r acc=" + acc + " fileOriginal="
		// + fileOriginal);
		// PrintWriter tempPw = new PrintWriter(new File(
		// "c:/backtest/temp2/"
		// + acc + "__2.txt"));

		if (StringUtils.indexOfIgnoreCase(html, "<td") >= 0) {
			// tempPw.append(html);
			// tempPw.close();
			tpHtml.getTablesFromFiling(html, false, parseEntireHtml);
		}

		else {
			// tempPw.append(html);
			// tempPw.close();
			ttp.tableTextParser(html, false, parseEntireHtml, adjustIdxLoc);

			// PrintWriter pwTtpHtml = new PrintWriter(
			// new File(
			// baseFolder
			// + getTableNamePrefixYearQtr(fileDate)
			// + "_ttpHtml2_" + acc + ".txt"));
			// pwTtpHtml.append(html);
			// pwTtpHtml.close();

			if (acceptedDate.substring(0, 4).contains("200[0-2]{1}|199\\d")
					|| fileDate.substring(0, 4).contains("200[0-2]{1}|199\\d")
							&& !fileType.equalsIgnoreCase("bsnWire")) {

				// getBottomTag(nlp.removeAttachments(html =
				// html.substring(html.indexOf("<TYPE>EX-27"))), acc);
			}
		}
		entireHtml = html;
		str10QK = "";
		strEx13 = "";
		str8k = "";
		strEx99 = "";

	}

	public void dateRangeQuarters(Calendar startDate, Calendar endDate, boolean justFilesNotInMysql,
			boolean parseEntireHtmlFiling) throws SocketException, IOException, SQLException {

		// System.Xut.println("parsing just accnos not in mysql=" + mysql);

		int startYear = startDate.get(Calendar.YEAR);
		System.out.println("startYear=" + startYear);
		int endYear = endDate.get(Calendar.YEAR);
		System.out.println("startYear=" + endYear);
		int startQtr = getQuarter(startDate);
		int endQtr = getQuarter(endDate);
		System.out.println("startQtr=" + startQtr);
		System.out.println("endQtr=" + endQtr);
		// total # of loops=totalQtrs.

		int QtrYrs = (endYear - startYear) * 4;
		iQtr = (endQtr - startQtr) + 1;
		int totalQtrs = QtrYrs + iQtr;
		startYr = startYear;
		iQtr = startQtr;
		Calendar cal = Calendar.getInstance();

		for (int i = 1; i <= totalQtrs; i++) {
			// load into mysql any previously parsed .nc files.

			cal.set(Calendar.YEAR, startYr);
			cal.set(Calendar.MONTH, (iQtr * 3) - 1);
			String localPath = baseTableParserFolder + startYr + "/QTR" + iQtr + "/";

			// BELOW USED TO RE-ZIP ANY ACCNO AND REALTED HTM AND PLACE BACK IN
			// SECZIPFILES/DOWNLOADED. PASTE BACK TO BACKUP WHEN FINISHED. IT
			// MAY HAVE ADDITIONAL FILES DOWNLOADED. THIS WORKS WITH TABLEPARSER
			// AND TABLEPARSERLOCAL. NOT SURE IF THIS WORK EIGHTK

			String zipFile = baseSecZipDownloadedFolder + startYr + "QTR" + iQtr + ".zip";
			String extractToFolder = baseTableParserFolder + startYr;
			File extractDirectory = new File(extractToFolder);
			System.out.println(
					"extractToFolder=" + extractToFolder + "\r extractDirectory=" + extractDirectory.getAbsolutePath());
			if (extractDirectory.isDirectory()) {
				Utils.deleteDirectory(extractDirectory);
			}
			extractDirectory = new File(localPath);
			System.out.println("localPath=" + localPath + " \r extractDirectory=" + extractDirectory.getAbsolutePath());

			if (extractDirectory.isDirectory()) {
				Utils.deleteDirectory(extractDirectory);
				System.out.println("deleted directory=" + extractDirectory);
			}

			Utils.createFoldersIfReqd(extractToFolder);

			System.out.println("gonna unzip this zip file if it exists=" + zipFile);
			File zip = new File(zipFile);
			if (zip.exists()) {
				ZipUtilsUnZip.unzip(zipFile, extractToFolder);
			}

			if (justFilesNotInMysql) {
				getMasterIdx(startYr, iQtr, cal);
				TableParserLocal.getTpIdx(startYr, iQtr, justFilesNotInMysql);
			}

			else if (!justFilesNotInMysql) {
				// this assumes I'm using 'seczipfiles'
				getMasterIdx(startYr, iQtr, cal);

				File file = new File(localPath, "master.idx");
				if (!file.exists()) {
					ZipUtils.deflateZipFile(localPath + "/master.zip", localPath);
				}

				String zipFilePath = localPath + "/" + startYr + "Qtr" + iQtr + ".zip";
				file = new File(zipFilePath);
				if (file.exists()) {
					ZipUtils.deflateZipFile(zipFilePath, localPath);
				}
			}

			downloadFilingDetails(cal, startYr, iQtr, localPath, parseEntireHtmlFiling, justFilesNotInMysql);

			loadIntoMysqlNCparsings(startYr, iQtr,false);

			// BELOW USED TO RE-ZIP ANY ACCNO AND REALTED HTM AND PLACE BACK IN
			// SECZIPFILES/DOWNLOADED. PASTE BACK TO BACKUP WHEN FINISHED. IT
			// MAY HAVE ADDITIONAL FILES DOWNLOADED. THIS WORKS WITH TABLEPARSER
			// AND TABLEPARSERLOCAL. NOT SURE IF THIS WORK EIGHTK

			String folderToZip;
			String zipPathAndFileName;

			folderToZip = localPath;
			zipPathAndFileName = baseSecZipDownloadedFolder + startYr + "QTR" + iQtr + ".zip";

			System.out.println("a localPath=" + localPath + " zipPathAndFileName=" + zipPathAndFileName);

			File z = new File(zipPathAndFileName);
			if (z.exists())
				z.delete();

			ZipUtils.zipAllFilesInFolder(folderToZip, zipPathAndFileName);

			extractToFolder = baseTableParserFolder + startYr;
			extractDirectory = new File(extractToFolder);
			Utils.deleteDirectory(extractDirectory);
			extractDirectory = new File(localPath);
			Utils.deleteDirectory(extractDirectory);

			iQtr++;
			if (iQtr > 4) {
				startYr++;
				iQtr = 1;
			}
		}
	}

	public static void loadIntoMysqlNCparsings(int yr, int qtr, boolean isSecZipFile) throws SQLException, IOException {

		NLP nlp = new NLP();
		String subF = "secZip";
		if(!isSecZipFile)
			subF="secSite";
		
		String backup = "c:/tableParser/"+subF+"/";
		Utils.createFoldersIfReqd(backup);
		
		File folder = new File(baseTableParserFolder + yr + "/qtr" + qtr);
		
//		System.out.println("folder=" + folder.getAbsolutePath());
		File[] listOfFiles = folder.listFiles();

		String path = baseTableParserFolder + "bac/",text = "";
		Utils.createFoldersIfReqd(path);
		Utils.createFoldersIfReqd(baseTableParserFolder + "SEC/");
		
		PrintWriter pw = new PrintWriter(new File(baseTableParserFolder + "SEC/tmp.txt"));
		PrintWriter pwSaveFinal = new PrintWriter(new File(backup+"tmp2.txt"));
		
		File origFile = new File("");
		
		for (int i = 0; i < listOfFiles.length; i++) {

			origFile = listOfFiles[i];
//			System.out.println("loadIntoMysqlNCparsings origFile=" + origFile.getAbsolutePath());
			if (nlp.getAllIndexEndLocations(origFile.getName(),
					Pattern.compile("^([12]{1}[09]{1}[\\d]{2}-[\\d]{2}-[\\d]{2})")).size() < 1)
				continue;

			text = Utils.readTextFromFile(origFile.getAbsolutePath());
			// repl cleans-up cases where there's a hard return that should not exist
			text = text.replaceAll("[\r\n]{1,2}(?! ?\t?[\\d]{10}-[\\d]{2}-[\\d]{6})", "");
			text = text.replaceAll("( ?[\\p{Alnum}\\p{Punct}] ?)"
					+ "([\\d]{10}-[\\d]{2}-[\\d]{6})", "$1\r$2");
			text = text.replaceAll("\n", "\r").replaceAll("\r\r", "\r").replace("\\N", "''");

			
			if (origFile.exists())
				origFile.delete();
			pw = new PrintWriter(origFile);
			pw.println(text.replaceAll("(?ism)([\\d]{10}-[\\d]{2}-[\\d]{6})", "\r$1")
					.replaceAll("\r\r", "\r"));
			pw.close();
			pwSaveFinal = new PrintWriter(new File(backup+origFile.getName()));
			pwSaveFinal.println(text.replaceAll("(?ism)([\\d]{10}-[\\d]{2}-[\\d]{6})", "\r$1")
					.replaceAll("\r\r", "\r"));
			pwSaveFinal.close();
			// System.out.println("saved origFile2=" + origFile.getAbsolutePath());
			
			String query = "LOAD Data LOCAL INFILE '" + origFile.getAbsolutePath().replaceAll("\\\\", "//")
					+ "' ignore INTO TABLE bac_tp_raw" + yr + "qtr" + qtr + " FIELDS TERMINATED BY ',' "
					+ "\rLINES TERMINATED BY '\\r';";
			MysqlConnUtils.executeQuery(query);

		}
	}
	
	public static void reLoadintoBac_Tp_Raw(String folderPath) throws SQLException, IOException {
		// input folder path to grab previously parsed bac_tp_raw files.

		System.out.println("folder to read files form=" + folderPath);
		
			File folder = new File(folderPath);
			File[] listOfFiles = folder.listFiles();

			String text = "";
			PrintWriter pw = new PrintWriter(new File(baseTableParserFolder + "SEC/tmp.txt"));
			
			File origFile = new File("");
			NLP nlp = new NLP();
			System.out.println("listOfFiles.len="+listOfFiles.length);
			for (int i = 0; i < listOfFiles.length; i++) {
				System.out.println("file="+listOfFiles[i]);
				origFile = listOfFiles[i];
				
				if (nlp.getAllIndexEndLocations(origFile.getName(),
						Pattern.compile("^([12]{1}[09]{1}[\\d]{2}-[\\d]{2}-[\\d]{2})")).size() < 1)
					continue;
				
				System.out.println("reading text");

				text = Utils.readTextFromFile(origFile.getAbsolutePath());
				text = text.replaceAll("[\r\n]{1,2}(?! ?\t?[\\d]{10}-[\\d]{2}-[\\d]{6})", "");
				text = text.replaceAll("( ?[\\p{Alnum}\\p{Punct}] ?)"
						+ "([\\d]{10}-[\\d]{2}-[\\d]{6})", "$1\r$2");
				text = text.replaceAll("(\n)", "\r").replaceAll("\r\r", "\r").replace("\\N", "''");

				
				if (origFile.exists())
					origFile.delete();
				
				System.out.println("delete origFile="+origFile.delete());
				pw = new PrintWriter(origFile);
				pw.append(text.replaceAll("(?ism)([\\d]{10}-[\\d]{2}-[\\d]{6})", "\r$1")
						.replaceAll("\r\r", "\r"));
				pw.close();
				System.out.println("resaved orig file=");
				
				String yr = origFile.getName().substring(0, 4);
				int q = Integer.parseInt(origFile.getName().substring(5, 7));
				System.out.println("q=" + q + " year=" + origFile.getName().substring(0, 4));
				
				if(q>=0 && q<4) {
					q=1;
				}
				if(q>=4 && q<7) {
					q=2;
				}
				if(q>=7 && q<10) {
					q=3;
				}
				if(q>=10 && q<=12) {
					q=4;
				}
				
				System.out.println("q="+q);
				
				String query = "LOAD Data LOCAL INFILE '" + origFile.getAbsolutePath().replaceAll("\\\\", "//")
						+ "' ignore INTO TABLE bac_tp_raw"+yr+"qtr"+q
						+ "\rFIELDS TERMINATED BY ',' "
						+ "\rLINES TERMINATED BY '\\r';";
				
				MysqlConnUtils.executeQuery(query);
			}
	}
	
	public static void loadIntoMysqlNCparsingsBsnWire(int yr, int qtr) throws SQLException, IOException {

		NLP nlp = new NLP();

		File folder = new File(baseTableParserFolder + yr + "/qtr" + qtr);
//		System.out.println("folder=" + folder.getAbsolutePath());
		File[] listOfFiles = folder.listFiles();

//		String path = baseFolder+"bac/",
		String text = "";
//		Utils.createFoldersIfReqd(path);
		PrintWriter pw = new PrintWriter(new File(baseTableParserFolder + "SEC/tmp.txt"));
		PrintWriter pwSaveFinal = new PrintWriter(new File(baseTableParserFolder + "SEC/tmp2.txt"));
		
		File origFile = new File("");
		
		for (int i = 0; i < listOfFiles.length; i++) {

			origFile = listOfFiles[i];
//			System.out.println("loadIntoMysqlNCparsings origFile=" + origFile.getAbsolutePath());
			if (nlp.getAllIndexEndLocations(origFile.getName(),
					Pattern.compile("^([12]{1}[09]{1}[\\d]{2}-[\\d]{2}-[\\d]{2})")).size() < 1)
				continue;

			text = Utils.readTextFromFile(origFile.getAbsolutePath());
			// repl cleans-up cases where there's a hard return that should not exist
			// text = text.replaceAll("[\r\n]{1,2}(?! ?\t?[\\d]{13})", "");
			if (origFile.exists())
				origFile.delete();
			pw = new PrintWriter(origFile);
			pw.println(text);
			pw.close();
			// System.out.println("saved origFile=" + origFile.getAbsolutePath());

			pwSaveFinal = new PrintWriter(new File(baseTableParserFolder + "SEC/"+origFile.getName()));
			
			text = Utils.readTextFromFile(baseTableParserFolder+"SEC/"+origFile.getName());
			text = text.replaceAll("( ?[\\p{Alnum}\\p{Punct}] ?)"
					+ "([\\d]{10}-[\\d]{2}-[\\d]{6})", "$1\r$2");
			text = text.replaceAll("(\n)", "\r").replaceAll("\r\r", "\r").replace("\\N", "''");

			
			pwSaveFinal.append(text.replaceAll("(?ism)([\\d]{10}-[\\d]{2}-[\\d]{6})", "\r$1").replaceAll("\r\r", "\r"));
			pwSaveFinal.close();
			System.out.println("saved origFile2=" + origFile.getAbsolutePath());
			
			String query = "LOAD Data LOCAL INFILE '" + origFile.getAbsolutePath().replaceAll("\\\\", "//")
					+ "' ignore INTO TABLE bac_tp_raw" + yr + "qtr" + qtr + " FIELDS TERMINATED BY ',' "
					+ "LINES TERMINATED BY '\\r';"
					+ "\r\r\rdelete from "
					+ "bac_tp_raw" + yr + "qtr" + qtr
					+ " where accno='' ;\r\r"
					+ "update ignore "
					+ "bac_tp_raw" + yr + "qtr" + qtr
					+" set accno=replace(accno,'\\n','')"
					+ "\r where left(accno,1)='\\n';";

			MysqlConnUtils.executeQuery(query);
			
		}
	}


	public void getMasterIdx(int year, int qtr, Calendar endDate) throws SocketException, IOException {
		//saves master.zip / .idx to baseFolder (we can change it to a different basefolder name if we like
		String localPath = baseTableParserFolder + year + "/QTR" + qtr + "/";
		GetContracts gk= new GetContracts();
		FileSystemUtils.createFoldersIfReqd(localPath);
		// create folder to save files to
		System.out.println("get masterIdx");
		File f = new File(localPath + "master.idx");
		// identify if file 'master.zip' is in local path.
		if(!GetContracts.downloadMasterIdx)
			return;
		
		if (f.exists() && !isCurrentQuarter(endDate)) {
			return;
		}
		if (f.exists() && isCurrentQuarter(endDate)) {
			// System.Xut.println("localPath:" + localPath);
			f.delete();//if current - delete old b/c new is updated daily until Qtr is over.
			QuarterlyMasterIdx.download(year, qtr, localPath, "master.zip");
			// ZipUtils.deflateZipFile(localPath + "/master.zip", localPath);
		}
		if (!f.exists()) {
			QuarterlyMasterIdx.download(year, qtr, localPath, "master.zip");
			// ZipUtils.deflateZipFile(localPath + "/master.zip", localPath);
		}
	}

	// this may help with speed b/c we only go through the text once to replace
	// with all the patterns - this pattern and method below would replace the
	// if <PRE> a problem just replace them all first with <xx> or something.
	// unfortunately negative look back doesn't work b/c you can hvae
	// <p>...<pre>....<pre/>...<p/>

	public Pattern ContentsWithinTagPattern = Pattern.compile("(?ims)(?:<(tr|p)[^>]*?>)(.+?)(?:</(\\1|table)>)");

	// \\1 references to first group ((tr|p): if pattern grp1 match is tr the
	// pattern to use to close is </tr> if it matches p it is closing </p>

	public void extractIndexHtmlInformation(String html, String pageUrl) {
		// System.Xut.println(html);
		document = Jsoup.parse(html, pageUrl);

		// it should be either pattersn <fiscal-year-end> or fiscal year end.
		Pattern ptrnFye = Pattern.compile("(?<=(Fiscal Year End:)).+(?=Type)");
		Matcher matchFye = ptrnFye.matcher(html);
		int cnt = 0;
		while (matchFye.find()) {
			fye = matchFye.group().replaceAll(" ", "");
			// System.Xut.println("fye=" + fye);
			cnt++;
			if (cnt >= 0)
				break;
		}

		// Elements fyeElement = document.getElementsMatchingOwnText(ptrnFye);
		// String fiscalYearEnd = null;

		Pattern ptrnA = Pattern.compile("(?s)(?<=Accepted).{0,5}\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
		// Elements dateElement = document.getElementsMatchingOwnText(ptrnA);
		// if (dateElement.size() > 0)
		// acceptedDate = dateElement.get(0).text();

		Matcher matchA = ptrnA.matcher(html);
		cnt = 0;
		while (matchA.find()) {
			acceptedDate = matchA.group().replaceAll("\r\n", "");
			System.out.println("acceptedDate=" + acceptedDate);
			cnt++;
			if (cnt >= 0)
				break;
		}

		// Pattern ptrnI = Pattern.compile("formGrouping"); //Results of
		// Operations
		Elements formGroups = document.getElementsByAttributeValue("class", "formGrouping");
		// html source is [<div class="formGrouping">]. class is
		// attribute of
		// div el and "formGrouping" is value. See tech notes for
		// getElementsByAttributeValue

		// System.Xut.println("XBRLInfoSecGov-2");
		// System.Xut.println("formGroups=" + formGroups.size());
		for (Element ele : formGroups) {
			// loop thru each element of formGroups
			Elements children = ele.getElementsByTag("div");
			// where element name/tag is "div" provide all elements
			boolean rightGroupFound = false;
			for (Element child : children) {
				// System.Xut.println(JSoupXMLParser.getAttributeValue(child,"class")
				// + "::" + child.text());
				if ("infoHead".equalsIgnoreCase(JSoupXMLParser.getAttributeValue(child, "class"))
						&& "Items".equalsIgnoreCase(child.text())) {
					// sub routine then checks for attribute "class" to
					// see if
					// it has value "infoHead" and to see if
					// child element value is equal to "Items"
					rightGroupFound = true;
				}
				if (rightGroupFound && "info".equalsIgnoreCase(JSoupXMLParser.getAttributeValue(child, "class"))
						&& child.text().startsWith("Item")) {
					// the div that hold the items value...
					String items = child.text();
					// System.Xut.println("child.text::" + items);
					String[] item = items.split("Item \\d[\\.\\d:]{0,4}");
					formItems = Arrays.toString(item);
					// System.Xut.println("itemStr::" + formItems);

					formStr = formItems.substring(formItems.indexOf(",") + 2, formItems.lastIndexOf("]"));

					// System.Xut.println("i[]: " +
					// Arrays.toString(item));
					// System.Xut.println("itemsCsv: " + itemsCsv);
				}
			}
		}
	}

	public void getBottomTag(String html, String acc) throws FileNotFoundException, SQLException {
		String bottomTagText = html.substring(Math.max(html.length() - 40000, 0), html.length());

		// System.Xut.println("bottomTagText:: " + bottomTagText);
		//
		Matcher matchBottomTags = BottomTagsPattern.matcher(bottomTagText);
		// pattern above grabs the bottomTag text (about 1 and 1/2 page)

		String bottomTags = "";
		row = 0;
		while (matchBottomTags.find()) {
			bottomTags = matchBottomTags.group()
					.replaceAll("<S>|<C>|</ARTICLE>|</TABLE>|</TEXT>|</DOCUMENT>|</SUBMISSION>|</TYPE>|<PAGE>|</PAGE>"
							+ "|<CAPTION>|<TABLE>|<TEXT>|<DOCUMENT>|<SUBMISSION>|<TYPE>|<ARTICLE>.{1,5}[1|9]{1}|", "")
					// .replaceAll("<", "")
					.replaceAll(">", ">\t").replaceAll("  [ ]+", "\t").replaceAll("\\$[ \t]+", "\\$")
					.replaceAll("[ \t]+\\)", "\\)").replaceAll("[\t ]+[\t ]+", "\t").replaceAll("[ \t]+[ \t]+", "\t")
					.replaceAll("[\r\n]{2,}$", "").replaceAll("\t", "||").replaceAll(" \r\n", "\r\n")
					.replaceAll("\r\n\\|\\|", "\r\n").replaceAll("[\r\n]{2,}", "\r\n").toUpperCase()
					.replaceAll("JAN-", "01-").toUpperCase().replaceAll("FEB-", "02-").toUpperCase()
					.replaceAll("MAR-", "03-").toUpperCase().replaceAll("APR-", "04-").toUpperCase()
					.replaceAll("MAY-", "05-").toUpperCase().replaceAll("JUN-", "06-").toUpperCase()
					.replaceAll("JUL-", "07-").toUpperCase().replaceAll("AUG-", "08-").toUpperCase()
					.replaceAll("SEP-", "09-").toUpperCase().replaceAll("OCT-", "10-").toUpperCase()
					.replaceAll("NOV-", "11-").toUpperCase().replaceAll("DEC-", "12-").toUpperCase()
					.replaceAll("JAN-", "01-").replaceAll(" \r\n", "\r\n");
			getTags(bottomTags, acc);
		}
	}

	public void getTags(String bottomTags, String acc) throws FileNotFoundException, SQLException {
		Pattern TagPattern = Pattern.compile("((?<=(<))[A-Z-\\d].{1,60}>.+\r\n)");

		Matcher matchTag = TagPattern.matcher(bottomTags);
		String tagValue = "";
		// System.Xut.println("12 fileDate=" + fileDate);
		// PrintWriter bottomTagPw = new PrintWriter(new File("c:/backtest/"
		// + getTableNamePrefixYearQtr(fileDate) + "tempBottomTags.txt"));

		while (matchTag.find()) {
			row++;
			// System.Xut.println("cik||fileDate:" + cik + "|" + fileDate);
			tagValue = matchTag.group().trim().replaceAll(">|<F[\\d]", "").replaceAll("\r\n", "");
			if (tagValue.contains("-200") || tagValue.contains("-199")) {
				tagValue = tagValue.trim().replaceAll("\\b0", "").replaceAll("-0|-(?=[\\d])", "\\/");
				// convert 03/01/1999 to 3/1/1999.
			}
			if (tagValue.toUpperCase().contains("PERIOD-TYPE")) {
				tagValue = tagValue.trim().replaceAll("-MOS", "").replaceAll("-MO", "").replaceAll("MO", "")
						.toUpperCase().replaceAll("MONTHS", "").toUpperCase().replace("YEAR", "12").toUpperCase()
						.replaceAll("QUARTER", "3").replaceAll("\\b0", "");
			}

			if (!tagValue.contains("-[\\d]") && !tagValue.contains("\\/")) {
				// not picking up (.86.
				tagValue = tagValue.trim().replaceAll("\\((?=[\\.\\d]{2})", "-")
						.replaceAll(",(?=[\\d])|\\$(?=[\\d])|(?<=[\\d])\\)", "").replaceAll("\\((?=[\\d])", "-");
				// convert to integer / double. $(1.35)==> -1.35
			}
			if (tagValue.contains("MULTIPLIER")) {
				tagValue = tagValue.trim().replaceAll("1000000", "-6").replaceAll("100000", "-5").replaceAll("1000",
						"-3");
			}

			// bottomTagPw.append(acc + "||" + fileDate + "||" + row + "||"
			// + tagValue);
			// bottomTagPw.append("\r\n");
			// System.Xut
			// .println((acc + "||" + fileDate + "||" + row + "||" + tagValue));
		}
		// bottomTagPw.close();
		// System.Xut.println("13 fileDate=" + fileDate);
		// Utils.loadIntoMysql("c:/backtest/"
		// + getTableNamePrefixYearQtr(fileDate) + "tempBottomTags.txt",
		// "tp_btags" + getTableNamePrefixYearQtr(fileDate));
	}

	public void getAccFromNCfile(int year, int quarter, String localPath, boolean parseEntireHtml)
			throws IOException, ParseException, SQLException {
		
		boolean isSecZipFile = true;
		

		String folder = baseTableParserFolder + "/" + year + "/QTR" + quarter + "/";
		String tpIdx = folder + "tpidx.csv";
		// System.Xut.println("getAcc localPath: " + localPath);
		File f3 = new File(tpIdx);
		// System.Xut.println("f3::" + f3);

		if (!f3.exists()) {
			// System.Xut
			// .println("mysql exported file of accno to parse DOES NOT EXIST");
			return;
		}

		BufferedReader tpIdxBR = null;
		try {
			tpIdxBR = new BufferedReader(new FileReader(tpIdx));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		String line;
		File fl = new File(baseTableParserFolder + "/tmp");
		int cnt = 0;

		pwYyyyQtr = new PrintWriter(fl);

		try {

			while ((line = tpIdxBR.readLine()) != null) {

				acc = line.split("\\|\\|")[0];
				cik = line.split("\\|\\|")[1];
				formType = line.split("\\|\\|")[2];
				companyName = line.split("\\|\\|")[4];
				// System.Xut.println("getAcc:: accno==" + acc + " cik: " + cik
				// + " formType: " + formType + " fileDate: "
				// + fileDate + " companyName: " + companyName);

				String filePath = localPath + "/" + acc + ".nc";
				filePath = filePath.replaceAll("\\\\", "//");
				File file = new File(filePath);

				// if file size>5mb it goes to the next file.
				String entireHtml = "";
				String html = "";
				String str10QK = "";
				String strEx13 = "";
				String str8k = "";
				String strEx99 = "";

				if (file.exists()) {

					fileDate = line.split("\\|\\|")[3];

					if (cnt == 0 || fl.length() > fileSizeToLoad) {

						pwYyyyQtr.close();
						fl = new File(folder + fileDate.substring(0, 10) + "_" + cnt);
						// System.Xut.println("filename="+fl.getAbsolutePath());

						if (fl.exists())
							fl.delete();

						pwYyyyQtr = new PrintWriter(fl);

						cnt++;
					}

					headerPortionOfFile = Utils.readSecHeaderPortionOnlyFromFile(filePath);

					// when pulling from .nc file items are in this format
					if (formType.contains("8") && !headerPortionOfFile.contains("<ITEMS>2")
							&& !headerPortionOfFile.contains("<ITEMS>5") && !headerPortionOfFile.contains("<ITEMS>7")
							&& !headerPortionOfFile.contains("<ITEMS>9")
							&& !headerPortionOfFile.toLowerCase().contains("financial")
							&& !headerPortionOfFile.toLowerCase().contains("result")
							&& !headerPortionOfFile.toLowerCase().contains("dividend")

							&& !headerPortionOfFile.toLowerCase().contains("other")) {
						// System.Xut
						// .println("form 8k does not contain items 2, items 9 or text
						// financial/result/dividend");
						continue;
					}

					entireHtml = Utils.readTextFromFile(filePath);
					html = "";
					str10QK = "";
					strEx13 = "";
					str8k = "";
					strEx99 = "";

					// whether its 10Q/K or 8k - set html to the text to parse
					// (10-Q+ex13 or ex99 for example if 8k). Then tpHtml or
					// tpTxt parser fails - run against entire filing

					if (formType.contains("8-K") && entireHtml.indexOf("<TYPE>EX-99") > 0) {
						str8k = NLP.removeGobblyGookGetContracts(NLP.getEx99s(entireHtml));
						html = str8k;
					}

					// set html to just 10-Q and ex13 if !parseEntireHtml.
					// Doesn't call ttp or tpHtml.
					// if parseEntireHtml -- after removal of gobbly gook is
					// less than maxFileSize - use entireHtml w/o gobbly gook
					if (parseEntireHtml && !formType.contains("8-K")) {
						html = NLP.removeGobblyGookGetContracts(entireHtml);
						// don't need to recheck removeGobblyGook!
					}

					if ((!parseEntireHtml || (parseEntireHtml && html.length() >= maxFileSize))
							&& ((formType.contains("10-Q") || formType.contains("10-K"))
									&& entireHtml.indexOf("<TYPE>10-") >= 0)
					// <==this must always must be

					) {

						if (parseEntireHtml && html.indexOf("<TYPE>EX-99") > 0) {
							// means fileSize>maxFileSize - so also include ex99
							// but only if parsing entire html
							strEx99 = NLP.getEx99s(html);
						}

						System.out.println("formType is 10-Q/K");
						str10QK = entireHtml.substring(entireHtml.indexOf("<TYPE>10-"));
						if (str10QK.indexOf("</TEXT>") >= 0) {
							str10QK = NLP.removeGobblyGookGetContracts(str10QK.substring(0, str10QK.indexOf("</TEXT>")));
							System.out.println("is 10-Q/K - and have 10K/Q string");
							html = str10QK;
						}

						if (entireHtml.indexOf("<TYPE>EX-13") >= 0) {
							strEx13 = entireHtml.substring(entireHtml.indexOf("<TYPE>EX-13"));
							if (strEx13.indexOf("</TEXT>") >= 0) {
								strEx13 = NLP.removeGobblyGookGetContracts(strEx13.substring(0, strEx13.indexOf("</TEXT>")));
							}
							// ex99 will be blank or have text if parsing entire
							// html and it is too large.
						}
						html = str10QK + " \r\r" + strEx13 + "\r\r" + strEx99;
					}

					/*
					 * String tmpHtml = str8k.replaceAll(
					 * "(?<=[ \\w\\.]{1})(\\r\\n)(?=[ \\w \\.]{1})", " "); String html8k99 =
					 * DivParser.getTextSimple(tmpHtml, "8k");
					 * DividendSentenceExtractor.getTextBlock(html8k99, fileDate, acc, "8-K"); NOT A
					 * LOT OF VALUE IN PARSING DIVIDEND DATA - UNTIL ITS COMPLETE I CAN'T MAKE USE
					 * OF IT.
					 */

					TableParserHTML tpHtml = new TableParserHTML(acc, fileDate, cik, tableCount, fye, formType,
							companyName);

					TableTextParser ttp = new TableTextParser(acc, fileDate, cik, tableCount, fye, formType,
							companyName);

					int idx = StringUtils.indexOfIgnoreCase(html, "</tr>");
					int idxTd = StringUtils.indexOfIgnoreCase(html, "</td>");
					// System.Xut.println("idx="+idx+" idxTd="+idxTd);

					int idxEx13 = StringUtils.indexOfIgnoreCase(strEx13, "</tr>");
					int idxTdEx13 = StringUtils.indexOfIgnoreCase(strEx13, "</td>");
					// System.Xut.println("idxEx13="+idxEx13+" idxTdEx13="+idxTdEx13);

					// PrintWriter tempPw = new PrintWriter(new File(
					// "c:/backtest/temp2/" + acc + "__3.txt"));

					if (formType.contains("8") && !headerPortionOfFile.contains("<ITEMS>2")
							&& !headerPortionOfFile.contains("<ITEMS>5") && !headerPortionOfFile.contains("<ITEMS>7")
							&& !headerPortionOfFile.contains("<ITEMS>9")
							&& !headerPortionOfFile.toLowerCase().contains("financial")
							&& !headerPortionOfFile.toLowerCase().contains("result")
							&& !headerPortionOfFile.toLowerCase().contains("dividend")

							&& !headerPortionOfFile.toLowerCase().contains("other")) {
						// System.Xut
						// .println("form 8k does not contain items 2, items 9 or text
						// financial/result/dividend");
						continue;
					}

					entireHtml = Utils.readTextFromFile(filePath);
					html = "";
					str10QK = "";
					strEx13 = "";
					str8k = "";
					strEx99 = "";

					// whether its 10Q/K or 8k - set html to the text to parse
					// (10-Q+ex13 or ex99 if 8-k, entire file ). Then tpHtml or
					// tpTxt parser fails - run against entire filing

					if (formType.contains("8-K") && entireHtml.indexOf("<TYPE>EX-99") > 0) {
						str8k = NLP.removeGobblyGookGetContracts(NLP.getEx99s(entireHtml));
						html = str8k;
					}

					// set html to just 10-Q and ex13 if !parseEntireHtml.
					// Doesn't call ttp or tpHtml.
					// if parseEntireHtml -- after removal of gobbly gook is
					// less than maxFileSize - use entireHtml w/o gobbly gook
					if (parseEntireHtml && !formType.contains("8-K")) {
						html = NLP.removeGobblyGookGetContracts(entireHtml);
						// don't need to recheck removeGobblyGook!
					}

					if ((!parseEntireHtml || (parseEntireHtml && html.length() >= maxFileSize))
							&& ((formType.contains("10-Q") || formType.contains("10-K"))
									&& entireHtml.indexOf("<TYPE>10-") >= 0)
					// <==this must always must be

					) {

						if (parseEntireHtml && html.indexOf("<TYPE>EX-99") > 0) {
							// means fileSize>maxFileSize - so also include ex99
							// but only if parsing entire html
							strEx99 = NLP.getEx99s(html);
						}

						System.out.println("formType is 10-Q/K");
						str10QK = entireHtml.substring(entireHtml.indexOf("<TYPE>10-"));
						if (str10QK.indexOf("</TEXT>") >= 0) {
							str10QK = NLP.removeGobblyGookGetContracts(str10QK.substring(0, str10QK.indexOf("</TEXT>")));
							System.out.println("is 10-Q/K - and have 10K/Q string");
							html = str10QK;
						}

						if (entireHtml.indexOf("<TYPE>EX-13") >= 0) {
							strEx13 = entireHtml.substring(entireHtml.indexOf("<TYPE>EX-13"));
							if (strEx13.indexOf("</TEXT>") >= 0) {
								strEx13 = NLP.removeGobblyGookGetContracts(strEx13.substring(0, strEx13.indexOf("</TEXT>")));
							}
							// ex99 will be blank or have text if parsing entire
							// html and it is too large.
						}
						html = str10QK + " \r\r" + strEx13 + "\r\r" + strEx99;
					}

					System.out.println("b localPath=" + localPath);

					// html is either entire html or just 10k/ex-13 based on
					// whether I asked to parse entire html when calling class
					// or not. Now either ttp or tpHtml is called which is
					// agnostic as to what is in html/text passed.
					// PrintWriter tempPw = new PrintWriter(new File(
					// "c:/backtest/temp2/"
					// + acc + "__.txt"));

					if ((formType.contains("10") || formType.contains("8")) && html.length() > 2) {

						if (idx >= 0 && idxTd >= 0) {

							tpHtml.getTablesFromFiling(html, false, parseEntireHtml);

							// tempPw.append(html);
							// tempPw.close();

							if (idxEx13 < 0 || idxTdEx13 < 0) {
								tableCount = 100;

								// PrintWriter tempPw2 = new PrintWriter(new File(
								// "c:/backtest/temp2/"
								// + acc + "htmlJustEx13_.txt"));
								// tempPw2.append(strEx13);
								// tempPw2.close();
								ttp.tableTextParser(strEx13, false, parseEntireHtml, adjustIdxLoc);

							}
							// if not html -- no <html in entireHtml (complete
							// filing)
						} else {
							// tempPw.append(html);
							// tempPw.close();
							System.out.println("adjustIdxLoc=" + adjustIdxLoc);
							ttp.tableTextParser(html, false, parseEntireHtml, adjustIdxLoc);

						}
					}

				}

				str10QK = "";
				strEx13 = "";
				html = "";
				entireHtml = "";
				str8k = "";
			}
			tpIdxBR.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		pwYyyyQtr.close();
		
		loadIntoMysqlNCparsings(year,quarter,isSecZipFile);		

	}

	public static void main(String[] arg) throws IOException, ParseException, SQLException {

		// TODO: finish dividend parser so that I pair dividend amt, frequency,
		// share type etc and validate either by simple ticker reference or b/c
		// I can match against historical. Once this is finished think about
		// grabbing from 8-Ks.

		// TODO: save 8-Ks that have eps info saved to file so I can parse more
		// quickly.

		@SuppressWarnings("resource")
		Scanner Scan = new Scanner(System.in);
		System.out.println("Enter start date of time period to check for tables to parse (yyyymmdd)");
		String startDateStr = Scan.nextLine();

		System.out.println("Enter end date of time period to check for tables to parse (yyyymmdd)");
		String endDateStr = Scan.nextLine();

		System.out.println("Do you want to parse just files not in mysql (Y/N)?");
		boolean mysql = false;
		adjustIdxLoc = "1";
		if (Scan.nextLine().toLowerCase().contains("y"))
			mysql = true;

		if (mysql = true) {
			System.out.println(
					"Do you want to also adjust table name end idx mysql (Y/N)? Say Y if getting missed in mysql");
			if (Scan.nextLine().toLowerCase().contains("y"))
				adjustIdxLoc = "2";
		}

		boolean parseEnitreHtml = true;
		System.out.println("To parse just 10-Q/K & EX-13 (enter 1)?. "
		// + "\r\r\rTODO: RESET public static String skipAmend - see 'and cik between 0
		// and 10000 '"
		);

		if (Scan.nextLine().toLowerCase().contains("1")) {
			parseEnitreHtml = false;
		}

		System.out.println("parseEntireHtml=" + parseEnitreHtml);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date sDate = new Date();
		sDate = sdf.parse(startDateStr);
		Calendar startDate = Calendar.getInstance();
		startDate.setTime(sDate);
		Date eDate = new Date();
		eDate = sdf.parse(endDateStr);
		Calendar endDate = Calendar.getInstance();
		endDate.setTime(eDate);
		String earliestDateStr = "19930101";
		Date firstDate = sdf.parse(earliestDateStr);
		Calendar badDate = Calendar.getInstance();
		badDate.setTime(firstDate);

		if (endDate.before(startDate)) {
			System.out.println("End date must be later than start date. Please re-enter.");
			return;
		}

		if (endDate.after(Calendar.getInstance())) {
			System.out.println("End date cannot be later than today. Please re-enter.");
			return;
		}

		TableParser tableParser = new TableParser();
		tableParser.dateRangeQuarters(startDate, endDate, mysql, parseEnitreHtml);
		
	}
}
