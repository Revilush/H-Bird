package contracts;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrServerException;

import xbrl.NLP;
import xbrl.Utils;

public class MetaDataRegex {

	public static Pattern patternEntityTypesAllCaps = Pattern.compile("(?sm)"
			+  "( |^)(CORPORATION|INCORPORATED|NATIONAL ASSOCIATION|LIMITED|BRANCH|STATUTORY TRUST"
			+ "|TRUST( [\\d-]+)|MASTER (NOTE )?TRUST( [\\d-]+)?)(([\\.]{1}|([ ,]{1,2})|(?=[\r\n]{1,2})|\\)|$))"
			+ "|"
			+ "( |^)"
			+ "(INC\\.?|CORP\\.?|CO\\.|LLC|LP|LLP|NA|AG|SA|NV|FSB\\.?|PLC\\.?|LTD\\.?|PC|P\\.C\\.|PLLC)(([\\.]{1}|([ ,]{1,2})"
			+ "|(?=[\r\n]{1,2})|\\)|$))"
			+ "|( |^)(S\\.A\\./N\\.V\\.|L\\.L\\.C\\.|L\\.P\\.|L\\.L\\.P\\.|N\\.A\\.|A\\.G\\.|S\\.A\\.|N\\.V\\.|F\\.S\\.B\\."
			+ ""
			+ "|\\(NCUA\\)|P\\.L\\.C\\.|S\\.A\\.R\\.L\\."
			+ "|TRUST COMPANY OF (THE )?[A-Z]{3,15}|(TRUST )?COMPANY( AMERICAS)?|GROUP(,? INC\\.|CORP\\.|,? INCORPORATED)?"
			+ "|INVESTMENT COMPANY"
			+ "(?!(,? ([ICFPNSA]{1}\\.|National|Limi|Branch|Inc|Corp|LL|LTD|Ltd)))"
			+ ")"
			+ "(([\\.]{1}|([ ,]{1,2})|(?=[\r\n]{1,2})|\\)|$))"
);// Always at end of entity name
	
	public static Pattern patternEntityTypesInitialCaps = Pattern.compile("(?sm)"
			+  "( |^)(Corporation|Incorporated|National Association|Limited|Branch|Statutory Trust"
			+ "|Trust( [\\d-]+)|Master (Note )?Trust( [\\d-]+)?)(([\\.]{1}|([ ,]{1,2})|(?=[\r\n]{1,2})|\\)|$))"
			+ "|"
			+ "( |^)(Inc\\.?|Corp\\.?|Co\\.|llc\\.?|lp|llp\\.?|fsb\\.?|plc\\?|ltd\\.?|Ltd\\.?)(([\\.]{1}|([ ,]{1,2})|(?=[\r\n]{1,2})|\\)|$))"
			+ "|( |^)(l\\.l\\.c\\.|l\\.p\\.|l\\.l\\.p\\.|n\\.a\\.|a\\.g\\.|s\\.a\\.|n\\.v\\.\\|f\\.s\\.b\\."
			+ "|p\\.l\\.c\\.|S\\.a\\.r\\.l\\.|Partners|Company(,? [A-Z]{1}[a-z\\.]{2,4})?"
			+ "|Trust Company [Oof]{2} ([Tthe]{3} )?[A-Z]{3,15}|(Trust )?Company( Americas)?Group(, Inc\\.?|Corp\\.?)?"
			+ "(?!(,? ([FPNSAL]{1}\\.|National|Limi|Branch|Inc|Corp|LL|LTD|Ltd)))"
			+ ")(([\\.]{1}|([ ,]{1,2})|(?=[\r\n]{1,2})|\\)|$))"
);// Always at end of entity name

	public static Pattern patternALLEntityTypes = Pattern
			.compile(
					patternEntityTypesAllCaps.pattern() 
					+ "|" + patternEntityTypesInitialCaps.pattern());

