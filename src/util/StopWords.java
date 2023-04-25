package util;

import contracts.PatternsDif;;

public class StopWords {

	public static String removeStopWords(String text){
		
		
		text = text.replaceAll("'s|,|'", "");
//				System.out.println("text at replace StopWordsContract - text="+text);
		text = PatternsDif.patternStopWordsContract.matcher(text).replaceAll("");

//		 System.out.println("before replace legal stopwords - text ="+text);
		
		text = PatternsDif.patternStopWordsLegal.matcher(text).replaceAll("");
//		text = PatternsDif..patternStopWordsLegalEntity.matcher(text).replaceAll("");
//		System.out.println("before patternStopWordsLegalEntitySpecific - text =" + text);
	
		text = PatternsDif.patternStopWordsLegalEntitySpecific.matcher(text).replaceAll("");

//		System.out.println("before replaceUsingTwoStringArrays - text =" + text);

		text = TermFinder.replaceUsingTwoStringArrays(
				PatternsDif.stateAndCountryLongNames,
				PatternsDif.stateAndCountryAbbrevs,
				text);
		

		text = PatternsDif.patternStopWords
				.matcher(text).replaceAll("");
		
//		System.out.println("after patternStopWords - text =" + text);

		return text;
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
