package xbrl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class TableParserHTML {

	public static Elements prevChRows = null;
	public static String acc;
	public static String chAboveRowname;
	public static String tsShort = "";
	public static String excludeTable = "";
	public static String fileDate;
	public static String cik;
	public static String originalText = ""; // set based on text passed into
											// class. don't reset at each table
	public static int tableCount;
	public static int tblC2 = 0;
	public static int firstNonChLineNumberNotFound;
	public static String fye;
	public static String formType;
	public static String allColTextOneMonth = "";
	public String tableHtml;
	public static String tableNameLong = "";
	public String priorTableNameLong = "";
	public String textAfterTableName = "";
	public String decimal = "1";
	public static String tableSentence = "";
	public static String prevTableSentence = "";
	public static String tableNameShort = "";
	public static String companyName = "";// from master.idx
	public static String coNameMatched = ""; // co name matched.
	public String coNameOnPriorLine = ""; // co name matched.
	public int maxCoNameLength = 75;
	public int coMatch;
	public int noteColIdxSt = -99;
	public int noteColIdxEnd = -99;

	public static boolean noteColIdxFound = false;
	public static boolean getTableOfContents = false; // only reset when
														// tpHtml is called
														// -- not at each
														// table.
	public static boolean parseEntireHtml = false; // set based on what is
													// passed into class. Don't
													// reset at each table
	public static int numberOfColumnHdgs;
	public static String allColText = "";
	public static String colHdgAllLinesPatterns = "";
	// public String tp_CH_Html = "";
	public static String tableSaved = "";
	public static boolean parseTableFromFolder = false;
	public static boolean insertIntoMysql = false;
	public static boolean columnOneAndTwoSameYear = false;
	public static boolean textBetweenTables = true;
	public static boolean colHdgOnSameRowAsTableName = false;
	public static int tableStartIdx;
	public static int prevTableStartIdx;
	public static int tableStartIdxLoopCount;
	public static int tableEndIdx;
	public static int prevTableEndIdx = 0;
	public static int nextTableEndIdx;

	public Map<Integer, List<String>> colHeadingsMap = new HashMap<Integer, List<String>>();
	public static List<String[]> edtPeriodList = new ArrayList<>();
	public List<String[]> edtPeriodListRawText = new ArrayList<>();
	public List<String> colHeadingsList = new ArrayList<>();
	public List<String> finalMergedChFromRawTextList = new ArrayList<>();
	public List<String> listOfStrHtmlColHdgs = new ArrayList<>();
	public List<String[]> listCHEndedYrandMo = new ArrayList<>();
	public List<String> listFirstColEdtPer = new ArrayList<String>();
	public List<String> listLastColEdtPer = new ArrayList<String>();
	public List<String> listCoName = new ArrayList<String>();
	public List<Integer> ListTableEndIdx = new ArrayList<Integer>();
	public Map<Integer, List<String[]>> mapCHs = new HashMap<Integer, List<String[]>>();

	private boolean tableNameInsideTable = false;
	private boolean tableNameOutsideTable = false;
	private boolean tableNameOutsideButNotValidate = false;
	private static List<String[]> endDatesPeriodsListFromTableSentences = null;

	/*
	 * constructor (in order to call this class you need to 'construct' it with
	 * required variables it needs. These variables are then required to be passed
	 * to the class whenever the class is instantiated.
	 */

	public TableParserHTML(String acc, String fileDate, String cik, int tableCount, String fye, String formType,
			String companyName) {
		TableParserHTML.acc = acc;
		TableParserHTML.fileDate = fileDate;
		TableParserHTML.cik = cik;
		TableParserHTML.tableCount = tableCount;
		TableParserHTML.fye = fye;
		TableParserHTML.formType = formType;
		TableParserHTML.companyName = companyName;
	}

	public static Pattern EndedHeadingPatternGood = Pattern.compile("([a-zA-Z]{0,5}(?i)(?s)"
			+ "(three[\t ]{1,4}months? ?([ ]{0,3}end[ed|ing]{2,3})?|half.{1,3}year|first[ ]{1,2}half[ ]{0,3}end[ed|ing]{2,3}|[s]ix[\t ]{1,4}months? ?"
			+ "([ ]{0,3}end[ed|ing]{2,3})?|nine[\t ]{1,4}months? ?([ ]{0,3}end[ed|ing]{2,3})?|twelve[\t ]{1,4}months? ?"
			+ "([ ]{0,3}end[ed|ing]{2,3})?|(three|two)(.{1,2}fiscal)?.{1,2}quarters.{1,2}end[eding]{2,3}|(first|second|third|fourth|1st|2nd"
			+ "|3rd|4th)[- \t]{1,3}Q[uarters]{0,7}|quarters? ?{1,3}end[ed|ing]{2,3}"
			+ "|(two|three|four|five).{1,3}years|year[s]{0,1}(?![- ]{1,3}to[ -]{1,3}date)([ ]{1,3}(ended|ending))"
			+ "|fiscal.{1,2}year(.{1,2}end[eding]{2,3})?|year[\r\t s]{1,3}(ended|ending)?" + "|(3|6|9"
			+ "|12|three| six|nine|twelve)[- \t]{1,3}(mo\\.|mos|months? ?)[ ]{0,2}(end[ed|ing]{2,3})?"
			+ "|(12|twelve|13|thirteen|14|fourteen|15|fifteen|sixteen|16|24|twenty.{1,3}four|25|twenty.{1,3}five|26|twenty.{1,3}six"
			+ "|27|twenty.{1,3}seven|28|twenty.{1,3}eight|36|thirty.{1,3}six|37|thirty.{1,3}seven|"
			+ "38|thirty.{1,3}eight|39|thirty.{1,3}nine|40|forty|51"
			+ "|fifty.{0,2}two|52|fifty.{0,2}three|53)[\t- ]{1,3}w.{0,2}k[s](.{0,3}end[ed|ing]{2,3})?"
			+ "|(thirteen|twenty.{1,3}six|for the three|for the quarters?( ended)?|for[ ]{1,2}the[ ]{1,2}six"
			+ "|twelve|thirty.{1,3}nine|for.{1,3}the.{1,3}nine[ months]{0,7}|for.{1,2}the.{1,2}years.{1,2}end[eding]{2,3}))|fifty.{0,3}(one|two|three))"
			+
			// below will find three,six,nine or twelve sandwiched
			// between tab/hard return eg: Three<tab>Six<tab>Twelve.
			"|((?i)(?m)[\r\t]{1} ?three ?(?=([\r\t]{1}))|[\r\t]{1} ?six ?(?=([\r\t]{1}))|[\r\t]{1} ?nine ?(?=([\r\t]{1}))"
			+ "|[\r\t]{1} ?twelve ?(?=([\r\t]{1}))|[\r\t]{1} ?quarter ?(?=([\r\t]{1}))"
			+ "|^ ?three|^ ?six|^ ?nine|^ ?twelve|^ ?quarter[ ended]{0,6}|two quarters[ ended]{0,6}|[1,2,3,4]{1}.{1,3}quarter)"
			+ "");

	public static Pattern OddBallPeriodPattern = Pattern
			.compile("[a-zA-Z]{0,5}(?i)(?s)(((?<!(twenty[- ]{1,3}|thirty[- ]{1,3}|fifty[- ]{1,3}))"
					+ "(one|two|four|five|seven|eight|fifteen|sixteen))[ ]{1,4}month[s ](ending|ended|end)?|year[s]{0,1}[- ]{1,3}to[ -]{1,3}date)");

	public static Pattern EndedHeadingPattern = Pattern
			.compile(EndedHeadingPatternGood.pattern() + "|" + OddBallPeriodPattern.pattern());

	public static Pattern YearPatternOrMoDayYrPattern = Pattern.compile("[\\xA0 \t\r\n]*((19\\d{2}|20\\d{2})"
			+ "|((\\d{1,2}[\\/-]{1}\\d{1,2}[\\/-]{1}(\\d{2,4}" + "[\t \\s\r\n]{1}|\\d{4}[\t \\s\r\n]{1}))"
			+ "|[1-3]{1}\\d-(JAN|FEB|MAR|ARP|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)-[90]{1}\\d))[\\xA0 \t\r\n]*");

	// don't use cap insensitive b/c need initial caps or not.
	public static Pattern RestatedPattern = Pattern.compile("Restated|RESTATED|restated"
			+ "|PRO.{1,2}FORMA|Pro.{1,2}Forma|(^| )Consolida|(^| )CONSOLIDA|Revised"
			+ "|restated|REVISED|Reported|REPORTED|Adjusted|ADJUSTED" + "|Parent|Guarantor|Eliminations?|Subsidiaries"
			+ "|PARENT|GUARANTOR|ELIMINATIONS?|SUBSIDIARIES" + "|Percent|\\%");

	// don't add percent or increase or decrease
	// |HK\\$|\\$HK|Percent|Increas|PERCENT|INCREAS|Decreas|DECREAS

	public static Pattern EndedOnlyPattern = Pattern.compile("(?i)end[eding]{2,3}");

	public static Pattern yearPattern = Pattern.compile("[1,2]{1}[09]{1}[\\d]{2}");

	public static Pattern ColumnHeadingPattern = Pattern.compile(EndedHeadingPattern.pattern() + "|"
			+ TableParser.MonthPattern.pattern() + "|" + YearPatternOrMoDayYrPattern.pattern() + "|"
			+ RestatedPattern.pattern() + "|" + EndedOnlyPattern.pattern());

	public static Pattern alphabetPattern = Pattern.compile(" [A-Za-z]{1,} |[A-Za-z]{3,}");

	public Document parseHtml(String tableText) {
		Document doc = Jsoup
				.parseBodyFragment(tableText.replaceAll("\\&nbsp;|\\xA0|\\&#160;", " ").replaceAll("\\&#151;", "-"));
		return doc;
	}

	public void parseTable(String tableText) throws IOException, SQLException, ParseException {

		// replaces all non-ascii characters (special characters used in english
		// language)
		NLP nlp = new NLP();
		TableParser tp = new TableParser();

		// NLP.pwNLP.append(//NLP.printLn("@parseTable tableNameLong=",
		// tableNameLong));

		listLastColEdtPer.clear();
		listFirstColEdtPer.clear();

		// PICKUP HERE - RETHINK USE OF CAPTION -- SEARCH CAPTION HERE AND NLP!!
		// UNCLEAR WHY BS TABLE ISN'T PICKEDUP
		tableText = tableText.replaceAll("[^\\x00-\\x7F]", "");
		Document doc = parseHtml(tableText);
		// NLP.pwNLP.append(//NLP.printLn("METHOD parseHtml complete", ""));
		// this elements array is made up of two variables - column heading
		// row[0] and dataRow[1]. The col hdgs are not yet merged.

		Elements[] chAndDataRows = getCHnDataRows(doc);
		for (int i = 0; i < chAndDataRows[0].size(); i++) {
			// NLP.pwNLP.append(//NLP.printLn("row=", i
			// + " chAndDataRows[0].get(row).text="
			// + chAndDataRows[0].get(i).text()));
		}

		// data rows are feteched previously even if not CH found for that
		// table. If they were feteched chAndDataRows[1] (data rows) should be
		// >8 and if there's no text between prior table and current then prior
		// table's CH apply

		// NLP.pwNLP.append(//NLP.printLn("@prevCHRows. chAndDataRows[0].size()="
		// + chAndDataRows[0].size() + " textBetweenTables="
		// + textBetweenTables + "\r8c tableSentence=" + tableSentence);

		if (null != prevChRows && chAndDataRows[0].size() == 0 && !textBetweenTables
				&& chAndDataRows[1].size() > 0/*
												 * && chAndDataRows [ 1].size() >3
												 */) {
			chAndDataRows[0] = prevChRows;
			tableNameShort = tp.getTableNameShort(priorTableNameLong);
			tableNameLong = priorTableNameLong.replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n", "");

			// NLP.pwNLP.append(//NLP.printLn("@set prevChRows --use priorTablenameLong --
			// tableNameLong="+priorTableNameLong);
			// NLP.pwNLP.append(//NLP.printLn(" # data rows chAndDataRows[1].size="
			// + chAndDataRows[1].size());
			// NLP.pwNLP.append(//NLP.printLn("after set to prevChRows -
			// chAndDataRows[0].size=="+chAndDataRows[0].size());

		}

		if (tableNameLong.length() < 1 && !textBetweenTables
				&& chAndDataRows[1].size() > 0/* && chAndDataRows[1].size()>3 */) {
			tableNameShort = tp.getTableNameShort(priorTableNameLong);
			tableNameLong = priorTableNameLong.replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n", "");

		}

		if (!textBetweenTables && tableSentence.length() < 2) {
			tableSentence = prevTableSentence;
			// NLP.pwNLP
			// .append(//NLP.printLn("1a-- tableSentence==", tableSentence));

			List<String> list = NLP.parseTableSentence(tableSentence,
					companyName.substring(0, Math.min(companyName.length(), 7)), false, tableNameLong, false);

			if (list.get(6).length() > 6) {
				tableNameLong = list.get(6).replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n", "");
				tableNameShort = tp.getTableNameShort(tableNameLong);
			}

			tableSentence = list.get(0);
			// NLP.pwNLP
			// .append(//NLP.printLn("1b-- tableSentence==", tableSentence));
			tsShort = list.get(1);
			String tsPattern = list.get(2).replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)", "");
			// NLP.pwNLP.append(//NLP.printLn("1 tsshort=", tsShort));

			if (coNameMatched.length() < 1 && list.size() > 5 && list.get(5).length() > 3) {
				coNameMatched = list.get(5).replaceAll("'", "");
			}

			if (!tsPattern.equals("MY")) {
				endDatesPeriodsListFromTableSentences = NLP.getTablesentenceColumnHeadings(tableSentence, tsShort,
						tsPattern);
			}
		}

		// NLP.pwNLP.append(//NLP.printLn("last? tableSentence==", tableSentence
		// + " prevTableSentence=" + prevTableSentence));
		Element tr, td;

		// if colCnt (cnt from lft to rt) or TDs.size-colCnt (cnt from rt to
		// lft) are same across rows - those cols align. Insert direct into
		// tp_raw table - and then use to grab from edt2?/yr/mo/end? or start in
		// tp_colIdx - by matching cols that have same left or right col idx #?

		// int month = Integer.parseInt(fileDate.substring(4, 6));
		// int qtr = ((month - 1) / 3) + 1;

		// String table = "tp_colHdgs" + fileDate.substring(0, 4) + "qtr" + qtr;

		// StringBuffer sb = new StringBuffer();
		String type = "", txt;

		int totCol = 0, tmpTotCol = 0, prevTotCol = 0;
		// NLP.pwNLP.append(//NLP.printLn("chAndDataRows[0].size()=",
		// chAndDataRows[0].size() + ""));
		for (int row = 0; row < chAndDataRows[0].size(); row++) {
			tr = chAndDataRows[0].get(row);
			// NLP.pwNLP.append(//NLP.printLn("1. CH row=", tr.text()));
			Elements TDs = tr.getElementsByTag("td");
			tmpTotCol = 0;
			// split of CH row by tab.
			for (int colCnt = 0; colCnt < TDs.size(); colCnt++) {
				type = "";
				txt = TDs.get(colCnt).text().replaceAll(" ?\\*", "");
				if (txt.length() > 0) {
					tmpTotCol++;
					// NLP.pwNLP.append(//NLP.printLn("CH text in cell=" , txt);
				}
			}
			if (tmpTotCol > prevTotCol)
				totCol = tmpTotCol;
			else
				totCol = prevTotCol;
			prevTotCol = totCol;
		}

		int lToR, rToL;

		List<String[]> listLRandRLandRowandColTxt = new ArrayList<String[]>();
		String colSpanStr = "";
		int colSpanInt = 0;
		for (int row = 0; row < chAndDataRows[0].size(); row++) {
			tr = chAndDataRows[0].get(row);
			// NLP.pwNLP.append(//NLP.printLn("AAAA tr="+tr.text());
			Elements TDs = tr.getElementsByTag("td");
			@SuppressWarnings("unused")
			int priorNoteColIdx = -99;
			for (int colCnt = 0; colCnt < TDs.size(); colCnt++) {
				type = "";
				txt = TDs.get(colCnt).text().replaceAll("'", "");

				td = TDs.get(colCnt);
				colSpanStr = td.attr("colspan").replaceAll("[^0-9]", "");
				if (colSpanStr.length() > 0
						&& nlp.getAllIndexEndLocations(colSpanStr, Pattern.compile("(?i)[a-zA-Z\\.;:]")).size() < 1
						&& nlp.getAllIndexEndLocations(colSpanStr, Pattern.compile("(?i)[a-zA-Z\\.;:]")) != null

				) {
					colSpanInt = Integer.parseInt(colSpanStr);
					// NLP.pwNLP.append(//NLP.printLn("colSpanStr=", colSpanStr));
				}

				// NLP.pwNLP.append(//NLP.printLn("AAAA txt=="+txt);
				if (txt.length() > 0) {
					// NLP.pwNLP.append(//NLP.printLn("BBBB txt=="+txt);
					lToR = colCnt;
					rToL = TDs.size() - colCnt;
					type = nlp.getColHeadingType(txt);
					// NLP.pwNLP.append(//NLP.printLn("CCCC CH type=="+txt);
					// NLP.pwNLP.append(//NLP.printLn("lToR="+lToR+" rToL="+rToL);
					// NLP.pwNLP.append(//NLP.printLn("CH Col start="+lToR+" CH Col
					// end="+(colSpanInt+lToR));
					// 2 to 8 - 3 mo ended sep30
					// 1 to 3 - 2007 (problem is rowspan causes undercount by 1
					// 4 to 6 - 2006. - when there isn't a colSpan (or rowSpan)
					// it works. rowspan causes one tab undercount
					// in addition why aren't I counting the span of columns a
					// heading covers - e.g., start = lTOr td count
					// +cumulative count of all colspans plus 1 if rowspan
					// present.

					// don't want to record col # if it isn't past col1. Don't
					// use additional filter b/c table structure needs to
					// dictate output-not NLP (eg, don't use ch pattern
					// filters).
					if (lToR > 1 || colSpanInt > 1) {
						// NLP.pwNLP
						// .append(//NLP.printLn("DDDD adding to ch=", txt));
						String[] ary = { lToR + "", rToL + "", (row + 1) + "", txt };
						listLRandRLandRowandColTxt.add(ary);
						// colSpanInt is number of cols spanned, and if colSpan
						// is 3 and start of span begins at col 1 - then span
						// ends at col 3 - so if you add colspan 3 to start col
						// idx it will over count by 1. Hence why I minus 1.
						if (txt.toUpperCase().contains("NOTE") && noteColIdxSt == -99) {
							int tmpColSpanInt = 0;
							if (colSpanInt > 0) {
								tmpColSpanInt = colSpanInt - 1;
							}
							noteColIdxSt = lToR;
							noteColIdxEnd = lToR + tmpColSpanInt;
							priorNoteColIdx = noteColIdxSt;
							noteColIdxFound = true;
							// NLP.pwNLP.append(//NLP.printLn("noteColIdxFound=",
							// noteColIdxFound + " colSpanInt="
							// + colSpanInt + "noteColIdxSt="
							// + noteColIdxSt + " noteColIdxEnd="
							// + noteColIdxEnd));
							// noteColIdx records the colIdx where 'NOTE' is the
							// CH (lToR=colIdx). Later if a number appears in
							// the NOTE colIdx <20 and NOTE is not otherwise in
							// a good CH - I will skip the value b/c it likely
							// not a data value and would otherwise throw off
							// the alignment of data cols against good CHs.
						}
					}

					// NLP.pwNLP.append(//NLP.printLn(
					// "at tp_ch insert -- noteColIdxFound="
					// + noteColIdxFound + " colSpanInt=",
					// colSpanInt + "noteColIdxSt=" + noteColIdxSt
					// + " noteColIdxEnd=" + noteColIdxEnd));

					// sb.append(acc + "||" + fileDate + "||" + cik + "||"
					// + tableNameShort + "||" + tableCount + "||"
					// + (row + 1) + "||" + txt + "||'" + type + "'||"
					// + lToR + "||" + rToL + "||" + "\\N" + "||" + totCol
					// + "||" + companyName + "||" + coNameMatched + "||"
					// + tableSentence + "\n");
					// NLP.pwNLP.append(//NLP.printLn("tp_colhdgs==", acc + "||"
					// + fileDate + "||" + cik + "||" + tableNameShort
					// + "||" + tableCount + "||" + (row + 1) + "||" + txt
					// + "||'" + type + "'||" + lToR + "||" + rToL + "||"
					// + "\\N" + "||" + totCol + "\n"));
				}
			}
		}

		if (tableEndIdx != prevTableEndIdx || prevTableStartIdx - tableStartIdx < 2000) {
			prevChRows = chAndDataRows[0];
		}

		// NLP.pwNLP.append(//NLP.printLn("@3/4 way thru parseTable tableNameLong=",
		// tableNameLong));

		prevTableStartIdx = tableStartIdx;
		prevTableEndIdx = tableEndIdx;

		// NLP.pwNLP.append(//NLP.printLn("set prevChRows - size=",
		// prevChRows.size() + " tableEndIdx=" + tableEndIdx
		// + " tableStartIdx=" + tableStartIdx
		// + " tableStartIdxLoopCount=" + tableStartIdxLoopCount));

		// NLP.pwNLP.append(//NLP.printLn("printing listLRandRLandRowandColTxt in
		// reverse");
		//// NLP.printListOfStringArrayInReverse(listLRandRLandRowandColTxt);
		// NLP.pwNLP.append(//NLP.printLn(
		// "@3/4..2 way thru parseTable tableNameLong=", tableNameLong));

		String firstColTxt = "", lastColTxt = "", row = "", priorRow = "", priorColTxt = "", colTxt = "";
		Integer loopSize = listLRandRLandRowandColTxt.size();
		int lr = -1, rl, pRL = 0, pLR = -1, pRL2 = -1, rl2 = -1;
		if (loopSize != null && loopSize > 0) {
			// listLRandRLandRowandColTxt has the [0]left to rt col number,
			// [1] rt to left col #,[2]row # and [3]col txt. In table (1st col
			// may occur at the col # 4 in table).

			// note audited/restated are often bottom row, however they may not
			// be under each CH, so row with most columns with text is going to
			// be row with year value.
			int rowCnt = 0;
			for (int i = (loopSize - 1); i >= 0; i--) {
				// go in reverse order b/c greater integrity at bottom of CH
				colTxt = listLRandRLandRowandColTxt.get(i)[3].replaceAll("[\r\n]", " ");
				// get right to left col loc (col #in tbl)
				rl = Integer.parseInt(listLRandRLandRowandColTxt.get(i)[1]);

				row = listLRandRLandRowandColTxt.get(i)[2];
				if (rowCnt > 0 && !row.equals(priorRow)) {
					// if at 2nd row - compare rows and if not equal - 1st
					// occurrence on new row is furthest right col
					// NLP.pwNLP.append(//NLP.printLn("row=", row + " priorRow="
					// + priorRow + " colTxt=" + colTxt + " lastcolTxt="
					// + lastColTxt + " lr=" + lr
					// + " lr colTxt (priorColTxt)=" + priorColTxt
					// + " rowCnt=" + rowCnt + " pLR=" + pLR));

					// if previous rt to lft col# less cur is 1 or less - match
					// it.
					if (Math.abs(rl - pRL) < 2) {
						lastColTxt = colTxt + " " + lastColTxt;
						pRL = rl;
					}

					// if first instance of rows not equal (rowcnt=1) then
					// immediatley prior col is the first col of prior row (b/c
					// loop is in goes in reverse)
					if (rowCnt == 1 && i != 0) {
						firstColTxt = priorColTxt;
						pLR = lr;
						pRL2 = rl2;
						// NLP.pwNLP.append(//NLP.printLn(
						// "rowCnt==1 and firstColTxt=", firstColTxt
						// + "pRL2=" + rl2));

					}
					if (rowCnt > 1 && (Math.abs(pLR - lr) < 2 || Math.abs(rl2 - pRL2) == 0)) {
						firstColTxt = priorColTxt + " " + firstColTxt;
						pLR = lr;
						pRL2 = rl2;
						// NLP.pwNLP.append(//NLP.printLn("rowCnt>1 firstColTxt="
						// ,
						// firstColTxt + " rl2=" + rl2));
					}

					// last prior LR idx is lr when i=0. so compare to cur LR.
					// When i=0 I am at very 1st col b/c looped in reverse

					rowCnt++;
				}

				if (i == 0) {
					// NLP.pwNLP.append(//NLP.printLn("i==0 pRL2=", pRL2
					// + " this rl2="
					// + listLRandRLandRowandColTxt.get(i)[1] + " colTxt="
					// + listLRandRLandRowandColTxt.get(i)[3]));
				}
				if (i == 0 && (Math.abs(lr - Integer.parseInt(listLRandRLandRowandColTxt.get(i)[0])) < 2

						|| Math.abs(pRL2 - Integer.parseInt(listLRandRLandRowandColTxt.get(i)[1])) == 0)) {
					// || rl2 -...
					firstColTxt = colTxt + " " + firstColTxt;
					// NLP.pwNLP.append(//NLP.printLn("i==0 firstColTxt=",
					// firstColTxt));
				}

				rl2 = Integer.parseInt(listLRandRLandRowandColTxt.get(i)[1]);
				lr = Integer.parseInt(listLRandRLandRowandColTxt.get(i)[0]);
				// NLP.pwNLP.append(//NLP.printLn("lr=", lr + " colTxt=" + colTxt
				// + " rl2=" + rl2));

				// last line in reverse is very first CH
				if (!row.equals(priorRow) && rowCnt == 0) {
					// NLP.pwNLP.append(//NLP.printLn("row=", row + " priorRow="
					// + priorRow + " colTxt=" + colTxt + " lastcolTxt="
					// + lastColTxt));
					lastColTxt = colTxt + " " + lastColTxt;
					rowCnt++;
					pRL = rl;
				}
				priorColTxt = colTxt;
				priorRow = row;
			}
		}

		// NLP.pwNLP.append(//NLP.printLn(
		// "@3/4..3 way thru parseTable tableNameLong=", tableNameLong));

		// NLP.pwNLP.append(//NLP.printLn("A FIRSTCOLTXT=", firstColTxt
		// + "\nB LASTCOLTXT=" + lastColTxt));
		listFirstColEdtPer = nlp.getEnddatePeriod(" " + firstColTxt);
		listLastColEdtPer = nlp.getEnddatePeriod(" " + lastColTxt);

		// if (sb.toString().length() > 50) {
		// String path = "c:/backtest/tableParser/" + fileDate.substring(0,
		// 4)
		// + "/QTR" + qtr + "/tables/ch";
		// Utils.createFoldersIfReqd(path);

		// String filename = path + acc + "_" + tableCount + "sql.txt";
		// PrintWriter pw3 = new PrintWriter(filename);
		// pw3.write(sb.toString().substring(0, sb.toString().length() -
		// 1));
		// pw3.close();
		// Utils.loadIntoMysql(filename, table);
		// NLP.pwNLP.append(//NLP.printLn("tp_colhdgs=" ,
		// sb.toString().substring(0,
		// sb.toString().length() - 1)));
		// sb.delete(0, sb.toString().length());
		// }

		// NLP.pwNLP.append(//NLP.printLn(
		// "@3/4..4 way thru parseTable tableNameLong=", tableNameLong));
		List<String> htmlColHdgsList = new ArrayList<>();
		// Processing ch row

		if (chAndDataRows[0].size() > 0) {
			// NLP.pwNLP.append(//NLP.printLn("2. # of CH rows=",
			// chAndDataRows[0].size() + ""));
			// NLP.pwNLP.append(NLP
			// .println("@3/4..5 way thru parseTable tableNameLong=",
			// tableNameLong));
			htmlColHdgsList = mergeCHs(chAndDataRows[0], chAndDataRows[1]);
			// NLP.pwNLP.append(NLP
			// .println("@3/4..6 way thru parseTable tableNameLong=",
			// tableNameLong));

			// NLP.pwNLP.append(//NLP.printLn("htmlColHdgsList======", ""));
			//NLP.printListOfString("htmlColHdgsList", htmlColHdgsList);
			// NLP.pwNLP.append(//NLP.printLn(
			// "numberOfColumnHdgs in htmlColHdgsList=",
			// numberOfColumnHdgs + ""));

			if (decimal.equals("1")) {
				decimal = nlp.getDecimal(chAndDataRows[0].text());
				// NLP.pwNLP.append(//NLP.printLn("decimal
				// chAndDataRows[0].text()=="+chAndDataRows[0].text());
			}

			if (decimal.equals("1") && tableSentence != null) {
				decimal = nlp.getDecimal(tableSentence);
				// NLP.pwNLP.append(//NLP.printLn("decimal from tableSentence=="+decimal+
				// " tablesentence="+tableSentence);
			}

			if (decimal.equals("1")) {
				String str = nlp.stripHtmlTags(tableText.substring(0, Math.min(tableText.length(), 10000)));
				decimal = nlp.getDecimal(str.substring(0, Math.min(str.length(), 250)));
				// NLP.pwNLP.append(//NLP.printLn("decimal from first part of table="+decimal);
			}

			// CHs is column heading text (chDataRows[0]).

			if (edtPeriodList.size() < 1)
				edtPeriodList = getEndDatesPeriods(htmlColHdgsList);
			// <<==slightly different than same method in NLP
			//// NLP.printListOfStringArray("edtPeriodList", edtPeriodList);

			Pattern patternShortYear = Pattern
					.compile("[09]{1}[0-9]{1}-(0[1-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})");
			Matcher matchShortYear;

			List<String[]> tmpListEdtPerIdx = new ArrayList<String[]>();
			String tmpEdt, tmpPer, prevYr = "", curYr = "";
			for (int i = 0; i < edtPeriodList.size(); i++) {

				tmpEdt = edtPeriodList.get(i)[0];
				// NLP.pwNLP.append(//NLP.printLn("tmpEdt="+tmpEdt);
				tmpPer = edtPeriodList.get(i)[1];

				matchShortYear = patternShortYear.matcher(tmpEdt);
				if (matchShortYear.find() && tmpEdt.substring(0, 1).equals("9")) {
					tmpEdt = "19" + tmpEdt;
				}

				matchShortYear = patternShortYear.matcher(tmpEdt);
				if (matchShortYear.find() && tmpEdt.substring(0, 1).equals("0")) {
					tmpEdt = "20" + tmpEdt;
					// NLP.pwNLP.append(//NLP.printLn("01 found?=", tmpEdt));
				}
				// NLP.pwNLP.append(//NLP.printLn("11 tmpEdt=", tmpEdt));
				String[] ary = { tmpEdt, tmpPer };
				tmpListEdtPerIdx.add(ary);
				if (tmpEdt.length() > 4) {
					curYr = tmpEdt.substring(0, 4);
				}
				// NLP.pwNLP.append(//NLP.printLn("i=", i + " tmpEdt=" + tmpEdt
				// + " curYr=" + curYr + " prevYr=" + prevYr));
				// fix in TableTextParser
				if (tsShort.equals("PPMYY") || tsShort.equals("PPMYMY") || tsShort.equals("PMPM")) {
					if (i == 1 && prevYr == curYr) {
						// NLP.pwNLP.append(//NLP.printLn(
						// "columnOneAndTwo are different", ""));
						columnOneAndTwoSameYear = true;
					}

				}
				prevYr = tmpEdt.substring(Math.min(tmpEdt.length(), 4));
			}
			edtPeriodList = tmpListEdtPerIdx;

			// NLP.pwNLP.append(//NLP.printLn("chAndDataRows[1].size()=",
			// chAndDataRows[1].size() + ""));
			if (chAndDataRows[0].size() > 0 && chAndDataRows[1].size() > 0
			/* && chAndDataRows [1].size() >3 */
			) {
				// at least 1 CH && at least 9 data rows

				// NLP.pwNLP.append(//NLP.printLn("printing chAndDataRows[0]"
				// + chAndDataRows[0].text());
				finalMergedChFromRawTextList.clear();
				finalMergedChFromRawTextList = mergeCHRowFromRawText(chAndDataRows[0]);
				// NLP.pwNLP.append(//NLP.printLn(
				// "AAA finalMergedChFromRawTextList.size()=",
				// finalMergedChFromRawTextList.size() + ""));

				edtPeriodListRawText.clear();
				edtPeriodListRawText = getEndDatesPeriods(finalMergedChFromRawTextList);

				// NLP.pwNLP.append(//NLP.printLn(
				// "at getting data row map -- printingin edtPeriodList",
				// ""));
				//NLP.printListOfStringArray("edtPeriodList", edtPeriodList);
				// NLP.pwNLP.append(//NLP.printLn(
				// "GETTING DATA ROW MAP. tableNameLong=", tableNameLong));
				getDataRowColumnMap(chAndDataRows[0], chAndDataRows[1], htmlColHdgsList, edtPeriodList);
			}
		}
	}

	public Map<Integer, List<String>> getMapofCh(Elements ColumnHeadingRows) throws IOException, ParseException {

		allColText = "";
		NLP nlp = new NLP();
		Element tr;
		Elements TDs;
		Map<Integer, List<String>> mapOfCh = new TreeMap<Integer, List<String>>();

		int cnt = 0, cnt2 = 0;
		int prevRow = 0;
		for (int row = 0; row < ColumnHeadingRows.size(); row++) {
			tr = ColumnHeadingRows.get(row);
			TDs = tr.getElementsByTag("td");
			List<String> listCHfromRow = new ArrayList<>();
			for (int i = 0; i < TDs.size(); i++) {
				String str = TDs.get(i).text().replaceAll("[\\\\,\\']", "");
				// NLP.pwNLP.append(NLP
				// .println("allcolText str="
				// + str);

				if (nlp.getAllIndexStartLocations(str, ColumnHeadingPattern).size() > 0) {
					if (row == prevRow || cnt2 == 0)
						cnt2++;
					else
						cnt2 = 1;
					listCHfromRow.add(str);
					// NLP.pwNLP.append(//NLP.printLn("adding at row1=" + str);
					allColText = allColText + "|L" + (row + 1) + "C" + cnt2 + ":" + str + "|";
					prevRow = row;
				}
			}

			if (null != listCHfromRow && listCHfromRow.size() > 0) {
				mapOfCh.put(cnt, listCHfromRow);
				cnt++;
			}
		}

		// NOTE: below is finalizing format of allcolText and if just 1 month
		// then setting allColTextOneMonth to that month.
		// NLP.pwNLP.append(//NLP.printLn("at mapOfCh allcoltext=", allColText));

		// String prevMatch = "", match = "";
		// int pCntD, mCntD, yCntD;
		// List<String> tmpCHcntList = new ArrayList<String>();
		allColText = allColText.replaceAll("[\\|]{2,}", "\\|").replaceAll("'", "").replaceAll("[\r\n]", "");

		allColText = allColText + "|" + "pCnt:" + (nlp.getAllIndexEndLocations(allColText, EndedHeadingPattern).size());
		allColText = allColText + "|" + "yCnt:"
				+ nlp.getAllIndexEndLocations(allColText, YearPatternOrMoDayYrPattern).size();
		allColText = allColText + "|" + "mCnt:"
				+ nlp.getAllIndexEndLocations(allColText, TableParser.MonthPattern).size();
		allColText = allColText.replaceAll("[\\|]{2,}", "\\|").replaceAll("[\r\n]", "");

		String[] distinctPMY = NLP.getDistinctPeriodMonthYearCountHtml(allColText);
		allColText = allColText + distinctPMY[0];
		allColTextOneMonth = distinctPMY[1];
		// NLP.pwNLP.append(//NLP.printLn("allColTextOneMonth================",
		// allColTextOneMonth));

		return mapOfCh;
	}

	public List<String> mergeCHRowFromRawText(Elements ColumnHeadingRows) throws IOException, ParseException {

		NLP nlp = new NLP();

		Map<Integer, List<String>> mapOfCh = new TreeMap<Integer, List<String>>();
		mapOfCh = getMapofCh(ColumnHeadingRows);

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

		int cntL = largeList.size(), cntS = smallList.size();

		if (cntL / cntS < 1 || cntL % cntS != 0) {
			// NLP.pwNLP.append(//NLP.printLn("remainder chCnt/tsCnt");
			return finalCHRaw;
		}

		// merges 1st 2 CHs
		List<String> mergedCHRawList = new ArrayList<>();
		mergedCHRawList = nlp.mergeTwoLists(largeList, smallList, reverse);
		reverse = false;

		// if ch size is 3 or more loop through till end of ch rows.
		if (mapOfCh.size() < 3) {
			// NLP.pwNLP.append(//NLP.printLn("ColumnHeadingRows.size()="
			// + ColumnHeadingRows.size());
			return mergedCHRawList;
		}

		for (int row = 2; row < mapOfCh.size(); row++) {
			// NLP.pwNLP.append(//NLP.printLn("remainder of largeList/smallList==="
			// + ((mergedCHRawList.size() % mapOfCh.get(row).size()) +
			// " largeList/smallList="
			// + (mergedCHRawList.size() / mapOfCh.get(row).size()))
			// + " largeList size=" + mergedCHRawList.size() +
			// " smallList size=" + mapOfCh.get(row).size());
			if (mergedCHRawList.size() >= mapOfCh.get(row).size() && mergedCHRawList.size() != 0
					&& mapOfCh.get(row).size() != 0
			/* && (mergedCHRawList.size() % mapOfCh.get(row).size()) == 0 */) {
				mergedCHRawList = nlp.mergeTwoLists(mergedCHRawList, mapOfCh.get(row), false);
			} else if (mergedCHRawList.size() != 0 && mapOfCh.get(row).size() != 0
			/* && (mapOfCh.get(row).size() % mergedCHRawList.size()) == 0 */) {
				mergedCHRawList = nlp.mergeTwoLists(mapOfCh.get(row), mergedCHRawList, true);
			}
		}
		// picks-up remaining columnHeadings rows at last point (row).
		// NLP.pwNLP.append(//NLP.printLn("PRINTING mergedCHRawList");
		//// NLP.printListOfString(mergedCHRawList);
		return mergedCHRawList;

	}

	private List<String[]> getEndDatesPeriods(List<String> CHs) throws ParseException, IOException {

		// List<String[]> finalEndDatesPeriod = new ArrayList<String[]>();
		List<String[]> colHdgEnddatePeriod = new ArrayList<String[]>();
		List<String> listColHdgs = new ArrayList<String>();

		NLP nlp = new NLP();

		// this will form list of just colHdg texts by ignoring blank col hdgs.
		String colHdg;
		if (null == CHs || CHs.size() < 1)
			return colHdgEnddatePeriod;

		for (int i = 0; i < CHs.size(); i++) {
			colHdg = CHs.get(i).replaceAll("-", " ");
			if (colHdg.length() > 0 && (nlp.getAllIndexStartLocations(colHdg, ColumnHeadingPattern).size() > 0
					|| nlp.getAllIndexStartLocations(colHdg, NLP.patternInception).size() > 0)) {
				listColHdgs = nlp.getEnddatePeriod(" " + colHdg);
				// NLP.pwNLP.append(//NLP.printLn("colHdgs not blank edt=",
				// listColHdgs.get(0).replaceAll("--", "") + " per="
				// + listColHdgs.get(1)));

				// only add if edt is not 0
				if (listColHdgs.get(0).replaceAll("--", "").length() > 0
						|| nlp.getAllIndexStartLocations(colHdg, NLP.patternInception).size() > 0) {
					if (nlp.getAllIndexStartLocations(colHdg, NLP.patternInception).size() > 0) {
						String[] ary2 = { listColHdgs.get(0).replaceAll("--", ""), "-1" };
						// NLP.pwNLP.append(//NLP.printLn(
						// "2 adding to colHdgEnddatePeriod edt:", ary2[0]
						// + " per:" + ary2[1]));
						colHdgEnddatePeriod.add(ary2);
					} else {
						String[] ary = { listColHdgs.get(0).replaceAll("--", ""), listColHdgs.get(1) };

						// NLP.pwNLP.append(//NLP.printLn(
						// "adding to colHdgEnddatePeriod edt:", ary[0]
						// + " per:" + ary[1]));
						colHdgEnddatePeriod.add(ary);

					}
				}
			}
		}

		return colHdgEnddatePeriod;

		// if colhdgs from tbl are blank then grab entire tbl sentence
		// enddate/period if # of lists in tblsentence edt/per = # of cols and
		// data is otherwise complete in tblsent

		// if tblSent edt is blank - skip.
		// if no year in chdg - we should skip entirely.

	}

	public static List<String> getInsideTable(Document doc) throws IOException, ParseException {

		List<String> listInside = new ArrayList<String>();
		TableParser tp = new TableParser();
		NLP nlp = new NLP();
		colHdgOnSameRowAsTableName = false;

		int tableNameRow = 0;
		Elements TRs = doc.getElementsByTag("tr"); // all row elements
		Elements CAPTION = doc.getElementsByTag("CAPTION"); // all row elements
		Element tr; // a single row
		String textInCellAfterTableName = "";
		// Pattern nmbPattern = Pattern
		// .compile(",[\\d]{3}[^\\d]|\\d\\.\\d|(\\$|\\()[^a-zA-Z]{0,8}\\d\\d[\\d,\\.
		// ]{1,12}[^a-zA-Z]");
		Matcher matchTableNames = null, matchTableNameOnRow2 = null, matchCHThisRow;

		// this determines when tableNameInside &how many rows to ck for colhdg
		int chStartRow = -1;
		// this if condition relates to parsing of tableSentence only
		int tblNameCol = -1;

		Elements TDs; // initializing el ary obj
		boolean tableNameFound = false;
		int a = 0;
		// ck max of 1st 7 tbl rows.
		int r = 0, maxNoRowsToCheck = Math.min(TRs.size(), 9);
		// ck up to 9 rows of table
		int numColsInTableNameRow = 0;
		if (CAPTION.size() == 1) {
			matchTableNames = TableParser.TableNamePattern.matcher(CAPTION.text());
			if (matchTableNames.find())
				tableNameFound = true;
		}

		int minusR = 0;
		// minusR is -1 if co name is on CH row followed by tablename. This
		// way I pick up CHs prior to tn.

		for (; r < maxNoRowsToCheck; r++) {
			tr = TRs.get(r);
			// NLP.pwNLP.append(//NLP.printLn("row text to ck for tableName=",
			// tr.text() + " tableSentence=" + tableSentence));
			TDs = tr.getElementsByTag("td");
			// NLP.pwNLP.append(//NLP.printLn(" tableNameFound?=" +
			// tableNameFound);
			if (!tableNameFound) {
				// we want it in the 1st or 2nd col or skip.
				Element td;
				for (a = 0; a < TDs.size() && a < 2; a++) {
					td = TDs.get(a);

					// NLP.pwNLP.append(//NLP.printLn(
					// "is TableName in this TD text=", td.text()));
					matchTableNames = TableParser.TableNamePattern.matcher(TDs.get(a).text());

					if (r + 1 < maxNoRowsToCheck) {
						// see if tn on next row - and on this there areCHs.
						matchTableNameOnRow2 = TableParser.TableNamePattern.matcher(TRs.get(r + 1).text());
						if (matchTableNameOnRow2.find()
								&& nlp.getAllIndexStartLocations(tr.text(), ColumnHeadingPattern).size() > 1
								&& TDs.size() > 2) {
							int cntCH = 0;
							for (int b = 0; b < TDs.size(); b++) {
								if (nlp.getAllIndexStartLocations(TDs.get(a).text(), ColumnHeadingPattern).size() > 0) {
									cntCH++;
									// must find CH in two separate tabs.
								}
							}
							// if 2 or more CHs on current row and tablename on
							// next - the CHs started already
							if (cntCH > 1) {
								minusR = -1;
							}
						}
					}

					List<String[]> listTn = nlp.getAllEndIdxAndMatchedGroupLocs(TDs.get(a).text(),
							TableParser.TableNamePattern);
					// tableNameRowAllText = TDs.get(a).text();
					tableNameRow = r;
					numColsInTableNameRow = TDs.size();
					if (!matchTableNames.find()) {
						continue;
					} else {
						chStartRow = r;
						tableNameFound = true;
						tblNameCol = a;

						// NLP.pwNLP.append(//NLP.printLn("tableName found=",
						// listTn.get(listTn.size() - 1)[1]
						// + " at table row=" + r
						// + " A colHeadingStartRow=" + chStartRow
						// + " tableName found in col=" + a));

						textInCellAfterTableName = TDs.get(tblNameCol).text()
								.substring(Integer.parseInt(listTn.get(listTn.size() - 1)[0]));

						tableNameLong = listTn.get(listTn.size() - 1)[1].replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n", "");
						tableNameShort = tp.getTableNameShort(tableNameLong);

						// at getTable we captured tableSentence that is on
						// same row and is after tableNameEndIdx.
						// and we now know "a" is td loc (col loc) to ck
						// next 2 rows for additional words to append to
						// tblSent. If however tablename is on same row as
						// CHs - which is true if two or more cols to rt has
						// colhdg pattern

						int cntCHsaftTblNm = 0;
						// cks cols to rt of col where tblnm found and if 2
						// or more cols w/ CH - then set boolean value to
						// true and no further rows ck'd for TS and I reset
						// the starting row value to -10 - which gets picked
						// up below to set it to r
						for (; a < TDs.size(); a++) {
							if (nlp.getAllIndexStartLocations(TDs.get(a).text(), ColumnHeadingPattern).size() > 0) {
								cntCHsaftTblNm++;
							}
						}
						if (cntCHsaftTblNm > 1) {
							colHdgOnSameRowAsTableName = true;
							chStartRow = r;
							// NLP.pwNLP.append(//NLP.printLn("cntCHsaftTblNm=",
							// cntCHsaftTblNm + ""));
						}
						break;
					}
				}
				// if tableName not in 1st 2 cols of this row, go to next
				if (tableNameFound)
					break;
			}
		}

		// NLP.pwNLP.append(//NLP.printLn("tablename found at this row=", "" + r));

		// tablename found inside table - so tablesentence cannot occur
		// prior to to tableName row. This contains all text of tablename
		// row

		if (tableSentence.length() < 1)
			tableSentence = textInCellAfterTableName;

		// NLP.pwNLP.append(//NLP.printLn(
		// "@textInCellAfterTableName tableSentence==", tableSentence));

		String tableSentenceToAppend = tableSentence;

		// problem is picking up AFTER where tablesentence ended
		// this loop gets the tableSentence
		// if tablesentence already has column headings - it is highly unlike
		// that additional tablesentence is on a separate row. And not filtering
		// out subsequent row when CH match already found in 1st TS row causes
		// errors.

		if (!colHdgOnSameRowAsTableName
				&& nlp.getAllIndexStartLocations(tableSentence, TableParser.ColumnHeadingPattern).size() < 1) {
			for (int i = r + 1; i < Math.min(maxNoRowsToCheck, r + 3); i++) {
				// NLP.print("checking next couple rows for tableSentence. row text=",
				// TRs.get(i).text());
				// NLP.pwNLP.append(//NLP.printLn("Math.min(maxNoRowsToCheck, r + 4)="
				// +
				// Math.min(maxNoRowsToCheck, r + 4));

				// r=row, go to next row to ck for tblSent to append to
				// tblSent; and ck max of 2 rows after tableName row.
				// grab text of td==a (a is col of tabeName and must be
				// either
				// col 1/2) and noty empty.
				tr = TRs.get(i);
				if (tr.text().length() < 4)
					continue;
				TDs = tr.getElementsByTag("td");
				// tableSentenceToAppend = "";
				// NLP.pwNLP.append(//NLP.printLn("line of ts?=" + tr.text()+ "
				// tblNameCol="+tblNameCol);
				// tablesentence text must be in same col as tblName

				// tableSentence must be in same col tablename and cols in
				// cur row has equal number of column in tablename row.
				if (null != tr && tr.text().length() > 4 && null != TDs && tblNameCol < TDs.size() && TDs.size() > 0
						&& null != TDs.get(tblNameCol).text() && TDs.get(tblNameCol).text().length() > 4
						&& numColsInTableNameRow == TDs.size()) {
					// NLP.pwNLP
					// .append(NLP
					// .println(
					// "\n1st row to ck for remaining tableSentence inside table=",
					// tr.text()));
					// NLP.pwNLP.append(//NLP.printLn("inside table = append to
					// tableSentence
					// TDs.get(a).text()="+TDs.get(tblNameCol).text());
					tableSentenceToAppend = tableSentence + " " + TDs.get(tblNameCol).text();
					// NLP.pwNLP.append(//NLP.printLn("1. tableSentenceToAppend=",
					// tableSentenceToAppend));

				}

				// I may need to add a filter in addition to location (col
				// below tableName ) to determine if tableSentence condition
				// is met.

				if ((null != tableSentenceToAppend && tableSentenceToAppend.length() > 4)
						|| (null != tableSentence && tableSentence.length() > 5)) {
					chStartRow = i;
					// i=row=r -- if tableSentence then CH starts after it.
					// NLP.pwNLP.append(//NLP.printLn(
					// "final (inside) tableSentence=", tableSentence
					// + " 2 colHeadingStartRow=" + i));

				} else
					break;
			}
		}
		// below parses table sentence to edt and period
		tableSentence = tableSentenceToAppend;

		if (tableSentence != null && tableSentence.length() > 1 && tsShort.length() < 1) {
			// NLP.pwNLP
			// .append(NLP
			// .println(
			// "**tableSentence right before parseTableSentence inside table=",
			// tableSentence));

			List<String> listTableSentenceAndTSshort = NLP.parseTableSentence(tableSentence.replaceAll("[ ]{2,}", " "),
					companyName.substring(0, Math.min(companyName.length(), 7)), false, tableNameLong, false);

			// tableSentence was retrieved after tablename row by stepping
			// down each row in table. parseTableSentence uses ws to find
			// col hdgs - which is not relevant in html - so removed 2 or
			// more ws from ts above

			if (listTableSentenceAndTSshort.get(6).length() > 6) {
				// NLP.pwNLP
				// .append(NLP
				// .println(
				// "@ before listTableSentenceAndTSshort - tableNameLong=",
				// tableNameLong));
				tableNameLong = listTableSentenceAndTSshort.get(6).replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n", "");
				tableNameShort = tp.getTableNameShort(tableNameLong);
				// NLP.pwNLP.append(//NLP.printLn(
				// "@ after listTableSentenceAndTSshort - tableNameLong=",
				// tableNameLong));

			}

			// NLP.pwNLP
			// .append(NLP
			// .println(
			// "**tableSentence right after parseTableSentence inside table=",
			// tableSentence));

			tableSentence = listTableSentenceAndTSshort.get(0);
			tsShort = listTableSentenceAndTSshort.get(1);
			String tsPattern = listTableSentenceAndTSshort.get(2)
					.replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)", "");

			if (coNameMatched.length() < 1 && listTableSentenceAndTSshort.size() > 5
					&& listTableSentenceAndTSshort.get(5).length() > 3) {
				coNameMatched = listTableSentenceAndTSshort.get(5).replaceAll("'", "");
			}

			// NLP.pwNLP.append(//NLP.printLn("from parseTableSentence tsShort=",
			// tsShort + "\ntableSentence=" + tableSentence));

			if (!tsPattern.equals("MY")) {
				endDatesPeriodsListFromTableSentences = NLP.getTablesentenceColumnHeadings(tableSentence, tsShort,
						tsPattern);
			}

			//// NLP.printListOfStringArray(
			// "2 endDatesPeriodsListFromTableSentences right after
			// NLP.getTablesentenceColumnHeadings",
			// endDatesPeriodsListFromTableSentences);
			// NLP.pwNLP.append(NLP
			// .println("tableSentence rt after parseTableSentence=",
			// tableSentence));
		}

		/*
		 * how to get tablensentence when it is inside the table: capture all text
		 * immediately after tableNameIdx that is in the same col as tblNm or immediate
		 * 2 rows after in same col. Note - same col assumes subsequent rows are a col
		 * that has same colspan (although not programmed). Eg., if tableName col has
		 * colspan=4 then immediately following rows will also - otherwise stop.
		 */

		// NLP.pwNLP.append(//NLP.printLn("tableNameRow=", tableNameRow
		// + " tblNameCol=" + tblNameCol + " chStartRow=" + chStartRow
		// + " minusR=" + minusR));

		listInside.add((chStartRow) + " ");
		listInside.add(tableNameRow + " ");
		listInside.add(tblNameCol + " ");
		listInside.add(tableSentence);
		listInside.add(minusR + "");

		return listInside;

	}

	public Elements[] getCHnDataRows(Document doc) throws IOException, SQLException, ParseException {
		Elements chRows = new Elements();
		Elements dataRows = new Elements();

		TableParser tp = new TableParser();
		NLP nlp = new NLP();
		// String tableNameRowAllText ="";
		/*
		 * how to identify CH/data rows: - CH rows must not match the tr text with
		 * pattern: d,dd instead, should match one of the 3 patterns (ended/month/yr) -
		 * if continuous 5 lines do not have CH row, break, and take from last row where
		 * a ch row was found as data rows till end of table
		 */

		int tableNameRow = 0;
		Elements TRs = doc.getElementsByTag("tr"); // all row elements
		Element tr; // a single row
		String lineText;
		Pattern patternMultiNumber = Pattern.compile("\\d\\d  ?\\d\\d");
		Pattern nmbPattern = Pattern.compile(
				",[\\d]{3}(?!\\d)" + "|\\d\\.\\d" + "|(\\$|\\()(?![a-zA-Z]{0,8})\\d\\d[\\d,\\. ](?!{1,12}[a-zA-Z])");
		Matcher matchMultiNumber;

		// this determines when tableNameInside &how many rows to ck for colhdg
		int chStartRow = -1;
		// this if condition relates to parsing of tableSentence only

		// NLP.pwNLP.append(//NLP.printLn("@getCHnDataRows tableNameInsideTable==",
		// tableNameInsideTable + ""));

		int minusR = 0;
		int tblNameCol = -1;
		if (tableNameInsideTable) {
			List<String> listInsideTable = TableParserHTML.getInsideTable(doc);
			// returns: listInside.add(chStartRow+" ");
			// listInside.add(tableNameRow+" ");
			// listInside.add(tblNameCol+" ");
			chStartRow = Integer.parseInt(listInsideTable.get(0).trim());
			tableNameRow = Integer.parseInt(listInsideTable.get(1).trim());
			tblNameCol = Integer.parseInt(listInsideTable.get(2).trim());
			tableSentence = listInsideTable.get(3);
			minusR = Integer.parseInt(listInsideTable.get(4).trim());
			// chStartRow is actually tableName
			// NLP.pwNLP.append(//NLP.printLn(
			// "@getCHnDataRows -- tablenameInside. chStartRow=",
			// chStartRow + " tblNameCol=" + tblNameCol
			// + " tableSentence=" + tableSentence));
		}

		int rowAfterLastCH = -1;
		firstNonChLineNumberNotFound = -1;

		// NLP.pwNLP.append(//NLP.printLn("3c tableSentence=", tableSentence));

		List<String> tmpTSL = NLP.parseTableSentence(tableSentence,
				companyName.substring(0, Math.min(companyName.length(), 7)), false, tableNameLong, false);

		if (tmpTSL.get(6).length() > 6) {
			tableSentence = tmpTSL.get(0);
			tableNameLong = tmpTSL.get(6).replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n", "");
			tableNameShort = tp.getTableNameShort(tableNameLong);
		}

		// NLP.pwNLP.append(//NLP.printLn(" colHdgOnSameRowAsTableName=",
		// colHdgOnSameRowAsTableName + ""));

		if (!colHdgOnSameRowAsTableName && tableNameInsideTable) {
			chStartRow = tableNameRow + 1;
		}
		if (colHdgOnSameRowAsTableName && tableNameInsideTable
		/* && tableSentence.length()<1 */) {

			// minusR is -1 if co name is on CH row followed by tablename. This
			// way I pick up CHs prior to tn.
			chStartRow = tableNameRow + minusR;
		}

		Elements TDs;
		Element td;

		// if none of above are true - chStartRow=-1 but set to zero below.

		// NLP.pwNLP.append(//NLP.printLn("what is chStartRow=", (chStartRow)
		// + " checking for CH rows"));

		int cntMulti = 0, numbersFoundOnRow = 0;
		for (int i = Math.max(chStartRow, 0); i < Math.min(TRs.size(), 12); i++) {
			// if multi lines in cell (where two data lines in same data col
			// cell) found 3 or more times - this loop will in essence stop via
			// 'continue'

			if (cntMulti > 1)
				continue;

			tr = TRs.get(i);
			lineText = tr.text().replaceAll(" ?\\*", "");
			boolean stop = false;

			// NLP.pwNLP.append(//NLP.printLn("aa ch lineText??=====", lineText
			// + "\r i=" + i + " chStartRow=" + chStartRow));

			TDs = tr.getElementsByTag("td");
			if (i < chStartRow) {
				// ckg row prior to tablename to see if it has ch and is after
				// col of tablename col. If ch found at or prior to tblNameCol -
				// stop checking tablename row
				int tdCnt = 0;
				for (int c = 0; c < TDs.size(); c++) {
					td = TDs.get(c);
					// NLP.pwNLP
					// .append(//NLP.printLn(
					// "aa - did I find 'note' in CH cellText=",
					// td.text()));
					if (stop) {
						continue;
					}
					tdCnt = nlp.getAllIndexStartLocations(td.text(), ColumnHeadingPattern).size();
					if (tdCnt > 0 && c <= tblNameCol) {
						stop = true;
						break;
					}
				}
				// NLP.pwNLP.append(//NLP.printLn("row before tableName="+lineText);
			}

			if (lineText.trim().length() == 0)
				continue;
			if (firstNonChLineNumberNotFound < 0) {
				// not yet initialized
				firstNonChLineNumberNotFound = i;
				// consider this line as non-ch line
				// NLP.pwNLP.append(//NLP.printLn("1 firstNonChLineNumberNotFound=",
				// firstNonChLineNumberNotFound + ""));
			}

			// TODO: chg ColumnHeadingPattern-cks entire tr - dont have[\\d,]
			lineText = lineText.replaceAll("(?i)([\\$']{1,2}000|000s)", "");
			// b/c lineText is not associated with firstNonCHLhnieNumberNotFound
			// we still run 'if' test below and ck 'if' condition after it until
			// (i-firstNonChLineNumberNotFound)>5.
			Matcher matchNmb = nmbPattern.matcher(lineText);

			List<String> chList = nlp.getAllMatchedGroups(lineText, ColumnHeadingPattern);
			// NLP.pwNLP.append(//NLP.printLn("lineText="+lineText+
			// "\rA chList.size="+chList.size()+" lineNo="+i);
			int cntTn = 0;
			boolean nmbFound = false;
			numbersFoundOnRow = 0;
			String cellTxt = "";
			for (int p = 0; p < TDs.size(); p++) {
				td = TDs.get(p);
				cellTxt = td.text();
				// NLP.pwNLP.append(//NLP.printLn("AAA 'NOTE' found? -- cellTxt="
				// +
				// cellTxt+" row="+i+ " column="+p+" nmbFound="+nmbFound);
				if (p > 0) {
					matchNmb = nmbPattern.matcher(cellTxt.replaceAll("\\$|\\(|\\)", ""));
					if (matchNmb.find() || (cellTxt.length() > 0
							&& cellTxt.replaceAll("\\(?\\)?\\$? ?\\d\\d\\d ?", "").length() == 0)) {
						nmbFound = true;
						// NLP.pwNLP.append(//NLP.printLn("matchNmb nmbFound=" +
						// nmbFound
						// + " cellTxt=" + cellTxt+ " row="+i+
						// " column="+p+" nmbFound="+nmbFound);

						if (nlp.getAllIndexEndLocations(lineText, TableParser.ColumnHeadingPattern).size() < 1
								&& nlp.getAllMatchedGroups(cellTxt, TableParser.TableNamePattern).size() < 1) {
							numbersFoundOnRow++;
						}
					}

					matchMultiNumber = patternMultiNumber.matcher(cellTxt);
					if (matchMultiNumber.find() && nmbFound) {
						if (cntMulti == 0) {
							firstNonChLineNumberNotFound = i;
							// NLP.pwNLP.append(//NLP.printLn(
							// "2 firstNonChLineNumberNotFound=",
							// firstNonChLineNumberNotFound + ""));

						}
						cntMulti++;
						// it mult-lines in same cell (rows within a cell) -
						// than on 3 instance - this loop ends - see at start of
						// loop - if CntMulti>2.
					}

				}
				cntTn = nlp.getAllMatchedGroups(cellTxt, TableParser.TableNamePattern).size();
			}

			// NLP.pwNLP.append(//NLP.printLn("chList.size()===", chList.size()
			// + " nmbFound=" + nmbFound + " lineText=" + lineText + ""));

			if ((chList.size() - cntTn) > 0 && !nmbFound && numbersFoundOnRow < 2) {
				// TODO: fix matchNmb not to pickup 1/27/07
				// ",[\\d]{3}[^\\d]|\\d\\.\\d|(\\$|\\().{0,8}\\d\\d[\\d,\\. ]"

				firstNonChLineNumberNotFound = -1;
				// NLP.pwNLP.append(//NLP.printLn(
				// "3 reset firstNonChLineNumberNotFound=",
				// firstNonChLineNumberNotFound + ""));

				// reset to -1 so it goes through the loop again
				// this loop cks if any cell only has numbers - if so that row
				// cant be a CH
				// NLP.pwNLP.append(//NLP.printLn("chRows == tr.text=", tr.text()));
				String cellText;
				Pattern ptrnYear = Pattern.compile("[12]{1}[09]{1}[0-9]{2}");
				Matcher mtchYear;
				int cntData = 0;
				for (int q = 0; q < TDs.size(); q++) {
					td = TDs.get(q);
					cellText = td.text().replaceAll("[\\$\\(\\) -]{1,}", "");
					// NLP.pwNLP.append(//NLP.printLn("fetching chRows -- cellText="
					// +
					// cellText);
					mtchYear = ptrnYear.matcher(cellText);
					if (mtchYear.find())
						continue;
					if (q > 0// 186
							&& (cellText.length() > 1 && cellText.replaceAll("[\\d]{1}", "").length() == 0) ||
					// 1,234
							(cellText.length() > 4 && cellText.replaceAll("[\\d,]{5,7}", "").length() == 0)

					) {
						cntData++;
						// NLP.pwNLP.append(//NLP.printLn("cellText="+cellText+" cntData=="
						// + cntData);
					}
				}

				if (cntData == 0) {
					rowAfterLastCH = i + 1;
					chRows.add(tr);

					// NLP.pwNLP.append(//NLP.printLn("last row added="+i+" adding chRows="
					// + tr.text());

					firstNonChLineNumberNotFound = i;
					// NLP.pwNLP.append(//NLP.printLn(
					// "4 firstNonChLineNumberNotFound=",
					// firstNonChLineNumberNotFound + ""));

				}
			}

			// NLP.pwNLP.append(//NLP.printLn("A chRows.Size=="+chRows.size());
			if ((firstNonChLineNumberNotFound > 0 && (i - firstNonChLineNumberNotFound) > 5) || numbersFoundOnRow > 1) {
				// NLP.pwNLP.append(//NLP.printLn(
				// "5 break b/c firstNonChLineNumberNotFound=", ""));

				// we have not yet found CH line since last 5 rows, meaning must
				// have started data rows
				// NLP.pwNLP.append(//NLP.printLn("chRows.size="+chRows.size()+" 1 break"+"
				// firstNonChLineNumberNotFound="+firstNonChLineNumberNotFound+" i="+i);
				break;
			}

			// NLP.pwNLP.append(//NLP.printLn("5b firstNonChLineNumberNotFound=",
			// firstNonChLineNumberNotFound + " nmbFound=" + nmbFound
			// + " numbersFoundOnRow==" + numbersFoundOnRow));
		}

		firstNonChLineNumberNotFound = rowAfterLastCH;
		// NLP.pwNLP.append(//NLP.printLn(
		// "6 rowAfterLastCH=firstNonChLineNumberNotFound=", ""
		// + firstNonChLineNumberNotFound));

		// NLP.pwNLP.append(//NLP.printLn("firstNonChLineNumberNotFound="
		// + firstNonChLineNumberNotFound + " TRs.size()=" + TRs.size());

		// allowing for no col hdg found
		boolean noColHdgFound = false;
		if (firstNonChLineNumberNotFound == -1) {
			// NLP.pwNLP.append(//NLP.printLn(
			// "7 firstNonChLineNumberNotFound==-1. noColHdgFound=true",
			// ""));
			noColHdgFound = true;
		}
		// if firstNonChLineNumberNotFound==0 -- then all are data row - and I
		// try to pair with prior table if prior table has col hdgs [only] and
		// prior table end idx is within 20 of start of the data table.

		// NLP.pwNLP.append(//NLP.printLn("right before I get data rows -
		// textBetweenTables="+textBetweenTables);

		if (firstNonChLineNumberNotFound == -1 && !textBetweenTables) {
			firstNonChLineNumberNotFound = 0;
			tableNameLong = priorTableNameLong.replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n", "");
			// NLP.pwNLP.append(//NLP.printLn(
			// "setting tablename to --priorTablenameLong"
			// + priorTableNameLong, ""));
		}

		// NLP.pwNLP.append(//NLP.printLn(
		// "@dataRows --- # of dataRows potentiall are -- TRs.size="
		// + TRs.size(), ""));
		if (TRs.size() > 1 && firstNonChLineNumberNotFound >= 0) {
			for (int i = firstNonChLineNumberNotFound; i < TRs.size(); i++) {
				// if data rows are blank - skip?
				if (TRs.get(i).text().length() > 0) {
					dataRows.add(TRs.get(i));
					// NLP.pwNLP.append(//NLP.printLn(
					// "adding dataRow==" + TRs.get(i).text() + " dataRow.len="
					// + TRs.get(i).text().length());
				}
			}
		}

		// NLP.pwNLP.append(//NLP.printLn("noColHdgFound=" + noColHdgFound, ""));
		// NLP.pwNLP.append(//NLP.printLn("dataRows.size():" + dataRows.size(), ""));
		// NLP.pwNLP.append(//NLP.printLn("z chRows.size():" + chRows.size(), ""));

		// NLP.pwNLP.append(//NLP.printLn("CH Rows:" + chRows.text());
		// NLP.pwNLP.append(//NLP.printLn("data Rows:" + dataRows.text());
		return new Elements[] { chRows, dataRows };
	}

	public void getHtmlIdxOnly(Elements chRows) {

		Map<Integer, List<String[]>> mapCHs2 = new HashMap<Integer, List<String[]>>();
		Map<Integer, String> mapHtml = new TreeMap<Integer, String>();

		mapCHs2 = mapCHs;
		// NLP.pwNLP.append(//NLP.printLn("3 ACCNO=" + acc, ""));

		// b/c it is html - I should take each ch cell and check every
		// subsequent row to see if that cell's idx is with another cell's or
		// another cell is within its. The below loop.
		// mapCh - key=row#,List of string has cell info, [0]=start col idx, [1]
		// end col idx, cell text (ch). eg:
		// key=0| [9, 13, Years ended June 30,]
		// key=1| [3, 4, Year ended September 30, 2003]
		// this only works if there are only 2 lines necessary to merge.
		// listOfStrHtmlColHdgs is only used later in parsing into mysql when
		// other methodology failed. If more than 2 cells lines merge - it will
		// miss the 3rd

		@SuppressWarnings("unused")
		int sIdx = 0, sIdx2 = 0, eIdx = 0, eIdx2 = 0, k1 = 0;
		List<String[]> listTmpMerged = new ArrayList<>();
		List<String[]> listTmpMerged2 = new ArrayList<>();
		String cTxt = "", cTxt2 = "";
		// for each row - key- see if any other row's arrays in the list (cell)
		// pair. get list - for each array in list compare against all other
		// arrays of all other lists that don't share the same map key

		// TODO: PUT THIS ENTIRE ASPECT IN ITS OWN METHOD
		listOfStrHtmlColHdgs.clear();
		List<String> listCtxtAdded = new ArrayList<>();
		boolean cTxtAdded = false, foundCtxt = false;
		for (int i = 0; i < mapCHs.size(); i++) {
			listTmpMerged = mapCHs.get(i);
			// NLP.pwNLP.append(//NLP.printLn("i==" + i, ""));
			// fetch cell txt which I'll use to try and pair with other cell
			// text on subsequent row
			for (int c = 0; c < listTmpMerged.size(); c++) {
				sIdx = Integer.parseInt(listTmpMerged.get(c)[0]);
				eIdx = Integer.parseInt(listTmpMerged.get(c)[1]);
				cTxt = listTmpMerged.get(c)[2];
				// NLP.pwNLP.append(//NLP.printLn("i=" + i + " c=" + c + " cTxt="
				// + cTxt
				// + " sIdx=" + sIdx + " eIdx=" + eIdx);
				cTxtAdded = false;
				// fetch cell text to pair that is at a later key (i+1)
				for (int n = i + 1; n < mapCHs2.size(); n++) {
					listTmpMerged2 = mapCHs2.get(n);
					// i is outer loop key - ony ckg keys after i
					for (int p = 0; p < listTmpMerged2.size(); p++) {
						sIdx2 = Integer.parseInt(listTmpMerged2.get(p)[0]);
						eIdx2 = Integer.parseInt(listTmpMerged2.get(p)[1]);
						cTxt2 = listTmpMerged2.get(p)[2];
						// NLP.pwNLP.append(//NLP.printLn("i=" + i + " c=" + c
						// + " cTxt=" + cTxt + " sIdx=" + sIdx + " eIdx="
						// + eIdx, ""));
						// NLP.pwNLP.append(//NLP.printLn("i=" + i + " c=" + c
						// + " n=" + n + " p=" + p + " cTxt2=" + cTxt2
						// + " sIdx2=" + sIdx2 + " eIdx2=" + eIdx2, ""));
						if ((sIdx <= sIdx2 && eIdx >= eIdx2) || (sIdx2 <= sIdx && eIdx2 >= eIdx)) {
							// NLP.pwNLP.append(//NLP.printLn(
							// "this is a pair - cTxt=" + cTxt + " cTxt2="
							// + cTxt2, ""));
							// NLP.pwNLP.append(//NLP.printLn("sIdx=" + sIdx
							// + " eIdx" + eIdx + " sIdx2=" + sIdx2
							// + " eIdx2=" + eIdx2, ""));
							// need record in map without regard to row but
							// based on least sIdx-eIdx value in order to get
							// correct order
							if (eIdx - sIdx < eIdx2 - sIdx2) {
								mapHtml.put(sIdx, cTxt + " " + cTxt2);
							} else {
								k1++;
								mapHtml.put(sIdx2, cTxt + " " + cTxt2);
							}
							listCtxtAdded.add(cTxt);
							listCtxtAdded.add(cTxt2);
							cTxtAdded = true;
						}

						// if cTxt has check all cTxt2 for pairings and none
						// found then add to list
					}
				}
				foundCtxt = false;
				if (!cTxtAdded) {
					for (int q = 0; q < listCtxtAdded.size(); q++) {
						if (listCtxtAdded.get(q).equals(cTxt)) {
							// NLP.pwNLP.append(//NLP.printLn("foundCtXt", ""));
							foundCtxt = true;
							break;
						}
					}

					if (!foundCtxt) {
						// NLP.pwNLP.append(NLP
						// .println("did not find cTxt and addind cTxt="
						// + cTxt, ""));
						k1++;
						mapHtml.put(sIdx, cTxt);

					}
				}
			}
		}

		//// NLP.printMapIntStr("mapHtml", mapHtml);

		for (Map.Entry<Integer, String> entry : mapHtml.entrySet()) {
			// NLP.pwNLP.append(//NLP.printLn("key|" + entry.getKey() + "| val|"
			// + entry.getValue() + "|");
			listOfStrHtmlColHdgs.add(entry.getValue().replaceAll("'", ""));
		}

		//// NLP.printListOfString("listOfStrHtmlColHdgs", listOfStrHtmlColHdgs);
	}

	public List<String> mergeCHs(Elements chRows, Elements dataRows) throws IOException, ParseException, SQLException {

		// NLP.pwNLP.append(//NLP.printLn(
		// "mergedCHs2 # of CH rows=" + chRows.size(), ""));
		TableParser tp = new TableParser();
		NLP nlp = new NLP();

		// Element td;
		// chRow,sIdxCol,eIdxCol (sIdxCol+colspan),columnText
		mapCHs.clear();
		mapCHs = getHtmlColHdgMap(chRows, dataRows);
		// NLP.pwNLP.append(//NLP.printLn("just returned mapCHs - printing mapCHs",
		// ""));
		//NLP.printMapIntListOfStringAry("mapCHs", mapCHs);
		/* mapCHs2 =mapCHs; */
		// NLP.pwNLP.append(//NLP.printLn("3 ACCNO=" + acc, ""));

		// b/c it is html - I should take each ch cell and check every
		// subsequent row to see if that cell's idx is with another cell's or
		// another cell is within its. The below loop.
		// mapCh - key=row#,List of string has cell info, [0]=start col idx, [1]
		// end col idx, cell text (ch). eg:
		// key=0| [9, 13, Years ended June 30,]
		// key=1| [3, 4, Year ended September 30, 2003]
		// this only works if there are only 2 lines necessary to merge.
		// listOfStrHtmlColHdgs is only used later in parsing into mysql when
		// other methodology failed. If more than 2 cells lines merge - it will
		// miss the 3rd

		/*
		 * pick up where CH is actually over rowname - by seeing if MapCH has found
		 * 'year' only over data cols (not in col 1/2) and I have MM-DD and P in col
		 * over rowname.
		 */

		List<String> listEnded = nlp.getAllMatchedGroups(chAboveRowname, TableParser.EndedHeadingPattern);
		List<String> listMonth = nlp.getAllMatchedGroups(chAboveRowname, TableParser.MonthPatternSimple);
		int cntM = listMonth.size(), cntE = listEnded.size();

		if ((cntE > 0 || cntM > 0) && tableSentence.length() < 2 && tsShort.length() < 1) {
			// NLP.pwNLP.append(//NLP.printLn("22 chAboveRowname=" + chAboveRowname,
			// ""));
			//NLP.printMapIntListOfStringAry("mapCHs", mapCHs);
			chAboveRowname = nlp.confirmItIsColHdgAboveRowname(mapCHs, chAboveRowname);
			tableSentence = chAboveRowname.trim();
			// NLP.pwNLP.append(//NLP.printLn("@chAboveRowname
			// conameparsed--coNameMatched="+coNameMatched);
			// NLP.pwNLP.append(//NLP.printLn("chAboveRowname confirmed=",
			// tableSentence));
			// can't put BS as dummy tableNameLong else corrupt outcome of
			// tableNameLong
			List<String> listTableSentenceAndTSshort = NLP.parseTableSentence(
					tableNameLong + "\r\n" + tableSentence.replaceAll("[ ]{2,}", " "),
					companyName.substring(0, Math.min(companyName.length(), 7)), false, tableNameLong, false);
			if (listTableSentenceAndTSshort.get(6).replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n", "").length() > 6) {
				tableNameLong = listTableSentenceAndTSshort.get(6).replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n", "");
				tableNameShort = tp.getTableNameShort(tableNameLong);
			}

			if (coNameMatched.length() < 1 && listTableSentenceAndTSshort.size() > 5
					&& listTableSentenceAndTSshort.get(5).length() > 3) {
				coNameMatched = listTableSentenceAndTSshort.get(5).replaceAll("'", "");
			}

			// tableSentence was retrieved after tablename row by stepping
			// down each row in table. parseTableSentence uses ws to find
			// col hdgs - which is not relevant in html - so removed 2 or
			// more ws from ts above

			tableSentence = listTableSentenceAndTSshort.get(0);
			tsShort = listTableSentenceAndTSshort.get(1);
			String tsPattern = listTableSentenceAndTSshort.get(2)
					.replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)", "");

			// NLP.pwNLP.append(//NLP.printLn("from parseTableSentence
			// tsShort="+tsShort+"\ntableSentence="+tableSentence);

			if (!tsPattern.equals("MY")) {
				endDatesPeriodsListFromTableSentences = NLP.getTablesentenceColumnHeadings(tableSentence, tsShort,
						tsPattern);
			}

			// NLP.pwNLP
			// .append(NLP
			// .println(
			// "4 endDatesPeriodsListFromTableSentences right after
			// NLP.getTablesentenceColumnHeadings=",
			// ""));
			//NLP.printListOfStringArray("endDatesPeriodsListFromTableSentences",
			// endDatesPeriodsListFromTableSentences);
			// NLP.pwNLP.append(//NLP.printLn(
			// "tableSentence rt after parseTableSentence="
			// + tableSentence, ""));
		}

		// printing ary=[9, 13, Years ended June 30, 2002]
		// printing ary=[6, 7, For the three months ended September 30, 2002]
		// printing ary=[3, 4, Year ended September 30, 2003]

		// NLP.pwNLP.append(//NLP.printLn("b. printing mapCHs");
		//// NLP.printMapIntListOfStringAry(mapCHs);

		// because of rowspans there can be cols that have nothing above or
		// below it and there can be cells that should pair with a cell 2 rows
		// below it. That won't get picked up using method below unless I create
		// a dummy row with ch text in each cell that doesn't overlap with any
		// other

		/*
		 * small list is 1st list in map & has list of cells w/ potential CH text. this
		 * will pair any small CH whose col spans the other row's CH. So if the small CH
		 * spans cols 1 thru 3 and the large CH has a cell at col2 - that small CH gets
		 * paired with that large CH. The result is a large list with the underlying
		 * large list col spans
		 */
		List<String[]> smList = new ArrayList<String[]>();
		List<String[]> lgList = new ArrayList<String[]>();
		List<String[]> mergedList = new ArrayList<String[]>();
		int smColIdxStart = 0, smColIdxEnd, lgColIdxStart, lgColIdxEnd;
		String smCHtxt = "", lgCHtxt = "";
		// NLP.pwNLP.append(//NLP.printLn("mapCHs.size=" + mapCHs.size());

		// always carry the lg list with what is merged as final list. Take any
		// item that col hdg pattern or after 4 or 5 col idx.

		boolean merged = false, reverse = false;
		/*
		 * Merges 1st 2 rows. NOTE: If sm and lg list sizes are equal then it can fail
		 * to properly pair b/c the assumption is a lg list can't merge across 2 sm list
		 * CHs. But if there is a rowspan - then I am currently putting that cell at
		 * first row. However it I need to put it on last row of rowspan. B/c I have not
		 * fixed that-the sm list (too few CHs) can be the bottom row when in fact it is
		 * the lg list. B/c a lg list by def can't merge across 2 sm ch - this creates a
		 * problem b/c in this example the sm list can get mis-designated as the lg
		 * list. Generally this shouldn't occur but rarely and then ususally the 2 lists
		 * are the same size - so to fix it temporarly I've deferred to the last key
		 * being the lg list (last row). That will fix most of the time. ROOT CAUSE:
		 * designate the key of a rowspan CH as the the last row in the rowspan!!!
		 */

		if (mapCHs.size() > 1) {
			if (mapCHs.get(1).size() >= mapCHs.get(0).size()) {
				smList = mapCHs.get(0);
				lgList = mapCHs.get(1);
				// list=[0]col# (start),[1] col# (end), [2]colHdg txtt
				reverse = false;
			} else {
				smList = mapCHs.get(1);
				lgList = mapCHs.get(0);
				reverse = true;
			}
		}

		//// NLP.printListOfStringArray("1 smList", smList);
		//// NLP.printListOfStringArray("1 lgList", lgList);

		int n;
		// I merge in reverse b/c greater accuracy left to right
		for (n = lgList.size() - 1; n >= 0; n--) {

			lgColIdxStart = Integer.parseInt(lgList.get(n)[0]);
			lgColIdxEnd = Integer.parseInt(lgList.get(n)[1]);
			lgCHtxt = lgList.get(n)[2];
			// NLP.pwNLP.append(NLP
			// .println("1. lgColIdxStart=" + lgColIdxStart + " lgColIdxEnd=" +
			// lgColIdxEnd + " lgCHtxt=" + lgCHtxt);

			if (n < (lgList.size() - 1) && !merged
					&& (nlp.getAllIndexEndLocations(lgCHtxt, ColumnHeadingPattern).size() > 0
							|| nlp.getAllIndexEndLocations(lgCHtxt, NLP.patternInception).size() > 0)
					&& (nlp.getAllIndexEndLocations(smCHtxt, ColumnHeadingPattern).size() > 0
							|| nlp.getAllIndexEndLocations(smCHtxt, NLP.patternInception).size() > 0)) {
				if (!reverse) {
					String[] ary = { lgList.get(n)[0], lgList.get(n)[1], smCHtxt + " " + lgCHtxt };
					mergedList.add(ary);
					// NLP.pwNLP.append(//NLP.printLn("!reverse merged colTxt=",
					// Arrays.toString(ary)));
					//NLP.printListOfStringArray("!reverse mergedList",
					// mergedList);
				}
				if (reverse) {
					String[] ary = { lgList.get(n)[0], lgList.get(n)[1], smCHtxt + " " + lgCHtxt };
					mergedList.add(ary);
					// NLP.pwNLP.append(//NLP.printLn("reverse merged colTxt=",
					// Arrays.toString(ary)));
					//NLP.printListOfStringArray("reverse mergedList",
					// mergedList);
				}
			}
			// add lgList
			merged = false;
			for (int c = 0; c < smList.size(); c++) {

				// NOTE THIS IS WHERE THERE ARE JUST 2 ROWS THAT MAKE UP CHs
				// LATER IF MAP SIZE IS 3 (3 ROWS) ANOTHER ITERATION OF THIS IS
				// RUN VIA CALLING METHOD: mergeTwoListBasedOnIdx

				if (merged) {
					// NLP.pwNLP.append(//NLP.printLn("1 continue -- merged="
					// + merged, ""));
					continue;
				}

				smColIdxStart = Integer.parseInt(smList.get(c)[0]);
				smColIdxEnd = Integer.parseInt(smList.get(c)[1]);

				// NLP.pwNLP.append(//NLP.printLn("A lgColIdxStart=", lgColIdxStart
				// + " lgColIdxEnd=" + lgColIdxEnd + " lgCHtxt=" + lgCHtxt
				// + "\rmerged=" + merged));
				// NLP.pwNLP
				// .append(//NLP.printLn("B smColIdxStart=" + smColIdxStart,
				// " smColIdxEnd=" + smColIdxEnd + " smCHtxt="
				// + smList.get(c)[2]));
				// #2 where sm ch doesn't stretch to very right of last lg CH
				// but clearly relates b/c it spans the entire lg ch as
				// evidenced by sm ch start idx preceding lg ch start idx.
				Pattern patternMo = Pattern.compile("(?i)(jan|feb|march|april|may |june|july|aug|sep|oct|nov|dec)");
				if (((lgColIdxEnd <= smColIdxEnd && lgColIdxStart >= smColIdxStart)
						|| (lgColIdxEnd >= smColIdxEnd && lgColIdxStart <= smColIdxStart)) && !merged
						|| (smColIdxEnd - smColIdxStart == 1
								&& smCHtxt.replaceAll("[12]{1}[09]{1}[0-9]{2}", "").length() == 0)
								&& nlp.getAllIndexEndLocations(lgCHtxt, patternMo).size() > 0
								&& (smColIdxEnd == lgColIdxStart)
						|| (smColIdxStart == lgColIdxEnd) || (lgColIdxEnd - 1 == smColIdxEnd
								&& lgColIdxStart - smColIdxStart > 2 && n == lgList.size() - 1)// #2 - see above
				) {

					// A lgColIdxStart==14 lgColIdxEnd=15 lgCHtxt=Stage
					// merged=false||END
					// B smColIdxStart=14= smColIdxEnd=15
					// smCHtxt=Exploration||END

					// last or condition logic is:
					// if difference between smColIdxEnd-smColIdxStart is 1 then
					// if it merely overlaps with lgColIdx (end or start) it
					// would be paired - and if limit to instances where
					// smallColTxt is a year and lgColText contains a month it
					// should not cause errors.
					merged = true;
					smCHtxt = smList.get(c)[2];
					if (!reverse && merged) {
						String[] ary = { lgList.get(n)[0], lgList.get(n)[1], smCHtxt + " " + lgCHtxt };
						mergedList.add(ary);
						// NLP.pwNLP.append(//NLP.printLn(
						// "!reverse this list was added:",
						// Arrays.toString(ary)));
						//NLP.printListOfStringArray("mergedList", mergedList);
						// NLP.pwNLP.append(//NLP.printLn("?merged=", merged + ""));
					}
					if (reverse && merged) {
						// list=[0]col# (start),[1] col# (end),
						// [2]column heading text
						String[] ary = { lgList.get(n)[0], lgList.get(n)[1], lgCHtxt + " " + smCHtxt };
						mergedList.add(ary);
						// NLP.pwNLP.append(//NLP.printLn(
						// "reverse this list was added:",
						// Arrays.toString(ary)));
						//NLP.printListOfStringArray("mergedList", mergedList);
						// NLP.pwNLP.append(//NLP.printLn("?merged=", merged + ""));
					}
				}
			}

			if (!merged && (nlp.getAllIndexEndLocations(lgCHtxt, ColumnHeadingPattern).size() > 0
					|| nlp.getAllIndexEndLocations(lgCHtxt, NLP.patternInception).size() > 0)) {
				String[] ary = { lgList.get(n)[0], lgList.get(n)[1], lgCHtxt };
				mergedList.add(ary);
				merged = true;
				// NLP.pwNLP.append(//NLP.printLn("just this lgList was added:",
				// Arrays.toString(ary)));
				//NLP.printListOfStringArray("mergedList", mergedList);
				// NLP.pwNLP.append(//NLP.printLn("?merged=", merged + ""));
			}
		}

		//NLP.printListOfStringArray("2 smList", smList);
		//NLP.printListOfStringArray("2 lgList", lgList);
		//NLP.printListOfStringArray("2 - 1st loop complete - mergedList",
		// mergedList);
		// FIX! HERE AS WELL

		if (mapCHs.size() == 1) {
			mergedList = mapCHs.get(0);
		}

		if (mapCHs.size() > 2) {
			//NLP.printMapIntListOfStringAry(
			// "yikes mapCHs.size>2 -- but oh frig- the 3rd is frign odd ball. mapCHs",
			// mapCHs);

			for (int i = 2; i < mapCHs.size(); i++) {
				if (mergedList.size() >= mapCHs.get(i).size()) {
					smList = mapCHs.get(i);
					lgList = mergedList;
					reverse = false;
				} else {
					lgList = mapCHs.get(i);
					smList = mergedList;
					reverse = true;
				}

				mergedList = mergeTwoListBasedOnIdx(lgList, smList, reverse);
				//// NLP.printListOfStringArray("2a mergedList", mergedList);
			}
		}

		if (mapCHs.size() > 2) {
			//// NLP.printListOfStringArray("3 mergedList", mergedList);
			//// NLP.printListOfStringArray("3 smList", smList);
		}

		String str = "";
		colHeadingsList.clear();

		// order is based on colIdx - mergedList[0]=startColIdx of CH,
		// [1]=endColIdx of CH ([0] to [1] are the columns the CH span). If
		// first list of String in mergedList has a startColIdx less than the
		// last list of String startColIdx - the CHs are in correct order
		// If last instance of colIdx is less than first -mergedList is in
		// reverse order - so I reverse it back when adding to colheadinglist

		// mergedList =[0]col# (start),[1] col# (end),
		// [2]column heading text

		// If this is the final mergedList -- I should not have stranded any
		// mapCHs. But far rt CH might not be captured b/c rowspan annomaly-then
		// I'd pick that up. By seeing its start col idx is greater than
		// greatest value in mergedList. Rowspan annomaly makes it difficult to
		// assign that CH to either the 1st or 2nd row in the rowSpan. If top
		// aligned it is first if bottom aligned it is second. But I do know
		// that if its start and end col Idx is not w/n any of mergedList - it
		// is missing and should be added based on it col start and end idx.
		// for now I will assume the sm CH is key=0 (usually is) and that the
		// potential missing CH is the last col of sm CH (doesn't seem possible
		// to miss lg CHs). I ck if last col of sm CH is missing by seeing if
		// the last col start idx of sm CH is greater than last col's end idx of
		// lg list. Then I know this sm CH should be by itself at far right of
		// col - and gets added to merged list if it isn't already in it (which
		// seems to be the case when there's a rowspan associated with the last
		// sm CH). See below marked: find missing last col sm ch start /end

		// find miss last col sm ch start
		List<String[]> listTmpSmCh = new ArrayList<String[]>();
		int smChLastColStartIdx = 0, tmpChEndIdx = 0;
		boolean missingSmCh = true;
		String missingSmChToAdd = "";
		if (mergedList.size() > 0) {
			if (mapCHs.size() == 2 && mapCHs.get(0).size() <= mapCHs.get(1).size()) {
				// <<=I make sure smCH is key=0
				listTmpSmCh = mapCHs.get(0);
				smChLastColStartIdx = Integer.parseInt(listTmpSmCh.get(listTmpSmCh.size() - 1)[0]);
				// NLP.pwNLP.append(//NLP.printLn("A lastColStartIdx=="
				// + smChLastColStartIdx, ""));
			}
			for (int i = 0; i < mergedList.size(); i++) {
				tmpChEndIdx = Integer.parseInt(mergedList.get(i)[1]);
				if (tmpChEndIdx >= smChLastColStartIdx) {
					missingSmCh = false;
					// if last sm ch has a start idx that isn't greater than all
					// others it can't be missing
				}
			}
			if (missingSmCh && listTmpSmCh.size() > 0) {
				missingSmChToAdd = listTmpSmCh.get(listTmpSmCh.size() - 1)[2];
			}
		}
		// end find miss last col sm ch start

		// if - missingSmChToAdd is >0 - add to merged list. I add as last item
		// in list
		if (mergedList.size() > 0) {
			int first = Integer.parseInt(mergedList.get(0)[0]);
			int last = Integer.parseInt(mergedList.get(mergedList.size() - 1)[0]);
			if (last < first) {
				//// NLP.printListOfStringArrayInReverse("mergedList", mergedList);
				for (int i = (mergedList.size() - 1); i >= 0; i--) {
					str = nlp.getDateFromSlashMoDyYear(" " + mergedList.get(i)[2]);
					if (nlp.getAllIndexStartLocations(str, ColumnHeadingPattern).size() > 0
							|| nlp.getAllIndexStartLocations(str, NLP.patternInception).size() > 0) {
						colHeadingsList.add(str.replaceAll("[\\\\,\\']", "").replaceAll("[\r\n]", ""));
					}
				}
			} else {
				//// NLP.printListOfStringArray("mergedList", mergedList);

				for (int i = 0; i < mergedList.size(); i++) {
					str = nlp.getDateFromSlashMoDyYear(" " + mergedList.get(i)[2]);
					// NLP.pwNLP.append(//NLP.printLn("str=" + str, ""));
					if (nlp.getAllIndexStartLocations(str, ColumnHeadingPattern).size() > 0) {
						colHeadingsList.add(str.replaceAll("[\\\\,\\']", "").replaceAll("[\r\n]", ""));
					}
				}
			}

			if (missingSmChToAdd.length() > 0) {
				colHeadingsList.add(missingSmChToAdd.replaceAll("[\\\\,\\']", "").replaceAll("[\r\n]", ""));
			}
		}

		//// NLP.printListOfString("colHeadingsList", colHeadingsList);
		return colHeadingsList;

	}

	public static Map<Integer, List<String[]>> getHtmlColHdgMap(Elements chRows, Elements dataRows)
			throws IOException, ParseException, SQLException {

		Map<Integer, Integer> mapDataColIdxsCount = NLP.getStartIdxOfEachDataCol(dataRows);
		// mapDataColIdx: key=data col idx (col #), value=number of times found.
		int numberOfDataColumns = mapDataColIdxsCount.size();

		Integer maxNumberChsOnAnyRow = NLP.getMaxNumberOfChsOnAnyChRow(chRows);
		// NLP.pwNLP
		// .append(NLP
		// .println(
		// "\rmaxNumberChsOnAnyRow (ruff count of number of CHs="
		// + maxNumberChsOnAnyRow
		// + "\r ruff count of number of data cols="
		// + numberOfDataColumns, ""));
		// get row with maxNumberOfChs - this is a RUFF measure. if that value
		// is equal to number of data cols - I know there is no ch above rowname

		/*
		 * NOTE: this method will return each CH's start and end column idx. Many CHs
		 * span multiple columns (colpsan) and in order to pair multiple rows of
		 * colspans I need to know where on each row that CHs sits (it idx) and how many
		 * columns it spans. This will get the start and end column idx location for
		 * each CH. The complexity is rowSpans - which require adjustments of col idx
		 * for each row subsequent to the initial rowspan for which that rowspan spans.
		 * getRowSpans is called by this method and if there are rowSpans this method
		 * scans the retrieved rowspan list and adjusts the col idx accordingly.
		 */

		NLP nlp = new NLP();
		Element td = null;
		Elements TDs;

		Map<Integer, String[]> mapRowColIdx = new TreeMap<Integer, String[]>();

		// return map, key=row,List<String[]>:: [0]=col hdg startIdx,[1]=col hdg
		// endIdx,[2]=colhdg text

		/*
		 * ROWSPAN RULESET: if row is after 1st row of rowspan and w/n rowspan range and
		 * the current start col idx equals the rowspan start idx then the current start
		 * col idx is set to to the rowspan end idx plus 1.
		 */

		String data = "";
		int firstDataColIdx = 99;
		boolean foundFirstDataColIdx = false;
		for (int i = 0; i < Math.min(dataRows.size(), 6); i++) {
			foundFirstDataColIdx = false;
			TDs = dataRows.get(i).getElementsByTag("td");
			// data col can't be 0. first data col idx shouldn't be after col 5
			for (int n = 1; n < Math.min(TDs.size(), 5); n++) {
				data = TDs.get(n).text();
				// NLP.pwNLP.append(//NLP.printLn("td data txt ==="+data);
				if (data.replaceAll("[ -]", "").length() > 0
						&& data.replaceAll("[\\$\\(\\) \\d\\-,\\.]", "").length() == 0) {
					if (n < firstDataColIdx)
						firstDataColIdx = n;
					foundFirstDataColIdx = true;
					// NLP.pwNLP.append(//NLP.printLn("found first data col at this cell.
					// colIdx="+n+" cell data is="+data);
					break;
				}
			}
		}

		List<Integer[]> listRowSpan = NLP.getRowSpans(chRows);
		chAboveRowname = "";
		boolean foundAtZero = false, foundAtOne = false;

		int key = 0;
		String colSpanStr = "", cellTxt = "";
		int colSpan = 0, row = 0, col = 0, sIdx = 0, eIdx = 0, pEidx = 0, rsRowSt = 999, rsRowEnd = -1, rsIdxSt = -1,
				rsIdxEnd = -1;
		boolean isChAboveRowname = false;
		for (; row < chRows.size(); row++) {
			TDs = chRows.get(row).getElementsByTag("td");
			sIdx = 0;
			eIdx = 0;
			pEidx = 0;

			for (col = 0; col < TDs.size(); col++) {
				td = TDs.get(col);
				colSpanStr = td.attr("colspan").replaceAll("[,\\(\\)\\$ ]", "").trim();
				cellTxt = td.text().replaceAll(" ?\\*", "").replaceAll(" ?(\\(|\\[)[\\dA-Za-z]{1}(\\]|\\))", "");
				// assumes no ch data on this row - skips this cell
				if (cellTxt.length() > 1) {
					// NLP.pwNLP.append(//NLP.printLn("this is row#=" + row
					// + " cellTxt==" + cellTxt + " col #=" + col, ""));
				}

				if (nlp.getAllIndexStartLocations(cellTxt, TableParser.TableNamePattern).size() > 0) {
					if (col != 0) {
						sIdx = pEidx + 1;
						eIdx = sIdx + colSpan;
					} else {
						sIdx = 0;
						eIdx = 0 + colSpan;
					}
					continue;
				}

				colSpan = 0;
				if (colSpanStr.length() > 0 && nlp.isNumeric(colSpanStr) && !colSpanStr.contains(".")) {
					colSpan = Integer.parseInt(colSpanStr.replaceAll("[,\\(\\)\\$ ]", "").trim()) - 1;
				}

				// logic: start col idx = pEidx+1. eIdx = sIdx+colSpan.

				if (col != 0) {
					sIdx = pEidx + 1;
					eIdx = sIdx + colSpan;
				} else {
					sIdx = 0;
					eIdx = 0 + colSpan;
				}

				if (null != listRowSpan && listRowSpan.size() > 0) {
					for (int i = 0; i < listRowSpan.size(); i++) {
						rsRowSt = listRowSpan.get(i)[0];
						rsRowEnd = listRowSpan.get(i)[1];
						rsIdxSt = listRowSpan.get(i)[2];
						rsIdxEnd = listRowSpan.get(i)[3];

						// rowspan ruleset (this is universal). sidx/eIdx will
						// iterate ahead thru each consecutive rowspan in the
						// list to the extent there are consecutive ones. so I
						// don't merge in listRowspan consecutive idx.
						if (row <= rsRowEnd && row >= rsRowSt && sIdx == rsIdxSt) {
							sIdx = rsIdxEnd + 1;
							eIdx = rsIdxEnd + 1 + colSpan;
						}
					}
				}

				// if eIdx==0 then it has to be above rowname
				// col Start Idx at 0 is first column
				isChAboveRowname = false;
				if (sIdx == 0 && nlp.getAllIndexEndLocations(cellTxt, TableParserHTML.ColumnHeadingPattern).size() > 0
						&& (!foundAtOne || eIdx == 0)) {
					chAboveRowname = chAboveRowname + " " + cellTxt;
					// NLP.pwNLP.append(//NLP.printLn("aa chAboveRowname="
					// + chAboveRowname, ""));
					foundAtZero = true;
					isChAboveRowname = true;
				}

				// col Start Idx at 1 is first column. It is not unusual for ch
				// to be in col 1 - but it is NOT a chAbove rowname if col idx
				// is above a data col. It also can't be just a year value.

				if (sIdx == 1 && sIdx < firstDataColIdx
						&& nlp.getAllIndexEndLocations(cellTxt, TableParserHTML.ColumnHeadingPattern).size() > 0
						&& cellTxt.replaceAll("[12]{1}[09]{1}[0-9]{2}", "").length() > 1 && !foundAtZero
						&& maxNumberChsOnAnyRow > numberOfDataColumns) {
					chAboveRowname = chAboveRowname + " " + cellTxt;
					// NLP.pwNLP.append(//NLP.printLn("bb chAboveRowname="
					// + chAboveRowname + " firstDataColIdx="
					// + firstDataColIdx, ""));
					foundAtOne = true;
					isChAboveRowname = true;
				}

				if (cellTxt.length() > 1 && !isChAboveRowname) {
					// NLP.pwNLP.append(//NLP.printLn("getHtmlColHdgMap\rrow=" + row
					// + " sIdx=" + sIdx + " eIdx=" + eIdx + " cellTxt="
					// + cellTxt, ""));

					String[] ary = { sIdx + "", eIdx + "", cellTxt };
					key = (row + 1) * 100 + eIdx;

					// NLP.pwNLP.append(//NLP.printLn(
					// "ary (colStartIdx,colEndIdx,celltxt)=" + "\r ary="
					// + Arrays.toString(ary), ""));

					if (((eIdx - sIdx) / TDs.size() > .7 || eIdx - sIdx > 3)
							&& (NLP.patternCoIncLtdLLC.matcher(cellTxt).find() || cellTxt.toUpperCase()
									.contains(companyName.substring(0, Math.min(companyName.length(), 6))))) {
						// NLP.pwNLP.append(//NLP.printLn(
						// "continue past tableheading. colunText="
						// + cellTxt, ""));
						continue;
					}

					else {
						mapRowColIdx.put(key, ary);
						// NLP.pwNLP.append(//NLP.printLn("put ary in mapRowColIdx="
						// + Arrays.toString(ary), ""));
					}

				}
				pEidx = eIdx;
			}
		}

		// current map has key=(row+1)*100+colEndIdx. For same rows put into 1
		// list.
		//// NLP.printMapIntStringAry("mapRowColIdx", mapRowColIdx);
		Map<Integer, List<String[]>> mapCHtmp = new TreeMap<Integer, List<String[]>>();
		// return map, key=row,List<String[]>:: [0]=col hdg
		// startIdx,[1]=col hdg endIdx,[2]=colhdg text

		// NOTE: cannot change a list that is part of a map unless you first
		// instantiate a new list first that then gets populated and put into
		// map
		int cnt = 0, k = 0, priorK = -1;
		for (Map.Entry<Integer, String[]> entry : mapRowColIdx.entrySet()) {
			List<String[]> listTmp = new ArrayList<String[]>();
			// NLP.pwNLP.append(//NLP.printLn("mapRowColIdx key="+entry.getKey());
			// NLP.pwNLP.append(//NLP.printLn("mapRowColIdx value="+
			// Arrays.toString(entry.getValue()));
			// getListOfStringAryFromMapOfString converts map key row*100+col
			// idx to just row and return list<String[]> with all same rows.
			// create resulting map with key equal to CH row and list<String[]>
			// of all CHs in row
			// NLP.pwNLP.append(//NLP.printLn("get listTmp--mapRowColIdx");
			k = entry.getKey() / 100;

			// gets entire row's CHs in correct format -- it will check if k
			// (key/100) is equal to any other key/100 in entire map. These are
			// same rows and are put in returned list.
			listTmp = NLP.getListOfStringAryFromMapOfStringWithSameConvertedKey(mapRowColIdx, k);
			// NLP.pwNLP.append(//NLP.printLn("listTmp.size="+listTmp.size());
			if (listTmp.size() > 0 && k != priorK) {
				// each list is row's CHs
				mapCHtmp.put(cnt, listTmp);
				cnt++;
			}
			priorK = k;
		}

		if (mapCHtmp.size() > 0) {
			//// NLP.printMapIntListOfStringAry("reconstituted mapCHtmp", mapCHtmp);
		}

		return mapCHtmp;
	}

	public List<String[]> mergeTwoListBasedOnIdx(List<String[]> lgList, List<String[]> smList, boolean reverse) {

		boolean merged = false;
		NLP nlp = new NLP();
		List<String[]> mergedList = new ArrayList<String[]>();
		//// NLP.printListOfStringArray("@mergeTwoListBasedOnIdx. smList", smList);
		//// NLP.printListOfStringArray("@mergeTwoListBasedOnIdx. lgList", lgList);

		int smColIdxStart = 0, smColIdxEnd, lgColIdxStart, lgColIdxEnd;
		String smCHtxt = "", lgCHtxt = "";
		int n;
		// this should be small list checking which of each largelist it joins
		// to - but I need to ensure I start with largest list.

		for (n = lgList.size() - 1; n >= 0; n--) {
			lgColIdxStart = Integer.parseInt(lgList.get(n)[0]);
			lgColIdxEnd = Integer.parseInt(lgList.get(n)[1]);
			lgCHtxt = lgList.get(n)[2];

			merged = false;
			for (int c = 0; c < smList.size(); c++) {
				// NLP.pwNLP.append(//NLP.printLn("if this is merged="+merged+" then this
				// instance of smList ary is stranded?="+Arrays.toString(smList.get(c)));

				if (merged)
					continue;
				smColIdxStart = Integer.parseInt(smList.get(c)[0]);
				smColIdxEnd = Integer.parseInt(smList.get(c)[1]);
				if (((smColIdxStart == lgColIdxStart && smColIdxEnd == lgColIdxEnd)
						|| (lgColIdxEnd <= smColIdxEnd && lgColIdxStart >= smColIdxStart)
						|| (lgColIdxEnd >= smColIdxEnd && lgColIdxStart <= smColIdxStart)) && !merged) {
					// NLP.pwNLP.append(//NLP.printLn("2 smColIdxStart="
					// + smColIdxStart + " smColIdxEnd=" + smColIdxEnd
					// + " smCHltxt=" + smList.get(c)[2], ""));
					// NLP.pwNLP.append(//NLP.printLn("3 lgColIdxEnd=" + lgColIdxEnd
					// + " lgColIdxStart" + lgColIdxStart + " merged="
					// + merged, ""));

					merged = true;
					smCHtxt = smList.get(c)[2];
					// NLP.pwNLP.append(//NLP.printLn("2 merged");
					if (!reverse) {
						String[] ary = { lgList.get(n)[0], lgList.get(n)[1], lgCHtxt + " " + smCHtxt };
						// NLP.pwNLP.append(//NLP.printLn("prior to add at 6 mergedList=");
						//// NLP.printListOfStringArray(mergedList);
						mergedList.add(ary);
						//// NLP.printListOfStringArray("6 mergedList", mergedList);
					}
					if (reverse) {
						// list=[0]col# (start),[1] col# (end),
						// [2]column heading text
						String[] ary = { lgList.get(n)[0], lgList.get(n)[1], smCHtxt + " " + lgCHtxt };
						//// NLP.printListOfStringArray("7 mergedList", mergedList);
						mergedList.add(ary);
					}
				}
			}

			//// NLP.printListOfStringArray("printListOfStringArray mergedList",
			// mergedList);
			// if small list CH wasn't merged with lg list CH I add lg list CH
			// below.
			if (!merged && (nlp.getAllIndexEndLocations(lgCHtxt, ColumnHeadingPattern).size() > 0
					|| nlp.getAllIndexEndLocations(lgCHtxt, NLP.patternInception).size() > 0) && n >= 0) {
				String[] ary = { lgList.get(n)[0], lgList.get(n)[1], lgCHtxt };
				// NLP.pwNLP.append(//NLP.printLn("prior to add at 8 mergedList=");
				//// NLP.printListOfStringArray(mergedList);
				mergedList.add(ary);
				// NLP.pwNLP.append(//NLP.printLn("8 !merged 2 list colTxt=",
				// Arrays.toString(ary)));
				//NLP.printListOfStringArray("8 mergedList", mergedList);
			}
		}

		// NLP.pwNLP.append(//NLP.printLn("final mergedList before cycle through=");
		//// NLP.printListOfStringArray(mergedList);

		// this is intended to filter out duplicates and keep longest one.
		List<String[]> mergL2 = new ArrayList<String[]>();
		int sIdx = 110, eIdx = 110, sIdxPrev = 0, eIdxPrev = 0, cnt = 0;
		String ch = "", prevCh = "";
		for (int c = 0; c < mergedList.size(); c++) {
			cnt++;
			sIdx = Integer.parseInt(mergedList.get(c)[0]);
			eIdx = Integer.parseInt(mergedList.get(c)[1]);
			ch = mergedList.get(c)[2];

			if (sIdx != sIdxPrev || eIdx != eIdxPrev) {
				String[] ary = { mergedList.get(c)[0], mergedList.get(c)[1], mergedList.get(c)[2] };
				// NLP.pwNLP.append(//NLP.printLn(
				// "ary at mergL2=" + Arrays.toString(ary), ""));
				mergL2.add(ary);
			}

			if (sIdx == sIdxPrev && eIdx == eIdxPrev && ch.length() > prevCh.length()) {
				// compare CH - and if current is longer- it was merged so
				// former is removed (first).
				String[] ary = { mergedList.get(c)[0], mergedList.get(c)[1], mergedList.get(c)[2] };

				if (mergL2.size() > 0 && mergL2.size() > cnt) {
					mergL2.remove(cnt);

					mergL2.add(ary);
					// NLP.pwNLP.append(//NLP.printLn(
					// "ary bef cnt>=mergL2.size ary="
					// + Arrays.toString(ary), ""));
				}

				cnt--;
				if (cnt >= mergL2.size()) {
					return mergL2;
				}

				// 1st remove prior item added -- see --
				// file:///c://backtest/tableparser/2003/qtr4/tables/0000950134-03-014270_5.htm
				// can't get it to work if I add the below back.
				/*
				 * if (mergL2.size() - 1 > cnt && mergL2.size() > 0) {
				 * NLP.pwNLP.append(//NLP.printLn("mergL2.size=" + mergL2.size() + " cnt=" + cnt
				 * + " printing mergL2="); //NLP.printListOfStringArray(mergL2);
				 * mergL2.remove(cnt); mergL2.add(ary);
				 * 
				 * }
				 */
			}

			prevCh = ch;
			sIdxPrev = sIdx;
			eIdxPrev = eIdx;
		}

		// NLP.pwNLP.append(//NLP.printLn("printing mergL2");
		//// NLP.printListOfStringArray(mergL2);

		return mergL2;
	}

	public void getDataRowColumnMap(Elements chRows, Elements dataRows, List<String> CHs, List<String[]> edtPerList)
			throws ParseException, SQLException, IOException {

		List<String> tmpListCh = new ArrayList<String>();
		for (int b = 0; b < CHs.size(); b++) {
			tmpListCh.add(CHs.get(b).replaceAll("'", ""));
		}
		CHs = tmpListCh;

		NLP nlp = new NLP();
		//// NLP.printListOfString("can I isolate 'NOTE' column--this is CH list",
		// CHs);

		if (coNameMatched.length() < 1 && coNameOnPriorLine.length() > 1) {
			coNameMatched = coNameOnPriorLine.replaceAll("'", "");
			// NLP.pwNLP.append(//NLP.printLn("1 coNameOnPriorLine=="
			// + coNameOnPriorLine, ""));
		}

		coNameMatched = coNameMatched.replaceAll("[^a-zA-Z;:,\\&\\$<>\\d\\(\\) ']", " ").replaceAll("'", "\\\\'")
				.replaceAll("[ \\s]{2,30}", " ");
		coNameMatched = coNameMatched.replaceAll("[^a-zA-Z;:,\\&\\$<>\\d\\(\\) ']", " ").replaceAll("'", "\\\\'")
				.replaceAll("[ \\s]{2,30}", " ").replaceAll("xx", "");

		// this works with noteColIdx - noteColIdx isolates the CH that has as a
		// CH 'NOTES' - such that the only purpose of the column is to record
		// the note or footnote reference. If the word NOTE doesn't appear in
		// any CH than I know it wasn't part of a larger worded CH and when I
		// get to the point below of pairing CHs with data cols - when I get to
		// a NOTE column I skip it (continue past it)

		boolean noteNotInCH = true;
		for (int i = 0; i < CHs.size(); i++) {
			if (CHs.get(i).toUpperCase().contains("NOTE"))
				noteNotInCH = false;
		}

		// TODO:
		// this is an overly lengthy and somewhat jumbled method -- could
		// certaily be cleand up by moving ancillary and or subroutines to their
		// own method - would make it far easier to skim through when there are
		// problems..

		/*
		 * EXPLANATION HOW THIS METHOD WORKS: primary functions are (1) SEE STEP1: to
		 * first check if number of CHs are same as number of data cols. This is tested
		 * at each row of table. If it fails - two other alternatives are attempted to
		 * rectify (is it due to decimal in its own col or 2 CHs in same cell). If those
		 * fail as well - then method stops and no table is parsed. However if at each
		 * row's numbr of data cols match number of CHs a data map is successfully
		 * created. Next leg is to go through the data map just created and pair it with
		 * the col hdg.
		 * 
		 * IMPORTANT - See STEP2 below: Pairing of CH and data col is based on which
		 * data col number - if this is the 3rd column with data then it will pair with
		 * the 3rd col hdg. In order to ensure it is in fact the 3rd col with data the
		 * program confirms by checking backward (and forward if backward fails) to see
		 * if another row had the same col idx as the 3rd data col. This allows CHs to
		 * be paired w/o regard to col idx CH in table but instead data col count paired
		 * to CH count
		 * 
		 * Anciallary functions include colHdgPatterns and related tablesentence edt
		 * merged.
		 */

		// if pattern is funky - I need to capture that at the column level -
		// the
		// text of which is contained in columnText - I can manage that in mysql
		// visa word serach columnText -- or do so in JAVA
		// Matcher matchFunkyCH;
		// for (int i = 0; i < CHs.size(); i++) {
		// NLP.pwNLP.append(//NLP.printLn("is it funky CH in columnText=" +
		// CHs.get(i));
		// matchFunkyCH = NLP.patternColHdgFunky.matcher(CHs.get(i));
		// if (matchFunkyCH.find()) {
		// tableNameShort = "CH funky";
		// NLP.pwNLP.append(//NLP.printLn("funky tablename found - so tableNameShort="
		// + tableNameShort);
		// NLP.pwNLP.append(//NLP.printLn("funky match=" + matchFunkyCH.group());
		// }
		// }

		if (excludeTable.equals("exclude"))
			tableNameShort = "exclude";

		int tableRow = 0, prevTableRow = -2;

		String tmpStr = "";
		colHdgAllLinesPatterns = NLP.getColHdgAllLinesPatterns(colHdgAllLinesPatterns, CHs);

		String edtH = null, perH = null, edtR = null, perR = null;

		TableParser tp = new TableParser();
		String table = tp.getTableNamePrefixYearQtr(fileDate);
		String tsPattern = "";
		// NLP.pwNLP.append(NLP
		// .println("getTableSentencePatterns - tableSentence="
		// + tableSentence, ""));
		if (tableSentence != null && tableSentence.length() > 1) {
			tsPattern = nlp.getTableSentencePatterns(tableSentence, "")
					.replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)", "");
		}

		tsPattern = tsPattern.replaceAll("[\\|]{2,}", "\\|").replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)",
				"");

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
		// NLP.pwNLP.append(//NLP.printLn("initial tsPattern==" + tsPattern);
		// cut from start to pC. Keep pC to end as substring to add back
		tsPattern = NLP.getMonthYearPeriodFromTSpatternFormatted(tmpTs)
				.replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)", "");
		tsPattern = tsPattern + pCnts + "|" + yCnts + "|" + mCnts;

		tsPattern = tsPattern.replaceAll("[\\|]{2,}", "\\|").replaceAll("'", "").replaceAll("[\r\n]", "");

		/*
		 * dataColMap captures as key tablerow in 100s plus col# - last integer in key
		 * is col# - e.g., 304 means table row 3 and col 4. Integer[]: [0]=# of cols in
		 * row (including blanks), [1]=loc of current data col, [2]=table row. col # =
		 * (key - table row[2]*100)
		 */

		Map<Integer, Integer[]> dataColMap = new TreeMap<Integer, Integer[]>();
		Element tr;
		Elements TDs;

		numberOfColumnHdgs = CHs.size();
		// NLP.pwNLP.append(//NLP.printLn("start dataColMap # of col hdgs="
		// + numberOfColumnHdgs, ""));
		//NLP.printListOfString("CHs", CHs);
		//
		//NLP.printListOfStringArray("B. edtPerList", edtPerList);

		if (endDatesPeriodsListFromTableSentences != null) {
			// NLP.pwNLP.append(//NLP.printLn("samePer=" + columnOneAndTwoSameYear
			// + " tsShort=" + tsShort
			// + " endDatesPeriodsListFromTableSentences.size="
			// + endDatesPeriodsListFromTableSentences.size()
			// + " columnOneAndTwoSameYear=" + columnOneAndTwoSameYear
			// + " tableNameLong=" + tableNameLong, " allColText="
			// + allColText));
			//NLP.printListOfStringArray("AA. edtPerList", edtPerList);
		}

		// NLP.printzListOfStringArray("1 endDatesPeriodsListFromTableSentences=",
		// endDatesPeriodsListFromTableSentences);
		// NLP.pwNLP.append(//NLP.printLn("tsShort=", tsShort + "\rtableSentence="
		// + tableSentence));

		if (endDatesPeriodsListFromTableSentences.size() > 0 && tsShort.length() > 0 && tableSentence.length() > 5) {

			//NLP.printLn("1 printing mergeTablesentenceEdtPerWithEdtPerIdx=", "");
			edtPerList = NLP.mergeTablesentenceEdtPerWithEdtPerIdx(edtPeriodList, endDatesPeriodsListFromTableSentences,
					tsShort, numberOfColumnHdgs, columnOneAndTwoSameYear, allColText);

			//NLP.printListOfStringArray("C. edtPerList", edtPerList);

		} else
			edtPerList = edtPeriodList;

		//NLP.printListOfStringArray("D. edtPerList", edtPerList);

		// find when # of data cols = # of col hdgs. Then assign each data col
		// idx to col hdg (key=data col idx, value=col hdg #)

		StringBuffer sb = new StringBuffer();

		// loops through dataRows - and gets initial data map. If it finds a CH
		// pattern w/n a table (a tbl w/n a tbl) it will record the lastGoodKey
		// which is the last row prior to finding a CH w/n a table that was a
		// key that had day. To get the row# (which is equal to the row# of the
		// table) divide the key by 100 (technically subtract the col # first
		// but given rounding it works) and lastGoodRow.

		int cnt = 0, lastGoodKey = 0, k = 0;
		boolean potentialCH = false, gotTableWithin = false, foundCH = false, decimalInOwnCol = false;
		int row;

		// if for loop below breaks - call it again but have it continue past
		// colIdx identified as having just decimal value.

		@SuppressWarnings("unused")
		String decStr = "";
		@SuppressWarnings("unused")
		int decIdx = -1, cnt2 = 0, nmbDataColLessThanCHs = 0, nmbDataColEqualCHs = 0;
		boolean loop2 = false, allDone = false, moreThanOneYearInCH = false;
		Map<Integer, Integer> mapDecimalInItsOwnRow = new TreeMap<Integer, Integer>();

		/*
		 * STEP1 LOOP starts here - goes through all data rows. If first loop fails b/c
		 * CH.size!= number of data cols. loop2 is set to true. If loop2=true then I
		 * check if first loop failure is due to decimal in its own col (this causes CH
		 * idx to not align with data col idx b/c there are 2 cols for one number - one
		 * for the integer and one for the decimal values) OR if there were 2 CHs in one
		 * cell in the case of 2 years in same cell - one is right aligned for data col
		 * 1 for example and the other year is left aligned for data col 2. Based on
		 * which of these conditions were identified a separate methodology is employed
		 * to rectify. See eg where boolean values mapDecimalInItsOwnRow and
		 * moreThanOneYearInCH if true various conditions are employed)
		 */

		// STEP1: start to ck if number of data cols are same as number of CHs
		int cnt3 = 0;
		boolean multiCellTried = false;

		// NLP.pwNLP
		// .append(NLP
		// .println(
		// "at STEP1 - ck decimal, ck multcell, ck 2 ch in same cell",
		// ""));
		for (row = 0; row < dataRows.size(); row++) {
			// NLP.pwNLP.append(//NLP.printLn("STEP1: dataRows.size="+dataRows.size());
			// NLP.pwNLP.append(//NLP.printLn("start -- multiCellTried==" +
			// multiCellTried
			// + " moreThanOneYearInCH=" + moreThanOneYearInCH);
			// NLP.pwNLP.append(//NLP.printLn("moreThanOneYearInCH=="+moreThanOneYearInCH+"
			// loop2="+loop2);
			// creates map used later to merge integer and decimal col

			if (!moreThanOneYearInCH && loop2 && row == 1) {
				// NLP.pwNLP.append(//NLP.printLn(
				// "checking if decimal in its own row=", ""));
				mapDecimalInItsOwnRow = NLP.mapDecimalInItsOwnColumn(dataRows, CHs);
				// mapDecimalInItsOwnRow key is the col that contains the
				// decimal that needs to be skipped below
				if (null != mapDecimalInItsOwnRow) {
					// NLP.pwNLP.append(NLP
					// .println("printing map of DecimalInItsOwnColumn (key is colIdx with just
					// decimal and val is number of times found");
					//// NLP.printMapIntInt(mapDecimalInItsOwnRow);
					decimalInOwnCol = true;
				}
				// NLP.pwNLP.append(//NLP.printLn("decimalInOwnCol="
				// + decimalInOwnCol, ""));
			}

			// this will reformat very rare instance where 2 years in same cell
			// if boolean condition was triggered where loop2 was set to true of
			// a CH. See eg: c:/temp2/2 years.htm
			if (moreThanOneYearInCH && loop2 && row == 1) {
				// TODO: move to separate method and return as reconstituted CHs
				// NLP.pwNLP.append(//NLP.printLn("moreThanOneYearInCH="+moreThanOneYearInCH+"
				// loop2="+loop2+
				// " these are the CHs=");
				//// NLP.printListOfString("CHs=", CHs);
				List<String> listCHtmp = new ArrayList<>();
				List<String[]> listTsEdtPerTmp = new ArrayList<>();
				String tsTmp = "", tsShortTmp = "", tsPatternTmp = "";
				for (int b = 0; b < CHs.size(); b++) {
					listCHtmp = NLP.parseTableSentence(CHs.get(b),
							companyName.substring(0, Math.min(companyName.length(), 7)), false, tableNameLong, false);
					tsShortTmp = listCHtmp.get(1);
					tsPatternTmp = listCHtmp.get(2);
					tableNameLong = listCHtmp.get(6).replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n", "");
					tableNameShort = tp.getTableNameShort(tableNameLong);

					// NLP.pwNLP.append(//NLP.printLn("tsTmp=" + tsTmp +
					// " \rtsPatternTmp="
					// + tsPatternTmp);
					if (tsShortTmp.equals("MMYY")) {
						String[] tsPatternSplitTmp = tsPatternTmp.split("\\|");
						tsPatternTmp = "|" + tsPatternSplitTmp[1] + "|" + tsPatternSplitTmp[3] + "|"
								+ tsPatternSplitTmp[2] + "|" + tsPatternSplitTmp[4] + "|";
						tsShortTmp = "MYMY";
						tsTmp = "was MMYY||" + tsPatternSplitTmp[1].substring(3, tsPatternSplitTmp[1].length()) + " "
								+ tsPatternSplitTmp[3].substring(3, tsPatternSplitTmp[3].length()) + " and "
								+ tsPatternSplitTmp[2].substring(3, tsPatternSplitTmp[2].length()) + " "
								+ tsPatternSplitTmp[4].substring(3, tsPatternSplitTmp[4].length());
						// NLP.pwNLP.append(NLP
						// .println("revised MMYY to MYMY - tsPatternTmp="
						// + tsPatternTmp + "9c tableSentence=" + tsTmp);
						tableSentence = tsTmp;
					}
					listTsEdtPerTmp.addAll(NLP.getTablesentenceColumnHeadings(tsTmp, tsShortTmp, tsPatternTmp));
				}
				//// NLP.printListOfStringArray(
				// "printing final CHs and per using TS method-listTsEdtPerTmp",
				// listTsEdtPerTmp);
				edtPerList = listTsEdtPerTmp;
				// now reformat so it is in same format as original CH
				List<String> tmpCH = new ArrayList<>();
				String tmpEdt = "", tmpPer = "";
				for (int d = 0; d < listTsEdtPerTmp.size(); d++) {
					tmpEdt = listTsEdtPerTmp.get(d)[0];
					tmpPer = listTsEdtPerTmp.get(d)[1];
					if (nlp.isNumeric(tmpPer) && Integer.parseInt(tmpPer) != 0) {
						tmpCH.add(tmpPer + " months " + tmpEdt);
					} else
						tmpCH.add(tmpEdt.replaceAll("'", ""));
				}
				CHs = tmpCH;
				numberOfColumnHdgs = CHs.size();
				// for CHs - use parseTableSentence and then edt/per from ts.
				// Each col idx then needs to be assigned to each consecutive
				// map data col idx if # of data col Idx equal # of CH idx.
				// NLP.getCHs(CHs);//run parseTableSentence, then edt/per for ts
				// NLP.getDataMap(dataRows);
				//// NLP.printListOfString(
				// "reconstituted CHs where 2 years in same cell -- CHs",
				// CHs);
			}

			tr = dataRows.get(row);
			// NLP.pwNLP.append(//NLP.printLn("data row++===" + tr.text());
			if (decimal.equals("1") && row < 4 && !foundCH) {
				decimal = nlp.getDecimal(tr.text());
				// NLP.pwNLP.append(//NLP.printLn("decimal=" + decimal, ""));
			}

			Pattern nmbPattern = Pattern.compile("[0-9]{1,}|-");
			// more than one number in a cell
			Pattern patternMultiNumber = Pattern.compile("\\d\\d  ?\\d\\d");
			// w/o including '-' it wasn't picking up cells with just hyphen
			// which causes assignment CH to be wrong (b/c it does't pickup
			// hyphen as data value the CH is not assigned so next col CH is and
			// that CH should have been assigned to col w/ hyphen)
			cnt = 0;
			potentialCH = false;
			int dataMarker, dataLength;
			// what happens when there's nothing in a column?

			// NLP.print("row text=" , tr.text());
			TDs = tr.getElementsByTag("TD");
			for (int a = 0; a < TDs.size(); a++) {
				// NLP.pwNLP.append(//NLP.printLn("a. mapDecimalInItsOwnRow - a="+a+" col a text
				// is="+TDs.get(a).text());

				if (!moreThanOneYearInCH && loop2 && decimalInOwnCol && null != mapDecimalInItsOwnRow
						&& null != mapDecimalInItsOwnRow.get(a)) {
					// NLP.pwNLP.append(NLP
					// .println("moreThanOneYearInCH && loop2 && decimalInOwnCol -- column found at
					// colidx="
					// + a + " colText=" + TDs.get(a).text());
					continue;
				}

				Matcher matchAlph = alphabetPattern.matcher(TDs.get(a).text());
				// NLP.pwNLP.append(//NLP.printLn("text to match to see if nmbPattern
				// matched="+TDs.get(a).text().replaceAll(" ",
				// ""));
				Matcher matchNmb = nmbPattern.matcher(TDs.get(a).text().replaceAll(" ", ""));
				Matcher matchMultiNumber = patternMultiNumber.matcher(TDs.get(a).text());
				// NLP.pwNLP.append(//NLP.printLn("1. cell text="+TDs.get(a).text());
				if (!multiCellTried && matchNmb.find() && matchMultiNumber.find()) {
					// NLP.pwNLP.append(//NLP.printLn("multi numbers found", ""));
					cnt3++;
				}

				if (cnt3 > 2 && !multiCellTried) {
					// found 2 instances where 2 numbers are in same cell.
					// method below will reconstitute data rows - see
					// explanation at getMultiRowsWithinTableHtmlRow
					// NLP.pwNLP.append(//NLP.printLn("ckg multiLinesInCell", ""));
					dataRows = NLP.getMultiLinesInTableHtmlRow(tableHtml, firstNonChLineNumberNotFound);
					// NLP.pwNLP.append(//NLP.printLn(
					// "dataRows.size=" + dataRows.size(), ""));
					row = 0;
					cnt3 = 0;
					multiCellTried = true;
					moreThanOneYearInCH = NLP.getYrCount(CHs);
					// NLP.pwNLP.append(//NLP.printLn(
					// "is there moreThanOneYearInCH="
					// + moreThanOneYearInCH, ""));
					loop2 = true;
					// NLP.pwNLP.append(//NLP.printLn("multi lines in same cell!!",
					// ""));
					continue;
				}

				dataMarker = TDs.get(a).text().replaceAll("[\\-]{1,}|(?i)N.{0,1}[MA]{1}.{0,1}", "").length();
				dataLength = TDs.get(a).text().length();
				// NLP.pwNLP.append(//NLP.printLn("matched nmb against this
				// text="+TDs.get(a).text());
				// NLP.pwNLP.append(//NLP.printLn("other="+dataMarker);

				// if last col is a CH pattern and there are at least 2 CHs
				// patterns on the row it must be a new CH pattern
				if (a > 0 && nlp.getAllIndexStartLocations(tr.text(), TableParser.ColumnHeadingPattern).size() > 1
						&& !foundCH) {
					potentialCH = true;
				}

				// NLP.pwNLP.append(//NLP.printLn("potential ch=" + potentialCH);
				if (nlp.getAllIndexStartLocations(TDs.get(a).text(), TableParser.ColumnHeadingPattern).size() > 0
						&& a == (TDs.size() - 1) && potentialCH && !foundCH) {

					// if CHs had 2 years in one cell - I need to adjust the CH
					// fetch here
					// NLP.pwNLP.append(//NLP.printLn("this is CH -- col text++=="+TDs.get(a));

					/*
					 * dataColMap records each row of entire table. So based on lastGoodKey (table
					 * row value[2] I can re-create tableHtml by skipping first group of rows parsed
					 * based on last good row # (eg after lastGoodKey). Given rowname has to be at
					 * colIdx=0 and if at least 2 data cols a has to be at least>1 (col3).
					 * Difficulty is false positives. I can require a tableName to be prior to
					 * current row - so go back
					 */

					// NLP.pwNLP.append(//NLP.printLn("it is a year=" +
					// TDs.get(a).text() +
					// "| at colIdx #=" + a);
					// NLP.pwNLP.append(//NLP.printLn("lastGoodKey=" + k);
					lastGoodKey = k;
					foundCH = true; // use to signify to NOT .put this row into
									// map

				}

				// first col can't be a data col -- hence a>0.
				// NLP.pwNLP.append(//NLP.printLn("cell text before cnt++="+TDs.get(a).text());
				// NLP.pwNLP.append(//NLP.printLn("at table insert into mysql. noteColIdxSt="
				// + noteColIdxSt + " cur colIdx=" + a + " noteNotInCh="
				// + noteNotInCH + " noteColIdxEnd=" + noteColIdxEnd);

				// if 'NOTE' is a standalone CH (noteNotInCH) then I will not
				// consider values in this column as they are just footenotes
				double tmpInt = 0;
				if (nlp.isNumeric(TDs.get(a).text())) {
					tmpInt = Double.parseDouble(TDs.get(a).text().replaceAll("[,\\(\\)\\$]", "").trim());

				}

				// if text 'NOTE' is note in CH - noteNotInCH=true and highest
				// integer found in NOTE CH is <12 - don't worry if this col has
				// a number that doesn't align - ignore and continue past
				if (a >= noteColIdxSt && a <= noteColIdxEnd && noteNotInCH && tmpInt < 12) {
					// NLP.pwNLP.append(//NLP.printLn("continue past noteCol");
					continue;
				}

				// had to add condition that if it is a numeric value to proceed
				// - for some reason if value was just a single integer it
				// wasn't picked up (eg. if it were 8)
				if (!matchAlph.find() && a > 0
						&& (((matchNmb.find() || nlp.isNumeric(TDs.get(a).text().replaceAll(" ", "")))
								|| (dataMarker == 0 && dataLength > 0)) && TDs.get(a).text().length() < 20)) {
					cnt++;
					// NLP.pwNLP.append(//NLP.printLn("A CHs.size=" + CHs.size() +
					// " cnt="
					// + cnt);
					// NLP.pwNLP.append(//NLP.printLn("A row text="+tr.text() +
					// " decimalInOwnCol="+decimalInOwnCol);

					if (cnt > CHs.size()) {
						cnt2++;

						if (cnt2 > 2) {
							if (!loop2) {
								loop2 = true;
								// NLP.pwNLP.append(//NLP.printLn("loop2="+loop2);
								row = 0;
								cnt2 = 0;
								cnt = 0;
								moreThanOneYearInCH = NLP.getYrCount(CHs);
								// NLP.pwNLP.append(//NLP.printLn("moreThanOneYearInCH=="+moreThanOneYearInCH+"
								// cnt2="+cnt2);
							}
						}

						if (cnt2 > 2 && allDone) {
							// NLP.pwNLP.append(//NLP.printLn("allDone=" + allDone
							// + " cnt2==" + cnt2, ""));
							return;
						}

						if (cnt2 > 2 && loop2 && !allDone) {
							edtPerList.clear();
							getHtmlIdxOnly(chRows);
							// <=creates listOfStrHtmlColHdgs
							CHs = listOfStrHtmlColHdgs;
							edtPerList = getEndDatesPeriods(CHs);
							numberOfColumnHdgs = CHs.size();
							// NLP.pwNLP.append(//NLP.printLn("CHs set to listOfStrHtmlColHdgs");
							// NLP.pwNLP.append(//NLP.printLn("new edtPerList=");
							//// NLP.printListOfStringArray(edtPerList);
							cnt2 = 0;
							allDone = true;
						}
					}
				}
			}
			// NLP.pwNLP.append(//NLP.printLn("cnt is =="+cnt);
			if (cnt == numberOfColumnHdgs) {
				nmbDataColEqualCHs++;
				// NLP.pwNLP.append(//NLP.printLn("numberOfColumnHdgs=" +
				// numberOfColumnHdgs
				// + " nmbDataColEqualCHs=" + nmbDataColEqualCHs
				// + " nmbDataColLessThanCHs==" + nmbDataColLessThanCHs);
			}

			if (nmbDataColLessThanCHs > 5 && (double) (nmbDataColLessThanCHs / (nmbDataColEqualCHs + .1)) > 2)
				return;
			// if more than 5 rows don't match number of data cols - return.
			if (cnt < numberOfColumnHdgs && cnt != 0) {
				nmbDataColLessThanCHs++;
				// if cnt is 0 that means for the row there was no data at all.
				// So ignore.
			}

			// NLP.pwNLP.append(//NLP.printLn("# of data cols found versus
			// numberOfColumnHdgs. data cols="+cnt+" number of CHs="+numberOfColumnHdgs);

			// assumes all cols you'd expect have data do.
			// NLP.pwNLP.append(//NLP.printLn("cnt==" + cnt + " numberOfColHdgs="
			// +
			// numberOfColumnHdgs);

			// must still add mapping if tbl w/n tbl found b/c I need to use it
			// to reconstitute tableHtml

			// this has to be set to == OTHERWISE cols data cols don't go in
			// correct ch when each col is not occupied.
			// but when column is completely blank table doesn't parse --
			// expirement to see how to fix

			if (cnt == numberOfColumnHdgs) {
				int colHdgNo = 0, totalNumberOfColumns = TDs.size();
				// NLP.pwNLP.append(//NLP.printLn("totalNumberOfColumns=" +
				// totalNumberOfColumns);
				// NLP.pwNLP.append(//NLP.printLn("row text=" + tr.text());

				for (int colIdx = 0; colIdx < totalNumberOfColumns; colIdx++) {
					if (decimalInOwnCol && null != mapDecimalInItsOwnRow && mapDecimalInItsOwnRow.size() > 0
							&& null != mapDecimalInItsOwnRow.get(colIdx)) {
						// NLP.pwNLP.append(//NLP.printLn("decimalInOwnCol="+decimalInOwnCol);
						// NLP.pwNLP.append(//NLP.printLn("decimal column found at colIdxLR="
						// + colIdx
						// + " colText=" + TDs.get(colIdx).text());
						continue;
					}

					Matcher matchAlph = alphabetPattern.matcher(TDs.get(colIdx).text());
					Matcher matchNmb = nmbPattern.matcher(TDs.get(colIdx).text());

					/*
					 * total#ofCols (blank and data), colHdgNo is col number that has data which has
					 * to be paired with corresponding CH number. Col numbers (CH and data col idx
					 * is number when counting all cols in table - including blanks etc) and key in
					 * table is row number *100 plus col idx
					 */

					if (!matchAlph.find() && matchNmb.find()) {
						// NLP.pwNLP.append(//NLP.printLn("its a number at this colIdx="+colIdx+
						// " number in this column is="
						// + TDs.get(colIdx).text());
						colHdgNo++;

						Integer[] intAry = { totalNumberOfColumns, colIdx, row };
						k = (row + 1) * 100 + colHdgNo;
						if (lastGoodKey == 0 && !foundCH) {
							// NLP.pwNLP.append(NLP
							// .println("all good Keys - insert data row into
							// table");

							dataColMap.put(k, intAry);
							// NLP.pwNLP.append(//NLP.printLn("inputted into map key="
							// + ((row + 1) * 100 + colHdgNo)
							// + "| totalNumberOfColumns="
							// + totalNumberOfColumns + "| colIdx="
							// + colIdx + "| row=" + row
							// + "| corresponding colHdgNo=" + colHdgNo
							// + "data value=" + TDs.get(colIdx).text());
						}

						// reconstitute tableHtml at location after last good
						// row which is start of where the next CH w/n the table
						// starts
						if (lastGoodKey != 0 && !gotTableWithin) {
							String[] tableHtmlSplit = tableHtml.split("</tr>");
							tableHtml = "<Table>";
							for (int n = 0; n < tableHtmlSplit.length; n++) {
								if (n > lastGoodKey / 100) {
									tableHtml = tableHtml + tableHtmlSplit[n] + "</tr>";
								}
							}
							// NLP.pwNLP.append(//NLP.printLn("tableHtml reconstituted="
							// +
							// tableHtml + "</table>");
							gotTableWithin = true;
							// if gotTableWithin - I know we ran already - so
							// next time through the map it won't rerun. I also
							// use it to call at very end of this methd
							// parseTable(tableHtml);
						}
					}
				}
			}
		}

		// NLP.pwNLP.append(//NLP.printLn(
		// "AA dataColMap.size()===" + dataColMap.size(), ""));
		/*
		 * data col map is complete - map has row and col# (key=100*row#+colNo), and
		 * intAry=totalNumberOfColumns, colIdx, row if more data cols then CHs above is
		 * returned to caller. If there were never as many data cols as CHs map is
		 * empty.
		 */
		if (null == dataColMap || dataColMap.size() < 1) {
			// NLP.pwNLP.append(//NLP.printLn("B this table was not parsed b/c initial
			// mapping failed - but closed here");
			return;
		}

		String rowname, rowText;
		int rowCnt = 0;

		colHdgAllLinesPatterns = colHdgAllLinesPatterns + allColText.substring((allColText.indexOf("pCnt")) - 1);
		colHdgAllLinesPatterns = colHdgAllLinesPatterns.replaceAll("[\\|]{2,}", "\\|").replaceAll("[\r\n]", "");

		// NLP.pwNLP.append(//NLP.printLn("2 tspattern is =" + tsPattern);

		tsPattern = tsPattern.replaceAll("[\\|]{2,}", "\\|").replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)",
				"");
		if (null != tableSentence && tableSentence.length() > 1) {
			tsPattern = tsPattern + NLP.getDistinctPeriodMonthYearCount(tableSentence);
		}

		// NLP.pwNLP.append(//NLP.printLn("3 tspattern is =" + tsPattern);

		tsPattern = tsPattern + "||" + tableSentence;
		// tsPattern = tsPattern.replaceAll("[^a-zA-Z]", " ");
		// NLP.pwNLP.append(//NLP.printLn("tsPattern===="+tsPattern.replaceAll("\\>\\<",
		// ""));

		// NLP.pwNLP.append(//NLP.printLn("colHdgAllLinesPatterns="+colHdgAllLinesPatterns);
		// NLP.pwNLP.append(//NLP.printLn("tableSentence="+tableSentence+"
		// tsPattern
		// is="+tsPattern);
		String[] chAllLinesPatternSplit = colHdgAllLinesPatterns.split("\\|");
		String tmpString1 = "", tmpString2 = "", tmpString3 = "", tmpString4 = "", tmpString5 = "";
		if (chAllLinesPatternSplit.length > 2) {
			for (int n = 0; n < chAllLinesPatternSplit.length; n++) {
				tmpString1 = chAllLinesPatternSplit[n];
				// NLP.pwNLP.append(//NLP.printLn("chAllLinesPatternSplit[n]="+tmpString1);
				if (tmpString1.contains("M:")) {
					tmpString2 = tp.getCorrectMonthName(chAllLinesPatternSplit[n].replaceAll(".*?M:", "").trim());
					tmpString3 = tmpString1.substring(0, 5);
					// NLP.pwNLP.append(//NLP.printLn("tmpString1=" + tmpString1
					// + " tmpString1.split( ).length="
					// + tmpString1.split(" ").length);
					tmpString5 = "";
					if (tmpString1.split(" ").length > 1)
						tmpString5 = tmpString1.split(" ")[1];
					// NLP.pwNLP.append(//NLP.printLn("got month and
					// revised="+tmpString3+tmpString2);
					tmpString1 = tmpString3 + tmpString2 + " " + tmpString5;
				}
				tmpString4 = tmpString4 + "|" + tmpString1;
			}
		}
		colHdgAllLinesPatterns = tmpString4.replaceAll("[\\|]{2,}", "\\|").replaceAll("[\r\n]", "");
		// NLP.pwNLP.append(//NLP.printLn("colHdgAllLinesPatterns revised to
		// include month
		// long name="+colHdgAllLinesPatterns);

		int mnth = Integer.parseInt(fileDate.replaceAll("-", "").substring(4, 6));
		int qtr = ((mnth - 1) / 3) + 1;

		String path = "c:/backtest/tableParser/" + fileDate.substring(0, 4) + "/QTR" + qtr + "/tables/";

		FileSystemUtils.createFoldersIfReqd(path);

		/*
		 * dataColMap captures as key tablerow in 100s plus col# - last integer in key
		 * is col# - e.g., 304 means table row 3 and col 4. Integer[]: [0]=total number
		 * of cols in row (including blanks), [1]=loc of current data col, [2]=table
		 * row. col # = (key - table row[2]*100)
		 */

		// NLP.pwNLP.append(//NLP.printLn("printing dataColMap");
		//// NLP.printMapIntIntAry(dataColMap);
		// totalNumberOfColumns the table has (which is almost alwaysmore than
		// the # of data cols), colIdx, row

		int rowToLoopThrough;
		// if ch w/ tbl - stops at ch in tbl.
		if (lastGoodKey != 0)
			rowToLoopThrough = lastGoodKey / 100;
		else
			rowToLoopThrough = dataRows.size();

		// NLP.pwNLP.append(//NLP.printLn("rowToLoopThrough=" + rowToLoopThrough
		// + " dataRows.size=" + dataRows.size() + " lastGoodKey="
		// + lastGoodKey, ""));

		@SuppressWarnings("unused")
		int prevI = 0, prevIRnh = 0, returnCnt = 0, prevIVal = 0, prevRowCntRnh = 0, prevRowCntVal = 0, prevRowCnt = 0;

		if ((tableNameInsideTable || tableNameOutsideTable)
				// && !tableNameShort.equals("funky")
				&& tableNameShort.equals("exclude")) {
			tableNameShort = tp.getTableNameShort(tableNameLong);
		}

		// NLP.pwNLP.append(//NLP.printLn("1 tableNameLong=" + tableNameLong
		// + " tableNameShort=" + tableNameShort, ""));

		// STEP2 -- THIS IS WHERE DATA COLUMNS ARE PAIRED WITH COL HDGS!
		// if multi-cell - can't use dataRows - but use data map.

		if (dataRows.size() < 6)
			return;
		List<String[]> listTocEdtPerTotColsTblnm = new ArrayList<String[]>();

		// System.xut.println("getTableOfContents=" + getTableOfContents);

		if (parseEntireHtml && getTableOfContents) {
			double startTime = System.currentTimeMillis();

			listTocEdtPerTotColsTblnm = NLP.getTableOfContents(nlp.stripHtmlTags(originalText), "1");
			//// NLP.printListOfStringArray("listTocEdtPerTotColsTblnm=",
			// listTocEdtPerTotColsTblnm);

			// return list with edt,p,tsShort,tc,tn - but I will have issue in
			// terms of accuracy however
			double endTime = System.currentTimeMillis();
			double duration = (endTime - startTime) / 1000;
			System.out.println("duration for stripHtmlTags and getTableOfContents=" + duration);
			getTableOfContents = false;
		}

		// no further changes are made to edtPerList after this point (CHs
		// list). If sole month or period is available - below will pair it to
		// CHs
		boolean solePer = true, soleMo = true;
		for (int b = 0; b < CHs.size(); b++) {
			if (nlp.getAllMatchedGroups(CHs.get(b).replaceAll("'", ""), NLP.patternInception).size() > 0) {
				solePer = false;
				soleMo = false;
			}
		}

		String[] soleMonthSolePeriod = NLP.getSoleMonthSolePeriod(allColText, tsPattern);
		String soleMonth = soleMonthSolePeriod[0];
		String solePeriod = soleMonthSolePeriod[1];

		// NLP.pwNLP.append(//NLP.printLn("ary - soleMonthSolePeriod=",
		// Arrays.toString(soleMonthSolePeriod)));

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

		String tmpEdt = "", tmpPer = "";
		List<String[]> tmpEdtPerList = new ArrayList<>();
		for (int e = 0; e < edtPerList.size(); e++) {

			tmpEdt = edtPerList.get(e)[0];
			tmpPer = edtPerList.get(e)[1];

			if ((tmpEdt.replaceAll("[12]{1}[09]{1}[0-9]{2}-?-?", "").length() == 0
					|| tmpEdt.replaceAll("[12]{1}[09]{1}[0-9]{2}-(0[0-9]{1}|1[0-2]{1})-?", "").length() == 0)
					&& soleMonth.length() > 3 && tmpEdt.length() > 3) {
				tmpEdt = tmpEdt.substring(0, 4) + soleMonth;
				// NLP.pwNLP.append(//NLP.printLn(
				// "soleMonth added -- edt2=", tmpEdt));
			}
			if (tmpPer.trim().replaceAll("[0 ]", "").length() == 0) {
				tmpPer = solePeriod;
			}
			String[] ary = { tmpEdt, tmpPer };
			tmpEdtPerList.add(ary);
		}

		edtPerList = tmpEdtPerList;

		// TODO: add if statement as to when this is run xxxxxx
		if (parseEntireHtml) {

			edtPerList = NLP.mergeWithTableOfContentsEndDatePeriods(edtPerList, listTocEdtPerTotColsTblnm,
					tableNameShort, tableNameLong, columnOneAndTwoSameYear, allColText);
		}

		for (int i = 0; i < rowToLoopThrough; i++) {
			// i is tableRow - but when I merge rows- 'i' will not be same
			// as table row - so it is adjusted by -1.
			if (dataRows.size() == i)
				break;

			tr = dataRows.get(i);
			TDs = tr.getElementsByTag("td");

			Pattern nmbPattern = Pattern.compile("[\\d]|-");
			// see long note comment at other instance where nmbPattern =
			// [\\d]|-

			int totalNumberOfColumns = TDs.size(), colHdgNumber = -1;
			String dataValue;
			rowText = tr.text();
			// NLP.pwNLP.append(//NLP.printLn("rowText()====" + rowText +
			// " TDs.size=" +
			// TDs.size());
			if (TDs.size() < 1)
				continue;
			rowname = TDs.get(0).text().replaceAll("[\r\n]{1,}", "").replaceAll("[ ]{2,}", " ").replaceAll("\\&#151;",
					"-");
			// NLP.pwNLP.append(//NLP.printLn("1 rowname after repl hard returns="+rowname);
			String rownameHeader = "";

			if (rowText.replaceAll("(?i)([\r\t\\$ \\s\\&nbsp;])", "").length() < 1) {
				// skip blank rows
				continue;
			}

			// NLP.pwNLP.append(//NLP.printLn("rowtext="+rowText);
			for (int colIdx = 0; colIdx < TDs.size(); colIdx++) {
				if (decimalInOwnCol && null != mapDecimalInItsOwnRow.get(colIdx)) {
					// NLP.pwNLP
					// .append(//NLP.printLn(
					// "decimal column found at colidx=" + colIdx
					// + " colText="
					// + TDs.get(colIdx).text(), ""));
					decIdx = colIdx;
					decStr = TDs.get(colIdx).text();
					continue;
				}

				dataValue = TDs.get(colIdx).text();
				// NLP.pwNLP.append(//NLP.printLn("dataValue=" + dataValue);

				Matcher matchAlph = alphabetPattern.matcher(dataValue);
				Matcher matchNmb = nmbPattern.matcher(dataValue);

				int ch = 0;

				if (tableSentence != null && tableSentence.length() > 1)
					tableSentence = tableSentence.replaceAll("\'", "");

				// NLP.pwNLP.append(//NLP.printLn("B rowname="+rowname);
				// NLP.pwNLP.append(//NLP.printLn("B rowText="+rowText);

				if (matchAlph.find() && colIdx < 6) {
					rowname = TDs.get(colIdx).text().replaceAll("â€™", "\\'").replaceAll("'", "\\'")
							.replaceAll("[\r\n]{1,}", "").replaceAll("[ ]{2,}", " ");
					// NLP.pwNLP.append(//NLP.printLn("rowText before replace dash=="+rowText);
					String rowText2 = rowText.replaceAll("[\\$\\-\\—]", " ").replaceAll("[ ]{1,}$", "");
					// NLP.pwNLP.append(//NLP.printLn("rowText2 after replaced dash="+rowText2);
					// NLP.pwNLP.append(//NLP.printLn("rowname="+rowname);
					// NLP.pwNLP.append(//NLP.printLn("TDs.get(colIdx).text()="+TDs.get(colIdx).text()+"\n
					// rowText="+rowText);

					if (rowname.trim().length() == rowText2.trim().length()
							|| TDs.get(colIdx).text().length() == rowText.length()) {
						rownameHeader = rowname.replaceAll("\\&#151;", "-");
						rowCnt++;

						// NLP.pwNLP.append(//NLP.printLn("i - prevI============="+(i-prevI)+
						// " i="+i+" prevI="+prevI);
						// NLP.pwNLP.append(//NLP.printLn("rowCnt - prevRowCnt==="+(rowCnt-prevRowCnt)+
						// " rowCnt=="+rowCnt+" prevRowCnt="+prevRowCnt);
						// NLP.pwNLP.append(//NLP.printLn("i - prevIRnh=========="+(i-prevIRnh)+
						// " i="+i+" prevIRnh="+prevIVal);
						// NLP.pwNLP.append(//NLP.printLn("rowCnt-prevRowCntRnh=="+(rowCnt-prevRowCntRnh)+
						// " rowCnt=="+rowCnt+" prevRowCntRnh="+prevRowCntRnh);

						sb.append("('" + acc + "','" + fileDate + "','" + cik + "','" + tableNameShort + "','" + rowCnt
								+ "','0','" + i + "','" + tableCount + "','" + rownameHeader
								+ "','xxy',\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,'',''),\r");
						// acc,fd,cik,tblNmSh,row,col,tblrow,tNo,rowname,value,ttl,stt,net,sub,p1,edt1,p2,edt2,totcol,decc,tsShort,colTxt
						// ,ended,yr,mo,allcolTxt,coMatch,companyNameParsed,companyName,htmlTxt,form,tsLong,ts,fye

						// NLP.pwNLP.append(//NLP.printLn("('" + acc + "','" +
						// fileDate +
						// "','" + cik + "','" + tableNameShort + "','" + rowCnt
						// + "','0','" + i + "','" + tableCount + "','" +
						// rownameHeader
						// +
						// "','xxy',\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,\\N,'',''),\r");

						// acc,filedate,cik,tbl_nm,row,tb_no,rowname,value,col,per1,edt1,per2,edt2,deci,columnText,totCol
						// ,coNmMatch,CompanyNameParsed,CompanyName,htmlOrtxt,formtype,fye

						// because no data cols - go to next line after
						// inputting rownameheader

						prevI = i;
						prevIRnh = i;
						prevRowCntRnh = rowCnt;
						prevRowCnt = rowCnt;
						// NLP.pwNLP.append(//NLP.printLn("prevTableRow=="+prevTableRow);
						continue;
					}
				}

				// below cks first 5 rows prior to current to see if a prior row
				// was recorded in the map and if so if it has the same total
				// number of columns as the current row. If so - get colHdgNo
				// from map based on datacolIdx of current data col. key value
				// in map is equal to= (row+1)*100+colHdgNo. b/c first row=0
				// have to add 1. To call key you employ formula (b+1) * 100 +
				// ch+1 -- where b is row# and ch is col hdg # (col hdg starts
				// at zero but recorded started at 1). If not found in 1st 5
				// rows-I then check all rows after current and if that fails I
				// see if there were any earlier than the five prior to the
				// current.

				// for each data row I loop through each of the 4 col in col hdg
				// until there's a match and if no match go to next data row.

				else if (!matchAlph.find() && matchNmb.find()) {

					/*
					 * if(dataValue.length()==0 || dataValue.replaceAll("(?i)(n.{0,1}a|-_)",
					 * "").length()<1){ dataValue="0"; }
					 */
					// NLP.pwNLP.append(//NLP.printLn("checking dataValue=" +
					// dataValue +
					// " which is at colIdx=" + colIdx);
					// at current data row i search up to 5 prior row for if
					// condition
					boolean found = false;
					for (int b = i; b > Math.max(i - 5, 0); b--) {
						// dataColMap(k,v) k=row*100+colHdgNum,
						// v={totalNumberOfColumns,colIdx,row}
						int key;
						for (ch = 0; ch < CHs.size(); ch++) {
							key = (b + 1) * 100 + ch + 1;
							// use b+1 b/c initial row was set at (1+0)*100 -
							// see where ary is populated.
							// NLP.pwNLP.append(//NLP.printLn(
							// "going in reverse checking key=" + key +
							// " tot # of cols=" + totalNumberOfColumns);
							if (null != dataColMap.get(key)
									&& (dataColMap.get(key)[0] == totalNumberOfColumns || i > 12)
									&& dataColMap.get(key)[1] == colIdx) {
								tableRow = dataColMap.get(key)[2];
								tableRow++;
								// NLP.pwNLP.append(//NLP.printLn("dataColMap tableRow=="+tableRow);
								colHdgNumber = (key - (dataColMap.get(key)[2] + 1) * 100) - 1;
								// NLP.pwNLP.append(//NLP.printLn("1 colHdgNumber="+colHdgNumber);
								found = true;

								// k=(row+1)*100,v= totalNumberOfColumns,
								// colIdx, row.
								// NLP.pwNLP.append(NLP
								// .println("found going in reverse colHdgNumber="
								// + colHdgNumber
								// + "| rowname="
								// + rowname
								// + "| dataValue="
								// + dataValue
								// + "| key="
								// + key
								// + " | row="
								// + tableRow
								// + "|data colIdx=" + colIdx+
								// " CHs.get(colHdgNumber)="+CHs.get(colHdgNumber));
							}
							if (found)
								break;
						}
						if (found)
							break;
					}

					if (colHdgNumber == -1) {
						// NLP.pwNLP.append(//NLP.printLn("2 colHdgNumber="+colHdgNumber);
						for (int c = i; c < rowToLoopThrough; c++) {
							int key;
							for (ch = 0; ch < CHs.size(); ch++) {
								key = (c + 1) * 100 + ch + 1;
								// use b+1 b/c initial row was set at (1+0)*100
								// - see where ary is populated.
								// NLP.pwNLP.append(//NLP.printLn(
								// "going forwwrd checking key=" + key +
								// " tot # of cols=" + totalNumberOfColumns);
								if (null != dataColMap.get(key)
										&& (dataColMap.get(key)[0] == totalNumberOfColumns || i > 12)
										&& dataColMap.get(key)[1] == colIdx) {
									colHdgNumber = (key - (dataColMap.get(key)[2] + 1) * 100) - 1;
									// NLP.pwNLP.append(NLP
									// .println("found going in forward colHdgNumber="
									// + colHdgNumber
									// + " rowname="
									// + rowname
									// + " dataValue="
									// + dataValue
									// + " CHs.get(colHdgNumber)="
									// + CHs.get(colHdgNumber));
									found = true;
								}
								if (found)
									break;
							}
							if (found)
								break;
						}

						if (colHdgNumber == -1) {
							// NLP.pwNLP.append(//NLP.printLn("3 colHdgNumber="+colHdgNumber);
							// NLP.pwNLP.append(//NLP.printLn("still not found - checking earliest rows
							// dataValue="
							// + dataValue);
							for (int c = 0; c < i; c++) {
								int key;
								for (ch = 0; ch < CHs.size(); ch++) {
									key = (c + 1) * 100 + ch + 1;
									// use b+1 b/c initial row was set at
									// (1+0)*100
									// - see where ary is populated.
									// NLP.pwNLP.append(//NLP.printLn("going forwwrd checking key="
									// + key + " tot # of cols="
									// +
									// totalNumberOfColumns+" colIdx="+colIdx);
									if (null != dataColMap.get(key)
											// remove requirement for
											// totalNumberOfColumns to match b/c
											// some table will tail at lower end
											// of table.
											&& (dataColMap.get(key)[0] == totalNumberOfColumns || i > 12)
											&& dataColMap.get(key)[1] == colIdx) {
										colHdgNumber = (key - (dataColMap.get(key)[2] + 1) * 100) - 1;
										// NLP.pwNLP.append(//NLP.printLn("found going forward colHdgNumber="
										// + colHdgNumber
										// + " rowname=" + rowname +
										// " dataValue=" + dataValue +
										// " CHs.get(colHdgNumber)="+CHs.get(colHdgNumber));
										found = true;
									}
									if (found)
										break;
								}
								if (found)
									break;
							}

						}

					}

					// if decimal is in its own column - it would have to be in
					// the immediately succeeding column

					/*
					 * NLP.pwNLP.append(//NLP.printLn("colIdx="+colIdx+" data cell = " +
					 * TDs.get(colIdx).text() + " decimal cell=" + TDs.get(colIdx + 1).text() +
					 * " decIdx=" + (colIdx + 1) + " decimalInOwnCol=" + decimalInOwnCol +
					 * " mapDecimalInItsOwnRow.get(colIdx+1)=" + mapDecimalInItsOwnRow.get(colIdx +
					 * 1) + " dataValue=" + dataValue+ " TDs.size="+TDs.size());
					 */

					if (decimalInOwnCol && null != mapDecimalInItsOwnRow.get(colIdx + 1) && TDs.size() > (1 + colIdx)) {
						// NLP.pwNLP.append(//NLP.printLn(
						// "decimalInOwnCol -- merging adjacent columns - integer dataValue="
						// + dataValue + " decimal column="
						// + TDs.get(colIdx + 1).text(), ""));
						dataValue = nlp.getDataColumnValue(dataValue + TDs.get(colIdx + 1).text());
						/*
						 * dataValue = dataValue.substring(0, dataValue.indexOf(".")) + TDs.get(colIdx +
						 * 1).text() .replaceAll("\\)", "");
						 *//*
							 * NLP.pwNLP.append(NLP .println(
							 * "merged datacell and decimal cell -- indexOf(.)=" + dataValue.substring(0,
							 * dataValue.indexOf("."))); NLP.pwNLP.append(NLP
							 * .println("TDs.get(colIdx + 1).text().replaceAll" + TDs.get(colIdx + 1).text()
							 * .replaceAll("\\)", ""));
							 */

						// NLP.pwNLP.append(//NLP.printLn("dataValue=" + dataValue,
						// ""));

					} else {
						dataValue = nlp.getDataColumnValue(dataValue);
					}

					// NLP.pwNLP.append(//NLP.printLn("nlp.getDataColumnValue dataValue="
					// +
					// dataValue+ " decStr="+decStr);
					rowCnt++;

					// NLP.pwNLP.append(//NLP.printLn("final data rowname=" +
					// rowname +
					// " dataValue=" + dataValue + " colHdgNumber="
					// + colHdgNumber + " CHs.size()=" + CHs.size()+
					// " number of data cols="+numberOfColumnHdgs);

					// add condition-if tablename found-go up to next 8 rows and
					// see if 2 or more year patterns found in non-rowname col
					// and if furthest right year aligns w/n 1 col of furthest
					// right data col. If this condition is met - load into
					// mysql up to row prior to tblnm that also has data value

					// 1st if condition - if # of Col is < cur col
					if ((CHs.size() < colHdgNumber || colHdgNumber < 0)) {
						// NLP.pwNLP.append(//NLP.printLn("C. this table was not parsed if returnCnt>1
						// =="+returnCnt+" - colHdgNumber="
						// + colHdgNumber
						// + " lastGoodKey" + lastGoodKey
						// +" CHs.size="+CHs.size());
						// NLP.pwNLP.append(//NLP.printLn(" printing dataColMap=");
						//// NLP.printMapIntIntAry(dataColMap);
						// if colHdgNumber<0 - its b/c there's no data in that
						// col
						if (colHdgNumber >= 0)
							returnCnt++;

						// if (tableRow > 9) {
						// }

						// if I can't pair twice for entire table then return -
						// and table isn't parsed.
						if (returnCnt < 2) {
							continue;
						} else
							return;
					}

					listCHEndedYrandMo.clear();
					String ended = null, yr = null, mo = null, end = null, year = null, month = null;
					for (int b = 0; b < CHs.size(); b++) {
						tmpStr = nlp.getDateFromSlashMoDyYear(CHs.get(b));
						// NLP.pwNLP.append(//NLP.printLn("CHs.get(b)" + tmpStr);
						// ended=0, yr=1,mo=2
						List<String> listEnded = nlp.getAllMatchedGroups(tmpStr, TableParser.EndedHeadingPattern);
						List<String> listMonth = nlp.getAllMatchedGroups(tmpStr, TableParser.MonthPatternSimple);
						List<String> listYr = nlp.getAllMatchedGroups(tmpStr, TableParser.YearOrMoDayYrPattern);
						int cntY = listYr.size(), cntM = listMonth.size(), cntE = listEnded.size();
						if (cntE > 0)
							ended = listEnded.get(0);
						if (cntM > 0)
							mo = listMonth.get(0);
						if (cntY > 0)
							yr = listYr.get(0);

						String[] ary = { ended, yr, mo };
						// NLP.pwNLP.append(//NLP.printLn("CHs.get(b)
						// ended,yr,mo"+Arrays.toString(ary));
						listCHEndedYrandMo.add(ary);
						ended = null;
						mo = null;
						yr = null;
					}

					// NLP.pwNLP.append(//NLP.printLn("PRINTING listCHEndedYrandMo=");
					//// NLP.printListOfStringArray(listCHEndedYrandMo);

					if (edtPerList.size() > colHdgNumber && edtPerList.size() > 0) {
						end = null;
						year = null;
						month = null;
						edtH = edtPerList.get(colHdgNumber)[0];
						if (edtH.replaceAll("[12]{1}[09]{1}[0-9]{2}-?-?", "").length() < 1
								&& allColTextOneMonth.length() > 2) {
							// NLP.pwNLP.append(//NLP.printLn(
							// "edtH amended with allColTextOneMonth="
							// + edtH.replaceAll("[-]{1,}", "")
							// + allColTextOneMonth, ""));
							// NLP.pwNLP.append(//NLP.printLn("AllColTextOneMonth="
							// + allColTextOneMonth, ""));
							edtH = edtH.replaceAll("[-]{1,}", "") + allColTextOneMonth;
							// NLP.pwNLP.append(//NLP.printLn("edtH fixed=" + edtH,
							// ""));
						}

						perH = edtPerList.get(colHdgNumber)[1];
						// NLP.pwNLP.append(//NLP.printLn("col#="+colHdgNumber+" A edtH="+edtH);
						end = listCHEndedYrandMo.get(colHdgNumber)[0];
						year = listCHEndedYrandMo.get(colHdgNumber)[1];
						month = listCHEndedYrandMo.get(colHdgNumber)[2];
						// NLP.pwNLP.append(//NLP.printLn("col#="+colHdgNumber+" A perH="+perH);
					}

					// NLP.pwNLP.append(//NLP.printLn("edtPeriodListRawText.size()==="
					// + edtPeriodListRawText.size() + " colHdgNumber="
					// + colHdgNumber);
					if (edtPeriodListRawText.size() > colHdgNumber && edtPeriodListRawText.size() > 0) {
						edtR = edtPeriodListRawText.get(colHdgNumber)[0];
						perR = edtPeriodListRawText.get(colHdgNumber)[1];
					}

					// if cur col # is same as last col (colHdgNumber + 1 =
					// CHs.size()) and edtH!=edtR or edtH<10 then get from
					// lastColEdtPer (or if col#=0 get from firstColEdtPer).
					// Same for perH!=perR or perH=0 and table not 'bs' - then
					// get from same lists.

					// unclear how accurate first/last ch technique is - if it
					// is 100% accurate - I don't need a confirmatory value in
					// edtR/perR. Currently I'm not using one.

					// I fail to get last edt/p and first edt/p
					// NLP.pwNLP.append(//NLP.printLn("1. perH="+perH+" edtH="+edtH);
					// NLP.pwNLP.append(//NLP.printLn("2. perR="+perR+" edtR="+edtR);
					boolean badEdtH = true, badPerH = true;
					boolean goodEdtR = false, goodPerR = false;

					if (edtH == null || nlp.getAllIndexEndLocations(edtH, Pattern.compile(
							"[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})"))
							.size() > 0) {
						badEdtH = false;
					}
					// NLP.pwNLP.append(//NLP.printLn("is perH null="+perH);
					if (perH == null || nlp.getAllIndexEndLocations(perH, Pattern.compile("[\\d]{1,2}")).size() > 0
							&& !perH.equals("0")) {
						badPerH = false;
					}

					if (nlp.getAllIndexEndLocations(edtR, Pattern.compile(
							"[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})")) != null
							&& nlp.getAllIndexEndLocations(edtR, Pattern.compile(
									"[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})"))
									.size() > 0) {
						goodEdtR = true;
					}

					if (nlp.getAllIndexEndLocations(perR, Pattern.compile("[\\d]{1,2}")) != null
							&& nlp.getAllIndexEndLocations(perR, Pattern.compile("[\\d]{1,2}")).size() > 0
							&& !perR.equals("0")) {
						goodPerR = true;
					}

					// if good raw edt and it doesn't match edtHtml then use
					// good raw. if goodPer and it doesn't mach perH these
					// goodPer

					// NLP.pwNLP.append(NLP
					// .println("goodEdtR=" + goodEdtR + " edtR=" + edtR+
					// " badEdtH="+badEdtH);
					if (goodEdtR && badEdtH
					/*
					 * && edtH.substring(0, 3).equals( edtR.substring(0, 3))
					 */) {
						if (colHdgNumber == 0) {
							edtH = listFirstColEdtPer.get(0);
						}
						if (colHdgNumber + 1 == CHs.size()) {
							edtH = listLastColEdtPer.get(0);
							// NLP.pwNLP.append(//NLP.printLn("edtH=edtR?="+edtH);
						}
					}

					// NLP.pwNLP.append(NLP
					// .println("goodPerR=" + goodPerR + " perR=" + perR);
					if (goodPerR && badPerH
					/*
					 * && !perH.equals( perR)
					 */) {
						if (colHdgNumber == 0) {
							perH = listFirstColEdtPer.get(1);
						}
						if (colHdgNumber + 1 == CHs.size()) {
							perH = listLastColEdtPer.get(1);
						}
					}

					// NLP.pwNLP.append(//NLP.printLn("3. perH="+perH+" edtH="+edtH);
					// NLP.pwNLP.append(//NLP.printLn("4. perR="+perR+" edtR="+edtR);

					// need to add tablesentence in one field and CH matches in
					// another.
					List<String> listCHs = nlp.getAllMatchedGroups(tableSentence, ColumnHeadingPattern);
					String tableSent = "";
					for (int m = 0; m < listCHs.size(); m++) {
						tableSent = tableSent + "|" + listCHs.get(m);
					}
					// NLP.pwNLP.append(//NLP.printLn("33 tableSentence="+tableSent);
					// TODO: if tblSent.len>150 - not tablesentence
					// capture tableSent patterns that match followed by ts
					// capture count of TS of each pattern.

					// CHs.get(colHdgNumber) pulls the column hdg text (nothing
					// from tablesentence).

					// AccNo, fileDate, cik, tn, row, col, tRow, tNo, rowName,
					// value, ttl, stt, net, sub, p1, edt1, p2, edt2, tc, `dec`,
					// tsShort, ColumnText, ColumnPattern, allColText, ended,
					// yr, mo, coMatch, CompanyNameParsed, CompanyName, htmlTxt,
					// form, TSlong, TS, fye

					// NLP.pwNLP.append(//NLP.printLn("i - prevI============="+(i-prevI)+
					// " i="+i+" prevI="+prevI);
					// NLP.pwNLP.append(//NLP.printLn("rowCnt - prevRowCnt==="+(rowCnt-prevRowCnt)+
					// " rowCnt=="+rowCnt+" prevRowCnt="+prevRowCnt);
					// NLP.pwNLP.append(//NLP.printLn("i - prevIVal=========="+(i-prevIVal)+
					// " i="+i+" prevIVal="+prevIVal);
					// NLP.pwNLP.append(//NLP.printLn("rowCnt-prevRowCntVal=="+(rowCnt-prevRowCntVal)+
					// " rowCnt=="+rowCnt+" prevRowCntVal="+prevRowCntVal);
					// NLP.pwNLP.append(//NLP.printLn("tsPattern now?="+tsPattern);
					Matcher matchFunkyCH;
					matchFunkyCH = NLP.patternColHdgFunky.matcher(CHs.get(colHdgNumber));
					if (matchFunkyCH.find()) {
						edtH = "";
						edtR = "";
						perH = "";
						perR = "";
					}

					sb.append("('" + acc + "','" + fileDate + "','" + cik + "','" + tableNameShort + "','" + rowCnt
							+ "','" + (colHdgNumber + 1) + "','" + i + "','" + tableCount + "','" + rowname + "','"
							+ dataValue + "','" + perR + "','" + edtR + "','" + perH + "','" + edtH + "','" + CHs.size()
							+ "','" + tableNameLong + "','" + coMatch + "','" + coNameMatched + "','" + decimal + "','"
							+ tsShort + "','" + CHs.get(colHdgNumber).trim() + "','"
							+ colHdgAllLinesPatterns.replaceAll("(?i)\\(|\\)|new |\'", "") + "','"
							+ allColText.replaceAll("(?i)\\(|\\)|new |\'", "") + "','" + end + "','" + year + "','"
							+ month + "','" + "html" + "','" + formType + "','"
							+ tsPattern.replaceAll("(?i)\\(|\\)|new |\'", "") + "'),\n");

					prevI = i;
					prevIVal = i;
					prevRowCnt = rowCnt;
					prevRowCntVal = rowCnt;

					// NLP.pwNLP.append(//NLP.printLn("('" + acc + "','" +
					// fileDate + "','"
					// + cik + "','" + tableNameShort + "','"
					// + rowCnt + "','" + tableCount + "','" + rowname + "','" +
					// dataValue + "','"
					// + (colHdgNumber + 1) + "','" + perR + "','" + edtR +
					// "','" + perH + "','" + edtH + "','"
					// + decimal + "','" + end + "','" + year + "','" + month +
					// "','" + CHs.get(colHdgNumber)
					// + colHdgAllLinesPatterns + "','" + allColText + "','" +
					// CHs.size() + "','" + -1
					// + "','" + coMatch + "','" + coNameParsed.replaceAll("'",
					// "") + "','"
					// + companyName.replaceAll("'", "") + "','" + "html" +
					// "','" + formType + "','"
					// + tsPattern + "','" + tableSentence + "','" + fye +
					// "'),\n");

					// acc,filedate,cik,tbl_nm,row,tb_no,rowname,value,col,per1,edt1,per2,edt2,deci,columnText,totCol,coNmMatch,
					// CompanyNameParsed,CompanyName,htmlOrtxt,formtype,fye
					dataValue = "";
					colHdgNumber = -1;
				}
			}
		}

		// most HTMLs have rownames map in cell - but this will pick up those
		// where they span rows.
		String tableStr = mergeRownameHtml(sb.toString());
		// NLP.pwNLP.append(//NLP.printLn("merged tableStr==="+tableStr+"end merge
		// TableStr");
		String line = "";
		String[] lineField;
		String[] prepSubTotalTable = tableStr.split("\r");
		String rownm = "", colStr = "";
		Integer pTR = -2, tbRw = -3;
		String data;
		// this preps map so it can be passed to getSubtotals method
		Map<Integer, List<String[]>> mapData = new TreeMap<Integer, List<String[]>>();
		List<String[]> listRow = new ArrayList<String[]>();

		for (int i = 0; i < prepSubTotalTable.length; i++) {
			line = prepSubTotalTable[i];
			lineField = line.split("\',\'");
			if (line.replaceAll("[\r \\-=]", "").length() < 1)
				continue;
			rownm = lineField[8];
			colStr = lineField[5];
			tbRw = Integer.parseInt(lineField[6].replaceAll("[\']{1,}", ""));
			data = lineField[9];
			// NLP.pwNLP.append(//NLP.printLn("tbRw=" + tbRw+" line=" + line);
			// NLP.pwNLP.append(//NLP.printLn("colStr.replaceAll([\']{1,},
			// ).length()="+colStr.replaceAll("[\']{1,}",
			// "").length());

			// get RNH and put into tbl.
			// NLP.pwNLP.append(//NLP.printLn("colStr="+colStr+ " data="+data
			// + " prevTblRow=" + pTR+ " tblRow="+tbRw);

			if (tbRw != pTR) {

				// add dummy ary[2] to pass getSubtotal method
				String[] ary = { rownm.length() + "", rownm + " ", 1 + "" };
				listRow.add(ary);
				// NLP.pwNLP.append(//NLP.printLn("added ary="+Arrays.toString(ary));
			}

			if (colStr.replaceAll("[\']{1,}", "").equals("0") /* || pTR==-2 */) {
				// NLP.pwNLP.append(//NLP.printLn("RNH colStr="+colStr+" pTR="+pTR+"
				// data="+data+" rownm="+rownm);
				// NLP.pwNLP.append(//NLP.printLn("RNH - added to mapData. Key="+tbRw+"
				// listRow=");
				//// NLP.printListOfStringArray(listRow);
				mapData.put(tbRw, listRow);
				// NLP.pwNLP.append(//NLP.printLn(" map after adding RNH=");
				//// NLP.printMapIntListOfStringAry(mapData);
				listRow = new ArrayList<String[]>();
				pTR = tbRw;
				continue;
			}

			if (tbRw != pTR) {
				// NLP.pwNLP.append(//NLP.printLn("B tblrow="+tbRw+" prevTblRow="+pTR+"
				// data="+data+" rownm="+rownm);
				// NLP.pwNLP.append(//NLP.printLn("before adding ary2. listRow=");
				//// NLP.printListOfStringArray(listRow);
				String[] ary = { colStr, data, 1 + "" };
				// NLP.pwNLP.append(//NLP.printLn("B ary2 -- colStr="+colStr+" data="+data);
				listRow.add(ary);
				// NLP.pwNLP.append(//NLP.printLn("key="+tbRw+" B added to map listRow=");
				//// NLP.printListOfStringArray(listRow);
				mapData.put(tbRw, listRow);
				// NLP.pwNLP.append(//NLP.printLn("B map after adding listRow=");
				//// NLP.printMapIntListOfStringAry(mapData);
				listRow = new ArrayList<String[]>();
			}

			pTR = tbRw;
		}

		// NLP.pwNLP.append(//NLP.printLn("print mapData before getSubtotal=");
		//// NLP.printMapIntListOfStringAry(mapData);

		mapData = NLP.getSubtotals(mapData, 1, true, tableNameShort, false);

		// NLP.pwNLP.append(//NLP.printLn("mapdata subtotal finished - print==");
		//// NLP.printMapIntListOfStringAry(mapData);

		String rownameMap = "";
		// Connection con = MysqlConnUtils.getConnection();
		// java.sql.DatabaseMetaData dbm = con.getMetaData();
		// check if table exists

		StringBuffer sb2 = new StringBuffer(""/*
												 * "insert ignore into tp_raw" + table + " values"
												 */);
		// This rejoins the subtotaled map with entire lines of table - key in
		// map fetched by tblrow in each line.
		int linesInTable = prepSubTotalTable.length;
		boolean hasSubTotal = false;
		for (int i = 0; i < linesInTable; i++) {
			// get key at each line equal to tblrow
			line = prepSubTotalTable[i];
			if (line.replaceAll("[\r \\-=]", "").length() < 1)
				continue;
			lineField = line.split("\',\'");
			rownm = lineField[8];

			tbRw = Integer.parseInt(lineField[6].replaceAll("[\']{1,}", ""));
			if (mapData.size() > 0 && mapData.get(tbRw).size() > 0) {
				rownameMap = mapData.get(tbRw).get(0)[1].replaceAll("[ ]{2,}", " ").trim().replaceAll("'", "");
			}

			sb2.append("\r");
			for (int c = 0; c < lineField.length; c++) {
				if (c == 8) {
					// NLP.pwNLP.append(//NLP.printLn("c==8"+" rownameMap="+rownameMap);

					String tl = "";
					if (rownameMap.contains(";TL")) {
						tl = rownameMap.substring(rownameMap.indexOf(";TL") + 3).trim();
						// NLP.pwNLP.append(//NLP.printLn("1 tl="+tl);
						tl = tl.substring(0, tl.indexOf(";")).trim();
						// NLP.pwNLP.append(//NLP.printLn("2 tl="+tl);
						hasSubTotal = true;
					}

					if (!rownameMap.contains(";TL")) {
						tl = "\\N";
					}

					String st = "";
					if (rownameMap.contains(";ST")) {
						st = rownameMap.substring(rownameMap.indexOf(";ST") + 3).trim();
						// NLP.pwNLP.append(//NLP.printLn("1 st="+st);
						st = st.substring(0, st.indexOf(";")).trim();
						// NLP.pwNLP.append(//NLP.printLn("2 st="+st);
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

					// System.xut.println("rowNameMap="+rownameMap);

					sb2.append(TableTextParser.replaceEndStrands(rownameMap.replaceAll(",", " ")) + "','" + lineField[9]
							+ "'," + tl + "," + st + "," + net + "," + sub + ",'");
				}
				if (c != 8 && c != 9) {
					if (c < (lineField.length - 1)) {
						sb2.append(lineField[c].replaceAll(",", " ") + "','");
					} else
						sb2.append(lineField[c].replaceAll(",", " "));
				}
			}
		}

		// is sub/net/ttl/stt - then count if row>10.
		if (hasSubTotal) {
			// actual count of tables
			tblC2++;
		}

		if (sb2.toString() != null && sb2.toString().length() > 4000) {
			insertIntoMysql = true;
			tblC2++;
			tableCount++;
			TableParser.pwYyyyQtr.append(sb2.toString().trim().substring(1, sb2.toString().length() - 3)
					.replaceAll("\'xxy\'", "\\\\N").replaceAll("\\) ?[\r\n]{1,2}\\(", "\r").replaceAll("\'", ""));

			// System.out.println(sb2.toString().trim().substring(1, sb2.toString().length()
			// - 3)
			// .replaceAll("\'xxy\'", "\\\\N").replaceAll("\\) ?[\r\n]{1,2}\\(",
			// "\r").replaceAll("\'", ""));
			//
			// MysqlConnUtils.executeQuery(sb2.toString()
			// .replaceAll("\'xxy\'", "\\\\N")
			// + ";");
			// xxxxxx

			// NLP.pwNLP.append(//NLP.printLn("2 coNameOnPriorLine=="
			// + coNameOnPriorLine, ""));

			hasSubTotal = false;
			sb2.setLength(0);
			sb.setLength(0);
			allColText = "";
			colHdgAllLinesPatterns = "";
			decimal = "1";
		}

		if (parseTableFromFolder)
			return;

		if (gotTableWithin) {
			// NLP.pwNLP.append(//NLP.printLn("gotTableWithin=" + gotTableWithin
			// + " tableNameLong=" + tableNameLong, ""));
			priorTableNameLong = tableNameLong.replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n", "");
			parseTable(tableHtml);
		}
	}

	public String mergeRownameHtml(String tableStr) {

		// NLP.pwNLP.append(//NLP.printLn("tableStr="+tableStr+"end tableStr");
		// final pass in order to merge rownames:
		boolean isRNH = false, done = false;
		int tRow = 0, pTrow = 0, tRowSub = 0, cnt;
		String dataSub, lineSub, line, rowname, rownameSub, data;

		// if current row is not RNH and has no data see if next row is not RNH
		// and if not merge
		Map<Integer, String> mapRownameMerge = new TreeMap<Integer, String>();
		String[] fnlTable = tableStr.split("[\r\n]"), lineField;

		for (int i = 0; i < fnlTable.length; i++) {
			line = fnlTable[i];
			if (line.replace("[ \\-=]{1,}", "").length() < 1)
				continue;
			isRNH = false;
			done = false;
			lineField = line.split("\',\'");
			if (lineField.length < 9)
				continue;
			rowname = lineField[8];
			if (lineField.length < 10)
				continue;
			data = lineField[9];
			tRow = Integer.parseInt(lineField[6].trim());
			isRNH = seeIfItIsRNH(data, rowname);

			if (isRNH)
				rowname = rowname + " RNH";
			// NLP.pwNLP.append(//NLP.printLn("isRNH="+isRNH+" rowname="+rowname+"
			// tRow="+tRow+"\r line="+line);
			if (!isRNH && data.contains("xxy'")) {
				cnt = 0;
				int c = i;
				done = false;
				for (; c < fnlTable.length; c++) {
					cnt++;
					if (cnt == 1)
						continue;
					if (cnt > 5)
						break;
					lineSub = fnlTable[c];
					tRowSub = Integer.parseInt(lineSub.split("\',\'")[6].trim());
					rownameSub = lineSub.split("\',\'")[8];
					// NLP.pwNLP.append(//NLP.printLn("rownamesub="+rownameSub+"first
					// letter
					// in rownamesub="+rownameSub.substring(0,1)+
					// "
					// rownameSplit.len="+rownameSub.substring(0,1).replaceAll("[A-Z]{1}","").length());

					if (rownameSub != null && rownameSub.length() > 0
							&& rownameSub.substring(0, 1).replaceAll("[A-Z]{1}", "").length() == 0) {
						isRNH = true;
						break;
					}

					dataSub = lineSub.split("\',\'")[9];
					isRNH = seeIfItIsRNH(dataSub, rownameSub);

					// NLP.pwNLP.append(//NLP.printLn("cnt=" + cnt + "\rlineSub="
					// + lineSub
					// + "\rrownmSub=" + rownameSub + "\rline=" + line
					// + "\rdataSub=" + dataSub + "\risRnh=" + isRNH);
					if (!isRNH) {
						rowname = rowname + " |" + rownameSub;
						// NLP.pwNLP.append(//NLP.printLn("rownm in sub routine="+rowname+
						// " datasub"+dataSub);
					}
					if (!dataSub.contains("xxy")) {
						done = true;
						i = c;
						tRow = tRowSub;
						break;
					}
				}
			}

			if (pTrow != tRow || tRow == 0) {
				// NLP.pwNLP.append(//NLP.printLn("FINAL rownm=" + rowname +
				// "\rtrow=" +
				// tRow+ "\npTrow="+pTrow);
				mapRownameMerge.put(tRow, rowname);
			}
			pTrow = tRow;
		}

		// NLP.pwNLP.append(//NLP.printLn("printng mapRownameMerge=");
		// printMapIntStr(mapRownameMerge);

		NLP nlp = new NLP();

		StringBuffer sb = new StringBuffer();
		int curTrow = 1, prevTrow = 0;
		for (int i = 0; i < fnlTable.length; i++) {
			line = fnlTable[i];
			if (line.replace("[ \\-=]{1,}", "").length() < 1)
				continue;
			isRNH = false;
			done = false;
			lineField = line.split("\',\'");
			if (lineField.length < 7 || !nlp.isNumeric(lineField[6].trim()))
				continue;
			tRow = Integer.parseInt(lineField[6].trim());
			rowname = mapRownameMerge.get(tRow);
			if (rowname != null) {
				// NLP.pwNLP.append(//NLP.printLn("from map rowname="+rowname);
				// NLP.pwNLP.append(//NLP.printLn("line="+line);
				// NLP.pwNLP.append(//NLP.printLn(" tRow="+tRow);

				sb.append("\r");
				for (int c = 0; c < lineField.length; c++) {
					// NLP.pwNLP.append(//NLP.printLn(" c="+c+" lineField="+lineField[c]);
					/*
					 * if( c == 6){ curTrow = Integer.parseInt(lineField[c]); NLP
					 * .pwNLP.append(//NLP.printLn("curTrow - prevTrow="+(curTrow - prevTrow));
					 * if(curTrow - prevTrow > 1){ curTrow = prevTrow+1;
					 * 
					 * } prevTrow = curTrow; NLP.pwNLP.append(//NLP.printLn("prevTrow="+prevTrow);
					 * sb.append(curTrow + "','");
					 * NLP.pwNLP.append(//NLP.printLn("curTrow="+curTrow); prevTrow = curTrow; }
					 */

					if (c == 8)
						sb.append(rowname + "','");
					if (/* c != 6 && */c != 8 && c != (lineField.length - 1))
						sb.append(lineField[c] + "','");
					if (c == (lineField.length - 1))
						sb.append(lineField[c]);
				}
			}
		}
		// NLP.pwNLP.append(//NLP.printLn("sb.toString merged rownames="+sb.toString());

		return sb.toString();
	}

	public boolean seeIfItIsRNH(String data, String rownm) {

		Pattern patternALLCAPS = Pattern.compile("(?<!([a-z]))[A-Z]{5,}");
		Matcher matchALLCAPS = patternALLCAPS.matcher(rownm);
		boolean isRNH = false;
		if (data.contains("xxy'") && rownm.trim().endsWith(":") || matchALLCAPS.find()
				|| (rownm.split("(\\b[A-Z])").length > 2 && rownm.split("(?i)(plant|equip|propert)").length > 1
						&& rownm.split("(?i)(plant|equip|propert)").length > 1)
				|| (rownm.split("(\\b[^A-Z\\)\\( ;:])").length < 2
						&& rownm.split("(\\b[A-Z])").length / rownm.replaceAll("  ", " ").split(" ").length + 1 > .65)
				|| (tableNameShort.equals("cf") && rownm.split("(?i)(reconcile|adjustment)").length > 1)) {
			isRNH = true;
			// NLP.pwNLP.append(//NLP.printLn("isRnh=" + isRNH);
		} else
			isRNH = false;

		return isRNH;
	}

	public void getTablesFromFiling(String text, boolean parseFromTablesFolder, boolean parseEntire)
			throws IOException, ParseException, SQLException {

		TableParser tp = new TableParser();
		// THIS METHOD DIGESTS SEC.GOV FILING

		System.out.println("table html");

		NLP nlp = new NLP();

		File filename = new File("c:/temp2/1");
		// if (filename.exists())
		// filename.delete();

		// NLP.pwNLP = new PrintWriter(filename);
		System.out.println("filename=" + filename);

		// only reset here - not at each table!
		parseEntireHtml = parseEntire;
		if (parseEntireHtml) {
			getTableOfContents = true;
		}
		originalText = text;

		double startTime = System.currentTimeMillis();
		text = text.replaceAll("\\&nbsp;|\\xA0|\\&#160;", " ").replaceAll("\\&#151;", "-").replaceAll("&#151;", "0")
				.replaceAll("(?i)<TH", "<td").replaceAll("(?i)</TH", "</td")
				.replaceAll("(?i)Sept[\\. ]{1}", "September").replaceAll(
						"(?i)(January|February|March|April|May|June|July|August|September|October|November|December)([0-9]{1})",
						"$1 $2");

		text = text.replaceAll("(?i)balance sheet data", "XX BS XX DATA");
		text = text.replaceAll("(?i)Decmeber", "December");

		text = text.replaceAll("\\(unaudited for 2003\\)|\\]|\\[", "");

		// remove 2 or more blank table rows
		text = text.replaceAll("<TR><TD>( |\\&nbsp;)?</TD></TR><TR><TD>( |\\&nbsp;)?</TD></TR>", "");

		// fixes December 31,1999 to 31, 1999
		text = text.replaceAll("(0?[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1},)([12]{1}[09]{1}[0-9]{2}[$ \t<]{1})", "$1 $2");

		text = text.replaceAll("[-_=]{5,}", "");// throws off matcher if not
												// removed.

		text = text.replaceAll("(?ism)C O N S O L I D A T E D.(?=.{0,5})B A L A N C E.{0,5}S H E E T ? S?",
				"CONSOLIDATED BALANCE SHEET");

		text = text.replaceAll("(?ism)(C O N S O L I D A T E D)?(?=.{0,5})"
				+ "(S T A T E M E N T ? S?|R E S U L T S?).{0,5}" + "(O.{0,2}F)." + "{0,5}"
				+ "(E A R N I N G S|O P E R A T I O N S?|I N C O M E|P R O F I T S?|D I S T R I B U T A B L E  ? ?I N C O M E)",
				"CONSOLIDATED STATEMENT OF INCOME");

		text = text.replaceAll("(?ism)(C O N S O L I D A T E D)(?=.{0,5})" + "(S T A T E M E N T ? S?).{0,5}"
				+ "(O.{0,2}F)." + "{0,5}" + "(C A S H.{0,5}F L O W S?)", "CONSOLIDATED STATEMENT OF CASH FLOWS");

		text = text.replaceAll("(sm)STATEMENTS?.{0,5}OF", "STATEMENTS OF");

		// ensure separation by just one space. But have to contain the .{0,5}
		// in its own group to avoid grabbing and replacing parts of html. To
		// skip that group I also need to indicate ?=.{0,5} (in that case it
		// also doesn't count it as a capturing group. So group #s are 1,2,3
		// versus 1,2,3,4,5,6. Utilize non-capturing where I don't want to strip
		// html which is at start and end of pattern

		text = text.replaceAll(
				"(?ism)(condensed)?(?=.{0,5})(consolidated)?(?=.{0,5})(Results|Statements?).{0,5}(of).{0,5}"
						+ "(Cash.{0,3}Flow|Financial Condition|Income|Operations?|Earnings?|Profits?|Distributable Income)",
				"$1 $2 $3 $4 $5");

		text = text.replaceAll("(?ism)(consolidated|condensed)[ ]{2,}(statements)", "$1 $2");

		text = text.replaceAll("(?ism)consolidated(?=.{0,5})balance.{0,5}sheets?", "Consolidated Balance Sheets");

		// fix if lower case
		text = text.replaceAll("(?sm)(consolidated)? ?statements? of (income|operations?|earnings?)",
				"Consolidated Statement of Income");
		// fix if lower case
		text = text.replaceAll("(?sm)(consolidated)? statements? of cash flows", "Consolidated Statement of Cash Flow");

		// replace with common tablename and fix if lower case
		text = text.replaceAll("(?ism)(condensed)? ?(consolidated)? ?statements? of (profits?|distributable income)",
				"Consolidated Statement of Income");

		text = text.replaceAll("(?ism)(The.{0,3}accompanying.{0,3}|See.{0,3})?notes.{0,3}(to.{0,3}|"
				+ "are.{0,3}an.{0,3}integral.{0,3}part.{0,3}of.{0,3}these.{0,3})"
				+ "(u?n?audited.{0,3})?(condensed.{0,3})?(consolidated.{0,3})?financial.{0,3}statements", "");

		text = text.replaceAll("(?i)<br />", "<br>");
		double endTime = System.currentTimeMillis();
		double duration = (endTime - startTime) / 1000;
		System.out.println("text replacement=" + duration);
		startTime = System.currentTimeMillis();

		List<Integer> ListTableStartIdx = new ArrayList<Integer>();
		Pattern tableStartHtmlPattern = Pattern.compile("(?i)(<table)");
		Matcher matchHtmlTableStart = tableStartHtmlPattern.matcher(text);
		endTime = System.currentTimeMillis();
		duration = (endTime - startTime) / 1000;
		// System.xut.println("match html tableStart pattern=" + duration);
		startTime = System.currentTimeMillis();

		Pattern tableEndHtmlPattern = Pattern.compile("(?i)(</table)");
		Matcher matchHtmlTableEnd = tableEndHtmlPattern.matcher(text);
		endTime = System.currentTimeMillis();
		duration = (endTime - startTime) / 1000;
		// System.xut.println("match html tableEnd pattern=" + duration);
		startTime = System.currentTimeMillis();

		// public variables are not automatically set based on the pre-set
		// global value -- they must be set manually below.
		prevChRows = null;
		tableHtml = "";
		tableCount = 0;
		tableNameLong = "";
		priorTableNameLong = "";
		tableStartIdx = 0;
		tableEndIdx = 0;
		nextTableEndIdx = 0;
		allColText = "";
		allColTextOneMonth = "";
		chAboveRowname = "";
		colHdgAllLinesPatterns = "";
		colHeadingsList.clear();
		colHeadingsMap.clear();
		columnOneAndTwoSameYear = false;
		colHdgOnSameRowAsTableName = false;
		coMatch = 0;
		coNameMatched = "";
		coNameOnPriorLine = "";
		decimal = "1";
		excludeTable = "";
		edtPeriodList.clear();
		edtPeriodListRawText.clear();
		if (prevChRows != null)
			prevChRows.clear();
		endDatesPeriodsListFromTableSentences = new ArrayList<>();
		finalMergedChFromRawTextList.clear();
		insertIntoMysql = false;
		listCHEndedYrandMo.clear();
		listCoName.clear();
		listFirstColEdtPer.clear();
		listLastColEdtPer.clear();
		numberOfColumnHdgs = 0;
		parseTableFromFolder = false;
		prevTableStartIdx = 0;
		prevTableEndIdx = 0;
		tableHtml = "";
		tableNameInsideTable = false;
		tableNameLong = "";
		tableNameOutsideTable = false;
		tableNameShort = "";
		textAfterTableName = "";
		tableSaved = "";
		tblC2 = 0;
		tsShort = "";
		tableNameOutsideButNotValidate = false;
		textBetweenTables = true;
		prevTableSentence = tableSentence;
		tableSentence = "";
		noteColIdxSt = -99;
		noteColIdxEnd = -99;
		noteColIdxFound = false;

		while (matchHtmlTableEnd.find()) {
			tableEndIdx = matchHtmlTableEnd.start();
			ListTableEndIdx.add(tableEndIdx);
		}

		while (matchHtmlTableStart.find()) {
			tableStartIdx = matchHtmlTableStart.start();
			ListTableStartIdx.add(tableStartIdx);
		}

		endTime = System.currentTimeMillis();
		duration = (endTime - startTime) / 1000;
		// System.xut.println("added to lists table start/eidx locs=" + duration);
		startTime = System.currentTimeMillis();

		// NLP.pwNLP.append(//NLP.printLn("ListTableStartIdx.size="
		// + ListTableStartIdx.size(), ""));

		/*
		 * find tablename either inside or outside start of html table (<table). Look
		 * outside first - and if validated - that is tablename. If no valid tablename
		 * found outside table check inside. If nothing found inside but invalid found
		 * outside (or inside) - parse but label tn inval
		 */

		int inside = 1800, outside = 2300, tnEidx = 0, tnSidx = 0;
		Matcher matchTableNames = null;
		Matcher matchTableNameInsideTable = null, matchTableNameOutsideTable = null;
		String tableName = null, tableNameInvalid = null, outSideTableTextHtmlStripped = null, outSideTableText = "";
		tableCount = 0;
		tblC2 = 0;
		// NLP.pwNLP.append(//NLP.printLn(
		// "start of loop ListTableStartIdx- number of table starts are="
		// + ListTableStartIdx.size(), ""));
		for (int i = 0; i < ListTableStartIdx.size(); i++) {
			// NLP.pwNLP.append(//NLP.printLn(" ACCNO=" + acc, ""));
			// NLP.pwNLP.append(//NLP.printLn(
			// "@getTablesFromFiling looping thru each html table start -- accno=="
			// + acc, ""));
			// each loop starts to parse a new table - So clear all public
			// variables first.
			tableStartIdxLoopCount = i;
			allColText = "";
			allColTextOneMonth = "";
			chAboveRowname = "";
			colHdgAllLinesPatterns = "";
			colHeadingsList.clear();
			colHeadingsMap.clear();
			columnOneAndTwoSameYear = false;
			colHdgOnSameRowAsTableName = false;
			coMatch = 0;
			coNameMatched = "";
			// coNameOnPriorLine = "";
			decimal = "1";
			excludeTable = "";
			edtPeriodList.clear();
			edtPeriodListRawText.clear();
			endDatesPeriodsListFromTableSentences = new ArrayList<>();
			listOfStrHtmlColHdgs = new ArrayList<>();
			finalMergedChFromRawTextList.clear();
			listCHEndedYrandMo.clear();
			listCoName.clear();
			listFirstColEdtPer.clear();
			listLastColEdtPer.clear();
			numberOfColumnHdgs = 0;
			parseTableFromFolder = false;
			mapCHs.clear();
			tableHtml = "";
			tableNameInsideTable = false;
			priorTableNameLong = tableNameLong.replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n", "");
			tableNameLong = "";
			tableNameOutsideTable = false;
			tableNameShort = "";
			tableSaved = "";
			textAfterTableName = "";
			tblC2 = 0;
			tsShort = "";
			tableNameOutsideButNotValidate = false;
			textBetweenTables = true;
			prevTableSentence = tableSentence;
			tableSentence = "";
			noteColIdxSt = -99;
			noteColIdxEnd = -99;
			noteColIdxFound = false;
			matchTableNames = null;
			matchTableNameInsideTable = null;
			matchTableNameOutsideTable = null;
			tableStartIdx = ListTableStartIdx.get(i);
			int tmpEndIdx = 0;

			if (i > 0 && (i - 1) < ListTableEndIdx.size()) {
				if (ListTableEndIdx.get(i - 1) < tableStartIdx) {
					tmpEndIdx = ListTableEndIdx.get(i - 1);
				}

				if (ListTableEndIdx.get(i - 1) > tableStartIdx && i > 1) {
					tmpEndIdx = ListTableEndIdx.get(i - 2);
				}

				if (tmpEndIdx == 0)
					break;

				// NLP.pwNLP.append(//NLP.printLn("tmpEndIdx=" + tmpEndIdx, ""));
				// NLP.pwNLP.append(//NLP.printLn("tableStartIdx===="
				// + tableStartIdx, ""));

				// NLP.pwNLP.append(//NLP.printLn("textBetwTables="
				// + text.substring(tmpEndIdx, tableStartIdx));
				// NLP.pwNLP.append(//NLP.printLn("textBetwTables replace xx|\r\n\t="
				// + text.substring(tmpEndIdx, tableStartIdx));

				String textBetwTables = "";
				// System.xut.println("tmpEndIdx="+tmpEndIdx+" tableStartIdx="+tableStartIdx);
				if (tmpEndIdx < tableStartIdx && tableStartIdx > 0 && tmpEndIdx > 0) {
					textBetwTables = nlp.stripHtmlTags(text.substring(tmpEndIdx, tableStartIdx))
							.replaceAll("xx|[ \r\n\t]{1}", "");
				} else
					textBetwTables = "";

				endTime = System.currentTimeMillis();
				duration = (endTime - startTime) / 1000;
				// System.xut.println("stripHtml text between tables=" + duration);
				startTime = System.currentTimeMillis();

				// NLP.pwNLP.append(//NLP.printLn("textBetwTables.len="
				// + textBetwTables.length() + " text between tables="
				// + textBetwTables + "|end", ""));
				if (textBetwTables.length() < 50
						&& textBetwTables.replaceAll("(?i)(liabilities)(and)?(share|stock)?(holder)", "").length() < 20)
					textBetweenTables = false;
			}

			if (textBetweenTables)
				coNameOnPriorLine = "";

			// NLP.pwNLP.append(//NLP.printLn("textBetweenTables="
			// + textBetweenTables, ""));

			if (text.substring(tableStartIdx).indexOf("</table") < 1
					&& text.substring(tableStartIdx).indexOf("</TABLE") < 1)
				continue;

			tableHtml = text.substring(tableStartIdx, StringUtils.indexOfIgnoreCase(text, "</table", tableStartIdx) - 8)
					.replaceAll("([12]{1}) ([09]{1}) (\\d) (\\d).{0,2}(?!\\d)", "$1$2$3$4").replaceAll(
							"(?i)([\\d]{2})[- ]{1}(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[- ]{1}([\\d]{2})",
							"$2 $1, $3");
			// NLP.pwNLP.append(//NLP.printLn("tableHtml=",tableHtml));

			tableName = null;
			tableNameInvalid = null;
			outSideTableTextHtmlStripped = null;
			outSideTableText = "";
			textAfterTableName = "";
			tnEidx = 0;
			tnSidx = 0;
			String[] tabenameLongStartIdxAndEndIdx;

			// stripLineWrap - joins the two lines where it is due to a
			// <div|<p|<br and NOT due to a natural line wrap within a paragraph
			// for example.

			// can't have start of outside table text start prior to start of
			// last tableHtml end idx

			if ((i - 1) < ListTableEndIdx.size() && i > 0) {
				if (ListTableEndIdx.get(i - 1) < tableStartIdx) {
					tmpEndIdx = ListTableEndIdx.get(i - 1);
				}
				if (ListTableEndIdx.get(i - 1) > tableStartIdx && i > 1) {
					tmpEndIdx = ListTableEndIdx.get(i - 2);
				}
				if (tmpEndIdx == 0)
					break;

				// NLP.pwNLP.append(//NLP.printLn("1 outSideTableText bef
				// strip="+text.substring(
				// Math.max(Math.max(tableStartIdx - outside,tmpEndIdx), 0),
				// tableStartIdx)+"|END");
				if (Math.max(Math.max(tableStartIdx - outside, tmpEndIdx), 0) < tableStartIdx) {
					outSideTableText = nlp.stripLineWrap(
							text.substring(Math.max(Math.max(tableStartIdx - outside, tmpEndIdx), 0), tableStartIdx));
				} else
					outSideTableText = "";
			}

			else {
				outSideTableText = nlp
						.stripLineWrap(text.substring(Math.max(tableStartIdx - outside, 0), tableStartIdx));
			}

			// NLP.pwNLP.append(//NLP.printLn("1 outSideTableText="
			// + outSideTableText, ""));
			outSideTableTextHtmlStripped = nlp
					.stripHtmlTags(outSideTableText.replaceAll("(?i)</(H1|DIV|P|BR)>", "\r\n"))
					.replaceAll("[ ]{2,}", " ").replaceAll("\r\n|\r\n|\r\r|\n\n", "\r");
			// NLP.pwNLP.append(//NLP.printLn("1 outSideTableTextHtmlStripped="
			// + outSideTableTextHtmlStripped + "|END", ""));

			// for html text - col hdgs are not defined by two or more ws - so
			// remove them at this point (parseTableSentence method uses two or
			// ws) and later I need to measure tablename end Idx location in
			// this string.

			// can use strip b/c tablename matcher ignores hard return
			tabenameLongStartIdxAndEndIdx = NLP.getTablenamePlainText(outSideTableTextHtmlStripped);

			// NLP.pwNLP.append(//NLP.printLn("outSideTableTextHtmlStripped="+outSideTableTextHtmlStripped);
			// gets last instance of tablename in text - or in text above last
			// instance prior to start of table

			tableNameLong = tabenameLongStartIdxAndEndIdx[0].replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n", "");
			tableNameShort = tp.getTableNameShort(priorTableNameLong);

			// NLP.pwNLP.append(//NLP.printLn("1a tableNameLong=" + tableNameLong,
			// ""));
			// textAfterTableName is all text after tablename prior to start of
			// table - but once it is passed thru getTablenamePlainText it
			// returns just text AFTER tn on line tablename is found.
			// textAfterTableName = tabenameLongStartIdxAndEndIdx[3];

			tnEidx = Integer.parseInt(tabenameLongStartIdxAndEndIdx[2]);
			tnSidx = Integer.parseInt(tabenameLongStartIdxAndEndIdx[1]);

			// get all text on one line related to tablename.

			// NLP.pwNLP.append(//NLP.printLn("tnEidx=" + tnEidx
			// + " outSideTableTextHtmlStripped.length()="
			// + outSideTableTextHtmlStripped.length(), ""));
			if (tnEidx > 0 && tnEidx < outSideTableTextHtmlStripped.length()) {
				textAfterTableName = outSideTableTextHtmlStripped.substring(tnEidx,
						outSideTableTextHtmlStripped.length());
			}

			// NLP.pwNLP.append(//NLP.printLn("2 outside tableNameLong="
			// + tableNameLong + "\r2 textAfterTableName.len="
			// + textAfterTableName.length()
			// + "\r2 textOnLineAfterTableName=" + textAfterTableName
			// + "|end", ""));

			if (textAfterTableName.length() > 10) {
				// NLP.pwNLP.append(//NLP.printLn("validateTablename 1.
				// textAfterTableName="+textAfterTableName);

				// NLP.pwNLP.append(//NLP.printLn("validateTablename 1.
				// outSideTableTextHtmlStripped="+outSideTableTextHtmlStripped);

				tableNameOutsideTable = NLP.validateTablename(textAfterTableName,
						companyName.substring(0, Math.min(companyName.length(), 7)), outSideTableTextHtmlStripped);
				// NLP.pwNLP.append(//NLP.printLn("1. tableNameOutsideTable="
				// + tableNameOutsideTable, ""));

				if (tableNameOutsideTable)
					tableNameOutsideButNotValidate = false;
				else
					tableNameOutsideButNotValidate = true;

			}

			if (textAfterTableName.length() <= 10) {
				tableNameOutsideTable = true;
				tableNameOutsideButNotValidate = false;
			}

			// NLP.pwNLP.append(//NLP.printLn("a tableNameOutsideTable="
			// + tableNameOutsideTable + " tableNameLong=" + tableNameLong
			// + " coNameOnPriorLine=" + coNameOnPriorLine, ""));
			if (tableNameOutsideTable) {
				listCoName = NLP.getCompanyName(outSideTableText, companyName);
				if (listCoName.get(1).length() > 2) {
					coNameMatched = listCoName.get(1).replaceAll("\'", "");
				}
				if (listCoName.get(0).length() > 2) {
					coNameOnPriorLine = listCoName.get(0).replaceAll("'", "");
				}
				decimal = nlp.getDecimal(outSideTableTextHtmlStripped);
				// NLP.pwNLP.append(//NLP.printLn("A @tableNameOutsideTable
				// coNameMatched="+coNameMatched);
				// NLP.pwNLP.append(//NLP.printLn("3 coNameOnPriorLine=="+coNameOnPriorLine);
			}

			// what if tablename found outside but there is also tablename
			// inside followed by CH?

			String[] coNmTblNmTsDecCoNmPriorLn;
			// if tablename not found outside table now search inside table
			// NLP.pwNLP.append(//NLP.printLn("B
			// tableNameOutsideTable="+tableNameOutsideTable+"
			// tableNameLong="+tableNameLong);

			if (null == tableNameLong || tableNameLong.length() < 6 || !tableNameOutsideTable) {
				// NLP.pwNLP
				// .append(NLP
				// .println(
				// "B getting tableName inside table -- checking inside tableHtml",
				// ""));
				coNmTblNmTsDecCoNmPriorLn = NLP.getTablenameInsideHtmlTable(tableHtml, companyName);
				if (coNmTblNmTsDecCoNmPriorLn != null && coNmTblNmTsDecCoNmPriorLn[1].length() > 2) {
					tableNameLong = coNmTblNmTsDecCoNmPriorLn[1].replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n", "");
					// NLP.pwNLP.append(//NLP.printLn(
					// "11 tablename found inside - tableNameLong="
					// + tableNameLong, ""));
					priorTableNameLong = tableNameLong.replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n", "");
					textAfterTableName = coNmTblNmTsDecCoNmPriorLn[2].trim();
					// NLP.pwNLP.append(//NLP.printLn(
					// "validateTablename 2. textAfterTableName="
					// + textAfterTableName, ""));
					tableNameInsideTable = NLP.validateTablename(textAfterTableName.trim(),
							companyName.substring(0, Math.min(companyName.length(), 7)), outSideTableTextHtmlStripped);

					if (tableNameInsideTable) {
						tableNameShort = tp.getTableNameShort(tableNameLong);
					}

					// NLP.pwNLP.append(//NLP.printLn(
					// "11 validateTableName? tableNameInsideTable="
					// + tableNameInsideTable + " tableNameShort="
					// + tableNameShort, ""));

					if (coNmTblNmTsDecCoNmPriorLn != null) {
						// String [] ary =
						// {coNameParsed,tableName,tableSentence,getDecimalText,coNameOnPriorLine};
						coNameMatched = coNmTblNmTsDecCoNmPriorLn[0].replaceAll("\'", "");
						// NLP.pwNLP.append(//NLP.printLn("1 from getTablenameInsideHtmlTable -
						// coNameMatched="+coNameMatched);
						if (coNameMatched.length() > 2 && coNameMatched.length() < maxCoNameLength) {
							coMatch = 1;
						}

						coNameOnPriorLine = coNmTblNmTsDecCoNmPriorLn[4].replaceAll("'", "");
						// NLP.pwNLP.append(//NLP.printLn("4 coNameOnPriorLine=="
						// + coNameOnPriorLine);
						decimal = nlp.getDecimal(coNmTblNmTsDecCoNmPriorLn[3]);
					}
				}
			}

			if (null == tableNameLong) {
				// NLP.pwNLP.append(NLP.printLn("tablname not found", ""));
				continue;
			}

			if (!tableNameInsideTable && !tableNameOutsideTable) {
				tableNameShort = "validate";
				// NLP.pwNLP.append(//NLP.printLn("tablensentence wasn't attempt - lets try it
				// if tablname was found outside but note validated");
			}
			if (tableNameInsideTable || tableNameOutsideTable) {
				// NLP.pwNLP.append(//NLP.printLn("4 tableNameLong=" +
				// tableNameLong);
				tableNameShort = tp.getTableNameShort(tableNameLong);
			}
			if (coNameMatched.length() > 2 && coNameMatched.length() < maxCoNameLength) {
				coMatch = 1;
			} else {
				coMatch = 0;
			}

			String tsPattern = "";
			// NLP.pwNLP.append(//NLP.printLn("was tablename outside?
			// tableNameOutsideTable="
			// + tableNameOutsideTable
			// + " tableNameOutsideButNotValidate="
			// + tableNameOutsideButNotValidate);
			// NLP.pwNLP.append(//NLP.printLn("4c tableSentence="+tableSentence);
			if (tableNameOutsideTable || tableNameOutsideButNotValidate) {
				// NOTE:?? this if condition: tableNameOutsideTable ||
				// tableNameOutsideButNotValidate -- will look for tablename
				// even if it is found outside and validated. That can't be
				// right. At best all I want to look for is ts which is almost
				// all instances there won't be any. If table name was found
				// outside and validated - this should skipped.
				// if tableNameInside - the below is run at getCHnDataRows

				// NLP.pwNLP.append(//NLP.printLn(
				// "11 not validated *textAfterTableName right before parseTableSentence="
				// + textAfterTableName + "|tablenamelong="
				// + tableNameLong, ""));
				if (textAfterTableName != null && tsShort.length() < 1) {
					// NLP.pwNLP.append(//NLP.printLn("5 tableNameOutside="
					// + tableNameOutsideTable
					// + " tableNameOutsideButNotValidate="
					// + tableNameOutsideButNotValidate
					// + " tableNameLong=" + tableNameLong, ""));
					List<String> listTableSentenceAndTSshort = NLP.parseTableSentence(
							/* tableNameLong+"\r "+ */textAfterTableName,
							companyName.substring(0, Math.min(companyName.length(), 7)), false, tableNameLong, false);
					tableSentence = listTableSentenceAndTSshort.get(0);
					// NLP.pwNLP.append(//NLP.printLn("5c tableSentence="
					// + tableSentence, "earlier tableNameLong="
					// + tableNameLong));

					if (listTableSentenceAndTSshort.get(6).length() > 6) {

						tableNameLong = listTableSentenceAndTSshort.get(6).replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n",
								"");
						tableNameShort = tp.getTableNameShort(tableNameLong);

						tsShort = listTableSentenceAndTSshort.get(1);
						tsPattern = listTableSentenceAndTSshort.get(2)
								.replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)", "");

						// NLP.pwNLP.append(//NLP.printLn(
						// "5c tableNameOutsideTable="
						// + tableNameOutsideTable
						// + " was tablesentence found="
						// + tableSentence + " \r3 tsShort="
						// + tsShort
						// + " tableNameOutsideButNotValidate="
						// + tableNameOutsideButNotValidate, ""));
					}

					// if tablesentence not found - maybe tablename is inside
					// table. Can't search inside for tablename if found outside
					// & validated.

					if (tableSentence.trim().length() < 1 && !tableNameOutsideButNotValidate) {
						// tablesentence NOT found outside table. Ck if
						// tablename and tablesentence is inside table - if so
						// then use that tablename even if tablesentence not
						// found inside. tableName inside should be more
						// accurate - see method. UNLESS F/P cash flow from
						// operating activities etc -- which will be after CH?

						String[] tmpAry = NLP.getTablenameInsideHtmlTable(tableHtml, "");
						// [0]=coNameParsed,[1]=tableName,[2]=tableSentence,[3]=getDecimalText
						if (tmpAry[1].length() > 7) {
							// NLP.pwNLP.append(//NLP.printLn("tmpAry[1]=",
							// tmpAry[1]));
							tableSentence = tmpAry[2];
							tableNameLong = tmpAry[1].replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n", "");
							// NLP.pwNLP
							// .append(NLP
							// .println(
							// "2 tablename found inside - tableNameLong=",
							// tableNameLong));
							coNameOnPriorLine = tmpAry[4].replaceAll("'", "");
							// NLP.pwNLP.append(//NLP.printLn("5 @tmpAry
							// coNameOnPriorLine="+coNameOnPriorLine);
							// keep this tablename even if not validated
							if (tableSentence.length() <= 1) {
								tableNameInsideTable = true;
							}

							if (tableSentence.length() > 1) {
								// NLP.pwNLP.append(//NLP.printLn(
								// "validateTablename 3. textAfterTableName="
								// + tableSentence, ""));
								tableNameInsideTable = NLP.validateTablename(tableSentence,
										companyName.substring(0, Math.min(companyName.length(), 7)),
										outSideTableTextHtmlStripped);
								// if validated - tableNameInsideTable set to
								// true - and elsewhere in this class
								// parseTableSentence method is run
								// tableNameShort =
								// tp.getTableNameShort(tableNameLong);
								// NLP.pwNLP.append(//NLP.printLn("tableNameLong="+tableNameLong+
								// " tableNameShort="+tableNameShort);

							}

							// NLP.pwNLP
							// .append(NLP
							// .println(
							// "C getTablenameInsideHtmlTable - tmpAry[0] -- coNameMatched=",
							// coNameMatched
							// + " \rtableNameLong="
							// + "\rtableNameLong"
							// + " \rtableSentence="
							// + tableSentence));
						}

						// if tablename validated as inside table - reset
						// tableNameOutside as false. - parseTableSentence
						// method is run later in this class

						if (tableNameInsideTable) {
							tableNameOutsideTable = false;
							// NLP.pwNLP.append(//NLP.printLn("7 tableNameLong="
							// + tableNameLong, ""));
							priorTableNameLong = tableNameLong.replaceAll("\\\\|\\)|\\(|\\$|'|\\*|\r|\n", "");
							tableNameShort = tp.getTableNameShort(tableNameLong);
						}

						// NLP.pwNLP.append(//NLP.printLn("set coNameMatched here if tmpAry[0].len>1");
						if (tmpAry[0].length() > 1 && tmpAry[0].length() < maxCoNameLength
								&& coNameMatched.length() < 2) {
							coNameMatched = tmpAry[0];
							// NLP.pwNLP.append(//NLP.printLn("@tmpAry coNameMatched="+coNameMatched);
							coMatch = 1;
						}
						if (decimal.equals("1")) {
							decimal = nlp.getDecimal(tmpAry[3]);
						}
					}

				}
			}

			// if textAfterTableName is >50 - continue to next table- record
			// textAfterTableName in bac_tp_raw
			// NLP.pwNLP.append(//NLP.printLn("tableNameOutsideTable="
			// + tableNameOutsideTable
			// + " tableNameOutsideButNotValidate="
			// + tableNameOutsideButNotValidate + " tableNameInsideTable="
			// + tableNameInsideTable
			// + "\rtextAfterTableName.length()-tableSentence.length()="
			// + (textAfterTableName.length() - tableSentence.length()),
			// ""));

			if (!tableNameOutsideTable && !tableNameInsideTable && tableNameOutsideButNotValidate
					&& (textAfterTableName.length() - tableSentence.length()) > 200) {
				// NLP.pwNLP.append(//NLP.printLn("2 tablename not found");
				excludeTable = "exclude";
				// for that which I intend to 'exclude' mark tsShort=exclude
				// (this is where tn is 100 chars before tableHtml start. If
				// later they are all worthless
				// I can add 'continue' at point where i populate 'excludeTable'
				// string.
				continue;
			}

			if (!tableNameOutsideButNotValidate && !tableNameOutsideTable && !tableNameInsideTable) {
				// NLP.pwNLP.append(//NLP.printLn("tablname not found", ""));
				continue;
			}
			boolean parsed = false;
			// NLP.pwNLP.append(//NLP.printLn("6c tableSentence="+tableSentence);
			if (!tsPattern.equals("MY")
					&& (tableNameOutsideTable || (tableNameOutsideButNotValidate && !tableNameInsideTable))) {
				endDatesPeriodsListFromTableSentences = NLP.getTablesentenceColumnHeadings(tableSentence, tsShort,
						tsPattern);
				// NLP.pwNLP.append(NLP
				// .println("endDatesPeriodsListFromTableSentences right after
				// NLP.getTablesentenceColumnHeadings=");
				//// NLP.printListOfStringArray(endDatesPeriodsListFromTableSentences);
				// NLP.pwNLP.append(//NLP.printLn("was tableNameOutsideTable="
				// + tableNameOutsideTable + "\nwas tableNameInsideTable="
				// + tableNameInsideTable + "\n8 tableNameLong="
				// + tableNameLong + " \ncoNameMatched=" + coNameMatched);
				// NLP.pwNLP.append(//NLP.printLn("tableHtml.len="+tableHtml.length());
				// NLP.pwNLP.append(//NLP.printLn("7c before parseTable.
				// tableSentence="+tableSentence);

				parseTable(tableHtml);
				parsed = true;
			}

			if (tableNameInsideTable && !parsed) {
				// NLP.pwNLP.append(//NLP.printLn(
				// "parsing tableHtml - tableNameInsideTable", ""));
				parseTable(tableHtml);
			}
		}

		if (!insertIntoMysql) {
			// removed replacement of </table -- this replaceAll use to go after
			// isTextTableInHtml
			text = originalText.replaceAll(
					"(?i)(<(P|FONT|STRONG|HR|BR|B|I|CENTER|PRE)[^>]*>)|</(P|TABLE|FONT|STRONG|HR|BR|B|I|CENTER|PRE)>",
					"");
			// NLP.pwNLP.append(//NLP.printLn("it wasn't insertIntoMysql", ""));
			boolean parseItWithTableTextParser = false;
			// text = nlp.stripHtmlTags(text);
			parseItWithTableTextParser = NLP.isTextTableInHtml(text);
			// NLP.pwNLP.append(//NLP.printLn("parseItWithTableTextParser=="
			// + parseItWithTableTextParser, ""));
			if (parseItWithTableTextParser) {
				TableTextParser ttp = new TableTextParser(acc, fileDate, cik, tableCount, fye, formType, companyName);
				// NLP.pwNLP.append(//NLP.printLn("text substringafter html
				// strip=="+text.substring(0,Math.min(100,
				// text.length())));
				ttp.tableTextParser(text, false, parseEntireHtml, "1");
			}
		}
		// NLP.pwNLP.close();
	}

	public static void main(String[] arg) throws IOException, ParseException, SQLException {
		
		TableParserHTML tpHtml = new TableParserHTML("0000000000-00-000000", "20180101", "1", 0, "1231", "10-Q", "NBT BANCORP");
		MysqlConnUtils.executeQuery("truncate bac_tp_raw2018qtr1; truncate bac_toc;");
		// boolean parseEntireFiling = true;
		double startTime = System.currentTimeMillis();

		// this is the raw sec file that is run through TableParserHTML
		String text = Utils.readTextFromFile("c:/temp2/0001390777-18-000076.nc");
		text = NLP.getEx99s(NLP.removeGobblyGookGetContracts(text));
		// this is the file that is written and later ingested into mysql
		File file = new File("c:/backtest/tableparser/tmp.txt");
		if (file.exists())
			file.delete();

		TableParser.pwYyyyQtr = new PrintWriter(file);
		tpHtml.getTablesFromFiling((text), false, true);
		TableParser.pwYyyyQtr.close();
		
		File f = new File("c:/backtest/tableparser/addedHardReturnsPriortoAccno.txt");
		PrintWriter pwAddedHardReturnsPriortoAccno = new PrintWriter(f);
		String textAddedHardReturnsPriortoAccno = Utils.readTextFromFile(file.getAbsolutePath());
		
		textAddedHardReturnsPriortoAccno = textAddedHardReturnsPriortoAccno
				.replaceAll("(?ism)([\\d]{10}-[\\d]{2}-[\\d]{6})", "\r$1")
				.replaceAll("\r\r", "\r");
		//NOTE: single slash -- "\r$"
		pwAddedHardReturnsPriortoAccno.append(textAddedHardReturnsPriortoAccno);
		pwAddedHardReturnsPriortoAccno.close();
		
		String query = "LOAD Data LOCAL INFILE '" + f.getAbsolutePath().replaceAll("\\\\", "//")
				+ "' ignore INTO TABLE bac_tp_raw2018qtr1 FIELDS TERMINATED BY ',' " + "\rLINES TERMINATED BY '\\r';";

		MysqlConnUtils.executeQuery(query);

		double endTime = System.currentTimeMillis();
		double duration = (endTime - startTime) / 1000;
		System.out.println("final overall duration=" + duration);
		// NLP.pwNLP.close();
		
	}
}
