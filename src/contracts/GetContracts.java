package contracts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import xbrl.FileSystemUtils;
import xbrl.MysqlConnUtils;
import xbrl.NLP;
import xbrl.Utils;

/*
 import org.apache.solr.client.solrj.SolrServerException;
 import org.apache.solr.client.solrj.impl.HttpSolrServer;
 import org.apache.solr.common.SolrInputDocument;
 */

public class GetContracts {

//	public static boolean onlyOneContractType = false;
	public static List<String> list_metadata_all = new ArrayList<String>();
	public static int min_mod_filter_size = 50000;
	public static int max_mod_filter_size = 300000;
	public static String modifiedFilter = "";// ] and FSIZE<" + max_mod_filter_size + " and fsize>" +
												// min_mod_filter_size;
	public static boolean parsingDisclosure = false;
	public static boolean turnOnPrinterWriter = false;
	/*
	 * modified filters out reduces the total pool to parse so I can do it in 2
	 * steps. To get remainder just reverse these conditions. eg
	 * fsize>=max_mod_filter_size and fsize<=min_mod_filter_size
	 */
	public static String modifiedFilterBackFill = " and FSIZE>=" + max_mod_filter_size + " or fsize<="
			+ min_mod_filter_size;

	public static boolean downloadMasterIdx = false;
	public static TreeMap<String, List<String>> map_contract_type = new TreeMap<String, List<String>>();
	public static TreeMap<String, String> mapOfKidsParsed = new TreeMap<String, String>();
	public static TreeMap<String, String> mapOfKidsStripped = new TreeMap<String, String>();
	public Document document;
	public static String secZipFolder = "e:/secZipFiles/";
	public static String masterIdxFolder = "c:/backtest/tableparser/";
	public static String contractsFolder = "c:/LincParser_2nd_final_Command_new_java_files/getContracts/";
	public static String masterIdxMetaFolder = "e:/getContracts/metaCsv/";
	public static String metadataFolder = contractsFolder + "solrIngestion/metadata_new/";
	public static String downloadFolder = contractsFolder + "solrIngestion/downLoaded/";
	public static String strippedFolder = contractsFolder + "solrIngestion/stripped/";
	public static String strippedFolder_no_meta = contractsFolder + "solrIngestion/stripped_no_metadata/";
	public static String solrDocsFolder = contractsFolder + "solrIngestion/solrDocs/";
	public static String solrDocsCleanedFolder = contractsFolder + "solrIngestion/solrDocs_Cleaned/";
	public static String href_data = contractsFolder + "solrIngestion/metadata_href/";
	public static String href_csv = contractsFolder + "solrIngestion/metadata_href_csv/";
	public static String quotedTerms = "";
	public static String quotedTermsTxt = "";
	public static String clientMetaData = "";

//	public static String openingParaFolder = contractsFolder+"solrIngestion/openingPara/";
	public static String governLaw = "";
	public int largeFilePostStrip = 12000000;
	public static String toc = null;

	public int defCnt = 0;
	public int counter = 0;
	public Integer tocStartIdx = null;
	public Integer tocEndIdx = null;
	public Integer exhStartIdx = null;
	public Integer exhEndIdx = null;
	public boolean contractAlgoFoundNameTwice = false;
	public boolean contractAlgoSelectedFirstInstance = false;
	public boolean contractAlgoSelectedAnAllCapsName = false;

	private String solrCore2PushInto = null;
	private String baseSolrUrl = "http://localhost:8983/solr/";

	public List<String[]> listOfDefinedTermsIdxsAndNames = new ArrayList<>();
	public List<String[]> listOfExhibitsIdxsAndNames = new ArrayList<>();
	public TreeMap<Integer, String[]> mapOfAllContractParts = new TreeMap<Integer, String[]>();
	// key=start idx, String[]: [0]=endIdx of last match, [1]=type (e.g.,
	// TOC,Section,Exhibit), [2]=first text match, [3]=last text match

	public Pattern patternBetweenAmong = Pattern.compile("(?ism)(among| between| is made| made as of|entered into)");
	public Pattern patternDated = Pattern.compile(
			"(?ism)(dated|made as of |made this|effective as of|entered into as of|january|february|march|april|may |june|july|august|september|october|november|december|[12]{1}[09]{1}[\\d]{2})");

	public Pattern patternQuotedRole = Pattern.compile("(?ism)(?<=[,\\(]as|the \"?)"
			+ "([A-Z]{1,20}[a-z]+[ -]{0,3})([A-Z]{1,20}[a-z]+[ -]{0,3})?([A-Z]{1,20}[a-z]+[ -]{0,3})?([A-Z]{1,20}[a-z]+[ -]{0,3})"
			+ "?(?=\")");

	public Pattern patternsFormTypesGenerallyNotToParse = Pattern.compile(
			"^(1|1-A|1-A POS|1-A-W|1-E|1-E AD|1-K|1-SA|1-U|1-Z|1-Z-W|10-12B|10-12G|10-C|10-D|10-K|10-K405|10-KSB|10-KT|10-Q|10-QSB|10-QT|10KSB|10KSB40|10KT405|10QSB|10SB12B|10SB12G|11-K|11-KT|12G3-2B|12G32BR|13F-E|13F-HR|13F-NT|13FCONP|144|15-12B|15-12G|15-15D|15F-12B|15F-12G|15F-15D|18-12B|18-K|19B-4E|2-A|2-AF|2-E|20-F|20FR12B|20FR12G|24F-1|24F-2EL|24F-2NT|24F-2TM|25|25-NSE|253G1|253G2|253G3|253G4|3|305B2|34-12H|35-APP|35-CERT|39-304D|39-310B|4|40-17F|40-17F"
//			+ "|40-17G"
					+ "|40-17GCS|40-202A|40-203A|40-205E|40-206A|40-24B2|40-33|40-6B|40-6C|40-8B|40-8F-2|40-8F-A|40-8F-B|40-8F-L|40-8F-M|40-8FC|40-APP|40-F|40-OIP|40-RPT|40FR12B|40FR12G|424A|424B1|424B2|424B3|424B4|424B5|424B7|424B8|424H"
//			+ "|425"
					+ "|485A24E|485A24F|485APOS|485B24E|485B24F|485BPOS|485BXT|485BXTF|486A24E|486APOS|486B24E|486BPOS|486BXT|487|497|497AD|497H2|497J|497K|497K1|497K2|497K3A|497K3B|5|6-K|6B NTC|6B ORDR|8-A12B|8-A12G|8-B12B|8-B12G|8-K12B|8-K12G3|8-K15D5|8-M|8A12BEF|8A12BT|8F-2 NTC|8F-2 ORDR|9-M|ABS-15G|ABS-EE|ADB|ADN-MTL|ADV|ADV-E|ADV-H-C|ADV-H-T|ADV-NR|ADVCO|ADVW|AFDB|ANNLRPT|APP NTC|APP ORDR|APP WD|APP WDG|ARS|ATS-N|ATS-N-C|ATS-N/CA|ATS-N/MA|ATS-N/UA|AW|AW WD|BDCO|BW-2|BW-3|C|C-AR|C-AR-W|C-TR|C-TR-W|C-U|C-U-W|C-W|CB|CERT|CERTAMX|CERTARCA|CERTBATS|CERTBSE|CERTCBO|CERTCSE|CERTNAS|CERTNYS|CERTPAC|CERTPBS|CFPORTAL|CFPORTAL-W|CORRESP|CT ORDER|D|DEF 14A|DEF 14C|DEF-OC|DEF13E3"
//			+ "|DEFA14A"
					+ "|DEFA14C|DEFC14A|DEFC14C|DEFM14A|DEFM14C|DEFN14A|DEFR14A|DEFR14C|DEFS14A|DEFS14C|DEL AM|DFAN14A|DFRN14A|DOS|DOSLTR|DRS|DRSLTR|DSTRBRPT|EBRD|EFFECT|F-1|F-10|F-10EF|F-10POS|F-1MEF|F-2|F-3|F-3ASR|F-3D|F-3DPOS|F-3MEF|F-4|F-4 POS|F-4MEF|F-6|F-6 POS|F-6EF|F-7|F-7 POS|F-8|F-8 POS|F-80|F-80POS|F-9|F-9 POS|F-9EF|F-N|F-X|FOCUSN|FWP|G-405|G-405N|G-FIN|G-FINW|IADB|ID-NEWCIK|IFC|IRANNOTICE|MA|MA-A|MA-I|MA-W|MSD|MSDCO|MSDW|N-1|N-14|N-14 8C|N-14AE|N-14MEF|N-18F1|N-1A|N-1A EL|N-2|N-23C-1|N-23C-2|N-23C3A|N-23C3B|N-23C3C|N-27D-1|N-2MEF|N-3|N-3 EL|N-30B-2|N-30D|N-4|N-4 EL|N-5|N-54A|N-54C|N-6|N-6F|N-8A|N-8B-2|N-8B-4|N-8F|N-8F NTC|N-8F ORDR|N-CEN|N-CR|N-CSR|N-CSRS|N-MFP|N-MFP1|N-MFP2|N-PX|N-Q|N14AE24|N14EL|NO ACT|NPORT-EX|NPORT-P|NRSRO-CE|NRSRO-UPD|NSAR-A|NSAR-AT|NSAR-B|NSAR-BT|NSAR-U|NT 10-D|NT 10-K|NT 10-Q|NT 11-K|NT 15D|NT 20-F|NT N-CEN|NT N-MFP|NT N-MFP|NT N-MFP|NT NPORT-EX|NT NPORT-P|NT-NCEN|NT-NCSR|NT-NSAR|NTFNCEN|NTFNCSR|NTFNSAR|NTN 10D|NTN 10K|NTN 10Q|NTN 11K|NTN 20F|NTN15D2|OIP NTC|OIP ORDR|POS 8C|POS AM|POS AMC|POS AMI|POS EX|POS462B|POS462C|POSASR|PRE 14A|PRE 14C|PRE13E|PREA14A|PREA14C|PREC14A|PREC14C|PREM14A|PREM14C|PREN14A|PRER14A|PRER14C|PRES14A|PRES14C|PRRN14A|PX14A6G|PX14A6N|QRTLYRPT|QUALIF|REG-NR|REGDEX|REVOKED|RW|RW WD|S-1|S-11|S-11MEF|S-1MEF|S-2|S-20|S-2MEF|S-3|S-3ASR|S-3D|S-3DPOS|S-3MEF|S-4|S-4.{1}A|S-4 POS|S-4EF|S-4MEF|S-6|S-6EL|S-8|S-8 POS|S-B|S-BMEF|SB-1|SB-1MEF|SB-2|SB-2MEF|SC 13D|SC 13E1|SC 13E3|SC 13E4|SC 13G|SC 14D1|SC 14D9|SC 14F1|SC 14N|SC TO-C|SC TO-I|SC TO-T|SC13E4F|SC14D1F|SC14D9C|SC14D9F|SD|SDR|SE|SEC ACTION|SEC STAFF ACTION|SEC STAFF LETTER|SF-1|SF-3|SL|SP 15D2|STOP ORDER|T-3|TA-1|TA-2|TA-W|TACO|TH|TTW|U-1|U-12-IA|U-12-IB|U-13-60|U-13E-1|U-33-S|U-3A-2|U-3A3-1|U-5|U-6B-2|U-7D|U-9C-3|U5A|U5B|U5S|UNDER|UPLOAD|WDL-REQ|X-17A-5)$");

//	public Pattern patternsToExclude = Pattern.compile("");
//	public Pattern patternsToInclude = Pattern.compile("");

	public Pattern patternMiscDocuments = Pattern
			.compile("(?sm)ACCOUNTANT|ACCOUNTING|ARTICLES|AUDIT|CERTIFICATE|INCORPORATION|PROXY"
					+ "|Accountant|Accounting|Articles|Audit|Certificate|Incorporation|Proxy");

	public Pattern patternContractNameThis = Pattern.compile("((?<=(^|[\r\n]{1})([ ]{1,40})?)(This|THIS).{1,2})"
			+ "[A-Z].{3,80}?((?=[\"\\(])|Agreements?|AGREEMENTS?|Supplemental|SUPPLEMENTAL|Indenture|Lease"
			+ "|LEASE|INDENTURE|(REGARDING )?CHANGE OF CONTROL|(Regarding )Change of Control)");

	public Pattern patternContractNameParenDefined = Pattern
			.compile("(?<![\r\n]{1}[\\s ])\"(?=[A-Za-z\\s \\d]+{1,70}\")");

	public Pattern patternContractNameALLCAPSdated = Pattern
			.compile("(?<=(^|[\r\n]{1})([ ]{0,60})?(This |THIS )?)[A-Z].{1,50}?(?=,? ?dated)");

	public Pattern patternContractNameAgreementHardReturns = Pattern.compile(// upper/lower case but must end in
																				// these words
			"(?<=(^|[\r\n]{1})([ ]{0,60})?)([A-Z].{1,70}?(Agreements?|AGREEMENTS?)|(INDENTURE|Indenture|(REGARDING )?CHANGE OF CONTROL"
					+ "|(Regarding )Change of Control)(?=[ \r\n]{1,50}))");

	public Pattern patternContractNameByItselfHardReturns = Pattern// Add any K on its own line in all caps
			.compile("(?<=[\r\n]{1}[\t ]{0,50})[A-Z]{1}[A-Z ]{0,30}(AGREEMENTS?|INDENTURE)[A-Z ]{0,30}(?=[\r\n]{1})");

	public Pattern patternContractNameFromCoverPage = Pattern.compile(
			"(?ism)(?<=\r\n).{5,20}[\n]+(?=(dated|Dated|DATED).{0,50}(\r\n)?(between|BETWEEN|Between|among|AMONG|Among))");

	public Pattern patternOpinionInBody = Pattern.compile("(?ism)(?<=we have acted as.{1,25} )counsel(?= )");

	public Pattern patternEmploymentContract = Pattern
			.compile("(?sm)AWARD|COMPENSATION|EMPLOYEE|EXECUTIVE|INCENTIVE|OPTION|RELEASE|SEVERANCE"
					+ "|Award|Compensation|Employee|Executive|Incentive|Option|Release|Severance");

	public Pattern patternDescriptionOfSecurities = Pattern
			.compile("(?sm)[ \r\n]{0,30}(DESCRIPTION OF (SECURITIES|NOTES|BONDS|DEBENTURES|CERTIFICATES|DEBT)"
					+ "|Description [Oo]f (Securities|Notes|Bonds|Debentures|Certificates|Debt))");

	public Pattern patternArticlesShares = Pattern.compile(
			"(?sm)ARTICLES|INCORPORATION|COMMON|STOCK|SHARE" + "   |Articles|Incorporation|Common|Stock|Share");

	public Pattern patternContractsFinancial = Pattern.compile(
			// also includes policies (privacy, ethics), code of conduct, and opinions
			"(?sm)ACCOUNT|ACQUISITION|ADMININ|AGENCY|ASSIGN|ASSUMPTION|CALCULATION|CHANGE|COLLATERAL|CONFIDENTIAL|CONSULT"
					+ "|CONTINUATION|CONTRIBUTION|CONTROL|CONVERSION|CREDIT|CUSTOD|DEALER|DEPOSIT|DISCLOSURE|DISTRIBUTION|ESCROW"
					+ "|EXCHANGE|EXTENSION|FACILIT|FINANC|FORBEAR|GUARANT|INDEMN|INDENTURE|INTERCREDITOR|INVEST|(?=[ ^\r\n]{1})LEASE"
					+ "|LENDING|LETTERS? OF CREDIT|LICENS|LOAN|MANAGEMENT|MANUFACT|MARKET"
//			+ "|MASTER"//to generic
					+ "|MEMO OF UNDERSTANDING|MEMORANDUM OF UNDERSTANDING|MERGER|MODIFICAT|MORTGAGE|MRA|MSLA|OPINION|PARTICIPATION|PAYING"
					+ "|PAYMENT|PLACEMENT|PLAN(?= OF)|PLEDGE|POOLING|POWER OF ATTORNEY|PRICING|PROMISSORY|PURCHASE|REFUSAL|REIMBURS"
					+ "|REINSURANC|REORG|REPURCHASE|RIGHT|REINSURANCE|SALE|SECURIT|SERVIC|SETTLE|STANDSTILL|SUBORDINAT|SUBSCRIPTION"
					+ "|SUPPLEMENT|SUPPLY|TERMINATION|TRANSFER|TRANSITION|TRUST(?= )|UNDERWRITING(?= AGREEMENT)|VENTURE|VOTING|WARRANT"
					+ "|Account|Acquisition|Adminin|Agency|Assign|Assumption|Calculation|Change|Collateral|Confidential|Consult"
					+ "|Continuation|Contribution|Control|Conversion|Credit|Custod|Dealer|Deposit|Disclosure|Distribution|Escrow"
					+ "|Exchange|Extension|Facilit|Financ|Forbear|Guarant|Indemn|Indenture|Intercreditor|Invest"
					+ "|(?=[ ^\r\n]){1}Lease|Lending|Letter? OF Credit|Licens|Loan|ManagemENT|Manufact|Market"
//			+ "|Master"//too generic
					+ "|Memo [Oo]f Understanding|Memorandum [Oo]f Understanding|Memorandum Of|Merger|Modificat|Mortgage|Mra|Msla"
					+ "|Opinion|Participation|Paying|Payment|Placement|Plan(?= Of)|Pledge|Pooling|Power [Oo] Attorney|Pricing"
					+ "|Promissory|Purchase|Refusal|Reimburs|Reinsuranc|Reorg|Repurchase|Right|Reinsurance|Sale|Securit|Servic"
					+ "|Settle|Standstill|Subordinat|Subscription|Supplement|Supply|Termination|Transfer|Transition"
					+ "|TrustTRUST(?= )|Underwriting(?= Agreement)|Venture|Voting|Warrant"
					+ "|privacy|ethics|policies|policy|opinion|code of conduct");

	public static Pattern patternExhibitInContractA = Pattern
			.compile("(?sm)[\r\n]{1}[ \t]{0,200}(Exhibit|EXHIBIT|APPENDIX|Appendix|SCHEDULE|Schedule|Annex|ANNEX)"
					+ "[ ]{1,3}[a-z\\.A-Z\\-\\d\\(\\)]{1,7}( to( Indenture)?)? ? ? ?" + "([\r\n]{2}"
					+ "|(\\[?\\(?[A-Z-]{1,15} [A-Z-]{1,15} [A-Z]{1,15} )" + ")");

	public static Pattern patternExhibitPrefix = Pattern
			.compile("(?sm)[\r\n]{1}[ \t]{0,200}(Exhibit|EXHIBIT|APPENDIX|Appendix|SCHEDULE|Schedule|Annex|ANNEX)"
					+ "[ ]{1,3}[a-z\\.A-Z\\-\\d\\(\\)]{1,7}(?=( to( Indenture)?)? ? ? ?" + "([\r\n]{2})"
					+ "|(?=\\[?\\(?[A-Z-]{1,15} [A-Z-]{1,15} [A-Z]{1,15} )" + ")");

	public Pattern patternExhibitInContract = Pattern.compile(patternExhibitInContractA.pattern()
//				+"|"+
//						patternExhibitInContractB.pattern()
	);

	// Can you find Exhibit A on a new line essentially by itself.
	// Or EXHIBIT ON A NEW LINE WITH ALL CAPS THAT FOLLOW AT LEAST FOR THREE
	// WORDS.

	public static Pattern patternPAGE_PRE_S_C_SEC_CODES = Pattern.compile("<PAGE>|<PRE>|<S>|<C>");

	public static Pattern patternExhibitToc = Pattern
			.compile("[\r\n]{1}[ \t]{0,4}(Exhibit|EXHIBIT|APPENDIX|Appendix|SCHEDULE|Schedule|Annex|ANNEX)"
					+ "([ ]{1,2}[A-Z\\-\\d\\.\\(\\)a-z]{1,9})[ \t]{1,20}"
					+ "[\\p{Alnum}\\p{Punct}]{1,90}.{1,90}?[\r\n]{1}");
	// 5.1(e)(2)
	public static Pattern patternArticle = Pattern
			.compile("(?s)([\r\n]{1}[ ]{1,100}(ARTICLE|Article)[ \r\n\t]{1,3}[iIvVxX\\d]{1,5}"
					+ ".{1,200}?(?=(Section|Subsection|SECTION|SUBSECTION)[ \r\n\t]{1,4}[iIvVxX\\d\\.]{1,7}))");

	public static Pattern patternExhibitPageNumber = Pattern.compile(
			"(?sm)(EXHIBIT|ANNEX|APPENDIX|SCHEDULE|Exhibit|Annex|Appendix|Schedule).{1,150}?[A-Z]-?[\\d]{1,3}\r\n");

	public static Pattern patternTocPageNumber1 = Pattern.compile("\r\n[\\d]{1,3}\r\n");

	public static Pattern patternTocPageNumber = Pattern
			.compile("[\r\n]{1}.{1,250}?([ \t]{1}[A-Z]{1})?-?([\\d]{1,3}|[ixv]{1,3})( ?[\r\n])"// was: [\\d]{1,3}+?
					+ "|\r\n.*?\r\n[ ]{0,2}([ \t]{1}[A-Z]{1})?-?([\\d]{1,3}|[ixv]{1,3}) ? ?[\r\n]");
	// where pg# gets put on its own line due to html stripping.
	// patternTocPageNumberIgnoreHardReturnto be in synch with patternTocPageNumber

	public static Pattern patternTocPageNumberIgnoreHardReturn = Pattern
			.compile("(?<=[\r\n]{1}.{1,250}?)([ \t]{0,1}[A-Z]{0,1})-?[\\d]{1,3}+(?= ?[\r\n])"
					+ "|(?<=\r\n.{3,250}?\r\n[ ]{0,2})[ \t]{0,1}[A-Z]{0,1}-?[\\d]{1,3}(?= ? ?[\r\n])");

	public static Pattern patternTocPageNumberAll = Pattern.compile(patternTocPageNumber1.pattern()
//					+ "|" + patternTocPageNumber.pattern() + "|"
//					+ patternExhibitToc.pattern() + "|" + patternExhibitPageNumber.pattern()
	);

//	public static Pattern patternContractToc = Pattern.compile("(?s)(Section|Subsection|SUBSECTION|SubSection|"
//			+ "SECTION)[ \r\n\t]{1,4}[iIvVxX\\d\\.]{1,6}.{1,175}?.{1,175}?[\\d]{1,4}");

//	public static Pattern patternContractTocWithExhibits = Pattern
//			.compile(patternContractToc.pattern() + "|" + patternExhibitToc.pattern() + "|" + patternArticle.pattern());

//	public static Pattern patternContractTocWithExhibitsAndTocPageNumber = Pattern.compile(
//					patternContractToc.pattern() + "|"+ 
//			patternExhibitToc.pattern() + "|" + patternArticle.pattern() + "|" + patternTocPageNumber.pattern());

	public static Pattern alphabetPattern = Pattern.compile("[A-Za-z]{5,}");

	public static Pattern nonAlphaPattern = Pattern.compile("[^A-Za-z]");

	public static Pattern patternClosingPara = Pattern
			.compile("(?sm)(WITNESS WHEREOF.{1,90}(executed|set.{1,4}" + "their.{1,4}hand).{1,150}\\.)");

	public static Pattern spaceNumberspacePattern = Pattern.compile("(?<!([A-Za-z])) [-]{0,}[\\d,]{1,}");

	public Pattern patternParentChildParas = Pattern
			.compile("([:;]|[,;] (and|or)) ?\r ?([A-Z]{1}|[\\d\\.]{2}|\\([a-z\\dA-Z]\\))");

	public static Pattern patternDescription = Pattern.compile("(?sm)<DESCRIPTION>.*?$");

	public static Pattern patternDefinedWordInBodyOfText = Pattern
			.compile("(?<![\r\n]{1}[\\s ])(Qx)?\"[A-Za-z\\s \\d]+{1,60}\"");

	public static Pattern patternDefinedWordInDefSecA = Pattern
			.compile("(?<=[\r\n]{3}[ \t]{0,20}) ?(Qx)?(Section [\\d\\.]{1,4}|SECTION [\\d\\.]{1,4})(XQ)?"
					+ " ?\"[\\{\\}\\dA-Zx]{1}[A-Za-z ,;:'/\\.\\&\\-\\)\\(\\[\\]\\$\\%\\*\\d]{1,95}\"(XQ)?[;:,]{0,1}(XQ)?"
					+ "(?=([\r\n]{1,2})?([ \t]{1,30})?[\\[\\(\\$A-Za-z\\d]{1})");

	public static Pattern patternDefinedWordInDefSecB = Pattern
			.compile("(?<=[\r\n]{3}[ \t]{0,20} ?)(The term |[\\d\\.]+)?"
					+ "(Qx)?( ?\"|'')[\\dA-Z]{1}[A-Za-z \\{\\},;:'/\\.\\&\\-\\)\\(\\[\\]\\$\\%\\*\\d]{1,95}(\"|'')(XQ)?[;:,]{0,1}(XQ)?"
					+ "(?=([\r\n]+)?([ \t]{1,30})?[\\[\\(\\\\)$A-Za-z\\d]{1})");

	public static Pattern patternDefinedWordInDefSecNoQuotes = Pattern.compile("(?<=[\r\n]{3}[ \t]{0,20})"
			+ "((\\b([A-Z]\\w*)|[\\d-A-Z]{3,6}) ?(\\b([A-Z]\\w*)|[\\d-A-Z]{3,6})? ?(\\b([A-Z]\\w*)|[\\d-A-Z]{3,6})? ?"
			+ "(\\b([A-Z]\\w*)|[\\d-A-Z]{3,6})? ?(\\b([A-Z]\\w*)|[\\d-A-Z]{3,6})?)(?=(XQ)?[;:,]{0,1}(XQ)?"
			+ "((XQ)?[ -:]{0,2}(XQ)?(has|have) the meaning|(XQ)?[ -:]{0,2}(XQ)?shall mean|(XQ)?[ -:]0,2}(XQ)?means?|defined?)"
			+ "(?=([\r\n]{1,2})?([ \t]{1,30})?[\\[A-Za-z\\d]{1}))");

	public static Pattern patternDefinedWordColon = Pattern.compile("(?<=[\r\n]{2}[ \t]{0,20})"
			+ "(\\b([A-Z]\\w*)|[A-Z]{1,}[a-zA-Z]{1,30}-[A-Z]{1,3}[a-zA-Z]{1,30}|[\\d-A-Z]{3,6}) (or |OR )?"
			+ "(\\b([A-Z]\\w*)|[A-Z]{1,}[a-zA-Z]{1,30}-[A-Z]{1,3}[a-zA-Z]{1,30}|[\\d-A-Z]{3,6})? ?"
			+ "(\\b([A-Z]\\w*)|[A-Z]{1,}[a-zA-Z]{1,30}-[A-Z]{1,3}[a-zA-Z]{1,30}|[\\d-A-Z]{3,6})? ?"
			+ "(\\b([A-Z]\\w*)|[A-Z]{1,}[a-zA-Z]{1,30}-[A-Z]{1,3}[a-zA-Z]{1,30}|[\\d-A-Z]{3,6})? ?"
			+ "(\\b([A-Z]\\w*)|[A-Z]{1,}[a-zA-Z]{1,30}-[A-Z]{1,3}[a-zA-Z]{1,30}|[\\d-A-Z]{3,6})? ?"
			+ "(\\b([A-Z]\\w*)|[A-Z]{1,}[a-zA-Z]{1,30}-[A-Z]{1,3}[a-zA-Z]{1,30}|[\\d-A-Z]{3,6})? ?"
			+ "(\\b([A-Z]\\w*)|[A-Z]{1,}[a-zA-Z]{1,30}-[A-Z]{1,3}[a-zA-Z]{1,30}|[\\d-A-Z]{3,6})?" + "(?=(XQ)?:(XQ)?"
			+ "([\r\n]{1,5})?([ \t]{1,30})?[\\[A-Za-z\\d]{1,}.{4,100} mean" + ")");

