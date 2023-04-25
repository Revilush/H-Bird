package contracts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;

import util.GoLawLancasterStemmer;
import xbrl.ContractParser;
import xbrl.MysqlConnUtils;
import xbrl.NLP;
import xbrl.Utils;

public class Utilities {

	public static TreeMap<String, String> mapUniqWords = new TreeMap<>();
	public static List<String> listWords = new ArrayList<String>();
	public static List<String[]> listCommentaryWordsOrClauses = new ArrayList<>();

	// from data from xml file field termName

//	public static Pattern patternEventOfDefault = Pattern.compile("Events? of Default|(Special )?Servicer Termination Event");
//	public static String synonymsEventOfDefault = "Administrative Agent Termination Event"
//			+ "|Event of Default|Events of Default|Servicer Termination Event"
//			+ "|Events? of Servicing Termination"
//			+ "|Special Servicer Termination Event";
//	public static String synonymsConstantEventOfDefault = "Default";	

	public static Pattern patternAgreementSimple = Pattern.compile("(?<=this )([A-Z]{1}[a-z]{1,20}[, ]{1})+");

	public static String synonymsAgreement = "(Pass Through) Trust Agreement|Preferred Securities Guarantee"
			+ "|(Trust|Note|Guarantee|Subordinated|Security) Agreement" + "|Subordinated Indenture"
			+ "|Declaration|Guarantee|Indenture";
	public static String synonymConstantAgreement = "Agreement";

	public static Pattern patternNote = Pattern.compile("(?<=the )([A-Z]{1}[a-z]{1,20}[, ]{1})+");
	public static String synonymsNote = "Securities";
	public static String synonymConstantNote = "the Note";

	public static Pattern patternTrustee = Pattern.compile("([A-Z]{1}[a-z]{1,20} )+Trustee");
	public static String synonymsTrustee = "(Funding Note Indenture|Preferred Guarantee|Senior Discount Notes"
			+ "|Capital Markets|Credit Facility|Pass Through|Bond"
			+ "|Debenture|Delaware|Facility|Guarantee|Indenture|Institutional|Insurance"
			+ "|Notes?|Owner|Property|Slot) Trustee";
	public static String synonymConstantTustee = "Trustee";

	public static Pattern patternPrudentPerson = Pattern.compile("prudent (([a-z]{1,20} )|([A-Z]{1}[a-z]{1,20} )+)");
	public static String synonymsPrudentPerson = "prudent (corporate( indenture)?( trustee)?"
			+ "|Person|man|(indenture |institutional )?trustee|indenture"
			+ "|institutional|institution|investor|[Mm]anager|[Pp]ersons?|trustee|Individual|individual)";
	public static String synonymConstantprudentPerson = "prudent person";

	public static Pattern patternIf = Pattern
			.compile("" + "(?i)so long as any|(in case (of )?|in the event of |upon |so long as)");
	public static String synonymsIf = "" + "(?i)(so long as any ?|in case (of )?|in the event of ?"
			+ "|upon |If one or more (of )?(the )?|if any ?|if an ?)";
	public static String synonymConstantIf = "if a ";

	// case sensitive!

	public static Pattern patternAgreement = Pattern
			.compile("(?sm)(([A-Z\\#\\d]{1,30}( [toTO]{2}| [ANDand]{3})?|[A-Z\\#\\d]{1}[a-z]{1,30}"
					+ "( [toTO]{2}| [ANDand]{3})?) )+" + "(Agreement|AGREEMENT|GUARANTEE|Guarantee)[ \t]{0,20}$");

	public static Pattern patternAgreementIndenture = Pattern
			.compile("(?sm)(SUBORDINATED (NOTE )?|Subordinated (Note )?"
					+ "|FORM OF |Form [Oo]{1}f )?( ?Indenture| ?INDENTURE)" + "(?=[ \t]{0,20}$.{1,20}( ?DATE| ?Date))");

	public static Pattern patternAgreementDatedOrEndofLine = Pattern
			.compile("(?sm)((\\d?-?[A-Z]{1,30}\\d?-?|(NO\\. \\d)|\\d?-?[A-Z]{1}[a-z]{1,30}\\d?-?|(No\\. \\d)) )+"
					+ "(Agreement|AGREEMENT|GUARANT(Y|EE)|Guarant(y|ee))(?=[ \t]{0,20}$|  ?Dated?|  ?DATED?)");

	// ONLY DO ALL CAPS?
	public static Pattern patternAgreementAmendment = Pattern
			.compile("(?sm)(?<=(THIS|This) )(([A-Z]{1,30}|[A-Z]{1}[a-z]{1,30}) )+" + "(AMENDMENT|Amendment) ?"
					+ "((([A-Z]{1,30}|[A-Z]{1}[a-z]{1,30}) )+(Agreement|AGREEMENT))?(?=.{1,25}( ?DATE| ?Date|  ?date))");

	public static Pattern patternAgreementBetweenAmong = Pattern
			.compile("(?sm)(?<=THIS )(([A-Z\\d\\(\\)]{1,30}|[A-Z]{1}[a-z]{1,30}) )"
					+ "+(AGREEMENT)(?=.{1,25}( ?(AMONG|BETWEEN)| ?(Among|Between)|  ?(among|between)))");
//	<=== will the above capture this ===>Commercial Paper Dealer Agreement 4(a)(2) Program    Between?

	public static Pattern patternAgreementConsent = Pattern
			.compile("(?ism)(CONSENT OF.{1,50}(ACCOUNTING|AUDITOR).{1,50}$)");

	public static Pattern patternAgreementForm = Pattern.compile(
			"(?ism)^ ?Form 10-D ?$|^ ?Form T-1 ?$|^ ?Form S-3 ?$|Form of.{1,40}" + "(Note|Bond|Certificate|Security)");

	// this creates the name so I'm not capturing regex that simply identifies it is
	// an opinion that is not an inherent naming convention

	public static Pattern patternAgreementOfficerCertificate = Pattern.compile("(?ism)OFFICER['S]{0,2} CERTIFICATE ?$");

	public static Pattern patternAgreementRegistration = Pattern
			.compile("(?ism)^ ?registration (rights )?(agreement|statement) ?$");

//	public static Pattern patternAgreementNameTypes = Pattern.compile(
//			"("+patternAgreement.toString()+")" +"|"+ 
//					"("+patternAgreementIndenture.toString()+")"+"|"+
//					"("+patternAgreementDatedOrEndofLine.toString()+")"+"|"+
//					"("+patternAgreementBetweenAmong.toString()+")" + "|"+
//					"("+patternAgreementAmendment.toString()+")"+"|"+
//					"("+patternAgreementConsent.toString()+")"+"|"+
//					"("+patternAgreementOpin.toString() +")"+"|"+ 
//			"("+patternAgreementOpin2.toString()+")"+"|"+
//			"("+patternAgreementOfficerCertificate.toString()+")"
//			+"|"+
//			"("+patternAgreementRegistration.toString()+")"+"|"+
//			"("+patternAgreementForm.toString()+")"
//			);

	public static Pattern patternAgtName = Pattern.compile("(" + patternAgreement.toString() + ")" + "|" + "("
			+ patternAgreementIndenture.toString() + ")" + "|" + "(" + patternAgreementDatedOrEndofLine.toString() + ")"
			+ "|" + "(" + patternAgreementBetweenAmong.toString() + ")" + "|" + "("
			+ patternAgreementAmendment.toString() + ")" + "|" + "(" + patternAgreementConsent.toString() + ")" + "|"
			+ "(" + patternAgreementOfficerCertificate.toString() + ")" + "|" + "("
			+ patternAgreementRegistration.toString() + ")" + "|" + "(" + patternAgreementForm.toString() + ")");

	public static String sins(String text, String contractType, String legalTerm, String queryName) throws IOException {
//		folderLegalTermQueries
		NLP nlp = new NLP();

		/*
		 * String txtRepl = Utils.readTextFromFile(LegalTerms.folderLegalTermQueries +
		 * "/" + contractType + "/"+legalTerm+"/" + queryName.replaceAll("\\.txt",
		 * "_sin\\.txt")); // String txtRepl = // Utils.readTextFromFile(
		 * "c:/getContracts/queryLegalTerms/indenture/indenture_sent_prudent_person_syn.txt"
		 * ); // System.out.println("replacing txt - sins"); List<String> list =
		 * nlp.getAllMatchedGroups(txtRepl,
		 * Pattern.compile("(?<=replace=).+?(?=\r\n)|(?<=with=).+?(?=\r\n)")); for(int
		 * i=0; i<list.size(); i++) { txt = txt.replaceAll(list.get(i),
		 * list.get(i+1)).replaceAll("Xx", " ").trim().replaceAll("[ ]+", " ").trim();
		 * // System.out.println("i"+i+" repl="+list.get(i)+" with="+list.get(i+1));
		 * i++; }
		 */
//		System.out.println("1a. aft txt sin.repl=" + txt);

//		for (int c = 0; c < NLPlegalTerms.listSins.size(); c++) {
		/*
		 * System.out.println("text cleanup bef repl="+text.substring(0,18)); text =
		 * text.replaceAll("(?sm)^" + "([\\d]+ \\([a-zA-Z]{1,4}\\) ?" + "|Page ?|PAGE ?"
		 * + "|Ex ?[\\d\\.]+ ?- ?[\\d\\.]+ \\([a-zA-Z]{1,4}\\) ?" +
		 * "|-? ?[\\d]+- ?[\\d]+ ?\\(c\\)  ?" + "|[A-Z]+ \\([a-zA-Z]{1,4}\\) ? ?"
		 * 
		 * + "[a-zA-Z]+[\\d\\.]+ \\(\\d\\) ?" +
		 * "|-? ?[\\d]+- ?[\\d]+ \\([a-zA-Z]{1,4}\\) ?" + "|[\\d\\.]+  ?" +
		 * "|\\[\\[.*?]\\](( \\([a-zA-Z]{1,4}\\))?  ?)?" + "|TABLE OF C.*? ?" +
		 * "|Table of C.*? " + "|TABLE Of C.*? )" + "|" + "\\([A-Za-z\\d]{1,4}\\) ?|" +
		 * "[A-Z]{2}.*? " // + ")"//?delete ,"") .trim();
		 */
//			System.out.println("aft replXXXXX="+text.substring(0,18));

		// don't trim()!!!!!
//			Pattern p = Pattern.compile(NLPlegalTerms.listSins.get(c)[1]);
//			Matcher m = p.matcher(text);
//			if (m.find()) {
//				System.out.println("p="+NLPlegalTerms.listSins.get(c)[1]);
//				System.out.println("text sins bef="+text.substring(0,18));
//				text = m.replaceAll(NLPlegalTerms.listSins.get(c)[2]);
//			    System.out.println("m.repl="+NLPlegalTerms.listSins.get(c)[2]);
//			    System.out.println("text sins aftXXXX="+text);
//			}
//		}

		return text;

	}

