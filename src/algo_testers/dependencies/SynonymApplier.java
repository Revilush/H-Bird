package algo_testers.dependencies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;

import algo_testers.dependencies.TextDiffHelper.DiffDetail;
import algo_testers.dependencies.TextDiffHelper.DiffOperation;
import charting.FileSystemUtils;
import charting.JSonUtils;

public class SynonymApplier {
	protected static Log log = LogFactory.getLog(SynonymApplier.class);
	
	// [ ["agents? or attorneys?", "attorneys? or agents?"], ["Officer's", "Officers'"], ["Trustee", "(Indenture |Noteholder )Trustee"], .... ]
	private List<List<String>> synonyms = new ArrayList<>();
	
	// ---- required for internal working ------ //
	//private Map<String, String> synWord2SynIdMap = new HashMap<>();		// <Trustee = __sy_0__2>	....
	//private Map<String, String[]> synListId2SynListMap = new HashMap<>();		// <__sy_0 = ["agents? or attorneys?", "attorneys? or agents?"]>	....
	
	// <__sy_0 = {agents? or attorneys?|attorneys? or agents?"}>	....
	private Map<String, Pattern> synListId2ListPatternMap = new HashMap<>();
	// <{"Trustee|(Indenture |Noteholder )Trustee"} = Trustee>	....
	private Map<String, String> synPattern2CommonSynMap = new HashMap<>();
	
	@JsonIgnore
	private Synonymizer synonymizer1;
	@JsonIgnore
	private Synonymizer synonymizer2;
	
	public SynonymApplier() {}
	public SynonymApplier(List<List<String>> synonyms) {
		this.synonyms = synonyms;
		makeSynWord2SynIdMap();
	}
	
	public SynonymApplier setSynonymsList(List<List<String>> synonyms) {
		if (null == synonyms  ||  synonyms.size() == 0)
			return this;
		this.synonyms = new ArrayList<>(SynonymFilter.cleanSynonymsList4Java(synonyms, (String[])null));
		makeSynWord2SynIdMap();
		return this;
	}
	public SynonymApplier setSynonymsArray(List<String[]> synonyms) {
		if (null == synonyms  ||  synonyms.size() == 0)
			return this;
		List<String[]> synonymsArray = SynonymFilter.cleanSynonymsArray4Java(synonyms, (String[])null);
		this.synonyms = new ArrayList<>();
		for (String[] syns : synonymsArray) {
			this.synonyms.add(Arrays.asList(syns));
		}
		makeSynWord2SynIdMap();
		return this;
	}
	
	public List<List<String>> getSynonyms() {
		return synonyms;
	}
	
	/**
	 * It replaces each synonym occurring in the given text with a common word of that synonym-list. This is repeated for all the synonym-lists. 
	 * This process produces a text that can not be re-stored (i.e. un-apply synonyms) to its original words since they are lost (replaced with a common word).
	 * This may be helpful in some processing where syn'd text produces better results than the original text.
	 * @param text
	 * @return
	 */
	public String getOneWaySynonymizedText(String text) {
		String resp = text;
		for (String ptrn : synPattern2CommonSynMap.keySet()) {
			resp = resp.replaceAll(ptrn, synPattern2CommonSynMap.get(ptrn));
		}
		return resp;
	}
	
	public String[] applySynonyms(String text1, String text2) {
		synonymizer1 = new Synonymizer();
		synonymizer1.applySynonymsTo(text1);
		
		synonymizer2 = new Synonymizer();
		synonymizer2.applySynonymsTo(text2);
		
		return new String[]{synonymizer1.synonymizedText, synonymizer2.synonymizedText};
	}
	
	public List<DiffDetail> unApplySynonyms(List<DiffDetail> diffs) {
		// un-apply synonyms, ie restore original words, based on the snippet is common/deleted/inserted.
		String orgTxt;
		for (DiffDetail dif : diffs) {
			if (dif.op == DiffOperation.EQ) {
				// common snippet in both texts
				orgTxt = new String(dif.text);
				dif.text = synonymizer1.unApplySynonyms(orgTxt);
				// unApply (and discard) to text2 as well
				synonymizer2.unApplySynonyms(orgTxt);
			} else if (dif.op == DiffOperation.INS) {
				// its in text2
				dif.text = synonymizer2.unApplySynonyms(dif.text);
			} else if (dif.op == DiffOperation.DEL) {
				// its in text1
				dif.text = synonymizer1.unApplySynonyms(dif.text);
			}
			if (dif.text.indexOf("__sy_") >= 0) {
				log.info("some markers are left in diffJson: " + dif );
				try {
					log.info("synonymizer1 = " + JSonUtils.object2JsonString(synonymizer1));
					log.info("synonymizer2 = " + JSonUtils.object2JsonString(synonymizer2));
				} catch (Exception e) {
					log.warn("", e);
				}
			}
		}
		return diffs;
	}
	
