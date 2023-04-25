package contracts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

//import javax.json.Json;
import javax.xml.bind.annotation.XmlElementDecl.GLOBAL;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.tika.parser.txt.TXTParser;
import org.bouncycastle.jce.provider.JDKGOST3410Signer.gost3410;

import ucar.ma2.ArrayDouble.D3.IF;
import xbrl.NLP;
import xbrl.Utils;

public class ContractFinder {

	public static String parseTheseFolder = "e:/solrJsons/parseThese/";
	public static String parseTheseNotFolder = "e:/solrJsons/parseTheseNot/";
	public static String solrJsonDataFolder = "E:/getContracts/solrIngestion/solrDocs/";

	public static String oP = "";

	public static Pattern patternNotContractName = Pattern
			.compile("( AND |AMENDED|RESTATED|AGREEMENT|SUPPLEMENT|AMENDMENT|RESTATEMENT|PARTIES TO THE AGREEMENT"
					+ "|NO[\\. ]{1}|CONTRACT|FIRST|SECOND|THIRD|FOURTH"
					+ "|FIFTH|SIXTH|SEVENTH|EIGHTH|NINTH|TENTH|ELEVENTH|TWELFTH|THIRTEENTH|FOURTEENTH|FIFTEENTH"
					+ "|SIXTEENTH|SEVENTEENTH|EIGHTEENTH|NINETEENTH|TWENTIETH|TWENTY.{1,3}FIRST|TWENTY.{1,3}SECOND"
					+ "|TWENTY.{1,3}THIRD|TWENTY.{1,3}FOURTH|TWENTY.{1,3}FIFTH|TWENTY.{1,3}SIXTH|TWENTY.{1,3}SEVENTH"
					+ "|TWENTY.{1,3}EIGHTH|TWENTY.{1,3}NINTH|THIRTIETH)+");

	public static Pattern patternOtherContractRelatedMaterials = Pattern.compile("(?sm)(((this|This|THIS)( ).{0,100}?"
			+ "(CONFIRMATION|CONVERSION|COMMITMENT|DESCRIPTION|OFFICER.{1,3}CERTIFICATE|POWER OF ATTORNEY|EXCHANGE|REDEMPTION|TENDER|CALCULATION"
			+ "|Confirmation|Conversion|Commitment|Description|Officer.{1,3}Certificate|Power of Attorney|Exchange|Redemption|Tender|Calculation)(?=[,;: \\.\r\n]{1}))"
			+ "|(CONFIRMATION|CONVERSION|COMMITMENT|DESCRIPTION|OFFICER.{1,3}CERTIFICATE|POWER OF ATTORNEY|EXCHANGE|REDEMPTION|TENDER|CALCULATION))(?=[,;: \\.\r\n])");

	public static Pattern patternPolicyArticles = Pattern.compile("(?sm)(((this|This|THIS)( ).{0,100}?"
			+ "(ARTICLES OF (AMENDMENT|ASSOCIATION)|POLICY|PRIVACY|GUIDELINE|PROCEDURE"
			+ "|Articles of (Amendment|Association)|Policy|Privacy|Guideline|Procedure)(?=[,;: \\.\r\n]{1}))"
			+ "|(ARTICLES OF (AMENDMENT|ASSOCIATION)|POLICY|PRIVACY|GUIDELINE|PROCEDURE))(?=[,;: \\.\r\n])");

	public static Pattern patternContractNameInContractLongNameAllCaps = Pattern.compile("(?sm)(" + "("
			+ "POWER OF ATTORNEY|CONFIRMATION|COMMITMENT LETTER|LETTER AGREEMENT|POOLING AND SERVICING|DEBENTURE|DEED OF TRUST|(SUPPLEMENTAL )?INDENTURE|(AMENDED AND RESTATED )?(AGREEMENT AND )?"
			+ "PLAN OF (MERGER|ACQUISITION|STOCK PURCHASE)"
			+ "|AGREEMENT TO MERGE AND PLAN OF REORGANIZATION|(AMENDED AND RESTATED |SUPPLEMENTAL )(INDENTURE)|GUARANTEE|GUARANTY|[A-Z]?!LEASE"
			+ "|(SUBORDINATED|JUNIOR|PRMISSORY|SENIOR)? ?NOTE|WARRANT|CONTRACT" + ""
			+ "|[A-Z\\.\\d ]{1,70}AGREEMENT))");

	public static Pattern patternContractNameAllCaps = Pattern
			.compile("(?sm)((this |This |THIS |^\\^|^\\^this |^\\^This |^\\^THIS )" + "("
					+ "POWER OF ATTORNEY|CONFIRMATION|COMMITMENT LETTER|LETTER AGREEMENT|POOLING AND SERVICING|DEBENTURE|DEED OF TRUST|(SUPPLEMENTAL )?INDENTURE|(AMENDED AND RESTATED )?(AGREEMENT AND )?PLAN OF (MERGER|ACQUISITION|STOCK PURCHASE)"
					+ "|AGREEMENT TO MERGE AND PLAN OF REORGANIZATION|(AMENDED AND RESTATED |SUPPLEMENTAL )(INDENTURE)|GUARANTEE|GUARANTY|[A-Z]?!LEASE"
					+ "|(SUBORDINATED|JUNIOR|PRMISSORY|SENIOR)? ?NOTE|WARRANT|CONTRACT" + ""
					+ "|[A-Z\\.\\d ]{1,70}(INDENTURE|AGREEMENT))(?=[,;: \\.\r\n]))");

	public static Pattern patternContractNameInitialCaps = Pattern
			.compile("(?sm)((this |This |THIS |^\\^|^\\^this |^\\^This |^\\^THIS )" + "("
					+ "Confirmation|Commitment Letter|Power [oO]f Attorney|Letter Agreement|Pooling [Aa]nd Servicing|Debenture|Deed [oO]f Trust|(Supplemental )?Indenture"
					+ "|(Amended [aA]nd Restated )?(Agreement [Aa]nd )?Plan [oO]f (Merger|Acquisition"
					+ "|Stock Purchase|Reorganization)|Agreement [tT]o Merge [Aa]nd Plan [Oo]f Reorganization|(Amended [Aa]nd Restated |Supplemental )(Indenture)"
					+ "|Guarantee|Guaranty|[A-Z]?!Lease|(Subordinated|Junior|Prmissory|Senior)? ?Note|Warrant|Contract"
					+ "" + "|([A-Z]{1}[a-z\\d\\.]{1,15} )+Agreement)(?=[,;: \\.\r\n]))");
//	POWER OF ATTORNEY|CONFIRMATION|COMMITMENT LETTER|LETTER AGREEMENT|
	public static Pattern patternLetterAgreement = Pattern
			.compile("(?sm)((this |This |THIS |^\\^|^\\^this |^\\^This |^\\^THIS )" + " ("
					+ "(GENERAL )?RELEASE|CONFIRMATION|COMMITMENT LETTER|Confirmation|Commitment Letter|Letter Agreement)(?=[,;: \\.\r\n]))");
//do not put general release in initial caps

	public static Pattern patternUWAgreement = Pattern
			.compile("(?sm)(^UNDERWRITING AGREEMENT$|^Underwriting Agreement$)");

	public static Pattern patternThisLeasefromOp = Pattern.compile("(?sm)(?ism)(^\\^THIS LEASE)");

	public static Pattern patternThisAgreementFor = Pattern
			.compile("(?sm)(\\^This|\\^this|\\^THIS) AGREEMENT [A-Z ]{5,38}");

	public static Pattern patternDeclOfTrust = Pattern.compile("(?sm)(\\^This|\\^this|\\^THIS )?"
			+ "(AMENDED AND RESTATED) ?DECLARATION OF TRUST|(Amended [Aa]nd Restated )Declaration [oO]f Trust"
			+ "|TRUST INDENTURE(?! ACT)|Trust Indenture(?! Act)");

	public static Pattern patternContractName = Pattern.compile(patternThisAgreementFor.pattern() + "|"
			+ patternDeclOfTrust.pattern() + "|" + patternContractNameAllCaps.pattern() + "|"
			+ patternContractNameInitialCaps.pattern() + "|" + patternLetterAgreement.pattern() + "|"
			+ patternUWAgreement.pattern() + "|" + patternThisLeasefromOp.pattern());