	public static String cleanup(String text, String contractType, String legalTerm, String queryName)
			throws IOException {
//		folderLegalTermQueries
		NLP nlp = new NLP();

//		System.out.println("listCleanup.siz="+listCleanup);
//		if(NLPlegalTerms.listCleanup.size()==0)
//			return text;
//		for (int c = 0; c < NLPlegalTerms.listCleanup.size(); c++) {
		/*
		 * System.out.println("text cleanup bef repl="+text.substring(0,18)); text =
		 * text.replaceAll("(?sm)^" + "([\\d]+ \\([a-zA-Z]{1,4}\\) ?" + "|Page ?|PAGE ?"
		 * + "|Ex ?[\\d\\.]+ ?- ?[\\d\\.]+ \\([a-zA-Z]{1,4}\\) ?" +
		 * "|-? ?[\\d]+- ?[\\d]+ ?\\(c\\)  ?" + "|[A-Z]+ \\([a-zA-Z]{1,4}\\) ? ?"
		 * 
		 * + "[a-zA-Z]+[\\d\\.]+ \\(\\d\\) ?" +
		 * "|-? ?[\\d]+- ?[\\d]+ \\([a-zA-Z]{1,4}\\) ?" + "|[\\d\\.]+  ?" +
		 * "|\\[\\[.*?]\\](( \\([a-zA-Z]{1,4}\\))?  ?)?" + "|TABLE OF C.*? ?" +
		 * "|Table of C.*? " + "|TABLE Of C.*? )" + "|" + "\\([A-Za-z\\d]{1,4}\\) ?|" +
		 * "[A-Z]{2}.*? " // + ")"//?delete ,"") .trim();
		 */
//			System.out.println("aft replXXXXX="+text.substring(0,18));
//			Pattern p = Pattern.compile(NLPlegalTerms.listCleanup.get(c)[1]);
//			Matcher m = p.matcher(text.trim());
//			if (m.find()) {
//				System.out.println("p="+listCleanup.get(c)[1]);
//				System.out.println("m.repl="+listCleanup.get(c)[2]);
//				System.out.println("text cleanup bef="+text.substring(0,18));
//			    text = m.replaceAll(NLPlegalTerms.listCleanup.get(c)[2].trim());
//			    System.out.println("m.repl="+listCleanup.get(c)[2]);
//			    System.out.println("text cleanup aftXXXX="+text.substring(0,18));
//			}
//		}

		if (text.length() == 0) {
			return "";
		}

		return text.trim();

	}

	public static List<String> getFields(String text) throws IOException {

		NLP nlp = new NLP();
		List<String> list = new ArrayList<>();
		StringBuilder sb = new StringBuilder();

		Pattern patternTxt = Pattern.compile("\\[\\[txt:.*?\r\n");
		Pattern patternTxtCnt = Pattern.compile("(?sm)\\|\\|txtCnt: [\\d]+(?=\r\n)");
		Pattern patternTxtIdStr = Pattern.compile("\\|\\|txtId: [\\d]+(?=\r\n)");
		Pattern patternScore = Pattern.compile("(\\|\\|score: )[\\d\\.]+(?=\r\n)");
		Pattern patternId = Pattern.compile("(?sm)\\|\\|id:.+?(?=\r\n)");
		Pattern patternSec = Pattern.compile("(?sm)\\|\\|sec:.+?(?=\r\n)");
		Pattern patternExh = Pattern.compile("(?sm)\\|\\|exh:.+?(?=\r\n)");
		Pattern patternDef = Pattern.compile("(?sm)\\|\\|def:.+?(?=\r\n)");
		Pattern patternLead = Pattern.compile("(?sm)\\|\\|lead:.+?(?=\r\n)");
		Pattern patternLL = Pattern.compile("(?sm)\\|\\|LL:.+?(?=\r\n)");
		Pattern patternPL = Pattern.compile("(?sm)\\|\\|PL:.+?(?=\r\n)");
		Pattern patternL = Pattern.compile("(?sm)\\|\\|L:.+?(?=\r\n)");
		Pattern patternPorC = Pattern.compile("(?sm)\\|\\|pOrC:.+?(?=\r\n)");

		sb.append(nlp.getAllMatchedGroups(text, patternTxt).get(0) + "\r\n");
		sb.append(nlp.getAllMatchedGroups(text, patternScore).get(0) + "\r\n");
		sb.append(nlp.getAllMatchedGroups(text, patternId).get(0) + "\r\n");
		sb.append(nlp.getAllMatchedGroups(text, patternTxtCnt).get(0) + "\r\n");
		sb.append(nlp.getAllMatchedGroups(text, patternTxtIdStr).get(0) + "\r\n");

		if (nlp.getAllMatchedGroups(text, patternSec).size() > 0) {
			sb.append(nlp.getAllMatchedGroups(text, patternSec).get(0) + "\r\n");
		}
		if (nlp.getAllMatchedGroups(text, patternExh).size() > 0) {
			sb.append(nlp.getAllMatchedGroups(text, patternExh).get(0) + "\r\n");
		}
		if (nlp.getAllMatchedGroups(text, patternDef).size() > 0) {
			sb.append(nlp.getAllMatchedGroups(text, patternDef).get(0) + "\r\n");
		}
		if (nlp.getAllMatchedGroups(text, patternLead).size() > 0) {
			sb.append(nlp.getAllMatchedGroups(text, patternLead).get(0) + "\r\n");
		}
		if (nlp.getAllMatchedGroups(text, patternLL).size() > 0) {
			sb.append(nlp.getAllMatchedGroups(text, patternLL).get(0) + "\r\n");
		}
		if (nlp.getAllMatchedGroups(text, patternPL).size() > 0) {
			sb.append(nlp.getAllMatchedGroups(text, patternPL).get(0) + "\r\n");
		}
		if (nlp.getAllMatchedGroups(text, patternL).size() > 0) {
			sb.append(nlp.getAllMatchedGroups(text, patternL).get(0) + "\r\n");
		}
		if (nlp.getAllMatchedGroups(text, patternPorC).size() > 0) {
			sb.append(nlp.getAllMatchedGroups(text, patternPorC).get(0) + "\r\n");
		}

		list.add(sb.toString());
		return list;

	}

	public static Double getMaxscore(String filename) throws IOException {

		NLP nlp = new NLP();
		Double maxScore = 0.0;
		String text = Utils.readTextFromFile(filename);

		maxScore = Double
				.parseDouble(nlp.getAllMatchedGroups(text, Pattern.compile("(?<=maxScore\":)[\\d\\.]{3,7}")).get(0));
		System.out.println("maxScore=" + maxScore);
		return maxScore;
	}

	public static String getSolrFieldsFromTxtFile(String items) throws IOException {
		// NOTE: return full search results of sections extracted below. I can add
		// additional extractions based on what is in the search results. It currently
		// doesn't extract POS

		StringBuilder sb = new StringBuilder();
		// List<String> list = new ArrayList<>();
		NLP nlp = new NLP();
//		String paraSentCl = 
//				paragraphSentenceClause.replaceAll("paragraph", "para").replaceAll("sentence", "sent");
//		System.out.println("items===="+items);
// List<String> listparaStopStem= nlp.getAllMatchedGroups(items,
//		Pattern.compile("(?sm)(\""+paraSentCl+"StopStem\":\\[\").*?.(\"\\])"));
//		if(listparaStopStem.size()>0) {
//			sb.append("||"+paraSentCl+"StopStem: " + listparaStopStem.get(0)+"\r\n");
//			System.out.println("StopStem====: " + listparaStopStem.get(0)); 
//		}
//		List<String> listparaStopStemNoDef= nlp.getAllMatchedGroups(items, 
//				Pattern.compile("(?sm)(\""+paraSentCl+"StopStemNoDef\":\\[\").*?.(\"\\])"));

//		if(listparaStopStemNoDef.size()>0) {
//			sb.append("||"+paraSentCl+"StopStemNoDef: "+listparaStopStemNoDef.get(0)+"\r\n");
//		}
//		List<String> listPOS= nlp.getAllMatchedGroups(items,
//				Pattern.compile("(?sm)(?<=\"POS\":\\[\").*?.(?=\"\\])"));
//		if(listPOS.size()>0) {
//			 sb.append("\r\n||POS: "+listPOS.get(0)+"\r\n");
//		}

		List<String> listParaSentClause = nlp.getAllMatchedGroups(items,
				Pattern.compile("(?sm)(\"txt\":\\[\").*?.(\"\\])"));
		if (listParaSentClause.size() > 0) {
			sb.append("txt: " + listParaSentClause.get(0) + "\r\n");
		}

		List<String> listLeadIn = nlp.getAllMatchedGroups(items,
				Pattern.compile("(?sm)(?<=\"lead\":\\[\").*?.(?=\"\\])"));
		if (listLeadIn.size() > 0) {
			sb.append("||lead: " + listLeadIn.get(0) + "\r\n");
		}

		List<String> listSectionHeading = nlp.getAllMatchedGroups(items,
				Pattern.compile("(?sm)(?<=\"sec\":\\[\").*?(?=\"\\])"));
		if (listSectionHeading.size() > 0) {
			sb.append("||sec: " + listSectionHeading.get(0) + "\r\n");
		}

		List<String> listExhibitHeading = nlp.getAllMatchedGroups(items,
				Pattern.compile("(?sm)(?<=\"exh\":\\[\").*?(?=\"\\])"));
		if (listExhibitHeading.size() > 0) {
			sb.append("||exh: " + listExhibitHeading.get(0) + "\r\n");
		}

		List<String> listDefinitionHeading = nlp.getAllMatchedGroups(items,
				Pattern.compile("(?sm)(?<=\"def\":\\[\").*?" + "(?=\"\\])"));
		if (listDefinitionHeading.size() > 0) {
			sb.append("||def: " + listDefinitionHeading.get(0) + "\r\n");
		}

		List<String> listId = nlp.getAllMatchedGroups(items, Pattern.compile("(?sm)(?<=\"id\":\")[\\d]{10}.*?(?=\")"));
		if (listId.size() > 0)
			sb.append("||id: " + listId.get(0) + "\r\n");

		List<String> listkId = nlp.getAllMatchedGroups(items, Pattern.compile("(?sm)(?<=,\"kId\":\").*?(?=\")"));
		if (listkId.size() > 0)
			sb.append("||kId: " + listkId.get(0) + "\r\n");

		List<String> listTyp = nlp.getAllMatchedGroups(items, Pattern.compile("(?sm)(?<=\"typ\":)[\\d\\.]{1,2}"));
		if (listTyp.size() > 0) {
			sb.append("||typ: " + listTyp.get(0) + "\r\n");
		}

		List<String> listScore = nlp.getAllMatchedGroups(items, Pattern.compile("(?sm)(?<=\"score\":)[\\d\\.]{1,20}"));
		if (listScore.size() > 0) {
			sb.append("||score: " + listScore.get(0).substring(0, Math.min(listScore.get(0).length(), 5)) + "\r\n");
		}

		return sb.toString();

	}