	public static Pattern patternBankOrTrustCo = Pattern.compile(
			"(?sm)(" + "BANK|TRUST COMPANY( AMERICAS)?|TRUST CO\\.?|" + "Bank|Trust Company( Americas)?|Trust Co\\.?" + ")" + "[, \r\n]{1}");
	
	public static Pattern patternAnyEntityName = Pattern.compile("(?sm)"
			+ ""//add stand alones here
			+ "("
			+ "(?ism)"
			+ "([\\r\\n^ ]{1})State Street Bank and Trust Company of California, N\\.A\\.)"
			+ "|"
			+ "([\r\n^ ]{1})" + "("
			+ "[\\)A-Z\\.'\\-\\&\\d]+[\\)\r\n]{0,6}" + "([\\(\\)A-Za-z\\.'\\-\\&\\d/]+)?" + 
			"($|[,\\. \r\n]+)" + "(of |OF |AND )?" +
			")+");

//	public static Pattern patternAnyEntityName = Pattern.compile("( |\\s|\t|\r|\n)The Bank of New York");

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
//		System.out.println("entityTypeIsAtEndOfName text=="+text);
		
		int eIdx = Integer.parseInt(list.get(list.size() - 1)[0]);
//		NLP.printListOfStringArray("", list);

		if (text.length() - eIdx > 2) {
//			System.out.println("excluded = entity type isn't at end=" + text);
			return false;
		}