	public static Pattern patternDefinedWordInDefSecBracket = Pattern.compile("(" + "?<=[\r\n]{3}[ \t]{0,20} ?"
			+ " ?\\[)(Qx)?\"(?=\\[)?[\\dA-Z]{1}[A-Za-z ,;:'/\\.\\&\\-\\)\\(\\[\\]\\{\\}\\$\\%\\*\\d]{1,95}\"(XQ)?[;:,]{0,1}(XQ)?"
			+ "(?=([\r\n]{1,2})?([ \t]{1,30})?[\\[\\(\\$A-Za-z\\d]{1})");

	public static Pattern patternDefinedWordPrecededByParaNumber = Pattern.compile("(" + "(?<=[\r\n]{3}[ \t]{0,20}"
			+ "\\(?[A-Za-z\\d]{1,5}" + "[\\.\\)]{1,2}" + "[A-Za-z\\d\\.]{0,5}" + "[ \r\n\t]{1,8}" + "))" + "\""
			+ "[\\dA-Z]{1}[A-Za-z ,;:'/\\.\\&\\-\\)\\(\\[\\]\\{\\}\\$\\%\\*\\d]{1,95}\"(XQ)?[;:,]{0,1}(XQ)?"
			+ "(?=([\r\n]{1,2})?([ \t]{1,30})?[\\[\\(\\$A-Za-z\\d]{1})");

	public static Pattern patternDef_mean = Pattern
			.compile("(?<=" + "\r\n(Qx)?\\(?([\\d\\.]{0,5}|[A-Za-z]{0,1}[\\.\\)]{1,2})(XQ)?)" + "(Qx)? ?\""
					+ " ?[A-Z].{2,65}\"(XQ)?" + "(?=.{1,20}mean[sing]{0,3}.*?\r\n)" + "");

//	where def is preceded by 2.22 etc

	public static Pattern patternDefinedWordInDefSec = Pattern.compile(patternDefinedWordInDefSecA.pattern() + "|"
			+ patternDefinedWordInDefSecB.pattern() + "|" + patternDefinedWordInDefSecNoQuotes.pattern() + "|"
			+ patternDefinedWordColon.pattern() + "|" + patternDefinedWordInDefSecBracket.pattern() + "|"
			+ patternDefinedWordPrecededByParaNumber.pattern() + "|" + patternDef_mean.pattern());

	/*
	 * public static Pattern patternDefinedWordsNoQuotes = Pattern
	 * .compile("(?<=[\r\n]{3,6}([ ]{2,20}|[\t]{1,10}))" +
	 * "(?!Re:|RE:|Subject:|W ?I ?T ?N ?E ?S ?S ? ??E? ?T? ?H? ?  ?T ?H ?A ?T ?:" +
	 * "|SUBJECT:|Section|SECTION|Article|ARTICLE)[A-Z]{1}[A-Za-z]{2}.{3,150}" +
	 * "(?<!( [a-z]{3,20} ?.{3,150}))" + "(: |\\. |- )" +
	 * "(?=([\r\n]{1,2})?([ \t]{1,30})?[A-Za-z\\d]{1})");
	 */
	// pickup here - why is this a defined term: THE PRINCIPAL OF THIS NOTE IS
	// PAYABLE AS SET FORTH HEREIN.?????
	public static Pattern patternDefinedWordsInDefinitionArticle = Pattern.compile(patternDefinedWordInDefSec.pattern()
	// + "|"
	// + patternDefinedWordsNoQuotes.pattern()
	);

	// public static Pattern patternDefinedWordsFromDefSection = Pattern
	// .compile("(?s)(?<=[\r\n]{1}[\\s
	// \t]{0,20})\"[\\p{Alnum}\\p{Punct}].{1,50}\"(?=.*([\r\n]( \t\\s)?))");

	// need to have [\r\n]{3,} b/c won't pickup 2 or more if just \r or \n

	public static Pattern patternSection = Pattern
			.compile("(Section|Subsection|SUBSECTION|SubSection|SECTION)[ \r\n\t]{1,3}[iIvVxX\\d\\.]{1,6}");

	public static Pattern patternSectionHeading = Pattern.compile(
			"(?s)(([\r\n]{1,}([\t]{1,4}|[\\s]{1,15})?)(Section|Subsection|SUBSECTION|SubSection|SECTION)[ \r\n\t]{1,3}[iIvVxX\\d\\.]{1,6}(?![\r]{2,})"
					+ "[\\-\t\\(\\)\\d\\&\\$\\%A-Za-z ;,\\[\\{\\}']{4,150}(\\.|[\r\n]{2}))"
					+ "|((Section|SECTION).{1,5}[\\d\\.]{1,6}.{1,4}Taxes.{1,5}[\r\n])");
	// added Taxes -didn't know how to otherwise get. Had to add (?sm)

	public static Pattern patternSectionHeadingWithClosingParagraph = Pattern.compile(patternClosingPara.pattern());

	public Pattern patternContractSentenceStart = Pattern
			.compile("(?<=(\\.(\r\n|[ ])))[A-Z\\(]{1}|(?<=[\r\n]{1}[ \\s])[A-Z\\(]{1}");
	// start of sentence -- hard return followed by A-Z \\(
	// [\r\n]{1}[ \\s][A-Z\\(]{1}
	// start of sentence is end of space period.
	// (?<=(\\.(\r\n|[ ])))[A-Z\\(]{1}

	public static Pattern ExtraHtmlPattern = Pattern.compile("(?i)(</?[[font]|[b]|[i]|[u]]*[^>]*>)");

	public static Pattern ExtraHtmlPattern2 = Pattern.compile("(?i)(</?p[^>]*>)");

	public String getSolrCore2PushInto() {
		return solrCore2PushInto;
	}

	public void setSolrCore2PushInto(String solrCore2PushInto) {
		this.solrCore2PushInto = solrCore2PushInto;
	}

	public String getBaseSolrUrl() {
		return baseSolrUrl;
	}

	public void setBaseSolrUrl(String baseSolrUrl) {
		this.baseSolrUrl = baseSolrUrl;
	}

	public static int getNextParagraph_eIdx(String text, int eIdx) {

		if (eIdx >= text.length())
			return eIdx;

		String txtSnip = text.substring(eIdx, text.length());
		String[] lines = txtSnip.split("\r\n");
		String line;
		for (int i = 0; i < lines.length; i++) {
			line = lines[i];
			if (line.trim().length() == 0)
				continue;
			if (i > 8)
				break;
			else {
				eIdx = eIdx + line.length() + (i + 1) * 2;// 2* to account for \r\n
//				System.out.println("line==" + line);
//				System.out.println("text.cut="+text.substring(0,eIdx));
				break;
			}
		}

		return eIdx;
	}

	public String keepCellsInSameRow(String html, Pattern startPattern, Pattern endPattern)
			throws FileNotFoundException {

		// this simply removes all hard returns within start and end pattern

		NLP nlp = new NLP();

		StringBuffer sb = new StringBuffer();
		// sb.delete(0, sb.toString().length());
		int start = 0, htmlLen = html.length();
		List<Integer> idxStartTrs = nlp.getAllIndexStartLocations(html, startPattern);
		List<Integer> idxEndTrs = nlp.getAllIndexStartLocations(html, endPattern);
		if (idxStartTrs.size() == 0 || idxEndTrs.size() == 0) {
			// //System.out.println("no pattern found..");
			return html;
		}
		int endTrI = 0, endTrLoc = 0;
		for (Integer idxStartTr : idxStartTrs) {
			if (start > idxStartTr)
				continue;
			sb.append(new String("\r" + html.substring(start, idxStartTr)));
			// above is identifying JUST the start of the pattern - so we do NOT
			// want to replace anything here!!

			for (Integer eTr = endTrI; eTr < idxEndTrs.size(); eTr++) {
				endTrI++;
				endTrLoc = idxEndTrs.get(eTr);
				if (endTrLoc <= idxStartTr)
					continue;
				else {
					String htmlTemp = new String(html.substring(idxStartTr, endTrLoc));
					//
					htmlTemp = htmlTemp
							.replaceAll("(?<=[\\p{Alnum}\\p{Punct}])[\r\n]{1,}(?=[\\p{Alnum}\\p{Punct}])", " ")
							.replaceAll("[\r\n]{1,}", "").replaceAll("[ ]{2,}", " ");
					// If I replace [\\s]{2,} w/ "" it causes errors as well
					if (startPattern.equals("startTd") || startPattern.equals("startTh")) {
						htmlTemp = NLP.htmlPara.matcher(htmlTemp).replaceAll(" ");
						// if <td > <p>hello</p>world</td> it removes the <p
						// (same for <div and <br)
					}
					sb.append(new String(htmlTemp) + "\r");
					break;
				}
			}
			start = endTrLoc;
		}
		String keepCellsTextTogether = (html.substring(start, htmlLen));
		// sb.append(new String(html.substring(start, htmlLen)))
		sb.append(new String(keepCellsTextTogether));
		String temp = sb.toString();
		return temp;
	}

	public static boolean hasDisclosureIWant(List<String> listParseThese, String formType) throws IOException {

		NLP nlp = new NLP();

		/*
		 * if I am only parsing disclosure and it is not a form I want and I don't want
		 * metadata (almost always true) then don't parse. if I only want to parse
		 * disclosure then I check if it is the formType I want, and if not I go to the
		 * next item in the master Idx
		 */

		for (int i = 0; i < listParseThese.size(); i++) {
//				System.out
//						.println(
//								"list.get(i)=" + list.get(i) + " nlp.match - formType="
//										+ nlp.getAllMatchedGroups(list.get(i),
//												Pattern.compile("(?sm)(?<=contractType=).*?(?=\\|\\|)"))
//												.get(0));
			if (formType.equals(nlp
					.getAllMatchedGroups(listParseThese.get(i), Pattern.compile("(?sm)(?<=contractType=).*?(?=\\|\\|)"))
					.get(0))) {
//				System.out.println("getAcc. a form I want=" + nlp.getAllMatchedGroups(listParseThese.get(i),
//						Pattern.compile("(?sm)(?<=contractType=).*?(?=\\|\\|)")).get(0));

				return true;
			}

		}

//			System.out.println("formType=" + formType + " listParseOnlyThisFormTypeInMasterIdx.get(j)="
//			+ listParseOnlyThisFormTypeInMasterIdx.get(j));

		return false;

	}

	public static boolean doNotParseThis() {
		boolean parseThis = true;

		return parseThis;

	}

	public static List<String> isParsingDisclosureFormFound(TreeMap<String, List<String>> mapParseThese,
			String docFormType) throws IOException {

		NLP nlp = new NLP();

		List<String> listAttributes = new ArrayList<String>();

		List<String> listParse = new ArrayList<String>();
		String parseFormType = "", contractType = "", solrCore = "";
		if (mapParseThese.containsKey("D")) {
			listParse = mapParseThese.get("D");
//			System.out.println("this is a disclosure doc. map contains D");
			for (int i = 0; i < listParse.size(); i++) {
//				System.out.println("docFormType=" + docFormType + " is it this type?=" + listParse.get(i));
				parseFormType = nlp
						.getAllMatchedGroups(listParse.get(i), Pattern.compile("(?<=contractType=).*?(?=$|\\|)")).get(0)
						.replaceAll("[\\|]+", "");
//				System.out.println("mapParse docFormType=" + parseFormType + " .nc docFormType=" + docFormType);
				if (docFormType.equals(parseFormType) && parseFormType.trim().length() > 0) {
//					System.out.println("2 This is the Disclosure I want.docFormType=" + docFormType + " parseFormType="
//							+ parseFormType);
					contractType = parseFormType;
					solrCore = nlp.getAllMatchedGroups(listParse.get(i), Pattern.compile("(?<=solrCore=).*?(?=$|\\|)"))
							.get(0).replaceAll("[\\|]+", "");

//					System.out.println("returning disclosure formType that matches. listParse=" + listParse.get(i));
					listAttributes.add(listParse.get(i));

					return listAttributes;
				}
			}
		}

		return listAttributes;
	}

	public static String getQuotedTerms(String text) throws IOException, SQLException {
		NLP nlp = new NLP();

		System.out.println("getQuotedTerms");
		long startTime = System.currentTimeMillis();
//		PrintWriter pw = new PrintWriter(new File("c:/temp/quotedTermTxt.txt"));
//		pw.append(text);
//		pw.close();

		List<String[]> listQuotedTerms = nlp.getAllStartIdxLocsAndMatchedGroups(text,
				Pattern.compile("(?sm)\"([a-zA-Z\\d\\-]{1,25} ?){1,7}\""));

		TreeMap<String, Integer> mapQuotedTerms = new TreeMap<String, Integer>();
		double len_cap, len_lower_case;
		String quotedTerm = "";
		System.out.println("pattern got. listQuotedTerms.size=" + listQuotedTerms.size());
		for (int i = 0; i < listQuotedTerms.size(); i++) {
//			System.out.println("i="+i+" : quotedTerm==" + Arrays.toString(listQuotedTerms.get(i))+"||||||");
			quotedTerm = listQuotedTerms.get(i)[1].replaceAll("[\"]+|[,\\.;: ]+$", "").replaceAll("xxPD", "\\.").trim();
			len_cap = nlp.getAllIndexEndLocations(quotedTerm, Pattern.compile("(^| )[A-Z]{1}")).size();
			len_lower_case = nlp.getAllIndexEndLocations(quotedTerm, Pattern.compile("(^| )[a-z]{1}")).size();
//			System.out.println("i=="+i);
			if (quotedTerm.length() > 0 && len_cap > len_lower_case && quotedTerm.split(" ").length < 50
					&& nlp.getAllIndexEndLocations(quotedTerm, Pattern.compile("(^| )[A-Z]{1}")).size() < 8
					&& nlp.getAllIndexEndLocations(quotedTerm, Pattern.compile("\\.  [A-Z]|^[^A-Z\\d]")).size() < 1) {
				mapQuotedTerms.put(quotedTerm, Integer.parseInt(listQuotedTerms.get(i)[0]));
			}
//			System.out.println("2i=="+i);
		}
		int qCnt = 0;
		quotedTerms = "";
		StringBuilder sbQT = new StringBuilder();
		StringBuilder sbQtTxt = new StringBuilder();
		String val = "";

//		System.out.println("i end.");
		List<String[]> listNotDef = ContractReviewTools.it_is_not_defined(text, text, true, true, false);
		// set last to false so it does not print to screen

		TreeMap<Integer, String> mapOrdered = new TreeMap<Integer, String>();
		for (Map.Entry<String, Integer> entry : mapQuotedTerms.entrySet()) {

			mapOrdered.put(entry.getValue(), entry.getKey());

		}

//		NLP.printListOfStringArray("listNotDef==", listNotDef);
		for (int i = 0; i < listNotDef.size(); i++) {
			mapOrdered.put(Integer.parseInt(listNotDef.get(i)[1]), listNotDef.get(i)[0]);
		}

		for (Map.Entry<Integer, String> entry : mapOrdered.entrySet()) {
			val = entry.getValue().replaceAll("[\\.;,:]$", "").replaceAll("\r\n", " ").replaceAll("[ ]+", " ");
			sbQtTxt.append(val.replaceAll(" ", "_") + " ");
			if (qCnt == 0) {
				sbQT.append("\"" + val + "\"");
			}

			else {
				sbQT.append(",\"" + val + "\"");
			}
			qCnt++;
		}
		quotedTerms = sbQT.toString().replaceAll("[\\[\\]]", "").replaceAll("xxOP", "(").replaceAll("CPxx", ")")
				.replaceAll("(xxPD)+", ".").replaceAll("xxOB", "").replaceAll("CBxx", "").replaceAll("OBCB", "")
				.replaceAll("CBOB", "");
//		System.out.println("quoted terms===" + quotedTerms);

		quotedTermsTxt = sbQtTxt.toString();

//		System.out.println("quotedTerms, seconds==" + ((endTime - startTime) / 1000));

//		PrintWriter pw = new PrintWriter(new File("c:/temp/quotedTerms.txt"));
//		pw.append(quotedTerms);
//		pw.close();

		return quotedTerms;
	}

