package contracts;

import java.text.Normalizer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.zookeeper.cli.LsCommand;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import charting.JSonUtils;
import util.GoLawLancasterStemmer;
import xbrl.FileSystemUtils;
import xbrl.MysqlConnUtils;
import xbrl.NLP;
import xbrl.Utils;

public class HeadingNamesCleanup {

	public static TreeMap<String, String[]> map_Headings_exh_cdn_json = new TreeMap<String, String[]>();

	public static TreeMap<String, String[]> map_Headings_sec_cdn_json = new TreeMap<String, String[]>();

	public static TreeMap<String, String[]> map_Headings_hdg_sub_mHd_cdn_json = new TreeMap<String, String[]>();

	public static TreeMap<String, String[]> map_Headings_def_cdn_json = new TreeMap<String, String[]>();

//	public static TreeMap<String, String[]> map_Headings_contractNameAlgo_cdn_json = new TreeMap<String, String[]>();

	public static TreeMap<String, Integer> map_Headings_exh_json = new TreeMap<String, Integer>();
	public static TreeMap<String, Integer> map_Headings_sec_json = new TreeMap<String, Integer>();
	public static TreeMap<String, Integer> map_Headings_hdg_sub_mHd_json = new TreeMap<String, Integer>();
	public static TreeMap<String, Integer> map_Headings_def_json = new TreeMap<String, Integer>();
//	public static TreeMap<String, Integer> map_Headings_contractNameAlgo_json = new TreeMap<String, Integer>();
	public static String folderHdgs = "e:\\getContracts\\hdg_for_mysql\\";

	public static StringBuilder sbGlobal = new StringBuilder();
	public static int commitCnt = 0, globalCnt = 0;
	public static String hdgFilname = "";
	private static Pattern hdgPattern = Pattern.compile(

			"\"(exh|sec|hdg|mHd|sub|def"
//			+ "|contractNameAlgo

					+ "|exhL|secL|hdgL|mHdL|subL|defL)\"" + "(?> {0,2}" + ": {0,2}\")(.+?)(?=[\"\r\n]{1,2})");

	public static String paraNumbReplStr = "^ ?(\\([a-zA-Z\\d]{1,4}\\) ?)";// use for hashId
//identifies (a) and variants of para #s
	public static String simplePunctReplStr = "[\\.,;]+";// use for hashId

	public static String exhibitsReplStr = "^(EXHIBIT|Exhibit|SCHEDULE|Schedule|APPENDIX|Appendix|ANNEX|Annex) ";// Deepak:
																													// use
																													// for
																													// hashId

//	public static String contract_name_algo_ReplStr = "'S\'|S\\b|(^|\\b)[12]{1}[09]{1}[\\d]{2}\\b|[A-Z]+[\\d]+|-|\\bto\\b|&|"
//
//			+ "\\b(" + "[a-z]{1,2}|EX "
//			+ "|[\\d\\.]+|EXHIBIT|RESTATEMENT|ADDITIONAL|AMENDING|COPY|ENTERED|EXECUTION|INTO|IN|CONNECTION|WITH|THE|FIRST|SECOND|THIRD|FOURTH"
//			+ "|FIFTH|SIXTH|SEVENTH|EIGHTH|NINETH|TENTH|ELEVENTH|TWELFTH|VERSION|AMENDED AND RESTATED|AMENDMENT|AMENDED|RESTATED|AGREEMENT"
//			+ "|FORM OF|NO\\. [\\d]+|\'" + ")\\b"// surrounded by word boundry
//
//			+ "| (OF|THE|AND) |AGREEMENT";// Deepak:use for hashId for kname algo only. This
//											// will be expanded to include other replacement
//											// words based on Prahalad's finding in looking
//											// at your list.
//	// [12]{1}[09]{1}[\\d]{2}\\b == year -- 2021
//	// [A-Z]+[\\d]+ cap ltr follwed by #

	public static String jsonReplStr = "\"(exh|sec|hdg|mHd|sub|def"
//			+ "|contractNameAlgo"
			+ "|exhL|secL|hdgL|mHdL|subL|defL)\" ?: ?\"";

	public static String section_name_homog(String text) {
//Deepak: use for hashId
		text = text.replaceAll("(?sm)(Section|SECTION|Article|ARTICLE).{1,2}[\\d\\.]+", "")
				.replaceAll(paraNumbReplStr, "").replaceAll(simplePunctReplStr, "").replaceAll(jsonReplStr, "")
				.replaceAll("[ ]+", " ").trim();
		return text;
	}

	public static String definition_name_homog(String text) {
		// Deepak: use for hashId
		text = text.replaceAll(paraNumbReplStr, "").replaceAll(simplePunctReplStr, "").replaceAll(jsonReplStr, "")
				.replaceAll("[ ]+", " ").trim();

		return text;
	}

	public static String exhibit_name_homog(String text) {
		// Deepak: use for hashId
		text = text.replaceAll(paraNumbReplStr, "").replaceAll(simplePunctReplStr, "").replaceAll(jsonReplStr, "")
				.replaceAll(exhibitsReplStr, "").replaceAll("[ ]+", " ").trim();

		return text;
	}

//	public static String contract_name_algo_homog(String text) {
//		// Deepak: use for hashId
//		text = text.replaceAll(paraNumbReplStr, "").replaceAll(simplePunctReplStr, "").replaceAll(jsonReplStr, "")
//				.replaceAll(contract_name_algo_ReplStr, " ").replaceAll("[ ]+", " ").trim();
//		return text;
//	}

	public static boolean amendedContract(String text) {
		NLP nlp = new NLP();
		// Deepak: if it is amended we need to boolean capture that so we can include or
		// exclude. but I strip out amended etc so hashId is same as where there is no
		// amended, this is why we have the boolean from list of contracts we show user
		// based on their selection. let Prahalad/design and Vishnu know
		if (nlp.getAllIndexEndLocations(text, Pattern.compile("(?ism)amended|amendment|restated|amending")).size() > 0)
			return true;

		return false;
	}

	public static void add_simple_headings_map(String text) throws IOException {
		/*
		 * for each name: record the homogenized name (this is where you replace certain
		 * text that is superfluous), count how many times it occurs (but, you only give
		 * it one count per contract because a name can occur hundreds of times in the
		 * contract. But we only counted once per contract), record the section name
		 * that is not fully homogenized. Later, we perform a review of names and
		 * validate that those with high counts can be added to the list Vishnu uses.
		 * When we add it from the list we will use the name that was not fully
		 * homogenized.
		 * 
		 * for example: 1. hdg_user_sees = hdg_user_sees.replaceAll(hdgValRepl, "");
		 * 
		 * 2. hdg_homog_Repl = exhibit_name_homog(hdg_user_sees).toLowerCase(); 2.
		 * hdg_homog_Repl = StanfordParser.lemma(hdg_homog_Repl);
		 * 
		 * Number one is the section name that gets displayed in the list the user sees.
		 * Number two is the homogenized name used to create the hash ID and thereby the
		 * counts. each use patterns, that you can trace.
		 * 
		 * when finished, you should be able to create a list showing counts for each
		 * name type in descending order of count, with the highest count at the top.
		 * The name that is displayed is the one that is the actual name after the small
		 * amount of homogenization. In addition, since you are providing the aggregate
		 * count then you need to understand which name to show. Because our process
		 * removes potential erroneous information from the name, the one we wish to
		 * display is one that has the highest number of instances in the count for that
		 * name.
		 * 
		 * for examples: Governing Law is the section name, and it occurs 1000. But, you
		 * have instances where it was mistakingly written as Governing /Law. So we
		 * would want to display that one, even though it would be part of the account
		 * due to how we replace certain strings, so to avoid that you pick the name
		 * that occurs most frequently within the count of that name- which would then
		 * result in the selection of Governing Law and ignoring Governing/ Law. Because
		 * the latter will not occur with any frequency within that count.
		 */

	}