	public String toJson() throws JsonProcessingException {
		Map<String, Object> map = new HashMap<>();
		map.put("synonyms", synonyms);
		map.put("synListId2ListPatternMap", synListId2ListPatternMap);
		map.put("synPattern2CommonSynMap", synPattern2CommonSynMap);
		return JSonUtils.object2JsonString(map);
	}
	
	// ********************************* //
	
	private void makeSynWord2SynIdMap() {
		//synWord2SynIdMap.clear();
		//synListId2SynListMap.clear();
		synListId2ListPatternMap.clear();
		synPattern2CommonSynMap.clear();
		
		String synListId, listPtrn;
		List<String> synList;
		//Iterator<String> itr;
		//List<String> synSubList;
		String shortestWord, synW;
		for(int s=0; s < synonyms.size(); s++) {
			synList =  synonyms.get(s);
			if (null == synList)
				continue;
			shortestWord = "";
			if (synList.size() > 1) {
				shortestWord = synList.get(0);
				for (String syn : synList) {
					synW = syn.replaceAll("\\(.*?\\)|[\\p{Punct}}]", "").trim();
					if (StringUtils.isNotBlank(synW)  &&  synW.length() < shortestWord.length())
						shortestWord = synW;
				}
				if (StringUtils.isBlank(shortestWord))
					shortestWord = synList.get(synList.size() - 1).replaceAll("\\(.*?\\)|[\\p{Punct}}]", "").trim();
			}
			
			// add syn-list
			synListId = "__sy_"+s;
			//synListId2SynListMap.put(synListId, synSubList);
			// put words of the list into map
			/*
			for (int i=0; i < synSubList.length; i++) {
				synWord2SynIdMap.put(synSubList[i], synListId + "__"+i);			//"__sy_0__1" ...
			}
			*/
			// create syn list pattern
			listPtrn = StringUtils.join(synList, "|");
			synListId2ListPatternMap.put(synListId, Pattern.compile(listPtrn));
			// map syn sub-list pattern to last/smallest common word/or empty
			//shortestWord = (1 == synSubList.length)? "": synSubList[synSubList.length-1];
			synPattern2CommonSynMap.put(listPtrn, shortestWord.replaceAll("[\\p{Punct}}]", "").trim());
		}
	}
	
	
	// ************************************************************* //
	// ************************************************************* //
	
	private class Synonymizer {
		private String orgText = null;
		private List<SynonymWordMatch> synWordMatches = null;
		private String synonymizedText = "";			// syn'd text (original text after replacing all syn-words with corresponding markers/ids).
		private String syndTextSeenSoFar = "";			// text seen so far for "un-applying" synonyms (ie replacing markers back with their corresponding words).
		
		String applySynonymsTo(String text) {
			if (StringUtils.isBlank(text)) {
				synonymizedText = text;
				return synonymizedText;
			}
			// keep org-text safe
			this.orgText = new String(text);

			Set<SynonymWordMatch> synWordMatcheSet = getSynonymsMatched(text);
			// see if we found some synonyms
			if (synWordMatcheSet.size() == 0) {
				// no synonym found in the text !! - we are done
				synonymizedText = text;
				return synonymizedText;
			}
			
			// now we have all the syns applicable, sorted by startIdx of org-word in the text given.
			synWordMatches = new ArrayList<>(synWordMatcheSet);
			
			// we may have some matches overlapping (starts within previous match but extends beyond previous match), or 
			// scenarios where a multi-word syn matched and another smaller/1-word syn also matched to same set of words ie ("Indenture Trustee", "Trustee", "Indenture") etc.
			discardOverlappingMatches();

			// now we have all the syns applicable, sorted by startIdx of org-word in the text given. Lets replace them in given text and return 'synonymizedText'
			int start = 0;
			StringBuilder sb = new StringBuilder();
			for (SynonymWordMatch swm1 : synWordMatches) {
				if (start > swm1.start)
					continue;
				sb.append(orgText.substring(start, swm1.start));
				sb.append(swm1.synId);
				start = swm1.end;
			}
			// rest of text, if left any
			sb.append(orgText.substring(start));
			// set the syn'd text
			this.synonymizedText = sb.toString();
			return this.synonymizedText;
		}
		