	public static void checkSentenceNoAndIdsAreConsecutive(String filepath) throws IOException {

		/*
		 * This method when pointed to a filepath it will run through all the files
		 * prepared for solr ingestion and check that the sentence nos are consecutive
		 * eg path ="c:/getContracts/sentences/indenture/1996/QTR4/"
		 */

		File files = new File(filepath);
		NLP nlp = new NLP();

//		StringBuilder sb = new StringBuilder();

		File[] listOfFiles = files.listFiles();
		int sentenceNo = 0, priorSentNo = 0, id = 0, priorId = 0;
		String line = "", xmlFile = "", sentStrNo = "", idStr = "";
		// System.out.println("listOfFiles.len="+listOfFiles.length);
		int cnt = 0;
		for (File file : listOfFiles) {
			cnt = 0;
			xmlFile = Utils.readTextFromFile(file.getAbsolutePath());
			// System.out.println("xmlFile.leng=" + xmlFile.length() + " path=" +
			// file.getAbsolutePath());
			String[] lines = xmlFile.split("\r\n");
			// System.out.println("lines.leng="+lines.length);

			for (int i = 0; i < lines.length; i++) {
				line = lines[i];

				if (nlp.getAllMatchedGroups(line,
						Pattern.compile("[\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,2}_[\\d]{1,6}_[\\d]{1,6}")).size() > 0) {
					idStr = nlp
							.getAllMatchedGroups(line,
									Pattern.compile(
											"(?<=[\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,2}_[\\d]{1,6}_)[\\d]{1,6}"))
							.get(0);
					id = Integer.parseInt(idStr);
					if (id > 0 && id - priorId != 1) {
						System.out.println("error. file=" + file.getName() + " id=" + id + " priorId=" + priorId);
					}
					priorId = id;
				}

				if (line.contains("<field name=\"sentenceNo\">")) {
					// System.out.println(line);
					sentStrNo = line.substring(25, line.length());
					sentStrNo = sentStrNo.substring(0, sentStrNo.indexOf("<"));
//					System.out.println("sentStrNo:" + sentStrNo);
					sentenceNo = Integer.parseInt(sentStrNo);
					if ((sentenceNo > 0 && sentenceNo - priorSentNo != 1) || sentenceNo != cnt) {
						System.out.println("error. file=" + file.getName() + " sentNo=" + sentenceNo + " cnt=" + cnt
								+ " priorSentNo=" + priorSentNo);

					}
					cnt++;
					priorSentNo = sentenceNo;
				}
			}
		}
	}

	public static void checkSentenceNoAndIdsAreConsecutiveAndFix(String filepath) throws IOException {

		boolean fix = false;
		System.out.println(
				"don't set fix=true - until you fix it until you fix SolrPrep method. turn off fix=false here. fix="
						+ fix);

		/*
		 * This method when pointed to a filepath it will run through all the files
		 * prepared for solr ingestion and check that the sentence nos are consecutive
		 * eg path ="c:/getContracts/sentences/indenture/1996/QTR4/"
		 */

		File files = new File(filepath);
		NLP nlp = new NLP();

		File f = new File("c:/getContracts/tmp.txt");
		PrintWriter pw = new PrintWriter(f);

		StringBuilder sb = new StringBuilder();

		File[] listOfFiles = files.listFiles();
		int sentNo = 0, priorSentNo = 0, sentNoFromId = 0
//				,priorSentNoFromId = 0, id = 0, priorId = 0
		;
		String line = "", xmlFile = "", sentNoStr = "", sentNoStrFromId = "", /* idStr = "", */ idCorrected = "",
				idToReplace = "";
		int cnt = 0;
		boolean error = false;
//		System.out.println("listOfFiles.len="+listOfFiles.length);
		for (File file : listOfFiles) {
			cnt = 0;
			error = false;
			xmlFile = Utils.readTextFromFile(file.getAbsolutePath());
			// System.out.println("xmlFile.leng=" + xmlFile.length() + " path=" +
			// file.getAbsolutePath());

			List<String> listSentNos = nlp.getAllMatchedGroups(xmlFile,
					Pattern.compile("(?<=\"sentenceNo\">)[\\d]{1,6}(?=</field>)"));
			List<String> listIdSentNos = nlp.getAllMatchedGroups(xmlFile,
					Pattern.compile("(?<=[\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,2}_)[\\d]{1,6}"));

			priorSentNo = -1;
//			System.out.println(
//					file.getName() + " listIdSentNos.size=" + listIdSentNos + " listSentNo.size=" + listSentNos.size());
			for (int n = 0; n < listIdSentNos.size(); n++) {
				sentNoStr = listSentNos.get(n).trim();
				sentNo = Integer.parseInt(sentNoStr);

				sentNoStrFromId = listIdSentNos.get(n).trim();
				sentNoFromId = Integer.parseInt(sentNoStrFromId);

				if (sentNo != sentNoFromId || sentNo - priorSentNo != 1) {
					System.out.println(
							"error - sentNo!=idSentNo. sentNo=" + sentNoStr + " sentNoFromId=" + sentNoStrFromId);
					System.out.println("error - sentNo=" + sentNo + " priorSentNo=" + priorSentNo);
					error = true;
					break;
				}

//				if (sentNo == sentNoFromId && sentNo - priorSentNo == 1) {
//					System.out.println(
//							"1. sentNo=\t" + sentNo + " sentNoFromId=\t" + sentNoFromId + " priorSentNo=\t" + priorSentNo);
//				}

				priorSentNo = sentNo;
			}

			if (!error)
				System.out.println("no error in file " + file.getAbsolutePath());

			if (error)
				System.out.println(
						"error in file " + file.getAbsolutePath() + " don't fix it until you fix SolrPrep method");

			if (error && fix) {

				String[] lines = xmlFile.split("\r\n");
				// System.out.println("lines.leng="+lines.length);

				cnt = 0;
				for (int i = 0; i < lines.length; i++) {
					line = lines[i];

					if (nlp.getAllMatchedGroups(line,
							Pattern.compile("[\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,2}_[\\d]{1,6}")).size() > 0) {

						cnt++;

						List<String> listkIdPlusLastHyphen = nlp.getAllMatchedGroups(line,
								Pattern.compile("([\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,2}_)"));

						// removed old sentNo in id - and replaced with updated due to clause parsing
						idCorrected = listkIdPlusLastHyphen.get(0) + (cnt + "");

						List<String> listIdToRepladeWithCorrectId = nlp.getAllMatchedGroups(line,
								Pattern.compile("([\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,2}_[\\d]{1,6})"));
						idToReplace = listIdToRepladeWithCorrectId.get(0);

						line = line.replaceAll(idToReplace, idCorrected);
						sb.append(line + "\r\n");
						continue;
					}

					if (line.contains("<field name=\"sentenceNo\">")) {
						line = line.replaceAll("[\\d]{1,6}", cnt + "");
						sb.append(line + "\r\n");
//						System.out.println("sent="+line);
						continue;
					}

					System.out.println("other=" + line);
					sb.append(line + "\r\n");
				}

				if (file.exists())
					file.delete();

				pw = new PrintWriter(file);
				pw.println(file);
				pw.close();
			}
		}
	}

	

	

	/*
	 * public static String tmpCleanup(String text) throws IOException {
	 * 
	 * text = text.replaceAll("[-\\_]{2,}", " ").replaceAll("[ ]+", " ");
	 * 
	 * // text = text.replaceAll( // "(((^ ?|\\r\\n ?)[\\d\\.]{0,4}" // +
	 * "(((Certain|CERTAIN|SECTION|Section).{1,20})?(DUT[IESY]{1,3}|Dut[iesy]{3}).{1,30}(Trustee|TRUSTEE).{1,10}\\([A-Za-z\\d]+\\) ?)))|(^ ?|\r\n ?)\\([a-zA-Z\\d]+\\) ?|(^ ?|\r\n ?)"
	 * , // "").replaceAll("[ ]+", " ");// removes erroneous subheadings
	 * 
	 * text =
	 * text.replaceAll("(?i); (provided|nothing).*?$|; and (provided|nothing).*?$" +
	 * "|, provided, however,.*?$|, however,.*?$"," ").replaceAll("[ ]+", " ");
	 * return text;
	 * 
	 * }
	 */

