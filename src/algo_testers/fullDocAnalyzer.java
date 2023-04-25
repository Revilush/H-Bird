package algo_testers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.HttpMethod;

import charting.JSonUtils;
import contracts.GoLaw;
import search.SolrJClient;
import xbrl.EasyHttpClient;
import xbrl.NLP;
import xbrl.Utils;

public class fullDocAnalyzer {

	public static ArrayList<String> breakPara(String para) {
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

	public static Map<String, Object> countFrequencies(ArrayList<String> list) {

		// hash set is created and elements of
		// arraylist are insertd into it
		Set<String> st = new HashSet<String>(list);
		Map<String, Object> ltCount = new HashMap<>();
		for (String s : st)
			ltCount.put(s, Collections.frequency(list, s));
		return ltCount;
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

	public static void main(String args[]) throws SolrServerException, IOException {
		fullDocAnalyzer fda = new fullDocAnalyzer();

		GoLaw gl = new GoLaw();

		ArrayList<String> sentList = new ArrayList<>();

		SolrJClient solrClient = new SolrJClient("http://localhost:8983/solr/", "coreTermLibrary");

		SolrJClient solrClientBig = new SolrJClient("http://localhost:8983/solr/", "indenture");

		int default_hCnt = 6;
		int default_hwR = 15;

		ArrayList<String> tempSentList = new ArrayList<>();

		StringBuilder sbHtml = new StringBuilder();// delete after testing

		StringBuffer sb = new StringBuffer();// delete after testing
		StringBuffer sb2 = new StringBuffer();// delete after testing
		int totalLegalTermCount = 0, wCntCS = 0, wCntLS = 0;// TODO: wCnt of client sent and lib sent
		int hCnt = 0;

		double hwR = 0.0;

		float bsRatio = 0;

		String tempSent = null;

		EasyHttpClient httpClient = new EasyHttpClient(false);
		httpClient.disableChunking();
		httpClient.useHttpProtocol_10();

		String htmlText = null;
		int count_set = 0, minW = 0, maxW = 0;

		sb = new StringBuffer();// delete after testing

		ArrayList<String> legalTermPerSent = new ArrayList<String>();

		float max_Score = 0;
		float max_sim_Score2 = 0;
		int bigNumber = 0, totalCount = 0;

		Map<String, List<String>> searchReq = new HashMap<>();
		Map<String, List<String>> searchReq2 = new HashMap<>();

		StringBuilder html = new StringBuilder();
		StringBuilder sbMsg = new StringBuilder();

		String htmlTmp = null;
		String json2 = null;

		String bs = null;
		String lsCs = null;
		String tcTt = null;
		int tcTtInt = 0;
		Map<String, Object> lt_counts = new HashMap<>();

		// TODO: can we mirror how file is retrieved from word? -- > text = ""

		String filePath = "E:\\legalterms\\clientdoc4.txt";
		String clause = fda.readAndCleanText(filePath);
		tempSentList = fda.breakPara(clause);

		// TODO: fix code. Instantiate once, true whether variable, map, list,
		// stringbuilder etc. Get them out of the loop. And make sure you always reset
		// them whenever they are repopulated

		for (int k = 0; k < tempSentList.size(); k++) {
			tempSent = tempSentList.get(k).replaceAll("xxPD", ".");
			sb.append("\r\nsent==" + tempSent);

			hCnt = gl.goLawGetHtxt(tempSent).split(" ").length;

			wCntCS = 0;// always reset. Unless you are 100% sure it will ALWAYS get repopulated.

			if (hCnt > default_hCnt) {

				wCntCS = tempSent.split(" ").length;

				hwR = ((double) hCnt / (double) wCntCS) * 100;

				if (hwR > default_hwR) {
					sentList.add(tempSent);
					sb2.append("\r\nsent==" + tempSent);
				}
			}
		}

		createTextFiles("e:/legalTerms/all_sents.txt", sb);

		createTextFiles("e:/legalTerms/filtered_sents.txt", sb2);

		Map<String, String> otherParams = new HashMap<>();
		otherParams.put("defType", "edismax");
		otherParams.put("mm", "70%");

		for (int i = 0; i < sentList.size(); i++) {

			legalTermPerSent = new ArrayList<String>();

			wCntCS = sentList.get(i).split(" ").length;

			minW = (int) (wCntCS * 0.5);
			maxW = (int) (wCntCS * 2.2);

			String[] fqs = { "wCnt:[" + minW + " TO " + maxW + "]" };

//			fqs = ArrayUtils.add(fqs, "");
//			String[] fls = { "legalTerm" };

			SolrQuery query = solrClient
					.makeQuery("hTxt:(" + gl.goLawGetHtxt(sentList.get(i)) + ")", fqs, null, otherParams).setRows(5000);

			QueryResponse solrQuery = solrClient.search(query);
			SolrDocumentList resultDoc = solrQuery.getResults();

			if (resultDoc.size() == 0) {// delete after testing
				sb.append(query + "\r\n");
				continue;
			}

			boolean lt_check = false;

			SolrQuery bigQuery = solrClientBig
					.makeQuery("hTxt:(" + gl.goLawGetHtxt(sentList.get(i)) + ")", fqs, null, otherParams).setRows(50);

			QueryResponse solrBigQuery = solrClientBig.search(bigQuery);

			searchReq = new HashMap<>();

			List<String> al = new ArrayList<>();
			al.add(sentList.get(i));
			searchReq.put("list_1", (List<String>) resultDoc.get(0).get("txt"));
			searchReq.put("list_2", al);

			String json = JSonUtils.object2JsonString(searchReq);
			ByteArrayInputStream bis = new ByteArrayInputStream(json.getBytes());
			String resp = httpClient.makeHttpRequest("POST", "http://localhost:8000/api/use/similarity/", bis,
					json.length(), "application/json", null);

			Map<String, Object> respMap = JSonUtils.json2Map(resp);

			max_Score = Math.round((Double) JSonUtils.object2Map(respMap.get("meta")).get("max_score"));

			count_set = count_set + 1;

			for (SolrDocument sd : resultDoc) {

				legalTermPerSent.add((String) sd.getFieldValue("legalTerm"));
			}

			lt_counts = countFrequencies(legalTermPerSent);

			html = new StringBuilder();

			html.append("<p> <b>Solr Query - </b><a href='").append(solrClient.getConnectionUrl()).append("/select?")
					.append(query).append("' target='_blank'>").append(solrClient.getConnectionUrl()).append("/select?")
					.append(query).append("</a></p>").append("<p><b>Result found in core term library - </b>")
					.append(resultDoc.getNumFound()).append("</p>").append("<p><b>Top SolrDoc legalTerm</b> - ")
					.append(resultDoc.get(0).get("legalTerm")).append("</p>").append("<p><b>Similarity Score - </b>")
					.append(max_Score).append("</p><p><b>Number of results in Big core (indenture) - </b>")
					.append(solrBigQuery.getResults().getNumFound()).append("</p><a href='")
					.append(solrClientBig.getConnectionUrl()).append("/select?").append(bigQuery)
					.append("' target='_blank'>").append(solrClientBig.getConnectionUrl()).append("/select?")
					.append(bigQuery).append("</a>")
					.append("<p><b>Client text - </b>" + sentList.get(i) + "</p><p><b>Total Legal Terms - </b>")
					.append(lt_counts.size()).append("</p>").append("<table>").append("<tr>")
					.append("<th>Legal Term</th><th>Count</th><th>Total Count</th><th>Ratio b/s</th><th>Sim score with legal term</th>")
					.append("<th>Sim score with big core</th><th>CS wCnt</th><th>LS wCnt</th><th>wCntLS/wCntCS</th><th>Total LT count</th>")
					.append("<th>totalCount/totalTerms</th><th>Solr docs</th>").append("</tr>");

			htmlTmp = html.toString();
			bigNumber = (int) solrBigQuery.getResults().getNumFound();

			for (String k : lt_counts.keySet()) {

				String[] fqsTemp = { "wCnt:[" + minW + " TO " + maxW + "]" };
				fqsTemp = ArrayUtils.add(fqsTemp, "legalTerm:\"" + k + "\"");

				SolrQuery ltQuery = solrClient
						.makeQuery("hTxt:(" + gl.goLawGetHtxt(sentList.get(i)) + ")", fqsTemp, null, otherParams)
						.setRows(5000);
				QueryResponse solrLTQuery = solrClient.search(ltQuery);

				totalLegalTermCount = (int) solrLTQuery.getResults().get(0).get("totalTerms");

				searchReq2 = new HashMap<>();

				searchReq2.put("list_1", (List<String>) solrLTQuery.getResults().get(0).get("txt"));
				searchReq2.put("list_2", al);

				json2 = JSonUtils.object2JsonString(searchReq2);

				ByteArrayInputStream bis2 = new ByteArrayInputStream(json2.getBytes());

				String resp2 = httpClient.makeHttpRequest("POST", "http://localhost:8000/api/use/similarity/", bis2,
						json2.length(), "application/json", null);

				Map<String, Object> respMap2 = JSonUtils.json2Map(resp2);

				bsRatio = 0;
				max_sim_Score2 = Math.round((Double) JSonUtils.object2Map(respMap2.get("meta")).get("max_score"));

				bs = "";
				lsCs = "";
				tcTt = "";
				totalCount = 0;

				for (SolrDocument sda : solrLTQuery.getResults()) {
					totalCount += (int) sda.getFieldValue("txtCnt");
				}

				bsRatio = ((float) bigNumber / (float) totalCount);
				// 1. if total count of legal terms found versus total count of legal terms is
				// low (less than 10%) - then f/p
				// 2. if highest sim score<65 then f/p
				// 3. if cs wCnt is small versus ls wCnt and sim score not >70, likely f/p

				wCntLS = solrLTQuery.getResults().get(0).get("txt").toString().split(" ").length;
				bs = ((float) bigNumber / (float) totalCount) + "";
				bs = bs.substring(0, bs.indexOf(".") + 2);

				lsCs = ((float) wCntLS / (float) wCntCS) + "";
				lsCs = lsCs.substring(0, lsCs.indexOf(".") + 2);

				tcTtInt = (int) (((float) totalCount / (float) totalLegalTermCount) * 100);
				tcTt = (((float) totalCount / (float) totalLegalTermCount) * 100) + "";
				tcTt = tcTt.substring(0, tcTt.indexOf(".") + 2) + " %";

				if (max_sim_Score2 < 63 || tcTtInt < 7 || (max_sim_Score2 < 66 && tcTtInt < 30)
						|| (Double.parseDouble(lsCs) > 1.5 && max_sim_Score2 < 68)) {

					if (max_sim_Score2 > 60) {

						sbMsg.append("<br><br><b>DID NOT MATCH</b>" + htmlTmp + "<br>");
						sbMsg.append("<tr><td>").append(k).append("</td><td>").append(lt_counts.get(k))
								.append("</td><td>").append(totalCount).append("</td><td>").append(bs)
								.append("</td><td>").append(max_sim_Score2).append("</td><td>").append(max_Score)
								.append("</td><td>").append(wCntCS).append("</td><td>").append(wCntLS)
								.append("</td><td>").append(lsCs).append("</td><td>").append(totalLegalTermCount)
								.append("</td><td>").append(tcTt).append("</td>");

					}

				} else {
					if (!lt_check) {
						sbHtml.append(htmlTmp);
					}

					lt_check = true;
					// but this is a problem if the big DB isn't same
					// source as the core term library data. But still okay, b/c
					// if data is big results are good!

					sbHtml.append("<tr><td>").append(k).append("</td><td>").append(lt_counts.get(k)).append("</td><td>")
							.append(totalCount).append("</td><td>").append(bs).append("</td><td>")
							.append(max_sim_Score2).append("</td><td>").append(max_Score).append("</td><td>")
							.append(wCntCS).append("</td><td>").append(wCntLS).append("</td><td>").append(lsCs)
							.append("</td><td>").append(totalLegalTermCount).append("</td><td>").append(tcTt)
							.append("</td>");

					sbHtml.append("<td><a href='").append(solrClient.getConnectionUrl()).append("/select?")
							.append(ltQuery).append("' target='_blank'>Check here all Doc</td></tr>");
				}

			}

			if (lt_check) {
				sbHtml.append("</table></br><p>------------------------------------------------------</p></br>");
			}
		}

		createTextFiles("e:/legalterms/queries with no results.txt", sb);

		System.out.println(count_set);

		sbHtml.append(sbMsg.toString());
		writeHTMLFile("e:\\legalTerms\\test_html.html", sbHtml);

		System.out.println("created completed");
		
		// TODO: Are we only search typ=1 or both typ=3 and 1. Why did it not get
		// Definition - Opinion of Counsel in "E:\legalTerms\\clientDoc4.txt" - Keshav
		// to ck why this was not picked up as a match
		
		// FIX: sim score with big core is always the same as sim score for legal term

		// TODO: So we can check the legal terms that were not found, print list of
		// legal terms that were rejected due to our filters. See sbMsg

		// TODO: add sim score of client sent against top txtCnt of the legalTerm

		// TODO: Show legal term sentence in report that had the highest sim score

		// TODO: cleanup utility - run sim score of all the variations of a legal term
		// against the highest txtCnt text of that legal term. These can then be
		// reviewed to see if any are erroneous. This will help us prevent bad results
		// being put into a legal term.

		// TODO: allow user to add results to an existing legal term - unless sim score
		// against highest txtCnt score is too low
	}
}
