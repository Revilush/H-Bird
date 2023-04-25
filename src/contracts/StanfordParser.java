package contracts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import charting.FileSystemUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.CoreMap;
import util.GoLawLancasterStemmer;
import xbrl.NLP;

public class StanfordParser {

	public static String lemma(String text) {

		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		pipeline = new StanfordCoreNLP(props, false);
		Annotation document = pipeline.process(text);

		StringBuilder sb = new StringBuilder();
		for (CoreMap sentence : document.get(SentencesAnnotation.class)) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String word = token.get(TextAnnotation.class);
				String lemma = token.get(LemmaAnnotation.class);
				// System.out.println("lemmatized version :" + lemma);
				sb.append(" " + lemma);
			}
		}

		return sb.toString();
	}

	public static String getNER(String text) throws IOException {

//		System.out.println("getSentencesStanfordParser");
//		NLP nlp = new NLP();

		Properties props = new Properties();

		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, regexner");
//		props.put("regexner.mapping", "org/foo/resources/jg-regexner.txt");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		NamedEntityTagAnnotation nea = new NamedEntityTagAnnotation();

		Annotation annotation = new Annotation(text);
		pipeline.annotate(annotation);

//		List<String> listSentences = new ArrayList<>();
		String sentence = annotation.get(CoreAnnotations.NamedEntityTagAnnotation.class);
//		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
//		for (CoreMap sentence : sentences) {
//			listSentences.add(sentence.toString());
//		}
		System.out.println("sentences=" + sentence);

		return sentence;
	}

	public static void main(String[] args) throws IOException {

//		String text ="\r\n" + 
//				"INDENTURE dated as of August 22, 2012 among CAESARS OPERATING ESCROW LLC, a Delaware limited liability company, CAESARS ESCROW CORPORATION, a Delaware corporation (together, the Escrow Issuers or the Issuer, provided that, for purposes of this Indenture with respect to any class of Notes, including Additional Notes, prior to an Assumption (as defined herein) with respect to such class of Notes, the references to the Issuer in this Indenture refer only to the Escrow Issuers; after the consummation of such Assumption with respect to such class of Notes, the references to the Issuer refer only to Caesars Entertainment Operating Company, Inc., a Delaware corporation, and not to any of its subsidiaries), CAESARS ENTERTAINMENT CORPORATION, a Delaware corporation (the Parent Guarantor), and U.S. BANK NATIONAL ASSOCIATION, as trustee (the Trustee).";
//		System.out.println(SolrPrep.getPartsOfSpeech(text));
//		System.out.println(Stemmer.stemmedOutPutPorter("occurred"));
//		System.out.println(Stemmer.stemmedOutPutPorter("occurrence"));
//		System.out.println(Stemmer.stemmedOutPutPorter("occurs"));

		GoLawLancasterStemmer lc = new GoLawLancasterStemmer();
		System.out.println(lc.stemWord("occurred"));
		System.out.println(lc.stemWord("occurrence"));
		System.out.println(lc.stemWord("occurs"));

//		System.out.println(lc.stemWord(StanfordParser.lemma("occurred")));
//		System.out.println(lc.stemWord(StanfordParser.lemma("occurrence")));
//		System.out.println(lc.stemWord(StanfordParser.lemma("occurs")));
//		
		//library of words that don't have the same stem but should:
		// occurs has an anomalous stem as compared to its past tense - we want it to be
		// occur. Therefore we create a tool for these words that convert them to the
		// word that will produce the same stem result
		
		String text = "occurs".replaceAll("(occurs)|(liable)", "occurred");//solely for purpopses of getting same ste. We would put this in stemmer
		System.out.println("text == "+text);
		System.out.println(lc.stemWord("occurred"));
		System.out.println(lc.stemWord("occurrence"));
		System.out.println(lc.stemWord(text));

	}
}
