package contracts;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import charting.JSonUtils;
import xbrl.FileSystemUtils;
import xbrl.NLP;
import xbrl.Utils;

public class elasticDocRectifier {

	public static TreeMap<Integer, String> map_CIK_symbol = new TreeMap<Integer, String>();
	public static TreeMap<String, String[]> map_symbol_sector_industry = new TreeMap<String, String[]>();
	public static TreeMap<String, String[]> map_company_symbol_sector_industry = new TreeMap<String, String[]>();
	public static int count = 0;
//	public static boolean removeSensitiveData = false;
	public static boolean removeSmallHcnt = true;

	public boolean ignoreParsingErrors = true;
	int movedBadFiles = 0;
	public boolean firstPassDone = false;

	public static TreeMap<String, String> map_of_headings_before_and_after_cleanup = new TreeMap<String, String>();

	public static TreeMap<String, String[]> get_map_symbol_sector_industry(String file_path) throws IOException {
		// run once to populate the map. Then use the map to call the sector and
		// industry using the symbol. The symbol is the key for the map
		// Utils.readTextFromFile("E:\\getContracts\\CIK_symbol\\symbol_sector_industry");
		String text = Utils.readTextFromFile(file_path);
//		System.out.println(file_path);
		System.out.println("text.len=" + text.length());
		String[] lines = text.split("\r\n");
		String symbol, sector, industry, company_name;
		for (int i = 0; i < lines.length; i++) {
//			System.out.println("lines.len="+ lines.length+ " lines ="+ Arrays.toString(lines));
			if (lines[i].split("\t").length < 4)
				continue;
//			System.out.println("line i=" + i);
			company_name = lines[i].split("\t")[0];
			symbol = lines[i].split("\t")[1].toUpperCase().trim();
			industry = lines[i].split("\t")[2];
			sector = lines[i].split("\t")[3];
			String[] ary = { sector, industry, company_name, symbol };
			map_symbol_sector_industry.put(symbol, ary);
		}
		return map_symbol_sector_industry;
	}

	public static TreeMap<Integer, String> get_map_CIK_symbol(String file_path) throws IOException {
		// run wants to populate the map. Then use the map to call the sector and
		// industry using the symbol. The symbol is the key for the map
		// Utils.readTextFromFile("E:\\getContracts\\CIK_symbol\\cik_symbol.txt");
		String text = Utils.readTextFromFile(file_path);
		int central_index_key;
		String symbol;

		String[] lines = text.split("\r\n");
		for (int i = 0; i < lines.length; i++) {
			// Arrays.toString(lines));
			if (lines[i].split("\t").length < 2)
				continue;
//			System.out.println("line i=" + i);
			symbol = lines[i].split("\t")[0].toUpperCase().trim();
			central_index_key = Integer.parseInt(lines[i].split("\t")[1]);
			map_CIK_symbol.put(central_index_key, symbol);
		}
		return map_CIK_symbol;
	}

	public static String[] get_industry_sector_with_just_company_name(String company_name) {

		String symbol, sector, industry;
		if (map_company_symbol_sector_industry.containsKey(company_name.toUpperCase())) {
			sector = map_company_symbol_sector_industry.get(company_name.toUpperCase())[0];
			industry = map_company_symbol_sector_industry.get(company_name.toUpperCase())[1];
			symbol = map_company_symbol_sector_industry.get(company_name.toUpperCase())[3];
			String[] array = { symbol, sector, industry };
			return array;
		}

		return null;
	}

	public static String[] get_industry_sector(int central_index_key) {
		String industry, sector, symbol;
		if (!map_CIK_symbol.containsKey(central_index_key))
			return null;

		symbol = map_CIK_symbol.get(central_index_key);
//		System.out.println("symbol=" + symbol);
//		System.out.println("map_symbol_sector_industry==" + map_symbol_sector_industry.size());
		if (!map_CIK_symbol.containsKey(central_index_key))
			return null;
		if (!map_symbol_sector_industry.containsKey(symbol))
			return null;

		sector = map_symbol_sector_industry.get(symbol)[0];
		industry = map_symbol_sector_industry.get(symbol)[1];
		String[] array = { symbol, sector, industry };
//		System.out.println("sector=" + sector + " industry=" + industry);

		return array;

	}

	NLP nlp = new NLP();
	Map<String, Integer> fileParsingRetry = new HashMap<>();

	@SuppressWarnings("rawtypes")

