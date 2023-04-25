package search;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;

import charting.FileSystemUtils;
import charting.JSonUtils;
import contracts.SolrPrep;
//import sun.rmi.server.UnicastRef;
import util.EasyFileFilter;
import xbrl.JSoupXMLParser;
import xbrl.Utils;

public class Post2Solr {

	public static boolean uniquekIds;
	public boolean ignorePostErrors;
	public boolean json;

	private SolrJClient solrClient;

	private File folder2Submit;
	private int commitAfterDocCount = 10;

	public Post2Solr(String folder2Submit, String core) {
		this("http://localhost:8983/solr/", core, folder2Submit);
	}

	public Post2Solr(String solrUrl, String core, String folder2Submit) {
		if (null != folder2Submit)
			this.folder2Submit = new File(folder2Submit);
		this.solrClient = new SolrJClient(solrUrl, core, true);
	}

	public Post2Solr withCommitAfterDocCount(int commitAfterDocCount) {
		this.commitAfterDocCount = commitAfterDocCount;
		return this;
	}
	
	public void deleteDocsByQuery(String query) throws SolrServerException, IOException {
		solrClient.deleteDocsByQuery(query);
	}

	public void deleteAllDocs() throws SolrServerException, IOException {
		solrClient.deleteAllDocs();
	}

	/**
	 * Push documents from 'folder2Submit' and sub-folders, recursively, to solr
	 * (with url/core given); and committing after 'commitAfterDocCount' docs are
	 * submitted to Solr.
	 * 
	 * @throws SolrServerException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws XPathExpressionException
	 * @throws FileNotFoundException
	 */

	public void postNow() throws FileNotFoundException, XPathExpressionException, IOException,
			ParserConfigurationException, SAXException, SolrServerException {
		pushFolder(folder2Submit);

		/*
		 * File[] files = folder2Submit.listFiles(new EasyFileFilter(null, null, null,
		 * true, false)); if (null != files) pushFiles(files); // sub-folders of the top
		 * folder File[] subFolders = folder2Submit.listFiles(new EasyFileFilter(null,
		 * null, null, false, true)); if (null != subFolders) { for (File sf :
		 * subFolders) pushFolder(sf); }
		 */
	}

	public void pushFiles(File[] files) throws FileNotFoundException, XPathExpressionException, IOException,
			ParserConfigurationException, SAXException, SolrServerException {
		// submit docs to solr in bunch and commit after 'commitAfterDocCount' counts
		// int maxDocsIn1Shot = Math.min(commitAfterDocCount, files.length);
		List<SolrInputDocument> solrDocs = new ArrayList<>();
		String filePushed = "";

		Map<String, String> mapCIKs = new TreeMap<String, String>();

		if (uniquekIds && !json) {
			for (int n = 0; n < files.length; n++) {
				System.out.println("file=" + files[n]);
				mapCIKs.put(files[n].getName().substring(0, 10), files[n].getCanonicalPath());
			}

			int cnt = -1;
			for (Map.Entry<String, String> entry : mapCIKs.entrySet()) {

				solrDocs.clear();
				filePushed = "";

				File f = new File("");
				for (int j = 0; j < commitAfterDocCount && cnt < mapCIKs.size(); j++, cnt++) {

					f = new File(entry.getValue());
					// solrDocs.addAll(getDocumentsFromFile(f));
					filePushed = f.getCanonicalPath() + ", ";

				}

				if (solrDocs.size() > 0) {
					solrClient.postDocsToSolr(solrDocs);
					solrClient.commit();
					System.out.println("\rcnt" + cnt + "parsing CIKs for each folder--ie, quarter=" + uniquekIds + ""
							+ "\r filePushed = " + filePushed);
				}
			}
		}

		if (!uniquekIds || json) {

			for (int i = 0; i < files.length;) {
				solrDocs.clear();
				filePushed = "";
				for (int j = 0; j < commitAfterDocCount && i < files.length; j++, i++) {
					// solrDocs.addAll(getDocumentsFromFile(files[i]));
					solrDocs.addAll(getSolrDocumentsFromFile(files[i]));
					filePushed = files[i].getCanonicalPath() + ", ";

				}

				if (solrDocs.size() > 0) {
					solrClient.postDocsToSolr(solrDocs);
					solrClient.commit();
					System.out.println("\rcnt" + i + " parsing All contracts filePushed = " + filePushed);
				}
			}
		}

	}

