package contracts;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import xbrl.NLP;
import xbrl.Utils;

public class Alphabetical {

	public static List<String[]> listNotInOrder = new ArrayList<String[]>();
	public static List<String[]> listOutOfOrder = new ArrayList<String[]>();

	public static boolean alphaMethod = false;

	public static boolean outOfOrderBecauseOfSpace(String wordone, String wordtwo) {
		boolean isalphabetical = true;
		String ltrone, ltrtwo = null, replChar_not_for_alpha = "\\d,;\\- \\.:\\)\\('\"\\&";
//		boolean hasP = false;
//		if (wordone.contains(".") || wordtwo.contains(".")) {
//			hasP = true;
//			System.out.println("wordone=" + wordone + " wordtwo=" + wordtwo);
//		}
//		System.out.println("wordone.len=" + wordone.length() + " wordtwo.len=" + wordtwo.length());

		for (int i = 0; i < wordone.length(); i++) {
//			System.out.println("i=="+i);
			ltrone = wordone.substring(i, i + 1);
//			if (hasP) {
//				System.out.println("ltrone=" + ltrone);
//			}

			if (i + 1 > wordtwo.length() && ltrone.replaceAll("[" + replChar_not_for_alpha + "]", "").length() == 0) {
//				if (hasP) {
//					System.out.println("1.");
//				}
				return true;
			}

			if (i + 1 > wordtwo.length()) {
//				if (hasP) {
//					System.out.println("2.");
//				}
				return false;
			}

			ltrtwo = wordtwo.substring(i, i + 1);
			// if (hasP) {
			// System.out.println("ltrtwo=" + ltrtwo);
			// }

			if (!ltrone.equals(ltrtwo) && ltrone.replaceAll("[" + replChar_not_for_alpha + "]+", "").length() > 0
					&& ltrtwo.replaceAll("[" + replChar_not_for_alpha + "]+", "").length() > 0) {
				// if (hasP) {
				// MacySystem.out.println("3.");
				// }
				return false;

			}

			if (!ltrone.equals(ltrtwo) && (ltrone.replaceAll("[" + replChar_not_for_alpha + "]+", "").length() == 0
					|| ltrtwo.replaceAll("[" + replChar_not_for_alpha + "]+", "").length() == 0)) {
//				if (hasP) {
//					System.out.println("4.");
//				}

				return true;
			}
		}

		return isalphabetical;
	}

	public static List<String[]> areDefinedTermsAlphabetical(List<String[]> list, String text) {
		// convert each defined word to digits. iterate over list and see if digits are
		// in ascending order.
		String definedTerm = "", pDefinedTerm;
//		int ltr1, ltr2, ltr3, ltr4, ltr5, ltr6, ltr7, ltr8, ltr9, ltr10, pLtr1, pLtr2, pLtr3, pLtr4, pLtr5, pLtr6,
//				pLtr7, pLtr8, pLtr9, pLtr10;
		/*
		 * a much simplier way to do this is to assign each letter a value of 10 to 99.
		 * (Numbers should be assigned negative value of -1 to -99 - starting with -99
		 * and working back to -1. With 0=-98 and 1=-97). White space is always the
		 * smallest value (-99). If for example you have the defined term Paying Agent
		 * and the defined term Payee you would assign the following values.
		 * 
		 * 27_10_35_19_24_17_00_10_17_15_16_24_30 (don't use underscore of course) and
		 * for Payee it it 27_10_35_15_15 Now append to each value X number of zeros so
		 * that they are of equal length. So if the defined term if 30 characters
		 * (including white spaces) then the number of digits will be 60. If we set a
		 * max length of lets say 40 characters we would take the first 80 digits and
		 * save it as a string. Next if we have a defined term that is only 10
		 * characters - we take those 20 digits and append to it 60 zeros. These are all
		 * put into a map with they key being the string of 80 length digits. The value
		 * is the defined term and its sequence (1st or 2nd defined terms etc). If the
		 * sequence of the list is no consecutive across keys you know the first
		 * instance it occurs it is out of order. I do not know of java maps order
		 * defined terms correctly. Lower case and upper case words are treated the
		 * same.
		 */
//		TreeMap<String, String[]> mapDefinedTermsAlgoOrdered_array = new TreeMap<String, String[]>();
		// TODO-Keshav can use this map Above instead of the one below in order to
		// retain the original defined term capitalization
		TreeMap<String, String[]> mapDefinedTermsAlgoOrdered = new TreeMap<String, String[]>();
//		TreeMap<Integer, String> mapDefinedTermsDocOrder = new TreeMap<Integer, String>();

//		String repl = ";,-";

		for (int i = 0; i < list.size(); i++) {
			System.out.println("list, ary=" + Arrays.toString(list.get(i)));
			definedTerm = list.get(i)[3];
//			.replaceAll("[" + repl + "]+", "aAa").replaceAll("[ ]+", " ").trim()
//					.toLowerCase();
			definedTerm = definedTerm.replaceAll("[\"\r\n]", "")
//					.replaceAll("[ ]+", "aaaaa")
			;
//			ltr1 = GetContracts.getNumberInAlphabet(definedTerm.substring(0, 1));

			String[] ary = { i + "", list.get(i)[0] + "" };
			mapDefinedTermsAlgoOrdered.put(definedTerm.toLowerCase(), ary);// todo: chg to ary so I can retain the
																			// original def term
		}

//		NLP.printMapStrInt("ifd====", mapDefinedTermsAlgoOrdered);

		int val = 0, valPrior = 0;
		String word, wordprior = null, snippet = "";
		int i = 0, sIdx = 0;
		boolean nospace = true;
		// TODO : put in list -- and if out-of-order
		for (Map.Entry<String, String[]> entry : mapDefinedTermsAlgoOrdered.entrySet()) {
			val = Integer.parseInt(entry.getValue()[0]);
			sIdx = Integer.parseInt(entry.getValue()[1]);
			word = entry.getKey();
			if (i > 0) {
				nospace = outOfOrderBecauseOfSpace(word, wordprior);
//				System.out.println("nospace=" + nospace);
			}

			i++;

			if (val - valPrior < 1 && valPrior > 0 && !nospace) {
//
//				System.out.println("out of order key =" + val + " def=" + word + " \nshould be after def=" + wordprior
//						+ " key prior=" + valPrior);

				snippet = text.substring(sIdx, Math.min(sIdx + 50, text.length()));
				System.out.println("\"" + word + "\" is out of order, it should be after: \"" + wordprior + "\"");
				String[] ary = { "\"" + word + "\" is out of order", ", it should be after: \"" + wordprior + "\"",
						" snippet: " + snippet };
				listOutOfOrder.add(ary);
				// Use the nospace method to address out of order results due to '.'
			}
			valPrior = val;
			wordprior = word;
		}

		return list;
	}

