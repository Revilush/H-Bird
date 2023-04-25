package xbrl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import contracts.GetContracts;

public class ContractParser {

	// MAP: key will be start idx location in file. String[] has
	// [0]=end idx, [1]=contract the text falls under-dynamic field - e.g.:
	// contract_Name_definedTerm_, contract_section_, contract_exhibit_ and
	// 2=text.

	public static String id = "id", contract = "contract", contractLongName = "contractLongName",
			tableOfContents = "tableOfContents", fieldNameOpen = "<field name=\"", fieldNameClose = "\">",
			fieldEnd = "</field>", fieldTextOpen = "<![CDATA[", fieldTextClose = "]]></field>";

	public static TreeMap<Integer, String[]> mapOfContractIdxLocs = new TreeMap<Integer, String[]>();
	public static TreeMap<Integer, String[]> mapOfSectionHeadingsFromToc = new TreeMap<Integer, String[]>();

	public static String contractsFolder_Raw = "c:/getContracts/unStrippedKs/";
	public static String contractsFolder_Parsed = "c:/getContracts/strippedKs/";

	public static String type = null, toc = "", tocHeading = null;
	public String acc = null;
	public static Integer tocStartIdx = null;
	public static Integer tocEndIdx = null;
	public static Integer exhStartIdx = null;
	public static Integer exhEndIdx = null;
	public static List<String[]> listOfExhibitsIdxsAndNames = new ArrayList<>();

	public static Pattern patternPAGE_PRE_S_C = Pattern.compile("<PAGE>|<PRE>|<S>|<C>");

	public static Pattern patternExhibitInContract = Pattern
			.compile("(?<=[\r\n]{1}[ \t]{0,110})(Exhibit|EXHIBIT|APPENDIX|Appendix|SCHEDULE|Schedule|Annex|ANNEX)"
					+ "[ ]{1,3}[A-Z\\-\\d\\(\\)]{1,5}( to)? ?[\r\n]{1}");

	public static Pattern patternArticle = Pattern
			.compile("(?s)(?<=([\r\n]{1}[ ]{1,100})(ARTICLE|Article)[ \r\n\t]{1,3}[iIvVxX\\d]{1,5}"
					+ ".{1,200}?(?=Section|SECTION[ \r\n\t]{1,4}[iIvVxX\\d\\.]{1,7}))");

	public static Pattern patternTocPageNumber = Pattern
			.compile("[\r\n]{1}[\t ]{0,10}[\\p{Alnum}\\p{Punct}].{1,170}[\\d]+ ?[\r\n]");

	public static Pattern patternExhibitToc = Pattern
			.compile("(?<=[\r\n]{1}[ \t]{0,4})(Exhibit|EXHIBIT|APPENDIX|Appendix|SCHEDULE|Schedule|Annex|ANNEX)"
					+ "[ ]{1,2}[A-Z\\-\\d]{1,4}[ \t]{1,20}" + "[\\p{Alnum}\\p{Punct}]{1,90}.{1,90}?[\r\n]{1}");

	public static Pattern patternContractToc = Pattern
			.compile("(?s)(?<=[\r\n]{1,2}[\t ]{0,30})(Section|Subsection|SUBSECTION|SubSection|SECTION|Article|ARTICLE)"
					+ "[ \t]{1,5}[iIvVxX\\d\\.]{1,6}[A-Za-z ;\\.]{3,100}(?=([\r\n]{1,2}A-Za-z ;\\.])?)"
					+ "(?=[\r\n]{1,2}[\t ]{0,30}(Section|Subsection|SUBSECTION|SubSection|SECTION|Article|ARTICLE))");

	public static Pattern patternContractTocWithExhibits = Pattern.compile(patternContractToc.pattern() + "|"
	// + patternTocPageNumber.pattern() + "|"
			+ patternExhibitToc.pattern() + "|" + patternArticle.pattern());

	public static Pattern alphabetPattern = Pattern.compile("[A-Za-z]{5,}");

	public static Pattern nonAlphaPattern = Pattern.compile("[^A-Za-z]");

	public static Pattern patternPageNumber = Pattern
			.compile("(?ims)([\r\n]{1}[\t ]{0,100}(page)?[\\dixv\\-]{1,6}[\t ]{0,100}[\r\n]{1})");

	public static Pattern patternClosingPara = Pattern
			.compile("(?sm)(WITNESS WHEREOF.{1,90}(executed|set.{1,4}their.{1,4}hand).{1,150}\\.)");

	public static Pattern spaceNumberspacePattern = Pattern.compile("(?<!([A-Za-z])) [-]{0,}[\\d,]{1,}");

	public Pattern patternParentChildParas = Pattern
			.compile("([:;]|[,;] (and|or)) ?\r ?([A-Z]{1}|[\\d\\.]{2}|\\([a-z\\dA-Z]\\))");

	public static Pattern patternDescription = Pattern.compile("(?is)(DESCRIPTION>.+?\\<)");

	// public static Pattern patternDefinedWordInBodyOfText = Pattern
	// .compile("(?<![\r\n]{1}[\\s ])\"[A-Za-z\\s \\d]+{1,60}\"");

	/*
	 * pattern will find a defined term immediately folloewd by a colon ':' and
	 * preceded by a hard return and either two or more ws or tab and where the
	 * defined term has no lower case word more than 3 characters
	 */

	public static Pattern patternDefinedWordsNoQuotes = Pattern.compile("(?<=[\r\n]{3,6}([ ]{2,20}|[\t]{1,10}))"
			+ "(?!Re:|RE:|Subject:|W ?I ?T ?N ?E ?S ?S ? ??E? ?T? ?H? ?  ?T ?H ?A ?T ?:|SUBJECT:|Section|SECTION|Article|ARTICLE)[A-Z]{1}[A-Za-z]{2}.{3,150}"
			+ "(?<!( [a-z]{3,20} ?.{3,150}))" + "(: |\\. |- )" + "(?=([\r\n]{1,2})?([ \t]{1,30})?[A-Za-z\\d]{1})");

	public static Pattern patternDefinedWordsFromDefSection = Pattern
			.compile("(?s)(?<=[\r\n]{1}[\\s \t]{0,20})\"[\\p{Alnum}\\p{Punct}].{1,50}\"(?=.*([\r\n]( \t\\s)?))");

	public static Pattern patternDefinedWordInDefSec = Pattern.compile(
			"(?<=(?<=[\r\n]{1}[\\s \t]{0,20}))\"[\\dA-Z]{1,5}[A-Za-z ,;:'\\.\\&\\-\\)\\(\\[\\]\\$\\%\\*]{1,70}\""
					+ "(?=([\r\n]{1,2})?([ \t]{1,30})?[A-Za-z\\d]{1})");

	public static Pattern patternDefinedWordsInDefinitionArticle = Pattern
			.compile(patternDefinedWordInDefSec.pattern() + "|" + patternDefinedWordsNoQuotes.pattern());

	public static Pattern patternSection = Pattern
			.compile("(Qx)?(Section|Subsection|SUBSECTION|SubSection|SECTION)(XQ)?[ \r\n\t]{1,3}[iIvVxX\\d\\.]{1,6}");

	// NOTE: THESE TWO PATTERNS - HERE TO END - MUST HAVE SAME EXACT START IDX
	// IF THEY BOTH FIND SAME PATTERN!!!

	public static Pattern patternSectionHeadingRestrictive = Pattern
			.compile("(?s)(((?<=([\r\n]{1})([\t]{1,4}|[\\s]{1,15})?))"
					+ "(Qx)?(Section|Subsection|SUBSECTION|SubSection|SECTION)(XQ)?[ \r\n\t]{1,3}[iIvVxX\\d\\.-]{1,6}(?![\r]{2,}) "
					// +" )"
					+ "[\"\\|\\-\t\\(\\)\\d\\&\\$\\%A-Za-z ;,\\/\\[\\]\\{\\}':]{4,375}(\\.|[\r\n]{2}))"
					+ "|((Qx)?(Section|SECTION)(XQ)?.{1,5}[\\d\\.]{1,6}.{1,4}(Qx)?Taxes.{1,5}(XQ)?[\r\n\\.])");

	public static Pattern patternSectionHeadingLessRestrictiveWithoutNumber = Pattern.compile("(?sm)" + "(("
			+ "(?<=([\r\n]{1})([\t]{1,4}|[\\s]{1,35})?)" + ")" + "(Qx)?(Section|Subsection|SUBSECTION|SubSection|SECTION)(XQ)?"
			+ "[ \r\n\t]{1,3}[iIvVxX\\d\\.-]{1,6}([\r\n]{0,11})"
			+ "[\"\\|\\-\t\\(\\)\\d\\&\\$\\%A-Za-z ;,/\\[\\]\\{\\}':]"
//					+ "."
			+ "{4,255}(\\.|[\r\n]{2}))" +

			"|" + "(" + "(Qx)?(Section|SECTION)(XQ)?.{1,5}[\\d\\.]{1,6}.{1,4}Taxes.{1,5}(XQ)?[\r\n\\.]" + ")" + "");

	public static Pattern patternSectionHeadingNoSectionMarker = Pattern
			.compile("(?s)(((?<=([\r\n]{1})([\t]{1,4}|[\\s]{1,15})?))" + "(?![12]{1}[09]{1}[\\d]{1})"
					+ "(Qx)?[\\d]{1,3}\\.?[\\d]{1,4}(?![\r]{2,})?![\\%,\\d] "
					// +" )"
					+ "[\"\\|\\-\t\\(\\)\\d\\&\\$\\%A-Za-z ;,\\/\\[\\]\\{\\}':]{5,375}(\\.|[\r\n]{2}))"
					+ "|(Qx)?([\\d]{1,3}\\.?[\\d]{1,3}.{1,5}[\\d\\.]{1,6}.{1,4}Taxes.{1,5}(XQ)?[\r\n\\.])");

	public static Pattern patternSectionHeadingLessRestrictiveWithNumberSectionAlso = Pattern.compile(
			patternSectionHeadingLessRestrictiveWithoutNumber + "|" + patternSectionHeadingNoSectionMarker.pattern());

	// some Section headings can be 350 in length - see:
	// The Collection Account, the Lower-Tier REMIC Distribution Account, the
	// Upper-Tier REMIC Distribution Account, the Companion Distribution
	// Account, the Interest Reserve Account, the Excess Interest Distribution
	// Account, the Gain-on-Sale Reserve Account, the Class [PEX] Distribution
	// Account and the [LOAN-SPECIFIC] REMIC Distribution Account
	// END

	public static Pattern patternSectionHeadingWithClosingParagraph = Pattern.compile(patternClosingPara.pattern());

	public static Pattern patternContractSentenceStart = Pattern
			.compile("(?<=((\\.|\\?|\\!)(\r\n|[ ])))[A-Z\\(]{1}|(?<=[\r\n]{1}[ \\s])[A-Z\\(]{1}");

	// start of sentence -- hard return followed by A-Z \\(
	// [\r\n]{1}[ \\s][A-Z\\(]{1}
	// start of sentence is end of space period.
	// (?<=(\\.(\r\n|[ ])))[A-Z\\(]{1}

	public static Pattern patternType = Pattern.compile("(?i)(TYPE>.{1,100}?\\<)", Pattern.DOTALL);

	public static String checkText(String text, String priorText) {

		// System.out.println("priorText.len="+priorText.length());
		// System.out.println("text.len="+text.length());

		if (text.length() * 95 > 100 * priorText.length())
			return priorText;
		else
			return text;

	}

	public static String stripHtmlTags(String text) throws IOException {
		

		// DO NOT MAKE CHANGES AFTER NOTE IN BODY OF METHOD THAT SAYS ==>>
		// *****NOTE**** KEEP THESE 2 =>text.replaceAll("(?sm)<[^>]*>", ""); -- BUT FIX
		// ERRORS PRIOR TO HERE!!!

		NLP nlp = new NLP();
		// System.out.println("1 text length="+text.length());
//		PrintWriter pw = new PrintWriter(new File("c:/getContracts/temp/stripHtmlTagsAaB1.txt"));
//		pw.append(text);
//		pw.close();

		text = text.replaceAll("([\\.\\.]{2})+", " ").replaceAll(" \\.", " ");
		// ..... in txt causes the xml parser to repeat sentences in the xml (same
		// sentences are recorded multiple times. This happens with table of contents
		// where format is often like:
		// Section 2.2 Form of Indenture Trustee’s Certificate of
		// Authentication...........3

		text = text.replaceAll("(?ism)<PRE>", "");
//		pw = new PrintWriter(new File("c:/getContracts/temp/stripHtmlTagsAaB2.txt"));
//		pw.append(text);
//		pw.close();

		text = text.replaceAll("(?i)[ ]{0,}<p[^>]*>", "<p>");

//		pw = new PrintWriter(new File("c:/getContracts/temp/stripHtmlTagsAaB3.txt"));
//		pw.append(text);
//		pw.close();

		text = text.replaceAll("&#145;|&#146;|‘|’|&rsquo;|&#8217;|&#x2019;|&#8216;", "'");
		text = text.replaceAll("”|“|&#147;|&#148;|&#8221;|&#8220;|&ldquo;|&rdquo;|&quot;|&#x201C;|&#x201D", "\"");
		text = text.replaceAll("&#36", "\\$");

		text = text.replaceAll("(?i)(?<=[a-zA-Z\\d]{1})<BR>(?=[a-zA-Z\\d]{1})", " <BR>");

//			pw = new PrintWriter(new File("c:/getContracts/temp/stripHtmlTagsA64.txt"));
//			pw.append(text);
//			pw.close();

		text = text.replaceAll("</h\\d>", "\r\n");
		// can't replace three hard returns with 1, so 2-->

		text = text.replaceAll("(?i)<BR> ?\r\n ?<BR>", "\r\n\r\n");
		text = ContractNLP.numberBR.matcher(text).replaceAll("\r\n");
		// System.out.println("3 text.repl len="+text.length());
		text = text
				// .replaceAll("(?i)(<BR>\r\n|\r\n<BR>)", "\r\n")
				.replaceAll("(?i)<br />", "\r\n");
		text = text.replaceAll("(?i)<BR>", "\r\n ");

		text = text.replaceAll("(?ism)<h\\d[^>]*>", "\r\n");
		text = text.replaceAll("(?i)</h\\d>", "\r\n");

		// end requires hard return

		// *****NOTE**** KEEP THESE 2 =>text.replaceAll("(?sm)<[^>]*>", ""); --
//			FIX ERRORS PRIOR TO HERE!!!
		text = text.replaceAll("(?ism)</div>|</p>", "\r\n");

		text = text.replaceAll("(?sm)<[^>]*>", "");
		text = text.replaceAll("(?sm)</[^>]*>", "\r\n");

		text = text.replaceAll(ContractNLP.TRPattern.toString(), ContractNLP.LineSeparator);
//			pw = new PrintWriter(new File("c:/getContracts/temp/stripHtmlTagsA8.txt"));
//			pw.append(text);
//			pw.close();

//			System.out.println("2 text.repl len="+text.length());

		text = text.replaceAll("&#9;|&nbsp;|\\xA0|&#160;|&#xA0;|&#168;|&#173;|&#32;|&#8194;", " ");

//			System.out.println("3 text.repl len="+text.length());
		text = text.replaceAll("&#151;|&mdash|&#95;|&#9744;", "_");
//			System.out.println("4 text.repl len="+text.length());
		text = text.replaceAll("&#8211;|&#9679;|&#150;|&#8212;|&#8209;|&#111;|&ndash;|&#x2022;|&middot;", "-");

//			System.out.println("5 text.repl len="+text.length());
//			System.out.println("6 text.repl len="+text.length());
		text = text.replaceAll("&amp;", "&").replaceAll("&#091;", "\\[").replaceAll("&#093;", "\\]");

//			System.out.println("7 text.repl len="+text.length());
		text = text.replaceAll("&#169;", "xA9");
//			System.out.println("8 text.repl len="+text.length());

		text = text.replaceAll("â€”", "-").replaceAll("DESCRIPTION>|&#133;|&#9;", "");

		// XXXXXXXXXX====>CAUSES ERROR IN STRIP
		text = text.replaceAll("&sect;|&#167;", "§");
		// XXXXXXXXXX

		text = text.replaceAll("&#184;", ",");
//			System.out.println("6 text.repl len="+text.length());
		text = text.replaceAll(ContractNLP.TDWithColspanPattern.toString(), "\t\t");
//			System.out.println("7 text.repl len="+text.length());
		text = text.replaceAll(ContractNLP.TDPattern.toString(), "\t");

//			System.out.println("4 text.repl len="+text.length());
		text = text.replaceAll("(?<=[\\p{Alnum};,\":\\-&]) <", "&nbsp;<");
		// need placeholder otherwise all blanks are removed between dummy
		// extraHtmlS eg HELLO<FONT> </FONT>WORLD. [this will hold the space
		// between the carrots
//			text = text.replaceAll("&nbsp;||&#x25CF;", " ");// replace 2 or 3 w/ 1
//			System.out.println("1 text.repl len="+text.length());
		text = text.replaceAll("(?is)<!--.*?-->", "");
		text = text.replaceAll("[\r\n]{1,}\\.", "\\.\r\n");
//			System.out.println("3 text.repl len="+text.length());
		text = text.replaceAll("([\r\n]{1,2})([ ]+)([\r\n]{1,2})", "$1$3");

//			pw = new PrintWriter(new File("c:/getContracts/temp/stripHtmlTagsA12.txt"));
//			pw.append(text);
//			pw.close();

		text = text.replaceAll("&#187;[ \r\n\t]{0,15}\\.", "\\.");
//			System.out.println("2a text.repl len="+text.length());
		text = text.replaceAll("([ ]+)(\r\n)", "$2");
//			System.out.println("2b text.repl len="+text.length());
		text = text.replaceAll("([a-zA-Z]{1})(\\.)([a-zA-Z]{1})(\\.)([a-zA-Z]{1})(\\.)", "$1xxPD$3xxPD$5xxPD");

//			pw = new PrintWriter(new File("c:/getContracts/temp/stripHtmlTagsA12a.txt"));
//			pw.append(text);
//			pw.close();

//			System.out.println("2 text.repl len="+text.length());
		text = text.replaceAll("([a-zA-Z]{1})(\\.)([a-zA-Z]{1})(\\.)", "$1xxPD$3xxPD");
		text = text.replaceAll(" (?=(INC|CO|Inc|Co))\\.(?=[, ]{1})", "xxPD");
		text = text.replaceAll("(?<= (INC|CO|Inc|Co))\\.(?=[, ]{1})", "xxPD");
		text = text.replaceAll("\r\n ?\r\n[ \t]{0,10}[\\d\\.]{4,20}[ \t]{0,10}\r\n ?\r\n", "");

		// text = text.replaceAll("U\\.S\\.A\\.", "USA");
		// text = text.replaceAll("U\\.S\\.", "US");
		// text = text.replaceAll("(N\\.A\\.)", "NA");

		// replace words that are in quotes that are in body of text (not in
		// definitions section). Such as:... in Section 1381(a)(2)(C) of the
		// Code, (v) an "electing large partnership," as defined

		// first word can be caps but second word must be lower caps. next is what is in
		// quotes.

//			pw = new PrintWriter(new File("c:/getContracts/temp/stripHtmlTagsA12b.txt"));
//			pw.append(text);
//			pw.close();

		text = text.replaceAll("(" +
		// 1st word
				" [\\)\\(\\[\\]A-Za-z,;]{1,11}" +
				// 2nd word
				" [\\)\\(\\[\\]a-z,;]{1,11} )" + "(\")" + "([\\[\\]_-a-zA-Z;, ]{1,50})" + "(\")", "$1''$3''");
		// are rated at least "[__]" by [RA2] and "[__]" fr
		// not followed by two spaces or 1 space and an initial caps.

//			pw = new PrintWriter(new File("c:/getContracts/temp/stripHtmlTagsA13.txt"));
//			pw.append(text);
//			pw.close();

//			System.out.println("2g text.repl len="+text.length());
		text = text.replaceAll("(?i)(etc)\\.(?! [A-Z]{1}[a-z]{1,} [a-z]{1}|  [A-Z]| ?[\r\n]| ? \\([a-z]{1}\\))", "$1");

//			pw = new PrintWriter(new File("c:/getContracts/temp/stripHtmlTagsA13b.txt"));
//			pw.append(text);
//			pw.close();

		text = text.replaceAll("(?<=[a-z]{1}) \\.  ?(?=[A-Z]{1}1)", "\\. ");

//			pw = new PrintWriter(new File("c:/getContracts/temp/stripHtmlTagsA13c.txt"));
//			pw.append(text);
//			pw.close();

		text = text.replaceAll("&#043;", "+");
		text = text.replaceAll("&#038;", "&");
		text = text.replaceAll("&#[\\d]{1,5};", "");

		text = text.replaceAll(
				"(\r\n ?)(SECTION |Section )?([\\d]{1,3}\\.[\\d]{1,2})(\r\n ?)([A-Z]{1,14}[a-z]{0,14} )+?",
				"$1$2$3 $5");
		text = text.replaceAll("(\r\n ?)([\\d]{1,3}\\.[\\d]{1,2})(\r\n ?)([A-Z].{5,100}?\r\n\r\n)", "$1$2 $4");
		text = text.replaceAll("[\r\n]{4,}", "\r\n\r\n\r\n");
//		text = text.replaceAll("\r\n ", "\r\n");

//			pw = new PrintWriter(new File("c:/getContracts/temp/stripHtmlTagsA14.txt"));
//			pw.append(text);
//			pw.close();

//			System.out.println("tocText.len="+tocText.length());

		return text;

//		} catch (Throwable t) {
//			t.printStackTrace(System.out);
//		}

//		return text;
	}