	private List<SolrInputDocument> getSolrDocumentsFromFile(File file) throws FileNotFoundException,
			XPathExpressionException, IOException, ParserConfigurationException, SAXException {
		// based on extension (.xml/.json)
		String extn = FilenameUtils.getExtension(file.getAbsolutePath());
		if (StringUtils.containsIgnoreCase(extn, "json"))
			return getSolrDocumentsFromJsonFile(file);
		else
			return getSolrDocumentsFromXmlFile(file);
	}

	private void pushFolder(File folder) throws FileNotFoundException, XPathExpressionException, IOException,
			ParserConfigurationException, SAXException, SolrServerException {
		System.out.println("Posting folder: " + folder.getAbsolutePath());
		// push files, and then folders recursively
		File[] files = folder.listFiles(new EasyFileFilter(null, null, null, true, false));
		if (null != files)
			pushFiles(files);
		// sub-folders of the folder coming in
		File[] subFolders = folder.listFiles(new EasyFileFilter(null, null, null, false, true));
		if (null != subFolders) {
			for (File sf : subFolders) {
				pushFolder(sf);
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<SolrInputDocument> getSolrDocumentsFromXmlFile(File file) throws FileNotFoundException, IOException,
			ParserConfigurationException, SAXException, XPathExpressionException {
		List<SolrInputDocument> solrDocs = new ArrayList<>();

		Map<String, Object> docFields = new HashMap<>();
		Document xmlDocument = JSoupXMLParser.parseFile(file.getAbsolutePath());
		Elements fields, docs = xmlDocument.getElementsByTag("doc");
		String fn, fieldVal;
		Object vals;
		for (Element doc : docs) {
			docFields.clear();
			fields = doc.getElementsByTag("field");
			for (Element field : fields) {
				fn = field.attr("name");
				fieldVal = field.text();
				if (!docFields.containsKey(fn) || null == docFields.get(fn)) {
					docFields.put(fn, fieldVal);
				} else {
					// the field was seen already - value must be treated as part of list
					vals = docFields.get(fn);
					if (vals instanceof List) {
						((List) vals).add(fieldVal);
					} else {
						vals = new ArrayList<>(Arrays.asList(vals));
						((List) vals).add(fieldVal);
						docFields.put(fn, vals);
					}
				}
			}
			solrDocs.add(solrClient.getSolrInputDocument(docFields));
		}
		return solrDocs;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<SolrInputDocument> getSolrDocumentsFromJsonFile(File file) throws IOException {
		String json = Utils.readTextFromFile(file.getAbsolutePath());
		System.out.println("file.getAbsolutePath()::" + file.getAbsolutePath());
		List<Map> docs = JSonUtils.json2List(json, Map.class);
		List<SolrInputDocument> solrDocs = new ArrayList<>();
		for (Map m : docs) {
			solrDocs.add(solrClient.getSolrInputDocument(m));
		}
		return solrDocs;
	}

	/*
	 * private List<SolrInputDocument> getDocumentsFromFile(File file) throws
	 * FileNotFoundException, IOException, ParserConfigurationException,
	 * SAXException, XPathExpressionException { List<SolrInputDocument> solrDocs =
	 * new ArrayList<>();
	 * 
	 * Map<String, Object> docFields = new HashMap<>(); Document xmlDocument =
	 * JSoupXMLParser.parseFile(file.getAbsolutePath()); Elements fields, docs =
	 * xmlDocument.getElementsByTag("doc"); for (Element doc : docs) {
	 * docFields.clear(); fields = doc.getElementsByTag("field"); for (Element field
	 * : fields) { docFields.put(field.attr("name"), field.text()); }
	 * solrDocs.add(solrClient.getSolrInputDocument(docFields)); } return solrDocs;
	 * }
	 */

//	public static void post2solrByYearQuarter(int yr, int q, String core, boolean parseSentences)
//			throws FileNotFoundException, XPathExpressionException, IOException, ParserConfigurationException,
//			SAXException, SolrServerException {
//
//		String strippedQtrPath = SolrPrep.folderParagraphsXml;
//		if (parseSentences)
//			strippedQtrPath = SolrPrep.folderSentencesXml;
//
//		File sentenceFolder = new File(strippedQtrPath);
//		File[] listOfSentenceFiles = sentenceFolder.listFiles();
//		int amountPriorToCommit = 10;
//		// this posts the folder- I don't need to specify the file
//
//		for (File f : listOfSentenceFiles) {
//			// each K type
//			File sentenceContractSubFolder = new File(f.getAbsolutePath() + "/" + yr + "/");
//			File[] listOfSentenceFilesSub = sentenceContractSubFolder.listFiles();
//			if (listOfSentenceFilesSub == null || listOfSentenceFilesSub.length < 1)
//				continue;
//
//			System.out.println("contract sub folder=" + sentenceContractSubFolder);
//			for (File f2 : listOfSentenceFilesSub) {
//				System.out.println(f2.getAbsolutePath());
//
//				if (f2.getName().contains("QTR" + q)) {
//					Post2Solr p2s = new Post2Solr(f2.getAbsolutePath(), core)
//							.withCommitAfterDocCount(amountPriorToCommit);
//					p2s.postNow();
//					System.out.println("DONE..");
//				}
//
//			}
//		}
//	}

	public static void main(String[] arg) throws FileNotFoundException, XPathExpressionException, IOException,
			ParserConfigurationException, SAXException, SolrServerException {

		/*
		 * boolean parseSentence = false; // 1997. then refresh once more. for (int
		 * y=205; y < 2015; y++) { int q=1; if(y==1995) q=3; for (q=1; q < 5; q++) {
		 * post2solrByYearQuarter(y, q, "core3", parseSentence); } }
		 */
		/*
		 * parseSentence = true; // if false then parses paragraphs. for (int y=19; y <
		 * 2019; y++) { int q=1; if(y==1995) q=3; for (q=1; q < 5; q++) {
		 * post2solrByYearQuarter(y, q, "core1", parseSentence); } }
		 */

		/*
		 * String strippedQtrPath = SolrPrep.paragraphsXMLPath; File sentenceFolder =
		 * new File(strippedQtrPath);
		 * 
		 * File[] listOfSentenceFiles = sentenceFolder.listFiles();
		 */
		// this posts the folder- I don't need to specify the file

		// }

		// this posts the folder- I don't need to specify the file
		// i need to grab a sample

		// kIdFirst10 = ""; priorkIdFirst10 = "";
		// String strippedQtrPath = "c:/getContracts/sent/";
		// String sentencesQtrPath = "c:/getContracts/sentences/";
		// File sentenceFolder = new File(sentencesQtrPath);
		// File[] listOfSentenceFiles = sentenceFolder.listFiles();

		Post2Solr p2s = new Post2Solr("D:/getContracts/tmp/clauses/", "custody").withCommitAfterDocCount(20);
		p2s.postNow();
		System.out.println("DONE..");
		p2s = new Post2Solr("D:/getContracts/tmp/sentences/", "custody").withCommitAfterDocCount(20);
		p2s.postNow();
		System.out.println("DONE..");
		p2s = new Post2Solr("D:/getContracts/tmp/paragraphs/", "custody").withCommitAfterDocCount(20);
		p2s.postNow();
		System.out.println("DONE..");
//		String contractType = "psa";
//		String tmpQtrPath = "";
//		String year = "2018";//don't need to specify year - and it will run through all folders.
//		int amountPriorToCommit = 10;
//		List<String> listPaths = new ArrayList<>();
//		tmpQtrPath = "c:/getContracts/solrIngestion/solrSentences/" 
//		+ contractType + "/" + year + "/"
		;
		// listPaths.add(tmpQtrPath);
//		tmpQtrPath = "c:/getContracts/solrIngestion/solrParagraphs/";
//		listPaths.add(tmpQtrPath);//NEXT!
//		tmpQtrPath = "c:/getContracts/solrIngestion/solrSections/";
//		listPaths.add(tmpQtrPath);
//		tmpQtrPath = "c:/getContracts/solrIngestion/solrParentChild/";
//		listPaths.add(tmpQtrPath);

//		System.out.println("enter core");
//		Scanner Scan = new Scanner(System.in);
//		String core = Scan.nextLine();

//		System.out.println("how many files to commit to solr db at a time? recommended is 10, but can be 1");
//		Scan = new Scanner(System.in);
//		amountPriorToCommit = Integer.parseInt(Scan.nextLine());
		// automatically parses each subfolder
//		for (int i = 0; i < listPaths.size(); i++) {
//			Post2Solr p2s = new Post2Solr(listPaths.get(i), core)
//					.withCommitAfterDocCount(amountPriorToCommit);
//			p2s.postNow();
//			System.out.println("DONE..");
//		}
	}// next 2018 indenture and psa
}