		return true;
	}
	
	public static int entityIsInString(String text) throws IOException {
		NLP nlp = new NLP();
		List<String[]> list = nlp.getAllEndIdxAndMatchedGroupLocs(text, patternALLEntityTypes);
		if(list.size()==0)
			return 0;
		int eIdx = Integer.parseInt(list.get(list.size() - 1)[0]);
//		NLP.printListOfStringArray("", list);
//		System.out.println("text.len="+text.length()+" eIdx="+eIdx);
//		System.out.println("entity="+text.substring(0,eIdx));
		
			return eIdx;
	}
	
	public static boolean excludeMe(String text) throws IOException {
		NLP nlp = new NLP();

		if (nlp.getAllMatchedGroups(text, Pattern.compile("(?sm)"
				+ "EX-[\\d\\.]{2}|Exhibit |EXHIBIT|Execution|EXECUTION|DATED? |Dated? |Schedule|SCHEDULE|Form |FORM "
				+ "|Guarant|GUARANT"
				+ "|[\\-\\_]{2}"
				+ "|^ (Investment Company( Act)?|Clearing Corporation|Limited Liability Company Agreement)[ \\.]$"
				+ "|Clearing (Agenc[iesy]{1,3}|Corporation)|Federal Reserve Bank"
				+ "|^ (INVESTMENT COMPANY( ACT)?|CLEARING CORPORATION|LIMITED LIABILITY COMPANY AGREEMENT)[ \\.]$"
				+ "|CLEARING (AGENC[IESY]{1,3}|CORPORATION)|FEDERAL RESERVE BANK"
				+ "|The United States Investment Company|THE UNITED STATES INVESTMENT COMPANY"
				+ "|Liability Company|Limited Liability Company|LIABILITY COMPANY|LIMITED LIABILITY COMPANY"
				+ "|Investment Company|INVESTMENT COMPANY|Agent Bank|AGENT BANK|Bank Holding Company|BANK HOLDING COMPANY"
				+ "|Opinion|OPINION|Initial|Purchaser|Issuer|INITIAL|PURCHASER|ISSUER|Organization|ORGANIZATION|Bank\\.|BANK\\."
				+ "|[a-z]{2}\\..{1,6}(Compan|Bank)|[A-Z]{2}\\..{1,6}(COMPAN|BANK)|The Company|THE COMPANY|[\\d]{5,}"
				+ "")).size() > 0

		) {
			return true;
		}

		return false;
		
	}
	
	public static List<String[]> isItTwoEntities(String [] ary) throws IOException {
		
		NLP nlp = new NLP();

		List<String[]> list = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(ary[0], patternALLEntityTypes);
		if(list.size()<2)
			return null;

		int eIdx =0, sIdxNext =0, sIdx=0, sIdxOriginal = Integer.parseInt(ary[1]); String entity ="";
//		NLP.printListOfStringArray("there could be two entities -- list==", list);
		// goes through list check to see if there are 2 entity types. Saves them. Then
		// It checks if there are two entity names.
		List<String[]> list2Entities = new ArrayList<String[]>();
		
		for(int i=0; i<list.size(); i++) {
			eIdx = Integer.parseInt(list.get(i)[2]);

			if(eIdx-sIdx<2) {
//				System.out.println("eIdx-sIdx<2=="+(eIdx-sIdx));
				continue;
			}
			
//			System.out.println(
//					"this is 1 of 2 entity=" + ary[0].substring(sIdx, eIdx) + " sIdx=" + sIdx + " eIdx=" + eIdx);
			
			if(i+1==list.size()) {
				entity = ary[0].substring(sIdx,eIdx);
				if(excludeMe(entity) || entity.toLowerCase().contains("branch"))
					continue;
//				System.out.println("22-1 adding entity="+entity);
				String [] ar = {entity,""+(sIdx+sIdxOriginal),""+(sIdxOriginal+ary[0].length())};
				list2Entities.add(ar);
			}
			if(i+1<list.size()) {
				entity = ary[0].substring(sIdx,eIdx);
				if(excludeMe(entity) || entity.toLowerCase().contains("branch"))
					continue;
//				System.out.println("22-2 adding entity="+entity);
				String [] ar = {entity,""+(sIdx+sIdxOriginal),""+(eIdx+sIdxOriginal)};
				list2Entities.add(ar);
			}
				sIdx=eIdx;
				
			if(i+1<list.size()) {
				sIdxNext = Integer.parseInt(list.get(i+1)[1]);
				if(sIdxNext-eIdx<3) {
//					System.out.println("eIdx="+eIdx+" sIdxNext="+sIdxNext+" RETURN NULL");
					return null;
				}
			}
		}
		
//		System.out.println("original ary==" + Arrays.toString(ary));
//		NLP.printListOfStringArray("list2Entities==", list2Entities);
		
		return list2Entities;
		
	}

	
	public static List<String[]> getEntities(String text) throws IOException {

		NLP nlp = new NLP();
		boolean found = false;

		int splitCommaLen = 0;
		String entity = "", sIdxStr = "", eIdxStr = "";
		List<String[]> listEntities = new ArrayList<String[]>();

		List<String[]> list = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(text, patternAnyEntityName);
//		System.out.println("this many possible entities=="+list.size());
//		NLP.printListOfStringArray("", list);
		for (int i = 0; i < list.size(); i++) {
			splitCommaLen=0;
			found = false;
			entity = list.get(i)[0];
			sIdxStr = list.get(i)[1];
			eIdxStr = list.get(i)[2];
//			System.out.println("initial go at an entity name="+entity);
			//if "as InitCap InitCap", Entity Name -- then drop 
			
			if(isAllCapsText(sIdxStr,eIdxStr,entity,text)) {
//				System.out.println("isAllCapsText="+isAllCapsText(sIdxStr,eIdxStr,entity,text));
				continue;
			}
			
			List<String[]> listTwoMoreEntities = isItTwoEntities(list.get(i));
			if(null!=listTwoMoreEntities && listTwoMoreEntities.size()>1) {
				for(int n=0; n<listTwoMoreEntities.size(); n++) {
//					System.out.println("adding two or more entities=="+Arrays.toString(listTwoMoreEntities.get(n)));
					listEntities.add(listTwoMoreEntities.get(n));
				}
				continue;
			}
			
			if (excludeMe(entity.split(",")[0]) && entity.length() - entity.split(",")[0].length() > 8
					&& nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(entity.split(",")[1], patternAnyEntityName)
							.size() > 0) {
				sIdxStr = (Integer.parseInt(sIdxStr) + entity.split(",")[0].length()) + "";
//				System.out.println("text.sub entity="+text.substring(Integer.parseInt(sIdxStr),Integer.parseInt(eIdxStr)));
				entity = entity.substring(entity.split(",")[0].length(), entity.length());
				// picks-up where there is an entity
//				Initially, The Bank of New York Mellon Trust Company, N.A. will <---grabs as The Bank of New York Mellon Trust Company, N.A. 
			}

			String[] ary = {entity, sIdxStr + "", (Integer.parseInt(eIdxStr)) + "" };
			int noOfWords = entity.trim().split(" ").length;
//			System.out.println("entity="+entity);
//			System.out.println("itIsnEntity=" + itIsAnEntity(entity));
//			System.out.println("entityTypeIsAtEndOfName=" + entityTypeIsAtEndOfName(entity));
//			System.out.println("nlp.getAllMatchedGroups(entity, patternALLEntityTypes).size()=="
//					+ nlp.getAllMatchedGroups(entity, patternALLEntityTypes).size());

			// don't add #s b/c of street address
			if(excludeMe(entity)) {
//				System.out.println("entity=|"+entity+"| exclude me="+excludeMe(entity));
				continue;
			}

			if (nlp.getAllMatchedGroups(entity, patternALLEntityTypes).size() > 0) {
				if ((itIsAnEntity(entity) && entityTypeIsAtEndOfName(entity))
						//&& nlp.getAllMatchedGroups(entity, patternBankOrTrustCo).size() == 0
						) {
//					System.out.println("1. is entity-x-"+entity+" entity.split comma.len="+entity.split(", ").length);
					
					splitCommaLen=entity.split(", ").length;

					if(splitCommaLen==3) {
						//fixes===>as Trustee, Deutsche Bank AG, London Branch
//						System.out.println("entity split=3. entity.split[0]="+entity.split(", ")[0]);
						sIdxStr = ""+(Integer.parseInt(sIdxStr)+entity.split(", ")[0].length()+1);
						entity = text.substring(Integer.parseInt(sIdxStr),Integer.parseInt(eIdxStr));
//						System.out.println("2 split,  entity==="+entity+"|");
						noOfWords = entity.trim().split(" ").length;
						if (noOfWords > 1) {
							String[] ary2 = { entity, sIdxStr, (Integer.parseInt(eIdxStr))+"" };
//							System.out.println("1 adding entity="+entity);
							listEntities.add(ary2);
							found = true;
						}
					}
					
					if (!found && splitCommaLen < 3) {
						noOfWords = entity.trim().split(" ").length;
						if (noOfWords > 1) {
//							System.out.println("2 adding entity="+entity);
							listEntities.add(ary);
							found = true;
						}
					}
				}
			}

			if (!found && nlp.getAllMatchedGroups(entity, patternBankOrTrustCo).size() > 0
					&& nlp.getAllMatchedGroups(entity, patternALLEntityTypes).size() == 0) {
				
				if (itIsAnEntity(entity) && entity.trim().split(" ").length > 1) {
//					System.out.println("2. entity.len=" + entity.length() + "\r\nbank/trust co = i=" + i + " DONE="
//							+ entity.trim() + "| found==" + found + "entity.trim().split(\" \").len="
//							+ entity.trim().split(" ").length);

					noOfWords = entity.trim().split(" ").length;
					if (noOfWords > 1) {
//						System.out.println("3 adding entity=" + entity);
						listEntities.add(ary);
						found = true;
					}
				}
			}

			
			if (!found && entity.split(",").length>1 && nlp.getAllMatchedGroups(entity, patternBankOrTrustCo).size() > 0
					&& nlp.getAllMatchedGroups(entity.split(",")[1], patternALLEntityTypes).size() == 0
					&& nlp.getAllMatchedGroups(entity, patternALLEntityTypes).size() > 0
					) {
				String e = entity.split(",")[1];
//				System.out.println("??");
				//fixes:  Company, The Bank of New York 
				if (itIsAnEntity(entity) && e.trim().split(" ").length > 1) {
//					System.out.println("3. entity.len=" + e.length() + "\r\nbank/trust co = i=" + i + " DONE="
//							+ e.trim() + "| found==" + found + "entity.trim().split(\" \").len="
//							+ e.trim().split(" ").length);

					noOfWords = entity.trim().split(" ").length;
					if (noOfWords > 1 
							) {
						String[] ary2 = { e, ""+(entity.split(",")[0].length()+Integer.parseInt(sIdxStr)), (Integer.parseInt(eIdxStr))+"" };
//						System.out.println("4 adding entity=" + entity);
						listEntities.add(ary2);
						found = true;
					}
				}
			}

			
			if (!found && nlp.getAllEndIdxAndMatchedGroupLocs(entity, patternALLEntityTypes).size()>0 &&
					!entityTypeIsAtEndOfName(entity) 
					&& itIsAnEntity(entity)) {
//				System.out.println("not found and entity name isn't at end==" + entity);
				//fixes: Citibank, N.A., 480 Washington
				entity = entity.substring(0,nlp.getAllIndexEndLocations(entity, patternALLEntityTypes).get(0));
				
				noOfWords = entity.trim().split(" ").length;
				if (noOfWords > 1) {
//					System.out.println("3 entity wasn't at end - fixed===" + entity + "|");
					String[] ary2 = { entity, sIdxStr, "" + (Integer.parseInt(sIdxStr) + entity.length())+""
							 };
//					System.out.println("5 adding entity="+entity);
					listEntities.add(ary2);
					found = true;
				}
			}
		}

//		NLP.printListOfStringArray("listEntities====", listEntities);

		return listEntities;
	}
	
	public static boolean isAllCapsText(String sIdxStr,String eIdxStr, String entity, String text) throws IOException {
		
//		NLP nlp = new NLP();
		int sIdx = Integer.parseInt(sIdxStr), eIdx = Integer.parseInt(eIdxStr);
//		String s=text.substring(Math.max(0, sIdx-250),Math.min(eIdx+250, text.length()));
		//get last index of \r\n before sIdx
		//get first index of \r\n after eIdx
		//use those two idx locations to cut text.
		//check if all characters are all caps
		
		String tmpStr =text.substring(0,sIdx);
		int sIdxHardReturn = tmpStr.lastIndexOf("\r\n");
		if(sIdxHardReturn<0) {
			
		}
		tmpStr = text.substring(eIdx,text.length());
		int eIdxHardReturn = tmpStr.indexOf("\r\n");
		
//		System.out.println("sIdx="+sIdx+" sIdxHardReturn=" + sIdxHardReturn + " eIdx=" + eIdx + " eIdxHardReturn=" + eIdxHardReturn);
		if(sIdxHardReturn<0) {
			sIdxHardReturn=0;
		}
		tmpStr = text.substring(sIdxHardReturn,(eIdx+eIdxHardReturn));
		if(tmpStr.length()==tmpStr.replaceAll("[a-z]+", "").length()) {
//			System.out.println("line is all caps");
			return true;
		}
//		System.out.println("hard return to hard return=="+tmpStr+"||");
		return false;
	}

	public static String boldEntities(List<String[]> list, String text) throws IOException {

		//		NLP nlp = new NLP();

		String entity = "",sIdxStr = "", eIdxStr = "", str="",strPrior ="", gap ="";
		int eIdx =0, sIdx =0, eIdxPrior=0;
		for(int i=0; i<list.size(); i++) {
			
			entity = list.get(i)[0];
			sIdxStr = list.get(i)[1];
			eIdxStr = list.get(i)[2];
//			System.out.println("entity to bold="+entity);
			sIdx = Integer.parseInt(sIdxStr);//start of entity
			eIdx = Integer.parseInt(eIdxStr);//end of entity
			
			gap = text.substring(eIdxPrior,sIdx);//gap
//			System.out.println("eIdxPrior="+eIdxPrior+" sIdx="+sIdx);
//			System.out.println("1 str="+str);
			entity = "<b>"+entity+"</b>";
			entity=entity
					.replaceAll(", </b>", "</b>, ")
					.replaceAll("<b>\\), ", "\\), <b>")
					.replaceAll("\\)</b>","</b>\\)")
					.replaceAll("<b> ", " <b>")
					;
			str = strPrior + gap+entity;
					;
//			System.out.println("2 str="+str);
			
			strPrior = str;
			eIdxPrior = eIdx;

		}
		
		str = str+text.substring(eIdx,text.length());
//		System.out.println("3 str="+str.trim());
		
		str = str.replaceAll("XOP", "\\(").replaceAll("xcp", "\\)")
				.replaceAll("(?sm)(</b>)(, N\\.A\\.)", "$2$1").replaceAll("(<b>)(, )", "$2$1")
//				.replaceAll("(<b>)( ?[A-Za-z]{2,15})( [A-Za-z]{2,15}, )","$2$3$1")//<b>Owner Trustee,
				//can't do this b/c then Continental Airlines, Inc.
				.replaceAll("(<b>)( ?[A-Za-z]{2,15}, )(?!(?i)(INC\\.?|CORP\\.?|CO\\.|LLC|LP|LLP|NA|AG|SA|NV|FSB\\.?|PLC|LTD|PC))","$2$1")//<b>Depositor,
				.replaceAll("(</b>)(,? ?((L\\.?)?L\\.?P|I[NCnc]{2}|L\\.?L\\.?C)\\.?)","$2$1")//</b>, INC.
//				<b>CONWOOD COMPANY </b>L.P
				.replaceAll("(</b>)(, NATIONAL ASSOCIATION)","$2$1")//group 2 can't be preceded by a ','
				.replaceAll("([A-Z]{2,15}, )(<b>)(N\\.A\\.|INC\\.|Inc\\.)(</b>)","$2$1$3$4")//SEECO, <b>Inc.</b>
				;
		
//		System.out.println("4 str="+str.trim());

		
		return str;
	}
	

	public static void main(String[] args) throws IOException, SQLException, SolrServerException {
//		NLP nlp = new NLP();
		
//		as <b>Owner Trustee, Wilmington Trust Company</b>  <----create rule xxxx
		
		//TODO: capture legal entities in xml (longest entity name first)
		//TODO: mark legal entities in rendering - on the fly or from xml?
		
		String text = Utils.readTextFromFile("d:/getContracts/temp/openingParas.txt");
//		text ="ormerly known as The Bank of New York as hello";
		System.out.println("1 text.len"+text.length());
//		formerly known as The Bank of New York as hello
		text = text.replaceAll("(f.{0,1}k.{0,1}a.{0,1}|a.{0,1}k.{0,1}a.{0,1}|known)( )(as)", "$1xxxx$3 ")
				.replaceAll("(?ism)(?<= as) (?=[A-Z]{1}[A-Za-z]{1,15})", "xxxx");
//		System.out.println(text);
		List<String[]> listEntities = getEntities(text);
//		System.out.println("2 text.len="+text.length());
		text = boldEntities(listEntities, text);
		text = text
				.replaceAll("\r\n", "\r\n\r\n<br><br>").replaceAll("xxxx", " ")
		.replaceAll("( COMPANY )(</b>)(OF [A-Z]{2,15})", "$1$3$2")
		.replaceAll("(a )(<b>)([A-Z]{1}[a-z]{2,15} )(Corporation)(</b>)", "$1$3$4	");
		
//		a <b>Delaware Corporation</b>
//		CONWOOD COMPANY L.P.<---bold isn't picking up L.P.
		File fn = new File("d:/getContracts/temp/openingParas.html");
		Utils.writeTextToFile(fn, text);

//		iHeartCommunications, Inc. see also: PacifiCare eHoldings, Inc.,
		
	}
}
	