	public static Pattern patternContractNameStopWords = Pattern.compile("(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" + ""
			+ "(in case)|(a)|(about)|(above)|(after)|(again)|(all)|(am)|(an)|(any)|(are)|(as)|(at)|(be)"
			+ "|(because)|(been)|(before)|(being)|(below)|(between)|(both)|(by)|(can)|(did)|(do)|(does)|(doing)|(down)"
			+ "|(during)|(each)|(either)|(few)|(for)|(from)|(further)|(had)|(has)|(have)|(having)|(he)|(her)|(here)|(hers)"
			+ "|(herself)|(him)|(himself)|(his)|(how)|(I)|(if)|(in)|(into)|(is)|(it)|(its)|(it\'s)|(itself)|(just)|(me)"
			+ "|(might)|(more)|(most)|(must)|(my)|(myself)|(need)|(now)|(off)|(on)|(once)|(only)|(other)|(otherwise)"
			+ "|(our)|(ours)|(ourselves)|(out)|(over)|(own)|(same)|(she)|(she\'s)|(should)|(so)|(some)|(such)|(than)"
			+ "|(that)|(the)|(their)|(theirs)|(them)|(themselves)|(then)|(there)|(these)|(they)|(those)|(through)"
			+ "|(too)|(under)|(until)|(up)|(very)|(very)|(was)|(we)|(were)|(what)|(when)|(where)|(whether)|(which)|(while)|(who)"
			+ "|(whom)|(why)|(will)|(with)|(won)|(would)|(you)|(your)|(yours)|(yourself)|(yourselves)" + ""
			+ ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

	public static Pattern patternContractTypes = Pattern
			.compile("(?ism)employ|separation|subord|limitedsd|subscription|limited|compen"
					+ "|memorandum|support|transition|placement|retention|extension|forbear|standstill|power|market|termination|acquis"
					+ "|participation|severance|collab|contribution|confidential|award|transfer|assumpt|contemp|assign|lease|Purchase|underwrit"
					+ "|settle|indenture|mortgage|reorg|sale|share|guarant|incent|indemn|compet|cooperat|dealer|develop|facilit|fee |financ"
					+ "|intellect|admin|contract|intercredit|credit|loan|trust|Invest|Advis|Manage|Change|Control|escrow|deposit|Operat|Pledg"
					+ "|Pooling|Agency|Collateral|Custod|Securit|Consult|Distribut|Servic|Licens|Stock|Option|exchange|supply"
					+ "|Voting|Warrant|right|joinder|venture|repurchas|msla|mra|lending|understand|merger|plan|reimburs|reinsuranc|refusal"
					+ "|continuation|salary|grant|fund|disclos|collective|bargain|selling|pricing|intent|executive|retire|conversion|manufact|invent"
					+ "|recognit|research|solicit|modificat|waiver|note|mutual|master|letter|agreement|articles|incorporation|supp|amendment");

	public static Pattern patternContractTypesAllCapsOrInitialCaps = Pattern.compile(
			"(?sm)(Employ|Separation|Subord|Limitedsd|Subscription|Limited|Compen|Memorandum|Support|Transition|Placement|Retention|Extension"
					+ "|Forbear|Standstill|Power|Market|Termination|Acquis|Participation|Severance|Collab|Contribution|Confidential|Award|Transfer"
					+ "|Assumpt|Contemp|Assign|Lease|Purchase|Underwrit|Settle|Indenture|Mortgage|Reorg|Sale|Share|Guarant|Incent|Indemn|Compet|"
					+ "Cooperat|Dealer|Develop|Facilit|Fee |Financ|Intellect|Admin|Contract|Intercredit|Credit|Loan|Trust|Invest|Advis|Manage|Change"
					+ "|Control|Escrow|Deposit|Operat|Pledg|Pooling|Agency|Collateral|Custod|Securit|Consult|Distribut|Servic|Licens|Stock|Option"
					+ "|Exchange|Supply|Voting|Warrant|Right|Joinder|Venture|Repurchas|Msla|Mra|Lending|Understand|Merger|Plan|Reimburs|Reinsuranc"
					+ "|Refusal|Continuation|Salary|Grant|Fund|Disclos|Collective|Bargain|Selling|Pricing|Intent|Executive|Retire|Conversion|Manufact"
					+ "|Invent|Recognit|Research|Solicit|Modificat|Waiver|Note|Mutual|Master|Letter|Agreement|Articles|Incorporation|Supp|Amendment"
					+ "|EMPLOY|SEPARATION|SUBORD|LIMITEDSD|SUBSCRIPTION|LIMITED|COMPEN|MEMORANDUM|SUPPORT|TRANSITION|PLACEMENT|RETENTION|EXTENSION"
					+ "|FORBEAR|STANDSTILL|POWER|MARKET|TERMINATION|ACQUIS|PARTICIPATION|SEVERANCE|COLLAB|CONTRIBUTION|CONFIDENTIAL|AWARD|TRANSFER"
					+ "|ASSUMPT|CONTEMP|ASSIGN|LEASE|PURCHASE|UNDERWRIT|SETTLE|INDENTURE|MORTGAGE|REORG|SALE|SHARE|GUARANT|INCENT|INDEMN|COMPET|COOPERAT"
					+ "|DEALER|DEVELOP|FACILIT|FEE |FINANC|INTELLECT|ADMIN|CONTRACT|INTERCREDIT|CREDIT|LOAN|TRUST|INVEST|ADVIS|MANAGE|CHANGE|CONTROL"
					+ "|ESCROW|DEPOSIT|OPERAT|PLEDG|POOLING|AGENCY|COLLATERAL|CUSTOD|SECURIT|CONSULT|DISTRIBUT|SERVIC|LICENS|STOCK|OPTION|EXCHANGE|SUPPLY"
					+ "|VOTING|WARRANT|RIGHT|JOINDER|VENTURE|REPURCHAS|MSLA|MRA|LENDING|UNDERSTAND|MERGER|PLAN|REIMBURS|REINSURANC|REFUSAL|CONTINUATION"
					+ "|SALARY|GRANT|FUND|DISCLOS|COLLECTIVE|BARGAIN|SELLING|PRICING|INTENT|EXECUTIVE|RETIRE|CONVERSION|MANUFACT|INVENT|RECOGNIT|RESEARCH"
					+ "|SOLICIT|MODIFICAT|WAIVER|NOTE|MUTUAL|MASTER|LETTER|AGREEMENT|ARTICLES|INCORPORATION|SUPP|AMENDMENT)");

	public static HashMap<Integer, List<String>> openingParagraphAttributes(String text) throws IOException {

//		How to identify opening paragraph (apply only to sentence - typ=3. And only apply after stripped):
//			1. 	is byAndAmong pattern satisfied. Then +1
//			2. 	is dated as of /made as of /effective as of present. Then +1
//			3.	are 2 or more legal entities found. Then +1
//			4.	are 4 or more quotes found. Then +1
//			5.	is this pattern present - This [A-Z] then +1
//			6.  is contract type pattern present
//			7.	is the paragraph near the start of the documents - w/n 15%. Then +1 *but can filter.
//			8	is it first opening paragraph found in kId - Then +1 (*record count of opening paragraphs found).
//			*record location of opening paragraph (idx loc of text len of opening para = 30kb, text.len of doc=200kb, therefore 30/200=15%)
//			*put contractLongName in when recording sentence attributes. 
//			filter out idx locs past x% or lack of enough attributes.
//			opening paragraph cannot contain the word 'mean' or 'means'

		NLP nlp = new NLP();
		HashMap<Integer, List<String>> map = new HashMap<Integer, List<String>>();

		map.put(0, nlp.getAllMatchedGroups(text, Pattern.compile("(?ism)among|between")));// 1
		map.put(1, nlp.getAllMatchedGroups(text, Pattern.compile("(?ism)dated|made as of |effective as of")));// 2
		map.put(3, nlp.getAllMatchedGroups(text, Pattern.compile("(?ism)\"")));// 4
		map.put(4, nlp.getAllMatchedGroups(text, Pattern.compile("(?sm)(THIS|This) [A-Z]{1}")));// 5
		map.put(5, nlp.getAllMatchedGroups(text, patternContractTypesAllCapsOrInitialCaps));// 7
		map.put(6, nlp.getAllMatchedGroups(text, Pattern.compile("(?ism)mean|reconcil")));//

		return map;

	}

	public static List<String[]> contractNameFinderCount(String text) throws IOException {

		// to test getting these other types of docs use the other patterns in lieu of
		// patterbContractName
		NLP nlp = new NLP();

//		String text = Utils.readTextFromFile("c://temp/k.txt");
		List<String> listThis = nlp.getAllMatchedGroups(text, patternContractName);
//		NLP.printListOfString("listThis==", listThis);

//		listThis = nlp.getAllMatchedGroups(text, Pattern.compile("(?sm)(this|This|THIS) .{0,100}Indenture"));
//		System.out.println("thisList=" + listThis.size());
		String key = "", keyOrig = "";
		int val = 0;

		TreeMap<String, Integer> mapThisContractName = new TreeMap<String, Integer>();
		for (int i = 0; i < listThis.size(); i++) {
			keyOrig = listThis.get(i).replaceAll("[\r\n]+", " ").trim();
//			System.out.println(keyOrig);
			if (nlp.getAllIndexStartLocations(keyOrig,
					Pattern.compile("(?ism)(section|article|trustee|^agreement$|^Note$)")).size() > 0
					|| patternContractNameStopWords
							.matcher((keyOrig).replaceAll("([ ]{2,})+|[\\*\\$\\%\\d\\?\\!\\+=\\-;:]+", ""))
							.replaceAll("").length() + 5 < keyOrig.length())
				continue;
			key = keyOrig;
			keyOrig = "KOs" + keyOrig;
			key = key.toLowerCase();
//			System.out.println("key==" + key);
			val = 0;

			if (mapThisContractName.containsKey(key)) {
				val = mapThisContractName.get(key) + 1;
//				System.out.println(".put= key=" + key + " val=" + val);
				mapThisContractName.put(key, val);
			}

			if (!mapThisContractName.containsKey(key)) {
//				System.out.println("put= key=" + key + " val=" + 1);
				mapThisContractName.put(key, 1);
			}
		}

		TreeMap<Double, String> mapOrd = new TreeMap<Double, String>();
		// NLP.printMapStrInt("mapThisContrsctName", mapThisContractName);
		Double cntDbl = 0.0001;
		for (Map.Entry<String, Integer> entry : mapThisContractName.entrySet()) {
			mapOrd.put(-1 * (entry.getValue() + cntDbl), entry.getKey());
			cntDbl = cntDbl + 0.0001;
		}
		String firstVal = "";
		boolean foundThis = false;
		int score = 0;
		int inDoc = 0, cnt = 0, firstInDoc = 0;
		double idxD = 0, txtL = 0, firstsIdxD = 0, cntKn = 0;
//		NLP.printMapDoubleString("mapOrd===", mapOrd);
		String replStr = "([\\[\\]\\*\\$\\\\\\.\\(\\)])", kN = "";
		for (Map.Entry<Double, String> entry : mapOrd.entrySet()) {
			if (entry.getValue()
					.replaceAll(
							"(?ism)(((this )?(agreement)|" + patternNotContractName.toString() + ")|[^a-zA-Z]{0,10})+",
							"")
					.replaceAll("[ ]+", "").length() < 3)
				continue;

			if (cnt == 0) {
				firstVal = entry.getValue().replaceAll(replStr, "\\\\$1");
				kN = entry.getValue().toUpperCase();
				cntKn = -entry.getKey() / 5;
			}

			firstVal = entry.getValue().replaceAll(replStr, "\\\\$1");
//			System.out.println(
//					"replStr=" + replStr + " firstVal=" + firstVal + " entry.getValue()====" + entry.getValue());
			if (cnt > 0
					&& entry.getValue().trim().contains("this ") && nlp
							.getAllMatchedGroups(entry.getValue().replaceAll(replStr, "\\\\$1"),
									Pattern.compile(
											"(?sm)(" + firstVal.trim().replaceAll("(?ism)^this", "").trim() + ")"))
							.size() > 0) {
				score = 3;
				kN = entry.getValue().toUpperCase();
//				System.out.println("scor==3..thisthis. kn==" + kN);
				foundThis = true;
			}
			cnt++;
			if (nlp.getAllIndexEndLocations(text,
					Pattern.compile("(?i)(" + entry.getValue().replaceAll(replStr, "\\\\$1") + ")")).size() > 0) {
				idxD = (double) nlp.getAllIndexEndLocations(text,
						Pattern.compile("(?i)(" + entry.getValue().replaceAll(replStr, "\\\\$1") + ")")).get(0);
			} else {
				continue;
			}
			txtL = (double) text.length();
			inDoc = (int) ((idxD / txtL) * 100);
			if (inDoc < 3) {
				score = score + 1;
			}
			if (cnt == 1) {
				firstInDoc = inDoc;
				firstsIdxD = idxD;
			}
//			System.out.println("inDoc==" + inDoc + " cntKn=" + cntKn + " foundThis==" + foundThis);
			if ((inDoc > 25 || cntKn < 5 || foundThis) && score > 3)
				break;
			if (entry.getKey() > 6) {
				score = score + 1;
			}

//			System.out.println("key=" + entry.getKey() + " val=" + entry.getValue());
		}

		if (score > 3) {
			score = 4;
		}

		List<String[]> listKN = new ArrayList<String[]>();

		String[] aryKN = { kN.trim()
//				.replaceAll("(?ism)^this ", "")
				, (cntKn + "").substring(0, (cntKn + "").indexOf(".")),
				(firstsIdxD + "").substring(0, (firstsIdxD + "").indexOf(".")) + "", firstInDoc + "", score + "" };
//		System.out.println("ary...=" + Arrays.toString(aryKN));
		// [0]=kN,[1]=cnt,[2]=idx,%loc,score

		// if 'this...' is found then probably right. guess that as the name if no algo
		// results and not in openingPara. if in either one then good else guess and put
		// as score of 1. if not glaw and highest key count > lessor of 1 per * kb
		// size/2 and 10. then take #1. but use only if algo blank and this as
		// contractNameFinder. infer==this ... record idx of 'this'. see if match w/
		// algo and whenever true scor>3. this is really to say if K, and secondly to
		// see if i can get name. id contract name algo retain all IN doc%. so long as 1
		// word in #1 top cnt IN is also in other INs

		// include this, top cnt AND all must have max idx 5%

		// [0]=kN,[1]=cnt,[2]=idx,%loc,score

		listKN.add(aryKN);

		return listKN;
	}

	public static String reduceJson(String jsonText, int wCntLessThan, String[] typ, String[] fields) {
		// this removes from ingestion where: wCnt<7 (0r x); typ is NOT 1 or 3 (or 1,3);
		// and IF parsing <doc> remove ONLY these specified fields: vTxt; hashVtxtId;
		// <lead>;

		// if wCnt<7 -- contiune - don't parse this <doc>
		// if typ 1,3 or 1,3 -- continue - don't parse this <doc>
		// if parse <doc> remove <lead>
		// if parse <doc> remove <vTxt> and hashVtxtId

		return jsonText;
	}

	public static String parseClientText(String text) {
		text = "<DOCUMENT>\r\n<TEXT>\r\n<TYPE>485POS\r\n<FILENAME>123.htm</FILENAME>\r\n"
				+ "<DESCRIPTION>INDENTURE OR OFFERING SUMMARY</DESCRIPTION>\r\n" + text + "\r\n</TEXT>\r\n</DOCUMENT>";
		System.out.println("this is parsing a client sentence");

		return text;
	}

	public static List<String> getDocumentHeaderData(String headerText) throws IOException {
		NLP nlp = new NLP();
		GoLaw gl = new GoLaw();
		GetContracts gK = new GetContracts();

		Pattern patternFilename = Pattern.compile("(?ism)(?<=<FILENAME>).*?$");// edgar link filename
		Pattern patternDocType = Pattern.compile("(?ism)(?<=<TYPE>).*?$");
		List<String> listDocumenteHeaderData = new ArrayList<String>();
		// at the <DOCUMENT> level the meta data is: <TYPE>, <FILENAME> and
		// <DESCRIPTION>

		String filename = "", description = "", contractLongName, type = "";
		if (nlp.getAllMatchedGroups(headerText, GetContracts.patternDescription).size() > 0) {
			description = nlp.getAllMatchedGroups(headerText, GetContracts.patternDescription).get(0);
			description = description
					.replaceAll("(?is)(<DESCRIPTION>|[\\$\\*\r\n\\%\\&\\)\\(<>\"\'\\[\\]\\{\\}])|\\\\|\\/", " ")
					.replaceAll("[:;^\\%\\&\\*\\(\\)\\+\\=]", " ").replaceAll("[\t_\\-\\=]+|[ ]+", " ");

			contractLongName = description
					.replaceAll("(?i)(<DESCRIPTION>|IDEA: " + "|[\\$\\*\r\n\\%\\&\\)\\(<>\"\'\\[\\]\\{\\}])|\\\\|\\/",
							" ")
					.replaceAll("[^a-zA-Z-\\.\\d_ ]", " ").replaceAll("[ ]+", " ").trim();
			// System.out.println("description=" + description);
			contractLongName = contractLongName.substring(0, Math.min(contractLongName.length(), 120));
			listDocumenteHeaderData.add(description);
			listDocumenteHeaderData.add(contractLongName);
		}

		if (nlp.getAllMatchedGroups(headerText, patternFilename).size() > 0) {
			filename = nlp.getAllMatchedGroups(headerText, patternFilename).get(0);
			listDocumenteHeaderData.add(filename);
		}

		if (nlp.getAllMatchedGroups(headerText, patternDocType).size() > 0) {
			type = nlp.getAllMatchedGroups(headerText, patternDocType).get(0);
			listDocumenteHeaderData.add(type);
		}

		return listDocumenteHeaderData;

	}

//	public static boolean parseThisContract(String contractLongName, String type) {
//		
//		NLP nlp = new NLP();
//		GetContracts gc = new GetContracts();
//		
//					if (
//							 nlp.getAllIndexEndLocations(contractLongName, patternContractNameInContractLongNameAllCaps)
//									.size() == 0
//							&& nlp.getAllIndexEndLocations(contractLongName,
//									Pattern.compile("(?ism)contract|indenture|agreement|lease|declaration of trust|guarant"))
//									.size() == 0
//							&& (nlp.getAllIndexEndLocations(type, gc.patternsFormTypesGenerallyNotToParse).size() > 0
//									|| nlp.getAllIndexEndLocations(type, Pattern.compile("^EX")).size() == 0)
//
//					) {
//						return false;
//					}
//
//	}

	public static String parseNCtext(String ncFileText, List<String> listNCfileHeaderData, boolean parseContract)
			throws IOException, SQLException {
		GetContracts gc = new GetContracts();
		GoLaw gl = new GoLaw();
		NLP nlp = new NLP();

		Pattern patternDocument = Pattern.compile("(?ism)<DOCUMENT>.*?</DOCUMENT>");
		List<String> listDocuments = nlp.getAllMatchedGroups(ncFileText, patternDocument);
		List<String> listSecDocumenteHeaderData = new ArrayList<String>();
		boolean typeOktoParse = false, hasGoverningLaw = false;

		String kId = "", type = "", description = "", contractLongName = "", text = "", headerText, gLaw = "",
				strippedText;

		for (int c = 0; c < listDocuments.size(); c++) {
			hasGoverningLaw = true;
			typeOktoParse = true;

			text = listDocuments.get(c);
			headerText = text.substring(0, Math.min(text.length(), 5000));

			listSecDocumenteHeaderData = getDocumentHeaderData(headerText);
			// [0]=description,[1]=contractLongName,[2]=htmlFilename,[3]=type
			type = listSecDocumenteHeaderData.get(3);
			contractLongName = listSecDocumenteHeaderData.get(1);
			strippedText = gl.stripHtml(text);
			gLaw = GoLaw.getGoverningLaw(strippedText);// xxxxxx
			gLaw = gLaw.replaceAll("governing law not found", "").trim();
			if (gLaw.length() < 2) {
				hasGoverningLaw = false;
			}

			typeOktoParse = true;

			if (parseContract && !typeOktoParse && !hasGoverningLaw) {
				return null;
			}

			List<String> listAllMetaHeaderData = new ArrayList<String>();
			kId = listNCfileHeaderData.get(0) + "_" + c;
			for (int i = 0; i < listSecDocumenteHeaderData.size(); i++) {
				// [0]=acc,[1]=cik,[2]=fDate,[3]=companyName,[4]formType,[5]edgarFilePath,
				// [6]=description,[7]=contractLongName,[8]=filename
				// ,[9]=type,[10]fsize,[11]=gLaw
				listNCfileHeaderData.add(listSecDocumenteHeaderData.get(i));
			}
			listAllMetaHeaderData = listNCfileHeaderData;
			listAllMetaHeaderData.add(strippedText.length() + "");
			listAllMetaHeaderData.add(gLaw);

			if (parseContract && !typeOktoParse && !hasGoverningLaw) {
//				contractNameAlgo.len>40 || scor<5
				// return null
			}
		}

		return text;
	}

//	public static void parseThisContract(String text, boolean parseClientText) throws IOException, SQLException {
//
//		// if it has 1. governing law AND 2. contractNameAlgo OR type has regex '^EX':
//		// then it is a contract and parsed. This only works for contracts. Not
//		// documents such as disclosure or 10-Q etc.
//
//		if (parseClientText) {
//			text = parseClientText(text);
//		}
//
//		NLP nlp = new NLP();
//		int oPkNCnt = 0, oPkNthisCnt = 0;
//		Utils.createFoldersIfReqd("c:\\tempTest\\");
//		PrintWriter pw = new PrintWriter(new File("c:\\tempTest\\t1.txt"));
////		PrintWriter pwTxt = new PrintWriter(new File("c:\\temp\\TT.txt"));
//
//		List<String[]> listAry = new ArrayList<String[]>();
//
//		StringBuilder sb = new StringBuilder();
//		StringBuilder sbTxt = new StringBuilder();
//		String oPkName = "", openParaContractName = "", openParaContractNameReplaced = "", contractNameAlgo = "",
//				contractLongName = "", fnlKn = "", txt = "";
//
//		int score = 0, origScor = 0;
//		boolean needToFix = false, opContainsMeans = false;
//
//		String origTxt = Utils.readTextFromFile(file.getAbsolutePath()).replaceAll("(?<=,\"kId\" ?:\" ?)kId=", "")
//				.replaceAll("\r\n[ ]+", "\r\n").replaceAll("\" : \"", "\":\"").replaceAll("\" : ", "\":");
//
//		txt = origTxt;
//		sbTxt = new StringBuilder();
//		oP = "";
//		oPkName = "";
//		oPkNCnt = 0;// how many times does openParaConbtractName occur
//		oPkNthisCnt = 0;// how many times does openParaConbtractName occur preceded by this
//		GetContracts gk = new GetContracts();
//
//		String type = nlp.getAllMatchedGroups(txt.substring(0, 1000), Pattern.compile("(?<=\"type).*?(?=,?[\r\n])"))
//				.get(0).replace("/A", "").replaceAll("[:\"]+", "").trim();
//		List<Integer> listDisclosure = nlp.getAllIndexEndLocations(type, gk.patternsFormTypesGenerallyNotToParse);
//		boolean parse = true;
//		if (nlp.getAllMatchedGroups(txt.substring(0, 1000), Pattern.compile("(?<=score\" ?:)[\\d]+")).size() > 0) {
//			origScor = Integer.parseInt(
//					nlp.getAllMatchedGroups(txt.substring(0, 1000), Pattern.compile("(?<=score\" ?:)[\\d]+")).get(0));
//		}
////		System.out.println("txtsnip=" + txt.substring(0, 1000));
//		if (nlp.getAllMatchedGroups(txt.substring(0, 1000),
//				Pattern.compile("(?sm)(?<=contractNameAlgo).*?(?=,?[\r\n])")).size() > 0) {
//			contractNameAlgo = nlp
//					.getAllMatchedGroups(txt.substring(0, 1000),
//							Pattern.compile("(?sm)(?<=contractNameAlgo).*?(?=,?[\r\n])"))
//					.get(0).replaceAll("[\":]", "").trim();
//		}
//		if (nlp.getAllMatchedGroups(txt.substring(0, Math.min(1000, txt.length())),
//				Pattern.compile("(?<=openParaContractName).*?(?=,?[\r\n])")).size() > 0) {
//			openParaContractName = nlp
//					.getAllMatchedGroups(txt.substring(0, Math.min(1000, txt.length())),
//							Pattern.compile("(?<=openParaContractName).*?(?=,?[\r\n])"))
//					.get(0).replaceAll("[\":]", "").trim();
//		}
//
//		// if type is not the kind to parse -- such as 485 and score is low PR klago len
//		// is long
//		String governLaw = "";
//		governLaw = GoLaw.getGoverningLaw(origTxt);
//		governLaw = governLaw.replaceAll("governing law not found", "");
//		if (nlp.getAllMatchedGroups(origTxt.substring(0, origTxt.indexOf("gLaw") + 100),
//				Pattern.compile("(?ism)(?<=gLaw\" ?: ?\").*?(?=\")")).get(0).length() < 4 ||
//
//				nlp.getAllMatchedGroups("(?i)" + origTxt.substring(0, origTxt.indexOf("gLaw") + 100),
//						Pattern.compile("governing law not found")).size() > 0) {
//
//			origTxt = origTxt.replaceAll("(,? ?gLaw\" ?: ?\")(.*?)(\")", ",$1" + governLaw + "$3");
//			origTxt = origTxt.replaceAll(",? ?\"txt\" ?:", ",\"gLaw\" ?:\" ?" + governLaw + "\" ?\r\n,\"txt\" : ");
////			System.out.println("governLaw===" + governLaw);
//			txt = origTxt;
//			// add to text!
//		}
//		String gLaw = "";
//		if (nlp.getAllMatchedGroups(origTxt.substring(0, origTxt.indexOf("gLaw") + 100),
//				Pattern.compile("(?ism)(?<=gLaw\" ?: ?\").*?(?=\")")).get(0).length() > 3) {
//			gLaw = nlp.getAllMatchedGroups(origTxt.substring(0, origTxt.indexOf("gLaw") + 100),
//					Pattern.compile("(?ism)(?<=gLaw\" ?: ?\").*?(?=\")")).get(0);
//			origTxt = origTxt.replaceAll(",? ?\"txt\" ?: ?\"", ",\"gLaw\" : \"" + gLaw + "\"\r\n,\"txt\" : ");
//		}
//
//		origTxt = origTxt.replaceAll(",\r\n,", ",\r\n");
//
////		System.out.println("gLaw===" + gLaw);
//		List<String> listDoc = nlp.getAllMatchedGroups(txt, Pattern.compile("(?sm)\\{.*?\\}"));
//
//		if (listDisclosure.size() > 0 && (origScor < 5 || contractNameAlgo.length() > 40))
//			parse = false;
//		if (!parse) {
//			if (nlp.getAllMatchedGroups(listDoc.get(0),
//					Pattern.compile("(?sm)(?<=openingParagraph\" ?: ?\").*?(?=,?[\r\n])")).size() > 0) {
//				oP = nlp.getAllMatchedGroups(listDoc.get(0),
//						Pattern.compile("(?sm)(?<=openingParagraph\" ?: ?\").*?(?=,?[\r\n])")).get(0);
//			}
//			type = "";
////			System.out.println("listDoc.get0=" + listDoc.get(0));
//			if (nlp.getAllMatchedGroups(listDoc.get(0),
//					Pattern.compile("(?<=contractLongName\" ?: ?\").*?(?=,?[\r\n])")).size() > 0) {
//				contractLongName = nlp.getAllMatchedGroups(listDoc.get(0),
//						Pattern.compile("(?<=contractLongName\" ?: ?\").*?(?=,?[\r\n])")).get(0);
//				System.out.println("contractLongName=x=" + contractLongName);
//			}
//
//			if (governLaw.length() > 3 && (nlp
//					.getAllIndexEndLocations(contractNameAlgo + " " + contractLongName,
//							patternContractNameInContractLongNameAllCaps)
//					.size() > 0
//					|| nlp.getAllIndexEndLocations(contractNameAlgo + " " + contractLongName,
//							Pattern.compile("(?ism)contract|indenture|agreement|lease|declaration of trust|guarant"))
//							.size() > 0
//					|| nlp.getAllMatchedGroups(oP.substring(0, 30), Pattern.compile("(?ism)(" + "("
//							+ "POWER OF ATTORNEY|CONFIRMATION|COMMITMENT LETTER|LETTER AGREEMENT|POOLING AND SERVICING|DEBENTURE|DEED OF TRUST|(SUPPLEMENTAL )?INDENTURE|(AMENDED AND RESTATED )?(AGREEMENT AND )?"
//							+ "PLAN OF (MERGER|ACQUISITION|STOCK PURCHASE)"
//							+ "|AGREEMENT TO MERGE AND PLAN OF REORGANIZATION|(AMENDED AND RESTATED |SUPPLEMENTAL )(INDENTURE)|GUARANTEE|GUARANTY|[A-Z]?!LEASE"
//							+ "|(SUBORDINATED|JUNIOR|PRMISSORY|SENIOR)? ?NOTE|WARRANT|CONTRACT" + "" + "|AGREEMENT))"))
//							.size() > 0)
//
//			) {
//
//				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
//				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//				pw.close();
////				System.out.println("1. do parse=" + "\r\norigScor=" + origScor + "\r\n contractLongName="
////						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
////						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
//				return;
//			}
//
//			else {
//				pw = new PrintWriter(new File(parseTheseNotFolder + file.getName()));
//				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//				pw.close();
////				System.out.println("1. do not parse=" + "\r\norigScor=" + origScor + "\r\n contractLongName="
////						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
////						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
//				return;
//			}
//		}
//
//		for (int i = 0; i < 1; i++) {
//			// ck doc0
//			opContainsMeans = false;
//			score = 0;
//			origScor = 0;
//			oPkName = "";
//			openParaContractName = "";
//			openParaContractNameReplaced = "";
////			contractNameAlgo = "";
//			contractLongName = "";
//			fnlKn = "";
//			if (nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("(?<=score?\" ?:)[\\d]+")).size() > 0) {
////				System.out.println("listDoc=====" + listDoc.get(i));
//				origScor = Integer.parseInt(
//						nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("(?<=score?\" ?:)[\\d]+")).get(0));
//			}
//
//			if (nlp.getAllMatchedGroups(listDoc.get(0), Pattern.compile("(?<=openParaContractName\" ?: ?\").*?(?=\")"))
//					.size() > 0) {
//				openParaContractName = nlp.getAllMatchedGroups(listDoc.get(0),
//						Pattern.compile("(?<=openParaContractName\" ?: ?\").*?(?=\")")).get(0);
//			}
//
//			if (nlp.getAllMatchedGroups(listDoc.get(0), Pattern.compile("(?<=contractNameAlgo\" ?: ?\").*?(?=\")"))
//					.size() > 0) {
//				contractNameAlgo = nlp
//						.getAllMatchedGroups(listDoc.get(0), Pattern.compile("(?<=contractNameAlgo\" ?: ?\").*?(?=\")"))
//						.get(0);
//			}
//
//			if (nlp.getAllMatchedGroups(listDoc.get(0),
//					Pattern.compile("(?<=contractLongName=?\" ?: ?\").*?(?=\",? ?[\r\n]{1})")).size() > 0) {
//				contractLongName = nlp.getAllMatchedGroups(listDoc.get(i),
//						Pattern.compile("(?<=contractLongName=?\" ?: ?\").*?(?=\",? ?[\r\n]{1})")).get(0);
//				// PA: should it be (?sm) matches all lines
//			}
//
//			openParaContractNameReplaced = openParaContractName
//					.replaceAll("(?sm)" + patternNotContractName.toString(), "").trim();
////			System.out.println("1. origScor=" + origScor + " orig contractNameAlgo=" + contractNameAlgo
////					+ " openParaContractName==" + openParaContractName + " openParaContractNameReplaced=|"
////					+ openParaContractNameReplaced + "| contractLongName=" + contractLongName + "\r\n");
//
//			if ((nlp.getAllMatchedGroups(listDoc.get(i),
//					Pattern.compile("\"gLaw\" ?: ?\".{1,2}governing law not found")).size() > 0
//					|| nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("\"gLaw\" ?: ?\"\'?\'?\"")).size() > 0)
//
//					&& nlp.getAllMatchedGroups(contractNameAlgo, Pattern.compile("(?ism)^insuring agreement"))
//							.size() == 0
//					&& nlp.getAllMatchedGroups(contractLongName,
//							Pattern.compile("(?ism)^EX|contract|indenture|agreement|lease|declaration of trust"))
//							.size() == 0) {
//
//				pw = new PrintWriter(new File(parseTheseNotFolder + file.getName()));
//				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//				pw.close();
////				System.out.println("2. do not parse K =" + "\r\norigScor=" + origScor + "\r\ncontractLongName="
////						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
////						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
//				return;
//			}
//
//			if (origScor > 2 && contractNameAlgo.replaceAll(
//					"(?ism)(((this )?(agreement)|" + patternNotContractName.toString() + ")|[^a-zA-Z]{0,10})+", "")
//					.length() < 50 &&
//
//					nlp.getAllIndexEndLocations(contractLongName.replaceAll("[^A-Za-z]", "").trim(),
//							Pattern.compile("(?ism)" + contractNameAlgo.replaceAll("[^A-Za-z]", "").trim()))
//							.size() == 0) {
//				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
//				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//				pw.close();
////				System.out.println("3. OKAY1. No chg =" + "\r\norigScor=" + origScor + "\r\ncontractLongName="
////						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
////						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
//				return;
//
//			}
//
////			System.out.println("listDoc==" + listDoc.get(i));
////			System.out.println(
////					"txt===" + nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("(?sm)(?<=\"txt\":).*?\r\n"))
////							.get(0).replaceAll("\r\n|,$|\"$|^\"", "") + "\r\n");
////			"openingParagraph":"^
//			if (nlp.getAllMatchedGroups(listDoc.get(i),
//					Pattern.compile("(?<=openingParagraph\" ?: ?\").*?(?=,?[\r\n])")).size() > 0) {
//				oP = nlp.getAllMatchedGroups(listDoc.get(i),
//						Pattern.compile("(?sm)(?<=openingParagraph\" ?: ?\").*?(?=,?[\r\n])")).get(0);
//				oP = oP.substring(0, Math.min(oP.length(), 80)).replaceAll("-", " ");
////				System.out.println("oP===="
////						+ nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("(?sm)(openingParagraph).*?[\r\n]"))
////								.get(0));
////				System.out.println("snippet oP==" + oP + "\r\n");
//				if (oP.contains(" means? "))
//					opContainsMeans = true;
////				System.out.println("listDoc.get==" + listDoc.get(i));
//
//			}
//
////			if (nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("(?<=openingParagraph\" ?: ?\").*?(?=[\r\n])"))
////					.size() == 0) {
////				System.out.println("no openinigParagraph+\r\n");
////			}
//
//			if (origScor < 3
////					|| contractName.replaceAll("(?ism)" + patternNotContractName.toString(), "").replaceAll("[ ]+", "")
////							.length() < 3
//					|| contractNameAlgo.replaceAll(
//							"(?ism)(((this )?(agreement)|" + patternNotContractName.toString() + ")|[^a-zA-Z]{0,10})+",
//							"").length() > 60) {
//
//				needToFix = true;
//			}
//
//			if (contractNameAlgo.replaceAll("(?ism)this| and | of ", "").length() - patternContractNameStopWords
//					.matcher(contractNameAlgo.toLowerCase().replaceAll("(?ism)this| and | of ", "")).replaceAll("")
//					.length() > 5) {
//				needToFix = true;
//			}
//
//			if (!needToFix) {
//				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
//				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//				pw.close();
////				System.out.println("4. OKAY2. No chg =" + "\r\norigScor=" + origScor + "\r\ncontractLongName="
////						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
////						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
//				return;
//			}
//		}
//
//		for (int i = 1; i < listDoc.size(); i++) {
//			if (nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("\"typ\" ?: ?(\\[ ?1, ?3 ?\\]|1)"))
//					.size() == 0) {
//				// only fetch typ para
////				System.out.println("continue...");
//				continue;
//			}
//			if (nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("(?sm)(?<=\"txt\" ?: ?\").*?\"")).size() == 0) {
//				continue;
//			}
////			System.out.println("xxxxxxxxxxxxxx"+listDoc.get(i));
//			sbTxt.append(nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("(?sm)(?<=\"txt\" ?: ?\").*?\""))
//					.get(0).replaceAll("[\r\n]+|,$|\"$|^\"", "").replaceAll("(\\\\r\\\\n|\\\\n|\\\\r)+", "\r\n")
//					+ "\r\n");// ,"txt":"
//		}
//
//		if (openParaContractNameReplaced.length() > 0 && nlp.getAllMatchedGroups(oP, patternContractName).size() > 0) {
////			System.out.println("openParaContractName???=="
////					+ openParaContractName.replaceAll("(?sm)" + patternNotContractName.toString(), ""));
////			System.out.println("found opening openParaContractName="
////					+ nlp.getAllMatchedGroups(sbTxt.toString(), Pattern.compile("(?ism)" + openParaContractName))
////							.size());
////			System.out.println("but here is oP kName ptrn==" + nlp.getAllMatchedGroups(oP, patternContractName).get(0));
//			oPkNthisCnt = nlp
//					.getAllMatchedGroups(sbTxt.toString(), Pattern.compile("(?ism)this " + openParaContractName))
//					.size();
//
//			oPkNCnt = nlp.getAllMatchedGroups(sbTxt.toString(), Pattern.compile("(?ism)" + openParaContractName))
//					.size();
////			System.out.println("oPkNthisCnt==" + oPkNthisCnt + "");
////			System.out.println("oPkNCnt==" + oPkNCnt + "\r\n");
//
//		}
//
//		if (nlp.getAllIndexEndLocations(oP, Pattern.compile("(^\\^This|^\\^this|^\\^THIS) "
//				+ openParaContractName.replaceAll("\\^", "") + "(?ism)(Agreement|indenture)?")).size() > 0
//
//		) {
//			score = 3;
//			openParaContractName = nlp
//					.getAllMatchedGroups(oP, Pattern.compile(
//							"(^\\^This|^\\^this|^\\^THIS) " + openParaContractName + "(?ism)( Agreement| indenture)?"))
//					.get(0).replaceAll("(?ism)\\^this ", "");
////			System.out.println("IT IS openParaContractName==========" + openParaContractName);
//
//		}
//
//		if (oPkNthisCnt > 3 || oPkNCnt > 8) {
//			score = 2;
//			if (oPkNthisCnt > 3)
//				score = 3;
//			fnlKn = openParaContractName;
////			System.out.println(
////					"fnlKn=" + fnlKn + " scor=" + scor + " oPkNthisCnt==" + oPkNthisCnt + " oPkNCnt==" + oPkNCnt);
//		}
//
//		String txtSnip = sbTxt.toString().substring(0, Math.min(15000, sbTxt.toString().length()));
//
//		if (fnlKn.length() == 0) {
//			listAry = contractNameFinderCount(txtSnip);
//			// using contract name method b/c nothing found in openParaContractName field.
////			NLP.printListOfStringArray("listAry===x==", listAry);
//
//			sb.append("\r\n\r\n" + file.getName() + " ");
//			// System.out.println("1111. oP="+oP);
//
//			if (nlp.getAllMatchedGroups(oP, patternContractName).size() > 0
//					|| nlp.getAllMatchedGroups(oP, Pattern.compile("^\\^(INDENTURE|Indenture)")).size() > 0) {
//				// ckg if there's a kname in oP
//
//				if (nlp.getAllMatchedGroups(oP, patternContractName).size() > 0)
//					oPkName = nlp.getAllMatchedGroups(oP, patternContractName).get(0).replaceAll("\\^", "");
//
//				if (nlp.getAllMatchedGroups(oP, Pattern.compile("^\\^(INDENTURE|Indenture)")).size() > 0)
//					oPkName = nlp.getAllMatchedGroups(oP, Pattern.compile("^\\^(INDENTURE|Indenture)")).get(0)
//							.replaceAll("\\^", "");
//
////				System.out.println("\r\nkName in oP=" + oPkName + "\r\n");
//
//			}
//
//			for (int c = 0; c < listAry.size(); c++) {
////				System.out.println("listAry=" + Arrays.toString(listAry.get(c)) + " scor=" + score);
//				sb.append(Arrays.toString(listAry.get(c)) + "\r\n");
//				if (!listAry.get(c)[0].toLowerCase().contains("this") && oPkName.length() > 0 && !opContainsMeans
//						&& (listAry.get(c)[0].replaceAll("(?ism)(((this )?(agreement)|"
//								+ patternNotContractName.toString() + ")|[^a-zA-Z]{0,10})+", "").length() == 0
//								|| score < 3)) {
//
////					System.out.println("picked oP=" + oPkName + " score=" + 3 + "\r\n");
//					fnlKn = oPkName;
//					score = 3;
////				System.out.println("but ary is" + Arrays.toString(listAry.get(c)));
//				} else {
////					contractName = listAry.get(c)[0] + " score==" + listAry.get(c)[4];
////					System.out.println("10. oPkN=" + oPkName);
//					score = Integer.parseInt(listAry.get(c)[4]);
//					if (oPkName.contains(listAry.get(c)[0])) {
//						if (score < 3)
//							score = 4;
//					} else {
//						score = 3;
//					}
//					fnlKn = listAry.get(c)[0];
////					System.out.println("listary fnlKn=" + fnlKn + " scor==" + score);
//				}
//
////				if (contractNameAlgo
////						.replaceAll("(?ism)(((this )?(agreement)|" + patternNotContractName.toString()
////								+ ")|[^a-zA-Z]{0,10})+", "")
////						.length() > 60
////						&& (fnlKn.length() < 3 || fnlKn.replaceAll("(?ism)(((this )?(agreement)|"
////								+ patternNotContractName.toString() + ")|[^a-zA-Z]{0,10})+", "").length() > 60)) {
////					System.out.println("1=x=too long contractNameAlgo=" + contractNameAlgo);
////					System.out.println("1=x=contractLongName==" + contractLongName);
////					System.out.println("1=x=fnlKn==" + fnlKn);
////				}
//
//				if (score < 3
//						&& fnlKn.replaceAll("(?ism)(((this )?(agreement)|" + patternNotContractName.toString()
//								+ ")|[^a-zA-Z]{0,10})+", "").length() > 7
//
//						&& nlp.getAllIndexStartLocations(txtSnip, Pattern.compile("(?ism)" + fnlKn)).size() > 5) {
//					score = 2;
//				}
//
//				if (fnlKn.length() == 0) {
//					fnlKn = openParaContractName;
//					score = 1;
////					System.out.println("fnl kn is openParaContractName=" + fnlKn + " scor=" + score);
//				}
//			}
////			System.out.println("2=x=contractNameAlgo=" + contractNameAlgo);
////			System.out.println("2=x=contractLongName==" + contractLongName);
////			System.out.println("filename====" + file.getName() + " oP==" + oP);
////			System.out.println("2=x=          fnlKn==" + fnlKn);
////			System.out.println("2=x=          scor==" + score);
//		}
//
//		fnlKn = fnlKn.replaceAll("(?ism)(^this )", "");
//
//		if (nlp.getAllIndexEndLocations(fnlKn.replaceAll("[^A-Za-z]", "").trim(),
//				Pattern.compile("(?ism)" + contractLongName.replaceAll("[^A-Za-z]", "").trim())).size() > 0) {
//			if (fnlKn.replaceAll(patternNotContractName.toString(), "").length() > 4) {
//				score = score + 2;
////				System.out.println("1******fnlKn is contractLongName*****==" + fnlKn + " score==" + score + "\r\n");
//			}
//		}
//
//		if (nlp.getAllIndexEndLocations(contractLongName.replaceAll("[^A-Za-z]", "").trim(),
//				Pattern.compile("(?ism)" + fnlKn.replaceAll("[^A-Za-z]", "").trim())).size() > 0) {
//			if (contractLongName.replaceAll(patternNotContractName.toString(), "").length() > 4) {
//				score = score + 2;
//				fnlKn = contractLongName;
////				System.out.println("2******fnlKn is contractLongName*****==" + fnlKn + " score==" + score + "\r\n");
//			}
//		}
//
//		if (nlp.getAllIndexEndLocations(openParaContractName.replaceAll("[^A-Za-z]", "").trim(),
//				Pattern.compile("(?ism)" + fnlKn.replaceAll("[^A-Za-z]", "").trim())).size() > 0) {
//			if (openParaContractName.replaceAll(patternNotContractName.toString(), "").length() > 4) {
//				score = 3;
//				fnlKn = openParaContractName;
////				System.out.println("3******fnlKn is contractLongName*****==" + fnlKn + " score==" + score + "\r\n");
//			}
//		}
//
//		if (fnlKn.replaceAll("(?ism)( |agreement|contract|amended|restated|and )+", "").length() < 4
//				|| fnlKn.replaceAll("([^A-Za-z]+)", "").length() < 4
//				|| nlp.getAllIndexEndLocations(fnlKn,
//						Pattern.compile("(?ism)(DEBENTURE|DEED|SUPPLEMENTAL|MERGER|PLAN OF|INDENTURE"
//								+ "|DECLARATION OF TRUST|AGREEMENT|GUARANTEE|GUARANTY|LEASE|NOTE|WARRANT|CONTRACT|VOTING|CONTRACT)"))
//						.size() == 0) {
////			System.out.println("fnlKn was=" + fnlKn + " now blank fnlKn=" + fnlKn);
//			fnlKn = "";
//			score = 0;
//		}
//		if (fnlKn.replaceAll("(?ism)this| and | of ", "").length() - patternContractNameStopWords
//				.matcher(fnlKn.toLowerCase().replaceAll("(?ism)this| and | of ", "")).replaceAll("").length() > 5) {
////			System.out.println("2222");
//			fnlKn = "";
//			score = 0;
//		}
//
//		if (fnlKn.length() > 3 && score < 2) {
//			score = 3;
//		}
//
//		if (fnlKn.length() == 0 && nlp.getAllIndexStartLocations(oP, Pattern.compile("(?ism)\\^Indenture")).size() > 0
//				&& !opContainsMeans) {
////			System.out.println("it is indenture");
//			fnlKn = "INDENTURE";
//			score = 3;
//		}
//
//		if (fnlKn.length() > 0) {
//			if (fnlKn.length() < 6 && openParaContractName.length() > 6) {
//				score = 3;
//				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
//				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
//				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\" : " + score + "$1");
//				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"contractNameAlgo\" : \""
//						+ openParaContractName.replaceAll("(?ism)\\^?THIS", "") + "\"$1");
//
//				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
//				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//				pw.close();
//				fnlKn = openParaContractName;
////				System.out.println("5a. new kAlgo=" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
////						+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
////						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
////						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
//				return;
//			} else {
//				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
//				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
//				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\" : " + score + "$1");
//				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
//						"\r\n,\"contractNameAlgo\":\"" + fnlKn + "\"$1");
////				System.out.println("parseTheseFolder===" + parseTheseFolder);
//				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
//				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//				pw.close();
////				System.out.println("5b. new kAlgo=" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
////						+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
////						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
////						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
//				return;
//			}
//
//		}
//
//		if (fnlKn.trim().length() == 0 && contractNameAlgo.trim().length() == 0
//				&& (nlp.getAllMatchedGroups(contractLongName, patternContractNameInContractLongNameAllCaps).size() == 0
//						|| contractLongName.replaceAll(patternNotContractName.toString(), "")
//								.replaceAll("[^A-Za-z ]+", "").length() < 4)) {
//
//			if (openParaContractName.replaceAll(patternNotContractName.toString(), "")
//					.replaceAll("(?ism)\\^?This ?", "").length() > 3) {
//
//				score = 3;
//				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
//				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
//				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\" : " + score + "$1");
//				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
//						"\r\n,\"contractNameAlgo\" : \"" + openParaContractName + "\"$1");
//
//				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
//				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//				pw.close();
//
////				System.out.println(
////						"nlp.getAllMatchedGroups(contractLongName, patternContractNameInContractLongNameAllCaps).size()="
////								+ nlp.getAllMatchedGroups(contractLongName, patternContractNameInContractLongNameAllCaps)
////										.get(0));
////				System.out.println("6d. NO kAlgo=" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
////						+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
////						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
////						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
//				return;
//			}
////			System.out.println("txt600=" + sbTxt.toString().substring(0, 600));
//			if (nlp.getAllIndexEndLocations(sbTxt.substring(0, Math.min(sbTxt.toString().length(), 600)),
//					Pattern.compile("(?ism)Underwriting Agreement")).size() > 0) {
//
//				score = 3;
//				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
//				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
//				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\" : " + score + "$1");
//				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
//						"\r\n,\"contractNameAlgo\" : \"Underwriting Agreement\"$1");
//
//				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
//				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//				pw.close();
//				return;
//			} else {
////				System.out.println(
////						"ex.siz==" + nlp.getAllMatchedGroups(contractLongName, Pattern.compile("(?ism)^EX")).size());
////				System.out.println("glaw.len==" + governLaw.length());
//				if (
//
//				(nlp.getAllMatchedGroups(contractLongName, Pattern.compile("(?ism)(" + "("
//						+ "^EX|POWER OF ATTORNEY|CONFIRMATION|COMMITMENT LETTER|LETTER AGREEMENT|POOLING AND SERVICING|DEBENTURE"
//						+ "|DEED OF TRUST|(SUPPLEMENTAL )?INDENTURE|(AMENDED AND RESTATED )?(AGREEMENT AND )?"
//						+ "PLAN OF (MERGER|ACQUISITION|STOCK PURCHASE)"
//						+ "|AGREEMENT TO MERGE AND PLAN OF REORGANIZATION|(AMENDED AND RESTATED |SUPPLEMENTAL )"
//						+ "(INDENTURE)|GUARANTEE|GUARANTY|[A-Z]?!LEASE"
//						+ "|(SUBORDINATED|JUNIOR|PRMISSORY|SENIOR)? ?NOTE|WARRANT|CONTRACT" + "" + "|AGREEMENT))"))
//						.size() > 0
//
//						||
//
//						nlp.getAllMatchedGroups(type, Pattern.compile("(?ism)(" + "("
//								+ "^EX|POWER OF ATTORNEY|CONFIRMATION|COMMITMENT LETTER|LETTER AGREEMENT|POOLING AND SERVICING|DEBENTURE"
//								+ "|DEED OF TRUST|(SUPPLEMENTAL )?INDENTURE|(AMENDED AND RESTATED )?(AGREEMENT AND )?"
//								+ "PLAN OF (MERGER|ACQUISITION|STOCK PURCHASE)"
//								+ "|AGREEMENT TO MERGE AND PLAN OF REORGANIZATION|(AMENDED AND RESTATED |SUPPLEMENTAL )"
//								+ "(INDENTURE)|GUARANTEE|GUARANTY|[A-Z]?!LEASE"
//								+ "|(SUBORDINATED|JUNIOR|PRMISSORY|SENIOR)? ?NOTE|WARRANT|CONTRACT" + ""
//								+ "|AGREEMENT))")).size() > 0
//
//				) && (governLaw.length() > 3
//						|| nlp.getAllMatchedGroups(origTxt.substring(0, origTxt.indexOf("gLaw") + 100),
//								Pattern.compile("(?ism)(?<=gLaw\" ?: ?\").*?(?=\")")).get(0).length() > 4)
//						&& (nlp.getAllMatchedGroups(oP.substring(0, Math.min(50, oP.length())),
//								Pattern.compile("(?ism)(" + "("
//										+ "POWER OF ATTORNEY|CONFIRMATION|COMMITMENT LETTER|LETTER AGREEMENT|POOLING AND SERVICING"
//										+ "|DEBENTURE|DEED OF TRUST|(SUPPLEMENTAL )?INDENTURE|(AMENDED AND RESTATED )?(AGREEMENT AND )?"
//										+ "PLAN OF (MERGER|ACQUISITION|STOCK PURCHASE)"
//										+ "|AGREEMENT TO MERGE AND PLAN OF REORGANIZATION|(AMENDED AND RESTATED |SUPPLEMENTAL )(INDENTURE)|GUARANTEE|GUARANTY|[A-Z]?!LEASE"
//										+ "|(SUBORDINATED|JUNIOR|PRMISSORY|SENIOR)? ?NOTE|WARRANT|CONTRACT|AGREEM))"))
//								.size() > 0 ||
//
//								nlp.getAllMatchedGroups(
//										sbTxt.toString().substring(0, Math.min(500, sbTxt.toString().length())),
//										Pattern.compile("(?ism)(" + "("
//												+ "POWER OF ATTORNEY|CONFIRMATION|COMMITMENT LETTER|LETTER AGREEMENT|POOLING AND SERVICING"
//												+ "|DEBENTURE|DEED OF TRUST|(SUPPLEMENTAL )?INDENTURE|(AMENDED AND RESTATED )?(AGREEMENT AND )?"
//												+ "PLAN OF (MERGER|ACQUISITION|STOCK PURCHASE)"
//												+ "|AGREEMENT TO MERGE AND PLAN OF REORGANIZATION|(AMENDED AND RESTATED |SUPPLEMENTAL )(INDENTURE)|GUARANTEE|GUARANTY|[A-Z]?!LEASE"
//												+ "|(SUBORDINATED|JUNIOR|PRMISSORY|SENIOR)? ?NOTE|WARRANT|CONTRACT|AGREEM))"))
//										.size() > 0)
//
//				) {
////					System.out.println("has glaw & ^EX in contractLongName...............");
//					pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
//					pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//					pw.close();
////					System.out.println("6a. parse NO kAlgo=" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
////							+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
////							+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
////							+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
//					return;
//				} else {
//					pw = new PrintWriter(new File(parseTheseNotFolder + file.getName()));
//					pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//					pw.close();
////					System.out.println(oP);
////					System.out.println("6b. not parse NO kAlgo=" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
////							+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
////							+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
////							+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\nGOVERNlAW=" + governLaw + " oP="
////							+ oP.substring(0, Math.min(50, oP.length())));
//					return;
//				}
//			}
//		}
//
//		if (origScor < 3) {
//
////System.out.println("openParaContractName="+openParaContractName);
////System.out.println("contractLongName="+contractLongName+"||");
//			if (
//
////					nlp.getAllMatchedGroups(openParaContractName, Pattern.compile("(?ism)" + contractNameAlgo)).size() > 0
////					||
//			nlp.getAllMatchedGroups(openParaContractName, Pattern.compile("(?ism)" + contractLongName)).size() > 0
//					&& nlp.getAllMatchedGroups(openParaContractName, Pattern.compile("(?ism)" + contractLongName))
//							.get(0).replaceAll(patternNotContractName.toString(), "").length() > 3
//
//			) {
////				System.out.println("it is opkn");
//				fnlKn = openParaContractName;
//				score = 3;
//
//				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
//				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
//				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\":" + score + "$1");
//				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
//						"\r\n,\"contractNameAlgo\":\"" + fnlKn + "\"$1");
//
//				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
//				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//				pw.close();
////				
////				System.out.println("7a. chg score to 3" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
////						+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
////						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
////						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
//				return;
//			}
//
//			if (
////					nlp.getAllMatchedGroups(contractLongName, Pattern.compile("(?ism)" + contractNameAlgo)).size() > 0
////					||
//			nlp.getAllMatchedGroups(contractLongName, Pattern.compile("(?ism)" + openParaContractName)).size() > 0
//					&& nlp.getAllMatchedGroups(contractLongName, Pattern.compile("(?ism)" + openParaContractName))
//							.get(0).replaceAll(patternNotContractName.toString(), "").length() > 3) {
////				System.out.println("1 it is klongname");
//				fnlKn = contractLongName;
//				score = 3;
//
//				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
//				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
//				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\" : " + score + "$1");
//				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
//						"\r\n,\"contractNameAlgo\" : \"" + fnlKn + "\"$1");
//
//				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
//				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//				pw.close();
////				
////				System.out.println("7b. chg score to 3" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
////						+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
////						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
////						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
//				return;
//			}
//
//			if (
////					nlp.getAllMatchedGroups(contractLongName, Pattern.compile("(?ism)" + contractNameAlgo)).size() > 0
////					||
//			nlp.getAllMatchedGroups(openParaContractName.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", ""),
//					Pattern.compile("(?ism)" + contractNameAlgo.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", "")))
//					.size() > 0
//					&& nlp.getAllMatchedGroups(
//							openParaContractName.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", ""),
//							Pattern.compile(
//									"(?ism)" + contractNameAlgo.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", "")))
//							.get(0).replaceAll(patternNotContractName.toString(), "").length() > 3) {
////				System.out.println("1 kalgo in oPkn");
//				fnlKn = contractNameAlgo;
//				score = 3;
//
//				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
////				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
//				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\":" + score + "$1");
////				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
////						"\r\n,\"contractNameAlgo\":\"" + fnlKn + "\"$1");
//
//				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
//				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//				pw.close();
////				
////				System.out.println("7e. chg score to 3" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
////						+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
////						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
////						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
//				return;
//			}
//
//			if (
////					nlp.getAllMatchedGroups(contractLongName, Pattern.compile("(?ism)" + contractNameAlgo)).size() > 0
////					||
//			nlp.getAllMatchedGroups(contractLongName.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", ""),
//					Pattern.compile("(?ism)" + contractNameAlgo.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", "")))
//					.size() > 0
//					&& nlp.getAllMatchedGroups(contractLongName.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", ""),
//							Pattern.compile(
//									"(?ism)" + contractNameAlgo.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", "")))
//							.get(0).replaceAll(patternNotContractName.toString(), "").length() > 3
//
//			) {
////				System.out.println("2 it is klongname");
//				fnlKn = contractLongName;
//				score = 3;
//				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
//				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
//				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\" : " + score + "$1");
//				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
//						"\r\n,\"contractNameAlgo\":\"" + fnlKn + "\"$1");
//
//				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
//				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//				pw.close();
////				
////				System.out.println("7c. chg score to 3" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
////						+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
////						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
////						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
//				return;
//			}
//		}
//
//		if (contractNameAlgo.length() == 0 && fnlKn.length() == 0
//				&& (nlp.getAllMatchedGroups(contractLongName.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", ""),
//						patternContractName).size() > 0
//						|| contractLongName.replaceAll(patternNotContractName.toString(), "")
//								.replaceAll("[^A-Za-z ]+", "").length() > 3)) {
//
//			fnlKn = contractLongName;
//			score = 3;
//			origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
//			origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
//			origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\":" + score + "$1");
//			origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
//					"\r\n,\"contractNameAlgo\":\"" + fnlKn + "\"$1");
//
//			pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
//			pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//			pw.close();
////			
////			System.out.println("7d. chg score to 3" + "\r\norigScor=" + origScor + " fnlKn=" + fnlKn
////					+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
////					+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
////					+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
//			return;
//		}
//
//		if (openParaContractName.length() > 0 && fnlKn.length() == 0
//				&& nlp.getAllMatchedGroups("\\^THIS " + openParaContractName.toUpperCase() + "\r\n",
//						patternContractName).size() > 0
//				&& openParaContractName.replaceAll(patternNotContractName.toString(), "").replaceAll("[^A-Za-z ]+", "")
//						.length() > 3
//				&& patternContractNameStopWords.matcher(contractNameAlgo.toLowerCase()).replaceAll("")
//						.length() < contractNameAlgo.length() - 5
//
//		) {
//
//			fnlKn = openParaContractName;
//			score = 3;
//			origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
//			origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
//			origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\":" + score + "$1");
//			origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
//					"\r\n,\"contractNameAlgo\":\"" + fnlKn + "\"$1");
//
//			pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
//			pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//			pw.close();
////			
////			System.out.println("7E. chg score to 3" + "\r\norigScor=" + origScor + " fnlKn=" + fnlKn
////					+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
////					+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
////					+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
//			return;
//		}
//
//		else {
//			pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
//			pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
//			pw.close();
////			System.out.println("8. NO CHG=" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
////					+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
////					+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
////					+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
//			return;
//		}
//
//	}

	public static void checkContractNameAlgoInSolrJson(File file) throws IOException, SQLException {

		NLP nlp = new NLP();
		int oPkNCnt = 0, oPkNthisCnt = 0;
		Utils.createFoldersIfReqd("c:\\tempTest\\");
		PrintWriter pw = new PrintWriter(new File("c:\\tempTest\\t1.txt"));
//		PrintWriter pwTxt = new PrintWriter(new File("c:\\temp\\TT.txt"));

		List<String[]> listAry = new ArrayList<String[]>();

		StringBuilder sb = new StringBuilder();
		StringBuilder sbTxt = new StringBuilder();
		String oPkName = "", openParaContractName = "", openParaContractNameReplaced = "", contractNameAlgo = "",
				contractLongName = "", fnlKn = "", txt = "";

		int score = 0, origScor = 0;
		boolean needToFix = false, opContainsMeans = false;

		String origTxt = Utils.readTextFromFile(file.getAbsolutePath()).replaceAll("(?<=,\"kId\" ?:\" ?)kId=", "")
				.replaceAll("\r\n[ ]+", "\r\n").replaceAll("\" : \"", "\":\"").replaceAll("\" : ", "\":");

		txt = origTxt;
		sbTxt = new StringBuilder();
		oP = "";
		oPkName = "";
		oPkNCnt = 0;// how many times does openParaConbtractName occur
		oPkNthisCnt = 0;// how many times does openParaConbtractName occur preceded by this
		GetContracts gk = new GetContracts();

		String type = nlp.getAllMatchedGroups(txt.substring(0, 1000), Pattern.compile("(?<=\"type).*?(?=,?[\r\n])"))
				.get(0).replace("/A", "").replaceAll("[:\"]+", "").trim();
		List<Integer> listDisclosure = nlp.getAllIndexEndLocations(type, gk.patternsFormTypesGenerallyNotToParse);
		boolean parse = true;
		if (nlp.getAllMatchedGroups(txt.substring(0, 1000), Pattern.compile("(?<=score\" ?:)[\\d]+")).size() > 0) {
			origScor = Integer.parseInt(
					nlp.getAllMatchedGroups(txt.substring(0, 1000), Pattern.compile("(?<=score\" ?:)[\\d]+")).get(0));
		}
//		System.out.println("txtsnip=" + txt.substring(0, 1000));
		if (nlp.getAllMatchedGroups(txt.substring(0, 1000),
				Pattern.compile("(?sm)(?<=contractNameAlgo).*?(?=,?[\r\n])")).size() > 0) {
			contractNameAlgo = nlp
					.getAllMatchedGroups(txt.substring(0, 1000),
							Pattern.compile("(?sm)(?<=contractNameAlgo).*?(?=,?[\r\n])"))
					.get(0).replaceAll("[\":]", "").trim();
		}
		if (nlp.getAllMatchedGroups(txt.substring(0, Math.min(1000, txt.length())),
				Pattern.compile("(?<=openParaContractName).*?(?=,?[\r\n])")).size() > 0) {
			openParaContractName = nlp
					.getAllMatchedGroups(txt.substring(0, Math.min(1000, txt.length())),
							Pattern.compile("(?<=openParaContractName).*?(?=,?[\r\n])"))
					.get(0).replaceAll("[\":]", "").trim();
		}

		// if type is not the kind to parse -- such as 485 and score is low PR klago len
		// is long
		String governLaw = "";
		governLaw = GoLaw.getGoverningLaw(origTxt);
		governLaw = governLaw.replaceAll("governing law not found", "");
		if (nlp.getAllMatchedGroups(origTxt.substring(0, origTxt.indexOf("gLaw") + 100),
				Pattern.compile("(?ism)(?<=gLaw\" ?: ?\").*?(?=\")")).size() > 0
				&& (nlp.getAllMatchedGroups(origTxt.substring(0, origTxt.indexOf("gLaw") + 100),
						Pattern.compile("(?ism)(?<=gLaw\" ?: ?\").*?(?=\")")).get(0).length() < 4 ||

						nlp.getAllMatchedGroups("(?i)" + origTxt.substring(0, origTxt.indexOf("gLaw") + 100),
								Pattern.compile("governing law not found")).size() > 0)) {

			origTxt = origTxt.replaceAll("(,? ?gLaw\" ?: ?\")(.*?)(\")", ",$1" + governLaw + "$3");
			origTxt = origTxt.replaceAll(",? ?\"txt\" ?:", ",\"gLaw\" ?:\" ?" + governLaw + "\" ?\r\n,\"txt\" : ");
//			System.out.println("governLaw===" + governLaw);
			txt = origTxt;
			// add to text!
		}
		String gLaw = "";
		if (nlp.getAllMatchedGroups(origTxt.substring(0, origTxt.indexOf("gLaw") + 100),
				Pattern.compile("(?ism)(?<=gLaw\" ?: ?\").*?(?=\")")).size() > 0
				&& nlp.getAllMatchedGroups(origTxt.substring(0, origTxt.indexOf("gLaw") + 100),
						Pattern.compile("(?ism)(?<=gLaw\" ?: ?\").*?(?=\")")).get(0).length() > 3) {
			gLaw = nlp.getAllMatchedGroups(origTxt.substring(0, origTxt.indexOf("gLaw") + 100),
					Pattern.compile("(?ism)(?<=gLaw\" ?: ?\").*?(?=\")")).get(0);
			origTxt = origTxt.replaceAll(",? ?\"txt\" ?: ?\"", ",\"gLaw\" : \"" + gLaw + "\"\r\n,\"txt\" : ");
		}

		origTxt = origTxt.replaceAll(",\r\n,", ",\r\n");

//		System.out.println("gLaw===" + gLaw);
		List<String> listDoc = nlp.getAllMatchedGroups(txt, Pattern.compile("(?sm)\\{.*?\\}"));

		if (listDisclosure.size() > 0 && (origScor < 5 || contractNameAlgo.length() > 40))
			parse = false;
		if (!parse) {
			if (nlp.getAllMatchedGroups(listDoc.get(0),
					Pattern.compile("(?sm)(?<=openingParagraph\" ?: ?\").*?(?=,?[\r\n])")).size() > 0) {
				oP = nlp.getAllMatchedGroups(listDoc.get(0),
						Pattern.compile("(?sm)(?<=openingParagraph\" ?: ?\").*?(?=,?[\r\n])")).get(0);
			}
			type = "";
//			System.out.println("listDoc.get0=" + listDoc.get(0));
			if (nlp.getAllMatchedGroups(listDoc.get(0),
					Pattern.compile("(?<=contractLongName\" ?: ?\").*?(?=,?[\r\n])")).size() > 0) {
				contractLongName = nlp.getAllMatchedGroups(listDoc.get(0),
						Pattern.compile("(?<=contractLongName\" ?: ?\").*?(?=,?[\r\n])")).get(0);
//				System.out.println("contractLongName=x=" + contractLongName);
			}

			if (governLaw.length() > 3 && (nlp
					.getAllIndexEndLocations(contractNameAlgo + " " + contractLongName,
							patternContractNameInContractLongNameAllCaps)
					.size() > 0
					|| nlp.getAllIndexEndLocations(contractNameAlgo + " " + contractLongName,
							Pattern.compile("(?ism)contract|indenture|agreement|lease|declaration of trust|guarant"))
							.size() > 0
					|| (oP.length() > 30 && nlp.getAllMatchedGroups(oP.substring(0, 30), Pattern.compile("(?ism)(" + "("
							+ "POWER OF ATTORNEY|CONFIRMATION|COMMITMENT LETTER|LETTER AGREEMENT|POOLING AND SERVICING|DEBENTURE|DEED OF TRUST|(SUPPLEMENTAL )?INDENTURE|(AMENDED AND RESTATED )?(AGREEMENT AND )?"
							+ "PLAN OF (MERGER|ACQUISITION|STOCK PURCHASE)"
							+ "|AGREEMENT TO MERGE AND PLAN OF REORGANIZATION|(AMENDED AND RESTATED |SUPPLEMENTAL )(INDENTURE)|GUARANTEE|GUARANTY|[A-Z]?!LEASE"
							+ "|(SUBORDINATED|JUNIOR|PRMISSORY|SENIOR)? ?NOTE|WARRANT|CONTRACT" + "" + "|AGREEMENT))"))
							.size() > 0))

			) {

				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
				pw.close();
				System.out.println("1. do parse=" + "\r\norigScor=" + origScor + "\r\n contractLongName="
						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
				return;
			}

			else {
				pw = new PrintWriter(new File(parseTheseNotFolder + file.getName()));
				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
				pw.close();
				file.delete();
				System.out.println("1. do not parse=" + "\r\norigScor=" + origScor + "\r\n contractLongName="
						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
				return;
			}
		}

		for (int i = 0; i < 1; i++) {
			// ck doc0
			opContainsMeans = false;
			score = 0;
			origScor = 0;
			oPkName = "";
			openParaContractName = "";
			openParaContractNameReplaced = "";
//			contractNameAlgo = "";
			contractLongName = "";
			fnlKn = "";
			if (nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("(?<=score?\" ?:)[\\d]+")).size() > 0) {
				System.out.println("listDoc=====" + listDoc.get(i));
				origScor = Integer.parseInt(
						nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("(?<=score?\" ?:)[\\d]+")).get(0));
			}

			if (nlp.getAllMatchedGroups(listDoc.get(0), Pattern.compile("(?<=openParaContractName\" ?: ?\").*?(?=\")"))
					.size() > 0) {
				openParaContractName = nlp.getAllMatchedGroups(listDoc.get(0),
						Pattern.compile("(?<=openParaContractName\" ?: ?\").*?(?=\")")).get(0);
			}

			if (nlp.getAllMatchedGroups(listDoc.get(0), Pattern.compile("(?<=contractNameAlgo\" ?: ?\").*?(?=\")"))
					.size() > 0) {
				contractNameAlgo = nlp
						.getAllMatchedGroups(listDoc.get(0), Pattern.compile("(?<=contractNameAlgo\" ?: ?\").*?(?=\")"))
						.get(0);
			}

			if (nlp.getAllMatchedGroups(listDoc.get(0),
					Pattern.compile("(?<=contractLongName=?\" ?: ?\").*?(?=\",? ?[\r\n]{1})")).size() > 0) {
				contractLongName = nlp.getAllMatchedGroups(listDoc.get(i),
						Pattern.compile("(?<=contractLongName=?\" ?: ?\").*?(?=\",? ?[\r\n]{1})")).get(0);
				// PA: should it be (?sm) matches all lines
			}

			openParaContractNameReplaced = openParaContractName
					.replaceAll("(?sm)" + patternNotContractName.toString(), "").trim();
			System.out.println("1. origScor=" + origScor + " orig contractNameAlgo=" + contractNameAlgo
					+ " openParaContractName==" + openParaContractName + " openParaContractNameReplaced=|"
					+ openParaContractNameReplaced + "| contractLongName=" + contractLongName + "\r\n");

			if ((nlp.getAllMatchedGroups(listDoc.get(i),
					Pattern.compile("\"gLaw\" ?: ?\".{1,2}governing law not found")).size() > 0
					|| nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("\"gLaw\" ?: ?\"\'?\'?\"")).size() > 0)

					&& nlp.getAllMatchedGroups(contractNameAlgo, Pattern.compile("(?ism)^insuring agreement"))
							.size() == 0
					&& nlp.getAllMatchedGroups(contractLongName,
							Pattern.compile("(?ism)^EX|contract|indenture|agreement|lease|declaration of trust"))
							.size() == 0) {

				pw = new PrintWriter(new File(parseTheseNotFolder + file.getName()));
				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
				pw.close();
				System.out.println("2. do not parse K =" + "\r\norigScor=" + origScor + "\r\ncontractLongName="
						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
				return;
			}

			if (origScor > 2 && contractNameAlgo.replaceAll(
					"(?ism)(((this )?(agreement)|" + patternNotContractName.toString() + ")|[^a-zA-Z]{0,10})+", "")
					.length() < 50 &&

					nlp.getAllIndexEndLocations(contractLongName.replaceAll("[^A-Za-z]", "").trim(),
							Pattern.compile("(?ism)" + contractNameAlgo.replaceAll("[^A-Za-z]", "").trim()))
							.size() == 0) {
				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
				pw.close();
				System.out.println("3. OKAY1. No chg =" + "\r\norigScor=" + origScor + "\r\ncontractLongName="
						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
				return;

			}

//			System.out.println("listDoc==" + listDoc.get(i));
//			System.out.println(
//					"txt===" + nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("(?sm)(?<=\"txt\":).*?\r\n"))
//							.get(0).replaceAll("\r\n|,$|\"$|^\"", "") + "\r\n");
//			"openingParagraph":"^
			if (nlp.getAllMatchedGroups(listDoc.get(i),
					Pattern.compile("(?<=openingParagraph\" ?: ?\").*?(?=,?[\r\n])")).size() > 0) {
				oP = nlp.getAllMatchedGroups(listDoc.get(i),
						Pattern.compile("(?sm)(?<=openingParagraph\" ?: ?\").*?(?=,?[\r\n])")).get(0);
				oP = oP.substring(0, Math.min(oP.length(), 80)).replaceAll("-", " ");
//				System.out.println("oP===="
//						+ nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("(?sm)(openingParagraph).*?[\r\n]"))
//								.get(0));
//				System.out.println("snippet oP==" + oP + "\r\n");
				if (oP.contains(" means? "))
					opContainsMeans = true;
//				System.out.println("listDoc.get==" + listDoc.get(i));

			}

//			if (nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("(?<=openingParagraph\" ?: ?\").*?(?=[\r\n])"))
//					.size() == 0) {
//				System.out.println("no openinigParagraph+\r\n");
//			}

			if (origScor < 3
//					|| contractName.replaceAll("(?ism)" + patternNotContractName.toString(), "").replaceAll("[ ]+", "")
//							.length() < 3
					|| contractNameAlgo.replaceAll(
							"(?ism)(((this )?(agreement)|" + patternNotContractName.toString() + ")|[^a-zA-Z]{0,10})+",
							"").length() > 60) {

				System.out.println("NEED TO FIX");
				needToFix = true;
			}

			if (contractNameAlgo.replaceAll("(?ism)this| and | of ", "").length() - patternContractNameStopWords
					.matcher(contractNameAlgo.toLowerCase().replaceAll("(?ism)this| and | of ", "")).replaceAll("")
					.length() > 5) {
				needToFix = true;
			}

			if (!needToFix) {
				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
				pw.close();
				System.out.println("4. OKAY2. No chg =" + "\r\norigScor=" + origScor + "\r\ncontractLongName="
						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
				return;
			}
		}

		for (int i = 1; i < listDoc.size(); i++) {
			if (nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("\"typ\" ?: ?(\\[ ?1, ?3 ?\\]|1)"))
					.size() == 0) {
				// only fetch typ para
//				System.out.println("continue...");
				continue;
			}
			if (nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("(?sm)(?<=\"txt\" ?: ?\").*?\"")).size() == 0) {
				continue;
			}
//			System.out.println("xxxxxxxxxxxxxx"+listDoc.get(i));
			sbTxt.append(nlp.getAllMatchedGroups(listDoc.get(i), Pattern.compile("(?sm)(?<=\"txt\" ?: ?\").*?\""))
					.get(0).replaceAll("[\r\n]+|,$|\"$|^\"", "").replaceAll("(\\\\r\\\\n|\\\\n|\\\\r)+", "\r\n")
					+ "\r\n");// ,"txt":"
		}

		if (openParaContractNameReplaced.length() > 0 && nlp.getAllMatchedGroups(oP, patternContractName).size() > 0) {
			System.out.println("openParaContractName???=="
					+ openParaContractName.replaceAll("(?sm)" + patternNotContractName.toString(), ""));
			System.out.println("found opening openParaContractName=" + nlp
					.getAllMatchedGroups(sbTxt.toString(), Pattern.compile("(?ism)" + openParaContractName)).size());
			System.out.println("but here is oP kName ptrn==" + nlp.getAllMatchedGroups(oP, patternContractName).get(0));
			oPkNthisCnt = nlp
					.getAllMatchedGroups(sbTxt.toString(), Pattern.compile("(?ism)this " + openParaContractName))
					.size();

			oPkNCnt = nlp.getAllMatchedGroups(sbTxt.toString(), Pattern.compile("(?ism)" + openParaContractName))
					.size();
			System.out.println("oPkNthisCnt==" + oPkNthisCnt + "");
			System.out.println("oPkNCnt==" + oPkNCnt + "\r\n");

		}

		if (nlp.getAllIndexEndLocations(oP, Pattern.compile("(^\\^This|^\\^this|^\\^THIS) "
				+ openParaContractName.replaceAll("\\^", "") + "(?ism)(Agreement|indenture)?")).size() > 0

		) {
			score = 3;
			openParaContractName = nlp
					.getAllMatchedGroups(oP, Pattern.compile(
							"(^\\^This|^\\^this|^\\^THIS) " + openParaContractName + "(?ism)( Agreement| indenture)?"))
					.get(0).replaceAll("(?ism)\\^this ", "");
			System.out.println("IT IS openParaContractName==========" + openParaContractName);

		}

		if (oPkNthisCnt > 3 || oPkNCnt > 8) {
			score = 2;
			if (oPkNthisCnt > 3)
				score = 3;
			fnlKn = openParaContractName;
			System.out.println(
					"fnlKn=" + fnlKn + " scor=" + score + " oPkNthisCnt==" + oPkNthisCnt + " oPkNCnt==" + oPkNCnt);
		}

		String txtSnip = sbTxt.toString().substring(0, Math.min(15000, sbTxt.toString().length()));

		if (fnlKn.length() == 0) {
			listAry = contractNameFinderCount(txtSnip);
			// using contract name method b/c nothing found in openParaContractName field.
//			NLP.printListOfStringArray("listAry===x==", listAry);

			sb.append("\r\n\r\n" + file.getName() + " ");
			// System.out.println("1111. oP="+oP);

			if (nlp.getAllMatchedGroups(oP, patternContractName).size() > 0
					|| nlp.getAllMatchedGroups(oP, Pattern.compile("^\\^(INDENTURE|Indenture)")).size() > 0) {
				// ckg if there's a kname in oP

				if (nlp.getAllMatchedGroups(oP, patternContractName).size() > 0)
					oPkName = nlp.getAllMatchedGroups(oP, patternContractName).get(0).replaceAll("\\^", "");

				if (nlp.getAllMatchedGroups(oP, Pattern.compile("^\\^(INDENTURE|Indenture)")).size() > 0)
					oPkName = nlp.getAllMatchedGroups(oP, Pattern.compile("^\\^(INDENTURE|Indenture)")).get(0)
							.replaceAll("\\^", "");

//				System.out.println("\r\nkName in oP=" + oPkName + "\r\n");

			}

			for (int c = 0; c < listAry.size(); c++) {
//				System.out.println("listAry=" + Arrays.toString(listAry.get(c)) + " scor=" + score);
				sb.append(Arrays.toString(listAry.get(c)) + "\r\n");
				if (!listAry.get(c)[0].toLowerCase().contains("this") && oPkName.length() > 0 && !opContainsMeans
						&& (listAry.get(c)[0].replaceAll("(?ism)(((this )?(agreement)|"
								+ patternNotContractName.toString() + ")|[^a-zA-Z]{0,10})+", "").length() == 0
								|| score < 3)) {

//					System.out.println("picked oP=" + oPkName + " score=" + 3 + "\r\n");
					fnlKn = oPkName;
					score = 3;
//				System.out.println("but ary is" + Arrays.toString(listAry.get(c)));
				} else {
//					contractName = listAry.get(c)[0] + " score==" + listAry.get(c)[4];
//					System.out.println("10. oPkN=" + oPkName);
					score = Integer.parseInt(listAry.get(c)[4]);
					if (oPkName.contains(listAry.get(c)[0])) {
						if (score < 3)
							score = 4;
					} else {
						score = 3;
					}
					fnlKn = listAry.get(c)[0];
//					System.out.println("listary fnlKn=" + fnlKn + " scor==" + score);
				}

//				if (contractNameAlgo
//						.replaceAll("(?ism)(((this )?(agreement)|" + patternNotContractName.toString()
//								+ ")|[^a-zA-Z]{0,10})+", "")
//						.length() > 60
//						&& (fnlKn.length() < 3 || fnlKn.replaceAll("(?ism)(((this )?(agreement)|"
//								+ patternNotContractName.toString() + ")|[^a-zA-Z]{0,10})+", "").length() > 60)) {
//					System.out.println("1=x=too long contractNameAlgo=" + contractNameAlgo);
//					System.out.println("1=x=contractLongName==" + contractLongName);
//					System.out.println("1=x=fnlKn==" + fnlKn);
//				}

				if (score < 3
						&& fnlKn.replaceAll("(?ism)(((this )?(agreement)|" + patternNotContractName.toString()
								+ ")|[^a-zA-Z]{0,10})+", "").length() > 7

						&& nlp.getAllIndexStartLocations(txtSnip, Pattern.compile("(?ism)" + fnlKn)).size() > 5) {
					score = 2;
				}

				if (fnlKn.length() == 0) {
					fnlKn = openParaContractName;
					score = 1;
//					System.out.println("fnl kn is openParaContractName=" + fnlKn + " scor=" + score);
				}
			}
//			System.out.println("2=x=contractNameAlgo=" + contractNameAlgo);
//			System.out.println("2=x=contractLongName==" + contractLongName);
//			System.out.println("filename====" + file.getName() + " oP==" + oP);
//			System.out.println("2=x=          fnlKn==" + fnlKn);
//			System.out.println("2=x=          scor==" + score);
		}

		fnlKn = fnlKn.replaceAll("(?ism)(^this )", "");

		if (nlp.getAllIndexEndLocations(fnlKn.replaceAll("[^A-Za-z]", "").trim(),
				Pattern.compile("(?ism)" + contractLongName.replaceAll("[^A-Za-z]", "").trim())).size() > 0) {
			if (fnlKn.replaceAll(patternNotContractName.toString(), "").length() > 4) {
				score = score + 2;
//				System.out.println("1******fnlKn is contractLongName*****==" + fnlKn + " score==" + score + "\r\n");
			}
		}

		if (nlp.getAllIndexEndLocations(contractLongName.replaceAll("[^A-Za-z]", "").trim(),
				Pattern.compile("(?ism)" + fnlKn.replaceAll("[^A-Za-z]", "").trim())).size() > 0) {
			if (contractLongName.replaceAll(patternNotContractName.toString(), "").length() > 4) {
				score = score + 2;
				fnlKn = contractLongName;
//				System.out.println("2******fnlKn is contractLongName*****==" + fnlKn + " score==" + score + "\r\n");
			}
		}

		if (nlp.getAllIndexEndLocations(openParaContractName.replaceAll("[^A-Za-z]", "").trim(),
				Pattern.compile("(?ism)" + fnlKn.replaceAll("[^A-Za-z]", "").trim())).size() > 0) {
			if (openParaContractName.replaceAll(patternNotContractName.toString(), "").length() > 4) {
				score = 3;
				fnlKn = openParaContractName;
//				System.out.println("3******fnlKn is contractLongName*****==" + fnlKn + " score==" + score + "\r\n");
			}
		}

		if (fnlKn.replaceAll("(?ism)( |agreement|contract|amended|restated|and )+", "").length() < 4
				|| fnlKn.replaceAll("([^A-Za-z]+)", "").length() < 4
				|| nlp.getAllIndexEndLocations(fnlKn,
						Pattern.compile("(?ism)(DEBENTURE|DEED|SUPPLEMENTAL|MERGER|PLAN OF|INDENTURE"
								+ "|DECLARATION OF TRUST|AGREEMENT|GUARANTEE|GUARANTY|LEASE|NOTE|WARRANT|CONTRACT|VOTING|CONTRACT)"))
						.size() == 0) {
//			System.out.println("fnlKn was=" + fnlKn + " now blank fnlKn=" + fnlKn);
			fnlKn = "";
			score = 0;
		}
		if (fnlKn.replaceAll("(?ism)this| and | of ", "").length() - patternContractNameStopWords
				.matcher(fnlKn.toLowerCase().replaceAll("(?ism)this| and | of ", "")).replaceAll("").length() > 5) {
//			System.out.println("2222");
			fnlKn = "";
			score = 0;
		}

		if (fnlKn.length() > 3 && score < 2) {
			score = 3;
		}

		if (fnlKn.length() == 0 && nlp.getAllIndexStartLocations(oP, Pattern.compile("(?ism)\\^Indenture")).size() > 0
				&& !opContainsMeans) {
//			System.out.println("it is indenture");
			fnlKn = "INDENTURE";
			score = 3;
		}

		if (fnlKn.length() > 0) {
			if (fnlKn.length() < 6 && openParaContractName.length() > 6) {
				score = 3;
				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\" : " + score + "$1");
				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"contractNameAlgo\" : \""
						+ openParaContractName.replaceAll("(?ism)\\^?THIS", "") + "\"$1");

				pw = new PrintWriter(new File(parseTheseFolder + "chgd" + "_" + file.getName()));
				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
				pw.close();
				fnlKn = openParaContractName;
				System.out.println("5a. new kAlgo=" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
						+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
				return;
			} else {
				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\" : " + score + "$1");
				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
						"\r\n,\"contractNameAlgo\":\"" + fnlKn + "\"$1");
//				System.out.println("parseTheseFolder===" + parseTheseFolder);
				pw = new PrintWriter(new File(parseTheseFolder + "chgd" + "_" + file.getName()));
				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
				pw.close();
				System.out.println("5b. new kAlgo=" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
						+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
				return;
			}

		}

