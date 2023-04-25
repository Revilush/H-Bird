package contracts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrServerException;

import com.google.protobuf.Internal.ListAdapter;

import xbrl.NLP;
import xbrl.Utils;

public class EntityRecognition {

	public static List<String> ListOfLegalEntitiesInContract = new ArrayList<>();

	public static Pattern patternEntityTypesAllCaps = Pattern.compile("(?sm)" + "( |^)("
			+ "CORPORATION|INCORPORATED|NATIONAL ASSOCIATION|LIMITED|BRANCH|STATUTORY TRUST|BANCORPORATION|BANCORP|AUTHORITY"
			+ "|& COMPANY|& CO|SERVICING OF|Servicing of"
			+ "|TRUST( [\\d-]+)|TRUST|FUND|PORTFOLIOS?( SERIES)?( [IXV]{1,3})?" + "|MASTER (NOTE )?TRUST( [\\d-]+)?"
			+ ")" + "(([\\.]{1}|([ ,]{1,2})" + "|(?=[\r\n]{1,2})|\\)" + "|$))" + "|" + "( |^)"
			+ "(INC\\.?|CORP\\.?|C[Oo]\\.|sitting alone( S\\.R\\.L\\.)?|FSB|LLC|LP|LLP|NA|AG|SRL|SA|NV|FSB\\.?|PLC\\.?|LTD\\.?|PC|P\\.C\\.|PLLC)"
			+ "(([\\.]{1}|([ ,]{1,2})" + "|(?=[\r\n]{1,2})|\\)|$))"
			+ "|( |^)(S\\.A\\.|N\\.V\\.|L\\.L\\.C\\.|L\\.P\\.|L\\.L\\.P\\.|N\\.A\\.|A\\.G\\.|S\\.A\\.|N\\.V\\.|F\\.S\\.B\\."
			+ "|B\\.\\V." + "|\\(NCUA\\)|P\\.L\\.C\\.|S\\.R\\.L\\.|S\\.A\\.R\\.L\\."
			+ "|TRUST COMPANY (OF )?(THE )?[A-Z]{3,15}|(TRUST )?COMPANY( AMERICAS)?|GROUP(,? INC\\.?|CORP\\.|,? INCORPORATED)?"
			+ "|INVESTMENT COMPANY" + "(?!(,? ([ICFPNSA]{1}\\.|National|Limi|Branch|Inc|Corp|LL|LTD|Ltd|Pte|PTE|GMBH)))"
			+ ")" + "((\\.|([ ,]{1,2})|(?=[\r\n]{1,2})|\\)|$))");// Always at end of entity name

	public static Pattern patternEntityTypesInitialCaps = Pattern.compile("(?sm)"
			+ "( |^)(Savings Association|Corporation|Incorporated|National Association|Limited|Branch|Statutory Trust"
			+ "|Bancorporation|Bancorp|& Company|\\& Co"
			+ "|Trust( [\\d-]+)|Trust|Fund|Portfolios?( Series)?( [IVX]{1,3})?|Master (Note )?Trust( [\\d-]+)?)(([\\.]{1}|([ ,]{1,2})|(?=[\r\n]{1,2})|\\)|$))"
			+ "|"
			+ "( |^)(Inc\\.?|Corp\\.?|Co\\.|llc\\.?|lp|llp\\.?|fsb\\.?|plc\\.?|fsb|ltd\\.?|Ltd\\.?)(([\\.]{1}|([ ,]{1,2})|(?=[\r\n]{1,2})|\\)|$))"
			+ "|( |^)(l\\.l\\.c\\.|l\\.p\\.|l\\.l\\.p\\.|n\\.a\\.|a\\.g\\.|s\\.a\\.|n\\.v\\.\\|f\\.s\\.b\\."
			+ "|p\\.l\\.c\\.|S\\.a\\.r\\.l\\.|Partners|Company(,? [A-Z]{1}[a-z\\.]{2,4})?"
			+ "|Trust Company ([Oof]{2} )?([Tthe]{3} )?[A-Z]{3,15}|(Trust )?Company( Americas)?Group(, Inc\\.?|Corp\\.?)?"
			+ "(?!(,? ([FPNSAL]{1}\\.|National|Limi|Branch|Inc|Corp|LL|LTD|Ltd|Srl|srl|Srl|gmbh|Gmbh)))"
			+ ")(([\\.]{1}|([ ,]{1,2})|(?=[\r\n]{1,2})|\\)|$))");// Always at end of entity name

	public static Pattern patternALLEntityTypes = Pattern
			.compile(patternEntityTypesAllCaps.pattern() + "|" + patternEntityTypesInitialCaps.pattern());

	public static Pattern patternBankOrTrustCo = Pattern
			.compile("(?sm)(" + "BANK|BANCSHARES|TRUST COMPANY( AMERICAS)?|TRUST CO\\.?|"
					+ "Bank|Bancshares|Trust Company( Americas)?|Trust Co\\.?" + ")" + "[, \r\n]{1}");

	public static Pattern patternAnyEntityName = Pattern.compile("(?sm)" + ""// add stand alones here
			+ "((?ism) State Street Bank and Trust Company of California, N\\.A\\.)|"
			+ "((?ism) Deutsche Bank Trust Company (of )Americas )|" + "( |\r\n)(" + ""
			+ "[\\)A-Z\\.'\\-\\&]+[\\)\r\n]{0,2}" + "(" + "[\\(\\)A-Za-z\\.'\\-\\&\\d/]+" + ")?"
			+ "($|[,\\. \r\n]{1,2})(of |OF |AND )?" + "" + ")+");

	public static Pattern patternExcludeMe = Pattern.compile("(?sm)"
			+ "EX-[\\d\\.]{2}|APPOINT| If | IF |Attention|ATTENTION|:|Fundamental|FUNDAMENTAL|Change|Change|^INDUSTRIES$|^Industries$|Name of"
			+ "|NAME OF|ACCOUNT|AUDITOR|ARTICLE|ASSET MANAGER|BANK PRODUCT|EACH COMPANY|COMPANY (TAX|NEW)|SECRETARY|BOARD"
			+ "|SECRECY|PARENT COMPANY|^BANK OF$|PRESIDENT|ISSUER|DOCUMENT|WITNESS|WHEREOF|TAXABLE|AGREEMENT|AMENDED|RESTATED|COMMON|NOT "
			+ "|Not |Stock|RECITALS?|UPON |Upon |Company May|COLLECTIONS?|Collections?|Recitals?|AGAINST|EXHIBIT|Execution"
			+ "|[A-Z] EXECUTION|DATED? |Dated? |Schedule|SCHEDULE|Form |FORM "
			+ "|[a-z] Guarant|[a-z] GUARANT|AUTHORIZ|JOINT|INDEMNI|INFORMATION|INTEREST|Interest|PARENT COMPANY|Parent Company"
			+ "|PROVIDER|Provider|Successor|SUCCESSOR|The (Corporation|Company)|Lists?|LISTS?|REPORTS|Reports|Section|SECTION"
			+ "|[\\-\\_]{2}|Cash |Assets|TERMINATION|Consoli|CONSOLI"
			+ "|^ (Investment Company( Act)?|Clearing Corporation|Limited Liability Company Agreement)[ \\.]$|Agent|Each |Exhibit "
			+ "|Clearing (Agenc[iesy]{1,3}|Corporation)|Federal Reserve Bank"
			+ "|^ (INVESTMENT COMPANY( ACT)?|CLEARING CORPORATION|LIMITED LIABILITY COMPANY AGREEMENT)[ \\.]$"
			+ "|CLEARING (AGENC[IESY]{1,3}|CORPORATION)|FEDERAL RESERVE BANK"
			+ "|The United States Investment Company|Division|Company Order|Any Corporation|U\\.S\\. Corporation|Authoriz"
			+ "|Account|Auditor|Bank Product|Each Company|Company (New|Tax)|Board|Secretary|Secrecy|^Bank of$|President|Issuer"
			+ "|THE UNITED STATES INVESTMENT COMPANY"
			+ "|^Liability Company|Agreement|Article|Asset Manager|Appoint|Document|Carrying|Amended|Restated"
			+ "|^Limited Liability Company|^LIABILITY COMPANY|^LIMITED LIABILITY COMPANY"
			+ "|Investment Company|INVESTMENT COMPANY|Agent Bank|AGENT BANK|Bank Holding Company|BANK HOLDING COMPANY"
			+ "|Opinion|OPINION|Initial|INITIAL|Issuing Bank|ISSUING BANK|\\. The Fund|\\. THE FUND|DIRECTOR|Director|Head|HEAD"
			+ "|Joint|Indemni|Information|Purchaser|[a-z] Issuer|INITIAL|PURCHASER|ISSUER|Organization|ORGANIZATION"
			+ "|STATE OF|State of|Surviving|SURVIVING|Subsidiaries|SUBSIDIARIES|GENERAL CORPORATION|General Corporation|Bank\\.|BANK\\."
			+ "|[a-z]{2}\\..{1,6}(Compan|Bank)|[A-Z]{2}\\..{1,6}(COMPAN|BANK)|The Company|THE COMPANY|[\\d]{5,}" + "");

