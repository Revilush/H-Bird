package contracts;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.Normalizer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.net.ssl.SNIHostName;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import util.GoLawLancasterStemmer;
import xbrl.ContractNLP;
import xbrl.ContractParser;
import xbrl.NLP;
import xbrl.Utils;

public class GoLaw {
	public static boolean print7c = false;
	public static boolean print7d = false;
	public static boolean print8a = false;
	public static boolean print9a = false;
	public static boolean print9c = false;

	public static TreeMap<Double, String[]> mapSections = new TreeMap<Double, String[]>();
	public static boolean forceParse = false;
	// *********** patternGoLawStopWords2 and patternGoLawStopWords MUST NOT BE
	// CHANGED. HTXT IS IMMUTABLE AFTER IT IS IN SOLR.
	// This method is run on client text queries and must use same
	// algorithms/methods as was used to parse into solr.

//	public static Pattern patternGoLawStopWordsOf2 = Pattern.compile("(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" + ""
//			+ "(in case)|(with respect)|(in respect)|(respective)|(set forth)" + ""
//			+ ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

	// *********** patternGoLawStopWords2 and patternGoLawStopWords MUST NOT BE
	// CHANGED. HTXT IS IMMUTABLE AFTER IT IS IN SOLR.
	// This method is run on client text queries and must use same
	// algorithms/methods as was used to parse into solr.

//	private static final Object OBJECT = pwSent;

//	public static String tmpTestStr = "";
	public static int mergedLinesCount = 0;
	public static int paraCount = 0;

	public static boolean pushTosolr = false;
	public static String clientScore = "";
//	public static boolean asciiHelperUsed = false;

	// aggressive marked patterns can be changed - very carefully! It removes words
	// like 'not' and 'no'. So use sparingly
	public static Pattern patternParaNmb = Pattern.compile("(" + "(?<=SECTION|Section)?( (\\(?[\\d]+\\)?)\\.?"
			+ "(\\(?[\\d]+\\.?\\d?\\d?|\\(?[a-zA-Z]{1,4}[\\)\\.]{1,2})? ?)" + "|(\\(?[a-zA-Z\\d]{1,7}\\)) ?"
			+ "|[\\dA-Za-z]{1,3}\\.([\\d]+\\.?)+ " // 1.10. OR 7.2.9 or 7.2.9.
			+ "|[\\d]{1,3}\\.([\\d]+)?(\\.[\\d]+\\.?)? ?" + "|[A-Za-z]{1}\\. ?(?=[A-Za-z]{2})"// 1.1.1
			+ "|(SECTION|Section)[\\d]+\\.([\\d]+)?(?=[A-Z]{1})" + ")");

	public static Pattern patternStopWordsAggressiveNotForHtxtorDisplayData = Pattern
			.compile("(?sm)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(a|able|about|across|aforementioned|" + "allows?|almost|"
					+ "along|already|also|although|am|among|amongst|"
					+ "an|and|another|anybody|anyhow|anyone|anything|anyways?|"
					+ "anywhere|apart|appears?|applicable|appreciates?|are|around|as|"
					+ "aside|ask|asking|associated|at|away|awfully|be|"
					+ "became|because|becomes?|becoming|been|before|beforehand|behind|"
					+ "being|below|besides?|better|between|" + "brief|by|came|can|cases?|"
					+ "comes?|company|consequently|consider|considering|" + "corporation|corresponding|could|course|"
					+ "described|did|do|does|doing|done|don't|down|"
					+ "downwards|during|e\\.?t\\.?c\\.?|e\\.?g\\.?|each|eg|eight|else|"
					+ "elsewhere|enough|etc|even|ever|everybody|" + "everyone|everything|everywhere|example|far|fifth|"
					+ "first|five|followed|following|follows|for|foregoing|former|formerly|"
					+ "forth|four|from|further|furthermore|gets?|getting|given|gives|go|"
					+ "goes|going|gone|got|gotten|greetings|had|has|have"
					+ "|having|he|hello|help|hence|her|here|hereafter|hereby|herein|"
					+ "hereunder|hereupon|hers|herself|hi|him|himself|his|hither|hopefully|"
					+ "how|howbeit|however|i|i\\.?e\\.?|ie|if|ignored|in|inasmuch|"
					+ "inc\\.?|incorporated|indeed|indicated|indicates?|inner|insofar|into|inward|is|"
					+ "it|its|itself|just|keeps?|kept|last|lest|let|like|liked|little|looking|looks|made|"
					+ "mainly|may|maybe|me|mean|means|meaning|meanwhile|merely|might|more|moreover|"
					+ "most|mostly|much|my|myself|name|namely|necessary|" + "needs?|nevertheless|next|nine|nobody|"
					+ "noone|normally|novel|now|nowhere|obviously|of|off|often|"
					+ "oh|ok|okay|on|once|ones?|only|onto|or|others?|ought|" + "ours?|ourselves|"
					+ "per|perhaps|placed|please|presumably|probably|provides|"
					+ "provisions?|que|quite|rather|re|really|regarding|regardless|regards|"
					+ "respectively|right|said|same|saw|saying|second|secondly|"
					+ "see|seeing|seemed|seemingl?y?|seems?|seen|self|selves|sensible|sent|"
					+ "serious|seriously|seven|shall|she|should|since|"
					+ "six|so|some|somebody|somehow|someone|something|sometimes?|somewhat|somewhere|"
					+ "sorry|still|such|sure|tell|tends|than|thanks?|that|the|theirs?|them|themselves|then|thence|"
					+ "thereafter|thereby|therefore|therein|thereon|therewith|theres?|thereunder|thereupon|these|they|"
					+ "think|third|this|those|though|three|through|throughout|thru|"
					+ "thus|to|together|too|took|towards?|tried|tries|truly|try|trying|twice|two|"
					+ "under|unfortunately|unto|up|us|used|useful|uses?|using|"
					+ "very|via|viz|vs|wants?|was|way|we|welcome|well|"
					+ "went|were|what|whatever|when|whence|whenever|where|whereafter|whereas|whereby|"
					+ "wherein|whereupon|wherever|whether|which|while|whither|who|whoever|whole|whom|whose|"
					+ "why|will|willing|wish|with|within|without|wonder|would|would|"
					+ "yet|you|yours?|yourself|yourselves|zero|\\([ivxc]{1,4}\\)|\\([a-z0-9]\\))([\\),;\\.: ]| ?$)");

	public static Pattern patternGoLawAggressiveStopWordsContract = Pattern
			.compile("(?ism)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))" + "(bonds?|notes?|debenture?s?|deed"
					+ "|(certificate|note)holders?|certificates?|holders?"
					+ "|senior|junior|convertible|preferred|(this|the)[\r\n ]{1,3}(debt)"
					+ "|class|series|[A-Z]-\\d|securities"
					+ "|(this|the)[\r\n ]{1,3}(first|second|third|fourth|fifth|sixth)" + "|supplementa?l?|indenture?|"
					// + "pooling and servicing|"
					+ "agreement" + "|(this|the)[\r\n ]{1,3}"
					+ "(trust)|guarante[yees]{1,3}|collateral documents?|documents?"
					+ "|[12]{1}[09]{1}[\\d]{1}-?\\d?)([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

	public static Pattern patternGoLawAggressiveStopWordsLegal = Pattern
			.compile("(?ism)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" + "(aforementioned)|(aforesaid)|(applicable)|(foregoing)"
					+ "|(hereafter)|(hereby)|(herein)|(hereinafter)|(hereof)|(hereunder)|(herewith)|(hereto)|(means)"
					+ "|(pursuant)|(relation)|(relating)|(related)"
					+ "|(thereafter)|(thereby)|(therefor)|(therefore)|(therefrom)|(therein)|(thereof)|(thereto)|(thereunder)"
					+ "|(regarding)|(respect)" + ")([\r\n\t \\)\\]\\}\":;\\.,]{1}|$)");

	public static Pattern patternGoLawAggressiveStopWordsOf2 = Pattern.compile("(?ism)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))("
			+ "(dated as of)|(in connection)|(in case)|(with respect)|(in respect)|(respective)|(set forth)|(in addition)|(including but not limited to)"
			+ "|(including without limitation)|(limited liability company)|(limited liability corporation)|(without limitation)"
			+ "" + ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

	// ***************** DO NOT CHANGE B/C IT CORRUPTS SEARCH AND MORE
	// *****************//
	public static Pattern patternGoLawHtxtStopWordsLimited = Pattern.compile("(?ism)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))("
			+ "" + "(a)|(about)|(above)|(after)|(again)|(all)|(am)|(an)|(and)|(are)|(as)|(at)|(be)"
			+ "|(because)|(been)|(before)|(being)|(below)|(between)|(both)|(by)|(can)|(did)|(do)|(does)|(doing)|(down)"
			+ "|(during)|(each)|(either)|(few)|(for)|(from)|(further)|(had)|(has)|(have)|(having)|(he)|(her)|(here)|(hers)"
			+ "|(herself)|(him)|(himself)|(his)|(how)|(I)|(in)|(into)|(is)|(it)|(its)|(it\'s)|(itself)|(just)|(me)"
			+ "|(might)|(most)|(must)|(my)|(myself)|(need)|(now)|(of)|(off)|(on)|(only)|(or)|(other)|(otherwise)"
			+ "|(our)|(ours)|(ourselves)|(out)|(over)|(own)|(same)|(she)|(she\'s)|(should)|(so)|(some)|(such)|(than)"
			+ "|(that)|(the)|(their)|(theirs)|(them)|(themselves)|(then)|(there)|(these)|(they)|(this)|(those)|(through)"
			+ "|(to)|(too)|(under)|(until)|(up)|(very)|(very)|(was)|(we)|(were)|(what)|(when)|(where)|(whether)|(which)"
			+ "|(while)|(who)"
			+ "|(whom)|(why)|(will)|(with)|(won)|(would)|(you)|(your)|(yours)|(yourself)|(yourselves)" + ""
			+ ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

	// ***************** DO NOT CHANGE B/C IT CORRUPTS SEARCH AND MORE
	// *****************//

	// ***************** DO NOT CHANGE B/C IT CORRUPTS SEARCH AND MORE
	// *****************//
	public static Pattern patternGoLawStopWordsContract = Pattern.compile("(?ism)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))"
			+ "(bonds?|notes?|debenture?s?|deed" + "|(certificate|note)holders?|certificates?|holders?"
			+ "|senior|junior|convertible|preferred|(this|the)[\r\n ]{1,3}(debt)" + "|class|series|[A-Z]-\\d|securities"
			+ "|(this|the)[\r\n ]{1,3}(first|second|third|fourth|fifth|sixth)" + "|supplementa?l?|indenture?"
			+ "|pooling and servicing" + "|agreement" + "|(this|the)[\r\n ]{1,3}"
			+ "(trust)|guarante[yees]{1,3}|collateral documents?|documents?"
			+ "|[12]{1}[09]{1}[\\d]{1}-?\\d?)([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");
	// ***************** DO NOT CHANGE B/C IT CORRUPTS SEARCH AND MORE
	// *****************//

	// ***************** DO NOT CHANGE B/C IT CORRUPTS SEARCH AND MORE
	// *****************//
	public static Pattern patternGoLawStopWordsLegal = Pattern.compile("(?ism)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))("
			+ "(aforementioned)|(aforesaid)|(applicable)|(foregoing)"
			+ "|(hereafter)|(hereby)|(herein)|(hereof)|(hereunder)|(herewith)|(hereto)|(means)"
			+ "|(pursuant)|(relation)|(relating)|(related)"
			+ "|(thereafter)|(thereby)|(therefor)|(therefore)|(therefrom)|(therein)|(thereof)|(thereto)|(thereunder)"
			+ "|(regarding)|(respect)" + ")([\r\n\t \\)\\]\\}\":;\\.,]{1}|$)");
	// ***************** DO NOT CHANGE B/C IT CORRUPTS SEARCH AND MORE
	// *****************//

	public static Pattern patternGoLawStopWordsOf2 = Pattern.compile("(?ism)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" + ""
			+ "(in case)|(with respect)|(in respect)|(respective)|(set forth)" + ""
			+ ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

	// ***************** DO NOT CHANGE B/C IT CORRUPTS SEARCH AND MORE
	// *****************//

	// ***************** DO NOT CHANGE B/C IT CORRUPTS SEARCH AND MORE
	// *****************//
	public static String[] goLawStateAndCountryLongNames = { "ALASKA", "ALABAMA", "ARKANSAS",
			"AMERICAN[\r\n ]{0,3}SAMOA", "ARIZONA", "CALIFORNIA", "COLORADO", "CONNECTICUT",
			"DISTRICT[\r\n ]{0,3}OF[\r\n ]{0,3}COLUMBIA", "DELAWARE", "FLORIDA",
			"FEDERATED[\r\n ]{0,3}STATES[\r\n ]{0,3}OF[\r\n ]{0,3}MICRONESIA", "GEORGIA", "GUAM", "HAWAII", "IOWA",
			"IDAHO", "ILLINOIS", "INDIANA", "KANSAS", "KENTUCKY", "LOUISIANA", "MASSACHUSETTS", "MARYLAND", "MAINE",
			"MARSHALL[\r\n ]{0,3}ISLANDS", "MICHIGAN", "MINNESOTA", "MISSOURI",
			"NORTHERN[\r\n ]{0,3}MARIANA[\r\n ]{0,3}ISLANDS", "MISSISSIPPI", "MONTANA", "NORTH[\r\n ]{0,3}CAROLINA",
			"NORTH[\r\n ]{0,3}DAKOTA", "NEBRASKA", "NEW[\r\n ]{0,3}HAMPSHIRE", "NEW[\r\n ]{0,3}JERSEY",
			"NEW[\r\n ]{0,3}MEXICO", "NEVADA", "NEW[\r\n ]{0,3}YORK", "OHIO", "OKLAHOMA", "OREGON", "PENNSYLVANIA",
			"PUERTO[\r\n ]{0,3}RICO", "PALAU", "RHODE[\r\n ]{0,3}ISLAND", "SOUTH[\r\n ]{0,3}CAROLINA",
			"SOUTH[\r\n ]{0,3}DAKOTA", "TENNESSEE", "TRUST[\r\n ]{0,3}TERRITORIES", "TEXAS", "UTAH",
			"WEST[\r\n ]{0,3}VIRGINIA", "VIRGIN[\r\n ]{0,3}ISLANDS", "VERMONT", "WASHINGTON", "WISCONSIN", "VIRGINIA",
			"WYOMING" };
	// ***************** DO NOT CHANGE B/C IT CORRUPTS SEARCH AND MORE
	// *****************//

	// each of the above and below string[] arrays must line up so that I can
	// run loop.

	// replace with ZZ preceding b/c I can then use ZZ as common word in order
	// to treat each of these as if they were the same for aggregation of common
	// patterns.

	// ***************** DO NOT CHANGE B/C IT CORRUPTS SEARCH AND MORE
	// *****************//
	public static String[] goLawStateAndCountryAbbrevs = { "AK", "AL", "AR", "AS", "AZ", "CA", "CO", "CT", "DC", "DE",
			"FL", "FM", "GA", "GU", "HI", "IA", "ID", "IL", "IN", "KS", "KY", "LA", "MA", "MD", "ME", "MH", "MI", "MN",
			"MO", "MP", "MS", "MT", "NC", "ND", "NE", "NH", "NJ", "NM", "NV", "NY", "OH", "OK", "OR", "PA", "PR", "PW",
			"RI", "SC", "SD", "TN", "TT", "TX", "UT", "WV", "VI", "VT", "WA", "WI", "VA", "WY" };
	// ***************** DO NOT CHANGE B/C IT CORRUPTS SEARCH AND MORE
	// *****************//

	public static String goLawReplaceUsingTwoStringArrays(String[] wordsToReplace, String[] replacedWith, String text) {
		for (int i = 0; i < wordsToReplace.length; i++) {

			/*
			 * *********** MUST NOT BE CHANGED. HTXT IS IMMUTABLE AFTER IT IS IN SOLR. This
			 * method is run on client text queries and must use same algorithms/methods as
			 * was used to parse into solr.
			 */

			text = text.replaceAll("(?i)" + wordsToReplace[i], replacedWith[i]);
		}
		return text;
	}

	public static String goLawRemoveStopWordsAndContractNamesLegalStopWords(String text) {

		/*
		 * *********** MUST NOT BE CHANGED. HTXT IS IMMUTABLE AFTER IT IS IN SOLR. This
		 * method is run on client text queries and must use same algorithms/methods as
		 * was used to parse into solr.
		 */

		text = text.replaceAll("'s|,|'", " ");
		text = patternGoLawStopWordsContract.matcher(text).replaceAll("");
		text = patternGoLawStopWordsLegal.matcher(text).replaceAll("");
		text = goLawReplaceUsingTwoStringArrays(goLawStateAndCountryLongNames, goLawStateAndCountryAbbrevs, text);

		text = patternGoLawHtxtStopWordsLimited.matcher(text).replaceAll("");

		return text;

	}

	private String solrCore2PushInto = null;
	private String baseSolrUrl = "http://localhost:8983/solr/";
	public static String metaHdgs = "";

	public boolean isNextConsecutive = true;
//	public boolean getFormattedHeadings = false;// for non-disclosure docs set to false (if contracts=true)
//	public boolean parsingContract = true;// set to 'true' if formType!=disclsure type (485BPOS, 497, etc)
//	public boolean parsingDisclosure = false;// set to 'tru'e if formType=disclosure (485BPOS, 497, etc)
	public String clauseText = "";
	public List<String> listFormattedHeaders = new ArrayList<String>();
	public List<String[]> listParagraphParentChild = new ArrayList<>();

	public String getSolrCore2PushInto() {
		return solrCore2PushInto;
	}

	public void setSolrCore2PushInto(String solrCore2PushInto) {
		this.solrCore2PushInto = solrCore2PushInto;
	}

	public String getBaseSolrUrl() {
		return baseSolrUrl;
	}

	public void setBaseSolrUrl(String baseSolrUrl) {
		this.baseSolrUrl = baseSolrUrl;
	}

	public Pattern patternExcludeMeFromHeadings = Pattern.compile(
			"EX-\\d|QxThis |QxTHIS |^(Qx)?RECITALS |Annex |ANNEX [A-Z\\d]|Appendix [A-Z\\d]|Qx\\([A-Za-z ,:;]{7,}|provided[X, ]"
					+ "|APPENDIX [A-Z\\d]|Exhibit [A-Z\\d]|EXHIBIT [A-Z\\d]|Schedule [A-Z\\d]|SCHEDULE [A-Z\\d]| SECTION | Section |W I T N | ARTICLE "
					+ "| ?QxNOW,? THEREFOREXQ ?| Article |Subject to |Street|St\\.|Blvd\\.?|Attention|ATTENTION|ATTN:|Attn:|attn:|BOULEVARD|Boulevard|Parkway|PARKWAY"
					+ "| drive| DRIVE| Corp\\.|CORP\\.|Drive| STE | ste | Place| PLACE| AVENUE | AVE\\.| Avenue| Ave\\.| ROAD| Road| road| Floor| floor| FLOOR| FL\\.|RECITAL|WHEREAS|(?i)Suite | Vice | VICE "
					+ "|Chief | CHIEF| [\\d]{5}([, ]|$)" + "|President |PRESIDENT"
					+ "|(?i)INC\\.|(?i)Bancorp|(?i)Pte(\\.| |$)|(?i)Table of Content"
					+ "|(?i)Signatures?<!\\.|[a-z]{1} (D\\.D\\.S|M\\.D| DR| Rd|Dr)\\."
					+ "|Signature Page to |SIGNATURE PAGE TO |Notwithstanding |NOTWITHSTANDING |Thereof|THEREOF|^[\\d]+  "
//					+ "|\\$"//don't include.
					+ "");

	public Pattern patternExhPrefFromTocHdg = Pattern.compile(
			"(Exhibit|EXHIBIT|APPENDIX|Appendix|SCHEDULE|Schedule|Annex" + "|ANNEX)[ \r\n]+([A-Ziabc\\d-\\.]{1,6})");

	public static String mergeLines(String text) throws IOException {

		String origText = text;
		Pattern startP = Pattern.compile("(?i)(<p [^>]*>|<p>)");
		Pattern endP = Pattern.compile("(?i)(</p>)");
		Pattern startTr = Pattern.compile("(?i)(<tr [^>]*>|<tr>)");
		Pattern endTr = Pattern.compile("(?i)(</(tr|table)>)");
		Pattern startTd = Pattern.compile("(?i)(<td [^>]*>|<td>)");
		Pattern endTd = Pattern.compile("(?i)(</td>)");

		String tmpStr;
		tmpStr = keepLinesInSamePara(text, startP, endP);
		System.out.println("<p cnt=" + paraCount + " mergedLinesCount=" + mergedLinesCount);
		if (mergedLinesCount > 1.1 * paraCount && paraCount > 0) {
			System.out.println("keep in same para");
			text = tmpStr;
		}

//		int paraCnt = NLP.getAllMatchedGroupsAndStartIdxLocs(text, Pattern.compile("(?ism)(</p>)")).size();
//		System.out.println("alt mergeLine cnt=="
//				+ NLP.getAllMatchedGroupsAndStartIdxLocs(text, Pattern.compile("(?ism)(\r|\n)")).size());
//		System.out.println("mergedLinesCount=" + mergedLinesCount + " paragraphs in doc=" + paraCnt);
//		if (mergedLinesCount > 5 * paraCnt) {
//			text = str;
//		}
		// don't add <div, <tr , <td etc - corrupts.
		return text;

	}

	public static String keepLinesInSamePara(String html, Pattern startPattern, Pattern endPattern) throws IOException {

		// this simply removes all hard returns within start and end pattern
		mergedLinesCount = 0;
		paraCount = 0;
		ContractNLP nlpK = new ContractNLP();
		NLP nlp = new NLP();

		html = html.replaceAll("<PRE>|</PRE>|<PAGE>|</PAGE>", "");

		StringBuffer sb = new StringBuffer();
		int start = 0, htmlLen = html.length();
		List<Integer> idxStartTrs = nlpK.getAllIndexStartLocations(html, startPattern);
		List<Integer> idxEndTrs = nlpK.getAllIndexStartLocations(html, endPattern);
		paraCount = idxStartTrs.size();
		if (idxStartTrs.size() == 0 || idxEndTrs.size() == 0) {
			// //NLP.pwNLP.append(NLP.println("no pattern found..");
			return html;
		}
		int endTrI = 0, endTrLoc = 0;

		for (Integer idxStartTr : idxStartTrs) {
			if (start > idxStartTr)
				continue;
			sb.append(new String("\r\n" + html.substring(start, idxStartTr)
//			.replaceAll("(?i)</?[pd]>", "")<===doesn't work. Corrupts other stuff.
			));
			// above is identifying JUST the start of the pattern - so we do NOT
			// want to replace anything here!!

			for (Integer eTr = endTrI; eTr < idxEndTrs.size(); eTr++) {
				endTrI++;
				endTrLoc = idxEndTrs.get(eTr);
				if (endTrLoc <= idxStartTr)
					continue;

				else {
					String htmlTemp = new String(html.substring(idxStartTr, endTrLoc));
					//
					mergedLinesCount = mergedLinesCount
							+ nlp.getAllMatchedGroups(htmlTemp, Pattern.compile("(?ism)\n")).size();
					htmlTemp = htmlTemp
							.replaceAll("(?<=[\\p{Alnum}\\p{Punct}])[\r\n]{1,}(?=[\\p{Alnum}\\p{Punct}])", " ")
							.replaceAll("[\r\n]{1,}", "").replaceAll(startPattern.toString(), " ")
							.replaceAll(endPattern.toString(), "").replaceAll("[ ]+", " ");
					// If I replace [\\s]{2,} w/ "" it causes errors as well
					// if (startPattern.equals("startTd") ||
					// startPattern.equals("startTh")) {
					// htmlTemp =
					// ContractNLP.htmlPara.matcher(htmlTemp).replaceAll(" ");
					// if <td > <p>hello</p>world</td> it removes the <p
					// (same for <div and <br)
					// }

					sb.append(new String(htmlTemp) + "\r\n");
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

	public String stripHtml(String text) throws IOException, SQLException {

		GetContracts gK = new GetContracts();
//		if(GetContracts.turnOnPrinterWriter) Utils.writeTextToFile(new File("c:/temp/1. before strip.txt"), text);

//		long startTime = System.currentTimeMillis();

		// DO NOT MAKE CHANGES AFTER NOTE IN BODY OF METHOD THAT SAYS ==>>
		// *****NOTE**** KEEP THESE 2 =>text.replaceAll("(?sm)<[^>]*>", ""); -- BUT FIX
		// ERRORS PRIOR TO HERE!!!

		NLP nlp = new NLP();

		text = text.replaceAll("(?ism)<STRIKE>.*?</STRIKE>", "");
		text = text.replaceAll("([\\.\\.]{2})+", " ").replaceAll(" \\.", " ");

//		if(GetContracts.turnOnPrinterWriter) Utils.writeTextToFile(new File("c:/temp/2. strip.txt"), text);

		text = text.replaceAll("(?ism)<PRE>", "");

//		System.out.println("12 repl");
		text = text.replaceAll("</p> <p ", "</p>\r\n<p ");
		text = text.replaceAll("(?i)[ ]{0,}<p [^>]*>", "<p>");

//		pw = new PrintWriter(new File("c:/temp/3a. bef flatten - strip.txt"));
//		pw.append(text);
//		pw.close();

		// gl called multiple times do do not set boolean to true here!
//		System.out.println("at gl.stripHtml. ascii helper used="+asciiHelperUsed);
//		if(!asciiHelperUsed)
		text = HeadingNamesCleanup.flattenToAscii(text);

		if (GetContracts.turnOnPrinterWriter)
			Utils.writeTextToFile(new File("c:/temp/3. strip - flattened.txt"), text);

		text = text.replaceAll("&#145;|&#146;", "'");// has to come after flattener:
														// �|�|

//		System.out.println("13 repl");

//		text = text.replaceAll("&#36", "\\$");

//		System.out.println("14 repl");

		text = text.replaceAll("(?i)(?<=[a-zA-Z\\d]{1})<BR>(?=[a-zA-Z\\d]{1})", " <BR>");
//		System.out.println("15 repl");

		text = text.replaceAll("</h\\d>", "\r\n");
		// can't replace three hard returns with 1, so 2-->
//		System.out.println("16 repl");

		text = text.replaceAll("(?i)<BR> ?\r\n ?<BR>", "\r\n\r\n");
		text = ContractNLP.numberBR.matcher(text).replaceAll("\r\n");
//		 System.out.println("3 text.repl len="+text.length());
		text = text
				// .replaceAll("(?i)(<BR>\r\n|\r\n<BR>)", "\r\n")
				.replaceAll("(?i)<br />", "\r\n");
		text = text.replaceAll("(?i)<BR>", "\r\n ");
//		System.out.println("17 repl");

		text = text.replaceAll("(?ism)<h\\d[^>]*>", "\r\n");
		text = text.replaceAll("(?i)</h\\d>", "\r\n");

//		System.out.println("18 repl");

		// end requires hard return
		// *****NOTE**** KEEP THESE 2 =>text.replaceAll("(?sm)<[^>]*>", ""); --
//			FIX ERRORS PRIOR TO HERE!!!

//		System.out.println("19 repl");

		text = text.replaceAll("(?ism)</div>|</p>", "\r\n");

		text = text.replaceAll("(?sm)<[^>]*>", "");
		text = text.replaceAll("(?sm)</[^>]*>", "\r\n");

		text = text.replaceAll(ContractNLP.TRPattern.toString(), ContractNLP.LineSeparator);

//			System.out.println("2 text.repl len="+text.length());

//		System.out.println("20 repl");
//		if(GetContracts.turnOnPrinterWriter) Utils.writeTextToFile(new File("c:/temp/20. strip.txt"), text);
		text = text.replaceAll("&#9;|&nbsp;|\\xA0|&#160;|&#xA0;|&#168;|&#173;|&#32;|&#8194;|&#8239;|&#8201;|&thinsp;",
				" ");
		text = text.replaceAll("&#151;|&mdash|&#95;|&#9744;", "_");
//		System.out.println("21 text.repl len="+text.length());
		text = text.replaceAll("&#8211;|&#9679;|&#150;|&#8212;|&#8209;|&#111;|&ndash;|&#x2022;|&middot;", "-");
		text = text.replaceAll("&amp;", "&").replaceAll("&#091;", "\\[").replaceAll("&#093;", "\\]");
//		System.out.println("22 text.repl len="+text.length());
		text = text.replaceAll("&#169;", "xA9");

		text = text.replaceAll("—", "-").replaceAll("DESCRIPTION>|&#133;|&#9;|&zwnj;", "");

		text = text.replaceAll("&sect;|&#167;", "�");
//		System.out.println("23 repl");

		text = text.replaceAll("&#184;", ",");
		text = text.replaceAll(ContractNLP.TDWithColspanPattern.toString(), "\t\t");
		text = text.replaceAll(ContractNLP.TDPattern.toString(), "\t");
//			text = text.replaceAll("[/\\[\\]]", "\\|");
//			System.out.println("24 text.repl len="+text.length());

		// "(?i)</?(html|head|meta|title|body|img|hr|div|table|thead|tbody|tr|th|td)[^>]*>",
		// " ")
		// .replaceAll(
		// "(?i)</?i>|</?b>|</?u>|</?font[^>]*>|</?strong>|</?small>",
		// "").replaceAll("(?i)</?(p|br|h\\d|a)[^>]*>", "\r\n");

//			System.out.println("4 text.repl len="+text.length());
		text = text.replaceAll("(?<=([\\p{Alnum};,\":\\-&])) <", "&nbsp;<");
		// need placeholder otherwise all blanks are removed between dummy
		// extraHtmlS eg HELLO<FONT> </FONT>WORLD. [this will hold the space
		// between the carrots
//			text = text.replaceAll("&nbsp;||&#x25CF;", " ");// replace 2 or 3 w/ 1
//		System.out.println("23b text.repl len=" + text.length());
		text = text.replaceAll("(?is)<!--.{0,100}-->", "");
		text = text.replaceAll("[\r\n]{1,}\\.", "\\.\r\n");
		text = text.replaceAll("([\r\n]{1,2})([ ]+)([\r\n]{1,2})", "$1$3");
//		System.out.println("24 repl");

		text = text.replaceAll("&#187;[ \r\n\t]{0,15}\\.", "\\.");
		text = text.replaceAll("([ ]+)(\r\n)", "$2");
		text = text.replaceAll("([a-zA-Z]{1})(\\.)([a-zA-Z]{1})(\\.)([a-zA-Z]{1})(\\.)", "$1xxPD$3xxPD$5xxPD");
		text = text.replaceAll("([a-zA-Z]{1})(\\.)([a-zA-Z]{1})(\\.)", "$1xxPD$3xxPD");
		text = text.replaceAll(" (?=(INC|CO|Inc|Co))\\.(?=[, ]{1})", "xxPD");
		text = text.replaceAll("(?<= (INC|CO|Inc|Co))\\.(?=[, ]{1})", "xxPD");
		text = text.replaceAll("(?ism)(\\.)(pdf|jpeg|jpg|tif|xml|xbrl|html)", "xxPD$2");
		text = text.replaceAll("\r\n ?\r\n[ \t]{0,10}[\\d\\.]{4,20}[ \t]{0,10}\r\n ?\r\n", "");
//		System.out.println("25 repl");

		// replace words that are in quotes that are in body of text (not in
		// definitions section). Such as:... in Section 1381(a)(2)(C) of the
		// Code, (v) an "electing large partnership," as defined

		// first word can be caps but second word must be lower caps. next is what is in
		// quotes.

		text = text.replaceAll("(" +
		// 1st word
				" [\\)\\(\\[\\]A-Za-z,;]{1,11}" +
				// 2nd word
				" [\\)\\(\\[\\]a-z,;]{1,11} )" + "(\")" + "([\\[\\]_-a-zA-Z;, ]{1,50})" + "(\")", "$1''$3''");
		// are rated at least "[__]" by [RA2] and "[__]" fr
		// not followed by two spaces or 1 space and an initial caps.

//		System.out.println("26 text.repl len=" + text.length());
		text = text.replaceAll("(?i)(etc)\\.(?! [A-Z]{1}[a-z]{1,} [a-z]{1}|  [A-Z]| ?[\r\n]| ? \\([a-z]{1}\\))", "$1");
		text = text.replaceAll("(?<=[a-z]{1}) \\.  ?(?=[A-Z]{1}1)", "\\. ");
		text = text.replaceAll("&#043;", "+");
		text = text.replaceAll("&#038;", "&");
		text = text.replaceAll("&#[\\d]{1,5};", "");
		text = text.replaceAll(
				"(\r\n ?)(SECTION |Section )?([\\d]{1,3}\\.[\\d]{1,2})(\r\n ?)([A-Z]{1,14}[a-z]{0,14} )+?",
				"$1$2$3 $5");
		text = text.replaceAll("(\r\n ?)([\\d]{1,3}\\.[\\d]{1,2})(\r\n ?)([A-Z].{5,100}?\r\n\r\n)", "$1$2 $4");
		text = text.replaceAll("[\r\n]{4,}", "\r\n\r\n\r\n");
//		System.out.println("27 repl");
		// System.out.println("text len aft stripped=" + text.length());
		if (text.length() > gK.largeFilePostStrip)
			return "";

		// see GetContracts.pageNumbersLocate(String method) - create a page number
		// finder that uses consecutive methodolgy

		text = text.replaceAll("\n", "\r\n").replaceAll("\r\r\n", "\r\n");
//		text = GetContracts.removePageNumber(text);// page numbers corrupt sentence flow
		// text = listTextPageIdx.get(listTextPageIdx.size()-1)[0];

		// this replace fixes section ends occurring in the middle of a sentence
		text = text.replaceAll(
				"((?ism)(?<=(corp|inc|p\\.?l\\.?c\\.|" + "s\\.?p\\.?a\\.|l\\.?l\\.?c\\.|l\\.?l\\.?p\\.))\\.(?= \\())",
				"");

		text = text.replaceAll("(?sm)(?<= pursuant to( this)?|subject to( this)?|including( this)?"
				+ "|set ?forth (under|in)|contemplated by|under( this)?|accordance with|of) ?[\r\n]{1,5} ?(?=[A-Z]{1})",
				" ");// put in stripHtml
		// this replaces ensure [Reserved] is attached to section. (don't worry
		// elsewhere).
//		System.out.println("28 repl");

		text = text.replaceAll(
				"(?sm)(?<=(S(ECTION|ection) [\\d\\.]{1,6}))[\r\n]{1,8}(?=\\[?[RD]{1}(ELETED?|eleted?|ESERVED?|eserved?)\\.?\\]?\\.?\r\n)",
				" ");// put in stripHtml

		// gets rid of hanging section number - only use for all caps.
		text = text.replaceAll("(?sm)(?<=[\r\n ]{1}(SECTION ?))\r\n ?(?=[\\d\\.]{1,6} [A-Z]{1})", " ");// put in

		// stripHtml

		// makes sure [Reserved] section on its own line
		text = text.replaceAll(
				"(?sm)\\.  ?(?=SECTION [\\d\\.]{1,7} \\[?[RD]{1}(ELETED?|eleted?|ESERVED?|eserved?)\\]?)",
				"\\.\r\n\r\n");
		// puts a hard return before a section heading
		text = text.replaceAll("(?sm)(?<=[a-z]{2}\\.)  ?(?=(SECTION [\\d]{1,2}"
				+ "\\.[\\d]{1,2}\\.? [A-Z]{1}[a-z]{1,20} " + "([A-Z]{1}[a-z]{1,20}|[a-z]{1,3})" + "))", "\r\n\r\n");

		text = text.replaceAll("and/or", "and or");
		// String tocText = GetContracts.getContractToc(text);
		// tocText.length();
		// System.out.println("4 text.len=" + text.length());
//		System.out.println("29 repl");

		text = text.replaceAll("([\r\n]{1,2})([ ]+)([\r\n]{1,2})", "$1$3").replaceAll("[\r\n]{9,}", "\r\n\r\n\r\n\r\n");

		text = text.replaceAll("(?<=[a-z]{1}) \\.  ?(?=[A-Z]{1}1)", "\\. ");
		text = text.replaceAll("[ ]+", " ");
		text = text.replaceAll("(?sm)[\r\n]+ ?\\. ", "\\.\r\n\r\n");
		text = text.replaceAll("(?sm)[\t ]{1,4}\\.(?= ?[A-Z]{1}[A-Za-z]{1})", "\\. ");

		text = text.replaceAll("zc.{1,9}zc", "");
		text = text.replaceAll("([\\dA-Za-z]{1,2})(\\.)([a-zA-Z\\d]{1,2})(\\.)( [a-z]+)", "$1xxPD$3xxPD$5");//
//		System.out.println("30 repl");
		text = text.replaceAll("(?<=[A-WYZa-wyz]{1})(\\\")(?=[A-WYZa-wyz])", "'");
		text = text.replaceAll(
				"(?ism)(?<=\"([a-z\\d,\\-]{1,25} ?)([a-z\\d,\\-]{1,25} ?)?([a-z\\d,\\-]{1,25} ?)?([a-z\\d,\\-]{1,25} ?)?)(\')(?=[\\), \\.;])",
				"\"");// 1
		text = text.replaceAll(
				"(?ism)(?<=[\r\n ])(\')(?=([a-z\\d,\\-]{1,25} ?)([a-z\\d,\\-]{1,25} ?)?([a-z\\d,\\-]{1,25} ?)?([a-z\\d,\\-]{1,25} ?)?\")",
				"\"");// 2
		// 1 and 2 above repl "hello World' and 'hello World" each with "hello World"
		text = text.replaceAll("&lt;", "[").replaceAll("&gt;", "]").replaceAll("XQ \\. ", "XQ\\. ").replaceAll("Qx\"XQ",
				"\"");
		text = text.replaceAll("([\r\n] ? ?)(Qx.{0,18}?)(XQ)( ? ?\r\n ? ?)(Qx)(.{1,135}?XQ)", "$1$2 $6");// replaces:
		if (GetContracts.turnOnPrinterWriter)
			Utils.writeTextToFile(new File("c:/temp/28 strip.txt"), text);
		text = text.replaceAll("(\") (Qx)(.{3,115})(XQ) (\")", "$1$2$3$4$5");
		text = text.replaceAll(" ?\r\n ", "\r\n");

		text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", " ").replaceAll("[ ]+", " ");
//		System.out.println("31 finished repl");
		text = text.replaceAll(
				"(?sm)(?<=\r\n(\\(?[A-Za-z\\d\\.]\\)|\\(?[A-Za-z\\d\\.]+)|([\\d]{1,3}\\.?)([\\d]{1,3}\\.?)?([\\d]{1,3})?)(\r\n)(?=[A-Z]{1}[a-zA-Z]{1,15}(\r\n| .{1,135}?(\r\n|[\\.:])))",
				" ");

		text = text.replaceAll("(?<=\r\n[\\d\\.]{1,10})(?=[A-Z]{1})|(?=Qx[\\d\\.]+)XQ\r\n\\r\nQx", " ");
//(?<=[\d\.]+)XQ\r\n\r\nQx

		if (GetContracts.turnOnPrinterWriter)
			Utils.writeTextToFile(new File("c:/temp/29 strip.txt"), text);

		text = text.replaceAll("" + "(\r\nQx)" + "([\\d]{1,4}[\\d\\.]{1,5}+XQ)([\r\n]{2,6})(Qx)", "$1$2 $4");

		text = text.replaceAll("(\r\n" + "[\\d]{1,4}[\\d\\.]{1,5})([\r\n]{2,6})" + "(Qx.{1,85}XQ)(\r\n)" + "",
				"$1 $3$4");

		if (GetContracts.turnOnPrinterWriter)
			Utils.writeTextToFile(new File("c:/temp/30 strip.txt"), text);
		text = text.replaceAll("(\\d[\\d\\.]{1,5})(\r\n ? ?)(Qx)", "$1 $3");
		if (GetContracts.turnOnPrinterWriter)
			Utils.writeTextToFile(new File("c:/temp/31 strip.txt"), text);
		text = text.replaceAll("(Qx)(\")(.{1,85})(\")(XQ)", "$2$1$3$5$4");
		if (GetContracts.turnOnPrinterWriter)
			Utils.writeTextToFile(new File("c:/temp/32 strip.txt"), text);

		text = text.replaceAll("(XQ)(\\.)( )?(Qx)", "$2$3");
		text = text.replaceAll("(Qx)( ?\\.?)(XQ)", "$2");
		text = text.replaceAll("(XQ)( ?\\.?)(Qx)", "$2");
		text = text.replaceAll("(Qx)(SECTION )?([\\d]+\\.?)(XQ[\r\n]{2,8}Qx)", "$1$2$3 ");
		if (GetContracts.turnOnPrinterWriter)
			Utils.writeTextToFile(new File("c:/temp/strip finished.txt"), text);
		return text;

	}

	public TreeMap<Double, String[]> getContractToc(String text) throws IOException {

		/*
		 * NOTE Do NOT touch text - length must not chg! No search and replace - length
		 * cannot chg! Or everything gets corrupted.
		 */

//		StringBuffer sb = new StringBuffer();
		NLP nlp = new NLP();

		int dist = 400;

		// cks captured number that occurs at the end of a line. And if there are enough
		// consecutively numbered and they are w/n X number of hard return lines - then
		// it is a TOC.

		TreeMap<Double, String[]> mapIdx = new TreeMap<Double, String[]>();

		List<String[]> listP = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(text,
				GetContracts.patternTocPageNumberAll);
//		NLP.printListOfStringArray("listP=", listP);

//		NLP.printListOfStringArray("listPgNo=", listPgNo);

		int pgNmb = 0, pgNmbPrev = 0, eIdxPrev = 0, eIdx = 0, sIdx = 0, sIdxPrev = -10000, sIdxNext = -999,
				eIdxNext = -9199, pgNmbNext = -999;
		String confirmed = "Y", pgNmbStr = "", pStr = "", tocHdg = "", exhPrefix = "", toc = "toc=";
		boolean definitionsPgNmb = false;

		Pattern patternPgnmb = Pattern.compile("(?sm)(?<=[A-Za-z]{1} ?- ?)?([xvi]{1,3}|[\\d]+)(?= ?($|\r\n))"
//				+ "( ?[\r\n])"
				+ "");
		Pattern patternPgnmbNotHidden = Pattern.compile("([A-Za-z]{1} ?- ?)?([xvi]{1,3}|[\\d]+)( ?($|\r\n))");
		Pattern patternPgnmbRomanShort = Pattern.compile("(?<= )([xvi]{1,3})( ?($|[\r\n]))");

		for (int i = 0; i < listP.size(); i++) {
			toc = "toc=";
			pgNmbPrev = -100;
			pgNmbNext = -999;
			confirmed = "N";
			tocHdg = "";
			pgNmb = -1;
			pStr = listP.get(i)[0];
//			System.out.println("pStr=" + pStr);
			if (nlp.getAllMatchedGroups(pStr, patternPgnmb).size() > 0 && nlp.getAllMatchedGroups(pStr, patternPgnmb)
					.get(0).replaceAll("[\\d]+", "").trim().length() == 0) {
//				System.out.println("pgNo size=" + nlp.getAllMatchedGroups(pStr, patternPgnmb).size());
//				System.out.println("pgNo=" + nlp.getAllMatchedGroups(pStr, patternPgnmb).get(0));
				pgNmbStr = nlp.getAllMatchedGroups(pStr, patternPgnmb).get(0);
				pgNmb = Integer.parseInt(pgNmbStr);
			} else if (nlp.getAllMatchedGroups(pStr, patternPgnmb).size() > 0 && nlp
					.getAllMatchedGroups(pStr, patternPgnmb).get(0).replaceAll("[\\d]+", "").trim().length() > 0) {
				pgNmb = convertAlphaNumericPgNo(nlp.getAllMatchedGroups(pStr, patternPgnmb).get(0));
			}

			if (i + 1 < listP.size() && nlp.getAllMatchedGroups(listP.get(i + 1)[0], patternPgnmb).size() > 0
					&& nlp.getAllMatchedGroups(listP.get(i + 1)[0], patternPgnmb).get(0).replaceAll("[\\d]+", "").trim()
							.length() == 0) {
				pgNmbNext = Integer.parseInt(nlp.getAllMatchedGroups(listP.get(i + 1)[0], patternPgnmb).get(0));
				sIdxNext = Integer.parseInt(listP.get(i + 1)[1]);
				eIdxNext = Integer.parseInt(listP.get(i + 1)[2]);

			}

			else if (i + 1 < listP.size() && nlp.getAllMatchedGroups(listP.get(i + 1)[0], patternPgnmb).size() > 0
					&& nlp.getAllMatchedGroups(listP.get(i + 1)[0], patternPgnmb).get(0).replaceAll("[\\d]+", "").trim()
							.length() > 0
					&& nlp.getAllMatchedGroups(listP.get(i + 1)[0], patternPgnmb).size() > 0) {
				pgNmbNext = convertAlphaNumericPgNo(nlp.getAllMatchedGroups(listP.get(i + 1)[0], patternPgnmb).get(0));
				sIdxNext = Integer.parseInt(listP.get(i + 1)[1]);
				eIdxNext = Integer.parseInt(listP.get(i + 1)[2]);
			}

			if (i > 0 && nlp.getAllMatchedGroups(listP.get(i - 1)[0], patternPgnmb).size() > 0
					&& nlp.getAllMatchedGroups(listP.get(i - 1)[0], patternPgnmb).get(0).replaceAll("[\\d]+", "").trim()
							.length() == 0) {
				pgNmbPrev = Integer.parseInt(nlp.getAllMatchedGroups(listP.get(i - 1)[0], patternPgnmb).get(0));
				sIdxPrev = Integer.parseInt(listP.get(i - 1)[1]);
				eIdxPrev = Integer.parseInt(listP.get(i - 1)[2]);

			} else if (i + 1 < listP.size() && nlp.getAllMatchedGroups(listP.get(i + 1)[0], patternPgnmb).size() > 0
					&& nlp.getAllMatchedGroups(listP.get(i + 1)[0], patternPgnmb).get(0).replaceAll("[\\d]+", "").trim()
							.length() > 0
					&& nlp.getAllMatchedGroups(listP.get(i + 1)[0], patternPgnmb).size() > 0) {
				pgNmbNext = convertAlphaNumericPgNo(nlp.getAllMatchedGroups(listP.get(i + 1)[0], patternPgnmb).get(0));
				sIdxNext = Integer.parseInt(listP.get(i + 1)[1]);
				eIdxNext = Integer.parseInt(listP.get(i + 1)[2]);
			}

			if (pgNmb == -1) {
//				System.out.println("continue=-1");
				continue;
			}

			sIdx = Integer.parseInt(listP.get(i)[1]);

//			System.out.println("listP.get(i)[0]=" + listP.get(i)[0]);
//			System.out.println("listP.get(i + 1)[0]=" + listP.get(i + 1)[0]);

			if (sIdx - eIdxPrev > dist)
				continue;

			if (nlp.getAllMatchedGroups(pStr,
					Pattern.compile("([12][09][\\d]{2}-?\\d?\\d?" + "|(Suite|SUITE) [\\d\\.]+" + "|[A-Za-z]{1}\\d"
							+ "|/\\d\\d?" + "|[\\d]{4,})" + " ?[\r\n]+|dated:"))
					.size() > 0
					|| nlp.getAllMatchedGroups(pStr, Pattern.compile("\\$[\\d]{1,3}|[\\d]{1,3},[\\d]")).size() > 0) {
//				System.out.println("1 continue listP=" + Arrays.toString(listP.get(i)));
//				 add exceptions to pattern above '|'. current exception is eg 2010-1 or 2010
				continue;
			}

//			System.out.println("is this a pg #? pStr.snip=" + pStr.substring(0, Math.min(pStr.length(), 25)));

//			if (nlp.getAllMatchedGroups(pStr, patternPgnmb).size() == 0) {
//				System.out.println("2 continue pStr=" + pStr);
//				continue;
//			}

//			System.out.println("listP.get(i + 1)[0].len=="+listP.get(i + 1)[0].length());
			if ((i + 1 < listP.size() && i > 0 && pgNmbNext == -999 && pgNmbPrev == -100)) {
//				System.out.println("3 continue pStr=" + pStr);
				continue;
			}

			// pgNmb!=0, w/n <dist, pgNmb can't be ###,###
			if (i > 0 && (
//							(sIdx>0 && sIdxPrev>0 && sIdx - sIdxPrev > dist) || 
			pgNmb == 0 || nlp.getAllMatchedGroups(pStr, Pattern.compile("([\\d]{1}," + pgNmb + ")")).size() > 0

			)) {
//				pgNmbPrev = pgNmb;
//				sIdxPrev = sIdx;
//				System.out.println("4xcontinuing - pgNmb=" + pgNmb + " pStr=" + pStr);
				continue;
			}

//			System.out.println("A. pgNmb=" + pgNmb + " pgNmbNext=" + pgNmbNext + " pgNmbPrev=" + pgNmbPrev + " def?="
//					+ definitionsPgNmb);

			definitionsPgNmb = GetContracts.isDefPgNo(listP.get(i)[0]);

			if ((Math.abs(pgNmb - pgNmbPrev) < 10 && sIdx - sIdxPrev < dist * .5 && sIdx > sIdxPrev
					&& pgNmb >= pgNmbPrev)
					|| (Math.abs(pgNmb - pgNmbPrev) < 4
							&& sIdx - sIdxPrev < dist && sIdx > sIdxPrev && pgNmb >= pgNmbPrev)
					|| (pgNmb >= pgNmbPrev && sIdx - sIdxPrev < dist && definitionsPgNmb)
					|| (Math.abs(pgNmbNext - pgNmb) < 10 && sIdxNext - sIdx < dist * .5 && sIdxNext > sIdx
							&& pgNmbNext >= pgNmb)
					|| (pgNmbNext >= pgNmb && sIdxNext - sIdx < dist && definitionsPgNmb)) {

				confirmed = "Y";
//				System.out.println("listP.get=" + pStr + " pgNmb=" + pgNmb + " pgNmbPrev=" + pgNmbPrev + " def?="
//						+ definitionsPgNmb);

				if (nlp.getAllMatchedGroups(pStr, patternPgnmbNotHidden).size() > 0) {

					pgNmbStr = nlp.getAllMatchedGroups(pStr, patternPgnmb).get(0);

//					pgNmbStr = nlp.getAllMatchedGroups(pStr, patternPgnmbNotHidden).get(0).trim();
//					System.out.println("3a pgNmbStr=" + pgNmbStr + " pgNmb=" + pgNmb);
//					System.out.println(
//							"3b pgNmbStr=" + nlp.getAllMatchedGroups(pStr, patternPgnmb).get(0) + " pgNmb=" + pgNmb);
				}

				pStr = pStr.replaceAll("\r\n", " ").trim();// this has to come after pgNmbStr retrieval from pStr

//				System.out.println("pattern=" + Arrays.toString(listP.get(i)).trim() + " pgNo=" + pgNmb + " pgNoPrev="
//						+ pgNmbPrev + " pgNmbNext=" + pgNmbNext + " sIdx=" + sIdx + " sIdxPrev=" + sIdxPrev
//						+ " sIdxNext=" + sIdxNext + " pgNmbStr=" + pgNmbStr);

				tocHdg = pStr.trim();
//				System.out.println("tocHdg=" + tocHdg);
				exhPrefix = "";
				if (nlp.getAllMatchedGroups(tocHdg, patternExhPrefFromTocHdg).size() > 0) {

					exhPrefix = nlp.getAllMatchedGroups(tocHdg, patternExhPrefFromTocHdg).get(0).trim();
//					System.out.println("exhPrefix..." + exhPrefix);
//					tocHdg = tocHdg.replaceAll(exhPrefix, "").trim();
//					System.out.println("1 tocHdg=" + tocHdg);

					tocHdg = tocHdg.replaceAll("(?sm)(" + exhPrefix.trim() + ")", "").trim();
//					System.out.println("tocHdg repl exhPre=" + tocHdg + " exhPref=" + exhPrefix);

					if (nlp.getAllMatchedGroups(tocHdg,
							Pattern.compile("((^| )[A-Z]{1,2}-?\\d|[i]{1,3}-?\\d|[a-c]{1,2}]{1}-?\\d?)")).size() > 0) {
						pgNmbStr = nlp
								.getAllMatchedGroups(tocHdg,
										Pattern.compile("((^| )[A-Z]{1,2}-?\\d|[i]{1,3}-?\\d|[a-c]{1,2}]{1}-?\\d?)"))
								.get(0).trim();

//						System.out.println("4a pgNmbStr=" + pgNmbStr + " pgNmb=" + pgNmb);
//						System.out.println("4b pgNmbStr=" + nlp.getAllMatchedGroups(pStr, patternPgnmb).get(0)
//								+ " pgNmb=" + pgNmb);

//						System.out.println("pgNmbStr tocExhHdg=" + pgNmbStr);//
						tocHdg = tocHdg.trim().replaceAll("(" + pgNmbStr + ")", "").trim();
					}
					exhPrefix = "exhPre=" + exhPrefix;
				}

				if (tocHdg.length() == 0) {
					tocHdg = pStr.trim().replaceAll("(" + pgNmbStr + ")", "").trim();
//					System.out.println("2 tocHdg=" + tocHdg);
				}

				if (exhPrefix.length() > 0) {
					toc = "tocExh=";
				}

//				System.out.println("tocHdg="+tocHdg);
				if (tocHdg.replaceAll(patternPgnmbNotHidden.toString(), "").length() == 0) {
					tocHdg = tocHdgPriorLine(text.substring(Math.max(Integer.parseInt(listP.get(i)[1]) - 150, 0),
							Integer.parseInt(listP.get(i)[1])));

				}

				pgNmbStr = nlp.getAllMatchedGroups(pStr, patternPgnmbNotHidden).get(0);

				String[] ary = { listP.get(i)[1].trim(), listP.get(i)[2],
						toc + tocHdg.replaceAll("(?sm)" + pgNmbStr.trim() + "$", "").trim(), "confirmed=" + confirmed,
						"pgNo=" + pgNmb, "pgNmbStr=" + pgNmbStr, exhPrefix };
//				System.out.println(".add ary=" + Arrays.toString(ary));
				mapIdx.put(Double.parseDouble(listP.get(i)[1]), ary);

				pgNmbPrev = pgNmb;
				sIdxPrev = sIdx;

			}
		}

		return mapIdx;

	}

	public String tocHdgPriorLine(String text) {

		String hdg = "", line = "";
		String[] ary = text.split("\r\n");
		// finds the hdg prior to the pg no that is on its own line.
		// it will look up to 2 hard returns prior

//		System.out.println("tocHdgPriorLine=" + text+"||end");
		int cnt = 0;
		for (int i = ary.length - 1; i >= 0; i--) {
			cnt++;
			line = ary[i];
			if (line.replaceAll("[\r\n ]+", "").length() == 0) {
				continue;
			}
			if (line.replaceAll("[\r\n ]+", "").length() > 0) {
//				System.out.println("cnt=" + cnt + "line>0=" + line);
				hdg = line.replaceAll("[\r\n]+", "").trim();
				break;
			}
			if (cnt > 2)
				break;
		}

		return hdg;
	}

	public TreeMap<Double, String[]> getExhibitToc(String text, TreeMap<Double, String[]> mapIdx) {

		/*
		 * NOTE Do NOT touch text - length must not chg! No search and replace - length
		 * cannot chg! Or everything gets corrupted.
		 */

		NLP nlp = new NLP();
		Pattern patternExhibitToc = Pattern
				.compile("[\r\n]{1}[ \t]{0,4}(Exhibit|EXHIBIT|APPENDIX|Appendix|SCHEDULE|Schedule|Annex|ANNEX)"
						+ "([ ]{1,2}[A-Z\\-\\d\\.\\(\\)a-z]{1,9})(\r\n)?[ \t]{1,20}"
						+ "[\\p{Alnum}\\p{Punct}]{1,90}.{1,90}?[\r\n]{1}");

		List<String[]> listExhToc = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(text, patternExhibitToc);

		double sIdx = 0, eIdx = 0, sIdxNext = 10000, eIdxNext = 10000, sIdxPrev = -10000, eIdxPrev = -10000,
				cntg = -0.0000002;

		for (int i = 0; i < listExhToc.size(); i++) {
			if (i > 0) {
				sIdxPrev = Double.parseDouble(listExhToc.get(i - 1)[1]);
				eIdxPrev = Double.parseDouble(listExhToc.get(i - 1)[2]);
			}

			if (i + 1 < listExhToc.size()) {
				sIdxNext = Double.parseDouble(listExhToc.get(i + 1)[1]);
				eIdxNext = Double.parseDouble(listExhToc.get(i + 1)[2]);
			}

			sIdx = Double.parseDouble(listExhToc.get(i)[1]);
			eIdx = Double.parseDouble(listExhToc.get(i)[2]);
			if (Math.abs(sIdx - eIdxPrev) < 200 || Math.abs(eIdx - sIdxNext) < 200) {
				String[] ary = { listExhToc.get(i)[1], listExhToc.get(i)[2] + "",
						"exhToc=" + listExhToc.get(i)[0].replaceAll("[\r\n]+", " ").replaceAll("[ ]+", " ") };
				cntg = cntg + -0.0000002;
				mapIdx.put(sIdx + cntg, ary);
//				System.out.println("adding - exhToc==" + Arrays.toString(ary));
			}
		}

		return mapIdx;

	}

	public TreeMap<Double, String[]> getDefinitionsMapped(String text, TreeMap<Double, String[]> mapIdx)
			throws IOException {

//		System.out.println("**getDefinitions");
		/*
		 * NOTE Do NOT touch text - length must not chg! No search and replace - length
		 * cannot chg! Or everything gets corrupted.
		 */

		// System.out.println("def text.len="+text.length());

		List<String[]> listSidxEidxTypeDefHdgsTmp = SolrPrep.getDefinitions_for_parsing_jsons(text);

//		NLP.printListOfStringArray("1.getDefinitions==", listSidxEidxTypeDefHdgsTmp);

		SolrPrep.listSidxEidxTypeDefHdgs = new ArrayList<String[]>();
		SolrPrep.listSidxEidxTypeDefHdgs = SolrPrep.validateDefinitions(listSidxEidxTypeDefHdgsTmp, text);

//		NLP.printListOfStringArray("2.validated. getDefinitions==", SolrPrep.listSidxEidxTypeDefHdgs);

		TreeMap<Integer, String[]> mapDefFinal = new TreeMap<Integer, String[]>();
		for (int i = 0; i < SolrPrep.listSidxEidxTypeDefHdgs.size(); i++) {
			mapDefFinal.put(Integer.parseInt(SolrPrep.listSidxEidxTypeDefHdgs.get(i)[0]),
					SolrPrep.listSidxEidxTypeDefHdgs.get(i));
		}

//		NLP.printMapIntStringAry("mapDefAdded====", SolrPrep.mapDefAdded);
		String defadd = "";
		int eIdx;
		for (Map.Entry<Integer, String[]> entry : SolrPrep.mapDefAdded.entrySet()) {
			if (!mapDefFinal.containsKey(entry.getKey())) {
//				System.out.println("mapDefAdded array==" + Arrays.toString(entry.getValue()));
				String[] ary = { entry.getValue()[0] + "", entry.getValue()[1] + "" };
				defadd = text.substring(Integer.parseInt(entry.getValue()[0]),
						GetContracts.getLastDefinedTermSimple(ary, text)); // for testing viewing.
//				System.out.println("defadd......." + defadd);
				eIdx = GetContracts.getLastDefinedTermSimple(ary, text);
				String[] ar = { entry.getKey() + "", eIdx + "", "def", entry.getValue()[3] };
				mapDefFinal.put(entry.getKey(), ar);

			}
		}
		SolrPrep.mapDefAdded = new TreeMap<Integer, String[]>();

//		NLP.printMapIntStringAry("mapDefFinal", mapDefFinal);

//		NLP.printListOfStringArray("listFinalllllll=", listFinal);
		SolrPrep.listSidxEidxTypeDefHdgs = new ArrayList<String[]>();
		for (Map.Entry<Integer, String[]> entry : mapDefFinal.entrySet()) {
			SolrPrep.listSidxEidxTypeDefHdgs.add(entry.getValue());
		}

//		NLP.printListOfStringArray("SolrPrep.listSidxEidxTypeDefHdgs==", SolrPrep.listSidxEidxTypeDefHdgs);

		String definitionName = "";
		int sIdx = 0;
		eIdx = 0;

		if (SolrPrep.listSidxEidxTypeDefHdgs.size() == 0)
			return mapIdx;

		String txt = "";
		double cntg = -0.000002;
		int sIdxNext = 0;
		int eIdx_stub = 0;
		// if eIdx>sIdxNext then get idx of last def and if it is less than sIdxNext
		// keep else discard.
		for (int n = 0; n < SolrPrep.listSidxEidxTypeDefHdgs.size(); n++) {
//			System.out.println("n=" + n + " listSidxEidxTypeDefHdgs.size=" + SolrPrep.listSidxEidxTypeDefHdgs.size()
//					+ " ary=" + Arrays.toString(SolrPrep.listSidxEidxTypeDefHdgs.get(n)));

			sIdx = Integer.parseInt(SolrPrep.listSidxEidxTypeDefHdgs.get(n)[0]);
			eIdx = Integer.parseInt(SolrPrep.listSidxEidxTypeDefHdgs.get(n)[1]);
			if (n + 1 < SolrPrep.listSidxEidxTypeDefHdgs.size()) {
				sIdxNext = Integer.parseInt(SolrPrep.listSidxEidxTypeDefHdgs.get(n + 1)[0]);
//				System.out.println("1.sIdxNext==" + sIdxNext + " eIdx=" + eIdx);
			}
			if (n + 1 == SolrPrep.listSidxEidxTypeDefHdgs.size()) {
				sIdxNext = text.length();
//				System.out.println("2.sIdxNext==" + sIdxNext + " eIdx=" + eIdx);
			}
			if (sIdxNext < eIdx) {
				String[] ay = { sIdx + "", sIdxNext + "" };
				eIdx_stub = GetContracts.getLastDefinedTermSimple(ay, text);
//				System.out.println("eIdx_stub=" + eIdx_stub);
				if (eIdx_stub < sIdxNext) {
					eIdx = eIdx_stub;
				}
			}
			definitionName = SolrPrep.listSidxEidxTypeDefHdgs.get(n)[3];
//			System.out.println("22 sIdx=" + sIdx + " eIdx=" + eIdx);
			String[] ary = { sIdx + "", eIdx + "", "def="
					+ definitionName.replaceAll("^(Qx)? ?(SECTION [\\d\\.]{1,4}|Section [\\d\\.]{1,4})", "").trim() };
			cntg = cntg + -0.0000002;
//			System.out.println(".put==" + Arrays.toString(ary));
			mapIdx.put((double) sIdx + cntg, ary);
		}

//		NLP.printMapDblStrAry("def mapIdx...", mapIdx);

//		TreeMap<Double, String[]> mapIdx2 = new TreeMap<Double, String[]>();
//		// clean up - where def and sec have same sIdx -- remove sec.
//		int sidx = 0;
//		int sIdxPr = 10;
//		double key = 0.0, keyPr = 0.0;
//		for (Map.Entry<Double, String[]> entry : mapIdx.entrySet()) {
//			// find where sec and def conflict and remove sec hdg.
//			sIdx = Integer.parseInt(entry.getValue()[0]);
//			key = entry.getKey();
//			String[] ar = entry.getValue();
//			if (sIdx == sIdxPr && ar[2].contains("sec=")) {
//				mapIdx2.put(key, ar);
//			}
//
//			sIdxPr = sIdx;
//			keyPr = key;
//		}
//
//		for (Map.Entry<Double, String[]> entry : mapIdx2.entrySet()) {
//			mapIdx.remove(entry.getKey());
//		}
//
//		NLP.printMapDblStrAry("cleaned sec conflict -- mapIdx2...", mapIdx2);

		return mapIdx;

	}

	public static String resetHeadingName(String sectionName) throws IOException {

		String lowerCaseWord, upperCaseWord;
		NLP nlp = new NLP();

//		System.out.println("1a.  BAD sectionName is=" + sectionName);
		List<String[]> listLowerCase = nlp.getAllEndIdxAndMatchedGroupLocs(sectionName,
				Pattern.compile("(?<=^|[ \\(\\[])([a-z]+[ ,;:\\)\\]]{1})"));
//		TreeMap<String, Integer> mapLowerCase = new TreeMap<String, Integer>();
		for (int i = 0; i < listLowerCase.size(); i++) {
//			mapLowerCase.put(listLowerCase.get(i)[1], Integer.parseInt(listLowerCase.get(i)[0]));
			lowerCaseWord = listLowerCase.get(i)[1];
//			System.out.println("lower case words are=" + lowerCaseWord);
			if (lowerCaseWord.toLowerCase().equals("etc")) {
//				System.out.println("lower case word is etc.===" + lowerCaseWord);
				sectionName = sectionName.substring(0, Integer.parseInt(listLowerCase.get(i)[0]));
//				System.out.println("sectionName FIXED=" + sectionName);
				break;
				// cut at idx of etc plus etc len
			}
		}
		List<String[]> listUpperCase = nlp.getAllEndIdxAndMatchedGroupLocs(sectionName,
				Pattern.compile("(?<=^|[ \\(\\[])([A-Z]+[a-z]+[ ,;:\\)\\]]{1})"));

		for (int i = 1; i < listUpperCase.size(); i++) {
			// skip first word. which is always initial caps
			upperCaseWord = listUpperCase.get(i)[1];
			if (upperCaseWord.trim().toLowerCase().equals("etc")) {
//				System.out.println("uppercase word is etc.===" + upperCaseWord);
				// cut at idx of etc plus etc len
				sectionName = sectionName.substring(0, Integer.parseInt(listUpperCase.get(i)[0]));
//				System.out.println("sectionName FIXED=" + sectionName);
				break;
				// cut at idx of etc plus etc len

			}
//			System.out.println("upper case words are=" + upperCaseWord);
			// ck if word is stop word - if so then cut section name here.
		}

		return sectionName;
	}

	public TreeMap<Double, String[]> getSectionsMapped(String text, TreeMap<Double, String[]> mapIdx)
			throws IOException {

		/*
		 * NOTE Do NOT touch text - length must not chg! No search and replace - length
		 * cannot chg! Or everything gets corrupted.
		 */

		double cntg = 0.00001;
		NLP nlp = new NLP();

//		long startTime = System.currentTimeMillis();
//		System.out.println("list sections restrictive");
		SolrPrep.listPrelimIdxAndSectionRestrictive = ContractNLP.getAllIdxLocsAndMatchedGroups(text,
				ContractParser.patternSectionHeadingRestrictive);

		List<String[]> listPrelimIdxAndSectionLessRestrictiveWithNumberSectionAlso = ContractNLP
				.getAllIdxLocsAndMatchedGroups(text,
						ContractParser.patternSectionHeadingLessRestrictiveWithNumberSectionAlso);
//		startTime = System.currentTimeMillis();

//		NLP.printListOfStringArray("1b listPrelimIdxAndSectionLessRestrictiveWithNumberSectionAlso",
//				listPrelimIdxAndSectionLessRestrictiveWithNumberSectionAlso);

//		startTime = System.currentTimeMillis();
		List<String[]> patternSectionHeadingLessRestrictiveWithoutNumber = ContractNLP
				.getAllIdxLocsAndMatchedGroups(text, ContractParser.patternSectionHeadingLessRestrictiveWithoutNumber);
		// System.out.println("3 textSidxEidx.len="+textSidxEidx.length());

//		NLP.printListOfStringArray("1c patternSectionHeadingLessRestrictiveWithoutNumber==",
//				patternSectionHeadingLessRestrictiveWithoutNumber);

		if (listPrelimIdxAndSectionLessRestrictiveWithNumberSectionAlso.size()
				* 1.3 < patternSectionHeadingLessRestrictiveWithoutNumber.size()) {

			// if many section headings with 'Section' are found than there
			// shouldn't be a section numbering format w/o 'Section' in the
			// format of the heading.

			SolrPrep.listPrelimIdxAndSectionLessRestrictive = patternSectionHeadingLessRestrictiveWithoutNumber;

		} else {
			SolrPrep.listPrelimIdxAndSectionLessRestrictive = listPrelimIdxAndSectionLessRestrictiveWithNumberSectionAlso;
		}
//		NLP.printListOfStringArray("1t SolrPrep.listPrelimIdxAndSectionLessRestrictive==",
//				SolrPrep.listPrelimIdxAndSectionLessRestrictive);
		// System.out.println("4 textSidxEidx.len="+textSidxEidx.length());

//		startTime = System.currentTimeMillis();
		List<String[]> listSidxEidxTypeSecHdgTmp = SolrPrep.getSectionIdxsAndNames(text);
//		NLP.printListOfStringArray("123 getSectionIdxsAndNamesTmp=", listSidxEidxTypeSecHdgTmp);//okay

		// getDefidxAndDefinition MUST come after getSectionIdxsAndNames!!
		// b/c it uses its idx loc of sec hdgs.

		List<String[]> listSidxEidxTypeSecHdgs = new ArrayList<>();
		String secHdg = "", secHdgNext = "", secHdgPrior = "";
		// , str = "";
		for (int i = 0; i < listSidxEidxTypeSecHdgTmp.size(); i++) {
			secHdg = listSidxEidxTypeSecHdgTmp.get(i)[3];
			// System.out.println("Z. secHdg="+secHdg);
			if (i + 1 < listSidxEidxTypeSecHdgTmp.size()) {
				secHdgNext = listSidxEidxTypeSecHdgTmp.get(i + 1)[3];
			}

			if (i + 1 == listSidxEidxTypeSecHdgTmp.size() && listSidxEidxTypeSecHdgTmp.size() > 1) {
				secHdgPrior = listSidxEidxTypeSecHdgTmp.get(i - 1)[3];
			}

			if ((secHdgNext.equals(secHdg) && i + 1 < listSidxEidxTypeSecHdgTmp.size())
					|| (i + 1 == listSidxEidxTypeSecHdgTmp.size() && secHdgPrior.equals(secHdg))) {
				continue;
			}

//			System.out.println("listSidxEidxTypeSecHdgTmp - .add=" + Arrays.toString(listSidxEidxTypeSecHdgTmp.get(i)));
			listSidxEidxTypeSecHdgs.add(listSidxEidxTypeSecHdgTmp.get(i));

		}
//		System.out.println("SolrPrep.listPrelimIdxAndSectionLessRestrictive.size ="
//				+ SolrPrep.listPrelimIdxAndSectionLessRestrictive.size());
		if (null == SolrPrep.listPrelimIdxAndSectionLessRestrictive
				|| SolrPrep.listPrelimIdxAndSectionLessRestrictive.size() == 0) {
			return mapIdx;
		}

		// System.out.println("sec list.size="+listSidxEidxTypeSecHdgs2.size());
		String sectionName = "";
		int sIdx = 0, eIdx = 0, eIdxNext = 0;
		sIdx = 0;
		eIdx = 0;

		String lowerCaseWord, upperCaseWord;
		int sectionName_is_likely_wrong_ratio = 0;
		for (int n = 0; n < listSidxEidxTypeSecHdgs.size(); n++) {
			sectionName_is_likely_wrong_ratio = 0;
//			System.out.println("a. listSidxEidxTypeSecHdgs=" + Arrays.toString(listSidxEidxTypeSecHdgs.get(n)));
			sIdx = Integer.parseInt(listSidxEidxTypeSecHdgs.get(n)[0]);
			if (n + 1 < listSidxEidxTypeSecHdgs.size()) {
				eIdxNext = Integer.parseInt(listSidxEidxTypeSecHdgs.get(n + 1)[1]);
			}
			eIdx = Integer.parseInt(listSidxEidxTypeSecHdgs.get(n)[1]);
			sectionName = listSidxEidxTypeSecHdgs.get(n)[3];
//			System.out.println(
//					"eIdx=" + eIdx + " eIdxNext=" + eIdxNext + " sIdx=" + sIdx + "\r\nsectionName=" + sectionName);
			if (eIdx > eIdxNext) {
//				System.out.println("continue sectionName =" + sectionName);
				continue;
			}

//			System.out.println("1 prelim sectionName==" + sectionName);

			if (sectionName.length() > 100) {
//				System.out.println("sectionName=" + sectionName);
				if (nlp.getAllIndexEndLocations(sectionName,
						Pattern.compile("(?sm)[a-z]{1}\\.(?=  ?([A-Z]{1}|\\([a-z]{1}\\)))")).size() > 0) {
					int idx = nlp.getAllIndexEndLocations(sectionName,
							Pattern.compile("(?sm)[a-z]{1}\\.(?=  ?([A-Z]{1}|\\([a-z]{1}\\)))")).get(0);
					sectionName = sectionName.substring(0, idx);
				}
			}

			if (n + 1 == listSidxEidxTypeSecHdgs.size()) {

//				System.out.println("last section is==" + sectionName);
				String[] ary = { sIdx + "", eIdx + "" };
				eIdx = GetContracts.getLastDefinedTermSimple(ary, text);
//				System.out.println("last section using last defined term simple is=="
//						+ text.substring(sIdx, GetContracts.getLastDefinedTermSimple(ary, text)));
			}
//			System.out.println("7 seconds = " + (endTime - startTime) / 1000);

//			System.out.println("bef sectionHeadingValidation==" + sectionName);
			sectionName_is_likely_wrong_ratio = elasticDocRectifier.heading_is_likely_wrong(sectionName);
			/*
			 * sectionHeading_is_likely_wrong method returns min val of 50 else 0
			 */

			if (sectionName_is_likely_wrong_ratio > 0) {
				sectionName = resetHeadingName(sectionName);
			}
			if (sectionName_is_likely_wrong_ratio > 0 && sectionName.length() > 200) {
//				System.out.println("1. sec name is wrong, len>200 and ratio>50. sectionName==" + sectionName);
				sectionName = "";
			}

//			System.out.println("aft sectionHeadingValidation==" + sectionName);
			String[] ary = { "" + sIdx, "" + eIdx, "sec=" + sectionName };
//			System.out.println(".put ary=" + Arrays.toString(ary));
			cntg = cntg + 0.00001;
			mapIdx.put((double) sIdx + cntg, ary);

		}

		mapSections = mapIdx;
		return mapIdx;

	}

	public TreeMap<Double, String[]> getExhibitsMapped(String text, TreeMap<Double, String[]> mapIdx)
			throws IOException {

		/*
		 * NOTE Do NOT touch text - length must not chg! No search and replace - length
		 * cannot chg! Or everything gets corrupted.
		 */

		NLP nlp = new NLP();

		double cntg = -0.00001;

		String sIdxStr = "", eIdxStr, exhPrefixPrev = "", exhPrefixNext = "--", exhPrefix = "-", exh;

//		long startTime = System.currentTimeMillis();

		List<String[]> listExh = SolrPrep.getExhibitNamesLax(text);

//		long elapsedTime = System.currentTimeMillis() - startTime;
//		System.out.println("getExh - SolrPrep.getExhibitNamesLax: " + elapsedTime / 1000 + " seconds");

//		startTime = System.currentTimeMillis();
		List<String[]> list = new ArrayList<String[]>();
		for (int i = 0; i < listExh.size(); i++) {
			// if exhibits are equal - then eIdx is at next instance.

			exh = listExh.get(i)[0];
			exhPrefix = nlp.getAllMatchedGroups(exh, patternExhPrefFromTocHdg).get(0)
//					.replaceAll("-?[\\d]", "")
			;
			if (nlp.getAllMatchedGroups(exhPrefix, Pattern.compile("\\.$")).size() > 0) {
//				System.out.println("a. listExh continue exhPrefix=" + exhPrefix);
				continue;
			}

			if (!exhPrefix.equals(exhPrefixPrev))
				sIdxStr = listExh.get(i)[1];
			if (exhPrefix.equals(exhPrefixPrev)) {
//				System.out.println("b. listExh continue - exhPrefix=" + exhPrefix);
				continue;
			}

//			System.out.println("exhPrefix=" + exhPrefix);

			if (i + 1 < listExh.size()) {
				eIdxStr = listExh.get(i + 1)[1];
			} else {
				eIdxStr = text.length() + "";
			}
			exhPrefixPrev = exhPrefix;
			String[] ary = { sIdxStr, Integer.parseInt(eIdxStr) + "", "exh=" + exh, "exhPrefix=" + exhPrefix };
			list.add(ary);
//			System.out.println("exh - ary=" + Arrays.toString(ary));
		}

//		elapsedTime = System.currentTimeMillis() - startTime;
//		System.out.println("getExh - list1: " + elapsedTime / 1000 + " seconds");

//		startTime = System.currentTimeMillis();
		int eIdx = 0, exhLtr = -1, exhLtrPrev = -1, exhLtrNext = -1, exhLtrNmb = -1, exhLtrNmbPrev = -1,
				exhLtrNmbNext = -1;

		String exhLtrStr = "", exhLtrPrevStr = "", exhLtrNextStr = "", exhLtrNmbStr = "", exhLtrNmbPrevStr = "",
				exhLtrNmbNextStr = "";

		Pattern pExhLtrNmbStr = Pattern.compile("(?sm)([A-Z]{1,2} ?- ?)(?=[\\d]{1,2})");
		Pattern pExhLtrStr = Pattern.compile("(?sm)(?<=[A-Z]{1,2})(-[\\d]{1,2}+)");

		for (int i = 0; i < list.size(); i++) {
			exhPrefix = list.get(i)[3];
			exhLtrNmb = -9;
			exhLtrNmbNext = -6;
			exhLtrNmbPrev = -3;
//			[0]=sIdx,[1]=eIdx,[2]=exhName,[3]=exhPrefix
//			System.out.println("AAAAlist.get=" + Arrays.toString(list.get(i)));

			if (list.get(i).length <= 3 || list.get(i)[3].split(" ").length <= 1
//					|| nlp.getAllMatchedGroups(list.get(i)[3].split(" ")[1], Pattern.compile("[^\\d]")).size() > 0
			) {
//				System.out.println("continuing==" + Arrays.toString(list.get(i)));
//				System.out.println("a. listExh#2 continue");
				continue;
			}

			exhLtrNmbStr = list.get(i)[3].split(" ")[1].replaceAll(pExhLtrNmbStr.toString(), "");
			exhLtrStr = list.get(i)[3].split(" ")[1].replaceAll(pExhLtrStr.toString(), "");
			exhLtr = convertAlphaExhNumb(exhLtrStr);
//			System.out.println(
//					exhPrefix + " exhLtrStr=" + exhLtrStr + " exhLtrNmbStr=" + exhLtrNmbStr + " exhLtr#=" + exhLtr);
			if (nlp.getAllMatchedGroups(exhLtrNmbStr, Pattern.compile("[\\d]+")).size() > 0) {
				exhLtrNmb = Integer.parseInt(nlp.getAllMatchedGroups(exhLtrNmbStr, Pattern.compile("[\\d]+")).get(0));
			}

			if (i + 1 < list.size() && list.get(i + 1).length > 3 && list.get(i + 1)[3].split(" ").length > 1) {
				exhLtrNmbNextStr = list.get(i + 1)[3].split(" ")[1].replaceAll(pExhLtrNmbStr.toString(), "");
				exhLtrNextStr = list.get(i + 1)[3].split(" ")[1].replaceAll(pExhLtrStr.toString(), "");
				exhLtrNext = convertAlphaExhNumb(exhLtrNextStr);
//				System.out.println(list.get(i + 1)[3] + " exhLtrNextStr=" + exhLtrNextStr + " exhLtrNmbNextStr="
//						+ exhLtrNmbNextStr + " exhLtrNext#=" + exhLtrNext);

				if (nlp.getAllMatchedGroups(exhLtrNmbNextStr, Pattern.compile("[\\d]+")).size() > 0) {
					exhLtrNmbNext = Integer
							.parseInt(nlp.getAllMatchedGroups(exhLtrNmbNextStr, Pattern.compile("[\\d]+")).get(0));
				}
			}

			if (i > 0 && list.get(i - 1)[3].split(" ").length > 1) {
				exhLtrNmbPrevStr = list.get(i - 1)[3].split(" ")[1].replaceAll(pExhLtrNmbStr.toString(), "");
				exhLtrPrevStr = list.get(i - 1)[3].split(" ")[1].replaceAll(pExhLtrStr.toString(), "");
				exhLtrPrev = convertAlphaExhNumb(exhLtrNextStr);
//				System.out.println(list.get(i - 1)[3] + " exhLtrPrevStr=" + exhLtrPrevStr + " exhLtrNmbPrevStr="
//						+ exhLtrNmbPrevStr + " exhLtrPrev#=" + exhLtrPrev);
				if (nlp.getAllMatchedGroups(exhLtrNmbPrevStr, Pattern.compile("[\\d]+")).size() > 0) {
					exhLtrNmbNext = Integer
							.parseInt(nlp.getAllMatchedGroups(exhLtrNmbPrevStr, Pattern.compile("[\\d]+")).get(0));
				}
			}

			if (i + 1 < list.size() && exhLtrNext - exhLtr == 1) {
				eIdx = Integer.parseInt(list.get(i + 1)[0]) - 1;
				// minus 1 - only do so if it is equal to next? Do this at 'Oh No'
//				System.out.println("dif=1. exh=" + list.get(i)[2]);
			} else {
				eIdx = Integer.parseInt(list.get(i)[1]) - 1;
				// minus 1 - only do so if it is equal to next? Do this at 'Oh No'
			}

			if (exhLtr - exhLtrPrev != 0 && exhLtr != 1 && exhLtr != 9 && exhLtr - exhLtrPrev != 1
					&& exhLtrNext - exhLtr != 1 && exhLtrNmbNext - exhLtrNmb != 1 && exhLtrNmb - exhLtrNmbPrev != 1) {
//				System.out.println("continue A. list.get(i)[3]=" + list.get(i)[3]);

//				System.out.println("exhLtr=" + exhLtr + " exhLtrNext=" + exhLtrNext + " exhPrefix=" + exhPrefix
//						+ " exhPrefixPrev=" + exhPrefixPrev + " exhLtr=" + exhLtr + " exhLtrPrev=" + exhLtrPrev
//						+ " exhLtrNext=" + exhLtrNext);
				continue;
			}
			if (exhLtr - exhLtrPrev == 0 && exhPrefix.equals(exhPrefixPrev)) {
//				System.out.println("continue B. list.get(i)[2]=" + list.get(i)[2]);
				continue;
			}

			cntg = cntg + -0.00001;
			String[] ary = { list.get(i)[0], eIdx + "", list.get(i)[2].trim() };
//			System.out.println("exh.ary==" + Arrays.toString(ary));
			mapIdx.put(Double.parseDouble(list.get(i)[0]) + cntg, ary);
			exhPrefixPrev = exhPrefix;
		}

//		elapsedTime = System.currentTimeMillis() - startTime;
//		System.out.println("getExh - mapIdx: " + elapsedTime / 1000 + " seconds");

		TreeMap<Double, String[]> mapIdx2 = new TreeMap<Double, String[]>();
		// scrub exhibit mapIdx

//		startTime = System.currentTimeMillis();
		int sIdx = 0, eIdxPrior = 0, sIdxPrior = 0;
		eIdx = 0;
		String exhName = "", exhNamePrior = "";
		double key = 0.0, keyPrior = 0.0;
		for (Map.Entry<Double, String[]> entry : mapIdx.entrySet()) {
			sIdx = Integer.parseInt(entry.getValue()[0]);
			eIdx = Integer.parseInt(entry.getValue()[1]);
			exhName = entry.getValue()[2].trim().replaceAll("exh(Toc)?=", "").trim();
			key = entry.getKey();
//			System.out.println("map key=" + key + " exhName" + exhName + " exhNamePrior=" + exhNamePrior);
			if (exhName.equals(exhNamePrior) && eIdx - sIdx > eIdxPrior - sIdxPrior && eIdxPrior > sIdx) {
				// then remove prior provided sIdx/sIdxPrior are w/n 3
				mapIdx2.remove(keyPrior);
//				System.out.println("removing it. exhNamePrior=" + exhNamePrior + " keyPrior=" + keyPrior);
			}
			if (exhName.equals(exhNamePrior) && eIdx - sIdx < eIdxPrior - sIdxPrior && eIdxPrior > sIdx) {
//				then continue;
				sIdxPrior = sIdx;
				eIdxPrior = eIdx;
				exhNamePrior = exhName;
				keyPrior = key;
//				System.out.println("continue C.");
				continue;
			}

			sIdxPrior = sIdx;
			eIdxPrior = eIdx;
			exhNamePrior = exhName;
			keyPrior = key;
			mapIdx2.put(entry.getKey(), entry.getValue());
		}
//		elapsedTime = System.currentTimeMillis() - startTime;
//		System.out.println("getExh - mapIdx2: " + elapsedTime / 1000 + " seconds");

//		NLP.printMapDblStrAry("mapIdx-exh", mapIdx2);

		return mapIdx2;

	}

	public List<Integer[]> convertAlphaParaNumb(String alphaNumber, int cnt) {

//		System.out.println("11 alphaNumber=" + alphaNumber);

		List<Integer[]> listInt = new ArrayList<>();
		// converts each to 1, 2, 3 ...

		String[] alphaParenLc = { "(a)", "(b)", "(c)", "(d)", "(e)", "(f)", "(g)", "(h)", "(i)", "(j)", "(k)", "(l)",
				"(m)", "(n)", "(o)", "(p)", "(q)", "(r)", "(s)", "(t)", "(u)", "(v)", "(w)", "(x)", "(y)", "(z)",
				"(aa)", "(bb)", "(cc)", "(dd)", "(ee)", "(ff)", "(gg)", "(hh)", "(ii)", "(jj)", "(kk)", "(ll)", "(mm)",
				"(nn)", "(oo)", "(pp)", "(qq)", "(rr)", "(ss)", "(tt)", "(uu)", "(vv)", "(ww)", "(xx)", "(yy)",
				"(zz)" };

		// v[5,22],i[1,9],x[10,24],ii[2,35],[20,50] <=Uc/Lc
		boolean found = false;
		if (alphaNumber.replaceAll("\\([a-z]{1,2}\\)", "").trim().length() < 1) {
			found = false;
			for (int i = 0; i < alphaParenLc.length; i++) {
				if (!found && alphaNumber.equals(alphaParenLc[i])) {
					found = true;
					Integer[] paraNumber = { (i + 1), cnt };
//					System.out.println("adding="+alphaNumber+" paraNmb="+paraNumber);
					listInt.add(paraNumber);
				}
			}
		}

		String[] romanParenLc = { "(i)", "(ii)", "(iii)", "(iv)", "(v)", "(vi)", "(vii)", "(viii)", "(ix)", "(x)",
				"(xi)", "(xii)", "(xiii)", "(xiv)", "(xv)", "(xvi)", "(xvii)", "(xviii)", "(ixx)", "(xx)" };

		if (alphaNumber.replaceAll("\\([ivx]{1,5}\\)", "").trim().length() < 1) {
			found = false;
			for (int i = 0; i < romanParenLc.length; i++) {
				if (!found && alphaNumber.equals(romanParenLc[i])) {
					found = true;
					Integer[] paraNumber = { (i + 1), cnt };
//					System.out.println("roman - adding="+alphaNumber+" paraNmb="+paraNumber);
					listInt.add(paraNumber);
				}
			}
		}

		String[] romanParenUc = { "(I)", "(II)", "(III)", "(IV)", "(V)", "(VI)", "(VII)", "(VIII)", "(IX)", "(X)",
				"(XI)", "(XII)", "(XIII)", "(XIV)", "(XV)", "(XVI)", "(XVII)", "(XVIII)", "(IXX)", "(XX)" };

		if (alphaNumber.replaceAll("\\([ivx]{1,5}\\)", "").trim().length() < 1) {
			found = false;
			for (int i = 0; i < romanParenUc.length; i++) {
				if (!found && alphaNumber.equals(romanParenUc[i])) {
					found = true;
					Integer[] paraNumber = { (i + 1), cnt };
//					System.out.println("adding="+alphaNumber+" paraNmb="+paraNumber);
					listInt.add(paraNumber);
				}
			}
		}

		String[] alphaParenUc = { "(A)", "(B)", "(C)", "(D)", "(E)", "(F)", "(G)", "(H)", "(I)", "(J)", "(K)", "(L)",
				"(M)", "(N)", "(O)", "(P)", "(Q)", "(R)", "(S)", "(T)", "(U)", "(V)", "(W)", "(X)", "(Y)", "(Z)"// ,
//				"(AA)", "(BB)", "(CC)", "(DD)", "(EE)", "(FF)", "(GG)", "(HH)", "(II)", "(JJ)", "(KK)", "(LL)", "(MM)",
//				"(NN)", "(OO)", "(PP)", "(QQ)", "(RR)", "(SS)", "(TT)", "(UU)", "(VV)", "(WW)", "(XX)", "(YY)",
//				"(ZZ)" 
		};

		if (alphaNumber.replaceAll("\\([A-Z]{1,2}\\)", "").trim().length() < 1) {
			found = false;
			for (int i = 0; i < alphaParenUc.length; i++) {
				if (!found && alphaNumber.equals(alphaParenUc[i])) {
					found = true;
					Integer[] paraNumber = { (i + 1), cnt };
//					System.out.println("adding="+alphaNumber+" paraNmb="+paraNumber);
					listInt.add(paraNumber);
				}
			}
		}

		String[] alphaPeriodUc = { "A.", "B.", "C.", "D.", "E.", "F.", "G.", "H.", "I.", "J.", "K.", "L.", "M.", "N.",
				"O.", "P.", "Q.", "R.", "S.", "T.", "U.", "V.", "W.", "X.", "Y.", "Z.", "AA.", "BB.", "CC.", "DD.",
				"EE.", "FF.", "GG.", "HH.", "II.", "JJ.", "KK.", "LL.", "MM.", "NN.", "OO.", "PP.", "QQ.", "RR.", "SS.",
				"TT.", "UU.", "VV.", "WW.", "XX.", "YY.", "ZZ." };

		if (alphaNumber.replaceAll("[A-Z\\.]{2,3}", "").trim().length() < 1) {
			found = false;
			for (int i = 0; i < alphaPeriodUc.length; i++) {
				if (!found && alphaNumber.equals(alphaPeriodUc[i])) {
					found = true;
					Integer[] paraNumber = { (i + 1), cnt };
//					System.out.println("adding="+alphaNumber+" paraNmb="+paraNumber);
					listInt.add(paraNumber);
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
			found = false;
			for (int i = 0; i < numberParen.length; i++) {
				if (!found && alphaNumber.equals(numberParen[i])) {
					found = true;
					Integer[] paraNumber = { (i + 1), cnt };
//						System.out.println("adding="+alphaNumber+" paraNmb="+paraNumber);
					listInt.add(paraNumber);
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
			found = false;
			for (int i = 0; i < numberPeriod.length; i++) {
				if (!found && alphaNumber.equals(numberPeriod[i])) {
					found = true;
					Integer[] paraNumber = { (i + 1), cnt };
//					System.out.println("adding=" + alphaNumber + " paraNmb=" + Arrays.toString(paraNumber));
					listInt.add(paraNumber);
				}
			}
		}

//		System.out.println("return listInt.size="+listInt.size());

		return listInt;
	}

	public int convertAlphaExhNumb(String alphaNumber) {

//		System.out.println("alphaNumber=" + alphaNumber);

		boolean found = false;
		String[] alphaPeriodUc = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q",
				"R", "S", "T", "U", "V", "W", "X", "Y", "Z", "AA", "BB", "CC", "DD", "EE", "FF", "GG", "HH", "II", "JJ",
				"KK", "LL", "MM", "NN", "OO", "PP", "QQ", "RR", "SS", "TT", "UU", "VV", "WW", "XX", "YY", "ZZ" };

		if (alphaNumber.replaceAll("[A-Z]{1,2}", "").trim().length() < 1) {
			found = false;
			for (int i = 0; i < alphaPeriodUc.length; i++) {
				if (!found && alphaNumber.equals(alphaPeriodUc[i])) {
					found = true;
//					System.out.println("adding="+alphaNumber+" paraNmb="+paraNumber);
					return (i + 1);
				}
			}
		}
		return -10;
	}

	public int convertAlphaNumericPgNo(String alphaNumber) {

		boolean found = false;
		String[] romanParenLc = { "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x", "xi", "xii", "xiii",
				"xiv", "xv", "xvi", "xvii", "xviii", "ixx", "xx" };

		if (alphaNumber.replaceAll("[ivx]{1,5}", "").trim().length() < 1) {
			found = false;
			for (int i = 0; i < romanParenLc.length; i++) {
				if (!found && alphaNumber.equals(romanParenLc[i])) {
					found = true;
					return (i + 1);
				}
			}
		}

		return -10;

	}

	public boolean isItConsecutivelyNumbered(List<Integer[]> list1cur, List<Integer[]> list2prior,
			List<Integer[]> list3next) {

		int cur = -1, prior = -3, next = -5;
		for (int n = 0; n < list1cur.size(); n++) {

			cur = list1cur.get(n)[0];

			for (int c = 0; c < list2prior.size(); c++) {
				prior = list2prior.get(c)[0];
			}

			for (int c = 0; c < list3next.size(); c++) {
				next = list3next.get(c)[0];
			}

			if (cur - prior == 0 || next - cur == 1 || next - cur == 0) {
//				System.out.println("IS consecutive="+true+" cur="+cur+" next="+next+" prior="+prior);
				return true;
			}

		}
//		System.out.println("NOT consecutive="+false+" cur="+cur+" next="+next+" prior="+prior);
		return false;

	}

	public String[] isHeadingConsecutive(List<String[]> list, int i, int hdg) throws IOException {

		NLP nlp = new NLP();
//		boolean consecutive = false;

		Pattern patternAlphNumber = Pattern.compile("\\([a-zA-Z\\d]{1,7}\\)|[\\d]{1,4}[\\.\\)]{1,2}");
//		System.out.println("list.get=" + Arrays.toString(list.get(i)) + " hdg=" + hdg);
//		System.out.println(nPlp.getAllMatchedGroups(list.get(i)[hdg], patternAlphNumber).get(0));

		String pNmb = "", pNmbNext = "", pNmbPrev = "";
		int pNmbInt = 0, pNmbNextInt = 0, pNmbPrevInt = 0;

		List<Integer[]> listPnmb = new ArrayList<Integer[]>();
		List<Integer[]> listPnmbPrev = new ArrayList<Integer[]>();
		List<Integer[]> listPnmbNext = new ArrayList<Integer[]>();

		// if alph numb paras are consecutive for a heading it is likely that hdg
		// relates to all the text between them. Or to next section hdg.
		pNmbInt = -20;
		pNmbNextInt = -30;
		pNmbPrevInt = -10;

		listPnmb = new ArrayList<Integer[]>();
		listPnmbPrev = new ArrayList<Integer[]>();
		listPnmbNext = new ArrayList<Integer[]>();

		pNmb = "";
		pNmbNext = "";
		pNmbPrev = "";

		if (i > 0 && nlp.getAllMatchedGroups(list.get(i - 1)[hdg], patternAlphNumber).size() > 0) {
			pNmbPrev = nlp.getAllMatchedGroups(list.get(i - 1)[hdg], patternAlphNumber).get(0);
			listPnmbPrev = convertAlphaParaNumb(pNmbPrev, -1);
//			System.out.println("listPnmbPrev.size=" + listPnmbPrev.size());
//			System.out.println("pNmbPrev=" + pNmbPrev);
		}

		if (nlp.getAllMatchedGroups(list.get(i)[hdg], patternAlphNumber).size() > 0) {
			pNmb = nlp.getAllMatchedGroups(list.get(i)[hdg], patternAlphNumber).get(0);
			listPnmb = convertAlphaParaNumb(pNmb, 0);
//			System.out.println("pNmb=" + pNmb);
		}

		if (i + 1 < list.size() && nlp.getAllMatchedGroups(list.get(i + 1)[hdg], patternAlphNumber).size() > 0) {
			pNmbNext = nlp.getAllMatchedGroups(list.get(i + 1)[hdg], patternAlphNumber).get(0);
			listPnmbNext = convertAlphaParaNumb(pNmbNext, 1);
//			System.out.println("pNmbNext=" + pNmbNext);
		}

//		consecutive = false;
//		System.out.println("listPnmb.size=" + listPnmb.size());
		for (int n = 0; n < listPnmb.size(); n++) {
//			System.out.println("listPnmb=" + Arrays.toString(listPnmb.get(n)));
			pNmbInt = listPnmb.get(n)[0];

			for (int c = 0; c < listPnmbNext.size(); c++) {
				pNmbNextInt = listPnmbNext.get(c)[0];
			}

			for (int c = 0; c < listPnmbPrev.size(); c++) {
				pNmbPrevInt = listPnmbPrev.get(c)[0];
			}

			if (pNmbNextInt - pNmbInt != 1) {
				isNextConsecutive = false;
			}

			if (pNmbNextInt - pNmbInt == 0 || pNmbNextInt - pNmbInt == 1 || pNmbInt - pNmbPrevInt == 0
					|| pNmbInt - pNmbPrevInt == 1) {
//				consecutive = true;
//				System.out.println("consec=true pNmb=" + pNmb + " pNmbNext=" + pNmbNext + " pNmbPrev=" + pNmbPrev);
				if (pNmbNext.trim().length() == 0) {
					pNmbNext = "0";
				}

				String[] ary = { "true", "pNmbNext=" + pNmbNext };
				return ary;
			}
		}

//		System.out.println("consec=false pNmb=" + pNmb + " pNmbNext=" + pNmbNext + " pNmbPrev=" + pNmbPrev);

		String[] ary = { "false", "pNmbNext=0" };
		return ary;
//		return consecutive;

	}

	public TreeMap<Double, String[]> getHeadings(String text, TreeMap<Double, String[]> mapIdx, boolean isDisclosure)
			throws IOException {

		// GoLaw gl = new GoLaw();

		/*
		 * NOTE Do NOT touch text - length must not chg! No search and replace - length
		 * cannot chg! Or everything gets corrupted.
		 */

		/*
		 * getHeadings() runs on non-Section, Definition (and non-exhibit and non-toc)
		 * formmatted hdgs. It fetches first round of hdgs that are initial caps or
		 * formatted as previously marked by Qs
		 */

//		System.out.println("headings start");
		NLP nlp = new NLP();
		double cntg = 0.000002;

		int dist = 135;
//		

		Pattern patternHdg1 = Pattern.compile("(?im)(?<=[\r\n]{2}[ \"]{0,3}|XQ)(\\.? ?)(Qx).{1," + dist + "}?(XQ)" + "|"
				+ "(?<=[\r\n]{2})\\(?[a-zA-Z\\d]{1,5}\\.\\)? ?(Qx).{1," + dist + "}?(XQ)\\.?" + "|" + "([\r\n]{2} ?)"
				+ "(\\(?[\\dA-Za-z]{1,3} ?\r?\n? ?" + "(\\.\\d?\\d?\\.?\\d?|\\)" + ")? ?" + ") ? ?\\[?[A-Z].{1," + dist
				+ "}?((\\.?!\\d|:)(XQ)?|((\r\n)))"

				+ "|" + "(?<=[\r\n]{2} ?)(\\(?[\\dA-Za-z]{1,3}" + "(" + "\\.\\d?\\d?\\.?\\d?|\\)" + ") "
				+ ")? ? ?\\[?[A-Z].{1," + dist + "}?((\\.?!\\d|:)|(?=(\r\n)))" + "");

		Pattern patternHdg2 = Pattern.compile(
				"(?<=\r\n)(\\([a-zA-Z\\d]{1,2}\\)|[\\d\\.]{2,4}).{1,3}[A-Z]{1}.{4," + dist + "}?(\r\n|[\\.:]{1})");

		String paraNmbStr = "((?<=[\r\n]{2}[ ]{0,10})" + "(" + "(SECTION|Section)( (\\(?[\\d]+\\)?)\\.?"
				+ "(\\(?[\\d]+\\.?\\d?\\d?|\\(?[a-zA-Z]{1,4}[\\)\\.]{1,2})? ?)" + "|(\\(?[a-zA-Z\\d]{1,7}\\)) ?"
				+ "|[\\dA-Za-z]{1,3}\\.([\\d]+\\.?)+ " // 1.10. OR 7.2.9 or 7.2.9.
				+ "|[\\d]{1,3}\\.([\\d]+)?(\\.[\\d]+\\.?)? ?" + "|[A-Za-z]{1}\\. ?(?=[A-Za-z]{2})"// 1.1.1
				+ "|(SECTION|Section)[\\d]+\\.([\\d]+)?(?=[A-Z]{1})" + "))";

		Pattern patternHdg3 = Pattern.compile(paraNmbStr + "(.{0,1500}\\.[ ]{0,5}(XQ|(?=[^\\d]{1}|\r\n)))");

		Pattern patternHdg = Pattern
				.compile(patternHdg1.pattern() + "|" + patternHdg3.pattern() + "|" + patternHdg2.pattern());

		// the heading is the shorter of the pattern found at the first period or first
		// hard return.

		Pattern ptmpRepl = Pattern.compile("(?<=(ITEM|Item) [\\d]{1,2})\\.(?= [A-Z]{1})");
		// must replace any character with an equal # of chars else indexing is thrown
		// off
		String specialReplChar = "~";
		Pattern ptmpInsert = Pattern.compile("(?<=\\d)" + specialReplChar + "(?= [A-Z]{1})");
		Pattern pAlphaNumeric = Pattern.compile(" ?(^\\([A-Za-z\\d]{1,3}\\)|^[A-Za-z\\d]{1,3}\\.\\)?) ");
		text = text.replaceAll(ptmpRepl.toString(), specialReplChar);// replacement is 1 for 1 so original txt.len is
																		// not corrupted.

		List<String[]> list = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(text, patternHdg);
		if (list.size() > 500)
			return mapIdx;
//		NLP.printListOfStringArray("paranmbstr=", list);

//		System.out.println("size of list of potential headings=" + list.size());
		PrintWriter pw = new PrintWriter("test.txt");
		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter(new File("c:/temp/listParas.txt"));
			for (int i = 0; i < list.size(); i++) {
				pw.append(Arrays.toString(list.get(i)) + "||\r\n");
			}
			pw.close();
		}

		String txtStopped = "", txt = "", para = "";
		int excludeInt = 0, excludeIntAsci = 0, eIdxHdr = 0, sIdx, sIdxNext;
		String tmp = "";
		List<String[]> listHdg = new ArrayList<String[]>();
//		int alphNmb = -1, alphNmbNext = -5, alphNmbPrev = -10;
		List<Integer[]> listCur = new ArrayList<Integer[]>();
		List<Integer[]> listPrior = new ArrayList<Integer[]>();
		List<Integer[]> listNext = new ArrayList<Integer[]>();

		String alphNmbStr = "", alphNmbNextStr = "", alphNmbPrevStr = "";

		/*
		 * As it iterates of hdg patterns - first cursory exclusion tools are used to
		 * remove F/Ps. Thereafter primary tool to validate it is a hdg is if it is
		 * followed by a paragraph (len>60 or so) and hdg is only initial caps/all caps
		 * after removing stop words
		 */

//		System.out.println("list.size===" + list.size());
		String snippet = "";
		long startTime = System.currentTimeMillis();
		String qXhdg = "", asciHdg = "", asciHdgStop = "";
		Pattern patternAsciHdg = Pattern.compile("(^(SECTION|Section) [\\d]+\\.? [A-Z].{0,95}\\.(?=( ? ?\r\n| [A-Z])))|"
				+ "^([\\d\\.]{2,}|\\(?[A-Za-z\\d]{1,5}\\)|[A-Za-z\\d]{1,4}\\.)( [A-Z].{0,95}\\.)(?=( ? ?\r\n| [A-Z]))");
		boolean asciHdgIsInitialCaps = false;
		for (int i = 0; i < list.size(); i++) {
			asciHdgIsInitialCaps = false;
			sIdxNext = 0;
			if (i + 1 < list.size()) {
				sIdxNext = Integer.parseInt(list.get(i + 1)[1]);
			}
			if (i + 1 == list.size())
				sIdxNext = text.length();

			sIdx = Integer.parseInt(list.get(i)[1]);
			txt = list.get(i)[0].trim();
			txtStopped = goLawRemoveStopWordsAndContractNamesLegalStopWords(
					txt.replaceAll("[\r\n\\.]+", " ").replace("/", " ")
							.replaceAll(" ?^\\([a-z\\d]+\\)|^[a-z\\d]+\\.\\)?", "").replaceAll("[^a-zA-Z ]+", ""))
									.replaceAll(" XQ", " ").replaceAll("[ ]+", " ").trim();
			snippet = txt.substring(0, Math.min(75, txt.length()));

			if (print7c) {
				System.out.println("7c.snipped=" + snippet);
			}
			asciHdg = "";
			asciHdgStop = "";
			qXhdg = "";
			if (nlp.getAllMatchedGroups(txt, Pattern.compile("^.{0,18}Qx.{0,95}XQ(\r?\n?\\.?)")).size() > 0) {
				qXhdg = nlp.getAllMatchedGroups(txt, Pattern.compile("^.{0,18}Qx.{0,95}XQ(\r?\n?\\.?)")).get(0);
			}
			if (nlp.getAllMatchedGroups(txt, patternAsciHdg).size() > 0) {// and must be mostly initial caps
				asciHdg = nlp.getAllMatchedGroups(txt, patternAsciHdg).get(0);
				asciHdgStop = goLawRemoveStopWordsAndContractNamesLegalStopWords(
						asciHdg.replaceAll("[\r\n\\.]+", " ").replace("/", " ")
								.replaceAll(" ?^\\([a-z\\d]+\\)|^[a-z\\d]+\\.\\)?", "").replaceAll("[^a-zA-Z ]+", ""))
										.replaceAll("[ ]+", " ").trim();
				if (SolrPrep.isItMostlyInitialCaps_measure(asciHdgStop) < .8) {
					asciHdg = "";
				} else {
					asciHdgIsInitialCaps = true;
				}

//				System.out.println("asciHdg=" + asciHdg + "\r\nasciHdgStop=" + asciHdgStop);
			}

//			System.out.println("snippet==" + snippet);
			if (nlp.getAllMatchedGroups(txt,
					Pattern.compile("^.{0,20}Qx.{1,100}XQ" + "|^.{0,5}Section" + "|(^.{0,5}" + "|^[\\d\\.]{2,}"
							+ "|^\\(?[A-Za-z\\d]{1,5}\\)" + "|^[A-Za-z\\d]{1,4}\\.) [A-Z]{1}"))
					.size() == 0
					|| nlp.getAllMatchedGroups(snippet, Pattern.compile("^(Qx)?([\\d]{2,}|To|TO) ")).size() > 0
					|| nlp.getAllMatchedGroups(snippet, Pattern.compile("^(Qx)?([\\d\\.]{2,}%) ")).size() > 0) {
				if (print7c) {
					System.out.println("1aaa. continue..txt snip=" + snippet);
				}
				continue;

			}

			if (!isDisclosure
					&& nlp.getAllMatchedGroups(txt.replaceAll("[\r\n]+ ?|Qx ?", ""),
							Pattern.compile("^([A-Za-z]{2} |[A-Za-z]{1} )")).size() > 0
					&& nlp.getAllMatchedGroups(txt.replaceAll("[\r\n]+ ?|Qx ?", ""),
							Pattern.compile("^([\r\n]|(Qx))? ?(SECTION|Section) ")).size() == 0
					&& !txt.contains("def=")
					&& nlp.getAllMatchedGroups(txt, Pattern.compile("^Qx.{3,135}XQ$")).size() == 0

			) {
				if (print7c) {
					System.out.println("1aaaa snip=" + txt + " sz="
							+ nlp.getAllMatchedGroups(txt, Pattern.compile("^([\r\n]|(Qx))?([A-Za-z]{2}|[A-Za-z]{1} )"))
									.size()
							+ " sz2="
							+ nlp.getAllMatchedGroups(txt, Pattern.compile("^([\r\n]|(Qx))? ?(SECTION|Section) "))
									.size());
					// must start w/ para # if contract.
				}
				continue;

			}

//			
			// System.out.println("heading pattern found snip=" + snippet);
//			alphNmb = -1;
//			alphNmbNext = -5;
//			alphNmbPrev = -10;
			para = "";

			alphNmbStr = txt.split(" ")[0];
			listCur = convertAlphaParaNumb(alphNmbStr, 0);
			if (i + 1 < list.size()) {
				alphNmbNextStr = list.get(i + 1)[0].trim().split(" ")[0];
				if (convertAlphaParaNumb(alphNmbNextStr, 0).size() > 0) {
					listNext = convertAlphaParaNumb(alphNmbNextStr, 0);
				}
			}

			if (i > 0) {
				alphNmbPrevStr = list.get(i - 1)[0].trim().split(" ")[0];
				if (convertAlphaParaNumb(alphNmbPrevStr, 0).size() > 0) {
					listPrior = convertAlphaParaNumb(alphNmbPrevStr, 0);
				}
			}

			eIdxHdr = Integer.parseInt(list.get(i)[2]);
//			System.out.println("eIdxHdr=" + eIdxHdr);

			if (nlp.getAllMatchedGroups(txt,
					Pattern.compile("^((\\(?[a-zA-Z]{1,5}\\.?\\)|\\(?[\\d\\.]{1,6}\\)?)?) ?Qx.{1,80}XQ")).size() == 0
					&& (txt.replaceAll("[^a-zA-Z]", "").replaceAll("(?ism)(qx|XQ|xxPD)+", "").trim().length() < 4
							|| txt.replaceAll("[a-z\\d;,\\.\\$]+", "").replaceAll("Qx|XQ|xxPD|xx", "").length() == 0
							|| txt.trim().substring(txt.trim().length() - 1, txt.trim().length()).equals(";"))

			) {
//				System.out.println("1bbb. continue");// . txt.snip=" + txt.substring(0, Math.min(txt.length(),
				// 30)).trim());
				if (print7c) {
					System.out.println("1 cont.txt.snip=" + txt.substring(0, Math.min(txt.length(), 30)).trim()
							+ " len=="
							+ txt.replaceAll("[^a-zA-Z]", "").replaceAll("(?ism)(qx|XQ|xxPD)+", "").trim().length()
							+ " 2L=" + txt.replaceAll("[QXXPDa-z\\d;,\\.\\$]+", "").length());
				}
				continue;

			}

//			System.out.println("txtStop==" + txtStopped);
			tmp = txt.replaceAll(pAlphaNumeric.toString(), "");
			if (nlp.getAllMatchedGroups(tmp, Pattern.compile("^.{0,18}Qx.{1,85}XQ")).size() > 0)
				tmp = nlp.getAllMatchedGroups(tmp, Pattern.compile("^.{0,18}Qx.{1,85}XQ")).get(0);

//			System.out.println("tmp=" + tmp);
			excludeInt = 0;
			excludeIntAsci = 0;
			excludeInt = nlp.getAllIndexStartLocations(tmp, patternExcludeMeFromHeadings).size();
			excludeIntAsci = nlp.getAllIndexStartLocations(asciHdg, patternExcludeMeFromHeadings).size();

			if ((excludeInt > 0 && !nlp.getAllMatchedGroups(tmp, patternExcludeMeFromHeadings).get(0).toUpperCase()
					.equals("SECTION") && !asciHdgIsInitialCaps) ||

					(excludeIntAsci > 0 && !nlp.getAllMatchedGroups(asciHdg, patternExcludeMeFromHeadings).get(0)
							.toUpperCase().equals("SECTION") && asciHdgIsInitialCaps)
					|| (excludeInt > 0 && nlp.getAllMatchedGroups(tmp, patternExcludeMeFromHeadings).get(0)
							.toUpperCase().equals("SECTION") && snippet.split(" ").length < 5)

			)

			{
				if (print7c) {
					System.out.println("1......excludeInt>0. continue. excluded b/c="
							+ nlp.getAllMatchedGroups(tmp, patternExcludeMeFromHeadings).get(0));
				}
				continue;
			}
			if (

			(// a header can't contain certain words or characters.
			((txtStopped.length() > 100
					|| nlp.getAllIndexStartLocations(txtStopped, Pattern.compile("(^| )[a-z]{1}")).size() > 0)
					&& !txt.contains("Qx"))
					|| nlp.getAllIndexEndLocations(txt,
							Pattern.compile("(?sm)QxSee |Qx.{0,4}(YEAR|Year|For the Year Ended).{0,4}XQ")).size() > 0)

					&& nlp.getAllMatchedGroups(txt.trim(), Pattern.compile("(?sm)^.{1,18}Qx.{1,85}?XQ")).size() == 0
					&& nlp.getAllMatchedGroups(txt.replaceAll("[\r\n]+ ?|Qx ?", ""),
							Pattern.compile("^([\r\n]|(Qx))? ?(SECTION|Section) ")).size() == 0
					&& nlp.getAllMatchedGroups(txt, Pattern.compile("^(Qx)?\\(?[\\da-zA-Z]{1,5}(\\.|\\))")).size() == 0
					&& nlp.getAllMatchedGroups(txt, Pattern.compile("^Qx.{3,135}XQ$")).size() == 0)

			// a header can't have lower case words once the stop words are removed.
			{
				// it is not a header if: the text is too long, if it has excluded pattern or
				// there are initial cap words after being stopped.
				if (print7c) {
					System.out.println("2... continue. txt snip=" + txt.substring(0, Math.min(100, txt.length()))
							+ " sz===="
							+ nlp.getAllMatchedGroups(txt.trim(), Pattern.compile("(?sm)^.{1,18}Qx.{1,85}?XQ")).size()
							+ " exclude=" + excludeInt);
				}
				continue;
			}

//			System.out.println("isHdg?=" + txt.replaceAll(ptmpInsert.toString(), "\\."));
			// what follows a header should be a paragraph that is at least 300 chars
			para = getParagraphAfterHeading(text, (sIdx + list.get(i)[0].length()), sIdxNext);

			if (para.length() < 60 // this is important to remove tons of garbage -- like Exhibit 10.1 etc
					&& nlp.getAllIndexEndLocations(list.get(i)[0], Pattern.compile("(?ism)ticker|symbol")).size() == 0
					&& !isItConsecutivelyNumbered(listCur, listPrior, listNext)
					&& nlp.getAllMatchedGroups(list.get(i)[0].trim(), Pattern.compile(
							"^((Qx)?(Qx)?\\(?[a-zA-Z\\d]{1,5}\\.?\\)|[a-zA-Z\\d]{1,2}\\.\\)?|[\\d\\.]{1,7})|^(Qx)(Qx)[\\d]{1,3}\\. [A-Z]{1}|^Qx[\\d\\.]{2,5}.{2,80}XQ"))
							.size() == 0
					&& nlp.getAllMatchedGroups(list.get(i)[0].trim(),
							Pattern.compile("^(Qx)?(Qx)? ?(Section|SECTION) ")).size() == 0

					&& nlp.getAllMatchedGroups(txt, Pattern.compile("^.{0,7}Qx.{3,85}XQ")).size() == 0
					// Can be all caps if encased in Qx..XQ
					&& nlp.getAllMatchedGroups(txt, Pattern.compile("(^Section \\d|SECTION \\d).{1,7}(Qx)?[A-Z]"))
							.size() == 0

			) {
				// if the text that follows isn't at least 1 para of 70 chars or more it isn't
				// possibly a hdg
//				System.out
//						.println("3..continue.." + " para.len=" + para.length() + " list.get(i)[0]=" + list.get(i)[0]);

//						+ "\r\ni=" + i + "<60 para.len=" + para.length() + " para.snip="
//						+ para.substring(0, Math.min(150, para.length())).replaceAll("(?<=Qx) ?[\r\n]+", " ")
//								.replaceAll("[ ]+(?=XQ)", "").replaceAll("Qx[ ]+", "Qx").replaceAll("[ ]+", " ")
//								.trim());
				if (print7c) {
					System.out.println("3.continue. txt=" + txt);
				}
				continue;
			}

			if ((goLawRemoveStopWordsAndContractNamesLegalStopWords(StopWords.removeDefinedTerms(para))
					.replaceAll("[ ]+", " ").split(" ").length < 5
					|| para.replaceAll("[a-z]+", "").length() == para.length())

					&& nlp.getAllMatchedGroups(para, Pattern.compile(":$")).size() == 0 && !txt.contains("Qx")
					&& !SolrPrep.isItMostlyInitialCaps(txt)
					&& nlp.getAllMatchedGroups(txt, patternParaNmb).size() == 0) {
				if (print7c) {
					System.out.println("4 continue.... snip=" + snippet);
				}
				continue;

			}

			if ((goLawRemoveStopWordsAndContractNamesLegalStopWords(StopWords.removeDefinedTerms(para))
					.replaceAll("[ ]+", " ").split(" ").length < 5
					|| para.replaceAll("[a-z]+", "").length() == para.length())

					&& nlp.getAllMatchedGroups(para, Pattern.compile(":$")).size() == 0 && !txt.contains("Qx")
					&& txt.length() > 75 && nlp.getAllMatchedGroups(txt, patternParaNmb).size() == 0) {
				if (print7c) {
					System.out.println("4b continue. . snip=" + snippet);// txt.snip=" + txt.substring(0,
//																		 Math.min(txt.length(), 30)).trim()
//						+ "\r\npara=" + para);
				}
				continue;

			}

			if (goLawRemoveStopWordsAndContractNamesLegalStopWords(goLawRemoveDefinedTerms(para)).length() == 0
					&& ((qXhdg.length() > 0 && qXhdg.indexOf(". ") > 0
							&& SolrPrep.isItMostlyInitialCaps_measure(qXhdg.substring(0, qXhdg.indexOf(". "))) < .5)
							|| (qXhdg.length() == 0 && SolrPrep.isItMostlyInitialCaps_measure(txt) < .5))
					&& !asciHdgIsInitialCaps) {
				if (print7c) {
					System.out.println("5. continue.. snip=" + snippet);// para=" + para.substring(0,
					// Math.min(para.length(), 50))
//						+ " rem stop words, def term="
//						+ StopWords.removeStopWords(StopWords.removeDefinedTerms(list.get(i)[0])).trim());
				}
				continue;

			}

			if (SolrPrep.isItMostlyInitialCaps_measure(txt) < .5 && para.split(" ").length > 10
					&& nlp.getAllMatchedGroups(txt, Pattern.compile("Qx.{1,100}XQ")).size() == 0
					&& nlp.getAllMatchedGroups(txt, Pattern.compile("^.{0,95}[A-Z]{1}[a-z]{3,}\\. ")).size() > 0// same
																												// as 5a
					&& !SolrPrep.isItMostlyInitialCaps(
							nlp.getAllMatchedGroups(txt, Pattern.compile("^.{0,95}[A-Z]{1}[a-z]{3,}\\. ")).get(0))) {// same
																														// as
																														// above
				if (print7c) {
					System.out.println("5aa. continue... snip=" + snippet);
				}
				continue;
			}

			if (SolrPrep.isItMostlyInitialCaps_measure(txtStopped) < .9 && para.split(" ").length > 10
					&& nlp.getAllMatchedGroups(txt, Pattern.compile("Qx.{1,100}XQ")).size() == 0
					&& nlp.getAllMatchedGroups(txt, Pattern.compile("^.{0,95}[A-Z]{1}[a-z]{3,}\\. ")).size() == 0) {
				// same as 5aa
				if (print7c) {
					System.out.println("5a. continue.... snip=" + snippet);
				}
				continue;
			}

			if (SolrPrep.isItMostlyInitialCaps_measure(txtStopped) < .9 && txt.length() > 60
					&& nlp.getAllMatchedGroups(txt, Pattern.compile("^(\\(?[a-zA-Z]{1,5}\\.?\\))?Qx.{1,80}XQ"))
							.size() == 0
					&& nlp.getAllMatchedGroups(txt,
							Pattern.compile("^((\\(?[a-zA-Z]{1,5}\\.?\\)|\\(?[\\d\\.]{1,6}\\)?)?) ?Qx.{1,80}XQ"))
							.size() == 0
					&& nlp.getAllMatchedGroups(txt, Pattern.compile("^(Qx)?(Qx)?(SECTION|Section)  ?[\\d]")).size() == 0
					&& !asciHdgIsInitialCaps) {
				if (print7c) {
					System.out.println("6a. continue.  not initial caps and long txtStopped... snip=" + snippet);
				}
				continue;

			}
//			if (goLawRemoveStopWordsAndContractNamesLegalStopWords(goLawRemoveDefinedTerms(para)).length() == 0
//					&& para.length() > 75 ) {
//				if (print7c) {
//					System.out.println("6b. continue... snip=" + snippet);// para=" + para.substring(0,
			// Math.min(para.length(), 50))
//						+ " rem stop words, def term="
//						+ StopWords.removeStopWords(StopWords.removeDefinedTerms(list.get(i)[0])).trim());
//				}
//				continue;

//			} //not needed b/c I exclude if no para number starts heading. And if one does - the above would kick it it out and it should not b/c even if all caps if heading is preceded by a para # it is a heading.

			if (!SolrPrep.isItMostlyInitialCaps(txt)
					&& nlp.getAllMatchedGroups(txt, Pattern.compile("^.{0,18}Qx.{1,200}?XQ")).size() == 0
					&& nlp.getAllMatchedGroups(txt, Pattern.compile("^.{0,95}[A-Z]{1}[a-z]{3,}\\. ")).size() == 0
					&& nlp.getAllMatchedGroups(txt, patternParaNmb).size() == 0
			// same as 5a.
			) {
				if (print7c) {
					System.out.println("6c.continue... snip=" + snippet);
				}
				continue;
			}

			if (!SolrPrep.isItMostlyInitialCaps(txt) && txt.length() > 500 && nlp
					.getAllMatchedGroups(txt, Pattern.compile("[\\(\\)\\.\\dA-Za-z]{1,7} ?Qx.{1,90}XQ")).size() == 0) {
				if (print7c) {
					System.out.println("7.continue... snip=" + snippet);
				}
				continue;
			}

			if (txt.replaceAll("[A-Z, :;'\" \\(\\)]+", "").length() < 5
					&& (txt.length() > 80 || (txt.length() > 45 && !txt.contains("\\. ?$")))
					&& txt.replaceAll("(\\(?[\\d\\.]+\\)? |\\(?[A-Za-z][\\)\\.]{1,2} )?Qx.{1,85}XQ", "").trim()
							.length() > 1
					&& nlp.getAllMatchedGroups(txt, Pattern.compile("\\. ?$")).size() == 0
			// can be all of caps so long as it is encased in Qx..XQ
			) {
				if (print7c) {
					System.out.println("8. all caps. continue..... snip=" + snippet);
				}
				continue;
			}

			if (SolrPrep.isItMostlyInitialCaps_measure(txtStopped) < .7

					&& nlp.getAllMatchedGroups(list.get(i)[0].trim(), Pattern.compile(
							"^((Qx)?(Qx)?\\(?[a-zA-Z\\d]{1,5}\\.?\\)|(Qx)?(Qx)?[a-zA-Z\\d]{1,2}\\.\\)?|[\\d\\.]{1,7})|^(Qx)(Qx)?[\\d]{1,3}\\. [A-Z]{1}|^Qx(Qx)?[\\d\\.]{2,5}.{2,80}XQ"))
							.size() == 0

					&& nlp.getAllMatchedGroups(txt, Pattern.compile("^(Qx)?(Qx)?(SECTION|Section)  ?[\\d]"))
							.size() == 0) {
				if (print7c) {
					System.out.println("9.not initial caps....txtStopped=" + txtStopped + " ... snip=" + snippet);
				}
				continue;
			}

			Pattern ptrn = Pattern.compile("^(\\(?[A-Za-z\\d]{1,4}\\. )?.{0,95}[A-Z]+[a-z]+[\r\n\\.]");// move to start

			if ((nlp.getAllMatchedGroups(txt, ptrn).size() > 0
					&& nlp.getAllMatchedGroups(txt, Pattern.compile("^.{0,18}Qx.{1,200}?XQ")).size() == 0
					&& SolrPrep.isItMostlyInitialCaps(nlp.getAllMatchedGroups(txt, ptrn).get(0))
//							|| ( SolrPrep.isItMostlyInitialCaps(
//									nlp.getAllMatchedGroups(txt, ptrn).get(0))
//									&&
//									nlp.getAllMatchedGroups(txt,patternParaNmb).size()==0
//									)
			)

			) {
				String[] ary = { list.get(i)[1],
						(Integer.parseInt(list.get(i)[1]) + nlp.getAllMatchedGroups(txt, ptrn).get(0).length()) + "",
						nlp.getAllMatchedGroups(txt, ptrn).get(0).replaceAll(ptmpInsert.toString(), "\\.").trim() };
//				System.out.println("..........1.adding ary==" + Arrays.toString(ary));
				listHdg.add(ary);
			} else {
				String[] ary = { list.get(i)[1], list.get(i)[2],
						list.get(i)[0].replaceAll(ptmpInsert.toString(), "\\.").trim() };
				listHdg.add(ary);
//				System.out.println("..........2..adding ary==" + Arrays.toString(ary));
			}
//			System.out.println(".add hdg==" + list.get(i)[0].replaceAll(ptmpInsert.toString(), "\\.").trim());
		}
		System.out.println("getHeadings part1 seconds=" + (System.currentTimeMillis() - startTime) / 1000);
		startTime = System.currentTimeMillis();

//		System.out.println("end list. listHdg.size=" + listHdg.size());

		/*
		 * after this initial filter is performed and results stored to a list, the new
		 * list below iterated over to determine if hdgs numbers are consecutive by
		 * calling the method isHeadingConsecutive. A hdg can be consecutive b/c the
		 * next hdg is consecutively numbered or the current hdg is equal to 1 (eg
		 * (a),(1),(A), etc). If hdg is consecutively numbered then eIdx = sIdxNext-1.
		 */

		tmp = "";
		int eIdx = 0;
		/*
		 * if hdg does not contain Qx and it is not consecutive then don't include as
		 * hdg. If it is consecutive but last consecutive item (eg (a)-(d) and at (d) is
		 * last consec and it does not contain Qx then only get eIdx as end of para. If
		 * consec get eIdx as sIdxNext-1
		 */

		// DELETE ME====>
		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter(new File("c:/temp/7c. listHdgs.txt"));
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < listHdg.size(); i++) {
				String[] ary = listHdg.get(i);
				sb.append("7c. listHdgs i=" + i + " " + Arrays.toString(ary) + "\r\n");
			}
			pw.append(sb.toString());
			pw.close();
		}
		// <====DELETE ME

		List<String[]> listHdgCut = new ArrayList<String[]>();
		String tmpStr = "";
		for (int i = 0; i < listHdg.size(); i++) {
//			System.out.println("2. 7d heading pattern snip=" + listHdg.get(i)[2]);
			tmpStr = listHdg.get(i)[2];
			tmpStr = tmpStr
//					.replaceAll("XQQx", "")
					.replaceAll("Qx.{1,2}XQ", "");
//			System.out.println("meas=" + SolrPrep.isItMostlyInitialCaps_measure(tmpStr) + " tmpStr words="
//					+ tmpStr.split(" ").length + " QX pat siz=="
//					+ nlp.getAllMatchedGroups(tmpStr, Pattern.compile("Qx.{1,100}?XQ")).size());
			if (SolrPrep.isItMostlyInitialCaps_measure(tmpStr) < .5 && tmpStr.split(" ").length > 10
					&& nlp.getAllMatchedGroups(tmpStr, Pattern.compile("^.{1,18}Qx.{1,100}?XQ")).size() > 0) {
				String[] ary = { listHdg.get(i)[0], listHdg.get(i)[1],
						nlp.getAllMatchedGroups(tmpStr, Pattern.compile("^.{1,18}Qx.{1,100}?XQ")).get(0) };
				listHdgCut.add(ary);
				continue;
			}
			if (tmpStr.indexOf("XQ") > 0 && listHdg.get(i)[2].indexOf("Qx") < 19) {
				String[] ary = { listHdg.get(i)[0], listHdg.get(i)[1], tmpStr.substring(0, tmpStr.indexOf("XQ") + 2) };
				listHdgCut.add(ary);
				continue;
			}

			else {
				String[] ary = { listHdg.get(i)[0], listHdg.get(i)[1], tmpStr };
				listHdgCut.add(ary);
				continue;
			}
		}

		String hdg = "";
		for (int i = 0; i < listHdgCut.size(); i++) {
			if (print7d) {
				System.out.println("listHdgCut=" + Arrays.toString(listHdgCut.get(i)));
			}
			hdg = listHdgCut.get(i)[2];
			txt = hdg;
			txtStopped = StopWords.removeDefinedTerms(goLawRemoveStopWordsAndContractNamesLegalStopWords(
					hdg.replaceAll("(Qx|XQ|SECTION|Section|[^a-zA-Z -])+", "").replace("/", "")).trim());
			txtStopped = StopWords.removeDefinedTerms(txtStopped);
//			System.out.println("hdg cut...==" + Arrays.toString(listHdgCut.get(i)) + " txtStopped====" + txtStopped
//					+ " SolrPrep.isItMostlyInitialCaps_measure(txt)===="
//					+ SolrPrep.isItMostlyInitialCaps_measure(txt.replaceAll("[\\.\\d]+", "")));

			if (hdg.replaceAll(
					"(?ism)(Qx|XQ|xxPD|xxOB|CBxx|xxOP|CPxx|\\(?[a-zA-Z\\d]\\.?\\)|[a-zA-Z\\d]\\.\\)?|[\\d\\.]+|This |The |\\(|\\)|article ?"
							+ "|(Q?x?NOW,? )?Q?x?THEREFOREX?Q? ?|Q?x?WHEREASX?Q?|Qx|XQ|(IN )?(WITNESS )?WHEREOF)+",
					"").replaceAll(" ", "").length() < 5
					&& nlp.getAllMatchedGroups(hdg,
							Pattern.compile("^((Qx)?\\(?[a-zA-Z\\d]\\.?\\)|[a-zA-Z\\d]{1,2}\\.\\)?)")).size() == 0

					&& nlp.getAllMatchedGroups(txt, Pattern.compile("^((Qx)?\\(?[\\d\\.]{2,7}.{1,15}XQ)")).size() == 0
			// if "Qx9.3 Taxes.XQ" or "Qx9.3 Fees.XQ" -- then ok

			) {
				if (print7d) {
					System.out.println("1aa.continue. txt=" + txt);
				}
				continue;
			}

//			Qx 7.1(h)(iii)XQ
			if (hdg.replaceAll("(Qx|XQ|\\(?[a-zA-Z\\.\\d]{1,5}\\)|\\d|\\.| )", "").length() < 4) {
				if (print7d) {
					System.out.println("1ac.continue.");
				}
				continue;
			}

			if (hdg.length() > 25 && nlp.getAllMatchedGroups(hdg, Pattern.compile("Qx.{2,55}XQ$")).size() > 0
					&& nlp.getAllMatchedGroups(hdg, Pattern.compile("Qx.{2,55}XQ$")).get(0).length() * 2 < hdg.length()
					&& nlp.getAllMatchedGroups(hdg, Pattern.compile("(?ism)reserve|delete|omitted|intentionally"))
							.size() == 0) {
				if (print7d) {
					System.out.println("2.continue.");
				}
				continue;
			}

//			if (hdg.replaceAll(ptmpInsert.toString(), "\\.").trim().contains(":")
//					&& nlp.getAllMatchedGroups(hdg, Pattern.compile(":XQ")).size() == 0) {
//				if (print7d) {
//					System.out.println("3.continue...... contains :");
//				}
//				continue;
//			}
//			System.out.println("hdg==" + hdg);
			String[] aryCon = isHeadingConsecutive(listHdgCut, i, 2);
//			System.out.println("aryCon=" + Arrays.toString(aryCon));
			if (!hdg.contains("Qx") && aryCon[0].contains("false") && !SolrPrep.isItMostlyInitialCaps(hdg)) {
				if (print7d) {
					System.out.println("4.continue");
				}
				continue;
			}
			if (!hdg.contains("Qx") && aryCon[0].contains("false") && hdg.length() > 75) {
				if (print7d) {
					System.out.println("4b.continue");
				}
				continue;
			}

			tmpStr = hdg;
			if (hdg.indexOf("consecutive=") > 0)
				tmpStr = hdg.substring(0, hdg.indexOf("consecutive=") - 12);

//			(a) QxSection 4.19(a)XQ
			if (tmpStr.replaceAll(
					"(Subject to |Section |SECTION |\\(?[a-zA-Z\\d]{1,5}[\\.\\)]{1,2}|[\\(\\)\\.\\d]+ |Qx|XQ| )+", "")
					.length() < 4
					&& nlp.getAllMatchedGroups(tmpStr, Pattern.compile("[A-Z]{1}[A-Za-z]+\\.")).size() == 0

					&& nlp.getAllMatchedGroups(tmpStr, Pattern.compile("^(Qx)?(Qx)?(Section|SECTION)  ?[\\d]"))
							.size() == 0) {
				if (print7d) {
					System.out.println("5. continue=" + tmpStr.replaceAll(
							"(Subject to |Section |SECTION |\\(?[a-zA-Z\\d]{1,5}[\\.\\)]{1,2}|[\\(\\)\\.\\d]+ |Qx|XQ| )+",
							"") + " tmpStr=" + tmpStr);
				}
				continue;
			}
			if (aryCon[0].contains("true") && aryCon[1].equals("pNmbNext=0")) {
				tmp = text.substring(Integer.parseInt(listHdgCut.get(i)[1]));
				tmp = tmp.substring(0, tmp.indexOf("\r\n"));
				eIdx = Integer.parseInt(listHdgCut.get(i)[1]) + tmp.length();
//				System.out
//						.println("az hdg=" + listHdgCut.get(i)[2].substring(0, Math.min(listHdgCut.get(i)[2].length(), 50)));

			} else {
//				System.out.println("is consec hdg test ==" + Arrays.toString(listHdgCut.get(i)) + " size=" + listHdgCut.size()
//						+ " i=" + i);
				if (i + 1 < listHdgCut.size()) {
					eIdx = Integer.parseInt(listHdgCut.get(i + 1)[0]) - 1;
				} else {
					eIdx = text.length();
				}
			}

			if (nlp.getAllIndexStartLocations(goLawRemoveStopWordsAndContractNamesLegalStopWords(listHdgCut.get(i)[2]),
					Pattern.compile("(^| )[a-z]{1}")).size() > 0 && listHdgCut.get(i)[2].contains("Qx")) {

				String[] ary = { listHdgCut.get(i)[0], eIdx + "",
						"hdg=" + listHdgCut.get(i)[2].replaceAll(ptmpInsert.toString(), "\\.").replaceAll("\r\n", " ")
								.replaceAll("[ ]+", " ").trim(),
						"consecutive=" + aryCon[0], aryCon[1] };
//				System.out.println("1. adding ary=" + Arrays.toString(ary));
				cntg = cntg + 0.000002;
				mapIdx.put(Double.parseDouble(listHdgCut.get(i)[0]) + cntg, ary);

			} else {
				String[] ary = { listHdgCut.get(i)[0], eIdx + "",
						"hdg=" + listHdgCut.get(i)[2].replaceAll(ptmpInsert.toString(), "\\.")
								.replaceAll("(?<=Qx) ?[\r\n]+", " ").replaceAll("[ ]+(?=XQ)", " ")
								.replaceAll("Qx[ ]+", "Qx ").replaceAll("[ ]+", " ").trim(),
						"consecutive=" + aryCon[0], aryCon[1] };
//				System.out.println("2 adding ary=" + Arrays.toString(ary));
				cntg = cntg + 0.000002;
				mapIdx.put(Double.parseDouble(listHdgCut.get(i)[0]) + cntg, ary);
			}
		}

		System.out.println("getHeadings part2 seconds=" + (System.currentTimeMillis() - startTime) / 1000);
		startTime = System.currentTimeMillis();

		TreeMap<Double, String[]> mapIdx2 = new TreeMap<Double, String[]>();
		TreeMap<String, Integer> mapDupCheck = new TreeMap<String, Integer>();
		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter(new File("c:/temp/mapDupCheck.txt"));
		}

		String hdgDup = "", sec = "";
		for (Map.Entry<Double, String[]> entry : mapIdx.entrySet()) {
			tmpStr = entry.getValue()[2];
			sec = "";
			if (tmpStr.contains("sec=")) {
				sec = tmpStr;// picks-up cases where section was previously captured in getSectionsMapped
//				System.out.println("sec.....=" + sec);
			}

			hdg = "";
			hdgDup = "";
			if (SolrPrep.isItMostlyInitialCaps_measure(tmpStr) < .5 && tmpStr.split(" ").length > 10
					&& nlp.getAllMatchedGroups(tmpStr, Pattern.compile("^.{0,19}Qx.{1,200}?XQ")).size() > 0) {
//				System.out.println("bef - ary==" + Arrays.toString(entry.getValue())+"\r\ntmpStr=="+tmpStr);
				hdg = nlp.getAllMatchedGroups(tmpStr, Pattern.compile("^.{0,19}Qx.{1,200}?XQ")).get(0);
				if (nlp.getAllMatchedGroups(hdg, Pattern.compile("Qx.{0,3000}XQ")).size() > 0) {
					hdgDup = nlp.getAllMatchedGroups(hdg, Pattern.compile("Qx.{0,3000}XQ")).get(0);
				}

				if (hdgDup.length() > 0 && mapDupCheck.containsKey(hdgDup)
						&& Math.abs(mapDupCheck.get(hdgDup) - Integer.parseInt(entry.getValue()[0])) < 6) {
					if (GetContracts.turnOnPrinterWriter) {
						pw.append("this is dupe=" + hdgDup + " idx=" + mapDupCheck.get(hdgDup) + "\r\n");
					}
//					System.out.println("this is dupe");
					continue;
				} else if (!mapDupCheck.containsKey(hdgDup)
						|| Math.abs(mapDupCheck.get(hdgDup) - Integer.parseInt(entry.getValue()[0])) > 6) {

					if (sec.length() > 0) {
						String[] ary = { entry.getValue()[0],
								(Integer.parseInt(entry.getValue()[0]) + hdg.length()) + "", sec };
//						System.out.println("mapIdx2....put sec="+sec);
						mapIdx2.put(entry.getKey(), ary);
					}

					else {
						String[] ary = { entry.getValue()[0],
								(Integer.parseInt(entry.getValue()[0]) + hdg.length()) + "", hdg };
//						System.out.println("mapIdx2....put hdg="+hdg);
						mapIdx2.put(entry.getKey(), ary);

					}

					if (nlp.getAllMatchedGroups(hdg, Pattern.compile("Qx.{0,3000}XQ")).size() > 0) {
						hdgDup = nlp.getAllMatchedGroups(hdg, Pattern.compile("Qx.{0,3000}XQ")).get(0);
						mapDupCheck.put(hdgDup, Integer.parseInt(entry.getValue()[0]));
						if (GetContracts.turnOnPrinterWriter) {
							pw.append("1.put=" + hdgDup + " int=" + entry.getValue()[0] + "\r\n");
						}
//						System.out.println("2. put =" + Arrays.toString(ary));
					}
				}
//				System.out.println("aft - ary==" + Arrays.toString(ary));
			} else {

				if (nlp.getAllMatchedGroups(entry.getValue()[2], Pattern.compile("Qx.{0,3000}XQ")).size() > 0) {
					hdgDup = nlp.getAllMatchedGroups(entry.getValue()[2], Pattern.compile("Qx.{0,3000}XQ")).get(0);
				}
				if (hdgDup.length() > 0 && mapDupCheck.containsKey(hdgDup)
						&& Math.abs(mapDupCheck.get(hdgDup) - Integer.parseInt(entry.getValue()[0])) < 6) {
//					System.out.println("2.continue. this is dupe..");
//					System.out.println("mapDupCheck val=" + mapDupCheck.get(hdgDup) + " hdgDup=" + hdgDup);
//					System.out.println("entry.getval=" + Arrays.toString(entry.getValue()) + " key=" + entry.getKey());
					mapIdx2.remove((double) mapDupCheck.get(hdgDup));
					mapIdx2.put(Double.parseDouble(entry.getValue()[0]), entry.getValue());
					continue;
				} else if (!mapDupCheck.containsKey(hdgDup)
						|| Math.abs(mapDupCheck.get(hdgDup) - Integer.parseInt(entry.getValue()[0])) > 6) {
					mapDupCheck.put(hdgDup, Integer.parseInt(entry.getValue()[0]));
					if (GetContracts.turnOnPrinterWriter) {
						pw.append("2...entry.getValue()put=" + hdgDup + " int=" + entry.getValue()[0] + "\r\n");
					}
//					System.out.println("2...put =" + Arrays.toString(entry.getValue()));

					if ((!SolrPrep.isItMostlyInitialCaps(entry.getValue()[2]) || entry.getValue()[2].length() > 200)
							&& nlp.getAllMatchedGroups(entry.getValue()[2],
									Pattern.compile("(?sm)^.{0,18}Qx.{1,500}?XQ")).size() > 0) {
						if (sec.length() > 0) {
							String[] ary = { entry.getValue()[0], entry.getValue()[1], sec };
							listHdg.add(ary);
							mapIdx2.put(entry.getKey(), ary);
//							System.out.println("mapIdx2....put sec="+sec);
						} else {
							hdg = nlp.getAllMatchedGroups(entry.getValue()[2], Pattern.compile("^.{0,18}Qx.{1,500}?XQ"))
									.get(0).trim();
							String[] ary = { entry.getValue()[0], entry.getValue()[1], hdg };
							listHdg.add(ary);
							mapIdx2.put(entry.getKey(), ary);
//							System.out.println("mapIdx2....put hdg="+hdg);
						}

//						System.out.println("last continue....");
						continue;
					}
					mapIdx2.put(entry.getKey(), entry.getValue());
				}
			}

		}

		String hdgPr = null, sIdxPr = null, eIdxPr = null;
		double k = 0, kP = 0;
		TreeMap<Double, String[]> mapIdx3 = new TreeMap<Double, String[]>();
		for (Map.Entry<Double, String[]> entry : mapSections.entrySet()) {
//			System.out.println("mapsections===" + Arrays.toString(entry.getValue()));
			if (entry.getValue()[2].contains("toc=")
					|| (entry.getValue()[0].equals(sIdxPr) && entry.getValue()[2].equals(hdgPr))) {
				continue;
			}
			if ((Math.abs(entry.getKey() - kP) < 7 && (hdgPr.contains("sec=") || entry.getValue()[2].contains("sec=")))
					&& hdgPr.replaceAll("(hdg=|sec=|\\.|XQ|Qx)", "").replaceAll(",? consecutive=.*?$", "")
							.equals(entry.getValue()[2].replaceAll("(hdg=|sec=|\\.|XQ|Qx)", "")
									.replaceAll(",? consecutive=.*?$", ""))) {
//				System.out.println("1.hdg=" + entry.getValue()[2] + " hdgPr=" + hdgPr + " k-Kp=" + (k - kP));
				if (hdgPr.contains("sec="))// don't overwrite it.
					continue;
				// if cur hdg contains "sec=" then remove the prior hdg
				mapIdx3.remove(kP);
			}

			mapIdx3.put(entry.getKey(), entry.getValue());
			sIdxPr = entry.getValue()[0];
			hdgPr = entry.getValue()[2];
			kP = entry.getKey();
		}

		mapSections = new TreeMap<Double, String[]>();

		for (Map.Entry<Double, String[]> entry : mapIdx2.entrySet()) {
//			System.out.println("mapIdx2......" + Arrays.toString(entry.getValue()));
			if (entry.getValue()[2].contains("toc=")
					|| (entry.getValue()[0].equals(sIdxPr) && entry.getValue()[2].equals(hdgPr))) {
				continue;
			}
			if (null != sIdxPr && eIdxPr != null && hdgPr != null && sIdxPr.equals(entry.getValue()[0])
					&& eIdxPr.equals(entry.getValue()[1])
					&& entry.getValue()[2].replaceAll("(hdg=|sec=|\\.|XQ|Qx)", "").replaceAll(",? consecutive=.*?$", "")
							.equals(hdgPr.replaceAll("(hdg=|sec=|\\.|XQ|Qx)", "").replaceAll(",? consecutive=.*?$",
									""))) {
				sIdxPr = entry.getValue()[0];
				eIdxPr = entry.getValue()[1];
				hdgPr = entry.getValue()[2];
				kP = entry.getKey();
				continue;
			}
			if ((Math.abs(entry.getKey() - kP) < 7 && (hdgPr.contains("sec=") || entry.getValue()[2].contains("sec=")))
					&& hdgPr.replaceAll("(hdg=|sec=|\\.|XQ|Qx)", "").replaceAll(",? consecutive=.*?$", "")
							.equals(entry.getValue()[2].replaceAll("(hdg=|sec=|\\.|XQ|Qx)", "")
									.replaceAll(",? consecutive=.*?$", ""))) {
//				System.out.println("2.hdg=" + entry.getValue()[2] + " hdgPr=" + hdgPr + " k-Kp=" + (k - kP));
				if (hdgPr.contains("sec="))// don't overwrite it.
					continue;

				// if cur hdg contains "sec=" then remove the prior hdg
				mapIdx3.remove(kP);
			}

			mapIdx3.put(entry.getKey(), entry.getValue());
			sIdxPr = entry.getValue()[0];
			eIdxPr = entry.getValue()[1];
			hdgPr = entry.getValue()[2];
			kP = entry.getKey();
		}

		if (GetContracts.turnOnPrinterWriter) {
			pw.close();
		}
//		NLP.printMapDblStrAry("mapIdx3...=", mapIdx3);

		System.out.println("getHeadings part3 seconds=" + (System.currentTimeMillis() - startTime) / 1000);

//		System.out.println("mapIdx.size==" + mapIdx.size());
		return mapIdx3;

	}

	public List<String[]> scrubMapIdx(String text, TreeMap<Double, String[]> mapIdx, boolean parsingDisclosure,
			TreeMap<String, Integer[]> mapDefStr_sIdx_eIdx) throws NumberFormatException, IOException {

		/*
		 * NOTE Do NOT touch text - length must not chg! No search and replace - length
		 * cannot chg! Or everything gets corrupted.
		 */

		NLP nlp = new NLP();

//		NLP.printMapDblStrAry("mapIdx=", mapIdx);
		TreeMap<Integer, String[]> mapIdxInt = new TreeMap<Integer, String[]>();
		int cnt = 0;
		String sIdxStr = "", eSp = "";

		// if hdg does not contain Qx its eIdx and sIdx are correct.

		for (Map.Entry<Double, String[]> entry : mapIdx.entrySet()) {
			sIdxStr = entry.getKey() + "";
			sIdxStr = sIdxStr.substring(0, sIdxStr.indexOf("."));
//			System.out.println("sIdxStr=" + sIdxStr);
			String[] ary = { sIdxStr, Arrays.toString(entry.getValue()).replaceAll("\\[?" + sIdxStr + ",", "")
					.replaceAll("(?sm)[\\[\\]\\|]+", " ") };
//			System.out.println("a.put cnt=key, mapIdxInt=" + Arrays.toString(ary));
			mapIdxInt.put(cnt, ary);
			cnt++;
		}

//		NLP.printMapIntStringAry("mapIdxInt=...", mapIdxInt);
		TreeMap<Integer, String[]> mapIdxFinal = new TreeMap<Integer, String[]>();
		cnt = 0;
		int sIdx, sIdxPrior = 0, sIdxNext = 0, eIdx, eIdxNext = 0, eIdxPrior = 0;
		String str, strPrior = "", strNext = "", tmp = "";
		for (Map.Entry<Integer, String[]> entry : mapIdxInt.entrySet()) {
//			System.out.println("mapIdxInt ary=" + Arrays.toString(entry.getValue()));
//			System.out.println("repl===" + entry.getValue()[1].replaceAll("^.*?(hdg|sec|def|mHd|sub|exh)=", "").trim());
			if (entry.getValue()[1].replaceAll("^.*?(hdg|sec|def|mHd|sub|exh)=", "").trim().length() < 1) {
//				System.out.println("continue. no hdg");
				continue;
			}
			tmp = Arrays.toString(mapIdxInt.get(cnt + 1)).substring(1);
			tmp = tmp.substring(0, tmp.length() - 1);

//			System.out.println("scrubMapIdx...==" + tmp);
			if (nlp.getAllMatchedGroups(tmp, Pattern.compile("(?<=\\[[\\d]+, ? ?)[\\d]+")).size() > 0) {
//				System.out.println("scrubMapIdx continue..." + tmp);
//				System.out.println("[\\d]+\")).get(0)=="
//						+ nlp.getAllMatchedGroups(tmp, Pattern.compile("(?<=\\[[\\d]+, ? ?)[\\d]+")).get(0));
				continue;// -- this screws up parsing of sec/def/exh/hdg/mHd/sub if you do not remove
							// array [
			}
			if (cnt + 1 < mapIdxInt.size() && nlp.getAllMatchedGroups(Arrays.toString(mapIdxInt.get(cnt + 1)),
					Pattern.compile("(?<=\\[[\\d]+, ? ?)[\\d]+")).size() > 0) {
//				System.out.println("1. scrubMapIdx ary=" + Arrays.toString(mapIdxInt.get(cnt + 1)));
				sIdxNext = Integer.parseInt(nlp
						.getAllMatchedGroups(Arrays.toString(mapIdxInt.get(cnt + 1)), Pattern.compile("(?<=\\[)[\\d]+"))
						.get(0));
				eIdxNext = Integer.parseInt(nlp.getAllMatchedGroups(Arrays.toString(mapIdxInt.get(cnt + 1)),
						Pattern.compile("(?<=\\[[\\d]+, ? ?)[\\d]+")).get(0));
//				System.out.println("eIdxNext=" + eIdxNext);
				strNext = Arrays.toString(mapIdxInt.get(cnt + 1));
//				eSpNext = strNext.toLowerCase().split(" ")[1].trim();
			}
			if (cnt > 0 && nlp.getAllMatchedGroups(Arrays.toString(mapIdxInt.get(cnt + 1)),
					Pattern.compile("(?<=\\[[\\d]+, ? ?)[\\d]+")).size() > 0) {
//				System.out.println("2. scrubMapIdx ary=" + Arrays.toString(mapIdxInt.get(cnt + 1)));
				sIdxPrior = Integer.parseInt(nlp
						.getAllMatchedGroups(Arrays.toString(mapIdxInt.get(cnt - 1)), Pattern.compile("(?<=\\[)[\\d]+"))
						.get(0));
//				System.out.println("sIdxPrior=" + sIdxPrior);
				eIdxPrior = Integer.parseInt(nlp.getAllMatchedGroups(Arrays.toString(mapIdxInt.get(cnt - 1)),
						Pattern.compile("(?<=\\[[\\d]+,  ?)[\\d]+")).get(0));
//				System.out.println("eIdxPrior=" + eIdxPrior);
				strPrior = Arrays.toString(mapIdxInt.get(cnt - 1));
//				eSpPrior = strPrior.toLowerCase().split(" ")[1].trim();
			}

			sIdx = Integer.parseInt(
					nlp.getAllMatchedGroups(Arrays.toString(mapIdxInt.get(cnt)), Pattern.compile("(?<=\\[)[\\d]+"))
							.get(0));
//			System.out.println("sIdx=" + sIdx);

			eIdx = Integer.parseInt(nlp.getAllMatchedGroups(Arrays.toString(mapIdxInt.get(cnt)),
					Pattern.compile("(?<=\\[[\\d]+,  ?)[\\d]+")).get(0));
//			System.out.println("eIdx=" + eIdx);
			str = Arrays.toString(mapIdxInt.get(cnt));
			eSp = str.toLowerCase().split(" ")[2].trim();
			// if prior array doesn't have valid=y - then insert
			// if next array doesn't have valid=y - then insert
			// if prior has valid=y but distance is greater than 500 insert so long as next
			// is 500 away or does not have valid
//			System.out.println("eSp==" + eSp);
			if (!strPrior.toLowerCase().contains(eSp) && !strNext.toLowerCase().contains(eSp)
					&& str.contains("valid=Maybe")
					&& ((sIdx - sIdxPrior > 500 && eIdx - eIdxPrior > 500)
							|| (!strPrior.contains("valid=y") && !strPrior.contains("true")))
					&& ((sIdxNext - sIdx > 500 && eIdxNext - eIdx > 500)
							|| (!strNext.contains("valid=y")) && !strNext.contains("true"))) {
//				System.out.println("maybe = = sIdxPrior=" + sIdxPrior + " sIdx=" + sIdx + " sIdxNext=" + sIdxNext
//						+ "\r\nstr=" + str + "\r\nstrPrior=" + strPrior + "\r\nstrNext=" + strNext);
//				System.out.println("entry.getValue=" + entry.getValue()[0] + " | | | " + entry.getValue()[1]);

				String[] ary = { entry.getValue()[0],
						entry.getValue()[1]
								.replaceAll("(?sm)([\\d]+, )(.{1,150}?)(, valid=Maybe)", "$1hdg=$2, hdg=tocConfirmed=Y")
								.trim().replaceAll("[ ]+", " ") };
//				System.out.println("11.added ary...=" + Arrays.toString(ary) + " ary.len===" + entry.getValue().length);
				mapIdxFinal.put(Integer.parseInt(entry.getValue()[0]), ary);
			} else if (!str.contains("valid=Maybe")) {
				mapIdxFinal.put(Integer.parseInt(entry.getValue()[0]), entry.getValue());
//				System.out.println("12.added ary...=" + Arrays.toString(entry.getValue()) + " ary.len==="
//						+ entry.getValue().length + " ary1=" + entry.getValue()[0] + " ary2=" + entry.getValue()[1]);
			}
			cnt++;
		}

//		System.out.println("mapIdxFinal.size==" + mapIdxFinal.size());
//		NLP.printMapIntStringAry("kind of mapIdxFinal=", mapIdxFinal);
		str = "";
		sIdxStr = "";
		String eIdxStr = "";
		String hdg = "", hdgPrior = "";
		// format map

//		TreeMap<Integer, String[]> mapIdxFormat = new TreeMap<Integer, String[]>();

		String key = "";
		cnt = 0;
		eIdxPrior = -10;
		eIdx = 0;
		sIdxPrior = -11;
		sIdx = 0;
		List<String[]> listTmp = new ArrayList<String[]>();

		for (Map.Entry<Integer, String[]> entry : mapIdxFinal.entrySet()) {
//			System.out.println("2. mapIdxInt ary=" + Arrays.toString(entry.getValue()));
			String[] ary = entry.getValue();
			eIdxStr = "";
			sIdxStr = "";
			hdg = "";
			sIdx = 0;
			eIdx = 0;
			for (int c = 0; c < ary.length; c++) {
//				System.out.println("final loop at scrubidx ary=" + ary[c].trim());
				str = ary[c].trim();
//				System.out.println("str===" + str);
				if (c == 0) {
					sIdxStr = str;
					continue;
				}

				if (nlp.getAllMatchedGroups(str, Pattern.compile("(?sm)(?<=[\\d]+, )([\\d]+)(?=, (def|exh|exhToc)=)"))
						.size() > 0) {
					eIdxStr = nlp.getAllMatchedGroups(str,
							Pattern.compile("(?sm)(?<=[\\d]+, )([\\d]+)(?=, (def|exh|exhToc)=)")).get(0);
//					System.out.println("1 eIdxStr=" + eIdxStr);
					hdg = str.substring(str.indexOf(eIdxStr + ",") + eIdxStr.length() + 2).trim();
				} else if (nlp.getAllMatchedGroups(str, Pattern.compile("(?sm)^([\\d]+)(?=,)")).size() > 0) {
					eIdxStr = nlp.getAllMatchedGroups(str, Pattern.compile("(?sm)^([\\d]+)(?=,)")).get(0);
					hdg = str.substring(str.indexOf(eIdxStr + ",") + eIdxStr.length() + 1).trim();
//					System.out.println("2 eIdxStr=" + eIdxStr);
				}
			}

			if (eIdxStr.replaceAll("[^\\d]", "").length() == 0 || sIdxStr.replaceAll("[^\\d]", "").length() == 0) {
//				System.out.println("2..continue...");
				continue;
			}

			sIdx = Integer.parseInt(sIdxStr);
			eIdx = Integer.parseInt(eIdxStr);
//			System.out.println(
//					"sIdxStr=" + sIdxStr + " eIdxStr=" + eIdxStr + " eIdx-sIdx=" + (eIdx - sIdx) + " hdg=" + hdg);
			if (eIdx - sIdx < 0 || (hdg.contains("exh=") && eIdx - sIdx < 50)
					|| nlp.getAllMatchedGroups(hdg, Pattern.compile("(Exhibit|Schedule|Annex|Appendix) [a-zA-Z\\d]\\."))
							.size() > 0
					|| hdg.replaceAll("(sec|hdg|exh|exhToc|toc|def)=", "").length() == 0) {
//				System.out.println("3.continue....");
				continue;
			}

//			.replaceAll("Qx|XQ|[^a-zA-Z ]+", "")SAME REPL AS WHEN MAP IS CREATED.
			key = hdg.replaceAll("Qx|XQ|[^a-zA-Z= ]+|(hdg|sec|mHd|sub)=", "").trim();
			key = key.replaceAll("(.*?)(consecutive=.*?$)", "$1").trim();
//			System.out.println("is this key==" + key + " hdg=" + hdg);
			if (mapDefStr_sIdx_eIdx.containsKey(key)
					&& Math.abs(mapDefStr_sIdx_eIdx.get(key)[0] - Integer.parseInt(sIdxStr)) < 10
					&& !key.contains("def=")) {
//				System.out.println("hdg to continue past==" + hdg + " and the key is==" + key);
				continue;
			}

			String[] aryVal = { sIdxStr, eIdx + "", hdg };
			listTmp.add(aryVal);
//			System.out.println(".add final at scrubIdx="+Arrays.toString(aryVal));

			eIdxPrior = eIdx;
			sIdxPrior = sIdx;
			hdgPrior = hdg;
			cnt++;
		}

//		NLP.printListOfStringArray("listTmp==..", listTmp);// still good with non-Qx hdgs
//		System.out.println("listTmp.size==" + listTmp.size());
		List<String[]> listScrubIdxFinal = new ArrayList<String[]>();

		for (int i = 0; i < listTmp.size(); i++) {
			if (nlp.getAllMatchedGroups(listTmp.get(i)[2], Pattern.compile("(hdg|mHd|sub)=(Qx)?(SECTION|Section)"))
					.size() > 0) {

				String[] ary = { listTmp.get(i)[0], listTmp.get(i)[1],
						listTmp.get(i)[2].replaceAll("(hdg|mHd|sub)=", "sec=") };
				listScrubIdxFinal.add(ary);
			} else {
				listScrubIdxFinal.add(listTmp.get(i));
			}
		}

		return listScrubIdxFinal;

	}

	public List<String[]> removePageHeaders(List<String[]> list) {

		NLP nlp = new NLP();
		TreeMap<Integer, String[]> mapHdg = new TreeMap<Integer, String[]>();
		String str = "";
		int c = 0;
//		System.out.println("list ----.size=" + list.size());
		for (int i = 0; i < list.size(); i++) {
			// also using the opportunity to scrub instances where there's dangling extra
			// after first end qx.
			if (nlp.getAllIndexEndLocations(list.get(i)[2], Pattern.compile("XQ(?= ?[A-Z]{1})")).size() > 0) {
				str = list.get(i)[2];
				c = nlp.getAllIndexEndLocations(list.get(i)[2], Pattern.compile("XQ(?= ?[A-Z]{1})")).get(0);

				String[] ary = { list.get(i)[0], list.get(i)[1], str.substring(0, c) };
				mapHdg.put(i, ary);

			} else {
				mapHdg.put(i, list.get(i));
			}
		}
//		System.out.println("removePageHeaders map.size==" + mapHdg.size());
		TreeMap<String, List<Integer>> mapFindDuplicatesLikelyPageHeader = new TreeMap<String, List<Integer>>();
		String hdgPortion = "";
		for (Map.Entry<Integer, String[]> entry : mapHdg.entrySet()) {
			/*
			 * this looks for headers that have repeated many times. And if they contain
			 * certain words they are removed from hdgs list b/c they are likely header info
			 * in terms of a repeating header in the header of the doc or footer of the doc
			 */

			/*
			 * to be considered a hdg it must have these words. This can be expanded of
			 * course to remove other common headers.
			 */

			List<Integer> listI = new ArrayList<Integer>();
//			System.out.println("1. entry.getValue()[2]=="+entry.getValue()[2]);
			if (nlp.getAllIndexEndLocations(entry.getValue()[2], Pattern.compile("(?ism)trust|fund")).size() > 0) {
//				System.out.println("2. entry.getVal[2]=" + entry.getValue()[2]);
				String[] hdgPortionSplit = entry.getValue()[2].replaceAll("(?sm)(Qx|XQ|hdg=|sub=|mHd=)+", " ")
						.replaceAll("([ ]+|[\r\n]+)+", " ").trim().split(" ");
//				System.out.println("1. hdgPortionSplit.len=" + hdgPortionSplit.length + " hdgPortionSplit="
//						+ Arrays.toString(hdgPortionSplit));
				hdgPortion = "";
				for (int n = 0; n < Math.min(3, hdgPortionSplit.length); n++) {
//					System.out.println("2. hdgPortion=" + hdgPortion);
					hdgPortion = hdgPortion + " " + hdgPortionSplit[n].trim();
					hdgPortion.replaceAll("([ ]+|[\r\n]+)+", "").trim();
					// List<Integer>.size reflects number of repeating instances. and each integer
					// in the list is the key to remove should the list be greater >
				}
//				System.out.println("4. hdgPortion=" + hdgPortion);
				if (mapFindDuplicatesLikelyPageHeader.containsKey(hdgPortion)) {
//					System.out.println("3. hdgPortion=" + hdgPortion);
					listI = new ArrayList<Integer>();
					listI = mapFindDuplicatesLikelyPageHeader.get(hdgPortion);
					listI.add(entry.getKey());
					mapFindDuplicatesLikelyPageHeader.put(hdgPortion, listI);
				} else {
					listI = new ArrayList<Integer>();
					listI.add(entry.getKey());
					mapFindDuplicatesLikelyPageHeader.put(hdgPortion, listI);
				}
			}
		}

		for (Map.Entry<String, List<Integer>> entry : mapFindDuplicatesLikelyPageHeader.entrySet()) {
			/*
			 * if there are 3 headers that are the same (subject to header contain pattern
			 * above) it is removed b/c they are likely page headers (eg in header margins
			 * of footer or header).
			 */
			if (entry.getValue().size() > 2) {
//				System.out.println("entry.getKey()===" + entry.getKey());
				if (entry.getValue().size() > 2) {
					for (int i = 0; i < entry.getValue().size(); i++) {
						mapHdg.remove(entry.getValue().get(i));
					}
				}
			}
		}

//		TreeMap<Integer, String[]> mapHdg = new TreeMap<Integer, String[]>();

		list = new ArrayList<String[]>();
		for (Map.Entry<Integer, String[]> entry : mapHdg.entrySet()) {
			list.add(entry.getValue());
		}

		return list;
	}

	public static String goLawGetPartsOfSpeech(String text) throws IOException {

		NLP nlp = new NLP();

		Properties props = new Properties();
		props.setProperty("annotators", "tokenize" + ", ssplit" + ", pos"
		// + ", lemma"
		// +", parse"
		// +", sentiment"
		// //, dcoref, ner"
		);

//		System.out.println("stanford?");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		Annotation annotation = new Annotation(text);
		// run all the selected Annotators on this text
		pipeline.annotate(annotation);
		// this prints out the results of sentence analysis to file(s) in good formats
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		pipeline.prettyPrint(annotation, baos);
		// String print = baos.toString();

		baos.reset();
		pipeline.xmlPrint(annotation, baos);
		String xmlPOS = baos.toString();

		List<String> listPOS = nlp.getAllMatchedGroups(xmlPOS, Pattern.compile("(?sm)(?<=<POS>).*?(?=</POS>)"));
		List<String> listWord = nlp.getAllMatchedGroups(xmlPOS, Pattern.compile("(?sm)(?<=<word>).*?(?=</word>)"));
//		List<String[]> listPosWord = new ArrayList<>();
		String pos, word;

		// System.out.println("xmlPOS.len="+xmlPOS.length()+"
		// listPOS.size="+listPOS.size());
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < listPOS.size(); i++) {
			pos = listPOS.get(i);
			word = listWord.get(i);
			// if (nlp.getAllIndexStartLocations(pos,
			// Pattern.compile("VB|CC|FW|JJ|LS")).size()>0) {

			sb.append("|" + word + " " + pos);
			// System.out.println("word=" + word + " POS=" + pos);

			// }
		}

		sb.append("|");

		return sb.toString();
	}

	public static String goLawRemoveDuplicateWords(String text) {

		TreeMap<String, String> map = new TreeMap<String, String>();
		String[] ary = text.split(" ");
		text = "";
		String word = "";
		for (int i = 0; i < ary.length; i++) {

			word = ary[i]
//					.replaceAll("(y|ies|s)$", "")
			;
			// responsibilities
			if (map.containsKey(word))
				continue;
			map.put(word, word);
			text = text + word + " ";
		}

		return text.replaceAll("[ ]+", " ").trim();
	}

	public static String goLawAlphabetizeWords(String text) {

		String[] textSplit = text.split(" ");
		List<String> listOfWordsOfTerm = new ArrayList<String>();

		for (int i = 0; i < textSplit.length; i++) {
			listOfWordsOfTerm.add(textSplit[i]);
		}

		Collections.sort(listOfWordsOfTerm.subList(1, listOfWordsOfTerm.size()));

		List<String> list = listOfWordsOfTerm.subList(1, listOfWordsOfTerm.size());
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			sb.append(list.get(i) + " ");
		}

//		System.out.println("text="+sb.toString());
		return sb.toString();
	}

	public static String goLawGetVerbs(String text) throws IOException {
//		System.out.println(text);
		text = goLawGetPartsOfSpeech(text);
//		System.out.println("POS=" + text);
		text = text.replaceAll("\\|[0-9A-Za-z,\\-\\$\\'\\.;\\_\\:]+ [A-UW-Z\\-\\.,]+", "");
		text = text.replaceAll("\\|[A-Z]+[a-z]+ .*?(?=\\|)", "").replaceAll("; |: ", "").trim();
		text = text.replaceAll(" V.{1,10}\\||\\|", " ").replaceAll("; |: ", "").trim();

		return text;
	}

	public TreeMap<Integer, String[]> getSubHeadingsFinalScrub(String text, List<String[]> listIdx,
			boolean parsingDisclosure) throws IOException {
		/*
		 * NOTE Do NOT touch text - length must not chg! No search and replace - length
		 * cannot chg! Or everything gets corrupted.
		 */

//		System.out.println("master listIdx.size=" + listIdx.size());

		NLP nlp = new NLP();
		List<String[]> listPrepIdx = new ArrayList<String[]>();
		String str = "";

		// if str has 'hdg=' && consec=true && pNmbNext!=0 && has alpha numeral of this
		// type \\d\\.\\d?\\d? or A-Z\\. then it is a mHdg. and ck if next is of type
		// \\(?[a-z\\d]\\.?\\) and if so that is a subHdg. Eg mHdg=14. Right to Audit
		// and hdg=(a) Inspection. First I remove all that are not potential mHdg

		TreeMap<Integer, String[]> mapIdx = new TreeMap<Integer, String[]>();
		TreeMap<Integer, String[]> mapHdgs = new TreeMap<Integer, String[]>();
		Pattern patternHdgNmb = Pattern.compile("hdg=[\\d]{1,3}\\.[\\d]{0,3} ");
		int cnt = 0;
		// separating potential hdgs from sub hdgs
		for (int i = 0; i < listIdx.size(); i++) {
//			System.out.println("uncut str="+listIdx.get(i)[2]);
			str = listIdx.get(i)[2].trim();
			str = str.substring(0, Math.min(str.length(), 12));
			if (nlp.getAllMatchedGroups(str, patternHdgNmb).size() > 0) {
//				System.out.println("hdg? #=" + str);
				mapHdgs.put(cnt++, listIdx.get(i));// can always add i to ary to match with below
				continue;
			}

//			if (nlp.getAllMatchedGroups(str, Pattern.compile("hdg=\\(?[a-zA-Z\\d]{1,4}\\.?\\)")).size() > 0) {
			mapIdx.put(i, listIdx.get(i));
		}

		List<Integer[]> listPnmb = new ArrayList<Integer[]>();
//		List<Integer[]> listPnmbPrev = new ArrayList<Integer[]>();
		List<Integer[]> listPnmbNext = new ArrayList<Integer[]>();
		cnt = 0;

		//
		String hdg = "", subHdg = "", consec = "", pNmbStr, pNmbNextStr = "", sIdxNextStr = null;
		int pNmb = 0, pNmb2 = 0, pNmbNext = 0, pNmbNext2 = 0;

		// now i validate they are successive numbered, even where pNmbNext=0 (which
		// would otherwise signify bad hdg - but if confirmed consecutive it is good )
		TreeMap<Integer, String[]> mapSuccessive = new TreeMap<Integer, String[]>();
		for (Map.Entry<Integer, String[]> entry : mapHdgs.entrySet()) {
			pNmb = -3;
			pNmb2 = -7;
			pNmbNext = -11;
			pNmbNext2 = -15;
			sIdxNextStr = "";
			listPnmb = new ArrayList<Integer[]>();
//			listPnmbPrev = new ArrayList<Integer[]>();
			listPnmbNext = new ArrayList<Integer[]>();
			// if consec=false, continue. pNmbNext=0 it is possible it is correct or
			// incorrect. check by subtracting next pNmbNext value from current.

			if (nlp.getAllMatchedGroups(mapHdgs.get(cnt)[2], patternHdgNmb).size() > 0) {
				pNmbStr = nlp.getAllMatchedGroups(mapHdgs.get(cnt)[2], patternHdgNmb).get(0).replaceAll("hdg=", "")
						.trim();
				listPnmb = convertAlphaParaNumb(pNmbStr, -1);
//				System.out.println("pNmb=" + pNmbStr + " listPnmb.size=" + listPnmb.size());
				if (listPnmb.size() == 1) {
					pNmb = listPnmb.get(0)[0];
				}
				if (listPnmb.size() > 1) {
					pNmb2 = listPnmb.get(1)[0];
				}
			}

			// if pNmbNex=0 get by converting hdg value. grab hdg pattern value and convert
			// it to value.
			if (cnt + 1 < mapHdgs.size()) {
				sIdxNextStr = mapHdgs.get(cnt + 1)[0];
//				System.out.println("this is map val[2]=" + mapHdgs.get(cnt + 1)[2]);
				if (nlp.getAllMatchedGroups(mapHdgs.get(cnt + 1)[2], patternHdgNmb).size() > 0) {
					pNmbNextStr = nlp.getAllMatchedGroups(mapHdgs.get(cnt + 1)[2], patternHdgNmb).get(0)
							.replaceAll("hdg=", "").trim();
					listPnmbNext = convertAlphaParaNumb(pNmbNextStr, -1);
//					System.out.println("pNmbNext=" + pNmbNext + " listPnmbNext.size=" + listPnmbNext.size());
					if (listPnmbNext.size() == 1) {
						pNmbNext = listPnmbNext.get(0)[0];
					}
					if (listPnmbNext.size() > 1) {
						pNmbNext2 = listPnmbNext.get(1)[0];
					}
				}
			}

			if (!entry.getValue()[2].contains("false") && (pNmbNext - pNmb == 1 || pNmbNext2 - pNmb2 == 1)) {
				String[] ary = { entry.getValue()[0], (Integer.parseInt(sIdxNextStr) - 1) + "",
						entry.getValue()[2].replaceAll("hdg=", "mHd=") };
				mapSuccessive.put(Integer.parseInt(entry.getValue()[0]), ary);
			} else {
				String[] ary = { entry.getValue()[0], entry.getValue()[1],
						entry.getValue()[2].replaceAll("hdg=", "mHd=") };
				mapSuccessive.put(Integer.parseInt(entry.getValue()[0]), ary);
			}
			cnt++;
		}

		// add back the hdgs that were not potentially mHdgs - eg the (a) Right of....
		for (Map.Entry<Integer, String[]> entry : mapIdx.entrySet()) {
			mapSuccessive.put(Integer.parseInt(entry.getValue()[0]), entry.getValue());
		}

//		System.out.println("2. mapSuccessive.size=" + mapSuccessive.size());
//		NLP.printMapIntStringAry("mapSuccessive==", mapSuccessive);

		int sIdx = 0, eIdx = 0, eIdxPr = -1000, sIdxM = -1, eIdxM = 1;
		String mHd = "";
		cnt = 0;

		for (Map.Entry<Integer, String[]> entry : mapSuccessive.entrySet()) {
			sIdx = Integer.parseInt(entry.getValue()[0]);
			eIdx = Integer.parseInt(entry.getValue()[1]);
			hdg = entry.getValue()[2];

//			System.out.println("hdg snip==" + hdg.substring(0, Math.min(hdg.length(), 100)));

//			if (sIdx - eIdxPr < 50 && nlp.getAllIndexEndLocations(hdg, Pattern.compile("(def|sec|exh)=")).size() == 0) {
//				System.out.println("1. continue hdg=");
//				continue;
//			}

			if (entry.getValue()[2].contains("mHd=")) {
				mHd = hdg;
				sIdxM = sIdx;
				eIdxM = eIdx;
				listPrepIdx.add(entry.getValue());
				eIdxPr = eIdx;
//				System.out.println("1a.continue.mHd=" + mHd);
				continue;
			}

			if (nlp.getAllIndexEndLocations(Arrays.toString(entry.getValue()),
					Pattern.compile("ADDRESS[e]{0,2}|Address[e]{0,2}|AVENUE|Avenue|FORM |Form |FLOOR|Floor|NONE|None"
							+ "|OFFICE |Office |REGISTRATION STATEMENT |Securities and | SECURITIES AND |TABLE OF "
							+ "|Table Of " + "|^\\=[a-z]{1}" + ""))
					.size() > 0
					&& nlp.getAllIndexEndLocations(Arrays.toString(entry.getValue()), Pattern.compile("false"))
							.size() > 0
					&& nlp.getAllIndexEndLocations(Arrays.toString(entry.getValue()), Pattern.compile("pNmbNext=0"))
							.size() > 0) {
				eIdxPr = eIdx;
//				System.out.println("2. continue==");
				continue;
			}

			if ((nlp.getAllIndexEndLocations(Arrays.toString(entry.getValue()), Pattern.compile("toc=")).size() > 0
					&& entry.getValue()[entry.getValue().length - 1].length() > 60
					&& entry.getValue()[entry.getValue().length - 1].replaceAll("[^A-Z]", "").split(" ").length < 3)
					|| nlp.getAllIndexEndLocations(Arrays.toString(entry.getValue()),
							Pattern.compile("TABLE OF |Table [Oo]{1}f |toc=[\\d\\.\\$,\\%]+, confirmed"
									+ "|hdg=SECURITIES AND EXCH|hdg=Securities and Exch"))
							.size() > 0) {
				eIdxPr = eIdx;
//				System.out.println("3. continue hdg=");
				continue;
			}

			if (sIdx >= sIdxM && eIdx <= eIdxM) {
				String[] ary = { entry.getValue()[0], entry.getValue()[1], entry.getValue()[2], sIdxM + "", eIdxM + "",
						mHd };
				eIdxPr = eIdx;
				listPrepIdx.add(ary);
			}

			else {
				eIdxPr = eIdx;
				listPrepIdx.add(entry.getValue());
			}

		}

//		System.out.println("bef listFinal.size=" + listPrepIdx.size());
		for (int i = 0; i < listIdx.size(); i++) {
			if (Arrays.toString(listIdx.get(i)).contains("toc=")) {
				listPrepIdx.add(listIdx.get(i));
			}
		}

//		TreeMap<Integer, String[]> mapFinalIdx = new TreeMap<Integer, String[]>();
//		for (int i = 0; i < listPrepIdx.size(); i++) {
//			if (!mapSuccessive.containsKey(Integer.parseInt(listPrepIdx.get(i)[0]))) {
//				mapFinalIdx.put(Integer.parseInt(listPrepIdx.get(i)[0]), listPrepIdx.get(i));
//			}
//		}

		TreeMap<Integer, String[]> mapCk = new TreeMap<Integer, String[]>();
		hdg = "";
		String hdgPrior = "", hdgCt = "";
		int hIdx = 0;
		cnt = 0;
		// remove where current hdg is in priorHdg.
		for (Map.Entry<Integer, String[]> entry : mapSuccessive.entrySet()) {
			hdg = entry.getValue()[2];

//			System.out.println("aa.hdg snip==" + hdg.substring(0, Math.min(hdg.length(), 150)));

//			if (nlp.getAllIndexEndLocations(hdg,
//					Pattern.compile("hdg=(\\([a-z\\d]{1,4}\\)" + "|[\\d]{1,3})? ?[a-z]{1}([^\\.\\)])"// |[^xvi]{1,3}[^\\)\\.]
//							+ "|hdg=.{1,4}(Deleted?|DELETED?|Reserved?|RESERVED?)"))
//					.size() > 0) {
//				System.out.println("4. hdg continue. hdg="+hdg);
//				continue;
//			}
			hdgCt = hdg;
			if (hdg.indexOf("consecutive=") > 0) {
				hIdx = nlp.getAllIndexStartLocations(hdgCt, Pattern.compile(",? ?consecutive=")).get(0);
				hdgCt = hdg.substring(0, hIdx).replaceAll("hdg=", "");
//				System.out.println("hdgPrior=" + hdgPrior);
//				System.out.println("cur=" + hdgCt);
			}

			if (hdgPrior.contains(hdgCt)) {
//				System.out.println("5. hdg continue");
				continue;
			}

			hdgPrior = hdg;
			mapCk.put(cnt, entry.getValue());
			cnt++;
		}

		// if a eIdx stretches past sIdx of next - and it isn't alread appended, then
		// append it.
		Pattern patternPara = Pattern.compile("[^\r\n].*?[\r\n]|[\r\n]+.*?\r\n");
		String para = "", para2 = "";
		int sIdxNext = 0;
		cnt = 0;
		sIdx = 0;
		int hdgLen = 0;
		int dist = 100;
		if (parsingDisclosure) {
			dist = 70;
		}

		str = "";
		int hardReturns = 0;
		TreeMap<Integer, String[]> mapClean = new TreeMap<Integer, String[]>();

		// when a hdg is not encompassing any text (sIdx,eIdx is the hdg) then I change
		// eIdx to equal the end of the first text paragraph subject to rules that
		// exclude the heading. This can't be applied generally b/c it's exclusion rules
		// are overbroad which is okay in the context of these hdgs as they are
		// non-consecutive (b/c of their low sIdx,eIdx dist).

		eIdx = 0;
//		int eIdxNext = 0;
		boolean inMap = false;
		for (Map.Entry<Integer, String[]> entry : mapCk.entrySet()) {
//			System.out.println("mapCk. ary=" + Arrays.toString(entry.getValue()));
			para = "";
			para2 = "";
			inMap = false;
			if (cnt + 1 < mapCk.size()) {
				sIdxNext = Integer.parseInt(mapCk.get(cnt + 1)[0]);
			}

			sIdx = Integer.parseInt(entry.getValue()[0]);
			eIdx = Integer.parseInt(entry.getValue()[1]);
			hdg = entry.getValue()[2].replaceAll("(.*?=)(.*?)(,? ?consecutive=.*?$)", "$2");
			hdgLen = hdg.length();

//			System.out.println("hdg snip==" + hdg.substring(0, Math.min(hdg.length(), 150)));

			if (nlp.getAllIndexEndLocations(entry.getValue()[2], Pattern.compile("def=|sec=")).size() == 0
					&& nlp.getAllIndexEndLocations(entry.getValue()[2],
							Pattern.compile(", as [A-Z]{1}|=B[Yy]{1} [A-Z]|.{0,2}(DATE).{0,2}")).size() > 0) {
//				System.out.println("1...continue.==");
				continue;
			}

			if (nlp.getAllIndexEndLocations(entry.getValue()[2], Pattern.compile("(mHd|hdg|sub)")).size() > 0
					&& sIdxNext - eIdx > 20 && eIdx - sIdx - hdgLen < 80) {

//				System.out.println("hdgLen=" + hdgLen + " hdg=" + hdg + " eIdx=" + eIdx + " sIdxNext=" + sIdxNext
//						+ " sIdx=" + sIdx);

				if (text.length() > sIdx + hdgLen
						&& nlp.getAllMatchedGroups(text.substring(sIdx + hdgLen), patternPara).size() > 0) {
					para = nlp.getAllMatchedGroups(text.substring(sIdx + hdgLen), patternPara).get(0);
					// gets the paragraph
					// System.out.println("para.len=" + para.length());

					para2 = getParagraphAfterHeading(text, sIdx + hdgLen + 1, sIdxNext - 1);
					if (para2.length() > para.length())
						para = para2;
				}

				if (para.length()<2 ||
				nlp.getAllIndexEndLocations(entry.getValue()[2], Pattern.compile(", as [A-Z]{1}")).size() > 0
						|| (para.substring(0, 1).replaceAll("[a-z]+", "").length() == 0 && nlp
								.getAllMatchedGroups(para.substring(0, Math.min(8, para.length())),
										Pattern.compile("(?sm)\\(?[a-zA-Z\\d]{1,5}\\.?\\)|[a-zA-Z\\d]{1,4}\\."))
								.size() == 0)
						|| para.replaceAll("[\r\n]+[a-z]", "").length() < para.length()) {// para can't start l/c.
//					System.out.println("2...continue hdg=" + entry.getValue()[2] + " para.len==" + para.length()
//							+ " para.sub(0,1)=" + para.substring(0, 1).replaceAll("[a-z]+", "") + " para.repl.len="
//							+ para.replaceAll("[\r\n]+[a-z]", "").length() + " para.snip==" + para.substring(0, 10)
//							);
					continue;
				}

				if (para.length() >= dist && sIdx + hdgLen < text.length()) {
					// add revised eIdx by adding length of para to - which is sIdx+hdgLen+para.len
					// .put

					str = text.substring(sIdx + hdgLen, Math.min(sIdx + hdgLen + para.length(), text.length()));

					/*
					 * the patternPara excludes the first hard returns prior to a char - therefore
					 * when I measure para length it is short by the # of hard returns chars
					 * (\r\n=2). So I count how many are in para captured then I add it to para.len.
					 * The count of para hard returns is accurate b/c the hard returns are always at
					 * the start of the para and I know the start based on the end of hdg.
					 */

					hardReturns = nlp.getAllIndexEndLocations(str, Pattern.compile("(?sm)\r\n")).size() * 2;
//					System.out.println("hard returns=" + hardReturns);
//					System.out.println("Yes. hdg=" + text.substring(sIdx, sIdx + hdgLen) + "\r\npara="
//							+ text.substring(sIdx + hdgLen, sIdx + hdgLen + para.length() + hardReturns) + "|END");
					String[] ary = { sIdx + "", (sIdx + hdgLen + para.length() + hardReturns) + "",
							entry.getValue()[2] };
					mapClean.put(entry.getKey(), ary);
					inMap = true;
//					System.out.println(".put=="+Arrays.toString(ary));
					cnt++;
				}

			} else {
				// .put here
				mapClean.put(entry.getKey(), entry.getValue());
//				System.out.println(".put=="+Arrays.toString(entry.getValue()));
				cnt++;
			}
			if (!inMap)
				mapClean.put(entry.getKey(), entry.getValue());
		}

		cnt = 0;
		TreeMap<Integer, String[]> mapCl = new TreeMap<Integer, String[]>();
		for (Map.Entry<Integer, String[]> entry : mapClean.entrySet()) {
			if (entry.getValue()[2].toLowerCase().contains("toc=")) {
//				System.out.println("has toc. continue=" + entry.getValue()[2]);
				continue;
			}
			mapCl.put(cnt++, entry.getValue());
		}

		return mapCl;

	}

	public String removeDataTables(String htmlTxt) throws IOException {

		NLP nlp = new NLP();

		Pattern patternTable = Pattern.compile("(?ism)<TABLE.*?>.*?</TABLE>");
		Pattern patternRows = Pattern.compile("(?ism)<TR.*?>.*?</TR>");
		Pattern patternRowCount = Pattern.compile("(?ism)<TR.*?>|</TR>");
//		Pattern patternCells = Pattern.compile("(?ism)<TD.*?>.*?</TD>");
		Pattern patternCellsCnt = Pattern.compile("(?ism)</TD>");
		Pattern patternDiv = Pattern.compile("(?ism)</DIV>|</P>");
		Pattern patternNumbers = Pattern.compile("(?ism)[\\$\\.\\d\\%]+ ?<");
		List<String> listRows = new ArrayList<String>();
		List<String> listTabs = new ArrayList<String>();
		List<String[]> listTable = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(htmlTxt, patternTable);

		boolean itIsAtable = false;
		int cnt = 0, cnt2 = 0, divCnt = 0, numCnt = 0;
		double cells = 0, rows = 0;
		String row = "", tab = "";
		int sIdx = 0, eIdx = 0, eIdxPrior = 0;
//		System.out.println("listTable.size=" + listTable.size());

		StringBuffer sb = new StringBuffer();
//		int cellCnt = 0;
		for (int i = 0; i < listTable.size(); i++) {
//			cellCnt = 0;
			cells = 0;
//			cont = false;
			cells = nlp.getAllIndexEndLocations(listTable.get(i)[0], patternCellsCnt).size();
			rows = nlp.getAllIndexEndLocations(listTable.get(i)[0], patternRowCount).size();
			divCnt = nlp.getAllIndexEndLocations(listTable.get(i)[0], patternDiv).size();
			numCnt = nlp.getAllIndexEndLocations(listTable.get(i)[0], patternNumbers).size();
//			System.out.println("numCnt=====" + numCnt + " divCnt=" + divCnt + " rows=" + rows + " cells=" + cells);
			if (cells > 2000 | divCnt > 1000 | numCnt > 1000)
				continue;
//			System.out.println(
//					"table kept -- table.len=" + listTable.get(i)[0].length() + " rows=" + rows + " cells=" + cells);

			itIsAtable = false;
			cnt = 0;
			sIdx = Integer.parseInt(listTable.get(i)[1]);
			eIdx = Integer.parseInt(listTable.get(i)[2]);

			if (i == 0 && sIdx > 0) {
				sb.append(
//						"<i=" + i + ">" + 
						htmlTxt.substring(0, sIdx)
//				+ "<iEnd>"
				);
			} else {
				sb.append(
//						+ "<ePrior i=" + i + ">" + 
						htmlTxt.substring(eIdxPrior, sIdx)
//						+ "<iEnd>"
				);
			}

			listRows = nlp.getAllMatchedGroups(listTable.get(i)[0], patternRows);
			for (int c = 0; c < listRows.size(); c++) {
				row = listRows.get(c);
//				System.out.println("rows=" + row);
				listTabs = nlp.getAllMatchedGroups(row, patternCellsCnt);
//				System.out.println("listTabs.size=" + listTabs.size());
				cnt2 = 0;
				if (listRows.size() > 8 && listTabs.size() > 5) {
					// <==catchall. more than 6 columns and 10 rows
					itIsAtable = true;
					break;
				}
				for (int n = 0; n < listTabs.size(); n++) {
					if (n == 0)
						continue;
					tab = listTabs.get(n).replaceAll("(<.*?>)*", "").replaceAll("[\t\r\n &nbsp]+", "");
//					System.out.println("tab=" + tab);

					// don't be aggressive on what to exclude
					if (nlp.getAllMatchedGroups(tab, Pattern.compile("[\\d]{3,},|\\d%|\\$\\d" + "")).size() > 0) {
						cnt2++;
//						System.out.println("1nmb=" + tab);
						// if there are 2 columns with #s greater than 99 or decimal 99.9
					}
				}
				if (cnt2 > 1) {
					cnt++;
				}
				if (cnt > 2)
					// if there are 3 rows with 2 or more cols w/ #s.
					itIsAtable = true;
			}

			if (!itIsAtable) {
				sb.append(htmlTxt.substring(sIdx, eIdx));
			}
			eIdxPrior = eIdx;

		}

		sb.append(htmlTxt.substring(eIdx, htmlTxt.length()));
//		System.out.println("this many cells after ascihelper is done=="
//				+ nlp.getAllIndexStartLocations(sb.toString(), patternCells).size() + " this many tables="
//				+ nlp.getAllIndexStartLocations(sb.toString(), patternTable).size());

		String text = sb.toString();
		if (nlp.getAllIndexStartLocations(text, patternCellsCnt).size() > 5000) {
			text = text.replaceAll("(?ism)<TABLE.*?>.*?</TABLE>", "");
		}

		return text;
	}

	public String getFormattedHeadings(String text) throws IOException {
//		System.out.println("start getHeadersFromFormatting: " + text.length());

//		<p[^>]*>
		double bef = 1, aft = 1;
		NLP nlp = new NLP();
		bef = nlp.getAllMatchedGroups(text, Pattern.compile("(?ism)\r|\n")).size();
		System.out.println("at getFormattedHeadings, # of hard returns bef=" + bef);
		text = text.replaceAll("(?ism)<u>|<i>|<b>|<u [^>]*>|<i [^>]*>|<b [^>]*>", "Qx");
//		System.out.println("2.");
		text = text.replaceAll("(?ism)(</u>)|(</i>)|(</b>)", "XQ");
//		System.out.println("3.");
		Document doc = Jsoup.parse(text);
//		System.out.println("4.");
		Elements eles = doc.select("B,I,U,[style~=(bold|italic|underline)]");
		Node n;
		TextNode tn;
//		System.out.println("starting getHeadersFromFormatting");

		for (Element e : eles) {

//			System.out.println("Qx" + e.html() + "XQ");XXXXXXXXXXXXXXXX

			if (e.childNodeSize() > 0) {
				n = e.childNode(0);
				if (n instanceof TextNode) {
//					System.out.println(n.nodeName());
					tn = (TextNode) n;
					tn.text("Qx" + tn.getWholeText() + "XQ");
//					System.out.println("tn=" + tn.getWholeText() + "||");
//					System.out.println("TN: " + e.ownText() + "||");
				}
			}
		}

		text = doc.outerHtml();

		aft = nlp.getAllMatchedGroups(text, Pattern.compile("(?ism)\r|\n")).size();
		System.out.println("at getFormattedHeadings, # of hard returns after=" + aft);
		// System.out.println("end doc.outerHtml()=" + text.length());

		text = text.replaceAll(">\n[ ]+", ">");
//		if(aft/bef>5)
//			text = text.replaceAll("(?sm)XQ</[a-zA-Z]{1,35}>\r\n[ ]+", "");
		if (GetContracts.turnOnPrinterWriter)
			Utils.writeTextToFile(new File("c:/temp/getFormattedHeadings.txt"), text);

		// FileSystemUtils.writeToAsciiFile("C:/temp/tmpHtml_marked.htm", text);
		return text;

	}

	public String asciiHelper(String text) throws IOException {

//		long sTime = System.currentTimeMillis();
//		System.out.println("1 repl");
//		System.out.println("at asciiHelper");

		NLP nlp = new NLP();

		text = text.replaceAll("(?ism)<font.*?>", "");
		text = text.replaceAll("(?ism)</font>", "");
//		if(GetContracts.turnOnPrinterWriter) Utils.writeTextToFile(new File("c:/temp/1.asciihelper 1A.txt"), text);
		text = text.replaceAll("([\\.\\.]{2,99})", " ")
//				.replaceAll(" \\.", " ")
		;
//		 .</p>
		text = text.replaceAll("(?ism)(?<=[A-Za-z\\)]) \\.</p>", "\\.</p>");
		text = text.replaceAll("(?ism)<PRE>", "");
//		System.out.println("3. text.len=" + text.length());
//		sTime = System.currentTimeMillis();
//		System.out.println("2 repl");
//		if(GetContracts.turnOnPrinterWriter) Utils.writeTextToFile(new File("c:/temp/1.asciihelper 1B.txt"), text);
		text = text.replaceAll("(�)(?ism)(.{3,50})(�)", "\"$2\"");
//		if(GetContracts.turnOnPrinterWriter) Utils.writeTextToFile(new File("c:/temp/1.asciihelper 2.txt"), text);
		text = text.replaceAll("“|”|&#147;|&#148;|&#8221;|&#8220;|&ldquo;|&rdquo;|&quot;|&#x201C;|&#x201D", "\"");
		text = text.replaceAll("&#145;|’|&#146;|�|�|&rsquo;|&#8217;" + "|&#x2019;" + "|&#8216;", "'");

//		System.out.println("seconds==" + (sTime - System.currentTimeMillis()) / 1000);
//		sTime = System.currentTimeMillis();
//		System.out.println("3 repl");
//		text = text.replaceAll("&#36", "\\$");
		text = text.replaceAll("&#9;|&nbsp;|\\xA0|&#160;|&#xA0;|&#168;|&#173;|&#32;|&#8194;", " ");
//		System.out.println("seconds==" + (sTime - System.currentTimeMillis()) / 1000);
//		sTime = System.currentTimeMillis();
//		System.out.println("4 repl");
		text = text.replaceAll("&#151;|&mdash|&#95;|&#9744;", "_");
		text = text.replaceAll("&#8211;|&#9679;|&#150;|&#8212;|&#8209;|&#111;|&ndash;|&#x2022;|&middot;", "-");
//		System.out.println("seconds==" + (sTime - System.currentTimeMillis()) / 1000);
//		sTime = System.currentTimeMillis();
//		System.out.println("5 repl");
		text = text.replaceAll("&amp;|&#038", "&").replaceAll("&#091;", "\\[").replaceAll("&#093;", "\\]");
//		System.out.println("seconds==" + (sTime - System.currentTimeMillis()) / 1000);
//		sTime = System.currentTimeMillis();
//		System.out.println("6 repl");
//		System.out.println("text.len="+text.length());
		text = text.replaceAll("&#184;", ",");
		text = text.replaceAll("(?is)<!--.*?-->", "");
		text = text.replaceAll("[\r\n]{1,}\\.", "\\.\r\n");
//		text = text.replaceAll("([\r\n]{1,2})([ ]+)([\r\n]{1,2})", "$1$3");
//		text = text.replaceAll("&#187;[ \r\n\t]{0,15}\\.", "\\.");
		text = text.replaceAll("([a-zA-Z]{1})(\\.)([a-zA-Z]{1})(\\.)([a-zA-Z]{1})(\\.)", "$1xxPD$3xxPD$5xxPD");
//		System.out.println("seconds==" + (sTime - System.currentTimeMillis()) / 1000);
//		sTime = System.currentTimeMillis();
//		System.out.println("7 repl");
		text = text.replaceAll("([a-zA-Z]{1})(\\.)([a-zA-Z]{1})(\\.)", "$1xxPD$3xxPD");
		text = text.replaceAll(" (?=(INC|CO|Inc|Co))\\.(?=[, ]{1})", "xxPD");
		text = text.replaceAll("(?<= (INC|CO|Inc|Co))\\.(?=[, ]{1})", "xxPD");
		text = text.replaceAll("\r\n ?\r\n[ \t]{0,10}[\\d\\.]{4,20}[ \t]{0,10}\r\n ?\r\n", "");
//		System.out.println("seconds==" + (sTime - System.currentTimeMillis()) / 1000);
//		System.out.println("text.len="+text.length());
//		sTime = System.currentTimeMillis();
//		System.out.println("8 repl");
		text = text.replaceAll("(" +
		// 1st word
				" [\\)\\(\\[\\]A-Za-z,;]{1,11}" +
				// 2nd word
				" [\\)\\(\\[\\]a-z,;]{1,11} )" + "(\")" + "([\\[\\]_-a-zA-Z;, ]{1,50})" + "(\")", "$1''$3''");

		text = text.replaceAll("(?i)(etc)\\.(?! [A-Z]{1}[a-z]{1,} [a-z]{1}|  [A-Z]| ?[\r\n]| ? \\([a-z]{1}\\))", "$1");
		text = text.replaceAll("(?<=[a-z]{1}) \\.  ?(?=[A-Z]{1}1)", "\\. ");
//		System.out.println("seconds==" + (sTime - System.currentTimeMillis()) / 1000);
//		sTime = System.currentTimeMillis();
//		System.out.println("9 repl");
//		System.out.println("text.len="+text.length());
		text = text.replaceAll("(\r\n ?)([\\d]{1,3}\\.[\\d]{1,2})(\r\n ?)([A-Z].{5,100}?\r\n\r\n)", "$1$2 $4");

		text = text.replaceAll("\r\n\r\n\r\n", "\r\n\r\n");
//		System.out.println("seconds==" + (sTime - System.currentTimeMillis()) / 1000);
//		System.out.println("text.len="+text.length());
//		sTime = System.currentTimeMillis();
//		System.out.println("10 repl");
//		System.out.println("ascii helper seconds==" + (sTime - System.currentTimeMillis()) / 1000);
		return text;

	}

	public String getClauseLeadin(String text) throws IOException {

		// gets clause based on there being consecutive numerals - otherwise it won't
		// pick it up. Also it won't get any sub-numerals

		boolean listSizeIs1 = false;
		List<String[]> list = getParentChildSimple(text, listSizeIs1);

		if (list.size() == 1) {
//			System.out.println("==1");
			listSizeIs1 = true;
			list = getParentChildSimple(text, listSizeIs1);
		}

		if (list.size() == 0)
			return null;

		int sIdx = 0;

		sIdx = 0;
		for (int i = 0; i < list.size(); i++) {
			sIdx = Integer.parseInt(list.get(i)[3]);
			if (sIdx > 0 && i == 0) {
//				System.out.println("leadin=" + clauseText.substring(0, sIdx));
				return clauseText.substring(0, sIdx).replaceAll("(sssS1s)", "");
			}
		}

		return "";
	}

	public List<String> getSolrClause(String text) throws IOException {
		NLP nlp = new NLP();

		// gets clause based on there being consecutive numerals - otherwise it won't
		// pick it up. Also it won't get any sub-numerals

		List<String> listClause = new ArrayList<String>();
		boolean listSizeIs1 = false;
		List<String[]> list = getParentChildSimple(text, listSizeIs1);
//		System.out.println("getParentChildSimple.size()==" + list.size());

		if (list.size() == 1) {
//			System.out.println("==1");
			listSizeIs1 = true;
			list = getParentChildSimple(text, listSizeIs1);
		}

		if (list.size() == 0)
			return null;

		int sIdx = 0, sIdxNext = 0;
		String leadin = "", clause;
//		NLP.printListOfStringArray("list xXx=", list);
//		System.out.println("text==" + text);

//		System.out.println("clauseText=" + clauseText + "||");
//		clauseText = clauseText.replaceAll("sssS1s", "");
		sIdx = 0;
		for (int i = 0; i < list.size(); i++) {
			sIdx = Integer.parseInt(list.get(i)[3]);
			if (sIdx > 0 && i == 0) {
//				System.out.println("leadin=" + clauseText.substring(0, sIdx));
				listClause.add("leadin=" + clauseText.substring(0, sIdx));
			}
			if (i + 1 < list.size()) {
				sIdxNext = Integer.parseInt(list.get(i + 1)[3]);
			} else {
				sIdxNext = clauseText.length();//
			}

			if (sIdxNext < sIdx)
				break;

//			System.out.println("clause=" + clauseText.substring(sIdx, sIdxNext) + "|");
			listClause.add("clause=" + clauseText.substring(sIdx, sIdxNext));
		}

		return listClause;
	}

	public List<String[]> getAllNumeralClauses(List<String[]> listParentChildOrig, boolean listSizeIsZero) {

		List<String[]> listParentChildOrigTmp = new ArrayList<String[]>();

		for (int i = 0; i < listParentChildOrig.size(); i++) {
			if (listParentChildOrig.get(i).length < 5)
				continue;
			if (listSizeIsZero && listParentChildOrig.get(i)[3].equals("0"))
				continue;
			String[] ary = { listParentChildOrig.get(i)[0], listParentChildOrig.get(i)[1],
					listParentChildOrig.get(i)[2], listParentChildOrig.get(i)[3], listParentChildOrig.get(i)[4] };
			listParentChildOrigTmp.add(ary);
		}

		return listParentChildOrigTmp;

	}

	public static List<String[]> findParaNumbers(List<String[]> list) {
		/*
		 * 2 listParentChildOrig=x= i=0 [sssS1s(f), 6, 2, 0, slave] 2
		 * listParentChildOrig=x= i=1 [sssS1s(i), 1, 0, 10, slave] 2
		 * listParentChildOrig=x= i=2 [sssS1s(i), 9, 2, 10, slave] 2
		 * listParentChildOrig=x= i=3 [sssS1s(ii), 2, 0, 386, slave] 2
		 * listParentChildOrig=x= i=4 [sssS1s(ii), 35, 2, 386, slave] 2
		 * listParentChildOrig=x= i=5 [sssS1s(iii), 3, 0, 1003, slave] 2
		 * listParentChildOrig=x= i=6 [sssS1s(x), 10, 0, 1220, slave] 2
		 * listParentChildOrig=x= i=7 [sssS1s(x), 24, 2, 1220, slave] 2
		 * listParentChildOrig=x= i=8 [sssS1s(y), 25, 2, 1510, slave] 2
		 * listParentChildOrig=x= i=9 [sssS1s(iv), 4, 0, 1628, slave]
		 */

		TreeMap<Integer, String[]> map = new TreeMap<Integer, String[]>();
		int key = 0, typ = 0, number = 0, sIdx = 0;
		for (int i = 0; i < list.size(); i++) {
			sIdx = Integer.parseInt(list.get(i)[3]);
			number = Integer.parseInt(list.get(i)[1]);
			typ = Integer.parseInt(list.get(i)[2]);
			key = (typ + 1) * 100000 + sIdx;
			String[] ary = { list.get(i)[0], number + "", typ + "", sIdx + "", list.get(i)[4] };
			map.put(key, ary);
		}

		List<String[]> listTyp1 = new ArrayList<String[]>();
		List<String[]> listTyp2 = new ArrayList<String[]>();

		key = 0;
		boolean startList2 = false;
		int keyPrior = 0, cnt = 0;
		for (Map.Entry<Integer, String[]> entry : map.entrySet()) {
//			System.out.println("key=" + entry.getKey() + " ary=" + Arrays.toString(entry.getValue()));
			key = entry.getKey();

			if (key - keyPrior > 10000 && cnt > 0) {
				startList2 = true;
			}
			if (startList2) {
				listTyp2.add(entry.getValue());
			} else {
				listTyp1.add(entry.getValue());
			}
			cnt++;
			keyPrior = key;
		}

//		NLP.printListOfStringArray("listTyp2==", listTyp2);

		int numberNext = 0, ct = 3;
		number = 0;
		TreeMap<Integer, String[]> mapConsec = new TreeMap<Integer, String[]>();
		for (int i = 0; i < listTyp1.size(); i++) {
			number = Integer.parseInt(listTyp1.get(i)[1]);
			for (int c = i; c < Math.min((i + ct), listTyp1.size()); c++) {
				numberNext = Integer.parseInt(listTyp1.get(c)[1]);
				if (numberNext - number == 1) {
//					System.out.println("these are consecutive ary1=" + Arrays.deepToString(listTyp1.get(i)) + " ary2="
//							+ Arrays.deepToString(listTyp1.get(c)));
					mapConsec.put(Integer.parseInt(listTyp1.get(i)[3]), listTyp1.get(i));
					mapConsec.put(Integer.parseInt(listTyp1.get(c)[3]), listTyp1.get(c));

				}
			}
		}

		for (int i = 0; i < listTyp2.size(); i++) {
			number = Integer.parseInt(listTyp2.get(i)[1]);
			for (int c = i; c < Math.min((i + ct), listTyp2.size()); c++) {
				numberNext = Integer.parseInt(listTyp2.get(c)[1]);
				if (mapConsec.containsKey(Integer.parseInt(listTyp2.get(i)[3])))
					continue;
				if (mapConsec.containsKey(Integer.parseInt(listTyp2.get(c)[3])))
					continue;

				if (numberNext - number == 1) {
//					System.out.println("2 these are consecutive ary1=" + Arrays.deepToString(listTyp2.get(i)) + " ary2="
//							+ Arrays.deepToString(listTyp2.get(c)));
					mapConsec.put(Integer.parseInt(listTyp2.get(i)[3]), listTyp2.get(i));
					mapConsec.put(Integer.parseInt(listTyp2.get(c)[3]), listTyp2.get(c));
				}
			}
		}

		List<String[]> listOrdered = new ArrayList<String[]>();
		for (Map.Entry<Integer, String[]> entry : mapConsec.entrySet()) {
			listOrdered.add(entry.getValue());
		}

		return listOrdered;

	}

	public List<String[]> getParentChildSimple(String sentenceTxt, boolean listSizeIsZero) throws IOException {

		List<String[]> listParChi = new ArrayList<>();

		int lineage = 0;
		List<String[]> listParentChildOrig = new ArrayList<String[]>();
		// returns this list to attach solrFields to.
//		System.out.println("sentenceTxt--"+sentenceTxt);
		listParentChildOrig = sentenceParentChild(sentenceTxt);
//		NLP.printListOfStringArray("1 bef sentenceParentChild=x=", listParentChildOrig);
		listParentChildOrig = getAllNumeralClauses(listParentChildOrig, listSizeIsZero);
//		NLP.printListOfStringArray("2 bef getAllNumeralClauses=x=", listParentChildOrig);
		listParentChildOrig = findParaNumbers(listParentChildOrig);
//		NLP.printListOfStringArray("2 bef findParaNumbers=x=", listParentChildOrig);

//		System.out.println("sentenceTxt=x=" + sentenceTxt.trim());
//		listParChi = parentChildSentence(listParentChildOrig, sentenceTxt, lineage, null);
		// at parentChildSentence the roman numerals are selected or disarded here

//		NLP.printListOfStringArray("AA listParChi==", listParChi);
//			System.out.println("listParagraphParentChild.size="+listParagraphParentChild.size());
		// here I should call matter that returns the final text to put into xml.
//			NLP.printListOfStringArray("listParagraphParentChild==",listParagraphParentChild);
		listParagraphParentChild = listParentChildOrig;
		List<String[]> listParChi2 = new ArrayList<>();
		if (listParagraphParentChild.size() > 0) {
			lineage++;
			listParChi2 = parentChildSentence(listParagraphParentChild, sentenceTxt, lineage, listParChi);
			for (int n = 0; n < listParChi2.size(); n++) {
				listParChi.add(listParChi2.get(n));
			}
//			NLP.printListOfStringArray("BB parentChildSentence=", listParChi);
		}

		/*
		 * NOTE: THIS has to be an iteration of each sentence. And after each sentence
		 * to start new!!
		 */

//		NLP.printListOfStringArray("FINAL getParentChildSimple==", listParChi);

		return listParChi;

	}

	public List<String[]> parentChildSentence(List<String[]> list1, String text, int lineage, List<String[]> listParChi)
			throws IOException {

		List<String[]> listParChiSent = new ArrayList<>();
		int type = -1, sIdx = -1, val = -1, valLast2 = -1, valLast = -1, valPrior = -1, typeOrig = -1, val2 = -1,
				type2 = -1, typeLast = -1, type2Last = -1;
		int type2_ = -1, sIdx2 = -1, val2_ = -1, valLast22 = -1, valLast2_ = -1, valPrior2 = -1, typeOrig2 = -1,
				val22 = -1, type22 = -1, typeLast2 = -1, type2Last2 = -1;

		// for (x) there can be 2 vals and 2 types = eg val=10 or val2=24
		String mastSlav = "", numeral = "", numeralLast = "", sIdxChi = "", originalNumeral = "";
		String mastSlav2 = "", numeral2 = "", numeralLast2 = "", sIdxChi2 = "", originalNumeral2 = "";

		boolean added = false, get2 = false;
//		List<String[]> listParagraphParentChild = new ArrayList<>();
		List<String[]> list2 = list1;
		List<String[]> list = new ArrayList<String[]>();

//		sssS1s(i), 1, 0, 36, slave --->list
		for (int i = 0; i < list2.size(); i++) {
			numeral = list2.get(i)[0];
			val = Integer.parseInt(list2.get(i)[1]);
			type = Integer.parseInt(list2.get(i)[2]);
			sIdx = Integer.parseInt(list2.get(i)[3]);
			for (int c = 0; c < list2.size(); c++) {
				val2 = Integer.parseInt(list2.get(c)[1]);
				type2 = Integer.parseInt(list2.get(c)[2]);
				sIdx2 = Integer.parseInt(list2.get(c)[3]);
				numeral2 = list2.get(c)[0];
//				System.out.println("numeral=" + numeral2 + " type=" + type + " type2=" + type2 + " sIdx=" + sIdx
//						+ " sIdx2=" + sIdx2 + " val=" + val + " val2=" + val2);
				if (type2 == type && sIdx < sIdx2 && val2 - val == 1) {
//					System.out.println("i found the match===" + numeral + " numveral2=" + numeral2);
					String[] ary = { numeral, val + "", type + "", sIdx + "", list2.get(i)[4] };
					list.add(ary);
					String[] ary2 = { numeral2, val2 + "", type2 + "", sIdx2 + "", list2.get(c)[4] };
					list.add(ary2);
//					if (list2.size() > 0) {
//						list2.remove(c);
//					}
				}
			}

		}

//		NLP.printListOfStringArray("===", list2);

		for (int i = 0; i < list.size(); i++) {

//			System.out.println("from AA to list.get(i)==" + Arrays.toString(list.get(i)));
			added = false;
			numeral = list.get(i)[0];
			val = Integer.parseInt(list.get(i)[1]);
			type = Integer.parseInt(list.get(i)[2]);
			sIdx = Integer.parseInt(list.get(i)[3]);
			mastSlav = list.get(i)[4];
//			System.out.println("i==" + i);
			if (i > 0) {
				valPrior = Integer.parseInt(list.get(i - 1)[1]);
//				System.out.println("val=" + val + " valPrior=" + valPrior + " type=" + type + " typePrior="
//						+ Integer.parseInt(list.get(i - 1)[2]));
			}

			if (i == 0) {
				typeOrig = type;
//				System.out.println("typeOrig===" + typeOrig);
				valLast = val;
				numeralLast = numeral;
				originalNumeral = numeral;
				String[] ary = { numeral, val + "", type + "", sIdx + "", mastSlav, lineage + "" };
				// (v), 5, 0, 3652, master
//				System.out.println("i=" + i + " 1. add ary=" + Arrays.toString(ary) + " valLast=" + valLast);
				listParChiSent.add(ary);
				added = true;
				continue;
			}

			// get current numeral to match with originaltype and then pull the numeral
			// value and see if that value minus prior =1!!

			// if I can get typ and dif=1 to match then I've found it. So I need to find 2
			// consec numerals - then hard retrieve based on that typ and val. So until I
			// have at least 2 numerals - I can't start to figure out values and types until
			// I land on where types are equal and val-valprior=1

			if ((val - valLast == 1 && list.size() == 2) || (val - valLast == 1 && typeOrig == type)
//					|| (originalNumeral.toLowerCase().contains("x"))
			) {
				String[] ary = { numeral, val + "", type + "", sIdx + "", mastSlav, lineage + "" };
				listParChiSent.add(ary);
				added = true;
				valLast = val;
				numeralLast = numeral;
//				System.out.println("i=" + i + " 2. add ary=" + Arrays.toString(ary) + " valLast=" + valLast);
			}

//			System.out.println("added=" + added + " valLast=" + valLast + " numeral=" + numeral + " val=" + val
//					+ " type=" + type + " sIdx=" + sIdx + " mastSlav=" + mastSlav);

			if (itIsAnException(val, valLast, numeral, numeralLast)) {
				String[] ary = { numeral, val + "", type + "", sIdx + "", mastSlav, lineage + "" };
				listParChiSent.add(ary);
				added = true;
				valLast = val;
				numeralLast = numeral;
//				System.out.println(
//						"it is an exception\r\ni=" + i + " 2. add ary=" + Arrays.toString(ary) + " valLast=" + valLast);
				added = true;
				continue;
			}

			// is it after a parent and if so does it start at 1? if so set at lineage=child
			// and reset valLast

//			System.out.println("x list.get(i)=" + Arrays.toString(list.get(i)));
			sIdxChi = list.get(i)[3];

			if (val == 1 && type == typeOrig && !added && null != listParChi && itIsParent(sIdxChi, listParChi, list)) {
				// if itIsParent and type==typeOrig then reset valLast and set as child

				String[] ary = { numeral, val + "", type + "", sIdx + "", mastSlav, lineage + "" };
//				System.out.println("did I add it?");
				listParChiSent.add(ary);
//				System.out.println(
//						"3. addded \r\ni=" + i + " 2. add ary=" + Arrays.toString(ary) + " valLast=" + valLast);
				added = true;
				valLast = val;
				numeralLast = numeral;

			}

			// cks to see if (x) as typ1 val=10 or typ2 val=24 will results in val dif of 1
			// - if so then add it. This is checked by getting val for both types and seeing
			// if the dif is 1
			if (!added) {
//				System.out.println("not added yet list.get(i)[0]==="+list.get(i)[0]);
				val2 = -1;
				val = -1;
				valLast2 = -1;
				type2Last = -1;
				List<Integer[]> listValIntAry = NLP.convertAlphaParaNumbAndTypeList(list.get(i)[0]);
				for (int c = 0; c < listValIntAry.size(); c++) {
//					System.out.println("listValIntAry.get(c)[0]==" + listValIntAry.get(c)[0]);
					if (val == -1) {
						val = listValIntAry.get(c)[0];
						type = listValIntAry.get(c)[1];
						continue;
					}
					if (val != -1 && val2 == -1) {
						val2 = listValIntAry.get(c)[0];
						type2 = listValIntAry.get(c)[1];
						break;
					}
				}
				List<Integer[]> listNumeralLastIntAry = NLP.convertAlphaParaNumbAndTypeList(numeralLast);
				for (int c = 0; c < listNumeralLastIntAry.size(); c++) {
//					System.out.println("listNumeralLastIntAry.get(c)[0]==" + listNumeralLastIntAry.get(c)[0]);
					if (valLast == -1) {
						valLast = listNumeralLastIntAry.get(c)[0];
						typeLast = listNumeralLastIntAry.get(c)[1];
						continue;
					}
					if (valLast != -1 && valLast2 == -1) {
						valLast2 = listNumeralLastIntAry.get(c)[0];
						type2Last = listNumeralLastIntAry.get(c)[1];
						break;
					}
				}

//				System.out.println("3. val=" + val + " val2=" + val2 + " valLast=" + valLast + " valLast2=" + valLast2
//						+ " type=" + type + " type2=" + type2 + " typeLast=" + typeLast + " type2Last=" + type2Last);

				// - each of the if statements below have to be separated with added
				// set to true and valLast and numeralLast reset to applicable values. This way
				// the value that follows realizes the 'misfit' is of the type that matches.

				if (!added && val - valLast == 1 && type == typeLast) {

					String[] ary = { numeral, val + "", type + "", sIdx + "", mastSlav, lineage + "" };
					listParChiSent.add(ary);
					added = true;
					valLast = val;
					numeralLast = numeral;
					added = true;
//					System.out.println("i=" + i + " 3. add ary=" + Arrays.toString(ary) + " valLast=" + valLast);
				}

				if (!added && val - valLast2 == 1 && type == type2Last) {

					String[] ary = { numeral, val + "", type + "", sIdx + "", mastSlav, lineage + "" };
//					System.out.println("4.\r\ni=" + i + " 2. add ary=" + Arrays.toString(ary) + " valLast=" + valLast);

					listParChiSent.add(ary);
					added = true;
					valLast = val;
					numeralLast = numeral;
					added = true;
				}

				if (!added && val2 - valLast == 1 && type2 == typeLast) {

					String[] ary = { numeral, val + "", type + "", sIdx + "", mastSlav, lineage + "" };
					listParChiSent.add(ary);
//					System.out.println("5.\r\ni=" + i + " 2. add ary=" + Arrays.toString(ary) + " valLast=" + valLast);
					added = true;
					valLast = val2;
					numeralLast = numeral;
					added = true;
				}

				if (!added && val2 - valLast2 == 1 && type2 == type2Last) {

					String[] ary = { numeral, val + "", type + "", sIdx + "", mastSlav, lineage + "" };
					listParChiSent.add(ary);
//					System.out.println("6.\r\ni=" + i + " 2. add ary=" + Arrays.toString(ary) + " valLast=" + valLast);

					added = true;
					valLast = val2;
					numeralLast = numeral;
					added = true;
				}
			}
		}

//		NLP.printListOfStringArray("listParagraphParentChild======", listParagraphParentChild);
//		NLP.printListOfStringArray("listParChiSent======", listParChiSent);
		return listParChiSent;

	}

	public boolean itIsAnException(int val, int valLast, String numeral, String numeralLast) {

		if (Math.abs(valLast - val) == 15 && numeral.replaceAll("[\\.\\(\\)]+", "").toLowerCase().equals("y")) {
			return true;
		}

		return false;

	}

	public boolean itIsParent(String sIdxStrChild, List<String[]> listParChi, List<String[]> listParentChildOrig) {

		// find if the immediately preceding numeral is a parent lineage. By passing
		// sIdx of what may be a child and stopping at the original parent child list.
		// If the immediately preceding numeral is a parent as checked against
		// listParChi then return true

		int sIdxChild = Integer.parseInt(sIdxStrChild), sIdxPrior = 0, sIdxPar = 0;
//		System.out.println("@isItParent - sIdxChid="+sIdxChild);
		for (int i = 0; i < listParentChildOrig.size(); i++) {
			if (sIdxStrChild.equals(listParentChildOrig.get(i)[3]) && i > 0) {
				// if this is true - then I found child.
				sIdxPrior = Integer.parseInt(listParentChildOrig.get(i - 1)[3]);
				// 2529
//				System.out.println("found child loc. possible parent sIdxPrior="+sIdxPrior);
				for (int n = 0; n < listParChi.size(); n++) {
//					prior type equals parent type - then return true if sIdxPar=sIdxPrior
					sIdxPar = Integer.parseInt(listParChi.get(n)[3]);
//					System.out.println("?sIdxPar"+sIdxPar+" sIdxPrior="+sIdxPrior);
					if (sIdxPar == sIdxPrior)
						return true;
				}
			}
		}

		return false;

	}

	public String prepClause1(String sentence) {

//		System.out.println("prepClause1 ssssss");

		Pattern patternClause1 = Pattern
				.compile("(?ism) " + "(\\([a-zA-Z\\d]{1,7}\\))" + "((\\([a-zA-Z\\d]{1,7}\\)" + ")?)");

		sentence = sentence.replaceAll(patternClause1.toString(), "sssS1s$1$2");
		// .replaceAll("xxxsssS1s", "xxx");

		// how to address this -- this is saying -- too many s1s finds - so
		// remove. this is okay.
		sentence = sentence.replaceAll("(sssS1s)(\\([a-zA-Z\\d]{1,7}\\) and ?)" + "(sssS1s)(\\([a-zA-Z\\d]{1,7}\\))",
				" $2$4");
		// subclauses (A), sssS1s(B). same as above
		// just gets rid of sss - so idx loc not changed.
		sentence = sentence.replaceAll("(\\([a-zA-Z\\d]{1,7}\\), ?)(sssS1s)(\\([a-zA-Z\\d]{1,7}\\))", "$1$3");

		// just gets rid of sss - so idx loc not changed.
		sentence = sentence.replaceAll("(?ism)(sections? ?|clauses? ?|paragraphs? ?|through ?)sssS1s(\\()", "$1$2");
		// in clauses (i) -sssS1s(iii)
		// just gets rid of sss - so idx loc not changed.

		sentence = sentence.replaceAll("(?ism)(sections|clauses?|paragraphs)( \\([a-zA-Z\\d]{1,7}\\) ?-?)"
				+ "(through|thru)?(sssS1s)(\\([a-zA-Z\\d]{1,7}\\))", "$1$2$3$5");

		sentence = sentence.replaceAll("(sssS1s)(\\([\\d]{1,2}\\))(,|;)( )", " $2$3$4");

		// sssS1s(FRB) | sssS1s(OCC)
		sentence = sentence.replaceAll("(sssS1s)(\\([A-Z]{3,}\\))", "$2");

//		System.out.println("sentence=======" + sentence);
		return sentence;
	}

	public String getNumeralNumberedSubSentencesInSentence(String sentence) {
		// List<String> listOfText = new ArrayList<>();
		// NLP nlp = new NLP();

//		System.out.println("getNumeralNumberedSubSentencesInSentence ssssss");
		sentence = sentence.replaceAll("(?<=\\([a-zA-Z]{1,6}\\) ?)" + "([\r\n]{1,4})(?= ?[a-z]{1})", " ")
				.replaceAll("[ ]+", " ");
//		 System.out.println("aa. sentence=="+sentence);
		sentence = sentence.replaceAll("(?<=Section [\\d\\.]{1,7}) (?=\\([a-zA-Z]{1}\\))", "");
		sentence = sentence.replaceAll("(?ism)" + "(\\()"
		// $1==>xxOp
				+ "([a-zA-Z\\d]{1,7})"
				// $2
				+ "(\\))" +
				// $3=>CPxx
				"(\\]?)"
				// $4
				+ " (business|through|largest|smallest|months?|days?)"
		// space $5
				, "xxOP$2CPxx$4 $5");

		// paragraphs (1), (2), (3) or (7)
		sentence = sentence.replaceAll("(?ism)"
				// $1==>xxOp
				+ "(\\()"
				// $2
				+ "([a-zA-Z\\d]{1,7})"
				// $3==>CPxx
				+ "(\\))" +
				// $4
				"(, | or | and )" +
				// $5
				"(\\()" +
				// $6
				"([a-zA-Z\\d]{1,7})" +
				// $7 CPxx
				"(\\))", "xxOP$2CPxx$4xxOP$6CPxx");

//		System.out.println("aaa sent=="+sentence);

//		sentence = sentence.replaceAll("(Section |Clause |Paragraph |section |clause |paragraph )"
//				+ "(\\()([a-z\\d]{1,7})(\\))" + "([\r\n \t]{1})", "$1xxOP$3CPxx$5");

		sentence = sentence.replaceAll("(Section |Clause |Paragraph |section |clause |paragraph |over)"
				+ "(\\()([A-Za-z\\d]{1,7})(\\))" + "([\r\n \t]{1})", "$1xxOP$3CPxx$5");

		sentence = sentence.replaceAll("(through )" + "(\\()([A-Za-z\\d]{1,7})(\\))" + "([\r\n \t]{1})",
				"$1xxOP$3CPxx$5");

		Pattern patternClause1 = Pattern.compile("(?ism)( |^)(\\([a-zA-Z\\d]{1,7}\\)|[\\d]{1,3}\\.)"// add 4/4 =====>
																									// |\\d\\.
				+ "((\\([a-zA-Z\\d]{1,7}\\))?)");

		sentence = sentence.replaceAll(patternClause1.toString(), "$1sssS1s$2$3");

		// too many s1s finds - so remove. this is okay.
		// clauses(i) or (ii), clauses (i) and (ii)
		sentence = sentence.replaceAll(
				"(sssS1s)(\\([a-zA-Z\\d]{1,7}\\) " + "?(and" + "|or) ?)(sssS1s)(\\([a-zA-Z\\d]{1,7}\\))", "$2$5");
//		 System.out.println("1="+sentence+"||||");

		sentence = sentence.replaceAll("(sssS1s)(\\([a-zA-Z\\d]{1,7}\\), )" + "(sssS1s)(\\([a-zA-Z\\d]{1,7}\\))",
				"$2$4");
		// System.out.println("2="+sentence+"||||");

		// subclauses (A), sssS1s(B).
		sentence = sentence.replaceAll("(\\([a-zA-Z\\d]{1,7}\\), ?)(sssS1s)" + "(\\([a-zA-Z\\d]{1,7}\\))", "$1$3");
		// NLP.pwNLP.append(NLP.println("1a3 ssssss - sentence==", sentence));
		// System.out.println("3="+sentence+"||||");

		sentence = sentence.replaceAll("(?ism)(sections? ?|clauses? ?|paragraphs? ?" + "|through ?)" + "sssS1s(\\()",
				"$1 $2");
		// in clauses (i) -sssS1s(iii)
		// System.out.println("4="+sentence+"||||");

		sentence = sentence.replaceAll("(?ism)(sections|clauses?|paragraphs)" + "( \\([a-zA-Z\\d]{1,7}\\) ?-?)"
				+ "(through|thru)?(sssS1s)(\\([a-zA-Z\\d]{1,7}\\))", "$1$2$3$5");
		// System.out.println("5="+sentence+"||||");

		sentence = sentence.replaceAll("(sssS1s)(\\([\\d]{1,2}\\))(,|;)( )", " $2$3$4");

		// System.out.println("6="+sentence+"||||");
		// sssS1s(FRB) | sssS1s(OCC)
		sentence = sentence.replaceAll("(sssS1s)(\\([A-Z]{3,}\\))", "$2");
		// and the case of each of clause(
		// System.out.println("7="+sentence+"||||");

		// gets eg - " ; provided"
		// sentence = sentence.replaceAll("(;)( [a-z]{3,15}[,;]{0,1} )", "sssS1s; $2");
//		System.out.println("8=" + sentence + "||");

		int sIdx = 0, eIdx = 0;
		String clause = "";

		sentence = prepClause1(sentence);

//		System.out.println("prepClause1 9=" + sentence + "||||");

		// fix==> clause(i), (ii) and(iii)
		sentence = sentence.replaceAll(
				"(?ism)(clauses?|sections?" + "|paragraphs?|articles|and|or)(\\([a-zA-Z\\d]{1,7}\\))", "$1 $2");
		// System.out.println("10="+sentence+"||||");
		// fix==>clauses(i) orsssS1s(ii) and(iii)
		sentence = sentence
				.replaceAll("(?ism)(sections? ?|paragraphs? ?|clauses? ?)(\\([a-zA-Z\\d]{1,7}\\) ?(and|or) ?)"
						+ "(sssS1s)(\\([a-zA-Z\\d]{1,7}\\))", "$1 $2 $5");
//		System.out.println("11=" + sentence + "||||");

		sentence = sentence.replaceAll("(?ism)(sssS1s)(\\()([A-Za-z\\d]{1,5})(\\))" + "([ \r\n]{1,3}above)",
				"xxOP$3CPxx$5");
		// System.out.println("12="+sentence+"||||");
		sentence = sentence.replaceAll("(?ism)((in|under)[ \r\n]{1,3}clause )" + "(\\()([A-Za-z\\d]{1,5})(\\))",
				"$1xxOP$4CPxx");
		// System.out.println("13="+sentence+"||||");

		sentence = sentence.replaceAll("(?ism)(SECTION [\\d]+\\.[\\d]+)(\\()([A-Za-z]{1})(\\))", "$1xxOP$3CPxx");
		sentence = sentence.replaceAll("(?ism)(section[ \r\n]{1,8})" + "(\\()" + "([A-Z]{1,4})" + "(\\))",
				"$1xxOP$3CPxx");

		sentence = sentence.replaceAll("xxOP[\r\n]{1,3}", "xxOP");
		sentence = sentence.replaceAll("(\r\n)([\\d\\.]{1,7}{1,5}\\%)", " $2");
		sentence = sentence.replaceAll("(?<=Section [\\d\\.]{1,7}) (?=\\([A-Za-z]{1}\\))", "");
//		sentence = sentence.replaceAll("\\(loss\\)", "xxOPlossCPxx");
//		System.out.println("sentence check=" + sentence + "||");

		// COPY AS NEW METHOD THEN MAKE A LIST OF SSSS1S AND REPLACE WITH [[#
		return sentence;
	}

	public List<Integer[]> convertAlphaParaNumbAndType(String alphaNumber, boolean matchType, String typeMatch)
			throws IOException {
//		System.out.println("alphaNumber="+alphaNumber);
		int paraNumber = 0;
		// converts each to 1, 2, 3 ...
		List<Integer[]> listIntAry = new ArrayList<Integer[]>();

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
					listIntAry.add(aryInt);
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
					listIntAry.add(aryInt);
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
					listIntAry.add(aryInt);
				}
			}
		}

		type++;
		String[] alphaParenUc = { "(A)", "(B)", "(C)", "(D)", "(E)", "(F)", "(G)", "(H)", "(I)", "(J)", "(K)", "(L)",
				"(M)", "(N)", "(O)", "(P)", "(Q)", "(R)", "(S)", "(T)", "(U)", "(V)", "(W)", "(X)", "(Y)", "(Z)"// ,
//				"(AA)", "(BB)", "(CC)", "(DD)", "(EE)", "(FF)", "(GG)", "(HH)", "(II)", "(JJ)", "(KK)", "(LL)", "(MM)",
//				"(NN)", "(OO)", "(PP)", "(QQ)", "(RR)", "(SS)", "(TT)", "(UU)", "(VV)", "(WW)", "(XX)", "(YY)",
//				"(ZZ)"
		};

		if (alphaNumber.replaceAll("\\([A-Z]{1,2}\\)", "").trim().length() < 1) {
			for (int i = 0; i < alphaParenUc.length; i++) {
				if (alphaNumber.equals(alphaParenUc[i])) {
					Integer[] aryInt = { (i + 1), type };
					listIntAry.add(aryInt);
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
					listIntAry.add(aryInt);
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
					listIntAry.add(aryInt);
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

//		System.out.println("it is this type!==="+type+" alphanumber="+alphaNumber);
		if (alphaNumber.replaceAll("[0-9]{1,2}\\.", "").trim().length() < 1) {
			for (int i = 0; i < numberPeriod.length; i++) {
				if (alphaNumber.equals(numberPeriod[i])) {
					Integer[] aryInt = { (i + 1), type };
					listIntAry.add(aryInt);
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
					listIntAry.add(aryInt);
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
					listIntAry.add(aryInt);
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
					listIntAry.add(aryInt);
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
					listIntAry.add(aryInt);
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
					listIntAry.add(aryInt);
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
					listIntAry.add(aryInt);
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
					listIntAry.add(aryInt);
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
					listIntAry.add(aryInt);
				}
			}
		}

//		type++;
//		Integer[] aryInt = { (1), type };
//		listIntAry.add(aryInt);

		return listIntAry;

	}

	public List<String[]> sentenceParentChild(String sentenceTxt) throws IOException {

		String origText = sentenceTxt;
//		TreeMap<Integer, List<String[]>> map = new TreeMap<Integer, List<String[]>>();
		// if parent = then key=1, child key=2 - so I just iterate through the map - it
		// isn't much
		NLP nlp = new NLP();

		sentenceTxt = getNumeralNumberedSubSentencesInSentence(sentenceTxt);
//		System.out.println("2....sentenceTxt===" + sentenceTxt);
		List<String[]> listParaNumberMark = NLP.getAllMatchedGroupsAndStartIdxLocs(sentenceTxt,
				Pattern.compile("sssS1s.*? "));
//		NLP.printListOfStringArray("listParaNumberMark===", listParaNumberMark);
		String sIdxStr = "";
//		, typPrev = "", type = "";
//		int typVal = 0, typValPrev = 0;

		if (listParaNumberMark.size() == 0) {

			List<String[]> lst = new ArrayList<>();
			String[] ary = { "1000", origText };
			lst.add(ary);
//			System.out.println("null - returned lst. lst.size="+lst.size());
			return lst;
		}

		List<String[]> listParas = new ArrayList<>();
		// get number related to roman number/alph/nmb in para and type. Used for
		// parent/child lineage
		boolean endIsColon = false;
		String masterOrSlave = "", listNmb = "", strColon = "";
//		int idxNext = 0;
		for (int i = 0; i < listParaNumberMark.size(); i++) {
//			System.out.println("listParaNumberMark.get(i)=" + Arrays.toString(listParaNumberMark.get(i)));
			endIsColon = false;
			listNmb = listParaNumberMark.get(i)[0]; // don't trim!
			strColon = "";
			if (i + 1 < listParaNumberMark.size()) {
				strColon = sentenceTxt.substring(Integer.parseInt(listParaNumberMark.get(i)[1]),
						Integer.parseInt(listParaNumberMark.get(i + 1)[1])).trim();
				strColon = strColon.substring(strColon.length() - 2, strColon.length());
				if (strColon.contains(":")) {
					endIsColon = true;
//					 System.out.println("endIsColon="+endIsColon);
				}
			}

			sIdxStr = listParaNumberMark.get(i)[1];
//			System.out.println("listNmb=" + listNmb.trim().replaceAll("sssS1s", "") + "||");
			List<Integer[]> listI = convertAlphaParaNumbAndType(listNmb.trim().replaceAll("sssS1s", ""), false, "");
			if (listI == null || listI.size() == 0) {
//				System.out.println("a1==" + a1);
				continue;
			}

//			NLP.printListOfIntegerArray("1 listI", listI);
//			System.out.println("eIdxStr=" + sIdxStr + " listNmb=" + listNmb);

			// if a hard return precedes the # it is a para #, if not it is a clause #.

			if (nlp.getAllIndexEndLocations(listNmb, Pattern.compile("[\r\n]{1}")).size() > 0) {
				masterOrSlave = "master";
				if (endIsColon) {
					masterOrSlave = "masterColon";
				}
//				System.out.println("1.. masterOrSlave==" + masterOrSlave + " listNmb==" + listNmb.trim());
			}

			if (nlp.getAllIndexEndLocations(listNmb, Pattern.compile("[\r\n]{1}")).size() == 0) {
				masterOrSlave = "slave";
//				System.out.println("2.. masterOrSlave==" + masterOrSlave + " listNmb==" + listNmb.trim());
			}

			if (listI.size() == 1) {
				String[] aryStr = { listNmb.trim(), listI.get(0)[0] + "", listI.get(0)[1] + "", sIdxStr,
						masterOrSlave };
				listParas.add(aryStr);
			}

			if (listI.size() > 1) {
				String[] aryStr = { listNmb.trim(), listI.get(0)[0] + "", listI.get(0)[1] + "", sIdxStr,
						masterOrSlave };
				listParas.add(aryStr);

				String[] ary = { listNmb.trim(), listI.get(1)[0] + "", listI.get(1)[1] + "", sIdxStr, masterOrSlave };
				listParas.add(ary);
//				System.out.println(".add aryStr=" + Arrays.toString(aryStr));
			}
		}

//		System.out.println("sentenceTxt=" + sentenceTxt);
//		NLP.printListOfStringArray("listParas=", listParas);
		clauseText = sentenceTxt;

		return listParas;

	}

	public String getLeadInPara(String text) throws IOException {

		NLP nlp = new NLP();
		String leadInClause = "";
		int idx = 0;

		// cant be "Class B Principal Distribution Amount":" for example
		if (text.replaceAll("\":", "").indexOf(":") > 0
				&& text.substring(0, text.indexOf(":")).replaceAll("[ \r\n\\.;\\:\\-_]+", "").trim().length() > 6 && nlp
						.getAllMatchedGroups(text.substring(0, (Math.min(text.indexOf(":") + 4, text.length()))),
								Pattern.compile("(?ism)(([ \r\n^]{1}by|dated?| title|If to the.{1,14}|Attention"
										+ "|signe?d?|Name|facsimile|telecopy|telephone|mobile|email|"
										+ "Signature Guarant[oresy]{1,3}?)\\)?:|[\\d]{1,2}:[\\d}]{2})|"
										+ "((paragraphs?|clauses?|FIRST|SECOND|THIRD|FOURTH)"
										+ "(\\([A-Za-z\\d]{1,4}\\),? )+((and|or) \\([A-Za-z\\d]{1,4}\\))?)"))
						.size() == 0) {

			// Gets mached group that ignore !\": - eg a colon preceded by a quote b/c it is
			// almost always a defined term. Altho there will be exceptions where --> '": '
			// but that's extremely rare and I will allow myself not to pickup that leadin!

			// idx = nlp.getAllIndexEndLocations(text,
			// Pattern.compile("(?sm)(?<!\"):[\r\n]{3,}")).get(0);
			idx = nlp.getAllIndexEndLocations(text, Pattern.compile("(?sm)(?<!\"):")).get(0);
			leadInClause = text.substring(0, idx).trim();

			if (leadInClause.trim().length() >= 15)
				return leadInClause;

		}

		return "";

	}

	public List<String> getSolrSentence(String text) throws IOException {

//		String tmpTestStr = " determined and adjusted quarterly on the date five";
//		System.out.println("getSolrSentence text====" + text);
		List<String[]> listSentEidx = new ArrayList<String[]>();
		NLP nlp = new NLP();
		text = text.replaceAll("(?sm)(?<=[a-z]{3},?)[\\r\\n]{3,10}(?= ?[a-z]{3,10})", " ");
		List<Integer> listSentenceEnd = nlp.getAllIndexEndLocations(text, NLP.patternSentenceEnd);
		List<Integer> listMorePatterns = new ArrayList<Integer>();
		List<String> list = new ArrayList<>();
		List<String> list2 = new ArrayList<>();

		String sentence = "", sentMore = "";
		int sIdx = 0, eIdx = 0;

		int tmpSidx = 0, tmpEidx = 0;

		String stub = "";
//		boolean tmpTestTrue = false;
		for (int i = 0; i < listSentenceEnd.size(); i++) {

			sIdx = eIdx;
			eIdx = listSentenceEnd.get(i);
			sentence = text.substring(sIdx, eIdx);

//			tmpTestTrue = false;// delete boolean variable
//			if (text.contains(tmpTestStr)) {
////				 <===========delete me ========= >
//				System.out.println("i=" + i + " listSentenceEnd.size=" + listSentenceEnd.size()
//						+ "\r\nsentence=========" + sentence);
//				tmpTestTrue = true;
//			}
			if (stub.length() > 0) {
				sentence = stub + sentence;
				stub = "";
			}

			if (i + 1 < listSentenceEnd.size()
					&& sentence.replaceAll("[\r\n\t \\s" + "\\-\\_\\=" + "]+", "").length() < 5) {
				stub = sentence;
				continue;
			}
			stub = "";

			if (i + 1 == listSentenceEnd.size()
					&& sentence.replaceAll("[\r\n\t \\s" + "\\-\\_\\=" + "]+", "").length() < 5) {
				stub = sentence;
//				System.out.println("stub====" + stub);
			}

//			 System.out.println("@patternSent10 sent");
			listMorePatterns = nlp.getAllIndexEndLocations(sentence, NLP.patternSent10);
//			if (text.contains(tmpTestStr)) {
////				// <===========delete me ========= >
//				System.out.println("listMorePatterns.size()=" + listMorePatterns.size());
//			}
			if (i + 1 < listSentenceEnd.size()) {
				if (listMorePatterns.size() > 0) {
					sentMore = "";
					tmpSidx = 0;
					tmpEidx = 0;
					for (int n = 0; n < listMorePatterns.size(); n++) {
						if (n == 0) {
							tmpSidx = 0;
							tmpEidx = 0;
						}
						tmpEidx = listMorePatterns.get(n);

						if (n + 1 < listMorePatterns.size()) {
							sentMore = sentence.substring(tmpSidx, tmpEidx);
							String[] arySentEidx = { sentMore, (sIdx + tmpEidx) + "" };
							listSentEidx.add(arySentEidx);
//							if (text.contains(tmpTestStr)) {
//								System.out.println("1a. sent tmpTestStr .add ary=" + Arrays.toString(arySentEidx));
//							}
						}

						if (n + 1 == listMorePatterns.size()) {
							sentMore = sentence.substring(tmpSidx, tmpEidx);
							String[] ary2SentEidx = { sentMore, (sIdx + sentMore.length()) + "" };
							listSentEidx.add(ary2SentEidx);
//							if (text.contains(tmpTestStr)) {
//								System.out.println("2a. sent tmpTestStr .add ary=" + Arrays.toString(ary2SentEidx));
//							}
							// System.out.println("1b. sent .add ary=" + Arrays.toString(ary2SentEidx));
							sentMore = sentence.substring(tmpEidx, sentence.length());
							String[] ary3SentEidx = { sentMore, (sIdx + sentence.length()) + "" };
							listSentEidx.add(ary3SentEidx);
//							if (text.contains(tmpTestStr)) {
//								System.out.println("3a. sent tmpTestStr .add ary=" + Arrays.toString(ary3SentEidx));
//							}
							// System.out.println("1c. sent .add ary=" + Arrays.toString(ary3SentEidx));
						}
						tmpSidx = tmpEidx;
					}
				}

				// additional patterns were not found - so use default
				// mechanism. Also i+1<listSentenceEnd.size
//				System.out.println("getSolrSentence 2");
				if (listMorePatterns.size() == 0) {
					String[] arySentEidx = { sentence, eIdx + "" };
					listSentEidx.add(arySentEidx);
//					if (text.contains(tmpTestStr)) {
//						System.out.println("4a. sent tmpTestStr .add ary=" + Arrays.toString(arySentEidx));
//					}
				}
			}

			sentence = "";
//			System.out.println("getSolrSentence 3");
			if (i + 1 == listSentenceEnd.size()) {
				sentence = text.substring(sIdx, eIdx);
//				System.out.println("last sentence=="+sentence);
//				System.out.println("last sentence stub=="+stub);
				String[] arySentEidx = { sentence, eIdx + "" };
				listSentEidx.add(arySentEidx);
//				if (text.contains(tmpTestStr)) {
//					System.out.println("5a. sent tmpTestStr .add ary=" + Arrays.toString(arySentEidx));
//				}

//				 System.out.println("1e. sent .add ary=" + Arrays.toString(arySentEidx));
				sentence = text.substring(eIdx, text.length());
				String[] arySentEidx2 = { sentence, eIdx + text.length() + "" };
//				if (text.contains(tmpTestStr)) {
//					System.out.println("6a. sent tmpTestStr .add ary=" + Arrays.toString(arySentEidx2));
//				}
				listSentEidx.add(arySentEidx2);
			}
//			System.out.println("getSolrSentence 4");
		}

//		System.out.println("getSolrSentence 4b");

		List<String> listSent = new ArrayList<>();
		String text2 = "";
		int cnt = 0;
		// here I have multi sentences
		List<String> listMaster = new ArrayList<>();

		for (int i = 0; i < listSentEidx.size(); i++) {
			// System.out.println("listSentEidx=" + Arrays.toString(listSentEidx.get(i)));
			text2 = listSentEidx.get(i)[0];
			// if secHdgFound - it is not part of text - so need to add it here.
			// this runs 1 on the 1 sentence but its results need to be added to a full list
			// for each list returned. so I run through each sentence in this outer loop and
			// take results of the cleanup list to add to a new list and that ends the loop
			// and creates the list for the next setup cleanup methods.

//			 System.out.println("going to sentenceSectionCleanup");
//			System.out.println("getSolrSentence 4c");
			list = SolrPrep.sentenceSectionCleanup(text2);
//			System.out.println("getSolrSentence 4d");
//			if (text.contains(tmpTestStr)) {
//				NLP.printListOfString("after sentenceSectionCleanup", list);
//			}

			// list = SolrPrep.sentenceSubHeadingCleanup(list);

			list = SolrPrep.sentenceMultipleCleanup(list);
//			System.out.println("getSolrSentence 4e");
//			if (text.contains(tmpTestStr)) {
//				NLP.printListOfString("after sentenceMultipleCleanup", list);
//			}
			list = SolrPrep.sentenceSemiColon(list);
//			System.out.println("getSolrSentence 4f");
//			if (text.contains(tmpTestStr)) {
//				NLP.printListOfString("after sentenceSemiColon", list);
//			}
			// System.out.println("listSemiColon.size="+list.size());

//			if (text.contains(tmpTestStr))
//				NLP.printListOfString("list1.......", list);
			list2 = getSentenceAwayFromShortLines(list);
//			if (text.contains(tmpTestStr))
//				NLP.printListOfString("list2.......", list2);
//			System.out.println("getSolrSentence 4g");
			for (int a = 0; a < list2.size(); a++) {
				listMaster.add(list2.get(a));
//				if (text.contains(tmpTestStr)) {
//					System.out.println("tmpTestStr. list2.get(a)==" + list2.get(a));
//				}
			}
//			System.out.println("getSolrSentence 4h");
		}

//		System.out.println("getSolrSentence 5");

		for (int c = 0; c < listMaster.size(); c++) {

			if (listMaster.get(c).trim().length() < 1)
				continue;
			listSent.add(listMaster.get(c).trim());
//			if (text.contains(tmpTestStr)) {
//				System.out.println("tmpTestStr. listMaster.get(c)==" + listMaster.get(c));
//			}
			cnt++;
		}
//		System.out.println("getSolrSentence 6");

//		if (text.contains(tmpTestStr)) {
//			System.out.println("listSent.size==" + listSent.size() + "  tmpTestTrue. text===" + text);
////			NLP.printListOfString("....listSent===", listSent);
//		}

		return listSent;

	}

	public List<String> getSentenceAwayFromShortLines(List<String> list) throws IOException {

		StringBuilder sb = new StringBuilder();
		List<String> listSent = new ArrayList<>();
		int hardReturnCnt = 0;
		double wCnt = 0;
		double i = 0;
		double avgWordCount = 0, priorAvgWCnt = 0, avgInitCap = 0;
		double initialCapCnt = 0;
		double cnt = 0;
		String line = "";
		boolean found = false;
		String text = "";
//		boolean tmpStrhere = false;

//		String tmpString = "argin shall be determined and adjusted quarterly on the date fiv";
		for (int a = 0; a < list.size(); a++) {
			found = false;
			text = list.get(a);

			String[] textSplit = text.split("\r\n");
//			if (text.contains(tmpString)) {
//				System.out.println("1.tmpstring line===" + text);// delete tmpString after testing.
//				tmpStrhere = true;
//			}

			for (; i < textSplit.length; i++) {

				line = textSplit[(int) i];
//				if (tmpStrhere)
//					System.out.println("2.tmpstring line===" + line);// delete tmpString after testing.
				wCnt = wCnt + line.split(" ").length;
				avgWordCount = wCnt / (i + 1.0);
				initialCapCnt = (double) NLP.getAllMatchedGroupsAndStartIdxLocs(textSplit[(int) i],
						Pattern.compile("(?sm)[\r\n ](?=[A-Z]{1})")).size() + initialCapCnt;
				if (line.replaceAll("[ \t]+", "").length() > 1)
					cnt++;

				avgInitCap = (initialCapCnt / cnt);

				if (i > 4 && priorAvgWCnt < 10 && avgInitCap / avgWordCount > .4 && line.length() > 70 && cnt > 3
						&& sb.toString().length() < text.length()) {
//					System.out.println("new sent. shortline=" + line);
//					if (tmpStrhere)
//						System.out.println("3.tmpstring line===" + line);// delete tmpString after testing.

					listSent.add(sb.toString());
//					if (tmpStrhere)
//						System.out.println("4.tmpstring add sb.to===" + sb.toString());

					if (hardReturnCnt > 0) {
						listSent.add(text.substring(sb.toString().length() - hardReturnCnt * 2, text.length()));
					}
					if (hardReturnCnt == 0)
						listSent.add(text.substring(sb.toString().length(), text.length()));
//					if (tmpStrhere)
//						System.out.println(
//								"5.tmpstring add sb.to===" + text.substring(sb.toString().length(), text.length()));

					found = true;
					sb = new StringBuilder();
					hardReturnCnt = 0;
					break;
				} else {
					sb.append(line + "\r\n");
					hardReturnCnt++;
//					if (tmpStrhere)
//						System.out.println("6.tmpstring line=" + line);
				}
				priorAvgWCnt = avgWordCount;
//				System.out.println("initialCapCnt==" + initialCapCnt);

//				if (tmpStrhere)
//					System.out.println("4.tmpstring ....i=" + i + " line=" + line + "\r\nwCnt avg=" + avgWordCount
//							+ " avgInitCap=" + avgInitCap + " line wCnt=" + textSplit[(int) i].split(" ").length);
			}

			if (!found) {
//				if (tmpStrhere)
//					System.out.println("5.tmpstring addingline===" + list.get(a));// delete tmpString after testing.
				listSent.add(list.get(a));
			}
		}

//		NLP.printListOfString("listSent====", listSent);
		return listSent;

	}

	public List<String> providedClause(String text) {

		NLP nlp = new NLP();
		List<String> listProvided = new ArrayList<String>();

		Pattern patternProvided = Pattern.compile("[,;]{1}(?= provided(;?,?( (however|further),?))? that)");
		List<Integer> list = nlp.getAllIndexStartLocations(text, patternProvided);
		if (list.size() == 1) {
			listProvided.add(text.substring(0, list.get(0) + 1));
			listProvided.add(text.substring(list.get(0) + 1, text.length()));
		}

		return listProvided;
	}

	public List<String> semiColonMiddleClause(String text) {

		NLP nlp = new NLP();
		List<String> listProvided = new ArrayList<String>();

		Pattern patternProvided = Pattern.compile(";");
		String t1, t2;
		List<Integer> list = nlp.getAllIndexStartLocations(text, patternProvided);
		if (list.size() == 1) {
			t1 = text.substring(0, list.get(0) + 1);
			t2 = text.substring(list.get(0) + 1, text.length());
//			listProvided.add(t1);
//			listProvided.add(t2);
		}

		return listProvided;
	}

	public static String getSentenceLeadIn(String text) throws IOException {

		NLP nlp = new NLP();
		String leadInClause = "";
		int idx = 0;

		if (text.replaceAll("\":", "").indexOf(":") > 0
				&& nlp.getAllIndexEndLocations(text, Pattern.compile("(?sm)(?<!\"):")).size() > 0

				&& text.substring(0, text.indexOf(":")).replaceAll("[ \r\n\\.;\\:\\-_]+", "").trim().length() > 9

				&& nlp.getAllMatchedGroups(text.substring(0, (Math.min(text.indexOf(":") + 4, text.length()))),
						Pattern.compile("(?ism)(([ \r\n^]{1}dated?| title|If to the.{1,14}|Attention"
								+ "|signe?d?|Name|facsimile|telecopy|telephone|mobile|email|"
								+ "Signature Guarant[oresy]{1,3}?)\\)?:|[\\d]{1,2}:[\\d}]{2})|"
								+ "((paragraphs?|clauses?|FIRST|SECOND|THIRD|FOURTH)"
								+ "(\\([A-Za-z\\d]{1,4}\\),? )+((and|or) \\([A-Za-z\\d]{1,4}\\))?)"))
						.size() == 0
				&& nlp.getAllIndexEndLocations(text, Pattern.compile(
						"(?ism)(title|Signature Guarantee.{0,1}|attention|facsimile|email|signature|box.{0,1}|box below.{0,1}):"))
						.size() == 0) {

			// Gets mached group that ignore !\": - eg a colon preceded by a quote b/c it is
			// Pattern.compile("(?sm)(?<!\"):[\r\n]{3,}")).get(0);
			idx = nlp.getAllIndexEndLocations(text, Pattern.compile("(?sm)(?<!\"):")).get(0);
			leadInClause = text.substring(0, idx).trim();
		}

		if (leadInClause.trim().length() >= 11)
			return leadInClause;

		return leadInClause;

	}

	public static String getGoverningLaw(String text) throws IOException, SQLException {
//		long starttime = System.currentTimeMillis();

		NLP nlp = new NLP();
		Pattern patternStatesAndCountry = Pattern.compile(
				"((?ism)ALASKA|ALABAMA|ARKANSAS|AMERICAN[\r\n ]{0,3}SAMOA|ARIZONA|CALIFORNIA|COLORADO|CONNECTICUT|DISTRICT[\r\n ]{0,3}OF[\r\n ]{0,3}"
						+ "COLUMBIA|DELAWARE|FLORIDA|FEDERATED[\r\n ]{0,3}STATES[\r\n ]{0,3}OF[\r\n ]{0,3}MICRONESIA|GEORGIA|GUAM|HAWAII|IOWA|IDAHO"
						+ "|ILLINOIS|INDIANA|KANSAS|KENTUCKY|LOUISIANA|MASSACHUSETTS|MARYLAND|MAINE|MARSHALL[\r\n ]{0,3}ISLANDS|MICHIGAN|MINNESOTA"
						+ "|MISSOURI|NORTHERN[\r\n ]{0,3}MARIANA[\r\n ]{0,3}ISLANDS|MISSISSIPPI|MONTANA|NORTH[\r\n ]{0,3}CAROLINA|NORTH[\r\n ]{0,3}DAKOTA"
						+ "|NEBRASKA|NEW[\r\n ]{0,3}HAMPSHIRE|NEW[\r\n ]{0,3}JERSEY|NEW[\r\n ]{0,3}MEXICO|NEVADA|NEW[\r\n ]{0,3}YORK|OHIO|OKLAHOMA|OREGON"
						+ "|PENNSYLVANIA|PUERTO[\r\n ]{0,3}RICO|PALAU|RHODE[\r\n ]{0,3}ISLAND|SOUTH[\r\n ]{0,3}CAROLINA|SOUTH[\r\n ]{0,3}DAKOTA|TENNESSEE"
						+ "|TRUST[\r\n ]{0,3}TERRITORIES|TEXAS|UTAH|WEST[\r\n ]{0,3}VIRGINIA|VIRGIN[\r\n ]{0,3}ISLANDS|VERMONT|WASHINGTON|WISCONSIN|VIRGINIA"
						+ "|WYOMING"
						+ "|England|English|Whales|Ireland|Spain|Germany|Japan|China|Hong Kong|South Korea|Malayisa|Vietnam|Singapore|Australia"
						+ "|India|China|Russia" + "|New South Whales"
						+ "|Liechtenstein|Italy|Israel|France|UK|U\\.K\\.|Luxembourg|Swiss|Canada|Ontario|Quebec|Mexico|Brazil|Argentina"
						+ "|Bolivia|Uruguay|Suriname|Paraguay|Falkland|Chile|Ecuador|Uruguay|Guatemala|Trinidad|Guyana|Venezuela|Guinia|Peru|Panama|Hondura"
						+ "|Puerto Rico|Dominican Republic" + "|British Columbia|Cayman|Bermuda|Guernsey|Jersey"
						+ "|Liberia|Virgina Islands"
						+ "|Afghanistan|Albania|Algeria|Andorra|Angola|Antigua and Barbuda|Argentina|Armenia|Australia|Austria|Azerbaijan|Bahamas|Bahrain"
						+ "|Bangladesh|Barbados|Belarus|Belgium|Belize|Benin|Bhutan|Bolivia|Bosnia and Herzegovina|Botswana|Brazil|Brunei|Bulgaria"
						+ "|Burkina Faso|Burundi|C.{1,2}te d.{1,2}Ivoire|Cabo Verde|Cambodia|Cameroon|Canada|Central African Republic|Chad|Chile|China"
						+ "|Colombia|Comoros|Congo|Costa Rica|Croatia|Cuba|Cyprus|Czechia|Czech Republic|Denmark|Djibouti|Dominica|Dominican Republic|Ecuador"
						+ "|Egypt|El Salvador|Equatorial Guinea|Eritrea|Estonia|Eswatini| Swaziland|Ethiopia|Fiji|Finland|France|Gabon|Gambia|Georgia|Germany"
						+ "|Ghana|Greece|Grenada|Guatemala|Guinea|Guinea-Bissau|Guyana|Haiti|Holy See|Honduras|Hungary|Iceland|India|Indonesia|Iran|Iraq"
						+ "|Ireland|Israel|Italy|Jamaica|Japan|Jordan|Kazakhstan|Kenya|Kiribati|Kuwait|Kyrgyzstan|Laos|Latvia|Lebanon|Lesotho|Liberia"
						+ "|Libya|Liechtenstein|Lithuania|Luxembourg|Madagascar|Malawi|Malaysia|Maldives|Mali|Malta|Marshall Islands|Mauritania|Mauritius"
						+ "|Mexico|Micronesia|Moldova|Monaco|Mongolia|Montenegro|Morocco|Mozambique|Myanmar|Burma |Namibia|Nauru|Nepal|Netherlands"
						+ "|New Zealand|Nicaragua|Niger|Nigeria|North Korea|Macedonia|Norway|Oman|Pakistan|Palau|Palestine State|Panama|Papua New Guinea"
						+ "|Paraguay|Peru|Philippines|Poland|Portugal|Qatar|Romania|Russia|Rwanda|Saint Kitts| Nevis|Saint Lucia"
						+ "|Saint Vincent.{1,10}Grenadines|Samoa|San Marino|Sao Tome.{1,6}Principe|Saudi Arabia|Senegal|Serbia|Seychelles|Sierra Leone"
						+ "|Singapore|Slovakia|Slovenia|Solomon Islands|Somalia|South Africa|South Korea|South Sudan|Spain|Sri Lanka|Sudan|Suriname"
						+ "|Sweden|Switzerland|Syria|Tajikistan|Tanzania|Thailand|Timor.{1,2}Leste|Togo|Tonga|Trinidad.{1,6}Tobago|Tunisia|Turkey"
						+ "|Turkmenistan|Tuvalu|Uganda|Ukraine|United Arab Emirates|United Kingdom|Uruguay|Uzbekistan|Vanuatu|Venezuela|Vietnam|Yemen"
						+ "|Zambia|Zimbabwe)|Federal|FEDERAL");
		Pattern patternGoverning = Pattern
				.compile("(?ism)governed|governs|governing|(interpret|construed).{1,30}accordan");
		Pattern patternLaw = Pattern.compile("(?ism) laws?[\r\n ]|Commonwealth|Republic");

		List<Integer> listGoverning = nlp.getAllIndexEndLocations(text, patternGoverning);
//		System.out.println("listGoverning.size=" + listGoverning.size());
//		System.out.println("millis=" + (System.currentTimeMillis() - starttime));
		String para = "", stateLaw = "";
		for (int i = 0; i < listGoverning.size(); i++) {
			para = text.substring(Math.max(0, listGoverning.get(i) - 200),
					Math.min(text.length(), listGoverning.get(i) + 200));
			if (nlp.getAllMatchedGroups(para, patternStatesAndCountry).size() > 0
					&& nlp.getAllMatchedGroups(para, patternLaw).size() > 0
					&& nlp.getAllIndexEndLocations(para, Pattern.compile(
							"Agreement|Contract|Document|Indenture|Plan of Merger|AGREEMENT|CONTRACT|DOCUMENT|INDENTURE|PLAN OF MERGER"))
							.size() > 0) {
				stateLaw = nlp.getAllMatchedGroups(para, patternStatesAndCountry).get(0);
//				System.out.println("stateLaw=" + stateLaw);
//				System.out.println(para);
				return stateLaw;
			}
		}

		for (int i = 0; i < listGoverning.size(); i++) {
			para = text.substring(Math.max(0, listGoverning.get(i) - 200),
					Math.min(text.length(), listGoverning.get(i) + 200));
			if (nlp.getAllMatchedGroups(para, patternStatesAndCountry).size() > 0
					&& nlp.getAllMatchedGroups(para, patternLaw).size() > 0 && nlp.getAllIndexEndLocations(para,
							Pattern.compile("NOTE|SECURIT|CERTIFICAT|PLAN|Plan|Note|Securit")).size() > 0) {
				stateLaw = nlp.getAllMatchedGroups(para, patternStatesAndCountry).get(0);
//				System.out.println("stateLaw=" + stateLaw);
//				System.out.println(para);
				return stateLaw;
			}
		}

		return "governing law not found";

	}

	public TreeMap<String, List<String[]>> getSolrMap(List<String[]> listTmp, String text) throws IOException {

		GoLaw gl = new GoLaw();
		NLP nlp = new NLP();
		// listTmp is the list of headings, with the sIdx and eIdx equal to the start
		// and end of the body of text that relates to that heading

		TreeMap<String, List<String[]>> map = new TreeMap<String, List<String[]>>();

//		NLP nlp = new NLP();
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize" + ", ssplit");

		String para = "", strn = "";
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		List<String[]> listArray = new ArrayList<String[]>();

//		long startTime = System.currentTimeMillis();

//		List<String> listHdgs = new ArrayList<String>();
		String hdg = "", hdg2 = "", typ = "", typP = ""
//				, pgNoAndMarker = ""
		;
		int wCnt = 0;
		int chunkCnt = 0;

		String hdgLen = "", hdgLen2 = "";
		List<String[]> list = new ArrayList<String[]>();
		boolean paraIsSent = false, thereIsASubClauseOrClause = false;

		int hdL = 0, hdLpr = -1, sIdx = 0, eIdx = 0, sIdxNext = 0, eIdxNext = 0, sIdxPrior = 0, eIdxPrior = 0, cnt = 0,
				round = 10000, addThisSent = 0, addThisClause = 0, addThisSubClause = 0;

//		PrintWriter pwPara = new PrintWriter(new File("c:/temp/11. getSolrM_Para.txt"));
//		StringBuilder sbPara = new StringBuilder();
//		PrintWriter pwSnt = new PrintWriter(new File("c:/temp/11. getSolrM_Sent.txt"));
//		StringBuilder sbSent = new StringBuilder();
//		PrintWriter pwClaus = new PrintWriter(new File("c:/temp/11. getSolrM_Clau.txt"));
//		StringBuilder sbClaus = new StringBuilder();
//		PrintWriter pwSubCl = new PrintWriter(new File("c:/temp/11. getSolrM_SubCl.txt"));
//		StringBuilder sbSubCl = new StringBuilder();
//		PrintWriter pwProvCl = new PrintWriter(new File("c:/temp/11. getSolrM_ProvCl.txt"));
//		StringBuilder sbProvCl = new StringBuilder();
//		PrintWriter pw30 = new PrintWriter(new File("c:/temp/11. getSolrM_sent30.txt"));
//		StringBuilder sb30 = new StringBuilder();
//		PrintWriter pw40 = new PrintWriter(new File("c:/temp/11. getSolrM_sent40.txt"));
//		StringBuilder sb40 = new StringBuilder();
//		PrintWriter pw50 = new PrintWriter(new File("c:/temp/11. getSolrM_sent50.txt"));
//		StringBuilder sb50 = new StringBuilder();

		TreeMap<Integer, String> mapRemoveDupes = new TreeMap<Integer, String>();
		String tmpDup = "", tmpDup2 = "";
		int paraMinWcnt = 300;
		for (int i = 0; i < listTmp.size(); i++) {
			// if sIdx - eIdxPrior >3 then record
//			System.out.println("111listTmp=" + Arrays.toString(listTmp.get(i)));

			sIdx = Integer.parseInt(listTmp.get(i)[0]);
			eIdx = Integer.parseInt(listTmp.get(i)[1]);

			if (i == 0) {
				String[] ary = { "0", (sIdx - 1) + "" };
				list.add(ary);
			}
			if (i > 0 && sIdx - eIdxPrior > 2) {
				String[] ary = { (eIdxPrior + 1) + "", (sIdx - 1) + "" };
				list.add(ary);
			}

			list.add(listTmp.get(i));
			sIdxPrior = sIdx;
			eIdxPrior = eIdx;
		}

//		System.out.println(
//				"getting solr map first loop this many seconds==" + (startTime - System.currentTimeMillis()) / 1000);
//		startTime = System.currentTimeMillis();

		String[] ay = { eIdx + "", text.length() + "" };
		list.add(ay);

//		NLP.printListOfStringArray("1.list ay=", list);

		sIdx = 0;
		eIdx = 0;
		sIdxNext = 0;
		eIdxNext = 0;
		sIdxPrior = 0;
		eIdxPrior = 0;
//		int sIdxRolling =0, eIdxRolling =0;
//		String origText ="";

		cnt = 0;
		round = 10000;
		addThisSent = 0;
		addThisClause = 0;
		addThisSubClause = 0;

		int sentCnt = 0, clauCnt = 0, cntProv = 0;
		String sentLead = "", clauLead = "", paraLead = "", tmpStr = "";
//		boolean parentLeadisSentLead = false, sentLeadisClauLead = false;

		List<String[]> listTemp = new ArrayList<String[]>();

		for (int i = 0; i < list.size(); i++) {// list text chunks
			sIdx = Integer.parseInt(list.get(i)[0]);
			eIdx = Integer.parseInt(list.get(i)[1]);
			if (eIdx <= sIdx)
				continue;
			listTemp.add(list.get(i));
		}

//		NLP.printListOfStringArray("2.listTemp ay=", list);
		list = new ArrayList<String[]>();
		list = listTemp;// TODO pickup multi-thread here - each chunk is a thread. Everything preceding
						// here is prep of list of chunks

		int cnk = 0, lastPutCl = 0, lastPutSubCl = 0;

		// this loops remove sentence ends within a paragraph so that stanford parser
		// treats the entire paragraph as a sentence. I use this b/c I don't have a
		// parser to identify a paragraph and stanford does it pretty well using this
		// technique.

//		PrintWriter pw = new PrintWriter(new File("c:/temp/1. create solr file para.txt"));

		for (int i = 0; i < list.size(); i++) {
//			System.out.println("2.list===" + Arrays.toString(list.get(i)));
			// list text chunks TODO this loop cuts the chnks into
			// sect/para/sents/clause etc - potential candidate for M/T.
//			parentLeadisSentLead = true;
//			sentLeadisClauLead = true;
			sIdx = Integer.parseInt(list.get(i)[0]);
			eIdx = Integer.parseInt(list.get(i)[1]);
			hdgLen2 = (eIdx - sIdx) + "";

			if (eIdx <= sIdx)
				continue;
			eIdx = Math.min(text.length(), eIdx);
			if(eIdx>text.length() ||eIdx<sIdx)
				continue;
			strn = text.substring(sIdx, eIdx);
			if (strn.replaceAll("[^A-Za-z]+", "").length() < 1 || eIdx - sIdx < 3) {
//				System.out.println("chunkCnt="+chunkCnt+"..i=" + i + " sIdx=" + sIdx + " eIdx=" + eIdx + " txt==" + strn);
				continue;
			}
			chunkCnt = (round + cnk++);// each list.get(i) can be multi-threaded by sending through the below. That is
			// then sent to createSolrFile were json is created.
			// System.out.println("sIdx="+sIdx+" eIdx="+eIdx+" text.len="+text.length());

			// either the ary length is 2 (sIdx,eIdx) or 3 (sIdx,eIdx,hdg) or 4
			// (sIdx,eIdx,hdg,hdg2)
			hdg = "";
			hdg2 = "";
			strn = strn.replaceAll("\\. ", "xZxZ1").replaceAll("\\.  ", "xZxZ2")
					.replaceAll("Regulation S", "Regulation_S").replaceAll("\\.\r\n(?=[A-Z]{1})", "aZaZ")
					.replaceAll("\\.\" ", "yZyZ").replaceAll("(?<=\\d)\\. (?=[\\dA-Za-z]{1})", "bZbZ1")
					.replaceAll("(?<=\\d)\\.  (?=[\\dA-Za-z]{1})", "bZbZ2").replaceAll("Qx|XQ", "");

//			listHdgs = nlp.getAllMatchedGroups(str,
//					Pattern.compile("<(exh|sec|def|mHd|hdg|sub)>.*?</(exh|sec|def|mHd|hdg|sub)>"));

			if (list.get(i).length > 2) {
//				System.out.println("ary>2 ....list.get(i)... ary=" + Arrays.toString(list.get(i)));
				hdL = Integer.parseInt(list.get(i)[1]);
				if (hdLpr < hdL) {
//					System.out.println("hdL=" + hdL + " hdLpr=" + hdLpr + " 1b. ary>3 ....list.get(i)... ary="
//							+ Arrays.toString(list.get(i)));
					hdgLen = "" + (Integer.parseInt(list.get(i)[1]) - Integer.parseInt(list.get(i)[0]));
					hdLpr = Integer.parseInt(list.get(i)[1]);
				}

//				System.out.println("hdg=" + hdg + " hdgLen=" + hdgLen);
				hdg = list.get(i)[2].replaceAll("Regulation_S", "Regulation S").replaceAll("(?sm)(aZaZ)+", ". \r\n")
						.replaceAll("(?sm)(xZxZ1|bZbZ1)+", ". ").replaceAll("(?sm)(xZxZ2|bZbZ2)+", ".  ")
						.replaceAll("(?sm)(yZyZ)+", ".\" ").replaceAll("[ ]+", " ").replaceAll("(sssS1s)", "")
						.replaceAll("xxOP", "(").replaceAll("CPxx", ")").replaceAll("(xxPD)+", ".")
						.replaceAll("xxOB", "[").replaceAll("CBxx", "]").replaceAll("OBCB", "[]")
						.replaceAll("CBOB", "][").replaceAll(",? consecutive=.*?$", "").replaceAll("Qx|XQ", "").trim();
				if (!hdg.contains("hdgLen=")) {
					hdg = hdg + "||hdgLen=" + hdgLen + "\r\n";
				}

			}

			if (list.get(i).length > 3) {
//				System.out.println("1a. ary>3 ....list.get(i)... ary=" + Arrays.toString(list.get(i)));
//				System.out.println("list.len>3?? hdgLen=" + hdgLen);
				hdg2 = list.get(i)[3].replaceAll("Regulation_S", "Regulation S").replaceAll("(?sm)(aZaZ)+", ". \r\n")
						.replaceAll("(?sm)(xZxZ1|bZbZ1)+", ". ").replaceAll("(?sm)(xZxZ2|bZbZ2)+", ".  ")
						.replaceAll("(?sm)(yZyZ)+", ".\" ").replaceAll("[ ]+", " ").replaceAll("(sssS1s)", "")
						.replaceAll("xxOP", "(").replaceAll("CPxx", ")").replaceAll("(xxPD)+", ".")
						.replaceAll("xxOB", "[").replaceAll("CBxx", "]").replaceAll("OBCB", "[]")
						.replaceAll("CBOB", "][").replaceAll(",? consecutive=.*?$", "").replaceAll("Qx|XQ", "").trim();
				if (!hdg2.contains("hdgLen=")) {
					hdg2 = hdg2 + "||hdgLen=" + hdgLen2 + "\r\n";
				}
			}

//			if (hdg2.length() == 0) {
//				for (int k = 0; k < listSpanHdgs.size(); k++) {
//					if (sIdx >= Integer.parseInt(listSpanHdgs.get(k)[0])
//							&& eIdx <= Integer.parseInt(listSpanHdgs.get(k)[1])) {
//						System.out
//								.println("this is main hdg==" + listSpanHdgs.get(k)[2] + "\r\nfor this subHdg=" + hdg);
//					}
//				}
//			}

			// unfortunately I can't retain the location of the pg # or the pg#.
//			if (nlp.getAllMatchedGroups(str, pPgMarker).size() > 0) {
//				pgNoAndMarker = nlp.getAllMatchedGroups(str, PgMarker).get(0);
//			}

			Annotation annotation = new Annotation(strn);
			pipeline.annotate(annotation);
			List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
			List<String> listSent = new ArrayList<String>();
			List<String> listClause = new ArrayList<String>();
			List<String> listSubClause = new ArrayList<String>();
			List<String> listProvided = new ArrayList<String>();

			/*
			 * fields save are-
			 * [0]para=,sent=,clau=,subCl=,paraLeadPlusSent=,sentLeadPlusClau,
			 * clauLeadPlusSubCl [1]h1 [2]h2 [3]paraLead,sentLead,clauLead
			 */

			cnt = 0;
			for (CoreMap sentence : sentences) {
				paraIsSent = false;
				thereIsASubClauseOrClause = false;

//				System.out.println("at stanford. sentence.."+sentence.toString()+"|||||");
//				System.out.println("this many hard returns exists");
				para = sentence.toString();

//				if (para.contains("argin shall be determined and adjusted quarterly on the date five"))
//					System.out.println("1.para.....==" + para + "||end");

				para = para.replaceAll("Regulation_S", "Regulation S").replaceAll("(?sm)(aZaZ)+", ". \r\n")
						.replaceAll("(?sm)(xZxZ1|bZbZ1)+", ". ").replaceAll("(?sm)(xZxZ2|bZbZ2)+", ".  ")
						.replaceAll("(?sm)(yZyZ)+", ".\" ").replaceAll("(sssS1s)", "")
//						.replaceAll("xxOP", "(")
//						.replaceAll("CPxx", ")")
//						.replaceAll("(xxPD)+", ".").replaceAll("xxOB", "[")
//						.replaceAll("CBxx", "]")
						.replaceAll("OBCB", "[]").replaceAll("CBOB", "][").replaceAll("[ ]+", " ")
						.replaceAll("Qx|XQ", "").trim();
				wCnt = para.split(" ").length;

//				if (para.contains("argin shall be determined and adjusted quarterly on the date five"))
//					System.out.println("2.para.....==" + para + "||end");

//				System.out.println("getSolrSentence");
				listSent = getSolrSentence(para);// main method - determines sentences.
//				System.out.println("end getSolrSentence");
//				NLP.printListOfString("golaw listsent===", listSent);
				// from each paragraph a list of sentences, clauses and subclauses are obtained
				// together with lead-ins and reconstituted sentences from lead-ins - typ=30,40,
				// and 50.

//				boolean temp = false;
//				if (para.contains("Buyer desires to purchase and the Company desires to"))
//					temp = true;
//				if (temp) {
//					System.out.println("# of sents=" + listSent.size() + "\r\nbuyer desires===" + para);
//					NLP.printListOfString("listSent===", listSent);
//				}
				if (wCnt < paraMinWcnt) {
					cnt++;

					if (listSent.size() > 1) {
						tmpDup = para
								.replaceAll("(\r\n)" + "(" + "\\([A-Za-z]{1,4}[\\.\\)]{1}|\\([\\d]{1,2}[\\.\\)]{1}"
										+ ")" + "( ?[\r\n]+)", "$2 ")
								.replaceAll("[\r\n]{6,}", "\r\n\r\n").replaceAll("[ ]+", " ");
						// 0=para,1=hdg1,2=hdg2,3=typ,4="",5=paraCnt,6=solrSecCnt
						String[] array = {
								"para=" + para
										.replaceAll(
												"(\r\n)" + "(" + "\\([A-Za-z]{1,4}[\\.\\)]{1}|\\([\\d]{1,2}[\\.\\)]{1}"
														+ ")" + "( ?[\r\n]+)",
												"$2 ")
										.replaceAll("[\r\n]{6,}", "\r\n\r\n").replaceAll("[ ]+", " "),
								hdg, hdg2, "typ=1", "", "paraCnt=" + (cnt) + "", "chunkCnt=" + chunkCnt };

//						if (para.contains("argin shall be determined and adjusted quarterly on the date five")) {
//							System.out.println("3.para.....==" + para
//									.replaceAll("(\r\n)" + "(" + "\\([A-Za-z]{1,4}[\\.\\)]{1}|\\([\\d]{1,2}[\\.\\)]{1}"
//											+ ")" + "( ?[\r\n]+)", "$2 ")
//									.replaceAll("[\r\n]{6,}", "\r\n\r\n").replaceAll("[ ]+", " ") + "||end");
//						}

						listArray = new ArrayList<String[]>();
						listArray.add(array);

//						System.out.println(".put==" + Arrays.toString(array));
						if (!mapRemoveDupes.containsKey(tmpDup.hashCode() + sIdx)
						// sometimes the same sentence is in 2 places.
						// sometimes it is in one place, but has slightly different sIdx

						) {
							map.put(chunkCnt + "-" + (cnt + round) + "-" + round + "-" + round + "-" + round,
									listArray);
							mapRemoveDupes.put(tmpDup.hashCode() + sIdx, "");
//						sbPara.append(chunkCnt + "-" + (cnt + round) + "-" + round + "-" + round + "-" + round + "\r\n"
//								+ Arrays.toString(array));
							// this is the uniq solr doc id being created
						}
					}
				}

				if (null == listSent || listSent.size() == 0) {
//					System.out.println("no sentences but this is para listSent .add==" + para);
					listSent.add(para);
				}

				if (listSent.size() == 1) {
					paraIsSent = true;
				}

				paraLead = "";

				tmpStr = "";

				for (int z = 0; z < Math.min(2, listSent.size()); z++) {// lead can really only be in 1st sent.
					tmpStr = listSent.get(z).trim().replaceAll("(sssS1s)", "")
//							.replaceAll("xxOP", "(")
//							.replaceAll("CPxx", ")").replaceAll("(xxPD)+", ".")
							.replaceAll("xxOB", "[").replaceAll("CBxx", "]").replaceAll("OBCB", "[]")
							.replaceAll("CBOB", "][");
//					if (para.contains("argin shall be determined and adjusted quarterly on the date five")) {
//						System.out.println("1.tmpStr....==" + tmpStr);
//					}
//					if(tmpStr.indexOf("\r\n")>0) {
//						tmpStr = tmpStr.substring(0,tmpStr.indexOf("\r\n"));
//					}

//					if (nlp.getAllIndexEndLocations(tmpStr, Pattern.compile("\\:")).size() < 2) {
					paraLead = getSentenceLeadIn(tmpStr);
//					}

					if (paraLead.length() > 5) {
						paraLead = paraLead.trim();
//						System.out.println("para leadin z=" + z + " ldStr=" + paraLead);
						break;
					}
				}

				tmpStr = "";
				for (int n = 0; n < listSent.size(); n++) {
//					if (para.contains("argin shall be determined and adjusted quarterly on the date five")) {
//						System.out.println(" listSent.get(n).==" + listSent.get(n));
//					}
					tmpStr = listSent.get(n).replaceAll("(sssS1s)", "")
//							.replaceAll("xxOP", "(").replaceAll("CPxx", ")")
//							.replaceAll("(xxPD)+", ".")
							.replaceAll("xxOB", "[").replaceAll("CBxx", "]").replaceAll("OBCB", "[]")
							.replaceAll("CBOB", "][");
//					if (para.contains("argin shall be determined and adjusted quarterly on the date five")) {
//						System.out.println("2.tmpStr....==" + tmpStr);
//					}

					sentLead = getClauseLeadin(tmpStr);
					if (null == sentLead) {
						sentLead = "";
					}

//					if (wCnt >= paraMinWcnt) {
//						paraIsSent = true;
//						cnt++;
////						System.out.println("para>500");
//					}
					if (listSent.size() <= 1 || wCnt >= paraMinWcnt) {
						paraIsSent = true;
						cnt++;
					}
//---- here is where I see how clauses are parsed
					typ = "typ=3";
					typP = "typ=30";
					if (paraIsSent) {
						typ = "typ=1,3";
						typP = "typ=10,30";
					}
					// 0=sent,1=hdg1,2=hdg2,3=typ,4=paraLead,5=paraCnt,6=solrSecCnt
					String[] ary = { "sent=" + tmpStr.replaceAll("[\r\n]+", " ").replaceAll("[ ]+", " "), hdg, hdg2,
							typ, "paraLead=" + paraLead, "paraCnt=" + (cnt + ""), "sentCnt=" + (sentCnt++),
							"chunkCnt=" + chunkCnt };
					tmpDup2 = tmpStr.replaceAll("[\r\n]+", " ").replaceAll("[ ]+", " ") + paraLead;
//					if (tmpStr.contains("argin shall be determined and adjusted quarterly on the date five"))
//						System.out
//								.println("sent.....put1==" + tmpStr.replaceAll("[\r\n]+", " ").replaceAll("[ ]+", " "));

//					System.out.println(".put2=="+Arrays.toString(ary2));
					// 0=txt,1=hdg1,2=hdg2,3=typ,4=paraLead,5=paraCnt,6=solrSecCnt

					listArray = new ArrayList<String[]>();
					listArray.add(ary);

//					parentLeadisSentLead = true;
					if (paraLead.length() > 0 && n > 0
					// 1st sent is the leadin so can't append to itself
					// remove cases where lead and sent txt are equal (don't join lead+txt)==>
							&& !paraLead.replaceAll("[^A-Za-z ]", "").replaceAll("[ ]+", " ").trim()
									.equals(tmpStr.replaceAll("leadin=", "").replaceAll("[^A-Za-z ]", "")
											.replaceAll("[ ]+", " ").trim())) {

						tmpDup = paraLead + " "
								+ tmpStr.replaceAll("[\r\n]+", " ").replaceAll("leadin=", "").replaceAll("[ ]+", " ")
								+ paraLead;

						String[] ary2 = {
								"paraLeadPlusSent=" + paraLead + " "
										+ tmpStr.replaceAll("[\r\n]+", " ").replaceAll("leadin=", "").replaceAll("[ ]+",
												" "),
								hdg, hdg2, typP, "paraLead=" + paraLead, "paraCnt=" + (cnt + ""),
								"sentCnt=" + (sentCnt), "chunkCnt=" + chunkCnt };

						if (!mapRemoveDupes.containsKey(tmpDup.hashCode() + sIdx) && nlp
								.getAllIndexEndLocations(paraLead + " " + tmpStr.replaceAll("[\r\n]+", " ")
										.replaceAll("leadin=", "").replaceAll("[ ]+", " "), Pattern.compile(":"))
								.size() < 2) {
							List<String[]> listArray2 = new ArrayList<String[]>();
							listArray2.add(ary2);
							map.put(chunkCnt + "-" + (cnt + round) + "-" + (addThisSent + round) + "-" + round + "-"
									+ round, listArray2);
							mapRemoveDupes.put(tmpDup.hashCode() + sIdx, "");
						}

//						sbPara.append(chunkCnt + "-" + (cnt + round) + "-" + (addThisSent + round) + "-" + round + "-"
//								+ round + "\r\n" + Arrays.toString(ary2));
						cnt++;
					}

					if (nlp.getAllIndexEndLocations(paraLead + " "
							+ tmpStr.replaceAll("[\r\n]+", " ").replaceAll("leadin=", "").replaceAll("[ ]+", " "),
							Pattern.compile("\\:")).size() > 1 && !paraLead.contentEquals(tmpStr)) {
//						System.out.println("2 ':' - paraLead=="+paraLead+" tmpStr="+tmpStr+"||");
						paraLead = "";
					}

					if (paraIsSent) {
//						addThisSent++;
						addThisSent = n + 1;
					} else {
//						addThisSent++;
						addThisSent = n + 1;
					}

//					if (tmpDup2.contains(tmpTestStr)) {
//						System.out.println("tmpDup2===" + tmpDup2);
//						NLP.printListOfStringArray("typ=" + typ + " tmpDup2 listArray-==-", listArray);
//					}
					if (!mapRemoveDupes.containsKey(tmpDup2.hashCode() + sIdx)) {
						map.put(chunkCnt + "-" + (cnt + round) + "-" + (sentCnt + round)
//								+(addThisSent + round) 
								+ "-" + round + "-" + round, listArray);
						mapRemoveDupes.put(tmpDup2.hashCode() + sIdx, "");
					}

//					sbPara.append(chunkCnt + "-" + (cnt + round) + "-" + (addThisSent + round) + "-" + round + "-"
//							+ round + "\r\n" + Arrays.toString(ary));

					listClause = getSolrClause(listSent.get(n));
					clauCnt = 0;
					if (null != listClause && listClause.size() > 0)
						for (int c = 0; c < listClause.size(); c++) {
							tmpStr = listClause.get(c).replaceAll("(sssS1s)", "")
//									.replaceAll("xxOP", "(")
//									.replaceAll("CPxx", ")").replaceAll("(xxPD)+", ".")
									.replaceAll("xxOB", "[").replaceAll("CBxx", "]").replaceAll("OBCB", "[]")
									.replaceAll("CBOB", "][");

							clauLead = getClauseLeadin(tmpStr);
							if (null == clauLead) {
								clauLead = "";
							}

							String[] arry = {
									"clau=" + tmpStr.replaceAll("((clause|leadin)=)+", "").replaceAll("[\r\n]+", " ")
											.replaceAll("[ ]+", " "),
									hdg, hdg2, "typ=4", "sentLead=" + sentLead.replaceAll("leadin=", "") };
//							System.out.println(".put4==" + Arrays.toString(arry));
							tmpDup = tmpStr.replaceAll("((clause|leadin)=)+", "").replaceAll("[\r\n]+", " ")
									.replaceAll("[ ]+", " ") + sentLead.replaceAll("leadin=", "");
							String[] arry2 = {
									("sentLeadPlusClau=" + sentLead + " "
											+ tmpStr.replaceAll("((clause|leadin)=)+", "").replaceAll("[\r\n]+", " "))
													.replaceAll("[ ]+", " "),
									hdg, hdg2, "typ=40", "sentLead=" + sentLead };
							tmpDup2 = (sentLead + " "
									+ tmpStr.replaceAll("((clause|leadin)=)+", "").replaceAll("[\r\n]+", " "))
											.replaceAll("[ ]+", " ")
									+ sentLead;

//							System.out.println(".add40==" + Arrays.toString(arry2));

							listArray = new ArrayList<String[]>();
							listArray.add(arry);
							if (listClause.size() == 1) {
								addThisClause = c + 0;
							} else {
								addThisClause = clauCnt++;
								thereIsASubClauseOrClause = true;
							}

							if (!mapRemoveDupes.containsKey(tmpDup.hashCode() + sIdx)) {
								map.put(chunkCnt + "-" + (cnt + round) + "-" + (addThisSent + round) + "-"
										+ (1 + addThisClause + round) + "-" + round, listArray);
								mapRemoveDupes.put(tmpDup.hashCode() + sIdx, "");
							}

							if (sentLead.length() > 0 && c > 0 &&
							// 1st clau is the leadin so can't append to itself
							// remove cases where lead and sent txt are equal (don't join lead+txt)==>
									!sentLead.replaceAll("[^A-Za-z ]", "").replaceAll("[ ]+", " ").trim()
											.equals(tmpStr.replaceAll("((clause|leadin)=)+", "")
													.replaceAll("[^A-Za-z ]", "").replaceAll("[ ]+", " ").trim())) {
								if (!mapRemoveDupes.containsKey(tmpDup2.hashCode() + eIdx)) {
									listArray = new ArrayList<String[]>();// NOTE: typ=40 doesn't parse correctly
									listArray.add(arry2);
									map.put(chunkCnt + "-" + (cnt + round) + "-" + (addThisSent + round) + "-"
											+ (1 + addThisClause + round) + "-" + round, listArray);
									mapRemoveDupes.put(tmpDup2.hashCode() + eIdx, "");
								}
							}

							lastPutCl = 1 + addThisClause + round;

							if (listClause.size() > 0) {
//								System.out.println("XX str=" + str);
								strn = listClause.get(c).replaceAll("" + "^(leadin=|clause=sssS1s)[\\(\\da-zA-Z\\)]+",
										"");
//									System.out.println("listClause.get(b).repl==" + str);
								listSubClause = getSolrClause(strn);
								// to get subclause just iterate over the prior text. This can be run over and
								// over to get as many subclauses as wanted
								if (null == listSubClause || listSubClause.size() == 0)
									continue;
								for (int d = 0; d < listSubClause.size(); d++) {
//									System.out.println("subclause=" + listSubClause.get(d) + "||end\r\n");
									if (listSubClause.size() == 1) {
										addThisSubClause = 0;
									} else {
										addThisSubClause = d;
										thereIsASubClauseOrClause = true;
									}

									tmpStr = listSubClause.get(d).replaceAll("(sssS1s)", "")
//											.replaceAll("xxOP", "(")
//											.replaceAll("CPxx", ")").replaceAll("(xxPD)+", ".")
											.replaceAll("xxOB", "[").replaceAll("CBxx", "]").replaceAll("OBCB", "[]")
											.replaceAll("CBOB", "][");

									listArray = new ArrayList<String[]>();
									String[] arr = {
											"sub=" + tmpStr.replaceAll("((leadin|clause)=)+", "")
													.replaceAll("[\r\n]+", " ").replaceAll("[ ]+", " "),
											hdg, hdg2, "typ=5",
											"clauLead=" + clauLead.replaceAll("((leadin|clause)=)+", "") };
									tmpDup = tmpStr.replaceAll("((leadin|clause)=)+", "").replaceAll("[\r\n]+", " ")
											.replaceAll("[ ]+", " ") + clauLead.replaceAll("((leadin|clause)=)+", "");

									listArray.add(arr);

									String[] arr2 = {
											("clauLeadPlusSub=" + clauLead.replaceAll("((leadin|clause)=)+", "") + " "
													+ tmpStr.replaceAll("((leadin|clause)=)+", "").replaceAll("[\r\n]+",
															" ")).replaceAll("[ ]+", " "),
											hdg, hdg2, "typ=50",
											"clauLead=" + clauLead.replaceAll("((leadin|clause)=)+", "") };
									tmpDup2 = (clauLead.replaceAll("((leadin|clause)=)+", "") + " "
											+ tmpStr.replaceAll("((leadin|clause)=)+", "").replaceAll("[\r\n]+", " "))
													.replaceAll("[ ]+", " ")
											+ clauLead.replaceAll("((leadin|clause)=)+", "");
									// NOTE: typ=50 doesn't parse correctly.

									if (!mapRemoveDupes.containsKey(tmpDup.hashCode() + sIdx)) {
										map.put(chunkCnt + "-" + (cnt + round) + "-" + (addThisSent + round) + "-"
												+ (1 + addThisClause + round) + "-" + (1 + addThisSubClause + round),
												listArray);
										mapRemoveDupes.put(tmpDup.hashCode() + sIdx, "");
									}

									if (sentLead.length() > 0 && d > 0 &&
									// remove cases where lead and sent txt are equal (don't join lead+txt)==>
//											!sentLeadisClauLead &&
											!clauLead.replaceAll("((leadin|clause)=)+", "").replaceAll("[^A-Za-z ]", "")
													.replaceAll("[ ]+", " ")
													.equals(tmpStr.replaceAll("((clause|leadin)=)+", "")
															.replaceAll("[^A-Za-z ]", "").replaceAll("[ ]+", " ")
															.trim())) {
										// 1st clau is the leadin so can't append to itself, clauLead=tmpStr
										if (!mapRemoveDupes.containsKey(tmpDup2.hashCode() + eIdx)) {
											listArray = new ArrayList<String[]>();
											listArray.add(arr2);
											map.put(chunkCnt + "-" + (cnt + round) + "-" + (addThisSent + round) + "-"
													+ (1 + addThisClause + round) + "-"
													+ (1 + addThisSubClause + round), listArray);
											mapRemoveDupes.put(tmpDup2.hashCode() + eIdx, "");
//									 NOTE: typ=50doesn't parse correctly.
										}
									}

								}
							}
						}

					if (!thereIsASubClauseOrClause) {
//						System.out.println("there are no clauses.");
						listProvided = providedClause(listSent.get(n));
//						System.out.println("listProvided.size=" + listProvided.size());
						if (null == listProvided || listProvided.size() == 0)
							continue;
						cntProv = 0;
						for (int d = 0; d < listProvided.size(); d++) {
//							System.out.println("providedClause=" + listProvided.get(d) + "||end\r\n");
							tmpStr = listProvided.get(d).replaceAll("(sssS1s)", "")
//									.replaceAll("xxOP", "(")
//									.replaceAll("CPxx", ")").replaceAll("(xxPD)+", ".")
									.replaceAll("xxOB", "[").replaceAll("CBxx", "]").replaceAll("OBCB", "[]")
									.replaceAll("CBOB", "][");

							listArray = new ArrayList<String[]>();
							String[] arr = { "prov=" + tmpStr.replaceAll("[\r\n]+", " ").replaceAll("[ ]+", " "), hdg,
									hdg2, "typ=6", "clauLead=" + clauLead.replaceAll("((leadin|clause)=)+", "") };
							listArray.add(arr);
							tmpDup = tmpStr.replaceAll("[\r\n]+", " ").replaceAll("[ ]+", " ")
									+ clauLead.replaceAll("((leadin|clause)=)+", "");

							cntProv++;
//							System.out.println("prov  --- .put=" + (cnt + round) + "-" + (addThisSent + round) + "-"
//									+ (1 + addThisClause + round) + "-" + (1 + d + round) + " added ary="
//									+ Arrays.toString(arr));
							if (!mapRemoveDupes.containsKey(tmpDup.hashCode() + sIdx)) {
								map.put(chunkCnt + "-" + (cnt + round) + "-" + (addThisSent + round) + "-"
										+ (lastPutCl + round) + "-" + ((cntProv) + round), listArray);
								mapRemoveDupes.put(tmpDup.hashCode() + sIdx, "");
							}
						}
					}
				}
			}
		}

//		System.out.println(
//				"getting solr map second loop this many seconds==" + (startTime - System.currentTimeMillis()) / 1000);
//		startTime = System.currentTimeMillis();

		return map;// for MT we would remove the return and replace it with the next task which is
					// at createSolrFile (we would need to modify that method to exclude dealing
					// with metadata (doc0). Note we would retain portion of metadata that goes into
					// each doc such as yr
	}

	public String createSolrFile(TreeMap<String, List<String[]>> map, List<String> metadataList) throws IOException {

//		long startTime = System.currentTimeMillis();
//		GC_Tester gct = new GC_Tester();
		NLP nlp = new NLP();

		// TODO- Do this as a list -- add it when i retrieve.

		List<String[]> listSolr = new ArrayList<String[]>();

		// if it is a new para and there is a leadin, that leadin should be associated
		// with all sentences related to that para.

		/*
		 * id=kId+(i++) within each <doc> is the corresponding cnt of the typ. b/c sent
		 * and para represent the entire doc each get their own consecutively numbered
		 * counts (not treated as children). This way I can retrieve information in a
		 * simple manner that corresponds to each other - sent#1 is before sent#2 and so
		 * on. Same is true for para. Some sentences and para are the same - so they
		 * will be designated in typ as <typ>1,3</typ. However, clauses (and subclauses)
		 * are children of the sentence only. So their numbering will reset at new
		 * sentence. In addition, each sentence (and clau or subclause) will have lead
		 * in clause. Each leadin comes from the parent. So a para leadin is associated
		 * ONLY with sentence text (typ=3). I desigante leadin text in its own text
		 * field called <ld>. I then also pair ld+txt - that is designated by typ+0. If
		 * typ=3 and I join ld to 3 then typ becomes 30. Any two digit type reflects a
		 * ld plus txt with the first digit being the typ joined to ld.
		 */

		// id=acc_k#_chunk#_para#_sent#(+L)_clause#(+L)_subCl#(+L)

		StringBuilder sb = new StringBuilder();
		// TODO- populate metadata fields here- kId,contractType,legal entities, etc

		String txtTmp = "", hashHdg = "", hashHdg2 = "", hashHdgIdComb = "", hdgTypComb = "", pCnt = "", sCnt = "",
				cCnt = "", sbCnt = "", chunkCnt = "", fDate = ""
//				, paraCntPrior = ""
				, lead = "", tmp = "", yr = "", id = "", typ = "", txt = ""
//				, vTxt = ""
				, hTxt = "", hdg1 = "", hdg2 = "", hdgL1 = "", hdgL2 = "", key = "", contractType = "", score = "",
				hdgTyp = "", hdgTyp2, heading, heading2, contractNameAlgo = "", kId = "", parent_group, sector,
				industry, ticker;
		int hCnt,
//		vCnt,
				wCnt;
//		boolean itIsToc = false;

		sb.append("[");// json new doc

//		NLP.printListOfString("metadataList====", metadataList);

		if (metadataList.size() == 0) {
			// it is therefore client text

			yr = "1701";
			kId = "0123456789-99-123456_1";
			id = kId + "_0_0_0_0_0";
			sb.append("{\r\n\"id\":\"").append(id).append("\"\r\n,");
			sb.append("\"kId\":\"").append(kId.trim()).append("\"\r\n,");
			sb.append("\"yr\":").append(yr + "\r\n,");
			sb.append(metaHdgs.replaceAll(", ?$", "") + "}");

		}

		if (metadataList.size() > 0) {
//			NLP.printListOfString("1. metadataList====", metadataList);
			sb.append("\r\n{");
			kId = metadataList.get(0).trim();
//			parent_group =  metadataList.get(21).trim();
//			ticker =  metadataList.get(22).trim();
//			sector =  metadataList.get(23).trim();
//			industry =  metadataList.get(24).trim();
			// sb.append - metadata
//		System.out.println("listSolr.size=" + listSolr.size());

			/*
			 * id= is based on sec chunks (these are the pieces of the contract that are
			 * broken into pieces based on headings. Each chunk we multi thread b/c they can
			 * be run independently. When I parse en masse it isn't necessary to
			 * multi-thread b/c I just run multiple instances of the entire program. But for
			 * running on a client document it can take 30 to 60 seconds depending on the
			 * document size and multi-threading with chunks can reduce that time
			 * significantly.
			 */

			// schema names to adjust (add file size)-
//	 	   <field name="kTyp" type="text_general" indexed="true" stored="true" omitNorms="true"/>
//	 	   <!-- kTyp is contract type, only exists where pNo/sNo is zero -->   
//	 	   <field name="kName" type="text_general" stored="true" indexed="true"  multiValued="false" /> 
//	 	   <!-- kName is contract longname, only exists where pNo/sNo is zero -->   
//	 	   <field name="fDate" type="date" indexed="true" stored="true"/>   

			id = kId.trim().replaceAll("kId=", "") + "_0_0_0_0_0";
			yr = metadataList.get(1).replaceAll("[^\\d]", "").substring(0, 4);
			fDate = metadataList.get(1).replaceAll("[^\\d]", "");
			if (metadataList.size() > 15)
				score = metadataList.get(16);
			if (clientScore.length() > 0) {
				score = clientScore;
			}
//			System.out.println("metadata.get(1)==" + metadataList.get(1) + " yr === " + yr);

			sb.append("\r\n\"id\":\"").append(id).append("\"\r\n,");
			sb.append("\"kId\":\"").append(kId.trim()).append("\"\r\n,");
			sb.append("\"yr\":").append(yr + "\r\n,");
			sb.append(metaHdgs);
			String tmpStr = "";

			// this is doc0 --- metadata.
			// System.out.println("1 solr map.size=" + map.size());

			for (int z = 0; z < metadataList.size(); z++) {
				// System.out.println("A-AAtmp=" + tmp);
				tmp = metadataList.get(z).replaceAll("[\\|]+", "");
				if (nlp.getAllIndexEndLocations(tmp,
						Pattern.compile("contractingPartyRole|contractingParty|contractingRole")).size() > 0) {
					// System.out.println("tmp1234=" + tmp);
					tmpStr = tmpStr + tmp + "\r\n,";
				}
				if (tmp.split("=").length == 1) {
					continue;
				}
				// System.out.println("AAtmp=" + tmp);

				if (tmp.split("=")[1].replaceAll("[\\d]+", "").length() > 0) {
//				System.out.println("1 tmp==" + tmp);
					if (!tmp.contains("sIdx")) {
						tmpStr = tmpStr + ("\"" + tmp.split("=")[0].replaceAll("=", "") + "\":\""
								+ tmp.substring(tmp.indexOf("=")).replaceAll("=", "").replaceAll("sIdx", " sIdx")
										.replaceAll("[ ]+", " ").replaceAll("xxOP", "(").replaceAll("CPxx", ")")
										.replaceAll("(xxPD)+", ".").replaceAll("xxOB", "[").replaceAll("CBxx", "]")
										.replaceAll("OBCB", "[]").replaceAll("CBOB", "][").replaceAll("\"", "\\\\\"")
								+ "\"\r\n,");
					}

					if (tmp.contains("sIdx")) {
//					System.out.println("tmp===" + tmp);

						tmp = tmp.replaceAll("\"", "\\\\\"");
						tmp = tmp.replaceAll("[:]+", "").replaceAll("(legalEntities=)+", "").replaceAll("=", "");
						tmp = tmp.replaceAll("sIdx", "\", \"sIdx").replaceAll("=", "").replaceAll("\\[\",", "")
								+ "\"]\r\n,";

						tmp = tmp.replaceAll("=", "").replaceAll("[ ]+", " ").replaceAll("xxOP", "(")
								.replaceAll("CPxx", ")").replaceAll("(xxPD)+", ".").replaceAll("xxOB", "[")
								.replaceAll("CBxx", "]").replaceAll("OBCB", "[]").replaceAll("CBOB", "][");

						tmp = "\"legalEntities\":[" + tmp;
						tmpStr = tmpStr + tmp.replaceAll("\\[\", \"", "[\"");
					}
				} else {
					// System.out.println("1234tmp=" + tmp);
					tmpStr = tmpStr + ("\"" + tmp.split("=")[0] + "\":" + tmp.substring(tmp.indexOf("="))
							.replaceAll("=", "").replaceAll("\"", "\\\\\"").replaceAll("^[0]+", "") + "\r\n,");
				}
			}

			tmpStr = tmpStr.replaceAll("\"legalEntities\":\\[\r\n" + "legalEntitiesInOpeningParagraph\", ",
					"\"legalEntitiesInOpeningParagraph\":[");

			tmpStr = tmpStr + "xx},";

//			System.out.println("tmpStr=====" + tmpStr);
			if (nlp.getAllIndexEndLocations(tmpStr, Pattern.compile("contractNameAlgo\":\".*?\"\r\n")).size() > 0) {
				contractNameAlgo = nlp.getAllMatchedGroups(tmpStr, Pattern.compile("contractNameAlgo\":\".*?\"\r\n"))
						.get(0).replaceAll("\r\n\"", "\"") + "\r\n,";
			}
			if (contractNameAlgo.length() > 0 && tmpStr.contains("\"openParaKNameEqualsContractAlgo\":\"Y\"")) {
//				System.out.println("contractNameAlgo=" + contractNameAlgo);
				contractNameAlgo = (contractNameAlgo.replaceAll("\r\n", "") + "\"Y\"" + "]\r\n,").replaceAll(":", ":[")
						.replaceAll("\r\n\"", "\"");
//				System.out.println("contractNameAlgo=" + contractNameAlgo);
			}

			if (nlp.getAllIndexEndLocations(tmpStr, Pattern.compile("\"contractType2\":\".*?\"\r\n")).size() > 0) {
				contractType = nlp.getAllMatchedGroups(tmpStr, Pattern.compile("\"contractType\":\".*?\"\r\n")).get(0)
						+ "\r\n,";
			}

			tmpStr = tmpStr.replaceAll("[\r\n],xx}", "}").replaceAll("(?sm)[\r\n]+\"", "\"")
					.replaceAll("[\n]{2,}", "\n").replaceAll("[\r\n]+", "\r\n");

			tmpStr = tmpStr.replaceAll("(?sm),\"contractNameAlgo_Top\":\".*?\"[\r\n]+", "")
					.replaceAll("(?sm),\"contractNameAlg_Top\":\".*?\"[\r\n]+", "")
					.replaceAll("legalEntities\":\\[legalEntitiesInOpeningParagraph\",",
							"legalEntitiesInOpeningParagraph\":[");

			score = "\"scor\":" + score.replaceAll("score=", "").trim() + "\r\n,";
			sb.append(score);
			sb.append(tmpStr);

//		NLP.printListOfString("metadata==", metadata);
			kId = kId.trim().replaceAll("kId=", "");
		}

//This is the actual <doc> from solrMap M/T. Each map is a chunk.  
//This creates the json for ingestion into solr. TODO- Once solr json is created - post2solr 

//		System.out.println("2 solr map.size=" + map.size());

//		System.out.println("map.size=" + map.size());
//		System.out.println("create solr json file metadata done==" + (System.currentTimeMillis() - startTime) / 100);

		String tmpStringFindIt = "a reportable event as defined in Section 4043 of ERISA with";
		for (Map.Entry<String, List<String[]>> entry : map.entrySet()) {
//			itIsToc = false;
//			paraCntPrior = pCnt;
			key = entry.getKey();
//			System.out.println(
//					"chunkCnt=" + chunkCnt + " pCnt=" + pCnt + " sCnt=" + sCnt + " cCnt=" + cCnt + " sbCnt=" + sbCnt);

			if (nlp.getAllIndexEndLocations(entry.getValue().get(0)[0], Pattern.compile("\r\n")).size() > 40
					|| nlp.getAllIndexEndLocations(entry.getValue().get(0)[0], Pattern.compile("Qx")).size() > 12
					|| nlp.getAllIndexEndLocations(entry.getValue().get(0)[0],
							Pattern.compile("\r\n.{0,3}(SECTION|ARTICLE|Section|Article)")).size() > 12
					|| nlp.getAllIndexEndLocations(entry.getValue().get(0)[0], Pattern.compile("Qx")).size() > 12) {
//				System.out.println("continue1=="+entry.getValue().get(0)[0]+"|end");
				continue;
			}

			listSolr = entry.getValue();
			hashHdgIdComb = "";
			hdgTypComb = "";
			typ = "";
			hTxt = "";
			for (int i = 0; i < listSolr.size(); i++) {
//				System.out.println("i=" + i + " listSolr.get=" + Arrays.toString(listSolr.get(i)));
				// If there are two instances -- then I need to conjoin [4] and [0] in the
				// second. If there are 3 - I do the same and in that that order and then record
				// as separate <DOC>. Lead is the same in both cases [4]
//				if(nlp.getAllMatchedGroups(listSolr.get(i)[0], Pattern.compile(tmpStringFindIt)).size()>0)
//					System.out.println("i=" + i + " listSolr.get=" + Arrays.toString(listSolr.get(i)));
				lead = "";

				txt = listSolr.get(i)[0];
				txt = txt.replaceAll(
						"paraLeadPlusSent=|sentLeadPlusClau=|clauLeadPlusSub=|clauLeadPlusProv=|para=|sent=|clau="
								+ "|sub=|prov=|(Qx)+|(XQ)+",
						"").replaceAll("\"", "\\\\\"").replaceAll("[ ]+", " ").replaceAll("xxOP", "(")
						.replaceAll("CPxx", ")").replaceAll("(xxPD)+", ".").replaceAll("xxOB", "[")
						.replaceAll("CBxx", "]").replaceAll("OBCB", "[]").replaceAll("CBOB", "][").trim();

				txtTmp = txt;
				txt = txt.replaceAll("\r\n", "\\\\n");
				if (txt.replaceAll("[ \r\n\\d\\(\\)\\[\\];:,'\"\\.\\-_\\*\\$]+", "").length() < 3) {
//					System.out.println("continue2");
					continue;
				}

				String[] ary = listSolr.get(i);
				typ = listSolr.get(i)[3].replaceAll("typ=", "");
				chunkCnt = (Integer.parseInt(key.substring(0, 5)) - 10000) + "";
//				System.out.println("key=" + key);
				pCnt = (Integer.parseInt(key.substring(6, 11)) - 10000) + "";
				if (typ.contains("10")) {
					pCnt = "L" + pCnt;
					txt = lead + " " + txt;
					txtTmp = lead + " " + txtTmp;

				}

				sCnt = (Integer.parseInt(key.substring(12, 17)) - 10000) + "";
				if (typ.contains("30")) {
					sCnt = "L" + sCnt;
					txt = lead + " " + txt;
					txtTmp = lead + " " + txtTmp;

				}

				cCnt = (Integer.parseInt(key.substring(18, 23)) - 10000) + "";
				if (typ.contains("40")) {
					sCnt = "L" + sCnt;
					txt = lead + " " + txt;
					txtTmp = lead + " " + txtTmp;
				}

				sbCnt = (Integer.parseInt(key.substring(24, key.length())) - 10000) + "";
				if (typ.contains("50")) {
					sbCnt = "L" + sbCnt;
					txt = lead + " " + txt;
					txtTmp = lead + " " + txtTmp;
				}

				hdg1 = "";
				hdg2 = "";
				hdg1 = ary[1].replaceAll("xxPD", ".");
				hdg2 = ary[2].replaceAll("xxPD", ".");
//				System.out.println("hdg1=" + hdg1 + " hdg2=" + hdg2);
				typ = ary[3].replaceAll("typ=", "");
				wCnt = 0;
				hCnt = 0;

				sb.append("\r\n{");
				id = kId.trim() + "_" + chunkCnt + "_" + pCnt + "_" + sCnt + "_" + cCnt + "_" + sbCnt;
				sb.append("\r\n\"id\":\"").append(id).append("\"\r\n,");
				sb.append("\"kId\":\"").append(kId.trim()).append("\"\r\n,");
				sb.append(contractNameAlgo);

//				if (StringUtils.isNotBlank(score))
				sb.append(score);
				if (typ.contains(",")) {
					sb.append("\"typ\":[" + typ + "]\r\n,");
				} else {
					sb.append("\"typ\":" + typ + "\r\n,");
				}

				lead = listSolr.get(i)[4].replaceAll("(Qx|XQ|paraLead=|sentLead=|clauLead=|provLead=)+", "")
						.replaceAll("\"", "\\\\\"").replaceAll("[ ]+", " ").replaceAll("xxOP", "(")
						.replaceAll("CPxx", ")").replaceAll("(xxPD)+", ".").replaceAll("xxOB", "[")
						.replaceAll("CBxx", "]").replaceAll("OBCB", "[]").replaceAll("CBOB", "][");
				if (lead.replaceAll("[ \r\n\\d\\(\\)\\[\\];:,'\"\\.-_\\*\\$]+", "").length() > 3) {
					sb.append("\"lead\":\"").append(lead.replaceAll("[\r\n]+", "\\\\n")).append("\"\r\n,");
				}

				if (hdg1.length() > 4) {

					if (nlp.getAllMatchedGroups(hdg1, Pattern.compile("(^[a-zH]{3}|exhToc)(?==)")).size() == 0) {
//						System.out.println("oh fuck -- not continue past....hdg1=" + hdg1);
						continue;
					}

					hdgTyp = nlp.getAllMatchedGroups(hdg1, Pattern.compile("(^[a-zH]{3}|exhToc)(?==)")).get(0);
					heading = hdg1.replaceAll("([a-zH]{3}|exhToc)=|(Qx)+|(XQ)+", "").replaceAll("xxOP", "(")
							.replaceAll("CPxx", ")").replaceAll("(xxPD)+", ".").replaceAll("xxOB", "[")
							.replaceAll("CBxx", "]").replaceAll("OBCB", "[]").replaceAll("CBOB", "][")
							.replaceAll("\"", "\\\\\"").replaceAll("\r\n", " ").replaceAll("[ ]+", " ").trim();
					hdgL1 = heading.substring(heading.indexOf("||"), heading.length()).replaceAll("\\|\\|hdgLen=", "")
							.trim();
					hdgL1 = "\"" + hdgTyp.replaceAll("\"", "\\\\\"") + "L" + "\":" + hdgL1 + "\r\n,";
					heading = heading.substring(0, heading.indexOf("||"));
					sb.append("\"" + hdgTyp.replaceAll("\"", "\\\\\"") + "\":\"" + heading + "\"\r\n,");
					sb.append(hdgL1);
//					System.out.println("heading=" + heading);

//				[0]para=,sent=,clau=,sub=,prov=,
//				[3]paraLeadPlusSent=,sentLeadPlusClau=,clauLeadPlusSub=,provPlusClau=
					if (heading.length() > 4) {
						hashHdg = goLawHeadingHtxt(heading).hashCode() + "";
//						System.out.println(
//								"1 hashHdg=" + goLawHeadingHtxt(heading.replaceAll("[\r\n]+", " ")).hashCode() + "");
						if (hashHdg.length() > 4) {
							hashHdgIdComb = hashHdg + "";
							hdgTypComb = hdgTyp;
						}
					}
				}

				if (hdg2.length() > 4
						&& nlp.getAllMatchedGroups(hdg2, Pattern.compile("(^[a-zH]{3}|exhToc)(?==)")).size() > 0) {
					hdgTyp2 = nlp.getAllMatchedGroups(hdg2, Pattern.compile("(^[a-zH]{3}|exhToc)(?==)")).get(0);
					heading2 = hdg2.replaceAll("([a-zH]{3}|exhToc)=|(Qx)+|(XQ)+", "").replaceAll("xxOP", "(")
							.replaceAll("CPxx", ")").replaceAll("(xxPD)+", ".").replaceAll("xxOB", "[")
							.replaceAll("CBxx", "]").replaceAll("OBCB", "[]").replaceAll("CBOB", "][")
							.replaceAll("\"", "\\\\\"").replaceAll("\r\n", " ").replaceAll("[ ]+", " ").trim();

					hdgL2 = heading2.substring(heading2.indexOf("||"), heading2.length())
							.replaceAll("\\|\\|hdgLen=", "").trim();
					hdgL2 = "\"" + hdgTyp2.replaceAll("\"", "\\\\\"") + "L" + "\":" + hdgL2 + "\r\n,";
					heading2 = heading2.substring(0, heading2.indexOf("||"));
					sb.append("\"" + hdgTyp2.replaceAll("\"", "\\\\\"") + "\":\"" + heading2 + "\"\r\n,");
					sb.append(hdgL2);

//					hdgTyp2 = "\"" + hdgTyp2.replaceAll("\"", "\\\\\"") + "\":\"";
//					sb.append(hdgTyp2 + heading22 + "\"\r\n,");

//					System.out.println("heading2=" + heading2);
					if (heading2.length() > 4) {
						hashHdg2 = goLawHeadingHtxt(heading2).hashCode() + "";
						if (hashHdg2.length() > 4 && hashHdgIdComb.length() > 4) {
							hashHdgIdComb = hashHdgIdComb + "," + hashHdg2;
							hdgTypComb = hdgTypComb + "," + hdgTyp2;
						} else if (hashHdgIdComb.length() <= 4 && hashHdg2.length() > 4) {
							hashHdgIdComb = hashHdg2 + "";
							hdgTypComb = hdgTyp2;
						}
					}
				}

//				System.out.println("hashHdgIdComb=" + hashHdgIdComb);
				if (hashHdgIdComb.length() > 0) {
					sb.append("\"hashHdgId\":[" + hashHdgIdComb + "]\r\n,");
					sb.append("\"hdgOrd\":[\"" + hdgTypComb.replaceAll(",", "\",\"") + "\"]\r\n,");
				}

				// don't use revised txt as it has inserted \\n.
				hTxt = goLawGetHtxt(txtTmp.replaceAll("[\r\n]", " "));
				hCnt = hTxt.split(" ").length;
//
//				vTxt = (goLawGetHtxt(goLaw_PrepTextforStemming(
//						goLawGetVerbs(txtTmp.replaceAll("[\r\n]", " ").replaceAll("[^A-Za-z ]+", ""))))).trim();
//				vCnt = vTxt.split(" ").length;
				wCnt = txtTmp.split(" ").length;

				String hdgTmp = hdg1;
				if (hdgTmp.length() > 4) {
					hdgTmp = hdgTmp.substring(0, hdgTmp.indexOf("||"));
					hdgTmp = hdgTmp.replaceAll("(hdg|mHd|sub|sec|exh)=", "");
					if (hdgTmp.length() <= txt.length() && hdgTmp.contains(txt.substring(0, hdgTmp.length()))) {
						txt = txt.substring(hdgTmp.length(), txt.length()).trim();
					}
					wCnt = txt.split(" ").length + 1;// also changes hashHdgId
				}

				hdgTmp = hdg2;
				if (hdgTmp.length() > 4) {
					hdgTmp = hdgTmp.substring(0, hdgTmp.indexOf("||"));
					hdgTmp = hdgTmp.replaceAll("(hdg|mHd|sub|sec|exh)=", "");
					if (hdgTmp.length() <= txt.length() && hdgTmp.contains(txt.substring(0, hdgTmp.length()))) {
						txt = txt.substring(hdgTmp.length(), txt.length()).trim();
					}
					wCnt = txt.split(" ").length + 1;// also changes hashHdgId
				}
				txt = txt.replaceAll("^\\. ?", "").trim();
				if (txt.contains("Company determines that you have violated")) {
					System.out.println("txt====" + txt);
				}
				sb.append("\"txt\":\"").append(txt).append("\"\r\n,");

				sb.append("\"wCnt\":").append(wCnt).append("\r\n,");
				sb.append("\"hashTxtId\":").append(txt.hashCode() + "" + wCnt + "\r\n,");

				if (hTxt.length() > 4) {
					sb.append("\"hCnt\":").append(hCnt).append("\r\n,");
					sb.append("\"hTxt\":\"").append(hTxt.replaceAll("\r\n", "\\\\n")).append("\"\r\n,");
					sb.append("\"hashHtxtId\":").append(hTxt.hashCode() + "" + hCnt + "\r\n,");
				}

//				if (vTxt.length() > 4) {
//					sb.append("\"vCnt\":").append(vCnt).append("\r\n,");
//					sb.append("\"vTxt\":\"").append(vTxt.replaceAll("\r\n", "\\\\n")).append("\"\r\n,");
//					sb.append("\"hashVtxtId\":").append(vTxt.hashCode() + "" + vCnt + "\r\n,");
//				}

				sb.append("\"yr\":").append(yr + "\r\n");
				if (StringUtils.isNotBlank(fDate))
					sb.append(",\"fDate\":").append(fDate + "\r\n");

				sb.append("},");
				// System.out.println("sb.len=" + sb.toString().length());
			}

		}

		String str = sb.toString().substring(0, sb.toString().length() - 1) + "\r\n]";
		str = str.replaceAll("(?sm)openingParagraph\":\"", "openingParagraph\":\"^")
				.replaceAll(",contractNameAlgo", ",\"contractNameAlgo")
				.replaceAll(" cnt[\\d]{1,3} idx[\\d]+ \\%locInDoc[\\d]+ score[\\d]+", "")
				.replaceAll(",\"score\":", ",\"scor\":");
		// System.out.println("solr json.len=" + str.length());
		sb.delete(0, sb.toString().length());

//		System.out.println(
//				"total time just to create solr json file seconds==" + (System.currentTimeMillis() - startTime) / 1000);

		return str;

	}

	public String goLawHeadingHtxt(String text) {

		// *********** THIS MUST NOT BE CHANGED. HTXT IS IMMUTABLE AFTER IT IS IN SOLR.
		// This method is run on client text queries and must use same
		// algorithms/methods as was used to parse into solr.

		text = text.replaceAll("(SECTION|Section|ARTICLE|Article) ", "").replaceAll("\\([A-Za-z\\d]{1,5}\\)", "")
				.replaceAll("[^a-zA-Z ]", "")//
				.toLowerCase().replaceAll("[\r\n]", " ").replaceAll("[ ]+", " ").trim();

		text = patternGoLawHtxtStopWordsLimited.matcher(text).replaceAll("");
		text = text.replaceAll("(?ism)(^| )(no)( |$)", "xx$2xx").replaceAll("(^| )[a-z]{2}( |$)", " ")
				.replaceAll("(?ism)(xx)(no)(xx)", " $2 ").replaceAll("[ ]+", " ");

		if (text.length() < 3)
			return "";

		text = goLawGetHtxt(text); // this has to be reduced to lower case else no value is returned.
		return text;
	}

	public String goLawStemmer(String text) {

		/*
		 * *********** MUST NOT BE CHANGED. HTXT IS IMMUTABLE AFTER IT IS IN SOLR. This
		 * method is run on client text queries and must use same algorithms/methods as
		 * was used to parse into solr.
		 */

		GoLawLancasterStemmer lc = new GoLawLancasterStemmer();

		String[] wds = text.replaceAll("[ ]+", " ").split(" ");
		String hTxt = "", word = "", stem = "";
		/*
		 * go with straight lancaster - there are rare cases where it stems supervision
		 * and supervis different, but this is extremely rare. In addition, it is
		 * aggressive but if you try to correct it you end up with other issues. Its
		 * aggressive stemming is good for search cdn. for lib builder it creates some
		 * issues, but that is secondary.
		 */

		for (int n = 0; n < wds.length; n++) {
			word = wds[n];

			int last = word.length();
			String temp = "";
			for (int i = 0; i < last; i++) {
				if ((word.charAt(i) >= 'a') & (word.charAt(i) <= 'z')) {
					temp += word.charAt(i);
				}
			}

			word = temp;

			stem = lc.stemWord(word);
			hTxt = hTxt + " " + stem;
			hTxt = hTxt.trim();
		}

		return hTxt;

	}

	public static String goLawRemoveDefinedTerms(String text) {
		// *********** THIS MUST NOT BE CHANGED. HTXT IS IMMUTABLE AFTER IT IS IN SOLR.
		// This method is run on client text queries and must use same
		// algorithms/methods as was used to parse into solr.

//		System.out.println("text=="+text);
		text = " " + text.replaceAll("[^A-Za-z ]", "") + " ";
//		System.out.println("text==="+text);
		text = text.replaceAll("(?sm)[A-Z]{1}[A-Za-z]+( |$)", " ").replaceAll("[ ]+", " ").trim();

		return text;
	}

	public String goLawGetHtxt(String text) {
		/*
		 * *********** MUST NOT BE CHANGED. HTXT IS IMMUTABLE AFTER IT IS IN SOLR. This
		 * method is run on client text queries and must use same algorithms/methods as
		 * was used to parse into solr. All code is intentionally embedded in this one
		 * method - that way there's no chance code gets changed inadvertantly elsewhere
		 * affecting this.
		 * 
		 */

		Pattern patternGoLawStopWordsOf2 = Pattern.compile("(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" + ""
				+ "(in case)|(with respect)|(in respect)|(respective)|(set forth)" + ""
				+ ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

		Pattern patternGoLawStopWordsContract = Pattern.compile("(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))"
				+ "(bonds?|notes?|debenture?s?|deed" + "|(certificate|note)holders?|certificates?|holders?"
				+ "|senior|junior|convertible|preferred|(this|the)[\r\n ]{1,3}(debt)"
				+ "|class|series|[A-Z]-\\d|securit[iesy]{1,3}"
				+ "|(this|the)[\r\n ]{1,3}(first|second|third|fourth|fifth|sixth)"
				+ "|supplementa?l?|indenture?|pooling and servicing|agreement" + "|(this|the)[\r\n ]{1,3}"
				+ "(trust)|guarante[yees]{1,3}|collateral documents?|documents?"
				+ "|[12]{1}[09]{1}[\\d]{1}-?\\d?)([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

		Pattern patternGoLawStopWordsLegal = Pattern.compile(
				"(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" + "(aforementioned)|(aforesaid)|(applicable)|(foregoing)"
						+ "|(hereafter)|(hereby)|(herein)|(hereof)|(hereunder)|(herewith)|(hereto)|(means)"
						+ "|(pursuant)|(relation)|(relating)|(related)"
						+ "|(thereafter)|(thereby)|(therefor)|(therefore)|(therein)|(thereof)|(thereto)|(thereunder)"
						+ "|(regarding)|(respect)" + ")([\r\n\t \\)\\]\\}\":;\\.,]{1}|$)");

		String[] goLawStateAndCountryLongNames = { "ALASKA", "ALABAMA", "ARKANSAS", "AMERICAN[\r\n ]{0,3}SAMOA",
				"ARIZONA", "CALIFORNIA", "COLORADO", "CONNECTICUT", "DISTRICT[\r\n ]{0,3}OF[\r\n ]{0,3}COLUMBIA",
				"DELAWARE", "FLORIDA", "FEDERATED[\r\n ]{0,3}STATES[\r\n ]{0,3}OF[\r\n ]{0,3}MICRONESIA", "GEORGIA",
				"GUAM", "HAWAII", "IOWA", "IDAHO", "ILLINOIS", "INDIANA", "KANSAS", "KENTUCKY", "LOUISIANA",
				"MASSACHUSETTS", "MARYLAND", "MAINE", "MARSHALL[\r\n ]{0,3}ISLANDS", "MICHIGAN", "MINNESOTA",
				"MISSOURI", "NORTHERN[\r\n ]{0,3}MARIANA[\r\n ]{0,3}ISLANDS", "MISSISSIPPI", "MONTANA",
				"NORTH[\r\n ]{0,3}CAROLINA", "NORTH[\r\n ]{0,3}DAKOTA", "NEBRASKA", "NEW[\r\n ]{0,3}HAMPSHIRE",
				"NEW[\r\n ]{0,3}JERSEY", "NEW[\r\n ]{0,3}MEXICO", "NEVADA", "NEW[\r\n ]{0,3}YORK", "OHIO", "OKLAHOMA",
				"OREGON", "PENNSYLVANIA", "PUERTO[\r\n ]{0,3}RICO", "PALAU", "RHODE[\r\n ]{0,3}ISLAND",
				"SOUTH[\r\n ]{0,3}CAROLINA", "SOUTH[\r\n ]{0,3}DAKOTA", "TENNESSEE", "TRUST[\r\n ]{0,3}TERRITORIES",
				"TEXAS", "UTAH", "WEST[\r\n ]{0,3}VIRGINIA", "VIRGIN[\r\n ]{0,3}ISLANDS", "VERMONT", "WASHINGTON",
				"WISCONSIN", "VIRGINIA", "WYOMING" };

		String[] goLawStateAndCountryAbbrevs = { "AK", "AL", "AR", "AS", "AZ", "CA", "CO", "CT", "DC", "DE", "FL", "FM",
				"GA", "GU", "HI", "IA", "ID", "IL", "IN", "KS", "KY", "LA", "MA", "MD", "ME", "MH", "MI", "MN", "MO",
				"MP", "MS", "MT", "NC", "ND", "NE", "NH", "NJ", "NM", "NV", "NY", "OH", "OK", "OR", "PA", "PR", "PW",
				"RI", "SC", "SD", "TN", "TT", "TX", "UT", "WV", "VI", "VT", "WA", "WI", "VA", "WY" };

		text = text.replaceAll("(\\([A-Za-z\\d]{1,3}\\)|[A-Za-z\\d]\\.\\))+", "").replaceAll("[-\\/]", " ")
				.replaceAll("[^a-zA-Z ]", "").replaceAll("[ ]+", " ").trim();
		// remove para #s
		if (text.length() < 3)
			return "";

		text = " " + text.replaceAll("[^A-Za-z ]", "") + " ";
		text = text.replaceAll("(?sm)[A-Z]{1}[A-Za-z]+( |$)", " ").replaceAll("[ ]+", " ").trim();
		// removes defined terms (capitalized words). Auto removes first word.

		// System.out.println("removed defined terms=" + text);
		if (text.length() < 3)
			return "";

		text = patternGoLawStopWordsOf2.matcher(text).replaceAll("");

		if (text.length() < 3)
			return "";

		text = text.replaceAll("'s|,|'", " ");
		text = patternGoLawStopWordsContract.matcher(text).replaceAll("");
		text = patternGoLawStopWordsLegal.matcher(text).replaceAll("");

		for (int i = 0; i < goLawStateAndCountryLongNames.length; i++) {
			text = text.replaceAll("(?i)" + goLawStateAndCountryLongNames[i], goLawStateAndCountryAbbrevs[i]);
		}

		Pattern patternGoLawHtxtStopWordsLimited = Pattern.compile("(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" + ""
				+ "(a)|(about)|(above)|(after)|(again)|(all)|(am)|(an)|(and)|(are)|(as)|(at)|(be)"
				+ "|(because)|(been)|(before)|(being)|(below)|(between)|(both)|(by)|(can)|(did)|(do)|(does)|(doing)|(down)"
				+ "|(during)|(each)|(either)|(few)|(for)|(from)|(further)|(had)|(has)|(have)|(having)|(he)|(her)|(here)|(hers)"
				+ "|(herself)|(him)|(himself)|(his)|(how)|(I)|(in)|(into)|(is)|(it)|(its)|(it\'s)|(itself)|(just)|(me)"
				+ "|(might)|(most)|(must)|(my)|(myself)|(need)|(now)|(of)|(off)|(on)|(only)|(or)|(other)|(otherwise)"
				+ "|(our)|(ours)|(ourselves)|(out)|(over)|(own)|(same)|(shall)|(she)|(she\'s)|(should)|(so)|(some)|(such)|(than)"
				+ "|(that)|(the)|(their)|(theirs)|(them)|(themselves)|(then)|(there)|(these)|(they)|(this)|(those)|(through)"
				+ "|(to)|(too)|(under)|(until)|(up)|(very)|(very)|(was)|(we)|(were)|(what)|(when)|(where)|(whether)|(which)"
				+ "|(while)|(who)"
				+ "|(whom)|(why)|(will)|(with)|(won)|(would)|(you)|(your)|(yours)|(yourself)|(yourselves)" + ""

				+ ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");
		text = patternGoLawHtxtStopWordsLimited.matcher(text).replaceAll("");

//		System.out.println("removed stopwrds1=" + text);
		if (text.length() < 3)
			return "";

		// System.out.println("removed stranded letters=" + text);
		text = text.replaceAll("(?ism)(^| )(no)( |$)", "xx$2xx").replaceAll("(^| )[a-z]{2}( |$)", " ")
				.replaceAll("(?ism)(xx)(no)(xx)", " $2 ").replaceAll("[ ]+", " ");
		if (text.length() < 3)
			return "";

		text = text.replaceAll("[^a-z ]+", "").replaceAll("[ ]+", " ").trim();
		if (text.length() < 3)
			return "";
		text = goLawStemmer(text);
//		System.out.println("stemmed=" + text);

		return text.trim();

	}

	public static String getParagraphAfterHeading(String text, int sIdx, int sIdxNext) throws IOException {
		// maxEidx is the sIdx of the next hdg
		// eIdx is the end of the heading.
		// text is the entire text of the contact
		// sIdx of problematic hdg and maxEidx=sIdxNext
		NLP nlp = new NLP();
		if (sIdxNext <= sIdx || text.length() < sIdxNext)
			return "";
		String para = text.substring(sIdx, Math.max(sIdx, sIdxNext));
//		System.out.println("sIdx=" + sIdx + " sIdxNext=" + sIdxNext);
//		System.out.println("initial cut of para.snip=" + para.substring(0, Math.min(75, para.length())) + "||");
		if (nlp.getAllMatchedGroups(para, Pattern.compile(".*?\r\n")).size() > 0) {
			para = nlp.getAllMatchedGroups(para, Pattern.compile(".*?\r\n")).get(0);
			// gets the full para if there's a hard return (might not be b/c only have 300
			// chars). Or could be blank because there's hdg is on its own line.
//			if (text.substring(sIdx, sIdxNext).contains("eatment of Company Equity Awa")) {
//				System.out.println("1.para===" + para + " txt.snip bef/aft=" + text.substring(sIdx, sIdx + 75));
//			}
//			System.out.println("this is the cut para that follows1=" + para + "|END");
			if (para.replaceAll("[\r\n ]", "").length() > 10) {
				return para;
			}
		}

		if (para.replaceAll("[\r\n \\.]", "").trim().length() < 2) {
			para = text.substring(sIdx);
			if (nlp.getAllMatchedGroups(para, Pattern.compile("\r\n.*?[^ ]\r\n")).size() > 0) {
				// if hdg on its own line, after hard returns take all text that follows and
				// stop at hard return
				para = nlp.getAllMatchedGroups(para, Pattern.compile("\r\n.*?[^ ]\r\n")).get(0);
//				if (text.substring(sIdx, sIdxNext).contains("eatment of Company Equity Awa")) {
//					System.out.println("2.para===" + para + " txt.snip bef/aft=" + text.substring(sIdx, sIdx + 75));
//				}
//				System.out.println("par01=" + para);
				if (para.indexOf("\r\n") > 0) {// this cuts it to the first line with text
					return para.substring(0, para.indexOf("\r\n"));
				} else {
					return para;
				}
			}
		}

//		System.out.println("para=" + para);
		return "";
	}

	public static List<String[]> fixEndIndexesOfEndingHeadings(List<String[]> listOfScrubIdxHeading, String text)
			throws IOException {

		// if cur hdg = typ 'hdg' or 'mHd' or 'sub' and next is 'exh' or 'def' or 'exh'
		// then only go 1 para for that hdg. eIdx can't go further than 1 para after
		// that hdg. If next hdg is not consecutive then the same rule

		NLP nlp = new NLP();

//		NLP.printListOfStringArray("listOfScrubIdxHeading===", listOfScrubIdxHeading);
		//
		int sIdx = 0, sIdxN = 0, eIdx = 0, eIdxN = 0, sIdxNLoop = 0, eIdxNLoop = 0, cnt = 0;
		String hdgTyp = "", hdg = "", hdgTypNext = "", hdgNext = "", hdgNextLoop = "", hdgTypNextLoop = "", para = "";
		List<String[]> listEidxScrub = new ArrayList<String[]>();
		boolean remove = false;
		for (int i = 0; i < listOfScrubIdxHeading.size(); i++) {
//			System.out.println(i);
			remove = false;
			hdg = listOfScrubIdxHeading.get(i)[2];
			hdgTyp = hdg.split("=")[0];
			if (listOfScrubIdxHeading.get(i)[0].replaceAll("[^\\d]+", "").length() < 1)
				continue;
			if (listOfScrubIdxHeading.get(i)[1].replaceAll("[^\\d]+", "").length() < 1)
				continue;

			sIdx = Integer.parseInt(listOfScrubIdxHeading.get(i)[0].replaceAll("[^\\d]+", ""));
			eIdx = Integer.parseInt(listOfScrubIdxHeading.get(i)[1].replaceAll("[^\\d]+", ""));
			cnt = 0;

			// cks 7 ahead
			cnt = Math.min(7, listOfScrubIdxHeading.size() - i);
			if (eIdx > eIdxN) {

//				System.out.println(
//						"1 eIdx=" + eIdx + " eIdxN=" + eIdxN + " hdgTyp=" + hdgTyp + " hdgTypNext=" + hdgTypNext);

				for (int c = i; c < Math.min((i + cnt), listOfScrubIdxHeading.size()); c++) {
					if (listOfScrubIdxHeading.get(c)[0].replaceAll("[^\\d]+", "").length() < 1)
						continue;
					if (listOfScrubIdxHeading.get(c)[1].replaceAll("[^\\d]+", "").length() < 1)
						continue;
					sIdxNLoop = Integer.parseInt(listOfScrubIdxHeading.get(c)[0].replaceAll("[^\\d]+", ""));
					eIdxNLoop = Integer.parseInt(listOfScrubIdxHeading.get(c)[1].replaceAll("[^\\d]+", ""));
					hdgNextLoop = listOfScrubIdxHeading.get(c)[2];
					hdgTypNextLoop = hdgNextLoop.split("=")[0];

					if (eIdx > eIdxNLoop && (hdgTyp.equals(hdgTypNextLoop) || (!hdgTyp.equals(hdgTypNextLoop)

							&& sIdx + listOfScrubIdxHeading.get(i)[2].length() + 30 < sIdxNLoop
							&& (nlp.getAllIndexEndLocations(hdgTypNextLoop, Pattern.compile("sec|exh")).size() > 0
									&& nlp.getAllIndexEndLocations(hdgTyp, Pattern.compile("mHd|hdg|sub")).size() > 0)
							|| (nlp.getAllIndexEndLocations(hdgTypNextLoop, Pattern.compile("def")).size() > 0
									&& nlp.getAllIndexEndLocations(hdgTyp, Pattern.compile("mHd|hdg|sub")).size() > 0)
							|| (nlp.getAllIndexEndLocations(hdgTypNextLoop, Pattern.compile("mHd")).size() > 0
									&& nlp.getAllIndexEndLocations(hdgTyp, Pattern.compile("hdg|sub")).size() > 0)

					)

					)) {
						// to be thorough - need to check a few ahead. cur sec eIdx cannot be > next sec
						// (nor hdg or any other hdg). In that case remove current sec/hdg etc.
//						System.out.println("1 remove --- hdg=" + hdg + "typ=" + hdgTyp + "\nhdgNextLoop=" + hdgNextLoop
//								+ " hdgTypNextLoop=" + hdgTypNextLoop);

						// sIdx of problematic hdg and maxEidx=sIdxNext

						if (listOfScrubIdxHeading.size() > cnt + 1
								&& sIdx + listOfScrubIdxHeading.get(i)[2].length() + 30 < sIdxNLoop) {
							para = getParagraphAfterHeading(text, sIdx + hdg.length() - 4, sIdxNLoop);
							String[] ary = { sIdx + "", (sIdx + para.length()) + "", hdg };
//						System.out.println("hdg=" + hdg + " hdgcut=" + text.substring(sIdx, sIdx + para.length()));
							listEidxScrub.add(ary);
							remove = true;
							break;
						} else {
							String[] ay = { sIdx + "", eIdx + "" };
							String[] ary = { sIdx + "", GetContracts.getLastDefinedTermSimple(ay, text) + "", hdg };
							listEidxScrub.add(ary);
							remove = true;
							break;
						}
					}
				}
			}

			if (remove)
				continue;

			listEidxScrub.add(listOfScrubIdxHeading.get(i));
		}

//		NLP.printListOfStringArray("listEidxScrub===", listEidxScrub);

		return listEidxScrub;

	}

	public String joinTextOverPages(String text) throws IOException {
//		System.out.println("joinTextOverPages text=" + text);
//		NLP nlp = new NLP();

		Pattern patternPgNo = Pattern.compile("([a-z]{1})"// 1
				+ "([\r\n]{3,})"// 2
				+ "(-? ?[\\d]{1,4} ?-?)"// 3
				+ "([\r\n]{6,20})"// 4
				+ "(\\(?[\"\\da-z])");// 5

//		Pattern patternPgNoLong = Pattern.compile("(\\.\r\n)"// 1
//				+ "([\r\n]{2,})"// 2
//				+ "(-? ?\\(?[\\d]{1,4} ?-?)"// 3
//				+ "([\r\n]{6,20})"// 4
//				+ "(\\(?[\\dA-Za-z])");// 5 //don't \" b/c it corrupts defs

		Pattern patternPgNoExhibit = Pattern.compile("(\r\n)"// 1
				+ "([\r\n]{4,})"// 2
				+ "(-? ?[A-Z]{1,4} ?-?[\\d]{1,3})"// 3
				+ "([\r\n]{6,20})"// 4
				+ "(\\(?[\"A-Za-z])");// 5

		Pattern patternPgNoEndOfSentence = Pattern.compile("([a-z]{1})"// 1
				+ "(\\. ?|; ?|: ?|; and ?|; or ?)"// 2,
				+ "([\r\n]{4,})"// 3
				+ "(-? ?[\\d]{1,4} ?-?)"// 4
				+ "([\r\n]{6,20})"// 5
				+ "(\\([a-z\\dA-Z]{1,2}\\)|\\d\\.|\"?[A-Z])");// 6
//		+ "(-? ?([\\diI]{1,4}|[A-B]{1}-?\\d) ?-?)"// $3
//		+ "([page]{0,4}[\\dixv\\-]{1,6})"// $3

		Pattern patternPgNoEndOfSentence2 = Pattern.compile("([A-Z]{1})"// 1
				+ "([\\.]{1}|; or|; and)"// 2
				+ "([\r\n]{4,})"// 3
				+ "(-? ?[\\d]{1,4} ?-?)"// 4
				+ "([\r\n]{6,20})"// 5
				+ "([A-Z]{1,2}|\"?[A-Z])");// 6

		// this will join text that was separated by page numbers and for purposes of
		// solr ingestion - mark location the page number was removed together with what
		// that pg no was.

		// NOTE: THIS REMOVES PAGE NUMBERS AND AS A RESULT THE SOLR DOC IS NOT EXACTLY
		// THE SAME AS THE INGESTED.
//		text = text.replaceAll(patternPgNoEndOfSentence.toString(), "$1$2EoS$4\r\n\r\n\r\n $6");
		text = text.replaceAll(patternPgNoEndOfSentence.toString(), "$1$2\r\n\r\n\r\n$6");
		text = text.replaceAll(patternPgNoEndOfSentence2.toString(), "$1$2\r\n\r\n\r\n$6");
		// 1,2,3,7

//		text = text.replaceAll(patternPgNo.toString(), "$1 PgJoin$3\r\n\r\n\r\n$5");
		// marks the location the pg no was replaced.
		text = text.replaceAll(patternPgNo.toString(), "$1 $5");
//		text = text.replaceAll(patternPgNoLong.toString(), "$1 PgJoin$3\r\n\r\n\r\n$5");
//		text = text.replaceAll(patternPgNoLong.toString(), " $5");

//		text = text.replaceAll(patternPgNoExhibit.toString(), "$1 PgJoin$3\r\n\r\n\r\n$5");
		text = text.replaceAll(patternPgNoExhibit.toString(), " $5");

		text = text.replaceAll("PgJoin(?=[12]{1}[09]{1}[\\d]{2}\r\n)", "");
		text = text
				.replaceAll("(?sm)(?<=(; (and |less |plus )?)|[,;]{1}()|(; or ?)|(\\.[\r\n ]{0,5})|([;:]{1}[\r\n ]{0,5}"
						+ "|,[\r\n ]{0,5}))PgJoin(?=\\([a-z\\d]{1,3}\\)|[-\\d]{1,6}|[A-Z]{1,2}-)", "");

		return text;

	}

	public boolean isItTableOfContents(String text) {

		NLP nlp = new NLP();

		double cntSectionArticlesExhibits = nlp
				.getAllIndexStartLocations(text, Pattern.compile("(?sm)(SECTION|Section|Article|ARTICLE)")).size();
//		System.out.println("cntSectionArticlesExhibits=="+cntSectionArticlesExhibits);
		if (cntSectionArticlesExhibits > 5 && text.length() < 300) {
			return true;
		}

		if (cntSectionArticlesExhibits > 6 && text.length() < 350) {
			return true;
		}

		if (cntSectionArticlesExhibits > 7 && text.length() < 500) {
			return true;
		}

		if (cntSectionArticlesExhibits > 11 && text.length() < 800) {
			return true;
		}

		if (cntSectionArticlesExhibits > 15 && text.length() < 1200) {
			return true;
		}

		if (cntSectionArticlesExhibits > 18 && text.length() >= 1200) {
			return true;
		}

		return false;

	}

	public static TreeMap<Integer, List<String[]>> getHeadingsPaired(TreeMap<Integer, String[]> map) {

		NLP nlp = new NLP();

		int sIdx, eIdx, sIdxNext = 0, eIdxNext = 0, sIdxPrior = 0, eIdxPrior = 0, cnt = 0, sIdxMainHdg = 0,
				eIdxMainHdg = 0, eIdxMaingHdgPrior = 0, sIdxMaingHdgPrior = 0, mHdgCnt = 0;
		String hdg = "", hdgNext = "", mainHdg = "", mainHdgPrior = "";

		TreeMap<Integer, List<String[]>> mapPaired = new TreeMap<Integer, List<String[]>>();
		List<String[]> listAry = new ArrayList<String[]>();

		boolean recordedMainHeading = false;
		for (Map.Entry<Integer, String[]> entry : map.entrySet()) {
//			System.out.println("cnt=" + cnt + ".................ary=" + Arrays.toString(entry.getValue()));
			recordedMainHeading = false;
			sIdx = Integer.parseInt(entry.getValue()[0]);
			eIdx = Integer.parseInt(entry.getValue()[1]);
			hdg = entry.getValue()[2];
//			System.out.println("ary.....=" + Arrays.toString(entry.getValue()));

			if (cnt + 1 < map.size()) {
//				System.out.println("map cnt+1=" + Arrays.toString(map.get(cnt + 1)));
				eIdxNext = Integer.parseInt(map.get(cnt + 1)[1]);
				sIdxNext = Integer.parseInt(map.get(cnt + 1)[0]);
			}

			// if sIdx>eIdxPrior there is a gap and if sIdxMainHdg<sIdx and eIdxMainHdg>eIdx
			// then record main hdg as hdg and put in map.

//			System.out
//					.println("cnt=" + cnt + " eIdx=" + eIdx + " eIdxNext=" + eIdxNext + " eIdxMainHdg=" + eIdxMainHdg);
			// if sIdx<sIdxNext && eIdx>eIdxnext then this is a main hdg and it relates to a
			// subheading. Record main heading.
			if ((eIdx > eIdxNext && sIdx < sIdxNext) || cnt == 0 || (sIdx > sIdxMainHdg && eIdx > eIdxMainHdg)) {
				if (eIdxPrior < eIdxMainHdg && sIdx > eIdxPrior) {
					String[] ar = { (eIdxPrior + 1) + "", (sIdx - 2) + "", mainHdgPrior };
//					System.out.println("1..add cnt=" + cnt + " subheading==" + Arrays.toString(ar));
					listAry.add(ar);
				}
				mapPaired.put(cnt, listAry);

				listAry = new ArrayList<String[]>();
				cnt++;
				eIdxMainHdg = eIdx;
				sIdxMainHdg = sIdx;
				mainHdg = hdg;
				String[] ary = { sIdxMainHdg + "", eIdxMainHdg + "", mainHdg };
//				System.out.println("2..add cnt=" + cnt + " subheading==" + Arrays.toString(ary));
				listAry.add(ary);
				recordedMainHeading = true;

				eIdxMaingHdgPrior = eIdxMainHdg;
				sIdxMaingHdgPrior = sIdxMainHdg;
				mainHdgPrior = mainHdg;
				// main heading.
//				System.out.println("cnt=" + cnt + " new mainheading==" + entry.getValue()[2]);
			}
//			System.out
//					.println("cnt=" + cnt + " eIdx=" + eIdx + " eIdxNext=" + eIdxNext + " eIdxMainHdg=" + eIdxMainHdg);

			if (sIdx > sIdxMainHdg && eIdx < eIdxMainHdg) {
				listAry = new ArrayList<String[]>();

//				if (sIdx- sIdxMainHdg>10 && eIdxPrior>sIdx ) {
//					String[] ar = { (sIdxMainHdg + 1) + "", (sIdxPrior - 2) + "", mainHdg };
//					listAry.add(ar);
//				}

				if (!recordedMainHeading) {
					String[] ary = { sIdxMainHdg + "", eIdxMainHdg + "", mainHdg };
					listAry.add(ary);
//					System.out.println(".add cnt=" + cnt + " subheading==" + Arrays.toString(ary));

				}
			}

			String[] ary = { sIdx + "", eIdx + "", hdg };
//			System.out.println(".add.put. cnt=" + cnt + " subheading==" + Arrays.toString(ary));
			listAry.add(ary);
			mapPaired.put(cnt, listAry);

			listAry = new ArrayList<String[]>();
			cnt++;

			eIdxPrior = eIdx;
			sIdxPrior = sIdx;
		}
//		NLP.printMapIntListOfStringAry(" mapPaired====", mapPaired);

		return mapPaired;

	}

	public static int getNextParagraph_eidx(int hdgEidx, String text) throws IOException {
		int eidx = 0;
		Pattern pattern = Pattern.compile("\r\n.*[^\r\n]\r\n");
		NLP nlp = new NLP();

//		System.out.println(nlp.getAllMatchedGroups(text.substring(hdgEidx), pattern).get(0));
		if(text.length()<hdgEidx+4)
			return hdgEidx;
		if (nlp.getAllMatchedGroups(text.substring(Math.max(hdgEidx - 1, 0), Math.min(text.length(), hdgEidx + 4)),
				Pattern.compile("\r\n|\\.  ?[A-Z]{1}")).size() == 0)
			// the hdg should be followed by a hard return else don't try to get end of next
			// para. DON'T CHANGE.
			return hdgEidx;

//		System.out.println("txt.len="+text.length()+" hdgEidx="+hdgEidx);
		if (nlp.getAllIndexEndLocations(text.substring(hdgEidx), pattern).size() == 0)
			return hdgEidx;

		eidx = nlp.getAllIndexEndLocations(text.substring(hdgEidx), pattern).get(0) + hdgEidx;
		return eidx;
	}

	public static List<String[]> headingsPaired(TreeMap<Integer, String[]> map)
			throws NumberFormatException, IOException {
		// replaces finalizeMap(TreeMap<Integer, String[]> map)
		// return containers so solr can grab txt and associate hdgs
		NLP nlp = new NLP();

		int sIdx, eIdx, sIdxNext = 0, eIdxNext = 0, sIdxPrior = 0, eIdxPrior = 0, cnt = 0, sIdxMainHdg = 0,
				eIdxMainHdg = 0, eIdxMaingHdgPrior = 0, sIdxMaingHdgPrior = 0, mHdgCnt = 0;
		String hdg = "", hdgNext = "", mainHdg = ""
//				, hdgTyp = "", hdgTypNext = ""
		;

		TreeMap<Integer, List<String[]>> mapPaired = new TreeMap<Integer, List<String[]>>();
//		List<String[]> listAry = new ArrayList<String[]>();

		mapPaired = getHeadingsPaired(map); // pickup here

//		NLP.printMapIntListOfStringAry("mapPaired===", mapPaired);
		// now put each text chunk into its own array as follow, sIdx,eIdx, hdg1, hdg2.
		// subtract 1 from eIdx so I don't overlap. This produces the text chunks that I
		// then run through para/sent/clas/subcl parsers

		String hdg2;
		List<String[]> listArray = new ArrayList<String[]>();
		List<String[]> listHdgs = new ArrayList<String[]>();
		List<String[]> listHdgsFinal = new ArrayList<String[]>();
//		List<String[]> listArrayNext = new ArrayList<String[]>();
		// for some reason - headings get lost here

//		NLP.printMapIntListOfStringAry("mapPaired====", mapPaired);
		for (Map.Entry<Integer, List<String[]>> entry : mapPaired.entrySet()) {
			listArray = entry.getValue();

			cnt++;
			for (int i = 0; i < listArray.size(); i++) {
//				System.out.println("key==" + entry.getKey() + " listArray=" + Arrays.toString(listArray.get(i)));
				sIdx = Integer.parseInt(nlp.getAllMatchedGroups(listArray.get(i)[0], Pattern.compile("[\\d]+")).get(0));
				eIdx = Integer.parseInt(nlp.getAllMatchedGroups(listArray.get(i)[1], Pattern.compile("[\\d]+")).get(0))
						- 1;

				if (listArray.size() == 3) {
					// HERE THE GAP IS i+1 so it gets main hdg only
					// gets gap - 'THIS TXT GETS MAIN HDG ONLY='
					hdg = listArray.get(i)[2];
					sIdx = Integer
							.parseInt(nlp.getAllMatchedGroups(listArray.get(i)[0], Pattern.compile("[\\d]+")).get(0));
					eIdx = Integer.parseInt(
							nlp.getAllMatchedGroups(listArray.get(i)[1], Pattern.compile("[\\d]+")).get(0)) - 1;
					String[] ary = { sIdx + "", eIdx + "", hdg };
					listHdgs.add(ary);
//					System.out.println("1...add ary=" + Arrays.toString(ary));

					hdg2 = listArray.get(i + 2)[2];
					sIdx = Integer.parseInt(
							nlp.getAllMatchedGroups(listArray.get(i + 2)[0], Pattern.compile("[\\d]+")).get(0));
					eIdx = Integer.parseInt(
							nlp.getAllMatchedGroups(listArray.get(i + 2)[1], Pattern.compile("[\\d]+")).get(0)) - 1;
					String[] array = { sIdx + "", eIdx + "", hdg, hdg2 };
					listHdgs.add(array);
//					System.out.println("2...add ary=" + Arrays.toString(array));
					i++;
					i++;
				}

				if (listArray.size() == 2) {
					// gets gap - 'THIS TXT GETS MAIN HDG ONLY='
					hdg = listArray.get(i)[2];
					hdg2 = listArray.get(i + 1)[2];
					sIdx = Integer.parseInt(
							nlp.getAllMatchedGroups(listArray.get(i + 1)[0], Pattern.compile("[\\d]+")).get(0));
					eIdx = Integer.parseInt(
							nlp.getAllMatchedGroups(listArray.get(i + 1)[1], Pattern.compile("[\\d]+")).get(0)) - 1;
					String[] array = { sIdx + "", eIdx + "", hdg, hdg2 };
					listHdgs.add(array);
//					System.out.println("3...add ary=" + Arrays.toString(array));
					i++;
				}

				if (listArray.size() == 1) {
					hdg = listArray.get(i)[2];
					String[] array = { sIdx + "", eIdx + "", hdg };
					listHdgs.add(array);
//					System.out.println("4...add ary=" + Arrays.toString(array));
				}
				sIdxPrior = sIdx;
			}
		}

		TreeMap<String, String> mapTmp = new TreeMap<String, String>();
		sIdx = 0;
		sIdxNext = 0;
		eIdx = 0;
		eIdxNext = 0;

//		NLP.printListOfStringArray("after mapPaired.listHdgs====", listHdgs);
		List<String[]> listSpanHdgs = new ArrayList<String[]>();
		for (int i = 0; i < listHdgs.size(); i++) {
//			System.out.println("listHdgs==" + Arrays.toString(listHdgs.get(i)));
			if (mapTmp.containsKey(listHdgs.get(i)[0] + listHdgs.get(i)[1])
//					&& !listHdgs.get(i)[1].contains("sub=")
			) {
//				System.out.println("1....continue");
				continue;
			}
			sIdx = Integer.parseInt(listHdgs.get(i)[0]);
			eIdx = Integer.parseInt(listHdgs.get(i)[1]);
			if (i + 1 < listHdgs.size()) {
				sIdxNext = Integer.parseInt(listHdgs.get(i + 1)[0]);
				eIdxNext = Integer.parseInt(listHdgs.get(i + 1)[1]);
			}

			if (eIdx > eIdxNext && i + 1 < listHdgs.size()) {
				String[] ary = { listHdgs.get(i)[0], sIdxNext + "", listHdgs.get(i)[2] + "||hdgLen=" + (eIdx - sIdx) };
				listSpanHdgs.add(listHdgs.get(i));
				listHdgsFinal.add(ary);
				mapTmp.put(listHdgs.get(i)[0] + listHdgs.get(i)[1], "");
				// TODO: Store EXH, SEC hdgs to global map then apply after solrMap is finished.
				// TODO: Store sIdx, eIdx in getSolrMap - to then apply
//				System.out.println("2.....continue. 1.add ary==" + Arrays.toString(ary) + " 1.add span="
//						+ Arrays.toString(listHdgs.get(i)));
				continue;
			}
			listHdgsFinal.add(listHdgs.get(i));
			mapTmp.put(listHdgs.get(i)[0] + listHdgs.get(i)[1], "");
//			System.out.println("2. add ary=="+Arrays.toString(listHdgs.get(i)));
		}
//		NLP.printListOfStringArray("main hdgs == ", listSpanHdgs);
//		NLP.printListOfStringArray("listHdgsFinal == ", listHdgsFinal);

		List<String[]> listHdgsFinal2 = new ArrayList<String[]>();
		boolean mainPaired = false, added = false;
		for (int i = 0; i < listHdgsFinal.size(); i++) {
			mainPaired = false;
			added = false;
//			System.out.println("i==" + i + " source ary===" + Arrays.toString(listHdgsFinal.get(i)));

			if (nlp.getAllIndexEndLocations(Arrays.toString(listHdgsFinal.get(i)), Pattern.compile("hdgLen"))
					.size() == 0) {
				sIdx = Integer.parseInt(listHdgsFinal.get(i)[0].trim());
				eIdx = Integer.parseInt(listHdgsFinal.get(i)[1].trim());

				for (int c = 0; c < listSpanHdgs.size(); c++) {
					if (mainPaired)
						break;
					sIdxMainHdg = Integer.parseInt(listSpanHdgs.get(c)[0].trim());
					eIdxMainHdg = Integer.parseInt(listSpanHdgs.get(c)[1].trim());
//					System.out.println("1 sIdx=" + sIdx + " eIdx=" + eIdx + " sIdxMainHdg=" + sIdxMainHdg
//							+ " eIdxMainHdg=" + eIdxMainHdg + " listHdgsFinal ary="
//							+ Arrays.toString(listHdgsFinal.get(i)) + " dif====" + (eIdxMainHdg - eIdx));

					if (sIdxMainHdg < sIdx && eIdx <= eIdxMainHdg && listHdgsFinal.get(i).length > 3) {
//						System.out.println("2 sIdx=" + sIdx + " eIdx=" + eIdx + " sIdxMainHdg=" + sIdxMainHdg
//								+ " eIdxMainHdg=" + eIdxMainHdg + " listHdgsFinal ary="
//								+ Arrays.toString(listHdgsFinal.get(i)) + " dif====" + (eIdxMainHdg - eIdx));
						String[] ary = { listHdgsFinal.get(i)[0], listHdgsFinal.get(i)[1],
								listHdgsFinal.get(i)[2] + "||hdgLen=" + (eIdxMainHdg - sIdxMainHdg),
								listHdgsFinal.get(i)[3] };
						listHdgsFinal2.add(ary);
//						System.out.println("i=" + i + " 1...main ary==" + Arrays.toString(ary));

						mainPaired = true;
						added = true;
						break;
					}

					if (mainPaired)
						break;

				}

				if (!mainPaired && !added) {
//					System.out.println("i=" + i + " 2...main ary==" + Arrays.toString(listHdgsFinal.get(i)));
					listHdgsFinal2.add(listHdgsFinal.get(i));
					added = true;
				}
			}

			if (!added) {
//				System.out.println("i=" + i + " 3...main ary==" + Arrays.toString(listHdgsFinal.get(i)));
				listHdgsFinal2.add(listHdgsFinal.get(i));
				added = true;
			}
		}

//		NLP.printListOfStringArray("listHdgsFinal2==", listHdgsFinal2);
		return listHdgsFinal2;

	}

	public String gettingSolrFileReady(String text, File fileSolr, List<String> metadata, boolean parsingDisclosure,
			String solrCore, boolean parseClientText) throws IOException, SolrServerException, SQLException {

		System.out.println("getting solrFileReady at 6338 f=" + fileSolr.getAbsolutePath());
		NLP nlp = new NLP();
		// GoLaw gl = new GoLaw();
		long startTime = System.currentTimeMillis();

		// TODO: NOTE SEE COMMENTS RE BOOLEANS BELOW!!
//		boolean getExhibitsMapped = true;//undo for disclosure?? search rest of method for this boolean to reinstatiate OR call another method all together and don't mix with this one.
//		boolean getSectionsMapped = true;//undo for disclosure?? search rest of method for this boolean to reinstatiate OR call another method all together and don't mix with this one.
//		boolean getDefinitionsMapped = true;//undo for disclosure?? search rest of method for this boolean to reinstatiate OR call another method all together and don't mix with this one.

		List<String[]> headingsPaired = new ArrayList<String[]>();

		parseClientText = false;
//		if (parsingDisclosure && !parseClientText) {
//			getExhibitsMapped = false;
//			getSectionsMapped = false;
//			getDefinitionsMapped = false;
//		}
//		System.out.println("getSectionsMapped==" + getSectionsMapped);
		TreeMap<Double, String[]> mapIdx = new TreeMap<Double, String[]>();
		if (!parseClientText) {
			mapIdx = getContractToc(text);
		}

		startTime = System.currentTimeMillis();
		mapIdx = getExhibitsMapped(text, mapIdx);
		startTime = System.currentTimeMillis();
		startTime = System.currentTimeMillis();

		mapIdx = getSectionsMapped(text, mapIdx);
		System.out.println("getSectionsMapped mapIdx.size() is now=" + mapIdx.size());
		PrintWriter pw = new PrintWriter("TOC.txt");
		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter(new File("c:/temp/5. Sections.txt"));
			for (Map.Entry<Double, String[]> entry : mapIdx.entrySet()) {
				pw.append("5. sections=" + Arrays.toString(entry.getValue()) + "\r\n");
			}
			pw.close();
		}
		System.out.println("getSectionsMapped seconds=" + (System.currentTimeMillis() - startTime) / 1000);
//		NLP.printMapDblStrAry("getSections mapIdx=", mapIdx);
//			}

//			if (getDefinitionsMapped) {undo for disclosure??
		mapIdx = getDefinitionsMapped(text, mapIdx);
//				System.out.println("getDefinitionsMapped seconds " + (System.currentTimeMillis() - startTime) / 1000);
		startTime = System.currentTimeMillis();
		System.out.println("getDefinitionsMapped. mapIdx.size() is now=" + mapIdx.size());
//		NLP.printMapDblStrAry("getDefinitionsMapped mapIdx=", mapIdx);
		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter(new File("c:/temp/6. Definitions.txt"));
			if (GetContracts.turnOnPrinterWriter) {
				for (Map.Entry<Double, String[]> entry : mapIdx.entrySet()) {

					pw.append("6. definitions=" + Arrays.toString(entry.getValue()) + "\r\n");
				}
				pw.close();
			}
		}

		TreeMap<String, Integer[]> mapDefStr_sIdx_eIdx = new TreeMap<String, Integer[]>();
		String keyStr;
		for (Map.Entry<Double, String[]> entry : mapIdx.entrySet()) {
			keyStr = entry.getValue()[2].replaceAll("def=", "").replaceAll("Qx|XQ|[^a-zA-Z ]+", "").trim();
//			System.out.println("map def ck key==" + keyStr);
			Integer[] intAry = { Integer.parseInt(entry.getValue()[0]), Integer.parseInt(entry.getValue()[0]) };
			mapDefStr_sIdx_eIdx.put(keyStr, intAry);
		}
		startTime = System.currentTimeMillis();
		System.out.println("getHeadings");
		mapIdx = getHeadings(text, mapIdx, parsingDisclosure);// hdg has sub-hdg - then name hdg sec and sub-hdg child
		System.out.println("total time for getHeadings seconds " + (System.currentTimeMillis() - startTime) / 1000);
		startTime = System.currentTimeMillis();

//			System.out.println("getHeadings.size=" + mapIdx.size());
//		NLP.printMapDblStrAry("getHeadings mapIdx=", mapIdx);
		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter(new File("c:/temp/7d. Heading.txt"));
			for (Map.Entry<Double, String[]> entry : mapIdx.entrySet()) {
				pw.append("7d. heading=" + Arrays.toString(entry.getValue()) + "\r\n");
			}
			pw.close();
		}
		List<String[]> listNearFinal = scrubMapIdx(text, mapIdx, parsingDisclosure, mapDefStr_sIdx_eIdx);
//			System.out.println("scrubMapIdx seconds " + (System.currentTimeMillis() - startTime) / 1000);
		startTime = System.currentTimeMillis();

		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter(new File("c:/temp/8a. listNearFinal.txt"));
			if (GetContracts.turnOnPrinterWriter) {
				for (int i = 0; i < listNearFinal.size(); i++) {
					pw.append("8a. listNearFinal=" + Arrays.toString(listNearFinal.get(i)) + "\r\n");
				}
				pw.close();
			}
		}

		List<String[]> listFinal = fixEndIndexesOfEndingHeadings(listNearFinal, text);
//			System.out.println(
//					"fixEndIndexesOfEndingHeadings seconds " + (System.currentTimeMillis() - startTime) / 1000);
		startTime = System.currentTimeMillis();

//			NLP.printListOfStringArray("scrubMapIdx==", listFinal);
		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter(new File("c:/temp/8b. listFinal.txt"));

			for (int i = 0; i < listFinal.size(); i++) {
				pw.append("8b. listFinal=" + Arrays.toString(listFinal.get(i)) + "\r\n");
			}
			pw.close();
		}

//		PrintWriter pw = new PrintWriter(new File("c:/temp/text at ScrubHdg.txt"));
//		pw.append(text);
//		pw.close();
		TreeMap<Integer, String[]> mapIdxFinal = getSubHeadingsFinalScrub(text, listFinal, parsingDisclosure);
//		System.out.println("getSubHeadingsFinalScrub seconds " + (System.currentTimeMillis() - startTime) / 1000);
		startTime = System.currentTimeMillis();
		int key = 0;
		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter(new File("c:/temp/9. getSubHeadingsFinalScrub.txt"));
			if (GetContracts.turnOnPrinterWriter) {
				for (Map.Entry<Integer, String[]> entry : mapIdxFinal.entrySet()) {
					key = entry.getKey();
					pw.append("9. final key=" + key + " ary=" + Arrays.toString(entry.getValue()) + "\r\n");
				}
				pw.close();
			}
		}

		TreeMap<Integer, String[]> mapIdxFinal2 = mark_what_is_consecutive(mapIdxFinal, true);

		List<String[]> headingsPaired_tmp = new ArrayList<String[]>();

		for (Map.Entry<Integer, String[]> entry : mapIdxFinal2.entrySet()) {
			if (entry.getValue()[2].length() > 35 && nlp
					.getAllMatchedGroups(entry.getValue()[2], Pattern.compile("(sec|hdg|mHd|sub)=(Qx)?.{1,230}XQ"))
					.size() > 0) {
				String[] ary = { entry.getValue()[0], entry.getValue()[1], nlp
						.getAllMatchedGroups(entry.getValue()[2], Pattern.compile("(sec|hdg|mHd|sub)=(Qx)?.{1,230}XQ"))
						.get(0) };
				headingsPaired_tmp.add(ary);// keep
			}

			else {
				headingsPaired_tmp.add(entry.getValue());// keep
			}
		}

		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter(new File("c:/temp/9b. final_fix_of_eIdx_of_headings.txt"));
			for (int i = 0; i < headingsPaired_tmp.size(); i++) {
				pw.append("9b=" + Arrays.toString(headingsPaired_tmp.get(i)) + "\r\n");
			}
			pw.close();
		}

		TreeMap<Integer, String[]> map_headingsPairedFinal = final_fix_of_eIdx_of_headings(headingsPaired_tmp, text);
		int sIdx = 0, eIdx = 0;
		TreeMap<Integer, String[]> map_headingsPairedFinal2 = new TreeMap<Integer, String[]>();
		int cntt = 0;

//		NLP.printMapIntStringAry("map_headingsPairedFinal", map_headingsPairedFinal);

		String hdgTmp, hdg1, hdg2;
		int sidx1 = 0, eidx1 = 0, neweidx = 0, sidx1N = 0, eidx1N = 0;
		int cnt2 = 0, smallDist = 20;
		boolean mapS = true;
		for (Map.Entry<Integer, String[]> entry : map_headingsPairedFinal.entrySet()) {
//			System.out.println("map_headingsPairedFinal ary="+Arrays.toString( entry.getValue()));
			sidx1 = Integer.parseInt(map_headingsPairedFinal.get(cnt2)[0]);
			eidx1 = Integer.parseInt(map_headingsPairedFinal.get(cnt2)[1]);
			mapS = false;
			hdg1 = "";
			hdg2 = "";
			hdgTmp = map_headingsPairedFinal.get(cnt2)[2].replaceAll("(sec|def|hdg|mHd)=", "").replaceAll("[ ]+", " ")
					.trim();

			if (nlp.getAllMatchedGroups(hdgTmp, Pattern.compile(".*?(?=consecutive=)")).size() > 0) {
				hdgTmp = nlp.getAllMatchedGroups(hdgTmp, Pattern.compile(".*?(?=consecutive=)")).get(0);
//				System.out.println("1.hdgTmp=" + hdgTmp + " repl="
//						+ hdgTmp.replaceAll("[^a-zA-Z ]+", "").replaceAll("Qx|XQ|\\([a-zA-Z\\d\\.]{1,5} |\"", "")
//						+ " meas=" + SolrPrep.isItMostlyInitialCaps_measure(hdgTmp.replaceAll("[^a-zA-Z ]+", "")
//								.replaceAll("Qx|XQ|\\([a-zA-Z\\d\\.]{1,5} |\"", "")));
			}

			if (SolrPrep.isItMostlyInitialCaps_measure(
					hdgTmp.replaceAll("[^a-zA-Z ]+", "").replaceAll("Qx|XQ|\\([a-zA-Z\\d\\.]{1,5} |\"", "")) < .35) {// don't
				// chg!

//				System.out.println("2.continue.mostly small caps.==" + hdgTmp + " repl="
//						+ hdgTmp.replaceAll("[^a-zA-Z ]+", "").replaceAll("Qx|XQ|\\([a-zA-Z\\d\\.]{1,5}\\) |\"", ""));
				hdg1 = hdgTmp;
				cnt2++;
				continue;

			}
			if (map_headingsPairedFinal.get(cnt2).length > 3) {
				hdgTmp = map_headingsPairedFinal.get(cnt2)[3].replaceAll("(sec|def|hdg|mHd)=", "")
						.replaceAll("[ ]+", " ").trim();
//				System.out.println("3.hdgTmp=" + hdgTmp + " repl="
//						+ hdgTmp.replaceAll("[^a-zA-Z ]+", "").replaceAll("Qx|XQ|\\([a-zA-Z\\d\\.]{1,5} |\"", "")
//						+ " meas=" + SolrPrep.isItMostlyInitialCaps_measure(hdgTmp.replaceAll("[^a-zA-Z ]+", "")
//								.replaceAll("Qx|XQ|\\([a-zA-Z\\d\\.]{1,5} |\"", "")));

				if (nlp.getAllMatchedGroups(hdgTmp, Pattern.compile(".*?(?=consecutive=)")).size() > 0) {
					hdgTmp = nlp.getAllMatchedGroups(hdgTmp, Pattern.compile(".*?(?=consecutive=)")).get(0);
//					System.out.println("4.hdgTmp=" + hdgTmp);
				}

				if (SolrPrep.isItMostlyInitialCaps_measure(hdgTmp.replaceAll("[^a-zA-Z ]+", "")
						.replaceAll("Qx|XQ|\\([a-zA-Z\\d\\.]{1,5} |\"", "")) < .35) {// don't
																						// chg!
//					System.out.println("5.continue.mostly small caps.==" + hdgTmp + " repl=" + hdgTmp
//							.replaceAll("[^a-zA-Z ]+", "").replaceAll("Qx|XQ|\\([a-zA-Z\\d\\.]{1,5}\\) |\"", ""));
					hdg2 = hdgTmp;
					cnt2++;
					continue;
				}
			}

			hdgTmp = map_headingsPairedFinal.get(cnt2)[2].replaceAll("(sec|def|hdg|mHd)=", "").replaceAll("[ ]+", " ")
					.trim();

			if (map_headingsPairedFinal.size() > cnt2 + 1) {
				sidx1N = Integer.parseInt(map_headingsPairedFinal.get(cnt2 + 1)[0]);
				eidx1N = Integer.parseInt(map_headingsPairedFinal.get(cnt2 + 1)[1]);
				mapS = true;
			}

			if (entry.getValue().length == 3) {
				if (mapS && eidx1 - sidx1 - entry.getValue()[2].length() <= smallDist) {
					neweidx = getNextParagraph_eidx(eidx1, text);
					if (neweidx < eidx1N)
						eidx1 = neweidx;
					String[] ary = { sidx1 + "", eidx1 + "", entry.getValue()[2] };
					map_headingsPairedFinal2.put(cntt, ary);
					cntt++;
				}

				if (eidx1 - sidx1 - entry.getValue()[2].length() > smallDist) {
					String[] ary = { sidx1 + "", eidx1 + "", entry.getValue()[2] };
					map_headingsPairedFinal2.put(cntt, ary);
					cntt++;
				}
				if (eidx1 - sidx1 - entry.getValue()[2].length() <= smallDist
						&& nlp.getAllMatchedGroups(entry.getValue()[2],
								Pattern.compile("(?ism)(delete|reserve|omit|intentional)")).size() > 1) {
					String[] ary = { sidx1 + "", eidx1 + "", entry.getValue()[2] };
					map_headingsPairedFinal2.put(cntt, ary);
					cntt++;
				}

			}
			if (entry.getValue().length == 4) {
				if (mapS && eidx1 - sidx1 - entry.getValue()[3].length() <= smallDist) {
					neweidx = getNextParagraph_eidx(eidx1, text);
					if (neweidx < eidx1N)
						eidx1 = neweidx;
					String[] ary = { sidx1 + "", eidx1 + "", entry.getValue()[2] };
					map_headingsPairedFinal2.put(cntt, ary);
					cntt++;
				}

				if (eidx1 - sidx1 - entry.getValue()[3].length() > smallDist) {
					String[] ary = { sidx1 + "", eidx1 + "", entry.getValue()[2] };
					map_headingsPairedFinal2.put(cntt, ary);
					cntt++;
				}
			}
			cnt2++;
		}

		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter(new File("c:/temp/9c. final_fix_of_eIdx_of_headings.txt"));
			for (Map.Entry<Integer, String[]> entry : map_headingsPairedFinal2.entrySet()) {
				pw.append("9c." + entry.getKey() + "| val|" + Arrays.toString(entry.getValue()) + "\r\n");
			}
			pw.close();
		}

//		NLP.printMapIntStringAry("map_headingsPairedFinal2==", map_headingsPairedFinal2);

		headingsPaired = headingsPaired(map_headingsPairedFinal2);
//		NLP.printListOfStringArray("headingsPaired===", headingsPaired);

		// System.out.println("at temp3....");
//		headingsPaired = temp3.plugGaps(map_headingsPairedFinal);
//		System.out.println("headingsPaired seconds " + (System.currentTimeMillis() - startTime) / 1000);
		startTime = System.currentTimeMillis();

		System.out.println("headingsPaired.size=" + headingsPaired.size());
		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter(new File("c:/temp/10. hdgs-p.txt"));// SEE IF THIS IS STILL GOOD AFTER MOVING final_fix
			for (int i = 0; i < headingsPaired.size(); i++) {
				if (headingsPaired.get(i).length == 3) {
					if (Integer.parseInt(headingsPaired.get(i)[1]) - Integer.parseInt(headingsPaired.get(i)[0])
							- headingsPaired.get(i)[2].length() < 10) {
						pw.append("10. headingsPaired small hdg==" + Arrays.toString(headingsPaired.get(i)) + "\r\n");
					} else {
						pw.append("10. headingsPaired==" + Arrays.toString(headingsPaired.get(i)) + "\r\n");
					}
				}
				if (headingsPaired.get(i).length == 4) {
					if (Integer.parseInt(headingsPaired.get(i)[1]) - Integer.parseInt(headingsPaired.get(i)[0])
							- headingsPaired.get(i)[3].length() < 10) {
						pw.append("10. headingsPaired small hdg==" + Arrays.toString(headingsPaired.get(i)) + "\r\n");
					} else {
						pw.append("10. headingsPaired==" + Arrays.toString(headingsPaired.get(i)) + "\r\n");
					}
				}

			}
			pw.close();
		}

//		TreeMap<Integer, String[]> map_headingsPairedFinal = final_fix_of_eIdx_of_headings(headingsPaired, text);//undelete
		// final_fix_of_eIdx_of_headings: this finds the last consecutive heading and
		// stubs the eIdx and finds consecutive headings where next hdg sIdx is not
		// close to eIdx and moves the eIdx close to the sIdx.
//		headingsPaired = headingsPairedFinal;

		List<String[]> headingsPairedFinal = new ArrayList<String[]>();
		eIdx = 0;
		int sIdxNext = 0;
		for (int i = 0; i < headingsPaired.size(); i++) {
//			pw.append("map_hdgs_final: ary.len=" + entry.getValue().length + "||" + Arrays.toString(entry.getValue())
//					+ "\r\n");
			eIdx = Integer.parseInt(headingsPaired.get(i)[1]);
			sIdxNext = 0;
			if (i + 1 < headingsPaired.size()) {
				sIdxNext = Integer.parseInt(headingsPaired.get(i + 1)[0]);
			}
			if (headingsPaired.size() > i + 1 && eIdx > sIdxNext) {
				eIdx = sIdxNext - 1;
				if (headingsPaired.get(i).length == 3) {
					String[] ary = { headingsPaired.get(i)[0] + "", eIdx + "",
							headingsPaired.get(i)[2].replaceAll("\r\n", " ").replaceAll("[ ]+", " ") };
					headingsPairedFinal.add(ary);
				}
				if (headingsPaired.get(i).length == 4) {
					String[] ary = { headingsPaired.get(i)[0] + "", eIdx + "",
							headingsPaired.get(i)[2].replaceAll("\r\n", " ").replaceAll("[ ]+", " "),
							headingsPaired.get(i)[3].replaceAll("\r\n", " ").replaceAll("[ ]+", " ") };
					headingsPairedFinal.add(ary);
				}
			} else {
				headingsPairedFinal.add(headingsPaired.get(i));
			}
		}

		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter(new File("c:/temp/11. Hdgs Final.txt"));
			if (GetContracts.turnOnPrinterWriter) {
				for (int i = 0; i < headingsPairedFinal.size(); i++) {
					pw.append("11. headingsPairedFinal==" + Arrays.toString(headingsPairedFinal.get(i)) + "||\r\n");
				}
				pw.close();
			}
		}

//		List<String[]> headingsPairedFinal = headingsPaired(map_headingsPairedFinal);

		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter(new File("c:/temp/11ary. Hdgs Final.txt"));
		}
		TreeMap<Integer, String[]> mapToOrderIt = new TreeMap<Integer, String[]>();
//		String headg="", headgP="";
		for (int i = 0; i < headingsPairedFinal.size(); i++) {
			if (GetContracts.turnOnPrinterWriter) {
//				System.out.println("11ary hdg=" + Arrays.toString(headingsPairedFinal.get(i)));
				pw.append("11ary. Hdgs Final=" + Arrays.toString(headingsPairedFinal.get(i)) + "||\r\n");
			}
			key = Integer.parseInt(headingsPairedFinal.get(i)[0]);
			mapToOrderIt.put(key, headingsPairedFinal.get(i));
		}

		if (GetContracts.turnOnPrinterWriter) {
			pw.close();
		}

		headingsPairedFinal = new ArrayList<String[]>();
		sIdx = 0;
		int sIdxNxt = 0, cnt = 0;
		eIdx = 0;
		String mn_hdg;
		for (Map.Entry<Integer, String[]> entry : mapToOrderIt.entrySet()) {
			headingsPairedFinal.add(entry.getValue());
//			pw.append("12. final==" + Arrays.toString(entry.getValue()) + "||\r\n");
//			System.out.println("12. final==" + Arrays.toString(entry.getValue()));
		}
//		pw.close();

		List<String[]> headingsPairedFinal2 = new ArrayList<String[]>();
		int eIdxFixed = 0;
		String hdg;

		int eIdxP = 0;
		String hdgP = "", hdgPtmp;
		for (int i = 0; i < headingsPairedFinal.size(); i++) {
			hdgP = "";
			hdgPtmp = "";
//			System.out.println("at12. ary="+Arrays.toString(headingsPairedFinal.get(i))+" ary.len="+headingsPairedFinal.get(i).length);

			sIdx = Integer.parseInt(headingsPairedFinal.get(i)[0]);
			eIdx = Integer.parseInt(headingsPairedFinal.get(i)[1]);
			hdg = headingsPairedFinal.get(i)[2];
			if (i > 0) {
				eIdxP = Integer.parseInt(headingsPairedFinal.get(i - 1)[1]);
				hdgP = headingsPairedFinal.get(i - 1)[2];
			}

//			if (eIdx - sIdx < 30) {
//				System.out.println("at 12 - to small a hdg == ary.len==" + Arrays.toString(headingsPairedFinal.get(i)));
//				continue;
//			}

			if (sIdx - eIdxP > 50 && i > 0) {
//				System.out.println(
//						"at 12 - gap? sIdx=" + sIdx + " eIdxP=" + eIdxP + " hdg=" + hdg + "\r\na.hdgP=" + hdgP);

//				String tmpHdgP=hdgP, tmpHdg=hdg;
//				if(tmpHdgP.indexOf(", consecutive")>0) {
//					tmpHdgP=nlp.getAllMatchedGroups(tmpHdgP, Pattern.compile(".*?(?=, consecutive)")).get(0);
//					System.out.println("tmpHdgP="+tmpHdgP);
//				}
				hdgPtmp = hdgP.replaceAll("(sec|mHd|hdg|sub)=|Qx|XQ", "").replaceAll(", consecutive.*?$", "").trim();
				hdgTmp = hdg.replaceAll("(sec|mHd|hdg|sub)=|Qx|XQ", "").replaceAll(", consecutive.*?$", "").trim();
//				System.out.println("1hdgP repl==" + hdgPtmp);
//				System.out.println("2hdg  repl==" + hdgTmp);

				if (hdgPtmp.equals(hdgTmp)) {
					String[] ary = { (eIdxP + 1) + "", (sIdx - 1) + "", hdgPtmp };
//					System.out.println("3.adding hdgP=" + Arrays.toString(ary));
					headingsPairedFinal2.add(ary);
				}

			}

			if (i + 1 < headingsPairedFinal.size()) {
				sIdxNext = Integer.parseInt(headingsPairedFinal.get(i + 1)[0]);
			}

//			System.out.println("hdg. eIdx=" + Integer.parseInt(headingsPairedFinal.get(i)[1]) + " sIdx="
//					+ Integer.parseInt(headingsPairedFinal.get(i)[0]) + " len=" + headingsPairedFinal.get(i)[2].length()
//					+ " eIdx-sIdx=" + (eIdx - sIdx) + " sIdxNext=" + sIdxNext + " " + headingsPairedFinal.get(i)[2]);

			if ((eIdx - sIdx) - headingsPairedFinal.get(i)[2].length() < 4
					&& (sIdxNext - eIdx > 100 || i + 1 == headingsPairedFinal.size())) {
				// if sIdxNext is close by there's no para
//				System.out.println("hdg has not related text=" + headingsPairedFinal.get(i)[2]);
//				System.out.println("txt snip=" + text.substring(eIdx, eIdx + 70));

				eIdxFixed = 0;
				eIdxFixed = GetContracts.getNextParagraph_eIdx(text, eIdx);
//				System.out.println("eIdxFixed==" + eIdxFixed);
				if ((eIdxFixed < sIdxNext || (headingsPairedFinal.size() == i + 1 && eIdxFixed < text.length())
						&& eIdxFixed - eIdx < 3500) && eIdxFixed != 0) {
//					System.out.println("sIdx=" + sIdx + " eIdx=" + eIdx + " fixed eIdx===" + eIdxFixed + " para="
//							+ text.substring(sIdx, eIdx));
					eIdx = eIdxFixed;
				}
			}

			if (headingsPairedFinal.get(i).length == 3) {
				String[] ary = { sIdx + "", eIdx + "", hdg };
				headingsPairedFinal2.add(ary);
				continue;
			}

			if (headingsPairedFinal.get(i).length == 4) {
				String[] ary = { sIdx + "", eIdx + "", hdg, headingsPairedFinal.get(i)[3] };
				headingsPairedFinal2.add(ary);
			}
		}

//		NLP.printListOfStringArray("headingsPairedFinal2", headingsPairedFinal2);

		List<String[]> headingsPairedFinal3 = new ArrayList<String[]>();
		String sIdxStr, eIdxStr, hdgM = "", hdgS = "";
		for (int i = 0; i < headingsPairedFinal2.size(); i++) {
			sIdxStr = headingsPairedFinal2.get(i)[0];
			eIdxStr = headingsPairedFinal2.get(i)[1];
			hdgM = headingsPairedFinal2.get(i)[2];
			if (headingsPairedFinal2.get(i).length == 4) {
				hdgS = headingsPairedFinal2.get(i)[3];
				hdgS = hdgS.replaceAll("(def|sec|mHd|hdg|sub|exh)=", "");

				if (hdgS.equals(hdgM.replaceAll("(def|sec|mHd|hdg|sub|exh)=", ""))) {
					String[] ary = { sIdxStr, eIdxStr, hdgM };
					headingsPairedFinal3.add(ary);
				} else {
					headingsPairedFinal3.add(headingsPairedFinal2.get(i));
				}
				continue;
			}
			headingsPairedFinal3.add(headingsPairedFinal2.get(i));
		}

		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter(new File("c:/temp/12. Hdgs Final.txt"));

			for (int i = 0; i < headingsPairedFinal3.size(); i++) {

				sIdx = Integer.parseInt(headingsPairedFinal3.get(i)[0]);
				if (i > 0) {
					eIdxP = Integer.parseInt(headingsPairedFinal3.get(i - 1)[1]);
				}
				if (sIdx - eIdxP > 50) {
					pw.append("12 hdg.gap ary.len=" + headingsPairedFinal3.get(i).length + " ary="
							+ Arrays.toString(headingsPairedFinal3.get(i)) + "\r\n");
				} else {
					pw.append("12 hdg ary.len=" + headingsPairedFinal3.get(i).length + " ary="
							+ Arrays.toString(headingsPairedFinal3.get(i)) + "\r\n");
				}
			}
		}
		pw.close();

		List<String[]> headingsPairedFinal4 = new ArrayList<String[]>();
		int sIdxNx = 0;
		String hdgNx = "", sbHdg = "", hdgtmp = "";
		for (int i = 0; i < headingsPairedFinal3.size(); i++) {
//			System.out.println("aft 12." + Arrays.toString(headingsPairedFinal3.get(i)));
			hdg1 = "";
			hdg2 = "";
			hdgtmp = headingsPairedFinal3.get(i)[2].replaceAll("(sec|def|hdg|mHd)=", "").replaceAll("[ ]+", " ").trim();

			if (nlp.getAllMatchedGroups(hdgtmp, Pattern.compile(".*?(?=consecutive=)")).size() > 0) {
				hdgtmp = nlp.getAllMatchedGroups(hdgtmp, Pattern.compile(".*?(?=consecutive=)")).get(0);
//				System.out.println("1.hdgtmp=" + hdgtmp + " repl="
//						+ hdgtmp.replaceAll("[^a-zA-Z ]+", "").replaceAll("Qx|XQ|\\([a-zA-Z\\d\\.]{1,5} |\"", "")
//						+ " meas=" + SolrPrep.isItMostlyInitialCaps_measure(hdgtmp.replaceAll("[^a-zA-Z ]+", "")
//								.replaceAll("Qx|XQ|\\([a-zA-Z\\d\\.]{1,5} |\"", "")));
			}

			if (SolrPrep.isItMostlyInitialCaps_measure(
					hdgtmp.replaceAll("[^a-zA-Z ]+", "").replaceAll("Qx|XQ|\\([a-zA-Z\\d\\.]{1,5} |\"", "")) < .35) {// don't
				// chg!

//				System.out.println("1.continue.mostly small caps.==" + hdgtmp + " repl="
//						+ hdgtmp.replaceAll("[^a-zA-Z ]+", "").replaceAll("Qx|XQ|\\([a-zA-Z\\d\\.]{1,5}\\) |\"", ""));
//				continue;
				hdg1 = hdgtmp;
			}
			if (headingsPairedFinal3.get(i).length > 3) {
				hdgtmp = headingsPairedFinal3.get(i)[3].replaceAll("(sec|def|hdg|mHd)=", "").replaceAll("[ ]+", " ")
						.trim();
//				System.out.println("2.hdgtmp=" + hdgtmp + " repl="
//						+ hdgtmp.replaceAll("[^a-zA-Z ]+", "").replaceAll("Qx|XQ|\\([a-zA-Z\\d\\.]{1,5} |\"", "")
//						+ " meas=" + SolrPrep.isItMostlyInitialCaps_measure(hdgtmp.replaceAll("[^a-zA-Z ]+", "")
//								.replaceAll("Qx|XQ|\\([a-zA-Z\\d\\.]{1,5} |\"", "")));

				if (nlp.getAllMatchedGroups(hdgtmp, Pattern.compile(".*?(?=consecutive=)")).size() > 0) {
					hdgtmp = nlp.getAllMatchedGroups(hdgtmp, Pattern.compile(".*?(?=consecutive=)")).get(0);
//					System.out.println("2.hdgtmp=" + hdgtmp);
				}

				if (SolrPrep.isItMostlyInitialCaps_measure(hdgtmp.replaceAll("[^a-zA-Z ]+", "")
						.replaceAll("Qx|XQ|\\([a-zA-Z\\d\\.]{1,5} |\"", "")) < .35) {// don't
																						// chg!
//					System.out.println("2.continue.mostly small caps.==" + hdgtmp + " repl=" + hdgtmp
//							.replaceAll("[^a-zA-Z ]+", "").replaceAll("Qx|XQ|\\([a-zA-Z\\d\\.]{1,5}\\) |\"", ""));
//					continue;
					hdg2 = hdgtmp;
				}
			}

			sIdx = Integer.parseInt(headingsPairedFinal3.get(i)[0]);
			eIdx = Integer.parseInt(headingsPairedFinal3.get(i)[1]);
			hdg = headingsPairedFinal3.get(i)[2];

			if (i + 1 < headingsPairedFinal3.size()) {
				sIdxNx = Integer.parseInt(headingsPairedFinal3.get(i + 1)[0]);
//				eIdxNx= Integer.parseInt( headingsPairedFinal3.get(i + 1)[1]);
				hdgNx = headingsPairedFinal3.get(i + 1)[2];
			}

			if (hdg.contains("XxX")) {
				// then eIdx=sIdxN
				eIdx = sIdxNx - 1;
			}

//			System.out.println("array.len==" + headingsPairedFinal3.get(i).length);
			if (headingsPairedFinal3.get(i).length == 4) {
				sbHdg = headingsPairedFinal3.get(i)[3];
				if (hdg1.length() > 0)
					hdg = "";
				if (hdg2.length() > 0)
					sbHdg = "";
				String[] array = { sIdx + "", eIdx + "", hdg.replaceAll("^[Xx]+", ""), sbHdg.replaceAll("^[Xx]+", "") };
//				System.out.println("1added.ary==" + Arrays.toString(array) + "\r\norig=="
//						+ Arrays.toString(headingsPairedFinal3.get(i)));
				headingsPairedFinal4.add(array);

			} else {
//				if (hdg1.length() > 0) {
//					hdg = "";
//				}
				String[] array = { sIdx + "", eIdx + "", hdg.replaceAll("^[Xx]+", "") };
//				System.out.println("2added==" + Arrays.toString(array));
				headingsPairedFinal4.add(array);

			}
		}

		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter(new File("c:/temp/14. Hdgs Final.txt"));

			for (int i = 0; i < headingsPairedFinal4.size(); i++) {

				sIdx = Integer.parseInt(headingsPairedFinal4.get(i)[0]);
				eIdx = Integer.parseInt(headingsPairedFinal4.get(i)[1]);
				if (i > 0) {
					eIdxP = Integer.parseInt(headingsPairedFinal4.get(i - 1)[1]);
				}
				if (sIdx - eIdxP > 50) {
					if (eIdx - sIdx > 50) {
						pw.append("14.>50 hdg.gap ary.len=" + headingsPairedFinal4.get(i).length + " ary="
								+ Arrays.toString(headingsPairedFinal4.get(i)) + "\r\n");
					} else {
						pw.append("14.<50 hdg.gap ary.len=" + headingsPairedFinal4.get(i).length + " ary="
								+ Arrays.toString(headingsPairedFinal4.get(i)) + "\r\n");
					}

				} else {
					if (eIdx - sIdx > 50) {
						pw.append("14.>50 hdg. ary.len=" + headingsPairedFinal4.get(i).length + " ary="
								+ Arrays.toString(headingsPairedFinal4.get(i)) + "\r\n");
					} else {
						pw.append("14.<50 hdg. ary.len=" + headingsPairedFinal4.get(i).length + " ary="
								+ Arrays.toString(headingsPairedFinal4.get(i)) + "\r\n");
					}

				}
//			if(nlp.getAllMatchedGroups(headingsPairedFinal4.get(i)[2], patternExcludeMeFromHeadings).size()>0) {
//				System.out.println("exclude???"+headingsPairedFinal4.get(i)[2]);
//			}

			}
		}
		pw.close();

		List<String[]> headingsPairedFinal5 = new ArrayList<String[]>();
		hdgNx = "";
		hdg = "";
		int aryLen = 0, aryLenNx = 0;
		Pattern ptrn = Pattern.compile(".*?(?=(,? ?consecutive=|\\|\\|))");
//		NLP.printListOfStringArray("4..", headingsPairedFinal4);
		for (int i = 0; i < headingsPairedFinal4.size(); i++) {
			hdgNx = "";
			aryLenNx = 0;
//		System.out.println("15....hdg=="+headingsPairedFinal4.get(i)[2]);
			if (i + 1 < headingsPairedFinal4.size()
					&& nlp.getAllMatchedGroups(headingsPairedFinal4.get(i + 1)[2], ptrn).size() > 0) {
				hdgNx = nlp.getAllMatchedGroups(headingsPairedFinal4.get(i + 1)[2], ptrn).get(0)
						.replaceAll("(Qx|XQ|sec|hdg|mHd|def|hdg|sub|exh|toc)=?", "").replaceAll("[ ]+", " ").trim();
				aryLenNx = headingsPairedFinal4.get(i + 1).length;
//				System.out.println("1.aryLenNx="+aryLenNx+" hdgNx=="+hdgNx);
			}

			if (i + 1 < headingsPairedFinal4.size()
					&& nlp.getAllMatchedGroups(headingsPairedFinal4.get(i + 1)[2], ptrn).size() == 0) {
				hdgNx = headingsPairedFinal4.get(i + 1)[2].replaceAll("(Qx|XQ|sec|hdg|mHd|def|hdg|sub|exh|toc)=?", "")
						.replaceAll("[ ]+", " ").trim();
				aryLenNx = headingsPairedFinal4.get(i + 1).length;
//				System.out.println("aryLenNx="+aryLenNx+" hdgNx=="+hdgNx);
			}

			aryLen = headingsPairedFinal4.get(i).length;
			hdg = headingsPairedFinal4.get(i)[2];
			if (nlp.getAllMatchedGroups(headingsPairedFinal4.get(i)[2], ptrn).size() > 0) {
				hdg = nlp.getAllMatchedGroups(headingsPairedFinal4.get(i)[2], ptrn).get(0)
						.replaceAll("(Qx|XQ|sec|hdg|mHd|def|hdg|sub|exh|toc)=?", "").replaceAll("[ ]+", " ").trim();
			}

//			System.out.println("aryLen="+aryLen+" hdg revised=="+hdg);
			if (i + 1 < headingsPairedFinal4.size() && aryLen == 3 && aryLenNx == 3 && hdg.equals(hdgNx)) {
//				System.out.println("are same. p4.hdg=" + hdg + " ary==" + Arrays.toString(headingsPairedFinal4.get(i)));
//				System.out.println("are.samep4.hdgNx=" + hdgNx + " aryNx==" + Arrays.toString(headingsPairedFinal4.get(i + 1)));
				String[] ary = { headingsPairedFinal4.get(i)[0], headingsPairedFinal4.get(i + 1)[1],
						headingsPairedFinal4.get(i)[2] };
				headingsPairedFinal5.add(ary);
//				System.out.println("1.added ar=="+Arrays.toString( ary));
				i++;
				continue;
			}

//			System.out.println("2.added ar=="+Arrays.toString( headingsPairedFinal4.get(i)));

			headingsPairedFinal5.add(headingsPairedFinal4.get(i));

		}

		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter(new File("c:/temp/15. Hdgs Final.txt"));

			for (int i = 0; i < headingsPairedFinal5.size(); i++) {

				sIdx = Integer.parseInt(headingsPairedFinal5.get(i)[0]);
				eIdx = Integer.parseInt(headingsPairedFinal5.get(i)[1]);
				if (i > 0) {
					eIdxP = Integer.parseInt(headingsPairedFinal5.get(i - 1)[1]);
				}
				if (sIdx - eIdxP > 50) {
					if (eIdx - sIdx > 50) {
						pw.append("15..>50 hdg.gap ary.len=" + headingsPairedFinal5.get(i).length + " ary="
								+ Arrays.toString(headingsPairedFinal5.get(i)) + "\r\n");
					} else {
						pw.append("15..<50 hdg.gap ary.len=" + headingsPairedFinal5.get(i).length + " ary="
								+ Arrays.toString(headingsPairedFinal5.get(i)) + "\r\n");
					}

				} else {
					if (eIdx - sIdx > 50) {
						pw.append("15..>50 hdg. ary.len=" + headingsPairedFinal5.get(i).length + " ary="
								+ Arrays.toString(headingsPairedFinal5.get(i)) + "\r\n");
					} else {
						pw.append("15..<50 hdg. ary.len=" + headingsPairedFinal5.get(i).length + " ary="
								+ Arrays.toString(headingsPairedFinal5.get(i)) + "\r\n");
					}

				}
//			if(nlp.getAllMatchedGroups(headingsPairedFinal5.get(i)[2], patternExcludeMeFromHeadings).size()>0) {
//				System.out.println("exclude???"+headingsPairedFinal5.get(i)[2]);
//			}

			}
		}
		pw.close();

		sIdx = 0;
		eIdx = 0;
		System.out.println("sbHdgs...start..");
//		pw = new PrintWriter("c:/temp/11a. bef getSolrMap.txt");
//		pw.append(text);
//		pw.close();
		System.out.println("getting solrMap");
//		PrintWriter pw = new PrintWriter(new File("c:/temp/text at headingsPaired.txt"));
//		pw.append(text);
//		pw.close();

		String tmpStr = "";
		List<String[]> headingsPairedFinal6 = new ArrayList<String[]>();
		int i0 = 0, i1 = 0;
		for (int i = 0; i < headingsPairedFinal5.size(); i++) {
			tmpStr = headingsPairedFinal5.get(i)[2].replaceAll("(?ism)(sec|hdg|mHd|sub|exh|toc)=|Qx|XQ", "").trim();
			i0 = Integer.parseInt(headingsPairedFinal5.get(i)[0]);
			i1 = Integer.parseInt(headingsPairedFinal5.get(i)[1]);
			hdgTmp = headingsPairedFinal5.get(i)[2].replaceAll("(?ism)(sec|hdg|mHd|sub|exh|toc|def)=", "").trim();
			if ((i1 - i0 - hdgTmp.length() < 75 && i1 < 9000 || i1 - i0 - hdgTmp.length() < 0)
					&& !headingsPairedFinal5.get(i)[2].contains("def="))
				continue;
			if (nlp.getAllMatchedGroups(tmpStr,
					Pattern.compile("(?sm)" + "^(Section |SECTION )|^(\\(?[a-zA-Z\\d]{1,8}\\.?\\)"
							+ "|[a-zA-Z\\d]{1,7}\\.|\\d[\\d\\.]+)"))
					.size() > 0
					|| nlp.getAllMatchedGroups(tmpStr, Pattern.compile("(?sm)(^def=|Definition|DEFINITION)")).size() > 0

			) {
				headingsPairedFinal6.add(headingsPairedFinal5.get(i));// keep!!
			}

		}
		if (GetContracts.turnOnPrinterWriter) {// keep 6!
			pw = new PrintWriter(new File("c:/temp/16. Hdgs Final.txt"));
			for (int i = 0; i < headingsPairedFinal6.size(); i++) {
				pw.append("16=" + Arrays.toString(headingsPairedFinal6.get(i)) + "\r\n");
			}
			pw.close();
		}

		TreeMap<String, List<String[]>> mapSolr = getSolrMap(headingsPairedFinal6, text);
		System.out.println("getSolrMap seconds " + (System.currentTimeMillis() - startTime) / 1000);
		startTime = System.currentTimeMillis();

		keyStr = "";
		if (GetContracts.turnOnPrinterWriter) {
			pw = new PrintWriter("c:/temp/17. solrMap.txt");
			List<String[]> listSolr = new ArrayList<String[]>();
			for (Map.Entry<String, List<String[]>> entry : mapSolr.entrySet()) {
				keyStr = entry.getKey();
				listSolr = entry.getValue();
				pw.append("17. solrMap key=" + keyStr + " ");
				for (int i = 0; i < listSolr.size(); i++) {
					String[] ary = listSolr.get(i);
					for (int c = 0; c < ary.length; c++) {
						pw.append("ary=[" + c + "] " + ary[c].trim() + "\r\n");
					}
					pw.append("\r\n");
				}
			}
			pw.close();
		}

//		NLP.printMapStringListOfStringAry("mapSolr=", mapSolr);

//		System.out.println("quotedTerms at writing solr file. quotedTerms==");
		startTime = System.currentTimeMillis();
		String allHdgs = "", allDefs = "", allExhs = "", allQuotedTerms_m = "", allQuotedTerms = "";

//		if (sbHdgs.toString().replaceAll("[\\[\\]\"]+", "").length() > 4) {
//			allHdgs = sbHdgs.toString();
//			allHdgs = "\"allHdgs\":[\"" + allHdgs.replaceAll("xxPD", "\\.").replaceAll("xxOB|CBxx|xxOP|CPxx", "")
//					.replaceAll("[\\[\\]]", "")
//					.replaceAll(
//							"(?<=\"|^)(Section ?|SECTION ?)?( ?[\\d]{1,3}\\.[\\d]{0,3}\\.? ?| ?\\(?[a-zA-Z]{1,6}\\) )+",
//							" ")
//					.replaceAll("\" (?=[A-Za-z\\d]{1})", "\"").replaceAll(",\"\",", ",") + "],\r\n";
//			allHdgs = allHdgs.replaceAll("\"\\. ", "\"") + "\"allHdgsCnt\":" + (sbHdgs.toString().split("\",").length)
//					+ ",\r\n";
////			System.out.println("allHdgs="+allHdgs);			
//		}
//
//		if (sbDefs.toString().replaceAll("[\\[\\]\"]+", "").length() > 4) {
//			allDefs = "\"allDefs\":[\""
//					+ sbDefs.toString().replaceAll("xxPD", "\\.").replaceAll("xxOB|CBxx|xxOP|CPxx", "")
//							.replaceAll("[\\[\\]]", "")
//					+ "],\r\n" + "\"allDefsCnt\":" + (sbDefs.toString().split("\",").length) + ",\r\n";
//		}
//
//		if (sbExhibits.toString().replaceAll("[\\[\\]\"]+", "").length() > 4) {
//			allExhs = "\"allExhs\":["
//					+ sbExhibits.toString().replaceAll("xxPD", "\\.").replaceAll("xxOB|CBxx|xxOP|CPxx", "")
//							.replaceAll("[\\[\\]]", "").replaceAll(",\"\"", "")
//					+ "],\r\n" + "\"allExhsCnt\":" + (sbExhibits.toString().split("\",").length) + ",\r\n";
//		}

//		GetContracts.quotedTerms =
		allQuotedTerms_m = GetContracts.getQuotedTerms(text);
		if (allQuotedTerms_m.replaceAll("xxOB|CBxx|xxPd", "").replaceAll("[\\[\\]\"]+", "").length() > 4) {
//			System.out.println("quotedTerms.GetContracts.quotedTermsTxt="+GetContracts.quotedTermsTxt);

			allQuotedTerms_m = "\"allQuotedTerms_m\":["
					+ allQuotedTerms_m.replaceAll("xxPD", "\\.").replaceAll("xxOB|CBxx|xxOP|CPxx|Qx|XQ", "")
					+ "\"],\r\n" + "\"allQuotedTermsCnt_m\":" + (allQuotedTerms_m.split("\",").length) + ",\r\n";
			allQuotedTerms = "\"allQuotedTerms\":[\""
					+ GetContracts.quotedTermsTxt.replaceAll("xxPD", "\\.").replaceAll("xxOB|CBxx|xxOP|CPxx|Qx|XQ", "")
							.replaceAll("\"(\\d\\.[\\d]{0,3}|\\(?[a-zA-Z]{1,6}\\)) ", "").replaceAll("\"", "")
					+ "\"],\r\n" + "\"allQuotedTermsCnt\":" + GetContracts.quotedTermsTxt.split(" ").length + ",\r\n";
			allQuotedTerms = allQuotedTerms.replaceAll("_", " ");
		}

		if (allQuotedTerms_m.length() < 5) {
			allQuotedTerms_m = "";
		}

		metaHdgs =
//				allHdgs + allDefs + allExhs + 
				allQuotedTerms_m + allQuotedTerms;
		metaHdgs.replaceAll("(\"all(Hdgs|Defs|Exh|QuotedTerms)\":\\[\\],)+", "");
//		sbExhibits = new StringBuilder();
//		sbDefs = new StringBuilder();
//		sbHdgs = new StringBuilder();
		allQuotedTerms_m = "";
		allQuotedTerms = "";
//		allHdgs = "";
//		allDefs = "";
//		allExhs = "";
		GetContracts.quotedTermsTxt = "";
		GetContracts.quotedTerms = "";

//		metadata.add(metaHdgs);
//		System.out.println("created quotedTerms/allHdgs seconds " + (System.currentTimeMillis() - startTime) / 1000);
//		startTime = System.currentTimeMillis();

		String solrText = createSolrFile(mapSolr, metadata);

//		System.out.println("createSolrFile seconds " + (System.currentTimeMillis() - startTime) / 1000);
		startTime = System.currentTimeMillis();

		// cleanup for when parsing from metadata:
		solrText = solrText.replaceAll(",\"\r\n", ",\"").replaceAll("\"id\":\"\r\n", "\"id\":\"")
				.replaceAll("\\[\", \"", "[\"").replaceAll("xOrx", "|").replaceAll("CBxx?", "")
				.replaceAll("(?sm),\",\"hashTxtId", ",\"\r\n,\"hashTxtId");

//		System.out.println("writing solr file to drive==" + fileSolr);
//		Utils.writeTextToFile(fileSolr, solrText);// saved the json file for solr ingestion.
		// pos2solr here.

//		System.out.println("json is done");
		
		return solrText;

	}

	public static TreeMap<Integer, String[]> final_fix_of_eIdx_of_headings(List<String[]> headingsPaired, String text)
			throws IOException, SQLException {

		NLP nlp = new NLP();
//		List<String[]> headingsPairedFinal = new ArrayList<String[]>();

		TreeMap<Integer, String[]> mapSec = new TreeMap<Integer, String[]>();
		TreeMap<Integer, String[]> mapDef = new TreeMap<Integer, String[]>();
		TreeMap<Integer, String[]> mapHdg = new TreeMap<Integer, String[]>();
		TreeMap<Integer, String[]> mapmHd = new TreeMap<Integer, String[]>();
		TreeMap<Integer, String[]> mapSub = new TreeMap<Integer, String[]>();
		String priorSec = null, priorDef = null, priorHdg = null, priorMhd = null, priorSub = null;
		String heading = "", subheading = "";
		int cS = 0, cD = 0, cH = 0, cM = 0, cSb = 0;
		for (int i = 0; i < headingsPaired.size(); i++) {
			heading = headingsPaired.get(i)[2];
			if (headingsPaired.get(i).length > 3)
				subheading = headingsPaired.get(i)[3];
			else
				subheading = "";

			// if(subheading.length()>0)
			// System.out.println("subheading...="+subheading);
			// System.out.println("heading=="+Arrays.toString(headingsPaired.get(i)));
			if (!heading.equals(priorDef) && nlp.getAllMatchedGroups(heading, Pattern.compile("def=")).size() > 0) {
				mapDef.put(cD, headingsPaired.get(i));
				priorDef = heading;
				cD++;
//				System.out.println("mapDef is it consec...[2]=" + headingsPaired.get(i)[2]);
			}

			if (!heading.equals(priorSec) && nlp.getAllMatchedGroups(heading, Pattern.compile("sec=")).size() > 0) {
				mapSec.put(cS, headingsPaired.get(i));
				priorSec = heading;
				cS++;
			}

			if (!heading.equals(priorHdg) && nlp.getAllMatchedGroups(heading, Pattern.compile("hdg=")).size() > 0) {
				mapHdg.put(cH, headingsPaired.get(i));
				priorHdg = heading;
				cH++;
			}

			if (!heading.equals(priorMhd) && nlp.getAllMatchedGroups(heading, Pattern.compile("mHd=")).size() > 0) {
				mapmHd.put(cM, headingsPaired.get(i));
				priorMhd = heading;
				cM++;
			}

			if (!heading.equals(priorSub) && nlp.getAllMatchedGroups(heading, Pattern.compile("sub=")).size() > 0) {
				mapSub.put(cSb, headingsPaired.get(i));
				priorSub = heading;
				cSb++;
			}

			if (!subheading.equals(priorDef)
					&& nlp.getAllMatchedGroups(subheading, Pattern.compile("def=")).size() > 0) {
				mapDef.put(cD, headingsPaired.get(i));
				priorDef = subheading;
				cD++;
//				System.out.println("mapDef is it consec...[3]=" + headingsPaired.get(i)[3]);
			}

			if (!subheading.equals(priorSec)
					&& nlp.getAllMatchedGroups(subheading, Pattern.compile("sec=")).size() > 0) {
				mapSec.put(cS, headingsPaired.get(i));
				priorSec = subheading;
				cS++;
			}

			if (!subheading.equals(priorHdg)
					&& nlp.getAllMatchedGroups(subheading, Pattern.compile("hdg=")).size() > 0) {
				mapHdg.put(cH, headingsPaired.get(i));
				priorHdg = subheading;
				cH++;
			}

			if (!subheading.equals(priorMhd)
					&& nlp.getAllMatchedGroups(subheading, Pattern.compile("mHd=")).size() > 0) {
				mapmHd.put(cM, headingsPaired.get(i));
				priorMhd = subheading;
				cM++;
			}

			if (!subheading.equals(priorSub)
					&& nlp.getAllMatchedGroups(subheading, Pattern.compile("sub=")).size() > 0) {
				mapSub.put(cSb, headingsPaired.get(i));
				priorSub = subheading;
				cSb++;
			}

			// String[] ary = headingsPaired.get(i);
//			for (int n = 0; n < ary.length; n++) {
//					System.out.println("i==" + i + " field #=" + n + " val=" + ary[n]);
			// rules:
			// if # has 2 or more decimals then get # value at last dec
			// if next para # is dif format, see if last digit is 1.
			// if val=def and not followed by def, then get idx of end of para.
			// if value is less than 1 but not .1 then get idx of end of para.
//			}
		}

//		NLP.printMapIntStringAry("mapSec=", mapSec);
//		NLP.printMapIntStringAry("mapmHd=", mapmHd);
//		NLP.printMapIntStringAry("mapHdg=", mapHdg);
//		NLP.printMapIntStringAry("mapDef=", mapDef);
//		NLP.printMapIntStringAry("mapSub=", mapSub);

//		System.out.println("fetching consec or not for mapSec");
		mapSec = mark_what_is_not_consecutive(mapSec, text);
//		System.out.println("fetching consec or not for mapHdg");
		mapHdg = mark_what_is_not_consecutive(mapHdg, text);
//		System.out.println("fetching consec or not for mapmHd");
		mapmHd = mark_what_is_not_consecutive(mapmHd, text);
//		System.out.println("fetching consec or not for mapSub");
		mapSub = mark_what_is_not_consecutive(mapSub, text);

//		NLP.printMapIntStringAry("1.mapSec=", mapSec);
//		NLP.printMapIntStringAry("1.mapHdg=", mapHdg);
//		NLP.printMapIntStringAry("1.mapmHd=", mapmHd);
//		NLP.printMapIntStringAry("1.mapDef=", mapDef);
//		NLP.printMapIntStringAry("1.mapSub=", mapSub);

		if (mapSec.size() > 0) {
			mapSec = mark_what_is_consecutive(mapSec, false);
//			System.out.println("mapSec...mark_what_is_consecutive");
		}
		if (mapHdg.size() > 0) {
			mapHdg = mark_what_is_consecutive(mapHdg, false);
//			System.out.println("mapHdg...mark_what_is_consecutive");
		}
		if (mapmHd.size() > 0) {
			mapmHd = mark_what_is_consecutive(mapmHd, false);
//			System.out.println("mapmHd...mark_what_is_consecutive");
		}
		if (mapSub.size() > 0) {
			mapSub = mark_what_is_consecutive(mapSub, false);
//			System.out.println("mapSub...mark_what_is_consecutive");
		}
//		NLP.printMapIntStringAry("mapmHdg", mapmHd);
//		System.out.println("fetching consec or not for mapDef");
//		mark_what_is_not_consecutive(mapDef); XXXX add this back also.
//		NLP.printMapIntStringAry("2.mapSec=", mapSec);
//		NLP.printMapIntStringAry("2.mapmHd=", mapmHd);
//		NLP.printMapIntStringAry("2.mapHdg=", mapHdg);
//		NLP.printMapIntStringAry("2.mapDef=", mapDef);
//		NLP.printMapIntStringAry("2.mapSub=", mapSub);

		TreeMap<Integer, String[]> mapAll = new TreeMap<Integer, String[]>();
		mapAll = addToMap(mapAll, mapSec);// adds to one map and sets key to sIdx
//		NLP.printMapIntStringAry("3.mapALL+sec=", mapAll);
		mapAll = addToMap(mapAll, mapmHd);
//		NLP.printMapIntStringAry("3.mapALL+mHd=", mapAll);
		mapAll = addToMap(mapAll, mapHdg);
//		NLP.printMapIntStringAry("3.mapALL+hdg=", mapAll);
		mapAll = addToMap(mapAll, mapSub);
//		NLP.printMapIntStringAry("3.mapALL+sub=", mapAll);
		mapAll = addToMap(mapAll, mapDef);
//		NLP.printMapIntStringAry("3.mapALL+def=", mapAll);

		// put smallest last.

//		List<String[]> hdgsPairedFinal = new ArrayList<String[]>();
//		for (Map.Entry<Integer, String[]> entry : mapAll.entrySet()) {
////			System.out.println("hdgsPairedFinal key=" + entry.getKey() + " val=" + Arrays.toString(entry.getValue()));
//			hdgsPairedFinal.add(entry.getValue());
//		}
//		// then merge all 5 maps with idx as key then turn map into list.
//		System.out.println("headingsPairedFinal.size==" + headingsPairedFinal.size() + " versus hdgsPairedFinal.size="
//				+ hdgsPairedFinal.size());

//		return hdgsPairedFinal;

		int cnt = 0;
		TreeMap<Integer, String[]> mapAllFinal = new TreeMap<Integer, String[]>();
		System.out.println("mapAll.size=" + mapAll.size());
		for (Map.Entry<Integer, String[]> entry : mapAll.entrySet()) {
			mapAllFinal.put(cnt, entry.getValue());
			cnt++;
		}

		return mapAllFinal;

	}

	public static TreeMap<Integer, String[]> addToMap(TreeMap<Integer, String[]> map, TreeMap<Integer, String[]> map2) {

//		NLP.printMapIntStringAry("", map);

		for (Map.Entry<Integer, String[]> entry : map2.entrySet()) {

//			System.out.println("map2=="+Arrays.toString(entry.getValue()));

			if (map.containsKey(Integer.parseInt(entry.getValue()[0])))

			{
				String[] ary = map.get(Integer.parseInt(entry.getValue()[0]));
//				System.out.println("ary1 key="+Arrays.toString(ary));
				String[] ary2 = { ary[0], ary[1], ary[2], entry.getValue()[1], entry.getValue()[2] };
//				System.out.println("1.put key="+entry.getValue()[0]+" ary1+2=="+Arrays.toString(ary2));
				map.put(Integer.parseInt(entry.getValue()[0]), ary2);
			} else {
//				System.out.println("2.put key="+entry.getValue()[0]+" ary1+2=="+Arrays.toString(entry.getValue()));
				map.put(Integer.parseInt(entry.getValue()[0]), entry.getValue());
			}
		}

		return map;
	}

	public static TreeMap<Integer, String[]> mark_what_is_not_consecutive(TreeMap<Integer, String[]> map, String text)
			throws IOException {

//		System.out.println("mark_what_is_not_consecutive mapMarked.size==" + map.size());
		NLP nlp = new NLP();
		GoLaw gl = new GoLaw();

		TreeMap<Integer, String[]> mapMarked = new TreeMap<Integer, String[]>();
		// find those that are not consecutive or is last hdg and mark as such.

		List<Integer[]> listI_cur = new ArrayList<Integer[]>();
		List<Integer[]> listI_next = new ArrayList<Integer[]>();
		List<String> listCur = new ArrayList<String>();
		List<String> listNext = new ArrayList<String>();

		boolean foundNext = false, foundCurrent = false;
		String pTyp = "", pTypNx = "", paraNmbStr = null, paraNmbNextStr = null, paraNmbStrDec = null,
				paraNmbNextStrDec = null, para = null;
		double paraNmb = 0, paraNmbNext = 0;
		int p = 0, pN = 0, pNdec = 0, pDec = 0, eIdx = 0, sIdx = 0, sIdxNxt = 0, key = 0;

		for (int cnt = 0; cnt < map.size(); cnt++) {
			pTyp = "";
			pTypNx = "";
			eIdx = 0;
			paraNmbStrDec = null;
			paraNmbNextStrDec = null;
//			if (map.get(cnt)[2].substring(0, 4).equals("hdg="))
//				System.out.println("cnt=" + cnt + " ary==xx===" + Arrays.toString(map.get(cnt)));
			foundNext = false;
			foundCurrent = false;
			if (map.containsKey(cnt)) {
				String[] ary = map.get(cnt);
//				if (ary.length > 3) {
//					System.out.println("3.ary len==" + ary.length);
//				}
				listCur = nlp.getAllMatchedGroups(ary[2], patternParaNmb);
//				System.out.println("ary[2]=" + ary[2]);
				if (listCur.size() == 0 && ary[2].contains("def="))
					continue;
				if (listCur.size() > 0) {
//					System.out.println("found cur");
					foundCurrent = true;
					paraNmbStr = listCur.get(0);
//					System.out.println("cur paraNmb=" + listCur.get(0) + " ary=" + Arrays.toString(ary));
					if (paraNmbStr.length() == 0) {
						paraNmbStr = listCur.get(0).replaceAll("[\\.]+", "");
					}

					if (nlp.getAllIndexEndLocations(paraNmbStr, Pattern.compile("\\.")).size() > 1) {
						paraNmbStr = listCur.get(0).replaceAll("[\\.]+", "");
//						System.out.println("paraNmbStr bef repl=" + listCur.get(0) + " and aft repl=" + paraNmbStr);
					}

					if (nlp.getAllIndexEndLocations(paraNmbStr, Pattern.compile("\\.")).size() > 0) {
						paraNmbStrDec = paraNmbStr.replaceAll("[\\.]+", "");
					}

//					System.out.println("1.convertAlphaParaNumbAndType.="+listCur.get(0)+" to..="+Arrays.toString( gl.convertAlphaParaNumbAndType(listCur.get(0).trim(), false, "").get(0)));

//					if (nlp.getAllIndexEndLocations(listCur.get(0), Pattern.compile("[^\\d\\.]")).size() > 0) {
//						System.out.println("1.convertAlphaParaNumbAndType.="+listCur.get(0));

					if (paraNmbStr.replaceAll("[\\d\\.]+", "").trim().length() > 0) {
						listI_cur = gl.convertAlphaParaNumbAndType(listCur.get(0).trim(), false, "");
//						NLP.printListOfIntegerArray("listI_cur", listI_cur);
						if (listI_cur.size() > 0) {
							paraNmbStr = listI_cur.get(0)[0] + "";
							pTyp = listI_cur.get(0)[1] + "";
						} else {
							paraNmbStr = "";
							pTyp = "";
						}
					}

					if (paraNmbStr.length() > 0 && paraNmbStr.replaceAll("[\\d\\.]+", "").trim().length() == 0) {
//						System.out.println("paraNmb from paraNmbStr=" + paraNmbStr);
						paraNmb = Double.parseDouble(paraNmbStr.trim());
					} else {
						paraNmb = 907;
					}
				}
			}
			if (null != paraNmbStr && listCur.size() > 0
					&& listCur.get(0).replaceAll("[\\d\\.]+", "").trim().length() == 0) {
				pTyp = "100";
			}

			if (map.containsKey(cnt + 1)) {
				String[] ary = map.get(cnt + 1);
				sIdxNxt = Integer.parseInt(ary[0]);
//				System.out.println("did I get next para nmb from this ? next ary==" + Arrays.toString(ary));
				listNext = nlp.getAllMatchedGroups(ary[2], patternParaNmb);
				if (listNext.size() == 0 && ary[2].contains("def="))
					continue;

				if (listNext.size() > 0) {
//					System.out.println("found next");
					foundNext = true;
					paraNmbNextStr = listNext.get(0);
//					System.out.println("next paraNmb=" + listNext.get(0) + " nxt ary=" + Arrays.toString(ary));
					if (paraNmbNextStr.length() == 0) {
						paraNmbNextStr = listNext.get(0).replaceAll("[\\.]+", "");
					}

					if (nlp.getAllIndexEndLocations(paraNmbNextStr, Pattern.compile("\\.")).size() > 1) {
						paraNmbNextStr = listNext.get(0).replaceAll("[\\.]+", "");
//						System.out.println(
//								"paraNmbNextStr bef repl=" + listNext.get(0) + " and aft repl=" + paraNmbNextStr);
					}

					if (nlp.getAllIndexEndLocations(paraNmbNextStr, Pattern.compile("\\.")).size() > 0) {
						paraNmbNextStrDec = paraNmbNextStr.replaceAll("[\\.]+", "");
					}

					// System.out.println("nxt paraNmb=" + listNext.get(0) + " ary=" +
					// Arrays.toString(ary));
//					if (nlp.getAllIndexEndLocations(listNext.get(0), Pattern.compile("[^\\d\\.]")).size() > 0) {
//						System.out.println("2.convertAlphaParaNumbAndType.="+listNext.get(0));

					if (paraNmbNextStr.replaceAll("[\\d\\.]+", "").trim().length() > 0) {
//						System.out.println("did this kill paranmbNextStr=" + paraNmbNextStr);
						listI_next = gl.convertAlphaParaNumbAndType(listNext.get(0), false, "");
//						NLP.printListOfIntegerArray("listI_next", listI_next);
						if (listI_next.size() > 0) {
							paraNmbNextStr = listI_next.get(0)[0] + "";
							pTypNx = listI_next.get(0)[1] + "";
						} else {
							paraNmbNextStr = "";
							pTypNx = "";
						}
					}
					if (paraNmbNextStr.length() > 0
							&& paraNmbNextStr.replaceAll("[\\d\\.]+", "").trim().length() == 0) {
						paraNmbNext = Double.parseDouble(paraNmbNextStr.trim());
					} else {
						paraNmbNext = -1113;
					}
				}
			}

			if (null != paraNmbNextStr && listNext.size() > 0
					&& listNext.get(0).replaceAll("[\\d\\.]+", "").trim().length() == 0) {
				pTypNx = "100";
			}

			p = (int) Math.round(paraNmb * 1000) / 10;
			pN = (int) Math.round(paraNmbNext * 1000) / 10;

//			System.out.println("paraNmbStr=" + paraNmbStr + " paraNmbNextStr=" + paraNmbNextStr);
			if (null != paraNmbNextStrDec && null != paraNmbStrDec && paraNmbStrDec.length() > 0
					&& paraNmbNextStrDec.length() > 0 && paraNmbStrDec.replaceAll("[\\d]+", "").trim().length() == 0
					&& paraNmbNextStrDec.replaceAll("[\\d]+", "").trim().length() == 0
					&& paraNmbNextStrDec.length() < 10 && paraNmbStrDec.length() < 10) {
				pDec = Integer.parseInt(paraNmbStrDec.trim());
				pNdec = Integer.parseInt(paraNmbNextStrDec.trim());
			} else {
				pDec = 0;
				pNdec = 0;
			}

			if (foundCurrent && foundNext) {
//				System.out.println("1.eIdx ok??cnt=" + cnt + " val=" + Arrays.toString(map.get(cnt)) + "\r\nfoundCurrent="
//						+ foundCurrent + " foundNext=" + foundNext + " lN=" + listNext.get(0) + " paraNmbNextStr="
//						+ paraNmbNextStr + " listCur=" + listCur.get(0) + " paraNmbStr=" + paraNmbStr + " pTypNx=" + pTypNx
//						+ " pTyp=" + pTyp + " pN=" + pN + " p=" + p);

				// if dif is 1000, 100, 10, 1 or 1/10 then ok else fix eIdx
				if (paraNmbNextStr.length() > 0 && pTyp.trim().equals(pTypNx.trim())
						&& (pN - p == 1 || pN - p == 10 || pN - p == 100 || pN - p == 1000 || pNdec - pDec == 1
								|| pNdec - pDec == 10 || pNdec - pDec == 100)) {
//					System.out.println("eIdx OK. pTyp=" + pTyp + " pTypNx=" + pTypNx + " paraNmbStr=" + listCur.get(0)
//							+ " paraNmbStrNext=" + listNext.get(0) + " p=" + p + " pN=" + pN);
//pickup here .. see 6.7 as it seems to identify gap b/w it and 6.8 but not fix it correctly. print sIdxN, eIdx
					eIdx = Integer.parseInt(map.get(cnt)[1]);
					sIdxNxt = Integer.parseInt(map.get(cnt + 1)[0]);
//					System.out.println("1.put. eIdx OK?==" + eIdx + " sIdxNxt=" + sIdxNxt + " pN-p=" + (pN - p));
					if (sIdxNxt - eIdx > 5) {
//						System.out.println("but eIdx is wrong. eIdx=" + eIdx + " next sIdx=" + sIdxNxt);
						String[] ar = { map.get(cnt)[0], (sIdxNxt - 1) + "", map.get(cnt)[2] };

//						if (map.get(cnt)[2].substring(0, 4).equals("hdg=")) {
//							System.out.println("1..key=" + key + " ary=" + Arrays.toString(ar));
//						}

						mapMarked.put(key, ar);
						key++;
					}

					if (sIdxNxt - eIdx <= 5) {
						mapMarked.put(key, map.get(cnt));

//						if (map.get(cnt)[2].substring(0, 4).equals("hdg=")) {
//							System.out.println("2..key=" + key + " ary=" + Arrays.toString(map.get(cnt)));
//						}

						key++;
					}

				} else {

					para = "";
					eIdx = 0;
					sIdx = Integer.parseInt(map.get(cnt)[0]);
					if (map.size() > cnt + 1
							&& sIdx + map.get(cnt)[2].length() + 30 < Integer.parseInt(map.get(cnt + 1)[0])) {
						para = getParagraphAfterHeading(text, sIdx + map.get(cnt)[2].length() - 4,
								Integer.parseInt(map.get(cnt + 1)[0]));// -4 b/c each has 'hdg=' or 'sec=' etc
						if (para.length() > 0) {
							eIdx = text.substring(sIdx).indexOf(para) + sIdx + para.length();
						}

						/*
						 * start at sIdx so I get the correct instance, add back sIdx and par length
						 * (index of is the sIdx of para)
						 */
						else {
							eIdx = 0;
						}
					}

					if (eIdx == 0) {
						String[] ary = { sIdx + "", map.get(cnt)[1] };
						eIdx = GetContracts.getLastDefinedTermSimple(ary, text);
					}
					if (eIdx == 0) {
						eIdx = Integer.parseInt(map.get(cnt + 1)[0]);
					}
//					System.out.println("1.it is not consec.\r\n"
//					 + "cur ary=" + Arrays.toString(map.get(cnt))
//					 + "\r\nnxt ary="
//					 + Arrays.toString(map.get(cnt + 1)));

//					System.out.println("2.put. chg eIdx=" + (pN - p) + " ary.len==" + ary.length + " eIdx=" + ary[1]
//							+ " new eIdx=" + eIdx);
//					System.out.println("chg eIdx. cur ary=" + Arrays.toString(map.get(cnt)) + " nxt ary="
//							+ Arrays.toString(map.get(cnt + 1)));
//					System.out.println("chg eIdx. p=" + p + " pN=" + pN + " pDec=" + pDec + " pNdec=" + pNdec
//							+ " pNdecStr=" + paraNmbNextStrDec + " pDecStr==" + paraNmbStrDec);

//					System.out.println("chg eIdx, para="+text.substring(Integer.parseInt(map.get(cnt)[0]),eIdx));
					String[] ar = { sIdx + "", eIdx + "", map.get(cnt)[2] };
					mapMarked.put(key, ar);

//					if (map.get(cnt)[2].substring(0, 4).equals("hdg=")) {
//						System.out.println("3..key=" + key + " ary=" + Arrays.toString(ar));
//					}

					key++;
				}
			} else {
//				System.out.println("2.eIdx ok??cnt=" + cnt + " val=" + Arrays.toString(map.get(cnt)) + "\r\nfoundCurrent="
//						+ foundCurrent + " foundNext=" + foundNext 
//						+ " listNext=" + listNext.get(0) 
//						+ " paraNmbNextStr="
//						+ paraNmbNextStr + 
//						" listCur=" + listCur.get(0) + 
//						" paraNmbStr=" + paraNmbStr + " pTypNx=" + pTypNx
//						+ " pTyp=" + pTyp + " pN=" + pN + " p=" + p);

//				System.out.println("3.put. chg eId[pN=" + pN);

				para = "";
				eIdx = 0;
				sIdx = Integer.parseInt(map.get(cnt)[0]);
				if (map.size() > cnt + 1
						&& sIdx + map.get(cnt)[2].length() + 30 < Integer.parseInt(map.get(cnt + 1)[0])) {
//					System.out.println("sidxnext word added to len=" + map.get(cnt)[2]);
//					System.out.println("chg eIdx. cur ary=" + Arrays.toString(map.get(cnt)) + " nxt ary="
//							+ Arrays.toString(map.get(cnt + 1)));
					para = getParagraphAfterHeading(text, sIdx + map.get(cnt)[2].length() - 4,
							Integer.parseInt(map.get(cnt + 1)[0]));// -4 b/c each has 'hdg=' or 'sec=' etc
					if (para.length() > 0) {
//						System.out.println("1.para="+para);
						eIdx = text.substring(sIdx).indexOf(para) + sIdx + para.length();
//						System.out.println("1.para eIdx="+eIdx);
					}

					/*
					 * start at sIdx so I get the correct instance, add back sIdx and par length
					 * (index of is the sIdx of para)
					 */
					else {
						eIdx = 0;
					}
				}

				if (eIdx == 0) {
					String[] ary = { sIdx + "", map.get(cnt)[1] };
					eIdx = GetContracts.getLastDefinedTermSimple(ary, text);
//					System.out.println("2.eIdx getlast def term="+eIdx);
				}
				if (eIdx == 0) {
					eIdx = Integer.parseInt(map.get(cnt + 1)[0]);
//					 System.out.println("3.eIdx="+eIdx);
				}
//				System.out.println("2.it is not consec. new eIdx?="+eIdx+"\r\n"
//						 + "cur ary=" + Arrays.toString(map.get(cnt))
//						 + "\r\nnxt ary="
//						 + Arrays.toString(map.get(cnt + 1))+" eIdx recorded="+eIdx);

//				System.out.println("2.put. chg eIdx=" + (pN - p) + " ary.len==" + ary.length + " eIdx=" + ary[1]
//						+ " new eIdx=" + eIdx);
//				System.out.println("chg eIdx. cur ary=" + Arrays.toString(map.get(cnt)) + " nxt ary="
//						+ Arrays.toString(map.get(cnt + 1)));
//				System.out.println("chg eIdx. p=" + p + " pN=" + pN + " pDec=" + pDec + " pNdec=" + pNdec
//						+ " pNdecStr=" + paraNmbNextStrDec + " pDecStr==" + paraNmbStrDec);

//				System.out.println("chg eIdx, para="+text.substring(Integer.parseInt(map.get(cnt)[0]),eIdx));
				String[] ar = { sIdx + "", eIdx + "", map.get(cnt)[2] };
				mapMarked.put(key, ar);

//				if (map.get(cnt)[2].substring(0, 4).equals("hdg=")) {
//					System.out.println("4..key=" + key + " ary==" + Arrays.toString(ar));
//				}

				key++;
			}
		}

//		System.out.println("finished - mapMarked.size=" + mapMarked.size());
//		NLP.printMapIntStringAry("mapMarked", mapMarked);
		return mapMarked;
	}

	public static TreeMap<Integer, String[]> mark_what_is_consecutive(TreeMap<Integer, String[]> map,
			boolean checkHeadingType) throws IOException {
		// checks when numbering is consecutive but sIdx less prior eIdx has a gap and
		// then extends prior eIdx to sIdx-1.

		System.out.println("mark_what_is_consecutive");

		NLP nlp = new NLP();
		GoLaw gl = new GoLaw();
		TreeMap<Integer, String[]> mapChanged = new TreeMap<Integer, String[]>();
		int sIdxNext = 0, eIdx = 0, sIdx = 0;
		String tmp = "";
		List<Integer[]> listI_cur = new ArrayList<Integer[]>();
		List<Integer[]> listI_next = new ArrayList<Integer[]>();
		List<String> listCur = new ArrayList<String>();
		List<String> listNext = new ArrayList<String>();

//			int alphNmb = -1, alphNmbNext = -5, alphNmbPrev = -10;
		listCur = new ArrayList<String>();

		boolean foundNext = false, foundCurrent = false, changed_eIdx = false, mapped = false;
		String pTyp = "", pTypNxt = "", paraNmbStr = "", paraNmbNextStr = "", paraNmbStrDec = "",
				paraNmbNextStrDec = null, hdg = "", hdgNxt = "", heading;
		double paraNmb = 0, paraNmbNext = 0;
		int p = 0, pN = 0, pNdec = 0, pDec = 0;
//		System.out.println("map.size======" + map.size());
		for (int cnt = 0; cnt < map.size(); cnt++) {
			changed_eIdx = false;
//			System.out.println("key=" + cnt + " map ary=" + Arrays.toString(map.get(cnt)));
			if (map.get(cnt) == null || !map.containsKey(cnt)) {
//				System.out.println("1.continue...");
				continue;
			}
			mapped = false;
			eIdx = Integer.parseInt(map.get(cnt)[1]);
			sIdx = Integer.parseInt(map.get(cnt)[0]);
			heading = map.get(cnt)[2];

			if (map.get(cnt + 1) != null && map.containsKey(cnt + 1) && cnt + 1 < map.size()) {
//				System.out.println("map.get cnt+1=" + Arrays.toString(map.get((cnt + 1))));
				sIdxNext = Integer.parseInt(map.get(cnt + 1)[0]);
			}

			paraNmbStrDec = null;
			paraNmbNextStrDec = null;
//			System.out.println("cnt=" + cnt + " ary=====" + Arrays.toString(map.get(cnt)));
			foundNext = false;
			foundCurrent = false;
			if (map.containsKey(cnt)) {
				String[] ary = map.get(cnt);
//				if (ary.length > 3) {
//					System.out.println("3.ary len==" + ary.length);
//				}
				listCur = nlp.getAllMatchedGroups(ary[2], patternParaNmb);
				if (listCur.size() == 0 && ary[2].contains("def=") && !checkHeadingType) {
//					System.out.println("2.continue...");
					continue;
				}
				if (listCur.size() > 0) {
//						System.out.println("found cur");
					foundCurrent = true;
					paraNmbStr = listCur.get(0);
//					System.out.println("cur paraNmb=" + listCur.get(0) + " ary=" + Arrays.toString(ary));
					if (nlp.getAllIndexEndLocations(paraNmbStr, Pattern.compile("\\.")).size() > 1) {
						paraNmbStr = listCur.get(0).replaceAll("[\\.]+", "");
//						System.out.println("paraNmbStr aft repl \\.=" + paraNmbStr);
					}

					if (nlp.getAllIndexEndLocations(paraNmbStr, Pattern.compile("\\.")).size() > 0) {
						paraNmbStrDec = paraNmbStr.substring(paraNmbStr.lastIndexOf(".") + 1);
					}
//					if (nlp.getAllIndexEndLocations(listCur.get(0), Pattern.compile("[^\\d\\.]")).size() > 0) {
//						System.out.println("3.convertAlphaParaNumbAndType.="+listCur.get(0));
					if (paraNmbStr.replaceAll("[\\d\\. ]+", "").length() > 0) {
						listI_cur = gl.convertAlphaParaNumbAndType(paraNmbStr.trim(), false, "");
//							NLP.printListOfIntegerArray("listI_cur", listI_cur);
						if (listI_cur.size() > 0) {
							paraNmbStr = listI_cur.get(0)[0] + "";
							pTyp = listI_cur.get(0)[1] + "";
						} else {
							paraNmbStr = "";
						}
					} else {
						pTyp = "10";
					}
					if (paraNmbStr.length() > 0 && paraNmbStr.replaceAll("[\\d\\.]+", "").trim().length() == 0) {
						paraNmb = Double.parseDouble(paraNmbStr.trim());
					} else {
						paraNmb = -913;
						pTyp = "";
					}
				}
			}

			if (map.containsKey(cnt + 1)) {
				String[] ary = map.get(cnt + 1);
				listNext = nlp.getAllMatchedGroups(ary[2], patternParaNmb);
				if (listNext.size() == 0 && ary[2].contains("def=") && !checkHeadingType) {
//					System.out.println("1.continue...");
					continue;

				}

				if (listNext.size() > 0) {
//						System.out.println("found next");
					foundNext = true;
					paraNmbNextStr = listNext.get(0);
//					System.out.println("next paraNmb=" + listNext.get(0) + " nxt ary=" + Arrays.toString(ary));
					if (nlp.getAllIndexEndLocations(paraNmbNextStr, Pattern.compile("\\.")).size() > 1) {
						paraNmbNextStr = listNext.get(0).replaceAll("[\\.]+", "");
					}
					if (nlp.getAllIndexEndLocations(paraNmbNextStr, Pattern.compile("\\.")).size() > 0) {
						paraNmbNextStrDec = paraNmbNextStr.substring(paraNmbNextStr.lastIndexOf(".") + 1);
					}
					// System.out.println("nxt paraNmb=" + listNext.get(0) + " ary=" +
					// Arrays.toString(ary));
//					if (nlp.getAllIndexEndLocations(listNext.get(0), Pattern.compile("[^\\d\\.]")).size() > 0) {
//						System.out.println("4.convertAlphaParaNumbAndType.="+listNext.get(0));
					listI_next = gl.convertAlphaParaNumbAndType(listNext.get(0), false, "");
//							NLP.printListOfIntegerArray("listI_next", listI_next);

					if (paraNmbNextStr.replaceAll("[\\d\\. ]+", "").length() > 0) {
						if (listI_next.size() > 0) {
							paraNmbNextStr = listI_next.get(0)[0] + "";
							pTypNxt = listI_next.get(0)[1] + "";
						} else {
							paraNmbNextStr = "";
						}
					} else {
						pTypNxt = "10";
					}
					if (paraNmbNextStr.length() > 0 && paraNmbNextStr.replaceAll("[\\d\\.]+", "").trim().length() == 0)
						paraNmbNext = Double.parseDouble(paraNmbNextStr.trim());
					else {
						paraNmbNext = -1113;
						pTypNxt = "";
					}
				}
			}

			p = (int) Math.round(paraNmb * 1000) / 10;
			pN = (int) Math.round(paraNmbNext * 1000) / 10;

			if (null != paraNmbNextStrDec && null != paraNmbStrDec && paraNmbStrDec.length() > 0
					&& paraNmbNextStrDec.length() > 0 && paraNmbStrDec.replaceAll("[\\d]+", "").trim().length() == 0
					&& paraNmbNextStrDec.replaceAll("[\\d]+", "").trim().length() == 0) {
				pDec = Integer.parseInt(paraNmbStrDec.trim());
				pNdec = Integer.parseInt(paraNmbNextStrDec.trim());
			} else {
				pDec = 0;
				pNdec = 0;
			}

//			if (listNext.size() > 0 && listCur.size() > 0) {
//				System.out.println("both...foundCurrent==" + foundCurrent + " foundNext=" + foundNext + " pTyp=" + pTyp
//						+ " pTypNxt=" + pTypNxt + " hdg=" + hdg + " hdgNxt=" + hdgNxt + " pN=" + pN + " p=" + p
//						+ " paraNmbStr=" + paraNmbStr + " paraNmbNextStr=" + paraNmbNextStr + " origParaNmbStr="
//						+ listCur.get(0) + " origParaNmbNextStr=" + listNext.get(0));
//			}
//
//			if (listCur.size() > 0 && listNext.size() == 0) {
//				System.out.println("cur only...foundCurrent==" + foundCurrent + " foundNext=" + foundNext + " pTyp="
//						+ pTyp + " pTypNxt=" + pTypNxt + " hdg=" + hdg + " hdgNxt=" + hdgNxt + " pN=" + pN + " p=" + p
//						+ " paraNmbStr=" + paraNmbStr + " paraNmbNextStr=" + paraNmbNextStr + " origParaNmbStr="
//						+ listCur.get(0));
//			}
//			if (listCur.size() == 0 && listNext.size() == 0) {
//				System.out.println("none...foundCurrent==" + foundCurrent + " foundNext=" + foundNext + " pTyp=" + pTyp
//						+ " pTypNxt=" + pTypNxt + " hdg=" + hdg + " hdgNxt=" + hdgNxt + " pN=" + pN + " p=" + p
//						+ " paraNmbStr=" + paraNmbStr + " paraNmbNextStr=" + paraNmbNextStr);
//			}
//			System.out.println(
//					"WHAT IS CONSEC pTyp=" + pTyp + " pTypNxt=" + pTypNxt + " hdg=" + hdg + " hdgNxt=" + hdgNxt + " pN="
//							+ pN + " p=" + p + " paraNmbStr=" + paraNmbStr + " paraNmbNextStr=" + paraNmbNextStr
//					+ " origParaNmbStr=" + listCur.get(0) 
//					+ " origParaNmbNextStr="
//					+ listNext.get(0)
//							+ " eIdx=" + eIdx + " sIdxNext=" + sIdxNext);

			if (foundCurrent && foundNext) {
				mapped = false;
				// if dif is 1000, 100, 10, 1 or 1/10 then ok else fix eIdx
				if (paraNmbNextStr.length() > 0 && pTyp.trim().equals(pTypNxt.trim())
						&& (pN - p == 1 || (pN - p == 10 && p > 10 - 1) || (pN - p == 100 && p > 100 - 1)
								|| (pN - p == 1000 && p > 1000 - 1) || pNdec - pDec == 1
								|| (pNdec - pDec == 10 && pDec > 10 - 1) || (pNdec - pDec == 100 && pDec > 100 - 1)
								|| (pNdec - pDec == 1000 && pDec > 1000 - 1))) {
//					System.out.println("it is consecutive!!!. hdg=" + map.get(cnt)[2] + "sIdx=" + map.get(cnt)[0]
//							+ " eIdx=" + map.get(cnt)[1] + " hdgNext=" + map.get(cnt + 1)[2] + " sIdxNext="
//							+ map.get(cnt + 1)[0] + " eIdxNext=" + map.get(cnt + 1)[1]);
					if (cnt > 0) {

						hdg = heading.substring(0, 4);
						hdgNxt = map.get(cnt + 1)[2].substring(0, 4);

						if (hdgNxt.equals("sec=")) {
							heading = heading.replaceAll(hdg, hdgNxt);// keeps SEC marks.
							hdg = hdgNxt;
						}

						if (!hdg.equals(hdgNxt) && !hdg.equals("def=") && !hdgNxt.equals("def=")) {
//							System.out.println("it is consecutive but hdgs dif. cnt=" + cnt + " cur hdg=" + hdg
//									+ " hdgNxt=" + hdgNxt + " sIdxNext=" + map.get(cnt + 1)[0] + " eIdxNext="
//									+ map.get(cnt + 1)[1]);
							String[] ary = { map.get(cnt + 1)[0], map.get(cnt + 1)[1] + "", map.get(cnt + 1)[2]
									.replaceAll(hdgNxt, hdg).replaceAll("(sec|hdg|mHd|sub)(=)", "XxX$1$2") };
//							System.out.println("key=" + (cnt + 1) + " replacing this==" + map.get(cnt + 1)[2]
//									+ "\r\nwith this==" + Arrays.toString(ary));
//							System.out.println("1..put replaced with this cnt=" + cnt + " ary=" + Arrays.toString(ary));
							mapChanged.put(cnt + 1, ary);

						}

					}
					if (sIdxNext - eIdx > 10) {
//						System.out.println("is consec, but sIdxNext-eIdx=" + (sIdxNext - eIdx) + " cur="
//								+ Arrays.toString(map.get(cnt)) + " next=" + Arrays.toString(map.get(cnt + 1)));
						String[] ary = { sIdx + "", (sIdxNext - 1) + "",
								heading.replaceAll("(sec|hdg|mHd|sub)(=)", "XxX$1$2") };
						changed_eIdx = true;
						mapChanged.put(cnt, ary);
						mapped = true;
//						System.out.println("2..put map2 map.get cnt=" + cnt + " ary=" + Arrays.toString(ary));
					}

					else {
						String[] ary = { map.get(cnt)[0], map.get(cnt)[1],
								map.get(cnt)[2].replaceAll("(sec|hdg|mHd|sub)(=)", "XxX$1$2") };
						mapChanged.put(cnt, ary);
						mapped = true;
					}
				}

				else if (!mapped) {// did not confirm consec
					mapChanged.put(cnt, map.get(cnt));
//					System.out.println("3..put map2 map.get cnt=" + cnt + " ary=" + Arrays.toString(map.get(cnt)));
					mapped = true;
				}
			}

			if (!changed_eIdx && !mapped) {
//				System.out.println("4..put map2 map.get cnt=" + cnt + " ary=" + Arrays.toString(map.get(cnt)));
				mapChanged.put(cnt, map.get(cnt));
				continue;
			}

		}

//		NLP.printMapIntStringAry("mapChanged====", mapChanged);

//		System.out.println("end fixed");
		return mapChanged;

	}

	public List<Pattern> populateGlobalPatternsVariables() {
		GetContracts gK = new GetContracts();

		Pattern patternToExclude = Pattern.compile(gK.patternArticlesShares.pattern() + "|"
				+ gK.patternMiscDocuments.pattern() + "|" + gK.patternEmploymentContract.pattern());

		Pattern patternsToInclude = Pattern.compile(gK.patternContractsFinancial.pattern() + "|"
				+ gK.patternOpinionInBody.pattern() + gK.patternDescriptionOfSecurities.pattern());

		Pattern patternsFormTypesGenerallyNotToParse = Pattern
				.compile("^(1|1-A|1-A POS|1-A-W|1-E|1-E AD|1-K|1-SA|1-U|1-Z|1-Z-W|10-12B|10-12G|10-C|10-D|10-K|10-K405"
						+ "|10-KSB|10-KT|10-Q|10-QSB|10-QT|10KSB|10KSB40"
						+ "|10KT405|10QSB|10SB12B|10SB12G|11-K|11-KT|12G3-2B|12G32BR|13F-E|13F-HR|13F-NT"
						+ "|13FCONP|144|15-12B|15-12G|15-15D|15F-12B|15F-12G|15F-15D|18-12B|18-K|19B-4E|2-A|2-AF|2-E|20-F|20FR12B"
						+ "|20FR12G|24F-1|24F-2EL|24F-2NT|24F-2TM|25|25-NSE|253G1|253G2|253G3|253G4|3|305B2|34-12H|35-APP|35-CERT"
						+ "|39-304D|39-310B|4|40-17F|40-17F|40-17G|40-17GCS|40-202A|40-203A|40-205E|40-206A|40-24B2|40-33|40-6B|40-6C"
						+ "|40-8B|40-8F-2|40-8F-A|40-8F-B|40-8F-L|40-8F-M|40-8FC|40-APP|40-F|40-OIP|40-RPT|40FR12B|40FR12G|424A|424B1"
						+ "|424B2|424B3|424B4|424B5|424B7|424B8|424H|425|485A24E|485A24F|485APOS|485B24E|485B24F|485BPOS|485BXT|485BXTF"
						+ "|486A24E|486APOS|486B24E|486BPOS|486BXT|487|497|497AD|497H2|497J|497K|497K1|497K2|497K3A|497K3B|5|6-K|6B NTC"
						+ "|6B ORDR|8-A12B|8-A12G|8-B12B|8-B12G|8-K12B|8-K12G3|8-K15D5|8-M|8A12BEF|8A12BT|8F-2 NTC|8F-2 ORDR|9-M"
						+ "|ABS-15G|ABS-EE|ADB|ADN-MTL|ADV|ADV-E|ADV-H-C|ADV-H-T|ADV-NR|ADVCO|ADVW|AFDB|ANNLRPT|APP NTC|APP ORDR"
						+ "|APP WD|APP WDG|ARS|ATS-N|ATS-N-C|ATS-N/CA|ATS-N/MA|ATS-N/UA|AW|AW WD|BDCO|BW-2|BW-3|C|C-AR|C-AR-W|C-TR"
						+ "|C-TR-W|C-U|C-U-W|C-W|CB|CERT|CERTAMX|CERTARCA|CERTBATS|CERTBSE|CERTCBO|CERTCSE|CERTNAS|CERTNYS|CERTPAC"
						+ "|CERTPBS|CFPORTAL|CFPORTAL-W|CORRESP|CT ORDER|D|DEF 14A|DEF 14C|DEF-OC|DEF13E3|DEFA14A|DEFA14C|DEFC14A|DEFC14C"
						+ "|DEFM14A|DEFM14C|DEFN14A|DEFR14A|DEFR14C|DEFS14A|DEFS14C|DEL AM|DFAN14A|DFRN14A|DOS|DOSLTR|DRS|DRSLTR|DSTRBRPT"
						+ "|EBRD|EFFECT|F-1|F-10|F-10EF|F-10POS|F-1MEF|F-2|F-3|F-3ASR|F-3D|F-3DPOS|F-3MEF|F-4|F-4 POS|F-4MEF|F-6|F-6 POS|F-6EF"
						+ "|F-7|F-7 POS|F-8|F-8 POS|F-80|F-80POS|F-9|F-9 POS|F-9EF|F-N|F-X|FOCUSN|FWP|G-405|G-405N|G-FIN|G-FINW|IADB|ID-NEWCIK"
						+ "|IFC|IRANNOTICE|MA|MA-A|MA-I|MA-W|MSD|MSDCO|MSDW|N-1|N-14|N-14 8C|N-14AE|N-14MEF|N-18F1|N-1A|N-1A EL|N-2|N-23C-1"
						+ "|N-23C-2|N-23C3A|N-23C3B|N-23C3C|N-27D-1|N-2MEF|N-3|N-3 EL|N-30B-2|N-30D|N-4|N-4 EL|N-5|N-54A|N-54C|N-6|N-6F|N-8A"
						+ "|N-8B-2|N-8B-4|N-8F|N-8F NTC|N-8F ORDR|N-CEN|N-CR|N-CSR|N-CSRS|N-MFP|N-MFP1|N-MFP2|N-PX|N-Q|N14AE24|N14EL|NO ACT"
						+ "|NPORT-EX|NPORT-P|NRSRO-CE|NRSRO-UPD|NSAR-A|NSAR-AT|NSAR-B|NSAR-BT|NSAR-U|NT 10-D|NT 10-K|NT 10-Q|NT 11-K|NT 15D"
						+ "|NT 20-F|NT N-CEN|NT N-MFP|NT N-MFP|NT N-MFP|NT NPORT-EX|NT NPORT-P|NT-NCEN|NT-NCSR|NT-NSAR|NTFNCEN|NTFNCSR|NTFNSAR"
						+ "|NTN 10D|NTN 10K|NTN 10Q|NTN 11K|NTN 20F|NTN15D2|OIP NTC|OIP ORDR|POS 8C|POS AM|POS AMC|POS AMI|POS EX|POS462B"
						+ "|POS462C|POSASR|PRE 14A|PRE 14C|PRE13E|PREA14A|PREA14C|PREC14A|PREC14C|PREM14A|PREM14C|PREN14A|PRER14A|PRER14C"
						+ "|PRES14A|PRES14C|PRRN14A|PX14A6G|PX14A6N|QRTLYRPT|QUALIF|REG-NR|REGDEX|REVOKED|RW|RW WD|S-1|S-11|S-11MEF|S-1MEF"
						+ "|S-2|S-20|S-2MEF|S-3|S-3ASR|S-3D|S-3DPOS|S-3MEF|S-4|S-4.{1}A|S-4 POS|S-4EF|S-4MEF|S-6|S-6EL|S-8|S-8 POS|S-B"
						+ "|S-BMEF|SB-1|SB-1MEF|SB-2|SB-2MEF|SC 13D|SC 13E1|SC 13E3|SC 13E4|SC 13G|SC 14D1|SC 14D9|SC 14F1|SC 14N|SC TO-C"
						+ "|SC TO-I|SC TO-T|SC13E4F|SC14D1F|SC14D9C|SC14D9F|SD|SDR|SE|SEC ACTION|SEC STAFF ACTION|SEC STAFF LETTER|SF-1"
						+ "|SF-3|SL|SP 15D2|STOP ORDER|T-3|TA-1|TA-2|TA-W|TACO|TH|TTW|U-1|U-12-IA|U-12-IB|U-13-60|U-13E-1|U-33-S|U-3A-2"
						+ "|U-3A3-1|U-5|U-6B-2|U-7D|U-9C-3|U5A|U5B|U5S|UNDER|UPLOAD|WDL-REQ|X-17A-5)(/A)?$");

		List<Pattern> list = new ArrayList<Pattern>();
		list.add(patternToExclude);// must be 1st
		list.add(patternsToInclude);// must be 2nd
		list.add(patternsFormTypesGenerallyNotToParse);// must be 3rd

		return list;
	}

	public List<Integer> populateGlobalIntegerVariables() {

		int smallFileSize = 5000, minStrippedDisclosureDocSize = 50000, largeFileSize = 20000000,
				maxNcFileSize = 100000000, maxKfile = 20000000;

		List<Integer> list = new ArrayList<Integer>();
		list.add(smallFileSize);// 0 must be 1st
		list.add(largeFileSize);// 1 must be 2nd
		list.add(maxNcFileSize);// 2 must be 3rd
		list.add(maxKfile);// 3 must be be 4th
		list.add(minStrippedDisclosureDocSize);// 4 must be be 5th

		return list;
	}

	public List<String> populateDisclosureFormTypesWeWantList() {
		List<String> list = new ArrayList<String>();
//		list.add("497K");// must be exact form name/#.
//		list.add("497");
		list.add("485BPOS");
		list.add("485APOS");
//		list.add("424B5");
//		list.add("424B3");

//		list.add("485bpos");
//		list.add("424b5");
//		list.add("424b3");

		return list;
	}

	public static boolean documentConditions() {

		return false;
	}

	public static boolean fileSizeConditions() {

		return false;
	}

	public static boolean documentText() {
		return false;
	}

	public static boolean contractNameConditions() {

		return false;
	}

	public static void main(String[] args) throws IOException, SQLException, SolrServerException, ParseException {

	}
}