		if (fnlKn.trim().length() == 0 && contractNameAlgo.trim().length() == 0
				&& (nlp.getAllMatchedGroups(contractLongName, patternContractNameInContractLongNameAllCaps).size() == 0
						|| contractLongName.replaceAll(patternNotContractName.toString(), "")
								.replaceAll("[^A-Za-z ]+", "").length() < 4)) {

			if (openParaContractName.replaceAll(patternNotContractName.toString(), "")
					.replaceAll("(?ism)\\^?This ?", "").length() > 3) {

				score = 3;
				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\" : " + score + "$1");
				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
						"\r\n,\"contractNameAlgo\" : \"" + openParaContractName + "\"$1");

				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
				pw.close();

//				System.out.println(
//						"nlp.getAllMatchedGroups(contractLongName, patternContractNameInContractLongNameAllCaps).size()="
//								+ nlp.getAllMatchedGroups(contractLongName, patternContractNameInContractLongNameAllCaps)
//										.get(0));
//				System.out.println("6d. NO kAlgo=" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
//						+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
//						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
//						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
				return;
			}
//			System.out.println("txt600=" + sbTxt.toString().substring(0, 600));
			if (nlp.getAllIndexEndLocations(sbTxt.substring(0, Math.min(sbTxt.toString().length(), 600)),
					Pattern.compile("(?ism)Underwriting Agreement")).size() > 0) {

				score = 3;
				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\" : " + score + "$1");
				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
						"\r\n,\"contractNameAlgo\" : \"Underwriting Agreement\"$1");

				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
				pw.close();
				return;
			} else {
//				System.out.println(
//						"ex.siz==" + nlp.getAllMatchedGroups(contractLongName, Pattern.compile("(?ism)^EX")).size());
//				System.out.println("glaw.len==" + governLaw.length());
				if (

				(nlp.getAllMatchedGroups(contractLongName, Pattern.compile("(?ism)(" + "("
						+ "^EX|POWER OF ATTORNEY|CONFIRMATION|COMMITMENT LETTER|LETTER AGREEMENT|POOLING AND SERVICING|DEBENTURE"
						+ "|DEED OF TRUST|(SUPPLEMENTAL )?INDENTURE|(AMENDED AND RESTATED )?(AGREEMENT AND )?"
						+ "PLAN OF (MERGER|ACQUISITION|STOCK PURCHASE)"
						+ "|AGREEMENT TO MERGE AND PLAN OF REORGANIZATION|(AMENDED AND RESTATED |SUPPLEMENTAL )"
						+ "(INDENTURE)|GUARANTEE|GUARANTY|[A-Z]?!LEASE"
						+ "|(SUBORDINATED|JUNIOR|PRMISSORY|SENIOR)? ?NOTE|WARRANT|CONTRACT" + "" + "|AGREEMENT))"))
						.size() > 0

						||

						nlp.getAllMatchedGroups(type, Pattern.compile("(?ism)(" + "("
								+ "^EX|POWER OF ATTORNEY|CONFIRMATION|COMMITMENT LETTER|LETTER AGREEMENT|POOLING AND SERVICING|DEBENTURE"
								+ "|DEED OF TRUST|(SUPPLEMENTAL )?INDENTURE|(AMENDED AND RESTATED )?(AGREEMENT AND )?"
								+ "PLAN OF (MERGER|ACQUISITION|STOCK PURCHASE)"
								+ "|AGREEMENT TO MERGE AND PLAN OF REORGANIZATION|(AMENDED AND RESTATED |SUPPLEMENTAL )"
								+ "(INDENTURE)|GUARANTEE|GUARANTY|[A-Z]?!LEASE"
								+ "|(SUBORDINATED|JUNIOR|PRMISSORY|SENIOR)? ?NOTE|WARRANT|CONTRACT" + ""
								+ "|AGREEMENT))")).size() > 0

				) && (governLaw.length() > 3
						|| nlp.getAllMatchedGroups(origTxt.substring(0, origTxt.indexOf("gLaw") + 100),
								Pattern.compile("(?ism)(?<=gLaw\" ?: ?\").*?(?=\")")).get(0).length() > 4)
						&& (nlp.getAllMatchedGroups(oP.substring(0, Math.min(50, oP.length())),
								Pattern.compile("(?ism)(" + "("
										+ "POWER OF ATTORNEY|CONFIRMATION|COMMITMENT LETTER|LETTER AGREEMENT|POOLING AND SERVICING"
										+ "|DEBENTURE|DEED OF TRUST|(SUPPLEMENTAL )?INDENTURE|(AMENDED AND RESTATED )?(AGREEMENT AND )?"
										+ "PLAN OF (MERGER|ACQUISITION|STOCK PURCHASE)"
										+ "|AGREEMENT TO MERGE AND PLAN OF REORGANIZATION|(AMENDED AND RESTATED |SUPPLEMENTAL )(INDENTURE)|GUARANTEE|GUARANTY|[A-Z]?!LEASE"
										+ "|(SUBORDINATED|JUNIOR|PRMISSORY|SENIOR)? ?NOTE|WARRANT|CONTRACT|AGREEM))"))
								.size() > 0 ||

								nlp.getAllMatchedGroups(
										sbTxt.toString().substring(0, Math.min(500, sbTxt.toString().length())),
										Pattern.compile("(?ism)(" + "("
												+ "POWER OF ATTORNEY|CONFIRMATION|COMMITMENT LETTER|LETTER AGREEMENT|POOLING AND SERVICING"
												+ "|DEBENTURE|DEED OF TRUST|(SUPPLEMENTAL )?INDENTURE|(AMENDED AND RESTATED )?(AGREEMENT AND )?"
												+ "PLAN OF (MERGER|ACQUISITION|STOCK PURCHASE)"
												+ "|AGREEMENT TO MERGE AND PLAN OF REORGANIZATION|(AMENDED AND RESTATED |SUPPLEMENTAL )(INDENTURE)|GUARANTEE|GUARANTY|[A-Z]?!LEASE"
												+ "|(SUBORDINATED|JUNIOR|PRMISSORY|SENIOR)? ?NOTE|WARRANT|CONTRACT|AGREEM))"))
										.size() > 0)

				) {
//					System.out.println("has glaw & ^EX in contractLongName...............");
					pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
					pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
					pw.close();
//					System.out.println("6a. parse NO kAlgo=" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
//							+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
//							+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
//							+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
					return;
				} else {
					pw = new PrintWriter(new File(parseTheseNotFolder + file.getName()));
					pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
					pw.close();
//					System.out.println(oP);
//					System.out.println("6b. not parse NO kAlgo=" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
//							+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
//							+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
//							+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\nGOVERNlAW=" + governLaw + " oP="
//							+ oP.substring(0, Math.min(50, oP.length())));
					return;
				}
			}
		}

		if (origScor < 3) {

//System.out.println("openParaContractName="+openParaContractName);
//System.out.println("contractLongName="+contractLongName+"||");
			if (

//					nlp.getAllMatchedGroups(openParaContractName, Pattern.compile("(?ism)" + contractNameAlgo)).size() > 0
//					||
			nlp.getAllMatchedGroups(openParaContractName, Pattern.compile("(?ism)" + contractLongName)).size() > 0
					&& nlp.getAllMatchedGroups(openParaContractName, Pattern.compile("(?ism)" + contractLongName))
							.get(0).replaceAll(patternNotContractName.toString(), "").length() > 3

			) {
//				System.out.println("it is opkn");
				fnlKn = openParaContractName;
				score = 3;

				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\":" + score + "$1");
				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
						"\r\n,\"contractNameAlgo\":\"" + fnlKn + "\"$1");

				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
				pw.close();
