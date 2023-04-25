package xbrl;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;


public class ContractNLP {


	public static int formtxtPara = 0;

	public static Pattern RestatedPattern = Pattern
			.compile("Restated|RESTATED|Revised|Restated|Adjust|ADJUST|PRO.{1,2}FORMA|Pro.{1,2}Forma|Reported|REPORTED");

	public static Pattern MonthDayYearPattern = Pattern
			.compile("[\\d]{1,2}[\\/-]{1}[\\d]{1,2}[\\/-]{1}[\\d]{2,4}");

	public static Pattern htmlPara = Pattern.compile("(?i)"
			+ "(<p[^>]*>|</p[^>]*>|" + "<div[^>]*>|</div[^>]*>|" + "<BR>)");

	public static Pattern hardReturnTabNumberTabNumber = Pattern
			.compile("[\r\n]{1}[ ]{0,2}\t(?=(\\$?([\\.\\d,]{1,15}|\\d{1,3})[,\\d{3}]{0,15}\t\\$?"
					+ "([\\.\\d,]{1,15}|\\d{1,3})[,\\d{3}]{0,15}))");

	public static Pattern numberBR = Pattern
			.compile("(?<=(\t ?\\$?\\d{1,3}|,[\\d]{3}|\\d\\)|[-]{1,3}))(?i) ?<BR>");

	public static Pattern TRPattern = Pattern
			.compile("(?i)(<tr[^>]*>|</tr[^>]*>)");

	public static Pattern TDPattern = Pattern
			.compile("(?i)(<td[^>]*>|<th[^>]*>)");

	public static Pattern TDWithColspanPattern = Pattern
			.compile("(?i)(<td[^>]*?COLSPAN[^>]*?>|<th[^>]*?COLSPAN[^>]*?>)");

	public static Pattern ExtraHtmlPattern = Pattern
			.compile("(?i)(</?[[font]|[b]|[i]|[u]]*[^>]*>)");

	public static Pattern ExtraHtmlPattern2 = Pattern
			.compile("(?i)(</?p[^>]*>|</td[^>]*>)");
	// - </ (once or not at all - should it be at least once? followed by any of
	// . . . . ending with stuff after *....

	public static final String LineSeparator = System.getProperty(
			"line.separator", "\r\n");

	public Pattern removeHtmlFromPartialHtmlTextExtract = Pattern
			.compile(".*[^<]>");
	// only works where partial text was extracted from html and after
	// stripHtmlTabs is run. will remove remainder such as nt-family: Arial,
	// Helvetica">

	public Pattern DecimalPattern = Pattern
			.compile("(?i)[ \t]*((in |\\$)thousands?|[\\( ]{0,1}000[\\' \\)]{0,2}s|in millions?|million[s] of"
					+ "|\\(.{1,2}000 omit|in billions?|billion[s] of)[ \t]*");

	public static Pattern patternStemmer = Pattern
			.compile("(?i)(ication|dictory|ility|atory|edly|ably|rian|dict|ied|ing|ity|ed|ly|y)$");

	// public Pattern patternContractSentenceStart = Pattern.compile("(?sm)(" +
	// "(?<=([\r\n\t]|[,;] ?[\r\n]|\\. ))[A-Z]{1,}"
	// +
	// "|\\([a-zA-Z\\d]{1,3}\\(([\t]|  )" +
	// ")");

	public static Pattern patternContractSentenceStart = Pattern
			.compile("(?sm)([\r\n] ?(?<=[A-Z\\(])|(?<=\\.)[ \r\n])|(?<=\\:) ?[\r\n]");

	public static Pattern patternContractSentenceEnd = Pattern
			.compile("(?sm)(\\!|\\?|[a-zA-Z\\)]{1}\\.[\r\n ](?![a-z])|([,;:] ?| and ?)[\r\n]|[\r\n])");
	//
	// " +
	// "([A-Za-z]{1}\\.([\r\n]| |[A-Z]{1})|\\;[\r\n])|\\?\\!)

	public static Pattern patternParaReference = Pattern
			.compile("(this|preceding|previous).{1,3}(clause|[Ss]ection"
					+ "|[Pp]aragraph|[Aa]rticle)");

	public static Pattern patternParaMarkerEnd = Pattern
			.compile("(?i)([\\:\\;\\,]{1})( and| or)?$(?! ?[\\p{Alnum}\\p{Punct}])");
	// para marker will ignore patterns w/n para.

	public static Pattern patternParaMarkerParent = Pattern
			.compile("(?i)(:) ?$(?! ?[\\p{Alnum}\\p{Punct}])");
	// para marker will ignore patterns w/n para.

	public static Pattern patternParaNumberMarker = Pattern
			.compile("(\\([A-Za-z\\.\\d]{1,5}\\)|[\\dA-Z]{4}(\\.|[ ]{3}))");

	public static Pattern patternTocPageNumber = Pattern
			.compile("[ \\.\t]{5,}([ABCDEFG]{1}-?)?[\\d]{1,3}[ ]{0,4}(\r|\n|$)");

	
	public Pattern patternAnyVisualCharacter = Pattern
			.compile("[\\p{Alnum}\\p{Punct}]");

	public Pattern patternFoundLowerCaseWordStart = Pattern
			.compile("(\\b[^A-Z\\)\\( ;:])");

