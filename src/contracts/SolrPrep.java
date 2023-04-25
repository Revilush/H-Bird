package contracts;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import charting.FileSystemUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import search.SolrJClient;
import xbrl.ContractNLP;
import xbrl.ContractParser;
import xbrl.MysqlConnUtils;
import xbrl.NLP;
import xbrl.Utils;

public class SolrPrep {

	public static TreeMap<Double, String[]> mapIdx = new TreeMap<Double, String[]>();
	public static TreeMap<Integer, String[]> mapDefAdded = new TreeMap<Integer, String[]>();
	public static String contractType = "";
	public static String contractLongName = "";
	public static String paraGraphText = "";
	public static int maxStrippedFileSize = 5000000;
	// public static String downloadPath = "e:/downLoaded/";
	public static String folderDownload = "e:/getContracts/solrIngestion/downLoaded/";
	// C:\getContracts\solrIngestion\downloaded\1901\QTR1
	public static String folderStripped = "e:/getContracts/solrIngestion/solrStripped/";
	// public static String SectionsXMLPath =
	// "e:/getContracts/solrIngestion/solrSections/";
	public static String folderSolrIngestion = "e:/getContracts/solrIngestion/";

	public static String folderSectionsXml = "e:/getContracts/solrIngestion/solrSections/";
	public static String folderParagraphsXml = "e:/getContracts/solrIngestion/solrParagraphs/";
	public static String folderSentencesXml = "e:/getContracts/solrIngestion/solrSentences/";
	public static String folderParentChildXml = "e:/getContracts/solrIngestion/solrParentChild/";
	public static String folderParentChildSentenceXml = "e:/getContracts/solrIngestion/solrParentChildSentence/";
	public static String folderClausesXml = "e:/getContracts/solrIngestion/solrClausesFromSentAndPara/";
	public static String folderDocsWithErrorsXML = "e:/getContracts/solrIngestion/solrDocsWithErrors";

	public static String folderClientSectionsXml = "e:/getContracts/solrIngestion/solrClientSections/";
	public static String folderClientParagraphsXml = "e:/getContracts/solrIngestion/solrClientParagraphs/";
	public static String folderClientSentencesXml = "e:/getContracts/solrIngestion/solrClientSentences/";
	public static String folderClientParentChildXml = "e:/getContracts/solrIngestion/solrClientParentChild/";
	public static String folderClientParentChildSentenceXml = "e:/getContracts/solrIngestion/solrClientParentChildSentence/";
	public static String folderClientClausesXml = "e:/getContracts/solrIngestion/solrClientClausesFromSentAndPara/";

//	public static String folderSolrXml = "e:/getContracts/solrIngestion/solrXmlFiles/";
	// public static String sectionsXMLPath =
	// "e:/getContracts/solrIngestion/solrSections/";
//	public static String solrFieldstemStop = "e:/getContracts/solrIngestion";

	public static String solrFieldStop = "\r\n<field name=\"S\">";
	public static String solrFieldStopStem = "\r\n<field name=\"SS\">";
	public static String solrFieldNoDef = "\r\n<field name=\"ND\">";
	public static String solrFieldLeadNoDef = "\r\n<field name=\"LND\">";
	public static String solrFieldStopNoDefStem = "\r\n<field name=\"SNDS\">";
	public static String solrFieldPOS = "\r\n<field name=\"POS\">";
//	public static String solrFieldStopPOS = "\r\n<field name=\"SP\">";
//	public static String solrFieldStopNoDefPOS = "\r\n<field name=\"SNDP\">";

//	public static String solrFieldLeadStop = "\r\n<field name=\"LS\">";
//	public static String solrFieldLeadStopStem = "\r\n<field name=\"LSS\">";
//	public static String solrFieldLeadStopNoDefStem = "\r\n<field name=\"LSNDS\">";
//	public static String solrFieldLeadPOS = "\r\n<field name=\"LPOS\">";
//	public static String solrFieldLeadStopPOS = "\r\n<field name=\"LSP\">";
//	public static String solrFieldLeadStopNoDefPOS = "\r\n<field name=\"LSNDP\">";

	public static String solrFieldDoc = "<doc>";
	public static String solrFieldComment = "\r\n<field name=\"comment\">";
	public static String solrFieldDocEnd = "\r\n</doc>\r\n\r\n";
	public static String solrFieldAdd = "<add>\r\n";
	public static String solrFieldAddEnd = "</add>\r\n";
	public static String solrFieldId = "\r\n<field name=\"id\">";
	public static String solrFieldKid = "\r\n<field name=\"kId\">";
	public static String solrFieldWordCount = "\r\n<field name=\"wCnt\">";
	public static String solrFieldHomogStrWordCount = "\r\n<field name=\"homogStrwCnt\">";
	public static String solrFieldLead = "\r\n<field name=\"lead\">";
	public static String solrFieldClause = "\r\n<field name=\"clause\">";
	// public static String solrFieldSection = "\r\n<field name=\"section\">";
	public static String solrFieldText = "\r\n<field name=\"txt\">";
	// txt is primary searched query - eg the sentence or section or paragraph or
	// parent child text
	public static String solrFieldDuplicate = "\r\n<field name=\"dupe\">";
	// sentence is same as para=0, clause same as sentence =0, par/chi same as para
	// or sent = 0 if they not the same (ie 2 or more sentences to 1 para then
	// same=1. Or 2 clauses relate to 1 sentence
	public static String solrFieldTextType = "\r\n<field name=\"typ\">";
	public static String solrFieldTextType2 = "\r\n<field name=\"typ2\">";

	// if typ=0 is Section (entire section text is solr doc), typ=1 paragraph, if
	// typ=2 it is parent chikd, typ=3 sentence
//	public static String solrFieldParentChildLineage = "\r\n<field name=\"lin\">";

	public static List<String[]> listParagraphParentChild = new ArrayList<>();
	public static List<String[]> listParentChildOrig = new ArrayList<>();

	// lin=p4c2 - this is the 2nd child clause of parent 4 - eg: (4)(b) or (iv)(B)
	// if lin=s2 this is a numbered sentence eg: (ii)Adjustment to Daily Rate..

	// section xml=0, para xml=1, sent xml=2, clau=3
	// lead is the lead in of a sentence or parent child
	public static String solrFieldSectionHeading = "\r\n<field name=\"sec\">";
	// public static String solrFieldParagraphHeading = "\r\n<field
	// name=\"paraHdg\">";
	public static String solrFieldDefinitionHeading = "\r\n<field name=\"def\">";
	public static String solrFieldExhibitHeading = "\r\n<field name=\"exh\">";
	public static String solrFieldExhibitHeadingSub = "\r\n<field name=\"exhSub\">";
	public static String solrFieldContractType = "\r\n<field name=\"kTyp\">";
	public static String solrFieldFiler = "\r\n<field name=\"filer\">";
	public static String solrFieldFileDate = "\r\n<field name=\"fDate\">";
	public static String solrFieldCIK = "\r\n<field name=\"cik\">";
	public static String solrFieldFileSize = "\r\n<field name=\"fSize\">";
	// public static String solrFieldParagraphNo = "\r\n<field name=\"paraNo\">";
	// public static String solrFieldSentenceNo = "\r\n<field name=\"sentNo\">";
	// public static String solrFieldSectionNo = "\r\n<field name=\"secNo\">";
	public static String solrFieldTextCount = "\r\n<field name=\"cnt\">";// the number of text documents till now, i.e.,
	// if type=1 (paragraph) and cnt=30 - it is
	// the 30th paragraph in the xml (30th doc)
	public static String solrFieldParaNo = "\r\n<field name=\"paraNo\">"; // x-ref paragrpah no used in sentence xml and
	public static String solrFieldSecNo = "\r\n<field name=\"secNo\">"; // x-ref paragrpah no used in sentence xml and
	public static String solrFieldParChiNo = "\r\n<field name=\"PCN\">"; // x-ref paragrpah (this is parno used in
																			// sentence xml and
	public static String solrFieldError = "\r\n<field name=\"ERR\">"; // grab if error in parent child lineage
	public static String solrFieldSidx = "\r\n<field name=\"sIdx\">"; // start Idx of error
	public static String solrFieldLeadLineage = "\r\n<field name=\"LL\">";
	public static String solrFieldParentLead = "\r\n<field name=\"PL\">";
	public static String solrFieldLineage = "\r\n<field name=\"L\">";
	public static String solrFieldParentOrChild = "\r\n<field name=\"pOrC\">";

	// parent child fields, fields must be lower case for parent child fields - eg:
	// solrFieldopenLead
//	public static String solrFieldopenLead = "\r\n<field name=\"openLead\">";
	// this is a lead prior to a paragraph number
//	public static String solrFieldparentLead = "\r\n<field name=\"parentLead\">";
//	public static String solrFieldchildLead = "\r\n<field name=\"childLead\">";
//	public static String solrFieldgrandChildLead = "\r\n<field name=\"grandChildLead\">";

//	public static String solrFieldparent = "\r\n<field name=\"parent\">";
//	public static String solrFieldchild = "\r\n<field name=\"child\">";
//	public static String solrFieldgrandChild = "\r\n<field name=\"grandChild\">";
//	public static String solrFieldgreatGrandChild = "\r\n<field name=\"greatGrandChild\">";

	// if parent num=2 - that means it is (b) or (2).
//	public static String solrFieldparentNum = "\r\n<field name=\"parentNum\">";
//	public static String solrFieldchildNum = "\r\n<field name=\"childNum\">";
//	public static String solrFieldgrandChildNum = "\r\n<field name=\"grandChildNum\">";
//	public static String solrFieldgreatGrandChildNum = "\r\n<field name=\"greateGrandChildNum\">";

	// public static String solrFieldSentNo = "\r\n<field name=\"sentNo\">";
	// public static String tmpStrGlobal = "FIRST:";
	// claus xml.
	// public static String solrFieldSentNo = "\r\n<field name=\"sNo\">";// x-ref
	// sent no used in clause xml
	// public static String solrFieldContractBody = "\r\n<field name=\"kBody\">";
	public static String solrFieldContractLongSECname = "\r\n<field name=\"kName\">";

	public static String solrFieldEnd = "</field>";
	public static String solrFieldCDATA = "<![CDATA[";
	public static String solrFieldCDATAEnd = "]]>";

	public static List<String> listSolrFields = new ArrayList<>();
	public static List<String[]> listTextExhMarkers = new ArrayList<>();
	public static double forecastedSection = 0.0;
	public static double forecastedNextSection = 0.0;
	public static boolean LawGo = false;

	public static boolean forecastedSectionFound = false;
//	public static boolean outOfOrder = false;
	public static boolean forecastedNextSectionFound = false;
	public static boolean forecastedNextSectionInNextArticleFound = false;
	public static boolean isConsecutiveArticle = false;
	public static boolean sbSec = false, sbParChi = false, sbPOS = false;
	public static boolean regenerate = true;
//	public static boolean restrip = true;
	public static boolean strip = true;
	public static int nextArt = -1;
	public static int endOfK = 0;
	public static double secDif = 0.0, secDif2 = 0.01, artDif = 0.0;
	// public static List<String[]> listSidxEidxTypeSecHdgs = new
	// ArrayList<String[]>();
	// public static List<String[]> listSidxEidxTypeExhHdgs = new
	// ArrayList<String[]>();
	public static List<String[]> listSidxEidxTypeDefHdgs = new ArrayList<String[]>();

	public static List<String[]> listPrelimIdxAndSectionRestrictive = new ArrayList<String[]>();
	public static List<String[]> listPrelimIdxAndSectionLessRestrictive = new ArrayList<String[]>();
	public static List<String[]> listTocHdgs = new ArrayList<String[]>();
//	public static List<String[]> listContractAttributes = new ArrayList<>();

	public static Pattern patternSent1 = Pattern.compile("(?sm)" + "(" + "[A-Z]{2}|[a-z\\)\\]]{2}" + ")[\\.]{1}"
			+ "(?=( ?[\r\n\t ]{1,9}\\(" + "[\\dA-Za-z]{1,4}\\)" + "))");

	public static Pattern patternSent1b = Pattern.compile("(?sm)"
			// + ";( ?and| ?or)?|"
			+ "(?<=[a-z]{3}\\)?\\.)" + "[ \r\n]{1,5}(?=[A-Z]{1}[a-z]{1})");

	public static Pattern patternSent2 = Pattern.compile("(?sm)[A-Za-z\\)]{2}: ?[\r\n]{3,}");

	public static Pattern patternClau1b = Pattern.compile("(?sm);( ?and| ?or)? ?[\\r\\n]{3,}");

	public static Pattern patternSentence = Pattern.compile(patternSent1.pattern() + "|" + patternSent1b.pattern() + "|"
			+ patternClau1b.pattern() + "|" + patternSent2.pattern());

	public static Pattern patternIAA = Pattern.compile("(?i)advisor");
	public static Pattern patternPSA = Pattern.compile("(?i)pooling.{1,30}servic.{1,70}agreement");
	public static Pattern patternIndenture = Pattern.compile("(?i)indenture|trust.{1,4}agreement");
	public static Pattern patternSuppIndenture = Pattern.compile(
			"(?i)(supplement|amend).{1,15}(indenture|(trust.{1,6}servicing).{1,70}agreement|trust.{1,4}agreement)");
	public static Pattern patternFiscalAg = Pattern.compile("(?i)fiscal.{1,4}agen.{1,70}agreement");
	public static Pattern patternPayingAg = Pattern.compile("(?i)paying.{1,4}agen.{1,70}agreement");
	public static Pattern patternCollateralPledgeAg = Pattern
			.compile("(?i)(collateral|pledge)(?!manage)(?!administr).{1,70}agreement");
	public static Pattern patternPurchase = Pattern.compile("(?i)purchase.{1,70}agreement");
	public static Pattern patternEscrow = Pattern.compile("(?i)escrow.{1,70}agreement");
	public static Pattern patternCustody = Pattern.compile("(?i)custod");
	public static Pattern patternSecLending = Pattern.compile("(?i)securit.{1,6}lending.{1,70}agreement");
	public static Pattern patternTransferAg = Pattern.compile("(?i)transfer.{1,4}agen.{1,77}agreement");
	public static Pattern patternControlAg = Pattern.compile("(?i)account.{1,4}contro.{1,71}agreement");
	public static Pattern patternPriceSuppProsp = Pattern.compile("(?i)424|485APOS|485BPOS|FWP|485");
	public static Pattern patternSAI = Pattern.compile("(?i)SAI");

	public static Pattern patternReinsuranceTrustAg = Pattern.compile("(?i)reinsurance.{1,4}trust.{1,70}agreement");
	public static Pattern patternCollateralMgtAg = Pattern.compile("(?i)(collateral.{1,4}manage.{1,80}agreement)");
	public static Pattern patternCollateralAdminAg = Pattern
			.compile("(?i)(collateral(?!ized).{1,4}admin).{1,85}agreement");
	public static Pattern patternRepurchaseAg = Pattern.compile("(?i)repurchase.{1,70}agreement");
	public static Pattern patternCalculationAgentAg = Pattern
			.compile("(?i)(calculation|conversion).{1,10}agen.{1,70}agreement");
	public static Pattern patternAccountBankAg = Pattern.compile("(?i)account.{1,10}bank.{1,70}agreement");
	public static Pattern patternAdministrativeAgentAg = Pattern.compile("(?i)administrati.{1,10}agen.{1,70}agreement");
	public static Pattern patternListingAgentAg = Pattern.compile("(?i)listing.{1,10}agen.{1,70}agreement");
	public static Pattern patternFundAdminAg = Pattern
			.compile("" + "(?i)(fund.{1,4}(administrati.{0,4}|account.{0,4}))");
	public static Pattern patternExhangeAg = Pattern.compile("(?i)(exchan.{1,5}(agen.{1,5})?.{1,70}agreement)");
	public static Pattern patternDepositoryAg = Pattern.compile("(?i)(Depository.{1,5}(agen.{1,5})?.{1,70}agreement)");
	public static Pattern patternDepositaryAg = Pattern.compile("(?i)(Depositary.{1,5}(agen.{1,5})?.{1,70}agreement)");
	public static Pattern patternPortfolioAdminAg = Pattern.compile("(?i)(portfolio.{1,5}admin.{1,70}agreement)");
	public static Pattern patternLoanCreditAg = Pattern.compile("(?i)(credit |loan )");
	public static Pattern patternSaleServicingAg = Pattern.compile("(?i)(sale.{1,10}servicing.{1,70}agreement)");
	public static Pattern patternInterCreditorAg = Pattern.compile("(?i)(inter.{1,3}creditor.{1,70}agreement)");
	public static Pattern patternCorporateActions = Pattern
			.compile("(?i)" + "(change|interest rate|fixed|variable|warrant|tender|put "
					+ "|make.{1,3}whole|prepaid|pre.{1,3}paid|break.{1,3}amount"
					+ "|right|redempt|conver).{1,33}notice|(notic.{1,33})"
					+ "(change|interest rate|fixed|variable|warrant|tender|put "
					+ "|make.{1,3}whole|prepaid|pre.{1,3}paid|break.{1,3}amount" + "|right|redempt|conver)");

	public static Pattern patternThisPSA = Pattern.compile("(?i)(?<=this.{1,4})(pooling.{1,30}servic.{1,70}agreement)");
	public static Pattern patternThisIndenture = Pattern.compile("(?i)(?<=this.{1,4})(indenture|trust.{1,6}agreement)");
	public static Pattern patternThisSuppIndenture = Pattern
			.compile("(?i)(?<=this.{1,4})((supplement|amend).{1,15}(indenture|trust{1,6}agreement))");
	public static Pattern patternThisFiscalAg = Pattern.compile("(?i)(?<=this.{1,4})(.{1,4}agen.{1,70}agreement)");
	public static Pattern patternThisPayingAg = Pattern
			.compile("(?i)(?<=this.{1,4})(paying.{1,4}agen.{1,70}agreement)");
	public static Pattern patternThisCollateralPledgeAg = Pattern
			.compile("(?i)(?<=this.{1,4})((collateral|pledge)(?!manage)(?!administr).{1,70}agreement)");
	public static Pattern patternThisPurchase = Pattern.compile("(?i)(?<=this.{1,4})(purchase.{1,70}agreement)");
	public static Pattern patternThisEscrow = Pattern.compile("(?i)(?<=this.{1,4})(escrow.{1,70}agreement)");
	public static Pattern patternThisCustody = Pattern.compile("(?i)(?<=this.{1,4})(custody.{1,70}agreement)");
	public static Pattern patternThisSecLending = Pattern
			.compile("(?i)(?<=this.{1,4})(securit.{1,6}lending.{1,70}agreement)");
	public static Pattern patternThisTransferAg = Pattern
			.compile("(?i)(?<=this.{1,4})(transfer.{1,4}agen.{1,77}agreement)");
	public static Pattern patternThisControlAg = Pattern
			.compile("(?i)(?<=this.{1,4})(account.{1,4}contro.{1,71}agreement)");
	public static Pattern patternThisPriceSuppProsp = Pattern.compile("(?i)(?<=this.{1,4})(424|485APOS|485BPOS|FWP)");
	public static Pattern patternThisReinsuranceTrustAg = Pattern
			.compile("(?i)(?<=this.{1,4})(reinsurance.{1,4}trust.{1,70}agreement)");
	public static Pattern patternThisCollateralMgtAg = Pattern
			.compile("(?i)(?<=this.{1,4})((collateral.{1,4}manage.{1,80}agreement))");
	public static Pattern patternThisCollateralAdminAg = Pattern
			.compile("(?i)(?<=this.{1,4})((collateral(?!ized).{1,4}admin).{1,85}agreement)");
	public static Pattern patternThisRepurchaseAg = Pattern.compile("(?i)(?<=this.{1,4})(repurchase.{1,70}agreement)");
	public static Pattern patternThisCalculationAgentAg = Pattern
			.compile("(?i)(?<=this.{1,4})((calculation|conversion).{1,10}agen.{1,70}agreement)");
	public static Pattern patternThisAccountBankAg = Pattern
			.compile("(?i)(?<=this.{1,4})(account.{1,10}bank.{1,70}agreement)");
	public static Pattern patternThisAdministrativeAgentAg = Pattern
			.compile("(?i)(?<=this.{1,4})(administrati.{1,10}agen.{1,70}agreement)");
	public static Pattern patternThisListingAgentAg = Pattern
			.compile("(?i)(?<=this.{1,4})(listing.{1,10}agen.{1,70}agreement)");
	public static Pattern patternThisFundAdminAg = Pattern
			.compile("" + "(?i)(?<=this.{1,4})((fund.{1,4}(administrati.{0,4}|account.{0,4})))");
	public static Pattern patternThisExhangeAg = Pattern
			.compile("(?i)(?<=this.{1,4})((exchan.{1,5}(agen.{1,5})?.{1,70}agreement))");
	public static Pattern patternThisDepositoryAg = Pattern
			.compile("(?i)(?<=this.{1,4})((Depository.{1,5}(agen.{1,5})?.{1,70}agreement))");
	public static Pattern patternThisDepositaryAg = Pattern
			.compile("(?i)(?<=this.{1,4})((Depositary.{1,5}(agen.{1,5})?.{1,70}agreement))");
	public static Pattern patternThisPortfolioAdminAg = Pattern
			.compile("(?i)(?<=this.{1,4})((portfolio.{1,5}admin.{1,70}agreement))");
	public static Pattern patternThisLoanAg = Pattern.compile("(?i)(?<=this.{1,4})((loan |credit))");
	public static Pattern patternThisSaleServicingAg = Pattern
			.compile("(?i)(?<=this.{1,4})((sale.{1,10}servicing.{1,70}agreement))");
	public static Pattern patternThisInterCreditorAg = Pattern
			.compile("(?i)(?<=this.{1,4})((inter.{1,3}creditor.{1,70}agreement))");

	public static Pattern patternOpinionInBody = Pattern.compile("(?ism)(?<=we have acted as.{1,20} )counsel(?= )");

	/*
	 * READ THISNOTE!!! whenever I add a new contract pattern - then it must be
	 * embedded through this class. Every contract above pattern must be embeddedyou
	 * all will will will will will will she will wire you will will will will will
	 * will will will will will will will will will will will will will into
	 * getContractAttributes and patternContractsToInclude. Just search pattern
	 * immediately prior to the one you are adding.
	 */

//	public static Pattern patternAgreement = Pattern.compile("(?ism)agreement|suppl|amend|"
//			+ "opinion|certificate|consent|instruction|power of|restate|letter|form (s|t)"
//			+ "|notice|credit|commercial paper");

	public static Pattern patternOTHER = Pattern.compile("OTHERx ");

	// patternOTHER is the 'type' for all filings related to
	// patternGetAllContractsInFilingIfItHasThisContract. If there are opinions or
	// u/w agt associated with an accno filing where there IT is an indenture then
	// they will be downloaded and stored in folder 'Other'

	public Pattern patternGetAllContractsInFilingIfItHasThisContract = Pattern.compile(
//					patternFiscalAg
//					+ "|" + patternIndenture + "|" + 
			patternReinsuranceTrustAg.toString()// dumby so we don't grab everythimg
//							+ "|" 
//					+ patternPSA
	);

	public static Pattern patternNeverToInclude = Pattern.compile("(?ism)xbrl|10-(q|k)");

	public static Pattern patternContractsToInclude =

			Pattern.compile(patternAccountBankAg.pattern() + "|" + patternAdministrativeAgentAg.pattern() + "|"
					+ patternReinsuranceTrustAg.toString()// dumby so we don't grab everythimg
					+ "|" + patternCollateralAdminAg.pattern() + "|" + patternCollateralMgtAg.pattern() + "|"
					+ patternCollateralPledgeAg.pattern() + "|" + patternControlAg.pattern() + "|"
					+ patternCustody.pattern() + "|" + patternDepositaryAg.pattern() + "|"
					+ patternDepositoryAg.pattern() + "|" + patternEscrow.pattern() + "|" + patternExhangeAg.pattern()
					+ "|" + patternFiscalAg.pattern() + "|" + patternFundAdminAg.pattern() + "|"
					+ patternIndenture.pattern() + "|" + patternSuppIndenture.pattern() + "|"
					+ patternInterCreditorAg.pattern() + "|" + patternListingAgentAg.pattern() + "|"
					+ patternLoanCreditAg.pattern() + "|" + patternPayingAg.pattern() + "|"
					+ patternPortfolioAdminAg.pattern() + "|" + patternPSA.pattern() + "|" + patternPurchase.pattern()
					+ "|" + patternReinsuranceTrustAg.pattern() + "|" + patternRepurchaseAg.pattern() + "|"
					+ patternSaleServicingAg.pattern() + "|" + patternSecLending.pattern() + "|"
					+ patternTransferAg.pattern() + "|" + patternCorporateActions.pattern() + "|"
					+ patternOpinionInBody.pattern() + "|" + patternPriceSuppProsp.pattern() + "|"
					+ patternOTHER.pattern());

	public static Pattern patternContractsLongNamesToInclude =

			Pattern.compile(patternThisAccountBankAg.pattern() + "|" + patternThisAdministrativeAgentAg.pattern() + "|"
					+ patternThisReinsuranceTrustAg.toString()// dumby so we don't grab everythimg
					+ "|" + patternThisCollateralAdminAg.pattern() + "|" + patternThisCollateralMgtAg.pattern() + "|"
					+ patternThisCollateralPledgeAg.pattern() + "|" + patternThisControlAg.pattern() + "|"
					+ patternThisCustody.pattern() + "|" + patternThisDepositaryAg.pattern() + "|"
					+ patternThisDepositoryAg.pattern() + "|" + patternThisEscrow.pattern() + "|"
					+ patternThisExhangeAg.pattern() + "|" + patternThisFiscalAg.pattern() + "|"
					+ patternThisFundAdminAg.pattern() + "|" + patternThisIndenture.pattern() + "|"
					+ patternThisSuppIndenture.pattern() + "|" + patternThisInterCreditorAg.pattern() + "|"
					+ patternThisListingAgentAg.pattern() + "|" + patternThisLoanAg.pattern() + "|"
					+ patternThisPayingAg.pattern() + "|" + patternThisPortfolioAdminAg.pattern() + "|"
					+ patternThisPSA.pattern() + "|" + patternThisPurchase.pattern() + "|"
					+ patternThisReinsuranceTrustAg.pattern() + "|" + patternThisRepurchaseAg.pattern() + "|"
					+ patternThisSaleServicingAg.pattern() + "|" + patternThisSecLending.pattern() + "|"
					+ patternThisTransferAg.pattern() + "|" + patternThisPriceSuppProsp.pattern());

	public static Pattern patternExclude = Pattern.compile(
			"(?i)copyright|report|excess.{1,5}cash|location|salary|retire|detail|assertion|compen|erisa|cafet");

	public static Pattern patternContractTypesToExclude = Pattern.compile("xx");

	public static Pattern patternThisContractTypesToExclude = Pattern.compile("");

	public static Pattern patternContractNameToExcludeIndentures = Pattern
			.compile("(?i)supp|amend|amd|restat|1st|2nd|3rd|4th|5th|6th|first|second|third|fourth|fifth|sixth");

	public static Pattern patternRole = Pattern
			.compile("as (the )?\\b([A-Z]{1}[a-z;,]\\w*)(\\b([A-Z]{1}[a-z;,]\\w*))?");

	public static List<String[]> validateDefinedTerms_shall_mean(String text) throws IOException {
		NLP nlp = new NLP();
		List<String[]> list = new ArrayList<String[]>();
		List<String[]> listIdxDef = nlp.getAllStartIdxLocsAndMatchedGroups(text,
				GetContracts.patternDefinedWordInDefSec);

		// has word 'means?' and there are double quotes.
		String def, snip;
		int sIdx = 0;
		for (int i = 0; i < listIdxDef.size(); i++) {
			def = listIdxDef.get(i)[1];
			sIdx = Integer.parseInt(listIdxDef.get(i)[0]);
//			System.out.println("def=="+def);
			snip = text.substring(sIdx, Math.min(sIdx + 100, text.length()));
//			if (nlp.getAllMatchedGroups(snip, patternDef_mean).size() > 0) {
			if (nlp.getAllMatchedGroups(snip, Pattern.compile("\".{0,10} (means|meaning)[, ]")).size() > 0) {

//				System.out.println("this is valid def, quotes and means--" + def);
				list.add(listIdxDef.get(i));
			}
		}

//		NLP.printListOfStringArray("list of shall mean def==", list);
		return list;
	}

	public static List<String[]> getDefinitions_for_alphabetical(String text) throws IOException {
		// used for alphabetcial

		// this is the preliminary method - see near bottom for routine used to
		// vaidate and prep for final list.

		NLP nlp = new NLP();
		List<String[]> listIdxDef = nlp.getAllStartIdxLocsAndMatchedGroups(text,
				GetContracts.patternDefinedWordInDefSec);
//		System.out.println("1.getting defs");
		NLP.printListOfStringArray("getDefinitions_for_alphabetical...prelim def list=", listIdxDef);
		if (listIdxDef.size() < 8)
			return null;

		List<String[]> listpreLimDef = new ArrayList<String[]>();

//		 NLP.pwNLP.append(NLP.println("listIdxDef.size=", listIdxDef.size() + ""));

		@SuppressWarnings("unused")
		int sIdx = 0, ltr = 0, ltrF1 = 0, ltrF2 = 0, ltrF3 = 0, ltrF4 = 0, ltrF5 = 0, ltrF6 = 0, ltrF7 = 0, ltrF8 = 0,
				ltrP1 = 0, ltrP2 = 0, ltrP3 = 0, ltrP4 = 0, ltrP5 = 0, ltrP6 = 0, ltrP7 = 0, ltrP8 = 0, eIdx = 0,
				eIdxSecHdg = 0, tmpIdx = 0, ltr1Ah = 1, nextIdx = 0, maxLtr = 0;

		// cycle thru preliminary list of definitions (def)
//		System.out.println("2.getting defs");
		for (int i = 0; i < listIdxDef.size(); i++) {

			// get numerical value of ltr (\\d=0, A=1, B=2, C=3 ...)
			ltr = GetContracts.getNumberInAlphabet(listIdxDef.get(i)[1].replaceAll("[\"\r\n ]", "").substring(0, 1));

			if (i > 5) {
				// get previous 5 values numerical values of lt

				ltrP1 = GetContracts
						.getNumberInAlphabet(listIdxDef.get(i - 1)[1].replaceAll("[\"\r\n ]", "").substring(0, 1));

				ltrP2 = GetContracts
						.getNumberInAlphabet(listIdxDef.get(i - 2)[1].replaceAll("[\"\r\n ]", "").substring(0, 1));

				ltrP3 = GetContracts
						.getNumberInAlphabet(listIdxDef.get(i - 3)[1].replaceAll("[\"\r\n ]", "").substring(0, 1));

				ltrP4 = GetContracts
						.getNumberInAlphabet(listIdxDef.get(i - 4)[1].replaceAll("[\"\r\n ]", "").substring(0, 1));

				ltrP5 = GetContracts
						.getNumberInAlphabet(listIdxDef.get(i - 5)[1].replaceAll("[\"\r\n ]", "").substring(0, 1));
			}

			if (i < 6 && (i + 5 < listIdxDef.size()) || (ltr < 2 && i + 5 < listIdxDef.size())) {
				// get forward 5 values numerical values of lt

				ltrF1 = GetContracts
						.getNumberInAlphabet(listIdxDef.get(i + 1)[1].replaceAll("[\"\r\n ]", "").substring(0, 1));

				ltrF2 = GetContracts
						.getNumberInAlphabet(listIdxDef.get(i + 2)[1].replaceAll("[\"\r\n ]", "").substring(0, 1));

				ltrF3 = GetContracts
						.getNumberInAlphabet(listIdxDef.get(i + 3)[1].replaceAll("[\"\r\n ]", "").substring(0, 1));

				ltrF4 = GetContracts
						.getNumberInAlphabet(listIdxDef.get(i + 4)[1].replaceAll("[\"\r\n ]", "").substring(0, 1));

				ltrF5 = GetContracts
						.getNumberInAlphabet(listIdxDef.get(i + 5)[1].replaceAll("[\"\r\n ]", "").substring(0, 1));

			}
			// add exclusions here - eg: Name:, Title: etc.
			if (nlp.getAllIndexEndLocations(listIdxDef.get(i)[1], Pattern.compile("(?i)(Attention|Name|Title):"))
					.size() > 0) {
				continue;
			}

//			System.out.println("ltr=" + ltr + "\rltrF1=" + ltrF1 + " ltrF2=" + ltrF2 + " ltrF3=" + ltrF3 + "\rltrP1="
//					+ ltrP1 + " ltrP2=" + ltrP2 + " ltrP3=" + ltrP3 + "\ri=" + i);

			// this is checking cur ltr against forward ltrs - if i<6 - I have
			// retrieved forward 3 ltr given def size must be >9. if>5 I have
			// prior 3. If ltrs are in order - than add to def list.
			if (((i < 6) && ((ltr <= ltrF1 && ltrF1 <= ltrF2 && ltrF2 <= ltrF3
			// && ltrF3<= ltrF4
			// && ltrF4 <= ltrF5 && ltrF5 <= ltrF6 &&
			// ltrF6 <= ltrF7 && ltrF7 <= ltrF8
			) || (ltr <= ltrF2 && ltrF2 < ltrF3) || (ltr <= ltrF1 && ltrF1 <= ltrF3)))
					|| (i > 5 && ((ltr >= ltrP1 && ltrP1 >= ltrP2 && ltrP2 >= ltrP3
					// && ltrP3>= ltrP4
					// && ltrP4 >= ltrP5 && ltrP5 >= ltrP6 &&
					// ltrP6 >= ltrP7 && ltrP7 >= ltrP8
					) || (ltr >= ltrP2 && ltrP2 >= ltrP3) || (ltr >= ltrP1 && ltrP1 >= ltrP3)))) {

//				System.out.println("1 def in order. added=" + Arrays.toString(listIdxDef.get(i)));
				String[] ary = { listIdxDef.get(i)[0], listIdxDef.get(i)[1].replaceAll("[\"\r\n]", "").trim() };
				listpreLimDef.add(ary);
			}

			// nmb cnt in order <9 && i>15; start ltr!=a, ltr-ltr3>3
			// see if gap -- is ltr behind and after close in value
			else if (ltr < 3 && (ltrP1 > 5 || ltrP2 > 5 || ltrP3 > 5) && ltr <= ltrF1 && ltrF1 <= ltrF2
					&& ltrF2 <= ltrF3) {
//				System.out.println("2 def in order. added=" + Arrays.toString(listIdxDef.get(i)));

				String[] ary = { listIdxDef.get(i)[0], listIdxDef.get(i)[1].replaceAll("[\"\r\n]", "").trim() };
				listpreLimDef.add(ary);

				// an 'else if' can create false negative b/c it is undefined as
				// to what is excluded - its everything else excluded. This is
				// okay - b/c we want to under include versus over include.
				// Except that this will cause prior def to have an eIdx that
				// encompasses next def if such next def is correct.

			} else {
//				System.out.println(
//						"not def - not in order=" + Arrays.toString(listIdxDef.get(i)).replaceAll("[\"\r\n]", "")
//								+ " ltr=" + ltr + " ltrF1=" + ltrF1 + " ltrF2=" + ltrF2 + " ltrF3=" + ltrF3 + " ltrP1="
//								+ ltrP1 + " ltrP2=" + ltrP2 + " ltrP3=" + ltrP3 + " ltrP4=" + ltrP4);

				if (Alphabetical.alphaMethod) {

					sIdx = Integer.parseInt(listIdxDef.get(i)[0]);
					if (i > 0) {
						String[] ary = { listIdxDef.get(i)[1] + " is out of order,", " ",
								"but I could be wrong. snippet: "
										+ text.substring(sIdx, Math.min(sIdx + 50, text.length())) };
						Alphabetical.listNotInOrder.add(ary);
					}
					if (i == 0) {
						String[] ary = { listIdxDef.get(i)[1] + " is out of order, but I could be wrong.", "",
								" snippet: ", text.substring(sIdx, Math.min(sIdx + 50, text.length())) };
						Alphabetical.listNotInOrder.add(ary);
					}

				}
				continue;
			}
		}

		System.out.println("listpreLimDef.size=" + listpreLimDef.size());
//		NLP.printListOfStringArray("1. listpreLimDef:", listpreLimDef);

		List<String[]> listDefFinalVal = new ArrayList<String[]>();
		sIdx = 0;
		@SuppressWarnings("unused")
		int prevIdx = 0, prevIdx2 = 0, prevIdx3 = 0, prevIdx4 = 0, prevIdx5 = 0, prevLtr = 0, ltr2Ah = -2, prev2Ltr = 0,
				prev3Ltr = 0, prev4Ltr = 0, prev5Ltr = 0, cntDefList = 0;
		ltr = 0;
		boolean inOrder = true, getLastDef = true, defList = true;

		for (int i = 0; i < listpreLimDef.size(); i++) {
			sIdx = Integer.parseInt(listpreLimDef.get(i)[0]);
//			System.out.println("in alpha - prelim def list=" + Arrays.toString(listpreLimDef.get(i)));
			inOrder = true;
			ltr = GetContracts.getNumberInAlphabet(listpreLimDef.get(i)[1].replaceAll("[\"\r\n ]", "").substring(0, 1));
			if (i + 1 < listpreLimDef.size()) {
				ltr1Ah = 1;
				ltr2Ah = 2;
				// see if next def is in alpha.
				ltr1Ah = GetContracts
						.getNumberInAlphabet(listpreLimDef.get(i + 1)[1].replaceAll("[\"\r\n ]", "").substring(0, 1));
				if (ltr > ltr1Ah) {
					inOrder = false;
				}
			}

			if (i + 2 == listpreLimDef.size()) {
				ltr2Ah = GetContracts
						.getNumberInAlphabet(listpreLimDef.get(i + 1)[1].replaceAll("[\"\r\n ]", "").substring(0, 1));
			}

			// skips over list of defitions incorporated by reference to TIA.
			// eg: Paying Agent
			// Protected Purchaser
			// Registrar

			if (i > 4 && text.substring(prevIdx, sIdx).replaceAll("[ \r\n\\d\\.\"]+", "").length() < 45
					&& text.substring(prevIdx2, prevIdx).replaceAll("[ \r\n\\d\\.\"]+", "").length() < 45
					&& text.substring(prevIdx3, prevIdx2).replaceAll("[ \r\n\\d\\.\"]+", "").length() < 45
					&& text.substring(prevIdx4, prevIdx3).replaceAll("[ \r\n\\d\\.\"]+", "").length() < 45
					&& text.substring(prevIdx5, prevIdx4).replaceAll("[ \r\n\\d\\.\"]+", "").length() < 45) {
				// has to be placed here. need to track prevIdx bef continuing.
				prevIdx5 = prevIdx4;
				prevIdx4 = prevIdx3;
				prevIdx3 = prevIdx2;
				prevIdx2 = prevIdx;
				prevIdx = sIdx;

				prev5Ltr = prev4Ltr;
				prev4Ltr = prev3Ltr;
				prev3Ltr = prev2Ltr;
				prev2Ltr = prevLtr;
				prevLtr = ltr;
				continue;
			}
			// if it doesn't continue
			prevIdx4 = prevIdx3;
			prevIdx3 = prevIdx2;
			prevIdx2 = prevIdx;
			prevIdx = sIdx;

			// nmb cnt in order <9 && i>15; start ltr!=a, ltr-ltr3>3
			getLastDef = false;
			if (i + 1 < listpreLimDef.size()) {
				nextIdx = Integer.parseInt(listpreLimDef.get(i + 1)[0]);
			}

			// nmb cnt in order <9 && i>15; start ltr!=a, ltr-ltr3>3
			if ((Math.abs(ltr - prevLtr) > 5 && ltr1Ah - ltr > 3) || (nextIdx - sIdx) > 5000 && ltr - prevLtr > 3) {
				getLastDef = true;
				NLP.println("1. getLastDef. ltr=",
						+ltr + " ltr1Ah=" + ltr1Ah + " prevLtr=" + prevLtr + Arrays.toString(listpreLimDef.get(i)));
			}

			// if ltr (plus margin of error) is less than prev 4 ltrs BUT that
			// start ltr of def is after F as is def 1Ahead and 2Ahead or near
			// end of defs it NOT a new list
			if (ltr + 2 <= prevLtr && ltr + 2 <= prev2Ltr && ltr + 2 <= prev3Ltr && ltr + 2 < prev4Ltr && ltr > 3
					&& ltr > 6 && (ltr1Ah > 3 || i + 3 > listIdxDef.size())) {
				defList = false;
				cntDefList = 0;
			}

			else {
				cntDefList++;
				defList = true;
			}

			if (cntDefList < 2 && ltr > 4) {
//				System.out.println("1. continuing....");
				continue;

			}

			if (cntDefList < 4 && ltr > 9) {
//				System.out.println("2. continuing....");
				continue;

			}

			// ltr is less than 5 - reset.

			if (!defList) {
				prev4Ltr = prev3Ltr;
				prev3Ltr = prev2Ltr;
				prev2Ltr = prevLtr;
				prevLtr = ltr;
				continue;
			}

			if (i + 1 < listpreLimDef.size() && inOrder && !getLastDef) {

				String[] ary = { listpreLimDef.get(i)[0], listpreLimDef.get(i + 1)[0], "def", listpreLimDef.get(i)[1] };
				// //NLP.pwNLP.append(NLP.println("prelim - def in order - added to def list=" +
				// Arrays.toString(ary), ""));
				listDefFinalVal.add(ary);

			} else {

				eIdxSecHdg = 0;
				// a consecutive list of defined terms can only relate to one
				// section heading. use section heading pattern tool to get last
				// def if it is prior to what last definition otherwise
				// retrieves. Do this by running definitions retrieval after
				// getting sec locations. Note this in body where def, sec and
				// exh retrievals are listed.

				String lastDef = ContractParser.getLastDefinition(text, sIdx, listpreLimDef.get(i)[1], "def", i,
						listpreLimDef.size());
				// System.out.println("lastDef="+lastDef);
				tmpIdx = lastDef.length();

				Pattern patternExhibitInContract = Pattern.compile(
						"(?<=([\r\n]{1}[ \t]{0,110}))(Exhibit|EXHIBIT|APPENDIX|Appendix|SCHEDULE|Schedule|Annex|ANNEX)"
								+ "[ ]{1,3}[A-Z\\-\\d\\(\\)]{1,5}( to)? ?[\r\n]{1}");

				List<String[]> listEnd = nlp.getAllStartIdxLocsAndMatchedGroups(lastDef, patternExhibitInContract);

				// NLP.printListOfStringArray("list to find end of last def=",
				// listEnd);

				// //NLP.pwNLP.append(NLP.println("lastDef=", lastDef + "||end"));

				if (listEnd.size() > 0) {
					// if >0 than it found endOfK before end of lastSection txt
					// - so it is earlier end point and idx to use.

					tmpIdx = Integer.parseInt(listEnd.get(0)[0]);
					// //NLP.pwNLP.append(NLP.println("end of last Def=",
					// Arrays.toString(listEnd.get(0))
					// +"|end last def")
					// + " text.snipp="
					// + text.substring(
					// sIdx + tmpIdx,
					// Math.min((sIdx + tmpIdx + 100),
					// text.length())
					// ));

				}

				eIdx = sIdx + tmpIdx;

				String[] ary = { listpreLimDef.get(i)[0], eIdx + "", "def", listpreLimDef.get(i)[1] };
				// System.out.println("1b prelim - def in order - added to def list=" +
				// Arrays.toString(ary));

				listDefFinalVal.add(ary);

			}

			prev4Ltr = prev3Ltr;
			prev3Ltr = prev2Ltr;
			prev2Ltr = prevLtr;
			prevLtr = ltr;

		}

//		NLP.printListOfStringArray("listDefFinalVal=", listDefFinalVal);

		return listDefFinalVal;

	}

	public static List<String[]> getDefinitions_for_parsing_jsons(String text) throws IOException {

		// this is the preliminary method - see near bottom for routine used to
		// vaidate and prep for final list.

		List<String[]> listShallMeanValidated = validateDefinedTerms_shall_mean(text);
//		List<String[]> listShallMeanValidated = new ArrayList<String[]>();

		NLP nlp = new NLP();

		List<String[]> listIdxDef = nlp.getAllStartIdxLocsAndMatchedGroups(text,
				GetContracts.patternDefinedWordInDefSec);
//		System.out.println("1.getting defs");
		System.out.println("prelim def list.siz=" + listIdxDef.size());
//		NLP.printListOfStringArray("a.prelim def list=", listIdxDef);
		if (listIdxDef.size() < 5)
			return null;

		List<String[]> listpreLimDef = new ArrayList<String[]>();

		// NLP.pwNLP.append(NLP.println("listIdxDef.size=", listIdxDef.size() + ""));

		@SuppressWarnings("unused")
		int sIdx = 0, sIdxP1 = 0, sIdxP2 = 0, sIdxP3 = 0, sIdxP4, sIdxP5, sIdxF1 = 0, sIdxF2, sIdxF3, sIdxF4, sIdxF5,
				sIdxF6, ltr = 0, ltrF1 = 0, ltrF2 = 0, ltrF3 = 0, ltrF4 = 0, ltrF5 = 0, ltrF6 = 0, ltrF7 = 0, ltrF8 = 0,
				ltrP1 = 0, ltrP2 = 0, ltrP3 = 0, ltrP4 = 0, ltrP5 = 0, ltrP6 = 0, ltrP7 = 0, ltrP8 = 0, eIdx = 0,
				eIdxSecHdg = 0, tmpIdx = 0, ltr1Ah = 1, nextIdx = 0, maxLtr = 0;

		// cycle thru preliminary list of definitions (def)
//		System.out.println("2.getting defs");
		for (int i = 0; i < listIdxDef.size(); i++) {
			if (listIdxDef.get(i)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").length() == 0)
				continue;
			if (listIdxDef.get(i)[0].replaceAll("[\"\r\n ]|Qx|XQ", "").length() == 0)
				continue;
			sIdxP1 = 0;
			sIdxP2 = 0;
			sIdxP3 = 0;
			sIdxP4 = 0;
			sIdxP5 = 0;
			sIdxF1 = 0;
			sIdxF2 = 0;
			sIdxF3 = 0;
			sIdxF4 = 0;
			sIdxF5 = 0;
			sIdxF6 = 0;
			ltr = 0;
			ltrF1 = 0;
			ltrF2 = 0;
			ltrF3 = 0;
			ltrF4 = 0;
			ltrF5 = 0;
			ltrF6 = 0;
			ltrF7 = 0;
			ltrF8 = 0;
			ltrP1 = 0;
			ltrP2 = 0;
			ltrP3 = 0;
			ltrP4 = 0;
			ltrP5 = 0;
			ltrP6 = 0;
			ltrP7 = 0;
			ltrP8 = 0;
			eIdx = 0;
			eIdxSecHdg = 0;
			tmpIdx = 0;
			ltr1Ah = 1;
			nextIdx = 0;
			maxLtr = 0;

			sIdx = Integer.parseInt(listIdxDef.get(i)[0]);

			// get numerical value of ltr (\\d=0, A=1, B=2, C=3 ...)
			ltr = GetContracts
					.getNumberInAlphabet(listIdxDef.get(i)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").substring(0, 1));

			if (i > 5) {
				// get previous 5 values numerical values of lt

				if (listIdxDef.get(i - 1)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").length() > 0)
					ltrP1 = GetContracts.getNumberInAlphabet(
							listIdxDef.get(i - 1)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").substring(0, 1));

				if (listIdxDef.get(i - 2)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").length() > 0)
					ltrP2 = GetContracts.getNumberInAlphabet(
							listIdxDef.get(i - 2)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").substring(0, 1));

				if (listIdxDef.get(i - 3)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").length() > 0)
					ltrP3 = GetContracts.getNumberInAlphabet(
							listIdxDef.get(i - 3)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").substring(0, 1));

				if (listIdxDef.get(i - 4)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").length() > 0)
					ltrP4 = GetContracts.getNumberInAlphabet(
							listIdxDef.get(i - 4)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").substring(0, 1));

				if (listIdxDef.get(i - 5)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").length() > 0)
					ltrP5 = GetContracts.getNumberInAlphabet(
							listIdxDef.get(i - 5)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").substring(0, 1));

				sIdxP1 = Integer.parseInt(listIdxDef.get(i - 1)[0]);
				sIdxP2 = Integer.parseInt(listIdxDef.get(i - 2)[0]);
				sIdxP3 = Integer.parseInt(listIdxDef.get(i - 3)[0]);
				sIdxP4 = Integer.parseInt(listIdxDef.get(i - 4)[0]);
				sIdxP5 = Integer.parseInt(listIdxDef.get(i - 5)[0]);
			}

			if (i < 6 && (i + 5 < listIdxDef.size()) || (ltr < 2 && i + 5 < listIdxDef.size())) {
				// get forward 5 values numerical values of lt

				if (listIdxDef.get(i + 1)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").length() > 0)
					ltrF1 = GetContracts.getNumberInAlphabet(
							listIdxDef.get(i + 1)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").substring(0, 1));

				if (listIdxDef.get(i + 2)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").length() > 0)
					ltrF2 = GetContracts.getNumberInAlphabet(
							listIdxDef.get(i + 2)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").substring(0, 1));

				if (listIdxDef.get(i + 3)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").length() > 0)
					ltrF3 = GetContracts.getNumberInAlphabet(
							listIdxDef.get(i + 3)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").substring(0, 1));

				if (listIdxDef.get(i + 4)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").length() > 0)
					ltrF4 = GetContracts.getNumberInAlphabet(
							listIdxDef.get(i + 4)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").substring(0, 1));

				if (listIdxDef.get(i + 5)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").length() > 0)
					ltrF5 = GetContracts.getNumberInAlphabet(
							listIdxDef.get(i + 5)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").substring(0, 1));
				sIdxF2 = Integer.parseInt(listIdxDef.get(i + 2)[0]);
				sIdxF3 = Integer.parseInt(listIdxDef.get(i + 3)[0]);
				sIdxF4 = Integer.parseInt(listIdxDef.get(i + 4)[0]);
				sIdxF5 = Integer.parseInt(listIdxDef.get(i + 5)[0]);
			}
			if (i + 2 < listIdxDef.size()) {
				sIdxF1 = Integer.parseInt(listIdxDef.get(i + 1)[0]);
			}

			// add exclusions here - eg: Name:, Title: etc.
			if (nlp.getAllIndexEndLocations(listIdxDef.get(i)[1], Pattern.compile("(?i)(Attention|Name|Title):"))
					.size() > 0) {
//				System.out.println("continue. attention|name|title");
				continue;
			}

//			System.out.println("ltr=" + ltr + "\rltrF1=" + ltrF1 + " ltrF2=" + ltrF2 + " ltrF3=" + ltrF3 + "\rltrP1="
//					+ ltrP1 + " ltrP2=" + ltrP2 + " ltrP3=" + ltrP3 + "\ri=" + i+" sIdx="+sIdx+" sIdxF1=="+sIdxF1+" sIdxP1="+sIdxP1+"\r\n"+Arrays.toString( listIdxDef.get(i)));

			// this is checking cur ltr against forward ltrs - if i<6 - I have
			// retrieved forward 3 ltr given def size must be >9. if>5 I have
			// prior 3. If ltrs are in order - than add to def list.
			if (((i < 6) && ((ltr <= ltrF1 && ltrF1 <= ltrF2 && ltrF2 <= ltrF3
			// && ltrF3<= ltrF4
			// && ltrF4 <= ltrF5 && ltrF5 <= ltrF6 &&
			// ltrF6 <= ltrF7 && ltrF7 <= ltrF8
			) || (ltr <= ltrF2 && ltrF2 < ltrF3) || (ltr <= ltrF1 && ltrF1 <= ltrF3)))
					|| (i > 5 && ((ltr >= ltrP1 && ltrP1 >= ltrP2 && ltrP2 >= ltrP3
					// && ltrP3>= ltrP4
					// && ltrP4 >= ltrP5 && ltrP5 >= ltrP6 &&
					// ltrP6 >= ltrP7 && ltrP7 >= ltrP8
					) || (ltr >= ltrP2 && ltrP2 >= ltrP3) || (ltr >= ltrP1 && ltrP1 >= ltrP3)))) {

//				System.out.println("1 def in order. added=" + Arrays.toString(listIdxDef.get(i)));
				String[] ary = { listIdxDef.get(i)[0], listIdxDef.get(i)[1].replaceAll("[\"\r\n ]", " ").trim() };
				listpreLimDef.add(ary);
//				continue;
			}

			// nmb cnt in order <9 && i>15; start ltr!=a, ltr-ltr3>3
			// see if gap -- is ltr behind and after close in value
			else if (ltr < 3 && (ltrP1 > 5 || ltrP2 > 5 || ltrP3 > 5) && ltr <= ltrF1 && ltrF1 <= ltrF2
					&& ltrF2 <= ltrF3) {
//				System.out.println("2 def in order. added=" + Arrays.toString(listIdxDef.get(i)));

				String[] ary = { listIdxDef.get(i)[0], listIdxDef.get(i)[1].replaceAll("[\"\r\n ]", " ").trim() };
				listpreLimDef.add(ary);
//				continue;
				// an 'else if' can create false negative b/c it is undefined as
				// to what is excluded - its everything else excluded. This is
				// okay - b/c we want to under include versus over include.
				// Except that this will cause prior def to have an eIdx that
				// encompasses next def if such next def is correct.

			} else if (ltr < 3 && (sIdx - sIdxP1 > 10000 || sIdx - sIdxP2 > 10000 || sIdx - sIdxP3 > 10000
					|| (sIdxF1 > sIdx && nlp.getAllMatchedGroups(text.substring(sIdx, Math.min(sIdxF1, sIdx + 50)),
							Pattern.compile("means|mean|meaning")).size() > 0)
					|| (ltr <= ltrF1 && ltrF1 <= ltrF2 && ltrF2 <= ltrF3))) {
//				System.out.println("3 def in order. added=" + Arrays.toString(listIdxDef.get(i)));

				String[] ary = { listIdxDef.get(i)[0], listIdxDef.get(i)[1].replaceAll("[\"\r\n ]", " ").trim() };
				listpreLimDef.add(ary);
			} else {
//				System.out.println(
//						"not def - not in order=" + Arrays.toString(listIdxDef.get(i)).replaceAll("[\"\r\n ]", "")
//								+ " ltr=" + ltr + " ltrF1=" + ltrF1 + " ltrF2=" + ltrF2 + " ltrF3=" + ltrF3 + " ltrP1="
//								+ ltrP1 + " ltrP2=" + ltrP2 + " ltrP3=" + ltrP3 + " ltrP4=" + ltrP4);

				if (Alphabetical.alphaMethod) {
//					System.out.println("alphabetical.....=" + Alphabetical.alphaMethod);

					if (i > 0) {
						String[] ary = { listIdxDef.get(i)[1] + " is out of order,", " ",
								"but I could be wrong. snippet: "
										+ text.substring(sIdx, Math.min(sIdx + 50, text.length())) };
						Alphabetical.listNotInOrder.add(ary);
					}
					if (i == 0) {
						String[] ary = { listIdxDef.get(i)[1] + " is out of order, but I could be wrong.", "",
								" snippet: ", text.substring(sIdx, Math.min(sIdx + 50, text.length())) };
						Alphabetical.listNotInOrder.add(ary);
					}

				}
				continue;
			}
		}

//		NLP.printListOfStringArray("1. listpreLimDef:", listpreLimDef);

		List<String[]> listDefFinalVal = new ArrayList<String[]>();
		sIdx = 0;
		@SuppressWarnings("unused")
		int prevIdx = 0, prevIdx2 = 0, prevIdx3 = 0, prevIdx4 = 0, prevIdx5 = 0, prevLtr = 0, ltr2Ah = -2, prev2Ltr = 0,
				prev3Ltr = 0, prev4Ltr = 0, prev5Ltr = 0, cntDefList = 0;
		ltr = 0;
		boolean inOrder = true, getLastDef = true, defList = true;

		for (int i = 0; i < listpreLimDef.size(); i++) {
			sIdx = Integer.parseInt(listpreLimDef.get(i)[0]);
//			System.out.println("in alpha - prelim def list=" + Arrays.toString(listpreLimDef.get(i)));
			inOrder = true;
			ltr = GetContracts
					.getNumberInAlphabet(listpreLimDef.get(i)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").substring(0, 1));
			if (i + 1 < listpreLimDef.size()) {
				ltr1Ah = 1;
				ltr2Ah = 2;
				// see if next def is in alpha.
				ltr1Ah = GetContracts.getNumberInAlphabet(
						listpreLimDef.get(i + 1)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").substring(0, 1));
				if (ltr > ltr1Ah) {
					inOrder = false;
				}
			}

			if (i + 2 == listpreLimDef.size()) {
				ltr2Ah = GetContracts.getNumberInAlphabet(
						listpreLimDef.get(i + 1)[1].replaceAll("[\"\r\n ]|Qx|XQ", "").substring(0, 1));
			}

			// skips over list of defitions incorporated by reference to TIA.
			// eg: Paying Agent
			// Protected Purchaser
			// Registrar

			if (i > 4 && text.substring(prevIdx, sIdx).replaceAll("[ \r\n\\d\\.\"]+", "").length() < 45
					&& text.substring(prevIdx2, prevIdx).replaceAll("[ \r\n\\d\\.\"]+", "").length() < 45
					&& text.substring(prevIdx3, prevIdx2).replaceAll("[ \r\n\\d\\.\"]+", "").length() < 45
					&& text.substring(prevIdx4, prevIdx3).replaceAll("[ \r\n\\d\\.\"]+", "").length() < 45
					&& text.substring(prevIdx5, prevIdx4).replaceAll("[ \r\n\\d\\.\"]+", "").length() < 45) {
				// has to be placed here. need to track prevIdx bef continuing.
				prevIdx5 = prevIdx4;
				prevIdx4 = prevIdx3;
				prevIdx3 = prevIdx2;
				prevIdx2 = prevIdx;
				prevIdx = sIdx;

				prev5Ltr = prev4Ltr;
				prev4Ltr = prev3Ltr;
				prev3Ltr = prev2Ltr;
				prev2Ltr = prevLtr;
				prevLtr = ltr;
//				System.out.println("continue44444");
				continue;
			}
			// if it doesn't continue
			prevIdx4 = prevIdx3;
			prevIdx3 = prevIdx2;
			prevIdx2 = prevIdx;
			prevIdx = sIdx;

			// nmb cnt in order <9 && i>15; start ltr!=a, ltr-ltr3>3
			getLastDef = false;
			if (i + 1 < listpreLimDef.size()) {
				nextIdx = Integer.parseInt(listpreLimDef.get(i + 1)[0]);
			}

			// nmb cnt in order <9 && i>15; start ltr!=a, ltr-ltr3>3
			if ((Math.abs(ltr - prevLtr) > 5 && ltr1Ah - ltr > 3) || (nextIdx - sIdx) > 5000 && ltr - prevLtr > 3) {
				getLastDef = true;
//				System.out.println("..getLastdef=="+Arrays.toString(listpreLimDef.get(i)));
			}

			// if ltr (plus margin of error) is less than prev 4 ltrs BUT that
			// start ltr of def is after F as is def 1Ahead and 2Ahead or near
			// end of defs it NOT a new list
			if (ltr + 2 <= prevLtr && ltr + 2 <= prev2Ltr && ltr + 2 <= prev3Ltr && ltr + 2 < prev4Ltr && ltr > 3
					&& ltr > 6 && (ltr1Ah > 3 || i + 3 > listIdxDef.size())) {
				defList = false;
				cntDefList = 0;
			}

			else {
				cntDefList++;
				defList = true;
			}

			if (cntDefList < 2 && ltr > 4) {
//				System.out.println("1. continuing....");
				continue;

			}

			if (cntDefList < 4 && ltr > 9) {
//				System.out.println("2. continuing....");
				continue;

			}

			// ltr is less than 5 - reset.

			if (!defList) {
				prev4Ltr = prev3Ltr;
				prev3Ltr = prev2Ltr;
				prev2Ltr = prevLtr;
				prevLtr = ltr;
//				System.out.println("3. continuing....");
				continue;
			}

			if (i + 1 < listpreLimDef.size() && inOrder && !getLastDef) {

				String[] ary = { listpreLimDef.get(i)[0], listpreLimDef.get(i + 1)[0], "def", listpreLimDef.get(i)[1] };
				// //NLP.pwNLP.append(NLP.println("prelim - def in order - added to def list=" +
				// Arrays.toString(ary), ""));
//				System.out.println("adding ary. listDefFinalVal");
				listDefFinalVal.add(ary);

			} else {

				eIdxSecHdg = 0;
				// a consecutive list of defined terms can only relate to one
				// section heading. use section heading pattern tool to get last
				// def if it is prior to what last definition otherwise
				// retrieves. Do this by running definitions retrieval after
				// getting sec locations. Note this in body where def, sec and
				// exh retrievals are listed.

				String lastDef = ContractParser.getLastDefinition(text, sIdx, listpreLimDef.get(i)[1], "def", i,
						listpreLimDef.size());
				// System.out.println("lastDef="+lastDef);
				tmpIdx = lastDef.length();

				Pattern patternExhibitInContract = Pattern.compile(
						"(?<=([\r\n]{1}[ \t]{0,110}))(Exhibit|EXHIBIT|APPENDIX|Appendix|SCHEDULE|Schedule|Annex|ANNEX)"
								+ "[ ]{1,3}[A-Z\\-\\d\\(\\)]{1,5}( to)? ?[\r\n]{1}");

				List<String[]> listEnd = nlp.getAllStartIdxLocsAndMatchedGroups(lastDef, patternExhibitInContract);

				// NLP.printListOfStringArray("list to find end of last def=",
				// listEnd);

				// //NLP.pwNLP.append(NLP.println("lastDef=", lastDef + "||end"));

				if (listEnd.size() > 0) {
					// if >0 than it found endOfK before end of lastSection txt
					// - so it is earlier end point and idx to use.

					tmpIdx = Integer.parseInt(listEnd.get(0)[0]);
					// //NLP.pwNLP.append(NLP.println("end of last Def=",
					// Arrays.toString(listEnd.get(0))
					// +"|end last def")
					// + " text.snipp="
					// + text.substring(
					// sIdx + tmpIdx,
					// Math.min((sIdx + tmpIdx + 100),
					// text.length())
					// ));

				}

				eIdx = sIdx + tmpIdx;

				String[] ary = { listpreLimDef.get(i)[0], eIdx + "", "def", listpreLimDef.get(i)[1] };
				// System.out.println("1b prelim - def in order - added to def list=" +
				// Arrays.toString(ary));
//				System.out.println("2.adding ary. listDefFinalVal");

				listDefFinalVal.add(ary);

			}

			prev4Ltr = prev3Ltr;
			prev3Ltr = prev2Ltr;
			prev2Ltr = prevLtr;
			prevLtr = ltr;

		}

//		NLP.printListOfStringArray("listDefFinalVal=", listDefFinalVal);
		sIdx = 0;
		eIdx = 0;
		prevIdx = 0;
		String def;
		TreeMap<Integer, String[]> mapDef = new TreeMap<Integer, String[]>();
		for (int i = 0; i < listDefFinalVal.size(); i++) {
			sIdx = Integer.parseInt(listDefFinalVal.get(i)[0]);
			eIdx = Integer.parseInt(listDefFinalVal.get(i)[1]);
			def = listDefFinalVal.get(i)[3];
			String[] ary = { sIdx + "", eIdx + "", "def", def };
			mapDef.put(-sIdx, ary);
		}

//		NLP.printMapIntStringAry("mapDef==", mapDef);

		for (int i = 0; i < listShallMeanValidated.size(); i++) {
//			System.out.println("listShallMeanValidated==" + Arrays.toString(listShallMeanValidated.get(i)));
			if (mapDef.containsKey(-Integer.parseInt(listShallMeanValidated.get(i)[0]))) {
				continue;
			} else {
				String[] ary = { listShallMeanValidated.get(i)[0], "getEidx", "def", listShallMeanValidated.get(i)[1] };
				mapDef.put(-1 * (Integer.parseInt(listShallMeanValidated.get(i)[0])), ary);
//				System.out.println("not in map, putting it in==" + Arrays.toString(listShallMeanValidated.get(i)));
			}
		}

		String eIdxStr, sIdxStr, sIdxStrPrior = null, defPrior = null;

		mapDefAdded = new TreeMap<Integer, String[]>();
		for (Map.Entry<Integer, String[]> entry : mapDef.entrySet()) {
			sIdxStr = entry.getValue()[0];
			eIdxStr = entry.getValue()[1];
			def = entry.getValue()[3];
			mapDefAdded.put(-1 * entry.getKey(), entry.getValue());
//			System.out.println("1.adding....=" + Arrays.toString(entry.getValue()));
			if (eIdxStr.equals("getEidx")) {
				if (sIdxStrPrior == null) {
					String[] lastAry = { sIdxStr, (Integer.parseInt(sIdxStr) + def.length()) + "" };
//					System.out.println("lastAry=" + Arrays.toString(lastAry));
					int sIdx1 = GetContracts.getLastDefinedTermSimple(lastAry, text);
					String[] a1 = { sIdxStr, sIdx1 + "", "def", def };
					mapDefAdded.put(-1 * entry.getKey(), a1);
//					System.out.println("2. a1adding....=" + Arrays.toString(a1));
					sIdxStrPrior = sIdx1 + "";
					continue;
				}
				if (sIdxStrPrior != null) {
					String[] a1 = { sIdxStr, sIdxStrPrior, "def", def };
					mapDefAdded.put(-1 * entry.getKey(), a1);
//					System.out.println("2. a1adding....=" + Arrays.toString(a1));
				}

			}
			sIdxStrPrior = sIdxStr;
			defPrior = def;
		}

		// if def terms is found between 2 def terms then the eIdx of the first overlaps
		// with the sIdx of second and at a minimum the first is wrong. figure out how
		// to address.

		List<String[]> listDefFinal_new = new ArrayList<String[]>();
		for (Map.Entry<Integer, String[]> entry : mapDefAdded.entrySet()) {
			listDefFinal_new.add(entry.getValue());
		}

//		NLP.printMapIntStringAry("after adding shall mean -- mapDef==", mapDef);
//		NLP.printMapIntStringAry("after adding shall mean -- mapDefAdded==", mapDefAdded);
//		NLP.printListOfStringArray("listDefFinal_new=", listDefFinal_new);
		SolrPrep.mapDefAdded = new TreeMap<Integer, String[]>();

		return listDefFinal_new;

	}

	public static List<String> sentenceSectionCleanup(String text) throws IOException {
		NLP nlp = new NLP();
		boolean isItMostlyInitialCaps = false;

//		System.out.println("sentenceCleanup text=="+text);
		String origText = text;

		List<String> listSec = nlp.getAllMatchedGroups(text,
				Pattern.compile("(?sm)(^)S(ection|ECTION) [\\d\\.]{1,8}[ ]{1,2}(?=[A-Z]{1})"));

		List<String> listSentence = new ArrayList<>();

		String str = "";
		if (listSec.size() == 1) {
			str = listSec.get(0);
			// cut out 'Section' so that split '.' doesn't get corrupted
		}

		isItMostlyInitialCaps = SolrPrep.isItMostlyInitialCaps(str);

		if (isItMostlyInitialCaps) {
			// System.out.println("str="+str);
			// System.out.println("text="+text.substring(str.length(),text.length()).trim());
			text = origText.substring(str.length(), origText.length());
			String[] ary = text.split("\\.(?=[ ]{1,2}[A-Z]{1})");

			// System.out.println(
			// "ary[0].len=" + ary[0].length() + " str.len=" + str.length() +
			// "origText.len=" + origText.length());
			if (ary.length > 0 && str.trim().length() > 0) {
				str = origText.substring(0, Math.min(str.length() + ary[0].length() + 1, origText.length()));
				// str now is equal to section through to first '.'
				listSentence.add(str.trim());
				listSentence.add(origText.substring(str.length(), origText.length()).trim());
				// System.out.println("is Sent 1=" + str);
				// System.out.println("is Sent 2=" + origText.substring(str.length(),
				// origText.length()).trim());
				return listSentence;
			}

			else
				listSentence.add(origText);
			return listSentence;
		}

		else
			listSentence.add(origText);

		return listSentence;

	}

	public static List<String> sentenceSubHeadingCleanup(List<String> list) throws IOException {

		// this was used originally in order to remove sub-headings from a sentence by
		// breaking into 2: "Duties of the Trustee. The Trustee shall .. .."
		// NOTE: unclear if I need to build this

		List<String> listSent = new ArrayList<>();

		NLP nlp = new NLP();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			// keep it simply - use Stringbuilder (always!), nlp.getMatchedGroups
		}

		// chg return list
		return listSent;

	}

	public static List<String> sentenceMultipleCleanup(List<String> list) throws IOException {

		NLP nlp = new NLP();

		String str = "", sent = "", sentNext = "";

		List<String> listSent = new ArrayList<>();
		int eIdx = 0, nextEidx = 0, priorEidx = 0;
		// works only where listSentence == 1 - break after.
//		System.out.println("4d1 sentenceMultipleCleanup - list.size==" + list.size());
		for (int i = 0; i < list.size(); i++) {
			str = list.get(i);
//			System.out.println("4d2 sentenceMultipleCleanup list====" + str);

			if (str.replaceAll("[ \t\r\n]+", "").length() < 1) {
//				System.out.println("4d2a sentenceMultipleCleanup continue");
				continue;
			}

			Pattern pattern = Pattern.compile("(?sm)" + "(, ?(or ?|and ?)?[\r\n]{3,})"
					+ "|(?<=[a-z]{3}\\)?\\.)[ \r\n]{1,5}(?=[A-Z]{1}[a-z]{1})"
					+ "|\r\n([A-Z]{1}([A-Za-z]{2,25}) ((and |or |the |of |to )?([A-Z]{1}[A-Za-z]{2,25} ?)){1,8}[\r\n]{1,6}){2,4}"
					+ "");
			// I find commas ',' followed by at least 2 hard returns
//			System.out.println("4d3a sentenceMultipleCleanup continue");
			List<Integer> listSentence = nlp.getAllIndexEndLocations(str, pattern);
//			System.out.println("sentenceMultipleCleanup 1");

			if (listSentence.size() == 0) {
				listSent.add(str);
//				System.out.println("sentenceMultipleCleanup.1a .str add str=" + str);
			}

			// add False Positives
//			System.out.println("sentenceMultipleCleanup list.size==" + listSentence.size());
			for (int n = 0; n < listSentence.size(); n++) {
//				System.out.println("sentenceMultipleCleanup 2. i=" + i);
				eIdx = listSentence.get(n);
				if (n + 1 < listSentence.size()) {
					nextEidx = listSentence.get(n + 1);
					sentNext = str.substring(eIdx, nextEidx);
				}
				if (n + 1 == listSentence.size()) {
					nextEidx = str.length();
					sentNext = str.substring(eIdx, nextEidx);
				}

				if (n == 0) {
					sent = str.substring(0, eIdx);
				}
				if (n > 0) {
					sent = str.substring(priorEidx, eIdx);
				}

//				System.out.println("sentenceMultipleCleanup.1b sent?=" + sent);
//				System.out.println("sentenceMultipleCleanup.1b sentNext?=" + sentNext);

				if (isSentence(sent) || isSentence(sentNext) || sent.length() > 400 || sentNext.length() > 400) {
					listSent.add(sent);
					listSent.add(sentNext);
					priorEidx = nextEidx;
					// if(temp){
//					System.out.println("1 .add sent=" + sent + "\r\n\"1b .add sentNext=" + sentNext);
					// }

				} else {
					// System.out.println("3 no sent=" + sent);
					listSent.add(sent + sentNext);
					priorEidx = nextEidx;
					// if(temp){
//					System.out.println("2a .add sent=" + sent + "\r\n\"2b .add sentNext=" + sentNext);
					// }
				}
				// break;
			}
		}

		return listSent;

	}

	public static List<String> clauseSemiColon(List<String> list) {

		// List<String> listClau = new ArrayList<>();
		// split sentence by ; - provided both are big chunks.
		String text = "", clau = "";
		NLP nlp = new NLP();

		Pattern patternClau1 = Pattern.compile("(?sm)[A-Za-z\\)]{2}: ");
		Pattern patternClau1b = Pattern.compile("(?sm);( ?and| ?or)? ");

		// patternSent1 picksup sentence after ':'
		Pattern patternClause = Pattern.compile(patternClau1.pattern() + "|" + patternClau1b.pattern());

		int idx = 0;
		List<String> listC = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {

			text = list.get(i);
			idx = 0;
			List<Integer> listClaus = nlp.getAllIndexEndLocations(text, patternClause);
			// System.out.println("listSemi - size="+listSemi.size());
			int eIdx = 0, priorEidx = 0;
			if (listClaus.size() == 0) {
				listC.add(text);
				System.out.println("clause listC=" + text + "||");
			} else {

				for (int n = 0; n < listClaus.size(); n++) {
					eIdx = listClaus.get(n);

					if (listClaus.size() == 1) {
						clau = text.substring(0, eIdx);
						// System.out.println("clau2 .add=" + clau + "||END\r\n");
						System.out.println("clause listC=" + clau + "||");
						listC.add(clau);
						clau = text.substring(eIdx, text.length());
						listC.add(clau);
						// System.out.println("clau3 .add=" + clau + "||END\r\n");
						break;
					}

					if (n == 0 && listClaus.size() != 1) {
						clau = text.substring(0, eIdx);
						listC.add(clau);
						System.out.println("clause listC=" + clau + "||");
						priorEidx = eIdx;
						continue;
					}

					if (n + 1 != listClaus.size() && n != 0) {
						clau = text.substring(priorEidx, eIdx);
						System.out.println("clause listC=" + clau + "||");
						listC.add(clau);
						priorEidx = eIdx;
						// System.out.println("clau2.add=" + clau + "||END\r\n");
						continue;
					}

					if (n + 1 == listClaus.size()) {
						clau = text.substring(priorEidx, eIdx);
						listC.add(clau);
						// System.out.println("clau3.add=" + clau + "||END\r\n");
						clau = text.substring(eIdx, text.length());
						System.out.println("clause listC=" + clau + "||");
						listC.add(clau);
						// System.out.println("clau4.add=" + clau + "||END\r\n");
					}
				}
			}
		}

		return listC;

	}

	public static List<String> sentenceSemiColon(List<String> list) throws IOException {

		// List<String> listSent = new ArrayList<>();
		// split sentence by ; - provided both are big chunks.
		String text = "", sent = "";
		NLP nlp = new NLP();

		int idx = 0;
		List<String> listS = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {

			text = list.get(i);
			idx = 0;
			List<Integer> listSemi = nlp.getAllIndexEndLocations(text, patternSentence);
			// System.out.println("listSemi - size="+listSemi.size());
			int eIdx = 0, priorEidx = 0;
			if (listSemi.size() == 0) {
				listS.add(text);
			} else {

				for (int n = 0; n < listSemi.size(); n++) {
					eIdx = listSemi.get(n);

					if (listSemi.size() == 1) {
						sent = text.substring(0, eIdx);
						// System.out.println("sent2 .add=" + sent + "||END\r\n");
						listS.add(sent);
						sent = text.substring(eIdx, text.length());
						listS.add(sent);
						// System.out.println("sent3 .add=" + sent + "||END\r\n");
						break;
					}

					if (n == 0 && listSemi.size() != 1) {
						sent = text.substring(0, eIdx);
						listS.add(sent);
						// System.out.println("sent1.add=" + sent + "||END\r\n");
						priorEidx = eIdx;
						continue;
					}

					if (n + 1 != listSemi.size() && n != 0) {
						sent = text.substring(priorEidx, eIdx);
						listS.add(sent);
						priorEidx = eIdx;
						// System.out.println("sent2.add=" + sent + "||END\r\n");
						continue;
					}

					if (n + 1 == listSemi.size()) {
						sent = text.substring(priorEidx, eIdx);
						listS.add(sent);
						// System.out.println("sent3.add=" + sent + "||END\r\n");
						sent = text.substring(eIdx, text.length());
						listS.add(sent);
						// System.out.println("sent4.add=" + sent + "||END\r\n");
					}
				}
			}
		}

		return listS;

	}

//	public static void getClauses(String text, String filePathandName) throws FileNotFoundException {
//		// this runs against sent xml - which is transferred here from sbSent.toString
//
//		StringBuilder sbClau = new StringBuilder();
//		File fileXMLclause = new File(filePathandName);
//
//		// put search parameters here.
//
//		if (fileXMLclause.exists())
//			fileXMLclause.delete();
//
//		PrintWriter pwClause = new PrintWriter(fileXMLclause);
//		pwClause.append(sbClau.toString());
//		pwClause.close();
//
//	}

	public static boolean isSentence(String text) {

		// System.out.println("sent.len="+text.length());
		NLP nlp = new NLP();
		// boolean isSent = false;
		String tmpStr = text.replaceAll("[\\d\\~\\`\\!\\@\\#\\^\\+\\=\\?\\/\\\\\"\\*\\$\\[\\]\\}\\{\\.\\%]+", " ")
				.replaceAll("[ ]+", " ").trim();
		double cntInitialCaps = nlp.getAllIndexStartLocations(tmpStr, Pattern.compile("( |^)[A-Z]{1}")).size();
		double cntWords = tmpStr.split("[ \r\n]").length;

		// System.out.println("cntWords=" + cntWords + " cntInitialCaps=" +
		// cntInitialCaps
		// + " sent.len=" + text.length());
		if (cntInitialCaps / cntWords < .25 && cntWords > 50 && cntWords > 20
				|| cntInitialCaps / cntWords < .35 && cntWords <= 50 && cntWords > 20) {
			return true;
		}

		return false;
	}

	public static void getContractReadyForSolr(String year, String qtr, String contractFolder)
			throws IOException, SQLException {

		NLP nlp = new NLP();

		File folderStrip = new File(
				GetContracts.strippedFolder + "/" + contractFolder + "/" + year + "/QTR" + qtr + "/");

		File[] listOfStrippedFiles = folderStrip.listFiles();
		// this is where contracts are saved that can be ingested into solr
		// String contractPathYrQtr = contractsPath + contractFolder + "/" +
		// year
		// + "/QTR" + qtr + "/";
		// FileSystemUtils.createFoldersIfReqd(contractPathYrQtr);

		@SuppressWarnings("unused")
		String kId = "", fileDate, cik, filer = "", text = "", textSplit = "";
		// StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();

		// this listOfFiles is either pulling from stripped or downloaded path.
		// If d path must be stripped.

		@SuppressWarnings("unused")
		String contractName = "";
		contractLongName = "";
		// int cnt = 0;

		for (File f : listOfStrippedFiles) {
			text = "";
			contractLongName = f.getName();
			if (nlp.getAllMatchedGroups(f.getName(), Pattern.compile("(?ism)jpeg|jpg|gif")).size() > 0
					|| nlp.getAllMatchedGroups(f.getName(),
							Pattern.compile("(?ism)(^|[\r\n ]{1})(GRAPHIC|IMAGE)($|[\r\n \\.]{1})")).size() > 0) {
				continue;
			}

			text = Utils.readTextFromFile(f.getAbsolutePath());
			if (text.length() < 100)
				continue;
			textSplit = text.substring(0, Math.min(text.length(), 3000)).replaceAll("^[ ]{0,10}", "")
					.replaceAll("^[\r\n]{3,10}", "");
			filer = textSplit.split("\\|\\|")[3].replaceAll("xxPD", "\\.").trim();
			filer = filer.substring(0, filer.indexOf("\r")).replaceAll("\r", "");
			kId = nlp.getAllMatchedGroups(f.getName(), Pattern.compile("[\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,2}"))
					.get(0);

			// //NLP.pwNLP.append(NLP.println("getContractReadyForSolr - kId=" +
			// kId, ""));
			// //NLP.pwNLP.append(NLP.println("get K - kId=", kId));
			// //NLP.pwNLP.append(NLP.println("get K - not stripped - file=",
			// f.getAbsolutePath()));

			cik = textSplit.split("\\|\\|")[1].trim();
			fileDate = textSplit.split("\\|\\|")[2].trim() + "T00:00:00Z";
			contractName = textSplit.split("\\|\\|")[0].replaceAll("xxPD", "\\.").trim();

			// //NLP.pwNLP.append(NLP.println("saving K- cik=" + cik + "fileDate=" +
			// fileDate + "filer=", filer));

			sb2.append(kId + "||" + contractLongName + "\r\n");
		}

		File f4 = new File("e:/getContracts/" + year + "QTR" + qtr + "contractLongNames.txt");
		if (f4.exists())
			f4.delete();
		PrintWriter pw2 = new PrintWriter(f4);
		pw2.println(sb2.toString());
		pw2.close();
		sb2.delete(0, sb2.toString().length());

		String query = "LOAD DATA INFILE 'e:/getContracts/" + year + "QTR" + qtr + "contractLongNames.txt'"
				+ "Ignore INTO TABLE k_longNames FIELDS TERMINATED BY '\\|\\|' LINES TERMINATED BY '\n'";

		MysqlConnUtils.executeQuery(query);

	}

	public static boolean isItTwoSecDifCounters(String priorTwoBefSecNmb, String priorSecStr, String curSecStr,
			String nextSecStr) {

		// boolean isItTwoCounters = false;

		if (priorTwoBefSecNmb.replaceAll("[^\\d]", "").length() > 0 && priorTwoBefSecNmb.length() > 0
				&& curSecStr.replaceAll("[^\\d]", "").length() > 0 && curSecStr.length() > 0
				&& nextSecStr.replaceAll("[^\\d]", "").length() > 0 && nextSecStr.length() > 0) {

			double priorTwoBefNmb = Double.parseDouble(priorTwoBefSecNmb),
					priorSecNmb = Double.parseDouble(priorSecStr), curSecNmb = Double.parseDouble(curSecStr),
					nextSecNmb = Double.parseDouble(nextSecStr);

			// if 2.8 => 2.9 =>2.10 || 7.8=>7.9=>7.10
			// System.out.println("priorTwoBefSecNmb=" + priorTwoBefSecNmb
			// + " priorSecStr=" + priorSecStr + " curSecStr=" + curSecStr
			// + " nextSecStr=" + nextSecStr);

			// System.out.println("priorSecNmb - curSecNmb="
			// + (priorSecNmb - curSecNmb) + "\rpriorSecNmb - priorTwoBefNmb="
			// + (priorSecNmb - priorTwoBefNmb) + "\rnextSecNmb - curSecNmb="
			// + (nextSecNmb - curSecNmb));

			// 7.8=>7.9=>(7.10=>7.11 or 7.10=>8.1). 7.10 is current
			if ((priorSecNmb - curSecNmb) > .795 && (priorSecNmb - curSecNmb) < .805
					&& (priorSecNmb - priorTwoBefNmb) > .095 && (priorSecNmb - priorTwoBefNmb) < .105
					&& (((nextSecNmb - curSecNmb) > .0095 && (nextSecNmb - curSecNmb) < .0105)
							|| ((nextSecNmb - curSecNmb) > .995) && (nextSecNmb - curSecNmb) < 1.005)) {
				// System.out.println("isItTwoSecDifCounters=true");
				return true;
			}
			// if so ck if by adding either .1 or .01 works for forecasted.
		}

		return false;
	}

	public static double getSecDif(List<String[]> list) {
		// returns the most common difference between sections - e.g., 110 to
		// 111 - then difference is 1. or 1.01 to 1.02 - dif is .01. This will
		// count that dif which occur most frequently and return that value.
		// This can then be used to forecast next sec value.

		TreeMap<Double, Integer> mapCount = new TreeMap<>();

		secDif = 0.0;
		artDif = 0.0;
		double sec, prevSec = 0, secDifTmp = 0.0;
		String secStr = "";
		for (int i = 0; i < list.size(); i++) {
			secStr = getSectionNumber(list.get(i)[1]);
			if (secStr.length() < 1)
				continue;
			sec = Math.round(Double.parseDouble(secStr) * 100000.000) / 100000.000;
			if (i > 0) {
				// secDifTmp = Math.round(sec* 100000.000 - prevSec* 100000.000)
				// / 100000.000;
				secDifTmp = Math.round((sec - prevSec) * 100000.000) / 100000.000;
				// //NLP.pwNLP.append(
				// NLP.println("mapCount - secDifTmp=", secDifTmp + " sec=" + sec + " prevSec="
				// + prevSec));
				if (mapCount.get(secDifTmp) == null || mapCount.get(secDifTmp) < 1) {
					mapCount.put(secDifTmp, 1);
				} else
					mapCount.put(secDifTmp, mapCount.get(secDifTmp) + 1);
			}
			prevSec = sec;
		}

		double val = 0, maxVal = -1;
		for (Map.Entry<Double, Integer> entry : mapCount.entrySet()) {
			val = entry.getValue();

			if (val > maxVal) {
				secDifTmp = entry.getKey();
				// //NLP.pwNLP.append(
				// NLP.println("ck highest secDifTmp=", secDifTmp + " val=" + val + " prior
				// maxVal=" + maxVal));
				maxVal = val;
			}

			// secDif =.01 etc. -- I don't need secDifCnt - I need secDf
			// measure secDifCnt against list size. >80%!
		}

		// //NLP.pwNLP.append(NLP.println("ck highest count secDifTmp=",
		// secDifTmp + " maxVal cnt=" + maxVal + " total sec hdgs=" + list.size()));

		if (maxVal / ((double) list.size()) > .65) {
			secDif = secDifTmp;
			artDif = getArtDif(mapCount, list);
			// //NLP.pwNLP.append(NLP.println("final artDif=", artDif + ""));
			return secDif;

		}

		else
			return 0;

	}

	public static double getArtDif(TreeMap<Double, Integer> mapCount, List<String[]> list) {

		/*
		 * GETS CALLED FROM getSecDif find articles by getting from list those cases
		 * where difference b/w section numbers is not equal to the difference commonly
		 * seen b/w sections. For example going from 1701 to 1702 is typical of two
		 * consecutive sections and secDif=1. Going from 1619 to 1701 results in a
		 * difference of 82 - which is not equal to secDif nor typical of consecutive
		 * sections but more likely showing consecutive articles. I then record these
		 * sections into a list. I then cycle through this list and determine the
		 * difference between article by subtracting a single number of each sec number
		 * against prev sec number until it equals 1. If it stops at 2nd number and that
		 * is last number prior to decimal - then articles change by 1. If it is 2nd
		 * number and 3rd number before decimal than it changes by 100. I have to create
		 * an exception in straddling when prior section number rolls to 10 Eg 9.09 to
		 * 10.01 but not 1.9 to 2.01 or 909 to 1001. I then record each determined
		 * artDif to a map for counting which occurs most often. I then product the one
		 * that occurs most often and use that. the map
		 */

		List<Double[]> listArt = new ArrayList<Double[]>();

		double sec = 0.0, prevSec = 0.0, prevSec2 = 0.0, artDif = 0.0, secDifTmp = 0.0, secDifTmp2 = 0.0,
				artDifTmp = 0.0;
		String secStr = "";
		for (int i = 0; i < list.size(); i++) {

			secStr = getSectionNumber(list.get(i)[1]);
			if (secStr.length() < 1)
				continue;
			sec = Double.parseDouble(secStr);

			/*
			 * setup to ensure only grab articles where dif is not equal to that of secDif
			 * and I capture the first section instance of that prior article (and not the
			 * last instance. I.e., grab 2.01 and not 2.09 so that when i get to 3.01 of new
			 * article - I'm recording current - 3.01 and prior 2.01! This is then used to
			 * measure numerical differenc b/w articles - which is 1 here - and then to
			 * count how many times it is 1 thru out toc/secs.
			 */
			if (i > 0) {
				secDifTmp = Math.round(sec * 100000.000 - prevSec * 100000.000) / 100000.000;
				secDifTmp2 = Math.round(sec * 100000.000 - prevSec2 * 100000.000) / 100000.000;
				if (secDifTmp != secDif && secDifTmp2 != secDif) {
					// //NLP.pwNLP.append(NLP.println("this is new article? sec=", sec + "
					// prevSec2=" + prevSec2));

					Double[] aryDbl = { sec, prevSec2 };
					listArt.add(aryDbl);
					prevSec2 = sec;
				}

				prevSec = sec;
				// I then need to count each artDiv that is same via mapCount
				// technique.
			}

			if (i == 0) {
				prevSec2 = sec;
			}

		}

		// NO LONGER FOLLOWED - THIS WAS FAR TOO COMPLEXT!!!!!!
		/*
		 * now loop thru and find artDifs by compaing each single # in sec against
		 * corresponding of prior and record to map (counting those that repeat).
		 * Formula if sec#-prevSec# = 1 then measure how many numbers follow it that
		 * precede decimal. Or if after decimal how many places after decimal is it. If
		 * 1619 to 1701 dif of 1 occurs at 2nd # and 2 #s follow it -- so artDif=100. If
		 * 16.19 to 17.19 it is also 2nd #s that produce if cur sec nmb len>prior sec
		 * nmb len - I know that I'm rolling from 909 to 1001 - and I should probably
		 * just skip versus trying to measure thru complex ruleset.
		 */

		TreeMap<Integer, Integer> mapArtCount = new TreeMap<Integer, Integer>();

		secStr = "";
		String prevSecStr = "";// ,multStr = "1";

		double secN = 0.0, prevSecN = 0.0;
		int dif = 0;

		// differences between articles should be at least 1. And b/c I only
		// count instances where dif b/w secs are unusual these are the most
		// likely articles and dif b/w articles should probably always be
		// multiples of 1 or 10 or 100.
		for (int i = 0; i < listArt.size(); i++) {
			sec = listArt.get(i)[0];
			prevSec = listArt.get(i)[1];
			secStr = sec + "";
			prevSecStr = prevSec + "";
			secN = Double.parseDouble(secStr);
			prevSecN = Double.parseDouble(prevSecStr);
			dif = (int) (secN - prevSecN);
			if (mapArtCount.get(dif) == null || mapArtCount.get(dif) == 0) {
				mapArtCount.put(dif, 1);
				// //NLP.pwNLP.append(NLP.println("c mapArtCount=", 0 + ""));
			} else {
				mapArtCount.put(dif, mapArtCount.get(dif) + 1);
				// //NLP.pwNLP.append(NLP.println("d mapArtCount=", (mapArtCount.get(dif) + 1) +
				// ""));
			}

		}

		double val = 0, maxVal = -1;

		for (Map.Entry<Integer, Integer> entry : mapArtCount.entrySet()) {
			val = entry.getValue();

			if (val > maxVal) {
				artDifTmp = entry.getKey();
				// //NLP.pwNLP.append(
				// NLP.println("ck highest artDifTmp=", artDifTmp + " val=" + val + " prior
				// maxVal=" + maxVal));
				maxVal = val;
			}
			// secDif =.01 etc. -- I don't need secDifCnt - I need secDf
			// measure secDifCnt against list size. >80%!

		}

		// //NLP.pwNLP.append(NLP.println("ck highest count artDifTmp=",
		// artDifTmp + " maxVal cnt=" + maxVal + " total art hdgs=" + listArt.size()));

		if (maxVal / ((double) listArt.size()) > .6) {
			artDif = artDifTmp;
			return artDif;
		} else
			return 0.0;
	}

	public static List<String[]> getLastSecHdgFromToc(String curSecNmb, String curSidx) {
		// see if toc list incorrectly captured first sec hdg

		List<String[]> list = new ArrayList<>();
		String sIdx, secHdg, tocSecHdgNmb;

		if (listTocHdgs.size() > 0) {
			// System.out.println("last listTocHdgs=" +
			// Arrays.toString(listTocHdgs.get(listTocHdgs.size() - 1)));

			secHdg = listTocHdgs.get(listTocHdgs.size() - 1)[1];
			tocSecHdgNmb = getSectionNumber(secHdg);
			sIdx = listTocHdgs.get(listTocHdgs.size() - 1)[0];

			// //NLP.pwNLP.append(NLP.println("2 calling is isItConsecutiveInSameArticle",
			// ""));
			boolean isConsecutive = isItConsecutiveInSameArticle(tocSecHdgNmb, curSecNmb, "", 0);
			// //NLP.pwNLP.append(NLP.println("last sec from toc - same Art isConsecutive",
			// isConsecutive + " curSecNmb"));

			if (isConsecutive) {
				String[] ary = { sIdx, curSidx + "", "sec", secHdg };
				list.add(ary);
				// reset-so cur sec can be cared against next
			}
		}

		return list;

	}

	public static boolean isItTableOfContents(String text) {

		NLP nlp = new NLP();

		double cntSectionArticlesExhibits = nlp.getAllIndexStartLocations(text,
				Pattern.compile("(?sm)[\r\n]{2}[ \t]{0,20}(SECTION|Section|Article|ARTICLE)")).size();
//		System.out.println("cntSectionArticlesExhibits=="+cntSectionArticlesExhibits);
		if (cntSectionArticlesExhibits > 5 && text.length() < 800) {
			return true;
		}

		if (cntSectionArticlesExhibits > 10 && text.length() >= 800 && text.length() < 2000) {
			return true;
		}

		if (cntSectionArticlesExhibits > 15 && text.length() >= 2000) {
			return true;
		}

		else
			return false;

	}

	public static boolean isItMostlyInitialCaps(String secHdgTxt) {
		// is it a sec hdg - or f/ps: (1) are there mostly initial caps
		// or (2) is sechdg length the same length as entire section.

		NLP nlp = new NLP();
		double cntInitialCapsWords = nlp.getAllIndexStartLocations(secHdgTxt, Pattern.compile("( |^)[A-Z]")).size();
		double cntWords = secHdgTxt.split(" ").length;

//		System.out.println("cntWords=" + cntWords + " cntInitialCapsWords=" + cntInitialCapsWords);
		if (cntWords > 7 && cntInitialCapsWords / cntWords < .5) {
			// //NLP.pwNLP.append(NLP.println("isItMostlyInitialCaps=",
			// "false" + " cntWords=" + cntWords + " cntInitialCapWords=" +
			// cntInitialCapsWords));
			return false;
		} else
			return true;
	}

	public static double isItMostlyInitialCaps_measure(String secHdgTxt) {
		// is it a sec hdg - or f/ps: (1) are there mostly initial caps
		// or (2) is sechdg length the same length as entire section.

		NLP nlp = new NLP();
		double cntInitialCapsWords = nlp.getAllIndexStartLocations(secHdgTxt, Pattern.compile("( |^)[A-Z]")).size();
		secHdgTxt = secHdgTxt.replaceAll("(^| )[a-z]{1}[,:; ]", "");
//		System.out.println("secHdgTxt==="+secHdgTxt);
		double cntWords = secHdgTxt.split(" ").length;

//		System.out.println("cntWords="+cntWords+" cntInitialCapsWords="+cntInitialCapsWords);
		if (cntWords >=4) {
			// //NLP.pwNLP.append(NLP.println("isItMostlyInitialCaps=",
			// "false" + " cntWords=" + cntWords + " cntInitialCapWords=" +
			// cntInitialCapsWords));
			return cntInitialCapsWords / cntWords;
		} else
			return 1.0;
	}

	public static boolean isSectionBlank(String secHdgTxt, String secTxt) {

		int secHdgLen = secHdgTxt.trim().length();
		int secTxtLen = secTxt.replaceAll("[\r\n\t]", " ").replaceAll("[ ]+", " ").trim().length();

		if (secHdgLen + 5 > secTxtLen) {
			// //NLP.pwNLP.append(
			// NLP.println("isSectionBlank=", "true" + " secHdgLen=" + secHdgLen + "
			// secTxtLen=" + secTxtLen));
			return true;
		}

		return false;
	}

	public static boolean sectionPrecededByArticle(String text) {
		boolean ArticlePrecedesSection = false;
		// if section is preceded by ARTICLE in all caps than this is a section
		int art = 0;
		NLP nlp = new NLP();
		art = nlp.getAllIndexEndLocations(text, Pattern.compile("ARTICLE")).size();
		if (art > 0)
			ArticlePrecedesSection = true;
		if (text.contains("[a-z]"))
			ArticlePrecedesSection = false;
		return ArticlePrecedesSection;
	}

	public static List<String[]> validateSecHdgList(List<String[]> list, String text) throws IOException {

		// PrintWriter pw = new PrintWriter(new
		// File("e:/getContracts/temp/validateSecHdgs.txt"));

		// ensures sec hdgs are consecutive
		List<String[]> lastTocSecAry = new ArrayList<String[]>();
		List<String[]> listValidated = new ArrayList<String[]>();
		String origCurSecHdg = "", curSecHdg = "", nextSecHdg = "", twoAheadSecHdg = "", priorSecHdg = "",
				curSecNmb = "", nextSecNmb = "", twoAheadSecNmb = "", priorSecNmb = "", priorTwoBefSecNmb = "",
				lastSection = "";
		NLP nlp = new NLP();
		// only for testing - can remove listSkipped
		List<String[]> listSkipped = new ArrayList<String[]>();

		boolean isItConsecutiveInSameArticle = false, skip6b = false, foundTwoSecDif = false, isItTwoSecDif = false,
				isItBlankSection = false, isItInitialCaps = true, foundEnd = false, tocPageNumberFound = false,
				gotIt = false;
		isConsecutiveArticle = false;
		double rounded = 1; // decimal is 2.01 than rounded = 100 if 2.1 than 10
							// but if 2.10 it is 100.
		double curSec = 0.0, priorSec = 0.0, nextSec = 0.0, curSecForecastSimple = 0.0;
		Integer curSidx = 0, nextSidx = 0, nIdx = 0, priorSidx = 0, twoAheadSidx = 0, cnt = 0;
		secDif2 = 0.0;
		secDif = getSecDif(list);

		for (int i = 0; i < list.size(); i++) {
			skip6b = false;
			isItTwoSecDif = false;
			gotIt = false;
			if (foundEnd) {
				if (cnt == 0) {
					String[] ary1 = { list.get(i)[0], text.length() + "", "sec", "" };
					listValidated.add(ary1);
//					System.out.println("1x - ary=" + Arrays.toString(ary1));
					// pw.append("\r\n1 .add=" + Arrays.toString(ary1));
				}
				cnt++;
				continue;
			}

			isConsecutiveArticle = false;
			isItConsecutiveInSameArticle = false;
			origCurSecHdg = list.get(i)[1];
			// pw.append("\r\n1a origCurSecHdg" + origCurSecHdg);

			tocPageNumberFound = false;

			curSecHdg = origCurSecHdg.replaceAll("[\r\n]", " ").replaceAll("[ ]+", " ");
			curSecNmb = getSectionNumber(curSecHdg);
			// pw.append("\r\n1b getSectionNumber - curSecHdg" + curSecHdg + " curSecNmb=" +
			// curSecNmb);

			if (curSecNmb.length() < 1 || curSecHdg.toLowerCase().contains("subject"))
				continue;
			curSec = Double.parseDouble(curSecNmb);
			if (priorSecNmb.length() > 0) {
				priorSec = Double.parseDouble(priorSecNmb);
				// pw.append("\r\n1c - priorSecNmb=" + priorSecNmb + " priorSec=" + priorSec);
			}

			curSidx = Integer.parseInt(list.get(i)[0]);
//			System.out.println("curSidx=" + curSidx);

			// if lastToc sec hdg is consec add as first to list (if consec it
			// would have grabbed first sec hdg of K Body.

			if (i == 0 && list.size() > 9 && listTocHdgs.size() < 9) {
				// see if toc list incorrectly captured first sec hdg
				lastTocSecAry = getLastSecHdgFromToc(curSecNmb, curSidx + "");
				if (null != lastTocSecAry && lastTocSecAry.size() > 0) {
					String[] ary = lastTocSecAry.get(0);
					listValidated.add(ary);
					System.out.println("2x - last secHdgFromToc..ary=" + Arrays.toString(ary));
					// pw.append("\r\n1d - .add lastTocSecAry=" + Arrays.toString(ary));
				}
			}

			if (i + 1 < list.size()) {
				nextSecHdg = list.get(i + 1)[1].replaceAll("[\r\n]", " ").replaceAll("[ ]+", " ");
				nextSecNmb = getSectionNumber(nextSecHdg);
				if (nextSecNmb.length() < 1) {
//					System.out.println("continuing x1=" + Arrays.toString(list.get(i)));
					continue;
				}

				nextSec = Double.parseDouble(nextSecNmb);
				nextSidx = Integer.parseInt(list.get(i + 1)[0]);
				// pw.append("\r\n1e - curSidx=" + curSidx + " nextSidx=" + nextSidx + "
				// nextSecNmb=" + nextSecNmb
				// + " nextSec=" + nextSec);
			}

			if (i + 2 < list.size()) {
				twoAheadSecHdg = list.get(i + 2)[1].replaceAll("[\r\n]", " ").replaceAll("[ ]+", " ");
				twoAheadSecNmb = getSectionNumber(twoAheadSecHdg);
				if (twoAheadSecNmb.length() < 1) {
//					System.out.println("continuing x2=" + Arrays.toString(list.get(i)));
					continue;
				}

				twoAheadSidx = Integer.parseInt(list.get(i + 2)[0]);
				// pw.append("\r\n1f - twoAheadSecHdg=" + twoAheadSecHdg + " twoAheadSecNmb=" +
				// twoAheadSecNmb
				// + " nextSecNmb=" + nextSecNmb + " twoAheadSidx=" + twoAheadSidx);

			}

			// is it a sec hdg - or f/ps: (1) are there mostly initial caps
			// or (2) is sechdg length the same length as entire section.
			isItInitialCaps = isItMostlyInitialCaps(curSecHdg);
			// isItBlankSection - entire section text is same length as sec hdg
			isItBlankSection = false;
			if (i + 1 < list.size()) {
				isItBlankSection = isSectionBlank(curSecHdg, text.substring(curSidx, nextSidx));
			}
			isItConsecutiveInSameArticle = isItConsecutiveInSameArticle(curSecNmb, nextSecNmb, priorSecNmb, i);
			// pw.append("\r\n1g - isItConsecutiveInSameArticle=" +
			// isItConsecutiveInSameArticle
			// + " isItMostlyInitialCaps=" + isItInitialCaps);

			// if cur sec is not what is expected based on prior sec than search
			// for what is forecasted and if found add it and if next thereafter
			// is consecutive add it. If next thereafter is not
			// consecutive don't add and continue.

			if (listValidated.size() > 0) {
				rounded = getNumberOfDecimalsPlaceToRoundTo(
						getSectionNumber(listValidated.get(listValidated.size() - 1)[3]));

				// pw.append("\r\n1h - rounded=" + rounded + " secDif=" + secDif + "
				// priorSecNmb="
				// + getSectionNumber(listValidated.get(listValidated.size() - 1)[3]));

			}

			// b/c double can default 2.02 to 2.01999 and converting 2.01999 to
			// int causes it to go to 2.01 I have to add .009 which will result
			// in int rounding down to 2.02 and not 2.01. Do this by taking 9
			// and dividing if by 9*round and adding it to double val

			if (secDif != 0 && listValidated.size() > 0
					&& listValidated.get(listValidated.size() - 1)[3].replaceAll("[ ]+", "").length() > 1) {

				// If I use a rounding mechanism elsewhere - test in tester!
				// round double value to number of decimal places

				// System.out.println("curSec - listV="
				// + Arrays.toString(listValidated.get(listValidated
				// .size() - 1)));

				curSecForecastSimple = roundDoubleValueToNumberOfDecimalPlace(
						Double.parseDouble(getSectionNumber(listValidated.get(listValidated.size() - 1)[3])), rounded);
				// pw.append("\r\n1i curSecForecastSimple=" + curSecForecastSimple + "
				// curSecNmb=" + curSecNmb);

				if (Double.parseDouble(curSecNmb) != curSecForecastSimple) {

					// if nextSidx is too early (b/c interrupted versus skipped
					// sec hdg - need to go to nextSidx two ahead. Why am I not
					// getting section 4.05?

					// Printout next sec hdg - to make
					// sure it is 4.06 and printout prior to make sure it is
					// 4.04 - then print text snipped contain curSidx to
					// nextSidx
					List<String[]> listIdxGrp = nlp.getAllStartIdxLocsAndMatchedGroups(
							text.substring(priorSidx, nextSidx),
							Pattern.compile("([\r\n\t]{1}.{0,5}|[ ]{4,30})(Section|SECTION)[ ]{1,20}"
									+ curSecForecastSimple + "[ ]{1,20}([A-Z][a-z]{1,15}(;|,|-)?[ ])+"));

					// pw.append("\r\ncurSecNmb!=curSecForecastSimple and did I find forecasted?=" +
					// listIdxGrp.size()
					// + " priorSecHdg=" + priorSecHdg + " curSecHdg=" + curSecHdg + " nextSecHdg="
					// + nextSecHdg);

					// found 4.05. It won't necessarily get correct secHdg -
					// which is okay
					if (listIdxGrp.size() > 0 && (Integer.parseInt(listIdxGrp.get(0)[0]) + priorSidx) < nextSidx - 30) {

						int nxIdx = Integer.parseInt(listIdxGrp.get(0)[0]);
						// NLP.pwNLP.append(NLP.println("got listIdxGrp=",
						// Arrays.toString(listIdxGrp.get(0))));

						// eg remove 4.04 b/c it has wrong eIdx at start of 4.06
						// instead of at 4.05.
						listValidated.remove(listValidated.size() - 1);
						// pw.append("\r\n1j removed=" +
						// Arrays.toString(listValidated.get(listValidated.size() - 1)));
						// insert 4.04 with same sIdx and new correct eIdx
						// (which is priorSidx to start of found 4.05)
						String[] ary = { priorSidx + "", (priorSidx + nxIdx) + "", "sec", priorSecHdg };
						// System.out.println("ary="+Arrays.toString(ary));
						listValidated.add(ary);
//						System.out.println("3x - ary=" + Arrays.toString(ary));
						// pw.append("\r\n1k .add=" + Arrays.toString(ary));

						// insert 4.05 w/
						String[] ary2 = { (priorSidx + nxIdx) + "", curSidx + "", "sec",
								listIdxGrp.get(0)[1].replaceAll("[ ]+", " ").trim() };
						listValidated.add(ary2);
//						System.out.println("4x - ary=" + Arrays.toString(ary2));
						// pw.append("\r\n1l .add=" + Arrays.toString(ary2));
					}
				}
			}

			if (!isItConsecutiveInSameArticle) {
				isItConsecutiveInSameArticle = isItConsecutiveNewArticle(curSecNmb, nextSecNmb, priorSecNmb,
						twoAheadSecHdg, i);

				// pw.append("\r\n1m isItConsecutiveNewArticle=" + isItConsecutiveInSameArticle
				// + " curSecNmb=" + curSecNmb
				// + " nextSecNmb=" + nextSecNmb + " priorSecNmb=" + priorSecNmb + "
				// twoAheadSecHdg="
				// + twoAheadSecHdg);

				/*
				 * !isConsec - see if forecasted val can be found. if 3.09 than forecast 3.10 if
				 * 3.1 than forecast 3.2. Take right of decimal and add 1 - 3.9 forecasts 3.10
				 * (ck in text between cur and next idx). return List<String[]> so I can add it
				 * to listValdiate ary. I then see if curSecHdg is consec with forecasted and if
				 * not rerun getForecasted - once it does return as consec I add curSecHdg ary.
				 * Create 10 count loop that breaks when is consec with cur sec hdg If I can't
				 * forecast break and set !isConsec and add blank.
				 */
			}

			// there are two SecDifs than I know if dif=.01 it is consecutive.
			// And will skip !isConsecutive and go straight to add to list.
			// once foundTwoSecDif set it is set for entire list.
			if (foundTwoSecDif && ((

			((nextSec - curSec) > .0095 && (nextSec - curSec) < .0105)

			) || ((curSec - priorSec) > .0095 && (curSec - priorSec) < .0105))) {

				isItConsecutiveInSameArticle = true;

				// pw.append("\r\n1m isItConsecutiveInSameArticle=" +
				// isItConsecutiveInSameArticle);

			}

			// see if consecutive by catching where it goes from 2.8=>2.9=>2.10.
			// Thereafter if foundTwoSecDif true and dif b/w .0095 and .0105 then
			if (i > 1 && !isItConsecutiveInSameArticle && i + 2 < list.size()) {
				isItConsecutiveInSameArticle = isItTwoSecDifCounters(priorTwoBefSecNmb, priorSecNmb, curSecNmb,
						nextSecNmb);
				secDif2 = 0.01;
				// PICKUP HERE!!
				// this means there is a .01 secDif and a .1 - so allow for
				// either when forecasting - if 2nd fails or now that I have
				// found this - keep going until secDif works - then come back
				// to this once this gets triggered. Think thru logic in excell
				// first. This way is better so less likelihood of f/p
				isItTwoSecDif = isItConsecutiveInSameArticle;
				foundTwoSecDif = isItConsecutiveInSameArticle;
				// pw.append("\r\n1n foundTwoSecDif/isItTwoSecDif=" +
				// isItConsecutiveInSameArticle);

			}

			// System.out.println("priorSec="+priorSec+" curSec="+curSec+"
			// nextSec="+nextSec+
			// " isItTwoSecDif="+isItTwoSecDif+
			// " foundTwoSecDif="+foundTwoSecDif);

			if (!isItConsecutiveInSameArticle) {
				// if not consecutive - can I find next section. If so add it
				// and the next thereafter.

				List<Integer> listI = new ArrayList<>();

				// System.out.println("text.len=" + text.length() + " curSidx="
				// + curSidx + " nextSidx=" + nextSidx);

				if (nextSidx > curSidx) {
					listI = nlp.getAllIndexStartLocations(text.substring(curSidx, nextSidx), Pattern.compile(
							"([\r\n\t]{1}.{0,5}|[ ]{4,30})(Section|SECTION) " + forecastedNextSection + "[\\. ]{1}"));
				}

				if (listI.size() > 0 && (listI.get(0) + curSidx) < nextSidx - 30) {
					int nxIdx = listI.get(0);

					// sectionPrecededByArticle(text.substring(Math.max(0,
					// curSidx - 400)));
					if (!sectionPrecededByArticle(text.substring(Math.max(0, curSidx - 400)))) {
						String[] ary = { curSidx + "", (curSidx + nxIdx) + "", "sec", "" };
						listValidated.add(ary);
//						System.out.println("5x - ary=" + Arrays.toString(ary));
						// pw.append("\r\n1o .add=" + Arrays.toString(ary));

					}

					else {
						// ADD AS WELL THAT CURSEC # IS GREATER THAN PRIOR AND
						// INT DIF B/W PRIOR SEC AND CUR = ARTDIF OR INT DIF B/W
						// CUR AND PRIOR SEC NMB +9 OR +90 IS EQUAL TO ARTDIF
//						System.out.println("6x curSec=" + curSec + " priorSec=" + priorSec + " nextSec=" + nextSec);
						String[] ary = { curSidx + "", (curSidx + nxIdx) + "", "sec", curSecHdg };
//						System.out.println("6x -but not added?? - ary=" + Arrays.toString(ary));
						// pw.append("\r\n1p .add=" + Arrays.toString(ary));

					}

					if (nxIdx > curSidx) {
						curSidx = nxIdx;
					}

				}
				if ((int) curSec - (int) priorSec == 1 || (int) nextSec - (int) curSec == 1) {
					String[] ary = { curSidx + "", nextSidx + "", "sec", curSecHdg };
					listValidated.add(ary);
//					System.out.println("7x - ary=" + Arrays.toString(ary) + " curSec=" + curSec + " priorSec="
//							+ priorSec + " nextSec=" + nextSec);

				} else {
					String[] ary = { curSidx + "", nextSidx + "", "sec", "" };
					listValidated.add(ary);
//					System.out.println("8x - ary=" + Arrays.toString(ary) + " curSec=" + curSec + " priorSec="
//							+ priorSec + " nextSec=" + nextSec);

				}

				// pw.append("\r\n1q .add=" + Arrays.toString(ary));

			}

			if (foundTwoSecDif && !isItTwoSecDif && (((nextSec - curSec) > .0095 && (nextSec - curSec) < .0105)
					|| ((curSec - priorSec) > .0095 && (curSec - priorSec) < .0105))) {
				// if isTwoSecDef || foundTwoSecDif isConsecutiv=true
				String[] ary = { curSidx + "", nextSidx + "", "sec", curSecHdg };
				listValidated.add(ary);
//				System.out.println("9x - ary=" + Arrays.toString(ary));

				// pw.append("\r\n1r .add=" + Arrays.toString(ary));

			}
			if (isItTwoSecDif) {
				String[] ary = { curSidx + "", nextSidx + "", "sec", curSecHdg };
				listValidated.add(ary);
//				System.out.println("10x - ary=" + Arrays.toString(ary));

				// pw.append("\r\n1s .add=" + Arrays.toString(ary));
			}

			if (isItConsecutiveInSameArticle && !isItTwoSecDif) {

				// if isIttwoSecDif

				/*
				 * if it is consecutive with prior sec (isConsecutive) but it can't be
				 * forecasted. Such as prior 1.04 to 1.05 but there is no 1.05 b/c it goes 1.04
				 * to 2.01 then - then b/w 1.04 and 2.01 there should be no 1.05 and if so
				 * lastSection method not implemented and I go to 2.01. So if listI>0 to
				 * implement lastSec. Or if listI==0 (next sec not found) but next sec article
				 * is not consecutive than use last section - such as sec 12.13 and forecasted
				 * is 12.14 - which doesn't exist (listI.size=0) and next sec art is 2 - then
				 * use last sect method - this can happen when an exhibit for exmaple also has
				 * sec heading which start at 1.01 and main K section ends at 12.13
				 */

				// sidx,edix,type,secHdg
				// is forecastedNextSection between cur idx and next?
				List<Integer> listI = new ArrayList<>();
				// System.out.println("text.len=" + text.length() + " curSidx=" + curSidx + "
				// nextSidx=" + nextSidx);

				if (text.length() > nextSidx && text.length() > curSidx && nextSidx > curSidx) {
					listI = nlp.getAllIndexStartLocations(text.substring(curSidx, nextSidx),
							Pattern.compile("(Section|SECTION) " + forecastedNextSection + "[\\. ]{1}"));
				}

				else if (i + 1 != list.size())
					continue;

				// //NLP.pwNLP.append(NLP.println("is consecutive.
				// Math.Round(forecastedNextSection)=",
				// (Math.round(forecastedNextSection * 100000.0000) / 100000.0000) + ""));

				if (i + 1 < list.size()) {

					if (secDif != 0 && artDif != 0) {
						isConsecutiveArticle = isItConsecutiveArticle(curSecNmb, nextSecNmb);
					}

					lastSection = "";
					// if order is from 1.03 to 1.04 to 2.01 than 1.05 does not
					// exist and @1.04 listI==0 and isConsecutiveArticle
					// if last sec is 12.13 but K exhibit after there's 2.01 -
					// than listI==0 && !isConsecutiveArticle.

					// if skipped a section - this will find it or if last
					// section - this will find it.

					// //NLP.pwNLP.append(NLP.println("forecastedNextSectionFound=",
					// forecastedNextSectionFound + " forecastedNextSectionInNextArticleFound="
					// + forecastedNextSectionInNextArticleFound + " isConsecutiveArticle="
					// + isConsecutiveArticle + " listI.size=" + listI.size()));

					/*
					 * here if it is consecutive sec number but next section after it can't be
					 * forecasted However that takes place w/n if clause requirement that cur sec is
					 * consecutive with prior. And b/c it is consecutive that will act as
					 * verification enough in cases where I don't have artDif or secDif (b/c I
					 * record sIdx and eIdx of section text I continue past blank headings ==>
					 */

					if (!forecastedNextSectionFound && forecastedSectionFound && listI.size() == 0 && secDif != 0) {

						if (curSec - priorSec > 9.9 && curSec - priorSec < 10.1 && curSec > 999 && curSec < 9999
								&& nextSec - curSec > 600 && nextSec - curSec < 1010) {

							gotIt = true;

							String[] ary = { curSidx + "", nextSidx + "", "sec", curSecHdg };
							listValidated.add(ary);
//							System.out.println("11x - ary=" + Arrays.toString(ary));

							// pw.append("\r\n1t .add=" + Arrays.toString(ary));
							// NLP.pwNLP.append(NLP.println("gotIt 1= validate method, forecasted sec
							// list=",
							// Arrays.toString(ary)));

						}

						if (!gotIt && curSec - priorSec > .99 && curSec - priorSec < 1.01 && curSec > 99 && curSec < 999
								&& nextSec - curSec > 60 && nextSec - curSec < 101) {

							String[] ary = { curSidx + "", nextSidx + "", "sec", curSecHdg };
							listValidated.add(ary);
//							System.out.println("12x - ary=" + Arrays.toString(ary));
							// pw.append("\r\n1u .add=" + Arrays.toString(ary));
							// NLP.pwNLP.append(NLP.println("gotIt 2= validate method, forecasted sec
							// list=",
							// Arrays.toString(ary)));
						}

						if (!gotIt && curSec - priorSec > .09 && curSec - priorSec < 0.11 && curSec > .99
								&& nextSec - curSec > .60 && nextSec - curSec < 1.01) {

							String[] ary = { curSidx + "", nextSidx + "", "sec", curSecHdg };
							listValidated.add(ary);
//							System.out.println("13x - ary=" + Arrays.toString(ary));
							// pw.append("\r\n1v .add=" + Arrays.toString(ary));
							// NLP.pwNLP.append(NLP.println("gotIt 3= validate method, forecasted sec
							// list=",
							// Arrays.toString(ary)));
						}

						/*
						 * if forecasted next sec not found b/c it does not exist - i.e., cur is 113 and
						 * forecasted does not match - b/c it is 114 and actual next sec is 201. And
						 * when I search for 114 it is not found (listI=0) in text and next sec less cur
						 * sec (201 less 114) is greater than 60 - than it is valid where cur sec >99.
						 * Make subrules for other val difs (e.g., 1.19 v 2.01)
						 */

					}

					if (!gotIt && !forecastedNextSectionInNextArticleFound && !forecastedSectionFound
							&& !forecastedNextSectionFound && listI.size() == 0 && !isConsecutiveArticle
							&& (artDif != 0 || secDif != 0)) {

						// forecasted not found. So find end of sec or closing
						// para.

						lastSection = ContractParser.getLastDefinition(text, curSidx, curSecHdg, "sec", i, list.size());

						nIdx = 0;
						if (curSidx + lastSection.length() < nextSidx && !foundTwoSecDif) {
							nIdx = curSidx + lastSection.length();

							String[] ary = { curSidx + "", nIdx + "", "sec", curSecHdg };
							listValidated.add(ary);
//							System.out.println("14x - ary=" + Arrays.toString(ary));
							// pw.append("\r\n1w .add=" + Arrays.toString(ary));
							String[] ary2 = { nIdx + "", nextSidx + "", "sec", "" };
							listValidated.add(ary2);
//							System.out.println("15x - ary=" + Arrays.toString(ary2));
							// pw.append("\r\n1x .add=" + Arrays.toString(ary2));
							// NLP.pwNLP.append(NLP.println("5 validate method, adding blank ary to
							// validated sec list=",
							// Arrays.toString(ary2) + " list.size=" + list.size() + " i=" + i));

							if (i + 1 < list.size()) {
								// NLP.pwNLP.append(NLP.println("what is next ary=",
								// Arrays.toString(list.get(i + 1))));
								// List<Integer> listCurNextArt =
								// getCurrentAndNextArticle(
								// curSecNmb, nextSecNmb);
								// if nextArt is less than curArt and not many
								// sec hdgs left in list - than likely remaining
								// sec hdgs are garbage. I set foundEnd to true
								// - and add blank hdg equal to next cur hdg to
								// end of text (text.len)
								if (list.size() - 1 < 9
										&& Double.parseDouble(nextSecNmb) < Double.parseDouble(curSecNmb))
									// (1)==nextArt, (0)=curArt.
									// if (listCurNextArt.get(0) >
									// listCurNextArt
									// .get(1) && list.size() - i < 9) {
									foundEnd = true;
								// }
							}
						}
					}

					else {

						// //NLP.pwNLP.append(NLP.println("!gotIt=", gotIt + "
						// !forecastedNextSectionInNextArticleFound="
						// + forecastedNextSectionInNextArticleFound + " !forecastedSectionFound="
						// + forecastedSectionFound + " !forecastedNextSectionFound=" +
						// forecastedNextSectionFound
						// + " listI.size()=0. " + listI.size() + " !isConsecutiveArticle=" +
						// isConsecutiveArticle
						// + "artDif != 0. " + artDif + " secDif != 0. " + secDif));

						if (listI.size() > 0 && !forecastedNextSectionFound) {
							skip6b = true;

							// if forecastedNextSection=false -- but it is found
							// here - then I need to end cur eIdx at start of
							// next start idx. I also need to make sure the next
							// section after this is forecasted else use last
							// section technique.
							List<String[]> tmplistI = nlp.getAllStartIdxLocsAndMatchedGroups(
									text.substring(curSidx, nextSidx),
									Pattern.compile("(Section|SECTION) " + forecastedNextSection + "[\\. ]{1}"));
							forecastedNextSectionFound = true;
							String tmpSecHdg = text.substring(Integer.parseInt(tmplistI.get(0)[0]) + curSidx);
							tmpSecHdg = tmpSecHdg.substring(0, tmpSecHdg.indexOf("\r"));

							// //NLP.pwNLP.append(
							// NLP.println("tmplistI.get(0)[1]=", tmplistI.get(0)[1] + " tmpTxt=" +
							// tmpSecHdg));

							// //NLP.pwNLP.append(NLP.println("text.substring(curSidx, nextSidx)=",
							// text.substring(curSidx, nextSidx)));

							int tmpEidx = (Integer.parseInt(tmplistI.get(0)[0]) + curSidx);
							// if it went from 12.12 to 12.14 - but now I've
							// found 12.13 - this gets from 12.12 to 12.13
							String[] ary = { curSidx + "", tmpEidx + "", "sec", curSecHdg };
							listValidated.add(ary);
//							System.out.println("16x - ary=" + Arrays.toString(ary));
							// pw.append("\r\n1y .add=" + Arrays.toString(ary));
							// //NLP.pwNLP.append(
							// NLP.println("6a validate method, forecasted sec list=",
							// Arrays.toString(ary)));

							// if found 12.13 then is 12.13 plus secDif equal to
							// 12.14
							String tmpSecNmb = getSectionNumber(tmplistI.get(0)[1]).trim();
							if (Double.parseDouble(tmpSecNmb) + secDif > .999 * nextSec
									&& Double.parseDouble(tmpSecNmb) + secDif < 1.001 * nextSec) {
								String[] ary2 = { tmpEidx + "", nextSidx + "", "sec", tmpSecHdg };
								listValidated.add(ary2);
//								System.out.println("16xb - ary=" + Arrays.toString(ary2));

								// pw.append("\r\n1z .add=" + Arrays.toString(ary2));
							}

							// if not then use lastSect method
							else {

								lastSection = ContractParser.getLastDefinition(text, tmpEidx, curSecHdg, "sec", i,
										list.size());
								String[] ary2 = { tmpEidx + "", (tmpEidx + lastSection.length() + ""), "sec",
										tmpSecHdg };
								listValidated.add(ary2);
//								System.out.println("17x - ary=" + Arrays.toString(ary2));
								// pw.append("\r\n1aa .add=" + Arrays.toString(ary2));
							}
						}

						if (!skip6b) {
							if (forecastedNextSectionFound || forecastedNextSectionInNextArticleFound) {
								String[] ary = { curSidx + "", nextSidx + "", "sec", curSecHdg };
								listValidated.add(ary);
//								System.out.println("18x - ary=" + Arrays.toString(ary));
								// pw.append("\r\n1bb .add=" + Arrays.toString(ary));
								// NLP.pwNLP.append(
								// NLP.println("6b validate method, forecasted sec list=",
								// Arrays.toString(ary)));
							}
							// if not forecasted than use last section method
							else {
								lastSection = ContractParser.getLastDefinition(text, curSidx, curSecHdg, "sec", i,
										list.size());
								String[] ary = { curSidx + "", (curSidx + lastSection.length() + ""), "sec",
										curSecHdg };
								listValidated.add(ary);
//								System.out.println("19x - ary=" + Arrays.toString(ary));
								// pw.append("\r\n1cc .add=" + Arrays.toString(ary));
							}
						}
					}

					/*
					 * String[] ary = { curSidx + "", nIdx + "", "sec", curSecHdg };
					 * listValidated.add(ary);
					 * 
					 * //NLP.pwNLP.append(NLP.println( "6b validate method, =",
					 * Arrays.toString(ary)));
					 */

				}

				if (i + 1 == list.size()) {

					lastSection = ContractParser.getLastDefinition(text, curSidx, curSecHdg, "sec", i, list.size());

					String[] ary = { curSidx + "", (curSidx + lastSection.length() + ""), "sec", curSecHdg };
					listValidated.add(ary);
//					System.out.println("20x - ary=" + Arrays.toString(ary));
					// pw.append("\r\n1dd .add=" + Arrays.toString(ary));

					// //NLP.pwNLP.append(NLP.println("7 validate method, not? sec hdg=",
					// Arrays.toString(ary)));

				}
			}

			priorSecHdg = curSecHdg;
			priorTwoBefSecNmb = priorSecNmb;
			priorSecNmb = curSecNmb;
			priorSidx = curSidx;

		}

		// NLP.printListOfStringArray("listValidated secHdg=", listValidated);
		// NLP.printListOfStringArray("sec hdgs - listSkipped", listSkipped);

		// pw.close();
		return listValidated;

	}

	public static double roundDoubleValueToNumberOfDecimalPlace(double number, double rounded) {

		return number = ((double) (int) ((number + 9.0 / (rounded * 10) + secDif) * rounded)) / rounded;

	}

	public static double getNumberOfDecimalsPlaceToRoundTo(String numberStr) {

		double round = 1.0;

		if (numberStr.replaceAll("[ \\d]+\\.", "").trim().length() == 0 || !numberStr.contains("\\."))
			return round;

		String[] numbSplit = numberStr.split("\\.");
		// System.out.println("numberStr="+numberStr);
		if (numbSplit.length < 1)
			return round;

		Integer decm = numbSplit[1].trim().length();
		if (null == decm || decm == 0 || decm < 1)
			return round;

		// System.out.println("numbSplit[1].len="+decm);

		for (int i = 0; i < decm; i++) {
			round = round * 10;
		}

		return round;

	}

	public static List<String[]> checkList(List<String[]> list) {
		List<String[]> listIdxAndSection = new ArrayList<>();
		@SuppressWarnings("unused")
		int idx = 0;
		String idxStr = "", sectionHdg, sectionName, sectSubStr;
		for (int i = 0; i < list.size(); i++) {
			idxStr = list.get(i)[0];
			idx = Integer.parseInt(idxStr);
			// gets rid of "Section #" and gets just sec name.
			sectionHdg = list.get(i)[1];
			sectionName = sectionHdg.trim().replaceAll("^S(?i)ection.*?[\\da-zA-Z].*?(?=[a-zA-Z\\(])", "")
					.replaceAll("[ ]{2,}", " ").trim();
			sectSubStr = sectionName.replaceAll(" ", "");

			if (sectSubStr.length() < 1) {
				System.out.println("sectSubStr.len<1=" + sectionHdg.replaceAll("[\r\n]", " ").replaceAll("[ ]+", " "));
				continue;
			}

			// if 1st ltr of a word 6 characters in length or more is not
			// initial caps- skip
			boolean isInitialCaps = isItInitialCaps(sectSubStr, sectionName);
			if (!isInitialCaps) {
//				System.out.println("!initialCaps. continue=" + sectionName);
				continue;
			}

			String[] ary = { idxStr, sectionHdg.replaceAll("[\r\n]", " ").replaceAll("[ ]+", " ") };

			// System.out.println("checkList hdg ary="+Arrays.toString(ary));

			listIdxAndSection.add(ary);
		}

		return listIdxAndSection;

	}

	public static List<String[]> formatSecHdgsList(List<String[]> list, String text) throws IOException {

		NLP nlp = new NLP();

		// create method to check to see if current section is greater than next
		// section - such as end of K is section 14.14 and exhibit has section
		// 1.1. 14.14 is last section and I can then run getLastSection.

		List<String[]> listFormatted = new ArrayList<String[]>();
		String secHdg = "", sIdx = "", lastSection = "", curSecStr = "", nextSecStr = "";
		double curSec = 0.0, nextSec = 0.0;

		int idx = 0, idxFnl = 0, idxTmp = 0;
		for (int i = 0; i < list.size(); i++) {

//			System.out.println("formatSecHdgsList - list="+Arrays.toString(list.get(i)));

			secHdg = list.get(i)[3];
			if (i + 1 < list.size()) {
				curSecStr = getSectionNumber(secHdg);
				nextSecStr = getSectionNumber(list.get(i + 1)[3]);

				if (nextSecStr.replaceAll("[\t \\.\r\n]", "").length() == 0) {
					nextSec = 0.0;
				} else {
					// 1.9 is greater than 1.10 but 19 <110 (where numbering is
					// 1.1,1.2...1.9,1.10
					nextSec = Double.parseDouble(nextSecStr.replaceAll("\\.", ""));
				}

				if (curSecStr.replaceAll("[\t \\.\r\n]", "").length() == 0) {
					curSec = 0.0;
				} else {
					curSec = Double.parseDouble(curSecStr.replaceAll("\\.", ""));
				}

			}

			if (i + 1 < list.size() && list.get(i)[3].replaceAll("[\t \\.\r\n]", "").length() > 1
					&& list.get(i + 1)[3].replaceAll("[\t \\.\r\n]", "").length() > 1 && curSec < nextSec) {
				/*
				 * [3].len is actual sec hdg - and if it or sec hdg that follows has no sec hdg
				 * (if sec hdg is not consecutive it is blank) - than don't use sIdx of next sec
				 * hdg to end current - and instead go to else clause which uses last def.
				 */

				listFormatted.add(list.get(i));
			}

			else {

				sIdx = list.get(i)[0];
				idx = Integer.parseInt(sIdx);

				lastSection = ContractParser.getLastDefinition(text, idx, secHdg, "sec", i, list.size());
				// //NLP.pwNLP.append(NLP.println("sec lastSection.len=" + lastSection.length(),
				// ""));
				// see if in lastSection there is a sig page.

				idxTmp = lastSection.length();

				List<String[]> listEndOfK = nlp.getAllStartIdxLocsAndMatchedGroups(lastSection,
						PatternsDif.patternEndOfContract2);// Can't use end of contract patterns that has long regex
															// distance to check - eg 1,350

				if (listEndOfK.size() > 0) {
					// if >0 than it found endOfK before end of lastSection txt
					// - so it is earlier end point and idx to use.

					idxTmp = Integer.parseInt(listEndOfK.get(0)[0]);
					// //NLP.pwNLP.append(NLP.println("listEndOfK=" +
					// Arrays.toString(listEndOfK.get(0)), ""));
				}

				idxFnl = Integer.parseInt(sIdx) + (idxTmp);
				String[] ary = { sIdx, idxFnl + "", list.get(i)[2], list.get(i)[3] };
				listFormatted.add(ary);
				// //NLP.pwNLP.append(NLP.println("formatting sec hdg. adding lastSection ary="
				// + Arrays.toString(ary), ""));
			}
		}

		return listFormatted;
	}

	public static boolean isItInitialCaps(String sectSubStr, String sectionName) {

		if (!Character.isUpperCase(sectSubStr.charAt(0))) {
			// //NLP.pwNLP.append(NLP.println("not getting this ssectionName=",
			// sectionName));
			return false;
			// if sec name has word w/ 6 chars & its not initial caps: skip
			// get just sectionName ==>
		}

		String[] sectionNameWords = StopWords.removeStopWords(sectionName).split(" ");
		boolean isCap = true;

		// if there's a word that is not initial cap and greater than 5
		// characters than skip
		int cnt = 0;
		for (int c = 0; c < sectionNameWords.length; c++) {
			if (sectionNameWords[c].replaceAll("between|without", "").length() > 5
					&& !Character.isUpperCase(sectionNameWords[c].charAt(0))) {
				cnt++;
				if (cnt > 1) {
					isCap = false;
				}
				if (isCap)
					break;
				// //NLP.pwNLP.append(NLP.println("isCap=", isCap + ""));
			}
		}

		return isCap;
	}

	public static boolean isItInitialCapsAgreementId(String potentialAgreementName) throws IOException {

		if (potentialAgreementName.trim().length() < 3) {
			return false;
		}
		if (!Character.isUpperCase(potentialAgreementName.charAt(0))) {
			// //NLP.pwNLP.append(NLP.println("not getting this ssectionName=",
			// sectionName));
			System.out.println("1");
			return false;
			// if sec name has word w/ 6 chars & its not initial caps: skip
			// get just sectionName ==>
		}

		String[] potentialAgreementNameWords = potentialAgreementName.split(" ");
		boolean isCap = true;

		// if there's a word that is not initial cap and greater than 5
		// characters than skip
		for (int c = 0; c < potentialAgreementNameWords.length; c++) {
			if (potentialAgreementNameWords[c].trim().equals("and"))
				continue;
			if (NLP.getAllMatchedGroupsAndStartIdxLocs(potentialAgreementNameWords[c], Pattern.compile("[a-zA-Z]{1}"))
					.size() > 0 && !Character.isUpperCase(potentialAgreementNameWords[c].charAt(0))
					&& potentialAgreementNameWords[c].replaceAll("[\\d]+", "").trim().length() > 0) {
				isCap = false;
//				System.out
//						.println("2 false?=" + c + " potentialAgreementNameWords[c]=" + potentialAgreementNameWords[c]);
				// //NLP.pwNLP.append(NLP.println("isCap=", isCap + ""));
				break;
			}
		}

		return isCap;
	}

	public static List<String[]> getRidOfTocSecHdgs(List<String[]> list, String text) {

		// NLP.printListOfStringArray("at getRidOfTocSecHdgs==", list);

		NLP nlp = new NLP();

		List<String[]> listWithOutTocSecHdg = new ArrayList<String[]>();
		Pattern patternTocPgNmbSimple = Pattern
				.compile("(?sm)[ ]{5,}([ABCDEFG]{1}-?)?[\\d]{1,3}[ ]{0,5}([\r\n]{1,}[ ]{0,5})|$");

		@SuppressWarnings("unused")
		int idx = 0, fiveAftIdx = 0, sixBefIdx = 0, fiveBefIdx = 0, twoAftIdx = 0, twoBefIdx = 0, dif5Aft = 0,
				dif1Aft = 0, dif2Aft = 0, dif2Bef = 0, dif5Bef = 0, dif6Bef = 0;
		double initCapsCnt = 0.0, lowerCaseCnt = 0.0;
		String secNmbStr = "", priorSecNmbStr = "", secShortened = "", secHeadingOnly = "";
		boolean isItToc = false;
		// if not at least 10 where no TOC found

		@SuppressWarnings("unused")
		int cntSecHdgNotToc = 0;
		if (list.size() > 9) {
			for (int i = 0; i < list.size(); i++) {
				initCapsCnt = 10.0;
				lowerCaseCnt = 1.0;
				// tests determines if isItToc true so reset to false.
				isItToc = false;
				idx = Integer.parseInt(list.get(i)[0]);

				twoAftIdx = 0;
				fiveAftIdx = 0;
				dif5Aft = 0;
				dif1Aft = 0;
				dif2Aft = 0;

				twoBefIdx = 0;
				fiveBefIdx = 0;
				sixBefIdx = 0;
				dif2Bef = 0;
				dif5Bef = 0;
				dif6Bef = 0;

				if (i + 5 < list.size()) {
					fiveAftIdx = Integer.parseInt(list.get(i + 5)[0]);
					dif5Aft = fiveAftIdx - idx;
				}

				if (i + 2 < list.size()) {
					twoAftIdx = Integer.parseInt(list.get(i + 2)[0]);
					dif2Aft = twoAftIdx - idx;
				}

				if (i > 1) {
					twoBefIdx = Integer.parseInt(list.get(i - 2)[0]);
					dif2Bef = idx - twoBefIdx;
				}

				/*
				 * can't use fiveAftIdx going forward because when you get near end it will
				 * reach fwd to true sec hdgs and create a f/p b/c 5 aft will be far away while
				 * 2 or 3 or 4 etd will be close but now all are deemed not toc. By using only 5
				 * behind after the 5th sec hdg - this won't happen b/c it is a look back.
				 */

				if (i > 4) {
					fiveBefIdx = Integer.parseInt(list.get(i - 5)[0]);
					dif5Bef = idx - fiveBefIdx;
					// fiveAftIdx =0;
				}

				if (i > 5) {
					sixBefIdx = Integer.parseInt(list.get(i - 6)[0]);
					dif6Bef = idx - sixBefIdx;
					// fiveAftIdx =0;
				}

				if (i + 1 < list.size()) {
					dif1Aft = Integer.parseInt(list.get(i + 1)[0]) - idx;
				}

				// dif start is zero?
				if (dif6Bef < 1400 && dif5Aft < 1400) {
					listTocHdgs.add(list.get(i));
					// NLP.pwNLP.append(NLP.println("1a not sec hdg=",
					// Arrays.toString(list.get(i))));
					isItToc = true;
					continue;
				}

				/*
				 * if not TOC hdg than 5aft and 5behing should be at least 1200 - see above. If
				 * 5behind<1200 and 1aft is large - see if new art is 1 - if yes. than it is not
				 * toc.
				 */

				if (dif5Bef < 1200 && dif1Aft > 800) {
					secNmbStr = getSectionNumber(list.get(i)[1]);
					if (i > 0) {
						priorSecNmbStr = getSectionNumber(list.get(i - 1)[1]);
					}
					// List<Integer> listArt =
					// getCurrentAndNextArticle(priorSecNmbStr, secNmbStr);
					// NLP.pwNLP.append(NLP.println("dif5B<1200 && dif1Ah>800. secNmbStr=",
					// secNmbStr + " priorSecNmbStr=" + priorSecNmbStr
					// + " listArt.size=" + listArt.size()
					// ));

					if (i > 0 && priorSecNmbStr.replaceAll("[^\\d]", "").length() > 0
							&& secNmbStr.replaceAll("[^\\d]", "").length() > 0
							&& Double.parseDouble(priorSecNmbStr) > Double.parseDouble(secNmbStr)
							&& secNmbStr.substring(0, 1).equals("1"))

					// listArt.size()>1 && listArt.get(1)==1)
					{
						// NLP.pwNLP.append(NLP.println("this is new art 1. section=", list.get(i)[1]));
						isItToc = false;
					}
				}

				// if it has a page # at end of line than it is likely a toc sec
				// hdg, IF it also occurs 2 or more times in a snippet prior -
				// and the next toc sec hdg is relatively close -- if it is very
				// far - than it is likely sect 1.01 def which is a very long
				// sect.

				if (twoBefIdx > 0 && idx - twoBefIdx < 700
						&& nlp.getAllIndexEndLocations(text.substring(twoBefIdx, idx), patternTocPgNmbSimple).size() > 2
						&& dif5Bef < 1100 && dif1Aft < 5000) {
					isItToc = true;
					// NLP.pwNLP.append(NLP.println("2 not sec hdg=",
					// Arrays.toString(list.get(i))));
					// && dif5Bef<1500

					continue;
				}

				if (twoAftIdx > 0 && twoAftIdx - idx < 700 && dif5Bef < 1500 && nlp
						.getAllIndexEndLocations(text.substring(idx, twoAftIdx), patternTocPgNmbSimple).size() > 2) {
					isItToc = true;
					// NLP.pwNLP.append(NLP.println("3 not sec hdg=",
					// Arrays.toString(list.get(i))));
					continue;
				}

				if (isItToc) {
					// NLP.pwNLP.append(NLP.println("4 not sec hdg=",
					// Arrays.toString(list.get(i))));
					continue;
				}

				secShortened = list.get(i)[1];
				secHeadingOnly = list.get(i)[1]
						.replaceAll("(?i)section[ \t\r\n]{1,8}[\\d]{1,3}[\\.]{0,1}[\\d]{0,2}([\\. ])?", "").trim();

				if (nlp.getAllIndexEndLocations(secHeadingOnly, Pattern.compile(" [a-z]")).size() > 10
						&& secHeadingOnly.split("  ").length > 0) {
					// cuts a long sec hdg that has a full sentence plus sec
					// hdg. see for example:
					// Section 2.09 Mutilated, Destroyed, Lost and Stolen Notes
					// If any mutilated Note is surrendered to the Trustee, the
					// Company shall execute and the Trustee shall authenticate
					// and deliver in exchange therefor a new Note of like tenor
					// and principal amount and bearing a number not
					// contemporaneously outstanding.

					secShortened = list.get(i)[1].split("(?<=[a-z]{2})  (?=[A-Z]{1})")[0];

				}

				lowerCaseCnt = nlp.getAllIndexEndLocations(secShortened, Pattern.compile(" [a-z]")).size();
				initCapsCnt = nlp.getAllIndexEndLocations(secShortened, Pattern.compile(" [A-Z]")).size();

				if ((lowerCaseCnt != 0 && initCapsCnt != 0 && lowerCaseCnt / (initCapsCnt + lowerCaseCnt) > .6
						&& lowerCaseCnt > 7) || secShortened.replaceAll("[\\d\\. ]{1,}", "").length() < 1) {
					// if lots of initial lower caps in heading - it is a f/p.
					// NLP.pwNLP.append(
					// NLP.println("is NOT sec hdg B/C too many initial caps =",
					// Arrays.toString(list.get(i))
					// + " lowerCaseCnt=" + lowerCaseCnt + " initCapsCnt=" + initCapsCnt));
					continue;
				}

				String[] ary = { list.get(i)[0], secShortened.replaceAll("[\r\n]+", " ").replaceAll("[ ]+", " ") };

				listWithOutTocSecHdg.add(ary);
				// NLP.pwNLP.append(NLP.println("is sec hdg=",
				// list.get(i)[0] + " " + secShortened + " fiveAftIdx=" + fiveAftIdx + "
				// fiveBefIdx=" + fiveBefIdx
				// + " dif5Bef=" + dif5Bef + " dif5Aft=" + dif5Aft)
				// + " confirmed this many=" + cntSecHdgNotToc);

				cntSecHdgNotToc++;

			}
		}

		// nlp.printListOfStringArray("listWithOutTocSecHdg=", listWithOutTocSecHdg);
		return listWithOutTocSecHdg;

	}

	public static List<String[]> pickSecHdgList(List<String[]> listRestrictive, List<String[]> listLessRestrictive,
			String text) throws IOException {
		// list. [0]=sIdx,[1]just sec#,[2]=section

		// Looping through more restrictive list - if sec hdg is not consecutive
		// - than got to sIdx of less restrictive list of prior sIdx of
		// restrictive list and get next hdg of less restrictive list - and see
		// if that now causes it to be consecutive

		boolean isConsecutiveSec = false, isConsecutiveArt = false;
		String secNmbStr = "", priorSecNmbStr = "", nextSecNmbStr = "", twoAheadSecNmbStr = "", secHdg = "";
		int skipCnt = 0;
		for (int i = 0; i < listRestrictive.size(); i++) {
			isConsecutiveSec = false;
			secHdg = listRestrictive.get(i)[1];
			secNmbStr = getSectionNumber(secHdg);
			if (i + 1 < listRestrictive.size()) {
				nextSecNmbStr = getSectionNumber(listRestrictive.get(i + 1)[1]);
			}

			if (i + 2 < listRestrictive.size()) {
				twoAheadSecNmbStr = getSectionNumber(listRestrictive.get(i + 2)[1]);
			}

			// NLP.pwNLP.append(NLP.println("3 calling is isItConsecutiveInSameArticle",
			// ""));

			isConsecutiveSec = isItConsecutiveInSameArticle(secNmbStr, nextSecNmbStr, priorSecNmbStr, i);
			// //NLP.pwNLP.append(NLP.println("1 isItConsecutiveInSameArticle="
			// + isConsecutiveSec + " secNmbStr=" + secNmbStr
			// + " nextSecNmbStr=" + nextSecNmbStr + " priorSecNmbStr="
			// + priorSecNmbStr, ""));
			// NLP.pwNLP.append(NLP.println(" pickSecHdgList", "calling isConsecutiveArt"));
			isConsecutiveArt = isItConsecutiveNewArticle(secNmbStr, nextSecNmbStr, priorSecNmbStr, twoAheadSecNmbStr,
					i);
			// //NLP.pwNLP.append(NLP.println("5 same Art isConsecutive",
			// isConsecutiveArt + ""));

			priorSecNmbStr = secNmbStr;
			if (!isConsecutiveArt && !isConsecutiveSec) {
				skipCnt++;

				// //NLP.pwNLP.append(NLP.println(
				// "not consecutive so this sec being skipped. secNmbStr="
				// + secNmbStr + " nextSecNmbStr=" + nextSecNmbStr
				// + " priorSecNmbStr=" + priorSecNmbStr,
				// "\risConsecutiveArt (same art)=" + isConsecutiveArt
				// + " isConsecutiveNewArt=" + isConsecutiveSec));

			}
		}

		// NLP.pwNLP.append(NLP.println("listLessRestr.size=" +
		// listLessRestrictive.size(), ""));
		// NLP.pwNLP.append(NLP.println("listMoreRestr.size=" + listRestrictive.size(),
		// ""));
		// NLP.pwNLP.append(NLP.println("skipCnt=" + skipCnt, ""));

		// use less restrictive list when it is less than restrictive liss plus
		// sections skipped (what true size ought to be) and its less
		// restrictive list is larger list.
		int extra = 4;
		if (((listLessRestrictive.size() - extra) < (listRestrictive.size() + skipCnt))
				&& listLessRestrictive.size() > listRestrictive.size()) {
			// NLP.printListOfStringArrayInReverse("picked less restrictive list=",
			// listLessRestrictive);
			return listLessRestrictive;
		}

		// NLP.printListOfStringArray("picked restrictive list=", listRestrictive);

		return listRestrictive;
	}

	public static double forecastedSectionBasedOnPrior(String priorSecNmbStr) {

		double secNmb = 0.0;

		// THIS COULD BE MUCH SIMPLER - priorSecNmb+secDif! - maybe do so and
		// change this method to forecastedSectionSameArticleBasedOnPriorSec

		// then method is simple as priorSecNmb+secDif! Then use this when I go
		// from 4.04 to 4.06 b/c I can't find 4.05. This return 4.05 -- and then
		// searches it in text b/w end of 4.04 and start of 4.06.

		// also use more simply to say prior sec is 3.01 and cur is 3.02 - confirmed!

		return secNmb;
	}

	public static boolean isItConsecutiveInSameArticle(String secNmbStr, String nextSecNmbStr, String priorSecNmbStr,
			int i) {

		forecastedSection = 0.0;
		forecastedNextSection = 0.0;
		forecastedSectionFound = false;
		forecastedNextSectionFound = false;
		boolean isConsecutive = false;
		// if value has decimal than take whatever is to left of decimal and
		// subtract - it should equal 1. replace any 0 values that start a
		// string
		@SuppressWarnings("unused")
		Integer curSec = -1, nextSec = -1, priorSec = -1, curSecNoOfDecPlaces = -1, nextSecNoOfDecPlaces = -1,
				priorSecNoOfDecPlaces = -1;
		@SuppressWarnings("unused")
		String art = "";

		// if (secDif != 0.0) {
		forecastedNextSection = Math.round(getForecastedSection(secNmbStr) * 100000.0000) / 100000.0000;
		if (nextSecNmbStr.length() > 0 && forecastedNextSection == Double.parseDouble(nextSecNmbStr)) {
			forecastedNextSectionFound = true;
			isConsecutive = true;

			// NLP.pwNLP
			// .append(NLP.println(
			// "1a isItConsecutiveInSameArticle - forecastedNextSection=" +
			// forecastedNextSection
			// + " nextSec=" + nextSecNmbStr,
			// " secNmbStr=" + secNmbStr + " isConsec?=" + isConsecutive));
		}

		forecastedSection = Math.round(getForecastedSection(priorSecNmbStr) * 100000.0000) / 100000.0000;

		// NLP.pwNLP.append(NLP.println(
		// "1c using prior sec - forecastedSection=" + forecastedSection + " priorSec="
		// + priorSecNmbStr,
		// " secNmbStr=" + secNmbStr));

		if (secNmbStr.length() > 0 && forecastedSection == Double.parseDouble(secNmbStr)) {

			forecastedSectionFound = true;
			isConsecutive = true;

			// NLP.pwNLP.append(NLP.println("1a isItConsecutiveInSameArticle -
			// forecastedSection=" + forecastedSection
			// + " priorSecNmbStr=" + priorSecNmbStr, " secNmbStr=" + secNmbStr + "
			// isConsec?=" + isConsecutive));
		}

		return isConsecutive;

	}

	public static double getForecastedSection(String section) {

		// NLP.pwNLP.append(NLP.println("getForecasted - articleSection=", section + "
		// secDif=" + secDif));
		// if secDif calculated correctly I will be able to forecast next
		// section. When comparing I just take secNmbStr and convert to double.

		if (secDif != 0.0 && section.length() > 0) {
			forecastedSection = Math.round(Double.parseDouble(section) * 1000000.000) / 1000000.000 + secDif;
			return forecastedSection;
		}
		// if secDif is 0.0 -- I try to get forecastedSection in this manner. Is
		// this fruitfull?
		String forecastedSection = "", decimalPlaces = "", addZeroAtEnd = "", article = "", sec = "";
		Integer noOfDecimalPlaces = null;
		if (section.trim().replaceAll("[\\d]{0,}\\.", "").length() > 0 && section.contains(".")) {

			article = section.split("\\.")[0];
			noOfDecimalPlaces = getDec(section)[1];
			sec = getDec(section)[0] + "";
			// //NLP.pwNLP.append(NLP.println("1 getForecasted - article=" +
			// article
			// + " noOfDecimalPlaces=" + noOfDecimalPlaces, " section="
			// + section));

		}

		if (!section.contains(".") && section.length() > 1) {
			article = section.substring(0, Math.max(1, section.length() - 2));
			sec = section.substring(article.length(), section.length() - 1);
			// //NLP.pwNLP.append(NLP.println("2 getForecasted - article=" +
			// article
			// + " noOfDecimalPlaces=" + noOfDecimalPlaces+ " section="
			// + section,""));

		}

		// if it is 9 to 10 - than don't need to add a zero at end. Only time
		// this could come up is when numbering is like 2.080 and I need to go
		// to 2.081
		// if (sec.equals("9") && noOfDecimalPlaces >1)

		// {
		// noOfDecimalPlaces = noOfDecimalPlaces-1;
		// addZeroAtEnd = "0";
		//
		// }

		// adds a zero when value sec# is 2 but it is 5.02.
		if (null != sec && null != noOfDecimalPlaces && sec.length() != noOfDecimalPlaces
				&& noOfDecimalPlaces > sec.length()) {
			if (noOfDecimalPlaces > 0) {
				for (int i = 0; i < (noOfDecimalPlaces - sec.length()); i++) {
					// if section
					decimalPlaces = decimalPlaces + "0";
				}
			}
		}

		if (sec.length() == 0)
			sec = "0";

		forecastedSection = article + "." + decimalPlaces + (Integer.parseInt(sec) + 1) + addZeroAtEnd;
		// //NLP.pwNLP.append(NLP.println("3 getForecasted - forecastedSection="
		// + forecastedSection + " article=" + article
		// + " noOfDecimalPlaces=" + noOfDecimalPlaces,
		// " section="+section+" sec="
		// + sec));

		return Math.round(Double.parseDouble(forecastedSection) * 100000.000) / 100000.000;
	}

	public static boolean isItConsecutiveArticle(String secNmbStr, String nextSecNmbStr) {

		// boolean isItC = false;

		double curArt = -1, nextArt = -1;
		int curArtInt = -1, nextArtInt = -1;
		// ck current section article (9.2 - ck 9) against next section article
		// (10.1).

		// secNmbStr = secNmbStr.trim().replaceAll("\\.$", "");
		// nextSecNmbStr = nextSecNmbStr.trim().replaceAll("\\.$", "");

		// if(secNmbStr.contains(".")){
		if (artDif > 0) {
			curArt = (Double.parseDouble(secNmbStr) / artDif) * artDif;
			curArtInt = (int) ((Double.parseDouble(secNmbStr) / artDif) * artDif);

			// NLP.pwNLP.append(NLP.println("a1a artDif=", artDif + " secNmbStr=" +
			// secNmbStr + " curArt=" + curArt));curArt = Math.round(curArt * 100000.0000) /
			// 100000.0000;
			// NLP.pwNLP.append(NLP.println("a1 artDif=",
			// artDif + " secNmbStr=" + secNmbStr + " curArt=" + curArt + "curArtInt=" +
			// curArtInt));
		}

		if (artDif > 0) {
			nextArt = (Double.parseDouble(nextSecNmbStr) / artDif) * artDif;
			nextArtInt = (int) ((Double.parseDouble(nextSecNmbStr) / artDif) * artDif);
			nextArt = Math.round(nextArt * 100000.0000) / 100000.0000;
		}

		// if curArt+1=nextArt than is consecutive. curArt can be prior and
		// nextArt be current.

		// NLP.pwNLP.append(NLP.println("1ab artDif=",
		// artDif + " secDif=" + secDif + " nextArtInt=" + nextArtInt + " curArtInt=" +
		// curArtInt + " secNmbStr="
		// + secNmbStr + " nextSecNmbStr=" + nextSecNmbStr + " nextArtInt * artDif="
		// + (nextArtInt * artDif) + " curArtInt * artDif + artDif=" + (curArtInt *
		// artDif + artDif)
		// + " nextArtInt * artDif + secDif=" + (nextArtInt * artDif + secDif)));
		// secDif=1.0 1 artDif==100.0 nextArt=2.0 curArt=1.0 secNmbStr=122
		// nextSecNmbStr=201||END

		// NLP.pwNLP.append(NLP.println("1ac curArt=", curArtInt + " nextArtInt=" +
		// nextArt));

		if (nextSecNmbStr.length() > 0 && nextArtInt * artDif == (curArtInt * artDif + artDif)
				&& Double.parseDouble(nextSecNmbStr) == nextArtInt * artDif + secDif && artDif > 0)

		{
			forecastedNextSectionFound = true;
			return true;
		}

		else
			return false;

	}

	public static boolean isItConsecutiveNewArticle(String secNmbStr, String nextSecNmbStr, String priorSecNmbStr,
			String twoAheadSecNmbStr, int i) {

		// check if articles are consecutive (values before decimal 1.2 to 2.1
		// or next 3 sections are consecutive after decimal - 8.1,8.2,8.3)

		// NLP.pwNLP.append(NLP.println(
		// "isItConsecutiveNewArticle secNmbStr=" + secNmbStr + " nextSecNmbStr=" +
		// nextSecNmbStr
		// + " twoAheadSecNmbStr=" + twoAheadSecNmbStr + " priorSecNmbStr=" +
		// priorSecNmbStr + " i=" + i,
		// ""));

		boolean isConsecutive = false;
		// if value has decimal than subtracting values prior to decimals should
		// equal 1. if value has no decimal this should be true for first values
		// or first two values

		double curArt = -1, priorArt = -1, nextArt = -1;
		// String sec ="", dec = "", curArtStr = "";

		// ck current section article (9.2 - ck 9) against next section article
		// (10.1).

		if (secNmbStr.length() > 0 && artDif > 0) {
			curArt = Integer.parseInt(secNmbStr.replaceAll("(?sm)\\.[\\d]{1,}\\.?|\\.$", "").trim()) / artDif;
			// curArt = Math.round(curArt * 100000.0000) / 100000.0000;
		}

		// if priorArt plus 1 is equal to cur art and cur sec is equal to 1 or
		// 10 - it is a match.

		if (nextSecNmbStr.length() > 0 && artDif > 0) {
			nextArt = Integer.parseInt(nextSecNmbStr.replaceAll("\\.[\\d]{1,}\\.?|\\.$", "").trim()) / artDif;
			// nextArt = Math.round(nextArt * 100000.0000) / 100000.0000;
		}

		if (i > 0 && priorSecNmbStr.length() > 0 && artDif > 0) {
			priorArt = Integer.parseInt(priorSecNmbStr.replaceAll("\\.[\\d]{1,}\\.?|\\.$", "").trim()) / artDif;
			// priorArt = Math.round(priorArt * 100000.0000) / 100000.0000;
		}

		if (nextSecNmbStr.length() > 0 && nextArt * artDif == (curArt * artDif + artDif)
				&& nextArt == Math.round((curArt * artDif + artDif + secDif) * 100000.0000) / 100000.0000
				&& artDif > 0) {
			forecastedNextSectionFound = true;
			forecastedNextSectionInNextArticleFound = true;
		}

		if (nextSecNmbStr.length() > 0 && Double.parseDouble(nextSecNmbStr) == nextArt * artDif + secDif
				&& artDif > 0) {
			forecastedNextSectionFound = true;
		}

		// NLP.pwNLP.append(NLP.println("1bc curArt=" + curArt,
		// " nextArt=" + nextArt + " priorArt=" + priorArt + " artDif=" + artDif));

		// if new article i cannot =0.
		// next article must be higher. so don't use abs

		if (i > 0 && artDif + priorArt * artDif == curArt * artDif && artDif > 0) {
			isConsecutive = true;
			isConsecutiveArticle = true;
		}

		if (artDif + curArt * artDif == nextArt * artDif && artDif > 0) {
			isConsecutive = true;
			isConsecutiveArticle = true;
		}

		// NLP.pwNLP.append(NLP.println(
		// "1cc curArt=" + curArt + " priorArt=" + priorArt + " curArt-priorArt=" +
		// (curArt - priorArt),
		// " isConsec?=" + isConsecutive));

		if (isConsecutive)
			return isConsecutive;

		// if still not consecutive see if next is consecutive and also check if
		// next after next is consecutive
		boolean isC = false;
		isConsecutiveArticle = false;

		if (!isConsecutive) {
			// one ahead is consec with cur sec # as is next 2 ahead
			// NLP.pwNLP.append(NLP.println("5 calling is isItConsecutiveInSameArticle",
			// ""));

			isC = isItConsecutiveInSameArticle(secNmbStr, nextSecNmbStr, priorSecNmbStr, i);

			// isItConsecutiveInSameArticle fetch method that sets
			// forecastedNextSectionFound. isItConsecutiveInSameArticle will
			// use 1 prior go see if cur is forecasted. Instead I just need to
			// see if next after cur is forecasted. So I only need to see if
			// next after current is forecasted.
			isC = forecastedNextSectionFound;
			// NLP.pwNLP
			// .append(NLP.println(
			// "a run in new article isItConsecutiveInSameArticle secNmbStr=" + secNmbStr
			// + " nextSecNmbStr=" + nextSecNmbStr + " priorSecNmbStr=" + priorSecNmbStr,
			// " same Art isC?=" + isC));

			// two ahead is consec with next (1 prior) sec#-
			// secNmbStr=nextSecNmbStr,next=twoAhead,prior=secNmbStr
			if (isC) {

				// NLP.pwNLP.append(NLP.println("6 calling is isItConsecutiveInSameArticle",
				// ""));

				isC = isItConsecutiveInSameArticle(nextSecNmbStr, twoAheadSecNmbStr, secNmbStr, i);
				isC = forecastedNextSectionFound;
				// NLP.pwNLP.append(NLP.println("b run in new article -- nextSec#=",
				// nextSecNmbStr + " twoAheadSecNmbStr=" + twoAheadSecNmbStr + " same Art isC?="
				// + isC));
			}
		}

		return isC;
	}

	public static Integer[] getDec(String secNmbStr) {

		Integer secNo = -1, noOfDecPlaces = 0;
		if (secNmbStr.trim().split("\\.").length > 1 && secNmbStr.trim().split("\\.")[1].length() > 0
				&& secNmbStr.trim().split("\\.")[1].replaceAll("[\\d]", "").length() == 0) {

			// //NLP.pwNLP.append(NLP.println("getDec -- secNmbStr.trim().split(\\.)[1]=",
			// secNmbStr.trim().split("\\.")[1]));

			noOfDecPlaces = secNmbStr.trim().split("\\.")[1].length();
			if (secNmbStr.trim().split("\\.")[1].replaceAll("^00?0?", "").replaceAll("[^\\d]", "").length() < 1
					|| secNmbStr.trim().split("\\.")[1].replaceAll("^00?0?", "").length() < 1) {
				secNo = 0;
			} else {
				secNo = Integer.parseInt(secNmbStr.trim().split("\\.")[1].replaceAll("^00?0?", ""));
			}
			Integer[] intAry = { secNo, noOfDecPlaces };
			return intAry;
		}

		if (secNmbStr.trim().replaceAll("[\\d]", "").length() == 0) {
			Integer[] intAry = { 0, 0 };
			return intAry;
		}

		else
			return null;
	}

	public static int getBefDec(String secNmbStr) {
		return Integer.parseInt(secNmbStr.trim().split("\\.")[0]);
	}

	public static String getSectionNumber(String sectionHeading) {

		String sectionNumberStr = "";

		Pattern patternSectionNumber = Pattern.compile("[\\d]{1,3}[\\.]{0,1}[\\d]{0,2}(?=[\\. ])");
		Matcher match = patternSectionNumber.matcher(sectionHeading);
		if (match.find()) {
			sectionNumberStr = match.group().trim();
		}

		return sectionNumberStr;
	}

	public static boolean isSecNumberingConsecutive(String priorSecNumbStr, String curSecNumbStr,
			String nextSecNumbStr) {

		// if curSecNumbStr and priorSecNumbStr are consecutive return true
		// (subtract value after decimal - eg 6.10 from 6.9 than 10-9=1
		// if cur not consec with prior than see if value prior to dec is consec
		// with nextSecNumbStr -- e.g., if it goes from 3.8 to 4.1 or no decimal
		// eg if 310 to 401 than try subtracting 1st digit of each of both - eg,
		// 909 to 1001

		return false;
	}

	public static boolean isSectionHeading(List<String[]> list, int i) {
		boolean isSectionHeading = false;
		// using section number with decimal to see if there's a match.
		String secNmbStr = "", befDec = "", aftDec = "", oneAheadSecNmbStr = "", oneAheadBefDec = "",
				oneAheadAftDec = "", oneBehindSecNmbStr = "", oneBehindBefDec = "", oneBehindAftDec = "";
		int befD, aftD, oAhBefD = 0, oAhAftD = 0, oBeBefD = 0, oBeAftD = 0;
		// oA=one ahead cur sec heading, BefD is value before Decimal and AftD
		// is value after decimal. oBe is one behind cur sec hdg.
		boolean noDecimal = false;
		secNmbStr = list.get(i)[3];
		// if no decimal length must be at least 3 b/c less than it would be eg,
		// 10, 11, 12 sec nmbs and therefore always consec.
		if (!secNmbStr.contains(".") && secNmbStr.length() < 4)
			return false;
		if (!secNmbStr.contains(".")) {
			noDecimal = true;
			// if no decimal -than insert one if secNmb is at least 3 digits.
			secNmbStr = insertDec2ndFromLeft(secNmbStr);
		}

		befDec = secNmbStr.split("\\.")[0];
		befD = Integer.parseInt(befDec);
		aftDec = secNmbStr.split("\\.")[1];
		aftD = Integer.parseInt(aftDec);

		if (i > 0) {
			oneBehindSecNmbStr = list.get(i - 1)[3];
			if (noDecimal) {
				oneBehindSecNmbStr = insertDec2ndFromLeft(oneBehindSecNmbStr);
			}

			oneBehindBefDec = oneBehindSecNmbStr.split("\\.")[0];
			oBeBefD = Integer.parseInt(oneBehindBefDec);
			oneBehindAftDec = oneBehindSecNmbStr.split("\\.")[1];
			oBeAftD = Integer.parseInt(oneBehindAftDec);
		}

		if (i + 1 < list.size()) {

			oneAheadSecNmbStr = list.get(i + 1)[3];

			if (noDecimal) {
				oneAheadSecNmbStr = insertDec2ndFromLeft(oneAheadSecNmbStr);
			}

			oneAheadBefDec = oneAheadSecNmbStr.split("\\.")[0];
			oAhBefD = Integer.parseInt(oneAheadBefDec);
			oneAheadAftDec = oneAheadSecNmbStr.split("\\.")[1];
			oAhAftD = Integer.parseInt(oneAheadAftDec);
		}

		// difference between cur sec and ahead (for bef and aft dec vals
		// respectively).
		int difAhBef, difAhAft, difBehBef, difBehAft;
		difAhBef = oAhBefD - befD;
		difAhAft = (oAhAftD - aftD) * 100;
		difBehBef = befD - oBeBefD;
		difBehAft = (aftD - oBeAftD) * 100;

		// dif bef should not be multipled.
		if (difBehBef == 1 || difBehBef == 10 || difBehBef == 100 || difBehBef == 1000 || difBehAft == 1
				|| difBehAft == 10 || difBehAft == 100 || difBehAft == 1000 || difAhBef == 1 || difAhBef == 10
				|| difAhBef == 100 || difAhBef == 1000 || difAhAft == 1 || difAhAft == 10 || difAhAft == 100
				|| difAhAft == 1000) {
			isSectionHeading = true;
			// NLP.pwNLP.append(NLP.println("isSectionHeading=" + isSectionHeading, ""));
			return isSectionHeading;
		}

		// NLP.pwNLP.append(NLP.println("secNmbStr=" + secNmbStr + " befDec=" + befDec +
		// " aftDec=" + aftDec
		// + "\roneBehindSecNmbStr=" + oneBehindSecNmbStr + " oneBehindBefDec=" +
		// oneBehindBefDec
		// + " oneBehindAftDec=" + oneBehindAftDec + "\roneAheadSecNmbStr=" +
		// oneAheadSecNmbStr
		// + " oneAheadBefDec=" + oneAheadBefDec + " oneAheadAftDec=" + oneAheadAftDec,
		// ""));

		return false;
	}

	public static String insertDec2ndFromLeft(String secNmbStr) {

		if (secNmbStr.trim().length() < 3)
			return null;
		else {
			secNmbStr = secNmbStr.substring(0, 2) + "." + secNmbStr.substring(2, secNmbStr.length());
		}

		return secNmbStr;
	}

	public static String[] fetchHeading(List<String[]> list, int sentSIdx, int sentEIdx, String type, int lastIdx) {

		// determines which sec heading a sentence falls within - or which
		// exhibit heading a sentence falls within or which definition heading a
		// sentence falls within. In addition it returns the sIdx and eIdx of
		// text that the sec/def/exh heading encompasses. This is used at the
		// moment solely to make sure the last defined term and its sentences
		// don't encompass two section headings - as any alphabetically
		// consecutive list of defined terms are contained in one sec hdg.

		int sIdx = 0, eIdx = 0, midIdx = 0;
		String hdg = "";
		// NLP.pwNLP.append(NLP.println("fetch hdg type=" + type + " i - lastIdx=" +
		// lastIdx + " sentSIdx=" + sentSIdx
		// + " sentEIdx=" + sentEIdx + " hdg=", hdg));

		// System.out.println("listSidxEidxTypeDefHdgs list.size="+lastIdx);
		for (int i = lastIdx; i < list.size(); i++) {
			sIdx = Integer.parseInt(list.get(i)[0]);
			eIdx = Integer.parseInt(list.get(i)[1]);
			midIdx = (sIdx + eIdx) / 2;

			// if(type.contains("def")) {
			// System.out.println(" sidx=" + sIdx + " eIdx=" + eIdx + " sentSIdx=" +
			// sentSIdx + " sentEIdx=" + sentEIdx
			// + " hdg=" + list.get(i)[3]);
			// }

			// if ((sentSIdx + 1) >= sIdx && sentEIdx >= sIdx && (sentEIdx - 1) < eIdx &&
			// list.get(i)[2].equals(type)) {

			if (midIdx > sentSIdx && midIdx < sentEIdx && list.get(i)[2].equals(type)) {

				hdg = list.get(i)[3];

				if (type.toLowerCase().contains("def")) {
					// System.out.println("i=" + i + " fetch hdg type=" + type + " sIdx=" + sIdx + "
					// eIdx=" + eIdx
					// + " sentSIdx=" + sentSIdx + " sentEidx=" + sentEIdx + " hdg=" + hdg);
				}

				String[] ary = { i + "", hdg, sIdx + "", eIdx + "" };
				return ary;
			}
		}
		return null;
	}

	public static void getContractSentencesReadyForSolr(String year, String qtr) throws IOException, SQLException {

		GetContracts gK = new GetContracts();
//		System.out.println("listContractAttributes.get(0)[1]==" + listContractAttributes.get(0)[1]);

		Utils.createFoldersIfReqd("e:/getContracts/tmp/");
		String strippedQtrPath = GetContracts.strippedFolder + "/" + contractType + "/" + year + "/QTR" + qtr + "/";
		File strippedFilesFolder = new File(strippedQtrPath);
		File[] listOfStrippedFiles = strippedFilesFolder.listFiles();
		String sectionYrQtrXMLPath = "";
		String paragraphYrQtrXMLPath = "";
		String parentChildYrQtrXMLPath = "";
		String sentenceYrQtrXMLPath = "";
		String clausesYrQtrXMLPath = "";

		/*
		 * simpler approach that mirrors the russian doll. 1st: its the contract (which
		 * we don't push into solr as a doc). Then we find sections (see getSections)
		 * and we push that into solr. Then b/c we know a paragraph is contained in a
		 * section - we only look to the russian parent and iterate getParagraphs
		 * through that section. Then we need sentences and we iterate getSentences
		 * through the paragraph and so on.
		 */

		if (null == listOfStrippedFiles || listOfStrippedFiles.length == 0)
			return;

		@SuppressWarnings("unused")
		String sentence = "", wordCount = "";
		@SuppressWarnings("unused")
		int sIdx = 0, sentEIdx = 0, endKidx = 0;
		// , lastThreeWords ="", nextSent = "", nextSentFirstWord = ""

		System.out.println("strippedfolderQtrYear=" + strippedQtrPath);

		int cnt = 0;
		for (File f2 : listOfStrippedFiles) {

			File strippedFilesFolderContractType = f2;
			File[] listOfStrippedFilesContractTypes = strippedFilesFolderContractType.listFiles();

			cnt = 0;
			for (File f : listOfStrippedFilesContractTypes) {
				sectionYrQtrXMLPath = f2.getAbsolutePath().replaceAll("(?ism)stripped", "sections");
				paragraphYrQtrXMLPath = f2.getAbsolutePath().replaceAll("(?ism)stripped", "paragraphs");
				sentenceYrQtrXMLPath = f2.getAbsolutePath().replaceAll("(?ism)stripped", "sentences");
				parentChildYrQtrXMLPath = f2.getAbsolutePath().replaceAll("(?ism)stripped", "parentChild");
				clausesYrQtrXMLPath = f2.getAbsolutePath().replaceAll("(?ism)stripped", "clauses");

				System.out.println("f.len=" + f.length() + "\rfilename=" + f.getAbsolutePath());
				if (f.length() > 5000) {

					System.out.println("2. f===" + f.getName());
					System.out.println("f2.getAbsolutePath() - with contractType=" + f2.getAbsolutePath());
					solrFiles(f, year, qtr, contractType, f2.getAbsolutePath(), sectionYrQtrXMLPath,
							paragraphYrQtrXMLPath, parentChildYrQtrXMLPath, sentenceYrQtrXMLPath, regenerate);

				}
			}
		}
	}

	public static String getDefinition(String text) throws IOException {

		// System.out.println("def text.len="+text.length());

		List<String[]> listSidxEidxTypeDefHdgsTmp = SolrPrep.getDefinitions_for_parsing_jsons(text);

		listSidxEidxTypeDefHdgs = new ArrayList<String[]>();
		listSidxEidxTypeDefHdgs = SolrPrep.validateDefinitions(listSidxEidxTypeDefHdgsTmp, text);

		NLP.printListOfStringArray("validateDefinitions==", listSidxEidxTypeDefHdgs);

		String definitionName = "";
		int sIdx = 0, eIdx = 0, priorEidx = 0;

		if (SolrPrep.listSidxEidxTypeDefHdgs.size() == 0)
			return text;

		// NLP.printListOfStringArray("validated Def=",
		// SolrPrep.listSidxEidxTypeDefHdgs);
		StringBuilder sb = new StringBuilder();
		String txt = "";
		for (int n = 0; n < SolrPrep.listSidxEidxTypeDefHdgs.size(); n++) {
			sIdx = Integer.parseInt(SolrPrep.listSidxEidxTypeDefHdgs.get(n)[0]);
			eIdx = Integer.parseInt(SolrPrep.listSidxEidxTypeDefHdgs.get(n)[1]);
			definitionName = SolrPrep.listSidxEidxTypeDefHdgs.get(n)[3];
			// System.out.println("22 sIdx="+sIdx+" eIdx="+eIdx);
			if (n == 0 && sIdx > 0) {
				txt = (text.substring(0, sIdx));
				txt = txt.replaceAll("[\r\n]{4,}", "\r\n\r\n");
				sb.append(txt);
			}

			if (n > 0 && sIdx != priorEidx && eIdx != priorEidx && priorEidx < sIdx) {

				// System.out.println("sIdx=" + sIdx + " priorEidx=" + priorEidx);

				txt = (text.substring(priorEidx, sIdx));
				txt = txt.replaceAll("[\r\n]{4,}", "\r\n\r\n");
				sb.append(txt);
			}

			// <dn>=Definition name, <dt>=definition text
			// System.out.println("definitionName="+definitionName);
			txt = text.substring(sIdx, eIdx);
			txt = txt.replaceAll("[\r\n]{3,}", "\r\n\r\n");
			sb.append("<dT><dN>" + definitionName + "</dN>" + txt + "</dT>");

			priorEidx = eIdx;

			if (1 + n == listSidxEidxTypeDefHdgs.size()) {
				sb.append(text.substring(eIdx, text.length()));
			}
		}

		// sb.append(text.substring(sb.toString().length(),text.length()));

		return sb.toString();

	}

	public static void solrFiles(File f, String year, String qtr, String contractFolder, String strippedQtrPath,
			String sectionYrQtrXMLPath, String paragraphYrQtrXMLPath, String parentChildYrQtrXMLPath,
			String sentenceYrQtrXMLPath, boolean regenerate) throws IOException, SQLException {

		GetContracts gK = new GetContracts();
		System.out.println("1. f=======" + f.getAbsolutePath());

		// String strippedQtrPath = strippedPath + "/" + contractFolder + "/" + year +
		// "/QTR" + qtr + "/";
		// File strippedFilesFolder = new File(strippedQtrPath);
		// File[] listOfStrippedFiles = strippedFilesFolder.listFiles();
		// String sentencesQtrPath = sentencesPath + contractFolder + "/" + year +
		// "/QTR" + qtr + "/";
		String text = "", textSplit = "", fileDate, cik, filer;

		int sIdx = 0, sentEIdx = 0, sentSIdx = 0
		// ,endKidx = 0
		;

		FileSystemUtils.createFoldersIfReqd(sectionYrQtrXMLPath);
		FileSystemUtils.createFoldersIfReqd(paragraphYrQtrXMLPath);
		FileSystemUtils.createFoldersIfReqd(parentChildYrQtrXMLPath);
		FileSystemUtils.createFoldersIfReqd(sentenceYrQtrXMLPath);

		String kId, filename, contractLongName;
		// , lastThreeWords ="", nextSent = "", nextSentFirstWord = ""

		NLP nlp = new NLP();
		int cnt = 0;
		String contractName = contractFolder;
		// StringBuffer sb = new StringBuffer();
		String fn = "";

		File fileXMLsec = new File(strippedQtrPath + "/");
		File fileXMLpara = new File(strippedQtrPath + "/");
		File fileXMLparChi = new File(strippedQtrPath + "/");
		File fileXMLsent = new File(strippedQtrPath + "/");

		System.out.println("fn=" + f.getName());

		if (nlp.getAllMatchedGroups(f.getName(), Pattern.compile("(?ism)jpeg|jpg|gif")).size() > 0
				|| nlp.getAllMatchedGroups(f.getName(),
						Pattern.compile("(?ism)(^|[\r\n ]{1})(GRAPHIC|IMAGE)($|[\r\n \\.]{1})")).size() > 0) {
			return;
		}

		fn = f.getName().substring(0, f.getName().length());
		System.out.println("fn=" + fn);
		fn = (fn.substring(0, 22) + fn.substring(22, 23) + ".xml").replaceAll("[ ]+\\.xml", ".xml");
//		System.out.println("fn===" + fn);
		fileXMLsec = new File(sectionYrQtrXMLPath + "/" + fn);
		fileXMLpara = new File(paragraphYrQtrXMLPath + "/" + fn);
		fileXMLparChi = new File(parentChildYrQtrXMLPath + "/" + fn);
		fileXMLsent = new File(sentenceYrQtrXMLPath + "/" + fn);
		// System.out.println("regenerate sentence xml regenerate?=" + regenerate);
		if (fileXMLpara.exists() && !regenerate) {
			// System.out.println("already sentence idx - skipping");
			return;
		}

		System.out.println("fileXml===" + fileXMLpara.getName());

		text = Utils.readTextFromFile(f.getAbsolutePath());
		long fileSize = f.length();

		// System.out.println("fileSize=" + fileSize + " file=" + f.getAbsolutePath());

		if (fileSize < 5000 || fileSize > 12000000) {
			// if (fileSize > 7000000) {
			// System.out.println("too big fileSize=" + fileSize);
			// }

			return;
		}

		// sb.delete(0, sb.toString().length());
		textSplit = text.substring(0, Math.min(text.length(), 3000)).replaceAll("^[ ]{0,10}", "")
				.replaceAll("^[\r\n]{3,10}", "");

		System.out.println("f=" + f.getName());
		kId = nlp.getAllMatchedGroups(f.getName(), Pattern.compile("[\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,2}")).get(0);
		System.out.println("kId=" + kId);

		if (!LawGo && textSplit.split("\\|\\|").length < 4) {
			System.out.println("return .len<4");
			return;
		}

		// System.out.println("start sentence .xml kId=" + kId + "|END");
		// endKidx = Integer.parseInt(textSplit.split("\\|\\|")[0].trim());

		List<String> listMetaDataFetched = nlp.getAllMatchedGroups(textSplit, Pattern.compile("\\|\\|"));
		List<String> listMetaData = new ArrayList<String>();
		if (!LawGo) {

			cik = listMetaDataFetched.get(1).trim();
			listMetaData.add(cik);
			fileDate = listMetaDataFetched.get(2).trim() + "T00:00:00Z";
			listMetaData.add(fileDate);
			filer = textSplit.split("\\|\\|")[3].replaceAll("xxPD", "\\.").trim();
			filename = listMetaDataFetched.get(4).replaceAll("xxPD", "\\.").trim();
			if (filer.indexOf("\r") > 0)
				filer = filer.substring(0, filer.indexOf("\r")).replaceAll("\r", "");
			if (filer.indexOf("\n") > 0)
				filer = filer.substring(0, filer.indexOf("\n")).replaceAll("\n", "");
			listMetaData.add(filer);
			contractLongName = f.getName().replaceAll("\\.txt", "").replaceAll("[ ]+", " ");
			listMetaData.add(contractLongName);
			listMetaData.add(fileSize + "");
			listMetaData.add(kId);
			listMetaData.add(contractName);
			listMetaData.add(filename);
			// listMetaData[0]=cik,[1]=filedate,[2]=filer,[3]=contractLongName,[4]=fileSize,[5]=kId,[6]=contractName/type,[7]=filename

		}

		if (LawGo) {
			cik = f.getName().substring(0, 10);
			listMetaData.add(cik);
			fileDate = "20" + cik.substring(0, 2) + "-" + cik.substring(2, 4) + "-" + cik.substring(4, 6)
					+ "T00:00:00Z";
			listMetaData.add(fileDate);
			filer = "";
			listMetaData.add(filer);
			contractLongName = f.getName().replaceAll("\\.txt", "").replaceAll("[ ]+", " ");
			contractLongName = "";
			listMetaData.add(contractLongName);
			listMetaData.add(fileSize + "");
			listMetaData.add(kId);
			contractName = "";
			listMetaData.add(contractName);
			// listMetaData[0]=cik,[1]=filedate,[2]=filer,[3]=contractLongName,[4]=fileSize,[5]=kId,[6]=contractName/type,[7]=filename

//			List<String> ListOfLegalEntitiesInContract = nlp.getAllMatchedGroups(
//					EntityRecognition.getLegalEntities(text), Pattern.compile("(?<=<b>).*?(?=</b>)"));

//			for (int i = 0; i < ListOfLegalEntitiesInContract.size(); i++) {
//				if (i < 8)
//					continue;
//				listMetaData.add(ListOfLegalEntitiesInContract.get(i));
			// grabbing legal entities.
//			}
		}

		// Logic: as I cycle through each paragraph - ck for first instance of each of
		// sec,def,exh end - if a respective start of any occur first - there is no hdg
		// of that kind. Definitions are taken even if multiple paragraphs.
		// solrFileSaved goes through stripped text and through each of the
		// getSections,getExhibits,getDefinitions and getParagraphs it assigns idx
		// values to raw text for each of these sections into mysql. Then I fetch those
		// idx to mark for solr fields. This removes issues of idx mismatches and idx
		// now line-up.

		// System.out.println("listMetaData");
		// NLP.printListOfString("", ListMetaData);
		// PrintWriter pw = new PrintWriter(new File("e:/getContracts/temp/Text1.txt"));
		// pw.append(text);
		// pw.close();

		System.out.println("save file=" + fileXMLsent.getAbsolutePath());
		solrFileSaved(fileXMLsec, fileXMLpara, fileXMLparChi, fileXMLsent, text, listMetaData);

		// solrFilesSections(text,listData);
		// solrFilesSections();
		// solrFilesSections();

	}

	public static void solrFileSaved(File fileXMLsec, File fileXMLpara, File fileXMLparChi, File fileXMLsent,
			String text, List<String> listMetaData) throws IOException, SQLException {

		System.out.println("NOTE** method solrFileSaved starts the process of identifying \r\n"
				+ "for exhibits, sections, paragraphs etc. So DO NOT CHG K text or it \r\n"
				+ "will corrupt the IDX locations and solr docs.");

		// PrintWriter pw = new PrintWriter(new File("e:/getContracts/temp/t2.txt"));

		// this is location where the exhibit page numbers are saved
		// String exhibitPageNumbersFilePath=
		// fileXMLpara.getAbsolutePath().replaceAll("solrParagraphs",
		// "exhibitPageNumbers");
		// System.out.println("exhibitPageNumbersFilePath="+exhibitPageNumbersFilePath);

		text = SolrPrep.getExhibits(text);

		// pw = new PrintWriter(new File("e:/getContracts/temp/tmpExh.txt"));
		// pw.append(text);
		// pw.close();

//		System.out.println("after getExhibits text.len=" + text.length());

		List<String> listSec = SolrPrep.getSections(text);
//		System.out.println("after getSections listSec.size=" + listSec.size());

		// File fileXMLsec = new
		// File(fileXMLsent.getAbsolutePath().replaceAll("(?ism)sentence|paragrah",
		// "Section"));

		// Utils.createFoldersIfReqd(
		// fileXMLsec.getAbsolutePath().substring(0,
		// fileXMLsec.getAbsolutePath().lastIndexOf("\\")));

		// System.out.println("fileXMLsec===" + fileXMLsec.getAbsolutePath());
		// solrSectionXMLfileFinalized(listSec, listMetaData, fileXMLsec);

		text = SolrPrep.getParagraphs(listSec);
		// see listSec above.
		// System.out.println("after getParagraphs text.len=" + text.length());

		text = getDefinition(text);

		// System.out.println("after getDefinitions text.len=" + text.length());
		// pw = new PrintWriter(new File("e:/getContracts/temp/tmpDef.txt"));
		// pw.append(text);
		// pw.close();
		// <==delete this

		// THIS IS USING MYSQL - WE WILL ABANDON MYSQL FOR A SIMPLER WAY. WHICH IS TO
		// LOOP FROM LARGEST TEXT TYPE XML TO SMALLEST. EG IF SECTION IS LARGEST - WE
		// THEN RUN PARA ON SECTION THEN SENTENCE ON PARA AND SO ON. THIS RETAIN X-REF
		// BACK TO EACH AND IDX LOC ETC.
		text = text.replaceAll("(?sm)<dT>[\r\n]+</dT>", "");
		text = text.replaceAll("(?sm)<pT>[\r\n]+</pT>", "");
		text = text.replaceAll("(?sm)<sT>[\r\n]+</sT>", "");
		text = text.replaceAll("(?sm)<eT>[\r\n]+</eT>", "");
		text = text.replaceAll("(?sm)<eT>", "<eT></pT><pT>");
		text = text.replaceAll("(?sm)>[\r\n]+<", "><");
//		System.out.println("9 after trim text.len=" + text.length());
		// File f = new File("e:/getContracts/temp/prelimFnlText.txt");

		// pw = new PrintWriter(new File("e:/getContracts/temp/repl_def.txt"));
		// pw.append(text);
		// pw.close();

		NLP nlp = new NLP();
		StringBuilder sb = new StringBuilder();
		List<Integer[]> listDef = nlp.getAllStartAndEndIdxLocations(text, Pattern.compile("((?sm)<dT>.*?</dT>)"));
		int priorEidx = 0, sIdx = 0, eIdx = 0;
//		System.out.println("listDef.size=" + listDef.size());
		String textPart = "";

//		System.out.println("creating paragraph markers to encase entire def");
		for (int i = 0; i < listDef.size(); i++) {
			sIdx = listDef.get(i)[0];
			eIdx = listDef.get(i)[1];
			// System.out.println("sIdx="+sIdx+" eIdx="+eIdx);
			if (i == 0) {
				if (sIdx > 0) {
					sb.append(text.substring(0, sIdx));
					// System.out.println("1 text part="+text.substring(0, eIdx));
				}

				sb.append(text.substring(sIdx, eIdx).replaceAll("(?sm)\r\n</pT><pT>\r\n", "\r\n"));
				// System.out.println("2 text part="+text.substring(sIdx, eIdx));
			}

			if (i > 0 && i + 1 < listDef.size()) {
				if (priorEidx != sIdx) {
					sb.append(text.substring(priorEidx, sIdx));
				}
				sb.append(text.substring(sIdx, eIdx).replaceAll("(?sm)\r\n</pT><pT>\r\n", "\r\n"));
			}

			if (i + 1 == listDef.size()) {
				sb.append(text.substring(priorEidx, sIdx));
				sb.append(text.substring(sIdx, eIdx).replaceAll("(?sm)\r\n</pT><pT>\r\n", "\r\n"));
				// System.out.println("c sIdx="+sIdx+" c eIdx="+eIdx);
				sb.append(text.substring(eIdx, text.length()));
				// System.out.println("d eIdx="+eIdx+" d text.len="+text.length());
			}

			priorEidx = eIdx;

		}

		if (listDef.size() > 0) {
			text = sb.toString();
		}

//		System.out.println("11 text.len=" + text.length());
		// fileXML = new File("e:/getContracts/temp/fnlText.txt");

		// return file of of indexes for each para, section,etc
		// pw = new PrintWriter(new File("e:/getContracts/temp/fileXML.txt"));
		// pw.append(text);
		// pw.close();

		File fileOfIndexes = solrFileFieldIndexes(text, fileXMLpara);
		// pickup with this method to embed solrfields for ingestion

		List<String> listSecParaSentText = solrFileFinalized(fileOfIndexes, text, listMetaData);
		// <====THIS IS WHERE THE PROCESSING NEEDS TO GET REVISED TO ABANDON MYSQL

		if (fileXMLsec.exists())
			fileXMLsec.delete();

		if (fileXMLpara.exists())
			fileXMLpara.delete();

		if (fileXMLparChi.exists())
			fileXMLparChi.delete();

		if (fileXMLsent.exists())
			fileXMLsent.delete();

		PrintWriter pwSec = new PrintWriter(fileXMLsec);

		pwSec.append(listSecParaSentText.get(0));
		pwSec.close();

		System.out.println("saving para xml file = " + fileXMLpara.getAbsolutePath());
		PrintWriter pwPara = new PrintWriter(fileXMLpara);
		pwPara.append(listSecParaSentText.get(1));
		pwPara.close();

		PrintWriter pwParChi = new PrintWriter(fileXMLparChi);
		pwParChi.append(listSecParaSentText.get(2));
		pwParChi.close();

//		PrintWriter pwSent = new PrintWriter(fileXMLsent);
//		pwSent.append(listSecParaSentText.get(3));
//		pwSent.close();

		String sentText = getSecondLeadinClause(listSecParaSentText.get(3));
//		System.out.println("sentText==="+sentText);
		if (fileXMLsent.exists())
			fileXMLsent.delete();

		System.out.println("saving sent xml file = " + fileXMLsent.getAbsolutePath());
		PrintWriter pwSent2 = new PrintWriter(fileXMLsent);
		pwSent2.append(sentText);
		pwSent2.close();
		// getClauses method takes the final xml sentence and parses into clauses and
		// saves to clause folder
		// getClauses(listParaSentText.get(1),
		// fileXMLsent.getAbsolutePath().replaceAll("(?ism)sentences", "clauses"));

	}

	public static List<String[]> getSidxAndExhLtr(List<String[]> list, int i, int plusMinus) throws IOException {

		NLP nlp = new NLP();
		List<String[]> listIdxExhLtr = new ArrayList<>();
		String idx = list.get(i + plusMinus)[0];
		Pattern patternExhLtr = Pattern.compile(
				"(?sm)(?<=Exhibit|EXHIBIT|APPENDIX|Appendix|SCHEDULE" + "|Schedule|Annex|ANNEX).{1,4}[A-Zi\\d]{1,6}");
		String exhLtr = "";
		List<String> listTmpExhLtr = nlp.getAllMatchedGroups(list.get(i + plusMinus)[1], patternExhLtr);
		String tmpExhLtrNo = "";
		if (listTmpExhLtr.size() > 0) {
			// to change K-1 to K if it contains - '-'
			exhLtr = listTmpExhLtr.get(0);
			if (exhLtr.contains("-") && exhLtr.length() > 2) {
				// System.out.println("exhLtr=" + exhLtr + " exhLtr.split[0]=" +
				// exhLtr.split("-")[0]);
				tmpExhLtrNo = exhLtr.substring(exhLtr.indexOf("-"));
				exhLtr = exhLtr.split("-")[0];
			}
		}

		if (exhLtr.length() > 0 && idx.length() > 0) {
			String[] ary = { idx, exhLtr, tmpExhLtrNo };
			listIdxExhLtr.add(ary);
			return listIdxExhLtr;
		}

		return listIdxExhLtr;

	}

	public static List<String[]> getExhibitConfirmationData(List<String[]> listExhSidxPrefix, int i, String text)
			throws IOException {

		NLP nlp = new NLP();
		// sidx[0] exhLtr[1] exhLtrNo[2] hasHypen[3] aftHypenNo[4] pgNo[5] pgNosidx[6]

		String hasHyphen = "false";
		double sIdx = -1;
		int exhLtrNo = -1;
		String exhLtr = "", aftHypNo = "-1";
		List<String[]> listExhDataPoints = new ArrayList<>();

		List<String[]> list = getSidxAndExhLtr(listExhSidxPrefix, i, 0);
		sIdx = Double.parseDouble(list.get(0)[0]);
		if (list.size() > 0) {
			exhLtr = list.get(0)[1];
		}

		if (nlp.getAllMatchedGroups(exhLtr, Pattern.compile("[A-Z]{1}")).size() > 0) {
			exhLtrNo = GetContracts.getNumberInAlphabet(exhLtr);
		}

		// exhibit prefix letter is k-1, k-2 (eg Exhibit K-1, Exhibit K-2). So letter
		// can be same but after hyphen can't
		if (list.get(0)[2].length() > 0) {
			hasHyphen = "true";
		}

		if (hasHyphen.equals("true")) {
			// System.out.println("list - has hyphen="+Arrays.toString(list.get(0)));
			aftHypNo = list.get(0)[2].substring(list.get(0)[2].indexOf("-") + 1);
		}

		// sIdx,exhLtr,exhLtrNo,hasHyphen,aftHypNo

		String[] ary = { sIdx + "", exhLtr, exhLtrNo + "", hasHyphen, aftHypNo + "" };

		listExhDataPoints.add(ary);

		return listExhDataPoints;
	}

	public static List<String[]> getExhPgNos(String text) throws IOException {

		NLP nlp = new NLP();

		// examples: Sch. N-1, D-2, D-Exh A-11, A-I-15, A-2-3, (all on a line by
		// themself)
		List<String[]> listExhPgNos = NLP.getAllMatchedGroupsAndStartIdxLocs(text,
				Pattern.compile("(?<=[\r\n]{1,2}[ \t]{0,100})" + "("
						+ "(Exhibit|EXHIBIT|Schedule|Schedule|Annex|ANNEX|Appendix|APPENDIX) [A-Za-z]{1,3}-?\\d?\\d?|"
						+ "[A-Z]{1}-[\\d]{1,2}-[\\d]{1,2}" + "|[A-Z]{1}-[\\d]{1,2}"
						+ "|[A-Z]{1}[a-zA-Z]{0,4}\\. ?[A-Z]-\\d" + ")" + "(?=[ \t]{0,100}[\r\n]{1,2})"));

		return listExhPgNos;

	}

	public static List<String[]> getExhibitNamesLax(String text) throws IOException {

		List<String[]> listExhNameSidxLax = new ArrayList<>();
		NLP nlp = new NLP();

//		NLP.patternExhName = Pattern.compile("(?sm)[\r\n \t]{1,10}.+?\r\n.+?\r\n.+?\r\n.+?\r\n");
		List<String[]> listExhSidxPrefixLax = nlp.getAllStartIdxLocsAndMatchedGroups(text, NLP.patternExhibitPrefix);
		List<Integer> listPrefixAllCaps = nlp.getAllIndexEndLocations(text,
				Pattern.compile("(EXHIBIT |APPENDIX |SCHEDULE |ANNEX )(\\d|[A-Z]{1,2})"));
		List<Integer> listPrefixInitialAllCaps = nlp.getAllIndexEndLocations(text,
				Pattern.compile("(Exhibit |Appendix |Schedule |Annex )(\\d|[A-Z]{1,2})"));

//		System.out.println("listPrefixAllCaps.size=" + listPrefixAllCaps.size() + " listPrefixInitialAllCaps.size()="
//				+ listPrefixInitialAllCaps.size());

//		System.out.println("(double) listPrefixInitialAllCaps..size() / (double) listPrefixAllCaps.size()="
//				+ (double) listPrefixInitialAllCaps.size() / (double) listPrefixAllCaps.size());

		// if there are all caps exhibits then they would be the only ones to look for
		// b/c you don't typically use all caps in body or as page #. Keep this unless I
		// go to exhibit pg no to validate exhibit
		if ((double) listPrefixInitialAllCaps.size() / (double) listPrefixAllCaps.size() > 1.1) {
			listExhSidxPrefixLax = nlp.getAllStartIdxLocsAndMatchedGroups(text, NLP.patternExhibitPrefixAllCaps);
		} else {
			listExhSidxPrefixLax = nlp.getAllStartIdxLocsAndMatchedGroups(text, NLP.patternExhibitPrefix);
		}

		String tmpExhName = "", word = "", exhNameLax = "", exhNameTmpLax = "", exhNameStrLax = "", str = "",
				exhPrefLax = "", exhPrefNext = "", exhPrefPrior = "", nextExhName = "", nextExhPref = "",
				priorExhNameLax = "", priorExhPrefLax = "", exhNameStopped = "";
		double sIdx = 0, sIdxNext = 0, sIdxPrior = 0, textLen = text.length(), cntWords = 0, cntInitialCaps = 0,
				capRatio = 0.0;
		int matchExhNameInToc = 0, cnt = 0, cnt2 = 0, cnt3 = 0, cntBadPattern = 0, cntGoodPattern = 0,
				exhLastCharVal = -1, exhLastCharValPrior = -1, exhLastCharValNext = -1, distNext = -1, distPrior = -1,
				numberOfHardReturnsNext = -1, numberOfHardReturnsPrior = -1, minHardReturns = 11;
		String textAftExhPref = "", textBefExhPref = "", snip = "", snip2 = "", exhPrefLastChar = "",
				exhPrefLastCharNext = "", exhPrefLastCharPrior = "";

		// boolean notExh = false;
//		System.out.println("listExhSidxPrefix.size=" + listExhSidxPrefixLax.size());
		// see - exhName.replaceAll below - it must have same replacement

		for (int i = 0; i < listExhSidxPrefixLax.size(); i++) {
			// can't have exhibit prefix end with a '.'
//			System.out.println("listExhSidxPrefix=" + Arrays.toString(listExhSidxPrefixLax.get(i)));

			if (nlp.getAllMatchedGroups(listExhSidxPrefixLax.get(i)[1],
					Pattern.compile("(EXHIBIT|APPENDIX|SCHEDULE|ANNEX|Exhibit|Appendix|Schedule|Annex) (TO|To|to) "))
					.size() > 0) {
//				System.out.println("1 continue - listExhSidxPrefix=" + Arrays.toString(listExhSidxPrefixLax.get(i)));
				continue;
			}

			matchExhNameInToc = 0;
			sIdxPrior = sIdx;
			sIdx = Double.parseDouble(listExhSidxPrefixLax.get(i)[0]);

			if (sIdx / textLen < .3) {
//				System.out.println("2 continue - listExhSidxPrefix=" + Arrays.toString(listExhSidxPrefixLax.get(i)));
				continue;
			}

			priorExhNameLax = exhNameLax;
			priorExhPrefLax = exhPrefLax;
			distPrior = (int) sIdx - (int) sIdxPrior;
			if (i > 0) {
				numberOfHardReturnsPrior = nlp
						.getAllIndexEndLocations(text.substring((int) sIdxPrior, (int) sIdx), Pattern.compile("[\r\n]"))
						.size();
			}

			if (1 + i < listExhSidxPrefixLax.size()) {

				sIdxNext = Double.parseDouble(listExhSidxPrefixLax.get(i + 1)[0]);
				exhPrefNext = listExhSidxPrefixLax.get(i + 1)[1];
				exhPrefLastCharNext = exhPrefNext.substring(exhPrefNext.trim().length() - 1,
						exhPrefNext.trim().length());
				exhLastCharValNext = NLP.convertExhibitAlphaParaNumb(exhPrefLastCharNext);
				distNext = (int) sIdxNext - (int) sIdx;
				numberOfHardReturnsNext = nlp
						.getAllIndexEndLocations(text.substring((int) sIdx, (int) sIdxNext), Pattern.compile("[\r\n]"))
						.size();
			}

			exhPrefPrior = exhPrefLax;
			exhPrefLastCharPrior = exhPrefLastChar;
			exhLastCharValPrior = exhLastCharVal;

			exhPrefLax = listExhSidxPrefixLax.get(i)[1];
			exhPrefLastChar = exhPrefLax.substring(exhPrefLax.trim().length() - 1, exhPrefLax.trim().length());
			exhLastCharVal = NLP.convertExhibitAlphaParaNumb(exhPrefLastChar);

			if (nlp.getAllIndexEndLocations(exhPrefLastChar, Pattern.compile("\\d")).size() > 0
					&& Integer.parseInt(exhPrefLastChar) > 0) {
				exhLastCharVal = Integer.parseInt(exhPrefLastChar);
			}

			if (nlp.getAllIndexEndLocations(exhPrefLastCharNext, Pattern.compile("\\d")).size() > 0
					&& Integer.parseInt(exhPrefLastCharNext) > 0) {
				exhLastCharValNext = Integer.parseInt(exhPrefLastCharNext);
			}

			if (nlp.getAllIndexEndLocations(exhPrefLastCharPrior, Pattern.compile("\\d")).size() > 0
					&& Integer.parseInt(exhPrefLastCharPrior) > 0) {
				exhLastCharValPrior = Integer.parseInt(exhPrefLastCharPrior);
			}

			// if few hard returns after exhibit - it can't be an exhibit or is blank. I
			// can't use this for number of returns prior b/c prior could have a large
			// exhibit. But I can count how many exhibit patters occur prior and if a lot if
			// it not an exhibit. See below re exh text prior.

			exhNameStrLax = text.substring((int) sIdx + exhPrefLax.length(), Math.min(text.length(), (int) sIdx + 250));

			// find first line of exhibit name then if after stopped there are lower case -
			// continue - see 1,2,3,4;
//			System.out.println("1a. exh sIdx=" + listExhSidxPrefixLax.get(i)[0] + " exhPrefixLax=" + exhPrefLax
//					+ "\r\nexhNameStrLax=" + exhNameStrLax.substring(0, Math.min(50, exhNameStrLax.length())).trim());

			exhNameStopped = exhNameStrLax.replaceAll("[\r\n]+", "\r\n").trim();

			if (exhNameStopped.indexOf("\r\n") > 0) {
				exhNameStopped = exhNameStopped.substring(0, exhNameStopped.indexOf("\r\n")).trim();// 2
				exhNameStopped = StopWords.removeStopWords(exhNameStopped.replaceAll("[^a-zA-Z ]+", ""))
						.replaceAll("[ ]+", " ").replaceAll("Qx ?| ?XQ", "").trim();

				if (exhNameStopped.length() > 150) {
//					System.out
//							.println("3 continue - listExhSidxPrefix=" + Arrays.toString(listExhSidxPrefixLax.get(i)));
					continue;
				}

			}

			exhNameStopped = StopWords.removeStopWords(exhNameStopped.replaceAll("[^a-zA-Z ]+", ""))
					.replaceAll("[ ]+", " ").replaceAll("Qx ?| ?XQ", "").trim();

//			System.out.println("exhNameStop - 1st \r aft exhName=" + exhNameStopped + "|END");

//			System.out.println("1c. exhNameStop=" + exhNameStopped);

			// if 1st line aft exh prefix are lower case words then it's not a exh hdg name

			if (nlp.getAllIndexEndLocations(exhNameStopped, Pattern.compile("( |^)[a-z]")).size() > 0) {
//				System.out.println("4 continue - listExhSidxPrefix=" + Arrays.toString(listExhSidxPrefixLax.get(i)));
				continue;
			}

			exhNameLax = getExhibitName(exhNameStrLax, exhPrefLax, true).trim();
//			System.out.println("exhPref="+exhPrefLax+" got exhNameLax=" + exhNameLax);

			if (exhNameLax.trim().length() < 5 ||
			// (exhName.length()<17 &&
			// nlp.getAllMatchedGroups(exhName,
			// Pattern.compile("(?i)EXHIBIT|APPENDIX|SCHEDULE|ANNEX")).size()>0)
			// ||
					exhNameLax.contains("ARTICLE") || exhNameLax.contains("Article") || nlp
							.getAllMatchedGroups(exhNameLax,
									Pattern.compile("(EXHIBIT|APPENDIX|SCHEDULE|ANNEX"
											+ "|Exhibit|Appendix|Schedule|Annex) [A-Za-z\\d\\-]{1,7}\\)?\\."))
							.size() > 0
					|| exhNameLax.length() > 300

			) {
//				System.out.println("5 continue - exhNameLax=" + exhNameLax);
				continue;
			}

			// System.out.println("exhName="+exhName);
			// exhNameTmp = exhName.substring(0,
			// exhName.length()-1).replaceAll("[\\{\\}\\[\\%\\]\\*\\)\\( ]+",
			// "").toLowerCase();
			String textTmpLax = text.substring(0, (int) (textLen * .15)).replaceAll("[\\{\\}\\[\\%\\]\\*\\)\\( ]+", "")
					.toLowerCase();
			// <===if I change this - see textTmp below where I replace exhName
			matchExhNameInToc = nlp.getAllMatchedGroups(textTmpLax, Pattern.compile("(?ism)" + exhNameTmpLax)).size();
			// see textTmp above - it must have same replacement

			// System.out.println("exhName="+exhName+" 1=yes,0=no.
			// matchExhNameToc="+matchExhNameInToc);

			if (matchExhNameInToc < 1) {
				// textTmp replacements must be same as exhName replacements.
				if (i + 1 < listExhSidxPrefixLax.size() && numberOfHardReturnsNext < minHardReturns
						&& numberOfHardReturnsPrior < minHardReturns) {
//					System.out.println("1aa. not exh. exhPref=" + exhPrefLax + " exhName=" + exhNameLax);
					continue;
				}

				if (i == 0 && numberOfHardReturnsNext < minHardReturns) {
//					System.out.println("1ab1. not exh. exhPref=" + exhPrefLax + " exhName=" + exhNameLax);
					continue;
				}

				if (i + 1 < listExhSidxPrefixLax.size()
						&& (numberOfHardReturnsPrior >= minHardReturns && (distNext < 150 & distPrior < 150))) {
//					System.out.println("1ab2. not exh=" + exhPrefLax + " exhName=" + exhNameLax);
					continue;
				}

				if (nlp.getAllIndexEndLocations(exhPrefLastChar, Pattern.compile("(?sm)[\\.;\\:,]")).size() > 0 || nlp
						.getAllIndexEndLocations(exhPrefLax, Pattern.compile("(?sm)(4|10)\\.[\\d]{1,3}")).size() > 0) {
//					System.out.println("1ab3. not exh=" + exhPrefLax + " exhName=" + exhNameLax);
					continue;
				}

				textBefExhPref = text.substring(Math.max((int) sIdx - 15, 0), (int) sIdx);
				// System.out.println("textBefExhPref="+textBefExhPref);
				String[] textBefExhPrefSpl = textBefExhPref.split("\r\n");

				// if any text prior to exhibit prefix but no hard return (b/c it has to be same
				// line)
				if (textBefExhPrefSpl.length == 0 && textBefExhPref.replaceAll("[^a-zA-Z]", "").length() > 0) {
//					 System.out.println("1c. not exh textBefExhPref=" + textBefExhPref);
//					System.out.println("1c. not exh. exhPref" + exhPrefLax + " exhName=" + exhNameLax);
					continue;
				}

				textBefExhPref = text.substring(Math.max((int) sIdx - 700, 0), (int) sIdx);
				// if too many exhibit patterns prior then it can't an exhibit.
				if (nlp.getAllIndexEndLocations(textBefExhPref, NLP.patternExhibitPrefix).size() > 2) {
					// System.out.println("1d. not exh textBefExhPref=" + textBefExhPref);
//					System.out.println("1d. not exh. exhPref" + exhPrefLax + " exhName=" + exhNameLax);
					continue;
				}

				textAftExhPref = text.substring((int) sIdx, Math.min((int) sIdx + 700, text.length()));
				// if too many exhibit patterns after exhibit then it can't be an exhibit.
				if (nlp.getAllIndexEndLocations(textAftExhPref, NLP.patternExhibitPrefix).size() > 2) {
					// System.out.println("1e. not exh textBefExhPref=" + textBefExhPref);
//					System.out.println("1e. not exh. exhPref" + exhPrefLax + " exhName=" + exhNameLax);
					continue;
				}

				// if any text prior to exhibit prefix and on same line = f/p
				if (textBefExhPrefSpl.length > 0
						&& textBefExhPrefSpl[textBefExhPrefSpl.length - 1].replaceAll("[^a-zA-Z]", "").length() > 0) {
					// System.out.println("1f. not exh textBefExhPref=" +
					// textBefExhPrefSpl[textBefExhPrefSpl.length - 1]);
//					System.out.println("1f. not exh. exhPref" + exhPrefLax + " exhName=" + exhNameLax);
					continue;
				}
			}

			cntGoodPattern = 0;
			cntBadPattern = 0;

			cntBadPattern = nlp.getAllMatchedGroups(exhPrefLax,
					Pattern.compile("(?sm)(Exhibit |EXHIBIT |APPENDIX |Appendix |SCHEDULE |Schedule |Annex \"\r\n"
							+ "			+ \"|ANNEX )(OF|of|Of)"))
					.size();

			cntBadPattern = cntBadPattern + nlp
					.getAllMatchedGroups(exhNameLax,
							Pattern.compile(
									"(?ism)" + "[ \r\n](attention|If to the.{1,14}|signed|dated|as of|addresse?d?):"))
					.size();

			if (cntBadPattern > 0) {
//				System.out.println("1g. not exh. exhPref" + exhPrefLax + " exhName=" + exhNameLax);
				exhNameLax = "";
			}

			if (exhNameLax.length() > 0) {
				String[] ary = { exhPrefLax + " " + exhNameLax, (int) sIdx + "" };
//				System.out.println("I found the exhibit=" + Arrays.toString(ary));
				listExhNameSidxLax.add(ary);
			}
		}

//		 NLP.printListOfStringArray("final exhibit name=", listExhNameSidxLax);
		return listExhNameSidxLax;

	}

	public static List<String[]> exhibitMarkers(String text) throws IOException {

		NLP nlp = new NLP();

		TreeMap<Integer, String[]> mapPages = new TreeMap<>();
		List<String> listPatterns = new ArrayList<>();
		listPatterns.add(NLP.patterni.toString());
		listPatterns.add(NLP.patternPgNoSimple.toString());
		listPatterns.add(NLP.patternA1.toString());
		listPatterns.add(NLP.patternADash1.toString());
		listPatterns.add(NLP.patternADash1Dash1.toString());
		String pattern = "";

		int cnt = 0;
		String pgLtrTmp = "";
		for (int i = 0; i < listPatterns.size(); i++) {
			pattern = listPatterns.get(i);
			if (nlp.getAllStartIdxLocsAndMatchedGroups(text, Pattern.compile(pattern)).size() > 0) {
				// System.out.println("pattern.toString=" + pattern);
//				System.out.println("it worked");
				List<String[]> list = nlp.getAllStartIdxLocsAndMatchedGroups(text, Pattern.compile(pattern));
				// NLP.printListOfStringArray("", list);
				for (int c = 0; c < list.size(); c++) {

					if (nlp.getAllMatchedGroups(list.get(c)[1], NLP.patternPageLtrs).size() > 0) {
						// System.out.println("idx=" + list.get(c)[0] + " pg=" + list.get(c)[1] + " pg#"
						// + nlp.getAllMatchedGroups(list.get(c)[1], NLP.patternPageNumbers).get(0) + "
						// pgLtr="
						// + nlp.getAllMatchedGroups(list.get(c)[1], NLP.patternPageLtrs).get(0));

						// String[] ary = { "pg=" + list.get(c - 1)[1],
						// "pg#=" + nlp.getAllMatchedGroups(list.get(c - 1)[1],
						// NLP.patternPageNumbers).get(0),
						// "pgLtr=" + nlp.getAllMatchedGroups(list.get(c - 1)[1],
						// NLP.patternPageLtrs).get(0) };
						// mapPages.put(Integer.parseInt(list.get(c - 1)[0]), ary);

						if (c > 0) {
							String[] ary1 = { "pg=" + list.get(c)[1],
									"pg#=" + nlp.getAllMatchedGroups(list.get(c)[1], NLP.patternPageNumbers).get(0),
									"pgLtr=" + nlp.getAllMatchedGroups(list.get(c)[1], NLP.patternPageLtrs).get(0),

									"sIdx=" + list.get(c - 1)[0], "pg=" + list.get(c - 1)[1],
									"pg#=" + nlp.getAllMatchedGroups(list.get(c - 1)[1], NLP.patternPageNumbers).get(0),
									"pgLtr=" + nlp.getAllMatchedGroups(list.get(c - 1)[1], NLP.patternPageLtrs)
											.get(0) };
							mapPages.put(Integer.parseInt(list.get(c)[0]), ary1);
						}
						if (c == 0) {

							String[] ary1 = { "pg=" + list.get(c)[1],
									"pg#=" + nlp.getAllMatchedGroups(list.get(c)[1], NLP.patternPageNumbers).get(0),
									"pgLtr=" + nlp.getAllMatchedGroups(list.get(c)[1], NLP.patternPageLtrs).get(0),

									"sIdx=", "pg=", "pg#=", "pgLtr=" };
							mapPages.put(Integer.parseInt(list.get(c)[0]), ary1);

						}
					}

					else {
						// System.out.println("idx=" + list.get(c)[0] + " pg=" + list.get(c)[1] + " pg#"
						// + nlp.getAllMatchedGroups(list.get(c)[1], NLP.patternPageNumbers).get(0) + "
						// pgLtr=");
						pgLtrTmp = "";
						if (c > 0 && nlp.getAllMatchedGroups(list.get(c - 1)[1], NLP.patternPageLtrs).size() > 0) {
							pgLtrTmp = "pgLtr="
									+ nlp.getAllMatchedGroups(list.get(c - 1)[1], NLP.patternPageLtrs).get(0);
						}

						// String[] ary = { "pg=" + list.get(c-1)[1],
						// "pg#=" + nlp.getAllMatchedGroups(list.get(c-1)[1],
						// NLP.patternPageNumbers).get(0),
						// pgLtrTmp };
						// mapPages.put(Integer.parseInt(list.get(c-1)[0]), ary);

						if (c > 0) {
							String[] ary1 = { "pg=" + list.get(c)[1],
									"pg#=" + nlp.getAllMatchedGroups(list.get(c)[1], NLP.patternPageNumbers).get(0),
									"pgLtr=",

									"sIdx=" + list.get(c - 1)[0], "pg=" + list.get(c - 1)[1],
									"pg#=" + nlp.getAllMatchedGroups(list.get(c - 1)[1], NLP.patternPageNumbers).get(0),
									pgLtrTmp };
							mapPages.put(Integer.parseInt(list.get(c)[0]), ary1);
						}
						if (c == 0) {

							String[] ary1 = { "pg=" + list.get(c)[1],
									"pg#=" + nlp.getAllMatchedGroups(list.get(c)[1], NLP.patternPageNumbers).get(0),
									"pgLtr=",

									"sIdx=", "pg=", "pg#=", pgLtrTmp };
							mapPages.put(Integer.parseInt(list.get(c)[0]), ary1);

						}
					}
				}
			}
		}

		// NLP.printMapIntStringAry("mapPages", mapPages);
		// throw away any pg# where next is less than 300 - this will throw out TOC pg#
		// also - which is fine!

		String pgLtr = "", pgLtrPrior = "", pgNoStr = "", pgNoStrPrior = "", pg = "", pgPrior = "";
		int pgNo = -1, pgNoPrior = -1, pgLtrVal = -1, pgLtrValPrior = -1, sIdxPrior = 0, sIdx = 0, cnt3 = 0, cnt2 = 0,
				consecutiveCnt = 0, consecutiveCntPrior = 0;
		cnt = 0;
//		System.out.println("mapPages.size=" + mapPages.size());
		listTextExhMarkers = new ArrayList<>();
		for (Map.Entry<Integer, String[]> entry : mapPages.entrySet()) {
			cnt++;
			cnt3++;
			// if prior pgNo distance less than 300 - continue;
			sIdxPrior = sIdx;
			sIdx = entry.getKey();
			if (sIdxPrior == 0)
				continue;
			// if(sIdx-sIdxPrior<100 || cnt==1)
			// continue;
			// cnt2++;
			pgPrior = pg;
			pg = entry.getValue()[0];
			pgLtrPrior = pgLtr;
			pgLtr = entry.getValue()[2];
			pgNoPrior = pgNo;
			pgNoStr = entry.getValue()[1].trim();

			if (pgNoStr.replaceAll("[\\d]+", "").replace("pg#=", "").length() == 0) {
				pgNo = Integer.parseInt(pgNoStr.replace("pg#=", ""));
			} else {
				continue;
			}

			if (pgLtr.equals("pgLtr=") && pgNo - pgNoPrior != 1) {
				consecutiveCnt = 0;
				continue;
			}

			if (/* cnt2 == 1 || */pgNo == 1 || !pgLtr.equals(pgLtrPrior)) {
				// System.out.println("sIdx=" + sIdxPrior + " " + entry.getValue()[0] + " " +
				// entry.getValue()[1] + " "
				// + entry.getValue()[2] + " " + "consecutiveCnt=" + consecutiveCnt);

				// System.out.println("1. full ary="+Arrays.toString(entry.getValue()));
				String[] ary = { sIdxPrior + "", pg, "pg#=" + pgNo, pgLtr, entry.getValue()[5], entry.getValue()[6],
						"consecutiveCnt=" + consecutiveCnt };
				listTextExhMarkers.add(ary);
				// System.out.println("1. add exhPg="+Arrays.toString(ary));
				consecutiveCntPrior = consecutiveCnt;
				consecutiveCnt = 0;
			}

			if (cnt3 == mapPages.size()) {
				if (pgNo - pgNoPrior == 1) {
					consecutiveCnt++;
				}
				String[] ary = { sIdxPrior + "", pg, "pg#=" + pgNo, pgLtr, entry.getValue()[5], entry.getValue()[6],
						"consecutiveCnt=" + consecutiveCnt };
				// System.out.println("2. add exhPg full="+Arrays.toString(entry.getValue()));
				// System.out.println("2.add ary="+Arrays.toString(ary));
				listTextExhMarkers.add(ary);
			}

			if (pgNo - pgNoPrior == 1) {
				// System.out.println("pgNo="+pgNo+" pgNoPrior="+pgNoPrior+"
				// consecCnt="+consecutiveCnt+" "+ entry.getValue()[0]);
				consecutiveCnt++;
			}
			if (pgNo - pgNoPrior != 1) {
				// System.out.println("!pgNo="+pgNo+" pgNoPrior="+pgNoPrior+"
				// consecCnt="+consecutiveCnt+" "+ entry.getValue()[0]);
				consecutiveCnt = 1;
			}

		}
		// System.out.println("cnt3="+cnt3);
		// NLP.printListOfStringArray("initial=", listTextExhMarkers);

		// [sIdx=405267, pg=A-1, pg#=1, pgLtr=A, pg#Prior=, pgLtr=, consecutive pg Cnt
		// prior to here=144]|
		// below shows that sIdx of B-1 is 435789 which is the end of pg18 of exh pg ltr
		// A (A-18).
		// [435789, pg=B-1, pg#=1, pgLtr=B, pg#=18, pgLtr=A, consecutiveCnt=18]|
		// [441766, pg=B-4, pg#=4, pgLtr=B, pg#=3, pgLtr=B, consecutiveCnt=4]|
		return listTextExhMarkers;

	}

	public static List<String[]> getExhibitNames(String text) throws IOException {

		// this is a purposefully conservative method to capture exhibits. It starts by
		// capturing exhibits that match to the table of contents and other filters. I
		// then run a getExhibitNamesLax (overcaptures) and I use the first start index
		// in lax version that occurs after the more conservative to get the end
		// location of the conservative index.

		// List<String[]> listExhPgNumbers =
		// validateExhibitPageNumbers(exhPageFilePath);
		List<String[]> listExhNameSidx = new ArrayList<>();
		NLP nlp = new NLP();

		List<String[]> listExhSidxPrefix = nlp.getAllStartIdxLocsAndMatchedGroups(text, NLP.patternExhibitPrefix);

		String exhName = "", exhNameTmp = "", exhNameStr = "", str = "", exhPref = "", exhPrefNext = "";
		double sIdx = 0, sIdxNext = 0, sIdxPrior = 0, textLen = text.length(), cntWords = 0, cntInitialCaps = 0,
				capRatio = 0.0;
		int matchExhNameInToc = 0, cnt = 0, cnt2 = 0, cnt3 = 0, cntBadPattern = 0, cntGoodPattern = 0,
				exhLastCharNmb = -1, exhLastCharPriorNmb = -1, exhLastCharNextNmb = -1, distNext = -1, distPrior = -1,
				exhPrefsIdx = -1, numberOfHardReturnsNext = -1, numberOfHardReturnsPrior = -1, minHardReturns = 11;

		int sIdxExh = 0, eIdxExh = 0, eIdxExhPg1 = 0, eIdxExhPg1LessSidxExh = 0, sIdxExhLessExhPrefSidx = 0,
				exhPgNumber = 0, exhPgNumberPrior = 0;

		String textAftExhPref = "", nextExhName = "", textBefExhPref = "", snip = "", snip2 = "", exhPrefLastChar = "",
				exhPrefLastCharNext = "", exhPrefLastCharPrior = "", exhPgNmbLtr = "", exhPgNmbLtrPrior = "";

		boolean formOf = false, exhPrefInTOC = false, exhPgNoMatched = false;
		// boolean notExh = false;
//		System.out.println("listExhSidxPrefix.size=" + listExhSidxPrefix.size());
		// see - exhName.replaceAll below - it must have same replacement

		String textTmp = text.substring(0, (int) (textLen * .15));

		// get end location of exhibits in the TOC - then use that as the place after to
		// hunt for exhibits.
		List<String[]> listExhPreSidxToc = nlp.getAllStartIdxLocsAndMatchedGroups(textTmp, NLP.patternExhibitPref);
		System.out.println("listExhPreSidxToc.size()=" + listExhPreSidxToc.size());
		int idxFirst = 0;
		boolean idxFirstFound = false;
		int sIdxToc = 0, sidxTocPrior = 0, sidxTocNext = 0;
//		List<Integer[]> listSidxEidxExhibitPgNmb = new ArrayList<>();
		if (listExhPreSidxToc.size() > 1) {
			System.out.println("start listExhPreSidxToc.size()=" + listExhPreSidxToc.size());
			for (int c = 0; c < listExhPreSidxToc.size(); c++) {
				sidxTocPrior = sIdxToc;
				sIdxToc = Integer.parseInt(listExhPreSidxToc.get(c)[0]);
				if (nlp.getAllMatchedGroups(listExhPreSidxToc.get(c)[1],
						Pattern.compile("(?ism)Exhibit.{1,3}(4|10)\\.\\d.{0,3}[\r\n ]{1}")).size() > 0
						|| sIdxToc < 1000) {
					continue;
				}

				if (listExhPreSidxToc.size() > c + 1) {
					sidxTocNext = Integer.parseInt(listExhPreSidxToc.get(c + 1)[0]);
				}

				if (sIdxToc > textTmp.length())
					continue;
				// System.out.println("exhPref==" + listExhPreSidxToc.get(c)[1]);
				// System.out.println("sIdxTOC=" + sIdxToc+" sIdxTocPrior="+sidxTocPrior);
				if (c > 0 && sIdxToc < (int) textLen && sIdxToc - sidxTocPrior > 250) {
					break;
				}

				// System.out.println("idxFirstFound="+idxFirstFound+" "
				// + " sidxTocNext="+sidxTocNext+" sIdxToc="+sIdxToc);
				if (!idxFirstFound && c + 1 < listExhPreSidxToc.size() && sidxTocNext - sIdxToc < 250) {
					idxFirst = sIdxToc;
					// System.out.println("sidxTocNext="+sidxTocNext+" idxFirst/sIdxToc="+sIdxToc);
					idxFirstFound = true;
				}
				// System.out.println("listExhPreSidxToc.get(c)="+Arrays.toString(listExhPreSidxToc.get(c)));

			}

			// System.out.println("last SidxTocPrior="+sidxTocPrior+ "textlen="+textLen);

		}

//		System.out.println("end listExhPreSidxToc.size()=" + listExhPreSidxToc.size());
//		NLP.printListOfStringArray("listExhPreSidxToc==", listExhPreSidxToc);
		textTmp = text.substring(idxFirst, Math.min(text.length(), sidxTocPrior + 350));
		// System.out.println("textTmpToc snip-start="+textTmp.substring(0,300));
		// System.out.println("textTmpToc snip-end="
		// +textTmp.substring(Math.max(0, textTmp.length()-300),textTmp.length()));
		// System.out.println("textTmp="+textTmp+"||END");
		textTmp = textTmp.replaceAll("[\\{\\}\\[\\%\\]\\*\\)\\( \r\n\t]+", "").toLowerCase();

		String exhPrefPgNmb = "", exhPrefPgNmbType = "", exhPrefPgNmbTypePrior = "";
		int pgNmbVal = 0, pgNmbValProir = 0, pgNmbCnt = 0, pgNmbBadCnt = 0;

		for (int i = 0; i < listExhSidxPrefix.size(); i++) {
			formOf = false;
			exhPrefInTOC = false;
			// can't have exhibit prefix end with a '.'
			exhPrefsIdx = Integer.parseInt(listExhSidxPrefix.get(i)[0]);
			System.out.println("1 exhPrefsIdx=" + exhPrefsIdx);

			if (nlp.getAllMatchedGroups(listExhSidxPrefix.get(i)[1], Pattern.compile("(EXHIBIT|APPENDIX|SCHEDULE|ANNEX"
					+ "|Exhibit|Appendix|Schedule|Annex) (TO|To|to) " + "|(Exhibit (4|10)\\.)(.{0,3}\r\n)"))
					.size() > 0) {
				System.out.println("continuing exhPref=" + listExhSidxPrefix.get(i)[1]);
				continue;
			}

			sIdxPrior = sIdx;
			sIdx = Double.parseDouble(listExhSidxPrefix.get(i)[0]);
			if ((int) sIdx < 300 + sidxTocPrior) {
				System.out.println("continue.....listExhSidxPrefix=" + Arrays.toString(listExhSidxPrefix.get(i)));
				continue;
			}
			System.out.println("2 listExhSidxPrefix=" + Arrays.toString(listExhSidxPrefix.get(i)));
			// System.out.println("sIdx="+sIdx+" sidxTocPrior="+sidxTocPrior);
			distPrior = (int) sIdx - (int) sIdxPrior;

			if (i > 0) {
				numberOfHardReturnsPrior = nlp
						.getAllIndexEndLocations(text.substring((int) sIdxPrior, (int) sIdx), Pattern.compile("[\r\n]"))
						.size();
			}

			if (1 + i < listExhSidxPrefix.size()) {

				sIdxNext = Double.parseDouble(listExhSidxPrefix.get(i + 1)[0]);
				exhPrefNext = listExhSidxPrefix.get(i + 1)[1];
				exhPrefLastCharNext = exhPrefNext.substring(exhPrefNext.trim().length() - 1,
						exhPrefNext.trim().length());
				exhLastCharNextNmb = NLP.convertExhibitAlphaParaNumb(exhPrefLastCharNext);
				distNext = (int) sIdxNext - (int) sIdx;
				numberOfHardReturnsNext = nlp
						.getAllIndexEndLocations(text.substring((int) sIdx, (int) sIdxNext), Pattern.compile("[\r\n]"))
						.size();
			}

			exhPrefLastCharPrior = exhPrefLastChar;
			exhLastCharPriorNmb = exhLastCharNmb;

			exhPref = listExhSidxPrefix.get(i)[1];
			exhPrefLastChar = exhPref.substring(exhPref.trim().length() - 1, exhPref.trim().length());
			exhLastCharNmb = NLP.convertExhibitAlphaParaNumb(exhPrefLastChar);

			if (nlp.getAllIndexEndLocations(exhPrefLastChar, Pattern.compile("\\d")).size() > 0
					&& Integer.parseInt(exhPrefLastChar) > 0) {

				exhLastCharNmb = Integer.parseInt(exhPrefLastChar);

			}

			if (nlp.getAllIndexEndLocations(exhPrefLastCharNext, Pattern.compile("\\d")).size() > 0
					&& Integer.parseInt(exhPrefLastCharNext) > 0) {

				exhLastCharNextNmb = Integer.parseInt(exhPrefLastCharNext);

			}

			if (nlp.getAllIndexEndLocations(exhPrefLastCharPrior, Pattern.compile("\\d")).size() > 0
					&& Integer.parseInt(exhPrefLastCharPrior) > 0) {

				exhLastCharPriorNmb = Integer.parseInt(exhPrefLastCharPrior);

			}

			/*
			 * Pattern patternPageNumbers = Pattern.compile( // have to go in order of
			 * longest to shortest page number type - B-1-2 before // B-2
			 * NLP.patternADash1Dash1PgNo.pattern()// B-1-2 (pgno=2) + "|" +
			 * NLP.patternADash1PgNo.pattern() + "|" + NLP.patternA1PgNo.pattern() + "|" +
			 * NLP.patternPgNoSimplePgNo.pattern() + "|" + NLP.patterniPgNo.pattern());
			 * 
			 * Pattern patternPageLtrs = Pattern.compile( // have to go in order of longest
			 * to shortest page number type - B-1-2 before // B-2
			 * NLP.patternADash1Dash1Ltr.pattern()// B-1-2 (pgno=2) + "|" +
			 * NLP.patternADash1Ltr.pattern() + "|" + NLP.patternA1Ltr.pattern());
			 */

			String exhPgLtrNmb = "", exhPgLtrNmbPrior = "", exhPgLtr = "", exhPgLtrNext = "", exhPgLtrPrior = "",
					exhPg = "";
			exhPgNoMatched = false;

			/*
			 * if cur pgLtr is 1 less than nextPgLtr then nextSidx is Eidx. if cur pgLtr =
			 * nextPgLtr and consecCnt is = or 1 less than next pg ltr # then next sIdx is
			 * eidx. See: listTextExhMarkers.get(c)=[A-1, 395603,
			 * prior=pgLtr=,consecutiveCnt=144] listTextExhMarkers.get(c)=[B-1, 424218,
			 * prior=pgLtr=A,consecutiveCnt=18] . listTextExhMarkers.get(c)=[B-4, 430552,
			 * prior=pgLtr=B, consecutiveCnt=4] HOLD OFF - exhPref is best way to find eIdx
			 * I think - then this can be used as a check potentially
			 */

			// List<String[]> listExhPgSidxEidxPgPriorPgLtrConsecCnt = new ArrayList<>();

			NLP.printListOfStringArray("listTextExhMarkers==", listTextExhMarkers);
			for (int c = 0; c < SolrPrep.listTextExhMarkers.size(); c++) {
				System.out.println("fnl exhPgs=" + Arrays.toString(SolrPrep.listTextExhMarkers.get(c)));
				sIdxExh = Integer.parseInt(SolrPrep.listTextExhMarkers.get(c)[1]);
				exhPg = listTextExhMarkers.get(c)[0];
				if (nlp.getAllMatchedGroups(exhPg, NLP.patternPageLtrs).size() > 0)
					exhPgLtr = nlp.getAllMatchedGroups(exhPg, NLP.patternPageLtrs).get(0);
				exhPgNumber = Integer
						.parseInt(nlp.getAllMatchedGroups(listTextExhMarkers.get(c)[0], NLP.patternPageNumbers).get(0));
				if (exhPrefLastChar.equals(exhPgLtr) && sIdxExh - exhPrefsIdx < 25
						&& (exhPgNumber == 1 || exhPgNumber == 2)) {
					// if(c+1<SolrPrep.listTextExhMarkers.size()) {
					// exhPgLtrNext = listTextExhMarkers.get(c+1)[0];//apply pattern matcher to get
					// pg ltr then run conversion then subtract.
					// }
					// 8.. exhPref=EXHIBIT E words=
					System.out.println("\r\n" + "matched exPg with exhPref=" + exhPref + " ary="
							+ Arrays.toString(listTextExhMarkers.get(c)));

					exhPgNoMatched = true;
				}
			}
			// see below - these values can be matched against pgNos!
			System.out.println(
					"exhPref=" + exhPref + " exhPrefLastChar=" + exhPrefLastChar + " exhLastCharNmb=" + exhLastCharNmb);
			System.out.println(
					"exhPrefLastCharPrior=" + exhPrefLastCharPrior + " exhLastCharPriorNmb=" + exhLastCharPriorNmb);
			System.out.println("exhPrefNext=" + exhPrefNext + " exhPrefLastCharNext=" + exhPrefLastCharNext
					+ " exhLastCharNextNmb=" + exhLastCharNextNmb + " distPrior=" + distPrior + "distNext=" + distNext);
			// distNext,distPrior measure number of chars from existing to next and prior.
			// exhLastCharNmb is the converted last character of the exhibit (Exhibit A)
			// into a number, A=1
			// if exhibit prefixes are close together they are part of TOC and I skip
			// System.out.println("i=" + i + " text.len=" + text.length() + " sIdx=" + sIdx
			// + " sIdxNext=" + sIdxNext
			// + " sIdxPrior=" + sIdxPrior + " hard return.size next=" +
			// numberOfHardReturnsNext
			// + " hard return.size prior=" + numberOfHardReturnsPrior);

			// if few hard returns after exhibit - it can't be an exhibit or is blank. I
			// can't use this for number of returns prior b/c prior could have a large
			// exhibit. But I can count how many exhibit patters occur prior and if a lot if
			// it not an exhibit. See below re exh text prior.

			exhNameStr = text.substring((int) sIdx + exhPref.length(), Math.min(text.length(), (int) sIdx + 400));
			System.out.println("exhNameStr=" + exhNameStr);
			exhName = getExhibitName(exhNameStr, exhPref, exhPgNoMatched).trim();
			System.out.println("exhName=" + exhName);
			System.out.println("exhPref=" + exhPref);
			if (exhName.trim().length() < 5
					|| (exhName.length() < 17
							&& nlp.getAllMatchedGroups(exhName, Pattern.compile("(?i)EXHIBIT|APPENDIX|SCHEDULE|ANNEX"))
									.size() > 0)
					|| exhName.contains("ARTICLE") || exhName.contains("Article") || nlp
							.getAllMatchedGroups(exhName,
									Pattern.compile("(EXHIBIT|APPENDIX|SCHEDULE|ANNEX"
											+ "|Exhibit|Appendix|Schedule|Annex) [A-Za-z\\d\\-]{1,7}\\)?\\."))
							.size() > 0
					|| exhName.length() > 300

			) {
				System.out.println("continuing...3");

				continue;
			}

			if (nlp.getAllMatchedGroups(textTmp,
					Pattern.compile("(?ism)" + exhPref.replaceAll("[\\{\\}\\[\\%\\]\\*\\)\\( \r\n\t]+", "")))
					.size() > 0) {
				exhPrefInTOC = true;
			}

//			if (!exhPrefInTOC) {
//				System.out.println("exhPrefInTOC=" + exhPrefInTOC + " exhPref=" + exhPref);
//				continue; 
//			}

			// CLEARLY AN EXHIBIT - SO I INCLUDE THESE ON A MORE LAX BASIS.
			formOf = false;
			if (nlp.getAllIndexEndLocations(exhPref, Pattern.compile("EXHIBIT [A-Z]-?\\d?")).size() > 0
					&& nlp.getAllIndexEndLocations(exhName, Pattern.compile("(\\[?(Form|FORM|Face|FACE) [oOfF]{2})"))
							.size() > 0) {
				formOf = true;
			}

			matchExhNameInToc = 0;
			// System.out.println("exhName="+exhName);
			exhNameTmp = exhName.substring(0, exhName.length() - 1)
					.replaceAll("[\\{\\}\\[\\%\\]\\*\\)\\( \r\n\t\\\\]+", "").toLowerCase();
			// !!!!!<===if I change exhNameTmp repl - I must change in textTmp above.
			exhNameTmp = exhNameTmp.substring(0, Math.min(exhNameTmp.length(), 40));
			matchExhNameInToc = nlp.getAllMatchedGroups(textTmp, Pattern.compile("(?ism)" + exhNameTmp)).size();

			if (matchExhNameInToc < 1 && !formOf && !exhPgNoMatched) {
				System.out.println("continuing - matchExhNameInToc &&.... exhNameTmp=" + exhNameTmp);
				continue;
			}

			// formOf = false; - moved to correct place

			if (!exhPgNoMatched && matchExhNameInToc == 0 && formOf && i + 1 < listExhSidxPrefix.size()
					&& numberOfHardReturnsNext < minHardReturns && numberOfHardReturnsPrior < minHardReturns) {
				System.out.println("1aa. not exh. exhPref=" + exhPref + " exhName=" + exhName);
				System.out.println("continuing - exhPgNoMatched &&....");
				continue;
			}

			if (!exhPgNoMatched && matchExhNameInToc == 0 && formOf && i == 0
					&& numberOfHardReturnsNext < minHardReturns) {
				System.out.println("1ab1. not exh. exhPref=" + exhPref + " exhName=" + exhName);
				System.out.println("2 continuing - exhPgNoMatched &&....");
				continue;
			}

			if (!exhPgNoMatched && matchExhNameInToc == 0 && formOf && i + 1 < listExhSidxPrefix.size()
					&& (numberOfHardReturnsPrior >= minHardReturns && (distNext < 150 & distPrior < 150))) {
				System.out.println("1ab2. not exh=" + exhPref + " exhName=" + exhName);
				System.out.println("3 continuing - exhPgNoMatched &&....");
				continue;
			}

			if (!exhPgNoMatched
					&& nlp.getAllIndexEndLocations(exhPrefLastChar, Pattern.compile("(?sm)[\\.;\\:,]")).size() > 0
					|| nlp.getAllIndexEndLocations(exhPref, Pattern.compile("(?sm)(4|10)\\.[\\d]{1,3}")).size() > 0) {
				System.out.println("1ab3. not exh=" + exhPref + " exhName=" + exhName);
				System.out.println("4 continuing - exhPgNoMatched &&....");
				continue;
			}

			textBefExhPref = text.substring(Math.max((int) sIdx - 15, 0), (int) sIdx);
			// System.out.println("textBefExhPref="+textBefExhPref);
			String[] textBefExhPrefSpl = textBefExhPref.split("\r\n");

			// if any text prior to exhibit prefix but no hard return (b/c exh pre has to
			// start its own line)
			if (!exhPgNoMatched && textBefExhPrefSpl.length == 0
					&& textBefExhPref.replaceAll("[^a-zA-Z]", "").length() > 0) {
//				 System.out.println("1c. not exh textBefExhPref=" + textBefExhPref);
				System.out.println("1c. not exh. exhPref" + exhPref + " exhName=" + exhName);
				System.out.println("5 continuing - exhPgNoMatched &&....");
				continue;
			}

			textBefExhPref = text.substring(Math.max((int) sIdx - 400, 0), (int) sIdx);
			// if too many exhibit patterns prior then it can't an exhibit.
			if (i + 1 == listExhSidxPrefix.size()) {
				if (!exhPgNoMatched
						&& nlp.getAllIndexEndLocations(textBefExhPref, NLP.patternExhibitPrefix).size() > 2) {
					// System.out.println("1d. not exh textBefExhPref=" + textBefExhPref);
					System.out.println("1d. not exh. exhPref" + exhPref + " exhName=" + exhName);
					System.out.println("6 continuing - exhPgNoMatched &&....");
					continue;
				}
			}

			textAftExhPref = text.substring((int) sIdx, Math.min((int) sIdx + 400, text.length()));
			// if too many exhibit patterns after exhibit then it can't be an exhibit.
			if (!exhPgNoMatched && i + 1 < listExhSidxPrefix.size()) {
				if (nlp.getAllIndexEndLocations(textAftExhPref, NLP.patternExhibitPrefix).size() > 2) {
					// System.out.println("1e. not exh textBefExhPref=" + textBefExhPref);
					System.out.println("1e. not exh. exhPref" + exhPref + " exhName=" + exhName);
					System.out.println("7 continuing - exhPgNoMatched &&....");
					continue;
				}
			}

			// if any text prior to exhibit prefix not separated by and on same line = f/p
			textBefExhPref = text.substring(Math.max((int) sIdx - 2, 0), (int) sIdx);
			if (!exhPgNoMatched && textBefExhPref.replaceAll("[\r\n \t]+", "").length() != 0) {
				// System.out.println("textBefExh="+textBefExhPref);
				// System.out.println("1f. not exh textBefExhPref=" +
				// textBefExhPrefSpl[textBefExhPrefSpl.length - 1]);
				System.out.println("1f. not exh. exhPref" + exhPref + " exhName=" + exhName);
				System.out.println("8 continuing - exhPgNoMatched &&....");
				continue;
			}

			cntGoodPattern = 0;
			cntBadPattern = 0;

			cntBadPattern = nlp.getAllMatchedGroups(exhPref,
					Pattern.compile("(?sm)(Exhibit |EXHIBIT |APPENDIX |Appendix |SCHEDULE |Schedule |Annex \"\r\n"
							+ "			+ \"|ANNEX )(OF|of|Of)"))
					.size();

			cntBadPattern = cntBadPattern + nlp
					.getAllMatchedGroups(exhName,
							Pattern.compile(
									"(?ism)" + "[ \r\n](attention|If to the.{1,14}|signed|dated|as of|addresse?d?):"))
					.size();

			if (cntBadPattern > 0) {
				System.out.println("1g. not exh. exhPref" + exhPref + " exhName=" + exhName);
				exhName = "";
				System.out.println("cntBadPattern!....");
			}

			if (exhName.length() > 0) {
				String[] ary = { exhPref + " " + exhName, (int) sIdx + "" };
				System.out.println("I found the exhibit .add" + Arrays.toString(ary));
				listExhNameSidx.add(ary);
				// if exhPgNoMatched - add as an additoinal variable in array.
				// I also need to add eIdx for exhPgNoMached!
			}
		}

		NLP.printListOfStringArray("bef lax exhnames=", listExhNameSidx);

		// next scrub is to determine when not to have an exhibit b/c it is too long

		List<String[]> listExhNameSidxLax = SolrPrep.getExhibitNamesLax(text);
		// I use the lax getExhibitNamesLax method to overcapture exhibits. When one of
		// these occur after but before the next non-lax exhibit found in this method -
		// then the exhibit name is blank until the next good exhibit is found.

		List<String[]> listExhNameSidxFinal = new ArrayList<>();

		// exhPgNoMached needs to be used as a check to ignore lax
		boolean added = false;
		int sIdx2 = 0, eIdx2 = 0, sIdx2Lax = 0, n = 0;
		for (int i = 0; i < listExhNameSidx.size(); i++) {
			sIdx2 = Integer.parseInt(listExhNameSidx.get(i)[1]);
			if (i + 1 == listExhNameSidx.size()) {
				eIdx2 = sIdx2 + listExhNameSidx.get(i)[1].length();
			}
			if (i + 1 < listExhNameSidx.size()) {
				eIdx2 = Integer.parseInt(listExhNameSidx.get(i + 1)[1]);
			}

			for (; n < listExhNameSidxLax.size(); n++) {
				sIdx2Lax = Integer.parseInt(listExhNameSidxLax.get(n)[1]);
				if (sIdx2Lax > sIdx2 && sIdx2Lax < eIdx2) {
					String[] ary = { listExhNameSidx.get(i)[0], sIdx2 + "" };
					// System.out.println("1 added to final. ary="+Arrays.toString(ary));
					listExhNameSidxFinal.add(ary);
					String[] ary2 = { "", sIdx2Lax + "" };
					System.out.println("2 added blank to final. ary=" + Arrays.toString(ary2));
					listExhNameSidxFinal.add(ary2);
					added = true;
					break;
				}
			}

			if (!added) {
				listExhNameSidxFinal.add(listExhNameSidx.get(i));
				System.out.println("3 added to final. ary=" + Arrays.toString(listExhNameSidx.get(i)));
			}

			added = false;
		}

		// return listExhNameSidxFinal;

		System.out.println("listExhNameSidxFinal.size==" + listExhNameSidxFinal.size());
		NLP.printListOfStringArray("listExhNameSidxFinal==", listExhNameSidxFinal);
		List<String[]> listExhNameSidxExhibitsAndRelatedSchedules = new ArrayList<>();
		String exhibitAndLtrNext = "", exhibitAndLtrPrior = "", exhibitAndLtr = "", exhibitAndLtrParent = "",
				parentExhibitName = "";
		cnt = 0;
		for (int i = 0; i + 1 < listExhNameSidxFinal.size(); i++) {
			// if i==0 automatically append
			if (i == 0) {
				listExhNameSidxExhibitsAndRelatedSchedules.add(listExhNameSidxFinal.get(i));
				// System.out.println("1 add first
				// ary="+Arrays.toString(listExhNameSidxFinal.get(i)));
			}
			exhName = listExhNameSidxFinal.get(i)[0].trim();
			nextExhName = listExhNameSidxFinal.get(i + 1)[0].trim();
			System.out.println("exhName=" + exhName);
			List<String> listE = nlp.getAllMatchedGroups(exhName,
					Pattern.compile("(?sm)(EXHIBIT|Exhibit) [A-Z]{1,3}-?[\\d]{0,3}-?[\\d]{0,3} "));
			List<String> listEnext = nlp.getAllMatchedGroups(nextExhName,
					Pattern.compile("(?sm)(EXHIBIT|Exhibit) [A-Z]{1,3}-?[\\d]{0,3}-?[\\d]{0,3} "));
			// System.out.println("listeNext.size="+listEnext.size());
			// System.out.println("listE.size="+listE.size());
			if (listEnext.size() == 1) {
				exhibitAndLtrNext = listEnext.get(0).trim();
				System.out.println("exhibitAndLtrNext==" + exhibitAndLtrNext);
			}

			if (listE.size() == 1) {
				exhibitAndLtrPrior = exhibitAndLtr;
				exhibitAndLtr = listE.get(0).trim();
				// System.out.println("exhibitAndLtr=="+exhibitAndLtr);
			}

			if (listE.size() > 0 && !exhibitAndLtr.equals(exhibitAndLtrPrior)) {
				parentExhibitName = listExhNameSidxFinal.get(i)[0];
				exhibitAndLtrParent = listE.get(0).trim();
			}
			// if sub exhibit E repeats I need to retain the parentExhibit E. This occurs
			// when prior ExhLtr!=cur
			// System.out.println("exhibitAndLtrPrior="+exhibitAndLtrPrior+"
			// exhibitAndLtr="+exhibitAndLtr+" exhibitAndLtrNext="+exhibitAndLtrNext+
			// " parentExhibitName="+parentExhibitName+" nextExhName="+nextExhName);
			if (exhibitAndLtrNext.equals(exhibitAndLtrParent) && !nextExhName.equals(parentExhibitName)
					&& !parentExhibitName.equals(listExhNameSidxFinal.get(i + 1)[1])) {

				// set exhibit E first to precede each exhibit E thereafter (exhibit of an
				// exhibit). If true boolean = true
				String[] ary = { "parentExhibitName=" + parentExhibitName + "subExhibitName=" + nextExhName,
						listExhNameSidxFinal.get(i + 1)[1] };
				listExhNameSidxExhibitsAndRelatedSchedules.add(ary);
				System.out.println("1 add parentExh to ary next=" + Arrays.toString(ary));
				System.out.println("exhName =" + exhName);
				System.out.println("nextExhName=" + nextExhName);

				// if(true then parentExhibitNameappend next with sub-exhibit (exh of exh)
			}

			else {
				System.out.println("2 add ary next=" + Arrays.toString(listExhNameSidxFinal.get(i + 1)));
				listExhNameSidxExhibitsAndRelatedSchedules.add(listExhNameSidxFinal.get(i + 1));
			}
		}

//		System.out.println("listExhNameSidxExhibitsAndRelatedSchedules.size=="
//				+ listExhNameSidxExhibitsAndRelatedSchedules.size());

//		if (listExhNameSidxExhibitsAndRelatedSchedules.size() > 0)
//			listExhNameSidxExhibitsAndRelatedSchedules.add(listExhNameSidxFinal.get(listExhNameSidxFinal.size() - 1));

		NLP.printListOfStringArray("listExhNameSidxExhibitsAndRelatedSchedules=",
				listExhNameSidxExhibitsAndRelatedSchedules);

		return listExhNameSidxExhibitsAndRelatedSchedules;

	}

	public static String getExhibitName(String exhibitText, String exhPref, boolean exhPgNoMatched) throws IOException {

		NLP nlp = new NLP();
		StringBuilder sb = new StringBuilder();

		int cntWords = 0, cntInitialCaps = 0, cnt = 0, dist = 70;
		double capRatio = 0.0;
		boolean allCaps = false, stop = false;
		// System.out.println("exhibitText="+exhibitText+"|End");
		String exhNameLastChar = "", exhNameLine = "";

		String str = "", word = "";
		exhibitText = exhibitText.replaceAll("[\r\n]+", "\r\n").trim();
		String[] exhNameLines = exhibitText.split("\r\n");

		String lineNext = "", lineNext2Ahead = "", lineNext3Ahead = "", lineNext4Ahead = "";

		allCaps = false;
		if (exhNameLines.length == 0)
			return "";

		exhNameLine = exhNameLines[0].trim();
		if (exhNameLine.length() == 0)
			return "";

		if (nlp.getAllIndexEndLocations(StopWords.removeStopWords(exhNameLine), Pattern.compile("(^| )[a-z]"))
				.size() > 0) {
			System.out.println("no exhName found");
			return "";
		}

		sb.append(exhNameLine);

		// grab up to 2 lines after exh name if they are all caps and less than chars 90
		// in len

		if (exhNameLines.length < 3)
			return sb.toString();

		lineNext = exhNameLines[1].trim();
//		System.out.println("lineNext.len=" + lineNext.length() + " lineNext=" + lineNext);
		if (
//				nlp.getAllIndexEndLocations(StopWords.removeStopWords(lineNext), Pattern.compile("(^| )[a-z]")).size() == 0
//				&& 
		lineNext.length() < (int) ((double) dist * .8)
				&& lineNext.length() == lineNext.replaceAll("[a-z]+", "").length()) {
			sb.append(" " + lineNext);
//			System.out.println("2nd line appending ==" + lineNext);
		} else {
			stop = true;
		}

		if (exhNameLines.length < 4)
			return sb.toString();

//		System.out.println("lineNext2Ahead=" + lineNext2Ahead);
		if (!stop) {
			lineNext2Ahead = exhNameLines[2].trim();
			if (
//					nlp.getAllIndexEndLocations(StopWords.removeStopWords(lineNext2Ahead), Pattern.compile("(^| )[a-z]")).size() == 0 && 
			lineNext2Ahead.length() < (int) ((double) dist * .7)
					&& lineNext2Ahead.length() == lineNext2Ahead.replaceAll("[a-z]+", "").length()) {
				sb.append(" " + lineNext2Ahead);
//				System.out.println("3rd line appending ==" + lineNext);
			}
		}

		return sb.toString();

	}

	@SuppressWarnings("resource")
	public static String getExhibits(String text) throws IOException {

		// File file = new File("e:/getContracts/temp/exh1.txt");
		// PrintWriter pw = new PrintWriter(file);

		StringBuilder sb = new StringBuilder();
		List<String[]> listExhSidx = new ArrayList<>();

		listExhSidx = getExhibitNames(text);

		if (listExhSidx.size() == 0)
			return text;

		String str = "", exhibitName = "";
		int sIdx = 0, eIdx = 0;
		for (int i = 0; i < listExhSidx.size(); i++) {
			sIdx = Integer.parseInt(listExhSidx.get(i)[1]);
			exhibitName = listExhSidx.get(i)[0];
			if (i + 1 < listExhSidx.size()) {
				eIdx = Integer.parseInt(listExhSidx.get(i + 1)[1]);
			}
			if (i + 1 == listExhSidx.size()) {
				eIdx = text.length();
			}

			if (i == 0) {
				str = text.substring(0, sIdx);
				sb.append(str);
				// pw.append("\r\nsIdx=" + 0 + " eIdx=" + sIdx + " i=" + i + " AA.||" + str +
				// "||END");
				// System.out.println("1 sIdx=" + sIdx + " eIdx=" + eIdx);
			}
			// System.out.println("2 sIdx=" + sIdx + " eIdx=" + eIdx);
			str = text.substring(sIdx, eIdx);

			sb.append("<eT><eN>" + exhibitName + "</eN>" + str + "</eT>");
			// pw.append("\r\nsIdx=" + sIdx + " eIdx=" + eIdx + " i=" + i + "
			// AA.||\r\n\r\n\r\n\r\n<eT><eN>\r\n\r\n\r\n"
			// + exhibitName + "\r\n\r\n\r\n</eN>\r\n\r\n\r\n" + str +
			// "\r\n\r\n\r\n</eT>\r\n\r\n\r\n|||END");
		}

		// pw.close();

		// System.out.println("text.len="+text.length()+" eIdx="+eIdx);
		// listExh.add(text.substring(eIdx,text.length())+"end");

		return sb.toString();

	}

	public static List<String> getSections(String text) throws IOException {
		// this method can be deleted once solrprep is refactored.

		NLP nlp = new NLP();
		List<String> listSec = new ArrayList<>();
		// String text = "";
		// for(int i=0; i<list.size(); i++) {
		// text = list.get(i);

		SolrPrep.listPrelimIdxAndSectionRestrictive = ContractNLP.getAllIdxLocsAndMatchedGroups(text,
				ContractParser.patternSectionHeadingRestrictive);

		// System.out.println("2 listPrelimIdxAndSectionRestrictive.size="
		// + listPrelimIdxAndSectionRestrictive.size());

		List<String[]> listPrelimIdxAndSectionLessRestrictiveWithNumberSectionAlso = ContractNLP
				.getAllIdxLocsAndMatchedGroups(text,
						ContractParser.patternSectionHeadingLessRestrictiveWithNumberSectionAlso);

//		 NLP.printListOfStringArray("less restrictive=",listPrelimIdxAndSectionLessRestrictiveWithNumberSectionAlso);

		List<String[]> patternSectionHeadingLessRestrictiveWithoutNumber = ContractNLP
				.getAllIdxLocsAndMatchedGroups(text, ContractParser.patternSectionHeadingLessRestrictiveWithoutNumber);
		// System.out.println("3 textSidxEidx.len="+textSidxEidx.length());

//		 NLP.printListOfStringArrayInReverse("patternSectionHeadingLessRestrictiveWithoutNumber=",patternSectionHeadingLessRestrictiveWithoutNumber);

		if (listPrelimIdxAndSectionLessRestrictiveWithNumberSectionAlso.size()
				* 1.3 < patternSectionHeadingLessRestrictiveWithoutNumber.size()) {

			// if many section headings with 'Section' are found than there
			// shouldn't be a section numbering format w/o 'Section' in the
			// format of the heading.

			SolrPrep.listPrelimIdxAndSectionLessRestrictive = patternSectionHeadingLessRestrictiveWithoutNumber;

		} else {
			SolrPrep.listPrelimIdxAndSectionLessRestrictive = listPrelimIdxAndSectionLessRestrictiveWithNumberSectionAlso;
		}

		// System.out.println("4 textSidxEidx.len="+textSidxEidx.length());

		List<String[]> listSidxEidxTypeSecHdgTmp = getSectionIdxsAndNames(text);
		NLP.printListOfStringArray("1 getSectionIdxsAndNamesTmp=", listSidxEidxTypeSecHdgTmp);

		// getDefidxAndDefinition MUST come after getSectionIdxsAndNames!!
		// b/c it uses its idx loc of sec hdgs.

		List<String[]> listSidxEidxTypeSecHdgs = new ArrayList<>();
		String secHdg = "", secHdgNext = "", secHdgPrior = "";
		for (int i = 0; i < listSidxEidxTypeSecHdgTmp.size(); i++) {
			secHdg = listSidxEidxTypeSecHdgTmp.get(i)[3];
			// System.out.println("Z. secHdg="+secHdg);
			if (i + 1 < listSidxEidxTypeSecHdgTmp.size()) {
				secHdgNext = listSidxEidxTypeSecHdgTmp.get(i + 1)[3];
			}

			if (i + 1 == listSidxEidxTypeSecHdgTmp.size() && listSidxEidxTypeSecHdgTmp.size() > 1) {
				secHdgPrior = listSidxEidxTypeSecHdgTmp.get(i - 1)[3];
			}

			if ((secHdgNext.equals(secHdg) && i + 1 < listSidxEidxTypeSecHdgTmp.size())
					|| (i + 1 == listSidxEidxTypeSecHdgTmp.size() && secHdgPrior.equals(secHdg))) {
				continue;
			}

			// System.out.println("listSidxEidxTypeSecHdgTmp - .add=
			// " + Arrays.toString(listSidxEidxTypeSecHdgTmp.get(i)));
			listSidxEidxTypeSecHdgs.add(listSidxEidxTypeSecHdgTmp.get(i));

		}

//		NLP.printListOfStringArray("2 getSectionIdxsAndNamesTmp=", listSidxEidxTypeSecHdgTmp);

		if (null == SolrPrep.listPrelimIdxAndSectionLessRestrictive
				|| SolrPrep.listPrelimIdxAndSectionLessRestrictive.size() == 0) {
//			listSec.add("\r\n<sT><sN></sN>\r\n" + text + "\r\n</sT>\r\n");// <===can be deleted eventually
//			NLP.printListOfString("1 final listSec=", listSec);
			String[] ary = { "sIdx=" + 0, "sectionStart" };
			mapIdx.put((double) 0, ary);
			String[] ary2 = { "eIdx=" + text.length() + "", "sectionEnd" };
			mapIdx.put((double) (text.length() - .01), ary2);

			return listSec;
		}

		// System.out.println("sec list.size="+listSidxEidxTypeSecHdgs2.size());
		String sectionName = "", sectionBody = "";
		int sIdx = 0, sIdxPrior = 0, eIdx = 0, eIdxPrior = 0, cnt = 0, eIdxNext = 0;
		sIdx = 0;
		eIdx = 0;
		eIdxPrior = 0;
		boolean eIdxBad = false;
		for (int n = 0; n < listSidxEidxTypeSecHdgs.size(); n++) {
			// System.out.println("a
			// listSidxEidxTypeSecHdgs="+Arrays.toString(listSidxEidxTypeSecHdgs.get(n)));
			sIdxPrior = sIdx;
			sIdx = Integer.parseInt(listSidxEidxTypeSecHdgs.get(n)[0]);
			eIdxPrior = eIdx;
			if (n + 1 < listSidxEidxTypeSecHdgs.size()) {
				eIdxNext = Integer.parseInt(listSidxEidxTypeSecHdgs.get(n + 1)[1]);
			}
			eIdx = Integer.parseInt(listSidxEidxTypeSecHdgs.get(n)[1]);
			sectionName = listSidxEidxTypeSecHdgs.get(n)[3];
			// System.out.println("eIdx=" + eIdx + " eIdxNext=" + eIdxNext + " sIdx=" + sIdx
			// + " sidxPrior=" + sIdxPrior
			// + "\r\nsectionName=" + sectionName);
			if (eIdx > eIdxNext) {
				// System.out.println("continue");
				continue;
			}

			// System.out.println("1 prelim sectionName=="+sectionName);

			// 263720, 377938 - if eIdxPrior>sIdx then set eIdxPrior to cur eIdx
			// correct where sectionBody is split eg -- relating to the Certifi || ates and
			// servi -- I check ending of current text and see if ends in [a-z]{1} and if
			// next sIdx starts with [a-z]{1} and no ws betw just hard returns. If so eIdx =
			// n+1 and then I reset n=n+1

			// System.out.println("sectionName...=" + sectionName);
			if (n == 0 && sIdx > 0) {
				// eventually can be deleted====>
				sectionBody = text.substring(0, sIdx);// <===can be deleted eventually.
				listSec.add("<sT><sN></sN>" + sectionBody + "</sT>");// <===can be deleted eventually.
				// <====eventually can be deleted
				String[] ary = { "sIdx=0", "sectionStart" };
				mapIdx.put((double) 0, ary);
				String[] ary2 = { "eIdx=" + sIdx, "sectionEnd" };
				mapIdx.put((double) sIdx - .01, ary2);
			}

			if (n > 0 && sIdx != eIdxPrior && sIdx > eIdxPrior) {
				// System.out.println("sectionBody sIdx>eIdxPrior sIdx=="+eIdxPrior+"
				// sIdx>eIdxPrior eIdx="+sIdx);
				// eventually can be deleted====>
				sectionBody = text.substring(eIdxPrior, sIdx);
				listSec.add("<sT><sN></sN>" + sectionBody + "</sT>");
				sectionBody = text.substring(0, sIdx);
				listSec.add("<sT><sN></sN>" + sectionBody + "</sT>");
				// <====eventually can be deleted

				String[] ary = { "sIdx=" + eIdxPrior, "sectionStart" };
				mapIdx.put((double) eIdxPrior, ary);
				String[] ary2 = { "eIdx=" + sIdx + "", "sectionEnd" };
				mapIdx.put((double) sIdx - .01, ary2);

			}

			// undue?
			// if (n > 0 && sIdx != eIdxPrior && sIdx < eIdxPrior) {
			//// System.out.println("continue. sIdx="+sIdx+" priorEidx="+eIdxPrior);
			// continue;
			// }
			sectionBody = text.substring(sIdx, eIdx);
			// System.out.println("2 prelim sectionName=="+sectionName);
			if (sectionName.length() > 100) {
				// System.out.println("sectionName=" + sectionName);
				if (nlp.getAllIndexEndLocations(sectionName,
						Pattern.compile("(?sm)[a-z]{1}\\.(?=  ?([A-Z]{1}|\\([a-z]{1}\\)))")).size() > 0) {
					int idx = nlp.getAllIndexEndLocations(sectionName,
							Pattern.compile("(?sm)[a-z]{1}\\.(?=  ?([A-Z]{1}|\\([a-z]{1}\\)))")).get(0);
					sectionName = sectionName.substring(0, idx);
					// System.out.println("cut sectionName=" + sectionName.substring(0,
					// Math.min(100,sectionName.length())));
				}
			}

			// System.out.println(".add sectionName="+sectionName);
			listSec.add("<sT><sN>" + sectionName + "</sN>" + sectionBody + "</sT>");

			String[] ary = { "sIdx=" + sIdx, "sectionStart" };
			mapIdx.put((double) sIdx, ary);
			String[] ary1a = { "sIdx=" + sIdx, "sectionName=" + sectionName };
			mapIdx.put(((double) sIdx), ary1a);
			String[] ary2 = { "eIdx=" + eIdx + "", "sectionEnd" };
			mapIdx.put((double) eIdx - .01, ary2);

			if (1 + n == listSidxEidxTypeSecHdgs.size() && eIdx < text.length()) {
				sectionBody = text.substring(eIdx, text.length());
				listSec.add("<sT><sN>" + "" + "</sN>" + sectionBody + "</sT>");
				String[] ary3 = { "sIdx=" + eIdx, "sectionStart" };
				mapIdx.put((double) sIdx, ary3);
				String[] ary4 = { "eIdx=" + text.length() + "", "sectionEnd" };
				mapIdx.put((double) eIdx - .01, ary4);
			}
		}

		if (listSec.size() == 0) {
			listSec.add("<sT><sN>" + "" + "</sN>" + text + "</sT>");
			// ensure doc is marked so I can loop through <sT> and </sT> for paras etc
//			NLP.printListOfString("2 final listSec=", listSec);

			String[] ary = { "sIdx=" + 0, "sectionStart" };
			mapIdx.put((double) sIdx, ary);
			String[] ary2 = { "eIdx=" + text.length() + "", "sectionEnd" };
			mapIdx.put((double) text.length() - .01, ary2);

			return listSec;
		}

//		NLP.printListOfString("3 final listSec=", listSec);
		return listSec;

	}

	public static List<String[]> getSectionIdxsAndNames(String text) throws IOException {

//		System.out.println("getSectionIdxsAndNames");
		// SolrPrep.secDif = 0.0;
		// SolrPrep.artDif = 0.0;

		long startTime = System.currentTimeMillis();

		List<String[]> listIdxAndSectionLessRestrictive = SolrPrep
				.getRidOfTocSecHdgs(SolrPrep.listPrelimIdxAndSectionLessRestrictive, text);
//		NLP.printListOfStringArray("aft get rid of TOC hdgs sections==", listIdxAndSectionLessRestrictive);
		long endTime = System.currentTimeMillis();
//		System.out.println("getRidOfTocSecHdgs 6 seconds = " + (endTime - startTime) / 1000);
//		startTime = System.currentTimeMillis();

		List<String[]> listIdxAndSection = SolrPrep.checkList(listIdxAndSectionLessRestrictive);
//		NLP.printListOfStringArray("checkList listIdxAndSection==", listIdxAndSection);

//		endTime = System.currentTimeMillis();
//		System.out.println("7 sections checkList seconds = " + (endTime - startTime) / 1000);
//		startTime = System.currentTimeMillis();

//		NLP.printListOfStringArray("listIdxAndSection raw?=", listIdxAndSection);
		listIdxAndSection = SolrPrep.validateSecHdgList(listIdxAndSection, text);
//		endTime = System.currentTimeMillis();
//		System.out.println("8 validate sections seconds = " + (endTime - startTime) / 1000);
//		startTime = System.currentTimeMillis();

//		NLP.printListOfStringArray("listIdxAndSection?==", listIdxAndSection);

		List<String[]> listStartSectionStartIdxAndHdg = SolrPrep.formatSecHdgsList(listIdxAndSection, text);
//		endTime = System.currentTimeMillis();
//		System.out.println("9 format sec seconds = " + (endTime - startTime) / 1000);

//		NLP.printListOfStringArray("formatSecHdgsList==", listStartSectionStartIdxAndHdg);

//		return listIdxAndSection;

		return listStartSectionStartIdxAndHdg;

	}

	public static String getPartsOfSpeech(String text) throws IOException {

		NLP nlp = new NLP();

		Properties props = new Properties();
		props.setProperty("annotators", "tokenize" + ", ssplit" + ", pos"
		// + ", lemma"
		// +", parse"
		// +", sentiment"
		// //, dcoref, ner"
		);

//		System.out.println("stanford?");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		Annotation annotation = new Annotation(text);
		// run all the selected Annotators on this text
		pipeline.annotate(annotation);
		// this prints out the results of sentence analysis to file(s) in good formats
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		pipeline.prettyPrint(annotation, baos);
		// String print = baos.toString();

		baos.reset();
		pipeline.xmlPrint(annotation, baos);
		String xmlPOS = baos.toString();

		List<String> listPOS = nlp.getAllMatchedGroups(xmlPOS, Pattern.compile("(?sm)(?<=<POS>).*?(?=</POS>)"));
		List<String> listWord = nlp.getAllMatchedGroups(xmlPOS, Pattern.compile("(?sm)(?<=<word>).*?(?=</word>)"));
//		List<String[]> listPosWord = new ArrayList<>();
		String pos, word;

		// System.out.println("xmlPOS.len="+xmlPOS.length()+"
		// listPOS.size="+listPOS.size());
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < listPOS.size(); i++) {
			pos = listPOS.get(i);
			word = listWord.get(i);
			// if (nlp.getAllIndexStartLocations(pos,
			// Pattern.compile("VB|CC|FW|JJ|LS")).size()>0) {

			sb.append("|" + word + " " + pos);
			// System.out.println("word=" + word + " POS=" + pos);

			// }
		}

		sb.append("|");

		return sb.toString();
	}

	public static String getParagraphs(List<String> list) throws IOException {
		System.out.println("get paragraphs");
		NLP nlp = new NLP();

		Properties props = new Properties();
		props.setProperty("annotators", "tokenize" + ", ssplit"
		// +", parse"
		// +", sentiment"
		// //, dcoref, ner"
		);

		// List<String> listPara = new ArrayList<>();

		String text = "",
				// definedTerm = "",
				para = "",
				// para2 = "",
				sectionName = "";
		// int sectionNameLength = 0, paraLength = 0,wordCountSection = 0;
		// double initialCapsCnt = 0, wordCountPara = 0;

		// boolean gotIt = false;
		StringBuilder sb = new StringBuilder();
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
//		boolean tmp = false;
		for (int i = 0; i < list.size(); i++) {

			text = list.get(i).replaceAll("\\.  ?", "xZxZ").replaceAll("Regulation S", "Regulation_S")
					.replaceAll("\\. ?\r\n ?(?=[A-Z]{1})", "aZaZ").replaceAll("\\.\" ", "yZyZ")
					.replaceAll("(?<=\\d)\\.  ?(?=[\\dA-Za-z]{1})", "bZbZ");

			// delete
			// if (text.contains(
			// "ther nominee of the Depositary or to a successor thereto or a nominee of
			// such successor thereto")) {
			// System.out.println("section text=" + text.substring(0, 500)+"|END");
			// tmp = true;
			// }
			// <==delete

			Annotation annotation = new Annotation(text);
			pipeline.annotate(annotation);
			List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
			for (CoreMap sentence : sentences) {
				// <pt>=paragragh text
				para = sentence.toString();

				// delete
				// if(tmp) {
				// System.out.println("1 each para is -- start="+para.substring(0,
				// Math.min(para.length(), 100))+"|END");
				// System.out.println("1 each para is --
				// end="+para.substring(Math.max(para.length()-100,0), para.length())+"|END");
				// }

				para = para.replaceAll("Regulation_S", "Regulation S").replaceAll("aZaZ", "\\. \r\n")
						.replaceAll("xZxZ|bZbZ", "\\. ").replaceAll("yZyZ", "\\.\" ")
						.replaceAll("(?<=[a-z\\),\\d\"]{1})\r\n(?=[A-Za-z\"]{1})", " ");

				// delete
				// if(tmp) {
				// System.out.println("2 each para is -- start="+para.substring(0,
				// Math.min(para.length(), 200))+"|END");
				// System.out.println("2 each para is --
				// end="+para.substring(Math.max(para.length()-200,0), para.length())+"|END");
				// }

				para = "<pT>" + para + "</pT>";
				sb.append(para);
			}
		}

		return sb.toString();

	}

	public static List<String> getSentencesStanfordParser(String text) throws IOException {
//		System.out.println("getSentencesStanfordParser");
		NLP nlp = new NLP();

		Properties props = new Properties();
		props.setProperty("annotators", "tokenize" + ", ssplit"
		// +", parse"
		// +", sentiment"
		// //, dcoref, ner"
		);

		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		String sent = "";
		Annotation annotation = new Annotation(text);
		pipeline.annotate(annotation);
		List<String> listSentences = new ArrayList<>();
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			listSentences.add(sentence.toString());
		}

		return listSentences;

	}

	public static List<String[]> validateDefinitions(List<String[]> listDef, String text) throws IOException {

		// find where there is likely 2 definitions contained in 1 defined term
		// heading and break the second part away w/o a heading. The purpose is
		// to ensure accuracy even if I break away a section that should be
		// included. Would rather under include text in a defined term heading
		// than over-include. Over inclusion will produce f/p search results in
		// solr where under-inclusion will not produce results but given there
		// will be a large DB of files this shouldn't be an issue.

		NLP nlp = new NLP();
		Pattern patternDefSplit = Pattern.compile("[\r\n]{3,}[ ]{1,}(?=\")");
		String defText = "";
		int sIdx = 0, eIdx = 0, priorEidx = 0, priorEidx2Behind = 0, priorEidx3Behind = 0, priorEidx4Behind = 0,
				nextEidx = 0, nextEidx2Ahead = 0, nextEidx3Ahead = 0, nextEidx4Ahead = 0, loc = 0;

		if (null == listDef || listDef.size() == 0) {
			return listSidxEidxTypeDefHdgs;

		}

		List<String[]> listTmpFinal = new ArrayList<>();

		for (int i = 0; i < listDef.size(); i++) {
			sIdx = Integer.parseInt(listDef.get(i)[0]);
			eIdx = Integer.parseInt(listDef.get(i)[1]);
			defText = text.substring(sIdx, eIdx);

			List<Integer> list = nlp.getAllIndexEndLocations(defText, patternDefSplit);
			// if this pattern is found - split def into 2 parts. part 1 has def
			// heading, part 2 (ary2) has none.
			if (list.size() > 0) {
				loc = list.get(0);
				String ary[] = { listDef.get(i)[0], (sIdx + loc) + "", listDef.get(i)[2],
						listDef.get(i)[3].replaceAll("(?sm)The (defined )?term (''|\")?", "") };
				// NLP.pwNLP.append(NLP.println("break into 2 this def", defText + "|END"));
				listTmpFinal.add(ary);
				// NLP.pwNLP.append(NLP.println("1st part def", Arrays.toString(ary) + "|END"));
				String ary2[] = { (sIdx + loc) + "", listDef.get(i)[1], listDef.get(i)[2], "" };
				listTmpFinal.add(ary2);
				// NLP.pwNLP.append(NLP.println("1st part def", Arrays.toString(ary2) +
				// "|END"));
			}

			else {
				// the space b/w defs are at least 10
				String[] ary = { listDef.get(i)[0], listDef.get(i)[1], listDef.get(i)[2],
						listDef.get(i)[3].replaceAll("(?sm)The (defined )?term (''|\")?", "") };
				listTmpFinal.add(ary);
			}
		}
//		NLP.printListOfStringArray("validate. listTmpFinal==", listTmpFinal);

		boolean outOfOrderNext = false, outOfOrderNext2Ahead = false, outOfOrderNext3Ahead = false,
				outOfOrderNext4Ahead = false, outOfOrderPrior = false, outOfOrder2Prior = false,
				outOfOrder3Prior = false, outOfOrder4Prior = false;

		// if a def is out of order - then def prior and after will still be in order
		// and priorEidx=sIdx and eIdx=nextSidx. In that case I mark defHdg as
		// '&&ERROR: defined term is out of Order'is out of order', then before I append
		// to xml file use an if then statement (don't replace global field) and use
		// either error or def txt field.

		sIdx = 0;
		eIdx = 0;
		int dif = 0, ltr = -1, nextLtr = -1, nextLtr2ahead = -1, nextLtr3ahead = -1, nextLtr4ahead = -1, priorLtr = -1,
				priorLtr2behind = -1, priorLtr3behind = -1, priorLtr4behind = -1, cnt = -1, dist = 10000, nextSidx = 0,
				nextSidx2Ahead = 0, nextSidx3Ahead = 0, nextSidx4Ahead = 0, priorSidx = 0, priorSidx2Behind = 0,
				priorSidx3Behind = 0, priorSidx4Behind = 0, cntConsecDefTermsInOrder = 0, iLeft = 8;
		double textLen = text.length(), listSize = listTmpFinal.size();

		List<String[]> listFinal = new ArrayList<String[]>();
		for (int i = 0; i < listTmpFinal.size(); i++) {
//			System.out.println("i=="+i+" listTmpFinal="+Arrays.toString(listTmpFinal.get(i)));
			outOfOrderNext = false;
			outOfOrderNext2Ahead = false;
			outOfOrderNext3Ahead = false;
			outOfOrderNext4Ahead = false;
			outOfOrderPrior = false;
			outOfOrder2Prior = false;
			outOfOrder3Prior = false;
			outOfOrder4Prior = false;
			ltr = -1;
			nextLtr = -1;
			nextLtr2ahead = -1;
			nextLtr3ahead = -1;
			nextLtr4ahead = -1;
			priorLtr = -1;
			priorLtr2behind = -1;
			priorLtr3behind = -1;
			priorLtr4behind = -1;
			nextEidx = 0;
			nextEidx2Ahead = 0;
			nextEidx3Ahead = 0;
			nextEidx4Ahead = 0;
			priorEidx = 0;
			priorEidx2Behind = 0;
			priorEidx3Behind = 0;
			priorEidx4Behind = 0;
			nextSidx = 0;
			nextSidx2Ahead = 0;
			nextSidx3Ahead = 0;
			nextSidx4Ahead = 0;
			priorSidx = 0;
			priorSidx2Behind = 0;
			priorSidx3Behind = 0;
			priorSidx4Behind = 0;

			cnt = 0;
			sIdx = Integer.parseInt(listTmpFinal.get(i)[0]);
			eIdx = Integer.parseInt(listTmpFinal.get(i)[1]);
//			if (listTmpFinal.get(i)[3].trim().length() < 2
//					|| listTmpFinal.get(i)[3].replaceAll("[^a-z]", "").length() < 4)
//				continue;
			// fixed for alphabetizer 2/2022.

			ltr = nlp.convertAlphaParaNumbDefinedTerms(listTmpFinal.get(i)[3].trim().substring(0, 1));

			// set values here - see //END
			if (i + 1 < listSize && listTmpFinal.get(i + 1)[3].trim().length() > 0) {
				nextLtr = nlp.convertAlphaParaNumbDefinedTerms(listTmpFinal.get(i + 1)[3].trim().substring(0, 1));
				nextEidx = Integer.parseInt(listTmpFinal.get(i + 1)[1]);

			}

			if (i + 2 < listSize && listTmpFinal.get(i + 2)[3].trim().length() > 0) {
				nextLtr2ahead = nlp.convertAlphaParaNumbDefinedTerms(listTmpFinal.get(i + 2)[3].trim().substring(0, 1));
				nextEidx2Ahead = Integer.parseInt(listTmpFinal.get(i + 2)[1]);
			}

			if (i + 3 < listSize && listTmpFinal.get(i + 3)[3].trim().length() > 0) {
				nextLtr3ahead = nlp.convertAlphaParaNumbDefinedTerms(listTmpFinal.get(i + 3)[3].trim().substring(0, 1));
				nextEidx3Ahead = Integer.parseInt(listTmpFinal.get(i + 3)[1]);
			}

			if (i + 4 < listSize && listTmpFinal.get(i + 4)[3].trim().length() > 0) {
				nextLtr4ahead = nlp.convertAlphaParaNumbDefinedTerms(listTmpFinal.get(i + 4)[3].trim().substring(0, 1));
				nextEidx4Ahead = Integer.parseInt(listTmpFinal.get(i + 4)[1]);
			}

			if (i - 1 >= 0 && listTmpFinal.get(i - 1)[3].trim().length() > 0) {
				priorLtr = nlp.convertAlphaParaNumbDefinedTerms(listTmpFinal.get(i - 1)[3].trim().substring(0, 1));
				priorEidx = Integer.parseInt(listTmpFinal.get(i - 1)[1]);
			}

			if (i - 2 >= 0 && listTmpFinal.get(i - 2)[3].trim().length() > 0) {
				priorLtr2behind = nlp
						.convertAlphaParaNumbDefinedTerms(listTmpFinal.get(i - 2)[3].trim().substring(0, 1));
				priorEidx2Behind = Integer.parseInt(listTmpFinal.get(i - 2)[1]);
			}

			if (i - 3 >= 0 && listTmpFinal.get(i - 3)[3].trim().length() > 0) {
				priorLtr3behind = nlp
						.convertAlphaParaNumbDefinedTerms(listTmpFinal.get(i - 3)[3].trim().substring(0, 1));
				priorEidx2Behind = Integer.parseInt(listTmpFinal.get(i - 3)[1]);
			}

			if (i - 4 >= 0 && listTmpFinal.get(i - 4)[3].trim().length() > 0) {
				priorLtr3behind = nlp
						.convertAlphaParaNumbDefinedTerms(listTmpFinal.get(i - 4)[3].trim().substring(0, 1));
				priorEidx2Behind = Integer.parseInt(listTmpFinal.get(i - 4)[1]);
			}

			// END

			/*
			 * NOTE: this is setup not to stop until list nears end. NOTE **** don't add to
			 * list (list.add()) until all 'continue' statements have been run that don't
			 * add to list. Last group of continues first add to list. Whenever there is a
			 * continue reset cntConsecDefTermsInOrder to 0. If -1 then it is at end of list
			 * and there is no next.
			 */

//			if (listSize - i <= iLeft && ltr > nextLtr && nextLtr != -1
//					&& nlp.getAllMatchedGroups(text.substring(sIdx, Math.min(sIdx + 100, text.length())),
//							Pattern.compile("\".{0,35} (means?|meaning)[, ]")).size() == 0) {
				// added jul 14, 2022 nlp.get...
				/*
				 * if there are less than 8 (iLeft) remaining potential defined terms and
				 * existing ltr is > next then return. Better to be conservative and end early
				 * even if drafting error caused me not to get other defined terms. Note this
				 * allows me to go to the next list b/c only occurs if most of defined terms in
				 * list is finished
				 */

//				System.out.println("1a@ validate - returned list early=" + Arrays.toString(listTmpFinal.get(i)));
//				NLP.printListOfStringArray("1. final validated def list=", listFinal);
//				return listFinal;
//
//			}

			if (i + 1 == listSize && ltr < priorLtr
					&& nlp.getAllMatchedGroups(text.substring(sIdx, Math.min(sIdx + 100, text.length())),
							Pattern.compile("\".{0,10} (means|meaning)[, ]")).size() == 0) {// added jul 14, 2022
																							// nlp.get...) {
				/*
				 * if the are less than 2 potential def terms remaining and existing
				 * ltr<priorLtr then return list
				 */

//				System.out.println("1b@ validate - listSize - i <= iLeft && ltr > nextLtr --returned stubbed list="
//						+ Arrays.toString(listTmpFinal.get(i)));
				NLP.printListOfStringArray("2. final validated def list=", listFinal);
				return listFinal;
			}

			if ((eIdx - sIdx > dist && !listTmpFinal.get(i)[3].toLowerCase().contains("permitted"))
					|| ((double) (eIdx - sIdx)) / textLen > .2 || (listSize - i < iLeft && priorEidx - sIdx > .5 * dist)
					|| (listSize - i < iLeft && eIdx - sIdx > .5 * dist)

			) {
				/*
				 * if def terms are separated by more than 20 percent of entire doc length -
				 * then skip. Or if a defined term is more than 20 percent of entire doc len -
				 * then skip. Or if a defined term has a length more than the distance.
				 */
//				System.out.println("2@a validate - continue >.2|>dist=" + Arrays.toString(listTmpFinal.get(i)));
				cntConsecDefTermsInOrder = 0;
				continue;
			}

			if ((ltr < priorLtr || ltr > nextLtr) && (double) (priorEidx - sIdx) >= .3 * (double) dist) {
				// if defined terms are in following order due to drafting error: a,b,c,d,s,e,f.
				// BUT dist is large - continue;
				cntConsecDefTermsInOrder = 0;
//				 System.out.println("2@b validate - ltr<priorLtr || ltr>nextLtr continue &&distance is large=" + Arrays.toString(listTmpFinal.get(i)));
				continue;
			}

			if ((ltr < priorLtr && ltr > nextLtr) && (double) (priorEidx - sIdx) < .3 * (double) dist) {
				// if defined terms are in following order due to drafting error: a,b,c,d,s,e,f.
				// then dist is small - so add and - continue;
				eIdx = GetContracts.getLastDefinedTermSimple(listTmpFinal.get(i), text);
				String[] ary = { listTmpFinal.get(i)[0], eIdx + "", listTmpFinal.get(i)[2], listTmpFinal.get(i)[3],
						"Out of Order" };
				listFinal.add(ary);
				cntConsecDefTermsInOrder = 0; // set to zero b/c I know next is out of order
//				 System.out.println("2@c validate - ltr<priorLtr || ltr>nextLtr continue &&	 but dist small - last def - outOfOrder but still.add="
//				 + Arrays.toString(ary));
				continue;
			}

			// these are intended to capture 90 percent (i+4,i+3, i>=3, i>=2)
			if (i >= 3 && ltr >= priorLtr && priorLtr >= priorLtr2behind && priorLtr2behind >= priorLtr3behind
					&& priorLtr3behind >= priorLtr4behind) {
				/*
				 * if dist from eIdx to nextSidx is large then use last def for def
				 */
				listFinal.add(listTmpFinal.get(i));
				cntConsecDefTermsInOrder++; // set to zero b/c I know next is out of order
//				 System.out.println("3@a validate - i>=3 .add=" +Arrays.toString(listTmpFinal.get(i)));
				continue;
			}

			if (i + 4 < listSize && ltr <= nextLtr && nextLtr <= nextLtr2ahead && nextLtr2ahead <= nextLtr3ahead
					&& nextLtr3ahead <= nextLtr4ahead) {
				/*
				 * if dist from eIdx to nextSidx is large then use last def for def
				 */
				listFinal.add(listTmpFinal.get(i));
				cntConsecDefTermsInOrder++; // set to zero b/c I know next is out of order
//				 System.out.println("3@b validate - i+4<listSize .add=" +Arrays.toString(listTmpFinal.get(i)));
				continue;
			}

			if (i >= 2 && ltr >= priorLtr && priorLtr >= priorLtr2behind && priorLtr2behind >= priorLtr3behind) {
				/*
				 * if dist from eIdx to nextSidx is large then use last def for def
				 */
				listFinal.add(listTmpFinal.get(i));
				cntConsecDefTermsInOrder++; // set to zero b/c I know next is out of order
//				 System.out.println("3@c validate - i>=3 =" + Arrays.toString(listTmpFinal.get(i)));
				continue;

			}

			if (i + 3 < listSize && ltr <= nextLtr && nextLtr <= nextLtr2ahead && nextLtr2ahead <= nextLtr3ahead) {
				/*
				 * if dist from eIdx to nextSidx is large then use last def for def
				 */
				listFinal.add(listTmpFinal.get(i));
				cntConsecDefTermsInOrder++; // set to zero b/c I know next is out of order
//				 System.out.println("3@d validate - i+4<listSize .add=" +Arrays.toString(listTmpFinal.get(i)));
				continue;
			}

			if (i >= 1 && ltr >= priorLtr && priorLtr >= priorLtr2behind) {
				/*
				 * if dist from eIdx to nextSidx is large then use last def for def
				 */
				listFinal.add(listTmpFinal.get(i));
				cntConsecDefTermsInOrder++; // set to zero b/c I know next is out of order
//				 System.out.println("3@e validate - i>=3 =" +Arrays.toString(listTmpFinal.get(i)));
				continue;
			}

			if (i + 2 < listSize && ltr <= nextLtr && nextLtr <= nextLtr2ahead) {
				/*
				 * if dist from eIdx to nextSidx is large then use last def for def
				 */
				listFinal.add(listTmpFinal.get(i));
				cntConsecDefTermsInOrder++; // set to zero b/c I know next is out of order
//				System.out.println("3@f validate - i+4<listSize .add=" + Arrays.toString(listTmpFinal.get(i)));
				continue;
			}

			if (i + 2 == listSize && (ltr > nextLtr)) {
				/*
				 * if ltr<priorLtr - then should have stopped at prior. So I say if nextLtr<ltr
				 * then return
				 */
				eIdx = GetContracts.getLastDefinedTermSimple(listTmpFinal.get(i), text);
				String[] ary = { listTmpFinal.get(i)[0], eIdx + "", listTmpFinal.get(i)[2], listTmpFinal.get(i)[3] };
//				 System.out.println("4@ validate - i+2=listSize - last def .add. return list=" +
//				 Arrays.toString(listTmpFinal.get(i))+" next==="+Arrays.toString(listTmpFinal.get(i+1)));
				listFinal.add(ary);
//				 NLP.printListOfStringArray("3. final validated def list=", listFinal);

				return listFinal;
			}

			if (i + 1 == listSize && ltr >= priorLtr) {
				eIdx = GetContracts.getLastDefinedTermSimple(listTmpFinal.get(i), text);
				String[] ary = { listTmpFinal.get(i)[0], eIdx + "", listTmpFinal.get(i)[2], listTmpFinal.get(i)[3] };
				listFinal.add(ary);
//				 System.out.println("5@ validate - i+2=listSize - last def added - return and.add=" + Arrays.toString(listTmpFinal.get(i)));
//				 NLP.printListOfStringArray("4. final validated def list=", listFinal);
				// add back

				return listFinal;
			}

			if (nlp.getAllMatchedGroups(text.substring(sIdx, Math.min(sIdx + 100, text.length())),
					Pattern.compile("\".{0,20} (means?|meaning)[, ]")).size() > 0 && eIdx - sIdx < 2000) {
				String[] ary = { listTmpFinal.get(i)[0], eIdx + "", listTmpFinal.get(i)[2], listTmpFinal.get(i)[3] };
				listFinal.add(ary);
//				 System.out.println("6@ validate - i+2=listSize - has shall mean...=" + Arrays.toString(listTmpFinal.get(i)));
				continue;
			}
			
//			System.out.println("no match at all???");

		}

		return listFinal;

	}

	public static String getNumeralNumberedSubSentencesInSentence(String sentence, int sentNo) {
		// List<String> listOfText = new ArrayList<>();
		// NLP nlp = new NLP();

//		System.out.println("getNumeralNumberedSubSentencesInSentence ssssss");
		sentence = sentence.replaceAll("(?<=\\([a-zA-Z]{1,6}\\) ?)" + "([\r\n]{1,4})(?= ?[a-z]{1})", " ")
				.replaceAll("[ ]+", " ");
		// System.out.println("aa. sentence=="+sentence);
		sentence = sentence.replaceAll("(?<=Section [\\d\\.]{1,7}) (?=\\([a-zA-Z]{1}\\))", "");
		sentence = sentence.replaceAll("(?ism)" + "(\\()"
		// $1==>xxOp
				+ "([a-zA-Z\\d]{1,7})"
				// $2
				+ "(\\))" +
				// $3=>CPxx
				"(\\]?)"
				// $4
				+ " (business|through|largest|smallest|months?|days?)"
		// space $5
				, "xxOP$2CPxx$4 $5");
		// NLP.pwNLP.append(NLP.println("1aa ssssss - sentence==", sentence));

		// paragraphs (1), (2), (3) or (7)
		sentence = sentence.replaceAll("(?ism)"
				// $1==>xxOp
				+ "(\\()"
				// $2
				+ "([a-zA-Z\\d]{1,7})"
				// $3==>CPxx
				+ "(\\))" +
				// $4
				"(, | or | and )" +
				// $5
				"(\\()" +
				// $6
				"([a-zA-Z\\d]{1,7})" +
				// $7 CPxx
				"(\\))", "xxOP$2CPxx$4xxOP$6CPxx");

//		sentence = sentence.replaceAll("(Section |Clause |Paragraph |section |clause |paragraph )"
//				+ "(\\()([a-z\\d]{1,7})(\\))" + "([\r\n \t]{1})", "$1xxOP$3CPxx$5");

		sentence = sentence.replaceAll("(Section |Clause |Paragraph |section |clause |paragraph )"
				+ "(\\()([A-Za-z\\d]{1,7})(\\))" + "([\r\n \t]{1})", "$1xxOP$3CPxx$5");

		sentence = sentence.replaceAll("(through )" + "(\\()([A-Za-z\\d]{1,7})(\\))" + "([\r\n \t]{1})",
				"$1xxOP$3CPxx$5");

		Pattern patternClause1 = Pattern.compile("(?ism)( |^)(\\([a-zA-Z\\d]{1,7}\\))" + "((\\([a-zA-Z\\d]{1,7}\\))?)");

		sentence = sentence.replaceAll(patternClause1.toString(), "$1sssS1s$2$3");
		// NLP.pwNLP.append(NLP.println("1a1 ssssss - sentence==", sentence));

		// too many s1s finds - so remove. this is okay.
		// clauses(i) or (ii), clauses (i) and (ii)
		sentence = sentence.replaceAll(
				"(sssS1s)(\\([a-zA-Z\\d]{1,7}\\) " + "?(and" + "|or) ?)(sssS1s)(\\([a-zA-Z\\d]{1,7}\\))", "$2$5");
		// System.out.println("1="+sentence+"||||");

		// NLP.pwNLP.append(NLP.println("1a2 ssssss - sentence==", sentence));

		sentence = sentence.replaceAll("(sssS1s)(\\([a-zA-Z\\d]{1,7}\\), )" + "(sssS1s)(\\([a-zA-Z\\d]{1,7}\\))",
				"$2$4");
		// System.out.println("2="+sentence+"||||");

		// NLP.pwNLP.append(NLP.println("1aa2 ssssss - sentence==", sentence));

		// subclauses (A), sssS1s(B).
		sentence = sentence.replaceAll("(\\([a-zA-Z\\d]{1,7}\\), ?)(sssS1s)" + "(\\([a-zA-Z\\d]{1,7}\\))", "$1$3");
		// NLP.pwNLP.append(NLP.println("1a3 ssssss - sentence==", sentence));
		// System.out.println("3="+sentence+"||||");

		sentence = sentence.replaceAll("(?ism)(sections? ?|clauses? ?|paragraphs? ?" + "|through ?)" + "sssS1s(\\()",
				"$1 $2");
		// in clauses (i) -sssS1s(iii)
		// NLP.pwNLP.append(NLP.println("1a4 ssssss - sentence==", sentence));
		// System.out.println("4="+sentence+"||||");

		sentence = sentence.replaceAll("(?ism)(sections|clauses?|paragraphs)" + "( \\([a-zA-Z\\d]{1,7}\\) ?-?)"
				+ "(through|thru)?(sssS1s)(\\([a-zA-Z\\d]{1,7}\\))", "$1$2$3$5");
		// NLP.pwNLP.append(NLP.println("1a5 ssssss - sentence==", sentence));
		// System.out.println("5="+sentence+"||||");

		sentence = sentence.replaceAll("(sssS1s)(\\([\\d]{1,2}\\))(,|;)( )", " $2$3$4");
		// NLP.pwNLP.append(NLP.println("1a6 ssssss - sentence==", sentence));

		// System.out.println("6="+sentence+"||||");
		// sssS1s(FRB) | sssS1s(OCC)
		sentence = sentence.replaceAll("(sssS1s)(\\([A-Z]{3,}\\))", "$2");
		// and the case of each of clause(
		// NLP.pwNLP.append(NLP.println("1b ssssss - sentence==", sentence));
		// System.out.println("7="+sentence+"||||");

		// gets eg - " ; provided"
		// sentence = sentence.replaceAll("(;)( [a-z]{3,15}[,;]{0,1} )", "sssS1s; $2");
		// NLP.pwNLP.append(NLP.println("1c ssssss - sentence==", sentence));
		// System.out.println("8="+sentence+"||||");

		int sIdx = 0, eIdx = 0;
		String clause = "";

		sentence = prepClause1(sentence);
		// NLP.pwNLP.append(NLP.println("1d ssssss - sentence==", sentence));
		// System.out.println("9="+sentence+"||||");

		// fix==> clause(i), (ii) and(iii)
		sentence = sentence.replaceAll(
				"(?ism)(clauses?|sections?" + "|paragraphs?|articles|and|or)(\\([a-zA-Z\\d]{1,7}\\))", "$1 $2");
		// System.out.println("10="+sentence+"||||");
		// fix==>clauses(i) orsssS1s(ii) and(iii)
		// NLP.pwNLP.append(NLP.println("1e ssssss - sentence==", sentence));
		sentence = sentence
				.replaceAll("(?ism)(sections? ?|paragraphs? ?|clauses? ?)(\\([a-zA-Z\\d]{1,7}\\) ?(and|or) ?)"
						+ "(sssS1s)(\\([a-zA-Z\\d]{1,7}\\))", "$1 $2 $5");
		// System.out.println("11="+sentence+"||||");

		sentence = sentence.replaceAll("(?ism)(sssS1s)(\\()([A-Za-z\\d]{1,5})(\\))" + "([ \r\n]{1,3}above)",
				"xxOP$3CPxx$5");
		// System.out.println("12="+sentence+"||||");
		sentence = sentence.replaceAll("(?ism)((in|under)[ \r\n]{1,3}clause )" + "(\\()([A-Za-z\\d]{1,5})(\\))",
				"$1xxOP$4CPxx");
		// System.out.println("13="+sentence+"||||");

		sentence = sentence.replaceAll("(?ism)(SECTION 10.06)(\\()([A-Za-z]{1})(\\))", "$1xxOP$3CPxx");
		sentence = sentence.replaceAll("(?ism)(section[ \r\n]{1,8})" + "(\\()" + "([A-Z]{1,4})" + "(\\))",
				"$1xxOP$3CPxx");

		sentence = sentence.replaceAll("xxOP[\r\n]{1,3}", "xxOP");
		sentence = sentence.replaceAll("(\r\n)([\\d\\.]{1,7}{1,5}\\%)", " $2");
		sentence = sentence.replaceAll("(?<=Section [\\d\\.]{1,7}) (?=\\([A-Za-z]{1}\\))", "");
//		sentence = sentence.replaceAll("\\(loss\\)", "xxOPlossCPxx");
//		 System.out.println("sentence check="+sentence+"||");

		return sentence;

	}

	public static String prepClause1(String sentence) {

//		System.out.println("prepClause1 ssssss");

		Pattern patternClause1 = Pattern
				.compile("(?ism) " + "(\\([a-zA-Z\\d]{1,7}\\))" + "((\\([a-zA-Z\\d]{1,7}\\)" + ")?)");

		sentence = sentence.replaceAll(patternClause1.toString(), "sssS1s$1$2");
		// .replaceAll("xxxsssS1s", "xxx");

		// NLP.pwNLP.append(NLP.println("1 patternClause1 sssS1s-sentence==",
		// sentence));

		// how to address this -- this is saying -- too many s1s finds - so
		// remove. this is okay.
		sentence = sentence.replaceAll("(sssS1s)(\\([a-zA-Z\\d]{1,7}\\) and ?)" + "(sssS1s)(\\([a-zA-Z\\d]{1,7}\\))",
				" $2$4");
		// subclauses (A), sssS1s(B). same as above
		// just gets rid of sss - so idx loc not changed.
		// NLP.pwNLP.append(NLP.println("2 patternClause1 sssS1s-sentence==",
		// sentence));
		sentence = sentence.replaceAll("(\\([a-zA-Z\\d]{1,7}\\), ?)(sssS1s)(\\([a-zA-Z\\d]{1,7}\\))", "$1$3");
		// NLP.pwNLP.append(NLP.println("3 patternClause1 sssS1s-sentence==",
		// sentence));

		// just gets rid of sss - so idx loc not changed.
		sentence = sentence.replaceAll("(?ism)(sections? ?|clauses? ?|paragraphs? ?|through ?)sssS1s(\\()", "$1$2");
		// in clauses (i) -sssS1s(iii)
		// just gets rid of sss - so idx loc not changed.
		// NLP.pwNLP.append(NLP.println("4 patternClause1 sssS1s-sentence==",
		// sentence));

		sentence = sentence.replaceAll("(?ism)(sections|clauses?|paragraphs)( \\([a-zA-Z\\d]{1,7}\\) ?-?)"
				+ "(through|thru)?(sssS1s)(\\([a-zA-Z\\d]{1,7}\\))", "$1$2$3$5");

		// NLP.pwNLP.append(NLP.println("5 patternClause1 sssS1s-sentence==",
		// sentence));

		sentence = sentence.replaceAll("(sssS1s)(\\([\\d]{1,2}\\))(,|;)( )", " $2$3$4");
		// NLP.pwNLP.append(NLP.println("6 patternClause1 sssS1s-sentence==",
		// sentence));

		// sssS1s(FRB) | sssS1s(OCC)
		sentence = sentence.replaceAll("(sssS1s)(\\([A-Z]{3,}\\))", "$2");

		return sentence;
	}

	public static List<String> getSentences(String text, String sectionHeading) throws IOException {
		double origTextLen = text.length();
		String origText = text;
		StringBuilder sbLen = new StringBuilder();

		int secHdgLen = sectionHeading.length();
		String sectionHeadingChgd = sectionHeading.replaceAll("[ ]+", "").trim();
		String matchSecHdg = text.substring(0, Math.min(text.length(), secHdgLen)).replaceAll("[ ]+", "").trim();
//		boolean secHdgFound = false;
		boolean tmp = false;
//		if (text.contains("CalculationX Agent")) {
//			System.out.println("text-----" + text+"|");
//			tmp = true;
//		}

		List<String[]> listSentEidx = new ArrayList<String[]>();
		NLP nlp = new NLP();

		text = text.replaceAll("(?sm)(?<=[a-z]{3},?)[\\r\\n]{3,10}(?= ?[a-z]{3,10})", " "
		// + "(?= ?[a-z]{3,40},? [a-z]{3,40},? )"," "
		);
		// System.out.println("text repl="+text);
		List<Integer> listSentenceEnd = nlp.getAllIndexEndLocations(text, NLP.patternSentenceEnd);
		List<Integer> listMorePatterns = new ArrayList<Integer>();
		List<String> list = new ArrayList<>();
		List<String> list2 = new ArrayList<>();
		// List<String[]> listSenEnd = nlp.getAllEndIdxAndMatchedGroupLocs(text,
		// NLP.patternSentenceEnd);

		String sentence = "", sentMore = "";
		int sIdx = 0, eIdx = 0;

		int tmpSidx = 0, tmpEidx = 0;

//		// delete
//		 if (tmp) {
//		 System.out.println("2sectionHeading="+sectionHeading);
//		 System.out.println("2secHdgFound text="+text+"||END");
//		 }

		if (listSentenceEnd.size() == 0) {
//			 System.out.println("semicolon ==0 .add");
//			listSentenceEnd.add(text.length());

			// delete
//			 if (tmp) {
//			 System.out.println("3sectionHeading="+sectionHeading);
//			 System.out.println("3secHdgFound text="+text+"||END");
//			 }

		}

		// if(listSentenceEnd.size()==0 && secHdgFound) {
		// System.out.println("semicolon ==0 .add");
		// listSentenceEnd.add(text.length());
		// }

		// if(listSentenceEnd.size()==0 && !secHdgFound) {
		// listSentenceEnd.add(text.length());
		// }

		// if sentence is less than 5 characters after replacing w/s then I need to
		// append that with next sentence. I can do that by continuing and adding last
		// sentence to current.
		String stub = "";
		for (int i = 0; i < listSentenceEnd.size(); i++) {
			// sIdx=0 at start (eIdx=0) and eIdx thereafter
			sIdx = eIdx;
			eIdx = listSentenceEnd.get(i);
			sentence = text.substring(sIdx, eIdx);

//			//0 delete
//			 if(tmp) {
//			 System.out.println("initial sent=="+sentence+"||END\r\n");
//			 System.out.println("initial sent.length="+sentence.length());
//			 System.out.println("stub.len="+stub.length());
//			 }

			if (stub.length() > 0) {
				sentence = stub + sentence;

//				// delete
//				if (tmp) {
//					System.out.println("stub + sent==" + sentence + "||END\r\n");
//				}

				stub = "";
			}

			// System.out.println("sentence right before repl="+sentence+"||");
			// System.out.println("\r\nsentence.len after
			// repl="+sentence.replaceAll("[\r\n\t \\s\\-\\_\\=]+", "").length());
			if (sentence.replaceAll("[\r\n\t \\s" + "\\-\\_\\=" + "]+", "").length() < 5) {
				stub = sentence;
				// System.out.println("stub="+stub+"||END\r\n");
				continue;
			}
			stub = "";
			// last ck to see if sentences were not captured. This will also
			// capture idx location in order to pair w/ appropriate def/sec/exh
			// fields.

			// System.out.println("@patternSent10 sent=="+sentence+"||END");
			listMorePatterns = nlp.getAllIndexEndLocations(sentence, NLP.patternSent10);
			// System.out.println("listMorePatterns.size()="+listMorePatterns.size());
			if (i + 1 < listSentenceEnd.size()) {
				if (listMorePatterns.size() > 0) {
					sentMore = "";
					tmpSidx = 0;
					tmpEidx = 0;
					for (int n = 0; n < listMorePatterns.size(); n++) {
						if (n == 0) {
							tmpSidx = 0;
							tmpEidx = 0;
						}
						tmpEidx = listMorePatterns.get(n);

						if (n + 1 < listMorePatterns.size()) {
							sentMore = sentence.substring(tmpSidx, tmpEidx);
							// use sIdx b/c that's prior eIdx.
							// yyy sentMore = sentence.replaceAll("[\r\n]+", " ").replaceAll("[ ]+", "
							// ").trim();
							/*
							 * don't do replacement - this removes format - text format can be used as a
							 * tool to better identify sentence and clause relationships.
							 */
							String[] arySentEidx = { sentMore, (sIdx + tmpEidx) + "" };
							listSentEidx.add(arySentEidx);
							// System.out.println("1a. sent .add ary=" + Arrays.toString(arySentEidx));
						}

						if (n + 1 == listMorePatterns.size())
						// else
						{
							sentMore = sentence.substring(tmpSidx, tmpEidx);
							String[] ary2SentEidx = { sentMore
									// yyy .replaceAll("[\r\n]+", " ").replaceAll("[ ]+", " ").trim()
									, (sIdx + sentMore.length()) + "" };
							listSentEidx.add(ary2SentEidx);
							// System.out.println("1b. sent .add ary=" + Arrays.toString(ary2SentEidx));

							sentMore = sentence.substring(tmpEidx, sentence.length());
							String[] ary3SentEidx = { sentMore
									// yyy .replaceAll("[\r\n]+", " ").replaceAll("[ ]+", " ").trim()
									, (sIdx + sentence.length()) + "" };
							listSentEidx.add(ary3SentEidx);
							// System.out.println("1c. sent .add ary=" + Arrays.toString(ary3SentEidx));
						}
						tmpSidx = tmpEidx;
					}
				}

				// additional patterns were not found - so use default
				// mechanism. Also i+1<listSentenceEnd.size

				if (listMorePatterns.size() == 0) {

					// else {
					// sentence = sentence
					// yyy .replaceAll("[\r\n]+", " ").replaceAll("[ ]+", " ").trim()
					// ;
					String[] arySentEidx = { sentence, eIdx + "" };
					listSentEidx.add(arySentEidx);
					// System.out.println("1d. sent .add ary=" + Arrays.toString(arySentEidx));
				}
			}

			sentence = "";

			if (i + 1 == listSentenceEnd.size()) {

				sentence = text.substring(sIdx, eIdx);
				String[] arySentEidx = { sentence, eIdx + "" };
				listSentEidx.add(arySentEidx);
				// System.out.println("1e. sent .add ary=" + Arrays.toString(arySentEidx));
				sentence = text.substring(eIdx, text.length());
				String[] arySentEidx2 = { sentence, eIdx + text.length() + "" };
				// System.out.println("1f. sent .add ary=" + Arrays.toString(arySentEidx));
				listSentEidx.add(arySentEidx2);
			}
		}

		List<String> listSent = new ArrayList<>();
		String text2 = "";
		int cnt = 0;
		// here I have multi sentences
		List<String> listMaster = new ArrayList<>();

		for (int i = 0; i < listSentEidx.size(); i++) {
			// System.out.println("listSentEidx=" + Arrays.toString(listSentEidx.get(i)));
			text2 = listSentEidx.get(i)[0];

			// if secHdgFound - it is not part of text - so need to add it here.

			// this runs 1 on the 1 sentence but its results need to be added to a full list
			// for each list returned. so I run through each sentence in this outer loop and
			// take results of the cleanup list to add to a new list and that ends the loop
			// and creates the list for the next setup cleanup methods.

			// System.out.println("going to sentenceSectionCleanup");

			// delete
			// if (tmp) {
			// NLP.printListOfString("before sentenceSectionCleanup", list);
//			}

			list = SolrPrep.sentenceSectionCleanup(text2);
			// NLP.printListOfString("after sentenceSectionCleanup", list);

			// list = SolrPrep.sentenceSubHeadingCleanup(list);
			// NLP.printListOfString("after sentenceSubHeadingCleanup", list);
			// NOTE: THIS METHOD IS - sentenceSubHeadingCleanup - IS DEAD.

			// delete
//			if (tmp) {
//				NLP.printListOfString("after sentenceSectionCleanup", list);
//			}

			list = SolrPrep.sentenceMultipleCleanup(list);

//			 // delete
//			 if (tmp) {
//					 NLP.printListOfString("after sentenceMultipleCleanup", list);
//			 }

			list = SolrPrep.sentenceSemiColon(list);
//			System.out.println("listSemiColon.size="+list.size());

			list2 = getSentenceAwayFromShortLines(list);

			for (int a = 0; a < list2.size(); a++) {
//				if(tmp) {
//					System.out.println("list2==" + list2.get(a));
//				}
				listMaster.add(list2.get(a));
			}

		}

		for (int c = 0; c < listMaster.size(); c++) {
//			if (secHdgFound && c == 0 && sectionHeading.trim().length() > 1) {
//				listSent.add(sectionHeading.trim());
//				sbLen.append(sectionHeading.trim());
//						cnt++;
//			}

			if (listMaster.get(c).trim().length() < 1)
				continue;
			listSent.add(listMaster.get(c).trim());
			sbLen.append(listMaster.get(c).trim());
//			if(tmp) {
//				System.out.println("c="+c+" adding to sbLen.app="+listMaster.get(c)+"||");
//			}
			cnt++;
		}

		// 1 chg here.
		if (cnt == 0) {
//			listSent.add(sectionHeading.trim());
			sbLen.append(sectionHeading.trim());
			listSent.add(origText);
			sbLen.append(origText);
		}

		double sbLength = sbLen.toString().length();
		double textLength = text.length();

//		if(tmp) {
//			System.out.println("sbLen=="+sbLength);
//			System.out.println("textLen=="+textLength);
//			System.out.println("sbTmp.toStr="+sbLen.toString());
//			System.out.println("origText.length()="+origText.length());
//			System.out.println("sbTmp.toStr.len="+sbLen.toString().length());
//		}

		if (sbLength / textLength < .7) {
			listSent = new ArrayList<>();
			listSent.add(origText);
//			System.out.println("oh crap - ck==" + text);
//			System.out.println("sbLength/ textLength==" + sbLength / textLength);
		}

		return listSent;

	}

	public static String stripHtml(String text) throws IOException, SQLException {
		// System.out.println("--2--"+text.substring(1000, 6000));
		// System.out.println("stripHtml text.len=" + text.length());

		// PrintWriter pw = new PrintWriter(new
		// File("e:/getContracts/temp/beforeRemoveGobblyGookGetContracts.txt"));
		// pw.append(text);
		// pw.close();
		// System.out.println("after save before removeGobbly text.len="+text.length());

		// System.out.println(text.substring(0, 5000));

		long startTime = System.nanoTime();
		text = NLP.removeGobblyGookGetContracts(text);// why again? - necessary?? did this get done in getContracts()
		long elapsedTime = System.nanoTime() - startTime;
		System.out.println("execution remove gobblygook: " + elapsedTime / 1000000);

		// System.out.println("--3--"+text.substring(2000, 30000));
		// i'm stripping twice! TODO! THEN FIGURE OUT FORMAT OF SENT/PARA THEN INTO
		// SOLR!0
		// pw = new PrintWriter(new
		// File("e:/getContracts/temp/afterRemoveGobblyGookGetContracts.txt"));
		// pw.append(text);
		// pw.close();

//		System.out.println("text len before stripped=" + text.length());

		startTime = System.nanoTime();
		text = ContractParser.stripHtmlTags(text);
		elapsedTime = System.nanoTime() - startTime;
		System.out.println("old @stripHtmlTags: " + elapsedTime / 1000000);

//		System.out.println("text len aft stripped=" + text.length());
		if (text.length() > maxStrippedFileSize)
			return "";

		// pw = new PrintWriter(new File("e:/getContracts/temp/fnl_stripHtmlTags.txt"));
		// pw.append(text);
		// pw.close();

		// see GetContracts.pageNumbersLocate(String method) - create a page number
		// finder that uses consecutive methodolgy

		text = text.replaceAll("\n", "\r\n").replaceAll("\r\r\n", "\r\n");
		startTime = System.nanoTime();
//		text = GetContracts.removePageNumber(text);// page numbers corrupt sentence flow
		// text = listTextPageIdx.get(listTextPageIdx.size()-1)[0];
		elapsedTime = System.nanoTime() - startTime;
		System.out.println("@end repl: " + elapsedTime / 1000000);

		// this replace fixes section ends occurring in the middle of a sentence
		text = text.replaceAll(
				"((?ism)(?<=(corp|inc|p\\.?l\\.?c\\.|" + "s\\.?p\\.?a\\.|l\\.?l\\.?c\\.|l\\.?l\\.?p\\.))\\.(?= \\())",
				"");

		text = text.replaceAll("(?sm)(?<= pursuant to( this)?" + "|subject to( this)?" + "|including( this)?"
				+ "|set ?forth (under|in)" + "|contemplated by"
				+ "|under( this)?|accordance with|of) ?[\r\n]{1,5} ?(?=[A-Z]{1})", " ");// put in stripHtml
		// this replaces ensure [Reserved] is attached to section. (don't worry
		// elsewhere).

		text = text.replaceAll(
				"(?sm)(?<=(S(ECTION|ection) [\\d\\.]{1,6}))[\r\n]{1,8}(?=\\[?[RD]{1}(ELETED?|eleted?|ESERVED?|eserved?)\\.?\\]?\\.?\r\n)",
				" ");// put in stripHtml

		// gets rid of hanging section number - only use for all caps.
		text = text.replaceAll("(?sm)(?<=[\r\n ]{1}(SECTION ?))\r\n ?(?=[\\d\\.]{1,6} [A-Z]{1})", " ");// put in
		// pw = new PrintWriter(new File("e:/getContracts/temp/tmpStrip2.txt"));
		// pw.append(text);
		// pw.close();

		// stripHtml

		// makes sure [Reserved] section on its own line
		text = text.replaceAll(
				"(?sm)\\.  ?(?=SECTION [\\d\\.]{1,7} \\[?[RD]{1}(ELETED?|eleted?|ESERVED?|eserved?)\\]?)",
				"\\.\r\n\r\n");
		// puts a hard return before a section heading
		text = text.replaceAll("(?sm)(?<=[a-z]{2}\\.)  ?(?=(SECTION [\\d]{1,2}"
				+ "\\.[\\d]{1,2}\\.? [A-Z]{1}[a-z]{1,20} " + "([A-Z]{1}[a-z]{1,20}|[a-z]{1,3})" + "))", "\r\n\r\n");

		text = text.replaceAll("and/or", "and or");
		// String tocText = GetContracts.getContractToc(text);
		// tocText.length();
		// System.out.println("4 text.len=" + text.length());
		// pw = new PrintWriter(new File("e:/getContracts/temp/tmpStrip3.txt"));
		// pw.append(text);
		// pw.close();

		text = text.replaceAll("([\r\n]{1,2})([ ]+)([\r\n]{1,2})", "$1$3").replaceAll("[\r\n]{9,}", "\r\n\r\n\r\n\r\n");

		// pw = new PrintWriter(new File("e:/getContracts/temp/tmpStripFnl.txt"));
		// pw.append(text);
		// pw.close();

		listPrelimIdxAndSectionRestrictive = ContractNLP.getAllIdxLocsAndMatchedGroups(text,
				ContractParser.patternSectionHeadingRestrictive);

		// pw = new PrintWriter(new File("e:/getContracts/temp/stripHtml2.txt"));
		// pw.append(text);
		// pw.close();

		if (listPrelimIdxAndSectionRestrictive.size() < 2) {
			// //NLP.pwNLP
			// .append(NLP.println("going to cleanupSectionHeadings", ""));
			cleanupSectionHeadings(text);
		}

		// pw = new PrintWriter(new File("e:/getContracts/temp/stripHtml3.txt"));
		// pw.append(text);
		// pw.close();

		text = text.replaceAll("(?<=[a-z]{1}) \\.  ?(?=[A-Z]{1}1)", "\\. ");
		text = text.replaceAll("[ ]+", " ");
		text = text.replaceAll("(?sm)[\r\n]+ ?\\. ", "\\.\r\n\r\n");
		text = text.replaceAll("(?sm)[\t ]{1,4}\\.(?= ?[A-Z]{1}[A-Za-z]{1})", "\\. ");

		// SolrPrep.listTextExhMarkers = new ArrayList<>();
		int sIdx = 0, cnt = 0;
		List<String[]> listEM = NLP.getAllMatchedGroupsAndStartIdxLocs(text, Pattern.compile("(?<=zc).{1,9}(?=zc)"));
		List<String[]> listEMtmp = new ArrayList<>();
		for (int i = 0; i < listEM.size(); i++) {
			if (listTextExhMarkers.size() == 0)
				break;
			cnt = cnt + -listEM.get(i)[0].trim().length();
			// System.out.println("negative cnt at lisEM. cnt="+cnt);
			sIdx = Integer.parseInt(listEM.get(i)[1]) + (cnt);
			String[] ary = { listEM.get(i)[0], sIdx + "", "prior" + listTextExhMarkers.get(i)[5],
					listTextExhMarkers.get(i)[6] };
			listEMtmp.add(ary);
		}

		SolrPrep.listTextExhMarkers = listEMtmp;

		// NLP.printListOfStringArray("2. SolrPrep.listTextExhMarkers=",
		// SolrPrep.listTextExhMarkers);
		// System.out.println("final listTextExhMarkers.size="+listTextExhMarkers.size()
		// +" replaced exh pg markers");
		text = text.replaceAll("zc.{1,9}zc", "");

		// pw = new PrintWriter(new
		// File("e:/getContracts/temp/stripHtmlTxtFinished.txt"));
		// pw.append(text);
		// pw.close();

		return text;

	}

	public static String stripHtmlSuperSimple(String text) throws IOException {

		text = NLP.removeGobblyGookGetContracts(text);

		text = text.replaceAll("(?i)<sup>", " ");
		text = text.replaceAll("(?i)</sup>", "");
		text = text.replaceAll("(?i)</(FONT|I|U|B)>", "");
		text = text.replaceAll("(?i)(?<=[a-zA-Z\\d]{1})<BR>(?=[a-zA-Z\\d]{1})", " <BR>");
		text = text.replaceAll("</h\\d>", "\r\n");
		text = text.replaceAll("(?i)<BR> ?\r\n ?<BR>", "\r\n\r\n");
		text = ContractNLP.numberBR.matcher(text).replaceAll("\r\n");
		text = text.replaceAll("(?i)<br />|<BR>|</h\\d>", "\r\n");
		text = text.replaceAll("(?ism)<h\\d[^>]*>", "\r\n");
		// *****NOTE**** KEEP THESE 2 =>text.replaceAll("(?sm)<[^>]*>", ""); --
		// FIX ERRORS PRIOR TO HERE!!!
		text = text.replaceAll("(?ism)</div>|</p>", "\r\n");
		text = text.replaceAll("(?sm)<[^>]*>", "");
		text = text.replaceAll("(?sm)</[^>]*>", "\r\n");
		text = text.replaceAll(ContractNLP.TRPattern.toString(), ContractNLP.LineSeparator);
		text = text.replaceAll(
				"\\&\\#9;|\\&nbsp;|\\xA0|\\&#149;|\\&#160;|\\&#xA0;" + "|\\&#168;|\\&#173;|\\&#32;|\\&#8194;", " ");
		text = text.replaceAll("\\&#183;|\\&#8211;|\\&#9679;|\\&#150;|\\&#8212;|\\&#8209;|\\&#111;", "-");
		text = text.replaceAll("\\&amp;", "\\&");
		text = text.replaceAll("â€”", "-").replaceAll("DESCRIPTION>|"
				+ "\\&reg;|\\&#166;|\\&#133;|\\&#174;|\\&#9;|\\&#229;|\\&#230;|\\&#068;|\\&#097;|\\&#103;|&#093;|\\&#091;",
				"");
		text = text.replaceAll("\\&#146;|\\&rsquo;|\\&#8217;|\\&#x2019;|\\&#8216;", "'").replaceAll(
				"\\&#147;|\\&#148;|Ã¢â‚¬Å|”|“|Ã¢â‚¬?|\\&#8221;|\\&#8220;|\\&ldquo;|\\&rdquo;|\\&quot;|\\&\\#x201C;|\\&\\#x201D",
				"\"");
		text = text.replaceAll("\\&#184;", ",");
		text = text.replaceAll(ContractNLP.TDWithColspanPattern.toString(), "\t\t");
		text = text.replaceAll(ContractNLP.TDPattern.toString(), "\t");
		text = text.replaceAll("(?<=[\\p{Alnum};,\":\\-\\&]) <", "\\&nbsp;<");
		text = text.replaceAll("\\&nbsp;", " ");// replace 2 or 3 w/ 1
		text = text.replaceAll("(?is)<!--.*?-->", "");
		text = text.replaceAll("[\r\n]{1,}\\.", "\\.\r\n");
		text = text.replaceAll("([\r\n]{1,2})([ ]+)([\r\n]{1,2})", "$1$3");
		text = text.replaceAll("\\&#187;[ \r\n\t]{0,15}\\.", "\\.");
		text = text.replaceAll("([ ]+)(\r\n)", "$2");
		text = text.replaceAll("([a-zA-Z]{1})(\\.)([a-zA-Z]{1})(\\.)([a-zA-Z]{1})(\\.)", "$1xxPD$3xxPD$5xxPD");
		text = text.replaceAll("([a-zA-Z]{1})(\\.)([a-zA-Z]{1})(\\.)", "$1xxPD$3xxPD");
		text = text.replaceAll(" (?=(INC|CO|Inc|Co))\\.(?=[, ]{1})", "xxPD");
		text = text.replaceAll("(?<= (INC|CO|Inc|Co))\\.(?=[, ]{1})", "xxPD");
		text = text.replaceAll("(" +
		// 1st word
				" [\\)\\(\\[\\]A-Za-z,;]{1,11}" +
				// 2nd word
				" [\\)\\(\\[\\]a-z,;]{1,11} )" + "(\")" + "([\\[\\]_-a-zA-Z;, ]{1,50})" + "(\")", "$1''$3''");
		// are rated at least "[__]" by [RA2] and "[__]" fr
		// not followed by two spaces or 1 space and an initial caps.

		// System.out.println("2g text.repl len="+text.length());

		text = text.replaceAll("(?<=[a-z]{1}) \\.  ?(?=[A-Z]{1}1)", "\\. ");
		text = text.replaceAll("\\&#043;", "+");
		text = text.replaceAll("\\&#038;", "\\&");

		return text;

	}

	public static void cleanupSectionHeadings(String text) throws IOException {

		// NLP nlp = new NLP();

		listPrelimIdxAndSectionRestrictive = ContractNLP.getAllIdxLocsAndMatchedGroups(text,
				ContractParser.patternSectionHeadingRestrictive);

		if (listPrelimIdxAndSectionRestrictive.size() > 10) {
			// ACTUALLY DON'T DO THIS --- CLEANUP BY REMOVING EXTRA HARD
			// RETURNS!
			// listPrelimIdxAndSection = ContractNLP
			// .getAllIdxLocsAndMatchedGroups(text, patternSec);
		} else {
			listPrelimIdxAndSectionRestrictive = new ArrayList<>();
		}

	}

	public static String filterTrustAgr(String text, String contractName, String filename) {

		// type is not indenture
		if (!contractName.toLowerCase().equals("indenture"))
			return text;
		// indenture is in filename (contractLongName)
		if (filename.toLowerCase().contains("indenture"))
			return text;

		// if it has trust in filename or fund or
		// pfolio or size < 100k return not text
		if ((filename.toLowerCase().contains("trust") && (filename.toLowerCase().contains("fund"))
				|| filename.toLowerCase().contains("portfolio") || text.length() < 100000)) {
			return text = "";
		}

		return text;
	}

	public static String getContractName(String text, String contractName, String filename) {

		// type is not indenture
		if (!contractName.toLowerCase().equals("indenture"))
			return contractName;
		// indenture is in filename (contractLongName)
		if (filename.toLowerCase().contains("indenture"))
			return contractName;

		// if it is not a trust indenture
		if (text.length() > 100000 && filename.toLowerCase().contains("trust")
				&& !filename.toLowerCase().contains("fund") && !filename.toLowerCase().contains("portfolio")) {
			return "trust indenture";
		}

		return contractName;
	}

	public static void prepDownloadedContracts(String year, String qtr) throws IOException, SQLException {
		NLP nlp = new NLP();
		System.out.println("prep download");

		// takes downloaded contracts and strip html and inserts at first line
		// at char 0 the contractName (solr contractType)

		File files = new File(folderDownload + "/" + year + "/QTR" + qtr + "/");
		File[] listOfFiles = files.listFiles();

		System.out.println("prepdownload folder=" + files.getAbsolutePath());

		if (null != listOfFiles && listOfFiles.length > 0)
			System.out.println("# of files=" + listOfFiles.length);
		if (null == listOfFiles || listOfFiles.length == 0)
			return;

		String strippedfolderQtrYear = GetContracts.strippedFolder + "/" + year + "/QTR" + qtr + "/";
		Utils.createFoldersIfReqd("e:/getContracts/tmpDwndLd/");

//		System.out.println("a strippedfolderQtrYear=" + strippedfolderQtrYear);
		String cik_K_Id = "";
		File f = new File(strippedfolderQtrYear + "tmp.txt");

		for (File file : listOfFiles) {
			File files2 = new File(file.getAbsolutePath());
			File[] listOfFiles2 = files2.listFiles();
			for (File file2 : listOfFiles2) {

				strippedfolderQtrYear = file2.getAbsolutePath().replaceAll("(?ism)downloaded", "stripped");
				System.out.println("1strippedfolderQtrYear==" + strippedfolderQtrYear);
				strippedfolderQtrYear = strippedfolderQtrYear.substring(0, strippedfolderQtrYear.lastIndexOf("\\"));
				System.out.println("2strippedfolderQtrYear==" + strippedfolderQtrYear);

				Utils.createFoldersIfReqd(strippedfolderQtrYear);

				f = new File(strippedfolderQtrYear + "/" + f.getName());
				if (!strip && f.exists()) {
					continue;
				}
				System.out.println("next file is=" + file2.getAbsolutePath());

				System.out.println("2. start method stripFiles(). ");
				stripFiles(file2, strippedfolderQtrYear, year, qtr);
			}
		}
	}

	public static void stripFiles(File file, String strippedfolderQtrYear, String year, String qtr)
			throws IOException, SQLException {

		GetContracts gK = new GetContracts();

		NLP nlp = new NLP();
		String text = "", contractType = "";

		File fileStrip = new File(strippedfolderQtrYear + "/" + file.getName());
		System.out.println("fileStrip path?=" + fileStrip.getAbsolutePath());

		System.out.println("reStrip=" + strip);
		if (fileStrip.exists() && !strip) {
			System.out.println("getContractSentencesReadyForSolr - file=" + file.getName());
			SolrPrep.getContractSentencesReadyForSolr(year, qtr);
			System.out.println("stripped file exists");
			return;
		}

		text = Utils.readTextFromFile(file.getAbsolutePath());
		if (text.length() < 5000)
			return;

		text = stripHtml(text);

		// text = listTextPageIdx.get(listTextPageIdx.size()-1)[0];

		System.out.println("aft stripHtml=" + text.length());

		System.out.println("contractName=" + contractType);

		text = contractType.replaceAll("[\r\n]", "") + "||" + text;
		System.out.println("saving stripped file=" + strippedfolderQtrYear + "/" + file.getName());

		PrintWriter pw2 = new PrintWriter(new File(strippedfolderQtrYear + "/" + file.getName()));
		pw2.println(text);
		pw2.close();

		System.out.println("getContractSentencesReadyForSolr - contractType=" + contractType);
		SolrPrep.getContractSentencesReadyForSolr(year, qtr);

	}

	public static void getSolrReady(int startYr, int endYr, int startQtr, int endQtr) throws IOException, SQLException {

		// call prepDownLoadedContracts and getContractSentencesReadyForSolr
//		System.out.println("listContractAttributes.size=" + listContractAttributes.size());
//		int year = endYr;
		System.out.println("startYr=" + startYr + " endYr=" + endYr);
		int qtr = 1, eYr = endYr;

		// at this point we know we want to strip/parse into solr the contract. The
		// rulesets used if it was filter by contract types gives some indication of
		// what contract type it is (sometimes a good indication sometimes not). We
		// would want this information irrespective.

//		for (int i = 0; i < listContractAttributes.size(); i++) {
//			 System.out.println("attri="+Arrays.toString(listContractAttributes.get(i)));

		for (; endYr >= startYr; endYr--) {
			System.out.println("endYr=" + endYr + " startYr=" + startYr);
			qtr = endQtr;
			if (startYr == 1995)
				startQtr = 3;
			else
				startQtr = 1;

			for (; qtr >= startQtr; qtr--) {
				System.out.println("qtr=" + qtr + " startQtr=" + startQtr);
				// stripFiles == in case I just want to skip prepDownloadedContracts versus just
				// those I already stripped
				if (strip) {
					System.out.println("going to stripFile and getContractSentencesReadyForSolr");
					SolrPrep.prepDownloadedContracts(endYr + "", qtr + "");
				}
			}
			qtr = 4;
		}

		endYr = eYr;

//		}
	}

//	public static List<String[]> getContractAttributes() {
//
//		Pattern patternToExclGen = Pattern.compile("stock");
//		Pattern patternIndentureToExclude = Pattern.compile("(?i)supp|amend");
//		// minimum fileSize = [0]
//		String[] IAA = { "InvestmentAdvisorAgt", "10000", patternIAA.toString(), patternIndentureToExclude.toString(),
//				patternIAA.toString(), patternIndentureToExclude.toString() };
//
//		String[] indenture = { "Indenture", "50000", patternIndenture.toString(), patternIndentureToExclude.toString(),
//				patternIndenture.toString(), patternIndentureToExclude.toString() };
////[0]=contractType,[1]=fileSize,[2]=patternIndenture,[3]=patternExclude,[4]=kName to include [5]=kName to exclude. 4 and 5 used after stripped and only if choosing unq ciks. 2 and 3 used before stripped.
//		String[] suppIndenture = { "SuppAmenIndenture", "1000", patternSuppIndenture.toString(), "xx",
//				patternSuppIndenture.toString(), "xx" };
//
//		String[] psa = { "PSA", "100000", patternPSA.toString(), patternContractTypesToExclude.toString(),
//				patternPSA.toString(), patternContractTypesToExclude.toString(), patternPSA.toString() };
//		String[] purchase = { "Purchase Agreement", "100000", patternPurchase.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] escrow = { "Escrow", "20000", patternEscrow.toString(), patternContractTypesToExclude.toString() };
//		String[] custody = { "Custody", "12000", patternCustody.toString(), patternContractTypesToExclude.toString(),
//				patternCustody.toString(), patternContractTypesToExclude.toString(), patternCustody.toString() };
//
//		String[] secLending = { "SLAA", "20000", patternSecLending.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] fiscalAg = { "FiscalAgent", "80000", patternFiscalAg.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] payingAg = { "PayingAgent", "10000", patternPayingAg.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] transferAg = { "TransferAgent", "20000", patternTransferAg.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] controlAg = { "AccountControl", "5000", patternControlAg.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] reinsuranceTrustAg = { "RTA", "2000", patternReinsuranceTrustAg.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] collateralPledgeAg = { "Collateral", "10000", patternCollateralPledgeAg.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] calculationAgentAg = { "CalculationAgent", "1000", patternCalculationAgentAg.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] accountBankAg = { "AccountBank", "2000", patternAccountBankAg.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] administrativeAgentAg = { "AdministrativeAgent", "10000", patternAdministrativeAgentAg.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] listingAgentAg = { "ListingAgent", "2000", patternListingAgentAg.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] fundAdminAgent = { "FundAdminAgent", "2000", patternFundAdminAg.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] exhangeAg = { "ExhangeAg", "2000", patternExhangeAg.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] depositoryAg = { "Depository", "2000", patternDepositoryAg.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] depositaryAg = { "DepositaryAg", "2000", patternDepositaryAg.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] portfolioAdminAg = { "PortfolioAdminAg", "21", patternPortfolioAdminAg.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] loanCreditAg = { "LoanCreditAg", "22", patternLoanCreditAg.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] saleServicingAg = { "SaleServicingAg", "100000", patternSaleServicingAg.toString(),
//				patternContractTypesToExclude.toString() };
////		String[] interCreditorAg = { "InterCreditorAg", "100000", patternInterCreditorAg.toString(),
////				patternContractTypesToExclude.toString() };
//		String[] corporateActions = { "CorporateActions", "1000", patternCorporateActions.toString(),
//				patternContractTypesToExclude.toString() };
//		String[] opinions = { "Opinions", "500", patternOpinion.toString(), patternContractTypesToExclude.toString(),
//				patternOpinion.toString(), patternContractTypesToExclude.toString() };
//		String[] other = { "Other", "500", patternOTHER.toString(), patternContractTypesToExclude.toString() };
//		String[] agreement = { "Agreement", "25", patternAgreement.toString(), Pattern.compile("").toString() };
//		String[] priceSuppProsp = { "suppProsp", "1000", patternPriceSuppProsp.toString(),
//				patternContractTypesToExclude.toString(), patternPriceSuppProsp.toString(),
//				patternContractTypesToExclude.toString() };
//
//		String[] sai = { "SAI", "1000", patternSAI.toString(), patternContractTypesToExclude.toString(),
//				patternSAI.toString(), patternContractTypesToExclude.toString() };
//
////		listContractAttributes.add(suppIndenture);
////		listContractAttributes.add(loanCreditAg);
////		listContractAttributes.add(IAA);
//		listContractAttributes.add(indenture);
////		listContractAttributes.add(psa);
////		listContractAttributes.add(fiscalAg);
////		listContractAttributes.add(payingAg);
////		listContractAttributes.add(priceSuppProsp);
////		listContractAttributes.add(sai);
//
////		listContractAttributes.add(reinsuranceTrustAg);
////		listContractAttributes.add(saleServicingAg);
////		listContractAttributes.add(interCreditorAg);
////		listContractAttributes.add(escrow);
////		listContractAttributes.add(custody);
////		listContractAttributes.add(secLending);
////		listContractAttributes.add(transferAg);
////		listContractAttributes.add(controlAg);
////		listContractAttributes.add(collateralPledgeAg);
////		listContractAttributes.add(calculationAgentAg);
////		listContractAttributes.add(accountBankAg);
////		listContractAttributes.add(administrativeAgentAg);
////		listContractAttributes.add(listingAgentAg);
////		listContractAttributes.add(fundAdminAgent);
////		listContractAttributes.add(exhangeAg);
////		listContractAttributes.add(depositaryAg);
////		listContractAttributes.add(portfolioAdminAg);
////		listContractAttributes.add(purchase);
////		listContractAttributes.add(interCreditorAg);
////		listContractAttributes.add(agreement);
////		listContractAttributes.add(corporateActions);
////		listContractAttributes.add(opinions);
////		listContractAttributes.add(other);
//
//		return listContractAttributes;
//
//	}

	public static String getLeadInClause(String text) throws IOException {

		NLP nlp = new NLP();
		String leadInClause = "";
		int idx = 0;

		// cant be "Class B Principal Distribution Amount":" for example
		if (text.replaceAll("\":", "").indexOf(":") > 0
				&& text.substring(0, text.indexOf(":")).replaceAll("[ \r\n\\.;\\:\\-_]+", "").trim().length() > 6 && nlp
						.getAllMatchedGroups(text.substring(0, (Math.min(text.indexOf(":") + 4, text.length()))),
								Pattern.compile("(?ism)(([ \r\n^]{1}by|dated?| title|If to the.{1,14}|Attention"
										+ "|signe?d?|Name|facsimile|telecopy|telephone|mobile|email|"
										+ "Signature Guarant[oresy]{1,3}?)\\)?:|[\\d]{1,2}:[\\d}]{2})|"
										+ "((paragraphs?|clauses?|FIRST|SECOND|THIRD|FOURTH)"
										+ "(\\([A-Za-z\\d]{1,4}\\),? )+((and|or) \\([A-Za-z\\d]{1,4}\\))?)"))
						.size() == 0) {

			// Gets mached group that ignore !\": - eg a colon preceded by a quote b/c it is
			// almost always a defined term. Altho there will be exceptions where --> '": '
			// but that's extremely rare and I will allow myself not to pickup that leadin!

			// idx = nlp.getAllIndexEndLocations(text,
			// Pattern.compile("(?sm)(?<!\"):[\r\n]{3,}")).get(0);
			idx = nlp.getAllIndexEndLocations(text, Pattern.compile("(?sm)(?<!\"):")).get(0);
			leadInClause = text.substring(0, idx).trim();
			// xxxx delete
			// if(leadInClause.contains(tmpStrGlobal)) {
			// System.out.println("@getL..leadin="+leadInClause);
			// }

			if (leadInClause.trim().length() >= 15)
				return leadInClause;

		}

		return "";

	}

	public static String getSecondLeadinClause(String text) throws IOException {
		NLP nlp = new NLP();

		StringBuilder sb = new StringBuilder();

		Pattern patternDoc = Pattern.compile("(?sm)<doc>.*?</doc>");
		Pattern patternPno = Pattern.compile("(?sm)(?<=<field name=\"paraNo\">)[\\d]+(?=</field>)");
		Pattern patternTxt = Pattern.compile("(?sm)(?<=<field name=\"txt\"><\\!\\[CDATA\\[).*?: ?(?=\\]\\]></field>)");
		Pattern patternLead = Pattern
				.compile("(?sm)(?<=<field name=\"lead\"><\\!\\[CDATA\\[).*?: ?(?=\\]\\]></field>)");
		List<String> listDoc = nlp.getAllMatchedGroups(text, patternDoc);
		List<String> listPno = new ArrayList<>();
		System.out.println(listDoc.size());

		// if leadin exists does anything that follow this sentence have a ':' that ends
		// the sentence. First I find if a doc has a leadin. Then I continue so long as
		// 'lead' exists and pNo are the same. Once leadin does not exists I determine
		// if a ':' ends any next sentence and if so that is my 2nd leadin and I append
		// to doc at that point and going forward until pNo changes.
		boolean foundLeadIn = true;
		String doc = "", pNoPrior = "", pNo = "", lead2 = "";
		// i=1 to skip metadata field.
		int cnt = 0, cnt2 = 0;
		sb.append("<add>\r\n");
		// System.out.println("list.Doc.size="+listDoc);
		for (int i = 0; i < listDoc.size(); i++) {
//			 System.out.println("sentTxt - list.doc.get(i)="+listDoc.get(i));
			doc = listDoc.get(i);
			// xxxxx delete
			// if(doc.contains(tmpStrGlobal)) {
			// System.out.println("doc=="+doc);
			// }
			if (i == 0) {
				// no pNo in first <doc>.*?</doc>
				sb.append(doc + "\r\n");
				continue;
			}

			// System.out.println("doc=="+doc);
			pNoPrior = pNo;
			pNo = nlp.getAllMatchedGroups(doc, patternPno).get(0);
			// System.out.println("pNo=" + pNo + " pNoPrior=" + pNoPrior);
			if (!pNoPrior.equals(pNo)) {
				cnt2 = 0;
				sb.append(doc + "\r\n");
				continue;
			}

			if (!doc.contains("ield name=\"lead\">")) {
				cnt2 = 0;
				sb.append(doc + "\r\n");
				continue;
			}

			if (nlp.getAllMatchedGroups(doc, patternTxt).size() == 0 && cnt2 == 0) {
				sb.append(doc + "\r\n");
				continue;
			}

			if (cnt2 == 0 && nlp.getAllMatchedGroups(doc, patternTxt).size() > 0) {
				lead2 = nlp.getAllMatchedGroups(doc, patternTxt).get(0);
				if (lead2.equals(nlp.getAllMatchedGroups(doc, patternLead).get(0))) {
					sb.append(doc + "\r\n");
					continue;
				}

				if (lead2.substring(0, lead2.indexOf(":")).replaceAll("[ \r\n\\.;\\:\\-_]+", "").trim().length() <= 6 ||

						nlp.getAllMatchedGroups(lead2.substring(0, (Math.min(lead2.indexOf(":") + 4, lead2.length()))),
								Pattern.compile("(?ism)(([ \r\n^]{1}by|dated?| title|If to the.{1,14}|Attention"
										+ "|signe?d?|Name|facsimile|telecopy|telephone|mobile|email|"
										+ "Signature Guarant[oresy]{1,3}?)\\)?:|[\\d]{1,2}:[\\d}]{2})|"
										+ "((paragraphs?|clauses?|FIRST|SECOND|THIRD|FOURTH)"
										+ "(\\([A-Za-z\\d]{1,4}\\),? )+((and|or) \\([A-Za-z\\d]{1,4}\\))?)"))
								.size() > 0) {
					sb.append(doc + "\r\n");
					continue;
				}

				cnt2 = 1;
				if (lead2.trim().length() < 1) {
					sb.append(doc + "\r\n");
					continue;
				}

				// System.out.println("1 lead2=" + lead2);
			}

			doc = doc.replace("</doc>", "<field name=\"lead2\"><![CDATA[" + lead2 + "]]></field>\r\n</doc>\r\n");

			sb.append(doc + "\r\n");
			// System.out.println("i=" + i + " lead2=" + doc);
		}

		sb.append("</add>");
		// System.out.println("sentenceXml="+sb.toString());
		return sb.toString();

	}

	public static String getParagraphLeadInClause(String text) throws IOException {

		NLP nlp = new NLP();
		// StringBuilder sb = new StringBuilder();

		text = text.replaceAll(
				"(?ism)(paragraphs?|clauses?)" + " (\\([A-Za-z\\d]{1,4}\\),? )+((and|or) \\([A-Za-z\\d]{1,4}\\))?", "");
		Pattern patternParaNumberMarker = Pattern
				.compile("[\r\n ]{1}(\\([A-Za-z]{1,3}\\)|([A-Z]{2}|[a-z]{2})\\.|\\([\\d]{1,3}\\)|[\\d]{1,3}\\.) ");
		List<String[]> listParaNumberMarket = NLP.getAllMatchedGroupsAndStartIdxLocs(text, patternParaNumberMarker);
		// the parentLeadInClause must be the start of the sentence OR end with a ':'.
		// If start of sentence it can simply end with the first alphaParaNumberMarker
		// System.out.println("listParaNumberMarket.size=" +
		// listParaNumberMarket.size());

		if (listParaNumberMarket.size() < 2)
			return "";

		// first cut should probably store alpha para markers in a list.
		boolean consecCount = false;
		String leadInList = "";
		for (int i = 0; i < listParaNumberMarket.size(); i++) {
			// stanford LS f/p=clause (i) || (b) to (f) || (b) and[or] (f)
			// clauses (i), (ii) or (iii)

			int a = nlp.convertAlphaParaNumb2(listParaNumberMarket.get(0)[0].trim(), 0);
			// System.out.println("para alpha 1 marker=" + listParaNumberMarket.get(0)[0]);
			// System.out.println("para alpha 1 converted to #=" + a);

			int b = nlp.convertAlphaParaNumb2(listParaNumberMarket.get(1)[0].trim(), 1);
			// System.out.println("para alpha 2 marker=" + listParaNumberMarket.get(1)[0]);
			// System.out.println("para alpha 2 converted to #=" + b);

			// System.out.println("listParaNumberMarket.size()=" +
			// listParaNumberMarket.size());
			// System.out.println("c,d,e...="+nlp.convertAlphaParaNumb2(listParaNumberMarket.get(2)[0].trim(),
			// i));

			if (listParaNumberMarket.size() == 2 && b - a == 1
					|| (listParaNumberMarket.size() > 2 && b - a == 1
							&& nlp.convertAlphaParaNumb2(listParaNumberMarket.get(2)[0].trim(), i)
									- nlp.convertAlphaParaNumb2(listParaNumberMarket.get(1)[0].trim(), i - 1) == 1)
					|| (listParaNumberMarket.size() > 3 // c-b=1, d-c=1
							&& nlp.convertAlphaParaNumb2(listParaNumberMarket.get(2)[0].trim(), i)
									- nlp.convertAlphaParaNumb2(listParaNumberMarket.get(1)[0].trim(), i - 1) == 1
							&& nlp.convertAlphaParaNumb2(listParaNumberMarket.get(3)[0].trim(), i)
									- nlp.convertAlphaParaNumb2(listParaNumberMarket.get(2)[0].trim(), i - 1) == 1)) {

				consecCount = true;
				// System.out.println("consecCount=" + true);
				break;
			}
		}

		if (consecCount) {
			// System.out.println("leadin len to
			// ':'="+text.substring(0,text.indexOf(":")).length());
			leadInList = text.substring(0, Integer.parseInt(listParaNumberMarket.get(0)[1]));
			// System.out.println("leadInList.len="+leadInList.length());
			// System.out.println("leadInToList=" + leadInList + "|END");

			// If i remove F/Ps - then when confirm there is a lead-in clause how do I
			// ensure what I grab has not been effectd by the text that was replaced?

			// requires no ':' or for it be no more than 50 characters/spaces from start of
			// list.
			if (nlp.getAllIndexEndLocations(text, Pattern.compile(":|,")).size() == 0
					|| nlp.getAllIndexEndLocations(text, Pattern.compile(":|,")).size() > 0 && leadInList.length()
							- text.substring(0, nlp.getAllIndexEndLocations(text, Pattern.compile(":|,")).get(0))
									.length() > 100) {
				leadInList = "";
			}
		}

		// if (leadInList.length() > 0)
		// System.out.println("leadInList=" + leadInList);

		return leadInList;

	}

	public static String solrFilesSections(String text, List<String[]> listData) throws IOException {
		// listData = meta data fields.

		System.out.println("solrFilesSections text.len=" + text.length());
		NLP nlp = new NLP();
		StringBuilder sb = new StringBuilder();
		// PrintWriter pw = new PrintWriter(new File("e:/getcontracts/temp/tmp5.txt"));
		List<Integer[]> list = nlp.getAllStartAndEndIdxLocations(text, Pattern.compile("(?sm)<sT>.*?</sT>"));
		System.out.println("list.size=" + list.size());
		if (list.size() == 0)
			return text;

		int sIdx = 0, eIdx = 0, priorEidx = 0;
		String sectionName = "";
		for (int i = 0; i < list.size(); i++) {

			sIdx = list.get(i)[0];
			eIdx = list.get(i)[1];

			if (i == 0 && sIdx > 0) {
				sb.append("<sectionText>" + text.substring(0, sIdx) + "</sectionText>");
				sectionName = "";

				sectionName = nlp
						.getAllMatchedGroups(text.substring(sIdx, eIdx), Pattern.compile("(?sm)(?<=<sN>).*?(?=</sN>)"))
						.get(0).trim();
				sb.append("<sectionName>" + sectionName + "</sectionName>");
				sb.append("<sectionText>" + text.substring(sIdx, eIdx) + "</sectionText>");
				priorEidx = eIdx;
				continue;
			}

			if (i == 0 && sIdx == 0) {

				sectionName = nlp
						.getAllMatchedGroups(text.substring(sIdx, eIdx), Pattern.compile("(?sm)(?<=<sN>).*?(?=</sN>)"))
						.get(0).trim();
				sb.append("<sectionName>" + sectionName + "</sectionName>");
				sb.append("<sectionText>" + text.substring(sIdx, eIdx) + "</sectionText>");
			}

			if (priorEidx < sIdx) {

				sb.append(text.substring(priorEidx, sIdx));
				sectionName = "";

				sectionName = nlp
						.getAllMatchedGroups(text.substring(sIdx, eIdx), Pattern.compile("(?sm)(?<=<sN>).*?(?=</sN>)"))
						.get(0).trim();
				sb.append("<sectionName>" + sectionName + "</sectionName>");
				sb.append("<sectionText>" + text.substring(sIdx, eIdx) + "</sectionText>");

				// System.out.println("sectionName=" + sectionName);
			}

			priorEidx = eIdx;
		}

		sb.append("<sectionText>" + text.substring(eIdx, text.length()) + "</sectionText>");
		sectionName = "";

		// pw.append(sb.toString());
		// pw.close();
		return sb.toString();

	}

	public static File solrFileFieldIndexes(String text, File fileXML) throws SQLException, IOException {

//		System.out.println("what the xml file path will be==" + fileXML.getAbsolutePath());
//		System.out.println("recording idx locations of sections, defs, exh and paras\r\n"
//				+ "to mysql table. The table is then manipulated in mysql via queries" + "embedded here (in java)\r\n"
//				+ "");
		// String text = Utils.readTextFromFile(file.getAbsolutePath());
		// System.out.println("text.len="+text.length());
		NLP nlp = new NLP();
		// there will always be at least 1 section. if 1 it is entire doc.
		List<Integer[]> listSec = nlp.getAllStartAndEndIdxLocations(text, Pattern.compile("((?sm)<sT>.*?</sT>)"));
		List<Integer[]> listSecName = nlp.getAllStartAndEndIdxLocations(text, Pattern.compile("((?sm)<sN>.*?</sN>)"));
		List<Integer[]> listDef = nlp.getAllStartAndEndIdxLocations(text, Pattern.compile("((?sm)<dT>.*?</dT>)"));
		List<Integer[]> listDefName = nlp.getAllStartAndEndIdxLocations(text, Pattern.compile("((?sm)<dN>.*?</dN>)"));
		List<Integer[]> listExh = nlp.getAllStartAndEndIdxLocations(text, Pattern.compile("((?sm)<eT>.*?</eT>)"));
		List<Integer[]> listExhName = nlp.getAllStartAndEndIdxLocations(text, Pattern.compile("((?sm)<eN>.*?</eN>)"));
		List<Integer[]> listPara = nlp.getAllStartAndEndIdxLocations(text, Pattern.compile("((?sm)<pT>.*?</pT>)"));

		StringBuilder sb = new StringBuilder();
		sb.append(returnList(listSec, "sT"));
		sb.append(returnList(listSecName, "sN"));
		sb.append(returnList(listDef, "dT"));
		sb.append(returnList(listDefName, "dN"));
		sb.append(returnList(listExh, "eT"));
		sb.append(returnList(listExhName, "eN"));
		sb.append(returnList(listPara, "pT"));

//		System.out.println("idx text.len=" + sb.toString().length());
		File file = new File(fileXML.getAbsolutePath().replaceAll("\\.xml", "_idx.txt"));
		System.out.println("idx file is=" + file.getAbsolutePath());
		if (file.exists())
			file.delete();

		// idx file is file
		PrintWriter pw = new PrintWriter(file);
		pw.append(sb.toString());
		pw.close();

//		System.out.println("11 text.len=" + sb.toString().length());

		String tableName = "SOLRIDX_" + fileXML.getName().replaceAll("[_\\.xml-]+", "");
		String query = "DROP TABLE IF EXISTS " + tableName + ";" + "\r\n" + "CREATE TABLE " + tableName + "(\r\n"
				+ "  `sIdx` int(11) NOT NULL,\r\n" + "  `eIdx` int(11) NOT NULL,\r\n"
				+ "  `type` varchar(2) NOT NULL,\r\n" + "  PRIMARY KEY (sIdx,eIdx,type),\r\n" + "  KEY (type),\r\n"
				+ "  KEY `sIdx` (`sIdx`),\r\n" + "  KEY `eIdx` (`eIdx`)\r\n"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n";

		MysqlConnUtils.executeQuery(query);

		query = "LOAD DATA INFILE '" + file.getAbsolutePath().replaceAll("\\\\", "/") + "' ignore INTO TABLE "
				+ tableName + " FIELDS TERMINATED BY '||';";

		MysqlConnUtils.executeQuery(query + "\r\ndelete from " + tableName + " where sIdx=0 and eIdx=0;\r\n");

		query = "DROP TABLE IF EXISTS " + tableName + "1;\r\n" + "CREATE TABLE `" + tableName + "1` (\r\n"
				+ "  `sIdx` int(11) NOT NULL,\r\n" + "  `eIdx` int(11) NOT NULL,\r\n"
				+ "  `type` varchar(2) NOT NULL,\r\n" + "  `psIdx` int(11),\r\n" + "  `peIdx` int(11),\r\n"
				+ "  `pType` varchar(2),\r\n" + "  key(ptype)\r\n" + "  ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n"
				+ "\r\n" + "DROP TABLE IF EXISTS " + tableName + "2;\r\n" + "CREATE TABLE `" + tableName + "2` (\r\n"
				+ "  `sIdx` int(11) NOT NULL,\r\n" + "  `eIdx` int(11) NOT NULL,\r\n"
				+ "  `type` varchar(2) NOT NULL,\r\n" + "  `psIdx` int(11),\r\n" + "  `pEidx` int(11),\r\n"
				+ "  `dSidx` int(11),\r\n" + "  `dEidx` int(11)\r\n" + ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n"
				+ "\r\n" + "DROP TABLE IF EXISTS " + tableName + "3;\r\n" + "CREATE TABLE `" + tableName + "3` (\r\n"
				+ "  `ad` int(1) NOT NULL DEFAULT '0',\r\n" + "  `cmb` int(1) NOT NULL DEFAULT '0',\r\n"
				+ "  `sIdx` int(11) NOT NULL,\r\n" + "  `eIdx` int(11) NOT NULL,\r\n"
				+ "  `psIdx` int(11) DEFAULT NULL,\r\n" + "  `pEidx` int(11) DEFAULT NULL,\r\n"
				+ "  `dSidx` int(11) DEFAULT NULL,\r\n" + "  `dEidx` int(11) DEFAULT NULL,\r\n"
				+ "  key(pSidx,dSidx)\r\n" + ") ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
				+ "DROP TABLE IF EXISTS " + tableName + "4;\r\n" + "CREATE TABLE `" + tableName + "4` (\r\n"
				+ "  `pSidx` int(11) DEFAULT NULL,\r\n" + "  `peidx` int(11) DEFAULT NULL,\r\n"
				+ "  `ad` int(11) NOT NULL DEFAULT '0',\r\n" + "  `cmb` int(11) NOT NULL DEFAULT '0',\r\n"
				+ "  `sidx` int(11) NOT NULL,\r\n" + "  `eidx` int(11) NOT NULL,\r\n"
				+ "  `dsidx` int(11) NOT NULL,\r\n" + "  `deidx` int(11) NOT NULL\r\n"
				+ "  ) ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n" + "DROP TABLE IF EXISTS " + tableName
				+ "5;\r\n" + "CREATE TABLE `" + tableName + "5` (\r\n" + "  `row` int(11) NOT NULL,\r\n"
				+ "  `sIdx` int(11) NOT NULL,\r\n" + "  `eIdx` int(11) NOT NULL,\r\n"
				+ "  `pSIdx` int(11) NOT NULL DEFAULT '0',\r\n" + "  `pEIdx` int(11) NOT NULL DEFAULT '0',\r\n"
				+ "  `dSIdx` int(11) NOT NULL DEFAULT '0',\r\n" + "  `dEIdx` int(11) NOT NULL DEFAULT '0',\r\n"
				+ "  `eSIdx` int(11) NOT NULL DEFAULT '0',\r\n" + "  `eEIdx` int(11) NOT NULL DEFAULT '0',\r\n"
				+ "  primary key(psidx,peidx),\r\n" + "  key(pSidx),  \r\n" + "  key (`row`) )\r\n"
				+ " ENGINE=MyISAM DEFAULT CHARSET=latin1;\r\n" + "\r\n"
//				+ "/*identify paragraph associated with each section idx*/"
				+ "\r\n" + "INSERT IGNORE INTO " + tableName + "1\r\n"
				+ "select t1.*,t2.sIdx psIdx, t2.eIdx peIdx, t2.type pType from \r\n" + tableName + " t1 left join "
				+ tableName + " t2\r\n" + "on t1.sIdx < (t2.sIdx+t2.eIdx)/2 and t1.eIdx> (t2.sIdx+t2.eIdx)/2\r\n"
				+ "and t1.type = 'sT' and t2.type = 'pT' where t2.sIdx is not null order by t1.sIdx\r\n"
				+ ", t2.sIdx ;\r\n" + "ALTER TABLE " + tableName + "1 ADD KEY(pSidx),ADD KEY(pType), add key(\r\n"
				+ "TYPE);\r\n" + "\r\n"
//				+ "/*identify each definition associated with each paragraph. Unfortunately, also n\r\n"
//				+ "eed to find \r\n"
//				+ "where there are 2 or more defnitions in one para and break them into 2. Where 1 \r\n"
//				+ "definition\r\n" + "is in two paragraph I have already fixed it in java..*/\r\n" 
				+ "\r\n" + "INSERT IGNORE INTO " + tableName + "2\r\n" + "select \r\n"
				+ "t1.sIdx, t1.eIdx, t1.type, psIdx, pEidx,t2.sIdx dSidx, t2.eidx dEidx\r\n" + "from " + tableName
				+ "1 t1 left join " + tableName + " t2 \r\n"
				+ "on t1.psIdx < (t2.sIdx+t2.eIdx)/2 and t1.peIdx> (t2.sIdx+t2.eIdx)/2 and t1.ptype\r\n"
				+ " = 'pT' and t2.type rlike 'dT'\r\n" + "where pSidx is not null order by t1.psIdx, t2.sIdx;\r\n"
				+ "\r\n" + "set @pSidx = 0; set @pEIdx = 0; set @dSidx = 0; set @dEidx = 0;\r\n"
//				+ "/*mark where duplicate paragraphs which indicates 2 defs in same para*/"
				+ "\r\n" + "INSERT IGNORE INTO " + tableName + "3\r\n" + "select \r\n"
				+ "case when @pSidx = psIdx and @pEidx = pEidx then 1 else 0 end ad,\r\n"
				+ "case when @dEidx > pEidx then 1 else 0 end cmb,\r\n"
				+ "t1.sIdx, t1.eIdx, @psIdx:=psIdx psIdx, @pEidx:=pEidx pEidx, @dSidx:=dSidx dSidx,\r\n"
				+ " @dEidx:=dEidx dEidx\r\n" + " from " + tableName + "2 t1;\r\n" + "\r\n"
//				+ "/*this now substitutes the def idxs for the para when there are 2 in a def*/\r\n"
				+ "set @cmb=0; set @ad =0;\r\n" + "INSERT IGNORE INTO " + tableName + "4\r\n" + "select \r\n"
				+ "case when ad=1 or @ad=1 then dsidx else psidx end pSidx,\r\n"
				+ "case when ad=1 or @ad=1 then deidx else peidx end peidx,\r\n" + "\r\n"
				+ "@ad:=ad ad,@cmb:=cmb cmb,sidx,eidx,dsidx,deidx from \r\n" + "" + tableName + "3 t1\r\n"
				+ "  order by t1.psIdx , t1.dsIdx desc ;\r\n" + "\r\n" + "set @`row` = 0;\r\n" + "\r\n"
				+ "INSERT IGNORE INTO " + tableName + "5\r\n"
				+ "select @`row`:=@`row`+1 `row`, t1.sidx,t1.eidx,t1.psidx,t1.peidx,t1.dsidx,t1.deidx, (\r\n"
				+ "select sidx from " + tableName + " t2 \r\nwhere t2.type='eT' and (t1.pSidx\r\n"
				+ "+t1.pEidx)/2>t2.sidx\r\n" + "and (t1.pSidx+t1.pEidx)/2<t2.eidx) eSidx,\r\n" + "(select eidx from "
				+ tableName + " t2 where t2.type='eT' and (t1.pSidx+t1.pEidx)/2>t2.sidx\r\n"
				+ "and (t1.pSidx+t1.pEidx)/2<t2.eidx) eEidx\r\n" + "from " + tableName + "4 t1 order by psidx;\r\n"
				+ "\r\n" + "set @sIdx=0; set @eIdx=0; set @pSIdx=0; set @pEIdx=0; \r\n" + "\r\n" + "insert ignore into "
				+ tableName + "5\r\n" + "select `row`,sidx2,eidx2,psidx2,peidx2,dsidx,deidx,esidx,eeidx from (\r\n"
				+ "select `row`,case when `row`=1 then sidx else @sidx end sidx2\r\n"
				+ ",case when `row`=1 then eidx else @eidx end eidx2\r\n"
				+ ",case when psidx-@peidx>11 and @psidx!=psidx and @peidx!=peidx and `row`=1 then 0 \r\n"
				+ "when psidx-@peidx>11 and @psidx!=psidx and @peidx!=peidx and `row`!=1 then @peidx else -1 \r\n"
				+ "end psidx2,\r\n"
				+ "case when psidx-@peidx>11 and @psidx!=psidx and @peidx!=peidx and `row`=0 then psidx \r\n"
				+ "when psidx-@peidx>11 and @psidx!=psidx and @peidx!=peidx and `row`!=0 then psidx else -1 \r\n"
				+ "end peidx2,\r\n"
				+ "@sIdx:=sIdx sIdx,@eIdx:=eIdx eIdx,@pSIdx:=pSIdx pSIdx,@pEIdx:=pEIdx pEIdx\r\n,0 dsidx,0 deidx,0 esidx,0 eeidx\r\n"
				+ " from " + tableName + "5 order by `row`) t1 where psidx2!=-1;\r\n";

		MysqlConnUtils.executeQuery(query);

		// System.out.println("file==="+file.getAbsolutePath());
		File fileOfIndexes = new File(file.getAbsolutePath().replaceAll("\\\\", "//").replaceAll(".xml", ".txt"));
		if (fileOfIndexes.exists())
			fileOfIndexes.delete();
		// System.out.println("exporting fileOfIndexes to=="+fileOfIndexes);
		// col1=sectionIdx, col2= sectionEidx,col3=para sidx,col4=para eidx,col5=def
		// sidx, col6=def eidx, col7=exh sidx,col8=exh eidx
		query = "\r\nSELECT sidx,eidx,psidx,peidx,dsidx,deidx,esidx,eeidx INTO I " + "'" +

				fileOfIndexes.getAbsolutePath().replaceAll("\\\\", "//") + "' \rFIELDS TERMINATED BY '||' " + "\r FROM "
				+ tableName + "5 order by psidx;";

		MysqlConnUtils.executeQuery(query);

		query = "DROP TABLE IF EXISTS " + tableName + ";\r\n" + "DROP TABLE IF EXISTS " + tableName + "1;\r\n"
				+ "DROP TABLE IF EXISTS " + tableName + "2;\r\n" + "DROP TABLE IF EXISTS " + tableName + "3;\r\n"
				+ "DROP TABLE IF EXISTS " + tableName + "4;\r\n" + "DROP TABLE IF EXISTS " + tableName + "5;\r\n";

		MysqlConnUtils.executeQuery(query);

		System.out.println("returing fileOfIndexes=" + fileOfIndexes.getAbsolutePath());

		return fileOfIndexes;

	}

	public static String getTextWithOutSectionHeading(String text, String sectionHeading) {

		int secHdgLen = sectionHeading.length();
		String sectionHeadingChgd = sectionHeading.replaceAll("[ ]+", "").trim();
		String matchSecHdg = text.substring(0, Math.min(text.length(), secHdgLen)).replaceAll("[ ]+", "").trim();
		// xxxx delete
		// if(text.contains(tmpStrGlobal)) {
		// System.out.println("start @getTextWithOutSectionHeading..sent or
		// leadin="+text);
		// }

		if (sectionHeadingChgd.equals(matchSecHdg)) {
			text = text.substring(sectionHeading.length(), text.length()).trim();
		}

		return text;
	}

	public static List<String> retrieveSectionXmlTxt(String sectionText, String sectionHeading, int secCnt,
			List<String> listMetaData) {

		List<String> listSectionXml = new ArrayList<>();

		StringBuilder sbSec = new StringBuilder();
		String kId = kId = listMetaData.get(5);

		sectionHeading = sectionHeading.replaceAll("xxPD", "\\.").replaceAll("\\\\", "")
				.replaceAll("</?[dpes]{1}[TN]{1}>", "").trim();
		sectionText = sectionText.replaceAll("xxPD", "\\.").replaceAll("\\\\", "")
				.replaceAll("</?[dpes]{1}[TN]{1}>", "").replaceAll("[\r\n]{6,}", "\r\n\r\n").trim();
		sectionText = getTextWithOutSectionHeading(sectionText, sectionHeading);

		secCnt++;
		if (secCnt == 0) {
			secCnt++;
		}
		String cik = listMetaData.get(0), fileDate = listMetaData.get(1), filer = listMetaData.get(2),
				contractLongName = listMetaData.get(3), fileSize = listMetaData.get(4),
				contractType = listMetaData.get(6);

		if (SolrPrep.sbSec && secCnt == 0) {// metadata here
			sbSec.append(solrFieldDoc);
			sbSec.append(solrFieldId + kId + "_" + secCnt + solrFieldEnd);
			sbSec.append(solrFieldKid + kId + solrFieldEnd);
			sbSec.append(solrFieldContractLongSECname + solrFieldCDATA
					+ contractLongName.replaceAll("[\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,4}", "").toUpperCase().trim()
					+ solrFieldCDATAEnd + solrFieldEnd);
			sbSec.append(
					solrFieldContractType + solrFieldCDATA + contractType.trim() + solrFieldCDATAEnd + solrFieldEnd);
			sbSec.append(solrFieldFiler + solrFieldCDATA + filer.trim() + solrFieldCDATAEnd + solrFieldEnd);
			sbSec.append(solrFieldCIK + cik.trim() + solrFieldEnd);
			sbSec.append(solrFieldFileSize + sectionText.trim().length() + solrFieldEnd);
			sbSec.append(solrFieldFileDate + fileDate.trim() + solrFieldEnd);
			sbSec.append(solrFieldTextType + "0" + solrFieldEnd);
			sbSec.append(solrFieldTextCount + secCnt + solrFieldEnd);
			sbSec.append(solrFieldComment + solrFieldCDATA + "comments: This is Section capture because typ=0. "
					+ "The id is the id, contract# and section cnt# (same as field cnt)."
					+ "Where this a section heading this will in all likelihood mean the document "
					+ "is also broken into sections." + solrFieldCDATAEnd + solrFieldEnd);
			sbSec.append(solrFieldDocEnd);
		}

		if (SolrPrep.sbSec) {
			sbSec.append(solrFieldDoc);
			sbSec.append(solrFieldId + kId + "_" + secCnt + solrFieldEnd);
//			sbSec.append(solrFieldKid + kId + solrFieldEnd);
//			sbSec.append(solrFieldContractLongSECname + solrFieldCDATA
//					+ contractLongName.replaceAll("[\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,4}", "").toUpperCase().trim()
//					+ solrFieldCDATAEnd + solrFieldEnd);
//			sbSec.append(
//					solrFieldContractType + solrFieldCDATA + contractType.trim() + solrFieldCDATAEnd + solrFieldEnd);
//			sbSec.append(solrFieldFiler + solrFieldCDATA + filer.trim() + solrFieldCDATAEnd + solrFieldEnd);
//			sbSec.append(solrFieldCIK + cik.trim() + solrFieldEnd);
//			sbSec.append(solrFieldFileSize + sectionText.trim().length() + solrFieldEnd);
//			sbSec.append(solrFieldFileDate + fileDate.trim() + solrFieldEnd);
			sbSec.append(solrFieldTextCount + secCnt + solrFieldEnd);
			sbSec.append(solrFieldTextType + "0" + solrFieldEnd);
			sbSec.append(solrFieldWordCount + sectionText.split("[ ]+").length + "" + solrFieldEnd);
			sbSec.append(solrFieldSectionHeading + solrFieldCDATA + sectionHeading + solrFieldCDATAEnd + solrFieldEnd);
			sbSec.append(
					solrFieldText + solrFieldCDATA + sectionText.replaceAll("xxPD", "\\.").replaceAll("\\\\", "").trim()
							+ solrFieldCDATAEnd + solrFieldEnd);
			sbSec.append(solrFieldDocEnd);

		}

		listSectionXml.add(sbSec.toString());
		listSectionXml.add(secCnt + "");

		return listSectionXml;

	}

	public static boolean isItaSubheading(String text) {
		boolean mostlyInitialCaps = false;

		NLP nlp = new NLP();

		text = StopWords.removeStopWords(text).replaceAll("and|or", "");
		if (text.split(" ").length > 20)
			return false;
		double cntInitialCapsWords = nlp.getAllIndexStartLocations(text, Pattern.compile("( |^)[A-Z]")).size();
		double cntWords = text.split(" ").length;
//		System.out.println("cntInitialCapsWords="+cntInitialCapsWords+" cntWords="+cntWords);

		if (cntInitialCapsWords / cntWords < .65)
			return false;
		return true;

	}

	public static List<String> solrFileFinalized(File fileOfIndexes, String text, List<String> listMetaData)
			throws IOException {

		/*
		 * System.out.
		 * println("this method - solrFileFinalized - saves the xml files sentence.xml \r\n"
		 * "and paragraph.xml - fields are homogenized - eg 'txt' is either sentence or \r\n"
		 * "paragraph or later parent/child or clause or section. What designates which is\r\n"
		 * "which in xml is 'typ'.  In addition the ID will designate relationships b/w para and sent\r\n"
		 * "as will cnt (cnt in sent is para #). Don't use this method to grab clauses or \r\n"
		 * "anything else - pull the xml files or use the stripped final text or listSec \r\n"
		 * "which has section markers. Or paragraph.xml if I want to run parent/child relations\r\n"
		 * "for paragraphs or later from section.xml. If I want clauses but need to do it off\r\n"
		 * + "of paragraphs use pargraph.xml and so on.");
		 */
		// I also need solr path in listMetaData
		// listMetaData ==
		// cik=0,filedate=1,filer=2,contractLongName=3,fileSize=4,kId=5,
		// contractName=6
		String cik = listMetaData.get(0), fileDate = listMetaData.get(1), filer = listMetaData.get(2),
				contractLongName = listMetaData.get(3), fileSize = listMetaData.get(4), kId = listMetaData.get(5),
				contractType = listMetaData.get(6);

		String leadIn = "", leadInToSave = "";
		// PrintWriter pw = new PrintWriter(new
		// File("e:/getContracts/temp/tmpIdxCuts.txt"));
		String revisedSolrText = "", revisedSolrTextPrior = "";

		NLP nlp = new NLP();
		// System.out.println("1 text.snip="+text.substring(7282,7353));
		// System.out.println("2 text.snip="+text.substring(7353,7418));

		// PrintWriter pw = new PrintWriter(new
		// File("e:/getContracts/temp/finalText.txt"));
		// pw.append(text);
		// pw.close();

		System.out.println("fileOfIndexes=" + fileOfIndexes.getAbsolutePath());
		// System.out.println("solrFileFinalized 12 text.len="+text.length());
		String idxText = Utils.readTextFromFile(fileOfIndexes.getAbsolutePath()).replaceAll("\\\\N", "");

		String[] lines = idxText.split("\r\n");
		String line = "", sectionHeading = "", sectionHeadingPrior = "", section = "", definitionName = "",
				definition = "", exhibitName = "", exhibitNameSub = "", exhStrUnform = "", solrText = "",
				solrTextPOS = "", solrTextStopped = "", solrTextStoppedPOS = "", exhibit = "",
				sentSolrFieldSameAsPara = "";

		List<String> listSectionXml = new ArrayList<>();

		int sIdx = 0, eIdx = 0, pSidx = 0, pEidx = 0, priorPeidx = 0, dSidx = 0, dEidx = 0, eSidx = 0, eEidx = 0,
				priorSidx = -1, priorEidx = -1, priorEsidx = -1, priorEeidx = -1, priorDsidx = -1, priorDeidx = -1;
		// System.out.println("lines.len="+lines.length);
		StringBuilder sbSec = new StringBuilder(); // for sections xml
		StringBuilder sbPara = new StringBuilder();
		StringBuilder sbParChi = new StringBuilder();
		StringBuilder sbSent = new StringBuilder();
		StringBuilder sbClause = new StringBuilder();
		StringBuilder sbTmp = new StringBuilder();
		// col1=sectionIdx, col2= sectionEidx,col3=para sidx,col4=para eidx,col5=def
		// sidx, col6=def eidx, col7=exh sidx,col8=exh eidx (3/4 can be sent or clause)

		// each row has the start and end idx for each field - for each para/sent or
		// clause I fetch sectionName, definitionName and exhibit name. section start
		// and end idx will always have values as will paras. But section name will be
		// blank in many cases. For definitions and and exhibits if the values are zero
		// - then there is no related definition names.

		String sentence = "", sentencePrior = "", idPara = "", idParChi = "", idSent = "", parChiSolr = "",
				parChiStr = "", lineage = "", parChiLead = "", linP = "", paraHdg = "";
		int parChiCnt = 0, cntParaHdg = 0, sentCnt = -1, paraCnt = 0, secCnt = -1, paraCntPrior;// leave paraCnt=0,and
																								// claus&sent@0
		if (SolrPrep.sbSec)
			sbSec.append(solrFieldAdd);

		sbPara.append(solrFieldAdd);

		if (SolrPrep.sbParChi)
			sbParChi.append(solrFieldAdd);

		sbSent.append(solrFieldAdd);
		// System.out.println("aa text snip="+text.substring(7418,16003)+"|");

		boolean leadInExists = false, mainlyInitialCaps = false, leadInFound = false, removeSectionHeading = true;
		int cntRev = 0;

		for (int i = 0; i < lines.length; i++) {
			paraHdg = "";
			paraCntPrior = paraCnt;
			cntParaHdg = 0;
			revisedSolrText = "";
			sentence = "";
			leadInToSave = "";

			leadInExists = false;
			leadInFound = false;

			solrText = "";
			definitionName = "";
			line = lines[i];
			// System.out.println("idx line==" + line);
			String[] cells = line.split("\\|\\|");
			if (cells[0].trim().length() == 0)
				continue;
			sIdx = Integer.parseInt(cells[0].replaceAll("[A-Za-z]+", "")); // sections
			eIdx = Integer.parseInt(cells[1].replaceAll("[A-Za-z]+", ""));
			pSidx = Integer.parseInt(cells[2].replaceAll("[A-Za-z]+", ""));// paragraphs
			pEidx = Integer.parseInt(cells[3].replaceAll("[A-Za-z]+", ""));
			dSidx = Integer.parseInt(cells[4].replaceAll("[A-Za-z]+", ""));// definitions
			dEidx = Integer.parseInt(cells[5].replaceAll("[A-Za-z]+", ""));
			eSidx = Integer.parseInt(cells[6].replaceAll("[A-Za-z]+", ""));// exhibits
			eEidx = Integer.parseInt(cells[7].replaceAll("[A-Za-z]+", ""));
			;

			int cntSec = 0, cntDef = 0, cntExh = 0;

			if (priorSidx != sIdx || priorEidx != eIdx) {
				// if new section
				sectionHeading = "";
				section = text.substring(sIdx, eIdx);
				List<String> list = nlp.getAllMatchedGroups(section, Pattern.compile("(?sm)(?<=<sN>).*?(?=</sN>)"));
				if (list.size() > 0) {
					sectionHeading = list.get(0).replaceAll("</?[psed]{1}[NT]{1}>", "").replaceAll("[\r\n\t]+", " ")
							.replaceAll("[ ]+", " ").trim();

					if (sectionHeading.replaceAll("[ \\.]+", "").length() > 1) {
						cntSec++;
					}
				} else
					sectionHeading = "";

				// for section.xml - b/c definitions and exhibits may be subsets I can't align
				// them to any section - so I just pass as a parameter sectionHeading

				listSectionXml = retrieveSectionXmlTxt(text.substring(sIdx, eIdx), sectionHeading, secCnt,
						listMetaData);

				if (SolrPrep.sbSec) {
					sbSec.append(listSectionXml.get(0));
				}
				secCnt = Integer.parseInt(listSectionXml.get(1));

			}

			if (priorEsidx != eSidx || priorEeidx != eEidx) {
				exhibitName = "";
				exhibit = text.substring(eSidx, eEidx);
				List<String> list = nlp.getAllMatchedGroups(exhibit, Pattern.compile("(?sm)(?<=<eN>).*?(?=</eN>)"));
				if (list.size() > 0) {
					exhStrUnform = list.get(0);

					exhibitName = list.get(0).replaceAll("</?[psed]{1}[NT]{1}>", "").replaceAll("[\r\n]+", " ")
							.replaceAll("[ ]+", " ").replaceAll("$,", "").trim();
					cntExh++;
				} else
					exhibitName = "";
			}

			if (priorDsidx != dSidx || priorDeidx != dEidx) {
				definitionName = "";
				definition = text.substring(dSidx, dEidx);
				List<String> list = nlp.getAllMatchedGroups(definition, Pattern.compile("(?sm)(?<=<dN>).*?(?=</dN>)"));
				if (list.size() > 0) {
					definitionName = list.get(0).replaceAll("</?[psed]{1}[NT]{1}>", "").replaceAll("[\r\n]+", " ")
							.replaceAll("[ ]+", " ").replaceAll("(?sm)the term ", "").replaceAll("$,", "").trim();
					cntDef++;
				} else
					definitionName = "";
			}

			priorSidx = sIdx;
			priorEidx = eIdx;
			priorEsidx = eSidx;
			priorEeidx = eEidx;
			priorPeidx = pEidx;

			solrText = text.substring(pSidx, pEidx).replaceAll("\\\\", "");

			if (solrText.replaceAll("</?[psed]{1}[NT]{1}>", "").replaceAll("[\r\n]+", " ").replaceAll("[ ]+", " ")
					.replaceAll("$,", "").trim().length() < 1) {
				continue;
			}

			// System.out.println("aa pSidx="+pSidx+" pEidx="+pEidx);
			if (sectionHeading.length() > 0 && cntSec == 1) {
				solrText = solrText.replaceAll("(?sm)<sN>.*?</sN>", "").trim();
			}

			if (definitionName.length() > 0 && cntDef == 1) {
				solrText = solrText.replaceAll("(?sm)<dN>.*?</dN>", "").trim();
			}

			if (exhibitName.length() > 0 && cntExh == 1) {
				solrText = solrText.replaceAll("(?sm)<eN>.+?</eN>", "").trim();
			}

			solrText = solrText.replaceAll("</?[psed]{1}[NT]{1}>", "").replaceAll("[\r\n]{3,}", "\r\n\r\n")
					.replaceAll("[ ]+", " ").replaceAll("\''", "\"").trim();
			if (solrText.length() < 1) {
				continue;
			}

			cntRev++;
			revisedSolrText = "";
			// sometimes there's a section heading that didn't break off from the para - so
			// in that case - keep and don't worry about it showinup in para.
			if (!sectionHeadingPrior.equals(sectionHeading) && solrText.trim().length() - 5 < sectionHeading.length()) {

				revisedSolrText = sectionHeading;

				cntRev = 0;
				// if new sectionHeading - then need to record it somewhere as para or sent -
				// can't ignore heading as text. But to not corrupt text - given it is just a
				// heading I expell it from the text by just including it here
			}

			removeSectionHeading = true;
			if (!sectionHeadingPrior.equals(sectionHeading) && solrText.trim().length() > sectionHeading.length() + 4) {
				removeSectionHeading = false;
			}

			if (cntRev != 0 && removeSectionHeading) {
				revisedSolrText = getTextWithOutSectionHeading(solrText, sectionHeading);
			}

			sectionHeadingPrior = sectionHeading;

			if (solrText.replaceAll("[\r\n\t ]+", "").length() < 1) {
				revisedSolrTextPrior = revisedSolrText;
				continue;
			}

			if (revisedSolrText.replaceAll("[\r\n\t ]+", "").length() < 1 && solrText.trim().length() > 0) {
				revisedSolrText = solrText;
				// System.out.println("9r* it is...cntRev=" + cntRev + "repl revisedSolrText=" +
				// revisedSolrText);
			}

			if (solrText.trim().length() < 1 && revisedSolrText.replaceAll("[\r\n\t ]+", "").length() < 1) {
				revisedSolrTextPrior = revisedSolrText;
				continue;
			}

			// b/c metadata repeats - I only put it in the first solr doc of that K.
			// Thereafter just the Id.
			if (paraCnt == 0) {// metadata here
				idPara = kId + "_" + secCnt + "_" + paraCnt;
				sbPara.append(solrFieldDoc);
				sbPara.append(solrFieldId + idPara + solrFieldEnd);
				sbPara.append(solrFieldKid + kId + solrFieldEnd);
				sbPara.append(solrFieldSecNo + "0" + solrFieldEnd);
				sbPara.append(solrFieldTextCount + paraCnt + solrFieldEnd);
				sbPara.append(solrFieldTextType + "1" + solrFieldEnd);
				sbPara.append(solrFieldContractLongSECname + solrFieldCDATA
						+ contractLongName.replaceAll("[\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,4}", "").toUpperCase().trim()
						+ solrFieldCDATAEnd + solrFieldEnd);
				sbPara.append(solrFieldContractType + solrFieldCDATA + contractType.trim() + solrFieldCDATAEnd
						+ solrFieldEnd);
				sbPara.append(solrFieldFiler + solrFieldCDATA + filer.trim() + solrFieldCDATAEnd + solrFieldEnd);
				sbPara.append(solrFieldCIK + cik.trim() + solrFieldEnd);
				sbPara.append(solrFieldFileSize + text.trim().length() + solrFieldEnd);
				sbPara.append(solrFieldFileDate + fileDate.trim() + solrFieldEnd);
				sbPara.append(solrFieldComment + solrFieldCDATA + "comments: This is paragraph capture because typ=1. "
						+ "The id is the id, contract# and section # and para cnt# (same as field cnt)."
						+ solrFieldCDATAEnd + solrFieldEnd);
				sbPara.append(solrFieldDocEnd);
				paraCnt++;
			}

			// solrText = solrText; don't .replaceAll("xxPD", "\\.") until insert into xml
			sentSolrFieldSameAsPara = "";

			idPara = kId + "_" + secCnt + "_" + paraCnt;
			if (!revisedSolrText.equals(revisedSolrTextPrior)) {
				sbPara.append(solrFieldDoc);
				sbPara.append(solrFieldId + idPara + solrFieldEnd);
//				sbPara.append(solrFieldKid + kId + solrFieldEnd);
//				sbPara.append(solrFieldContractLongSECname + solrFieldCDATA + contractLongName
//						.replaceAll("[\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,4}", "").toUpperCase().trim()
//						+ solrFieldCDATAEnd + solrFieldEnd);
//				sbPara.append(solrFieldContractType + solrFieldCDATA + contractType.trim() + solrFieldCDATAEnd
//						+ solrFieldEnd);
//				sbPara.append(solrFieldFiler + solrFieldCDATA + filer.trim() + solrFieldCDATAEnd + solrFieldEnd);
//				sbPara.append(solrFieldCIK + cik.trim() + solrFieldEnd);
//				sbPara.append(solrFieldFileSize + text.trim().length() + solrFieldEnd);
//				sbPara.append(solrFieldFileDate + fileDate.trim() + solrFieldEnd);
				sbPara.append(solrFieldSecNo + secCnt + solrFieldEnd);
				sbPara.append(solrFieldTextCount + paraCnt + solrFieldEnd);

//				if(revisedSolrText.contains("\"QIB\" means any \"qualified institutional buyer\" as defined in Rule 144A.")) {
//					System.out.println("sent========="+sentence);
//				}

				if (isItTableOfContents(revisedSolrText)) {
					sbPara.append(solrFieldTextType + "-1" + solrFieldEnd);
				} else {
					sbPara.append(solrFieldTextType + "1" + solrFieldEnd);
				}
				sbPara.append(solrFieldWordCount + revisedSolrText.split("[ ]+").length + "" + solrFieldEnd);
				paraCnt++;

			}
			// sentSolrFieldSameAsPara = sentSolrFieldSameAsPara;

			if (definitionName.length() > 0) {
				if (!revisedSolrText.equals(revisedSolrTextPrior)) {
					sbPara.append(solrFieldDefinitionHeading + solrFieldCDATA
							+ definitionName.replaceAll("xxPD", "\\.").trim().replaceAll("\\\\|:$", "")
							+ solrFieldCDATAEnd + solrFieldEnd);
				}

				sentSolrFieldSameAsPara = sentSolrFieldSameAsPara + solrFieldDefinitionHeading + solrFieldCDATA
						+ definitionName.replaceAll("xxPD", "\\.").trim().replaceAll("\\\\|:$", "") + solrFieldCDATAEnd
						+ solrFieldEnd;
			}

			if (sectionHeading.length() > 0) {
				if (!revisedSolrText.equals(revisedSolrTextPrior)) {
					sbPara.append(solrFieldSectionHeading + solrFieldCDATA
							+ sectionHeading.replaceAll("xxPD", "\\.").replaceAll("\\\\", "").trim() + solrFieldCDATAEnd
							+ solrFieldEnd);
				}

				sentSolrFieldSameAsPara = sentSolrFieldSameAsPara + solrFieldSectionHeading + solrFieldCDATA
						+ sectionHeading.replaceAll("xxPD", "\\.").replaceAll("\\\\", "").trim() + solrFieldCDATAEnd
						+ solrFieldEnd;
			}

			if (exhibitName.length() > 0) {
				if (!revisedSolrText.equals(revisedSolrTextPrior)) {

					if (exhibitName.contains("parentExhibitName=")) {
						exhibitName = exhibitName.replace("parentExhibitName=", "");
						if (exhibitName.contains("subExhibitName=")) {
							exhibitNameSub = exhibitName.substring(exhibitName.indexOf("subExhibitName="))
									.replace("subExhibitName=", "");
							exhibitName = exhibitName.substring(0, exhibitName.indexOf("subExhibitName="))
									.replace("subExhibitName=", "").replace("parentExhibitName=", "");
//							System.out.println("parentExhibitName=" + exhibitName);
//							System.out.println("subExhibitName=" + exhibitNameSub);
						}
						sbPara.append(solrFieldExhibitHeading + solrFieldCDATA
								+ exhibitName.replaceAll("xxPD", "\\.").replaceAll("\\\\", "").trim()
								+ solrFieldCDATAEnd + solrFieldEnd);
						sbPara.append(solrFieldExhibitHeadingSub + solrFieldCDATA
								+ exhibitNameSub.replaceAll("xxPD", "\\.").replaceAll("\\\\", "").trim()
								+ solrFieldCDATAEnd + solrFieldEnd);

						sentSolrFieldSameAsPara = sentSolrFieldSameAsPara + solrFieldExhibitHeading + solrFieldCDATA
								+ exhibitName.replaceAll("xxPD", "\\.").replaceAll("\\\\", "").trim()
								+ solrFieldCDATAEnd + solrFieldEnd + solrFieldExhibitHeadingSub + solrFieldCDATA
								+ exhibitNameSub.replaceAll("xxPD", "\\.").replaceAll("\\\\", "").trim()
								+ solrFieldCDATAEnd + solrFieldEnd;
					} else {
						sbPara.append(solrFieldExhibitHeading + solrFieldCDATA
								+ exhibitName.replaceAll("xxPD", "\\.").replaceAll("\\\\", "").trim()
								+ solrFieldCDATAEnd + solrFieldEnd);

						sentSolrFieldSameAsPara = sentSolrFieldSameAsPara + solrFieldExhibitHeading + solrFieldCDATA
								+ exhibitName.replaceAll("xxPD", "\\.").replaceAll("\\\\", "").trim()
								+ solrFieldCDATAEnd + solrFieldEnd;
					}
				}

			}

			leadIn = getLeadInClause(solrText.trim()).replaceAll("\\\\", "");
			// a leadin clause is the first part of sentence - b/c it acts for multi-clauses
			// - eg a list or a paragraph with sub-numerals
			if (leadIn.length() > sectionHeading.length()) {
				leadIn = getTextWithOutSectionHeading(leadIn, sectionHeading);
			}

			if (leadIn.length() > 15 && leadIn.length() < 1500 && leadIn.split("\r\n").length < 18) {
				leadInExists = true;
			}

			if (!revisedSolrText.equals(revisedSolrTextPrior)) {
				sbPara.append(solrFieldText + solrFieldCDATA + revisedSolrText.replaceAll("xxPD", "\\.").trim()
						+ solrFieldCDATAEnd + solrFieldEnd);
				sbPara.append(solrFieldDocEnd);
			}

			revisedSolrTextPrior = revisedSolrText.replaceAll("xxPD", "\\.").trim();

			if (SolrPrep.sbParChi) {
				List<String> listParentChild = getParentChild(
						revisedSolrText.replaceAll("xxPD", "\\.").trim().replaceAll("^ ?\\([a-zA-Z\\d]{1,6}\\)", "")

				);

				// we no longer need parChi - have redone with new method. BUT NOT AN ISSUE. B/c
				// with russian doll approach we can always feed back to the xml text.
				if (parChiCnt == 0 && listParentChild.size() > 0) {
					idParChi = idPara + "_p" + parChiCnt;
					sbParChi.append(solrFieldDoc);
					sbParChi.append(solrFieldId + idParChi + solrFieldEnd);
					sbParChi.append(solrFieldKid + kId + solrFieldEnd);
					sbParChi.append(solrFieldSecNo + "0" + solrFieldEnd);
					sbParChi.append(solrFieldParaNo + "0" + solrFieldEnd);
					sbParChi.append(solrFieldTextCount + paraCnt + solrFieldEnd);
					sbParChi.append(solrFieldTextType + "2" + solrFieldEnd);
					sbParChi.append(solrFieldContractLongSECname + solrFieldCDATA + contractLongName
							.replaceAll("[\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,4}", "").toUpperCase().trim()
							+ solrFieldCDATAEnd + solrFieldEnd);
					sbParChi.append(solrFieldContractType + solrFieldCDATA + contractType.trim() + solrFieldCDATAEnd
							+ solrFieldEnd);
					sbParChi.append(solrFieldFiler + solrFieldCDATA + filer.trim() + solrFieldCDATAEnd + solrFieldEnd);
					sbParChi.append(solrFieldCIK + cik.trim() + solrFieldEnd);
					sbParChi.append(solrFieldFileSize + text.trim().length() + solrFieldEnd);
					sbParChi.append(solrFieldFileDate + fileDate.trim() + solrFieldEnd);
					sbParChi.append(
							solrFieldComment + solrFieldCDATA + "comments: This is parent child capture because typ=2. "
									+ "The id is the id, contract# and section # and para and parent child cnt# "
									+ "preceded by 'p'. lineage is parent child identifier - eg p1c3, parentLead "
									+ "is the parent txt w/o the child embedded in it. if there is a parentLead field "
									+ "then pOrC=c - and that means the txt is child txt. if pOrC=p then txt is full "
									+ "parent txt (eg text of both p1 and c1)." + solrFieldCDATAEnd + solrFieldEnd);
					sbParChi.append(solrFieldDocEnd);

				}

				for (int z = 0; z < listParentChild.size(); z++) {
					idParChi = idPara + "_p" + parChiCnt;
					parChiStr = listParentChild.get(z).trim();
					parChiSolr = parChiFields(parChiStr).trim();
					if (parChiStr.length() == 0 && parChiSolr.length() == 0)
						continue;

					sbParChi.append(solrFieldDoc);
					parChiCnt++;

					if (parChiSolr.length() > 0) {
						sbParChi.append(parChiSolr);
						if (isItTableOfContents(parChiSolr)) {
							sbParChi.append(solrFieldTextType + "-1" + solrFieldEnd);
						} else {
							sbParChi.append(solrFieldTextType + "2" + solrFieldEnd);
						}
					}

					if (parChiSolr.length() == 0) {
						sbParChi.append(
								solrFieldText + solrFieldCDATA + parChiStr + solrFieldCDATA + solrFieldCDATAEnd);
						if (isItTableOfContents(parChiStr)) {
							sbParChi.append(solrFieldTextType + "-1" + solrFieldEnd);
						} else {
							sbParChi.append(solrFieldTextType + "2" + solrFieldEnd);
						}
					}

					sbParChi.append(solrFieldId + idParChi + solrFieldEnd);
//				sbParChi.append(solrFieldKid + kId + solrFieldEnd);
//				sbParChi.append(solrFieldContractLongSECname + solrFieldCDATA + contractLongName
//						.replaceAll("[\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,4}", "").toUpperCase().trim()
//						+ solrFieldCDATAEnd + solrFieldEnd);
//				sbParChi.append(solrFieldContractType + solrFieldCDATA + contractType.trim() + solrFieldCDATAEnd
//						+ solrFieldEnd);
//				sbParChi.append(solrFieldFiler + solrFieldCDATA + filer.trim() + solrFieldCDATAEnd + solrFieldEnd);
//				sbParChi.append(solrFieldCIK + cik.trim() + solrFieldEnd);
//				sbParChi.append(solrFieldFileSize + text.trim().length() + solrFieldEnd);
//				sbParChi.append(solrFieldFileDate + fileDate.trim() + solrFieldEnd);
					sbParChi.append(solrFieldSecNo + secCnt + solrFieldEnd);
					sbParChi.append(solrFieldParaNo + paraCnt + solrFieldEnd);
					sbParChi.append(solrFieldTextCount + parChiCnt + solrFieldEnd);

					if (listParentChild.size() == 1) {
						sbParChi.append(solrFieldDuplicate + "0" + solrFieldEnd);// parChi same as para=0
					}
					if (listParentChild.size() > 1) {
						sbParChi.append(solrFieldDuplicate + "1" + solrFieldEnd);//
					}

					sbParChi.append(sentSolrFieldSameAsPara);
					sbParChi.append(solrFieldDocEnd);
				}
			}

			List<String> listSentence = getSentences(solrText.trim(), sectionHeading);

			if (nlp.getAllMatchedGroups(solrText.trim(), Pattern.compile("CalculationX")).size() > 0) {
				System.out
						.println("solrText para contains calculation agent. listSentence.size--" + listSentence.size());
//				System.out.println("solrText para contains calculation agent=="+solrText.trim()+"||||");
			}

			// NOTE: if leadin is second sentence (or third etc). Then if 2nd sentence 1st
			// sentence should not have a leadin, 2nd sentence is just a leadin and all
			// sentences thereafter have leadin.

			if (listSentence.size() < 2) {
				sentence = solrText.trim();

				if (sentence.length() < 1 || (sentence.replaceAll("xxPD", "\\.").trim().equals(sentencePrior)
						&& sentencePrior.length() > 0)) {
					continue;
				}

				sentCnt++;
				idSent = idPara + "_" + sentCnt;
				// Id format is kId (accno) - k# (how many Ks are there for this accno - this is
				// the # ) sec#_para#_sent#
				if (sentCnt == 0) {// metadata here
					sbSent.append(solrFieldDoc + solrFieldId + idSent + solrFieldEnd);
					sbSent.append(solrFieldKid + kId + solrFieldEnd);
					sbSent.append(solrFieldContractLongSECname + solrFieldCDATA + contractLongName
							.replaceAll("[\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,4}", "").toUpperCase().trim()
							+ solrFieldCDATAEnd + solrFieldEnd);
					sbSent.append(solrFieldContractType + solrFieldCDATA + contractType.trim() + solrFieldCDATAEnd
							+ solrFieldEnd);
					sbSent.append(solrFieldFiler + solrFieldCDATA + filer.trim() + solrFieldCDATAEnd + solrFieldEnd);
					sbSent.append(solrFieldCIK + cik.trim() + solrFieldEnd);
					sbSent.append(solrFieldFileSize + text.trim().length() + solrFieldEnd);
					sbSent.append(solrFieldFileDate + fileDate.trim() + solrFieldEnd);
					sbSent.append(solrFieldSecNo + "0" + solrFieldEnd);
					sbSent.append(solrFieldParaNo + "0" + solrFieldEnd);
					sbSent.append(solrFieldParChiNo + "0" + solrFieldEnd);
					sbSent.append(solrFieldTextCount + sentCnt + solrFieldEnd);
					sbSent.append(solrFieldTextType + "3" + solrFieldEnd);
					sbSent.append(solrFieldComment
							+ "the txt type is 3 b/c it is sentence (sec typ is 0 and para=1,parent \r\n"
							+ "child =2 and sent=3. id is kId,secID,paraId,sentId" + solrFieldEnd);
					sbSent.append(solrFieldDocEnd);
					sentCnt++;
				}

				sbSent.append(solrFieldDoc + solrFieldId + idSent + solrFieldEnd);
//				sbSent.append(solrFieldKid + kId + solrFieldEnd);
//				sbSent.append(solrFieldContractLongSECname + solrFieldCDATA + contractLongName
//						.replaceAll("[\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,4}", "").toUpperCase().trim()
//						+ solrFieldCDATAEnd + solrFieldEnd);
//				sbSent.append(solrFieldContractType + solrFieldCDATA + contractType.trim() + solrFieldCDATAEnd
//						+ solrFieldEnd);
//				sbSent.append(solrFieldFiler + solrFieldCDATA + filer.trim() + solrFieldCDATAEnd + solrFieldEnd);
//				sbSent.append(solrFieldCIK + cik.trim() + solrFieldEnd);
//				sbSent.append(solrFieldFileSize + text.trim().length() + solrFieldEnd);
//				sbSent.append(solrFieldFileDate + fileDate.trim() + solrFieldEnd);
				sbSent.append(solrFieldSecNo + secCnt + solrFieldEnd);
				sbSent.append(solrFieldParaNo + (paraCnt - 1) + solrFieldEnd);
//				sbSent.append(solrFieldParChiNo + (parChiCnt - 1) + solrFieldEnd);
				sbSent.append(solrFieldTextCount + sentCnt + solrFieldEnd);
				sbSent.append(solrFieldDuplicate + "0" + solrFieldEnd);// sentence same as para=0
				sbSent.append(solrFieldWordCount + sentence.split("[ ]+").length + "" + solrFieldEnd);
				sbSent.append(sentSolrFieldSameAsPara);

				/*
				 * If there is a leadin in para but it occurs after the first or second sentence
				 * in that paragraph then for each sentence I search to see if a ':' exists -
				 * and if it does - then I get the leadInClause from paragraph (solrText) not
				 * the sentnece. If it does I populate leadin value that gets sbSent.appended to
				 * xml file.
				 */

				if (leadInExists && nlp.getAllIndexEndLocations(sentence, Pattern.compile("(?<!\"):")).size() > 0) {
					// leadin==sentence

					leadInToSave = getLeadInClause(solrText.replaceAll("xxPD", "\\.").trim() + "\r\n");
					List<String> list = SolrPrep.getSentences(leadInToSave, "");

					if (list.size() > 0) {
						leadInToSave = list.get(list.size() - 1);
						leadInToSave = getTextWithOutSectionHeading(leadInToSave, sectionHeading);
					}
				}

				if (leadInToSave.length() > 0) {

					sbSent.append(solrFieldLead + solrFieldCDATA + leadInToSave.replaceAll("xxPD", "\\.").trim()
							+ solrFieldCDATAEnd + solrFieldEnd);
//					sbSent.append(solrFieldLeadNoDef + solrFieldCDATA 
//							+ removeDefinitions(leadInToSave.replaceAll("xxPD", "\\.")).trim()
//							+ solrFieldCDATAEnd + solrFieldEnd);

//					sbSent.append(solrFieldLeadStopStem + solrFieldCDATA
//							+ Stemmer.stemmedOutPut(StopWords.removeStopWords(leadInToSave.replaceAll("xxPD", "\\.").trim())) + solrFieldCDATAEnd
//							+ solrFieldEnd);

//					sbSent.append(solrFieldLeadStopNoDefStem + solrFieldCDATA
//							+ Stemmer.stemmedOutPut(removeDefinitions(StopWords.removeStopWords(leadInToSave.replaceAll("xxPD", "\\.").trim())))
//							+ solrFieldCDATAEnd + solrFieldEnd);

//					if(sbPOS) {
//					sbSent.append(solrFieldLeadStopNoDefPOS + solrFieldCDATA
//							+ getPartsOfSpeech(removeDefinitions((StopWords.removeStopWords(leadInToSave.replaceAll("xxPD", "\\.").trim()))))
//							+ solrFieldCDATAEnd + solrFieldEnd);
//					}
				}

				if (isItTableOfContents(sentence.replaceAll("xxPD", "\\.").trim())
//						|| nlp.getAllMatchedGroups(sentence, Pattern.compile("\r\n\r\n")).size()>2
//						|| shortLines(sentence.replaceAll("xxPD", "\\.").trim())<7
				) {
					sbSent.append(solrFieldTextType + "-1" + solrFieldEnd);
				}
				if (!isItTableOfContents(sentence.replaceAll("xxPD", "\\.").trim())) {
					sbSent.append(solrFieldTextType + "3" + solrFieldEnd);
				}

				if (sentence.contains("CalculationX")) {
					System.out.println("3sent snip=" + sentence.substring(0, Math.min(sentence.length(), 75)) + "||");
				}

				sbSent.append(solrFieldText + solrFieldCDATA + sentence.replaceAll("xxPD", "\\.").trim()
						+ solrFieldCDATAEnd + solrFieldEnd);

//				sbSent.append(solrFieldNoDef + solrFieldCDATA + removeDefinitions
//						(sentence.replaceAll("xxPD", "\\.")).trim() 
//				+ solrFieldCDATAEnd
//						+ solrFieldEnd);

//				sbSent.append(solrFieldStopStem + solrFieldCDATA
//						+ Stemmer.stemmedOutPut(StopWords.removeStopWords(sentence.replaceAll("xxPD", "\\.").trim()))
//						+ solrFieldCDATAEnd + solrFieldEnd);

//				sbSent.append(solrFieldStopNoDefStem + solrFieldCDATA
//						+ Stemmer.stemmedOutPut(
//								removeDefinitions(StopWords.removeStopWords(sentence.replaceAll("xxPD", "\\.").trim())))
//						+ solrFieldCDATAEnd + solrFieldEnd);

//				if(sbPOS) {
//					sbSent.append(solrFieldStopNoDefPOS + solrFieldCDATA
//							+ getPartsOfSpeech(
//									removeDefinitions((StopWords.removeStopWords(sentence.replaceAll("xxPD", "\\.").trim()))))
//							+ solrFieldCDATAEnd + solrFieldEnd);
//				}

				sentencePrior = sentence.replaceAll("xxPD", "\\.").trim();
				sbSent.append(solrFieldDocEnd);

				// parent child here. if pcCnt==0 - then put metadata here. copy sent format.
				// marker for parentLeadin is at i=0 and is parentLeading
				continue;
			}

			if (listSentence.size() > 1) {
//				System.out.println("listSentence.size()=========="+listSentence.size());

				for (int n = 0; n < listSentence.size(); n++) {
					sentence = listSentence.get(n).trim();

					if (sentence.length() < 1 || (sentence.replaceAll("xxPD", "\\.").trim().equals(sentencePrior)
							&& sentencePrior.length() > 0)) {
//						 System.out.println("continue at 13. sentCnt="+sentCnt);

						continue;
					}

					sentCnt++;
					idSent = idPara + "_" + sentCnt;
					if (sentCnt == 0) {
//						 System.out.println("0sentCnt="+sentCnt);
						sbSent.append(solrFieldDoc + solrFieldId + idSent + solrFieldEnd);
						sbSent.append(solrFieldKid + kId + solrFieldEnd);
						sbSent.append(solrFieldContractLongSECname
								+ solrFieldCDATA + contractLongName
										.replaceAll("[\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,4}", "").toUpperCase().trim()
								+ solrFieldCDATAEnd + solrFieldEnd);
						sbSent.append(solrFieldContractType + solrFieldCDATA + contractType.trim() + solrFieldCDATAEnd
								+ solrFieldEnd);
						sbSent.append(
								solrFieldFiler + solrFieldCDATA + filer.trim() + solrFieldCDATAEnd + solrFieldEnd);
						sbSent.append(solrFieldCIK + cik.trim() + solrFieldEnd);
						sbSent.append(solrFieldFileSize + text.trim().length() + solrFieldEnd);
						sbSent.append(solrFieldFileDate + fileDate.trim() + solrFieldEnd);
						sbSent.append(solrFieldSecNo + "0" + solrFieldEnd);
						sbSent.append(solrFieldParaNo + "0" + solrFieldEnd);
						sbSent.append(solrFieldParChiNo + "0" + solrFieldEnd);
						sbSent.append(solrFieldTextCount + sentCnt + solrFieldEnd);
						sbSent.append(solrFieldTextType + "3" + solrFieldEnd);
						sbSent.append(solrFieldComment
								+ "the txt type is 3 b/c it is sentence (sec typ is 0 and para=1,parent \r\n"
								+ "child =2 and sent=3. id is kId,secID,paraId,sentId" + solrFieldEnd);
						sbSent.append(solrFieldDocEnd);
						sentCnt++;
					}

					idSent = idPara + "_" + sentCnt;
					sbSent.append(solrFieldDoc + solrFieldId + idSent + solrFieldEnd);
//					sbSent.append(solrFieldKid + kId + solrFieldEnd);
//					sbSent.append(solrFieldContractLongSECname + solrFieldCDATA + contractLongName
//							.replaceAll("[\\d]{10}-[\\d]{2}-[\\d]{6}_[\\d]{1,4}", "").toUpperCase().trim()
//							+ solrFieldCDATAEnd + solrFieldEnd);
//					sbSent.append(solrFieldContractType + solrFieldCDATA + contractType.trim() + solrFieldCDATAEnd
//							+ solrFieldEnd);
//					sbSent.append(solrFieldFiler + solrFieldCDATA + filer.trim() + solrFieldCDATAEnd + solrFieldEnd);
//					sbSent.append(solrFieldCIK + cik.trim() + solrFieldEnd);
//					sbSent.append(solrFieldFileSize + text.trim().length() + solrFieldEnd);
//					sbSent.append(solrFieldFileDate + fileDate.trim() + solrFieldEnd);
					sbSent.append(solrFieldSecNo + secCnt + solrFieldEnd);
					sbSent.append(solrFieldParaNo + (paraCnt - 1) + solrFieldEnd);
					sbSent.append(solrFieldTextCount + sentCnt + solrFieldEnd);
					sbSent.append(solrFieldDuplicate + "1" + solrFieldEnd);// sentence dif than para not same=1
					sbSent.append(solrFieldWordCount + sentence.split("[ ]+").length + "" + solrFieldEnd);
					sbSent.append(sentSolrFieldSameAsPara);

					if (leadInExists && nlp.getAllIndexEndLocations(sentence, Pattern.compile("(?<!\"):")).size() > 0) {

						leadInToSave = getLeadInClause(solrText.replaceAll("xxPD", "\\.") + "\r\n");

						List<String> list = SolrPrep.getSentences(leadInToSave, "");
						if (list.size() > 0) {
							leadInToSave = list.get(list.size() - 1);
							leadInToSave = getTextWithOutSectionHeading(leadInToSave, sectionHeading);
						}

					}

					if (leadInToSave.length() > 0) {
						sbSent.append(solrFieldLead + solrFieldCDATA + leadInToSave.replaceAll("xxPD", "\\.").trim()
								+ solrFieldCDATAEnd + solrFieldEnd);
//						if(leadInToSave.contains("CalculationX")) {
//							System.out.println("5leadInToSave snip="+leadInToSave.substring(0,Math.min(sentence.length(), 75)));
//						}

//						sbSent.append(solrFieldLeadStop + solrFieldCDATA + StopWords.removeStopWords(leadInToSave.replaceAll("xxPD", "\\.").trim())
//								+ solrFieldCDATAEnd + solrFieldEnd);

//						sbSent.append(solrFieldLeadNoDef + solrFieldCDATA+
//								removeDefinitions(leadInToSave.replaceAll("xxPD", "\\.")).trim()
//								+ solrFieldCDATAEnd + solrFieldEnd);

//						sbSent.append(solrFieldLeadStopNoDefStem + solrFieldCDATA
//								+ Stemmer.stemmedOutPut(removeDefinitions(
//										StopWords.removeStopWords(leadInToSave.replaceAll("xxPD", "\\.").trim())))
//								+ solrFieldCDATAEnd + solrFieldEnd);

//						if(sbPOS) {
//						sbSent.append(solrFieldLeadStopNoDefPOS + solrFieldCDATA
//								+ getPartsOfSpeech(removeDefinitions(
//										(StopWords.removeStopWords(leadInToSave.replaceAll("xxPD", "\\.").trim()))))
//								+ solrFieldCDATAEnd + solrFieldEnd);
//
//					}
					}

					if (isItTableOfContents(sentence.replaceAll("xxPD", "\\.").trim())
//							|| nlp.getAllMatchedGroups(sentence, Pattern.compile("\r\n\r\n")).size() > 2
//							|| shortLines(sentence.replaceAll("xxPD", "\\.").trim()) < 7
					) {

						sbSent.append(solrFieldTextType + "-1" + solrFieldEnd);

					}
					if (!isItTableOfContents(sentence.replaceAll("xxPD", "\\.").trim())) {
						sbSent.append(solrFieldTextType + "3" + solrFieldEnd);
					}

//					if (sentence.contains("CalculationX")) {
//						System.out.println("6sent snip=" + sentence.substring(0, Math.min(sentence.length(), 75))+"||");
//					}

					sbSent.append(solrFieldText + solrFieldCDATA + sentence.replaceAll("xxPD", "\\.").trim()
							+ solrFieldCDATAEnd + solrFieldEnd);

//					sbSent.append(solrFieldNoDef + solrFieldCDATA+
//							removeDefinitions(sentence.replaceAll("xxPD", "\\.")).trim()
//							+ solrFieldCDATAEnd + solrFieldEnd);

//					sbSent.append(solrFieldStop + solrFieldCDATA + StopWords.removeStopWords(sentence.replaceAll("xxPD", "\\.").trim()) + solrFieldCDATAEnd
//							+ solrFieldEnd);

//					sbSent.append(solrFieldStopStem + solrFieldCDATA
//							+ StopWords.removeStopWords(Stemmer.stemmedOutPut(sentence.replaceAll("xxPD", "\\.").trim()))
//							+ solrFieldCDATAEnd + solrFieldEnd);

//					sbSent.append(solrFieldStopNoDefStem + solrFieldCDATA
//							+ Stemmer.stemmedOutPut(
//									removeDefinitions(StopWords.removeStopWords(sentence.replaceAll("xxPD", "\\.").trim())))
//							+ solrFieldCDATAEnd + solrFieldEnd);

//					if(sbPOS) {
//					sbSent.append(solrFieldStopNoDefPOS + solrFieldCDATA
//							+ getPartsOfSpeech(
//									removeDefinitions((StopWords.removeStopWords(sentence.replaceAll("xxPD", "\\.")))))
//							+ solrFieldCDATAEnd + solrFieldEnd);
//					}

					sbSent.append(solrFieldDocEnd);
					sentencePrior = sentence.replaceAll("xxPD", "\\.").trim();

//					if(sentence.contains("CalculationX")) {
//						System.out.println("7prior sent snip="+sentence.substring(0,Math.min(sentence.length(), 75))+"||");
//					}
				}
			}
		}

		if (SolrPrep.sbSec)
			sbSec.append(solrFieldAddEnd);
		sbPara.append(solrFieldAddEnd);
		if (SolrPrep.sbParChi)
			sbParChi.append(solrFieldAddEnd);
		sbSent.append(solrFieldAddEnd);

//		System.out.println("fileOfIndexes==" + fileOfIndexes.getAbsolutePath());

		// System.out.println("12 text.len=" + sbPara.toString().length()) ;
		if (fileOfIndexes.exists())
			fileOfIndexes.delete();

		List<String> listSecParaSent = new ArrayList<>();
		if (SolrPrep.sbSec)// 0
			listSecParaSent.add(sbSec.toString());
		else
			listSecParaSent.add(null);

		listSecParaSent.add(sbPara.toString());// 1

		if (SolrPrep.sbParChi)// 2
			listSecParaSent.add(sbParChi.toString());
		else
			listSecParaSent.add(null);

		listSecParaSent.add(sbSent.toString());// 3

		return listSecParaSent;
	}

	public static List<String> getSentenceAwayFromShortLines(List<String> list) throws IOException {

		StringBuilder sb = new StringBuilder();
		List<String> listSent = new ArrayList<>();
		double wCnt = 0;
		double i = 0;
		double avgWordCount = 0, priorAvgWCnt = 0, avgInitCap = 0;
		double initialCapCnt = 0;
		double cnt = 0;
		String line = "";
		boolean found = false;
		String text = "";
//		deletexxxxxx
//		boolean tmp =true;
//		if(tmp) {
//			for(int g=0; g<list.size(); g++) {
//				if(list.get(g).contains("CalculationX Agent")) {
//					System.out.println("start of get short lines");
//					break;
//				}
//			}
//		}
//		deletexxxxxx

		for (int a = 0; a < list.size(); a++) {
			found = false;
			text = list.get(a);

			// deletexxxxxxx
//			tmp = false;
//			if (text.contains("CalculationX Agent")) {
//				System.out.println("shortline text-----" + text+"|");
//				tmp = true;
//			}

			String[] textSplit = text.split("\r\n");

			for (; i < textSplit.length; i++) {
				line = textSplit[(int) i];
				wCnt = wCnt + line.split(" ").length;
				avgWordCount = wCnt / (i + 1.0);
				initialCapCnt = (double) NLP
						.getAllMatchedGroupsAndStartIdxLocs(textSplit[(int) i], Pattern.compile("(?sm)[\r\n ][A-Z]{1}"))
						.size() + initialCapCnt;
				if (line.replaceAll("[ \t]+", "").length() > 1)
					cnt++;

				avgInitCap = (initialCapCnt / cnt);

				if (i > 4 && priorAvgWCnt < 10 && avgInitCap / avgWordCount > .4 && line.length() > 70 && cnt > 3
						&& sb.toString().length() < text.length()) {
//					System.out.println("new sent. shortline=" + line);
//					xxxxxxxdelete
//					if (tmp) {
//						System.out.println("1a listSent.add sb.toString=" + sb.toString());
//						System.out.println("1b listsent.add text.substring(sb.toString().length(), text.length())"
//								+ text.substring(sb.toString().length(), text.length()));
//					}

					listSent.add(sb.toString());
					listSent.add(text.substring(sb.toString().length(), text.length()));
					found = true;
					sb = new StringBuilder();
					break;
				} else {
//					xxxxxxxdelete
//					if(tmp) {
//					System.out.println("2 listSent.add line \r\n=" + line+"\r\n||");
//					}
					sb.append(line + "\r\n");
				}
				priorAvgWCnt = avgWordCount;
//				System.out.println("initialCapCnt==" + initialCapCnt);

//				System.out.println("i=" + i + " line=" + line + "\r\nwCnt avg=" + avgWordCount + " avgInitCap="
//						+ avgInitCap + " line wCnt=" + textSplit[(int) i].split(" ").length);
			}

			if (!found) {
//				xxxxxxxdelete
//				if(tmp) {
//				System.out.println("3 listSent.add list.get(a)=" + list.get(a)+"||");
//			}
				listSent.add(list.get(a));
			}
		}

//		NLP.printListOfString("listSent====", listSent);
		return listSent;

	}

	public static int shortLines(String text) {

		String[] textSplit = text.split("\r\n");
		double wCnt = 0;
		double i = 0;
		for (; i < textSplit.length; i++) {
			wCnt = wCnt + textSplit[(int) i].split(" ").length;
//			System.out.println("i="+i+" wCnt="+wCnt+" wCntLine="+textSplit[(int)i].split(" ").length);
		}
		double avg = 0;
		avg = wCnt / (i + 1.0);

		return (int) avg;

	}

	public static String removeDefinitions(String text) {

		return text.replaceAll("(" + "?<=" + "(" + "[\r\n\t \\(\\[\\{\"]{1}|^" + ")" + ")" + "(" + "[A-Z]{1}[a-z]{1,20}"
				+ ")" + "(" + "[\r\n\t \\)\\]\\}\",:;\\.]{1}|$" + ")" + "", "");

	}

	public static List<String> getParentChild(String text) throws IOException {
//		List<String> listParentChild = new ArrayList<>();

//		NLP nlp = new NLP();

		/*
		 * THIS ITERATES THROUGH A SENTENCE BUT PULL SENTENCE LEADIN ALONG. ONCE I HAVE
		 * PROPER MAPPING I THEN USE P1 CLAUSE AS LEAD FOR C2 WHERE P2C2 INCLUDING ALL
		 * OF C2 LINEAGE WHICH WOULD BE ALL THE WAY TO THE POINT C2 CHGS TO C3 OR P2
		 * CHANGES TO P3.
		 * 
		 */

		List<String> listSent = SolrPrep.getSentencesStanfordParser(text);
//		System.out.println("listSent.size="+listSent.size());

//		NLP.printListOfString("listSent====", listSent);
		List<String[]> listParChi = new ArrayList<>();

		int lineage = 0;

		// returns this list to attach solrFields to.
		List<String> listFields = new ArrayList<>();
		for (int i = 0; i < listSent.size(); i++) {
			text = listSent.get(i);
			listParentChildOrig = paragraphParentChild(text);
//			System.out.println("text="+text);
			NLP.printListOfStringArray("listParentChildOrig", listParentChildOrig);

			if (listParentChildOrig.size() == 1) {
				/*
				 * ONLY sentence captured. This however is a stanford sentence - which is
				 * different (generally lengthier) than my customized sentence parser (which for
				 * examples can end at a semi colon followed by a hard return and about 30 other
				 * rules).
				 */
				listFields.add("\r\n\r\nnewDoc==\r\n[[sentTxt==" + text.trim() + "]]\r\n==endDoc");
				// if no parChi - then add to list.
				continue;
			}

			// NOTE: THIS IS PURPOSEFULLY LIMITED TO JUST PARENT AND CHILD. NOT GC OR GGC
			// ETC.
			listParChi = parentChildSentence(listParentChildOrig, paraGraphText, lineage, null);
//			NLP.printListOfStringArray("AA listParChi==", listParChi);
//			System.out.println("listParagraphParentChild.size="+listParagraphParentChild.size());
			// here I should call matter that returns the final text to put into xml.
//			NLP.printListOfStringArray("listParagraphParentChild==",listParagraphParentChild);
			List<String[]> listParChi2 = new ArrayList<>();
			if (listParagraphParentChild.size() > 0) {
				lineage++;
				listParChi2 = parentChildSentence(listParagraphParentChild, paraGraphText, lineage, listParChi);
				for (int n = 0; n < listParChi2.size(); n++) {
					listParChi.add(listParChi2.get(n));
				}

				NLP.printListOfStringArray("BB listParChi=", listParChi);
			}

			/*
			 * NOTE: THIS has to be an iteration of each sentence. And after each sentence
			 * to start new!!
			 */

			NLP.printListOfStringArray("FINAL listParChi==", listParChi);
			List<String[]> listParentChildSolrFields = getParChiSolrFields(listParChi, paraGraphText);
			NLP.printListOfStringArray("CC listParentChildSolrFields==", listParentChildSolrFields);

			for (int a = 0; a < listParentChildSolrFields.size(); a++) {
				listFields.add("\r\n\r\nnewDoc==" + Arrays.toString(listParentChildSolrFields.get(a))
						.replaceAll("xxOP", "\\(").replaceAll("CPxx", "\\)") + "\r\n==endDoc");
			}
		}

		/*
		 * TODO: RETURN listSorlFields and for each string in list - add solrFields that
		 * carry from para.xml and break string back into separate fields based on their
		 * marks in the string - ie., [lineageLead=...] and so on. re-establish
		 */

		// TODO: fix p15 v p25 or not worry about it b/c solr search is agnostic to
		// naming convention its just that the results could be confusing

		NLP.printListOfString("listFields======", listFields);

		return listFields;

	}

	public static String parChiFields(String text) throws IOException {

		NLP nlp = new NLP();
		Pattern patternLineageLead = Pattern.compile("(?sm)(?<=\\[\\[lineageLead==).*?(?=\\]\\])");
		Pattern patternParentLead = Pattern.compile("(?sm)(?<=\\[\\[parentLead==).*?(?=\\]\\])");
		Pattern patternTxt = Pattern.compile("(?sm)(?<=\\[\\[txt==).*?(?=\\]\\])");
		Pattern patternSentTxt = Pattern.compile("(?sm)(?<=\\[\\[sentTxt==).*?(?=\\]\\])");
		Pattern patternLineage = Pattern.compile("(?sm)(?<=\\[\\[lineage==).*?(?=\\]\\])");
		Pattern patternpOrC = Pattern.compile("(?sm)(?<=\\[\\[pOrC==).*?(?=\\]\\])");

		text = text.replaceAll("(?sm) ?\\[\\[-\\]\\] ?", " ");
		StringBuilder sb = new StringBuilder();
		if (nlp.getAllMatchedGroups(text, patternLineageLead).size() > 0
				&& nlp.getAllMatchedGroups(text, patternLineageLead).get(0).length() > 0) {
			sb.append(solrFieldLeadLineage + solrFieldCDATA + nlp.getAllMatchedGroups(text, patternLineageLead).get(0)
					+ solrFieldCDATAEnd + solrFieldEnd);
		}

		if (nlp.getAllMatchedGroups(text, patternParentLead).size() > 0
				&& nlp.getAllMatchedGroups(text, patternParentLead).get(0).length() > 0) {
			sb.append(solrFieldParentLead + solrFieldCDATA + nlp.getAllMatchedGroups(text, patternParentLead).get(0)
					+ solrFieldCDATAEnd + solrFieldEnd);
		}
		if (nlp.getAllMatchedGroups(text, patternTxt).size() > 0
				&& nlp.getAllMatchedGroups(text, patternTxt).get(0).length() > 0) {
			sb.append(solrFieldText + solrFieldCDATA + nlp.getAllMatchedGroups(text, patternTxt).get(0)
					+ solrFieldCDATAEnd + solrFieldEnd);
		}
		if (nlp.getAllMatchedGroups(text, patternSentTxt).size() > 0
				&& nlp.getAllMatchedGroups(text, patternSentTxt).get(0).length() > 0) {
			sb.append(solrFieldText + solrFieldCDATA + nlp.getAllMatchedGroups(text, patternSentTxt).get(0)
					+ solrFieldCDATAEnd + solrFieldEnd);
		}
		if (nlp.getAllMatchedGroups(text, patternLineage).size() > 0
				&& nlp.getAllMatchedGroups(text, patternLineage).get(0).length() > 0) {
			sb.append(solrFieldLineage + solrFieldCDATA + nlp.getAllMatchedGroups(text, patternLineage).get(0)
					+ solrFieldCDATAEnd + solrFieldEnd);
		}
		if (nlp.getAllMatchedGroups(text, patternpOrC).size() > 0
				&& nlp.getAllMatchedGroups(text, patternpOrC).get(0).length() > 0) {
			sb.append(solrFieldParentOrChild + solrFieldCDATA + nlp.getAllMatchedGroups(text, patternpOrC).get(0)
					+ solrFieldCDATAEnd + solrFieldEnd);
		}

		sb.append(solrFieldWordCount + text.split("[ ]+").length + "" + solrFieldEnd);

		return sb.toString();
	}

	public static List<String[]> getParChiSolrFields(List<String[]> listParChi, String text) {

		List<String[]> listParChiSolrFields = new ArrayList<>();
		List<String[]> ltmp = listParChi;
		TreeMap<Integer, String[]> map1 = new TreeMap<Integer, String[]>();

		for (int c = 0; c < ltmp.size(); c++) {
			map1.put(Integer.parseInt(listParChi.get(c)[3]), listParChi.get(c));
//		System.out.println("ordering by sIdx="+listParChi.get(c)[3]);
		}

		listParChi = new ArrayList<>();
		boolean parIsPrior = false, chiIsPrior = false;
		boolean outOfOrder = false;
		int par = 0, parPrior = 0, chi = 0, chiPrior = 0, cnt = 0, cntPrior = 0;
		for (Map.Entry<Integer, String[]> entry : map1.entrySet()) {
			// System.out.println("check if out of order here");
			outOfOrder = false;
			cnt++;

//			System.out.println("out of order lineage=" + entry.getValue()[5]);
//			System.out.println("out of order lineage=" + entry.getValue()[1]);

			if (entry.getValue()[5].equals("0")) {
				par = Integer.parseInt(entry.getValue()[1]);

				if (par - parPrior != 1) {
//					System.out.println("1 parent is out of order!");
					if (listParagraphParentChild.size() == 0)
						listParagraphParentChild.add(entry.getValue());
					outOfOrder = true;
				}

				parPrior = par;
				parIsPrior = true;

				chiIsPrior = false;
			}

			if (entry.getValue()[5].equals("1")) {
				chi = Integer.parseInt(entry.getValue()[1]);

				if (cnt - cntPrior != 1 && chiIsPrior) {
//					System.out.println("1 child is out of order!");
					if (listParagraphParentChild.size() == 0)
						listParagraphParentChild.add(entry.getValue());
					outOfOrder = true;
				}

				if (parIsPrior && chi != 1) {
//					System.out.println("2 child is out of order!");
					if (listParagraphParentChild.size() == 0)
						listParagraphParentChild.add(entry.getValue());
					outOfOrder = true;
				}

				chiPrior = chi;
				chiIsPrior = true;

				parIsPrior = false;
			}

			if (!outOfOrder)
				listParChi.add(entry.getValue());
			cntPrior = cnt;

		}

//		NLP.printListOfStringArray("cxcx listParChi====", listParChi);
		TreeMap<Integer, String[]> map = new TreeMap<Integer, String[]>();
		for (int c = 0; c < listParChi.size(); c++) {

			if (c + 1 < listParChi.size()) {

//				 System.out.println("listParChi.get(c)[4]=="+listParChi.get(c)[4]);
//				 System.out.println("listParChi.get(c)[5]=="+listParChi.get(c)[5]);
//				 System.out.println("listParChi.get(c+1)[4]=="+listParChi.get(c+1)[4]);
//				 System.out.println("listParChi.get(c+1)[1]=="+listParChi.get(c+1)[1]);
//				 System.out.println("listParChi.get(c)[1]=="+listParChi.get(c)[1]);

				if ((listParChi.get(c)[4].equals("slave") && listParChi.get(c)[5].equals("0")
						&& Integer.parseInt(listParChi.get(c + 1)[1]) - Integer.parseInt(listParChi.get(c)[1]) != 1)
						|| (listParChi.get(c)[4].equals("master") && listParChi.get(c + 1)[4].equals("master")
								&& Integer.parseInt(listParChi.get(c + 1)[1])
										- Integer.parseInt(listParChi.get(c)[1]) != 1)) {

					// System.out.println("listParChi.get(c)[4]=="+listParChi.get(c)[4]);
					// System.out.println("listParChi.get(c)[5]=="+listParChi.get(c)[5]);
					// System.out.println("listParChi.get(c+1)[4]=="+listParChi.get(c+1)[4]);
					// System.out.println("listParChi.get(c+1)[1]=="+listParChi.get(c+1)[1]);
					// System.out.println("listParChi.get(c)[1]=="+listParChi.get(c)[1]);
//					System.out.println("careful of iv - v or (u) to (v) if it is outOfOrder** "
//							+ "\r\nparent child is outOfOrder=" + outOfOrder);
					outOfOrder = true;
					listParChi = new ArrayList<>();
					return listParChi;
				}

			}

			map.put(Integer.parseInt(listParChi.get(c)[3]), listParChi.get(c));
		}

		outOfOrder = false;
		String linLead = "";
//		NLP.printMapIntStringAry("=map==", map);

		List<String[]> listTmp = new ArrayList<>();

		for (Map.Entry<Integer, String[]> entry : map.entrySet()) {
			listTmp.add(entry.getValue());
		}

//		NLP.printListOfStringArray("listTmp==", listTmp);

		String txt = "", parentTxt = "", parentLead = "", lineageStrNext = "", childNmb = "", parentNmb = "",
				childTxt = "", lineageLead = "", lineageStr = "-1", lineageStrPrior = "-1", valStrPrev = "-1",
				valStr = "-1", valStr2 = "-1";
		int c = 1, sIdx = 0, sIdxPrev = 0, eIdx = 0, sIdx2 = 0, eIdx2 = 0;

//		System.out.println("listTmp.size(i)=" + listTmp.size());
		for (int i = 0; i < listTmp.size(); i++) {
			// map== key|53| val|[(1), 1, 5, 53, master, 0] cur=0 and next = 0, therefore
			// cur=p only - continue;
			// map== key|377| val|[(2), 2, 5, 377, master, 0] cur=0 and next = 1, inner loop
			// map== key|788| val|[(a), 1, 2, 788, slave, 1] cur=1 and next=0, break and
			// start at next (0)
			// map== key|1025| val|[(3), 3, 5, 1025, master, 0] cur=0 and next = 0,
			// therefore cur=p only - continue;
			// map== key|1318| val|[(4), 4, 5, 1318, master, 0] cur=0 and next = 0,
			// therefore cur=p only - continue;
			// map== key|1550| val|[(5), 5, 5, 1550, master, 0] cur=0 and next = 1, inner
			// loop to pair
			// map== key|1798| val|[(b), 2, 2, 1798, slave, 1] cur=1 and next=0, next = 1
			// cur is inner loop and pairs
			// map== key|2071| val|[(c), 3, 2, 2071, slave, 1] cur=1 and next=0, break and
			// start at next (0)
			// map== key|2914| val|[(6), 6, 5, 2914, master, 0] cur=0 and next = 0,
			// therefore cur=p only - continue;

			// map== key|53| val|[(1), 1, 5, 53, master, 0] cur=0 and next = 0, therefore
			// cur=p only - continue;
			sIdx = Integer.parseInt(listTmp.get(i)[3]);
			lineageStr = listTmp.get(i)[5].trim();
			valStr = listTmp.get(i)[1].trim();

			if (i + 1 < listTmp.size()) {
				eIdx = Integer.parseInt(listTmp.get(i + 1)[3]);
				lineageStrNext = listTmp.get(i + 1)[5].trim();
			}
			if (i + 1 == listTmp.size()) {
				eIdx = text.length();
			}

			if (sIdx > 0 && i == 0) {
				txt = text.substring(0, sIdx).trim();
				lineageLead = "lineageLead==" + txt;
				String[] ary = { "\r\n[[" + lineageLead + "]]", "\r\n[[txt==" + txt + "]]" };
//				System.out.println("1 last ary - lineage="+Arrays.toString(ary));
				listParChiSolrFields.add(ary);
			}

			if ((i + 1 < listTmp.size() && lineageStr.equals("0") && lineageStrNext.equals("0"))
					|| (i + 1 == listTmp.size() && lineageStr.equals("0"))) {

				txt = text.substring(sIdx, eIdx).trim();
				String[] ary = { "\r\n[[" + lineageLead + "]]", "\r\n[[txt==" + txt + "]]",
						"\r\n[[lineage==p" + Integer.parseInt(valStr.trim()) + "]]", "\r\n[[pOrC==p]]" };
//				System.out.println("2 last ary - lineage="+Arrays.toString(ary));
				listParChiSolrFields.add(ary);
				continue;
			}

//			System.out.println("outer i="+i+" listTmp.get(i)="+Arrays.toString(listTmp.get(i)));
//			System.out.println("i="+i+" lineageStr="+lineageStr+" lineageStrNext="+lineageStrNext);

			if (lineageStr.equals("0")) {
				txt = text.substring(sIdx, eIdx).trim();
				parentLead = txt;
//				System.out.println("listTmp.size(c)=" + listTmp.size());
				for (c = (i + 1); c < listTmp.size(); c++) {

//					System.out.println("inner loop - c="+c+" listTmp.get(c)="+Arrays.toString(listTmp.get(c)));
					lineageStr = listTmp.get(c)[5].trim();
					valStr2 = listTmp.get(c)[1].trim();
					sIdx2 = Integer.parseInt(listTmp.get(c)[3]);
					if (c + 1 < listTmp.size()) {
						eIdx2 = Integer.parseInt(listTmp.get(c + 1)[3]);
					}

					if (c + 1 == listTmp.size()) {
						eIdx2 = text.length();
					}

					if (lineageStr.equals("1")) {
						childTxt = text.substring(sIdx2, eIdx2).trim();
						childNmb = valStr2;

						/*
						 * these are the solr xml chema fields. parentText is the fusion of the parent
						 * numeral with each of its children respectively. if there is (A).....,
						 * (1)......, (2)......(B)......(1) ---- then if pC=p the parent text is the
						 * fusion of (A)+(1) text as one solr txt field next solr txt field is (A)+2 and
						 * so on
						 */

						String[] ary = { "\r\n[[" + lineageLead + "]]",
								"\r\n[[txt==" + parentLead + " " + childTxt + "]]",
								"\r\n[[lineage==p" + valStr + "c" + valStr2 + "]]", "\r\n[[pOrC==p]]" };
						listParChiSolrFields.add(ary);
//						System.out.println("3 last ary - lineage="+Arrays.toString(ary));

						String[] ary2 = { "\r\n[[" + lineageLead + "]]", "\r\n[[parentLead==" + parentLead + "]]",
								"\r\n[[txt==" + childTxt + "]]", "\r\n[[lineage==p" + valStr + "c" + valStr2 + "]]",
								"\r\n[[pOrC==c]]" };

//						System.out.println("4 last ary - lineage="+Arrays.toString(ary2));
						listParChiSolrFields.add(ary2);
					}

					if (listTmp.get(c)[5].trim().equals("0")) {
						break;
					}
				}
			}

			lineageStrPrior = lineageStr;
		}

		return listParChiSolrFields;

	}

	/*
	 * public static List<String[]> getParChiSolrFields(List<String[]> listParChi,
	 * String text){ List<String[]> listParChiSolrFields = new ArrayList<>();
	 * 
	 * TreeMap<Integer, String[]> map = new TreeMap<Integer,String[]>(); for(int
	 * c=0; c<listParChi.size(); c++) {
	 * map.put(Integer.parseInt(listParChi.get(c)[3]), listParChi.get(c)); }
	 * 
	 * String linLead = ""; NLP.printMapIntStringAry("map==", map);
	 * 
	 * List<String[]> listTmp = new ArrayList<>();
	 * 
	 * for (Map.Entry<Integer, String[]> entry : map.entrySet()) {
	 * listTmp.add(entry.getValue()); }
	 * 
	 * NLP.printListOfStringArray("listTmp==", listTmp);
	 * 
	 * 
	 * String txt = "", parentTxt = "", parentLead = "", lineageStrNext = "",
	 * childNmb="", parentNmb ="", childTxt = "", lineageLead ="", lineageStr =
	 * "-1", lineageStrPrior = "-1", valStrPrev = "-1", valStr = "-1",valStr2 =
	 * "-1"; int c=1, sIdx = 0, sIdxPrev = 0, eIdx = 0, sIdx2 = 0, eIdx2 = 0,cnt=-1;
	 * 
	 * System.out.println("listTmp.size(i)=" + listTmp.size()); for(int i=0;
	 * i<listTmp.size(); i++) {
	 * System.out.println("outer i="+i+" listTmp.get(i)="+Arrays.toString(listTmp.
	 * get(i))); cnt++; sIdx = Integer.parseInt(listTmp.get(i)[3]);
	 * 
	 * if(i+1<listTmp.size()) { eIdx = Integer.parseInt(listTmp.get(i+1)[3]); }
	 * 
	 * if(i+1==listTmp.size()) { eIdx = text.length(); }
	 * 
	 * if(i+1<listTmp.size()) { lineageStrNext = listTmp.get(i+1)[5].trim(); }
	 * 
	 * lineageStr = listTmp.get(i)[5].trim(); valStr = listTmp.get(i)[1].trim();
	 * System.out.println("i="+i+" lineageStr="+lineageStr+" lineageStrNext="
	 * +lineageStrNext);
	 * 
	 * if(sIdx>0 && i==0) { txt = text.substring(0, sIdx).trim(); lineageLead =
	 * "lineageLead=="+txt; // System.out.println("lineageLead=x="+lineageLead);
	 * String[] ary = {"\r\n[["+lineageLead+"]]","\r\n[[txt==" +txt+"]]"};
	 * System.out.println("1 last ary - lineage="+Arrays.toString(ary));
	 * listParChiSolrFields.add(ary); if(i+1<listTmp.size() &&
	 * lineageStr.equals("0") && lineageStrNext.equals("0")) { txt =
	 * text.substring(sIdx, Integer.parseInt(listTmp.get(i+1)[3])).trim(); String[]
	 * ary2 = { "\r\n[["+lineageLead+"]]" ,"\r\n[[txt==" +txt+"]]"
	 * ,"\r\n[[lineage==p" + Integer.parseInt(valStr.trim())+"]]"
	 * ,"\r\n[[pOrC==p]]"}; listParChiSolrFields.add(ary);
	 * System.out.println("1b last ary ="+Arrays.toString(ary2)); continue; }
	 * continue; }
	 * 
	 * System.out.println("again - i="+i+" lineageStr="
	 * +lineageStr+" lineageStrNext="+lineageStrNext); if(i+1<listTmp.size() &&
	 * lineageStr.equals("0") && lineageStrNext.equals("0")) { // if(i>0) { //
	 * sIdxPrev = Integer.parseInt(listTmp.get(i-1)[3]); // txt =
	 * text.substring(sIdxPrev, sIdx); // } // // String[] ary = {
	 * "\r\n[["+lineageLead+"]]" // ,"\r\n[[txt==" +txt+"]]" // ,"\r\n[[lineage==p"
	 * + (Integer.parseInt(valStr.trim())-1)+"]]" // ,"\r\n[[pOrC==p]]"}; //
	 * listParChiSolrFields.add(ary); //
	 * System.out.println("2 last ary ="+Arrays.toString(ary)); txt =
	 * text.substring(sIdx, Integer.parseInt(listTmp.get(i+1)[3])).trim(); String[]
	 * ary = { "\r\n[["+lineageLead+"]]" ,"\r\n[[txt==" +txt+"]]"
	 * ,"\r\n[[lineage==p" + Integer.parseInt(valStr.trim())+"]]"
	 * ,"\r\n[[pOrC==p]]"}; listParChiSolrFields.add(ary);
	 * System.out.println("2 last ary ="+Arrays.toString(ary)); continue; }
	 * 
	 * if(i+1==listTmp.size() && lineageStr.equals("0") ) {
	 * 
	 * if(i>0) { sIdxPrev = Integer.parseInt(listTmp.get(i-1)[3]); txt =
	 * text.substring(sIdxPrev, sIdx);
	 * 
	 * String[] ary = { "\r\n[["+lineageLead+"]]" ,"\r\n[[txt==" +txt+"]]xz"
	 * ,"\r\n[[lineage==p" + (Integer.parseInt(valStr.trim())-1)+"]]"
	 * ,"\r\n[[pOrC==p]]"}; listParChiSolrFields.add(ary);
	 * System.out.println("3 last ary ="+Arrays.toString(ary)); txt =
	 * text.substring(sIdx, text.length());
	 * 
	 * String[] ary2 = { "\r\n[["+lineageLead+"]]" ,"\r\n[[txt==" +txt+"]]xz"
	 * ,"\r\n[[lineage==p" + valStr+"]]" ,"\r\n[[pOrC==p]]"};
	 * System.out.println("4 last ary ="+Arrays.toString(ary2));
	 * listParChiSolrFields.add(ary2);
	 * 
	 * }
	 * 
	 * if(i==0) { txt = text.substring(0, sIdx); String[] ary = {
	 * "\r\n[["+lineageLead+"]]" ,"\r\n[[txt==" +txt+"]]xz" ,"\r\n[[lineage==p" +
	 * valStr+"]]" ,"\r\n[[pOrC==p]]"};
	 * System.out.println("5 last ary ="+Arrays.toString(ary));
	 * listParChiSolrFields.add(ary); }
	 * 
	 * 
	 * }
	 * 
	 * 
	 * if(lineageStr.equals("0")) { txt = text.substring(sIdx,eIdx).trim();
	 * parentLead = txt; // System.out.println("\r\nparentLead==" + parentLead); //
	 * String[] ary = { "parentLead==" + parentLead, lineageLead + "type==" + valStr
	 * }; // listParChiSolrFields.add(ary); System.out.println("listTmp.size(c)=" +
	 * listTmp.size()); for(c=(i+1);c< listTmp.size(); c++) {
	 * 
	 * System.out.println("inner loop - c="+c+" listTmp.get(c)="+Arrays.toString(
	 * listTmp.get(c))); // if(sIdx2<sIdx) // continue; lineageStr =
	 * listTmp.get(c)[5].trim(); valStr2 = listTmp.get(c)[1].trim(); sIdx2 =
	 * Integer.parseInt(listTmp.get(c)[3]); if(c+1<listTmp.size()) { eIdx2 =
	 * Integer.parseInt(listTmp.get(c+1)[3]); }
	 * 
	 * if(c+1==listTmp.size()) { eIdx2 = text.length(); }
	 * 
	 * if (lineageStr.equals("1") ) {
	 * 
	 * // System.out.println("sIdx2=" + sIdx2 + " eIdx2=" + eIdx2); childTxt =
	 * text.substring(sIdx2, eIdx2).trim(); childNmb = valStr2;
	 * 
	 * 
	 * these are the solr xml chema fields. parentText is the fusion of the parent
	 * numeral with each of its children respectively. if there is (A).....,
	 * (1)......, (2)......(B)......(1) ---- then if pC=p the parent text is the
	 * fusion of (A)+(1) text as one solr txt field next solr txt field is (A)+2 and
	 * so on
	 * 
	 * 
	 * String[] ary = { "\r\n[["+lineageLead+"]]" ,"\r\n[[txt==" +parentLead+" "+
	 * childTxt+"]]" ,"\r\n[[lineage==p" + valStr+"c"+valStr2+"]]"
	 * ,"\r\n[[pOrC==p]]"}; listParChiSolrFields.add(ary);
	 * System.out.println("6 last ary p="+Arrays.toString(ary));
	 * 
	 * String[] ary2 = {"\r\n[["+lineageLead+"]]" ,"\r\n[[parentLead==" +
	 * parentLead+"]]" ,"\r\n[[txt=="+ childTxt+"]]" ,"\r\n[[lineage==p" +
	 * valStr+"c"+valStr2+"]]" ,"\r\n[[pOrC==c]]"};
	 * 
	 * System.out.println("7 last ary p="+Arrays.toString(ary2));
	 * listParChiSolrFields.add(ary2);
	 * 
	 * }
	 * 
	 * if(lineageStr.equals("0") && !lineageStrNext.equals("0")) { String[] ary = {
	 * "\r\n[["+lineageLead+"]]" ,"\r\n[[txt==" +text.substring(sIdx2,
	 * eIdx2).trim()+"]]" ,"\r\n[[lineage==p" + valStr+"]]" ,"\r\n[[pOrC==p]]"};
	 * System.out.println("8 again - i="+i+" lineageStr="
	 * +lineageStr+" lineageStrNext="+lineageStrNext);
	 * System.out.println("8 last ary p="+Arrays.toString(ary));
	 * listParChiSolrFields.add(ary); i=c+1; break; }
	 * 
	 * 
	 * FORMAT: To prepare list for attachment of solrFields label as follows.
	 * sent/txt==, lineageLead, parentLead, type==[parent# or p#c#],
	 * 
	 * 
	 * } }
	 * 
	 * lineageStrPrior = lineageStr;
	 * 
	 * }
	 * 
	 * return listParChiSolrFields;
	 * 
	 * }
	 */

	public static List<String[]> parentChildSentence(List<String[]> list, String text, int lineage,
			List<String[]> listParChi) {

		List<String[]> listParChiSent = new ArrayList<>();

		// parent leadTxt is 0 to first sidx. if numeral is new type OR is of same type
		// as a prior but not consecutive with any of the prior then it is a new
		// lineage. So I parse forward finding all like lineages of the first type and
		// adding them to a new list labeled p1,p2 etc. Then I look for the next first
		// instance and so.

		/* THIS IS ONLY PARENT AND CHILD -- NOT GC OR GGC! */

		// TODO: ENSURE I CAPTURE SIDX AND EIDX ABOVE RIGHT AT THE START!!

		int type = -1, sIdx = -1, val = -1, valLast = -1, valPrior = -1, typeOrig = -1;
		;
		String mastSlav = "", numeral = "", numeralLast = "", sIdxChi = "";
		boolean added = false;

		listParagraphParentChild = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
//			System.out.println("list.get(i)==" + Arrays.toString(list.get(i)));
			added = false;
			numeral = list.get(i)[0];
			val = Integer.parseInt(list.get(i)[1]);
			type = Integer.parseInt(list.get(i)[2]);
			sIdx = Integer.parseInt(list.get(i)[3]);
			mastSlav = list.get(i)[4];

			if (i > 0) {
				valPrior = Integer.parseInt(list.get(i - 1)[1]);
			}

			if (i == 0) {
				typeOrig = type;
				valLast = val;
				numeralLast = numeral;
				String[] ary = { numeral, val + "", type + "", sIdx + "", mastSlav, lineage + "" };
				// (v), 5, 0, 3652, master
//				System.out.println("i=" + i + " 1. add ary=" + Arrays.toString(ary) + " valLast=" + valLast);
				listParChiSent.add(ary);
				added = true;
				continue;
			}

			if (val - valLast == 1 && typeOrig == type) {
				String[] ary = { numeral, val + "", type + "", sIdx + "", mastSlav, lineage + "" };
				listParChiSent.add(ary);
				added = true;
				valLast = val;
				numeralLast = numeral;
//				System.out.println("i=" + i + " 2. add ary=" + Arrays.toString(ary) + " valLast=" + valLast);
			}

//			System.out.println("added=" + added + " valLast=" + valLast + " numeral=" + numeral + " val=" + val
//					+ " type=" + type + " sIdx=" + sIdx + " mastSlav=" + mastSlav);

			if (itIsAnException(val, valLast, numeral, numeralLast)) {
				String[] ary = { numeral, val + "", type + "", sIdx + "", mastSlav, lineage + "" };
				listParChiSent.add(ary);
				added = true;
				valLast = val;
				numeralLast = numeral;
//				System.out.println(
//						"it is an exception\r\ni=" + i + " 2. add ary=" + Arrays.toString(ary) + " valLast=" + valLast);
				added = true;
				continue;
			}

			// is it after a parent and if so does it start at 1? if so set at lineage=child
			// and reset valLast

//			System.out.println("list.get(i)=" + Arrays.toString(list.get(i)));
			sIdxChi = list.get(i)[3];

			if (val == 1 && type == typeOrig && !added && null != listParChi && itIsParent(sIdxChi, listParChi)) {
				// if itIsParent and type==typeOrig then reset valLast and set as child

				String[] ary = { numeral, val + "", type + "", sIdx + "", mastSlav, lineage + "" };
//				System.out.println("did I add it?");
				listParChiSent.add(ary);
				added = true;
				valLast = val;
				numeralLast = numeral;

			}

			if (!added
			// && val-valPrior != 1
			) {
//				System.out.println("not added=" + added + " i=" + i + " added to listParagraphParentChild="
//						+ Arrays.toString(list.get(i)));
				if (listParagraphParentChild.size() == 0)
					listParagraphParentChild.add(list.get(i));// out of order
//				System.out.println("IS OUT OF ORDER");
				// OUT OF ORDER! XXXXXX
			}
		}

//		NLP.printListOfStringArray("listParagraphParentChild======", listParagraphParentChild);
//		NLP.printListOfStringArray("listParChiSent======", listParChiSent);
		return listParChiSent;

	}

	public static boolean itIsParent(String sIdxStrChild, List<String[]> listParChi) {

		// find if the immediately preceding numeral is a parent lineage. By passing
		// sIdx of what may be a child and stopping at the original parent child list.
		// If the immediately preceding numeral is a parent as checked against
		// listParChi then return true

		int sIdxChild = Integer.parseInt(sIdxStrChild), sIdxPrior = 0, sIdxPar = 0;
//		System.out.println("@isItParent - sIdxChid="+sIdxChild);
		for (int i = 0; i < listParentChildOrig.size(); i++) {
			if (sIdxStrChild.equals(listParentChildOrig.get(i)[3]) && i > 0) {
				// if this is true - then I found child.
				sIdxPrior = Integer.parseInt(listParentChildOrig.get(i - 1)[3]);
				// 2529
//				System.out.println("found child loc. possible parent sIdxPrior="+sIdxPrior);
				for (int n = 0; n < listParChi.size(); n++) {
//					prior type equals parent type - then return true if sIdxPar=sIdxPrior
					sIdxPar = Integer.parseInt(listParChi.get(n)[3]);
//					System.out.println("?sIdxPar"+sIdxPar+" sIdxPrior="+sIdxPrior);
					if (sIdxPar == sIdxPrior)
						return true;
				}
			}
		}

		return false;

	}

	public static boolean itIsAnException(int val, int valLast, String numeral, String numeralLast) {

		if (Math.abs(valLast - val) == 15 && numeral.replaceAll("[\\.\\(\\)]+", "").toLowerCase().equals("y")) {
			return true;
		}

		return false;

	}

	public static List<String[]> paragraphParentChild(String text) throws IOException {

		String origText = text;

		TreeMap<Integer, List<String[]>> map = new TreeMap<Integer, List<String[]>>();
		// if parent = then key=1, child key=2 - so I just iterate through the map - it
		// isn't much
		NLP nlp = new NLP();

		paraGraphText = SolrPrep.getNumeralNumberedSubSentencesInSentence(text, 0);
		System.out.println("@paraGraphText=" + paraGraphText);
		paraGraphText = paraGraphText.replaceAll("sssS1s", "");
		paraGraphText = paraGraphText.replaceAll("\\(loss\\)", "xxOPlossCPxx");
		paraGraphText = paraGraphText.replaceAll("(?<=\\([a-zA-Z]{1}\\)) (?=\\([a-zA-Z]{1}\\))", "  ").trim();
		paraGraphText = paraGraphText.trim().replaceAll("^" + "(?=\\(([A-Za-z]{1,4}" + "|[xvil]{1,7})\\)|([A-Z]{1,2}"
				+ "|[a-z]{1,2})\\.|\\([\\d]{1,3}\\)|[\\d]{1,3}\\.)", "\r\n\r\n");
		paraGraphText = paraGraphText.replaceAll("\\)(?=[a-zA-Z]{1})", "\\) ");

		paraGraphText = paraGraphText.replaceAll("(?<=[:;]{1} ?(and)? ?)\r\n(?=\\([\\da-zA-Z]{1,7}\\))", " ");
		// (iii) (A) ==> (iii) [[-]] (A)

		paraGraphText = paraGraphText.replaceAll("(?<=\\([\\da-zA-Z]{1,7}\\)) (?=\\([\\da-zA-Z]{1,7}\\))",
				" \\[\\[-\\]\\] ");

//		PrintWriter pwtmp = new PrintWriter(new File("e:/getContracts/temp/tmpNumeral.txt"));
//		pwtmp.append(paraGraphText);
//		pwtmp.close();

		List<String[]> listParaNumberMark = NLP.getAllMatchedGroupsAndStartIdxLocs(paraGraphText,
				NLP.patternParaNumbMarker);
		String sIdxStr = "", typPrev = "", type = "";
		int typVal = 0, typValPrev = 0;

		if (listParaNumberMark.size() == 0) {

			List<String[]> lst = new ArrayList<>();
			String[] ary = { "1000", origText };
			lst.add(ary);
//			System.out.println("null - returned lst. lst.size="+lst.size());
			return lst;
		}

		List<String[]> listParas = new ArrayList<>();
		// get number related to roman number/alph/nmb in para and type. Used for
		// parent/child lineage
		boolean endIsColon = false;
		String masterOrSlave = "", listNmb = "", strColon = "";
		int idxNext = 0;
		for (int i = 0; i < listParaNumberMark.size(); i++) {
//			System.out.println("listParaNumberMark.get(i)="+Arrays.toString(listParaNumberMark.get(i)));
			endIsColon = false;
			listNmb = listParaNumberMark.get(i)[0]; // don't trim!
			strColon = "";
			if (i + 1 < listParaNumberMark.size()) {
				strColon = paraGraphText.substring(Integer.parseInt(listParaNumberMark.get(i)[1]),
						Integer.parseInt(listParaNumberMark.get(i + 1)[1])).trim();
				strColon = strColon.substring(strColon.length() - 2, strColon.length());
				if (strColon.contains(":")) {
					endIsColon = true;
//					 System.out.println("endIsColon="+endIsColon);
				}
			}

			sIdxStr = listParaNumberMark.get(i)[1];
//			System.out.println("listNmb=" + listNmb.trim() + "||");
			int[] a1 = nlp.convertAlphaParaNumbAndType(listNmb.trim(), false, "");
			if (a1 == null || a1.length < 1) {
//				System.out.println("a1==" + a1);
				continue;
			}

//			System.out
//					.println("a1[1]=" + a1[1] + " a1[0]=" + a1[0] + " " + "eIdxStr=" + sIdxStr + " listNmb=" + listNmb);

			// if a hard return precedes the # it is a para #, if not it is a clause #.

			if (nlp.getAllIndexEndLocations(listNmb, Pattern.compile("[\r\n]{1}")).size() > 0) {
				masterOrSlave = "master";
				if (endIsColon) {
					masterOrSlave = "masterColon";
				}
//				 System.out.println("1.. masterOrSlave=="+masterOrSlave+" listNmb=="+listNmb.trim());
			}

			if (nlp.getAllIndexEndLocations(listNmb, Pattern.compile("[\r\n]{1}")).size() == 0) {
				masterOrSlave = "slave";
//				 System.out.println("2.. masterOrSlave=="+masterOrSlave+" listNmb=="+listNmb.trim());
			}

			String[] aryStr = { listNmb.trim(), a1[0] + "", a1[1] + "", sIdxStr, masterOrSlave };
//			System.out.println(".add aryStr=" + Arrays.toString(aryStr));
			listParas.add(aryStr);
			// xxxxx SEE ; (ii) such "ion; (ii) such C]" ENDING IN TMP10
		}

//		NLP.printListOfStringArray("listParas=", listParas);
		// delete xxxx
		String str = "";
		for (int i = 0; i < listParas.size(); i++) {
			if (i + 1 < listParas.size()) {
//				System.out.println("initial cut=" + paraGraphText.substring(Integer.parseInt(listParas.get(i)[3]),
//						Integer.parseInt(listParas.get(i + 1)[3])));
			}
		}
		return listParas;
	}

	public static String returnList(List<Integer[]> list, String type) {

		String text = "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {

			if (type.equals("sT")) {
				// System.out.println("sIdx="+list.get(i)[0]+" eIdx="+list.get(i)[1]);
			}

			sb.append("\r\n" + list.get(i)[0] + "||" + list.get(i)[1] + "||" + type + "");

		}

		return sb.toString();

	}

	public static boolean isItExecutedContract(File file, boolean fromStripFile) throws IOException, SQLException {

		NLP nlp = new NLP();
		// System.out.println("file="+file.getAbsolutePath());

		if (file.getName().toString().toLowerCase().contains("date")
				|| file.getName().toString().toLowerCase().contains("executed")) {
			// System.out.println("filename="+file.getName());
//			System.out.println("1. see filename it is an - executed k=" + file.getName());
			return true;

		}

		String text = Utils.readTextFromFile(file.getAbsolutePath());

		text = text.substring(0, (int) ((double) text.length() * .2))
				.replaceAll("(?ism)(&#160;)|(<p>|</pr>|<br>)", "$1 $2\r\n")
				.replaceAll("(?sm)(<[^>]*>)" + "|(&#[\\d]{2,5};)", "");

		if (!fromStripFile) {
			text = stripHtmlSuperSimple(text);
		}

		// if executed contract pattern found - then see if the year in the found
		// pattern matches that of the accno year. if so it is an executed K.
		Pattern patternSignedContract = Pattern.compile("(?sm)"
				+ "EXECUT[EDION]{2,3} VERSION|Execut[edion]{2,3}.{0,2}Version|"
				+ "Executed (Agreement|Contract|Trust|Pooling|Servicing)|EXECUTED (AGREEMENT|CONTRACT|TRUST|POOLING|SERVICING)|"
				+ "((?ism)(INDENTURE|Indenture|TRUST|Trust|AGREEMENT|Agreement|POOLING).{0,2}dated"
				+ "(.{0,2}as.{0,2}of)?[\r\n \t]{0,30}(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov).{1,10}\\d\\d?"
				+ ",.{0,2}[12]{1}[09]{1}[\\d]{2})");

		List<String[]> list = nlp.getAllStartIdxLocsAndMatchedGroups(text, patternSignedContract);
		// System.out.println("executed K = list.size="+list.size());
		if (list.size() == 0) {
//			System.out.println("2. it is not an executed K. file=" + file.getAbsolutePath());
			return false;
		}

		String yr = file.getName().substring(11, 13);
		if (yr.substring(0, 1).equals("9")) {
			yr = "19" + yr;
		} else {
			yr = "20" + yr;
		}

		// System.out.println("yr=" + yr);
		// System.out.println("executed K = list.size=" + list.size());
		if (list.size() == 0) {
//			System.out.println("3. it is not an executed K. file=" + file.getAbsolutePath());
			return false;
		}

		int sIdx = 0, sIdxNext = 0;
		for (int n = 0; n < list.size(); n++) {
			// System.out.println("executed K = list.get=" + Arrays.toString(list.get(n)));
			if (nlp.getAllMatchedGroups(list.get(n)[1], Pattern.compile("[12]{1}[09]{1}[\\d]{2}")).size() > 0) {
				String yrText = nlp.getAllMatchedGroups(list.get(n)[1], Pattern.compile("[12]{1}[09]{1}[\\d]{2}"))
						.get(0);
				// System.out.println("yrText=" + yrText);
				if (Math.abs(Integer.parseInt(yr) - Integer.parseInt(yrText)) > 1) {
					// System.out.println("yr don't match. yr=" + yr + " yrTxt=" + yrText);
					continue;
				}
			}

//			System.out.println("4. it is an executed K. file=" + file.getAbsolutePath());
			return true;

		}

//		System.out.println("5. it is not an executed K. file=" + file.getAbsolutePath());
		return false;

	}

	/*
	 * public static void getAccCompleteSubmission(String masterIdxPath, String
	 * typesOfForms, int startYear, int endYear, int startQtr, int endQtr, boolean
	 * onlyContract) throws IOException, ParseException, SQLException {
	 * 
	 * NLP nlp = new NLP();
	 * 
	 * String tpIdx = masterIdxPath + "/master.idx"; // e:\backtest\tableParser
	 * //System.out.println("getAcc master.idx="+tpIdx); // masterIdxPath; String
	 * tmpFolder = masterIdxPath.replaceAll("(?i)backtest","getContracts")
	 * .replaceAll("(?i)tableParser","solrIngestion\\/downLoadCompleteSubmission");
	 * Utils.createFoldersIfReqd(tmpFolder);
	 * 
	 * System.out.println("getAcc masterIdxPath: " +
	 * masterIdxPath+"\r\ntmpFolder="+tmpFolder);
	 * 
	 * File f3 = new File(tpIdx);
	 * 
	 * Calendar cal = Calendar.getInstance(); for (; startYear <= endYear;
	 * startYear++) { for (; startQtr <= endQtr; startQtr++) {
	 * 
	 * // will retrieve masterIdx if not in folder or current String localPath =
	 * GetContracts.masterIdxFolder + "/" + startYear + "/QTR" + startQtr + "/";
	 * String localPath2 = localPath; // //System.out.println("localPath1=" +
	 * localPath); File file = new File(localPath2 + "/master.idx"); TableParser tp
	 * = new TableParser(); tp.getMasterIdx(startYear, startQtr, cal); } }
	 * 
	 * BufferedReader tpIdxBR = null; try { tpIdxBR = new BufferedReader(new
	 * FileReader(tpIdx)); } catch (FileNotFoundException e1) {
	 * e1.printStackTrace(); }
	 * 
	 * String address = ""; String line; String tmpAcc; int c = 0;
	 * //System.out.println("read master.idx csv"); File fileTxt = new
	 * File(tmpFolder); File fileNc = new File(tmpFolder);
	 * 
	 * try { while ((line = tpIdxBR.readLine()) != null) { c++; if (c > 10 &&
	 * line.split("\\|").length > 3) { tmpAcc =
	 * line.split("\\|")[4].replaceAll(".txt", ""); GetContracts.acc =
	 * tmpAcc.substring(tmpAcc.length() - 20, tmpAcc.length()); GetContracts.cik =
	 * line.split("\\|")[0]; GetContracts.formType = line.split("\\|")[2];
	 * GetContracts.fileDate = line.split("\\|")[3]; GetContracts.companyName =
	 * line.split("\\|")[1]; System.out.println("getAcc:: " + GetContracts.acc +
	 * " formType: " + GetContracts.formType + " companyName: " +
	 * GetContracts.companyName + " cik: " + GetContracts.cik + " fileDate: " +
	 * GetContracts.fileDate);
	 * 
	 * if(nlp.getAllIndexStartLocations(
	 * GetContracts.formType,Pattern.compile("(?i)"+typesOfForms+"")).size()>0) {
	 * address = "https://www.sec.gov/Archives/edgar/data/" + GetContracts.cik + "/"
	 * + GetContracts.acc + ".txt"; HttpDownloadUtility.downloadFile(address,
	 * tmpFolder+"/"); fileTxt = new File(tmpFolder+"/"+GetContracts.acc+".txt");
	 * fileNc = new File(tmpFolder+"/"+GetContracts.acc+".nc"); if(fileTxt.exists())
	 * { fileTxt.renameTo(fileNc); fileTxt.delete(); }
	 * 
	 * 
	 * GetContracts.getContract(tmpFolder + "/" + GetContracts.acc +
	 * ".nc",onlyContract);
	 * 
	 * } } }
	 * 
	 * tpIdxBR.close(); } catch (IOException e) { e.printStackTrace(); } }
	 * 
	 */

	/*
	 * public static void getParentChildClausesFromXML(int startYear, int endYear,
	 * int sQ, int eQ, List<String[]> listXmlAndContractType) throws IOException {
	 * 
	 * NLP nlp = new NLP(); String text = "", doc = ""; StringBuilder sbClau = new
	 * StringBuilder(); String path = ""; for (int i = 0; i <
	 * listXmlAndContractType.size(); i++) { System.out.println("listContractTypes="
	 * + Arrays.toString(listXmlAndContractType.get(i)));
	 * 
	 * for (; endYear >= startYear; endYear--) { System.out.println("endyy=" +
	 * endYear); for (; eQ >= sQ; eQ--) { System.out.println("qtr=" + eQ );
	 * Utils.createFoldersIfReqd(SolrPrep.folderClausesXml + "/" +
	 * listXmlAndContractType.get(i)[1] + "/" + endYear + "/QTR" + eQ + "/"); //
	 * Utils.createFoldersIfReqd(SolrPrep.folderClausesXml + "/" // +
	 * listXmlAndContractType.get(i)[1] + "/" + endYear + "/QTR" + eQ + "/");
	 * 
	 * Utils.createFoldersIfReqd(SolrPrep.folderDocsWithErrorsXML + "/" +
	 * listXmlAndContractType.get(i)[1] + "/" + endYear + "/QTR" + eQ + "/");
	 * 
	 * 
	 * path = SolrPrep.folderSolrIngestion + "solr"+listXmlAndContractType.get(i)[0]
	 * + "/" + listXmlAndContractType.get(i)[1] + "/" + endYear + "/QTR" + eQ + "/";
	 * System.out.println("path=x=x="+path);
	 * 
	 * File folder = new File(path); System.out.println("folder=" +
	 * folder.getAbsolutePath()); File[] listOfFiles = folder.listFiles(); for (int
	 * c = 0; c < listOfFiles.length; c++) { //
	 * System.out.println("reading file="+listOfFiles[c].getAbsolutePath()); text =
	 * Utils.readTextFromFile(listOfFiles[c].getAbsolutePath());
	 * 
	 * List<String> listClausAndPC =
	 * getClauseAndParentChildSentence(text,listOfFiles[c]);
	 * 
	 * File fileClau = new File(SolrPrep.folderClausesXml + "/" +
	 * listXmlAndContractType.get(i)[1] + "/" + endYear + "/QTR" + eQ + "/" +
	 * listOfFiles[c].getName());
	 * 
	 * // System.out.println("write file"); Utils.writeTextToFile(fileClau,
	 * listClausAndPC.get(0));
	 * 
	 * File fileError = new File(SolrPrep.folderDocsWithErrorsXML+ "/" +
	 * listXmlAndContractType.get(i)[1] + "/" + endYear + "/QTR" + eQ + "/" +
	 * listOfFiles[c].getName()); if(listClausAndPC.size()>1) {
	 * Utils.writeTextToFile(fileError, listClausAndPC.get(1)); }
	 * 
	 * } } System.out.println("going to next year-1 . qtr=4"); eQ = 4; } } }
	 */

	/*
	 * public static List<String> getClauseAndParentChildSentence(String text, File
	 * file) throws IOException{
	 * 
	 * StringBuilder sbClause = new StringBuilder("<add>\r\n"); StringBuilder
	 * sbErrors = new StringBuilder("<add>\r\n");
	 * 
	 * List<String> listPC = new ArrayList<>(); List<String>
	 * listClauseAndParentChildSentence = new ArrayList<>(); NLP nlp = new NLP();
	 * 
	 * Pattern patternSemiColon = Pattern.
	 * compile("(?i)(; (to the extent|except|however|provided|but),? [a-z]{1})|provided, however,"
	 * );
	 * 
	 * 
	 * List<String> listDocs = nlp.getAllMatchedGroups(text,
	 * Pattern.compile("(?ism)((?<=<doc>).*?(?=</doc>))")); String claus = "",
	 * parentChild = "", parentLeadClaus = "", parentLeadClausPrior = "",
	 * leadLineage = "", leadLineageParentChildSentence = "", pOrC = "",txt = "",
	 * semiColon1 = "", semiColon2 = ""; int semiCnt = 0;
	 * 
	 * boolean leadLineageFound = false, leadLineageFoundSemiColon = false; String
	 * tmpStr = "", doc = "";
	 * System.out.println("listDocs.size()=="+listDocs.size()); String id = "",
	 * paraNo, secNo, wCnt, definition = "", section = "", exhibit = "",
	 * sameSolrFields =""; int cnt =0, cnt2 = 0; for (int c = 0; c <
	 * listDocs.size(); c++) { doc = listDocs.get(c);
	 * 
	 * id = nlp.getAllMatchedGroups(doc,
	 * Pattern.compile("(?<=<field name=\"id\">)([\\d-\\_]{26,40})(?=</field>)")).
	 * get(0); id= id+"_c";
	 * 
	 * System.out.println("doc=="+doc); paraNo = nlp.getAllMatchedGroups(doc,
	 * Pattern.compile("(?<=<field name=\"cnt\">)[\\d]{1,10}(?=</field>)")).get(0);
	 * 
	 * secNo = nlp.getAllMatchedGroups(doc,
	 * Pattern.compile("(?<=<field name=\"secNo\">)([\\d]{1,10})(?=</field>)")).get(
	 * 0);
	 * 
	 * if(c==0) { sbClause.append(solrFieldDoc); sbClause.append(solrFieldComment +
	 * solrFieldCDATA +
	 * "the txt type is 4 b/c sec typ is 0, para=1,parentChild=2, sentence=3 and clause=4.\r\n"
	 * +
	 * "The type of clause is in typ2: SC=semiColon clause, numeral or child clause (CH),\r\n"
	 * +
	 * "PS=parentSolo (no children), PL=parentLead clause, PC=parentChild, LPC=leadLineage\r\n"
	 * + "parent child, leadLineage=LL, leadLineageParentSolo=LPS. \r\n" +
	 * "id=kId_sec#_para#_c_cnt (c is for clause)." +solrFieldCDATAEnd+
	 * solrFieldEnd+"\r\n"); doc =
	 * doc.replaceAll("(<field name=\"id\">)([\\d-\\_]{26,40})(</field>)",
	 * "$1$2_c0$3").trim(); doc = doc.replaceAll("<field name=\"typ\">1</field>",
	 * "<field name=\"typ\">4</field>").trim(); doc =
	 * doc.replaceAll("(<field name=\"comment\">.*?</field>)", "").trim();
	 * sbClause.append(doc);
	 * sbClause.append(solrFieldTextType2+solrFieldCDATA+"parent child, clause etc"
	 * +solrFieldCDATAEnd+solrFieldEnd);
	 * sbClause.append(solrFieldParaNo+"0"+solrFieldEnd);
	 * sbClause.append(solrFieldTextCount+"0"+solrFieldEnd);
	 * sbClause.append(solrFieldDocEnd); continue; }
	 * 
	 * if (nlp.getAllMatchedGroups(doc,
	 * Pattern.compile("(?sm)(?<=\"sec\"><!\\[CDATA\\[).*?(?=\\]\\])")) .size() > 0)
	 * { section = nlp.getAllMatchedGroups(doc,
	 * Pattern.compile("(?sm)(?<=\"sec\"><!\\[CDATA\\[).*?(?=\\]\\])")) .get(0);
	 * 
	 * section = solrFieldSectionHeading+solrFieldCDATA +section +
	 * solrFieldCDATAEnd+solrFieldEnd;
	 * 
	 * }
	 * 
	 * if (nlp.getAllMatchedGroups(doc,
	 * Pattern.compile("(?sm)(?<=\"def\"><!\\[CDATA\\[).*?(?=\\]\\])")) .size() > 0)
	 * {
	 * 
	 * definition = nlp.getAllMatchedGroups(doc,
	 * Pattern.compile("(?sm)(?<=\"def\"><!\\[CDATA\\[).*?(?=\\]\\])")) .get(0);
	 * 
	 * definition = solrFieldDefinitionHeading+solrFieldCDATA +definition +
	 * solrFieldCDATAEnd+solrFieldEnd; }
	 * 
	 * 
	 * if (nlp.getAllMatchedGroups(doc,
	 * Pattern.compile("(?sm)(?<=\"exh\"><!\\[CDATA\\[).*?(?=\\]\\])")) .size() > 0)
	 * { exhibit = nlp.getAllMatchedGroups(doc,
	 * Pattern.compile("(?sm)(?<=\"exh\"><!\\[CDATA\\[).*?(?=\\]\\])")) .get(0);
	 * 
	 * exhibit = solrFieldExhibitHeading+solrFieldCDATA +exhibit +
	 * solrFieldCDATAEnd+solrFieldEnd;
	 * 
	 * }
	 * 
	 * sameSolrFields = definition +section+ exhibit;
	 * 
	 * claus = ""; pOrC = ""; parentChild = ""; parentLeadClaus = "";
	 * leadLineageParentChildSentence = ""; txt = ""; leadLineage ="";
	 * leadLineageFoundSemiColon = false;
	 * 
	 * if(c==0) { text = doc; } else { text =
	 * nlp.getAllMatchedGroups(doc,Pattern.compile(
	 * "(?sm)(?<=\"txt\"><!\\[CDATA\\[).*?(?=\\]\\])")).get(0).trim(); }
	 * System.out.println("doc text=="+text); tmpStr = ""; listPC =
	 * getParentChild(text); System.out.println("getParentChild finished");
	 * if(listParagraphParentChild.size()>0) {
	 * System.out.println("OutOfOrder is this paraNo=="+paraNo+" of this id="
	 * +id+" of this file="+file.getAbsolutePath());
	 * System.out.println("and this one is out of order");
	 * System.out.println(Arrays.toString(listParagraphParentChild.get(0))); //TODO:
	 * SAVE THIS TO A FOLDER CALLED - draftingErrors\ParentChild. //TODO: fix so
	 * that I pickup semi colon clauses ----> ; provided
	 * sbErrors.append(solrFieldDoc); sbErrors.append(solrFieldComment +
	 * solrFieldCDATA +
	 * "parent-child error: eg (a) to (c). sIdx is the start of error. \r\n" +
	 * "error field is para numeral in error" +solrFieldCDATAEnd+ solrFieldEnd);
	 * sbErrors.append("\r\n"+doc); sbErrors.append(solrFieldParaNo +
	 * nlp.getAllMatchedGroups(doc,
	 * Pattern.compile("(?<=<field name=\"cnt\">)[\\d]{1,10}(?=</field>)")).get(0) +
	 * solrFieldEnd); sbErrors.append(solrFieldSidx +
	 * listParagraphParentChild.get(0)[3] + solrFieldEnd);
	 * sbErrors.append(solrFieldDocEnd);
	 * 
	 * sbErrors.append(solrFieldDoc); sbErrors.append("\r\n"+doc);
	 * sbErrors.append(solrFieldParaNo + nlp.getAllMatchedGroups(doc,
	 * Pattern.compile("(?<=<field name=\"cnt\">)[\\d]{1,10}(?=</field>)")).get(0) +
	 * solrFieldEnd); sbErrors.append(solrFieldSidx +
	 * listParagraphParentChild.get(0)[3]+ solrFieldEnd);
	 * sbErrors.append(solrFieldTextType2 + solrFieldCDATA + "ER"+ solrFieldCDATAEnd
	 * + solrFieldEnd);
	 * sbErrors.append(solrFieldError+solrFieldCDATA+listParagraphParentChild.get(0)
	 * [0]+solrFieldCDATAEnd+solrFieldEnd); //
	 * sbErrors.append(solrFieldText+solrFieldCDATA+text+solrFieldCDATAEnd+
	 * solrFieldEnd); sbErrors.append(solrFieldDocEnd);
	 * 
	 * 
	 * continue;
	 * 
	 * }
	 * 
	 * cnt2++;
	 * 
	 * NLP.printListOfString("listPC==", listPC); // if (outOfOrder) { // sbClause =
	 * new StringBuilder(); // text = ""; // listPC = new ArrayList<>(); //
	 * System.out.println("outOfOrder="+outOfOrder); // continue; // }
	 * System.out.println("listParentChild.size="+listPC.size()); cnt=1; for (int i
	 * = 0; i < listPC.size(); i++) { //wCnt, when typ=-1 tmpStr ="";
	 * System.out.println("i="+i +" list.get(i)="+ listPC.get(i));
	 * 
	 * if (nlp.getAllMatchedGroups(listPC.get(i),
	 * Pattern.compile("(?sm)(?<=\\[\\[txt\\=\\=(\\[CDATA\\[)?).*?(?=\\]\\]\\]?)"))
	 * .size() > 0) { txt = nlp.getAllMatchedGroups(listPC.get(i),
	 * Pattern.compile("(?sm)(?<=\\[\\[txt\\=\\=(\\[CDATA\\[)?).*?(?=\\]\\]\\]?)"))
	 * .get(0); System.out.println("??txt="+txt); }
	 * 
	 * if (nlp.getAllMatchedGroups(listPC.get(i),
	 * Pattern.compile("(?sm)(?<=lineageLead\\=\\=(\\[CDATA\\[)?).*?(?=\\]\\])")).
	 * size() > 0 && nlp.getAllMatchedGroups(listPC.get(i),
	 * Pattern.compile("(?sm)pOrC\\=\\=")) .size() == 0 &&
	 * nlp.getAllMatchedGroups(listPC.get(i), Pattern.compile("(?sm)lineage\\=\\="))
	 * .size() == 0) { leadLineage = nlp.getAllMatchedGroups(listPC.get(i),
	 * Pattern.compile("(?sm)(?<=lineageLead\\=\\=(\\[CDATA\\[)?).*?(?=\\]\\])")).
	 * get(0).trim();
	 * 
	 * if(leadLineage.length()>0) { sbClause.append(solrFieldDoc);
	 * sbClause.append(solrFieldId+id+(cnt++)+solrFieldEnd);
	 * sbClause.append(solrFieldWordCount + leadLineage.split(" ").length +
	 * solrFieldEnd);
	 * 
	 * if(isItTableOfContents(txt)) { sbClause.append(solrFieldTextType + "-1" +
	 * solrFieldEnd); } else{ sbClause.append(solrFieldTextType + "4" +
	 * solrFieldEnd); }
	 * 
	 * sbClause.append(solrFieldSecNo+secNo+solrFieldEnd);
	 * sbClause.append(solrFieldParaNo+paraNo+solrFieldEnd);
	 * sbClause.append(sameSolrFields); sbClause.append(solrFieldTextType2 +
	 * solrFieldCDATA + "LL"+ solrFieldCDATAEnd + solrFieldEnd);
	 * sbClause.append(solrFieldText + solrFieldCDATA + leadLineage +
	 * solrFieldCDATAEnd + solrFieldEnd); sbClause.append(solrFieldDocEnd); } }
	 * //parentSolo if (nlp.getAllMatchedGroups(listPC.get(i),
	 * Pattern.compile("\\[\\[lineage\\=\\=p\\d\\d?\\]")) .size() > 0 &&
	 * nlp.getAllMatchedGroups(listPC.get(i), Pattern.compile("pOrC\\=\\=p\\]"))
	 * .size() > 0 ) {
	 * 
	 * if (txt.trim().length() > 0) { tmpStr = txt.trim();
	 * 
	 * sbClause.append(solrFieldDoc);
	 * sbClause.append(solrFieldId+id+(cnt++)+solrFieldEnd);
	 * sbClause.append(solrFieldWordCount + tmpStr.split(" ").length +
	 * solrFieldEnd);
	 * 
	 * if(isItTableOfContents(tmpStr)) { sbClause.append(solrFieldTextType + "-1" +
	 * solrFieldEnd); } else{ sbClause.append(solrFieldTextType + "4" +
	 * solrFieldEnd); }
	 * 
	 * sbClause.append(solrFieldSecNo+secNo+solrFieldEnd);
	 * sbClause.append(solrFieldParaNo+paraNo+solrFieldEnd);
	 * sbClause.append(sameSolrFields); sbClause.append( solrFieldTextType2 +
	 * solrFieldCDATA + "PS"// parent - no child + solrFieldCDATAEnd +
	 * solrFieldEnd);
	 * sbClause.append(solrFieldText+solrFieldCDATA+tmpStr+solrFieldCDATAEnd+
	 * solrFieldEnd); sbClause.append(solrFieldDocEnd); tmpStr = "";
	 * 
	 * if(leadLineage.length()>0) {
	 * 
	 * tmpStr = leadLineage.trim() + " " + txt.trim();
	 * 
	 * sbClause.append(solrFieldDoc);
	 * sbClause.append(solrFieldId+id+(cnt++)+solrFieldEnd);
	 * sbClause.append(solrFieldWordCount + tmpStr.split(" ").length +
	 * solrFieldEnd);
	 * 
	 * if(isItTableOfContents(tmpStr)) { sbClause.append(solrFieldTextType + "-1" +
	 * solrFieldEnd); } else{ sbClause.append(solrFieldTextType + "4" +
	 * solrFieldEnd); }
	 * 
	 * sbClause.append(solrFieldSecNo+secNo+solrFieldEnd);
	 * sbClause.append(solrFieldParaNo+paraNo+solrFieldEnd);
	 * sbClause.append(sameSolrFields); sbClause.append(solrFieldTextType2 +
	 * solrFieldCDATA + "LPS"// parent - no child + solrFieldCDATAEnd +
	 * solrFieldEnd);
	 * sbClause.append(solrFieldText+solrFieldCDATA+tmpStr+solrFieldCDATAEnd+
	 * solrFieldEnd); sbClause.append(solrFieldDocEnd); tmpStr = ""; } } }
	 * 
	 * 
	 * //parentLead, child, parentChild, lineageParentChild if
	 * (nlp.getAllMatchedGroups(listPC.get(i),
	 * Pattern.compile("(?<=\\[\\[parentLead\\=\\=).*?(?=\\]\\])")).size() > 0) {
	 * parentLeadClaus = nlp.getAllMatchedGroups(listPC.get(i),
	 * Pattern.compile("(?<=\\[\\[parentLead\\=\\=).*?(?=\\]\\])")).get(0).trim();
	 * 
	 * if(!parentLeadClaus.equals(parentLeadClausPrior)) { tmpStr = parentLeadClaus;
	 * 
	 * sbClause.append(solrFieldDoc);
	 * sbClause.append(solrFieldId+id+(cnt++)+solrFieldEnd);
	 * sbClause.append(solrFieldWordCount + tmpStr.split(" ").length +
	 * solrFieldEnd);
	 * 
	 * if(isItTableOfContents(tmpStr)) { sbClause.append(solrFieldTextType + "-1" +
	 * solrFieldEnd); } else{ sbClause.append(solrFieldTextType + "4" +
	 * solrFieldEnd); }
	 * 
	 * sbClause.append(solrFieldSecNo+secNo+solrFieldEnd);
	 * sbClause.append(solrFieldParaNo+paraNo+solrFieldEnd);
	 * sbClause.append(sameSolrFields); sbClause.append(solrFieldTextType2 +
	 * solrFieldCDATA + "PL"// semi colon clause + solrFieldCDATAEnd +
	 * solrFieldEnd); sbClause.append(solrFieldText + solrFieldCDATA + tmpStr +
	 * solrFieldCDATAEnd + solrFieldEnd); sbClause.append(solrFieldDocEnd); tmpStr =
	 * ""; }
	 * 
	 * if (nlp.getAllMatchedGroups(listPC.get(i),
	 * Pattern.compile("(?<=\\[\\[txt\\=\\=).*?(?=\\]\\])")) .size() > 0) { claus =
	 * nlp .getAllMatchedGroups(listPC.get(i),
	 * Pattern.compile("(?<=\\[\\[txt\\=\\=).*?(?=\\]\\])")) .get(0).trim(); tmpStr
	 * = claus;
	 * 
	 * sbClause.append(solrFieldDoc);
	 * sbClause.append(solrFieldId+id+(cnt++)+solrFieldEnd);
	 * sbClause.append(solrFieldWordCount + tmpStr.split(" ").length +
	 * solrFieldEnd);
	 * 
	 * if(isItTableOfContents(tmpStr)) { sbClause.append(solrFieldTextType + "-1" +
	 * solrFieldEnd); } else{ sbClause.append(solrFieldTextType + "4" +
	 * solrFieldEnd); }
	 * 
	 * sbClause.append(solrFieldSecNo+secNo+solrFieldEnd);
	 * sbClause.append(solrFieldParaNo+paraNo+solrFieldEnd);
	 * sbClause.append(sameSolrFields); sbClause.append(
	 * solrFieldTextType2+solrFieldCDATA+"CH" +solrFieldCDATAEnd+ solrFieldEnd);
	 * sbClause.append(solrFieldText+solrFieldCDATA+tmpStr+solrFieldCDATAEnd+
	 * solrFieldEnd); sbClause.append(solrFieldDocEnd); tmpStr = ""; }
	 * 
	 * if (claus.length() > 0 && parentLeadClaus.length() > 0) { parentChild =
	 * parentLeadClaus.trim() + " " + claus.trim(); tmpStr = parentChild;
	 * 
	 * sbClause.append(solrFieldDoc);
	 * sbClause.append(solrFieldId+id+(cnt++)+solrFieldEnd);
	 * sbClause.append(solrFieldWordCount + tmpStr.split(" ").length +
	 * solrFieldEnd);
	 * 
	 * if(isItTableOfContents(tmpStr)) { sbClause.append(solrFieldTextType + "-1" +
	 * solrFieldEnd); } else{ sbClause.append(solrFieldTextType + "4" +
	 * solrFieldEnd); }
	 * 
	 * sbClause.append(solrFieldSecNo+secNo+solrFieldEnd);
	 * sbClause.append(solrFieldParaNo+paraNo+solrFieldEnd);
	 * sbClause.append(sameSolrFields); sbClause.append(
	 * solrFieldTextType2+solrFieldCDATA+"PC" +solrFieldCDATAEnd+ solrFieldEnd);
	 * sbClause.append(solrFieldText+solrFieldCDATA+tmpStr+solrFieldCDATAEnd+
	 * solrFieldEnd); sbClause.append(solrFieldDocEnd); tmpStr = ""; }
	 * 
	 * if(leadLineage.length()>0) { leadLineageParentChildSentence =
	 * leadLineage.trim() + " " + parentChild.trim(); tmpStr =
	 * leadLineageParentChildSentence;
	 * 
	 * sbClause.append(solrFieldDoc);
	 * sbClause.append(solrFieldId+id+(cnt++)+solrFieldEnd);
	 * sbClause.append(solrFieldWordCount + tmpStr.split(" ").length +
	 * solrFieldEnd);
	 * 
	 * if(isItTableOfContents(tmpStr)) { sbClause.append(solrFieldTextType + "-1" +
	 * solrFieldEnd); } else{ sbClause.append(solrFieldTextType + "4" +
	 * solrFieldEnd); }
	 * 
	 * sbClause.append(solrFieldSecNo + secNo + solrFieldEnd);
	 * sbClause.append(solrFieldParaNo + paraNo + solrFieldEnd);
	 * sbClause.append(sameSolrFields); sbClause.append(solrFieldTextType2 +
	 * solrFieldCDATA + "LPC" + solrFieldCDATAEnd + solrFieldEnd);
	 * sbClause.append(solrFieldText + solrFieldCDATA + tmpStr + solrFieldCDATAEnd +
	 * solrFieldEnd); sbClause.append(solrFieldDocEnd); tmpStr = ""; }
	 * parentLeadClausPrior = parentLeadClaus;
	 * 
	 * }
	 * 
	 * 
	 * semiColon1 = ""; semiColon2 = ""; semiCnt = nlp.getAllMatchedGroups(txt,
	 * Pattern.compile("(?i); (to the extent|except|however|provided|but),? " +
	 * "[a-z]{1}")).size();
	 * 
	 * System.out.println("txt contains this many '; provided'==" + semiCnt);
	 * 
	 * if (txt.contains(";") && semiCnt > 0) { System.out.println("; found in txt=="
	 * + txt); semiColon1 = nlp.getAllMatchedGroups(txt,
	 * Pattern.compile(".*?;")).get(0) .replaceAll("\\[\\[|\\[CDATA\\[",
	 * "").trim().replaceAll("lineageLead\\=\\=", "").trim(); semiColon2 =
	 * txt.substring(txt.indexOf(";") + 1,
	 * txt.length()).trim().replaceAll("lineageLead\\=\\=", "").trim();
	 * 
	 * sbClause.append(SolrPrep.solrFieldDoc);
	 * 
	 * sbClause.append(solrFieldId+id+(cnt++)+solrFieldEnd);
	 * sbClause.append(solrFieldWordCount + tmpStr.split(" ").length +
	 * solrFieldEnd);
	 * 
	 * if(isItTableOfContents(semiColon1)) { sbClause.append(solrFieldTextType +
	 * "-1" + solrFieldEnd); } else{ sbClause.append(solrFieldTextType + "4" +
	 * solrFieldEnd); } sbClause.append(sameSolrFields);
	 * sbClause.append(SolrPrep.solrFieldTextType2 + SolrPrep.solrFieldCDATA +
	 * "SC"// semi colon clause + SolrPrep.solrFieldCDATAEnd + solrFieldEnd);
	 * sbClause.append(SolrPrep.solrFieldText + SolrPrep.solrFieldCDATA + semiColon1
	 * + SolrPrep.solrFieldCDATAEnd + solrFieldEnd);
	 * sbClause.append(solrFieldDocEnd);
	 * 
	 * 
	 * sbClause.append(SolrPrep.solrFieldDoc);
	 * 
	 * sbClause.append(solrFieldId+id+(cnt++)+solrFieldEnd);
	 * sbClause.append(solrFieldWordCount + tmpStr.split(" ").length +
	 * solrFieldEnd);
	 * 
	 * if(isItTableOfContents(semiColon2)) { sbClause.append(solrFieldTextType +
	 * "-1" + solrFieldEnd); } else{ sbClause.append(solrFieldTextType + "4" +
	 * solrFieldEnd); }
	 * 
	 * sbClause.append(sameSolrFields); sbClause.append(SolrPrep.solrFieldTextType2
	 * + SolrPrep.solrFieldCDATA + "SC"// semi colon clause +
	 * SolrPrep.solrFieldCDATAEnd + solrFieldEnd);
	 * sbClause.append(SolrPrep.solrFieldText + SolrPrep.solrFieldCDATA + semiColon2
	 * + SolrPrep.solrFieldCDATAEnd + solrFieldEnd);
	 * sbClause.append(solrFieldDocEnd);
	 * 
	 * }
	 * 
	 * System.out.println("8/9="+listPC.get(i)); }
	 * 
	 * // xxxxxx - end of loop of <doc> }
	 * 
	 * // System.out.println("cnt2>>="+cnt2); if(cnt2>0) { // ensures that if no
	 * clause parent/children - the comment doc isn't saved as // that's the only
	 * field and there's as a result no data. sbClause.append("</add>");
	 * listClauseAndParentChildSentence.add(sbClause.toString());
	 * 
	 * if (listParagraphParentChild.size() > 0) { sbErrors.append("</add>");
	 * listClauseAndParentChildSentence.add(sbErrors.toString()); }
	 * 
	 * } else{ listClauseAndParentChildSentence.add(""); }
	 * 
	 * NLP.printListOfString("list...", listPC);
	 * 
	 * return listClauseAndParentChildSentence;
	 * 
	 * }
	 */
	public static String getContractName(String text) throws IOException {

		NLP nlp = new NLP();

		text = text.substring(0, (int) ((double) text.length() * .2))
				.replaceAll("(?ism)(&#160;)|(<p>|</pr>|<br>)", "$1 $2\r\n")
				.replaceAll("(?sm)(<[^>]*>)" + "|(&#[\\d]{2,5};)", "");

		text = stripHtmlSuperSimple(text);

		// if executed contract pattern found - then see if the year in the found
		// pattern matches that of the accno year. if so it is an executed K.
		Pattern patternContractName = Pattern.compile("(?sm).{0,2}dated"
				+ "(.{0,2}as.{0,2}of)?[\r\n \t]{0,30}(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov).{1,10}\\d\\d?"
				+ ",.{0,2}[12]{1}[09]{1}[\\d]{2})");

//		List<String[]> list = nlp.getAllStartIdxLocsAndMatchedGroups(text, patternContractName);
		// System.out.println("executed K = list.size="+list.size());

		return "";
	}

	private static SolrDocumentList readDocsFromSolr(String url, String core, String q, String[] FQs)
			throws SolrServerException, IOException {
		// System.out.println("readDocsFromSolr...url="+url);
		SolrJClient cl = new SolrJClient(url, core);
		Map<String, String> otherParams = new HashMap<>();
		otherParams.put("rows", "5000");
//		System.out.println("q="+q+"\r\nFQs="+Arrays.toString(FQs));
		QueryResponse resp = cl.search(q, FQs, null, otherParams);
		return resp.getResults();
	}

	public static SolrDocumentList readAdminDocsFromSolr(SolrAdminDocumentType docType, String q, String[] FQs)
			throws SolrServerException, IOException {
		FQs = ArrayUtils.add(FQs, "documentType:(" + docType + ")");
//		System.out.println("..FQs="+Arrays.toString(FQs)+"\r\n docType="+docType);

		return readDocsFromSolr("http://localhost:8983/solr", "coreAdmin", q, FQs);
	}

	public static List<List<String>> readAdminDataFromSolr(SolrAdminDocumentType docType, String[] FQs)
			throws SolrServerException, IOException {
		FQs = ArrayUtils.add(FQs, "documentType:(" + docType + ")");
		// TODO: expand FQs to allow for filtering by core, contractType, etc.

		SolrDocumentList solrDocs = readAdminDocsFromSolr(docType, "*:*", FQs);
		if (null == solrDocs || solrDocs.size() == 0)
			return null;
		if (docType == SolrAdminDocumentType.legalPack) {
			// TODO: read specific fields
		} else {
			List<List<String>> response = new ArrayList<>();
			for (SolrDocument sd : solrDocs) {
				response.add((List<String>) sd.getFieldValue(docType.getFieldName()));
			}
			return response;
		}
		return null;
	}

	public static enum SolrAdminDocumentType {
		synonyms("synonyms"), clientSentence("patterns"), longIdPattern("ngramList"), substantive("patterns"),
		longIdKeywords("patterns"), legalPack("commonFqs"), termCreationPreReqs("");

		private String fieldName;

		SolrAdminDocumentType(String fieldName) {
			this.fieldName = fieldName;
		}

		public String getFieldName() {
			return fieldName;
		}

	}

	public static void main(String[] args) throws IOException, SQLException, ParseException {

		/*
		 * NOTE: DO NOT DELETE If I leave out repeating fields in solr DB I reduce solr
		 * size by 75 perent. As a result in order to search by contract type I first
		 * search only where cnt=0 and then set typ to desired clause category and run
		 * idPoolCaptureSearchNumberMustBeMoreThanALLcontractsInSolrFolders. I only need
		 * to run once and thereafter I can call Ids and filter by legal term. I can
		 * also choose not to use the filter at all and start with prudent person as the
		 * initial filter.
		 */

		// NOTE: java regex goes bonkers when I use "_". -- DO NOT USE '_' IN REGEX!!!
		// TODO: provided; <--- in paragraph.xml CANNOT be the end of a paragraph!!!

		// HOW THIS WORKS: See *1., *2. and *3.
		GetContracts gK = new GetContracts();
		SolrPrep.LawGo = true;
		int startYr = 2005, endYr = startYr;// *1 should be complete, but ck cnt.
		// chg to 1900 when NOT using GoLaw (UI)
		// *2 next is 2018 for *3. next=2010
		int sQtr = 1, eQtr = 1;

		if (LawGo) {// keep 1801 as location dedicated to GoLaw
			startYr = 1801;
			sQtr = 1;
			eQtr = 1;
		}

		String folder = "e:\\getContracts\\solrIngestion\\solrSentences\\Indenture\\1801\\QTR1";
		File fFolder = new File(folder);
		if (fFolder.exists())
			FileUtils.forceDelete(new File(folder));
		Utils.createFoldersIfReqd(folder);

		folder = "e:\\getContracts\\solrIngestion\\solrClauses\\Indenture\\1801\\QTR1";
		fFolder = new File(folder);
		if (fFolder.exists())
			FileUtils.forceDelete(new File(folder));
		Utils.createFoldersIfReqd(folder);

		folder = "e:\\getContracts\\solrIngestion\\solrParentChild\\Indenture\\1801\\QTR1";
		fFolder = new File(folder);
		if (fFolder.exists())
			FileUtils.forceDelete(new File(folder));
		Utils.createFoldersIfReqd(folder);

		folder = "e:\\getContracts\\solrIngestion\\solrParagraphs\\Indenture\\1801\\QTR1";
		fFolder = new File(folder);
		if (fFolder.exists())
			FileUtils.forceDelete(new File(folder));
		Utils.createFoldersIfReqd(folder);

		folder = "e:\\getContracts\\solrIngestion\\solrStripped\\Indenture\\1801\\QTR1";
		fFolder = new File(folder);
		if (fFolder.exists())
			FileUtils.forceDelete(new File(folder));
		Utils.createFoldersIfReqd(folder);

//		 *1. gK.dateRangeQuarters downloads from secZipfiles and puts in
		// solrIngestion/download folders. Must set patternContractsToInclude,
		// patternThisContractsToInclude and patternContractTypesToExclude
		patternContractsToInclude = Pattern.compile(
				patternIndenture.pattern() + "|" + patternSuppIndenture.pattern() + "|" + patternPSA.pattern());
		/*
		 * see full list of patternsToInclude above
		 */
		patternContractsLongNamesToInclude = Pattern.compile(patternThisIndenture.pattern() + "|"
				+ patternThisSuppIndenture.pattern() + "|" + patternThisPSA.pattern());
		// see full list of patternsToInclude above

		patternContractTypesToExclude = Pattern.compile(patternAccountBankAg.pattern() + "|"
				+ patternAdministrativeAgentAg.pattern() + "|" + patternReinsuranceTrustAg.toString()// dumby so we
																										// don't grab
																										// everythimg
				+ "|" + patternCollateralAdminAg.pattern() + "|" + patternCollateralMgtAg.pattern() + "|"
				+ patternCollateralPledgeAg.pattern() + "|" + patternControlAg.pattern() + "|"
				+ patternCustody.pattern() + "|" + patternDepositaryAg.pattern() + "|" + patternDepositoryAg.pattern()
				+ "|" + patternEscrow.pattern() + "|" + patternExhangeAg.pattern() + "|" + patternFiscalAg.pattern()
				+ "|" + patternFundAdminAg.pattern() /* + "|" + patternIndenture.pattern() */
				/*
				 * + "|"+ patternSuppIndenture.pattern() + "|" +
				 * patternInterCreditorAg.pattern()
				 */ + "|" + patternListingAgentAg.pattern() + "|" + patternLoanCreditAg.pattern() + "|"
				+ patternPayingAg.pattern() + "|" + patternPortfolioAdminAg.pattern() /* + "|" + patternPSA.pattern() */
				+ "|" + patternPurchase.pattern() + "|" + patternReinsuranceTrustAg.pattern() + "|"
				+ patternRepurchaseAg.pattern() + "|" + patternSaleServicingAg.pattern() + "|"
				+ patternSecLending.pattern() + "|" + patternTransferAg.pattern() + "|"
				+ patternCorporateActions.pattern() + "|" + patternOpinionInBody.pattern() + "|"
				+ patternPriceSuppProsp.pattern() + "|" + patternOTHER.pattern() + "|" + patternExclude.pattern());

		/*
		 * gK.dateRangeQuarters (this method JUST puts secZipFiles in downloaded
		 * folder). HOW TO USE: put Ks I want to include into patternContractsToInclude.
		 * Everything else is what I want to exclude & put into
		 * patternContractTypesToExclude each. For where I use contractBody to find
		 * contract types - I use patternThisContractsToInclude which requires the
		 * contract type precede with the word "This" such as "THIS INDENTURE dated..."
		 */

//		gK.dateRangeQuarters(startYr, endYr, sQtr, eQtr, true/*
//																 * // * must set SolrPrep.patternContractsToInclude and
//																 * // * SolrPrep.patternContractTypesToExclude //
//																 */);

		// 2*. getAccCompleteSubmission will gather directly from www.sec.gov these form
		// types and place the downloaded txt into solrIngestion/download folders.
		// Bypasses secZipFiles
		// String masterIdxPath = GetContracts.masterIdxFolder+startYr+"/QTR"+sQtr+"/";
		// getAccCompleteSubmission(masterIdxPath,
		// "424|485APOS|485BPOS|FWP",startYr,endYr,sQtr,eQtr, justDescription);

		// setting sbSec and sbParChi and sbPOS to false - I just get sent.xml, para.xml
		// with stop/stem. If I want parent child or part of speech set to true. This
		// may reparse everything
		sbSec = false;
		sbParChi = false; // re-instate marked out stop/stem/pos

		// *3. this will strip and make .xml files for submission to solr. See notes re
		// POS.
		// 2001 started for all. Laptop ONLY indentures and PSAs
		// p-up @2000. will cycle through each K type.
//		listContractAttributes = getContractAttributes();

		/*
		 * P: listContractAttributes loads up the contractTypes to fetch from downloaded
		 * folder. We should reconsider approach. The filtering mechanisms I'm using
		 * need a close and improvement
		 */

		// not needed for gK
		// isItExecutedContract method checks if K is dated in contract long name and
		// executedContractFound finds in body or long name it is executed K

		// NOTE: to have legal-UI work: folder=1901/Q1, regenerate etc =true. If it
		// isn't working ReBuild Linc_D.. at 'build/build-downloader.xml" and select
		// "run as" then ant. Then hit f5 to refresh then COPY
		// /Linc_Downloader/WEB-INF/lib/downloader-1.0.jar to --->
		// /legalTerm-ui/src/main/webapp/WEB-INF/lib/downloader-1.0.jar THEN reinstall
		// VIA "run as" ==>maven install mave
		// at legalTerm-ui
		;

		// PGW -- must first getContractAttributes(); - see above. HOWEVER - we should
		// explore how to better manage retrieval of what types of contracts we want
		// and/or all contracts etc. The current method is too complex and ineffective -
		// poor results. NOTE for later that often contracts are buried in form 8-k.
		// getSolrReady is run to parse all the downloaded and preliminarily prepared
		// files contained in each accno.nc file that was in each daily secZipFile that
		// getContracts method performed

		SolrPrep.regenerate = true;
		SolrPrep.strip = true;
		SolrPrep.getSolrReady(startYr, endYr, sQtr, eQtr);
		System.out.println("ClauseXML");
//		ClauseXML.getClauseFromSentenceXML(startYr, endYr, sQtr, eQtr, SolrPrep.folderSentencesXml, "indenture");
	}
}
