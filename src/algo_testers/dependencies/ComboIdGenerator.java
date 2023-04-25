package algo_testers.dependencies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import contracts.GoLaw;
import xbrl.NLP;


public class ComboIdGenerator {

	protected static Log log = LogFactory.getLog(ComboIdGenerator.class);
	
	protected static NLP nlp = new NLP();
	protected static GoLaw glaw = new GoLaw();
	
	
	public static final int ComboIdPatternSequenceIncrement = 10;

	public static final String solrField_comboId = "comboId";
	public static final String solrField_comboOrdId = "comboOrdId";
	public static final String solrField_comboOrdCdnId = "comboOrdCdnId";
	
	private Map<String, String> comboName2Id = new HashMap<>();

	public Map<String, String>  generateComboIds(String text, List<String> patterns) throws IOException {
		// there are 3 forms of comboId - the one I use principally is comboOrdCdnId.
		//comboId = natural comboId. 100200200100300
		// comboOrd = putting the combinations in ascending order. 100100200200300 
		// comboOrdCdn = putting the combinations in ascending order but over-writing (unique numbers)
		// duplicates. To go in order - I think that is just 'sorting'. To remove
		// duplicates use a map or something else. 100200300
		List<Integer> combos = getComboSequences(text, patterns);
		//comboId = 100200200100300 - occurrences of patterns 'in order' of their location in text
		String comboId = StringUtils.join(combos, "");
		Collections.sort(combos);
		// comboOrdId = sorted patterns' indexes
		String comboOrdId = StringUtils.join(combos,"");
		Set<Integer> uniqCombos = new TreeSet<>(combos);
		// comboOrdCdnId = consolidated combo id:- indexes of unique patterns found
		String comboOrdCdnId = StringUtils.join(uniqCombos,"");
		comboName2Id.clear();
		comboName2Id.put(solrField_comboId, comboId);
		comboName2Id.put(solrField_comboOrdId, comboOrdId);
		comboName2Id.put(solrField_comboOrdCdnId, comboOrdCdnId);
		return comboName2Id;
	}
	
	public String generateComboId(String text, List<String> patterns) throws IOException {
		generateComboIds(text, patterns);
		return getComboId();
	}
	/**
	 * Ensure 'generateComboIds' method is called before calling this.
	 * @return
	 */
	public String getComboId() {
		return comboName2Id.get(solrField_comboId);
	}
	
	
	/**
	 * 
	 * @param text
	 * @param closeWords
	 * @param withinWordCount
	 * @return
	 */
	//TODO: should the stop-words be ignored/removed while looking for words in proximity?
	public List<Integer> getProximityWordsIndexes(String text, String closeWords, int withinWordCount) {
		String[] textWs = text.replaceAll("[ \\s]+", " ").trim().split(" ");
		String[] Ws = closeWords.replaceAll("[ \\s]+", " ").trim().split(" ");
		List<Integer> w1Idxs = new ArrayList<>();
		String w1 = Ws[0];
		for (int i=0; i < textWs.length; i++) {
			if (textWs[i].startsWith(w1)) {			// TODO: startsWith, or equal??
				w1Idxs.add(i);
			}
		}
		// first word itself was not found anywhere
		if (w1Idxs.size() == 0)
			return null;
		
		// there may be multiple occurrences of W1. We should check for each location
		List<Integer> wordsIdxs = new ArrayList<>();
		int sIdx, eIdx;
		String[] twPart;
		boolean wFound = false;
		for (int start : w1Idxs) {
			// look at left side
			sIdx = Math.max(0, start - withinWordCount);
			eIdx = start+1;			// since eIdx is exclusive in subArray
			wordsIdxs.clear();
			
			twPart = ArrayUtils.subarray(textWs, sIdx, eIdx);
			for (String w : Ws) {
				wFound = false;
				for (int i=0; i < twPart.length; i++) {
					if (twPart[i].startsWith(w)) {
						wordsIdxs.add(i + sIdx);
						wFound = true;
						break;
					}
				}
				if (! wFound)
					break;
			}
			if (wordsIdxs.size() == Ws.length)
				return wordsIdxs;
			
			// look at right side
			wordsIdxs.clear();
			sIdx = start;
			eIdx = Math.min(start + withinWordCount, textWs.length);
			twPart = ArrayUtils.subarray(textWs, sIdx, eIdx);
			for (String w : Ws) {
				wFound = false;
				for (int i=0; i < twPart.length; i++) {
					if (twPart[i].startsWith(w)) {
						wordsIdxs.add(i + sIdx);
						wFound = true;
						break;
					}
				}
				if (! wFound)
					break;
			}
			if (wordsIdxs.size() == Ws.length)
				return wordsIdxs;
		}
		if (wordsIdxs.size() == Ws.length)
			return wordsIdxs;
		return null;
	}
	
	
	
