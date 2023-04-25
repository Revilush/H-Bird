package algo_testers.dependencies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class QuickSynonymApplier {

	private static final Map<String, String> synonymKwd2PatternMap = new HashMap<>();
	
	/*
	 * static { // read synonyms and prepare the map List<List<String>> rawSyns =
	 * SynonymReader.getSynonymsRaw();
	 * 
	 * String pattern; List<String> regex = new ArrayList<>(); for (List<String>
	 * synList : rawSyns) { regex.clear(); for (int i=0; i < synList.size(); i++) {
	 * regex.add(RefreshSynonyms.convertToSynRegex(synList.get(i))); } // sort the
	 * syn-list by length - DESC: longest on top
	 * regex.sort(Comparator.comparing(String::length).reversed()); pattern =
	 * StringUtils.join(regex, "|"); for (String kwd : synList) {
	 * synonymKwd2PatternMap.put(kwd, pattern); } }
	 * 
	 * }
	 */
	
	public void applySynonyms(String clientText, String... dataTexts) {
		/**
		 * Steps:
		 * 	1- find synonym keywords/phrases that ARE in client sent :- tells these are the only synonyms we need to use on variations/dataTexts
		 * 	2- each such client kwd becomes the master word to be used as replacement for the syn-list in dataTexts
		 */
	}
	
	
	public String[] applyQuickSynonyms(String... dataTexts) {
		if (dataTexts.length <= 1)
			return dataTexts;
//		String[] grams = dataTexts[0].split(" ");
		List<String> grams = textNGrams(dataTexts[0], 4);
		List<String> synPatternsAlreadyUsed = new ArrayList<>();
		String ptrn;
		for (String cw : grams) {
			ptrn = synonymKwd2PatternMap.get(cw);
			if (StringUtils.isBlank(ptrn)  ||  synPatternsAlreadyUsed.indexOf(ptrn) >= 0)
				continue;
			// apply this pattern to all the sents
			for (int i=0; i < dataTexts.length; i++) {
				dataTexts[i] = dataTexts[i].replaceAll(ptrn, cw);
			}
			// this pattern is now used
			synPatternsAlreadyUsed.add(ptrn);
		}
		return dataTexts;
	}
	
	
	public static String applyQuickSynonyms2Sent(String dataText) {
		if (StringUtils.isBlank(dataText))
			return dataText;
		List<String> grams = textNGrams(dataText, 1);		// 1-word gram
		List<String> synPatternsAlreadyUsed = new ArrayList<>();
		String ptrn;
		for (String cw : grams) {
			ptrn = synonymKwd2PatternMap.get(cw);
			if (StringUtils.isBlank(ptrn)  ||  synPatternsAlreadyUsed.indexOf(ptrn) >= 0)
				continue;
			// apply this pattern to  the sent
			dataText = dataText.replaceAll(ptrn, cw);
			// this pattern is now used
			synPatternsAlreadyUsed.add(ptrn);
		}
		return dataText;
	}
	
	
	// -------------------------------------------
	
	private static List<String> textNGrams(String text, int maxGramSize) {
		List<String> grams = new ArrayList<>();
		String[] parts = text.replaceAll("[\\p{Punct}}]", " ").replaceAll("[ ]+", " ").trim().split(" ");
		List<String> words = new ArrayList<>(Arrays.asList(parts));
		for (int g=maxGramSize; g > 0; g--) {
			for (int j=0; (j+g) <= words.size(); j++) {
				grams.add(StringUtils.join(words.subList(j, j+g), " "));
			}
		}
		return grams;
	}
	
	
	public static void main(String[] arg) {
		//QuickSynonymApplier qsa = new QuickSynonymApplier();
		System.out.println(textNGrams("shall each have", 4));
	}
	
	
}
