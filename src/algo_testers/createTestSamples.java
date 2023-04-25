package algo_testers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import contracts.GoLaw;
//import contracts.GoLaw;
import search.SolrJClient;
import xbrl.NLP;
import xbrl.Utils;

public class createTestSamples {
	
	
	public ArrayList<String> breakPara(String para) {
		ArrayList<String> sentList = new ArrayList<String>();
		NLP nlp = new NLP();
		List<Integer> list = nlp.getAllIndexEndLocations(para, NLP.patternSentenceEnd);// won't work unless \r\n
																						// ==>\r\n\r\n
		int eIdx = 0, priorEidx = 0;
		for (int i = 0; i < list.size(); i++) {
			eIdx = list.get(i);
			sentList.add(para.substring(priorEidx, eIdx + 1).trim());
			priorEidx = eIdx;
		}
		sentList.add(para.substring(eIdx, para.length()).trim());
		return sentList;
	}

	public String replaceNonAsciiChars(String text) {
		// replace special quotes/chars with ascii ones or remove
		text = text.replaceAll("[\\“\\”]", "\"").replaceAll("\\’", "'").replaceAll("⁄", "/")
				.replaceAll("(?i)[\\uFEFF]", "").replaceAll("(?i)[^\\p{ASCII}]", ""); // Verify if need to remove ALL
																						// non-ascii chars
																						// ([^\\p{ASCII}]). some may be
																						// of interest to client.
		return text;
	}

	
	public String readAndCleanText(String filePath) throws IOException {

		String text = Utils.readTextFromFile(filePath);

		text = text.replaceAll("\r\n", "xzXzCx");// KEEP THESE
		text = text.replaceAll("\n", "xzXzCx");// KEEP THESE
		text = text.replaceAll("\r", "xzXzCx");// KEEP THESE
		text = text.replaceAll(" ?xzXzCx ?", "xzXzCx");// KEEP THESE
		text = text.replaceAll("xzXzCx", "\r\n\r\n");// KEEP THESE!!!
		text = text.replaceAll("[\r\n]", "\r\n\r\n");// KEEP THESE!!!
//		Utils.writeTextToFile(new File("e:/legalterms/clientDocFixed.txt"), text);
		fullDocAnalyzer fda = new fullDocAnalyzer();
		String clause = fda.replaceNonAsciiChars(text);

		clause = clause.replaceAll("([a-zA-Z]{1})(\\.)([a-zA-Z]{1})(\\.)([a-zA-Z]{1})(\\.)", "$1xxPD$3xxPD$5xxPD");
		clause = clause.replaceAll("([a-zA-Z]{1})(\\.)([a-zA-Z]{1})(\\.)", "$1xxPD$3xxPD");
		clause = clause.replaceAll(" (?=(INC|CO|Inc|Co))\\.(?=[, ]{1})", "xxPD");
		clause = clause.replaceAll("(?<= (INC|CO|Inc|Co))\\.(?=[, ]{1})", "xxPD");

		return clause;
	}
	
	public static void createCsvFile(String filePath, StringBuilder SB) throws FileNotFoundException {
//		System.out.println(filePath + SB);
		PrintWriter pw = new PrintWriter(new File(filePath));// delete after testing
		pw.append(SB.toString());// delete after testing
		pw.close();// delete after testing
	}
	
	public static void main(String args[]) throws SolrServerException, IOException {
		
		fullDocAnalyzer fda = new fullDocAnalyzer();
				
		GoLaw gl = new GoLaw();
		
		double hwR = 0.0;
		int wCntCS = 0;
		
		StringBuilder sb = new StringBuilder();
		sb.append("text");
		sb.append('|');
		sb.append("def");
		sb.append('\n');

		
		ArrayList<String> tempSentList = new ArrayList<>(); 
		String tempSent = null;
		String def = null;
		String sec = null;
		SolrJClient solrClientBig = new SolrJClient("http://localhost:8983/solr/", "indenture");
		
		String filePath = "E:\\legalterms\\clientdoc4.txt";
		String clause = fda.readAndCleanText(filePath);
		tempSentList = fda.breakPara(clause);
		int hCnt = 0;
		String[] fls = { "def","sec" };
		for (int k = 200; k < tempSentList.size(); k++) {
			
			System.out.println("Sentence no - "+ k);
			tempSent = tempSentList.get(k).replaceAll("xxPD", ".");
			
			hCnt = gl.goLawGetHtxt(tempSent).split(" ").length;
			if(hCnt > 6) {
				wCntCS = tempSent.split(" ").length;

				hwR = ((double) hCnt / (double) wCntCS) * 100;

				if (hwR > 15) {
					SolrQuery bigQuery = solrClientBig.makeQuery("hTxt:(" + gl.goLawGetHtxt(tempSent) + ")", null, fls, null).setRows(1);
					QueryResponse solrBigQuery = solrClientBig.search(bigQuery);
//					System.out.println(solrBigQuery);
					SolrDocumentList resultDoc = solrBigQuery.getResults();
					if(resultDoc.size() > 0) {
						def = (String) solrBigQuery.getResults().get(0).get("def");
						sec = (String) solrBigQuery.getResults().get(0).get("sec");
						String text_tb = tempSent.replaceAll("\n","").replaceAll("\\r\\n|\\r|\\n", " ");
						sb.append( text_tb );
						sb.append("|");
						if(null != def) {
							sb.append(def);	
						}
						else {
							sb.append(sec);
						}
						
						sb.append('\n');	
					}	
				}				
			}		
							
		}
		
		createCsvFile("e:/keshav/test_y.csv", sb);
		System.out.println("Done");
	}		
	
}