	public static TreeMap<Integer, List<String[]>> get_multi_definition_sections(List<String[]> listStrAry,
			int distance) {
		NLP nlp = new NLP();
		Pattern pattern_Annex = Pattern.compile("(Annex|ANNEX|Appendix|APPENDIX) [A-Z]");
		int sidx = 0, eidxP = 0, cnt = 0;
		List<String[]> list = new ArrayList<String[]>();
		TreeMap<Integer, List<String[]>> map = new TreeMap<Integer, List<String[]>>();
		String letter = "", letter_next = null, word = "";
		int letter_no = 0, letter_next_no = 0;
		String[] array = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
				"S", "T", "U", "V", "W", "X", "Y", "Z" };

		for (int i = 0; i < listStrAry.size(); i++) {
			word = listStrAry.get(i)[3].trim();
			if (cnt == 0 && nlp.getAllIndexStartLocations(word, pattern_Annex).size() > 0) {
				System.out.println("appendix/Annex. Word=" + word);
				continue;
			}

			sidx = Integer.parseInt(listStrAry.get(i)[0]);
			if (i > 0) {
				eidxP = Integer.parseInt(listStrAry.get(i - 1)[1]);
			}

			letter = listStrAry.get(i)[3].trim().substring(0, 1).toUpperCase();
			if (i + 1 < listStrAry.size()) {
				letter_next = listStrAry.get(i + 1)[3].trim().substring(0, 1).toUpperCase();
			}

			if (i + 1 < listStrAry.size()) {
				for (int c = 0; c < array.length; c++) {
					if (letter.equals(array[c])) {
						letter_no = c;
					}
					if (letter_next.equals(array[c])) {
						letter_next_no = c;
					}
				}
			}

			if (letter_no - letter_next_no > 7) {

//				System.out.println("letter, pr-break,ltr==" + letter + " " + Arrays.toString(listStrAry.get(i)));
//				System.out.println("" + "letter, new def sec,ltr_next==" + letter_next + " "
//						+ Arrays.toString(listStrAry.get(i + 1)));
				list.add(listStrAry.get(i));
				map.put(i, list);
				list = new ArrayList<String[]>();
				cnt = 0;
				continue;
			}

			if (sidx - eidxP > distance && i > 0) {
//				System.out.println("pr-break==" + Arrays.toString(listStrAry.get(i - 1)));
				System.out.println("this is the start of a new def sec"// + Arrays.toString(listStrAry.get(i))
				);
				map.put(i, list);
				list = new ArrayList<String[]>();
				cnt = 0;
				continue;
			}
			cnt++;
			list.add(listStrAry.get(i));

		}

		map.put(listStrAry.size(), list);