	public static boolean itIsAnEntity(String text) {

//		System.out.println("text===="+text);
		String s = (" " + text).replaceAll("(" + patternALLEntityTypes.toString() + ")", "").trim();
//		System.out.println("s=="+s);
		if (s.length() < 2) {
//			System.out.println("excluded = it only contains entity type=" + text);
			return false;
		}

		return true;
	}

	public static boolean entityTypeIsAtEndOfName(String text) throws IOException {
		NLP nlp = new NLP();
		List<String[]> list = nlp.getAllEndIdxAndMatchedGroupLocs(text, patternALLEntityTypes);
		int l = 0;
		if (nlp.getAllIndexEndLocations(text, Pattern.compile("N\\.A\\.")).size() > 0)
			l = nlp.getAllIndexEndLocations(text, Pattern.compile("N\\.A\\.")).get(0);
//		System.out.println("entityTypeIsAtEndOfName text=="+text);

		int eIdx = Integer.parseInt(list.get(list.size() - 1)[0]);
		if (l > eIdx)
			eIdx = l;

		if (text.length() - eIdx > 2) {
//			System.out.println("excluded = entity type isn't at end=" + text);
			return false;
		}

//		System.out.println("NOT EXCLUDED "+text);
		return true;
	}

	public static int entityIsInString(String text) throws IOException {
		NLP nlp = new NLP();
		List<String[]> list = nlp.getAllEndIdxAndMatchedGroupLocs(text, patternALLEntityTypes);
		if (list.size() == 0)
			return 0;
		int eIdx = Integer.parseInt(list.get(list.size() - 1)[0]);
//		NLP.printListOfStringArray("", list);
//		System.out.println("text.len="+text.length()+" eIdx="+eIdx);
//		System.out.println("entity="+text.substring(0,eIdx));

		return eIdx;
	}

	public static boolean excludeMe(String text) throws IOException {
		NLP nlp = new NLP();

		for (int a = 0; a < text.split("\r\n").length; a++) {
			if (nlp.getAllMatchedGroups(text.split("\r\n")[a], patternExcludeMe).size() == 0)
				return false;
		}

		return true;

	}

	public static List<String[]> isItTwoEntities(String[] ary) throws IOException {

		NLP nlp = new NLP();

		List<String[]> list = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(ary[0], patternALLEntityTypes);
		if (list.size() < 2)
			return null;

		int eIdx = 0, sIdxNext = 0, sIdx = 0, sIdxOriginal = Integer.parseInt(ary[1]);
		String entity = "";
//		NLP.printListOfStringArray("there could be two entities -- list==", list);
		// goes through list check to see if there are 2 entity types. Saves them. Then
		// It checks if there are two entity names.
		List<String[]> list2Entities = new ArrayList<String[]>();

		for (int i = 0; i < list.size(); i++) {
			eIdx = Integer.parseInt(list.get(i)[2]);

			if (eIdx - sIdx < 2) {
//				System.out.println("eIdx-sIdx<2=="+(eIdx-sIdx));
				continue;
			}

//			System.out.println(
//					"this is 1 of 2 entity=" + ary[0].substring(sIdx, eIdx).trim() + " sIdx=" + sIdx + " eIdx=" + eIdx);

			if (i + 1 == list.size()) {
				entity = ary[0].substring(sIdx, eIdx);
				if (excludeMe(entity) || entity.toLowerCase().contains("branch"))
					continue;
				String[] ar = { entity, "" + (sIdx + sIdxOriginal), "" + (sIdx + sIdxOriginal + entity.length()) };
//				System.out.println("22-1 adding ary=" + Arrays.toString(ar) + " ary[0]==" + ary[0]);
				list2Entities.add(ar);
			}

			if (i + 1 < list.size()) {
				entity = ary[0].substring(sIdx, eIdx);
				if (excludeMe(entity) || entity.toLowerCase().contains("branch"))
					continue;
				String[] ar = { entity, "" + (sIdx + sIdxOriginal), "" + (eIdx + sIdxOriginal) };
//				System.out.println("22-2 adding ary=" + Arrays.toString(ar));
				list2Entities.add(ar);
			}
			sIdx = eIdx;

			if (i + 1 < list.size()) {
				sIdxNext = Integer.parseInt(list.get(i + 1)[1]);
				if (sIdxNext - eIdx < 3) {
//					System.out.println("eIdx="+eIdx+" sIdxNext="+sIdxNext+" RETURN NULL");
					return null;
				}
			}
		}

//		System.out.println("original ary==" + Arrays.toString(ary));
//		NLP.printListOfStringArray("list2Entities==", list2Entities);
		for (int i = 0; i < list2Entities.size() - 1; i++) {
			if (nlp.getAllMatchedGroups(list2Entities.get(i)[0], Pattern.compile("(?i)trust")).size() > 0
					&& nlp.getAllMatchedGroups(list2Entities.get(i + 1)[0], Pattern.compile("(?i)company|N\\.A\\.?"))
							.size() > 0

			) {
//				System.out.println("return null");
				return null;

			}
		}
		return list2Entities;

	}

