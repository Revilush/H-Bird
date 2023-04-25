package algo_testers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;


import algo_testers.dependencies.LegalPackQueryDetail;
//import com.segemai.legal.core.utils.JSonUtils;

import algo_testers.GolawSearch;
//import algo_testers.KAnalysisTester;

import algo_testers.search_dependencies.SearchResponse;
import algo_testers.search_dependencies.SearchResultsGroupSummary;
import charting.JSonUtils;
import contracts.GoLaw;
import search.SolrJClient;
import xbrl.EasyHttpClient;
import xbrl.NLP;
import xbrl.Utils;

public class addinVaritionfinder {


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

	public static void createTextFiles(String filePath, StringBuffer SB) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new File(filePath));// delete after testing
		pw.append(SB.toString());// delete after testing
		pw.close();// delete after testing
	}


	public static void writeHTMLFile(String filePath, StringBuilder SBhtml)
			throws FileNotFoundException, UnsupportedEncodingException {

		File newHtmlFile = new File(filePath);

		Utils.writeTextToFile(newHtmlFile,
				"<html><style> table, th, td { border: 1px solid black;border-collapse: collapse;} th, td {padding: 15px;text-align: left;}</style><body>"
						+ "<p>Count = unique hashTxtId </p>"
						+ "<p>Total Count = total txtCount (sum of hashTxtId, txtcnt)</p>"
						+ "<p>Ratio b/s : # of big core/lt core (Total Count)</p>"
						+ "<p>sim score w/ legal term : highest LT solr result sim score to client sent</p>"
						+ "<p>sim score w/ big core : highest solr result of big core and its sim score v. client sent</p>"
						+ "<p>csWcnt : client sent wCnt</p>"
						+ "<p>lsWcnt : highest solr score sentence wCnt of legal term core</p>"
						+ "<p>totalLTCount : total count of that legal term in legal term lib</p>"
						+ "<p>totalCount/TotalTerms : see def terms above</p>"
						+ "<p>solrDocs : links to solr legal term solr queries</p>"
						+ "----------------------------------------------------------------------------------------------"
						+ SBhtml.toString() + "</body></html>");

	}	
	
	
	
	public static void main(String args[]) throws SolrServerException, IOException, SQLException {
		

	}
}