	// *********************************************
	
	/**
	 * This method generates the actual combo sequence for each individual pattern (ie 100, 120, ... etc).
	 * @param text
	 * @param patterns
	 * @return
	 * @throws IOException 
	 */
	public List<Integer> getComboSequences(String text, List<String> patterns) throws IOException {
		if (null == patterns  ||  patterns.size() == 0)
			return new ArrayList<Integer>();
		Map<Integer, Integer> idx2PMap = new TreeMap<>();
		int id;
		for (int i=0; i < patterns.size(); i++) {
			List<Integer> list = getAllIndexLocationsOfPattern(text, patterns.get(i));
			if (null == list  ||  list.size() == 0)
				continue;
			id = getPatternSequenceId(i);
			for (Integer idx : list)
				idx2PMap.put(idx, id);
		}
		List<Integer> combos = new ArrayList<>(idx2PMap.values());
		return combos;
	}
	
	public int getPatternSequenceId(int patternOrderNumber) {
		return 100 + (patternOrderNumber * ComboIdPatternSequenceIncrement);		//ie   100+(i*10)
	}
	
	
	/**
	 * Returns start-index locations of the pattern in the given text. The pattern may have
	 * 	- AND :- indicating all words separated by AND must be present, regardless of each word's location/order of presence
	 * 	- "..."n	:- indicating proximity condition to be satisfied by words within ""
	 * 	- "...."	:- phrase match
	 * 
	 * We may have a combination of words/phrase/proximity in AND'd and OR'd type patterns. 
	 * However, it is assumed that a pattern will be either of type AND or OR (typically separated by space), and not mixed.
	 * ie 
	 * 		punitive AND consequential AND "gross negligence" AND "grossly negligence"~7
	 * 		misfeasance except "not liable" "no liability" "not responsible" "no responsibility" "neither liable"~7 "nor liable"~7 
	 * 		
	 * @param text
	 * @param pattern
	 * @return
	 * @throws IOException 
	 */
	public List<Integer> getAllIndexLocationsOfPattern(String text, String patternStr)  {
		String pattern = patternStr.trim();
		Set<Integer> idxLocs = new TreeSet<>();
		List<Integer> idxs;
		Integer idx;
		if (pattern.contains(" AND ")) {
			String[] parts = pattern.replaceAll("[ \\s]+", " ").trim().split(" AND ");
			// each part may be either a word, phrase or proximity type
			List<String> words = new ArrayList<>();
			List<String> phrases = new ArrayList<>();
			for (String part : parts) {
				if (part.startsWith("\"")) {
					phrases.add(part);
				} else {
					words.add(part);
				}
			}
			if (words.size() > 0) {
				idxs = getAllWordsIndexLocations(text, words);
				if (null == idxs  ||  idxs.size() != words.size())
					return null;
				idxLocs.addAll(idxs);
			}
			if (phrases.size() > 0) {
				for (String part : phrases) {
					if (part.matches("^\".+?\".*?\\d+$")) {
						// proximity match
						idxs = getProximityIndexLocations(text, part);
						if (null == idxs)		// this proximity was not found, AND condition is not satisfied
							return null;
						idxLocs.addAll(idxs);
					} else {
						// phrase match
						idx = getPhraseIndexLocation(text, part);
						if (null == idx  ||  idx < 0)		// this phrase was not found, AND condition is not satisfied
							return null;
						idxLocs.add(idx);
					}
				}
			}
		} else {
			// its OR conditions
			List<String> phrases = getAllMatchedGroups(pattern, Pattern.compile("(\".+?\"[^ ]*)"));
			for (String ph : phrases)
				pattern = pattern.replace(ph, "").trim();
			if (StringUtils.isNotBlank(pattern)) {
				String[] words = pattern.replaceAll("[ \\s]+", " ").split(" ");
				idx=-1;
				for (String w : words) {
					if (StringUtils.isBlank(w))
						continue;
					idx = text.indexOf(w, idx+1);
					while (idx >= 0) {		// if word has multiple occurrences
						idxLocs.add(idx);
						idx = text.indexOf(w, idx+1);
					}
				}
			}
			if (phrases.size() > 0) {
				for (String part : phrases) {
					if (part.matches("^\".+?\".*?\\d+$")) {
						// proximity match
						idxs = getProximityIndexLocations(text, part);
						if (null != idxs)
							idxLocs.addAll(idxs);
					} else {
						// phrase match
						idx = getPhraseIndexLocation(text, part);
						if (null != idx  &&  idx >= 0)
							idxLocs.add(idx);
					}
				}
			}
		}
		List<Integer>resp = new ArrayList<Integer>(idxLocs);
		Collections.sort(resp);
		return resp;
	}
	