	public static List<String[]> getEntities(String text, boolean applyPanyFileter) throws IOException {

		int maxWordsInAnEntityName = 12;
		NLP nlp = new NLP();
		boolean found = false;

		int splitCommaLen = 0, txtLn = 0;
		String entity = "", sIdxStr = "", eIdxStr = "";
		List<String[]> listEntities = new ArrayList<String[]>();

		Pattern patternPany = Pattern.compile(
				"(\r\n[A-Za-z\\d\\.\\$\\%\\)]{1,3})([ ]+[A-Za-z\\d\\.\\$\\%\\)]{1,3}[ ]+)|([\\$\\d\\%\\.,\\(\\)]+[ \t]+)([\\$\\d\\%\\.,\\(\\)])");

//		System.out.println("patternPany? size==" + nlp.getAllIndexEndLocations(text, patternPany).size());
		if (applyPanyFileter && nlp.getAllIndexEndLocations(text, patternPany).size() > 150) {
			return listEntities;
		}

//		if (nlp.getAllIndexEndLocations(text, patternPany).size() > 200) {
//			System.out.println("return listEntities");
//			txtLn = (int) (0.25 * (double) text.length());
//			text = text.substring(0, txtLn);
//		}
//
//		if (nlp.getAllIndexEndLocations(text, patternPany).size() > 200) {
//			System.out.println("return listEntities");
//			txtLn = (int) (0.25 * (double) text.length());
//			text = text.substring(0, txtLn);
//		}
//
//		if (nlp.getAllIndexEndLocations(text, patternPany).size() > 200) {
//			return listEntities;
//		}

		List<String[]> list = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(text, patternAnyEntityName);
		// if legal entity is actually a defined term then discard it.
//		System.out.println("this many possible entities==" + list.size());
//		System.out.println("text.lennnnn="+text.length());
//		NLP.printListOfStringArray("all patterns--", list);
//		boolean excludeMe = false;
		for (int i = 0; i < list.size(); i++) {
//			excludeMe = false;
			splitCommaLen = 0;
			found = false;
			entity = list.get(i)[0];
			sIdxStr = list.get(i)[1];
			eIdxStr = list.get(i)[2];
//			System.out.println("initial go at an entity name="+entity);
			// if "as InitCap InitCap", Entity Name -- then drop

			if (isItAllCapsText(sIdxStr, eIdxStr, entity, text)) {
				// SOMETIMES THE TEXT IS A GIANT PARAGRAPH OF ALL CAPITALIZED TEXT. THIS
				// CONFUSES REGEX. SO THIS IDENTIFIES ALL CAP TEXT AND SKIPS IT
//				System.out.println("isAllCapsText");
				continue;
			}

//			System.out.println("list.get(i)="+Arrays.toString(list.get(i)));
			List<String[]> listTwoMoreEntities = isItTwoEntities(list.get(i));
			if (null != listTwoMoreEntities && listTwoMoreEntities.size() > 1) {
//				System.out.println("listTwoMoreEntities?==" + listTwoMoreEntities.size());
				// sometimes the entity pattern captures 2 entities at the same - this figures
				// out if that occurred and fixes it:
				// Momentive Performance Materials Inc., WELLS FARGO BANK, NATIONAL ASSOCIATION,
				// NA - this would be picked up as one entity - but it is 2. this gets fixed
				for (int n = 0; n < listTwoMoreEntities.size(); n++) {
//					System.out.println("adding two or more entities?==" + Arrays.toString(listTwoMoreEntities.get(n)));
					if (listTwoMoreEntities.get(n)[0].split(" ").length <= maxWordsInAnEntityName) {
						listEntities.add(listTwoMoreEntities.get(n));
//						System.out.println("adding 22-3: " + Arrays.toString(listTwoMoreEntities.get(n)));
					}
				}
				continue;
			}

			if (excludeMe(entity.split(",")[0]) && entity.length() - entity.split(",")[0].length() > 8
					&& nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(entity.split(",")[1], patternAnyEntityName)
							.size() > 0) {
//				System.out.println("skipping exclude me. entity=" + entity);
				// This is skipping over the excludeMe text and starting at its end point. Then
				// treats the rest as an entity.
				// picks-up where there is an entity
//				Initially, The Bank of New York Mellon Trust Company, N.A. will <---grabs as The Bank of New York Mellon Trust Company, N.A. 
				sIdxStr = (Integer.parseInt(sIdxStr) + entity.split(",")[0].length()) + "";
//				System.out.println("exclud aa text.sub entity="
//						+ text.substring(Integer.parseInt(sIdxStr), Integer.parseInt(eIdxStr)));
				entity = entity.substring(entity.split(",")[0].length(), entity.length());
			}

			String[] ary = { entity, sIdxStr + "", (Integer.parseInt(eIdxStr)) + "" };
			int noOfWords = entity.trim().split(" ").length;
//			System.out.println("entity=" + entity);
//			System.out.println("itIsnEntity=" + itIsAnEntity(entity));
//			System.out.println("entityTypeIsAtEndOfName=" + entityTypeIsAtEndOfName(entity));
//			System.out.println("nlp.getAllMatchedGroups(entity, patternALLEntityTypes).size()=="
//					+ nlp.getAllMatchedGroups(entity, patternALLEntityTypes).size());
//			System.out.println("entityTypeIsAtEndOfName(entity)==="+entityTypeIsAtEndOfName(entity));

			// don't add #s b/c of street address
			if (excludeMe(entity)) {
//				System.out.println("11entity=|" + entity + "| exclude me=" + excludeMe(entity));
				continue;
			}

			if (nlp.getAllMatchedGroups(entity, patternALLEntityTypes).size() > 0) {
				if ((itIsAnEntity(entity) && entityTypeIsAtEndOfName(entity))
				// && nlp.getAllMatchedGroups(entity, patternBankOrTrustCo).size() == 0
				) {
//					System.out.println(
//							"1. is entity-x-" + entity + " entity.split  comma.len=" + entity.split(", ").length);

					splitCommaLen = entity.split(", ").length;

					if (splitCommaLen == 3) {
						// fixes===>as Trustee, Deutsche Bank AG, London Branch
//						System.out.println("entity split=3. entity.split[0]=" + entity.split(", ")[0]);
						sIdxStr = "" + (Integer.parseInt(sIdxStr) + entity.split(", ")[0].length() + 1);
						entity = text.substring(Integer.parseInt(sIdxStr), Integer.parseInt(eIdxStr));
//						System.out.println("2 split,  entity==="+entity+"|");
						noOfWords = entity.trim().split(" ").length;
						if (noOfWords > 1) {
							String[] ary2 = { entity, sIdxStr, (Integer.parseInt(eIdxStr)) + "" };
							if (entity.split(" ").length <= maxWordsInAnEntityName) {
								listEntities.add(ary2);
								found = true;
//								System.out.println("1 adding entity=" + entity);
							}
						}
					}

					if (!found && splitCommaLen < 3) {
						noOfWords = entity.trim().split(" ").length;
						if (noOfWords > 1) {
							if (entity.split(" ").length <= maxWordsInAnEntityName) {
//								System.out.println("2 adding entity=" + entity + "fix 000 on its own line here||");
								listEntities.add(ary);
								found = true;
							}
						}
					}
				}
			}

			if (!found && nlp.getAllMatchedGroups(entity, patternBankOrTrustCo).size() > 0
					&& nlp.getAllMatchedGroups(entity, patternALLEntityTypes).size() == 0) {

				// sometimes when the entity is a bank of trust company - it doesn't have clear
				// entity ending suffix types (entity types) identified. eg Trust Company is
				// ending entity type. This method focuses on banks and trust companies when not
				// found in more common format above

				if (itIsAnEntity(entity) && entity.trim().split(" ").length > 1) {
//					System.out.println("2. entity.len=" + entity.length() + "\r\nbank/trust co = i=" + i + " DONE="
//							+ entity.trim() + "| found==" + found + "entity.trim().split(\" \").len="
//							+ entity.trim().split(" ").length);

					noOfWords = entity.trim().split(" ").length;
					if (noOfWords > 1) {
						if (entity.split(" ").length <= maxWordsInAnEntityName) {
							listEntities.add(ary);
							found = true;
//							System.out.println("3 adding entity=" + entity);
						}
					}
				}
			}

			if (!found && entity.split(",").length > 1
					&& nlp.getAllMatchedGroups(entity, patternBankOrTrustCo).size() > 0
					&& nlp.getAllMatchedGroups(entity.split(",")[1], patternALLEntityTypes).size() == 0
					&& nlp.getAllMatchedGroups(entity, patternALLEntityTypes).size() > 0) {
				String e = entity.split(",")[1];
				// this is similar attempt as prior but for banks or trust companies.
				// fixes: Company, The Bank of New York

				if (itIsAnEntity(entity) && e.trim().split(" ").length > 1) {
//					System.out.println("3. entity.len=" + e.length() + "\r\nbank/trust co = i=" + i + " DONE="
//							+ e.trim() + "| found==" + found + "entity.trim().split(\" \").len="
//							+ e.trim().split(" ").length);

					noOfWords = entity.trim().split(" ").length;
					if (noOfWords > 1) {
						String[] ary2 = { e, "" + (entity.split(",")[0].length() + Integer.parseInt(sIdxStr)),
								(Integer.parseInt(eIdxStr)) + "" };
						if (entity.split(" ").length <= maxWordsInAnEntityName) {
							listEntities.add(ary2);
							found = true;
//							System.out.println("4 adding entity=" + entity);
						}
					}
				}
			}

			if (!found && nlp.getAllEndIdxAndMatchedGroupLocs(entity, patternALLEntityTypes).size() > 0
					&& !entityTypeIsAtEndOfName(entity) && itIsAnEntity(entity)) {

				if (nlp.getAllEndIdxAndMatchedGroupLocs(entity, Pattern.compile(" Trust Company,? ?(n\\.a\\.?)?"))
						.size() > 0) {
					String[] ar = { entity, (Integer.parseInt(eIdxStr) - entity.length()) + "",
							(Integer.parseInt(eIdxStr)) + "" };
					listEntities.add(ar);
					found = true;
				}

//				System.out.println("not found and entity name isn't at end==" + entity);
				// fixes: Citibank, N.A., 480 Washington Street
				entity = entity.substring(0, nlp.getAllIndexEndLocations(entity, patternALLEntityTypes).get(0));

				noOfWords = entity.trim().split(" ").length;
				if (noOfWords > 1 && !entity.toLowerCase().contains("deutsche")) {
//					System.out.println("3 entity wasn't at end - fixed===" + entity + "|");
					String[] ary2 = { entity, sIdxStr, "" + (Integer.parseInt(sIdxStr) + entity.length()) + "" };
					if (entity.split(" ").length <= maxWordsInAnEntityName) {
						listEntities.add(ary2);
						found = true;
//						System.out.println("5 adding entity=" + entity);
					}
				}
			}

			if (!found && nlp.getAllIndexStartLocations(
					list.get(i)[0].replaceAll("xxPD", "").replaceAll("CBxx", "").replaceAll(" xxOB", "")
							.replaceAll("[ ]+", " ").trim(),
					Pattern.compile("(?ism)(corporation|n\\.?a|inc|l\\.p|ltd|l\\.?l\\.?c|l\\.?l\\.?p|corp)\\.?,?$"))
					.size() > 0
					&& list.get(i)[0].replaceAll("xxPD", "\\.").replaceAll("CBxx", "").replaceAll(" xxOB", "")
							.replaceAll("[ ]+", " ").split(" ").length > 1) {
				String[] ary2 = { entity, sIdxStr, "" + (Integer.parseInt(sIdxStr) + entity.length()) + "" };
				listEntities.add(ary2);
				found = true;
//				System.out.println("last hurrah found=" + found);
			}

//			if (!found) {
//				System.out.println("not found="+list.get(i)[0]);
//			}
		}

//		NLP.printListOfStringArray("listEntities====", listEntities);

		return listEntities;
	}

