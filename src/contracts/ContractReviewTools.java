package contracts;

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

import xbrl.ContractNLP;
import xbrl.ContractParser;
import xbrl.NLP;
import xbrl.Utils;

public class ContractReviewTools {

	public static Pattern patternGoLawStopWordsReviewTools = Pattern.compile("(?ism)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))("
			+ "" + "(a)|(about)|(above)|(after)|(again)|(all)|(am)|(an)|(and)|(are)|(as)|(at)|(be)"
			+ "|(because)|(been)|(before)|(being)|(below)|(between)|(both)|(by)|(can)|(did)|(do)|(does)|(doing)|(down)"
			+ "|(during)|(each)|(either)|(few)|(for)|(from)|(further)|(had)|(has)|(have)|(having)|(he)|(her)|(here)|(hers)"
			+ "|(herself)|(him)|(himself)|(his)|(how)|(I)|(in)|(into)|(is)|(it)|(its)|(it\'s)|(itself)|(just)|(me)"
			+ "|(might)|(most)|(must)|(my)|(myself)|(need)|(now)|(of)|(off)|(on)|(only)|(or)|(otherwise)"
			+ "|(our)|(ours)|(ourselves)|(out)|(over)|(own)|(same)|(she)|(she\'s)|(should)|(so)|(some)|(such)|(than)"
			+ "|(that)|(the)|(their)|(theirs)|(them)|(themselves)|(then)|(there)|(these)|(they)|(this)|(those)|(through)"
			+ "|(to)|(too)|(under)|(until)|(up)|(very)|(very)|(was)|(we)|(were)|(what)|(when)|(where)|(whether)|(which)"
			+ "|(while)|(who)"
			+ "|(whom)|(why)|(will)|(with)|(won)|(would)|(you)|(your)|(yours)|(yourself)|(yourselves)" + ""
			+ ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

	public static Pattern patternExcludeMeFromHeadings = Pattern.compile(
			"EX-\\d|Annex |ANNEX |Appendix |APPENDIX |Exhibit |EXHIBIT |Schedule |SCHEDULE |SECTION |Section |Subject to "
					+ "|RECITAL|WHEREAS|(?i)Suite " + "| Vice | VICE " + "|Chief | CHIEF"
//					+ "|[\\d]{4,}"
					+ "|President |PRESIDENT" + "|(?i)INC\\.|(?i)Bancorp|(?i)Pte|(?i)Table of Content"
					+ "|(?i)Signatures?<!\\.|[a-z]{1} (D\\.D\\.S|M\\.D|DR|Dr)\\."
//					+ "|(^| )[A-Za-z]{1}\\.
//					|/
					+ "|\\$" + "");

	public static void didYouDefineItOrDefineItButUseALowerCase(String text, String defined_terms_text_document,
			boolean areDefinedTermsInThisDoc, boolean thorough) throws IOException {

		String total_text = "";

		total_text = text;
//		System.out.println("text.len=" + text.length());
//		System.out.println("total_text.len=" + total_text.length());
		NLP nlp = new NLP();
//		String textStoppedSimple = text;
		String textStopped = patternGoLawStopWordsReviewTools.matcher(text.replaceAll("    ", "XXXX ")).replaceAll(" ")
				.replaceAll("[ ]+", " ");// only review the primary document for term that may not have been defined
											// etc.
		textStopped = textStopped.replaceAll("XXXX ", "    ");
//		System.out.println("textStopped="+textStopped);
//		Pattern patternInitialCapsInSentence = Pattern.compile("(?sm)(?<=[ \t]{1}[a-z]{1,10} )([A-Z]{1}.*?[ ],?)+");

//		textStoppedSimple = text.replaceAll(" (and|of) ", " ");

		Pattern patternInitialCapsInSentence = Pattern.compile("(?sm)(?<=[ \t]{1}[a-z]{1,10} )([A-Z]{1}.{1,350}[ ],?)+" + ""
				+ "|(?<=[ ]{4}|\r\n)([A-Z]{1}.[A-Za-z\\d-]{5,10}(?=,?) )([A-Z]{1}.[A-Za-z\\d-]{1,10}(?=,?) )+(?=([,;a-z]+ ))");

//		List<String[]> list = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(textStoppedSimple,
//				patternInitialCapsInSentence);
//		List<String[]> list = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(textStopped,
//				patternInitialCapsInSentence);

		TreeMap<String, String> map_prelim_IC = new TreeMap<String, String>();

		String match = "";
		Matcher matcher = patternInitialCapsInSentence.matcher(textStopped);

		int sIdx, eIdx;
		String match_sIdx_eIdx;
		while (matcher.find()) {
			match = matcher.group();
			sIdx = matcher.start();
			eIdx = matcher.end();
			match_sIdx_eIdx = match + "||" + sIdx + "||" + eIdx;
			map_prelim_IC.put(match, match_sIdx_eIdx);
		}

		TreeMap<String, String> mapOfInitialCaps = new TreeMap<String, String>();
		TreeMap<Integer, String> mapItIsDefined = new TreeMap<Integer, String>();
		String str = "";
//		boolean notDef = false;
//		for (int i = 0; i < list.size(); i++) {

//		NLP.printMapStrStr("map_prelim_IC===", map_prelim_IC);

		for (Map.Entry<String, String> entry : map_prelim_IC.entrySet()) {
			str = entry.getKey();

//			notDef = false;
//			sIdx = Integer.parseInt(list.get(i)[1]);
//			eIdx = Integer.parseInt(list.get(i)[2]);
//			str = str; list.get(i)[0];
//			System.out.println("Defined Term===" + str);
			if (str.split(" ").length < 2)
				continue;
			if (nlp.getAllMatchedGroups(text, Pattern.compile(str.replaceAll("[^A-Z a-z\\d']", ""))).size() > 0
					&& nlp.getAllIndexEndLocations(str, Pattern.compile("[^a-zA-Z ]")).size() == 0) {
				mapOfInitialCaps.put(str.trim(), entry.getValue());
//				System.out.println("mapIC.put==" + str);
			}
		}

//		NLP.printMapStrStr("mapOfInitialCaps===", mapOfInitialCaps);
		String defs = "", def7 = "", def6 = "", def = "", def2 = "", def3 = "", def4 = "", def5 = "";
		List<String[]> listQuotes = nlp.getAllMatchedGroupsAndEndIdxLocs(textStopped, Pattern.compile("\".{1,100}?\""));
		// The use of all the def is to see if it exists in the map. Everything we look
		// for in the map is a potential defined terms encased in quotes. We are doing
		// this inefficiently because we re-search the entire text document for every
		// initial Word which can number in the multiple of hundreds. When all you
		// really need to do is find all instances in the document where a word is
		// encased "use those words then to see if they exist and if they do then you
		// continue. Create a map of words in quotes, then call from map def1/2/3/... -
		// and if ... then ...
//		NLP.printListOfStringArray("listQuotes===", listQuotes);
		String text_of_quoted_text = "", val;

		for (int i = 0; i < listQuotes.size(); i++) {
			text_of_quoted_text = text_of_quoted_text + listQuotes.get(i)[0] + "\r\n";
		}

//		System.out.println("strTest===" + strTest.substring(0,1555));

		for (Map.Entry<String, String> entry : mapOfInitialCaps.entrySet()) {
			val = entry.getValue().substring(0, entry.getValue().indexOf("||")).trim();
//			System.out.println("val==="+val);
			def = "\"" + val + "\"";
			def2 = "\"" + val.replaceAll("s$", "") + "\"";
			def3 = "\"" + val + "s\"";
			def4 = "\"" + val.replaceAll("(y$)", "ies").replaceAll("Y$", "IES") + "\"";
			def5 = "\"" + val + "\\.\"";
			def6 = "\"" + val + ",\"";
			def7 = "\"" + val.trim().replaceAll("es$", "") + "\"";

			defs = def + "|" + def2 + "|" + def3 + "|" + def4 + "|" + def5 + "|" + def6 + "|" + def7;
//			System.out.println("defs="+defs);
			if (nlp.getAllIndexEndLocations(text_of_quoted_text, Pattern.compile("(?sm)(" + defs + ")")).size() > 0
					|| nlp.getAllIndexEndLocations(val, Pattern.compile(
							"(?ism)(Accredited Investor|Standard|International|Reporting|Eastern|Qualified Instit|Outstanding|Depository Trust|Euroclear|Internal Rev"
									+ "|Account|The|United|Company|States?|Exchange|Bank |Article|ARTICLE|Section|SECTION"
									+ "|Board|Statistical Release|European Union|Vice |Secretary|Exhibit |Schedule|Annex |Signature"
									+ "|New York|LLCIRS|LLP|County|Uniform|[A-Z]{1}[a-z]+ Act ?$|Commodity Futures Trading Commission"
									+ "|Federal Reserve System|Federal Emergency Management Agency|Regulation|Federal Reserve|Foreign Corrupt Practices Act|"
									+ "Flood Insurance Rate Map|National Flood Insurance Program|Association)"))
							// add country/state pattern
							// also - add where if 1st and 2nd words
							.size() > 0
					|| nlp.getAllMatchedGroups(val.trim(), Pattern.compile("(ies|IES)$")).size() > 0) {
				continue;
			}

			if ((def.split(" ").length > 1
					&& nlp.getAllIndexEndLocations(text_of_quoted_text,
							Pattern.compile("(?sm)(" + def.split(" ")[0].trim() + "s?\")")).size() > 0
					&& nlp.getAllIndexEndLocations(text_of_quoted_text,
							Pattern.compile("(?sm)(\"" + def.split(" ")[1].trim() + ")")).size() > 0)
					|| (def2.split(" ").length > 1
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(" + def2.split(" ")[0].trim() + "s?\")")).size() > 0
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(\"" + def2.split(" ")[1].trim() + ")")).size() > 0)
					|| (def3.split(" ").length > 1
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(" + def3.split(" ")[0].trim() + "s?\")")).size() > 0
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(\"" + def3.split(" ")[1].trim() + ")")).size() > 0)
					|| (def4.split(" ").length > 1
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(" + def4.split(" ")[0].trim() + "s?\")")).size() > 0
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(\"" + def4.split(" ")[1].trim() + ")")).size() > 0)
					|| (def5.split(" ").length > 1
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(" + def5.split(" ")[0].trim() + "s?\")")).size() > 0
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(\"" + def5.split(" ")[1].trim() + ")")).size() > 0)
					|| (def6.split(" ").length > 1
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(" + def6.split(" ")[0].trim() + "s?\")")).size() > 0
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(\"" + def6.split(" ")[1].trim() + ")")).size() > 0)
					|| (def7.split(" ").length > 1
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(" + def7.split(" ")[0].trim() + "s?\")")).size() > 0
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(\"" + def7.split(" ")[1].trim() + ")")).size() > 0)

					|| nlp.getAllIndexEndLocations(val.replaceAll("\\.", ""),
							Pattern.compile("(?ism) (Corporation|Inc|Corp.|LTD|limited|LLC|LLP|PLC)")).size() > 0

			) {
//				System.out.println("continue -- defs=" + defs);
				continue;
			}

			else {
				if (!areDefinedTermsInThisDoc
						&& nlp.getAllMatchedGroups(defined_terms_text_document, Pattern.compile(defs)).size() > 0) {
					continue;
				}
				// xxxxx call sIdx/eIdx using val.substring......
				else {
//					System.out.println("1. it isn't defined=" + def);// Keshav: This is what we tell the user! then we
				}
				// finish here, and moved to the next function
				// below. All u need to do is find match anywhere in
				// docx ( |^)match[;:,.\r\n$] for the user. and it
				// can occur many times. we may want to allow user
				// to go to all instances
				// make a map of string, list<integer[sidx,edix]. this method w/o map will do
				// this: Applicable Law, Key Man, Applicable Law, Interest Rate, Key Man
			}
		}

		List<String[]> listSidxEidxTypeDefHdgsTmp = SolrPrep.getDefinitions_for_parsing_jsons(total_text);// what if there are two defined
																						// term sections?

