package xbrl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import contracts.GoLaw;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;

public class NLP {

	public static int formtxtPara = 0;
	public static int autoKey = 0;
	public static int priorTblNo = 0;
	public static String priorAccNo = "0";
	public static int priorLineNo = 0;
	public static String tableNextHtml = null;
	public boolean specialTableSentencePatternOneEndDateManyPeriods = false;
	public static String tablenameLongFromMultiCell = "";

	public static PrintWriter pwNLP = null;

	// pick up here to convert 'testText' to printWriter append. Trick is to
	// find where to close!

	// find roman numeral/alpha/nmb markers
	// xxxviii = 7 characters
	public static Pattern patternParaNumbMarker = Pattern.compile("([\r\n]{3} ? ?|^| )" + "(\\(([A-Za-z]{1,4}"
			+ "|[xvil]{1,7})\\)|([A-Z]{1,2}|[a-z]{1,2})\\." + "|\\([\\d]{1,3}\\)|[\\d]{1,3}\\.) ");

	public static Pattern patterni = Pattern
			.compile("(?sm)(?<=[\r\n]{1}[ ]{1,50} ?-?)[ixv]{1,4}(?= ?-?[ ]{0,50}[\\r\\n]{1})");
	public static Pattern patterniPgNo = Pattern.compile("[ixv]{1,4}");

	public static Pattern patternPgNoSimple = Pattern
			.compile("(?sm)(?<=[\r\n]{1}[ ]{1,50}-? ?)[\\d]{1,3}(?= ?-?[ ]{0,50}[\r\n]{1})");
	public static Pattern patternPgNoSimplePgNo = Pattern.compile("(?sm)[\\d]{1,3}");

	public static Pattern patternA1 = Pattern
			.compile("(?sm)(?<=[\r\n]{1}[ ]{1,50}-? ?)" + "[A-Z]{1,2}[a-z]{0,2}[\\d]{1,3}(?= ?-?[ ]{0,50}[\r\n]{1})");
	public static Pattern patternA1PgNo = Pattern.compile("(?<=[A-Z]{1,2}[a-z]{0,2})[\\d]{1,3}");
	public static Pattern patternA1Ltr = Pattern.compile("[A-Z]{1,2}[a-z]{0,2}(?=[\\d]{1,3})");
	// <==A1, A2, A3=page 1,2,3 of exh A
	public static Pattern patternADash1 = Pattern
			.compile("(?sm)(?<=[\r\n]{1}[ ]{1,50}-? ?)" + "[A-Z]{1,2}[a-z]{0,2}-[\\d]{1,3}(?= ?-?[ ]{0,50}[\r\n]{1})");
	public static Pattern patternADash1PgNo = Pattern.compile("(?<=[A-Z]{1,2}[a-z]{0,2}{1}-)[\\d]{1,3}");
	public static Pattern patternADash1Ltr = Pattern.compile("[A-Z]{1,2}[a-z]{0,2}(?=-[\\d]{1,3})");
	// <==A-1, A-2, A-3=page 1,2,3 of exh A
	public static Pattern patternADash1Dash1 = Pattern.compile("(?sm)(?<=[\r\n]{1}[ ]{1,50}-? ?)"
			+ "[A-Z]{1,2}[a-z]{0,2}-[\\d]{1,2}-[\\d]{1,3}(?= ?-?[ ]{0,50}[\r\n]{1})");
	public static Pattern patternADash1Dash1PgNo = Pattern.compile("(?<=[A-Z]{1,2}[a-z]{0,2}-[\\d]{1,2}-)[\\d]{1,3}");
	public static Pattern patternADash1Dash1Ltr = Pattern.compile("[A-Z]{1,2}[a-z]{0,2}-[\\d]{1,2}(?=-[\\d]{1,3})");
	// <==A-1-2, A-1-2, A-1-3=page 1,2,3 of exh A-1

	public static Pattern patternPageNumbers = Pattern.compile(
			// have to go in order of longest to shortest page number type - B-1-2 before
			// B-2
			patternADash1Dash1PgNo.pattern()// B-1-2 (pgno=2)
					+ "|" + patternADash1PgNo.pattern() + "|" + patternA1PgNo.pattern() + "|"
					+ patternPgNoSimplePgNo.pattern() + "|" + patterniPgNo.pattern());

	public static Pattern patternPageLtrs = Pattern.compile(
			// have to go in order of longest to shortest page number type - B-1-2 before
			// B-2
			patternADash1Dash1Ltr.pattern()// B-1-2 (pgno=2)
					+ "|" + patternADash1Ltr.pattern() + "|" + patternA1Ltr.pattern());

	public static Pattern patternExhibitPref = Pattern
			.compile("(Exhibit |EXHIBIT |APPENDIX |Appendix |SCHEDULE |Schedule |Annex |ANNEX )"
					+ ".{0,4}([A-Ziabc\\d-\\.]{1,6}[\r\n]{0,3}.{8,250})" + "");

	public static Pattern patternExhibitPrefix = Pattern
			.compile("(?<=\r\n" + ")(Exhibit |EXHIBIT |APPENDIX |Appendix |SCHEDULE |Schedule |Annex " + "|ANNEX )("
					+ "([A-Z]{1}-[\\d]{1,2}|[A-Z]{2}|\\d)[A-Z]{0,1}" + "(?=\r\n)" + ")");// don't chg

	public static Pattern patternExhibitPrefixAllCaps = Pattern.compile("(?<=\r\n" + ")(EXHIBIT |APPENDIX |SCHEDULE "
			+ "|ANNEX )(" + "([A-Z]{1}-[\\d]{1,2}|[A-Z]{1,2}|\\d)[A-Z]{0,1}" + "(?=\r\n)" + ")");// don't chg

	public static Pattern Sch1Pattern = Pattern.compile("(\\b.*([ ]{2,30}|\t)\\b)(?!.*?(,\\d{3}))");

	public static Pattern isItTableSentencePattern = Pattern.compile("(\\b.*([ ]{2,30}|\t)\\b)(?!.*?(,\\d{3}))");

	public static Pattern patternColHdgFunky = Pattern.compile(
			"(?i)(annuity|authori|closed|machine|mbc|adjustment|product|engine|cumulat|develop|eliminat|guarant|health|histor"
					+ "|inception|investment|joint|mortgag|other|parent|portfolio"
					+ "|predecesor|previous|reorg|segments?|subsidiar|transition|under|wholly|operat)");

	public static Pattern patternCoIncLtdLLC = Pattern
			.compile(" COMPAN[IESY]{1,3}| CO[\\. $]{1}| LTD[\\. $]{1}| LIMITED| INC[\\. $]{1}| INCORPORATED"
					+ "| SUBSIDIAR[IESY]{1,3}| CORP[\\. $]{1}| CORPORATION|"
					+ " Compan[iesy]{1,3}| Co[\\. $]{1}| Ltd[\\. $]{1}| Limited| Inc[\\. $]{1}| Incorporated| Subsidiar[iesy]{1,3}"
					+ "| Corp[\\. $]{1}| Corporation|"
					+ " L\\.?L\\.?P| L\\.?P[\\. $]{1}|L\\.?L\\.?C[\\. $]{1}| P\\.?L\\.?C[\\. $]{1}");

	public static Pattern patternInception = Pattern.compile(
			"(?i)succesor|predecesor|\\(?commencement( of)?|guarant|(\\(?date )?(of )?inception\\)?( to)?|transition"
					+ "|cumulative ?(since)?|develop|histor|reorg|cumulat|joint|wholly|explorat|developm");

	public static Pattern Sch2Pattern = Pattern.compile("[ |\t]{30,100}\\p{ASCII}(?!.*?(\\<|\\>|,\\d{3})).*");

	public static Pattern SimpleColumnHeadingPattern = Pattern
			.compile(Sch1Pattern.pattern() + "|" + Sch2Pattern.pattern());

	public static Pattern lessThan1000Pattern = Pattern.compile("[ \t]{1}\\d{3}[ \t]{1}");

	public static Pattern monthSimplePattern = Pattern
			.compile("(Jan[\\. -]{1,3}[\\d]{1,2}|Feb[\\. -]{1,3}[\\d]{1,2}|Mar[\\. -]{1,3}[\\d]{1,2}"
					+ "|Apr[\\. -]{1,3}[\\d]{1,2}|May[\\. -]{1,3}[\\d]{1,2}|Jun[\\. -]{1,3}[\\d]{1,2}|Jul[\\. -]{1,3}[\\d]{1,2}"
					+ "|Aug[\\. -]{1,3}[\\d]{1,2}|Sep[\\. -]{1,3}[\\d]{1,2}|Oct[\\. -]{1,3}[\\d]{1,2}|Nov[\\. -]{1,3}[\\d]{1,2}"
					+ "|Dec[\\. -]{1,3}[\\d]{1,2}|JAN[\\. -]{1,3}[\\d]{1,2}|FEB[\\. -]{1,3}[\\d]{1,2}|MAR[\\. -]{1,3}[\\d]{1,2}"
					+ "|APR[\\. -]{1,3}[\\d]{1,2}|MAY[\\. -]{1,3}[\\d]{1,2}|JUN[\\. -]{1,3}[\\d]{1,2}|JUL[\\. -]{1,3}[\\d]{1,2}"
					+ "|AUG[\\. -]{1,3}[\\d]{1,2}|SEP[\\. -]{1,3}[\\d]{1,2}|OCT[\\. -]{1,3}[\\d]{1,2}|NOV[\\. -]{1,3}[\\d]{1,2}"
					+ "|DEC[\\. -]{1,3}[\\d]{1,2}"
					+ "|January[ -]{1,3}[\\d]{1,2}|February[ -]{1,3}[\\d]{1,2}|March[ -]{1,3}[\\d]{1,2}|April[ -]{1,3}[\\d]{1,2}"
					+ "|May[ -]{1,3}[\\d]{1,2}|June[ -]{1,3}[\\d]{1,2}|July[ -]{1,3}[\\d]{1,2}|August[ -]{1,3}[\\d]{1,2}"
					+ "|September[ -]{1,3}[\\d]{1,2}|October[ -]{1,3}[\\d]{1,2}|November[ -]{1,3}[\\d]{1,2}|December[ -]{1,3}[\\d]{1,2}"
					+ "|JANUARY[ -]{1,3}[\\d]{1,2}|FEBRUARY[ -]{1,3}[\\d]{1,2}|MARCH[ -]{1,3}[\\d]{1,2}|APRIL[ -]{1,3}[\\d]{1,2}"
					+ "|MAY[ -]{1,3}[\\d]{1,2}|JUNE[ -]{1,3}[\\d]{1,2}|JULY[ -]{1,3}[\\d]{1,2}|AUGUST[ -]{1,3}[\\d]{1,2}"
					+ "|SEPTEMBER[ -]{1,3}[\\d]{1,2}|OCTOBER[ -]{1,3}[\\d]{1,2}|NOVEMBER[ -]{1,3}[\\d]{1,2}|DECEMBER[ -]{1,3}[\\d]{1,2})");

	public static Pattern yearSimplePattern = Pattern
			.compile("(?<=[ >\r\n])[1,2]{1}[09]{1}([0-9]{2}$|[0-9]{2}(?=[, \r\n<]){1,3})");

	public static Pattern yPattern = Pattern.compile("[1,2]{1}[09]{1}[\\d]{2}");

	public static Pattern mPattern = Pattern
			.compile("(?ism)(Jan[\\.\\! -]{1}|Feb[\\.\\! -]{1}|Mar[\\.\\! -]{1}|Apr[\\.\\! -]{1}"
					+ "|May[\\! -]{1}(?!(not|Dep|Limit))|Jun[\\.\\! -]{1}" + "|Jul[\\.\\! -]{1}|Aug[\\.\\! -]{1}"
					+ "|Sep[\\.\\! -]{1}|Sept[\\.\\! -]{1}|September|Oct[\\.\\! -]{1}|Nov[\\.\\! -]{1}"
					+ "|Dec[\\.\\! -]{1}|January|February|March|April"
					+ "|May (?!([Nn]{1}ot|Dep|Limit))|June|July|August|September|October|November|December)");

	public static Pattern dPattern = Pattern
			.compile("(?i)(?<=(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|June|July"
					+ "|August|September|October|November|December)\\.? )[\\d]{1,2}(?=(,|$|\t|\r| |\\!))");

	public static Pattern periodSimplePattern = Pattern.compile(
			"(?i)(Week|Wk|Month|Years ?|end(ed|ing)|Fiscal|Quarter|Qtr|First|Second|Third|Fourth|1st|2nd|3rd|4th"
					+ "|three|six |nine|twelve|thirteen|fourteen|twenty|thirty|12|13|14|25|26|27|38|39|40|51|52|53)");

	public static Pattern periodSimpleTableSentencePattern = Pattern
			.compile("(?i)(Week|Wk|Month|Years ?|Fiscal|Quarter|Qtr|First|Second|Third|Fourth|1st|2nd|3rd|4th"
					+ "|three|six |nine|twelve|thirteen|fourteen|twenty|thirty|12|13|14|25|26|27|38|39|40|51|52|53)");

	public static Pattern RestatedPattern = Pattern.compile(
			"Restated|RESTATED|Revised|Restated|Adjust|ADJUST|PRO.{1,2}FORMA|Pro.{1,2}Forma|Reported|REPORTED");

	public static Pattern MonthDayYearPattern = Pattern.compile("[\\d]{1,2}[\\/-]{1}[\\d]{1,2}[\\/-]{1}[\\d]{2,4}");

	public static Pattern YearMoEndedPattern = Pattern.compile(
			monthSimplePattern.pattern() + "|" + yearSimplePattern.pattern() + "|" + periodSimplePattern.pattern() + "|"
					+ RestatedPattern.pattern() + "|" + yearSimplePattern.pattern());

	public static Pattern htmlPara = Pattern
			.compile("(?i)" + "(<p[^>]*>|</p[^>]*>|" + "<div[^>]*>|</div[^>]*>|" + "<BR>)");

	public static Pattern hardReturnTabNumberTabNumber = Pattern
			.compile("[\r\n]{1}[ ]{0,2}\t(?=(\\$?([\\.\\d,]{1,15}|\\d{1,3})[,\\d{3}]{0,15}\t\\$?"
					+ "([\\.\\d,]{1,15}|\\d{1,3})[,\\d{3}]{0,15}))");

	public static Pattern patterDataColumns = Pattern.compile("" +
	// #,### followed by WSs #,### or # at end of line
			"\\d,\\d\\d\\d ?\\)?([ ]{6,}|\t|[\t ]{2,})[\\( \\$]{0,5}\\d|" +
			// WSs # WSs #
			"([ ]{6,}|\t|[\t ]{2,})\\(?\\$? ?\\d\\d?\\d? ?\\)?([ ]{2,}|\t|[\t ]{2,})\\(?\\$? ?([\\d]{1,3},\\d|\\d\\d?\\d?$)|"
			+
			// 12 plus WSs (or at least 2 tabs) ### not followed by a
			// number (not year
			"([ ]{12,}|\t{2,}|[\t ]{3,})[\\( \\$]{0,5}\\d\\d\\d(?<!\\d)|" +
			// WSs #.# WSs #.#
			"([ ]{6,}|\t|[\t ]{2,})\\(? ?\\d\\d?\\d?\\.\\d\\d? ?\\)?([ ]{2,}|\t|[\t ]{2,})\\(? ?\\d\\d?\\d?\\.\\d|" +
			// WS # WSs # WSs # (1 to 2 #s in each case else above picks
			// up
			"([ ]{6,}|\t|[\t ]{2,})\\(? ?\\d\\d? ?\\)?([ ]{3,}|\t|[\t ]{2,})\\(? ?\\d\\d? ?\\)?([ ]{3,}|\t|[\t ]{2,})\\(? ?\\d\\d? ?\\)?|"
			+ "([ ]{6,}|\t|[\t ]{2,})\\(? ?[\\d]{3}\\)?([ ]{6,}|\t|[\t ]{2,})\\(? ?[\\d]{3}\\)? ?[\r\n]{1}|"
			// WS #,### WS -
			+ "([ ]{6,}|\t|[\t ]{2,})\\(? ?\\d?\\d?\\d?,?\\d?\\d?\\d([ ]{3,}|\t|[\t ]{2,})- ? ? ?[\r\n]{1}"
			// ws -|# ws -|# ws -|#
			+ "|([ ]{6,}|\t|[\t ]{2,})(-|[\\(\\)\\d,]{4,10})([ ]{6,}|\t|[\t ]{2,})(-|[\\(\\)\\d,]{4,10})([ ]{6,}|\t|[\t ]{2,})(-|[\\(\\)\\d,]{4,10})([ ]{6,}|\t|[\t ]{2,}|[\r\n]{1})");

	public static Pattern numberBR = Pattern.compile("(?<=(\t ?\\$?\\d{1,3}|,[\\d]{3}|\\d\\)|[-]{1,3}))(?i) ?\\<BR\\>");

	public static Pattern TRPattern = Pattern.compile("(?i)(<tr[^>]*>|</tr[^>]*>)");

	public static Pattern TDPattern = Pattern.compile("(?i)(<td[^>]*>|<th[^>]*>)");

	public static Pattern TDWithColspanPattern = Pattern
			.compile("(?i)(<td[^>]*?COLSPAN[^>]*?>|<th[^>]*?COLSPAN[^>]*?>)");

	public static Pattern ExtraHtmlPattern = Pattern.compile("(?i)(</?[[font]|[b]|[i]|[u]]*[^>]*>)");

	public static Pattern ExtraHtmlPattern1 = Pattern.compile("(?i)(</?(!</?TABLE)[[font]|[b]|[i]|[u]]*[^>]*>)");

	public static Pattern ExtraHtmlPattern2 = Pattern.compile("(?i)(</?p[^>]*>|</td[^>]*>)");
	// - </ (once or not at all - should it be at least once? followed by any of
	// . . . . ending with stuff after *....

	public static final String LineSeparator = System.getProperty("line.separator", "\r\n");

	public Pattern removeHtmlFromPartialHtmlTextExtract = Pattern.compile(".*[^<]>");
	// only works where partial text was extracted from html and after
	// stripHtmlTabs is run. will remove remainder such as nt-family: Arial,
	// Helvetica">

	public Pattern DecimalPattern = Pattern
			.compile("(?i)[ \t]*((in |\\$|\\()thousands?|[\\( ]{0,1}000[\\' \\)]{0,2}s|in millions?|million[s] of"
					+ "|\\(.{1,2}000 omit|in billions?|billion[s] of)[ \t]*");

	public static Pattern patternStemmer = Pattern
			.compile("(?i)(ication|dictory|ility|atory|edly|ably|rian|dict|ied|ing|ity|ed|ly|y)$");

	// public Pattern patternContractSentenceStart = Pattern.compile("(?sm)(" +
	// "(?<=([\r\n\t]|[,;] ?[\r\n]|\\. ))[A-Z]{1,}"
	// +
	// "|\\([a-zA-Z\\d]{1,3}\\(([\t]| )" +
	// ")");
	public static Pattern patternTocPageNumber = Pattern.compile("[ \\.\t]{5,}([ABCDEFG]{1}-?)?[\\d]{1,2} ?(\r|\n|$)");

	public static Pattern patternExhName = Pattern
			.compile("(?sm)[\r\n \t]{0,100}.*?[A-Z][\\p{Alnum}\\p{Punct} ]" + "{1,300}\r\n");

	public static Pattern patternContractSentenceStart = Pattern
			.compile("(?sm)([\r\n] ?(?<=[A-Z\\(])|(?<=\\.)[ \r\n])|(?<=\\:) ?[\r\n]");

	public static Pattern patternContractSentenceEnd = Pattern
			.compile("(?sm)(\\!|\\?|[a-zA-Z\\)]{1}\\.[\r\n ](?![a-z])|([,;:] ?| and ?)[\r\n]|[\r\n])");

	// simple Hellow word. It is Mr. Rogers.
	// Two ALL CAPS or lower caps or 4 any caps followed by ) or ] or " or \\d
	// then period (end of sent). then ....

	public static Pattern patternSent1a = Pattern.compile("(?sm)" + "(" +

			"(" +

			"([A-Z]{2}|[a-z]{2}|[A-Za-z]{4})" + "[\\)\\]\"]{0,2}" + "| [\\d]{2}" +

			")" +

			"\\." + "(?=[ ]{1,20}([A-Z]{1}|[\"\\(\\[]))" +

			")"
	// grp2
	/*
	 * +"|((\\.[\\]\\)]{0,1}" + "(?= ?[\r\n]{3,}[ ]{0,15}\"?[\\(\\dA-Za-z]{1})" +
	 * "))"
	 */
	);

	public static Pattern patternSent1b = Pattern
			.compile("(?sm)(\\.|;|:)[\\]\\)]{0,1}(?= ?[\r\n]{3,}[ ]{0,15}\"?[\\(\\dA-Za-z]{1})");

	public static Pattern patternSent1c = Pattern.compile("(?sm)" + "(" + "[A-Z]{2}|[a-z\\)\\]]{2}" + ")"
			+ "[\\.\\:]{1}" + "(?=( ?[\r\n\t ]{1,9}\\(" + "[\\dA-Za-z]{1,4}\\)" + "))");

	// added: \\].\r\n
//	public static Pattern patternSent1c = Pattern.compile("(?sm)" + "(" + "[A-Z]{2}|[a-z\\)\\]]{2}" + ")" + "[\\.\\:]{1}"
//	+ "(?="
//	+ "("
//	+ " ?[\r\n\t ]{1,9}[A-Z]{1}[a-z]{2}\\(" + "[\\dA-Za-z]{1,4}\\)"
//			+ "| ?[\r\n]{1,9} ? ?[A-Z]{1}[A-Za-z]{2}"//added this line 3/13
//			+ ")" 
//	+ ")"
//	)
	;

	public static Pattern patternSent2a = Pattern.compile(
			"(?sm)(" + "(" + "([A-Z]{2}|[a-z\\)\\]]{1})" + ")" + ";" + "([ ]{1,2}| and ?)?" + "(?=[\r\n]{3,})" + ")");

	public static Pattern patternSent2b = Pattern.compile("(?sm); (plus|less|minus) ?([\r\n]{3,})");

	public static Pattern patternSent3 = Pattern
			.compile("(?sm)([\\d\\.a-z]{1}\\d\\.|:|[a-z]{1}\"\\.)(?=[ \r\n\t]{1,20}[\"A-Z]{1})");

	public static Pattern patternSent4 = Pattern
			.compile("(?sm)[a-z\\)\"\\]]{2}\\.(?=[ \r\n]{5,30}[A-Z]{1}|  ?[A-Z]{1})");

	// require at least 4 hard returns in order to only require 1 cap ltr to
	// start a sent
	public static Pattern patternSent5 = Pattern.compile("(?sm)[\r\n]{3}[\r\n ]{6,20}(?=[A-Z]{1})");
	// was ==>public static Pattern patternSent5 = Pattern.compile("(?sm)[\r\n
	// ]{6,20}(?=[A-Z]{1})");

	// captures [HELLO THIS IS THE END OF SENTENCE] - has no \\. but followed by
	// multiple hard returns.
	public static Pattern patternSent6 = Pattern.compile("(?sm)[A-Z]{3,15}[ ]{1,3}[A-Z\\]\\)]{3,15} ?(?=[\r\n]{3,})");

	// at least 5 words in lower case (\b=word boundary,\w=word char
	// (a-zA-Z_0-9) followed by up to 15 chars etc and period space/hard return

	public static Pattern patternSent7a = Pattern.compile("\\b([a-z]\\w*)\\b" + "( |[\r\b]{1,5})([a-z]\\w*)\\b"
			+ "( |[\r\b]{1,5})([a-z]\\w*)\\b" + "( |[\r\b]{1,5})([a-z]\\w*)\\b" + "( |[\r\b]{1,5})([a-z]\\w*)\\b"
			+ ".{1,15}\\.(?=(  ?[A-Z\"]{1}| ? ?[\r\n]{1}))");

	// period followed by 1 to 2 spaces and and initial cap word followed by at
	// least 4 lower case words.
	public static Pattern patternSent7b = Pattern.compile("\\. (" + "?=( ?\\b([A-Z]{1}[a-z]\\w*)\\b"
			+ "( |[\r\b]{1,5})([a-z]\\w*)\\b" + "( |[\r\b]{1,5})([a-z]\\w*)\\b" + "( |[\r\b]{1,5})([a-z]\\w*)\\b"
			+ "( |[\r\b]{1,5})([a-z]\\w*)\\b" + "))");

	public static Pattern patternSent8 = Pattern.compile("(" + "( |[\r\n])[a-z\",;]{1,11})"
			+ "(( |[\r\n])[a-z\",;]{1,11})" + "(( |[\r\n])[a-z\";,]{1,11})" + "(( |[\r\n])[a-z\";,]).{1,11}"
			+ "(( |[\r\n])[A-Za-z\"\\d,;]{1,11})" + "(( |[\r\n])[A-Za-z\"\\d,;]{1,11})?"
			+ "(( |[\r\n])[A-Za-z\"\\d,;]{1,11})?" + "\\.\"?(?=(  ?[A-Z\"]{1}| ? ?[\r\n]{1}))");
	// notice or both would be, an Event of Default."
	// not picking up this pattern as sentence end: in reliance on Rule 144A.
	// PICKUP HERE! See how to use word boundaries - I need to see 5 to 6 words
	// prior of which 4 are lower case.

	// public static Pattern patternSent9 =
	// Pattern.compile("(?sm)([A-Z]{2}|[a-z]{2})\\.(?=[ ]{0,}[$\r\n]{1,}[
	// ]+[A-Z])");

	public static Pattern patternSent9 = Pattern.compile("(?sm)(([A-Z]{2}|[a-z]{2})\\. ?(?=[\r\n]{1,}[ ]+[A-Z]))");

	// [\r\n ]{1}this[\r\n ]{1}
	public static Pattern patternSent10 = Pattern.compile("(?sm)(Section\\.(?= ? ?[\r\n]{1}))");
	// this is used for section cleanup method that is now fallow

	public static Pattern patternSent11 = Pattern.compile("(?sm)" + "([A-Z\\&\\(\\)\\[\\]\\d]{1,50}( |[\r\n;,]{0,3}))"
			+ "[A-Z\\&\\(\\)\\[\\]\\d]{2,50}\\.\"?  ?" + "(" + "?="
			+ "([A-Z\\&\\(\\)\\[\\]\\d]{2,50}( |[\r\n;,]{0,3}))([A-Z\\&\\(\\)\\[\\]\\d]{1,50}( |[\r\n;,]{0,3})))");

//	public static Pattern patternClau = Pattern.compile("(?sm); (and )?(?=\\()");<==this is for getClau

	public static Pattern patternSentenceEnd = Pattern
			.compile(patternSent1a.pattern() + "|" + patternSent1b.pattern() + "|" + patternSent1c.pattern() + "|"
					+ patternSent2a.pattern() + "|" + patternSent2b.pattern() + "|" + patternSent3.pattern() + "|"
					+ patternSent4.pattern() + "|" + patternSent5.pattern() + "|" + patternSent6.pattern() + "|"
					+ patternSent7a.pattern() + "|" + patternSent7b.pattern() + "|" + patternSent8.pattern() + "|"
					+ patternSent9.pattern() + "|" + patternSent11.pattern() + "|" + ContractParser.patternExhibitToc);

	public static Pattern patternParaReference = Pattern
			.compile("(this|preceding|previous).{1,3}(clause|[Ss]ection" + "|[Pp]aragraph|[Aa]rticle)");

	public static Pattern patternParaMarkerEnd = Pattern
			.compile("(?i)([\\:\\;\\,]{1})( and| or)?$(?! ?[\\p{Alnum}\\p{Punct}])");
	// para marker will ignore patterns w/n para.

	public static Pattern patternParaMarkerParent = Pattern.compile("(?i)(:) ?$(?! ?[\\p{Alnum}\\p{Punct}])");
	// para marker will ignore patterns w/n para.

	public static Pattern patternParaNumberMarker = Pattern
			.compile("(\\([A-Za-z\\.\\d]{1,5}\\)|[\\dA-Z]{4}(\\.|[ ]{3}))");

	public Pattern patternAnyVisualCharacter = Pattern.compile("[\\p{Alnum}\\p{Punct}]");

	// finds a word
	public static Pattern patternWord = Pattern
			.compile("(?<=\r|\b| |^|\\[\\{\\()[\\p{Alnum}\\p{Punct}].*?(?=$|\r|\b|\\.|\\!|\\?| |;|:|\\,|\\]|\\}|\\))");

	public Pattern patternFoundLowerCaseWordStart = Pattern.compile("(\\b[^A-Z\\)\\( ;:])");

	public static Pattern patternIsExpense = Pattern
			.compile("(?i)(?<!(before).{1,50})(expense|tax|charge|cost|depreciat|amortizat"
					+ "|minority|salar|(selling.{1,3}general.{1,7}administrative))");

	public static Pattern patternIsLoanLoss = Pattern.compile("(?i)(?<!(before|after|net of).{1,50})(loan.{1,15}loss)");
	// TODO: ck if any other distinct rowname types to treat on an exception
	// basis (such as before - maybe see if 'after' is used w/ loan loss). Then
	// if 'patternIsLoanLoss' is present - treat as a plus or minus - by having
	// subAlt2 -- only call subAlt2 if loan loss is present.

	public static Pattern patternIsIncomeOrLoss = Pattern.compile("(?i)((income|earning).{1,4}\\(loss)");

	public static Pattern patternIsResearch = Pattern.compile("(?i)(research)");
	// research in BS is typically an asset (accrued), in CF it is added bac and
	// in IS it is an expense

	public static Pattern patternIsIncomeSales = Pattern
			.compile("(?i)(?<!(Cost).{1,20})(sales|income|revenue|earn)(?!(.{1,5}tax))"
					+ "|(?i)(?<!(Cost).{1,20})(earn|income|sales|revenue).{1,15}before");

	public static Pattern patternTableSentenceEndedMoYear = Pattern
			.compile("(?ism)((one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|years?).{1,2})?"
					+ "((and.{1,2})?(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|years?)"
					+ ".{1,2})?(Months?.{1,2})?(Periods?.{1,2})?(end[eding]{2,3}).{1,2}"
					+ "(January|February|March|April|May|June|July|August|September|October|November|December)"
					+ ".{1,2}[0-9]{1,2}(.{1,3}[0-9]{4})?((.{0,2}[and,]{1,3}.{1,2}[0-9]{4})+)?");

	public static Pattern patternAnyEnded = Pattern.compile("weeks?|wk|months?|days|years?|mo\\.|mos\\.?|period");

	public static Pattern patternTableSentenceMonthDay = Pattern.compile(
			"(?i)(January|February|March|April|May|June|July|August|September|October|November|December).{1,2}[0-9]{1,2}");

	public String getDataColumnValue(String value) throws ParseException {

		double amt = SubTotalFinder.parseToAmount(value.trim().replaceAll("[^\\x20-\\x7e]", ""));
		String amtS;
		if (amt == 3.3333) {
			amtS = "null";
		} else {
			amtS = amt + "";
		}
		return amtS;
	}

	public static String getTextBeforeTableName(String text, String companyName) {

		NLP nlp = new NLP();

		String[] lines = text.split("[\r\n]");
		String line = "", str = "";
		for (int i = 0; i < lines.length; i++) {
			line = lines[i];
			if (nlp.getAllIndexEndLocations(line, TableParser.TableNamePattern).size() > 0) {
				// System.Xut.println("line matched=="+line);
				str = line.substring(0,
						nlp.getAllStartAndEndIdxLocations(line, TableParser.TableNamePattern).get(0)[0]);
				// System.Xut.println("found line before tablename and it equals==="+str);
			}
		}

		Pattern patternCompanyNameAndCorp = Pattern
				.compile("(" + companyName + ")" + "|" + patternCoIncLtdLLC.pattern());

		int eIdx = 0;
		Matcher matchCompanyNameAndCorp = patternCompanyNameAndCorp.matcher(str);

		while (matchCompanyNameAndCorp.find()) {
			// System.Xut.println("matchCompanyNameAndCorp="+matchCompanyNameAndCorp.group());
			eIdx = matchCompanyNameAndCorp.end();
		}
		str = str.substring(eIdx);
		// System.Xut.println("getTextBeforeTableName==" + str);
		return str;
	}

	public static boolean validateTablename(String textAfterTableName, String companyName,
			String outSideTableTextHtmlStripped) {

		// add boolean vaue to designate if html or txt. If html - get last
		// instance of tablename in that cell (is that what textAfterTableName
		// is equal to?)

		// if there is more than 25 characters of text before tablename then it
		// is not valid.

		outSideTableTextHtmlStripped = outSideTableTextHtmlStripped.replaceAll(
				"(?ism)Item.{0,4}8.{0,5}FINANCIAL.{0,5}STATEMENTS?.{0,5}(AND.{0,5})?(SUPPLEMENTARY.{0,5})?(DATA)?|"
						+ "PART.{0,4}I.{0,5}FINANCIAL.{0,5}INFORMATION|(u?n?audited)"
						+ "|ITEM.{0,5}1.{0,5}(CONDENSED.{0,5})?(CONSOLIDATED.{0,5})?FINANCIAL.{0,5}STATEMENTS?|[ \\-_=]{1,}| of",
				" ");
		// needs to be replaced with a ws else all one glob and if I try to run
		// getTextBeforeTableName none of the matchers work.

		String textBeforeTableName = NLP.getTextBeforeTableName(outSideTableTextHtmlStripped, companyName);

		// NLP.pwNLP
		// .append(NLP.println("if lenght>20 - returns false and tablename not
		// validated. textBeforeTablename.len",
		// textBeforeTableName.length() + "\rtextBeforeTableName==" +
		// textBeforeTableName));

		if (textBeforeTableName.replaceAll("CONDENSED|CONSOLIDATED|COMBINED", "").length() > 20)
			return false;

		boolean validatedTablename = false;

		// sees if tablenames found outside table are valid. May also work for
		// ascii filings- check text after tablename endIdx
		// this replacement should generally be the same as parseTableSentence
		// method.
		// with Comparative Amounts to
		// |\\(|\\) ---- ADD BACK
		// (STAGE ?)?(activities ?|PERIOD ?)?
		String replCleanupAftTn = "(?ism)\\(|\\)|Note.{0,3}|;|:|(in)? ?Canadian|\\[\\]|US\\$000\\'?s|UNITED STATES DOLLARS|GAAP|in millions of US\\$"
				+ "|(With ?)?Comparative ?(amounts? )?(to|for)? ?|(\\$ )?in millions|Part ?I?|Financial( Information)|Item ?1?"
				+ "|(In |All )?((u.s. ?)?Dollars? ?(and shares? )?(amounts? )?|Millions?,?|Thousands?( of)?|Hundreds?)|as of ?|Prepared by Management"
				+ "|(of ?|AND ?|THE ?)?DEVELOPMENT ?|STAGE ?|ACTIVITIES ?|SINCE ?|Dollar amounts? ?|\\/ ?|for each of the ?|Figures? for ?"
				+ "|(january|february|march|april|may|june|july|august|september|october|november|december)? ?([0-9]{1,2}\\,)? [12]{1}[09]{1}[0-9]{2}"
				+ "|(shares?|stock)holders.{1,4}Equity|notes|percent(ages?)? ?|continued ?|DEFICIENCY|TRANSITION|unassigned deficit"
				+ "|(Other )?Comprehensive (Income|loss|earnings?)|(AND )?RETAINED EARNINGS|CONDENSED"
				+ "| ?EXCEPT (per )?(units?|NUMBER( OF)?|COMMON) ?(SHARES?)? ?(DATA)?| ?except (net income |EARNINGS )?(LOSSE?S?S? ?)?(amounts )?per "
				+ "(units? |common |ordinary )?|(INTEREST )?(AND )?(DIVIDEND )?INCOME:? ?|respectively ?"
				+ "|(shares?|units?) ?(data ?)?|Historical( Cost)?"
				+ "|INCREASE ?|DECREASE ?| IN CASH AND CASH EQUIVALENTS"
				+ "|Expressed (in )?|except parenthetical|amounts?"
				+ "|,? ?(omitted )?Except (for )?(net )?(Earnings )?(per[- ]{1}|for )?(ordinary |treasury )?Shares?( Amounts?)?"
				+ "( and| per( ordinary)? shares?| related| amounts?| data)?"
				+ "|per( ordinary)? shares? related data|PER (ordinary )?shares?|Omitted|previous page"
				+ "|(and )?( ?per)?( ordinary)?([- ]{1})?(units? |shares? data ?|information ?)"
				+ "|,? ?except par[- ]{1}value ?(and number of (shares?|units?) ?)?"
				+ "|,? ?except (earnings? )?(per[- ]{1})?(ordinary )?(shares? |units? )(amounts)?|shares? data"
				+ "|(All )?(numbers|figures?) ?|are presented ?|as adjusted" + "|supplemental disclosure"
				+ "|(in )?accordance with U\\.?S\\.? generally accepted account[ing]{0,3}( princi.{1,4})?"
				+ "|Stated in thousands of US dollars( except shares issued and outstanding)?"
				+ "|Stated in thousands of US dollars except per shares? amounts" + "|STATED IN"
				+ "|(,? except (amounts )?(for|Per)?[- ]{1}?(ordinary )?(units?|shares?)? ?)( ?and per[- ]{1}?)?(ordinary )?(shares?|units?) (Data|amounts?|figures?)?"
				+ "|(and )?Expenses?|A?S? ?RESTATED" + "|[-]+|[_]+|[\t]+| ?xx ?"
				+ "| of (u.s. )?dollars|^or the|par[- ]{1}value ?|amounts ?|which are reflected in (millions?|thousands)"
				+ "|(IN )?000\\'?s|</div>|</p>|rmb ?|us\\$ ?"
				+ "| ?u? ?n? ?a ?u ?d ?i ?t ?e ?d ?|Series [0-9]|as (of|at )|DATE (OF )?(OPERATIONS? ?|FORMATION ?|INCORPORATION ?)(COMMENCED)?"
				+ "|REORGANIZED ?|PREDECESSOR ?|SUCCESSOR ?|COMPANY|Predecessor ?|Company ?|(in)?corporation"
				+ "|(AND )?DEFICIT ACCUMULATED|(and )?accumulated deficit|DURING THE |EXPLORATION STAGE|CUMULATIVE ?"
				+ "|(Statutory|Going Concern) ?(Basis)?|results? |(from |date of )?inception ?| ?ASSETS ?| ?Loss |first ?|second ?|third ?|fourth ?"
				+ "|January|February|March|April|May|June|July|August|September|October|November|December|Jan[\\. ]{0,1}|Feb[\\. ]{0,1}|Mar[\\. ]{0,1}|Apr[\\. ]{0,1}"
				+ "|Jun[\\. ]{0,1} ]{0,1}|Jul[\\. ]{0,1}|Aug[\\. ]{0,1}|Sep[\\. ]{0,1}|Sept[\\. ]{0,1}|Oct[\\. ]{0,1}|Nov[\\. ]{0,1}"
				+ "|Dec[\\. ]{0,1}|\\d\\d?,|[12]{1}[09]{1}[0-9]{2}|Or Unless Otherwise|BDC|prior|outstanding"
				+ "|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|twenty|thirty|years?|quarters?|fifty"
				+ "|12|13|14|15|23|24|25|26|27|28|37|38|39|40|41|50|51|52|53|54|[\\d]{1,4}|-"
				+ "|fiscal|months?|mo |periods?|weeks?|\\/|wk|end[eding]{2,3}|inc\\.|corp\\.|subsidiar[iesy]{1,3}|Interim ?"
				+ "|AND | AND|FOR |THE | to |from | from|through |ASSETS? ?|LIABILITIES ?|(STOCK|SHARE)HOLDER\\'?S?\\'|EQUITY ?|PARTNERS ?|CAPITAL ?"
				+ "|REVENUES? ?|SALES ?|(SPLIT TABLE)|Continued|shares? ?| a s s e t s|per |common ?|OPERATIONS?|except |Comprehensive|other than ?"
				+ "|[_]+|<TABLE>|<.*[^>](>|$)|\r|\n|<BR>|</BR>|<B>|</B>|<U>|</U>|<PRE>|(Dollars )?in thousands|in millions?|deficit|[ ]+";

		textAfterTableName = textAfterTableName.replaceAll("[\n\r]{1,}", "\r");
		// System.Xut.println("textAfterTableName rt before split by line="
		// + textAfterTableName);
		String[] lines = textAfterTableName.split("\r");
		String line = "";
		textAfterTableName = "";

		int eIdx = 0;
		for (int i = 0; i < lines.length; i++) {
			line = lines[i];

			// company name pattern of Co/Inc etc skip
			Matcher matchCoIncLtdLLC = patternCoIncLtdLLC.matcher(line);
			// System.Xut.println("match CoIncLtdLLC pattern against this line="+line);
			eIdx = 0;
			while (matchCoIncLtdLLC.find()) {
				eIdx = matchCoIncLtdLLC.end();
				// System.Xut.println(matchCoIncLtdLLC.group());
//				NLP.pwNLP.append(NLP.println("matchCoIncLtdLLC=", matchCoIncLtdLLC.group()));
			}
//			NLP.pwNLP.append(NLP.println("line.substring(eIdx)=", line.substring(eIdx)));

			if ((line.toLowerCase().contains(companyName.toLowerCase()) && companyName.length() > 3)
					|| line.substring(eIdx).length() < 4 || line.replaceAll("[ ]{3,}", "").length() < 4) {
				// System.Xut
				// .println("continuing past line - not including it as I reconstitute
				// textAfterTableName. line="
				// + line);
				continue;
			} else {
				textAfterTableName = textAfterTableName + line + "\r";
				// System.Xut.println("adding line to textAfterTableName. line="+line);
			}

			// System.Xut.println("reconstituting line-by-line
			// textAfterTableName="+textAfterTableName);
		}

		// System.Xut.println("reconstituted textAfterTableName="
		// + textAfterTableName);

		// System.Xut.println("groups of replace
		// are="+textAfterTableName.replaceAll("(?ism)("
		// + replCleanupAftTn + ")", "$1"));

		// System.Xut.println("tableTextParser.companyName="+TableTextParser.companyName);
		textAfterTableName = textAfterTableName.replaceAll("[\r\n]{1,}", "")
				.replaceAll("(?ism)(" + replCleanupAftTn + ")", "").replaceAll("[, ]+", " ")
				.replaceAll("(?ism)(" + TableTextParser.companyName + ")", "").trim() + " ";

		// System.Xut.println("replacement complete --
		// textAfterTableName="+textAfterTableName);

//		NLP.pwNLP.append(NLP.println(
//
//				"IF TEXT.LENGTH>10 AFTER REPLACEMENTS - THEN TABLENAME WAS NOT VALIDATED. TEXT.LEN=",
//				textAfterTableName.length() + "\n first 75 chars of text not validated is="
//						+ textAfterTableName.substring(0, Math.min(75, textAfterTableName.length()))));
//
//		NLP.pwNLP.append(NLP.println("NLP.validatedTablename -- text after tablename=", textAfterTableName));
		if (textAfterTableName.length() < 12)
			validatedTablename = true;
		else
			validatedTablename = false;

		return validatedTablename;

	}

	public static List<String[]> getListOfStringAryFromMapOfStringWithSameConvertedKey(Map<Integer, String[]> map,
			int key) {
		List<String[]> listStrAry = new ArrayList<>();
		// convert map key is a multipe of 100 (eg., row*100+col#) and if key
		// passed through method - e.g., 1 is equal to that converted key it is
		// inserted into map

		int k = 0;
		for (Map.Entry<Integer, String[]> entry : map.entrySet()) {
			k = entry.getKey();
			if (k / 100 == key) {
				listStrAry.add(entry.getValue());
			}
			// System.Xut.println("key=="+key+" k|" + entry.getKey() + "| val|"
			// + Arrays.toString(entry.getValue()) + "|");
		}

		// System.Xut.println("returning this list - and its size="+listStrAry.size());
		return listStrAry;
	}

	public static void createTPrawTable(String table) throws SQLException, FileNotFoundException {

		String createTable = "";

		if (table.toLowerCase().contains("bac")) {

			createTable = "\r drop table if exists " + table + ";\r" + "CREATE TABLE " + table + " (\r"
					+ "  `AccNo` varchar(20) NOT NULL DEFAULT '-1',\r" + "  `fileDate` datetime DEFAULT NULL,\r"
					+ "  `cik` int(11) DEFAULT NULL,\r" + "  `tn` varchar(10) DEFAULT NULL,\r"
					+ "  `row` int(5) NOT NULL DEFAULT '-1' COMMENT 'table row',\r"
					+ "  `col` tinyint(2) DEFAULT NULL COMMENT 'data col number in financial table',\r"
					+ "  `tRow` tinyint(2) DEFAULT NULL COMMENT 'row number in financial table',\r"
					+ "  `tno` int(5) NOT NULL,\r" + "  `rowName` varchar(125) DEFAULT NULL,\r"
					+ "  `value` double(23,5) DEFAULT NULL,\r" + "  `ttl` int(4) DEFAULT NULL,\r"
					+ "  `stt` int(4) DEFAULT NULL,\r" + "  `net` int(4) DEFAULT NULL,\r"
					+ "  `sub` int(4) DEFAULT NULL,\r"
					+ "  `p1` int(3) DEFAULT NULL COMMENT 'if html - per1 parsed from cell, if txt per1 parsed based on col hdg ratio matching',\r"
					+ "  `edt1` varchar(14) DEFAULT NULL COMMENT 'same as per1',\r"
					+ "  `p2` int(3) DEFAULT NULL COMMENT 'if html - per2 based on col hdg ratio matching, if txt based on idx alignments of each match',\r"
					+ "  `edt2` varchar(14) DEFAULT NULL COMMENT ' same as per2',\r"
					+ "  `tc` tinyint(3) DEFAULT NULL COMMENT 'total number of data cols',\r"
					+ "  `tableName` varchar(255) DEFAULT '',\r"
					+ "  `coMatch` tinyint(1) DEFAULT NULL COMMENT '1 means company name is in tableheading',\r"
					+ "  `companyNameMatched` varchar(100) DEFAULT '',\r" + "  `dec` int(10) DEFAULT NULL,\r"
					+ "  `tsShort` varchar(20) DEFAULT NULL COMMENT 'Yr mo per in order found in tablesentence. This pattern can then be used to grab data in TSLong',\r"
					+ "  `ColumnText` varchar(255) DEFAULT NULL COMMENT 'shows this col #s text used for edt2. ',\r"
					+ "  `ColumnPattern` varchar(255) DEFAULT NULL,\r"
					+ "  `allColText` varchar(255) DEFAULT NULL COMMENT 'shows by Line each Column based on words being separated by two spaces.',\r"
					+ "  `ended` varchar(50) DEFAULT NULL,\r" + "  `yr` varchar(10) DEFAULT NULL,\r"
					+ "  `mo` varchar(25) DEFAULT NULL,\r"
					+ "  `htmlTxt` varchar(15) DEFAULT NULL COMMENT 'if txt it has loc end idx of far right data col, else it will say html or generic to show which parser used',\r"
					+ "  `form` varchar(15) DEFAULT NULL COMMENT 'this will equal rowratioBeforeColumnUtil if generic in htmlTxt field',\r"
					+ "  `TSlong` varchar(200) DEFAULT NULL,\r" + "  PRIMARY KEY (`AccNo`,`tno`,`row`),\r"
					+ "  KEY `edt2` (`edt2`),\r" + "  KEY `per2` (`p2`),\r" + "  KEY `AccNo` (`AccNo`),\r"
					+ "  KEY `fileDate` (`fileDate`),\r" + "  KEY `tNo` (`tno`),\r" + "  KEY `row` (`row`),\r"
					+ "  KEY `rowname` (`rowName`),\r" + "  KEY `tn` (`tn`),\r" + "  KEY `value` (`value`),\r"
					+ "  KEY `cik` (`cik`),\r" + "  KEY `col` (`col`),\r" + "  KEY `totcol` (`tc`),\r"
					+ "  KEY `columnPattern` (`ColumnPattern`),\r" + "  KEY `tRow` (`tRow`)\r"
					+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r";
			MysqlConnUtils.executeQuery(createTable);

		}

		else {

			createTable = "\r drop table if exists " + table + ";\r" + "CREATE TABLE " + table + " (\r"
					+ "  `AccNo` varchar(20) NOT NULL DEFAULT '-1',\r" + "  `fileDate` datetime DEFAULT NULL,\r"
					+ "  `cik` int(11) DEFAULT NULL,\r" + "  `tn` varchar(10) DEFAULT NULL,\r"
					+ "  `row` int(5) NOT NULL DEFAULT '-1' COMMENT 'table row',\r"
					+ "  `col` tinyint(2) DEFAULT NULL COMMENT 'data col number in financial table',\r"
					+ "  `tRow` tinyint(2) DEFAULT NULL COMMENT 'row number in financial table',\r"
					+ "  `tno` int(5) NOT NULL,\r" + "  `rowName` varchar(125) DEFAULT NULL,\r"
					+ "  `value` double(23,5) DEFAULT NULL,\r" + "  `ttl` int(4) DEFAULT NULL,\r"
					+ "  `stt` int(4) DEFAULT NULL,\r" + "  `net` int(4) DEFAULT NULL,\r"
					+ "  `sub` int(4) DEFAULT NULL,\r"
					+ "  `p1` int(3) DEFAULT NULL COMMENT 'if html - per1 parsed from cell, if txt per1 parsed based on col hdg ratio matching',\r"
					+ "  `edt1` varchar(14) DEFAULT NULL COMMENT 'same as per1',\r"
					+ "  `p2` int(3) DEFAULT NULL COMMENT 'if html - per2 based on col hdg ratio matching, if txt based on idx alignments of each match',\r"
					+ "  `edt2` varchar(14) DEFAULT NULL COMMENT ' same as per2',\r"
					+ "  `tc` tinyint(3) DEFAULT NULL COMMENT 'total number of data cols',\r"
					+ "  `tableName` varchar(255) DEFAULT '',\r"
					+ "  `coMatch` tinyint(1) DEFAULT NULL COMMENT '1 means company name is in tableheading',\r"
					+ "  `companyNameMatched` varchar(100) DEFAULT '',\r" + "  `dec` int(10) DEFAULT NULL,\r"
					+ "  `tsShort` varchar(20) DEFAULT NULL COMMENT 'Yr mo per in order found in tablesentence. This pattern can then be used to grab data in TSLong',\r"
					+ "  `ColumnText` varchar(255) DEFAULT NULL COMMENT 'shows this col #s text used for edt2. ',\r"
					+ "  `ColumnPattern` varchar(255) DEFAULT NULL,\r"
					+ "  `allColText` varchar(255) DEFAULT NULL COMMENT 'shows by Line each Column based on words being separated by two spaces.',\r"
					+ "  `ended` varchar(50) DEFAULT NULL,\r" + "  `yr` varchar(10) DEFAULT NULL,\r"
					+ "  `mo` varchar(25) DEFAULT NULL,\r"
					+ "  `htmlTxt` varchar(15) DEFAULT NULL COMMENT 'if txt it has loc end idx of far right data col, else it will say html or generic to show which parser used',\r"
					+ "  `form` varchar(15) DEFAULT NULL COMMENT 'this will equal rowratioBeforeColumnUtil if generic in htmlTxt field',\r"
					+ "  `TSlong` varchar(200) DEFAULT NULL,\r" + "  PRIMARY KEY (`AccNo`,`tno`,`row`)"
					+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\n" + "\n";
			MysqlConnUtils.executeQuery(createTable);

		}

	}

	public static List<String> getColumnHeadingAboveRowname(String text, String tnValid,
			List<String> list_TS_tsShort_tsPattern_tnValid) {
		String tableSentenceSubstitute = "";
		NLP nlp = new NLP();
		// THIS CAN ONLY BE USED WITH TABLETEXTPARSER - SEE 4WS

		// System.Xut.println("text above rowname=="+text);
		String[] textSplit = text.split("[\r\n]");
		String line = "";
		// within 0 to 5 spaces of start of line find either ended mo day or mo
		// day pattern that is followed by at least 4 ws|\t then year (eg
		// 2003 then 4 or more ws|\t
		Pattern patternAboveRownameColHdgFollowedByYearColHdg = Pattern
				.compile("(?i)^.{0,14}(for )?(the )?(period )?(quarter |three |six |nine |twelve |quarters? )?"
						+ "(fiscal )?(months? |years? )?(ended ?|ending ?)?"
						+ "(january |jan|february |feb|march |mar|april |apr|may |june |jun|july "
						+ "|jul|august |aug|september |sep|october |oct|november |nov|december |dec)"
						+ "([0-9]{1,2})?,?(?=([ ]{10,100}(\\(|[12]{1}[09]{1}[0-9]{2})))"
				// +"(?=([
				// \t]{5,}([12]{1}[09]{1}[0-9]{2}|\\(restated\\)|\\(u?n?audited\\))[
				// \t]{5,}))
				);

		// this was where rowname=May 1, 2005 and May 2, 2004
		Pattern patternAboveRownameMYMY = Pattern.compile("(?i)^.{0,14}"
				+ "(january |jan|february |feb|march |mar|april |apr|may |june |jun|july "
				+ "|jul|august |aug|september |sep|october |oct|november |nov|december |dec) ?[0-9]{1,2}, [12]{1}[09]{1}[0-9]{2} AND "
				+ "(january |jan|february |feb|march |mar|april |apr|may |june |jun|july "
				+ "|jul|august |aug|september |sep|october |oct|november |nov|december |dec) ?[0-9]{1,2}, [12]{1}[09]{1}[0-9]{2}");

		Pattern patternAnyVisualCharPrecededBy4Ws = Pattern.compile("[ ]{4,}[\\p{Alnum}\\p{Punct}]");

		Pattern patternAboveRowname = Pattern.compile(
				patternAboveRownameColHdgFollowedByYearColHdg.pattern() + "|" + patternAboveRownameMYMY.pattern());

		Matcher matchAboveRowname;
		boolean found = false;
		for (int i = 0; i < textSplit.length; i++) {
			line = textSplit[i];
			line = line.replaceAll("(?i)\\(?DOLLARS IN THOUSANDS\\)?", "");
//			NLP.pwNLP.append(NLP.println("cking if this line has ch above rowname=", line));
			matchAboveRowname = patternAboveRowname.matcher(line);
			// continue below allows greater flexibility b/c it will not grab
			// lines where there isn't at least two columns. I can also further
			// refine and add greater flexibility by cutting line at first
			// instance of 4ws or certain distance. Patterns and this logic
			// assume Year columns on same row

			//

			if (nlp.getAllIndexEndLocations(line, patternAnyVisualCharPrecededBy4Ws).size() < 2)
				continue;
			if (matchAboveRowname.find()) {
//				NLP.pwNLP.append(NLP.println("matchColHdgAboveRowname=", matchAboveRowname.group()));
				tableSentenceSubstitute = matchAboveRowname.group();
				found = true;

				break;
			}
		}

		StringBuffer sbTSPattern = new StringBuffer();
		if (found) {

			sbTSPattern.append(" " + nlp.getTableSentencePatterns(tableSentenceSubstitute, 0 + ""));
			// System.Xut
			// .println("C. above rowname tableSentenceSubstitute sbTSPattern="
			// + sbTSPattern.toString());

			String tsPattern = sbTSPattern.toString().replaceAll("[\\|]{2,}", "\\|").replaceAll("'", "")
					.replaceAll("[\r\n]", "");
			// System.Xut.println("tsPattern=" + tsPattern);
			Pattern patternTSshort = Pattern.compile("(?<=(\\dL\\d))[PMYt](?=:)");
			Matcher matchTSshort = patternTSshort.matcher(tsPattern);
			String tsShort = "";
			while (matchTSshort.find()) {
				// System.Xut.println("matchTSshort.group=" +
				// matchTSshort.group());
				tsShort = tsShort + matchTSshort.group();
			}

			// System.Xut.println("tsShort=" + tsShort);
			tsPattern = tsPattern.replaceAll("\\dL", "");

			list_TS_tsShort_tsPattern_tnValid.add(tableSentenceSubstitute);
			list_TS_tsShort_tsPattern_tnValid.add(tsShort);
			list_TS_tsShort_tsPattern_tnValid.add(tsPattern);
			list_TS_tsShort_tsPattern_tnValid.add(tnValid);

		}

		return list_TS_tsShort_tsPattern_tnValid;
	}

	public static String getCompanyNameOnPriorLine(String text, String coNameMatched) {
		text = text.replaceAll("\r\n|\n\r", "\r");
		String coNameOnPriorLine = "";
		Matcher matchTblNm;
		String[] lines = text.split("[\r\n]");
		String line = "";
		for (int i = 0; i < lines.length; i++) {
			line = lines[i];
			// System.Xut.println("line at get coNameOnPriorLine=" + line);
			matchTblNm = TableParser.TableNamePattern.matcher(line);
			if (matchTblNm.find()) {
				// System.Xut
				// .println("line at matched tblNm=" + coNameOnPriorLine);

				if (i > 1) {
					Matcher matchCoIncLtdLLC = patternCoIncLtdLLC.matcher(lines[i - 2]);
					if (matchCoIncLtdLLC.find() && i > 1) {
						coNameOnPriorLine = lines[i - 2];
						// System.Xut.println("-2 set NLP.coNameOnPriorLine="
						// + coNameOnPriorLine);
					}
				}

				if (i > 0 && coNameOnPriorLine.length() < 3 && lines[i - 1].length() > 3) {
					coNameOnPriorLine = lines[i - 1];
					// System.Xut.println("-1 set NLP.coNameOnPriorLine="+coNameOnPriorLine);

				} else if (i > 1 && lines[i - 2].length() > 3) {
					coNameOnPriorLine = lines[i - 2];
					// System.Xut.println("else -2 set NLP.coNameOnPriorLine="+coNameOnPriorLine);
				}
				break;
			}
		}

		return coNameOnPriorLine;
	}

	public static List<String> parseTableSentence(String tableSentence, String coNameParsed, boolean tableTextParser,
			String tableNameLong, boolean isToc) throws IOException {

		// PICKUP HERE if ts is after tn - why isn't tn validated and grabbed?
		// carry tn thru! see how tn is determined here !

		NLP nlp = new NLP();
		String tmp = tableSentence;
//		NLP.pwNLP.append(NLP.println("@parseTableSentence - tableSentence=", tableSentence + "!end"));
		// System.Xut.println("@parseTableSentence -- conameparse=="+coNameParsed);
		// if it is tableTextParser I stop tablesentence at next line if there
		// are is a blank line or not another match.

		String tablenameIsValid = "false";
		/*
		 * 1. get lines after tablename & replace common non-PMY words (uses startGroup)
		 * 2. if PMY pattern starts directly after tn continue - else break (there's no
		 * tablesentence). 3. for each line see if it is a CH line (cnts instances of 2
		 * or 3 ws between words - if three or more times there are two ws between words
		 * (or if 2 3 ws)- skip and go to next line. 4. replace all non-PMY key words
		 * and meass length of line versus length of all PMY patterns found. If close -
		 * it is a match. NOTE: All patterns utilized are in this method
		 */

		@SuppressWarnings("unused")
		Matcher matchTS, matchCH, matchTableName, matchTSshort, matchSkipHeaderRow;

		// System.Xut
		// .println("first 300 chars of tableSentence to match tableName against="
		// + tableSentence.substring(0,
		// Math.min(300, tableSentence.length())));
		tableSentence = tableSentence.replaceAll("(?i)cash flow partners", "                  ")
				.replaceAll("(?i)notes to consolidated financial statements", "")
				.replaceAll("CONSOLIDATED FINANCIAL STATEMENTS", "                                 ");
		int eIdx = 0;

		Pattern patternCHsbyThreeWs = Pattern.compile("[A-Za-z0-9,]{1,}[ \t]{3,}[A-Za-z0-9,]{1,}");
		Pattern patternCHsbyTwo = Pattern.compile("[A-Za-z0-9,]{1,}[ \t]{2,}[,A-Za-z0-9]{1,}");

		// matcher will run patterns to get all matches. Pattern doesn't need
		// repeater
		Pattern patternEnded = Pattern.compile(
				"(?i)(sixteen|seventeen|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|years?|first|second|third|fourth|"
						+ "for the quarters?|quarters?"
						+ "|12|13|14|15|23|24|25|26|27|28|37|38|39|40|41|50|51|52|53|54|fifty|twenty|thirty|thirteen|fourteen|fifteen)");
		// will retrieve 'FOR THE THREE,SIX' but not after 'AND'. Only pattern
		// with repeater see: +
		Pattern patternYear = Pattern.compile("(?i)(and[ ]{0,1})?[12]{1}[09]{1}[0-9]{2}[$,]{0,1}(?= )?");
		Pattern patternMonthDay = Pattern
				.compile("(?i)(in )?(January|February|March|April|May|June|July|August|September|October|November"
						+ "|December|Sept)" + "(([ ]{0,2}[12]{1}[09]{1}[0-9]{2})|([ ]{0,2}([0-9]{1,2}[ ,$]{0,2}))?)");

		Pattern patternStragglers = Pattern.compile("(?i)(fiscal|months?|periods?|weeks|wk|end[eding]{2,3}"
				+ "|AND | AND|FOR |THE | to |from | from|through )");
		Pattern patternPMY = Pattern.compile(patternEnded.pattern() + "|" + patternYear.pattern() + "|"
				+ patternMonthDay.pattern() + "|" + patternStragglers.pattern());

		Pattern patternSkipHeaderRow = Pattern
				.compile("(?i)(([ ]{2,}|\t)(ASSETS?|LIABILITIES( AND)?|(SHARE|STOCK)HOLDER\\'?S?\\'?"
						+ "|EQUITY|REVENUE|SALES|\\(?CONTINUED\\)?)"
						+ "|LIABILITIES AND (STOCK|SHARE)HOLDERS?\\'? EQUITY" + ")");

		// |,? except par value ?(and number of (shares?|units?) ?)?
		StringBuffer sb = new StringBuffer();
		StringBuffer sbTSPattern = new StringBuffer();

		//
		String replCleanupAftTn = "(?i)((\\(|\\)|in millions of US\\$|\\$ in millions|;|UNITED STATES DOLLARS|(In |All )?((u.s. ?)?Dollars? ?"
				+ "(and )?(shares? ?)(information|amounts? )?|BALANCE ?(AT ?)?"
				+ "|Millions?,?|Thousands?( of)?( dollars)?|Hundreds?)|as of ?|for each of the ?"
				+ "|(AND )?(THE )?(DEVELOPMENT )(STAGE )?(PERIOD )?"
				+ "|(stock|Share)holders.{1,4}Equity|percent(ages?)? ?|continued ?|DEFICIENCY ?|unassigned deficit|TRANSITION ?|With Comparative Figures? for ?"
				+ "(january|february|march|april|may|june|july|august|september|october|november|december)? ?([0-9]{1,2}\\,)? [12]{1}[09]{1}[0-9]{2}"
				+ "|(Other )?Comprehensive (Income|loss|earnings?)|(AND )?RETAINED EARNINGS|CONDENSED"
				+ "| ?EXCEPT (NUMBER( OF)?|COMMON) ?(SHARES?)? ?(DATA)?| ?except (for )?(net )?(EARNINGS )?(\\(LOSS\\) )?(amounts )?per "
				+ "(units? |common |ordinary )?shares? ?(data ?)?|(INTEREST )?(AND )?(DIVIDEND )?INCOME:? ?|EARNINGS?"
				+ "|Expressed in |except parenthetical|respectively ?|Historical( Cost)?"
				+ "|,? ?Except (treasury )?(shares? )?(for )?(Net Income |Earnings )?(and )?per[- ]{1}(ordinary )?shares?( related| amounts?"
				+ "| data)?|(and )?( per)?[- ]{1}?(ordinary )?(shares? |units? )(data ?|information ?)"
				+ "|Stated in thousands of US dollars( except shares issued and outstanding)?"
				+ "|Stated in thousands of US dollars except per( ordinary)? (units?|shares?) (amounts)?" + "|stated in"
				+ "|(in )?accordance with U\\.?S\\.? generally accepted account[ing]{0,3}( princi.{1,4})?"
				+ "|All numbers ?|are presented ?|supplemental disclosure|Dollar amounts? ?"
				+ "|,? ?except par[- ]{1}value ?(and number of (shares?|units?) ?)?"
				+ "|,? ?except (earnings? )?(per[- ]{1})?(ordinary )?(units?|shares?) (amounts)?"
				+ "|(,? except (for|Per)?[- ]{1}?(ordinary )?(units?|shares?)? ?)( ?and per[- ]{1}?)?(ordinary)?(shares?|units?) (Data|amounts?|figures?)?"
				+ "|per (ordinary )?(shares?|units?) related data|Omitted|previous page" + "|(and )?Expenses|results? "
				+ "|[-]+|[_]+"
				+ "| of (u.s. )?dollars|^or the|par[- ]{1}value ?|amounts ?|which are reflected in (millions?|thousands)"
				+ "|(,? except (for|per)?[- ]{1}?(par[- ]{1}value|(ordinary )?shares?)? ?)( ?and (per[- ]{1}?|number of ?))(Shares?|Units?) (Data|amounts?"
				+ "|figures?)?|(All )?figures?|</div>|</p>"
				+ "|REORGANIZED ?|PREDECESSOR ?|SUCCESSOR ?|COMPANY ?| ?AND ?|EXCEPT PER COMMON|per |common ?|except |Comprehensive"
				+ "| ?u? ?n? ?a ?u ?d ?i ?t ?e ?d ?|Series [0-9]|as (of |at )|DATE (OF )?(OPERATIONS? ?|FORMATION ?|INCORPORATION ?)(COMMENCED ?)?"
				+ "|AND DEFICIT ACCUMULATED|DURING THE |EXPLORATION STAGE|shares?"
				+ "|(from |date of )?inception ?|commencement ?|(Statutory|Going Concern) ?(Basis)?| ?ASSETS ?| ?Loss |(IN )?000[s\\']{1,2}|\\. Seven"
				+ "|notes? to consolidated financial statements\\.?|Cash flows? from|Cash Flow Partner|dollars?|, except| xx ))";

		String origLines = "";

		String tn = "";
		// this should be 'if' because I check in method
		// checkForTableNameLaterInStartGroup in TableTextParser for last VALID
		// instance of tablnameIndex and that should be start of startgroup if
		// it is found. If for some reason 'while' works in some cases better
		// than 'if' - create a loop - where loop 1 does the if loop and if no
		// TS found loop 2 does the while loop

		boolean matchTSFound = false;
		boolean loop1 = true;

		// first loop uses 'if' match. if no ts found - then at very bottom this
		// same method is called but will not be loop1 so while match find is
		// used

		matchTableName = TableParser.TableNamePattern.matcher(tableSentence);
		// get end loc of tableName
		// System.Xut.println("tableSentence="+tableSentence);
		Matcher matchDummyTn = Pattern.compile("xx").matcher(tableSentence);

//		NLP.pwNLP.append(NLP.println("tableSentence=", tableSentence));
		boolean dummyTn = false;

		if (loop1) {
			// gets to the last tablename found - except this can create issues
			// if it goes to table data
			while (matchTableName.find()) {
				eIdx = matchTableName.end();
				tn = matchTableName.group();
//				NLP.pwNLP.append(NLP.println("@nlp.parseTableSentence loop1 tablename=", tn));
				if (matchDummyTn.find() && matchDummyTn.end() - matchTableName.start() < 10) {
					dummyTn = true;
				} else
					dummyTn = false;
			}

		} else if (!loop1) {
			while (matchTableName.find()) {
				eIdx = matchTableName.end();
				tn = matchTableName.group();
//				NLP.pwNLP.append(NLP.println("@nlp.parseTableSentence loop2 tablename=", tn));
			}
			if (matchDummyTn.find() && matchDummyTn.end() - matchTableName.start() < 10) {
				dummyTn = true;
			} else
				dummyTn = false;
		}

		// eIdx is end of tablename and start of tableSentence
		if (eIdx > 0) {
			tableSentence = tableSentence.substring(eIdx).trim();
			if (StringUtils.indexOfIgnoreCase(tableSentence, "<TABLE") >= 0 && !tableTextParser) {
				tableSentence = tableSentence.substring(0, StringUtils.indexOfIgnoreCase(tableSentence, "<TABL"))
						.trim();
				// I may want to stop at first </div or </p or </tr
				// (particularly </tr).
				// </div more so than </p -- but both are used as hard returns
				// so can
				// create f/p. See how results fair to see if needed..

			}
			// there are cases where TSEnd occurs prior to start of
			// tableSentence--so this doesn't work.

		}
		int tsSidx = 0;
		tableSentence = nlp.stripHtmlTags(tableSentence);
//		NLP.pwNLP.append(NLP.println("@nlp parseTableSentence - 1 tableSentence=", tableSentence + "!"));
		String[] tsSplit = tableSentence.split("\r\n|[\r\n]");
		tableSentence = "";
		for (int i = 0; i < tsSplit.length; i++) {
			List<Integer> listTmp = nlp.getAllIndexEndLocations(tsSplit[i], nlp.patternAnyVisualCharacter);
			if (listTmp.size() > 0) {
				tsSidx = listTmp.get(0);
			}
//			NLP.pwNLP.append(NLP.println("1 tsSidx=" + tsSidx + " tsSplit[i]", tsSplit[i]));
			if (tsSidx > 85 || (tsSidx > 30
					&& nlp.getAllIndexEndLocations(tsSplit[i], Pattern.compile("[, ]{1}  [\\p{Alnum}\\p{Punct}]"))
							.size() > 1)) {
				break;
			}

//			NLP.pwNLP.append(NLP.println("tsSidx=", tsSidx + " tsSplit[i]=" + tsSplit[i]));
			tableSentence = tableSentence + "\r" + tsSplit[i];
//			NLP.pwNLP.append(NLP.println("tableSentence=", tableSentence));
		}

		tableSentence = tableSentence.replaceAll(
				"(?i)(?<=(January|February|March|April|May|June|July|August|September|October|November|December))(?=[0-9]{1,2})",
				" ").trim();

//		NLP.pwNLP.append(NLP.println("@nlp parseTableSentence - 2 tableSentence=", tableSentence));

		boolean tsInTable = false;
		if (tableSentence.contains("xx"))
			tsInTable = true;
		// System.Xut
		// .println(" tsInTable="
		// + tsInTable
		// + " tableSentence before loop and before xx repl and hard return="
		// + tableSentence + "||");

		tableSentence = tableSentence.replaceAll("\r\n", "\n");
		tableSentence = tableSentence.replaceAll("[\r\n]{1} xx ?", "");
//		NLP.pwNLP.append(NLP.println("tableSentence to split==", tableSentence));
		String[] lines = tableSentence.split("[\r\n]");
		String line = "", newLineMatch = "";

		// int cntCH = 0, cntCH2 = 0;
		Matcher matchFirstWordIdxLoc;
		int wordStartIdx = 0;
		String lineBefRep = "";
		Pattern patternFirstWordIdx = Pattern.compile("[A-Za-z\\d]{1}");
		// tablesentence to occur w/n 3 lines after tablename
		// including any blank lines! Otherwise I risk grabbing CH lines that
		// are false positive tablenSentence. If ts found at line 0,1 or 2 allow
		// check of line 4 else return blank ts value as null

		Matcher matchCoIncLtdLLC = null;

		// in case company name isn't found - this will check for a company name
		// ender - like llc or corp or inc - and then search after that up to
		// but stopping at a line with 3 ws
		tableSentence = "";
		int lineStart = 0;
		String possibleCompanyName = "";
		for (int i = 0; i < lines.length; i++) {
			line = lines[i] + "\r";
			matchCoIncLtdLLC = patternCoIncLtdLLC.matcher(line);
			lineStart = 0;
			while (matchCoIncLtdLLC.find()) {
				// continue;
				lineStart = matchCoIncLtdLLC.end();
				possibleCompanyName = line.substring(0, lineStart);
			}

			matchCoIncLtdLLC = patternCoIncLtdLLC.matcher(line);
			// subsidiary/company/llc etc then skip and allow ts to work. this
			// is for ltd instances where company name is after ts.
			if (matchCoIncLtdLLC.find() && line.indexOf(". ") < 0 && tableSentence.length() > 1
					&& nlp.getAllIndexStartLocations(line, patternPMY).size() < 2) {
				continue;
			}
			line = line.substring(lineStart) + "\r";
			if (nlp.getAllIndexEndLocations(line, patternCHsbyThreeWs).size() > 0)
				break;
			tableSentence = tableSentence.trim() + " \r" + line;
		}

//		NLP.pwNLP.append(NLP.println("tablesentence reconstituted after common company type - inc corp llc etc==",
//				tableSentence));

		lines = tableSentence.trim().split("[\r\n]");
		line = "";
		newLineMatch = "";

		int maxCount = 0;
		if (tsInTable) {
			maxCount = Math.min(lines.length, 5);
		} else
			maxCount = Math.min(lines.length, 4);
		for (int i = 0; i < maxCount; i++) {
			// if tsInTable - than don't require next line after first ts match
			// to not be blank - searc tsInTable below. If txt file require next
			// line not be blank
			line = lines[i];
			newLineMatch = "";
			// cntCH = 0;cntCH2 = 0;
			if (tableSentence.length() < 1 && i == 3) {
				return null;
			}
			// if text line has company name on it - the line likely has no TS
			// data -- however if there are TS matches after TN -then the below
			// will grab it and treat it as the line for purposes of TS matching
			// and not skip this line
			if (coNameParsed.length() > 5 && line.toLowerCase()
					.contains(coNameParsed.toLowerCase().substring(0, Math.min(coNameParsed.length(), 9)) + "")) {
				// I did not cut line after company name - could refine further
				// - but seems unnecessary -- I've never seen TS come prior to
				// company name
				matchTS = patternPMY.matcher(line);
				if (matchTS.find()) {
//					NLP.pwNLP.append(
//							NLP.println("on company name line was ts found -- matchTS.group==", matchTS.group()));
					line = line.substring(matchTS.start());
				}

				else
					continue;
			}

			// bef repl ==>
			lineBefRep = line;

			// this filters our CH aboveRowname - may want to ingegrate to
			// capture CH above rowname here.

			if (lineBefRep.replaceAll("[ \t]", "").length() < 2 && matchTSFound && tableTextParser) {
//				NLP.pwNLP.append(NLP.println("BREAK 1 lineBefRep=", lineBefRep));
				break;
				// assume that tablesentences are not separated by blank lines -
				// far less prone to error than when you allow hard returns
				// between lines of a tableSentence. If prior line was a
				// tablensentence then next line cannot be blank.

			}

			line = line.replaceAll("(" + replCleanupAftTn + ")", "").replaceAll("[ \t]+", " ");

//			NLP.pwNLP.append(NLP.println("line aft repl cleanup line.subString(0,90)=",
//					line.substring(0, Math.min(90, line.length()))));
			if (line.replaceAll("[\t ]+", "").length() < 1 && tableSentence.length() < 3) {
//				NLP.pwNLP.append(NLP.println("continue 1", ""));
				continue;
			}

			if (line.replaceAll("[\t ]+", "").length() < 1 && sb.toString().length() > 6 && !tsInTable) {
				// tableSentence starts initially as text after tablename -
				// where tableSentence type matches are found (eg., ended, month
				// day, year) it is appended to string buffer - sb. If there
				// were previously tableSentence words than you wouldn't expect
				// a blank row to follow.

				matchFirstWordIdxLoc = patternFirstWordIdx.matcher(lineBefRep);
				if (matchFirstWordIdxLoc.find()) {
//					NLP.pwNLP.append(NLP.println("matched first word on this line=", lineBefRep));
					wordStartIdx = matchFirstWordIdxLoc.start();
				}
//				NLP.pwNLP.append(NLP.println("break 2, tableSentence=", tableSentence));
				break;
			}

			matchCH = patternCHsbyThreeWs.matcher(line);

			// System.Xut.println("line to match three ws=" + lines[i]);
			if (nlp.getAllIndexEndLocations(line, patternCHsbyThreeWs).size() > 0) {
				// System.Xut.println("three ws");
				break;
			}

			matchCH = patternCHsbyTwo.matcher(lines[i]);
			if (nlp.getAllIndexEndLocations(lines[i], patternCHsbyTwo).size() > 1) {
				// System.Xut.println("two ws");
				break;
			}

			// System.Xut.println("line right before skip this header row ="
			// + line);
			matchSkipHeaderRow = patternSkipHeaderRow.matcher(line);
			if (matchSkipHeaderRow.find()) {
//				NLP.pwNLP.append(NLP.println("continue 2 - skipHeaderRow", ""));
				continue;
			}

			// find all patterns on line.

			// line = line.replaceAll("[ ]+", " ").trim();
			matchTS = patternPMY.matcher(line);

			if (matchTS.find()) {
				// System.Xut.println("matchTS=" + matchTS.group()
				// + " append full line=" + line);
				origLines = (origLines + " " + line).replaceAll("[ ]+", " ").trim();
//				NLP.pwNLP.append(NLP.println("origLines=", origLines));
				matchTSFound = true;
				// System.Xut.println("matchTS origLines=" + origLines);
				// next line must follow directly after this of we break from
				// loop and populate and return list
			}
			matchTS = patternPMY.matcher(line);
			if (!matchTS.find() && matchTSFound && tableTextParser) {
//				NLP.pwNLP.append(NLP.println("no ts match found && it is txt parser", ""));
				break;
				// if next line after I found a TS is not a TS - break
			}

			matchTS = patternPMY.matcher(line);
			// have to re-match else skips first match b/c of find above
//			NLP.pwNLP.append(NLP.println("line being matched=", line));
			while (matchTS.find() && line.length() < 175) {
				newLineMatch = (newLineMatch.trim() + " " + matchTS.group()).replaceAll("[ ]+", " ").trim();
//				NLP.pwNLP.append(NLP.println("ts <175 match found at line=", matchTS.group()));
				matchFirstWordIdxLoc = patternFirstWordIdx.matcher(lineBefRep);
				if (matchFirstWordIdxLoc.find()) {
					wordStartIdx = matchFirstWordIdxLoc.start();
				}
			}

			newLineMatch = newLineMatch.replaceAll("[ ]+", " ").trim();
			sb.append(" " + newLineMatch);
//			NLP.pwNLP.append(NLP.println("newLineMatch len=",
//					newLineMatch.trim().length() + "\npattern matches on line=" + newLineMatch.trim()));
//			NLP.pwNLP.append(NLP.println("line len=", line.trim().length() + " line=" + line.trim()));
		}

		if (!isToc) {
			tableSentence = sb.toString().replaceAll("[ ]+", " ").trim();
//			NLP.pwNLP.append(NLP.println("tableSentence here??", tableSentence));
		}
		tableSentence = tableSentence.replaceAll("[\r\n]", " ").replaceAll("[ ]+", " ");

//		NLP.pwNLP.append(NLP.println("NLP parseTableSentence. A. tableSentence=", tableSentence));

		matchCoIncLtdLLC = patternCoIncLtdLLC.matcher(origLines);
		if (matchCoIncLtdLLC.find()) {
			origLines = matchCoIncLtdLLC.replaceFirst("");
//			NLP.pwNLP.append(NLP.println("2 origLines=", origLines));
		}

//		NLP.pwNLP.append(NLP.println("(origLines.length() - tableSentence.length())==",
//				(origLines.length() - tableSentence.length()) + "\rtableSentence=" + tableSentence) + "\r1 origLines="
//				+ origLines + "\r\r");

		if ((Math.abs(origLines.length() - tableSentence.length()) <= 5 || tableSentence.length() > 5)
				&& origLines.length() > 8) {
//			NLP.pwNLP.append(NLP.println("3 origLines=", origLines));
//			NLP.pwNLP.append(NLP.println("true tableSentence=", tableSentence));
			tablenameIsValid = "true";
		}

		if ((Math.abs(origLines.length() - tableSentence.length()) > 5 || tableSentence.length() < 5) && !isToc) {
			tableSentence = "";
		}

//		NLP.pwNLP.append(NLP.println("used to get tsPattern - tableSentence is", tableSentence));

		sbTSPattern.append(" " + nlp.getTableSentencePatterns(tableSentence, 0 + ""));
		// System.Xut.println("B. sbTSPattern=" + sbTSPattern.toString());

//		NLP.pwNLP.append(NLP.println(
//				"C.parseTableSentence -tsPattern -before replacing match equal just to 'period ending' tsPattern\r=",
//				sbTSPattern.toString()));

		String tsPattern = sbTSPattern.toString().replaceAll("[\\|]{2,}", "\\|").replaceAll("'", "")
				.replaceAll("[\r\n]", "");

		tsPattern = tsPattern.replaceAll("(?i)\\|\\d?L?\\dP:PERIODS? END[INGED]{2,3}(?=\\|)", "");
		Pattern patternTSshort = Pattern.compile("(?<=(\\dL\\d))[PMYt](?=:)");

//		NLP.pwNLP.append(NLP.println(
//				"C.parseTableSentence - tsPattern -after replacing match equal just to 'period ending' tsPattern\r=",
//				tsPattern));

		matchTSshort = patternTSshort.matcher(tsPattern);
		String tsShort = "";
		while (matchTSshort.find()) {
			// System.Xut.println("matchTSshort.group=" + matchTSshort.group());
			tsShort = tsShort + matchTSshort.group();
		}

		// System.Xut.println("tsShort=" + tsShort);
		tsPattern = tsPattern.replaceAll("\\dL", "");

		// System.Xut.println("tsPattern=" + tsPattern);

		if (tableSentence.length() < 0 && loop1) {
			loop1 = false;
			// System.Xut.println("starting loop2");
			parseTableSentence(tmp, coNameParsed, tableTextParser, tableNameLong, isToc);
		}

		if (tn.length() == 0 && !tablenameIsValid.equals("false")) {
			tn = tableNameLong;
		}

		// else??
		List<String> list_TS_tsShort_tsPattern_tnValid = new ArrayList<String>();
//		NLP.pwNLP.append(NLP.println("@parseTableSentence -- list_TS_tsShort_tsPattern_tnValidadd(totsShort)=",
//				tsShort + " tableSentence==" + tableSentence));
		list_TS_tsShort_tsPattern_tnValid.add(tableSentence);
		list_TS_tsShort_tsPattern_tnValid.add(tsShort);
		list_TS_tsShort_tsPattern_tnValid.add(tsPattern);
		list_TS_tsShort_tsPattern_tnValid.add(tablenameIsValid);
		list_TS_tsShort_tsPattern_tnValid.add(wordStartIdx + "");
		list_TS_tsShort_tsPattern_tnValid.add(possibleCompanyName);
		if (dummyTn) {
			tn = "";
		}
		list_TS_tsShort_tsPattern_tnValid.add(tn);
		// NLP.printListOfString("list_TS_tsShort_tsPattern_tnValid",
		// list_TS_tsShort_tsPattern_tnValid);

		return list_TS_tsShort_tsPattern_tnValid;
	}

	public static List<String[]> getTablesentenceColumnHeadings(String tableSentence, String tsShort, String tsPattern)
			throws ParseException {

		String[] tsPatternSplit = tsPattern.split("\\|");
		// System.Xut.println("tsPatternSplit
		// created="+Arrays.toString(tsPatternSplit));

		@SuppressWarnings("unused")
		List<String[]> listTSColHdgs = new ArrayList<String[]>();
		return listTSColHdgs = getTableSentEdtPerFromPattern(tsPatternSplit, tsShort, tableSentence);
	}

	public static List<String[]> getTableSentEdtPerFromPattern(String[] tsPatternSplit, String tsShort,
			String tableSentence) throws ParseException {

		// System.Xut.println("tableSentence="+tableSentence+" tsPatternSplit="+"\r"
		// +Arrays.toString(tsPatternSplit)+
		// "\r tsPatternSplit.len="+tsPatternSplit.length+" tsShort="+tsShort +
		// "\rtsPatternSplit[0].len="+tsPatternSplit[0].length());

//		NLP.pwNLP.append(NLP.println("tableSentence=",
//				tableSentence + " tsPatternSplit=" + "\r" + Arrays.toString(tsPatternSplit) + "\r tsPatternSplit.len="
//						+ tsPatternSplit.length + " tsShort=" + tsShort + "\rtsPatternSplit[0].len="
//						+ tsPatternSplit[0].length()));

		/*
		 * if(tsPatternSplit == null || tsPatternSplit.length<1 ||
		 * (tsPatternSplit.length==1 && tsPatternSplit[0].replaceAll(" ",
		 * "").length()==0)) return null;
		 */

//		NLP.pwNLP.append(
//				NLP.println("@nlp.getTableSentEdtPerFromPattern -- tsPatternSplit==", Arrays.toString(tsPatternSplit)));
		NLP nlp = new NLP();

		// This creates list of string array for ts - each array in list is edt
		// and per

		List<String[]> listTSColHdgs = new ArrayList<String[]>();
		// tsPatternSplit is eg:
		// |1P:FOR THE THREE MONTHS|1M:MARCH 31|1Y:1999
		// to retrieve part of col text if would call
		// tsPatternSplit[1]=FORTHE THREE MONTHS

		if (tsShort.equals("M") && tsPatternSplit.length > 1) {
			listTSColHdgs
					.add(nlp.getEnddatePeriodFromTS(tsPatternSplit[1].replaceAll("1M:", "") + " ", " ", tableSentence));
		}

		if (tsShort.equals("P") && tsPatternSplit.length > 1) {
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(" ", tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

		}

		if (tsShort.equals("PM") && tsPatternSplit.length > 2) {
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(tsPatternSplit[2].replaceAll("1M:", "") + " ",
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));
		}

		if (tsShort.equals("MYY") && tsPatternSplit.length > 3) {

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[1].replaceAll("1M:", "") + " " + tsPatternSplit[2].replaceAll("1Y:", ""), " ",
					tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[1].replaceAll("1M:", "") + " " + tsPatternSplit[3].replaceAll("2Y:", ""), " ",
					tableSentence));
		}

		if (tsShort.equals("MtM") && tsPatternSplit.length > 3) {

			// use dummy yr (no yr available)
			String earlierDate = (nlp.getEnddatePeriodFromTS(tsPatternSplit[1].replaceAll("1M:", "") + " " + "2000",
					" ", tableSentence))[0];

			String laterDate = (nlp.getEnddatePeriodFromTS(tsPatternSplit[3].replaceAll("2M:", "") + " " + "2000", " ",
					tableSentence))[0];

			int per = getDateDifferenceInMonths(laterDate, earlierDate);
			// System.Xut.println("MtM per=" + per);
			if (per != 3 && per != 6 && per != 9 && per != 12) {
				per = 0;
			}

			// yr not used
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(tsPatternSplit[3].replaceAll("2M:", ""), per + " months",
					tableSentence));
		}

		if (tsShort.equals("PPM") && tsPatternSplit.length > 3) {
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(tsPatternSplit[3].replaceAll("1M:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(tsPatternSplit[3].replaceAll("1M:", ""),
					tsPatternSplit[2].replaceAll("1P:", ""), tableSentence));

		}

		if (tsShort.equals("PMY") && tsPatternSplit.length > 2) {
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[3].replaceAll("1Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));
		}

		if (tsShort.equals("PMPM") && tsPatternSplit.length > 4) {
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(tsPatternSplit[2].replaceAll("1M:", "") + " ",
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(tsPatternSplit[4].replaceAll("2M:", "") + " ",
					tsPatternSplit[3].replaceAll("2P:", ""), tableSentence));
		}

		if (tsShort.equals("MYMY") && tsPatternSplit.length > 4) {
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[1].replaceAll("1M:", "") + " " + tsPatternSplit[2].replaceAll("1Y:", ""), " ",
					tableSentence));
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[3].replaceAll("2M:", "") + " " + tsPatternSplit[4].replaceAll("2Y:", ""), " ",
					tableSentence));
		}

		if (tsShort.equals("PMYY") && tsPatternSplit.length > 4) {
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[3].replaceAll("1Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[4].replaceAll("2Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));
		}

		if (tsShort.equals("PMMM") && tsPatternSplit.length > 4) {
			// edt,per,ts
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(tsPatternSplit[2].replaceAll("1M:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(tsPatternSplit[3].replaceAll("2M:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(tsPatternSplit[4].replaceAll("3M:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));
		}

		if (tsShort.equals("MM") && tsPatternSplit.length > 2) {
			// edt,per,ts
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(tsPatternSplit[1].replaceAll("1M:", ""), "", tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(tsPatternSplit[2].replaceAll("2M:", ""), "", tableSentence));
		}

		if (tsShort.equals("PMYYY") && tsPatternSplit.length > 3) {
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[3].replaceAll("1Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[4].replaceAll("2Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[5].replaceAll("3Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

		}

		if (tsShort.equals("MYYMY") && tsPatternSplit.length > 5) {
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[1].replaceAll("1M:", "") + " " + tsPatternSplit[2].replaceAll("1Y:", ""), " ",
					tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[1].replaceAll("1M:", "") + " " + tsPatternSplit[3].replaceAll("2Y:", ""), " ",
					tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[4].replaceAll("2M:", "") + " " + tsPatternSplit[5].replaceAll("3Y:", ""), " ",
					tableSentence));
		}

		if (tsShort.equals("PPMYY") && tsPatternSplit.length > 5) {

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[3].replaceAll("1M:", "") + " " + tsPatternSplit[4].replaceAll("1Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[3].replaceAll("1M:", "") + " " + tsPatternSplit[5].replaceAll("2Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[3].replaceAll("1M:", "") + " " + tsPatternSplit[4].replaceAll("1Y:", ""),
					tsPatternSplit[2].replaceAll("2P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[3].replaceAll("1M:", "") + " " + tsPatternSplit[5].replaceAll("2Y:", ""),
					tsPatternSplit[2].replaceAll("2P:", ""), tableSentence));
		}

		if (tsShort.equals("PPMYMY") && tsPatternSplit.length > 6) {
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[3].replaceAll("1M:", "") + " " + tsPatternSplit[4].replaceAll("1Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[5].replaceAll("2M:", "") + " " + tsPatternSplit[6].replaceAll("2Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[3].replaceAll("1M:", "") + " " + tsPatternSplit[4].replaceAll("1Y:", ""),
					tsPatternSplit[2].replaceAll("2P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[5].replaceAll("2M:", "") + " " + tsPatternSplit[6].replaceAll("2Y:", ""),
					tsPatternSplit[2].replaceAll("2P:", ""), tableSentence));
		}

		if (tsShort.equals("MYMYMY") && tsPatternSplit.length > 6) {
			// System.Xut.println("tsPatternSplit[1]="
			// + tsPatternSplit[1].replaceAll("1M:", "")
			// + " tsPatternSplit[2]"
			// + tsPatternSplit[2].replaceAll("1Y:", ""));
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[1].replaceAll("1M:", "") + " " + tsPatternSplit[2].replaceAll("1Y:", ""), " ",
					tableSentence));
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[3].replaceAll("2M:", "") + " " + tsPatternSplit[4].replaceAll("2Y:", ""), " ",
					tableSentence));
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[5].replaceAll("3M:", "") + " " + tsPatternSplit[6].replaceAll("3Y:", ""), " ",
					tableSentence));
		}

		if (tsShort.equals("PMYMY") && tsPatternSplit.length > 5) {
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[3].replaceAll("1Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[4].replaceAll("2M:", "") + " " + tsPatternSplit[5].replaceAll("2Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

		}

		// System.Xut.println("tsPattern="+Arrays.toString(tsPatternSplit));
		if (tsShort.equals("PMYMYMY") && tsPatternSplit.length > 6) {
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[3].replaceAll("1Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[4].replaceAll("2M:", "") + " " + tsPatternSplit[5].replaceAll("2Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[6].replaceAll("3M:", "") + " " + tsPatternSplit[7].replaceAll("3Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));
		}

		if (tsShort.equals("PMYMYY") && tsPatternSplit.length > 6) {
			// old PMYYPMYY
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[3].replaceAll("1Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[4].replaceAll("2M:", "") + " " + tsPatternSplit[5].replaceAll("2Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[4].replaceAll("2M:", "") + " " + tsPatternSplit[6].replaceAll("3Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

		}

		if (tsShort.equals("PMYMYYPMY") && tsPatternSplit.length > 9) {
			// old PMYYPMYY
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[3].replaceAll("1Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[4].replaceAll("2M:", "") + " " + tsPatternSplit[5].replaceAll("2Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[4].replaceAll("2M:", "") + " " + tsPatternSplit[6].replaceAll("3Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[8].replaceAll("3M:", "") + " " + tsPatternSplit[9].replaceAll("4Y:", ""),
					tsPatternSplit[7].replaceAll("2P:", ""), tableSentence));

		}

		if (tsShort.equals("PMYYPMYY") && tsPatternSplit.length > 8) {
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[3].replaceAll("1Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[4].replaceAll("2Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[6].replaceAll("2M:", "") + " " + tsPatternSplit[7].replaceAll("3Y:", ""),
					tsPatternSplit[5].replaceAll("2P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[6].replaceAll("2M:", "") + " " + tsPatternSplit[8].replaceAll("4Y:", ""),
					tsPatternSplit[5].replaceAll("2P:", ""), tableSentence));
		}

		if (tsShort.equals("PMYMYtMY") && tsPatternSplit.length > 7) {
			String laterDate1 = (nlp.getEnddatePeriodFromTS(
					tsPatternSplit[7].replaceAll("3M:", "") + " " + tsPatternSplit[8].replaceAll("3Y:", ""), " ",
					tableSentence))[0];

			String earlierDate2 = (nlp.getEnddatePeriodFromTS(
					tsPatternSplit[4].replaceAll("2M:", "") + " " + tsPatternSplit[5].replaceAll("2Y:", ""), " ",
					tableSentence))[0];

			// System.Xut.println("earlierDate=" + earlierDate2 + " laterDate1="
			// + laterDate1);
			int per = getDateDifferenceInMonths(laterDate1, earlierDate2);

			if (per != 3 && per != 6 && per != 9 && per != 12) {
				per = 0;
			}

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[3].replaceAll("1Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[7].replaceAll("3M:", "") + " " + tsPatternSplit[8].replaceAll("3Y:", ""),
					per + " months", tableSentence));
		}

		// MYtMY is from period to period- I don't attempt to get period.
		if (tsShort.equals("PMYYPMYYMYtMY") && tsPatternSplit.length > 13) {

			String laterDate1 = (nlp.getEnddatePeriodFromTS(
					tsPatternSplit[12].replaceAll("4M:", "") + " " + tsPatternSplit[13].replaceAll("6Y:", ""), " ",
					tableSentence))[0];

			String earlierDate2 = (nlp.getEnddatePeriodFromTS(
					tsPatternSplit[9].replaceAll("3M:", "") + " " + tsPatternSplit[10].replaceAll("5Y:", ""), " ",
					tableSentence))[0];

			int per = getDateDifferenceInMonths(laterDate1, earlierDate2);
			// System.Xut.println("per=" + per);

			if (per != 3 && per != 6 && per != 9 && per != 12) {
				per = 0;
			}

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[3].replaceAll("1Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[4].replaceAll("2Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[6].replaceAll("2M:", "") + " " + tsPatternSplit[7].replaceAll("3Y:", ""),
					tsPatternSplit[5].replaceAll("2P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[6].replaceAll("2M:", "") + " " + tsPatternSplit[8].replaceAll("4Y:", ""),
					tsPatternSplit[5].replaceAll("2P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[12].replaceAll("4M:", "") + " " + tsPatternSplit[13].replaceAll("6Y:", ""),
					per + " months", tableSentence));
		}

		if (tsShort.equals("PPMYYMYtMY") && tsPatternSplit.length > 10) {

			String laterDate1 = (nlp.getEnddatePeriodFromTS(
					tsPatternSplit[9].replaceAll("3M:", "") + " " + tsPatternSplit[10].replaceAll("4Y:", ""), " ",
					tableSentence))[0];

			String earlierDate2 = (nlp.getEnddatePeriodFromTS(
					tsPatternSplit[6].replaceAll("2M:", "") + " " + tsPatternSplit[7].replaceAll("3Y:", ""), " ",
					tableSentence))[0];

			// System.Xut.println("earlierDate=" + earlierDate2 + " laterDate1="
			// + laterDate1);
			int per = getDateDifferenceInMonths(laterDate1, earlierDate2);

			if (per != 3 && per != 6 && per != 9 && per != 12) {
				per = 0;
			}

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[3].replaceAll("1M:", "") + " " + tsPatternSplit[4].replaceAll("1Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[3].replaceAll("1M:", "") + " " + tsPatternSplit[5].replaceAll("2Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[3].replaceAll("1M:", "") + " " + tsPatternSplit[4].replaceAll("1Y:", ""),
					tsPatternSplit[2].replaceAll("2P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[3].replaceAll("1M:", "") + " " + tsPatternSplit[5].replaceAll("2Y:", ""),
					tsPatternSplit[2].replaceAll("2P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[9].replaceAll("3M:", "") + " " + tsPatternSplit[10].replaceAll("4Y:", ""),
					per + " months", tableSentence));
		}

		if (tsShort.equals("PMYYMYtMY") && tsPatternSplit.length > 9) {

			String laterDate1 = (nlp.getEnddatePeriodFromTS(
					tsPatternSplit[8].replaceAll("3M:", "") + " " + tsPatternSplit[9].replaceAll("4Y:", ""), " ",
					tableSentence))[0];

			String earlierDate2 = (nlp.getEnddatePeriodFromTS(
					tsPatternSplit[5].replaceAll("2M:", "") + " " + tsPatternSplit[6].replaceAll("3Y:", ""), " ",
					tableSentence))[0];

			// System.Xut.println("earlierDate=" + earlierDate2 + " laterDate1="
			// + laterDate1);
			int per = getDateDifferenceInMonths(laterDate1, earlierDate2);

			if (per != 3 && per != 6 && per != 9 && per != 12) {
				per = 0;
			}

			// System.Xut.println("PMYYMYtMY per=" + per);

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[3].replaceAll("1Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[4].replaceAll("2Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[8].replaceAll("3M:", "") + " " + tsPatternSplit[9].replaceAll("4Y:", ""),
					per + " months", tableSentence));
		}

		if (tsShort.equals("PMYYYMYtMY") && tsPatternSplit.length > 10) {
			String laterDate1 = (nlp.getEnddatePeriodFromTS(
					tsPatternSplit[9].replaceAll("3M:", "") + " " + tsPatternSplit[10].replaceAll("5Y:", ""), " ",
					tableSentence))[0];

			String earlierDate2 = (nlp.getEnddatePeriodFromTS(
					tsPatternSplit[6].replaceAll("2M:", "") + " " + tsPatternSplit[7].replaceAll("3Y:", ""), " ",
					tableSentence))[0];

			// System.Xut.println("earlierDate=" + earlierDate2 + " laterDate1="
			// + laterDate1);
			int per = getDateDifferenceInMonths(laterDate1, earlierDate2);

			if (per != 3 && per != 6 && per != 9 && per != 12) {
				per = 0;
			}

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[3].replaceAll("1Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[4].replaceAll("2Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[5].replaceAll("3Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[9].replaceAll("3M:", "") + " " + tsPatternSplit[10].replaceAll("5Y:", ""),
					per + " months", tableSentence));

		}

		if (tsShort.equals("PMYYPMYYY") && tsPatternSplit.length > 9) {
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[3].replaceAll("1Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[4].replaceAll("2Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[6].replaceAll("2M:", "") + " " + tsPatternSplit[7].replaceAll("3Y:", ""),
					tsPatternSplit[5].replaceAll("2P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[6].replaceAll("2M:", "") + " " + tsPatternSplit[8].replaceAll("4Y:", ""),
					tsPatternSplit[5].replaceAll("2P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[6].replaceAll("2M:", "") + " " + tsPatternSplit[9].replaceAll("5Y:", ""),
					tsPatternSplit[5].replaceAll("2P:", ""), tableSentence));
		}

		if (tsShort.equals("PMYPMYPMYMY") && tsPatternSplit.length > 10) {
			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[2].replaceAll("1M:", "") + " " + tsPatternSplit[3].replaceAll("1Y:", ""),
					tsPatternSplit[1].replaceAll("1P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[5].replaceAll("2M:", "") + " " + tsPatternSplit[6].replaceAll("2Y:", ""),
					tsPatternSplit[4].replaceAll("2P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[8].replaceAll("3M:", "") + " " + tsPatternSplit[9].replaceAll("3Y:", ""),
					tsPatternSplit[7].replaceAll("3P:", ""), tableSentence));

			listTSColHdgs.add(nlp.getEnddatePeriodFromTS(
					tsPatternSplit[10].replaceAll("4M:", "") + " " + tsPatternSplit[11].replaceAll("4Y:", ""),
					tsPatternSplit[7].replaceAll("3P:", ""), tableSentence));
		}
//		NLP.printListOfStringArray("listTSColHdgs", listTSColHdgs);
		return listTSColHdgs;
	}

	public static List<String[]> mergeTablesentenceEdtPerWithEdtPerIdx(List<String[]> listEdtPerIdx,
			List<String[]> listEdtPerTableSentence, String tsShort, int totalColumns, boolean columnOneAndTwoSameYear,
			String allColText) {

		List<String[]> listMergedEdtPerIdxWithTableSentence = new ArrayList<String[]>();

		NLP nlp = new NLP();

		if (totalColumns > 1 && listEdtPerIdx.size() > 1 && listEdtPerIdx.get(0)[0].length() > 4
				&& listEdtPerIdx.get(1)[0].length() > 4
				&& !listEdtPerIdx.get(0)[0].substring(0, 4).equals(listEdtPerIdx.get(1)[0].substring(0, 4))) {
			columnOneAndTwoSameYear = false;
		} else {
			columnOneAndTwoSameYear = true;
		}

		/*
		 * create enddate pattern matchers for edt in both ts edt per and CH edt per
		 * list and run against each variable. If edtIdx isn't found but TS - use TS if
		 * both idx and ts year match. Create variables upfront -- don't use elongated
		 * process below. ts list also has to satisfy size requirements based on tsShort
		 * type. Year must always match. TC w/ tsShort and ts variable cnt are tools to
		 * match. See excel notes. Matching period??
		 */

		String perIdx, perTs = "", edtTs = "", edtIdx;
		int totalColsTs = listEdtPerTableSentence.size(), totalColsIdx = listEdtPerIdx.size();

//		NLP.pwNLP.append(NLP.println("mergeTablesentenceEdtPerWithEdtPerIdx -- columnOneAndTwoSameYear="
//				+ columnOneAndTwoSameYear + " tsShort=", tsShort));

		// System.Xut.println("totalColsTs=" + totalColsTs + " totalColsIdx="
		// + totalColsIdx + " totalColumns=" + totalColumns);
		/* if replace results in 0 len then that's the pattern */
		// NOTE: repl will replace 1st item and then look for others. Therefore
		// the replace that is a subset of another must go after the one it is a
		// subset of.

		if ((tsShort.replaceAll("^PMYMYtMY$|^PMYMY$|^PMYY$|^MYY$", "").length() == 0 && totalColumns == 2
				&& totalColsTs == 2 && (totalColsIdx == 2 || totalColsIdx == 0))

				|| (tsShort.replaceAll("^PMYMYMY$|^MYMYMY$|^MYYMY$|^PMYYY$", "").length() == 0 && totalColumns == 3
						&& totalColsTs == 3 && (totalColsIdx == 3 || totalColsIdx == 0))

				|| (tsShort.replaceAll("^PMYYYMYtMY$|^PMYPMYPMYMY$|^PPMYMY$|^PPMYY$|PMYYPMYY", "").length() == 0
						&& totalColumns == 4 && totalColsTs == 4 && (totalColsIdx == 4 || totalColsIdx == 0))

				|| (tsShort.replaceAll("^PMYYPMYYMYtMY$|^PMYYPMYYY$", "").length() == 0 && totalColumns == 5
						&& totalColsTs == 5 && (totalColsIdx == 5 || totalColsIdx == 0))

				|| (tsShort.replaceAll("^PM$", "").length() == 0)
				|| (tsShort.replaceAll("^PMPM$", "").length() == 0 && totalColumns == 4 && totalColsTs == 2
						&& (totalColsIdx == 4 || totalColsIdx == 0))

				|| (tsShort.equals("PPMYYMYtMY") && totalColumns == 5 && totalColsTs == 5
						&& (totalColsIdx == 5 || totalColsIdx == 0) && !columnOneAndTwoSameYear)

				|| (tsShort.equals("PMYYMYtMY") && totalColumns == 3 && totalColsTs == 3
						&& (totalColsIdx == 3 || totalColsIdx == 0) && !columnOneAndTwoSameYear)

				|| (tsShort.equals("M") || (nlp.getAllIndexEndLocations(tsShort, Pattern.compile("P")).size() == 1
						&& (nlp.getAllIndexEndLocations(allColText, Pattern.compile("pCntD:1")).size() == 1
								|| nlp.getAllIndexEndLocations(allColText, Pattern.compile("pCntD:0")).size() == 1
								|| allColText.length() < 1))

						|| (nlp.getAllIndexEndLocations(tsShort, Pattern.compile("M")).size() == 1
								&& (nlp.getAllIndexEndLocations(allColText, Pattern.compile("mCntD:1")).size() == 1
										|| nlp.getAllIndexEndLocations(allColText, Pattern.compile("mCntD:0"))
												.size() == 1
										|| allColText.length() < 1))

						|| tsShort.equals("MtM") || tsShort.equals("PMY") || tsShort.equals("MYY")
						|| tsShort.equals("PPM") || tsShort.equals("PMMM") || tsShort.equals("MM")
						|| tsShort.equals("MYMY"))

				|| (tsShort.equals("PMYMYY") && totalColumns == 3 && totalColsIdx == 3)) {

//			NLP.pwNLP.append(NLP.println("mergeTablesentenceEdtPerWithEdtPerIdx. 2 tsshort=",
//					tsShort + " totalColumns=" + totalColumns + " allColText=" + TableTextParser.allColText));
			for (int i = 0; i < totalColumns; i++) {

				if (totalColsIdx != 0 && listEdtPerIdx != null && listEdtPerIdx.size() > i) {
					edtIdx = listEdtPerIdx.get(i)[0];
					perIdx = listEdtPerIdx.get(i)[1];
				} else {
					edtIdx = "";
					perIdx = "0";
				}

				if (listEdtPerIdx.size() == listEdtPerTableSentence.size() && i < listEdtPerIdx.size()) {
					edtTs = listEdtPerTableSentence.get(i)[0];
					perTs = listEdtPerTableSentence.get(i)[1];
				}

				// if one M - it can still be multiple edtTs given multi years.
				if (nlp.getAllIndexEndLocations(tsShort, Pattern.compile("P")).size() == 1) {
					perTs = listEdtPerTableSentence.get(0)[1];
				}

//				NLP.pwNLP.append(NLP.println("before merged edtIdx=", edtIdx + " perIdx=" + perIdx));

				// if ts CH applies to each CH idx (eg., only 1 ts edt PM) place
				// here.
				if ((tsShort.equals("PM") || tsShort.equals("P") || tsShort.equals("M") || tsShort.equals("MtM")
						|| tsShort.equals("PMY") || tsShort.equals("PPM")
						|| nlp.getAllIndexEndLocations(tsShort, Pattern.compile("P")).size() == 1
						|| nlp.getAllIndexEndLocations(tsShort, Pattern.compile("M")).size() == 1)
						&& listEdtPerTableSentence != null && listEdtPerTableSentence.size() > 0) {

					// PPMYY = three and six months ended July 31, 1995 and
					// 1994. Where in col idx - year1 and 2 are not the same.
					// c1=1995-07-31,3
					// c2=1994-07-31,3
					// c3=1995-07-31,6
					// c4=1995-07-31,6
					// PMYYPMYY: for the three months ended July 31, 1995 and
					// 1994 and the six months ended July 31 1995 and 1994.

					if ((tsShort.equals("PPM") || tsShort.equals("PPMYY") || tsShort.equals("PMYYPMYY"))
							&& !columnOneAndTwoSameYear && totalColumns == 4) {
						if (i == 0 || i == 1) {
							if (tsShort.equals("PPM")) {
								perTs = listEdtPerTableSentence.get(0)[1];
								edtTs = listEdtPerTableSentence.get(0)[0];
							} else {
								perTs = listEdtPerTableSentence.get(0)[1];
								edtTs = listEdtPerTableSentence.get(i)[0];
							}
						}
						// had to chg to listEdtPerTablesentence.get(2) w/ toc
						// ts
						if (i == 2 || i == 3) {
							if (listEdtPerTableSentence.size() == 2) {
								if (tsShort.equals("PPM")) {
									perTs = listEdtPerTableSentence.get(1)[1];
									edtTs = listEdtPerTableSentence.get(0)[0];
								} else {
									perTs = listEdtPerTableSentence.get(1)[1];
									edtTs = listEdtPerTableSentence.get(1)[0];
								}

							} else {
								if (tsShort.equals("PPM")) {
									perTs = listEdtPerTableSentence.get(1)[1];
									edtTs = listEdtPerTableSentence.get(0)[0];
								} else {
									perTs = listEdtPerTableSentence.get(2)[1];
									edtTs = listEdtPerTableSentence.get(i)[0];
								}
							}
						}
					}

					if ((tsShort.equals("PPM") || tsShort.equals("PPMYY") || tsShort.equals("PMYYPMYY"))
							&& columnOneAndTwoSameYear && totalColumns == 4) {
						if (i == 0 || i == 2) {
							if (tsShort.equals("PPM")) {
								perTs = listEdtPerTableSentence.get(0)[1];
								edtTs = listEdtPerTableSentence.get(0)[0];
							} else {
								perTs = listEdtPerTableSentence.get(0)[1];
								edtTs = listEdtPerTableSentence.get(i)[0];
							}
						}
						if (i == 1 || i == 3) {
							if (listEdtPerTableSentence.size() == 2) {
								if (tsShort.equals("PPM")) {
									perTs = listEdtPerTableSentence.get(1)[1];
									edtTs = listEdtPerTableSentence.get(0)[0];
								} else {
									perTs = listEdtPerTableSentence.get(1)[1];
									edtTs = listEdtPerTableSentence.get(1)[0];
								}

							} else {
								perTs = listEdtPerTableSentence.get(2)[1];
								edtTs = listEdtPerTableSentence.get(i)[0];
							}
						}
					}

					edtIdx = getEdt(edtIdx, edtTs, perIdx, perTs, tsShort);
					perIdx = getPer(edtIdx, edtTs, perIdx, perTs, tsShort, columnOneAndTwoSameYear, totalColumns, i);
//					NLP.pwNLP.append(NLP.println("1 mergedEdtwithTs. column=",
//							i + " perIdx=" + perIdx + " perTs=" + perTs + " edtIdx=" + edtIdx + " edtTs=" + edtTs));
					String[] ary = { edtIdx, perIdx };
					listMergedEdtPerIdxWithTableSentence.add(ary);
					continue;
				}

				if (tsShort.equals("MYY") && totalColumns == 4 && !columnOneAndTwoSameYear) {

					if (i == 0 || i == 2) {
						edtTs = listEdtPerTableSentence.get(0)[0];
						edtIdx = getEdt(edtIdx, edtTs, perIdx, "0", tsShort);
					}

					if (i == 1 || i == 3) {
						edtTs = listEdtPerTableSentence.get(1)[0];
						edtIdx = getEdt(edtIdx, edtTs, perIdx, "0", tsShort);
					}
//					NLP.pwNLP.append(NLP.println("2 mergedEdtwithTs. column=",
//							i + " perIdx=" + perIdx + " perTs=" + perTs + " edtIdx=" + edtIdx + " edtTs=" + edtTs));
					String[] ary = { edtIdx, perIdx };
					listMergedEdtPerIdxWithTableSentence.add(ary);
					continue;
				}

				// if 4 cols - 1st 2 get 1st PM
				if ((tsShort.equals("PMPM") && !columnOneAndTwoSameYear && totalColumns == 4)
						&& listEdtPerTableSentence != null && listEdtPerTableSentence.size() > 0) {
					if (i < 2) {
						edtTs = listEdtPerTableSentence.get(0)[0];
						perTs = listEdtPerTableSentence.get(0)[1];
						edtIdx = getEdt(edtIdx, edtTs, perIdx, perTs, tsShort);
						perIdx = getPer(edtIdx, edtTs, perIdx, perTs, tsShort, columnOneAndTwoSameYear, totalColumns,
								i);
					}

					if (i >= 2) {
						edtTs = listEdtPerTableSentence.get(1)[0];
						perTs = listEdtPerTableSentence.get(1)[1];
						edtIdx = getEdt(edtIdx, edtTs, perIdx, perTs, tsShort);
						perIdx = getPer(edtIdx, edtTs, perIdx, perTs, tsShort, columnOneAndTwoSameYear, totalColumns,
								i);
					}
//					NLP.pwNLP.append(NLP.println("3 mergedEdtwithTs. column=",
//							i + " perIdx=" + perIdx + " perTs=" + perTs + " edtIdx=" + edtIdx + " edtTs=" + edtTs));
					String[] ary = { edtIdx, perIdx };
					listMergedEdtPerIdxWithTableSentence.add(ary);
					continue;

				}

				// If I add similar exceptions like this - include in opening if
				// clause of tsShort.equal("MYMY")
				// but make sure the catch all .add(...)

				if (tsShort.equals("MYMY") && listEdtPerTableSentence.size() == 2) {
					// if years match - then add month and or peroid
					if (null == edtIdx || edtIdx.length() < 4)
						continue;

					String edtIdxYr = edtIdx.substring(0, 4).trim();
					String edtTsYr1 = listEdtPerTableSentence.get(0)[0].substring(0, 4).trim();
					String edtTsYr2 = listEdtPerTableSentence.get(1)[0].substring(0, 4).trim();
					if (edtIdxYr.equals(edtTsYr1)) {
						edtTs = listEdtPerTableSentence.get(0)[0];
						edtIdx = getEdt(edtIdx, edtTs, perIdx, "0", tsShort);
					}
					if (edtIdxYr.equals(edtTsYr2)) {
						edtTs = listEdtPerTableSentence.get(1)[0];
						edtIdx = getEdt(edtIdx, edtTs, perIdx, "0", tsShort);
					}

//					NLP.pwNLP.append(NLP.println("4 mergedEdtwithTs. column=",
//							i + " perIdx=" + perIdx + " perTs=" + perTs + " edtIdx=" + edtIdx + " edtTs=" + edtTs));
					String[] ary = { edtIdx, perIdx };
					listMergedEdtPerIdxWithTableSentence.add(ary);
					continue;
				}

				// where # of Ts cols = # of idx cols - this distributes 1:1

				if (listEdtPerTableSentence.size() == listEdtPerIdx.size()) {

					if ((tsShort.equals("PMMM") || tsShort.equals("MM"))
							&& (edtIdx.replaceAll("[12]{1}[09]{1}[0-9]{2}[ -]{0,}", "").length() == 0
									|| edtIdx.replaceAll("[12]{1}[09]{1}[0-9]{2}-\\d\\d-? ?", "").length() == 0)
							&& edtIdx.length() > 4) {
						if (perIdx.equals("0")
								&& nlp.getAllIndexEndLocations(tsShort, Pattern.compile("P")).size() == 1) {
							perIdx = perTs;
						}

						edtIdx = (edtIdx.substring(0, 4) + edtTs).replaceAll("[-]+", "-");
					}

					else {
						if (!tsShort.equals("MY")) {
							edtIdx = getEdt(edtIdx, edtTs, perIdx, perTs, tsShort);
							perIdx = getPer(edtIdx, edtTs, perIdx, perTs, tsShort, columnOneAndTwoSameYear,
									totalColumns, i);
						}
					}

//					NLP.pwNLP.append(NLP.println("5 mergedEdtwithTs. column=",
//							i + " perIdx=" + perIdx + " perTs=" + perTs + " edtIdx=" + edtIdx + " edtTs=" + edtTs));
					String[] ary = { edtIdx, perIdx };
					listMergedEdtPerIdxWithTableSentence.add(ary);
					continue;
				}
			}

//			NLP.printListOfStringArray("@mergeTablesentenceEdtPerWithEdtPerIdx --listMergedEdtPerIdxWithTableSentence=",
//					listMergedEdtPerIdxWithTableSentence);

		}
		// if none of the tsShort are found (loop conditions) returns original
		// list
		else {
			// System.Xut.println("1 nothng merged mergeTablesentenceEdtPerWithEdtPerIdx =
			// returned listEdtPerIdx");
			return listEdtPerIdx;
		}

		if (listMergedEdtPerIdxWithTableSentence == null || listMergedEdtPerIdxWithTableSentence.size() == 0
				|| (listMergedEdtPerIdxWithTableSentence.size() != listEdtPerIdx.size()
						&& listEdtPerIdx.size() == totalColumns)) {
//			NLP.pwNLP.append(NLP.println("2 returned listEdtPerIdx.listMergedEdtPerIdxWithTableSentence.size=",
//					listMergedEdtPerIdxWithTableSentence.size() + "\rlistEdtPerIdx.size()=" + listEdtPerIdx.size()
//							+ " totalColumns=" + totalColumns));
			return listEdtPerIdx;
		}

		else {
//			NLP.pwNLP.append(NLP.println("returned listMergedEdtPerIdxWithTableSentence", ""));
			return listMergedEdtPerIdxWithTableSentence;

		}

	}

	public static String getEdt(String edtIdx, String edtTs, String perIdx, String perTs, String tsShort) {

		// if edtIdx is incomplete - but has year and that year match edtTs and
		// edtTS is complete - then edtIdx set to edtTs. If no year value for
		// edtIdx but month value for edtIdx then set edtIdx to edtTs. If edtIdx
		// is blank but edtTs is complete - then edtTs

		String yrIdx = "", yrTs = "", moDayIdx = "", moDayTs = "";
		NLP nlp = new NLP();

		Pattern PatternEnddate = Pattern
				.compile("[12]{1}[09]{1}[0-9]{2}-(0[1-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})");
		Pattern PatternYear = Pattern.compile("[12]{1}[09]{1}[0-9]{2}");
		Pattern PatternMonthDay = Pattern.compile("-(0[1-9]{1}|1[0-2]{1})-(0[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})");
		Matcher matchEnddateIdx, matchEnddateTS, matchYearIdx/* , matchYearTS */, matchMonthDayIdx, matchMonthDayTS;

		matchEnddateIdx = PatternEnddate.matcher(edtIdx);
		matchYearIdx = PatternYear.matcher(edtIdx);
		matchMonthDayIdx = PatternMonthDay.matcher(edtIdx);

		matchEnddateTS = PatternEnddate.matcher(edtTs);
		// matchYearTS = PatternYear.matcher(edtTs);
		matchMonthDayTS = PatternMonthDay.matcher(edtTs);

		boolean foundEdtTs = false;

		if (edtIdx != null && edtIdx.length() > 3
				&& (edtIdx.replaceAll("[12]{1}[09]{1}[0-9]{2}-?-?", "").length() == 0
						|| edtIdx.replaceAll("[12]{1}[09]{1}[0-9]{2}-\\d\\d-?", "").length() == 0)
				&& edtTs.length() > 4 && nlp.getAllIndexEndLocations(tsShort, Pattern.compile("M")).size() == 1) {
			edtIdx = edtIdx.substring(0, 4) + "-" + edtTs.substring(edtTs.length() - 5, edtTs.length());
		}

		// System.Xut.println("@nlp - matchEnddateIdx-----edtIdx==="+edtIdx);
		if (matchEnddateIdx.find()) {
			// System.Xut
			// .println("enddate already in column - no need for ts--edtIdx="
			// + edtIdx);
			return edtIdx;
		}

		if (matchYearIdx.find()) {
			yrIdx = edtIdx.substring(0, 4);
		}

		if (matchMonthDayIdx.find()) {
			moDayIdx = edtIdx.substring(edtIdx.length() - 6, edtIdx.length());
		}

		if (matchEnddateTS.find()) {
			yrTs = edtTs.substring(0, 4);
			// System.Xut.println("yrTs=" + yrTs);
			foundEdtTs = true;
		}

		if (matchMonthDayTS.find()) {
			moDayTs = edtTs.substring(edtTs.length() - 6, edtTs.length());
		}

		if (foundEdtTs && yrTs.equals(yrIdx)) {
			// System.Xut.println("years match - so edt is=" + edtTs);
			return edtTs;
		}

		if (foundEdtTs && moDayTs.equals(moDayIdx)) {
			// System.Xut.println("months match so edt is=" + edtTs);
			return edtTs;
		}

		// A. at PMPM - caller of this method filters correct PM
		// B. when I use replaceAll - wrong order will corrupt results. It will
		// first replace PMPM - so you can't then look for PMPMPM

		// TODO: FIX THIS - CAN ONLY USE IF EDT2 IS NOT VALID DATE ALREADY!
		Pattern patternPMY = Pattern.compile("^(PMPM|PMY|PM|MtM|M)$");
		Matcher matchPMY = patternPMY.matcher(tsShort);
		if (matchPMY.find() && edtIdx.replaceAll("-", "").length() < 8 && edtTs.replaceAll("-", "").length() > 3
				&& edtIdx.length() > 3) {
			edtIdx = edtIdx.substring(0, 4) + "-" + edtTs.substring(edtTs.length() - 5, edtTs.length());
			// System.Xut.println("appended month day of ts with year edtIdx ="
			// + edtIdx);
			return edtIdx;
		}

		if (edtIdx.replaceAll("[- ]", "").length() < 1 && foundEdtTs)
			return edtTs;

		else
			return edtIdx;
	}

	public static String getPer(String edtIdx, String edtTs, String perIdx, String perTs, String tsShort,
			boolean columnOneAndTwoSameYear, int totalColumns, int column) {

		String yrTs = "", yrIdx = "";
		// System.Xut.println("get per perTs=" + perTs);
		boolean foundPerTs = false, foundPerIdx = false;
		Pattern PatternPeriod = Pattern.compile("3|6|9|12");
		Pattern PatternYear = Pattern.compile("[12]{1}[09]{1}[0-9]{2}");
		Matcher matchYear, matchPeriod;
		matchYear = PatternYear.matcher(edtIdx);

		matchPeriod = PatternPeriod.matcher(perIdx);
		if (matchPeriod.find()) {
			foundPerIdx = true;
			return perIdx;
		}

		if (matchYear.find()) {
			yrIdx = edtIdx.substring(0, 4);
		}

		matchYear = PatternYear.matcher(edtTs);
		if (matchYear.find()) {
			yrTs = edtTs.substring(0, 4);
		}

		matchPeriod = PatternPeriod.matcher(perTs);
		if (matchPeriod.find()) {
			// System.Xut.println("perTs=" + perTs);
			foundPerTs = true;
		}

		// System.Xut.println("tsShort=" + tsShort + " foundPerIdx=" +
		// foundPerIdx
		// + " foundPerTs=" + foundPerTs + " yrTs=" + yrTs + " yrIdx="
		// + yrIdx + " perTs=" + perTs);

		if (!tsShort.equals("PPMYY") && !tsShort.equals("PPMYMY") && !foundPerIdx && foundPerTs
				&& (yrTs.equals(yrIdx) || yrIdx.length() < 1)) {
			// System.Xut.println("woohoo it works ts per to use is=" + perTs);
			perIdx = perTs;
		}

		// if only one ended (P) - then after removing all MY - there should be
		// only 1 P. And that can then be perIdx if perIdx not found
		if ((tsShort.replaceAll("[MY]{1}", "").length() == 1 || tsShort.equals("PMPM")) && !foundPerIdx) {
			perIdx = perTs;
		}

		// if tsShort=PPMYY - it could be 3-1999,6-1999 3-1998,6-1998 or
		// 3-1999,3-1998,6-1999,6-1998
		// if same year - then alternate P values! Add boolean variable:
		// 'samePeriod'
		if ((tsShort.equals("PPMYY") || tsShort.equals("PPMYMY")) && !foundPerIdx && foundPerTs && yrTs.equals(yrIdx)
				&& !columnOneAndTwoSameYear) {
//			NLP.pwNLP.append(NLP.println("woohoo it works ts period to use is=", perTs));
			perIdx = perTs;
		}

		if (tsShort.equals("PPM") && !columnOneAndTwoSameYear && totalColumns == 4 && !perIdx.equals("3")
				&& !perIdx.equals("6") && !perIdx.equals("9") && !perIdx.equals("12")) {
			if (column == 0 || column == 1) {
				perIdx = perTs;
			}
			if (column == 2 || column == 3) {
				perIdx = perTs;
			}

		}

		if (tsShort.equals("PPM") && columnOneAndTwoSameYear && totalColumns == 4 && !perIdx.equals("3")
				&& !perIdx.equals("6") && !perIdx.equals("9") && !perIdx.equals("12")) {
			if (column == 0 || column == 2) {
				perIdx = perTs;
			}
			if (column == 1 || column == 3) {
				perIdx = perTs;
			}
		}
		return perIdx;
	}

	public static int getDateDifferenceInMonths(String laterDate1, String earlierDate2) throws ParseException {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		Date dt1;
		if (laterDate1
				.replaceAll("[12]{1}[09]{1}[0-9]{2}-(0?[1-9]{1}|1[0-2]{1})-(0?[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})", "")
				.length() == 0) {
			dt1 = format.parse(laterDate1);
		} else
			return 0;
		Date dt2;
		if (earlierDate2
				.replaceAll("[12]{1}[09]{1}[0-9]{2}-(0?[1-9]{1}|1[0-2]{1})-(0?[1-9]{1}|[1-2]{1}[0-9]{1}|3[0-1]{1})", "")
				.length() == 0) {
			dt2 = format.parse(laterDate1);
		} else
			return 0;

		@SuppressWarnings("deprecation")
		int dtDifYr = dt1.getYear() - dt2.getYear();
		@SuppressWarnings("deprecation")
		int dtDifMo = dt1.getMonth() - dt2.getMonth();
		int day1 = Integer.parseInt(laterDate1.substring(laterDate1.length() - 2, laterDate1.length()));
		int day2 = Integer.parseInt(earlierDate2.substring(earlierDate2.length() - 2, earlierDate2.length()));
		int dayDiff = day1 - day2;
		int monthDiff;
		// System.Xut.println("dt1=" + dt1 + " dt2=" + dt2 + " dtDifYr=" +
		// dtDifYr
		// + " dtDifMo=" + dtDifMo + " dy1=" + day1 + " dy2=" + day2
		// + " dayDiff=" + dayDiff);
		if (dayDiff > 15) {
			monthDiff = (dtDifYr * 12 + dtDifMo) + 1;
			return monthDiff;
		} else {
			return monthDiff = (dtDifYr * 12 + dtDifMo);
		}
	}

	public static int getDataColumnCountBasedOnEachRows(String tableText) throws IOException {

		NLP nlp = new NLP();

		int numberOfColumns = 0;

		double totRows = 0;
		tableText = tableText.replaceAll("[\r\n]{1,3}", "\r");
		String[] tableTextRows = tableText.split("\r");
		String line;

		// key = number of data cols, value is count of rows with that many data
		// cols
		TreeMap<Integer, Integer> mapDataCols = new TreeMap<Integer, Integer>();

		for (int i = 0; i < tableTextRows.length; i++) {
			line = tableTextRows[i];
//			NLP.pwNLP.append(NLP.println("@nlp - tableText line prior to data map=", line));
			if (line.replaceAll("[ ]{1,}|[_=]{1,}|[-]{1,}", "").length() < 2)
				continue;

			Pattern simpleColumnPattern = Pattern.compile("(\t|[\t ]{2}|   )(\\(?\\.?\\d|-)");
			List<Integer> listRow = nlp.getAllIndexStartLocations(line, simpleColumnPattern);
			if (listRow.size() > 0) {
				// System.Xut.println("line==" + line);
				if (mapDataCols.containsKey(listRow.size())) {
					// System.Xut.println("1 listRow.size="+listRow.size());
					mapDataCols.put(listRow.size(), mapDataCols.get(listRow.size()) + 1);
				} else {
					// System.Xut.println("2 listRow.size="+listRow.size());
					mapDataCols.put(listRow.size(), 1);
				}
				totRows++;
			}

		}
		NLP.println("totRows=", totRows + " mapDataCols.size=" + mapDataCols.size());
		NLP.printMapIntInt("mapDataCols=", mapDataCols);

		// for (Map.Entry<Integer, Integer> entry : mapDataCols.entrySet()) {
		// System.Xut.println(" key|" + entry.getKey() + "| val|"
		// + entry.getValue() + "|");
		// }

		int val = 0, maxVal = 0, columnCount = 0;
		for (Map.Entry<Integer, Integer> entry : mapDataCols.entrySet()) {
			val = entry.getValue();
			if (maxVal <= val) {
				maxVal = val;
				columnCount = entry.getKey();
			}
		}

//		NLP.pwNLP.append(NLP.println("\rtotRows=",
//				totRows + "\rdataColCountOnRow=" + columnCount + "\rnumberOfRowsWithThisManyDataCols=" + maxVal));

		// FIX HERE ALSO TO REQUIRE MIN # OF ROWS IN TABLE W/ DATA TO AT LEAST
		// 10

		double percentRowsWithDataColCount = maxVal / totRows;

//		NLP.pwNLP.append(NLP.println("numberOfRowsWithThisManyDataCols/totRows=", "" + percentRowsWithDataColCount));
		if (percentRowsWithDataColCount > .7) {
			return (int) columnCount;
		}

		else
			return numberOfColumns;
	}

	public static Map<Integer, List<String[]>> hasPercentColumns(Map<Integer, List<String[]>> mapData,
			int numberOfCHsRatio) {

		Map<Integer, List<String[]>> mapDataTmp = new TreeMap<Integer, List<String[]>>();
		int key;
		for (Map.Entry<Integer, List<String[]>> entry : mapData.entrySet()) {
			key = entry.getKey();
			List<String[]> listStrAryTmp = new ArrayList<String[]>();
			for (int i = 0; i < entry.getValue().size(); i++) {
				if (!entry.getValue().get(i)[1].contains("%")) {
					if (entry.getValue().size() / numberOfCHsRatio == 2
							&& entry.getValue().get(i)[1].replaceAll("[ ]{1}", "").length() == 0) {
						continue;
					}
					String[] ary = { entry.getValue().get(i)[0], entry.getValue().get(i)[1] };
					// System.Xut.println("listStrAryTmp adding ary="+Arrays.toString(ary));
					// listStrAryTmp.add(ary);
				}
			}
			if (null != listStrAryTmp && listStrAryTmp.size() > 0) {
				mapDataTmp.put(key, listStrAryTmp);
			}
		}

		return mapData = mapDataTmp;

	}

	public static List<String> checkForTableNameLaterInStartGroup(String text) throws IOException {
		List<String> list = new ArrayList<String>();
		Pattern patternCHsbyTwoWs = Pattern.compile("[ \t]{3}[A-Za-z\\d\\$]{1,}|[ ]{2}[\\$\\d,\\(\\!]{5}");
		// System.Xut.println("texttext=" + text);

		Pattern patternTocWords = Pattern
				.compile("[A-Z0-9]{1}[ ]{5,100}\\d$|^ [ ].{1,150}\\d$|SIGNATURES|EXHIBIT|NOTES TO|PART I");
		// <<fix elsewhere this pattern
		Matcher matchTableName, matchCHs, matchToc;
		String[] textSplit = text.split("[\r\n]");
		String line = "", tablename = "";
		int eIdx = 0, sIdx = 0, cnt;
		int lineLength = 0, totLen = 0;
		for (int i = 0; i < textSplit.length; i++) {
			line = textSplit[i];
			lineLength = line.length();
			totLen = totLen + lineLength;
			cnt = 0;
//			NLP.pwNLP.append(NLP.println("checkForTableNameLaterInStartGroup line==", line));
			matchCHs = patternCHsbyTwoWs.matcher(line);
			matchToc = patternTocWords.matcher(line);
			Matcher matchYearYear = Pattern
					.compile("[ ]{2,}[12]{1}[09]{1}[0-9]{2}[ ]{2,}[12]{1}[09]{1}[0-9]{2}( ?$|[ ]{2,})").matcher(line);

			Matcher matchNumber = TableTextParser.patternSpaceNumberNoYearNoMo.matcher(line);

			// not fool proof - but prevents a tablename capture after year CHs
			// (2003 2002) or where tablename is 'cash flows from'
			if ((matchYearYear.find() && matchYearYear.start() > 20) || line.toLowerCase().contains("cash flows from")
					|| line.toLowerCase().contains("cash flow from")
					|| (matchNumber.find() && i > 1 && matchNumber.start() > 20)) {
				// number after 2nd line in CH can make it stop - but not in 1st
				// 2 lines given year year not found - hence i>1

				if (list.size() > 0) {
//					NLP.pwNLP.append(NLP.println("YearYear/cash flow", ""));
					return list;
				} else {
//					NLP.pwNLP.append(NLP.println("return null", ""));
					return null;
				}
			}

			NLP nlp = new NLP();
			int pgNo = 0;
			boolean isNum = false;
			List<String[]> listMatch = nlp.getAllEndIdxAndMatchedGroupLocs(line, patternCHsbyTwoWs);
			// is last match a pgNo (furtherst to right)? - if so - don't count
			if (listMatch.size() > 1) {
				isNum = nlp.isNumeric(listMatch.get(listMatch.size() - 1)[1].trim());
				if (isNum && nlp.getAllIndexEndLocations(listMatch.get(listMatch.size() - 1)[1],
						Pattern.compile("(?i)[a-zA-Z;]")).size() < 1) {
					pgNo = Integer.parseInt(listMatch.get(listMatch.size() - 1)[1].replaceAll("[,\\)\\$]", "")
							.replaceAll("\\(", "-").trim());
				}
				if (pgNo > 100 || pgNo < 1 || listMatch.get(listMatch.size() - 1)[1].contains("$")) {
					isNum = false;
				}
			}

			while (matchCHs.find() && !isNum && !matchToc.find()
					&& nlp.getAllIndexEndLocations(line, TableParser.ColumnHeadingPattern).size() > 0) {
				cnt++;
//				NLP.pwNLP.append(NLP.println("found CHs=" + line, " cnt=" + cnt));
			}

			if (cnt > 1) {
				if (list.size() > 0) {
//					NLP.pwNLP.append(NLP.println("return list", ""));
					return list;
				} else {
//					NLP.pwNLP.append(NLP.println("return null", ""));
					return null;
				}
			}

			matchTableName = TableParser.TableNamePattern.matcher(line);
			if (matchTableName.find()) {

//				NLP.pwNLP.append(
//						NLP.println("tablename found at checkForTableNameLaterInStartGroup on this line==", line));

				sIdx = matchTableName.start() + totLen - lineLength + i * 2 + 4;
				eIdx = matchTableName.end() + totLen - lineLength;
				tablename = matchTableName.group().replaceAll("\\)|\\(|\\$|\\'|\\*", "");
//				NLP.pwNLP.append(NLP.println("checkForTableNameLaterInStartGroup tablename=",
//						tablename + " eIdx=" + eIdx + " sIdx=" + sIdx));

				if (eIdx > 0) {

					list.clear();
					// System.Xut.println("sIdx=" + sIdx);
					list.add(sIdx + "");
					list.add(eIdx + "");
					list.add(tablename);
					// System.Xut.println("list.size=" + list.size());
				}

			}

		}
		return list;
	}

	public static int[] getDataMap(Elements dataRows) {

		// receive html data rows - and map their idx locations (what column
		// numbers they are in). dataRows may have CHs and this will filter them
		// out. Map is checked based on density count - did I see at least 6
		// numbers in each column mapped

		int[] intAry = null;

		NLP nlp = new NLP();
		Elements TDs;
		Element td, tr;
		Pattern alphabetPattern = Pattern.compile(" [A-Za-z]{1,} |[A-Za-z]{3,}");
		Pattern nmbPattern = Pattern.compile("[\\d]|-");
		int cnt = 0;
		boolean foundCH = false, potentialCH = false;
		int dataMarker, dataLength;
		String cellContents = "", rowContents = "";

		for (int row = 0; row < dataRows.size(); row++) {
			tr = dataRows.get(row);
			rowContents = tr.text();
			TDs = tr.getElementsByTag("td");
			cnt = 0;
			potentialCH = false;
			foundCH = false;

			for (int a = 0; a < TDs.size(); a++) {
				cellContents = TDs.get(a).text();

				Matcher matchAlph = alphabetPattern.matcher(cellContents);
				Matcher matchNmb = nmbPattern.matcher(cellContents);
				dataMarker = cellContents.replaceAll("[\\-]{1,}|(?i)N.{0,1}[MA]{1}.{0,1}", "").length();
				dataLength = cellContents.length();

				if (a > 0 && nlp.getAllIndexStartLocations(rowContents, TableParser.ColumnHeadingPattern).size() > 1
						&& nlp.getAllIndexStartLocations(cellContents, TableParser.ColumnHeadingPattern).size() > 0
						&& a == (TDs.size() - 1)) {
					foundCH = true;
					// foundCH - so will need to skip putting this row into map
				}

			}

		}

		return intAry;
	}

	public static boolean getYrCount(List<String> CHs) {
		NLP nlp = new NLP();
		boolean moreThanOneYearInCH = false;
		int yrCnt = 0;
		String yrsStr = "";
		for (int z = 0; z < CHs.size(); z++) {
			yrsStr = CHs.get(z);
			yrCnt = nlp.getAllIndexEndLocations(yrsStr, Pattern.compile("[12]{1}[09]{1}[0-9]{2}")).size();
			if (yrCnt > 1)
				moreThanOneYearInCH = true;
			break;
		}

		return moreThanOneYearInCH;
	}

	public static Elements getMultiLinesInTableHtmlRow(String tableHtml, int firstNonChLineNumberNotFound)
			throws IOException {

		/*
		 * this fixes a the data rows of a table where multi-lines have been identified
		 * in a single cell (where 2 numbers in one cell on separate lines). STEP1 it
		 * maps the lines by assigning each line in a cell a row number and inputting
		 * that into a map. That map will row #s for each line a cell that correspond to
		 * each line in each of the other cell in the multi line row so that later at
		 * STEP2 I can loop through map and join each line as if it were its own row.
		 * When I rejoin the lines as their own row I reconstitue the html table by
		 * inserting <td>, </tr> etc. This then returns the Elements dataRows (returns
		 * the html data rows - filters out the CH rows).
		 * 
		 * Key of reconstituted table is equal to original table row * 100 plus line
		 * number in cell *10 plus column number. However this does not incorporate
		 * columns spans - which I need to do in order to get proper column count
		 */

		// System.Xut.println("@NLP.getMultiLinesInTableHtmlRow");

		NLP nlp = new NLP();
		tableHtml = tableHtml.replaceAll("(?i)<BR>|</p>", " xx ").replaceAll("[ ]{3,}xx[ ]{3,}", "  ").replaceAll("\\$",
				"");
		// System.Xut.println("multi cell tableHtml=" + tableHtml + " |end");
		// need to set line separator prior to Jsoup. jSoupe returns text body
		// but not <BR>. <BR> is used to create line separators in same cell. I
		// use xx as market to show line separator later in process
		Document doc = Jsoup
				.parseBodyFragment(tableHtml.replaceAll("\\&nbsp;|\\xA0|\\&#160;", " ").replaceAll("\\&#151;", "-"));
		// System.Xut.println("multi cell tableHtml=" + tableHtml + " |end");
		Elements TRs = doc.getElementsByTag("tr");
		Elements TDs;
		Element tr, td;
		String cellText = "", priorColumnCellText = "", lineInCellText = "";
		String[] linesInCell;
		boolean multiLineCell = false, rownameWraps = false;
		Map<Integer, String[]> dataColMap = new TreeMap<Integer, String[]>();
		int priorColumnLinesInCell = 0, longRowTextNumber = 0, wrap = 0;
		// STEP1: creates a map that reviews each multi-line cell in a row and
		// break each line in a cell into its own row numbers ensuring a line
		// number in one cell of same row has the same row number of cell in
		// another column of same row at same line.

		int dataRowStart = firstNonChLineNumberNotFound;
		// System.Xut.println("@nlp.getMultiLinesInTableHtmlRow
		// firstNonChLineNumberNotFound=="+firstNonChLineNumberNotFound+
		// " TRs.size="+TRs.size());

		Pattern patternNmb = Pattern.compile(",[\\d]{3} |xx \\(?[\\d]{1,3}\\)? xx| \\(?[\\d]{1,3}\\.[\\d]{1,3}\\)? ");
		// Pattern patternHasNmbr = Pattern.compile("[\\d]{5,}.{1,7}[\\d]{5,}");
		// hasNmbr is where there are multi numbers in row
		// single

		Matcher matchNmb;
		int cntNmbrs = 0, k = 0;
		for (int tblRow = dataRowStart; tblRow < TRs.size(); tblRow++) {
			tr = TRs.get(tblRow);
			// System.Xut.println("@NLP.getMultiLinesInTableHtmlRow -- tr.text="
			// + tr.text());
			TDs = tr.getElementsByTag("td");
			// if there aren't at least 2 numbers in a table row it can't be a
			// row with multi-lines given there should be 5 or 6 lines in any
			// cell or at a minimum 3 columns each with 1 line and numbers in
			// each.

			cntNmbrs = nlp.getAllIndexEndLocations(tr.text(), patternNmb).size();
			if (cntNmbrs < 3) {
				// System.Xut.println("ck tablename in get multi lines");
				if (TDs.size() > 0 && TDs.get(0).text().length() > 0) {
					List<String> listTmp = nlp.getAllMatchedGroups(TDs.get(0).text(), TableParser.TableNamePattern);
					if (listTmp.size() > 0)
						tablenameLongFromMultiCell = listTmp.get(0);
					// if(tablenameLongFromMultiCell.length()>0)
					// System.Xut.println("tablenameLongFromMultiCell=="+tablenameLongFromMultiCell);
				}
				// If I continue past this it won't pickup below rows with just
				// one line each cell. At this point it should have confirmed
				// multiLines in cell. Anyway.
				// continue;
			}

			// System.Xut.println("number of columns in table. TDs.size="
			// + TDs.size());

			if (TDs.size() < 3) {
				// System.Xut.println("tblRow number at break="+tblRow);
				// List<Integer> listInt =
				// nlp.getAllIndexEndLocations(tableHtml,
				// Pattern.compile("(?i)</tr>"));
				// tableNextHtml = tableHtml.substring(listInt.get(tblRow));
				// System.Xut.println("tableNextHtml at
				// break===="+tableNextHtml.substring(0,1000));
				break;
			}

			multiLineCell = false;
			rownameWraps = false;
			// ck if rowname wraps and as a result has too few lines. If so
			// - which line is the one that wraps so later I can insert a hard
			// return prior to it
			// int cntCHs =0;
			for (int column = 0; column < TDs.size(); column++) {
				cellText = TDs.get(column).text();
				// cntCHs = nlp.getAllIndexStartLocations(cellText,
				// TableParser.ColumnHeadingPattern).size();
				if (cellText.replaceAll(" ", "").length() < 1)
					continue;
				matchNmb = patternNmb.matcher(cellText.replaceAll("\\$|\\(|\\)", ""));
				linesInCell = cellText.split("xx");
				// System.Xut.println("linesInCell="
				// + Arrays.toString(linesInCell) + "\rcolumn=" + column
				// + " priorColumnLinesInCell=" + priorColumnLinesInCell
				// + " linesInCell=" + linesInCell.length);

				if (column == 1 && priorColumnLinesInCell < linesInCell.length && priorColumnLinesInCell > 0) {
					// System.Xut.println("mismatch here.
					// priorColumnCellText="+priorColumnCellText);
					rownameWraps = true;
					// System.Xut.println("a true rownameWraps=" + rownameWraps
					// + " cellText=" + cellText);
				}
				priorColumnLinesInCell = linesInCell.length;
				priorColumnCellText = cellText;
			}

			// assumes col 0 is rowname column. Added complexity elsewise
			if (rownameWraps) {
				// System.Xut.println("b true rownameWraps=" + rownameWraps);
				lineInCellText = "";
				cellText = TDs.get(0).text();
				linesInCell = cellText.split("xx");
				for (int n = 0; n < linesInCell.length - 1; n++) {
					if (linesInCell[n].length() > lineInCellText.length()) {
						lineInCellText = linesInCell[n];
						// System.Xut.println("longRowTextNumber="+longRowTextNumber+"
						// linesInCell[n]="+linesInCell[n]);
						longRowTextNumber = n;
					}
				}
				// System.Xut.println("longRowTextNumber=="+longRowTextNumber);
			}

			String colSpanStr = "";
			int colSpanInt = 0, colSpanIntTotal = 0;
			for (int column = 0; column < TDs.size(); column++) {
				td = TDs.get(column);
				cellText = TDs.get(column).text();
				colSpanStr = td.attr("colspan");
				if (nlp.isNumeric(colSpanStr)) {
					colSpanInt = Integer.parseInt(colSpanStr) - 1;
					colSpanIntTotal = colSpanIntTotal + colSpanInt;
					// System.Xut.println("colSpanInt=" + colSpanInt + " col="
					// + column + " colSpanIntTotal=" + colSpanIntTotal
					// + " \rcolIdx=" + column + colSpanIntTotal
					// + " cellText=" + cellText);
					// this is the amount to add (adjust) each column number I
					// put into the data map so that col idx are consistent.
				}
				if (!nlp.isNumeric(colSpanStr)) {
					colSpanInt = 0;
				}
				if (cellText.replaceAll(" ", "").length() < 1)
					continue;
				// System.Xut.println("cellText=" + cellText);
				linesInCell = cellText.split("xx");
				for (int n = 0; n < linesInCell.length; n++) {
					lineInCellText = linesInCell[n];
					if (column > 0 && nlp.isNumeric(lineInCellText)) {
						multiLineCell = true;
					}

					/*
					 * by taking original table row and multiplying by 1000 then adding 10 times the
					 * line number of multi-line cell and then adding the col idx I have created a
					 * unique and new row # that can parallel with each line in subsequent multi
					 * line cell of same original table row
					 */

					// row = key/100, col#= last 2 digits
					k = (tblRow * 1000 + (n + 1) * 100 + (column + colSpanIntTotal - colSpanInt));
					// System.Xut.println("rownumber=" + ((n + 1) * 100)
					// + (column + colSpanIntTotal - colSpanInt)
					// + " cellText=" + cellText);
					// System.Xut.println("rownameWraps=" + rownameWraps
					// + " longRowTextNumber=" + longRowTextNumber + " n="
					// + n + " column=" + column);

					if (rownameWraps && n == longRowTextNumber && column == 0) {

						String[] strAry = { TDs.size() + "", (column + colSpanIntTotal - colSpanInt) + "", k + "",
								"LINE INSERT", linesInCell.length + "", tblRow + "" };
						dataColMap.put(k, strAry);
						// System.Xut
						// .println("1 if wrap putting in dataColMap strAry={total # of
						// cols,colIdx,key,line#,totalLines,tblRow}=="
						// + Arrays.toString(strAry)
						// + "\r colSpanInt="
						// + colSpanInt
						// + " column="
						// + column
						// + " tblRow="
						// + tblRow
						// + " lineNo="
						// + n
						// + " (column+colSpanIntTotal-colSpanInt)="
						// + (column + colSpanIntTotal - colSpanInt));
						wrap++;
					}
					// map value List<String[]>{[0]=total of all columns in
					// table,[1]=colIdxStart,[2]=key,[3]=celltext,[4]=total
					// lines in cell,[5]=true tbl row}
					String[] strAry = { TDs.size() + "", (column + colSpanIntTotal - colSpanInt) + "", k + "",
							lineInCellText, linesInCell.length + "", tblRow + "" };
					dataColMap.put(k, strAry);
					// System.Xut
					// .println("2 putting in dataColMap strAry={total # of
					// cols,colIdx*,key,line#,totalLines,tblRow}==\r"
					// + Arrays.toString(strAry)
					// + "\r colSpanInt="
					// + colSpanInt
					// + " column="
					// + column
					// + " tblRow="
					// + tblRow
					// + " lineNo="
					// + n
					// + " (column+colSpanIntTotal-colSpanInt)="
					// + (column + colSpanIntTotal - colSpanInt));
				}
			}
		}

		/*
		 * STEP2: reconstitute table html (add in order to reconstitute table I have to
		 * insert <td></td> and <tr></tr> at each col and row. I need to keep colIdx
		 * consistent so need to insert additional <td> based on colIdx of map of data
		 * col I'm reconstituting.
		 */

		// System.Xut.println("printing dataColMap==");
		NLP.printMapIntStringAry("dataColMap", dataColMap);
		StringBuffer sb = new StringBuffer("<table>");
		int prevRowNumber = 0, key = 0, totalColumns, colIdx = 0, rowNumber = 0, cnt = 0, tdInserted = 0,
				tdInsertedTmp = 0;
		for (Map.Entry<Integer, String[]> entry : dataColMap.entrySet()) {
			key = entry.getKey();
			// System.Xut.println("key="+key);
			// System.Xut.println("value="+Arrays.toString(entry.getValue()));
			colIdx = Integer.parseInt(entry.getValue()[1]);
			/*
			 * key = tblRow (when it was multiline) * 1000 + (lineNo+1)*100 + column. map
			 * value List<String[]>{[0]=total of all columns in
			 * table,[1]=colIdxStart,[2]=key,[3]=celltext,[4]=total lines in cell,[5]=true
			 * tbl row}
			 */

			// rownumber is converted by dividing key by 100 - I take the table
			// row * 1000 and add from row with multi line the line number *100.
			// So if table row 3 and I'm at the 5th line in multi lines row- key
			// is equal to 3500 and therefore the rownumber is 35. 35 is a
			// multiple of actual row count,
			rowNumber = Integer.parseInt(entry.getValue()[2]) / 100;
			cellText = entry.getValue()[3];
			// System.Xut.println("rowNumber==" + rowNumber + " cellText="
			// + cellText + "cnt=" + cnt);

			if (cnt > 0 && prevRowNumber == rowNumber) {
				tdInsertedTmp = 0;

				// add in a number of <td></td> pairs equal to colIdx. After 1st
				// colIdx though you have to account for prior <td> already
				// inserted. So tdCnt used to track (reset when new row)

				// System.Xut.println("prevRowNumber=rowNumber. colIdx="+
				// colIdx+" tdCnt="+tdInserted);
				for (int c = 0; c < (colIdx - tdInserted); c++) {
					sb.append("<td></td>");
					// System.Xut.println("<td></td>");
					tdInsertedTmp++;
				}

				tdInsertedTmp++;
				tdInserted = tdInserted + tdInsertedTmp;
				// System.Xut.println("tdCnt="+tdInserted+" tdInserted="+tdInsertedTmp);
				sb.append("<td>" + cellText + "</td>");
			}

			if (cnt == 0 || prevRowNumber != rowNumber) {
				tdInsertedTmp = 0;
				tdInserted = 0;
				// System.Xut.println("cnt==0 || prevRowNumber != rowNumber. colIdx="+
				// colIdx+" tdCnt="+tdInserted);
				sb.append("</tr>\r\n<tr>");
				// System.Xut.println("</tr>\r\n<tr>");

				// pickup here - track thru how it is insert <td>

				for (int c = 0; c < (colIdx - tdInserted); c++) {
					sb.append("<td></td>");
					// System.Xut.println("<td></td>");
					tdInsertedTmp++;
				}

				tdInsertedTmp++;
				tdInserted = tdInserted + tdInsertedTmp;
				// System.Xut.println("tdCnt="+tdInserted+" tdInserted="+tdInsertedTmp);
				sb.append("<td>" + cellText + "</td>");
				// System.Xut.println("<td>" + cellText + "</td>");
				// System.Xut.println("cnt==0 || prevRowNumber != rowNumber </tr><td>"
				// + cellText + "</td>");
			}

			cnt++;
			prevRowNumber = rowNumber;
		}
		sb.append("</table>");
		// System.Xut.println("printing reconstituted table==\r" +
		// sb.toString());

		Elements dataRows = new Elements();
		doc = Jsoup.parseBodyFragment(
				sb.toString().replaceAll("\\&nbsp;|\\xA0|\\&#160;", " ").replaceAll("\\&#151;", "-"));
		TRs = doc.getElementsByTag("tr"); // all row elements

		for (int i = 0; i < TRs.size(); i++) {
			dataRows.add(TRs.get(i));
		}
		// System.Xut.println("@nlp.getMultiLinesInTableHtmlRow -- return data
		// dataRows.Size==="+dataRows.size());
		// System.Xut.println("from multi-line -- printing final reformatted data
		// rows");
		// for (int i = 0; i < dataRows.size(); i++) {
		// TRs = doc.getElementsByTag("tr");
		// System.Xut.println("row=" + TRs.get(i).text() + "\r");
		// }
		return dataRows;
	}

	public static String getColumnTextDateDifference(String columnText) throws ParseException {
		// subtracts dates to get period - if columnText is:
		// "Period from October 13, 2004 through December 31, 2004"
		// it will return period string value of 3

		String period = "";
		int per = 0;

		NLP nlp = new NLP();

		// System.Xut.println("D. getColumnTextDateDifference columnTExt="
		// + columnText);
		columnText = (" " + nlp.getTableSentencePatterns(columnText, 0 + ""));
		// System.Xut.println("columnText using getTableSentencePatterns="
		// + columnText);
		String tsPattern = columnText.replaceAll("[\\|]{2,}", "\\|").replaceAll("'", "").replaceAll("[\r\n]", "");
		Pattern patternTSshort = Pattern.compile("(?<=(\\dL\\d))[PMYt](?=:)");

		Matcher matchTSshort = patternTSshort.matcher(tsPattern);
		String tsShort = "";
		while (matchTSshort.find()) {
			// System.Xut.println("matchTSshort.group=" + matchTSshort.group());
			tsShort = tsShort + matchTSshort.group();
		}

		// System.Xut.println("tsShort=" + tsShort);
		tsPattern = tsPattern.replaceAll("\\dL", "");

		if (tsShort.equals("MtMY")) {
			String[] tsPatternSplit = tsPattern.split("\\|");

			String laterDate = (nlp.getEnddatePeriodFromTS(
					tsPatternSplit[3].replaceAll("2M:", "") + " " + tsPatternSplit[4].replaceAll("1Y:", ""), " ",
					columnText))[0];

			String earlierDate = (nlp.getEnddatePeriodFromTS(
					tsPatternSplit[1].replaceAll("1M:", "") + " " + tsPatternSplit[4].replaceAll("1Y:", ""), " ",
					columnText))[0];

			// System.Xut.println("MyMY at columnText earlierDate=" +
			// earlierDate
			// + " laterDate=" + laterDate);
			per = getDateDifferenceInMonths(laterDate, earlierDate);

			// System.Xut.println("MtMY per=" + per);
			if (per != 3 && per != 6 && per != 9 && per != 12) {
				per = 0;
			}
		}

		if (tsShort.equals("MYtMY")) {
			String[] tsPatternSplit = tsPattern.split("\\|");

			String laterDate = (nlp.getEnddatePeriodFromTS(
					tsPatternSplit[4].replaceAll("2M:", "") + " " + tsPatternSplit[5].replaceAll("2Y:", ""), " ",
					columnText))[0];

			String earlierDate = (nlp.getEnddatePeriodFromTS(
					tsPatternSplit[1].replaceAll("1M:", "") + " " + tsPatternSplit[2].replaceAll("1Y:", ""), " ",
					columnText))[0];

			// System.Xut.println("earlierDate=" + earlierDate + " laterDate1="
			// + laterDate);
			per = getDateDifferenceInMonths(laterDate, earlierDate);

			// System.Xut.println("MYtMY per=" + per);
			if (per != 3 && per != 6 && per != 9 && per != 12) {
				per = 0;
			}
		}

		period = per + "";

		return period;

	}

	public static List<Integer> getCountOfYearMonthEndedFromList(List<String[]> listCHandMPidx) {
		NLP nlp = new NLP();

		List<Integer> list = new ArrayList<>();
		int cntE = 0;
		int cntM = 0;
		int cntY = 0;

		for (int i = 0; i < listCHandMPidx.size(); i++) {
			if (nlp.getAllIndexStartLocations(listCHandMPidx.get(i)[1], TableParser.MonthPattern).size() > 0) {
				cntM = nlp.getAllIndexStartLocations(listCHandMPidx.get(i)[1], TableParser.MonthPattern).size() + cntM;
			}

			if (nlp.getAllIndexStartLocations(listCHandMPidx.get(i)[1], TableTextParser.patternMoDayYear).size() > 0) {
				cntM = nlp.getAllIndexStartLocations(listCHandMPidx.get(i)[1], TableTextParser.patternMoDayYear).size()
						+ cntM;
			}

			if (nlp.getAllIndexStartLocations(listCHandMPidx.get(i)[1], TableParser.YearSimple).size() > 0) {
				cntY = nlp.getAllIndexStartLocations(listCHandMPidx.get(i)[1], TableParser.YearOrMoDayYrPattern).size()
						+ cntY;

				// System.Xut.println("listCHandMPidx.get(i)[1]="
				// + listCHandMPidx.get(i)[1] + " cntY=" + cntY);
			}

			if (nlp.getAllIndexStartLocations(listCHandMPidx.get(i)[1], TableParser.EndedHeadingPattern).size() > 0) {
				cntE = nlp.getAllIndexStartLocations(listCHandMPidx.get(i)[1], TableParser.EndedHeadingPattern).size()
						+ cntE;
			}
		}
		list.add(cntY);
		list.add(cntM);
		list.add(cntE);

		return list;
	}

	public static List<Integer> getMidpoints(List<String[]> listOfStringArray) {
		List<Integer> list = new ArrayList<Integer>();

		// System.Xut.println("getting midpoints");
		int mp = 0;
		String mpStr = "";
		for (int i = 0; i < listOfStringArray.size(); i++) {
			mpStr = listOfStringArray.get(i)[0];
			// System.Xut.println("mpStr=" + mpStr);
			if (mpStr.indexOf(".") > 0)
				mpStr = mpStr.substring(0, mpStr.indexOf("."));

			mp = (int) (Integer.parseInt(mpStr) - (listOfStringArray.get(i)[1].length() * .5));
			list.add(mp);
		}
		return list;
	}

	public static List<String[]> get3to2(List<String[]> largeList, List<String[]> smallList, boolean reverse,
			List<String[]> mergedList, double maxDist) {

		boolean pairedSm1 = false, pairedSm2 = false, pairedLg1 = false, pairedLg2 = false, pairedLg3 = false;
		String lgCh1 = largeList.get(0)[1].trim();
		String lgCh2 = largeList.get(1)[1].trim();
		String lgCh3 = largeList.get(2)[1].trim();
		String smCh1 = smallList.get(0)[1].trim();
		String smCh2 = smallList.get(1)[1].trim();
		double lgChMp1 = Double.parseDouble(largeList.get(0)[0]);
		double lgChMp2 = Double.parseDouble(largeList.get(1)[0]);
		double lgChMp3 = Double.parseDouble(largeList.get(2)[0]);
		double smChMp1 = Double.parseDouble(smallList.get(0)[0]);
		double smChMp2 = Double.parseDouble(smallList.get(1)[0]);
		double lgMp1to2 = (lgChMp1 + lgChMp2) / 2;
		double lgMp2to3 = (lgChMp2 + lgChMp3) / 2;

		double lgChEndIdx1 = Double.parseDouble(largeList.get(0)[0]) + lgCh1.length() / 2;
		double lgChEndIdx2 = Double.parseDouble(largeList.get(1)[0]) + lgCh2.length() / 2;
		double lgChEndIdx3 = Double.parseDouble(largeList.get(2)[0]) + lgCh2.length() / 2;
		double smChEndIdx1 = Double.parseDouble(smallList.get(0)[0]) + smCh1.length() / 2;
		double smChEndIdx2 = Double.parseDouble(smallList.get(1)[0]) + smCh2.length() / 2;

//		NLP.pwNLP.append(NLP.println("3:2 ratio. lgCh1=", lgCh1 + " lgCh2=" + lgCh2 + " lgCh3=" + lgCh3));
//		NLP.pwNLP.append(NLP.println("3:2 ratio. smCh1=", smCh1 + " smCh2=" + smCh2));
//		NLP.pwNLP.append(NLP.println("3:2 ratio. lgMp1=", lgChMp1 + " lgChMp2=" + lgChMp2 + " lgChMp3=" + lgChMp3));
//		NLP.pwNLP.append(NLP.println("3:2 ratio. smChMp1=", smChMp1 + " smChMp2=" + smChMp2));

		// what if lgCh1 is stranded or lgCh2 is stranded or lgCh3 is stranded?
		// - if any lgCh is stranded then smCh1 and smCh2 are one to one.

		if (Math.abs(lgChMp1 - smChMp1) < 2) {

			pairedLg1 = true;
			pairedSm1 = true;
			String[] ary = { lgChMp1 + "", lgCh1 + " " + smCh1 };
			mergedList.add(ary);

		}

		if (Math.abs(lgChMp2 - smChMp1) < 2 && !pairedLg1 && !pairedSm1) {
			pairedLg2 = true;
			pairedSm1 = true;
			pairedLg1 = true;

			String[] ary = { lgChMp1 + "", lgCh1 };
			mergedList.add(ary);

			String[] ary2 = { lgChMp2 + "", lgCh2 + " " + smCh1 };
			mergedList.add(ary2);

		}

		if (Math.abs(lgChMp2 - smChMp2) < 2 && pairedLg1 && pairedSm1) {
			pairedSm2 = true;
			pairedLg2 = true;

			String[] ary = { lgChMp2 + "", lgCh2 + " " + smCh2 };
			mergedList.add(ary);

		}

		if (Math.abs(lgChMp3 - smChMp2) < 2 && (pairedLg1 || pairedLg2) && pairedSm1) {

			String[] ary = { lgChMp3 + "", lgCh3 + " " + smCh2 };
			mergedList.add(ary);

			pairedSm2 = true;
			pairedLg3 = true;
		}

		if (!pairedSm1 || !pairedSm2 || !pairedLg1 || !pairedLg2 || !pairedLg3) {
			mergedList.clear();
		}

		if (pairedSm1 && pairedSm2 && pairedLg1 && pairedLg2 && pairedLg3) {
			return mergedList;
		}

		// System.Xut.println("pairedSm1=" + pairedSm1 + " pairedSm2=" + pairedSm2 + "
		// pairedLg1=" + pairedLg1
		// + " pairedLg2=" + pairedLg2 + " pairedLg3=" + pairedLg3);

		pairedSm1 = false;
		pairedSm2 = false;
		pairedLg1 = false;
		pairedLg2 = false;
		pairedLg3 = false;

		// TODO: must meet mpDist. AND if not met need to add to lg w/o
		// sm ch. Must then return mergedList here!
		if (Math.abs(lgMp1to2 - smChMp1) < maxDist) {
			pairedLg1 = true;
			pairedLg2 = true;
			pairedSm1 = true;

			if (!reverse) {
				String[] ary = { lgChMp1 + "", smCh1 + " " + lgCh1 };
				mergedList.add(ary);
//				NLP.pwNLP.append(NLP.println("3:2 ratio-smCh paired w/ (2 cols) lgCh1=", Arrays.toString(ary)));

				String[] ary2 = { lgChMp2 + "", smCh1 + " " + lgCh2 };
				mergedList.add(ary2);
//				NLP.pwNLP.append(NLP.println("3:2 ratio-smCh paired w/ (2 cols) lgCh2=", Arrays.toString(ary)));
			}
			if (reverse) {
				String[] ary = { lgChMp1 + "", lgCh1 + " " + smCh1 };
				mergedList.add(ary);
//				NLP.pwNLP.append(NLP.println("3:2 ratio-smCh paired w/ (2 cols) lgCh1=", Arrays.toString(ary)));

				String[] ary2 = { lgChMp2 + "", lgCh2 + " " + smCh1 };
				mergedList.add(ary2);
//				NLP.pwNLP.append(NLP.println("3:2 ratio-smCh paired w/ (2 cols) lgCh2=", Arrays.toString(ary)));
			}
		}

		if (!pairedSm1 && Math.abs(lgChMp1 - smChMp1) < maxDist || Math.abs(lgChEndIdx1 - smChEndIdx1) < 2) {
			pairedLg1 = true;
			pairedSm1 = true;

			if (!reverse) {
				String[] ary = { lgChMp1 + "", smCh1 + " " + lgCh1 };
				mergedList.add(ary);
//				NLP.pwNLP.append(NLP.println("3:2 ratio-smCh paired w/ (1 cols) lgCh1=", Arrays.toString(ary)));
			}

			if (reverse) {
				String[] ary = { lgChMp1 + "", lgCh1 + " " + smCh1 };
				mergedList.add(ary);
//				NLP.pwNLP.append(NLP.println("3:2 ratio-smCh paired w/ (2 cols) lgCh1=", Arrays.toString(ary)));
			}
		}

		if (!pairedLg2 && Math.abs(lgMp2to3 - smChMp2) < maxDist) {
			pairedLg2 = true;
			pairedLg3 = true;
			pairedSm2 = true;

			if (!reverse) {
				String[] ary = { lgChMp2 + "", smCh2 + " " + lgCh2 };
				mergedList.add(ary);
//				NLP.pwNLP.append(NLP.println("3:2 ratio-smCh2 paired w/ (2 cols) lgCh2=", Arrays.toString(ary)));

				String[] ary2 = { lgChMp3 + "", smCh2 + " " + lgCh3 };
				mergedList.add(ary2);
//				NLP.pwNLP.append(NLP.println("3:2 ratio-smCh2 paired w/ (2 cols) lgCh3=", Arrays.toString(ary2)));
			}
			if (reverse) {
				String[] ary = { lgChMp2 + "", lgCh2 + " " + smCh2 };
				mergedList.add(ary);
//				NLP.pwNLP.append(NLP.println("3:2 ratio-smCh2 paired w/ (2 cols) lgCh2=", Arrays.toString(ary)));

				String[] ary2 = { lgChMp3 + "", lgCh3 + " " + smCh2 };
				mergedList.add(ary2);
//				NLP.pwNLP.append(NLP.println("3:2 ratio-smCh2 paired w/ (2 cols) lgCh3=", Arrays.toString(ary)));
			}
		}

		if (!pairedSm2 && Math.abs(lgChMp3 - smChMp2) < maxDist || Math.abs(lgChEndIdx3 - smChEndIdx2) < 2) {
			pairedLg3 = true;
			pairedSm2 = true;

			if (!reverse) {
				String[] ary = { lgChMp3 + "", smCh2 + " " + lgCh3 };
				mergedList.add(ary);
//				NLP.pwNLP.append(NLP.println("3:2 ratio-smCh2 paired w/ (1 cols) lgCh3=", Arrays.toString(ary)));
			}

			if (reverse) {
				String[] ary = { lgChMp3 + "", lgCh3 + " " + smCh2 };
				mergedList.add(ary);
//				NLP.pwNLP.append(NLP.println("3:2 ratio-smCh2 paired w/ (2 cols) lgCh3=", Arrays.toString(ary)));
			}
		}

		if (pairedLg1 && pairedLg2 && pairedLg3 && pairedSm1 && pairedSm2)
			return mergedList;

		// System.Xut.println("returned null");
		return null;

	}

	public static String getMergedCH(String lgCHStr, String smCHStr, boolean reverse) {
		String mergCh = "";

		if (!reverse) {
			mergCh = smCHStr + " " + lgCHStr;
		} else {
			mergCh = lgCHStr + " " + smCHStr;
		}

		return mergCh;

	}

	public static int getMonthDayDifference(String monthMM1, String monthMM2, String dayDD1, String dayDD2) {
		int difference = -1;

		// getMonthDayDifference("10","09","01","30");

		SimpleDateFormat myFormat = new SimpleDateFormat("dd MM yyyy");
		String inputString1 = dayDD1 + " " + monthMM1 + " " + "2001";
		String inputString2 = dayDD2 + " " + monthMM2 + " " + "2001";

		// System.Xut.println(inputString1);

		// System.Xut.println(inputString2);
		try {
			Date date1 = myFormat.parse(inputString1);
			Date date2 = myFormat.parse(inputString2);
			// System.Xut.println("date1:" + date1);
			// System.Xut.println("date2:" + date2);
			long diff = date1.getTime() - date2.getTime();
			difference = (int) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
			// System.Xut.println("Days: "
			// + TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return difference;
	}

	public static boolean monthYearFound(Map<Integer, String> map) throws IOException {
		boolean done = true;
		NLP nlp = new NLP();

		if (null == map || map.size() < 1)
			return false;

		String str;
		int cntM, cntY, cntE;
		for (Map.Entry<Integer, String> Entry : map.entrySet()) {
			str = Entry.getValue();

			cntE = (nlp.getAllMatchedGroups(str, TableParser.EndedHeadingPattern)).size();

			cntM = (nlp.getAllMatchedGroups(str, TableParser.MonthPatternSimple)).size();
			cntY = (nlp.getAllMatchedGroups(str, TableParser.YearOrMoDayYrPattern)).size();
			if (cntY != 1) {
				done = false;
				break;
			}
			if (cntM != 1 && cntE != 1) {
				done = false;
				break;
			}

			if (done)
				break;
		}
		return done;
	}

	public static Map<Integer, String> addBackInceptionColumn(Map<Integer, String> mapMonthYear,
			Map<Integer, String> mapInceptionAllRelatedRows) {

		Map<Integer, String> mapFinal = new TreeMap<Integer, String>();

		int cnt = 0, key = 0, mp, prevMp = 0;
		String prevStr = "", str = "";
		for (Map.Entry<Integer, String> entry : mapInceptionAllRelatedRows.entrySet()) {
			key = entry.getKey();
			cnt++;
			str = entry.getValue().trim();
			mp = (key - key / 1000 * 1000);
			if (cnt > 1 && Math.abs(prevMp - mp) > 4.6)
				return null;
			else {
				str = str.trim() + " " + prevStr.trim();
			}
			prevStr = str;
			prevMp = mp;
		}

		// System.Xut.println("INCEPT str="+str);
		mapMonthYear.put(key, "|INCEPT|" + " " + str);
		return mapMonthYear;

	}

	public static Map<Integer, String> getAllInceptionRelatedRows(List<String[]> listKeysToRemove,
			Map<Integer, String> mapInceptionAllRelatedRows) {

		int key = 0;
		if (null != listKeysToRemove && listKeysToRemove.size() > 0) {
			for (int i = 0; i < listKeysToRemove.size(); i++) {
				// add back and mark col=inception
				key = Integer.parseInt(listKeysToRemove.get(i)[0]);
				mapInceptionAllRelatedRows.put(key, listKeysToRemove.get(i)[1]);
			}
		}

		return mapInceptionAllRelatedRows;
	}

	public static List<String[]> getListOfKeysToRemove(Map<Integer, String> MapToRemoveKeysFrom, int mp) {
		// if I pass two map parameters thru this method - it will add to a list
		// all keys and related string values to listKeysToRemove provided the
		// mp value of the related key and the parameter mp is w/n 3. I use this
		// to remove columns that have inception or other hard to pass column
		// headers in TableTextParser

		List<String[]> listKeysToRemove = new ArrayList<>();

		// System.Xut.println("get list of keys to remove");
		int key, midpt;
		String str;
		for (Map.Entry<Integer, String> entry : MapToRemoveKeysFrom.entrySet()) {
			key = entry.getKey();

			midpt = key - (key / 1000) * 1000;
//			NLP.pwNLP.append(NLP.println("entry.getValue()",
//					entry.getValue() + " incept mp=" + mp + " mp to compare to=" + midpt));

			str = entry.getValue();
			if (Math.abs(mp - midpt) < 5.1) {
				String[] ary = { key + "", str };
				listKeysToRemove.add(ary);
				// System.Xut.println("key being removed="+key+" str="+str+" str midpt="+midpt+"
				// beacon mp="+mp);
			}
		}

		return listKeysToRemove;
	}

	public static List<Integer> getListOfKeysOfMap(Map<Integer, String> map) {

		if (null == map)
			return null;
		List<Integer> listOfKeys = new ArrayList<Integer>();
		for (Map.Entry<Integer, String> lgEntry : map.entrySet()) {
			listOfKeys.add(lgEntry.getKey());
		}
		return listOfKeys;

	}

	public static Integer getMaxNumberOfChsOnAnyChRow(Elements chRows) {
		Integer maxNumberOfChOnRow = 0;

		NLP nlp = new NLP();
		Element td = null;
		Elements TDs;

		String cellTxt = "";
		int chCnt = 0;
		for (int row = 0; row < chRows.size(); row++) {
			TDs = chRows.get(row).getElementsByTag("td");
			chCnt = 0;
			for (int col = 0; col < TDs.size(); col++) {
				td = TDs.get(col);
				cellTxt = td.text().replaceAll(" ?\\*", "").replaceAll(" ?(\\(|\\[)[\\dA-Za-z]{1}(\\]|\\))", "");
				// System.Xut.println("maxNumberOfChOnRow cellTxt="+cellTxt);
				if (nlp.getAllIndexStartLocations(cellTxt, TableParser.ColumnHeadingPattern).size() > 0) {
					chCnt++;
					if (chCnt > maxNumberOfChOnRow) {
						maxNumberOfChOnRow = chCnt;
					}
				}
			}
		}
		return maxNumberOfChOnRow;
	}

	public static Map<Integer, Integer> getStartIdxOfEachDataCol(Elements dataRows)
			throws IOException, ParseException, SQLException {

		NLP nlp = new NLP();
		Element td = null;
		Elements TDs;

		String colSpanStr = "";
		int colSpan = 0;

		String data = "";
		;

		Map<Integer, Integer> mapDataColIdxsCount = new TreeMap<Integer, Integer>();
		// mapDataColIdx: key=data col idx (col #), value=number of times found.

		for (int i = 0; i < Math.min(dataRows.size(), 12); i++) {
			TDs = dataRows.get(i).getElementsByTag("td");
			// data col can't be 0. first data col idx shouldn't be after col 5
			for (int n = 1; n < TDs.size(); n++) {
				colSpan = 0;
				td = TDs.get(n);
				colSpanStr = td.attr("colspan").replaceAll("\"", "");
				if (colSpanStr.length() > 0 && nlp.isNumeric(colSpanStr) && !colSpanStr.contains(".")) {
					// System.Xut.println("colSpanStr="+colSpanStr);
					colSpan = Integer.parseInt(colSpanStr.replaceAll("\"", "").trim()) - 1;
				}
				data = TDs.get(n).text();
				// System.Xut.println("@nlp.getStartIdxOfEachDataCol potential td data txt
				// ==="+data);
				if (data.replaceAll("[ -\\$\\(\\)\\.]", "").length() > 0
						&& data.replaceAll("[\\$\\(\\) \\d\\-,\\.]", "").length() == 0) {
					// System.Xut.println("@nlp.getStartIdxOfEachDataCol data="+data);
					if (null != mapDataColIdxsCount.get(n + colSpan)) {
						// System.Xut.println("mapDataColIdxs.put key="+(n+colSpan)+"
						// count="+(mapDataColIdxsCount.get(n+colSpan)+1));
						mapDataColIdxsCount.put(n + colSpan, mapDataColIdxsCount.get(n + colSpan) + 1);
					} else {
						mapDataColIdxsCount.put(n + colSpan, 1);
						// System.Xut.println("mapDataColIdxs.put key=" + (n
						// + colSpan) + " count="+1);

					}
				}
			}
		}

		NLP.printMapIntInt("bef removing keys. mapDataColIdxsCount", mapDataColIdxsCount);

		Map<Integer, Integer> tmpMapDataColIdxsCount = new TreeMap<Integer, Integer>();
		int key = 0, val = 0;
		for (Map.Entry<Integer, Integer> entry : mapDataColIdxsCount.entrySet()) {
			key = entry.getKey();
			val = entry.getValue();
			if (val < 3)
				continue;
			else {
				tmpMapDataColIdxsCount.put(key, val);
			}
		}

		mapDataColIdxsCount = tmpMapDataColIdxsCount;

		NLP.printMapIntInt("aft removing keys. mapDataColIdxsCount", mapDataColIdxsCount);

		return mapDataColIdxsCount;
	}

	public static boolean allColHeadingsCaptured(String startGroupLimited, Map<Integer, String> mapFinal)
			throws IOException {

		NLP nlp = new NLP();
		if (mapFinal == null)
			return false;
		String str = "";
		// System.Xut.println("startGroupLimited==" + startGroupLimited);
		int cntM = (nlp.getAllMatchedGroups(startGroupLimited, TableParser.MonthPatternSimple)).size();
		int cntY = (nlp.getAllMatchedGroups(startGroupLimited, TableParser.YearOrMoDayYrPattern)).size();
		int cntE = (nlp.getAllMatchedGroups(startGroupLimited, TableParser.EndedHeadingPattern)).size();
		int cntE2 = (nlp.getAllMatchedGroups(startGroupLimited, Pattern.compile("(?i)(to |from|through|fiscal)")))
				.size();

		cntE = cntE + cntE2;

		List<Integer> listOfKeysOfMap = NLP.getListOfKeysOfMap(mapFinal);

		int cntMck = 0, cntYck = 0, cntEck3 = 0, cntEck = 0, cntE2ck = 0;
		for (int i = 0; i < listOfKeysOfMap.size(); i++) {
			str = mapFinal.get(listOfKeysOfMap.get(i));
			// multiply key by 1000 and add mp. lowest value is col1 and if two
			// items in map share same first dig - same row and map is in error
			// (can't have two years on same row for example)
			// System.Xut.println("str===============" + str);
			cntMck = (nlp.getAllMatchedGroups(str, TableParser.MonthPatternSimple)).size() + cntMck;
			cntYck = (nlp.getAllMatchedGroups(str, TableParser.YearOrMoDayYrPattern)).size() + cntYck;
			cntEck = (nlp.getAllMatchedGroups(str, TableParser.EndedHeadingPattern)).size();
			cntE2ck = (nlp.getAllMatchedGroups(str, Pattern.compile("(?i)(to |from|through|fiscal)"))).size();
			cntEck3 = cntEck + cntE2ck + cntEck3;
		}

		// System.Xut.println("cntMck=" + cntMck + " cntYck=" + cntYck
		// + " cntEck3=" + cntEck3);
		// System.Xut.println("cntM=" + cntM + " cntY=" + cntY + " cntE=" +
		// cntE);

		// if not at least as many for each category - skip.
		if (cntM > cntMck || cntE > cntEck3 || cntY > cntYck)
			return false;

		return true;

	}

	public static Map<Integer, String> mergeOneToOne(Map<Integer, String> mapLg, Map<Integer, String> mapSm,
			boolean monthYearDone, int totalColumns, int cntM, int cntY, int cntInception) {

		NLP nlp = new NLP();
		if (monthYearDone)
			return mapLg;

		if (cntInception > 0) {
			cntInception = 1;
		} else
			cntInception = 0;

//		NLP.pwNLP.append(NLP.println("mergeOneToOne cntM or cntE=",
//				cntM + " cntY=" + cntY + " monthYearDone or monthYearDoneEnded=" + monthYearDone + " totalColumns="
//						+ totalColumns + " cntI (0 or 1 if inception col exists)=" + cntInception));

		if (cntY > 0 && cntM > 0 && (totalColumns - cntInception) <= 3 && cntM / cntY == 1 && cntY % cntM == 0) {

			NLP.printMapIntStr("1 small map at mergeColHdgsOneToOneRatio. small map", mapSm);
			NLP.printMapIntStr("1 lg map at mergeColHdgsOneToOneRatio. lg map", mapLg);

			Map<Integer, String> mapOneToOne = new TreeMap<Integer, String>();
			boolean monthFound = false;
			@SuppressWarnings("unused")
			double yrMp = 0, moMp = 0, yrSidx = 0, moSidx = 0, yrEidx;
			int yrKey = 0, moKey, cnt = 0;
			String yrStr, moStr;

			for (Map.Entry<Integer, String> entryYr : mapLg.entrySet()) {
				// if (cnt > 0 && !monthFound)
				// return null;
				// find mo for each yr
				// System.Xut.println("map1 loop");
				yrKey = entryYr.getKey();
				yrStr = entryYr.getValue().trim();
				yrMp = yrKey - (yrKey / 1000) * 1000;
				// yrSidx b/c it is based on length of CH - which after merge
				// chgs. So I have to capture at initial capture of year map
				if (mapLg.size() == TableTextParser.mapYearStartIdx.size()
						&& null != TableTextParser.mapYearStartIdx.get(yrKey)) {
					yrSidx = TableTextParser.mapYearStartIdx.get(yrKey);
					yrEidx = (yrMp - yrSidx) + yrMp;
				}

				monthFound = false;
				cnt++;
				for (Map.Entry<Integer, String> entryMo : mapSm.entrySet()) {
					moKey = entryMo.getKey();
					moStr = entryMo.getValue().trim();
					moMp = moKey - (moKey / 1000) * 1000;
					moSidx = moMp - moStr.length() / 2;

//					NLP.pwNLP.append(NLP.println("mergeOneToOne. MoStr=", moStr + " moMp=" + moMp + " yrStr=" + yrStr
//							+ " yrMp=" + yrMp + " moSidx=" + moSidx + " yrSidx=" + yrSidx));
					/*
					 * Can't be same row (mp<=2 filters this) && MPs very close. !contains prevents
					 * duplicate entry if year ended june 30 (would get captured in ended and month
					 * routines otherwise)
					 */
					if (((!yrStr.trim().contains(moStr.trim()))
							|| (nlp.getAllIndexStartLocations(yrStr, NLP.monthSimplePattern).size() > 0
									&& !yrStr.trim().contains(moStr.trim())))
							&& (Math.abs(moMp - yrMp) <= 2 || Math.abs(moSidx - yrSidx) <= 1.6) && !monthFound) {
						mapOneToOne.put(yrKey, moStr + " " + yrStr);
						// System.Xut.println("put yrKey=" + yrKey +
						// " MoStr+yrStr="
						// + moStr + " yrStr=" + yrStr);
						monthFound = true;
						break;
					}
				}

				// System.Xut.println("monthFound=" + monthFound);
				if (!monthFound) {
					mapOneToOne.put(yrKey, yrStr);
					// System.Xut.println("put yrStr=" + yrStr);
				}
			}
			return mapOneToOne;
		} else
			return null;
	}

	public static Map<Integer, String> mergeTwoToOne(Map<Integer, String> mapSmallest, Map<Integer, String> mapLargest,
			boolean monthYearDone, int cntM, int cntY) {
		// map1 is smaller map, but always use mp of larger map.

//		NLP.pwNLP.append(NLP.println("mergeTwoToOne cntM or cntE=",
//				cntM + " cntY=" + cntY + " monthYearDone or monthYearDoneEnded=" + monthYearDone));

		if (monthYearDone)
			return mapLargest;

		if (cntY > 0 && cntM > 0 && !monthYearDone && cntM != 0 && cntY != 0 && cntY / cntM == 2 && cntY % cntM == 0) {

//			NLP.pwNLP.append(NLP.println("mergeColHdgsTwoToOneRatio -- mapSmallest.size=",
//					mapSmallest.size() + "" + " mapLargest.size=" + mapLargest.size()));
			Map<Integer, String> mapTwoToOne = new TreeMap<Integer, String>();
			boolean lgFound = false;
			int lgMp, prevLgMp = 0, smMp, lgKey, prevLgKey = 0, smKey, cntS = 0;
			String lgStr, prevLgStr = null, smStr;

			/*
			 * reorder map -- such that if 2 years are on different rows I order based
			 * solely on horizontal index locations unless both years are very close in idx
			 * value as they are then on top of each other and not sep columns. By
			 * reordering - I can digest the following CH:
			 */
			Map<Integer, String> mapReorderLarge = new TreeMap<Integer, String>();
			int rK1 = 0, rK2 = 0, rMp1 = 0, rMp2 = 0, rCnt = 0;
			String rStr1 = "", rStr2 = "";
			@SuppressWarnings("unused")
			boolean reordered = false;
			// ck if each mp is at leat 5 idx space apart
			if (mapLargest.size() == 2) {
				for (Map.Entry<Integer, String> entry : mapLargest.entrySet()) {
					rCnt++;
					if (rCnt == 1) {
						rK1 = entry.getKey();
						rMp1 = rK1 - (rK1 / 1000) * 1000;
						rStr1 = entry.getValue();
						// System.Xut.println("rMp1=" + rMp1 + " rStr1=" +
						// rStr1);
					}
					if (rCnt == 2) {
						rK2 = entry.getKey();
						rMp2 = rK2 - (rK2 / 1000) * 1000;
						rStr2 = entry.getValue();
						// System.Xut.println("rMp2=" + rMp2 + " rStr2=" +
						// rStr2);
					}
				}

				if (Math.abs(rMp2 - rMp1) > 5 && rMp1 > rMp2) {
					// dummy row=9
					mapReorderLarge.put(rMp1 + 9000, rStr1);
					mapReorderLarge.put(rMp2 + 9000, rStr2);
					mapLargest = mapReorderLarge;
					reordered = true;
					// System.Xut.println("printing mapReorderLarge=");
					// nlp.printMapIntStr(mapReorderLarge);
				}
			}

			// 2:1 ratio
			int cntL = 0;
			for (Map.Entry<Integer, String> smEntry : mapSmallest.entrySet()) {
				if (cntS > 0 && !lgFound)
					return null;
				smKey = smEntry.getKey();
				smMp = smKey - (smKey / 1000) * 1000;
				smStr = smEntry.getValue();
				// System.Xut.println("smStr=" + smStr + " smMp=" + smMp);
				lgFound = false;
				lgStr = "";
				lgMp = 0;
				lgKey = 0;
				cntS++;
				cntL = 0;
				for (Map.Entry<Integer, String> lgEntry : mapLargest.entrySet()) {
					cntL++;
					if (cntL != (2 * cntS) && cntL != (2 * cntS) - 1)
						continue;
					// want to grab lgCh if cntL=(2*cntS)-1 or cntL=(2*cntS)
					// if(cnt/cntL)
					lgKey = lgEntry.getKey();
					lgMp = lgKey - (lgKey / 1000) * 1000;
					lgStr = lgEntry.getValue();
//					NLP.pwNLP.append(
//							NLP.println("lgStr=", lgStr + " lgMp=" + lgMp + " smStr=" + smStr + " smMp=" + smMp));
					// !lgStr prevents dup entries of smStr where smStr has
					// both month and end for example (year ended june 30)
					if (prevLgStr != null && lgStr != null && null != smStr
							&& Math.abs(smMp - (lgMp + prevLgMp) / 2) <= 2 && !lgStr.contains(smStr)
							&& !prevLgStr.contains(smStr)
							|| (prevLgStr != null && lgStr != null && null != smStr
									&& Math.abs(smMp - (lgMp + prevLgMp) / 2) <= 3.6 && !lgStr.contains(smStr)
									&& !prevLgStr.contains(smStr) && lgMp - prevLgMp >= 11)) {
						mapTwoToOne.put(prevLgKey, smStr + " " + prevLgStr);
						mapTwoToOne.put(lgKey, smStr + " " + lgStr);
						lgFound = true;
//						NLP.pwNLP.append(
//								NLP.println(".put prevLgStr=", prevLgStr + " smStr=" + smStr + " prevLgMp" + prevLgMp));
//						NLP.pwNLP.append(NLP.println(".put lgStr=", lgStr + " smStr=" + smStr + " lgMp" + lgMp));
						break;
					}

					if (!lgFound && cntL == (2 * cntS)) {
						mapTwoToOne.put(prevLgKey, prevLgStr);
						mapTwoToOne.put(lgKey, lgStr);
					}
					prevLgKey = lgKey;
					prevLgMp = lgMp;
					prevLgStr = lgStr;
				}
			}

			// System.Xut.println("printing final 2year:1month map ==");
			// nlp.printMapIntStr(mapTwoToOne);

			NLP.printMapIntStr("from mergeColHdgsTwoToOneRatio mapMonthYear", mapTwoToOne);

			return mapTwoToOne;
		} else {
			return null;
		}
	}

	public static Map<Integer, String> mergeThreeToOne(Map<Integer, String> mapSmallest,
			Map<Integer, String> mapLargest, boolean monthYearDone, int cntM, int cntY) {

//		NLP.pwNLP.append(NLP.println("mergeThreeToOne cntM or cntE=",
//				cntM + " cntY=" + cntY + " monthYearDone or monthYearDoneEnded=" + monthYearDone));

		if (monthYearDone)
			return mapLargest;

		if (cntY > 0 && cntM > 0 && !monthYearDone && cntM != 0 && cntY != 0 && cntY / cntM == 3 && cntY % cntM == 0) {

			// map1 is smaller map, but always use mp of larger map.
			NLP nlp = new NLP();

			// NLP.printMapIntStr(
			// "@nlp mergeColHdgsThreeToOneRatio mapLargest", mapLargest);
			// NLP.printMapIntStr(
			// "@nlp mergeColHdgsThreeToOneRatio mapSmallest", mapSmallest);

			Map<Integer, String> mapThreeToOne = new TreeMap<Integer, String>();
			int lgMpCol1 = 0, lgMpCol2 = 0, lgMpCol3 = 0, smMp = 0, lgKeyCol1 = 0, lgKeyCol2 = 0, lgKeyCol3 = 0, smKey,
					cntSm = 0, cntLg = 0;
			String lgStrCol1 = "", lgStrCol2 = "", lgStrCol3 = "", smStr = "";
			// 3:1 ratio

			// reorder map -- such that if 2 years are on different rows I order
			// based solely on horizontal index locations unless both years are
			// very
			// close in idx value as they are then on top of each other and not
			// sep
			// columns. By reordering - I can digest the following CH:

			// YEAR ENDED DECEMBER 31,
			// 2002 2001
			// 2003 (RESTATED) (RESTATED)

			Map<Integer, String> mapReorderLarge = new TreeMap<Integer, String>();
			int rK1 = 0, rK2 = 0, rK3 = 0, rMp1 = 0, rMp2 = 0, rMp3 = 0, rCnt = 0;
			String rStr1 = "", rStr2 = "", rStr3 = "";
			// ck if each mp is at leat 5 idx space apart
			if (mapLargest.size() == 3) {
				for (Map.Entry<Integer, String> entry : mapLargest.entrySet()) {
					rCnt++;
					if (rCnt == 1) {
						rK1 = entry.getKey();
						rMp1 = rK1 - (rK1 / 1000) * 1000;
						rStr1 = entry.getValue();
						// System.Xut.println("rMp1=" + rMp1 + " rStr1=" +
						// rStr1);
					}
					if (rCnt == 2) {
						rK2 = entry.getKey();
						rMp2 = rK2 - (rK2 / 1000) * 1000;
						rStr2 = entry.getValue();
						// System.Xut.println("rMp2=" + rMp2 + " rStr2=" +
						// rStr2);
					}
					if (rCnt == 3) {
						rK3 = entry.getKey();
						rMp3 = rK3 - (rK3 / 1000) * 1000;
						rStr3 = entry.getValue();
						// System.Xut.println("rMp3=" + rMp3 + " rStr3=" +
						// rStr3);
					}
				}

				if (Math.abs(rMp3 - rMp2) > 5 && Math.abs(rMp3 - rMp1) > 5 && Math.abs(rMp2 - rMp1) > 5
						&& (rMp3 < rMp2 || rMp3 < rMp1 || rMp2 < rMp1)) {
					// dummy row=9
					mapReorderLarge.put(rMp1 + 9000, rStr1);
					mapReorderLarge.put(rMp2 + 9000, rStr2);
					mapReorderLarge.put(rMp3 + 9000, rStr3);
					mapLargest = mapReorderLarge;
					NLP.printMapIntStr("mapReorderLarge", mapReorderLarge);
				}
			}

			for (Map.Entry<Integer, String> smEntry : mapSmallest.entrySet()) {
				cntSm++;
				if (cntSm == 1) {
					smKey = smEntry.getKey();
					smMp = smKey - (smKey / 1000) * 1000;
					smStr = smEntry.getValue();
					// System.Xut.println("smMp=" + smMp + " smStr=" + smStr);
				}
			}

			for (Map.Entry<Integer, String> lgEntry : mapLargest.entrySet()) {
				cntLg++;
				if (cntLg == 1) {
					lgKeyCol1 = lgEntry.getKey();
					lgMpCol1 = lgKeyCol1 - (lgKeyCol1 / 1000) * 1000;
					lgStrCol1 = lgEntry.getValue();
					// System.Xut.println("lgStrCol1=" + lgStrCol1 +
					// " lgMpCol1="
					// + lgMpCol1);
				}

				if (cntLg == 2) {
					lgKeyCol2 = lgEntry.getKey();
					lgMpCol2 = lgKeyCol2 - (lgKeyCol2 / 1000) * 1000;
					lgStrCol2 = lgEntry.getValue();
				}

				// System.Xut.println("lgStrCol2=" + lgStrCol2 + " lgMpCol2="
				// + lgMpCol2);

				if (cntLg == 3) {
					lgKeyCol3 = lgEntry.getKey();
					lgMpCol3 = lgKeyCol3 - (lgKeyCol3 / 1000) * 1000;
					lgStrCol3 = lgEntry.getValue();
					// System.Xut.println("lgStrCol3=" + lgStrCol3 +
					// " lgMpCol3="
					// + lgMpCol3);
				}
			}

			// System.Xut.println("smMp=" + smMp + " lgMpCol3=" + lgMpCol3
			// + " lgMpCol1=" + lgMpCol1
			// + " Math.abs(smMp - (lgMpCol3 - lgMpCol1)/2)="
			// + Math.abs(smMp - (lgMpCol3 + lgMpCol1) / 2) + "\r smStr="
			// + smStr + " lgStrCol1=" + lgStrCol1 + " lgStrCol2="
			// + lgStrCol2 + " lgStrCol3=" + lgStrCol3 + "\rcntLg="
			// + cntLg + " cntSm=" + cntSm);

			// b/c 3:1- if any lgStrCol already contains smStr don't populate
			// mapThreeToOne
			// !lgStrCol1 prevents duplicate entries of smStr where smStr has
			// both month and end for example (year ended june 30)

			if (Math.abs(smMp - (lgMpCol3 + lgMpCol1) / 2) < 6.1 && !lgStrCol1.contains(smStr)) {
				// System.Xut.println("adding to mapThreeToOne:");
				mapThreeToOne.put(lgMpCol1, smStr + " " + lgStrCol1);
				// System.Xut.println("lgKeyCol1=" + lgKeyCol1 + " smStr=" +
				// smStr
				// + " lgStrCol1=" + lgStrCol1);
				mapThreeToOne.put(lgMpCol2, smStr + " " + lgStrCol2);
				// System.Xut.println("lgKeyCol2=" + lgKeyCol2 + " smStr=" +
				// smStr
				// + " lgStrCol2=" + lgStrCol2);
				mapThreeToOne.put(lgMpCol3, smStr + " " + lgStrCol3);
				// System.Xut.println("lgKeyCol3=" + lgKeyCol3 + " smStr=" +
				// smStr
				// + " lgStrCol3=" + lgStrCol3);
			}

			// System.Xut.println("return mapThreeToOne");

			return mapThreeToOne;
		} else {
			return null;
		}
	}

	public static Map<Integer, String> mergeThreeToTwo(Map<Integer, String> mapSmallest,
			Map<Integer, String> mapLargest, boolean monthYearDone, int cntM, int cntY) throws IOException {
		// map1 is smaller map, but always use mp of larger map.

//		NLP.pwNLP.append(NLP.println("mergeThreeToTwo cntM or cntE=",
//				cntM + " cntY=" + cntY + " monthYearDone or monthYearDoneEnded=" + monthYearDone));

		if (monthYearDone)
			return mapLargest;

		if (cntY > 0 && cntM > 0 && !monthYearDone && cntY == 3 && cntM == 2) {

			NLP nlp = new NLP();
			// System.Xut.println("@nlp 3:2 printing mapSmallest");
			// nlp.printMapIntStr(mapSmallest);
			// System.Xut.println("@nlp 3:2 printing mapLargest");
			// nlp.printMapIntStr(mapLargest);

			Map<Integer, String> mapThreeToTwo = new TreeMap<Integer, String>();
			boolean lgFound = false;
			int lgMp, prevYrMp = 0, moMp, lgKey = 0, prevYrKey = 0, smKey, cnt = 0;
			String lgStr = "", prevYrStr = null, smStr = "";
			// 2:3 ratio
			for (Map.Entry<Integer, String> smEntry : mapSmallest.entrySet()) {

				// cnt>0 ensure i pickup prior lgStr and if it wasn't paired w/
				// a
				// smStr (!lgFound) and it has yr and mon value - add it anyway
				// as
				// col hdg structur is 1:1,1:1 1:0 (3:2)

				if (cnt > 0 && !lgFound)
					return null;
				smKey = smEntry.getKey();
				moMp = smKey - (smKey / 1000) * 1000;
				smStr = smEntry.getValue();
				// System.Xut.println("smStr=" + smStr + " smMp=" + moMp);
				lgFound = false;
				cnt++;
				prevYrKey = -999;
				prevYrMp = -999;
				prevYrStr = "";
				for (Map.Entry<Integer, String> lgEntry : mapLargest.entrySet()) {
					lgKey = lgEntry.getKey();
					lgMp = lgKey - (lgKey / 1000) * 1000;
					lgStr = lgEntry.getValue();
					// System.Xut.println("lgStr=" + lgStr + " lgMp=" + lgMp);

					if (Math.abs(moMp - lgMp) <= 2 && !lgStr.contains(smStr)) {
						mapThreeToTwo.put(lgKey, smStr + " " + lgStr);
						lgFound = true;
						// System.Xut.println(".put - lgKey=" + lgKey
						// + " smStr+lgStr=" + smStr + " " + lgStr);
						prevYrKey = lgKey;
						prevYrMp = lgMp;
						prevYrStr = lgStr;
						break;
					}

					if (null != lgStr && null != smStr && null != prevYrStr
							&& Math.abs(moMp - (lgMp + prevYrMp) / 2) <= 2 && !lgStr.contains(smStr)
							&& !prevYrStr.contains(smStr) && !lgFound) {
						mapThreeToTwo.put(prevYrKey, smStr + " " + prevYrStr);
						mapThreeToTwo.put(lgKey, smStr + " " + lgStr);
						// System.Xut.println(".put - prevYrKey=" + prevYrKey
						// + " smStr+prevYrStr=" + smStr + " " + prevYrStr);
						// System.Xut.println(".put - lgKey=" + lgKey
						// + " smStr+lgStr=" + smStr + " " + lgStr);
						lgFound = true;
						prevYrKey = lgKey;
						prevYrMp = lgMp;
						prevYrStr = lgStr;
						break;
					}
					prevYrKey = lgKey;
					prevYrMp = lgMp;
					prevYrStr = lgStr;
				}
			}

			NLP.printMapIntStr("mapThreeToTwo", mapThreeToTwo);

			return mapThreeToTwo;
		} else {
			return null;
		}
	}

	public static Map<Integer, String> mergeFourToThree(Map<Integer, String> mapSmallest,
			Map<Integer, String> mapLargest, boolean monthYearDone, int cntM, int cntY) throws IOException {
		// map1 is smaller map, but always use mp of larger map.

//		NLP.pwNLP.append(NLP.println("mergeFourToThree cntM or cntE=",
//				cntM + " cntY=" + cntY + " monthYearDone or monthYearDoneEnded=" + monthYearDone));

		if (monthYearDone)
			return mapLargest;

		if (cntY > 0 && cntM > 0 && !monthYearDone && cntY == 4 && cntM == 3) {

			// NLP nlp = new NLP();
			// System.Xut.println("@nlp 3:2 printing mapSmallest");
			// nlp.printMapIntStr(mapSmallest);
			// System.Xut.println("@nlp 3:2 printing mapLargest");
			// nlp.printMapIntStr(mapLargest);

			Map<Integer, String> mapFourToThree = new TreeMap<Integer, String>();
			boolean lgFound = false;
			int lgMp, prevYrMp = 0, moMp, lgKey = 0, prevYrKey = 0, smKey, cnt = 0;
			String lgStr = "", prevYrStr = null, smStr = "";
			for (Map.Entry<Integer, String> smEntry : mapSmallest.entrySet()) {

				// cnt>0 ensure i pickup prior lgStr and if it wasn't paired w/
				// a smStr (!lgFound) and it has yr and mon value - add it
				// anyway col hdg structur is 1:1,1:1 1:0 (4:3)

				if (cnt > 0 && !lgFound)
					return null;
				smKey = smEntry.getKey();
				moMp = smKey - (smKey / 1000) * 1000;
				smStr = smEntry.getValue();
				// System.Xut.println("smStr=" + smStr + " smMp=" + moMp);
				lgFound = false;
				cnt++;
				prevYrKey = -999;
				prevYrMp = -999;
				prevYrStr = "";
				for (Map.Entry<Integer, String> lgEntry : mapLargest.entrySet()) {
					lgKey = lgEntry.getKey();
					lgMp = lgKey - (lgKey / 1000) * 1000;
					lgStr = lgEntry.getValue();
//					NLP.pwNLP.append(NLP.println("4:3 lgStr=",
//							lgStr + " lgMp=" + lgMp + " smStr=" + smStr + " end or moMp=" + moMp));

					if (Math.abs(moMp - lgMp) <= 2 && !lgStr.contains(smStr)) {
						mapFourToThree.put(lgKey, smStr + " " + lgStr);
						lgFound = true;
						// System.Xut.println(".put - lgKey=" + lgKey
						// + " smStr+lgStr=" + smStr + " " + lgStr);
						prevYrKey = lgKey;
						prevYrMp = lgMp;
						prevYrStr = lgStr;
						break;
					}

					if (null != lgStr && null != smStr && null != prevYrStr
							&& Math.abs(moMp - (lgMp + prevYrMp) / 2) <= 2 && !lgStr.contains(smStr)
							&& !prevYrStr.contains(smStr) && !lgFound) {
						mapFourToThree.put(prevYrKey, smStr + " " + prevYrStr);
						mapFourToThree.put(lgKey, smStr + " " + lgStr);
						// System.Xut.println(".put - prevYrKey=" + prevYrKey
						// + " smStr+prevYrStr=" + smStr + " " + prevYrStr);
						// System.Xut.println(".put - lgKey=" + lgKey
						// + " smStr+lgStr=" + smStr + " " + lgStr);
						lgFound = true;
						prevYrKey = lgKey;
						prevYrMp = lgMp;
						prevYrStr = lgStr;
						break;
					}
					prevYrKey = lgKey;
					prevYrMp = lgMp;
					prevYrStr = lgStr;
				}
			}

			NLP.printMapIntStr("mapFourToThree", mapFourToThree);

			return mapFourToThree;
		} else {
			return null;
		}
	}

	// 5:3
	public static Map<Integer, String> mergeFiveToThree(Map<Integer, String> mapSmallest,
			Map<Integer, String> mapLargest, boolean monthYearDone, int cntM, int cntY) {
		// map1 is smaller map, but always use mp of larger map.
		NLP nlp = new NLP();

		if (monthYearDone)
			return mapLargest;

		if (cntY > 0 && cntM > 0 && !monthYearDone && cntY == 5 && cntM == 3) {

			Map<Integer, String> mapFiveToThree = new TreeMap<Integer, String>();
			boolean lgFound = false;
			int lgMp, prevYrMp = 0, smMp, lgKey, prevYrKey = 0, smKey, cnt = 0;
			String lgStr, prevYrStr = null, smStr;
			// 5:3 ratio. can be: 1:3, 1:1,1:1 or 1:2 and 2:3 or 1:1 and
			// 1:2,1:2,

			// System.Xut.println("@mergemapFiveToThree - mapSmallest.size="
			// + mapSmallest.size() + " mapLargest.size=" + mapLargest.size());
			for (Map.Entry<Integer, String> smEntry : mapSmallest.entrySet()) {
				cnt++;
				if (cnt > 1 && !lgFound)
					return null;
				// if(cnt!=1 && cnt!=mapSmallest.size())
				// continue;
				smKey = smEntry.getKey();
				smMp = smKey - (smKey / 1000) * 1000;
				smStr = smEntry.getValue();
				// System.Xut.println("smStr=" + smStr + " smMp=" + smMp);
				lgFound = false;
				prevYrKey = -999;
				prevYrMp = -999;
				prevYrStr = "";
				for (Map.Entry<Integer, String> lgEntry : mapLargest.entrySet()) {

					lgKey = lgEntry.getKey();
					lgMp = lgKey - (lgKey / 1000) * 1000;
					lgStr = lgEntry.getValue();
					// System.Xut.println("lgStr=" + lgStr + " lgMp=" + lgMp);

					if (Math.abs(smMp - lgMp) <= 2 && !lgStr.contains(smStr)) {
						// System.Xut.println(".put smStr+lgStr=" + smStr + " "
						// + lgStr);
						mapFiveToThree.put(lgKey, smStr + " " + lgStr);
						lgFound = true;
						prevYrKey = lgKey;
						prevYrMp = lgMp;
						prevYrStr = lgStr;
						break;
					}

					if (null != lgStr && null != smStr && null != prevYrStr
							&& Math.abs(smMp - (lgMp + prevYrMp) / 2) <= 2 && !lgStr.contains(smStr)
							&& !prevYrStr.contains(smStr) && !lgFound) {

						// System.Xut.println("prevYrMp=" + prevYrMp + " lgMp="
						// +
						// lgMp
						// + " smMp" + smMp);
						// System.Xut.println(".put prevYrKey=" + prevYrKey
						// + " smStr=" + smStr + " prevYrStr=" + prevYrStr);
						// System.Xut.println("then .put lgKey=" + lgKey +
						// " smStr="
						// + smStr + " lgStr=" + lgStr);

						mapFiveToThree.put(prevYrKey, smStr + " " + prevYrStr);
						mapFiveToThree.put(lgKey, smStr + " " + lgStr);
						lgFound = true;
						prevYrKey = lgKey;
						prevYrMp = lgMp;
						prevYrStr = lgStr;
						break;
					}
					prevYrKey = lgKey;
					prevYrMp = lgMp;
					prevYrStr = lgStr;
				}
			}

			NLP.printMapIntStr("mapFiveToThree", mapFiveToThree);

			return mapFiveToThree;
		} else {
			return null;
		}
	}

	public static Map<Integer, String> reOrderMap(Map<Integer, String> mapSmallest) {

		TreeMap<Integer, String> mapReordered = new TreeMap<Integer, String>();

		int key;
		String value;
		for (Map.Entry<Integer, String> smEntry : mapSmallest.entrySet()) {
			key = smEntry.getKey();
			key = key - (key / 1000) * 1000;
			key = key + 1000;
			value = smEntry.getValue();
			mapReordered.put(key, value);
		}

		return mapReordered;
	}

	public static TreeMap<Integer, List<String[]>> getStrandedMoDayYear(String startGroupReconstituted,
			TreeMap<Integer, List<String[]>> map, int startGrpSplitLen) throws ParseException, IOException {

		NLP nlp = new NLP();
		// finds only a mo/dy/yr value stranded on line to far right not in map
		// (mapAllCH2) - and adds it.
		List<String[]> listYrRecon = nlp.getAllEndIdxAndMatchedGroupLocs(startGroupReconstituted,
				TableParser.YearOrMoDayYrPattern2);
		String startGroupLimited = startGroupReconstituted;
		// System.Xut
		// .println("startGroupReconstituted=" + startGroupReconstituted);
		int yrRecEidx = 0, yrLtdEidx = 0;
		boolean stranded = false;
		if (listYrRecon.size() < 1) {
			listYrRecon = nlp.getAllEndIdxAndMatchedGroupLocs(startGroupReconstituted,
					Pattern.compile("(?<=[ ]{50,})[\\p{Alnum}\\p{Punct}]{1,12}( [\\p{Alnum}\\p{Punct}]{1,12})?"
							+ "( [\\p{Alnum}\\p{Punct}]{1,12})? ?[\r\n]{1}"));
		}
		if (listYrRecon.size() == 1) {
			stranded = true;
		}

		// System.Xut.println("a stranded=" + stranded + " listYrRecon.size="
		// + listYrRecon.size());

		if (stranded = true) {
			for (int r = map.size() - 1; r >= 0; r--) {
				if (!stranded)
					break;
				for (int c = 0; c < listYrRecon.size(); c++) {
					List<String[]> tmpL3 = map.get(r);
					for (int b = 0; b < tmpL3.size(); b++) {
						yrRecEidx = Integer.parseInt(listYrRecon.get(c)[0]);
						yrLtdEidx = Integer.parseInt(tmpL3.get(b)[0]);
						String tmpStr = listYrRecon.get(c)[1].trim().substring(0,
								Math.min(5, listYrRecon.get(c)[1].trim().length()));
						// System.Xut.println("tmpStr=" + tmpStr);
						if (tmpL3.get(b)[1].trim().contains(tmpStr))
							stranded = false;
						// System.Xut.println("tmpStr=" + tmpStr + " stranded="
						// + stranded);
						// System.Xut
						// .println("tmpL3.get(b)[1]=" + tmpL3.get(b)[1]);
						// System.Xut
						// .println("tmpL3.get(b)[0]=" + tmpL3.get(b)[0]);
						// System.Xut.println("yrRecEidx=" + yrRecEidx);
						// System.Xut.println("yrLtdEidx=" + yrLtdEidx);
						// if there's no eIdx w/n 10 and a year value - it is a
						// find
						// -- and I need to add to map after adding '19' or '20'
						// preceding the year value
					}
				}
			}
		}

		listYrRecon = nlp.getAllEndIdxAndMatchedGroupLocs(startGroupReconstituted, TableParser.YearOrMoDayYrPattern2);
		if (listYrRecon.size() < 1) {
			listYrRecon = nlp.getAllEndIdxAndMatchedGroupLocs(startGroupReconstituted,
					Pattern.compile("(?<=[ ]{50,})[\\p{Alnum}\\p{Punct}]{1,12}( [\\p{Alnum}\\p{Punct}]{1,12})?"
							+ "( [\\p{Alnum}\\p{Punct}]{1,12})? ?[\r\n]{1}"));
		}

		// System.Xut.println("aa listYrRecon.size=" + listYrRecon.size()
		// + " stranded=" + stranded);

		if (stranded && listYrRecon.size() > 0) {
			String str = "";
			// System.Xut.println("putting in map this list. listYrRecon.size="
			// + listYrRecon.size());
//			NLP.printListOfStringArray("listYrRecon", listYrRecon);
			str = nlp.getEnddatePeriod(listYrRecon.get(0)[1]).get(0);
			String[] ary = { listYrRecon.get(0)[0], str, "" + (startGrpSplitLen + 1) };
			List<String[]> tmpL4 = new ArrayList<>();
			tmpL4.add(ary);
			map.put(map.size(), tmpL4);
			// System.Xut.println("map.size=" + map.size());
			NLP.printMapIntListOfStringAry("after put stranded - map", map);
		}

		return map;
	}

	// This is a catchall - where I pair first and last of small list only to
	// first and last of largeList centered midpoints. Difficulty is that first
	// or last small list can be unrelated to first or last largelist. Therefore
	// I should discard small list UNLESS its mp is w/n 1.5 of mp of either 1st
	// largeList CH or 1st and 2nd (same logic for last ch in lg list and last
	// small list ch)

	public static Map<Integer, String> mergeFirstAndLastCHs(Map<Integer, String> mapSmallest,
			Map<Integer, String> mapLargest, boolean monthYearDone, int cntICol, int totalColumns) {

		// map1 is smaller map, but always use mp of larger map.
		// loop through largeList -- and if smallList pairs w/n 2 of 1st or last
		// then pair (or w/n 2 of first 2 lg or last 2 lg) - else skip

		if (monthYearDone)
			return mapLargest;

		if (null != mapLargest && (!monthYearDone || (mapLargest.size() + cntICol) != totalColumns)
				&& totalColumns > 0) {

			NLP nlp = new NLP();
			Map<Integer, String> mapFirstAndLast = new TreeMap<Integer, String>();
			int smKey = 0, smKey2 = 0, smMp2 = 0, lg1Mp = 0, lg2Mp = 0, lg1Sidx = 0, lg2Sidx = 0, lg1Eidx = 0,
					lg2Eidx = 0, smMp = 0, key, lastSmCHmp = 0, firstSmCHmp = 10000, firstSmMp = 0, firstSmKey = 0,
					lastSmMp = 0, lastSmKey = 0, lastSmSidx = 0, lastSmEidx = 0, cnt = 0, lg1Key = 0, lg2Key = 0,
					firstSmSidx = 0, firstSmEidx = 0;
			String lg1Str = "", lg2Str = "", firstSmStr = null, lastSmStr = "", smStr, smStr2 = "";
			double dist = 3.6;

//			NLP.pwNLP.append(NLP.println("@mergemapFirstAndLast - mapSmallest.size=",
//					mapSmallest.size() + " mapLargest.size=" + mapLargest.size()));
//			NLP.printMapIntStr("mapLargest", mapLargest);
//			NLP.printMapIntStr("mapSmallest", mapSmallest);

			mapSmallest = reOrderMap(mapSmallest);
//			NLP.printMapIntStr("reordered - mapSmallest", mapSmallest);
			cnt = 0;
			for (Map.Entry<Integer, String> smEntry : mapSmallest.entrySet()) {
				cnt++;
				key = smEntry.getKey();
				smMp = key - (key / 1000) * 1000;
				smStr = smEntry.getValue();

				if (cnt == mapSmallest.size()) {
					lastSmCHmp = smMp;
					lastSmKey = key;
					lastSmMp = lastSmKey - (lastSmKey / 1000) * 1000;
					lastSmStr = smStr;
					lastSmSidx = lastSmMp - (lastSmStr.length() / 2);
					lastSmEidx = lastSmMp + (lastSmStr.length() / 2);

				}

				if (cnt == 1) {
					firstSmCHmp = smMp;
					firstSmKey = key;
					firstSmMp = firstSmKey - (firstSmKey / 1000) * 1000;
					firstSmStr = smStr;
					firstSmSidx = firstSmMp - (firstSmStr.length() / 2);
					firstSmEidx = firstSmMp + (firstSmStr.length() / 2);

				}
			}

//			NLP.pwNLP.append(
//					NLP.println("mergeColHdgsFirstAndLast -- lastSmCHmp=", lastSmCHmp + " firstSmCHmp=" + firstSmCHmp));

			// create list of map keys so I can call later
			List<Integer> listLargeMapKeys = new ArrayList<Integer>();
			for (Map.Entry<Integer, String> lgEntry : mapLargest.entrySet()) {
				listLargeMapKeys.add(lgEntry.getKey());
			}

			List<Integer> listSmallMapKeys = new ArrayList<Integer>();
			for (Map.Entry<Integer, String> smEntry : mapSmallest.entrySet()) {
				listSmallMapKeys.add(smEntry.getKey());
			}
			// mapSmallest may not actually be in correct order. So check if
			// first or last key - and if not see if it will pair
			boolean done = false;
			for (int i = 0; i < listLargeMapKeys.size(); i++) {
				done = false;
				for (Map.Entry<Integer, String> smEntry : mapSmallest.entrySet()) {
					smKey2 = 0;
					smMp = 0;
					smStr = "";
					smKey = smEntry.getKey();
					smMp = smKey - (smKey / 1000) * 1000;
					smStr = smEntry.getValue();

//					nlp.pwNLP.append(NLP.println("smMp=", smMp + " firstSmCHmp=" + firstSmCHmp));
					if (smMp == firstSmCHmp && i == 0) {

						lg1Key = listLargeMapKeys.get(0);
						lg1Str = mapLargest.get(lg1Key);
						lg1Mp = lg1Key - (lg1Key / 1000) * 1000;
						lg1Sidx = lg1Mp - (lg1Str.length() / 2);
						lg1Eidx = lg1Mp + (lg1Str.length() / 2);

						if (listLargeMapKeys.size() > 1) {
							lg2Key = listLargeMapKeys.get(1);
							lg2Str = mapLargest.get(lg2Key);
							lg2Mp = lg2Key - (lg2Key / 1000) * 1000;
							lg2Sidx = lg2Mp - (lg2Str.length() / 2);
							lg2Eidx = lg2Mp + (lg2Str.length() / 2);
						}

//						NLP.pwNLP.append(NLP.println("done=",
//								done + " firstSmMp=" + firstSmMp + " lg1Mp=" + lg1Mp + " lg2Mp=" + lg2Mp + " lg1Str="
//										+ lg1Str + " lg2Str=" + lg2Str + " firstSmStr=" + firstSmStr));

						if (!done && Math.abs(firstSmMp - (lg1Mp + lg2Mp) / 2) < dist && !lg1Str.contains(firstSmStr)
								&& !lg2Str.contains(firstSmStr)) {

							mapFirstAndLast.put(lg1Key, firstSmStr + " " + lg1Str);
							mapFirstAndLast.put(lg2Key, firstSmStr + " " + lg2Str);
//							NLP.pwNLP.append(NLP.println("A lg1/2-.put firstSmStr=", firstSmStr + " lg1Str=" + lg1Str
//									+ "\rA lg1/2-.put firstSmStr=" + firstSmStr + " lg2Str=" + lg2Str));
							i++;
							done = true;
						}

						// if largeList=4 and smallList=2 smCH starts prior and
						// ends after lgCh than pair them
						if ((!done && Math.abs(firstSmMp - lg1Mp) < dist || (mapLargest.size() == 4
								&& mapSmallest.size() == 2 && firstSmSidx <= lg1Sidx && firstSmEidx >= lg1Eidx)

						) && !lg1Str.contains(firstSmStr)) {
							mapFirstAndLast.put(lg1Key, firstSmStr + " " + lg1Str);
//							NLP.pwNLP.append(NLP.println("B lg1-.put firstSmStr=" + firstSmStr + " lg1Str=", lg1Str));
							done = true;

							if (mapSmallest.size() > 1) {
								smKey2 = listSmallMapKeys.get(1);
								smMp2 = smKey2 - (smKey2 / 1000) * 1000;
								smStr2 = mapSmallest.get(smKey2);
							}
							if (Math.abs(lg2Mp - smMp2) < dist && !lg2Str.contains(smStr2)) {
								mapFirstAndLast.put(lg2Key, smStr2 + " " + lg2Str);
//								NLP.pwNLP.append(NLP.println("B lg2-.put smStr=" + smStr + " lg2Str=", lg2Str));
							} else {
								mapFirstAndLast.put(lg2Key, lg2Str);
//								NLP.pwNLP.append(NLP.println("B lg2-.put lg2Str=", lg2Str));
							}
							i++;
						}

						if (!done) {
							mapFirstAndLast.put(lg1Key, lg1Str);
							mapFirstAndLast.put(lg2Key, lg2Str);
//							NLP.pwNLP.append(
//									NLP.println("B2 lg1-.put lg1Str=" + lg1Str, "\rB2 lg2-.put lg2Str=" + lg2Str));
							i++;
							done = true;
						}
					}

//					NLP.pwNLP.append(NLP.println("smMp=" + smMp, " lastCmChMp=" + lastSmCHmp));
					if ((i + 2) == listLargeMapKeys.size() && smMp == lastSmCHmp) {

						if (listLargeMapKeys.size() > 1) {
							lg1Key = listLargeMapKeys.get(listLargeMapKeys.size() - 2);
							lg1Str = mapLargest.get(lg1Key);
							lg1Mp = lg1Key - (lg1Key / 1000) * 1000;
						}

						lg2Key = listLargeMapKeys.get(listLargeMapKeys.size() - 1);
						lg2Str = mapLargest.get(lg2Key);
						lg2Mp = lg2Key - (lg2Key / 1000) * 1000;

//						NLP.pwNLP
//								.append(NLP.println(
//										"done=" + done + " lastSmMp=" + lastSmMp + " lg1Mp=" + lg1Mp + " lg2Mp=" + lg2Mp
//												+ " lg1Str=" + lg1Str + " lg2Str=" + lg2Str + " lastSmStr=",
//										lastSmStr));

						if (!done && Math.abs(lastSmMp - (lg1Mp + lg2Mp) / 2) < dist && !lg1Str.contains(lastSmStr)
								&& !lg2Str.contains(lastSmStr)) {

							mapFirstAndLast.put(lg1Key, lastSmStr + " " + lg1Str);
							mapFirstAndLast.put(lg2Key, lastSmStr + " " + lg2Str);
//							NLP.pwNLP.append(NLP.println("C lg1/2-.put lastSmStr=" + lastSmStr + " lg1Str=" + lg1Str,
//									"\rC lg1/2-.put lastSmStr=" + lastSmStr + " lg2Str=" + lg2Str));

							i++;
							done = true;
						}

						if (!done && !lg2Str.contains(lastSmStr) && ((Math.abs(lastSmMp - lg2Mp) < dist) ||

								mapLargest.size() == 4 && mapSmallest.size() == 2 && lastSmSidx <= lg2Sidx
										&& lastSmEidx >= lg2Eidx)) {
							mapFirstAndLast.put(lg2Key, lastSmStr + " " + lg2Str);
//							NLP.pwNLP.append(NLP.println("D lg2-.put lastSmStr=" + lastSmStr + " lg2Str=", lg2Str));
							done = true;

							if (mapSmallest.size() > 1) {
								smKey2 = listSmallMapKeys.get(mapSmallest.size() - 2);
								smMp2 = smKey2 - (smKey2 / 1000) * 1000;
								smStr2 = mapSmallest.get(smKey2);
							}

							if (Math.abs(lg2Mp - smMp2) < dist && !lg1Str.contains(smStr2)) {
								mapFirstAndLast.put(lg1Key, smStr2 + " " + lg1Str);
//								NLP.pwNLP.append(NLP.println("B lg2-.put smStr=" + smStr + " lg1Str=", lg1Str));
							} else if (null == mapFirstAndLast.get(lg1Key)
									|| mapFirstAndLast.get(lg1Key).length() < 1) {
								mapFirstAndLast.put(lg1Key, lg1Str);
//								NLP.pwNLP.append(NLP.println("B lg2-.put lg1Str=", lg1Str));
							}
							i++;
						}

						if (!done) {
							mapFirstAndLast.put(lg1Key, lg1Str);
							mapFirstAndLast.put(lg2Key, lg2Str);
							i++;
						}
					}

					if (!done && i != 0 && i != 1 && i != listLargeMapKeys.size() - 1
							&& i != listLargeMapKeys.size() - 2) {
						lg1Key = listLargeMapKeys.get(i);
						lg1Str = mapLargest.get(lg1Key);
						lg1Mp = lg1Key - (lg1Key / 1000) * 1000;

						if (Math.abs(lg1Mp - smMp) < dist) {
							mapFirstAndLast.put(lg1Key, smStr + " " + lg1Str);
//							NLP.pwNLP.append(NLP.println("E lg1-.put smStr=" + smStr + " lg1Str=", lg1Str));
							done = true;
						} else {
							mapFirstAndLast.put(lg1Key, lg1Str);
//							NLP.pwNLP.append(NLP.println("F lg1-.put lg1Str=", lg1Str));
							done = true;
						}
					}

					if (!done && i != 0 && i != 1 && i != listLargeMapKeys.size() - 1
							&& i != listLargeMapKeys.size() - 2) {
						mapFirstAndLast.put(lg1Key, lg1Str);
//						NLP.pwNLP.append(NLP.println("lg1 lg1Str=", lg1Str));
						done = true;
					}
				}
			}

			NLP.printMapIntStr("returned - mapFirstAndLast", mapFirstAndLast);
			return mapFirstAndLast;
		} else
			return null;
	}

	public static Map<Integer, List<String[]>> getSubtotals(Map<Integer, List<String[]>> mapData, int lastColEidx,
			boolean isHtml, String tableNameShort, boolean redoMap) throws ParseException {

		NLP.printMapIntListOfStringAry("at subTotal - parameter pass mapData", mapData);
		// System.Xut.println("getSubtotals. lastColEdix="+lastColEidx);

		/*
		 * getSubtotals is used to net and add all rows in data table - both methods are
		 * applied concurrently. The first to net or aggregate to the totaling row
		 * creates a successful match. Those sub rows that total or net to the totaling
		 * row (cur row) are removed but the net or total rows remain. The process then
		 * move to the next row -- rows are totaled by going down the table to each row
		 * - but then at that row working reverse to total or net the rows above it. The
		 * process works when mapData key is rownumber, each list is a single row and
		 * each string[] is a single column - first list is first ary and is col=rowname
		 * length, rowname, 2nd col is 2nd ary in same list and is endIdx (if html - it
		 * is col #) of data and data value. Based on eIdx/col# the column is
		 * summed/netted. I only net/sum against last col but then label entire row by
		 * marking the rowname with 'sub' (a sub net line) or 'net' (a line that netted)
		 * or 'tl' (a line that totaled) or st' (a sub total line) followed by
		 * applicable tableRow#-- technically each last data col. Then in
		 * tableTextParser and tableParserHtml if rowname has this mark I cut the value
		 * and place in corresponding table col sub/net/tl/st
		 */

		NLP nlp = new NLP();

		@SuppressWarnings("unused")
		int key = 0, keyRev, eIdx, eIdxSub = 0;
		@SuppressWarnings("unused")
		Double subTotaling = 0.0, subTtlNetting = 0.0, loanLossAmt = 0.0, val, prevVal = -1.11, valSub = null;
		String rowname = "", valStr = "", valSubStr = "";
		boolean isNumeric = false, subtotalFound = false, subTtlNet = false, subTtl = false,
				isExpenseTotalingRowname = false, complete = false;
		String totalingRowname = "", currentRowname = "";
		List<String[]> listStrAry = new ArrayList<String[]>();
		List<String[]> listStrAryRev = new ArrayList<String[]>();
		// NavigableMap avoids concurrent error where you try to modify a list
		// that is being iterated
		NavigableMap<Integer, List<String[]>> mapReversed = new ConcurrentSkipListMap<Integer, List<String[]>>();

		NavigableMap<Integer, List<String[]>> mapDataNavigable = new ConcurrentSkipListMap<Integer, List<String[]>>();
		// using NavigableMap b/c it allows for concurrent revisions of map
		// (when you iterate/loop over a map it won't let you modify it it it is
		// as hashMap or TreeMap but it will if it is a NavigableMap)

		// casting mapData to NavigableMap
		mapDataNavigable.clear();
		mapDataNavigable = (NavigableMap<Integer, List<String[]>>) mapData;
		// System.Xut.println("printing nmapDataNavigable right after synching to
		// mapDAta");
		// nlp.printMapIntListOfStringAry(mapDataNavigable);
		// Iterate over mapData and put into reverse map each key in negative
		// value so largest is smallest (map reversed). Then for each last col
		// sum all items in mapReversed to see if totals to cur row or greater.
		// If greater subtotal failed and go to next row. If subtotal occurs -
		// mark each component row (ST) and subtotal row (TL). Then remove all
		// from mapReversed other than subtotal row.

		for (Map.Entry<Integer, List<String[]>> entry : mapDataNavigable.entrySet()) {
			key = entry.getKey();
			// System.Xut.println("-key=" + (-key));
			mapReversed.put(-key, entry.getValue());

			listStrAry = entry.getValue();
			// System.Xut.println("listStrAry.size=" + listStrAry.size());
			if (listStrAry.size() < 1)
				continue;
			totalingRowname = listStrAry.get(0)[1].trim();
			// System.Xut.println(" listStrAry="+Arrays.toString(listStrAry.get(0)));
			// System.Xut.println(" listStrAry last
			// col="+Arrays.toString(listStrAry.get(listStrAry.size()-1)));
			subTotaling = 0.0;
			subTtlNetting = 0.0;
			Matcher matchIsExpense = patternIsExpense.matcher(totalingRowname);
			// if isExpense - pattern won't match with others.
			Matcher matchIsIncomeOrSales = patternIsIncomeOrLoss.matcher(totalingRowname);
			Matcher matchIsIncomeSales = patternIsIncomeSales.matcher(totalingRowname);

			if (matchIsExpense.find())
				isExpenseTotalingRowname = true;
			else
				isExpenseTotalingRowname = false;

			if (listStrAry.get(listStrAry.size() - 1)[0].trim().length() == 0)
				continue;
			// System.Xut.println("eidx=="+listStrAry.get(listStrAry.size() -
			// 1)[0]);
			if (!nlp.isNumeric(listStrAry.get(listStrAry.size() - 1)[0]))
				continue;
			eIdx = Integer.parseInt(listStrAry.get(listStrAry.size() - 1)[0]);
			// System.Xut.println("eIdx="+eIdx+"|| lastColEidx="+lastColEidx+
			// "|| totalingRowname=" + totalingRowname
			// + " isExpenseTotalingRowname=" + isExpenseTotalingRowname+
			// " isHtml="+isHtml);
			valStr = listStrAry.get(listStrAry.size() - 1)[1];
			isNumeric = false;
			isNumeric = nlp.isNumeric(valStr);
			// System.Xut.println("listStrAry rowname="+Arrays.toString(listStrAry.get(0)));
			// System.Xut.println("valStr="+valStr+ " endIdx="+eIdx);

			// can't sum unless at least 2 rows prior. cks if this column's eIdx
			// is equal to lastColEidx passed to this method and if number. Else
			// gets net iteration of loop occurs

			// System.Xut.println("key=="+key);

			if (key == 0 && isNumeric && ((!isHtml && Math.abs(eIdx - lastColEidx) < 3)
					|| (isHtml && Math.abs(eIdx - lastColEidx) == 0))) {
				if (valStr.contains("E")) {
					valStr = new BigDecimal(valStr).intValue() + "";
				}
				prevVal = SubTotalFinder.parseToAmount(valStr);
			}

			// if it is HTML - the difference must be 0.
			// System.Xut.println("key=="+key+" eIdx="+eIdx+" lastColEidx="+lastColEidx+"
			// isHtml="+isHtml);
			if (key > 1 && isNumeric && ((!isHtml && Math.abs(eIdx - lastColEidx) < 3)
					|| (isHtml && Math.abs(eIdx - lastColEidx) == 0))) {
				isNumeric = false;
				// System.Xut
				// .println("key="
				// + key
				// + " this is last col="
				// + Arrays.toString(listStrAry.get(listStrAry
				// .size() - 1)));
				int cnt = 0, maxCnt = 30, maxCntReverse = 0;

				if (valStr.contains("E")) {
					valStr = new BigDecimal(valStr).intValue() + "";
				}

				val = SubTotalFinder.parseToAmount(valStr);
				if (Math.abs(val) < 100)
					continue;
				subTotaling = 0.0;
				subtotalFound = false;

				// iterates over mapReverse to see if cur val sums to prior
				// rows. Stops when it does or sum exceeds val

				int cntSubTtlnetting = 0, cntSubTtl = 0;
				for (Map.Entry<Integer, List<String[]>> ent : mapReversed.entrySet()) {

					// System.Xut.println("cnt=" + cnt);
					cnt++;
					if (cnt == 1)
						continue;

					// only ck up to prior 15 rows
					if (cnt > maxCnt)
						break;
					keyRev = ent.getKey();
					// System.Xut.println("keyRev="+keyRev);
					listStrAryRev = ent.getValue();
					if (null != listStrAryRev && listStrAryRev.size() > 0
							&& listStrAryRev.get(listStrAryRev.size() - 1)[0].trim().length() == 0)
						continue;

					if (listStrAryRev.size() > 1) {
						eIdxSub = Integer.parseInt(listStrAryRev.get(listStrAryRev.size() - 1)[0]);
					}
					// current row col is not same as lastColEidx -- b/c some
					// rows won't have data in this col - so wrong col is
					// present for html

					if (listStrAryRev.size() < 2)
						continue;

					if ((isHtml && (lastColEidx - eIdxSub) != 0) || !isHtml && Math.abs(lastColEidx - eIdxSub) > 2)
						continue;

					// System.Xut.println("printing listStrAryRev=");
					// nlp.printListOfStringArray(listStrAryRev);

					valSubStr = listStrAryRev.get(listStrAryRev.size() - 1)[1].trim().replaceAll("[\t\r\n ]", "");
					isNumeric = nlp.isNumeric(valSubStr);
					// System.Xut.println("valSubStr="+valSubStr+
					// " isNumberic="+isNumeric);
					// System.Xut.println("total row?="
					// + totalingRowname + "| value=" + val);

					if (val == 0)
						continue;

					if (isNumeric) {
						currentRowname = listStrAryRev.get(0)[1].trim();

						// 1st method
						if (valSubStr.contains("E")) {
							valSubStr = new BigDecimal(valSubStr).intValue() + "";
						}
						valSub = SubTotalFinder.parseToAmount(valSubStr);
						// System.Xut.println("valSub="+valSub+" valSubStr="+valSubStr);
						subTotaling = subTotaling + valSub;
						cntSubTtl++;

						// alt method
						Matcher matchIsResearch = patternIsResearch.matcher(currentRowname);
						Matcher matchIsLoanLoss = patternIsLoanLoss.matcher(currentRowname);
						matchIsExpense = patternIsExpense.matcher(currentRowname);
						matchIsIncomeOrSales = patternIsIncomeOrLoss.matcher(currentRowname);
						matchIsIncomeSales = patternIsIncomeSales.matcher(currentRowname);

						if (isExpenseTotalingRowname && ((!isHtml && Math.abs(eIdxSub - lastColEidx) < 3)
								|| (isHtml && (eIdxSub - lastColEidx) == 0))) {
							// expense totalingrow
							// System.Xut.println("expense totaling row. currentRowname=="+currentRowname+"|
							// isHtml="+isHtml+"| eIdx="+eIdx+"| lastColEidx"+lastColEidx);
							// must be same pattern that determines is
							// ExpenseTotalingRow (see above)
							if (!matchIsExpense.find()) {
								subTtlNetting = subTtlNetting - valSub;
								// System.Xut.println("A. value to add="+valSub+" to cur
								// subtotalAlt="+subTtlNetting+"\rcur rowname="+currentRowname);
							}
							// tbl='is' research is an expense.
							if (null == tableNameShort)
								tableNameShort = "";
							if (matchIsResearch.find() && tableNameShort.equals("is") || matchIsExpense.find()) {
								subTtlNetting = subTtlNetting + valSub;
								// System.Xut.println("B1. found research expense -- value to add="+valSub+" to
								// cur subtotalAlt="+subTtlNetting+"\rcur rowname="+currentRowname);
							}
							complete = true;
							cntSubTtlnetting++;
						}

						if (!isExpenseTotalingRowname && !complete && ((!isHtml && Math.abs(eIdxSub - lastColEidx) < 3)
								|| (isHtml && (eIdxSub - lastColEidx) == 0))) {
							// System.Xut.println("Not exp totaling row.currentRowname=="+currentRowname+"|
							// isHtml="+isHtml+"| eIdx="+eIdx+"| lastColEidx"+lastColEidx);
							// totaling row is not an expense
							// but if tbl='is' research is an expense. If BS
							// typically asset (+), if CF typically a plus
							if (matchIsExpense.find()
									|| (matchIsResearch.find() && tableNameShort.toLowerCase().equals("is"))) {

								// System.Xut.println("C. value to add=" +
								// valSub
								// + " to cur subtotalAlt="
								// + subTtlNetting + "\rcur rowname="
								// + currentRowname);

								subTtlNetting = subTtlNetting - valSub;

								// System.Xut.println("C. subTotalAlt="
								// + subTtlNetting);
							}

							else {
								subTtlNetting = subTtlNetting + valSub;
								// System.Xut.println("D. value to add="+valSub+" to cur
								// subtotalAlt="+subTtlNetting+"\rcur rowname="+currentRowname);
							}

							cntSubTtlnetting++;
						}

						if (matchIsLoanLoss.find() && tableNameShort.toLowerCase().equals("is")
								&& ((!isHtml && Math.abs(eIdxSub - lastColEidx) < 3)
										|| (isHtml && (eIdxSub - lastColEidx) == 0)))
						// System.Xut.println("currentRowname=="+currentRowname);
						// loan loss found - non-expense totaling row
						{// assume only 1 instance possible - so can otherwise
							// utilize subTotalAlt
							loanLossAmt = valSub;
						}

						complete = false;

						// System.Xut.println("checking this rowname="
						// + currentRowname
						// + "\rvalue=" + valSub + "\rsubTotal="
						// + subTotaling + "\rvalue to match=" + val+
						// " subTotalAlt="+subTtlNetting);

						// System.Xut.println("printing mapReversed");
						// nlp.printMapIntListOfStringAry(mapReversed);

						subTtl = false;
						if (Math.abs(subTotaling - val) < 0.000001 && cntSubTtl > 1 || (Math.abs(val) > 999
								&& Math.abs(subTotaling) > 999 && Math.abs(subTotaling - val) < 1.01))
							subTtl = true;
						subTtlNet = false;
						if (Math.abs(subTtlNetting - val) < 0.000001 && cntSubTtlnetting > 1
								|| (Math.abs(val) > 999 && Math.abs(subTtlNetting) > 999
										&& Math.abs(subTtlNetting - val) < 1.01 && cntSubTtlnetting > 1))
							subTtlNet = true;
						// take aggregate up to cur row and subtract it from
						// current row
						// System.Xut.println("valSub="+valSub+" subTotaling="+subTotaling+" val="+val);
						// if (cntSubTtl > 1
						// && Math.abs(valSub) > 99
						// && Math.abs(((valSub - (subTotaling - valSub)) -
						// val)) < 0.000001)
						// subTtlNet = true;
						// System.Xut.println("valSub="+valSub+" subTotaling="+subTotaling+" val="+val);

						// loanLossAmt is difficult to isolate as pls - so for
						// netting approach - I see what happens when I switch
						// to subtract instead of plus above (loanLossAmt*2)
						if (Math.abs(subTtlNetting - loanLossAmt * 2 - val) < 0.000001 && cntSubTtlnetting > 1
								&& ((!isHtml && Math.abs(eIdx - lastColEidx) < 3)
										|| (isHtml && Math.abs(eIdx - lastColEidx) == 0)))
							subTtlNet = true;

						// keep having preVal adding with earlier prevVal unless
						// equal - then zero it out. Not sure if preVal is
						// correct location in loop. It needs to be w/n loop of
						// mapreverse prior to removal of mapreverse rows I
						// believe. See acc open in mysql

						/*
						 * #have to play with net / sub - e.g., if 'expenses' and value is positive -
						 * change to negative. #when netting I'm taking a top line # which is usually a
						 * positive # is I/s -- such as income #and then so long as all numbers after it
						 * are negative (expense/tax) - I subtract - and that #should equal net value.
						 * But I go in reverse - so start with net value (inc) and find all #expenses
						 * above it - adjusting values based on key (losses) and expenses are negative
						 * and then #when I get to row with income I add the trailing summed value
						 * unless a net value was hit prior #to that.
						 */

						/*
						 * if(Math.abs(valSub - prevVal) == Math.abs(val)) subTract = true; -- above
						 * methodology is of no use
						 */

						if (subTtl || subTtlNet) {
							rowname = listStrAry.get(0)[1].trim();

							// System.Xut.println("subtotal found for this row="
							// + rowname + "\rsubtotal equals="
							// + subTotaling + " cnt==" + cnt);

							// if rowname is blank - get rnh (first row prior
							// start of row that starts subtotal if it meets rnh
							// conditions)
							if (rowname.trim().length() < 10 && null != mapReversed.get(keyRev + 1)
									&& mapReversed.get(keyRev + 1).get(0)[1].toUpperCase().contains("RNH")) {

								String rownameheader = mapReversed.get(keyRev + 1).get(0)[1];

								String tmpS = rowname;
								if (rowname.trim().length() <= 3) {
									rowname = "RNH " + rownameheader + "set";
									if (rowname.contains("RNH RNH"))
										rowname = tmpS;
								}

								else {
									rowname = rowname + "RNH " + rownameheader + "set";
									if (rowname.contains("RNH RNH"))
										rowname = tmpS;
								}

							}

							// mapReversed will have all rows used to subtotal.
							// 1st key will equal subtotal row. So skip that.
							// Next
							subTotaling = 0.0;
							subTtlNetting = 0.0;
							maxCntReverse = cnt;
							// System.Xut.println("maxCnt="+maxCnt+" maxCntReverse="+maxCntReverse);
							subtotalFound = true;

							// revise any rows that are part of subtotal with ST
							// tag and subtotal with TL tag
							int key2 = 0, kCnt = 0, subTotK = 0, maxCntReverseRename = maxCntReverse;
							for (Map.Entry<Integer, List<String[]>> entry2 : mapReversed.entrySet()) {
								maxCntReverseRename--;
								if (maxCntReverseRename < 0)
									break;
								key2 = entry2.getKey();
								kCnt++;
								List<String[]> tmpList = new ArrayList<>();
								tmpList = entry2.getValue();
								for (int i = 0; i < tmpList.size(); i++) {
									if (i == 0 && kCnt == 1) {
										String tmpStr = " ;TL";
										if (subTtlNet)
											tmpStr = " ;NET";

										subTotK = key2 * -1;
										String row = entry2.getValue().get(i)[0];
										for (int z = 0; z < entry2.getValue().size(); z++) {
											// System.Xut.println("each entry2
											// ary="+Arrays.toString(entry2.getValue().get(z)));
										}
										// System.Xut.println("entry2.getValue().size="+entry2.getValue().size());
										// System.Xut.println("subtotal i=" +
										// i);
										// System.Xut.println("entry2.getValue().get(i)[2]="
										// + entry2.getValue().get(i)[2]);
										String revisedRowname = rowname + tmpStr + subTotK + ";";
										String sIdx = entry2.getValue().get(i)[2];
										String[] ary = { row, revisedRowname, sIdx };
										// System.Xut.println("revisedRowname="+revisedRowname);
										tmpList.set(i, ary);
									}

									if (i == 0 && kCnt > 1) {
										String tmpStr = " ;ST";
										if (subTtlNet)
											tmpStr = " ;SUB";

										String row = entry2.getValue().get(i)[0];
										String revisedRowname = entry2.getValue().get(i)[1] + tmpStr + subTotK + ";";

										// System.Xut.println("entry2.getValue().size="+entry2.getValue().size());
										// System.Xut.println("subtotal i=" +
										// i);
										// System.Xut.println("entry2.getValue().get(i)[0]="
										// + entry2.getValue().get(i)[0]);
										// System.Xut.println("entry2.getValue().get(i)[1]="
										// + entry2.getValue().get(i)[1]);
										// System.Xut.println("entry2.getValue().get(i)[2]="
										// + entry2.getValue().get(i)[2]);

										String sIdx = entry2.getValue().get(i)[2];
										String[] ary = { row, revisedRowname, sIdx };
										tmpList.set(i, ary);
										// System.Xut.println("revisedRowname="+revisedRowname);
									}

									// System.Xut.println("key=" + key +
									// "| list string array="
									// +
									// Arrays.toString(entry2.getValue().get(i)));
								}
								// System.Xut.println("printing tmpList with revised rowname");
								// nlp.printListOfStringArray(tmpList);
							}

							if (subtotalFound) {
								int k = 0, cnt2 = 0, subTotalKey = 0, prevK = -1;
								// System.Xut
								// .println("printing mapReversed prior to key removal");
								// nlp.printMapIntListOfStringAry(mapReversed);
								for (Map.Entry<Integer, List<String[]>> e : mapReversed.entrySet()) {
									// System.Xut.println("prevK="+prevK+
									// " k="+k+" cnt2="+cnt2);
									k = e.getKey();
									if (prevK != k) {
										cnt2++;
										// System.Xut.println("cnt2="+cnt2);
									}

									if (cnt2 == 1)
										subTotalKey = e.getKey();
									// System.Xut.println("subTotalKey="+subTotalKey);
									prevK = k;
									// System.Xut.println("k=="+" subTotalKey="+subTotalKey);
									if (k == subTotalKey)
										continue;
									// System.Xut.println("cnt2="+cnt2+
									// " maxCntReverse="+maxCntReverse);
									if (cnt2 > maxCntReverse)
										break;
									// System.Xut
									// .println("removing this key=" + k);
									mapReversed.remove(k);
								}
								// System.Xut
								// .println("map at subtotal after removing subtotaled rows==");
								// nlp.printMapIntListOfStringAry(mapReversed);
							}
							prevVal = val;
							break;
						}
						subTtl = false;
						subTtlNet = false;
					}
				}

				// removes all lines prior to the subtotal row
				// that equals the sum of the subtotal. the
				// first row (cnt2=0) is subtotaled row (keep)
				// and cnt is number of rows that sum to
				// subtotal. Delete all rows that subtotaled b/c
				// they are no longer necessary to determine if
				// future rows subtotal - if future rows do they
				// will subtotal based on prior subtotals or new rows.

			}
		}

		int subTotK = 0;
		return mapDataNavigable;

	}

	public List<String> mergeTwoLists(List<String> largeList, List<String> smallList, boolean reverse) {
		List<String> mergedList = new ArrayList<>();
		int cntL = largeList.size(), cntS = smallList.size();

		// System.Xut.println("cntS=" + cntS + " cntL=" + cntL);
		if (cntS == 0)
			return largeList;

		int l = 0;
		String mergCh;
		if (cntL / cntS >= 1 && cntL % cntS == 0) {
			for (int s = 0; s < cntS; s++) {
				for (int x = 0; x < cntL / cntS; x++) {
					if (!reverse) {
						// System.Xut.println("getting largeList index=" + l
						// + " smallList index=" + s + " largeList="
						// + largeList.get(l) + " smallList="
						// + smallList.get(s));
						mergCh = largeList.get(l) + " " + smallList.get(s);
					} else {
						// System.Xut.println("getting largeList index=" + l
						// + " smallList index=" + s);
						mergCh = smallList.get(s) + " " + largeList.get(l);
					}
					// System.Xut.println("merged 2 chs=" + mergCh);
					l++;
					mergedList.add(mergCh);
				}
			}
		}

		String mergChCk, mrgSm;
		int s = 0;
		if (cntL / cntS >= 1 && cntL % cntS != 0) {
			for (int x = 0; x < cntL; x++) {
				if (x == 0) {
					s = 0;
					mrgSm = smallList.get(s);
				} else if (x == (cntL - 1)) {
					s = (cntS - 1);
					mrgSm = smallList.get(s);
				} else {
					mrgSm = "";
				}

				if (!reverse) {
					// System.Xut.println("getting largeList index=" + x
					// + " smallList index=" + s);
					mergChCk = largeList.get(x) + " " + mrgSm;
				} else {
					// System.Xut.println("getting largeList index=" + x
					// + " smallList index=" + s);
					mergChCk = mrgSm + " " + largeList.get(x);
				}
				// System.Xut.println("merged 2 chs=" + mergCh);
				mergedList.add(mergChCk);
			}
		}

		return mergedList;
	}

	public static String countPeriod(String text) {

		// used to confirm sole period count. All relates only to
		// allColText and tsPattern where format is:
		// |1231|12.0|M1:December 31|P1:52 weeks|

		// System.Xut
		// .println("NLP.countPeriod - text to get period count=" + text);

		NLP nlp = new NLP();

		// if inception pattern period is corrupt - so set greater than 1.
		if (nlp.getAllIndexEndLocations(text, NLP.patternInception).size() > 0)
			return null;

		String[] textSplit = text.split("\\|");
		String perStr = "";
		// find solePeriod tsPattern or allColText
		double per = 0, pCnt = 0, prevPer = 0;
		boolean isNumber = false;
		// if pCntD:2 but both are '12' but derive from '52' and '53' week - it
		// should be pCntD:1. Move this method to location where tsPattern
		// generates pCntD, mCntD etc.
		for (int i = 0; i < textSplit.length; i++) {
			isNumber = false;
			// removes L1C1: from allcoltext
			perStr = textSplit[i].replaceAll("L\\dC\\d:", "");
			// System.Xut.println("perStr=" + perStr);
			if (perStr.contains("pC"))
				break;
			isNumber = nlp.isNumeric(perStr);
			// System.Xut.println("isNumber" + isNumber);
			if (perStr.length() == 0 || !isNumber)
				continue;
			per = Double.parseDouble(perStr);
			// System.Xut.println("per=" + per + " prevPer=" + prevPer);
			if ((pCnt == 0 || per != prevPer) && per > 0 && per < 13) {
				pCnt++;

				// System.Xut.println("prevPr=" + prevPer + " pCnt=" + pCnt
				// + " per=" + per);
				prevPer = per;
			}
		}
		// System.Xut.println("returned pCnt=" + pCnt);
		if (pCnt == 1) {
			return prevPer + "";
		} else
			return null;
	}

	public static String countMonth(String text) {

		// used to confirm sole sole month count.

		// System.Xut
		// .println("NLP.countPerdiod - text to get period count=" + text);

		NLP nlp = new NLP();

		// if inception pattern period is corrupt - so set greater than 1.
		if (nlp.getAllIndexEndLocations(text, NLP.patternInception).size() > 0)
			return null;

		String[] textSplit = text.split("\\|");
		String moDayStr = "", prevMoDayStr = "", moStr = "", prevMoStr = "", dayStr = "", prevDayStr = "";
		// find solePeriod tsPattern or allColText
		int mCnt = 0, dayDiff = 100;
		// if pCntD:2 but both are '12' but derive from '52' and '53' week - it
		// should be pCntD:1. Move this method to location where tsPattern
		// generates pCntD, mCntD etc.
		for (int i = 0; i < textSplit.length; i++) {
			// removes L1C1: from allcoltext
			moDayStr = textSplit[i].replaceAll("L\\dC\\d:", "");
			// System.Xut.println("moDayStr=" + moDayStr);
			if (moDayStr.contains("pC"))
				break;
			if (moDayStr.length() == 0)
				continue;

			moStr = moDayStr.substring(0, 2);
			dayStr = moDayStr.substring(2, 4);

			// System.Xut.println("moDayStr=" + moDayStr + " moStr=" + moStr
			// + " dayStr" + dayStr + " prevMoStr=" + prevMoStr);
			if ((mCnt == 0 || !moStr.equals(prevMoStr)) && nlp
					.getAllIndexEndLocations(moDayStr, Pattern.compile("(0[\\d]|1[0-2]{1})([0-2]{1}\\d|3[0-1]{1})"))
					.size() > 0) {

				if (prevMoStr.length() > 0 && prevDayStr.length() > 0) {
					dayDiff = NLP.getMonthDayDifference(moStr, prevMoStr, dayStr, prevDayStr);
				}

				// System.Xut.println("day diff=" + dayDiff);

				if (Math.abs(dayDiff) < 8) {
					continue;
				}

				mCnt++;

				// System.Xut.println("a prevPr=" + prevMoStr + " mCnt=" + mCnt
				// + " moStr=" + moStr);
				prevMoStr = moStr;
				prevDayStr = dayStr;
				prevMoDayStr = moDayStr;
			}
		}
		// System.Xut.println("returned mCnt=" + mCnt);

		if (mCnt == 1) {
			// System.Xut.println("returned moDayStr=" + prevMoDayStr);
			return prevMoDayStr + "";
		} else
			return null;
	}

	public static String[] getSoleMonthSolePeriod(String allColText, String tsPattern) {

		// returns the sole month and sole period found in columnHeading
		// (allColText) after checking that it matches the value found in
		// tableSentence (where there is a value). countPeriod and countMonth
		// scrubs the allcolText and tsPattern data (after formatted) to
		// determine if there are 1 or more or none periods or months

		NLP nlp = new NLP();
		// getTableSentencePatterns formats allColText so that pCnt can occur at
		// getMonthYearPeriodFromTSpatternFormatted
		allColText = nlp.getTableSentencePatterns(allColText, "0");
		// need to replace #L so that M1, P1 etc can be picked up in method
		// below
		allColText = getMonthYearPeriodFromTSpatternFormatted(allColText.replaceAll("\\dL", ""));

		String solePeriodTsPattern = NLP.countPeriod(allColText);
		// System.Xut.println("solePeriodTsPattern=" + solePeriodTsPattern);
		String solePeriodAllColText = NLP.countPeriod(tsPattern);
		// System.Xut.println("solePeriodAllColText=" + solePeriodAllColText);
		// get largest value to determine if I have sole period value.
		String solePeriod = "";

		if ((solePeriodAllColText != null && solePeriodTsPattern != null
				&& solePeriodAllColText.equals(solePeriodTsPattern))
				|| (solePeriodAllColText != null && solePeriodTsPattern == null)

		) {
			solePeriod = solePeriodAllColText;
		}

		if (solePeriodAllColText == null && solePeriodTsPattern != null) {
			solePeriod = solePeriodTsPattern;
		}

		// System.Xut.println("getSoleMonthSolePeriod solePeriod=" +
		// solePeriod);

		// get sole month

		String soleMonthTsPattern = NLP.countMonth(allColText);
		// System.Xut.println("soleMonthTsPattern=" + soleMonthTsPattern);
		String soleMonthAllColText = NLP.countMonth(tsPattern);
		// System.Xut.println("soleMonthAllColText=" + soleMonthAllColText);
		// get largest value to determine if I have sole month value.
		String soleMonth = "";

		if ((soleMonthAllColText != null && soleMonthTsPattern != null
				&& soleMonthAllColText.equals(soleMonthTsPattern))
				|| (soleMonthAllColText != null && soleMonthTsPattern == null)

		) {
			soleMonth = soleMonthAllColText;
		}

		if (soleMonthAllColText == null && soleMonthTsPattern != null) {
			soleMonth = soleMonthTsPattern;
		}

		String[] soleMonthSolePeriod = { soleMonth, solePeriod };
		// System.Xut.println("getSoleMonthSoleMonth soleMonthPeriod ary="
		// + Arrays.toString(soleMonthSolePeriod));

		return soleMonthSolePeriod;

	}

	public static List<String> getDistinctPeriodMonthYearCount(String text) throws IOException, ParseException {

		NLP nlp = new NLP();

		String prevMatch = "", match = "";
		int pCntD, mCntD, yCntD;
		List<String> tmpCHcntList = new ArrayList<String>();

		tmpCHcntList.addAll(nlp.getAllMatchedGroups(text, TableParser.EndedHeadingPattern));

		int cnt = 0;
		for (int i = 0; i < tmpCHcntList.size(); i++) {
			match = tmpCHcntList.get(i).trim();
			if (i == 0)
				cnt++;
			if (i > 0 && !match.equals(prevMatch))
				cnt++;
			prevMatch = match;
		}
		pCntD = cnt;

		if (nlp.getAllIndexEndLocations(text, TableParser.OddBallPeriodPattern).size() > 0) {
			pCntD = 0;
		}

		// System.Xut
		// .println("NLP getDistinctPeriodMonthYearCount - all ended patterns from nlp
		// text=");
//		NLP.printListOfString("@NLP getDistinctPeriodMonthYearCount - tmpCHcntList", tmpCHcntList);
		tmpCHcntList.clear();

		tmpCHcntList.addAll(nlp.getAllMatchedGroups(text, TableParser.MonthPatternSimple));
		// System.Xut
		// .println("getDistinctPeriodMonthYearCount all months patterns from text=");
		// nlp.printListOfString(tmpCHcntList);

		cnt = 0;
		String oneMonth = "";
		for (int i = 0; i < tmpCHcntList.size(); i++) {
			// only get month name - don't parse - day - first split=month name.
			match = tmpCHcntList.get(i).trim().split(" ")[0];
			oneMonth = tmpCHcntList.get(i).trim();

			if (i == 0)
				cnt++;
			if (i > 0 && !match.equals(prevMatch))
				cnt++;
			prevMatch = match;
		}
		mCntD = cnt;
		if (mCntD != 1)
			oneMonth = "";

		if (oneMonth.length() > 0) {
			// if only 1 month in limited startGroup - oneMonth is populated
			// with it else it is empty
			oneMonth = nlp.getEnddatePeriod(oneMonth).get(0);
			// System.Xut.println("oneMonth in mm-dd format=" + oneMonth);
		}

		tmpCHcntList.clear();
		tmpCHcntList.addAll(nlp.getAllMatchedGroups(text, TableParser.YearOrMoDayYrPattern));
		// System.Xut
		// .println("getDistinctPeriodMonthYearCount all year patterns from
		// allcoltext=");
		// nlp.printListOfString(tmpCHcntList);

		cnt = 0;
		for (int i = 0; i < tmpCHcntList.size(); i++) {
			match = tmpCHcntList.get(i).trim();
			if (i == 0)
				cnt++;
			if (i > 0 && !match.equals(prevMatch))
				cnt++;
			prevMatch = match;
		}
		yCntD = cnt;

		text = "|pCntD:" + pCntD + "|yCntD:" + yCntD + "|mCntD:" + mCntD;

		List<String> listPatternOneMonth = new ArrayList<>();
		listPatternOneMonth.add(text);
		listPatternOneMonth.add(oneMonth);
		// System.Xut.println("added tsPattern = "+text+" added oneMonth="+oneMonth);
		return listPatternOneMonth;

	}

	public static String[] getDistinctPeriodMonthYearCountHtml(String text) throws IOException, ParseException {

		NLP nlp = new NLP();

		String prevMatch = "", match = "";
		int pCntD, mCntD, yCntD;
		List<String> tmpCHcntList = new ArrayList<String>();

		tmpCHcntList.addAll(nlp.getAllMatchedGroups(text, TableParser.EndedHeadingPattern));

		int cnt = 0;
		for (int i = 0; i < tmpCHcntList.size(); i++) {
			match = tmpCHcntList.get(i).trim();
			if (i == 0)
				cnt++;
			if (i > 0 && !match.equals(prevMatch))
				cnt++;
			prevMatch = match;
		}
		pCntD = cnt;

		// System.Xut
		// .println("NLP getDistinctPeriodMonthYearCount - all ended patterns from nlp
		// text=");

//		NLP.printListOfString("@NLP getDistinctPeriodMonthYearCount - tmpCHcntList", tmpCHcntList);
		tmpCHcntList.clear();

		tmpCHcntList.addAll(nlp.getAllMatchedGroups(text, TableParser.MonthPatternSimple));
		// System.Xut
		// .println("getDistinctPeriodMonthYearCount all months patterns from text=");
		// nlp.printListOfString(tmpCHcntList);

		cnt = 0;
		String oneMonth = "";
		for (int i = 0; i < tmpCHcntList.size(); i++) {
			// only get month name - don't parse - day - first split=month name.
			match = tmpCHcntList.get(i).trim().split(" ")[0];
			oneMonth = tmpCHcntList.get(i).trim();

			if (i == 0)
				cnt++;
			if (i > 0 && !match.equals(prevMatch))
				cnt++;
			prevMatch = match;
		}
		mCntD = cnt;
		if (mCntD != 1)
			oneMonth = "";

		if (oneMonth.length() > 0) {
			// if only 1 month in limited startGroup - oneMonth is populated
			// with it else it is empty
			oneMonth = nlp.getEnddatePeriod(oneMonth).get(0);
			// System.Xut.println("oneMonth in mm-dd format=" + oneMonth);
		}

		tmpCHcntList.clear();
		tmpCHcntList.addAll(nlp.getAllMatchedGroups(text, TableParser.YearOrMoDayYrPattern));
		// System.Xut
		// .println("getDistinctPeriodMonthYearCount all year patterns from
		// allcoltext=");
		// nlp.printListOfString(tmpCHcntList);

		cnt = 0;
		for (int i = 0; i < tmpCHcntList.size(); i++) {
			match = tmpCHcntList.get(i).trim();
			if (i == 0)
				cnt++;
			if (i > 0 && !match.equals(prevMatch))
				cnt++;
			prevMatch = match;
		}
		yCntD = cnt;

		text = "|pCntD:" + pCntD + "|yCntD:" + yCntD + "|mCntD:" + mCntD;

		String[] ary = { text, oneMonth };
		return ary;

	}

	public static List<Integer[]> getRowSpans(Elements chRows) {

		NLP nlp = new NLP();

		List<Integer[]> listRowSpan = new ArrayList<>();
		String rowSpanStr = "", colSpanStr = "";
		int rowSpan = 0, colSpan = 0, adjIdx = 0;
		Elements TDs;
		Element td;
		for (int row = 0; row < chRows.size(); row++) {
			TDs = chRows.get(row).getElementsByTag("td");
			rowSpanStr = "";
			colSpanStr = "";
			colSpan = 0;
			rowSpan = 0;
			adjIdx = 0;
			for (int colIdx = 0; colIdx < TDs.size(); colIdx++) {
				rowSpan = 0;
				colSpan = 0;
				td = TDs.get(colIdx);
				rowSpanStr = td.attr("rowspan");
				colSpanStr = td.attr("colspan");

				if (colSpanStr.length() > 0 && nlp.isNumeric(colSpanStr) && !colSpanStr.contains(".")) {
					colSpan = Integer.parseInt(colSpanStr) - 1;
					adjIdx = adjIdx + colSpan;
					// System.Xut.println("row=" + row + " colIdx=" + colIdx
					// + " colSpan=" + colSpan);
				}
				// rowSpan list = rowSpanStart start row (row+1),rowSpanEnd row,
				// rowSapn col idx start, rowSpan col idx end. If 2 or more
				// rowspans on same row with same span separated 1 col idx -
				// discard earlier - serves no purpose.

				if (rowSpanStr.length() > 0) {
					if (nlp.isNumeric(rowSpanStr) && !rowSpanStr.contains(".")) {
						rowSpan = Integer.parseInt(rowSpanStr) - 1;
						Integer[] aryInt = { row + 1, row + rowSpan, adjIdx - colSpan + colIdx, adjIdx + colIdx };
						listRowSpan.add(aryInt);
						// System.Xut.println("printing rowSpan intAry="
						// + Arrays.toString(aryInt));
					}
				}
			}
		}

//		NLP.printListOfIntegerArray("listRowSpan", listRowSpan);
		return listRowSpan;
	}

	public static String getColHdgAllLinesPatterns(String colHdgAllLinesPatterns, List<String> CHs) {
		NLP nlp = new NLP();

		String tmpStr = "";
		List<String> listCHpatterns = new ArrayList<>();

		for (int e = 0; e < CHs.size(); e++) {
//			NLP.pwNLP.append(NLP.println("CHs.get(e)==", CHs.get(e)));
			tmpStr = nlp.getDateFromSlashMoDyYear(CHs.get(e)).trim();
			listCHpatterns.add(nlp.getTableSentencePatterns(tmpStr, (e + 1) + ""));

		}

		colHdgAllLinesPatterns = "";

		for (int c = 0; c < listCHpatterns.size(); c++) {
			// System.Xut.println("colHdgAllLinesPatterns="
			// + listCHpatterns.get(c));
			colHdgAllLinesPatterns = colHdgAllLinesPatterns + listCHpatterns.get(c);
			// System.Xut.println("colHdgAllLinesPatterns="
			// + listCHpatterns.get(c));
		}

		colHdgAllLinesPatterns = colHdgAllLinesPatterns.replaceAll("[\\|]{2,}", "\\|");

		return colHdgAllLinesPatterns;
	}

	public static String getEx99s(String text) throws IOException {

		String tmpStr = "";

		NLP nlp = new NLP();

		List<Integer> listEx99s = nlp.getAllIndexStartLocations(text, Pattern.compile("<TYPE>EX-99."));
		List<Integer> listEx99End = nlp.getAllIndexEndLocations(text, Pattern.compile("</TEXT>"));

		if (listEx99s.size() < 1 || listEx99End.size() < 1)
			return "";

		int sIdx = 0, eIdx = 0;
		for (int i = 0; i < listEx99s.size(); i++) {
			sIdx = listEx99s.get(i);
			for (int n = 0; n < listEx99End.size(); n++) {
				eIdx = listEx99End.get(n);
				if (eIdx > sIdx) {
					tmpStr = tmpStr + "\r\r" + text.substring(sIdx, eIdx);
					break;
				}
			}
		}

		return tmpStr;
	}

	public static String removeEx99s(String text) throws IOException {

		String tmpStr = "";

		NLP nlp = new NLP();

		List<Integer> listEx99s = nlp.getAllIndexStartLocations(text, Pattern.compile("<TYPE>EX-99."));
		if (listEx99s.size() < 1)
			return text;
		List<Integer> listEx99End = nlp.getAllIndexEndLocations(text, Pattern.compile("</TEXT>"));

		if (listEx99End.size() < 1)
			return text;

		int sIdx = 0, eIdx = 0, prevEidx = 0;
		for (int i = 0; i < listEx99s.size(); i++) {
			sIdx = listEx99s.get(i);
			for (int n = 0; n < listEx99End.size(); n++) {
				eIdx = listEx99End.get(n);
				if (eIdx > sIdx) {
					tmpStr = tmpStr + "\r\r" + text.substring(prevEidx, sIdx);
					prevEidx = eIdx;
					break;
				}
			}
		}
		tmpStr = tmpStr + "\r\r" + text.substring(eIdx, text.length());

		return tmpStr;
	}

	public static String removeGobblyGookGetContracts(String text) throws IOException {

		String tmpStr = "";

		NLP nlp = new NLP();

		List<Integer> listPdfStart = nlp.getAllIndexStartLocations(text, Pattern.compile("(?i)<PDF>"));
		List<Integer> listPdfEnd = nlp.getAllIndexEndLocations(text, Pattern.compile("(?i)</PDF>"));

//		System.out.println("text len before removeGobblyGook text.len==" + text.length());
//		System.out.println("listPdfStart.size=" + listPdfStart.size());
//		System.out.println("listPdfEnd.size=" + listPdfEnd.size());

		if (listPdfStart.size() >= 1 && listPdfEnd.size() >= 1) {
			int n = 0, i = 0, sIdx = 0, eIdx = 0, prevEidx = 0;
			for (; i < listPdfStart.size(); i++) {
				sIdx = listPdfStart.get(i);
				for (; n < listPdfEnd.size(); n++) {
					eIdx = listPdfEnd.get(n);
					if (eIdx > sIdx && prevEidx < sIdx) {
						tmpStr = tmpStr + "\r\n" + text.substring(prevEidx, sIdx);
						prevEidx = eIdx;
						break;
					}
				}
			}

			text = tmpStr;
		}

		List<Integer> listEx99Start = nlp.getAllIndexStartLocations(text,
				Pattern.compile("(?sm)(<FILENAME>).{0,70}" + "\\.(jpg|gif|GIF|pdf|PDF|jpeg|JPEG)"));
		List<Integer> listEx99End = nlp.getAllIndexEndLocations(text, Pattern.compile("end|</TEXT>"));

		// System.out.println("1");
//		System.out.println("tmpStr.len=" + tmpStr.length());
//		System.out.println("listEx99Start.size=" + listEx99Start.size());
//		System.out.println("listEx99End.size=" + listEx99End.size());

		if (listEx99Start.size() < 1 || listEx99End.size() < 1) {
//			System.out.println("1a.text len after removeGobblyGook text.len==" + text.length()+" txt snip="+text.substring(0,1000));
			return text;
		}

//		System.out.println("1 text.len=" + text.length() + " listEx99Start.size=" + listEx99Start.size());
		if (listEx99End.size() != listEx99Start.size()) {
//			System.out.println("1b.text len after removeGobblyGook text.len==" + text.length()+" txt snip="+text.substring(0,1000));
			return text;
		}

		int sIdx = 0, eIdx = 0, prevEidx = 0, n = 0, i = 0;
		for (; i < listEx99Start.size(); i++) {
//			System.out.println(
//					"listEx99Start snip-==" + text.substring((listEx99Start.get(i) - 10), (listEx99Start.get(i) + 10)));
			sIdx = listEx99Start.get(i);
			for (; n < listEx99End.size(); n++) {
				eIdx = listEx99End.get(n);
				if (eIdx == 1 && sIdx == 1) {
					tmpStr = tmpStr + "\r\n" + text.substring(sIdx, eIdx);
				}

				if (eIdx > 1 && sIdx > 1) {
					tmpStr = tmpStr + "\r\n" + text.substring(prevEidx, sIdx);
				} else {

//					System.out.println("prevIdx=" + prevEidx + " sIdx=" + sIdx);
					tmpStr = tmpStr + "\r\n" + text.substring(prevEidx, sIdx);
//					System.out.println("n=" + n + " 2a=" + tmpStr.length());
					prevEidx = eIdx;
					break;
				}
			}
		}

//		System.out.println("2 text.len="+text.length());
		tmpStr = tmpStr + "\r\n" + text.substring(eIdx, text.length());
		System.out.println("text len after removeGobblyGook text.len==" + text.length());
		return tmpStr;

	}

//	public static String removeGobblyGook(String text) throws IOException {
//
//		String tmpStr = "";
//
//		NLP nlp = new NLP();
//
//		List<Integer> listPdfStart = nlp.getAllIndexStartLocations(text, Pattern.compile("(?i)<PDF>"));
//		List<Integer> listPdfEnd = nlp.getAllIndexEndLocations(text, Pattern.compile("(?i)</PDF>"));
//
//		if (listPdfStart.size() >= 1 && listPdfEnd.size() >= 1) {
//			int sIdx = 0, eIdx = 0, prevEidx = 0;
//			for (int i = 0; i < listPdfStart.size(); i++) {
//				sIdx = listPdfStart.get(i);
//				for (int n = 0; n < listPdfEnd.size(); n++) {
//					eIdx = listPdfEnd.get(n);
//					if (eIdx > sIdx) {
//						tmpStr = tmpStr + "\r\r" + text.substring(prevEidx, sIdx);
//						prevEidx = eIdx;
//						break;
//					}
//				}
//			}
//
//			text = tmpStr;
//		}
//
//		List<Integer> listEx99s = nlp.getAllIndexStartLocations(text,
//				Pattern.compile("<FILENAME>[\\p{Alnum}\\p{Punct} ].{0,150}\\.(jpg|gif|GIF|pdf|PDF|jpeg|JPEG)"));
//		List<Integer> listEx99End = nlp.getAllIndexEndLocations(text, Pattern.compile("</TEXT>"));
//
//		if (listEx99s.size() < 1 || listEx99End.size() < 1)
//			return text;
//
//		int sIdx = 0, eIdx = 0, prevEidx = 0;
//		for (int i = 0; i < listEx99s.size(); i++) {
//			sIdx = listEx99s.get(i);
//			for (int n = 0; n < listEx99End.size(); n++) {
//				eIdx = listEx99End.get(n);
//				if (eIdx > sIdx) {
//					tmpStr = tmpStr + "\r\r" + text.substring(prevEidx, sIdx);
//					prevEidx = eIdx;
//					break;
//				}
//			}
//		}
//		tmpStr = tmpStr + "\r\r" + text.substring(eIdx, text.length());
//
////		PrintWriter pw = new PrintWriter(new File("c:/getContracts/gobbly.txt"));
////		pw.append(text);
////		pw.close();
//
//		return tmpStr;
//	}

	public static List<String[]> reorderListsOfStringAry(List<String[]> list, int aryInstance,
			int numberOfDataColumns) {

		List<String[]> reorderedList = new ArrayList<>();

		Map<Integer, String[]> map = new TreeMap<Integer, String[]>();

		for (int i = 0; i < list.size(); i++) {
			map.put(Integer.parseInt(list.get(i)[aryInstance]), list.get(i));
		}

		int prevKey = 0, key;
		for (Map.Entry<Integer, String[]> entry : map.entrySet()) {
			key = entry.getKey();
//			NLP.pwNLP.append(
//					NLP.println("reordered list key|", key + "|ary val|" + Arrays.toString(entry.getValue()) + "|"));
			// add any missing columns as blanks.
			if ((prevKey + 1) != key) {
				for (int i = 0; i < (key - prevKey); i++) {
					String[] ary = { "", "", "", "" };
					reorderedList.add(ary);
				}
			}
			reorderedList.add(entry.getValue());
			prevKey = key;
		}

		return reorderedList;
	}

	public Map<Integer, String> getStartLocsAndMatchedGroups(String text, Pattern pattern, String prefix) {
		// returns both the idxs (key) and word (value).
		// the program will accept the text - and run the pattern and from that
		// will record into the map the idx and value

		Map<Integer, String> map = new TreeMap<Integer, String>();

		if (text != null) {
			Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				map.put(matcher.start(), (null != prefix ? prefix : "") + matcher.group());
				// System.Xut.println("match==" + matcher.group());
			}
		}

		return map;
	}

	public int getPeriodFromText(String text) {

		@SuppressWarnings("unused")
		int period = 0, p = 0;

		if (TableParser.Period12MonthsPattern.matcher(text).find()
				|| text.replaceAll("[ ]{2,}", " ").toLowerCase().contains("for the year")
				|| text.replaceAll("[ ]{2,}", " ").toLowerCase().contains("year ended")) {
			p++;
			period = 12;
			// System.Xut.println("1 getEnddatePeriod="+period);
		}
		// System.Xut.println("p count="+p);

		if (TableParser.Period9MonthsPattern.matcher(text).find()) {
			p++;
			period = 9;

			// System.Xut.println("2 getEnddatePeriod="+period);
		}
		// System.Xut.println("p count="+p);

		if (TableParser.Period6MonthsPattern.matcher(text).find()) {
			p++;
			period = 6;
			// System.Xut.println("3 getEnddatePeriod="+period);
		}

		if (TableParser.Period3MonthsPattern.matcher(text).find() && !text.toLowerCase().contains("year")) {
			p++;
			period = 3;
			// System.Xut.println("4 getEnddatePeriod=" + period);
		}
		// System.Xut.println("p count="+p);

		// if above don't match to period = try==>
		if (period == 0) {
			if (text.toLowerCase().contains("year") && text.toLowerCase().contains("years")) {
				p++;
				period = 12;
				// System.Xut.println("0.12 period="+period);
				// System.Xut.println("5 p="+p);

			}

			if (text.toLowerCase().contains("quarter")) {
				p++;
				period = 3;
				// System.Xut.println("0.qtr period="+period);
				// System.Xut.println("6 p="+p);
			}

			if (text.toLowerCase().contains("the seven") || text.toLowerCase().contains("seven mo")) {
				p++;
				period = 7;
				// System.Xut.println("0.qtr period=" + period);
				// System.Xut.println("6 p="+p);
			}
		}

		// System.Xut.println("text=" + text + " period=" + period);
		return period;

	}

	public String getYearFromText(String text) {

		@SuppressWarnings("unused")
		int y = 0;
		String year = "";

		Matcher matcher = yPattern.matcher(text);
		while (matcher.find()) {
			y++;
			year = matcher.group().trim();
			// System.Xut.println("getEnddatePeriod year="+year);
		}
		// System.Xut.println("y="+y);

		return year;
	}

	public String getMonthFromText(String text) {
		TableParser tp = new TableParser();

		@SuppressWarnings("unused")
		int m = 0;
		String month = "";

		Matcher matcher = mPattern.matcher(text);
		while (matcher.find()) {
			m++;
			month = tp.getCorrectMonthName(matcher.group().replaceAll(" ", "").trim());
			// System.Xut.println("getEnddatePeriod month="+month);
		}
		// System.Xut.println("m=" + m);

		return month;
	}

	public String getDayFromText(String text) {
		String day = "";

		Matcher matcher = dPattern.matcher(text);
		while (matcher.find()) {
			day = matcher.group().replaceAll(" ", "").trim();
			// System.Xut.println("day=" + day);
		}

		if (day.length() > 0 && Integer.parseInt(day) < 10) {
			day = "0" + day;
		}

		return day;
	}

	public String convertMoDayYeartoEndDate(String text) {

		String revisedText = null;

		Matcher matcher = MonthDayYearPattern.matcher(text);
		int sIdx = 0, eIdx = 0;
		if (matcher.find()) {
			// System.Xut.println("convertMoDayYear matcher.start()="
			// + matcher.start() + "\r convertMoDayYear matcher.end()="
			// + matcher.end());
			// used to mark section of text matched - later I put back text not
			// matched.
			sIdx = matcher.start();
			eIdx = matcher.end();

			String[] edtStr = matcher.group().replaceAll("/", "-").split("-");
			if (edtStr.length == 3) {
				// System.Xut.println("edtStr="+edtStr);
				String yr = edtStr[2];
				String mo = edtStr[0];
				if (mo.length() < 2)
					mo = "0" + mo;
				String dy = edtStr[1];
				if (dy.length() < 2)
					dy = "0" + dy;
				revisedText = yr + "-" + mo + "-" + dy;
			}
		}

		revisedText = text.substring(0, sIdx) + " " + revisedText + " "
				+ text.substring(eIdx, text.length()).replaceAll("[ ]{2,}", " ");

		return revisedText;
	}

	public String[] getEnddatePeriodFromTS(String edt, String per, String tableSentence) {
		TableParser tp = new TableParser();

		String[] columnHdg = { edt, per };
		columnHdg[0] = columnHdg[0].replaceAll(
				"(?i)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) ([\\d]{1,2}) ([0\\d]{2} ?)$", "$1 $2, 20$3");
		columnHdg[0] = columnHdg[0].replaceAll(
				"(?i)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) ([\\d]{1,2}) ([9\\d]{2} ?)$", "$1 $2, 19$3");
		columnHdg[0] = columnHdg[0].replaceAll("([\\d]{1,2}/[\\d]{1,2}) ([\\d]{4} ?)$", "$1/$2");
		Matcher matcher = TableTextParser.patternMonthDayYY.matcher(columnHdg[0].replaceAll("\\!", " "));
		if (matcher.find()) {
			columnHdg[0] = columnHdg[0].replaceAll("\\!", " ").replaceAll("'(?=\\d\\d)", "20");
			// System.Xut.println("nlp.getEnddatePeriod columnHdg.replace
			// MonthDayYY="+columnHdg);
		}

		matcher = TableTextParser.patternMonthDayYY.matcher(columnHdg[1].replaceAll("\\!", " "));
		if (matcher.find()) {
			columnHdg[1] = columnHdg[1].replaceAll("\\!", " ").replaceAll("'(?=\\d\\d)", "20");
			// System.Xut.println("nlp.getEnddatePeriod columnHdg.replace
			// MonthDayYY="+columnHdg);
		}

		int period = 0, y = 0, m = 0, p = 0;
		String endDate = "";
		String month = "", day = "", year = "";

		matcher = yPattern.matcher(columnHdg[0]);
		matcher = yPattern.matcher(columnHdg[0]);
		while (matcher.find()) {
			y++;
			year = matcher.group().trim();
			// System.Xut.println("A getEnddatePeriod year=" + year);
		}
		// System.Xut.println("y="+y);

		matcher = mPattern.matcher(columnHdg[0]);
		while (matcher.find()) {
			m++;

			month = tp.getCorrectMonthName(matcher.group().replaceAll(" ", "").trim());
			// System.Xut.println("getEnddatePeriod month=" + month+
			// " match before correctMonthName="+matcher.group());
		}
		// System.Xut.println("m=" + m);

		// System.Xut.println("columnHdg[1]==" + columnHdg[1]);
		if (TableParser.Period12MonthsPattern.matcher(columnHdg[1]).find()
				|| columnHdg[1].replaceAll("[ ]{2,}", " ").toLowerCase().contains("for the year")
				|| columnHdg[1].replaceAll("[ ]{2,}", " ").toLowerCase().contains("year ended")
				|| columnHdg[1].replaceAll("[ ]{2,}", " ").toLowerCase().contains("fiscal year")) {
			p++;
			period = 12;
			// System.Xut.println("2 getEnddatePeriod 12=" + period);
		}
		// System.Xut.println("p count="+p);

		if (tp.Period9MonthsPattern.matcher(columnHdg[1]).find()
				|| (columnHdg[1].contains("nine") && tableSentence.toLowerCase().contains("month"))) {
			p++;
			period = 9;

			// System.Xut.println("2 getEnddatePeriod=" + period);
		}
		// System.Xut.println("p count="+p);

		if (tp.Period6MonthsPattern.matcher(columnHdg[1]).find()
				|| (columnHdg[1].toLowerCase().contains("six") && tableSentence.toLowerCase().contains("month"))) {
			p++;
			period = 6;
			// System.Xut.println("3 getEnddatePeriod="+period);
		}

		if ((tp.Period3MonthsPattern.matcher(columnHdg[1]).find() && !columnHdg[1].toLowerCase().contains("year"))
				|| columnHdg[1].toLowerCase().contains("three months")
				|| columnHdg[1].toLowerCase().contains("the three and ")
				|| (columnHdg[1].toLowerCase().contains("three") && tableSentence.toLowerCase().contains("month"))) {

			p++;
			period = 3;
			// System.Xut.println("4 getEnddatePeriod 3=" + period);
		}
		// System.Xut.println("p count="+p);

		// if above don't match to period = try==>
		if (period == 0) {
			if (columnHdg[1].toLowerCase().contains("year") && columnHdg[1].toLowerCase().contains("years")) {
				p++;
				period = 12;
				// System.Xut.println("0.12 period=" + period);
				// System.Xut.println("5 p=" + p);

			} else if (columnHdg[1].toLowerCase().contains("quarter")) {
				p++;
				period = 3;
				// System.Xut.println("0.qtr period=" + period);
				// System.Xut.println("6 p=" + p);
			}
		}

		// System.Xut.println("p=" + p);

		matcher = dPattern.matcher(columnHdg[0]);
		while (matcher.find()) {
			day = matcher.group().replaceAll(" ", "").trim();
			// System.Xut.println("day=" + day);
		}

		if (day.length() > 0 && Integer.parseInt(day) < 10) {
			day = "0" + Integer.parseInt(day);
		}

		endDate = year + "-" + DateUtils.getMontIntegerAsString(month) + "-" + day;
		// System.Xut.println("1 endDate prelim="+endDate);
		matcher = MonthDayYearPattern.matcher(columnHdg[0]);

		if (matcher.find()) {
			String[] edtStr = matcher.group().replaceAll("/", "-").split("-");
			if (edtStr.length == 3) {
				// System.Xut.println("edtStr=" + Arrays.toString(edtStr));
				String yr = edtStr[2];
				String mo = edtStr[0];
				if (mo.length() < 2)
					mo = "0" + mo;
				String dy = edtStr[1];
				if (dy.length() < 2)
					dy = "0" + dy;
				endDate = yr + "-" + mo + "-" + dy;
			}
		}

		Pattern alphabetPattern = Pattern.compile("[A-Za-z]{4,}|\\%");
		matcher = alphabetPattern.matcher(columnHdg[0]);
		while (matcher.find()) {
			if (endDate.replaceAll("-", "").length() < 1 && endDate.length() > 3) {
				endDate = matcher.group().replaceAll(" ", "").trim();
				// System.Xut.println("oddCol=" + endDate);
			}
		}

		if (endDate.replaceAll("-", "").length() < 1)
			endDate = "";

		// System.Xut.println("cht edt=" + endDate + " cht per=" + period);
		String[] ary = { endDate, period + "" };
		return ary;
	}

	public static String getPeriodOnly(String text) {

		// if result is "-1" - it means there is a period but it is not 3,6,9 or
		// 12 but an odd ball period
		TableParser tp = new TableParser();
		@SuppressWarnings("unused")
		int p = 0, per = 0;
		String period = "";

		// System.Xut.println("text==" + text);
		if (TableParser.Period12MonthsPattern.matcher(text).find()
				|| text.replaceAll("[ ]{2,}", " ").toLowerCase().contains("for the year")
				|| text.replaceAll("[ ]{2,}", " ").toLowerCase().contains("year ended")) {
			p++;
			per = 12;
			// System.Xut.println("3 getEnddatePeriod 12=" + per);
		}
		// System.Xut.println("p count="+p);

		if (TableParser.Period9MonthsPattern.matcher(text).find()
				|| (text.contains("nine") && text.toLowerCase().contains("month"))) {
			p++;
			per = 9;

			// System.Xut.println("2 getEnddatePeriod=" + period);
		}
		// System.Xut.println("p count="+p);

		if (TableParser.Period6MonthsPattern.matcher(text).find()
				|| (text.toLowerCase().contains("six") && text.toLowerCase().contains("month"))) {
			p++;
			per = 6;
			// System.Xut.println("3 getEnddatePeriod="+period);
		}

		if ((TableParser.Period3MonthsPattern.matcher(text).find() && !text.toLowerCase().contains("year"))
				|| text.toLowerCase().contains("three months") || text.toLowerCase().contains("the three and ")
				|| (text.toLowerCase().contains("three") && text.toLowerCase().contains("month"))) {

			p++;
			per = 3;
			// System.Xut.println("4 getEnddatePeriod 3=" + period);
		}
		// System.Xut.println("p count="+p);

		// if above don't match to period = try==>
		if (per == 0) {
			if (text.toLowerCase().contains("year") && text.toLowerCase().contains("years")) {
				p++;
				per = 12;
				// System.Xut.println("0.12 period=" + period);
				// System.Xut.println("5 p=" + p);

			} else if (text.toLowerCase().contains("quarter")) {
				p++;
				per = 3;
				// System.Xut.println("0.qtr period=" + period);
				// System.Xut.println("6 p=" + p);
			}
		}

		if (per == 0) {
			Matcher matchAnyEnded = patternAnyEnded.matcher(text);
			if (matchAnyEnded.find()) {
				per = -1;
			}
		}

		period = per + "";
		return period;

	}

	public static int[] howManyMissingEdtP(List<String> list) throws ParseException, IOException {

		NLP nlp = new NLP();

		int cntP = 0, cntEdt = 0;
		for (int j = 0; j < list.size(); j++) {
//			NLP.pwNLP.append(NLP.println("howManyMissingEdtP - list.get(j)=", list.get(j)));
			// System.Xut.println(list.size());
			if (null == nlp.getAllIndexEndLocations(list.get(j), patternInception)
					|| nlp.getAllIndexEndLocations(list.get(j), patternInception).size() > 0)
				continue;
			List<String> listEPtmp = nlp.getEnddatePeriod(list.get(j));
			for (int k = 0; k < listEPtmp.size(); k++) {
//				NLP.pwNLP.append(NLP.println("listEPtmp.get(k) - edt/p=", listEPtmp.get(k)));
				if (listEPtmp.get(k).replaceAll("[12]{1}[09]{1}[0-9]{2}-[\\d]{2}-[\\d]{2}", "").trim().length() == 0) {
					cntEdt++;
				}
				if (listEPtmp.get(k).replaceAll("3|6|9|12", "").trim().length() == 0) {
					cntP++;
				}
			}
		}

		int[] ary = { list.size() - cntEdt, list.size() - cntP };

		return ary;

	}

	public List<String> getEnddatePeriod(String columnHdg) throws ParseException, IOException {
		TableParser tp = new TableParser();

//		NLP.pwNLP.append(NLP.println("1 nlp.getEnddatePeriod -- columnHdg.substr(0,30)===",
//				columnHdg.substring(0, Math.min(30, columnHdg.length()))));

		columnHdg = columnHdg.replaceAll("(?i) mo(s)?( |$)", " MONTH$1$2").replaceAll("  ", " ").replaceAll("  ", " ");
		columnHdg = columnHdg.replaceAll("(?i)Jan ", "January ");
		columnHdg = columnHdg.replaceAll("(?i)Feb ", "February ");
		columnHdg = columnHdg.replaceAll("(?i)Mar ", "March ");
		columnHdg = columnHdg.replaceAll("(?i)Apr ", "April ");
		columnHdg = columnHdg.replaceAll("(?i)Jun ", "June ");
		columnHdg = columnHdg.replaceAll("(?i)Jul ", "July ");
		columnHdg = columnHdg.replaceAll("(?i)Aug ", "August ");
		columnHdg = columnHdg.replaceAll("(?i)sept(\\.|\\!)|sep ", "September ");
		columnHdg = columnHdg.replaceAll("(?i)Oct ", "October ");
		columnHdg = columnHdg.replaceAll("(?i)Nov ", "November ");
		columnHdg = columnHdg.replaceAll("(?i)Dec ", "December ");
		columnHdg = columnHdg.replaceAll("  ", " ");

		columnHdg = columnHdg.replaceAll(
				"(?i)(January|February|March|April|May|June|July|August|September|October|November|December) ([\\d]{1,2})\\!?,? (0[\\d]{1} ?)$",
				"$1 $2, 20$3");

		columnHdg = columnHdg.replaceAll(
				"(?i)(January|February|March|April|May|June|July|August|September|October|November|December) ([\\d]{1,2})\\!?,? (9[\\d]{1} ?)$",
				"$1 $2, 19$3");

		// 30 June 1996 to June 30 1996
		columnHdg = columnHdg.trim().replaceAll(
				"(?i)(\\d\\d) (January|February|March|April|May|June|July|August|September|October|November|December) ([12]{1}[09]{1}[0-9]{2})",
				"$2 $1, $3");

		Matcher matcher = TableTextParser.patternMonthDayYY.matcher(columnHdg.replaceAll("\\!", " "));

		NLP nlp = new NLP();

		matcher = MonthDayYearPattern.matcher(columnHdg);
		if (matcher.find()) {
			// columnHdg = convertMoDayYeartoEndDate(columnHdg);

			if (columnHdg.replaceAll("-", "").length() == 6
					&& columnHdg.substring(0, 3).replaceAll("\\d\\d-", "").length() == 0
					&& !columnHdg.substring(columnHdg.length() - 2, columnHdg.length() - 1).contains("9")
					&& !columnHdg.substring(columnHdg.length() - 2, columnHdg.length() - 1).contains("2")) {

				// System.Xut.println("columnHdg"+
				// columnHdg.substring(0,Math.min(30, columnHdg.length())));

				// System.Xut.println("columnHdg length==6.
				// columnHdg="+columnHdg.substring(0,3));
				// System.Xut.println("columnHdg.substring(0,0)="+columnHdg.substring(0,1));
				if (columnHdg.substring(0, 1).equals("0")) {
					columnHdg = "20" + columnHdg;
					// System.Xut.println("col hdg 20");
				}
				if (columnHdg.substring(0, 1).equals("9")) {
					columnHdg = "19" + columnHdg;
					// System.Xut.println("col hdg 19");
				}

			}

			// System.Xut.println("mm/dd/yy converted="+columnHdg);
		}

		columnHdg = columnHdg.replaceAll("([\\d]{1,2}/[\\d]{1,2}) ([12]{1}[09]{1}[\\d]{2} ?)$", "$1/$2");
		// System.Xut.println("c nlp.getEnddatePeriod columnHdg="+columnHdg);

		int period = 0, y = 0, m = 0, p = 0;
		String endDate = "";
		String month = "", day = "", year = "";

		matcher = yPattern.matcher(columnHdg);
		matcher = yPattern.matcher(columnHdg);
		while (matcher.find()) {
			y++;
			year = matcher.group().trim();
			// System.Xut.println("A getEnddatePeriod year=" + year);
		}
		// System.Xut.println("y="+y);

//		NLP.pwNLP.append(NLP.println("@nlp getEnddatePeriod month pattern to check=", columnHdg));
		matcher = mPattern.matcher(columnHdg);
		while (matcher.find()) {
			m++;

			month = tp.getCorrectMonthName(matcher.group().replaceAll(" ", "").trim());
//			NLP.pwNLP.append(NLP.println("@nlp getEnddatePeriod month=" + month + " match before correctMonthName=",
//					matcher.group()));
		}
		// System.Xut.println("m=" + m);

		if (TableParser.Period12MonthsPattern.matcher(columnHdg).find()
				|| columnHdg.replaceAll("[ ]{2,}", " ").toLowerCase().contains("for the year")
				|| columnHdg.replaceAll("[ ]{2,}", " ").toLowerCase().contains("year ended")
				|| columnHdg.replaceAll("[ ]{2,}", " ").toLowerCase().contains("fiscal year")) {
			p++;
			period = 12;
			// System.Xut.println("1 getEnddatePeriod 12=" + period);
		}
		// System.Xut.println("p count="+p);

		if (TableParser.Period9MonthsPattern.matcher(columnHdg).find()) {
			p++;
			period = 9;

			// System.Xut.println("2 getEnddatePeriod=" + period);
		}
		// System.Xut.println("p count="+p);

		if (TableParser.Period6MonthsPattern.matcher(columnHdg).find()
				|| columnHdg.toLowerCase().contains("six-month")) {
			p++;
			period = 6;
			// System.Xut.println("3 getEnddatePeriod="+period);
		}

		// System.Xut.println("columnHdg="+columnHdg);
		if ((TableParser.Period3MonthsPattern.matcher(columnHdg).find() && !columnHdg.toLowerCase().contains("year"))
				|| columnHdg.toLowerCase().contains("three months")
				|| columnHdg.toLowerCase().contains("the three and ")) {

			p++;
			period = 3;
			// System.Xut.println("4 getEnddatePeriod 3=" + period);
		}
		// System.Xut.println("p count="+p+ " period="+period);

		// if above don't match to period = try==>
		if (period == 0) {
			if (columnHdg.toLowerCase().contains("year") && columnHdg.toLowerCase().contains("years")) {
				p++;
				period = 12;
				// System.Xut.println("0.12 period=" + period);
				// System.Xut.println("5 p=" + p);

			} else if (columnHdg.toLowerCase().contains("quarter")) {
				p++;
				period = 3;
				// System.Xut.println("0.qtr period=" + period);
				// System.Xut.println("6 p=" + p);
			}
		}

		List<String> ListEndDatePeriod = new ArrayList<String>();

		// System.Xut.println("p=" + p);

		// System.Xut.println("checking d pattern of columnHdg="+columnHdg);
		matcher = dPattern.matcher(columnHdg);
		while (matcher.find()) {
			day = matcher.group().replaceAll(" ", "").trim();
			// System.Xut.println("day=" + day);
		}

		if (day.length() > 0 && Integer.parseInt(day) < 10) {
			day = "0" + Integer.parseInt(day);
		}

		endDate = year + "-" + DateUtils.getMontIntegerAsString(month) + "-" + day;
		// System.Xut.println("2 endDate prelim=" + endDate);

		matcher = MonthDayYearPattern.matcher(columnHdg);

		if (matcher.find()) {
			String[] edtStr = matcher.group().replaceAll("/", "-").split("-");

			// System.Xut.println("matcher against columnHdg found="
			// + matcher.group() + " columnHdg=" + columnHdg);
			if (columnHdg.substring(0, 4).replaceAll("[12]{1}[09]{1}[0-9]{2}", "").length() == 0) {
				edtStr = columnHdg.replaceAll("/", "-").split("-");
				// System.Xut.println("cht columnHdg=" + columnHdg);

				String yr = edtStr[0];
				String mo = "", dy = "";
				if (columnHdg
						.replaceAll("([12]{1}[09]{1}[0-9]{2})-(0?[1-9]{1}|1[0-2]{1})-([0-2]{1}[1-9]{1}|3[0-1]{1})", "")
						.length() == 0) {
					mo = edtStr[1];
					if (mo.length() < 2)
						mo = "0" + mo;
					dy = edtStr[2];
					if (dy.length() < 2)
						dy = "0" + dy;
				}

				endDate = yr + "-" + mo + "-" + dy;
				// System.Xut.println("3 endDate==" + endDate);

			} else if (edtStr.length == 3
					&& columnHdg.substring(0, 4).replaceAll("[12]{1}[09]{1}[0-9]{2}", "").length() != 0) {
				// System.Xut.println("A edtStr=" + Arrays.toString(edtStr));
				String yr = edtStr[2];
				String mo = edtStr[0];
				if (mo.length() < 2)
					mo = "0" + mo;
				String dy = edtStr[1];
				if (dy.length() < 2)
					dy = "0" + dy;

				endDate = yr + "-" + mo + "-" + dy;
				// System.Xut.println("endDate="+endDate);
			}
		}

		Pattern alphabetPattern = Pattern.compile("[A-Za-z]{4,}|\\%");
		matcher = alphabetPattern.matcher(columnHdg);
		while (matcher.find()) {
			if (endDate.replaceAll("-", "").length() < 1 && endDate.length() > 3) {
				endDate = matcher.group().replaceAll(" ", "").trim();
				// System.Xut.println("oddCol endDate=" + endDate);
			}
		}

		if (endDate.replaceAll("-", "").length() < 1)
			endDate = "";

		if (period == 0) {
			Pattern tmpPatter = Pattern.compile("from|through");
			Pattern tmpPatter2 = Pattern.compile("(?i)(jan|feb|mar|apr|may |jun|jul|aug|sep|oct|nov|dec)");
			Pattern tmpPatter3 = Pattern.compile("[12]{1}[09]{1}[0-9]{2}");

			if (getAllIndexEndLocations(columnHdg, tmpPatter).size() > 1
					&& getAllIndexEndLocations(columnHdg, tmpPatter2).size() > 1
					&& getAllIndexEndLocations(columnHdg, tmpPatter3).size() > 0) {

				String per = NLP.getColumnTextDateDifference(columnHdg);
				period = Integer.parseInt(per);
			}
		}

		if (endDate.length() > 1 && endDate.substring(0, 1).equals("0")) {
			endDate = "20" + endDate;
			// System.Xut.println("col hdg 20");
		}
		if (endDate.length() > 1 && endDate.substring(0, 1).equals("9")) {
			endDate = "19" + endDate;
			// System.Xut.println("col hdg 19");
		}

//		NLP.pwNLP.append(NLP.println("a cht edt=", endDate + " cht per=" + period));
		// System.Xut.println("endDate="+endDate);
		ListEndDatePeriod.add(endDate);
		ListEndDatePeriod.add(period + "");
		ListEndDatePeriod.add(y + "");
		ListEndDatePeriod.add(m + "");
		ListEndDatePeriod.add(p + "");

		return ListEndDatePeriod;

	}

	public String getRevisedGroup(String revisedGrp) {

		if (revisedGrp.contains("/")) {

			String slashYear = revisedGrp.substring(revisedGrp.lastIndexOf("/")).replace("/", "");
			// System.Xut.println("slashYear=" + slashYear);

			if (slashYear.length() == 2) {
				// System.Xut
				// .println("tmpLine where slashYear - prior to adding slashYear="
				// + revisedGrp);
				if (slashYear.substring(0, 1).equals("9")) {
					revisedGrp = revisedGrp.substring(0, revisedGrp.length() - 2) + "19" + slashYear;
				}
				if (slashYear.substring(0, 1).equals("0")) {
					revisedGrp = revisedGrp.substring(0, revisedGrp.length() - 2) + "20" + slashYear;
				}
				// System.Xut.println("after slashYear added=" + revisedGrp);
			}
		}
		return revisedGrp;

	}

	public String getColHeadingType(String colHeading) {

		String type = "";

		if (null != getAllIndexStartLocations(colHeading, TableParserHTML.YearPatternOrMoDayYrPattern)
				&& getAllIndexStartLocations(colHeading, TableParserHTML.YearPatternOrMoDayYrPattern).size() > 0) {
			type = "|Y|";
		}

		if (null != getAllIndexStartLocations(colHeading, TableParser.MonthPattern)
				&& getAllIndexStartLocations(colHeading, TableParser.MonthPattern).size() > 0) {
			type = type + "|M|";
		}

		if (null != getAllIndexStartLocations(colHeading, TableParserHTML.EndedHeadingPattern)
				&& getAllIndexStartLocations(colHeading, TableParserHTML.EndedHeadingPattern).size() > 0) {
			type = type + "|E|";
		}

		if (null != getAllIndexStartLocations(colHeading, TableParserHTML.EndedOnlyPattern)
				&& getAllIndexStartLocations(colHeading, TableParserHTML.EndedOnlyPattern).size() > 0
				&& !type.contains("E")) {
			type = type + "|E|";
		}

		type = type.replaceAll("[\\|]{2,}", "\\|");
		return type;

	}

	public String getTableSentencePatterns(String tableSentence, String lineNo) {
		TableParser tp = new TableParser();

		if (tableSentence == null)
			return "";
		NLP nlp = new NLP();

		tableSentence = tableSentence.replaceAll("(?i)(three|six|nine|twelve) (three|six|nine|twelve)", "$1 and $2");

		Map<Integer, String> map = new TreeMap<Integer, String>();
		String tsPatternLong = "";

		// map.putAll(getStartLocsAndMatchedGroups(tableSentence,
		// TableParser.EndedHeadingPattern, "P:"));//changed to below 3/1/2016
		// System.Xut.println("ts at pmy map=" + tableSentence);
		Pattern patternToFromThrough = Pattern.compile("(?i) to |through");
		if (nlp.getAllIndexEndLocations(tableSentence, TableParser.OddBallPeriodPattern).size() < 1) {
			// System.Xut.println("tableSentence=" + tableSentence
			// + " no oddball patter");
			map.putAll(getStartLocsAndMatchedGroups(tableSentence.toUpperCase(), tp.EndedHeadingTablesentencePattern,
					"P:"));
		}
		map.putAll(getStartLocsAndMatchedGroups(tableSentence.toUpperCase(), TableParser.MonthPatternSimple, "M:"));
		map.putAll(getStartLocsAndMatchedGroups(tableSentence.toUpperCase(), TableParser.YearOrMoDayYrPattern, "Y:"));
		map.putAll(getStartLocsAndMatchedGroups(tableSentence.toUpperCase(), patternToFromThrough, "t:"));

		String tsAbr = "";
		if (lineNo.length() > 0) {
			lineNo = lineNo + "L";
		}

		int tsPcnt = 0, tsMcnt = 0, tsYcnt = 0, tsToCnt = 0, key;
		for (Map.Entry<Integer, String> entry : map.entrySet()) {
			key = entry.getKey();
			// System.Xut
			// .println("key|" + key + "| val|" + entry.getValue() + "|");

			tsAbr = entry.getValue().substring(0, 2);
			// System.Xut.println("tsAbr=" + tsAbr+" val="+entry.getValue());
			if (tsAbr.equals("P:")) {
				tsPcnt++;
				// tsPatternLong = "|" + tsPatternLong +"|"+key+ "|" + lineNo +
				// tsPcnt
				// + tsAbr + entry.getValue().substring(2).trim() + "|";
				// not sure if I want to record key which is eIdx in each case.
				// Possible use to better under order. Would repeat in each of 3
				// below.
				tsPatternLong = "|" + tsPatternLong + "|" + lineNo + tsPcnt + tsAbr
						+ entry.getValue().substring(2).trim() + "|";

			}

			if (tsAbr.equals("M:")) {
				tsMcnt++;
				tsPatternLong = "|" + tsPatternLong + "|" + lineNo + tsMcnt + tsAbr
						+ entry.getValue().substring(2).trim() + "|";
			}

			if (tsAbr.equals("Y:")) {
				tsYcnt++;
				tsPatternLong = "|" + tsPatternLong + "|" + lineNo + tsYcnt + tsAbr
						+ entry.getValue().substring(2).trim() + "|";
			}

			if (tsAbr.equals("t:")) {
				tsToCnt++;
				tsPatternLong = "|" + tsPatternLong + "|" + lineNo + tsToCnt + tsAbr
						+ entry.getValue().substring(2).trim() + "|";
			}
		}
		// System.Xut.println("getTableSentencePatterns -- tsPatternLong"
		// + tsPatternLong);

		return tsPatternLong.replaceAll("[\\|]{2,}", "\\|");

	}

	public static String getMonthYearPeriodFromTSpatternFormatted(String tsPattern) {

		NLP nlp = new NLP();

//		NLP.pwNLP.append(
//				NLP.println("getMonthYearPeriodFromTSpatternFormatted - tsPatttern unformatted=", "\r" + tsPattern));

		// uses natural order from ts pattern which is natural order of
		// tablesentence and formats year, month/day and period so that each
		// always has a length of 4 and is in yyyy,mmdd and p.ppp (3.00) or
		// pp.pp (12.00) format. By using set format - later I can pair with
		// short ts pattern of for example YMYM by knowing that first YM is c1
		// and will always be the first 8 characters with 2nd 4 being M1. And so
		// on. Can also pair far more complex arrangements.

		TableParser tp = new TableParser();

		String[] tsSplit = tsPattern.split("\\|");
		String m = null, y, d = null, f, moDy, pStr;
		int pInt = 0;
		tsPattern = "|";
		// System.Xut.println("getMonthYearPeriodFromTSpatternFormatted.
		// tsPattern="+tsPattern);
		for (int i = 0; i < tsSplit.length; i++) {
			f = tsSplit[i];
			if (f.length() < 1)
				continue;
			if (f.substring(1, 2).equals("M")) {
				// System.Xut.println("1 f===" + f);
				f = f.substring(2);
				// System.Xut.println("2 f===" + f);
				if (f.indexOf(" ") > 0)
					m = f.substring(1, f.indexOf(" "));
				m = f.substring(1);
				m = tp.getCorrectMonthName(m);
				// System.Xut.println("m prior to conversion=" + m);
				m = DateUtils.getMontIntegerAsString(m);
				if (m.length() == 1) {
					m = "0" + m;
				}
				// System.Xut.println("m==" + m);

				if (f.indexOf(" ") > 0)
					d = f.substring(f.indexOf(" ")).trim();
				d = f.substring(1).trim();

				d = d.replaceAll("[^\\d]", "");

				// System.Xut.println("1 d==" + d);
				if (d.length() == 1)
					d = "0" + d;
				if (d.length() == 0)
					d = "00";

				moDy = m + d;
				// System.Xut.println("2 d==" + d);
				// System.Xut.println(" moDy==" + moDy);

				tsPattern = tsPattern + moDy + "|";
			}

			if (f.substring(1, 2).equals("Y")) {
				f = f.substring(2);
				y = f.substring(1);
				y = nlp.getYearFromText(y);
				if (y.length() != 4)
					y = "0000";

				tsPattern = tsPattern + y + "|";
			}
			if (f.substring(1, 2).equals("P")) {
				f = f.substring(2);
				pStr = f.substring(1);
				pInt = nlp.getPeriodFromText(pStr);
				if (pInt == 0)
					pStr = "0000";
				if (pInt == 12)
					pStr = "12.0";
				if (pInt == 9)
					pStr = "9.00";
				if (pInt == 6)
					pStr = "6.00";
				if (pInt == 3)
					pStr = "3.00";
				if (pInt == 7)
					pStr = "7.00";

				tsPattern = tsPattern + pStr + "|";
			}
		}
		// System.Xut
		// .println("getMonthYearPeriodFromTSpatternFormatted tsPattern formatted="
		// + tsPattern);
		return tsPattern;
	}

	public List<String[]> getColumnHeadingFromTableSentenceNew(String tableSentence)
			throws SQLException, IOException, ParseException {

		// System.Xut.println("tableSentence at getColumnHeadingFromTableSentenceNew=" +
		// tableSentence);
		specialTableSentencePatternOneEndDateManyPeriods = false;

		// if # of matches of E, M or Y respectively equal number of CHs they
		// can be matched respectively. If only 1 E,M or Y respectively they can
		// be matched to all CHs. If # of E,M or Y don't match # of CHs
		// respectivey then only the 1st instance can be matched against 1st CH

		// return list of String[] where each list is equal to string ary of
		// edt="yyyy-mm-dd", per=3,6,9 or 12 (1 col). # of String[] is equal to
		// # of cols. I will add to each col the data I know conclusively is
		// associated with a certain CH#. Then I need to redesign pair method -
		// Create string that is instead [year=0,mo=1,ended=2,per=3] and when I
		// try to match to col hdgs - if year, mo, ended or TS are missing I
		// insert it from ts.

		Map<Integer, String> map = new TreeMap<Integer, String>();

		// each ary is the col
		List<String[]> listEdtPeriod = new ArrayList<String[]>();
		List<String> listEndDatePeriod = new ArrayList<String>();

		// getMatchedGroupsWithIndexLocations also uses TreeMap. And is
		// identical to the above map. And b/c it is a TreeMap the natural order
		// will be based on key value which is idx location of match.

		// step 1: get all e,m or y counts. If count=No of Cols or 1 insert int
		// list against all cols. If neither - only accept 1st instance as match
		// against 1st col

		TableParser tp = new TableParser();
		// System.Xut.println("get E map");
		map.putAll(getStartLocsAndMatchedGroups(tableSentence, tp.EndedHeadingTablesentencePattern, "E:"));
		// System.Xut.println("get M map");
		map.putAll(getStartLocsAndMatchedGroups(tableSentence, monthSimplePattern, "M:"));
		map.putAll(getStartLocsAndMatchedGroups(tableSentence, yearSimplePattern, "Y:"));

		NLP.printMapIntStr("CH map in end,yr&mo order", map);

		// Map<Integer, String> mapCHSeq = new TreeMap<Integer, String>();

		@SuppressWarnings("unused")
		int e = 0, y = 0, m = 0, p = 0;

		e = getAllIndexStartLocations(tableSentence, tp.EndedHeadingTablesentencePattern).size();
		// System.Xut.println("e=" + e);
		m = getAllIndexStartLocations(tableSentence, monthSimplePattern).size();
		// System.Xut.println("m=" + m);
		y = getAllIndexStartLocations(tableSentence, yearSimplePattern).size();
		// System.Xut.println("y=" + y);
		String year, month, ended = "";
		// System.Xut.println("from tableSentence e=" + e + " m=" + m + " y=" +
		// y);

		if (m == 1 && y == 1 && e >= 1) {
			// System.Xut.println("if 1 mo and 1 yr and more than 2 periods - ts pattern is
			// year-mo p1, year-mo p2 etc");

			year = getAllMatchedGroups(tableSentence, yearSimplePattern).get(0);
			month = getAllMatchedGroups(tableSentence, monthSimplePattern).get(0);
			for (int eI = 0; eI < e; eI++) {
				ended = getAllMatchedGroups(tableSentence, tp.EndedHeadingTablesentencePattern).get(eI);
				listEndDatePeriod = getEnddatePeriod(ended + " " + year + " " + month);
				// System.Xut.println("period from tableSentence==" + listEndDatePeriod.get(0));
				String[] ary = { listEndDatePeriod.get(1), listEndDatePeriod.get(0) };
				listEdtPeriod.add(ary);
			}
			specialTableSentencePatternOneEndDateManyPeriods = true;
			return listEdtPeriod;
		}

		if (m == 1 && y == 2 && e == 2) {
			// System.Xut.println("I can find unique CHs but it won't necessarily equal # of
			// CHs.");

			year = getAllMatchedGroups(tableSentence, yearSimplePattern).get(0);
			month = getAllMatchedGroups(tableSentence, monthSimplePattern).get(0);
			for (int eI = 0; eI < e; eI++) {
				ended = getAllMatchedGroups(tableSentence, tp.EndedHeadingTablesentencePattern).get(eI);
				listEndDatePeriod = getEnddatePeriod(ended + " " + year + " " + month);
				// System.Xut.println("period from tableSentence==" + listEndDatePeriod.get(0));
				String[] ary = { listEndDatePeriod.get(1), listEndDatePeriod.get(0) };
				listEdtPeriod.add(ary);
			}
			specialTableSentencePatternOneEndDateManyPeriods = true;
			return listEdtPeriod;
		}

		// TODO: ck to make sure chgs here don't corrupt other parser -- such as
		// generic and/or html. For html and generic we can have the same
		// scenario theoretically. So see if this method is used. By them.

		if (m == 0 && y == 0 && e > 0) {
			// System.Xut.println("if no enddate and only 1 or more periods");

			for (int eI = 0; eI < e; eI++) {
				ended = getAllMatchedGroups(tableSentence, tp.EndedHeadingTablesentencePattern).get(eI);
				listEndDatePeriod = getEnddatePeriod(ended);
				// System.Xut.println("period from tableSentence=="
				// + listEndDatePeriod.get(1));
				String[] ary = { listEndDatePeriod.get(1), listEndDatePeriod.get(0) };
				listEdtPeriod.add(ary);
			}
			specialTableSentencePatternOneEndDateManyPeriods = true;
			return listEdtPeriod;
		}

		// System.Xut.println("map.size="+map.size());
		String lastEnded = null, lastMonth = null;
		e = 0;
		y = 0;
		m = 0;
		p = 0;

		if (map.size() == 1) {
			// System.Xut
			// .println("map size=1 and tablesentence==" + tableSentence);
			String str = new ArrayList<String>(map.values()).get(0);
			// System.Xut.println("str=" + str);
			String value = str.substring(2);
			String ch = value;

			listEndDatePeriod = getEnddatePeriod(" " + ch);

			// ary = period, endDate
			String[] ary = { listEndDatePeriod.get(1), listEndDatePeriod.get(0) };
			listEdtPeriod.add(ary);

			// System.Xut.println("from getColumnHeadingFromTableSentence ary"
			// + Arrays.toString(ary));

			return listEdtPeriod;
		}

		for (Map.Entry<Integer, String> entry : map.entrySet()) {
			// System.Xut.println("map size>1. tablesentence===" +
			// tableSentence);

			String str = entry.getValue();
			// System.Xut.println("str=" + str);
			String value = str.substring(2);
			// System.Xut.println("value=" + value);
			String prefix = str.substring(0, 2);
			// System.Xut.println("prefix=" + prefix);
			// str has both prefix and pattern matched text.
			// if lastMo,lastYear and lastEnded are not blank - then create
			// enddate/period
			// if only lastMonth and ended in tablesentence - then create
			// period? and same for year?

			if (prefix.equals("E:")) {
				e++;
				lastEnded = value;
				// System.Xut.println("E lastEnded=" + lastEnded);
			} else if (prefix.equals("Y:")) {
				y++;
				// what if no y value? as in "years ended april 30"?
				String ch = "";
				if (StringUtils.isNotBlank(lastEnded))
					ch = lastEnded;
				if (StringUtils.isNotBlank(lastMonth))
					ch += " " + lastMonth;
				// convert July 30, 2000 to 2000-07-30 and Years to p=12

				// System.Xut
				// .println("getting period and enddate for ch + value=="
				// + ch + " " + value);

				listEndDatePeriod = getEnddatePeriod(" " + ch + " " + value);
				// System.Xut.println("listEndDatePeriod=" + listEndDatePeriod);

				// ary = period, endDate
				String[] ary = { listEndDatePeriod.get(1), listEndDatePeriod.get(0) };
				listEdtPeriod.add(ary);
				/*
				 * sb.append("insert ignore into tp_CHsent values('" + acc + "'," + tableCount +
				 * "," + y + "," + ListEndDatePeriod.get(1) + ",'" + ListEndDatePeriod.get(0) +
				 * "','" + ch + " " + value + "'," + ListEndDatePeriod.get(2) + "," +
				 * ListEndDatePeriod.get(3) + "," + ListEndDatePeriod.get(4) + ");\r");
				 */

			} else if (prefix.equals("M:")) {
				m++;

				// if map size=2 and lastEnded not blank then only month
				// and ended available (Columns headings are years and
				// tablensentence has mo / ended).

				if (map.size() == 2 && StringUtils.isNotBlank(lastEnded)) {
					String ch = lastEnded + " " + value;

					List<String> ListEndDatePeriod = new ArrayList<String>(getEnddatePeriod(" " + ch));

					// ary = period, endDate
					String[] ary = { ListEndDatePeriod.get(1), ListEndDatePeriod.get(0) };
					listEdtPeriod.add(ary);
				}

				lastMonth = value;
			}
		}

		// for (int i = 0; i < listEdtPeriod.size(); i++) {
		// System.Xut.println("ts ListEdtPeriod edt=="
		// + listEdtPeriod.get(i)[1] + " period=="
		// + listEdtPeriod.get(i)[0]);
		// }
		return listEdtPeriod;
	}

	public boolean isItTableSentenceInLine(String line) {
		// A table sentence is a line that has no tabs and no instances of 3
		// consecutive spaces [and whose length is > 50?].

		if (line.length() > 20) {
			Matcher matcher = isItTableSentencePattern.matcher(line);
			if (!matcher.find())
				return true;
		}
		return false;
	}

	public String isItTableSentenceInParagraph(String text) {
		// A table sentence is a line that has no tabs and no instances of 3
		// consecutive spaces [and whose length is > 50?].
		// System.Xut.println("method isItTableSentence line parsed=" +
		// text+"|end tableSentence");

		TableParser tp = new TableParser();

		// System.Xut.println("isItTableSentence=" + line);

		// moving backwards from start of startgroup with columnheadings - find
		// two instance col hdgs match and where this pattern is not found
		// isItTableSentencePattern

		String[] lines = text.split("\r\n");
		String line, tableSentence = null;
		for (int i = 0; i < lines.length; i++) {
			line = lines[i];
			getAllIndexStartLocations(line, TableParser.ColumnHeadingPattern);

			// System.Xut.println("line=" + line);
			if (null == line || line.length() < 20)
				continue;
			Matcher matcher = isItTableSentencePattern.matcher(line);
			if (matcher.find())
				tableSentence = line;
			return tableSentence;
		}
		return "";
	}

	public boolean isNumeric(String str) {
		NLP nlp = new NLP();

		if (nlp.getAllIndexEndLocations(str, Pattern.compile("\\d\\(")).size() > 0)
			return false;

		try {

			str = str.replaceAll("[,\\(\\)\\$]", "").trim();
			// System.Xut.println("str="+str);
			Double.parseDouble(str.trim());
			// System.Xut.println("Double.parseDouble(str)="
			// + Integer.valueOf(str.trim().replaceAll(",", "")));
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	public List<Integer> getSentenceStartLocations(String txt) {
		// en-sent.bin is like a dictionary so it can determine eng sent
		InputStream modelIn = NLP.class.getClassLoader().getResourceAsStream("en-sent.bin");
		List<Integer> locs = new ArrayList<Integer>();
		try {
			SentenceModel model = new SentenceModel(modelIn);
			SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
			Span[] sents = sentenceDetector.sentPosDetect(txt);
			for (Span s : sents) {
				locs.add(s.getStart());
				// System.Xut.println(
				// "sentStart==" + s.getStart() + "sentEndt==" + s.getEnd() + "::" +
				// s.getCoveredText(txt));
			}
		} catch (IOException e) {
			e.printStackTrace(System.out);
		} finally {
			if (modelIn != null) {
				try {
					modelIn.close();
				} catch (IOException e) {
					e.printStackTrace(System.out);
				}
			}
		}
		return locs;
	}

	public List<Integer> getAllIndexStartLocations(String text, Pattern pattern) {

		List<Integer> idxs = new ArrayList<Integer>();
		if (null == text || text.length() < 1 || pattern == null || pattern.toString().length() < 1)
			return idxs;

		Matcher matcher = pattern.matcher(text);
		int idx = 0;
		while (matcher.find()) {
			idx = matcher.start();
			// System.Xut.println("idxs="+idx+"matcher.group()="+matcher.group());
			idxs.add(idx);
		}
		return idxs;
	}

	public List<Integer> getAllIndexEndLocations(String text, Pattern pattern) {
		if (null == text)
			return null;

		int idx = 0;
		List<Integer> idxs = new ArrayList<Integer>();

		Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			idx = matcher.end();
			// System.Xut.println("idxs="+idx+"matcher.group()="+matcher.group());
			idxs.add(idx);
		}
		return idxs;
	}

	public List<Integer[]> getAllStartAndEndIdxLocations(String text, Pattern pattern) {
		if (null == text)
			return null;

		int sIdx = 0, eIdx = 0;
		List<Integer[]> idxs = new ArrayList<Integer[]>();

		Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			sIdx = matcher.start();
			eIdx = matcher.end();
			Integer[] intAry = { sIdx, eIdx };
			// System.Xut.println("idxs="+idx+"matcher.group()="+matcher.group());
			idxs.add(intAry);
		}
		return idxs;
	}

	public List<String[]> getAllMatchedGroupsAndStartAndEndIdxLocations(String text, Pattern pattern) {

		if (null == text)
			return null;

		int sIdx = 0, eIdx = 0;
		String match = "";
		List<String[]> idxs = new ArrayList<String[]>();
		Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			match = matcher.group();
			sIdx = matcher.start();
			eIdx = matcher.end();
			String[] ary = { match, sIdx + "", eIdx + "" };
			idxs.add(ary);
		}
		return idxs;
	}

	public String confirmItIsColHdgAboveRowname(Map<Integer, List<String[]>> mapCHs, String textAbovRowname)
			throws IOException {
		String textAboveRownameConfirmed = "";
		NLP nlp = new NLP();

		// confirms that CH found above rowname don't overlap with CH above
		// data cols. A year value should never be above rowname.

		// System.Xut
		// .println("AAA. printing mapAllCHs at start of getColHdgAboveRownameHtml");
		// nlp.printMapIntListOfStringAry(mapCHs);

		int cntEAbvRn = nlp.getAllMatchedGroups(textAbovRowname, TableParser.EndedHeadingPattern).size();
		int cntMAbvRn = nlp.getAllMatchedGroups(textAbovRowname, TableParser.MonthPatternSimple).size();
		int cntYabvRn = nlp.getAllMatchedGroups(textAbovRowname, TableParser.YearOrMoDayYrPattern).size();

		int key, cntEAbvDataCols = 0, cntMAbvDataCols = 0, cntYAbvDataCols = 0;
		for (Map.Entry<Integer, List<String[]>> entry : mapCHs.entrySet()) {
			key = entry.getKey();
			for (int i = 0; i < entry.getValue().size(); i++) {
				if (nlp.getAllIndexEndLocations(entry.getValue().get(i)[2], TableParser.EndedHeadingPattern)
						.size() > 0) {
					cntEAbvDataCols++;
				}
				if (nlp.getAllIndexEndLocations(entry.getValue().get(i)[2], TableParser.MonthPatternSimple)
						.size() > 0) {
					cntMAbvDataCols++;
				}
				if (nlp.getAllIndexEndLocations(entry.getValue().get(i)[2],
						Pattern.compile("199[0-9]{1}|20[01]{1}[0-9]{1}")).size() > 0) {
					cntYAbvDataCols++;
				}

//				NLP.pwNLP.append(NLP.println("key=",
//						key + "| list string array=" + Arrays.toString(entry.getValue().get(i)) + "cntMm="
//								+ cntMAbvDataCols + " cntEm=" + cntEAbvDataCols + " cntYm=" + cntYAbvDataCols));
			}
		}

		// confirms that CH found above rowname don't overlap with CH above
		// data cols. A year value should never be above rowname.
		if ((cntEAbvDataCols == 0 && cntMAbvDataCols == 0 && cntYabvRn == 0 && (cntEAbvRn == 1 || cntMAbvRn == 1))
				|| (cntEAbvDataCols > 0 && cntEAbvRn == 0 && cntYabvRn == 0 && cntMAbvRn > 0)
				|| (cntMAbvDataCols > 0 && cntMAbvRn == 0 && cntYabvRn == 0 && cntEAbvRn > 0)) {

			textAboveRownameConfirmed = textAbovRowname;
		}

		return textAboveRownameConfirmed;

	}

	public static void printMapIntListOfStringAry(String mapStr, Map<Integer, List<String[]>> map) {

		int key;
		for (Map.Entry<Integer, List<String[]>> entry : map.entrySet()) {
			key = entry.getKey();
			for (int i = 0; i < entry.getValue().size(); i++) {
				System.out.println(
						mapStr + " key=" + key + "| list of string array=" + Arrays.toString(entry.getValue().get(i)));
			}
		}

	}

	public static void printMapDoubleListOfStringAry(String mapStr, Map<Double, List<String[]>> map) {

		Double key;
		for (Map.Entry<Double, List<String[]>> entry : map.entrySet()) {
			key = entry.getKey();
			for (int i = 0; i < entry.getValue().size(); i++) {
				System.out.println(
						mapStr + " key=" + key + "| list of string array=" + Arrays.toString(entry.getValue().get(i)));
			}
		}

	}

	public static void printMapDoubleListOfString(String mapStr, Map<Double, List<String>> map) {

		Double key;
		for (Map.Entry<Double, List<String>> entry : map.entrySet()) {
			key = entry.getKey();
			System.out.println(mapStr + " key==" + key + " list.size=" + entry.getValue().size());
			for (int i = 0; i < entry.getValue().size(); i++) {
				System.out.println(mapStr + " key=" + key + "| list of string array=" + entry.getValue().get(i));
			}
		}

	}

	public static void printMapStringListOfStringAry(String mapStr, Map<String, List<String[]>> map) {

		String key;
		int cnt = 0, totalItems = 0;
		for (Map.Entry<String, List<String[]>> entry : map.entrySet()) {
			key = entry.getKey();
			cnt++;
			for (int i = 0; i < entry.getValue().size(); i++) {
				System.out.println(mapStr + "totalItems = " + (totalItems++) + " list#=" + cnt + "|key=" + key
						+ "|ary cnt=" + i + "| ary=" + Arrays.toString(entry.getValue().get(i)));
			}
		}

	}

	public static void printMapStrListOfString(String mapStr, Map<String, List<String>> map) {

		String key;
		for (Map.Entry<String, List<String>> entry : map.entrySet()) {
			key = entry.getKey();
			for (int i = 0; i < entry.getValue().size(); i++) {
				System.out.println(mapStr + " key=" + key + " i==" + i + " value=" + entry.getValue().get(i));
			}
		}
	}

	public static void printMapStringString(String mapStr, Map<String, String> map) {

		String key; int cnt=0;
		for (Map.Entry<String, String> entry : map.entrySet()) {
			key = entry.getKey(); cnt++;
			System.out.println(mapStr +"cnt="+(cnt)+ " key=" + key + " value=" + entry.getValue());
		}
	}

	public static void printMapIntListOfString(String mapStr, Map<Integer, List<String>> map) {

		int key;
		for (Map.Entry<Integer, List<String>> entry : map.entrySet()) {
			key = entry.getKey();
			for (int i = 0; i < entry.getValue().size(); i++) {
				System.out.println(mapStr + " key=" + key + "| list string=" + entry.getValue().get(i));
			}
		}

	}

	public static void printMapStrInt(String mapStr, Map<String, Integer> map) {

		for (Map.Entry<String, Integer> entry : map.entrySet()) {
			System.out.println(mapStr + "| key|" + entry.getKey() + " val==|" + entry.getValue() + "|");
		}

	}

	public static void printMapStrStrAry(String mapStr, Map<String, String[]> map) {

		for (Map.Entry<String, String[]> entry : map.entrySet()) {
			System.out.println(mapStr + " key==" + entry.getKey() + " val==" + Arrays.toString(entry.getValue()) + "|");
		}

	}

	public static void printMapStrStr(String mapStr, Map<String, String> map) {
		int cnt =0;
		for (Map.Entry<String, String> entry : map.entrySet()) {
			cnt++;
			System.out.println(mapStr + " cnt="+cnt+" key|" + entry.getKey() + "| val|" + entry.getValue() + "|");
		}
	}

	public static void printMapIntStr(String mapStr, Map<Integer, String> map) {

		if (map == null)
			return;

		for (Map.Entry<Integer, String> entry : map.entrySet()) {
			System.out.println(mapStr + " key=" + entry.getKey() + "| val|" + entry.getValue());
		}

	}

	public static void printMapIntInt(String mapStr, Map<Integer, Integer> map) {

		for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
			System.out.println(mapStr + " key=" + entry.getKey() + " v=" + entry.getValue());
		}
	}

	public static void printMapDoubleString(String mapStr, Map<Double, String> map) {

		for (Map.Entry<Double, String> entry : map.entrySet()) {
			System.out.println(mapStr + "=" + " key=" + entry.getKey() + " val=" + entry.getValue() + "|");
		}
	}

	public static void printMapDblStrAry(String mapStr, Map<Double, String[]> map) {

		String mapStrOrg = mapStr;
		int cnt = 0;
		for (Map.Entry<Double, String[]> entry : map.entrySet()) {
			mapStr = mapStrOrg + (cnt++);
			System.out.println(
					mapStr + "=" + " key=" + entry.getKey() + " val=" + Arrays.toString(entry.getValue()) + "|");
		}

	}

	public static void printMapIntIntAry(String mapStr, Map<Integer, Integer[]> map) {

		for (Map.Entry<Integer, Integer[]> entry : map.entrySet()) {
//			NLP.pwNLP.append(
//					NLP.println(mapStr + " key|", entry.getKey() + "| val|" + Arrays.toString(entry.getValue()) + "|"));

		}

	}

	public static void printMapStrListOfIntAry(Map<String, List<Integer[]>> map) {

		for (Map.Entry<String, List<Integer[]>> entry : map.entrySet()) {
			for (int i = 0; i < entry.getValue().size(); i++) {
				System.out.println(
						" key=" + entry.getKey() + "| list int ary=" + Arrays.toString(entry.getValue().get(i)));
			}
		}

	}

	public static void printMapIntStringAry(String mapStr, Map<Integer, String[]> map) {

		int cnt = 0;
		for (Map.Entry<Integer, String[]> entry : map.entrySet()) {
			System.out.println(mapStr + "   cnt=" + cnt + " map key|" + entry.getKey() + "| val|"
					+ Arrays.toString(entry.getValue()));
			cnt++;
		}

	}

	public static void printObjectArrays(String objStr, Object[][] arrayOfArray) {

		for (Object[] ary : arrayOfArray)
			System.out.println(Arrays.toString(ary));
//			NLP.pwNLP.append(NLP.println(objStr, Arrays.toString(ary)));

	}

	public static void printListOfString(String listStr, List<String> list) {

		if (list != null) {

			for (int i = 0; i < list.size(); i++) {
				System.out.println(listStr + " i=" + i + " " + list.get(i) );
			}
		}
	}

	public static void printListOfInteger(String listStr, List<Integer> list) {

		for (int i = 0; i < list.size(); i++) {
			System.out.println("listInt=" + list.get(i));
		}

	}

	public static void printListOfStringArray(String listStr, List<String[]> list) {
		if (list != null)
			for (int i = 0; i < list.size(); i++) {
				String[] ary = list.get(i);
				System.out.println(listStr + " i=" + i + " " + Arrays.toString(ary));
			}
	}

	public static void printListOfStringArrayInReverse(String listStr, List<String[]> list) {

		for (int i = (list.size() - 1); i >= 0; i--) {
			String[] ary = list.get(i);
//			NLP.pwNLP.append(NLP.println(listStr, Arrays.toString(ary)));
		}

	}

	public static void printListOfIntegerArray(String listStr, List<Integer[]> list) {

		for (int i = 0; i < list.size(); i++) {
			System.out.println(listStr + " i=" + i + " ary=" + Arrays.toString(list.get(i)));
		}

	}

	public static void printListOfDoubleArray(String listStr, List<Double[]> list) {

		for (int i = 0; i < list.size(); i++) {
			Double[] ary = list.get(i);
//			NLP.pwNLP.append(NLP.println(listStr, Arrays.toString(ary)));
		}

	}

	public static void printArray(String aryStr, String[] ary) {

		for (int i = 0; i < ary.length; i++) {
//			NLP.pwNLP.append(NLP.println(aryStr, ary[i]));
		}

	}

	public static String getTicker(String text) throws IOException {

		String ticker = "";

		Pattern StockTickerPattern = Pattern
				.compile("(?ism)(?<=\\((NASD[ AQGSM]{0,6}|OTC[B]{0,2}|NYSE|AMEX|NQB|PINK SHEETS|TSX"
						+ "|LSE|ARCA|OTC BULLETIN BOARD): ?)[A-Z]{1,5}(?=\\))");

		NLP nlp = new NLP();

		List<String> list = nlp.getAllMatchedGroups(text, StockTickerPattern);
//		System.out.println("list.size="+list.size());
		if (list.size() > 0) {
			ticker = list.get(0);
//			System.out.println("ticker=="+ticker);
		} else
			ticker = "";

		return ticker;

	}

	public List<String> getAllMatchedGroups(String text, Pattern pattern) throws IOException {

		// System.Xut.println("getAllMatchedGroups text="+text.substring(0,Math.min(100,
		// text.length())));
		// text = Utils.readTextFromFile(""+getTableNamePrefixYearQtr(fileDate)
		// + "regexv2.txt");
		List<String> idxs = new ArrayList<String>();

		if (text == null) {
			return idxs;
		}

		Matcher matcher = pattern.matcher(text);

		// int idx;
		String match = null;
		while (matcher.find()) {
			// idx = matcher.start();
			// if (idx >= 0)
			match = matcher.group().replaceAll("\t", " ").replaceAll("  ", " ").trim();
			// System.Xut.println("@getAllMatchedGroups matcher.group="+match);
			idxs.add(match);
			// NOTE: the replaceAll was added in order to address error that
			// occurs on rare occassion when a tab is present in a pattern that
			// was matched.
		}
		// System.Xut.println("idxs::" + idxs);
		// System.Xut.println("idx="+idxs);
		return idxs;
		// For the years ended December 31, 1999 and 2000
		// returns 1 of month, 1 of ended and 2 of year patterns.
	}

	public static String replaceWithWhiteSpace(String text) {

		// This method will cycle through 2 or more (up to 15) ==, -- or __ and
		// replace with corresponding number of white spaces.

		StringBuffer sb = new StringBuffer();

		String[] replaceText = { "-", "_", "=" };
		String ln = "";
		String[] tmpSplit = text.split("[\r\n]");
		// System.Xut.println("table text before reconstitute =="+text);
		sb.delete(0, sb.length());
		for (int a = 0; a < tmpSplit.length; a++) {
			ln = tmpSplit[a];
			for (int z = 0; z < replaceText.length; z++) {
				for (int y = 15; y > 1; y--) {
					for (int x = 0; x < y; x++) {
						ln = ln + " ";
					}
				}
			}
			sb.append("\r" + ln);
		}
		return text;
	}

	public List<String[]> getAllStartIdxLocsAndMatchedGroups(String text, Pattern pattern) throws IOException {

		Matcher matcher = pattern.matcher(text);

		// int idx;
		List<String[]> idxsGrps = new ArrayList<String[]>();
		while (matcher.find()) {
			// idx = matcher.start();
			// if (idx >= 0)
//			 System.out.println("matcher.start()="+matcher.start()+"matcher.group="+matcher.group()+"|");
			String[] ary = { matcher.start() + "", matcher.group() };
			idxsGrps.add(ary);
		}
		return idxsGrps;
	}

	public Map<Integer, String> getAllIdxLocsAndMatchedGroupsAndAddToList(String text, Pattern pattern,
			Map<Integer, String> mapIntegerString) throws IOException {

		// System.Xut.println("getAllMatchedGroups text="+text.substring(0,Math.min(100,
		// text.length())));
		// text = Utils.readTextFromFile(""+getTableNamePrefixYearQtr(fileDate)
		// + "regexv2.txt");

		Matcher matcher = pattern.matcher(text);

		// int idx;
		while (matcher.find()) {
			// idx = matcher.start();
			// if (idx >= 0)
			// System.Xut.println("matcher.group="+matcher.group());
			mapIntegerString.put(matcher.start(), matcher.group());
		}
		return mapIntegerString;
	}

	public List<String[]> getAllMidPointLocsAndMatchedGroupAfterRownameEndIdx(String text, Pattern pattern, int addBack)
			throws IOException {

		Matcher matcher = pattern.matcher(text.substring(addBack));
		List<String[]> idxsGrps = new ArrayList<String[]>();
		double mp;
		while (matcher.find()) {
			// idx = matcher.start();
			// if (idx >= 0)
			// System.Xut.println("matcher.group=" + matcher.group());
			mp = matcher.end() - matcher.group().trim().length() / 2 + addBack;
			String[] ary = { mp + "", matcher.group().trim() };
			idxsGrps.add(ary);
		}
		return idxsGrps;
	}

	public List<String[]> getAllEndIdxAndMatchedGroupLocs(String text, Pattern pattern) throws IOException {

		// System.Xut.println("getAllMatchedGroups text="+text.substring(0,Math.min(100,
		// text.length())));
		// text = Utils.readTextFromFile(""+getTableNamePrefixYearQtr(fileDate)
		// + "regexv2.txt");

		Matcher matcher = pattern.matcher(text);

		List<String[]> idxsGrps = new ArrayList<String[]>();

		while (matcher.find() && matcher.group().length() > 0) {
			// idx = matcher.start();
			// if (idx >= 0)
			// System.Xut.println("matcher.group="+matcher.group());
			String[] ary = { matcher.end() + "", matcher.group() };
			idxsGrps.add(ary);
		}
		return idxsGrps;
	}

	public List<String[]> getAllMatchedGroupsAndEndIdxLocs(String text, Pattern pattern) throws IOException {

		// System.Xut.println("getAllMatchedGroups text="+text.substring(0,Math.min(100,
		// text.length())));
		// text = Utils.readTextFromFile(""+getTableNamePrefixYearQtr(fileDate)
		// + "regexv2.txt");

		Matcher matcher = pattern.matcher(text);

		List<String[]> idxsGrps = new ArrayList<String[]>();

		while (matcher.find() && matcher.group().length() > 0) {
			// idx = matcher.start();
			// if (idx >= 0)
			// System.Xut.println("matcher.group="+matcher.group());
			String[] ary = { matcher.group(), matcher.end() + "" };
			idxsGrps.add(ary);
		}
		return idxsGrps;
	}

	public static List<String[]> getAllMatchedGroupsAndStartIdxLocs(String text, Pattern pattern) throws IOException {

		Matcher matcher = pattern.matcher(text);

		List<String[]> idxsGrps = new ArrayList<String[]>();

		while (matcher.find() && matcher.group().length() > 0) {
			// idx = matcher.start();
			// if (idx >= 0)
//			 System.out.println("matcher.group="+matcher.group());
			String[] ary = { matcher.group(), matcher.start() + "" };
			idxsGrps.add(ary);
		}

		return idxsGrps;
	}

	public List<String[]> getAllEndIdxMatchedGroupRow(String text, Pattern pattern, String row) throws IOException {

		// System.Xut.println("getAllMatchedGroups text="+text.substring(0,Math.min(100,
		// text.length())));
		// text = Utils.readTextFromFile(""+getTableNamePrefixYearQtr(fileDate)
		// + "regexv2.txt");

		Matcher matcher = pattern.matcher(text);

		List<String[]> idxsGrps = new ArrayList<String[]>();

		while (matcher.find() && matcher.group().length() > 0) {
			// idx = matcher.start();
			// if (idx >= 0)
			// System.Xut.println("matcher.group="+matcher.group());
			String[] ary = { matcher.end() + "", matcher.group(), row };
			idxsGrps.add(ary);
		}
		return idxsGrps;
	}

	public String keepCellsInSameRow(String html, Pattern startPattern, Pattern endPattern)
			throws FileNotFoundException {

		// <td[^>]*
		// this simply removes all hard returns within start and end pattern

		StringBuffer sb = new StringBuffer();
		int start = 0, htmlLen = html.length();
		List<Integer> idxStartTrs = getAllIndexStartLocations(html, startPattern);
		List<Integer> idxEndTrs = getAllIndexStartLocations(html, endPattern);
		if (idxStartTrs.size() == 0 || idxEndTrs.size() == 0) {
			// System.Xut.println("no pattern found..");
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
						htmlTemp = htmlPara.matcher(htmlTemp).replaceAll(" ");
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
		return temp;
	}

	public static Map<Integer, List<String[]>> getSimpleDataMap(String tableText) throws IOException {
		TableParser tp = new TableParser();
		NLP nlp = new NLP();

		tableText = tableText.replaceAll("[\r\n]{1,3}", "\r");
		String[] tableTextRows = tableText.split("\r");
		String line;

		Map<Integer, List<String[]>> mapSimple = new TreeMap<Integer, List<String[]>>();
		for (int i = 0; i < tableTextRows.length; i++) {

			List<String[]> listRow = new ArrayList<String[]>();
			line = tableTextRows[i];
//			NLP.pwNLP.append(NLP.println("@nlp - getSimpleDataMap -- tableText line prior to data map=", line));
			if (line.replaceAll("[ ]{1,}|[_=]{1,}|[-]{1,}", "").length() < 2)
				continue;

			Pattern simpleColumnPattern = Pattern
					.compile("((\t|[\t ]{2}|   )\\(?\\.?\\d)|((?<=(\t | \t|\t|  ))\\-\\-?(?=(\t | \t|\t|  |$)))");

			List<Integer> listRowTmp = nlp.getAllIndexStartLocations(line, simpleColumnPattern);
			List<String[]> listRowTmp2 = nlp.getAllStartIdxLocsAndMatchedGroups(line, simpleColumnPattern);
			// NLP.printListOfStringArray("what am I matching - first idx",
			// listRowTmp2);

			if (null == listRowTmp || listRowTmp.size() < 1) {
				String[] ary = { 0 + "", line, "30" };
				listRow.add(ary);
			}

			String strData;
			for (int n = 0; n < listRowTmp.size(); n++) {

				// NLP.pwNLP.append(NLP.println("each idx loc in list to put into
				// mapDataSimple==",
				// listRowTmp.get(n)+""));

				if (n == 1) {
					strData = line.substring(listRowTmp.get(n - 1), listRowTmp.get(n));
					String[] ary = { (n + 40) + "", strData };
					// use n+40 so that last col eidx in getSubtotal is not less
					// than 3.

					listRow.add(ary);

//					NLP.pwNLP.append(NLP.println("mapDataSimple - n==1 - data col=", strData));

				}

				if (n + 1 < listRowTmp.size() && n != 0) {

					strData = line.substring(listRowTmp.get(n), listRowTmp.get(n + 1));
					String[] ary = { (n + 40 + 1) + "", strData };
					listRow.add(ary);

//					NLP.pwNLP.append(NLP.println("mapDataSimple - n+1<size - data col=", strData));
				}

				if (n == 0) {

					strData = line.substring(0, listRowTmp.get(n));
					String[] ary = { (n + 40) + "", strData, "30" };
					listRow.add(ary);
//					NLP.pwNLP.append(NLP.println("mapDataSimple - i=0 - data col=", strData));

				}

				if (n + 1 == listRowTmp.size()) {
					strData = line.substring(listRowTmp.get(n), line.length());
					String[] ary = { (n + 40 + 1) + "", strData };
					listRow.add(ary);

//					NLP.pwNLP.append(NLP.println("mapDataSimple - n+1=size - data col=", strData));
				}

			}
			// NLP.printMapIntListOfStringAry("adding to mapSimple", mapSimple);
			mapSimple.put(i, listRow);

		}

		return mapSimple;

	}

	public String getSentence(String html, Pattern startPattern, Pattern endPattern) throws IOException {

		StringBuffer sb = new StringBuffer();
		// sb.delete(0, sb.toString().length());
		int start = 0, htmlLen = html.length();
		List<Integer> idxStartTrs = getAllIndexStartLocations(html, startPattern);
		List<Integer> idxEndTrs = getAllIndexEndLocations(html, endPattern);
		if (idxStartTrs.size() == 0 || idxEndTrs.size() == 0) {
			// System.Xut.println("no pattern found..");
			return html;
		}
		int endTrI = 0, endTrLoc = 0;
		for (Integer idxStartTr : idxStartTrs) {
			if (start > idxStartTr)
				continue;
			sb.append(new String(html.substring(start, idxStartTr)));
			// System.Xut.println("s1=" + new String(html.substring(start, idxStartTr)));
			// above is identifying JUST the start of the pattern - so we do NOT
			// want to replace anything here!!

			for (Integer eTr = endTrI; eTr < idxEndTrs.size(); eTr++) {
				endTrI++;
				endTrLoc = idxEndTrs.get(eTr) + 1;
				if (endTrLoc <= idxStartTr)
					continue;
				else {
					String htmlTemp = new String(html.substring(idxStartTr, endTrLoc));
					htmlTemp = htmlTemp.replaceAll("[\r\n]{1,}|[\\s]{2,}", " ");
					if (startPattern.equals("startTd") || startPattern.equals("startTh")) {
						htmlTemp = htmlPara.matcher(htmlTemp).replaceAll(" ");
						// if <td > <p>hello</p>world</td> it removes the <p
						// (same for <div and <br)
					}
					sb.append(new String(htmlTemp));
					// System.Xut.println("s2=" + new String(htmlTemp));
					break;
				}
			}
			start = endTrLoc;
		}
		String keepCellsTextTogether = (html.substring(start, htmlLen));
		// sb.append(new String(html.substring(start, htmlLen)))
		sb.append(new String(keepCellsTextTogether));
		// System.Xut.println("s3=" + new String(keepCellsTextTogether));
		String temp = sb.toString();
		return temp;
	}

	public String removeAttachments(String text) throws FileNotFoundException {
		// System.Xut.println("remove attachments....");

		// PrintWriter tempPw1a = new PrintWriter(new File("c:/backtest/"
		// + getTableNamePrefixYearQtr(fileDate) + "temp1a.txt"));
		// tempPw1a.append(text);
		// tempPw1a.close();

		List<Integer> attachStartMerkerLocs = getAllIndexStartLocations(text,
				Pattern.compile("(?i)(begin.+\\.(jpg|gif|png))"));
		if (attachStartMerkerLocs.size() == 0) {
			// PrintWriter tempPw = new PrintWriter(new File("c:/backtest/"
			// + getTableNamePrefixYearQtr(fileDate) + "temp1.txt"));
			// tempPw.append(text);
			// tempPw.close();
			return text;
		}
		StringBuffer sb = new StringBuffer();
		int start = 0, textLen = text.length(), endAtchI = 0, endAtchLoc = 0;
		List<Integer> attachEndMarkerLocs = getAllIndexStartLocations(text,
				Pattern.compile("(?im)(end[.\r\n]+</TEXT>)"));
		for (Integer attachStartLoc : attachStartMerkerLocs) {
			if (start > attachStartLoc)
				continue;
			// sb.append(text.substring(start, attachStartLoc));
			sb.append(new String(text.substring(start, attachStartLoc)));
			for (Integer eai = endAtchI; eai < attachEndMarkerLocs.size(); eai++) {
				endAtchI++;
				endAtchLoc = attachEndMarkerLocs.get(eai);
				if (endAtchLoc <= attachStartLoc)
					continue;
				else {
					// sb.append(text.substring(attachStartLoc,
					// endAtchLoc));
					break;
				}
			}
			start = endAtchLoc;
		}
		sb.append(new String(text.substring(start, textLen)));
		// System.Xut.println("acceptedDate at remove attachment at end::"
		// + acceptedDate);
		// System.Xut.println("removeAttachment2 subtext 0 to 10:: "
		// + sb.toString().substring(0, 10));
		// PrintWriter tempPw = new PrintWriter(new File("c:/backtest/"
		// + getTableNamePrefixYearQtr(fileDate) + "temp1b.txt"));
		// tempPw.append(sb.toString());
		// tempPw.close();
		return sb.toString();
	}

	public String removeExtraBlanks(String text) throws FileNotFoundException {

		// System.Xut.println("removeExtraBlanks text200 characters="
		// + text.substring(0, 200));

		Pattern lowercaseLetterTablowercaseLetter = Pattern.compile("(?<=([a-z]{1}))(\t)(?=([a-z]{1}))");

		text = text.replaceAll("[ ]{1,7}\\)", "\\)").replaceAll("(\\([ ]{1,7})", "\\(");

		text = text.replaceAll("\\|", "\t")
				// replace rare instances where "|" is used as separator
				// .replaceAll(" \t|\t ", "\t")
				.replaceAll("\\. \\.", "\t").replaceAll(" \\. ", "\t").replaceAll("\\.\\$|\\. \\$", "\t");

		text = text.replaceAll("[\t]+", "\t").replaceAll("[\\.]{2,}\\$", "\t").replaceAll("[\\.]{2,}", " ")
				.replaceAll("[_=]{3,}", "")
				// replace .....
				.replaceAll(" [ ]{2,}", "\t")
				// 3 or more spaces
				.replaceAll(":  ", " ").replaceAll("\\$[ \t]+", "\\$");

		text = text.replaceAll("[-]{4,}", " ").replaceAll("\r\n[_=-]{1}", "\r\n")
		// cause errors where intended as blank?
		;
		text = text.replaceAll("[ \t]+\\)", "\\)").replaceAll("( \t)+", "\t").replaceAll("(\t )+", "\t")
				.replaceAll("(?m)\\p{Blank}+$", "");

		// could put at startGrp and then at tableText? Need low hard returns to
		// get correct endOftable
		Pattern wordTabHardReturnWordPattern = Pattern
				.compile("(?<=([a-zA-Z ;:\\)\\(,\\$\\d]{10}[a-z ;:\\)\\(,\\$\\d-]{12}))([\r\n]{1,2}\t)"
						+ "(?=([a-z ]{12}([a-zA-Z ,;:\\d\\$-]{10})))");
		// each line must have 22 characters - the first 10 chars can be
		// anything followed by 12 lower case etc then [\r\n]{2}\t and then
		// again by 12 lower case then 10 anything. Should be very exclusive.

		text = wordTabHardReturnWordPattern.matcher(text).replaceAll(" ");
		text = text.replaceAll("(?m)^[\r\n\\p{Blank}]+", "");
		// remove lines w/ only \s|\t|new-line chars preceding any word
		// <tab><tab>September 30, 2004<tab>December 30, 2003 looks like:
		// September 30, 2004<tab>December 30, 2003
		text = text.replaceAll("(?m)^([\r\n\\p{Blank}]+)$", "");
		// remove pure empty lines having just newlines
		text = text.replaceAll("[\r\n]{2,}$", "");
		text = lowercaseLetterTablowercaseLetter.matcher(text).replaceAll(" ");

		Pattern YearSpaceMonthPattern = Pattern.compile(
				"(?<=(19\\d{2}|20\\d{2}))[ \\xA0]{1,2}(?=(Jan[\\.\t ]{1}|January|Feb[\\.\t ]{1}|February| Mar[\\.\t ]{1}|March|Apr[\\.\t ]{1}|April"
						+ "|May[\\.\t ](?!([Nn]{1}ot|Dep|Limit))|Jun[\\.\t ]{1}|June|Jul[\\.\t ]{1}|July|Aug[\\.\t ]{1}|August|Sep[\\.\t ]{1}|September|Oct[\\.\t ]{1}|October|Nov[\\.\t ]{1}"
						+ "|November|Dec[\\.\t ]{1}|December|JAN[\\.\t ]{1}|JANUARY|FEB[\\.\t ]{1}|FEBRUARY| MAR[\\.\t ]{1}|MARCH|APR[\\.\t ]{1}|APRIL"
						+ "|MAY[\\.\t ]{1}(?!(NOT|LIMIT|DEP))|JUN[\\.\t ]{1}|JUNE|JUL[\\.\t ]{1}|JULY|AUG[\\.\t ]{1}|AUGUST|SEP[\\.\t ]{1}|SEPTEMBER|OCT[\\.\t ]{1}|OCTOBER|NOV[\\.\t ]{1}"
						+ "|NOVEMBER|DEC[\\.\t ]{1}|DECEMBER))");
		// year followed by 1 or 2 spaces than month (can't be .).

		text = YearSpaceMonthPattern.matcher(text).replaceAll("\t");

		// this inserts tab when you have just two spaces between numbers.
		// e.g., $23,205 $23,250 or $(0.13) $.09
		Pattern numberTwoSpacesNumber = Pattern.compile("(?<=(\\d\\)?))  (?=(\\$?\\(?[\\.|\\d]))");

		// Pattern numberOneSpaceNumberHardReturn = Pattern
		// .compile("(?<=(\\d\\)?)) (?=(\\$?\\(?[\\.|\\d]).{2,14}\r\n)");
		// \\s matches any white space which includes tabs and hard returns.
		// above is duplicative of what column utility should do.
		// see for example row that reads:
		// Investments held-to-maturity -- fair value of $328,531 (2004) and
		// $344,814 (2003)
		// this also results in tableEnd b/c if you insert tab it picks up a
		// year in 2nd col which is a end tablemarker (after it gets past
		// startGrp). if there is a table that is very squeezed in this will be
		// useful - but somehow need to also exclude instances where it is a
		// year and not a number prior to replacing.

		text = numberTwoSpacesNumber.matcher(text).replaceAll("\t").replaceAll("\\$\\$", "\\$");
		// text = numberOneSpaceNumberHardReturn.matcher(text).replaceAll("\t");

		Pattern wordOneOrTwoSpacesNumberTab = Pattern.compile(
				"(?<=([A-Za-z;\\&,\\'\\$\\(\\)\\-]{3,5}))  (?=([\\$\\(\\- \\.]{0,4}[\\d]{1,3}[ ]{0,1}[\r\n\t]{1,2}" +
				// word2spaces then 3 digits o rless then \t and/or hard
				// return
						"|[\\$\\(\\- \\.]{0,4}[\\d]{1,3},[\\d,]{3,15}[ ]{0,1}[\r\n\t]{1,2}))");
		// word2spaces then 3 digits then ',' then 3to15 digits/',' then \t|\r\n
		// Cash\\s\\s$823,272\t$796,284\r\n

		text = wordOneOrTwoSpacesNumberTab.matcher(text).replaceAll("\t");

		text = text.replaceAll("Ã‚|\\xA0", "").replaceAll("Ã¢â‚¬â„¢", "'").replaceAll("[\t]{2,}+", "\t");
		// need to replace double tabs near last

		Pattern lowerCaseHardReturnlowerCase = Pattern.compile("(?<=[a-z] ?)\r\n(?= ?[a-z])");

		text = lowerCaseHardReturnlowerCase.matcher(text).replaceAll(" ");

		text = TableParser.hardReturnNumberTabNumber.matcher(text).replaceAll("\r\nBLANK ROW\t");
		text = hardReturnTabNumberTabNumber.matcher(text).replaceAll("\r\nBLANK ROW\t");

		text = text.replaceAll("[\t ]\r\n", "\r\n").replaceAll("[\r\n]{2,11}", "\r\n");
		text = text.replaceAll("(?i)[\r\n]{1}Indus", " Indus").replaceAll("(?i)ThreeMonth", "Three Month");
		text = text.replaceAll("[\t]{1,3}[\\$]{1,3}[\r\n]{1}", "\r")
				.replaceAll("[\r\n]{1}.{0,1}(Cons.{8,11})?statements?.{3,6}(operations?|income)",
						"\rStatements of Income\r")
				.replaceAll("[\r\n]{1}.{0,1}(Cons.{8,11})?balance.{1,2}sheets?", "\rBalance Sheets\r")
				.replaceAll("[\r\n]{1}.{0,1}(Cons.{8,11})?statements?.{3,6}cash.{0,2}flows?",
						"\rStatements of Cash Flows\r");
		// System.Xut.println("cash flow replace=="+text);
		// <==BLANKROW should probably be last
		return text;
	}

	public String getDateFromSlashMoDyYear(String moDayYr) {
		// may not work with generic or txt parser.

		if (moDayYr.contains("/") || moDayYr.contains("-")) {
			Pattern slashHyphenYY1990s = Pattern
					.compile("(?<=[\\d]{1,2}[\\/-]{1}[\\d]{1,2})[\\/-]{1}" + "(?=(9[\\d]{1}))");

			Pattern slashHyphenYY2000s = Pattern
					.compile("(?<=[\\d]{1,2}[\\/-]{1}[\\d]{1,2})[\\/-]{1}" + "(?=(0[\\d]{1}))");

			Pattern slashHyphenYYYY1990s = Pattern
					.compile("(?<=[\\d]{1,2}[\\/-]{1}[\\d]{1,2})[\\/-]{1}" + "(?=(19[\\d]{2}))");

			Pattern slashHyphenYYYY2000s = Pattern
					.compile("(?<=[\\d]{1,2}[\\/-]{1}[\\d]{1,2})[\\/-]{1}" + "(?=(20[\\d]{2}))");

			Pattern slashHyphenYY00s = Pattern.compile("([\\d]{1,2}[\\/-][\\d]{1,2}[\\/-](?=[0]{1}\\d))");
			// replace hyphen with " 20"

			Pattern slashHyphenYY90s = Pattern.compile("([\\d]{1,2}[\\/-][\\d]{1,2}[\\/-](?=[9]{1}\\d))");
			// replace hyphen with " 19"

			Pattern mo12SlashHyphen = Pattern.compile("(?<=[ \r\n\t]{1})12[\\/\\-]{1}(?=[\\d]{1,2})");
			Pattern mo11SlashHyphen = Pattern.compile("(?<=[ \r\n\t]{1})11[\\/\\-]{1}(?=[\\d]{1,2})");
			Pattern mo10SlashHyphen = Pattern.compile("(?<=[ \r\n\t]{1})10[\\/\\-]{1}(?=[\\d]{1,2})");
			Pattern mo9SlashHyphen = Pattern.compile("(?<=[ \r\n\t]{1})(9|09)[\\/\\-]{1}(?=[\\d]{1,2})");
			Pattern mo8SlashHyphen = Pattern.compile("(?<=[ \r\n\t]{1})(8|08)[\\/\\-]{1}(?=[\\d]{1,2})");
			Pattern mo7SlashHyphen = Pattern.compile("(?<=[ \r\n\t]{1})(7|07)[\\/\\-]{1}(?=[\\d]{1,2})");
			Pattern mo6SlashHyphen = Pattern.compile("(?<=[ \r\n\t]{1})(6|06)[\\/\\-]{1}(?=[\\d]{2})");
			Pattern mo5SlashHyphen = Pattern.compile("(?<=[ \r\n\t]{1})(5|05)[\\/\\-]{1}(?=[\\d]{1,2})");
			Pattern mo4SlashHyphen = Pattern.compile("(?<=[ \r\n\t]{1})(4|04)[\\/\\-]{1}(?=[\\d]{1,2})");
			Pattern mo3SlashHyphen = Pattern.compile("(?<=[ \r\n\t]{1})(3|03)[\\/\\-]{1}(?=[\\d]{1,2})");
			Pattern mo2SlashHyphen = Pattern.compile("(?<=[ \t\r\n]{1})(02|2)[\\/\\-]{1}(?=[\\d]{1,2})");
			Pattern mo1SlashHyphen = Pattern.compile("(?<=[ \t\r\n]{1})(01|1)[\\/\\-]{1}(?=[\\d]{1,2})");

			moDayYr = slashHyphenYYYY1990s.matcher(moDayYr).replaceAll(" ");

			// <tab> b/c pattern match to find end limitStart
			// pivot off \t

			moDayYr = slashHyphenYYYY2000s.matcher(moDayYr).replaceAll(" ");

			// System.Xut.println("moDayYr="+moDayYr);
			// slashHyphenYYYY must go before below (YY).

			moDayYr = slashHyphenYY1990s.matcher(moDayYr).replaceAll(" 19");
			moDayYr = slashHyphenYY2000s.matcher(moDayYr).replaceAll(" 20");
			// System.Xut.println("moDayYr=" + moDayYr);
			moDayYr = slashHyphenYY00s.matcher(moDayYr).replaceAll(" 20");
			// replace hyphen with " 20"

			moDayYr = slashHyphenYY90s.matcher(moDayYr).replaceAll(" 19");
			// replace hyphen with " 19"

			// System.Xut.println("A. moDayYr="+moDayYr);
			moDayYr = mo12SlashHyphen.matcher(moDayYr).replaceAll(" December ");
			moDayYr = mo11SlashHyphen.matcher(moDayYr).replaceAll(" November ");
			moDayYr = mo10SlashHyphen.matcher(moDayYr).replaceAll(" October ");
			moDayYr = mo9SlashHyphen.matcher(moDayYr).replaceAll(" September ");
			moDayYr = mo8SlashHyphen.matcher(moDayYr).replaceAll(" August ");
			moDayYr = mo7SlashHyphen.matcher(moDayYr).replaceAll(" July ");
			moDayYr = mo6SlashHyphen.matcher(moDayYr).replaceAll(" June ");
			moDayYr = mo5SlashHyphen.matcher(moDayYr).replaceAll(" May ");
			moDayYr = mo4SlashHyphen.matcher(moDayYr).replaceAll(" April ");
			moDayYr = mo3SlashHyphen.matcher(moDayYr).replaceAll(" March ");
			moDayYr = mo2SlashHyphen.matcher(moDayYr).replaceAll(" February ");
			moDayYr = mo1SlashHyphen.matcher(moDayYr).replaceAll(" January ");
		}

		return moDayYr;
	}

	public boolean doesPatternMatch(String text, Pattern pattern) {
		Matcher matcher = pattern.matcher(text);
		return matcher.find();
	}

	public String stripLineWrap(String text) throws FileNotFoundException {

		text = text.replaceAll("(?i)<div[^>]*>", "<div>xXx").replaceAll("(?i)<p[^>]*>", "<p>xXx")
				.replaceAll("(?i)<BR>", "<br>xXx").replaceAll("[\r\n]{1,}", " ").replaceAll("xXx", "\r\n")
				.replaceAll("\r\n[\t \\s]{1,}", "\r\n").replaceAll("[ ]{2}", " ").trim();

		return text;
	}

	public static String println(String DescriptionOfText, String textToPrint) {

		/*
		 * see also c:/temp2/t2.txt in TableParserText and c:/temp2/t1 in
		 * TableParserHtml -- make sure 'delete.file and create.file' is off' when
		 * parsing in mass. when testing and i want to see written sys print - uncomment
		 * this
		 */

//		System.out.println("textToPrint"+textToPrint);

		if (DescriptionOfText == null) {
			return "";
		}

		if (null == textToPrint) {
			return "";
		}

		String text = DescriptionOfText + "=" + textToPrint + "||END\r";

		text = text.replaceAll("[\r\n]{3}+", "\r\r");
		if (text.replaceAll("(?i)NUL", "").length() == 0) {
			return "";
		}

		return "\r\n" + text;

//		return "";

	}

	public String stripHtmlTags(String text) throws FileNotFoundException {

		// System.Xut.println("stripHtmlTags text200 characters="
		// + text.substring(0, 200));

		TableParser tp = new TableParser();

		if (tp.acceptedDate != null)
			tp.fileDate = tp.acceptedDate;

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

		try {
			text = text.replaceAll(TRPattern.toString(), LineSeparator);
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
			text = text.replaceAll("\\&#146;", "'");
			text = text.replaceAll("\\&#[\\d]*;", "");
			// <=necessary?
			// deleted .replaceAll(SpacesWithinTDs.toString(), " ")
			text = text.replaceAll(TDWithColspanPattern.toString(), "\t\t");
			text = text.replaceAll(TDPattern.toString(), "\t");
			text = text.replaceAll("(?i)(<SUP[^>]*>[\\(0-9\\) ]*</SUP[^>]*>)", "");
			// all superscripts [i.e. (1) (2)...] with blank
			text = text.replaceAll("</U>|<U>|</u>|<U>", "");
			// above is removed b/c it interfers with below <BR>
			text = text.replaceAll("(?i)<BR> ?\r\n ?<BR>", "\r\n");
			text = text.replaceAll("(?i)<BR>", "\r\n");
			// System.Xut.println("text aft <br> replace=="+text);
			// if 2 consecutive BRs-likely meant hard return
			// if BR after a number - likely end of a row.
			text = numberBR.matcher(text).replaceAll("\r\n");
			text = text.replaceAll("(?i)(<BR>\r\n|\r\n<BR>)", " ");

			text = text.replaceAll("(?i)<table[^>]*>", ""); // was replaced with
															// " xx "
			text = text.replaceAll("(?i)</table[^>]*>", "");// was replaced with
															// " xx "
			// need 'xx' b/c it will be picked up as shortline to count against
			// endofTable (if too many rows not meeting criteria it is
			// endOfTable - and 'xx' is one such row). Often </table if followed
			// by <table - so keep both - as those are 2 rows to count towards
			// likely endofT. Need space bef/aft xx or it corrupts tablenName

			text = text.replaceAll("(?i)(</strong>|<strong>|</small>|<small>)", "");

			text = text.replaceAll("(?i)<BR>[ ]{1,10}", "<BR>").replaceAll(ExtraHtmlPattern.toString(), "")
					.replaceAll(ExtraHtmlPattern2.toString(), " ");
			// previously I replaced all ExtraHtmlPatterns with a "~" and then
			// replaced all them with just 1 ws. This causes problems.
			// Ultimately if I need to remove problematic html code. Focus on
			// removing html code that only formats and replace with nothing and
			// for that which is a hard return or tab or row replace with a
			// space or hard return or tab.

		} catch (Throwable t) {
			t.printStackTrace(System.out);
		}

		return text;
	}

	public String formatTextParagraph(String text) throws IOException {
		// System.Xut.println("new section");
		formtxtPara++;
		// text = Utils.readTextFromFile("c:/getcontracts/temp/temp4.txt");

		text = text.replaceAll("[\r\n]{1}[\t \\-=]{1,200}[\r\n]{1}", "\r\r");
		// System.Xut.println("formatText after replacment=="+text);

		// \\s will search across lines - so can't use ';,' etc. (?sm) doesn't
		// work.

		Pattern patternReplace1 = Pattern.compile("(?<=[\\(\\)a-z ]{3})[\r\n](?=[a-z ]{3})", Pattern.MULTILINE);
		Matcher match1 = patternReplace1.matcher(text);
		text = match1.replaceAll(" ");

		String[] lines = text.split("\r");
		@SuppressWarnings("unused")
		String line = "", priorLine = null, nextLine = null, tmpLine, lineStart6, lineEnd6, nextTmpLine,
				nextLineStart6 = null, nextLineEnd6;
		int curLineIdxStart = -1, priorLineIdxStart = -1;
		StringBuffer sb = new StringBuffer();
		@SuppressWarnings("unused")
		int cnt = 0, i_pNo, i_prior_pNo;
		String pE = "";
		String prior_pE = "";
		String pNo = "";
		String prior_pNo = "";

		for (int i = 0; i < lines.length; i++) {
			line = lines[i] + " ";
			// get begin and end of a line to see if para start or end markers
			// are present.
			tmpLine = line.replaceAll("[ \t]{2,}", " ");
			List<String[]> listParaNumberMarkerNextLine = new ArrayList<>();

			if ((i + 1) < lines.length) {
				nextLine = lines[i + 1];
				nextTmpLine = nextLine.replaceAll("[ \t]{2,}", " ");
				if (nextTmpLine.length() < 1) {
					if ((i + 2) < lines.length) {
						nextLine = lines[i + 2];
						nextTmpLine = nextLine.replaceAll("[ \t]{2,}", " ");
					}
				}

				nextLineStart6 = nextTmpLine.substring(0, Math.min(nextTmpLine.length(), 6));

				nextLineEnd6 = nextTmpLine.substring(Math.max(nextTmpLine.length() - 6, 0), nextTmpLine.length());

				if (null != nextLineStart6 && (nextLineStart6.length() > 2
						&& nextLineStart6.substring(0, 3).replaceAll("[a-zA-Z;, ]{3}", "").trim().length() > 0
						|| nextLineStart6.length() < 3)) {
					listParaNumberMarkerNextLine = getAllStartIdxLocsAndMatchedGroups(nextLineStart6,
							patternParaNumberMarker);

					// if (listParaNumberMarkerNextLine.size() > 0) {
					// System.Xut.println("line=" + line.replaceAll("\n", "")
					// + "listParaNumberMarkerNextLine idxs=="
					// + " grp=="
					// + listParaNumberMarkerNextLine.get(0)[1]
					// + " listParaNumberMarkerNextLine.size=="
					// + listParaNumberMarkerNextLine.size());
					// }
				}
			}

			lineStart6 = tmpLine.substring(0, Math.min(tmpLine.length(), 6));
			// if(line.length()>4)
			// System.Xut.println("lineStart="+lineStart6+"| line.length="+line.length());
			// lineEnd10 =
			// tmpLine.substring(Math.max(tmpLine.length()-10,0),tmpLine.length());
			lineEnd6 = tmpLine.substring(Math.max(tmpLine.length() - 6, 0), tmpLine.length());

			// System.Xut.println("lineEnd6="+lineEnd6);
			// System.Xut.println("priorLine`` ="+priorLine+ "\r line="+line);

			if (i == 0) {
				priorLine = line;
				continue;
			}

			// any visible character - get first location
			List<Integer> listPriorLineStartIdx = getAllIndexStartLocations(priorLine, patternAnyVisualCharacter);
			List<Integer> listCurLineStartIdx = getAllIndexStartLocations(line, patternAnyVisualCharacter);
			List<String[]> listParaMarkerEnd = new ArrayList<>();
			List<String[]> listParaNumberMarker = new ArrayList<>();

			if (null != lineEnd6) {
				// see if prior line ends with a para marker end - eg,
				// 'and/or:,;'
				listParaMarkerEnd = getAllStartIdxLocsAndMatchedGroups(lineEnd6, NLP.patternParaMarkerEnd);
				// System.Xut.println("listParaMarkerEnd.size="+listParaMarkerEnd.size()+"
				// lineEnd6=="+lineEnd6);
			}

			// if (null == lineEnd6) {
			// System.Xut.println("lineEnd6==null");
			// }

			if (null != lineStart6 && getAllStartIdxLocsAndMatchedGroups(lineStart6, patternParaNumberMarker).size() > 0
					&& (lineStart6.length() > 2
							&& lineStart6.substring(0, 3).replaceAll("[a-zA-Z;, ]{3}", "").trim().length() > 0
							|| lineStart6.length() < 3)) {

				listParaNumberMarker = getAllStartIdxLocsAndMatchedGroups(lineStart6, patternParaNumberMarker);
				// System.Xut.println("lineStart6=" + lineStart6
				// + " listParaNumberMarker.size="
				// + listParaNumberMarker.size()
				// + " listParaNumberMarker.grp="
				// + listParaNumberMarker.get(0)[1]
				// +" lineEnd6="+lineEnd6
				// );
				// if(listParaMarkerEnd.size()>0)
				// System.Xut.println("pE="+listParaMarkerEnd.get(0)[1]);
			}

			// System.Xut.println("idxPriorList.size="+idxPriorList.size()+
			// " idxList.size="+idxList.size());

			// idx is start of first line and priorIdx is priorLine start (1st
			// visual char).
			if (listPriorLineStartIdx.size() > 0) {
				priorLineIdxStart = listPriorLineStartIdx.get(0);
			}
			if (listCurLineStartIdx.size() > 0) {
				curLineIdxStart = listCurLineStartIdx.get(0);
			}

			// cnt++ measures when a line is appended. (can only append once);
			if (listParaMarkerEnd.size() > 0) {
				pE = listParaMarkerEnd.get(0)[1].trim();
			}

			if (listParaNumberMarker.size() > 0) {
				pNo = listParaNumberMarker.get(0)[1].trim();
			}

			// System.Xut.println("pE="+pE+" prior_pE="+prior_pE+" pNo="+pNo);

			if (listParaNumberMarker.size() > 0 && listParaMarkerEnd.size() > 0
					&& (listParaNumberMarkerNextLine.size() > 0 || prior_pE.contains(" and")
							|| prior_pE.contains(" or"))) {

				i_prior_pNo = convertAlphaParaNumbering(prior_pNo, 0);
				i_pNo = convertAlphaParaNumbering(pNo, 0);
				// System.Xut.println("i_prior_pNo="+i_prior_pNo
				// +" i_pNo="+i_pNo);

				sb.append(/* "[[pNo:" + pNo + "]]" + */"\r" + line /*
																	 * + "[[pE:" + pE + "]]"
																	 */);

				// System.Xut.println("[[pNo:" + pNo + "]] line="+
				// line.substring(0,Math.min(line.length(), 15))+ " prior_pE="
				// + prior_pE.replaceAll("[\r\n]", "") + "|" + " [[pE:"
				// + pE + "]]" + "|");
				cnt++;
			}

			if (listParaMarkerEnd.size() > 0 && listParaNumberMarker.size() <= 0
					&& listParaNumberMarkerNextLine.size() > 0) {
				sb.append("\ryyPe" + line /* + "[[pE:" + pE + "]]" */);
				// System.Xut.println("yyPe [[pE:" + pE + "]] line="
				// + line.substring(0, Math.min(line.length(), 15))
				// + " prior_pNo=" + prior_pNo.replaceAll("[\r\n]", "")
				// + "|");
				i_prior_pNo = convertAlphaParaNumbering(prior_pNo, 0);
				i_pNo = convertAlphaParaNumbering(pNo, 0);
				// System.Xut.println("i_prior_pNo="+i_prior_pNo
				// +" i_pNo="+i_pNo);

				// cks prior line to see if it looks like a para end (',;:
				// and/or) and marks line
				cnt++;
			}

			if (listParaNumberMarker.size() > 0 && listParaMarkerEnd.size() <= 0) {
				sb.append(/* "[[pNo:" + pNo + "]]" + */"\r" + line);
				// ck cur line to see starts w/ para#
				// System.Xut.println("[[pNo:" + pNo + "]] line="
				// + line.substring(0, Math.min(line.length(), 15))
				// + " prior_pE=" + prior_pE.replaceAll("[\r\n]", "") + "|");

				i_prior_pNo = convertAlphaParaNumbering(prior_pNo, 0);
				i_pNo = convertAlphaParaNumbering(pNo, 0);
				// System.Xut.println("i_prior_pNo="+i_prior_pNo
				// +" i_pNo="+i_pNo);

				cnt++;
			}

			if (pE.contains(";") || pE.contains(":") || pE.contains(",")) {
				prior_pE = pE;
				// System.Xut.println("prior_pE="+prior_pE);
			}

			if (pNo.replaceAll("[pNo:\r\n\t ]{1,10}", "").length() > 0) {
				prior_pNo = pNo;
				// System.Xut.println("prior_pNo"+prior_pNo);
			}

			if (listPriorLineStartIdx.size() > 0 && listCurLineStartIdx.size() > 0
					&& curLineIdxStart <= priorLineIdxStart) {
				if (listParaNumberMarker.size() <= 0 && listParaMarkerEnd.size() <= 0) {
					sb.append("\rxxzz" + line.replaceAll("[ \\s]{2,}", " "));
					cnt++;
				}

			}
			if (cnt == 0) {
				sb.append("\r" + line);
			}
			cnt = 0;
			priorLine = line;
			// priorLineEnd10 = lineEnd10;
			// priorLineEnd6 = lineEnd6;
			// priorLineStart6 = lineStart6;
		}
		text = sb.toString();
		PrintWriter tempPw99 = new PrintWriter(new File("c:/getContracts/temp/temp99" + formtxtPara + ".txt"));
		tempPw99.append(text);
		tempPw99.close();

		text = text.replaceAll("xxzz[\r\n]|yyPe[\r\n]", "\r");

		PrintWriter tempPw100 = new PrintWriter(new File("c:/getContracts/temp/temp100" + formtxtPara + ".txt"));
		tempPw100.append(text);
		tempPw100.close();

		text = text.replaceAll("xxzz", " ");
		// gets lots of w/s between char on a line (not b/w paras).
		PrintWriter tempPw101 = new PrintWriter(new File("c:/getContracts/temp/temp101" + formtxtPara + ".txt"));
		tempPw101.append(text);
		tempPw101.close();

		text = text.replaceAll("(?<=([\\p{Punct}\\p{Alnum}]{1}))[ ]{2,}(?=([\\p{Punct}\\p{Alnum}]{1}))", " ");

		PrintWriter tempPw102 = new PrintWriter(new File("c:/getContracts/temp/temp102" + formtxtPara + ".txt"));
		tempPw102.append(text);
		tempPw102.close();

		text = text.replaceAll("\\]\\] \\[\\[wasPg\\#\\]\\]\\[\\[", "\\]\\] \r\\[\\[wasPg\\#\\]\\]\r\\[\\[");
		// gets instances of a pg# sandwhiched on a line - and puts it on a line
		// by itself

		PrintWriter tempPw103 = new PrintWriter(new File("c:/getContracts/temp/temp103" + formtxtPara + ".txt"));
		tempPw103.append(text);
		tempPw103.close();

		text = text.replaceAll(
				"(?<=([\\p{Punct}\\p{Alnum}]{1} ?))\\[\\[wasPg\\#\\]\\] ?(?=([\\p{Punct}\\p{Alnum}]{1}))",
				"\r\\[\\[wasPg\\#\\]\\]\r");

		PrintWriter tempPw104 = new PrintWriter(new File("c:/getContracts/temp/temp104" + formtxtPara + ".txt"));
		tempPw104.append(text);
		tempPw104.close();

		return text;

	}

	public List<String> getParaMapOfParents(Map<Integer, String> map) {

		List<String> tmpListOfMap = new ArrayList<String>();
		List<String> listOfMap = new ArrayList<String>();

		for (Map.Entry<Integer, String> entry : map.entrySet()) {
			tmpListOfMap.add(entry.getValue());
		}

		for (int i = 0; i < tmpListOfMap.size(); i++) {
			if (tmpListOfMap.contains(":")) {
			}
		}

		// if para ends with ':' then the para that precedes it is the parent -
		// irrespective of the pNo.
		Matcher match = null;
		// List<String[]> listWithParentMarker = new ArrayList<String[]>();
		for (int i = 0; i < listOfMap.size(); i++) {
			if ((i + 1) < listOfMap.size()) {
				match = patternParaNumberMarker.matcher(listOfMap.get(i + 1));
			}
			if ((i + 1) < listOfMap.size() && listOfMap.get(i + 1).contains(":") && match.find()) {
				// if ':' is present in

			}

		}
		return listOfMap;

	}

	public void getParaNumbering(Map<Integer, String> map) {
		Map<Integer, String> mapParaNmbr = new TreeMap<Integer, String>();
		Matcher match;

		String paraNumber;
		// int paraNumberConverted;
		for (Map.Entry<Integer, String> entry : map.entrySet()) {
			paraNumber = entry.getValue();
			match = patternParaNumberMarker.matcher(paraNumber);
			if (match.find()) {
				mapParaNmbr.put(entry.getKey(), paraNumber);
			}
		}
		for (Map.Entry<Integer, String> entry : mapParaNmbr.entrySet()) {
			// System.Xut.println("key==" + entry.getKey() + " value==" + entry.getValue());
		}
	}

	public static int convertAlphaParaNumbering(String alphaNumber, int cnt) {

		// converts each to a unique divisor of 1, 10, 100 and so on
		int paraNumber = 0;

		String[] alphaParenLc = { "(a)", "(b)", "(c)", "(d)", "(e)", "(f)", "(g)", "(h)", "(i)", "(j)", "(k)", "(l)",
				"(m)", "(n)", "(o)", "(p)", "(q)", "(r)", "(s)", "(t)", "(u)", "(v)", "(w)", "(x)", "(y)", "(z)",
				"(aa)", "(bb)", "(cc)", "(dd)", "(ee)", "(ff)", "(gg)", "(hh)", "(ii)", "(jj)", "(kk)", "(ll)", "(mm)",
				"(nn)", "(oo)", "(pp)", "(qq)", "(rr)", "(ss)", "(tt)", "(uu)", "(vv)", "(ww)", "(xx)", "(yy)",
				"(zz)" };

		// v[5,22],i[1,9],x[10,24],ii[2,35],[20,50] <=Uc/Lc
		if (alphaNumber.replaceAll("\\([a-z]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenLc.length; i++) {
				if (alphaNumber.equals(alphaParenLc[i])) {
					return paraNumber = (i + 1);
				}
			}
		}

		String[] romanParenLc = { "(i)", "(ii)", "(iii)", "(iv)", "(v)", "(vi)", "(vii)", "(viii)", "(ix)", "(x)",
				"(xi)", "(xii)", "(xiii)", "(xiv)", "(xv)", "(xvi)", "(xvii)", "(xviii)", "(ixx)", "(xx)" };

		if (alphaNumber.replaceAll("\\([ivx]{1,5}\\)", "").trim().length() < 1) {
			for (int i = 0; i < romanParenLc.length; i++) {
				if (alphaNumber.equals(romanParenLc[i])) {
					// System.Xut.println("romanParenLc alphaNumber="
					// + alphaNumber);
					return paraNumber = (i + 1) * 10;
				}
			}
		}

		String[] romanParenUc = { "(I)", "(II)", "(III)", "(IV)", "(V)", "(VI)", "(VII)", "(VIII)", "(IX)", "(X)",
				"(XI)", "(XII)", "(XIII)", "(XIV)", "(XV)", "(XVI)", "(XVII)", "(XVIII)", "(IXX)", "(XX)" };

		if (alphaNumber.replaceAll("\\([ivx]{1,5}\\)", "").trim().length() < 1) {
			for (int i = 0; i < romanParenUc.length; i++) {
				if (alphaNumber.equals(romanParenUc[i])) {
					return paraNumber = (i + 1) * 100;
				}
			}
		}

		String[] alphaParenUc = { "(A)", "(B)", "(C)", "(D)", "(E)", "(F)", "(G)", "(H)", "(I)", "(J)", "(K)", "(L)",
				"(M)", "(N)", "(O)", "(P)", "(Q)", "(R)", "(S)", "(T)", "(U)", "(V)", "(W)", "(X)", "(Y)", "(Z)",
				"(AA)", "(BB)", "(CC)", "(DD)", "(EE)", "(FF)", "(GG)", "(HH)", "(II)", "(JJ)", "(KK)", "(LL)", "(MM)",
				"(NN)", "(OO)", "(PP)", "(QQ)", "(RR)", "(SS)", "(TT)", "(UU)", "(VV)", "(WW)", "(XX)", "(YY)",
				"(ZZ)" };

		if (alphaNumber.replaceAll("\\([A-Z]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenUc.length; i++) {
				if (alphaNumber.equals(alphaParenUc[i])) {
					return paraNumber = (i + 1) * 1000;
				}
			}
		}

		String[] alphaPeriodUc = { "A.", "B.", "C.", "D.", "E.", "F.", "G.", "H.", "I.", "J.", "K.", "L.", "M.", "N.",
				"O.", "P.", "Q.", "R.", "S.", "T.", "U.", "V.", "W.", "X.", "Y.", "Z.", "AA.", "BB.", "CC.", "DD.",
				"EE.", "FF.", "GG.", "HH.", "II.", "JJ.", "KK.", "LL.", "MM.", "NN.", "OO.", "PP.", "QQ.", "RR.", "SS.",
				"TT.", "UU.", "VV.", "WW.", "XX.", "YY.", "ZZ." };

		if (alphaNumber.replaceAll("[A\\.]{2,3}", "").trim().length() < 1) {
			for (int i = 0; i < alphaPeriodUc.length; i++) {
				if (alphaNumber.equals(alphaPeriodUc[i])) {
					return paraNumber = (i + 1) * 10000;
				}
			}
		}

		String[] numberParen = { "(1)", "(2)", "(3)", "(4)", "(5)", "(6)", "(7)", "(8)", "(9)", "(10)", "(11)", "(12)",
				"(13)", "(14)", "(15)", "(16)", "(17)", "(18)", "(19)", "(20)", "(21)", "(22)", "(23)", "(24)", "(25)",
				"(26)", "(27)", "(28)", "(29)", "(30)", "(31)", "(32)", "(33)", "(34)", "(35)", "(36)", "(37)", "(38)",
				"(39)", "(40)", "(41)", "(42)", "(43)", "(44)", "(45)", "(46)", "(47)", "(48)", "(49)", "(50)", "(51)",
				"(52)", "(53)", "(54)", "(55)", "(56)", "(57)", "(58)", "(59)", "(60)", "(61)", "(62)", "(63)", "(64)",
				"(65)", "(66)", "(67)", "(68)", "(69)", "(70)", "(71)", "(72)", "(73)", "(74)", "(75)", "(76)", "(77)",
				"(78)", "(79)", "(80)", "(81)", "(82)", "(83)", "(84)", "(85)", "(86)", "(87)", "(88)", "(89)", "(90)",
				"(91)", "(92)", "(93)", "(94)", "(95)", "(96)", "(97)", "(98)", "(99)" };

		if (alphaNumber.replaceAll("\\([0-9]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < numberParen.length; i++) {
				if (alphaNumber.equals(numberParen[i])) {
					return paraNumber = (i + 1) * 100000;
				}
			}
		}

		String[] numberPeriod = { "1.", "2.", "3.", "4.", "5.", "6.", "7.", "8.", "9.", "10.", "11.", "12.", "13.",
				"14.", "15.", "16.", "17.", "18.", "19.", "20.", "21.", "22.", "23.", "24.", "25.", "26.", "27.", "28.",
				"29.", "30.", "31.", "32.", "33.", "34.", "35.", "36.", "37.", "38.", "39.", "40.", "41.", "42.", "43.",
				"44.", "45.", "46.", "47.", "48.", "49.", "50.", "51.", "52.", "53.", "54.", "55.", "56.", "57.", "58.",
				"59.", "60.", "61.", "62.", "63.", "64.", "65.", "66.", "67.", "68.", "69.", "70.", "71.", "72.", "73.",
				"74.", "75.", "76.", "77.", "78.", "79.", "80.", "81.", "82.", "83.", "84.", "85.", "86.", "87.", "88.",
				"89.", "90.", "91.", "92.", "93.", "94.", "95.", "96.", "97.", "98.", "99." };

		if (alphaNumber.replaceAll("[0-9\\.]{2,3}", "").trim().length() < 1) {
			for (int i = 0; i < numberPeriod.length; i++) {
				if (alphaNumber.equals(numberPeriod[i])) {
					return paraNumber = (i + 1) * 1000000;
				}
			}
		}
		// to add new para# types - copy 1 section above and increase by a
		// factor of 10 (i+1)*10000000
		return paraNumber;
	}

	public int convertAlphaParaNumb2(String alphaNumber, int n) {
		int paraNumber = 0;
		// converts each to 1, 2, 3 ...

		String[] alphaParenLc = { "(a)", "(b)", "(c)", "(d)", "(e)", "(f)", "(g)", "(h)", "(i)", "(j)", "(k)", "(l)",
				"(m)", "(n)", "(o)", "(p)", "(q)", "(r)", "(s)", "(t)", "(u)", "(v)", "(w)", "(x)", "(y)", "(z)",
				"(aa)", "(bb)", "(cc)", "(dd)", "(ee)", "(ff)", "(gg)", "(hh)", "(ii)", "(jj)", "(kk)", "(ll)", "(mm)",
				"(nn)", "(oo)", "(pp)", "(qq)", "(rr)", "(ss)", "(tt)", "(uu)", "(vv)", "(ww)", "(xx)", "(yy)",
				"(zz)" };

		// v[5,22],i[1,9],x[10,24],ii[2,35],[20,50] <=Uc/Lc
		if (alphaNumber.replaceAll("\\([a-z]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenLc.length; i++) {
				if (alphaNumber.equals(alphaParenLc[i]) && n == i) {
					return paraNumber = (i + 1);
				}
			}
		}

		String[] romanParenLc = { "(i)", "(ii)", "(iii)", "(iv)", "(v)", "(vi)", "(vii)", "(viii)", "(ix)", "(x)",
				"(xi)", "(xii)", "(xiii)", "(xiv)", "(xv)", "(xvi)", "(xvii)", "(xviii)", "(ixx)", "(xx)" };

		if (alphaNumber.replaceAll("\\([ivx]{1,5}\\)", "").trim().length() < 1) {
			for (int i = 0; i < romanParenLc.length; i++) {
				if (alphaNumber.equals(romanParenLc[i]) && n == i) {
//					 System.out.println("romanParenLc alphaNumber="
//					 + alphaNumber);
					return paraNumber = (i + 1);
				}
			}
		}

		String[] romanParenUc = { "(I)", "(II)", "(III)", "(IV)", "(V)", "(VI)", "(VII)", "(VIII)", "(IX)", "(X)",
				"(XI)", "(XII)", "(XIII)", "(XIV)", "(XV)", "(XVI)", "(XVII)", "(XVIII)", "(IXX)", "(XX)" };

		if (alphaNumber.replaceAll("\\([ivx]{1,5}\\)", "").trim().length() < 1) {
			for (int i = 0; i < romanParenUc.length; i++) {
				if (alphaNumber.equals(romanParenUc[i]) && n == i) {
					return paraNumber = (i + 1);
				}
			}
		}

		String[] alphaParenUc = { "(A)", "(B)", "(C)", "(D)", "(E)", "(F)", "(G)", "(H)", "(I)", "(J)", "(K)", "(L)",
				"(M)", "(N)", "(O)", "(P)", "(Q)", "(R)", "(S)", "(T)", "(U)", "(V)", "(W)", "(X)", "(Y)", "(Z)",
				"(AA)", "(BB)", "(CC)", "(DD)", "(EE)", "(FF)", "(GG)", "(HH)", "(II)", "(JJ)", "(KK)", "(LL)", "(MM)",
				"(NN)", "(OO)", "(PP)", "(QQ)", "(RR)", "(SS)", "(TT)", "(UU)", "(VV)", "(WW)", "(XX)", "(YY)",
				"(ZZ)" };

		if (alphaNumber.replaceAll("\\([A-Z]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenUc.length; i++) {
				if (alphaNumber.equals(alphaParenUc[i]) && n == i) {
					return paraNumber = (i + 1);
				}
			}
		}

		String[] alphaPeriodUc = { "A.", "B.", "C.", "D.", "E.", "F.", "G.", "H.", "I.", "J.", "K.", "L.", "M.", "N.",
				"O.", "P.", "Q.", "R.", "S.", "T.", "U.", "V.", "W.", "X.", "Y.", "Z.", "AA.", "BB.", "CC.", "DD.",
				"EE.", "FF.", "GG.", "HH.", "II.", "JJ.", "KK.", "LL.", "MM.", "NN.", "OO.", "PP.", "QQ.", "RR.", "SS.",
				"TT.", "UU.", "VV.", "WW.", "XX.", "YY.", "ZZ." };

		if (alphaNumber.replaceAll("[A\\.]{2,3}", "").trim().length() < 1) {
			for (int i = 0; i < alphaPeriodUc.length; i++) {
				if (alphaNumber.equals(alphaPeriodUc[i]) && n == i) {
					return paraNumber = (i + 1);
				}
			}
		}

		String[] numberParen = { "(1)", "(2)", "(3)", "(4)", "(5)", "(6)", "(7)", "(8)", "(9)", "(10)", "(11)", "(12)",
				"(13)", "(14)", "(15)", "(16)", "(17)", "(18)", "(19)", "(20)", "(21)", "(22)", "(23)", "(24)", "(25)",
				"(26)", "(27)", "(28)", "(29)", "(30)", "(31)", "(32)", "(33)", "(34)", "(35)", "(36)", "(37)", "(38)",
				"(39)", "(40)", "(41)", "(42)", "(43)", "(44)", "(45)", "(46)", "(47)", "(48)", "(49)", "(50)", "(51)",
				"(52)", "(53)", "(54)", "(55)", "(56)", "(57)", "(58)", "(59)", "(60)", "(61)", "(62)", "(63)", "(64)",
				"(65)", "(66)", "(67)", "(68)", "(69)", "(70)", "(71)", "(72)", "(73)", "(74)", "(75)", "(76)", "(77)",
				"(78)", "(79)", "(80)", "(81)", "(82)", "(83)", "(84)", "(85)", "(86)", "(87)", "(88)", "(89)", "(90)",
				"(91)", "(92)", "(93)", "(94)", "(95)", "(96)", "(97)", "(98)", "(99)" };

		if (alphaNumber.replaceAll("\\([0-9]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < numberParen.length; i++) {
				if (alphaNumber.equals(numberParen[i]) && n == i) {
					return paraNumber = (i + 1);
				}
			}
		}

		String[] numberPeriod = { "1.", "2.", "3.", "4.", "5.", "6.", "7.", "8.", "9.", "10.", "11.", "12.", "13.",
				"14.", "15.", "16.", "17.", "18.", "19.", "20.", "21.", "22.", "23.", "24.", "25.", "26.", "27.", "28.",
				"29.", "30.", "31.", "32.", "33.", "34.", "35.", "36.", "37.", "38.", "39.", "40.", "41.", "42.", "43.",
				"44.", "45.", "46.", "47.", "48.", "49.", "50.", "51.", "52.", "53.", "54.", "55.", "56.", "57.", "58.",
				"59.", "60.", "61.", "62.", "63.", "64.", "65.", "66.", "67.", "68.", "69.", "70.", "71.", "72.", "73.",
				"74.", "75.", "76.", "77.", "78.", "79.", "80.", "81.", "82.", "83.", "84.", "85.", "86.", "87.", "88.",
				"89.", "90.", "91.", "92.", "93.", "94.", "95.", "96.", "97.", "98.", "99." };

		if (alphaNumber.replaceAll("[0-9\\.]{2,3}", "").trim().length() < 1) {
			for (int i = 0; i < numberPeriod.length; i++) {
				if (alphaNumber.equals(numberPeriod[i]) && n == i) {
					return paraNumber = (i + 1);
				}
			}
		}
		// to add new para# types - copy 1 section above and increase by a
		// factor of 10 (i+1)*10000000
		return paraNumber;

	}

	public int convertAlphaParaNumbDefinedTerms(String alphaNumber) {
		int paraNumber = 0;
		// converts each to 1, 2, 3 ...

		String[] alphaPeriodUc = { "1", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
				"Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "1", "AA", "BB", "CC", "DD", "EE", "FF", "GG", "HH",
				"II", "JJ", "KK", "LL", "MM", "NN", "OO", "PP", "QQ", "RR", "SS", "TT", "UU", "VV", "WW", "XX", "YY",
				"ZZ" };

		for (int i = 0; i < alphaPeriodUc.length; i++) {
			if (alphaNumber.equals(alphaPeriodUc[i])) {
				return paraNumber = (i + 1);
			}
		}

		return paraNumber;
	}

	public int convertAlphaParaNumb(String alphaNumber) {
		int paraNumber = 0;
		// converts each to 1, 2, 3 ...

		String[] romanParenLc = { "(i)", "(ii)", "(iii)", "(iv)", "(v)", "(vi)", "(vii)", "(viii)", "(ix)", "(x)",
				"(xi)", "(xii)", "(xiii)", "(xiv)", "(xv)", "(xvi)", "(xvii)", "(xviii)", "(ixx)", "(xx)" };

		if (alphaNumber.replaceAll("\\([ivx]{1,5}\\)", "").trim().length() < 1) {
			for (int i = 0; i < romanParenLc.length; i++) {
				if (alphaNumber.equals(romanParenLc[i])) {
//					 System.out.println("romanParenLc alphaNumber="
//					 + alphaNumber);
					return paraNumber = (i + 1);
				}
			}
		}

		String[] romanParenUc = { "(I)", "(II)", "(III)", "(IV)", "(V)", "(VI)", "(VII)", "(VIII)", "(IX)", "(X)",
				"(XI)", "(XII)", "(XIII)", "(XIV)", "(XV)", "(XVI)", "(XVII)", "(XVIII)", "(IXX)", "(XX)" };

		if (alphaNumber.replaceAll("\\([ivx]{1,5}\\)", "").trim().length() < 1) {
			for (int i = 0; i < romanParenUc.length; i++) {
				if (alphaNumber.equals(romanParenUc[i])) {
					return paraNumber = (i + 1);
				}
			}
		}

		String[] alphaParenLc = { "(a)", "(b)", "(c)", "(d)", "(e)", "(f)", "(g)", "(h)", "(i)", "(j)", "(k)", "(l)",
				"(m)", "(n)", "(o)", "(p)", "(q)", "(r)", "(s)", "(t)", "(u)", "(v)", "(w)", "(x)", "(y)", "(z)",
				"(aa)", "(bb)", "(cc)", "(dd)", "(ee)", "(ff)", "(gg)", "(hh)", "(ii)", "(jj)", "(kk)", "(ll)", "(mm)",
				"(nn)", "(oo)", "(pp)", "(qq)", "(rr)", "(ss)", "(tt)", "(uu)", "(vv)", "(ww)", "(xx)", "(yy)",
				"(zz)" };

		// v[5,22],i[1,9],x[10,24],ii[2,35],[20,50] <=Uc/Lc
		if (alphaNumber.replaceAll("\\([a-z]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenLc.length; i++) {
				if (alphaNumber.equals(alphaParenLc[i])) {
					return paraNumber = (i + 1);
				}
			}
		}

		String[] alphaParenUc = { "(A)", "(B)", "(C)", "(D)", "(E)", "(F)", "(G)", "(H)", "(I)", "(J)", "(K)", "(L)",
				"(M)", "(N)", "(O)", "(P)", "(Q)", "(R)", "(S)", "(T)", "(U)", "(V)", "(W)", "(X)", "(Y)", "(Z)",
				"(AA)", "(BB)", "(CC)", "(DD)", "(EE)", "(FF)", "(GG)", "(HH)", "(II)", "(JJ)", "(KK)", "(LL)", "(MM)",
				"(NN)", "(OO)", "(PP)", "(QQ)", "(RR)", "(SS)", "(TT)", "(UU)", "(VV)", "(WW)", "(XX)", "(YY)",
				"(ZZ)" };

		if (alphaNumber.replaceAll("\\([A-Z]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenUc.length; i++) {
				if (alphaNumber.equals(alphaParenUc[i])) {
					return paraNumber = (i + 1);
				}
			}
		}

		String[] alphaPeriodUc = { "A.", "B.", "C.", "D.", "E.", "F.", "G.", "H.", "I.", "J.", "K.", "L.", "M.", "N.",
				"O.", "P.", "Q.", "R.", "S.", "T.", "U.", "V.", "W.", "X.", "Y.", "Z.", "AA.", "BB.", "CC.", "DD.",
				"EE.", "FF.", "GG.", "HH.", "II.", "JJ.", "KK.", "LL.", "MM.", "NN.", "OO.", "PP.", "QQ.", "RR.", "SS.",
				"TT.", "UU.", "VV.", "WW.", "XX.", "YY.", "ZZ." };

		if (alphaNumber.replaceAll("[A\\.]{2,3}", "").trim().length() < 1) {
			for (int i = 0; i < alphaPeriodUc.length; i++) {
				if (alphaNumber.equals(alphaPeriodUc[i])) {
					return paraNumber = (i + 1);
				}
			}
		}

		String[] numberParen = { "(1)", "(2)", "(3)", "(4)", "(5)", "(6)", "(7)", "(8)", "(9)", "(10)", "(11)", "(12)",
				"(13)", "(14)", "(15)", "(16)", "(17)", "(18)", "(19)", "(20)", "(21)", "(22)", "(23)", "(24)", "(25)",
				"(26)", "(27)", "(28)", "(29)", "(30)", "(31)", "(32)", "(33)", "(34)", "(35)", "(36)", "(37)", "(38)",
				"(39)", "(40)", "(41)", "(42)", "(43)", "(44)", "(45)", "(46)", "(47)", "(48)", "(49)", "(50)", "(51)",
				"(52)", "(53)", "(54)", "(55)", "(56)", "(57)", "(58)", "(59)", "(60)", "(61)", "(62)", "(63)", "(64)",
				"(65)", "(66)", "(67)", "(68)", "(69)", "(70)", "(71)", "(72)", "(73)", "(74)", "(75)", "(76)", "(77)",
				"(78)", "(79)", "(80)", "(81)", "(82)", "(83)", "(84)", "(85)", "(86)", "(87)", "(88)", "(89)", "(90)",
				"(91)", "(92)", "(93)", "(94)", "(95)", "(96)", "(97)", "(98)", "(99)" };

		if (alphaNumber.replaceAll("\\([0-9]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < numberParen.length; i++) {
				if (alphaNumber.equals(numberParen[i])) {
					return paraNumber = (i + 1);
				}
			}
		}

		String[] numberPeriod = { "1.", "2.", "3.", "4.", "5.", "6.", "7.", "8.", "9.", "10.", "11.", "12.", "13.",
				"14.", "15.", "16.", "17.", "18.", "19.", "20.", "21.", "22.", "23.", "24.", "25.", "26.", "27.", "28.",
				"29.", "30.", "31.", "32.", "33.", "34.", "35.", "36.", "37.", "38.", "39.", "40.", "41.", "42.", "43.",
				"44.", "45.", "46.", "47.", "48.", "49.", "50.", "51.", "52.", "53.", "54.", "55.", "56.", "57.", "58.",
				"59.", "60.", "61.", "62.", "63.", "64.", "65.", "66.", "67.", "68.", "69.", "70.", "71.", "72.", "73.",
				"74.", "75.", "76.", "77.", "78.", "79.", "80.", "81.", "82.", "83.", "84.", "85.", "86.", "87.", "88.",
				"89.", "90.", "91.", "92.", "93.", "94.", "95.", "96.", "97.", "98.", "99." };

		if (alphaNumber.replaceAll("[0-9\\.]{2,3}", "").trim().length() < 1) {
			for (int i = 0; i < numberPeriod.length; i++) {
				if (alphaNumber.equals(numberPeriod[i])) {
					return paraNumber = (i + 1);
				}
			}
		}
		// to add new para# types - copy 1 section above and increase by a
		// factor of 10 (i+1)*10000000
		return paraNumber;
	}

	public static int convertExhibitAlphaParaNumb(String alphaNumber) {
		int paraNumber = 0;
		// converts each to 1, 2, 3 ...

		String[] alphaPeriodUc = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q",
				"R", "S", "T", "U", "V", "W", "X", "Y", "Z" };

		if (alphaNumber.replaceAll("[A-Z]{1,2}", "").trim().length() == 0) {
			for (int i = 0; i < alphaPeriodUc.length; i++) {
				if (alphaNumber.equals(alphaPeriodUc[i])) {
					return (i + 1);
				}
			}
		}

		String[] alphaDblPeriodUc = { "AA", "BB", "CC", "DD", "EE", "FF", "GG", "HH", "II", "JJ", "KK", "LL", "MM",
				"NN", "OO", "PP", "QQ", "RR", "SS", "TT", "UU", "VV", "WW", "XX", "YY", "ZZ" };

		if (alphaNumber.replaceAll("[A-Z]{1,2}", "").trim().length() == 0) {
			for (int i = 0; i < alphaDblPeriodUc.length; i++) {
				if (alphaNumber.equals(alphaDblPeriodUc[i])) {
					return (i + 1);
				}
			}
		}

		String[] numberPeriod = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15" };
		if (alphaNumber.replaceAll("[0-9]{1,2}", "").trim().length() == 0) {
			for (int i = 0; i < numberPeriod.length; i++) {
				if (alphaNumber.equals(numberPeriod[i])) {
					return (i + 1);
				}
			}
		}

		// to add new para# types - copy 1 section above and increase by a
		// factor of 10 (i+1)*10000000
		return paraNumber;
	}

	public int[] convertAlphaParaNumbAndType(String alphaNumber, boolean matchType, String typeMatch)
			throws IOException {
//		System.out.println("alphaNumber="+alphaNumber);
		int paraNumber = 0;
		// converts each to 1, 2, 3 ...

		// System.out.println("convert alphNumber="+alphaNumber);
		String[] romanParenLc = { "(i)", "(ii)", "(iii)", "(iv)", "(v)", "(vi)", "(vii)", "(viii)", "(ix)", "(x)",
				"(xi)", "(xii)", "(xiii)", "(xiv)", "(xv)", "(xvi)", "(xvii)", "(xviii)", "(xix)", "(xx)", "(xxi)",
				"(xxii)", "(xxiii)", "(xxiv)", "(xxv)", "(xxvi)", "(xxvii)", "(xxviii)", "(xxix)", "(xxx)", "(xxxi)",
				"(xxxii)", "(xxxiii)", "(xxxiv)", "(xxxv)", "(xxxvi)", "(xxxvii)", "(xxxviii)", "(xxxix)", "(xl)",
				"(xli)", "(xlii)", "(xliii)", "(xliv)", "(xlv)", "(xlvi)", "(xlvii)", "(xlviii)", "(xlix)", "(l)",
				"(li)", "(lii)", "(liii)", "(liv)", "(lv)", "(lvi)", "(lvii)", "(lviii)", "(lix)", "(lx)", "(lxi)",
				"(lxii)", "(lxiii)", "(lxiv)", "(lxv)", "(lxvi)", "(lxvii)", "(lxviii)", "(lxix)", "(lxx)" };
		int type = 0;
		if (alphaNumber.replaceAll("\\([ivx]{1,5}\\)", "").trim().length() < 1) {
			for (int i = 0; i < romanParenLc.length; i++) {
				if (alphaNumber.equals(romanParenLc[i])) {
//					 System.out.println("aa romanParenLc alphaNumber="
//					 + alphaNumber);
					int[] aryInt = { (i + 1), type };
					return aryInt;
				}
			}
		}

		String[] romanParenUc = { "(I)", "(II)", "(III)", "(IV)", "(V)", "(VI)", "(VII)", "(VIII)", "(IX)", "(X)",
				"(XI)", "(XII)", "(XIII)", "(XIV)", "(XV)", "(XVI)", "(XVII)", "(XVIII)", "(IXX)", "(XX)" };
		type++;
		if (alphaNumber.replaceAll("\\([IVX]{1,5}\\)", "").trim().length() < 1) {
			for (int i = 0; i < romanParenUc.length; i++) {
				if (alphaNumber.equals(romanParenUc[i])) {
//					 System.out.println("bb romanParenLc alphaNumber="
//					 + alphaNumber+
//					 " i+1="+(i+1)
//					 +" type="+type);
					int[] aryInt = { (i + 1), type };
					return aryInt;
				}
			}
		}

		type++;
		String[] alphaParenLc = { "(a)", "(b)", "(c)", "(d)", "(e)", "(f)", "(g)", "(h)", "(i)", "(j)", "(k)", "(l)",
				"(m)", "(n)", "(o)", "(p)", "(q)", "(r)", "(s)", "(t)", "(u)", "(v)", "(w)", "(x)", "(y)", "(z)",
				"(aa)", "(bb)", "(cc)", "(dd)", "(ee)", "(ff)", "(gg)", "(hh)", "(ii)", "(jj)", "(kk)", "(ll)", "(mm)",
				"(nn)", "(oo)", "(pp)", "(qq)", "(rr)", "(ss)", "(tt)", "(uu)", "(vv)", "(ww)", "(xx)", "(yy)",
				"(zz)" };
		// v[5,22],i[1,9],x[10,24],ii[2,35],[20,50] <=Uc/Lc
		if (alphaNumber.replaceAll("\\([a-z]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenLc.length; i++) {
				if (alphaNumber.equals(alphaParenLc[i])) {
					int[] aryInt = { (i + 1), type };
					return aryInt;
				}
			}
		}

		type++;
		String[] alphaParenUc = { "(A)", "(B)", "(C)", "(D)", "(E)", "(F)", "(G)", "(H)", "(I)", "(J)", "(K)", "(L)",
				"(M)", "(N)", "(O)", "(P)", "(Q)", "(R)", "(S)", "(T)", "(U)", "(V)", "(W)", "(X)", "(Y)", "(Z)",
				"(AA)", "(BB)", "(CC)", "(DD)", "(EE)", "(FF)", "(GG)", "(HH)", "(II)", "(JJ)", "(KK)", "(LL)", "(MM)",
				"(NN)", "(OO)", "(PP)", "(QQ)", "(RR)", "(SS)", "(TT)", "(UU)", "(VV)", "(WW)", "(XX)", "(YY)",
				"(ZZ)" };

		if (alphaNumber.replaceAll("\\([A-Z]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenUc.length; i++) {
				if (alphaNumber.equals(alphaParenUc[i])) {
					int[] aryInt = { (i + 1), type };
					return aryInt;
				}
			}
		}

		type++;
		String[] alphaPeriodUc = { "a.", "b.", "c.", "d.", "e.", "f.", "g.", "h.", "i.", "j.", "k.", "l.", "m.", "n.",
				"o.", "p.", "q.", "r.", "s.", "t.", "u.", "v.", "w.", "x.", "y.", "z.", "aa.", "bb.", "cc.", "dd.",
				"ee.", "ff.", "gg.", "hh.", "ii.", "jj.", "kk.", "ll.", "mm.", "nn.", "oo.", "pp.", "qq.", "rr.", "ss.",
				"tt.", "uu.", "vv.", "ww.", "xx.", "yy.", "zz." };

		if (alphaNumber.replaceAll("[a-z]{1,2}\\.", "").trim().length() < 1) {
			for (int i = 0; i < alphaPeriodUc.length; i++) {
				if (alphaNumber.equals(alphaPeriodUc[i])) {
					int[] aryInt = { (i + 1), type };
					return aryInt;
				}
			}
		}

		type++;
		String[] numberParen = { "(1)", "(2)", "(3)", "(4)", "(5)", "(6)", "(7)", "(8)", "(9)", "(10)", "(11)", "(12)",
				"(13)", "(14)", "(15)", "(16)", "(17)", "(18)", "(19)", "(20)", "(21)", "(22)", "(23)", "(24)", "(25)",
				"(26)", "(27)", "(28)", "(29)", "(30)", "(31)", "(32)", "(33)", "(34)", "(35)", "(36)", "(37)", "(38)",
				"(39)", "(40)", "(41)", "(42)", "(43)", "(44)", "(45)", "(46)", "(47)", "(48)", "(49)", "(50)", "(51)",
				"(52)", "(53)", "(54)", "(55)", "(56)", "(57)", "(58)", "(59)", "(60)", "(61)", "(62)", "(63)", "(64)",
				"(65)", "(66)", "(67)", "(68)", "(69)", "(70)", "(71)", "(72)", "(73)", "(74)", "(75)", "(76)", "(77)",
				"(78)", "(79)", "(80)", "(81)", "(82)", "(83)", "(84)", "(85)", "(86)", "(87)", "(88)", "(89)", "(90)",
				"(91)", "(92)", "(93)", "(94)", "(95)", "(96)", "(97)", "(98)", "(99)" };

		if (alphaNumber.replaceAll("\\([0-9]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < numberParen.length; i++) {
				if (alphaNumber.equals(numberParen[i])) {
					int[] aryInt = { (i + 1), type };
					return aryInt;
				}
			}
		}

		type++;
		String[] numberPeriod = { "1.", "2.", "3.", "4.", "5.", "6.", "7.", "8.", "9.", "10.", "11.", "12.", "13.",
				"14.", "15.", "16.", "17.", "18.", "19.", "20.", "21.", "22.", "23.", "24.", "25.", "26.", "27.", "28.",
				"29.", "30.", "31.", "32.", "33.", "34.", "35.", "36.", "37.", "38.", "39.", "40.", "41.", "42.", "43.",
				"44.", "45.", "46.", "47.", "48.", "49.", "50.", "51.", "52.", "53.", "54.", "55.", "56.", "57.", "58.",
				"59.", "60.", "61.", "62.", "63.", "64.", "65.", "66.", "67.", "68.", "69.", "70.", "71.", "72.", "73.",
				"74.", "75.", "76.", "77.", "78.", "79.", "80.", "81.", "82.", "83.", "84.", "85.", "86.", "87.", "88.",
				"89.", "90.", "91.", "92.", "93.", "94.", "95.", "96.", "97.", "98.", "99." };

		if (alphaNumber.replaceAll("[0-9]{1,2}\\.", "").trim().length() < 1) {
			for (int i = 0; i < numberPeriod.length; i++) {
				if (alphaNumber.equals(numberPeriod[i])) {
					int[] aryInt = { (i + 1), type };
					return aryInt;
				}
			}
		}

		type++;
		String[] alphaParenUcP = { "A.", "B.", "C.", "D.", "E.", "F.", "G.", "H.", "I.", "J.", "K.", "L.", "M.", "N.",
				"O.", "P.", "Q.", "R.", "S.", "T.", "U.", "V.", "W.", "X.", "Y.", "Z.", "AA.", "BB.", "CC.", "DD.",
				"EE.", "FF.", "GG.", "HH.", "II.", "JJ.", "KK.", "LL.", "MM.", "NN.", "OO.", "PP.", "QQ.", "RR.", "SS.",
				"TT.", "UU.", "VV.", "WW.", "XX.", "YY.", "ZZ." };

		if (alphaNumber.replaceAll("[A-Z]{1,2}\\.", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenUcP.length; i++) {
				if (alphaNumber.equals(alphaParenUcP[i])) {
					int[] aryInt = { (i + 1), type };
					return aryInt;
				}
			}
		}

		type++;
		String[] alphaParenLcP = { "i.", "ii.", "iii.", "iv.", "v.", "vi.", "vii.", "viii.", "ix.", "x.", "xi.", "xii.",
				"xiii.", "xiv.", "xv.", "xvi.", "xvii.", "xviii.", "xix.", "xx.", "xxi.", "xxii.", "xxiii.", "xxiv.",
				"xxv.", "xxvi.", "xxvii.", "xxviii.", "xxix.", "xxx.", "xxxi.", "xxxii.", "xxxiii.", "xxxiv.", "xxxv.",
				"xxxvi.", "xxxvii.", "xxxviii.", "xxxix.", "xl.", "xli.", "xlii.", "xliii.", "xliv.", "xlv.", "xlvi.",
				"xlvii.", "xlviii.", "xlix.", "l.", "li.", "lii.", "liii.", "liv.", "lv.", "lvi.", "lvii.", "lviii.",
				"lix.", "lx.", "lxi.", "lxii.", "lxiii.", "lxiv.", "lxv.", "lxvi.", "lxvii.", "lxviii.", "lxix.",
				"lxx." };

		if (alphaNumber.replaceAll("[a-z]{1,7}\\.", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenLcP.length; i++) {
				if (alphaNumber.equals(alphaParenLcP[i])) {
					int[] aryInt = { (i + 1), type };
					return aryInt;
				}
			}
		}

		type++;
		String[] alphaParenLPar = { "i)", "ii)", "iii)", "iv)", "v)", "vi)", "vii)", "viii)", "ix)", "x)", "xi)",
				"xii)", "xiii)", "xiv)", "xv)", "xvi)", "xvii)", "xviii)", "xix)", "xx)", "xxi)", "xxii)", "xxiii)",
				"xxiv)", "xxv)", "xxvi)", "xxvii)", "xxviii)", "xxix)", "xxx)", "xxxi)", "xxxii)", "xxxiii)", "xxxiv)",
				"xxxv)", "xxxvi)", "xxxvii)", "xxxviii)", "xxxix)", "xl)", "xli)", "xlii)", "xliii)", "xliv)", "xlv)",
				"xlvi)", "xlvii)", "xlviii)", "xlix)", "l)", "li)", "lii)", "liii)", "liv)", "lv)", "lvi)", "lvii)",
				"lviii)", "lix)", "lx)", "lxi)", "lxii)", "lxiii)", "lxiv)", "lxv)", "lxvi)", "lxvii)", "lxviii)",
				"lxix)", "lxx)" };

		if (alphaNumber.replaceAll("[a-z]{1,7}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenLPar.length; i++) {
				if (alphaNumber.equals(alphaParenLPar[i])) {
					int[] aryInt = { (i + 1), type };
					return aryInt;
				}
			}
		}

		type++;
		String[] alphaParLc = { "a)", "b)", "c)", "d)", "e)", "f)", "g)", "h)", "i)", "j)", "k)", "l)", "m)", "n)",
				"o)", "p)", "q)", "r)", "s)", "t)", "u)", "v)", "w)", "x)", "y)", "z)", "aa)", "bb)", "cc)", "dd)",
				"ee)", "ff)", "gg)", "hh)", "ii)", "jj)", "kk)", "ll)", "mm)", "nn)", "oo)", "pp)", "qq)", "rr)", "ss)",
				"tt)", "uu)", "vv)", "ww)", "xx)", "yy)", "zz)" };

		if (alphaNumber.replaceAll("[a-z]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParLc.length; i++) {
				if (alphaNumber.equals(alphaParLc[i])) {
					int[] aryInt = { (i + 1), type };
					return aryInt;
				}
			}
		}

		type++;
		String[] numberParens = { "1)", "2)", "3)", "4)", "5)", "6)", "7)", "8)", "9)", "10)", "11)", "12)", "13)",
				"14)", "15)", "16)", "17)", "18)", "19)", "20)", "21)", "22)", "23)", "24)", "25)", "26)", "27)", "28)",
				"29)", "30)", "31)", "32)", "33)", "34)", "35)", "36)", "37)", "38)", "39)", "40)", "41)", "42)", "43)",
				"44)", "45)", "46)", "47)", "48)", "49)", "50)", "51)", "52)", "53)", "54)", "55)", "56)", "57)", "58)",
				"59)", "60)", "61)", "62)", "63)", "64)", "65)", "66)", "67)", "68)", "69)", "70)", "71)", "72)", "73)",
				"74)", "75)", "76)", "77)", "78)", "79)", "80)", "81)", "82)", "83)", "84)", "85)", "86)", "87)", "88)",
				"89)", "90)", "91)", "92)", "93)", "94)", "95)", "96)", "97)", "98)", "99)" };

		if (alphaNumber.replaceAll("[0-9]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < numberParens.length; i++) {
				if (alphaNumber.equals(numberParens[i])) {
					int[] aryInt = { (i + 1), type };
					return aryInt;
				}
			}
		}

		type++;
		String[] alphaUcParen = { "A)", "B)", "C)", "D)", "E)", "F)", "G)", "H)", "I)", "J)", "K)", "L)", "M)", "N)",
				"O)", "P)", "Q)", "R)", "S)", "T)", "U)", "V)", "W)", "X)", "Y)", "Z)", "AA)", "BB)", "CC)", "DD)",
				"EE)", "FF)", "GG)", "HH)", "II)", "JJ)", "KK)", "LL)", "MM)", "NN)", "OO)", "PP)", "QQ)", "RR)", "SS)",
				"TT)", "UU)", "VV)", "WW)", "XX)", "YY)", "ZZ)" };

		if (alphaNumber.replaceAll("[A-Z]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaUcParen.length; i++) {
				if (alphaNumber.equals(alphaUcParen[i])) {
					int[] aryInt = { (i + 1), type };
					return aryInt;
				}
			}
		}

		type++;
		String[] alphaParenUPar = { "I)", "II)", "III)", "IV)", "V)", "VI)", "VII)", "VIII)", "IX)", "X)", "XI)",
				"XII)", "XIII)", "XIV)", "XV)", "XVI)", "XVII)", "XVIII)", "XIX)", "XX)", "XXI)", "XXII)", "XXIII)",
				"XXIV)", "XXV)", "XXVI)", "XXVII)", "XXVIII)", "XXIX)", "XXX)", "XXXI)", "XXXII)", "XXXIII)", "XXXIV)",
				"XXXV)", "XXXVI)", "XXXVII)", "XXXVIII)", "XXXIX)", "XL)", "XLI)", "XLII)", "XLIII)", "XLIV)", "XLV)",
				"XLVI)", "XLVII)", "XLVIII)", "XLIX)", "L)", "LI)", "LII)", "LIII)", "LIV)", "LV)", "LVI)", "LVII)",
				"LVIII)", "LIX)", "LX)", "LXI)", "LXII)", "LXIII)", "LXIV)", "LXV)", "LXVI)", "LXVII)", "LXVIII)",
				"LXIX)", "LXX)" };

		if (alphaNumber.replaceAll("[A-Z]{1,7}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenUPar.length; i++) {
				if (alphaNumber.equals(alphaParenUPar[i])) {
					int[] aryInt = { (i + 1), type };
					return aryInt;
				}
			}
		}

		type++;
		String[] alphaRom = { "I.", "II.", "III.", "IV.", "V.", "VI.", "VII.", "VIII.", "IX.", "X.", "XI.", "XII.",
				"XIII.", "XIV.", "XV.", "XVI.", "XVII.", "XVIII.", "XIX.", "XX.", "XXI.", "XXII.", "XXIII.", "XXIV.",
				"XXV.", "XXVI.", "XXVII.", "XXVIII.", "XXIX.", "XXX.", "XXXI.", "XXXII.", "XXXIII.", "XXXIV.", "XXXV.",
				"XXXVI.", "XXXVII.", "XXXVIII.", "XXXIX.", "XL.", "XLI.", "XLII.", "XLIII.", "XLIV.", "XLV.", "XLVI.",
				"XLVII.", "XLVIII.", "XLIX.", "L.", "LI.", "LII.", "LIII.", "LIV.", "LV.", "LVI.", "LVII.", "LVIII.",
				"LIX.", "LX.", "LXI.", "LXII.", "LXIII.", "LXIV.", "LXV.", "LXVI.", "LXVII.", "LXVIII.", "LXIX.",
				"LXX." };

		if (alphaNumber.replaceAll("[A-Z]{1,7}\\.", "").trim().length() < 1) {
			for (int i = 0; i < alphaRom.length; i++) {
				if (alphaNumber.equals(alphaRom[i])) {
					int[] aryInt = { (i + 1), type };
					return aryInt;
				}
			}
		}

		type++;
		int[] aryInt = { (1), type };
		return aryInt;

	}

	public static List<Integer[]> convertAlphaParaNumbAndTypeList(String alphaNumber) throws IOException {

//		System.out.println("alphaNumber="+alphaNumber);
		// converts each to 1, 2, 3 ... and returns each value and type (eg (x) has 2
		// types and 2 values - eg val=10 (ix,x) or 24 (x,y,z)
		List<Integer[]> list = new ArrayList<Integer[]>();

		// System.out.println("convert alphNumber="+alphaNumber);
		String[] romanParenLc = { "(i)", "(ii)", "(iii)", "(iv)", "(v)", "(vi)", "(vii)", "(viii)", "(ix)", "(x)",
				"(xi)", "(xii)", "(xiii)", "(xiv)", "(xv)", "(xvi)", "(xvii)", "(xviii)", "(xix)", "(xx)", "(xxi)",
				"(xxii)", "(xxiii)", "(xxiv)", "(xxv)", "(xxvi)", "(xxvii)", "(xxviii)", "(xxix)", "(xxx)", "(xxxi)",
				"(xxxii)", "(xxxiii)", "(xxxiv)", "(xxxv)", "(xxxvi)", "(xxxvii)", "(xxxviii)", "(xxxix)", "(xl)",
				"(xli)", "(xlii)", "(xliii)", "(xliv)", "(xlv)", "(xlvi)", "(xlvii)", "(xlviii)", "(xlix)", "(l)",
				"(li)", "(lii)", "(liii)", "(liv)", "(lv)", "(lvi)", "(lvii)", "(lviii)", "(lix)", "(lx)", "(lxi)",
				"(lxii)", "(lxiii)", "(lxiv)", "(lxv)", "(lxvi)", "(lxvii)", "(lxviii)", "(lxix)", "(lxx)" };
		int type = 0;
		if (alphaNumber.replaceAll("\\([ivx]{1,5}\\)", "").trim().length() < 1) {
			for (int i = 0; i < romanParenLc.length; i++) {
				if (alphaNumber.equals(romanParenLc[i])) {
//					 System.out.println("aa romanParenLc alphaNumber="
//					 + alphaNumber);
					Integer[] aryInt = { (i + 1), type };
					list.add(aryInt);
					break;
				}
			}
		}

		String[] romanParenUc = { "(I)", "(II)", "(III)", "(IV)", "(V)", "(VI)", "(VII)", "(VIII)", "(IX)", "(X)",
				"(XI)", "(XII)", "(XIII)", "(XIV)", "(XV)", "(XVI)", "(XVII)", "(XVIII)", "(IXX)", "(XX)" };
		type++;
		if (alphaNumber.replaceAll("\\([IVX]{1,5}\\)", "").trim().length() < 1) {
			for (int i = 0; i < romanParenUc.length; i++) {
				if (alphaNumber.equals(romanParenUc[i])) {
//					 System.out.println("bb romanParenLc alphaNumber="
//					 + alphaNumber+
//					 " i+1="+(i+1)
//					 +" type="+type);
					Integer[] aryInt = { (i + 1), type };
					list.add(aryInt);
					break;
				}
			}
		}

		type++;
		String[] alphaParenLc = { "(a)", "(b)", "(c)", "(d)", "(e)", "(f)", "(g)", "(h)", "(i)", "(j)", "(k)", "(l)",
				"(m)", "(n)", "(o)", "(p)", "(q)", "(r)", "(s)", "(t)", "(u)", "(v)", "(w)", "(x)", "(y)", "(z)",
				"(aa)", "(bb)", "(cc)", "(dd)", "(ee)", "(ff)", "(gg)", "(hh)", "(ii)", "(jj)", "(kk)", "(ll)", "(mm)",
				"(nn)", "(oo)", "(pp)", "(qq)", "(rr)", "(ss)", "(tt)", "(uu)", "(vv)", "(ww)", "(xx)", "(yy)",
				"(zz)" };
		// v[5,22],i[1,9],x[10,24],ii[2,35],[20,50] <=Uc/Lc
		if (alphaNumber.replaceAll("\\([a-z]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenLc.length; i++) {
				if (alphaNumber.equals(alphaParenLc[i])) {
					Integer[] aryInt = { (i + 1), type };
					list.add(aryInt);
					break;
				}
			}
		}

		type++;
		String[] alphaParenUc = { "(A)", "(B)", "(C)", "(D)", "(E)", "(F)", "(G)", "(H)", "(I)", "(J)", "(K)", "(L)",
				"(M)", "(N)", "(O)", "(P)", "(Q)", "(R)", "(S)", "(T)", "(U)", "(V)", "(W)", "(X)", "(Y)", "(Z)",
				"(AA)", "(BB)", "(CC)", "(DD)", "(EE)", "(FF)", "(GG)", "(HH)", "(II)", "(JJ)", "(KK)", "(LL)", "(MM)",
				"(NN)", "(OO)", "(PP)", "(QQ)", "(RR)", "(SS)", "(TT)", "(UU)", "(VV)", "(WW)", "(XX)", "(YY)",
				"(ZZ)" };

		if (alphaNumber.replaceAll("\\([A-Z]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenUc.length; i++) {
				if (alphaNumber.equals(alphaParenUc[i])) {
					Integer[] aryInt = { (i + 1), type };
					list.add(aryInt);
					break;
				}
			}
		}

		type++;
		String[] alphaPeriodUc = { "a.", "b.", "c.", "d.", "e.", "f.", "g.", "h.", "i.", "j.", "k.", "l.", "m.", "n.",
				"o.", "p.", "q.", "r.", "s.", "t.", "u.", "v.", "w.", "x.", "y.", "z.", "aa.", "bb.", "cc.", "dd.",
				"ee.", "ff.", "gg.", "hh.", "ii.", "jj.", "kk.", "ll.", "mm.", "nn.", "oo.", "pp.", "qq.", "rr.", "ss.",
				"tt.", "uu.", "vv.", "ww.", "xx.", "yy.", "zz." };

		if (alphaNumber.replaceAll("[a-z]{1,2}\\.", "").trim().length() < 1) {
			for (int i = 0; i < alphaPeriodUc.length; i++) {
				if (alphaNumber.equals(alphaPeriodUc[i])) {
					Integer[] aryInt = { (i + 1), type };
					list.add(aryInt);
					break;
				}
			}
		}

		type++;
		String[] numberParen = { "(1)", "(2)", "(3)", "(4)", "(5)", "(6)", "(7)", "(8)", "(9)", "(10)", "(11)", "(12)",
				"(13)", "(14)", "(15)", "(16)", "(17)", "(18)", "(19)", "(20)", "(21)", "(22)", "(23)", "(24)", "(25)",
				"(26)", "(27)", "(28)", "(29)", "(30)", "(31)", "(32)", "(33)", "(34)", "(35)", "(36)", "(37)", "(38)",
				"(39)", "(40)", "(41)", "(42)", "(43)", "(44)", "(45)", "(46)", "(47)", "(48)", "(49)", "(50)", "(51)",
				"(52)", "(53)", "(54)", "(55)", "(56)", "(57)", "(58)", "(59)", "(60)", "(61)", "(62)", "(63)", "(64)",
				"(65)", "(66)", "(67)", "(68)", "(69)", "(70)", "(71)", "(72)", "(73)", "(74)", "(75)", "(76)", "(77)",
				"(78)", "(79)", "(80)", "(81)", "(82)", "(83)", "(84)", "(85)", "(86)", "(87)", "(88)", "(89)", "(90)",
				"(91)", "(92)", "(93)", "(94)", "(95)", "(96)", "(97)", "(98)", "(99)" };

		if (alphaNumber.replaceAll("\\([0-9]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < numberParen.length; i++) {
				if (alphaNumber.equals(numberParen[i])) {
					Integer[] aryInt = { (i + 1), type };
					list.add(aryInt);
					break;
				}
			}
		}

		type++;
		String[] numberPeriod = { "1.", "2.", "3.", "4.", "5.", "6.", "7.", "8.", "9.", "10.", "11.", "12.", "13.",
				"14.", "15.", "16.", "17.", "18.", "19.", "20.", "21.", "22.", "23.", "24.", "25.", "26.", "27.", "28.",
				"29.", "30.", "31.", "32.", "33.", "34.", "35.", "36.", "37.", "38.", "39.", "40.", "41.", "42.", "43.",
				"44.", "45.", "46.", "47.", "48.", "49.", "50.", "51.", "52.", "53.", "54.", "55.", "56.", "57.", "58.",
				"59.", "60.", "61.", "62.", "63.", "64.", "65.", "66.", "67.", "68.", "69.", "70.", "71.", "72.", "73.",
				"74.", "75.", "76.", "77.", "78.", "79.", "80.", "81.", "82.", "83.", "84.", "85.", "86.", "87.", "88.",
				"89.", "90.", "91.", "92.", "93.", "94.", "95.", "96.", "97.", "98.", "99." };

		if (alphaNumber.replaceAll("[0-9]{1,2}\\.", "").trim().length() < 1) {
			for (int i = 0; i < numberPeriod.length; i++) {
				if (alphaNumber.equals(numberPeriod[i])) {
					Integer[] aryInt = { (i + 1), type };
					list.add(aryInt);
					break;
				}
			}
		}

		type++;
		String[] alphaParenUcP = { "A.", "B.", "C.", "D.", "E.", "F.", "G.", "H.", "I.", "J.", "K.", "L.", "M.", "N.",
				"O.", "P.", "Q.", "R.", "S.", "T.", "U.", "V.", "W.", "X.", "Y.", "Z.", "AA.", "BB.", "CC.", "DD.",
				"EE.", "FF.", "GG.", "HH.", "II.", "JJ.", "KK.", "LL.", "MM.", "NN.", "OO.", "PP.", "QQ.", "RR.", "SS.",
				"TT.", "UU.", "VV.", "WW.", "XX.", "YY.", "ZZ." };

		if (alphaNumber.replaceAll("[A-Z]{1,2}\\.", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenUcP.length; i++) {
				if (alphaNumber.equals(alphaParenUcP[i])) {
					Integer[] aryInt = { (i + 1), type };
					list.add(aryInt);
					break;
				}
			}
		}

		type++;
		String[] alphaParenLcP = { "i.", "ii.", "iii.", "iv.", "v.", "vi.", "vii.", "viii.", "ix.", "x.", "xi.", "xii.",
				"xiii.", "xiv.", "xv.", "xvi.", "xvii.", "xviii.", "xix.", "xx.", "xxi.", "xxii.", "xxiii.", "xxiv.",
				"xxv.", "xxvi.", "xxvii.", "xxviii.", "xxix.", "xxx.", "xxxi.", "xxxii.", "xxxiii.", "xxxiv.", "xxxv.",
				"xxxvi.", "xxxvii.", "xxxviii.", "xxxix.", "xl.", "xli.", "xlii.", "xliii.", "xliv.", "xlv.", "xlvi.",
				"xlvii.", "xlviii.", "xlix.", "l.", "li.", "lii.", "liii.", "liv.", "lv.", "lvi.", "lvii.", "lviii.",
				"lix.", "lx.", "lxi.", "lxii.", "lxiii.", "lxiv.", "lxv.", "lxvi.", "lxvii.", "lxviii.", "lxix.",
				"lxx." };

		if (alphaNumber.replaceAll("[a-z]{1,7}\\.", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenLcP.length; i++) {
				if (alphaNumber.equals(alphaParenLcP[i])) {
					Integer[] aryInt = { (i + 1), type };
					list.add(aryInt);
					break;
				}
			}
		}

		type++;
		String[] alphaParenLPar = { "i)", "ii)", "iii)", "iv)", "v)", "vi)", "vii)", "viii)", "ix)", "x)", "xi)",
				"xii)", "xiii)", "xiv)", "xv)", "xvi)", "xvii)", "xviii)", "xix)", "xx)", "xxi)", "xxii)", "xxiii)",
				"xxiv)", "xxv)", "xxvi)", "xxvii)", "xxviii)", "xxix)", "xxx)", "xxxi)", "xxxii)", "xxxiii)", "xxxiv)",
				"xxxv)", "xxxvi)", "xxxvii)", "xxxviii)", "xxxix)", "xl)", "xli)", "xlii)", "xliii)", "xliv)", "xlv)",
				"xlvi)", "xlvii)", "xlviii)", "xlix)", "l)", "li)", "lii)", "liii)", "liv)", "lv)", "lvi)", "lvii)",
				"lviii)", "lix)", "lx)", "lxi)", "lxii)", "lxiii)", "lxiv)", "lxv)", "lxvi)", "lxvii)", "lxviii)",
				"lxix)", "lxx)" };

		if (alphaNumber.replaceAll("[a-z]{1,7}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenLPar.length; i++) {
				if (alphaNumber.equals(alphaParenLPar[i])) {
					Integer[] aryInt = { (i + 1), type };
					list.add(aryInt);
					break;
				}
			}
		}

		type++;
		String[] alphaParLc = { "a)", "b)", "c)", "d)", "e)", "f)", "g)", "h)", "i)", "j)", "k)", "l)", "m)", "n)",
				"o)", "p)", "q)", "r)", "s)", "t)", "u)", "v)", "w)", "x)", "y)", "z)", "aa)", "bb)", "cc)", "dd)",
				"ee)", "ff)", "gg)", "hh)", "ii)", "jj)", "kk)", "ll)", "mm)", "nn)", "oo)", "pp)", "qq)", "rr)", "ss)",
				"tt)", "uu)", "vv)", "ww)", "xx)", "yy)", "zz)" };

		if (alphaNumber.replaceAll("[a-z]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParLc.length; i++) {
				if (alphaNumber.equals(alphaParLc[i])) {
					Integer[] aryInt = { (i + 1), type };
					list.add(aryInt);
					break;
				}
			}
		}

		type++;
		String[] numberParens = { "1)", "2)", "3)", "4)", "5)", "6)", "7)", "8)", "9)", "10)", "11)", "12)", "13)",
				"14)", "15)", "16)", "17)", "18)", "19)", "20)", "21)", "22)", "23)", "24)", "25)", "26)", "27)", "28)",
				"29)", "30)", "31)", "32)", "33)", "34)", "35)", "36)", "37)", "38)", "39)", "40)", "41)", "42)", "43)",
				"44)", "45)", "46)", "47)", "48)", "49)", "50)", "51)", "52)", "53)", "54)", "55)", "56)", "57)", "58)",
				"59)", "60)", "61)", "62)", "63)", "64)", "65)", "66)", "67)", "68)", "69)", "70)", "71)", "72)", "73)",
				"74)", "75)", "76)", "77)", "78)", "79)", "80)", "81)", "82)", "83)", "84)", "85)", "86)", "87)", "88)",
				"89)", "90)", "91)", "92)", "93)", "94)", "95)", "96)", "97)", "98)", "99)" };

		if (alphaNumber.replaceAll("[0-9]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < numberParens.length; i++) {
				if (alphaNumber.equals(numberParens[i])) {
					Integer[] aryInt = { (i + 1), type };
					list.add(aryInt);
					break;
				}
			}
		}

		type++;
		String[] alphaUcParen = { "A)", "B)", "C)", "D)", "E)", "F)", "G)", "H)", "I)", "J)", "K)", "L)", "M)", "N)",
				"O)", "P)", "Q)", "R)", "S)", "T)", "U)", "V)", "W)", "X)", "Y)", "Z)", "AA)", "BB)", "CC)", "DD)",
				"EE)", "FF)", "GG)", "HH)", "II)", "JJ)", "KK)", "LL)", "MM)", "NN)", "OO)", "PP)", "QQ)", "RR)", "SS)",
				"TT)", "UU)", "VV)", "WW)", "XX)", "YY)", "ZZ)" };

		if (alphaNumber.replaceAll("[A-Z]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaUcParen.length; i++) {
				if (alphaNumber.equals(alphaUcParen[i])) {
					Integer[] aryInt = { (i + 1), type };
					list.add(aryInt);
					break;
				}
			}
		}

		type++;
		String[] alphaParenUPar = { "I)", "II)", "III)", "IV)", "V)", "VI)", "VII)", "VIII)", "IX)", "X)", "XI)",
				"XII)", "XIII)", "XIV)", "XV)", "XVI)", "XVII)", "XVIII)", "XIX)", "XX)", "XXI)", "XXII)", "XXIII)",
				"XXIV)", "XXV)", "XXVI)", "XXVII)", "XXVIII)", "XXIX)", "XXX)", "XXXI)", "XXXII)", "XXXIII)", "XXXIV)",
				"XXXV)", "XXXVI)", "XXXVII)", "XXXVIII)", "XXXIX)", "XL)", "XLI)", "XLII)", "XLIII)", "XLIV)", "XLV)",
				"XLVI)", "XLVII)", "XLVIII)", "XLIX)", "L)", "LI)", "LII)", "LIII)", "LIV)", "LV)", "LVI)", "LVII)",
				"LVIII)", "LIX)", "LX)", "LXI)", "LXII)", "LXIII)", "LXIV)", "LXV)", "LXVI)", "LXVII)", "LXVIII)",
				"LXIX)", "LXX)" };

		if (alphaNumber.replaceAll("[A-Z]{1,7}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenUPar.length; i++) {
				if (alphaNumber.equals(alphaParenUPar[i])) {
					Integer[] aryInt = { (i + 1), type };
					list.add(aryInt);
					break;
				}
			}
		}

		type++;
		String[] alphaRom = { "I.", "II.", "III.", "IV.", "V.", "VI.", "VII.", "VIII.", "IX.", "X.", "XI.", "XII.",
				"XIII.", "XIV.", "XV.", "XVI.", "XVII.", "XVIII.", "XIX.", "XX.", "XXI.", "XXII.", "XXIII.", "XXIV.",
				"XXV.", "XXVI.", "XXVII.", "XXVIII.", "XXIX.", "XXX.", "XXXI.", "XXXII.", "XXXIII.", "XXXIV.", "XXXV.",
				"XXXVI.", "XXXVII.", "XXXVIII.", "XXXIX.", "XL.", "XLI.", "XLII.", "XLIII.", "XLIV.", "XLV.", "XLVI.",
				"XLVII.", "XLVIII.", "XLIX.", "L.", "LI.", "LII.", "LIII.", "LIV.", "LV.", "LVI.", "LVII.", "LVIII.",
				"LIX.", "LX.", "LXI.", "LXII.", "LXIII.", "LXIV.", "LXV.", "LXVI.", "LXVII.", "LXVIII.", "LXIX.",
				"LXX." };

		if (alphaNumber.replaceAll("[A-Z]{1,7}\\.", "").trim().length() < 1) {
			for (int i = 0; i < alphaRom.length; i++) {
				if (alphaNumber.equals(alphaRom[i])) {
					Integer[] aryInt = { (i + 1), type };
					list.add(aryInt);
					break;
				}
			}
		}

		type++;

		return list;

	}

	public void sortByArraySize(List<String[]> headingLinePortionsList) {

		for (int i = 0; i < headingLinePortionsList.size(); i++) {
			// size is # of elements in list (pattern type = 3)
			for (int j = i; j < headingLinePortionsList.size(); j++) {
				if (headingLinePortionsList.get(j).length > headingLinePortionsList.get(i).length) {
					// it now compares the length each array (# of variable
					// instances) against each prior array's (??HOW is it
					// comparing
					// prior instance?). Isn't i=j?
					String[] tmp = headingLinePortionsList.get(i);
					// else below
					// Replaces element at specified position in list with
					// specified element (optional operation).
					// Parameters: index of the element to replace and element
					// to be stored at the specified position
					// Returns: the element previously at the specified position

					// this portion is quite unclear
					headingLinePortionsList.set(i, headingLinePortionsList.get(j));
					headingLinePortionsList.set(j, tmp);
				}
			}
		}
	}

	public String getCompanyNameConfirmed(String companyNameTextNotStripped, String companyName)
			throws FileNotFoundException {

		// for TableTextParser - pass same text thru both of the 1st 2
		// parameters/strings

		NLP nlp = new NLP();
		// System.Xut.println("@getCompanyNameConfirmed
		// companyNameTextNotStripped="+companyNameTextNotStripped+"\r\r
		// companyName="+companyName);
		// System.Xut.println("companyNameText="+companyNameText);

		companyNameTextNotStripped = companyNameTextNotStripped.replace("(?i)<BR>|</DIV>|</P>", "\n");
		companyNameTextNotStripped = nlp.stripHtmlTags(companyNameTextNotStripped).replaceAll("[ ]{2,}", " ")
				.replaceAll("[\r\n]{2,}", "\r");
		companyNameTextNotStripped = companyNameTextNotStripped.replaceAll("[ ]{2,}|[ ]{1,}(?=[\r\n$]{1,})|\t", "")
				.replaceAll(" ?[\r\n]{1,}", "\n").replaceAll("[\r\n]{2,}", "\n");
		companyNameTextNotStripped = companyNameTextNotStripped.replaceAll("[ ]{2,}|[ ]{1,}(?=[\r\n$]{1,})|\t", "")
				.replaceAll(" ?[\r\n]{1,}", "\n").replaceAll("[\r\n]{2,}", "\n");

		if (companyNameTextNotStripped.length() < 2)
			return "";

		// section1: pattern matchers and word conditions created - then
		// section2: apply pattern matcher via series of conditions

		String coName = "";
		String[] coNameSplit = companyName.trim().split(" ");
		// System.Xut.println("coNameSplit.len: " + coNameSplit.length
		// + " company name to match against=" + companyName);
		int startCoNameIdx = -1, endCoNameIdx = -1;
		String word0 = "";
		String word1 = "";
		if (coNameSplit.length > 1) {
			word0 = coNameSplit[0].substring(0, Math.min(coNameSplit[0].length(), 5));
			word1 = coNameSplit[1].substring(0, Math.min(coNameSplit[1].length(), 3));
			// System.Xut.println("l>1 word0:" + word0);
			// System.Xut.println("l>1 word1:" + word1);
		}

		if (coNameSplit.length == 1) {
			word0 = coNameSplit[0].substring(0, Math.min(coNameSplit[0].length(), 4));
			// System.Xut.println("Word0 (l==1):" + word0);
		}

		if (coNameSplit.length == 0) {
			word0 = "";
			word1 = "";
		}

		Pattern CoNameTwoWords = Pattern.compile("(?i)(?s)(" + Pattern.quote(word0)
				+ "[A-Za-z,\\-\\'\\&\\.]{0,15}[ \r\n\t]{0,2}" + Pattern.quote(word1) + ".{0,50}( |\t|\r\n))");
		Pattern ReverseWordsOneAndTwo = Pattern.compile("(?i)(?s)(" + Pattern.quote(word1)
				+ "[A-Za-z,\\-\\'\\&\\.]{0,15}[ \r\n\t]{0,2}" + Pattern.quote(word0) + ".{0,50}( |\t|\r\n))");
		// System.Xut.println("pattern matching this word0="+word0);
		Pattern OneWord = Pattern
				.compile("(?i)(?s)(" + Pattern.quote(word0) + "[A-Za-z\\',\\-\\&\\.]{0,50}( |\t|\r\n))");

		Matcher matchCoNameTwoWords = CoNameTwoWords.matcher(companyNameTextNotStripped);
		Matcher matchReverseWordsOneAndTwo = ReverseWordsOneAndTwo.matcher(companyNameTextNotStripped);
		Matcher matchOneWord = OneWord.matcher(companyNameTextNotStripped);

		if (coNameSplit.length > 1 && matchCoNameTwoWords.find()) {

			startCoNameIdx = matchCoNameTwoWords.start();
			endCoNameIdx = matchCoNameTwoWords.end();
			coName = matchCoNameTwoWords.group();
			// System.Xut.println("l>1 coName:" + coName);
		} else if (coNameSplit.length > 1 && matchReverseWordsOneAndTwo.find()) {
			// herman miller v. miller herman (re-order [1] then [0]
			startCoNameIdx = matchReverseWordsOneAndTwo.start();
			endCoNameIdx = matchReverseWordsOneAndTwo.end();
			coName = matchReverseWordsOneAndTwo.group();
			// System.Xut.println("l>1 coNameReverse:" + coName);
		}

		else if ((coNameSplit.length == 1 && word0.length() > 2) && matchOneWord.find()
				|| (coNameSplit.length > 1 && !matchCoNameTwoWords.find() && !matchReverseWordsOneAndTwo.find())
						&& matchOneWord.find()) {
			startCoNameIdx = matchOneWord.start();
			endCoNameIdx = matchOneWord.end();
			coName = matchOneWord.group();
			// System.Xut.println("matchOne coName:" + coName + " word0=" +
			// word0
			// + "\rcompanyText" + companyNameTextNotStripped);
		} else {
			coName = "";
		}
		// System.Xut.println(coName);
		int tblIdxStart = -1;
		Matcher matchTblName = TableParser.TableNamePattern.matcher(companyNameTextNotStripped);
		if (matchTblName.find()) {
			// System.Xut.println("TableNamePattern::" + match.group());
			tblIdxStart = matchTblName.start();
		}
		if (tblIdxStart > startCoNameIdx && tblIdxStart < endCoNameIdx) {
			// System.Xut.println(tblIdxStart + "::" + startCoNameIdx + "::"
			// + endCoNameIdx);
			coName = companyNameTextNotStripped.substring(startCoNameIdx, tblIdxStart);
			coName = coName.replaceAll("\r\n|\t", " ");
			// System.Xut.println("coName: " + coName);
		}

		// System.Xut.println("return coName: " + coName);
		return coName;
	}

	public String getDecimal(String tableSentence) {
		String decimal;

		Matcher matchDecimal = TableParser.DecimalPattern.matcher(tableSentence);
		String dec = "1";

		// System.Xut.println("decimal ts==="+tableSentence+"|end");
		while (matchDecimal.find()) {
			decimal = matchDecimal.group();
			// System.Xut.println("matchDecimal decimal==="+decimal);

			if (decimal.toLowerCase().contains("million")) {
				dec = "1000000";

			}
			if (decimal.toLowerCase().contains("billion")) {
				dec = "1000000000";
			}

			if (decimal.toLowerCase().contains("thousand") || decimal.toLowerCase().contains("000")) {
				dec = "1000";
			}

			if (decimal.toLowerCase().contains("hundred")
					|| (decimal.toLowerCase().contains("00") && !decimal.toLowerCase().contains("000"))) {
				dec = "100";
			}
		}
		// System.Xut.println("decimal======"+dec);
		return dec;
	}

	public static boolean isTableSentenceAtStartOfLine(String line) {

		NLP nlp = new NLP();

//		NLP.pwNLP.append(NLP.println("@nlp.isTableSentenceAtStartOfLine. line===", line));
		// appliable if tablesentence start directly after hard return (flush
		// left)

		String lineStub = "", lineAfterStub = "";
		int cntMls = 0, cntMls2 = 0, cntYls = 0, cntEls = 0, cntCHafterLineStub = 0;
		if (nlp.getAllIndexEndLocations(line, Pattern.compile("   ")).size() > 0) {
			// will only be >0 if 1st 3 ws is after text. So this only
			// applies to lines starting rt aft hard return.
			// line from start of row (at hard return) - lineStub may be a
			// tableSentence - and if I include it it will cause CH
			// matcher to digest it as if it were a CH and create error. But if
			// lineStub is a tableSentence it will be digested correctly
			// later. So exclude lineStub from analysis of CHs

			// if none of the remainder of the line has CH - clearly not a
			// CH row.
			lineStub = line.substring(0, line.indexOf("   "));

			// System.Xut.println("line stub prior to first 3ws=" + lineStub
			// + "\rlineStub.len=" + lineStub.length());
			lineAfterStub = line.substring(lineStub.length(), line.length());
			cntMls = nlp.getAllIndexStartLocations(lineAfterStub, TableParser.MonthPattern).size();
			cntMls2 = nlp.getAllIndexStartLocations(lineAfterStub, TableTextParser.patternMoDayYear).size();
			cntYls = nlp.getAllIndexStartLocations(lineAfterStub, TableParser.YearOrMoDayYrPattern).size();
			cntEls = nlp.getAllIndexStartLocations(lineAfterStub, TableParser.EndedHeadingPattern).size();
			cntCHafterLineStub = cntMls + cntMls2 + cntYls + cntEls;
		}

		boolean isTableSentence = false;

		// given lineStub starts immediately afte hard return it really much be
		// either tablesentence or CH above rowname either of which is digested
		// separately
		if (lineStub.length() > 50) {
			isTableSentence = true;
		}
		// System.Xut.println("lineStub="+lineStub+" lineStub.len="+lineStub.length());
		return isTableSentence;

	}

	public String startGroup(String startGrp) {

		startGrp = startGrp.replaceAll("l99", "199").replaceAll("-J", " J").replaceAll("-F", " F")
				.replaceAll("-M", " M").replaceAll("-A", " A").replaceAll("-M", " M").replaceAll("-S", " S")
				.replaceAll("-O", " O").replaceAll("-N", " N").replaceAll("-D", " D").replaceAll("-(?i)week", " week")
				.replaceAll("-(?i)m", " m").replaceAll("2 0 0 ", "   200").replaceAll("1 9 9 ", "   199")
				.replaceAll("Septembe ", "Sep.    ").replaceAll("December r", "December  ").toUpperCase()
				.replaceAll("JANAURY", "JANUARY");

		return startGrp;
	}

	public Map<String, Integer> getWordCount(String text) throws SQLException, FileNotFoundException {

		// String is the word (later stem), List<Integer[]> would hold each
		// words lineNo and lineNo idx loc. Or doesn't matter if we go by
		// sentence - then we just need sentence #
		Map<String, Integer> map = new HashMap<String, Integer>();
		// maps columns and rows. Key is 1st integer and its count the 2nd

		StringBuffer sb = new StringBuffer("insert ignore into getStem values\r");
		String[] lines = text.replaceAll("[\"\r\n]", " ").replaceAll("[ ]{2,}", " ").split(" ");
		int value = 1, cnt = 0, lnCnt = 0;
		Matcher matchStem;
		PrintWriter tempPw = new PrintWriter(new File("c:/temp/getLines.txt"));

		for (String line : lines) {
			if (line.length() > 4) {
				// System.Xut.println("lineNo=" + cnt + " line=" + line);
				// tempPw.append("\rlineNo="+cnt+" line="+line);
				matchStem = patternStemmer.matcher(line.trim());
				// ("(?i)(ication|dictory|ility|atory|edly|ably|rian|dict|ied|ing|ity|ly|y)$");

				if (matchStem.find()) {
					// System.Xut.println("found stem=" + matchStem.group()
					// + " word=" + line);
					if (cnt > 0) {
						sb.append(",\r('" + matchStem.group() + "','" + line.replaceAll("'", "\\\\'") + "')");
						// tempPw.append(" stem="+matchStem.group());
						cnt++;
					} else {
						sb.append("\r('" + matchStem.group() + "','" + line.replaceAll("'", "\\\\'") + "')");
						// tempPw.append(" stem="+matchStem.group());
						cnt++;
					}
				} else {
					if (lnCnt > 0) {
						sb.append(",\r('','" + line.replaceAll("'", "\\\\'") + "')");
					}

					else {
						sb.append("\r('','" + line.replaceAll("'", "\\\\'") + "')");
					}

				}
				lnCnt++;
			}

			if (map.containsKey(line)) {
				value = map.get(line);
				map.put(line, value + 1);
			} else
				map.put(line, 1);
			// could record line# and idxLoc in overall doc
		}
		tempPw.append(sb.toString() + ";");

		MysqlConnUtils.executeQuery(sb.toString() + ";");

		tempPw.close();
		return map;
	}

	public Map<Integer, Integer> getColumnCountMapForAllRows(String tableText) {
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

	public static String[] getTablenameInsideHtmlTable(String tableHtml, String companyName) throws IOException {

		NLP nlp = new NLP();
		Document doc = Jsoup
				.parseBodyFragment(tableHtml.replaceAll("\\&nbsp;|\\xA0|\\&#160;", " ").replaceAll("\\&#151;", "-"));
		Elements TRs = doc.getElementsByTag("tr"); // all row elements

		Elements TDs;
		Matcher matchTableNames, matchAnyVisualCharacter;
		@SuppressWarnings("unused")
		String coNameParsed = "", tableName = "", tsHold = "", tableSentence = "", decimalText = "",
				coNameOnPriorLine = "";
		Element tr, td;
		@SuppressWarnings("unused")
		int cnt = 0, textInThisManyColumns = 0, tblNameRow = -1, rowPriorToTblNm = -1;

		@SuppressWarnings("unused")
		boolean foundTablename = false, tableSentenceRow = false;
		@SuppressWarnings("unused")
		Pattern patternAlphaNumber = Pattern.compile("[A-Za-z\\d]{1,}");
		StringBuffer sb = new StringBuffer();
		Pattern patternTocWords = Pattern.compile("Item 1|ITEM 1|PART I|Part I");
		Matcher matchTocWords;

		Elements CAPTION = doc.getElementsByTag("caption");
		// System.Xut.println("CAPTIONS.text="+CAPTION.text());
		if (CAPTION.size() == 1) {
			List<String> listTmp = nlp.getAllMatchedGroups(CAPTION.text(), TableParser.TableNamePattern);

			if (listTmp.size() > 0) {
				tableName = listTmp.get(0).replaceAll("\\)|\\(|\\$|\\'|\\*", "");
				// System.Xut
				// .println("@nlp.getTablenameInsideHtmlTable 2 found tablename in caption==="
				// + tableName);
				foundTablename = true;
			}
		}

		for (int i = 0; i < Math.min(TRs.size(), 10); i++) {
			tr = TRs.get(i);
			if (tr.text().replaceAll(" |\\(|\\)|\\$|\\.|\\*", "").length() < 1)
				continue;
			// System.Xut.println("@getTablenameInsideHtmlTable tr text="
			// + tr.text());
			matchTocWords = patternTocWords.matcher(tr.text());
			if (matchTocWords.find()) {
				cnt--;
			}
			cnt++;
			sb.append(" " + tr.text());
			TDs = tr.getElementsByTag("td");
			if (foundTablename) {
				tableSentenceRow = true;
			}

			// System.Xut.println("tsHold==" + tsHold +
			// " textInThisManyColumns=="
			// + textInThisManyColumns);
			if (textInThisManyColumns == 1) {
				tableSentence = tableSentence.trim() + " " + tsHold.trim();
				// System.Xut.println("tsHold + tableSentence=" +
				// tableSentence);
			}

			// if there are 2 columns w/ text -- absent TS above rowname -
			// the tablename and tablesentence should NOT occur later

			if (textInThisManyColumns >= 1) {
				// if I have not found tablename-ck if it is in 1st col
				if (!foundTablename) {
					List<String> listTmp = nlp.getAllMatchedGroups(TDs.get(0).text(), TableParser.TableNamePattern);
					if (listTmp.size() > 0 && TDs.size() < 3) {
						// tableName 'cash flow provided by...' is often a
						// rowname and in col 1 but I've never seen a tableName
						// in a row with more than 1 col. I parse tables if 2 or
						// more data cols - so data col row would have to be at
						// least 3 - so if rowname TD size is at least 3 and
						// tableName row really can't be greater than 1.
						tableName = listTmp.get(0).replaceAll("\\)|\\(|\\$|\\'|\\*", "");
						foundTablename = true;
						// System.Xut
						// .println("@nlp.getTablenameInsideHtmlTable 1 foundTablename - tableName="
						// + tableName);
					}
				}
				break;
			}

			tsHold = "";
			textInThisManyColumns = 0;

			for (int c = 0; c < TDs.size(); c++) {
				if (textInThisManyColumns > 1)
					break;
				td = TDs.get(c);

				if (!foundTablename) {
					// System.Xut
					// .println("checking to see if this is a tablename="
					// + td.text());
					if (coNameParsed.length() < 1 && !foundTablename && cnt < 5) {
						coNameParsed = nlp.getCompanyNameConfirmed(td.text(), companyName);
						// System.Xut
						// .println("1 @getTablenameInsideHtmlTable coNameParsed="
						// + coNameParsed);
						if (coNameParsed.length() > 5) {
							coNameParsed = td.text();
							// System.Xut
							// .println("2 @getTablenameInsideHtmlTable - matched companyName -- so
							// coNameParsed="
							// + coNameParsed);
						}
					}

					List<String> listTn = new ArrayList<String>();

					matchTableNames = TableParser.TableNamePattern.matcher(td.text());

					listTn = nlp.getAllMatchedGroups(td.text(), TableParser.TableNamePattern);
					// System.Xut.println("cnt?="+cnt);
					if (matchTableNames.find() && cnt < 5 && !foundTablename) {
						// System.Xut
						// .println("@nlp.getTablenameInsideHtmlTable 3 found tablename match inside
						// table="
						// + td.text());
						tableName = listTn.get(listTn.size() - 1).replaceAll("\\)|\\(|\\$|\\'|\\*", "");

						// System.Xut
						// .println("before @nlp.getLineHtmltablename tableName="
						// + tableName);
						tableName = getLineHtml(td, tableName);

						// System.Xut.println("after @nlp.getLineHtmltablename="
						// + tableName);
						coNameOnPriorLine = getPriorLineInCell(td, tableName.trim());
						// System.Xut
						// .println("@getTablenameInsideHtmlTable -- priorLine--coNameOnPriorLine"
						// + coNameOnPriorLine);
						foundTablename = true;
						tblNameRow = i;
						if (i > 0 && coNameOnPriorLine.length() < 3) {
							rowPriorToTblNm = i - 1;
							coNameOnPriorLine = TRs.get(rowPriorToTblNm).text();
						}
					}
				}

				// System.Xut.println("A. td.text=" + td.text()
				// + " textInThisManyColumns=" + textInThisManyColumns);

				// if there are 2 columns w/ text -- absent TS above rowname -
				// the tablename and tablesentence should NOT occur later. Any
				// visual character used b/c chars in multi cols is what
				// indicates I have traversed rows that now come after tn and
				// ts.
				if ((tableSentenceRow || textInThisManyColumns == 1) && td.text().length() > 0) {
					matchAnyVisualCharacter = nlp.patternAnyVisualCharacter.matcher(td.text());
					if (matchAnyVisualCharacter.find()) {
						if (nlp.getAllIndexEndLocations(td.text(), TableParser.TableNamePattern).size() > 0) {
							tsHold = td.text().substring(
									nlp.getAllIndexEndLocations(td.text(), TableParser.TableNamePattern).get(0));
						} else {
							tsHold = td.text();
						}
						textInThisManyColumns++;
						// System.Xut.println("A. tsHold=" + tsHold
						// + " textInThisManyColumns="
						// + textInThisManyColumns);
					}
				}
			}
		}

		String getDecimalText = sb.toString();
		String[] ary = { coNameParsed, tableName, tableSentence, getDecimalText, coNameOnPriorLine };
//		NLP.pwNLP.append(NLP.println("@nlp.getTablenameInsideHtmlTable -- returned array=", Arrays.toString(ary)));
		return ary;
	}

	public static String getPriorLineInCell(Element td, String tableName) {

//		NLP.pwNLP.append(NLP.println("@nlp.getPriorLineInCel - tablename match=", tableName));
		String priorLine = "";

		Elements DIVs, Ps, BRs;
		Element div, p, br;

		DIVs = td.getElementsByTag("div");
		Ps = td.getElementsByTag("p");
		BRs = td.getElementsByTag("br");

		if (DIVs.size() > 0) {
			return priorLine = getPriorLineHtml(DIVs, tableName);
		}

		if (Ps.size() > 0) {
			return priorLine = getPriorLineHtml(Ps, tableName);
		}
		if (BRs.size() > 0) {
			return priorLine = getPriorLineHtml(BRs, tableName);
		}

		return priorLine;
	}

	public static String getPriorLineHtml(Elements elements, String tableName) {
		String priorLine = "";
		Element element;

		tableName = tableName.replaceAll("\\(|\\)|\\$|\\*", "");
		// System.Xut.println("tableName=" + tableName);
		for (int i = 0; i < elements.size(); i++) {
			element = elements.get(i);
//			NLP.pwNLP.append(
//					NLP.println("@getPriorLineHtml - checking lines in a cell for priorLine -- line==" + element.text(),
//							" match==" + tableName));
			if (element.text().replaceAll(tableName, "").length() < element.text().length() && i - 1 >= 0) {
				if (i - 2 >= 0) {
					Matcher matchCoIncLtdLLC = patternCoIncLtdLLC.matcher(elements.get(i - 1).text());
					if (matchCoIncLtdLLC.find()) {
						priorLine = elements.get(i - 1).text();
					}
				}

				if (priorLine.length() < 3) {
					priorLine = elements.get(i - 1).text();
				}
			}
		}

		return priorLine;
	}

	public static String getLineHtml(Element td, String tableName) throws IOException {

		// System.Xut.println("@getLineHtml -- tablenambe match=" + tableName
		// + "\rtd.text=" + td.text());

		Elements DIVs, Ps, BRs;

		DIVs = td.getElementsByTag("div");
		Ps = td.getElementsByTag("p");
		BRs = td.getElementsByTag("br");

		if (DIVs.size() > 0) {
			tableName = getTableNameFromTextBetwTags(DIVs, tableName);
		}
//		NLP.pwNLP.append(NLP.println("@getLineHtml @div -- tablename match=", tableName));

		if (Ps.size() > 0) {
			tableName = getTableNameFromTextBetwTags(Ps, tableName);
		}
//		NLP.pwNLP.append(NLP.println("@getLineHtml @Ps -- tablename match=", tableName));

		if (BRs.size() > 0) {
			tableName = getTableNameFromTextBetwTags(BRs, tableName);
		}
//		NLP.pwNLP.append(NLP.println("@getLineHtml @BRs -- tablename match=", tableName));

		return tableName;
	}

	public static String getTableNameFromTextBetwTags(Elements elements, String tableName) throws IOException {

		NLP nlp = new NLP();
		String line = "";
		Element element;

		for (int i = 0; i < elements.size(); i++) {
			element = elements.get(i);

//			NLP.pwNLP.append(
//					NLP.println("@nlp.getTableNameFromTextBetwTags - checking lines to see if tableName matched==",
//							element.text() + "\r--this is tablename==" + tableName));

			Matcher matchTableName = TableParser.TableNamePattern.matcher("  " + elements.get(i).text() + "  ");

			List<String> listTn = nlp.getAllMatchedGroups(elements.get(i).text(), TableParser.TableNamePattern);

			if (matchTableName.find() && line.contains(tableName)) {
				line = listTn.get(listTn.size() - 1);
//				NLP.pwNLP.append(NLP.println("@nlp.getTableNameFromTextBetwTags - match on this line=", line));
			}
		}

		if (line.length() == 0)
			line = tableName;

//		NLP.pwNLP.append(NLP.println("@nlp.getTableNameFromTextBetwTags - return line=", line));

		return line;
	}

	public String replaceTabsFromSecFiling(String text) {

		// sec.gov asci filings are based on 80 line character with every 8th
		// character. Therefore to replace a tab with correct number of ws I
		// have determine on each line where there is a tab the number of
		// characters that precede it in order to determine what position the
		// tab will end at. If for example the last character on a line is at 21
		// then there is a tab - the tab will add 3 ws to tab end 24 (tabs are
		// 8,16,24,32,40,48,56,64,72,80)
		// reduced it be 1 character - and it seems to have worked!
		// see:
		// https://www.sec.gov/Archives/edgar/data/799197/0000799197-98-000013.txt

		// System.Xut.println("startGroup indexOf \t="+startGroup.indexOf("\t"));
		// if no tabs to replace - return

		// int[] tabAry={9,17,25,33,41,49,57,65,73,81};
		// int[] tabAry={8,16,24,32,40,48,56,64,72,80};
		int[] tabAry = { 7, 15, 23, 31, 39, 47, 55, 63, 71, 79 };
		int tabIdx = 0, spacesToAdd = 0;
		// If I try to use just \r or just \n - or [$\r\n} or [\r\n] it does NOT
		// work. If I use $ it works.

		int i = 0;
		String line = "", newLine = "", spaces = "";

		String[] textSplit = text.split("[$\r\n]");
		text = "";
		for (i = 0; i < textSplit.length; i++) {
			line = textSplit[i];
			// System.Xut.println("line=" + line);
			String[] lineSplit = line.split("\t");
			newLine = "";
			spacesToAdd = 0;
			for (int c = 0; c < lineSplit.length; c++) {
//				 NLP.pwNLP.append(NLP.println("split by tab - lineSplit[c]=",
				// lineSplit[c]));
				newLine = newLine + lineSplit[c];
				spacesToAdd = 0;
				for (int n = 0; n < tabAry.length; n++) {
					tabIdx = tabAry[n];
					// if (tabIdx > newLine.length()) {
					// System.Xut
					// .println("spacesToAdd = tabIdx-newLine.len. tabIdx="
					// + tabIdx
					// + " newLine.len="
					// + newLine.length());
					// }
					if (newLine.length() < tabIdx) {
						spacesToAdd = tabIdx - newLine.length();
						// NLP.print("spacesToAdd=" , spacesToAdd);
						break;
					} else
						spacesToAdd = 0;
				}
				spaces = "";

				for (int b = 0; b < spacesToAdd; b++) {
					spaces = spaces + " ";
				}
				newLine = newLine + spaces;
				// NLP.print("after adding spaces -- newLine=" , newLine);
			}

			// if(newLine.replaceAll("\r|\n| ", "").length()<1)
			// continue;
			// doesn't work if i use continue.
			text = text + newLine + "\r\n";

		}
		// NLP.pwNLP.append(NLP.println(
		// "@replaceTabsFromSecFiling tableText after tabs replaced. tableText==",
		// text));
		return text;
	}

	public static List<String> getCompanyName(String companyNameTextNotStripped, String companyName)
			throws FileNotFoundException {

		// System.Xut.println("@nlp.getCompanyName - companyNameTextNotStripped =
		// "+companyNameTextNotStripped+
		// " companyName to match against=="+companyName);

		NLP nlp = new NLP();
		// System.Xut
		// .println("@nlp.getCompanyName - before replacement - companyName text ="
		// + companyNameTextNotStripped + "|end companyName text");

		companyNameTextNotStripped = companyNameTextNotStripped.replaceAll("(?i)(<BR>|</DIV>|</P>)", "\n");
		// System.Xut
		// .println("@nlp.getCompanyName - after 1 replacement - companyName text ="
		// + companyNameTextNotStripped + "|end companyName text");

		companyNameTextNotStripped = nlp.stripHtmlTags(companyNameTextNotStripped).replaceAll("[ ]{2,}", " ")
				.replaceAll("[\r\n]{2,}", "\r\n");
		companyNameTextNotStripped = companyNameTextNotStripped.replaceAll("[ ]{2,}|[ ]{1,}(?=[\r\n$]{1,})|\t", "")
				.replaceAll(" ?[\r\n]{1,}", "\n").replaceAll("[\r\n]{2,}", "\n");
		companyNameTextNotStripped = companyNameTextNotStripped.replaceAll("[ ]{2,}|[ ]{1,}(?=[\r\n$]{1,})|\t", "")
				.replaceAll(" ?[\r\n]{1,}", "\n").replaceAll("[\r\n]{2,}", "\n");

		// System.Xut
		// .println("@nlp.getCompanyName - after replacement - companyName text ="
		// + companyNameTextNotStripped + "|end companyName text");
		String companyNameConfirmed = "", companyNameOnLinePriorToTableName = "", line = "";
		String[] lines = companyNameTextNotStripped.split("\n");
		Matcher matchCoIncLtdLLC;
		int cnt = 0, tblNm = 0;
		for (int i = (lines.length - 1); i >= 0; i--) {
			cnt++;
			line = lines[i];
			// System.Xut.println("row=" + i + " companyName line=" + line);
			if (nlp.getAllIndexEndLocations(line + "  ", TableParser.TableNamePattern).size() > 0) {
				tblNm = i;
				// System.Xut.println("tblNm row=" + tblNm);
				if (tblNm - 2 > 0) {
					matchCoIncLtdLLC = patternCoIncLtdLLC.matcher(lines[i - 2]);
					if (matchCoIncLtdLLC.find()) {
						companyNameOnLinePriorToTableName = lines[i - 2];
						// System.Xut.println("-2 set
						// companyNameOnLinePriorToTableName=="+companyNameOnLinePriorToTableName);

					}
				}

				if (tblNm - 1 >= 0 && companyNameOnLinePriorToTableName.length() < 3) {
					companyNameOnLinePriorToTableName = lines[i - 1];
					// System.Xut.println("set
					// companyNameOnLinePriorToTableName=="+companyNameOnLinePriorToTableName);
				}
			}
			companyNameConfirmed = nlp.getCompanyNameConfirmed(line, companyName);
			// companyName on prior line
			// System.Xut.println("cnt="+cnt+" i="+i+"
			// companyNameConfirmed=="+companyNameConfirmed);
			if (companyNameConfirmed.length() > 3) {
				companyNameConfirmed = line;
				break;
			}
			if (cnt > 4)
				break;
		}

		// System.Xut.println("companyNameOnLinePriorToTableName[0]=="+companyNameOnLinePriorToTableName);
		// System.Xut.println("companyNameConfirmed[1]=="+companyNameConfirmed);
		List<String> listCoName = new ArrayList<>();
		listCoName.add(companyNameOnLinePriorToTableName);
		listCoName.add(companyNameConfirmed);
		return listCoName;

	}

	public static String[] getTablenamePlainText(String text) throws IOException {

		text = text.replaceAll(
				"(?ism)(CONDENSED)?(?=.{0,5})(CONSOLIDATED)?(?=.{0,5})(STATEMENTS?)(?=.{0,5})(OF)(?=.{0,5})(OPERATIONS?|INCOME|EARNINGS?)",
				"$1 $2 $3 $4 $5");

		// System.Xut.println("nlp.getTablenamePlainText is matching this text=="
		// + text + "|end");

		String[] lines = text.split("\r");
		String line = "", textOnLineAfterTableName = "", prevTextOnLineAfterTableName = "";
		String tablename = "", prevTablename = "";
		Pattern patternLiab = Pattern.compile("(?i)Liabilities.{1,2}and.{1,2}(Stock|Share)holder['s]{0,2}.{1,2}Equity");
		Matcher matchTableNameTable, matchLiab;

		/*
		 * PROBLEM: <p align="center"><b>Vail Resorts, Inc.<br /> Consolidated Condensed
		 * Statements of Operations <br /> (In thousands, except per share amounts)<br
		 * /> (Unaudited)<br /> </b></p>
		 */

		int eIdx = 0, sIdx = 0, prevEidx = 0, prevSidx = 0, prevI = 0, allPriorLinesCount = 0;
		for (int i = 0; i < lines.length; i++) {
			line = lines[i];
			matchTableNameTable = TableParser.TableNamePattern.matcher(line);
			while (matchTableNameTable.find()) {
				// System.Xut
				// .println("@nlp.getTablenamePlainText -- matched line of tableName="
				// + line);
				sIdx = matchTableNameTable.start() + i + allPriorLinesCount;
				eIdx = matchTableNameTable.end() + i + allPriorLinesCount;
				tablename = matchTableNameTable.group();
				tablename = line.replaceAll("\\)|\\(|\\$|\\'|\\*", "");
				// System.Xut.println("tn1=" + tablename);
				textOnLineAfterTableName = line.substring(matchTableNameTable.end());
			}
			// use while b/c it is a line.
			matchLiab = patternLiab.matcher(line);
			while (matchLiab.find() && prevTablename.toUpperCase().contains("BALANCE SHEET")) {
				// System.Xut
				// .println("prev tablename contains balance sheet and current line contains
				// Liabilities and Stockholder's Equity - use prior tn");
				eIdx = prevEidx;
				sIdx = prevSidx;
				tablename = prevTablename;
				// tablename =
				// prevTextOnLineAfterTableName.replaceAll("\\)|\\(|\\$|\\'|\\*",
				// "");;
				// System.Xut.println("tn2 tablename=" + tablename + " eIdx="
				// + eIdx + " sIdx=" + sIdx);
			}

			matchTableNameTable = TableParser.TableNamePattern.matcher(line);
			matchLiab = patternLiab.matcher(line);
			while (matchLiab.find() && matchTableNameTable.find()
					&& !prevTablename.toUpperCase().contains("BALANCE SHEET")) {
				sIdx = matchLiab.start() + allPriorLinesCount + i;
				eIdx = matchLiab.end() + allPriorLinesCount + i;
				tablename = matchLiab.group().replaceAll("\\)|\\(|\\$|\\'|\\*", "");
				// System.Xut.println("tn3=" + tablename);

				textOnLineAfterTableName = line.substring(matchTableNameTable.end());
			}
			prevSidx = sIdx;
			prevEidx = eIdx;
			prevTablename = tablename;
			prevTextOnLineAfterTableName = textOnLineAfterTableName;
			prevI = i;

			allPriorLinesCount = allPriorLinesCount + line.length();
		}

		// if prior tn was 'Balance Sheet' and next is 'Liabilities and
		// Shareholder' - then use 'Balance Sheet'

		tablename = tablename.replaceAll(
				"(?ism)Item.{1,3}8.{1,4}FINANCIAL.{1,3}STATEMENTS?.{1,3}AND.{1,3}SUPPLEMENTARY.{1,3}DATA", "");

		// System.Xut.println("found tablename outside of table=" + tablename);

		String[] ary = { tablename, sIdx + "", eIdx + "", textOnLineAfterTableName };

		// System.Xut
		// .println("@nlp.getTablenamePlainText
		// ary=[tableName,sIdx,eIdx,textOnLineAfterTableName]="
		// + Arrays.toString(ary));

		return ary;

	}

	public static Map<Integer, Integer> mapDecimalInItsOwnColumn(Elements dataRows, List<String> CHs)
			throws ParseException, SQLException, IOException {

		// NOTE: this will find tables where decimal values is in its own
		// columns and not in same column of value. For example 1,234.1 is
		// placed in two columsn - 1,234 and .1. The map key is the colIdx where
		// just decimal value is foudn (.1) and map value is the number of times
		// that colIdx had just a decimal.

//		NLP.pwNLP.append(NLP.println("@nlp.mapDecimalInItsOwnColumn -- dataRows.size=", "" + dataRows.size()));

		Map<Integer, Integer> map = new TreeMap<Integer, Integer>();

		NLP nlp = new NLP();
		Element tr, td;
		Elements TDs;
		String cellText = "-10", prevCellText = "-100", nextCellText = "", rowText = "";
		int prevColIdx = -10;
		int cntGood = 0, cntBad = 0, cntColIdx = 0, cntLessThan4Cols = 0;
		boolean decimalInSeparateCell = true;
		Pattern patternEPS = Pattern.compile("(?i)(earning|income|loss|share|dividend)");
		@SuppressWarnings("unused")
		Matcher matchEPS;
		for (int row = 0; row < dataRows.size(); row++) {
			// System.Xut.println("how many rows of table do I check each time checking for
			// decimals in separate col -- row#==="+row);
			tr = dataRows.get(row);
			TDs = tr.getElementsByTag("td");
			if (TDs.size() <= 3) {
				cntLessThan4Cols++;
			}
			// if 3 or more rows with less than 4 cols return null.
			if (TDs.size() <= 3 && cntLessThan4Cols > 2) {
//				NLP.pwNLP.append(NLP.println("@nlp.mapDecimalInItsOwnColumn -- return null at this row=", tr.text()));
				// only 3 cols - I can only parse where at least 2 data cols -
				// so this could theoretically be a table with decimal col but
				// will need to skip it -- so I return here if total cols is 3
				// or
				// less)
				return null;
			}
			rowText = tr.text();
			matchEPS = patternEPS.matcher(rowText);
			prevCellText = "";
			cellText = "";
			nextCellText = "";
			prevColIdx = -10;

			for (int colIdx = 0; colIdx < TDs.size(); colIdx++) {
				decimalInSeparateCell = true;
				td = TDs.get(colIdx);

				// if (cellText.length() > 0) {
				// System.Xut.println("1 cellText=" + cellText);
				// }
				cellText = td.text().replaceAll("(\\$|\\(|\\)| |,)", "").trim();
				if (colIdx + 1 < TDs.size()) {
					nextCellText = TDs.get(colIdx + 1).text();
				}

				// not decimalInItsOwnCell if: (1) cur cell text is a number but
				// prior and next is also (2) OR cur cell text is a number and
				// prior and next are
				if (colIdx > 1 && colIdx - prevColIdx == 1
						&& ((nlp.isNumeric(cellText) && !nlp.isNumeric(prevCellText) && colIdx + 1 < TDs.size()
								&& !nlp.isNumeric(nextCellText))

								|| (nlp.isNumeric(cellText) && nlp.isNumeric(prevCellText) && colIdx + 1 < TDs.size()
										&& nlp.isNumeric(nextCellText)))) {
					// System.Xut
					// .println("these consecutive cells don't show decimal in its own col
					// cellText="
					// + cellText
					// + " prevCellText="
					// + prevCellText+" rowname="+TDs.get(0).text());
					decimalInSeparateCell = false;
					cntBad++;
				}
				// System.Xut.println("A. cellText=" + cellText +
				// " prevCellText="
				// + prevCellText + " colIdx=" + colIdx + " prevColIdx="
				// + prevColIdx);
				// decimal value b/w 0.0 and .9
				if (decimalInSeparateCell && colIdx > 0 && nlp.isNumeric(cellText) && nlp.isNumeric(prevCellText)
						&& colIdx - prevColIdx == 1) {
					// if map was previously populated with colIdx with decimal
					// - then add one to last value populated to keep count
					// System.Xut.println("its a decimal column -- decimal value="+cellText);
					if (null != map.get(colIdx) && map.get(colIdx).intValue() >= 0) {
						cntColIdx = map.get(colIdx).intValue() + 1;
						map.put(colIdx, cntColIdx);
//						NLP.pwNLP.append(NLP.println("@nlp.mapDecimalInItsOwnColumn -- decimal map.put. colIdx="
//								+ colIdx + " cntColIdx=" + cntColIdx + " cellText=" + cellText + " prevCellText=",
//								prevCellText));
					} else {
						map.put(colIdx, 1);
					}

//					NLP.pwNLP.append(NLP.println(
//							"@nlp.mapDecimalInItsOwnColumn -- added to decimal in its own row list -- prevCell data=",
//							prevCellText + " current Cell=" + cellText + " colIdx=" + colIdx));
					decimalInSeparateCell = true;
					cntGood++;
				}

				if (colIdx > 0) {
					prevColIdx = colIdx;
					prevCellText = cellText;
				}
			}
		}

		// PICKUP HERE - STEP THRU LOGIC - FIGURE OUT HOW TO USE FILTER OF VALUE
		// AFTER REMOVING '.' IS BETWEEN 1 AND 9. MAY BE SIMPLEST MANNER B/C
		// THEN IT DOVE TAILS INTO THIS

//		NLP.pwNLP.append(NLP.println("@nlp.mapDecimalInItsOwnColumn -- final cntGood=",
//				cntGood + " cntBad=" + cntBad + " cntGood/(cntGood+cntBad)=" + (double) cntGood / (cntGood + cntBad)));

		// System.Xut
		// .println("printing map of DecimalInItsOwnColumn (key is colIdx with just
		// decimal and val is number of times found");
		// nlp.printMapIntInt(map);

		// remove instances where decimal wasn't found at least 5 times.
		List<Integer> listKeysToRemove = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
//			NLP.pwNLP.append(NLP.println("@nlp.mapDecimalInItsOwnColumn -- key=",
//					entry.getKey() + "\r val=" + entry.getValue()));
			if (entry.getValue() < 5)
				listKeysToRemove.add(entry.getKey());
		}

		for (int i = 0; i < listKeysToRemove.size(); i++) {
			map.remove(listKeysToRemove.get(i));
		}

		if ((double) cntGood / (cntBad + cntGood) > .9 && cntColIdx >= 15) {
//			NLP.pwNLP.append(NLP.println("@nlp.mapDecimalInItsOwnColumn -- returned decimal map", ""));
			return map;

		}

		else {
			return null;
		}
	}

	public static boolean isTextTableInHtml(String text) {
		boolean runTableTextParser = false;

		// this is to make numbers scientic versus accounting so it is easier to
		// pattern match. I also remove hard returns that separate data rows -to
		// better match consecutive rows with last char = number
		// System.Xut.println("is it tableText in html - text looks like this===="
		// + text);

		boolean blankRow = false;
		text = text.replaceAll("\\$|\\(|\\)", " ");
		String[] textLines = text.split("[\r\n]");
		String line = "";
		int priorLineNo = 0, tmpMaxCnt = 0, maxCnt = 0;
		Pattern patternTD = Pattern.compile("(?i)</TD>");
		Pattern patternTextDataColumns = Pattern.compile(
				"[ ]{4,}\\(?([\\d]{3,11}|[\\d]{2}|[\\d]{1,3}\\.[\\d]{1,2})\\)?[ ]{3,20}\\(?([\\d]{3,11}|[\\d]{2}|[\\d]{1,3}\\.[\\d]{1,2})\\)?");

		// a total row without a rowname will just be ## ws ## \r
		Matcher matchTextDataColumns, matchTD;

		// this will find at least 1 table with at least 6 consecutive rows with
		// data columns where each column value is greater than 4 digits AND
		// there is not 'td' on that row.
		int blankRowCount = 0;
		for (int i = 0; i < textLines.length; i++) {
			line = textLines[i].replaceAll("[\\$\\(\\)]{1,}", " ").replaceAll("[-_=,]{1,}", "");
			if (line.trim().length() < 3) {
				blankRow = true;
				blankRowCount++;
				continue;
			}
			// System.Xut.println("lines=" + line);

			matchTD = patternTD.matcher(line);
			matchTextDataColumns = patternTextDataColumns.matcher(line);

			if (matchTextDataColumns.find() && !matchTD.find() && (priorLineNo == 0 || 1 == i - priorLineNo
					|| (blankRow && 1 <= (i - priorLineNo + blankRowCount)))) {
				tmpMaxCnt++;

				if (maxCnt < tmpMaxCnt) {
					maxCnt = tmpMaxCnt;
					// System.Xut.println("maxCnt==" + maxCnt);
				}

			} else {
				// System.Xut.println("else - setting tmpMaxCnt to zero="
				// + tmpMaxCnt);
				tmpMaxCnt = 0;
			}

			matchTD = patternTD.matcher(line);
			matchTextDataColumns = patternTextDataColumns.matcher(line);
			if (matchTextDataColumns.find() && !matchTD.find()) {
				priorLineNo = i;
			}

			blankRow = false;
			blankRowCount = 0;
			if (maxCnt > 4) {
//				NLP.pwNLP.append(NLP.println("got a 6 maxComaxCnt==t at this line=\r", line));
				return runTableTextParser = true;
			}
		}

//		NLP.pwNLP.append(NLP.println("final maxCnt=", maxCnt + ""));
		return runTableTextParser;
	}

	public static String[] isTableOfContentsHtml(String tableHtml, String companyName) throws FileNotFoundException {

		Document doc = Jsoup
				.parseBodyFragment(tableHtml.replaceAll("\\&nbsp;|\\xA0|\\&#160;", " ").replaceAll("\\&#151;", "-"));
		Elements TRs = doc.getElementsByTag("tr"); // all row elements
		Elements TDs;
		// copy in txt TOC logic below.
		@SuppressWarnings("unused")
		Element tr, td;
		for (int i = 0; i < Math.min(TRs.size(), 10); i++) {
			tr = TRs.get(i);
			TDs = tr.getElementsByTag("td");
			for (int c = 0; c < TDs.size(); c++) {
				td = TDs.get(c);
			}
		}

		String[] ary = {};
		return ary;
	}

	public static int[] isTableOfContentsTxt(String toc) throws IOException {

		NLP nlp = new NLP();
		// if 2 or more tablenames, 2 or more toc page # patters, 2 or more
		// month and 2 or more year and one or more ended pattern then it is a
		// toc

		// System.Xut.println("toc text="+toc+"|end");
		boolean isToc = false, tocConfirmed = false;
		int cntTableName = nlp.getAllIndexEndLocations(toc, TableParser.TableNamePattern).size();
		int cntTocPgNo = nlp.getAllIndexEndLocations(toc, patternTocPageNumber).size();
		int cntE = nlp.getAllIndexEndLocations(toc, TableParser.EndedHeadingPattern).size();
		int cntY = nlp.getAllIndexEndLocations(toc, Pattern.compile("[\\p{Alnum}\\p{Punct}] ?[12]{1}[09]{1}[0-9]{2}"))
				.size();
		int cntM = nlp.getAllIndexEndLocations(toc, TableParser.MonthPatternSimple).size();

		@SuppressWarnings("unused")
		List<Integer> listTableName = nlp.getAllIndexEndLocations(toc, TableParser.TableNamePattern);
		@SuppressWarnings("unused")
		List<Integer> listTocPgNo = nlp.getAllIndexEndLocations(toc, patternTocPageNumber);
		@SuppressWarnings("unused")
		List<Integer> listE = nlp.getAllIndexEndLocations(toc, TableParser.EndedHeadingPattern);
		@SuppressWarnings("unused")
		List<Integer> listY = nlp.getAllIndexEndLocations(toc,
				Pattern.compile("[\\p{Alnum}\\p{Punct}] [12]{1}[09]{1}[0-9]{2}"));
		@SuppressWarnings("unused")
		List<Integer> listM = nlp.getAllIndexEndLocations(toc, TableParser.MonthPatternSimple);

		// System.Xut.println("@nlp.isTableOfContents --- cntTableName="
		// + cntTableName + " cntTocPgNo=" + cntTocPgNo + " cntE=" + cntE
		// + " cntY=" + cntY + " cntM=" + cntM);
		if (cntTableName > 1 && cntTocPgNo > 1 && cntE > 0 && cntY > 1 && cntM > 1) {
			isToc = true;
		}

		// System.Xut.println("isItToc=" + isToc);
		// return smallest eIdx value of all the above. then use that as the
		// next launching point on next loop - by setting tableTextEidx to this
		// value after adding back necessary idx.

		String line = "", lastLine = "";
		if (isToc) {
			String tocPrep = toc.replaceAll("[\r\n]{2,}", "\r");
			// System.Xut.println("tocPrep="+tocPrep);

			String[] tocSplit = tocPrep.split("\r");
			for (int i = 0; i < tocSplit.length; i++) {
				line = tocSplit[i];
				cntTableName = nlp.getAllIndexEndLocations(line, TableParser.TableNamePattern).size();
				cntTocPgNo = nlp.getAllIndexEndLocations(line, patternTocPageNumber).size();
				cntE = nlp.getAllIndexEndLocations(line, TableParser.EndedHeadingPattern).size();
				cntY = nlp.getAllIndexEndLocations(line,
						Pattern.compile("[\\p{Alnum}\\p{Punct}] ?[12]{1}[09]{1}[0-9]{2}")).size();
				cntM = nlp.getAllIndexEndLocations(line, TableParser.MonthPatternSimple).size();
				// System.Xut.println("@nlp.isTableOfContents ---at line="+line+"\r
				// cntTableName="
				// + cntTableName + " cntTocPgNo=" + cntTocPgNo + " cntE=" +
				// cntE
				// + " cntY=" + cntY + " cntM=" + cntM);
				if (cntTableName > 0 && i + 1 < tocSplit.length && (cntTocPgNo > 0
						|| nlp.getAllIndexEndLocations(tocSplit[i + 1], patternTocPageNumber).size() > 0)) {
					// System.Xut.println("confirmed toc at this line="+line);
					tocConfirmed = true;
				}
				if (tocConfirmed && cntTocPgNo > 0) {
					lastLine = line;
				}
				if (tocConfirmed && i + 1 < tocSplit.length
						&& nlp.getAllIndexEndLocations(tocSplit[i + 1], patternTocPageNumber).size() > 0) {
					lastLine = tocSplit[i + 1];
				}
			}
		}

		// match against original toc text - this preserves idx which can then
		// be added back in method that calls this

		// System.Xut.println("lastLine="+lastLine+"||||");
		lastLine = lastLine.replaceAll("[\\{\\}\\[\\]\\(\\)\\$\\.\\^\\\\]", ".{1}");

		Matcher matchLastLine = Pattern.compile("(" + lastLine + ")").matcher(toc);
		int eIdx = -1;
		while (lastLine.length() > 1 && matchLastLine.find()) {
			eIdx = matchLastLine.end();
			// System.Xut.println("matchLastLine=="+matchLastLine.group()+
			// " matchLastLine eIdx="+eIdx);

		}
		// if intAry[0]=0 - toc confirmed else 1. intAry[1]=endIdx of toc.
		int confirm = 1;
		if (tocConfirmed) {
			confirm = 0;
		}

		int[] intAry = { confirm, eIdx };
		// System.Xut.println("tocConfirmed=" + confirm + " eIdx=" + eIdx);
		return intAry;
	}

	public static Map<Integer, String[]> getPairedGroupsCloseToEachOther(String text) throws IOException {

		// returns a map of those pairs found close to another pair (distance
		// preset but could put as variable). key is idx location the start of
		// the first pattern is found.

		Map<Integer, String[]> map = new TreeMap<Integer, String[]>();
		NLP nlp = new NLP();

		double startTime = System.currentTimeMillis();

		Pattern patternTableNameSimple = Pattern.compile("(?i)(Statements? of |Consolidated |Condensened )"
				+ "(Shareholder|Stockholder|Changes in|Liabilit|Income|Operations?|Earnings|Loss|Cash Flows?)|Balance Sheets?");
		List<String[]> listStartIdxAndtablename = nlp.getAllStartIdxLocsAndMatchedGroups(text, patternTableNameSimple);

		Pattern patternColumnHeadingSimple = Pattern.compile(
				"(?i)three|six|nine|twelve|week|month|year|ended|january|february|march|april|may |june|july|august|september"
						+ "|october|november|december|[, ]{1}[12]{1}[09]{1}[0-9]{2}");
//		NLP.printListOfStringArray("toc -- listStartIdxAndTablename", listStartIdxAndtablename);
		double endTime = System.currentTimeMillis();
		double duration = (endTime - startTime) / 1000;
		List<String[]> listStartIdxAndColumnHeadings = nlp.getAllStartIdxLocsAndMatchedGroups(text,
				patternColumnHeadingSimple);
//		NLP.printListOfStringArray("toc - patternColumnHeadingSimple - listStartIdxAndColumnHeadings",
//				listStartIdxAndColumnHeadings);

		startTime = System.currentTimeMillis();
		duration = (startTime - endTime) / 1000;
		// System.Xut.println("duration listStartIdxAndColumnHeadings="+duration);

		List<String[]> listStartIdxAndPageNo = nlp.getAllStartIdxLocsAndMatchedGroups(text, patternTocPageNumber);
//		NLP.printListOfStringArray("listStartIdxAndPageNo", listStartIdxAndPageNo);
		endTime = System.currentTimeMillis();
		duration = (endTime - startTime) / 1000;
		// System.Xut.println("duration listStartIdxAndPageNo="+duration);

		int sIdxTn = 0, sIdxTnPrev = 0, sIdxCh = 0, sIdxChPrev = 0, sIdxPgNo = 0, c = 0;
		String tn, tnPrev = "", tnTmp, ch = "", chPrev = "";

		String tnText = "";
		for (int i = 0; i < listStartIdxAndtablename.size(); i++) {
			sIdxTn = Integer.parseInt(listStartIdxAndtablename.get(i)[0]);
			tn = listStartIdxAndtablename.get(i)[1];
			// tnText = listStartIdxAndtablename
//			NLP.pwNLP.append(NLP.println("before continue tn=", tn));
			if (nlp.getAllIndexEndLocations(tn, Pattern.compile(
					"(?ism)(cash fl|balance sh|changes|sharehold|stockhold|liabili|income|operation|earning|loss)"))
					.size() < 1 || tn.contains("balance") || tn.contains("cash") || tn.contains("operation")
					|| tn.contains("statement") || tn.contains("income") || tn.contains("income"))
				// if lower cash it is not a match.
				continue;
			// will grab all text on same line as tn that is prior to start of
			// tablename/tn and append to tn.
			// System.Xut.println("1a tn==="+tn);
			tnTmp = text.substring(Math.max(0, sIdxTn - 150), sIdxTn + tn.length()).replaceAll("\r\n", "\r")
					.replaceAll("\r\r", "\r");
			tnTmp = tnTmp.substring(0, tnTmp.length());
			// System.Xut.println("tnTmp="+tnTmp);

			if (tnTmp.lastIndexOf("\r") > 0) {
				tnTmp = tnTmp.substring(tnTmp.lastIndexOf("\r"));
				// System.Xut.println("2a tnTmp=="+tnTmp+"|");
				tnTmp = tnTmp.replaceAll("\r|\n|\t", "").trim();
				// System.Xut.println("2 tnTmp=="+tnTmp+"|");
				tn = tnTmp;
			}

			// will grab fuller tn to the extent TableNamePattern exists
			if (nlp.getAllMatchedGroups("  " + tn + "   ", TableParser.TableNamePattern).size() > 0) {
//				NLP.pwNLP.append(NLP.println("@getPairedGroupsCloseToEachOther text to match tn=", tn));
				tn = nlp.getAllMatchedGroups("  " + tn + "   ", TableParser.TableNamePattern).get(0);
//				NLP.pwNLP.append(NLP.println("@getPairedGroupsCloseToEachOther tableName pattern matched - tn=", tn));
			}
			// if for full line tn is on there's no tn match - then continue.
			// Issue here is part of it may be on next line - maybe for that I
			// do search and replace?
			else
				continue;

			// System.Xut.println("tnTmp.len - tn.len=="+(tnTmp.replaceAll("(?ism)("
			// + replCleanupAftTn + ")", "").length()-tn.length()));
			// System.Xut.println("tmpTn=" + tnTmp + "\rtn=" + tn);
			// if tnTmp is longer then tn - then a word found in text prior to
			// tn that isn't generally part of a tn - so continue.
			if (tnTmp.replaceAll("(?i)condensed|consolidated|statements? of", "").length()
					- tn.replaceAll("(?i)condensed|consolidated|statements? of", "").length() > 3)
				continue;

//			NLP.pwNLP.append(NLP.println("added text from start of line to start of tn to tn. new tn=====", tn + "|"));

//			NLP.pwNLP.append(NLP.println("after continue tn=", tn));
			// above to exclude f/p such as financial statements which causes
			// pairing to not - see eg:
			// https://www.sec.gov/Archives/edgar/data/785956/000078595602000002/r10q1202.txt
			boolean f = false, fprev = false, foundPageNumber = false;
			// find if page number is close to tn - if not continue.
			for (int n = 0; n < listStartIdxAndPageNo.size(); n++) {
				sIdxPgNo = Integer.parseInt(listStartIdxAndPageNo.get(n)[0]);
				if (sIdxPgNo - sIdxTn < 150) {
					foundPageNumber = true;
				}
			}

//			NLP.pwNLP.append(NLP.println("sIdxTn=", sIdxTn + " sIdxTnPrev=" + sIdxTnPrev
//					+ "\rlistStartIdxAndColumnHeadings.size=" + listStartIdxAndColumnHeadings.size() + " list #=" + c));
			if (sIdxTn - sIdxTnPrev < 400 && sIdxTnPrev != 0 /*
																 * && sIdxPgNo-sIdxTn <100
																 */) {
				// found two patterns close to each (two tablenames).
				// Grab block of text after and including pair which I can the
				// reformat as needed.

				chPrev = "";
				ch = "";

				for (c = 0; c < listStartIdxAndColumnHeadings.size(); c++) {
					sIdxCh = Integer.parseInt(listStartIdxAndColumnHeadings.get(c)[0]);
//					NLP.pwNLP.append(NLP.println("listStartIdxAndColumnHeadings.get(c)[0]==",
//							listStartIdxAndColumnHeadings.get(c)[0]));

					if (sIdxCh < sIdxTnPrev)
						continue;
//					NLP.pwNLP.append(NLP.println("sIdx>Ch>sIdxTnPrev - sIdxCh=", sIdxCh + " sIdxTnPrev=" + sIdxTnPrev
//							+ "\r" + "sIdxCh < sIdxTn. sIdxTn=" + sIdxTn + " sIdx w/n100 after sIdxTnPrev?"));

					if (sIdxCh > sIdxTnPrev && sIdxCh < sIdxTn && (sIdxCh - sIdxTnPrev) < 100 && sIdxTnPrev != 0) {
						sIdxChPrev = sIdxCh;
//						NLP.pwNLP.append(NLP.println("set sIdxChPrev = sIdxCh)", ""));
					}

//					NLP.pwNLP.append(NLP.println("sIdxCh - sIdxTn > 500 " + "|| Math.abs(sIdxCh - sIdxTn) > 100 "
//							+ "|| Math.abs(sIdxCh - sIdxChPrev) > 500 - then BREAK;", ""));

					if (sIdxCh - sIdxTn > 500 || Math.abs(sIdxCh - sIdxTn) > 100
							|| Math.abs(sIdxCh - sIdxChPrev) > 400) {
//						NLP.pwNLP.append(NLP.println("IT FRIGGIN BROKE ", ""));
						continue;
					}

//					NLP.pwNLP.append(NLP.println("1 c=" + c + " tnPrev=", tnPrev + "\rchPrev=" + chPrev + "|end"));
//					NLP.pwNLP.append(NLP.println("1 eIdxTnPrev=", sIdxTnPrev + " eIdxChPrev=" + sIdxChPrev));
//					NLP.pwNLP.append(NLP.println("1 tn=", tn + "\rch=" + ch + "|end"));
//					NLP.pwNLP.append(NLP.println("1 eIdxTn=", sIdxTn + " eIdxCh=" + sIdxCh + "\r\r"));

					// 1 c=0 tnPrev==Consolidated Balance Sheets
					// chPrev=|end||END
					// 1 eIdxTnPrev==433 eIdxChPrev=466||END
					// 1 tn==Consolidated Statements of Operations
					// ch=|end||END
					// 1 eIdxTn==515 eIdxCh=466

					// use end of tn to pick-up start of Ch
					chPrev = text.substring(sIdxTnPrev);
//					NLP.pwNLP.append(NLP.println("2 chPrev.indexOf r=", chPrev.indexOf("\r") + ""));
					if (chPrev.indexOf("\r") > 0) {
						String nextline = "", str = chPrev.substring(chPrev.indexOf("\r"));
						chPrev = chPrev.substring(0, chPrev.indexOf("\r"));
//						NLP.pwNLP.append(NLP.println("3 chPrev=", chPrev));
						// System.Xut.println("str="+str);

						String[] strSplit = str.split("\r\n|\r|\n");
						for (int b = 0; b < 3; b++) {
							if (strSplit[b].length() < 1)
								continue;
							// ck next 2 lines
//							NLP.pwNLP.append(NLP.println("strSplit[b]=", strSplit[b]));
							if (nlp.getAllIndexEndLocations(strSplit[b], patternTableNameSimple).size() > 0 || nlp
									.getAllIndexEndLocations(strSplit[b], patternColumnHeadingSimple).size() < 1) {
								break;
							}
							// - if page number found - don't break but stop
							// loop and parse till right before page number

							nextline = nextline + " " + strSplit[b].trim();
							if (nlp.getAllIndexEndLocations(strSplit[b].trim(), patternTocPageNumber).size() > 0) {
								break;
							}
						}
						chPrev = chPrev + " " + nextline.trim();
//						NLP.pwNLP.append(NLP.println("3 chPrev=", chPrev));
					}

					if (!f) {
						ch = text.substring(sIdxTn);
						// System.Xut.println("2 ch="+ch);

						if (ch.indexOf("\r") > 0) {
							String nextline = "", str = ch.substring(ch.indexOf("\r"));
							ch = ch.substring(0, ch.indexOf("\r"));
//							NLP.pwNLP.append(NLP.println("ch=", ch));
							// System.Xut.println("str=" + str);

							String[] strSplit = str.split("\r\n|\r|\n");
							for (int b = 0; b < 3; b++) {
								if (strSplit[b].length() < 1)
									continue;
								// ck next 2 lines
								// System.Xut.println("strSplit[b]="+strSplit[b]);
								if (nlp.getAllIndexEndLocations(strSplit[b], patternTableNameSimple).size() > 0 || nlp
										.getAllIndexEndLocations(strSplit[b], patternColumnHeadingSimple).size() < 1) {
									break;
								}
								// - if page number found - don't break but stop
								// loop and parse till right before page number

								nextline = nextline + " " + strSplit[b].trim();
								if (nlp.getAllIndexEndLocations(strSplit[b].trim(), patternTocPageNumber).size() > 0) {
									break;
								}
							}
							ch = ch + " " + nextline.trim();
//							NLP.pwNLP.append(NLP.println("3 ch=", ch));
						}
						f = true;
						break;
					}
					// if(ch.length()>1 && chPrev.length()>1)
					// break;
				}

				// System.Xut.println("ch.len=" + ch.length() + " chPrev="
				// + chPrev.length() + " tnPrev=" + tnPrev + "\rchPrev="
				// + chPrev);

				if (ch.length() > 0 && chPrev.length() > 0) {

					// TODO: at start of tn - market xxzz - then for captured
					// text blox move back 4 words (after replace hard returns
					// and double ws). Then run tableName pattern matcher to get
					// full tablename.

					tn = tn.replaceAll("[ ]+", " ");
					ch = ch.replaceAll("[ ]+", " ");

					String[] ary = { tn, ch, foundPageNumber + "" };
//					NLP.pwNLP.append(NLP.println("map.put ary=tn,ch,foundPageNumber", Arrays.toString(ary)));
					map.put(sIdxTn, ary);

					tnPrev = tnPrev.replaceAll("[ ]+", " ");
					chPrev = chPrev.replaceAll("[ ]+", " ");
					String[] ary2 = { tnPrev, chPrev, foundPageNumber + "" };
//					NLP.pwNLP.append(NLP.println("map.put ary2=tnPrev,chPrev,foundPageNumber", Arrays.toString(ary2)));
					map.put(sIdxTnPrev, ary2);

				}
			}

			tnPrev = tn;
			sIdxTnPrev = sIdxTn;
		}
		// find 2 tablenames that occur w/n 400 of each other.
		// then see if each has a month and year value (long or mm/dd/yyyy) that
		// starts w/n 50 of end of each tablename

		endTime = System.currentTimeMillis();
		duration = (endTime - startTime) / 1000;
		// System.Xut.println("duration to get paired groups (toc)=" +
		// duration);
		return map;

	}

	public static List<String[]> mergeWithTableOfContentsEndDatePeriods(List<String[]> listEdtPer,
			List<String[]> listTocEdtPerTotColsTblnm, String tableNameShort, String tableNameLong,
			boolean columnOneAndTwoSameYear, String allColText) {

		NLP nlp = new NLP();

		boolean edtPerComplete = true;
		for (int z = 0; z < listEdtPer.size(); z++) {
			if (tableNameShort.length() == 2) {
				// System.Xut.println("listEdtPer ary="
				// + Arrays.toString(listEdtPer.get(z)));
				if (nlp.getAllIndexEndLocations(listEdtPer.get(z)[0],
						Pattern.compile("[12]{1}[09]{1}[\\d]{2}-[01]{1}[\\d]{1}-[0-3]{1}[\\d]{1}")).size() < 1) {

					edtPerComplete = false;
					break;

				}
				if (nlp.getAllIndexEndLocations(listEdtPer.get(z)[1], Pattern.compile("3|6|9|12")).size() < 1) {
					edtPerComplete = false;
					break;
				}
			}
		}

		String tmpTsShort = "";
		int cntToc = 0, tocTblNo = -1;
		List<String[]> listEdtPerToc2 = new ArrayList<String[]>();
		// if not complete see if I can get from table of contents. This will
		// determine if there's a match of toc edt/per with table edt/per
		if (!edtPerComplete) {
			for (int y = 0; y < listTocEdtPerTotColsTblnm.size(); y++) {
				// listTocEdtPerTotColsTblnm=edt[0],per[1],tc[2],tn[3],tnShort[4],col[5],tocTblNo[6],
				// pgNo[7],tsShort[8] (and ALL tocs!).

//				NLP.pwNLP.append(NLP.println("@nlp listTocEdtPerTotColsTblnm=",
//						Arrays.toString(listTocEdtPerTotColsTblnm.get(y))));

//				NLP.pwNLP.append(NLP.println("@nlp toc tableNameShort=", listTocEdtPerTotColsTblnm.get(y)[4]));

//				NLP.pwNLP.append(NLP.println("@nlp toc tableNameLong=\r",
//						listTocEdtPerTotColsTblnm.get(y)[3].toUpperCase().replaceAll("  ", " ").trim()));
//				NLP.pwNLP.append(
//						NLP.println("tableNameLong=\r", tableNameLong.toUpperCase().replaceAll("  ", " ").trim()));

				tableNameLong = tableNameLong.toUpperCase().replaceAll("  ", " ").trim().replaceAll("(?i)STATEMENTS",
						"STATEMENT");

//				NLP.pwNLP.append(NLP.println("@nlp listTocEdtPerTotColsTblnm.get(y)[2] totcol=",
//						listTocEdtPerTotColsTblnm.get(y)[2]));

				// System.Xut.println("listEdtPer.size=" + listEdtPer.size());
				// if same tn,same total cols at col1 then break and pop
				// tocEdtPer list

				if (listTocEdtPerTotColsTblnm.get(y)[4].equals(tableNameShort)
						&& listTocEdtPerTotColsTblnm.get(y)[3].toUpperCase().replaceAll("  ", " ")
								.replaceAll("(?i)STATEMENTS", "STATEMENT").trim()
								.equals(tableNameLong.toUpperCase().replaceAll("  ", " ").trim())
						&& Integer.parseInt(listTocEdtPerTotColsTblnm.get(y)[2]) == listEdtPer.size()
						&& Integer.parseInt(listTocEdtPerTotColsTblnm.get(y)[5]) == 1) {
					tocTblNo = Integer.parseInt(listTocEdtPerTotColsTblnm.get(y)[6]);
					tmpTsShort = listTocEdtPerTotColsTblnm.get(y)[8];
//					NLP.pwNLP.append(NLP.println("@nlp tablenameshorts are the same", ""));
//					NLP.pwNLP.append(NLP.println("@nlp tableNameLong=", tableNameLong));
//					NLP.pwNLP.append(NLP.println("@nlp toc tableNameLong=", listTocEdtPerTotColsTblnm.get(y)[3]
//							.toUpperCase().replaceAll("  ", " ").replaceAll("(?i)STATEMENTS", "STATEMENT").trim()));
//					NLP.pwNLP.append(NLP.println("@nlp tmpTsShort=", tmpTsShort));
//					NLP.pwNLP.append(NLP.println("@nlp total data cols=", listEdtPer.size() + ""));
//					NLP.pwNLP.append(NLP.println("@nlp toc total cols=", listTocEdtPerTotColsTblnm.get(y)[2]));
					break;
				}
			}

			// found tbl in listTocEdtPerTotColsTblnm. Now I pull just the
			// relevant rows from list to create tocEdtPer list to compare
			// against listEdtIdx.
			for (int y = 0; y < listTocEdtPerTotColsTblnm.size(); y++) {
				if (tocTblNo == Integer.parseInt(listTocEdtPerTotColsTblnm.get(y)[6])) {
					String[] ary = { listTocEdtPerTotColsTblnm.get(y)[0], listTocEdtPerTotColsTblnm.get(y)[1], };
					listEdtPerToc2.add(ary);
//					NLP.pwNLP.append(NLP.println("@nlp ary=", Arrays.toString(ary)));
				}
			}

			if (null != listEdtPerToc2 && null != listEdtPer && listEdtPerToc2.size() == listEdtPer.size()) {

				// ck if same yrs per in listEdtPer with toc. Then it passes
				// thru merge mergeTablesentenceEdtPerWithEdtPerIdx. Will also
				// try toc edt/per in reverse order if original fails

				List<String[]> listEdtPerTocReverse = new ArrayList<String[]>();
				List<String[]> listTocTmp = new ArrayList<>();
				for (int y = listEdtPerToc2.size() - 1; y >= 0; y--) {
					String[] ary = { listEdtPerToc2.get(y)[0], listEdtPerToc2.get(y)[1] };
					listTocTmp.add(ary);
				}
				listEdtPerTocReverse = listTocTmp;

//				NLP.printListOfStringArray("listEdtPerToc2", listEdtPerToc2);
				List<String[]> listTmp = NLP.checkTableOfContentsEdtPerAgainstTableEdtPer(listEdtPerToc2, listEdtPer,
						tableNameShort);

				if (listTmp != null) {
					listEdtPerToc2 = listTmp;
				}

				if (null == listTmp) {

					listTmp = NLP.checkTableOfContentsEdtPerAgainstTableEdtPer(listEdtPerTocReverse, listEdtPer,
							tableNameShort);

					if (listTmp != null) {
						listEdtPerToc2 = listTmp;
					}
				}

//				NLP.pwNLP.append(NLP.println(
//						"B NLP.mergeTablesentenceEdtPerWithEdtPerIdx... listEdtPerToc - columnOneAndTwoSameYear="
//								+ columnOneAndTwoSameYear + " tmpTsShort=",
//						tmpTsShort));
//				NLP.printListOfStringArray("NLP.mergeWith...listEdtPer", listEdtPer);
//				NLP.printListOfStringArray("NLP.mergeWith...listEdtPerToc2", listEdtPerToc2);

				listEdtPer = NLP.mergeTablesentenceEdtPerWithEdtPerIdx(listEdtPer, listEdtPerToc2, tmpTsShort,
						listEdtPer.size(), columnOneAndTwoSameYear, allColText);
			}
		}

//		NLP.printListOfStringArray("NLP.mergeWith...final listEdtPer=", listEdtPer);

		return listEdtPer;

	}

	public static List<String[]> checkTableOfContentsEdtPerAgainstTableEdtPer(List<String[]> listEdtPerToc2,
			List<String[]> listTableEdtPer, String tableNameShort) {

		int yrIdx = 0;
		String perToc = "", perIdx2 = "", edtToc = "";
		int moToc = -1, dayToc = -1;
		int yrToc = 0;

//		NLP.printListOfStringArray("listEdtPerToc2", listEdtPerToc2);
		boolean failed = false, edtTocFound = false, perTocFound = false;
		List<String[]> listToc = new ArrayList<>();
		for (int y = 0; y < listEdtPerToc2.size(); y++) {
			if (failed)
				continue;
			edtTocFound = false;
			perTocFound = false;
			// make sure yrs match (unless jan or dec) and pers if
			// present. Here I just create the listEdtPerToc and then
			// run ck later. if mo=jan & day < 6 for toc and yr = (edt
			// yr+1)-then set toc edt to yr-1 w/ moday=12-31. In
			// addition, see if toc yr order is descending or ascending
			// and same for edt Idx

			if (listTableEdtPer.get(y)[0].length() > 3 && listEdtPerToc2.get(y)[0].length() > 3) {
				if (listTableEdtPer.get(y)[0].substring(0, 4).contains("-")) {
					yrIdx = 0;
				} else {
					String str = listTableEdtPer.get(y)[0].replaceAll("[,-]", "");
					if (str.length() > 4)
						str = str.substring(0, 4);
					else
						str = "";

					if (str.length() < 1) {
						yrIdx = 0;
					} else {
						yrIdx = Integer.parseInt(str);
					}
//					NLP.pwNLP.append(NLP.println("yrIdx=", yrIdx + ""));
				}
				if (listEdtPerToc2.get(y)[0].substring(0, 4).contains("-")) {
					yrToc = 0;
				} else {
					yrToc = Integer.parseInt(listEdtPerToc2.get(y)[0].substring(0, 4));
//					NLP.pwNLP.append(NLP.println("yrToc=", yrToc + ""));
				}

				if (listEdtPerToc2.get(y)[0].replaceAll("-", "").length() > 5) {
					moToc = Integer.parseInt(listEdtPerToc2.get(y)[0].substring(5, 7));
//					NLP.pwNLP.append(NLP.println("moToc=", moToc + ""));
				}
				if (listEdtPerToc2.get(y)[0].replaceAll("-", "").length() > 7) {
					dayToc = Integer.parseInt(listEdtPerToc2.get(y)[0].substring(8, 10));
//					NLP.pwNLP.append(NLP.println("dayToc=", dayToc + ""));
				}

//				NLP.pwNLP.append(NLP.println("@nlp yrIdx=",
//						yrIdx + " yrToc=" + yrToc + " moToc=" + moToc + " dayToc=" + dayToc
//								+ " listEdtPerToc.get(y)[0]=" + listEdtPerToc2.get(y)[0] + " edtTocFound "
//								+ edtTocFound));
			}

			if (listTableEdtPer.get(y)[0].replaceAll("-", "").length() == 4
					&& (yrToc - yrIdx == 1 && moToc == 1 && dayToc < 7) && !edtTocFound) {
//				NLP.pwNLP.append(NLP.println(
//						"@nlp onlyYear and 1 yr diff b/w yrIdx-yrToc and month is Jan for toc. set toc yr= yr idx and mo=1231",
//						""));
				edtToc = yrIdx + "-" + "12-31";
				edtTocFound = true;
			}

			if (listTableEdtPer.get(y)[0].replaceAll("-", "").length() == 4 && yrIdx - yrToc == 1 && moToc == 12
					&& dayToc > 24 && !edtTocFound) {
//				NLP.pwNLP.append(NLP.println(
//						"@nlp onlyYear and 1 yr diff b/w yrIdx-yrToc and month is Dec for toc. Set toc mo=jan 01 and toc yr=yrIdx",
//						""));
				edtToc = yrIdx + "-" + "01-01";
				edtTocFound = true;
			}

//			NLP.pwNLP.append(NLP.println("@nlp edtTocFound=", edtTocFound + " yrIdx=" + yrIdx + " yrToc=" + yrToc));

			if (yrIdx == yrToc && !edtTocFound) {
				edtToc = listEdtPerToc2.get(y)[0];
				edtTocFound = true;
//				NLP.pwNLP.append(NLP.println("@nlp edtToc=", edtToc + ""));

			}
			// at this point if edtToc not found - loop fails and
			// listEdtPerToc not sent to
			// mergeTablesentenceEdtPerWithEdtPerIdx method
			if (!edtTocFound) {
				break;
			}

			if (tableNameShort.equals("bs")) {
				perTocFound = true;
			}

			if (!tableNameShort.equals("bs")) {
				perToc = listEdtPerToc2.get(y)[1];
				perIdx2 = listTableEdtPer.get(y)[1];

				if (perIdx2.equals("0")) {
					perTocFound = true;
				}
				// requires match evn if zero for perToc!
				else if (!perIdx2.equals(perToc)) {
					perTocFound = false;
				} else if (perIdx2.equals(perToc)) {
					perTocFound = true;
				}
			}

			// System.Xut.println("ascendingYrIdx=" + ascendingYrIdx);
			// System.Xut.println("ascendingYrToc=" + ascendingYrToc);

//			NLP.pwNLP.append(NLP.println("@nlp listEdtPerToc edt=", listEdtPerToc2.get(y)[0]));
//			NLP.pwNLP.append(NLP.println("@nlp listEdtPer edt=", listTableEdtPer.get(y)[0]));
//			NLP.pwNLP.append(NLP.println("@nlp listEdtPerToc per=", listEdtPerToc2.get(y)[1]));
//			NLP.pwNLP.append(NLP.println("@nlp listEdtPer per=", listTableEdtPer.get(y)[1]));

//			NLP.pwNLP.append(NLP.println("@nlp edtTocFound==", edtTocFound + " perTocFound=" + perTocFound));
			if (!edtTocFound || !perTocFound) {
				failed = true;
				break;
			}
			String[] ary = { edtToc, perToc };
			listToc.add(ary);
//			NLP.pwNLP.append(
//					NLP.println("@nlp added to table of content edtper - edtToc=", edtToc + " perToc=" + perToc));
		}

		// make the above a method wherein I run with
		// listEdtPerTocReverse second instead of listEdtPerToc2 when returned
		// listToc is null or size=0.

//		NLP.printListOfStringArray("A NLP.mergeWith...listToc", listToc);

		// listEdtPerToc did not fail so merge with listEdtPer
		if (!failed && listToc.size() == listTableEdtPer.size()) {
			return listToc;
		}

		else
			return null;
	}

	public static List<String[]> getTableOfContents(String origText, String acc)
			throws IOException, ParseException, SQLException {

		NLP nlp = new NLP();
		TableParser tp = new TableParser();
		// listTocEdtPerTblNmTsShort records for each edt/per (each col) the
		// edt/per culled from toc and repeats the tablename, tsShort and pgNo.
		// listTocEdtPerTblNmTsShortPg.size=totalCols.
		List<String[]> listTocEdtPerTotColsTblnm = new ArrayList<String[]>();
		// #return List<String[]>edtPer, global: tsShort,tsPattern,global: tocTn

		String ts = "", tsPattern = "", tsShort = "";

		double startTime = System.currentTimeMillis();
		Map<Integer, String[]> map = getPairedGroupsCloseToEachOther(origText);
		double endTime = System.currentTimeMillis();
		double duration = (endTime - startTime) / 1000;
		// System.Xut.println("getPairedGroupsCloseToEachOther duration="
		// + duration);

		NLP.printMapIntStringAry("getPairedGroupsCloseToEachOther returned map\r", map);

		int tocNo = 0;
		for (Map.Entry<Integer, String[]> entry : map.entrySet()) {
			int pgNo = -1;
			String pgNoStr = "";
//			NLP.pwNLP.append(NLP.println("before replacement", ts));
			ts = entry.getValue()[1].replaceAll("\\. ?\\. ?", "  ").replaceAll(" \\. ", "   ");
//			NLP.pwNLP.append(NLP.println("after replacement", ts));

			String[] tsSplit = ts.split("   |\t");
//			NLP.pwNLP.append(NLP.println("tsSplit=", Arrays.toString(tsSplit)));

			if (tsSplit.length > 0) {
				ts = tsSplit[0].replaceAll("'", "");
			}
			if (tsSplit.length > 1) {
				pgNoStr = tsSplit[tsSplit.length - 1].replaceAll("'", "").trim();
			}

			// NLP.pwNLP.append(NLP.println("pgNoStr=", pgNoStr));
			// NLP.pwNLP.append(NLP.println("aa ts=", ts));

			// System.Xut.println("ts=="+ts);
			List<String> list = NLP.parseTableSentence(ts, "", true, entry.getValue()[0].replaceAll("'", "") + "\r",
					true);
			String tn = entry.getValue()[0].replaceAll("'", "");
			String foundPageNumber = entry.getValue()[2];

			if (foundPageNumber.equals("true") && nlp.isNumeric(pgNoStr) && !pgNoStr.contains(".")) {
				pgNo = Integer.parseInt(pgNoStr.replaceAll("[,\\(\\)]", ""));
			}

			int eIdx = entry.getKey();
			// System.Xut.println("list.size=" + list.size());
			if (list != null) {

				for (int i = 0; i < list.size(); i++) {
					// System.Xut.println(" string=" + list.get(i));
					if (i == 1) {
						tsShort = list.get(i);
//						NLP.pwNLP.append(NLP.println("tsShort", tsShort));
					}
					if (i == 2) {

						tsPattern = list.get(2).replaceAll("(?i)\\|\\dP:PERIODS? END[INGED]{2,3}(?=\\|)", "");

//						NLP.pwNLP.append(NLP.println("tsPattern=", tsPattern));
						String[] tsPatternSplit = tsPattern.split("\\|");
						// System.Xut.println("tsPattern==" + tsPattern
						// + "\rtsPatternSplit created="
						// + Arrays.toString(tsPatternSplit) + " tsShort="
						// + tsShort);
						// System.Xut.println("tsPattern=" + tsPattern);

						List<String[]> listEdtPer = NLP.getTableSentEdtPerFromPattern(tsPatternSplit, tsShort, ts);

						// System.Xut.println("2tsPattern=" + tsPattern);
						// tsPattern = NLP
						// .getMonthYearPeriodFromTSpatternFormatted(tsPattern);
						tsPattern = tsPattern.replaceAll("\\|0000", "").replaceAll("'", "");
//						NLP.pwNLP.append(NLP.println("final tsPattern=", tsPattern));
						// System.Xut.println("3tsPattern=" + tsPattern);
						// mysql toc accno,pg#,tableName,ts,tsShort,tsPattern,c1
						// edt/p to c6

						String tnShort = tp.getTableNameShort(tn);
						for (int c = 0; c < listEdtPer.size(); c++) {
							// System.Xut.println("('" + acc + "','" + tocNo
							// + "','" + tnShort + "','" + (c + 1) + "','"
							// + listEdtPer.get(c)[1] + "','"
							// + listEdtPer.get(c)[0] + "','"
							// + listEdtPer.size() + "','" + tsShort
							// + "','" + tn + "','" + ts + "','"
							// + tsPattern + "')\r,");

							if (nlp.getAllIndexEndLocations(listEdtPer.get(c)[0],
									Pattern.compile("[12]{1}[09]{1}[\\d]{2}-[01]{1}[\\d]{1}-[0-3]{1}[\\d]{1}"))
									.size() > 0) {
								String ary[] = { listEdtPer.get(c)[0], listEdtPer.get(c)[1], listEdtPer.size() + "", tn,
										tnShort, (c + 1) + "", tocNo + "", pgNo + "", tsShort, ts };
								listTocEdtPerTotColsTblnm.add(ary);
							}

							// sb.append("('" + acc + "','" + eIdx + "','" +
							// tocNo
							// + "','" + pgNo + "','" + tnShort + "','"
							// + (c + 1) + "','" + listEdtPer.get(c)[1]
							// + "','" + listEdtPer.get(c)[0] + "','"
							// + listEdtPer.size() + "','" + tsShort
							// + "','" + tn + "','" + ts + "','"
							// + tsPattern + "')\r,");
						}

						// if (listEdtPer.size() == 0) {
						// sb.append("('" + acc + "','" + eIdx + "','" + tocNo
						// + "','" + pgNo + "','" + tnShort + "','"
						// + 0 + "','" + "','" + "','" + 0 + "','"
						// + tsShort + "','" + tn + "','" + ts + "','"
						// + tsPattern + "')\r,");
						// }

						tocNo++;
					}
				}
			}

			// System.Xut.println(" key|" + entry.getKey() + "| val|"
			// + Arrays.toString(entry.getValue()));
		}
		// if (sb.toString().length() > 100) {
		// String query = sb.toString().substring(0,
		// sb.toString().length() - 1)
		// + ";";
		// MysqlConnUtils.executeQuery(query);
		// }

		return listTocEdtPerTotColsTblnm;
	}

	public static String[] getAry(boolean reverse, double mp, String word1, String word2) {

		if (!reverse) {
			String[] ary = { mp + "", word1 + " " + word2 };
//			NLP.pwNLP.append(NLP.println("!reverse - mp, small word merged with large word ary", Arrays.toString(ary)));
			return ary;
		} else {
			String[] ary = { mp + "", word2 + " " + word1 };
//			NLP.pwNLP.append(NLP.println("reverse - mp, small word merged with large word ary", Arrays.toString(ary)));
			return ary;
		}

	}

	public static String fullyJustifiedTextMadeLeftJustified(String text) {
		NLP nlp = new NLP();

		Pattern pattern = Pattern.compile("[a-z]{3}[ ]{2,3}[a-z]{3}");
		Pattern pattern2 = Pattern.compile("[a-z]{3} [a-z]{3}");

		List<Integer> list = nlp.getAllIndexStartLocations(text, pattern);
		List<Integer> list2 = nlp.getAllIndexStartLocations(text, pattern2);
		// System.Xut.println("list.size=" + list.size());
		// System.Xut.println("ab list2.size=" + list2.size());

		double l1 = list.size();
		double l2 = list2.size();

		if (l1 / l2 > .1) {
			// System.Xut.println("found=" + (l1 / l2));
			text = text.replaceAll("(?i)(?<=[a-z,;:\\.\"\\(\\)]{1})[ ]{2,3}(?=[a-z,:;\"\\)\\(]){1}", " ");
		}

		return text;
	}

	public static List<String> providedClause(String text) {

		NLP nlp = new NLP();
		List<String> listProvided = new ArrayList<String>();

		Pattern patternProvided = Pattern.compile("[,;]{1}(?= provided(,?( however,?))? that)");
		List<Integer> list = nlp.getAllIndexStartLocations(text, patternProvided);
		if (list.size() == 1) {
			listProvided.add(text.substring(0, list.get(0) + 1));
			listProvided.add(text.substring(list.get(0) + 1, text.length()));
		}

		return listProvided;
	}

	public static void main(String[] arg) throws IOException, SQLException, ParseException {
		NLP nlp = new NLP();
		GoLaw gct = new GoLaw();
		String text = "hello .pdf";
		text = text.replaceAll("(?ism)(\\.)(pdf|jpeg|jpg|tif)", "xxPD$2");
		System.out.println(text);
//		Pattern patternProvided = Pattern.compile("[,;]{1}(?= provided(;?,?( however,?))? that)");
//		String text = "\"Equity Offering\" means any public or private sale of common stock or Preferred Stock of the Issuer or any of its direct or indirect parent companies (excluding Disqualified Stock), other than:\r\n"
//				+ "\r\n"
//				+ "(1) public offerings with respect to the Issuer's or any direct or indirect parent company's common stock registered on Form S-8;\r\n"
//				+ "\r\n" + "(2) issuances to any Subsidiary of the Issuer; and\r\n" + "\r\n"
//				+ "(3) any such public or private sale that constitutes an Excluded Contribution.";
////		System.out.println("provided==" + nlp.getAllMatchedGroups(text, patternProvided).get(0));
//
////		System.out.println("para leadin==" + GC_Tester.getLeadInPara(text));
//		List<String> lst = providedClause(text);
//		NLP.printListOfString("lst=", lst);
//		List<String> listSent = gct.getSolrSentence(text);
//		listSent.add(text);
////		NLP.printListOfString("", listSent);
//		PrintWriter pw = new PrintWriter(new File("c:/temp/clause.txt"));
//		PrintWriter pwS = new PrintWriter(new File("c:/temp/sent.txt"));
//		// revise getSolrClause to save all clauses found with getAllNumeralClauses to a
//		// map. the n take final results which will be a subset and mark those that
//		// occur
//		// twice as typ=4 and those that occur once as typ=5 and append with 10000
//		// accordingly in solr map.
//
//		// experiment to see if I can run getSolrClause a second time on list returned
//		// by getSolrClause that way I keep getting leadin etc
//
//		System.out.println("start");
//		String str = "";
//		List listSubCl = new ArrayList<>();
//		for (int n = 0; n < listSent.size(); n++) {
//			text = listSent.get(n);
//			pwS.append("sent n=" + n + " " + text + "\r\n");
//			List<String> listCl = gct.getSolrClause(text);
//			// add if roman>0 then insert at very start of clause for clause=0. roman+
//			if (listCl == null || listCl.size() == 0)
//				continue;
//			for (int i = 0; i < listCl.size(); i++) {
//				System.out.println(listCl.get(i) + "||end\r\n");
////				System.out.println("listCl.size=" + listCl.size());
//
//				if (listCl.size() > 0) {
//					str = listCl.get(i).replaceAll("" + "^(leadin=|clause=sssS1s)[\\(\\da-zA-Z\\)]+", "");
////					System.out.println("listCl.get(b).repl==" + str);
//					listSubCl = gct.getSolrClause(str);
//					if (null == listSubCl || listSubCl.size() == 0)
//						continue;
//					for (int c = 0; c < listSubCl.size(); c++) {
////						System.out.println("subclause=" + listSubCl.get(c) + "||end\r\n");
//					}
//				}
//			}
//		}
//		pwS.close();
//		pw.close();

		// semi colon clause not otherwise picked up (such as ; provided -- but also ,
		// provided that or provided however.
	}
}