	public static boolean isItAllCapsText(String sIdxStr, String eIdxStr, String entity, String text)
			throws IOException {

		NLP nlp = new NLP();
		int sIdx = Integer.parseInt(sIdxStr), eIdx = Integer.parseInt(eIdxStr);
//		System.out.println("all caps? text="+text.substring(Math.max(0, sIdx-250),Math.min(eIdx+250, text.length())));
		// get last index of \r\n before sIdx
		// get first index of \r\n after eIdx
		// use those two idx locations to cut text.
		// check if all characters are all caps

		String tmpStr = text.substring(0, sIdx);
		int sIdxHardReturn = tmpStr.lastIndexOf("\r\n");
		tmpStr = text.substring(eIdx, text.length());
		int eIdxHardReturn = tmpStr.indexOf("\r\n");

//		System.out.println("sIdx="+sIdx+" sIdxHardReturn=" + sIdxHardReturn + " eIdx=" + eIdx + " eIdxHardReturn=" + eIdxHardReturn);
		if (sIdxHardReturn < 0) {
			sIdxHardReturn = 0;
		}
		tmpStr = text.substring(sIdxHardReturn, (eIdx + eIdxHardReturn));
		if (tmpStr.length() > 75)
			tmpStr = tmpStr.substring(0, 75);
//		System.out.println("tmpStr.length()==" + tmpStr.length() + " tmpStr.replaceAll(\"[a-z]+\", \"\").length()"
//				+ tmpStr.replaceAll("[a-z]+", "").length());
//		System.out.println("aa tmpStr.replaceAll(\"[a-z]+\", \"\")=="+tmpStr.replaceAll("[^a-z]+", ""));
//		System.out.println("A tmpStr===="+tmpStr.trim()+"\r\nB tmpStr.replaceAll(\"^[A-Z]+\", \"\")="+tmpStr.replaceAll("^[A-Z]+", "").trim());

		int endIdx = 100000, lsize = 0, tmpSlenNA = 0;
		if (nlp.getAllIndexEndLocations(tmpStr.trim(), patternALLEntityTypes).size() > 0) {
			lsize = nlp.getAllIndexEndLocations(tmpStr.trim(), patternALLEntityTypes).size();
			endIdx = nlp.getAllIndexEndLocations(tmpStr.trim(), patternALLEntityTypes).get(lsize - 1);
			tmpSlenNA = tmpStr.replaceAll("N\\.A\\.", "").trim().length();
//			System.out.println("endIdx="+endIdx+ " tmpStr.len="+tmpStr.length()+" tmpstr.len repl n.a."+tmpSlenNA);

		}

		// cases where all caps but ends in INC.
		if (tmpStr.length() == tmpStr.replaceAll("[a-z]+", "").length() && Math.abs(tmpStr.trim().length() - endIdx) > 2
				&& Math.abs(tmpSlenNA - endIdx) > 2 && tmpStr.trim().length() > 60) {
//			System.out.println("endIdx=="+endIdx);
//			System.out.println("line is all caps - tmpStr=="+tmpStr);
			return true;
		}

//		System.out.println("hard return to hard return=="+tmpStr+"||");
		return false;

	}