	public static void add_headings_map(String text) throws IOException {

		// Deepak: this method is far more complex than necessary. Your method will need
		// to create a count (outside of elastic) of each unique name( exhibit, section,
		// etc.). That count will be a count of that name after it is homogenized. We
		// use simple replace strings to homogenize.
		TreeMap<String, String> map_Headings_exh_one_JSON = new TreeMap<String, String>();
		TreeMap<String, String> map_Headings_sec_one_JSON = new TreeMap<String, String>();
		TreeMap<String, String> map_Headings_hdg_sub_mHd_one_JSON = new TreeMap<String, String>();
		TreeMap<String, String> map_Headings_def_one_JSON = new TreeMap<String, String>();
//		TreeMap<String, String> map_Headings_contractNameAlgo_one_JSON = new TreeMap<String, String>();

		NLP nlp = new NLP();
		List<String> list_headings = nlp.getAllMatchedGroups(text, hdgPattern);

		String hdg_homog_Repl, hdg_user_sees = "", hdgValRepl = "\"(exh|sec|hdg|mHd|sub|def"
//						+ "|contractNameAlgo"
				+ ")\".{0,1}:.{0,1}\"";
		for (int i = 0; i < list_headings.size(); i++) {
			hdg_user_sees = list_headings.get(i);

			if (hdg_user_sees.contains("\"exh\"")) {
				hdg_user_sees = hdg_user_sees.replaceAll(hdgValRepl, "");

				hdg_homog_Repl = exhibit_name_homog(hdg_user_sees).toLowerCase();
				hdg_homog_Repl = StanfordParser.lemma(hdg_homog_Repl);
				if (map_Headings_exh_one_JSON.containsKey(hdg_homog_Repl))
					continue;
				map_Headings_exh_one_JSON.put(hdg_homog_Repl, hdg_user_sees);
				continue;

			}
			if (hdg_user_sees.contains("\"def\"")) {
				hdg_user_sees = hdg_user_sees.replaceAll(hdgValRepl, "");
				hdg_homog_Repl = definition_name_homog(hdg_user_sees).toLowerCase();
				if (map_Headings_def_one_JSON.containsKey(hdg_homog_Repl))
					continue;
				map_Headings_def_one_JSON.put(hdg_homog_Repl, hdg_user_sees);
				continue;
			}
			if (hdg_user_sees.contains("\"sec\"")) {
				hdg_user_sees = hdg_user_sees.replaceAll(hdgValRepl, "");
				hdg_homog_Repl = section_name_homog(hdg_user_sees).toLowerCase();
				hdg_homog_Repl = StanfordParser.lemma(hdg_homog_Repl);

				if (map_Headings_sec_one_JSON.containsKey(hdg_homog_Repl))
					continue;
//				System.out.println("sec=="+hdg);
				map_Headings_sec_one_JSON.put(hdg_homog_Repl, hdg_user_sees);
				continue;
			}
			if (nlp.getAllIndexEndLocations(hdg_user_sees, Pattern.compile("\"(hdg|mHd|sub)\"")).size() > 0) {
				hdg_user_sees = hdg_user_sees.replaceAll(hdgValRepl, "");
				hdg_homog_Repl = section_name_homog(hdg_user_sees).toLowerCase();
				hdg_homog_Repl = StanfordParser.lemma(hdg_homog_Repl);
				if (map_Headings_hdg_sub_mHd_one_JSON.containsKey(hdg_homog_Repl))
					continue;
//				System.out.println("hdg="+hdg);
				map_Headings_hdg_sub_mHd_one_JSON.put(hdg_homog_Repl, hdg_user_sees);
				continue;
			}
//			if (nlp.getAllIndexEndLocations(hdg_user_sees, Pattern.compile("\"(contractNameAlgo)\"")).size() > 0) {
////				System.out.println("contractNameAlgo=" + hdg);
//				hdg_user_sees = hdg_user_sees.replaceAll(hdgValRepl, "");
//				hdg_homog_Repl = contract_name_algo_homog(hdg_user_sees).toLowerCase();
//				hdg_homog_Repl = StanfordParser.lemma(hdg_homog_Repl);
//				if (map_Headings_contractNameAlgo_one_JSON.containsKey(hdg_homog_Repl))
//					continue;
//				map_Headings_contractNameAlgo_one_JSON.put(hdg_homog_Repl, hdg_user_sees);
//				continue;
//			}
		}

//		NLP.printMapStringString("map_Headings_sec_one_JSON==", map_Headings_sec_one_JSON);
		// Deepak: below is used largely to obtain the count. what is count after
		// homogenized the other is the count prior to any homogenized. these are
		// generated for purposes of testing whether we have the right placement strings
		// for purposes of modularization. But these accounts will be run in eclipse
		for (Map.Entry<String, String> entry : map_Headings_sec_one_JSON.entrySet()) {
			if (map_Headings_sec_cdn_json.containsKey(entry.getKey())) {
				String[] ary = { entry.getValue(),
						(Integer.parseInt(map_Headings_sec_cdn_json.get(entry.getKey())[1]) + 1) + "",
						entry.getValue().hashCode() + "" };
				map_Headings_sec_cdn_json.put(entry.getKey(), ary);
				map_Headings_sec_json.put(hdg_user_sees, entry.getKey().hashCode());
				// TODO: Need to add count so we know the most common way
				// such hdg is displayed, it then is the name we assign
				// under (homog sec name). Needed for all uniq maps.
			} else {
//				System.out.println("sec.put=" + entry.getKey());
				String[] ary = { entry.getValue(), 1 + "", entry.getValue().hashCode() + "" };
				map_Headings_sec_cdn_json.put(entry.getKey(), ary);
				map_Headings_sec_json.put(hdg_user_sees, entry.getKey().hashCode());
			}
		}

		for (Map.Entry<String, String> entry : map_Headings_def_one_JSON.entrySet()) {
			if (map_Headings_def_cdn_json.containsKey(entry.getKey())) {
				String[] ary = { entry.getValue(),
						(Integer.parseInt(map_Headings_def_cdn_json.get(entry.getKey())[1]) + 1) + "",
						entry.getValue().hashCode() + "" };
				map_Headings_def_cdn_json.put(entry.getKey(), ary);
				map_Headings_def_json.put(hdg_user_sees, entry.getKey().hashCode());
			} else {
//				System.out.println("DEF.put=" + entry.getKey());
				String[] ary = { entry.getValue(), 1 + "", entry.getValue().hashCode() + "" };
				map_Headings_def_cdn_json.put(entry.getKey(), ary);
				map_Headings_def_json.put(hdg_user_sees, entry.getKey().hashCode());
			}
		}

		for (Map.Entry<String, String> entry : map_Headings_hdg_sub_mHd_one_JSON.entrySet()) {
			if (map_Headings_hdg_sub_mHd_cdn_json.containsKey(entry.getKey())) {
				String[] ary = { entry.getValue(),
						(Integer.parseInt(map_Headings_hdg_sub_mHd_cdn_json.get(entry.getKey())[1]) + 1) + "",
						entry.getValue().hashCode() + "" };
				map_Headings_hdg_sub_mHd_cdn_json.put(entry.getKey(), ary);
				map_Headings_hdg_sub_mHd_json.put(hdg_user_sees, entry.getKey().hashCode());
			} else {
				String[] ary = { entry.getValue(), 1 + "", entry.getValue().hashCode() + "" };
				map_Headings_hdg_sub_mHd_cdn_json.put(entry.getKey(), ary);
				map_Headings_hdg_sub_mHd_json.put(hdg_user_sees, entry.getKey().hashCode());
			}
		}

		for (Map.Entry<String, String> entry : map_Headings_exh_one_JSON.entrySet()) {
			if (map_Headings_exh_cdn_json.containsKey(entry.getKey())) {

				String[] ary = { entry.getValue(),
						(Integer.parseInt(map_Headings_exh_cdn_json.get(entry.getKey())[1]) + 1) + "",
						entry.getValue().hashCode() + "" };
				map_Headings_exh_cdn_json.put(entry.getKey(), ary);
				map_Headings_exh_json.put(hdg_user_sees, entry.getKey().hashCode());
			} else {
//				System.out.println("sec.put=" + entry.getKey());
				String[] ary = { entry.getValue(), 1 + "", entry.getValue().hashCode() + "" };
				map_Headings_exh_cdn_json.put(entry.getKey(), ary);
				map_Headings_exh_json.put(hdg_user_sees, entry.getKey().hashCode());
			}
		}

//		System.out.println("map_Headings_contractNameAlgo_one_JSON==" + map_Headings_contractNameAlgo_one_JSON.size());
//		for (Map.Entry<String, String> entry : map_Headings_contractNameAlgo_one_JSON.entrySet()) {
//			if (map_Headings_contractNameAlgo_cdn_json.containsKey(entry.getKey())) {
//
//				String[] ary = { entry.getValue(),
//						(Integer.parseInt(map_Headings_contractNameAlgo_cdn_json.get(entry.getKey())[1]) + 1) + "",
//						entry.getValue().hashCode() + "" };
//				map_Headings_contractNameAlgo_cdn_json.put(entry.getKey(), ary);
//				map_Headings_contractNameAlgo_json.put(hdg_user_sees, entry.getKey().hashCode());
//
//			} else {
//				String[] ary = { entry.getValue(), 1 + "", entry.getValue().hashCode() + "" };
//				map_Headings_contractNameAlgo_cdn_json.put(entry.getKey(), ary);
//				map_Headings_contractNameAlgo_json.put(hdg_user_sees, entry.getKey().hashCode());
//
//			}
//		}
	}/*
		 * public static TreeMap<String, String[]> map_Stem(TreeMap<String, String[]>
		 * map) { TreeMap<String, String[]> mapStem = new TreeMap<String, String[]>();
		 * 
		 * String keyStem = null; for (Map.Entry<String, String[]> entry :
		 * map.entrySet()) { // keyStem =
		 * goLawPropStemmer(entry.getKey().toLowerCase()); keyStem =
		 * StanfordParser.lemma(entry.getKey()); // System.out.println("key==" +
		 * entry.getKey() + " stem==" + keyStem); if (mapStem.containsKey(keyStem)) {
		 * mapStem.put(keyStem, mapStem.get(keyStem) + 1); } else { mapStem.put(keyStem,
		 * 1); } }
		 * 
		 * return mapStem; }
		 */

