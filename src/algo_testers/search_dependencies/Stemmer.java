package algo_testers.search_dependencies;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;


public class Stemmer {

	public static Pattern patternStopWords = Pattern.compile("(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" + ""
			+ "(in case)|(a)|(about)|(above)|(after)|(again)|(all)|(am)|(an)|(and)|(are)|(as)|(at)|(be)"
			+ "|(because)|(been)|(before)|(being)|(below)|(between)|(both)|(by)|(can)|(did)|(do)|(does)|(doing)|(down)"
			+ "|(during)|(each)|(either)|(few)|(for)|(from)|(further)|(had)|(has)|(have)|(having)|(he)|(her)|(here)|(hers)"
			+ "|(herself)|(him)|(himself)|(his)|(how)|(I)|(if)|(in)|(into)|(is)|(it)|(its)|(it\'s)|(itself)|(just)|(me)"
			+ "|(might)|(more)|(most)|(must)|(my)|(myself)|(need)|(now)|(of)|(off)|(on)|(once)|(only)|(or)|(other)|(otherwise)"
			+ "|(our)|(ours)|(ourselves)|(out)|(over)|(own)|(same)|(she)|(she\'s)|(should)|(so)|(some)|(such)|(than)"
			+ "|(that)|(the)|(their)|(theirs)|(them)|(themselves)|(then)|(there)|(these)|(they)|(this)|(those)|(through)"
			+ "|(to)|(too)|(under)|(until)|(up)|(very)|(very)|(was)|(we)|(were)|(what)|(when)|(where)|(whether)|(which)|(while)|(who)"
			+ "|(whom)|(why)|(will)|(with)|(won)|(would)|(you)|(your)|(yours)|(yourself)|(yourselves)" + ""
			+ ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");
	public static Pattern patternStopWordsOf2 = Pattern.compile("(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" + ""
			+ "(in case)|(with respect)|(in respect)|(respective)|(set forth)" + ""
			+ ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

	public static Pattern patternStopWordsContract = Pattern.compile("(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))"
			+ "(bonds?|notes?|debenture?s?|deed" + "|(certificate|note)holders?|certificates?|holders?"
			+ "|senior|junior|convertible|preferred|(this|the)[\r\n ]{1,3}(debt)"
			+ "|class|series|[A-Z]-\\d|securit[iesy]{1,3}"
			+ "|(this|the)[\r\n ]{1,3}(first|second|third|fourth|fifth|sixth)"
			+ "|supplementa?l?|indenture?|pooling and servicing|agreement" + "|(this|the)[\r\n ]{1,3}"
			+ "(trust)|guarante[yees]{1,3}|collateral documents?|documents?"
			+ "|[12]{1}[09]{1}[\\d]{1}-?\\d?)([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

	public static Pattern patternStopWordsLegal = Pattern
			.compile("(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" + "(aforementioned)|(aforesaid)|(applicable)|(foregoing)"
					+ "|(hereafter)|(hereby)|(herein)|(hereof)|(hereunder)|(herewith)|(hereto)|(means)"
					+ "|(pursuant)|(relation)|(relating)|(related)"
					+ "|(thereafter)|(thereby)|(therefor)|(therefore)|(therein)|(thereof)|(thereto)|(thereunder)"
					+ "|(regarding)|(respect)" + ")([\r\n\t \\)\\]\\}\":;\\.,]{1}|$)");

	// cannot be case insensitive.
	public static Pattern patternStopWordsLegalEntitySpecific = Pattern
			.compile("(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" + "(Administrators?)|(Affiliates?)|(Company)|((Conversion"
					+ "|Collateral|(Issuing and )?Paying )Agents?)|(Guarantor)" + "|(Issuer)|(Obligor)"
					+ "|((Security )?(Registrar))|(Trustee)" + "|(Agents?)|((Domestic ?)(Restricted ?)Subsidiary)"
					+ ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

	public static String[] stateAndCountryLongNames = { "ALASKA", "ALABAMA", "ARKANSAS", "AMERICAN[\r\n ]{0,3}SAMOA",
			"ARIZONA", "CALIFORNIA", "COLORADO", "CONNECTICUT", "DISTRICT[\r\n ]{0,3}OF[\r\n ]{0,3}COLUMBIA",
			"DELAWARE", "FLORIDA", "FEDERATED[\r\n ]{0,3}STATES[\r\n ]{0,3}OF[\r\n ]{0,3}MICRONESIA", "GEORGIA", "GUAM",
			"HAWAII", "IOWA", "IDAHO", "ILLINOIS", "INDIANA", "KANSAS", "KENTUCKY", "LOUISIANA", "MASSACHUSETTS",
			"MARYLAND", "MAINE", "MARSHALL[\r\n ]{0,3}ISLANDS", "MICHIGAN", "MINNESOTA", "MISSOURI",
			"NORTHERN[\r\n ]{0,3}MARIANA[\r\n ]{0,3}ISLANDS", "MISSISSIPPI", "MONTANA", "NORTH[\r\n ]{0,3}CAROLINA",
			"NORTH[\r\n ]{0,3}DAKOTA", "NEBRASKA", "NEW[\r\n ]{0,3}HAMPSHIRE", "NEW[\r\n ]{0,3}JERSEY",
			"NEW[\r\n ]{0,3}MEXICO", "NEVADA", "NEW[\r\n ]{0,3}YORK", "OHIO", "OKLAHOMA", "OREGON", "PENNSYLVANIA",
			"PUERTO[\r\n ]{0,3}RICO", "PALAU", "RHODE[\r\n ]{0,3}ISLAND", "SOUTH[\r\n ]{0,3}CAROLINA",
			"SOUTH[\r\n ]{0,3}DAKOTA", "TENNESSEE", "TRUST[\r\n ]{0,3}TERRITORIES", "TEXAS", "UTAH",
			"WEST[\r\n ]{0,3}VIRGINIA", "VIRGIN[\r\n ]{0,3}ISLANDS", "VERMONT", "WASHINGTON", "WISCONSIN", "VIRGINIA",
			"WYOMING" };

	// each of the above and below string[] arrays must line up so that I can
	// run loop.

	// replace with ZZ preceding b/c I can then use ZZ as common word in order
	// to treat each of these as if they were the same for aggregation of common
	// patterns.
	public static String[] stateAndCountryAbbrevs = { "AK", "AL", "AR", "AS", "AZ", "CA", "CO", "CT", "DC", "DE", "FL",
			"FM", "GA", "GU", "HI", "IA", "ID", "IL", "IN", "KS", "KY", "LA", "MA", "MD", "ME", "MH", "MI", "MN", "MO",
			"MP", "MS", "MT", "NC", "ND", "NE", "NH", "NJ", "NM", "NV", "NY", "OH", "OK", "OR", "PA", "PR", "PW", "RI",
			"SC", "SD", "TN", "TT", "TX", "UT", "WV", "VI", "VT", "WA", "WI", "VA", "WY" };

	public static String removeStopWordsOf2(String text) {

		text = patternStopWordsOf2.matcher(text).replaceAll("");

		return text;

	}

	public static String removeStopWords(String text) {

		text = text.replaceAll("'s|,|'", " ");
//				System.out.println("text at replace StopWordsContract - text="+text);
		text = patternStopWordsContract.matcher(text).replaceAll("");

//		 System.out.println("before replace legal stopwords - text ="+text);

		text = patternStopWordsLegal.matcher(text).replaceAll("");
//		text = PatternsDif.patternStopWordsLegalEntity.matcher(text).replaceAll("");
//		System.out.println("before patternStopWordsLegalEntitySpecific - text =" + text);

//		text = PatternsDif.patternStopWordsLegalEntitySpecific.matcher(text).replaceAll("");

//		System.out.println("before replaceUsingTwoStringArrays - text =" + text);

		text = replaceUsingTwoStringArrays(stateAndCountryLongNames, stateAndCountryAbbrevs, text);

//		System.out.println("before patternStopWords - text =" + text);

		text = patternStopWords.matcher(text).replaceAll("");

		return text;

	}

	public static String replaceUsingTwoStringArrays(String[] wordsToReplace, String[] replacedWith, String text) {
		for (int i = 0; i < wordsToReplace.length; i++) {
			text = text.replaceAll("(?i)" + wordsToReplace[i], replacedWith[i]);
		}
		return text;
	}

	/**
	 * Takes a string, removes definitions, stop-words, 2-word stopwords, stem each remaining word, and ensures that each stem-word is piece of its original/source word.
	 * @param text source text
	 * @return string of stemmed-words, separated by space
	 */
	public static String tokenized_def_stop_stem(String text) {
		text = removeDefsStopwords(text);
		//System.out.println("text removeDefsStopwords. text="+text);
		LancasterStemmer lc = new LancasterStemmer();
		String[] words = text.replaceAll("[ ]+", " ").split("[ ]+");
		StringBuilder sb = new StringBuilder();
		String sw;
		for (String w : words) {
			sw = lc.stemWord(w);
			if (StringUtils.isBlank(sw))
				continue;
			sw = ensureSourceWordStartsWithStemmedWord(w, sw);
			sb.append(sw).append(" ");
		}
		//text = StringUtils.join(lc.stemWords(text.replaceAll("[ ]+", " ").split("[ ]+")), " ");
		//text = text.replaceAll("[ ]+", " ").trim();
		return sb.toString().trim();
	}

	/**
	 * Takes a string, removes definitions, stop-words, 2-word stopwords, stem each remaining word, and maps to its original/source word.
	 * @param text
	 * @return	List of KeyValuePair such that [ {key=hTxtWord, value=sourceWord}, .....]
	 */
	public static List<KeyValuePair<String, String>> removeDefsStopwordsStem_IncludeSourceWord(String text) {
		text = removeDefsStopwords(text);
		// stem words and map to source word 
		List<KeyValuePair<String, String>> htWords = new ArrayList<>();
		LancasterStemmer lc = new LancasterStemmer();
		String[] words = text.split("((?<=[\\p{Punct}} ])|(?=[\\p{Punct}} ]))");		//replaceAll("[ ]+", " ").
		String ht;
		for (String w : words) {
			if (StringUtils.isBlank(w)  ||  w.length() <= 2)
				continue;
			ht = lc.stemWord(w.trim());
			if (StringUtils.isBlank(ht))
				continue;
			//ht = ensureSourceWordStartsWithStemmedWord(w, ht);
			htWords.add(new KeyValuePair<String, String>(ht, w.trim()));
		}
		return htWords;
	}
	
	public static void ensureSourceWordStartsWithStemmedWord(List<KeyValuePair<String, String>> hTxtWord2SourceWordPair) {
		// ensure each source word starts-with corresponding hTxt word. else cut initial few letters (upto key length) from source-word and treat that as hTxt word
		for (KeyValuePair<String, String> ht2W : hTxtWord2SourceWordPair) {
			ht2W.setKey(ensureSourceWordStartsWithStemmedWord(ht2W.getValue(), ht2W.getKey()) );
			/*
			if(ht2W.getValue().startsWith(ht2W.getKey())) {
				ht2W.setKey(ht2W.getKey());
			} else {
				ht2W.setKey( ht2W.getValue().substring(0, ht2W.getKey().length()) );
			}
			*/
		}
	}
	
	public static String ensureSourceWordStartsWithStemmedWord(String sourceWord, String stemWord) {
		// ensure each source word starts-with corresponding hTxt word. else cut initial few letters (upto key length) from source-word and treat that as hTxt word
		if(sourceWord.startsWith(stemWord)) {
			return stemWord;
		} else {
			return sourceWord.substring(0, stemWord.length());
		}
	}
	
	

	// ******************************************** //
	private static String removeDefsStopwords(String text) {
		text = text.replaceAll("(\\w+)-(\\w+?)", "$1 $2");		// "third-party" => "third party"
		// clean words having 1-2 letters only
		//System.out.println("text. repl1=="+text);
		text = text.replaceAll("(^| )[A-Za-z]{1}(?= |$)", "");
		//System.out.println("text. repl2=="+text);
		// clean sent counter ie  (vi) / (a) / (iii) /.....
		text = text.replaceAll("([\\(\\[][a-z\\d]{1,3}[\\)\\]])|([\\p{Punct}}])", "");
		//System.out.println("text. repl3=="+text);

		// must remove stop words that are 2 or more words first - eg "In case" or "in case" - what about 
		// otherwise removeDefinedTerms will pick-up "In". 
		text = removeStopWordsOf2(text);
		//System.out.println("text. repl4=="+text);

		// Definitions - 1st Capital letter :- remove
		text = text.replaceAll("(?sm)[A-Z]+[A-Za-z]+", " ").trim();
		//System.out.println("text. repl5=="+text);

		text = removeStopWords(text).replaceAll("[ ]+", " ").trim();
		//System.out.println("text. repl6=="+text);

		return text;
	}
	
	
	public static void main(String[] arg) {
		System.out.println(tokenized_def_stop_stem("hell this is automobile any no and sunshine , indenture / agreement"));
	}
}