	/*
	 * public static void getTrusteeIsPrudentPersonListReady() {
	 * 
	 * List<String>listTrusteePrudentPersonSynonyms = new ArrayList<>();
	 * 
	 * listTrusteePrudentPersonSynonyms.add(synonymsPrudentPerson);
	 * listTrusteePrudentPersonSynonyms.add(synonymConstantprudentPerson);
	 * listTrusteePrudentPersonSynonyms.add(synonymsTrustee);
	 * listTrusteePrudentPersonSynonyms.add(synonymConstantTustee);
	 * listTrusteePrudentPersonSynonyms.add(synonymsEventOfDefault);
	 * listTrusteePrudentPersonSynonyms.add(synonymsConstantEventOfDefault);
	 * listTrusteePrudentPersonSynonyms.add(synonymsIf);
	 * listTrusteePrudentPersonSynonyms.add(synonymConstantIf);
	 * listTrusteePrudentPersonSynonyms.add(synonymsInitialCaps);
	 * listTrusteePrudentPersonSynonyms.add(synonymConstantInitialCaps);
	 * 
	 * // listTrusteePrudentPersonSynonyms.add(synonymsNote); //
	 * listTrusteePrudentPersonSynonyms.add(synonymConstantNote);
	 * 
	 * }
	 * 
	 */
	/*
	 * public static void findSynonymsAgreement(String text, boolean
	 * showOnlyMissingSynonyms) throws IOException {
	 * 
	 * 
	 * see method findSynonymsTrustee for description of the purpose of this method
	 * 
	 * 
	 * NLP nlp = new NLP();
	 * 
	 * System.out.
	 * println("findSynonymsAgreement. showing Agreements not included in synonymsAgreement="
	 * + showOnlyMissingSynonyms);
	 * 
	 * String[] lines = text.split(",\\{"); String line = ""; TreeMap<String,String>
	 * map = new TreeMap<String, String>(); String str = ""; for (int i = 0; i <
	 * lines.length; i++) { line = lines[i]; List<String> list =
	 * nlp.getAllMatchedGroups(line, patternAgreementSimple);
	 * 
	 * if (list.size() > 0) { str = list.get(0).replaceAll(",", " ").trim();
	 * 
	 * if(showOnlyMissingSynonyms) str = str.replaceAll(synonymsAgreement, "");
	 * 
	 * map.put(str, str);
	 * 
	 * } }
	 * 
	 * NLP.printMapStrStr("", map);
	 * 
	 * }
	 * 
	 */
	/*
	 * public static void findSynonymsNote(String text, boolean
	 * showOnlyMissingSynonyms) throws IOException {
	 * 
	 * 
	 * see method findSynonymsTrustee for description of the purpose of this method
	 * 
	 * 
	 * NLP nlp = new NLP();
	 * 
	 * System.out.
	 * println("findSynonymsNote. showing Note not included in synonymsNote=" +
	 * showOnlyMissingSynonyms);
	 * 
	 * String[] lines = text.split(",\\{"); String line = ""; TreeMap<String,String>
	 * map = new TreeMap<String, String>(); String str = ""; for (int i = 0; i <
	 * lines.length; i++) { line = lines[i]; List<String> list =
	 * nlp.getAllMatchedGroups(line, patternNote);
	 * 
	 * if (list.size() > 0) { str = list.get(0).replaceAll(",", " ").trim();
	 * 
	 * if(showOnlyMissingSynonyms) str = str.replaceAll(synonymsNote, "");
	 * 
	 * map.put(str, str);
	 * 
	 * } }
	 * 
	 * NLP.printMapStrStr("", map);
	 * 
	 * }
	 */

	/*
	 * public static void findSynonymsPrudentPerson(String text, boolean
	 * showOnlyMissingSynonyms) throws IOException {
	 * 
	 * 
	 * see method findSynonymsTrustee for description of the purpose of this method
	 * 
	 * 
	 * NLP nlp = new NLP();
	 * 
	 * System.out.
	 * println("findSynonymsPrudentPerson. showing synonyms Prudent Person not included in synonymsPrudentPerson="
	 * + showOnlyMissingSynonyms);
	 * 
	 * String[] lines = text.split(",\\{"); String line = ""; TreeMap<String,String>
	 * map = new TreeMap<String, String>(); String str = ""; for (int i = 0; i <
	 * lines.length; i++) { line = lines[i]; List<String> list =
	 * nlp.getAllMatchedGroups(line, patternPrudentPerson);
	 * 
	 * if (list.size() > 0) { str = list.get(0).replaceAll(",", " ").trim();
	 * 
	 * 
	 * if(showOnlyMissingSynonyms) str = str.replaceAll(synonymsPrudentPerson, "");
	 * if(str.equals("prudent indenture")) // System.out.println("line="+line);
	 * 
	 * map.put(str, str);
	 * 
	 * } }
	 * 
	 * NLP.printMapStrStr("", map);
	 * 
	 * }
	 */

	/*
	 * public static void findSynonymsEventsOfDefault(String text, boolean
	 * showOnlyMissingSynonyms) throws IOException {
	 * 
	 * 
	 * see method findSynonymsTrustee for description of the purpose of this method
	 * 
	 * 
	 * NLP nlp = new NLP();
	 * 
	 * System.out.
	 * println("findSynonymsEventsOfDefault. showing Events of Default not included in synonymsEventOfDefault="
	 * + showOnlyMissingSynonyms);
	 * 
	 * System.out.println(
	 * "this trys to find items not homogenized by synonyms - BUT it sometimes shows items that have in fact been homogenized"
	 * );
	 * 
	 * 
	 * String[] lines = text.split(",\\{"); String line = ""; TreeMap<String,String>
	 * map = new TreeMap<String, String>(); String str = ""; for (int i = 0; i <
	 * lines.length; i++) { line = lines[i]; List<String> list =
	 * nlp.getAllMatchedGroups(line, patternEventOfDefault);
	 * 
	 * if (list.size() > 0) { str = list.get(0).replaceAll(",", " ").trim();
	 * 
	 * 
	 * if(showOnlyMissingSynonyms) str = str.replaceAll(synonymsEventOfDefault, "");
	 * map.put(str, str); if(str.equals("Default")|| str.equals("Event"))
	 * System.out.println("str="+str+" line="+line);
	 * 
	 * } }
	 * 
	 * NLP.printMapStrStr("", map);
	 * 
	 * }
	 * 
	 */

	/*
	 * public static void findSynonymsTrustee(String text, boolean
	 * showOnlyMissingSynonyms) throws IOException {
	 * 
	 * 
	 * using patternTrustee this will find from solr search result set or other text
	 * synonyms similar to that of a trustee. Based on synonymsTrustee string which
	 * I use to homogenize terms there are not trustee types included - then
	 * showOnlyMissingSynonyms if true will display those. Those displayed can then
	 * be used to expand synonymsTrustee string to improve homogenization process.
	 * 
	 * 
	 * NLP nlp = new NLP(); System.out.
	 * println("findTrusteeTypes. showing potential Trustee synonyms not included in synonymsTrustee="
	 * + showOnlyMissingSynonyms);
	 * 
	 * String[] lines = text.split(",\\{"); String line = ""; TreeMap<String,
	 * String> map = new TreeMap<String, String>(); String str = ""; for (int i = 0;
	 * i < lines.length; i++) { line = lines[i]; List<String> list =
	 * nlp.getAllMatchedGroups(line, patternTrustee);
	 * 
	 * if (list.size() > 0) { str = list.get(0).replaceAll(",", " ").trim(); str =
	 * str.replaceAll("The ", "").trim(); if(showOnlyMissingSynonyms) str =
	 * str.replaceAll(synonymsTrustee, ""); if (str.split(" ").length == 1)
	 * continue; map.put(str, str);
	 * 
	 * } }
	 * 
	 * NLP.printMapStrStr("", map);
	 * 
	 * }
	 * 
	 */

	/*
	 * public static String getAgreementName(File file) throws IOException {
	 * 
	 * String agreementName = file.getName();
	 * 
	 * NLP nlp = new NLP();
	 * 
	 * // File folder = new File("c:/getContracts/SampleContracts/tmp/"); // File[]
	 * listOfFiles = folder.listFiles(); String text = "", textsub = "";
	 * 
	 * 
	 * 
	 * TODO: Only use contract body when contract long name is incomplete - e.g., EX
	 * 4.1 - and in those cases use the regex tool to name the contract. If I decide
	 * to parse all contract types I could approach it how? But see also 8-K filings
	 * where cwabs don't give a description other than 'cwabs' And maybe for 8-Ks I
	 * continue to use regex.
	 * 
	 * 
	 * int filesize = 0, eIdx = 0, sIdx = 0; // for(int i=0; i<listOfFiles.length;
	 * i++) { text =
	 * Utils.readTextFromFile(file.getAbsolutePath()).replaceAll("xxPD|xxPD",
	 * "\\."); filesize = text.length(); textsub = text.substring(0,
	 * Math.min(text.length(), 1000)); List<String[]> listEidx =
	 * nlp.getAllEndIdxAndMatchedGroupLocs(textsub, patternAgtName); List<String[]>
	 * listSidx = nlp.getAllStartIdxLocsAndMatchedGroups(textsub, patternAgtName);
	 * 
	 * if(nlp.getAllMatchedGroups(file.getName(), Pattern
	 * .compile("(?ism)agreement|consent|form|instruction|" +
	 * "amendment|supplement|credit|registration (rights )?statememt" +
	 * "|notice|opinion")).size()>0 ) { System.out.
	 * println("from edgar description filename is complete?=\rc:/getContracts/SampleContracts/tmp/"
	 * +file.getName()); return agreementName; }
	 * 
	 * if(listEidx.size()==0) {
	 * System.out.println("XXX didn't find Agt name=\rc:/getContracts/temp/"+file.
	 * getName());
	 * System.out.println("filesize="+filesize+"|| textSub="+textsub.substring(0,
	 * Math.min(text.length(), 500))); return agreementName; }
	 * 
	 * for (int n = 0; n < listEidx.size();) {
	 * System.out.println("filename not complete=\rc:/getContracts/temp/" +
	 * file.getName());
	 * 
	 * eIdx = Integer.parseInt(listEidx.get(n)[0]); sIdx =
	 * Integer.parseInt(listSidx.get(n)[0]); System.out.println("txtSnippet=" +
	 * textsub.substring(sIdx - 5, eIdx + 5)); if (nlp.getAllMatchedGroups(textsub,
	 * SolrPrep.patternOpinion).size() > 0 && listSidx.get(n)[1].length() < 3) {
	 * 
	 * System.out.println("filesize=" + filesize + "\r" + "name=" + "Opinion");
	 * return "Opinion"; }
	 * 
	 * System.out.println("got agt name=" + listSidx.get(n)[1] + " filesize=" +
	 * filesize); return listSidx.get(n)[1]; }
	 * 
	 * 
	 * return agreementName;
	 * 
	 * }
	 */