	private List<Integer> getAllWordsIndexLocations(String text, List<String> words) {
		StringBuilder sb = new StringBuilder();
		String stW;
		// ALL words	::-- james AND jacob AND jack AND julie :=  (?=.*\\b(jam[a-z]{0,15})\\b)(?=.*\\b(jac[a-z]{0,15})\\b)(?=.*\\b(jul[a-z]{0,15})\\b)
		for (String w : words) {
			// FIXME: 28-Dec-20: toLowerCase() is temporary: sometime attr word starts with upper letter and will be discarded in stemming. what to do otherwise?
			stW = glaw.goLawGetHtxt(w.toLowerCase());
			if (StringUtils.isBlank(stW))
				continue;
			stW = w.substring(0, stW.length());
			sb.append("(?=.*\\b(");
			sb.append(stW);
			sb.append("[a-z]{0,15})\\b)");
		}
		
		String ptrnStr = sb.toString();
		if (ptrnStr.length() == 0) {
			if (log.isInfoEnabled())
				log.info("pattern words were AND'ed but no regex could be made! ::" + words);
			return null;
		}
		if (log.isDebugEnabled())
			log.debug("pattern words were AND'ed and the regex is ::" + ptrnStr);
		Pattern ptrn = Pattern.compile(ptrnStr);
//		Map<Integer, String> idxGrups = com.segemai.legal.core.utils.StringUtils.getAllMatchedLocationsAndGroups(text, ptrn);
		Map<Integer, String> idxGrups = nlp.getStartLocsAndMatchedGroups(text, ptrn, null);
		if (idxGrups.size() != words.size()) {		// if not all words are found, discard this pattern
			if (log.isDebugEnabled())
				log.debug("pattern words were AND'ed but not all words were found! pattern words:" + words + "  ::  words found:" + idxGrups);
			return null;
		}
		return new ArrayList<Integer>(idxGrups.keySet());
	}
	