	public String getContract(String txt, List<String[]> secMteaData, boolean parseSuperFast, boolean getMetadata,
			boolean parseFromMeta, boolean regenerate, boolean clientTextIsDisclosure,
			TreeMap<String, List<String>> mapParseThese,String date) throws IOException, SQLException, SolrServerException {

//		System.out.println("STARTING HERE ********************************* @@@@@getContract. line 1111=");

		GoLaw gl = new GoLaw();
		boolean isFinancialContract = false, parse = false, hasGoverningLaw = false, disclosureFormFound = false;
//		System.out.println("clientText.len=" + clientText.length());
		boolean parseClientText = true;
		parse = true;
		isFinancialContract = false;
//			System.out.println("1. parse===" + parse);

		System.out.println("it is client text=" + parseClientText);
		if (parseClientText && clientTextIsDisclosure) {
			parsingDisclosure = true;
		}

		GetContracts gK = new GetContracts();

		System.out.println("getContract start");
		NLP nlp = new NLP();
//		File f = new File(filePath);
//		if (!f.exists() && !parse) {
//			System.out.println("!f.exists - return=" + f.getAbsolutePath());
//			return;
//		}

		String meta = "", docFormType = "", description = "", textSnip = "", textSnipStrip = "", text = "", modify = "",
				contractLongName = "", contractLongNameHasKeyWords = "", contractTypeInBody = "", contractNameAlgo = "",
				inBody = "", firstPartOfK = "", nonStrippedText = "", year = "", moStr = "",
				allConractPatternsInBody = "", openingParagraph = "", openingParagraphItems = "",
				openingParaContractingParties = "", openingParaContractingPartyRole = "",
				openingParaContractingRole = "", openParaContractName = "", legalEntities = "",
				solrCore = (StringUtils.isNotBlank(solrCore2PushInto) ? solrCore2PushInto : ""), contractType = "",
				textMediumSnip = "", textMediumSnipStrip = "", document = "",
				// metadata = "",
				leTxt = "", edgarLink = "";

		String acc_link_filename = "", acc = "", formType = "", companyName = "", cik = "", fileDate = "";

//		System.out.println("secMetaData ary to str=" + Arrays.deepToString(secMetaData.get(0)));
		System.out.println("getAcc:: " + acc + " formType: " + formType + " companyName: " + companyName + " cik: "
				+ cik + " fileDate: " + fileDate);

		if (nlp.getAllIndexEndLocations(acc,
				Pattern.compile("0000912057.{1}02.{1}010869|0000912057.{1}02.{1}010869|0000950144.{1}02.{1}003801"))
				.size() > 0)
			return "";

		double kTypAmt = 0.25;// first part of text to search for contractType
		int leCnt = 0, largeFileKfile = 0, txtLen = 0, qtr = 0, numberOfLegalEntities = 0, smallFile = 0, largeFile = 0;
//		System.out.println("acc=" + acc);

//		System.out.println("a fileDate=" + fileDate);
		if (!parseClientText) {
			year = fileDate.substring(0, 4);
		} else {
			fileDate = date.replaceAll("-|/", "");
			year = fileDate.substring(0, 4);
		}
		System.out.println("b year=" + year);

		moStr = fileDate.replaceAll("-", "").substring(4, 6);
//		System.out.println("mosStr=" + moStr);
		if (moStr.substring(0, 1).equals("0")) {
			moStr = moStr.substring(1, 2);
			// //System.out.println("2mosStr="+moStr);
		}

		qtr = (Integer.parseInt(moStr) + 2) / 3;

		// if acc file is present - then don't parse this .nc file.

		Pattern patternDocument = Pattern.compile("(?ism)<DOCUMENT>.*?</DOCUMENT>");
		Pattern patternDocType = Pattern.compile("(?ism)(?<=<TYPE>).*?$");// differs from formType in master.idx
		Pattern patternFilename = Pattern.compile("(?ism)(?<=<FILENAME>).*?$");// edgar link filename
//		System.out.println("Look at me - I prepped all my doc patterns to help me find the documents I want! woo hoo.");

		txt = "<DOCUMENT>\r\n<TEXT>\r\n<TYPE>CLIENT_DOC\r\n<FILENAME>TBD.htm</FILENAME>\r\n"
				+ "<DESCRIPTION>EXHIBIT</DESCRIPTION>\r\n" + txt + "\r\n</TEXT>\r\n</DOCUMENT>";
		List<String[]> listOfContractNamesFromAlgo = new ArrayList<String[]>();
		List<String> listDocuments = nlp.getAllMatchedGroups(txt, patternDocument);
		List<String[]> listFileName_Displayed_Description = new ArrayList<String[]>();
		List<String> listContractAttrbutes = new ArrayList<String>();
		List<String[]> listDated = new ArrayList<String[]>();
		List<String[]> listBetweenAmong = new ArrayList<String[]>();

//		System.out.println("listDocuments.size()=" + listDocuments.size());

		List<String> metadataList = new ArrayList<String>();
		List<String> metadataList2 = new ArrayList<String>();
		List<String> listTmp = new ArrayList<String>();

		// if map size is 2 then it will have both "D" and "C", so I first check if form
		// type equals the form type in "D" and if not I know it can't be D and
		// therefore I default to "C"
		String filesAlreadyParsed = strippedFolder + year + "/QTR" + qtr + "/_FilesParsed/";
		Utils.createFoldersIfReqd(filesAlreadyParsed);
		File filesParsed = new File(filesAlreadyParsed + acc + ".txt");
//		System.out.println("listDoc.size=" + listDocuments.size());

		String kId = "";

		FileSystemUtils.createFoldersIfReqd(metadataFolder + year + "/QTR" + qtr + "/");
		File fileMetadata = new File(metadataFolder + year + "/QTR" + qtr + "/" + "_" + ".txt");

		PrintWriter pwMeta = new PrintWriter(fileMetadata);
		List<String[]> listKNameAlgoTmp = new ArrayList<String[]>();

		String json = "";
		for (int c = 0; c < listDocuments.size(); c++) {
			document = listDocuments.get(c);
			kId = "";

			if (acc.equals("0001104659-19-024450"))
				continue;
			if (parseClientText) {
				acc = "0123456789-23-567890";
			}
			kId = acc + "_" + c;
			System.out.println("kId=" + kId);

			governLaw = "";
			System.out.println("@listDoc kId=" + acc + "_" + c);
//			System.out.println("listDocuments.get(c)=" + listDocuments.get(c).length());
			disclosureFormFound = false;
			parsingDisclosure = false;

			if (parseClientText && clientTextIsDisclosure) {
				parsingDisclosure = true;
			}

			filesParsed = new File(filesAlreadyParsed + kId + ".txt");
			if (!parseClientText && !regenerate) {
				if (filesParsed.exists())
//					System.out.println("returned!!!");
					return "";
			}

			textSnip = "";
			textMediumSnip = "";
			textMediumSnipStrip = "";
			contractLongNameHasKeyWords = "";
			metadataList = new ArrayList<String>();
			leCnt = 0;
			hasGoverningLaw = false;
			txtLen = 0;
			allConractPatternsInBody = "";
			contractLongName = "";
			contractType = "";
			contractTypeInBody = "";// was contractType found in body of text, if so this is least reliable
			description = "";
			contractNameAlgo = "";
			inBody = "";
			legalEntities = "";
			meta = "";
			nonStrippedText = "";
			openingParagraphItems = "";
			openingParaContractingParties = "";
			openingParaContractingPartyRole = "";
			openingParaContractingRole = "";
			openParaContractName = "";
			text = "";
			firstPartOfK = "";
			textSnip = "";
			docFormType = "";
			parse = false;

			text = nlp.getAllMatchedGroups(document, patternDocument).get(0);
			System.out.println("1 HTML FILESIZE========" + (text.length() / 1000) + " KBs. kId=" + kId + " clientText="
					+ parseClientText);
			textSnip = document.substring(0, Math.min(document.length(), 5000));
			textMediumSnip = document.substring(0,
					Math.min(35000, (int) ((double) document.length() * (double) kTypAmt)));

//			PrintWriter pw = new PrintWriter(new File("c:/temp/temp_doctyp.txt"));
//			pw.append(text);
//			pw.close();

			if (nlp.getAllMatchedGroups(textSnip, patternDocType).size() > 0)
				docFormType = nlp.getAllMatchedGroups(textSnip, patternDocType).get(0);
//			System.out.println("1 type=" + docFormType);
			// if key="D" and type=formType (masterIdx) then it is disclosure and I want to
			// parse it and skip contract specific methods. If type!=formType OR if there is
			// not key="D" but a key = "C" then I hunt for contracts that meet the list
			// associated with "C" or if there is not key = "C" and type!=formType and I
			// want to parse metadata I continue with contract methodology but skip solr
			// push.

			List<String> listParse = new ArrayList<String>();
			listTmp = isParsingDisclosureFormFound(mapParseThese, docFormType);
			disclosureFormFound = false;
			if (listTmp.size() > 0) {
				disclosureFormFound = true;
				parsingDisclosure = true;
				System.out.println("disclosureFormFound=" + disclosureFormFound);
			}

			if (nlp.getAllMatchedGroups(textSnip, patternFilename).size() == 0) {
				acc_link_filename = acc + ".txt";
			}

			if (nlp.getAllMatchedGroups(textSnip, patternFilename).size() > 0) {
				acc_link_filename = nlp.getAllMatchedGroups(textSnip, patternFilename).get(0);
			}

			if (nlp.getAllMatchedGroups(textSnip, patternDescription).size() > 0) {
				description = nlp.getAllMatchedGroups(textSnip, patternDescription).get(0);
			}

			if (description.trim().length() == 0) {
				description = docFormType;
			}

			// cleanup description by removing various non-alpha characters.

			description = description
					.replaceAll("(?is)(<DESCRIPTION>|[\\$\\*\r\n\\%\\&\\)\\(<>\"\'\\[\\]\\{\\}])|\\\\|\\/", " ")
					.replaceAll("[:;^\\%\\&\\*\\(\\)\\+\\=]", " ").replaceAll("[\t_\\-\\=]+|[ ]+", " ");

			if (!parseClientText) {

				contractLongName = description.replaceAll(
						"(?i)(<DESCRIPTION>|IDEA: " + "|[\\$\\*\r\n\\%\\&\\)\\(<>\"\'\\[\\]\\{\\}])|\\\\|\\/", " ")
						.replaceAll("[^a-zA-Z-\\.\\d_ ]", " ").replaceAll("[ ]+", " ").trim();
			}

//			System.out.println("description=" + description);
			contractLongName = contractLongName.substring(0, Math.min(contractLongName.length(), 120));

			if (!parseClientText && !disclosureFormFound && mapParseThese.containsKey("D")
					&& mapParseThese.size() == 1) {
				System.out.println("continue...gk1. kId=" + kId);
				continue;// only parsing disclosure but form not found
			}

//			System.out.println("disclosureFormFound==" + disclosureFormFound);

			if (!parseClientText && disclosureFormFound) {
				listParse = new ArrayList<String>();
				listParse = listTmp;
				solrCore = listParse.get(0);
				contractType = nlp
						.getAllMatchedGroups(listParse.get(0), Pattern.compile("(?<=contractType=).*?(?=\\|\\|)"))
						.get(0);
				solrCore = nlp.getAllMatchedGroups(listParse.get(0), Pattern.compile("(?<=solrCore=).*?(?=\\|\\|)"))
						.get(0);

//				System.out.println("1.contractType=" + contractType);
//				System.out.println("1.solrCore=" + solrCore);
				parse = true;
				parsingDisclosure = true;
				System.out.println("2. parsingDisclosure===" + parse);
			}

			List<Pattern> listP = gl.populateGlobalPatternsVariables();

//			System.out.println(
//					"description=" + description + " acc_link_filename=" + acc_link_filename + " type=" + docFormType);
//			description= XBRL INSTANCE DOCUMENT acc_link_filename=\\. type=EX-101.INS
			if (nlp.getAllMatchedGroups(docFormType, Pattern.compile(
					"(?ism)XBRL|XML|GRAPHIC|IMAGE|GRAPHIC|\\.pdf|\\.gif|\\.GIF|\\.JPG|101\\.(CAL|SCH|PRE|DEF|LAB)"))
					.size() > 0
					|| nlp.getAllMatchedGroups(acc_link_filename,
							Pattern.compile("(?ism)XBRL|GRAPHIC|IMAGE|\\.(XML|XSD|gif|jpg|pdf)")).size() > 0
//					|| nlp.getAllMatchedGroups(acc_link_filename, Pattern.compile("(?ism)\\.jpg|\\.pdf|\\.xml|\\.gif"))
//							.size() > 0
					|| nlp.getAllIndexEndLocations(description, Pattern.compile("IDEA:? XBRL|XBRL")).size() > 0) {
//				System.out.println("12. - skipping images kId="+kId);
				// save heading metadata here.
//				pwMeta.append("kId=" + kId.replaceAll("kId=", "") + "\r\nfDate=" + fileDate + "\r\nfSize=" + text.length() + "\r\nacc=" + acc
//						+ "\r\ncik=" + cik + "\r\nformType=" + formType + "\r\ncompanyName=" + companyName
//						+ "\r\nacc_link_filename" + acc_link_filename
//						+ "\r\nedgarLink=https://www.sec.gov/Archives/edgar/data/" + cik + "/" + acc.replaceAll("-", "")
//						+ "/" + acc_link_filename + "\r\ncontractLongName=" + contractLongName + "\r\ntype="
//						+ docFormType);

//				fileMetadata = new File(metadataFolder + year + "/QTR" + qtr + "/" + kId + " "
//						+ contractLongName.replaceAll("[^a-zA-Z ]", "") + ".txt");

				System.out.println("not saving metadata-likely nothing!");
//				pwMeta = new PrintWriter(fileMetadata);
//				pwMeta.close();
//				fileMetadata.delete();
				System.out.println("deleted pwMeta?continue...gk2.b/c image. kId=" + kId);
				continue;
			}

			textMediumSnipStrip = gl.stripHtml(textMediumSnip);
//			System.out.println("@contractNameFinder. textMediumSnipStrip.len==" + textMediumSnipStrip.length());
//			System.out.println("textMediumStrip=="+textMediumSnipStrip);
			listKNameAlgoTmp = contractNameFinder(textMediumSnipStrip);
//			NLP.printListOfStringArray("listKNameAlgoTmp===", listKNameAlgoTmp);
//			System.out.println("contractName finder contractNameFinder.size==" + listKNameAlgoTmp.size());
			if (listKNameAlgoTmp.size() > 0) {
				contractNameAlgo = listKNameAlgoTmp.get(0)[0] + "\r\nscore=" + listKNameAlgoTmp.get(0)[4];
				System.out.println("contractnamealgo=" + contractNameAlgo);
			}

//			System.out.println("a.contractNameAlgo==" + contractNameAlgo);

			if (!parseClientText && !disclosureFormFound && !getMetadata
					&& nlp.getAllIndexEndLocations(docFormType, listP.get(2)).size() > 0) {

				/*
				 * if docTyp=S-4 then I know that I don't want it. Versus formType=S-4
				 * (masterIdx) wherein for that formType there can be exhibits I want whereas
				 * docTyp is at the doc level. listP.get(2) are form types I don't generally
				 * want
				 */
//				System.out.println("continue - not a form I want to parse and not metadata I want="
//						+ nlp.getAllIndexEndLocations(docFormType, listP.get(2)).get(0));
				// save heading metadata here.

				fileMetadata = new File(metadataFolder + year + "/QTR" + qtr + "/" + kId + " "
						+ contractLongName.replaceAll("[^a-zA-Z ]", "") + ".txt");

				pwMeta = new PrintWriter(fileMetadata);
				pwMeta.append("kId=" + kId.replaceAll("kId=", "") + "\r\nfDate=" + fileDate + "\r\nfSize="
						+ text.length() + "\r\nacc=" + acc + "\r\ncik=" + cik + "\r\nformType=" + formType
						+ "\r\ncompanyName=" + companyName + "\r\nacc_link_filename=" + acc_link_filename
						+ "\r\nedgarLink=https://www.sec.gov/Archives/edgar/data/" + cik + "/" + acc.replaceAll("-", "")
						+ "/" + acc_link_filename + "\r\ncontractLongName=" + contractLongName + "\r\ntype="
						+ docFormType + "\r\n" + contractNameAlgo + "\r\ngoverningLaw=" + governLaw);
				pwMeta.close();

//				if (contractNameAlgo.length() == 0) {
//					System.out.println("1.contractnamealgo=0 so delete");
//					fileMetadata.delete();
//				}

				System.out.println("1pwMeta.continue...gk3. kId=" + kId);
				continue;
			}

			if (nlp.getAllIndexEndLocations(contractLongName, patternContractsFinancial).size() > 0) {
				isFinancialContract = true;
			}

			if (parseClientText && !clientTextIsDisclosure)
				isFinancialContract = true;
//			System.out.println("isFinancialContract==" + isFinancialContract);
			nonStrippedText = text;
			// add conditions of file size based on what is in listParseThese
			// if<html> do a factor of 1.5x and if not then 1x

			txtLen = text.length();
//			System.out.println("aa text.len=" + txtLen);

			smallFile = gl.populateGlobalIntegerVariables().get(0);
			largeFile = gl.populateGlobalIntegerVariables().get(1);
			// <==== before I have contractType for "C"
			if (!parseClientText && (txtLen < smallFile || txtLen > largeFile) && !isFinancialContract) {
//				System.out.println("continue. Too big or small file - so skip <DOCUMENT> -- fileSize=" + txtLen
//						+ "  contractLongName=" + contractLongName);
				// save heading metadata here.

				fileMetadata = new File(metadataFolder + year + "/QTR" + qtr + "/" + kId + " "
						+ contractLongName.replaceAll("[^a-zA-Z ]", "") + ".txt");

				pwMeta = new PrintWriter(fileMetadata);

				pwMeta.append("kId=" + kId.replaceAll("kId=", "") + "\r\nfDate=" + fileDate + "\r\nfSize="
						+ text.length() + "\r\nacc=" + acc + "\r\ncik=" + cik + "\r\nformType=" + formType
						+ "\r\ncompanyName=" + companyName + "\r\nacc_link_filename=" + acc_link_filename
						+ "\r\nedgarLink=https://www.sec.gov/Archives/edgar/data/" + cik + "/" + acc.replaceAll("-", "")
						+ "/" + acc_link_filename + "\r\ncontractLongName=" + contractLongName + "\r\ntype="
						+ docFormType + "\r\n" + contractNameAlgo + "\r\ngoverningLaw=" + governLaw);
				pwMeta.close();

//				if (contractNameAlgo.length() == 0) {
//					System.out.println("2.contractnamealgo=0 so delete");
//					fileMetadata.delete();
//				}

				System.out.println("4.pwMeta. save meta continue..kId=" + kId);
				continue;
			}

			largeFileKfile = gl.populateGlobalIntegerVariables().get(3);
//			System.out.println("largeFileKfile="+largeFileKfile);
			if (!parseClientText && txtLen > largeFileKfile && !isFinancialContract) {
//				System.out.println("this is a contract that's too big. continue. fileSize=" + txtLen);
				// save heading metadata here.

				fileMetadata = new File(metadataFolder + year + "/QTR" + qtr + "/" + kId + " "
						+ contractLongName.replaceAll("[^a-zA-Z ]", "") + ".txt");

				pwMeta = new PrintWriter(fileMetadata);
				pwMeta.append("kId=" + kId.replaceAll("kId=", "") + "\r\nfDate=" + fileDate + "\r\nfSize="
						+ text.length() + "\r\nacc=" + acc + "\r\ncik=" + cik + "\r\nformType=" + formType
						+ "\r\ncompanyName=" + companyName + "\r\nacc_link_filename=" + acc_link_filename
						+ "\r\nedgarLink=https://www.sec.gov/Archives/edgar/data/" + cik + "/" + acc.replaceAll("-", "")
						+ "/" + acc_link_filename + "\r\ncontractLongName=" + contractLongName + "\r\ntype="
						+ docFormType + "\r\n" + contractNameAlgo + "\r\ngoverningLaw=" + governLaw);
				pwMeta.close();

//				if (contractNameAlgo.length() == 0) {
//					System.out.println("3.contractnamealgo=0 so delete");
//					fileMetadata.delete();
//				}

				System.out.println("5.pwMetagk5. save meta. lg file. continue..kId=" + kId);
				continue;
			}

			List<String> listParseThese = new ArrayList<String>();
			hasGoverningLaw = false;

			/*
			 * if it isn't a disclosure item being parsed, and MapParseThese containsKey="C"
			 * it is looking for a contract.
			 */

//			System.out.println("hasGoverningLaw fetch");
			hasGoverningLaw = itHasGoverningLaw(text);
			System.out.println("kId=" + kId + " hasGoverningLaw=" + hasGoverningLaw);

//			System.out.println("hasGoverningLaw==" + hasGoverningLaw);
			if (!hasGoverningLaw && !parsingDisclosure && !parseClientText && !isFinancialContract) {
				// save heading metadata here.
//				System.out.println("text.substring="+text.substring(0,1000));
				System.out.println("save meta gk6 continue..");

				fileMetadata = new File(metadataFolder + year + "/QTR" + qtr + "/" + kId + " "
						+ contractLongName.replaceAll("[^a-zA-Z ]", "") + ".txt");

				openingParagraph = gl
						.stripHtml(
								text.substring(0, Math.max(Math.min(25000, text.length()), (int) (text.length() * .2))))
						.replaceAll("[\r\n\t]", " ").replaceAll("[ ]+", " ").trim();
				openingParagraph = openingParagraph.substring(0, Math.min(openingParagraph.length(), 2000));

				pwMeta = new PrintWriter(fileMetadata);
				pwMeta.append("kId=" + kId + "\r\nfDate=" + fileDate + "\r\nfSize=" + text.length() + "\r\nacc=" + acc
						+ "\r\ncik=" + cik + "\r\nformType=" + formType + "\r\ncompanyName=" + companyName
						+ "\r\nacc_link_filename=" + acc_link_filename
						+ "\r\nedgarLink=https://www.sec.gov/Archives/edgar/data/" + cik + "/" + acc.replaceAll("-", "")
						+ "/" + acc_link_filename + "\r\ncontractLongName=" + contractLongName + "\r\ntype="
						+ docFormType + "\r\n" + contractNameAlgo + "\r\nopeningParagraph=" + openingParagraph
						+ "\r\ngoverningLaw=" + governLaw);

//				System.out.println("edgarLink=" + "https://www.sec.gov/Archives/edgar/data/" + cik + "/"
//						+ acc.replaceAll("-", "") + "/" + acc_link_filename + "\r\n"
//						+ "no governing law, contractLongName is not a financial contract type, and it is not client text dummy nopeningParagraph==\r\n"
//						+ openingParagraph + "|END");

				// System.out.println("kId=" + kId + "\r\nfDate=" + fileDate + "\r\nfSize=" +
				// text.length() + "\r\nacc=" + acc
				// + "\r\ncik=" + cik + "\r\nformType=" + formType + "\r\ncompanyName=" +
				// companyName
				// + "\r\nacc_link_filename=" + acc_link_filename
				// + "\r\nedgarLink=https://www.sec.gov/Archives/edgar/data/" + cik + "/" +
				// acc.replaceAll("-", "")
				// + "/" + acc_link_filename + "\r\ncontractLongName=" + contractLongName +
				// "\r\ntype="
				// + docFormType + "\r\n" + contractNameAlgo + "\r\nopeningParagraph=dummy" +
				// openingParagraph);

				pwMeta.close();
				System.out.println("6..pwMeta metadata");
//				if (contractNameAlgo.length() == 0) {
//					fileMetadata.delete();
//				}

				continue;
			}

			if (parseSuperFast && !parsingDisclosure && !parseClientText && mapParseThese.containsKey("C")) {
				listParseThese = mapParseThese.get("C");
//				System.out.println("it is parsingSuperFast, not disclosure, not client doc and it is a contract. save meta and continue..");
//				System.out.println("before parseSuperFast. listParseThese.size=" + listParseThese.size());
				listParseThese = parseSuperFast(listParseThese, nonStrippedText, contractLongName);
//				System.out.println("after parseSuperFast. listParseThese.size=" + listParseThese.size());
				if (listParseThese.size() == 0) {
					// save heading metadata here.
					fileMetadata = new File(metadataFolder + year + "/QTR" + qtr + "/" + kId + " "
							+ contractLongName.replaceAll("[^a-zA-Z ]", "") + ".txt");

					pwMeta = new PrintWriter(fileMetadata);

					pwMeta.append("kId=" + kId.replaceAll("kId=", "") + "\r\nfDate=" + fileDate + "\r\nfSize="
							+ text.length() + "\r\nacc=" + acc + "\r\ncik=" + cik + "\r\nformType=" + formType
							+ "\r\ncompanyName=" + companyName + "\r\nacc_link_filename=" + acc_link_filename
							+ "\r\nedgarLink=https://www.sec.gov/Archives/edgar/data/" + cik + "/"
							+ acc.replaceAll("-", "") + "/" + acc_link_filename + "\r\ncontractLongName="
							+ contractLongName + "\r\ntype=" + docFormType + "\r\n" + contractNameAlgo
							+ "\r\ngoverningLaw=" + governLaw);
					pwMeta.close();
//					if (contractNameAlgo.length() == 0) {
//						System.out.println("4.contractnamealgo=0 so delete");
//						fileMetadata.delete();
//					}

					continue;
				}
				System.out.println("7..pwMeta meta gk7 continue..");
//				System.out.println("continue. - listParseThese.get(0)=" + listParseThese.get(0));
			}
//			contractNameAlgo = "";
//			System.out.println("filename==" + acc_link_filename + " description=" + description + " contractLongName="
//					+ contractLongName);

//			System.out.println("removing GobblyGook. text.len=" + text.length());
			// NOTE: I have already embedded gobblygood method in GC_tester. Is this needed?
			if (GetContracts.turnOnPrinterWriter)
				Utils.writeTextToFile(new File("c:/temp/removeGobblyGookBef.html"), text);
//			startTime = System.currentTimeMillis();
			txtLen = text.length();

			// check at prepText to see if previously stripped and if so read text from
			// folder which is same as below text = utils.readText....
			text = prepText(text);// startTime = System.currentTimeMillis();
//			strippedFolder_no_meta

			// set boolean so File create and pw is only if file already exists
			FileSystemUtils.createFoldersIfReqd(strippedFolder_no_meta + year + "/QTR" + qtr + "/"
					+ contractType.replaceAll("contractType=", "") + "/");
			File strippedFolder_no_meta_file = new File(
					strippedFolder_no_meta + year + "/QTR" + qtr + "/" + contractType.replaceAll("contractType=", "")
							+ "/" + kId + " " + contractType.replaceAll("contractType=", "") + " " + contractTypeInBody
							+ contractLongName.replaceAll("\\?", "") + ".txt");

			PrintWriter pwStrippedFolder_no_meta = new PrintWriter(strippedFolder_no_meta_file);
			pwStrippedFolder_no_meta.append(text);
			pwStrippedFolder_no_meta.close();

			/*
			 * AFTER THIS POINT - I CANNOT MAKE ANY CHANGES TO TEXT! Text length must be
			 * immutable!!!
			 */

//			elapsedTime = System.currentTimeMillis() - startTime;
//			System.out.println("jSoup strip html time: " + elapsedTime / 1000 + " seconds");

//			strippedText = ContractParser.stripHtmlTags(text);
//			strippedText = stripTxt();

			String strippedText = text;
			governLaw = "";
			governLaw = GoLaw.getGoverningLaw(strippedText);
			System.out.println("2 kId=" + kId + " ...governLaw is=" + governLaw);
			// filesize must be at least X

			if (!parsingDisclosure && !parseClientText && !isFinancialContract
					&& strippedText.length() < gl.populateGlobalIntegerVariables().get(0)) {
//				System.out.println("contract - stripped file size is too small. size=" + strippedText.length());
				System.out.println("kId=" + kId + "already stripped, but not saved.. gk8 continue..");
				continue;
			}

			// filesize must be at least X
//			if (!parseClientText && parsingDisclosure
//					&& strippedText.length() < gl.populateGlobalIntegerVariables().get(4)) {
//				System.out.println("disclosure- stripped file size is too small. size=" + strippedText.length());
//				continue;
//			}

			txtLen = strippedText.length();
			firstPartOfK = strippedText.substring(0, (int) ((double) txtLen * kTypAmt));
//			System.out.println("firstPartOfK ===" + firstPartOfK.length() + " txtLen=" + txtLen);
//			startTime = System.currentTimeMillis();

//			System.out.println("parseClientText==" + parseClientText+" parsingDisclosure="+parsingDisclosure);
			if (!parsingDisclosure
//					&& !parseClientText
			) {

//				System.out.println("parsing a contract. now looking for contract name using algo.");
				// already determined attributes to parse superfast were present or not. But
				// that doesn't mean if I set parse super fast to false I don't want to check
				// attributes at stripped text level

				listDated = nlp.getAllMatchedGroupsAndEndIdxLocs(firstPartOfK, gK.patternDated);
				listBetweenAmong = nlp.getAllMatchedGroupsAndEndIdxLocs(firstPartOfK, gK.patternBetweenAmong);

//				System.out.println("2 textKtype.len=" + firstPartOfK.length());
				listOfContractNamesFromAlgo = contractNameFinder(firstPartOfK);
//				System.out.println("1. contractNameFinder - listOfContractNamesFromAlgo.size=="
//						+ listOfContractNamesFromAlgo.size());
//				NLP.printListOfStringArray("listOfContractNamesFromAlgo==", listOfContractNamesFromAlgo);

				listParseThese = mapParseThese.get("C");// this is resetting but should be okay.
				// each string in list is formatted as follows and in this order
				listContractAttrbutes = isItaContractIwantToParse(listParseThese, listOfContractNamesFromAlgo,
						"contractLongName", contractLongName, parsingDisclosure);

				// TODO: if isItaContractIwantToParse - then parse it. I should rely on
				// contractLongName && contractNameAlgo

//				System.out.println("1. listContractAttrbutes.size==" + listContractAttrbutes.size());
				// if based on contractLongName the condition is met I parse.
//				int d = 0;
				if (listContractAttrbutes.size() == 0 && listOfContractNamesFromAlgo.size() > 0) {
//					System.out.println("isItaContractIwantToParse using contractNameAlgo");
					// there is a high confidence at first contract name found w/ contract finder.
					listContractAttrbutes = isItaContractIwantToParse(listParseThese, listOfContractNamesFromAlgo,
							"contractName", listOfContractNamesFromAlgo.get(0)[0], parsingDisclosure);
//					System.out.println(
//							"listContractAttrbutes found? listContractAttrbutes.size=" + listContractAttrbutes.size());
				}

				if (listContractAttrbutes.size() > 0) {
					parse = true;
//					System.out.println("3. parse===" + parse);
					solrCore = listContractAttrbutes.get(1).replaceAll("solrCore=", "");
					contractType = listContractAttrbutes.get(2).replaceAll("contractType=", "");
					if (contractNameAlgo.length() == 0 && listOfContractNamesFromAlgo.size() > 0) {
						contractNameAlgo = listOfContractNamesFromAlgo.get(0)[0] + " cnt="
								+ listOfContractNamesFromAlgo.get(0)[1] + " idx="
								+ listOfContractNamesFromAlgo.get(0)[2] + " %locInDoc="
								+ listOfContractNamesFromAlgo.get(0)[3] + " score="
								+ listOfContractNamesFromAlgo.get(0)[4];
//						System.out.println("b.contractNameAlgo=" + contractNameAlgo);
					}
//					System.out.println("2.contractType=" + contractType);
					if (listContractAttrbutes.size() > 3) {
						contractLongNameHasKeyWords = listContractAttrbutes.get(3);
					}
				}

//				System.out.println("b1.contractNameAlgo=" + contractNameAlgo);

				if (listContractAttrbutes.size() == 0) {
					listOfContractNamesFromAlgo = contractNameLotto(
							strippedText.substring(0, ((int) ((double) strippedText.length() * .3))));
//					NLP.printListOfStringArray("conctract lotto names algo==", listOfContractNamesFromAlgo);
					if (listOfContractNamesFromAlgo.size() > 0) {
						contractNameAlgo = listOfContractNamesFromAlgo.get(0)[0] + " cnt="
								+ listOfContractNamesFromAlgo.get(0)[1] + " idx="
								+ listOfContractNamesFromAlgo.get(0)[2] + " %locInDoc="
								+ listOfContractNamesFromAlgo.get(0)[3] + " score="
								+ listOfContractNamesFromAlgo.get(0)[4];
//						System.out.println("c.contractNameAlgo=" + contractNameAlgo);
					}
				}

				if (!parse) {
					listParseThese = mapParseThese.get("C");
//					solrCore = listParseThese.get(0).replaceAll("solrCore=", "");
//					System.out.println("contractType=" + contractType);
//					System.out.println("3.!parse contractType=" + contractType);
					contractType = "";
				}

			}

			System.out.println("kId=" + kId + " C. listContractAttrbutes.size==" + listContractAttrbutes.size()
					+ " parse=" + parse + " parseClientText=" + parseClientText + " getMetadata=" + getMetadata);
//			NLP.printListOfStringArray("listOfContractNamesFromAlgo==", listOfContractNamesFromAlgo);

			if (!parse && !parseClientText && !getMetadata && !isFinancialContract) {
				// save heading metadata here.
//				System.out.println("3 continue. It hasn't found D or C and I don't want to parse metadata.");
				System.out.println("kId=" + kId + " already stripped, but not saved.. gk9 continue..");
				continue;
			}

			// set contractType based on what I specified in mapParseThese

//			System.out.println("acc=" + acc + " parsingDisclosure=" + parsingDisclosure
//					+ " if not parsing disclosure - was contratctName found?=" + parse + " listDated.size="
//					+ listDated.size() + " listBetweenAmong.size=" + listBetweenAmong.size() + " contractNameAlgo="
//					+ contractNameAlgo + " contractType=" + contractType);

			List<String> listOp = new ArrayList<String>();
//			if (!parseClientText) {

			listOp = getOpeningParagraphAttributes(listDated, listBetweenAmong, firstPartOfK, contractNameAlgo,
					contractType);
//			System.out.println("opening para list size=" + listOp.size());
//			}

			if (// !parseClientText &&
			listOp.size() == 0 && listDated.size() == 0 && listBetweenAmong.size() == 0 && !parsingDisclosure
					&& !getMetadata && !isFinancialContract) {
				// save heading metadata here.
//				System.out.println("5 continue.");
				System.out.println("kId=" + kId + " already stripped, but not saved.. gk10 continue..");
				continue;
			}

			if (!parsingDisclosure
//					&& !parseClientText
			) {

				if (listOp.size() > 0) {
					for (int x = 0; x < listOp.size(); x++) {
						openingParagraphItems = openingParagraphItems + listOp.get(x) + "|";
						if (listOp.get(x).contains("openingParagraph=")) {
							openingParagraph = listOp.get(x);
//						System.out.println("L1 - openingPara=" + openingParagraph);
						}
						if (listOp.get(x).contains("openParaContractName=")) {
							openParaContractName = listOp.get(x);
							System.out.println("L1 - openParaContractName=" + openParaContractName);
						}

						if (listOp.get(x).contains("contractingParty")) {
							openingParaContractingParties = listOp.get(x);
//						System.out.println("L1 - contractingParty=" + openingParaContractingParties);
						}

						if (listOp.get(x).contains("contractingPartyRole")) {
							openingParaContractingPartyRole = listOp.get(x);
//						System.out.println("L1 - contractingPartyRole=" + openingParagraph);
						}

						if (listOp.get(x).contains("contractingRole")) {
							openingParaContractingRole = listOp.get(x);
//						System.out.println("L1 - contractingRole=" + openingParagraph);
						}

//						System.out.println("??opening paragraph ??items=" + listOp.get(x));

					}
				}
			}

			TreeMap<Integer, String> leMap = new TreeMap<Integer, String>();
			if (parseClientText) {
				leTxt = strippedText.substring(0, Math.min(100000, strippedText.length()));
				leMap = EntityRecognition.getLegalEntities(leTxt, true);
				// getLegalEntities returns idx location (this shows which are found 1st which
				// shows which are primary).

				for (Map.Entry<Integer, String> entry : leMap.entrySet()) {
					if (leCnt + 1 < leMap.size()) {
						legalEntities = legalEntities.replaceAll("Qx|XQ", "") + "sIdx=" + entry.getKey() + " cnt="
								+ leCnt + " " + entry.getValue() + "|";
					} else {
						legalEntities = legalEntities.replaceAll("Qx|XQ", "") + "sIdx=" + entry.getKey() + " cnt="
								+ leCnt + " " + entry.getValue();
					}
					leCnt++;
				}

				if (parseClientText && !clientTextIsDisclosure) {
//					PrintWriter pw = new PrintWriter(new File("c:/temp/12. parseclient text.txt"));
//					pw.append("contractNameAlgo="+contractNameAlgo+"\r\n\r\n"+strippedText);
//					pw.close();
					// client metadata.
//					NLP.printMapIntStr("legalEntitiesMap", leMap);
//					NLP.printListOfString("listOp==", listOp);
					governLaw = GoLaw.getGoverningLaw(strippedText);
//					System.out.println("op==" + openingParagraph);
//					System.out.println("clientText governing law==" + governLaw);
//					System.out.println("d.parse client text contractNameAlgo==" + contractNameAlgo);
					TreeMap<Integer, String> leoPMap = new TreeMap<Integer, String>();
					leoPMap = EntityRecognition.getLegalEntities(openingParagraph, true);
//					NLP.printMapIntStr("legalEntitie-contracting parties", leoPMap);
					if (listOp.size() > 0) {
						for (int x = 0; x < listOp.size(); x++) {
							if (listOp.get(x).contains("openingParagraph=")) {
								openingParagraph = listOp.get(x);
//								System.out.println("L1 - openingPara=" + openingParagraph);
							}
							if (listOp.get(x).contains("openParaContractName=")) {
								openParaContractName = listOp.get(x);
//								System.out.println("L1 - openParaContractName=" + openingParagraph);
							}
						}
					}
				}
			}
			metadataList2 = new ArrayList<String>();
			// System.out.println("legal entities found seconds=" +
			// (System.currentTimeMillis() - startLe) / 1000);
//			System.out.println("3 legalEntities end?");
			if (parseClientText) {
				// if parsing client text - create metadata file here.
				// filename is name of file on drive, kid is filename up to first white space
				// assumes 1 doc per txt file is parsin client text. but if parsing sample .nc
				// files it can be many hence listDocuments.size=1

				if (nlp.getAllMatchedGroups(contractNameAlgo, Pattern.compile("score=[\\d]+")).size() > 0) {
					GoLaw.clientScore = nlp.getAllMatchedGroups(contractNameAlgo, Pattern.compile("score=[\\d]+"))
							.get(0);
				}

				if (nlp.getAllMatchedGroups(contractNameAlgo, Pattern.compile("")).size() == 0) {
					GoLaw.clientScore = "";
				}

				if (nlp.getAllMatchedGroups(contractNameAlgo, Pattern.compile("cnt=")).size() > 0) {
					contractNameAlgo = DisplayNames.makeContractNamesInitialCaps(
							contractNameAlgo.substring(0, contractNameAlgo.indexOf("cnt=")));
				}

				if (nlp.getAllMatchedGroups(contractNameAlgo, Pattern.compile("cnt=")).size() == 0
						&& contractNameAlgo.length() < 3) {
					contractNameAlgo = DisplayNames.makeContractNamesInitialCaps(GetClientDocs.clientContractType);
				}

//				System.out.println("kId=" + kId + "\r\nacc=" + acc + "\r\nacc_link_filename=" + acc_link_filename
//						+ "\r\nfSize=" + strippedText.length() + "\r\ncontractNameAlgo=" + contractNameAlgo
//						+ "\r\ngLaw=" + governLaw + "\r\nopenParaContractName" + openParaContractName
//						+ openingParagraph.trim() + "\r\nscore=" + GoLaw.clientScore);

				metadataList2.add("kId=" + kId);
				metadataList2.add("fDate=" + fileDate);
				metadataList2.add("acc=" + acc);
				metadataList2.add("gLaw=" + governLaw);

				if (contractNameAlgo.length() > 2) {
					metadataList2.add("contractNameOk=1");
				}

				if (contractNameAlgo.length() < 3) {
					contractNameAlgo = contractLongName.replaceAll(kId, "");
					metadataList2.add("contractNameOk=0");
				}

				contractLongName = contractLongName.replaceAll("(?ism)\\.txt", "").trim();

				if (nlp.getAllMatchedGroups(contractNameAlgo, Pattern.compile("(?ism).*?(?=cnt=)")).size() > 0) {
					contractNameAlgo = nlp.getAllMatchedGroups(contractNameAlgo, Pattern.compile("(?ism).*?(?=cnt=)"))
							.get(0);
				}

				if (nlp.getAllMatchedGroups(contractNameAlgo, Pattern.compile("(?ism).*?(?=\r\nscore)")).size() > 0) {
					contractNameAlgo = nlp
							.getAllMatchedGroups(contractNameAlgo, Pattern.compile("(?ism).*?(?=\r\nscore)")).get(0);
				}

//				System.out.println("e.final client text contractNameAlgo="+contractNameAlgo);
				metadataList2.add("contractNameAlgo=" + contractNameAlgo);
				metadataList2.add("fSize=" + strippedText.length());
				metadataList2.add("gLaw=" + governLaw);
				metadataList2.add("acc_link_filename=" + acc_link_filename);
				metadataList2.add("" + openingParagraph.trim());
//				System.out.println("openParaContractName="+openParaContractName);
				metadataList2.add("openParaContractName="
						+ openParaContractName.trim().replaceAll("openParaContractName=", "").trim());
				metadataList2.add("parentGroup=" + GetClientDocs.clientParentGroup);
				metadataList2.add("contractType=" + GetClientDocs.clientContractType);
				metadataList2.add("contractLongName=" + contractLongName);
//				
//				if(nlp.getAllMatchedGroups(acc_link_filename, Pattern.compile("(?ism)amend|restated|a ?& ?r")).size()>0){
//					modify="modify=Amend";
//				}
//				metadataList2.add("modify=" + modify);
			}

			System.out.println("333.kId=" + kId);
			if (!parseClientText) {
				System.out.println("3333.kId=" + kId);
				edgarLink = "https://www.sec.gov/Archives/edgar/data/" + cik + "/" + acc.replaceAll("-", "") + "/"
						+ acc_link_filename;

				numberOfLegalEntities = legalEntities.split("\\|").length;

//			metadata = "fSize=" + txtLen + "||cik=" + cik + "||fDate=" + fileDate + "||company=" + companyName
//					+ "||acc_link_filename=" + acc_link_filename + "||type=" + type + "||contractType="
//					+ contractType.replaceAll("contractType=", "") + "_" + "||contractLongName=" + contractLongName
//					+ "||acc=" + acc + "||formType=" + formType + "||contractTypeInBody=" + inBody
//					+ "||contractNameAlgo=" + contractNameAlgo + "||allConractPatternsInBody="
//					+ allConractPatternsInBody + "||legalEntities=" + legalEntities + "||numberOfLegalEntities="
//					+ numberOfLegalEntities + "||" + openingParagraphItems + "||edgarLink=" + edgarLink + "\r\n";
//			metadata = metadata.replaceAll("\\|(\r\n)?\\|(\r\n)?\\|(\r\n)?\\|?(\r\n)?", "\\|\\|")
//					.replaceAll("\\|\\|", "\\|\\|\r\n").replaceAll("\r\n\\|\\|", "\\|\\|").replaceAll("xxPD", "\\.")
//					.replaceAll("^\\|", "");

//			System.out.println("kId=" + kId + " parsing this file longname="
//					+ nlp.getAllMatchedGroups(textSnip, patternDescription).get(0));

				metadataList.add("kId=" + kId.replaceAll("kId=", "").trim());
				metadataList.add("fDate=" + fileDate);
//			System.out.println("fDate=" + fileDate);
				metadataList.add("fSize=" + txtLen);
				metadataList.add("acc=" + acc);
				metadataList.add("cik=" + cik);
				metadataList.add("formType=" + formType);
				metadataList.add("companyName=" + companyName);
				metadataList.add("acc_link_filename=" + acc_link_filename);
				metadataList.add("edgarLink=" + edgarLink);
				metadataList.add(
						"contractLongName=" + contractLongName.replaceAll("\\\\", " ").replaceAll("[ ]+", " ").trim());
				metadataList.add("contractType=" + contractType.replaceAll("contractType=", ""));
//			System.out.println("4.contractType=" + contractType);
				metadataList.add("type=" + docFormType);
				// <===edgar data (non-algo data)
				metadataList.add("contractNameAlgo=" + DisplayNames.makeContractNamesInitialCaps(
						contractNameAlgo.replaceAll("\\|(\r\n)?\\|(\r\n)?\\|(\r\n)?\\|?(\r\n)?", "\\|\\|")
								.replaceAll("\\|\\|", "\\|\\|\r\n").replaceAll("\r\n\\|\\|", "\\|\\|")
								.replaceAll("xxPD", "\\.").replaceAll("\\\\", " - ").replaceAll("[ ]+", " ")));
				metadataList.add("contractTypeInBody=" + inBody);
				metadataList.add("allConractPatternsInBody=" + allConractPatternsInBody);
				metadataList.add("legalEntities="
						+ legalEntities.replaceAll("\\|(\r\n)?\\|(\r\n)?\\|(\r\n)?\\|?(\r\n)?", "\\|\\|")
								.replaceAll("\\|\\|", "\\|\\|\r\n").replaceAll("\r\n\\|\\|", "\\|\\|")
								.replaceAll("xxPD", "\\."));
				metadataList.add("numberOfLegalEntities=" + numberOfLegalEntities);
//				metadataList.add("quotedTerms=" + quotedTerms);

//			NLP.printListOfString("metadataList====", metadataList);
//			System.out.println("qtr=" + qtr);

//			System.out.println("@getMetadata - contractType=" + contractType + " solrCore=" + solrCore);
//			if (getMetadata) {//always get metadata for doc meeting contractType sought to be arsed into solr
				// set && !parsingDisclosure to not get metadata here if I don't want it for
				// disclosure
//			System.out.println("getting metadata");
				// records metadata for later use to aggregate by contract name in mysql in
				// order to determine primary contracts (large pools)

				metadataList = prepareMetadataList(metadataList, listOp, listOfContractNamesFromAlgo,
						openParaContractName, openingParagraph, leMap, openingParaContractingPartyRole,
						openingParaContractingRole, openingParaContractingParties, contractLongNameHasKeyWords);

				TreeMap<String, String> map = new TreeMap<String, String>();

				for (int z = 0; z < metadataList.size(); z++) {
					if (metadataList.get(z).trim().length() < 5)
						continue;
					if (map.containsKey(metadataList.get(z).replaceAll("contractNameAlgo_Top=", "contractNameAlgo=")
							.replaceAll("[^A-Za-z]", "")))
						continue;
					map.put(metadataList.get(z).replaceAll("contractNameAlgo_Top=", "contractNameAlgo=")
							.replaceAll("[^A-Za-z]", ""), "");

					metadataList2.add(metadataList.get(z));
					meta = meta + (metadataList.get(z) + "\n");
				}

				if (!meta.contains("openingParagraph=")) {
					openingParagraph = gl
							.stripHtml(text.substring(0,
									Math.max(Math.min(20000, text.length()), (int) (text.length() * .1))))
							.replaceAll("[\r\n\t]", " ").replaceAll("[ ]+", " ").trim();
					openingParagraph = openingParagraph.substring(0, Math.min(openingParagraph.length(), 2000));
					meta = meta + "\r\n" + openingParagraph;
				}

//				System.out.println("saveing meta - governLaw=" + governLaw);
//				if (parseClientText) {
//					contractLongName = kId + " " + contractLongName.replaceAll("\\?", "") + ".txt";
//				}

				fileMetadata = new File(metadataFolder + year + "/QTR" + qtr + "/" + kId + " "
						+ contractLongName.replaceAll("[^a-zA-Z ]", "") + ".txt");

				pwMeta = new PrintWriter(fileMetadata);

				System.out.println("kId=" + kId + " -- 8.pwMeta. filename==" + fileMetadata.getAbsolutePath());
				pwMeta.append(meta.replaceAll("[\n]+", "\n").replaceAll("contractNameAlgo_Top=", "contractNameAlgo=")
						.replaceAll("(?<=[\r\n]).*?=[\r\n]+", "").replaceAll("[\r\n]+", "\r\n") + "\r\ngoverningLaw="
						+ governLaw);
				pwMeta.close();
			}

			// now run pattern against meta data and if conditions are met or not parse.

			// TODO: here the metadata is complete. Apply contractTypeToParse(metadataList2)
			// method to determine if the contract is the type I want to push to solr. If
			// not continue. zzzz

			if (!parse && !parseClientText && !GoLaw.forceParse && !parsingDisclosure) {
//				System.out.println("6 continue. Only parsed metadata");
				System.out.println("kId=" + kId + " already stripped, but not saved.. gk11 continue..");
				continue;
			}

			FileSystemUtils.createFoldersIfReqd(
					downloadFolder + year + "/QTR" + qtr + "/" + contractType.replaceAll("contractType=", "") + "/");
			contractLongName = contractLongName.replaceAll("\\?", "");
			contractLongName = contractLongName.substring(0, Math.min(contractLongName.length(), 175));
//			System.out.println("contractLongName=" + contractLongName);
//			System.out.println("5.contractType=" + contractType);
//			String kIdtmp = "";[[[
//			if (!parseClientText)
//				kIdtmp = acc + "_" + c;
//			if (parseClientText && nlp.getAllMatchedGroups(f.getName(), Pattern.compile("(?ism)" + kId)).size() == 0) {
//				kIdtmp = kId;
//			}

			System.out.println("meta==" + meta);
			System.out.println("gk12.save html to downloadFolder and txt to strippedFolder. kid=" + kId);

			File file = new File(
					downloadFolder + year + "/QTR" + qtr + "/" + contractType.replaceAll("contractType=", "") + "/"
							+ kId + " " + contractType.replaceAll("contractType=", "") + " " + contractTypeInBody
							+ contractLongName.replaceAll("\\?", "") + ".txt");
			File fileSolr = new File(
					solrDocsFolder + year + "/QTR" + qtr + "/" + contractType.replaceAll("contractType=", "") + "/"
							+ kId + " " + contractType.replaceAll("contractType=", "") + " " + contractTypeInBody
							+ contractLongName.replaceAll("\\?", "") + ".json");

			FileSystemUtils.createFoldersIfReqd(
					solrDocsFolder + year + "/QTR" + qtr + "/" + contractType.replaceAll("contractType=", "") + "/");

			FileSystemUtils.createFoldersIfReqd(
					strippedFolder + year + "/QTR" + qtr + "/" + contractType.replaceAll("contractType=", "") + "/");
			File fileStripped = new File(
					strippedFolder + year + "/QTR" + qtr + "/" + contractType.replaceAll("contractType=", "") + "/"
							+ kId + " " + contractType.replaceAll("contractType=", "") + " " + contractTypeInBody
							+ contractLongName.replaceAll("\\?", "") + ".txt");

			PrintWriter pwDownloaded = new PrintWriter(file);
			PrintWriter pwStripped = new PrintWriter(fileStripped);

			PrintWriter pwFilesParsed = new PrintWriter(filesParsed);
			pwFilesParsed.append(acc);
			pwFilesParsed.close();

			pwDownloaded.append(meta);
			// Need to use acc_link_filename to link directly to doc opening
			// this is recording meta data we want. When the parser is done with this
			// initial phase - it will save the accno.nc file to the hard drive and record
			// in it at the first line this metadata.

			pwDownloaded.append(nonStrippedText);
			pwDownloaded.close();

			pwStripped.append(meta + strippedText);
			pwStripped.close();

			filesParsed = new File(filesAlreadyParsed + acc + "_" + c + ".txt");
			PrintWriter pwParsed = new PrintWriter(filesParsed);
			pwParsed.append(acc + "_" + c);
			pwParsed.close();

//			System.out.println("stripped file=" + fileStripped.getAbsolutePath());

//			endTime = System.currentTimeMillis();
//			System.out.println("getContract seconds=" + (System.currentTimeMillis() - startTime) / 1000);

//			NLP.printListOfString("metadataList2=", metadataList2);
//			System.out.println("document#" + c);

//			System.out.println("gettingSolrFileReady");
//			if (StringUtils.isNotBlank(solrCore)) {
//				if (StringUtils.isNotBlank(getBaseSolrUrl()))
//					gl.setBaseSolrUrl(getBaseSolrUrl());
//				gl.setSolrCore2PushInto(solrCore);
//				gl.pushTosolr = true;
//			}

			System.out.println("1.gl.gettingSolrFileReady(");
			json = gl.gettingSolrFileReady(strippedText, fileSolr, metadataList2, parsingDisclosure, solrCore,
					parseClientText);
//			System.out.println("next loop=" + (c + 1));

		}

		return json;
	}