	public static void checkSolrXmlTextIsSameAsStrippedText(String xmlTxtFolder, String stripTxtFolder)
			throws IOException {

		String stripTxt = Utils.readTextFromFile(stripTxtFolder);
		stripTxt = stripTxt.replaceAll("xxPD", "\\.").replaceAll("[ ]+\r\n|[\r\n]+[ ]+", "\r\n")
				.replaceAll("[\r\n\t]+", "\r\n").replaceAll("[ ]+", " ");
		File f = new File("c:/getContracts/temp/ck.txt");
		Utils.writeTextToFile(f, stripTxt);
		System.out.println("ck=" + stripTxt.length());

		String xmlTxt = Utils.readTextFromFile(xmlTxtFolder);
		NLP nlp = new NLP();

		StringBuilder sb = new StringBuilder();
		List<String> list = nlp.getAllMatchedGroups(xmlTxt,
				Pattern.compile("(?sm)(?<=txt\"><\\!\\[CDATA\\[)" + ".*?(?=]]>)"));

		for (int i = 0; i < list.size(); i++) {
			sb.append(list.get(i) + "\r\n");
		}

		xmlTxt = sb.toString().replaceAll("[ ]+\r\n|[\r\n]+[ ]+", "\r\n").replaceAll("[\r\n\t]+", "\r\n")
				.replaceAll("[ ]+", " ");
		System.out.println("xmlTxt.len=" + xmlTxt.length());
		File fn = new File("c:/getContracts/temp/xml.txt");
		Utils.writeTextToFile(fn, xmlTxt);

	}

	public static void loadIntoMysql(String filename, String table, String fieldTermination, String lineTermination,
			String database) throws SQLException, FileNotFoundException {
		// for \r\n -- line termination - it must be \\r\\n
//		File file = new File(filename);
//		long size = file.length();
		// System.out.println("loadFileIntoMysql fileSize:: " + size);
		String query = "LOAD Data INFILE '" + filename + "' INTO TABLE " + table + " FIELDS TERMINATED BY '"
				+ fieldTermination + "' lines terminated by '" + lineTermination + "';";
//		if (size > 0) {
			MysqlConnUtils.executeQueryDB(query, database);
			System.out.println("query=="+query);
//		}

	}

//	public static void createContractTypeFiles(List<String> listOfContractTypeFolders, int yrStart, int yrEnd,
//			int qtrStart, int qtrEnd) throws IOException, SQLException {
//
//		// for each contract type in the list - cycle through the related paragraph
//		// year/Qtr folders of xml files to retrieve from the first doc the contractType
//		// and other contract level 'metadata'
//
//		NLP nlp = new NLP();
//		StringBuilder sb = new StringBuilder();
//		String folderContract = "", folderMetadata = "c:/getContracts/solrIngestion/contractIds/";
//		PrintWriter pw = new PrintWriter(new File(folderMetadata + "/temp.txt"));
//		File folder = new File("");
//		File[] listOfFiles = folder.listFiles();
//		File file = new File("");
//		File f = new File("");
//		String text = "";
//
//		int qtr = qtrStart;
//		for (int i = 0; i < listOfContractTypeFolders.size(); i++) {
//			System.out.println("contractType=" + listOfContractTypeFolders.get(i));
//			sb = new StringBuilder();
//			for (int yr = yrStart; yr <= yrEnd; yr++) {
////				System.out.println("yr="+yr);
//				qtr = 1;
//				for (; qtr < 5; qtr++) {
////					System.out.println("qtr="+qtr);
////					folderContract = SolrPrep.folderParagraphsXml + listOfContractTypeFolders.get(i) + "/" + yr + "/QTR"
//							+ qtr + "/";
////					System.out.println("contractType folder==" + folderContract);
//					folder = new File(folderContract);
//					listOfFiles = folder.listFiles();
//					if (null == listOfFiles)
//						continue;
//					for (int c = 0; c < listOfFiles.length; c++) {
//						file = listOfFiles[c];
////						System.out.println("file=" + file.getAbsolutePath());
//						text = Utils.readTextFromFile(file.getAbsolutePath());
//						if (nlp.getAllMatchedGroups(text.substring(0, 1000),
//								Pattern.compile("(?sm)<add>\r\n<doc>.*?</doc>")).size() == 0)
//							continue;
//						text = nlp.getAllMatchedGroups(text.substring(0, 1000),
//								Pattern.compile("(?sm)<add>\r\n<doc>.*?</doc>")).get(0);
////						 System.out.println("doc0=" + text);
//						List<String> listDoc = nlp.getAllMatchedGroups(text,
//								Pattern.compile("(?sm)<field name=\".*?</field>"));
//						for (int d = 0; d < listDoc.size(); d++) {
//							text = listDoc.get(d)
//									.replaceAll("(?sm)</?field>|<field name=\"|<\\!\\[CDATA\\[|\\]\\]\\>", "")
//									.replaceAll("T00:00:00Z", "").replaceAll("\">", "=").replaceAll(".*?=", "");
////							System.out.println(text);
//							if (d + 1 < listDoc.size())
//								sb.append(text.replaceAll(",", "") + ",");
//							else
//								sb.append(text);
//						}
//						sb.append("\r\n");
//					}
//
//					f = new File(folderMetadata + "metadata_" + yr + "QTR" + qtr + listOfContractTypeFolders.get(i)
//							+ ".txt");
//
//					pw = new PrintWriter((f));
//					pw.append(sb.toString());
//					pw.close();
//
//					loadIntoMysql(f.getAbsolutePath().replaceAll("\\\\", "/"), "contracts.contract_metadata", ",",
//							"\r\n", "contracts");
//					System.out.println("loadIntoMysql=" + f.getAbsolutePath());
//				}
//			}
//		}
//	}

	public static void saveContractAsHtmlFromExportedMysqlContract_MetadataTable(String linksFilePath)
			throws IOException {

		/*
		 * this will take an exported file from mysql of the contractIds filepaths to th
		 * files in solringestion/downloaded I'm working with and save them to a links
		 * folder after converting them to a html- in some cases for txt files adding a
		 * <br> in replace of \r\n so it is read properly in chrome. This will be needed
		 * regardless when viewing as txt file. This allow a point and click to open the
		 * file versus going to the edgar site.
		 * 
		 * see query - C:\getContracts\mySql\links_export.sql
		 */

		NLP nlp = new NLP();
		String text = "";
//		c:/getContracts/solrIngestion/downloaded/1996/QTR4/0000003153-96-000024_2 EXHIBIT B BODYX INDENTURE.txt
		String links = Utils.readTextFromFile(linksFilePath), link = "";
		String[] linksSplit = links.split("\n");
		String linkFileHtml = "c:/getContracts/solrIngestion/links/";
		Utils.createFoldersIfReqd(linkFileHtml);
		PrintWriter pw = new PrintWriter(new File("c:/getcontracts/tmp.txt"));
		for (int i = 0; i < linksSplit.length; i++) {
			System.out.println("linksSplit.length=" + linksSplit.length);
			link = linksSplit[i].replaceAll("\\\\", "/").replaceAll("(?sm)\r|\n", "")
					.replaceAll("(?<=\\d\\d?) (?=[12]{1}[09]{1}[\\d]{2})", ", ");
//			File file = new File(link);
			System.out.println("link=" + link);
			File file = new File(link);
			if (!file.exists()) {
				System.out.println("continue");
				continue;
			}

			System.out.println("file.getAbsolutePath()=" + file.getAbsolutePath());
			if (!text.contains("(?ism)<html>")) {
				text = Utils.readTextFromFile(file.getAbsolutePath()).replaceAll("\r\n", "<br>")
//					.replaceAll("<br><br><br><br>", "<br><br>")
				;
			}

			File f = new File(linkFileHtml + link.substring(link.lastIndexOf("/")).replace(".txt", "2.html"));

			pw = new PrintWriter(f);
			pw.append(text);
			pw.close();
		}
	}

	public static String getUniqueCiksFilteredYrbyKidExtReturnIds(String text, double percentDif) throws IOException {

		/*
		 * filterInSolr() starts with results of initial solr query using legalTerms
		 * method and underlying solr query. The contractIds of these results are then
		 * filtered to exclude duplicates. Duplicats are defined as those that have
		 * identical CIKs and contract Ids in any given calendar year. After a calender
		 * year is finished a CIK / contract Id previously included can be included
		 * again. To get slightly higher diversity of contractType the unique id --
		 * cik_year_kidExtension is used. This will help capture instance of a senior
		 * and subordinate indenture for example - which beyond the basic .
		 */
//		System.out.println("text.len="+text.length());
//		System.out.println("text.snip=="+text.substring(100, 700));
		NLP nlp = new NLP();
		StringBuilder sb = new StringBuilder();

		// solr search has already filterd by fileSize and contractType
		Pattern patternkId = Pattern.compile("(?sm)(?<=id.{3})[\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,3}");
		Pattern patternScore = Pattern.compile("(?sm)(?<=score.{2})[\\d\\.]{1,6}");

		TreeMap<Double, String> map = new TreeMap<Double, String>();

		List<String> listkId = nlp.getAllMatchedGroups(text, patternkId);
		List<String> listScore = nlp.getAllMatchedGroups(text, patternScore);

		double key, score, scorePrev;
		String keyStr = "";
		System.out.println("listkId.size=" + listkId.size());
		for (int i = 0; i < listkId.size(); i++) {
//			System.out.println("listkId.get=="+listkId.get(i));
			keyStr = listkId.get(i).replaceAll("-", "").substring(0, 12)// getting just cikYear!
					+ listkId.get(i).substring(listkId.get(i).indexOf("_") + 1, listkId.get(i).length());
			scorePrev = 99.99;
			score = Double.parseDouble(listScore.get(i));
//			System.out.println("1 keyStr=="+keyStr);
			keyStr = keyStr.replaceAll("^[0]{0,8}", "").trim();
//			System.out.println("2 keyStr=="+keyStr);
			key = Double.parseDouble(keyStr);
//			System.out.println("keyStr.len=="+keyStr.length()+" i=="+i);

			if (percentDif > 0) {

//				System.out.println("score="+score+" scorePrev="+scorePrev+" percentDif="+percentDif +
//						" percentDifCalc=="+ (scorePrev - score) / scorePrev);

				if ((scorePrev - score) / scorePrev > percentDif) {
//					System.out.println("adding kId=" + listkId.get(i));
					map.put(key, listkId.get(i));
					scorePrev = score;

				}
			}

			else {

//				 System.out.println("adding kId=" + listkId.get(i));
				map.put(key, listkId.get(i));

			}

		}

		System.out.println("map.size=" + map.size());

		for (Map.Entry<Double, String> entry : map.entrySet()) {
//			System.out.println(entry.getKey() + " " + entry.getValue() + "\r\n");

			// returning just kId
			sb.append(entry.getValue() + " ");
		}
//		System.out.println("sb.toString=="+sb.toString());

		return sb.toString();

	}