		@SuppressWarnings("unused")
		String unApplySynonyms(String syndText) {
			if (null == syndText  ||  null == synWordMatches  ||  synWordMatches.size() == 0)
				return syndText;

			// this maxIdx is helpful to identify upto which marker-idx to replace back.
			int maxIdx = (this.syndTextSeenSoFar.length() + syndText.length()) ;
			String synId, word;
			SynonymWordMatch match;
			for (int i=0; i < synWordMatches.size(); i++) {
				match = synWordMatches.get(i);
				//if (match.start > (maxIdx - match.synId.length()) )
				if (syndText.indexOf(match.synId) < 0)
					break;
				synId = match.synId;
				word = match.orgWord;
				if (syndText.indexOf(synId) < 0) {
					// strange - this should not happen
					log.warn("was expecting synId be found in syndText but could not!  syndText=" +syndText+ ", syndTextSeenSoFar="+this.syndTextSeenSoFar);
					try {
						log.warn("synWordMatches="+JSonUtils.object2JsonString(synWordMatches));
					} catch (JsonProcessingException e) {
						log.warn("", e);
					}
					//System.out.println("was expecting synId be found in syndText but could not!  syndText=" +syndText+ ", syndTextSeenSoFar="+this.syndTextSeenSoFar);
					break;
				}
				syndText = syndText.replaceFirst(synId, word);
				// since we have changed length of syndtext (replaced a synId with actual word, adjust/update the maxIdx (ie length so far)
				maxIdx += match.wordLen - match.synId.length();
				// remove this match, it has been used
				synWordMatches.remove(i);
				i--;
			}
			this.syndTextSeenSoFar += " " + syndText.trim();
			return syndText;
		}
		
		
		//// ---------------------
		private Set<SynonymWordMatch> getSynonymsMatched(String text) {
			Pattern regExp;
			//String match = null;
			int start, end;
			// 'set' to keep syn-matches. Why 'set': sometime a synonym may repeat in different list and thus we may have >1 matches to same word - we need only 1 match to one word/setOfWords
			Set<SynonymWordMatch> synWordMatcheSet = new TreeSet<>();
			for (String key : synListId2ListPatternMap.keySet()) {
			    regExp = synListId2ListPatternMap.get(key);
			    Matcher matcher = regExp.matcher(text);
				while (matcher.find()) {
					if (StringUtils.isBlank(matcher.group()))
						continue;
					start = matcher.start();
					end = matcher.end();
					synWordMatcheSet.add(new SynonymWordMatch(matcher.group(), key, start, end));
				}
			}
			return synWordMatcheSet;
		}
		
		private void discardOverlappingMatches() {
			// we may also have scenarios where a multi-word syn matched and another 1-word syn also matched to same set of words ie ("Indenture Trustee", "Trustee", "Indenture") etc. 
			// In such case, we need to discard those syns whose sIdx equal to that of previous/nest match or it 'contains' (>=sIdx, <=eIdx) of previous/nest match.
			SynonymWordMatch swm, prvSwm = synWordMatches.get(0);
			Integer delIdx;
			for (int i=1; i < synWordMatches.size(); i++) {
				//TODO: test prvSwm's re-assigning
				prvSwm = synWordMatches.get(i-1);
				
				swm = synWordMatches.get(i);
				delIdx = null;
				if (swm.start == prvSwm.start) {
					// both words starts at same index. Discard shorter word.
					if (swm.orgWord.length() > prvSwm.orgWord.length()   ||   swm.equals(prvSwm))
						delIdx = i-1;
					else
						delIdx = i;
				} else if (isBetween(swm.start, prvSwm.start, prvSwm.end) ) {			// &&  swm.end >= prvSwm.end
					// this word 'overlaps' or 'contained-within' (starts later than prevStart and before prevEnd,  and ends later or same) the previous one.
					// discard the shorter match.
					if (swm.orgWord.length() > prvSwm.orgWord.length())
						delIdx = i-1;
					else
						delIdx = i;
				}
				if (null != delIdx) {
					synWordMatches.remove(delIdx.intValue());		// remove the desired match
					i--;				// to keep next test on the same index
				}
			}
		}
		
		private boolean isBetween(int n, int n1, int n2) {
			return (n > n1  &&  n < n2);
		}
	}
	
	// ************************************************************* //

	private class SynonymWordMatch implements Comparable<SynonymWordMatch> {
		public String orgWord;
		public String synId;
		public int wordLen = 0;
		public Integer start = null;
		public Integer end = null;
		
		SynonymWordMatch(String orgWord, String synId, int start, int end) {
			this.orgWord = orgWord;
			if (null != orgWord)
				wordLen = orgWord.length();
			this.synId = synId;
			this.start = start;
			this.end = end;
			if (end <= 0)
				this.end = start + orgWord.length();
		}
		
