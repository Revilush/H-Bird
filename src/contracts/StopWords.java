package contracts;

public class StopWords {

	public static String removeDefinedTerms(String txt) {
		
		txt = txt.replaceAll("(?sm)(?<=(^|\r\n| |,|-))[A-Z]+[a-z]{0,}", "").replaceAll("[ ]{2,}", " ")
				.replaceAll("( )([,:;])", "$2").replaceAll(",\\.|\\.,", "\\.").trim();
		
		return txt;
	}

	public static String removeStopWordsOf2(String text){
		text = PatternsDif.patternStopWordsOf2
				.matcher(text).replaceAll("");
		return text;
	}

	public static String removeStopWords(String text){
		
		text = text.replaceAll("'s|,|'", " ");
//				System.out.println("text at replace StopWordsContract - text="+text);
		text = PatternsDif.patternStopWordsContract.matcher(text).replaceAll("");

//		 System.out.println("before replace legal stopwords - text ="+text);
		
		text = PatternsDif.patternStopWordsLegal.matcher(text).replaceAll("");
//		text = PatternsDif.patternStopWordsLegalEntity.matcher(text).replaceAll("");
//		System.out.println("before patternStopWordsLegalEntitySpecific - text =" + text);
	
//		text = PatternsDif.patternStopWordsLegalEntitySpecific.matcher(text).replaceAll("");

//		System.out.println("before replaceUsingTwoStringArrays - text =" + text);

		text = TermFinder.replaceUsingTwoStringArrays(
				PatternsDif.stateAndCountryLongNames,
				PatternsDif.stateAndCountryAbbrevs,
				text);
		
//		System.out.println("before patternStopWords - text =" + text);

		text = PatternsDif.patternStopWords
				.matcher(text).replaceAll("");
		
		return text;
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