	public static void filterByCIKandYear(String filepath, String contractType) throws SQLException, IOException {

		String query = "DROP TABLE IF EXISTS kIds;\r\n" + "CREATE TABLE kIds (\r\n"
				+ "  `kId` varchar(40) NOT NULL,\r\n" + "  Primary Key (kId)\r\n"
				+ "  ) ENGINE=myisam DEFAULT CHARSET=utf8;\r\n" + "\r\n";

		MysqlConnUtils.executeQuery(query);

		File file = new File("c:/getContracts/Ids/tmp.txt");
		PrintWriter pw = new PrintWriter(file);
		String text = Utils.readTextFromFile(filepath);
		text = text.replaceAll(" ", "\r\n");
		System.out.println("filepath==" + filepath + "\r\ntext=" + text);
		pw.append(text);
		pw.close();
		query = "LOAD Data INFILE 'c:/getContracts/Ids/tmp.txt' ignore INTO TABLE " + "kIds"
				+ " FIELDS TERMINATED BY '||';";

		MysqlConnUtils.executeQuery(query);

		File f = new File("c:/getContracts/Ids/" + contractType + "+CIK_kIds.txt");
		if (f.exists())
			f.delete();

		/*
		 * query = "nSELECT kIds "+
		 * "INTO OUTFILE 'c:/getContracts/Ids/"+contractType+"_CIK_kIds.txt' FROM kIds;"
		 * ;
		 * 
		 * MysqlConnUtils.executeQuery(query);
		 */
	}

	/*
	 * public static void javaContractFilterAndUniversalFieldRemover(List<String>
	 * listFldr, int sYr, int eYr, int sQ, int eQ, String kTyp, String fSize, String
	 * kNameContains, String kNameDoesNotContain) throws IOException {
	 * 
	 * 
	 * based on above: for each indenture folder I will keep xml files that
	 * fSize>100,00, kName contains: INDENTURE Indenture
	 * "Trust Agreement AND kName does not contain "supp amend restat". Next I'd
	 * choose PSAs and maybe have a higher fSize and kName filters etc.
	 * 
	 * javaFilterOfContractsToPutIntoSolr will iterate through the list of each
	 * contract type based on the conditions of filesize, and contract name. From
	 * that it will produced a list of trimmed now kIds placed in 1 folder for
	 * ingestion later by solr. This will substantially reduce the index size in
	 * solr by excluding non-consequential contract types.
	 * 
	 * 
	 * NLP nlp = new NLP(); String text = "", doc = "";
	 * 
	 * List<String[]> list = new ArrayList<>(); String[] ary = { kTyp, fSize,
	 * kNameContains, kNameDoesNotContain }; list.add(ary);
	 * 
	 * String folder =""; // "c:/getContracts/solrIngestion/solrSentences/"; for(int
	 * b=0; b<listFldr.size(); b++) { folder =listFldr.get(b).split(" ")[1]+"/";
	 * folder = SolrPrep.folderSolrXml + folder+kTyp;
	 * Utils.createFoldersIfReqd(folder); System.out.println("folder=="+folder); }
	 * 
	 * File files = new File(SolrPrep.folderSentencesXml); File[] listOfFiles =
	 * files.listFiles(); File fn = new File(SolrPrep.folderSentencesXml); String
	 * path = "",kName = ""; int qtr = 1, filesize; for (int i = 0; i < list.size();
	 * i++) { // System.out.println("kTyp="+Arrays.toString(list.get(i))); filesize
	 * = Integer.parseInt(list.get(0)[1]); kTyp=list.get(i)[0]; for (; eYr >= sYr;
	 * eYr--) { // System.out.println("eYr=" + eYr + " sYr=" + sYr); qtr = eQ; if
	 * (sYr == 1995) sQ = 3; else sQ=1;
	 * 
	 * for (; qtr >= sQ; qtr--) { for(int c=0; c<listFldr.size(); c++) {
	 * System.out.println(listFldr.get(c).split(" ")[0] + " yr=" + eYr + " qtr=" +
	 * qtr); listOfFiles = files.listFiles(); files = new
	 * File(listFldr.get(c).split(" ")[0]+list.get(i)[0] +"/"+eYr+"/QTR"+qtr+"/");
	 * // System.out.println("files="+files.getAbsolutePath()); listOfFiles =
	 * files.listFiles(); // System.out.println("listOfFiles[0]=="+listOfFiles[0]);
	 * if(null==listOfFiles || listOfFiles.length==0) continue; for (int n=0;
	 * n<listOfFiles.length; n++) { //
	 * C:\getContracts\solrIngestion\solrSentences\Indenture\1901\QTR1 path =
	 * listOfFiles[n].getAbsolutePath(); // System.out.println("path==="+path); text
	 * = Utils.readTextFromFile(path); //
	 * System.out.println("text.len="+text.length());
	 * if(nlp.getAllMatchedGroups(text,
	 * Pattern.compile("(?sm)<doc>.*?</doc>")).size()==0) continue; doc =
	 * text.substring(0, text.indexOf("</doc>") + 6); //
	 * System.out.println("doc=0="+doc);
	 * 
	 * kName = nlp .getAllMatchedGroups(doc, Pattern.
	 * compile("(?<=<field name=\"kName\"><\\!\\[CDATA\\[).*?(?=\\]\\]></field>)"))
	 * .get(0);
	 * 
	 * fSize = nlp .getAllMatchedGroups(doc,
	 * Pattern.compile("(?<=<field name=\"fSize\">)[\\d]{3,10}")) .get(0); //
	 * System.out.println("\r\nkTyp" + "\r\nfSize=" + fSize + "\r\nkkNameContains="
	 * + kNameContains // + "\r\nkNameDoesNotContain=" + kNameDoesNotContain);
	 * 
	 * if(filesize>Integer.parseInt(fSize)) { //
	 * System.out.println("filesize condition=="+filesize+" xml fSize="+Integer.
	 * parseInt(fSize)); continue; }
	 * 
	 * if(nlp.getAllMatchedGroups(kName, Pattern.compile("("+kNameDoesNotContain
	 * +")")).size()>0) { //
	 * System.out.println("filter-out b/c bad name found - xml no good="+nlp.
	 * getAllMatchedGroups(kName, Pattern.compile("("+kNameDoesNotContain //
	 * +")")).size()); continue; }
	 * 
	 * if(nlp.getAllMatchedGroups(kName,
	 * Pattern.compile("("+kNameContains+")")).size()==0) { //
	 * System.out.println("filter-name condition not - xml no good="+nlp.
	 * getAllMatchedGroups(kName, Pattern.compile("("+kNameDoesNotContain //
	 * +")")).size()); continue; }
	 * 
	 * // remove universal fields text =
	 * Utils.readTextFromFile(listOfFiles[i].getAbsolutePath()).substring(0,
	 * text.indexOf("</doc>") + 6) + text.substring(text.indexOf("</doc>") + 6,
	 * text.length()).replaceAll(
	 * "(?sm)(<field name=\"(kId|kTyp|kName|filer|cik|fDate|fSize|ND|LND)\">).*?</field>\r\n"
	 * , "");//removes universal fields from other than cnt=0
	 * 
	 * folder =listFldr.get(c).split(" ")[1]+"/"; folder = SolrPrep.folderSolrXml +
	 * folder+kTyp+"/"; // File targetFilename = new
	 * File(folder+listOfFiles[n].getName()); //
	 * FileSystemUtils.copyFile(listOfFiles[n], targetFilename); fn = new
	 * File(folder+listOfFiles[n].getName()); Utils.writeTextToFile(fn, text);
	 * 
	 * } } } } } }
	 * 
	 */