	public static String boldEntities(List<String[]> list, String text) throws IOException {

//		 NLP nlp = new NLP();
		String entity = "", str = "", strPrior = "", gap = "";
		int eIdx = 0, sIdx = 0, eIdxPrior = 0, boldCountingIsApainfulFngSport = 0;
//		System.out.println("boldEntities.size=" + list.size());
		for (int i = 0; i < list.size(); i++) {
			entity = list.get(i)[0];
//			System.out.println("entity…" + entity);
			sIdx = Integer.parseInt(list.get(i)[1]);// start of entity
			eIdx = Integer.parseInt(list.get(i)[2]);// end of entity
//			System.out.println("origText.cut from eIdx="+text.substring(eIdx));
			if (eIdxPrior < sIdx)
				gap = text.substring(eIdxPrior, sIdx);// gap
			entity = "<b>" + entity + "</b>";
			// sIdx has to change by this value
			str = strPrior + gap + entity;
//			System.out.println("sIdx=" + sIdx + " eIdx=" + eIdx + " i" + i + " text.len=" + text.length());
//			System.out.println("strPrior="+strPrior+"|| gap="+gap+"|| entity="+entity);
			strPrior = str;
			eIdxPrior = eIdx;

		}

		// i find the entity to bold - I have its sIdx and its eIdx.
		// from the prior eIdx (start of next cut) to sIdx is my text
		// I cut from sIdx to eIdx and that word is my entity. I add that bolded entity
		// I then surround it with <b>entity</b>.
		// I then go from the eIdx to the next sIdx as the gap to add at end of last
		// string.
//		System.out.println("last bit="+text.substring(eIdx));
//		System.out.println("orig text="+text);
		str = str + text.substring(eIdx, text.length());
		boldCountingIsApainfulFngSport = 0;
		// text.len doesn't count for newly included <b>,</b>. Therefore to get the part
		// of text after str - I take the original
		// length of string (str.len minus the number of inclusions of <b>,</b> and that
		// is the starting point of the txt to append

//		System.out.println("3 str="+str.trim()+"endStr");

		str = str.replaceAll(", </b>", "</b>, ").replaceAll("<b>\\), ", "\\), <b>").replaceAll("\\)</b>", "</b>\\)")
				.replaceAll("<b> ", " <b>").replaceAll("XOP", "\\(").replaceAll("xcp", "\\)")
				.replaceAll("(?sm)(</b>)(, N\\.A\\.)", "$2$1").replaceAll("(<b>)(, )", "$2$1")
				// .replaceAll("(<b>)( ?[A-Za-z]{2,15})( [A-Za-z]{2,15}, )","$2$3$1")//<b>Owner
				// Trustee,
				// can't do this b/c then Continental Airlines, Inc.
				.replaceAll(
						"(<b>)( ?[A-Za-z]{2,15}, )(?!(?i)(INC\\.?|CORP\\.?|CO\\.|LLC|LP|LLP|NA|AG|SA|NV|FSB\\.?|PLC|LTD|PC))",
						"$2$1")// <b>Depositor,
				.replaceAll("(</b>)(,? ?((L\\.?)?L\\.?P|I[NCnc]{2}|L\\.?L\\.?C)\\.?)", "$2$1")// </b>, INC.
//				<b>CONWOOD COMPANY </b>L.P
				.replaceAll("(</b>)(,? ?NATIONAL ASSOCIATION|,? ?National Association)", "$2$1")// group 2 can't be
																								// preceded by a ','
				.replaceAll("([A-Z]{2,15}, )(<b>)(N\\.A\\.|INC\\.|Inc\\.)(</b>)", "$2$1$3$4")// SEECO, <b>Inc.</b>
				.replaceAll(",</b>", "</b>,").replaceAll("</b><b>", "").replaceAll("(<b>)([\\d,\\$]+)", "$2$1")
				.replaceAll("(<b>[\r\n]+)(<br>)", "$2$1");

//		System.out.println("4 str="+str.trim());
		return str;
	}

	public static TreeMap<Integer, String> getLegalEntities(String text, boolean applyPanyFileter) throws IOException {

		NLP nlp = new NLP();

		text.replaceAll("(?ism)(XQ|Qx)+", "")
//		.replaceAll("xxPD", "\\.")
				.replaceAll("SxxPDAxxPD", "SA")
//		.replaceAll("CBxx", "").replaceAll(" xxOB", "")
				.replaceAll("[ ]+", " ");
		text = text
//				.replaceAll("xxPD", "\\.")
				.replaceAll("([ ]+)?[\t]+([ ]+)?(?=\r\n)", "").replaceAll("L\\.P\\.", "LP xxLPxx")
//				.replaceAll("S\\.A\\.", "SA xxSAxx")
				.replaceAll("L\\.L\\.C\\.", "LLC xxLLCxx").replaceAll("L\\.L\\.P\\.", "LLP xxLLPxx")
				.replaceAll("" + "(\\()" + "([A-Z]{1,15}[a-z]{0,15})" + "(\\))", "OPxx$2xxCP")
				.replaceAll("and Income Fund", "Xand XIncome Fund");
//		List<String> listOfOpenParas = nlp.getAllMatchedGroups(text, Pattern.compile("(?sm)\"txt\".*?(?=\"txt\")"));
//		NLP.printListOfString("", listOfOpenParas);
		StringBuilder sb = new StringBuilder();
//		System.out.println("read set go:");
//		File fn = new File("d:/getContracts/temp/openingParas1.html");
//		System.out.println("text.len=="+text.length());
//		System.out.println("listOfOpenParas.size()="+listOfOpenParas.size());
//		if (listOfOpenParas.size() == 0)
//			listOfOpenParas.add(text);
//		List<String>

//		for (int i = 0; i < listOfOpenParas.size(); i++) {

//			System.out.println("1 text.len" + text.length());
//			text = listOfOpenParas.get(i);
//			Utils.writeTextToFile(fn, text);
		text = text.replaceAll("(?i)(Dated as of )([A-Z]{1}[A-Za-z]+ )(.{1,7}[\\d]{4} ?\r?\n?)?", "$1xxxx$2xxxx$3xxxx");
//		System.out.println("repl1");
		text = text.replaceAll("(f.{0,1}k.{0,1}a.{0,1}|a.{0,1}k.{0,1}a.{0,1}|known)( )(as)", "$1xxxx$3 ")
				.replaceAll("(?ism)(?<= as) (?=[A-Z]{1}[A-Za-z]{1,15})", "xxxx");
//		System.out.println("repl2");

		List<String[]> listEntitiesTmp = getEntities(text, applyPanyFileter);
//		NLP.printListOfStringArray("listEntitiesTmp==", listEntitiesTmp);
		List<String[]> listEntities = new ArrayList<String[]>();
		for (int c = 0; c < listEntitiesTmp.size(); c++) {
			if (c + 1 < listEntitiesTmp.size() && listEntitiesTmp.get(c)[2].contentEquals(listEntitiesTmp.get(c + 1)[1])
					&& nlp.getAllMatchedGroups(listEntitiesTmp.get(c)[0], patternALLEntityTypes).size() == 0) {
//				System.out.println(
//						"merging these TWO 1:" + listEntitiesTmp.get(c)[0] + " 2:" + listEntitiesTmp.get(c + 1)[0]);
				String[] ary = { listEntitiesTmp.get(c)[0] + listEntitiesTmp.get(c + 1)[0], listEntitiesTmp.get(c)[1],
						listEntitiesTmp.get(c + 1)[2] };
//				System.out.println("adding 6=" + Arrays.toString(ary));
				listEntities.add(ary);
				c++;
				continue;
			}

			else {
//				System.out.println("adding 7=" + Arrays.toString(listEntitiesTmp.get(c)));
				listEntities.add(listEntitiesTmp.get(c));
			}
		}

//			NLP.printListOfStringArray("222 listEntities==", listEntities);

		// System.out.println("2 text.len="+text.length());
		text = boldEntities(listEntities, text);
//			System.out.println("after 1 bold="+text);
		text = text
//					.replaceAll("(<b>)([\r\n]+)", "$2$1")
//					.replaceAll("([\r\n]+)(</b>)", "$2$1")
				.replaceAll("\r\n", "\r\n\r\n<br><br>").replaceAll("xxxx", " ")
				.replaceAll("( COMPANY )(</b>)(OF [A-Z]{2,15})", "$1$3$2")
				.replaceAll("(a )(<b>)([A-Z]{1}[a-z]{2,15} )(Corporation)(</b>)", "$1$3$4	")
				.replaceAll("(</b>)( ?and Savings Association)", "$2$1");
		sb.append(text);
//		}

		text = sb.toString();
//		System.out.println("after 2 bold="+text);
//		fn = new File("d:/getContracts/temp/openingParas1.html");
//		Utils.writeTextToFile(fn, text);

		text = text.replaceAll("(?ism)Deutsche Bank Trust Company Americas",
				"<b>Deutsche Bank Trust Company Americas</b>");
		text = text.replaceAll("(Citibank, <b>N\\.A\\.</b>)", "<b>Citibank, N\\.A\\.</b>");
		text.replaceAll("(Citibank, <b>N\\.A\\. </b>)", "<b>Citibank, N\\.A\\.</b> ");
		text = text.replaceAll("(?sm)\"txt\":", "").replaceAll("[ ]+[\\},\\]\\{]+|\\[\r\n", "")
				.replaceAll("<br><br>", "<br>")
//				.replaceAll("\\\\", "")
//				.replaceAll("<br>[ ]+\"|\"\r\n", "")
				.replaceAll("(, As [A-Za-z]{1,30})(</b>)", "$2$1").replaceAll("(<b>)([A-Za-z]+)(</b>)", "$2")
				.replaceAll("(?sm)(<b>)(.{0,7}\\d\\d?\\-)", "$2$1").replaceAll("LLC </b>xxLLCxx", "L.L.C. </b>")
				.replaceAll("LLP </b>xxLLPCxx", "L.L.P. </b>").replaceAll("LP ?</b> ?xxLPxx", "L.P. </b>")
				.replaceAll(" </b>,", ",</b>").replaceAll("<br></b>", "</b><br>")
				.replaceAll("(<b>)(.*?)(</b>)(\")", "$2$4").replaceAll("(<b>)(Company Group.?)(</b>)", "$2")
				.replaceAll("(\\.)(</b>)(  ?[A-Z]{1})", "$2$1$3").replaceAll("(\\.)(</b>)(  ?[A-Z]{1})", "$2$1$3")
				.replaceAll("LP xxLPxx", "L\\.P\\.").replaceAll("LLC xxLLCxx", "L\\.L\\.C\\.")
				.replaceAll("LLP xxLLPxx", "L\\.L\\.P\\.").replaceAll("OPxx", "\\(").replaceAll("xxCP", "\\)")
				.replaceAll("Xand XIncome", "and Income");// unbold 1 word entity name

//		System.out.println("after 3 bold="+text);

//		fn = new File("d:/getContracts/temp/openingParas2.html");
//		Utils.writeTextToFile(fn, text);

//		fn = new File("d:/getContracts/temp/openingParas.html");
//		System.out.println("text.len=="+text.length());
//		Utils.writeTextToFile(fn, text);
		TreeMap<Integer, String> mapLegalEntityLocation = legalEntitiesMapped(text);
//		List<String> listRoles = nlp.getAllMatchedGroups(text, Pattern.compile("(?sm)(?<=\").{2,40}\"(?=.{1,50})<b>.{1,30}</b>"));
//		NLP.printListOfString("1 listRoles===", listRoles);
//		System.out.println("text.len="+text.length());
//		System.out.println("I'm done! - where's allen?");
//		NLP.printMapIntStr("mapLegalEntityLocation==", mapLegalEntityLocation);

		// remove duplate legalEntities b/c one is lower case and the other is
		// uppercase.

		String key = "";
		int sIdx = 0;
		TreeMap<String, String[]> mapDupRemoved = new TreeMap<String, String[]>();
		for (Map.Entry<Integer, String> entry : mapLegalEntityLocation.entrySet()) {
//			System.out.println("xxx key=" + entry.getKey() + "| val|" + entry.getValue());
			key = entry.getValue().toLowerCase().replaceAll("[ ]+", " ").replaceAll("\\.", "");
			if (mapDupRemoved.containsKey(key))
				continue;
			String[] ary = { entry.getKey() + "", entry.getValue() };
			mapDupRemoved.put(key, ary);
		}

		TreeMap<Integer, String> mapLegalEntityLoc = new TreeMap<Integer, String>();
		Pattern pattern_exclude = Pattern.compile("^(Corporate Trust|Application |N\\.A\\.?)");
		for (Map.Entry<String, String[]> entry : mapDupRemoved.entrySet()) {
			if (nlp.getAllMatchedGroups(entry.getValue()[1], pattern_exclude).size() > 0)
				continue;
			mapLegalEntityLoc.put(Integer.parseInt(entry.getValue()[0]), entry.getValue()[1]);
		}

		return mapLegalEntityLoc;

	}