	public List<Map> makeJsonFileValid(String json) throws IOException {

		NLP nlp = new NLP();
//		long start = System.currentTimeMillis();

		String subPartFirst = json.substring(0, json.indexOf("}"));
//		System.out.println("1.subpart= " + ((System.currentTimeMillis() - start)) / 100);
//		start = System.currentTimeMillis();
//		System.out.println("doc0="+subPartFirst);
		int kIdCnt = nlp.getAllMatchedGroups(subPartFirst, Pattern.compile("(?ism)kId")).size();
		if (kIdCnt > 1) {
			subPartFirst = subPartFirst.replaceFirst("(?sm),\"kId\":\"[\\d-_]{22,24}\"\r\n", "");
//			System.out.println("2.subpart= " + ((System.currentTimeMillis() - start)) / 100);
		}
//		start = System.currentTimeMillis();
		subPartFirst = subPartFirst.replaceAll("([^\"]{1})(\\]\r\n" + ",\")", "\"$2");
		subPartFirst = subPartFirst.replaceAll(",\"contractNameOk\":(?= ?\r\n)", ",\"contractNameOk\":0");
//		System.out.println("3.subpart= " + ((System.currentTimeMillis() - start)) / 100);
//		start = System.currentTimeMillis();
		subPartFirst = subPartFirst.replaceAll(",\"all(Hdgs|Exh|QuotedTerms)\":\\[\"?\\]", "");
		subPartFirst = subPartFirst.replaceAll("(?<=,\"all(Hdgs|Exh|QuotedTerms)\":\\[)\",(?=\")", "");
		subPartFirst = subPartFirst.replaceAll("(\"openParaContractName\":\")\"(.{1,250})(\")\"($|\r\n)", "$1$2$3$4");
//		System.out.println("4.subpart= " + ((System.currentTimeMillis() - start)) / 100);
//		start = System.currentTimeMillis();
		subPartFirst = subPartFirst.replaceAll("(,\"openParaContractName\":) ?(.{1,250})(\r\n)", "$1\"$2\"$3");
		subPartFirst = subPartFirst.replaceAll("(,\"openParaContractName\":\")(\"\") ?(.{1,250}\")(\"\")(\r\n)",
				"$1$3$5");
		subPartFirst  = subPartFirst .replaceAll("(?ism)(PartiesOp\" ?: ?\\[ \".{1,200}[^\"] ?)(\\])(,? ?\r\n\r?\n?,?\"openingParagrap)",
				"$1\"]$3");
		subPartFirst = subPartFirst.replaceAll("(\"allExhs?\":\\[)([^\"])", "$1\"$2");
		subPartFirst = subPartFirst.replaceAll(
				"(\")(allExhs|allHdgs|allDefs|allQuotedTerms|allQuotedTermsTxt)(\":\\[)(,)(\")", "\"$2\":[\"");
//		System.out.println("5.subpart= " + ((System.currentTimeMillis() - start)) / 100);
//		start = System.currentTimeMillis();
		subPartFirst = subPartFirst.replaceAll("(?<=\"(allDefs|allHdgs|allQuotedTerms|allExhs?)\":\\[\"),\"", "");
		subPartFirst = subPartFirst.replaceAll("(?<=[:] ?)\"\"|\"\"(?= ?\r\n)", "\"");
		subPartFirst = subPartFirst.replaceAll("\"\\]\"\r\n", "\"\\]\r\n");
		
//		System.out.println("subpart finished = " + ((System.currentTimeMillis() - start)) / 100);
//		start = System.currentTimeMillis();

		String subPartSecond = json.substring(json.indexOf("}"));

		json = subPartFirst + subPartSecond;

		json = json.replaceAll("(,\"ids\":\"\\\\N\"\r\n|,\"sctr\":\"\\\\N\"\r\n|,\"ticker\":\"\\\\N\"\r\n)+", "");
		json = json.replaceAll("(,\"kId\":\")(kId=)", "$1");
		json = json.replaceAll("(?sm)(\"contractNameAlgo\": ?\")(contractNameAlgo)(.{1,300})(\r\n)", "$1$3\"$4,");
		json = json.replaceAll("\\\\[A-Z\\-_ ;\\.\\dabcdefghijklmopqstuvwxyz=^\\(\\)]|", "");
		json = json.replaceAll("\\\"\r\n" + ",\"hashTxtId\":", "\"\r\n" + ",\"hashTxtId\":");
		json = json.replaceAll("\\\\\"\r\n", "\"\r\n");
		json = json.replaceAll("\"\"\\]", "\"\\]");
//		json = json.replaceAll("(,\"\"|,\"(?=\\]))", ""); multi value fields get corrupted by ,\"]
		json = json.replaceAll("\t", "");
		json = json.replaceAll("\r\n.{1,25}\\[\"\\]", "");
		json = json.replaceAll(",\"[a-zA-Z]+\"\"kId\":\"[\\d-_]{20,25}\"\r\n", "");
		json = json.replaceAll("(?<=,\"hashTxtId\":-?[\\d]{6,11})-\\d\\d?", "");
		json = json.replaceAll(",\"hashTxtId\":[\\d]{1,5}\r\n", "");
		json = json.replaceAll("xxPD", "\\.");
		json = json.replaceAll("xxOP", "\\(");
		json = json.replaceAll("CPxx", "\\)");
		json = json.replaceAll("(\")(,\")(defL|mHdL|subL|hdgL|secL|exhL)(\" ?: ?\\d)", "$1\r\n$2$3$4");
		json = json.replaceAll("[\r\n]+", "\r\n");
		json = json.replaceAll("\\}[\r\n]{2,6}\\{", "\\},\r\n\\{\r\n");
		json = json.replaceAll("(?<=[^\\}]{1})\r\n,\r\n\\{", "\r\n\\},\r\n\\{");
		json = json.replaceAll("\r\n,\"[A-Za-z]+\"(?=,\")", "\r\n");
		json = json.replaceAll("([\\*]+)", "\\\\\\\\*");
		json = json.replaceAll("\\\\\\\\\\\\", "\\\\\\\\");
		json = json.replaceAll("(\\\\)([\\#&])", "$2");
		json = json.replaceAll("\r\n]\r\n", "\r\n\\}\r\n\\]");
		json = json.replaceAll("(?ism)(\\}.{1,5}\\})", "\\}");
		json = json.replaceAll("[\\\\]{2}|[\\|]{2,}|\\\\(?=[A-Za-z])", "");
		json = json.replaceAll("Qx", "").replaceAll("(\\\\)([\\!\\[\\]\\@\\'\\,\\~\\:\\%|\\|])", "$2")
				.replaceAll(",\"txt\" ?: ?\"\".*?\r\n", "").replaceAll("\r\n,\"([A-Z]{1}[a-zA-Z]{2,3})\"\"fDate\"", "\r\n,\"fDate\"");
		json = json.replaceAll("\r\n,\r\n\\{", "\r\n\\},\r\n\\{");
		
//		System.out.println("5.secs = " + ((System.currentTimeMillis() - start)) / 100);
//		start = System.currentTimeMillis();

//		PrintWriter pwtmp = new PrintWriter(new File("e:/temp/test.json"));
//		pwtmp.append(json);
//		pwtmp.close();
//		System.out.println("check_if_heading_should_not_be_displayed_in_results");
//		if (!firstPassDone) {
			json = check_and_cleanHeading(json);
//			System.out.println("createAllHeadingsFields");
//			json = HeadingNamesCleanup.createAllHeadingsFields(json);// returns doc0 w/ all hdgs which gets saved
																		// to hdgs file/folder for ingestion to
																		// allDefs etc and later as sep index.
//			String filename = jsonFile.getAbsoluteFile().toString();
//			filename = filename.replaceAll(jsonFile.getName(), "allHdgs_" + jsonFile.getName());
//			System.out.println("filename==="+filename);
//			System.out.println("allheadings==="+allHeadings);
//			File file_allhdgs_ = new File(filename);
//			System.out.println("filename=="+filename);

//			PrintWriter pw = new PrintWriter(file_allhdgs_);
//			pw.print(allHeadings);
//			pw.close();
			firstPassDone = true;
//		}

		if (nlp.getAllIndexEndLocations(json, Pattern.compile("PartiesOp|openingParagraph2")).size() == 0) {
//			System.out.println("getLegalEntitiesFromOpOfJsonFile");
			json = EntityRecognition.getLegalEntitiesFromOpOfJsonFile(json);
			json = json.replaceAll(",\"PartiesOp\" : \\[\\]", "");
		}

//		System.out.println("PartiesOp");
		json = json.replaceAll("\"PartiesOp\" ?: ?\\[ \\],", "").replaceAll(",\"txt\"\"", ",\"txt\":\"");

		json = json.replace("\\\",\r\n", "\\\"\",\r\n").replace("\\\"],\r\n", "\\\"\"],\r\n");
//		String tmp = "\"Website\"\"]";
//		
//		System.out.println("tmp..."+tmp.replaceAll("\"\"\\]", "\"\\]"));
//		,"allHdgs":[","

//		PrintWriter pw = new PrintWriter(new File("c:/temp/json.json"));
//		pw.append(json);
//		pw.close();
		
		List<Map> docs = null;
		try {
			docs = JSonUtils.json2List(json, Map.class);
			System.out.println("  json parseable:    ");
		} catch (Exception e) {
			System.out.println("* json un-parseable: ");
			if (firstPassDone) {
				System.out.println("first pass done");
//				? if exists this is 2nd time so error is thrown
				if (!ignoreParsingErrors)
					throw e;
				else {
					// move to somewhere else. if qtr1\485bpos, then put at
					// e:\\unparseableJsonFile\\file abs path\\
//					String path = jsonFile.getAbsolutePath();
//					int colonIdx = path.indexOf(":");
//					path = "e:/unparseableJsonFile" + path.substring(colonIdx + 1); // remove initial drive:
//					File errF = new File(path);
//					if (!errF.getParentFile().exists())
//						errF.getParentFile().mkdirs();
//					FileSystemUtils.writeToAsciiFile(errF.getAbsolutePath(), json);
//					jsonFile.delete(); // remove existing bad json
					System.out.println("bad json. file moved to: ");

					movedBadFiles++;
					return null;
				}
			}

			// Note: this is where the patchwork of fixes occur. Special characters are
			// removed and other aspects, see below.

			// unparseable json - clean json, put into file and try again

			// ,"kId":"
			// 0001193125-13-442121_1"

			// remove some control chars:- 8 (back-space),
			json = json.replaceAll("[\\x08]", "");
			if (e.getMessage().contains("Unrecognized character escape '0'")) {
				while ((json.indexOf("\\0")) >= 0)
					json = json.replace("\\0", "0");
			}

			json = json.replaceAll("\"gLaw\" \\?:\" \\?.*?\" \\?\r\n,", ""); // "gLaw" ?:" ?New York" ?
			json = json.replaceAll(",[\r\n]{2,},|,,", ",");

			String patternStr;
			List<String> matches;

			// "txt" : .....", field value has no start \" but ends correctly
			patternStr = "(?<=\"txt\" ?: ?)[ ]*?([^\" ].*?\"[,\r\n]{3,})";
			matches = getAllMatchedGroups(json, Pattern.compile(patternStr));
			if (null != matches && matches.size() > 0) {
				json = json.replaceAll(patternStr, "\"$1");
			}

			patternStr = "(,\"scor\" ?: ?\\d+\r\n)(?!,)";
			matches = getAllMatchedGroups(json, Pattern.compile(patternStr));
			if (null != matches && matches.size() > 0) {
				json = json.replaceAll(patternStr, "$1".replace("\r\n", "") + ",\r\n");
			}

			patternStr = "(\r\n,\"txt\" ?: ?\",\r\n)";
			matches = getAllMatchedGroups(json, Pattern.compile(patternStr));
			if (null != matches && matches.size() > 0) {
				json = json.replaceAll(patternStr, ",\r\n");
			}

			// remove instances of ":\r\n, means some field value is missing - remove field
			json = json.replaceAll("\"[a-zA-Z0-9_]{3,}\":\r\n,", "");
			json = json.replaceAll("\r\n,\"txt\" ?: ?\"\r\n", "\r\n"); // txt field has no value and has only 1
																		// dbl-quote
			// if a comma found at last before list closing
			json = json.replaceAll(",[\r\n ]*\\]", "]");

			// in some indentures, opPara has no quotes around value: ,"openingParagraph":
			// VE...
			matches = getAllMatchedGroups(json,
					Pattern.compile("(?<=\"openingParagraph\":)([^\"].+)(?=[,\"\r\n]{2,6})"));
			// System.out.println(m);
			if (null != matches && matches.size() > 0) {
				json = json.replace("\"openingParagraph\":" + matches.get(0),
						"\"openingParagraph\":\"" + matches.get(0) + "\"");
			}

			json = json.replaceAll("\"gLaw\" \\?:\" \\?.*?\" \\?\r\n,", ""); // "gLaw" ?:" ?New York" ?
			json = json.replaceAll(",[\r\n]{2,},|,,", ",");
//			json = json.replaceAll("(\\\\)([A-Za-mo-qs-z ,;:])", "$2");
//			json = json.replaceAll("([\\( ])(\")", "$1\\\\\"");
//			json = json.replaceAll("([a-zA-Z])(\")(?=[ \\)][;]{0,1} ?[a-zA-Z])", "$1\\\\\"");// can't be a ','
//			json = json.replaceAll("\\\\'", "'");
//			json = json.replaceAll("(hdgOrd\":\\[\"[a-z]{3})(\\\\\")", "$1\",");
//			json = json.replaceAll("\\\\\"(?=\r\n,\")", "\"");
//			json = json.replaceAll("\\\\(?=[ \\d\\(])", "");
//			json = json.replaceAll("\\\\w", "w");
			json = json.replaceAll("(,\"scor\":)(numberOfLegalEntities=)([\\d])+", "$1$3");

			// write to file and try again
			return makeJsonFileValid(json);
		}
		return docs;
	}