//				
//				System.out.println("7a. chg score to 3" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
//						+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
//						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
//						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
				return;
			}

			if (
//					nlp.getAllMatchedGroups(contractLongName, Pattern.compile("(?ism)" + contractNameAlgo)).size() > 0
//					||
			nlp.getAllMatchedGroups(contractLongName, Pattern.compile("(?ism)" + openParaContractName)).size() > 0
					&& nlp.getAllMatchedGroups(contractLongName, Pattern.compile("(?ism)" + openParaContractName))
							.get(0).replaceAll(patternNotContractName.toString(), "").length() > 3) {
//				System.out.println("1 it is klongname");
				fnlKn = contractLongName;
				score = 3;

				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\" : " + score + "$1");
				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
						"\r\n,\"contractNameAlgo\" : \"" + fnlKn + "\"$1");

				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
				pw.close();
//				
				System.out.println("7b. chg score to 3" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
						+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
				return;
			}

			if (
//					nlp.getAllMatchedGroups(contractLongName, Pattern.compile("(?ism)" + contractNameAlgo)).size() > 0
//					||
			nlp.getAllMatchedGroups(openParaContractName.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", ""),
					Pattern.compile("(?ism)" + contractNameAlgo.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", "")))
					.size() > 0
					&& nlp.getAllMatchedGroups(
							openParaContractName.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", ""),
							Pattern.compile(
									"(?ism)" + contractNameAlgo.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", "")))
							.get(0).replaceAll(patternNotContractName.toString(), "").length() > 3) {
//				System.out.println("1 kalgo in oPkn");
				fnlKn = contractNameAlgo;
				score = 3;

				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
//				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\":" + score + "$1");
//				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
//						"\r\n,\"contractNameAlgo\":\"" + fnlKn + "\"$1");

				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
				pw.close();
//				
				System.out.println("7e. chg score to 3" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
						+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
				return;
			}

			if (
//					nlp.getAllMatchedGroups(contractLongName, Pattern.compile("(?ism)" + contractNameAlgo)).size() > 0
//					||
			nlp.getAllMatchedGroups(contractLongName.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", ""),
					Pattern.compile("(?ism)" + contractNameAlgo.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", "")))
					.size() > 0
					&& nlp.getAllMatchedGroups(contractLongName.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", ""),
							Pattern.compile(
									"(?ism)" + contractNameAlgo.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", "")))
							.get(0).replaceAll(patternNotContractName.toString(), "").length() > 3

			) {
//				System.out.println("2 it is klongname");
				fnlKn = contractLongName;
				score = 3;
				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
				origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\" : " + score + "$1");
				origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
						"\r\n,\"contractNameAlgo\":\"" + fnlKn + "\"$1");

				pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
				pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
				pw.close();
//				
				System.out.println("7c. chg score to 3" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
						+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
						+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
						+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
				return;
			}
		}

		if (contractNameAlgo.length() == 0 && fnlKn.length() == 0
				&& (nlp.getAllMatchedGroups(contractLongName.replaceAll("[^a-zA-Z ]+", "").replaceAll("[ ]+", ""),
						patternContractName).size() > 0
						|| contractLongName.replaceAll(patternNotContractName.toString(), "")
								.replaceAll("[^A-Za-z ]+", "").length() > 3)) {

			fnlKn = contractLongName;
			score = 3;
			origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
			origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
			origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\":" + score + "$1");
			origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
					"\r\n,\"contractNameAlgo\":\"" + fnlKn + "\"$1");

			pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
			pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
			pw.close();
//			
			System.out.println("7d. chg score to 3" + "\r\norigScor=" + origScor + " fnlKn=" + fnlKn
					+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
					+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
					+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
			return;
		}

		if (openParaContractName.length() > 0 && fnlKn.length() == 0
				&& nlp.getAllMatchedGroups("\\^THIS " + openParaContractName.toUpperCase() + "\r\n",
						patternContractName).size() > 0
				&& openParaContractName.replaceAll(patternNotContractName.toString(), "").replaceAll("[^A-Za-z ]+", "")
						.length() > 3
				&& patternContractNameStopWords.matcher(contractNameAlgo.toLowerCase()).replaceAll("")
						.length() < contractNameAlgo.length() - 5

		) {

			fnlKn = openParaContractName;
			score = 3;
			origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"scor.*?\r\n", "\r\n");
			origTxt = origTxt.replaceAll("(?ism)\r\n.{0,5}\"contractNameAlgo.*?\r\n", "\r\n");
			origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")", "\r\n,\"scor\":" + score + "$1");
			origTxt = origTxt.replaceAll("(?ism)(\r\n.{0,5}\"fDate\")",
					"\r\n,\"contractNameAlgo\":\"" + fnlKn + "\"$1");

			pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
			pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
			pw.close();
//			
			System.out.println("7E. chg score to 3" + "\r\norigScor=" + origScor + " fnlKn=" + fnlKn
					+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
					+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
					+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
			return;
		}

		else {
			pw = new PrintWriter(new File(parseTheseFolder + file.getName()));
			pw.append(origTxt.replaceAll("\"\r\n\"", "\"\r\n,\""));
			pw.close();
			System.out.println("8. NO CHG=" + "\r\norigScor=" + origScor + "\r\nfnlKn=" + fnlKn
					+ "\r\nnew contractNameAlgo=" + fnlKn + "\r\nscore=" + score + "\r\ncontractLongName="
					+ contractLongName + "\r\nopenParaContractName=" + openParaContractName
					+ "\r\norig contractNameAlgo=" + contractNameAlgo + "\r\n");
			return;
		}

	}

	public static void call_contractNameFinder(File files, int sY5, int sQ5, String parentGroup)
			throws IOException, SQLException {
		NLP nlp = new NLP();
		File[] listOfFiles = files.listFiles();
		ContractFinder.parseTheseFolder = files.getAbsolutePath();
//		"e:/solrJsons/parseThese/" + sY5 + "/QTR" + sQ5 + "/" + parentGroup + "/";
		ContractFinder.parseTheseNotFolder = GetContracts.solrDocsFolder + "/parseTheseNot/";
		System.out.println("parseTheseFolder=" + parseTheseFolder);
		System.out.println("parseTheseNotFolder=" + parseTheseNotFolder);
		Utils.createFoldersIfReqd(ContractFinder.parseTheseFolder);
		Utils.createFoldersIfReqd(ContractFinder.parseTheseNotFolder);
		System.out.println("files at==" + files.getAbsolutePath());
		for (int n = 0; n < listOfFiles.length; n++) {
			System.out.println("filename==" + listOfFiles[n].getName());
			if (nlp.getAllIndexStartLocations(listOfFiles[n].getName(), Pattern.compile("XBRL|XML|JPG|PDF"))
					.size() > 0) {
				listOfFiles[n].delete();
				System.out.println("deleted it");
				continue;
			}
			System.out.println("filename==" + listOfFiles[n].getAbsolutePath());
			ContractFinder.checkContractNameAlgoInSolrJson(listOfFiles[n]);
			// files are saved to : parseTheseFolder/parseTheseNotFolder
		}

	}

	public static void main(String[] args) throws IOException, SolrServerException, SQLException {

		int sY = 2021, eY = sY, sQ = 2, eQ = sQ, sY2 = sY, eY2 = eY, sQ2 = sQ, eQ2 = eQ, sY3 = sY, eY3 = eY, sQ3 = sQ,
				eQ3 = eQ, sY4 = sY, eY4 = eY, sQ4 = sQ, eQ4 = eQ, sY5 = sY, eY5 = eY, sQ5 = sQ, eQ5 = eQ;

		NLP nlp = new NLP();
		solrJsonDataFolder = "E:/solrJsons/";
		Utils.createFoldersIfReqd("c:\\tempTest\\");
		Utils.createFoldersIfReqd("e:/solrJsons/parseThese/");
		Utils.createFoldersIfReqd("e:/solrJsons/parseTheseNot/");
		String parentGroup = "Financial_Contracts";
		File files = new File("E:\\getContracts\\solrIngestion\\solrDocs\\2022\\QTR2\\Financial Contracts");
		File[] listOfFiles = files.listFiles();
		
//		checkContractNameAlgoInSolrJson(new File("c:/temp/kfinder.json"));
		for (int n = 0; n < listOfFiles.length; n++) {
			checkContractNameAlgoInSolrJson(listOfFiles[n]);
		}
//
//		for (; sY5 <= eY5; sY5++) {
//			for (; sQ5 <= eQ5; sQ5++) {
//				System.out.println("qtr=" + sQ5 + " endQtr=" + eQ5 + " yr=" + sY5 + " eY5=" + eY5);
//				files = new File(solrJsonDataFolder + sQ5 + "/QTR" + sQ5 + "/" + parentGroup + "/");
//				// E:\getContracts\solrIngestion\solrDocs\2012\QTR1\Financial_Contracts
//				files = new File("E:\\getContracts\\solrIngestion\\solrDocs\\" + sY5 + "\\QTR" + sQ5
//						+ "\\Financial_Contracts\\");
//				File[] listOfFiles = files.listFiles();
//				parseTheseFolder = "e:/solrJsons/parseThese/" + sY5 + "/QTR" + sQ5 + "/Financial_Contracts/";
//				parseTheseNotFolder = "e:/solrJsons/parseTheseNot/" + sY5 + "/QTR" + sQ5 + "/Financial_Contracts/";
//				// System.out.println("parseTheseFolder=" + parseTheseFolder);
//				// System.out.println("parseTheseNotFolder=" + parseTheseNotFolder);
//				Utils.createFoldersIfReqd(parseTheseFolder);
//				Utils.createFoldersIfReqd(parseTheseNotFolder);
//				for (int n = 0; n < listOfFiles.length; n++) {
//					// System.out.println("filename==" + listOfFiles[n].getName());
//					if (nlp.getAllIndexStartLocations(listOfFiles[n].getName(), Pattern.compile("XBRL|XML|JPG|PDF"))
//							.size() > 0) {
//						listOfFiles[n].delete();
//						System.out.println("deleted it");
//						continue;
//					}
//					System.out.println("filename==" + listOfFiles[n].getAbsolutePath());
//					checkContractNameAlgoInSolrJson(listOfFiles[n]);
//				}
//			}
//			sQ5 = 1;
//		}
//
//		// File f = new File("c:/tempFixes/fix4.json");
//		// checkContractNameAlgoInSolrJson(f);

	}
}