//		NLP.printListOfStringArray("getDefinitions==", listSidxEidxTypeDefHdgsTmp);

		SolrPrep.listSidxEidxTypeDefHdgs = new ArrayList<String[]>();
		SolrPrep.listSidxEidxTypeDefHdgs = SolrPrep.validateDefinitions(listSidxEidxTypeDefHdgsTmp, total_text);

		String definitionName = "";

		int eIdxOfDefSec = 0;
		TreeMap<String, Integer> mapItIsDefinedStrKey = new TreeMap<String, Integer>();
		for (int n = 0; n < SolrPrep.listSidxEidxTypeDefHdgs.size(); n++) {
			definitionName = SolrPrep.listSidxEidxTypeDefHdgs.get(n)[3];
//			 System.out.println("defs=x==="+Arrays.toString(SolrPrep.listSidxEidxTypeDefHdgs.get(n)));
			mapItIsDefined.put(Integer.parseInt(SolrPrep.listSidxEidxTypeDefHdgs.get(n)[0]), definitionName);
			mapItIsDefinedStrKey.put(definitionName, Integer.parseInt(SolrPrep.listSidxEidxTypeDefHdgs.get(n)[0]));
			// record last def in list. then see if there are any defs defined after that.
			// those should be cross-ref in defined term section
			if (n + 1 == SolrPrep.listSidxEidxTypeDefHdgs.size()) {
				eIdxOfDefSec = Integer.parseInt(SolrPrep.listSidxEidxTypeDefHdgs.get(n)[1]);
			}
		}