	public static int heading_is_likely_wrong(String heading) throws IOException {

		String sectionHeadingStopped = GoLaw.goLawRemoveStopWordsAndContractNamesLegalStopWords(heading);
		sectionHeadingStopped = GoLaw.patternStopWordsAggressiveNotForHtxtorDisplayData.matcher(sectionHeadingStopped)
				.replaceAll("").replaceAll("[^A-Za-z ]", "").replaceAll("[ ]+", " ");

//		System.out.println("sectionHeading_is_likely_wrong -- sectionHeadingStopped==="+sectionHeadingStopped);
		String[] sectionNameWords = StopWords.removeStopWords(sectionHeadingStopped).split(" ");
		boolean isCap = true;
		String word = "";
		// if the word is greater than 2 characters - eg Tax and the first character is
		// not uoppoer
		int cnt = 0;
		for (int c = 1; c < sectionNameWords.length; c++) {
			word = sectionNameWords[c].replaceAll("[^A-Za-z]", "");
			if (word.replaceAll("between|without|Rule|upon", "").length() < 3)
				continue;
//			System.out.println("word===" + word);

			if (!Character.isUpperCase(word.charAt(0))) {
				cnt++;
			}
		}

//		System.out.println("cnt lower case==" + cnt + " hdg initial cap cnt=="
//				+ (sectionHeadingStopped.trim().split(" ").length - cnt) + " secHgStopped==" + sectionHeadingStopped);
		if (cnt >= (sectionHeadingStopped.trim().split(" ").length - cnt) && cnt > 1) {
//			System.out.println("cnt=="+cnt+" stop words="+ sectionHeadingStopped.trim().split(" ").length);
			return (int) (100 * ((double) ((double) cnt / (double) sectionHeadingStopped.trim().split(" ").length)));
		}
		return 0;
	}