	public static boolean isRemainder(int total, int divisor) {

		{

//	        int quotient = total / divisor; 
			int remainder = total % divisor;
//	        System.out.println("The Quotient is = " + quotient); 
//	        System.out.println("The Remainder is = " + remainder); 

			if (remainder > 0)
				return true;
			else {
				return false;
			}
		}

	}

	public static TreeMap<Integer, String> legalEntitiesMapped(String text) throws IOException {
		TreeMap<Integer, String> mapLegalEntityLocation = new TreeMap<Integer, String>();
		GoLaw gl = new GoLaw();
//		System.out.println("legalEntitiesMapped");
		NLP nlp = new NLP();
		List<String> list = nlp.getAllMatchedGroups(text, Pattern.compile("(?sm)(?<=<b>).*?(?=</b>)"));
//		text = text.replaceAll("</?b>", "");
		TreeMap<String, Double> mapSD = new TreeMap<String, Double>();
		TreeMap<Double, String> mapDS = new TreeMap<Double, String>();

		Pattern patternStatesAndCountry = Pattern.compile(
				"^(((?ism)ALASKA|ALABAMA|ARKANSAS|AMERICAN[\r\n ]{0,3}SAMOA|ARIZONA|CALIFORNIA|COLORADO|CONNECTICUT|DISTRICT[\r\n ]{0,3}OF[\r\n ]{0,3}"
						+ "COLUMBIA|DELAWARE|FLORIDA|FEDERATED[\r\n ]{0,3}STATES[\r\n ]{0,3}OF[\r\n ]{0,3}MICRONESIA|GEORGIA|GUAM|HAWAII|IOWA|IDAHO"
						+ "|ILLINOIS|INDIANA|KANSAS|KENTUCKY|LOUISIANA|MASSACHUSETTS|MARYLAND|MAINE|MARSHALL[\r\n ]{0,3}ISLANDS|MICHIGAN|MINNESOTA"
						+ "|MISSOURI|NORTHERN[\r\n ]{0,3}MARIANA[\r\n ]{0,3}ISLANDS|MISSISSIPPI|MONTANA|NORTH[\r\n ]{0,3}CAROLINA|NORTH[\r\n ]{0,3}DAKOTA"
						+ "|NEBRASKA|NEW[\r\n ]{0,3}HAMPSHIRE|NEW[\r\n ]{0,3}JERSEY|NEW[\r\n ]{0,3}MEXICO|NEVADA|NEW[\r\n ]{0,3}YORK|OHIO|OKLAHOMA|OREGON"
						+ "|PENNSYLVANIA|PUERTO[\r\n ]{0,3}RICO|PALAU|RHODE[\r\n ]{0,3}ISLAND|SOUTH[\r\n ]{0,3}CAROLINA|SOUTH[\r\n ]{0,3}DAKOTA|TENNESSEE"
						+ "|TRUST[\r\n ]{0,3}TERRITORIES|TEXAS|UTAH|WEST[\r\n ]{0,3}VIRGINIA|VIRGIN[\r\n ]{0,3}ISLANDS|VERMONT|WASHINGTON|WISCONSIN|VIRGINIA"
						+ "|WYOMING"
						+ "|England|English|Whales|Ireland|Spain|Germany|Japan|China|Hong Kong|South Korea|Malayisa|Vietnam|Singapore|Australia"
						+ "|India|China|Russia" + "|New South Whales"
						+ "|Liechtenstein|Italy|Israel|France|UK|U\\.K\\.|Luxembourg|Swiss|Canada|Ontario|Quebec|Mexico|Brazil|Argentina"
						+ "|Bolivia|Uruguay|Suriname|Paraguay|Falkland|Chile|Ecuador|Uruguay|Guatemala|Trinidad|Guyana|Venezuela|Guinia|Peru|Panama|Hondura"
						+ "|Puerto Rico|Dominican Republic" + "|British Columbia|Cayman|Bermuda|Guernsey|Jersey"
						+ "|Liberia|Virgina Islands"
						+ "|Afghanistan|Albania|Algeria|Andorra|Angola|Antigua and Barbuda|Argentina|Armenia|Australia|Austria|Azerbaijan|Bahamas|Bahrain"
						+ "|Bangladesh|Barbados|Belarus|Belgium|Belize|Benin|Bhutan|Bolivia|Bosnia and Herzegovina|Botswana|Brazil|Brunei|Bulgaria"
						+ "|Burkina Faso|Burundi|C.{1,2}te d.{1,2}Ivoire|Cabo Verde|Cambodia|Cameroon|Canada|Central African Republic|Chad|Chile|China"
						+ "|Colombia|Comoros|Congo|Costa Rica|Croatia|Cuba|Cyprus|Czechia|Czech Republic|Denmark|Djibouti|Dominica|Dominican Republic|Ecuador"
						+ "|Egypt|El Salvador|Equatorial Guinea|Eritrea|Estonia|Eswatini| Swaziland|Ethiopia|Fiji|Finland|France|Gabon|Gambia|Georgia|Germany"
						+ "|Ghana|Greece|Grenada|Guatemala|Guinea|Guinea-Bissau|Guyana|Haiti|Holy See|Honduras|Hungary|Iceland|India|Indonesia|Iran|Iraq"
						+ "|Ireland|Israel|Italy|Jamaica|Japan|Jordan|Kazakhstan|Kenya|Kiribati|Kuwait|Kyrgyzstan|Laos|Latvia|Lebanon|Lesotho|Liberia"
						+ "|Libya|Liechtenstein|Lithuania|Luxembourg|Madagascar|Malawi|Malaysia|Maldives|Mali|Malta|Marshall Islands|Mauritania|Mauritius"
						+ "|Mexico|Micronesia|Moldova|Monaco|Mongolia|Montenegro|Morocco|Mozambique|Myanmar|Burma |Namibia|Nauru|Nepal|Netherlands"
						+ "|New Zealand|Nicaragua|Niger|Nigeria|North Korea|Macedonia|Norway|Oman|Pakistan|Palau|Palestine State|Panama|Papua New Guinea"
						+ "|Paraguay|Peru|Philippines|Poland|Portugal|Qatar|Romania|Russia|Rwanda|Saint Kitts| Nevis|Saint Lucia"
						+ "|Saint Vincent.{1,10}Grenadines|Samoa|San Marino|Sao Tome.{1,6}Principe|Saudi Arabia|Senegal|Serbia|Seychelles|Sierra Leone"
						+ "|Singapore|Slovakia|Slovenia|Solomon Islands|Somalia|South Africa|South Korea|South Sudan|Spain|Sri Lanka|Sudan|Suriname"
						+ "|Sweden|Switzerland|Syria|Tajikistan|Tanzania|Thailand|Timor.{1,2}Leste|Togo|Tonga|Trinidad.{1,6}Tobago|Tunisia|Turkey"
						+ "|Turkmenistan|Tuvalu|Uganda|Ukraine|United Arab Emirates|United Kingdom|Uruguay|Uzbekistan|Vanuatu|Venezuela|Vietnam|Yemen"
						+ "|Zambia|Zimbabwe)|Federal|FEDERAL)");

		double cntg = 0.000001;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).trim().replaceAll(",$", "").split(" ").length < 2
					|| list.get(i).replaceAll(",$", "").replaceAll("(?i)national association", "").length() < 2
					|| (nlp.getAllMatchedGroups(list.get(i).trim().replaceAll("[\\d,\\.]+|", "").trim(),
							Pattern.compile(
									"(?i)Fund$|Trust$|Portfolio$|Portfolios$|Portfolio [IVX]{1,2}$|Portfolios [IVX]{1,2}$"))
							.size() > 0
							&& list.get(i).replaceAll("(?i)(of |the |no )", "").replaceAll("[ ]+", " ").trim()
									.split(" ").length < 4))
				continue;

			mapSD.put(list.get(i).trim().replaceAll(",$", "").replaceAll(",? [\\d\\%]{1,3}\\.", ""),
					(list.get(i).length() + cntg));
			cntg = cntg + 0.000001;
