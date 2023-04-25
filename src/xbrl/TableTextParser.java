package xbrl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class TableTextParser {
	TableParser tp = new TableParser();

	public String acc = "0";
	public static String startGroup = "";
	public static String startGroupLimited = "";
	public String fileDate = "0";
	public String cik = "0";
	public int tableCount;
	public int tbC2 = 0;
	public static int noteInChEndIdx = 100;
	public static int mergeCnt;
	public String fye = "0";
	public String formType = "0";
	public static String tableNameLong = "";
	public static String decimal;
	public static String tableSentence = "";
	public static String tableSentenceLastLine = "";
	public static String tableNameShort;
	public static String companyName;// from master.idx
	public static String coNameMatched = ""; // matched.
	public static String coNameOnPriorLine = ""; // co name matched.
	public static String allColText = "";
	public static int coMatch;
	public static int rownameEIdx = 0;
	public static String tableSaved = "";
	public static boolean parseTableFromFolder = false;
	public static boolean removeNote = false;
	// public static boolean noteInCh = false;
	public static boolean twoDataColsForEachColHdg = false;
	public static boolean hasTabs = false;
	public static boolean runAll = true;
	public static boolean parseEntireText;
	// public static boolean remainder = false;
	public static String tsShort = "";
	public static String tsPattern = "";

	public TableTextParser(String acc, String fileDate, String cik, int tableCount, String fye, String formType,
			String companyName) {
		this.acc = acc;
		this.fileDate = fileDate;
		this.cik = cik;
		this.tableCount = tableCount;
		this.fye = fye;
		this.formType = formType;
		this.companyName = companyName;

	}

	public static int endStartGrpRow = 0;
	public static int rownumber = 0;
	// public String tp_CH_txt;
	public static int numberOfCHsRatio = 0;
	public static boolean booleanData;
	public static boolean removePrior = false;
	public static boolean hasOddBall = false;
	public static boolean isRownameheader = false;
	public static boolean allColHeadingsCaptured = false;

	public static TreeMap<Integer, List<String>> mapOfCh = new TreeMap<Integer, List<String>>();
	public static TreeMap<Integer, List<String[]>> mapAllCH = new TreeMap<Integer, List<String[]>>();
	public static TreeMap<Integer, List<String[]>> originalMapAllCH = new TreeMap<Integer, List<String[]>>();
	public static TreeMap<Integer, List<String[]>> mapAllCH2 = new TreeMap<Integer, List<String[]>>();
	public static Map<Integer, List<String[]>> mapData = new TreeMap<Integer, List<String[]>>();
	public static TreeMap<Integer, Double> mapYearStartIdx = new TreeMap<Integer, Double>();

	// key=table row,
	public static List<String[]> listEdtPerFinal = new ArrayList<String[]>();
	public static List<String[]> listTocEdtPerTotColsTblnm = new ArrayList<String[]>();
	// listTocEdtPerTotColsTblnm=edt[0],per[1],tc[2],tn[3],tnShort[4],col[5],tocTblNo[6],
	// pgNo[7],tsShort[8] (and ALL tocs!).

	public static List<String> listColumnHeadingText = new ArrayList<String>();
	public static List<String[]> listCHEndedYrandMoText = new ArrayList<String[]>();
	public static List<Integer[]> sIdxC1NumOfCsLastCeIdx = new ArrayList<Integer[]>();
	// int[[]=0:sIdx 1st data col, 1:number of data cols,2:last col eIdx, 3:
	// eidx 1st col

	public static List<String> list_TS_tsShort_tsPattern_tnValid = new ArrayList<String>();
	// public static List<String> listCHLines = new ArrayList<>();
	public static List<String> listCHLinesFromGetStartGroupCHLinesMethod = new ArrayList<>();
	public static Map<Integer, Integer[]> mapDataColIdxLocs = new TreeMap<Integer, Integer[]>();
	public static Map<Integer, Integer> mapNumberOfDataCols = new TreeMap<Integer, Integer>();

	public static List<Integer[]> listDataColsSIdxandEIdx = new ArrayList<Integer[]>();
	public static List<String> listCHbyIdxLoc = new ArrayList<String>();
	public static List<String> tmp2ListCHbyIdxLoc = new ArrayList<String>();
	public static List<String> ListCHbyMidptLoc = new ArrayList<String>();
	public static List<String[]> listEdtPerIdx = new ArrayList<>();
	public static List<String[]> listEdtPerTableSentence = new ArrayList<>();
	public static List<Double> listPrevLgEidx = new ArrayList<>();
	public static Pattern patternLiabilitiesAndStockholdersEq = Pattern
			.compile("(L((?i)IABILITIES.{1,3}AND.{10,18})|S((?i)TOCKHOLDER.{1,5}))E((?i)QUITY)");

	public static Pattern patternMoDayYear = Pattern.compile("(?ims)([\\xA0 \t\r\n]*("
			+ "(\\d{1,2}[\\/-]{1}\\d{1,2}[\\/-]{1}(\\d{2,4}" + "[$\t \\s\r\n]{0,2}|\\d{4}[$\t \\s\r\n]{0,2}))"
			+ "|[1-3]{1}\\d-(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)-[90]{1}\\d))");

	public static Pattern patternMonthDayYY = Pattern
			.compile("(?i)(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC).{1,10}[,\\d]{1,3}.{1,2}\\'\\d\\d");

	// Nov. 1, 2003
	public static Pattern patternMoDayYearLong = Pattern.compile(
			"(?ims)([\\xA0 \t\r\n]*((JANUARY |FEBRUARY |MARCH |APRIL |JUNE |JULY| AUGUST |SEPTEMBER |OCTOBER |NOVEMBER |DECEMBER |"
					+ "JAN[\\. ]{1}|FEB[\\. ]{1}|MAR[\\. ]{1}|APR[\\. ]{1}|MAY |JUN[\\. ]{1}|JUL[\\. ]{1}"
					+ "|AUG[\\. ]{1}|SEP[\\. ]{1}|OCT[\\. ]{1}|NOV[\\. ]{1}|DEC[\\. ]{1})[\\d]{1,2},? [12]{1}[09]{1}[\\d]{2}))");

	public static Pattern AllColumnHeadingPattern = Pattern
			.compile(TableParser.ColumnHeadingPattern.pattern() + "|" + TableParser.OddColumnPattern.pattern() + "|"
					+ patternMoDayYear.pattern() + "|" + patternMonthDayYY.pattern());

	public Pattern patternBS = Pattern.compile(TableParser.BalanceSheetAllCapsPattern.pattern() + "|"
			+ TableParser.BalanceSheetInitialCapsPattern.pattern());

	public Pattern patternIS = Pattern
			.compile(TableParser.IncomeAllCapsPattern.pattern() + "|" + TableParser.IncomeInitialCapsPattern.pattern());

	public Pattern patternCF = Pattern.compile(
			TableParser.CashFlowAllCapsPattern.pattern() + "|" + TableParser.CashFlowInitialCapsPattern.pattern());

	public static Pattern patternNoAlphaSpaceNumberspace = Pattern
			.compile("(?<!([A-Za-z,]))( [-\\.]?[\\d,\\.]{2,}|  [0-]{1,3})");

	public static Pattern patternSpaceNumberNoYearNoMo = Pattern.compile(
			"([ ]{2}[\\$\\(]{0,2}[\\d]{3}[$\\.\r\n ]{1,2}|" + "[\\$\\(]{1,2}[\\d,]{2,3}|\\d,[\\d]{3}|[\\d]{0,}\\.\\d)");

	public static Pattern patternNoMonthOrWeek = Pattern.compile("(?i)(months|weeks|mos\\.?|mo\\.?|wks)");

	public static Pattern patternWordOneSpaceNumber = Pattern.compile("[A-Za-z;,'] [\\d]");

	public static Pattern patternThreeSpaceSmallNumberThreeSpaceSmallNumber = Pattern
			.compile("(?s)([ ]{3}|\t)\\(?[\\d]{1,3}\\)?(\t|[ ]{3,})\\(?[\\d]{1,3}");

	public static Pattern patternThreeDigitNumber = Pattern
			.compile(",[\\d]{3}|(  \\(?\\.|  \\(?)[\\d]{3}\\)?[$ ]|\\d\\.[\\d]{1,}");

	public static Pattern patternDollarSignOrParenNotFollowedbyAlpha = Pattern
			.compile("(?<![a-zA-Z].{0,2})(\\$|\\)|\\()(?!.+([A-Za-z]{1,}))");

	public static Pattern patternOpenParenNotFollowedbyAlpha = Pattern.compile("\\([\\$ ]{0,2}(?!.+([A-Za-z]{1,}))");

	public static Pattern patternAlphabet = Pattern.compile("[A-Za-z]{1,}");

	public static Pattern patternColumnHeadingSimple = Pattern.compile(
			"(?i)three|six|nine|twelve|week|month|year|ended|january|march|april|may |june|july|august|september"
					+ "|october|november|december|[, ]{1}[12]{1}[09]{1}[0-9]{2}");

	// b/c the proximity is large I can't have BS go second or I could pickup a
	// BS that comes after TOC where cash flow is in TOC byut BS is the

	/*
	 * // tableName of actual BS. public static Pattern patternTOCTableNamePattern =
	 * Pattern .compile(
	 * "(?is)(((balance.{1,3}sheet).{1,100}([1,2]{1}[09]{1}[0-9]{2}|end(ed|ing)|year)).{1,500}"
	 * + "((cash.{1,3}flow).{1,100}([1,2]{1}[09]{1}[0-9]{2}|end(ed|ing)|year)))" );
	 */

	public String getAllColText() throws IOException, ParseException {

		NLP nlp = new NLP();

		// each key in map is line # & list is all the col hdgs on that line -
		// each list is an array that holds for each col hdg pattern the
		// startIdx and line count. So I need to get just ary[1] which is col
		// hdg text that was matched

		String line, allWords = "", word = "";
		int cntChL = 0, prevI = 0;

		Matcher colHdgsLikeInception;
		allColText = "";
		boolean hasInceptionLikeHeading = false;

		for (int i = 0; i < listCHLinesFromGetStartGroupCHLinesMethod.size(); i++) {
			line = listCHLinesFromGetStartGroupCHLinesMethod.get(i).replaceAll("!", "");
			colHdgsLikeInception = NLP.patternInception.matcher(line);
			if (colHdgsLikeInception.find()) {
				// System.xut.println("found inception in col hdg -- line=="+line);
				// if inception|development etc in CH - dont get PMY values
				// (counted)
				hasInceptionLikeHeading = true;
			}

			line = line.replaceAll("!", "").replaceAll("[ ]{2,}", "xx");
			String[] wordsSplit = line.split("xx");
			for (int c = 0; c < wordsSplit.length; c++) {
				word = wordsSplit[c].replaceAll("[-]{2,}", "");

				if (prevI != i)
					cntChL = 0;

				if (c == 0 && word.length() > 1) {
					cntChL++;
					allWords = "|L" + (i + 1) + "C1:" + wordsSplit[c] + "|";
					// NLP.pwNLP.append(//NLP.printLn( "1 words=",
					// allWords));
				}

				else if (word.length() > 1 && c > 0) {
					cntChL++;
					allWords = allWords + "|L" + (i + 1) + "C" + cntChL + ":" + wordsSplit[c] + "|";
					// NLP.pwNLP.append(//NLP.printLn( "2 words=",
					// allWords));
				}
				prevI = i;
			}

			allColText = allColText + allWords;
			allWords = "";
			// NLP.pwNLP.append(//NLP.printLn("allLines words added allColText=",
			// allColText));
		}

		if (hasInceptionLikeHeading) {
			return allColText;
		}

		allColText = allColText.replaceAll("[\\|]{2,}", "\\|").replaceAll("'|--|__", "");
		String prevMatch = "", match = "";
		int pCntD, mCntD, yCntD;
		// pair where pCntD=1 or note the same provided there is no other ended
		// value by running generic ended value (such as 5 or 2 months or 100
		// days etc). Then ensure nowhere in allColText the words
		// 'inception|predecessor' etc appear. Then if only one P value - I can
		// use that across all. I should also record the P value (3|6|9|12)
		// directly after pCntD as pCntD:1:12. Then if per=0 rt bef insert into
		// mysql I sub in this P value by cutting it from the allColText

		List<String> tmpCHcntList = new ArrayList<String>();
		tmpCHcntList = (nlp.getAllMatchedGroups(allColText, TableParser.EndedHeadingPattern));

		int cnt = 0, per = 0;

		String edt = "", prevPer = "", prevEdt = "";
		TreeMap<Integer, Integer> mapPer = new TreeMap<Integer, Integer>();

		for (int i = 0; i < tmpCHcntList.size(); i++) {
			match = tmpCHcntList.get(i).trim();
			per = Integer.parseInt(NLP.getPeriodOnly(match));
			mapPer.put(per, per);
		}

		pCntD = mapPer.size();
		// NLP.pwNLP.append(//NLP.printLn("z. pCntD=", pCntD + " per=" + per));

		tmpCHcntList = (nlp.getAllMatchedGroups(allColText, TableParser.MonthPatternSimple));

		cnt = 0;
		String mCntDStr = "", str = "";
		for (int i = 0; i < tmpCHcntList.size(); i++) {
			// only get month name - don't parse - day - first split=month name.
			str = tmpCHcntList.get(i).trim();
			if (str.indexOf(" ") > 0) {
				match = str.substring(0, str.indexOf(" "));
			} else
				match = str;
			if (i == 0) {
				cnt++;
			}
			if (i > 0 && !match.equals(prevMatch)) {
				cnt++;
			}
			prevMatch = match;
		}

		mCntD = cnt;
		mCntDStr = str;

		tmpCHcntList = (nlp.getAllMatchedGroups(allColText, TableParser.YearOrMoDayYrPattern));

		cnt = 0;
		for (int i = 0; i < tmpCHcntList.size(); i++) {
			match = tmpCHcntList.get(i).trim();
			if (i == 0) {
				cnt++;
			}
			if (i > 0 && !match.equals(prevMatch)) {
				cnt++;
			}
			prevMatch = match;
		}
		yCntD = cnt;

		String pCntDStr = pCntD + "";
		if (pCntD == 1 && per != 0 && per != -1) {
			pCntDStr = pCntD + "|" + per + "";
		}

		if (mCntD == 1) {
			mCntDStr = "|" + mCntDStr;
		} else
			mCntDStr = ""; // only if mCntD=1
		// need to ensure I get Day as well!

		// NLP.pwNLP.append(//NLP.printLn("mCntDstr=", mCntDStr));
		allColText = (allColText + "|pCntD:" + pCntDStr + "|yCntD:" + yCntD + "|mCntD:" + mCntD + mCntDStr)
				.replaceAll("[\\|]{2,}", "\\|");

		return allColText;

	}

	public static List<String> getStartGroupCHLines(String startGroupAtCHLines) throws IOException, ParseException {

		// adjustStartGroup tries to NOT changes the spacing! Spacing can't
		// change -what was removed must be equal in length to what was replaced

		// System.xut.println("startGroupAtCHLines="+startGroupAtCHLines);
		if (null == startGroupAtCHLines)
			return listCHLinesFromGetStartGroupCHLinesMethod;

		NLP nlp = new NLP();
		TableParser tp = new TableParser();

		// NLP.pwNLP.append(//NLP.printLn("@getStartGroupCHLines=", startGroupAtCHLines));

		// System.xut.println("list_TS_tsShort_tsPattern_tnValid.size="
		// + list_TS_tsShort_tsPattern_tnValid.size());
		if (list_TS_tsShort_tsPattern_tnValid.size() < 1) {
			list_TS_tsShort_tsPattern_tnValid = NLP.parseTableSentence(startGroupAtCHLines,
					companyName.substring(0, Math.min(companyName.length(), 7)), true, tableNameLong, false);
		}

		coNameOnPriorLine = NLP.getCompanyNameOnPriorLine(startGroupAtCHLines, coNameMatched).replaceAll("'", "");
		// NLP.pwNLP.append(//NLP.printLn("9 tableNameLong=", tableNameLong + "
		// tableNameShort=" + tableNameShort));
		if ((tableNameLong.length() < 2 && list_TS_tsShort_tsPattern_tnValid.get(6).length() > 1)
				|| (list_TS_tsShort_tsPattern_tnValid.size() > 6
						&& list_TS_tsShort_tsPattern_tnValid.get(6).length() > 1
						&& list_TS_tsShort_tsPattern_tnValid.get(3).equals("true"))) {
			tableNameLong = list_TS_tsShort_tsPattern_tnValid.get(6).replaceAll("\\\\|\\)|\\(|\\$|'|\\*", "");
			tableNameShort = tp.getTableNameShort(tableNameLong);
			// NLP.pwNLP.append(//NLP.printLn("10 tablenameLong=", tableNameLong + "
			// tableNameShort==" + tableNameShort
			// + " tablename is valid?=" + list_TS_tsShort_tsPattern_tnValid.get(3)));
		}

		// NLP.pwNLP.append(//NLP.printLn("9 aft parseTableSentence tsShort=", tsShort));

		// if no ts - see if col hdg is above rowname and on same
		// row as year pattern.

		if (list_TS_tsShort_tsPattern_tnValid.size() > 0 && list_TS_tsShort_tsPattern_tnValid.get(0).length() == 0) {

			// if tableSentence was previously found I won't ck for CH above
			// rowname
			String tnValid = list_TS_tsShort_tsPattern_tnValid.get(3);
			list_TS_tsShort_tsPattern_tnValid.clear();
			// System.xut.println("text to use to
			// getColumnHeadingAboveRowname="+startGroupAtCHLines);
			list_TS_tsShort_tsPattern_tnValid = NLP.getColumnHeadingAboveRowname(startGroupAtCHLines, tnValid,
					list_TS_tsShort_tsPattern_tnValid);
			// System.xut.println("at ts.len=0 list_TS_tsShort_tsPattern_tnValid.size()="
			// +list_TS_tsShort_tsPattern_tnValid.size()+" printing =");
			//NLP.printListOfString(list_TS_tsShort_tsPattern_tnValid);
		}

		if (list_TS_tsShort_tsPattern_tnValid.size() > 0) {
			// System.xut.println("tsShort=" + tsShort + " tableSentence="
			// + tableSentence);
			tableSentence = list_TS_tsShort_tsPattern_tnValid.get(0) + tableSentenceLastLine;
			if (!tsShort.equals("zz")) {
				tsShort = list_TS_tsShort_tsPattern_tnValid.get(1);
				// System.xut.println("tsShort=" + tsShort + " tableSentence="
				// + tableSentence);
			}
			tsPattern = list_TS_tsShort_tsPattern_tnValid.get(2)
					.replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)", "");
		}

		// if only have a month in tsshort - it can't be too far to the right -
		// list_TS_tsShort_tsPattern_tnValid.get(4) is idx loc of first word in
		// line

		if (list_TS_tsShort_tsPattern_tnValid.size() == 0 || list_TS_tsShort_tsPattern_tnValid.get(3).equals("false")) {
			List<Integer> tmpI = nlp.getAllIndexEndLocations(startGroupAtCHLines, TableParser.TableNamePattern);
			int tmpEidx = 0;
			if (tmpI.size() > 0) {
				tmpEidx = tmpI.get(Math.max(tmpI.size() - 1, 0));
			}

			// double startTime = System.currentTimeMillis();
			// NLP.pwNLP.append(//NLP.printLn("1 at validateTablename 1- tablenamelong",
			// tableNameLong + " tableNameShort=" + tableNameShort));

			boolean tnValid = NLP.validateTablename(startGroupAtCHLines.substring(tmpEidx),
					companyName.substring(0, Math.min(companyName.length(), 7)), startGroupAtCHLines);
			if (!tnValid) {
				// System.xut.println("tnValid="+tnValid);
				tableNameShort = "validate";
				// NLP.pwNLP.append(//NLP.printLn("tableNameShort set to validate=",
				// tableNameShort));
			}
		}

		if (!tsShort.equals("MY") && tsShort.trim().length() > 0) {
			listEdtPerTableSentence = NLP.getTablesentenceColumnHeadings(tableSentence, tsShort, tsPattern);
		}

		//NLP.printListOfStringArray("listEdtPerTableSentence", listEdtPerTableSentence);

		// this just returns the lines in a list of lines in their original form
		// - but only the CH lines (no ts etc). Puts them in list reverse order.
		// Also checks if CH Lines put in list have odd ball columns and set
		// boolean value hasOddBall to true if it does

		boolean tableNameFound = false;
		hasOddBall = false;
		String[] lines = startGroupAtCHLines.split("[\r\n]");
		listCHLinesFromGetStartGroupCHLinesMethod.clear();

		// firstColIdxMin is initially set at 2 minus end of rowname - I move it
		// back another 6 from there

		int cnt = 0, totCnt = 0, limitLinesToCntIncludingBlanks = 8, lineEnd,
				firstColIdxMin = sIdxC1NumOfCsLastCeIdx.get(0)[0] - 6;
		if (rownameEIdx > 25 && (rownameEIdx - 8) < firstColIdxMin)
			firstColIdxMin = rownameEIdx - 8;

		if (firstColIdxMin < 10)
			return null;

		String lineStr;
		// gets limited startGroup going from bottom to top
		int cntFromBottomUp = 0;
		for (int i = (lines.length - 1); i >= 0; i--) {
			cntFromBottomUp++;
			lineStr = lines[i];
			// if line length is less than where first col starts skip.
			if (lineStr.length() < firstColIdxMin) {
				totCnt++;
				continue;
			}

			if (totCnt > limitLinesToCntIncludingBlanks)
				break;

			if (lineStr.substring(firstColIdxMin - 1).replaceAll("[- =\t]|<.*[^>]>", "").length() < 2) {
				totCnt++;
				continue;
			}

			if (totCnt > limitLinesToCntIncludingBlanks) {
				break;
			}

			// NLP.print("1. line cnt=", "" + cnt);
			// NLP.print("column heading lines for compare all by idx=",
			// lineStr);

			List<Integer> listAnyVis = nlp.getAllIndexStartLocations(lineStr, nlp.patternAnyVisualCharacter);
			// get last visual char match idx loc on line.
			lineEnd = listAnyVis.get(listAnyVis.size() - 1);
			Integer lineStart = listAnyVis.get(0);
			String linesubstring = lineStr.substring(lineStart, lineEnd + 1);

			// NLP.pwNLP.append(//NLP.printLn(

			// "lineStr start of 1st visual char to last=",
			// linesubstring + "\r lineStart=" + lineStart + " lineEnd=" + lineEnd
			// + "\rlinesubstring.split( ).length=" + linesubstring.split(" ").length
			// + " lineStr.substring(Math.min(lineStr.length(), firstColIdxMin)"
			// + lineStr.substring(Math.min(lineStr.length(), firstColIdxMin)) + " cntY="
			// + (nlp.getAllIndexEndLocations(linesubstring,
			// Pattern.compile("\\d\\d\\d\\d")).size())));

			// this gets tableSentence: start idx of line<35, there are no
			// instances of 2 or more ws and its length>30
			// NLP.pwNLP.append(//NLP.printLn("1. line=", lineStr));

			if ((linesubstring.split("  ").length < 2 && lineStart < 35
					&& lineStr.substring(lineStart, lineEnd).length() > 30)
					|| ((nlp.getAllIndexEndLocations(linesubstring, Pattern.compile("\\d\\d\\d\\d")).size() > 1)
							&& linesubstring.split("  ").length < 2)) {
				// System.xut.println("2. line="+lineStr);
				continue;
			}
			// discard a tablename line w/o any col hdg patterns
			if (nlp.getAllIndexStartLocations(lineStr, TableParser.TableNamePattern).size() > 0
					&& nlp.getAllIndexStartLocations(lineStr, tp.AllColumnHeadingPattern).size() < 1) {

				// System.xut
				// .println("tablenamefound and line discarded -cntFromBottomUp="
				// + cntFromBottomUp);
				tableNameFound = true;
				continue;

			}
			// lineStart CAN start prior to firstColIdxMin - but there must also
			// be 2 or more instances of [ \t[{3} from first col idx (which is 6
			// prior to end of rowname). So any instance of 2 cols will pick up
			// splits of 2 and therefore len=2. Won't work for 1 col -but no
			// need then.

			if (lineEnd < firstColIdxMin
					/*
					 * goes from bottom ch line up - so if at 1,2 or 3 (0,1,2) make there be at
					 * least 2 instances of 3 ws/tabs. Else just 2. first occurs before 1st col b/c
					 * I cut at least 4 or 5 before 1st col and that leaves ws
					 */
					|| (lineStr.substring(firstColIdxMin).split("[ \t]{3}").length < 2 && cnt > 2)
					|| (nlp.getAllMatchedGroups(lineStr, TableParser.EndedHeadingPattern).size() < 2
							&& nlp.getAllMatchedGroups(lineStr, TableParser.YearOrMoDayYrPattern).size() < 2
							&& nlp.getAllMatchedGroups(lineStr, TableParser.MonthPattern).size() < 2
							&& lineStr.substring(firstColIdxMin).split("[ \t]{2}").length < 2

							&& nlp.getAllMatchedGroups(lineStr.substring(Math.min(firstColIdxMin, lineStr.length())),
									tp.AllColumnHeadingPattern).size() < 2)
					|| (lineStr.substring(Math.min(lineStr.length(), firstColIdxMin)).toLowerCase().contains(" and ")
							&& lineStr.substring(firstColIdxMin).split("[ \t]{3}").length < 2)) {
				totCnt++;

				continue;
			}

			// once tablename is found - likely at end of start group if it has
			// already gone 6 lines from bottom of start grp up
			if (totCnt > limitLinesToCntIncludingBlanks || (cntFromBottomUp > 6 && tableNameFound))
				break;

			// System.xut
			// .println("2. line cnt="
			// + cnt
			// + " column heading lines for compare all by idx="
			// + lineStr
			// + " listStartIdxFirstColandNoDataColsAndLastColEndIdx.get(0)[0]"
			// + sIdxC1NumOfCsLastCeIdx.get(0)[0]);
			if (nlp.getAllEndIdxAndMatchedGroupLocs(lineStr, TableParser.OddColumnPattern).size() > 0) {
				// NLP.pwNLP.append(//NLP.printLn(

				// "line has odddBall=", lineStr + " oldBall matched=" + nlp
				// .getAllEndIdxAndMatchedGroupLocs(lineStr,
				// TableParser.OddColumnPattern).get(0)[1]));
				hasOddBall = true;
			}

			// NLP.pwNLP.append(//NLP.printLn("totCnt=",
			// totCnt + " if > limit break limitLinesToCntIncludingBlanks=" +
			// limitLinesToCntIncludingBlanks));

			if (totCnt > limitLinesToCntIncludingBlanks)
				break;

			// NLP.pwNLP.append(//NLP.printLn("adding? lineStr", lineStr));
			// NLP.pwNLP.append(//NLP.printLn("lineStart=", "" + lineStart + ""));
			// NLP.pwNLP.append(//NLP.printLn("1stcolSt=", sIdxC1NumOfCsLastCeIdx.get(0)[0] +
			// ""));
			// NLP.pwNLP.append(//NLP.printLn("numberOfCHsRatio", "" + numberOfCHsRatio));
			// NLP.pwNLP.append(//NLP.printLn("hasOddBall=", "" + hasOddBall));
			// NLP.pwNLP.append(//NLP.printLn("lineStr.indexOf 3 ws=", lineStr.indexOf(" ") +
			// ""));

			int cntM = 0, cntE = 0, cntY = 0;
			if (lineStr.indexOf("   ") > 0) {
				String tmpLineStr = "";
				tmpLineStr = lineStr.substring(Math.min(20, lineStr.length()));
				if (lineStr.indexOf("    ") > 0) {
					tmpLineStr = lineStr.substring(lineStr.indexOf("    "));
				}
				cntE = nlp.getAllIndexEndLocations(tmpLineStr, TableParser.EndedHeadingPattern).size();
				cntY = nlp.getAllIndexEndLocations(tmpLineStr, TableTextParser.patternMoDayYear).size();
				if (cntY < 1) {
					cntY = nlp.getAllIndexEndLocations(tmpLineStr, TableParser.YearOrMoDayYrPattern).size();
				}
				cntM = nlp.getAllIndexEndLocations(tmpLineStr, TableParser.MonthPatternSimple).size() + cntY;
				// NLP.pwNLP.append(
				//NLP.printLn("tmpLineStr=", tmpLineStr + " cntM=" + cntM + " cntY=" + cntY + "
				// cntE=" + cntE));
			}

			// <<==if beginning text until first 3ws has no PMY - then it okay
			// (prior conditions found 1 or more columsn separated by 2 ws and
			// PMY).

			if ((((lineStart < sIdxC1NumOfCsLastCeIdx.get(0)[0] - 25) && numberOfCHsRatio <= 3)
					|| ((lineStart < sIdxC1NumOfCsLastCeIdx.get(0)[0] - 18) && numberOfCHsRatio > 3)) && hasOddBall
					&& cntM < 2 && cntE < 2 && cntY < 2) {
				continue;
			}

			// System.xut.println("listCHLinesFromGetStartGroupCHLinesMethod.add(lineStr)="+lineStr);
			listCHLinesFromGetStartGroupCHLinesMethod.add(lineStr);

			cnt++;// allos up to 6 lines
			if (cnt >= 5 || totCnt > limitLinesToCntIncludingBlanks)
				break;
			totCnt++;
		}

		//NLP.printListOfString("@getStartGroupCHLines -- listCHLinesFromGetStartGroupCHLinesMethod",
//				listCHLinesFromGetStartGroupCHLinesMethod);

		return listCHLinesFromGetStartGroupCHLinesMethod;
	}

	public static List<String> getCHbyIdxLoc(List<String> chLineListFromGetCHLines)
			throws SQLException, IOException, ParseException {
		NLP nlp = new NLP();
		TableParser tp = new TableParser();

		// takes column heading lines and matches column headings across rows
		// based on idx location.

		// it calls compareIdxAndMergeTwoLists after establishing the first two
		// lists. The compare assumes the list is passed from bottom to top
		// (reverse order). See compareIdxAndMergeTwoLists for furthe logic.
		// Uses logic to exclude an attempted match if column heading is already
		// complete with edt/per

		List<String[]> mergeList = new ArrayList<String[]>();
		listCHbyIdxLoc = new ArrayList<String>();
		ListCHbyMidptLoc = new ArrayList<String>();
		tmp2ListCHbyIdxLoc = new ArrayList<String>();

		List<String[]> startingRowListOfAry = new ArrayList<String[]>();
		List<String[]> startingRowListOfAry2 = new ArrayList<String[]>();
		List<String[]> nextRowListOfAry = new ArrayList<String[]>();
		List<String[]> nextRowListOfAry2 = new ArrayList<String[]>();
		List<String[]> smallList = new ArrayList<String[]>();
		List<String[]> largeList = new ArrayList<String[]>();

		if (null == chLineListFromGetCHLines)
			return null;

		// this pattern can create issues: the best I can do w/o passing
		// allColumnHeadingPatterns is find instance of 1st 2 visible characters
		// (can't include a space) preceded by two w/s and followed by the same
		// or end of line.

		Pattern tmpCHptrn = Pattern.compile("(?<=[ \t]{2,})[\\p{Alnum}\\p{Punct}]{2,}.*?(?=[ \t]{2,}| ?$)");

		String line, nextLine;
		// in reverse order

		boolean reverse = false;

		if (chLineListFromGetCHLines == null || chLineListFromGetCHLines.size() == 0)
			return listCHbyIdxLoc;

		int firstColIdxMin = sIdxC1NumOfCsLastCeIdx.get(0)[0] - 6;

		if (rownameEIdx > 25 && (rownameEIdx - 8) < firstColIdxMin)
			firstColIdxMin = rownameEIdx - 8;

		// if tableName is on line- then anything before tableName on same line
		// is not a CH - so I cut all text after tablename (if for example a CH
		// follows tablename) and for lines stub prior to end of tablename I
		// replace with ws.
		Matcher matchTableName;
		List<String> tmpChLineListFromGetCHLines = new ArrayList<>();
		String row = "", addBlanks = "";
		int endIdx = 0;
		for (int i = 0; i < chLineListFromGetCHLines.size(); i++) {
			row = chLineListFromGetCHLines.get(i);

			matchTableName = TableParser.TableNamePattern.matcher(row);
			if (matchTableName.find()) {
				endIdx = matchTableName.end();
				row = row.substring(endIdx, row.length());
				for (int d = 0; d < endIdx; d++) {
					addBlanks = " " + addBlanks;
				}
				row = addBlanks + row;
			}

			// NLP.pwNLP.append(//NLP.printLn("chLineListFromGetCHLines row=", row));

			if (nlp.getAllIndexEndLocations(row, nlp.DecimalPattern).size() > 0
					&& nlp.getAllIndexEndLocations(row, tp.AllColumnHeadingPattern).size() < 1) {
				continue;
			}

			if (nlp.getAllIndexEndLocations(row, tmpCHptrn).size() > 0) {
				tmpChLineListFromGetCHLines.add(row);
			}
		}

		chLineListFromGetCHLines = tmpChLineListFromGetCHLines;

		// System.xut.println("A
		// chLineListFromGetCHLines.size()="+chLineListFromGetCHLines.size());
		if (chLineListFromGetCHLines.size() == 1) {

			listCHbyIdxLoc = nlp.getAllMatchedGroups(chLineListFromGetCHLines.get(0).substring(firstColIdxMin),
					tmpCHptrn);
			// System.xut.println("1 printing listCHbyIdxLoc");
			//NLP.printListOfString(listCHbyIdxLoc);

			return listCHbyIdxLoc;
		}

		if (chLineListFromGetCHLines.size() == 0) {
			// System.xut.println("2 printing listCHbyIdxLoc");
			//NLP.printListOfString(listCHbyIdxLoc);
			return listCHbyIdxLoc;

		}

		line = chLineListFromGetCHLines.get(0);

		// tmpPtrn is restrictive - so I ck if more patterns are in
		// AllColumnHeading if the resulting pattern in ...Ary2 is equal to # of
		// data cols and first ary did not have that many. Only run on 1st 2 CH
		// lines. Unlikely in line3 (3rd from bottom of CH).

		// used to see if ary2 meets condition of at least same # of cntM/E/Y as
		// data cols
		int cntE = 0, cntM = 0, cntY = 0, cntO = 0;
		nextLine = chLineListFromGetCHLines.get(1);
		cntM = nlp.getAllIndexStartLocations(line, TableTextParser.patternMoDayYear).size() + cntM;
		cntY = nlp.getAllIndexStartLocations(line, TableParser.YearOrMoDayYrPattern).size();
		cntE = nlp.getAllIndexStartLocations(line, TableParser.EndedHeadingPattern).size();

		// System.xut.println("@getCHbyIdxLoc - row1 -- sum of cntY+cntM+cntE==="
		// + (cntY + cntE + cntM));

		startingRowListOfAry = nlp.getAllMidPointLocsAndMatchedGroupAfterRownameEndIdx(line, tmpCHptrn, firstColIdxMin);
		// System.xut.println("startingRowListOfAry");
		//NLP.printListOfStringArray(startingRowListOfAry);
		startingRowListOfAry2 = nlp.getAllMidPointLocsAndMatchedGroupAfterRownameEndIdx(line,
				tp.AllColumnHeadingPattern, firstColIdxMin);

		// System.xut.println("startingRowListOfAry.size="
		// + startingRowListOfAry.size() + " startingRowListOfAry2.size="
		// + startingRowListOfAry2.size());

		int numbOfDataCols = sIdxC1NumOfCsLastCeIdx.get(0)[1];
		if (startingRowListOfAry2.size() == numbOfDataCols && startingRowListOfAry.size() != numbOfDataCols
				&& (cntE == numbOfDataCols || cntM == numbOfDataCols || cntY == numbOfDataCols)) {
			startingRowListOfAry = startingRowListOfAry2;
		}

		// System.xut.println("nextline1===" + nextLine + "\r line0="
		// + line+" firstColIdxMin="+firstColIdxMin);

		nextRowListOfAry = nlp.getAllMidPointLocsAndMatchedGroupAfterRownameEndIdx(nextLine, tmpCHptrn, firstColIdxMin);
		// System.xut.println("nextRowListOfAry");
		//NLP.printListOfStringArray(nextRowListOfAry);

		// System.xut.println("@getCHbyIdxLoc - row2 -- sum of cntY+cntM+cntE==="
		// + (cntY + cntE + cntM));
		// System.xut.println("@getCHbyIdxLoc - row2 -- line="+line+"\r cntM==="
		// + cntM+" cntY="+cntY);

		nextRowListOfAry2 = nlp.getAllMidPointLocsAndMatchedGroupAfterRownameEndIdx(nextLine,
				tp.AllColumnHeadingPattern, firstColIdxMin);

		// used to see if ary2 meets condition of at least same # of cntM/E/Y as
		// data cols
		cntM = 0;
		cntE = 0;
		cntY = 0;
		cntO = 0;
		// System.xut.println("nextLine="+nextLine);

		cntO = nlp.getAllIndexStartLocations(nextLine, TableParser.OddColumnPattern).size();

		cntM = nlp.getAllIndexStartLocations(nextLine, TableParser.MonthPattern).size();

		cntM = nlp.getAllIndexStartLocations(nextLine, TableTextParser.patternMoDayYear).size() + cntM;
		cntY = nlp.getAllIndexStartLocations(nextLine, TableParser.YearOrMoDayYrPattern).size();
		cntE = nlp.getAllIndexStartLocations(nextLine, TableParser.EndedHeadingPattern).size();

		// System.xut.println("nextRowListOfAry.size="+nextRowListOfAry.size()+
		// " nextRowListOfAry2.size="+nextRowListOfAry2.size());
		// System.xut.println("nextine="+nextLine+" cntO="+cntO+" cntE="+cntE+"
		// cntM="+cntM+" cntY="+cntY);

		// numbOfDataCols>3 -- unclear if filter is going to protect against
		// anything. Fear is too many oddballs and that causes over count
		if (nextRowListOfAry2.size() == numbOfDataCols && nextRowListOfAry.size() != numbOfDataCols
				&& ((cntE == numbOfDataCols || cntM == numbOfDataCols || cntY == numbOfDataCols)
						|| (cntO + cntE + cntY + cntM == numbOfDataCols && numbOfDataCols != 0
								&& (cntE + cntM + cntY) != 0 && numbOfDataCols % (cntE + cntM + cntY) == 1))) {
			nextRowListOfAry = nextRowListOfAry2;
		}

		if (startingRowListOfAry.size() < nextRowListOfAry.size()) {
			reverse = true;
			largeList = nextRowListOfAry;
			smallList = startingRowListOfAry;
		}

		else {
			reverse = false;
			largeList = startingRowListOfAry;
			smallList = nextRowListOfAry;
		}

		// if merge fails it seems to keep going by discarding failed merge
		// list.
		//NLP.printListOfStringArray("1 compareIdxAndMergeTwoLists -- largeList", largeList);
		//NLP.printListOfStringArray("1 compareIdxAndMergeTwoLists --  smallList=", smallList);
		listPrevLgEidx = new ArrayList<>();

		mergeList = compareIdxAndMergeTwoLists(largeList, smallList, mergeList, reverse, numberOfCHsRatio);

		//NLP.printListOfStringArray("1 compareIdxAndMergeTwoLists -- mergeList", mergeList);

		if (mergeList == null) {
			return listCHbyIdxLoc;
		}
		boolean nothingInSmallList = false;
		for (int i = 2; i < chLineListFromGetCHLines.size(); i++) {
			// at line 3 I only use tmpPtrn - AllColumnHeadingPattern may be
			// useful but less value at line3 and higher
			nextLine = chLineListFromGetCHLines.get(i);
			// System.xut.println("nextLine in chLinesFromGetCHLines=" +
			// nextLine);
			nextRowListOfAry = nlp.getAllMidPointLocsAndMatchedGroupAfterRownameEndIdx(nextLine, tmpCHptrn,
					firstColIdxMin);
			// System.xut.println("nextRowListOfAry");
			//NLP.printListOfStringArray(nextRowListOfAry);
			// System.xut.println("nextRowListOfAry.size="+nextRowListOfAry.size());

			if (mergeList.size() < nextRowListOfAry.size()) {
				reverse = true;
				largeList = nextRowListOfAry;
				smallList = mergeList;
			}

			else {
				reverse = false;
				largeList = mergeList;
				smallList = nextRowListOfAry;
			}

			// merge after rows 1 and 2.
			// System.xut.println("printing largeList to merge");
			//NLP.printListOfStringArray(largeList);
			// System.xut.println("printing smallList to merge");
			//NLP.printListOfStringArray(smallList);
			// System.xut.println("printing exisgting mergeList");
			//NLP.printListOfStringArray(mergeList);

			//NLP.printListOfStringArray("2 compareIdxAndMergeTwoLists -- largeList", largeList);
			//NLP.printListOfStringArray("2 compareIdxAndMergeTwoLists --  smallList=", smallList);

			// if no CHs in small list I run risk when I try to merge it of
			// corrupting merge so I skip it if it is after having already
			// merged 3 CHs rows (this loop and if filter is at 4th CH row
			// or later in sequence)
			nothingInSmallList = true;
			for (int a = 0; a < smallList.size(); a++) {
				if (nlp.getAllIndexEndLocations(smallList.get(a)[1], AllColumnHeadingPattern).size() > 0) {
					nothingInSmallList = false;
				}
			}
			if (i > 2 && nothingInSmallList) {
				nothingInSmallList = false;
				mergeList = largeList;
				continue;
			}

			mergeList = compareIdxAndMergeTwoLists(largeList, smallList, mergeList, reverse, numberOfCHsRatio);

			//NLP.printListOfStringArray("2 compareIdxAndMergeTwoLists -- mergeList", mergeList);

		}

		listPrevLgEidx = new ArrayList<>();

		if (mergeList == null)
			return null;

		for (int i = 0; i < mergeList.size(); i++) {
			// System.xut.println("mergeList col txt=="+mergeList.get(i)[1]);
			listCHbyIdxLoc.add(mergeList.get(i)[1]);
		}

		// if numberOfColumns=listCHbyIdxLoc
		if (numbOfDataCols == listCHbyIdxLoc.size()) {
			// System.xut.println("3 printing listCHbyIdxLoc");
			//NLP.printListOfString(listCHbyIdxLoc);
			// System.xut.println("||end");
			return listCHbyIdxLoc;
		} else
			return null;

	}

	public static List<String[]> compareIdxAndMergeTwoLists(List<String[]> largeList, List<String[]> smallList,
			List<String[]> mList, boolean reverse, int numberOfDataColumns) throws ParseException, IOException {
		NLP nlp = new NLP();

		/*
		 * Description of this complex method: compareIdxAndMergeTwoLists passes each
		 * row of CHs as a list - 2 at a time - with each item of list being string ary
		 * consisting of endpoint for each CH cell and related CH text. Larger CHs list
		 * is called largeList (typically CH text is years) and smaller is smallList.
		 * mList is also passed as is number of data cols. mList is prior merged list.
		 * Used to see if similar data already merged so as to avoid false positives
		 * 
		 * mergeCnt++ used as global variable - if mergeCnt=1 then its merging first 2
		 * rows if 2 then 2 and 3.
		 * 
		 * listCompletedCHs, listCompletedCHsPer, and listCompletedCHsEdt populated with
		 * CH that are found
		 * 
		 * I loop thru mList and add to listCompletedCHsPer and listCompletedCHsEdt. And
		 * listCompletedCHs is added to. cntCompleted++, cntCompletedPer++ and
		 * cntCompletedEdt++ counted
		 * 
		 * if cntCompleted largeList is returned (only relates to BS f/s).
		 * 
		 * if a remainder of large/small list - large list is returned (no merge
		 * attempted).
		 * 
		 * then main part of method works by looping thru smaller and larger BUT only
		 * pairing based on 1:1 ratio. This creates issue when you have 1:2 for rows 1
		 * and 2 and 1:1 for 2 and 3 for example. Where it is 2:1 nothing is returned.
		 * Simples approach is to ask at this juncture (before returning large list that
		 * doesn't merge) - see at mergeDidNotFail. Calculations are already all there.
		 * I can then also incorporate merging only where what I'm merging doesn't have
		 * info already found (cntCompleted edt/per etc).
		 */

		// System.xut.println("check 19");

		mergeCnt++;

		List<String[]> mergedList = new ArrayList<String[]>();
		@SuppressWarnings("unused")
		double cntL = largeList.size(), cntS = smallList.size();
		int cntCompleted = 0, cntCompletedPer = 0, cntCompletedEdt = 0;

		// edt/per completed (or edt completed if BS tble)
		List<Integer> listCompletedCHs = new ArrayList<Integer>();
		// per completed if not BS table (edt not).
		List<Integer> listCompletedCHsPer = new ArrayList<Integer>();
		// edt completed when not BS table (but per not)
		List<Integer> listCompletedCHsEdt = new ArrayList<Integer>();

		int cntE = 0, cntM = 0, cntY = 0;

		// if mo/yr/edt found - then in next loop - don't parse if colNo
		// in listColsWithAll matches =l

		// I don't exclude if there is a col on row completed - I skip that
		// column and then try to pair with remaining. If errrors I can skip
		// alltogether - or have it only pair directly over col mipdpoint - see
		// note below.

		// NLP.pwNLP.append(//NLP.printLn("mList.size=", mList.size() + ""));
		//NLP.printListOfStringArray("mList=", mList);

		for (int i = 0; i < mList.size(); i++) {
			cntE = 0;
			cntM = 0;
			cntY = 0;
			// record colNo - so I can skip them later
			// System.xut.println("cking col has edt=" + mList.get(i)[1]);
			if (nlp.getAllIndexStartLocations(mList.get(i)[1], TableParser.MonthPattern).size() > 0) {
				cntM = nlp.getAllIndexStartLocations(mList.get(i)[1], TableParser.MonthPattern).size() + cntM;
			}
			if (nlp.getAllIndexStartLocations(mList.get(i)[1], TableTextParser.patternMoDayYear).size() > 0) {
				cntM = nlp.getAllIndexStartLocations(mList.get(i)[1], TableTextParser.patternMoDayYear).size() + cntM;
			}

			if (nlp.getAllIndexStartLocations(mList.get(i)[1], TableParser.YearSimple).size() > 0) {
				cntY = nlp.getAllIndexStartLocations(mList.get(i)[1], TableParser.YearOrMoDayYrPattern).size() + cntY;
				if (cntY > 0 && cntM > 0 && !hasOddBall) {
					listCompletedCHsEdt.add(i);
					cntCompletedEdt++;
				}
			}

			if (nlp.getAllIndexStartLocations(mList.get(i)[1], TableParser.EndedHeadingPattern).size() > 0) {
				cntE = nlp.getAllIndexStartLocations(mList.get(i)[1], TableParser.EndedHeadingPattern).size() + cntE;
				if (cntE > 0 && !hasOddBall) {
					listCompletedCHsPer.add(i);
					cntCompletedPer++;
				}
				// NLP.pwNLP.append(//NLP.printLn("cntE=", cntE + ""));
			}

			// NLP.pwNLP.append(//NLP.printLn("cntM=",
			// cntM + " cntY=" + cntY + " cntE=" + cntE + " cntCompleted=" + cntCompleted +
			// " numberOfColumns="
			// + sIdxC1NumOfCsLastCeIdx.get(0)[1] + " cntS=" + cntS + " cntL=" + cntL + "
			// hasOddBall="
			// + hasOddBall));

			List<String> tmpEdtP = nlp.getEnddatePeriod(mList.get(i)[1]);
			if ((tableNameShort.toLowerCase().equals("bs") && cntM > 0 && cntY > 0 && !hasOddBall)
					|| (!tableNameShort.toLowerCase().equals("bs") && cntM > 0 && cntY > 0 && cntE > 0
							&& !hasOddBall)) {
				// NLP.pwNLP.append(//NLP.printLn("is this CH complete (has y/m/d)? - text is==",
				// mList.get(i)[1]
				// + " edt from this text is=" + tmpEdtP.get(0) + " per from this text is=" +
				// tmpEdtP.get(1)));
				if (tmpEdtP.size() > 0 && tmpEdtP.get(0).length() > 3 &&

				// remove any distinction for bs - when 'is' mislabeled as 'bs'
				// - can still pickup period

				// (tableNameShort.toLowerCase().equals("bs")) ||
				// (!tableNameShort.toLowerCase().equals("bs") &&

						!tmpEdtP.get(1).equals("0"
						// )
						)) {
					listCompletedCHs.add(i);
					cntCompleted++;
					// NLP.pwNLP.append(//NLP.printLn("completed column=", mList.get(i)[1] + "
					// column#=" + i));
				}

				// if hasOddBall col some cols can have 2 or more of same type -
				// that will then throw off pairing to wrong cols - so can't
				// mark complete. In theory I can capture the col that is
				// oddBall as well at listCHLines location.
			}
		}

		// NLP.pwNLP.append(//NLP.printLn("2 cntM=",
		// cntM + " cntY=" + cntY + " cntE=" + cntE + " cntCompleted=" + cntCompleted +
		// " numberOfColumns="
		// + sIdxC1NumOfCsLastCeIdx.get(0)[1] + " cntS=" + cntS + " cntL=" + cntL + "
		// hasOddBall="
		// + hasOddBall));

		// relates only to BS - this means edt is completed
		/*
		 * if (cntCompleted == sIdxC1NumOfCsLastCeIdx.get(0)[1]) {
		 * 
		 * //NLP.pwNLP .append(NLP .println(
		 * "return just largeList - cntCompleted == sIdxC1NumOfCsLastCeIdx.get(0)[1]" ,
		 * "cntCompleted=" + cntCompleted + " sIdxC1NumOfCsLastCeIdx.get(0)[1]=" +
		 * sIdxC1NumOfCsLastCeIdx.get(0)[1]));
		 * 
		 * return largeList; }
		 */
		int l = 0;
		int largeColNoStart = 0, largeColNoMiddle = 0, largeColNoEnd = 0;
		double lgMpColStart = 0, lgMpColEnd = 0, lgMpColSpan = 0, smMp = 0, mpDist = 0, lgEidx = 0, smEidx = 0;
		boolean cntRatio2to1 = false, cntRatio3to2 = false, cntRatio3to1 = false, mergeDidNotFail = false,
				mergedListAdded = false;
		double maxDist = 2.6;// sets max distance of MP of lg1/lg2 and smCh
		double cntRatio = cntL / cntS;

		if (cntS == 0 || (cntL - cntCompleted) % cntS == 1) {
			// remainder - so will return just largeList unless I merge using
			// specific methods
			if (cntRatio == 1.5) {
				// NLP.pwNLP.append(//NLP.printLn("remainder at merge of largeList and smallList.
				// cntRatio3to2=",
				// cntRatio3to2 + " cntRatio=" + cntRatio));

				mergedList = NLP.get3to2(largeList, smallList, reverse, mergedList, maxDist);
				//NLP.printListOfStringArray("3:2 mergedList", mergedList);

				if (mergedList != null)
					return mergedList;
			}
			if (mergeCnt > 1 && numberOfDataColumns == largeList.size() && mergedList == null) {
				runAll = false;
				return largeList;
			}
			if (mergedList == null && mergeCnt < 2) {
				runAll = true;
				return null;
			}
		}

		if (cntS == 0) {
			// NLP.pwNLP.append(//NLP.printLn("cntS==0 returning largeList only", ""));
			return largeList;
		}

		boolean chComplete = false;
		// boolean chCompletePer = false;
		// boolean chCompleteEdt = false;

		String mergCh = null, largeCHWordStart = null, largeCHWordMiddle = null, largeCHWordEnd = null,
				smallCHWord = null;
		// NLP.pwNLP.append(//NLP.printLn("A. cntRatio=", cntRatio + " cntL=" + cntL + "
		// cntS=" + cntS + " cntCompleted="
		// + cntCompleted + " cntCompletedEdt=" + cntCompletedEdt + " cntCompletedPer="
		// + cntCompletedPer));

		// no remainders ==>
		boolean gotIt = false;
		for (int s = 0; s < cntS; s++) {
			cntRatio2to1 = false;
			cntRatio3to2 = false;
			cntRatio3to1 = false;
			// mp dist has to be reaffirmed for each small word being paired.
			for (int x = 0; x < (int) (cntRatio); x++) {
				gotIt = false;
				// measure midpt large row CHs to be joined once

				// NLP.pwNLP.append(
				//NLP.printLn("x=", x + " l=" + l + " s=" + s + " cntS=" + cntS + " cntRatio="
				// + cntRatio));

				// this should always calc large ch start and end col correctly
				// that pairs with any small col hd!
				largeColNoEnd = (int) ((s + 1) * cntRatio - 1);
				largeColNoStart = (int) ((s + 1) * cntRatio - cntRatio);
				// System.xut.println("cntRatio="+cntRatio);

				if (x == 0) {
					// largeColNoStart = l;
					if ((int) cntRatio == 3) {
						largeColNoMiddle = largeColNoEnd - 1;
						// lgMpMiddle = Double.parseDouble(largeList
						// .get(largeColNoMiddle)[0]);
						largeCHWordMiddle = largeList.get(largeColNoMiddle)[1].trim();
					}
					lgMpColStart = Double.parseDouble(largeList.get(largeColNoStart)[0]);
					lgMpColEnd = Double.parseDouble(largeList.get(largeColNoEnd)[0]);
					lgMpColSpan = (lgMpColStart + lgMpColEnd) / 2;
					smMp = Double.parseDouble(smallList.get(s)[0]);
					mpDist = Math.abs(lgMpColSpan - smMp);
					largeCHWordStart = largeList.get(largeColNoStart)[1].trim();
					largeCHWordEnd = largeList.get(largeColNoEnd)[1].trim();
					smallCHWord = smallList.get(s)[1].trim();

					// used to compare end idx instead of mp - but only where
					// ratio lg/sm=1 (1:1) I have to use original large list
					// eIdx b/c a merged list doesn't track original end idx
					if (listPrevLgEidx.size() == largeList.size()) {
						lgEidx = listPrevLgEidx.get(l);
					} else {
						lgEidx = lgMpColStart + .5 * largeCHWordStart.length();
						listPrevLgEidx.add(lgEidx);
					}

					smEidx = smMp + .5 * smallCHWord.length();

					if (smallCHWord.length() > 14) {
						// if long smallCHWord then mpDist can be wider where
						// ratio
						maxDist = 3.6;
						if (largeList.size() == 4 && smallList.size() == 2) {
							maxDist = 4.6;
						}
					}

					if (cntRatio == 2) {
						cntRatio2to1 = true;
					}

					if (cntRatio == 1.5 && mpDist < maxDist) {
						cntRatio3to2 = true;
					}

					if (cntRatio == 3 && mpDist < maxDist) {
						cntRatio3to1 = true;
					}

					// NLP.pwNLP.append(//NLP.printLn("mpDist=",
					// mpDist + " smMp=" + smMp + " sm eIdx=" + smallList.get(s)[0] + " lgMp=" +
					// lgMpColSpan
					// + " lg eIdx start=" + largeList.get(largeColNoStart)[0] + " lg eIdx end="
					// + largeList.get(largeColNoEnd)[0] + "\r smWord=" + smallCHWord
					// + " largeCHWordStart=" + largeCHWordStart + " largeCHWordEnd=" +
					// largeCHWordEnd));
				}

				// System.xut.println("x!=0");

				chComplete = false;
				// chCompletePer = false; chCompleteEdt = false; finds if list
				// of completed cols(i) has match to current col(l) (where
				// edt/per complete)

				if (listCompletedCHs.size() > 0) {
					for (int c = 0; c < listCompletedCHs.size(); c++) {
						if (listCompletedCHs.get(c) == l) {
							chComplete = true;
							// NLP.pwNLP.append(//NLP.printLn("column is complete l=",
							// l + " listCompleteCh#=" + listCompletedCHs.get(c)));
						}
					}
				}

				int tmpLgMo = nlp.getAllIndexEndLocations(largeList.get(l)[1], TableParser.MonthPattern).size();
				int tmpLgYr = nlp.getAllIndexEndLocations(largeList.get(l)[1], TableParser.YearSimple).size();
				int tmpLgEnd = nlp.getAllIndexEndLocations(largeList.get(l)[1], TableParser.EndedHeadingPattern).size();
				int tmpSmMo = nlp.getAllIndexEndLocations(smallList.get(s)[1], TableParser.MonthPattern).size();
				int tmpSmYr = nlp.getAllIndexEndLocations(smallList.get(s)[1], TableParser.YearSimple).size();
				int tmpSmEnd = nlp.getAllIndexEndLocations(smallList.get(s)[1], TableParser.EndedHeadingPattern).size();

				gotIt = false;
				boolean endIdxMatch = false;
				// System.xut.println("chComplete="+chComplete);
				if (!chComplete) {
					// see also dist of lgMp v smMp as another way
					// to address
					// NLP.pwNLP.append(//NLP.printLn("smallList.get(s)[1]=", smallList.get(s)[1]));
					// NLP.pwNLP.append(//NLP.printLn("largeList.get(l)[1]=", largeList.get(l)[1]));
					// NLP.pwNLP.append(//NLP.printLn(" tmpLgMo=",
					// tmpLgMo + " tmpSmMo=" + tmpSmMo + " tmpLgYr=" + tmpLgYr + " tmpSmYr=" +
					// tmpSmYr));

					endIdxMatch = true;
					if (cntS == cntL && ((Math.abs(smEidx - lgEidx) < 1.1 && cntS < 3))
							|| ((Math.abs(smEidx - lgEidx) < 1.6 && cntS > 2))) {
						endIdxMatch = true;
						// if 1to1 ratio and endIdx are very close - sm and lg
						// ch will be paired - see net if then after below
					} else {
						endIdxMatch = false;
					}
					// NLP.pwNLP.append(//NLP.printLn(" lgMp1=", lgMpColStart + " smMp=" + smMp + "
					// smEidx=" + smEidx
					// + " lgEidx=" + lgEidx + " endIdxMatch=" + endIdxMatch));
					// NLP.pwNLP.append(//NLP.printLn(" tmpLgEnd=", tmpLgEnd + " tmpSmEnd=" + tmpSmEnd
					// + " reverse="
					// + reverse + " gotIt=" + gotIt + " cntS=" + cntS + " cntL=" + cntL));

					// if !gotIt && either 1. Yr, 2. mo or 2. end are in both
					// small and lg ch && mp dif >2/4
					if (!gotIt && !endIdxMatch
							&& ((tmpLgYr >= 1 && tmpSmYr >= 1) || (tmpLgMo >= 1 && tmpSmMo >= 1)
									|| (tmpLgEnd >= 1 && tmpSmEnd >= 1))
							&& ((Math.abs(lgMpColSpan - smMp) > 4.1 && cntS == cntL && cntL > 2)
									|| (Math.abs(lgMpColSpan - smMp) > 2 && (cntS != cntL || cntL < 3)))) {

						// NLP.pwNLP.append(//NLP.printLn("1 largeList only - mergCH",
						// mergCh + " mergeDidNotFail=" + mergeDidNotFail));
						gotIt = true;
					}

					// added && Math.abs(lgMp1 - smMp) <= 2
					if (!gotIt && ((Math.abs(lgMpColSpan - smMp) <= 2)
							|| (Math.abs(lgMpColSpan - smMp) <= 4.1 && cntS == cntL && cntL > 2) || (endIdxMatch))) {
						// System.xut.println("getting largeList index=" + l
						// + " smallList index=" + s);
						if (!reverse) {
							mergCh = smallList.get(s)[1] + " " + largeList.get(l)[1];
						}
						if (reverse) {
							mergCh = largeList.get(l)[1] + " " + smallList.get(s)[1];
						}
						mergeDidNotFail = true;
						gotIt = true;
						// NLP.pwNLP.append(
						//NLP.printLn("4 sm&lg list - mergCH", mergCh + " mergeDidNotFail=" +
						// mergeDidNotFail));
					}

					if (!gotIt && !cntRatio2to1 && !cntRatio3to1 && !cntRatio3to2 && !endIdxMatch) {
						// NLP.pwNLP.append(//NLP.printLn("6 mergCh",
						// mergCh + " mergeDidNotFail=" + mergeDidNotFail + " cntRatio=" + cntRatio));
						mergCh = largeList.get(l)[1];
					}
				}

				if (!chComplete && cntRatio2to1) {
					// NLP.pwNLP.append(//NLP.printLn("merge using 2to1 ratio", ""));
					if (x == 0) {
						mergedListAdded = true;
						// NLP.pwNLP.append(//NLP.printLn("x=0 2:1 ratio=", ""));
						if (mpDist < maxDist) {
							mergedList.add(NLP.getAry(reverse, Double.parseDouble(largeList.get(largeColNoStart)[0]),
									smallCHWord, largeCHWordStart));
						}

						else {
							mergedList.add(NLP.getAry(reverse, Double.parseDouble(largeList.get(largeColNoStart)[0]),
									"", largeCHWordStart));
						}

					}
					if (x == 1) {
						mergedListAdded = true;
						// NLP.pwNLP.append(//NLP.printLn("x=1 2:1 ratio=", ""));
						if (mpDist < maxDist) {
							mergedList.add(NLP.getAry(reverse, Double.parseDouble(largeList.get(largeColNoEnd)[0]),
									smallCHWord, largeCHWordEnd));
						} else {
							mergedList.add(NLP.getAry(reverse, Double.parseDouble(largeList.get(largeColNoEnd)[0]), "",
									largeCHWordEnd));
						}

					}
				}

				// full loop
				if (!chComplete && cntRatio3to1) {
					if (x == 0) {
						mergedList.add(NLP.getAry(reverse, Double.parseDouble(largeList.get(largeColNoStart)[0]),
								smallCHWord, largeCHWordStart));
						mergedListAdded = true;
					}
					if (x == 1) {
						mergedList.add(NLP.getAry(reverse, Double.parseDouble(largeList.get(largeColNoMiddle)[0]),
								smallCHWord, largeCHWordMiddle));
						mergedListAdded = true;
					}

					if (x == 2) {
						mergedList.add(NLP.getAry(reverse, Double.parseDouble(largeList.get(largeColNoEnd)[0]),
								smallCHWord, largeCHWordEnd));
						mergedListAdded = true;
					}
				}

				if (chComplete) {
					mergCh = largeList.get(l)[1];
					// NLP.pwNLP.append(//NLP.printLn("6 chComplete - largeList only - mergCH",
					// mergCh));
				}

				// if some of col hdgs are complete (chComplete) - then see if
				// either first or 2nd col of large list lines up. If not return
				// list as null.

				// System.xut.println("chComplete="+chComplete+
				// " mpDist=".+mpDist);

				// this will filter out potential errors.
				if (!chComplete && !endIdxMatch && Math.abs(lgMpColStart - smMp) > 1.5
						&& Math.abs(lgMpColEnd - smMp) > 1.5
						&& ((!chComplete && mpDist > 5) || (chComplete && Math.abs(lgMpColStart - smMp) > 2)
								|| (chComplete && Math.abs(lgMpColEnd - smMp) > 2))) {

					// NLP.pwNLP.append(//NLP.printLn("1 returning largeList b/c mp dist failed",
					// ""));
					// NLP.pwNLP.append(//NLP.printLn(" largeList.size", "" + largeList.size()));
					//NLP.printListOfStringArray("largeList", largeList);
					// NLP.pwNLP.append(//NLP.printLn("lgMp1=", "" + lgMpColStart));
					// NLP.pwNLP.append(//NLP.printLn("lgMp2=", "" + lgMpColEnd));
					// NLP.pwNLP.append(//NLP.printLn("smMp=", "" + smMp + "\rchComplete=" +
					// chComplete));
					return largeList;
				}

				// Adds to mergedList to return the endpoint of largeList
				// and mergCh unless special merge occured (eg cntRatio2to1).
				// this should work even if 1st 2 lg CHs merged w/ 1st smCH b/c
				// 2nd mpDist is too large - so just lg list is added for that
				// ratio grp

				if (!mergedListAdded) {
					if (mergedList == null) {
						mergedList = new ArrayList<String[]>();
					}

					if (mergCh != null && mergCh.length() > 0) {
						String[] ary = { largeList.get(l)[0] + "", mergCh };
						mergedList.add(ary);
						// NLP.pwNLP.append(//NLP.printLn("mergedList.add(ary) - ary==",
						// Arrays.toString(ary)));
					} else {
						String[] ary = { largeList.get(l)[0] + "", largeList.get(l)[1] };
						mergedList.add(ary);
						// NLP.pwNLP.append(//NLP.printLn("mergedList.add(ary) - ary==",
						// Arrays.toString(ary)));
					}
				}
				l++;
			}
		}

		return mergedList;
	}

	public String getColumnHeadingMap(String startGroup, int colStartIdx) throws IOException {
		NLP nlp = new NLP();

		int startGroupLenBeforeReplacements = startGroup.length();

		// these items are startGroup related - and are replace in entire text
		// unless fileSize is too large. If it was too large - they'll get
		// replaced here irregardless.
		double startTime = System.currentTimeMillis();
		// System.xut.println("startGroup text bef="+startGroup);
		startGroup = startGroup.replaceAll("(?i)[\r\n]{1}.{10,90}"
				+ "(?<!((BALANCE |CASH |STATEM|CON|JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC|MONTH|WEEK|YEAR|THREE|SIX|NINE|TWELVE"
				+ "|[12]{1}[09]{1}[0-9]{2}).{0,90}))\\(?UNAUDITED\\)?" + "[\\. \\-\\_\\=\t]{0,90}[\r\n]{1}", "")

				.replaceAll("(?i)(?<=[\r\n]{1}[ ]{0,65})assets(?=($|[ \r\n]{1,4}))", "      ")

				.replaceAll("(?i)(?<=([\r\n]{1}[ ]{0,65}))\\(unaudited\\)"
						+ "(?!([ ]{5,70}([12]{1}[09]{1}[0-9]{2}|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|\\(unaudited\\))))",
						"           ")

				.replaceAll("(?i)( FOR 3 )(months?)", " Three $2")

				.replaceAll("(?<=[ ]{10,50})\\(UNAUDITED\\)(?=([ ]{10,70}[12]{1}[09]{1}[0-9]{2}))", "           ")

				.replaceAll("(?<=[\r\n]{1}[ ]{10,50})UNAUDITED(?=([ ]{10,70}[12]{1}[09]{1}[0-9]{2}))", "         ")

				.replaceAll("(?i)(?<=[\r\n]{1}[ ]{5,50})\\(IN THOUSANDS EXCEPT SHARE AND PER SHARE DATA\\)    ",
						"                                                      ")

				.replaceAll("(?i)((?<=[\r\n]{1}[ ]{5,50})(NOTES   |ASSETS  |REVENUE |REVENUES|SALES   )"
						+ "(?=([ ]{5,70}([12]{1}[09]{1}[0-9]{2}|\\(UNAU|\\(RESTATED))))", "        ")
				.replaceAll("(?i)((?<=[A-Za-z]{3,11} [\\d]{1,2}), (?=[A-Za-z]{1}))", "  ")

				.replaceAll("(?i)(?<=(jan|feb| mar|apr| may |jun|jul|aug|sep|oct|nov|dec).{1,13})3L", "31")
				.replaceAll("(?i)SEPTEMBERT", "SEPTEMBER")
				// CH separated by single space - add additional space.
				.replaceAll(
						"(?i)(13|26|39|52|THREE|SIX|NINE|TWELVE)[- ]{1}(WEEK|WKS?|MONTHS?) (PERIOD|END[EDING]{2,3})? "
								+ "(13|26|39|52|THREE|SIX|NINE|TWELVE)[- ]{1}(WEEK|WKS?|MONTHS?) (PERIOD|END[EDING]{2,3})?",
						"$1 $2 $3  $4 $5 $6");
		// System.xut.println("startGroup text aft="+startGroup);

		double endTime = System.currentTimeMillis();
		double duration = (endTime - startTime) / 1000;
		// System.xut.println("startGroup replacement duration=" + duration);

		int startGroupLenAfterReplacements = startGroup.length();
		int addBackToStartGroup = startGroupLenBeforeReplacements - startGroupLenAfterReplacements;
		String addToStartOfStartGroup = "";
		for (int i = 0; i < addBackToStartGroup; i++) {
			if (i == 0)
				continue;
			addToStartOfStartGroup = addToStartOfStartGroup + " ";
			// System.xut.println("adding");
		}
		startGroup = addToStartOfStartGroup + "\r" + startGroup;

		if (addBackToStartGroup < 0) {
			// won't cut beginning off by more than 50.
			startGroup = startGroup.substring(Math.min(Math.abs(addBackToStartGroup), 50), startGroup.length());
		}

		// NLP.pwNLP.append(//NLP.printLn("how much shorter is startGroup=",
		// (startGroupLenBeforeReplacements - startGroupLenAfterReplacements) + ""));
		// NLP.pwNLP.append(//NLP.printLn("startGroup after replacements not done where
		// entire text is large. startGroup=",
		// startGroup + "||"));

		decimal = nlp.getDecimal(startGroup);
		startGroup = startGroup.replaceAll("\\(000s omitted\\)", "              ");
		startGroup = startGroup.replaceAll("   COMPANY   ", "             ");
		startGroup = startGroup.replaceAll("    COMPANY", "           ");
		startGroup = startGroup.replaceAll("(?i)fifty three", "Fifty-Three");
		startGroup = startGroup.replaceAll("(?i)( Sept\\.)([\\d]{1,2})", "  Sep $2");
		mapAllCH.clear();

		// listCHLines.clear();

		TableParser tp = new TableParser();

		numberOfCHsRatio = 0; // reset to 0 prior to fetching CHs.
		// System.xut.println("4. startGroup START|" + startGroup
		// + "||||END startGroup");
		// 1 asci tab=8 spaces
		int prevMatcherEnd = 0;
		Matcher matchBS = patternBS.matcher(startGroup);
		Matcher matchCF = patternCF.matcher(startGroup);
		// Matcher matchIS = patternIS.matcher(startGroup);
		if (matchBS.find() && matchCF.find()) {
			// System.xut.println("found both BS and CF");
			return null;
		}

		startGroup = nlp.startGroup(startGroup);
		// don't split by "\r\n" - it corrupts idx loc of txt
		String line = "", tableNameLine, tmpLin, tmpLin2, tmpLin3;
		int cnt = 0, cntNmbr, cntPeriod, endStartGrpRow = 0, prevLineNo = -1, lineNo = 0, tsL = -1,
				tableNameLineNo = -1;
		String[] splitLine;
		@SuppressWarnings("unused")
		double lineMp = 0, tableNmLineMp = 0, tableNmLineStart = 0, lineStart = 0, lineStart2 = 0, lineStart3 = 0,
				lineStart4 = 0, lineMp2 = 0, lineMp3 = 0;
		int cntE, cntM, cntY;
		boolean tableNameFound = false;
		String[] lineFieldCh;
		String tmpLine = "";
		// allow max of 11 lines in startGroup - if more it is f/p
		int fail = 5;
		coNameMatched = coNameMatched.replaceAll("[\\\\\\/\\$\\(\\)\\&\\[\\]\\{\\}\\(\\)\\*\\+]", "");
		Pattern coNamePattern = Pattern.compile(coNameMatched);
		// System.xut.println("coNameMatched=="+coNameMatched);
		// allow 4 instances of no CHs matches.
		String[] startGroupRows = startGroup.split("\r\n|\r|\n");
		int loopCnt = startGroupRows.length;

		Pattern patternNmbr = Pattern.compile(",[0-9]{3}\\)?[ ]{2,50}\\(? ? ? ? ?\\$? ? ? ? ?\\(? ? ? ? ?[0-9]{1}");
		Pattern patternNmbr2 = Pattern
				.compile("[ ]{6,}\\(?\\$? ?\\d\\d?\\d? ?\\)?[ ]{2,}\\(?\\$? ?([\\d]{1,3},\\d|\\d\\d?\\d?$)");
		Pattern patternNmbr3 = Pattern.compile("[ ]{12,}\\d\\d\\d(?<!\\d)");
		// Pattern patternNmbr4 =
		// Pattern.compile("[ ]{6,}\\(?\\d,?\\d,?\\d,?");
		// if line over rowname and no CH text (no month, year period) - replace
		// it with spaces. Cut is start of text on line is prior to 10 idx to
		// first double white space
		int sIdx = -1;
		String lineOverRowname = "";
		boolean chAfterTableSentence = false;
		String lineAfterTableSentence = "";
		int startIdxOfLineAfterTs = 0, loop = 0;
		for (int i = 0; i < startGroupRows.length; i++) {
			sIdx = -1;
			lineOverRowname = "";
			// NLP.pwNLP.append(//NLP.printLn("line in startGroup=", line));
			line = startGroupRows[i].replaceAll("[\r\n]", "");
			if (line.replaceAll("[ \t]+", "").length() < 1) {
				// NLP.pwNLP.append(//NLP.printLn("at startGroup - continue to next line", ""));
				continue;
			}

			if (nlp.getAllIndexStartLocations(line,
					Pattern.compile("(?i)</?(DOCUMENT|TEXT|DESCRIPTION|TYPE|CAPTION|SEQUENCE|TABLE)>")).size() > 0
					&& !tableNameFound && nlp.getAllIndexStartLocations(line, TableParser.TableNamePattern).size() < 1
					&& i < 12 && fail < 4) {
				// NLP.pwNLP.append(//NLP.printLn("at startGroup - reset fail to 4. line no (i)=",
				// i + " line is=" + line));
				fail = 4;
			}

			if (chAfterTableSentence && loop == 0) {
				loop++;
				// NLP.pwNLP.append(//NLP.printLn("chAfterTableSentence===", chAfterTableSentence
				// + ""));
				i = i - 1;
				line = lineAfterTableSentence;
				for (int c = 0; c < startIdxOfLineAfterTs; c++) {
					line = " " + line;
				}
			}

			// find first visible char
			if (nlp.getAllIndexStartLocations(line, nlp.patternAnyVisualCharacter).size() > 0) {
				sIdx = nlp.getAllIndexStartLocations(line, nlp.patternAnyVisualCharacter).get(0);
				// System.xut.println("sIdx="+sIdx+" line="+line);
			}

			// is 1st vis char over rowname?
			if (sIdx >= 0 && sIdx < 10 && line.length() > 10 && line.indexOf("  ") > 0
					&& line.length() > (sIdx + line.indexOf("  ")) && sIdx < line.indexOf("  ")
					&& line.substring(sIdx, line.indexOf("  ")).length() > 0) {
				lineOverRowname = line.substring(sIdx, line.indexOf("  "));
				// NLP.pwNLP.append(
				//NLP.printLn("1 lineOverRowname=", lineOverRowname + "\rfor lineOverRowname -
				// line=" + line));
			}
			// does heading over rowname have any CHs?
			// System.xut.println("8 line=="+line);

			// to get parser to work remove CHs above rowname and replace them
			// with white spaces -- but don't remove it if it is a tablename.

			if (nlp.getAllIndexEndLocations(lineOverRowname, tp.AllColumnHeadingPattern).size() == 0
					&& nlp.getAllIndexStartLocations(line, TableParser.TableNamePattern).size() < 1) {

				String tmpLineOverRowname = "";
				// if no CH above rowname - replace this text with white spaces
				// provided it is not a tablename
				for (int c = 0; c < lineOverRowname.length(); c++) {
					tmpLineOverRowname = tmpLineOverRowname + " ";
				}
				// then reconstitute the line with whitespaces and remainder of
				// line after rowname above text.
				line = tmpLineOverRowname + line.substring(tmpLineOverRowname.length());
				// System.xut.println("reconstituted lineOverRowname with blanks="+line);
			}

			line = line.replaceAll("\\([\\d\\a-zA-Z]{1}\\)", "   ").replaceAll("[\\.]{2,}", " ");
			line = line.replaceAll("<[SC]{1}>", "   ");
			// NLP.pwNLP.append(//NLP.printLn("line in start group to check for mapAllCH=",
			// line));
			if (line.replaceAll("<.*?[^>]>|[-=\\._]", "").trim().length() > 1) {
				lineNo++;
				// NLP.pwNLP.append(//NLP.printLn("startGrp line#=", lineNo + " line=" +
				// line.trim()));
			}

			Matcher matchNmb = patternNmbr.matcher(line.replaceAll("\\$|\\(|\\)", " "));
			Matcher matchNmb2 = patternNmbr2.matcher(line.replaceAll("\\$|\\(|\\)", " "));
			Matcher matchNmb3 = patternNmbr3.matcher(line.replaceAll("\\$|\\(|\\)", " "));

			// NLP.pwNLP.append(//NLP.printLn("line bef matchNmb. line=", line + "\rloopCnt="
			// + loopCnt + "| i=" + i));

			if (((matchNmb.find() && matchNmb.start() > 20) || (matchNmb2.find() && matchNmb2.start() > 20)
					|| (matchNmb3.find() && matchNmb3.start() > 35))

					&& i > 2) {

				// NLP.pwNLP.append(//NLP.printLn("number found - so ch fail occurred -- line=",
				// line));

				// if first or 2nd line is a fail (start of startGroup) - it is
				// end of prior table -- so don't fail it until past 1st line
				// attempted. Can also set startGroup not to occur prior to
				// endIdx of last table.

				fail--;
				fail--;
				fail--;
				// fail--;
				// why did I fail it 3 times previously?
				continue;
			}

			// if no coname,tablename,ch pattern - it is a failed line (ignores
			// blank and format lines) - and ignores line1.
			if (nlp.getAllIndexStartLocations(line, AllColumnHeadingPattern).size() < 1
					&& nlp.getAllIndexStartLocations(line, nlp.DecimalPattern).size() < 1
					&& nlp.getAllIndexStartLocations(line, TableParser.TableNamePattern).size() < 1
					&& nlp.getAllIndexStartLocations(line, coNamePattern).size() < 1
					&& !line.toLowerCase().contains("inception") && !line.toLowerCase().contains("development")
					&& !line.toLowerCase().contains("predecessor") && !line.toLowerCase().contains("successor")
					&& !line.toLowerCase().contains("table") && !line.toLowerCase().contains("caption")
					&& (line.replaceAll("(?i)weeks?   |end[eding]{2,3}   ", "").length() == line.length())
					&& line.replaceAll("\\_|[\\.-= \\s]|<.*[^>]>|<C$|<S$", "").length() > 1 && i > 0) {
				fail--;
				// NLP.pwNLP.append(//NLP.printLn("ch line failed=", line));
				// NLP.pwNLP.append(//NLP.printLn("fail=", "" + fail));
			}

			if (fail < 0) {
				break;
			}

			// for each match record match,lineNo,eIdx
			List<String[]> listChsMatchLineNo = new ArrayList<>();

			// NLP.pwNLP.append(//NLP.printLn("line.len=" + line.length(),
			// " line in startgroup to analyze for CH=" + line));
			cntE = nlp.getAllIndexStartLocations(line, TableParser.EndedHeadingPattern).size();
			cntM = nlp.getAllIndexStartLocations(line, TableParser.MonthPattern).size();
			cntY = nlp.getAllIndexStartLocations(line, tp.YearPatternOrMoDayYrSimplePattern).size();

			// NLP.pwNLP.append(
			//NLP.printLn("line=", line + "\rfound in line - cntE=" + cntE + " cntM=" +
			// cntM + " cntY=" + cntY));

			String[] rnAry = line.trim().split("  ");
			String lineToCntNmbr = "";
			if (rnAry.length > 1) {
				for (int a = 1; a < rnAry.length; a++) {
					lineToCntNmbr = lineToCntNmbr.trim() + "  " + rnAry[a];
				}
			} else {
				lineToCntNmbr = line;
			}

			// NLP.pwNLP.append(//NLP.printLn("lineToCntNmbr=", lineToCntNmbr));

			cntNmbr = nlp.getAllIndexStartLocations(lineToCntNmbr, patternSpaceNumberNoYearNoMo).size();
			cntPeriod = nlp.getAllIndexStartLocations(lineToCntNmbr, patternNoMonthOrWeek).size();

			// NLP.pwNLP.append(//NLP.printLn("cntPeriod=", cntPeriod
			// + "\nlineToCntPeroid=" + lineToCntNmbr));

			// NLP.pwNLP.append(//NLP.printLn("line.trim=", line.trim()));
			splitLine = line.replaceAll("[ \\s]{2,100}", "   ").replaceAll("[ \\s]$", "").trim().split("   ");
			// NLP.pwNLP.append(//NLP.printLn("splitLine.len=", splitLine.length
			// + " cntM=" + cntM + " cntY=" + cntY + " cntE=" + cntE
			// + " line=======" + line));
			// NLP.pwNLP.append(//NLP.printLn("splitLine=", Arrays.toString(splitLine)));
			if (decimal.equals("1")) {
				decimal = nlp.getDecimal(line);
			}

			// get center of text line that contains tableNameLong. Skips after
			// tableName found
			// NLP.pwNLP.append(//NLP.printLn(
			// "tableName found at this line if >0",
			// nlp.getAllIndexStartLocations(line,
			// TableParser.TableNamePattern).size()
			// + " line is"
			// + line
			// + " tablenamefound="
			// + tableNameFound));

			if (nlp.getAllIndexStartLocations(line, TableParser.TableNamePattern).size() > 0
					&& tableNameFound == false) {
				tableNameLineNo = lineNo; // can keep looking for tableSentences
											// for 2 more lines after
				tableNameFound = true;

				// NLP.pwNLP.append(//NLP.printLn("line=", line));
				// NLP.pwNLP.append(//NLP.printLn("TABLENAMELONG FOUND=", tableNameLong));
				tableNameLine = line.replaceAll("[ \\s]{2,20}.{1,20}[$\r\n]", "");
				List<Integer> tmpIdxList = nlp.getAllIndexStartLocations(tableNameLine, patternAlphabet);
				if (tmpIdxList.size() > 0) {
					// get start of tableLine
					tableNmLineStart = tmpIdxList.get(0);
					tableNmLineMp = (tableNmLineStart + tableNameLine.length()) / 2;
					// NLP.pwNLP.append(//NLP.printLn(
					//
					// "Alpha match of line with tableName idx loc=",
					// tmpIdxList.get(0) + " line.length="
					// + tableNameLine.length() + " tableLineMp="
					// + (tmpIdxList.get(0) + line.length()) / 2));
				}
			}

			// NLP.pwNLP.append(//NLP.printLn("cntNmbr=", cntNmbr + " tableNameFound=" +
			// tableNameFound + " line=" + line));
			// if it is a line with patternSpaceNumberNoYearNoMo - skip and go
			// to next

			// NLP.pwNLP.append(NLP
			// .println("each line in startGroup being checked=", line
			// + "\ntableNameFound=" + tableNameFound
			// + " cntNmbr=" + cntNmbr + " cntPeriod=" + cntPeriod));

			if (!tableNameFound || ((cntNmbr - cntPeriod) > 0) && (cntNmbr - cntM) > 0)
				continue;

			// get center/mp of current line's text.
			tmpLin = line.replaceAll("[ \\s]{2,20}.{1,20}[$\r\n]", "");
			List<Integer> tmpIdxList = nlp.getAllIndexStartLocations(tmpLin, patternAlphabet);

			if (tmpIdxList.size() > 0) {
				lineStart = tmpIdxList.get(0);
				lineMp = (lineStart + tmpLin.length()) / 2;
				// NLP.pwNLP.append(
				//NLP.printLn("lineStart=", lineStart + " tmpLin.len=" + tmpLin.length() + "
				// tmpLin=" + tmpLin));

				// get center of following lines to ensure above isn't a column
				// line
				if ((i + 1) < startGroupRows.length) {
					tmpLin2 = startGroupRows[i + 1].replaceAll("[\r\n]", "").replaceAll("\\([\\d\\a-zA-Z]{1}\\)", "   ")
							.replaceAll("[\\.]{2,}", " ").replaceAll("[ \\s]{2,20}.{1,20}[$\r\n]", "");
					List<Integer> tmp2IdxList = nlp.getAllIndexStartLocations(tmpLin2, patternAlphabet);
					if (tmp2IdxList.size() > 0) {
						lineStart2 = tmp2IdxList.get(0);
						lineMp2 = (lineStart2 + tmpLin2.length()) / 2;
					}

				}
				if ((i + 2) < startGroupRows.length) {
					tmpLin3 = startGroupRows[i + 2].replaceAll("[\r\n]", "").replaceAll("\\([\\d\\a-zA-Z]{1}\\)", "   ")
							.replaceAll("[\\.]{2,}", " ").replaceAll("[ \\s]{2,20}.{1,20}[$\r\n]", "");
					List<Integer> tmp3IdxList = nlp.getAllIndexStartLocations(tmpLin3, patternAlphabet);
					if (tmp3IdxList.size() > 0) {
						lineStart3 = tmp3IdxList.get(0);
						lineMp3 = (lineStart3 + tmpLin3.length()) / 2;
					}

				}

			}
			// above may only pair where it is a number - not when there is a
			// year - below will reset lineMp so that last lineMp isn't used
			if (tmpIdxList == null || tmpIdxList.size() == 0) {
				lineMp = 0;
				// lineStart = 0;
				// NLP.pwNLP.append(//NLP.printLn("lineMp and lineStart set to 0 ", ""));
			}

			/*
			 * is tableSentence if: (A)(1) cur line is centered below tablename's, (2) cur
			 * line length>20, (3) cur line has 2 or less instances double w/s OR (B)(1) cur
			 * line starts at idx<10 and (2) tableLine start at idx<10; and for (A) and (B)
			 * conditions (i) cur line occurs AFTER tablename found and (ii) there is a ch
			 * pattern match
			 */

			int cntTsM, cntTsY, cntTsE, tsCnt = 0;
			if (tableSentence != null) {

				cntTsM = nlp.getAllIndexStartLocations(tableSentence, TableParser.MonthPattern).size();
				cntTsM = nlp.getAllIndexStartLocations(tableSentence, TableTextParser.patternMoDayYear).size() + cntTsM;
				cntTsY = nlp.getAllIndexStartLocations(tableSentence, TableParser.YearOrMoDayYrPattern).size();
				cntTsE = nlp.getAllIndexStartLocations(tableSentence, TableParser.EndedHeadingPattern).size();
				tsCnt = cntTsM + cntTsY + cntTsE;

			}

			// NLP.pwNLP.append(//NLP.printLn("Alpha line: lineStart2=",
			// +lineStart2
			// + " lineMp=" + lineMp + " tableNameLineMp=" + tableNmLineMp
			// + " cntY=" + cntY + " cntM=" + cntM + " cntE=" + cntE
			// + " line.len=" + line.length() + " splitLine.len="
			// + splitLine.length + "\r line=" + line
			// + "\r tableNameLineNo=" + tableNameLineNo
			// + "lineNo - tableNameLineNo=" + (lineNo - tableNameLineNo)
			// + "\r tableSentence=" + tableSentence));

			boolean isTableSentence = NLP.isTableSentenceAtStartOfLine(line);
			// System.xut.println("isTableSentenceAtStartOfLine="+isTableSentence+"
			// line="+line);
			chAfterTableSentence = false;
			lineAfterTableSentence = "";
			startIdxOfLineAfterTs = 0;
			if (isTableSentence) {
				// NLP.pwNLP.append(//NLP.printLn("2 parseTableSentence=", line.substring(0,
				// line.indexOf(" "))));
				// NLP.pwNLP.append(//NLP.printLn("2b parseTableSentence=",
				// line.substring(line.indexOf(" "))));
				lineAfterTableSentence = line.substring(line.indexOf("   "));
				startIdxOfLineAfterTs = line.indexOf("   ");
				if (nlp.getAllIndexEndLocations(lineAfterTableSentence, TableParser.ColumnHeadingPattern).size() > 0) {
					chAfterTableSentence = true;
					// NLP.pwNLP.append(//NLP.printLn("chAfterTableSentence", chAfterTableSentence +
					// ""));
				}
				List<String> tmpLstr = NLP.parseTableSentence(line.substring(0, line.indexOf("   ")), companyName, true,
						tableNameLong, false);

				if ((tableNameLong.length() < 2 && tmpLstr.get(6).length() > 1)
						|| (tmpLstr.get(6).length() > 1 && tmpLstr.get(3).equals("true"))) {
					tableNameLong = tmpLstr.get(6).replaceAll("\\\\|\\)|\\(|\\$|'|\\*", "");
					tableNameShort = tp.getTableNameShort(tableNameLong);
					// NLP.pwNLP.append(
					//NLP.printLn("ac tablenamelong=", tableNameLong + " tableNameShort==" +
					// tableNameShort));
				}

				//NLP.printListOfString("aa parseTableSentence list=", tmpLstr);
				tableSentence = tmpLstr.get(0);
				// NLP.pwNLP.append(//NLP.printLn("at getColumnHeadingMap -- tableSentence=",
				// tableSentence));
				if (!tsShort.equals("zz")) {
					tsShort = tmpLstr.get(1);
				}
				tsPattern = tmpLstr.get(2).replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)", "");
			}

			// if no 3 ws and start of line plus line length is >50 - see if it
			// is a tableSentence - unlikely to be one if there's no case of 2
			// or more P,M or Y
			if (nlp.getAllIndexStartLocations(line, Pattern.compile("[ ]{10}[A-Za-z]")).size() > 0) {
				if (nlp.getAllIndexStartLocations(line, Pattern.compile("[ ]{10}[A-Za-z]")).get(0)
						+ line.replaceAll("  ", "").length() > 50) {
					int lineSt = nlp.getAllIndexStartLocations(line, Pattern.compile("(?<=[ ]{10})[A-Za-z]")).get(0);
					// NLP.pwNLP.append(//NLP.printLn(
					//
					// "what is lineStart=",
					// lineSt + "\rline re lineStart is" + line
					// + "\rline after lineSt="
					// + line.substring(lineSt)));
					// NLP.pwNLP.append(//NLP.printLn("no of ws==",
					// line.substring(lineSt).split("[ \\s]{1}").length
					// + ""));

					// len=1 means 0 splits of 3 ws
					if (line.substring(lineSt).split("[ \\s]{3}").length < 2) {
						List<String> tmpLstr = NLP.parseTableSentence(line, companyName, true, tableNameLong, false);
						//NLP.printListOfString("ab parseTableSentence list=", tmpLstr);
						tableSentence = tmpLstr.get(0);
						if ((tableNameLong.length() < 2 && tmpLstr.get(6).length() > 1)
								|| (tmpLstr.get(6).length() > 1 && tmpLstr.get(3).equals("true"))) {
							tableNameLong = tmpLstr.get(6).replaceAll("\\\\|\\)|\\(|\\$|'|\\*", "");
							tableNameShort = tp.getTableNameShort(tableNameLong);
							// NLP.pwNLP.append(//NLP.printLn("1ac tablenamelong=",
							// tableNameLong + " tableNameShort==" + tableNameShort));

						}

						// NLP.pwNLP.append(//NLP.printLn("tableSentence=", tableSentence));
						if (!tsShort.equals("zz")) {
							tsShort = tmpLstr.get(1);
						}
						tsPattern = tmpLstr.get(2).replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)", "");
						if (nlp.getAllIndexStartLocations(tsPattern, Pattern.compile("P")).size() < 2
								&& nlp.getAllIndexStartLocations(tsPattern, Pattern.compile("M")).size() < 2
								&& nlp.getAllIndexStartLocations(tsPattern, Pattern.compile("Y")).size() < 2) {
							isTableSentence = false;
						}
					}
				}
			}

			// NLP.pwNLP.append(//NLP.printLn("zd tsShort=", tsShort + " tableSentence=" +
			// tableSentence));

			// NLP.pwNLP.append(//NLP.printLn("lineNo - tableNameLineNo=",
			// (lineNo - tableNameLineNo) + " isTableSentence="
			// + isTableSentence));
			if (lineNo - tableNameLineNo < 3 && isTableSentence)
				continue;

			if (lineNo - tableNameLineNo < 3 && line.toLowerCase().contains(" and ") && splitLine.length < 2
					&& tsCnt > 0) {
				tsL = lineNo;
				// System.xut.println("check 1o");
				continue;
			}

			if (lineMp > 0 && tableNmLineMp > 0
					&& ((lineMp / tableNmLineMp > .93 && lineMp / tableNmLineMp < 1.07)
							|| (lineStart < 10 && tableNmLineStart < 10))
					&& splitLine.length < 2 && line.length() > 20) {
				// System.xut.println("check 1p");

				// System.xut.println("lineMp / tableNmLineMp=" + lineMp
				// / tableNmLineMp);
				// System.xut.println("line.length()=" + line.length() +
				// " line="
				// + line);

				// NLP.pwNLP.append(//NLP.printLn("is it ts==", line));
				// can't be a ts if line is more than 2 after tableNameLineNo
				if (null != tableSentence && line.replaceAll("[\\s ]", "").length() > 5
						&& (lineNo - tableNameLineNo) < 3 && (cntE > 0 || cntY > 0 || cntM > 0)) {
					// NLP.pwNLP.append(//NLP.printLn("added to ts=", line.trim()));
					// tableSentence = tableSentence + " " + line.trim();
					// NLP.pwNLP.append(//NLP.printLn("101 tableSentence", tableSentence));
					tsL = lineNo;
					// System.xut.println("check 1q");
				}

				if (null == tableSentence && line.replaceAll("[\\s ]", "").length() > 5
						&& (lineNo - tableNameLineNo) < 3 && (cntE > 0 || cntY > 0 || cntM > 0)) {
					// System.xut.println("2 added to ts=" + line.trim());
					tableSentence = line.trim();
					// NLP.pwNLP.append(//NLP.printLn("102A tableSentence", tableSentence));
					tsL = lineNo;
				}

				// System.xut.println("A. tableSentence=" + tableSentence);
				// System.xut.println("A. tableSentence.len="+tableSentence.length());

				// NLP.pwNLP.append(//NLP.printLn("continue -- TblNmMp=",
				// tableNmLineMp + "cntE=" + cntE + " cntM=" + cntM + " cntY=" + cntY + "
				// lineSt=" + lineStart
				// + " #of dbl ws=" + line.split(" ").length + " line.len=" + line.length() + "
				// tblLnMp="
				// + tableNmLineMp + " lnMp=" + lineMp + " tblNmLnSt=" + tableNmLineStart + "
				// lnSt="
				// + lineStart + " ln=" + line));

				continue;
			}

			// if prior TS line ends w/ " AND" and next line has a TS CH and
			// there isn't a single instance of 2 or more w/s tabs add it to ts.

			if (null != tableSentence && tableSentence.length() > 4
					&& tableSentence.substring(tableSentence.length() - 4).toLowerCase().equals(" and")
					&& lineNo - tsL == 1 && nlp.getAllIndexStartLocations(line, AllColumnHeadingPattern).size() > 0
					&& line.trim().split("  ").length == 1) {
				// tableSentence = tableSentence + " " + line.trim();
				//NLP.printLn("103 picked up stub and added to tableSentence=", tableSentence);
				continue;
			}
			if (line.toLowerCase().contains(" and ") && line.trim().split("  ").length < 2) {
				// NLP.pwNLP.append(//NLP.printLn("picking up previously parsed tableSentence
				// line=",
				// line + "\rtableSentence=" + tableSentence));
				// NLP.pwNLP.append(//NLP.printLn("line.trim.split( ).len=", line.trim().split("
				// ").length + ""));
				continue;
			}

			// first split location is typically rowname if it starts prior to
			// 25. So if all CH matches are in rowname area it can't be a CH.
			// splitLine is split by 3 or more spaces. I don't yet have
			// rownameEidx so can't use that to cut line.

			Matcher mLineStart = nlp.patternAnyVisualCharacter.matcher(line);
			if (mLineStart.find()) {
				lineStart4 = mLineStart.start();
			}

			// NLP.pwNLP.append(//NLP.printLn("lineStart4=" ,
			// lineStart4 +
			// " cutLine="
			// + splitLine[0] + "\rline===" +line);

			if (lineStart4 < 22 && (nlp.getAllIndexStartLocations(line, tp.AllColumnHeadingPattern).size() == nlp
					.getAllIndexStartLocations(splitLine[0], tp.AllColumnHeadingPattern).size())) {
				// NLP.pwNLP.append(
				//NLP.printLn("2A.. this condition is not met -- line=",
				// line.replaceAll("[\r\n]{1,}", "")));
				continue;
			}

			// If tableName not yet found no CHs or tablesentence should have
			// been found so continue. If number (eg, 233,568) found (cntNo)-
			// can't be CH row and I should continue (or break?).

			// to get correct idx loc first get match. Then w/ CH match (eg:
			// 1994) get its idx loc on line. Pattern to get eg:1994 uses spaces
			// that throws of idx loc

			lineFieldCh = line.replaceAll("\r", "").split("[\\s ]{2}");

			tmpLine = "";
			// was just ColumnHeadingPattern-->now has oddball and
			// restate/audited.
			// NLP.pwNLP.append(//NLP.printLn("A.. lineStart=", "" + lineStart));
			// NLP.pwNLP.append(//NLP.printLn("line=", line));
			List<Integer> listTmp2 = nlp.getAllIndexStartLocations(line, patternAlphabet);
			Pattern pSpaceYearSpaceYear = Pattern.compile("[ ]{3,}[1,2]{1}[09]{1}[0-9]{2}([ ]{3,})");
			List<Integer> listTmp3 = nlp.getAllIndexEndLocations(line, pSpaceYearSpaceYear);

			if (listTmp2.size() > 0 && (listTmp2.get(listTmp2.size() - 1) < 30 || (lineNo - tableNameLineNo) > 6)
					&& cntY < 2 && cntM < 2 && cntE < 2
					|| (listTmp2.size() > 0 && listTmp2.get(listTmp2.size() - 1) < 22)
							&& (listTmp3.size() > 0 && listTmp3.get(listTmp3.size() - 1) < 25)) {

				// NLP.pwNLP.append(//NLP.printLn("2B.. this condition is not met -- line=",
				// line));
				continue;
			}

			// if list size = 1 and match is at point that is less than 25 - it
			// is not a CH line NLP.print("start group line", line);
			List<Integer> tmpListInt = nlp.getAllIndexStartLocations(line, AllColumnHeadingPattern);
			if (tmpListInt.size() == 1 && lineStart < 25) {
				// NLP.pwNLP.append(//NLP.printLn(

				// "this condition is met: tmpListInt.size()==1 && tmpListInt.get(0)<25 - so
				// skipp adding to CH",
				// ""));
				continue;
			}

			// System.xut.println("line before matching AllColumnHedingPattern to lines of
			// limited StartGroup="+line);
			// System.xut.println("did I get line="+line);
			// System.xut.println("lineFieldCh ary="+Arrays.toString(lineFieldCh));
			// System.xut.println("line to ck AllColumnHeadingPattern="+line);
			// NLP.pwNLP
			// .append(//NLP.printLn("line to see if AllColumnHeadingPattern found to build
			// mapAllCH. line=", line));
			if (nlp.getAllMatchedGroups(line, AllColumnHeadingPattern).size() > 0) {
				// NLP.pwNLP.append(//NLP.printLn("if column match found - line====", line));

				// have captured all odd ball CHs by chg to oddball ch pattern.
				// Now see why getChbyIdxLoc isn't working
				for (int g = 0; g < lineFieldCh.length; g++) {
					if (lineFieldCh[g].replaceAll("[ ]{2,}", "").length() < 1)
						continue;
					// NLP.pwNLP.append(//NLP.printLn("1 lineFieldCh===", lineFieldCh[g]));
					// NLP.pwNLP.append(//NLP.printLn(

					// "# of AllColumnHeading matched groups in lineFieldCh",
					// nlp.getAllMatchedGroups(" " + lineFieldCh[g] + " ",
					// AllColumnHeadingPattern).size()
					// + ""));

					if (nlp.getAllMatchedGroups("  " + lineFieldCh[g] + "  ", AllColumnHeadingPattern).size() > 0
							|| nlp.getAllMatchedGroups("  " + lineFieldCh[g] + "  ", NLP.patternInception).size() > 0
							|| lineFieldCh[g].toUpperCase().equals("ENDED") || lineFieldCh[g].equals("MONTHS")
							|| lineFieldCh[g].contains("QUARTER")) {

						// NLP.pwNLP.append(//NLP.printLn("but did pickup AllColumnHeadingPattern found in
						// lineFieldCh[g]=",
						// lineFieldCh[g]));
						// will ck if 2 of same type
						// (eg: December 31,September 30) b/c < 2 w/s
						cntE = nlp.getAllIndexStartLocations(lineFieldCh[g], TableParser.EndedHeadingPattern).size();
						cntM = nlp.getAllIndexStartLocations(lineFieldCh[g], TableParser.MonthPattern).size();
						cntY = nlp.getAllIndexStartLocations(lineFieldCh[g], tp.YearPatternOrMoDayYrSimplePattern)
								.size();
						if (cntE > 1 || cntM > 1 || cntY > 1 || lineFieldCh[g].toUpperCase().equals("ENDED")
								|| lineFieldCh[g].equals("MONTHS")) {
							// NLP.pwNLP.append(//NLP.printLn(
							// "two+ matches in same field b/c no separated by 2 or more w/s lineFieldCh=",
							// lineFieldCh[g]));
							List<String> lineFieldChList = new ArrayList<String>();
							if (lineFieldCh[g].toUpperCase().equals("ENDED") || lineFieldCh[g].equals("MONTHS")) {
								lineFieldChList = nlp.getAllMatchedGroups("  " + lineFieldCh[g] + "  ",
										AllColumnHeadingPattern);
							} else {
								// NLP.pwNLP.append(//NLP.printLn("else lineFieldCh[g]=", lineFieldCh[g]));
								lineFieldChList = nlp.getAllMatchedGroups("  " + lineFieldCh[g] + "  ",
										AllColumnHeadingPattern);
							}

							// get all in same field matched added
							for (int f = 0; f < lineFieldChList.size(); f++) {
								tmpLine = "  " + lineFieldChList.get(f).trim();
								String[] ary = { tmpLine.replaceAll("[\\s ]{2,10}", " ").trim(), lineNo + "" };

								// NLP.pwNLP.append(//NLP.printLn("1 adding to listChsMatchLineNo=",
								// Arrays.toString(ary)));

								listChsMatchLineNo.add(ary);
							}
						} else {
							tmpLine = "  " + lineFieldCh[g].trim();

							String[] ary = { tmpLine.replaceAll("[\\s ]{2,10}", " ").trim(), lineNo + "" };
							// NLP.pwNLP.append(//NLP.printLn("2 adding to listChsMatchLineNo=",
							// Arrays.toString(ary)));

							listChsMatchLineNo.add(ary);

							// NLP.pwNLP.append(//NLP.printLn(
							// "ary added to listCHsMatchLineNo=",
							// Arrays.toString(ary) + "\n 2. FOUND CH at lineFieldCh=" + lineFieldCh[g] + "
							// cntE="
							// + cntE + " cntY=" + cntY + " cntM=" + cntM));
						}
					}
				}
			}

			String tmpCH;
			Map<Integer, String[]> mapRowEndIdxCH = new TreeMap<Integer, String[]>();

			// for each match - find its location and add to CHs list
			// add back replacements to eIdx location
			int addBack = 0;
			String lineReplaced, tmpCHBef = "";
			//NLP.printListOfStringArray("listChsMatchLineNo", listChsMatchLineNo);

			for (int d = 0; d < listChsMatchLineNo.size(); d++) {
				tmpCHBef = listChsMatchLineNo.get(d)[0];
				tmpCH = tmpCHBef.replaceAll("[\\(\\)\\*\\]\\[\\}\\{\\+,\\.\\$\\\\\\?]", "!");
				// didn't remove ',' b/c it throws off midPt. repl above must be
				// same as lineReplaced. Addback can be removed if I keep '!'.
				// If I repl w/ nothing above/below I lose pstns. By repl w/ ! I
				// hold the pstn.

				// NLP.pwNLP.append(//NLP.printLn("listChsMatchLineNo lineNo=",
				// listChsMatchLineNo.get(d)[1]
				// + " match bef replacement=" + tmpCHBef + " match after replacement=" +
				// tmpCH));
				addBack = tmpCHBef.length() - tmpCH.length();
				Pattern patternCh = Pattern.compile(tmpCH);
				lineReplaced = line.replaceAll("[\\(\\)\\*\\]\\[\\}\\{\\+,\\.\\$\\\\\\?]", "!");
				// Matcher matcher =
				// patternCh.matcher(lineReplaced.substring(cutLine));
				Matcher matcher = patternCh.matcher(lineReplaced);
				addBack = addBack + (line.length() - lineReplaced.length());
				// NLP.pwNLP.append(//NLP.printLn("listChsMatchLineNo lineNo=",
				// listChsMatchLineNo.get(d)[1]
				// + " match bef replacement=" + tmpCHBef + " match after replacement=" +
				// tmpCH));
				addBack = tmpCHBef.length() - tmpCH.length();
				// NLP.pwNLP.append(//NLP.printLn("line.len bef replace=", line.length() + " len
				// after replace="
				// + lineReplaced.length() + " lineReplaced is this=" + lineReplaced + "
				// addBack" + addBack));

				// used map b/c if ch row=1994 1993 1994 1993
				// it finds 1994 twice when it searches idx loc for col1 and
				// col3 (and records 4) but b/c map and key=idx then it will
				// only be recorded twice with key=idx loc

				String revisedGrp;
				boolean found = false;
				while (matcher.find()) {
					if (prevLineNo != lineNo)
						prevMatcherEnd = 0;
					// new line - so reset previous match end to 0
					if (matcher.end() > prevMatcherEnd) {
						// find only 1st instance of match - see boolean found
						// at bottom. (immediate outer Loop goes through each
						// row in order).

						revisedGrp = matcher.group();
						revisedGrp = nlp.getRevisedGroup(revisedGrp);

						// NLP.pwNLP.append(//NLP.printLn("matcher.group=",
						// matcher.group() + " matcher.end=" + matcher.end() + " prevMatcherEnd=" +
						// prevMatcherEnd
						// + " addBack=" + addBack + " lineNo=" + lineNo + " prevLineNo=" + prevLineNo
						// + " matching this=" + tmpCHBef));

						// if prev and cur eIdx are w/n 3 the program is
						// erroneously reparsing the same col twice (provided it
						// is on the same line/row)

						if (Math.abs(matcher.end() - prevMatcherEnd) < 3 && lineNo == prevLineNo) {
							prevLineNo = lineNo;
							continue;
						}

						// NLP.pwNLP.append(//NLP.printLn(

						// "added: matcher.end+addBack=",
						// ((matcher.end() + addBack) + " revisedGrp=" + revisedGrp + " lineNo=" +
						// lineNo
						// + " lineNo-tableNameLineNo=" + (lineNo - tableNameLineNo))));

						String[] ary = { revisedGrp, (lineNo - tableNameLineNo) + "" };
						// System.xut.println("adding ary (to go into mapAllCH)="
						// + revisedGrp);
						mapRowEndIdxCH.put(matcher.end() + addBack, ary);
						prevMatcherEnd = matcher.end();
						prevLineNo = lineNo;
						found = true;
						// stop while loop.
					}
					if (found)
						break;
				}
			}

			//NLP.printMapIntStringAry("AA. mapRowEndIdxCH", mapRowEndIdxCH);

			// create list of each CH that has endIdx and ColHdg
			List<String[]> listCHRowEndIdxCH = new ArrayList<>();
			int cntCh = 0;
			String key = "";
			Integer lNo = null, prevLno = null;
			for (Map.Entry<Integer, String[]> entry : mapRowEndIdxCH.entrySet()) {
				// String[0] (value[0]=columnHeading (e.g., Three Months),
				// value[1]=col hdg line number)

				key = entry.getKey().toString();
				lNo = Integer.parseInt(entry.getValue()[1]);
				String value = entry.getValue()[0];
				// NLP.pwNLP.append(//NLP.printLn("key=", (key + " value[0]=" + value + "
				// value.len=" + value.length()
				// + " value[1]=" + entry.getValue()[1] + " rownameEIdx=" + rownameEIdx)));
				String[] ary = { key, value, entry.getValue()[1] };

				// adjusted this down from 30 to 25 now to 23 - this will filter
				// out a potential CH if the start of the columnHeading is less
				// than 23.
				// System.xut.println("cntCh++ if key-val.len>=25.. entry.getKey="
				// + entry.getKey() + " value.len=" + value.length()
				// + " entry.getVal=" + Arrays.toString(entry.getValue()));
				// if (entry.getKey() - value.length() >= 22) {
				listCHRowEndIdxCH.add(ary);
				cntCh++;
				// NLP.pwNLP.append(//NLP.printLn("cntCh=", "" + cntCh));
				// }

				if (entry.getKey() - value.length() < 25) {

					cntTsM = nlp.getAllIndexStartLocations(value, TableParser.MonthPattern).size();
					cntTsM = nlp.getAllIndexStartLocations(value, TableTextParser.patternMoDayYear).size() + cntTsM;
					cntTsY = nlp.getAllIndexStartLocations(value, TableParser.YearOrMoDayYrPattern).size();
					cntTsE = nlp.getAllIndexStartLocations(value, TableParser.EndedHeadingPattern).size();
					tsCnt = cntTsM + cntTsY + cntTsE;

					if (tsCnt > 0) {

						// Get the entire line if it is a tablesentence. But
						// this gets tested for each CH match in a line - so if
						// previously grabbed the line as part of TS - skip 2nd
						// time

						if (prevLno == null || prevLno != lNo) {

							// if (null != tableSentence
							// && tableSentence.length() > 1)
							// tableSentence = tableSentence + " "+line.trim();
							// else
							// tableSentence = line;

							prevLno = lNo;
						}

					}

					// System.xut.println("at map -
					// tablesentence=="+tableSentence+"\nline==="+line);
					cntTsM = 0;
					cntTsY = 0;
					cntTsE = 0;
					tsCnt = 0;

				}
			}

			if (cntCh > numberOfCHsRatio) {
				// NLP.pwNLP.append(//NLP.printLn("cntCh>numberOfCHsRatio=", "" +
				// numberOfCHsRatio));
				numberOfCHsRatio = cntCh;
			}

			if (listCHRowEndIdxCH.size() > 0) {
				//NLP.printListOfStringArray("putting this in mapAllCH/mapAllCH2. listCHRowEndIdxCH=", listCHRowEndIdxCH);
				if (listCHRowEndIdxCH.get(0)[1].replaceAll("(?i) ?notes?\\(?([\\p{Alnum}\\p{Punct}]{1,2})?\\)?", "")
						.length() == 0) {
					noteInChEndIdx = Integer.parseInt(listCHRowEndIdxCH.get(0)[0]);
					// NLP.pwNLP.append(//NLP.printLn("found noteInChEndIdx=", noteInChEndIdx + ""));
				}

				// if(noteInChEndIdx<55)
				// continue;

				mapAllCH.put(cnt, listCHRowEndIdxCH);
				mapRowEndIdxCH.clear();
				cnt++;
				endStartGrpRow = i;
			}
			// System.xut.println("listCHRowEndIdxCH.size=" +
			// listCHRowEndIdxCH.size()
			// + " mapAllCH.size()=" + mapAllCH.size());
		}

		// mapAllCH2.clear();
		originalMapAllCH = new TreeMap<>();
		originalMapAllCH = mapAllCH;
		//NLP.printMapIntListOfStringAry("originalMapAllCH", originalMapAllCH);
		mapAllCH2 = mapAllCH;
		// NLP.pwNLP.append(//NLP.printLn("2 mapAllCH.size=", "" + mapAllCH.size()));

		// System.xut.println("numbOfChs=" + numberOfCHsRatio);
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i <= endStartGrpRow; i++) {
			if (i > 0) {
				sb.append("\r" + startGroupRows[i]);
			} else
				sb.append(startGroupRows[i]);
		}

		// NLP.pwNLP.append(//NLP.printLn("endStartGrpRow=",
		// endStartGrpRow + " number of CHs=" + numberOfCHsRatio + " mapData.size()=" +
		// mapData.size()));

		// System.xut.println("A. printing mapAllCH");
		//NLP.printMapIntListOfStringAry(mapAllCH);

		// NLP.pwNLP.append(//NLP.printLn("a mapAllCH2.siz", mapAllCH2.size() + ""));
		//NLP.printMapIntListOfStringAry("a mapAllCH2", mapAllCH2);

		String tmp = sb.toString();
		// System.xut.println("tmp before repl="+tmp);
		startGroupLenBeforeReplacements = tmp.length();
		// 09-30-96 09-30-95
		tmp = tmp.replaceAll("(\\d\\d?[-/]{1}\\d\\d?[-/]{1}[\\d]{2,4}) (\\d\\d?[-/]{1}\\d\\d?[-/]{1}[\\d]{2,4})",
				"$1  $2");
		// System.xut.println("tmp="+tmp);
		startGroupLenAfterReplacements = tmp.length();

		addBackToStartGroup = startGroupLenBeforeReplacements - startGroupLenAfterReplacements;
		// NLP.pwNLP.append(//NLP.printLn("addbaddBackToStartGroupack at end=",
		// addBackToStartGroup + ""));
		addToStartOfStartGroup = "";
		for (int i = 0; i < addBackToStartGroup; i++) {
			if (i == 0)
				continue;
			addToStartOfStartGroup = addToStartOfStartGroup + " ";
			// System.xut.println("adding");
		}

		if (addBackToStartGroup < 0) {
			// won't cut start by more than 50.
			tmp = tmp.substring(Math.min(Math.abs(addBackToStartGroup), 50), tmp.length());
		}

		tmp = addToStartOfStartGroup + "\r" + tmp;

		sb.delete(0, sb.toString().length());
		return tmp;
	}

	public List<String> mergeColumnHeadingLists() {
		NLP nlp = new NLP();

		// StringBuffer sb = new StringBuffer();

		mapOfCh.clear();
		// System.xut.println("printing mapAllCh2 @mergeColumnHeadingLists");
		//NLP.printMapIntListOfStringAry(mapAllCH2);

		// System.xut.println("AA mapAllCH2.size="+mapAllCH2.size());
		// System.xut.println("mergeColumnHeadingList - mapallCH2 printing");
		//NLP.printMapIntListOfStringAry(mapAllCH2);

		@SuppressWarnings("unused")
		int rowMod = 0;
		for (Map.Entry<Integer, List<String[]>> entry : mapAllCH2.entrySet()) {
			int row = entry.getKey();
			List<String[]> listData = entry.getValue();
			// System.xut.println("listData.Size=" + listData.size());
			List<String> tmpL = new ArrayList<>();
			String ch;
			@SuppressWarnings("unused")
			double endIdx, startIdx, midIdx;
			int eIdx = 0;
			for (int i = 0; i < listData.size(); i++) {
				ch = listData.get(i)[1];
				endIdx = Integer.parseInt(listData.get(i)[0]);
				eIdx = Integer.parseInt(listData.get(i)[0]);
				startIdx = endIdx - ch.length();
				midIdx = (endIdx + startIdx) / 2;
				// System.xut.println("midIdx double=" + midIdx + " startidx="
				// + startIdx + " endidx=" + endIdx);
				// System.xut.println(" ch===" + ch);
				tmpL.add(ch.trim());

			}
			rowMod = row * 1000 + eIdx;

			mapOfCh.put(row, tmpL);
		}

		// System.xut.println("mapofCh.size="+mapOfCh.size());

		// for (int i = 0; i < mapOfCh.size(); i++) {
		// System.xut.println("mapOfCh.get(i)==" + i + " " + mapOfCh.get(i));
		// }

		List<String> smallList = new ArrayList<>();
		List<String> largeList = new ArrayList<>();
		List<String> finalCHRaw = new ArrayList<>();

		if (null == mapOfCh || mapOfCh.size() < 1)
			return finalCHRaw;

		if (mapOfCh.size() == 1) {
			return mapOfCh.get(0);
		}

		boolean reverse = false;
		if (mapOfCh.get(0).size() >= mapOfCh.get(1).size()) {
			largeList = mapOfCh.get(0);
			smallList = mapOfCh.get(1);
		} else {
			largeList = mapOfCh.get(1);
			smallList = mapOfCh.get(0);
			reverse = true;
		}

		// int cntL = largeList.size(), cntS = smallList.size();
		// System.xut.println("cntL="+cntL+ " cntS="+cntS);

		// merges 1st 2 CHs
		List<String> mergedCHRawList = new ArrayList<>();
		mergedCHRawList = nlp.mergeTwoLists(largeList, smallList, reverse);
		// System.xut.println("mergedCHRawList.size()="+mergedCHRawList.size());
		reverse = false;

		//NLP.printMapIntListOfString("mapOfCh", mapOfCh);

		// if ch size is 3 or more loop through till end of ch rows.
		if (mapOfCh.size() < 3) {
			// System.xut.println("mergedCHRawList.size()="
			// + mergedCHRawList.size());
			return mergedCHRawList;
		}

		// for(int i = 0; i< mergedCHRawList.size(); i++){
		// System.xut.println("mergedCHRawList="+mergedCHRawList.get(i));
		// }

		for (int row = 2; row < mapOfCh.size(); row++) {
			// System.xut
			// .println("remainder of largeList/smallList==="
			// + ((mergedCHRawList.size() % mapOfCh.get(row)
			// .size()) + " largeList/smallList=" + (mergedCHRawList
			// .size() / mapOfCh.get(row).size()))
			// + " largeList size=" + mergedCHRawList.size()
			// + " smallList size=" + mapOfCh.get(row).size());
			int mergCnt = mergedCHRawList.size(), nxtRowCnt = mapOfCh.get(row).size();

			if (mergCnt >= nxtRowCnt && mergCnt != 0 && nxtRowCnt != 0
			/* && (mergCnt % nxtRowCnt) == 0 */) {
				mergedCHRawList = nlp.mergeTwoLists(mergedCHRawList, mapOfCh.get(row), false);
			} else if (mergCnt != 0 && nxtRowCnt != 0
			/* && (nxtRowCnt % mergCnt) == 0 */) {
				mergedCHRawList = nlp.mergeTwoLists(mapOfCh.get(row), mergedCHRawList, true);
			}
		}

		//NLP.printMapIntListOfString("mapOfCh", mapOfCh);

		// picks-up remaining columnHeadings rows at last point (row).
		return mergedCHRawList;
	}

	public String getTableText(String text, boolean loop2) throws IOException {
		NLP nlp = new NLP();

		// don't send entire text through - just a large portion after start of
		// tableText (after CHs startGroup). Greatly reduces speed. 25,000 is
		// far more than any tableText could be. It could be 100,000 and won't
		// affect speed, but when fileSize is 3 mb - I'm passing 3 mb every time
		// through the replacement process etc.

		text = text.substring(0, Math.min(25000, text.length()));

		int tbCnt = nlp.getAllIndexEndLocations(text, Pattern.compile("\t")).size();

		if (tbCnt > 4 && !loop2) {
			// System.xut.println("replacing tabs in tableText. tbCnt=" +
			// tbCnt);
			text = nlp.replaceTabsFromSecFiling(text);
			hasTabs = true;
		}

		// stop process if no tabs and is loop2 - b/c I already returned this
		// tableText in earlier callin of this method.
		if (tbCnt < 5 && loop2) {
			return null;
		}

		text = text.replaceAll("\\$?\\d?\\.\\d\\d?.{0,3}declared", "     declared");
		// $1.40 declared

		Pattern patternDashes = Pattern.compile("[-_=]{5,}");
		// replace number of dashes found with equal number of ws.
		List<Integer[]> listDashes = nlp.getAllStartAndEndIdxLocations(text, patternDashes);
		int sIdx = 0, eIdx = 0, priorEidx = 0;
		StringBuffer sbuf = new StringBuffer();
		// loop through text - stop at point where I need to replace - for first
		// iteration - cut text from 0 to first sIdx of replacement. Then append
		// to that cut blox the replacement then go to next iteration - and
		// append from last eIdx +1 to cur startIdx. Last loop will have stopped
		// at last eIdx - so outside loop append again from eIdx+1 to text
		// length
		for (int i = 0; i < listDashes.size(); i++) {
			sIdx = listDashes.get(i)[0];
			eIdx = listDashes.get(i)[1];
			// System.xut.println("found dashes here="
			// + text.substring(sIdx, eIdx));
			if (i == 0) {
				sbuf.append(text.substring(0, sIdx));
			}
			if (i > 0) {
				sbuf.append(text.substring(priorEidx + 1, sIdx));
			}
			// this is replacement with ws.
			sbuf.append(text.substring(sIdx, eIdx).replaceAll("-|_|=", " ")
			// .replaceAll("(?<=(\t| ))--?(?=(\t| |$))", "0")
			// .replaceAll("-", " ")
			);
			if (i == listDashes.size() - 1) {
				sbuf.append(text.substring(Math.min((eIdx + 1), text.length())));
				text = sbuf.toString();
			}
			priorEidx = eIdx;
		}

		// System.xut.println("startGroup at getTable text. startGroup="+startGroup);
		// System.xut.println("table text ==" +
		// text.substring(0,Math.min(text.length(),1000))+"|end tabletext snipped");
		// chgs: 114 181 116 038 306 694 156 075
		// to: 114,181 116,038 306,694 156,075

		// System.xut.println("replaced all tabs=="+text+"|||||end");
		text = text.replaceAll("(?<=[\\d]{1}) (?=[\\d]{3}[\\)\r\n ])", ",");
		text = text.replaceAll("_(?=.{0,10}[\\d]{1})", " ");
		// System.xut.println("a tableText=" + text);
		int endTableRow = -1, nmbIdx = 0;
		@SuppressWarnings("unused")
		boolean isTable = false, crapItsAnotherCHinTable = false, setAtThisRow = true;
		text = text.replaceAll("([\r\n]{1,}[ \t_=-]{1,150}[\r\n]{1,})", "\r\n");
		String[] tableTextSplit = text.split("\r\n");
		// if a 3 digit # (>99) is w/n 6 rows after end of startGroup (CHs) and
		// #'s idx loc>30 it "isTable" we break and parsing occurs in next loop.
		String line = null;
		int maxCount = 9, rowsToCount = 9, minIdx = 18;
		int i = 0;
		for (i = 0; i < Math.min(tableTextSplit.length, maxCount); i++) {
			line = tableTextSplit[i];
			// System.xut.println("tableTextSplit.length="+tableTextSplit.length+"
			// rowsTCount="+rowsToCount+" i="+i+" line="+line);
			rowsToCount--;
			if (line.replaceAll("<.*[^>]>", "").length() < 1) {
				rowsToCount++;
			}
			if (rowsToCount < 0) {
				// System.xut.println("rowsToCount=" + rowsToCount);
				break;
			}

			Matcher matcher = patternThreeDigitNumber.matcher(line.replaceAll("\\$[ -]{5}", "1,000"));
			// System.xut.println("before matcher rowsToCount=" + rowsToCount);
			while (matcher.find()) {
				nmbIdx = matcher.end();
				// System.xut.println("nmb found=="+matcher.group()+" nmbIdx="+nmbIdx+"
				// minIdx="+minIdx);
				if (nmbIdx > minIdx) {
					isTable = true;
					// NLP.pwNLP.append(//NLP.printLn("3 isTable=", isTable + " line=" + line));
					break;
				}
			}

			if (isTable) {
				// NLP.pwNLP.append(//NLP.printLn("loop2=", loop2 + " isTable=" + isTable + "
				// line=" + line));
				break;
			}
		}

		// tableNotfound
		if (!isTable) {
			// NLP.pwNLP.append(//NLP.printLn("1 !isTable=", isTable + ""));
			return null;
		}

		// NLP.pwNLP.append(//NLP.printLn("1 isTable=", isTable + "i=" + i + " line=" +
		// line));
		// 1st 10 rows of table can fail, afterwords - reset at 6 to fail
		int fail = 10, reset = 9, countDataCols = 0, bad = 0, yrEidx = 0;
		List<Integer> listIsCHyear = new ArrayList<>();
		// 36:Three months ended September 30,
		int cnt = 0;
		if (isTable) {
			for (i = 0; i < tableTextSplit.length; i++) {
				line = tableTextSplit[i];
				// System.xut.println("line=="+line);
				if ((i + 1) - cnt < 2) {
					// System.xut.println("is this rownameheader?==" + line
					// + " i=" + i + " cnt=" + cnt);
					// System.xut.println("tableSentence=" + tableSentence);
					if (null == list_TS_tsShort_tsPattern_tnValid || list_TS_tsShort_tsPattern_tnValid.size() < 1
							|| list_TS_tsShort_tsPattern_tnValid.get(0) == null
							|| list_TS_tsShort_tsPattern_tnValid.get(0).length() < 5)
						// System.xut.println("getting CH above rowname in first rows of table for
						// line="+line);
						// System.xut.println("list_TS_tsShort_tsPattern_tnValid before get
						// tableText=");
						//NLP.printListOfString(list_TS_tsShort_tsPattern_tnValid);
						list_TS_tsShort_tsPattern_tnValid = NLP.getColumnHeadingAboveRowname(line, "false",
								list_TS_tsShort_tsPattern_tnValid);
					//NLP.printListOfString("aa list from getColumnHeadingAboveRowname",
//							list_TS_tsShort_tsPattern_tnValid);
					// I require in getColumnHeadingAboveRowname method there be
					// a CH to follow CH above rowname

					if (list_TS_tsShort_tsPattern_tnValid.size() > 0
							&& list_TS_tsShort_tsPattern_tnValid.get(0).length() > 5) {
						if (!tsShort.equals("zz")) {
							tsShort = list_TS_tsShort_tsPattern_tnValid.get(1);
						}
						tableSentence = list_TS_tsShort_tsPattern_tnValid.get(0);
						tsPattern = list_TS_tsShort_tsPattern_tnValid.get(2)
								.replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)", "");
					}
				}

				if (line.replaceAll("[ ]{1,}", "").length() < 1
						|| line.replaceAll("  \\$?\\(?[\\d]{4}||[ -=]{1,}", "").length() < 2) {
					fail--;
					// NLP.pwNLP.append(//NLP.printLn(
					// "loop2=", loop2 + " failed line failCount==" + fail + " line in table
					// replaced="
					// + line.replaceAll(" \\$?\\(?[\\d]{4}||[ -=]", "")));
					cnt++;
					continue;
				}

				// when it is a data col - record the furthest data eIdx. Then
				// run allCol pattern match - and if last match is year and w/n
				// 5 - terminate table pairing and go back to last instance of
				// prior row where there was a col hdg match. Which generally
				// should have failed anyway.

				listIsCHyear = nlp.getAllIndexEndLocations(line, TableParser.YearOrMoDayYrPattern);
				if (listIsCHyear.size() < 2) {
					// System.xut.println("listIsCHyear.size<2. line="+line);
					listIsCHyear = nlp.getAllIndexEndLocations(line, patternMoDayYear);
				}

				/*
				 * if 2 or more years found on line (size>1) and furthest eIdx (size-1) is w/n 3
				 * of further eIdx of next 3 data lines - stop prior to CH (fail=-1 causes rest
				 * of tests to automatically fail b/c fail<0 and only last successful data line
				 * set table end idx - so this should work in setting correct table end. Also
				 * restriction of when "crapItsAnotherCHinTable" is set to true is tight -
				 * yrEidx less lastDataColEidx must be less than 5 and I ck rows ahead of yrEidx
				 * (relevant rows versus prior)
				 */

				if (listIsCHyear.size() > 1) {
					// NLP.pwNLP.append(//NLP.printLn("line with years as potential CH=", line));
					yrEidx = listIsCHyear.get(listIsCHyear.size() - 1);
					int yrEidxMin = 50;
					// cks next 4 lines
					// NLP.pwNLP.append(//NLP.printLn("yrEidx=", yrEidx + " yrEidxMin=" + yrEidxMin));
					if (yrEidx < yrEidxMin)
						continue;
					int dataColSize = 0, lastDataColEidx = 0;
					for (int b = Math.min(i + 1, tableTextSplit.length); b < Math.min(i + 7,
							tableTextSplit.length); b++) {
						if (tableTextSplit[b].length() < 2)
							continue;
						dataColSize = nlp.getAllIndexEndLocations(tableTextSplit[b], patternSpaceNumberNoYearNoMo)
								.size();
						// NLP.pwNLP.append(//NLP.printLn(
						// "b/c prior line found yr -- cking this data line=", tableTextSplit[b]));
						// NLP.pwNLP.append(//NLP.printLn("dataColSize", dataColSize + ""));
						if (dataColSize > 0 && lastDataColEidx < nlp
								.getAllIndexEndLocations(tableTextSplit[b], patternSpaceNumberNoYearNoMo)
								.get(dataColSize - 1)) {

							lastDataColEidx = nlp
									.getAllIndexEndLocations(tableTextSplit[b], patternSpaceNumberNoYearNoMo)
									.get(dataColSize - 1);
						}
					}

					// NLP.pwNLP.append(//NLP.printLn("yrEidx=", yrEidx + " lastDataColEidx=" +
					// lastDataColEidx + "\rline="
					// + line + " yrEidxMin=" + yrEidxMin));

					if (yrEidx >= yrEidxMin && Math.abs(lastDataColEidx - yrEidx) < 4) {
						crapItsAnotherCHinTable = true;
						fail = -1;
					}
					if (fail == -1)
						break;

					// NLP.pwNLP.append(//NLP.printLn("crapItsAnotherCHinTable inside another table=",
					// crapItsAnotherCHinTable + "\r ch line with years inside table=" + line));
				}

				if (fail == -1)
					break;

				countDataCols = nlp
						.getAllIndexEndLocations(line.replaceAll("[\\(\\$ ]{1}000[\\$'s\\) ]{1}|\\(?\\)?", ""),
								patternSpaceNumberNoYearNoMo)
						.size();

				// NLP.pwNLP.append(//NLP.printLn("loop2=" + loop2,
				// " line.replaceAll([\\(\\$ ]{1}000[\\$'s\\) ]{1}) ="
				// + line.replaceAll("[\\(\\$ ]{1}000[\\$'s\\) ]{1}", "") + "\r1 countDataCols="
				// + countDataCols + " fail=" + fail + " line="));
				// NLP.pwNLP.append(//NLP.printLn(
				// "nlp.getAllIndexEndLocations of
				// TableParser.StockHoldersEquityInitialCapsPattern).size()",
				// nlp.getAllIndexEndLocations(line,
				// TableParser.StockHoldersEquityInitialCapsPattern)
				// .size()
				// + ""));

				setAtThisRow = true;
				// The accompanying notes are an integral part of these
				// statements.
				Pattern patternEndOfTable = Pattern.compile("</?TABLE>");
				if (countDataCols == 0 && fail >= 0 && ((!tableNameShort.equals("bs")
						&& (nlp.getAllIndexEndLocations(line, TableParser.StockHoldersEquityAllCapsPattern).size() > 0
								|| nlp.getAllMatchedGroups(line, patternLiabilitiesAndStockholdersEq).size() > 0
								|| nlp.getAllIndexEndLocations(line, TableParser.StockHoldersEquityInitialCapsPattern)
										.size() > 0
								|| nlp.getAllMatchedGroups(line, patternLiabilitiesAndStockholdersEq).size() > 0
								|| nlp.getAllIndexEndLocations(line, Pattern.compile("Consolidated Statements? of "))
										.size() > 0
								|| nlp.getAllIndexEndLocations(line, Pattern.compile("CONSOLIDATED STATEMENTS? OF "))
										.size() > 0)))

						|| nlp.getAllMatchedGroups(line, patternEndOfTable).size() > 0) {

					// NLP.pwNLP.append(//NLP.printLn("this may be end of table. line==",
					// line + " tablenameShort=" + tableNameShort));

					if (fail > 4 /* && countDataCols >= 0 */) {
						// NLP.pwNLP.append(//NLP.printLn("set fail to 4. line=", line + " fail=" +
						// fail));
						fail = 4;
						setAtThisRow = false;
						// not a row to set a
					}
				}

				if (fail == -1)
					break;

				if (countDataCols == 0 && fail >= 0 && !tableNameShort.equals("validate") && tableNameShort.length() > 0
						&& (

						(nlp.getAllIndexEndLocations(line, TableParser.BalanceSheetInitialCapsPattern).size() > 0
								&& !tableNameShort.equals("bs"))

								|| (nlp.getAllIndexEndLocations(line, TableParser.BalanceSheetAllCapsPattern).size() > 0
										&& !tableNameShort.equals("bs"))

								||

								(nlp.getAllIndexEndLocations(line, TableParser.CashFlowAllCapsPattern).size() > 0
										&& !tableNameShort.equals("cf"))

								|| (nlp.getAllIndexEndLocations(line, TableParser.CashFlowInitialCapsPattern).size() > 0
										&& !tableNameShort.equals("cf"))

								|| (nlp.getAllIndexEndLocations(line, TableParser.IncomeAllCapsPattern).size() > 0
										&& !tableNameShort.equals("is"))

								|| (nlp.getAllIndexEndLocations(line, TableParser.IncomeInitialCapsPattern).size() > 0
										&& !tableNameShort.equals("is")))) {

					// NLP.pwNLP.append(//NLP.printLn("this may be end of table. line==",
					// line + " tablenameShort=" + tableNameShort));

					if (fail > 3 /* && countDataCols >= 0 */) {
						// NLP.pwNLP.append(//NLP.printLn("set fail to 4. line=", line + " fail=" +
						// fail));
						fail = 3;
						setAtThisRow = false;
						// not a row to set a
					}
				}

				// bad=# of f/p.
				bad = nlp.getAllStartIdxLocsAndMatchedGroups(line, patternWordOneSpaceNumber).size();

				// picks-up rare instance of 3 ws followed by 3 or less digits
				// followed three spaces followed by 3 or less digits

				if (countDataCols == 0 && fail >= 0) {
					countDataCols = nlp
							.getAllStartIdxLocsAndMatchedGroups(line, patternThreeSpaceSmallNumberThreeSpaceSmallNumber)
							.size();
				}

				// NLP.pwNLP.append(//NLP.printLn("2 countDataCols==", countDataCols + " bad=" +
				// bad + " fail=" + fail
				// + " setAtThisRow=" + setAtThisRow + " line=" + line));

				if (countDataCols > 0 && countDataCols != bad && fail >= 0 && setAtThisRow) {
					fail = reset;
					endTableRow = i;// records last good row (i) - last we rerun
									// loop this point.

					// NLP.pwNLP.append(//NLP.printLn("data col line=", line));
					// System.xut.println("data col line="+line);
					// NLP.pwNLP.append(//NLP.printLn("reset lines to check at=", "" + fail));
					// System.xut.println("reset lines to check at="+fail);
				}

				// ("(?s)[ ]{3}[\\d]{1,3}[ ]{3,}[\\d]{1,3}");

				if ((countDataCols < 1 && line.length() > 0 && !line.toUpperCase().contains("<C>")
						&& !line.toUpperCase().contains("<S>") && !line.toUpperCase().contains("<TABLE") && fail >= 0)
						|| (countDataCols == bad && nlp.getAllStartIdxLocsAndMatchedGroups(line,
								patternThreeSpaceSmallNumberThreeSpaceSmallNumber).size() < 1 && fail >= 0)) {
					fail--;
					// NLP.pwNLP.append(//NLP.printLn("line.len=",
					// line.length() + " fail declining by 1=" + fail + "|| line==" + line));
				}

				if (fail < 0) {
					break;
				}

				if (fail == -1)
					break;
			}
		}

		// NLP.pwNLP.append(//NLP.printLn("endTableRow===", endTableRow + ""));
		StringBuffer sb = new StringBuffer();
		for (i = 0; i <= endTableRow; i++) {
			line = tableTextSplit[i];
			// System.xut.println("rownameEidx="+rownameEIdx);
			List<Integer> tmpL = nlp.getAllIndexEndLocations(line, Pattern.compile("\\d !\\d"));
			// previously I replace $ with ! b/c to add ws then would corrupt
			// table
			if (tmpL.size() > 0 && tmpL.get(0) > 50) {
				line = line.replaceAll("!", " ");
			}
			List<String[]> listEidxColsForNoteInChEndIdxMatching = nlp.getAllEndIdxAndMatchedGroupLocs(
					line/* .replaceAll("[\\(\\)]{1,}", " ") */,
					Pattern.compile("[ ]{3,}\\(?[A-Za-z\\d\\*]{1,2}\\)?(?=[ ]{3})"));
			if (i > 0) {
				if (listEidxColsForNoteInChEndIdxMatching.size() > 0) {
					int colEidx = Integer.parseInt(listEidxColsForNoteInChEndIdxMatching.get(0)[0]);
					if (Math.abs(colEidx - noteInChEndIdx) < 3 && noteInChEndIdx < 55) {
						removeNote = true;
						int colEidxLength = listEidxColsForNoteInChEndIdxMatching.get(0)[0].trim().length();
						String str = "  ";// either 2 or 1 ws based on colE
						if (colEidxLength == 1) {
							str = " ";
						}
						line = line.substring(0, (colEidx) - (colEidxLength)) + str + line.substring(colEidx);
					}

					// NLP.pwNLP.append(//NLP.printLn(
					// "listEidxColsForNoteInChEndIdxMatching.get(0)="
					// + Arrays.toString(listEidxColsForNoteInChEndIdxMatching.get(0)),
					// " noteInChEndIdx=" + noteInChEndIdx + "\rline==" + line));
				}

				sb.append("\r" + line);

				// NLP.pwNLP.append(//NLP.printLn("get table till endTableRow --- each line=",
				// line));
			} else {
				sb.append(line);
			}
		}

		String tmp = sb.toString();

		sb.delete(0, sb.toString().length());
		return tmp;
	}

	public static String textReplacements(String text, Integer skip, int fileSize, boolean parseEntireFiling)
			throws IOException {

		NLP nlp = new NLP();

		double startTime = System.currentTimeMillis();
		double endTime = System.currentTimeMillis();
		double duration = 0;

		// startTime = System.currentTimeMillis();
		// this has to go first
		text = text.replaceAll(NLP.TRPattern.toString(), NLP.LineSeparator);
		// <=replace row - <tr|</tr, with hard return - \r\n.
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("1 duration=" + duration);
		// startTime = System.currentTimeMillis();

		// text = text.replaceAll(
		// "(?i)([ ]{9}|[\r\n]{2})[A-F]{1}-[0-9]{1,2} ?[\r\n]", "\r");

		// text = text
		// .replaceAll(
		// "(?ism)[\r\n]{1}
		// ?[\\\\\\/]{1}s[\\\\\\/]{1}.{1,150}[\r\n]{1}(JANUARY|FEBRUARY|MARCH|APRIL|MAY|JUNE|JULY|AUGUST|SEPTEMBER|OCTOBER"
		// +
		// "|NOVEMBER|DECEMBER) [0-9]{2}, [12]{1}[09]{1}[0-9]{2}",
		// "\r");

		// text = text
		// .replaceAll(
		// "(?ism)[\r\n]{1} ?[\\\\\\/]{1}s[\\\\\\/]{1}.{1,100}?[\r\n]",
		// "\r");

		text = text.replaceAll(
				"(?ism)(INCOME|OPERATIONS?|CASH FLOWS?|LOSS[ES\\)]{0,4}|EARNINGS?)( \\(Unaudited\\)) (For The)[\r\n]{1,}"
						+ "([ ]{0,10})(Three |Six |Nine |Twelve )",
				"$1$2\r$3$4$5");
		// System.xut.println("text="+text);
		text = text.replaceAll("\\&nbsp\r\n;|\\&nbsp;\r\n", " ");
		// theory is you don't have hard return directly after a
		// non-breaking hard space
		text = text.replaceAll("\\&nbsp;?|\\xA0", " ");
		text = text.replaceAll("\\&#151;|\\&mdash", "_");
		text = text.replaceAll("\\&#8211;", "-");
		text = text.replaceAll("\\&amp;", "\\&");
		text = text.replaceAll("Ã¢â‚¬â€�", "-");
		text = text.replaceAll("Ã¢â‚¬Å“|Ã¢â‚¬?", "\"");
		text = text.replaceAll("\\&lt;", "<");
		text = text.replaceAll("\\&gt;", ">");
		text = text.replaceAll("\\&#146;", "'");
		text = text.replaceAll("\\&#[\\d]*;", "");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("1a duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll(NLP.TDWithColspanPattern.toString(), "\t\t");
		text = text.replaceAll(NLP.TDPattern.toString(), "\t");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("1b duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("(?i)(<SUP[^>]*>[\\(0-9\\) ]*</SUP[^>]*>)", "");
		text = text.replaceAll("(?i)statements? of revenues?", "Statements of Income");
		text = text.replaceAll("(?i)Statements of Financial Position", "Balance Sheets");
		text = text.replaceAll("(?i)(?<=\\d{1}[ ]{2,10})\\(restated\\)(?=[ ]{1,10}\\d)", "          ");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("1c duration=" + duration);
		// startTime = System.currentTimeMillis();

		// all superscripts [i.e. (1) (2)...] with blank
		text = text.replaceAll("</U>|<U>|</u>|<U>", "");
		// above is removed b/c it interfers with below <BR>
		text = text.replaceAll("(?i)<BR> ?\r\n ?<BR>", "\r\n");
		text = text.replaceAll("(?i)<BR>", "\r\n");
		// System.xut.println("1d duration=" + duration);

		// if 2 consecutive BRs-likely meant hard return
		// if BR after a number - likely end of a row.

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		if (fileSize < 3 * skip || parseEntireFiling) {
			// startTime = System.currentTimeMillis();
			text = NLP.numberBR.matcher(text).replaceAll("\r\n");
			text = text.replaceAll(
					"([ \t]{3,30}[12]{1}[09]{1}[0-9]{2}[ ]{3,30})[\r\n]{1,2}([12]{1}[09]{1}[0-9]{2})[ ]{0,5}[\r\n]{1,2}",
					"$1 $2\r\n");
			// 1996
			// 1995
			// pickup here. -- ADD GROUPS and any other safeguard filters!

			// endTime = System.currentTimeMillis();
			// duration = (endTime - startTime) / 1000;
			// System.xut.println("1e duration=" + duration);
		}

		// startTime = System.currentTimeMillis();
		text = text.replaceAll("(?i)(<BR>\r\n|\r\n<BR>|\\[|\\])", " ");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("1f duration=" + duration);
		// startTime = System.currentTimeMillis();
		// text = text.replaceAll("(?i)<table[^>]*>", "");
		// text = text.replaceAll("(?i)</table[^>]*>", "");
		// <--<table used to accelerate end of table determination
		text = text.replaceAll("(?i)(</strong>|<strong>|</small>|<small>)", "");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("1g duration=" + duration);
		// startTime = System.currentTimeMillis();

		// This seems to help - patterns replace specific items b/w <>
		text = text.replaceAll("(?i)<BR>[ ]{1,10}", "<BR>");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("1h duration=" + duration);
		// startTime = System.currentTimeMillis();
		text = text.replaceAll(NLP.ExtraHtmlPattern1.toString(), "").replaceAll(NLP.ExtraHtmlPattern2.toString(), " ");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("1i duration=" + duration);
		// startTime = System.currentTimeMillis();
		text = text.replaceAll("[\r\n]{1}[ \\-=]{1,200}[\r\n]{1}", "").replaceAll("\\( ", "\\ (")
				.replaceAll("\\( ", "\\ (").replaceAll("\\( ", "\\ (").replaceAll("\\( ", "\\ (")
				.replaceAll("\\&nbsp;", " ").replaceAll("(?i)<br>", "    ").replaceAll("  \\|  ", "     ");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("1L duration=" + duration);
		// startTime = System.currentTimeMillis();

		// text = text.replaceAll("[ ]{3,}(\\(note\\))([ ]{3,}|\r|\n)", "");

		if (fileSize < skip || parseEntireFiling)
			text = text.replaceAll("(?ims)([\r\n]{1}[\t ]{0,100}[page]{0,4})([\\dixv\\-])({1,6}[\t ]{0,1}[\r\n]{1})",
					"$1 $2");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("1c duration=" + duration);
		// startTime = System.currentTimeMillis();

		if (fileSize * 2 < skip || parseEntireFiling)
			text = text.replaceAll("(?i)(?<=(jan|feb| mar|apr| may |jun|jul|aug|sep|oct|nov|dec).{1,13})3L", "31")
					.replaceAll("(?i)SEPTEMBERT", "SEPTEMBER");

		text = text.replaceAll("-\\$", " -");
		// text = text.replaceAll("\\$", " ");
		// <=creates a double whitespace - so can't use above. Below won't

		text = text.replaceAll(" \\$(?=(\\(?[\\d,]{4,15}\\)?(  |\t)))", "  ");
		text = text.replaceAll("  \\$", "   ");
		text = text.replaceAll("  \\(\\$", "   \\(");

		text = text.replaceAll("(?<=\t)\\$", " ");

		text = text.replaceAll("((?<=[a-zA-Z,-;]{1} )\\$)", "!");
		text = text.replaceAll("(Aug 31,) ?(200[23]{1})", "8/31/$2");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("2 duration=" + duration);
		// startTime = System.currentTimeMillis();

		// Net Sales 10,129,252 !10,757,255 !20,670,914 !21,417,848
		text = text.replaceAll("(?<=[A-Za-z\\d;,\\:] )\\!(?=\\(?[\\d]{1,3},[\\d]{3},[\\d]{3}([,\\d]{3})?([,\\d]{3})?)",
				" ");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("2aa duration=" + duration);
		// startTime = System.currentTimeMillis();

		// System.xut.println("before any text replacements -text aft="+text);
		if (fileSize < 3 * skip || parseEntireFiling)
			text = text.replaceAll("(?i)(?<=(three|six))[ ]{2,3}(?=months)", " ")
					.replaceAll("(?i)(?<=(months))[ ]{2,3}(?=end[eding]{2,3})", " ").replaceAll("<S>|<C>", "   ")
					.replaceAll("<PRE>", "    ").replaceAll(
							"(?i)(JANUARY|FEBRUARY|MARCH|APRIL|MAY|JUNE|JULY|AUGUST|SEPTEMBER|OCTOBER|NOVEMBER|DECEMBER"
									+ "|JAN|FEB|MAR|APR|JUN|JUL|AUG|SEP|OCT|DEC|JAN\\.|FEB\\.|MAR\\.|APR\\.|JUN\\.|JUL\\.|AUG\\."
									+ "|SEP\\.|OCT\\.|DEC)  (\\d\\d)",
							"$1 $2");
		// System.xut.println("text aft="+text);

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("3 duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("  Quarter  Ended", "  Quarter Ended")
				.replaceAll("(?i)(Sept\\.)([, ]{1,2}[\\d]{1,2})", "Sep\\.$2 ")
				.replaceAll("(?i)(Sept\\.)( ,?[\\d]{1,2})", "Sep\\.$2 ")
				.replaceAll("(?i)(   Sept\\.)([\\d]{1,2})", "   Sep\\. $2").replaceAll("(?i)Sept ", "Sep\\. ");
		// System.xut.println("text aft="+text);

		text = text.replaceAll("(SHAREHOLDERS\\')(EQUITY)", "$1 $2");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("4 duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll(
				"C[ ]{1,2}O[ ]{1,2}N[ ]{1,2}S[ ]{1,2}O[ ]{1,2}L[ ]{1,2}I[ ]{1,2}D[ ]{1,2}A[ ]{1,2}T[ ]{1,2}E[ ]{1,2}D"
						+ "[ \t\r\n]{2,5}B[ ]{1,2}A[ ]{1,2}L[ ]{1,2}A[ ]{1,2}N[ ]{1,2}C[ ]{1,2}E"
						+ "[ \t\r\n]{2,5}S[ ]{1,2}H[ ]{1,2}E[ ]{1,2}E[ ]{1,2}T([ ]{1,2}S)?",
				"CONSOLIDATED BALANCE SHEET");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("4a duration=" + duration);

		if (fileSize < skip * 5 || parseEntireFiling) {

			// startTime = System.currentTimeMillis();

			text = text.replaceAll(
					"(?sm)(C[ ]{1,2}O[ ]{1,2}N[ ]{1,2}S[ ]{1,2}O[ ]{1,2}L[ ]{1,2}I[ ]{1,2}D[ ]{1,2}A[ ]{1,2}T[ ]{1,2}E[ ]{1,2}D)?[ \t\r\n]{2,5}"
							+ "(S[ ]{1,2}T[ ]{1,2}A[ ]{1,2}T[ ]{1,2}E[ ]{1,2}M[ ]{1,2}E[ ]{1,2}N[ ]{1,2}T[ ]{1,2}S"
							+ "|S[ ]{1,2}T[ ]{1,2}A[ ]{1,2}T[ ]{1,2}E[ ]{1,2}M[ ]{1,2}E[ ]{1,2}N[ ]{1,2}T"
							+ "|R[ ]{1,2}E[ ]{1,2}S[ ]{1,2}U[ ]{1,2}L[ ]{1,2}T[ ]{1,2}S?)[ \t\r\n]{2,5}"
							+ "(O[ ]{1,2}?F)[ \t\r\n]{2,5}"
							+ "(E[ ]{1,2}A[ ]{1,2}R[ ]{1,2}N[ ]{1,2}I[ ]{1,2}N[ ]{1,2}G[ ]{1,2}S"
							+ "|O[ ]{1,2}P[ ]{1,2}E[ ]{1,2}R[ ]{1,2}A[ ]{1,2}T[ ]{1,2}I[ ]{1,2}O[ ]{1,2}N[ ]{1,2}S?"
							+ "|I[ ]{1,2}N[ ]{1,2}C[ ]{1,2}O[ ]{1,2}M[ ]{1,2}E)",
					"CONSOLIDATED STATEMENTS OF INCOME");
			// endTime = System.currentTimeMillis();
			// duration = (endTime - startTime) / 1000;
			// System.xut.println("4b duration=" + duration);
		}

		// startTime = System.currentTimeMillis();

		text = text.replaceAll(
				"(?ism)(C[ ]{1,2}O[ ]{1,2}N[ ]{1,2}S[ ]{1,2}O[ ]{1,2}L[ ]{1,2}I[ ]{1,2}D[ ]{1,2}A[ ]{1,2}T[ ]{1,2}E[ ]{1,2}D)?[\t\r\n ]{1,5}"
						+ "(S[ ]{1,2}T[ ]{1,2}A[ ]{1,2}T[ ]{1,2}E[ ]{1,2}M[ ]{1,2}E[ ]{1,2}N[ ]{1,2}T[ ]{1,2}S"
						+ "|S[ ]{1,2}T[ ]{1,2}A[ ]{1,2}T[ ]{1,2}E[ ]{1,2}M[ ]{1,2}E[ ]{1,2}N[ ]{1,2}T)[\t\r\n ]{1,5}"
						+ "(O[ ]{1,2}F)[\r\n\t ]{1,5}"
						+ "(C[ ]{1,2}A[ ]{1,2}S[ ]{1,2}H[\r\n\t ]{1,4}F[ ]{1,2}L[ ]{1,2}O[ ]{1,2}W[ ]{1,2}S?)",
				"CONSOLIDATED STATEMENTS OF CASH FLOWS");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("4c duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("(?sm)STATEMENTS?[\r\n ]{1,4}OF ", "STATEMENTS OF ");
		// 2004,2003
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("4d duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("(?sm)( [12]{1}[09]{1}[0-9]{2},)([\\dA-Za-z]{1})", "$1 $2");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("4e duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("BALANCE[\r\n ]{1,4}SHEETS?", "BALANCE SHEETS");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("4f duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("(?ism)(Results|Statements?)[\r\n ]{1,4}(of)[\r\n ]{1,4}"
				+ "(Financial Condition|Income|Operations?|Earnings?)", "$1 $2 $3");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("4g duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("(?ism)(Statements?)?([\r\n ]{1,4})?(of)?([\r\n ]{1,4})?(cash)[\r\n ]{2,4}(flows)",
				"$1 $3 $5 $6");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("4h duration=" + duration);
		// startTime = System.currentTimeMillis();

		// 4/30 == Apr 30
		text = text.replaceAll(" 1\\/(?=\\d\\d? )", " Jan ");
		text = text.replaceAll(" 2\\/(?=\\d\\d? )", " Feb ");
		text = text.replaceAll(" 3\\/(?=\\d\\d? )", " Mar ");
		text = text.replaceAll(" 4\\/(?=\\d\\d? )", " Apr ");
		text = text.replaceAll(" 5\\/(?=\\d\\d? )", " May ");
		text = text.replaceAll(" 6\\/(?=\\d\\d? )", " Jun ");
		text = text.replaceAll(" 7\\/(?=\\d\\d? )", " Jul ");
		text = text.replaceAll(" 8\\/(?=\\d\\d? )", " Aug ");
		text = text.replaceAll(" 9\\/(?=\\d\\d? )", " Sep ");
		text = text.replaceAll(" 10\\/(?=\\d\\d? )", " Oct ");
		text = text.replaceAll(" 11\\/(?=\\d\\d? )", " Nov ");
		text = text.replaceAll(" 12\\/(?=\\d\\d? )", " Dec ");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("4i duration=" + duration);
		// startTime = System.currentTimeMillis();

		// June 30 2000
		text = text.replaceAll(
				"(?i)(JANUARY|FEBRUARY|MARCH|APRIL|MAY|JUNE|JULY|AUGUST|SEPTEMBER|OCTOBER|NOVEMBER)[ ]{1,2}(\\d\\d)[ ]{2}([12]{1}[09]{1}[0-9]{2})",
				"$1 $2, $3");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("4j duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll(
				"(?ism)(FOR).{0,4}t?h?e?.{0,4}(THREE|SIX|NINE|TWELVE).{0,4}(MONTHS).{0,4}(END[EDING]{2,3}).{0,4}"
						+ "(JANUARY|FEBRUARY|MARCH|APRIL|MAY|JUNE|JULY|AUGUST|SEPTEMBER|OCTOBER|NOVEMBER)"
						+ ".{1,2}([\\d]{2}).{0,3}([12]{1}[09]{1}[0-9]{2}).{0,2}AND.{0,1}([12]{1}[09]{1}[0-9]{2})?",
				"$1 THE $2 $3 $4 $5 $6 $7 AND $8");

		// above removes double ws from
		// "FOR THE NINE MONTHS ENDED SEPTEMBER 30 2004 AND 2003".

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("5 duration=" + duration);
		// startTime = System.currentTimeMillis();
		text = text.replaceAll("(UNAUDITED  -  AMOUNTS  IN  THOUSANDS)(,  EXCEPT  PER  SHARE  DATA)?",
				"UNAUDITED - AMOUNTS IN THOUSANDS, EXCEPT PER SHARE DATA");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("6 duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("<CAPTION>", "         ").replaceAll("</U>|<U>|<B>", "").replaceAll("(?i) and  ",
				" and ");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("7 duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("\\.\\.", "  ").replaceAll(" \\. ", "   ").replaceAll("\\\\[0-9A-Za-z;,]{0,3}\\\\", "");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("8 duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("(?i)(jan|feb|mar|arp|jun|jul|aug|sep|oct|nov|dec|jan)(\\d)", "$1 $2");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("9 duration=" + duration);
		// startTime = System.currentTimeMillis();
		if (fileSize < 6 * skip || parseEntireFiling) {
			text = text.replaceAll(
					"(?i)(January|February|March|April|May|June|July|August|September|October|November|December)([0-9]{1})",
					"$1 $2");

			// 09-30-98 09-30-97 to add 2 ws between them

			text = text.replaceAll("(  )(\\d\\d[/-]{1}\\d\\d[/-]{1}\\d\\d) (\\d\\d[/-]{1}\\d\\d[/-]{1}\\d\\d)",
					"$1$2  $3");
			endTime = System.currentTimeMillis();
			duration = (endTime - startTime) / 1000;
			// System.xut.println("10 duration=" + duration);
		}

		// startTime = System.currentTimeMillis();

		text = text.replaceAll("((?<= )[\\d]{1,2}) (MARCH|MAR\\.)", "$2 $1");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("11 duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("([\\d]{1,2}) (MAR(?=( |\t|\r)))", "$2\\. $1");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("12 duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("\\(\\*\\)", "   ").replaceAll("\\(\\*\\*\\)", "    ").replaceAll("\\*", " ");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("13 duration=" + duration);

		if (fileSize < 5 * skip || parseEntireFiling) {

			// startTime = System.currentTimeMillis();
			text = text.replaceAll("(?i)   (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\. ?", "    $1 ");

			text = text.replaceAll("(?i)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\. ?", "$1 ");

			// endTime = System.currentTimeMillis();
			// duration = (endTime - startTime) / 1000;
			// System.xut.println("14 duration=" + duration);
		}

		// startTime = System.currentTimeMillis();

		text = text.replaceAll("(?i)([\\d]{2})-(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)-([\\d]{2})",
				"$2 $1 $3");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("15 duration=" + duration);
		// startTime = System.currentTimeMillis();

		if (fileSize < skip || parseEntireFiling)
			text = text.replaceAll("(?i)((?<=[A-Za-z]{3,11} [\\d]{1,2}), (?=[A-Za-z]{1}))", "  ");
		// (replaces ', ' with 2 ws ' ' - which allows for CH recognition
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("16 duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("(?<=([12]{1}[09]{1}[0-9]{2})) (?=([JFMSOND]{1}|APR|AUG))", "  ");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("17 duration=" + duration);
		// startTime = System.currentTimeMillis();

		if (fileSize < skip || parseEntireFiling)
			text = text.replaceAll("(?i)((?<=[\r\n]{1}[ ]{5,50})(NOTES   |ASSETS  |REVENUE |REVENUES|SALES   )"
					+ "(?=([ ]{5,70}([12]{1}[09]{1}[0-9]{2}|\\(UNAU|\\(RESTATED))))", "        ");

		// can start anywhere but must have two columns follow
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("18 duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll(
				"(NOTES   |ASSETS  |REVENUE |REVENUES|SALES   )((?i)(?=[\t ]{10,70}(\\(?UNAUDITED\\)?|\\(?AUDITED\\)?|\\(?RESTATED\\)?"
						+ "|[12]{1}[09]{1}[0-9]{2})[ \t]{3,20}(\\(?UNAUDITED\\)?|\\(?AUDITED\\)?|\\(?RESTATED\\)?|[12]{1}[09]{1}[0-9]{2})))",
				"        ");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("19 duration=" + duration);
		// startTime = System.currentTimeMillis();

		if (fileSize < skip || parseEntireFiling)
			text = text.replaceAll("(?i)(?<=[\r\n]{1}[ ]{5,50})\\(IN THOUSANDS EXCEPT SHARE AND PER SHARE DATA\\)    ",
					"                                                      ");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("20 duration=" + duration);
		// startTime = System.currentTimeMillis();

		// put \\( and \\) back
		if (fileSize < skip || parseEntireFiling)
			text = text.replaceAll("(?<=[\r\n]{1}[ ]{10,50})UNAUDITED(?=([ ]{10,70}[12]{1}[09]{1}[0-9]{2}))",
					"         ");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("21 duration=" + duration);
		// startTime = System.currentTimeMillis();

		if (fileSize * 2 < skip || parseEntireFiling)
			text = text.replaceAll("(?<=[ ]{10,50})\\(UNAUDITED\\)(?=([ ]{10,70}[12]{1}[09]{1}[0-9]{2}))",
					"           ");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("22 duration=" + duration);
		// startTime = System.currentTimeMillis();
		// System.xut.println("6 text aft="+text);

		text = text.replaceAll(
				"(?i)([\r\n]{1,2}[ ]{5,100})(\\(Unaudited\\)|\\(consolidated\\)|subsidiar[yies]{1,3})[ ]{2,12}"
						+ "((\\(Unaudited\\)|\\(consolidated\\)|subsidiar[yies]{1,3})[ ]{2,12})?"
						+ "((\\(Unaudited\\)|\\(consolidated\\)|subsidiar[yies]{1,3})[ ]{2,12})?"
						+ "((\\(Unaudited\\)|\\(consolidated\\)|subsidiar[yies]{1,3}).{0,5})(?=[\r\n])",
				"");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("23 duration=" + duration);
		// startTime = System.currentTimeMillis();
		// at mo|w and next - adjusted by 1
		if (fileSize < skip || parseEntireFiling)
			text = text.replaceAll("(?i)( FOR 3 )(months?)", " Three $2");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("23a duration=" + duration);
		// startTime = System.currentTimeMillis();

		// text = text.replaceAll("(?i) 3 months",
		// " Three Mo");
		// text = text.replaceAll("(?i) 6 months",
		// " Six Mos");
		// text = text.replaceAll("(?i) 9 months",
		// " Nine Mo\\.");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("23b duration=" + duration);
		// startTime = System.currentTimeMillis();
		if (fileSize < 3 * skip || parseEntireFiling) {
			text = text
					// .replaceAll("(?i) 3 months", " Three Mo")
					// .replaceAll("(?i) 6 months", " Six Mos")
					// .replaceAll("(?i) 9 months", " Nine Mos")
					.replaceAll("(?i)(?<=(three|six|nine|twelve|thirteen|twelve|fourteen))  (?=(mo|w))", " ")
					.replaceAll("(?i)(THESIXM)", "THE SIX M")
					.replaceAll(
							"(?i)(?<=(FOR THE (THREE|SIX|NINE|TWELVE) MONTHS)) (?=(FOR THE (THREE|SIX|NINE|TWELVE) MONTHS))",
							"  ")
					// add two ws where this is 1.
					.replaceAll("(Three|Six|Nine|Twelve) (Months Ended) (Three|Six|Nine|Twelve) (Months Ended)",
							"$1 Months End   $3 Months End");

			text = text.replaceAll("(?i)    (three|six|nine|twelve)(?=(   | ?$|\r|\n))", "  $1 Mo");
			// endTime = System.currentTimeMillis();
			// duration = (endTime - startTime) / 1000;
			// System.xut.println("24 duration=" + duration);
		}
		// System.xut.println("7 text aft="+text);

		// startTime = System.currentTimeMillis();

		text = text.replaceAll("(?i)Sep\\.30", "Sep. 30");
		// System.xut.println("text aft="+text);

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("25 duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll(
				"(?ism)(The )?(accompanying)([ ]{1,2})?(notes)?([ ]{1,2})?(are an)?([ ]{1,2})?(integral([ ]{1,2})?part([ ]{1,2})?of)?"
						+ "([ ]{1,2})?(the|these|this)?"
						+ "([ ]{1,2})?(u?n?audited)?([ ]{1,2})?(condensed)?([ ]{1,2})?(consolidated)?([ ]{1,2})?(financial|balance([ ]{1,2})"
						+ "?sheets?)([ ]{1,2})?statement",
				"</TABLE>");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("25a duration=" + duration);
		// startTime = System.currentTimeMillis();
		text = text.replaceAll(
				"(?ism)(STATEMENTS?[ ]{1,4}OF) ?[\r\n]{1,2}[ ]{5,90}(OPERATIONS?|INCOME|CASH FLOWS?|EARNINGS?)",
				"$1 $2\r\n");
		// System.xut.println("8 text aft="+text);

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("25b duration=" + duration);
		// startTime = System.currentTimeMillis();
		text = text.replaceAll("(?ism)(STATEMENTS?) ?[\r\n]{1,2}[ ]{5,100}(OF OPERATIONS|OF INCOME|OF CASH FLOWS?)",
				"$1 $2");

		// Consolidated Statements of Cash
		// Flows
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("25c duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("(?ism)(STATEMENTS? OF CASH) ?[\r\n]{1,2}[ ]{5,100}(FLOWS?)", "$1 $2");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("26 duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("(?i)\\(A DEVELOPMENT STAGE COMPANY\\)", "");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("27 duration=" + duration);
		// startTime = System.currentTimeMillis();

		// cut off for a CH to match is it must be at least 25 characters after
		// start of line. However if you have a pattern on a single LINE that is
		// PMPM (Period Month Period Month) without a year it really can't be a
		// tablesentence - wouldn't make any sense unless it contained the word
		// 'and' - so I replace any line preceded by a hard return and followed
		// by 20 to 24 characters with a hard return and 25 characters so it
		// meets the test. I then add an additional space in the separator (b/w
		// PM and PM). There's no room for an 'and' separator in pattern below
		// n/w no negative lookback requirements

		// adjust idx by up to 4

		text = text.replaceAll("(?i)([\r\n]{1}[ ]{21,24}(?=((Three|Six|Nine|Twelve) Months? End[eding]{2,3} "
				+ "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec).{1,7}[\\d]{1,2},?"
				+ " (Three|Six|Nine|Twelve) Months? End[eding]{2,3} (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec).{1,7}[\\d]{1,2})))",
				"\r                         ");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("28 duration=" + duration);
		// startTime = System.currentTimeMillis();
		if (fileSize < 5 * skip || parseEntireFiling)
			text = text.replaceAll(
					"(?i)(?<=( (Three|Six|Nine|Twelve) Months? End[eding]{2,3} (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)"
							+ ".{1,7}[\\d]{1,2},?) (?=(Three|Six|Nine|Twelve) Months? End[eding]{2,3} (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)"
							+ ".{1,7}[\\d]{1,2}))",
					"  ");
		// System.xut.println("10 text aft="+text);
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("29 duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("(?i)in  200[01]", "");
		// ==>>if unaudited is followed by a month or year - don't replace it.
		// Strands the CH if only 1 on that line. See other blox text - I may
		// need to address in terms of how I map CHs.

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("30 duration=" + duration);

		// startTime = System.currentTimeMillis();
		if (fileSize < skip || parseEntireFiling)
			text = text.replaceAll("(?i)(?<=([\r\n]{1}[ ]{0,65}))\\(unaudited\\)"
					+ "(?!([ ]{5,70}([12]{1}[09]{1}[0-9]{2}|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|\\(unaudited\\))))",
					"           ");
		// System.xut.println("11 text aft="+text);
		if (fileSize * 2 < skip || parseEntireFiling)
			text = text.replaceAll("(?i)(?<=[\r\n]{1}[ ]{0,65})assets ?(?=($|[\r\n]{1}))", "      ");
		// Feb.28
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("31 duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("(?i)(?<=(jan|feb|mar|apr|jun|jul|aug|sep|oct|nov|dec))\\. ?", " ");
		// feb 28,2003
		// System.xut.println("12 text aft="+text);
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("32 duration=" + duration);
		// startTime = System.currentTimeMillis();
		if (fileSize < 5 * skip || parseEntireFiling)
			text = text.replaceAll(
					"(?i)( )(?<=((january|february|march|april|june|july|august|september|october|november|december)"
							+ " )([0-9]{1,2})),(?=([12]{1}[09]{1}[0-9]{2}))",
					" ");

		// March 31,`99 March 31,'99 --> Mar 31, 1999
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("32a duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll(
				"(?i)(jan|feb|mar|may|apr|jun|jul|aug|sep|oct|nov|dec)[uarychileystmbo]{1,6} (\\d\\d?),[\\`\\']{0,1}(9\\d)",
				"$1 $2,19$3 ");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("32b duration=" + duration);
		// startTime = System.currentTimeMillis();
		text = text.replaceAll(
				"(?i)(jan|feb|mar|may|apr|jun|jul|aug|sep|oct|nov|dec)[uarychileystmbo]{1,6} (\\d\\d?),[\\`\\']{0,1}(0\\d)",
				"$1 $2, 20$3 ");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("33 duration=" + duration);
		// startTime = System.currentTimeMillis();

		// LEAVE </TABLE B/C IT HELPS END A TABLE. REMOVE <TABLE> B/C IT
		// CORRUPTS PAIRING OF CH WITH DATA TABLE.

		text = text.replaceAll("<TABLE> ?[\r\n]{1,}|<CAPTION> ?[\r\n]{1,}", "");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("34 duration=" + duration);
		// startTime = System.currentTimeMillis();
		text = text.replaceAll("INCREASE \\(DECREASE\\) IN CASH AND CASH EQUIVALENTS(?=.{5,80}[12]{1}[09]{1}[\\d]{2})",
				"                                                ");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("34a duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("FISCAL.{1}PERIOD.{1}ENDED", "                   ");

		// requires minimum of 2 period ended or period or ended CHs but can be
		// 5 or 10
		// System.xut.println(" text aft="+text);
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("34b duration=" + duration);
		// startTime = System.currentTimeMillis();
		// System.xut.println(" text aft="+text);
		// must be preceded and followed by a hard return!
		text = text.replaceAll(
				"(?i)([\r\n]{1,2})" + "([ ]{6,120}(periods? end[inged]{2,3}|periods?|end[inged]{2,3}))"
						+ "(([ ]{6,120}(periods? end[inged]{2,3}|periods?|end[inged]{2,3}))+)(?=[ \t]{0,8}[\r\n]{1})",
				"");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("34b text aft="+text);
		// System.xut.println("35 duration=" + duration);
		// startTime = System.currentTimeMillis();
		// System.xut.println(" text aft="+text);
		text = text.replaceAll("   NIL   ", "   0     ");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("36 duration=" + duration);
		// startTime = System.currentTimeMillis();
		// System.xut.println(" text aft="+text);
		text = text.replaceAll("(?i)</P>|<P>|<B>|<BR>|</BR>|</B>|<PRE>|<U>|</U>|<FONT>|</FONT>", "");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("37 duration=" + duration);
		// startTime = System.currentTimeMillis();
		// System.xut.println(" text aft="+text);
		text = text.replaceAll("(?i)[ ]{10,200}cumulative from ?[\r\n]{1}", "");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("38 duration=" + duration);
		// startTime = System.currentTimeMillis();
		text = text.replaceAll("[\r\n]{1,2}AS  OF  ", "\r\nAS OF ");
		// System.xut.println(" text aft="+text);
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("39 duration=" + duration);
		// startTime = System.currentTimeMillis();
		text = text.replaceAll("FOR  THE  YEARS  ENDED  ", "FOR THE YEARS ENDED ");

		// System.xut.println(" text aft="+text);
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("40 duration=" + duration);
		// startTime = System.currentTimeMillis();
		// System.xut.println(" text aft="+text);
		if (fileSize < 100000)
			text = text.replaceAll("(?i)[\r\n]{1}.{10,90}"
					+ "(?<!((BALANCE |CASH |STATEM|CON|JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC|MONTH|WEEK|YEAR|THREE|SIX|NINE|TWELVE"
					+ "|[12]{1}[09]{1}[0-9]{2}).{0,90}))\\(?UNAUDITED\\)?" + "[\\. \\-\\_\\=\t]{0,90}[\r\n]{1}", "");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("41 duration=" + duration);
		// startTime = System.currentTimeMillis();
		// System.xut.println(" text aft="+text);
		text = text.replaceAll("([ ]{4})(9\\d)([ ]{4}| ?[$\r\n])", "  19$2    ");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("41a duration=" + duration);
		// startTime = System.currentTimeMillis();
		// System.xut.println(" text aft="+text);
		text = text.replaceAll("(?ism)(consolidated|condensed)[ ]{2,}(statements)", "$1 $2");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("41b duration=" + duration);
		// startTime = System.currentTimeMillis();
		text = text.replaceAll("(?ism)(consolidated.{0,4})(balance).{0,4}(sheets?)", "Consolidated Balance Sheets");
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("41c duration=" + duration);
		// startTime = System.currentTimeMillis();
		// System.xut.println(" text aft="+text);
		text = text.replaceAll("(?sm)((?i)(consolidated)? ?statements? of )(income|operations?|earnings?)",
				"Consolidated Statements of Income");
		// fix if lower case
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("41d duration=" + duration);
		// startTime = System.currentTimeMillis();
		// System.xut.println(" text aft="+text);
		text = text.replaceAll("(?sm)((?i)(consolidated )?statements? of )cash flows?",
				"Consolidated Statements of Cash Flows");

		// replace with common tablename and fix if lower case
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("41e duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("(?ism)(condensed)? ?(consolidated)? ?statements? of (profits?|distributable income)",
				"Consolidated Statements of Income");

		if (fileSize < skip || parseEntireFiling)
			text = text.replaceAll(
					"(?ism)(CONSOLIDATED).{0,2}(FINANCIAL)(?=.{0,400}(?!<(income|operations))(sales|revenue))",
					"STATEMENTS OF INCOME");
		// System.xut.println(" text aft="+text);
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("42 duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("   (Sep) (\\d\\d?) ,", "    $1 $2,");
		text = text.replaceAll("(?i)(?<=[ \t]{0,5})Income Statements(?=,)?", "Statements of Income");
		text = text.replaceAll("(?i)Statements? of Comprehensive Loss",
				"Statements of Comprehensive Income \\(Loss\\)");
		// System.xut.println(" text aft="+text);
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("42a duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll(
				"(?ism)[ \t]{4}(Condensed )?(Consolidated )?Statements of[ \r\n\t]{5,40}Financial Condition",
				"BALANCE SHEETS");

		text = text.replaceAll("(?i)Y e a r s?   ?e n d e d", "Years Ended            ");
		text = text.replaceAll("(?i)Y e a r s?   ?e n d i n g", "      Years Ending     ");
		// System.xut.println(" text aft="+text);
		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("42b duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("(?<=   )([12]{1}) ([09]{1}) (\\d) (\\d)(?=(   |$|\r|\n))", "$1$2$3$4   ");

		text = text.replaceAll("(?i)J A N U A R Y(   ?)?(\\d) ?(\\d)? ?(,)?", "   January $2$3$4    ");
		text = text.replaceAll("(?i)F E B R U A R Y(   ?)?(\\d) ?(\\d)? ?(,)?", "   February $2$3$4    ");
		text = text.replaceAll("(?i)M A R C H(   ?)?(\\d) ?(\\d)? ?(,)?", "  March $2$3$4  ");
		text = text.replaceAll("(?i)A P R I L(   ?)?(\\d) ?(\\d)? ?(,)?", "  April $2$3$4  ");
		text = text.replaceAll("(?i)M A Y(   ?)?(\\d) ?(\\d)? ?(,)?", " May $2$3$4 ");
		text = text.replaceAll("(?i)J U N E(   ?)?(\\d) ?(\\d)? ?(,)?", " June $2$3$4  ");
		text = text.replaceAll("(?i)J U L Y(   ?)?(\\d) ?(\\d)? ?(,)?", " July $2$3$4  ");
		text = text.replaceAll("(?i)A U G U S T(   ?)?(\\d) ?(\\d)? ?(,)?", "  AUGUST $2$3$4   ");
		text = text.replaceAll("(?i)S E P T E M B E R(   ?)?(\\d) ?(\\d)? ?(,)?", "    SEPTEMBER $2$3$4    ");
		text = text.replaceAll("(?i)O C T O B E R(   ?)?(\\d) ?(\\d)? ?(,)?", "   October $2$3$4   ");
		text = text.replaceAll("(?i)N O V E M BE R(   ?)?(\\d) ?(\\d)? ?(,)?", "   November $2$3$4   ");
		text = text.replaceAll("(?i)D E C E M B ER(   ?)?(\\d) ?(\\d)? ?(,)?", "   December $2$3$4   ");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("42c duration=" + duration);
		// startTime = System.currentTimeMillis();

		text = text.replaceAll("(?i)   from inception", "      Inception");
		text = text.replaceAll("(?i)(3|6|9|12) (months?) (end[eding]{2,3}) (?=(3|6|9|12))", "$1 $2 End   ");

		text = text.replaceAll("(?i)( of Financial Position)", " of Balance Sheet");

		// endTime = System.currentTimeMillis();
		// duration = (endTime - startTime) / 1000;
		// System.xut.println("43 duration=" + duration);
		// startTime = System.currentTimeMillis();
		// System.xut.println("43 text aft="+text);

		// ridiculous number format of # ### which should be #,###
		Pattern patternCommaGone = Pattern.compile("\\d [\\d]{3}\\)?[ ]{3,40}[\\$\\(]{0,2}[\\d]{1,3} [\\d]{3}");

		if (nlp.getAllIndexEndLocations(text, patternCommaGone).size() > 15) {
			text = text.replaceAll("(\\d) ([\\d]{3}\\)?[ ]{3,40}[\\$\\(]{0,2}[\\d]{1,3}) ([\\d]{3})", "$1,$2,$3");
			// System.xut
			// .println("text after replaced patternCommaGone (now comma put in). text=="
			// + text);
		}

		return text;
	}

	public int tableTextParser(String text, boolean parseFromTablesFolder, boolean parseEntireFiling,
			String adjustIdxLoc) throws IOException, SQLException, ParseException {
		double startT = System.currentTimeMillis();
		System.out.println("START");
		NLP nlp = new NLP();

		File filename = new File("c:/temp2/2.txt");

		// if (filename.exists())
		// filename.delete();

		// NLP.pwNLP = new PrintWriter(filename);

		hasTabs = false;
		tableCount = 0;
		tbC2 = 0;
		System.out.println("accno==" + acc);

		Pattern patternTab = Pattern.compile("\\t");

		if (parseFromTablesFolder) {
			parseTableFromFolder = true;
			String[] lineOne = text.split("\r\n");
			for (int i = 0; i < Math.min(1, lineOne.length); i++) {
				String[] fields = lineOne[0].split(",");
				acc = fields[0];
				cik = fields[1];
				fileDate = fields[2];
				if (fields[3].length() > 0) {
					coMatch = Integer.parseInt(fields[3]);
				}
				coNameMatched = fields[4];
				companyName = fields[5];
				formType = fields[6];
				fye = fields[7];
				tableCount = Integer.parseInt(fields[8]);
				// System.out.println("parsing from tables folder. ACCNO=" + acc);
			}
		}

		TableParser tp = new TableParser();
		int fileSize = text.length(), skip = 100000;
		if (parseEntireFiling) {
			skip = skip * 10;
		}

		double duration = 0;
		double startTime = System.currentTimeMillis();
		double endTime = 0;

		// saves time by removing
		if (fileSize > skip * 5) {

			// System.out.println("fileSize before 99 removal=" + fileSize);
			startTime = System.currentTimeMillis();
			text = NLP.removeEx99s(text);
			fileSize = text.length();
			// System.out.println("fileSize after 99 removal=" + fileSize);
			endTime = System.currentTimeMillis();
			duration = (endTime - startTime) / 1000;
			System.out.println("total duration to remove 99s=" + duration);

		}

		// too slow to run if filesize is too big
		if (fileSize < skip * 5) {

			startTime = System.currentTimeMillis();
			text = textReplacements(text, skip, fileSize, parseEntireFiling);
			endTime = System.currentTimeMillis();
			duration = (endTime - startTime) / 1000;
			System.out.println("total text replacement duration=" + duration);

		}

		startTime = System.currentTimeMillis();

		List<String[]> listTableNameStartIdxAndMatchedGroup = new ArrayList<String[]>();
		// too slow to run if filesize is too big
		if (fileSize < skip * 4) {
			listTableNameStartIdxAndMatchedGroup = nlp.getAllStartIdxLocsAndMatchedGroups(text,
					TableParser.TableNamePattern);
		}

		else {
			Pattern pattern1 = Pattern.compile("(?ism)(Statements? of |Consolidated |Condensend )"
					+ "(Shareholder|Stockholder|Changes in|Liabilit|Income|Operations?|Earnings|Loss|Cash Flows?)|Balance Sheets?");
			listTableNameStartIdxAndMatchedGroup = nlp.getAllStartIdxLocsAndMatchedGroups(text, pattern1);
		}

		endTime = System.currentTimeMillis();
		duration = (endTime - startTime) / 1000;
		// NLP.pwNLP.append(//NLP.printLn("duration to create
		// listTableNameStartIdxAndMatchedGroup", duration + ""));
		// System.xut
		// .println("duration to create listTableNameStartIdxAndMatchedGroup="
		// + duration);
		//NLP.printListOfStringArray("listTableNameStartIdxAndMatchedGroup=", listTableNameStartIdxAndMatchedGroup);

		startTime = System.currentTimeMillis();
		listTocEdtPerTotColsTblnm.clear();
		// listTocEdtPerTotColsTblnm=edt[0],per[1],tc[2],tn[3],tnShort[4],col[5],tocTblNo[6],pgNo[7],tsShort[8]
		// (and ALL tocs!).

		listTocEdtPerTotColsTblnm = NLP.getTableOfContents(text, acc);
		//NLP.printListOfStringArray("listTocEdtPerTotColsTblnm", listTocEdtPerTotColsTblnm);

		endTime = System.currentTimeMillis();
		duration = (endTime - startTime) / 1000;
		// System.xut.println("duration to getTableOfContents=" + duration);

		int endIdxCH = 0, startIdxCH = 0, buffer = 1200, tableNameStartIdx, tableNameEndIdx = 0, dataTableStartIdx,
				endOfSingleIterationOfLoop = -1;
		String tableText = "", toc;

		// System.xut
		// .println("printing -- listTableNameStartIdxAndMatchedGroup==");
		//NLP.printListOfStringArray(listTableNameStartIdxAndMatchedGroup);
		String tmpStr = "";

		Pattern patternNumberSimple = Pattern.compile("[ ]{3,}[\\d]{1,3},[\\d]{3}");
		Matcher matchNumberSimple;
		for (int i = 0; i < listTableNameStartIdxAndMatchedGroup.size(); i++) {

			// NLP.pwNLP.append(//NLP.printLn("ttp initial tablenamelong=",
			// listTableNameStartIdxAndMatchedGroup.get(i)[1]));
			// NLP.pwNLP.append(
			//NLP.printLn("startIdx initial tablenamelong=",
			// listTableNameStartIdxAndMatchedGroup.get(i)[0]));

			tableNameLong = listTableNameStartIdxAndMatchedGroup.get(i)[1].replaceAll("\\\\|\\)|\\(|\\$|'|\\*", "");

			if (nlp.getAllIndexEndLocations(tableNameLong,
					Pattern.compile("(?ism)(cash fl|balance sh|statements? of financial condition"
							+ "|changes in|sharehold|stockhold|liabili|income|operation|earning|loss)"))
					.size() < 1 || tableNameLong.contains("balance") || tableNameLong.contains("cash")
					|| tableNameLong.contains("operation") || tableNameLong.contains("statement")
					|| tableNameLong.contains("income") || tableNameLong.contains("earning"))
			// if lower case it is not a match.

			{
				// NLP.pwNLP.append(//NLP.printLn("continuing past this tableNameLong=",
				// tableNameLong));
				continue;
			}

			// see if rest of above are below -- distinguish between that which
			// is passed thru method (don't reset!)
			// see what maps and listsa are also reset here.

			allColHeadingsCaptured = false;
			allColText = "";
			booleanData = false;
			coMatch = 0;
			coNameMatched = "";
			coNameOnPriorLine = "";
			decimal = "1";
			endStartGrpRow = 0;
			hasOddBall = false;
			hasTabs = false;
			isRownameheader = false;
			list_TS_tsShort_tsPattern_tnValid.clear();
			listCHbyIdxLoc = new ArrayList<>();
			ListCHbyMidptLoc.clear();
			listCHEndedYrandMoText.clear();
			listPrevLgEidx = new ArrayList<>();
			// listCHLines.clear();
			listCHLinesFromGetStartGroupCHLinesMethod.clear();
			listColumnHeadingText.clear();
			listDataColsSIdxandEIdx.clear();
			listEdtPerFinal.clear();
			listEdtPerIdx.clear();
			mapAllCH.clear();
			mapAllCH2.clear();
			originalMapAllCH.clear();
			mapData.clear();
			mapYearStartIdx.clear();
			mapDataColIdxLocs.clear();
			mapNumberOfDataCols.clear();
			mapOfCh.clear();
			mergeCnt = 0;
			noteInChEndIdx = 100;
			numberOfCHsRatio = 0;
			parseTableFromFolder = false;
			removePrior = false;
			removeNote = false;
			rownameEIdx = 0;
			rownumber = 0;
			runAll = true;
			sIdxC1NumOfCsLastCeIdx.clear();
			startGroup = "";
			startGroupLimited = "";
			tableNameLong = "";
			tableNameShort = "";
			tableSaved = "";
			tableSentence = "";
			tableSentenceLastLine = "";
			tmp2ListCHbyIdxLoc.clear();
			tsPattern = "";
			tsShort = "";
			twoDataColsForEachColHdg = false;

			tableNameStartIdx = Integer.parseInt(listTableNameStartIdxAndMatchedGroup.get(i)[0]);
			// System.xut.println("tableNameStartIdx=" + tableNameStartIdx);

			tmpStr = text.substring(tableNameStartIdx + listTableNameStartIdxAndMatchedGroup.get(i)[1].length());
			tmpStr = tmpStr.substring(0, tmpStr.indexOf("\r"));
			// NLP.pwNLP.append(//NLP.printLn("tmpStr=", tmpStr));
			// ckS TableNamePattern is on same line as number - there can't be a
			// number on same line as tableName. e.g.,
			// LIABILITIES & STOCKHOLDERS' EQUITY 1,353,359
			matchNumberSimple = patternNumberSimple.matcher(tmpStr);
			if (matchNumberSimple.find()) {
				// System.xut.println("matchNumberSimple=" + matchNumberSimple);
				continue;
			}

			// NLP.pwNLP.append(//NLP.printLn("tableNameStartIdx=",
			// tableNameStartIdx + " tableNameLong=" +
			// listTableNameStartIdxAndMatchedGroup.get(i)[1]
			// + " endOfSingleIterationOfLoop=" + endOfSingleIterationOfLoop));
			// goes past last parsed table's endIdx
			if (endOfSingleIterationOfLoop > tableNameStartIdx) {
				continue;
			}

			tableNameLong = listTableNameStartIdxAndMatchedGroup.get(i)[1].replaceAll("\\\\|\\)|\\(|\\$|'|\\*", "");
			tableNameShort = tp.getTableNameShort(tableNameLong);

			// NLP.pwNLP.append(//NLP.printLn("idx=" + startIdxCH,
			// "\raw tableNameLong=" + tableNameLong + " tableNameShort=" +
			// tableNameShort));

			if (adjustIdxLoc.equals("2")) {
				tableNameEndIdx = tableNameStartIdx + tableNameLong.length();
			} else {

				// originally this - but sometimes it misses a table - so use
				// above as last try.
				tableNameEndIdx = tableNameStartIdx - tableNameLong.length();
			}

			// if (nlp.getAllIndexStartLocations(
			// text.substring(tableNameEndIdx, Math.min(tableNameEndIdx +
			// buffer, text.length())),
			// patternColumnHeadingSimple).size() < 3 )
			// continue;
			// doesn't really do anything.

			toc = text.substring(tableNameEndIdx, Math.min(tableNameEndIdx + 900, text.length()));

			startTime = System.currentTimeMillis();
			int[] isToc = NLP.isTableOfContentsTxt(toc);
			endTime = System.currentTimeMillis();
			duration = (endTime - startTime) / 1000;
			// System.xut.println("ck if it is toc duration=" + duration);

			// if isToc[0] then it is toc and continue; isToc[1] is the eIdx
			// of last match of toc which when added to tableNameEndIdx next
			// loop can't start before

			if (isToc[0] == 0 && isToc[1] >= 901) {
				endOfSingleIterationOfLoop = isToc[1] + tableNameEndIdx;
				// System.out.println("isToc[1]=" + isToc[1] + " tableNameEndIdx=" +
				// tableNameEndIdx);

				continue;
			}
			if (isToc[0] == 0 && isToc[1] < 901) {
				endOfSingleIterationOfLoop = tableNameEndIdx;
				continue;
			}

			// buffer=1000
			endIdxCH = Math.min(tableNameEndIdx + buffer, text.length());
			// if I shorten 240 - there are some tablenames at row of last CH
			// row-so prior two rows can be missed if this is say - 100 or -
			// 160. Here I find first instance of a CH prior to tn match and get
			// its startIdx and prior 90 spaces. If that start point result in
			// an earlier startpoint I use or if later (smaller amount of text
			// prior to tablename start) I use that subject to minimum backup of
			// 100

			int backup = 235, lastNumber = 0, firstCh = 0, b1 = 10000, b2 = 10000;
			String textBloxPrior = text.substring(Math.max(0, tableNameStartIdx - backup), tableNameStartIdx);
			// NLP.pwNLP.append(//NLP.printLn("textBloxPrior=", textBloxPrior));
			List<Integer> listNumbersSpacesNumbersPriorToTn = nlp.getAllIndexEndLocations(textBloxPrior,
					Pattern.compile(",\\d\\d\\d\\)?([ ]{3,}|\t)\\(?\\d.{0,10}[\r\n]{1}"));
			if (listNumbersSpacesNumbersPriorToTn.size() > 0) {
				lastNumber = listNumbersSpacesNumbersPriorToTn.get(listNumbersSpacesNumbersPriorToTn.size() - 1);
				// NLP.pwNLP.append(//NLP.printLn("last instance of
				// listNumbersSpacesNumbersPriorToTn=", lastNumber + ""));
			}

			List<Integer> listACHPriorToTn = nlp.getAllIndexStartLocations(textBloxPrior, AllColumnHeadingPattern);

			if (listACHPriorToTn.size() > 0) {
				firstCh = listACHPriorToTn.get(0);
				// NLP.pwNLP.append(//NLP.printLn("first instance of listACHPriorToTn=", firstCh +
				// ""));
			}

			// if last number occur prior to CH - I've backed up too far.
			if (lastNumber > 0) {
				b1 = backup - lastNumber;
				backup = Math.max(b1, 2);
				// NLP.pwNLP.append(//NLP.printLn("new backup from tablename (start after number)
				// start idx=",
				// backup + " lastNumber=" + lastNumber));

			}

			// need to start at first instance of firstCH
			if (firstCh > 0) {
				if ((firstCh < lastNumber) || lastNumber == 0) {
					// start at hard return prior to firstCH so that I backup to
					// the last hard retur prior to firstCH.
					String tStr = textBloxPrior.substring(0, firstCh);
					// NLP.pwNLP.append(//NLP.printLn("text blox to start of firstCH=", tStr));
					String tStrHR = tStr.substring(Math.min((tStr.lastIndexOf("\r") + 2), tStr.length()));
					// NLP.pwNLP.append(//NLP.printLn("text box lenth prior to firstCH=",
					// tStr.length() + ""));
					// NLP.pwNLP.append(//NLP.printLn("text.len on line prior to firstCH=",
					// tStrHR.length() + ""));
					// FIND START OF FIRST CH - HOW FAR IS THAT FROM START OF
					// BACKUP - IF ONLY 10 -- BACKUP==225. (235-10)
					b2 = (backup - (tStr.length() - tStrHR.length()));
					backup = Math.max(b2, 50);
					// NLP.pwNLP.append(//NLP.printLn(
					// "new backup from tablename (start at a minimum of 50 prior to first Ch) start
					// idx=",
					// backup + ""));
				}
			}

			// NLP.pwNLP.append(//NLP.printLn("blox prior to TableNameStartIdx =",
			// textBloxPrior + " and new amount to backup from tableNameStartIdx=" +
			// backup));

			// if not CHs prior to tn - then shorten 235 to 160 or after last
			// number whichever is greatest.

			if (firstCh == 0 && lastNumber == 0) {
				// neither firstCh nor a number was found prior to
				// tableNameStartIx - So I shouldn't start to much prior to
				// tablename start idx (just 1 line)
				backup = 115;
			}

			// NLP.pwNLP.append(//NLP.printLn("backup from tablename start idx=",
			// "" + backup + " firstCH=" + firstCh + " lastNumber=" + lastNumber));

			startIdxCH = Math.max(0, tableNameStartIdx - backup);
			startGroup = text.substring(startIdxCH, endIdxCH);
			int sgIdxLoc = text.indexOf(startGroup);

			// NLP.pwNLP.append(//NLP.printLn("first startGroup=", "\r" + startGroup + "|first
			// startGroup end"));

			// NLP.pwNLP.append(//NLP.printLn("data col count in startGroup=",
			// nlp.getAllIndexEndLocations(startGroup, NLP.patterDataColumns).size() + ""));
			// increased capture after tablename to 1200 - so if no data cols -
			// continue to next column name.
			if (nlp.getAllIndexEndLocations(startGroup, NLP.patterDataColumns).size() < 1)
				continue;

			/*
			 * if blox above is 2000 char and old tableNameEndIdx=15000 (which is start of
			 * blox) - then new tableNameEndIdx is old plus new (if new is 39 then
			 * 15000+39). If new tableNameStartIdx is 15 prior to new tableNameEndIdx then
			 * new tableNameStartIdx is old tableNameEndIdx plus new tableNameStartIdx. new
			 * sIdx[0],eIdx[1],tableNameLong[2]. add eIdx[1] to tableNameEndIdx to get new
			 * tableNameEndIdx add eIdx[1] to old. To get new sIdx add to old
			 * tableNameStartIdx
			 */

			String blox = text.substring(tableNameEndIdx, endIdxCH);
			// NLP.pwNLP.append(//NLP.printLn("blox=", blox));

			// this creates the problem! NOT SURE WHY
			// MAP OUT WHY THAT IS.
			startTime = System.currentTimeMillis();
			List<String> listAddToEndIdxChAndTablename = NLP.checkForTableNameLaterInStartGroup(blox);
			endTime = System.currentTimeMillis();
			duration = (endTime - startTime) / 1000;
			// System.xut.println("1 checkForTableNameLaterInStartGroup duration="
			// + duration);

			// NLP.pwNLP.append(//NLP.printLn("startIdxCH bef looking within prelminary
			// startGroup=",
			// startIdxCH + " and endIdxCH=" + endIdxCH + " tableNameLong=" +
			// tableNameLong));

			if (null != listAddToEndIdxChAndTablename && listAddToEndIdxChAndTablename.size() > 0) {

				tableNameLong = listAddToEndIdxChAndTablename.get(listAddToEndIdxChAndTablename.size() - 1)
						.replaceAll("\\\\|\\)|\\(|\\$|'|\\*", "");
				tableNameShort = tp.getTableNameShort(tableNameLong);
				// NLP.pwNLP.append(//NLP.printLn("ae tablenamelong=", tableNameLong + "
				// tableNameShort=" + tableNameShort));

				tableNameStartIdx = Integer
						.parseInt(listAddToEndIdxChAndTablename.get(listAddToEndIdxChAndTablename.size() - 3))
						+ tableNameStartIdx;

				// NLP.pwNLP.append(//NLP.printLn(
				// "new tablename found in startGroup -- tableNameLong=",
				// tableNameLong + " in startGroup tableNameEndIdx="
				// + Integer.parseInt(listAddToEndIdxChAndTablename.get(1))
				// + " in startGroup tableNameStartIdx="
				// + Integer.parseInt(listAddToEndIdxChAndTablename.get(0))));

				tableNameEndIdx = Integer.parseInt(
						listAddToEndIdxChAndTablename.get(listAddToEndIdxChAndTablename.size() - 2)) + tableNameEndIdx;
				// NLP.pwNLP.append(
				//NLP.printLn("adjusted tableNameEndIdx (old+new) tableNameEndIdx =",
				// tableNameEndIdx + ""));

				startIdxCH = Math.max(0,
						Integer.parseInt(listAddToEndIdxChAndTablename.get(listAddToEndIdxChAndTablename.size() - 3))
								+ startIdxCH);
				// endIdxCH = Math.min(tableNameEndIdx + 1050, text.length());
				// b/c I require any startGroup to have data cols - there's no
				// need to increase endIdx
				// System.xut.println("Integer.parseInt(listAddToEndIdxChAndTablename\r\n" +
				// " .get(listAddToEndIdxChAndTablename.size() - 3))" +
				// " + startIdxCH)==="+Integer.parseInt(listAddToEndIdxChAndTablename
				// .get(listAddToEndIdxChAndTablename.size() - 3))
				// + startIdxCH);

				// System.xut.println("startIdxCh="+startIdxCH+" text.len="+text.length()+"
				// endIdxCh="+endIdxCH);
				if (startIdxCH >= endIdxCH) {
					endIdxCH = Math.min(endIdxCH + 1500, text.length());
				}

				startGroup = text.substring(startIdxCH, Math.min(text.length(), endIdxCH));

				// NLP.pwNLP.append(//NLP.printLn("startGroup with new eIdx due to new tn=",
				// startGroup));

			}

			// NLP.pwNLP.append(//NLP.printLn("startIdxCH AFT looking within prelminary
			// startGroup=",
			// startIdxCH + " and endIdxCH=" + endIdxCH + " tableNameLong=" +
			// tableNameLong));

			// NLP.pwNLP.append(//NLP.printLn("1. startIdxCH=", startIdxCH + " endIdxCH=" +
			// endIdxCH));
			// NLP.pwNLP.append(//NLP.printLn("1. startGroup.len=", startGroup.length() +
			// ""));

			if (!parseFromTablesFolder) {
				String companyNameText = startGroup.replaceAll("[\r\n]", " ").replaceAll("[\\s ]{2,15}", " ").trim();
				coMatch = 0;
				startTime = System.currentTimeMillis();
				coNameMatched = nlp.getCompanyNameConfirmed(companyNameText, companyName);
				endTime = System.currentTimeMillis();
				duration = (endTime - startTime) / 1000;
				// System.xut.println("1 getCompanyNameConfirmed duration="
				// + duration);

				coNameMatched = coNameMatched.replaceAll("[\\\\\\/\\$\\(\\)\\&]", "");
				coNameMatched = coNameMatched.substring(0, Math.min(coNameMatched.length(), 255));

				if (coNameMatched.length() > 1) {
					coMatch = 1;
				}
			}

			tableNameShort = tp.getTableNameShort(tableNameLong);

			decimal = nlp.getDecimal(startGroup);
			// can't measure before and after because getColumnHeadingMap
			// significantly reduces startGroup length
			// NLP.pwNLP.append(//NLP.printLn(
			// "startGroup prior to getColumnHeadingMap=", startGroup));
			int startGroupLength = 0;
			startGroup = startGroup.replaceAll("[\r\n]", "\r");
			String[] startGroupSplit = startGroup.split("\r");
			boolean foundTabs = false;
			// original startGroup length depends if any tabs were replaced and
			// how many lines are discarded. Therefore split startGroup by line
			// prior to replacing tabs and then use its length based on how many
			// lines are in final limited startGroup.

			int tabCnt = nlp.getAllIndexEndLocations(startGroup, patternTab).size();
			if (tabCnt >= 1) {
				// NLP.pwNLP.append(//NLP.printLn("startGroup prior to tab replacements=",
				// startGroup));
				// if there are tabs - then length will be larger in beginning
				// than in end so I would shorten dataStartIdx by adding the
				// resulting negative value
				// NLP.pwNLP.append(//NLP.printLn("replacing tabs in startGroup. tabCnt=", tabCnt
				// + ""));
				// NLP.pwNLP.append(//NLP.printLn("before replaceTabs getColumnHeadingMap
				// startGroup.len=",
				// startGroup.length() + ""));
				startTime = System.currentTimeMillis();
				startGroup = nlp.replaceTabsFromSecFiling(startGroup);
				// NLP.pwNLP.append(//NLP.printLn("startGroup after tab replacement",
				// startGroup));
				endTime = System.currentTimeMillis();
				duration = (endTime - startTime) / 1000;
				// System.xut.println("1 replaceTabsFromSecFiling duration="
				// + duration);

				// NLP.pwNLP.append(//NLP.printLn(
				// "after replaceTabs getColumnHeadingMap startGroup.len=", startGroup.length()
				// + ""));
				// NLP.pwNLP.append(//NLP.printLn("startGroup after replaceTabsFromSecFiling=",
				// startGroup));
				foundTabs = true;
			}

			startTime = System.currentTimeMillis();
			// System.xut
			// .println("nlp.getAllIndexStartLocations(startGroup,
			// AllColumnHeadingPattern).size()=="
			// + nlp.getAllIndexStartLocations(startGroup,
			// AllColumnHeadingPattern).size());
			// if(nlp.getAllIndexStartLocations(startGroup,
			// AllColumnHeadingPattern).size()<4)
			// continue;
			// NLP.pwNLP.append(//NLP.printLn("initial startGroup after
			// replaceTabsFromSecFiling=", startGroup));

			startGroup = getColumnHeadingMap(startGroup.replaceAll("(?i)(</b>)", "    "), 0);
			// @getColumnHeadingMap - mapAllCH2 is now complete. runs once
			endTime = System.currentTimeMillis();
			duration = (endTime - startTime) / 1000;
			// System.xut.println("getColumnHeadingMap duration=" + duration);

			if (startGroup == null)
				continue;

			if (foundTabs) {
				// for each row of final start group I get the length of
				// original startGroup line prior to replacing tabs. this gives
				// me original length which is needed to append to text starting
				// point in order to locate start of data row
				String[] startGroupFinal = startGroup.split("\r");
				int startGroupSplitGLen = 0;
				for (int g = 0; g < startGroupFinal.length; g++) {
					if (g + 1 > startGroupSplit.length)
						continue;
					startGroupSplitGLen = startGroupSplit[g].length();
					startGroupLength = startGroupLength + startGroupSplitGLen + 1;
					if (g + 1 == startGroupFinal.length) {
						startGroupLength = startGroupLength - 1;
					}
				}
			}

			// if no tabs replacement I can use startGroup length and not run
			// loop above
			else
				startGroupLength = startGroup.length();

			// NLP.pwNLP.append(//NLP.printLn("after getColumnHeadingMap - final limited
			// startGroup=", startGroup));

			//NLP.printMapIntListOfStringAry("after ltd startgrp - mapAllCH2", mapAllCH2);

			if (mapAllCH2.size() < 1 && mapAllCH.size() < 1) {
				continue;// to next tablename
			}

			// use tmpStartGrpLen b/c it is the value prior to replacements and
			// text str is w/o regard to replacements in startGroup
			dataTableStartIdx = Math.min(text.length(), startIdxCH + startGroupLength);

			rownameEIdx = 0;
			// NLP.pwNLP.append(//NLP.printLn("at right before getTable -- noteInChEndIdx=",
			// noteInChEndIdx + ""));
			startTime = System.currentTimeMillis();
			// if startGroup ends w/n 3 of a hard return - the dataTableStartIdx
			// should be 2 at last idx of hard return.

			if (startGroup.lastIndexOf("\r") > 0
					&& startGroup.substring(startGroup.lastIndexOf("\r"), startGroup.length()).length() < 4) {
				// System.xut
				// .println("bef dataTableStartIdx=" + dataTableStartIdx);
				dataTableStartIdx = dataTableStartIdx - (startGroup.length() - startGroup.lastIndexOf("\r"));
				// System.xut
				// .println("aft dataTableStartIdx=" + dataTableStartIdx);
			}

			tableText = getTableText(text.substring(dataTableStartIdx), false);
			endTime = System.currentTimeMillis();
			duration = (endTime - startTime) / 1000;
			// System.xut.println("getTableText duration=" + duration);
			// NLP.pwNLP.append(//NLP.printLn("getTableText", tableText));

			// if (!parseFromTablesFolder) {
			// tableSaved = "\r" + startGroup + "\r" + tableText;
			// }

			if (null == tableText) {
				endOfSingleIterationOfLoop = dataTableStartIdx + 1;
				// NLP.pwNLP.append(//NLP.printLn(
				// "initial tableText is null - continue to tableName if after end of last
				// tableName end idx",
				// ""));
				continue;
			}

			// redoMap=false - not yet determined.
			sIdxC1NumOfCsLastCeIdx = getsIdxC1NumOfCsLastCeIdx(tableText, false);
			//NLP.printListOfIntegerArray("sIdxC1NumOfCsLastCeIdx", sIdxC1NumOfCsLastCeIdx);

			// retuns number of data col idx by finding all data cols end idxs
			// that are close to each other and if many have same eIdx they are
			// a data col. Also return sIdx of col1 and last col eIdx.
			int numberOfDataCols = sIdxC1NumOfCsLastCeIdx.get(0)[1];

			/*
			 * getDataColumnCountBasedOnEachRows will determine # of data cols by ckg how
			 * many data col are on each row and seeing how many rows in table have the same
			 * number of maximum data cols. If greater than 70% of all rows have the same #
			 * it will it will return that as # of data cols. If there are tabs - often
			 * method above returns too many or few data cols b/c eIdx don't match up (one
			 * row has dif eIdx for same col as another row)
			 */

			// tblTextLoop2 is w/ tabs! (true).
			String tableTextLoop2 = getTableText(text.substring(dataTableStartIdx), true);
			if (null == tableTextLoop2) {
				// if no tabs found getTable will stop and return null b/c
				// result is same as tableText previously retrieved with
				// getTableText
				tableTextLoop2 = tableText;
				// NLP.pwNLP.append(//NLP.printLn("getTable tableTextLoop2=", tableTextLoop2));

			}

			startTime = System.currentTimeMillis();
			int numberOfDataColumnsBasedOnRows = NLP.getDataColumnCountBasedOnEachRows(tableTextLoop2);
			endTime = System.currentTimeMillis();
			duration = (endTime - startTime) / 1000;
			// System.xut.println("getDataColumnCountBasedOnEachRows duration="
			// + duration);

			boolean redoMap = false;

			// NLP.pwNLP.append(//NLP.printLn("\r\rhasTabs=",
			// "" + hasTabs + "\rnumberOfCHsRatio=" + "" + numberOfCHsRatio +
			// "\rnumberOfDataCols=" + ""
			// + numberOfDataCols + "\rnumberOfDataColumnsBasedOnRows=" +
			// numberOfDataColumnsBasedOnRows));
			//NLP.printMapIntListOfStringAry("mapAllch2", mapAllCH2);

			/*
			 * if redoMap it will rerun mapData (see mapDataSimple) so that there's no
			 * reliance on eIdx to determine data cols - but just tabs and multi ws. This
			 * will only matter if there are tabs or spacing is off of data cols.
			 */

			// redoMap true when numberOfDataCols!=numberOfCHsRatio but number
			// of data cols found on each row equals numberOfCHsRatio.
			if (numberOfDataColumnsBasedOnRows > 0 && numberOfDataColumnsBasedOnRows != numberOfDataCols
					&& numberOfDataColumnsBasedOnRows == numberOfCHsRatio) {
				redoMap = true;
				// NLP.pwNLP.append(//NLP.printLn("\rA redoMap=", redoMap + ""));
			}

			// I may want to run this if tabs or
			// if I found numberOfDataColumnsBasedOnRows is same as
			// numberOOfCHsRatio - see 2nd condiiton.

			// Should this occur before after I attempt to fix numberOfCHsRatio<
			// or < numberOfDataCols in the 3 or method below?? Call this method
			if (redoMap && null != tableTextLoop2) {
				// redoMap=true numberOfCHsRatio!=noOfDataCols but does equal
				// numberOfDataColumnsBasedOnRows

				/*
				 * I don't need to rerun this if tblText !=null or if # of data cols counted on
				 * a row (based on rows) isn't equal to # of CHs. If hasTabs then previously
				 * tableTextLoop2 (tableText) was fetched w/o tab removal. Don't repl tabs else
				 * it corrupts idx loc. If hasTabs & numberOfCHsRatio=numberOfDataCols (eidx)
				 * this won't run and non-exception methods will.
				 */

				// redoMap passed here JUST to alter replacements to exclude
				// '-'.
				sIdxC1NumOfCsLastCeIdx = getsIdxC1NumOfCsLastCeIdx(tableTextLoop2, redoMap);

				// NLP.pwNLP.append(//NLP.printLn(
				// "numberOfDataCols reset to (same as numberOfCHsRatio)
				// numberOfDataColumnsBasedOnRows="
				// + numberOfDataColumnsBasedOnRows + " previously numberOfDataCols was="
				// + numberOfDataCols + " numberOfCHsRatio=" + numberOfCHsRatio + "at redoMap",
				// redoMap + "\r\r tableTextLoop2=" + tableTextLoop2 + "|END"));

				// NOTE: getsIdxC1NumOfCsLastCeIdx still sets idx loc based on
				// ws even if tab present. so will return corrupt spaces.
				// default is sIdxC1NumOfCsLastCeIdx.get(0)[1].

				numberOfDataCols = numberOfDataColumnsBasedOnRows;
			}

			if (numberOfCHsRatio > numberOfDataCols) {

				fixTooManyColumnHeadings(numberOfDataCols);
				// resets numberOfCHsRatio,mapAllCH2, mapAllCH,mapDataColIdxLocs
				//NLP.printMapIntListOfStringAry("after fixTooManyColumnHeadings - mapAllCH2",
				// mapAllCH2);
				// NLP.pwNLP.append(//NLP.printLn("printing mapAllCH - size=", mapAllCH.size() + "
				// numberOfCHsRatio="
				// + numberOfCHsRatio + " numberOfDataCols=" + numberOfDataCols));

				// if(noteInCh && numberOfDataCols!=numberOfCHsRatio){
				// noteInCh=false;
				// }

			}

			if (numberOfCHsRatio != numberOfDataCols) {
				// System.xut.println("2 set number of CHs to year CHs");
				// sets numberOfCHsRatio based on number of years (1998, 1997,
				// 1996) found. See:
				// https://www.sec.gov/Archives/edgar/data/800082/0000800082-98-000014.txt
				// filter applies if each yr endIdx is not at least 8 chars
				// apart

				int key, cntCh = 0;
				// each String[] in list is CH - endidx and CH text.
				for (Map.Entry<Integer, List<String[]>> entry : mapAllCH.entrySet()) {
					key = entry.getKey();
					String valueStr;
					for (int b = 0; b < entry.getValue().size(); b++) {
						valueStr = entry.getValue().get(0)[1];
						if (valueStr.replaceAll("[12]{1}[09]{1}[0-9]{2}", "").length() < valueStr.length()) {
							// NLP.pwNLP.append(//NLP.printLn(
							// "ch that increases cntCh b/c it is a year value=",
							// entry.getValue().get(b)[1]));
							cntCh++;
						}
						if (Integer.parseInt(entry.getValue().get(b)[0]) < (rownameEIdx - 5)) {
							cntCh--;
							continue;
						}
					}
				}

				if (cntCh == numberOfDataCols) {
					numberOfCHsRatio = cntCh;
				}
				// System.xut.println("2 numberOfCHsRatio=="+numberOfCHsRatio+
				// " numberOfDataCols="+numberOfDataCols);
			}

			// ?what is the value of this code?
			twoDataColsForEachColHdg = false;
			if ((numberOfCHsRatio > 0 && numberOfDataCols > 0 && numberOfDataCols / numberOfCHsRatio == 2) || redoMap) {
				numberOfDataCols = numberOfCHsRatio;
				// NLP.pwNLP.append(//NLP.printLn(
				// "reset numberOfCHsRatio to equal noOfDataCols - numberOfDataCols=",
				// numberOfDataCols + " numberOfCHsRatio=" + numberOfCHsRatio));
				if (numberOfCHsRatio != 0 && numberOfDataCols / numberOfCHsRatio == 2) {
					twoDataColsForEachColHdg = true;
				}
			}

			// NLP.pwNLP.append(//NLP.printLn("numberOfDataCols=", numberOfDataCols + "
			// numberOfDataColumnsBasedOnRows="
			// + numberOfDataColumnsBasedOnRows + " numberOfCHsRatio=" + numberOfCHsRatio));
			if ((numberOfCHsRatio + 1) == numberOfDataCols && numberOfDataColumnsBasedOnRows == numberOfDataCols
					&& nlp.getAllIndexStartLocations(startGroup, NLP.patternInception).size() > 0) {
				// if number of year matches in mapAllCh2 is equal to number of
				// data cols -then incep col has no year - so set
				// numberOfCHsRatio to numberOfDataCols

				int key, cntYr = 0;
				boolean hasInception = false;
				for (Map.Entry<Integer, List<String[]>> entry : mapAllCH2.entrySet()) {
					key = entry.getKey();
					for (int d = 0; d < entry.getValue().size(); d++) {
						String tStr = entry.getValue().get(d)[1].replaceAll("\\!|,|\\(|\\)", " ");

						// NLP.pwNLP.append(//NLP.printLn("entry.getValue().get(d)[1]==", tStr));

						if (nlp.getAllIndexStartLocations(entry.getValue().get(d)[1], NLP.patternInception)
								.size() > 0) {
							hasInception = true;
						}
						if (nlp.getAllIndexStartLocations(tStr, Pattern.compile(" [12]{1}[09]{1}[0-9]{2}"))
								.size() > 0) {
							cntYr++;
						}
					}
				}

				if (cntYr == numberOfCHsRatio) {
					numberOfCHsRatio = numberOfDataCols;
				}
				// NLP.pwNLP.append(//NLP.printLn("cntYr=", " " + cntYr));
			}

			// NLP.pwNLP.append(//NLP.printLn("aaaa numberOfCHsRatio=" + numberOfCHsRatio + "
			// numberOfDataCols=",
			// numberOfDataCols + ""));

			if (numberOfCHsRatio != numberOfDataCols) {
				// NLP.pwNLP.append(//NLP.printLn(
				// "numberOfCHsRatio != numberOfDataCols printing mapAllCH2", ""));
				//NLP.printMapIntListOfStringAry("mapAllCH2", mapAllCH2);
				// NLP.pwNLP.append(//NLP.printLn(
				// "numberOfCHsRatio!=noOfDataCols - skipping this table numberOfDataCols=",
				// numberOfDataCols + " numberOfCHsRatio=" + numberOfCHsRatio));
				continue;
			}

			// TODO: insert into mysql for analysis all possible mp of any group
			// of CHs that can be paired in mysql.

			List<String> mergedCHRawList = mergeColumnHeadingLists();
			// Long form column heading (pre-enddate/period format).

			// NLP.pwNLP.append(//NLP.printLn("final mapAllCH.size==", mapAllCH.size() + ""));

			//NLP.printMapIntListOfStringAry("mapAllCH2", mapAllCH2);
			//NLP.printListOfString("mergedCHRawList", mergedCHRawList);

			// this if condition will fix when there are two col hdgs rows (2
			// keys inmapAllCH2) but mergedCHRawList merged them as one.
			@SuppressWarnings("unused")
			int startColA, endColA, startColB, endColB, s1, s2, e1, e2;
			String colm1, colm2;

			// NLP.pwNLP.append(//NLP.printLn("mapAllCH2.size",
			// mapAllCH2.size() + " mergedCHRawList.size=" + mergedCHRawList.size()));

			if (mapAllCH2.size() > mergedCHRawList.size() && mapAllCH2.size() == 2 && mapAllCH2.get(0).size() > 0
					&& mapAllCH2.get(1).size() > 0) {

				mergedCHRawList = new ArrayList<>();

				startColA = Integer.parseInt(mapAllCH2.get(0).get(0)[0]);
				startColB = Integer.parseInt(mapAllCH2.get(1).get(0)[0]);

				endColA = startColA + (mapAllCH2.get(0).get(0)[1].length());
				endColB = startColB + mapAllCH2.get(1).get(0)[1].length();

				if (startColA < startColB) {
					e1 = endColB;
					e2 = endColA;
					s1 = startColB;
					s2 = startColA;
					colm1 = mapAllCH2.get(0).get(0)[1];
					colm2 = mapAllCH2.get(1).get(0)[1];

					// System.xut
					// .println("A startColA=" + startColA + " endColA="
					// + endColA + " startColB=" + startColB
					// + " endColB=" + endColB + " e1=" + e1
					// + " e2=" + e2 + " s1=" + s1);

				}

				else {
					e2 = endColA;
					e1 = endColB;
					s2 = startColA;
					s1 = startColB;
					colm1 = mapAllCH2.get(1).get(0)[1];
					colm2 = mapAllCH2.get(0).get(0)[1];

					// System.xut
					// .println("B startColA=" + startColA + " endColA="
					// + endColA + " startColB=" + startColB
					// + " endColB=" + endColB + " e1=" + e1
					// + " e2=" + e2 + " s1=" + s1);

				}

				// System.xut.println("startColA=" + startColA + " endColA="
				// + endColA + " startColB=" + startColB + " endColB="
				// + endColB + " e1=" + e1 + " e2=" + e2 + " s1=" + s1);

				if (s1 > 40 && e1 < e2) {
					// System.xut.println("colm1=" + colm1 + "colm2=" + colm2);
					mergedCHRawList.add(colm1);
					mergedCHRawList.add(colm2);
					numberOfCHsRatio = 2;
				}
			}

			// NLP.pwNLP.append(//NLP.printLn("2 numberOfCHsRatio=", numberOfCHsRatio + ""));

			// System.xut.println("PRINTING LIST listEdtPerFromTableSentence");
			//NLP.printListOfStringArray(listEdtPerFromTableSentence);

			listEdtPerFinal = new ArrayList<>();

			// TS method seems to corrupt otherwise good CHs.

			// at this point idx/merg of CH are done- just need to record CH# to
			// each CH -see below
			// System.xut.println("printing final mergedCHRawList before running
			// listEdtPerFinal");
			//NLP.printListOfString(mergedCHRawList);

			// I need to record which data col CHs I kept - by virtue of idx
			// location of CH# that corresponds to col#. In some instances I
			// discard some CHs b/c I can't match them - so i could have 7 total
			// CHs but only utilize two CHs. In that case I can't just match
			// data col 1 and 2 with CH 1 and 2 b/c it may be the first and last
			// CH (1 and 7).

			// listEdtPerFinal dropped mid CHs

			// at this point listEdtPerTableSentence is STILL blank
			// System.xut.println("11 printing listEdtPerTableSentence");
			//NLP.printListOfStringArray(listEdtPerTableSentence);
			// System.xut.println("11 printing mergedCHRawList");
			//NLP.printListOfString(mergedCHRawList);

			startTime = System.currentTimeMillis();
			listEdtPerFinal = getMergedEndDates(mergedCHRawList, listEdtPerTableSentence);
			endTime = System.currentTimeMillis();
			duration = (endTime - startTime) / 1000;
			// System.xut.println("1 getMergedEndDates=" + duration);

			// System.xut.println("11 printing listEdtPerFinal");
			//NLP.printListOfStringArray(listEdtPerFinal);

			// System.xut.println("PRINTING LIST listEdtPerFinal right after capture");
			//NLP.printListOfStringArray(listEdtPerFinal);

			// has just colHdg
			listColumnHeadingText = new ArrayList<>();
			listColumnHeadingText = mergedCHRawList;
			// System.xut.println("PRINTING LISTCOLUMNHEADINGTEXT");
			//NLP.printListOfString(listColumnHeadingText);

			listCHEndedYrandMoText = new ArrayList<>();
			String ended = null, yr = null, mo = null;
			for (int b = 0; b < listColumnHeadingText.size(); b++) {
				// ended=0, yr=1,mo=2
				List<String> listEnded = nlp.getAllMatchedGroups(listColumnHeadingText.get(b),
						TableParser.EndedHeadingPattern);
				List<String> listMonth = nlp.getAllMatchedGroups(listColumnHeadingText.get(b),
						TableParser.MonthPatternSimple);
				List<String> listYr = nlp.getAllMatchedGroups(listColumnHeadingText.get(b),
						TableParser.YearOrMoDayYrPattern);

				if (listEnded.size() > 0)
					ended = listEnded.get(0).replaceAll("'", "");
				if (listMonth.size() > 0)
					mo = listMonth.get(0).replaceAll("'", "");
				if (listYr.size() > 0)
					yr = listYr.get(0).replaceAll("'", "");

				String[] ary = { ended, yr, mo };
				listCHEndedYrandMoText.add(ary);
				ended = null;
				mo = null;
				yr = null;
			}

			// System.xut.println("PRINTING listCHEndedYrandMoText=");
			//NLP.printListOfStringArray(listCHEndedYrandMoText);

			/*
			 * NOTE: getCHbyIdxLoc potentially has very high accuracy - b/c it uses MP
			 * matching and I can adjust tolerance. It works from bottom up and it will not
			 * pair even if MP condition met if it is duplicate edt/per (unless oddball CH
			 * which is likley to have 2 edt). getCHbyIdxLoc is primary. As a fall back -
			 * when getCHbyIdxLoc doesn't return value - I pair using MP only w/ default to
			 * first and last col - see getCHbyMidptLoc. If getCHbyIdxLoc is not highly
			 * accurate - make mpDist more restrictive. It filters out potential duplicate
			 * edt/per by not including 2nd instance (works from bottom col hdg line).
			 * chCompleted technique is used to skip cols already done. If too complex and
			 * cause additional errors abandon.
			 */

			// List<String> listChByMidptLocToFindMissingEdtPer = new
			// ArrayList<String>();
			// System.xut.println("starting to run CH by idx Loc or mid point");

			mergeCnt = 0; // used to cnt how many attempted merges - if 1 - then
							// just 1st two attempted and if failed we will call
							// get midpt.
			runAll = true;

			// mapAllCh is retrieved first - and will insert a NOTE column. But
			// I record the noteInChEndIdx (note in ch end idx). Later when I
			// got to getTable text if I see a pattern in data cols that
			// correspond to a note (eg: (1) or 1) I will set removeNote to
			// true) - at which point when I got to create listChByIdxLoc I
			// remove note from startGroup so that I don't overcount CHs.

			if (removeNote) {
				startGroup = startGroup.replaceAll("(?i)   notes   ", "           ").replaceAll("(?i)   note   ",
						"          ");
			}

			List<String> startGroupCHLines = getStartGroupCHLines(startGroup);
			//NLP.printListOfString("2 startGroupCHLines", startGroupCHLines);

			// if startGroupCHLines only has PM -- then it must be distributed
			// to all columns assuming I only have year values in listCHbyIdxLoc

			// if cntY=3 and cntM=2 join these two
			startTime = System.currentTimeMillis();
			listCHbyIdxLoc = getCHbyIdxLoc(startGroupCHLines);
			//NLP.printListOfString("1st listCHbyIdxLoc=", listCHbyIdxLoc);

			// PICKUP HERE -- WHY ISN'T IT GETTING WHAT WAS CORRECTLY CAPTURED
			// ?? WITH @getMonthYearEndedColHdg

			// see how many edt2 and p2 I am missing - and if more than 1 (at
			// least 2) - see if getMonthYearEndedColHdg has fewer missing
			// edt2/p2.Don't attempt tho if no edt2 found.
			allColText = getAllColText();
			// System.xut.println("allColText=" + allColText);
			// System.xut.println("tableSentence=" + tableSentence);
			// System.xut.println("tsShort=" + tsShort);

			// System.xut.println("1 allColHeadingsCaptured="
			// + allColHeadingsCaptured);
			if (null != listCHbyIdxLoc && listCHbyIdxLoc.size() > 0 && !allColText.contains("mCntD:1")
					&& nlp.getAllIndexEndLocations(tsShort, Pattern.compile("M")).size() < 1
					&& !allColHeadingsCaptured) {
				int[] cntMsgEdtP = NLP.howManyMissingEdtP(listCHbyIdxLoc);
				if (cntMsgEdtP[0] > 1 || (cntMsgEdtP[1] > 1 && !tableNameShort.toLowerCase().equals("bs")) && !hasTabs
						&& (cntMsgEdtP[0] - listCHbyIdxLoc.size() != 0)) {

					List<String> listCHsMoYrEnd = getMonthYearEndedColHdg(startGroupLimited, mapAllCH2,
							numberOfCHsRatio);
					// NLP.pwNLP.append(//NLP.printLn(
					// "getMonthYearEndedColHdg ran- was 1 allColHeadingsCaptured=" +
					// allColHeadingsCaptured
					// + " listNumberOfMissingEdtP missing edt=",
					// cntMsgEdtP[0] + " missing P=" + cntMsgEdtP[1] + " listCHbyIdxLoc.size="
					// + listCHbyIdxLoc.size() + " tableNameShort=" + tableNameShort));

					if (null != listCHsMoYrEnd && listCHsMoYrEnd.size() > 0) {

						int[] cntMsgEdtPMoYrEnd = NLP.howManyMissingEdtP(listCHsMoYrEnd);
						// if this method returns more edt2/p2 - use it instead.
						if (cntMsgEdtP[0] > cntMsgEdtPMoYrEnd[0]
								|| (cntMsgEdtP[1] > cntMsgEdtPMoYrEnd[1] && !tableNameShort.equals("bs"))) {
							listCHbyIdxLoc = listCHsMoYrEnd;
						}

						//NLP.printListOfString("3 getMonthYearEndedColHdg listCHbyIdxLoc=",
						// listCHbyIdxLoc);
					}
				}
			}

			endTime = System.currentTimeMillis();
			duration = (endTime - startTime) / 1000;
			// System.xut.println("1 getCHbyIdxLoc=" + duration);

			// getCHbyIdxLoc also loops through
			// getColHdgTwoMonthsThreeYearsTwoLines
			//NLP.printListOfString("2b listCHLines", listCHLines);

			if (null != startGroupCHLines) {
				for (int z = startGroupCHLines.size() - 1; z >= 0; z--) {
					startGroupLimited = startGroupLimited + "\r\n" + startGroupCHLines.get(z);
				}
			}

			// System.xut.println("just got startGroupLimited=="+startGroupLimited);
			// if listCHbyIdxLoc isn't complete run getMonthYearEndedColHdg - if
			// listCHbyIdxLoc is corrupt (size!=no of cols) then use whatever
			// this produces. OR if missing edts in listCHbyIdxLoc and
			// results of getMonthYearEndedColHdg produces none -uses results of
			// getMonthYearEndedColHdg

			// System.xut.println("does startGroup have 'incept' -- if so check again.
			// startGroupLimited="+startGroupLimited);

			int cntInception = 0;
			cntInception = (nlp.getAllMatchedGroups(startGroupLimited, NLP.patternInception)).size();

			// any cntInception instance getMonthYearEndedColHdg is called. See
			// below. Should work well for current column types (1:1, 2:1 and
			// 3:2).
			boolean useThisLoop = false, checkAgain = false;

			if (null != startGroupLimited) {
				if (listCHbyIdxLoc == null || (numberOfCHsRatio != listCHbyIdxLoc.size() && cntInception > 0)) {
					// if inception col - >0- okay if
					// numberOfCHsRatio=listCHbyIdxLoc
					checkAgain = true;
					useThisLoop = true;
					// NLP.pwNLP.append(//NLP.printLn("checkAgain=", checkAgain + " useThisLoop=" +
					// useThisLoop));
					//NLP.printListOfString("listCHbyIdxLoc", listCHbyIdxLoc);
				}

				// if not checkAgain/!useThisLoop above - this will see if edt
				// is incomplet - and if so checkAgain
				if (listCHbyIdxLoc != null && listCHbyIdxLoc.size() > 0 && !useThisLoop) {
					for (int f = 0; f < listCHbyIdxLoc.size(); f++) {
						if (listCHbyIdxLoc.get(f) == null || listCHbyIdxLoc.get(f).length() < 1) {
							checkAgain = true;
							break;

						}
						List<String> tmpL = nlp.getEnddatePeriod(listCHbyIdxLoc.get(f));
						if (tmpL.get(0).length() != 10) {
							checkAgain = true;
							// NLP.pwNLP.append(//NLP.printLn("2 checkAgain=", checkAgain + " useThisLoop=" +
							// useThisLoop));
							//NLP.printListOfString("listCHbyIdxLoc", listCHbyIdxLoc);

							break;
						}
						if (checkAgain)
							break;
					}
				}
			}

			// for oddball columns (development stage/inception etc) use
			// getMonthYearEndedColHdg -- it can pull out
			// odd column and then format good and add back odd - currently any
			// 3:2 or 2:1 or 1:1 ratios (which can also be 4:2 or 5:5).
			// System.xut.println("2 allColHeadingsCaptured="
			// + allColHeadingsCaptured);

			if (checkAgain && useThisLoop && !allColHeadingsCaptured) {
				// NLP.pwNLP.append(//NLP.printLn("checkAgain && useThisLoop", ""));

				listCHbyIdxLoc = getMonthYearEndedColHdg(startGroupLimited, mapAllCH2, numberOfCHsRatio);

				// NLP.pwNLP.append(//NLP.printLn("getMonthYearEndedColHdg ran- was 2
				// allColHeadingsCaptured=",
				// allColHeadingsCaptured + ""));

				// NLP.pwNLP.append(//NLP.printLn(
				// "@checkAgain && useThisLoop -- @getMonthYearEndedColHdg printing
				// listCHbyIdxLoc", ""));
				//NLP.printListOfString("3 listCHbyIdxLoc", listCHbyIdxLoc);

			}
			List<String> tmpL2 = new ArrayList<>();
			if (checkAgain && !useThisLoop && !allColHeadingsCaptured) {
				boolean useThisList = true;
				// NLP.pwNLP.append(//NLP.printLn("10 getMonthYearEndedColHdg", ""));
				// only use if results of getMonthYearEndedColHdg all result in
				// edt2.len=10 - else break which will default to earlier
				// retrieved listCHbyIdxLoc
				useThisLoop = true;
				// System.xut.println("3 allColHeadingsCaptured="
				// + allColHeadingsCaptured);
				tmpL2 = getMonthYearEndedColHdg(startGroupLimited, mapAllCH2, numberOfCHsRatio);
				// NLP.pwNLP.append(//NLP.printLn("getMonthYearEndedColHdg ran- was 3
				// allColHeadingsCaptured=",
				// allColHeadingsCaptured + ""));

				//NLP.printListOfString("@ck2 && !useThisLoop. getMonthYearEndedColHdg (tmpL2 -
				// listCHbyIdxLoc)", tmpL2);
				// NLP.pwNLP.append(//NLP.printLn("b useThisLoop=",
				// useThisLoop + " allColHeadingsCaptured=" + allColHeadingsCaptured));

				if (tmpL2 != null && tmpL2.size() > 0) {
					for (int f = 0; f < tmpL2.size(); f++) {
						// NLP.pwNLP.append(
						//NLP.printLn("a tmpL2.get(f)=" + tmpL2.get(f) + " - useThisList=", useThisList
						// + ""));

						if (tmpL2.get(f) == null || tmpL2.get(f).length() < 1) {
							useThisList = false;
							// NLP.pwNLP.append(//NLP.printLn("a tmpL2.get(f) - useThisList=", useThisList +
							// ""));
							break;
						}
						List<String> tmpL = nlp.getEnddatePeriod(tmpL2.get(f));
						if (tmpL.get(0).length() != 10 && !tmpL2.get(f).toUpperCase().contains("INCEPT")) {
							useThisList = false;
							// NLP.pwNLP.append(
							//NLP.printLn("aa useThisLoop=", useThisList + " tmpL.get(0)=" + tmpL.get(0)));
						}

						// NLP.pwNLP.append(//NLP.printLn("aaa (listCHbyIdxLoc) tmpL2 - useThisList=",
						// useThisList + ""));
					}
				}

				// if (tmpL2 != null) {
				// NLP.pwNLP.append(//NLP.printLn("(listCHbyIdxLoc) tmpL2.size=",
				// tmpL2.size() + " numberOfCHsRatio=" + numberOfCHsRatio + " useThisList=" +
				// useThisList
				// + " allColHeadingsCaptured=" + allColHeadingsCaptured));
				// }

				// if all col hdgs captured don't discard - keep it.
				// if useThisList then swap tmpL2 for listCHbyIdxLoc
				if (((!useThisList && allColHeadingsCaptured) || useThisList) && tmpL2 != null && tmpL2.size() > 0
						&& tmpL2.size() == numberOfCHsRatio) {
					listCHbyIdxLoc = tmpL2;
					//NLP.printListOfString("@ck2?? && useThisLoop. getMonthYearEndedColHdg (listCHbyIdxLoc)",
//							listCHbyIdxLoc);
				}
			}

			checkAgain = false;
			useThisLoop = false;
			// only accept above if all edt are length=10 else revert to earlier
			// listCHbyIdxLoc

			// if NO tsShort and only 1 month in StartGroupLimited - assign
			// tsShort to that one month value and run assignment of tsShort. I
			// can do this at very end just prior to insert ignore into mysql

			// tmpL2 grabbed CHs using getMonthYearEndedColHdg - which uses
			// various specified ratios to match as well as first and last.
			// calibration is tight so should always be correct.
			boolean onlyYears = false, sameSize = false;
			int cnt = 0;
			List<String> tmpL3 = new ArrayList<>();
			if (listCHbyIdxLoc != null && listCHbyIdxLoc.size() > 0) {
				//NLP.printListOfString("@ck years (listCHbyIdxLoc)", listCHbyIdxLoc);
				if (null != tmpL2 && tmpL2.size() == listCHbyIdxLoc.size()) {
					sameSize = true;
				}
				for (int c = 0; c < listCHbyIdxLoc.size(); c++) {
					if (sameSize && null != tmpL2 && null != listCHbyIdxLoc) {
						if (null != tmpL2.get(c) && null != listCHbyIdxLoc.get(c)
								&& tmpL2.get(c).length() > listCHbyIdxLoc.get(c).length()) {
							tmpL3.add(tmpL2.get(c));
						} else
							tmpL3.add(listCHbyIdxLoc.get(c));
					}
					if (null != listCHbyIdxLoc.get(c)
							&& listCHbyIdxLoc.get(c).replaceAll("[12]{1}[09]{1}[0-9]{2}", "").length() == 0) {
						cnt++;
					}
				}
				if (sameSize) {
					listCHbyIdxLoc = tmpL3;
				}

				if (cnt == listCHbyIdxLoc.size())
					onlyYears = true;

				// NLP.pwNLP.append(//NLP.printLn("cnt=" + cnt + " listCHbyIdxLoc.size()====",
				// listCHbyIdxLoc.size() + "" + " onlyYears=" + onlyYears));

			}

			// below only used where there is just 1 period 1 mo in startGroup
			// b/c tsShort from loop below must result in "PM". If 'onlyYears' -
			// above has already determined that listCHByIdx only picked up
			// years - so only check 2 rows above it. Otherwise I haveto ck
			// entire
			// startGroup rows to make sure only PM (and not PMM or PPM etc).

			int cntE = (nlp.getAllMatchedGroups(startGroup, TableParser.EndedHeadingPattern)).size();
			int cntM = (nlp.getAllMatchedGroups(startGroup, TableParser.MonthPatternSimple)).size();
			int cntY = nlp.getAllMatchedGroups(startGroup, TableParser.YearOrMoDayYrPattern).size();

			if ((null != listCHbyIdxLoc && listCHbyIdxLoc.size() == 0)
					|| (onlyYears && ((cntE == 0 || cntE == 1) && cntM == 1))) {

				Pattern tmpPtrn = Pattern.compile("(?<=[ \t]{2,})[0-9]{4}.*?(?=[ \t]{2,}| ?$)");
				if (listCHLinesFromGetStartGroupCHLinesMethod.size() > 0) {

					if (null != listCHbyIdxLoc && listCHbyIdxLoc.size() == 0) {
						listCHbyIdxLoc = nlp.getAllMatchedGroups(listCHLinesFromGetStartGroupCHLinesMethod.get(0),
								tmpPtrn);
					}

					// System.xut.println("printing listCHbyIdxLoc here3");
					//NLP.printListOfString(listCHbyIdxLoc);

					StringBuffer sb = new StringBuffer();
					if (tableNameLong == null || tableNameLong.length() < 5) {
						sb.append("xx    Balance Sheet" + "\n");
					} else {
						sb.append(tableNameLong);
					}

					// dummy tn to make
					// parseTableSentence work.

					// year found at .get(0) above so start at 1 & see if I only
					// find month-year values b/c above is pattern to find only
					// years.

					// this will check all remaining ch rows to see if pattern
					// is PM
					if (tsShort.length() < 1) {
						int rowsToCheck = 0;
						if (onlyYears) {
							rowsToCheck = Math.min(listCHLinesFromGetStartGroupCHLinesMethod.size(), 2);
						} else {
							rowsToCheck = listCHLinesFromGetStartGroupCHLinesMethod.size();
						}
						// assumes that last row (this goes inreverse) - is year
						// row)
						for (int n = 1; n < rowsToCheck; n++) {
							sb.append(listCHLinesFromGetStartGroupCHLinesMethod.get(n) + "\n");
						}

						// System.xut.println("3 parseTableSentence");
						List<String> tmpL = NLP.parseTableSentence(sb.toString(),
								companyName.substring(0, Math.min(companyName.length(), 7)), true, tableNameLong,
								false);
						// System.xut
						// .println("printing tmpL - pickup ts if just PM because I found only year");
						//NLP.printListOfString(tmpL);
						tableSentence = tmpL.get(0);
						if ((tableNameLong.length() < 2 && tmpL.get(6).length() > 1)
								|| (tmpL.get(6).length() > 1 && tmpL.get(3).equals("true"))) {
							tableNameLong = tmpL.get(6).replaceAll("\\\\|\\)|\\(|\\$|'|\\*", "");
							tableNameShort = tp.getTableNameShort(tableNameLong);
							// NLP.pwNLP.append(//NLP.printLn("aq tablenamelong=",
							// tableNameLong + " tableNameShort==" + tableNameShort));

						}

						if (!tsShort.equals("zz")) {
							tsShort = tmpL.get(1);
						}
						tsPattern = tmpL.get(2).replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)", "");

						if (tsShort.equals("PM")) {
							listEdtPerTableSentence = NLP.getTablesentenceColumnHeadings(tableSentence, tsShort,
									tsPattern);
						}
					}
				}
			}

			// System.xut
			// .println("3333 listCHLinesFromGetStartGroupCHLinesMethod");
			//NLP.printListOfString(listCHLinesFromGetStartGroupCHLinesMethod);
			if (null != listCHbyIdxLoc) {
				// NLP.pwNLP.append(//NLP.printLn("got CH by idx Loc. listCHbyIdxLoc=", ""));
				//NLP.printListOfString("listCHbyIdxLoc", listCHbyIdxLoc);
				// NLP.pwNLP.append(//NLP.printLn("listCHbyIdxLoc.size=",
				// listCHbyIdxLoc.size() + " no of data cols=" + numberOfDataCols));
			}

			// if mismatch in size of list - try to remove bad one ## add. Have
			// to be very specific otherwise I force matches which creates
			// further inaccuracy. Sometimes 'NOTES' is a column - but the data
			// won't get picked up as data but as rowname - so excluding from
			// col hdgs is corrective.

			if (null != listCHbyIdxLoc && listCHbyIdxLoc.size() > numberOfDataCols) {
				// System.xut.println(">>listChByIdxLoc.size="
				// + listCHbyIdxLoc.size() + "# of data cols="
				// + numberOfDataCols);
				List<String> tmpListCHbyIdxLoc = new ArrayList<>();
				for (int b = 0; b < listCHbyIdxLoc.size(); b++) {
					if (listCHbyIdxLoc.get(b).toLowerCase().contains("notes"))
						continue;
					tmpListCHbyIdxLoc.add(listCHbyIdxLoc.get(b));
				}

				if (tmpListCHbyIdxLoc.size() == numberOfDataCols)
					listCHbyIdxLoc = tmpListCHbyIdxLoc;
				// System.xut.println("printing listCHbyIdxLoc here4");
				//NLP.printListOfString(listCHbyIdxLoc);

			}

			if (listCHbyIdxLoc == null) {
				// grab just last line of CH as a last resort - last line should
				// be year
				// NLP.pwNLP.append(//NLP.printLn(
				// "99 listCHLinesFromGetStartGroupCHLinesMethod.size()==",
				// listCHLinesFromGetStartGroupCHLinesMethod.size() + ""));
				//NLP.printListOfString("listCHLinesFromGetStartGroupCHLinesMethod",
				// listCHLinesFromGetStartGroupCHLinesMethod);
				//NLP.printMapIntListOfStringAry("- mapAllCH2", mapAllCH2);

				Pattern tmpPtrn = Pattern.compile("(?<=[ \t]{3,})[12]{1}[09]{1}[0-9]{2}.*?(?=[ \t]{3,}| ?$)");
				if (listCHLinesFromGetStartGroupCHLinesMethod.size() > 0) {

					listCHbyIdxLoc = nlp.getAllMatchedGroups(listCHLinesFromGetStartGroupCHLinesMethod.get(0), tmpPtrn);
					if (listCHbyIdxLoc.size() != numberOfCHsRatio)
						listCHbyIdxLoc = null;
				} else if (mapAllCH2.size() > 0 && (null == listCHbyIdxLoc || listCHbyIdxLoc.size() == 0)) {
					// last row in map is bottom row?
					List<String> tmpList = new ArrayList<String>();
					List<String[]> tmpL = mapAllCH2.get(mapAllCH2.size() - 1);
					for (int d = 0; d < tmpL.size(); d++) {
						if (null != tmpL.get(d)[1]
								&& tmpL.get(d)[1].replaceAll("[12]{1}[09]{1}[0-9]{2}", "").length() == 0) {
							// NLP.pwNLP.append(//NLP.printLn(
							// "set to listCHbyIdx year CH hdgs -- tmpL.get(d)[1]=", tmpL.get(d)[1]));
							tmpList.add(tmpL.get(d)[1]);
						}
					}
					if (tmpList.size() > 0) {
						listCHbyIdxLoc = tmpList;
					}
				}
				//NLP.printListOfString("set to year listCHbyIdx=", listCHbyIdxLoc);
			}

			// try to pair if right aligned.
			if (listCHLinesFromGetStartGroupCHLinesMethod.size() > 0
					&& (listCHbyIdxLoc == null || listCHbyIdxLoc.size() == 0)) {
				List<String> tmpListCHbyIdxLoc = new ArrayList<String>();
				// System.xut.println("ast resort -listCHLinesFromGetStartGroupCHLinesMethod");
				//NLP.printListOfString(listCHLinesFromGetStartGroupCHLinesMethod);
				String chOnLine = "";
				boolean matched = false;
				Map<Integer, String> mapRightAligned = new TreeMap<Integer, String>();
				Map<Integer, String> mapFinalRightAligned = new TreeMap<Integer, String>();

				Pattern patternChOnLine = Pattern.compile("([a-zA-Z]{1,30} [\\d,]{2,3}|[\\d]{4})(?=[ ]{2,}|$)");
				Matcher matchChOnLine;
				for (int d = 0; d < listCHLinesFromGetStartGroupCHLinesMethod.size(); d++) {
					chOnLine = listCHLinesFromGetStartGroupCHLinesMethod.get(d);
					matchChOnLine = patternChOnLine.matcher(chOnLine);
					while (matchChOnLine.find()) {
						// NLP.pwNLP.append(//NLP.printLn(
						// "rt line=",
						// d + "matchChOnLine.group()=" + matchChOnLine.group() + " eIdx=" +
						// matchChOnLine.end()));
						mapRightAligned.put((d + 1) * 1000 + matchChOnLine.end(), matchChOnLine.group());
					}
				}

				// System.xut.println("mapRightAligned.size="+mapRightAligned.size()+"\rprinting
				// mapRightAligned==");
				//NLP.printMapIntStr(mapRightAligned);
				int key = 0, key2 = 0;
				String chStr = "";
				Map<Integer, String> mapRightChs = new TreeMap<Integer, String>();

				int mapCnt = 0, mapCnt2 = 0;
				for (Map.Entry<Integer, String> entry : mapRightAligned.entrySet()) {
					chStr = "";
					key = entry.getKey();
					mapCnt = key / 1000;
					if (nlp.getAllIndexEndLocations(entry.getValue(), AllColumnHeadingPattern).size() > 0) {
						chStr = entry.getValue();
						// System.xut.println("CH Pattern found - chStr="+chStr);
					} else
						continue;
					matched = false;
					for (Map.Entry<Integer, String> entry2 : mapRightAligned.entrySet()) {
						key2 = entry2.getKey();
						mapCnt2 = key2 / 1000;
						// System.xut.println("mapCnt2="+mapCnt2+" mapCnt="+mapCnt);
						if (mapCnt2 <= mapCnt)
							continue;
						// System.xut.println("key=" + key + " key2=" + key2);
						// System.xut
						// .println("Math.max(key2, key) - Math.min(key2, key % 1000="+
						// ( (Math.max(key2, key) - Math.min(key2, key)) %
						// 1000));

						if (mapCnt2 > mapCnt && (Math.max(key2, key) - Math.min(key2, key)) % 1000 < 3) {
							if (nlp.getAllIndexEndLocations(entry2.getValue(), AllColumnHeadingPattern).size() > 0) {
								chStr = entry2.getValue() + " " + chStr;
								matched = true;
							}
						}
					}
					if (matched) {
						mapFinalRightAligned.put(key - (key / 1000) * 1000, chStr);
					}
				}

				for (Map.Entry<Integer, String> entry3 : mapFinalRightAligned.entrySet()) {
					// System.xut.println("mapFinalRightAligned key|" +
					// entry3.getKey() + "| val|"
					// + entry3.getValue() + "|");
					tmpListCHbyIdxLoc.add(entry3.getValue());
				}

				if (tmpListCHbyIdxLoc.size() == numberOfDataCols) {
					listCHbyIdxLoc = tmpListCHbyIdxLoc;
				}
				//NLP.printListOfString("rt aligned -listCHbyIdxLoc", listCHbyIdxLoc);
			}

			String edtM, perM, edt, per;
			List<String> tmpIList = new ArrayList<>();
			List<String> tmpMList = new ArrayList<>();
			List<String> tmpCHbyIdxL = new ArrayList<>();
			List<String> tmpCHbyIdxL2 = new ArrayList<>();

			boolean done = false;

			if (null != ListCHbyMidptLoc && null != listCHbyIdxLoc && ListCHbyMidptLoc.size() == listCHbyIdxLoc.size()
					&& numberOfDataCols == listCHbyIdxLoc.size()) {
				listEdtPerIdx = new ArrayList<>();
				for (int n = 0; n < listCHbyIdxLoc.size(); n++) {

					// NLP.pwNLP.append(//NLP.printLn("listCHbyIdxLoc.get(n)=",
					// listCHbyIdxLoc.get(n)));

					tmpIList = nlp.getEnddatePeriod(listCHbyIdxLoc.get(n));
					tmpMList = nlp.getEnddatePeriod(ListCHbyMidptLoc.get(n));
					edtM = tmpMList.get(0).trim();
					perM = tmpMList.get(1).trim();
					edt = tmpIList.get(0).trim();

					if (edt.length() == 8 && edt.substring(0, 1).equals("9"))
						edt = edt.replaceAll("^9[3-9]{1}-[\\d]{2}-[\\d]{2}", "19" + edt);

					if (edt.length() == 8 && edt.substring(0, 1).equals("0"))
						edt = edt.replaceAll("^9[3-9]{1}-[\\d]{2}-[\\d]{2}", "20" + edt);

					// System.xut
					// .println("before ck edtM=" + edtM + " edt=" + edt);
					if (edt.length() != 10 && edtM.length() == 10) {
						edt = edtM;
					}
					// System.xut.println("after ck edtM=" + edtM + " edt=" +
					// edt);

					per = tmpIList.get(1);
					if (!tableNameShort.toLowerCase().equals("bs")
							&& ((per.equals("0"))
									|| (!per.equals("3") && !per.equals("6") && !per.equals("9") && !per.equals("12")))
							&& (perM.equals("3") || perM.equals("6") || perM.equals("9") || perM.equals("12"))) {
						per = perM;
					}

					String[] ary = { edt, per };
					listEdtPerIdx.add(ary);
					tmpCHbyIdxL.add(listCHbyIdxLoc.get(n).replace("'", ""));
				}
				listCHbyIdxLoc = tmpCHbyIdxL;
				//NLP.printListOfString("5 listCHbyIdxLoc", listCHbyIdxLoc);

				done = true;
			}

			// System.xut.println("1 done==="+done);

			// if both same size - compare as above. If listCHbyIdxLoc is
			// correct size below isn't true. If listCHbyIdxLoc is wrong and
			// listCHbyMidptLoc is correct size - reset listCHbyIdxLoc

			if (!done && (null == listCHbyIdxLoc || listCHbyIdxLoc.size() != numberOfDataCols)
					&& (null != ListCHbyMidptLoc && ListCHbyMidptLoc.size() == numberOfDataCols)) {
				listCHbyIdxLoc = ListCHbyMidptLoc;
				//NLP.printListOfString("6 listCHbyIdxLoc", listCHbyIdxLoc);
			}

			if (null != listCHbyIdxLoc && !done && numberOfDataCols == listCHbyIdxLoc.size()) {

				listEdtPerIdx = new ArrayList<>();
				for (int n = 0; n < listCHbyIdxLoc.size(); n++) {
					// System.xut
					// .println("**************" + listCHbyIdxLoc.size());
					// NLP.pwNLP.append(//NLP.printLn("listCHbyIdxLoc.get(n)==",
					// listCHbyIdxLoc.get(n)));
					if (null == listCHbyIdxLoc.get(n))
						continue;
					tmpIList = nlp.getEnddatePeriod(listCHbyIdxLoc.get(n));
					edt = tmpIList.get(0).trim();
					per = tmpIList.get(1).trim();
					String[] ary = { edt, per };
					// NLP.pwNLP.append(//NLP.printLn("A. adding edt=", edt));

					listEdtPerIdx.add(ary);
					tmpCHbyIdxL2.add(listCHbyIdxLoc.get(n).replace("'", ""));
				}

				listCHbyIdxLoc = tmpCHbyIdxL2;
				//NLP.printListOfString("7 listCHbyIdxLoc", listCHbyIdxLoc);
				//NLP.printListOfStringArray("7 listEdtPerIdx", listEdtPerIdx);

			}

			if (listCHbyIdxLoc != null && listCHbyIdxLoc.size() != numberOfDataCols) {
				listCHbyIdxLoc = null;
			}

			listCHbyIdxLoc = tmpCHbyIdxL2;

			//NLP.printListOfString("8 listCHbyIdxLoc", listCHbyIdxLoc);

			@SuppressWarnings("unused")
			int mp, eIdx;

			if (hasTabs) {
				endOfSingleIterationOfLoop = dataTableStartIdx;
			} else {
				endOfSingleIterationOfLoop = dataTableStartIdx + +tableText.length();
			}
			// NLP.pwNLP.append(//NLP.printLn("last endOfSingleIterationOfLoop=",
			// endOfSingleIterationOfLoop + ""));

			// NLP.pwNLP.append(//NLP.printLn("at getDataMap. redoMap=", redoMap + ""));
			getDataMap(tableText, redoMap, tableTextLoop2);

		}

		endTime = System.currentTimeMillis();
		duration = (endTime - startT) / 1000;

		fileSize = fileSize / 1000;
		// System.xut.println("fileSize (kb)=" + fileSize+ " total duration="+duration);
		// NLP.pwNLP.close();

		return tableCount;

	}

	public void fixTooManyColumnHeadings(int numberOfDataCols) throws IOException {

		NLP nlp = new NLP();

		// I recreate mapAllCH2 by cloning it to mapAllCH3 created by this
		// routine.

		String[] startGroupSplitLastLine = startGroup.split("[\r\n]");
		String lastLineOfStartGroup = startGroupSplitLastLine[startGroupSplitLastLine.length - 1];
		String[] lastLineOfStartGroupSplitBy3Ws = lastLineOfStartGroup.split("(   |\t)(?=[\\p{Alnum}\\p{Punct}])");
		String startGroupWithOutLastLine = "";
		for (int i = 0; i + 1 < startGroup.split("[\r\n]").length; i++) {
			startGroupWithOutLastLine = startGroupWithOutLastLine + "xx" + startGroup.split("[\r\n]")[i];
		}

		startGroupWithOutLastLine = startGroupWithOutLastLine.replaceAll("[x]{2,}", "\r");
		// NLP.pwNLP.append(//NLP.printLn("a startGroupWithOutLastLine=",
		// startGroupWithOutLastLine));
		int rowNameEidxReCalibrated = 0;
		boolean removeIfMatchPriorToEidxOfRowAboveCH = false;
		if (numberOfDataCols == lastLineOfStartGroupSplitBy3Ws.length - 1) {
			if (rownameEIdx < lastLineOfStartGroupSplitBy3Ws[0].length() - 3) {
				// sometimes the CHs have CH above rowname b/c rownameEidx is
				// off a litte - here I regrab last row of startGroup and see if
				// number of data cols equals number of splits by 3ws and then
				// if then if the end of str above rowname (first match) is more
				// than rownameEidx and if so use it to remove excess CHs

				// NLP.pwNLP.append(//NLP.printLn("lastLineOfStartGroup", lastLineOfStartGroup));
				// NLP.pwNLP.append(//NLP.printLn("lastLineOfStartGroupSplitBy3Ws.len=",
				// lastLineOfStartGroupSplitBy3Ws.length + "\rarray of
				// lastLineOfStartGroupSplitBy3Ws="
				// + Arrays.toString(lastLineOfStartGroupSplitBy3Ws)));

				removeIfMatchPriorToEidxOfRowAboveCH = true;
				rowNameEidxReCalibrated = lastLineOfStartGroupSplitBy3Ws[0].length() - 3;
				if (!tableSentence.contains(lastLineOfStartGroupSplitBy3Ws[0])) {
					tableSentenceLastLine = lastLineOfStartGroupSplitBy3Ws[0];
					startGroupWithOutLastLine = startGroupWithOutLastLine + "\r" + tableSentenceLastLine;
					list_TS_tsShort_tsPattern_tnValid = NLP.parseTableSentence(startGroupWithOutLastLine,
							companyName.substring(0, Math.min(7, companyName.length())), true, tableNameLong, false);
					tableSentence = list_TS_tsShort_tsPattern_tnValid.get(0);
					tsShort = list_TS_tsShort_tsPattern_tnValid.get(1);
					tsPattern = list_TS_tsShort_tsPattern_tnValid.get(2)
							.replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)", "");
					// NLP.pwNLP.append(//NLP.printLn("tsShort = ", tsShort + "\r tableSentence=" +
					// tableSentence));
				}
			}
		}

		TreeMap<Integer, List<String[]>> mapAllCH3 = new TreeMap<Integer, List<String[]>>();
		int key = 0, prevK = 0, kCnt = 0, edIdx;

		List<String[]> tmpLst1 = new ArrayList<String[]>();
		int numberOfCHsRatio2 = 0;
		boolean foundPeriod = false, removedPeriod = false;
		// mapALLCH2, k=ch row,val[0]=eidx,val[1]=CH,val[2]=raw row
		for (Map.Entry<Integer, List<String[]>> entry : mapAllCH2.entrySet()) {
			List<String[]> tmpLst2 = new ArrayList<String[]>();
			key = entry.getKey();
			tmpLst1 = entry.getValue();
			for (int c = 0; c < tmpLst1.size(); c++) {
				edIdx = Integer.parseInt(tmpLst1.get(c)[0]);
				// unlikely a ch b/c it is long and ends very close to
				// end of rowname
				// NLP.pwNLP.append(//NLP.printLn("tmpLst1.get(c)[1]=", tmpLst1.get(c)[1] + "
				// rownameEidx=" + rownameEIdx
				// + " edIdx=" + edIdx + " tmpLst1.get(c)[1].length()=" +
				// tmpLst1.get(c)[1].length()));

				if ((Math.abs(rownameEIdx - edIdx) < 5 && tmpLst1.get(c)[1].length() > 20
						|| (removeIfMatchPriorToEidxOfRowAboveCH && edIdx <= rowNameEidxReCalibrated))
						|| (rownameEIdx > edIdx && tmpLst1.get(c)[1].length() > 14)) {

					if (nlp.getAllIndexEndLocations(tmpLst1.get(c)[1], tp.EndedHeadingTablesentencePattern)
							.size() > 0) {
						removedPeriod = true;
					}
					continue;
				}
				// NLP.pwNLP.append(//NLP.printLn("rownameEIdx - edIdx=",
				// (rownameEIdx - edIdx) + " tmpLst1.get(c)[1]=" + tmpLst1.get(c)[1]));
				if (Math.abs(rownameEIdx - edIdx) < 19 && tmpLst1.get(c)[1].toUpperCase().contains("NOTE")) {
					// NLP.pwNLP.append(//NLP.printLn("remove note ch=", tmpLst1.get(c)[1]));
					// noteInCh = true;
					continue;
				}
				// System.xut.println("tmpLst1.get(c)[1]=="+tmpLst1.get(c)[1]);
				if (tmpLst1.get(c)[1].replaceAll("[ ]{1,}", "").length() > 0) {
					tmpLst2.add(tmpLst1.get(c));
					if (nlp.getAllIndexEndLocations(tmpLst1.get(c)[1], tp.EndedHeadingTablesentencePattern)
							.size() > 0) {
						foundPeriod = true;
					}
				}
			}

			if (numberOfCHsRatio2 < tmpLst2.size())
				numberOfCHsRatio2 = tmpLst2.size();

			numberOfCHsRatio = numberOfCHsRatio2;
			mapAllCH3.put(key, tmpLst2);
		}

		// NLP.pwNLP.append(//NLP.printLn("foundPeriod=", foundPeriod + " removedPeriod="
		// + removedPeriod));
		if (foundPeriod && removedPeriod) {
			tsShort = "zz";
			// System.xut.println("9 tsShort=" + tsShort);
			for (Map.Entry<Integer, List<String[]>> entry : mapAllCH2.entrySet()) {
				List<String[]> tmpLst2 = new ArrayList<String[]>();
				key = entry.getKey();
				tmpLst1 = entry.getValue();
				for (int c = 0; c < tmpLst1.size(); c++) {
					edIdx = Integer.parseInt(tmpLst1.get(c)[0]);
					// unlikely a ch b/c it is long and ends very close
					// to end of rowname
					// NLP.pwNLP.append(//NLP.printLn("tmpLst1.get(c)[1]=", tmpLst1.get(c)[1] + "
					// rownameEidx=" + rownameEIdx
					// + " edIdx=" + edIdx + " tmpLst1.get(c)[1].length()=" +
					// tmpLst1.get(c)[1].length()));

					if ((Math.abs(rownameEIdx - edIdx) < 5 && tmpLst1.get(c)[1].length() > 20)
							|| (rownameEIdx > edIdx && tmpLst1.get(c)[1].length() > 14)) {
						continue;
					}
					if (Math.abs(rownameEIdx - edIdx) < 15 && tmpLst1.get(c)[1].toUpperCase().contains("NOTE")) {
						continue;
					}
					// System.xut.println("tmpLst1.get(c)[1]=="+tmpLst1.get(c)[1]);
					if (tmpLst1.get(c)[1].replaceAll("[ ]{1,}", "").length() > 0
							&& nlp.getAllIndexEndLocations(tmpLst1.get(c)[1], tp.EndedHeadingTablesentencePattern)
									.size() < 1) {
						tmpLst2.add(tmpLst1.get(c));
					}
				}

				if (numberOfCHsRatio2 < tmpLst2.size())
					numberOfCHsRatio2 = tmpLst2.size();

				numberOfCHsRatio = numberOfCHsRatio2;
				mapAllCH3.put(key, tmpLst2);
			}
		}

		//NLP.printMapIntListOfStringAry("mapAllCH3", mapAllCH3);
		mapAllCH2 = new TreeMap<Integer, List<String[]>>();
		mapAllCH = new TreeMap<Integer, List<String[]>>();
		mapAllCH2 = mapAllCH3;
		mapAllCH = mapAllCH3;
		//NLP.printMapIntListOfStringAry("first adjustment at final for fixTooManyColumnHeadings - mapAllCH3", mapAllCH3);

		// System.xut.println("numberOfCHsRatio2=" + numberOfCHsRatio2);

		Map<Integer, Integer[]> mapDataColIdxLocs2 = new TreeMap<Integer, Integer[]>();

		// clean-up of mapDataColIdxLocs (don't increase base <3 (minus)
		key = 0;
		prevK = 0;
		kCnt = 0;
		Integer[] tmpIAry, tmpIAryPrev = null;
		for (Map.Entry<Integer, Integer[]> entry : mapDataColIdxLocs.entrySet()) {
			kCnt++;
			key = entry.getKey();
			tmpIAry = entry.getValue();
			// if (kCnt > 1) {
			// System.xut.println("kCnt=" + kCnt + "(key-prevK)" +
			// (Math.abs(key - prevK)) + " tmpIAry[0]="
			// + tmpIAry[0] + " tmpIAryPrev[0]=" + tmpIAryPrev[0] +
			// " key=" + key + " prevK=" + prevK);
			// }
			// see other instance I filter by 7 (search filter by ) -
			// (so don't change this)
			if (kCnt > 1 && Math.abs(key - prevK) < 5 && tmpIAry[0] >= 7 && tmpIAryPrev[0] >= 7) {
				// System.out.println("not adding this key - b/c same col counted twice=" +
				// key);
				continue;
			} else
				mapDataColIdxLocs2.put(key, tmpIAry);
			prevK = key;
			tmpIAryPrev = tmpIAry;
		}
		mapDataColIdxLocs.clear();
		mapDataColIdxLocs = mapDataColIdxLocs2;

		//NLP.printMapIntIntAry("revised -- mapDataColIdxLocs", mapDataColIdxLocs);
		numberOfCHsRatio = numberOfCHsRatio2;
		// NLP.pwNLP.append(//NLP.printLn("revised numberOfCHsRatio=",
		// "" + numberOfCHsRatio + " numberOfDataCols=" + numberOfDataCols));

		// if not yet fix - try this
		if (numberOfCHsRatio > numberOfDataCols) {
			// NLP.pwNLP.append(//NLP.printLn("still too many CHs - numberOfCHsRatio=",
			// numberOfCHsRatio + " noOfDataCols=" + numberOfDataCols));
			//NLP.printMapIntListOfStringAry("zzz mapAllCH", mapAllCH);

			// this sees if one of the CH (each string in list is a CH on
			// that row) has an endIdx prior to rownameEndIdx and removes
			// it. Then resets numberOfCHsratio
			TreeMap<Integer, List<String[]>> mapAllCHtmp = new TreeMap<Integer, List<String[]>>();
			List<String[]> tmpLst = new ArrayList<>();
			key = -1;
			int chEndIdx, chStartIdx, cntCh = 0;
			String ch = "", tmpTSshort = "";
			// each String[] in list is CH - endidx and CH text.
			for (Map.Entry<Integer, List<String[]>> entry : mapAllCH.entrySet()) {
				key = entry.getKey();
				tmpLst = new ArrayList<>();

				for (int b = 0; b < entry.getValue().size(); b++) {
					if (b + 1 > cntCh) {
						cntCh++;
					}

					ch = entry.getValue().get(b)[1];
					chEndIdx = Integer.parseInt(entry.getValue().get(b)[0]);
					chStartIdx = Integer.parseInt(entry.getValue().get(b)[0]) - ch.length();

					tmpTSshort = NLP.parseTableSentence(ch, "", true, "", false).get(1);
					// System.xut.println("tmpTSshort="+tmpTSshort);
					if (chEndIdx < (rownameEIdx - 5)
							|| (ch.length() > 3 && ch.toLowerCase().trim().substring(0, 4).equals("note")
									|| (chEndIdx < sIdxC1NumOfCsLastCeIdx.get(0)[0]
											&& (tmpTSshort.equals("PM") || tmpTSshort.equals("M"))))
							// if ch start is 40 less than rowname and
							// rownameEidx close to chEndIdx. B/c long above
							// rowname ch can stretch base rownameEidx.
							|| (chEndIdx - rownameEIdx < 8 && (chStartIdx + 35) < rownameEIdx)
							// if ch len >40 and starts well bef rownameEIdx
							// it can't really be a ch.
							|| (chEndIdx - rownameEIdx < 15 && (chStartIdx + 35) < rownameEIdx
									&& entry.getValue().get(b)[1].length() > 40)
							|| (chEndIdx - rownameEIdx < 5 && nlp
									.getAllIndexEndLocations(ch, Pattern.compile("[12]{1}[09]{1}[0-9]{2}")).size() == 0)
									&& ch.length() > 7) {

						// this should work for all above b/c they are all ch
						// above rowname except for NOTE below
						if (ch.trim().length() > 3 && !ch.toLowerCase().trim().substring(0, 4).equals("note")) {
							list_TS_tsShort_tsPattern_tnValid = NLP.parseTableSentence(ch, "", true, "", false);
							tableSentence = list_TS_tsShort_tsPattern_tnValid.get(0);
							tsShort = list_TS_tsShort_tsPattern_tnValid.get(1);
							tsPattern = list_TS_tsShort_tsPattern_tnValid.get(2);
						}
						// NLP.pwNLP.append(//NLP.printLn("rownameEndIdx=",
						// rownameEIdx + " will not add==" + Arrays.toString(entry.getValue().get(b))
						// + " \rtableSentence=" + tableSentence + " \rtsShort=" + tsShort));

						cntCh--;
						continue;

					} else {
						// NLP.pwNLP.append(//NLP.printLn(
						// "rownameEndIdx=",
						// rownameEIdx + " adding to list==" +
						// Arrays.toString(entry.getValue().get(b))));
						tmpLst.add(entry.getValue().get(b));
					}
				}
				//NLP.printListOfStringArray("adding to mapAllCHTmp", tmpLst);
				mapAllCHtmp.put(key, tmpLst);
			}

			//NLP.printMapIntListOfStringAry("zzz mapAllCHtmp", mapAllCHtmp);

			mapAllCH = mapAllCHtmp;
			mapAllCH2 = mapAllCHtmp;
			numberOfCHsRatio = cntCh;
		}

		//NLP.printMapIntIntAry("revised -- mapDataColIdxLocs", mapDataColIdxLocs);
		numberOfCHsRatio = numberOfCHsRatio2;
		// NLP.pwNLP.append(//NLP.printLn("revised numberOfCHsRatio=",
		// "" + numberOfCHsRatio + " numberOfDataCols=" + numberOfDataCols));

		//NLP.printMapIntListOfStringAry("final for fixTooManyColumnHeadings -
		// mapAllCH3", mapAllCH3);

	}

	public static List<String> getCHbyMidptLoc(Map<Integer, List<String[]>> mapAllChLines)
			throws SQLException, IOException {

		NLP nlp = new NLP();

		// System.xut.println("1. getCHbyMidPtLoc runAll=" + runAll);
		tmp2ListCHbyIdxLoc = new ArrayList<String>();

		List<String[]> listEidxChRowColno = new ArrayList<String[]>();
		List<String[]> listCHeIdxMpColno = new ArrayList<String[]>();
		List<String[]> listTmp = new ArrayList<String[]>();

		// convert map to list of String[] -- can't use maps b/c calls by
		// key and will miss those with same key as I autoincrement

		// System.xut.println("3. printing mapAllChLines");
		// {
		//NLP.printMapIntListOfStringAry(mapAllChLines);
		// }
		for (Map.Entry<Integer, List<String[]>> entry : mapAllChLines.entrySet()) {
			listTmp.addAll(entry.getValue());
		}

		// System.xut.println("runAll="+runAll+" ch by midpoint loc listTmp");
		//NLP.printListOfStringArray(listTmp);

		// get and add colNo -- and determine which is largest row. Creates
		// list: listEidxChRowColno[0=eIdx][1=chRow][2=colNo]. By default
		// runAll=true so this runs. But if getChByIdxLoc failed due to a
		// remainder - we will skip this and go right to getLast/First CH given
		// the table CHs are likley difficult to parse accurately.
		if (runAll) {

			@SuppressWarnings("unused")
			int rw = 0, pRw = -1, colNo = 0, pColNo = 0, largestRow = 0, largestColnumber = -1;
			for (int i = 0; i < listTmp.size(); i++) {
				rw = Integer.parseInt(listTmp.get(i)[2]);
				if (rw != pRw)
					colNo = 1;
				else
					colNo++;
				String[] ary = { listTmp.get(i)[0], listTmp.get(i)[1], listTmp.get(i)[2], colNo + "" };
				// this now has eIdx,col txt and row#
				listEidxChRowColno.add(ary);
				if (colNo > pColNo) {
					// to determine row w/ most col - find largest col# and then
					// record row.
					largestRow = Integer.parseInt(listTmp.get(i)[2]);
					largestColnumber = colNo;
				}
				pRw = rw;
			}
			// largest row can be used as method to pair and keep colNos
			// System.xut.println("row with most CHs - largestRow=" +
			// largestRow);
			List<String[]> listEidxChRowColno2 = listEidxChRowColno;
			// System.xut.println("listEidxChRowColno (colNo raw count - eIdx will show true
			// colNo loc");
			//NLP.printListOfStringArray(listEidxChRowColno);

			// if at largest row (largestRow=row) then insert colNo, else
			// colNo=0.
			// if colNo=0, remove.
			@SuppressWarnings("unused")
			int row, row2 = 0, prevRow2 = 0, cnt;
			boolean hasNegative = false;
			double mp, mp2, eIdx, eIdx2, eIdxMax = 0, prevMaxEIdx = 0;
			String cTxt, cTxt2, cTxtPaired = "", colnumber = "-1";
			for (int i = 0; i < listEidxChRowColno.size(); i++) {
				colnumber = "-1";
				eIdx = Double.parseDouble(listEidxChRowColno.get(i)[0]);
				mp = (eIdx - listEidxChRowColno.get(i)[1].length() / 2);
				cTxt = listEidxChRowColno.get(i)[1];
				row = Integer.parseInt(listEidxChRowColno.get(i)[2]);
				if (row == largestRow)
					colnumber = listEidxChRowColno.get(i)[3];
				// System.xut.println("cTxt=" + cTxt + " mp=" + mp + " row=" +
				// row
				// + " eIdx=" + eIdx);
				if (i + 1 == listEidxChRowColno.size())
					break;

				cnt = 0;
				prevRow2 = 0;
				cTxtPaired = cTxt;
				for (int c = (i + 1); c < listEidxChRowColno2.size(); c++) {
					eIdx2 = Double.parseDouble(listEidxChRowColno2.get(c)[0]);
					mp2 = (eIdx2 - listEidxChRowColno2.get(c)[1].length() / 2);
					cTxt2 = listEidxChRowColno2.get(c)[1];
					row2 = Integer.parseInt(listEidxChRowColno2.get(c)[2]);
					// System.xut.println("cTxt2=" + cTxt2 + " mp2=" + mp2
					// + " row2=" + row2 + " eIdx2=" + eIdx2 + " cTxt="
					// + cTxt + " mp=" + mp + " row=" + row + " eIdx="
					// + eIdx + " prevRow2=" + prevRow2);

					if (prevRow2 != row2)
						prevMaxEIdx = 0;

					if (row < row2 && (Math.abs(mp - mp2) <= 3 || Math.abs(eIdx - eIdx2) <= 3)) {
						cTxtPaired = (cTxtPaired + " " + cTxt2).trim();
						eIdxMax = Math.max(eIdx, eIdx2);
						eIdxMax = Math.max(eIdxMax, prevMaxEIdx);
						prevMaxEIdx = eIdxMax;

						// System.xut.println("largestRow=" + largestRow
						// + " row2=" + row2);
						if (row2 == largestRow)
							colnumber = listEidxChRowColno.get(c)[3];

						listEidxChRowColno2.remove(c);
						c = c - 1;
						// System.xut
						// .println(" row<row2 cTxtPaired=" + cTxtPaired);
					}

					if ((c + 1) == listEidxChRowColno2.size() || (c + 1) > listEidxChRowColno2.size()) {
						hasNegative = false;
						String[] ary = { cTxtPaired, eIdxMax + "", mp2 + "", colnumber };
						listCHeIdxMpColno.add(ary);
						if (colnumber.contains("-1")) {
							hasNegative = true;
						}
						cTxtPaired = "";
						cTxt = "";
						cTxt2 = "";
						// System.xut.println("end prevRow2>row2 ary="
						// + Arrays.toString(ary) + " row2=" + row2
						// + " prevRow2=" + prevRow2 + " colnumber="
						// + colnumber);
					}
					cnt++;
				}
				prevRow2 = row2;
			}

			/*
			 * list of paired columns below: contains max eIdx of all columns paired and the
			 * mp it is centered over and the colnumber based on the row with max
			 * colnumbers. if colnumber = -1 -- discard col ==> b/c each match is paired if
			 * each col is centered over each other no filter necessary for duplicate
			 * yr/mo/end. If I try to always grab 1st and last col I will need to not pair
			 * if there are duplicate yr/mo/end
			 */

			// System.xut.println("no of data cols="
			// + sIdxC1NumOfCsLastCeIdx.get(0)[1]
			// + " listCHeIdxMpColNo.size=" + listCHeIdxMpColno.size()
			// + "hasNegative=" + hasNegative
			// + " 1.printing listCHeIdxMpColno");
			//NLP.printListOfStringArray(listCHeIdxMpColno);
			int cl;
			boolean reorder = false;
			// if has negative - 1 of the CHs were not paired.
			if (sIdxC1NumOfCsLastCeIdx.get(0)[1] == listCHeIdxMpColno.size() && !hasNegative) {
				for (int i = 0; i < listCHeIdxMpColno.size(); i++) {
					tmp2ListCHbyIdxLoc.add(listCHeIdxMpColno.get(i)[0]);
					cl = Integer.parseInt(listCHeIdxMpColno.get(i)[3]);
					if ((i + 1) != cl) {
						// System.xut
						// .println("ERROR - COLUMNS ARE NOT IN ORDER - STOP THE PRESSES! FIX "
						// +
						// "BY INSERTING INTO TO TREE MAP WITH LAST ARY INSTANCE AS THEY KEY - "
						// + "THEN REPRINT TO THE LIST");
						reorder = true;
						// out of order so clear idx of any additions
						tmp2ListCHbyIdxLoc = new ArrayList<String>();
					}
				}
				if (reorder) {
					// gets reorderedListOfStringAry - and orders is based on
					// the 3rd instance in the ary.
					tmp2ListCHbyIdxLoc = new ArrayList<>();
					listCHeIdxMpColno = NLP.reorderListsOfStringAry(listCHeIdxMpColno, 3,
							sIdxC1NumOfCsLastCeIdx.get(0)[1]);
					for (int i = 0; i < listCHeIdxMpColno.size(); i++) {
						tmp2ListCHbyIdxLoc.add(listCHeIdxMpColno.get(i)[0]);
						// System.xut.println("add listCHeIdxMpColno.get(i)[0]="
						// + listCHeIdxMpColno.get(i)[0]);
					}
				}
				// System.xut.println("printing reordered tmp2ListCHbyIdxLoc="
				// + tmp2ListCHbyIdxLoc);
				return tmp2ListCHbyIdxLoc;
			}
		}
		// TODO: always get first and last CH.
		// if colnumber=1 or colnumber=largestRow are not found then pair
		// first instance of a row's CH going in reverse order - ignore
		// duplicates by skipping

		// did not pair all columns (could be I paired 4 of 5 but 1 failed so
		// now I just get first and last.

		// System.xut.println("listTmp - printed forward");
		//NLP.printListOfStringArray(listTmp);

		// System.xut.println("listTmp - printed in reverse");
		//NLP.printListOfStringArrayInReverse(listTmp);

		// if listCHeIdxMpColno contains colnumber=1 and
		// colnumber=largestColnumber - then skip

		List<String[]> tmpFirstCol = new ArrayList<String[]>();
		int rw = -1;
		int pRw = -1;
		int rw2 = -1;
		int pRw2 = -1;
		int eCnt2 = 0;
		int yCnt2 = 0;
		int mCnt2 = 0;
		int eCnt = 0, yCnt = 0, mCnt = 0, a = -1;

		for (int i = (listTmp.size() - 1); i >= 0; i--) {
			rw = Integer.parseInt(listTmp.get(i)[2]);
			// System.xut.println("loop=" + i + " listTmp for firstCol ch="
			// + Arrays.toString(listTmp.get(i)) + " rw=" + rw + " pRw="
			// + pRw);
			if (rw != pRw && i != (listTmp.size() - 1)) {
				// System.xut.println("getting at a+1");
				a = (i + 1);
				// get the last
			}

			// b/c it adds entire line - need to not satisfy if condition if
			// added.

			boolean added = false;
			if (rw != pRw && i != (listTmp.size() - 1)) {
				pRw = rw;
				// System.xut.println("rw=" + rw + " pRw=" + pRw);
				// System.xut.println("checking 1st firstCol. listTmp.get(a)[1]="
				// + listTmp.get(a)[1]);
				if (eCnt == 0
						&& nlp.getAllIndexStartLocations(listTmp.get(a)[1], TableParser.EndedHeadingPattern).size() > 0
						&& !added) {
					eCnt = nlp.getAllIndexStartLocations(listTmp.get(a)[1], TableParser.EndedHeadingPattern).size();
					tmpFirstCol.add(listTmp.get(a));
					// System.xut.println("E1 added to firstCol="
					// + Arrays.toString(listTmp.get(a)));
					// continue; can't use continue -if this screws other things
					// up. use boolean condition that if prior if condition met
					// this if condition is not
				}

				if (yCnt == 0
						&& nlp.getAllIndexStartLocations(listTmp.get(a)[1], TableParser.YearOrMoDayYrPattern).size() > 0
						&& !added) {
					yCnt = nlp.getAllIndexStartLocations(listTmp.get(a)[1], TableParser.YearOrMoDayYrPattern).size();
					tmpFirstCol.add(listTmp.get(a));
					// System.xut.println("Y1 added to firstCol="
					// + Arrays.toString(listTmp.get(a)));
					// continue; can't use continue -if this screws other things
					// up. use boolean condition that if prior if condition met
					// this if condition is not
				}

				// System.xut.println("M1. mCnt=" + mCnt);
				if (mCnt == 0
						&& nlp.getAllIndexStartLocations(listTmp.get(a)[1], TableParser.MonthPatternSimple).size() > 0
						&& !added) {
					mCnt = nlp.getAllIndexStartLocations(listTmp.get(a)[1], TableParser.MonthPatternSimple).size();
					tmpFirstCol.add(listTmp.get(a));
					// System.xut.println("M1. added to firstCol="
					// + Arrays.toString(listTmp.get(a)));
					// continue; can't use continue -if this screws other things
					// up. use boolean condition that if prior if condition met
					// this if condition is not
				}
			}
			pRw = rw;
			// add last row first col

			added = false;
			if (i == 0) {
				// System.xut.println("i==0 checking 1st col listTmp.get(i)[1]="
				// + listTmp.get(i)[1]);
				if (eCnt == 0
						&& nlp.getAllIndexStartLocations(listTmp.get(i)[1], TableParser.EndedHeadingPattern).size() > 0
						&& !added) {
					eCnt = nlp.getAllIndexStartLocations(listTmp.get(i)[1], TableParser.EndedHeadingPattern).size();
					tmpFirstCol.add(listTmp.get(i));
					// System.xut.println("E@0 added to firstCol="
					// + Arrays.toString(listTmp.get(i)));
					// continue; can't use continue -if this screws other things
					// up. use boolean condition that if prior if condition met
					// this if condition is not
					added = true;
				}

				if (yCnt == 0
						&& nlp.getAllIndexStartLocations(listTmp.get(i)[1], TableParser.YearOrMoDayYrPattern).size() > 0
						&& !added) {
					yCnt = nlp.getAllIndexStartLocations(listTmp.get(i)[1], TableParser.YearOrMoDayYrPattern).size();
					tmpFirstCol.add(listTmp.get(i));
					// System.xut.println("Y@0 added to firstCol="
					// + Arrays.toString(listTmp.get(i)));
					// continue; can't use continue -if this screws other things
					// up. use boolean condition that if prior if condition met
					// this if condition is not
				}

				if (mCnt == 0
						&& nlp.getAllIndexStartLocations(listTmp.get(i)[1], TableParser.MonthPatternSimple).size() > 0
						&& !added) {
					mCnt = nlp.getAllIndexStartLocations(listTmp.get(i)[1], TableParser.MonthPatternSimple).size();
					tmpFirstCol.add(listTmp.get(i));
					// System.xut.println("M@0 added to firstCol="
					// + Arrays.toString(listTmp.get(i)));
					// continue; can't use continue -if this screws other things
					// up. use boolean condition that if prior if condition met
					// this if condition is not
				}
			}
			pRw = rw;
		}

		List<String[]> tmpLasttCol = new ArrayList<String[]>();

		for (int i = (listTmp.size() - 1); i >= 0; i--) {
			rw2 = Integer.parseInt(listTmp.get(i)[2]);
			if (rw2 != pRw2) {
				pRw2 = rw2;

				if (eCnt2 == 0 && nlp.getAllIndexStartLocations(listTmp.get(i)[1], TableParser.EndedHeadingPattern)
						.size() > 0) {
					eCnt2 = nlp.getAllIndexStartLocations(listTmp.get(i)[1], TableParser.EndedHeadingPattern).size();
					tmpLasttCol.add(listTmp.get(i));
					// System.xut.println("added to lastCol="
					// + Arrays.toString(listTmp.get(i)));
					// continue; can't use continue -if this screws other things
					// up. use boolean condition that if prior if condition met
					// this if condition is not
				}

				if (yCnt2 == 0 && nlp.getAllIndexStartLocations(listTmp.get(i)[1], TableParser.YearOrMoDayYrPattern)
						.size() > 0) {
					yCnt2 = nlp.getAllIndexStartLocations(listTmp.get(i)[1], TableParser.YearOrMoDayYrPattern).size();
					tmpLasttCol.add(listTmp.get(i));
					// System.xut.println("added to lastCol="
					// + Arrays.toString(listTmp.get(i)) + " rw2=" + rw2
					// + " pRw2=" + pRw2);
					// continue; can't use continue -if this screws other things
					// up. use boolean condition that if prior if condition met
					// this if condition is not
				}

				if (mCnt2 == 0 && nlp.getAllIndexStartLocations(listTmp.get(i)[1], TableParser.MonthPatternSimple)
						.size() > 0) {
					mCnt2 = nlp.getAllIndexStartLocations(listTmp.get(i)[1], TableParser.MonthPatternSimple).size();
					tmpLasttCol.add(listTmp.get(i));
					// System.xut.println("added to lastCol="
					// + Arrays.toString(listTmp.get(i)));
					// continue; can't use continue -if this screws other things
					// up. use boolean condition that if prior if condition met
					// this if condition is not
				}
			}
			pRw2 = rw2;
		}

		// System.xut.println("printing tmpLasttCol");
		//NLP.printListOfStringArray(tmpLasttCol);

		// gets/pairs first and last cols and requires midpoint dist
		// tolerance
		// System.xut.println("printing tmpFirstCol final");
		//NLP.printListOfStringArray(tmpFirstCol);
		// System.xut.println("|END");

		String firstCol = getFirstCol(tmpFirstCol);
		// System.xut.println("final firstCol=" + firstCol);
		// System.xut.println("printing tmpLasttCol");
		//NLP.printListOfStringArray(tmpLasttCol);
		// System.xut.println("|END");
		String lastCol = getLastColumn(tmpLasttCol);
		// System.xut.println("final lastCol=" + lastCol);
		// System.xut.println("3.listCHeIdxMpColno.size="
		// + listCHeIdxMpColno.size());

		// puts dummy list for cols b/w first and last cols
		tmp2ListCHbyIdxLoc = new ArrayList<String>();

		for (int i = 0; i < sIdxC1NumOfCsLastCeIdx.get(0)[1]; i++) {
			if (i == 0) {
				// System.xut.println("adding firstCol to tmp2ListCHbyIdxLoc="
				// + firstCol);
				tmp2ListCHbyIdxLoc.add(firstCol);
			}
			if ((i + 1) == sIdxC1NumOfCsLastCeIdx.get(0)[1]) {
				tmp2ListCHbyIdxLoc.add(lastCol);
				// System.xut.println("adding lastCol to tmp2ListCHbyIdxLoc="
				// + lastCol);

			}
			if ((i + 1) != sIdxC1NumOfCsLastCeIdx.get(0)[1] && i != 0) {
				tmp2ListCHbyIdxLoc.add("");
			}
		}

		if (null != tmp2ListCHbyIdxLoc) {
			// System.xut.println("22 tmp2ListCHbyIdxLoc");
			//NLP.printListOfString(tmp2ListCHbyIdxLoc);
		}

		return tmp2ListCHbyIdxLoc;
	}

	public static String getFirstCol(List<String[]> listFirsCol) {

		String firstCol = "", tmpCH;
		int midP, midP2 = 0, cnt = 0, sIdx = 0, sIdx2 = 0, eIdx = 0, eIdx2 = 0;
		for (int i = (listFirsCol.size() - 1); i >= 0; i--) {

			eIdx = Integer.parseInt(listFirsCol.get(i)[0]);
			midP = eIdx;
			tmpCH = listFirsCol.get(i)[1];
			sIdx = eIdx - tmpCH.length();
			midP = midP - tmpCH.length() / 2;

			// System.xut.println("firstCol -- tmpCH=" + tmpCH + " tmpCH midP="
			// + midP + " firstCol=" + firstCol + " midP2=" + midP2
			// + " sIdx|sIdx2|eIdx|eIdx2=" + "|" + sIdx + "|" + sIdx2
			// + "|" + eIdx + "|" + eIdx2 + "|");

			if (cnt == 0)
				firstCol = tmpCH;
			cnt++;

			if (cnt == 0)
				continue;

			// if CH w/ greatest eIdx is b/w 1st 2 or 3 data cols- then its a
			// match. listDataColsSIdxandEIdx.get(0)[0] = col1 sIdx,
			// listDataColsSIdxandEIdx.get(1)[1] = col2 eIdx
			if (cnt > 0 && Math.abs(midP - midP2) <= 4
					|| (listDataColsSIdxandEIdx.size() > 2 && Math.abs(
							(listDataColsSIdxandEIdx.get(0)[0] + listDataColsSIdxandEIdx.get(1)[1]) / 2 - midP2) < 4)
					|| (listDataColsSIdxandEIdx.size() > 3 && Math.abs(
							(listDataColsSIdxandEIdx.get(0)[0] + listDataColsSIdxandEIdx.get(2)[1]) / 2 - midP2) < 4)
					// if upper row (mp) has sIdx< lower row sIdx (sIdx2) and
					// eIdx > eIdx (we know if overlaps entirely). So must
					// relate to that col sIdx2<sIdx && eIdx2>eIdx is reverse in
					// lastCol b/c list is in sent through in reverse order in
					// last
					|| (sIdx2 < sIdx && eIdx2 > eIdx)) {
				firstCol = firstCol + " " + tmpCH;
				// System.xut.println("1 merged firstCol=" + firstCol);
			}
			midP2 = midP;
			sIdx2 = sIdx;
			eIdx2 = eIdx;

			cnt++;
		}

		// System.xut.println("2 merged firstCol returned=" + firstCol);
		return firstCol;
	}

	public static String getLastColumn(List<String[]> listLastCol) {

		String lastCol = "", tmpCH;
		int midP, midP2 = 0, sIdx = 0, eIdx = 0, sIdx2 = 0, eIdx2 = 0, cnt = 0, listSize = 0;
		for (int i = 0; i < listLastCol.size(); i++) {
			eIdx = Integer.parseInt(listLastCol.get(i)[0]);
			midP = eIdx;
			tmpCH = listLastCol.get(i)[1];
			sIdx = eIdx - tmpCH.length();
			midP = midP - tmpCH.length() / 2;
			// System.xut.println("lastcol -- tmpCH=" + tmpCH + " tmpCH midP="
			// + midP + " lastCol=" + lastCol + " midP2=" + midP2
			// + " sIdx|sIdx2|eIdx|eIdx2=" + "|" + sIdx + "|" + sIdx2
			// + "|" + eIdx + "|" + eIdx2 + "|");
			if (cnt == 0)
				lastCol = tmpCH;
			cnt++;

			if (cnt == 0)
				continue;

			// or if col to match is b/w last Col's eIdx & immediatley preceding
			// col's sIdx - match it
			if (listDataColsSIdxandEIdx.size() > 0)
				listSize = listDataColsSIdxandEIdx.size();
			if (cnt > 0 && Math.abs(midP - midP2) < 4 || (listDataColsSIdxandEIdx.size() > 2 && Math.abs(
					(listDataColsSIdxandEIdx.get(listSize - 1)[1] + listDataColsSIdxandEIdx.get(listSize - 2)[0]) / 2
							- midP2) < 4)
					|| (listDataColsSIdxandEIdx.size() > 2 && Math.abs((listDataColsSIdxandEIdx.get(listSize - 1)[1]
							+ listDataColsSIdxandEIdx.get(listSize - 2)[0]) / 2 - midP) < 4)
					// if upper row (mp) has sIdx< lower row sIdx (sIdx2) and
					// eIdx >
					// eIdx (we know if overlaps entirely). So must relate to
					// that col
					|| (sIdx < sIdx2 && eIdx > eIdx2)) {
				lastCol = tmpCH + " " + lastCol;
				// System.xut.println("lastCol merged w/ 1 row CH=" + lastCol);
			}
			midP2 = midP;
			eIdx2 = eIdx;
			sIdx2 = sIdx;
			cnt++;
		}

		return lastCol;
	}

	public List<String[]> getMergedEndDates(List<String> CHs, List<String[]> periodEnddateFromTableSentences)
			throws ParseException, IOException {
		List<String[]> finalEndDatesPeriod = new ArrayList<String[]>();
		List<String[]> colHdgEnddatePeriod = new ArrayList<String[]>();
		List<String> colHdgs = new ArrayList<String>();

		NLP nlp = new NLP();

		// System.xut.println("13 print CH at getMergedEnddate=");
		//NLP.printListOfString(CHs);
		// System.xut.println("13 print CH at periodEnddateFromTableSentences=");
		//NLP.printListOfStringArray(periodEnddateFromTableSentences);

		// System.xut.println("periodEnddateFromTableSentences.size="
		// + periodEnddateFromTableSentences.size());
		// System.xut.println("periodEnddateFromTableSentences.get(0)[0]="+periodEnddateFromTableSentences.get(0)[1]);
		// this will form list of just colHdg texts by ignoring blank col hdgs.
		String colHdg;
		if (null == CHs || CHs.size() < 1)
			return colHdgEnddatePeriod;

		for (int i = 0; i < CHs.size(); i++) {
			colHdg = CHs.get(i);
			// System.xut.println("colHdg#=" + i + " colHdg=" + colHdg);
			if (colHdg != null && colHdg.length() > 0
					&& nlp.getAllIndexStartLocations(colHdg, AllColumnHeadingPattern).size() > 0) {
				colHdg = colHdg.replaceAll(" 0(?=\\d )", " ");
				// System.xut.println("colHdgs that are not blank=" + colHdg);
				colHdgs = nlp.getEnddatePeriod(" " + colHdg);
				// System.xut.println("colHdgs not blank edt="
				// + colHdgs.get(0).replaceAll("--", "") + " per="
				// + colHdgs.get(1));
				// if only month in ts, and only yr in col - and ts mo to each
				// col
				if (periodEnddateFromTableSentences.size() == 1
						&& periodEnddateFromTableSentences.get(0)[1].replaceAll("-", "").length() == 4
						&& colHdgs.get(0).replaceAll("--", "").length() == 4
						&& colHdgs.get(0).replaceAll("[1-2]{1}[9,0]{1}[0-9]{2}[-]{1,2}", "").length() == 0) {
					// System.xut.println("holly shit");
					String[] ary = { colHdgs.get(0).replaceAll("--", "") + periodEnddateFromTableSentences.get(0)[1],
							colHdgs.get(1) };
					colHdgEnddatePeriod.add(ary);
				}
				// only add if edt is not 0
				else if (colHdgs.get(0).replaceAll("--", "").length() > 0) {
					String[] ary = { colHdgs.get(0).replaceAll("--", ""), colHdgs.get(1) };
					// System.xut.println("adding to colHdgEnddatePeriod edt:"
					// + ary[0] + " per:" + ary[1]);
					colHdgEnddatePeriod.add(ary);
				}

				else if (colHdgs.size() > 0 && periodEnddateFromTableSentences.size() > 0 && null != colHdgs.get(0)
						&& null != periodEnddateFromTableSentences.get(0)[1] && null != colHdgs.get(1)
						&& colHdgs.get(0).replaceAll("--", "").length() == 0 && colHdgs.get(1).length() > 0
						&& nlp.specialTableSentencePatternOneEndDateManyPeriods) {
					// System.xut.println("adding to colHdgEnddatePeriod edt:"
					// + periodEnddateFromTableSentences.get(0)[1]);
					// System.xut.println(" per:" + colHdgs.get(1));
					String[] ary = { periodEnddateFromTableSentences.get(0)[1], colHdgs.get(1) };
					// System.xut.println("adding to colHdgEnddatePeriod edt:"
					// + ary[0] + " per:" + ary[1]);
					colHdgEnddatePeriod.add(ary);
				}

			}

			// if for a given column nothing is added - I automatically insert
			// whatever was captured as the CH but couldn't otherwise get both
			// a conversoin to edt and period. This prevents loss of a CH (due
			// to failure to add it to list) and thereby corrupting table b/c CH
			// is no longer aligned with data col.

			if (colHdgEnddatePeriod.size() - 1 != i) {
				// System.xut
				// .println("holly crap - i am not adding all the columns just b/c i can't find
				// a frigging match. how stupid is that. Keep structure just stuff what you
				// can't match!");
				colHdg = CHs.get(i);
				colHdgs = nlp.getEnddatePeriod(" " + colHdg);
				// System.xut.println("colHdg="+colHdg);
				//NLP.printListOfString(colHdgs);
				String[] ary = { colHdgs.get(0), colHdgs.get(1) };
				colHdgEnddatePeriod.add(ary);
			}
		}

		// if colhdgs from tbl are blank then grab entire tbl sentence
		// enddate/period if # of lists in tblsentence edt/per = # of cols and
		// data is otherwise complete in tblsent

		// if tblSent edt is blank - skip.
		// if no year in chdg - we should skip entirely.

		// System.xut.println("periodEnddateFromTableSentences.size()="
		// + periodEnddateFromTableSentences.size());
		// System.xut.println("colHdgEnddatePeriod.size()="
		// + colHdgEnddatePeriod.size());

		// TODO: if periodEnddateFromTableSentence == null
		if (null == periodEnddateFromTableSentences || null == colHdgEnddatePeriod
				|| periodEnddateFromTableSentences.size() == 0 || colHdgEnddatePeriod.size() == 0
				|| colHdgEnddatePeriod.size() / periodEnddateFromTableSentences.size() < 1) {
			return colHdgEnddatePeriod;
		}

		boolean goodChPer = true;
		boolean goodChEdt = true;
		int w = 0;
		// if colhdg edt length is not 10 or if table is is/cf and period not
		// betwee 3 and 12 then bad col hdg.

		// System.xut.println("colHdgEnddatePeriod not returned");
		int z;
		for (z = 0; z < colHdgEnddatePeriod.size(); z++) {
			w = Integer.parseInt(colHdgEnddatePeriod.get(z)[1]);
			// System.xut.println("period from chEdt==" + w);
			// System.xut.println("edt from chEdt=="
			// + colHdgEnddatePeriod.get(z)[0]);
			if (colHdgEnddatePeriod.get(z)[0].trim().length() != 10) {
				goodChEdt = false;
				// System.xut.println(" badCh. chEdt.len="
				// + colHdgEnddatePeriod.get(z)[0]
				// + " need to get edt from ts");
			}
			if ((w < 2 || w > 13) && (tableNameShort == "is" || tableNameShort == "cf")) {
				goodChPer = false;
				// System.xut.println(" bad chPer="
				// + colHdgEnddatePeriod.get(z)[1].trim()
				// + " tableNameShort=" + tableNameShort
				// + " need to get per from ts");
			}
		}

		// if goodCh return good col hdgs.
		if (goodChEdt && goodChPer)
			return colHdgEnddatePeriod;

		int tsCnt = periodEnddateFromTableSentences.size(), chCnt = colHdgEnddatePeriod.size(), ts;
		// System.xut.println("c/t=" + chCnt / tsCnt + " c=" + chCnt + ", t="
		// + tsCnt + ", c % t=" + chCnt % tsCnt);

		// for (int i = 0; i < periodEnddateFromTableSentences.size(); i++) {
		// System.xut.println("ts Edt="
		// + periodEnddateFromTableSentences.get(i)[1] + " tsPer="
		// + periodEnddateFromTableSentences.get(i)[0]
		// + " tableNameShort=" + tableNameShort);
		// }

		// assume # of cols is greater than # of tbl sentence enddates found and
		// no remainder
		int colNumber = 0;
		// if there is a remainder then return
		if (chCnt / tsCnt < 1 || chCnt % tsCnt != 0) {
			// System.xut.println("remainder chCnt/tsCnt");
			return colHdgEnddatePeriod;
		}

		if (chCnt / tsCnt >= 1 && chCnt % tsCnt == 0) {
			// System.xut.println("checking tablesentence");
			// i=particular enddates in table sentence to be assigned. B/c there
			// are fewer enddates then total col to assign them to - I assign
			// each endate to 1 or more cols. The number of cols each ts edt is
			// assigned to is equal to (total cols)/(ts enddates). I then create
			// a
			// loop equal to each number ts edt assigning then each to a number
			// of cols equal to the (total cols)/(number of ts edts) and for
			// each assignment increasing the col no (colno).

			// difficulty here will be when I get periods from table sentence
			// but only get 1 and there are 2.
			for (int i = 0; i < tsCnt; i++) {
				for (int x = 0; x < chCnt / tsCnt; x++) {
					// append small col hdgs (i)
					ts = i;

					// System.xut.println(" periodEnddateFromTableSentences.size="
					// + periodEnddateFromTableSentences.size() + " ts="
					// + ts + " chCnt=" + chCnt + " loop=" + i);
					String chEdt = colHdgEnddatePeriod.get(colNumber)[0];
					String tsEdt = periodEnddateFromTableSentences.get(ts)[1];
					// System.xut.println("tsEdt=" + tsEdt + " chEdt=" + chEdt);

					String edt = "";

					// should identify what part of chEdt is present and
					// what part of tsEdt is present then join
					// accordingly IF chEdt!=10.

					// ischEdt year-mo, mo-day or year
					// isTsEdt year-mo, mo-day or year or year-mo-day

					// System.xut.println("tsEdt="+tsEdt+ " chEdt="+chEdt);
					if (chEdt.trim().length() != 10 && null != chEdt && chEdt.length() > 0 && null != tsEdt
							&& tsEdt.replaceAll("-", "").length() > 0) {
						Pattern yearPtrn = Pattern.compile("[1-2]{1}[09]{1}[0-9]{2}");
						Matcher yearMatch = yearPtrn.matcher(chEdt);
						// if just yr in chEdt append mo-dy of tsEdt
						if (yearMatch.find() && chEdt.trim().length() == 4
								&& (tsEdt.length() == 10 || tsEdt.length() == 6)) {
							// if chEdt is just year grabs mo-day from tsEdt.

							edt = chEdt + tsEdt.substring(tsEdt.length() - 6, tsEdt.length());
							// System.xut
							// .println("chEdt has just year="
							// + chEdt
							// + " after appending mo-dy of ts edt="
							// + edt);
						}

						else if (chEdt.trim().length() != 10 && tsEdt.length() > (10 - chEdt.length())
								&& chEdt.length() < 8) {
							edt = chEdt + tsEdt.substring(0, (10 - chEdt.length()));
							// System.xut.println("2. finalEndDatesPeriod edt==="
							// + edt);
						}
					}

					String chPer = colHdgEnddatePeriod.get(colNumber)[1];
					String tsPer = periodEnddateFromTableSentences.get(ts)[0];

					String per = chPer;
					if (chPer.equals("0") && null != tsPer && tsPer.length() > 0) {
						per = tsPer;

					}
					// System.xut
					// .println("chPer===" + chPer + " tsPer===" + tsPer);
					// System.xut.println("finalEndDatesPeriod per===" + per
					// + " edt=" + edt);
					if (goodChEdt)
						edt = colHdgEnddatePeriod.get(x)[0].trim();
					if (goodChPer)
						per = colHdgEnddatePeriod.get(x)[1].trim();
					// x is the ch count
					String[] ary = { edt, per };
					finalEndDatesPeriod.add(ary);

				}
				colNumber++;
			}
		}

		// System.xut.println("out of loop");

		// for (String[] ary : finalEndDatesPeriod)
		// System.xut.println("finalEndDatesPeriod=" + Arrays.toString(ary));
		return finalEndDatesPeriod;
	}

	public void getDataMap(String tableText, boolean redoMap, String tableTextWithTabs)
			throws IOException, SQLException, ParseException {

		// NLP.pwNLP.append(//NLP.printLn("@getDataMap tableText=", tableText));

		NLP nlp = new NLP();
		if (null != list_TS_tsShort_tsPattern_tnValid && list_TS_tsShort_tsPattern_tnValid.size() > 5
				&& tsShort.length() < 2 && list_TS_tsShort_tsPattern_tnValid.get(6).length() > 1) {

			tableNameLong = list_TS_tsShort_tsPattern_tnValid.get(6).replaceAll("\\\\|\\)|\\(|\\$|'|\\*", "");
			tableNameShort = tp.getTableNameShort(tableNameLong);
			// NLP.pwNLP.append(//NLP.printLn("ag tablenamelong=", tableNameLong + "
			// tableNameShort==" + tableNameShort));

		}

		// System.xut.println("last instance- tableNameLong="+tableNameLong);
		// System.xut.println("tableText="+tableText);

		// System.xut
		// .println("printing startidx of first col, no of data cols, and last col
		// endIdx at start of getDataMap method");
		//NLP.printListOfIntegerArray(sIdxC1NumOfCsLastCeIdx);

		List<String> tmpListCh = new ArrayList<String>();
		for (int b = 0; b < listCHbyIdxLoc.size(); b++) {
			tmpListCh.add(listCHbyIdxLoc.get(b).replaceAll("'|\\\\", ""));
		}
		listCHbyIdxLoc = tmpListCh;

		if (coNameMatched.length() < 1 && coNameOnPriorLine.length() > 1) {
			coNameMatched = coNameOnPriorLine;
		}
		if (coNameMatched.length() < 1 && list_TS_tsShort_tsPattern_tnValid.size() > 5
				&& list_TS_tsShort_tsPattern_tnValid.get(5).length() > 1) {
			coNameMatched = list_TS_tsShort_tsPattern_tnValid.get(5);
		}

		coNameMatched = coNameMatched.replaceAll("[\\\\\\/\\$\\(\\)\\&\\[\\]\\{\\}\\(\\)\\*\\+]", "");
		StringBuffer sb = new StringBuffer();

		allColText = "";
		String columnPattern = "";
		String columnPatternTs = "", chLine = "", chLinePrev = "";

		getAllColText();

		// System.xut.println("colhdg ======" + mapCHTextAndEidx);
		// System.xut.println(numberOfCHsRatio);
		// System.xut.println("getting DataMap");
		mapData.clear();
		rownumber = 0;
		mapData = new TreeMap<Integer, List<String[]>>();
		// ($ .35) or ($ 8.33)
		tableText = tableText.replaceAll("\\$", " ").replaceAll("(\\([ ]{1,6})(\\.?\\d)", "\\($2");

		Matcher match = patternOpenParenNotFollowedbyAlpha.matcher(tableText);
		while (match.find()) {
			tableText = match.replaceAll("-");
		}

		match = patternDollarSignOrParenNotFollowedbyAlpha.matcher(tableText);
		while (match.find()) {
			tableText = match.replaceAll(" ");
		}

		// System.xut.println("tableText w/o $()- =" + tableText);
		TableParser tp = new TableParser();
		tableText = tableText.replaceAll("[\r\n]{1,3}", "\r");
		String[] tableTextRows = tableText.split("\r");
		String line;
		String[] rowname;
		String[] priorRowname = null;
		List<String[]> priorListRow = new ArrayList<String[]>();

		// NLP.pwNLP.append(//NLP.printLn("if !redoMap=", redoMap + ""));

		if (!redoMap) {
			for (int i = 0; i < tableTextRows.length; i++) {
				List<String[]> listRow = new ArrayList<String[]>();
				line = tableTextRows[i];
				// System.xut.println("tableText line prior to data map=" +
				// line);
				if (line.replaceAll("[ ]{2,}", "").length() < 2)
					continue;
				boolean priorData = booleanData;
				// System.xut.println("A getting rowname--line="+line);
				rowname = getRowname(line, false);
				// System.xut.println("A got rowname===" +
				// Arrays.toString(rowname));
				// System.xut.println("B got rowname=" +
				// Arrays.toString(rowname)
				// + " priorData=" + priorData);
				removePrior = false;
				// System.xut.println("priorrowname ary="
				// + Arrays.toString(priorRowname));
				if (!priorData && null != priorRowname
						&& priorRowname[1].replaceAll("[\\s-]{1,30}", " ").trim().length() >= 5
						// rnh could be 'Sales:'= len=6
						&& Integer.parseInt(rowname[2]) >= Integer.parseInt(priorRowname[2])) {
					// System.xut.println("merging prior rowname. rowname=="
					// + Arrays.toString(rowname) + " priorRowname="
					// + Arrays.toString(priorRowname));
					// rowname = mergeRowname(rowname, priorRowname);
					// System.xut
					// .println("merged rowname=" + Arrays.toString(rowname));
				}

				// System.xut.println("adding rowname to listRow="
				// + Arrays.toString(rowname));
				listRow.add(rowname);
				// System.xut.println("Adding merged rowname=" + rowname[1]
				// + "booleanData=" + booleanData);

				booleanData = false;
				// for booleanData reset back to false - and run filter to
				// determine
				// if true
				String tmpDataStr = "";
				// System.xut.println("line.len=" + line.length());
				if (sIdxC1NumOfCsLastCeIdx.get(0)[0] <= -1)
					continue;
				// System.xut.println("sIdxC1NumOfCsLastCeIdx.get(0)[0]="+sIdxC1NumOfCsLastCeIdx.get(0)[0]);
				tmpDataStr = line.substring(Math.min(line.length(), sIdxC1NumOfCsLastCeIdx.get(0)[0]));
				if (tmpDataStr.split("  ").length > 1) {
					booleanData = true;
					// NLP.pwNLP.append(//NLP.printLn("line at. booleanData set to tru tmpDataStr=",
					// tmpDataStr));
				}

				if (booleanData) {
					String dataLine;
					if (sIdxC1NumOfCsLastCeIdx.get(0)[0] <= 0) {
						dataLine = line.substring(0, line.length());
					} else {
						dataLine = line.substring(Math.min(sIdxC1NumOfCsLastCeIdx.get(0)[0], line.length()),
								line.length());
					}

					// get any eIdx from line and if its eIdx is w/n 2 of
					// mapDataColIdxLocs - it is a number to input

					// if line has eIdx same as data map keys (eidx) then use
					// that to populate dataList below. Else use number pattern
					// matching technique below

					// will find any small nmb preceded by 3 space or ending a
					// line
					// or lg nmb. If eIdx of nmb matches for all cols then
					// foundEidx
					// and #s will populate dataList. Else use more restrictive
					// lg
					// nmb pattern to populate dataList

					Pattern nmbr = Pattern.compile("(?s)[ ]{3}[\\d]{1,3}(\\.[\\d]{1,2})?\\%|"
							+ "[ ]{3}\\-?[\\d]{1,3}\\%?(?=([ ]{0,2}$|[ ]{3}))|(?<!([A-Za-z,]))( [-\\.]?[\\d,\\.]{2,}\\%?| [-]{1,3})");

					// System.xut.println("dataLine="+dataLine);
					Matcher mt = nmbr.matcher(dataLine);

					// List<String[]> dataEidxList = nlp
					// .getAllEndIdxAndMatchedGroupLocs(dataLine, nmbr);

					// will grab % values as well
					List<String[]> dataEidxList = new ArrayList<String[]>();
					while (mt.find()) {
						// System.xut.println("data line match="+mt.group());
						String ary[] = { mt.end() + "", mt.group() };
						dataEidxList.add(ary);
						// System.xut.println("added data ary="+Arrays.toString(ary));
					}
					// System.xut.println("c. dataLine="+dataLine);
					// System.xut.println("printing dataEidxList");
					//NLP.printListOfStringArray(dataEidxList);

					if (dataEidxList.size() != numberOfCHsRatio) {

						// System.xut
						// .println("dataEidxList.size()!=numberOfCHsRatio -- dataLine="
						// + dataLine);

						dataLine = " " + dataLine.substring(1, dataLine.length());
						dataEidxList = nlp.getAllEndIdxAndMatchedGroupLocs(dataLine, nmbr);
						// System.xut.println("dataEidxList after I added a ws at the start==");
						//NLP.printListOfStringArray(dataEidxList);
						// System.xut.println("printing mapDataColIdxLocs<integer,list<String[]>:
						// key=data endIdx,val=[0]=count,val[1]startIdx");
						//NLP.printMapIntIntAry(mapDataColIdxLocs);
						// now ck if each eIdx align with map
						int eIdx = 0;
						@SuppressWarnings("unused")
						boolean eIdxPaired = false;
						for (int e = 0; e < dataEidxList.size(); e++) {
							eIdxPaired = false;
							eIdx = Integer.parseInt(dataEidxList.get(e)[0]) + dataEidxList.get(e)[1].length();

							for (Map.Entry<Integer, Integer[]> entry : mapDataColIdxLocs.entrySet()) {
								if (entry.getValue()[0] - eIdx < 3) {
									eIdxPaired = true;
									break;
								}
							}
						}
						if (eIdxPaired = false) {
							// System.xut.println("shit it didn't work! - so reset to original
							// dataEdixList.size");
							dataEidxList = nlp.getAllEndIdxAndMatchedGroupLocs(dataLine, nmbr);
						}
					}
					// System.xut.println("dataEidxList.size="+dataEidxList.size()+"
					// dataLine="+dataLine);

					// System.xut.println("printing dataEidxList");
					//NLP.printListOfStringArray(dataEidxList);
					Matcher mNmbr = nmbr.matcher(line);
					while (mNmbr.find()) {
						// System.xut.println("matched mNmbr.group="+mNmbr.group());
					}

					// need to add blank col if data col prior has no value but
					// one
					// data col value does have value that matches eIdx.
					int cl = 0;
					boolean foundEidx = false;
					int cnt3 = 0, cnt4 = 0;
					// System.xut.println("dataEidxList.size="+dataEidxList.size());
					// System.xut.println("printing mapDataColIdxLocs");
					//NLP.printMapIntIntAry(mapDataColIdxLocs);
					// System.xut.println("numberOfCHsRatio="+numberOfCHsRatio);

					// if dataEidxList.size!=numberOfCHsRatio - run less
					// rigorous
					// pattern matcher and if that results in equal value its
					// okay
					// so long as eIdx is very close to other data cols with
					// rigorous process

					for (int q = 0; q < dataEidxList.size(); q++) {
						cl = Integer.parseInt(dataEidxList.get(q)[0]);
						// System.xut.println("col eidx=" + cl +
						// " data col value="
						// + dataEidxList.get(q)[1]);
						cnt4 = 0;
						for (Map.Entry<Integer, Integer[]> entry : mapDataColIdxLocs.entrySet()) {
							// System.xut.println("mapDataColIdxLocs cnt="
							// + entry.getValue()[0]);
							if (entry.getValue()[0] < 7)
								continue;

							cnt4++;

							// if number of data cols on this row equal number
							// of
							// data columns in table than it doesn't have to
							// match
							// closely to end idx location. I use 7 because I
							// replace a tab with 6 ws

							// System.xut.println("de: numberOfCHsRatio=="
							// + numberOfCHsRatio + " dataEidxList.size="
							// + dataEidxList.size());

							// System.xut.println("entry.getKey()=" +
							// entry.getKey()
							// + " c1=" + cl
							// + " sIdxC1NumOfCsLastCeIdx.get(0)[0]="
							// + sIdxC1NumOfCsLastCeIdx.get(0)[0]
							// + " dataLine=" + dataLine
							// + "\r numberOfCHsRatio=" + numberOfCHsRatio
							// + " dataEidxList.size=" + dataEidxList.size()+
							// " dataEidxList.get(q)[1]="+dataEidxList.get(q)[1]);

							if ((Math.abs(entry.getKey() - (cl + sIdxC1NumOfCsLastCeIdx.get(0)[0])) < 7
									&& numberOfCHsRatio == dataEidxList.size())
									|| Math.abs(entry.getKey() - (cl + sIdxC1NumOfCsLastCeIdx.get(0)[0])) < 3) {
								// System.xut.println("found data col#="+cnt4+" number
								// equals="+dataEidxList.get(q)[1]);
								foundEidx = true;
								cnt3++;
								// add blank data col prior to cur col (back
								// fills)
								if (cnt3 < cnt4) {
									for (; cnt3 < cnt4; cnt3++) {
										String[] aryData = { cl + sIdxC1NumOfCsLastCeIdx.get(0)[0] + "", " " };

										// NLP.pwNLP.append(//NLP.printLn("adding blank data to ary and ary to listRow=",
										// Arrays.toString(aryData)));

										listRow.add(aryData);
									}
								}

								// System.xut.println("cnt3="+cnt3+" cnt4="+cnt4);
								if (cnt3 == cnt4 || numberOfCHsRatio == dataEidxList.size()) {
									String[] aryData = { cl + sIdxC1NumOfCsLastCeIdx.get(0)[0] + "",
											dataEidxList.get(q)[1] };

									// NLP.pwNLP.append(
									//NLP.printLn("A. adding data ary to listRow=", Arrays.toString(aryData)));

									listRow.add(aryData);

								}
							}
						}

						// front fill. last data col input but # of data cols in
						// list less than table. Insert diff. listRow-1 b/c it
						// has
						// rn so subtracting rowname you have # of datacols.

						// System.xut.println("listRow=======");
						//NLP.printListOfStringArray(listRow);
						int listRowNumberOfDataCols = listRow.size() - 1;
						// System.xut.println("q+1=" + (q + 1) +
						// " dataEidxList.size="
						// + dataEidxList.size() + " numb of data cols="
						// +
						// sIdxC1NumOfCsLastCeIdx.get(0)[1]+"
						// listRowNumberOfDataCols="+listRowNumberOfDataCols);
						if ((q + 1) == dataEidxList.size()
								&& listRowNumberOfDataCols < sIdxC1NumOfCsLastCeIdx.get(0)[1]) {
							for (int h = 0; h < (sIdxC1NumOfCsLastCeIdx.get(0)[1] - listRowNumberOfDataCols); h++) {
								String[] aryData = { cl + sIdxC1NumOfCsLastCeIdx.get(0)[0] + "", " " };

								// NLP.pwNLP
								// .append(//NLP.printLn("front fill: adding blank data to ary and ary to
								// listRow==",
								// Arrays.toString(aryData)));

								listRow.add(aryData);
							}
						}
					}

					// System.xut.println("C. printing listRow=");
					//NLP.printListOfStringArray(listRow);

					if (cnt3 != sIdxC1NumOfCsLastCeIdx.get(0)[1])
						foundEidx = false;

					// System.xut.println("foundEidx=" + foundEidx + " cnt3=" +
					// cnt3
					// + " numbOfCols=" + sIdxC1NumOfCsLastCeIdx.get(0)[1]);

					List<String> dataList = new ArrayList<>();

					// if cnt3 != numb of cols - I need to be careful to only
					// utilize remainder of line string. And if no data to
					// insert
					// blank as in above. so this picks up entire line- not just
					// left over!!!! FIX. I think I cut from last endIdx
					// successfully put into listRow -- see
					// dataEidxList.get(q)[1]
					// above (although unclear if that is always last one)

					if (!foundEidx) {
						dataList = nlp.getAllMatchedGroups(dataLine, patternNoAlphaSpaceNumberspace);
						// System.xut
						// .println("A PRINTING LIST OF PATTERNS NO ALPHA for line="
						// + line);
						//NLP.printListOfString(dataList);

						String data;
						// int lastIdx = Integer.parseInt(rowname[0]);
						int lastIdx = cl + sIdxC1NumOfCsLastCeIdx.get(0)[0];
						int endIdxData;
						// System.xut.println("lastIdx=" + lastIdx
						// + " versus endIdx of last retrieved data col="
						// + lastIdx);
						// if !foundEidx
						for (int c = 0; c < dataList.size(); c++) {
							String pattern = dataList.get(c).trim();
							// System.xut.println("A. matching dataCol patterns="
							// + pattern);
							// System.xut.println("A. lastIdx==" + lastIdx);
							Pattern patternData = Pattern.compile(pattern);
							// can only match against remaining portion of line
							// System.xut.println("A. line.substring(lastIdx)=="
							// + line.substring(lastIdx));
							Matcher matcher = patternData.matcher(line.substring(lastIdx));
							// System.xut.println("line.substring(lastIdx)" +
							// line
							// .substring(lastIdx)+" line="+line);
							if (matcher.find() && !line.substring(lastIdx).contains("%")) {
								data = matcher.group();
								// System.xut.println("A. data=" + data);
								endIdxData = matcher.end() + lastIdx;
								String[] aryData = { endIdxData + "", data };

								// NLP.pwNLP.append(
								//NLP.printLn("B. adding data ary to listRow=", Arrays.toString(aryData)));

								listRow.add(aryData);
								lastIdx = endIdxData;
							}
						}
					}
				}

				// next iteration of loop (next row)

				// System.xut.println("A removePrior=" + removePrior);
				if (removePrior) {
					// System.xut.println("removing from map row#=" + (rownumber
					// -
					// 1));
					mapData.remove(rownumber - 1);
					rownumber--;
				}

				if (isRownameheader) {
					// System.xut.println("isRownameheader="
					// + Arrays.toString(priorRowname));
					// System.xut.println("printing listRow");
					//NLP.printListOfStringArray(listRow);
					mapData.remove(rownumber - 1);
					rownumber--;
					String[] ary = { priorListRow.get(0)[0], priorListRow.get(0)[1].trim() + " RNH ",
							priorListRow.get(0)[2] };
					priorListRow.set(0, ary);
					mapData.put(rownumber, priorListRow);
					rownumber++;
				}

				isRownameheader = false;
				//NLP.printListOfStringArray("adding to mapData listRow", listRow);
				mapData.put(rownumber, listRow);
				rownumber++;
				// System.xut.println(" rowname/priorrowname="
				// + Arrays.toString(rowname));
				priorRowname = rowname;
				priorListRow = listRow;
			}

			// mapData has all tableText rows - further analysis is done below -
			// but
			// I can sum based on same eIdx in getSubtotals
			// key=row# value = list of string arrays list 1= ary[0]=endIdx,
			// rowname,list2= ary[1]..[99]=endIdx,colVal

			//NLP.printMapIntListOfStringAry("" + "", mapData);

			mapData = NLP.getSubtotals(mapData, sIdxC1NumOfCsLastCeIdx.get(0)[2], false, tableNameShort, redoMap);
			//NLP.printMapIntListOfStringAry("after subTotals -- mapData", mapData);

			// System.xut.println("mapData after subtotals");
			//NLP.printMapIntListOfStringAry(mapData);

			if (twoDataColsForEachColHdg) {
				Map<Integer, List<String[]>> mapData2 = NLP.hasPercentColumns(mapData, numberOfCHsRatio);
				if (mapData2 != null && mapData2.size() > 0) {
					mapData = mapData2;
				}
			}
		}

		// NLP.pwNLP.append(//NLP.printLn("if redoMap=", redoMap + ""));
		if (redoMap) {

			tableText = tableTextWithTabs.replaceAll("[\r\n]{1,3}", "\r");
			tableTextRows = tableText.split("\r");

			// redoMap is true when # of CHs equal # of data rows as
			// determined by counting number of data cols on each row and see if
			// that occurs 70% of the time WHERE number of data cols determined
			// by eIdx failed.
			Map<Integer, List<String[]>> mapDataSimple = NLP.getSimpleDataMap(tableText);

			//NLP.printMapIntListOfStringAry("mapDataSimple", mapDataSimple);
			if (null != mapDataSimple && mapDataSimple.size() > 0) {
				// NLP.pwNLP.append(//NLP.printLn("set mapData to mapDataSimple=", ""));
				mapData = mapDataSimple;
			}

			// first data row will always be 41 - which is the subtotal col
			// NLP.pwNLP.append(//NLP.printLn("mapAllCH2.siz", mapAllCH2.size() + ""));
			//NLP.printMapIntListOfStringAry("mapAllCH2", mapAllCH2);

			mapData = NLP.getSubtotals(mapData, 41, false, tableNameShort, redoMap);
			//NLP.printMapIntListOfStringAry("after subTotals -- mapDataSimple", mapData);

		}

		// remove percent column if '2:1 ratio - create global
		// twoDataColsForOneColHdg

		@SuppressWarnings("unused")
		int dataChCntMatch = 0, dataRows = 0, rowDataCols, colNo;
		String edt = "", ended = "", yr = "", mo = "", per = "", perIdx = "", edtIdx = "";

		if (tableSentence != null) {
			tableSentence = tableSentence.replaceAll("\'", "");
			// System.xut.println("107 tableSentence " + tableSentence);
		}

		String rn = "";
		String table = tp.getTableNamePrefixYearQtr(fileDate);
		// StringBuffer sb = new StringBuffer();
		int row = 0; // use to be 'key'
		boolean isNumber = true;
		String val, eIdxValue, eidxRowname;

		coNameMatched = coNameMatched.replaceAll("[^a-zA-Z;:,\\&\\$<>\\d\\(\\) ']", " ").replaceAll("'", "\\\\'")
				.replaceAll("[ \\s]{2,30}", " ");

		companyName = companyName.replaceAll("[^a-zA-Z;:,\\&\\$<>\\d\\(\\) ']", " ").replaceAll("'", "\\\\'")
				.replaceAll("[ \\s]{2,30}", " ");

		// allCnt: this is of the actual merged CHs (not TS) and if 1st place >1
		// or 10th>20 or 100th>200 -- colHdg is in error

		// System.xut.println("108 tableSentence @tsPattern=" + tableSentence);
		tsPattern = nlp.getTableSentencePatterns(tableSentence, "")
				.replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)", "");
		Pattern pCnt = Pattern.compile("\\dP:");
		Pattern yCnt = Pattern.compile("\\dY:");
		Pattern mCnt = Pattern.compile("\\dM:");
		String pCnts, yCnts, mCnts;

		int pI = 0, yI = 0, mI = 0;

		if (null != nlp.getAllIndexEndLocations(tsPattern, pCnt)
				&& nlp.getAllIndexEndLocations(tsPattern, pCnt).size() > 0)
			pI = nlp.getAllIndexEndLocations(tsPattern, pCnt).size();

		if (null != nlp.getAllIndexEndLocations(tsPattern, yCnt)
				&& nlp.getAllIndexEndLocations(tsPattern, yCnt).size() > 0)
			yI = nlp.getAllIndexEndLocations(tsPattern, yCnt).size();

		if (null != nlp.getAllIndexEndLocations(tsPattern, mCnt)
				&& nlp.getAllIndexEndLocations(tsPattern, mCnt).size() > 0)
			mI = nlp.getAllIndexEndLocations(tsPattern, mCnt).size();

		pCnts = "pCnt:" + pI;
		yCnts = "yCnt:" + yI;
		mCnts = "mCnt:" + mI;

		String tmpTs = tsPattern;
		// System.xut.println("initial tsPattern==" + tsPattern);
		// cut from start to pC. Keep pC to end as substring to add back
		tsPattern = NLP.getMonthYearPeriodFromTSpatternFormatted(tmpTs)
				.replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)", "");
		tsPattern = tsPattern + pCnts + "|" + yCnts + "|" + mCnts;

		allColText = allColText.replaceAll("[\\|]{2,}", "\\|").replaceAll("'", "");

		tsPattern = tsPattern.replaceAll("[\\|]{2,}", "\\|").replaceAll("'", "");

		List<String> listTmp = new ArrayList<>();
		listTmp = NLP.getDistinctPeriodMonthYearCount(tableSentence);
		if (null != tableSentence && listTmp.size() > 0) {
			tsPattern = tsPattern + listTmp.get(0).replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)", "");
		}

		tsPattern = tsPattern + "||" + tableSentence;

		Pattern patternShortYear = Pattern
				.compile("[09]{1}[0-9]{1}-(0[1-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})");
		Matcher matchShortYear;

		String soleMonth = "", solePeriod = "";
		// NLP.pwNLP.append(//NLP.printLn("tsPattern @soleMonth=" + tsPattern +
		// "\rtableSentence=", tableSentence));
		boolean solePer = true, soleMo = true;
		//NLP.printMapIntListOfStringAry("did it change from earlier? originalMapAllCH=", originalMapAllCH);
		// has to be original map of CHs - later maps are maniupated so whether
		// it is solemonth or period is then corrupted. Keeps this process
		// independent and less likely to produce f/p. Same is true for
		// allColText which reruns off of startGroupo

		for (Map.Entry<Integer, List<String[]>> entry : originalMapAllCH.entrySet()) {
			List<String[]> listData = entry.getValue();
			for (int u = 0; u < listData.size(); u++) {
				if (nlp.getAllMatchedGroups(listData.get(u)[1], NLP.patternInception).size() > 0) {
					solePer = false;
					soleMo = false;
				}
				// if all CHs periods are on same linethen I can use first and
				// last period to match against 1 and last cols
			}
		}

		// sole method to use to get sole month and sole period
		String[] soleMonthSolePeriod = NLP.getSoleMonthSolePeriod(allColText, tsPattern);
		if (soleMonthSolePeriod != null && soleMonthSolePeriod[0] != null && soleMonthSolePeriod[0].length() > 1
				&& soleMo && !tsShort.equals("MY")) {
			soleMonth = "-" + soleMonthSolePeriod[0].substring(0, 2) + "-" + soleMonthSolePeriod[0].substring(2, 4);
			if (soleMonth.substring(4, soleMonth.length()).equals("00")) {
				soleMonth = soleMonth.substring(0, 4);
			}

		}

		if (soleMonthSolePeriod != null && soleMonthSolePeriod[1] != null && soleMonthSolePeriod[1].length() > 0
				&& solePer) {
			solePeriod = soleMonthSolePeriod[1];
		}

		// NLP.pwNLP.append(//NLP.printLn("soleMonth=", soleMonth + " solePeriod=" +
		// solePeriod));

		// System.xut.println("4 listEdtPerIdx.size="+listEdtPerIdx.size());
		List<String[]> tmpListEdtPerIdx = new ArrayList<String[]>();
		String tmpEdt, tmpPer, prevYr = "", curYr = "";
		int prYr = 0, yr1 = 0;
		@SuppressWarnings("unused")
		boolean columnOneAndTwoSameYear = true, ascendingYrIdx = false;
		for (int i = 0; i < listEdtPerIdx.size(); i++) {
			tmpEdt = listEdtPerIdx.get(i)[0];
			tmpPer = listEdtPerIdx.get(i)[1];
			if (listEdtPerIdx.size() > 1 && i == 0 && tmpEdt.length() > 4) {
				if (!listEdtPerIdx.get(i)[0].substring(0, 4).equals(tmpEdt)) {
					columnOneAndTwoSameYear = false;
				}
			}

			// System.xut.println("tmpPer="+tmpPer);
			matchShortYear = patternShortYear.matcher(tmpEdt);
			if (matchShortYear.find() && tmpEdt.substring(0, 1).equals("9")) {
				tmpEdt = "19" + tmpEdt;
			}

			matchShortYear = patternShortYear.matcher(tmpEdt);
			if (matchShortYear.find() && tmpEdt.substring(0, 1).equals("0")) {
				tmpEdt = "20" + tmpEdt;
				// System.xut.println("01 found?="+tmpEdt);
			}

			if ((tmpEdt.replaceAll("[12]{1}[09]{1}[0-9]{2}-?-?", "").length() == 0
					|| tmpEdt.replaceAll("[12]{1}[09]{1}[0-9]{2}-(0[0-9]{1}|1[0-2]{1})-?", "").length() == 0)
					&& soleMonth.length() > 3) {
				// eg: soleMonth= -12-31 --- or just -01- (January)
				if (listEdtPerFinal.size() > i && listEdtPerFinal.get(i)[0] != null
						&& listEdtPerFinal.get(i)[0].trim().length() == 10
						&& listEdtPerFinal.size() == listEdtPerIdx.size() && tmpEdt.length() > 4
						&& listEdtPerFinal.get(i)[0].substring(0, 4).equals(tmpEdt.substring(0, 4))) {
					tmpEdt = listEdtPerFinal.get(i)[0];
				} else {
					if (tmpEdt.length() > 3) {
						tmpEdt = tmpEdt.substring(0, 4) + soleMonth;
						// NLP.pwNLP.append(//NLP.printLn("soleMonth added -- edt2=", tmpEdt));
					}
				}
			}

			/*
			 * if (listEdtPerTableSentence.size() > 0 && tmpEdt.replaceAll("[-]{1,}",
			 * "").length() == 4 && nlp.getAllIndexEndLocations(tsShort,
			 * Pattern.compile("M")).size() == 1 &&
			 * listEdtPerTableSentence.get(0)[0].replaceAll("[-]{1,}", "").length() == 2) {
			 * if (listEdtPerFinal.get(i)[0] != null &&
			 * listEdtPerFinal.get(i)[0].trim().length() == 10 && listEdtPerFinal.size() ==
			 * listEdtPerIdx.size() && listEdtPerFinal.get(i)[0].substring(0, 4).equals(
			 * tmpEdt.substring(0, 4))) { tmpEdt = listEdtPerFinal.get(i)[0]; } else {
			 * tmpEdt = tmpEdt.replaceAll("[-]{1,}", "") +
			 * listEdtPerTableSentence.get(0)[0]; } }
			 */
			String[] ary = { tmpEdt, tmpPer };
			tmpListEdtPerIdx.add(ary);

			if (tmpEdt.length() > 3) {
				curYr = tmpEdt.substring(0, 4);
				if (curYr.contains("-") || curYr.contains(",") || curYr.contains("(") || curYr.contains("$"))
					yr1 = 0;
				else {
					yr1 = Integer.parseInt(curYr);
				}
			}
			if (tmpEdt.length() <= 3) {
				curYr = "";
				yr1 = 0;
			}

			if (tsShort.equals("PPMYY") || tsShort.equals("PPMYMY") || tsShort.equals("PM")
					|| tsShort.equals("PPMYYMYtMY") || tsShort.equals("PMYYMYtMY")) {
				if (i == 1 && prevYr != curYr) {
					columnOneAndTwoSameYear = false;
					// System.xut.println("aa columnOneAndTwoSameYear ="
					// + columnOneAndTwoSameYear);
				}
			}

			// System.xut.println("prevYr=" + prevYr + " curYr=" + curYr);
			if (prYr < yr1) {
				ascendingYrIdx = true;
			}
			prevYr = curYr;
			prYr = yr1;
		}

		listEdtPerIdx = tmpListEdtPerIdx;
		//NLP.printListOfStringArray("listEdtPerIdx before insert into mysql and after tmpListEdtPerIdx=", listEdtPerIdx);

		if (null != listEdtPerTableSentence && listEdtPerTableSentence.size() > 0 && tsShort.length() > 0
				&& tableSentence.length() > 5) {
			listEdtPerIdx = NLP.mergeTablesentenceEdtPerWithEdtPerIdx(listEdtPerIdx, listEdtPerTableSentence, tsShort,
					numberOfCHsRatio, columnOneAndTwoSameYear, allColText);
			//NLP.printListOfStringArray("final chance to merge TS. listEdtPerIdx", listEdtPerIdx);
		}

		boolean hasSubtotal = false;

		// System.xut.println("pcntD1Per="+pCntD1Per);

		// if listEdtPerIdx is blank but if tsSort (tableSentence) lines up with
		// CH raw - then lets use!
		if (listEdtPerIdx.size() == 0 && null != listEdtPerTableSentence && listEdtPerTableSentence.size() > 0
				&& tsShort.length() > 0 && tableSentence.length() > 5) {
			listEdtPerIdx = NLP.mergeTablesentenceEdtPerWithEdtPerIdx(listEdtPerFinal, listEdtPerTableSentence, tsShort,
					numberOfCHsRatio, columnOneAndTwoSameYear, allColText);
			//NLP.printListOfStringArray("aft merge w/ TS edt/per rt bef insert into mysql. listEdtPerIdx",
//					listEdtPerIdx);
		}

		/*
		 * if (tsPattern.contains("pCntD:1") && tsShort.length() > 0 &&
		 * tsShort.substring(0, 1).equals("P") && !tsShort.substring(1,
		 * tsShort.length()).contains("P") && listEdtPerIdx.size() > 0 &&
		 * listEdtPerIdx.get(0)[1].equals("0") && listEdtPerIdx.get(listEdtPerIdx.size()
		 * - 1)[1].equals("0") && !tableNameShort.toUpperCase().equals("BS")) {
		 * 
		 * String tsP = tsPattern.substring(1, 5); List<String[]> listTmpP = new
		 * ArrayList<>(); boolean allSame = true; for (int i = 0; i <
		 * listEdtPerIdx.size(); i++) { if (!listEdtPerIdx.get(i)[1].equals("0") &&
		 * !listEdtPerIdx.get(i)[1].equals(tsP)) { //NLP.pwNLP.append(//NLP.printLn(
		 * "tsP=", tsP + " listEdtPerIdx.get(i)[1]=" + listEdtPerIdx.get(i)[1]));
		 * allSame = false; break; } String[] ary = { listEdtPerIdx.get(i)[0], tsP };
		 * listTmpP.add(ary); } if (allSame) listEdtPerIdx = listTmpP; }
		 */

		// start of merge with table of contents edt/per. can't attempt
		// edtPeridx merges after this -- listTocEdtPerTotColsTblnm<=this list
		// has all tables - so find the - one that has same tn short (eg 'is)
		// and tc. Start of merge with TOC.

		listEdtPerIdx = NLP.mergeWithTableOfContentsEndDatePeriods(listEdtPerIdx, listTocEdtPerTotColsTblnm,
				tableNameShort, tableNameLong, columnOneAndTwoSameYear, allColText);

		//NLP.printListOfStringArray("new method after merge with TOC enddate period - final listEdtPerIdx=",
//				listEdtPerIdx);

		for (Map.Entry<Integer, List<String[]>> entry : mapData.entrySet()) {
			int key = entry.getKey();
			List<String[]> value = entry.getValue();
			//NLP.printListOfStringArray("a List<String[] from mapData", value);
			rowDataCols = value.size() - 1;

			if (numberOfCHsRatio == rowDataCols) {
				dataChCntMatch++;
				// NLP.pwNLP.append(
				//NLP.printLn("numberOfCHsRatio == rowDataCols", numberOfCHsRatio + " == " +
				// rowDataCols));
			}
			// 1st list=rn, so if>1 there is addtl col w/ data
			if (value.size() > 1) {
				dataRows++;
			}

			if (value.size() != 1 && rowDataCols != numberOfCHsRatio) {

				// value = getCorrectedRow(value, mapData, numberOfCHsRatio,
				// key,
				// rowDataCols);
				// System.xut.println("getting row corrected - first dataCol="
				// + value.get(1)[1]);
				// System.xut.println("B data row=="
				// + Arrays.toString(value.get(1)));

			}

			// row use to be key

			for (int i = 0; i < value.size(); i++) {
				// System.xut.println("rn before replacement="+value.get(0)[1]);
				rn = value.get(0)[1].replaceAll("[^a-zA-Z;:,\\|\\.\\&\\$<>\\d\\(\\)]|<.*[^>]>|[\\s ]{2,}", " ")
						.replaceAll("[\\s=]{2,30}", " ").trim();
				// don't replace \\. b/c it has value in the rowname
				// System.xut.println("rn after replacements="+rn);
				eidxRowname = value.get(0)[0];
				// System.xut.println("rn after replacement="+rn);
				if (rn.length() < 2 && value.size() == 1) {
					continue;
				}

				// add ended,yr,mo here
				// NLP.pwNLP.append(//NLP.printLn("value.size==", value.size()
				// + " i=" + i + " listEdtPerFinal.size="
				// + listEdtPerFinal.size()));

				if (listEdtPerIdx.size() > i && value.size() > 0 && i > 0) {
					// NLP.pwNLP.append(//NLP.printLn("value.size=",
					// value.size() + " listEdtPerIdx.size=" + listEdtPerIdx.size() + " i=" + i
					// + " listEdtPerIdx.get(i - 1)[0]=" + listEdtPerIdx.get(i - 1)[0])
					// + " value=" + value.get(i)[1]);
				}

				if (value.size() == 1) {
					sb.append("(\r'" + acc + "','" + fileDate + "','" + cik + "','" + tableNameShort + "','" + row
							+ "','" + 0 + "','" + key + "','" + tableCount + "','"
							+ replaceEndStrands(rn).replaceAll(",", " ")
							+ "',\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,"
							+ eidxRowname.replaceAll(",", " ") + ",\\N,\\N,'',''),");

					row++;

					// acc,filedate,cik,tbl_nm,row,tb_no,rowname,value,col,per1,edt1,per2,edt2,deci,columnText,totCol
					// ,coNmMatch,CompanyNameParsed,CompanyName,htmlOrtxt,formtype,fye
				}
				// TODO may be able to adjust above to parse 1st row. Or all but
				// have dummy headers.

				else if (value.size() > 0 && i > 0 && listEdtPerIdx != null && listEdtPerIdx.size() > (i - 1)) {
					// NLP.testText =
					//NLP.printListOfStringArray("at insert into mysql final
					// listEdtPerFinal",listEdtPerFinal);
					// NLP.testText =
					//NLP.printListOfStringArray("at insert into mysql final
					// listEdtPerIdx",listEdtPerIdx;
					if (null != listEdtPerIdx && listEdtPerIdx.size() == numberOfCHsRatio) {

						edtIdx = listEdtPerIdx.get(i - 1)[0];
						perIdx = listEdtPerIdx.get(i - 1)[1];

						// NLP.pwNLP.append(//NLP.printLn("a. edtIdx=", edtIdx));
						// NLP.pwNLP.append(//NLP.printLn("b. perIdx=", perIdx));

						if (perIdx.equals("0")) {
							perIdx = solePeriod;
						}
					}

					if (null != listEdtPerFinal && listEdtPerFinal.size() == numberOfCHsRatio
							&& listEdtPerFinal.size() > 0 && listEdtPerFinal.size() > (i - 1)) {
						edt = listEdtPerFinal.get(i - 1)[0];
						per = listEdtPerFinal.get(i - 1)[1];

						if (edt != null && edt.length() > 8 && edt.substring(0, 1).equals("-")
								&& edt.substring(3, 4).equals("-")) {
							edt = edt.substring(6, edt.length()) + edt.substring(0, 6);
						}
					}

					else {
						edt = "";
						per = "";
					}

					// if edt= -12-312001 - will switch it to '2001-12-31'

					if (listCHEndedYrandMoText.size() == numberOfCHsRatio && listCHEndedYrandMoText.size() > (i - 1)) {
						ended = listCHEndedYrandMoText.get(i - 1)[0];
						yr = listCHEndedYrandMoText.get(i - 1)[1];
						mo = listCHEndedYrandMoText.get(i - 1)[2];
					} else {
						ended = "";
					}
					if (null != ended) {
						ended = ended.replaceAll("'", "");
					}
					if (null != yr) {
						yr = yr.replaceAll("'", "");
					}
					if (null != mo) {
						mo = mo.replaceAll("'", "");
					}
					// System.xut.println("val bef repl="+value.get(i)[1]);
					val = value.get(i)[1].replaceAll("[\\)\\$\\,]", "").replaceAll("\\(", "-").trim();
					// [^\\d\\(\\.\\-]| <<==replace everything other than a -,
					// (, \\d or \\
					// System.xut.println("val==="+val);

					eIdxValue = value.get(i)[0];
					// System.xut.println("val eIdx?="+eIdxValue);
					isNumber = nlp.isNumeric(val);
					if (!isNumber) {
						val = "null";
					}

					// must enclose allColText, colHdg and tsPattern in '' b/c
					// it uses '|'. Used xx as a separator b/w col hdg txt and
					// pattern so I can pickup in mysql regex if necessary.

					// if (null != listCHbyIdxLoc) {
					//NLP.printListOfString(listCHbyIdxLoc);
					// }

					// if i=5 - there are 4 colHdgs. So at i=1 i call 0 - so if
					// 4 col hdgs then max i=5 and 5-2=3 which must be less than
					// listchbyidxloc. i=0=rowname and i=1 ... and aft = data
					// cols.

					String tmpStr = "";
					if (i > 0 && null != listCHbyIdxLoc && listCHbyIdxLoc.size() > 0 && listCHbyIdxLoc.size() >= i
							&& null != listCHbyIdxLoc.get(i - 1) && listCHbyIdxLoc.get(i - 1).length() > 0) {
						tmpStr = listCHbyIdxLoc.get(i - 1).replaceAll("'", "");
						// System.xut
						// .println("printing listCHbyIdxLoc at tmpStr. get(i-1) # is="
						// + (i - 1) + " tmpStr=" + tmpStr);
					}

					String tl = "";
					if (rn.contains(";TL")) {
						tl = rn.substring(rn.indexOf(";TL") + 3).trim();
						// System.xut.println("1 tl="+tl);
						tl = tl.substring(0, tl.indexOf(";")).trim();
						// System.xut.println("2 tl="+tl);
						hasSubtotal = true;
					}

					String st = "";
					if (rn.contains(";ST")) {
						st = rn.substring(rn.indexOf(";ST") + 3).trim();
						// System.xut.println("1 st="+st);
						st = st.substring(0, st.indexOf(";")).trim();
						// System.xut.println("2 st="+st);
						hasSubtotal = true;
					}

					String net = "";
					if (rn.contains(";NET")) {
						net = rn.substring(rn.indexOf(";NET") + 4).trim();
						net = net.substring(0, net.indexOf(";")).trim();
						hasSubtotal = true;
					}

					String sub = "";
					if (rn.contains(";SUB")) {
						sub = rn.substring(rn.indexOf(";SUB") + 4).trim();
						sub = sub.substring(0, sub.indexOf(";")).trim();
						hasSubtotal = true;
					}

					Matcher matchFunkyCH;
					matchFunkyCH = NLP.patternColHdgFunky.matcher(tmpStr);
					if (matchFunkyCH.find()) {
						edtIdx = "";
						edt = "";
						perIdx = "";
						per = "";
					}

					if (ended == null) {
						ended = "";
					}
					if (mo == null) {
						mo = "";
					}
					if (yr == null) {
						yr = "";
					}

					sb.append("(\r'" + acc + "','" + fileDate + "','" + cik + "','" + tableNameShort + "','" + row
							+ "','" + i + "','" + key + "','" + tableCount + "','"
							+ replaceEndStrands(rn).replaceAll(",", " ") + "','" + val + "','" + tl + "','" + st + "','"
							+ net + "','" + sub + "','" + per + "','" + edt + "','" + perIdx + "','" + edtIdx + "','"
							+ numberOfCHsRatio + "','" + tableNameLong.replaceAll(",", " ") + "','" + coMatch + "','"
							+ coNameMatched + "','" + decimal + "','" + tsShort + "','" + tmpStr.replaceAll(",", " ")
							+ "','" + columnPattern.replaceAll(",", " ") + "','" + allColText.replaceAll(",", " ")
							+ "','" + ended.replaceAll(",", " ") + "','" + yr.replaceAll(",", " ") + "','"
							+ mo.replaceAll(",", " ") + "','" + eIdxValue.replaceAll(",", " ") + "','" + formType
							+ "','" + tsPattern.replaceAll(",", " ") + "'),");
					row++;

				} else {
				}
			}
		}

		rownumber = 0;
		mapData.clear();
		mapData = new TreeMap<Integer, List<String[]>>();

		// String tble = "";
		if (sb.toString().length() > 500) {

			// tble= "insert ignore into tp_raw" + table
			// + " values"
			// + sb.toString().substring(0, sb.toString().length() - 2)+";";
			// System.out.println("sb.toString.len=" + sb.toString().length());

			TableParser.pwYyyyQtr.append(sb.toString().substring(1, sb.toString().length() - 2)
					.replaceAll("\'xxy\'", "\\\\N").replaceAll("\\),\\(|\'", "").replaceAll("[\r\n]{2}", "\r"));

			// System.out.println(sb.toString().substring(1, sb.toString().length() - 2)
			// .replaceAll("\'xxy\'", "\\\\N").replaceAll("\\),\\(|\\'",
			// "").replaceAll("[\r\n]{2}", "\r"));

			// MysqlConnUtils.executeQuery(tble);
			// tble = "";

			// System.xut.println("inserting table tableNameShort="+tableNameShort);

			if (sb.toString().length() > 3000 && hasSubtotal) {
				tbC2++;
			}
		}
		sb.delete(0, sb.toString().length());

		allColText = "";

		// if (!parseTableFromFolder) {

		// FileSystemUtils.createFoldersIfReqd(path);
		// PrintWriter pw2 = new PrintWriter("c:/backtest/tableParser/"
		// + fileDate.substring(0, 4) + "/QTR" + qtr + "/tables/"
		// + acc + "_" + tableCount + ".txt");
		//
		// String tmp = ((acc + "," + cik + "," + fileDate + "," + coMatch
		// + "," + coNameMatched.replaceAll("[\r\n]", " ") + ","
		// + companyName.replaceAll("[\r\n]", "") + "," + formType
		// + "," + fye + "," + tableCount).replaceAll("[\r\n]", ""));
		// System.xut.println("tmp="+tmp);

		// System.xut.println("tableSaved before write="+tableSaved);
		// pw2.write(tmp + "\r" + tableSaved);
		// pw2.close();

		// }

		tableCount++;
		// System.xut.println("new table");
		listEdtPerIdx.clear();
		tableSentence = "";
		decimal = "1";
	}

	public static String replaceEndStrands(String text) {

		/*
		 * 1st replace is stripping from rowname ;sub22; etc. ONLY use this replace
		 * where rowname is inputted into mysql. not before. 2nd replace is getting end
		 * strands such as Revenues1 -->Revenues
		 */

		text = text.replaceAll("(?i) ?;(SUB|NET|ST|TL)[\\d]{1,4}; ?", "");

		text = text.replaceAll(
				"(?i)( ?\\(?note.{1,3}\\)? ?| ?(\\. )+ ?\\.?+|(?<=[A-Za-z;:]{2})\\d\\)? ?)(?=(( ?;.{3,6};)+?)?)", "")
				.replaceAll("\\. $| \\.$", "");

		// System.xut.println("after repl rn="+text);

		return text;

	}

	public List<String[]> getCorrectedRow(List<String[]> rowMissingDataColList, Map<Integer, List<String[]>> map,
			int noOfCol, int key, int rowDataCols) {
		List<String[]> holdList = new ArrayList<>();

		// System.xut.println("rowMissingDataColList.size @getcorrected row="
		// + rowMissingDataColList.size());
		if (rowDataCols < numberOfCHsRatio) {

			int numberMissingDataCols = noOfCol - (rowMissingDataColList.size() - 1);

			String rowname;
			int eIdxRn, eIdx;
			List<String[]> goodRowList = new ArrayList<>();
			// searching backward up to 4 rows
			// System.xut.println("searching backwards key==" + key);

			// if row is < 7 - search forward from key - so set keyCnt=key. If
			// key>=7 search set key five rows back by subtracting 5 from key
			// and then search forward

			int keyCnt = 0;
			if (key < 7)
				keyCnt = key;
			else
				keyCnt = key - 5;

			for (int i = keyCnt; i < Math.min(keyCnt + 5, map.size()); i++) {
				// for (int i = key; i < 5; i++) {
				// System.xut.println("map i="+i);
				goodRowList = map.get(i);// 1st row
				// System.xut.println("in map in reverse - prior row is="
				// + Arrays.toString(goodRowList.get(0)) + "val.size=" +
				// goodRowList.size());

				// System.xut.println("goodRowList dataCols @getcorrected
				// row="+(goodRowList.size()-1)
				// + " nOfCol="+noOfCol);

				if (null == goodRowList || goodRowList.size() < 1 || (goodRowList.size() - 1) != noOfCol) {
					continue;
				}
				// for (int c = 0; c < goodRowList.size(); c++) {
				// each col of good row has String[]= 0=eIdx, 1=rown|dataVal
				// System.xut.println("2 goodRowList.size @getcorrected row="
				// + goodRowList.size() + "nOfCol=" + noOfCol);

				if (((goodRowList.size() - 1) == noOfCol)) {
					// System.xut.println("3 goodRowList.size @getcorrected row="+goodRowList.size()
					// + "nOfCol="+noOfCol);

					eIdx = Integer.parseInt(goodRowList.get(0)[0]);
					// System.xut.println("val=" + goodRowList.get(0)[1]);
					// ck each col of miss'g data col. At n=0 ck'g rn endIdx
					// if>> 1st good row data col eIdx.
					// for (int n = 0; n < rowMissingDataColList.size(); n++) {

					rowname = rowMissingDataColList.get(0)[1];
					eIdxRn = Integer.parseInt(rowMissingDataColList.get(0)[0]);
					if (/* n == 0 && */numberMissingDataCols > 0 && rowname.replaceAll(" ", "").length() > 0
							&& eIdxRn > eIdx) {
						rowMissingDataColList = checkRownameEndForDataCol(rowname, rowMissingDataColList, eIdxRn);
						if (null != rowMissingDataColList && (rowMissingDataColList.size() - 1) == numberOfCHsRatio) {
							// 1 miss'g data col & found at end of row
							return rowMissingDataColList;
							// }
						}
					}
				}
				// }

				if ((rowMissingDataColList.size() - 1) < numberOfCHsRatio) {
					// System.xut.println("findblankdatacolumns");
					holdList = findBlankDataColumns(rowMissingDataColList, goodRowList);
					// System.xut.println("holdList.size=" + holdList.size());
					return holdList;

				}
			}
			// checked 4 rows prior and can't find instance of row with all data
			// cols present. Now ck forward.
			if ((holdList.size() - 1) < numberOfCHsRatio) {

			}

			return holdList;
		}

		else if (rowDataCols > numberOfCHsRatio) {
			// loop forward?? -- or is table just corrupt? or did I
			// inadvertantly grab a data col from end of row? - if good tbl
			// otherwise - see which data col is most funky
		}
		return holdList;
	}

	public List<String[]> findBlankDataColumns(List<String[]> listMissingDataColRow, List<String[]> listgoodRow) {

		// b/c rowMisingDataColList is 1 or more smaller - do compare against
		// goodRow using 1 loop. Both have same string format eg: each list is a
		// list of eIdx,col (0=eidx,rn,1=eidx,dataVal,2=eidx,dataVal...)

		int eIdxG, eIdxM, distCur = 1000;
		@SuppressWarnings("unused")
		String gStr, mStr;
		List<Integer[]> listColumnsMatched = new ArrayList<Integer[]>();
		List<String[]> listReconstitutedRow = new ArrayList<>();
		// if only rowname nothing to find
		if (null == listMissingDataColRow || listMissingDataColRow.size() < 2) {
			return listMissingDataColRow;
		}

		// ck each col of row with missing data col and compare its eIdx v eIdx
		// of row with all data col, if <4 - match.
		for (int i = 1; i < listMissingDataColRow.size(); i++) {
			// then compare to each data col in good list - and find closest
			// - then record idx location - if # gets bigger grab earlier.
			// record eidx difference

			for (int c = 1; c < listgoodRow.size(); c++) {
				eIdxG = Integer.parseInt(listgoodRow.get(c)[0]);
				eIdxM = Integer.parseInt(listMissingDataColRow.get(i)[0]);
				gStr = listgoodRow.get(c)[1];
				mStr = listMissingDataColRow.get(i)[1];
				distCur = Math.abs(eIdxG - eIdxM);
				if (distCur < 4) {
					// System.xut.println("its a match -- eIdx dist=" + distCur
					// + " gStr=" + gStr + " mStr=" + mStr + " eIdxG="
					// + eIdxG + " eIdxM=" + eIdxM + " gdCol#=" + c
					// + " v bdCol#" + i);
					Integer[] intAry = { c, i };
					listColumnsMatched.add(intAry);
					// each ary=correct col#, related bad col#
				}
			}
		}

		int cnt = 0;
		listReconstitutedRow.add(listMissingDataColRow.get(cnt));// add rn
		for (int i = 1; i < listgoodRow.size(); i++) {
			/*
			 * for each correct data col location (goodRow) - see if there's a match with
			 * column list's of matched col # @ ary[0]. If there is - insert eIdx,val from
			 * row that has data (missing data col row). Increase cnt so on next loop we ck
			 * next variable in list of matched cols. If nothing found - insert blank ary
			 * into reconstitued row.
			 */
			if (cnt < listColumnsMatched.size() && i == listColumnsMatched.get(cnt)[0]
					&& listMissingDataColRow.size() > cnt + 1) {
				// System.xut.println("listMissingDataCol==="+Arrays.toString(listMissingDataColRow.get(cnt+1)));
				listReconstitutedRow.add(listMissingDataColRow.get(cnt + 1));
				// System.xut.println("corrected col# and
				// dataVal="+Arrays.toString(listReconstitutedRow.get(cnt+1)));
				cnt++;
			} else {
				String[] aryBlank = { "", "" };
				listReconstitutedRow.add(aryBlank);
			}
		}

		// for (int i=0; i<listReconstitutedRow.size(); i++){
		// System.xut.println("reconstituted list that had
		// blanks="+Arrays.toString(listReconstitutedRow.get(i)));
		// }

		return listReconstitutedRow;

	}

	public List<String[]> checkRownameEndForDataCol(String rowname, List<String[]> rowMissingDataColList, int eIdxRn) {
		NLP nlp = new NLP();

		List<String[]> holdList = new ArrayList<>();
		String dataStr, rownameEndStr;
		String[] rownameEnd;
		@SuppressWarnings("unused")
		double dataFound;
		boolean isNumber = false;

		rownameEnd = rowname.split(" ");
		rownameEndStr = rownameEnd[rownameEnd.length - 1];
		dataStr = (rownameEndStr.replaceAll("(?i)([\\$,\\)\\, ]|(hk|usd|can))", "").replaceAll("\\(", "-"));
		// System.xut.println("dataStr aft rep="+dataStr);
		isNumber = nlp.isNumeric(dataStr);

		if (!isNumber)
			return rowMissingDataColList;

		else if (isNumber) {
			dataFound = Double.parseDouble(dataStr);
			rowname = rowname.substring(0, StringUtils.indexOfIgnoreCase(rowname, rownameEndStr));
			String[] ary = { rowname.length() + "", rowname };
			holdList.add(ary);
			String[] ary2 = { eIdxRn + "", rownameEndStr };
			holdList.add(ary2);

			// System.xut.println("dataFound at end of rowname===|"
			// + dataFound + "|new rowname===" + rowname
			// + "|");

			for (int x = 1; x < rowMissingDataColList.size(); x++) {
				String[] ary3 = { rowMissingDataColList.get(x)[0], rowMissingDataColList.get(x)[1] };
				// System.xut
				// .println("adding to holdList ary from rowMissingData remaining data items="
				// + Arrays.toString(ary3));
				holdList.add(ary3);
			}
		}
		return holdList;
	}

	public String[] mergeRowname(String rowname[], String[] priorRowname) {

		// System.xut.println("this is when I merge rownames");

		// NLP nlp = new NLP();
		isRownameheader = false;
		// System.xut.println("aa priorRowname=" + priorRowname[1]
		// + " rowname[1]=" + rowname[1] + " tablenameShort="
		// + tableNameShort);

		// System.xut
		// .println("priorRowname[1].split((?i)(plant|equip|propert)).length"
		// + priorRowname[1].split("(?i)(plant|equip|propert)").length);

		Pattern patternALLCAPS = Pattern.compile("(?<!([a-z]))[A-Z]{5,}");
		Matcher matchALLCAPS = patternALLCAPS.matcher(priorRowname[1]);
		priorRowname[1] = priorRowname[1].replaceAll("[ \\s-]{2,30},", " ").replaceAll(" $", "").trim();

		if (priorRowname[1].endsWith(":")
				|| priorRowname[1].replaceAll("(?i)cost[s].{1,2}and expense[s]", "").trim().length() < 1
				|| priorRowname[1].replaceAll("(?i)other.{1,2}income.{1,2}and.{1,2}expense", "").trim().length() < 1
				|| matchALLCAPS.find()
				|| (priorRowname[1].split("(\\b[A-Z])").length > 2
						&& priorRowname[1].split("(?i)(plant|equip|propert)").length > 1
						&& rowname[1].split("(?i)(plant|equip|propert)").length > 1)
				|| (priorRowname[1].split("(\\b[^A-Z\\)\\( ;:])").length < 2
						&& priorRowname[1].split("(\\b[A-Z])").length
								/ priorRowname[1].replaceAll("  ", " ").split(" ").length + 1 > .65)) {
			// if row is initial caps \\b[A-Z] and number of initial cap words
			// divided by # of words is more than 2/3 - it is rnh (assuming no
			// data). Length overcounts by 1 - so if 3 words and 2 ws separators
			// - it will count 4 words and 3 ws- this is why I add 1 to length
			// of ws sep.
			// System.xut
			// .println("priorRowname[1].split((\\b[^A-Z\\)\\( ;:])).length=="
			// + priorRowname[1].split("(\\b[^A-Z\\)\\( ;:])").length);
			// System.xut.println("This is rowname header==" + priorRowname[1]);
			isRownameheader = true;
			String[] rnAry = { rowname[0].replaceAll("[ \\s-]{2,10}", " ").trim(), rowname[1].trim(), rowname[2] };
			// System.xut.println("rnAry=" + Arrays.toString(rnAry));
			return rnAry;
		}

		//
		else if (null != rowname[1] && rowname[1].length() > 3 && rowname[1].substring(0, 1).equals(" ")
				&& priorRowname[1].length() > 10) {
			String[] rnAry = { rowname[0],
					priorRowname[1].replaceAll("[ \\s-]{2,10}", " ").trim() + "| "
							+ rowname[1].substring(1, rowname[1].length()).replaceAll("[ ]{2,10}", " ").trim(),
					rowname[2] };
			// System.xut.println("rowname to merge="
			// + rowname[0].replaceAll("[ \\s-]{2,10}", " ").trim()
			// + " prior rowname="
			// + priorRowname[1].replaceAll("[ \\s-]{2,10}", " ").trim());
			// System.xut
			// .println("rowname merged rnAry=" + Arrays.toString(rnAry));
			removePrior = true;
			return rnAry;
		}

		return rowname;
	}

	public String[] getRowname(String line, boolean getRownameEidx) {
		NLP nlp = new NLP();

		int adj = 0;
		String dataPortionOfLine = line;
		String rownamePortionOfLine = line;
		// NLP.pwNLP.append(//NLP.printLn("getting rowname line=====", line + "
		// \rhasTabs=" + hasTabs));
		if (sIdxC1NumOfCsLastCeIdx.size() > 0 && null != sIdxC1NumOfCsLastCeIdx.get(0)[0]
				&& sIdxC1NumOfCsLastCeIdx.get(0)[0] > 0) {

			dataPortionOfLine = line.substring(Math.min(sIdxC1NumOfCsLastCeIdx.get(0)[0], line.length()),
					line.length());
			String[] dataPorLineSp = dataPortionOfLine.split("  ");
			if (dataPorLineSp.length > 0 && dataPorLineSp[0].length() > 0 && dataPorLineSp[0].length() < 5) {
				dataPortionOfLine = line.substring(
						Math.min((sIdxC1NumOfCsLastCeIdx.get(0)[0] + dataPorLineSp[0].length()), line.length()),
						line.length());
				adj = dataPorLineSp[0].length();
			}

			// NLP.pwNLP.append(//NLP.printLn("initial dataPortionOfLine=",
			// dataPortionOfLine));
			boolean resetDataPortionAndRownamePortionOfLine = false;
			if (hasTabs && dataPortionOfLine.replaceAll(" ?[a-zA-Z]{1}", "").length() < dataPortionOfLine.length()) {
				// NLP.pwNLP.append(//NLP.printLn("bad data portion. line=", line));
				List<Integer[]> tmpListInt = nlp.getAllStartAndEndIdxLocations(line,
						Pattern.compile("(?<=([ ]{2,}|\t))\\$?\\(?\\d"));
				// NLP.pwNLP.append(//NLP.printLn("tmpListInt.size=", tmpListInt.size() + ""));
				if (tmpListInt.size() > 0) {
					dataPortionOfLine = line.substring(tmpListInt.get(0)[0]);
					rownamePortionOfLine = line.substring(0, tmpListInt.get(0)[0]);
					// NLP.pwNLP.append(
					//NLP.printLn("is this the start of the dataPortionOfLine line???",
					// dataPortionOfLine));
					// NLP.pwNLP.append(
					//NLP.printLn("is this the start of the rownamePortionOfLine line???",
					// rownamePortionOfLine));
					resetDataPortionAndRownamePortionOfLine = true;
				}
			}

			if (!resetDataPortionAndRownamePortionOfLine) {

				rownamePortionOfLine = line.substring(0,
						Math.min((sIdxC1NumOfCsLastCeIdx.get(0)[0] + adj), line.length()));
			}
		}

		Matcher mDataPortionOfLine = patternNoAlphaSpaceNumberspace.matcher(dataPortionOfLine);
		Matcher mRownamePortionOfLine = nlp.patternAnyVisualCharacter.matcher(rownamePortionOfLine);

		// System.xut.println("rownamePortionOfLine=" + rownamePortionOfLine
		// + "\r getRownameEidx=" + getRownameEidx);
		// if (rownameEIdx < rownamePortionOfLine.length() && getRownameEidx)
		// rownameEIdx = rownamePortionOfLine.length();
		// System.xut.println("rownameEIdx=" + rownameEIdx);

		// get the first instance of 3 whitespace prior to a number. Can't use
		// comma b/c it is in #s
		Pattern patternAlpha = Pattern.compile("[A-Za-z;:\\(\\)]");
		// System.xut.println("getRownameEidx=" + getRownameEidx);
		String rowname;
		booleanData = false;
		int rnEndIdx, rnStartIdx = 0;
		if (mDataPortionOfLine.find()) {
			// System.xut.println("found data portion="
			// + mDataPortionOfLine.group());
			if (mRownamePortionOfLine.find()) {
				rnStartIdx = mRownamePortionOfLine.start();
				if (getRownameEidx) {
					// rownamePortionOfLine is still full line
					Matcher mAlpha = patternAlpha.matcher(rownamePortionOfLine);
					// System.xut.println("rownamePortionOfLine for mAlpha
					// matching=="+rownamePortionOfLine);
					// System.xut.println("rownameEidx bef mAlpha="+rownameEIdx);
					while (mAlpha.find()) {
						if (mAlpha.end() > rownameEIdx) {
							rownameEIdx = mAlpha.end();
						}
					}
				}
				// System.xut.println("mAlpha rownameEidx="+rownameEIdx);
			}
			rnEndIdx = mDataPortionOfLine.start();
			rowname = rownamePortionOfLine.replaceAll("[ ]{2,}", " ");
			booleanData = true;
			String[] rnAry = { rnEndIdx + "", rowname, rnStartIdx + "" };
			// in below return of rowname I have rowname.length and not rowname
			// endIdx. Are they the same? Numbers look wrong. See mapData
			// System.xut.println("A
			// rnEndIdx,rowname,rnStartIdx="+rnEndIdx+"|"+rowname+"|"+rnStartIdx);
			// System.xut.println("booleanData=" + booleanData + " rowname="
			// + rowname);
			return rnAry;
		}

		else {
			// System.xut.println("rowname=line line=" + line);
			String[] lineSp = line.split("     \\d");
			for (int i = 0; i < lineSp.length; i++) {
				// System.xut.println("split #=" + i + " lineSp.len="
				// + lineSp.length + " lineSp=" + lineSp[i]);
			}

			if (lineSp.length > 0)
				rowname = lineSp[0];
			else
				rowname = line;

			// System.xut.println("rowname after split=" + rowname);
			if (mRownamePortionOfLine.find()) {
				rnStartIdx = mRownamePortionOfLine.start();
			}
			String[] rnAry = { rowname.length() + "", rowname, rnStartIdx + "" };
			// System.xut.println("B rnEndIdx,rowname,rnStartIdx="+Arrays.toString(rnAry));
			// System.xut.println("booleanData="+booleanData);
			return rnAry;
		}
	}

	public List<Integer[]> getsIdxC1NumOfCsLastCeIdx(String tableText, boolean redoMap) throws IOException {
		// System.xut.println("tableText======"+tableText);
		NLP nlp = new NLP();

		mapNumberOfDataCols.clear();

		// this will create map that counts the number of times an endIdx
		// occurs in tableText - this is used to determine rt boundary of each
		// datacol. If more than 7 instances of same endIdx then
		// that is rt edge of a col. Also finds the left most instance of
		// first col to determine where rowname's must end by! Rowname endIdx
		// can then be used to tell parser where not to look for CHs.

		// only remove from data cols: parens,$, minus sign (-) etc and replace
		// w/ ws. Must have these conditions else endup with 2 ws in rowname.

		// NLP.pwNLP.append(//NLP.printLn("redoMap=" + redoMap
		// + " tableText before replace <![a-zA=", tableText));

		String tmpStr = tableText;

		if (!redoMap) {
			tableText = tableText.replaceAll("(?<![a-zA-Z].{0,2})(0(?=\\.)|\\$|\\)|\\(|\\-)(?!.+([A-Za-z]{1,}))", " ");
		}

		if (redoMap) {
			tableText = tableText.replaceAll("(?sm)(?<=(\t | \t|\t|  ))\\-\\-?(?=(\t | \t|\t|  |$))", "0");
			tableText = tableText.replaceAll("(?<![a-zA-Z].{0,2})(0(?=\\.)|\\$|\\)|\\()(?!.+([A-Za-z]{1,}))", " ");
		}

		// NLP.pwNLP.append(//NLP.printLn("redoMap=", redoMap + " tableText after replace
		// <![a-zA=" + tableText));

		String[] tableTextRows = tableText.split("[\r\n]");
		String[] tmpStrSplit = tmpStr.split("[\r\n]");

		String line, line2;
		String[] rowname;

		mapDataColIdxLocs.clear();
		mapDataColIdxLocs = new TreeMap<Integer, Integer[]>();

		for (int i = 0; i < tableTextRows.length; i++) {
			line = tableTextRows[i];
			line2 = tmpStrSplit[i];
			if (line.replaceAll("[ ]{2,}", "").length() < 2)
				continue;
			// System.xut.println("getting rownameEidx (getRowname=True)");
			rowname = getRowname(line, true);
			// System.xut.println("prior to matching data cols line is=" +
			// line);
			List<String[]> dataList = nlp.getAllEndIdxAndMatchedGroupLocs(line, patternNoAlphaSpaceNumberspace);
			List<String[]> dataList2 = nlp.getAllEndIdxAndMatchedGroupLocs(line2, patternNoAlphaSpaceNumberspace);

			int lastIdx = Integer.parseInt(rowname[0]), endIdxData, startIdxData, priorEndDataIdx;

			String tmpS = "";

			// mapNumberOfDataCols is:
			// counts nmb of data cols per row (dataList.size). key=nmb of data
			// cols and value is count of row this number of data cols occurs.
			// key=nmb of data cols, val=nmb of rows w/ this many data cols
			// System.xut.println("dataList.size="+dataList.size()+" -- line==="+line);
			if (dataList2.size() > 0) {
				if (mapNumberOfDataCols.containsKey(dataList2.size())) {
					mapNumberOfDataCols.put(dataList2.size(), mapNumberOfDataCols.get(dataList2.size()) + 1);
					// NLP.pwNLP.append(//NLP.printLn("dataList2.size=", dataList2.size() + "\r --
					// data line2===" + line2));
				} else {
					mapNumberOfDataCols.put(dataList2.size(), 1);
					// NLP.pwNLP.append(
					//NLP.printLn("else dataList2.size=", dataList2.size() + "\r -- data line2==="
					// + line2));
				}
			}

			for (int c = 0; c < dataList.size(); c++) {
				tmpS = dataList.get(c)[1];
				// System.xut.println("will measure eIdx against this data="+tmpS);
				String pattern = tmpS;
				// System.xut.println("B matching dataCol patterns="+pattern);
				Pattern patternData = Pattern.compile(pattern);
				// can only match against remaining portion of line
				Matcher matcher = patternData.matcher(line.substring(lastIdx));
				// System.xut.println("remaining portion of line to match
				// against="+line.substring(lastIdx));
				if (matcher.find()) {
					startIdxData = matcher.start() + lastIdx;
					// this will count number of times an endIdx occurs (and
					// treats endIdx w/ 2 chars of each other as the same).
					// If endIdx occurs>7 times - it is rt edg of data col. The
					// number of unique instances this occurs are the no of data
					// cols. Also measures startIdx of data col and returns
					// lowest value to establish where a rowname must end before

					endIdxData = matcher.end() + lastIdx;
					lastIdx = endIdxData;

					// PICKUP HERE - FIGURE OUT WHAT DATA COL MAP SHOULD LOOK
					// LIKE - COUNT NUMBER OF EIDX FOR EACH COL!

					// System.xut.println("match="+matcher.group()+" mapDataColIdxLocs endIdxData="
					// + endIdxData);
					if (mapDataColIdxLocs.containsKey(endIdxData)) {
						priorEndDataIdx = mapDataColIdxLocs.get(endIdxData)[1];
						if (priorEndDataIdx > 0 && priorEndDataIdx > startIdxData) {
							Integer[] ary = { mapDataColIdxLocs.get(endIdxData)[0] + 1, startIdxData };
							mapDataColIdxLocs.put(endIdxData, ary);

						} else {
							Integer[] ary = { mapDataColIdxLocs.get(endIdxData)[0] + 1, priorEndDataIdx };
							mapDataColIdxLocs.put(endIdxData, ary);
						}
					} else if (mapDataColIdxLocs.containsKey(endIdxData + 1)) {
						priorEndDataIdx = mapDataColIdxLocs.get(endIdxData + 1)[1];
						if (priorEndDataIdx > 0 && priorEndDataIdx > startIdxData) {
							Integer[] ary = { mapDataColIdxLocs.get(endIdxData + 1)[0] + 1, startIdxData };
							mapDataColIdxLocs.put(endIdxData + 1, ary);

						} else {
							Integer[] ary = { mapDataColIdxLocs.get(endIdxData + 1)[0] + 1,
									mapDataColIdxLocs.get(endIdxData + 1)[1] };
							mapDataColIdxLocs.put(endIdxData + 1, ary);

						}
					} else if (mapDataColIdxLocs.containsKey(endIdxData + 2)) {
						priorEndDataIdx = mapDataColIdxLocs.get(endIdxData + 2)[1];
						if (priorEndDataIdx > 0 && priorEndDataIdx > startIdxData) {
							Integer[] ary = { mapDataColIdxLocs.get(endIdxData + 2)[0] + 1, startIdxData };
							mapDataColIdxLocs.put(endIdxData + 2, ary);
						} else {
							Integer[] ary = { mapDataColIdxLocs.get(endIdxData + 2)[0] + 1, priorEndDataIdx };
							mapDataColIdxLocs.put(endIdxData + 1, ary);
						}
					} else if (mapDataColIdxLocs.containsKey(endIdxData - 1)) {
						priorEndDataIdx = mapDataColIdxLocs.get(endIdxData - 1)[1];
						if (priorEndDataIdx > 0 && priorEndDataIdx > startIdxData) {
							Integer[] ary = { mapDataColIdxLocs.get(endIdxData - 1)[0] + 1, startIdxData };
							mapDataColIdxLocs.put(endIdxData - 1, ary);
						} else {
							Integer[] ary = { mapDataColIdxLocs.get(endIdxData - 1)[0] + 1, priorEndDataIdx };
							mapDataColIdxLocs.put(endIdxData - 1, ary);
						}

					} else if (mapDataColIdxLocs.containsKey(endIdxData - 2)) {
						priorEndDataIdx = mapDataColIdxLocs.get(endIdxData - 2)[1];
						if (priorEndDataIdx > 0 && priorEndDataIdx > startIdxData) {
							Integer[] ary = { mapDataColIdxLocs.get(endIdxData - 2)[0] + 1, startIdxData };
							mapDataColIdxLocs.put(endIdxData - 2, ary);
						} else {
							Integer[] ary = { mapDataColIdxLocs.get(endIdxData - 2)[0] + 1, priorEndDataIdx };
							mapDataColIdxLocs.put(endIdxData - 2, ary);
						}
					} else {
						Integer[] ary = { 1, startIdxData };
						mapDataColIdxLocs.put(endIdxData, ary);

					}
				}
			}
		}

		//NLP.printMapIntIntAry("at mapDataColIdxLocs: key=endIdx,val=[0]=count,val[1]startIdx", mapDataColIdxLocs);

		//NLP.printMapIntInt(
//				"at mapNumberOfDataCols: key=number of data cols,value=count of rows this number of data cols found",
//				mapNumberOfDataCols);

		int noOfDataCols = 0, rownameEndIdx = -1, lastColEndIdx = -1;

		// List<Integer[]> listDataColsEndIdxCount = new ArrayList<>();

		// if rownameEndIdx are w/n 3 of each other - it will not count as a new
		// rowname endIdx (and therefore not a datacol).
		// map=key=endidx, value[0]=count number of times endidx occurs and
		// value[1] is smallest startIdx of the idx grp - this tells us where
		// rowname ends.

		int prevDataColIdx = 0, prevrownameEndIdx = 999, dataColmnEIdx = 0, dataColmnSIdx = 0, cnt = 0, minus3 = 3;

		// if it occurred more than 7 times - it is a data col idx - the last
		// one is the last data col idx. The first is the 1st etc. Currently I
		// get the first but subtract 2

		listDataColsSIdxandEIdx = new ArrayList<>();

		// see other instance where I filter by 7 (so don't change this!).
		// search 'filter by' This also filters instances where distance is not
		// >4
		for (Map.Entry<Integer, Integer[]> entry : mapDataColIdxLocs.entrySet()) {
			if (entry.getValue()[0] >= 5) {
				if (cnt == 0 || prevrownameEndIdx > (entry.getValue()[1] - minus3)) {

					rownameEndIdx = (entry.getValue()[1] - minus3);
					// minus 3 b/c this is the startIdx of 1st data col and it
					// is used to mark end of rowname. So minus3 adjust mark
					// back so don't overlap data col

				}
				prevrownameEndIdx = rownameEndIdx;

				cnt++;

				dataColmnEIdx = entry.getKey();
				dataColmnSIdx = entry.getValue()[1];

				if (Math.abs(prevDataColIdx - dataColmnEIdx) > 4) {
					Integer[] iAry = { dataColmnSIdx, dataColmnEIdx };
					listDataColsSIdxandEIdx.add(iAry);
				}
				// System.xut.println("listDataColsSIdxandEIdx");
				//NLP.printListOfIntegerArray(listDataColsSIdxandEIdx);

				// System.xut.println("dataColmnSIdx=" + dataColmnSIdx
				// + " dataColmnEIdx=" + dataColmnEIdx + " dataColmnIdx="
				// + dataColmnEIdx + " dif="
				// + Math.abs(prevDataColIdx - dataColmnEIdx));
				if (Math.abs(prevDataColIdx - dataColmnEIdx) > 4) {
					noOfDataCols++;
					lastColEndIdx = entry.getKey();
				}

				prevDataColIdx = dataColmnEIdx;

				// System.xut.println(" data column found at this endIdx="
				// + dataColmnEIdx
				// + " column found this endIdx this many times="
				// + entry.getValue()[0]);
			}
		}

		// this has list of data col eIdx w/ removal of duplicates (duplicates
		// are those whose eIdx are w/n 4)
		// System.xut.println("C listDataColsSIdxandEIdx");
		//NLP.printListOfIntegerArray(listDataColsSIdxandEIdx);

		// System.xut.println("noOfDataCols|" + noOfDataCols + " rownameEndIdx="
		// + rownameEndIdx + " lastColEndIdx=" + lastColEndIdx);
		List<Integer[]> listStartIdxFirstColandNoDataCols = new ArrayList<>();
		Integer[] ary = { rownameEndIdx, noOfDataCols, lastColEndIdx };
		listStartIdxFirstColandNoDataCols.add(ary);

		Map<Integer, Integer[]> mapDataColIdxLocs3 = new TreeMap<Integer, Integer[]>();

		// listDataColsSIdxandEIdx has eliminated duplicate eIdx (those that are
		// 3 apart) and defaulted count to 99
		for (int n = 0; n < listDataColsSIdxandEIdx.size(); n++) {
			// NLP.pwNLP.append(//NLP.printLn("listDataColsSIdxandEIdx.get(n)[0]=",
			// listDataColsSIdxandEIdx.get(n)[0] + ""));
			Integer[] intAry = { 99, listDataColsSIdxandEIdx.get(n)[0] };
			mapDataColIdxLocs3.put(listDataColsSIdxandEIdx.get(n)[1], intAry);
		}

		// System.xut.println("B. printing mapDataColIdxLocs==");
		//NLP.printMapIntIntAry(mapDataColIdxLocs);
		// if each count of a data col eIdx is w/n 1 or 2 of each other and all
		// are greater than 4 accept table?
		mapDataColIdxLocs = mapDataColIdxLocs3;

		//NLP.printMapIntIntAry("C mapDataColIdxLocs", mapDataColIdxLocs);
		return listStartIdxFirstColandNoDataCols;

	}

	public static List<String> getMonthYearEndedColHdg(String startGroupLimited, TreeMap<Integer, List<String[]>> map,
			int totalColumns) throws IOException, ParseException {
		NLP nlp = new NLP();

		mapYearStartIdx.clear();

		//NLP.printMapIntListOfStringAry("@getMonthYearEndedColHdg - map", map);

		//NLP.printMapIntListOfStringAry("@getMonthYearEndedColHdg", map);

		boolean monthYearDone = false, monthYearEndedDone = false;

		// System.xut.println("getMonthYearEnded");

		/*
		 * METHOD: after removing any inception columns - I first pair year and month.
		 * if month year pair successful - attempt to pair ended column. if not
		 * successful in pairing ended - return month year after adding back incept
		 * column (if there is such a column). This works whether inception column or
		 * not. When there is an inception col this method identifies all related rows
		 * with MP very close to inception MP. It then temporarily removes that column -
		 * and I run month,year, ended pairing. Once run inception column is added back
		 */

		// NLP.pwNLP.append(//NLP.printLn(
		// "@nlp.getMonthYearEndedColHdg counting startgroup ended and month --text of
		// is startGroupLimited=",
		// startGroupLimited));
		boolean firstCHLine = false;
		startGroupLimited = startGroupLimited.replaceAll("\r\n|\n\r", "\r");
		String[] startGroupLimitedSplit = startGroupLimited.split("\r");
		String line = "", startGroupReconstituted = "";
		// System.xut.println("startGroupLimitedSplit.len="
		// + startGroupLimitedSplit.length);
		for (int i = 0; i < startGroupLimitedSplit.length; i++) {
			// reconstitute so that I don't pickup earlier than end of tablename
			// certainly and ideally at start of CH row -- which is any row with
			// 4 ws anyVis 4 ws AnyVis. Or any visa that start more than 80
			// chars from start of line
			line = startGroupLimitedSplit[i];
			if (firstCHLine) {
				startGroupReconstituted = startGroupReconstituted + " " + line + "\r";
				continue;
			}

			if (line.split("    [\\p{Alnum}\\p{Punct}]").length > 1) {
				firstCHLine = true;
				startGroupReconstituted = line + "\r";
			}
			if (line.split("[ ]{80,130}[\\p{Alnum}\\p{Punct}]").length > 1) {
				firstCHLine = true;
				startGroupReconstituted = line + "\r";
			}
		}

		map = NLP.getStrandedMoDayYear(startGroupReconstituted, map, startGroupLimitedSplit.length);

		Integer cntM = 0, cntY = 0, cntE = 0, cntE2 = 0, cntInception = 0, cntOddBall = 0;

		// NLP.pwNLP.append(//NLP.printLn("@nlp.getMonthYearEndedColHdg --
		// startGroupLimited", startGroupLimited));
		cntInception = (nlp.getAllMatchedGroups(startGroupLimited, NLP.patternInception)).size();
		// NLP.pwNLP.append(//NLP.printLn("cntInception=", "" + cntInception));

		cntM = (nlp.getAllMatchedGroups(startGroupLimited, TableParser.MonthPatternSimple)).size();
		cntY = nlp.getAllMatchedGroups(startGroupLimited, TableParser.YearOrMoDayYrPattern).size();
		// System.xut.println("cntY=" + cntY);

		Map<Integer, String> mapMonth = new TreeMap<Integer, String>();
		Map<Integer, String> mapYear = new TreeMap<Integer, String>();
		Map<Integer, String> mapEnded = new TreeMap<Integer, String>();
		Map<Integer, String> mapInception = new TreeMap<Integer, String>();

		// POPULATEs month and year maps
		for (int i = map.size() - 1; i >= 0; i--) {
			List<String[]> tmpL = map.get(i);
			// multiply key by 1000 and add mp. lowest value is col1 and if two
			// items in map share same first dig - same row and map is in error
			// (can't have two years on same row for example)
			for (int c = 0; c < tmpL.size(); c++) {
				cntM = (nlp.getAllMatchedGroups(tmpL.get(c)[1], TableParser.MonthPatternSimple)).size();
				cntY = (nlp.getAllMatchedGroups(tmpL.get(c)[1], TableParser.YearOrMoDayYrPattern)).size();
				cntE = (nlp.getAllMatchedGroups(tmpL.get(c)[1], TableParser.EndedHeadingPattern)).size();
				cntE2 = (nlp.getAllMatchedGroups(tmpL.get(c)[1],
						Pattern.compile("(?i)(to |from|through|fiscal|quarter)"))).size();
				cntOddBall = (nlp.getAllMatchedGroups(tmpL.get(c)[1], TableParser.OddColumnPattern)).size();

				cntE = cntE + cntE2;

				cntInception = (nlp.getAllMatchedGroups(tmpL.get(c)[1], NLP.patternInception)).size();

				// NLP.pwNLP
				// .append(//NLP.printLn("looping through mapAllCH2 to get E,M, Y counts - current
				// text in loop is=",
				// tmpL.get(c)[1] + "||did if find cntE=" + cntE + " or cntY=" + cntY + " or
				// cntM=" + cntM
				// + " or cntInception=" + cntInception));

				int row = 0, mp = 0;
				if (cntM > 0 && cntY < 1) {
					row = Integer.parseInt(tmpL.get(c)[2]) * 1000;
					mp = Integer.parseInt(tmpL.get(c)[0]) - tmpL.get(c)[1].length() / 2;
					mapMonth.put(row + mp, tmpL.get(c)[1]);
					// NLP.pwNLP.append(//NLP.printLn("a mapMonth.put=", tmpL.get(c)[1] + " mp=" +
					// mp));
					cntM = 0;
				}

				if (cntY > 0) {
					row = Integer.parseInt(tmpL.get(c)[2]) * 1000;
					mp = Integer.parseInt(tmpL.get(c)[0]) - tmpL.get(c)[1].length() / 2;
					mapYear.put(row + mp, tmpL.get(c)[1]);
					// NLP.pwNLP.append(//NLP.printLn("mapYear.put=", tmpL.get(c)[1] + " mp=" + mp));
					cntY = 0;
					cntE = 0;
				}

				if (cntE > 0 /* || cntE2 > 0) */) {
					// NLP.pwNLP.append(//NLP.printLn("adding E=", tmpL.get(c)[1]));
					row = Integer.parseInt(tmpL.get(c)[2]) * 1000;
					mp = Integer.parseInt(tmpL.get(c)[0]) - tmpL.get(c)[1].length() / 2;
					mapEnded.put(row + mp, tmpL.get(c)[1]);
					// NLP.pwNLP.append(//NLP.printLn("mapEndd.put=", tmpL.get(c)[1] + " mp=" + mp));
					cntE = 0;
					cntE2 = 0;
				}

				if (cntInception > 0) {
					row = Integer.parseInt(tmpL.get(c)[2]) * 1000;
					mp = Integer.parseInt(tmpL.get(c)[0]) - tmpL.get(c)[1].length() / 2;
					mapInception.put(row + mp, tmpL.get(c)[1]);
					// NLP.pwNLP.append(//NLP.printLn("mapInception.put=", tmpL.get(c)[1] + " mp=" +
					// mp));
					cntInception = 0;
				}
			}
		}

		// NLP.pwNLP.append(//NLP.printLn("2 mapInception.size=", mapInception.size() +
		// ""));

		/*
		 * 1st pass attempted to find inception column using map (mapAllCH2). Which may
		 * have missed it - so try one more pass using startGroupCHLines.
		 */
		if (mapInception.size() < 1 && nlp.getAllMatchedGroups(startGroupLimited, NLP.patternInception).size() > 0) {
			// NLP.pwNLP.append(//NLP.printLn("startGroupLimited=", startGroupLimited));
			String[] sgSplit = startGroupLimited.replaceAll("\r\n|\n", "\r").replaceAll("\r\r", "\r").split("\r");
			String lineSG = "";
			int mp = -1;
			int row = 0;
			for (int i = 0; i < sgSplit.length; i++) {
				lineSG = sgSplit[i];
				List<String[]> listIncep = nlp.getAllEndIdxAndMatchedGroupLocs(lineSG, NLP.patternInception);
				// NLP.pwNLP.append(//NLP.printLn("line to recheck incep. line=\r", lineSG));
				// NLP.pwNLP.append(//NLP.printLn("listIncep.size=", "" + listIncep.size()));
				if (listIncep.size() > 0) {
					// NLP.pwNLP.append(//NLP.printLn("incep match=", listIncep.get(0)[1]));
					mp = Integer.parseInt(listIncep.get(0)[0]) - listIncep.get(0)[1].length() / 2;
					row = (i + 1) * 1000;
					mapInception.put(row + mp, listIncep.get(0)[1]);
				}
			}
		}

		//NLP.printMapIntStr("1@nlp mapYear=", mapYear);
		//NLP.printMapIntStr("1@nlp mapMonth=", mapMonth);
		//NLP.printMapIntStr("1@nlp mapEnded=", mapEnded);
		//NLP.printMapIntStr("1@nlp mapInception=", mapInception);
		Map<Integer, String> mapMonthYear = new TreeMap<Integer, String>();
		Map<Integer, String> mapMonthYearEnded = new TreeMap<Integer, String>();
		Map<Integer, String> mapFinal = new TreeMap<Integer, String>();

		// if only mapYear - return it.
		if (mapYear.size() > 0 && mapYear.size() == totalColumns && (mapMonth == null || mapMonth.size() < 1)
				&& (mapEnded == null || mapEnded.size() < 1) && (mapInception == null || mapInception.size() < 1)) {

			Map<Integer, String> mapReorder = new TreeMap<Integer, String>();

			int key = 0;
			for (Map.Entry<Integer, String> entry : mapYear.entrySet()) {
				key = entry.getKey();
				mapReorder.put(key - key / 1000 * 1000, entry.getValue());
				// System.xut.println("entry.getValue=" + entry.getValue()
				// + " Key=" + key);
			}

			//NLP.printMapIntStr("@nlp.getMonthYearEndedColHdg reordered map -mapReorder", mapReorder);

			if (null != mapReorder && mapReorder.size() > 0) {
				listCHbyIdxLoc = new ArrayList<String>();
				for (Map.Entry<Integer, String> entry : mapReorder.entrySet()) {
					if (null != entry.getValue())
						listCHbyIdxLoc.add(entry.getValue());
				}
			}

			//NLP.printListOfString("returned listCHbyIdxLoc", listCHbyIdxLoc);
			return listCHbyIdxLoc;
		}

		// set clear mapEnded if no mapMonth b/c mapEnded is paired last if
		// prior is paired.
		boolean mapMonthDone = false;
		if (mapMonth.size() == 0 && mapEnded.size() > 0) {
			mapMonth = mapEnded;
			mapMonthDone = true;
			mapEnded = new TreeMap<Integer, String>();
		}

		// mapYear can have both year and month - in which case- I want to keep
		// it. Else if it is just year - return null! This can then fetch single
		// CHs on on two different lines -so there's no need for blocked out
		// sectoin above

		if (mapYear == null || mapYear.size() < 1
				&& (nlp.getAllMatchedGroups(startGroupLimited, TableParser.MonthPatternSimple).size() < 1
						&& nlp.getAllMatchedGroups(startGroupLimited, TableParser.MoDayYrSimplePattern).size() < 1)) {
			// NLP.pwNLP.append(//NLP.printLn("1@getMonthYearEndedColHdg return null", ""));
			return null;
		}

		// if no mapYear but mo/day/yr pattern - set mapYear=mapMonth and
		// monthYearDone so it doesn't attempt to pair mapYear w/ mapMonth and
		// pickup at attempt to pair with end
		if (mapYear == null || mapYear.size() < 1
				&& (/*
					 * nlp.getAllMatchedGroups(startGroupLimited,
					 * TableParser.MonthPatternSimple).size() < 1 &&
					 */nlp.getAllMatchedGroups(startGroupLimited, TableParser.MoDayYrSimplePattern).size() > 0)) {
			//NLP.printMapIntStr("@getMonthYearEndedColHdg mapYear set to mapMonth. now mapYear=", mapYear);
			mapMonthYear = mapMonth;
			mapMonth = new TreeMap<Integer, String>();
			monthYearDone = true;
		}

		//NLP.printMapIntStr("2@nlp mapYear=", mapYear);
		//NLP.printMapIntStr("2@nlp mapMonth=", mapMonth);
		//NLP.printMapIntStr("2@nlp mapEnded=", mapEnded);
		//NLP.printMapIntStr("2@nlp mapInception=", mapInception);

		// This gets midpoint of column w/ 'inception'. inceptMp is used as
		// beacon to remove all other words in that column further below.
		int incepMp = 0;
		if (null != mapInception && mapInception.size() > 0) {
			// only works where one inception value
			int incepKey;
			for (Map.Entry<Integer, String> incepEntry : mapInception.entrySet()) {
				incepKey = incepEntry.getKey();

				if (incepMp < incepKey - (incepKey / 1000) * 1000)
					incepMp = incepKey - (incepKey / 1000) * 1000;
			}

			// NLP.pwNLP.append(//NLP.printLn("@nlp.getMonthYearEndedColHdg incepMp=", incepMp
			// + ""));

		}

		// remove any keys that have mp w/n 3 of inception mid pt. When
		// finished - add to map a dummy inception header with inception mp
		// this removes the column that is 'inception' which is what complicates
		// CH pairing across rows. getListOfKeysToRemove is run on each map
		// (mapMonth, mapYear and mapEnd) and removes corresponding key/value
		// from map if that column is close to incepMp

		// REMOVES INCEPTION COLUMN
		List<String[]> listKeysToRemove = new ArrayList<>();
		Map<Integer, String> mapInceptionAllRelatedRows = new TreeMap<Integer, String>();
		if (null != mapMonth && mapMonth.size() > 0 && incepMp > 0 && null != mapInception && mapInception.size() > 0) {
			// this method gets any month values in INCEPT column to be
			// excluded.
			// NLP.pwNLP.append(//NLP.printLn("starting process of removing incep keys", "-
			// here"));
			listKeysToRemove = NLP.getListOfKeysToRemove(mapMonth, incepMp);
			if (null != listKeysToRemove && listKeysToRemove.size() > 0) {
				for (int i = 0; i < listKeysToRemove.size(); i++) {
					// INCEPT columns removed from mapMonth.
					mapMonth.remove(Integer.parseInt(listKeysToRemove.get(i)[0]));
				}

				// this map will retain any INCEPT columns removed.
				// Later mapInceptionAllRelatedRows used to add back.
				mapInceptionAllRelatedRows = NLP.getAllInceptionRelatedRows(listKeysToRemove,
						mapInceptionAllRelatedRows);
			}

			if (null != mapYear && mapYear.size() > 0 && incepMp > 0 && null != mapInception
					&& mapInception.size() > 0) {
				listKeysToRemove = NLP.getListOfKeysToRemove(mapYear, incepMp);
				if (null != listKeysToRemove && listKeysToRemove.size() > 0) {
					for (int i = 0; i < listKeysToRemove.size(); i++) {
						mapYear.remove(Integer.parseInt(listKeysToRemove.get(i)[0]));
					}

					mapInceptionAllRelatedRows = NLP.getAllInceptionRelatedRows(listKeysToRemove,
							mapInceptionAllRelatedRows);
				}
			}

			if (null != mapEnded && mapEnded.size() > 0 && incepMp > 0 && null != mapInception
					&& mapInception.size() > 0) {
				listKeysToRemove = NLP.getListOfKeysToRemove(mapEnded, incepMp);
				if (null != listKeysToRemove && listKeysToRemove.size() > 0) {
					for (int i = 0; i < listKeysToRemove.size(); i++) {
						mapEnded.remove(Integer.parseInt(listKeysToRemove.get(i)[0]));
					}

					mapInceptionAllRelatedRows = NLP.getAllInceptionRelatedRows(listKeysToRemove,
							mapInceptionAllRelatedRows);
				}
			}

			// System.xut.println("mapIncep=====" + mapInception);
			if (null != mapEnded && mapEnded.size() > 0 && incepMp > 0 && null != mapInception
					&& mapInception.size() > 0) {
				listKeysToRemove = NLP.getListOfKeysToRemove(mapInception, incepMp);
				if (null != listKeysToRemove && listKeysToRemove.size() > 0) {
					mapInceptionAllRelatedRows = NLP.getAllInceptionRelatedRows(listKeysToRemove,
							mapInceptionAllRelatedRows);
					// System.xut.println("mapInceptionAllRelatedRows.size====="
					// + mapInceptionAllRelatedRows.size());

				}
			}

		}

		//NLP.printMapIntStr("3@nlp  - mapInceptionAllRelatedRows", mapInceptionAllRelatedRows);
		//NLP.printMapIntStr("3@nlp after inception col removed - mapYear=", mapYear);
		//NLP.printMapIntStr("3@nlp after inception col removed - mapMonth=", mapMonth);
		//NLP.printMapIntStr("3@nlp after inception col removed - mapEnded=", mapEnded);

		// YEAR MONTH LOOP: loop thru mapYear and compare to mapMonth. Here
		// I can change setup so that it also checks for 2 months and three
		// years.
		cntM = mapMonth.size();
		cntY = mapYear.size();
		cntE = mapEnded.size();
		cntInception = mapInception.size();

		// idx locations key-(1st integer of key*1000).

		// NLP.pwNLP.append(
		//NLP.printLn("@nlp.getMonthYearEndedColHdg -- after removal of inception --
		// cntM=", cntM + " cntY="
		// + cntY + " cntE=" + cntE + " totalColumns=" + totalColumns + " cntInception="
		// + cntInception));

		// getEnded is global setting - set once here
		boolean getEnded = false;
		if (null != mapEnded && mapEnded.size() > 0 && !tableNameShort.equals("bs")) {
			getEnded = true;
		}
		// NLP.pwNLP.append(//NLP.printLn("tableNameShort=", tableNameShort + " getEnded="
		// + getEnded));

		/*
		 * Call ratio merge (eg - mergeOneToOne, mergeTwoToOne, etc). If month year
		 * merge is successful (monthYearDone=true) I then cycle thorugh all other ratio
		 * merge if getEnded=true (passing mapMonthYear and mapEnded) until ended
		 * paired. If successful (monthYearEndedDone) I add back any incep col and
		 * reorder map or if not successful I set mapFinal to mapMonthYear. Get to point
		 * where I don't filter Each merge ratio asks if monthYear/Ended Done and if it
		 * is will return lgMap (mapYear or mapYearMonth). each merge ratio method will
		 * also ask if necessary conditions are met and if not return null. Null result
		 * will then pass thru monthYeardone and return false.
		 */

		// largest smallest
		allColHeadingsCaptured = false;

		// 1:1 merge month year
		// 1:1 ratio so pass as largest, smallest
		// don't use 1:1 if number of cols >3
		// MergeOneToOne

		// If I want to use startIdx to pair CHs I need to record initial
		// startIdx of large group - which I assume for purposes of this method
		// to be mapYear. StartIdx can't be calculated after I merge 2 CHs b/c I
		// carry the original MP but after merge length changes which corrupts
		// ability to calc sIdx

		mapYearStartIdx = new TreeMap<Integer, Double>();
		int k = 0;
		double sIdx;
		String val = "";
		for (Map.Entry<Integer, String> entry : mapYear.entrySet()) {
			k = entry.getKey(); // contains mp
			val = entry.getValue();
			sIdx = (k - (k / 1000) * 1000) - val.length() / 2;
			mapYearStartIdx.put(k, sIdx);
		}

		mapMonthYear = NLP.mergeOneToOne(mapYear, mapMonth, monthYearDone, totalColumns, cntM, cntY, cntInception);
		monthYearDone = NLP.monthYearFound(mapMonthYear);
		monthYearDone = NLP.allColHeadingsCaptured(startGroupLimited, mapMonthYear);

		//NLP.printMapIntStr("@after mergeOneToOne - mapMonthYear=", mapMonthYear);
		// NLP.pwNLP.append(//NLP.printLn("monthYearDone=", monthYearDone + ""));

		// smallest largest
		if (!monthYearDone) {
			mapMonthYear = NLP.mergeTwoToOne(mapMonth, mapYear, monthYearDone, cntM, cntY);
			monthYearDone = NLP.monthYearFound(mapMonthYear);
			monthYearDone = NLP.allColHeadingsCaptured(startGroupLimited, mapMonthYear);
			//NLP.printMapIntStr("@after mergeColHdgsTwoToOneRatio - mapMonthYear=", mapMonthYear);
		}

		if (!monthYearDone) {
			mapMonthYear = NLP.mergeThreeToOne(mapMonth, mapYear, monthYearDone, cntM, cntY);
			monthYearDone = NLP.monthYearFound(mapMonthYear);
			if (!monthYearDone) {
				monthYearDone = NLP.allColHeadingsCaptured(startGroupLimited, mapMonthYear);
			}
			//NLP.printMapIntStr("@after mergeColHdgsThreeToOneRatio - mapMonthYear=", mapMonthYear);
		}

		if (!monthYearDone) {
			mapMonthYear = NLP.mergeThreeToTwo(mapMonth, mapYear, monthYearDone, cntM, cntY);
			monthYearDone = NLP.monthYearFound(mapMonthYear);
			if (!monthYearDone) {
				monthYearDone = NLP.allColHeadingsCaptured(startGroupLimited, mapMonthYear);
			}
			//NLP.printMapIntStr("@after mergeColHdgsThreeToTwoRatio - mapMonthYear=", mapMonthYear);
		}

		if (!monthYearDone) {
			mapMonthYear = NLP.mergeFourToThree(mapMonth, mapYear, monthYearDone, cntM, cntY);
			monthYearDone = NLP.monthYearFound(mapMonthYear);
			if (!monthYearDone) {
				monthYearDone = NLP.allColHeadingsCaptured(startGroupLimited, mapMonthYear);
			}
			//NLP.printMapIntStr("@after mergeColHdgsThreeToTwoRatio - mapMonthYear=", mapMonthYear);
		}

		if (!monthYearDone) {
			mapMonthYear = NLP.mergeFiveToThree(mapMonth, mapYear, monthYearDone, cntM, cntY);
			monthYearDone = NLP.monthYearFound(mapMonthYear);
			if (!monthYearDone) {
				monthYearDone = NLP.allColHeadingsCaptured(startGroupLimited, mapMonthYear);
			}
			//NLP.printMapIntStr("@after mergeColHdgsFiveToThree - mapMonthYear=", mapMonthYear);
		}

		// NLP.pwNLP.append(//NLP.printLn("getEnded", "" + getEnded + " monthYearDone=" +
		// monthYearDone));
		if (getEnded && monthYearDone) {

			if (!monthYearEndedDone) {
				// largest smallest
				mapMonthYearEnded = NLP.mergeOneToOne(mapMonthYear, mapEnded, monthYearEndedDone, totalColumns,
						mapEnded.size(), mapMonthYear.size(), cntInception);
				monthYearEndedDone = NLP.monthYearFound(mapMonthYearEnded);
				if (!monthYearEndedDone) {
					monthYearEndedDone = NLP.allColHeadingsCaptured(startGroupLimited, mapMonthYearEnded);
				}
				//NLP.printMapIntStr("@after mergeOneToOne - mapMonthYearEnded=", mapMonthYearEnded);
			}

			if (!monthYearEndedDone && null != mapMonthYear && mapMonthYear.size() > 0) {
				// smallest largest
				// NLP.pwNLP.append(//NLP.printLn("getting mapMonthYearEnded with mergeTwoToOne",
				// ""));
				mapMonthYearEnded = NLP.mergeTwoToOne(mapEnded, mapMonthYear, monthYearEndedDone, cntE,
						mapMonthYear.size());
				monthYearEndedDone = NLP.monthYearFound(mapMonthYearEnded);
				if (!monthYearEndedDone) {
					monthYearEndedDone = NLP.allColHeadingsCaptured(startGroupLimited, mapMonthYearEnded);
				}
				//NLP.printMapIntStr("@after mergeColHdgsTwoToOneRatio - mapMonthYearEnded=", mapMonthYearEnded);
			}

			if (!monthYearEndedDone && null != mapMonthYear && mapMonthYear.size() > 0) {
				mapMonthYearEnded = NLP.mergeThreeToTwo(mapEnded, mapMonthYear, monthYearEndedDone, cntE,
						mapMonthYear.size());
				monthYearEndedDone = NLP.monthYearFound(mapMonthYearEnded);
				if (!monthYearEndedDone) {
					monthYearEndedDone = NLP.allColHeadingsCaptured(startGroupLimited, mapMonthYearEnded);
				}
				//NLP.printMapIntStr("@after mergeColHdgsThreeToTwoRatio - mapMonthYearEnded=", mapMonthYearEnded);
			}

			if (!monthYearEndedDone && null != mapMonthYear && mapMonthYear.size() > 0) {
				mapMonthYearEnded = NLP.mergeFourToThree(mapEnded, mapMonthYear, monthYearEndedDone, cntE,
						mapMonthYear.size());
				monthYearEndedDone = NLP.monthYearFound(mapMonthYearEnded);
				if (!monthYearEndedDone) {
					monthYearEndedDone = NLP.allColHeadingsCaptured(startGroupLimited, mapMonthYearEnded);
				}
				//NLP.printMapIntStr("@after mergeColHdgsThreeToTwoRatio - mapMonthYearEnded=", mapMonthYearEnded);
			}

			if (!monthYearEndedDone && null != mapMonthYear && mapMonthYear.size() > 0) {
				mapMonthYearEnded = NLP.mergeThreeToOne(mapEnded, mapMonthYear, monthYearEndedDone, cntE,
						mapMonthYear.size());
				monthYearEndedDone = NLP.monthYearFound(mapMonthYearEnded);
				if (!monthYearEndedDone) {
					monthYearEndedDone = NLP.allColHeadingsCaptured(startGroupLimited, mapMonthYearEnded);
				}
				//NLP.printMapIntStr("@after mergeColHdgsThreeToOneRatio - mapMonthYearEnded=", mapMonthYearEnded);
			}

			if (!monthYearEndedDone && null != mapMonthYear && mapMonthYear.size() > 0) {
				mapMonthYearEnded = NLP.mergeFiveToThree(mapEnded, mapMonthYear, monthYearEndedDone, cntE,
						mapMonthYear.size());
				monthYearEndedDone = NLP.monthYearFound(mapMonthYearEnded);
				if (!monthYearEndedDone) {
					monthYearEndedDone = NLP.allColHeadingsCaptured(startGroupLimited, mapMonthYearEnded);
				}
				//NLP.printMapIntStr("@after mergeColHdgsFiveToThree - mapMonthYearEnded=", mapMonthYearEnded);
			}

		}

		// firstAndLast. if I can't pair monthYear such that the map size is now
		// equal to totalColumns - then don't attempt anything other than first
		// and last method which will try month year and then ended pairing.

		int cntICol = 0;
		if (cntInception > 0) {
			cntICol = 1;
		}

		// start firstAndLast - only run if none no pairing occured (no
		// monthYearDone).

		if (!monthYearDone) {
			mapMonthYear = NLP.mergeFirstAndLastCHs(mapMonth, mapYear, monthYearDone, cntICol, totalColumns);
			if (null != mapMonthYear && mapMonthYear.size() > 0) {
				monthYearDone = true;
			}

			//NLP.printMapIntStr("@after mergeColHdgsFirstAndLast - mapMonthYear=", mapMonthYear);
			// NLP.pwNLP.append(//NLP.printLn("mergeColHdgsFirstAndLast monthYearDone=",
			// monthYearDone + ""));

			if (getEnded && monthYearDone) {
				mapMonthYearEnded = NLP.mergeFirstAndLastCHs(mapEnded, mapMonthYear, monthYearEndedDone, cntICol,
						totalColumns);
				if (null != mapMonthYearEnded && mapMonthYearEnded.size() > 0) {
					monthYearEndedDone = true;
				}

				//NLP.printMapIntStr("@after mergeColHdgsFirstAndLast - mapMonthYearEnded=", mapMonthYearEnded);
			}
		}

		// end firstAndLast

		allColHeadingsCaptured = false;

		if (monthYearEndedDone) {
			mapFinal = mapMonthYearEnded;
			//NLP.printMapIntStr("mapMonthYear is mapFinal===", mapFinal);
		}

		if (monthYearDone && !monthYearEndedDone) {
			mapFinal = mapMonthYear;
			//NLP.printMapIntStr("mapMonthYear is mapFinal===", mapFinal);
		}

		// System.xut.println("mapFinal.size=" + mapFinal.size());
		allColHeadingsCaptured = NLP.allColHeadingsCaptured(startGroupLimited, mapFinal);
		// NLP.pwNLP.append(//NLP.printLn("allColHeadingsCaptured==",
		// allColHeadingsCaptured + ""));

		//NLP.printMapIntStr("@nlp.getMonthYearEndedColHdg printing final mapFinal:", mapFinal);
		//NLP.printMapIntStr("@nlp.getMonthYearEndedColHdg printing final mapInception:", mapInception);
		// NLP.pwNLP.append(//NLP.printLn("mapInceptionAllRelatedRows.size--",
		// mapInceptionAllRelatedRows.size() + ""));
		// NLP.pwNLP.append(//NLP.printLn("mapInception.size--", mapInception.size() +
		// ""));

		if (mapFinal != null && mapFinal.size() > 0 && mapInception.size() > 0) {
			// will return null if two or more incep rows whose Mps > 4.5
			mapFinal = NLP.addBackInceptionColumn(mapFinal, mapInception);
		}

		// prior to final need to add origional map inception match.

		List<String> listCHbyIdxLoc = new ArrayList<>();

		Map<Integer, String> mapReorder = new TreeMap<Integer, String>();

		int key = 0;
		if (null != mapFinal && mapFinal.size() > 0) {
			for (Map.Entry<Integer, String> entry : mapFinal.entrySet()) {
				key = entry.getKey();
				mapReorder.put(key - key / 1000 * 1000, entry.getValue());
			}
		}

		//NLP.printMapIntStr("@nlp.getMonthYearEndedColHdg reordered map -mapReorder", mapReorder);

		if (null != mapReorder && mapReorder.size() > 0) {
			for (Map.Entry<Integer, String> entry : mapReorder.entrySet()) {
				listCHbyIdxLoc.add(entry.getValue());
			}
		}

		// NLP.pwNLP.append(//NLP.printLn("2@getMonthYearEndedColHdg return
		// listCHbyIdxLoc. allColHeadingsCaptured=",
		// allColHeadingsCaptured + ""));
		//NLP.printListOfString("listCHbyIdxLoc", listCHbyIdxLoc);

		return listCHbyIdxLoc;

	}

	public static void main(String[] args) throws SQLException, IOException, ParseException {

		// NLP nlp = new NLP();

		// TODO: ensure tp_raw and bac_tp_raw yyyyQtr# tables have rowname
		// length of 255 - not 125. Rownames get cut off.

		// TODO: Get tmp_tp_co created when it does not exists.

		double startTime = System.currentTimeMillis();
		boolean parseTable = false;
		TableTextParser ttp = new TableTextParser("1", "20180101", "", 0, "", "", "KLLM");

		String text = Utils.readTextFromFile("c:/temp/t.txt");
		// System.xut.println("fileSize bef remove gobblyGook=" + text.length());
		// text = NLP.removeGobblyGook(text);
		// System.xut.println("fileSize aft remove gobblyGook=" + text.length());

		MysqlConnUtils.executeQuery("truncate tp_raw2018qtr1;\r truncate bac_toc;");

		// System.xut.println("final text.len=" + text.length());
		hasTabs = false;

		String adjustIdxLoc = "1";

		File file = new File("c:/backtest/tableparser/2018/qtr1/t0.txt");
		if (file.exists())
			file.delete();

		TableParser.pwYyyyQtr = new PrintWriter(file);

		ttp.tableTextParser(text, parseTable, true, adjustIdxLoc);

		NLP nlp = new NLP();

		List<String[]> list = nlp.getAllEndIdxAndMatchedGroupLocs(text, TableParser.TableNamePattern);
		for (int i = 0; i < list.size(); i++) {
			// System.out.println("list TNs=" + Arrays.toString(list.get(i)));
		}

		double endTime = System.currentTimeMillis();
		double duration = (endTime - startTime) / 1000;
		System.out.println("final overall duration=" + duration);

		/*
		 * first parse all into bac_tp_rawYYYYQtr tables then see what was not parsed
		 * into mysql and for tableTextParser select adjust table end idx loc in order
		 * to adjust table name end idx. This should help get tables I missed by going
		 * backwards after each table name end idx a little so that I don't skip tables.
		 */

		TableParser.pwYyyyQtr.close();

	}
}