		return map;
	}

	public static void main(String[] args) throws IOException, SQLException, ParseException {

		// use this to create source code to then give to Keshav.

		NLP nlp = new NLP();
		GoLaw gl = new GoLaw();

		alphaMethod = true;// always set to true;
		String clientDoc, defined_terms = null;
//		Utils.readWordDocXFile("F:\\Perkins_Matters\\SecurityLife\\Citadel - Trust Agreement.docx");
		boolean areDefinedTermsInThisDoc = true;// if false ask them to upload the doc with the defined terms.
//		clientDoc = Utils.readTextFromFile("C:/temp/temp1010.txt");// this works w/ getSectionsMapped
		clientDoc = Utils.readWordDocXFile("c:\\temp\\test2.docx");
		clientDoc = gl.stripHtml(clientDoc);
		clientDoc = clientDoc.replaceAll("\r\n", "\r\n\r\n").replaceAll("(?<=\r\n)(((?ism)section )? ?)[\\d\\.]+ ", "")
				.replaceAll("(?ism)xxPD", "\\.").replaceAll("\t", "  ").replaceAll("�", "\"").replaceAll("”|“", "\"")
				.replaceAll("ï¿½", "\"").replaceAll("â€?|â€œ", "\"");

//				.replaceAll("\r\n", "\r\n\r\n").replaceAll("�", "\"").replaceAll("”|“", "\"")
//				.replaceAll("(?<=\r\n)(((?ism)section )? ?)[\\d\\.]+ ", "").replaceAll("(?ism)xxPD", "\\.")
//				.replaceAll("\t", "  ");

		if (!areDefinedTermsInThisDoc) {
			// ask the user if DEFINED TERMS are in a separate document if true the user
			// will upload the defined terms, if false then they don't do anything
			System.out.println("defined terms are in this doc. user uploads docx with defined terms");
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
		// keshav: alpha method starts here.
		String defined_terms_doc = clientDoc;
		if (!areDefinedTermsInThisDoc) {
			System.out.println("defined terms are in this other docx");
			defined_terms_doc = defined_terms;
		}

//		System.out.println(defined_terms_doc);
		List<String[]> listSidxEidxTypeDefHdgsTmp = SolrPrep.getDefinitions_for_alphabetical(defined_terms_doc);

//		NLP.printListOfStringArray("listSidxEidxTypeDefHdgsTmp", listSidxEidxTypeDefHdgsTmp);

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
		TreeMap<Integer, List<String[]>> map_multi_defs = get_multi_definition_sections(listSidxEidxTyDefHdgs, 20000);

//		NLP.printMapIntListOfStringAry("multi def??", map_multi_defs);

//		System.out.println("are they alphabetical?");
		for (Map.Entry<Integer, List<String[]>> entry : map_multi_defs.entrySet()) {
			listSidxEidxTyDefHdgs = areDefinedTermsAlphabetical(entry.getValue(), defined_terms_doc);
//			NLP.printListOfStringArray("key = "+ entry.getKey()+ "  new key is new  definition section ==", entry.getValue());
		}
		// TODO: need to fix getDefinitions to ignore ridiculously out-of-order
		// definitions.
		// eg: Payment Date then Accrual. My code doesn't pick it up b/c they are so out
		// of order (not by a letter or two). So can just do the very prelim list. Run
		// over it and see how many are consecutive. The issue happens in the creation
		// of: listpreLimDef

		NLP.printListOfStringArray("listNotInOrder===", listNotInOrder);
		// each list returns definition that is out of order, this one does not specify
		// how to fix it.
		NLP.printListOfStringArray("listOutOfOrder===", listOutOfOrder);// this list specfies how to fix it.
		// changed code in SolrPrep. Added snippet to jump to.

		TreeMap<String, String[]> mapOutOfOrder = new TreeMap<String, String[]>();
		for (int i = 0; i < listOutOfOrder.size(); i++) {
			mapOutOfOrder.put(listOutOfOrder.get(i)[0], listOutOfOrder.get(i));
		}

		for (int i = 0; i < listNotInOrder.size(); i++) {
			mapOutOfOrder.put(listNotInOrder.get(i)[0], listNotInOrder.get(i));
		}

		for (Map.Entry<String, String[]> entry : mapOutOfOrder.entrySet()) {
			System.out.println(entry.getValue()[0] + entry.getValue()[1] + entry.getValue()[2]);// final list with
																								// snippets to jump to.
		}
		// TODO: See how I fixed getDefinitions for parsing to pickup on def terms that
		// are severly out of order and brief. If def terms are back to back (para to
		// para) -- eg patterns =\r\n\".{1,90}\".{1,30}mean[ings]\"... then we know they
		// are def terms. I don't need to to do any further coding as my algo pickups
		// when back to back via idx measure.
	}
}