	public static String keepCellsInSameRow(String html, Pattern startPattern, Pattern endPattern, String pType)
			throws FileNotFoundException {

		// this simply removes all hard returns within start and end pattern

		ContractNLP nlpK = new ContractNLP();
		NLP nlp = new NLP();

		StringBuffer sb = new StringBuffer();
		int start = 0, htmlLen = html.length();
		List<Integer> idxStartTrs = nlpK.getAllIndexStartLocations(html, startPattern);
		List<Integer> idxEndTrs = nlpK.getAllIndexStartLocations(html, endPattern);
		if (idxStartTrs.size() == 0 || idxEndTrs.size() == 0) {
			// //NLP.pwNLP.append(NLP.println("no pattern found..");
			return html;
		}
		int endTrI = 0, endTrLoc = 0;
		for (Integer idxStartTr : idxStartTrs) {
			if (start > idxStartTr)
				continue;
			sb.append(new String("\r\n" + html.substring(start, idxStartTr)
//			.replaceAll("(?i)</?[pd]>", "")<===doesn't work. Corrupts other stuff.
			));
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
							.replaceAll("[\r\n]{1,}", "").replaceAll(startPattern.toString(), " ")
							.replaceAll(endPattern.toString(), "").replaceAll("[ ]+", " ");
					// If I replace [\\s]{2,} w/ "" it causes errors as well
					// if (startPattern.equals("startTd") ||
					// startPattern.equals("startTh")) {
					// htmlTemp =
					// ContractNLP.htmlPara.matcher(htmlTemp).replaceAll(" ");
					// if <td > <p>hello</p>world</td> it removes the <p
					// (same for <div and <br)
					// }
					if (pType.toLowerCase().equals("div"))
						htmlTemp = htmlTemp.replaceAll("(?i)(</?p>)", "$1\r\n");

					sb.append(new String(htmlTemp) + "\r\n");
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

	/*
	 * public void parseAllFiles(String folderPath) throws IOException, SQLException
	 * { File folder = new File(folderPath); File[] files = folder.listFiles(); for
	 * (File f : files) parseContract(f); }
	 */
	/*
	 * public void parseContract(File docFile) throws IOException, SQLException {
	 * 
	 * File filename = new File("c:/getContracts/nlpContract.txt"); if
	 * (filename.exists()) filename.delete();
	 * 
	 * NLP.pwNLP = new PrintWriter(filename);
	 * 
	 * String ncFileText = Utils.readTextFromFile(docFile.getAbsolutePath());
	 * System.out.println("ncFileText=" + ncFileText.length()); ContractNLP nlpK =
	 * new ContractNLP(); // remove any attachments etc, and first empty lines
	 * ncFileText = removeAttachmentsIfAny(ncFileText.replaceAll("^[\r\n ]*", ""));
	 * 
	 * List<String[]> idxsGrps = nlpK.getAllIdxLocsAndMatchedGroups( ncFileText,
	 * patternDescription); System.out.println("idxsGrps.size="+idxsGrps.size()); if
	 * (idxsGrps.size() > 0) { String text = "", solrBeginField, solrEndField,
	 * minimalLongName; // int contractCount = 0; String contractText = null; String
	 * fn = docFile.getName(); acc = fn.substring(0, 20); fn =
	 * fn.substring(acc.length()); StringUtils.removeStart(fn, "_"); String[]
	 * fnParts = fn.split("_"); if (fnParts.length > 1) { acc += "_" + fnParts[1]; }
	 * for (int i = 0; i < idxsGrps.size(); i++) { String contractDescription =
	 * idxsGrps.get(i)[1].replaceAll( "(?i)DESCRIPTION>|(?i)[^a-z0-9\\.,\\-_ ]+",
	 * ""); System.out .println("contractDescription=" + contractDescription);
	 * 
	 * type = getDocumentType(contractDescription); if ((null != type &&
	 * type.toUpperCase().contains("GRAPHIC")) || (null != contractDescription &&
	 * contractDescription .toUpperCase().contains("GRAPHIC"))) continue; int
	 * startIdxContract = Integer.parseInt(idxsGrps.get(i)[0]);
	 * 
	 * if (ncFileText.indexOf("</TEXT") > 0 || ncFileText.indexOf("</text") > 0) {
	 * contractText = ncFileText.substring(startIdxContract,
	 * StringUtils.indexOfIgnoreCase(ncFileText, "</TEXT>", startIdxContract)); if
	 * (contractText.length() < 100) continue; } contractLongName =
	 * contractDescription.substring(0, Math.min(150,
	 * contractDescription.length())); minimalLongName =
	 * contractLongName.replaceAll("[ \t]{2,}", " ") .replaceAll("[^a-zA-Z]{1,}",
	 * "_") .replaceAll("[_,]{2}", "_");
	 * 
	 * //NLP.pwNLP.println(NLP.println("contractLongName=", contractLongName)); //
	 * contractCount++;
	 * 
	 * 
	 * if (containsAny(contractLongName.toLowerCase(), "indenture", "pooling",
	 * "liquidat", "transition", "trust", "agreement")) {
	 * 
	 * text = StringUtils.defaultIfEmpty(contractText, ncFileText); text =
	 * stripHtmlTags(text); System.out.println("text.sub=" + text.substring(0,
	 * 10000));
	 * 
	 * // need to ensure \r\n so I know each hard return is 2 idx // in order to
	 * keep track of idx locations.
	 * 
	 * File outputFile = new File(contractsFolder_Parsed, acc + "_" + i + "_" +
	 * minimalLongName + ".txt"); PrintWriter pwStripped = new
	 * PrintWriter(outputFile);
	 * 
	 * solrBeginField = "<add>\r" + "<doc>\r<field name=\"id\">" + acc + "_" + i +
	 * "</field>\r" + "<field name=\"contract\">" + contractLongName + "</field>\r"
	 * + "<field name=\"contractLongName\"><![CDATA[" + contractLongName +
	 * "]]></field>\r"
	 * 
	 * 
	 * + "<field name=\"cik\">"+cik+"</field>\r" +
	 * "<field name=\"formType\"><![CDATA[" +formType+"]]></field>\r" +
	 * "<field name=\"filer\"><![CDATA["+ companyName+"]]></field>\r" +
	 * "<field name=\"fileDate\">" + fileDate + "T00:00:00Z</field>\r" +
	 * "<field name=\"text\"><![CDATA[\r" ;
	 * 
	 * solrEndField = "\r</doc>\r</add>";
	 * 
	 * text = removePageNumber(text); // need to clean up dangling spaces so
	 * patterns can match. text = text.replaceAll("[ ]{2,}(?=[\r\n]{1})", "");
	 * 
	 * text = text.replaceAll("(?ism)[ \t]{0,100}\r\n[\t ]", "xxrnxxrn"); text =
	 * text.replaceAll("(?ism)[ \t]{0,100}\r[\t ]", "xxrnxxrn"); text =
	 * text.replaceAll("(?ism)[ \t]{0,100}\n[\t ]", "xxrnxxrn"); text =
	 * text.replaceAll("xxrnxxrn", "\r\n").replaceAll( "[\r\n]{5,}",
	 * "\r\n\r\n\r\n");
	 * 
	 * text = getContractIndexSections(text);
	 * 
	 * // File f = new File("c:/getContracts/tmp.txt"); // PrintWriter pw = new
	 * PrintWriter(f); // pw.println(text);
	 * 
	 * // TODO: Here I may want to split exhibits into separate // documents. Or
	 * somehow mark each field as linked // specifically to the contract as opposed
	 * to an Exhibit.
	 * 
	 * if (StringUtils.isNotBlank(text)) text = solrBeginField + text +
	 * solrEndField; // else // text = ""; // pwStripped.append(text); //
	 * pwStripped.close(); // if there was no text written, delete the file if
	 * (StringUtils.isBlank(text)) outputFile.delete(); } } } }
	 */
	private boolean containsAny(String sourceText, String... textsToCheck) {
		for (String str : textsToCheck)
			if (sourceText.contains(str))
				return true;
		return false;
	}

	private String removeAttachmentsIfAny(String text) {
		text = text.replaceAll("(?is)(<PDF>.+?</PDF>|<JPE?G>.+?</JPE?G>)", "");
		return text;
	}

	private String getDocumentType(String contractDescription) {
		if (contractDescription.matches("(?i).*?INDENTURE.*$"))
			return "INDENTURE";
		else if (contractDescription.matches("(?i).*?TRUST AGREEMENT.*$"))
			return "TRUST AGREEMENT";
		else if (contractDescription.matches("(?i).*?LIQUIDATION FILING.*$"))
			return "LIQUIDATION FILING";
		else if (contractDescription.matches("(?i).*?(Ex[\\.\\-]|EXHIBIT).*$"))
			return "EXHIBIT";
		return null;
	}

	public String removePageNumber(String text) throws IOException, SQLException {
		// text = text.replaceAll("[\r\n]{3,50}", "\r\r");
		// gets rid of stranded period (period that starts line)
		text = text.replaceAll("[\r\n]{1,3}\\.", "\\.").replaceAll("[\\.]{2,}", "");
		// gets rid of page # (eg: -1- or - 1 - ).
		text = patternPageNumber.matcher(text).replaceAll("\r\\[wasPgNumber\\]\r");
		/*
		 * PrintWriter tempPw3 = new PrintWriter(new File(
		 * "c:/getContracts/temp/temp33.txt")); tempPw3.append(text); tempPw3.close();
		 */
		return text;
	}

	/*
	 * public String getContractIndexSections(String text) throws IOException,
	 * SQLException {
	 * 
	 * // NOTE: CANNOT CHANGE LENGTH OF TEXT BECAUSE WE NEED TO BACKFILL GAPS //
	 * WITH SOLR FIELDS AND IF WE CHANGE TEXT LENGTH I CORRUPT THE ABILITY // TO
	 * MAKE SOLR FIELDS FOR TEXT NOT CAPTURED BY SECTION RULESET. SO NO // REPLACES
	 * 
	 * text = "\r" + text.replaceAll("([eE]tc)\\.", "$1");
	 * 
	 * tocStartIdx = 0; tocEndIdx = 0; // getContractToc(text);
	 * getContractTocHeadings(text);
	 * 
	 * // TODO: get opening para meta deta // getContractingPartyParagraph(text);
	 * 
	 * // TODO: ADD HERE PRE-AMBLE / WHEREAS CLAUSES ETC. // getPreAmble(text);
	 * 
	 * getDefinedTerms(text);
	 * 
	 * exhStartIdx = 0; exhEndIdx = 0;
	 * 
	 * getExhibitIdxAndNames(text);
	 * 
	 * // if sections from K - no exhibitName to parse //
	 * getSectionIdxsAndNames(text, false, "");
	 * 
	 * // TODO: GET RID OF MAPOFCONTRACT. // FOR STRING ARY -- ONLY NEED TO RECORD
	 * [0]=SIDX/EIDX AND [1] HDG // change filename in pw to be unique Id.
	 * PrintWriter pw = new PrintWriter("c:/temp2/tempContract.txt"); //
	 * pw.append("<add>\r\n<doc>");
	 * 
	 * StringBuffer sb = new StringBuffer();
	 * 
	 * int idx = 0, priorIdx = 0, cnt = 0; String pv = "", val = "", field = "",
	 * priorField = "", field2, priorField2 = null;
	 * 
	 * for (Map.Entry<Integer, String[]> entry : mapOfContractIdxLocs .entrySet()) {
	 * cnt++; val = entry.getValue()[0]; idx = entry.getKey(); field =
	 * entry.getValue()[1].trim(); if (entry.getValue().length > 2) { field2 =
	 * entry.getValue()[2].trim(); } else field2 = "";
	 * 
	 * if (pv.equals("eIdx") && priorIdx > 0) { // //NLP.pwNLP.append(NLP.println(
	 * // "prior idx=eIdx priorIdx=", // priorIdx + " current idx=" + val + " idx="
	 * + idx // + "\rpriorField=" + priorField // + "\r priorField2=" + priorField2
	 * // + "\r1 text.sub" // + text.substring(priorIdx, idx) // + "|endText.Sub"));
	 * }
	 * 
	 * if (idx > 0) { if (pv.equals("sIdx")) { //
	 * //NLP.pwNLP.append(NLP.println("prior idx=sIdx=", priorIdx // + "" +
	 * " current idx=" + val + " idx=" + idx // + "\rpriorField=" + priorField +
	 * "\r priorField2=" // + priorField2) // + "2 text.sub" +
	 * text.substring(priorIdx, idx));
	 * 
	 * if (priorField.replaceAll("[\r\n\t]", "").equals( "tableOfContents")) {
	 * sb.append("\r\n" + fieldNameOpen + priorField + fieldNameClose + "\r\n" +
	 * fieldTextOpen); }
	 * 
	 * if (priorField.replaceAll("[ \r\n\t]", "").equals( "definedTerm") ||
	 * priorField.replaceAll("[ \r\n\t]", "").matches( "sectionHeading") ||
	 * priorField.replaceAll("[ \r\n\t]", "").equals( "exhibitHeading")) {
	 * 
	 * priorField2 = priorField2 .replaceAll( "([\r\n\t]{1,}[ ]{2,}|[ ]{2,})?" +
	 * "(Section|SECTION|Exhibits?|EXHIBITS?)[\t ]{1,5}\\d?\\d?\\d\\.?\\d?\\d?\\d?\\d?.?[ ]{1,}"
	 * , "").trim(); if (priorField.replaceAll("[ \r\n\t]", "").equals(
	 * "definedTerm")) { priorField = priorField.replaceAll(
	 * "[\r\n]([ ]{1,})?|(: ?)", "").trim(); }
	 * 
	 * sb.append("\r\n" + fieldNameOpen + (priorField + "_" +
	 * getOnlyAlphaCharacters(priorField2)) + fieldNameClose + "\r\n" +
	 * fieldTextOpen + priorField2 + fieldTextClose + "\r\n" + fieldNameOpen); }
	 * 
	 * if (priorField.replaceAll("[ \r\n\t]", "").equals( "definedTerm")) {
	 * sb.append("definition_" + getOnlyAlphaCharacters(priorField2) +
	 * fieldNameClose + fieldTextOpen); } if (priorField.replaceAll("[ \r\n\t]",
	 * "").matches( "sectionHeading")) { sb.append("section_" +
	 * getOnlyAlphaCharacters(priorField2) + fieldNameClose + fieldTextOpen); } if
	 * (priorField.replaceAll("[ \r\n\t]", "").equals( "exhibitHeading")) {
	 * sb.append("exhibit_" + getOnlyAlphaCharacters(priorField2) + fieldNameClose +
	 * fieldTextOpen); //
	 * //NLP.pwNLP.append(NLP.println("writing to file exhibitMathch="
	 * ,getOnlyAlphaCharacters(priorField2))); } sb.append(text.substring(priorIdx,
	 * idx)); if (text.substring(priorIdx, idx) .replaceAll("[ \r\n\t]",
	 * "").length() < 10) { //
	 * //NLP.pwNLP.append(NLP.println("how come I'm getting nothing?=" // ,
	 * text.substring(priorIdx, idx) + " pv=" + pv)); } } if (pv.equals("eIdx")) {
	 * sb.append(fieldTextClose); } }
	 * 
	 * //NLP.pwNLP.append(NLP.println("field2=", field2 + " priorField2=" +
	 * priorField2 + " field=" + field + " priorField=" + priorField));
	 * //NLP.pwNLP.append(NLP.println( "text priorIdx=", priorIdx + " idx=" + idx +
	 * " text.start" + text.substring(priorIdx, priorIdx + 10)));
	 * //NLP.pwNLP.append(NLP.println("pv=", pv + " val=" + val)); if ((idx >= 0 &&
	 * priorIdx >= 0 && text.substring(priorIdx, idx) .replaceAll("[ \t\r\n]{1,}",
	 * "").length() > 3 && pv.equals("eIdx") && val.equals("sIdx")) || priorIdx == 0
	 * && idx > 0) { sb.append("\r\n<field name=\"GAP" + fieldNameClose + "\r\n" +
	 * fieldTextOpen + text.substring(priorIdx, idx) + fieldTextClose);
	 * //NLP.pwNLP.append(NLP.println("this is gap text==", text.substring(priorIdx,
	 * idx))); } pv = val; priorIdx = idx; priorField = field; priorField2 = field2;
	 * 
	 * // last pass. if (val.equals("eIdx") && cnt == mapOfContractIdxLocs.size()) {
	 * sb.append(fieldTextClose); }
	 * 
	 * if (cnt == mapOfContractIdxLocs.size() && idx < text.length()) { if
	 * (text.substring(idx, text.length()) .replaceAll("[\r\n\t \\.]", "").length()
	 * > 5)
	 * 
	 * { sb.append("\r\n<field name=\"GAP" + fieldNameClose + "\r\n" + fieldTextOpen
	 * + text.substring(idx, text.length()) + fieldTextClose);
	 * 
	 * } } }
	 * 
	 * // pw.append(sb.toString()); // System.out.println("sb.toString=" +
	 * sb.toString()); // pw.close(); // //NLP.pwNLP.close(); return text; }
	 * 
	 */
	public static void getContractCoverPage(String text) throws IOException {

		// TODO: CREATE LOGIC TO CAPTURE COVER PAGE.
		// List<String[]> listCoverPage = new ArrayList<>();
		// String[] strAry = { coverPageEndIdx + "", "coverPage", sb.toString()
		// };
		// listCoverPage.add(strAry);
		// mapOfContract.put(tocStartIdx, CoverPage);

	}

	public static void getContractTocHeadings(String text) throws NumberFormatException, IOException {

		NLP nlp = new NLP();

		Pattern patternSectionArticleToc = Pattern
				.compile("(?s)[\r\n]{1,2}[\t ]{0,30}(Section|Sub-? ?[Ss]ection|SUB-? ?SECTION|SECTION|Article|ARTICLE)"
						+ "[ \t]{1,5}[iIvVxX\\d]{1,6}\\.\\d?\\d?[A-Za-z ;\\.]");

		Pattern patternSecNumber = Pattern.compile("[ \t]{1}[\\d]{1,2}(\\.[\\d]{1,2})?");

		boolean isItToc = false, doesTocHaveExhibits = false;

		List<String[]> listTocSecHdgs = new ArrayList<>();
		List<String[]> listTocSecHdgsTmp = new ArrayList<>();
		listTocSecHdgsTmp = nlp.getAllStartIdxLocsAndMatchedGroups(text, patternSectionArticleToc);

		// //NLP.pwNLP.append(NLP.println("text=",text));

		int sIdx = 0, priorSidx = 0, priorSidx2 = 0, cnt = 0;
		double dif = 0, secNum = 0, priorSecNum = 0;
		int df = 0, maxDif = 300, lastSectionHeadingAddedLength = 0;
		boolean tocOrder = false, done = false;
		String tmpTxt = "", tmpStr = "", tmpStr2 = "";

//		//NLP.pwNLP.append(NLP.println(
//				"start of get contractTableOfContents listTocSecHdgsTmp.size=",
//				+listTocSecHdgsTmp.size() + ""));

		for (int i = 0; i < listTocSecHdgsTmp.size(); i++) {

//			//NLP.pwNLP.append(NLP.println("1 sIdx="+sIdx+" priorSidx=",
//					priorSidx + " priorSidx2=" + priorSidx2));

//			//NLP.pwNLP.append(NLP.println(
//					"2 toc - listTocSecHdgs[1]=",
//					listTocSecHdgsTmp.get(i)[1] + " sIdx="
//							+ listTocSecHdgsTmp.get(i)[0] + " i=" + i
//							+ " .size=" + listTocSecHdgsTmp.size()));
//			sIdx = Integer.parseInt(listTocSecHdgsTmp.get(i)[0]);

//			//NLP.pwNLP.append(NLP.println("3 sIdx="+sIdx+" priorSidx=",
//					priorSidx + " priorSidx2=" + priorSidx2));

			tmpTxt = text.substring(priorSidx, sIdx).replaceAll("(?sm)(ARTICLE|Article).{1,1500}(SECTION|Section)", "")
					.replaceAll("[\t\r\n ]{1,}", "");

//			//NLP.pwNLP.append(NLP.println("4 sIdx="+sIdx+" priorSidx=",
//					priorSidx + " priorSidx2=" + priorSidx2));

			if (tmpTxt.length() <= maxDif && i > 0) {
				String[] ary = { listTocSecHdgsTmp.get(i - 1)[0], listTocSecHdgsTmp.get(i - 1)[1] };
				// //NLP.pwNLP.append(NLP.println("5 added ary to listTocSecHdgs=",
				// Arrays.toString(ary)));
				// //NLP.pwNLP.append(NLP.println("6 sIdx="+sIdx+" priorSidx=",
				// priorSidx + " priorSidx2=" + priorSidx2));

				listTocSecHdgs.add(ary);
			}

			// //NLP.pwNLP.append(NLP.println("7 sIdx="+sIdx+" priorSidx=",
			// priorSidx + " priorSidx2=" + priorSidx2 + "1 tmpTxt.len="
			// + tmpTxt.length() + " i=" + i));

			if (tmpTxt.length() > maxDif && i >= 0 && priorSidx - priorSidx2 < 500 && listTocSecHdgsTmp.size() > (i + 1)
					&& i > 0) {
				String[] ary = { listTocSecHdgsTmp.get(i - 1)[0], listTocSecHdgsTmp.get(i - 1)[1] };
				// //NLP.pwNLP.append(NLP.println("8 added ary to listTocSecHdgs=",
				// Arrays.toString(ary) + "||1aENDAdded"));
				listTocSecHdgs.add(ary);
				tocEndIdx = Integer.parseInt(listTocSecHdgsTmp.get(i - 1)[0]);
				break;
			}

			if (i + 1 == listTocSecHdgsTmp.size() && tmpTxt.length() < maxDif) {
				String[] ary = { listTocSecHdgsTmp.get(i)[0], listTocSecHdgsTmp.get(i)[1] };
				listTocSecHdgs.add(ary);
//				//NLP.pwNLP.append(NLP.println(
//						"9 added last ary to listTocSecHdgs=",
//						Arrays.toString(ary) + "||2aENDAdded"));
			}

//			//NLP.pwNLP.append(NLP.println("10 sIdx="+sIdx+" priorSidx=",
//					priorSidx + " priorSidx2=" + priorSidx2));

			priorSidx2 = priorSidx;

//			//NLP.pwNLP.append(NLP.println("11 sIdx="+sIdx+" priorSidx=",
//					priorSidx + " priorSidx2=" + priorSidx2));

			priorSidx = sIdx;

			// NLP.pwNLP.append(NLP.println("12 sIdx="+sIdx+" priorSidx=",
//					priorSidx + " priorSidx2=" + priorSidx2));

		}

		cnt = 0;
		tmpTxt = "";
		sIdx = 0;
		priorSidx = 0;
		if (listTocSecHdgs.size() > 0) {
			for (int i = 0; i < listTocSecHdgs.size(); i++) {
				sIdx = Integer.parseInt(listTocSecHdgs.get(i)[0]);
				if (priorSidx > 0) {
					tmpTxt = text.substring(priorSidx, sIdx).replaceAll("[\t\r\n ]{1,}", "");
				}
//				//NLP.pwNLP.append(NLP.println("2 tmpTxt.len=", tmpTxt.length()
//						+ " sIdx="+sIdx+" priorSidx=" + priorSidx + " maxDif=" + maxDif
//						+ " toc sec tmpTxt=" + tmpTxt));
				if (priorSidx > 0 && tmpTxt.length() < maxDif) {
					cnt++;
//					//NLP.pwNLP.append(NLP
//							.println("cnt exhibit - cnt=", "" + cnt));
				}
				priorSidx = sIdx;
				if (cnt > 8) {
					isItToc = true;
				}
				if (isItToc)
					break;
			}

//			//NLP.pwNLP.append(NLP
//					.println("- isItToc=", "" + isItToc + " tocStartIdx="
//							+ tocStartIdx + " tocEndIdx=" + tocEndIdx));

			priorSidx = 0;
			sIdx = 0;
			boolean putInMap = false;
			if (isItToc) {
				if (listTocSecHdgs.size() > 1) {
					for (int i = 1; i < listTocSecHdgs.size(); i++) {
						putInMap = false;
						if (i == 1) {
							priorSidx = Integer.parseInt(listTocSecHdgs.get(0)[0]);
							tocStartIdx = priorSidx;
//							//NLP.pwNLP.append(NLP.println("@i=0, tocStartIdx=",
//									"" + tocStartIdx));
						}
						sIdx = Integer.parseInt(listTocSecHdgs.get(i)[0]);
//						//NLP.pwNLP.append(NLP.println("1 sIdx=", sIdx
//								+ " priorSidx=" + priorSidx));
//						//NLP.pwNLP.append(NLP.println(
//								"listTocSecHdgs.get(i)[1]=",
//								listTocSecHdgs.get(i)[1]));
						if (nlp.getAllMatchedGroups(listTocSecHdgs.get(i)[1], patternSecNumber).size() > 0) {
							secNum = Double.parseDouble(
									nlp.getAllMatchedGroups(listTocSecHdgs.get(i)[1], patternSecNumber).get(0));
//							//NLP.pwNLP.append(NLP.println("aa secNum=", secNum
//									+ ""));
						}

						tocOrder = false;
//						//NLP.pwNLP.append(NLP.println("secNum=", secNum
//								+ " priorSecNun=" + priorSecNum));
						dif = (secNum - priorSecNum) * 100;
						dif = Math.round(dif);
						df = (int) (dif);
						tmpTxt = text.substring(priorSidx, sIdx)
								.replaceAll("(?sm)(ARTICLE|Article).{1,1500}(SECTION|Section)", "")
								.replaceAll("[\t\r\n ]{1,}", "");

//						//NLP.pwNLP.append(NLP.println(
//								"secNum less priorSecNum df=",
//								df
//										+ " dif="
//										+ dif
//										+ " tmptTxt.len="
//										+ tmpTxt.length()
//										+ " priorSidx="
//										+ priorSidx
//										+ " sIdx="
//										+ sIdx
//										+ " listToc.get(i)=="
//										+ listTocSecHdgs.get(i)[1].replaceAll(
//												"[\r\n]", "")));

						// //NLP.pwNLP.append(NLP.println("4 tmpTxt=", tmpTxt));

						if (tmpTxt.length() < maxDif && ((int) secNum - (int) priorSecNum) == 0
								&& (df == 10 || df == 1 || df == -80) && priorSidx > 0) {
							tocOrder = true;
							// //NLP.pwNLP.append(NLP.println("1 tocOrder=" ,
							// tocOrder));
							cnt++;
						}

						if (tmpTxt.length() < maxDif && ((int) secNum - (int) priorSecNum) == 1 && priorSidx > 0) {
							tocOrder = true;
							// //NLP.pwNLP.append(NLP.println("2 tocOrder=" ,
							// tocOrder));
							cnt++;
						}

						// //NLP.pwNLP.append(NLP.println("1 listToc.get(i)==",
						// listTocSecHdgs.get(i)[1] + "secNum=" + secNum
						// + " priorSecNum=" + priorSecNum
						// + " sIdx=" + sIdx + " priorSidx="
						// + priorSidx + " tocOrder=" + tocOrder
						// + " df=" + df + " dif=" + dif));

						tmpStr = text.substring(priorSidx, sIdx);
						if (priorSidx > 0 && tocOrder && tmpTxt.length() < maxDif
								&& tmpStr.replaceAll("[\r\n\t ]", "").length() > 1) {

							// //NLP.pwNLP.append(NLP.println(
							// "2 listToc.get(i)==",
							// listTocSecHdgs.get(i)[1].replaceAll(
							// "[\r\n]", "")
							// + "secNum="
							// + secNum
							// + " priorSecNum="
							// + priorSecNum
							// + " sIdx="
							// + sIdx
							// + " priorSidx="
							// + priorSidx
							// + " tocOrder="
							// + tocOrder));

							// //NLP.pwNLP.append(NLP.println("tmpStr=" ,
							// tmpStr));
							int tmpSidx = 0;
							if (nlp.getAllIndexEndLocations(tmpStr, Pattern.compile("(?is)(ARTICLE|Article)"))
									.size() > 0) {
								tmpSidx = nlp
										.getAllIndexStartLocations(tmpStr, Pattern.compile("(?is)(ARTICLE|Article)"))
										.get(0);
							}

							if (tmpSidx > 0) {

								tmpStr2 = text.substring(priorSidx, tmpSidx + priorSidx);

								String[] strAry = { sIdx + "", "toc_heading" + getOnlyAlphaCharacters(tmpStr2),
										tmpStr2 };

								lastSectionHeadingAddedLength = tmpStr2.length();
								tocHeading = tocHeading + "\r\n"
										+ tmpStr2.replaceAll("[\r\n\t]", " ").replaceAll("[ ]{2,}", " ");
								mapOfSectionHeadingsFromToc.put(priorSidx, strAry);

//								//NLP.pwNLP.append(NLP.println("secNum=",
//										secNum + " priorSecNum=" + priorSecNum
//												+ "||1 toc_heading strAry="
//												+ Arrays.toString(strAry)));

								tmpStr2 = text.substring(tmpSidx + priorSidx, sIdx);

								String[] strAy = { tmpSidx + priorSidx + "",
										"toc_heading_" + getOnlyAlphaCharacters(tmpStr2), tmpStr2 };

								tocHeading = tocHeading + "\r\n"
										+ tmpStr2.replaceAll("[\r\n\t]", " ").replaceAll("[ ]{2,}", " ");
								lastSectionHeadingAddedLength = tmpStr2.length();
								tocEndIdx = sIdx;
								mapOfSectionHeadingsFromToc.put(tmpSidx + priorSidx, strAy);
//								//NLP.pwNLP.append(NLP.println("secNum=",
//										secNum + " priorSecNum=" + priorSecNum
//												+ "||2 toc_heading strAry="
//												+ Arrays.toString(strAry)));
							}

							else {

								tmpStr2 = text.substring(priorSidx, sIdx);

								String[] strAry = { sIdx + "", "toc_heading" + getOnlyAlphaCharacters(tmpStr2),
										text.substring(priorSidx, sIdx) };

								tocHeading = tocHeading + "\r\n"
										+ tmpStr2.replaceAll("[\r\n\t]", " ").replaceAll("[ ]{2,}", " ");
//								//NLP.pwNLP.append(NLP.println("secNum=",
//										secNum + " priorSecNum=" + priorSecNum
//												+ "||3 toc_heading strAry="
//												+ Arrays.toString(strAry)));
								lastSectionHeadingAddedLength = tmpStr2.length();
								tocEndIdx = sIdx;
								mapOfSectionHeadingsFromToc.put(priorSidx, strAry);
							}
						}

						if (sIdx - priorSidx >= 500) {
							int tmpIdx = 0;
							tmpStr = text.substring(priorSidx);
							tmpStr = tmpStr.replaceAll("(?is)[\r\n]{1,}(?=Section)", "");
							// nlp.getAllIndexEndLocations(tmpStr,
							// Pattern.compile("[\r\n]")).get(0);
							sIdx = priorSidx + 1;
							if (nlp.getAllIndexEndLocations(tmpStr, Pattern.compile("[\r\n]")).size() > 0) {
								sIdx = nlp.getAllIndexEndLocations(tmpStr, Pattern.compile("[\r\n]")).get(0);
							}

							tmpStr = text.substring(priorSidx, sIdx + priorSidx + 1);
							// //NLP.pwNLP.append(NLP.println("text===",text+"|"));
							if (tmpStr.replaceAll("[\r\n\t ]", "").length() < 2) {
								done = true;
								break;
							}

							String[] strAry = { (1 + priorSidx + sIdx) + "toc_heading" + getOnlyAlphaCharacters(tmpStr),
									tmpStr };

							tocHeading = tocHeading + "\r\n"
									+ tmpStr.replaceAll("[\r\n\t]", " ").replaceAll("[ ]{2,}", " ");
//							//NLP.pwNLP.append(NLP.println("secNum=",
//									secNum + " priorSecNum=" + priorSecNum
//											+ "||4 toc_heading strAry="
//											+ Arrays.toString(strAry)));
							tocEndIdx = 1 + priorSidx + sIdx;
							mapOfSectionHeadingsFromToc.put(priorSidx, strAry);
							lastSectionHeadingAddedLength = tmpStr.length();
							done = true;
						}

						boolean thisIsLast = false;
						if (i + 2 == listTocSecHdgs.size()) {
							String str = listTocSecHdgs.get(i + 1)[1];
							if (str.replaceAll("[\r\n \t]", "").length() < 1)
								thisIsLast = true;
						}

						if (i + 2 == listTocSecHdgs.size()) {
//							//NLP.pwNLP.append(NLP.println("done=",
//									done + " thisIsLast=" + thisIsLast
//											+ "listTocSecHdgs.size="
//											+ listTocSecHdgs.size() + " i=" + i
//											+ " listTocSecHdgs.get(i)="
//											+ listTocSecHdgs.get(i)[1]));
						}

						if (!done && (i + 1 == listTocSecHdgs.size() || thisIsLast)) {
							String lastSection = text.substring(sIdx);
							String line = "";
							String[] lastSectionSplit = lastSection.split("[\r\n]{1,2}");
							tmpStr = "";
							for (i = 0; i < lastSectionSplit.length; i++) {
								line = lastSectionSplit[i];
								if (i < 2 && line.replaceAll("[\r\n \t]", "").length() == 0) {
									tmpStr = "\r\n" + tmpStr;
									continue;
								}

								tmpStr = tmpStr + "\r\n" + line;
								// last section can't be more than 8 lines.
								if (line.replaceAll("[\r\n \t]", "").length() == 0 || i > 7) {
									done = true;
									break;
								}

								if (done)
									break;
							}

							if (tmpStr.replaceAll("[\r\n\t ]", "").length() < 2)
								break;

							String[] strAry = { ((sIdx + tmpStr.length())) + "",
									"toc_heading" + getOnlyAlphaCharacters(tmpStr), tmpStr };

							tocHeading = tocHeading + "\r\n"
									+ tmpStr.replaceAll("[\r\n\t]", " ").replaceAll("[ ]{2,}", " ");
//							//NLP.pwNLP.append(NLP.println("secNum=",
//									secNum + " priorSecNum=" + priorSecNum
//											+ "||5 toc_heading strAry="
//											+ Arrays.toString(strAry)));
							mapOfSectionHeadingsFromToc.put(priorSidx, strAry);
							tocEndIdx = sIdx;
							lastSectionHeadingAddedLength = tmpStr.length();
							if (done) {
								break;
							}
						}

						// //NLP.pwNLP.append(NLP.println("2 sIdx=" , sIdx +
						// " priorSidx="
						// + priorSidx));
						priorSidx = sIdx;
						priorSecNum = secNum;
					}
				}
			}

		}

		Pattern patternExhibitsSchedulesToc = Pattern
				.compile("[\r\n]{1,2}[\t ]{0,30}(ANNEXE?S?|SCHEDULES?|EXHIBITS?|Annexe?s?|Schedules?|Exhibits?)"
						+ "[ \t]{0,5}[iIvVxX\\dA-Za-z ;\\.-_]{0,5}.+[\r\n]{1}");

		Pattern patternExhNum = Pattern.compile("(?<=((ANNEXE?S?|SCHEDULES?|EXHIBITS?|Annexe?s?|Schedules?|Exhibits?)"
				+ "([\r\n]{0,3})?[ \t]{1,5}))[iIvVxX\\dA-Za-z ;\\.-_]{1,5}(?=[: \r\n\t]{1})");

		int lastIdx = priorSidx;

		if (priorSidx > 0) {
			tocEndIdx = lastIdx;
		}

//		//NLP.pwNLP.append(NLP.println("tocEndIdx=lastIdx=priorSidx=", tocEndIdx
//				+ ""));

		double exhNumAlpha = 0, priorExhNumAlpha = 0, exhNumRoma = 0, priorExhNumRoma = 0;
		int exhSidx = lastIdx, priorExhSidx = lastIdx;

		List<String[]> listExhibitsSchedulesToc = nlp.getAllStartIdxLocsAndMatchedGroups(text.substring(lastIdx),
				patternExhibitsSchedulesToc);

		for (int i = 0; i < listExhibitsSchedulesToc.size(); i++) {
//			//NLP.pwNLP.append(NLP.println("pattern found these Exhibits=",
//					listExhibitsSchedulesToc.get(i)[1]));
		}

		String exhibit = "", exhNumStr = "";

		// //NLP.pwNLP.append(NLP.println("aaa lastIdx=" , lastIdx));

		cnt = 0;
		tmpTxt = "";
		exhSidx = 0;
		priorExhSidx = lastIdx;
		if (listExhibitsSchedulesToc.size() > 0) {

			for (int i = 0; i < listExhibitsSchedulesToc.size(); i++) {
				exhSidx = Integer.parseInt(listExhibitsSchedulesToc.get(i)[0]) + lastIdx;
				if (priorExhSidx > 0) {
					tmpTxt = text.substring(priorExhSidx, exhSidx).replaceAll("[\t\r\n ]{1,}", "");
				}
//				//NLP.pwNLP.append(NLP.println("3 tmpTxt.len=", tmpTxt.length()
//						+ " priorExhSidx=" + priorExhSidx + " maxDif=" + maxDif
//						+ " exhibit tmpTxt=" + tmpTxt));
				if (priorExhSidx > 0 && tmpTxt.length() < maxDif) {
					cnt++;
//					//NLP.pwNLP.append(NLP
//							.println("cnt exhibit - cnt=", "" + cnt));
				}
				priorExhSidx = exhSidx;
				if (cnt > 1) {
					doesTocHaveExhibits = true;
				}
				if (doesTocHaveExhibits)
					break;
			}

//			//NLP.pwNLP.append(NLP.println("exhibit - isItToc=", "" + isItToc));
			boolean doneToc = false;

			if (doesTocHaveExhibits) {
				exhSidx = 0;
				priorExhSidx = lastIdx;
				for (int i = 0; i < listExhibitsSchedulesToc.size(); i++) {
					if (doneToc)
						break;
					exhibit = listExhibitsSchedulesToc.get(i)[1];
					exhSidx = Integer.parseInt(listExhibitsSchedulesToc.get(i)[0]) + lastIdx;
//					//NLP.pwNLP.append(NLP.println("exhibit=", exhibit + "|"));
					exhNumStr = "";
					if (nlp.getAllMatchedGroups(exhibit, patternExhNum).size() > 0) {
						exhNumStr = nlp.getAllMatchedGroups(exhibit, patternExhNum).get(0);
//						//NLP.pwNLP.append(NLP.println("exhNumStr=", exhNumStr));
//						//NLP.pwNLP.append(NLP.println("roman #",
//								"" + Roman.decode(exhNumStr)));
//						//NLP.pwNLP.append(NLP.println("alpha #", ""
//								+ getNumberInAlphabet(exhNumStr)));

					}

//					//NLP.pwNLP.append(NLP.println("exhSidx - priorExhSidx=",
//							(exhSidx - priorExhSidx) + " exhSidx=" + exhSidx
//									+ " priorExhSidx=" + priorExhSidx
//									+ " exhibit=" + exhibit));

					// get 1st exhibit that that will be passed over otherwise
					// if lastIdx=priorExhSidx.
					if (priorExhSidx == lastIdx
							&& Integer.parseInt(listExhibitsSchedulesToc.get(i + 1)[0]) - exhSidx < 170
							&& i + 1 < listExhibitsSchedulesToc.size()) {
						String[] strAry = { listExhibitsSchedulesToc.get(i)[0], listExhibitsSchedulesToc.get(i)[1] };
						tocHeading = tocHeading + "\r\n"
								+ exhibit.replaceAll("[\r\n\t]", " ").replaceAll("[ ]{2,}", " ");
						mapOfSectionHeadingsFromToc.put(exhSidx, strAry);
						tocEndIdx = Integer.parseInt(listExhibitsSchedulesToc.get(i)[0]);
						lastSectionHeadingAddedLength = listExhibitsSchedulesToc.get(i)[1].length();
						// NLP.pwNLP.append(NLP.println("1a tocEndIdx="
//								+ tocEndIdx + "||1st toc_heading strAry=",
//								Arrays.toString(strAry)));

					}

					if (exhSidx - priorExhSidx < 170 && priorExhSidx != lastIdx) {
						String[] strAry = { exhSidx + "", exhibit };

						tocHeading = tocHeading + "\r\n"
								+ exhibit.replaceAll("[\r\n\t]", " ").replaceAll("[ ]{2,}", " ");
						// NLP.pwNLP.append(NLP.println(
//								"||2 middle toc_heading strAry=",
//								Arrays.toString(strAry)));
						mapOfSectionHeadingsFromToc.put(priorExhSidx, strAry);
						tocEndIdx = exhSidx;
						lastSectionHeadingAddedLength = exhibit.length();
						// NLP.pwNLP.append(NLP.println("2a tocEndIdx="
//								+ tocEndIdx + "||1st toc_heading strAry=",
//								Arrays.toString(strAry)));

					}
					// this is the last exhibit end loop
					// NLP.pwNLP.append(NLP.println(
//							"listExhibitsSchedulesToc.size()=",
//							listExhibitsSchedulesToc.size() + " i=" + i));

					if (i - 1 < listExhibitsSchedulesToc.size() & i > 0
							&& Integer.parseInt(listExhibitsSchedulesToc.get(i - 1)[0]) > tocEndIdx) {
						break;
					}

					// this will break and thereby cause only 1st doc's' TOC to
					// be placed in its fields.

					if (exhSidx - priorExhSidx >= 170 && i - 1 < listExhibitsSchedulesToc.size() & i > 0
							&& Integer.parseInt(listExhibitsSchedulesToc.get(i - 1)[0]) > tocEndIdx) {
						// NLP.pwNLP.append(NLP.println(
//								"listExhibitsSchedulesToc.get(i - 1)[0]=",
//								listExhibitsSchedulesToc.get(i - 1)[0]));
						String[] strAry = { listExhibitsSchedulesToc.get(i - 1)[0],
								listExhibitsSchedulesToc.get(i - 1)[1] };

						tocHeading = tocHeading + "\r\n" + listExhibitsSchedulesToc.get(i - 1)[1]
								.replaceAll("[\r\n\t]", " ").replaceAll("[ ]{2,}", " ");
						// NLP.pwNLP.append(NLP.println(
//								"||3 last toc_heading strAry=",
//								Arrays.toString(strAry)));
						mapOfSectionHeadingsFromToc.put(priorExhSidx, strAry);
						// NLP.pwNLP.append(NLP
//								.println("last toc text.substr=", text
//										.substring(priorExhSidx,
//												priorExhSidx + 100)));

						// NLP.pwNLP.append(NLP.println(
//								"3a before adj tocEndIdx=", "" + tocEndIdx));

						tocEndIdx = (text.substring(priorExhSidx)).indexOf("\r") + priorExhSidx;

						lastSectionHeadingAddedLength = listExhibitsSchedulesToc.get(i - 1)[1].length();
						// NLP.pwNLP.append(NLP.println("3a tocEndIdx="
//								+ tocEndIdx + "||3 last toc_heading strAry=",
//								Arrays.toString(strAry)));

						doneToc = true;
					}

					// //NLP.pwNLP.append(NLP.println("schedule&exhibits="
					// , listExhibitsSchedulesToc.get(i)[1] + "||"));
					// getNumberInAlphabet
					// if roman number - get value
					// decode("MCMXC")

					priorExhSidx = exhSidx;
					priorExhNumAlpha = exhNumAlpha;
					priorExhNumRoma = exhNumRoma;
					if (doneToc)
						break;
				}
			}
		}

		if (!isItToc) {
			// NLP.pwNLP.append(NLP.println(
//					"not TOC - tocStartIdx set to zero from whta it was=",
//					tocStartIdx + ""));
			tocStartIdx = 0;
		}

		String[] ary = { "sIdx", "tableOfContents" };
		mapOfContractIdxLocs.put(tocStartIdx, ary);
		String[] ary2 = { "eIdx", "TOC" };
		mapOfContractIdxLocs.put(tocEndIdx + lastSectionHeadingAddedLength, ary2);
		// NLP.pwNLP.append(NLP.println("tocStartIdx=", tocStartIdx
//				+ " tocEndIdx=" + tocEndIdx));
		// tocEndIdx = 10540;
		if (tocStartIdx < tocEndIdx)
			toc = text.substring(tocStartIdx, tocEndIdx);
		// NLP.pwNLP.append(NLP.println("start of toc=", toc + "|tocEnd||"));

	}

	public static void getContractingPartyParagraph(String text) {

		// TODO: CREATE LOGIC. ALSO PARSE META DATA -- CONTRACT NAME, DATE,
		// CONTRACTING PARTIES AND THEIR ROLES AND DATE.

		// openNLP and lingPipe have java APIs. However I will probably want to
		// just create my own. Given I'm parsing in context of opening para and
		// can inherently use that context.

		/*
		 * THIS POOLING AND SERVICING AGREEMENT, dated as of November 1, 2004, among
		 * MORGAN STANLEY ABS CAPITAL I INC., a Delaware corporation (the "Depositor"),
		 * COUNTRYWIDE HOME LOANS SERVICING LP, a Texas limited partnership (the
		 * "Servicer"), AAMES CAPITAL CORPORATION, a California corporation ("Aames"),
		 * NC CAPITAL CORPORATION, a California corporation ("NC Capital"), ACCREDITED
		 * HOME LENDERS, INC., a California corporation ("Accredited"), and DEUTSCHE
		 * BANK NATIONAL TRUST COMPANY, a national banking association, as trustee (the
		 * "Trustee").
		 */

		// TODO: CREATE LOGIC TO CAPTURE COVER PAGE.
		// List<String[]> listOpeningPara = new ArrayList<>();
		// String[] strAry = { opEndIdx + "", "openingPara", sb.toString()
		// };
		// listOpeningPara.add(strAry);
		// mapOfContract.put(opStartIdx, listOpeningPara);

	}

	public static void getPreAmble(String text) {

		// TODO: CREATE LOGIC TO CAPTURE COVER PAGE.
		// List<String[]> listPreAmble = new ArrayList<>();
		// String[] strAry = { coverPageEndIdx + "", "PreAmble", sb.toString()
		// };
		// listPreAmble.add(strAry);
		// mapOfContract.put(PreAmbleStartIdx, listPreAmble);

	}

	public static void getDefinedTerms(String text) throws IOException {

		NLP nlp = new NLP();
		@SuppressWarnings("unused")
		StringBuffer sb = new StringBuffer();

		List<String[]> listDefinedWordsInDefinitionArticle = nlp.getAllStartIdxLocsAndMatchedGroups(text,
				patternDefinedWordsInDefinitionArticle);

		String firstLtr = "", oneAheadFirstLetter, oneAheadFirstLetter2, oneAheadFirstLetter3, definedTerm = "",
				oneDefinedTermAhead = "", definition = "", nextBehindDefinedTerm = "", twoBehindDefinedTerm = "",
				threeBehindDefinedTerm = "", fourBehindDefinedTerm = "", fiveBehindDefinedTerm = "",
				sixBehindDefinedTerm = "", sevenBehindDefinedTerm = "", eightBehindDefinedTerm = "";

		Integer oneAheadLtr = 0, oneAheadLtr2 = 0, oneAheadLtr3 = 0, ltr = 0, ltr2 = 0, ltr3 = 0, nextBehindLtr = 0,
				twoBehindLtr = 0, threeBehindLtr = 0, fourBehindLtr = 0, fiveBehindLtr = 0, sixBehindLtr = 0,
				sevenBehindLtr = 0, eightBehindLtr = 0, nextBehindLtr2 = 0, twoBehindLtr2 = 0, threeBehindLtr2 = 0,
				fourBehindLtr2 = 0, fiveBehindLtr2 = 0, sixBehindLtr2 = 0, sevenBehindLtr2 = 0, eightBehindLtr2 = 0,
				nextBehindLtr3 = 0, twoBehindLtr3 = 0, threeBehindLtr3 = 0, fourBehindLtr3 = 0, fiveBehindLtr3 = 0,
				sixBehindLtr3 = 0, sevenBehindLtr3 = 0, eightBehindLtr3 = 0;
		Integer sIdx = 0, nextBehindSidx = 0, twoBehindSidx = 0, threeBehindSidx = 0, fourBehindSidx = 0,
				fiveBehindSidx = 0, sixBehindSidx = 0, sevenBehindSidx = 0, eigthBehindSidx = 0, cnt = 0;
		if (listDefinedWordsInDefinitionArticle.size() > 7) {

			for (int i = 0; i < listDefinedWordsInDefinitionArticle.size(); i++) {
				definedTerm = listDefinedWordsInDefinitionArticle.get(i)[1];
				if (listDefinedWordsInDefinitionArticle.size() > (i + 1)) {
					oneDefinedTermAhead = listDefinedWordsInDefinitionArticle.get(i + 1)[1];
				}

				firstLtr = definedTerm.replaceAll("[\"\r\n ]", "").substring(0, 1);

				ltr = getNumberInAlphabet(firstLtr);
				sIdx = Integer.parseInt(listDefinedWordsInDefinitionArticle.get(i)[0]);
				if (firstLtr.matches("^[\\d]$")) {
					ltr2 = getNumberLetter(Integer.parseInt(firstLtr));
					// //NLP.pwNLP.append(NLP.println("ltr2=" , ltr2));
				} else {
					ltr2 = 0;
					ltr3 = 0;
				}

				// //NLP.pwNLP.append(NLP.println("definedTerm=" , definedTerm +
				// " firstLtr="
				// + firstLtr + " ltr#=" + ltr));

				if (i > 5) {
					cnt = 0;
					if ((nextBehindLtr <= ltr && nextBehindLtr > 0) || (nextBehindLtr2 <= ltr2 && nextBehindLtr2 > 0)
							|| (nextBehindLtr3 <= ltr3 && nextBehindLtr3 > 0))
						cnt++;
					if ((twoBehindLtr <= nextBehindLtr && twoBehindLtr > 0)
							|| (twoBehindLtr2 <= nextBehindLtr2 && twoBehindLtr2 > 0)
							|| (twoBehindLtr3 <= nextBehindLtr3 && twoBehindLtr3 > 0))
						cnt++;

					if ((threeBehindLtr <= twoBehindLtr && threeBehindLtr > 0)
							|| (threeBehindLtr2 <= twoBehindLtr2 && threeBehindLtr2 > 0)
							|| (threeBehindLtr3 <= twoBehindLtr3 && threeBehindLtr3 > 0))
						cnt++;
					if ((fourBehindLtr <= threeBehindLtr && fourBehindLtr > 0)
							|| (fourBehindLtr2 <= threeBehindLtr2 && fourBehindLtr2 > 0)
							|| (fourBehindLtr3 <= threeBehindLtr3 && fourBehindLtr3 > 0))
						cnt++;
					if ((fiveBehindLtr <= fourBehindLtr && fiveBehindLtr > 0)
							|| (fiveBehindLtr2 <= fourBehindLtr2 && fiveBehindLtr2 > 0)
							|| (fiveBehindLtr3 <= fourBehindLtr3 && fiveBehindLtr3 > 0))
						cnt++;
					if ((sixBehindLtr <= fiveBehindLtr && sixBehindLtr > 0)
							|| (sixBehindLtr2 <= fiveBehindLtr2 && sixBehindLtr2 > 0)
							|| (sixBehindLtr3 <= fiveBehindLtr3 && sixBehindLtr3 > 0))
						cnt++;
					if ((sevenBehindLtr <= sixBehindLtr && sevenBehindLtr > 0)
							|| (sevenBehindLtr2 <= sixBehindLtr2 && sevenBehindLtr2 > 0)
							|| (sevenBehindLtr3 <= sixBehindLtr3 && sevenBehindLtr3 > 0))
						cnt++;
					if ((eightBehindLtr <= sevenBehindLtr && eightBehindLtr > 0)
							|| (eightBehindLtr2 <= sevenBehindLtr2 && eightBehindLtr2 > 0)
							|| (eightBehindLtr3 <= sevenBehindLtr3 && eightBehindLtr3 > 0))
						cnt++;
				}

				// this gets first defined term when there are 6 of 8 ahead in
				// alphabetical order.

				if (cnt > 5 && ((eightBehindLtr <= sevenBehindLtr && sevenBehindLtr - eightBehindLtr < 2)
						|| (eightBehindLtr <= sevenBehindLtr && sevenBehindLtr - eightBehindLtr < 2)
						|| (eightBehindLtr3 <= sevenBehindLtr3 && sevenBehindLtr3 - eightBehindLtr3 < 2))) {
					definition = text.substring(eigthBehindSidx, sevenBehindSidx);

					// //NLP.pwNLP.append(NLP.println("1 add to list -- firstLtr="
					// ,
					// firstLtr
					// + " eightBehindDefinedTerm="
					// + eightBehindDefinedTerm + " \rdefinition="
					// + definition));

					String[] strAry2 = { "sIdx", "definedTerm",
							eightBehindDefinedTerm.replaceAll("[\r\n]([ ]{1,})?|(: ?)", "") };
					if (eightBehindDefinedTerm.replaceAll("[\r\n]([ ]{1,})?|(: ?)", "").length() > 3) {

						mapOfContractIdxLocs.put(eigthBehindSidx, strAry2);
						String[] strAry3 = { "eIdx", "" };
						mapOfContractIdxLocs.put((eigthBehindSidx + definition.length() - 1), strAry3);
						// //NLP.pwNLP.append(NLP.println("1 sIdx=" ,
						// eigthBehindSidx
						// + " eIdx"
						// + (eigthBehindSidx + definition.length() - 1)
						// + "\rdefinition=" + definition));
					}

					cnt = 0;
				}

				// if above fails - I need to look back from current defined
				// term to see if 6 of 8 prior defined terms are alphabetical.
				// Because there could be some overlap - use a map to record the
				// defined terms that are confirmed in this matter using sIdx
				// and if there are duplicates they won't be recorded twice.
				// Now I check if current definedTerm has at least 6 prior
				// definedTerms in alphabetical order - this allows for last 4
				// or 5 definedTerms to be pickedup.

				if (i > 5) {
					cnt = 0;

					if ((ltr >= nextBehindLtr && nextBehindLtr > 0) || (ltr2 >= nextBehindLtr2 && nextBehindLtr2 > 0)
							|| (ltr3 >= nextBehindLtr3 && nextBehindLtr3 > 0))
						cnt++;
					if ((nextBehindLtr >= twoBehindLtr && twoBehindLtr > 0)
							|| (nextBehindLtr2 >= twoBehindLtr2 && twoBehindLtr2 > 0)
							|| (nextBehindLtr3 >= twoBehindLtr3 && twoBehindLtr3 > 0))
						cnt++;
					if ((twoBehindLtr >= threeBehindLtr && threeBehindLtr > 0)
							|| (twoBehindLtr2 >= threeBehindLtr2 && threeBehindLtr2 > 0)
							|| (twoBehindLtr3 >= threeBehindLtr3 && threeBehindLtr3 > 0))
						cnt++;
					if ((threeBehindLtr >= fourBehindLtr && fourBehindLtr > 0)
							|| (threeBehindLtr2 >= fourBehindLtr2 && fourBehindLtr2 > 0)
							|| (threeBehindLtr3 >= fourBehindLtr3 && fourBehindLtr3 > 0))
						cnt++;
					if ((fourBehindLtr >= fiveBehindLtr && fiveBehindLtr > 0)
							|| (fourBehindLtr2 >= fiveBehindLtr2 && fiveBehindLtr2 > 0)
							|| (fourBehindLtr3 >= fiveBehindLtr3 && fiveBehindLtr3 > 0))
						cnt++;
					if ((fiveBehindLtr >= sixBehindLtr && sixBehindLtr > 0)
							|| (fiveBehindLtr2 >= sixBehindLtr2 && sixBehindLtr2 > 0)
							|| (fiveBehindLtr3 >= sixBehindLtr3 && sixBehindLtr3 > 0))
						cnt++;
					if ((sixBehindLtr >= sevenBehindLtr && sevenBehindLtr > 0)
							|| (sixBehindLtr2 >= sevenBehindLtr2 && sevenBehindLtr2 > 0)
							|| (sixBehindLtr3 >= sevenBehindLtr3 && sevenBehindLtr3 > 0))
						cnt++;
					if ((sevenBehindLtr >= eightBehindLtr && eightBehindLtr > 0)
							|| (sevenBehindLtr2 >= eightBehindLtr2 && eightBehindLtr2 > 0)
							|| (sevenBehindLtr3 >= eightBehindLtr3 && eightBehindLtr3 > 0))
						cnt++;

					if (cnt > 5 && (ltr >= nextBehindLtr && ltr - nextBehindLtr < 2
							|| (ltr2 >= nextBehindLtr2 && ltr2 - nextBehindLtr2 < 2)
							|| (ltr3 >= nextBehindLtr3 && ltr3 - nextBehindLtr3 < 2))) {
						// add filter here to determine if this is the last
						// defined term sequence by seeing if next defined term
						// is out of order or more than 2 ltr ahead of more than
						// an excessive number of characters - or at end of
						// list.

						oneAheadFirstLetter = oneDefinedTermAhead.replaceAll("[\"\r\n ]", "").substring(0, 1);
						oneAheadLtr = getNumberInAlphabet(oneAheadFirstLetter);
						// //NLP.pwNLP.append(NLP.println("ltr=",ltr+" oneAheadLtr="+oneAheadLtr));

						oneAheadFirstLetter2 = oneDefinedTermAhead.replaceAll("[\"\r\n ]", "").substring(0, 1);
						oneAheadLtr2 = getNumberInAlphabet(oneAheadFirstLetter);
						oneAheadFirstLetter3 = oneDefinedTermAhead.replaceAll("[\"\r\n ]", "").substring(0, 1);
						oneAheadLtr3 = getNumberInAlphabet(oneAheadFirstLetter);

						/*
						 * if (listDefinedWordsInDefinitionArticle.size() == i + 1 || oneAheadLtr < ltr
						 * || Math.abs(oneAheadLtr - ltr) > 3) { //
						 * //NLP.pwNLP.append(NLP.println("get last def--"); definition =
						 * getLastDefinition(text, sIdx, definedTerm);
						 * 
						 * String[] strAry2 = { "sIdx", "definedTerm", definedTerm.replaceAll(
						 * "[\r\n]([ ]{1,})?|(: ?)", "") }; if (definedTerm.replaceAll(
						 * "[\r\n]([ ]{1,})?|(: ?)", "").length() > 3) { mapOfContractIdxLocs.put(sIdx,
						 * strAry2); String[] strAry3 = { "eIdx", "" }; mapOfContractIdxLocs.put( (sIdx
						 * , definition.length() - 1), strAry3));
						 * //NLP.pwNLP.append(NLP.println("2 sIdx=" , sIdx + " eIdx" + (sIdx +
						 * definition.length() - 1)+" oneAheadLtr="+oneAheadLtr + "\rdefinition=" +
						 * definition)); } }
						 */
						if (listDefinedWordsInDefinitionArticle.size() > i + 1 && oneAheadLtr >= ltr
								&& Math.abs(oneAheadLtr - ltr) <= 3) {

							definition = text.substring(sIdx,
									Integer.parseInt(listDefinedWordsInDefinitionArticle.get(i + 1)[0]));

							String[] strAry2 = { "sIdx", "definedTerm",
									definedTerm.replaceAll("[\r\n]([ ]{1,})?|(: ?)", "") };

							if (definedTerm.replaceAll("[\r\n]([ ]{1,})?|(: ?)", "").length() > 3) {

								mapOfContractIdxLocs.put(sIdx, strAry2);
								String[] strAry3 = { "eIdx", "" };
								mapOfContractIdxLocs.put((sIdx + definition.length() - 1), strAry3);
								// //NLP.pwNLP.append(NLP.println("3 sIdx=" , sIdx
								// + " eIdx"
								// + (sIdx + definition.length() - 1)
								// + "\rdefinition=" + definition));
							}
						}
					}
				}

				eigthBehindSidx = sevenBehindSidx;
				sevenBehindSidx = sixBehindSidx;
				sixBehindSidx = fiveBehindSidx;
				fiveBehindSidx = fourBehindSidx;
				fourBehindSidx = threeBehindSidx;
				threeBehindSidx = twoBehindSidx;
				twoBehindSidx = nextBehindSidx;
				nextBehindSidx = sIdx;

				eightBehindLtr = sevenBehindLtr;
				sevenBehindLtr = sixBehindLtr;
				sixBehindLtr = fiveBehindLtr;
				fiveBehindLtr = fourBehindLtr;
				fourBehindLtr = threeBehindLtr;
				threeBehindLtr = twoBehindLtr;
				twoBehindLtr = nextBehindLtr;
				nextBehindLtr = ltr;

				eightBehindLtr2 = sevenBehindLtr2;
				sevenBehindLtr2 = sixBehindLtr2;
				sixBehindLtr2 = fiveBehindLtr2;
				fiveBehindLtr2 = fourBehindLtr2;
				fourBehindLtr2 = threeBehindLtr2;
				threeBehindLtr2 = twoBehindLtr2;
				twoBehindLtr2 = nextBehindLtr2;
				nextBehindLtr2 = ltr2;

				eightBehindLtr3 = sevenBehindLtr3;
				sevenBehindLtr3 = sixBehindLtr3;
				sixBehindLtr3 = fiveBehindLtr3;
				fiveBehindLtr3 = fourBehindLtr3;
				fourBehindLtr3 = threeBehindLtr3;
				threeBehindLtr3 = twoBehindLtr3;
				twoBehindLtr3 = nextBehindLtr3;
				nextBehindLtr3 = ltr3;

				eightBehindDefinedTerm = sevenBehindDefinedTerm;
				sevenBehindDefinedTerm = sixBehindDefinedTerm;
				sixBehindDefinedTerm = fiveBehindDefinedTerm;
				fiveBehindDefinedTerm = fourBehindDefinedTerm;
				fourBehindDefinedTerm = threeBehindDefinedTerm;
				threeBehindDefinedTerm = twoBehindDefinedTerm;
				twoBehindDefinedTerm = nextBehindDefinedTerm;
				nextBehindDefinedTerm = definedTerm;
			}
		}
	}

	public static int lastDefinedTermFinder(String text, int sIdx) {
		NLP nlp = new NLP();

		// //NLP.pwNLP.append(NLP.println(
		// "find end of definition section pattern. text.snip",
		// text.substring(0, Math.min(300, text.length())) + ""));

		Integer idx = -1;
		List<Integer> list = nlp.getAllIndexStartLocations(text,
				Pattern.compile("(?sm)[\r\n]{1,5} ? ?(Section|SECTION|Article|ARTICLE)"));
		// NLP.pwNLP.append(NLP.println("found end of definition pattern. list.size",
		// list.size()+""));
		// not being found
		if (list.size() > 0) {
			idx = list.get(0);
		}

		return idx;
	}

	public static int lastSectionFinder(String text, int sIdx) throws IOException {
		NLP nlp = new NLP();

		// //NLP.pwNLP.append(NLP.println(
		// "find last section pattern. text.snip",
		// text.substring(0, Math.min(300, text.length())) + ""));

//		IN WITNESS WHEREOF, the parties have executed this Indenture as of the date first written above.
//		Signatures on following pages

		// is it getting above?
		Pattern patternClosingPara = Pattern
				.compile("(?ism)(WITNESS WHEREOF.{1,60}(executed|set.{1,4}their.{1,4}hand).{1,70}\\.)"
						+ "|\\|?Signatures?.{1,6}((follow[ing]{0,3}.{1,5}pages?)|(pages? follow))\\|?"
						+ "|Signatures?.{1,6}following page");

		Integer idx = -1;
		List<Integer> list = nlp.getAllIndexStartLocations(text, patternClosingPara);

		List<String> listStr = nlp.getAllMatchedGroups(text, patternClosingPara);
//		System.out.println("found end of last section pattern. list.size="+ list.size()+"");
//		not being found
		if (list.size() > 0) {

//			//NLP.pwNLP.append(NLP.println("end idx of text returned=", (list.get(0)+sIdx)+""));
//			System.out.println("a last sec="+listStr.get(0)+"|END");

			return list.get(0);

		} else {
			return idx;
		}
	}

	public static String getLastDefinition(String text, Integer sIdx, String definedTerm, String type, int curListNo,
			int listSize) throws IOException {

		StringBuffer sb = new StringBuffer();
		NLP nlp = new NLP();
		// specific end of last paragraph patterns
		String prelLast = "", line = "";
		int idxEnd = 0;
		// create for exh,sec and def types.

//		 System.out.println(
//		  " find end of definition secttion pattern. text.snip"+
//		 text.substring(sIdx, Math.min(sIdx + 20, text.length()))
//		 + "|end snip");

		if (type.equals("sec") && text.length() > sIdx) {

			idxEnd = lastSectionFinder(text.substring(sIdx), sIdx);

//			 System.out.println("idxEnd="+idxEnd+" listSize="+listSize+" curListNo="+curListNo+" sIdx="+sIdx);

			if (listSize - curListNo < 3 && idxEnd < 3000) {
				// if near end of sec hdg list and and end of K (idxEnd found by
				// lastSectionFinder which gets end of K by finding closing
				// paragraph)

				if (idxEnd > 0) {
					// System.out.println("lastDefinedTermFinder - type is sec - returned idxEnd="
					// + idxEnd + " sIdx=" + sIdx);
					// //NLP.pwNLP.append(NLP.println(
					// "lastDefinedTermFinder type is sec idxEnd=", idxEnd
					// + " sIdx=" + sIdx)
					// + " def=" + definedTerm);
					prelLast = text.substring(sIdx, sIdx + idxEnd);
					// //NLP.pwNLP.append(NLP.println(
					// "section preLast==",prelLast+"||end"));
				}
			} else {

				idxEnd = lastDefinedTermFinder(text.substring(sIdx), sIdx);

				if (idxEnd > 0) {
					// System.out.println("lastDefinedTermFinder returned idxEnd="
					// + idxEnd + " sIdx=" + sIdx);
					// NLP.pwNLP
					// .append(NLP
					// .println(
					// "lastDefinedTermFinder type is def idxEnd=",
					// idxEnd + " sIdx=" + sIdx)+" def="+definedTerm);
					prelLast = text.substring(sIdx, sIdx + idxEnd);

				}

			}
		}

		if (text.length() > sIdx && (type.equals("def") || (idxEnd < 1 && type.equals("sec")))) {

			// if def - end determinant is a little different
			idxEnd = lastDefinedTermFinder(text.substring(sIdx), sIdx);
//			 System.out.println("lastDefinedTermFinder returned idxEnd="
//			 + idxEnd + " sIdx=" + sIdx);

			if (idxEnd > 0) {
				// NLP.pwNLP
				// .append(NLP
				// .println(
				// "lastDefinedTermFinder type is def idxEnd=",
				// idxEnd + " sIdx=" + sIdx)+" def="+definedTerm);
				prelLast = text.substring(sIdx, sIdx + idxEnd);

			}
		}

		if (sIdx > text.length())
			return "";
		String[] splitText = text.substring(sIdx).split("[\r\n]");
		// //NLP.pwNLP.append(NLP.println("splitText.len=", splitText.length + ""));

		boolean foundText = false;
		if (splitText.length < 1) {
			return "";
		}

		// find what start idx of line after definition - and any line
		// thereafter with different start idx then end at prior line.

		int cnt = 0, idx = -1, idxLine = -1;
		for (int i = 0; i < splitText.length; i++) {
			// start at 1 - skip 1st line
			line = splitText[i];
			// System.out.println(" line=="+ line);
			if (i == 0) {
				sb.append(line + "\r\n");
				continue;
			}
			if (i == 1)
				sb.append(line + "\r\n");

			if (line.replaceAll("[ \r\n\t]", "").length() > 5) {
				foundText = true;
				// //NLP.pwNLP.append(NLP.println("foundText=", foundText +
				// " cnt="
				// + cnt));
			}

			if (foundText && line.replaceAll("[ \r\n\t]", "").length() == 0) {
				cnt++;
//				System.out.println("cnt=="+ cnt + " foundText="+ foundText + " blank line=" + line);
			}

			if (!foundText || line.replaceAll("[ \r\n\t]", "").length() > 2) {
				cnt = 0;
//				System.out.println("reset cnt=0" +cnt +
//				 " foundText="
//					+ foundText + " line=" + line);
			}

			if (line.replaceAll("[\r\n \t]", "").length() == 0)
				continue;

			if ((i > 3 && line.replaceAll("[ \r\n\t]", "").length() == 0 && cnt > 2 && foundText && idx != -1)

					|| line.toLowerCase().contains("   article")

					|| (line.toLowerCase().contains("   section") && line.contains(definedTerm))

					|| (type.equals("def") && i > 0)) {

//				System.out.println(
//				 "get last def - found article|section? preceded by 3 ws or 3 lines in a row with no text.\rNot taking this line=" + line
//				 );

				break;
			}

			if (i > 0 && idx == -1
					&& nlp.getAllIndexStartLocations(splitText[i], nlp.patternAnyVisualCharacter).size() > 0
					&& !line.contains(definedTerm.replaceAll("[\r\n]", ""))) {
				idx = nlp.getAllIndexStartLocations(splitText[i], nlp.patternAnyVisualCharacter).get(0);
//				System.out.println("idx="+ idx + " definedTerm="
//				 + definedTerm + " line=" + line);
			}

			if (idx >= 0 && nlp.getAllIndexStartLocations(splitText[i], nlp.patternAnyVisualCharacter).size() > 0) {
				idxLine = nlp.getAllIndexStartLocations(splitText[i], nlp.patternAnyVisualCharacter).get(0);
			}

			// if next line starting idx (idxLine) is zero - that indicates
			// paragraph is continuing.
			if (idx != idxLine && idx >= 0 && idxLine > 0) {

//				System.out.println( "get last def break - end of para - idx!=idxLine. idx="+ idx
//				 + " idxLine=" + idxLine + "\rnot taking this line=" + line);
				break;
			} else {
				sb.append(line + "\r\n");
			}
		}

		// if preLast has text - it means an idx value was found using an ending
		// pattern that is prior to the line-by-line method above
		if (sb.toString().length() > prelLast.length() && prelLast.length() > 5) {
//			System.out.println("preLast="+prelLast);
			return prelLast;

		} else {
//			System.out.println("prelLast sb.toString="+sb.toString());
			return sb.toString();

		}

	}

	public static void getExhibitIdxAndNames(String text) throws IOException {
		System.out.println("getting exhibits");
		String tmpToc = toc.replaceAll("[ ]{2,}", " ");

		ContractNLP nlpK = new ContractNLP();
		// StringBuffer sb = new StringBuffer();

		String exhibits = null;

		int contractLength = text.length();
		listOfExhibitsIdxsAndNames.clear();

		// get prelim list - then ck each line follows exhibit for up to 2
		// lines that have text - check each line that has text and see if all
		// words > 5 chars are initial caps and if not - it is not an exhibit.

		List<String[]> prelimIdxExh = nlpK.getAllStartIdxLocsAndMatchedGroups(text, patternExhibitInContract);
		// NLP.pwNLP.append(NLP.println("exhibit list size=" ,
//		 prelimIdxExh.size()+""));
		int preStartIdx, cnt = 0;
		String exhibitMatch = null, exhSnip, firstLineStr = null, secondLineStr = null;
		String[] lines, words;
		boolean firstLine = true, foundExhibit = false, secondLine = true, skip = false;
		for (int i = 0; i < prelimIdxExh.size(); i++) {
			firstLine = true;
			secondLine = true;
			skip = false;

			cnt = 0;
			exhibitMatch = prelimIdxExh.get(i)[1];
			// //NLP.pwNLP.append(NLP.println("prelim exhibitMatch=" ,
			// exhibitMatch));
			preStartIdx = Integer.parseInt(prelimIdxExh.get(i)[0]) + exhibitMatch.length();
			// //NLP.pwNLP.append(NLP.println("exhibit.sub="
			// , text.substring(Integer.parseInt(prelimIdxExh.get(i)[0]),
			// Integer.parseInt(prelimIdxExh.get(i)[0]) + 100)));
			exhSnip = text.substring(preStartIdx, Math.min(preStartIdx + 400, text.length()));
			lines = exhSnip.split("[\r\n]");

			Matcher matchFalsePostiveExhibit;
			for (int a = 0; a < lines.length; a++) {
				if (skip)
					break;
				if (lines[a].replaceAll("[\\p{Punct} ]", "").length() > 1) {
					cnt++;
					matchFalsePostiveExhibit = patternPAGE_PRE_S_C.matcher(lines[a]);
					if (matchFalsePostiveExhibit.find() && cnt < 2 && !skip) {
						// //NLP.pwNLP.append(NLP.println("exclude this exhibit="
						// , lines[a]
						// + " exhibitMatch=" + exhibitMatch));
						skip = true;
						break;
					}
				}
			}

			if (skip)
				continue;

			cnt = 0;
			// //NLP.pwNLP.append(NLP.println("exhibitMatch==?" , exhibitMatch));
			for (int c = 0; c < lines.length; c++) {
				if (cnt > 1)
					break;
				// //NLP.pwNLP.append(NLP.println("line.repl=",lines[c].replaceAll("(<PAGE>|<S>|<C>|<PRE>)",
				// "").replaceAll("[a-z\\p{Punct} \\[\\]_]", "")));

				if (lines[c].replaceAll("(<PAGE>|<S>|<C>|<PRE>)", "").replaceAll("[a-z\\p{Punct} \\[\\]_]", "")
						.length() > 1) {
					cnt++;

					words = lines[c].replaceAll("[ ]{2,}", " ").split(" ");

					for (int n = 0; n < words.length; n++) {
						// //NLP.pwNLP.append(NLP.println("words[n].len=",words[n]));

						if (words[n].replaceAll("[\\[\\]\\%_\\p{Punct}\\(\\)]{2,}", "").length() > 4
								&& words[n].replaceAll("[a-z\\p{Punct}]", "").length() < 1) {
							if (cnt == 1) {
								// //NLP.pwNLP.append(NLP.println(" cnt="
								// , cnt
								// + " words[n]="
								// + words[n]
								// + " words[n]-repl="
								// + words[n].replaceAll(
								// "[a-z\\p{Punct}]", "")));
								firstLine = false;
								// //NLP.pwNLP.append(NLP.println("firstLine=false");
								break;
							}
							if (cnt == 2) {
								// secondLine = false;
								// //NLP.pwNLP.append(NLP.println("secondLine=false");
							}
						}

						else if (cnt == 1) {
							firstLineStr = lines[c];
							firstLine = true;
							// //NLP.pwNLP.append(NLP.println("1 firstLineStr=" ,
							// firstLineStr));
						} else if (cnt == 2) {
							secondLineStr = lines[c];
							// //NLP.pwNLP.append(NLP.println("1 secondLineStr=" ,
							// secondLineStr));
							secondLine = true;
						}
					}
				}
				if (cnt == 1 && !firstLine)
					continue;
			}

			// NLP.pwNLP.append(NLP.println(
//					"out of loop to get exhibit name and 1st 2 lines cnt=", cnt
//							+ ""));

			if (cnt == 2 && firstLine && secondLine) {
				String[] ary = { prelimIdxExh.get(i)[0],
						prelimIdxExh.get(i)[1] + "\r" + firstLineStr + "\r" + secondLineStr };
				listOfExhibitsIdxsAndNames.add(ary);
				// //NLP.pwNLP.append(NLP.println("1 add ary - prelimExh="
				// , Arrays.toString(ary)));

			}
			if (cnt == 2 && firstLine && !secondLine) {
				String[] ary = { prelimIdxExh.get(i)[0], prelimIdxExh.get(i)[1] + "\r" + firstLineStr };
				listOfExhibitsIdxsAndNames.add(ary);
				// //NLP.pwNLP.append(NLP.println("2 add ary - prelimExh="
				// , Arrays.toString(ary)));

			}
		}

		// if less than 2 idxs found - skip

		int startIdx, nextStartIdx = 0;
		String lastMach, exhibitName;

		// //NLP.pwNLP.append(NLP.println("listOfExhibitsIdxsAndNames.size="
		// , listOfExhibitsIdxsAndNames.size()));

		Pattern patternExhibit = Pattern.compile("(?i)(EXHIBIT|ANNEX|APPENDIX|SCHEDULE) [A-Za-z\\d\\-\\_]{1,6}");

		String exhMatchLast = null;
		// loop of list of exhibits to determine each.
		NLP.printListOfStringArray("listOfExhibitsIdxsAndNames=", listOfExhibitsIdxsAndNames);
		int cntExh = 0;
		if (listOfExhibitsIdxsAndNames.size() > 0) {
			for (int i = 0; i < listOfExhibitsIdxsAndNames.size(); i++) {
				startIdx = Integer.parseInt(listOfExhibitsIdxsAndNames.get(i)[0]);

				// below is solely to grab last exhibit long name - otherwise it
				// stops at 1 before last while getting just the nextStartIdx of
				// next exhibit as an endIdx. Have to fetch at i+2 b/c at that
				// point I end in the loop by putting into mapOfContractIdxLocs
				// last two exhibits
				if (i + 2 == listOfExhibitsIdxsAndNames.size()) {
					String[] exhLinesLast = listOfExhibitsIdxsAndNames.get(i + 1)[1].split("\r\n");

					Matcher matchExhLast;
					for (int b = 0; b < Math.min(3, exhLinesLast.length); b++) {
						matchExhLast = patternExhibit.matcher(exhLinesLast[b]);

						if (matchExhLast.find()) {
							exhMatchLast = matchExhLast.group().replaceAll("[ ]{2,}", " ").trim();
							// NLP.pwNLP.append(NLP.println("aa exhMatchLast=",
//									exhMatchLast));
							Pattern patternMatchedExhibit = Pattern.compile("(?i)" + exhMatchLast + ".*$");
							// finds exhibit pattern found in body in tmpToc
							matchExhLast = patternMatchedExhibit.matcher(tmpToc);
							if (matchExhLast.find()) {
								exhMatchLast = matchExhLast.group();
								// NLP.pwNLP.append(NLP.println(
//										"found in toc exhMatchLast=",
//										exhMatchLast));
							}
						}
					}
				}

				// remove condition that size must be at least 2. if size is 1
				// set nextStartIdx to end of idx based on last section logic.
				if ((i + 1) < listOfExhibitsIdxsAndNames.size()) {
					nextStartIdx = Integer.parseInt(listOfExhibitsIdxsAndNames.get(i + 1)[0]);
					// } else if (i < listOfExhibitsIdxsAndNames.size()) {

					// NLP.pwNLP.append(NLP.println("aa exhibit=",
//							listOfExhibitsIdxsAndNames.get(i)[1].replaceAll(
//									"[\r\n]|[ ]{2,}", " ")));

					// get first line of exhibit - and match it to toc long
					// name:
					String[] exhLines = listOfExhibitsIdxsAndNames.get(i)[1].split("\r\n");
					String exhMatch = null;
					Matcher matchExh;
					for (int b = 0; b < Math.min(3, exhLines.length); b++) {
						matchExh = patternExhibit.matcher(exhLines[b]);

						// TODO: finds exhibit pattern found in body in tmpToc
						if (matchExh.find()) {

							exhMatch = matchExh.group().replaceAll("[ ]{2,}", " ");
							// NLP.pwNLP.append(NLP.println("aa exhMatch=",
//									exhMatch));

							Pattern patternMatchedExhibit = Pattern.compile("(?i)" + exhMatch + ".+[\r\n]");
							matchExh = patternMatchedExhibit.matcher(tmpToc);
							if (matchExh.find()) {
								exhMatch = matchExh.group();
								// NLP.pwNLP.append(NLP.println("exhMatchToc=",
//										exhMatch));
							}
						}
					}

					foundExhibit = false;
					if (text.substring(startIdx, nextStartIdx).replaceAll("[ \r\n\t]{1,}", "").length() > 2
							&& listOfExhibitsIdxsAndNames.size() != i + 2) {

						exhMatch = exhMatch.replaceAll("[\r\n]([ ]{1,})?|(: )", "");
						String[] strAry2 = { "sIdx", "exhibitHeading", exhMatch };
						mapOfContractIdxLocs.put(startIdx, strAry2);

						cntExh++;
						if (cntExh == 1) {
							exhStartIdx = startIdx;
							// NLP.pwNLP.append("exhStartIdx="+exhStartIdx);
						}
						// use above so that when cntExh=1 it is first and I can
						// capture exhStartIdx

						String[] strAry3 = { "eIdx", "" };
						mapOfContractIdxLocs.put(nextStartIdx - 1, strAry3);

						// NLP.pwNLP.append(NLP.println(
//								"1 exhMatch=",
//								exhMatch
//										+ " exh sIdx="
//										+ (startIdx)
//										+ " exh eIdx="
//										+ nextStartIdx
//										+ "\r1 exhibit substr="
//										+ text.substring(startIdx,
//												startIdx + 150) + "||end"));
						foundExhibit = true;
					} else if (!foundExhibit && listOfExhibitsIdxsAndNames.size() != i + 2
							&& text.substring(startIdx, nextStartIdx).replaceAll("[ \r\n\t]{1,}", "").length() < 2) {

						exhMatch = exhMatch.replaceAll("[\r\n]([ ]{1,})?|(: )", "");

						String[] strAry4 = { "sIdx", "exhibitHeading", exhMatch };

						cntExh++;
						if (cntExh == 1) {
							exhStartIdx = startIdx;
							// NLP.pwNLP.append("exhStartIdx="+exhStartIdx);
						}

						mapOfContractIdxLocs.put(startIdx, strAry4);
						String[] strAry5 = { "eIdx", "" };
						mapOfContractIdxLocs.put((Math.min(startIdx + 2000, text.length() - 1)), strAry5);
						// NLP.pwNLP.append(NLP.println(
//								"2 exhMatch=",
//								exhMatch
//										+ " exh sIdx="
//										+ (startIdx)
//										+ " exh eIdx="
//										+ nextStartIdx
//										+ "\r2 exhibit substr="
//										+ text.substring(
//												startIdx - 1,
//												(Math.min(startIdx + 150,
//														text.length())))
//										+ "||end"));
					}

					// NLP.pwNLP.append(NLP.println("last exh?", exhMatch + " i="
//							+ i + " listOfExhibitsIdxsAndNames.siz="
//							+ listOfExhibitsIdxsAndNames.size()));

					if ((i + 2) == listOfExhibitsIdxsAndNames.size()) {

						exhMatch = exhMatch.replaceAll("[\r\n]([ ]{1,})?|(: )", "");
						String[] strAry2 = { "sIdx", "exhibitHeading", exhMatch };
						mapOfContractIdxLocs.put(startIdx, strAry2);
						String[] strAry3 = { "eIdx", "" };
						mapOfContractIdxLocs.put(nextStartIdx - 1, strAry3);
						// NLP.pwNLP.append(NLP.println(
//								"3 exhMatch=",
//								exhMatch
//										+ "3 exhibit substr="
//										+ text.substring(startIdx,
//												startIdx + 150) + " exh sIdx="
//										+ startIdx + " exh eIdx="
//										+ nextStartIdx));

						startIdx = nextStartIdx + 1;
						if (((double) nextStartIdx / (double) text.length()) > .85) {
							nextStartIdx = text.length();
						} else {
							nextStartIdx = Math.min(startIdx + 100, text.length() - 1);
						}

						exhMatchLast = exhMatchLast.replaceAll("[\r\n]([ ]{1,})?|(: )", "");
						String[] strAry6 = { "sIdx", "exhibitHeading", exhMatchLast };
						mapOfContractIdxLocs.put(startIdx, strAry6);
						String[] strAry7 = { "eIdx", "" };
						mapOfContractIdxLocs.put(nextStartIdx, strAry7);
						// NLP.pwNLP.append(NLP.println(
//								"4 exhMatch=",
//								exhMatchLast
//										+ "4 exhibit substr="
//										+ text.substring(startIdx,
//												startIdx + 150) + " exh sIdx="
//										+ startIdx + " exh eIdx="
//										+ nextStartIdx));

					}
				}
			}
		}

		exhEndIdx = nextStartIdx;
//		System.out.println("exhEndIdx="+text.length()+ " exhStartIdx="+exhStartIdx);
	}

	/*
	 * public static void getSectionIdxsAndNames(String text, boolean exhibit,
	 * String exhibitName) throws IOException {
	 * 
	 * // secStartIdx = null; secEndIdx = null;
	 * 
	 * // CANNOT CHANGE LENGTH OF TEXT BECAUSE WE NEED TO BACKFILL GAPS WITH // SOLR
	 * FIELDS AND IF WE CHANGE TEXT LENGTH I CORRUPT THE ABILITY TO // MAKE SOLR
	 * FIELDS FOR TEXT NOT CAPTURED BY SECTION RULESET. SO NO // REPLACES
	 * 
	 * // counts how many times the same distance between two sections occur. // If
	 * sections are 2.01, 2.02, 2.03 etc - distance of 1 to two decimal // places
	 * will occur quite often as measured by value. Key is the // distance, value[0]
	 * is number of times and [1]=decimal places
	 * 
	 * ContractNLP nlpK = new ContractNLP(); List<String[]> listPrelimIdxAndSection
	 * = nlpK .getAllIdxLocsAndMatchedGroups(text,
	 * patternSectionHeadingRestrictive);
	 * 
	 * List<String[]> listIdxAndSection = new ArrayList<>();
	 * 
	 * String sectSubStr, sectionHdg = null, idxStr = null, sectionHdgToc = null;
	 * String lastSection = null; String[] sectionNameWords; int idx = -1; int
	 * firstDecStrBeforeDecimal = 0, firstDecStrAfterDecimal = 0,
	 * nextDecStrBeforeDecimal = 0, nextDecStrAfterDecimal = 0;
	 * 
	 * 
	 * Filter used to get sectionHeading: sectionHeadingpattern requires
	 * sectionHeading be preceded by a hard return. Then I require 1st word in
	 * section name is Initial Caps and all words that have more than 5 characters
	 * on line in sectionHeading be Initial Caps (this captures what is generally
	 * the section heading format. This isn't going to be 100% - but its the best we
	 * can do. Some section headings may just have first word be initial caps). The
	 * above will then be all section headings. Then I need to determine the end of
	 * section text - if the next section is in sequence (e.g., 1.02 to 1.03) -- the
	 * section text is from the start of current section to start of next. If next
	 * section is not in sequence - I will capture all text that meet paragraph
	 * style conditions provided it ends before next section that is out of
	 * sequence.
	 * 
	 * 
	 * Pattern patternSectionNumber = Pattern
	 * .compile("[\\d]{1,3}[\\.]{0,1}[\\d]{0,2}(?=[\\. ])"); Matcher match = null;
	 * String secNumber; if (null == tocStartIdx) tocStartIdx = -1; if (null ==
	 * tocEndIdx) tocEndIdx = -1;
	 * 
	 * String firstDecStr = "";
	 * 
	 * // See if I'm getting only Section Headings that are actually matches. //
	 * Weed out non-matches below. for (int i = 0; i <
	 * listPrelimIdxAndSection.size(); i++) { idxStr =
	 * listPrelimIdxAndSection.get(i)[0]; idx = Integer.parseInt(idxStr); if (idx >=
	 * tocStartIdx && idx <= tocEndIdx) continue;
	 * 
	 * // //NLP.pwNLP.append(NLP.println("pattern - Sections=" // ,
	 * listPrelimIdxAndSection.get(i)[1])); }
	 * 
	 * boolean foundTocSecHdg = false; for (int i = 0; i <
	 * listPrelimIdxAndSection.size(); i++) { idxStr =
	 * listPrelimIdxAndSection.get(i)[0]; idx = Integer.parseInt(idxStr); //
	 * //NLP.pwNLP.append(NLP.println("Section===========" // ,
	 * listPrelimIdxAndSection.get(i)[1] + " tocStartIdx=" // + tocStartIdx +
	 * " tocEndIdx=" + tocEndIdx));
	 * 
	 * if (idx >= tocStartIdx && idx <= tocEndIdx) continue; //
	 * //NLP.pwNLP.append(NLP.println("Section===========" // ,
	 * listPrelimIdxAndSection.get(i)[1] + " tocStartIdx=" // + tocStartIdx +
	 * " tocEndIdx=" + tocEndIdx));
	 * 
	 * // gets rid of "Section #" and gets just sec name. sectionHdg =
	 * listPrelimIdxAndSection.get(i)[1]; //
	 * //NLP.pwNLP.append(NLP.println("aaa sectionHdg=" , sectionHdg + // "|end"));
	 * String sectionName = sectionHdg .trim()
	 * .replaceAll("^S(?i)ection.*?[\\da-zA-Z].*?(?=[a-zA-Z\\(])",
	 * "").replaceAll("[ ]{2,}", " ").trim();
	 * 
	 * // //NLP.pwNLP.append(NLP.println("sectionName==" , sectionName)); sectSubStr
	 * = sectionName.replaceAll(" ", "");
	 * 
	 * String sectionHdgtoFind = sectionHdg.replaceAll( "[\r\n\t\\(\\)]{1,}",
	 * "").replaceAll("[ ]{2,}", " "); //
	 * //NLP.pwNLP.append(NLP.println("secSubStr=" , sectSubStr + //
	 * " sectionHdgtoFind=" // + sectionHdgtoFind)); List<String[]> listSecHdgToFind
	 * = new ArrayList<String[]>(); foundTocSecHdg = false; if (null !=
	 * sectionHdgtoFind && null != tocHeading && sectionHdgtoFind.length() > 2 &&
	 * tocHeading.length() > 2) { listSecHdgToFind =
	 * nlpK.getAllIdxLocsAndMatchedGroups( tocHeading, Pattern.compile("((?is)" +
	 * sectionHdgtoFind + ")")); if (listSecHdgToFind.size() > 0) { foundTocSecHdg =
	 * true; } }
	 * 
	 * if (foundTocSecHdg)
	 * 
	 * { // //NLP.pwNLP.append(NLP.println("sectionHdgtoFind was found. It is="
	 * ,listSecHdgToFind.get(0)[1])); int idxToc =
	 * Integer.parseInt(listSecHdgToFind.get(0)[0]); String tocAfterIdx =
	 * tocHeading.substring(idxToc); //
	 * //NLP.pwNLP.append(NLP.println("tocAfterIdx=" , tocAfterIdx)); if
	 * (tocAfterIdx.indexOf("\r") > 0) { sectionHdgToc = tocAfterIdx.substring(0,
	 * tocAfterIdx.indexOf("\r")); }
	 * 
	 * // //NLP.pwNLP.append(NLP.println("sectionHdgToc=" , sectionHdgToc // +
	 * "\rsectionHdg=" + sectionHdg)); if (sectionHdgToc.replaceAll("[\r\n\t]",
	 * "").length() > sectionHdg .replaceAll("[\r\n\t]", "").length()) { sectionHdg
	 * = sectionHdgToc; } }
	 * 
	 * // //NLP.pwNLP.append(NLP.println("sectionHdgToc to sectionHdg=" , //
	 * sectionHdg // + " foundTocSecHdg=" + foundTocSecHdg + " secSubStr=" // +
	 * sectSubStr));
	 * 
	 * // find in toc secHdgs where tocSecHdgs contains what was found in // body -
	 * and if shorter use toc Heading. // break when sections match - need to //
	 * conform section hdg in body to replace spaces and decimals w/ // underscore
	 * so it compare to toc_heading.
	 * 
	 * // if no word after "Section if (sectSubStr.length() < 1) continue;
	 * 
	 * // if 1st ltr not initial cap (cap) continue if
	 * (!Character.isUpperCase(sectSubStr.charAt(0))) continue; // if sec name has
	 * word w/ 6 chars & its not initial caps: skip // get just sectionName ==>
	 * 
	 * sectionNameWords = sectionName.split(" "); boolean isCap = true;
	 * 
	 * for (int c = 0; c < sectionNameWords.length; c++) { if
	 * (sectionNameWords[c].replaceAll("between", "").length() > 5 && !Character
	 * .isUpperCase(sectionNameWords[c].charAt(0))) { isCap = false; //
	 * //NLP.pwNLP.append(NLP.println("isCap=" , isCap + // " foundTocSecHdg=" // +
	 * foundTocSecHdg)); break; } } if (!isCap) continue;
	 * 
	 * secNumber = null; match = patternSectionNumber.matcher(sectionHdg); if
	 * (match.find()) { //NLP.pwNLP.append(NLP.println("matchGroup=", match.group()
	 * + " sechdg=" + sectionHdg)); secNumber = match.group().replaceAll("[\\.]",
	 * "").trim(); //NLP.pwNLP.append(NLP.println("a firstDecStr is secNumber=",
	 * secNumber)); //NLP.pwNLP.append(NLP.println("a firstDecStr=", firstDecStr));
	 * firstDecStr = match.group().replaceAll("[ \t\r\n]", "").trim();
	 * //NLP.pwNLP.append(NLP.println("b firstDecStr=", firstDecStr)); if
	 * (firstDecStr.split("\\.").length > 1) { firstDecStrBeforeDecimal =
	 * Integer.parseInt(firstDecStr .split("\\.")[0]); firstDecStrAfterDecimal =
	 * Integer.parseInt(firstDecStr .split("\\.")[1].replaceAll("^0", "")); } else {
	 * firstDecStrBeforeDecimal = Integer.parseInt(firstDecStr .replaceAll("\\.",
	 * "")); firstDecStrAfterDecimal = 0; } } // sections # scrubbed: first word
	 * must be initial caps and any // words more than 5 chars. Now I check if
	 * current sec# is in // sequence with next and if it is - cut from cur idx to
	 * next, if // not cut from cur to end of paragraph. //
	 * //NLP.pwNLP.append(NLP.println("isCap=" , isCap + // " foundTocSecHdg=" // +
	 * foundTocSecHdg + "\radd to sec list secNumber=" // + secNumber +
	 * " sectionHdg=" + sectionHdg + " sectionName=" // + sectionName +
	 * " firstDecStr=" + firstDecStr + " idxStr" // + idxStr + "|END"));
	 * listIdxAndSection.add(new String[] { idxStr, secNumber, sectionHdg,
	 * sectionName, firstDecStr, foundTocSecHdg + "" }); }
	 * 
	 * // get Sec# from secHdg and if in sequence text is startLoc to startLoc //
	 * next if not - employ paragraph capture technique that ends prior to // next
	 * sec out of sequence.
	 * 
	 * String solrSecHdg = null, solrSec = null, solrEnd = "]]></field>\r", solrData
	 * = "\"><![CDATA["; boolean lastSectionFound = false; String lastMatch = null,
	 * nextDecStr, foreCastedNextDecStr, foreCastedTwoAheadDecStr, sectionText;
	 * Integer secStartIdx = null, nextSecStartIdx = null, lastIdx = null, firstDec
	 * = 0, nextDec = 0, foreCastedNextDec = 0, decChg = 0, decChg2 = 0, decChg3 =
	 * 0, priorDecChg = -1, priorDecChg2 = -1, priorDecChg3 = -1, decChgCnt = 0,
	 * decChgCnt2 = 0, decChgCnt3 = 0, mostFreqDecChg = -1; for (int i = 0; i <
	 * listIdxAndSection.size(); i++) { secStartIdx =
	 * Integer.parseInt(listIdxAndSection.get(i)[0]); firstDecStr =
	 * listIdxAndSection.get(i)[4]; sectionHdg = listIdxAndSection.get(i)[2]; // if
	 * Sec 9.9 retrieve # before dec=9 and retrieve after dec = 9 // if 9.10
	 * retrieve # before dec=9 and retrieve after dec = 10 // if 9.09 retrieve #
	 * before dec=9 and retrieve after dec = 9 // if 9.10 retrieve # before dec=9
	 * and retrieve after dec = 10 // now I can subtract bef and aft to know when
	 * new art or sec
	 * 
	 * if (firstDecStr.split("\\.").length > 1) { firstDecStrBeforeDecimal =
	 * Integer.parseInt(firstDecStr .split("\\.")[0]); firstDecStrAfterDecimal =
	 * Integer.parseInt(firstDecStr .split("\\.")[1].replaceAll("^0", "")); } else {
	 * firstDecStrBeforeDecimal = Integer.parseInt(firstDecStr .replaceAll("\\.",
	 * "")); firstDecStrAfterDecimal = 0; }
	 * 
	 * // sectionName = listIdxAndSection.get(i)[3]; //
	 * //NLP.pwNLP.append(NLP.println("bb sectionHdg=",sectionHdg));
	 * 
	 * nextDecStr = null; if (i + 1 == listIdxAndSection.size()) { lastIdx =
	 * secStartIdx; // if last section and closing para found lastSection is cut //
	 * below Matcher m = patternSectionHeadingWithClosingParagraph
	 * .matcher(text.substring(lastIdx, Math.min(lastIdx + 10000, text.length())));
	 * lastMatch = sectionHdg; //
	 * //NLP.pwNLP.append(NLP.println("lastMatch/sectionHdg=" , // lastMatch)); if
	 * (m.find()) { // //NLP.pwNLP.append(NLP.println("m.start=" , m.start() + //
	 * " lastIdx=" + // lastIdx + " text.len=" + text.length())); //
	 * //NLP.pwNLP.append(NLP.println("lastSection using closingPara==" // ,
	 * text.substring(lastIdx, m.end() + lastIdx))); lastSectionFound = true;
	 * lastSection = text.substring(lastIdx, m.end() + lastIdx); } } else if ((i +
	 * 1) < listIdxAndSection.size()) { nextSecStartIdx = Integer
	 * .parseInt(listIdxAndSection.get(i + 1)[0]); nextDecStr =
	 * listIdxAndSection.get(i + 1)[4].replaceAll( "[ \t\r\n]", "").trim(); //
	 * //NLP.pwNLP.append(NLP.println("a nextDecStr=" , nextDecStr));
	 * 
	 * if (nextDecStr.split("\\.").length > 1) { nextDecStrBeforeDecimal =
	 * Integer.parseInt(nextDecStr .split("\\.")[0]); nextDecStrAfterDecimal =
	 * Integer.parseInt(nextDecStr .split("\\.")[1].replaceAll("^0", "")); } else {
	 * nextDecStrBeforeDecimal = Integer.parseInt(nextDecStr .replaceAll("\\.",
	 * "")); nextDecStrAfterDecimal = 0; } }
	 * 
	 * // find out how many decimal places there are - then remove decimal // --
	 * find out integer difference than add that difference to // firstDecStr to
	 * understand whta next nextDecStr should be so that // at last section in an
	 * article I can go to end if in fact the next // anticipated section heading
	 * does not exist. In order to do that - // I need to understand what the value
	 * difference between two // sections is suppose to be - so I use a counter -and
	 * which ever // counter is highest I use to mark expected difference.
	 * 
	 * if (StringUtils.isNotBlank(firstDecStr) &&
	 * StringUtils.isNotBlank(nextDecStr)) {
	 * 
	 * // //NLP.pwNLP.append(NLP.println("firstDecStr=" , firstDecStr // +
	 * " nextDecStr=" + nextDecStr)); decChg = Math.abs(nextDecStrAfterDecimal -
	 * firstDecStrAfterDecimal); // //NLP.pwNLP.append(NLP.println("nextDec=" ,
	 * nextDec + // " firstDec=" // + firstDec + " decChg=" + decChg));
	 * 
	 * }
	 * 
	 * boolean done = false; if (decChg == priorDecChg || decChgCnt == 0) {
	 * decChgCnt++; priorDecChg = decChg; done = true; }
	 * 
	 * if ((decChgCnt2 == 0 || decChg == priorDecChg2) && !done) { decChgCnt2++;
	 * priorDecChg2 = decChg; done = true; }
	 * 
	 * if ((decChgCnt3 == 0 || decChg == priorDecChg3) && !done) { decChgCnt3++;
	 * priorDecChg3 = decChg; done = true; }
	 * 
	 * // //NLP.pwNLP.append(NLP.println("decChgCnt=" , decChgCnt + // " decChg=" +
	 * decChg // + " decChg2=" + decChg2 + " decChgCnt2=" + decChgCnt2 // +
	 * " decChgCnt3=" + decChgCnt3));
	 * 
	 * if (decChgCnt > Math.max(decChgCnt2, decChgCnt3)) { mostFreqDecChg =
	 * priorDecChg; } if (decChgCnt2 > Math.max(decChgCnt, decChgCnt3)) {
	 * mostFreqDecChg = priorDecChg2; } if (decChgCnt3 > Math.max(decChgCnt2,
	 * decChgCnt)) { mostFreqDecChg = priorDecChg3; }
	 * 
	 * foreCastedNextDecStr = firstDecStrBeforeDecimal + "." +
	 * (firstDecStrAfterDecimal + mostFreqDecChg); //
	 * //NLP.pwNLP.append(NLP.println("firstDecStr=" , firstDecStr // +
	 * " foreCastedNextDecStr=" + foreCastedNextDecStr // +
	 * " firstDecStrAfterDecimal=" + firstDecStrAfterDecimal // + " mostFreqDecChg="
	 * + mostFreqDecChg + " nextDecStr=" // + nextDecStr)); // boolean has to go
	 * before I put decimal place back in. boolean
	 * forecastedConfirmedAgainstNextDecStr = false, forecastedInText = false;
	 * boolean foundForeCasted = true, newSection = false; if
	 * (foreCastedNextDecStr.equals(nextDecStr)) {
	 * forecastedConfirmedAgainstNextDecStr = true; }
	 * 
	 * if (nlpK.getAllIndexEndLocations( text.substring(secStartIdx,
	 * Math.min(text.length(), secStartIdx + 30000)), Pattern.compile("(" +
	 * foreCastedNextDecStr + ")")).size() > 0) { forecastedInText = true; }
	 * 
	 * if (!forecastedInText && !forecastedConfirmedAgainstNextDecStr) { // nopw
	 * check if two ahead forecsted section exists.
	 * 
	 * //NLP.pwNLP.append(NLP.println(
	 * "not in text or confirmed - foreCastedNextDecStr=", foreCastedNextDecStr +
	 * " nextDecStr=" + nextDecStr));
	 * 
	 * // to doubly confirm I to see if two sections ahead of last // exists.
	 * foreCastedTwoAheadDecStr = firstDecStrBeforeDecimal + "." +
	 * (firstDecStrAfterDecimal + mostFreqDecChg + mostFreqDecChg);
	 * //NLP.pwNLP.append(NLP.println("foreCastedTwoAheadDecStr=",
	 * foreCastedTwoAheadDecStr)); boolean foundForecastedHere = false; if
	 * (nlpK.getAllIndexEndLocations( text.substring(secStartIdx,
	 * Math.min(text.length(), secStartIdx + 30000)), Pattern.compile("(" +
	 * foreCastedTwoAheadDecStr + ")")) .size() > 0) { foundForecastedHere = true; }
	 * 
	 * foreCastedTwoAheadDecStr = firstDecStrBeforeDecimal + ".0" +
	 * (firstDecStrAfterDecimal + mostFreqDecChg + mostFreqDecChg);
	 * 
	 * if (!foundForecastedHere && nlpK.getAllIndexEndLocations(
	 * text.substring(secStartIdx, Math.min( text.length(), secStartIdx + 30000)),
	 * Pattern.compile("(" + foreCastedTwoAheadDecStr + ")")).size() > 0) {
	 * foundForecastedHere = true;
	 * 
	 * }
	 * 
	 * // this sets forecasted to next decimal string when there an // increase by 1
	 * in section (article change) provided it is a // change of 1 AND the string
	 * after the decimal in nextDecStr is // 1 or 0 if (!foundForecastedHere &&
	 * nextDecStrBeforeDecimal - firstDecStrBeforeDecimal == 1 &&
	 * (nextDecStrAfterDecimal == 1 || nextDecStrAfterDecimal == 0)) { //
	 * //NLP.pwNLP.append(NLP.println("c mostFreqDecChg==" , // mostFreqDecChg // +
	 * " nextDecStr=" + nextDecStr)); foreCastedNextDecStr = nextDecStr;
	 * forecastedInText = true; forecastedConfirmedAgainstNextDecStr = true; } }
	 * 
	 * // //NLP.pwNLP.append(NLP.println("a secHdg=" , sectionHdg + //
	 * " mostFreqDecChg=" // + mostFreqDecChg)); //
	 * //NLP.pwNLP.append(NLP.println("forecasted secHdg#=" // ,
	 * (firstDecStrAfterDecimal + mostFreqDecChg) // + " current firstDecStr=" +
	 * firstDecStr)); // //NLP.pwNLP.append(NLP.println("forecastedConfirmed==" // ,
	 * forecastedConfirmedAgainstNextDecStr));
	 * 
	 * if (!forecastedConfirmedAgainstNextDecStr) { //
	 * //NLP.pwNLP.append(NLP.println("a forecastedConfirmed=" // ,
	 * forecastedConfirmedAgainstNextDecStr + " secHdg=" // + sectionHdg +
	 * "foreCastedNextDecStr=" // + foreCastedNextDecStr));
	 * 
	 * // if I don't have next up in sequence - search if forecasted // next up
	 * exists in the next 15,000 characters. If not - then // use the next section
	 * in sequence. E.g., if current is 2.06 // but next is 3.01 and there's no 2.07
	 * (forecasted) go to from // 2.06 to start of 3.01 (or article of) //
	 * //NLP.pwNLP.append(NLP.println("foreCastedNextDecStr=" // ,
	 * foreCastedNextDecStr + " nextDecStr=" + nextDecStr));
	 * 
	 * // //NLP.pwNLP.append(NLP.println("b nextDecStrAfterDecimal=" // ,
	 * nextDecStrAfterDecimal + " firstDecStrAfterDecimal=" // +
	 * firstDecStrAfterDecimal)); if (nextDecStrAfterDecimal -
	 * firstDecStrAfterDecimal == 1 || nextDecStrAfterDecimal -
	 * firstDecStrAfterDecimal == 10 || nextDecStrAfterDecimal -
	 * firstDecStrAfterDecimal == 100
	 * 
	 * ) { newSection = true; } }
	 * 
	 * // use forecasted when it is less than next by setting // nextSecStartIdx to
	 * foreCastedIdx value. if (foundForeCasted) { NLP.pwNLP .append(NLP .println(
	 * "nextDecStr!=foreCastedNextDecStr and foundForecasted=", foundForeCasted +
	 * "")); //NLP.pwNLP.append(NLP.println("foreCastedNextDecStr=",
	 * foreCastedNextDecStr + " nextDecStr=" + nextDecStr + "a forecastedInText=" +
	 * forecastedInText)); if (forecastedInText && nlpK.getAllIndexStartLocations(
	 * text.substring(secStartIdx, Math.min( text.length(), secStartIdx + 50000)),
	 * Pattern.compile("([\r\n\t]{1}[ ]{2,30}(Section|SECTION)[\t\r\n ]{1,5}" +
	 * foreCastedNextDecStr + ")")).size() > 0) { nextSecStartIdx = nlpK
	 * .getAllIndexStartLocations( text.substring(secStartIdx, Math.min(
	 * text.length(), secStartIdx + 50000)),
	 * Pattern.compile("([\r\n\t]{1}[ ]{2,30}(Section|SECTION)[\t\r\n ]{1,5}" +
	 * foreCastedNextDecStr + ")")).get( 0) + secStartIdx;
	 * 
	 * // //NLP.pwNLP.append(NLP.println("foreCasted=" , // foreCastedNextDecStr //
	 * + " set nextSecStartIdx=" + nextSecStartIdx)); //
	 * //NLP.pwNLP.append(NLP.println("text.substr=" // ,
	 * text.substring(secStartIdx, nextSecStartIdx))); newSection = true; } } //
	 * //NLP.pwNLP.append(NLP.println("a newSection=" , newSection));
	 * 
	 * sectionText = null;
	 * 
	 * // will go to newSection or next section in sequence. If newSection // I
	 * shoudl check if there's an article marker close to newSection.
	 * 
	 * // //NLP.pwNLP.append(NLP.println("i=" , i + // " listIdxAndSection.size=" //
	 * + listIdxAndSection.size() + " decChg=" + decChg // + " newSection=" +
	 * newSection)); if ((i + 1) < listIdxAndSection.size() && ((null != decChg &&
	 * decChg == 1) || (newSection))) {
	 * 
	 * // cut from current idx to next. Exclude Article Heading if // possible
	 * 
	 * sectionText = text.substring(secStartIdx, nextSecStartIdx); String[] strAry4
	 * = { "sIdx", "sectionHeading", sectionHdg.replaceAll("[\r\n]([ ]{1,})?|(: ?)",
	 * "") }; mapOfContractIdxLocs.put(secStartIdx, strAry4); String[] strAry5 = {
	 * "eIdx", "" }; mapOfContractIdxLocs.put(nextSecStartIdx - 1, strAry5);
	 * 
	 * // //NLP.pwNLP.append(NLP.println("1 secStartIdx=" // , secStartIdx // +
	 * " eIdx=" // + nextSecStartIdx // + " text.len=" // + sectionText.length() //
	 * + "\rsecHeading=" // + sectionHdg.replaceAll("[\r\n]([ ]{1,})?|(: ?)", "") //
	 * + "\r 1 sectionText start=" // + sectionText.substring(0,
	 * Math.min(50,sectionText.length())) // + "\r 1 sectionText=" // +
	 * "\rsectionText end=" // +
	 * sectionText.substring(Math.max(0,sectionText.length() - // 50), //
	 * sectionText.length())));
	 * 
	 * } else { NLP.pwNLP .append(NLP .println(
	 * " start portion of text passed to section lastSection=",
	 * text.substring(secStartIdx, secStartIdx + 200)));
	 * 
	 * lastSection = getLastDefinition(text, secStartIdx,
	 * sectionHdg,"sec",listIdxAndSection.size()); sectionText = lastSection;
	 * 
	 * String[] strAry4 = { "sIdx", "sectionHeading",
	 * sectionHdg.replaceAll("[\r\n]([ ]{1,})?|(: ?)", "") }; //
	 * //NLP.pwNLP.append(NLP.println("2 secStartIdx=" // , secStartIdx // +
	 * " eIdx=" // + (secStartIdx + sectionText.length() - 1) // + " text.len=" // +
	 * sectionText.length() // + "\r2 secHeading=" // +
	 * sectionHdg.replaceAll("[\r\n]([ ]{1,})?|(: ?)", "") // +
	 * "\r2 sectionText start=" // + sectionText.substring(0,
	 * Math.min(50,sectionText.length())) // + "\rsectionText end=" // +
	 * sectionText.substring(Math.max(0,sectionText.length() - // 50), //
	 * sectionText.length()));
	 * 
	 * mapOfContractIdxLocs.put(secStartIdx, strAry4); String[] strAry5 = { "eIdx",
	 * "" }; mapOfContractIdxLocs.put( (secStartIdx + sectionText.length() - 1),
	 * strAry5); } } }
	 */
	public static String getOnlyAlphaCharacters(String text) {
		if (text == null)
			return null;
		text = text.replaceAll("\"", "").replaceAll("[^A-Za-z\\d]|:", "_").replaceAll("[_]{2,}", "_");
		if (text.length() > 2)
			text = text.substring(0, text.length() - 1)
					+ text.substring(text.length() - 1, text.length()).replaceAll("_", "");
		return text;
	}

	public static int getNumberLetter(int number) {
		int num = -1;

		if (number == 0) {
			num = getNumberInAlphabet("Z");
		}
		if (number == 1) {
			num = getNumberInAlphabet("O");
		}
		if (number == 2) {
			num = getNumberInAlphabet("T");
		}
		if (number == 3) {
			num = getNumberInAlphabet("T");
		}
		if (number == 4) {
			num = getNumberInAlphabet("F");
		}
		if (number == 5) {
			num = getNumberInAlphabet("F");
		}
		if (number == 6) {
			num = getNumberInAlphabet("S");
		}
		if (number == 7) {
			num = getNumberInAlphabet("S");
		}
		if (number == 8) {
			num = getNumberInAlphabet("E");
		}
		if (number == 9) {
			num = getNumberInAlphabet("N");
		}

		return num;

	}

	public static int getNumberInAlphabet(String letter) {
		int ltrNo = -1;
		if (letter.matches("^[A-Z]$"))
			ltrNo = letter.charAt(0) - 'A' + 1;
		if (letter.matches("^[\\d]$"))
			ltrNo = Integer.parseInt(letter);
		return ltrNo;
	}

	public static void main(String[] arg) throws IOException, SQLException {

		NLP nlp = new NLP();
		ContractParser kP = new ContractParser();

		String text = Utils.readTextFromFile("c:/getcontracts/tmp.txt");
		text = stripHtmlTags(text);
		text = text.replaceAll("ffff", "\r\nffff");
//		System.out.println("text.snip="+text.substring(0, 10000));
		// kP.parseAllFiles(contractsFolder_Raw);
		File file = new File(contractsFolder_Raw, "0000004457-07-000011_3_BOX_TRUCK_INDENTURE.txt"
		// "0000905148-05-001017_2_EXHIBIT_FORM_OF_POOLING_AND_SERVICING_AGR.txt"
		// "0000003673-05-000039_3_SUPPLEMENTAL_INDENTURE.htm"
		// "0000010427-05-000140_1_SIXTH_SUPPLEMENTAL_INDENTURE.txt"
		// "0000018926-05-000012_5_EXHIBIT_THIRD_SUPP_INDENTURE.txt"
		// "0000041091-05-000011_2_SUPPLEMENTAL_INDENTURE.txt"
		// "0000849213-05-000062_1_EX_FORM_OF_DEBT_SECURITIES_INDENTURE.txt"
		// "0000790715-05-000011_7_FORM_OF_SUBORDINATED_INDENTURE.txt"
		// "0001137171-05-000296_10_SUPPLEMENT_DATED_JANUARY_TO_TRUST_INDENTURE_DATED_MAY_.txt"
		// "0001193125-05-067168_1_INDENTURE_DATED_MARCH_.txt"
		// File file = new File(contractsFolder_Raw,
		// "0001116502-05-000581_5_TRUST_INDENTURE_DATED_JULY_.txt"
		);

		// TODO: MAKE PUBLIC STRINGS OF SOLR FIELD STARTS AND ENDS BY CATEGOR
		// TYPE: EXHIBIT, TOC_HEADING, TOC, SECTION, DEFINEDTERM ETC THEN USE
		// THAT TO CYCLE THROUGH MAP WHEN I APPEND TO TXT TO FILE

		// TODO: If K has TOC and there's a doc as an exhibit
		// with a tableOfContents it won't be parsed. HOWEVER if a
		// doc is an exhibit to a contract best bet is to remove it
		// and leave a placeholder reference number so as to
		// retrieve. What I'm really interested in searching are
		// individual contracts and their non-contract related exhibits.
		// copy line above to see where break occurs that causes this to happen.
		// other alternative is when I find an exhibit that's marked - to send
		// exhibit to TOC Parser and DefinedTerm Parser and Section Parser. DO I
		// NEED TO GIVE IT ANY OTHER FIELDS.

		// TALK TO PRAVEEN BEFORE MOVING TO FAR AHEAD. OR GET A SAMPLE TO LOAD
		// INTO SOLR ON CHROME!

		File folder = new File("c:/getContracts/unStrippedKs/");
		File[] listOfFiles = folder.listFiles();

		/*
		 * delete files if same first 10 digits in same accno=> TreeMap<Integer,String>
		 * map = new TreeMap<>(); for (File f : listOfFiles) {
		 * System.out.println("f.getName()="+f.getAbsolutePath()); if
		 * (map.get(Integer.parseInt(f.getName().substring(0, 10))) != null) {
		 * f.delete(); continue; }
		 * map.put(Integer.parseInt(f.getName().substring(0,10)), f.getName()); }
		 */

		/*
		 * for (File f : listOfFiles) { kP.parseContract(f); }
		 */
		text = " <BODY BGCOLOR=\"WHITE\">\r\n" + "\r\n"
				+ " <P STYLE=\"margin-top:0pt; margin-bottom:0pt; font-size:10pt; font-family:Times New Roman\" ALIGN=\"right\"><B>Exhibit 4.1 </B></P> <P STYLE=\"font-size:12pt;margin-top:0pt;margin-bottom:0pt\">&nbsp;</P>\r\n"
				+ "<P STYLE=\"line-height:1.0pt;margin-top:0pt;margin-bottom:0pt;border-bottom:1px solid #000000\">&nbsp;</P> ";

		stripHtmlTags(text);
		System.out.println(text);
	}
}