	public static String check_and_cleanHeading(String json) throws IOException {

		NLP nlp = new NLP();
		String hdg = "", hdgBef, txt = "", typ = null, key = "";
		TreeMap<String, String> map = getAllMatchedGroupsIntoMap(json,
				Pattern.compile("(?ism)(,?\")(sec|hdg|mHd|sub|def|exh)(\" ?: ?\").{1,1000}?(\",?\r\n)"));

//		System.out.println("hdg map hdg size==" + map.size());
//		boolean hdgOk = false;//do not use!!! over complicating things

		for (Map.Entry<String, String> entry : map.entrySet()) {
			// here I only cleaned the hdgs and did not also strip it from the txt. And in
			// txt the hdg may still have \N separating SECTION from hdg name whereas in
			// sec/mHd etc field it does not.
			if (nlp.getAllMatchedGroups(entry.getKey(), Pattern.compile("\"(sec|hdg|mHd|sub|def|exh)\"")).size() > 0) {
				key = entry.getKey();
				hdg = nlp.getAllMatchedGroups(key, Pattern.compile("\"(sec|hdg|mHd|sub|def|exh)\".*?(?=\r\n)")).get(0);

//				System.out.println("full hdg==" + hdg);
				hdgBef = hdg;

				hdg = HeadingNamesCleanup.universaleHeadingCleaner(hdg);// get cleaned up hdg. then if that cleaned up
																		// heading changes (it should) search and
																		// replace with the revised hdg

//				System.out.println("hdgaft="+hdg);
				map_of_headings_before_and_after_cleanup.put(hdgBef, hdg);
//				System.out.println("hdg after cleaning=" + hdg);
//				hdgOk = sectionHeading_is_likely_wrong(hdg.replaceAll("\"(sec|hdg|mHd|sub|def|exh)\"", ""));
				typ = nlp.getAllMatchedGroups(hdg, Pattern.compile("(?<=\")(sec|hdg|mHd|sub|def|exh)(?=\")")).get(0);
				// TODO: Only if
//				if (!hdgOk) {
//					System.out.println("hdgBefbef="+hdgBef+nlp.getAllMatchedGroups(json.substring(0,500), Pattern.compile("kId.*?\r\n")).get(0));

//					json = json.replace(hdgBef, hdg + "\r\n  \"" + typ + "Ok\":\"no\",");
//					System.out.println(
//							"a.hdgOk=" + hdgOk + " hdgBef==" + hdgBef + " hdgAft=" + hdg + "\r\n" + typ + "Ok\":\"no\"");
//					System.out.println("a.json="+json);
//					if (hdgBef.replaceAll("Section|SECIION", "").length() > hdg.length() + 6
//							|| nlp.getAllMatchedGroups(hdg, Pattern.compile("\\d|[\\dA-Z][\\.\\)]+")).size() > 0) {
//						System.out.println("a.hdgOk=" + hdgOk + "ERROR?? hdgBef==" + hdgBef + " hdgAft=" + hdg + "\r\n"
//								+ typ + "Ok\":\"no\"");
//					}
//				}
				if (!hdg.equals(hdgBef)) {
//					System.out.println("hdgBefbef="+hdgBef+nlp.getAllMatchedGroups(json.substring(0,5000), Pattern.compile("kId.*?\r\n")).get(0));
//					System.out.println("hdgBefbef="+hdgBef);

					json = json.replace(hdgBef, hdg);

//					System.out.println("b.hdgOk=" + hdgOk + " hdgBef==" + hdgBef + " hdgAft=" + hdg);
//					System.out.println("b.json="+json);
//					if (hdgBef.replaceAll("Section|SECIION", "").length() > hdg.length() + 6 || nlp
//							.getAllMatchedGroups(hdg, Pattern.compile("\"(\\d|[\\dA-Z][\\.\\)]{1,2})")).size() > 0) {
//						System.out.println("b.hdgOk=" + hdgOk + "ERROR?? hdgBef==" + hdgBef + " hdgAft=" + hdg);
				}
//				}
			}
		}

//		PrintWriter pw = new PrintWriter("c:/temp/hdgfixed.json");
//		pw.append(json);
//		pw.close();
		return json;

	}

