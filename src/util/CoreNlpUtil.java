package util;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import contracts.SolrPrep;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import xbrl.NLP;
import xbrl.Utils;

public class CoreNlpUtil {

	public CoreNlpUtil() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] arg) throws IOException {
		NLP nlp = new NLP();
		

		// https://stanfordnlp.github.io/CoreNLP/annotators.html. NER (Named Entity Recognition) hangs the machine up.

		// the engine - StanfordCoreNLP - looks for the annotators which is the key and
		// then the value is which functions we are interested in - these values are
		// 'tokenize and then ssplit etc. Each one that generates the related algos to
		// produce the results for that value. 
		
		Properties props = new Properties();
	    props.setProperty("annotators"
	    		, "tokenize"
	    		+ ", ssplit"
	    		+ ", pos"
//	    		+ ", lemma"
	    		+", parse"
//	    		+", sentiment"
//	    		//, dcoref, ner"
	    		);
//	    tokenize,ssplit,pos,parse

		// We then create the new object - which standford calls a pipeine.
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    
	    String text = Utils.readTextFromFile("c:/getContracts/temp/tmp.txt");
	    System.out.println(SolrPrep.getPartsOfSpeech(text));
	    
	    StringBuilder sb = new StringBuilder();

	    Annotation annotation = new Annotation(text);
	    
		// run all the selected Annotators on this text
	    pipeline.annotate(annotation);

	    // this prints out the results of sentence analysis to file(s) in good formats
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    pipeline.prettyPrint(annotation, baos);
	    String print = baos.toString();
	    
	    baos.reset();
	    pipeline.xmlPrint(annotation, baos);
	    String xmlPOS = baos.toString();
		// System.out.println(annotation.toShorterString());
		// PrintWriter pw = new PrintWriter(new
		// File("c:/getContracts/stanfordNLP/testPOS.xml"));
		// pw.append(xml);
		// pw.close();
	    
	    List<String> listPOS = nlp.getAllMatchedGroups(xmlPOS, Pattern.compile("(?sm)(?<=<POS>).*?(?=</POS>)"));
	    List<String> listWord = nlp.getAllMatchedGroups(xmlPOS, Pattern.compile("(?sm)(?<=<word>).*?(?=</word>)"));
	    List<String[]> listPosWord = new ArrayList<>();
	    String pos, word;
	    
	    System.out.println("xmlPOS.len="+xmlPOS.length()+" listPOS.size="+listPOS.size());
	    for(int i=0; i<listPOS.size(); i++) {
	    	pos = listPOS.get(i);
	    	word = listWord.get(i);
	    	System.out.println("word="+word+" POS="+pos);
	    }


		// props.setProperty("annotators","tokenize, ssplit");
		// StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		// Annotation annotation = new Annotation(text);
	    pipeline.annotate(annotation);
	    List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
//	    List<CoreMap> sentences = annotation.get(CoreAnnotations.ParagraphAnnotation.class,false);
	    int cnt =0;
	    sb = new StringBuilder();
	    for (CoreMap sentence : sentences) {
	    	cnt++;
	    	sb.append("||"+ sentence);
	    }
	    
	    System.out.println("sb.toString="+sb.toString());
//	    pw = new PrintWriter(new File("c:/getContracts/stanfordNLP/testSentence.txt"));
//	    pw.append(sb.toString());
//	    pw.close();
		
		
	}
//	
	
}