		/**
		 * Ensures uniqueness. Two matches are equal when the word/start/end are same. 
		 */
		@Override
		public boolean equals(Object o) {
			if (! (o instanceof SynonymWordMatch))
				return false;
			SynonymWordMatch swm = (SynonymWordMatch)o;
			if (swm.orgWord.equals(this.orgWord)  &&  swm.start == this.start  &&  swm.end == this.end)
				return true;
			return false;
		}

		/**
		 * Helps in sort. Sort by 'start' index in ASC order, and 'end' in DESC order (if 2 words starts at same index). 
		 * This ensures we have bigger/multi-word matches come first/before when another smaller/1-word syn also matches to same word/setOfWords (ie same start index).
		 * 
		 */
		@Override
		public int compareTo(SynonymWordMatch o) {
			int eq = this.start.compareTo(o.start);		// ASC on 'start'
			if (0 == eq)
				eq = o.end.compareTo(this.end);			// DESC on 'end'
			return eq;
		}
		
	}
	
	// ************************************************************* //

	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] arg) throws IOException {
		String t1 = "In case an Event of Default with respect to the Securities of a series has occurred (that has not been cured or waived), the Trustee shall exercise with respect to Securities of that series such of the rights and powers vested in it by this Indenture, and use the same degree of care and skill in their exercise, as a prudent man would exercise or use under the circumstances in the conduct of his own affairs.";
		String t2 = "(a) If a Default has occurred and is continuing (of which the Trustee is deemed to have notice thereof within the meaning of Section 7.02(l) hereof), the Trustee will exercise such of the rights and powers vested in it by this Indenture, and use the same degree of care and skill in its exercise, as a prudent person would exercise or use under the circumstances in the conduct of such person's own affairs.";
		
		t1 = "The Trustee may file such proofs of claim, statements of interest and other papers or documents as may be necessary or advisable in order to have the claims of the Trustee (including any claim for reasonable compensation, expenses disbursements and advances of the Trustee (including counsel, accountants, experts or such other professionals as the Trustee deems necessary, advisable or appropriate)) and the Holders allowed in any judicial proceedings relative to the Company or any Note Guarantor, their creditors or their property, shall be entitled to participate as a member, voting or otherwise, of any official committee of creditors appointed in such matters and, unless prohibited by law or applicable regulations, may vote on behalf of the Holders in any election of a trustee in bankruptcy or other Person performing similar functions, and any Custodian in any such judicial proceeding is hereby authorized by each Holder to make payments to the Trustee and, in the event that the Trustee shall consent to the making of such payments directly to the Holders, to pay to the Trustee any amount due it for the reasonable compensation, expenses, disbursements and";
		t2 = "Trustee May File Proofs of Claim. The Trustee may file such proofs of claim and other papers or documents as may be necessary or advisable in order to have the claims of the Trustee (including any claim for the reasonable compensation, expenses, disbursements and advances of the Trustee, its agents and counsel) and the Holders allowed in any judicial proceedings relative to the Company, its Subsidiaries or its or their respective creditors or properties and, unless prohibited by law or applicable regulations, may be entitled and empowered to participate as a member of any official committee of creditors appointed in such matter and may vote on behalf of the Holders in any election of a trustee in bankruptcy or other Person performing similar functions, and any Custodian in any such judicial proceeding is hereby authorized by each Holder to make payments to the Trustee and the Agent and, in the event that the Trustee or the Agent shall consent to the making of such payments directly to the Holders, to pay to the Trustee or the Agent any amount due it for the compensation, expenses, disbursements and advances of the Trustee or the Agent, its respective agents and its respective counsel, and any other amounts due the Trustee or the Agent under Section 7.7";
		
		String json = FileSystemUtils.readTextFromFile2("c:/temp/synonyms.json");
		List syns = JSonUtils.json2List(json, List.class);
		
		SynonymApplier sa = new SynonymApplier(syns);
		String[] syndTxts = sa.applySynonyms(t1, t2);
		
		TextDiffHelper dh = new GoLawDiff(true, true);
		List<DiffDetail> diffs = dh.getDiff(syndTxts[0], syndTxts[1]);
		System.out.println(JSonUtils.object2JsonString(diffs));
		
		diffs = sa.unApplySynonyms(diffs);
		System.out.println(JSonUtils.object2JsonString(diffs));
		System.out.println(dh.getDiffWordsCount(diffs));
		System.out.println(dh.getDiffHtml(diffs));
	}
	
}