	public static TreeMap<String, String> getAllMatchedGroupsIntoMap(String text, Pattern pattern) throws IOException {

		TreeMap<String, String> idxs = new TreeMap<String, String>();
		if (text == null) {
			return idxs;
		}

		Matcher matcher = pattern.matcher(text);
		String match = null;
		while (matcher.find()) {
			match = matcher.group()
//					.replaceAll("\t", " ").replaceAll("  ", " ")
//					.replaceAll("(?ism)(\")(sec|hdg|mHd|sub|def|exh)(\" ?: ?\")(.{1,500})(\" ?, ??\r\n)","$1$2$3$4$5")
//					.trim()
			;
//			System.out.println("match=="+match);
			idxs.put(match, "");
		}
		return idxs;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })

	public List<Map> removeAllHdgs(List<Map> docs, String... fields) throws IOException {

//	System.out.println("remove small hCnt");
		Map doc;
		Iterator<Map> itr = docs.iterator();
		int cnt = 0;
		while (itr.hasNext()) {
			doc = itr.next();
			cnt++;

			if (cnt > 1)
				return docs;

			if (cnt == 1) {
//				if (doc.get("allQuotedTerms") != null) {
					doc.remove("allQuotedTerms");
//				}

//				if (doc.get("allQuotedTermsTxt") != null) {
					doc.remove("allQuotedTermsTxt");
//				}

//				if (doc.get("allQuotedTermsCnt") != null) {
					doc.remove("allQuotedTermsCnt");
//				}

//				if (doc.get("allQuotedTermsTxtCnt") != null) {
				doc.remove("allQuotedTermsTxtCnt");
//				}

//				if (doc.get("allHdgs") != null) {
				doc.remove("allHdgs");

//				}

//				if (doc.get("allHdgsCnt") != null) {
				doc.remove("allHdgsCnt");
//				}

//				if (doc.get("allDefs") != null) {
				doc.remove("allDefs");
//				}

//				if (doc.get("allDefsCnt") != null) {
				doc.remove("allDefsCnt");
//				}
//				if (doc.get("allExhs") != null) {
				doc.remove("allExhs");
//				}

//				if (doc.get("allExhsCnt") != null) {
				doc.remove("allExhsCnt");
//				}
//				if (doc.get("allHdgs_m") != null) {
				doc.remove("allHdgs_m");
//				}

//				if (doc.get("allHdgsCnt_m") != null) {
				doc.remove("allHdgsCnt_m");
//				}

//				if (doc.get("allDefs_m") != null) {
				doc.remove("allDefs_m");
//				}

//				if (doc.get("allDefsCnt_m") != null) {
				doc.remove("allDefsCnt_m");
//				}

//				if (doc.get("allExhs_m") != null) {
				doc.remove("allExhs_m");
//				}

//				if (doc.get("allExhsCnt_m") != null) {
				doc.remove("allExhsCnt_m");
//				}

//				if (doc.get("allQuotedTerms_m") != null) {
				doc.remove("allQuotedTerms_m");
//				}
			}
		}

		return docs;
	}

	public List<Map> removeSmallData(List<Map> docs, String... fields) throws IOException {

//		System.out.println("remove small hCnt");
		Map doc;
		Iterator<Map> itr = docs.iterator();
		int cnt = 0;
		while (itr.hasNext()) {
			doc = itr.next();
			cnt++;

			if (cnt < 2) {
				continue;
			}

			if (doc.get("txt") == null || doc.get("txt").toString().length() < 10) {
				itr.remove();
				continue;
			}

			if ((doc.get("hCnt") == null || Integer.parseInt(doc.get("hCnt").toString()) < 3)
					&& Integer.parseInt(doc.get("wCnt").toString()) < 8// this was fixed so that ALL CAPS are not
																		// skipped. Unfortunately not until fully parsed
																		// on 7/1. Need to reparse.

			) {
				itr.remove();
//				System.out.println("removed no hCnt. ");
				continue;
			}

		}

		return docs;
	}

	public List<Map> reviseFields(List<Map> docs) throws IOException {
		/*
		 * this checks when a hdg is in the txt and removes it. for example:
		 * "txt":"Submission by Contractor. Contractor shall submit copy..." where
		 * ,"hdg":"Submission by Contractor." result is: Contractor shall submit copy...
		 */

		Object txt = null;
		String secStr = null, txtStr = null, hdgStr = null, mHdStr = null, subStr = null, str = null;

		NLP nlp = new NLP();
		boolean removedHeading = false;
		for (Map doc : docs) {
			removedHeading = false;
			txt = null;
			txtStr = null;
			secStr = null;
			hdgStr = null;
			mHdStr = null;
			subStr = null;
			str = null;
			if (doc.containsKey("sec")) {
				secStr = doc.get("sec").toString();
			}
			if (doc.containsKey("hdg")) {
				hdgStr = doc.get("hdg").toString();
			}
			if (doc.containsKey("mHd")) {
				mHdStr = doc.get("mHd").toString();
			}
			if (doc.containsKey("sub")) {
				subStr = doc.get("sub").toString();
			}

			if (doc.containsKey("txt") && (secStr != null || hdgStr != null || mHdStr != null || subStr != null)) {
				txtStr = doc.get("txt").toString();
				if (!removedHeading && secStr != null && txtStr.length() > 6 + secStr.length()
						&& txtStr.substring(0, secStr.length()).equals(secStr)) {
//					System.out.println("1. sec txt str=" + txtStr.substring(0, Math.min(65, txtStr.length())));
					str = txtStr.substring(0, secStr.length() + 6);
					str = str.replace(secStr, "").trim();
//					System.out.println("1. sec str repl="+str);
					if (nlp.getAllMatchedGroups(str, Pattern.compile("^[\\.;:]{0,1} ? ?\\.?([\\n]{0,4}[A-Z])"))
							.size() > 0) {
						txtStr = txtStr.substring(secStr.length())
								.replaceAll("^[\\.;:]{0,1} ? ?\\.?(?=[\\n]{0,4}[A-Z])", "").trim();
						removedHeading = true;
//						System.out.println("remove this sec heading from txt field==" + secStr);
					}
				}
				if (!removedHeading && hdgStr != null && txtStr.length() > hdgStr.length() + 6
						&& txtStr.substring(0, hdgStr.length()).equals(hdgStr)) {
//					System.out.println("1. hdg txt str=" + txtStr.substring(0, Math.min(65, txtStr.length())));
					str = txtStr.substring(0, hdgStr.length() + 6);
					str = str.replace(hdgStr, "").trim();
//					System.out.println("1. hdg str repl="+str);
					if (nlp.getAllMatchedGroups(str, Pattern.compile("^[\\.;:]{0,1} ? ?\\.?([\\n]{0,4}[A-Z])"))
							.size() > 0) {
						txtStr = txtStr.substring(hdgStr.length())
								.replaceAll("^[\\.;:]{0,1} ? ?\\.?(?=[\\n]{0,4}[A-Z])", "").trim();
						removedHeading = true;
//						System.out.println("remove this hdg heading from txt field==" + hdgStr);
					}
				}
				if (!removedHeading && mHdStr != null && txtStr.length() > mHdStr.length() + 6
						&& txtStr.substring(0, mHdStr.length()).equals(mHdStr)) {
//					System.out.println("1. mHd txt str=" + txtStr.substring(0, Math.min(65, txtStr.length())));
					str = txtStr.substring(0, mHdStr.length() + 6);
					str = str.replace(mHdStr, "").trim();
//					System.out.println("1. sec str repl="+str);
					if (nlp.getAllMatchedGroups(str, Pattern.compile("^[\\.;:]{0,1} ? ?\\.?([\\n]{0,4}[A-Z])"))
							.size() > 0) {
						txtStr = txtStr.substring(mHdStr.length())
								.replaceAll("^[\\.;:]{0,1} ? ?(?=[\\n]{0,4}[A-Z])", "").trim();
						removedHeading = true;
//						System.out.println("remove this mHd heading from txt field==" + mHdStr);
					}
				}
				if (!removedHeading && subStr != null && txtStr.length() > subStr.length() + 6
						&& txtStr.substring(0, subStr.length()).equals(subStr)) {
//					System.out.println("1. sub txt str=" + txtStr.substring(0, Math.min(65, txtStr.length())));
					str = txtStr.substring(0, subStr.length() + 6);
					str = str.replace(subStr, "").trim();
//					System.out.println("1. sub str repl="+str);
					if (nlp.getAllMatchedGroups(str, Pattern.compile("^[\\.;:]{0,1} ? ?\\.?([\\n]{0,4}[A-Z])"))
							.size() > 0) {

						txtStr = txtStr.substring(subStr.length())
								.replaceAll("^[\\.;:]{0,1} ? ?\\.?(?=[\\n]{0,4}[A-Z])", "").trim();
						removedHeading = true;
//						System.out.println("remove this sub heading from txt field==" + subStr);
					}
				}
				if (txtStr == null || !removedHeading)
					continue;

//				System.out.println("removing heading");
				txt = doc.remove("txt");
				txt = txtStr;
//				System.out.println("repl hdg txt==" + txtStr.substring(0, Math.min(65, txtStr.length())));
				addField2Doc("txt", txt, doc);
			}

		}

		return docs;
	}

	public List<Map> removeSensitiveData(List<Map> docs, File jsonFile, String... fields) throws IOException {

		System.out.println("removing sensitive data");
		Map doc;
		String text = "";
		int hCnt, wCnt, cnt = 0;
		Iterator<Map> itr = docs.iterator();
		while (itr.hasNext()) {

			doc = itr.next();
			cnt++;
			if (cnt < 2)
				continue;

			if (doc.get("txt") == null)
				continue;

			wCnt = (int) doc.get("wCnt");
			if (doc.get("hCnt") == null && wCnt < 10) {
				itr.remove();
//				System.out.println("removed no hCnt. txt=" + doc.get("txt").toString());
				continue;
			}

			if (doc.get("hCnt") != null) {
				hCnt = (int) doc.get("hCnt");
			} else {
				hCnt = 0;
			}

			if (hCnt < 4 && wCnt < 10) {
//				System.out.println("removed, small hCnt==" + hCnt + "||txt=" + doc.get("txt").toString());
				itr.remove();
				continue;
			}

			if (100 * ((double) hCnt / (double) wCnt) < 16
					&& nlp.getAllMatchedGroups(doc.get("txt").toString(),
							Pattern.compile("THEREFORE|WITNESS|WHEREAS|WHEREOF|FURTHER|RESOLVE|SECURITIES ACT"))
							.size() == 0
					&& (5 + doc.get("txt").toString().replaceAll("[a-z]", "").length()) < doc.get("txt").toString()
							.length()) {
//				System.out.println("removed, low hCnt/wCnt ratiot=="+(100 * ((double) hCnt / (double) wCnt) )+"||txt=" + doc.get("txt").toString());
				itr.remove();
				continue;
			}

			text = doc.get("txt").toString(); // "typ" : [ 1, 3 ], "typ" : 4, "typ" : 1, || 1/3/4/40/...
//			if (patternRedact(text)) {
//				// discard the doc
//				itr.remove();
//				continue;
//			}
//			System.out.println("did not remove");
		}
		return docs;
	}

	public List<Map> removeRequiredFields(List<Map> docs, String... fields) {
		// if wCnt<7 -- contiune - don't parse this <doc>
		// if typ 1,3 or 1,3 -- continue - don't parse this <doc>
		// if parse <doc> remove <lead>, "vCnt", <vTxt> and hashVtxtId

		/*
		 * remove docs having 'typ' other than 1 and or 3 ([1,3]); typ:1, and typ:3.
		 * skip any <doc> if hCnt<6. remove lead txt field - We also don't need vTxt.
		 */
		Map doc;
		Object typObj;
		int typ, hCnt;
		Iterator<Map> itr = docs.iterator();
//		String gLaw;
		while (itr.hasNext()) {
			doc = itr.next();

			typObj = doc.get("typ"); // "typ" : [ 1, 3 ], "typ" : 4, "typ" : 1, || 1/3/4/40/...

			if (typObj instanceof Integer) {
				typ = (Integer) typObj;
				if (typ != 1 && typ != 3) {
					// discard the doc
					itr.remove();
					continue;
				}
			} else if (typObj instanceof List<?>) {
				List<Integer> typs = (List<Integer>) typObj;
				if (!(typs.contains(1) || typs.contains(3))) {
					// does not have 1 or 3. remove doc
					itr.remove();
					continue;
				}
			}

//			hCnt = (int) doc.get("hCnt");
//			if (hCnt < 4) {
//				itr.remove();
//				continue;
//			}

			// remove fields from doc
			for (String fld : fields) {
				doc.remove(fld);
			}
		}
		return docs;
	}

	// TODO: temporary
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<Map> fixGLawFields(List<Map> docs) {
		// remove ",gLaw" field, and put value in "gLaw"
		String gLaw;
		for (Map doc : docs) {
			doc.remove("glaw");
			gLaw = StringUtils.defaultIfBlank((String) doc.remove(",gLaw"), (String) doc.get("gLaw"));
			if (StringUtils.isNotBlank(gLaw))
				doc.put("gLaw", gLaw.trim());
		}
		return docs;
	}

	@SuppressWarnings({ "rawtypes", "unused" })

	public List<Map> addRequiredFields(List<Map> docs) throws IOException {
		Object contractNameOk = null, score = null, gLaw = null, parentGroup = null, ticker = null, sctr = null,
				ids = null, contractType = null, modify = null, contractNameAlgo = null;
		String company_name, id, kid, cik;
		int central_index_key = -1;
		int cnt = 0;

		NLP nlp = new NLP();
		for (Map doc : docs) {
			cnt++;
			// rename 'score' to 'scor'
			score = null;
//			if(doc.get("hashHtxtId")!=null) {
//			if(cnt>0 && doc.get("hashHtxtId")==null) {
//				addField2Doc("ALLCAPS", 1, doc);
			// If only text are stop words, initial caps, all caps and para#s then htxt is
			// null and this doc cannot be searched using hTxt. It is highly beneficial not
			// to search b/c the txt will generally have no contextual meaning.
//			}
//			System.out.println(doc.toString());
//			if (doc.containsKey("score")) {
//				score = doc.remove("score");
//				System.out.println("score==" + score);
//				if (null != score)
//					addField2Doc("scor", score, doc);
//			}

//			if (null != sctr)
//				addField2Doc("sctr", sctr, doc);
//			if (null != ids)
//				addField2Doc("ids", ids, doc);
//
//			if (doc.containsKey("cik") && (doc.get("cik") instanceof String)) {
//				doc.remove("cik");
//			}

//			id = doc.get("id").toString();
//			kid = nlp.getAllMatchedGroups(id, Pattern.compile(".*?_[\\d]+")).get(0);
//			kid = id.substring(0, id.indexOf("_", 21));
//			addField2Doc("kId", kid, doc);

			ensureHashOfHeadings(doc);
//			if (doc.containsKey("parentGroup")) {
//				parentGroup = doc.get("parentGroup");
//				addFieldInAllDocs(docs, jsonFile, "parentGroup", parentGroup);
//			}
//
//			if (doc.containsKey("gLaw")) {
//				gLaw = doc.get("gLaw");
//				if (!gLaw.toString().toLowerCase().contains("not found"))
//					addFieldInAllDocs(docs, jsonFile, "gLaw", gLaw);
//			}
//
//			if (doc.containsKey("contractType")) {
//				contractType = doc.get("contractType");
//				addFieldInAllDocs(docs, jsonFile, "contractType", contractType);
//			}
//
//			if (doc.containsKey("sctr")) {
//				sctr = doc.get("sctr");
//				addFieldInAllDocs(docs, jsonFile, "sctr", sctr);
//			}
//
//			if (doc.containsKey("ids")) {
//				ids = doc.get("ids");
//				addFieldInAllDocs(docs, jsonFile, "ids", ids);
//			}
//
//			if (doc.containsKey("ticker")) {
//				ticker = doc.get("ticker");
//				addFieldInAllDocs(docs, jsonFile, "ticker", ticker);
//			}
//
//			if (doc.containsKey("contractNameAlgo")) {
//				contractNameAlgo = doc.get("contractNameAlgo");
//				addFieldInAllDocs(docs, jsonFile, "contractNameAlgo", contractNameAlgo);
//			}
//
//			if (doc.containsKey("modify")) {
//				modify = doc.get("modify");
//				addFieldInAllDocs(docs, jsonFile, "modify", modify);
//			}
//
//			if (doc.containsKey("contractNameOk")) {
//				contractNameOk = doc.get("contractNameOk");
//				addFieldInAllDocs(docs, jsonFile, "contractNameOk", contractNameOk);
//			}
		}

		return docs;
	}

	@SuppressWarnings({ "rawtypes" })
	public List<Map> addParentGroup(List<Map> docs, String pGroup) {
		for (Map doc : docs) {
			addField2Doc("parentGroup", pGroup, doc);
		}
		return docs;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void addField2Doc(Object key, Object value, Map doc) {
		doc.put(key, value);
	}

	@SuppressWarnings("rawtypes")
	public List<Map> cleanESJsonData(String json,String date) throws IOException, ParseException {
		// This makes json file valid by cleaning it up. If it fails twice json cleaning
		// halts and inspect bad file#.

//		targetFileName = new File(targetFileName.getAbsoluteFile() + "//" + jsonFile.getName());
//		FileSystemUtils.copyFile(jsonFile, targetFileName);
//		jsonFile = targetFileName;

		List<Map> docs = makeJsonFileValid(json);
		if (null == docs)
			return null;
//		long starttime = System.currentTimeMillis();

		// remove undesired docs as well as fields from desired docs
		docs = removeRequiredFields(docs, json, "lead", "vCnt", "vTxt", "hashVtxtId");
		// any required field to be added to solr docs
//		System.out.println("add required Elastic Search fields");

//		if (removeSensitiveData)
//			removeSensitiveData(docs, json);

//		starttime = System.currentTimeMillis();
		if (removeSmallHcnt)
			removeSmallData(docs, json);

		removeAllHdgs(docs, json, null);

		// System.out.println("remove small hCnt took this
		// long="+(System.currentTimeMillis()-starttime)/1000+" seconds");
//		starttime = System.currentTimeMillis();

		docs = addRequiredFields(docs);
		docs = reviseFields(docs);// this checks when a hdg is in the txt and removes it.

		// for example: "txt":"Submission by Contractor. Contractor shall submit
		// copy..." where ,"hdg":"Submission by Contractor." result is: Contractor shall
		// submit copy...
//		System.out.println("addRequiredSolrFields took this long="+(System.currentTimeMillis()-starttime)/1000+" seconds");
//		starttime = System.currentTimeMillis();

		// parent group in each doc
//		if (null != parent_group)
//			docs = addParentGroup(docs, json, parent_group);

		docs = fixGLawFields(docs);
//		System.out.println("fixGLawFields took this long="+(System.currentTimeMillis()-starttime)/1000+" seconds");
//		starttime = System.currentTimeMillis();

		// write back map to json file
		// FileSystemUtils.writeToAsciiFile(json.getAbsolutePath(),
		// JSonUtils.object2JsonString(docs));

		// JSonUtils.prettyPrintInto(docs, json);
		addDateToMap(docs, date);
		return docs;

//		System.out.println("cleanSolrJsonData took " + (System.currentTimeMillis() - starttime) / 1000 + " seconds");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void ensureHashOfHeadings(Map doc) {
		List<String> hdgOrd = (List<String>) doc.get("hdgOrd");
		if (null == hdgOrd)
			hdgOrd = new ArrayList<>();
		else {
			String hd;
			String[] hdgs;
			for (int i = 0; i < hdgOrd.size(); i++) {
				hd = hdgOrd.get(i);
				if (hd.contains(",")) { // an error in data: hdgOrd:["exh,mHd"]
					hdgs = hd.split(",");
					hdgOrd.remove(i); // remove the joint value
					for (int j = 0; j < hdgs.length; j++) {
						hdgOrd.add(i + j, hdgs[j]);
					}
					i += hdgs.length - 1;
				}
			}
		}
		List<Long> hashHdgId = (List<Long>) doc.get("hashHdgId");
		if (null == hashHdgId)
			hashHdgId = new ArrayList<>();
		String[] hdgFields = { "sec", "exh", "def", "hdg", "mHd", "sub" };
		// PA: add exh/def/sec etc fields into hashHdgId / hdgOrd
		String hdgVal, htxt;
		GoLaw gl = new GoLaw();
		for (String hdg : hdgFields) {
			if (doc.containsKey(hdg) && !hdgOrd.contains(hdg)) {
				hdgOrd.add(hdg);
				// get value of heading field
				hdgVal = doc.get(hdg).toString();
				if (StringUtils.isBlank(hdgVal))
					continue;
				// get its hTxt - lower case first. It was missed earlier because the value must
				// be all/initial capital and thus hTxt was empty
//				htxt = gl.goLawGetHtxt(hdgVal.toLowerCase());//don't use htxt as the heading cleaned first.
				hashHdgId.add(Long.parseLong(hdgVal.hashCode() + "")); // put back
			}
		}
		if (hdgOrd.size() > 0) {
			doc.put("hdgOrd", hdgOrd);
			doc.put("hashHdgId", hashHdgId);
		}
	}

	public List<Map> cleanJsonFileOrFolder(String json,String date) throws IOException, ParseException {
		System.out.println("clean json");
//		if (fileOrFolder.isFile()) {
		firstPassDone = false;
		return cleanESJsonData(json,date);

//		}
	}

	public static List<String> getAllMatchedGroups(String text, String pattern) {
		return getAllMatchedGroups(text, Pattern.compile(pattern));
	}

	public static List<String> getAllMatchedGroups(String text, Pattern pattern) {
		Matcher matcher = pattern.matcher(text);
		int idx;
		List<String> idxs = new ArrayList<String>();
		while (matcher.find()) {
			for (int i = 1; i <= matcher.groupCount(); i++) {
				idx = matcher.start(i);
				if (idx >= 0) {
					idxs.add(matcher.group());
				}
			}
		}
		return idxs;
	}

	@SuppressWarnings({ "rawtypes" })
	public static List<Map> addFieldInAllDocs(List<Map> docs, String fieldName, Object fieldValue) {
		for (Map doc : docs) {
			addField2Doc(fieldName, fieldValue, doc);
		}
		return docs;
	}
	
	private void addDateToMap(List<Map> mapList, String date) throws ParseException {
		Date date1=new SimpleDateFormat("dd/MM/yyyy").parse(date);
		System.out.println(date+"\t"+date1+"\t"+date1.getTime());
		long dateInMillis = date1.getTime();
		String dateToIndex = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(dateInMillis);
		for (Map m : mapList) {
			m.put("fDate_Date", dateToIndex);
		}
	}

//	public static boolean patternRedact(String text) throws IOException {
//		// this finds keywords and other measures to determine if document text may have
//		// confidential information.
//		
//		System.out.println("REDACTING!");
//
//		NLP nlp = new NLP();
//		Pattern pattern = Pattern.compile(
//				"(?ism)((^| )(address:|apt.\\?|apt\\.?|apartment|attn|attention:?|avenue|ave|boulevard|blvd|email|floor|fl|name:?|ste"
//						+ "|street|st|suite|Inc|L\\.?P|L\\.?L\\.?C|L\\.?L\\.?P|N\\.?A|Nation Association"
//						+ "|[\\d]{5}-?([\\d]{4})?)(\\?:? ?$|[,:\\. ]))" + "|[\r\n]{1}[ ]+(by|title|directors?:|date:)"
//						+ "|\\.com|\\@| a [A-Z].{1,15}corporation");
//		if (nlp.getAllMatchedGroups(text.replaceAll("Perkins Coie.{0,5} ", "[  ]"), pattern).size() > 0 && nlp
//				.getAllMatchedGroups(text, Pattern.compile("(?ism)docusign.com|esign|privacy| name on ")).size() == 0) {
////			System.out.println("match==" + nlp.getAllMatchedGroups(text, pattern).get(0) + "||redacted txt=" + text);
//			return true;
//		} else
//			return false;
//	}

	public static void main(String[] arg) throws IOException, SQLException {
		elasticDocRectifier rectifier = new elasticDocRectifier();

//		rectifier.cleanESJsonData(new File("E:\\temp\\duplicate def.json"), new File("e:/temp/clean/"));
		File fileOrFolder = new File("E:\\temp\\solrDocs_Cleaned\\2020\\QTR3");
		File cleanFolder = new File("e:\\temp\\clean\\");
		// rectifier.cleanJsonFileOrFolder(fileOrFolder, cleanFolder);
//		String json = Utils.readTextFromFile("e:/temp/test2hdg.json");
//		check_and_cleanHeading(json);

	}
}