	// move to solrPrep
	/*
	 * public static void getXMLsforClauseAndParentChildSentence(int startYear, int
	 * endYear, int sQ, int eQ, List<String[]> listXmlAndContractType) throws
	 * IOException {
	 * 
	 * NLP nlp = new NLP(); String text = "", doc = ""; int qtr = sQ, eYr = endYear;
	 * StringBuilder sbClau = new StringBuilder(); StringBuilder sbParChi = new
	 * StringBuilder(); String path = ""; for (int i = 0; i <
	 * listXmlAndContractType.size(); i++) { System.out.println("listContractTypes="
	 * + Arrays.toString(listXmlAndContractType.get(i)));
	 * 
	 * for (; endYear >= startYear; endYear--) { System.out.println("endYr=" +
	 * endYear + " startYr=" + startYear); qtr = eQ; for (; qtr >= sQ; qtr--) {
	 * System.out.println("qtr=" + qtr + " sQ=" + sQ);
	 * Utils.createFoldersIfReqd(SolrPrep.folderClausesXml + "/" +
	 * listXmlAndContractType.get(i)[1] + "/" + endYear + "/QTR" + sQ + "/");
	 * 
	 * path = SolrPrep.folderSolrIngestion + "solr"+listXmlAndContractType.get(i)[0]
	 * + "/" + listXmlAndContractType.get(i)[1] + "/" + endYear + "/QTR" + sQ + "/";
	 * System.out.println("path=x=x="+path);
	 * 
	 * File folder = new File(path); System.out.println("folder=" +
	 * folder.getAbsolutePath()); File[] listOfFiles = folder.listFiles(); for (int
	 * c = 0; c < listOfFiles.length; c++) { text =
	 * Utils.readTextFromFile(listOfFiles[c].getAbsolutePath());
	 * 
	 * 
	 * List<String> listClausAndPC = getClauseAndParentChildSentence(text);
	 * 
	 * 
	 * 
	 * File fileClau = new File(SolrPrep.folderClausesXml + "/" +
	 * listXmlAndContractType.get(i)[1] + "/" + endYear + "/QTR" + sQ + "/" +
	 * listOfFiles[c].getName());
	 * 
	 * System.out.println("write filellllll"); Utils.writeTextToFile(fileClau,
	 * listClausAndPC.get(0)); } } qtr = 4; } endYear = eYr; qtr = 4; } }
	 */
	public static List<String> getClauseAndParentChildSentence(String text) throws IOException {

		StringBuilder sbClause = new StringBuilder("<add>");
		List<String> list = new ArrayList<>();
		List<String> listClauseAndParentChildSentence = new ArrayList<>();
		NLP nlp = new NLP();

		List<String> listDocs = nlp.getAllMatchedGroups(text, Pattern.compile("(?ism)(<doc>.*?</doc>)"));
		String claus = "", parentChild = "", parentLeadClaus = "", parentLeadClausPrior = "", leadLineage = "",
				leadLineageParentChildSentence = "", p = "", pPrior = "", txt = "", semiColon1 = "", semiColon2 = "";

		boolean leadLineageFound = false, leadLineageFoundSemiColon = false;

		for (int c = 0; c < listDocs.size(); c++) {
			claus = "";
			parentChild = "";
			parentLeadClaus = "";
			leadLineageParentChildSentence = "";
			txt = "";
			leadLineage = "";
			leadLineageFound = false;
			leadLineageFoundSemiColon = false;
			text = listDocs.get(c);
//			if(nlp.getAllMatchedGroups(text, Pattern.compile("; [a-zA-Z\\d]{1}")).size()==0)
//				continue;
			list = SolrPrep.getParentChild(text);
//			 System.out.println("1. start <doc>=="+text+"||END\r\n\r\n");
			for (int i = 0; i < list.size(); i++) {
//			System.out.println("listSent="+list.get(i));

				if (nlp.getAllMatchedGroups(list.get(i), Pattern.compile("(?<=\\[\\[txt\\=\\=).*?(?=\\]\\])"))
						.size() > 0) {
					txt = nlp.getAllMatchedGroups(list.get(i), Pattern.compile("(?<=\\[\\[txt\\=\\=).*?(?=\\]\\])"))
							.get(0);
//					System.out.println("txt--"+txt);
				}

				if (!leadLineageFound && nlp.getAllMatchedGroups(list.get(i),
						Pattern.compile("(?sm)(?<=leadLineage\\=\\=\\[CDATA\\[).*?(?=\\]\\])")).size() > 0) {
					leadLineage = nlp.getAllMatchedGroups(list.get(i),
							Pattern.compile("(?sm)(?<=leadLineage\\=\\=\\[CDATA\\[).*?(?=\\]\\])")).get(0);
					sbClause.append("1=leadLineage=" + leadLineage + "||END\r\n\r\n");
					leadLineageFound = true;
				}

				if (nlp.getAllMatchedGroups(list.get(i), Pattern.compile("\\[\\[lineage\\=\\=p\\d\\d?c")).size() == 0) {
//					System.out.println("list.get(i)=="+list.get(i));

					if (txt.trim().length() > 0) {
						sbClause.append("2=parentSolo=" + txt + "||END\r\n\r\n");
						sbClause.append("3=leadLineageparentSolo=" + leadLineage + " " + txt + "||END\r\n\r\n");
					}
				}

				if (nlp.getAllMatchedGroups(list.get(i), Pattern.compile("\\[\\[parentLead\\=\\=")).size() > 0) {
					parentLeadClaus = nlp.getAllMatchedGroups(list.get(i),
							Pattern.compile("(?<=\\[\\[parentLead\\=\\=).*?(?=\\]\\])")).get(0);
//					if (parentLeadClaus.equals(parentLeadClausPrior)) {
//						continue;
//					}

					if (!parentLeadClaus.equals(parentLeadClausPrior)) {
						sbClause.append("4=parentLeadClaus==" + parentLeadClaus + "||END\r\n\r\n");
					}

					if (nlp.getAllMatchedGroups(list.get(i), Pattern.compile("(?<=\\[\\[txt\\=\\=).*?(?=\\]\\])"))
							.size() > 0) {
						claus = nlp
								.getAllMatchedGroups(list.get(i), Pattern.compile("(?<=\\[\\[txt\\=\\=).*?(?=\\]\\])"))
								.get(0);
						sbClause.append("5=clause=" + claus + "||END\r\n\r\n");
					}

					if (claus.length() > 0 && parentLeadClaus.length() > 0) {
						parentChild = parentLeadClaus.trim() + " " + claus.trim();
						sbClause.append("6=parentChild=" + parentChild + "||END\r\n\r\n");
					}

					if (nlp.getAllMatchedGroups(list.get(i),
							Pattern.compile("(?sm)(?<=leadLineage\\=\\=\\[CDATA\\[).*?(?=\\]\\])")).size() > 0) {
						leadLineageParentChildSentence = leadLineage.trim() + " " + parentChild.trim();
						sbClause.append(
								"7=leadLineageParentChildSentence=" + leadLineageParentChildSentence + "||END\r\n\r\n");
					}

					parentLeadClausPrior = parentLeadClaus;

				}

				semiColon1 = "";
				semiColon2 = "";

				/*
				 * if (!leadLineageFoundSemiColon && list.get(i).contains(";")) {
				 * if(list.get(i).contains("lineageLead")) { System.out.println("i==" + i +
				 * " leadLineage?=" + list.get(i).contains("leadLineage"));
				 * leadLineageFoundSemiColon = true; } }
				 */
				/*
				 * if(nlp.getAllMatchedGroups(list.get(i), Pattern.compile(".*?;")).size()>0) {
				 * semiColon1 = nlp.getAllMatchedGroups(list.get(i),
				 * Pattern.compile(".*?;")).get(0); semiColon2 =
				 * list.get(i).substring(list.get(i).indexOf(";")+1,list.get(i).indexOf("]]"));
				 * 
				 * if(!leadLineageFound && semiColon1.split(" ").length>4 &&
				 * semiColon2.split(" ").length>4 ) {
				 * sbClause.append("8=semiColon1="+semiColon1+"||END\r\n\r\n");
				 * sbClause.append("9=semiColon2=" + semiColon2 + "||END\r\n\r\n");
				 * System.out.println("8/9="+list.get(i)); } }
				 */
			}
		}

		sbClause.append("</add>");

//		NLP.printListOfString("list...", list);

		listClauseAndParentChildSentence.add(sbClause.toString());
		return listClauseAndParentChildSentence;

	}

	public static String getPartsOfSpeechVerbs(String text) throws IOException {
//		System.out.println(text);
		text = SolrPrep.getPartsOfSpeech(text);
		System.out.println("POS="+text);
		text = text.replaceAll("\\|[0-9A-Za-z,\\-\\$\\'\\.;\\_\\:]+ [A-UW-Z\\-\\.,]+", "");
		text = text.replaceAll(" V.{1,10}\\||\\|", " ").replaceAll("; |: ", "").trim();
		text = text.replaceAll("[A-Z]+[a-z]+($| )", "").replaceAll("; |: ", "").trim();
		return text;
	}

	public static String removeAdjectivesAndAdverbs(String text) throws IOException {
		text = SolrPrep.getPartsOfSpeech(text);
		System.out.println("POS="+text);
//        7.      JJ      Adjective
//        8.      JJR     Adjective, comparative
//        9.      JJS     Adjective, superlative
//        20.     RB      Adverb
//        21.     RBR     Adverb, comparative
//        22.     RBS     Adverb, superlative
//        36.     WRB     Wh-adverb
		text = text.replaceAll("\\|[0-9A-Za-z,\\-\\$\\'\\.;\\_\\:]+ (JJ|RB|WR).*?(?=\\|)", "");

		return text;
	}

	public static String tokenized_def_stop_stem(String text) throws FileNotFoundException {
		text = removeEverythingOtherThanLetters(text).trim();
//		System.out.println("punctuation removed=="+text);
		// must remove stop words that are 2 or more words first - eg "In case" or "in
		// case" otherwise removeDefinedTerms will pick-up "In"
		text = StopWords.removeStopWordsOf2(text);
		text = removeDefinedTerms(text).trim();
//		System.out.println("and defined words removed=="+text);
		text = StopWords.removeStopWords(text).trim();
//		System.out.println("and stop words removed==" + text);
		// text = StanfordParser.lemma(text);
		// System.out.println("lemma="+text);
//		text = Stemmer.stemmedOutPutWithPunctuationRemoved(text).trim();
		GoLawLancasterStemmer lc = new GoLawLancasterStemmer();
//		text = text.replaceAll("[^a-zA-Z\\d]", "");
		text = StringUtils.join(lc.stemWords(text), " ");
//		System.out.println("text aft stem==" + text);
		text = removeStrandedLetters(text).trim();
//		System.out.println("tokenized_def_stop_stem=" + text);
		return text;
	}

	public static String removeDuplicateWords(String text) {

		TreeMap<String, String> map = new TreeMap<String, String>();
		String[] ary = text.split(" ");
		text = "";
		for (int i = 0; i < ary.length; i++) {
			if (map.containsKey(ary[i]))
				continue;
			map.put(ary[i], ary[i]);
			text = text + ary[i].trim() + " ";
		}

		return text.replaceAll("[ ]+", " ").trim();
	}