//		System.out.println("eIdxOfDefSec===" + eIdxOfDefSec + " text.len=" + text.length());
		// TODO - Create pattern that shows if def section refers to section in contract
		// body of where it is defined. "Account" shall have the meaning referred to in.
		// if so then run algo to see if terms are defined in body but not in def
		// section
		// TODO -- need to add eIdx of contract body based on first instance of exhibit
		// -- b/c you can have new def terms in exhibits. Then test!
		List<String[]> listDef = new ArrayList<String[]>();
		if (eIdxOfDefSec > 0) {
			listDef = nlp.getAllStartIdxLocsAndMatchedGroups(total_text.substring(eIdxOfDefSec),
					Pattern.compile("(?sm)(collectively|the) \"[A-Z].{1,3000}\"|\\(\"[A-Z].*?\"\\)"));
		}

		String defThe = "", text_snippet;
		int idx;
		for (int i = 0; i < listDef.size(); i++) {

			defThe = listDef.get(i)[1].replaceAll("(?sm)(the \")(.{1,200})(\")", "$2");

			if (!mapItIsDefinedStrKey.containsKey(defThe)
					&& nlp.getAllIndexEndLocations(defThe, Pattern.compile("[A-Z]+")).size() > 0) {
				System.out.println("oh crap. you did not put in def section=" + defThe);// Keshav: displayed to the
																						// user. it knows that it was
																						// not put in the definitions
																						// section because the defined
																						// term is in quotes. Many f/p
																						// so just show the user the
																						// list. do not need to jump to
																						// loc yet unless it is easy to
																						// code.

				idx = Integer.parseInt(listDef.get(i)[0]);
				sIdx = Math.max(0, idx - 50);
				eIdx = Math.min(idx + 50, total_text.substring(eIdxOfDefSec).length());
				text_snippet = total_text.substring(eIdxOfDefSec);
				text_snippet = text_snippet.substring(sIdx, eIdx);
				System.out.println("as in docx==" + listDef.get(i)[1]);
				System.out.println("text snippet=" + text_snippet);// to find (be careful same exact text in more than 1
																	// place
				// TODO: Keshav--if it is Lenders then Subsidiary then Lenders-fix so the
				// Lenders are bunched. Otherwise keep idx order (rendered order)
				// TODO: I can improve accuracy by seeing if f/p is in an exhibit, where it is
				// usual to have terms defined redundantly.
			}
		}

		int idxLC = 0, idxIC = 0, cnt = 0;
		for (Map.Entry<Integer, String> entry : mapItIsDefined.entrySet()) {
			cnt++;
			idxLC = 0;
			idxIC = entry.getKey();
//			System.out.println("def?=" + entry.getValue());
//			System.out.println("def lc="+nlp.getAllMatchedGroups(textStopped, Pattern.compile(" " + entry.getValue().toLowerCase() + "[,;\\. ]"))
//					.size() );
//			System.out.println("def quote"+nlp.getAllMatchedGroups(textStopped, Pattern.compile("\"" + entry.getValue() + ",?\""))
//							.size());

			if (nlp.getAllMatchedGroups(text, Pattern.compile(" " + entry.getValue().toLowerCase() + "[,;\\. ]"))
					.size() > 0
					&& nlp.getAllMatchedGroups(entry.getValue(), Pattern.compile(
							"(?ism)account[ing]{0,3}|affiliate|agent|agreement|auction|beneficia|board|claim|class|class |collateral|company|control|credit"
									+ "|custodian|default|depositar|depository|due date|entitlement|euro|euroclear|general|guarant|holder|ii|incur|indebtedness"
									+ "|indenture|internal revenue|investment|law|lease|lender|lessee|lessor|lien|loan|loss |new york|nominee|note|officer|outstanding"
									+ "|overnight rate|person|plan|price|principal|payment|proceeds|property|record|recover|repurchase|security|series|servicer"
									+ "|subsidiary|taxes|trustee"))
							.size() == 0

			)

			{
				idxLC = nlp.getAllIndexStartLocations(text,
						Pattern.compile("(?sm) " + entry.getValue().toLowerCase() + "[,;\\. ]")).get(0);
//				idxIC = nlp
//						.getAllIndexStartLocations(textStopped, Pattern.compile("(?sm)\"" + entry.getValue() + ",?\""))
//						.get(0);

//				System.out.println("idxLC=" + idxLC + " idxIC=" + idxIC); 
				if (Math.abs(idxIC - idxLC) > 1000) {
					if (thorough) {
						System.out.println(
//					"idxLC=" + idxLC + " idxIC=" + idxIC + 
								"todo: land them in docx to the offending term, defined but you used the lower case of it cnt=="
										+ cnt + "  " + entry.getValue() + " key==" + entry.getKey());// lots of f/p so
																										// don't
																										// worry, just
						// show the user the list. with
						// option to jump to word. eg if
						// Business Day is defined
						// (value=Business Day) then jump to
						// each business day instance.
						continue;
					} else if (entry.getValue().trim().split(" ").length > 1) {
						System.out.println(
//								"idxLC=" + idxLC + " idxIC=" + idxIC + 
								"todo: land them in docx to the offending term, defined but you used the lower case of it cnt=="
										+ cnt + "  " + entry.getValue() + " key==" + entry.getKey());
					}
				}
			}
		}
	}

	public static List<String[]> it_is_not_defined(String text, String defined_terms_text_document,
			boolean areDefinedTermsInThisDoc, boolean thorough, boolean proofing) throws IOException {

		String total_text = "";

		List<String[]> listNotDefined = new ArrayList<String[]>();

		total_text = text;
		System.out.println("text.len=" + text.length());
//		System.out.println("total_text.len=" + total_text.length());
		NLP nlp = new NLP();
//		String textStoppedSimple = text;
		String textStopped = patternGoLawStopWordsReviewTools.matcher(text.replaceAll("    ", "XXXX ")).replaceAll(" ")
				.replaceAll("[ ]+", " ");// only review the primary document for term that may not have been defined
											// etc.
		textStopped = textStopped.replaceAll("XXXX ", "    ");
//		System.out.println("textStopped="+textStopped);
//		Pattern patternInitialCapsInSentence = Pattern.compile("(?sm)(?<=[ \t]{1}[a-z]{1,10} )([A-Z]{1}.*?[ ],?)+");

//		textStoppedSimple = text.replaceAll(" (and|of) ", " ");

		Pattern patternInitialCapsInSentence = Pattern.compile("(?sm)(?<=[ \t]{1}[a-z]{1,10} )([A-Z]{1}.{1,350}[ ],?)+" + ""
				+ "|(?<=[ ]{4}|\r\n)([A-Z]{1}.[A-Za-z\\d-]{5,10}(?=,?) )([A-Z]{1}.[A-Za-z\\d-]{1,10}(?=,?) )+(?=([,;a-z]+ ))");

//		List<String[]> list = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(textStoppedSimple,
//				patternInitialCapsInSentence);
//		List<String[]> list = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(textStopped,
//				patternInitialCapsInSentence);

		TreeMap<String, String> map_prelim_IC = new TreeMap<String, String>();

		String match = "";
		Matcher matcher = patternInitialCapsInSentence.matcher(textStopped);

		System.out.println("matcher");
		int sIdx, eIdx;
		String match_sIdx_eIdx;
		int cnt=0;
		while (matcher.find()) {
			cnt++;
//			System.out.println("matcher. cnt="+cnt);
			match = matcher.group();
			sIdx = matcher.start();
			eIdx = matcher.end();
			match_sIdx_eIdx = match + "||sIdx=" + sIdx + "||eIdx=" + eIdx;
			map_prelim_IC.put(match, match_sIdx_eIdx);
		}

		TreeMap<String, String> mapOfInitialCaps = new TreeMap<String, String>();
		TreeMap<Integer, String> mapItIsDefined = new TreeMap<Integer, String>();
		String str = "";
//		boolean notDef = false;
//		for (int i = 0; i < list.size(); i++) {

//		NLP.printMapStrStr("map_prelim_IC===", map_prelim_IC);

		for (Map.Entry<String, String> entry : map_prelim_IC.entrySet()) {
			str = entry.getKey();

//			notDef = false;
//			sIdx = Integer.parseInt(list.get(i)[1]);
//			eIdx = Integer.parseInt(list.get(i)[2]);
//			str = str; list.get(i)[0];
//			System.out.println("Defined Term===" + str);
			if (str.split(" ").length < 2)
				continue;
			if (nlp.getAllMatchedGroups(text, Pattern.compile(str.replaceAll("[^A-Z a-z\\d']", ""))).size() > 0
					&& nlp.getAllIndexEndLocations(str, Pattern.compile("[^a-zA-Z ]")).size() == 0) {
				mapOfInitialCaps.put(str.trim(), entry.getValue());
//				System.out.println("mapIC.put==" + entry.getValue());
			}
		}

//		NLP.printMapStrStr("mapOfInitialCaps===", mapOfInitialCaps);
		String defs = "", def7 = "", def6 = "", def = "", def2 = "", def3 = "", def4 = "", def5 = "";
		List<String[]> listQuotes = nlp.getAllMatchedGroupsAndEndIdxLocs(textStopped, Pattern.compile("\".{1,100}?\""));
		// The use of all the def is to see if it exists in the map. Everything we look
		// for in the map is a potential defined terms encased in quotes. We are doing
		// this inefficiently because we re-search the entire text document for every
		// initial Word which can number in the multiple of hundreds. When all you
		// really need to do is find all instances in the document where a word is
		// encased "use those words then to see if they exist and if they do then you
		// continue. Create a map of words in quotes, then call from map def1/2/3/... -
		// and if ... then ...
//		NLP.printListOfStringArray("listQuotes===", listQuotes);
		String text_of_quoted_text = "", val;

		for (int i = 0; i < listQuotes.size(); i++) {
			text_of_quoted_text = text_of_quoted_text + listQuotes.get(i)[0] + "\r\n";
		}

//		System.out.println("strTest===" + strTest.substring(0,1555));

		for (Map.Entry<String, String> entry : mapOfInitialCaps.entrySet()) {
			val = entry.getValue().substring(0, entry.getValue().indexOf("||")).trim();
//			System.out.println("val==="+val);
			def = "\"" + val + "\"";
			def2 = "\"" + val.replaceAll("s$", "") + "\"";
			def3 = "\"" + val + "s\"";
			def4 = "\"" + val.replaceAll("(y$)", "ies").replaceAll("Y$", "IES") + "\"";
			def5 = "\"" + val + "\\.\"";
			def6 = "\"" + val + ",\"";
			def7 = "\"" + val.trim().replaceAll("es$", "") + "\"";

			defs = def + "|" + def2 + "|" + def3 + "|" + def4 + "|" + def5 + "|" + def6 + "|" + def7;
//			System.out.println("defs="+defs);
			if (nlp.getAllIndexEndLocations(text_of_quoted_text, Pattern.compile("(?sm)(" + defs + ")")).size() > 0
					|| nlp.getAllIndexEndLocations(val, Pattern.compile(
							"(?ism)(Accredited Investor|Standard|International|Reporting|Eastern|Qualified Instit|Outstanding|Depository Trust|Euroclear|Internal Rev"
									+ "|Account|The|United|Company|States?|Exchange|Bank |Article|ARTICLE|Section|SECTION"
									+ "|Board|Statistical Release|European Union|Vice |Secretary|Exhibit |Schedule|Annex |Signature"
									+ "|New York|LLCIRS|LLP|County|Uniform|[A-Z]{1}[a-z]+ Act ?$|Commodity Futures Trading Commission"
									+ "|Federal Reserve System|Federal Emergency Management Agency|Regulation|Federal Reserve|Foreign Corrupt Practices Act|"
									+ "Flood Insurance Rate Map|National Flood Insurance Program|Association)"))
							// add country/state pattern
							// also - add where if 1st and 2nd words
							.size() > 0
					|| nlp.getAllMatchedGroups(val.trim(), Pattern.compile("(ies|IES)$")).size() > 0) {
				continue;
			}

			if ((def.split(" ").length > 1
					&& nlp.getAllIndexEndLocations(text_of_quoted_text,
							Pattern.compile("(?sm)(" + def.split(" ")[0].trim() + "s?\")")).size() > 0
					&& nlp.getAllIndexEndLocations(text_of_quoted_text,
							Pattern.compile("(?sm)(\"" + def.split(" ")[1].trim() + ")")).size() > 0)
					|| (def2.split(" ").length > 1
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(" + def2.split(" ")[0].trim() + "s?\")")).size() > 0
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(\"" + def2.split(" ")[1].trim() + ")")).size() > 0)
					|| (def3.split(" ").length > 1
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(" + def3.split(" ")[0].trim() + "s?\")")).size() > 0
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(\"" + def3.split(" ")[1].trim() + ")")).size() > 0)
					|| (def4.split(" ").length > 1
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(" + def4.split(" ")[0].trim() + "s?\")")).size() > 0
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(\"" + def4.split(" ")[1].trim() + ")")).size() > 0)
					|| (def5.split(" ").length > 1
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(" + def5.split(" ")[0].trim() + "s?\")")).size() > 0
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(\"" + def5.split(" ")[1].trim() + ")")).size() > 0)
					|| (def6.split(" ").length > 1
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(" + def6.split(" ")[0].trim() + "s?\")")).size() > 0
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(\"" + def6.split(" ")[1].trim() + ")")).size() > 0)
					|| (def7.split(" ").length > 1
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(" + def7.split(" ")[0].trim() + "s?\")")).size() > 0
							&& nlp.getAllIndexEndLocations(text_of_quoted_text,
									Pattern.compile("(?sm)(\"" + def7.split(" ")[1].trim() + ")")).size() > 0)

					|| nlp.getAllIndexEndLocations(val.replaceAll("\\.", ""),
							Pattern.compile("(?ism) (Corporation|Inc|Corp.|LTD|limited|LLC|LLP|PLC)")).size() > 0

			) {
//				System.out.println("continue -- defs=" + defs);
				continue;
			}

			else {
				if (!areDefinedTermsInThisDoc
						&& nlp.getAllMatchedGroups(defined_terms_text_document, Pattern.compile(defs)).size() > 0) {
					continue;
				}
				// xxxxx call sIdx/eIdx using val.substring......
				else {
					if(proofing) {
					System.out.println("1. it isn't defined=" + def);// Keshav: This is what we tell the user! then we
					String[] ary = { val,
							nlp.getAllMatchedGroups(entry.getValue(), Pattern.compile("(?<=sIdx=)[\\d]+")).get(0) };
					listNotDefined.add(ary);
					}
				}
				// finish here, and moved to the next function
				// below. All u need to do is find match anywhere in
				// docx ( |^)match[;:,.\r\n$] for the user. and it
				// can occur many times. we may want to allow user
				// to go to all instances
				// make a map of string, list<integer[sidx,edix]. this method w/o map will do
				// this: Applicable Law, Key Man, Applicable Law, Interest Rate, Key Man
			}
		}

		return listNotDefined;
	}

	public static List<String[]> youDefinedItButDidYouUseIt(String text) throws IOException {
		NLP nlp = new NLP();

		String str = "";
		List<String[]> list = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(text, Pattern.compile("\".{1,20000}\""));
//		System.out.println("you defined it but you didn't use -- are you a knucklehead==" + list.size());
		for (int i = 0; i < list.size(); i++) {
			str = list.get(i)[0].replaceAll("[\"\\(\\),]+|s\"$", "");
			str = str.replaceAll("[\\]\\[\\+\\{\\}\\?\\*\\\\]+", "");// <---fix -- can't replace these
//			System.out.println("list.get(i)==" + list.get(i) + " str=" + str);
			if (str.length() < 5)
				continue;
			if (nlp.getAllMatchedGroups(text, Pattern.compile(str)).size() < 2
					&& nlp.getAllMatchedGroups(text, Pattern.compile(str + "|" + str.toUpperCase())).size() < 2
					&& nlp.getAllMatchedGroups(text, Pattern.compile(str.replaceAll("y$", "ies"))).size() == 0
					&& list.get(i)[0].split(" ").length == nlp
							.getAllMatchedGroups(list.get(i)[0], Pattern.compile("(\"| )[A-Z]")).size()
					&& list.get(i)[0].replaceAll("[\" A-Z]+", "").length() > 0

			) {
//then call from list.get(i) [1] and [2] to use for snipped
				System.out.println("defined but you didn't use it=" + list.get(i)[0]);// Keshav:this is a good one, this
																						// is
																						// what is shown to the user
//				System.out.println("# of wds in def=" + list.get(i).split(" ").length + " # of cap wrds"
//						+ nlp.getAllMatchedGroups(list.get(i), Pattern.compile("(\"| )[A-Z]")).size());

			}
		}

		return list;
	}

	public static List<String[]> isItGenderNeutral(String text) throws IOException {
		NLP nlp = new NLP();

		List<String[]> list = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(text,
				Pattern.compile("(?ism) (he|his|him|man|men|gentlemen)[,;: ]{1}"));
		String sexistTextSnip = "";
		int snip = 50;
		for (int i = 0; i < list.size(); i++) {
			sexistTextSnip = text.substring(Math.max(0, Integer.parseInt(list.get(i)[1]) - snip),
					Integer.parseInt(list.get(i)[1]));
			sexistTextSnip = sexistTextSnip + text.substring(Integer.parseInt(list.get(i)[1]),
					Math.min(text.length(), Integer.parseInt(list.get(i)[1]) + snip));

			// System.out.println("sexists? sexistTextSnip maybe=" + sexistTextSnip);
			if (nlp.getAllMatchedGroups(sexistTextSnip,
					Pattern.compile("(?ism)[ \r\n\t](key|she|her|hers|woman|women|ladies|lady|madam)[,;: ]{1}"))
					.size() > 0)
				continue;
			System.out.println("uh oh - you are a dam sexist==" + list.get(i)[0] + "\r\ntxtSnip=" + sexistTextSnip);
			// Keshav: this is what is shown to the user
		}

		return list;
	}

	public static String findUnClosedParens(String text) throws IOException {

		NLP nlp = new NLP();
		int eIdx = 0, pEidx = 0;
		List<Integer> list = nlp.getAllIndexEndLocations(text, NLP.patternSentenceEnd);
		String sentence = "";

		for (int i = 0; i < list.size(); i++) {
			eIdx = list.get(i);
			sentence = text.substring(pEidx, eIdx);
			if (nlp.getAllMatchedGroups(sentence, Pattern.compile("\\(")).size() != nlp
					.getAllMatchedGroups(sentence, Pattern.compile("\\)")).size()) {
				System.out.println("uh of. you forgot a paren=" + sentence);
			}
			pEidx = eIdx;
		}
		return text;
	}

	public static void canIsignElectronically(String text) throws IOException {
		NLP nlp = new NLP();
		List<Integer> list = nlp.getAllIndexEndLocations(text, Pattern.compile("(?ism)(manual|signature|affix|notar)"));

		String str = "";
		for (int i = 0; i < list.size(); i++) {
			str = text.substring(Math.max(0, list.get(i) - 50), Math.min(text.length(), list.get(i) + 50));
			if (nlp.getAllIndexEndLocations(str, Pattern.compile("(?ism)electron|page|guarantee|your|any signature"))
					.size() > 0 && nlp.getAllIndexEndLocations(str, Pattern.compile("(?ism)affix|notar")).size() == 0)
				continue;
			else
				System.out.println("you need to allow for electronic signature. str=" + str);
		}
	}

	public static TreeMap<Double, String[]> getHeadings(String text, TreeMap<Double, String[]> mapIdx)
			throws IOException {

		GoLaw gl = new GoLaw();

		/*
		 * NOTE Do NOT touch text - length must not chg! No search and replace - length
		 * cannot chg! Or everything gets corrupted.
		 */

		/*
		 * getHeadings() runs on non-Section, Definition (and non-exhibit and non-toc)
		 * formmatted hdgs. It fetches first round of hdgs that are initial caps or
		 * formatted as previously marked by Qs
		 */

		System.out.println("headings start");
		NLP nlp = new NLP();
		double cntg = 0.000002;

		int dist = 175;
//		
//		Pattern patternHdg = Pattern.compile("(?sm)[A-Z].{4," + dist + "}");
//	Pattern patternHdg = Pattern.compile("(?ism)(?<=[\r\n]{2,} ?)(\\(?[\\dA-Za-z]{1,3}" + "(" + "\\.\\d?|\\)" + ") "
//				+ ")?[A-Z].{4," + 400 + "}?((\\.)|(\r\n))");

		Pattern patternHdg1 = Pattern.compile("(?ism)" + "((?<=[\r\n]{2}[ ]{0,3})" + "(Qx).{1," + dist + "}?(XQ)"
				+ "((\\.|:|(?= ?- ?))|(?=(\r\n)))" + ")"

				+ "|"

				+ "(?<=[\r\n]{2} ?)(\\(?[\\dA-Za-z]{1,3}" + "(" + "\\.\\d?\\d?\\.?\\d?|\\)" + ") "
				+ ")? ? ?\\[?[A-Z].{1," + dist + "}?((\\.|:)|(?=(\r\n)))" + "");

		Pattern patternHdg2 = Pattern.compile(
				"(?<=\r\n)(\\([a-zA-Z\\d]{1,2}\\)|[\\d\\.]{2,4}).{1,3}[A-Z]{1}.{4," + dist + "}?(\r\n|[\\.:]{1})");

		Pattern patternHdg = Pattern.compile(patternHdg1.pattern() + "|" + patternHdg2.pattern());

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
//		System.out.println("size of list of potential headings=" + list.size());

		String txtStopped = "", txt = "", para = "";
		int excludeInt = 0, eIdxHdr = 0;

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

		for (int i = 0; i < list.size(); i++) {
//			alphNmb = -1;
//			alphNmbNext = -5;
//			alphNmbPrev = -10;
			para = "";
			txt = list.get(i)[0].trim();

			alphNmbStr = txt.split(" ")[0];
			listCur = gl.convertAlphaParaNumb(alphNmbStr, 0);
			if (i + 1 < list.size()) {
				alphNmbNextStr = list.get(i + 1)[0].trim().split(" ")[0];
				if (gl.convertAlphaParaNumb(alphNmbNextStr, 0).size() > 0) {
					listNext = gl.convertAlphaParaNumb(alphNmbNextStr, 0);
				}
			}
			if (i > 0) {
				alphNmbPrevStr = list.get(i - 1)[0].trim().split(" ")[0];
				if (gl.convertAlphaParaNumb(alphNmbPrevStr, 0).size() > 0) {
					listPrior = gl.convertAlphaParaNumb(alphNmbPrevStr, 0);
				}
			}

			eIdxHdr = Integer.parseInt(list.get(i)[2]);
//			System.out.println("eIdxHdr=" + eIdxHdr);

//			if (txt.split(" ").length > 1) {
//				System.out.println("listHdg.get(i) txt.split[0]=" + txt.split(" ")[0]);
//			}
			if (txt.replaceAll("[^a-zA-Z]", "").replaceAll("(?ism)(qx|XQ|xxPD)+", "").trim().length() < 4
					|| txt.replaceAll("[QXXPDa-z\\d;,\\.\\$]+", "").length() == 0
					|| txt.trim().substring(txt.trim().length() - 1, txt.trim().length()).equals(";")) {
//				System.out.println("1 continue. txt.snip=" + txt.substring(0, Math.min(txt.length(), 30)).trim());
				continue;
			}

			txtStopped = GoLaw
					.goLawRemoveStopWordsAndContractNamesLegalStopWords(txt.replaceAll("[\r\n]+", " ")
							.replaceAll(" ?^\\([a-z\\d]+\\)|^[a-z\\d]+\\.\\)?", "").replaceAll("[^a-zA-Z ]+", ""))
					.replaceAll(" XQ", " ").replaceAll("[ ]+", " ").trim();

			tmp = txt.replaceAll(pAlphaNumeric.toString(), "");
//			System.out.println("tmp=" + tmp);
			excludeInt = nlp.getAllIndexStartLocations(tmp, ContractReviewTools.patternExcludeMeFromHeadings).size();
			// remove alpha numeric that start a header -
			// ^(a) or ^A etc.
//			System.out.println("excludeInt=" + excludeInt + " txt.snip=" + txt.substring(0, Math.min(0, 70))
//					+ "\r\ntxtStopped.snip=" + txtStopped.substring(0, Math.min(txtStopped.length(), 70)));

//			if(excludeInt>0) {
//				System.out.println("I excluded it b/c this was there=="
//						+ nlp.getAllMatchedGroups(txt, GC_Tester.patternExcludeMeFromHeadings).get(0));
//			}

			if (excludeInt > 0 // a header can't contain certain words or characters.
					|| ((txtStopped.length() > 100
//							|| nlp.getAllIndexStartLocations(txtStopped, Pattern.compile("(^| )[a-z]{1}")).size() > 0
					) && !txt.contains("Qx")) || nlp
							.getAllIndexEndLocations(txt,
									Pattern.compile("(?sm)QxSee |Qx.{0,4}(YEAR|Year|For the Year Ended).{0,4}XQ"))
							.size() > 0)

			// a header can't have lower case words once the stop words are removed.
			{
				// it is not a header if: the text is too long, if it has excluded pattern or
				// there are initial cap words after being stopped.
//				if (excludeInt > 0) {
//					System.out.println("excluding=="
//							+ nlp.getAllIndexStartLocations(tmp, GC_Tester.patternExcludeMeFromHeadings).get(0) + " ");
//				}
//				System.out.println("2 continue. txt.snip=" + txt.substring(0, Math.min(txt.length(), 70)).trim()
//						+ "||txtStopped.snip=" + txtStopped.substring(0, Math.min(txtStopped.length(), 30)).trim()
//						+ "||excludeInt=" + excludeInt + "" + " nlp.get...="
//						+ nlp.getAllIndexStartLocations(txtStopped, Pattern.compile("(^| )[a-z]{1}")).size());
				continue;
			}

//			System.out.println("isHdg?=" + txt.replaceAll(ptmpInsert.toString(), "\\."));
			// what follows a header should be a paragraph that is at least 300 chars
			para = text.substring(eIdxHdr, Math.min(eIdxHdr + dist, text.length()));
//			System.out.println("this is the uncut para that follows=" + para + "|END");
			if (nlp.getAllMatchedGroups(para, Pattern.compile(".*?\r\n")).size() > 0) {
				para = nlp.getAllMatchedGroups(para, Pattern.compile(".*?\r\n")).get(0);
				// gets the full para if there's a hard return (might not be b/c only have 300
				// chars). Or could be blank because there's hdg is on its own line.
//				System.out.println("this is the cut para that follows1=" + para + "|END");
			}

			if (para.length() == 0) {
				para = text.substring(eIdxHdr, Math.min(eIdxHdr + dist, text.length()));// reset para
//				System.out.println("starting again para=" + para);
				if (nlp.getAllMatchedGroups(para, Pattern.compile("\r\n.*[^ ]\r\n")).size() > 0) {
					// if hdg on its own line, after hard returns take all text that follows and
					// stop at hard return
					para = nlp.getAllMatchedGroups(para, Pattern.compile("\r\n.*[^ ]\r\n")).get(0);
//					System.out.println("par01=" + para);
					if (para.indexOf("\r\n") > 0) {// this cuts it to the first line with text
						para = para.substring(0, para.indexOf("\r\n"));
					}
				}
			}

			// if para len<100 then exclude. If it is all caps - treat as toc. If 'symbol'
			// or 'ticker' then not toc

			if (para.length() < 60
					&& nlp.getAllIndexEndLocations(list.get(i)[0], Pattern.compile("(?ism)ticker|symbol")).size() == 0
					&& !gl.isItConsecutivelyNumbered(listCur, listPrior, listNext)) {
				// if the text that follows isn't at least 1 para of 70 chars or more it isn't
				// possibly a hdg
//				System.out.println(" i=" + i + " hdg="
//						+ list.get(i)[0].substring(0, Math.min(50, txt.length())).replaceAll("(?<=Qx)[\r\n]+", " ")
//								.replaceAll("[ ]+", " ")
//						+ "\r\ni=" + i + "<70 para.len=" + para.length() + " para.snip="
//						+ para.substring(0, Math.min(150, para.length())).replaceAll("(?<=Qx) ?[\r\n]+", " ")
//								.replaceAll("[ ]+(?=XQ)", "").replaceAll("Qx[ ]+", "Qx").replaceAll("[ ]+", " ")
//								.trim());
				continue;
			}

			if ((GoLaw.goLawRemoveStopWordsAndContractNamesLegalStopWords(StopWords.removeDefinedTerms(para))
					.replaceAll("[ ]+", " ").split(" ").length < 5
					|| para.replaceAll("[a-z]+", "").length() == para.length())
					&& nlp.getAllMatchedGroups(para, Pattern.compile(":$")).size() == 0 && !txt.contains("Qx")) {
//				System.out.println("3 continue. txt.snip=" + txt.substring(0, Math.min(txt.length(), 30)).trim()
//						+ "\r\npara=" + para);
				continue;
			}

			if (GoLaw.goLawRemoveStopWordsAndContractNamesLegalStopWords(GoLaw.goLawRemoveDefinedTerms(para))
					.length() == 0) {
//				System.out.println("4 continue. para=" + para.substring(0, Math.min(para.length(), 50))
//						+ " rem stop words, def term="
//						+ StopWords.removeStopWords(StopWords.removeDefinedTerms(list.get(i)[0])).trim());
				continue;

			}
//			System.out.println("this is the hdg=" + txt.replaceAll(ptmpInsert.toString(), "\\."));

			String[] ary = { list.get(i)[1], list.get(i)[2],
					list.get(i)[0].replaceAll(ptmpInsert.toString(), "\\.").trim(), };
			listHdg.add(ary);
//			System.out.println("hdg==" + list.get(i)[0].replaceAll(ptmpInsert.toString(), "\\.").trim());
		}

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
//		PrintWriter 
//		pw = new PrintWriter(new File("c:/temp/7c. listHdgs.txt"));
////		StringBuffer 
//		sb = new StringBuffer();
//		for (int i = 0; i < listHdg.size(); i++) {
//			String[] ary = listHdg.get(i);
//			sb.append("7c. listHdgs i=" + i + " " + Arrays.toString(ary) + "\r\n");
//		}
//		pw.append(sb.toString());
//		pw.close();
		// <====DELETE ME

		List<String[]> listHdgCut = new ArrayList<String[]>();
		String tmpStr = "";
		for (int i = 0; i < listHdg.size(); i++) {
			tmpStr = listHdg.get(i)[2];
			tmpStr = tmpStr.replaceAll("XQQx", "").replaceAll("Qx.{1,2}XQ", "");
			if (tmpStr.indexOf("XQ") > 0) {
				String[] ary = { listHdg.get(i)[0], listHdg.get(i)[1], tmpStr.substring(0, tmpStr.indexOf("XQ") + 2) };
				listHdgCut.add(ary);
			} else {
				String[] ary = { listHdg.get(i)[0], listHdg.get(i)[1], tmpStr };
				listHdgCut.add(ary);
			}
		}

		String hdg = "";
		for (int i = 0; i < listHdgCut.size(); i++) {
			hdg = listHdgCut.get(i)[2];
			if (hdg.replaceAll(ptmpInsert.toString(), "\\.").trim().contains(":"))
				continue;

			String[] aryCon = gl.isHeadingConsecutive(listHdgCut, i, 2);
//			System.out.println("aryCon=" + Arrays.toString(aryCon));
			if (!hdg.contains("Qx") && aryCon[0].contains("false"))
				continue;
			if (aryCon[0].contains("true") && aryCon[1].equals("pNmbNext=0")) {
				tmp = text.substring(Integer.parseInt(listHdgCut.get(i)[1]));
				tmp = tmp.substring(0, tmp.indexOf("\r\n"));
				eIdx = Integer.parseInt(listHdgCut.get(i)[1]) + tmp.length();
//				System.out
//						.println("az hdg=" + listHdgCut.get(i)[2].substring(0, Math.min(listHdgCut.get(i)[2].length(), 50)));
//				System.out.println("+1az hdg=" + listHdgCut.get(i+1)[2].substring(0,Math.min(listHdgCut.get(i+1)[2].length(), 50)));

			} else {
//				System.out.println("is consec hdg test ==" + Arrays.toString(listHdgCut.get(i)) + " size=" + listHdgCut.size()
//						+ " i=" + i);
				if (i + 1 < listHdgCut.size()) {
					eIdx = Integer.parseInt(listHdgCut.get(i + 1)[0]) - 1;
				} else {
					eIdx = text.length();
				}
			}

			if (nlp.getAllIndexStartLocations(
					gl.goLawRemoveStopWordsAndContractNamesLegalStopWords(listHdgCut.get(i)[2]),
					Pattern.compile("(^| )[a-z]{1}")).size() > 0 && listHdgCut.get(i)[2].contains("Qx")) {

				String[] ary = { listHdgCut.get(i)[0], eIdx + "",
						"sub=" + listHdgCut.get(i)[2].replaceAll(ptmpInsert.toString(), "\\.").replaceAll("\r\n", " ")
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

		return mapIdx;
	}

	public static void check_section_references(String text) throws IOException {
		GoLaw gl = new GoLaw();
		System.out.println("get sections");
		TreeMap<Double, String[]> map = new TreeMap<Double, String[]>();
		map = gl.getSectionsMapped(text, map);
//		NLP.printMapDblStrAry("final file ", map);
	}

	public static String cut_contract_at_first_exhibit(String clientDoc) throws IOException {
		GoLaw gl = new GoLaw();

		TreeMap<Double, String[]> mapIdx = new TreeMap<Double, String[]>();
		boolean includeExhibits = false;
		mapIdx = gl.getExhibitsMapped(clientDoc, mapIdx);
//				System.out.println("getSectionsMapped seconds " + (System.currentTimeMillis() - startTime) / 1000);

//				System.out.println("getSectionsMapped.size=" + mapIdx.size());
//				pw = new PrintWriter(new File("c:/temp/5. Sections.txt"));
//				for (Map.Entry<Double, String[]> entry : mapIdx.entrySet()) {
//					pw.append("5. sections=" + Arrays.toString(entry.getValue()) + "\r\n");
//				}
//				pw.close();

		NLP.printMapDblStrAry("getExhibits mapIdx=", mapIdx);
		int exh_sIdx = 0;
		if (mapIdx.size() > 0 && !includeExhibits) {
			for (Map.Entry<Double, String[]> entry : mapIdx.entrySet()) {
				exh_sIdx = Integer.parseInt(entry.getValue()[0]);
				break;
			}
			clientDoc = clientDoc.substring(0, exh_sIdx);
			// this is to cut docx so that we don't create as many f/p with other functions.

		}

		return clientDoc;

	}

	public static String theVersusThis(String text) throws IOException {
		NLP nlp = new NLP();

		GoLaw gl = new GoLaw();
		TreeMap<Double, String[]> mapIdx = new TreeMap<Double, String[]>();
		TreeMap<Double, String[]> map = gl.getExhibitsMapped(text, mapIdx);
		// use exhibit location
		NLP.printMapDblStrAry("theVersus map of Exhibits==", map);
		List<Integer[]> list = nlp.getAllStartAndEndIdxLocations(text,
				Pattern.compile("(?sm)this ([A-Z]+[a-z]+[^a-z])+"));

		if (list == null)
			return "";

		TreeMap<String, List<Integer[]>> mapThis = new TreeMap<String, List<Integer[]>>();
		List<Integer[]> listTmp = new ArrayList<Integer[]>();
		String key = "";
		for (int i = 0; i < list.size(); i++) {
			key = text.substring(list.get(i)[0], list.get(i)[1]).replaceAll("[^a-zA-Z ]+", "");
			if (nlp.getAllMatchedGroups(key, Pattern.compile("this (Section|Article|Clause|Paragraph|Offer)"))
					.size() > 0)
				continue;// skip these b/c it is okay for them to be preceded by 'this'
			if (mapThis.containsKey(key)) {
				listTmp = new ArrayList<Integer[]>();
				listTmp = mapThis.get(key);
				listTmp.add(list.get(i));
				mapThis.put(key, listTmp);
				continue;
			}
			listTmp = new ArrayList<Integer[]>();
			listTmp.add(list.get(i));
			mapThis.put(key, listTmp);
		}

		String priorKey = "", keyChanged, maxThisAgtIs = "";
		int maxThisIs = 0, listSize = 0;

		for (Map.Entry<String, List<Integer[]>> entry : mapThis.entrySet()) {
			key = entry.getKey();// don't trim or disturb b/c you need to find it with sIdx/eIdx
			keyChanged = key.trim();
			listSize = entry.getValue().size();
			if (keyChanged.equals(priorKey))
				continue;
			System.out.println(
					"should this be preceded by \"this\"==" + key.trim() + "\r\n\t\tfound this many times:" + listSize);
			if (listSize > maxThisIs) {
				maxThisIs = listSize;
				maxThisAgtIs = key.replaceAll("^ ?[Tt]{1}his ", maxThisAgtIs).trim();
			}
			priorKey = keyChanged;
		}
		System.out.println(" this docx is defined as==" + maxThisAgtIs.replaceAll("this ", ""));

		return maxThisAgtIs;
	}

	public static void thePrecedesThisAgreementReference(String text, String agreement,
			boolean onlyCheckTheThisForThisAgreement) throws IOException {

		NLP nlp = new NLP();

		List<Integer[]> list = nlp.getAllStartAndEndIdxLocations(text,
				Pattern.compile("(?sm)(the )([A-Z]+[a-z]+ )+(Agreement)|the Agreement|the Indenture|the Lease"));

		if (list == null)
			return;

		TreeMap<String, List<Integer[]>> mapThis = new TreeMap<String, List<Integer[]>>();
		List<Integer[]> listTmp = new ArrayList<Integer[]>();
		String key = "";
		for (int i = 0; i < list.size(); i++) {
			key = text.substring(list.get(i)[0], list.get(i)[1]).replaceAll("[^a-zA-Z ]+", "");
			if (mapThis.containsKey(key)) {
				listTmp = new ArrayList<Integer[]>();
				listTmp = mapThis.get(key);
				listTmp.add(list.get(i));
				mapThis.put(key, listTmp);
				continue;
			}
			listTmp = new ArrayList<Integer[]>();
			listTmp.add(list.get(i));
			mapThis.put(key, listTmp);
		}

		String priorKey = "", keyChanged;
		for (Map.Entry<String, List<Integer[]>> entry : mapThis.entrySet()) {
			key = entry.getKey();// don't trim or disturb b/c you need to find it with sIdx/eIdx
			keyChanged = key.trim();
			if (keyChanged.equals(priorKey))
				continue;
			if (onlyCheckTheThisForThisAgreement
					&& key.trim().replaceAll("^ ?[Tt]{1}he ", "").trim().equals(agreement)) {
				System.out.println("should this be preceded by \"the\"==" + key.trim());
			}
			if (!onlyCheckTheThisForThisAgreement) {
				System.out.println("should this be preceded by \"the\"==" + key.trim());
			}
			priorKey = keyChanged;
//			System.out.println(
//					"will this==" + key.trim().replaceAll("^ ?[Tt]{1}he ", "") + " match with this=" + agreement);

		}

		// if they say it should not be then let them to jum to each instance in the
		// list below
		for (Map.Entry<String, List<Integer[]>> entry : mapThis.entrySet()) {
			for (int i = 0; i < entry.getValue().size(); i++) {
				// if key is the one that they said they should check then we show all instances
				// of where that key appears by going to that text location through use of sIdx
				// and eIdx of the List<Ingeger[]> in the map
//				System.out.println(
//						" key=" + entry.getKey() + "| list int ary=" + Arrays.toString(entry.getValue().get(i)));
			}
		}
	}

	public static String getParagraphAtThisLocation(String text, int idx) {

		NLP nlp = new NLP();
		String para = "", paraEnd, paraStart;
		paraEnd = text.substring(idx, text.length());
		paraEnd = paraEnd.substring(0, paraEnd.indexOf("\r\n"));// first \r\n after para location
//		System.out.println("paraEnd=="+paraEnd+"|");
		paraStart = text.substring(0, idx);
		paraStart = paraStart.substring(paraStart.lastIndexOf("\r\n"), paraStart.length());// last \r\n before para
																							// location
		para = paraStart + paraEnd;

		return para.trim();
	}

	public static void checkParagraphEnds(String text) throws IOException {
		NLP nlp = new NLP();
		GoLaw gl = new GoLaw();
		System.out.println("check para ends");
		Pattern patternPend = Pattern.compile("[\\.;,] ? ?(and|or|plus|minus)? ? ?\r\n");// does not have to have and|or
		Pattern patternSecondToLast = Pattern.compile("[\\.;,] ? ?(and|or|plus|minus) ? ?\r\n");// must have and|or

		List<Integer[]> list = nlp.getAllStartAndEndIdxLocations(text, Pattern.compile("(?<!\r\n)" + patternPend));

		if (list == null)
			return;

		Pattern paraNumb = Pattern.compile("^ ?\\(?[a-zA-Z\\d]{1,6}\\.?\\)|^ ?\\(?[A-Z\\d]{1,2}\\.\\)?");
		boolean found = false;
		String snippet = "", snippetPrior = null, para = null, paraPrior = null, pEnd, pEndPrior = null;
		int lastSemi = 0, penUlt = 0, sIdx = 0, eIdx = 0, sIdxPrior = 0;
		for (int i = 0; i < list.size(); i++) {
			found = false;
			sIdx = list.get(i)[0];
			eIdx = list.get(i)[1];

			pEnd = text.substring(sIdx, eIdx);
			snippet = text.substring(Math.max(sIdx - 25, 0), Math.min(eIdx + 25, text.length()));

			if (nlp.getAllIndexEndLocations(pEnd, Pattern.compile("[,;]")).size() > 0
					&& nlp.getAllStartAndEndIdxLocations(pEnd, patternSecondToLast).size() == 0) {
//				System.out.println("i==" + i + " here's a para ending with semicolon" + pEnd);
				lastSemi = i;
			}

			if (nlp.getAllStartAndEndIdxLocations(pEnd, patternSecondToLast).size() > 0) {
//				System.out.println("i==" + i + " here's a para ending with semicolon then and/or == " + pEnd);
				penUlt = i;
			}

			if ((pEnd.trim().equals(".")) && i - lastSemi == 1 && i - penUlt != 1 && lastSemi > 0
					&& !snippetPrior.contains("provided")
					&& nlp.getAllIndexEndLocations(snippetPrior, Pattern.compile("[ ]{0,10}[\r\n]+[ ]{0,10}"))
							.size() < 3
					&& nlp.getAllIndexEndLocations(snippet, Pattern.compile("[ ]{0,10}[\r\n]+[ ]{0,10}")).size() < 3

			) {
				found = true;
				para = getParagraphAtThisLocation(text, sIdx).trim();
				paraPrior = getParagraphAtThisLocation(text, sIdxPrior).trim();

				if ((nlp.getAllMatchedGroups(para, paraNumb).size() > 0
						&& nlp.getAllMatchedGroups(paraPrior, paraNumb).size() == 0) ||

						(nlp.getAllMatchedGroups(paraPrior, paraNumb).size() > 0
								&& nlp.getAllMatchedGroups(para, paraNumb).size() == 0)
						|| nlp.getAllMatchedGroups(paraPrior, Pattern.compile("\\.,{1,2}$")).size() > 0
						|| nlp.getAllMatchedGroups(para, Pattern.compile("\\.,{1,2}$")).size() > 0) {
					found = false;
//					System.out.println("1.false. one has para# and other does not");
//					System.out.println("para1==" + paraPrior + "|end");
//					System.out.println("para2==" + para + "|end");
					// can't have one para with a para # that starts and the prior does not
				}
				if (nlp.getAllMatchedGroups(para, paraNumb).size() > 0
						&& nlp.getAllMatchedGroups(paraPrior, paraNumb).size() > 0
						&& gl.convertAlphaParaNumbAndType(nlp.getAllMatchedGroups(para, paraNumb).get(0), true, "").get(
								0)[1] != gl.convertAlphaParaNumbAndType(nlp.getAllMatchedGroups(para, paraNumb).get(0),
										true, "").get(0)[1]) {
					found = false;
//					System.out.println("1.false. one has para# type different than the other");
//					System.out.println("para1==" + paraPrior + "|end");
//					System.out.println("para2==" + para + "|end");

					// can't have a para# start with one type and the next para start with another
					// (eg (D) then (a) )
				}

				if (found && para.replaceAll("[^a-z]+", "").length() > 1
						&& paraPrior.replaceAll("[^a-z]+", "").length() > 1
						&& para.replaceAll("([A-Z\\.]+[a-z\\.\\:]+ ?)+", "").trim().length() > 3
						&& paraPrior.replaceAll("([A-Z\\.]+[a-z\\.\\:]+ ?)+", "").trim().length() > 3) {

					System.out.println("1. your prior paragraph ends with \"" + pEndPrior.trim()
							+ "\" but your current paragraph ends with: \"" + pEnd.trim() + "\"");
//					System.out.println(" snippetPrior==" + snippetPrior + "|end");
//					System.out.println(" snippet==" + snippet + "|end");
					System.out.println("1. jump them to this para first==" + paraPrior + "|end");
					System.out.println("\r\n1. then jump them to this para==" + para + "|end");

				}
			}

			if (nlp.getAllMatchedGroups(pEnd, Pattern.compile(" (and|or)")).size() > 0
					&& nlp.getAllMatchedGroups(pEndPrior, Pattern.compile(" (and|or)")).size() > 0
					&& i - lastSemi == 1) {
				found = true;

				para = getParagraphAtThisLocation(text, sIdx).trim();
				paraPrior = getParagraphAtThisLocation(text, sIdxPrior).trim();
				if ((nlp.getAllMatchedGroups(para, paraNumb).size() > 0
						&& nlp.getAllMatchedGroups(paraPrior, paraNumb).size() == 0) ||

						(nlp.getAllMatchedGroups(paraPrior, paraNumb).size() > 0
								&& nlp.getAllMatchedGroups(para, paraNumb).size() == 0)
						|| nlp.getAllMatchedGroups(paraPrior, Pattern.compile("\\.,{1,2}$")).size() > 0
						|| nlp.getAllMatchedGroups(para, Pattern.compile("\\.,{1,2}$")).size() > 0) {
					found = false;
					// can't have one para with a para # that starts and the prior does not

//					System.out.println("2.false. one has para# and other does not");
//					System.out.println("para1==" + paraPrior + "|end");
//					System.out.println("para2==" + para + "|end");
				}
				if (nlp.getAllMatchedGroups(para, paraNumb).size() > 0
						&& nlp.getAllMatchedGroups(paraPrior, paraNumb).size() > 0
						&& gl.convertAlphaParaNumbAndType(nlp.getAllMatchedGroups(para, paraNumb).get(0), true, "").get(
								0)[1] != gl.convertAlphaParaNumbAndType(nlp.getAllMatchedGroups(para, paraNumb).get(0),
										true, "").get(0)[1]) {
					found = false;
					// can't have a para# start with one type and the next para start with another
					// (eg (D) then (a) )

//					System.out.println("2.false. one has para# type different than the other");
//					System.out.println("para1==" + paraPrior + "|end");
//					System.out.println("para2==" + para + "|end");
				}
				if (found && para.replaceAll("[^a-z]+", "").length() > 1
						&& paraPrior.replaceAll("[^a-z]+", "").length() > 1
						&& para.replaceAll("([A-Z\\.]+[a-z\\.\\:]+ ?)+", "").trim().length() > 3
						&& paraPrior.replaceAll("([A-Z\\.]+[a-z\\.\\:]+ ?)+", "").trim().length() > 3) {

					System.out.println("2. And and -- your prior paragraph end with \"" + pEndPrior.trim()

							+ "\" but your current paragraph ends with: \"" + pEnd.trim() + "\"");
//			System.out.println(" snippetPrior==" + snippetPrior + "|end");
//			System.out.println(" snippet==" + snippet + "|end");
					System.out.println("2..jump them to this para first==" + paraPrior + "|end");
					System.out.println("2..them jump them to this para==" + para + "|end");

				}
			}

//			System.out.println("pEnd==" + pEnd.trim()+" i="+i+" penUlt="+penUlt+" lastSemi="+lastSemi);
			snippetPrior = snippet;
			pEndPrior = pEnd;
			sIdxPrior = sIdx;

		}
	}

	public static void main(String[] args) throws IOException, SQLException, ParseException {

		NLP nlp = new NLP();
		GoLaw gl = new GoLaw();

		String clientDoc, defined_terms = null;
//		Utils.readWordDocXFile("F:\\Perkins_Matters\\SecurityLife\\Citadel - Trust Agreement.docx");
		boolean areDefinedTermsInThisDoc = true;// if false ask them to upload the doc with the defined terms.
//		clientDoc = Utils.readTextFromFile("C:/temp/temp1010.txt");// this works w/ getSectionsMapped
		clientDoc = Utils
				.readWordDocXFile("C:\\Perkins_Matters\\GOL\\PrivatePlacement\\\\GOL (Project Glide) - Indenture.docx");
		// TODO: this does not work w/ getSectionsMapped!! need to use apose. See
		// Fox_Europcar 2021-1 - Base Indenture.DOCX

		String origClientDoc = clientDoc;
		long startTime = System.currentTimeMillis();
		clientDoc = gl.stripHtml(clientDoc);// Keshav: or just use the replaceAll to clean b/c this take time 2.5
		// seconds
		clientDoc = clientDoc.replaceAll("\r\n", "\r\n\r\n").replaceAll("�|”|“", "\"").replaceAll("�", "\"")
				.replaceAll("(?<=\r\n)(((?ism)section )? ?)[\\d\\.]+ ", "").replaceAll("(?ism)xxPD", "\\.")
				.replaceAll("\t", "  ");
		PrintWriter pw = new PrintWriter(new File("c:/temp/readWord_temp1010.txt"));
		pw.append(clientDoc);
		pw.close();

		System.out.println("text.len==" + clientDoc.length());
		System.out.println("strip ms: " + (System.currentTimeMillis() - startTime));

		if (!areDefinedTermsInThisDoc) {
			// ask the user if DEFINED TERMS are in a separate document if true the user
			// will upload the defined terms, if false then they don't do anything
			System.out.println("defined terms are in this doc");
			defined_terms = Utils
					.readWordDocXFile("C:\\Perkins_Matters\\Fox_Europcar\\Fox_Europcar 2021-1 - Base Indenture.DOCX");
			defined_terms = gl.stripHtml(defined_terms);// Keshav: or just use the replaceAll to clean b/c this take
														// time 2.5
			// seconds
			defined_terms = defined_terms.replaceAll("\r\n", "\r\n\r\n").replaceAll("\t", "    ")
					.replaceAll("�|”|“", "\"").replaceAll("�", "\"")
					.replaceAll("(?<=\r\n)(((?ism)section )? ?)[\\d\\.]+ ", "").replaceAll("(?ism)xxPD", "\\.");
		} else {

			defined_terms = clientDoc;

		}

//		put cut exh method here  xxxxxxx
		TreeMap<Double, String[]> mapIdx = new TreeMap<Double, String[]>();
		String originalClientDoc = clientDoc;
		boolean includeExhibits = false;
		mapIdx = gl.getExhibitsMapped(clientDoc, mapIdx);
//				System.out.println("getSectionsMapped seconds " + (System.currentTimeMillis() - startTime) / 1000);

//				System.out.println("getSectionsMapped.size=" + mapIdx.size());
//				pw = new PrintWriter(new File("c:/temp/5. Sections.txt"));
//				for (Map.Entry<Double, String[]> entry : mapIdx.entrySet()) {
//					pw.append("5. sections=" + Arrays.toString(entry.getValue()) + "\r\n");
//				}
//				pw.close();

		NLP.printMapDblStrAry("getExhibits mapIdx=", mapIdx);
		int exh_sIdx = 0;
		String clientDoc_withoutExhibits = clientDoc;
		if (mapIdx.size() > 0 && !includeExhibits) {
			for (Map.Entry<Double, String[]> entry : mapIdx.entrySet()) {
				exh_sIdx = Integer.parseInt(entry.getValue()[0]);
				break;
			}
			clientDoc_withoutExhibits = clientDoc.substring(0, exh_sIdx);
			// this is to cut docx so that we don't create as many f/p with other functions.

		}

		mapIdx = gl.getSectionsMapped(clientDoc_withoutExhibits, mapIdx);
		NLP.printMapDblStrAry("getSectionsMapped mapIdx=", mapIdx);
		SolrPrep.listPrelimIdxAndSectionRestrictive = ContractNLP.getAllIdxLocsAndMatchedGroups(clientDoc,
				ContractParser.patternSectionHeadingRestrictive);

		List<String[]> listPrelimIdxAndSectionLessRestrictiveWithNumberSectionAlso = ContractNLP
				.getAllIdxLocsAndMatchedGroups(clientDoc,
						ContractParser.patternSectionHeadingLessRestrictiveWithNumberSectionAlso);

//		NLP.printListOfStringArray("1b listPrelimIdxAndSectionLessRestrictiveWithNumberSectionAlso",
//				listPrelimIdxAndSectionLessRestrictiveWithNumberSectionAlso);

//		startTime = System.currentTimeMillis();
		List<String[]> patternSectionHeadingLessRestrictiveWithoutNumber = ContractNLP.getAllIdxLocsAndMatchedGroups(
				clientDoc, ContractParser.patternSectionHeadingLessRestrictiveWithoutNumber);
		// System.out.println("3 textSidxEidx.len="+textSidxEidx.length());

//		NLP.printListOfStringArray("1c patternSectionHeadingLessRestrictiveWithoutNumber==",
//				patternSectionHeadingLessRestrictiveWithoutNumber);

		if (listPrelimIdxAndSectionLessRestrictiveWithNumberSectionAlso.size()
				* 1.3 < patternSectionHeadingLessRestrictiveWithoutNumber.size()) {

			SolrPrep.listPrelimIdxAndSectionLessRestrictive = patternSectionHeadingLessRestrictiveWithoutNumber;

		} else {
			SolrPrep.listPrelimIdxAndSectionLessRestrictive = listPrelimIdxAndSectionLessRestrictiveWithNumberSectionAlso;
		}
//		NLP.printListOfStringArray("1t SolrPrep.listPrelimIdxAndSectionLessRestrictive==",
//				SolrPrep.listPrelimIdxAndSectionLessRestrictive);

		List<String[]> listSidxEidxTypeSecHdgTmp = SolrPrep.listPrelimIdxAndSectionLessRestrictive;
//		List<String[]> listSidxEidxTypeSecHdgTmp = SolrPrep.getSectionIdxsAndNames(clientDoc);
//		NLP.printListOfStringArray("listSidxEidxTypeSecHdgTmp....", listSidxEidxTypeSecHdgTmp);

		List<String[]> listSidxEidxTypeSecHdgs = new ArrayList<>();
		String secHdg = "", secHdgNext = "", secHdgPrior = "";
		// , str = "";
		for (int i = 0; i < listSidxEidxTypeSecHdgTmp.size(); i++) {
			secHdg = listSidxEidxTypeSecHdgTmp.get(i)[1];
			// System.out.println("Z. secHdg="+secHdg);
			if (i + 1 < listSidxEidxTypeSecHdgTmp.size()) {
				secHdgNext = listSidxEidxTypeSecHdgTmp.get(i + 1)[1];
			}

			if (i + 1 == listSidxEidxTypeSecHdgTmp.size() && listSidxEidxTypeSecHdgTmp.size() > 1) {
				secHdgPrior = listSidxEidxTypeSecHdgTmp.get(i - 1)[1];
			}

			if ((secHdgNext.equals(secHdg) && i + 1 < listSidxEidxTypeSecHdgTmp.size())
					|| (i + 1 == listSidxEidxTypeSecHdgTmp.size() && secHdgPrior.equals(secHdg))) {
				continue;
			}

//			System.out.println("listSidxEidxTypeSecHdgTmp - .add=" + Arrays.toString(listSidxEidxTypeSecHdgTmp.get(i)));
			listSidxEidxTypeSecHdgs.add(listSidxEidxTypeSecHdgTmp.get(i));

		}
//		NLP.printListOfStringArray("listSidxEidxTypeSecHdgs===", listSidxEidxTypeSecHdgs);

		String hdg;
		int sIdx = 0;
		for (int i = 0; i < listSidxEidxTypeSecHdgs.size(); i++) {
			hdg = listSidxEidxTypeSecHdgs.get(i)[1].replaceAll("\\.", "xxpd");
			hdg = hdg.replaceAll("(^| )upon( |,|;|:|$)", "");// remove these so I don't get f/p
			hdg = StopWords.removeStopWords(hdg);
			hdg = hdg.replaceAll("\\(?[a-zA-Z\\d]{1,6}\\.?\\)|[A-Z]+[a-zA-Z/’\'\"-]+", " ")
					.replaceAll("[^A-Za-z ]", " ").trim();
			if (nlp.getAllIndexEndLocations(hdg, Pattern.compile("etc|\\d")).size() > 0
					|| hdg.replaceAll("xxpd", "").length() == 0)
				continue;
			if (hdg.length() > 0)
				System.out.println("1. this heading word should be initial caps=="
						+ hdg.replaceAll("([A-Z]{0,1}xxpd)+", "\\.").replaceAll("^ ?\\.| \\.$", "").trim()
						+ "\r\nthis is the hdg==" + listSidxEidxTypeSecHdgs.get(i)[1]);// new proofing tool: section
																						// heading formatting
		}

//		mapIdx = getHeadings(clientDoc, mapIdx);
//		System.out.println("getHeadings.size=" + mapIdx.size());
//		NLP.printMapDblStrAry("getHeadings mapIdx=", mapIdx);
//
//		for (Map.Entry<Double, String[]> entry : mapIdx.entrySet()) {
//			hdg = StopWords.removeStopWords(entry.getValue()[2].replaceAll("hdg=", ""));
//			System.out.println("a. hdg==="+hdg);
//			hdg = hdg.replaceAll("\\(?[a-zA-Z\\d]{1,6}\\.?\\)|[A-Z]+[a-zA-Z/’\'\"-]+", "").replaceAll("[^A-Za-z ]", "")
//					.trim();
//			System.out.println("b. hdg==="+hdg);
//			if (hdg.length() > 0)
//				System.out.println("2. this heading word should be initial caps==" + hdg + "\r\nthis is the hdg=="
//						+ entry.getValue()[2]);
//
//		}

		check_section_references(clientDoc);

		// before doing anything else fix the below problem with the'.'
		// Throwing off the alphabetical order method. Just like
		// with space if it's a. That causes it to be out of order
		// then ignore it and don't because it to be reported as out
		// of order. The nospace method to work equally well.
		// out-of-order==u.s. government obligations key==218 if true its not out of
		// order=false word=u.s. government obligations wordprior=trustee
//		out-of-order==u.s. person key==222 if true its not out of order=false word=u.s. person wordprior=u.s. government obligations
//				out-of-order==ucc key==217 if true its not out of order=false word=ucc wordprior=u.s. person
//		String t = Utils.readTextFromFile("D:\\Perkins_Matters\\Atkore\\Indenture.txt");

		boolean thorough = false;
		didYouDefineItOrDefineItButUseALowerCase(clientDoc_withoutExhibits, defined_terms, areDefinedTermsInThisDoc,
				thorough);
		// TODO: Fix to remove 'it isn't defined' method embedded but works now.
		// TODO: fix bc it shld see reference to defined in paren and therefore not
		// treat as not defined --see - "Bridge Agreement (as defined in the indenture
		// governing the Existing Senior Notes) " in temp\kl.docx
		// ignore section headings; pickup defined word with lowercase stopwords.Ignore:
		// it isn't defined="Exhibit C" - also it isn't defined="Federal Reserve System"
		// Participated Whole Loan is thought to be undefined but it appears as
		// Participated Whole Loan(s). see temp\reinsurance trust.docx
//		f/P Recapture Date (as defined in the Reinsurance Agreement)  (it is defined.)
		it_is_not_defined(clientDoc, defined_terms, areDefinedTermsInThisDoc, thorough,true);// <---use entire client doc

		youDefinedItButDidYouUseIt(clientDoc);// fix "Credit Facilities" says it wasn't used. fix: exclude stuff in
												// quotes
		// that

		// are in lower case. Fix: says "Gaming Licenses" isn't used but Gaming License
		// is. Maybe use lemma tool to check. It thothat you 11ught Related Party was
		// not used, but
		// it was. Consider dropping y to determine if it was used.

		// keshav: alpha method starts here.
		String defined_terms_doc = clientDoc;
		if (!areDefinedTermsInThisDoc) {
			System.out.println("defined terms are in this other docx");
			defined_terms_doc = defined_terms;
		}

//		System.out.println(defined_terms_doc);
		List<String[]> listSidxEidxTypeDefHdgsTmp = SolrPrep.getDefinitions_for_parsing_jsons(defined_terms_doc);// extract the method
																								// versus using it in
																								// solrPrep
//		NLP.printListOfStringArray("listSidxEidxTypeDefHdgsTmp", listSidxEidxTypeDefHdgsTmp);
		List<String[]> list_defined_in_body = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(defined_terms_doc,
				Pattern.compile("(?<=\")[A-Z].{5,80}?(?=\"\\))"));
//		NLP.printListOfStringArray("list_defined_in_body=", list_defined_in_body);

		TreeMap<String, String> mapOfDefinitionsText = new TreeMap<String, String>();
		if (null != listSidxEidxTypeDefHdgsTmp && listSidxEidxTypeDefHdgsTmp.size() > 0) {
			for (int i = 0; i < listSidxEidxTypeDefHdgsTmp.size(); i++) {
				mapOfDefinitionsText.put(listSidxEidxTypeDefHdgsTmp.get(i)[3].trim(),
						defined_terms_doc.substring(Integer.parseInt(listSidxEidxTypeDefHdgsTmp.get(i)[0]),
								Integer.parseInt(listSidxEidxTypeDefHdgsTmp.get(i)[1])));
			}
		}
//		NLP.printMapStringString("mapOfDefinitionsText==", mapOfDefinitionsText);
		List<String[]> listSidxEidxTyDefHdgs = SolrPrep.validateDefinitions(listSidxEidxTypeDefHdgsTmp,
				defined_terms_doc);
//		NLP.printListOfStringArray("def list==", listSidxEidxTyDefHdgs);
		// return list with break for each def sec when multi-definition sections exist
		// rule-out when it is not the docx where everything is defined
//		NLP.printListOfStringArray("areDefinedTermsAlphabetical==", listSidxEidxTyDefHdgs);
		TreeMap<Integer, List<String[]>> map_multi_defs = Alphabetical
				.get_multi_definition_sections(listSidxEidxTyDefHdgs, 20000);

//		NLP.printMapIntListOfStringAry("multi def??", map_multi_defs);
		TreeMap<String, String> map_has_meaning_specified_in = new TreeMap<String, String>();
		String hasMeaningStr;
		for (Map.Entry<Integer, List<String[]>> entry : map_multi_defs.entrySet()) {
			for (int a = 0; a < entry.getValue().size(); a++) {
				hasMeaningStr = defined_terms.substring(Integer.parseInt(entry.getValue().get(a)[0]),
						Integer.parseInt(entry.getValue().get(a)[1]));
				// if has meaning specified in... or words to that affect
				// record as key the def term. and later if it is believed to be def twice and
				// cnt=2 then exclude as f/p (knucklehead) by calling it from this map the def
				// term produced by you defined it twice knucklehead=
//				System.out.println("hasMeaningStr==" + hasMeaningStr);
				if (nlp.getAllIndexEndLocations(hasMeaningStr,
						Pattern.compile("meaning specified in|meaning set forth in|"
								+ "meaning assigned to such term in|" + "defined (under|in) |mean.{2,25}identified "
								+ "|mean.{1,80}(Schedule|Exhibit|Annex|Appendix|SCHEDULE|EXHIBIT|ANNEX|APPENDIX)"))
						.size() > 0) {
//					System.out.println("hasMeaning Specified in=="+Arrays.toString(entry.getValue().get(a)));
					map_has_meaning_specified_in.put(entry.getValue().get(a)[3], hasMeaningStr);
				}
			}
		}

		NLP.printMapStringString("map_has_meaning_specified_in===", map_has_meaning_specified_in);
//		System.out.println("are they alphabetical?");
		for (Map.Entry<Integer, List<String[]>> entry : map_multi_defs.entrySet()) {
			listSidxEidxTyDefHdgs = Alphabetical.areDefinedTermsAlphabetical(entry.getValue(), defined_terms_doc);
//			NLP.printListOfStringArray("key = "+ entry.getKey()+ "  new key is new  definition section ==", entry.getValue());
		}
		// See if terms are defined twice by inserting the list of define terms from the
		// above two lists and if there are two of the same it was defined twice.
		TreeMap<String, Integer> mapWas_defined_twice = new TreeMap<String, Integer>();
		int eIdx;

		TreeMap<String, List<Integer>> map_multi_defs_idx_loc = new TreeMap<String, List<Integer>>();
		List<Integer> listInt = new ArrayList<Integer>();
		for (int i = 0; i < listSidxEidxTyDefHdgs.size(); i++) {
//			System.out.println("list def===" + Arrays.toString(listSidxEidxTyDefHdgs.get(i)));
			sIdx = Integer.parseInt(listSidxEidxTyDefHdgs.get(i)[0]);
			eIdx = Integer.parseInt(listSidxEidxTyDefHdgs.get(i)[1]);
//			definedTermText = defined_terms_doc.substring(sIdx, eIdx);
//			System.out.println("defined term text ="+ definedTermText);
			if (!mapWas_defined_twice.containsKey(listSidxEidxTyDefHdgs.get(i)[3])) {
				mapWas_defined_twice.put(listSidxEidxTyDefHdgs.get(i)[3], 1);
				listInt = new ArrayList<Integer>();
				listInt.add(sIdx);
				map_multi_defs_idx_loc.put(listSidxEidxTyDefHdgs.get(i)[3], listInt);
			} else {
				mapWas_defined_twice.put(listSidxEidxTyDefHdgs.get(i)[3],
						(mapWas_defined_twice.get(listSidxEidxTyDefHdgs.get(i)[3]) + 1));
				listInt = new ArrayList<Integer>();
				listInt = map_multi_defs_idx_loc.get(listSidxEidxTyDefHdgs.get(i)[3]);
				listInt.add(sIdx);
				map_multi_defs_idx_loc.put(listSidxEidxTyDefHdgs.get(i)[3], listInt);
			}
		}
		for (int i = 0; i < list_defined_in_body.size(); i++) {
			// MatchedGroups,Start, EndIdx
			if (!mapWas_defined_twice.containsKey(list_defined_in_body.get(i)[0])) {
				mapWas_defined_twice.put(list_defined_in_body.get(i)[0], 1);
				listInt = new ArrayList<Integer>();
				listInt.add(Integer.parseInt(list_defined_in_body.get(i)[1]));
				map_multi_defs_idx_loc.put(list_defined_in_body.get(i)[0], listInt);
			} else {
				mapWas_defined_twice.put(list_defined_in_body.get(i)[0],
						mapWas_defined_twice.get(list_defined_in_body.get(i)[0]) + 1);
				listInt = new ArrayList<Integer>();
				listInt = map_multi_defs_idx_loc.get(list_defined_in_body.get(i)[0]);
				listInt.add(Integer.parseInt(list_defined_in_body.get(i)[1]));
				map_multi_defs_idx_loc.put(list_defined_in_body.get(i)[0], listInt);
			}
		}
		// the map below will indicate when a definition is defined twice because the
		// map value is 2 or higher.
		for (Map.Entry<String, Integer> entry : mapWas_defined_twice.entrySet()) {
			if (entry.getValue() > 1) {
				if (!map_has_meaning_specified_in.containsKey(entry.getKey())) {
					System.out.println("you defined it twice knucklehead=" + entry.getKey());
					List<Integer> listI = map_multi_defs_idx_loc.get(entry.getKey());
//					NLP.printListOfInteger("knucklehead. its here===", listI);
					for (int c = 0; c < listI.size(); c++) {

						System.out.println("defined twice snippet=" + defined_terms_doc.substring(
								Math.max(0, listI.get(c)), Math.min(listI.get(c) + 35, defined_terms_doc.length())));

					}
				}
			}
		}

		isItGenderNeutral(originalClientDoc);// Keshav: this runs on entire clientDoc
		canIsignElectronically(clientDoc);

		String agreementIs = theVersusThis(clientDoc);// returns the Agreement's defined name as in the "Agreement" or
														// the "Escrow Agreement" if it is defined as an Agreement or
														// Escrow Agreement respectively
		// added map of exhibits in theVersusThis - so can eliminate checking once first
		// exhibit is found where it may refer to the Agreement instead of this
		// Agreement. This way I only suggest a fix when it is likely wrong.
		thePrecedesThisAgreementReference(clientDoc, agreementIs, true);
		// added map of exhibits in theVersusThis - so can eliminate checking once first
		// exhibit is found where it may refer to the Agreement instea of this
		// Agreement. This way I only suggest a fix when it is likely wrong.
		checkParagraphEnds(clientDoc);
//		CheckParaNumbering.print_this = false;
		// TODO: theVersusThis(clientDoc)
		// TODO: thePrecedesThisAgreementReference(clientDoc, agreementIs, true); revise
		// these 2 method so they can handle Notes and or other exhibits wherein
		// initially user identifies each exhibit and that exhibit is handled separately
		// but fix filter so it will allow for Notes/Certificates/Warrant etc as
		// sometimes that is the the/this docx!

		// TODO:
//		CheckParaNumbering.checkParaNumbering(Utils.readTextFromFile("c:\\temp\\temp_test.txt"));
		/*
		 * this has to be converted manually via asci link if it is a word docx b/c
		 * para# don't work when reading word docx ruleset: using list_of_para, if
		 * paraCnt chgs then it is a new para. If it is | para| then para # starts the
		 * para. If it is para=slave then para # starts para and this is an internal
		 * para #. If it is | clau| then there is no para # that starts the para and
		 * this is the first internal para #. If it is clau=slave it is preceded by a
		 * prior clau para #. See para 63 for example of temp_test.txt
		 * 
		 * TODO: IF type is | para| (para# starts paragraph == \r\n(a).....; and \r\n
		 * then capture the punctuation at the end of the paragraph! - e.g., ;
		 * ?(and|or)? ?\r\n - even if it has sub clause #s. This will help me measure
		 * consistency when para# starts a paragraph of the ending punctuation (eg ';'
		 * or ',' etc)
		 * 
		 * TODO: For -- checkParaNumbering - Capture whenever a paragraph ends with a
		 * ':' even if it is not containing a para#. This may help understand structure.
		 */

		// STOP
		// TODO: It says it is defined at section xx but it is only found to be defined
		// once and isn't defined in body (did not fine 2 instances of it visa via
		// search for term in quotes)
		// TODO: If def is defined in body and is x-ref as such -- ck if in fact it
		// exists in such section. see eg:
		// “Pledged Equity” has the meaning assigned to such term in Section 2.01.

		// TODO: when is it in error to put the quote after or before the period or the
		// comma in the context of a defined term and when not using a defined term

		// TODO:cross reference section heading helper. Tool to bring to the user
		// header/body. User can then quickly assess if cross reference section is
		// correct.

		// make sure I use the original partial credit guarantee for Bahamas. Alphabet
		// out of order, terms are defined but not used, terms are defined in the body
		// but not in the definition section. see file called C:\temp\bH_guaranty.docx

		// TO FIX: If location count of out of order def terms is more than 4 or 5
		// ignore as it picked up 2 def term sections. see c:\temp\alpha ord ex.docx
		// Each has a score of 1 to 3, with 1 being easiest for me to code.
		// TODO: see what of these can be patented
		// 2.TODO: use contractNameFinder to automate query pool.
		// contractNameFinder name cleanup and homogenized name. Create cnts by vTxtId
		// associated with each homog contractnamefinder name. Run vTxt on client docx.
		// See where a particular contactnamefinder homog name has a high percent of
		// vTxtIds in entire DB.
		// TODO: in checking whether or not terms are defined allow the user to load all
		// their documents. That way terms are defined by cross reference can be
		// checked. This should be a relatively straightforward coding exercise.
		// 1.TODO: Defined terms defined other than in the definitions section but not
		// listed in the definitions section; provided that's the format they intended
		// 1.TODO: x-ref Section main header checker: does your x-ref exists - e.g.,
		// subject to Section 4.9. yet there is no Section 4.9.
		// 2.TODO: x-ref Section sub-header checker: does your x-ref exists - e.g.,
		// subject to Section 4.9(e). yet there is no Section 4.9(e).
		// 1.TODO: did you mean to say this twice? - sim scoring after each sent solr
		// search.
		// 1. TODO: x-ref clause name fetcher. hover over 'Section 6.01' they
		// can see full name eg: Section 6.01 Duties of the Trustee.
		// 2. TODO: x-ref para txt fetcher If ref is to Section 6.01(a) it shows that
		// para if no clause name
		// 2. TODO: is x-ref right: show clause name and text most commonly associated
		// with that sentence. and see how it compares to yours. If low sim,
		// maybe it is wrong. (patent!). Would need to locate x-ref in client text.
		// 1. TODO: list sents - you didn't capitalize consistently in list sentences.
		// 1. TODO: sent/clause ends are inconsistent use of ';' or ','
		// 1. TODO: list sents: you put '; and' yet it is not penultimate
		// 2. TODO: Exhibits: referred to in the agt, but not attached (and vice versa).
		// 2. TODO: Exhibit is only a placeholder
		// 1. TODO: Are paragraph numbering in order? Are clause numbering in order
		// 1. TODO: Are headers properly capitalized (not capitalizing stop words)
		// 2. TODO: Did you refer to 'This Agreement' and not 'The Agreement'.
		// Difficulty is exhibits should refer to "The" and the Agreement "This"
		// 3. TODO: MyRL: were my changes accepted? Deal comment - retain all sentences
		// I
		// made changes to so I can check the doc that comes in. And create a report of
		// outstanding comments and/or color code my outstanding comments in deal doc.
		// TODO: Redact it tool. Any initial/all cap word or start of sentence not in
		// dictionary.
		// TODO: para formatting - sometimes they put a hard return before each numbered
		// para and sometimes they do not.
		// 2. TODO: is formatting consistent - defined terms bold when defined (or not),
		// 2. TODO: for multi docx deals terms are often defined in one docx and use in
		// another. solution: find in each docx terms not defined, and then in each docx
		// search if that term is defined, and if so remove from the list
//TODO: ck docx for []
		// TODO: Inject claus in docx format!
		// TODO: insert favorites. lists of favorite clauses. save clause
		// 2. TODO: When it says for example -- This Section 10.1 -- is it actually
		// 10.1.
		// My existing parser does this by looking w/n confirmed sections.
		// AND can we suggest changing it to: Section 10.1 (Duties of the Trustee) [with
		// suggestion this anchors x-ref should x-ref x# chg] <-------patent.

		// TODO: use repository of def to see if something should be def - and send them
		// to link of how to def. if very little results then skip.

		// 3. TODO: add amendment creator. drop in template open parent, legal terms
		// implemented. whether delete or revise, if revise then redline. Add sig page
		// based on confirmed LEs from OP.

		// 4. Check that section numbers are consecutive
		// 5. Check that the table of contents matches the section numbers
		// 6. check that headers do not capitalize stop words
		// 7. Check whether exhibits have been inserted --in print report

		// TO DO: create, separation rule
		// create simple rules to determine when commas as should be inserted and not
		// inserted. absent using a natural language parsing tool like Stanford the best
		// I can do is to determine if the clauses that are separated by paragraph
		// numbers are lengthy, and if they are to separate them by paragraphs.

		// TO DO: create rule to determine when the use of punctuation in relation to
		// quoted words correct, such as placement of the period and other punctuation
		// such as, etc. Read up to see if these rules are simple enough that they can
		// be done programmatically. Same goes for footnotes in relation thereto.

		// TODO: finding cases where there is a hard return followed by a space and then
		// start a sentence or defined. in that case, the space should be removed.

		// TODO: (.pdf) causes and erroneous end of the sentence.
		// https://www.sec.gov/Archives/edgar/data/1790930/000119312520023439/d804285dex1019.htm

		// TODO: make list of common terms that are not defined, such as Ministry, Euro,
		// etc.

		// TODO: create rule to determine if a defined term is not defined in the
		// definitions section.

		// to do: create a rule that finds terms are defined in the body the document
		// but are not listed in the defined terms section (but make sure that the
		// defined term sections are generally cross-referencing terms in the body that
		// are defined)

		// TODO:when a defined term is in the definitions section but makes a cross
		// reference to the section in the body of the contract words define HUNT FOR
		// THAT DEFINED TERM IN THE BODY then determine if IT IS IN FACT DEFINED IN THE
		// BODY AND if is defined in the body at the cross reference section referred
		// to by that defined term in the definition section.

		// inconsistent use of singular and plural in connection with lists. for
		// example: expenses, costs, loss and damages. Losses not plural. a potential
		// simple approach would be to gather lists and then to look up each list in a
		// dictionary to determine if it's in its singular or plural form. It's unclear
		// such a dictionary makes such clear designation. One open source tool out
		// there that seems to have a following is aSpell. another approach may be to
		// build the rule set to identify plural words that don't ended as and singular
		// words that do end in s. wordnet is a good source of lookup.

		// TODO: Past tense can cause f/p -- fix it. Lemma?

		// TODO: defined but you used the lower case of it -- Fix by eliminating single
		// word instances that are also in oxford's dictionary

		// TODO: inconsistent formatting of defined terms (bold in quotes, underlined,
		// etc. but not true everywhere)

		// TODO: Why is 'it isn.. defined' fooled by OANDA Corporation??

		// TODO: defined it twice f/p==“Agreement” has the meaning assigned to such term
		// in the preamble to this Agreement.//preamble - see also
		// “Collateral Agent” has the meaning assigned to such term in the preliminary
		// statement of this Agreement.
		// see also
		// f/p: “Article 9 Collateral” has the meaning assigned to such term in Section
		// 3.01(a).
		// TODO: Fix key man life re sexist

		// TODO: merge all methods into this one class to give freedom to make changes.

		// TODO: user to select the body of text proofing should apply so
		// that exhibits and/or annexes or schedules are not incorporated which would
		// then otherwise create lots of false positives. alternatively, determine where
		// the first exhibit begins and do not perform further analysis on the text that
		// follows, or to put it away only the text up to that point

		// TODO: How to capture formatting from word docx?

		// TODO: for terms that are not defined, do def="defined term" search and see
		// how
		// often it is defined and we assign confidence level. Keshav

		// TODO: What if there is a def Loan Parties and a def Dispose and I erroneously
		// pickup a term that's not defined called "Loan Party Disposes". I would want
		// to check if Loan is def and Party/s Disposes is def and if not if Loan
		// Party/s and Dispose/s is def. and if yes, term is defined. and if this is
		// found to be true, we still report but note and assign low confidence value.
		// c:/temp/loan party/dispose.docx - Keshav.

		// TODO: Sometimes a single docx may have multi agts etc where user want to run
		// our proofing tools each independently. let them identify those
		// sections.Keshav

		// TODO: Use DocxParser to parse headings and to supplement existing section
		// formatting tool.

		// TODO: add method that checks if terms are defined twice across 2 docs. 1 doc
		// is doc that carries all the definitions. the other has additional defs. see
		// if the other redundantly defines terms. Same with 'oh crap. you did not put
		// in def section

		// TODO: Search for exhibit/annex/schedule/appendix preceding a defined term
		// that is defined twice to see if 2nd def is in such exhibit. also ck location
		// of docx -- eg bottom 10%

		// TODO: If the lower case is used of a defined term see if it is defined in the
		// same sentence as its lower case use. see eg: 'demand notice' followed by its
		// definition "Demand Notice"
		// If (A) on any Determination Date, the Issuer determines that the Principal
		// Deficit Amount on the next succeeding Payment Date (after giving effect to
		// any draws on the Series 2021-1 Letters of Credit on such Payment Date
		// pursuant to Section 5.5(b)) will be greater than zero or (B) on the
		// Determination Date related to the Legal Final Payment Date, the Issuer
		// determines that the Series 2021-1 Principal Amount exceeds the amount to be
		// deposited into the Series 2021-1 Distribution Account (other than as a result
		// of this Section 5.5(c)) on the Legal Final Payment Date for payment of
		// principal of the Series 2021-1 Notes, then, prior to [10:00 a.m.] (New York
		// City time) on the [second (2nd)] Business Day prior to such Payment Date, the
		// Issuer shall instruct (provided that if the Issuer fails to so instruct the
		// Trustee then the Program Agent may instruct the Trustee) the Trustee in
		// writing (and provide the requisite information to the Trustee) to deliver a
		// demand notice substantially in the form of Exhibit B-2 (each a “Demand
		// Notice”) on Fox for payment under the Series 2021-1 Demand Note in an amount
		// equal to the lesser of (i) (x) on any such Determination Date related to a
		// Payment Date other than the Legal Final Payment Date, the Principal Deficit
		// Amount less the amount to be deposited into the Series 2021-1 Principal
		// Collection Account in accordance with Sections 5.4(b) and Section 5.5(b) and
		// (y) on the Determination Date related to the Legal Final Payment Date, the
		// excess, if any, of the Series 2021-1 Principal Amount over the amount to be
		// deposited into the Series 2021-1 Distribution Account (together with any
		// amounts to be deposited therein pursuant to the terms of this Series 2021-1
		// Supplement (other than this Section 5.5(c))) on the Legal Final Payment Date
		// for payment of principal of the Series 2021-1 Notes, and (ii) the principal
		// amount of the Series 2021-1 Demand Note.

		// TODO: If there are defined term lists in two docs, I need to run some of the
		// tools separately on the primary (one in the docx) such are they alphabetical;
		// defined but used lower case and so on. Go through each method and make sure
		// both sets of defined terms are passed through each method as appropriate. One
		// simply approach is to run the methods twice, once w/ separate defined terms
		// docx and once without. The one case that wouldn't work is 'it isn't defined'

		// TODO: F/P when it thinks lower case is used but it is immediately followed by
		// a defined term with such lower case words in it. though 'financing source'
		// was incorrectly lower case see:
		// as the new financing source (the “New Financing Source”) and [_], as the new
		// beneficiary (the “New Beneficiary”), does hereby make, constitute and appoint
		// [the Collateral Servicer, each Sub-Collateral-Servicer] its true and lawful
		// Attorney(s)-in-Fact for it and in its name, stead and behalf to execute any
		// and all documents and instruments with find when stop word is capitalized and
		// it should not be in a hdg. apply hdg format to def terms

		// fix issue with docx reader not getting paragraph headers -Section

		// TODO: see readWord_temp1010.docx -- "Monthly Administration Fee" is defined
		// twice but not picked up.

		// TODO: see Out of order.docx -- Holder and GAAP out of order but not picked
		// up.

		// TODO: create pattern matcher to find if in a docx it says def terms are
		// defined in another docx and use that as a way to also prompt user to load
		// such docx if they utilize def proof tools.

		// TODO: Bug - this is picked up as not defined "8% Secured Senior Notes Due
		// 2019" b/c it is doesn't realize 8% is part of S/S/N Due..

		// BUG: Not in Def section but in body of contract doesn't work when plural
		// version is body and singular version is in def section.

		// BUG: when in def section via cross ref to section in docx the proofing tool
		// defined but not used does not work b/c it sees it twice when really both
		// instances are reference to defined term being defined and there is no
		// instance where it is used as a newly defined term

		// TODO: See in c:\temp\bug . . . docx for bugs to fix in various proofing
		// tools.

		// see c:/temp/section 511 heading is skipped.html -- getSections - sees there
		// are 2 section 512 and no section 511 - see if I can make this a tool

//		 TODO: Fix "Not Defined": First: to the Servicer, any unreimbursed Servicer
//		 Advances, as defined in Section 3.6 of the Sale and Servicing Agreement, in
//		 respect of a prior Collection Period;
		System.out.println("did it work: " + "Hellow Word Time".replaceAll("([A-Z]+[a-z]+ ?)+", "").trim());
	}
}
