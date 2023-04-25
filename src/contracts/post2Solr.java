package contracts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.solr.client.solrj.SolrServerException;
import org.xml.sax.SAXException;

import search.Post2Solr;
import xbrl.NLP;
import xbrl.Utils;

public class post2Solr {

	public static void post2SolrByContract(int sY, int eY, int sQ, int eQ, int amountPriorToCommit,
			List<String[]> listParse) throws FileNotFoundException, XPathExpressionException, IOException,
			ParserConfigurationException, SAXException, SolrServerException {
		NLP nlp = new NLP();
		// listParse has solr files folder to parse, the contract subfolder (eg
		// indenture) and the solr core it goes into.
		String path = "", core = "", contractFolder = "";

		// I only have to "push" - i don't need to specify subfolders - pos2solr does it
		// automatically!

		int inQ = sQ, inY = sY, wCnt = 0, leCnt = 0;
		PrintWriter pw = new PrintWriter(new File("c:/temp/tmp.txt"));
		String text = "", leStr = "";
		for (int i = 0; i < listParse.size(); i++) {
			path = listParse.get(i)[0];
			contractFolder = listParse.get(i)[1];
			core = listParse.get(i)[2];
//			System.out.println("listParse=" + Arrays.toString.(listParse.get(i)));
			sY = inY;
			for (; sY <= eY; sY++) {
				for (; sQ <= eQ; sQ++) {
					leCnt = 0;
					wCnt = 0;
					File flder = new File(path + "/" + sY + "/qtr" + sQ + "/" + contractFolder + "/");
					System.out.println("posting=" + flder);
					File[] listOfFiles = flder.listFiles();
					for (int n = 0; n < listOfFiles.length; n++) {
						File f = listOfFiles[n];
						text = Utils.readTextFromFile(f.getAbsolutePath()).replaceAll(",\"score\":\r\n", "");

						if (nlp.getAllMatchedGroups(text, Pattern.compile("(?<=\"openingParagraph\":\").*?(?=\"\r\n)"))
								.size() > 0) {
							wCnt = nlp
									.getAllMatchedGroups(text,
											Pattern.compile("(?<=\"openingParagraph\":\").*?(?=\"\r\n)"))
									.get(0).split(" ").length;
							text = text.replaceAll("\r\n(?=,\"openingParagraph\")",
									"\r\n,\"wCntInOpeningParagraph\":" + wCnt);
						}

						if (nlp.getAllMatchedGroups(text, Pattern.compile("\"legalEntitiesInOpeningParagraph\":"))
								.size() > 0) {
//							System.out.println("leStr");
							leStr = nlp
									.getAllMatchedGroups(text,
											Pattern.compile(
													"(?sm)(?<=\"legalEntitiesInOpeningParagraph\":).*?(?=\"\\])"))
									.get(0);
							if (nlp.getAllMatchedGroups(leStr, Pattern.compile("cnt\\d")).size() > 0) {
								leCnt = nlp.getAllMatchedGroups(leStr, Pattern.compile("cnt\\d")).size();
//								System.out.println("leCnt="+leCnt);
//								System.out.println("leStr="+leStr);
								text = text.replaceAll("\r\n(?=,\"legalEntitiesInOpeningParagraph\")",
										"\r\n,\"legalEntityCountInOpeningPara\":" + leCnt);
							}
						}

						pw = new PrintWriter(f);
						pw.append(text);
						pw.close();
					}

					Post2Solr p2s = new Post2Solr(flder.getAbsolutePath(), core)
							.withCommitAfterDocCount(amountPriorToCommit);
					p2s.ignorePostErrors = true;
					try {
						p2s.postNow();
					} catch(Exception e) {
						e.printStackTrace();
					}
					System.out.println("DONE..");
				}
				sQ = inQ;
			}
		}
	}
	

	public static void main(String[] args) throws IOException, SQLException, SolrServerException, ParseException,
			XPathExpressionException, ParserConfigurationException, SAXException {

		NLP nlp = new NLP();
//		path = listParse.get(i)[0];
//		contractFolder = listParse.get(i)[1];
//		core = listParse.get(i)[2];

		List<String[]> listParse = new ArrayList<String[]>();
		String[] ary = { "E:\\getContracts\\solrIngestion\\solrDocs\\", "investAdv", "invest" };
		String[] ary2 = { "E:\\getContracts\\solrIngestion\\solrDocs\\", "investMgt", "invest" };
		String[] ary3 = { "E:\\getContracts\\solrIngestion\\solrDocs\\", "escrow", "escrow" };
//		String[] ary3 = { "E:\\getContracts\\solrIngestion\\solrDocs\\", "485APOS", "test485" };
		listParse.add(ary);
		listParse.add(ary2);
		listParse.add(ary3);

		int sY = 2019, eY = sY+3, sQ = 1, eQ = 4, amountPriorToCommit = 5;
		for (; sY <= eY; sY++) {
			for (; sQ <= eQ; sQ++) {
				post2SolrByContract(sY, sY, sQ, sQ, amountPriorToCommit, listParse);
			}
			sQ = 1;
		}
	}
}