	public void parseFromMeta(String filePath, boolean regenerateJson, TreeMap<String, List<String[]>> mapAccKids,
			String acc, boolean isClientText) throws IOException, SQLException, SolrServerException {

		System.out.println(
				"\r\n\r\n\r\n****************PARSING FROM METADATA - JUST LIKE GETCLIENTDOC*******************\r\n\r\n");
		// this is not reparsing everything that is done in getContract() - but instead
		// working with metadata already gathered and just parsing each kId previously
		// designated from metadatan. This
		// only parses contracts (not disclosure) and once kId is matched it is parsed
		// w/o condition.

		GoLaw gl = new GoLaw();
		boolean foundKid = false;

//		System.out.println("getContract start");
		NLP nlp = new NLP();
//		File f = new File(filePath);

		List<String[]> listKids = new ArrayList<String[]>();
		String gLaw = "", docFormType = "", text = "", solrCore = "", nonStrippedText = "", year = "", meta = "",
				moStr = "", fileDate = "1701-01-01", cik = "1234567890", companyName = null, formType = "";
		if (isClientText)
			acc = "01234567890-23-123456";

		if (!isClientText) {
			listKids = mapAccKids.get(acc);
//		System.out.println("listKids==" + Arrays.toString(listKids.get(0)));
			fileDate = listKids.get(0)[3];
			cik = listKids.get(0)[0];
			companyName = listKids.get(0)[1];
			formType = listKids.get(0)[2];
		}

		/*
		 * this is from secMetaData which has a different order for fields than list in
		 * map
		 */

		String modify = "", contractNameOk = "", contractType = "", fSize, edgarLink = "", acc_link_filename = "",
				contractLongName = "", legalEntitiesInOpeningParagraph, numberOfLegalEntities, legalEntities,
				openingParagraph, openParaContractName, contractNameAlgo, score, patternToFindInContractBodyStr,
				parentGroup = "", ticker, sector, industry;

		if (nlp.getAllIndexEndLocations(acc,
				Pattern.compile("0000912057.{1}02.{1}010869|0000912057.{1}02.{1}010869|0000950144.{1}02.{1}003801"))
				.size() > 0)
			return;

		int qtr = 0;
//		System.out.println("acc=" + acc);

//		System.out.println("a fileDate=" + fileDate);
		year = fileDate.substring(0, 4);
//		System.out.println("b year=" + year);

		moStr = fileDate.replaceAll("-", "").substring(4, 6);
//		System.out.println("mosStr=" + moStr);
		if (moStr.substring(0, 1).equals("0")) {
			moStr = moStr.substring(1, 2);
			// //System.out.println("2mosStr="+moStr);
		}

		qtr = (Integer.parseInt(moStr) + 2) / 3;

		// if acc file is present - then don't parse this .nc file.

		Pattern patternDocument = Pattern.compile("(?ism)<DOCUMENT>.*?</DOCUMENT>");
//		Pattern patternDocType = Pattern.compile("(?sm)(?<=<TYPE>).*?$");// differs from formType in master.idx
//		Pattern patternFilename = Pattern.compile("(?sm)(?<=<FILENAME>).*?$");// edgar link filename
//		System.out.println("Look at me - I prepped all my doc patterns to help me find the documents I want! woo hoo.");

		String ncFileText = Utils.readTextFromFile(filePath);
		ncFileText = ncFileText.replaceAll("&#8221;|&#8220;|�", "\"");
		if (GetContracts.turnOnPrinterWriter) {
			PrintWriter pw = new PrintWriter(new File("c:/temp/repl quote.txt"));
			pw.append(ncFileText);
			pw.close();
		}

		if (isClientText) {
			ncFileText = "<DOCUMENT>\r\n<TEXT>\r\n<TYPE>485POS\r\n<FILENAME>123.htm</FILENAME>\r\n"

					+ "<DESCRIPTION>INDENTURE OR OFFERING SUMMARY</DESCRIPTION>\r\n" + ncFileText
					+ "\r\n</TEXT>\r\n</DOCUMENT>";
			System.out.println("this is parsing a client sentence");
		}

//		System.out.println("ncFileText.len=" + ncFileText.length());
		List<String> listDocuments = nlp.getAllMatchedGroups(ncFileText, patternDocument);

//		System.out.println("listDocuments.size()=" + listDocuments.size());

		List<String> metadataList2 = new ArrayList<String>();

		// if map size is 2 then it will have both "D" and "C", so I first check if form
		// type equals the form type in "D" and if not I know it can't be D and
		// therefore I default to "C"
//		String filesAlreadyParsed = strippedFolder + year + "/QTR" + qtr + "/_FilesParsed/";
//		Utils.createFoldersIfReqd(filesAlreadyParsed);
//		File filesParsed = new File(filesAlreadyParsed + acc + ".txt");
		// System.out.println("listDoc.size=" + listDocuments.size());

		String kId = "";
		int w = 0, tblCnt = 0;
		long startTimeGK = System.currentTimeMillis();
		long endTimeGK = System.currentTimeMillis();

		// Pattern patternToFindInContractBody = Pattern.compile("");
		double fsize = 0.0;
		String fsizeStr = fsize + "";

		for (int c = 0; c < listDocuments.size(); c++) {
//			System.out.println("at parseFromMeta");
			startTimeGK = System.currentTimeMillis();
//			System.out.println("parseFromMeta @@line 2610");
			w = 0;
			foundKid = false;
//			System.out.println("@listDoc kId=" + acc + "_" + c);
			kId = acc + "_" + c;
			if (!regenerateJson && mapOfKidsParsed.size() > 0 && mapOfKidsParsed.containsKey(kId.trim())) {
//				System.out.println("json of kId exists - no need to parse. skipping kId=" + kId);
				continue;
			}

//			System.out.println("kId from masterIdx==" + listKids.get(w)[4].trim() + " mfg kid=" + kId);
			// now I need to determine if the above kId is in the listKids - if it is not I
			// continue to the next kId (acc_c)
			for (; w < listKids.size(); w++) {
				if (kId.equals(listKids.get(w)[4].trim())) {
					kId = listKids.get(w)[4];
//					System.out.println("found kId match");
					foundKid = true;
					break;
					// check if next kId is identical-if so there are 2 contractTypes for that kId
				}
			}

			if (isClientText)
				foundKid = true;

			if (!foundKid) {
//				System.out.println("kId not found. This should not be!!!!!!!!! kId===" + kId);
				continue;
			}
			metadataList2 = new ArrayList<String>();

			if (!isClientText) {
				contractType = listKids.get(w)[5];
				fSize = listKids.get(w)[6];
				acc_link_filename = listKids.get(w)[8];
				edgarLink = listKids.get(w)[9];
				contractLongName = listKids.get(w)[10];
				docFormType = listKids.get(w)[11];
				legalEntitiesInOpeningParagraph = listKids.get(w)[12];
				numberOfLegalEntities = listKids.get(w)[13];
				openingParagraph = listKids.get(w)[14];
				openParaContractName = listKids.get(w)[15];
				contractNameAlgo = DisplayNames.makeContractNamesInitialCaps(listKids.get(w)[16]);
				legalEntities = listKids.get(w)[17];
				score = listKids.get(w)[18];
				solrCore = listKids.get(w)[19];
				patternToFindInContractBodyStr = listKids.get(w)[20];
				gLaw = DisplayNames.makeContractNamesInitialCaps(listKids.get(w)[21]);
				parentGroup = listKids.get(w)[22].replaceAll("(?i)parent_group", "parentGroup");
				ticker = listKids.get(w)[24];
				sector = listKids.get(w)[25];
				industry = listKids.get(w)[26];
				contractNameOk = listKids.get(w)[27];
				modify = listKids.get(w)[28];
				meta = "";
				meta = "kId=" + kId.replaceAll("kId=", "") + "\r\nfDate=" + fileDate.replaceAll("-", "") + "\r\nfSize="
						+ fSize + "\r\nacc=" + acc + "\r\ncik=" + cik + "\r\nformType=" + formType + "\r\ncompanyName="
						+ companyName + "\r\nacc_link_filename=" + acc_link_filename + "\r\nedgarLink=" + edgarLink
						+ "\r\ncontractLongName=" + contractLongName + "\r\ncontractType=" + contractType + "\r\ntype="
						+ docFormType + "\r\nlegalEntitiesInOpeningParagraph=" + legalEntitiesInOpeningParagraph
						+ "\r\nnumberOfLegalEntities=" + numberOfLegalEntities + "\r\nlegalEntities=" + legalEntities
						+ "\r\ncontractNameAlgo=" + contractNameAlgo + "\r\nscore=" + score
						+ "\r\nopenParaContractName=" + openParaContractName + "\r\nopeningParagraph="
						+ openingParagraph + "\r\npatternToFindInContractBody="
						+ patternToFindInContractBodyStr.replaceAll("\\|", "xOrx") + "\r\ngLaw="
						+ gLaw.replaceAll("governingLaw=", "") + "\r\nparentGroup=" + parentGroup + "\r\nticker="
						+ ticker + "\r\nsctr=" + sector + "\r\nids=" + industry + "\r\ncontractNameOk=" + contractNameOk
						+ "\r\nmodify=" + modify;

				String[] metaSplit = meta.split("\r\n");
				for (int y = 0; y < metaSplit.length; y++) {
					metadataList2.add("\r\n" + metaSplit[y]);
				}
			}
//			filesParsed = new File(filesAlreadyParsed + kId + ".txt");
//			if (!regenerate) {
//				if (filesParsed.exists())
////					System.out.println("returned!!!");
//					return;
//			}

			nonStrippedText = "";
			text = "";
			text = listDocuments.get(c);
			fsize = (double) (text.length()) / 1000000;
			fsizeStr = fsize + "";
			tblCnt = nlp.getAllIndexStartLocations(text, Pattern.compile("</table")).size();
			System.out.println("text. kid=" + kId + " fSize MBs="
					+ fsizeStr.substring(0, Math.min(fsizeStr.length(), 4)) + " tblCnt=" + tblCnt);

			if (tblCnt > 200) {
				int s_idx, e_idx, p_e_idx = 0;
				Pattern patternTable = Pattern.compile("(?ism)<TABLE.*?>.*?</TABLE>");
				List<Integer[]> listTables = nlp.getAllStartAndEndIdxLocations(text, patternTable);

				for (int i = 0; i < listTables.size(); i++) {
					s_idx = listTables.get(i)[0];
					e_idx = listTables.get(i)[1];
					if (i == 0)
						text = listDocuments.get(c).substring(0, s_idx);
					else {
						text = text + "\r\n" + listDocuments.get(c).substring(p_e_idx, s_idx);
					}
					p_e_idx = e_idx;
				}
				fsize = (double) (text.length()) / 1000000;
				fsizeStr = fsize + "";
				System.out.println("too many tables, now fSize is ths many MBs="
						+ fsizeStr.substring(0, Math.min(fsizeStr.length(), 4)) + " tblCnt=" + tblCnt);
//				PrintWriter pwTb = new PrintWriter(new File("c:/temp/too_many_tbls.html"));
//				pwTb.append(text);
//				pwTb.close();
			}

			System.out.println("fsize now=" + text.length());
//			System.out.println("1 FILESIZE========" + (text.length() / 1000) + " KBs");
			// cleanup description by removing various non-alpha characters.

			long startTime = System.currentTimeMillis();
			nonStrippedText = text;
			text = prepText(text);

//			System.out.println("joinTextOverPages seconds=" + (System.currentTimeMillis() - startTime) / 1000);
			startTime = System.currentTimeMillis();
//			if(GetContracts.turnOnPrinterWriter) Utils.writeTextToFile(new File("c:/temp/parseFromMeta--joinTextOverPages.txt"), text);
			quotedTerms = getQuotedTerms(text);
//			if (quotedTerms.length() == 0
//					&& nlp.getAllIndexEndLocations(parentGroup, Pattern.compile("Contracts")).size() > 0) {
//				System.out.println("not parsing b/c it is a contract and it has no quotedTerms");
//				return;
//			}

//			System.out.println("quotedTerms====" + quotedTerms);
			/*
			 * AFTER THIS POINT - I CANNOT MAKE ANY CHANGES TO TEXT! Text length must be
			 * immutable!!!
			 */

			String strippedText = text;
			System.out.println("text stripped.len=" + text.length());
//			patternToFindInContractBody = Pattern.compile(patternToFindInContractBodyStr);
//			if (patternToFindInContractBodyStr.trim().length() > 0
//					&& nlp.getAllIndexEndLocations(strippedText, patternToFindInContractBody).size() == 0) {
//				continue;
//			}

//			System.out.println("nlp.getAllIndexEndLocations(strippedText, patternMustBeInContractBody).size()=="
//					+ nlp.getAllIndexEndLocations(strippedText, patternMustBeInContractBody).size());
//			System.out.println("patternMustBeInContractBody==" + patternToFindInContractBody);

//			File fileNotFound = new File(
//					solrDocsFolder + year + "/QTR" + qtr + "/" + parentGroup.replaceAll("parentGroup=", "")
//							+ "_patternNotFound/" + acc + "_" + c + " " + parentGroup.replaceAll("parentGroup=", "")
//							+ " " + contractLongName.replaceAll("\\?", "") + ".json");

//			FileSystemUtils.createFoldersIfReqd(solrDocsFolder + year + "/QTR" + qtr + "/"
//					+ parentGroup.replaceAll("parentGroup=", "") + "_patternNotFound/");

			FileSystemUtils.createFoldersIfReqd(
					downloadFolder + year + "/QTR" + qtr + "/" + parentGroup.replaceAll("parentGroup=", "") + "/");
//			System.out.println("contractLongName=" + contractLongName);
//			System.out.println("5.parentGroup=" + parentGroup);
			File file = new File(downloadFolder + year + "/QTR" + qtr + "/" + parentGroup.replaceAll("parentGroup=", "")
					+ "/" + acc + "_" + c + " " + parentGroup.replaceAll("parentGroup=", "") + " "
					+ contractLongName.replaceAll("\\?", "") + ".txt");
			File fileSolr = new File(
					solrDocsFolder + year + "/QTR" + qtr + "/" + parentGroup.replaceAll("parentGroup=", "") + "/" + acc
							+ "_" + c + " " + parentGroup.replaceAll("parentGroup=", "") + " "
							+ contractLongName.replaceAll("\\?", "") + ".json");

			FileSystemUtils.createFoldersIfReqd(
					solrDocsFolder + year + "/QTR" + qtr + "/" + parentGroup.replaceAll("parentGroup=", "") + "/");

			FileSystemUtils.createFoldersIfReqd(
					strippedFolder + year + "/QTR" + qtr + "/" + parentGroup.replaceAll("parentGroup=", "") + "/");
			File fileStripped = new File(
					strippedFolder + year + "/QTR" + qtr + "/" + parentGroup.replaceAll("parentGroup=", "") + "/" + acc
							+ "_" + c + " " + parentGroup.replaceAll("parentGroup=", "") + " "
							+ contractLongName.replaceAll("\\?", "") + ".txt");

			PrintWriter pwDownloaded = new PrintWriter(file);
			PrintWriter pwStripped = new PrintWriter(fileStripped);

//			PrintWriter pwFilesParsed = new PrintWriter(filesParsed);
//			pwFilesParsed.append(acc);
//			pwFilesParsed.close();

			// Need to use acc_link_filename to link directly to doc opening
			// this is recording meta data we want. When the parser is done with this
			// initial phase - it will save the accno.nc file to the hard drive and record
			// in it at the first line this metadata.

			pwDownloaded.append(meta + "\r\n" + nonStrippedText);
			pwDownloaded.close();

			pwStripped.append(meta + "\r\n" + strippedText);
			pwStripped.close();

//			filesParsed = new File(filesAlreadyParsed + acc + "_" + c + ".txt");
//			PrintWriter pwParsed = new PrintWriter(filesParsed);
//			pwParsed.append(acc + "_" + c);
//			pwParsed.close();

//			System.out.println("stripped file=" + fileStripped.getAbsolutePath());

//			endTime = System.currentTimeMillis();
//			System.out.println("getContract seconds=" + (System.currentTimeMillis() - startTime) / 1000);

//			NLP.printListOfString("metadataList2=", metadataList2);
//			System.out.println("document#" + c);

			System.out.println("gettingSolrFileReady");
			gl.gettingSolrFileReady(strippedText, fileSolr, metadataList2, parsingDisclosure, solrCore, false);
//			System.out.println("next loop=" + (c + 1));
			System.out.println("gk runtime (end to end): " + ((startTimeGK - endTimeGK) / 1000) + " seconds");
		}
	}