	private Integer getPhraseIndexLocation(String text, String phrase) {
		if (phrase.startsWith("\""))
			phrase = phrase.substring(1);
		if (phrase.endsWith("\""))
			phrase = phrase.substring(0, phrase.length()-1);
		return text.indexOf(phrase);
	}
	private Integer getPhraseIndexLocation_Htxt(String text, String phrase) {
		String[] srcWords = phrase.replaceAll("[\\p{Punct}}]", "").split("[ ]+");
		List<String[]> htw2SrcW = new ArrayList<>();
		for (String phW : phrase.split(" ")) {
			String htW = glaw.goLawGetHtxt(phW);
			if (StringUtils.isNotBlank(htW)) {
				htw2SrcW.add(new String[] {htW, phW});
			}
		}
		
//		List<KeyValuePair<String, String>> htw2SrcW = Stemmer.removeDefsStopwordsStem_IncludeSourceWord(phrase);
//		Stemmer.ensureSourceWordStartsWithStemmedWord(htw2SrcW);
//		KeyValuePair<String, String> stem;
		
		String ht = "";
		String[] stem;
		for (int kvi=0; kvi < htw2SrcW.size(); kvi++) {
			stem = htw2SrcW.get(kvi);
			// this stem word should match from start of source words, 
			// else pick source words (since they did not survive hTxt) till the kv value is same as word
			for (; srcWords.length > 0;) {
				if (stem[1].equals(srcWords[0]))
					break;
				// this srcWord was lost in hTxt - pick it
				ht += " "+srcWords[0];
				srcWords = ArrayUtils.remove(srcWords, 0);
			}
			// process
			if (StringUtils.isBlank(stem[0]))
				ht += " "+stem[1];
			else
				ht += " "+stem[0];
			if (srcWords.length > 0)		// since the equal word is processed, remove from src-W list
				srcWords = ArrayUtils.remove(srcWords, 0);
		}
		ht = ht.trim();
		if (StringUtils.isBlank(ht))
			return null;
		int lenBef = phrase.length();
		int lenAft = ht.length();
		// "\"responsible or liable\"" => "resp.{1,20}liab.*?[ ,;:\.]{1}"
		ht = ht.replaceAll(" ", ".{0," +Math.max( (lenBef - lenAft)+2, 10)+ "}");
			// generally only adding 10 is more than enough to get to the end of a word
			// starting with the stem. But if we remove stopwords, we need to adjust the
			// distance to find the next word based on the stop words removed.

		String ptrn = "\\b(("+ht+")[a-z]{0,15})\\b";
		NLP nlp = new NLP();
		List<Integer> list = nlp.getAllIndexStartLocations(text, Pattern.compile("((?sm)"+ptrn+")"));
		if (null != list  &&  list.size() > 0)
			return list.get(0);
		return null;
	}
	
	private List<Integer> getProximityIndexLocations(String text, String proximityWordsPattern) {
		String str = StringUtils.substringBetween(proximityWordsPattern, "\"").replaceAll("[ \\s]+", " ").trim();	// close words
		int withinW = Integer.parseInt( proximityWordsPattern.substring(proximityWordsPattern.lastIndexOf("\"")).replaceAll("[^\\d]", "") );
		List<Integer> idxs = getProximityWordsIndexes(text, str, withinW);
		// above idxs are indexes of words, not the char-indices
		if (null != idxs) {
			String[] words = text.replaceAll("[ \\s]+", " ").trim().split("[ \\s]+");
			Collections.sort(idxs);
			int cIdx = 0, w=0;
			for (int i=0; i < idxs.size(); i++) {
				for (; w < idxs.get(i); w++) {
					cIdx += words[w].length() + 1;
				}
				idxs.set(i, cIdx);
			}
		}
		return idxs;
	}

	protected List<String> getAllMatchedGroups(String text, Pattern pattern) {
		try {
			return new NLP().getAllMatchedGroups(text, pattern);
		} catch (IOException e) {
			return null;
		}
	}

	

	
	
	
//	public static void main(String[] arg) {
//		ComboIdGenerator cg = new ComboIdGenerator();
//		String text = "Neither the trustee or the collateral agent shall Neither be and all liable for consequential damages or loss of profits";
//
//		text="The Trustee shall not be deemed to have notice of any Default or Event of Default unless a Responsible Officer of the Trustee has actual knowledge thereof or unless written notice of any event which is in fact such a Default is received by the Trustee at the Corporate Trust Office of the Trustee, and such notice references the existence of a Default or Event of Default, the Notes and this Indenture";
//		String pattern = "\"Responsible Officer\" \"Trust Officer\"";
//		List<Integer> idxs = cg.getAllIndexLocationsOfPattern(text, pattern);
//		System.out.println(idxs);
//		
//		pattern = "\"reference Default Indenture Notes\"6 \"references Default Indenture Notes\"6 \"reference Default Indenture Securities\"6 \"references Default Indenture Securities\"6";
//		idxs = cg.getAllIndexLocationsOfPattern(text, pattern);
//		System.out.println(idxs);
//		
//		pattern = "\"trust office\" \"Trust Office\"";
//		idxs = cg.getAllIndexLocationsOfPattern(text, pattern);
//		System.out.println(idxs);
//	}
	
}