//			System.out.println("put in map=" + list.get(i));
		}

		ListOfLegalEntitiesInContract = new ArrayList<>();
		for (Map.Entry<String, Double> entry : mapSD.entrySet()) {
			mapDS.put(-entry.getValue(), entry.getKey());
		}

		int cnt = 0;
		List<String[]> listReplNo = new ArrayList<String[]>();
		// listReplNo ensure I search and replace an Entity name once - longest names go
		// first, but a long name can contain a short name, and this prevents this.
		String str = "";

		for (Map.Entry<Double, String> entry : mapDS.entrySet()) {
			cnt++;

			str = entry.getValue().replaceAll("^\\(", "OPxx").replaceAll("\\)$", "xxCP");

			if (nlp.getAllMatchedGroups(str, Pattern.compile("[\\)]+")).size() != nlp
					.getAllMatchedGroups(str, Pattern.compile("[\\(]+")).size()) {
				str = entry.getValue().replaceAll("\\)", "xxCP").replaceAll("\\(", "OPxx");
			}
//			System.out.println("str=" + str);
			// System.out.println("=" + " key=" + entry.getKey() + " val=" + str + "|");
//			System.out.println("html residue size="+nlp.getAllIndexEndLocations(text, Pattern.compile("[<>]+")).size());
			if (nlp.getAllIndexEndLocations(text, Pattern.compile("[<>]+")).size() > 0)
				continue;

			text = text.replaceAll("(?ism)("
					+ str.replaceAll("[\\*\\+\"\\$\\[\\]]+", "").replaceAll("\\)", "xxCP").replaceAll("\\(", "OPxx")
					+ ")", "<b>xzxz" + cnt + "xzxz</b>");
			String[] ary = { cnt + "", str };
//			System.out.println("array =" + ary);
			listReplNo.add(ary);
		}

		for (int i = 0; i < listReplNo.size(); i++) {
//			System.out.println("listReplNo.get(i)[0]--" + listReplNo.get(i)[0]);
			text = text.replaceAll("xzxz" + listReplNo.get(i)[0] + "xzxz", listReplNo.get(i)[1]);
		}

		text = text.replaceAll("(\")(<b>)(.*?)(</b>)(\")", "$1$3$5");
		ListOfLegalEntitiesInContract = nlp.getAllMatchedGroups(text, Pattern.compile("(?<=<b>).*?(?=</b>)"));
//		System.out.println("ListOfLegalEntitiesInContract.siz==" + ListOfLegalEntitiesInContract.size());
		TreeMap<String, String> mapFinal = new TreeMap<String, String>();
		for (int i = 0; i < ListOfLegalEntitiesInContract.size(); i++) {
			mapFinal.put(ListOfLegalEntitiesInContract.get(i).replaceAll("[\\&, ;'\"\\.]+", "").toLowerCase(),
					ListOfLegalEntitiesInContract.get(i));
		}

//		NLP.printMapStringString("mapFinal=", mapFinal);
//		NLP.printListOfString("ListOfLegalEntitiesInContract===", ListOfLegalEntitiesInContract);

		List<String> listOfLegalEntities = new ArrayList<String>();
		for (Map.Entry<String, String> entry : mapFinal.entrySet()) {
			if (nlp.getAllMatchedGroups(entry.getValue(),
					Pattern.compile("(?ism)routing|article|information|floor| of$|^of")).size() == 0
					&& nlp.getAllMatchedGroups(entry.getValue().replaceAll("[\\.\\(\\) ]+", ""),
							Pattern.compile("(?ism)^(authority|newyork)$")).size() == 0
					&& nlp.getAllMatchedGroups(entry.getValue().trim(), patternStatesAndCountry).size() == 0) {
//				System.out.println("adding to list.. entry.getValue() == " + entry.getValue());
				listOfLegalEntities.add(entry.getValue());
			}
		}