	public static String prepText(String text) throws IOException, SQLException {
		GoLaw gl = new GoLaw();
		NLP nlp = new NLP();
		int whitespaces = nlp.getAllMatchedGroups(text, Pattern.compile("[ ]{10,}")).size();
		System.out.println("white spaces==" + whitespaces);

		if (whitespaces > 1000)
			text = "";

		System.out.println("mergeLines");
		if (text.length() < 3000000)
			text = GoLaw.mergeLines(text);
		System.out.println("mergeLines finished");

		text = NLP.removeGobblyGookGetContracts(text);// remove jpg/graphic fles
//		System.out.println("removedGobbly seconds=" + (System.currentTimeMillis() - startTime) / 1000);
//		startTime = System.currentTimeMillis();
		if (GetContracts.turnOnPrinterWriter)
			Utils.writeTextToFile(new File("c:/temp/removeGobblyGook.html"), text);
		// delete me
//		System.out.println("removeGobblyGookGetContracts done. text.len=" + text.length());
//		System.out.println("@bef Tabl text.len=" + text.length());
		if (GetContracts.turnOnPrinterWriter)
			Utils.writeTextToFile(new File("c:/temp/1.beforeTable.html"), text);
		text = gl.removeDataTables(text);
//		System.out.println("removeDataTables seconds=" + (System.currentTimeMillis() - startTime) / 1000);
//		startTime = System.currentTimeMillis();// System.out.println("@raft Tabl text.len=" + text.length());
		if (GetContracts.turnOnPrinterWriter)
			Utils.writeTextToFile(new File("c:/temp/1.tmpNoTablesClient.html"), text);
//<===========DELETE
//		parsingDisclosure = true;
//		if (parsingDisclosure) {
//			System.out.println("it is disclosure - parsing headers based on bold/italics/underline");
		if (text.contains("<html>") || text.contains("<HTML>"))
			text = gl.getFormattedHeadings(text);
//			System.out.println("getFormattedHeadings seconds=" + (System.currentTimeMillis() - startTime) / 1000);
//			startTime = System.currentTimeMillis();
		// DELETE ME===>
		if (GetContracts.turnOnPrinterWriter)
			Utils.writeTextToFile(new File("c:/temp/1.tmpHeaderFormattingClient.html"), text);
//			long elapsedTime = System.currentTimeMillis() - startTime;
//			System.out.println("strip getHeadersFromFormatting: " + elapsedTime / 1000);
//		}

		// TODO NOTE: Add condition - if client sentence - avoid special characters
		// being stripped
		System.out.println("asciiHelper");
		long startTime = System.currentTimeMillis();

		text = gl.asciiHelper(text);// adds 2 sec. NOTE: This performs certain special character stripping
//		System.out.println("asciiHelper seconds=" + (System.currentTimeMillis() - startTime) / 1000);
		startTime = System.currentTimeMillis();
		// DELETE ME====XXX===>
		if (GetContracts.turnOnPrinterWriter)
			Utils.writeTextToFile(new File("c:/temp/1.tmpAsciiHelperClient.html"), text);

//		String str = ContractParser.stripHtmlTags(text); TEST WITHOUT THIS AND SEE IF IT WORKS

		// startTime = System.currentTimeMillis();
		if (nlp.getAllIndexEndLocations(text, Pattern.compile("(?ism)<html>")).size() > 0 && text.length() < 10000000) {

			// solution for hard return corrupting that happens with Jsoup when it is not
			// html
			System.out.println("It is an html file. So use Jsoup parser.");
			startTime = System.currentTimeMillis();
			Document doc = Jsoup.parse(text);
			System.out.println("Jsoup seconds=" + (System.currentTimeMillis() - startTime) / 1000);
			startTime = System.currentTimeMillis();
			if (GetContracts.turnOnPrinterWriter)
				Utils.writeTextToFile(new File("c:/temp/1.tmpJsoupClient.html"), doc.outerHtml());

//		Elements eles = doc.select("p[style~=font: ?24pt]");
//		eles = doc.select("p[style~=font: ?10pt] b");
//		doc.select("img").remove();

			startTime = System.currentTimeMillis();
			Elements eles = doc.select("table:contains(Original Lower-Tier)");
//			System.out.println(eles.size());
			String tableHtml;
			for (Element table : eles) {
//				System.out.println(
//						"getting rid of those dam tables. \nBUT if there's a lot of text algo will keep it.");
				tableHtml = "<p>" + StripHtmljSoup.table2Text(table, "<br>", " &nbsp; &nbsp;") + "</p>";
				table.before(tableHtml);
				table.remove();
			} // keep
			System.out.println("element table secs=" + (System.currentTimeMillis() - startTime) / 1000);

			if (GetContracts.turnOnPrinterWriter)
				Utils.writeTextToFile(new File("c:/temp/1.tmpHardReturnInsertClient.html"), doc.outerHtml());
//		<=========DELETE ME
//			System.out.println("Jsoup table removal seconds=" + (System.currentTimeMillis() - startTime) / 1000);
//			startTime = System.currentTimeMillis();

//			System.out.println("stripping html - where the magic happens! len="+doc.outerHtml().length());
			startTime = System.currentTimeMillis();

			text = gl.stripHtml(doc.outerHtml().replaceAll("(?ism)<TITLE>.*?</TITLE>", ""))
					.replaceAll("[\\x00-\\x1f\\x80-\\x9f&&[^\r\n]]", "");

			System.out.println("1. stripHtml seconds=" + (System.currentTimeMillis() - startTime) / 1000);
//			startTime = System.currentTimeMillis();
//		System.out.println("stripHtml seconds=" + (System.currentTimeMillis() - start) / 1000);

//			System.out.println("1 repl seconds==" + (sTime - System.currentTimeMillis()) / 1000);
//			sTime = System.currentTimeMillis();

//		<=========DELETE ME

//		start = System.currentTimeMillis();
			if (GetContracts.turnOnPrinterWriter)
				Utils.writeTextToFile(new File("c:/temp/finished stripped.txt"), text);
		} else {
			startTime = System.currentTimeMillis();
			System.out.println("it isn't HTML, welcome to 1984.");
			text = gl.stripHtml(text.replaceAll("(?ism)<TITLE>.*?</TITLE>", ""))
					.replaceAll("[\\x00-\\x1f\\x80-\\x9f&&[^\r\n]]", "");
			if (GetContracts.turnOnPrinterWriter)
				Utils.writeTextToFile(new File("c:/temp/finished stripped.txt"), text);

			text = text
//					.replaceAll("(?<=[A-Za-z\\)\\.\\d\",;]{1} ?)\r\n(?=[A-Za-z\\)\\.\\d\"\\(]{1})", " ") //corrupts by making everything into a para
					.replaceAll("\r\n ", "\r\n");
//			if(GetContracts.turnOnPrinterWriter) Utils.writeTextToFile(new File("c:/temp/1btmpStripped.txt"), text);

//			System.out.println(
//					"it isnt' html -- stripHtml seconds=" + (System.currentTimeMillis() - startTime) / 1000);
//			startTime = System.currentTimeMillis();

//		System.out.println("stripHtml seconds=" + (System.currentTimeMillis() - start) / 1000);

//		<=========DELETE ME

		}
		if (GetContracts.turnOnPrinterWriter)
			Utils.writeTextToFile(new File("c:/temp/B_tmpStripped.txt"), text);
//		startTime = System.currentTimeMillis();
//		System.out.println("Getting rid of pg #s! Pg #s cause parsing problems on the both data and client side.");
		text = gl.joinTextOverPages(text// this is not display text.
				.replaceAll("(?sm)&147;", "\r\n")
				// put joinTextOverPages prior to space bef hard return repl
				.replaceAll("(?sm)[ ]+\r\n", "\r\n").replaceAll("(?sm)\r\n[ ]+", "\r\n")
				.replaceAll("(Qx)+[\r\n ]+(XQ)+", " ").replaceAll("(Qx)+[\r\n ]+", "Qx ")
				.replaceAll("[\r\n ]+(XQ)", " XQ").replaceAll("''", "\"")
				.replaceAll("(\\-)(\r\n)([a-zA-Z]{1})", "$1 $3").replaceAll("[\r\n]{8,}", "\r\n\r\n\r\n")
				.replaceAll("(\r\n)(\\([a-z]{1}\\)|\\([ixv]{1,4}\\)|[\\d]{1,3}\\.?)([\r\n]{2,4})([A-Za-z]{1})",
						"$1$2 $4")
				.replaceAll("(\"\r\n)([A-Z]{1}[A-Za-z].{1,40})(\r\n\")", "\"$2\"")
				/*
				 * fixes where a quoted term has quotes on their owns lines
				 */

				.replaceAll("([a-z]{1})(\r\n)(\\.) ([A-Z]{1})", "$1$3$2$4")
				.replaceAll(
						"(\r\n)(SECTION|Section)( [\\d]{1,2}\\.[\\d]{1,3}| [\\d]{2,4})(\\.?)([\r\n]{2,4})(\\[?[A-Z]{1})",
						"$1$2$3$4 $6")
//				.replaceAll("[A-Za-z]{0,3}[\\d]{5,15}\\.?[a-zA-Z]{0,3}[\\d]{0,3}", "")// replace footer
				.replaceAll("(?sm)\\[", " xxOB").replaceAll("(?sm)\\]", "CBxx").replaceAll("\"\r\n(?=.{1,70})", "\"")
//				.replaceAll(
//						"[^a-zA-Z: \\\\\\[\\]\\{\\}\\|\\^\\~\\`\r\n\\(\\)\\d\\.\\;\\,\\!\\?\\}\\{\\[\\]\\{\\}\\#\\$\\%\\&\\@\\+\\=\\-\\_\"\']+",
//						" ")//bad idea - kills all special characters!!!
				.replaceAll("[ ]+", " ").replaceAll("\r\n \"", "\""));

//		System.out.println("getQuotedTerms");
//		quotedTerms = getQuotedTerms(text);

//		System.out.println("joinTextOverPages seconds=" + (System.currentTimeMillis() - startTime) / 1000);

		if (GetContracts.turnOnPrinterWriter)
			Utils.writeTextToFile(new File("c:/temp/1.joinTextOverPages.txt"), text);

		return text;

	}

	public static List<String> parseSuperFast(List<String> listParseThese, String txt, String contractLongName)
			throws IOException {
		NLP nlp = new NLP();
//XXXX DOUBLE CHECK WHEN I DELETED PARSE I DID NOT CAUSE AN ERROR.

		// Here all I check is if key words are in contract body and that certain key
		// words are NOT in contract long name name. keyWordsInContractName however can
		// be checked to see if they are in body of contract
		String keyWordsInContractNameStr = "", keyWordsNotToBeInContractNameStr = "", tmpStr = "";
		double fSize = 0, txtLen = txt.length();
		String firstPartOfText = txt.substring(0, (int) ((double) txtLen * .05));

		// can only check for what must be in contract body and not in contract name
		// because I don't have contractNameAlgo deployed
		for (int h = 0; h < listParseThese.size(); h++) {
			keyWordsInContractNameStr = "";
			keyWordsNotToBeInContractNameStr = "";

			tmpStr = "";

			if (nlp.getAllMatchedGroups(listParseThese.get(h), Pattern.compile("(?<=fSize=).*?(?=\\|\\|)"))
					.size() > 0) {
//				System.out.println("fSize?=="
//						+ nlp.getAllMatchedGroups(listParseThese.get(h), Pattern.compile("(?<=fSize=).*?(?=\\|\\|)"))
//								.get(0));
				fSize = Double.parseDouble(
						nlp.getAllMatchedGroups(listParseThese.get(h), Pattern.compile("(?<=fSize=).*?(?=\\|\\|)"))
								.get(0));
				if (firstPartOfText.toLowerCase().contains("html")) {
					fSize = fSize * 1.25;
				}
			}

//			System.out.println("fSize=" + fSize + " txtLen=" + txtLen);

			if (txtLen < fSize)
				continue;

			if (nlp.getAllMatchedGroups(listParseThese.get(h),
					Pattern.compile("(?<=keyWordsInContractName=).*?(?=\\|\\|)")).size() > 0) {
				keyWordsInContractNameStr = nlp.getAllMatchedGroups(listParseThese.get(h),
						Pattern.compile("(?<=keyWordsInContractName=).*?(?=\\|\\|)")).get(0);
//				System.out.println("s/fast keyWordsInContractName=" + keyWordsInContractName);

				if (nlp.getAllMatchedGroups(contractLongName, Pattern.compile(keyWordsInContractNameStr)).size() > 0) {
//					System.out.println("2 has patterns");
					tmpStr = listParseThese.get(h);
					listParseThese = new ArrayList<String>();
					listParseThese.add(tmpStr);
//				System.out.println("s/fast listParseThese=" + tmpStr);
//				System.out.println("keyWordsMustBeInContractBody.len=" + keyWordsMustBeInContractBody.length()
//						+ " keyWordsInContractName.len=" + keyWordsInContractName.length()
//						+ " keyWordsNotToBeInContractName.len=" + keyWordsNotToBeInContractName.length());
					return listParseThese;
					// keyWordsNotToBeInContractName - ck against contractLongName
				}
			}
		}
		// set listParseThese to just the one that meets the condition.

		listParseThese = new ArrayList<String>();
		return listParseThese;

	}

	public static List<String> prepareMetadataList(List<String> metadataList, List<String> listOp,
			List<String[]> listOfContractNamesFromAlgo, String openParaContractName, String openingParagraph,
			TreeMap<Integer, String> leMap, String openingParaContractingPartyRole, String openingParaContractingRole,
			String openingParaContractingParties, String contractLongNameHasKeyWords) throws IOException {

//		pwMeta.append(openingParaContractingPartyRole);
//		pwMeta.append(openingParaContractingRole);
//		pwMeta.append(openingParaContractingParties);
//		if (contractLongNameHasKeyWords.length() > 0)
//			pwMeta.append(contractLongNameHasKeyWords);

		NLP nlp = new NLP();

		for (int j = 0; j < listOp.size(); j++) {
//			 System.out.println("1 listOp==" + listOp.get(j));
			if (listOp.get(j).split("\\|\\|").length > 2 || nlp.getAllIndexEndLocations(listOp.get(j),
					Pattern.compile("(contractingPartyRole|contractingParty|contractingRole)")).size() > 0) {
				if (listOp.get(j).split("\\|\\|").length > 2
						&& listOp.get(j).split("\\|\\|")[0].split("=")[1].trim().length() > 2) {
					metadataList.add(listOp.get(j).split("\\|\\|")[0].trim());
					metadataList.add(listOp.get(j).split("\\|\\|")[1].trim());
//					System.out.println("1.L2 split - listOp==" + listOp.get(j));
				}

				if (nlp.getAllIndexEndLocations(listOp.get(j),
						Pattern.compile("(contractingPartyRole|contractingParty|contractingRole)")).size() > 0) {
//					System.out.println("2 added - listOp==" + listOp.get(j));
					metadataList.add(listOp.get(j).trim());
//					System.out.println("2.L2 listOp==" + listOp.get(j));
				}

			} else {
				if (listOp.get(j).trim().split("=").length > 1)
					metadataList.add(listOp.get(j).trim());
//				System.out.println("3.L2 listOp==" + listOp.get(j));
			}
		}

//		for (int w = 0; w < metadataList.size(); w++) {
//			if (metadataList.get(w).split("=").length < 2)
//				continue;
//			pwMeta.append(metadataList.get(w) + "\n");
//		}

		// produces list of all contract names found - this is for metadata purposes.
		for (int z = 0; z < listOfContractNamesFromAlgo.size(); z++) {
//			System.out.println("kN=" + listOfContractNamesFromAlgo.get(z)[0] + " openParaKn="
//					+ openParaContractName.replaceAll("openParaContractName=\\|\\|", "").trim());
			if (listOfContractNamesFromAlgo.get(z)[0].trim().toLowerCase()
					.equals(openParaContractName.replaceAll("openParaContractName=\\|\\|", "").toLowerCase().trim())) {
				metadataList.add("contractNameAlgo_Top=" + z + " "
						+ DisplayNames.makeContractNamesInitialCaps(listOfContractNamesFromAlgo.get(z)[0]) + " cnt="
						+ listOfContractNamesFromAlgo.get(z)[1] + " idx=" + listOfContractNamesFromAlgo.get(z)[2]
						+ " %locInDoc=" + listOfContractNamesFromAlgo.get(z)[3] + " score="
						+ listOfContractNamesFromAlgo.get(z)[4] + " contractNameInOpeningPara=Y\n");

				if (z == 0) {
					metadataList.add("contractNameAlgo=" + z + " "
							+ DisplayNames.makeContractNamesInitialCaps(listOfContractNamesFromAlgo.get(z)[0]) + " cnt="
							+ listOfContractNamesFromAlgo.get(z)[1] + " idx=" + listOfContractNamesFromAlgo.get(z)[2]
							+ " %locInDoc=" + listOfContractNamesFromAlgo.get(z)[3] + " score="
							+ listOfContractNamesFromAlgo.get(z)[4] + " contractNameInOpeningPara=Y\n");
				}

			} else {
				metadataList.add("contractNameAlg_Top=" + z + " "
						+ DisplayNames.makeContractNamesInitialCaps(listOfContractNamesFromAlgo.get(z)[0]) + " cnt="
						+ listOfContractNamesFromAlgo.get(z)[1] + " idx=" + listOfContractNamesFromAlgo.get(z)[2]
						+ " %locInDoc=" + listOfContractNamesFromAlgo.get(z)[3] + " score="
						+ listOfContractNamesFromAlgo.get(z)[4] + " contractNameInOpeningPara=N\n");

				if (z == 0) {
					metadataList.add("contractNameAlgo=" + z + " "
							+ DisplayNames.makeContractNamesInitialCaps(listOfContractNamesFromAlgo.get(z)[0]) + " cnt="
							+ listOfContractNamesFromAlgo.get(z)[1] + " idx=" + listOfContractNamesFromAlgo.get(z)[2]
							+ " %locInDoc=" + listOfContractNamesFromAlgo.get(z)[3] + " score="
							+ listOfContractNamesFromAlgo.get(z)[4] + " contractNameInOpeningPara=Y\n");
				}

			}
			// [0]=kN,[1]=cnt,[2]=idx,[3]%loc,[4]score
		}

		String contractingPartiesConfirmed = "legalEntitiesInOpeningParagraph=";
//		System.out.println("openingParagraph=="+openingParagraph);
		TreeMap<Integer, String> leOpeningParaMap = new TreeMap<Integer, String>();
//				EntityRecognition.getLegalEntities(openingParagraph);
//		NLP.printMapIntStr("legalEntitiesInOpeningParagraph==",leOpeningParaMap);

		int leCnt = 0;
		String legalEntity = "";
		for (Map.Entry<Integer, String> entryOp : leOpeningParaMap.entrySet()) {
//			System.out.println("legalEntity opening para=" + entryOp.getValue().replaceAll("(Qx|XQ|,|;|\\.)+", ""));

			leCnt = 0;
		}

		if (contractingPartiesConfirmed.split("=").length > 1) {
			metadataList.add(contractingPartiesConfirmed + "\n");
		}

		metadataList.add(openingParaContractingPartyRole);
		metadataList.add(openingParaContractingRole);
		metadataList.add(openingParaContractingParties);

		if (contractLongNameHasKeyWords.length() > 0)
			metadataList.add(contractLongNameHasKeyWords);

		return metadataList;
	}

	public static List<String> isItaContractIwantToParse(List<String> listParseThese,
			List<String[]> listOfContractNamesFromAlgo, String textType, String contractName, boolean parsingDisclosure)
			throws IOException {

		List<String> listOfContractAttributes = new ArrayList<String>();

		// determine if contractLongName contains pattern of key word(s) that must be in
		// the contract name. If that's the case then that over-rides whatever is found
		// by contract name algo.

		NLP nlp = new NLP();
		String keyWord = "", contractType = "", solrCore = "";

		for (int b = 0; b < listParseThese.size(); b++) {
//			System.out.println("listParseThese=" + listParseThese.get(b));
			keyWord = "";
			contractType = "";
			solrCore = "";

//			System.out.println("listParseThese.get=" + listParseThese.get(b));
			// this is to parse based on keyWordsInContractName and
			// keyWordsNotToBeInContractName and keyWordsMustBeInContractBody conditions
//			System.out.println("contractName=" + contractName);
//			listParseThese.add("solrCore=indenture||contractType=indenture||keyWordsInContractName=Trust|TRUST||"
//					+ "keyWordsMustBeInContractBody=Owner Trustee|Indenture Act|prudent person|prudent Person"
//					+ "|prudent individual|prudent institution|prudent investor|prudent corporate|prudent indenture|prudent trustee"
//					+ "|prudent man||keyWordsNotToBeInContractName=Supp|SUPP| Note| NOTE|Servicing|SERVICING||"
//					+ "fsize=60000||");

			if (nlp.getAllMatchedGroups(listParseThese.get(b),
					Pattern.compile("(?sm)(?<=keyWordsInContractName=).*?(?=\\|\\|)")).size() > 0) {
				// <==fetches the keyword
				keyWord = nlp.getAllMatchedGroups(listParseThese.get(b),
						Pattern.compile("(?sm)(?<=keyWordsInContractName=).*?(?=\\|\\|)")).get(0);
//				System.out.println(
//						"1. contractName=" + contractName + " keyWordsInContractName=" + keyWordsInContractName);

				// now I see if the contractName has the keyword
				if (nlp.getAllIndexEndLocations(contractName, Pattern.compile(keyWord)).size() > 0) {
//					System.out.println("foundKeyWordsInContractName=" + foundKeyWordsInContractName);
					contractType = nlp.getAllMatchedGroups(listParseThese.get(b),
							Pattern.compile("(?<=contractType=)(.*?)(?=\\|\\|)")).get(0);
					solrCore = nlp
							.getAllMatchedGroups(listParseThese.get(b), Pattern.compile("(?<=solrCore=).*?(?=\\|\\|)"))
							.get(0);
					// add field - contract key words are in contractLongName.
					listOfContractAttributes.add("parse=true");
					listOfContractAttributes.add("solrCore=" + solrCore);
					listOfContractAttributes.add(contractType);

					if (textType.contains("contactLongName")) {
						listOfContractAttributes.add("contractLongNameHasKeyWords=Y");
					}

					return listOfContractAttributes;

					// it then still goes to contractNamesFromAlgo which is good as it will
					// determine if there's a match via contract name found in contract versus only
					// the name.
				}
			}
		}

		listOfContractAttributes = new ArrayList<String>();
		return listOfContractAttributes;
	}

	public static boolean isAllCaps(String text, int idx) {

		String t1, t2;
		if (text.length() < idx)
			return false;

		t1 = text.substring(0, idx);
//		System.out.println("t1=" + t1);
		int sIdx = t1.lastIndexOf("\r\n");
		if (sIdx < 0) {
			sIdx = 0;
		}

		t2 = text.substring(idx);
//		System.out.println("t2=" + t2);
		int eIdx = t2.indexOf("\r\n") + idx;
		String str = text.substring(sIdx, eIdx);

		if (str.replaceAll("[a-wyz]", "").length() == str.length() && str.length() > 140) {
			// System.out.println("1 isAllCaps===" + true + " str=" + str);
			return true;
		}

		else {
			// System.out.println("2 isAllCaps===" + false + " str=" + str + "
			// str.repl([a-z]).len="
			// + str.replaceAll("[a-z]", "").length() + " str.len=" + str.length());
			return false;
		}
	}