	public static String alphabetizeWords(String text) {

		String[] textSplit = text.split(" ");
		List<String> listOfWordsOfTerm = new ArrayList<String>();

		for (int i = 0; i < textSplit.length; i++) {
			listOfWordsOfTerm.add(textSplit[i]);
		}

		Collections.sort(listOfWordsOfTerm.subList(1, listOfWordsOfTerm.size()));

		List<String> list = listOfWordsOfTerm.subList(1, listOfWordsOfTerm.size());
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			sb.append(list.get(i) + " ");
		}

//		System.out.println("text="+sb.toString());
		return sb.toString();
	}

	public static String removeEverythingOtherThanLetters(String text) {

		text = text.replaceAll("[^A-Za-z \r\n]", " ").replaceAll("[ ]+", " ");

		return text;
	}

	public static String removeStrandedLetters(String text) {

		text = text.replaceAll("(^| )[A-Za-z]{1,2}(?= |$)", " ").replaceAll("[ ]+", " ");

		return text;
	}

	public static String removeDefinedTerms(String text) {

		text = text.replaceAll("(?sm)[A-Z]+[A-Za-z]+", " ").replaceAll("[ ]+", " ").trim();

		return text;
	}

	public static String removeDuplicateWordsAndAlphabetialOrder(String text) {

		TreeMap<String, String> map = new TreeMap<String, String>();
		String[] words = text.split(" ");
		for (int i = 0; i < words.length; i++) {
			map.put(words[i], "");
		}

		text = "";
		for (Map.Entry<String, String> entry : map.entrySet()) {
//			System.out.println("key="+entry.getKey());
			text = text + " " + entry.getKey();
		}

		return text;
	}

	public static List<String[]> orderByWcnt(List<String[]> list) {

		TreeMap<Double, String[]> map = new TreeMap<Double, String[]>();
		double cntg = 0.000001, wCnt = 0;

		for (int i = 0; i < list.size(); i++) {
			if (list.get(i)[1].trim().length() == 0)
				continue;
			wCnt = -1 * (list.get(i)[1].split(" ").length + cntg);
			map.put(wCnt, list.get(i));
			cntg = cntg + 0.000001;
		}

		List<String[]> listAry = new ArrayList<String[]>();

		for (Map.Entry<Double, String[]> entry : map.entrySet()) {
			System.out.println("ordered entry.getKey()==" + entry.getKey() + " val=" + entry.getValue()[1]);
			listAry.add(entry.getValue());
		}

		NLP.printListOfStringArray("ordered by wCnt==", listAry);

		return listAry;
	}

	public static String highlightWordsPresentInOneSentenceButNotTheOtherAndViceVersa(String[] sent1, String[] sent2)
			throws IOException {

		List<String> list = new ArrayList<String>();
		list.add(sent1[0]);
		list.add(sent2[0]);

		System.out.println("1 hTxt1=" + sent1[0]);
		System.out.println("2 hTxt2=" + sent2[0]);
		System.out.println("1 sent1=" + sent1[1]);
		System.out.println("2 sent2=" + sent2[1]);

		// This is intended for a list of 2 sentences.

		TreeMap<String, String> map = new TreeMap<String, String>();
		TreeMap<String, String> map2 = new TreeMap<String, String>();

		int[] abovBelo = { 1, 0, 0, 1 };// points to which sentence is has word present other doesn't.
		// if looking above then int[0]=1, int[1]=0, if below then int[2]=0,int[3]=1

		List<String> listAB = new ArrayList<String>();
		List<String> listA = new ArrayList<String>();
		List<String> listB = new ArrayList<String>();

		for (int c = 0; c < abovBelo.length; c++) {
			System.out.println("abovBelo c=" + c + " abovBelo.len=" + abovBelo.length);
			listAB = new ArrayList<String>();
			for (int i = 0; i < list.size(); i++) {

				System.out.println("list sent size=" + list.size());
				if (i + 1 == list.size())
					break;

				map = new TreeMap<String, String>();
				map2 = new TreeMap<String, String>();

				System.out.println("sent#=" + list.get(i));

				String[] ary = list.get(i + abovBelo[c]).split(" ");
				for (int n = 0; n < ary.length; n++) {
					System.out.println("ary[n]=" + ary[n]);
					if (map.containsKey(ary[n])) {
						map.put(ary[n], map.get(ary[n]) + "||" + ary[n]);
						System.out.println("sent1 adding and contains duplicate=" + map.get(ary[n]) + "||" + ary[n]);
					} else {
						map.put(ary[n], ary[n]);
						System.out.println("sent1 adding =" + ary[n]);
					}
				}

				String[] ary2 = list.get(i + abovBelo[c + 1]).split(" ");

				// if it is in above sentence and not in below then HL in above.
				for (int n = 0; n < ary2.length; n++) {
					if (!map.containsKey(ary2[n])) {
						System.out.println("doesn't contain word=" + ary2[n]);
						if (map2.containsKey(ary2[n])) {
							map2.put(ary2[n], map.get(ary2[n]) + "||" + ary2[n]);
							listAB.add(ary2[n]);
						} else {
							map2.put(ary2[n], ary2[n]);
							listAB.add(ary2[n]);
						}
						map.remove(ary2[n]);
					}
				}

			}

			if (c == 0) {
				listA = listAB;
			} else {
				listB = listAB;
			}

			c++;
			System.out.println("c===" + c);
		}

		NLP.printListOfString("list above====", listA);
		NLP.printListOfString("list below====", listB);
		String str = "";

		StringBuilder sb = new StringBuilder();
		String[] sent1Ary = sent1[1].split(" ");
		// using order of hTxt and order of original text - iterate through hTxt present
		// in sent1 (abov) but not in sent2 (belo) - to ensure I get a match I see if
		// there's a match by converting original text word to see if it matches hTxt.
		// B/c hTxt algorithm could be refined as needed - reconvert original text to
		// hTxt then perform this algorithm to see what is missing to bold.

		boolean found = false;
		int i = 0, n = 0;
		for (; i < listA.size(); i++) {
			str = listA.get(i);
//			System.out.println("str= ="+str);
			found = false;
			for (; n < sent1Ary.length; n++) {
				if (found)
					break;
//			System.out.println("listA.size="+listA.size());
//			System.out.println("sent1[1]=="+sent1Ary[n]+" listA.get(i)="+listA.get(i));
//				System.out.println("str="+str);
				if (!found && str.equals(tokenized_def_stop_stem(sent1Ary[n]))) {
					System.out.println("above bolded=" + sent1Ary[n]);
					sb.append(" <b>" + sent1Ary[n] + "</b>");
					found = true;
				}
				if (!found) {
					System.out.println("above no bold=" + sent1Ary[n]);
					sb.append(" " + sent1Ary[n]);
				}
			}
		}

		sb.append("\r\n<br><br>");

		found = false;
		i = 0;
		n = 0;
		String[] sent2Ary = sent2[1].split(" ");
		for (; i < listB.size(); i++) {
			str = listB.get(i);
//			System.out.println("str= ="+str);
			found = false;
			for (; n < sent2Ary.length; n++) {
				if (found && i + 1 == listB.size()) {
					sb.append(" " + sent2Ary[n]);
					System.out.println("adding at end - below - ary=" + sent2Ary[n]);
					continue;
				}

				if (found && listB.size() != i + 1)
					break;

//			System.out.println("listB.size="+listB.size());
//			System.out.println("sent2[1]=="+sent2Ary[n]+" listB.get(i)="+listB.get(i));
				System.out.println("str=" + str);
				if (!found && str.equals(tokenized_def_stop_stem(sent2Ary[n]))) {
					System.out.println("below bolded=" + sent2Ary[n]);
					sb.append(" <b>" + sent2Ary[n] + "</b>");
					found = true;
					System.out.println("found==" + found + " listB.size=" + listB.size() + " i=" + i);
				}
				if (!found) {
					System.out.println("below no bold=" + sent2Ary[n]);
					sb.append(" " + sent2Ary[n]);
				}

			}
		}

		System.out.println("sent above and below text=" + sb.toString());

		return sb.toString();
	}

	public static void main(String[] args) throws IOException, SQLException, SolrServerException {
//		NLP nlp = new NLP();

//		Utilities.boldUniqueWords("Limitation_on_Right_to_Sue".replaceAll(" ", "_"), "indenture",
//				"sent", true, true, true, true);
		StringBuilder sb = new StringBuilder();

		String text =""; 
//				Utils.readTextFromFile("d:/getcontracts/temp/reducer.txt");
//		text = text.replaceAll("(?sm)^\\d.*?\\%[ ]+\r\n", "");
//		System.out.println("text=====" + text);
//		String[] ary = text.split("\r\n");
//		List<String[]> listSent = new ArrayList<String[]>();
//		for (int i = 0; i < ary.length; i++) {
//			text = ary[i];
		text = "In case an Event of Default shall occur and be continuing, the Trustee shall exercise such of its rights and powers under the applicable Indenture and use the same degree of care and skill in their exercise as a prudent person would exercise or use under the circumstances in the conduct of his own affairs.";
		System.out.println("nJJ="+removeAdjectivesAndAdverbs(text));
		
//		text = tokenized_def_stop_stem(text);
//		System.out.println("text===="+text);
//			text = removeDuplicateWords(text);
//			if (text.length() > 0) {
//				sb.append(text + "\r\n");
//				String[] ary2 = { text, ary[i] };
//				listSent.add(ary2);
//			}
//		System.out.println(text);
		// add ary here of both stemmed stop txt and original text so that they are
		// linked.
//		}
//
//		PrintWriter pw = new PrintWriter(new File("d:/getContracts/temp/reduced.txt"));
//		System.out.println("sb.toSt=" + sb.toString());
//		pw.append(sb.toString());
//		pw.close();

//		List<String[]> listAry = orderByWcnt(listSent);
//		NLP.printListOfStringArray("ordered by wCnt=", listAry);

//		System.out.println("1==" + listAry.get(0)[1]);
//		System.out.println("2==" + listAry.get(1)[1]);
//		String highlightTxt = highlightWordsPresentInOneSentenceButNotTheOtherAndViceVersa(listAry.get(0),
//				listAry.get(1));

//		pw = new PrintWriter(new File("d:/getContracts/temp/highlightTxt.html"));
//		pw.append("<html>" + highlightTxt + "</html>");
//		pw.close();
	}
}