	public List<Integer> getSentenceStartLocations(String txt) {
		// en-sent.bin is like a dictionary so it can determine eng sent
		InputStream modelIn = NLP.class.getClassLoader().getResourceAsStream(
				"en-sent.bin");
		List<Integer> locs = new ArrayList<Integer>();
		try {
			SentenceModel model = new SentenceModel(modelIn);
			SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
			Span[] sents = sentenceDetector.sentPosDetect(txt);
			for (Span s : sents) {
				locs.add(s.getStart());
				System.out.println("sentStart==" + s.getStart() + "sentEndt=="
						+ s.getEnd() + "::" + s.getCoveredText(txt));
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
		if (null == text || text.length() < 1 || pattern == null
				|| pattern.toString().length() < 1)
			return idxs;

		Matcher matcher = pattern.matcher(text);
		int idx = 0;
		while (matcher.find()) {
			idx = matcher.start();
			// System.out.println("idxs="+idx+"matcher.group()="+matcher.group());
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
			// System.out.println("idxs="+idx+"matcher.group()="+matcher.group());
			idxs.add(idx);
		}
		return idxs;
	}

	public void printMapStrInt(Map<String, Integer> map) {

		for (Map.Entry<String, Integer> entry : map.entrySet()) {
			System.out.println("key|" + entry.getKey() + "| val|"
					+ entry.getValue() + "|");

		}
	}

	public void printMapIntStr(Map<Integer, String> map) {

		for (Map.Entry<Integer, String> entry : map.entrySet()) {
			System.out.println("key|" + entry.getKey() + "| val|"
					+ entry.getValue() + "|");

		}
	}

	public void printMapIntInt(Map<Integer, Integer> map) {

		for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
			System.out.println("key|" + entry.getKey() + "| val|"
					+ entry.getValue() + "|");

		}
	}

	public void printMapIntIntAry(Map<Integer, Integer[]> map) {

		for (Map.Entry<Integer, Integer[]> entry : map.entrySet()) {
			System.out.println("key|" + entry.getKey() + "| val|"
					+ Arrays.toString(entry.getValue()) + "|");

		}
	}

	public void printMapIntStringAry(Map<Integer, String[]> map) {

		for (Map.Entry<Integer, String[]> entry : map.entrySet()) {
			System.out.println("key|" + entry.getKey() + "| val|"
					+ Arrays.toString(entry.getValue()) + "|");

		}
	}

	public void printMapIntListOfStringAry(Map<Integer, List<String[]>> map) {

		int key;
		for (Map.Entry<Integer, List<String[]>> entry : map.entrySet()) {
			key = entry.getKey();
			for (int i = 0; i < entry.getValue().size(); i++) {
				System.out.println("key=" + key + "| list string array="
						+ Arrays.toString(entry.getValue().get(i)));
			}
		}
	}

	public void printMapIntListOfString(Map<Integer, List<String>> map) {

		int key;
		for (Map.Entry<Integer, List<String>> entry : map.entrySet()) {
			key = entry.getKey();
			for (int i = 0; i < entry.getValue().size(); i++) {
				System.out.println("key=" + key + "| list string array="
						+ entry.getValue().get(i));
			}
		}
	}

	public void printObjectArrays(Object[][] arrayOfArray) {
		System.out.println("printing object array as below:");
		for (Object[] ary : arrayOfArray)
			System.out.print(Arrays.toString(ary));
		System.out.println();
	}

	public void printListOfString(List<String> list) {
		for (int i = 0; i < list.size(); i++) {
			System.out.println("string#=" + i + " string=" + list.get(i));
		}
	}

	public void printListOfInteger(List<Integer> list) {
		for (int i = 0; i < list.size(); i++) {
			System.out.println("list ==" + list.get(i));
		}
	}

	public void printListOfStringArray(List<String[]> list) {

		for (int i = 0; i < list.size(); i++) {
			String[] ary = list.get(i);
			System.out.println("printing ary=" + Arrays.toString(ary));
		}
	}

	public void printListOfStringArrayInReverse(List<String[]> list) {

		for (int i = (list.size() - 1); i >= 0; i--) {
			String[] ary = list.get(i);
			System.out.println("printing ary=" + Arrays.toString(ary));
		}
	}

	public void printListOfIntegerArray(List<Integer[]> list) {

		for (int i = 0; i < list.size(); i++) {
			Integer[] ary = list.get(i);
			System.out.println("printing ary=" + Arrays.toString(ary));
		}
	}

	public void printListOfDoubleArray(List<Double[]> list) {

		for (int i = 0; i < list.size(); i++) {
			Double[] ary = list.get(i);
			System.out.println("printing ary=" + Arrays.toString(ary));
		}
	}

	public void printArray(String[] ary) {
		for (int i = 0; i < ary.length; i++) {
			System.out.println("PRINTING ARY=" + ary[i]);
		}
	}

	public List<String> getAllMatchedGroups(String text, Pattern pattern)
			throws IOException {

		// System.out.println("getAllMatchedGroups text="+text.substring(0,Math.min(100,
		// text.length())));
		// text = Utils.readTextFromFile(""+getTableNamePrefixYearQtr(fileDate)
		// + "regexv2.txt");
		List<String> idxs = new ArrayList<String>();

		if(text==null){
			return idxs;
		}
					
		Matcher matcher = pattern.matcher(text);

		// int idx;
		while (matcher.find()) {
			// idx = matcher.start();
			// if (idx >= 0)
			// System.out.println("matcher.group="+matcher.group());
			idxs.add(matcher.group().replaceAll("\t", " ")
					.replaceAll("  ", " ").trim());
			// NOTE: the replaceAll was added in order to address error that
			// occurs on rare occassion when a tab is present in a pattern that
			// was matched.

			/*
			 * grpCount counts subgroups. this causes problem in column count
			 * for (int i = 1; i <= matcher.groupCount(); i++) { idx =
			 * matcher.start(i); if (idx >= 0) { idxs.add(matcher.group());
			 * System.out.println("idxs::" + idxs); } }
			 */
		}
		// System.out.println("idxs::" + idxs);
		// System.out.println("idx="+idxs);
		return idxs;
		// For the years ended December 31, 1999 and 2000
		// returns 1 of month, 1 of ended and 2 of year patterns.
	}

	public static List<String[]> getAllStartIdxLocsAndMatchedGroups(String text,
			Pattern pattern) throws IOException {

		// System.out.println("getAllMatchedGroups text="+text.substring(0,Math.min(100,
		// text.length())));
		// text = Utils.readTextFromFile(""+getTableNamePrefixYearQtr(fileDate)
		// + "regexv2.txt");

		Matcher matcher = pattern.matcher(text);

		// int idx;
		List<String[]> idxsGrps = new ArrayList<String[]>();
		while (matcher.find()) {
			// idx = matcher.start();
			// if (idx >= 0)
			// System.out.println("matcher.start()"+matcher.start()+"matcher.group="+matcher.group()+"|");
			String[] ary = { matcher.start() + "", matcher.group() };
			idxsGrps.add(ary);
		}
		return idxsGrps;
	}
	
	public static List<String[]> getAllIdxLocsAndMatchedGroups(String text,
			Pattern pattern) throws IOException {

		// System.out.println("getAllMatchedGroups text="+text.substring(0,Math.min(100,
		// text.length())));
		// text = Utils.readTextFromFile(""+getTableNamePrefixYearQtr(fileDate)
		// + "regexv2.txt");

		Matcher matcher = pattern.matcher(text);

		// int idx;
		List<String[]> idxsGrps = new ArrayList<String[]>();
		while (matcher.find()) {
			// idx = matcher.start();
			// if (idx >= 0)
			// System.out.println("matcher.start()"+matcher.start()+"matcher.group="+matcher.group()+"|");
			String[] ary = { matcher.start() + "", matcher.group() };
			idxsGrps.add(ary);
		}
		return idxsGrps;
	}


	

	public Map<Integer, String> getAllIdxLocsAndMatchedGroupsAndAddToList(
			String text, Pattern pattern, Map<Integer, String> mapIntegerString)
			throws IOException {

		// System.out.println("getAllMatchedGroups text="+text.substring(0,Math.min(100,
		// text.length())));
		// text = Utils.readTextFromFile(""+getTableNamePrefixYearQtr(fileDate)
		// + "regexv2.txt");

		Matcher matcher = pattern.matcher(text);

		// int idx;
		while (matcher.find()) {
			// idx = matcher.start();
			// if (idx >= 0)
			// System.out.println("matcher.group="+matcher.group());
			mapIntegerString.put(matcher.start(), matcher.group());
		}
		return mapIntegerString;
	}

	public List<String[]> getAllMidPointLocsAndMatchedGroupAfterRownameEndIdx(
			String text, Pattern pattern, int addBack) throws IOException {

		Matcher matcher = pattern.matcher(text.substring(addBack));
		List<String[]> idxsGrps = new ArrayList<String[]>();
		double mp;
		while (matcher.find()) {
			// idx = matcher.start();
			// if (idx >= 0)
//			System.out.println("matcher.group=" + matcher.group());
			mp = matcher.end() - matcher.group().trim().length() / 2 + addBack;
			String[] ary = { mp + "", matcher.group().trim() };
			idxsGrps.add(ary);
		}
		return idxsGrps;
	}


	public List<String[]> getAllEndIdxMatchedGroupRow(String text,
			Pattern pattern, String row) throws IOException {

		// System.out.println("getAllMatchedGroups text="+text.substring(0,Math.min(100,
		// text.length())));
		// text = Utils.readTextFromFile(""+getTableNamePrefixYearQtr(fileDate)
		// + "regexv2.txt");

		Matcher matcher = pattern.matcher(text);

		List<String[]> idxsGrps = new ArrayList<String[]>();

		while (matcher.find() && matcher.group().length() > 0) {
			// idx = matcher.start();
			// if (idx >= 0)
			// System.out.println("matcher.group="+matcher.group());
			String[] ary = { matcher.end() + "", matcher.group(), row };
			idxsGrps.add(ary);
		}
		return idxsGrps;
	}

	public String keepCellsInSameRow(String html, Pattern startPattern,
			Pattern endPattern) throws FileNotFoundException {

		// <td[^>]*
		// this simply removes all hard returns within start and end pattern

		StringBuffer sb = new StringBuffer();
		// sb.delete(0, sb.toString().length());
		int start = 0, htmlLen = html.length();
		List<Integer> idxStartTrs = getAllIndexStartLocations(html,
				startPattern);
		List<Integer> idxEndTrs = getAllIndexStartLocations(html, endPattern);
		if (idxStartTrs.size() == 0 || idxEndTrs.size() == 0) {
			// System.out.println("no pattern found..");
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
					String htmlTemp = new String(html.substring(idxStartTr,
							endTrLoc));
					htmlTemp = htmlTemp.replaceAll("[\r\n]{1,}|[\\s]{2,}", " ");
					if (startPattern.equals("startTd")
							|| startPattern.equals("startTh")) {
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

	public String getSentence(String html, Pattern startPattern,
			Pattern endPattern) throws IOException {

		StringBuffer sb = new StringBuffer();
		// sb.delete(0, sb.toString().length());
		int start = 0, htmlLen = html.length();
		List<Integer> idxStartTrs = getAllIndexStartLocations(html,
				startPattern);
		List<Integer> idxEndTrs = getAllIndexEndLocations(html, endPattern);
		if (idxStartTrs.size() == 0 || idxEndTrs.size() == 0) {
			// System.out.println("no pattern found..");
			return html;
		}
		int endTrI = 0, endTrLoc = 0;
		for (Integer idxStartTr : idxStartTrs) {
			if (start > idxStartTr)
				continue;
			sb.append(new String(html.substring(start, idxStartTr)));
			System.out.println("s1="
					+ new String(html.substring(start, idxStartTr)));
			// above is identifying JUST the start of the pattern - so we do NOT
			// want to replace anything here!!

			for (Integer eTr = endTrI; eTr < idxEndTrs.size(); eTr++) {
				endTrI++;
				endTrLoc = idxEndTrs.get(eTr) + 1;
				if (endTrLoc <= idxStartTr)
					continue;
				else {
					String htmlTemp = new String(html.substring(idxStartTr,
							endTrLoc));
					htmlTemp = htmlTemp.replaceAll("[\r\n]{1,}|[\\s]{2,}", " ");
					if (startPattern.equals("startTd")
							|| startPattern.equals("startTh")) {
						htmlTemp = htmlPara.matcher(htmlTemp).replaceAll(" ");
						// if <td > <p>hello</p>world</td> it removes the <p
						// (same for <div and <br)
					}
					sb.append(new String(htmlTemp));
					System.out.println("s2=" + new String(htmlTemp));
					break;
				}
			}
			start = endTrLoc;
		}
		String keepCellsTextTogether = (html.substring(start, htmlLen));
		// sb.append(new String(html.substring(start, htmlLen)))
		sb.append(new String(keepCellsTextTogether));
		System.out.println("s3=" + new String(keepCellsTextTogether));
		String temp = sb.toString();
		return temp;
	}

	public String removeAttachments(String text) throws FileNotFoundException {
		// System.out.println("remove attachments....");

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
		// System.out.println("acceptedDate at remove attachment at end::"
		// + acceptedDate);
		// System.out.println("removeAttachment2 subtext 0 to 10:: "
		// + sb.toString().substring(0, 10));
		// PrintWriter tempPw = new PrintWriter(new File("c:/backtest/"
		// + getTableNamePrefixYearQtr(fileDate) + "temp1b.txt"));
		// tempPw.append(sb.toString());
		// tempPw.close();
		return sb.toString();
	}

	public String removeExtraBlanks(String text) throws FileNotFoundException {

		// System.out.println("removeExtraBlanks text200 characters="
		// + text.substring(0, 200));

		PrintWriter tempPw11 = new PrintWriter(new File(
				"c:/backtest/ttemp11.txt"));
		tempPw11.append(text);
		tempPw11.close();

		Pattern lowercaseLetterTablowercaseLetter = Pattern
				.compile("(?<=([a-z]{1}))(\t)(?=([a-z]{1}))");

		PrintWriter tempPw12 = new PrintWriter(new File(
				"c:/backtest/ttemp12.txt"));
		tempPw12.append(text);
		tempPw12.close();

		text = text.replaceAll("[ ]{1,7}\\)", "\\)").replaceAll(
				"(\\([ ]{1,7})", "\\(");
		PrintWriter tempPw12a = new PrintWriter(new File(
				"c:/backtest/ttemp12a.txt"));
		tempPw12a.append(text);
		tempPw12a.close();

		text = text
				.replaceAll("\\|", "\t")
				// replace rare instances where "|" is used as separator
				// .replaceAll(" \t|\t ", "\t")
				.replaceAll("\\. \\.", "\t").replaceAll(" \\. ", "\t")
				.replaceAll("\\.\\$|\\. \\$", "\t")
				;
		PrintWriter tempPw12a1 = new PrintWriter(new File(
				"c:/backtest/ttemp12a1.txt"));
		tempPw12a1.append(text);
		tempPw12a1.close();

		text = text.replaceAll("[\t]+", "\t").replaceAll("[\\.]{2,}\\$", "\t")
				.replaceAll("[\\.]{2,}", " ").replaceAll("[_=]{3,}", "")
				// replace .....
				.replaceAll(" [ ]{2,}", "\t")
				// 3 or more spaces
				.replaceAll(":  ", " ").replaceAll("\\$[ \t]+", "\\$")
		// $<tabs> with $
		;
		PrintWriter tempPw12b = new PrintWriter(new File(
				"c:/backtest/ttemp12b.txt"));
		tempPw12b.append(text);
		tempPw12b.close();

		text = text.replaceAll("[-]{4,}", " ").replaceAll("\r\n[_=-]{1}",
				"\r\n")
		// cause errors where intended as blank?
		;
		text = text.replaceAll("[ \t]+\\)", "\\)").replaceAll("( \t)+", "\t")
				.replaceAll("(\t )+", "\t").replaceAll("(?m)\\p{Blank}+$", "");
		PrintWriter tempPw5b = new PrintWriter(new File(
				"c:/backtest/ttemp13b.txt"));
		tempPw5b.append(text);
		tempPw5b.close();

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
		PrintWriter tempPw5c = new PrintWriter(new File(
				"c:/backtest/ttemp14.txt"));
		tempPw5c.append(text);
		tempPw5c.close();
		// text =
		// lowercaseLetterUpperCaseNotMonthTablowercaseLetterUpperCaseNotMonth
		// .matcher(text).replaceAll(" ");
		// PrintWriter tempPw5d = new PrintWriter(new File("c:/backtest/"
		// + getTableNamePrefixYearQtr(fileDate) + "temp15.txt"));
		// tempPw5d.append(text);
		// tempPw5d.close();

		Pattern YearSpaceMonthPattern = Pattern
				.compile("(?<=(19\\d{2}|20\\d{2}))[ \\xA0]{1,2}(?=(Jan[\\.\t ]{1}|January|Feb[\\.\t ]{1}|February| Mar[\\.\t ]{1}|March|Apr[\\.\t ]{1}|April"
						+ "|May[\\.\t ](?!([Nn]{1}ot|Dep|Limit))|Jun[\\.\t ]{1}|June|Jul[\\.\t ]{1}|July|Aug[\\.\t ]{1}|August|Sep[\\.\t ]{1}|September|Oct[\\.\t ]{1}|October|Nov[\\.\t ]{1}"
						+ "|November|Dec[\\.\t ]{1}|December|JAN[\\.\t ]{1}|JANUARY|FEB[\\.\t ]{1}|FEBRUARY| MAR[\\.\t ]{1}|MARCH|APR[\\.\t ]{1}|APRIL"
						+ "|MAY[\\.\t ]{1}(?!(NOT|LIMIT|DEP))|JUN[\\.\t ]{1}|JUNE|JUL[\\.\t ]{1}|JULY|AUG[\\.\t ]{1}|AUGUST|SEP[\\.\t ]{1}|SEPTEMBER|OCT[\\.\t ]{1}|OCTOBER|NOV[\\.\t ]{1}"
						+ "|NOVEMBER|DEC[\\.\t ]{1}|DECEMBER))");
		// year followed by 1 or 2 spaces than month (can't be .).

		text = YearSpaceMonthPattern.matcher(text).replaceAll("\t");

		// this inserts tab when you have just two spaces between numbers.
		// e.g., $23,205 $23,250 or $(0.13) $.09
		Pattern numberTwoSpacesNumber = Pattern
				.compile("(?<=(\\d\\)?))  (?=(\\$?\\(?[\\.|\\d]))");

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

		text = numberTwoSpacesNumber.matcher(text).replaceAll("\t")
				.replaceAll("\\$\\$", "\\$");
		// text = numberOneSpaceNumberHardReturn.matcher(text).replaceAll("\t");

		Pattern wordOneOrTwoSpacesNumberTab = Pattern
				.compile("(?<=([A-Za-z;\\&,\\'\\$\\(\\)\\-]{3,5}))  (?=([\\$\\(\\- \\.]{0,4}[\\d]{1,3}[ ]{0,1}[\r\n\t]{1,2}"
						+
						// word2spaces then 3 digits o rless then \t and/or hard
						// return
						"|[\\$\\(\\- \\.]{0,4}[\\d]{1,3},[\\d,]{3,15}[ ]{0,1}[\r\n\t]{1,2}))");
		// word2spaces then 3 digits then ',' then 3to15 digits/',' then \t|\r\n
		// Cash\\s\\s$823,272\t$796,284\r\n

		text = wordOneOrTwoSpacesNumberTab.matcher(text).replaceAll("\t");

		text = text.replaceAll("Ãƒâ€š|\\xA0", "").replaceAll("ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢", "'")
				.replaceAll("[\t]{2,}+", "\t");
		// need to replace double tabs near last

		Pattern lowerCaseHardReturnlowerCase = Pattern
				.compile("(?<=[a-z] ?)\r\n(?= ?[a-z])");

		text = lowerCaseHardReturnlowerCase.matcher(text).replaceAll(" ");

		/*text = TableParser.hardReturnNumberTabNumber.matcher(text).replaceAll(
				"\r\nBLANK ROW\t");*/
		text = hardReturnTabNumberTabNumber.matcher(text).replaceAll(
				"\r\nBLANK ROW\t");

		text = text.replaceAll("[\t ]\r\n", "\r\n").replaceAll("[\r\n]{2,11}",
				"\r\n");
		text = text.replaceAll("(?i)[\r\n]{1}Indus", " Indus").replaceAll(
				"(?i)ThreeMonth", "Three Month");
		text = text
				.replaceAll("[\t]{1,3}[\\$]{1,3}[\r\n]{1}", "\r")
				.replaceAll(
						"[\r\n]{1}.{0,1}(Cons.{8,11})?statements?.{3,6}(operations?|income)",
						"\rStatements of Income\r")
				.replaceAll(
						"[\r\n]{1}.{0,1}(Cons.{8,11})?balance.{1,2}sheets?",
						"\rBalance Sheets\r")
				.replaceAll(
						"[\r\n]{1}.{0,1}(Cons.{8,11})?statements?.{3,6}cash.{0,2}flows?",
						"\rStatements of Cash Flows\r");
		// System.out.println("cash flow replace=="+text);
		// <==BLANKROW should probably be last
		// PrintWriter tempPw104 = new PrintWriter(new File("c:/backtest/"
		// + getTableNamePrefixYearQtr(fileDate) + "temp16.txt"));
		// tempPw104.append(text);
		// tempPw104.close();
		return text;
	}


	public boolean doesPatternMatch(String text, Pattern pattern) {
		Matcher matcher = pattern.matcher(text);
		return matcher.find();
	}

/*	public String stripHtmlTags(String text) throws FileNotFoundException {

		// System.out.println("stripHtmlTags text200 characters="
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
			text = text.replaceAll(TRPattern.toString(), LineSeparator);
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
			text = text.replaceAll("ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â�", "-");
			text = text.replaceAll("ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ|ÃƒÂ¢Ã¢â€šÂ¬?", "\"");
			text = text.replaceAll("\\&lt;", "<");
			text = text.replaceAll("\\&gt;", ">");
			text = text.replaceAll("</h\\d?>|<h\\d?>", "$1\r\n$2 ");
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
			text = text.replaceAll(TDWithColspanPattern.toString(), "\t\t");
			// PrintWriter tempPw8 = new PrintWriter(new File("c:/backtest/"
			// + getTableNamePrefixYearQtr(fileDate) + "temp9.txt"));
			// tempPw8.append(text);
			// tempPw8.close();
			text = text.replaceAll(TDPattern.toString(), "\t");
			// PrintWriter tempPw9 = new PrintWriter(new File("c:/backtest/"
			// + getTableNamePrefixYearQtr(fileDate) + "temp10.txt"));
			// tempPw9.append(text);
			// tempPw9.close();
			text = text.replaceAll("(?i)(<SUP[^>]*>[\\(0-9\\) ]*</SUP[^>]*>)",
					"");
			// all superscripts [i.e. (1) (2)...] with blank
			// PrintWriter tempPw10 = new PrintWriter(new File("c:/backtest/"
			// + getTableNamePrefixYearQtr(fileDate) + "temp11.txt"));
			// tempPw10.append(text);
			// tempPw10.close();
			text = text.replaceAll("</U>|<U>|</u>|<U>", "");
			// aboe is removed b/c it interfers with below <BR>
			text = text.replaceAll("(?i)<BR> ?\r\n ?<BR>", "\r\n");
			// if 2 consecutive BRs-likely meant hard return
			// if BR after a number - likely end of a row.
			text = numberBR.matcher(text).replaceAll("\r\n");
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

			text = text.replaceAll("(?i)(</strong>|<strong>|</small>|<small>)",
					"");

			// PrintWriter tempPw12b = new PrintWriter(new File("c:/backtest/"
			// + getTableNamePrefixYearQtr(fileDate) + "temp12b.txt"));
			// tempPw12b.append(text);
			// tempPw12b.close();

			text = text.replaceAll("(?i)<BR>[ ]{1,10}", "<BR>")
					.replaceAll(ExtraHtmlPattern.toString(), "")
					.replaceAll(ExtraHtmlPattern2.toString(), " ");
			// previously I replaced all ExtraHtmlPatterns with a "~" and then
			// replaced all them with just 1 ws. This causes problems.
			// Ultimately if I need to remove problematic html code. Focus on
			// removing html code that only formats and replace with nothing and
			// for that which is a hard return or tab or row replace with a
			// space or hard return or tab.

			// System.out.println("11 fileDate="+fileDate);
			// PrintWriter tempPw12c = new PrintWriter(new File("c:/backtest/"
			// + getTableNamePrefixYearQtr(fileDate) + "temp12c.txt"));
			// tempPw12c.append(text);
			// tempPw12c.close();

		} catch (Throwable t) {
			t.printStackTrace(System.out);
		}

		// PrintWriter tempPw11 = new PrintWriter(new File(
		// ""+getTableNamePrefixYearQtr(fileDate) + "temp12.txt"));
		// tempPw11.append(text);
		// tempPw11.close();
		return text;
	}
*/
	public String formatTextParagraph(String text) throws IOException {
		// System.out.println("new section");
		formtxtPara++;
		// text = Utils.readTextFromFile("c:/getcontracts/temp/temp4.txt");

		text = text.replaceAll("[\r\n]{1}[\t \\-=]{1,200}[\r\n]{1}", "\r\r");
		// System.out.println("formatText after replacment=="+text);

		// \\s will search across lines - so can't use ';,' etc. (?sm) doesn't
		// work.

		Pattern patternReplace1 = Pattern.compile(
				"(?<=[\\(\\)a-z ]{3})[\r\n](?=[a-z ]{3})", Pattern.MULTILINE);
		Matcher match1 = patternReplace1.matcher(text);
		text = match1.replaceAll(" ");

		PrintWriter pwformTextRep1 = new PrintWriter(new File(
				"c:/getContracts/temp/pwformTextRep1" + formtxtPara + ".txt"));
		pwformTextRep1.append(text);
		pwformTextRep1.close();

		String[] lines = text.split("\r");
		@SuppressWarnings("unused")
		String line = "", priorLine = null, nextLine = null, tmpLine, lineStart6, lineEnd6, nextTmpLine, nextLineStart6 = null, nextLineEnd6;
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

				nextLineStart6 = nextTmpLine.substring(0,
						Math.min(nextTmpLine.length(), 6));

				nextLineEnd6 = nextTmpLine.substring(
						Math.max(nextTmpLine.length() - 6, 0),
						nextTmpLine.length());

				if (null != nextLineStart6
						&& (nextLineStart6.length() > 2
								&& nextLineStart6.substring(0, 3)
										.replaceAll("[a-zA-Z;, ]{3}", "")
										.trim().length() > 0 || nextLineStart6
								.length() < 3)) {
					listParaNumberMarkerNextLine = getAllStartIdxLocsAndMatchedGroups(
							nextLineStart6, patternParaNumberMarker);

					// if (listParaNumberMarkerNextLine.size() > 0) {
					// System.out.println("line=" + line.replaceAll("\n", "")
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
			// System.out.println("lineStart="+lineStart6+"| line.length="+line.length());
			// lineEnd10 =
			// tmpLine.substring(Math.max(tmpLine.length()-10,0),tmpLine.length());
			lineEnd6 = tmpLine.substring(Math.max(tmpLine.length() - 6, 0),
					tmpLine.length());

			// System.out.println("lineEnd6="+lineEnd6);
			// System.out.println("priorLine``	="+priorLine+ "\r line="+line);

			if (i == 0) {
				priorLine = line;
				continue;
			}

			// any visible character - get first location
			List<Integer> listPriorLineStartIdx = getAllIndexStartLocations(
					priorLine, patternAnyVisualCharacter);
			List<Integer> listCurLineStartIdx = getAllIndexStartLocations(line,
					patternAnyVisualCharacter);
			List<String[]> listParaMarkerEnd = new ArrayList<>();
			List<String[]> listParaNumberMarker = new ArrayList<>();

			if (null != lineEnd6) {
				// see if prior line ends with a para marker end - eg,
				// 'and/or:,;'
				listParaMarkerEnd = getAllStartIdxLocsAndMatchedGroups(lineEnd6,
						NLP.patternParaMarkerEnd);
				// System.out.println("listParaMarkerEnd.size="+listParaMarkerEnd.size()+" lineEnd6=="+lineEnd6);
			}

			if (null == lineEnd6) {
				System.out.println("lineEnd6==null");
			}

			if (null != lineStart6
					&& getAllStartIdxLocsAndMatchedGroups(lineStart6,
							patternParaNumberMarker).size() > 0
					&& (lineStart6.length() > 2
							&& lineStart6.substring(0, 3)
									.replaceAll("[a-zA-Z;, ]{3}", "").trim()
									.length() > 0 || lineStart6.length() < 3)) {

				listParaNumberMarker = getAllStartIdxLocsAndMatchedGroups(
						lineStart6, patternParaNumberMarker);
				// System.out.println("lineStart6=" + lineStart6
				// + " listParaNumberMarker.size="
				// + listParaNumberMarker.size()
				// + " listParaNumberMarker.grp="
				// + listParaNumberMarker.get(0)[1]
				// +" lineEnd6="+lineEnd6
				// );
				// if(listParaMarkerEnd.size()>0)
				// System.out.println("pE="+listParaMarkerEnd.get(0)[1]);
			}

			// System.out.println("idxPriorList.size="+idxPriorList.size()+
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

			// System.out.println("pE="+pE+" prior_pE="+prior_pE+" pNo="+pNo);

			if (listParaNumberMarker.size() > 0
					&& listParaMarkerEnd.size() > 0
					&& (listParaNumberMarkerNextLine.size() > 0
							|| prior_pE.contains(" and") || prior_pE
								.contains(" or"))) {

				i_prior_pNo = convertAlphaParaNumbering(prior_pNo);
				i_pNo = convertAlphaParaNumbering(pNo);
				// System.out.println("i_prior_pNo="+i_prior_pNo
				// +"  i_pNo="+i_pNo);

				sb.append(/* "[[pNo:" + pNo + "]]" + */"\r" + line /*
																 * + "[[pE:" +
																 * pE + "]]"
																 */);

				// System.out.println("[[pNo:" + pNo + "]] line="+
				// line.substring(0,Math.min(line.length(), 15))+ " prior_pE="
				// + prior_pE.replaceAll("[\r\n]", "") + "|" + " [[pE:"
				// + pE + "]]" + "|");
				cnt++;
			}

			/*
			 * text = sb.toString(); PrintWriter tempPw50 = new PrintWriter(new
			 * File( "c:/getContracts/temp/temp50"+formtxtPara+".txt"));
			 * tempPw50.append(text); tempPw50.close();
			 */

			if (listParaMarkerEnd.size() > 0
					&& listParaNumberMarker.size() <= 0
					&& listParaNumberMarkerNextLine.size() > 0) {
				sb.append("\ryyPe" + line /* + "[[pE:" + pE + "]]" */);
				// System.out.println("yyPe [[pE:" + pE + "]] line="
				// + line.substring(0, Math.min(line.length(), 15))
				// + " prior_pNo=" + prior_pNo.replaceAll("[\r\n]", "")
				// + "|");
				i_prior_pNo = convertAlphaParaNumbering(prior_pNo);
				i_pNo = convertAlphaParaNumbering(pNo);
				// System.out.println("i_prior_pNo="+i_prior_pNo
				// +"  i_pNo="+i_pNo);

				// cks prior line to see if it looks like a para end (',;:
				// and/or) and marks line
				cnt++;
			}

			if (listParaNumberMarker.size() > 0
					&& listParaMarkerEnd.size() <= 0) {
				sb.append(/* "[[pNo:" + pNo + "]]" + */"\r" + line);
				// ck cur line to see starts w/ para#
				// System.out.println("[[pNo:" + pNo + "]] line="
				// + line.substring(0, Math.min(line.length(), 15))
				// + " prior_pE=" + prior_pE.replaceAll("[\r\n]", "") + "|");

				i_prior_pNo = convertAlphaParaNumbering(prior_pNo);
				i_pNo = convertAlphaParaNumbering(pNo);
				// System.out.println("i_prior_pNo="+i_prior_pNo
				// +"  i_pNo="+i_pNo);

				cnt++;
			}

			if (pE.contains(";") || pE.contains(":") || pE.contains(",")) {
				prior_pE = pE;
				// System.out.println("prior_pE="+prior_pE);
			}

			if (pNo.replaceAll("[pNo:\r\n\t ]{1,10}", "").length() > 0) {
				prior_pNo = pNo;
				// System.out.println("prior_pNo"+prior_pNo);
			}

			if (listPriorLineStartIdx.size() > 0
					&& listCurLineStartIdx.size() > 0
					&& curLineIdxStart <= priorLineIdxStart) {
				if (listParaNumberMarker.size() <= 0
						&& listParaMarkerEnd.size() <= 0) {
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
		PrintWriter tempPw99 = new PrintWriter(new File(
				"c:/getContracts/temp/temp99" + formtxtPara + ".txt"));
		tempPw99.append(text);
		tempPw99.close();

		text = text.replaceAll("xxzz[\r\n]|yyPe[\r\n]", "\r");

		PrintWriter tempPw100 = new PrintWriter(new File(
				"c:/getContracts/temp/temp100" + formtxtPara + ".txt"));
		tempPw100.append(text);
		tempPw100.close();

		text = text.replaceAll("xxzz", " ");
		// gets lots of w/s between char on a line (not b/w paras).
		PrintWriter tempPw101 = new PrintWriter(new File(
				"c:/getContracts/temp/temp101" + formtxtPara + ".txt"));
		tempPw101.append(text);
		tempPw101.close();

		text = text
				.replaceAll(
						"(?<=([\\p{Punct}\\p{Alnum}]{1}))[ ]{2,}(?=([\\p{Punct}\\p{Alnum}]{1}))",
						" ");

		PrintWriter tempPw102 = new PrintWriter(new File(
				"c:/getContracts/temp/temp102" + formtxtPara + ".txt"));
		tempPw102.append(text);
		tempPw102.close();

		text = text.replaceAll("\\]\\] \\[\\[wasPg\\#\\]\\]\\[\\[",
				"\\]\\] \r\\[\\[wasPg\\#\\]\\]\r\\[\\[");
		// gets instances of a pg# sandwhiched on a line - and puts it on a line
		// by itself

		PrintWriter tempPw103 = new PrintWriter(new File(
				"c:/getContracts/temp/temp103" + formtxtPara + ".txt"));
		tempPw103.append(text);
		tempPw103.close();

		text = text
				.replaceAll(
						"(?<=([\\p{Punct}\\p{Alnum}]{1} ?))\\[\\[wasPg\\#\\]\\] ?(?=([\\p{Punct}\\p{Alnum}]{1}))",
						"\r\\[\\[wasPg\\#\\]\\]\r");

		PrintWriter tempPw104 = new PrintWriter(new File(
				"c:/getContracts/temp/temp104" + formtxtPara + ".txt"));
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
			if ((i + 1) < listOfMap.size()
					&& listOfMap.get(i + 1).contains(":") && match.find()) {
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
			System.out.println("key==" + entry.getKey() + " value=="
					+ entry.getValue());
		}
	}
	

	public static int convertAlphaParaNumbering(String alphaNumber) {
		int paraNumber = 0;

		String[] alphaParenLc = { "(a)", "(b)", "(c)", "(d)", "(e)", "(f)",
				"(g)", "(h)", "(i)", "(j)", "(k)", "(l)", "(m)", "(n)", "(o)",
				"(p)", "(q)", "(r)", "(s)", "(t)", "(u)", "(v)", "(w)", "(x)",
				"(y)", "(z)", "(aa)", "(bb)", "(cc)", "(dd)", "(ee)", "(ff)",
				"(gg)", "(hh)", "(ii)", "(jj)", "(kk)", "(ll)", "(mm)", "(nn)",
				"(oo)", "(pp)", "(qq)", "(rr)", "(ss)", "(tt)", "(uu)", "(vv)",
				"(ww)", "(xx)", "(yy)", "(zz)" };

		// v[5,22],i[1,9],x[10,24],ii[2,35],[20,50] <=Uc/Lc
		if (alphaNumber.replaceAll("\\([a-z]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenLc.length; i++) {
				if (alphaNumber.equals(alphaParenLc[i])) {
					return paraNumber = (i + 1);
				}
			}
		}

		String[] romanParenLc = { "(i)", "(ii)", "(iii)", "(iv)", "(v)",
				"(vi)", "(vii)", "(viii)", "(ix)", "(x)", "(xi)", "(xii)",
				"(xiii)", "(xiv)", "(xv)", "(xvi)", "(xvii)", "(xviii)",
				"(ixx)", "(xx)" };

		if (alphaNumber.replaceAll("\\([ivx]{1,5}\\)", "").trim().length() < 1) {
			for (int i = 0; i < romanParenLc.length; i++) {
				if (alphaNumber.equals(romanParenLc[i])) {
					 System.out.println("romanParenLc alphaNumber="
					 + alphaNumber);
					return paraNumber = (i + 1) * 10;
				}
			}
		}

		String[] romanParenUc = { "(I)", "(II)", "(III)", "(IV)", "(V)",
				"(VI)", "(VII)", "(VIII)", "(IX)", "(X)", "(XI)", "(XII)",
				"(XIII)", "(XIV)", "(XV)", "(XVI)", "(XVII)", "(XVIII)",
				"(IXX)", "(XX)" };

		if (alphaNumber.replaceAll("\\([ivx]{1,5}\\)", "").trim().length() < 1) {
			for (int i = 0; i < romanParenUc.length; i++) {
				if (alphaNumber.equals(romanParenUc[i])) {
					return paraNumber = (i + 1) * 100;
				}
			}
		}

		String[] alphaParenUc = { "(A)", "(B)", "(C)", "(D)", "(E)", "(F)",
				"(G)", "(H)", "(I)", "(J)", "(K)", "(L)", "(M)", "(N)", "(O)",
				"(P)", "(Q)", "(R)", "(S)", "(T)", "(U)", "(V)", "(W)", "(X)",
				"(Y)", "(Z)", "(AA)", "(BB)", "(CC)", "(DD)", "(EE)", "(FF)",
				"(GG)", "(HH)", "(II)", "(JJ)", "(KK)", "(LL)", "(MM)", "(NN)",
				"(OO)", "(PP)", "(QQ)", "(RR)", "(SS)", "(TT)", "(UU)", "(VV)",
				"(WW)", "(XX)", "(YY)", "(ZZ)" };

		if (alphaNumber.replaceAll("\\([A-Z]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenUc.length; i++) {
				if (alphaNumber.equals(alphaParenUc[i])) {
					return paraNumber = (i + 1) * 1000;
				}
			}
		}

		String[] alphaPeriodUc = { "A.", "B.", "C.", "D.", "E.", "F.", "G.",
				"H.", "I.", "J.", "K.", "L.", "M.", "N.", "O.", "P.", "Q.",
				"R.", "S.", "T.", "U.", "V.", "W.", "X.", "Y.", "Z.", "AA.",
				"BB.", "CC.", "DD.", "EE.", "FF.", "GG.", "HH.", "II.", "JJ.",
				"KK.", "LL.", "MM.", "NN.", "OO.", "PP.", "QQ.", "RR.", "SS.",
				"TT.", "UU.", "VV.", "WW.", "XX.", "YY.", "ZZ." };

		if (alphaNumber.replaceAll("[A\\.]{2,3}", "").trim().length() < 1) {
			for (int i = 0; i < alphaPeriodUc.length; i++) {
				if (alphaNumber.equals(alphaPeriodUc[i])) {
					return paraNumber = (i + 1) * 10000;
				}
			}
		}

		String[] numberParen = { "(1)", "(2)", "(3)", "(4)", "(5)", "(6)",
				"(7)", "(8)", "(9)", "(10)", "(11)", "(12)", "(13)", "(14)",
				"(15)", "(16)", "(17)", "(18)", "(19)", "(20)", "(21)", "(22)",
				"(23)", "(24)", "(25)", "(26)", "(27)", "(28)", "(29)", "(30)",
				"(31)", "(32)", "(33)", "(34)", "(35)", "(36)", "(37)", "(38)",
				"(39)", "(40)", "(41)", "(42)", "(43)", "(44)", "(45)", "(46)",
				"(47)", "(48)", "(49)", "(50)", "(51)", "(52)", "(53)", "(54)",
				"(55)", "(56)", "(57)", "(58)", "(59)", "(60)", "(61)", "(62)",
				"(63)", "(64)", "(65)", "(66)", "(67)", "(68)", "(69)", "(70)",
				"(71)", "(72)", "(73)", "(74)", "(75)", "(76)", "(77)", "(78)",
				"(79)", "(80)", "(81)", "(82)", "(83)", "(84)", "(85)", "(86)",
				"(87)", "(88)", "(89)", "(90)", "(91)", "(92)", "(93)", "(94)",
				"(95)", "(96)", "(97)", "(98)", "(99)" };

		if (alphaNumber.replaceAll("\\([0-9]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < numberParen.length; i++) {
				if (alphaNumber.equals(numberParen[i])) {
					return paraNumber = (i + 1) * 100000;
				}
			}
		}

		String[] numberPeriod = { "1.", "2.", "3.", "4.", "5.", "6.", "7.",
				"8.", "9.", "10.", "11.", "12.", "13.", "14.", "15.", "16.",
				"17.", "18.", "19.", "20.", "21.", "22.", "23.", "24.", "25.",
				"26.", "27.", "28.", "29.", "30.", "31.", "32.", "33.", "34.",
				"35.", "36.", "37.", "38.", "39.", "40.", "41.", "42.", "43.",
				"44.", "45.", "46.", "47.", "48.", "49.", "50.", "51.", "52.",
				"53.", "54.", "55.", "56.", "57.", "58.", "59.", "60.", "61.",
				"62.", "63.", "64.", "65.", "66.", "67.", "68.", "69.", "70.",
				"71.", "72.", "73.", "74.", "75.", "76.", "77.", "78.", "79.",
				"80.", "81.", "82.", "83.", "84.", "85.", "86.", "87.", "88.",
				"89.", "90.", "91.", "92.", "93.", "94.", "95.", "96.", "97.",
				"98.", "99." };

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
	
	public static void main(String[] arg) {
		InputStream modelIn = NLP.class.getClassLoader().getResourceAsStream("en-sent.bin");
		try {
		  SentenceModel model = new SentenceModel(modelIn);
		  SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
		  
		  String txt = Utils.readTextFromFile("c:/pk/Eclipse_Indigo/workspace/Linc_Client/data/sent1.txt");
		  
		  String sentences[] = sentenceDetector.sentDetect(txt);
		  for (String sent : sentences)
			  System.out.println(">>>"+sent);
		}
		catch (IOException e) {
		  e.printStackTrace(System.out);
		}
		finally {
		  if (modelIn != null) {
		    try {
		      modelIn.close();
		    } catch (IOException e) {
				  e.printStackTrace(System.out);
		    }
		  }
		}
	}
}