	public static List<String> getOpeningParagraphAttributes(List<String[]> listDated, List<String[]> listBetweenAmong,
			String text, String contractNameFromContractNameFinder, String contractType) throws IOException {

		GetContracts gK = new GetContracts();
		NLP nlp = new NLP();

//		NLP.printListOfStringArray("listDated=", listDated);
//		NLP.printListOfStringArray("listBetweenAmong=", listBetweenAmong);
		List<String> listLegalEntities = new ArrayList<String>();
		Pattern patternThisAgreement = Pattern.compile("(?sm)(This|THIS) ([A-Z]{1}[a-zA-Z]{1,5})");
		List<String[]> listThis = nlp.getAllMatchedGroupsAndEndIdxLocs(text.substring(0, Math.min(text.length(), 5000)),
				patternThisAgreement);
//		NLP.printListOfStringArray("listThis=", listThis);
//		System.out.println("listThis=" + listThis.size());
		boolean found = false;
		List<String> list = new ArrayList<String>();
		if (listDated.size() == 0) {
			listDated = listThis;
		}
		if (listBetweenAmong.size() == 0 && listThis.size() > 0 && listDated.size() > 0
				&& !listDated.get(0)[1].equals(listThis.get(0)[1])) {
			listBetweenAmong = listDated;
		}
		TreeMap<Integer, String> mapEntityAndRole = new TreeMap<Integer, String>();
		List<String[]> listQuotedRoles = new ArrayList<String[]>();
		int idxDt = -1, idxAmg = -1, sIdx = -1, eIdx = -1;
		String t1, t2, openingPara, contractName;
		for (int c = 0; c < listDated.size(); c++) {
//			System.out.println("getOpeningParagraph -- listDated.get(c)[0]=" + listDated.get(c)[0]);
//			if (nlp.getAllIndexStartLocations(listDated.get(c)[0], Pattern.compile("1939|TIA|Trust Indenture Act"))
//					.size() > 0)
//				continue;

			idxDt = Integer.parseInt(listDated.get(c)[1]);
//			System.out.println("idxDt=" + idxDt);

			for (int n = 0; n < listBetweenAmong.size(); n++) {
//				System.out.println("getOpeningParagraph -- listBetweenAmong=" + listBetweenAmong.get(n)[0]);
				if (found)
					break;
//				System.out.println("idxAmg =" + idxAmg);

				idxAmg = Integer.parseInt(listBetweenAmong.get(n)[1]);
				if (Math.abs(idxAmg - idxDt) < 350) {
					if (Math.abs(idxAmg - idxDt) < 350) {
						t1 = text.substring(0, idxDt);
//						System.out.println("t1=" + t1);
						sIdx = t1.lastIndexOf("\r\n");
						t2 = text.substring(idxDt);
//						System.out.println("t2=" + t2);
						eIdx = t2.indexOf("\r\n") + idxDt;
//						System.out.println("eIdx==" + eIdx + " idxDt=" + idxDt + " sIdx=" + sIdx);
						openingPara = text.substring(sIdx, eIdx).replaceAll("[\r\n]+", " ").replaceAll("[ ]+", " ");
//						System.out.println("is this the openingPara=?" + openingPara);
						if (openingPara.length() < 80
								|| nlp.getAllIndexEndLocations(openingPara, Pattern.compile("(?is)whereas")).size() > 0)
							continue;
						found = true;
						// last index of \r\n before idxDt and get first instance if \r\n after idxDt.
//						System.out.println("this is the opending para=" + openingPara);
//						TreeMap<Integer, String> map = new TreeMap<Integer, String>();
//								EntityRecognition.getLegalEntities(openingPara);

//						for (Map.Entry<Integer, String> entry : map.entrySet()) {
//							listLegalEntities.add(entry.getValue());
//						}

						listQuotedRoles = nlp.getAllMatchedGroupsAndEndIdxLocs(openingPara, gK.patternQuotedRole);
//						NLP.printListOfStringArray("o/p listQuotedRoles", listQuotedRoles);
//						NLP.printListOfString("o/p listLegalEntities=", listLegalEntities);

						mapEntityAndRole.put(0,
								"openingParagraph=" + openingPara.replaceAll("Regulation_S", "Regulation S")
										.replaceAll("(?sm)(aZaZ)+", ". \r\n").replaceAll("(?sm)(xZxZ1|bZbZ1)+", ". ")
										.replaceAll("(?sm)(xZxZ2|bZbZ2)+", ".  ").replaceAll("(?sm)(yZyZ)+", ".\" ")
										.replaceAll("[ ]+", " ").replaceAll("(sssS1s)", "").replaceAll("xxOP", "(")
										.replaceAll("CPxx", ")").replaceAll("(xxPD)+", ".").replaceAll("xxOB", "[")
										.replaceAll("CBxx", "]").replaceAll("OBCB", "[]").replaceAll("CBOB", "][")
										.replaceAll(",? consecutive=.*?$", "").replaceAll("Qx|XQ", "").trim()
										+ "||\r\n");
						for (int z = 0; z < listLegalEntities.size(); z++) {
							List<String[]> listTmp = nlp.getAllMatchedGroupsAndEndIdxLocs(openingPara,
									Pattern.compile("(" + listLegalEntities.get(z) + ")"));
							for (int d = 0; d < listTmp.size(); d++) {
								// put legal entities in map at idx loc
								mapEntityAndRole.put(Integer.parseInt(listTmp.get(d)[1]), "contractingParty="
										+ listTmp.get(d)[0].replaceAll("Regulation_S", "Regulation S")
												.replaceAll("(?sm)(aZaZ)+", ". \r\n")
												.replaceAll("(?sm)(xZxZ1|bZbZ1)+", ". ")
												.replaceAll("(?sm)(xZxZ2|bZbZ2)+", ".  ")
												.replaceAll("(?sm)(yZyZ)+", ".\" ").replaceAll("[ ]+", " ")
												.replaceAll("(sssS1s)", "").replaceAll("xxOP", "(")
												.replaceAll("CPxx", ")").replaceAll("(xxPD)+", ".")
												.replaceAll("xxOB", "[").replaceAll("CBxx", "]")
												.replaceAll("OBCB", "[]").replaceAll("CBOB", "][")
												.replaceAll(",? consecutive=.*?$", "").replaceAll("Qx|XQ", "").trim()
										+ "||");
							}
						}
						for (int z = 0; z < listQuotedRoles.size(); z++) {
							// put legal quote roles in map at idx loc
							if (nlp.getAllMatchedGroups(listQuotedRoles.get(z)[0],
									Pattern.compile("(?sm)(^| )[a-z]{1}|Execution|Date|EXECUTION|DATE")).size() > 0) {
//								System.out.println("continue contractingPartyRole==" + listQuotedRoles.get(z)[0]);
								continue;
							} else {
								mapEntityAndRole.put(Integer.parseInt(listQuotedRoles.get(z)[1]),
										"contractingPartyRole=" + listQuotedRoles.get(z)[0]
												.replaceAll("Regulation_S", "Regulation S")
												.replaceAll("(?sm)(aZaZ)+", ". \r\n")
												.replaceAll("(?sm)(xZxZ1|bZbZ1)+", ". ")
												.replaceAll("(?sm)(xZxZ2|bZbZ2)+", ".  ")
												.replaceAll("(?sm)(yZyZ)+", ".\" ").replaceAll("[ ]+", " ")
												.replaceAll("(sssS1s)", "").replaceAll("xxOP", "(")
												.replaceAll("CPxx", ")").replaceAll("(xxPD)+", ".")
												.replaceAll("xxOB", "[").replaceAll("CBxx", "]")
												.replaceAll("OBCB", "[]").replaceAll("CBOB", "][")
												.replaceAll(",? consecutive=.*?$", "").replaceAll("Qx|XQ", "").trim()
												+ "||\r\n");
							}
						}

						if (contractType.length() > 0) {
							// ZZZZ need to put idx here for map - use hashcode as substitute
							mapEntityAndRole.put(contractType.hashCode(),
									"openParaContractType=" + contractType.replaceAll("Regulation_S", "Regulation S")
											.replaceAll("(?sm)(aZaZ)+", ". \r\n")
											.replaceAll("(?sm)(xZxZ1|bZbZ1)+", ". ")
											.replaceAll("(?sm)(xZxZ2|bZbZ2)+", ".  ").replaceAll("(?sm)(yZyZ)+", ".\" ")
											.replaceAll("[ ]+", " ").replaceAll("(sssS1s)", "").replaceAll("xxOP", "(")
											.replaceAll("CPxx", ")").replaceAll("(xxPD)+", ".").replaceAll("xxOB", "[")
											.replaceAll("CBxx", "]").replaceAll("OBCB", "[]").replaceAll("CBOB", "][")
											.replaceAll(",? consecutive=.*?$", "").replaceAll("Qx|XQ", "").trim()
											+ "||\r\n");
						}

//						System.out.println("1. openingPara=" + openingPara.trim());
						if (contractNameFinder(openingPara).size() > 0) {
							contractName = contractNameFinder(openingPara).get(0)[0];
						} else {
							contractName = "";
						}
						if (nlp.getAllMatchedGroupsAndEndIdxLocs(openingPara, Pattern.compile("("
								+ contractName.replaceAll("[\\\\\\}\\{\\[\\]\\(\\)\\*\\+-\\?\\!\\%\\^\\$]+", "") + ")"))
								.size() > 0) {
							if (contractName.trim().equals(contractNameFromContractNameFinder)) {
								mapEntityAndRole.put(
										Integer.parseInt(nlp.getAllMatchedGroupsAndEndIdxLocs(openingPara,
												Pattern.compile("(" + contractName + ")")).get(0)[1]),
										"openParaContractName=" + contractName.trim()
												.replaceAll("Regulation_S", "Regulation S")
												.replaceAll("(?sm)(aZaZ)+", ". \r\n")
												.replaceAll("(?sm)(xZxZ1|bZbZ1)+", ". ")
												.replaceAll("(?sm)(xZxZ2|bZbZ2)+", ".  ")
												.replaceAll("(?sm)(yZyZ)+", ".\" ").replaceAll("[ ]+", " ")
												.replaceAll("(sssS1s)", "").replaceAll("xxOP", "(")
												.replaceAll("CPxx", ")").replaceAll("(xxPD)+", ".")
												.replaceAll("xxOB", "[").replaceAll("CBxx", "]")
												.replaceAll("OBCB", "[]").replaceAll("CBOB", "][")
												.replaceAll(",? consecutive=.*?$", "").replaceAll("Qx|XQ", "").trim()
												+ "||\r\n" +

												"openParaKNameEqualsContractAlgo=Y||\r\n");

							} else {
								mapEntityAndRole
										.put(Integer
												.parseInt(nlp
														.getAllMatchedGroupsAndEndIdxLocs(openingPara,
																Pattern.compile("(" + contractName
																		.replaceAll("[\\)\\(\\+\\*\\$]+", "") + ")"))
														.get(0)[1]),
												"openParaContractName=" + contractName.trim()
														.replaceAll("Regulation_S", "Regulation S")
														.replaceAll("(?sm)(aZaZ)+", ". \r\n")
														.replaceAll("(?sm)(xZxZ1|bZbZ1)+", ". ")
														.replaceAll("(?sm)(xZxZ2|bZbZ2)+", ".  ")
														.replaceAll("(?sm)(yZyZ)+", ".\" ").replaceAll("[ ]+", " ")
														.replaceAll("(sssS1s)", "").replaceAll("xxOP", "(")
														.replaceAll("CPxx", ")").replaceAll("(xxPD)+", ".")
														.replaceAll("xxOB", "[").replaceAll("CBxx", "]")
														.replaceAll("OBCB", "[]").replaceAll("CBOB", "][")
														.replaceAll(",? consecutive=.*?$", "").replaceAll("Qx|XQ", "")
														.trim() + "||\r\n");
							}
						}
					}
				}
			}
		}

		List<String[]> listOpenAttr = new ArrayList<String[]>();
//		NLP.printMapIntStr("mapEntityAndRole=", mapEntityAndRole);
//		String priorLE;
//		int pIdx, idx;
		for (Map.Entry<Integer, String> entry : mapEntityAndRole.entrySet()) {
			// if quotedRole is subsequent to (idx loc) to legalEntity and w/n 170 then
			// quoted role is associated with that entity.
			String[] ary = { entry.getKey() + "", entry.getValue() };
//			System.out.println("party/roles ary=" + Arrays.toString(ary));
			listOpenAttr.add(ary);
		}

		StringBuilder sbKPR = new StringBuilder();
		StringBuilder sbKP = new StringBuilder();
		StringBuilder sbR = new StringBuilder();

		for (int i = 0; i < listOpenAttr.size(); i++) {
			// if legalEntit is it followed by a quotedRole that is less than 150 characters
			// away.
//			System.out.println("1 listOpenAttr=" + Arrays.toString(listOpenAttr.get(i)));
			if (i + 1 < listOpenAttr.size() && listOpenAttr.get(i)[1].contains("contractingParty=")
					&& listOpenAttr.get(i + 1)[1].contains("contractingPartyRole=")
					&& Integer.parseInt(listOpenAttr.get(i + 1)[0]) - Integer.parseInt(listOpenAttr.get(i)[0]) < 150) {

				sbKPR.append("\""
						+ listOpenAttr.get(i)[1].replaceAll("contractingParty=", "").replaceAll("(?ism)(?<=INC)x", ".")
								.trim()
						+ "\", \"" + listOpenAttr.get(i + 1)[1].replaceAll("(?ism)(?<=INC)x", ".")
								.replaceAll("contractingPartyRole=", "").trim()
						+ "\", ");

				list.add("\"contractingPartyRole\":[" + sbKPR.toString().substring(0, sbKPR.toString().length() - 1)
						.replaceAll("[\\|]+", "").replaceAll(",$", "") + "]\r\n");

				sbKPR = new StringBuilder();

				{
//					System.out.println("2 listOpenAttr=" + Arrays.toString(listOpenAttr.get(i)));
					i++;
					continue;
				}

			}

			if (listOpenAttr.get(i)[1].contains("contractingParty=")) {
				sbKP.append("\"" + listOpenAttr.get(i)[1].replaceAll("contractingParty=", "")
						.replaceAll("(?ism)(?<=INC)x", ".").trim() + "\", ");
			}
			if (listOpenAttr.get(i)[1].contains("contractingPartyRole=")) {
				sbR.append("\"" + listOpenAttr.get(i)[1].replaceAll("contractingPartyRole=", "").trim()
						.replaceAll("(?ism)(?<=INC)x", ".") + "\", ");
			} else if (nlp.getAllIndexEndLocations(listOpenAttr.get(i)[1],
					Pattern.compile("contractingParty|contractingRole")).size() == 0) {
//				System.out.println("add to list==" + listOpenAttr.get(i)[1]);
				list.add(listOpenAttr.get(i)[1].replaceAll("INCx", "INC."));
			}
		}

		if (sbKP.toString().length() > 0) {
			list.add("\"contractingParty\":[" + sbKP.toString().substring(0, sbKP.toString().length() - 1)
					.replaceAll("[\\|]+", "").replaceAll(",$", "") + "]\r\n");
		}
		if (sbR.toString().length() > 0) {
			list.add("\"contractingRole\":[" + sbR.toString().substring(0, sbR.toString().length() - 1)
					.replaceAll("[\\|]+", "").replaceAll(",$", "") + "]\r\n");
		}

//		NLP.printListOfString("2 listOpenAttri=", list);

		return list;

	}

	public static String contractTypePatternsFound(String text, Pattern patternsToInclude, Pattern patternsToExclude)
			throws IOException {

		GetContracts gK = new GetContracts();

		NLP nlp = new NLP();
		List<String> list = nlp.getAllMatchedGroups(text, patternsToInclude);

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			if (nlp.getAllIndexEndLocations(list.get(i), patternsToExclude).size() == 0) {
				sb.append(list.get(i).toLowerCase() + ", ");
			}
		}