//		NLP.printListOfString("2.ListOfLegalEntitiesInContract===", ListOfLegalEntitiesInContract);
		for (int i = 0; i < listOfLegalEntities.size(); i++) {
//			System.out.println("333.listOfLegalEntities==" + listOfLegalEntities.get(i));

			if (nlp.getAllIndexEndLocations(listOfLegalEntities.get(i), Pattern.compile("(?ism)("
					+ "Redempt|, The|Whereas|Obligation|Merger?(!.{1,20}(Inc|Corp|LLC|LLP|LP)\\.?)|Subsidiary of|laws|Certificate|Security|of$"
					+ "|OF THE|Associate|Fee |Disclosure" + "|"
					+ "Reference|regulat|^Parent|^Management|class |effective| time|acting|through"
					+ "|Series|Trustee|CEO|President)")).size() > 0
					|| (nlp.getAllIndexEndLocations(listOfLegalEntities.get(i),
							Pattern.compile("(?ism)(trust|inc|llc|llp|corp|trust comp)")).size() == 0
							&& nlp.getAllIndexEndLocations(listOfLegalEntities.get(i),
									Pattern.compile("(?ism)[12]{1}[09]{1}[\\d]{2}|Master Servicer")).size() > 0)) {
//				System.out.println("continue=" + listOfLegalEntities.get(i));
				continue;
			}
//			if (nlp.getAllIndexEndLocations(text,
//					Pattern.compile(listOfLegalEntities.get(i).replaceAll("[\\.\\(\\)]", ""))).size() > 0) 
			else {
//				System.out
//						.println(
//								".put=" + listOfLegalEntities.get(i).replaceAll("(?ism)'s|\\.$", "").replaceAll("xxPD", "\\.")
//								.replaceAll("CBxx", "").replaceAll(" xxOB", "").replaceAll("[ ]+", " "));
				mapLegalEntityLocation.put(i,
						listOfLegalEntities.get(i).replaceAll("(?ism)'s|\\.$", "").replaceAll("xxPD", "\\.")
								.replaceAll("CBxx", "").replaceAll(" xxOB", "").replaceAll("[ ]+", " "));
			}
		}
//		NLP.printMapDoubleString("mapDS===", mapDS);
//		System.out.println("Done");
//System.out.println("text=="+text);
//		NLP.printMapIntStr("aa mapLegalEntityLocation=", mapLegalEntityLocation);
		return mapLegalEntityLocation;

	}

	public static void getNameOfContract(String text) throws IOException {
		NLP nlp = new NLP();
		// This ..... (this "....")

//		text = "INDENTURE dated as of August 22, 2012 among CAESARS OPERATING ESCROW LLC, a Delaware limited liability company, CAESARS
//		ESCROW CORPORATION, a Delaware corporation (together, the Escrow Issuer or the Issuer, provided that, for purposes of this 
//				Indenture with respect to any class of Notes, including Additional Notes, prior to an Assumption (as defined herein) with
//				respect to such class of Notes, the references to the Issuer in this Indenture refer only to the Escrow Issuers; after 
//				the consummation of such Assumption with respect to such class of Notes, the references to the
//		Issuer refer only to Caesars Entertainment Operating Company, Inc., a Delaware corporation, and not to any of its subsidiaries),
//		CAESARS ENTERTAINMENT CORPORATION, a Delaware corporation (the Parent Guarantor), and U.S. BANK NATIONAL ASSOCIATION, as trustee
//		(the Trustee). ";
//		Pattern patternThisAgreement = Pattern.compile("(?<=T[HIShis]{3} )([A-Z][A-Za-z]+ )+"
//				+ "|INDENTURE datedxxx");
//		System.out.println(nlp.getAllMatchedGroups(text, patternThisAgreement).get(0));

	}

	public static String getLegalEntitiesFromOpOfJsonFile(String json) throws IOException {
		NLP nlp = new NLP();
		Pattern patternOp = Pattern.compile("(?<=\"openingParagraph\" ?: ?\").*?\r\n");
		List<String> list = nlp.getAllMatchedGroups(json, patternOp);
		TreeMap<Integer, String> mapLE = new TreeMap<Integer, String>();
		String origJson = json;
		if (list.size() > 0) {
			mapLE = EntityRecognition.getLegalEntities(list.get(0).replaceAll("zxz", " ").replace("\"", ""), false);
		}
		String parties = "\"PartiesOp\" : [ \"";
		StringBuilder sb = new StringBuilder();
		boolean added = false;
		if (mapLE.size() > 0) {
			sb.append(parties);
			for (Map.Entry<Integer, String> entry : mapLE.entrySet()) {
				if (nlp.getAllIndexEndLocations(entry.getValue(), Pattern.compile("(?ism)\\)|Deed of Trust|Chief|CEO|Officer"))
						.size() > 0 || entry.getValue().length() < 5)
					continue;
				added=true;
				sb.append(entry.getValue().replaceAll("(zxz|<b>|</b>)", "") + "\",\"");
			}
		}
		if(!added) {
			sb = new StringBuilder();
		}

//		NLP.printMapIntStr("parties==", mapLE);
		if (sb.toString().length() == 0) {
			if (nlp.getAllIndexEndLocations(origJson, Pattern.compile("zxz")).size() > 0) {
				origJson = origJson.replaceAll("zxz", "\"\r\n,\"openingParagraph2\":\"^");
			}
			return origJson;
		}
		parties = sb.toString();
		parties = parties.substring(0, parties.length() - 2) + "]\r\n,";
		json = json.replaceAll("(\"openingParagraph\")", parties + "$1");

		if (nlp.getAllIndexEndLocations(json, Pattern.compile("zxz")).size() > 0) {
			json = json.replaceAll("zxz", "\"\r\n,\"openingParagraph2\":\"^");
		}
//		File file2 = new File("c:/temp/t2.txt");
//		PrintWriter pw = new PrintWriter(file2);
//		pw.append(json);
//		pw.close();
		
//		System.out.println("parties=="+parties);

		return json;
	}

	public static void main(String[] args) throws IOException, SQLException, SolrServerException {
		
	
		String json = Utils.readTextFromFile("E:\\getContracts\\solrIngestion\\solrDocs\\2022\\QTR2\\Financial Contracts\\0001575705-22-000444_5 Financial Contracts EX 10.14.json");//Optimize so it only reads first chunk of text - like allhdgs fetcher.
		json = getLegalEntitiesFromOpOfJsonFile(json);

		String text = "^Opinion of Morgan, Lewis & Bockius LLP, as to the validity of the common stock, preferred stock, debt securities, warrants, depositary shares, stock purchase contracts and stock purchase units of FMC Corporation";
		TreeMap<Integer, String> mapLegalEntityLoc = getLegalEntities(text, false);
		NLP.printMapIntStr("done finalmapLegalEntityLoc==", mapLegalEntityLoc);

		// README: if I start this again, I should just scratch by remaking the patterns
		// starting with the way company names can end. for example, to or more types of
		// company and something can occur, such as " Trust Company" or "Trust Company,
		// N.A." Etc. I need to leave by finding the longest instance first. I could
		// automatically do this by putting the patterns into a map by length.

		// README: the next big hurdle are instances where there are multiple legal
		// entities listed out online, usually also problematic are legal entities that
		// are ALL cap letters. Particularly those on a line by them self. Multiple
		// legal entities on the same line are difficult the earlier doesn't acquire
		// exactness (meaning, acquire "Trust Company, N.A/),then you should be okay
		// because it can only match based on the entity suffix to the prior company
		// suffix. With regards to the company on its own line that is a little more
		// difficult. Therefore, I need to determine how best to score names of
		// companies. Based on such scores allow or disallow
	}
}