	public static String makeInitialCapsNamesAndHeadings(String hdg) throws IOException {

		NLP nlp = new NLP();
		Pattern patternStopWords = Pattern.compile("(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" + ""
				+ "(in case)|(a)|(about)|(above)|(after)|(again)|(all)|(am)|(an)|(any)|(and)|(are)|(as)|(at)|(be)"
				+ "|(because)|(been)|(before)|(being)|(below)|(between)|(both)|(by)|(can)|(day)|(did)|(do)|(does)|(doing)|(down)"
				+ "|(during)|(each)|(either)|(few)|(for)|(from)|(further)|(had)|(has)|(have)|(having)|(he)|(her)|(here)|(hers)"
				+ "|(herself)|(him)|(himself)|(his)|(how)|(I)|(if)|(in)|(into)|(is)|(it)|(its)|(it\'s)|(itself)|(just)|(me)"
				+ "|(might)|(more)|(most)|(must)|(my)|(myself)|(need)|(now)|(of)|(off)|(on)|(once)|(only)|(or)|(other)|(otherwise)"
				+ "|(our)|(ours)|(ourselves)|(out)|(over)|(own)|(same)|(she)|(she\'s)|(should)|(so)|(some)|(such)|(than)"
				+ "|(that)|(the)|(their)|(theirs)|(them)|(themselves)|(then)|(there)|(these)|(they)|(this)|(those)|(through)"
				+ "|(to)|(too)|(under)|(until)|(up)|(very)|(very)|(was)|(we)|(were)|(what)|(when)|(where)|(whether)|(which)|(while)|(who)"
				+ "|(whom)|(why)|(will)|(with)|(won)|(would)|(you)|(your)|(yours)|(yourself)|(yourselves)|(ADS)|(RSU)|(PSU)|(FSA)|(ESPP)|(As-Is)|(ESOP)"
				+ "" + ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

		hdg = patternStopWords.matcher(hdg).replaceAll("xx$2xx ").trim();
//		System.out.println("hdg stopwords marked=" + hdg);
		if (nlp.getAllIndexEndLocations(hdg, Pattern.compile("etc|\\d")).size() > 0)
			return "";

		String[] words = hdg.split(" ");
		String word = "", hdgInitialCaps = "";
		for (int i = 0; i < words.length; i++) {
			word = words[i];
			if (word.trim().length() < 1)
				continue;
			word = word.substring(0, 1).toUpperCase() + word.substring(1, word.length()).toLowerCase();
			if (nlp.getAllMatchedGroups(word, Pattern.compile("(?ism)xx[^ ].*?xx")).size() > 0) {
				word = word.toLowerCase().replaceAll("(xx)+", "");
//				System.out.println("words to intial cap is="+word);
			}
			hdgInitialCaps = hdgInitialCaps + " " + word;
		}

		hdgInitialCaps = hdgInitialCaps.trim();
//		System.out.println("hdgInitialCaps==" + hdgInitialCaps);
		return hdgInitialCaps;

	}

	public static String getDocZeroTerms(String json, String term) throws IOException {
		NLP nlp = new NLP();
//		System.out.println("getDocZeroTerm==" + term);
		List<String> list = nlp.getAllMatchedGroups(json,
				Pattern.compile("(?<=\" ?" + term + "\" ?: ?\\[? ?)\".*?\"\\]?.{0,3}\r\n"));
//		"allHdgs_m" : [ "

		String terms;

		System.out.println("table terms list.size==" + list.size());
		if (list.size() > 0) {
			terms = list.get(0);
//			System.out.println("terms list.get0=" + list.get(0));
			terms = terms.replaceAll("\"(.*?)\"", "$1\r\n");
			return terms;
		}
		return "";
	}

	public static String escapeCharacters(String text) {

		text = text.replaceAll("\\$", "\\\\\\$");

		return text;
	}

	public static String getHeadingLenght(String json) {

		String hdgL = "";
		// this will fetch for each def, sec, hdg, mHd, sub and exh their corresponding
		// length value of defL, secL, hdgL, subL and exhL

		return hdgL;

	}

	public static String universaleHeadingCleaner(String json) {
		// must run on json

//		long start = System.currentTimeMillis();
//		System.out.println("universaleHeadingCleaner start");
		// these are outlier cleanups
//		System.out.println("json bef="+json);
		json = json.replaceAll("(?ism)(def|mHd|sec|sub|hdg)(\" ?: ?\")(L C)( |\")", "$1$2L/C$4")
				.replaceAll("(?ism)(def|mHd|sec|sub|hdg)(\" ?: ?\")(.{1,25} )(L C)( |\")", "$1$2$3L/C$5")

				.replaceAll("(?ism)(def|mHd|sec|sub|hdg)(\" ?: ?\")(A R)( |\")", "$1$2L/C$4")
				.replaceAll("(?ism)(def|mHd|sec|sub|hdg)(\" ?: ?\")(.{1,25} )(A R)( |\")", "$1$2$3L/C$5")

				.replaceAll("(?ism)(def|mHd|sec|sub|hdg)(\" ?: ?\")(f k a)( |\")", "$1$2L/C$4")
				.replaceAll("(?ism)(def|mHd|sec|sub|hdg)(\" ?: ?\")(.{1,25} )(f k a)( |\")", "$1$2$3L/C$5");

//		System.out
//		.println("2.universaleHeadingCleaner finished. seconds=" + ((System.currentTimeMillis() - start) / 1000));
//		 start = System.currentTimeMillis();
		json = json.replaceAll("(?<=(sec|def|hdg|mHd|sub)\" ?: ?\")" + "(" + "(SECTION|Section)( (\\(?[\\d]+\\)?)\\.?"
				+ "(\\(?[\\d]+\\.?\\d?\\d?|\\(?[a-zA-Z]{1,4}[\\)\\.]{1,2})? ?)" + "|(\\(?[a-zA-Z\\d]{1,7}\\)) ?"
				+ "|[\\dA-Za-z]{1,3}\\.([\\d]+\\.?)+ " // 1.10. OR 7.2.9 or 7.2.9.
				+ "|[\\d]{1,3}\\.([\\d]+)?(\\.[\\d]+\\.?)? ?" + "|[A-Za-z]{1}\\. ?(?=[A-Za-z]{2})"// 1.1.1
				+ "|(SECTION|Section)[\\d]+\\.([\\d]+)?(?=[A-Z]{1})" + ")", "");
//		System.out.println("univ hdg==");
		// <===primary heading cleaner.
//		System.out
//		.println("3.universaleHeadingCleaner finished. seconds=" + ((System.currentTimeMillis() - start) / 1000));
//		 start = System.currentTimeMillis();
		json = json.replaceAll("(?<=(sec|def|hdg|mHd|sub)\" ?: ?\")(\\.|[\\d]+.[\\d]+) ", "");
//		System.out
//		.println("4.universaleHeadingCleaner finished. seconds=" + ((System.currentTimeMillis() - start) / 1000));
//		 start = System.currentTimeMillis();
		json = json.replaceAll("(?i)(?<=(exh)\" ?: ?\")" + "(EXHIBIT|ANNEX|APPENDIX|SCHEDULE) [A-Za-z\\d\\-\\_]{1,6} ",
				"");// <===primary exhibit heading cleaner.
//		System.out
//		.println("5.universaleHeadingCleaner finished. seconds=" + ((System.currentTimeMillis() - start) / 1000));
//		 start = System.currentTimeMillis();
		json = json.replaceAll("(?sm)(?<=(sec|def|hdg|mHd|sub)\" ?: ?\".{1,90})([;\\.,:]{1})" + "(?=\"([\r\n,]{1}|$))",
				"");
		json = json.replaceAll("(?sm)(sec|def|hdg|mHd|sub)(\" ?: ?\")(\\.)(.{1,90}\")([\r\n,]{1}|$)", "$1$2$4$5");

//		System.out
//		.println("6.universaleHeadingCleaner finished. seconds=" + ((System.currentTimeMillis() - start) / 1000));
//		 start = System.currentTimeMillis();
//		System.out
//				.println("universaleHeadingCleaner finished. seconds=" + ((System.currentTimeMillis() - start) / 1000));

		return json;
	}

	public static String createAllHeadingsFields(String json) throws IOException {
		NLP nlp = new NLP();
		// run after universalHeadingCleaner. must run on json

		TreeMap<String, String> map = new TreeMap<String, String>();
		List<String> list = nlp.getAllMatchedGroups(json,
				Pattern.compile("(?<=\r\n.{1,5})\"(exh|sec|mHd|hdg|sub|def)\" ?: ?\".*?(?=\r\n)"));
//		System.out.println("list of hdgs size==" + list.size());
		String str = "";
		for (int i = 0; i < list.size(); i++) {
			str = list.get(i);

			if (str.replaceAll("\"(exh|sec|mHd|hdg|sub|def)\" ?: ?\"\"?,?", "").length() < 2) {
//				System.out.println("list.get-i:" + list.get(i) + "\r\n..but=||"
//						+ str.replaceAll("\"(exh|sec|mHd|hdg|sub|def)\" ?: ?\"\"?,?", "") + "||");
				continue;
			}
			map.put(str, "");
		}

//		NLP.printMapStrStr("json map headings", map);

		StringBuilder sbDefs = new StringBuilder();
		StringBuilder sbHdgs = new StringBuilder();
		StringBuilder sbExhs = new StringBuilder();

		StringBuilder sbDefs_m = new StringBuilder();// multi value field if necessary later. can also reparse ES by
														// grabbing each unique hdg from json and pushing into a
														// multi-value field.
		StringBuilder sbHdgs_m = new StringBuilder();
		StringBuilder sbExhs_m = new StringBuilder();

		String key = "", key_m = "";
//		System.out.println("map.size===" + map.size());
		for (Map.Entry<String, String> entry : map.entrySet()) {
			key = entry.getKey();
			key_m = key;
//			System.out.println("key==" + key);
			if (key.contains("\"def\"")) {
				key = key.replaceAll("\"def\" ?: ?", "");
				key = key.replaceAll("\"|,$", "");
				sbDefs.append(key + " ");
				key_m = (key_m.replaceAll("\"def\" ?: ?", "") + ",").replaceAll(",,", ",");
				sbDefs_m.append(key_m);
//				System.out.println("adding defs.=" + key);
				continue;
			}
			if (!key.contains("\"def\"") && !entry.getKey().contains("\"exh\"")) {
				key = key.replaceAll("\"(hdg|sec|mHd|sub)\" ?: ?", "");
				key = key.replaceAll("\"|,$", "");
				sbHdgs.append(key + " ");
//				System.out.println("def key=="+key);
				key_m = (key_m.replaceAll("\"(hdg|sec|mHd|sub)\" ?: ?", "") + ",").replaceAll(",,", ",");
				sbHdgs_m.append(key_m);
//				System.out.println("adding hdgs.=" + key_m);
				continue;
			}
			if (key.contains("\"exh\"")) {
				key = key.replaceAll("\"exh\" ?: ?", "");
				key = key.replaceAll("\"|,$", "");
				sbExhs.append(key + " ");
				key_m = (key_m.replaceAll("\"exh\" ?: ?", "") + ",").replaceAll(",,", ",");
				sbExhs_m.append(key_m);
//				System.out.println("adding exhs_m=" + key_m);
				continue;
			}
		}

		String allHdgs = "", allDefs = "", allExhs = "";
		int allHdgsCnt = sbHdgs.toString().split(" ").length + 1, allDefsCnt = sbDefs.toString().split(" ").length + 1,
				allExhsCnt = sbExhs.toString().split(" ").length + 1;
		if (sbHdgs.toString().length() < 5) {
			allHdgsCnt = 0;
			allHdgs = "";
//			System.out.println("no allhdgs!");
		} else {
//			System.out.println("adding allHdgs..." + sbHdgs.toString().substring(0, sbHdgs.length() - 1));
			allHdgs = "\"allHdgs\" : \"" + sbHdgs.toString().substring(0, sbHdgs.length() - 1) + "\",\r\n";
		}

		if (sbExhs.toString().length() < 5) {
			allExhsCnt = 0;
			allExhs = "";
		} else {
			allExhs = "\"allExhs\" : \"" + sbExhs.toString().substring(0, sbExhs.length() - 1) + "\",\r\n";
		}

		if (sbDefs.toString().length() < 5) {
			allDefsCnt = 0;
			allDefs = "";
		} else {
			allDefs = "\"allDefs\" : \"" + sbDefs.toString().substring(0, sbDefs.length() - 1) + "\",\r\n";
//			System.out.println("adding allDefs==="+allDefs+"||");		
		}

		String headings = "";
		if (allHdgs.length() > 0) {
			// wasn't added???
//			System.out.println("added all hdgs to headings." + allHdgs);
			headings = allHdgs + "\"allHdgsCnt\" : " + allHdgsCnt + ",\r\n";
		}
		if (allDefs.length() > 0) {
			headings = headings + allDefs + "\"allDefsCnt\" : " + allDefsCnt + ",\r\n";
		}
		if (allExhs.length() > 0) {
			headings = headings + allExhs + "\"allExhsCnt\" : " + allExhsCnt + ",\r\n";
		}

		String headings_m = "", allHdgs_m = "", allDefs_m = "", allExhs_m = "";
		int allHdgsCnt_m = sbHdgs_m.toString().split("\",\"").length,
				allDefsCnt_m = sbDefs_m.toString().split("\",\"").length,
				allExhsCnt_m = sbExhs_m.toString().split("\",\"").length;
		if (sbHdgs_m.toString().length() < 5) {
			allHdgsCnt_m = 0;
			allHdgs_m = "";
//			System.out.println("no allHdgs!");
		} else {
//			System.out.println("adding allHdgs..." + sbHdgs_m.toString().substring(0, sbHdgs_m.length() - 1));
			allHdgs_m = "\"allHdgs_m\" : [" + sbHdgs_m.toString().substring(0, sbHdgs_m.length() - 1) + "],\r\n";
		}

		if (sbExhs_m.toString().length() < 5) {
			allExhsCnt_m = 0;
			allExhs_m = "";
		} else {
			allExhs_m = "\"allExhs_m\" : [" + sbExhs_m.toString().substring(0, sbExhs_m.length() - 1) + "],\r\n";
//			System.out.println("allExhs_m" + allExhs_m);
		}

		if (sbDefs_m.toString().length() < 5) {
			allDefsCnt_m = 0;
			allDefs_m = "";
		} else {
//			System.out.println("adding allDefs");
			allDefs_m = "\"allDefs_m\" : [" + sbDefs_m.toString().substring(0, sbDefs_m.length() - 1) + "],\r\n";
		}

		if (allHdgs_m.length() > 0) {

//			System.out.println("added all hdgs to headings." + allHdgs);
			headings_m = allHdgs_m + "\"allHdgsCnt_m\" : " + allHdgsCnt_m + ",\r\n";
		}
		if (allDefs_m.length() > 0) {
			headings_m = headings_m + allDefs_m + "\"allDefsCnt_m\" : " + allDefsCnt_m + ",\r\n";
		}
		if (allExhs_m.length() > 0) {
			headings_m = headings_m + allExhs_m + "\"allExhsCnt_m\" : " + allExhsCnt_m + ",\r\n";
		}

		// fix json here with (allHdgs)
//		System.out.println("sbDefs=" + sbDefs.toString());
//		System.out.println("sbhdgs=" + sbHdgs.toString());
//		System.out.println("heading I'm adding to json are==" + headings);
//		System.out.println("json.len bef removing allQuotedTermsTxt=" + json.length());
//		PrintWriter pw = new PrintWriter(new File("c:/temp/aqt bef json.json"));
//		pw.append(json);
//		pw.close();
		String alqt = "", alqt_m = "";
		if (nlp.getAllMatchedGroups(json, Pattern.compile("(?ism),? ?\"allQuotedTerms\".*?\r\n")).size() > 0) {
//			System.out.println("found alqt="+ nlp.getAllMatchedGroups(json, Pattern.compile("(?ism)\"allQuotedTerms\".*?\r\n")).get(0));
			alqt = nlp.getAllMatchedGroups(json, Pattern.compile("(?ism),? ?\"allQuotedTerms\".*?\r\n")).get(0);
			alqt = alqt.replaceAll("\", ?\"", " ").replaceAll("\\[|\\]", "");
			alqt = alqt + "\r\n\"allQuotedTermsCnt\":"
					+ alqt.replaceAll("[ ]+", " ").replaceAll("\" : \"|\r\n  ?", "\r\n").split(" ").length + ",\r\n";
//			System.out.println("found alqt=" + alqt);
			headings = headings + alqt;
		}

		if (nlp.getAllMatchedGroups(json, Pattern.compile("(?ism),? ?\"allQuotedTerms_m\".*?\r\n")).size() > 0) {
//			System.out.println("found alqt_m="+ nlp.getAllMatchedGroups(json, Pattern.compile("(?ism)\"allQuotedTerms_m\".*?\r\n")).get(0));
			alqt_m = nlp.getAllMatchedGroups(json, Pattern.compile("(?ism),? ?\"allQuotedTerms_m\".*?\r\n")).get(0);
			alqt_m = alqt_m.replaceAll("\", ?\"", " ").replaceAll("\\[|\\]", "");
			alqt_m = alqt_m + "\r\n\"allQuotedTermsCnt_m\":"
					+ alqt_m.replaceAll("[ ]+", " ").replaceAll("\" : \"|\r\n  ?", "\r\n").split(" ").length + ",\r\n";
//			System.out.println("found alqt_m=" + alqt_m);
			headings = headings + alqt_m;
		}
		headings = headings + headings_m;

//		System.out.println("heading I'm adding to json are==" + headings);
//		System.out.println("json.len aft removing allQuotedTerms=" + json.length());
//		pw = new PrintWriter(new File("c:/temp/aqt aft json.json"));
//		pw.append(json);
//		pw.close();
//		headings = headings
//				.replace("\",\r\n", "\\\"\",\r\n")
//				.replace("\\\"\\],\r\n", "\\\"\"],\r\n");

		String kId = "";
		Pattern pattern = Pattern.compile(",?kId.*?\r\n");
		Matcher matcher = pattern.matcher(json);
		while (matcher.find() && matcher.group().length() > 0) {
			kId = matcher.group();
			break;
		}
//		System.out.println("kId=="+kId);
		headings = "[{\r\n\"" + kId + "\r\n" + headings.trim().replaceAll(",$", "") + "}\r\n]";
		return headings;
	}

	public static void putHeadingIntoMySQL(File file, String tableTerm, int commitCounnt)
			throws IOException, SQLException {
		// must work off json

//		 System.out.println("putHeadingIntoMySQL -- file=" + file.getAbsolutePath());
		// table is same name as field to grab (allHdgs, allQuotedTerms, allDefs etc)
		NLP nlp = new NLP();

		String text = Utils.readTextFromFile(file.getAbsolutePath());
		text = text.substring(0, Math.min(text.length(), 100000));
		List<Integer> listDoc0 = nlp.getAllIndexEndLocations(text, Pattern.compile("\\},"));
		if (listDoc0.size() == 0) {
			System.out.println("no doc0....................................");
//			System.out.println("file=" + file.getAbsolutePath());
			return;
		}
		text = text.substring(0, listDoc0.get(0));

		System.out.println("\r\ntxt.len grabbed==" + text.length());

		// System.out.println("text=="+text.substring(0,4000));
		text = text.replaceAll("(?<=\")allExhs?_m(?=\" ?: ?\\[)", "allExhs_m");

		String fSize = nlp.getAllMatchedGroups(text, Pattern.compile("(?ism)(?<=fSize.{0,15})([\\d]{1,15})")).get(0)
				.trim();
		String fDate = nlp.getAllMatchedGroups(text, Pattern.compile("(?ism)(?<=fDate.{0,15})([\\d]{8})")).get(0)
				.trim();
		String kId = nlp.getAllMatchedGroups(text, Pattern.compile("(?ism)(?<=kId.{0,15})([\\d-_]+)")).get(0).trim();
		String cik = kId.substring(0, 10).replaceAll("^[0]+", "");
		String parentGroup = nlp
				.getAllMatchedGroups(text, Pattern.compile("(?ism)(?<=\"parentGroup\" ?: ?\").*?(?=\")")).get(0).trim();
		String contractType = nlp
				.getAllMatchedGroups(text, Pattern.compile("(?ism)(?<=\"contractType\" ?: ?\").*?(?=\")")).get(0)
				.trim();
		String contractNameAlgo = nlp
				.getAllMatchedGroups(text, Pattern.compile("(?ism)(?<=\"contractNameAlgo\" ?: ?\").*?(?=\")")).get(0)
				.trim();
		String gLaw = nlp.getAllMatchedGroups(text, Pattern.compile("(?ism)(?<=\"gLaw\" ?: ?\").*?(?=\")")).get(0)
				.trim();

		contractNameAlgo = escapeCharacters(contractNameAlgo).replaceAll("(?ism)&?zwnj;", "");

		System.out.println("parentGroup==" + parentGroup);
		System.out.println("tableTerm==" + tableTerm);
		String terms = getDocZeroTerms(text, tableTerm);
//		System.out.println("terms bef repl=\r\n" + terms + "|||");

		terms = terms// do NOT change the displayname - it must match in MySQL exactly with what is
						// in the json!! Any cleanup of the hdg has to happen at the
						// getAllHeadingsAndAllDefsFromJson method which implements the
						// universaleHeadingCleaner - which is what would be tweaked.
//				.replaceAll("(?<=\r\n ?)(\\([a-zA-Z\\d]{1,7}\\))", "")
//				.replaceAll("(?<=\r\n ?)\\. ?", "")
//				.replaceAll("(?<=\r\n ?,? ?|^ ?)(SECTION|Section) [\\d]+(\\.[\\d]+)? ?", "")
				.replaceAll(" ?\r\n,", "\r\n").replaceAll(" : \\[ ?|/ ?\\],?", "").replaceAll(", ", ",");
//		System.out.println("kId=" + kId + " cik=" + cik + " fDate=" + fDate + " parentGroup=" + parentGroup
//				+ " contractType=" + contractType + " contractNameAlgo=\r\n" + contractNameAlgo);
		terms = terms.replaceAll("\r\n",
				"\\|\\|" + kId + "\\|\\|" + cik + "\\|\\|" + fDate + "\\|\\|" + fSize + "\\|\\|" + parentGroup
						+ "\\|\\|" + contractType + "\\|\\|" + contractNameAlgo + "\\|\\|" + gLaw + "\r\n");
		terms = terms.replaceAll("\r\n,? ", "\r\n").replaceAll("[\\[\\]\\{\\}]+", "").trim();

		terms = terms.replaceAll("(?<=\r\n ?|^ ?)(\\([a-zA-Z\\d]{1,7}\\)) ?", "");
		System.out.println("terms before lines===" + terms + "||||||");
		String line = "", fieldTerm = "", origLine = "";
		String[] termsSpl = terms.split("\r\n");
		StringBuilder sb = new StringBuilder();
		// if all caps then lower case it else leave alone.
		boolean isAllCaps = false;
		for (int i = 0; i < termsSpl.length; i++) {
			isAllCaps = false;
			line = termsSpl[i];
			if (line.length() < 6) {
//				System.out.println("continue.....");
				continue;
			}

			line = line.replaceAll("^ ?\\.[\\d]+ ?|^ ?\\. ?", "").replaceAll("^ ?[\\d]+", "")
					.replaceAll("(?ism)^ ?(exhibit|annex|schedule|appendix) [A-Za-z\\d\\.\\-_]+ ", "").trim();
			origLine = line;
			fieldTerm = line.split("\\|\\|")[0];
			if (fieldTerm.replaceAll("[^A-Za-z]+|(?ism) ?and ?", "").trim().length() < 2)
				continue;

			if (fieldTerm.replaceAll("[^A-Z]+", "").length() == fieldTerm.replaceAll("[^A-Za-z]+", "").length()
					&& fieldTerm.trim().split(" ").length > 1
					&& nlp.getAllMatchedGroups(fieldTerm, Pattern.compile("[A-Z]{1}[a-z]{1}")).size() == 0) {
				// at least 2 words are all caps, no word is initial caps.
				isAllCaps = true;
			}
			if (isAllCaps && line.split("\\|\\|").length > 7) {
//				System.out.println("is allCaps fieldTerm=" + fieldTerm);
//				System.out.println("fieldTerm=" + fieldTerm + "\r\nrepl other than A-Z"
//						+ fieldTerm.replaceAll("[^A-Z]+", "").length() + " other than A-Za-z"
//						+ fieldTerm.replaceAll("[^A-Za-z]+", "").length());
//				System.out.println("makeInitialCapsNamesAndHeadings=" + makeInitialCapsNamesAndHeadings(fieldTerm));
				fieldTerm = makeInitialCapsNamesAndHeadings(fieldTerm);
				line = cleanField(fieldTerm) + "||" + line.split("\\|\\|")[1] + "||" + line.split("\\|\\|")[2] + "||"
						+ line.split("\\|\\|")[3] + "||" + line.split("\\|\\|")[4] + "||" + line.split("\\|\\|")[5]
						+ "||" + line.split("\\|\\|")[6] + "||" + line.split("\\|\\|")[7] + "||"
						+ line.split("\\|\\|")[8];
			}

			if (line.substring(0, 2).contains("||"))
				continue;

//			System.out.println("line to insert==" + line.length());
			List<String> listFieldTerm = nlp.getAllMatchedGroups(fieldTerm, Pattern.compile(".*?(?=;)"));// split at ';'
			String fieldCleaned = "";
			for (int c = 0; c < listFieldTerm.size(); c++) {
//				System.out.println("listFieldTerm.size==" + listFieldTerm.size());
//				System.out.println("fieldTerm=" + listFieldTerm.get(c));
//				System.out.println("1a. line==" + line);

				if (c + 1 < listFieldTerm.size()) {
					fieldTerm = listFieldTerm.get(c);
					if (isAllCaps)
						fieldTerm = makeInitialCapsNamesAndHeadings(fieldTerm);

					fieldCleaned = cleanField(fieldTerm).trim();
					if (fieldCleaned.trim().length() == 0 || line.split("\\|\\|").length < 7)
						continue;
//					System.out.println("aaa.line===" + line);
					line = "split_" + fieldCleaned + "||" + line.split("\\|\\|")[1] + "||" + line.split("\\|\\|")[2]
							+ "||" + line.split("\\|\\|")[3] + "||" + line.split("\\|\\|")[4] + "||"
							+ line.split("\\|\\|")[5] + "||" + line.split("\\|\\|")[6] + "||" + line.split("\\|\\|")[7]
							+ "||" + line.split("\\|\\|")[8];
//					System.out.println("1b. line==" + line);
					if (line.split("\\|\\|").length < 8)
						continue;

//					System.out.println("1b. split line ===" + line);
					sb.append(line.replaceAll(",", ", ").replaceAll("; ", "").replaceAll("[ ]+", " ") + "\r\n");
				}
				if (c + 1 == listFieldTerm.size() && origLine.indexOf("||") > 7
						&& origLine.lastIndexOf(";") < origLine.indexOf("||")) {
//					System.out.println("orig line==="+origLine);
					fieldTerm = origLine.substring(origLine.lastIndexOf(";"), origLine.indexOf("||"));
					if (isAllCaps)
						fieldTerm = makeInitialCapsNamesAndHeadings(fieldTerm);
					fieldCleaned = cleanField(fieldTerm).replaceAll(";", "").trim();
					if (fieldCleaned.trim().length() == 0)
						continue;
					line = "split_" + fieldCleaned + "||" + line.split("\\|\\|")[1] + "||" + line.split("\\|\\|")[2]
							+ "||" + line.split("\\|\\|")[3] + "||" + line.split("\\|\\|")[4] + "||"
							+ line.split("\\|\\|")[5] + "||" + line.split("\\|\\|")[6] + "||" + line.split("\\|\\|")[7]
							+ "||" + line.split("\\|\\|")[8];
//					System.out.println("1c. line==" + line);
					if (line.split("\\|\\|").length < 8)
						continue;

					sb.append(line.replaceAll("^ ?\\.[\\d]+ ?|^ ?\\. ?", "").replaceAll(",", ", ")
							.replaceAll("[ ]+", " ").replaceAll("^ ", "") + "\r\n");
				}
			}

			if (line.split("\\|\\|").length < 7)
				continue;

//			if (listFieldTerm.size() == 0) { record even if split with ';' as noted above.
			line = origLine.replaceAll("^ ?\\.[\\d]+ ?|^ ?\\. ?", "").replaceAll(",", ", ").replaceAll("[ ]+", " ")
					.replaceAll("^ ", "");
			sb.append(line + "\r\n");
//			System.out.println("2. line==" + line);
//			}

		}
		if (sb.toString().trim().length() < 5) {
//			System.out.println("no hdgs found ===file==\r\n"+file.getAbsolutePath());
			return;
		}
//		System.out.println("insert into table == terms=\r\n" + sb.toString());

		sbGlobal.append(sb.toString().trim());
		sbGlobal.append("\r\n");

		if (commitCnt == 0) {
			Utils.createFoldersIfReqd(folderHdgs);
			hdgFilname = folderHdgs + "/" + tableTerm + "_" + fDate.substring(0, 4) + "-" + fDate.substring(4, 6)
					+ "_cnt" + globalCnt + ".csv";
			globalCnt++;
		}

		commitCnt++;

		if (commitCnt > commitCounnt) {

			File f = new File(hdgFilname);
			if (f.exists())
				f.delete();
			Utils.writeTextToFile(f, sbGlobal.toString());
			loadIntoMysql_hh(f.getAbsolutePath().replaceAll("\\\\", "//"), tableTerm);
			f.delete();
			commitCnt = 0;
			sbGlobal = new StringBuilder();

		}
	}

	public static String cleanField(String fieldTerm) {

		fieldTerm = fieldTerm.replaceAll("^ ?(\\.[\\d]+ ?|\\. ?|- ?|; )", "").replaceAll("^ ?[\\d]+ ", "")
				.replaceAll("(?ism)^ ?and ", "")
				.replaceAll("(?ism)^ ?(exhibit|annex|schedule|appendix) [a-z\\d\\.-_]+ ", "").trim();

		return fieldTerm;
	}

	public static void loadIntoMysql_hh(String filename, String table) throws FileNotFoundException, SQLException {

		File file = new File(filename);
		long size = file.length();
		// System.out.println("loadFileIntoMysql fileSize:: " + size);
		String query = "SET GLOBAL local_infile =1;"

				+ "\r\n" + "LOAD DATA INFILE '" + filename + "' IGNORE INTO TABLE " + table.replaceAll("_m", "")
				+ " FIELDS TERMINATED BY '||' " + "lines terminated by " + "'\\n'" + ";";
//		 System.out.println("mysql query::" + query);
//		try {
		MysqlConnUtils.executeQuery(query);
//		} catch (SQLException e) {
//			e.printStackTrace(System.out);
//		}
	}

	public static void putHeadingIntoMySQLFromFileOrFolder(File fileOrFolder, String table, int cnt)
			throws FileNotFoundException, IOException, SQLException {

		int counter = 0;
		if (fileOrFolder.isFile()) {
			System.out.println("isFile===" + fileOrFolder.getAbsolutePath());
			putHeadingIntoMySQL(fileOrFolder, table, cnt);
//			System.out.println("writing to file for csv=" + fileOrFolder.getAbsolutePath());
			Utils.writeTextToFile(fileOrFolder, sbGlobal.toString());
			loadIntoMysql_hh(fileOrFolder.getAbsolutePath().replaceAll("\\\\", "//"), table);
			commitCnt = 0;
//			System.out.println("final insert happened");
			fileOrFolder.delete();
			sbGlobal = new StringBuilder();

			return;
		}
		File[] files = fileOrFolder.listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
//				System.out.println("putHeadingIntoMySQL - is folder==" + f.getAbsolutePath());
				putHeadingIntoMySQLFromFileOrFolder(f, table, cnt);
			} else {
				putHeadingIntoMySQL(f, table, cnt);
				counter++;
//				System.out.println("putHeadingIntoMySQL - is file==" + f.getAbsolutePath());
//				System.out.println("counter=="+counter+" files.len--="+files.length);
				if (counter == files.length) {
//					System.out.println("sbGlobal.length()=="+sbGlobal.length());
					File f2 = new File(hdgFilname);
					if (f2.exists())
						f2.delete();
					if (sbGlobal.length() == 0)
						continue;
					System.out.println("writing to file for csv=" + f2.getAbsolutePath());
					Utils.writeTextToFile(f2, sbGlobal.toString());
					loadIntoMysql_hh(f2.getAbsolutePath().replaceAll("\\\\", "//"), table);
					commitCnt = 0;
//					System.out.println("final insert happened");
					f2.delete();
					sbGlobal = new StringBuilder();
					// put remainder into mysql here.
				}

			}
		}

	}

	public static String flattenToAscii(String str) throws FileNotFoundException {

		long starttime = System.currentTimeMillis();
//		System.out.println("bef flattening ascii text string==."+str);
		str = str
				.replaceAll(
						"&#x201c;|&#x201d;|“|”|&#147;|&#148;|&#8221;|&#8220;|&ldquo;|&rdquo;|&quot;|&#x201C;|&#x201D",
						"\"")
				.replaceAll("&#x207d;|&#8317;", "(").replaceAll("&#x207e;", ")").replaceAll("(&sup)(\\d)(;)", "$2")
				.replaceAll("#xc0;|&#192;|&Agrave;", "À").replaceAll("&#xc1;|&#193;|&Aacute;", "Á")
				.replaceAll("&#xc2;|&#194;|&Acirc;", "Â").replaceAll("&#xc3;|&#195;|&Atilde;", "Ã")
				.replaceAll("&#xc4;|&#196;|&Auml;", "Ä").replaceAll("&#xc5;|&#197;|&Aring;", "Å")
				.replaceAll("&#xc6;|&#198;|&AElig;", "Æ").replaceAll("&#xc7;|&#199;|&Ccedil;", "Ç")
				.replaceAll("&#xc8;|&#200;|&Egrave;", "È").replaceAll("&#xc9;|&#201;|&Eacute;", "É")
				.replaceAll("&#xca;|&#202;|&Ecirc;", "Ê").replaceAll("&#xcb;|&#203;|&Euml;", "Ë")
				.replaceAll("&#xcc;|&#204;|&Lgrave;", "Ì").replaceAll("&#xcd;|&#205;|&Lacute;", "Í")
				.replaceAll("&#xce;|&#206;|&Lcirc;", "Î").replaceAll("&#xcf;|&#207;|&Luml;", "Ï")
				.replaceAll("&#xd0;|&#208;|&ETH;", "Ð").replaceAll("&#xd1;|&#209;|&Ntilde;", "Ñ")
				.replaceAll("&#xd2;|&#210;|&Ograve;", "Ò").replaceAll("&#xd3;|&#211;|&Oacute;", "Ó")
				.replaceAll("&#xd4;|&#212;|&Ocirc;", "Ô").replaceAll("&#xd5;|&#213;|&Otilde;", "Õ")
				.replaceAll("&#xd6;|&#214;|&Ouml;", "Ö").replaceAll("&#xd8;|&#216;|&Oslash;", "Ø")
				.replaceAll("&#xd9;|&#217;|&Ugrave;", "Ù").replaceAll("&#xda;|&#218;|&Uacute;", "Ú")
				.replaceAll("&#xdb;|&#219;|&Ucirc;", "Û").replaceAll("&#xdc;|&#220;|&Uuml;", "Ü")
				.replaceAll("&#xdd;|&#221;|&Yacute;", "Ý").replaceAll("&#xde;|&#222;|&THORN;", "Þ")
				.replaceAll("&#xdf;|&#223;|&szlig;", "ß").replaceAll("&#xe0;|&#224;|&agrave;", "à")
				.replaceAll("&#xe1;|&#225;|&aacute;", "á").replaceAll("&#xe2;|&#226;|&acirc;", "â")
				.replaceAll("&#xe3;|&#227;|&atilde;", "ã").replaceAll("&#xe4;|&#228;|&auml;", "ä")
				.replaceAll("&#xe5;|&#229;|&aring;", "å").replaceAll("&#xe6;|&#230;|&aelig;", "æ")
				.replaceAll("&#xe7;|&#231;|&ccedil;", "ç").replaceAll("&#xe8;|&#232;|&egrave;", "è")
				.replaceAll("&#xe9;|&#233;|&eacute;", "é").replaceAll("&#xea;|&#234;|&ecirc;", "ê")
				.replaceAll("&#xeb;|&#235;|&euml;", "ë").replaceAll("&#xec;|&#236;|&igrave;", "ì")
				.replaceAll("&#xed;|&#237;|&iacute;", "í").replaceAll("&#xee;|&#238;|&icirc;", "î")
				.replaceAll("&#xef;|&#239;|&iuml;", "ï").replaceAll("&#xf0;|&#240;|&eth;", "ð")
				.replaceAll("&#xf1;|&#241;|&ntilde;", "ñ").replaceAll("&#xf2;|&#242;|&ograve;", "ò")
				.replaceAll("&#xf3;|&#243;|&oacute;", "ó").replaceAll("&#xf4;|&#244;|&ocirc;", "ô")
				.replaceAll("&#xf5;|&#245;|&otilde;", "õ").replaceAll("&#xf6;|&#246;|&ouml;", "ö")
				.replaceAll("&#xf8;|&#248;|&oslash;", "ø").replaceAll("&#xf9;|&#249;|&ugrave;", "ù")
				.replaceAll("&#xfa;|&#250;|&uacute;", "ú").replaceAll("&#xfb;|&#251;|&ucirc;", "û")
				.replaceAll("&#xfc;|&#252;|&uuml;", "ü").replaceAll("&#xfd;|&#253;|&yacute;", "ý")
				.replaceAll("&#xfe;|&#254;|&thorn;", "þ").replaceAll("&#xff;|&#255;|&yuml;", "ÿ")
				.replaceAll("&#x0100;|&#256;|&Amacr;", "Ā").replaceAll("&#x0101;|&#257;|&amacr;", "ā")
				.replaceAll("&#x0102;|&#258;|&Abreve;", "Ă").replaceAll("&#x0103;|&#259;|&abreve;", "ă")
				.replaceAll("&#x0104;|&#260;|&Aogon;", "Ą").replaceAll("&#x0105;|&#261;|&aogon;", "ą")
				.replaceAll("&#x0106;|&#262;|&Cacute;", "Ć").replaceAll("&#x0107;|&#263;|&cacute;", "ć")
				.replaceAll("&#x0108;|&#264;|&Ccirc;", "Ĉ").replaceAll("&#x0109;|&#265;|&ccirc;", "ĉ")
				.replaceAll("&#x010A;|&#266;|&Cdot;", "Ċ").replaceAll("&#x010B;|&#267;|&cdot;", "ċ")
				.replaceAll("&#x010C;|&#268;|&Ccaron;", "Č").replaceAll("&#x010D;|&#269;|&ccaron;", "č")
				.replaceAll("&#x010E;|&#270;|&Dcaron;", "Ď").replaceAll("&#x010F;|&#271;|&dcaron;", "ď")
				.replaceAll("&#x0110;|&#272;|&Dstrok;", "Đ").replaceAll("&#x0111;|&#273;|&dstrok;", "đ")
				.replaceAll("&#x0112;|&#274;|&Emacr;", "Ē").replaceAll("&#x0113;|&#275;|&emacr;", "ē")
				.replaceAll("&#x0114;|&#276;", "Ĕ").replaceAll("&#x0115;|&#277;", "ĕ")
				.replaceAll("&#x0116;|&#278;|&Edot;", "Ė").replaceAll("&#x0117;|&#279;|&edot;", "ė")
				.replaceAll("&#x0118;|&#280;|&Eogon;", "Ę").replaceAll("&#x0119;|&#281;|&eogon;", "ę")
				.replaceAll("&#x011A;|&#282;|&Ecaron;", "Ě").replaceAll("&#x011B;|&#283;|&ecaron;", "ě")
				.replaceAll("&#x011C;|&#284;|&Gcirc;", "Ĝ").replaceAll("&#x011D;|&#285;|&gcirc;", "ĝ")
				.replaceAll("&#x011E;|&#286;|&Gbreve;", "Ğ").replaceAll("&#x011F;|&#287;|&gbreve;", "ğ")
				.replaceAll("&#x0120;|&#288;|&Gdot;", "Ġ").replaceAll("&#x0121;|&#289;|&gdot;", "ġ")
				.replaceAll("&#x0122;|&#290;|&Gcedil;", "Ģ").replaceAll("&#x0123;|&#291;", "ģ")
				.replaceAll("&#x0124;|&#292;|&Hcirc;", "Ĥ").replaceAll("&#x0125;|&#293;|&hcirc;", "ĥ")
				.replaceAll("&#x0126;|&#294;|&Hstrok;", "Ħ").replaceAll("&#x0127;|&#295;|&hstrok;", "ħ")
				.replaceAll("&#x0128;|&#296;|&Itilde;", "Ĩ").replaceAll("&#x0129;|&#297;|&itilde;", "ĩ")
				.replaceAll("&#x012A;|&#298;|&Imacr;", "Ī").replaceAll("&#x012B;|&#299;|&imacr;", "ī")
				.replaceAll("&#x012C;|&#300;", "Ĭ").replaceAll("&#x012D;|&#301;", "ĭ")
				.replaceAll("&#x012E;|&#302;|&Iogon;", "Į").replaceAll("&#x012F;|&#303;|&iogon;", "į")
				.replaceAll("&#x0130;|&#304;|&Idot;", "İ").replaceAll("&#x0131;|&#305;|&imath;", "ı")
				.replaceAll("&#x0132;|&#306;|&IJlig;", "Ĳ").replaceAll("&#x0133;|&#307;|&ijlig;", "ĳ")
				.replaceAll("&#x0134;|&#308;|&Jcirc;", "Ĵ").replaceAll("&#x0135;|&#309;|&jcirc;", "ĵ")
				.replaceAll("&#x0136;|&#310;|&Kcedil;", "Ķ").replaceAll("&#x0137;|&#311;|&kcedil;", "ķ")
				.replaceAll("&#x0138;|&#312;|&kgreen;", "ĸ").replaceAll("&#x0139;|&#313;|&Lacute;", "Ĺ")
				.replaceAll("&#x013A;|&#314;|&lacute;", "ĺ").replaceAll("&#x013B;|&#315;|&Lcedil;", "Ļ")
				.replaceAll("&#x013C;|&#316;|&lcedil;", "ļ").replaceAll("&#x013D;|&#317;|&Lcaron;", "Ľ")
				.replaceAll("&#x013E;|&#318;|&lcaron;", "ľ").replaceAll("&#x013F;|&#319;|&Lmidot;", "Ŀ")
				.replaceAll("&#x0140;|&#320;|&lmidot;", "ŀ").replaceAll("&#x0141;|&#321;|&Lstrok;", "Ł")
				.replaceAll("&#x0142;|&#322;|&lstrok;", "ł").replaceAll("&#x0143;|&#323;|&Nacute;", "Ń")
				.replaceAll("&#x0144;|&#324;|&nacute;", "ń").replaceAll("&#x0145;|&#325;|&Ncedil;", "Ņ")
				.replaceAll("&#x0146;|&#326;|&ncedil;", "ņ").replaceAll("&#x0147;|&#327;|&Ncaron;", "Ň")
				.replaceAll("&#x0148;|&#328;|&ncaron;", "ň").replaceAll("&#x0149;|&#329;|&napos;", "ŉ")
				.replaceAll("&#x014A;|&#330;|&ENG;", "Ŋ").replaceAll("&#x014B;|&#331;|&eng;", "ŋ")
				.replaceAll("&#x014C;|&#332;|&Omacr;", "Ō").replaceAll("&#x014D;|&#333;|&omacr;", "ō")
				.replaceAll("&#x014E;|&#334;", "Ŏ").replaceAll("&#x014F;|&#335;", "ŏ")
				.replaceAll("&#x0150;|&#336;|&Odblac;", "Ő").replaceAll("&#x0151;|&#337;|&odblac;", "ő")
				.replaceAll("&#x0152;|&#338;|&OElig;", "Œ").replaceAll("&#x0153;|&#339;|&oelig;", "œ")
				.replaceAll("&#x0154;|&#340;|&Racute;", "Ŕ").replaceAll("&#x0155;|&#341;|&racute;", "ŕ")
				.replaceAll("&#x0156;|&#342;|&Rcedil;", "Ŗ").replaceAll("&#x0157;|&#343;|&rcedil;", "ŗ")
				.replaceAll("&#x0158;|&#344;|&Rcaron;", "Ř").replaceAll("&#x0159;|&#345;|&rcaron;", "ř")
				.replaceAll("&#x015A;|&#346;|&Sacute;", "Ś").replaceAll("&#x015B;|&#347;|&sacute;", "ś")
				.replaceAll("&#x015C;|&#348;|&Scirc;", "Ŝ").replaceAll("&#x015D;|&#349;|&scirc;", "ŝ")
				.replaceAll("&#x015E;|&#350;|&Scedil;", "Ş").replaceAll("&#x015F;|&#351;|&scedil;", "ş")
				.replaceAll("&#x0160;|&#352;|&Scaron;", "Š").replaceAll("&#x0161;|&#353;|&scaron;", "š")
				.replaceAll("&#x0162;|&#354;|&Tcedil;", "Ţ").replaceAll("&#x0163;|&#355;|&tcedil;", "ţ")
				.replaceAll("&#x0164;|&#356;|&Tcaron;", "Ť").replaceAll("&#x0165;|&#357;|&tcaron;", "ť")
				.replaceAll("&#x0166;|&#358;|&Tstrok;", "Ŧ").replaceAll("&#x0167;|&#359;|&tstrok;", "ŧ")
				.replaceAll("&#x0168;|&#360;|&Utilde;", "Ũ").replaceAll("&#x0169;|&#361;|&utilde;", "ũ")
				.replaceAll("&#x016A;|&#362;|&Umacr;", "Ū").replaceAll("&#x016B;|&#363;|&umacr;", "ū")
				.replaceAll("&#x016C;|&#364;|&Ubreve;", "Ŭ").replaceAll("&#x016D;|&#365;|&ubreve;", "ŭ")
				.replaceAll("&#x016E;|&#366;|&Uring;", "Ů").replaceAll("&#x016F;|&#367;|&uring;", "ů")
				.replaceAll("&#x0170;|&#368;|&Udblac;", "Ű").replaceAll("&#x0171;|&#369;|&udblac;", "ű")
				.replaceAll("&#x0172;|&#370;|&Uogon;", "Ų").replaceAll("&#x0173;|&#371;|&uogon;", "ų")
				.replaceAll("&#x0174;|&#372;|&Wcirc;", "Ŵ").replaceAll("&#x0175;|&#373;|&wcirc;", "ŵ")
				.replaceAll("&#x0176;|&#374;|&Ycirc;", "Ŷ").replaceAll("&#x0177;|&#375;|&ycirc;", "ŷ")
				.replaceAll("&#x0178;|&#376;|&Yuml;", "Ÿ").replaceAll("&#x0179;|&#377;|&Zacute;", "Ź")
				.replaceAll("&#x017A;|&#378;|&zacute;", "ź").replaceAll("&#x017B;|&#379;|&Zdot;", "Ż")
				.replaceAll("&#x017C;|&#380;|&zdot;", "ż").replaceAll("&#x017D;|&#381;|&Zcaron;", "Ž")
				.replaceAll("U+00024|&#x24;|&#36;|&dollar;", "\\$").replaceAll("U+000A2|&#xa2;|&#162;|&cent;", "¢")
				.replaceAll("U+000A3|&#xa3;|&#163;|&pound;", "£").replaceAll("U+020AC|&#x20AC;|&#8364;|&euro;", "€")
				.replaceAll("U+000A5|&#xa5;|&#165;|&yen;", "¥").replaceAll("U+020A0|&#x20A0;|&#8352;", "₠")
				.replaceAll("U+020A1|&#x20A1;|&#8353;", "₡").replaceAll("U+000F7|&#xf7;|&#247;|&divide;", "÷")
				.replaceAll("U+02260|&#x2260;|&#8800;|&ne;", "≠").replaceAll("U+000B1|&#xb1;|&#177;|&plusmn;", "±")
				.replaceAll("U+000BC|&#xbc;|&#188;|&frac14;", "¼").replaceAll("U+000BD|&#xbd;|&#189;|&frac12;", "½")
				.replaceAll("U+000BE|&#xbe;|&#190;|&frac34;", "¾").replaceAll("U+02153|&#x2153;|&#8531;|&frac13;", "⅓")
				.replaceAll("U+02154|&#x2154;|&#8532;|&frac23;", "⅔").replaceAll("U+00027|&#x27;|&#39;|&apos;", "'")
				.replaceAll("U+000A7|&#xa7;|&#167;|&sect;", "§").replaceAll("U+000A9|&#xa9;|&#169;|&copy;", "©")
				.replaceAll("U+000AE|&#xae;|&#174;|&reg;", "®").replaceAll("U+000B6|&#xb6;|&#182;|&para;", "¶")
				.replaceAll("U+02010|&#x2010;|&#8208;|&hyphen;", "—").replaceAll("U+02011|&#x2011;|&#8209;", "—")
				.replaceAll("U+02012|&#x2012;|&#8210;", "—").replaceAll("U+02013|&#x2013;|&#8211;|&ndash;", "—")
				.replaceAll("U+02014|&#x2014;|&#8212;|&mdash;", "—")
				.replaceAll("U+02015|&#x2015;|&#8213;|&horbar;", "—").replaceAll("U+02016|&#x2016;|&#8214;|&Vert;", "—")
				.replaceAll("U+02018|&#x2018;|&#8216;|&lsquo;|&#145;|&lsaquo;", "'")
				.replaceAll("U+02019|&#x2019;|&#8217;|&rsquo;|&#146;|&rsaquo;", "'")
				.replaceAll("U+0201B|&#x201B;|&#8219;", "'").replaceAll("U+02122|&#x2122;|&#8482;|&trade;", "™")

		;

//		System.out.println("aft flattening ascii text string==."+str);
//		System.out.println("aft normalizer="+str);
//		System.out.println("aft \\\\u007 =="+sb.toString());

//		PrintWriter pw = new PrintWriter(new File("c:/temp/flatten.txt"));
//		pw.append(sb.toString());
//		pw.close();

//		System.out.println("flatten to ascii took " + (System.currentTimeMillis() - starttime) / 1000 + " seconds");
		return str;
	}

	public static String tempFixNoSpaceAfterDef(String text) throws IOException {

		text = text.replaceAll("([^ ])(\")([^ ])", "$1$2 $3");

		PrintWriter pw = new PrintWriter(new File("c:/temp/tempv12.txt"));
		pw.append(text);
		pw.close();

		return text;

	}

	public static String getAllHeadingFromJson(String json)
			throws JsonParseException, JsonMappingException, IOException {
		List<Map> docs = null;
		docs = JSonUtils.json2List(json, Map.class);
		String headings = "";
		// if typ=1 or 1,3 and sec/def/hdg/mHd/sub or exh present then get:
		// get secL/defL/subL/mHdL/hdgL/exhL and sec/def/sub/mHd/hdg/exh
		// get hashId of each hdg
		// get length of txt
		// get hashHtxtId
		// get hashTxtId
		// get kId
		// get fDate
		// get parent
		// get contractType
		// get contractNameAlgo
		// get id
		// if defL len==txt.len then this is entire heading. if defL !=txt.len then it
		// is a subpart. if not subpart label as such in

		String id, hashHtxtId, hashTxtId, hashHdgId, kId = null, kna = null, kt = null, p = null, fDate = null, secL,
				defL, hdgL, mHdL, subL, exhL, sec, def, hdg, mHd, sub, exh, txtL, typ, part, scKey = null, hdgTyp;
		int cnt = 0, typInt, rt;
		double ratio, txtLen, headLen;
		NLP nlp = new NLP();
		Object typObj;
		TreeMap<String, Integer> map_of_headings = new TreeMap<String, Integer>();

		for (Map doc : docs) {
			cnt++;
			if (cnt == 1)
				continue;
			// stub key id at sec level and if key exists then para does not equal entire
			// hdg. 0000950170-22-012464_1_57_5_285_0_0 - cut to 0000950170-22-012464_1_57

			hdgTyp = null;
			typ = null;
			typ = doc.get("typ").toString();
//			System.out.println("typ="+typ);
			if (typ.contains(",")) {
				typInt = Integer.parseInt(typ.split(",")[0].replaceAll("[^\\d]+", ""));
			} else {
				typInt = Integer.parseInt(typ.replaceAll("[^\\d]+", ""));
			}
			if (typInt != 1)
				continue;
//			System.out.println("id==" + doc.get("id").toString());
//			System.out.println(" typ=" + doc.get("typ").toString());
			if (doc.get("sec") != null)
				hdgTyp = "sec";
			if (doc.get("def") != null)
				hdgTyp = "def";
			if (doc.get("hdg") != null)
				hdgTyp = "hdg";
			if (doc.get("mHd") != null)
				hdgTyp = "mHd";
			if (doc.get("sub") != null)
				hdgTyp = "sub";
			if (hdgTyp == null)
				continue;

			scKey = doc.get("id").toString();
			scKey = nlp.getAllMatchedGroups(scKey, Pattern.compile("[\\d]{10}-\\d\\d-[\\d]{6}_[\\d]+_[\\d]+")).get(0);
//			System.out.println("secKey==" + secKey);

			if (doc.get(hdgTyp) != null && typInt == 1) {
				if (map_of_headings.containsKey(scKey)) {
					map_of_headings.put(scKey, map_of_headings.get(scKey) + 1);
				} else {
					map_of_headings.put(scKey, 1);
				}
			}
		}

//		NLP.printMapStrInt("map_of_headings==", map_of_headings);

		cnt = 0;
		int number_of_paras_in_heading = 0;
		StringBuilder sb = new StringBuilder();
		for (Map doc : docs) {
			number_of_paras_in_heading = 0;
			cnt++;
			if (cnt == 1) {
				kId = doc.get("kId").toString();
				kna = doc.get("contractNameAlgo").toString();
				kt = doc.get("contractType").toString();
				p = doc.get("parentGroup").toString();
				fDate = doc.get("fDate").toString();
//				System.out.println("kId=" + kId + " kna=" + kna + " parent=" + p + " ktyp=" + kt + " fDate=" + fDate);
				continue;
			}

			scKey = doc.get("id").toString();
			scKey = nlp.getAllMatchedGroups(scKey, Pattern.compile("[\\d]{10}-\\d\\d-[\\d]{6}_[\\d]+_[\\d]+")).get(0);

			if (doc.get("sec") != null && doc.get("typ").equals(1)) {// FIX
				sec = doc.get("sec").toString();
				secL = doc.get("secL").toString();
				hashHdgId = doc.get("sec").toString().hashCode() + "";
				txtL = (doc.get("txt").toString().length() + sec.length()) + "";
				hashTxtId = doc.get("hashTxtId").toString().hashCode() + "";
//				System.out.println("id=="+doc.get("id").toString());
				if (doc.get("hashHtxtId") != null) {
					hashHtxtId = doc.get("hashHtxtId").toString().hashCode() + "";
				} else {
					hashHtxtId = "0";
//					System.out.println("ALLCAPS or no words that are not removed txt=" + doc.get("txt").toString());
				}
				number_of_paras_in_heading = map_of_headings.get(scKey);
				sb.append(kId + "||" + fDate + "||" + kt + "||" + p + "||" + kna + "||" + number_of_paras_in_heading
						+ "||" + sec + "||" + secL + "||" + hashHdgId + "||" + txtL + "||" + hashTxtId + "||"
						+ hashHtxtId + "||sec\r\n");
			}

			if (doc.get("def") != null && doc.get("typ").equals(1)) {// FIX
				def = doc.get("def").toString();
				defL = doc.get("defL").toString();
				hashHdgId = doc.get("def").toString().hashCode() + "";
				txtL = (doc.get("txt").toString().length() + def.length()) + "";
				hashTxtId = doc.get("hashTxtId").toString().hashCode() + "";
//				System.out.println("id=="+doc.get("id").toString());
				if (doc.get("hashHtxtId") != null) {
					hashHtxtId = doc.get("hashHtxtId").toString().hashCode() + "";
				} else {
					hashHtxtId = "0";
//					System.out.println("ALLCAPS or no words that are not removed txt=" + doc.get("txt").toString());
				}
				number_of_paras_in_heading = map_of_headings.get(scKey);
				sb.append(kId + "||" + fDate + "||" + kt + "||" + p + "||" + kna + "||" + number_of_paras_in_heading
						+ "||" + def + "||" + defL + "||" + hashHdgId + "||" + txtL + "||" + hashTxtId + "||"
						+ hashHtxtId + "||def\r\n");
			}

			if (doc.get("hdg") != null && doc.get("typ").equals(1)) {// FIX
				hdg = doc.get("hdg").toString();
				hdgL = doc.get("hdgL").toString();
				hashHdgId = doc.get("hdg").toString().hashCode() + "";
				txtL = (doc.get("txt").toString().length() + hdg.length()) + "";
				hashTxtId = doc.get("hashTxtId").toString().hashCode() + "";
//				System.out.println("id=="+doc.get("id").toString());
				if (doc.get("hashHtxtId") != null) {
					hashHtxtId = doc.get("hashHtxtId").toString().hashCode() + "";
				} else {
					hashHtxtId = "0";
//					System.out.println("ALLCAPS or no words that are not removed txt=" + doc.get("txt").toString());
				}
				number_of_paras_in_heading = map_of_headings.get(scKey);
				sb.append(kId + "||" + fDate + "||" + kt + "||" + p + "||" + kna + "||" + number_of_paras_in_heading
						+ "||" + hdg + "||" + hdgL + "||" + hashHdgId + "||" + txtL + "||" + hashTxtId + "||"
						+ hashHtxtId + "||hdg\r\n");
			}

			if (doc.get("mHd") != null && doc.get("typ").equals(1)) {// FIX
				mHd = doc.get("mHd").toString();
				mHdL = doc.get("mHdL").toString();
				hashHdgId = doc.get("mHd").toString().hashCode() + "";
				txtL = (doc.get("txt").toString().length() + mHd.length()) + "";
				hashTxtId = doc.get("hashTxtId").toString().hashCode() + "";
//				System.out.println("id=="+doc.get("id").toString());
				if (doc.get("hashHtxtId") != null) {
					hashHtxtId = doc.get("hashHtxtId").toString().hashCode() + "";
				} else {
					hashHtxtId = "0";
//					System.out.println("ALLCAPS or no words that are not removed txt=" + doc.get("txt").toString());
				}
				number_of_paras_in_heading = map_of_headings.get(scKey);
				sb.append(kId + "||" + fDate + "||" + kt + "||" + p + "||" + kna + "||" + number_of_paras_in_heading
						+ "||" + mHd + "||" + mHdL + "||" + hashHdgId + "||" + txtL + "||" + hashTxtId + "||"
						+ hashHtxtId + "||mHd\r\n");
			}

			if (doc.get("sub") != null && doc.get("typ").equals(1)) {// FIX
				sub = doc.get("sub").toString();
				subL = doc.get("subL").toString();
				hashHdgId = doc.get("sub").toString().hashCode() + "";
				txtL = (doc.get("txt").toString().length() + sub.length()) + "";
				hashTxtId = doc.get("hashTxtId").toString().hashCode() + "";
//				System.out.println("id=="+doc.get("id").toString());
				if (doc.get("hashHtxtId") != null) {
					hashHtxtId = doc.get("hashHtxtId").toString().hashCode() + "";
				} else {
					hashHtxtId = "0";
//					System.out.println("ALLCAPS or no words that are not removed txt=" + doc.get("txt").toString());
				}
				number_of_paras_in_heading = map_of_headings.get(scKey);
				sb.append(kId + "||" + fDate + "||" + kt + "||" + p + "||" + kna + "||" + number_of_paras_in_heading
						+ "||" + sub + "||" + subL + "||" + hashHdgId + "||" + txtL + "||" + hashTxtId + "||"
						+ hashHtxtId + "||sub\r\n");
			}

		}

		System.out.println("hdgs==" + sb.toString());
		return headings;
	}

	public static void main(String[] args) throws FileNotFoundException, IOException, SQLException {
		GoLaw gl = new GoLaw();
		NLP nlp = new NLP();

		String json = Utils
				.readTextFromFile("E:\\TEMP\\0000002178-20-000105_1 Financial Contracts EX 10.1.json");
		PrintWriter pw = new PrintWriter(new File("e:/temp/hdgs.txt"));
		pw.append(createAllHeadingsFields(json));
		pw.close();
//
//		List<Map> docs = null;
//		docs = JSonUtils.json2List(json, Map.class);
//		getAllHeadingFromJson(json);
//		json = "  \"sec\" : \"SECTION 10.7 Non-Reliance on Administrative Agent and Other Lenders.\",";
//		System.out.println(universaleHeadingCleaner(json));
//		System.out.println(Temp.isJSONValid(json));

//		elasticDocRectifier rectifier = new elasticDocRectifier();
//		elasticDocRectifier.removeSmallHcnt = true;
//		int pushcommit = 100;
//
//		int sY = 2017, eY = 2017, sQ = 1, eQ = 1;
//		for (; sY <= eY; sY++) {
//			for (; sQ <= eQ; sQ++) {
//		String folder = "e:/getContracts/solrIngestion/solrDocs/" + sY + "/QTR" + sQ + "/";
//		File folder2Clean = new File(folder);
//				System.out.println("start folder2clean===" + folder2Clean.getAbsolutePath());
//				rectifier.cleanJsonFileOrFolder(folder2Clean);
//			}
//		}
//
//		DisplayNames.createListOfHeadingAndDefinitionDispolaynames("allDefs");
//		DisplayNames.createListOfHeadingAndDefinitionDispolaynames("allHdgs");
//		DisplayNames.createListOfHeadingAndDefinitionDispolaynames("allExhs");
//		DisplayNames.createListOfHeadingAndDefinitionDispolaynames("allQuotedTerms");

	}
}