		return sb.toString();
	}

	public static boolean isPartOfFileNameInDirectory(File folder, String acc_i) {

		try {
			boolean recursive = true;

			Collection files = FileUtils.listFiles(folder, null, recursive);

			for (Iterator iterator = files.iterator(); iterator.hasNext();) {
				File file = (File) iterator.next();
				if (file.getName().contains(acc_i)) {
//					System.out.println(file.getAbsolutePath());
					return true;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;

	}

	public static List<String[]> contractNameLotto(String text) throws IOException {
		NLP nlp = new NLP();
		text = text.replaceAll("(xxOB|CBxx|xxPD|xxOP|CPxx)", "");
		Pattern patternContractNameLotto = Pattern
				.compile("(x?x?[A-Z]{1}).{1,100}(CONTRACT|AGREEMENT)|(AMEND|SUPP|RESTATE).{1,1000}[a-z]");
		List<String[]> listContractNameLotto = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(text,
				patternContractNameLotto);
//		NLP.printListOfStringArray("listContractNameLotto==", listContractNameLotto);

		GetContracts gK = new GetContracts();
		String kN = "", t = "", strBef = "", strAft = "", para = "";
		boolean inOpeningParaHighConfidence = false;
//		System.out.println("contractNameFinder -- listPatterns.size=" + listPatterns.size());
		TreeMap<Integer, String[]> mapCount = new TreeMap<Integer, String[]>();
		int score = 0;
		double foundInThisTopPartOfText = 0;
		for (int b = 0; b < listContractNameLotto.size(); b++) {
			score = 0;
			inOpeningParaHighConfidence = false;
//				mayBeInOpeningPara = false;
//				System.out.println("listContractNameLotto.get(b)[0].trim()=" + listContractNameLotto.get(b)[0].trim());
			kN = listContractNameLotto.get(b)[0].trim();
//			System.out.println("Kn?=="+t);
			kN = kN.replaceAll(" \\(this \"Agreement\"\\)", "").replaceAll("\\)$", "xxCP").replaceAll("^\\(", "OPxx")
					.replaceAll("(?sm)This |THIS ", "").trim();
			t = kN.toLowerCase();
//				System.out.println("Kn?=="+t);
//				System.out.println("kFinder - textSnip="
//						+ text.substring(Math.max(0, Integer.parseInt(listContractNameLotto.get(b)[1]) - 25),
//								Math.min(text.length(), Integer.parseInt(listContractNameLotto.get(b)[2]) + 25)));
//				System.out.println("listContractNameLotto (b)[2] len=" + listContractNameLotto.get(b)[2] + " ?>text20%len=" + text.length());
			if (Integer.parseInt(listContractNameLotto.get(b)[2]) > text.length())
				break;

			strBef = text.substring(Math.max(0, Integer.parseInt(listContractNameLotto.get(b)[2]) - 500),
					Math.min(Integer.parseInt(listContractNameLotto.get(b)[2]), text.length()));
			if (nlp.getAllIndexStartLocations(strBef, Pattern.compile("(\r\n).*?\r\n")).size() > 0) {
				strBef = strBef.substring(
						nlp.getAllIndexStartLocations(strBef, Pattern.compile("(\r\n).*?\r\n")).get(
								nlp.getAllIndexStartLocations(strBef, Pattern.compile("(\r\n).*?\r\n")).size() - 1),
						strBef.length());
			}

			if (nlp.getAllIndexStartLocations(strBef, Pattern.compile("(\r\n).*?\r\n")).size() == 0) {
				strBef = strBef.substring(strBef.lastIndexOf("\r\n") + 2, strBef.length());
			}

			if (Integer.parseInt(listContractNameLotto.get(b)[2]) < text.length()) {
				strAft = text.substring(Integer.parseInt(listContractNameLotto.get(b)[2]));
			}

			if (nlp.getAllIndexEndLocations(strAft, Pattern.compile("(?sm)(\r\n).*?\r\n")).size() > 0) {
				strAft = strAft.substring(0,
						nlp.getAllIndexEndLocations(strAft, Pattern.compile("\r\n.*?\r\n")).get(0));
//					System.out.println("1. strAft=" + strAft);
			}

			if (strAft.length() > (strAft.lastIndexOf("\r\n") + 2)) {
				strAft = strAft.substring(strAft.lastIndexOf("\r\n") + 2, strAft.length());
//					System.out.println("2. strAft="+strAft);
			}

			// System.out.println("textSnip=" + text.substring(Math.max(0,
			// Integer.parseInt(listContractNameLotto.get(b)[1]) - 25),
			// Math.min(text.length(), Integer.parseInt(listContractNameLotto.get(b)[2]) +
			// 25)));
//			 System.out.println("match=" + listContractNameLotto.get(b)[0]);
			// System.out.println("strBef=" + strBef + "|end");
			// System.out.println("strAft=" + strAft + "|end");
//				System.out.println("strBef+aft=" + strBef + "<==b/w==>" + strAft + "||END");
			para = strBef + " " + strAft;
//			System.out.println("kN para=" + para.substring(0, Math.min(50, para.length())).trim());
			if (nlp.getAllIndexEndLocations(kN, Pattern.compile(
					"(?sm)Exhibit [A-Z]{1}|Schedule [A-Z]{1}|Annex [A-Z]{1}|Appendix [A-Z]{1}|Means?|Preamble|Section|Article|EXHIBIT [A-Z]{1}|SCHEDULE [A-Z]{1}|ANNEX [A-Z]{1}|APPENDIX [A-Z]{1}|MEANS?|PREAMBLE|SECTION|ARTICLE"))
					.size() > 0
					|| nlp.getAllIndexEndLocations(kN, Pattern.compile("(?ism)\\.")).size() > 0
					|| !SolrPrep.isItInitialCapsAgreementId(
							GoLaw.patternStopWordsAggressiveNotForHtxtorDisplayData.matcher(kN).replaceAll(""))
					|| nlp.getAllMatchedGroups(kN.replaceAll("[^A-Z]", ""), Pattern.compile("(?sm)[A-Z]{1,3}[;: ,]{1}"))
							.size() > 3
					|| nlp.getAllMatchedGroups(strAft, Pattern.compile("(?sm)(\r\n|^)+\\d")).size() > 3
//						strBef+aft=INDENTURE<==b/w==> dated as of February 1, 2013 between Netflix, IncxxPD, a Delaware corporation, 
//						and Wells Fargo Bank, National Association, a national banking association, as Trustee.||END
			) {
//				System.out.println("1 continuing. kN=" + kN + " kN sch?="
//						+ nlp.getAllIndexEndLocations(kN,
//								Pattern.compile(
//										"(?ism)exhibit|schedule|annex|appendix|means?|preamble|section|article"))
//								.size()
//						+ " listContractNameLotto.get(b)[1]=" + listContractNameLotto.get(b)[1]
//						+ " isItInitialCapsAgreementId="
//						+ SolrPrep.isItInitialCapsAgreementId(
//								GoLaw.patternStopWordsAggressiveNotForHtxtorDisplayData.matcher(kN).replaceAll(""))
//						+ " [A-Z]1,3 .size="
//						+ nlp.getAllMatchedGroups(kN.replaceAll("[^A-Z]", ""),
//								Pattern.compile("(?sm)[A-Z]{1,3}[;: ,]{1}")).size()
//						+ " strAft size=="
//						+ nlp.getAllMatchedGroups(strAft, Pattern.compile("(?sm)(\r\n|^)+\\d")).size());
				continue;
			}

			if (nlp.getAllMatchedGroups(strAft, Pattern.compile("(?ism)[\\d] ?$")).size() > 0
					&& !strAft.toLowerCase().contains("date")) {
//				System.out.println("1x1 continuing");
				continue;
			}

			if (nlp.getAllIndexStartLocations(para, Pattern.compile("(?ism)dated?|between|among")).size() > 1) {
				// both dated and among/between
				inOpeningParaHighConfidence = true;
				// plus 2
				score = score + 2;
//					System.out.println("kN=" + kN + " openPara plus 2 score=" + score);
			}

			if (para.replaceAll(
					listContractNameLotto.get(b)[0].replaceAll("[\\\\\\}\\{\\[\\]\\(\\)\\*\\+-\\?\\!\\%\\^\\$]+", ""),
					"").trim().length() == 0) {
				score = score + 1;
//					System.out.println("kN=" + kN + " by itself plus 1 score=" + score);
			}

			if (listContractNameLotto.get(b)[0].replaceAll("[A-Z ]+", "").length() == 0) {
				// plus1
				score = score + 1;
//					System.out.println("kN=" + kN + " all caps plus 1 score=" + score);
			}

			if (!inOpeningParaHighConfidence
					&& nlp.getAllIndexStartLocations(para, Pattern.compile("(?ism)dated?|between|among")).size() > 0) {
				// one of dated and among/between
//					mayBeInOpeningPara = true;
				// plus 1
				score = score + 3;
//					System.out.println("kN=" + kN + " mayBeInOpeningPara plus 1 score=" + score);
			}

			if (mapCount.containsKey(t.hashCode())) {

				score = score + Integer.parseInt(mapCount.get(t.hashCode())[1]) + 1;
//					System.out.println("count of times it kN occurred. kN=" + kN + " cnt="
//							+ mapCount.get(t.hashCode())[1] + " score=" + score);

				if (score < Integer.parseInt(mapCount.get(t.hashCode())[4])) {
					score = Integer.parseInt(mapCount.get(t.hashCode())[4]);
				}

				// [0]=kN,[1]=cnt,[2]=idx,%loc,score
				String[] ary = { kN, (Integer.parseInt(mapCount.get(t.hashCode())[1]) + 1) + "",
						mapCount.get(t.hashCode())[2], mapCount.get(t.hashCode())[3] + "", score + "" };
//				System.out.println("1. put in mapCoutn="+Arrays.toString(ary));
				mapCount.put(t.toLowerCase().hashCode(), ary);
				/*
				 * put confidence of opening para here - and don't override if high. Need to
				 * pull last instance and see if current is higher confidence. put % of
				 * text15percent earliest idx is found - if near end - then confi
				 */
			}
			if (!mapCount.containsKey(t.hashCode())) {

				// keep higher score. If w/n top 3% - plus 1%
				foundInThisTopPartOfText = (((Double.parseDouble(listContractNameLotto.get(b)[1])
						/ (double) text.length()) * 100));
				if (foundInThisTopPartOfText < 3) {
					score = score + 1;
//						System.out.println("foundInThisTopPartOfText. kN=" + kN + " %=" + (int) foundInThisTopPartOfText
//								+ " score=" + score);
				}

				String[] ary = { kN, "1", listContractNameLotto.get(b)[1], ((int) foundInThisTopPartOfText) + "",
						score + "" };
//				System.out.println("2. put in mapCoutn="+Arrays.toString(ary));
				mapCount.put(t.hashCode(), ary);
				// record initial confidence
				// [0]=kN,[1]=cnt,[2]=idx,%loc,score
			}

			// b/c I iterate multiple patterns results will be double/triple counted. To
			// prevent that the match is removed.
//				System.out.println("text15Percent.len bef=" + text15Percent.length());
//				System.out.println("listContractNameLotto.get(b)[0]===" + listContractNameLotto.get(b)[0]);
//				text = text.replace(listContractNameLotto.get(b)[0], "");
//				System.out.println("text15Percent.len aft=" + text15Percent.length());
		}

//		System.out.println("contract lotto - now at map");

		// [1]=count of how many times it occurred.
		// [2] is the first idx of where it occurred.
		// also peg if it was found near 'dated?' and/or between|almong

		score = 0;
		kN = "";

		double cntg = 0.0001;

		TreeMap<Double, String[]> mapC = new TreeMap<Double, String[]>();
		for (Map.Entry<Integer, String[]> entry : mapCount.entrySet()) {
			mapC.put(Double.parseDouble(entry.getValue()[1] + cntg), entry.getValue());
//			System.out.println("mapCount lotto=" + Arrays.toString(entry.getValue()));
			cntg = cntg + 0.0001;
		}

		cntg = 0.0001;
		TreeMap<Double, String[]> mapOrdered = new TreeMap<Double, String[]>();
		for (Map.Entry<Double, String[]> entry : mapC.entrySet()) {
			mapOrdered.put(-Double.parseDouble(entry.getValue()[4]) + cntg, entry.getValue());
			cntg = cntg + 0.0001;
//			System.out.println("mapOrdered lotto=" + Arrays.toString(entry.getValue()));
		}

		List<String[]> listContractLottoOrdered = new ArrayList<String[]>();
		for (Map.Entry<Double, String[]> entry : mapOrdered.entrySet()) {
			if (entry.getValue()[0].replaceAll("(?ism)(agreement|contract| and |OBx|CBx|xOB)+", "")
					.replaceAll("(?ism)(this|amended|amendment|restated|supplement|first|second|third|fourth)+", "")
					.trim().length() == 0)
				continue;
//			System.out.println("1. listContractLottoOrdered lotto=" + Arrays.toString(entry.getValue()));
			listContractLottoOrdered.add(entry.getValue());
		}

//		NLP.printListOfStringArray("contract name lotto list==", listContractLottoOrdered);

		return listContractLottoOrdered;

	}

	public static List<String[]> contractNameFinder(String text20Percent) throws IOException {

		NLP nlp = new NLP();
		GetContracts gK = new GetContracts();
		String kN = "", t = "", strBef = "", strAft = "", para = "";

		List<Pattern> listPatterns = new ArrayList<Pattern>();
		listPatterns.add(gK.patternContractNameThis);
		listPatterns.add(gK.patternContractNameByItselfHardReturns);
		listPatterns.add(gK.patternContractNameALLCAPSdated);
		listPatterns.add(gK.patternContractNameParenDefined);
		listPatterns.add(gK.patternContractNameAgreementHardReturns);
		listPatterns.add(gK.patternContractNameFromCoverPage);
		listPatterns.add(gK.patternOpinionInBody);

		boolean inOpeningParaHighConfidence = false;
//		System.out.println("contractNameFinder -- listPatterns.size=" + listPatterns.size());
		TreeMap<Integer, String[]> mapCount = new TreeMap<Integer, String[]>();
		int score = 0;
		double foundInThisTopPartOfText = 0;
		for (int d = 0; d < listPatterns.size(); d++) {
			List<String[]> list = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(text20Percent, listPatterns.get(d));
//			System.out.println("d=" + d + " listPatterns.size=" + listPatterns.size() + " list.size=" + list.size());
			for (int b = 0; b < list.size(); b++) {
				score = 0;
				inOpeningParaHighConfidence = false;
//				mayBeInOpeningPara = false;
//				System.out.println("b=" + b + " pattern#=" + d);
				kN = list.get(b)[0].trim();

				kN = kN.replaceAll(" \\(this \"Agreement\"\\)", "").replaceAll("\\)$", "xxCP")
						.replaceAll("^\\(", "OPxx").replaceAll("(?sm)This |THIS ", "").trim();
				t = kN.toLowerCase();
//				System.out.println("Kn?=="+t);
//				System.out.println("kFinder - textSnip="
//						+ text20Percent.substring(Math.max(0, Integer.parseInt(list.get(b)[1]) - 25),
//								Math.min(text20Percent.length(), Integer.parseInt(list.get(b)[2]) + 25)));
//				System.out.println("list (b)[2] len=" + list.get(b)[2] + " ?>text20%len=" + text20Percent.length());
				if (Integer.parseInt(list.get(b)[2]) > text20Percent.length())
					break;

				strBef = text20Percent.substring(Math.max(0, Integer.parseInt(list.get(b)[2]) - 500),
						Math.min(Integer.parseInt(list.get(b)[2]), text20Percent.length()));
				if (nlp.getAllIndexStartLocations(strBef, Pattern.compile("(\r\n).*?\r\n")).size() > 0) {
					strBef = strBef.substring(
							nlp.getAllIndexStartLocations(strBef, Pattern.compile("(\r\n).*?\r\n")).get(
									nlp.getAllIndexStartLocations(strBef, Pattern.compile("(\r\n).*?\r\n")).size() - 1),
							strBef.length());
				}

				if (nlp.getAllIndexStartLocations(strBef, Pattern.compile("(\r\n).*?\r\n")).size() == 0) {
					strBef = strBef.substring(strBef.lastIndexOf("\r\n") + 2, strBef.length());
				}

				if (Integer.parseInt(list.get(b)[2]) < text20Percent.length()) {
					strAft = text20Percent.substring(Integer.parseInt(list.get(b)[2]));
				}

				if (nlp.getAllIndexEndLocations(strAft, Pattern.compile("(?sm)(\r\n).*?\r\n")).size() > 0) {
					strAft = strAft.substring(0,
							nlp.getAllIndexEndLocations(strAft, Pattern.compile("\r\n.*?\r\n")).get(0));
//					System.out.println("1. strAft=" + strAft);
				}

				if (strAft.length() > (strAft.lastIndexOf("\r\n") + 2)) {
					strAft = strAft.substring(strAft.lastIndexOf("\r\n") + 2, strAft.length());
//					System.out.println("2. strAft="+strAft);
				}

				// System.out.println("textSnip=" + text.substring(Math.max(0,
				// Integer.parseInt(list.get(b)[1]) - 25),
				// Math.min(text.length(), Integer.parseInt(list.get(b)[2]) + 25)));
				// System.out.println("match=" + list.get(b)[0]);
				// System.out.println("strBef=" + strBef + "|end");
				// System.out.println("strAft=" + strAft + "|end");
//				System.out.println("strBef+aft=" + strBef + "<==b/w==>" + strAft + "||END");
				para = strBef + " " + strAft;
//				System.out.println("kN para=" + para.substring(0, Math.min(150, para.length())).trim());
				if (nlp.getAllIndexEndLocations(kN, Pattern.compile(
						"(?sm)Exhibit [A-Z]{1}|Schedule [A-Z]{1}|Annex [A-Z]{1}|Appendix [A-Z]{1}|Consoli|Means?|Preamble|Section|Article|EXHIBIT [A-Z]{1}|SCHEDULE [A-Z]{1}|ANNEX [A-Z]{1}|APPENDIX [A-Z]{1}|MEANS?|PREAMBLE|SECTION|ARTICLE"))
						.size() > 0
						|| nlp.getAllIndexEndLocations(kN, Pattern.compile("(?ism)\\.")).size() > 0
						|| !SolrPrep.isItInitialCapsAgreementId(
								GoLaw.patternStopWordsAggressiveNotForHtxtorDisplayData.matcher(kN).replaceAll(""))
						|| nlp.getAllMatchedGroups(kN.replaceAll("[^A-Z]", ""),
								Pattern.compile("(?sm)[A-Z]{1,3}[;: ,]{1}")).size() > 3
						|| nlp.getAllMatchedGroups(strAft, Pattern.compile("(?sm)(\r\n|^)+\\d")).size() > 3
//						strBef+aft=INDENTURE<==b/w==> dated as of February 1, 2013 between Netflix, IncxxPD, a Delaware corporation, 
//						and Wells Fargo Bank, National Association, a national banking association, as Trustee.||END
				) {
//					System.out.println("1 continuing. kN=" + kN + " kN sch?=" + nlp
//							.getAllIndexEndLocations(kN,
//									Pattern.compile(
//											"(?ism)exhibit|schedule|annex|appendix|means?|preamble|section|article"))
//							.size() + " \\. size="
//							+ nlp.getAllIndexEndLocations(kN, Pattern.compile("(?ism)\\.")).size() + " list.get(b)[1]="
//							+ list.get(b)[1] + " isItInitialCapsAgreementId="
//							+ SolrPrep.isItInitialCapsAgreementId(
//									GoLaw.patternStopWordsAggressiveNotForHtxtorDisplayData.matcher(kN).replaceAll(""))
//							+ " [A-Z]1,3 .size="
//							+ nlp.getAllMatchedGroups(kN.replaceAll("[^A-Z]", ""),
//									Pattern.compile("(?sm)[A-Z]{1,3}[;: ,]{1}")).size()
//							+ " strAft size=="
//							+ nlp.getAllMatchedGroups(strAft, Pattern.compile("(?sm)(\r\n|^)+\\d")).size());
					continue;
				}

				if (nlp.getAllMatchedGroups(strAft, Pattern.compile("(?ism)[\\d] ?$")).size() > 0
						&& !strAft.toLowerCase().contains("date")) {
//					System.out.println("1x1 continuing");
					continue;
				}

				if (nlp.getAllIndexStartLocations(para, Pattern.compile("(?ism)dated?|between|among")).size() > 1) {
					// both dated and among/between
					inOpeningParaHighConfidence = true;
					// plus 2
					score = score + 2;
//					System.out.println("kN=" + kN + " openPara plus 2 score=" + score);
				}

//				System.out.println("list.get(b)[0]="+list.get(b)[0]);
				if (para.replaceAll(list.get(b)[0].replaceAll("[\\\\\\}\\{\\[\\]\\(\\)\\*\\+-\\?\\!\\%\\^\\$]+", ""),
						"").trim().length() == 0) {
					score = score + 1;
//					System.out.println("kN=" + kN + " by itself plus 1 score=" + score);
				}

				if (list.get(b)[0].replaceAll("[A-Z ]+", "").length() == 0) {
					// plus1
					score = score + 1;
//					System.out.println("kN=" + kN + " all caps plus 1 score=" + score);
				}

				if (!inOpeningParaHighConfidence && nlp
						.getAllIndexStartLocations(para, Pattern.compile("(?ism)dated?|between|among")).size() > 0) {
					// one of dated and among/between
//					mayBeInOpeningPara = true;
					// plus 1
					score = score + 3;
//					System.out.println("kN=" + kN + " mayBeInOpeningPara plus 1 score=" + score);
				}

				if (mapCount.containsKey(t.hashCode())) {

					score = score + Integer.parseInt(mapCount.get(t.hashCode())[1]) + 1;
//					System.out.println("count of times it kN occurred. kN=" + kN + " cnt="
//							+ mapCount.get(t.hashCode())[1] + " score=" + score);

					if (score < Integer.parseInt(mapCount.get(t.hashCode())[4])) {
						score = Integer.parseInt(mapCount.get(t.hashCode())[4]);
					}

					// [0]=kN,[1]=cnt,[2]=idx,%loc,score
					String[] ary = { kN, (Integer.parseInt(mapCount.get(t.hashCode())[1]) + 1) + "",
							mapCount.get(t.hashCode())[2], mapCount.get(t.hashCode())[3] + "", score + "" };
//					System.out.println("3. put in mapCount=" + t);
					mapCount.put(t.toLowerCase().hashCode(), ary);
					/*
					 * put confidence of opening para here - and don't override if high. Need to
					 * pull last instance and see if current is higher confidence. put % of
					 * text15percent earliest idx is found - if near end - then confi
					 */
				}
				if (!mapCount.containsKey(t.hashCode())) {

					// keep higher score. If w/n top 3% - plus 1%
					foundInThisTopPartOfText = (((Double.parseDouble(list.get(b)[1]) / (double) text20Percent.length())
							* 100));
					if (foundInThisTopPartOfText < 3) {
						score = score + 1;
//						System.out.println("foundInThisTopPartOfText. kN=" + kN + " %=" + (int) foundInThisTopPartOfText
//								+ " score=" + score);
					}

					String[] ary = { kN, "1", list.get(b)[1], ((int) foundInThisTopPartOfText) + "", score + "" };
//					System.out.println("4. put in mapCoutn=" + Arrays.toString(ary));
					mapCount.put(t.hashCode(), ary);
					// record initial confidence
					// [0]=kN,[1]=cnt,[2]=idx,%loc,score
				}

				// b/c I iterate multiple patterns results will be double/triple counted. To
				// prevent that the match is removed.
//				System.out.println("text15Percent.len bef=" + text15Percent.length());
//				System.out.println("list.get(b)[0]===" + list.get(b)[0]);
//				text20Percent = text20Percent.replace(list.get(b)[0], "");
//				System.out.println("text15Percent.len aft=" + text15Percent.length());
			}
		}

//		System.out.println("1 now at map");

		// [1]=count of how many times it occurred.
		// [2] is the first idx of where it occurred.
		// also peg if it was found near 'dated?' and/or between|almong
//		NLP.printMapIntStringAry("mapCount=", mapCount);

		score = 0;
		kN = "";
		double cntg = 0.0001;

		TreeMap<Double, String[]> mapC = new TreeMap<Double, String[]>();
		for (Map.Entry<Integer, String[]> entry : mapCount.entrySet()) {
			mapC.put(Double.parseDouble(entry.getValue()[1] + cntg), entry.getValue());
//			System.out.println("mapCount=" + Arrays.toString(entry.getValue()));
			cntg = cntg + 0.0001;
		}

		cntg = 0.0001;

		TreeMap<Double, String[]> mapOrdered = new TreeMap<Double, String[]>();
		for (Map.Entry<Double, String[]> entry : mapC.entrySet()) {
//			System.out.println("mapC.... entry.getValue=" + Arrays.toString(entry.getValue()));
			mapOrdered.put(-Double.parseDouble(entry.getValue()[4]) + cntg, entry.getValue());
			cntg = cntg + 0.0001;
		}

//		System.out.println("done with mapC");

		List<String[]> listOrdered = new ArrayList<String[]>();
		for (Map.Entry<Double, String[]> entry : mapOrdered.entrySet()) {
//			if (entry.getValue()[0].replaceAll("(?ism)(agreement|contract)", "").trim().length() == 0)
//				continue;
			listOrdered.add(entry.getValue());
		}

//		NLP.printListOfStringArray("contractNameFinder - list==", listOrdered);
		return listOrdered;

	}

	public static boolean isGraphic(String text) throws IOException {
		NLP nlp = new NLP();
		boolean isGraphic = false;

		String[] lines = text.split("\r\n");
		for (int c = (lines.length - 1); c >= 0; c--) {
//			System.out.println("line=" + lines[c]);
			if (lines.length - c > 5 || isGraphic)
				break;
			if (nlp.getAllMatchedGroups(lines[c], Pattern.compile("(?i)<TYPE>GRAPHIC\r\n|<FILENAME>.{1,30}\\.jpg"))
					.size() > 0) {
				isGraphic = true;
//				System.out.println("isGraphic=" + isGraphic);
			}
		}

		return isGraphic;
	}

	public static String removePageNumber(String text) throws IOException, SQLException {

		// text = text.replaceAll("[\r\n]{3,50}", "\r\r");

		// gets rid of stranded period (period that starts line)
		text = text.replaceAll("[\r\n]{1,3}\\.", "\\.");
		text = text.replaceAll("(?sm)^[ \t]{0,50}<PAGE>[ \t]{1,40}-? ?[\\d]{1,3} ?-?[ \t]{0,40}$", "");

		StringBuilder sb = new StringBuilder();
		int sIdx = 0, eIdx = 0;

		SolrPrep.listTextExhMarkers = new ArrayList<>();
//	//System.out.println("K has now been stripped, page numbers have NOT been removed. \r\n"
//			+ "Now I create markers of page #s for use later in exhibit location. \r\n"
//			+ "this is embedded in method removePageNumbers SolrPrep.exhibitMarkers(text).\r\n"
//			+ "It is a lengthy method to run early in process as K might not be used -\r\n "
//			+ "but is necessary at this point. Once this method is done it populates the \r\n"
//			+ "public/global List - listTextExhMarkers which is then used later as it retans\r\n"
//			+ "the idx locations of the pg #s that change format - not eg A-2 thru A-10. \r\n"
//			+ "But last pg of each A-18, B-12. Later I use the locations of last pg format \r\n"
//			+ "to know that's where it is likely the next begings (e.g after A-18 - \r\n"
//			+ "last pg of Exhibit A) Exhibit B \r\n starts. This is a very complex method\r\n"
//			+ "- don't chg unless careful considerations. Important is this doesn't throw \r\n"
//			+ "off idx locations.");

		SolrPrep.listTextExhMarkers = SolrPrep.exhibitMarkers(text);
//	//nlp.printListOfStringArray("1a. @SolrPrep.exhibitMarkers(text) SolrPrep.listTextExhMarkers=", SolrPrep.listTextExhMarkers);

//	[sIdx=405267, pg=A-1, pg#=1, pgLtr=A, pg#Prior=, pgLtr=, consecutive pg Cnt prior to here=144]|
//	below shows that sIdx of B-1 is 435789 which is the end of pg18 of exh pg ltr A (A-18).
//	[435789, pg=B-1, pg#=1, pgLtr=B, pg#=18, pgLtr=A, consecutiveCnt=18]|
//	[441766, pg=B-4, pg#=4, pgLtr=B, pg#=3, pgLtr=B, consecutiveCnt=4]|

		// System.out.println("SolrPrep.listTextExhMarkers.size="+SolrPrep.listTextExhMarkers.size());
		for (int i = 0; i < SolrPrep.listTextExhMarkers.size(); i++) {
			sIdx = eIdx;
			eIdx = Integer.parseInt(SolrPrep.listTextExhMarkers.get(i)[0]);
			if (i + 1 == SolrPrep.listTextExhMarkers.size()) {
				eIdx = text.length();
			}
//		//System.out.println("listTextExhMarkers i="+i+" ary="+Arrays.toString(SolrPrep.listTextExhMarkers.get(i)));
			if (i == 0) {
				// System.out.println("i=0 substring=0,eIdx");
				sb.append(text.substring(0, eIdx) + "\r\nzc" + SolrPrep.listTextExhMarkers.get(i)[1].replace("pg=", "")
						+ "zc");
				continue;
			}

			sb.append(text.substring(sIdx, eIdx) + "\r\nzc" + SolrPrep.listTextExhMarkers.get(i)[1].replace("pg=", "")
					+ "zc");

		}

		// next I will check each zc...zc idx location and capture pg (A-1) then
		// remove zc...zc.
		if (SolrPrep.listTextExhMarkers.size() > 0)
			text = sb.toString();
//	pw = new PrintWriter(new File("e:/getContracts/temp/exhPgMarkers_zc.txt"));
//	pw.append(text);
//	pw.close();
		// if I remove hard returns I mess up capture of exhibits
//	List<String[]> list = NLP.getAllMatchedGroupsAndStartIdxLocs(text, patternPageNumber);

		text = joinTextOverPages(text);

		// TODO: figure out better method to conjoin text separated by pg#s. Do so based
		// on non-Initial Cap start of line on start of new page and prior page did not
		// end with a period of ';' or ':'. If conjoin the pages. But I need to leave a
		// mark in solr doc record so that sentence can be matched in kAnalzyer

		// String [] ary = {text};
		// list.add(ary);

		// Pattern patternJoinParaAfterPageNumber = Pattern
		// .compile("(?<!([;,\\.]|[,;]{1}
		// (or|and)))(?<=[a-z\\)]{1})[\r\n]{1,2}xxxx[\r]{1,3}(?!(\\([A-Za-z\\d]{1,5}\\)))");

		// PrintWriter tempPw3 = new PrintWriter(new File(
		// "e:/getContracts/temp/temp33.txt"));
		// tempPw3.append(text);
		// tempPw3.close();

		// //System.out.println("finished getContractTextOnly");

		return text;

	}

	public static String joinTextOverPages(String text) throws IOException {

		NLP nlp = new NLP();

//		Pattern patternPageNumberPlusBeforeAndAfterText = Pattern.compile("(?ism)( ?[^ \r\n\t]{1,20})+ ?" + "("
//				+ "([\r\n]{4,}[\t ]{0,100}[page]{0,4}[\\dixv\\-]{1,6}[\t ]{0,100}[\r\n]{4,})" + "|"
//				+ "([\r\n]{4,}[ ]{0,100}-? ?([\\diI]{1,4}|[A-B]{1}-?\\d) ?-?[ ]{0,100}[\r\n]{4,})" + ")"
//				+ "([ ]+([^ \r\n\t]{1,20} )+)");

		Pattern pattern1PgNoPlusTxt = Pattern.compile("(?ism)" + "( ?[^ \r\n\t]{1,20}+ ?)"// $1
				+ "([\r\n]{4,}[\t ]{0,100})"// $2
				+ "([page]{0,4}[\\dixv\\-]{1,6})"// $3
				+ "([\t ]{0,100}[\r\n]{4,})"// $4
				+ "([ ]+)"// $5
				+ "([^ \r\n\t]{1,20} )");// $6

		Pattern pattern2PgNoPlusTxt = Pattern.compile("(?ism)" + "( ?[^ \r\n\t]{1,20}+ ?)"// $1
				+ "([\r\n]{4,}[ ]{0,100})"// $2
				+ "(-? ?([\\diI]{1,4}|[A-B]{1}-?\\d) ?-?)"// $3
				+ "([ ]{0,100}[\r\n]{4,})"// $4
				+ "([\t ]{0,100}[\r\n]{4,}[ ]+)"// $5
				+ "([^ \r\n\t]{1,20} )");// $6
//$1$6
		Pattern patternPgNoPlusTxt = Pattern
				.compile(pattern1PgNoPlusTxt.pattern() + "|" + pattern2PgNoPlusTxt.pattern());

		Pattern patternEndOfSentence = Pattern.compile("(?ism)([\\.:]{1}|; ?)(and|or)?(  ?PgJoin)([\\d]+)");
//		+ "(-? ?([\\diI]{1,4}|[A-B]{1}-?\\d) ?-?)"// $3
//		+ "([page]{0,4}[\\dixv\\-]{1,6})"// $3

		// this will join text that was separated by page numbers and for purposes of
		// solr ingestion - mark location the page number was removed together with what
		// that pg no was.
		// if last character that precedes page number is: 1.'.', 2. ':' 3.';',
		// 4. '; (or|and)' or 5. '.' Then mark pg#. Otherwise leave hard return spacing
		// and mark pgNo.

		List<String> list = nlp.getAllMatchedGroups(text, patternPgNoPlusTxt);
//		NLP.printListOfString("listpgNos=", list);
		text = text.replaceAll(patternPgNoPlusTxt.toString(), "$1 PgJoin$3 $6");// marks the location the pg no was
																				// replaced.
		text = text.replaceAll("(PgJoin)([A-Z]-[\\d]{1,2} ?|-?[iv\\d]+-? ?)(\\(|\\[|\")", "\r\n\r\nEoS$2\r\n\r\n$3");// marks
																														// the
																														// location
																														// the
																														// pg
																														// no
																														// was
		// replaced.
		text = text.replaceAll(patternEndOfSentence.toString(), "$1\r\n\r\n$4EoS\r\n\r\n");// check if the pg no
																							// location was an end of
																							// sentence and if was marks
																							// it

		// TODO: add a rule that if pgJoin is followed by pgNo then capital letter to
		// treat as
		// EoS - eg EoSiv.

		return text;

	}

	public static String getOnlyAlphaCharacters(String text) {

		if (text == null)
			return null;
		text = text.replaceAll("\"", "").replaceAll("[^A-Za-z\\d]", "_").replaceAll("[_]{2,}", "_");

		return text;
	}

	public static boolean isDefPgNo(String text) throws IOException {
		NLP nlp = new NLP();
		if (nlp.getAllMatchedGroups(text, Pattern.compile("Definition|DEFINITION|Defined Terms|DEFINED TERMS"))
				.size() > 0) {
			// if prior pgNmb related to definitions section than the next pg # will be 10
			// or 20 or 30 or 40 pages after the def section. Therefore if the
			// prior toc pg# referred definitions section this is set to true so that if
			// next pg# is that far ahead it is still counted as confirmed. Comes aft map
			return true;
		} else {
			return false;// def section can be very long pg #3 and next section is pg#40. So allow flex
			// when this is true

		}

	}

	public static String getDefinedTerms(String text) throws IOException {

		StringBuffer sb = new StringBuffer();
		String definedTerm = null, firstLtr, prevMatch = null, definition;
		int ltr, priorLtr = 0, sIdx = 0, sIdxPrev = 0, cnt = 0;
		String solrDefTerm = "<field name=\"definedTerm\"><![CDATA[";
		String solrDef = "<field name=\"definition_";
		String solrEnd = "]]></field>";
		String solrStart = "\"><![CDATA[";

		Matcher match = patternDefinedWordInDefSec.matcher(text);
		while (match.find()) {
			sIdx = match.start();

			// //System.out.println("matched definedTerm=" + match.group()
			// + "|end matched definedTerm");

			firstLtr = match.group().replaceAll("[\"\r\n ]", "").substring(0, 1);
			ltr = getNumberInAlphabet(firstLtr);
			if (priorLtr > ltr) {
				definedTerm = prevMatch;
				definition = getLastDefinedTerm(text.substring(sIdxPrev), prevMatch, sIdxPrev);
				sb.append(solrDefTerm + definedTerm + solrEnd + "\r");
				sb.append((solrDef + getOnlyAlphaCharacters(definedTerm)) + solrStart + definition + solrEnd + "\r");

				// //System.out.println("definition="+definition);
			} else if (cnt > 0) {
				definedTerm = prevMatch;
				definition = text.substring(sIdxPrev, sIdx);
				sb.append(solrDefTerm + definedTerm + solrEnd + "\r");
				sb.append((solrDef + getOnlyAlphaCharacters(definedTerm)) + solrStart + definition + solrEnd + "\r");
				// //System.out.println("definition="+definition);
				// //System.out.println("definedTerm="+definedTerm+"
				// \rdefinition"+definition+"|defEnd");
			}

			priorLtr = ltr;
			sIdxPrev = sIdx;
			prevMatch = match.group().replaceAll("([\r\n\"]+|[\r\n]+[ ]+)", "").trim();
			// //System.out.println("definedTerm="+match.group().replaceAll("([\r\n\"]+|[\r\n]+[
			// ]+)", "").trim());
			cnt++;

		}

		// get last match
		definedTerm = prevMatch;
		definition = getLastDefinedTerm(text.substring(sIdx), prevMatch, sIdx);
		sb.append(solrDefTerm + definedTerm + solrEnd + "\r");
		sb.append((solrDef + getOnlyAlphaCharacters(definedTerm)) + solrStart + definition + solrEnd + "\r");
		// //System.out.println("definition="+definition);

		// //System.out.println("last definition="+definition+"|end last def");

		return sb.toString();

	}

	public static String getLastHeading(String text, String match, int idx) {

		// simplify to have it just be first instance of 2 hard returns that
		// follow first line with a \\. after match. If not found - then don't cut
		// anything;

		String definition = "";
		// //System.out.println("match at getLastDefinedTerm="+match);
		if (null == match)
			return null;

		String[] lines = text.substring(match.length(), Math.min(idx + 5000, text.length())).split("[\r\n]");
//		System.out.println("lastText==" + text.substring(idx, idx + 500));
		int cnt = 0;
		for (int i = 0; i < lines.length; i++) {
			System.out.println("lines[0]=" + lines[i]);
			if (i == 0) {
				System.out.println("continue....");
				definition = match + lines[i];
				continue;
			}

			if (i >= 2 && lines[i].replaceAll("[ \t]+", "").length() < 1) {
				cnt++;
			}
			if (i > 0 && lines[i].replaceAll("[ \t]{1,}", "").length() > 0) {
				definition = definition + "\r" + lines[i];
			}

			if (cnt > 1) {
				System.out.println("returned");
				break;
			}
		}

		System.out.println("lastHdg text returned=");
		return definition;
	}

	public static String getLastDefinedTerm(String text, String match, int idx) {

		// simplify to have it just be first instance of 2 hard returns that
		// follow first line with a \\. after match. If not found - then don't cut
		// anything;

		String definition = "";
		// //System.out.println("match at getLastDefinedTerm="+match);
		if (null == match)
			return null;

		String[] lines = text.substring(match.length(), Math.min(idx + 5000, text.length())).split("[\r\n]");
		// //System.out.println("text last=="
		// + text.substring(match.length(),
		// match.length()+50));
		int cnt = 0;
		for (int i = 0; i < lines.length; i++) {
			if (i == 0) {
				// System.out.println("lines[0]="+lines[i]);
				definition = match + lines[i];
				continue;
			}

			if (i >= 2 && lines[i].replaceAll("[ \t]+", "").length() < 1) {
				cnt++;
			}
			if (i > 0 && lines[i].replaceAll("[ \t]{1,}", "").length() > 0) {
				definition = definition + "\r" + lines[i];
			}

			if (cnt > 1)
				break;
		}

		return definition;
	}

	public static int getLastDefinedTermSimple(String[] ary, String text) {

		NLP nlp = new NLP();
		int sIdx = Integer.parseInt(ary[0]);
		int eIdx = Integer.parseInt(ary[1]);

		if (text.length() < eIdx)
			return eIdx;
		text = text.substring(sIdx, eIdx);
		if (nlp.getAllIndexEndLocations(text, Pattern.compile("[\r\n]{2}[\t ]{0,50}[\r\n]{2}")).size() > 0) {
			eIdx = sIdx + nlp.getAllIndexEndLocations(text, Pattern.compile("[\r\n]{2}[\t ]{0,50}[\r\n]{2}")).get(0);
//			System.out.println("getLastDefinedTermSimple. got eIdx = " + eIdx);
		} else {
//			System.out.println("getLastDefinedTermSimple. did not get eIdx. so returning this eIdx==" + eIdx);
			return eIdx;
		}

		return eIdx;

	}

	public static int getNumberInAlphabet(String letter) {
		int ltrNo = -1;

		// //System.out.println("letter="+letter);

		if (letter.matches("\\d"))
			ltrNo = 0;

		if (letter.toUpperCase().equals("A"))
			ltrNo = 1;
		if (letter.toUpperCase().equals("B"))
			ltrNo = 2;
		if (letter.toUpperCase().equals("C"))
			ltrNo = 3;
		if (letter.toUpperCase().equals("D"))
			ltrNo = 4;
		if (letter.toUpperCase().equals("E"))
			ltrNo = 5;
		if (letter.toUpperCase().equals("F"))
			ltrNo = 6;
		if (letter.toUpperCase().equals("G"))
			ltrNo = 7;
		if (letter.toUpperCase().equals("H"))
			ltrNo = 8;
		if (letter.toUpperCase().equals("I"))
			ltrNo = 9;
		if (letter.toUpperCase().equals("J"))
			ltrNo = 10;
		if (letter.toUpperCase().equals("K"))
			ltrNo = 11;
		if (letter.toUpperCase().equals("L"))
			ltrNo = 12;
		if (letter.toUpperCase().equals("M"))
			ltrNo = 13;
		if (letter.toUpperCase().equals("N"))
			ltrNo = 14;
		if (letter.toUpperCase().equals("O"))
			ltrNo = 15;
		if (letter.toUpperCase().equals("P"))
			ltrNo = 16;
		if (letter.toUpperCase().equals("Q"))
			ltrNo = 17;
		if (letter.toUpperCase().equals("R"))
			ltrNo = 18;
		if (letter.toUpperCase().equals("S"))
			ltrNo = 19;
		if (letter.toUpperCase().equals("T"))
			ltrNo = 20;
		if (letter.toUpperCase().equals("U"))
			ltrNo = 21;
		if (letter.toUpperCase().equals("V"))
			ltrNo = 22;
		if (letter.toUpperCase().equals("W"))
			ltrNo = 23;
		if (letter.toUpperCase().equals("X"))
			ltrNo = 24;
		if (letter.toUpperCase().equals("Y"))
			ltrNo = 25;
		if (letter.toUpperCase().equals("Z"))
			ltrNo = 26;
//	//System.out.println("ltrNo="+ltrNo);

		return ltrNo;

	}

	public static int getNumberInAlphabet10s(String letter) {
		int ltrNo = -1;

		// //System.out.println("letter="+letter);

		if (letter.matches("\\d"))
			ltrNo = 10;

		if (letter.toUpperCase().equals("A"))
			ltrNo = 11;
		if (letter.toUpperCase().equals("B"))
			ltrNo = 12;
		if (letter.toUpperCase().equals("C"))
			ltrNo = 13;
		if (letter.toUpperCase().equals("D"))
			ltrNo = 14;
		if (letter.toUpperCase().equals("E"))
			ltrNo = 15;
		if (letter.toUpperCase().equals("F"))
			ltrNo = 16;
		if (letter.toUpperCase().equals("G"))
			ltrNo = 17;
		if (letter.toUpperCase().equals("H"))
			ltrNo = 18;
		if (letter.toUpperCase().equals("I"))
			ltrNo = 19;
		if (letter.toUpperCase().equals("J"))
			ltrNo = 20;
		if (letter.toUpperCase().equals("K"))
			ltrNo = 21;
		if (letter.toUpperCase().equals("L"))
			ltrNo = 22;
		if (letter.toUpperCase().equals("M"))
			ltrNo = 23;
		if (letter.toUpperCase().equals("N"))
			ltrNo = 24;
		if (letter.toUpperCase().equals("O"))
			ltrNo = 25;
		if (letter.toUpperCase().equals("P"))
			ltrNo = 26;
		if (letter.toUpperCase().equals("Q"))
			ltrNo = 27;
		if (letter.toUpperCase().equals("R"))
			ltrNo = 28;
		if (letter.toUpperCase().equals("S"))
			ltrNo = 29;
		if (letter.toUpperCase().equals("T"))
			ltrNo = 30;
		if (letter.toUpperCase().equals("U"))
			ltrNo = 31;
		if (letter.toUpperCase().equals("V"))
			ltrNo = 32;
		if (letter.toUpperCase().equals("W"))
			ltrNo = 33;
		if (letter.toUpperCase().equals("X"))
			ltrNo = 34;
		if (letter.toUpperCase().equals("Y"))
			ltrNo = 35;
		if (letter.toUpperCase().equals("Z"))
			ltrNo = 36;
//	//System.out.println("ltrNo="+ltrNo);

		return ltrNo;

	}

	public static boolean itHasGoverningLaw(String text) throws IOException {
//		System.out.println("finding governing law. text.len="+text.length());
		NLP nlp = new NLP();
		Pattern patternGoverned = Pattern.compile("(?ism)governed|constru|govern.{1,6}law");
		Pattern patternLaw = Pattern.compile("(?ism)[\r\n ]{1}laws? ");
//		System.out.println("governed=?"+nlp.getAllMatchedGroups(text, patternGoverned).size());
//		System.out.println("laws=?"+nlp.getAllMatchedGroups(text, patternLaw).size());

		if (nlp.getAllMatchedGroups(text, patternGoverned).size() > 0
				&& nlp.getAllMatchedGroups(text, patternLaw).size() > 0) {
//			System.out.println("found governing law");
			return true;
		}

		else {
			return false;
		}

	}

	public static void copySecZipFiles(int yr, int qtr) throws IOException {

		NLP nlp = new NLP();
		File folder = new File("e:/secZipFiles/");
		File[] listOfFiles = folder.listFiles();
		File file = new File("");
		String filename = "", moStr = "", yrStr = "";

		// //System.out.println("listOfFiles.len="+listOfFiles.length);
		for (int i = 0; i < listOfFiles.length; i++) {
			file = listOfFiles[i];
			filename = file.getName();

			if (filename.length() > 7
					&& nlp.getAllMatchedGroups(filename.substring(0, 8), Pattern.compile("[\\d]{8}")).size() > 0) {
				// //System.out.println("filename="+filename);
				moStr = filename.substring(4, 6);
				yrStr = filename.substring(0, 4);
				if (yrStr.equalsIgnoreCase(yr + "") && qtr < 4
						&& (moStr.equalsIgnoreCase("0" + (qtr * 3)) || moStr.equalsIgnoreCase("0" + (qtr * 3 - 1))
								|| moStr.equalsIgnoreCase("0" + (qtr * 3 - 2)))) {
					// System.out.println("yr=" + filename.substring(0, 4) + "\rmo=" +
					// filename.substring(4, 6));
					// System.out.println("secZipFolder + filename=" + secZipFolder + "||" +
					// filename);
					File targetFilename = new File(secZipFolder + filename);
					FileSystemUtils.copyFile(file, targetFilename);

				}

				if (yrStr.equalsIgnoreCase(yr + "") && qtr == 4 && (moStr.equalsIgnoreCase("10")
						|| moStr.equalsIgnoreCase("11") || moStr.equalsIgnoreCase("12"))) {
					// System.out.println("yr=" + filename.substring(0, 4) + "\rmo=" +
					// filename.substring(4, 6));
					// System.out.println("secZipFolder + filename=" + secZipFolder + "||" +
					// filename);
					File targetFilename = new File(secZipFolder + filename);
					FileSystemUtils.copyFile(file, targetFilename);

				}
			}
		}
	}

	public static void deleteSecZipFiles(int yr, int qtr, String path) throws IOException {

		NLP nlp = new NLP();
		File folder = new File(path);
//	//System.out.println("path="+path);
		File[] listOfFiles = folder.listFiles();
		File file = new File("");
		String filename = "", moStr = "", yrStr = "";

//	//System.out.println(
//			"listOfFiles.len=" + listOfFiles.length + " delete files from this folder yr=" + yr + " qtr=" + qtr);

		if (listOfFiles == null)
			return;

		for (int i = 0; i < listOfFiles.length; i++) {
			file = listOfFiles[i];
			filename = file.getName();
//		//System.out.println("filename="+filename+" filename.substring(0, 8)="+filename.substring(0, 8));
			if (filename.length() > 7
					&& nlp.getAllMatchedGroups(filename.substring(0, 8), Pattern.compile("[\\d]{8}")).size() > 0) {
				moStr = filename.substring(4, 6);
				yrStr = filename.substring(0, 4);
				if (yrStr.equalsIgnoreCase(yr + "") && qtr < 4
						&& (moStr.equalsIgnoreCase("0" + (qtr * 3)) || moStr.equalsIgnoreCase("0" + (qtr * 3 - 1))
								|| moStr.equalsIgnoreCase("0" + (qtr * 3 - 2)))) {
					// System.out.println("file to delete=" + file.getAbsolutePath());
					if (file.exists())
						file.delete();
				}

				if (yrStr.equalsIgnoreCase(yr + "") && qtr == 4 && (moStr.equalsIgnoreCase("10")
						|| moStr.equalsIgnoreCase("11") || moStr.equalsIgnoreCase("12"))) {
					// System.out.println("file to delete=" + file.getAbsolutePath());
					if (file.exists())
						file.delete();

				}
			}
		}
	}

	public static void deleteFilesSimple(String fullPath) {

		File folder = new File(fullPath);
		File[] listOfFiles = folder.listFiles();

		if (listOfFiles == null)
			return;

		for (int i = 0; i < listOfFiles.length; i++) {

			// System.out.println("deleting file="+listOfFiles[i].getAbsolutePath());
			listOfFiles[i].delete();

		}
	}

	public static void deleteFilesGetContracts(int yr, int qtr, String path, Pattern pattern) throws IOException {

		NLP nlp = new NLP();
		String fullPath = path + "/" + yr + "/qtr" + qtr + "/";
//	String fullPath = path + "/qtr" + qtr + "/";// 
//	IF DOWNLOAD FOLDER DON'T INCLUDE YR IN FULLPATH

		// System.out.println("fullPath="+fullPath);
		File folder = new File(fullPath);
		File[] listOfFiles = folder.listFiles();
//	File file = new File("");
		String filename = "";
//	 //System.out.println("listOfFiles.len=" + listOfFiles.length);

		if (listOfFiles == null)
			return;

		// System.out.println("listOfFiles.len=" + listOfFiles.length);

		for (int i = 0; i < listOfFiles.length; i++) {
//		 if
//		 (listOfFiles[i].getAbsolutePath().toString().toLowerCase().contains("other"))
//		 {
			File fpath = new File(listOfFiles[i].getAbsolutePath());
			filename = listOfFiles[i].getName().toString();
			// System.out.println("filename="+listOfFiles[i].getAbsolutePath());
			if (nlp.getAllMatchedGroups(listOfFiles[i].getAbsolutePath(), pattern).size() > 0) {
				// System.out.println("file.delete=" + listOfFiles[i].getAbsolutePath());
				listOfFiles[i].delete();
			}
//	}
		}
	}

	public static List<String> getStandfordSentence(String text) {

		Properties props = new Properties();
		props.setProperty("annotators", "tokenize" + ", ssplit");

		List<String> list = new ArrayList<>();
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		Annotation annotation = new Annotation(text);
		pipeline.annotate(annotation);
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			list.add(sentence.toString());
		}

		return list;

	}

	public static String cleanMetadata(String filename) throws IOException {
		NLP nlp = new NLP();
		String text = Utils.readTextFromFile(filename);
//		System.out.println(text);
		StringBuilder sb = new StringBuilder();

		String kId = "" + "" + "", fDate = "", fSize = "", acc = "", cik = "", formType = "", companyName = "",
				acc_link_filename = "", edgarLink = "", contractLongName = "", type = "", numberOfLegalEntities = "",
				legalEntitiesInOpeningParagraph = "", legalEntities = "", openingParagraph = "", contractNameAlgo = "",
				openParaContractName = "", score = "", governingLaw = "", parentGroup = "", contract_type = "",
				ticker = "", sector = "", industry = "";

		kId = getField(text, "kId=").replaceAll("[\",]+", "") + ",";
		fDate = getField(text, "fDate=").replaceAll("[\",]+", "") + ",";
		fSize = getField(text, "fSize=").replaceAll("[\",]+", "") + ",";
		acc = getField(text, "acc=").replaceAll("[\",]+", "") + ",";
		cik = getField(text, "cik=").replaceAll("[\",]+", "") + ",";
		formType = getField(text, "formType=").replaceAll("[\",]+", "") + ",";
		companyName = getField(text, "companyName=").replaceAll("[\",]+", "") + ",";
		acc_link_filename = getField(text, "acc_link_filename=").replaceAll("[\",]+", "") + ",";
		edgarLink = getField(text, "edgarLink=").replaceAll("[\",]+", "") + ",";
		contractLongName = getField(text, "contractLongName=").replaceAll("[\",]+", "") + ",";
		type = getField(text, "type=").replaceAll("[\",]+", "") + ",";
		numberOfLegalEntities = getField(text, "numberOfLegalEntities=").replaceAll("[\",]+", "") + ",";
		legalEntitiesInOpeningParagraph = getField(text, "legalEntitiesInOpeningParagraph=").replaceAll("[\",]+", "")
				+ ",";
		legalEntities = getField(text, "legalEntities=").replaceAll("[\",]+", "") + ",";
		openParaContractName = getField(text, "openParaContractName=").replaceAll("[\",]+", "") + ",";
		openingParagraph = getField(text, "openingParagraph=").replaceAll("[\",]+", "") + ",";
		governingLaw = getField(text, "governingLaw=").replaceAll("[\",]+", "") + ",";
//		+ "\r\nparentGroup=" + parentGroup
//		+ "\r\nticker=" + ticker
//		+ "\r\nsctr=" + sector
//		+ "\r\nids=" + industry

		if (nlp.getAllMatchedGroups(text, Pattern.compile("(?<=contractNameAlgo=)")).size() > 0) {
			contractNameAlgo = nlp.getAllMatchedGroups(text, Pattern.compile("(?<=contractNameAlgo=).*?(?=cnt|[\r\n])"))
					.get(0).replaceAll("[\",]+", "") + ",";
		} else {
			contractNameAlgo = ",";
		}

//		System.out.println("contractNameAlgo=" + contractNameAlgo);

		if (nlp.getAllMatchedGroups(text, Pattern.compile("(?sm)(?<=score=).*?(?=[\r\n])")).size() > 0)
			score = nlp.getAllMatchedGroups(text, Pattern.compile("(?sm)(?<=score=).*?(?=[\r\n])")).get(0)
					.replaceAll("[\",]+", "") + ",";
		else
			score = ",";

		sb.append(kId).append(fDate).append(fSize).append(acc).append(cik).append(formType).append(companyName)
				.append(acc_link_filename).append(edgarLink).append(contractLongName).append(type)
				.append(legalEntitiesInOpeningParagraph).append(numberOfLegalEntities).append(openingParagraph)
				.append(openParaContractName).append(contractNameAlgo).append(score).append(legalEntities)
				.append(governingLaw);
		String csvText = sb.toString().replaceAll("[\\|]+", "") + "\n";

		return csvText;
	}

	public static String getField(String text, String fieldMarker) throws IOException {
		NLP nlp = new NLP();

		String field = "";
//		System.out.println(text);
//		System.out.println(nlp.getAllMatchedGroups(text, Pattern.compile("(?<=" + fieldMarker + ").*?(?=[\r\n])")).get(0));
		if (nlp.getAllMatchedGroups(text, Pattern.compile("(?sm)(?<=" + fieldMarker + ").*?(?=[\r\n])")).size() > 0) {
			field = nlp.getAllMatchedGroups(text, Pattern.compile("(?sm)(?<=" + fieldMarker + ").*?(?=[\r\n])")).get(0);
		}
		return field;
	}

	public static void loadIntoMysql(String filename, String table) throws FileNotFoundException, SQLException {

		File file = new File(filename);
		long size = file.length();
		// System.out.println("loadFileIntoMysql fileSize:: " + size);
		String query = "SET GLOBAL local_infile =1;"

				+ "\r\n" + "LOAD DATA INFILE '" + filename + "' IGNORE INTO TABLE " + table
				+ " FIELDS TERMINATED BY ',' " + "lines terminated by " + "'\\n'" + ";";
//		 System.out.println("mysql query::" + query);
//		try {
		MysqlConnUtils.executeQuery(query);
//		} catch (SQLException e) {
//			e.printStackTrace(System.out);
//		}
	}

	public static void importMetaDataIntoMySQL(String folder, int startYear, int endYear, int startQtr, int endQtr,
			int batchAmount) throws IOException, SQLException {

		String createTmpMetadataTble = "DROP TABLE IF EXISTS"
				+ "`contracts`.`tmp_metadata`;\r\nCREATE TABLE `contracts`.`tmp_metadata` (\r\n"
				+ "  `kId` varchar(24) NOT NULL DEFAULT '0',\r\n" + "  `fDate` date NOT NULL DEFAULT '1901-01-01',\r\n"
				+ "  `fSize` int DEFAULT NULL,\r\n" + "  `acc` varchar(20) DEFAULT NULL,\r\n"
				+ "  `cik` int DEFAULT NULL,\r\n" + "  `formType` varchar(50) DEFAULT NULL,\r\n"
				+ "  `companyName` varchar(155) DEFAULT NULL,\r\n"
				+ "  `acc_link_filename` varchar(100) DEFAULT NULL,\r\n"
				+ "  `edgarLink` varchar(250) DEFAULT NULL,\r\n" + "  `contractLongName` varchar(250) DEFAULT NULL,\r\n"
				+ "  `type` varchar(50) DEFAULT NULL,\r\n" + "  `legalEntitiesInOpeningParagraph` text,\r\n"
				+ "  `numberOfLegalEntities` int DEFAULT NULL,\r\n" + "  `openingParagraph` text,\r\n"
				+ "  `openParaContractName` varchar(255) DEFAULT NULL,\r\n"
				+ "  `contractNameAlgo` varchar(255) DEFAULT NULL,\r\n" + "  `score` int DEFAULT NULL,\r\n"
				+ "  `legalEntities` text,\r\n" + "  `governinglaw` varchar(155) DEFAULT NULL\r\n"
				+ ") ENGINE=InnoDB DEFAULT CHARSET=latin1;\r\n" + "";

		MysqlConnUtils.executeQuery(createTmpMetadataTble);

		int cnt = 0;
		File f1 = new File("c://temp//tmpCsv.txt");
		PrintWriter pw = new PrintWriter(f1);
		StringBuilder sb = new StringBuilder();
		for (; startYear <= endYear; startYear++) {
			for (; startQtr <= endQtr; startQtr++) {

				// will retrieve masterIdx if not in folder or current
				String localPath = folder + "/" + startYear + "/QTR" + startQtr + "/";
				System.out.println("localPath=" + localPath);
				File files = new File(localPath);
				File[] listOfFiles = files.listFiles();
				if (null == listOfFiles || listOfFiles.length == 0)
					continue;

				for (int n = 0; n < listOfFiles.length; n++) {
					cnt++;
					sb.append(cleanMetadata(listOfFiles[n].getAbsolutePath()));
					if (cnt >= batchAmount || n + 1 == listOfFiles.length) {
						f1 = new File("c://temp//tmpCsv.txt");
						if (f1.exists())
							f1.delete();

						pw = new PrintWriter(f1);
						pw.append(sb.toString());
						pw.close();
						System.out.println("file#=" + n + " " + listOfFiles[n].getAbsolutePath());
						loadIntoMysql(f1.getAbsolutePath().replaceAll("\\\\", "//"), "`contracts`.`tmp_metadata`");
						cnt = 0;
						sb = new StringBuilder();
					}
				}

				MysqlConnUtils.executeQuery(
						"insert ignore into  `contracts`.`metadata` select * from `contracts`.`tmp_metadata`;\n" + "");
				MysqlConnUtils.executeQuery(createTmpMetadataTble);
			}

			startQtr = 1;
			MysqlConnUtils.executeQuery(
					"insert ignore into  `contracts`.`metadata` select * from `contracts`.`tmp_metadata`;\n" + "");
			MysqlConnUtils.executeQuery(createTmpMetadataTble);
		}
		MysqlConnUtils.executeQuery(
				"insert ignore into  `contracts`.`metadata` select * from `contracts`.`tmp_metadata`;\n" + "");
		MysqlConnUtils.executeQuery(createTmpMetadataTble);

	}

	public static void exportAllMetadataToSolrJsonFile(String filename, int startYr, int endYr)
			throws IOException, SQLException {

		int yr = startYr;

		String originalFilename = filename;
		for (; yr <= endYr; yr++) {
			filename = originalFilename + yr + "_metaData.json";
			File file = new File(filename);
			if (file.exists())
				file.delete();

			String query = "select \r\n" + "\r\n"
					+ "'{\\\"kId\\\":\\\"',kId,'\\\",','\\\"fDate\\\":',fDate,',','\\\"fSize\\\":',fSize,',',\r\n"
					+ "'\\\"acc\\\":\\\"',acc,'\\\",',\r\n" + "'\\\"cik\\\":',cik,',',\r\n"
					+ "'\\\"formType\\\":\\\"',formType,'\\\",',\r\n"
					+ "'\\\"companyName\\\":\\\"',companyName,'\\\",',\r\n"
					+ "'\\\"acc_link_filename\\\":\\\"',acc_link_filename,'\\\",',\r\n"
					+ "'\\\"edgarLink\\\":\\\"',edgarLink,'\\\",',\r\n"
					+ "'\\\"contractLongName\\\":\\\"',contractLongName,'\\\",',\r\n"
					+ "'\\\"type\\\":\\\"',type,'\\\",',\r\n"
					+ "'\\\"legalEntitiesInOpeningParagraph\\\":\\\"',legalEntitiesInOpeningParagraph,'\\\",',\r\n"
					+ "'\\\"numberOfLegalEntities\\\":',numberOfLegalEntities,',',\r\n"
					+ "'\\\"openingParagraph\\\":\\\"',openingParagraph,'\\\",',\r\n"
					+ "'\\\"openParaContractName\\\":\\\"',openParaContractName,'\\\",',\r\n"
					+ "'\\\"contractNameAlgo\\\":\\\"',contractNameAlgo,'\\\",',\r\n"
					+ "'\\\"score\\\":',score,',',\r\n" + "'\\\"legalEntities\\\":\\\"',legalEntities,'\\\"}'\r\n"
					+ "\r\n" + " from metadata where year(fDate)=" + yr + " INTO OUTFILE '" + filename + "' ;\r\n" + "";

			MysqlConnUtils.executeQueryDB(query, "contracts");

			String text = Utils.readTextFromFile(file.getAbsolutePath());

			text = text.replaceAll("\t", " ").replaceAll("\\}", "\\},").replaceAll("\" | \"", "\"")
					.replaceAll(",\"", ",\r\n\"").trim();

			if (text.length() == 0)
				continue;
			text = "[" + text.substring(0, text.length() - 1) + "]";
			if (file.exists())
				file.delete();

			PrintWriter pw = new PrintWriter(file);
			pw.append(text);
			pw.close();

		}

	}

	public static boolean regexProximity(String sentence, String closeWords, int withinTheseNumberOfWords) {

		String word = "", wordStart = "";
		int wordStartLoc = -1;
		String[] closeWordsSplit = closeWords.split(" ");
		wordStart = closeWordsSplit[0].replaceAll("[^A-Za-z]", "").trim();

		String[] sentenceSplit = sentence.split(" ");

		for (int i = 0; i < sentenceSplit.length; i++) {
			word = sentenceSplit[i].replaceAll("[^A-Za-z]", "").trim();
			if (word.equals(wordStart))
				wordStartLoc = i;
		}
		if (wordStartLoc < 0)
			return false;

		boolean itIsproximate = false;
		for (int i = 1; i < closeWordsSplit.length; i++) {
			word = closeWordsSplit[i];
			itIsproximate = getWordNumberInText(sentence, wordStartLoc, word, withinTheseNumberOfWords);
			if (!itIsproximate)
				return false;
		}

		return true;
	}

	public static boolean getWordNumberInText(String sentence, int wordStartLoc, String word,
			int withinTheseNumberOfWords) {

		String wordSent = "";
		String[] sentenceSplit = sentence.split(" ");
		for (int i = 0; i < sentenceSplit.length; i++) {

			wordSent = sentenceSplit[i].replaceAll("[^A-Za-z]", "");
			if (word.equals(wordSent) && Math.abs(wordStartLoc - i) <= withinTheseNumberOfWords) {
				System.out.println(" wordSent=" + wordSent + " wordToMeas=" + word + "wordStartLoc=" + wordStartLoc
						+ " wordSentLoc=" + i);
				return true;

			}
		}

		return false;
	}

	public static void getGlobalMapOfKidsParsedFile(File f) throws IOException {

		mapOfKidsParsed.put(f.getName().substring(0, f.getName().indexOf(" ")).trim(), "");

	}

	public static void getGlobalMapOfKidsParsedFolderOrFile(File fileOrFolder) throws IOException {
		System.out.println("clean json. folder=" + fileOrFolder.getAbsolutePath());
		if (fileOrFolder.isFile()) {
			getGlobalMapOfKidsParsedFile(fileOrFolder);
			return;
		}
		File[] files = fileOrFolder.listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				getGlobalMapOfKidsParsedFolderOrFile(f);
			} else {
				getGlobalMapOfKidsParsedFile(f);
			}
		}
	}

	public static void getGlobalMapOfKidsStrippedFile(File f) throws IOException {

		mapOfKidsStripped.put(f.getName().substring(0, f.getName().indexOf(" ")).trim(), "");

	}

	public static void getGlobalMapOfKidsStrippedFolderOrFile(File fileOrFolder) throws IOException {
		System.out.println("clean json. folder=" + fileOrFolder.getAbsolutePath());
		if (fileOrFolder.isFile()) {
			getGlobalMapOfKidsStrippedFile(fileOrFolder);
			return;
		}
		File[] files = fileOrFolder.listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				getGlobalMapOfKidsStrippedFolderOrFile(f);
			} else {
				getGlobalMapOfKidsStrippedFile(f);
			}
		}
	}

	
}
