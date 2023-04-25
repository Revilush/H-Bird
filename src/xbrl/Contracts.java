package xbrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;

/*import org.apache.solr.client.solrj.SolrServerException;
 import org.apache.solr.client.solrj.impl.HttpSolrServer;
 import org.apache.solr.common.SolrInputDocument;*/

public class Contracts {

	public Document document;
	public String secZipFolder = "c:/SECZipFiles/";
	public String masterIdxFolder = "c:/backtest/master/";
	public String contractsFolder = "c:/getContracts/";
	public String downloadFolder = "c:/getContracts/downLoaded/";

	public String fileDate = "1901-01-01";
	public String companyName = "";// from master.idx
	public String cik = "0";
	public static String acc = "0";
	public String formType = "0";
	public static String type = null;
	public static String toc = null;

	public static int defCnt = 0;
	public static int counter = 0;
	public static Integer tocStartIdx = null;
	public static Integer tocEndIdx = null;
	public static Integer exhStartIdx = null;
	public static Integer exhEndIdx = null;
//	public static Integer secStartIdx = null;
//	public static Integer secEndIdx = null;

	// public static Integer addToIdx = null;
	public static String contractLongName;

	public static List<String[]> listOfDefinedTermsIdxsAndNames = new ArrayList<>();
	public static List<String[]> listOfExhibitsIdxsAndNames = new ArrayList<>();
//	public static TreeMap<Integer, String[]> mapOfAllContractParts = new TreeMap<Integer, String[]>();
	// key=start idx, String[]: [0]=endIdx of last match, [1]=type (e.g.,
	// TOC,Section,Exhibit), [2]=first text match, [3]=last text match

	public static Pattern contractNamePattern = Pattern.compile("(This|THIS).{1,2}[A-Z].*?(Agreement|AGREEMENT|[\"\\(])");
	
	public static Pattern patternClosingParagraph = Pattern
			.compile("(?ism)(WITNESS WHEREOF.{1,90}(executed|set.{1,4}their.{1,4}hand).{1,150}\\.)"
					+ "|\\|?Signatures?.{1,6}((follow[ing]{0,3}.{1,5}pages?)|(pages? follow))\\|?"
					+ "|Signatures?.{1,6}following page");

	public static Pattern patternSignaturePage = Pattern
			.compile("(?sm)[\r\n]{1,} ?\\|?"
					+ "(?=(Signature.{1,4}Page.{1,4}Follows|SIGNATURE.{1,4}PAGE.{1,4}FOLLOWS) ?\\|? ?)");


	public static Pattern patternContractToc = Pattern.compile(
			"(?s)(Section|Subsection|SUBSECTION|SubSection|SECTION)[ \r\n\t]{1,4}[iIvVxX\\d\\.]{1,6}.{1,175}?.{1,175}?[\\d]{1,4}");

	public static Pattern patternExhibitInContract = Pattern
			.compile("[\r\n]{1}[ \t]{0,110}(Exhibit|EXHIBIT|APPENDIX|Appendix|SCHEDULE|Schedule|Annex|ANNEX)"
					+ "[ ]{1,3}[A-Z\\-\\d\\(\\)]{1,5}( to)? ?[\r\n]{1}");
	// Can you find Exhibit A on a new line essentially by itself.
	public static Pattern patternPAGE_PRE_S_C_SEC_CODES = Pattern.compile("<PAGE>|<PRE>|<S>|<C>");

	public static Pattern patternExhibitToc = Pattern
			.compile("[\r\n]{1}[ \t]{0,4}(Exhibit|EXHIBIT|APPENDIX|Appendix|SCHEDULE|Schedule|Annex|ANNEX)"
					+ "[ ]{1,2}[A-Z\\-\\d]{1,4}[ \t]{1,20}" + "[\\p{Alnum}\\p{Punct}]{1,90}.{1,90}?[\r\n]{1}");

	public static Pattern patternArticle = Pattern
			.compile("(?s)([\r\n]{1}[ ]{1,100}(ARTICLE|Article)[ \r\n\t]{1,3}[iIvVxX\\d]{1,5}"
					+ ".{1,200}?(?=Section|SECTION[ \r\n\t]{1,4}[iIvVxX\\d\\.]{1,7}))");

	public static Pattern patternTocPageNumber = Pattern
			.compile("[\r\n]{1}[\t ]{0,10}[\\p{Alnum}\\p{Punct}].{1,170}[\\d]+ ?[\r\n]");

	public static Pattern patternContractTocWithExhibits = Pattern.compile(patternContractToc.pattern() + "|"
//					+ patternTocPageNumber.pattern() + "|"
			+ patternExhibitToc.pattern() + "|" + patternArticle.pattern());

	public static Pattern alphabetPattern = Pattern.compile("[A-Za-z]{5,}");

	public static Pattern nonAlphaPattern = Pattern.compile("[^A-Za-z]");

	public static Pattern patternPageNumber = Pattern
			.compile("(?ims)([\r\n]{1}[\t ]{0,100}[page]{0,4}[\\dixv\\-]{1,6}[\t ]{0,100}[\r\n]{1})");

	public static Pattern patternClosingPara = Pattern
			.compile("(?sm)(WITNESS WHEREOF.{1,90}(executed|set.{1,4}" + "their.{1,4}hand).{1,150}\\.)");

	public static Pattern spaceNumberspacePattern = Pattern.compile("(?<!([A-Za-z])) [-]{0,}[\\d,]{1,}");

	public Pattern patternParentChildParas = Pattern
			.compile("([:;]|[,;] (and|or)) ?\r ?([A-Z]{1}|[\\d\\.]{2}|\\([a-z\\dA-Z]\\))");

	public static Pattern patternDescription = Pattern.compile("(?i)(DESCRIPTION>.{1,300}?\\<)", Pattern.DOTALL);

	public static Pattern patternDefinedWordInBodyOfText = Pattern
			.compile("(?<![\r\n]{1}[\\s ])\"[A-Za-z\\s \\d]+{1,60}\"");
	// TODO: get list of words "Defined" in body of text. Need more complex
	// pattern matcher.

//	public static Pattern patternFullDefinitionFromDefSection = Pattern
//			.compile("[\r\n]{1}[\\s ]\"[\\p{Alnum}\\p{Punct}].{1,50}\".*([\r\n]( \t\\s)?)");
	// (?>[\"A-Z])

//	public static Pattern patternFullDefinitionFromDefSection = Pattern
//			.compile("[\r\n]{1}[\\s ]?\".*(?<=[\r\n]{1}[\\s ]?\")");

//	public static Pattern patternDefinedWordInDefSec = Pattern
//			.compile("[\r\n]{2}[ ]{0,15}\"[\\dA-Z]{1}.[^\"]*\"");

	public static Pattern patternDefinedWordInDefSec = Pattern
			.compile("[\r\n]{2}[ ]{0,15}\"[\\dA-Z]{1}[A-Za-z ,;:\\&\\-\\)\\(\\[\\]\\$\\%\\*]{1,70}\"");

	// need to have [\r\n]{3,} b/c won't pickup 2 or more if just \r or \n

	public static Pattern patternSection = Pattern
			.compile("(Section|Subsection|SUBSECTION|SubSection|SECTION)[ \r\n\t]{1,3}[iIvVxX\\d\\.]{1,6}");

	public static Pattern patternSectionHeading = Pattern.compile(
			"(?s)(([\r\n]{1,}([\t]{1,4}|[\\s]{1,15})?)(Section|Subsection|SUBSECTION|SubSection|SECTION)[ \r\n\t]{1,3}[iIvVxX\\d\\.]{1,6}(?![\r]{2,})"
					+ "[\\-\t\\(\\)\\d\\&\\$\\%A-Za-z ;,\\[\\{\\}']{4,150}(\\.|[\r\n]{2}))"
					+ "|((Section|SECTION).{1,5}[\\d\\.]{1,6}.{1,4}Taxes.{1,5}[\r\n])");
	// added Taxes -didn't know how to otherwise get. Had to add (?sm)

	public static Pattern patternSectionHeadingWithClosingParagraph = Pattern
			.compile(patternClosingPara.pattern() + "|" + patternClosingPara.pattern());

	public Pattern patternContractSentenceStart = Pattern
			.compile("(?<=(\\.(\r\n|[ ])))[A-Z\\(]{1}|(?<=[\r\n]{1}[ \\s])[A-Z\\(]{1}");
	// start of sentence -- hard return followed by A-Z \\(
	// [\r\n]{1}[ \\s][A-Z\\(]{1}
	// start of sentence is end of space period.
	// (?<=(\\.(\r\n|[ ])))[A-Z\\(]{1}

	public static Pattern patternType = Pattern.compile("(?i)(TYPE>.{1,100}?\\<)", Pattern.DOTALL);

	public static Pattern ExtraHtmlPattern = Pattern.compile("(?i)(</?[[font]|[b]|[i]|[u]]*[^>]*>)");

	public static Pattern ExtraHtmlPattern2 = Pattern.compile("(?i)(</?p[^>]*>)");

	// |</td[^>]*>

	public String stripHtmlTags(String text) throws FileNotFoundException {

		//fix these. <p> or <p [^>]*>  NOT <p[^>]*> - same for all startP. And all ends should be simply </p>
		text = text.replaceAll("<PRE>|</PRE>|<PAGE>|</PAGE>", "");
		Pattern startP = Pattern.compile("(?i)(<p[^>]*>)");
		Pattern endP = Pattern.compile("(?i)(</p[^>]*>)");
		Pattern startDiv = Pattern.compile("(?i)(<div[^>]*>)");
		Pattern endDiv = Pattern.compile("(?i)(</div[^>]*>)");
		Pattern startTh = Pattern.compile("(?i)(<th[^>]*>)");
		Pattern endTh = Pattern.compile("(?i)(</th[^>]*>)");
		Pattern startTr = Pattern.compile("(?i)(<tr[^>]*>)");
		Pattern endTr = Pattern.compile("(?i)(</(tr|table)[^>]*>)");
		Pattern startTd = Pattern.compile("(?i)(<td[^>]*>)");
		Pattern endTd = Pattern.compile("(?i)(</td[^>]*>)");

		try {
			text = text.replaceAll("(?i)[ ]{1,}<p", "<p");
			/*
			 * PrintWriter tempPw3A = new PrintWriter(new File(
			 * "c:/temp/temp3A"+contractLongName+".txt")); tempPw3A.append(text);
			 * tempPw3A.close();
			 */
			text = keepCellsInSameRow(text, startP, endP);
//			PrintWriter tempPw3 = new PrintWriter(new File(
//					"c:/temp/temp3"+contractLongName+".txt"));
//			tempPw3.append(text);
//			tempPw3.close();
		} catch (Throwable t) {
			t.printStackTrace(System.out);
		}

		try {

			text = keepCellsInSameRow(text, startDiv, endDiv);
//			PrintWriter tempPw4 = new PrintWriter(new File(
//					"c:/temp/temp4"+contractLongName+".txt"));
//			tempPw4.append(text);
//			tempPw4.close();
		} catch (Throwable t) {
			t.printStackTrace(System.out);
		}

		try {
			text = keepCellsInSameRow(text, startTd, endTd);
//			PrintWriter tempPw5 = new PrintWriter(new File(
//					"c:/temp/temp5"+contractLongName+".txt"));
//			tempPw5.append(text);
//			tempPw5.close();

		} catch (Throwable t) {
			t.printStackTrace(System.out);
		}

		try {
			text = keepCellsInSameRow(text, startTh, endTh);
//			PrintWriter tempPw6 = new PrintWriter(new File(
//					"c:/temp/temp6"+contractLongName+".txt"));
//			tempPw6.append(text);
//			tempPw6.close();

		} catch (Throwable t) {
			t.printStackTrace(System.out);
		}

		try {
			text = keepCellsInSameRow(text, startTr, endTr);
//			PrintWriter tempPw7 = new PrintWriter(new File(
//					"c:/temp/temp7"+contractLongName+".txt"));
//			tempPw7.append(text);
//			tempPw7.close();

		} catch (Throwable t) {
			t.printStackTrace(System.out);
		}

		try {
			text = text.replaceAll(NLP.TRPattern.toString(), NLP.LineSeparator);
			text = text.replaceAll("\\&nbsp;|\\xA0|\\&#149;|\\&#160;|\\&#xA0;|\\&#168;", " ");
			text = text.replaceAll("\\&#151;|\\&mdash|\\&#95;", "_");
			text = text.replaceAll("\\&#8211;|\\&#150;|&#8212;", "-");
			text = text.replaceAll("\\&amp;", "\\&").replaceAll("\\&#091;", "\\[").replaceAll("\\&#093;", "\\]");
			text = text.replaceAll("Ã¢â‚¬â€�", "-").replaceAll("\\&#166;|\\&#167;|\\&#133;|&#174;", "");
			text = text.replaceAll("\\&lt;", "<");
			text = text.replaceAll("\\&gt;", ">");
			text = text.replaceAll("\\&#146;|\\&rsquo;|\\&#8217;|\\&#x2019;", "'")
					.replaceAll("\\&#147;|\\&#148;|Ã¢â‚¬Å“|Ã¢â‚¬?|\\&#8221;|\\&#8220;|\\&ldquo;|\\&rdquo;", "\"");

//			PrintWriter tempPw8 = new PrintWriter(new File(
//					"c:/temp/temp8"+contractLongName+".txt"));
//			tempPw8.append(text);
//			tempPw8.close();

			text = text.replaceAll(NLP.TDWithColspanPattern.toString(), "\t\t");
			text = text.replaceAll(NLP.TDPattern.toString(), "\t");
			text = text.replaceAll("(?i)(<SUP[^>]*>[\\(0-9\\) ]*</SUP[^>]*>)", "");
			text = text.replaceAll("(?i)(<[/]U>)", "");
			// above is removed b/c it interfers with below <BR>
			text = text.replaceAll("(?i)<BR> ?\r\n ?<BR>", "\r\n");
			// if 2 consecutive BRs-likely meant hard return
			// if BR after a number - likely end of a row.
			text = NLP.numberBR.matcher(text).replaceAll("\r\n");
			text = text.replaceAll("(?i)(<BR>\r\n|\r\n<BR>)", " ");
			text = text.replaceAll("(?i)(</strong>|<strong>|</small>|<small>)", "");

//			PrintWriter tempPw9 = new PrintWriter(new File(
//					"c:/temp/temp9"+contractLongName+".txt"));
//			tempPw9.append(text);
//			tempPw9.close();

			text = text.replaceAll("(?<=[\\p{Alnum};,\":\\-\\&]) <", "\\&nbsp;<");
			// need placeholder otherwise all blanks are removed between dummy
			// extraHtmlS eg HELLO<FONT> </FONT>WORLD. [this will hold the space
			// between the carrots
			text = text.replaceAll(ExtraHtmlPattern2.toString(), "");
//			PrintWriter tempPw9b = new PrintWriter(new File(
//					"c:/temp/temp9b"+contractLongName+".txt"));
//			tempPw9b.append(text);
//			tempPw9b.close();

			text = text.replaceAll("(?i)<BR>", "\r ").replaceAll(ExtraHtmlPattern.toString(), "");

			text = text.replaceAll("\\&nbsp;", " ");// replace 2 or 3 w/ 1
			text = text.replaceAll("</td[^>]*>", "        ");

//			PrintWriter tempPw10 = new PrintWriter(new File(
//					"c:/temp/temp10"+contractLongName+".txt"));
//			tempPw10.append(text);
//			tempPw10.close();

		} catch (Throwable t) {
			t.printStackTrace(System.out);
		}
		return text;
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
			// System.out.println("no pattern found..");
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

	public void dateRangeQuarters(Calendar startDate, Calendar endDate)
			throws SocketException, IOException, SQLException, ParseException {

		int initialYear = startDate.get(Calendar.YEAR);
		int endYear = endDate.get(Calendar.YEAR);
		int startQtr = TableParser.getQuarter(startDate);
		int endQtr = TableParser.getQuarter(endDate);

		// total # of loops=totalQtrs.

		String minDate = "";
		String maxDate = "";
		// int cnt = 0;
		int QtrYrs = (endYear - initialYear) * 4;
		int iQtr = (endQtr - startQtr) + 1;
		int totalQtrs = QtrYrs + iQtr;
		int nextYr = initialYear;
		System.out.println("nextYr:" + nextYr);
		iQtr = startQtr;
		System.out.println("iQtr:" + iQtr);
		Calendar cal = Calendar.getInstance();
		for (int i = 1; i <= totalQtrs; i++) {
			System.out.println("totalQtrs=" + totalQtrs + ",i=" + i);
			cal.set(Calendar.YEAR, nextYr);
			cal.set(Calendar.MONTH, (iQtr * 3) - 1);
			// will retrieve masterIdx if not in folder or current
			String localPath = masterIdxFolder + "/" + nextYr + "/QTR" + iQtr + "/";
			String localPath2 = localPath;
			System.out.println("localPath1=" + localPath);
			File file = new File(localPath2 + "/master.idx");
			TableParser tp = new TableParser();

			if (!file.exists() && !tp.isCurrentQuarter(endDate)) {
				tp.getMasterIdx(nextYr, iQtr, cal);
			}

			String fileName;
			File folder = new File(secZipFolder);
			File[] listOfFiles = folder.listFiles();

			// only first time it will pass here
			// if (cnt == 0) {
			minDate = nextYr + "";
			maxDate = nextYr + "";
			if (iQtr == 1) {
				minDate += "01";
				maxDate += "03";
			} else if (iQtr == 2) {
				minDate += "04";
				maxDate += "06";
			} else if (iQtr == 3) {
				minDate += "07";
				maxDate += "09";
			} else if (iQtr == 4) {
				minDate += "10";
				maxDate += "12";
			}

			int iF = 0;
			System.out.println(
					"minDate=" + minDate + ", maxDate=" + maxDate + ",iQtr=" + iQtr + ",startYear=" + initialYear);
			for (iF = 0; iF < listOfFiles.length; iF++) {

				if (listOfFiles[iF].isFile()) {
					fileName = listOfFiles[iF].getName().substring(0, 6);
					if (fileName.compareTo(minDate) >= 0 && fileName.compareTo(maxDate) <= 0) {
						String fileDate = listOfFiles[iF].getName().substring(0, 8);
						String extractToFolder = folder + "/" + fileDate;
						FileSystemUtils.createFoldersIfReqd(extractToFolder);

						if (!ZipUtils.isTarGzipFileValid(listOfFiles[iF].getAbsolutePath())
								|| listOfFiles[iF].getName().contains(".old")
								|| listOfFiles[iF].getName().contains(".bad")) {

							Utils.createFoldersIfReqd(secZipFolder + "/bad/");
							listOfFiles[iF].renameTo(new File(secZipFolder + "/bad/" + listOfFiles[iF].getName()));
							continue;
						}

						if (listOfFiles[iF].getAbsolutePath().contains("nc.tar.gz")) {
							ZipUtils.deflateTarGzipFile(listOfFiles[iF].getAbsolutePath(), extractToFolder);
						}

						else {
							String outputFn = ZipUtils.deflateGzipFile(listOfFiles[iF].getAbsolutePath(),
									extractToFolder);

							// below extracts from yyyymmdd.gz file (this file
							// contains all submissions for that day in 1 file)
							// a single filing by capturing from start to end
							// submission marker and saves each filing as
							// accno.nc so that existing 'architechture' can be
							// run to parse it.

							Pattern ACCPattern = Pattern.compile("(?<=(<ACCESSION-NUMBER>)).*\r\n");

							BufferedReader br = new BufferedReader(new FileReader(outputFn));
							StringBuilder sb = new StringBuilder();
							String line, terminator = "</SUBMISSION>";
							while (true) {
								line = br.readLine();
								if (null == line || line.contains(terminator)) {
									String accNo = "";
									Matcher matcherACC = ACCPattern.matcher(sb.toString());
									if (matcherACC.find()) {
										accNo = extractToFolder + "/" + matcherACC.group().trim() + ".nc";
										FileSystemUtils.writeToAsciiFile(accNo, sb.toString());
									}
									if (null != line) {
										int idx = line.indexOf(terminator) + terminator.length();
										sb.append(line.substring(0, idx));
										line = line.substring(idx);
									} else
										// we are at EOF
										break;
									sb = new StringBuilder();
								}
								sb.append(line).append("\r\n");
							}
							br.close();
						}

						System.out.println("get accFile iQtr=" + iQtr + " extractToFolder=" + extractToFolder);

						getAcc(extractToFolder, localPath2);
						File tempDir = new File(extractToFolder);
						Utils.deleteDirectory(tempDir);

						FileSystemUtils.createFoldersIfReqd(folder + "/parsed/");
						File parseF = new File(folder + "/parsed/" + listOfFiles[iF].getName());
						// moves zip file to parsed folder after it has parsed
						// all.

						if (parseF.exists())
							parseF.delete();
						listOfFiles[iF].renameTo(parseF);

						System.out.println("deleted extractToFolder:: " + tempDir.getAbsolutePath());
						// file.delete();
					} else
						System.out.println(
								"fileName not in range.=" + fileName + ", maxDate=" + maxDate + ", minDate=" + minDate);
				}
			}

			iQtr++;
			if (iQtr > 4) {
				nextYr++;
				iQtr = 1;
			}
		}
	}

	public void getAcc(String localPath, String localPath2) throws IOException, ParseException, SQLException {

		// localPath "c:/backtest/secZipFiles/20041230";
		// NLP nlp = new NLP();

		String tpIdx = localPath2 + "/master.idx";
		// masterIdxFolder;
		System.out.println("getAcc localPath: " + localPath);
		File f3 = new File(tpIdx);
		// System.out.println("f3::" + f3);

		if (!f3.exists()) {
			System.out
					.println("mysql exported file of accno to parse DOES NOT EXIST:" + f3.getAbsolutePath().toString());
			return;
		}
		BufferedReader tpIdxBR = null;
		try {
			tpIdxBR = new BufferedReader(new FileReader(tpIdx));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		String line;
		String tmpAcc;
		int c = 0;
		try {
			while ((line = tpIdxBR.readLine()) != null) {
				c++;
				if (c > 10 && line.split("\\|").length > 3) {
					tmpAcc = line.split("\\|")[4].replaceAll(".txt", "");
					acc = tmpAcc.substring(tmpAcc.length() - 20, tmpAcc.length());
					cik = line.split("\\|")[0];
					formType = line.split("\\|")[2];
					fileDate = line.split("\\|")[3];
					companyName = line.split("\\|")[1];
					// System.out.println("getAcc:: " + acc + " formType: "
					// + formType + " companyName: " + companyName
					// + " cik: " + cik + " fileDate: " + fileDate);

					String localPathDate = localPath.substring(localPath.length() - 8, localPath.length());
					// System.out.println("THIS IS THE LOCALPATH date=="
					// + localPathDate);
					if (!fileDate.replaceAll("-", "").equals(localPathDate))
						continue;

					System.out.println("FILEDATE==LOCALPATH");
					String filePath = localPath + "/" + acc + ".nc";

					filePath = filePath.replaceAll("\\\\", "//");
					File file = new File(filePath);
					System.out.println("filePath=" + file.getAbsolutePath().toString());
					if (file.exists() &&

							!formType.trim().equals("4") && !formType.trim().equals("3") && !formType.trim().equals("5")
							&& !formType.trim().equals("4/A") && !formType.trim().equals("3/A")
							&& !formType.trim().equals("5/A")

					) {
						System.out.println("not an insider filing - calling getContract at filePath=" + filePath);
						getContract(filePath);
					}
				}
			}
			tpIdxBR.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void getContract(String filePath) throws IOException, SQLException {

		int contractCount = 0;
		String contractText = "", typeText, text = "";
		int sIdx, startIdxContract;
		NLP nlp = new NLP();
		String ncFileText = Utils.readTextFromFile(filePath);

		List<String[]> listIdxDescription = nlp.getAllStartIdxLocsAndMatchedGroups(ncFileText, patternDescription);
		List<Integer> listIdxsTextEnd = nlp.getAllIndexStartLocations(ncFileText, Pattern.compile("(?i)</TEXT>"));

		System.out.println("idxsGrps.size=" + listIdxDescription.size());

		if (listIdxDescription.size() > 0) {

			// StringBuffer sb2 = new StringBuffer();
			StringBuffer sb = new StringBuffer("insert ignore into getContracts values ");
			for (int i = 0; i < listIdxDescription.size(); i++) {
				String contractDescription = listIdxDescription.get(i)[1]
						.replaceAll("(?is)(DESCRIPTION>|[\\$\\*\r\n\\%\\&\\)\\(<>\"\'\\[\\]])|\\\\|\\/", "");
				System.out.println("contractDescription=" + contractDescription);
				startIdxContract = Integer.parseInt(listIdxDescription.get(i)[0]);
				sIdx = Math.max(0, startIdxContract - 150);
				System.out.println("eIdx=" + startIdxContract + " sIdx=" + sIdx);
				typeText = ncFileText.substring(sIdx, startIdxContract);
				Matcher match = patternType.matcher(typeText);
				while (match.find()) {
					type = match.group().replaceAll("(?is)(TYPE>|\r\n)", "").replaceAll("<", "");
				}

				System.out.println("TYPE==" + type);

				if (type.toUpperCase().contains("GRAPHIC") || contractDescription.toUpperCase().contains("GRAPHIC"))
					continue;

				sb.append("('" + acc + "','" + fileDate + "','" + cik + "','" + formType + "','"
						+ companyName.replaceAll("[\\\\'\\&><]", "-") + "','" + i + "','" + type + "','"
						+ contractDescription + "'),\r");

				for (int n = 0; n < listIdxsTextEnd.size(); n++) {
					if (listIdxsTextEnd.get(n) > startIdxContract) {
						contractText = ncFileText.substring(startIdxContract, listIdxsTextEnd.get(n));
						break;
					}

				}

				contractLongName = contractDescription.substring(0, Math.min(150, contractDescription.length()))
						.replaceAll("[^a-zA-Z]{1,}", "_");

				System.out.println("contractLongName=" + contractLongName);

				contractCount++;

				// this determines what contracts to include and exclude
				if (nlp.getAllIndexEndLocations(contractLongName, Pattern.compile("(?i)indenture|agreement|contract"))
						.size() > 0
						&& nlp.getAllIndexEndLocations(contractLongName,
								Pattern.compile("(?i)report|eligibili|form.{1,3}t|report")).size() == 0) {
					text = contractText;
					String year = fileDate.substring(0, 4);
					System.out.println("fileDate=" + fileDate);
					String moStr = fileDate.replaceAll("-", "").substring(4, 6);
					System.out.println("mosStr=" + moStr);
					if (moStr.substring(0, 1).equals("0")) {
						moStr = moStr.substring(1, 2);
						System.out.println("2mosStr=" + moStr);
					}
					int qtr = (Integer.parseInt(moStr) + 2) / 3;
					System.out.println("qtr=" + qtr);
					FileSystemUtils.createFoldersIfReqd(downloadFolder + year + "/QTR" + qtr + "/");
					PrintWriter pwDownloaded = new PrintWriter(new File(downloadFolder + year + "/QTR" + qtr + "/" + cik
							+ "_" + fileDate.replaceAll("-", "").substring(0, 8) + "_" + acc + "_" + i + "_"
							+ contractLongName + ".txt"));

					System.out.println("downloaded folder=" + downloadFolder + year + "/QTR" + qtr + "/" + cik + "_"
							+ fileDate.replaceAll("-", "").substring(0, 8) + "_" + acc + "_" + i + "_"
							+ contractLongName + ".txt" + "\rcontract text.len=" + text.length());
					pwDownloaded.append(text);
					pwDownloaded.close();

//					text = removePageNumber(text);
					// need to clean up danglinge spaces so patterns can match.
//					text = text.replaceAll("[ ]{2,}[\r\n]{1}", "\r");
//					text = solrBeginField + getContractIndexSections(text)
//							+ solrEndField;
					// System.out.println("getContractTextOnly=="+text);
//							System.out.println("final filePath of solr doc="+f.getAbsolutePath());
//							PrintWriter getKparts = new PrintWriter
//							(new File(contractsSolrFolder+acc+"_"+i+"_"+contractLongName+".txt"));	
//							getKparts.append(text);
//							getKparts.close();

				}
//				if(f.exists())
//					f.delete();
			}
//			String query = sb.toString().substring(0,
//					sb.toString().length() - 2)
//					+ ";\r";
			if (contractCount > 0) {
//			MysqlConnUtils.executeQuery(query);
			}

			sb.deleteCharAt(0);
		}
	}

	public String removePageNumber(String text) throws IOException, SQLException {

		// text = text.replaceAll("[\r\n]{3,50}", "\r\r");

		// gets rid of stranded period (period that starts line)
		text = text.replaceAll("[\r\n]{1,3}\\.", "\\.").replaceAll("[\\.]{2,}", "");

		// gets rid of page # (eg: -1- or - 1 - ).
		text = patternPageNumber.matcher(text).replaceAll("\r\\[wasPgNumber\\]\r");

		// Pattern patternJoinParaAfterPageNumber = Pattern
		// .compile("(?<!([;,\\.]|[,;]{1}
		// (or|and)))(?<=[a-z\\)]{1})[\r\n]{1,2}xxxx[\r]{1,3}(?!(\\([A-Za-z\\d]{1,5}\\)))");

		PrintWriter tempPw3 = new PrintWriter(new File("c:/getContracts/temp/temp33.txt"));
		tempPw3.append(text);
		tempPw3.close();

		System.out.println("finished getContractTextOnly");
		return text;
	}

	public static String getOnlyAlphaCharacters(String text) {

		if (text == null)
			return null;
		text = text.replaceAll("\"", "").replaceAll("[^A-Za-z\\d]", "_").replaceAll("[_]{2,}", "_");

		return text;
	}

	public void getWord(String filename, String text) throws SQLException, FileNotFoundException {

		String acc = filename.substring(0, 20);
		String exhibitNo = filename.substring(20, 22).replaceAll("_", "");
		contractLongName = filename.substring(22, filename.length()).replaceAll("_", "");

		text = text.replaceAll("([\r\n]{1,})", "\r ").replaceAll("([\r\n]{1}[ \\s]{1,}[\r\n]{1})", "\r").replaceAll("'",
				"\\\\'");

		StringBuffer sb = new StringBuffer(
				"\r SET global max_allowed_packet = 1024 * 1024 * 1024;\r" + "insert ignore into getWord values ");

		/*
		 * PrintWriter tempPw2 = new PrintWriter(new
		 * File("c:/getContracts/temp/temp2.txt")); tempPw2.append(text);
		 * tempPw2.close();
		 */

		String word;
		String[] words = text.split(" ");
		for (int i = 0; i < words.length; i++) {
			if (i >= 100000)
				break;
			word = words[i].replaceAll("[^a-zA-Z\\d\\.]", "").replaceAll("\\.$", "").trim();
			if (i > 0 && word.length() > 2 && i < 100000)
				sb.append(
						"\r,('" + acc + "','" + exhibitNo + "','" + contractLongName + "','" + i + "','" + word + "')");
			else if (word.length() > 2 && i < 100000)
				sb.append(
						"\r('" + acc + "','" + exhibitNo + "','" + contractLongName + "','" + i + "','" + word + "')");
		}

		/*
		 * File file = new File("c:/getContracts/temp/temp.txt"); PrintWriter tempPw3 =
		 * new PrintWriter(file); tempPw3.append(sb.toString() + ";"); tempPw3.close();
		 */

		// MysqlConnUtils.executeQuery(sb.toString()+";");
	}

	public String getContractIndexSections(String text) throws IOException, SQLException {

		// NOTE: CANNOT CHANGE LENGTH OF TEXT BECAUSE WE NEED TO BACKFILL GAPS
		// WITH SOLR FIELDS AND IF WE CHANGE TEXT LENGTH I CORRUPT THE ABILITY
		// TO MAKE SOLR FIELDS FOR TEXT NOT CAPTURED BY SECTION RULESET. SO NO
		// REPLACES

		text = "\r" + text.replaceAll("etc.", "etc").replaceAll("Etc.,", "Etc,");
		String solrEnd = "]]></field>";
		String solrStart = "\"><![CDATA[";

		tocStartIdx = 0;
		tocEndIdx = 0;
		toc = null;
		toc = getContractToc(text);
//		System.out.println("toc===" + toc + "!end toc");
		toc = "<field name=\"toc" + solrStart + toc + "]]></field>\r";

		// for any dynamic field name - pass the name to this method:
		// getOnlyAlphaCharacters
		String definedTerms = getDefinedTerms(text);

		exhStartIdx = 0;
		exhEndIdx = 0;

		String exhibits = getExhibitIdxAndNames(text);
		if (exhStartIdx > 0)
			text = text.substring(0, exhStartIdx);

		// if sections from K - no exhibitName to parse
		String sections = getSectionIdxsAndNames(text, false, "");

		text = "<field name=\"text" + solrStart + text + solrEnd;
		text = text + definedTerms + "\r" + toc + "\r" + sections;
		return text;
	}

	public static String getContractToc(String text) throws IOException {

		// returns start and endIdx of toc. return each section/exhibit in TOC
		// formmatted for solr

		StringBuffer sb = new StringBuffer();

		Matcher matcher = patternContractTocWithExhibits.matcher(text);

		String toc = null, tocPrev = null, tocPrev4 = null, tocPrev3 = null, tocPrev2 = null, firstMatch = null,
				lastMatch = null;
		int sIdxPrev = -1000, eIdxPrev = -1000, eIdxPrev2 = -1000, eIdxPrev3 = -1000, eIdxPrev4 = -1000,
				sIdxPrev4 = -1000, sIdxPrev3 = -1000, sIdxPrev2 = -1000, sIdx, eIdx, cnt = 0, dist = 1000;

		while (matcher.find()) {

			System.out.println("toc match=" + matcher.group() + " idx=" + matcher.start() + "|");

			toc = matcher.group();
			sIdx = matcher.start();
			eIdx = matcher.end();

			if ((sIdx - eIdxPrev) < dist && (sIdxPrev - eIdxPrev2) < dist && (sIdxPrev2 - eIdxPrev3) < dist
					&& (sIdxPrev3 - eIdxPrev4) < dist) {
				if (cnt == 0) {
					sb.append(tocPrev4 + "\r" + tocPrev3 + "\r" + tocPrev2 + "\r" + tocPrev + "\r" + toc);
					tocStartIdx = sIdxPrev4;
					tocEndIdx = eIdx;
					firstMatch = tocPrev4;
					lastMatch = toc;
				} else
					sb.append("\r" + toc);
				{
					tocEndIdx = eIdx;
					lastMatch = toc;
					cnt++;
				}
				// System.out.println("toc="+toc);

			}
			// if above not met not TOC so reset cnt=0 so cks above again next
			// loop
			else if (cnt < 4) {
				cnt = 0;
				tocStartIdx = null;
				tocEndIdx = null;
			}
			// cnt>=4 - 4 toc Lines previously found but then 1st if condition
			// above not met - so return TOC.

			else if (cnt >= 4) {

				// mapOfAllContractParts=
				// key=start idx, String[]: [0]=endIdx of last match, [1]=type
				// (eg,TOC,Section,Exhibit), [2]=first text match, [3]=last text
				// match

//				String[] ary = { tocEndIdx + "", "TOC", firstMatch, lastMatch };
//				mapOfAllContractParts.put(tocStartIdx, ary);
//
//				System.out.println("tocStartIdx=" + tocStartIdx + " tocEndIdx="
//						+ tocEndIdx + " toc=" + sb.toString() + "|tocEnd");
				return sb.toString();
			}

			tocPrev4 = tocPrev3;
			tocPrev3 = tocPrev2;
			tocPrev2 = tocPrev;
			tocPrev = toc;
			sIdxPrev4 = sIdxPrev3;
			sIdxPrev3 = sIdxPrev2;
			sIdxPrev2 = sIdxPrev;
			sIdxPrev = sIdx;
			eIdxPrev4 = eIdxPrev3;
			eIdxPrev3 = eIdxPrev2;
			eIdxPrev2 = eIdxPrev;
			eIdxPrev = eIdx;

		}

		// mapOfAllContractParts=
		// key=start idx, String[]: [0]=endIdx of last match, [1]=type
		// (eg,TOC,Section,Exhibit), [2]=first text match, [3]=last text
		// match

		// String[] ary = { tocEndIdx + "", "TOC", firstMatch, lastMatch };
		// mapOfAllContractParts.put(tocStartIdx, ary);

		return sb.toString();
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

			// System.out.println("matched definedTerm=" + match.group()
			// + "|end matched definedTerm");

			firstLtr = match.group().replaceAll("[\"\r\n ]", "").substring(0, 1);
			ltr = getNumberInAlphabet(firstLtr);
			if (priorLtr > ltr) {
				definedTerm = prevMatch;
				definition = getLastDefinedTerm(text.substring(sIdxPrev), prevMatch, sIdxPrev);
				sb.append(solrDefTerm + definedTerm + solrEnd + "\r");
				sb.append((solrDef + getOnlyAlphaCharacters(definedTerm)) + solrStart + definition + solrEnd + "\r");

				// System.out.println("definition="+definition);
			} else if (cnt > 0) {
				definedTerm = prevMatch;
				definition = text.substring(sIdxPrev, sIdx);
				sb.append(solrDefTerm + definedTerm + solrEnd + "\r");
				sb.append((solrDef + getOnlyAlphaCharacters(definedTerm)) + solrStart + definition + solrEnd + "\r");
				// System.out.println("definition="+definition);
				// System.out.println("definedTerm="+definedTerm+"
				// \rdefinition"+definition+"|defEnd");
			}

			priorLtr = ltr;
			sIdxPrev = sIdx;
			prevMatch = match.group().replaceAll("([\r\n\"]+|[\r\n]+[ ]+)", "").trim();
			// System.out.println("definedTerm="+match.group().replaceAll("([\r\n\"]+|[\r\n]+[
			// ]+)", "").trim());
			cnt++;

		}

		// get last match
		definedTerm = prevMatch;
		definition = getLastDefinedTerm(text.substring(sIdx), prevMatch, sIdx);
		sb.append(solrDefTerm + definedTerm + solrEnd + "\r");
		sb.append((solrDef + getOnlyAlphaCharacters(definedTerm)) + solrStart + definition + solrEnd + "\r");
		// System.out.println("definition="+definition);

		// System.out.println("last definition="+definition+"|end last def");

		return sb.toString();

	}

	public static String getLastDefinedTerm(String text, String match, int idx) {

		// simplify to have it just be first instance of 2 hard returns that
		// follow first line with a \\. after match. If not found - then don't cut
		// anything;

		String definition = "";
		System.out.println("match at getLastDefinedTerm=" + match);
		if (null == match)
			return null;

		String[] lines = text.substring(match.length(), Math.min(idx + 5000, text.length())).split("[\r\n]");
		System.out.println("text last==" + text.substring(match.length(), match.length() + 50));
		int cnt = 0;
		for (int i = 0; i < lines.length; i++) {
			if (i == 0) {
				System.out.println("lines[0]=" + lines[i]);
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

	public static int getNumberInAlphabet(String letter) {
		int ltrNo = -1;

		if (letter.equals("A"))
			ltrNo = 1;
		if (letter.equals("B"))
			ltrNo = 2;
		if (letter.equals("C"))
			ltrNo = 3;
		if (letter.equals("D"))
			ltrNo = 4;
		if (letter.equals("E"))
			ltrNo = 5;
		if (letter.equals("F"))
			ltrNo = 6;
		if (letter.equals("G"))
			ltrNo = 7;
		if (letter.equals("H"))
			ltrNo = 8;
		if (letter.equals("I"))
			ltrNo = 9;
		if (letter.equals("J"))
			ltrNo = 10;
		if (letter.equals("K"))
			ltrNo = 11;
		if (letter.equals("L"))
			ltrNo = 12;
		if (letter.equals("M"))
			ltrNo = 13;
		if (letter.equals("N"))
			ltrNo = 14;
		if (letter.equals("O"))
			ltrNo = 15;
		if (letter.equals("P"))
			ltrNo = 16;
		if (letter.equals("Q"))
			ltrNo = 17;
		if (letter.equals("R"))
			ltrNo = 18;
		if (letter.equals("S"))
			ltrNo = 19;
		if (letter.equals("T"))
			ltrNo = 20;
		if (letter.equals("U"))
			ltrNo = 21;
		if (letter.equals("V"))
			ltrNo = 22;
		if (letter.equals("W"))
			ltrNo = 23;
		if (letter.equals("X"))
			ltrNo = 24;
		if (letter.equals("Y"))
			ltrNo = 25;
		if (letter.equals("Z"))
			ltrNo = 26;

		return ltrNo;
	}

	public static String getExhibitIdxAndNames(String text) throws IOException {

		String tmpToc = toc.replaceAll("[ ]{2,}", " ");

		System.out.println("contractLongName=" + contractLongName);
		NLP nlp = new NLP();
		StringBuffer sb = new StringBuffer();
		StringBuffer sb2 = new StringBuffer();
		StringBuffer sb3 = new StringBuffer();

		String exhibits = null;

		int contractLength = text.length();
		listOfExhibitsIdxsAndNames.clear();

		// get prelim list - then ck each line follows exhibit for up to 2
		// lines that have text - check each line that has text and see if all
		// words > 5 chars are initial caps and if not - it is not an exhibit.

		List<String[]> prelimIdxExh = nlp.getAllStartIdxLocsAndMatchedGroups(text, patternExhibitInContract);

		if (prelimIdxExh.size() < 2 || prelimIdxExh == null)
			return exhibits;

		int preStartIdx, cnt;
		String exhibitMatch, exhSnip, firstLineStr = null, secondLineStr = null;
		String[] lines, words;
		boolean firstLine = true, secondLine = true, skip = false;
		for (int i = 0; i < prelimIdxExh.size(); i++) {
			firstLine = true;
			secondLine = true;
			skip = false;

			cnt = 0;
			exhibitMatch = prelimIdxExh.get(i)[1];
			// System.out.println("prelim exhibitMatch="+exhibitMatch);
			preStartIdx = Integer.parseInt(prelimIdxExh.get(i)[0]) + exhibitMatch.length();
			exhSnip = text.substring(preStartIdx, Math.min(preStartIdx + 400, text.length()));
			lines = exhSnip.split("[\r\n]");

			Matcher matchFalsePostiveExhibit;
			for (int a = 0; a < lines.length; a++) {
				if (skip)
					break;
				if (lines[a].replaceAll("[\\p{Punct} ]", "").length() > 1) {
					cnt++;
					matchFalsePostiveExhibit = patternPAGE_PRE_S_C_SEC_CODES.matcher(lines[a]);
					if (matchFalsePostiveExhibit.find() && cnt < 2 && !skip) {
						System.out.println("exclude this exhibit=" + lines[a] + " exhibitMatch=" + exhibitMatch);
						skip = true;
						break;
					}
				}
			}

			if (skip)
				continue;

			cnt = 0;
			System.out.println(exhibitMatch);
			for (int c = 0; c < lines.length; c++) {
				if (cnt > 1)
					break;
				// System.out.println("line.repl="+lines[c].replaceAll("(<PAGE>|<S>|<C>|<PRE>)",
				// "").replaceAll("[a-z\\p{Punct} \\[\\]_]", ""));

				if (lines[c].replaceAll("(<PAGE>|<S>|<C>|<PRE>)", "").replaceAll("[a-z\\p{Punct} \\[\\]_]", "")
						.length() > 1) {
					cnt++;
					words = lines[c].replaceAll("[ ]{2,}", " ").split(" ");
					for (int n = 0; n < words.length; n++) {
						if (words[n].replaceAll("[\\[\\]\\%_\\p{Punct}\\(\\)]{2,}", "").length() > 4
								&& words[n].replaceAll("[a-z\\p{Punct}]", "").length() < 1) {
							if (cnt == 1) {
								firstLine = false;
								System.out.println("firstLine=false");
								break;
							}
							if (cnt == 2) {
								// secondLine = false;
								// System.out.println("secondLine=false");
							}
						}

						else if (cnt == 1) {
							firstLineStr = lines[c];
							// System.out.println("firstLineStr="+firstLineStr);
							firstLine = true;
						} else if (cnt == 2) {
							secondLineStr = lines[c];
							// System.out.println("secondLineStr="+secondLineStr);
							secondLine = true;
						}
					}
				}
				if (cnt == 1 && !firstLine)
					continue;
			}

			// System.out.println("out of loop to get exhibit name and 1st 2 lines
			// cnt="+cnt);

			if (cnt == 2 && firstLine && secondLine) {
				// System.out.println("add final exh list first and seccondLineStr=");
				String[] ary = { prelimIdxExh.get(i)[0],
						prelimIdxExh.get(i)[1] + "\r" + firstLineStr + "\r" + secondLineStr };
				listOfExhibitsIdxsAndNames.add(ary);
			}
			if (cnt == 2 && firstLine && !secondLine) {
				String[] ary = { prelimIdxExh.get(i)[0], prelimIdxExh.get(i)[1] + "\r" + firstLineStr };
				listOfExhibitsIdxsAndNames.add(ary);
			}
		}

		// if less than 2 idxs found - skip
		if (listOfExhibitsIdxsAndNames.size() < 2 || listOfExhibitsIdxsAndNames == null)
			return exhibits;

		int startIdx, nextStartIdx = 0;
		String lastMach, exhibitName;
		String firstMatch = listOfExhibitsIdxsAndNames.get(0)[1];

		int firstIdx = Integer.parseInt(listOfExhibitsIdxsAndNames.get(0)[0]);
		for (int i = 0; i < listOfExhibitsIdxsAndNames.size(); i++) {
			startIdx = Integer.parseInt(listOfExhibitsIdxsAndNames.get(i)[0]);

			if ((i + 1) < listOfExhibitsIdxsAndNames.size()) {
				nextStartIdx = Integer.parseInt(listOfExhibitsIdxsAndNames.get(i + 1)[0]);

				// System.out.println("exhibit="
				// + listOfExhibitsIdxsAndNames.get(i)[1].replaceAll(
				// "[\r\n]|[ ]{2,}", " ") + " |sIdx /txt.len="
				// + ((double) startIdx / (double) contractLength)
				// + " exhibit full text="
				// + text.substring(startIdx, nextStartIdx));

				// get first line of exhibit - and match it to toc long name:
				String[] exhLines = listOfExhibitsIdxsAndNames.get(i)[1].split("\r\n");
				String exhMatch = null;
				Matcher matchExh;
				Pattern patternExhibit = Pattern
						.compile("(?i)(EXHIBIT|ANNEX|APPENDIX|SCHEDULE) [A-Za-z\\d\\-\\_]{1,6}");
				for (int b = 0; b < Math.min(3, exhLines.length); b++) {
					matchExh = patternExhibit.matcher(exhLines[b]);

					// TODO XX: finds exhibit pattern found in body in tmpToc
					if (matchExh.find()) {

						exhMatch = matchExh.group().replaceAll("[ ]{2,}", " ");
						System.out.println("exhMatch=" + exhMatch);

						Pattern patternMatchedExhibit = Pattern.compile("(?i)" + exhMatch + ".+[\r\n]");
						matchExh = patternMatchedExhibit.matcher(tmpToc);
						if (matchExh.find()) {
							exhMatch = matchExh.group();
							System.out.println("exhMatchToc=" + exhMatch);
						}
					}
				}

				System.out.println("exhibit names=" + listOfExhibitsIdxsAndNames.get(i)[1]);
				String tmp = (getSectionIdxsAndNames(text.substring(startIdx, nextStartIdx), true,
						listOfExhibitsIdxsAndNames.get(i)[1]));

//				 System.out.println("tmp--" + tmp);

				// exhSec -- add boolean that if true will pop 'exh' prior to each field in
				// getSec
				// exhDef -- add boolean that if true will pop 'exh' prior to each field in
				// getSec

				sb.append("<field name=\"Exhibit\"><![CDATA[" + text.substring(startIdx, nextStartIdx) + "]]></field>");
			}

			if ((i + 1) == listOfExhibitsIdxsAndNames.size() && ((double) startIdx / (double) contractLength) > .85) {

				// System.out.println("last exhibit="
				// + listOfExhibitsIdxsAndNames.get(i)[1].replaceAll(
				// "[\r\n]|[ ]{2,}", " ") + " |sIdx /txt.len="
				// + ((double) startIdx / (double) contractLength)
				// + " last exhibit full text="
				// + text.substring(startIdx, text.length()));

				sb.append(
						"<field name=\"Exhibit\"><![CDATA[" + text.substring(startIdx, text.length()) + "]]></field>");

			}

			else {// fails to get last Exh - so stop at last successful

				// mapOfAllContractParts=
				// key=start idx, String[]: [0]=endIdx of last match, [1]=type
				// (eg,TOC,Section,Exhibit), [2]=first text match, [3]=last text
				// match

				lastMach = text.substring(startIdx, Math.min(startIdx + 2000, text.length()));
			}

		}

		// All Exhibits are successful and last one is end of text
		// System.out.println("exhibits="+sb.toString());
		return sb.toString();
	}

	public static String getSectionIdxsAndNames(String text, boolean exhibit, String exhibitName) throws IOException {
		// secStartIdx = null; secEndIdx = null;

		System.out.println("@ getSectionIdxsAndNames");
		// CANNOT CHANGE LENGTH OF TEXT BECAUSE WE NEED TO BACKFILL GAPS WITH
		// SOLR FIELDS AND IF WE CHANGE TEXT LENGTH I CORRUPT THE ABILITY TO
		// MAKE SOLR FIELDS FOR TEXT NOT CAPTURED BY SECTION RULESET. SO NO
		// REPLACES

		StringBuffer sb = new StringBuffer();

		NLP nlp = new NLP();

		List<String[]> listPrelimIdxAndSection = nlp.getAllStartIdxLocsAndMatchedGroups(text, patternSectionHeading);

		if (listPrelimIdxAndSection.size() < 1)
			return null;

		List<String[]> listIdxAndSection = new ArrayList<>();

		String sectSubStr, firstLtr, sectionHdg = null, idxStr = null, idxStr2;
		Double nextSecNo = null, secNo = null, secChg = null;
		Integer secChgInt = null;
		String lastSection = null;
		String[] words;
		Integer idx = null, idx2 = null;

		/*
		 * Filter used to get sectionHeading: sectionHeadingpattern requires
		 * sectionHeading be preceded by a hard return. Then I require 1st word in
		 * section name is Initial Caps and all words that have more than 5 characters
		 * on line in sectionHeading be Initial Caps (this captures what is generally
		 * the section heading format. This isn't going to be 100% - but its the best we
		 * can do. Some section headings may just have first word be initial caps but
		 * there's on way to do this). The above will then be all section headings. Then
		 * I need to determine the end of section text - if the next section is in
		 * sequence (e.g., 1.02 to 1.03) -- the section text is from the start of
		 * current section to start of next. If next section is not in sequence - I will
		 * capture all text that meet paragraph style conditions provided it ends before
		 * next section that is out of sequence.
		 */

		System.out.println("listPrelimIdxAndSection.size=" + listPrelimIdxAndSection.size());

		for (int i = 0; i < listPrelimIdxAndSection.size(); i++) {
			idxStr = listPrelimIdxAndSection.get(i)[0];
			idx = Integer.parseInt(idxStr);

			if (i + 1 < listPrelimIdxAndSection.size()) {
				idxStr2 = listPrelimIdxAndSection.get(i + 1)[0];
				idx2 = Integer.parseInt(idxStr2);
			}
			// TODO: if pass exhibits as excerpts from larger text - then I need to
			// tell it to skip this.

			System.out.println("Section===========" + listPrelimIdxAndSection.get(i)[1] + " tocStartIdx=" + tocStartIdx
					+ " tocEndIdx=" + tocEndIdx + " idx=" + idx + "idx2=" + idx2);
			if (null == idx2 || null == idx)
				continue;

			if (tocEndIdx == null) {
				tocEndIdx = 0;
				tocStartIdx = 0;
			}
			if ((idx <= tocEndIdx && idx >= tocStartIdx) || (idx2 <= tocEndIdx && idx2 >= tocStartIdx)) {
				System.out.println("skip");
				continue;
			}

			System.out.println("Section2=========2" + listPrelimIdxAndSection.get(i)[1] + " tocStartIdx=" + tocStartIdx
					+ " tocEndIdx=" + tocEndIdx + " idx=" + idx + "idx2=" + idx2);

			sectionHdg = listPrelimIdxAndSection.get(i)[1];

			// gets rid of "Section #" and gets just sec name.

			sectSubStr = sectionHdg.replaceAll("(?i)section.*?[\\da-zA-Z].*?(?=[a-zA-Z\\(])", "").replaceAll(" ", "")
					.replaceAll("[\r\n]", "");

			System.out.println("sectSubStr=" + sectSubStr);

			// if no word after "Section
			if (sectSubStr.length() < 1)
				continue;

			// if 1st ltr not initial cap (cap) continue
			firstLtr = sectSubStr.substring(0, 1);
			// System.out.println("section firstLtr="+firstLtr);
			Pattern alpha = Pattern.compile("[A-Z]{1}");

			Matcher mAlph;
			mAlph = alpha.matcher(firstLtr);
			boolean cap = false;
			if (mAlph.find())
				cap = true;

			if (!cap)
				continue;

			// if sec name has word w/ 6 chars & its not initial caps: skip
			// get just sectionName ==>

			String sectionName = sectionHdg.replaceAll("(?i)section.*?[\\da-zA-Z].*?(?=[a-zA-Z\\(])", "");

			words = sectionName.replaceAll("[\r\n]", "").replaceAll("[ ]2,", " ").split(" ");
			for (int c = 0; c < words.length; c++) {
				mAlph = alpha.matcher(words[c]);
				if (!mAlph.find() && words[c].length() > 5) {
					System.out.println("breaking here=" + sectionHdg + " word=" + words[c]);
					cap = false;
					break;
				}
			}

			if (!cap)
				continue;

			// sections # scrubbed: first word must be initial caps and any
			// words more than 5 chars. Now I check if current sec# is in sequence
			// with next and if it is - cut from cur idx to next, if not cut from
			// cut to end of paragraph.

			String[] ary = { idxStr, sectionHdg };
			listIdxAndSection.add(ary);

		}

		// get Sec# from secHdg and if in sequence text is startLoc to startLoc
		// next if not - employ paragraph capture technique that ends prior to
		// next sec out of sequence.

		Pattern patternSectionNumber = Pattern.compile("[\\d]{1,3}[\\.]{0,1}[\\d]{0,2}(?=[\\. ])");

		String solrSecHdg = null, solrSec = null, solrEnd = "]]></field>\r", solrData = "\"><![CDATA[";
		boolean lastSectionFound = false;
		String sectionName = null, firstMatch = null, lastMatch = null, firstDecStr, nextDecStr;
		Integer startSecIdx = null, nextStartSecIdx = null, lastIdx = null, firstDec, nextDec, decChg = null;
		for (int i = 0; i < listIdxAndSection.size(); i++) {

			startSecIdx = Integer.parseInt(listIdxAndSection.get(i)[0]);

			if (i == 0) {
				// secStartIdx = startSecIdx;
				firstMatch = listIdxAndSection.get(i)[1];
			}

			if (i + 1 == listIdxAndSection.size()) {
				lastIdx = startSecIdx;
				// if last section and closing para found lastSection is cut
				// below
				Matcher m = patternSectionHeadingWithClosingParagraph
						.matcher(text.substring(lastIdx, Math.min(lastIdx + 10000, text.length())));
				lastMatch = listIdxAndSection.get(i)[1];
				System.out.println("lastMatch=" + lastMatch);
				if (m.find()) {
					// System.out.println("m.start=" + m.start() + " lastIdx="
					// + lastIdx + " text.len=" + text.length());
					// System.out.println("lastSection using closingPara=="
					// + text.substring(lastIdx, m.end() + lastIdx));
					lastSectionFound = true;
					lastSection = text.substring(lastIdx, m.end() + lastIdx);
				}
			}

			if ((i + 1) < listIdxAndSection.size()) {
				nextStartSecIdx = Integer.parseInt(listIdxAndSection.get(i + 1)[0]);
			}

			System.out.println("idx=" + listIdxAndSection.get(i)[0].replaceAll("[\r\n]", "")
					+ " prelim sectionHeadings=" + listIdxAndSection.get(i)[1].replaceAll("[\r\n]", ""));

			Matcher match = patternSectionNumber.matcher(listIdxAndSection.get(i)[1]);

			Matcher match2 = null;

			if ((i + 1) < listIdxAndSection.size()) {
				match2 = patternSectionNumber.matcher(listIdxAndSection.get(i + 1)[1]);
			}

			if (match != null && match2 != null && match.find() && match2.find()) {

				firstDecStr = match.group().replaceAll("[\\d].*\\.", "").trim();
				nextDecStr = match2.group().replaceAll("[\\d].*\\.", "").trim();

				if (firstDecStr != null && nextDecStr != null && firstDecStr.length() > 0 && nextDecStr.length() > 0
						&& firstDecStr.replaceAll("[\\d]", "").length() < 1
						&& nextDecStr.replaceAll("[\\d]", "").length() < 1) {
					System.out.println("firstDecStr=" + firstDecStr + " nextDecStr=" + nextDecStr);
					firstDec = Integer.parseInt(firstDecStr);
					nextDec = Integer.parseInt(nextDecStr);
					decChg = Math.abs(firstDec - nextDec);

				} else if (firstDecStr.replaceAll("[\\d]", "").length() > 1
						&& nextDecStr.replaceAll("[\\d]", "").length() > 1 || firstDecStr == null
						|| nextDecStr == null) {
					decChg = null;
				}

				System.out.println("firstDecStr=" + firstDecStr + " nextDecStr=" + nextDecStr);
				System.out.println("next match=" + match2.group() + " match=" + match.group());

				secNo = (double) Math.round(Double.parseDouble(match.group()) * 100) / 100;
				nextSecNo = (double) Math.round(Double.parseDouble(match2.group()) * 100) / 100;
				secChg = ((double) (Math.round(nextSecNo * 100) - Math.round(secNo * 100))) / 100;
				secChgInt = nextSecNo.intValue() - secNo.intValue();
				System.out.println("secChg=" + secChg + " secChgInt=" + secChgInt + " lastSecNo=" + secNo
						+ " nextSecNo=" + nextSecNo + " decChg=" + decChg);
			}

			// if last digit increaes by 1 (sec#) or first digit (Article) - cut
			// from start to start. If there isn't a next idx (i+1)=size - skip
			// b/c can't go next to next

			sectionName = listIdxAndSection.get(i)[1].replaceAll("(?i)section.*?[\\da-zA-Z].*?(?=[a-zA-Z\\(])", "||")
					.replaceAll(".*\\|\\|", "").replaceAll("[\r\n]", "").replaceAll("\\.", "");

			if ((i + 1) < listIdxAndSection.size() && ((null != secChg && secChg == 0.01)
					|| (null != secChg && secChg == .1) || (null != secChg && secChg == 1)
					|| (null != secChgInt && secChgInt == 1) || (null != decChg && decChg == 1))) {

				// cut from current idx to next. Exclude Article Heading if possible
				System.out.println("sectionName===" + sectionName);
				if (exhibit) {
					exhibitName = getOnlyAlphaCharacters(exhibitName);
					solrSecHdg = "<field name=\"" + exhibitName + "_exhSecHdg\"><![CDATA[";
					solrSec = "<field name=\"" + exhibitName + "_exhSec_";
				}

				if (!exhibit) {
					solrSecHdg = "<field name=\"sectionHeading\"><![CDATA[";
					solrSec = "<field name=\"section_";
				}

				sb.append(solrSecHdg + sectionName + solrEnd);
				sb.append(solrSec + getOnlyAlphaCharacters(sectionName) + solrData
						+ text.substring(startSecIdx, nextStartSecIdx) + solrEnd);
			}

			else if (lastSectionFound) {
				if (exhibit) {
					exhibitName = getOnlyAlphaCharacters(exhibitName);
					solrSecHdg = "<field name=\"" + exhibitName + "_exhSecHdg\"><![CDATA[";
					solrSec = "<field name=\"" + exhibitName + "_exhSec_";
				}
				if (!exhibit) {
					solrSecHdg = "<field name=\"sectionHeading\"><![CDATA[";
					solrSec = "<field name=\"section_";
				}
				sb.append(solrSecHdg + sectionName + solrEnd);
				sb.append(solrSec + getOnlyAlphaCharacters(sectionName) + solrData + lastSection + solrEnd);
			}

			else if (!lastSectionFound) {

				System.out.println(" start portion of text passed to section lastSection="
						+ text.substring(startSecIdx, startSecIdx + 200));
				lastSection = getLastDefinedTerm(text.substring(startSecIdx), listIdxAndSection.get(i)[1], startSecIdx);
				System.out.println("section lastsection===" + lastSection);

				if (exhibit) {
					exhibitName = getOnlyAlphaCharacters(exhibitName);
					solrSecHdg = "<field name=\"" + exhibitName + "_exhSecHdg\"><![CDATA[";
					solrSec = "<field name=\"" + exhibitName + "_exhSec_";
				}
				if (!exhibit) {
					solrSecHdg = "<field name=\"sectionHeading\"><![CDATA[";
					solrSec = "<field name=\"section_";
				}
				sb.append(solrSecHdg + sectionName + solrEnd);
				sb.append(solrSec + getOnlyAlphaCharacters(sectionName) + solrData + lastSection + solrEnd);
			}
		}

		return sb.toString();
	}

	public void getContractSentence(String text) throws IOException {
		NLP nlp = new NLP();

		text = nlp.getSentence(text, NLP.patternContractSentenceStart, NLP.patternContractSentenceStart);

	}

	public static void main(String[] args) throws ParseException, SocketException, IOException, SQLException {

//		acc = "1";
//		String beginSolrFields = "<add>\r" + "<doc>\r<field name=\"id\">"
//				+ "0000950162-02-000079_2</field>\r"
//				+ "<field name=\"contract\">INDENTURE</field>\r"
//				+ "<field name=\"filer\">"
//				+ "VAIL RESORTSINC</field>\r"
//				+ "<field name=\"fileDate\">"
//				+ "2000-01-01T00:00:00Z</field>\r";
//
//		String endSolrFields = "</doc>\r" + "</add>";

		Contracts gK = new Contracts();
//		type = "indenture";
		// String text = Utils.readTextFromFile("c:/temp/tmp3.txt");
		// String exhibits = getExhibitIdxAndNames(text);

//		 gK.getContract("c:/getContracts/0000882377-07-002295.txt");

		/*
		 * NOTES: Current program will parse contract above and properly label defs and
		 * sections in body of contract. Before that is done the exhibits are separated
		 * and parsed to identify sections/definitions. Same exact solr methodology as
		 * contract except add exh[Name] as a prefix to each field--this is where I left
		 * off. To get the proper exhibits I require the exhibit name match the name in
		 * toc. I do this b/c the toc name of exhibit is reliable. I then have to use
		 * that as the dynamic field name. I can attempt other methods to get the
		 * exhibit name w/o it being in TOC but all seem unreliable. Other method may be
		 * to just capture first 3 lines as Exhibit pattern match and treat that as a
		 * 'general' exhibit field so that at least exhibit names can still be searched
		 * quickly. Later I can use solr to find common words that help designate what a
		 * regex pattern should contain in order to isolate specific types of exhibits
		 */

		// TODO: Pickup at finishing exhibit name (see TODO XX) which gets
		// passed to getSections together with boolean=true (is exhibit) -- need
		// to pass exhibit name so that I can create dynamic field:

		// TODO: Type in sec.gov html must be 'EX..' and not for example '8-k'.
		// It should be in .nc file

		// https://www.sec.gov/Archives/edgar/data/1407876/000088237707002295/p07-1039_ex995.htm
		// TODO: above good example of def that is Initial Caps: w/o "

		// gK.getContract("c:/getContracts/0000916641-02-000037.txt");
		// above is good for perfecting exhibit process - SEE BELOW.
		// necessary to make definitions/sections relate only to the
		// contract or specific exhibit -- discuss with PRAVEEN how to do that.
		// I can limit sectionHeading/section/definedTerm/definition to just
		// contract text and then when I run against exhibits to precede each
		// with "exhibit_[Name]_definedTerm" but not sure. This may still prove
		// useful, but unclear how much benefit there is here.

		// TODO: keep Defs as dynamic field - useful b/c highly reliable way to
		// parse and beneficial to be able to limit searches to just defined
		// terms.

		// TODO: don't capture docs that are less than a certain size (less than 2kb?).

		// TODO: see if I can determine why I am getting these corrupted forms
		// of hard returns - hard return that are not recognized in asci. Get
		// rid of them if possible.

		// TODO: Use the strip page #s.

		// TODO: Keep testing if 90 plus rankings always work! and if we loosen
		// if a group of 80 ranks are close -and then do we grab entire
		// subsection and run compare? re-order to get better compare?

		// TODO: Get rid of corrupted docs (some are pdf scan and when I
		// download them and try to parse them as txt it produces gobbly gook.
		// See 0001327603-07-000055_2_JUNIOR_SUBORDINATED_INDENTURE.txt).

		// TODO: how to deal with searches in solr where there is a ':' or a '('
		// or ')'. This will cause no results to be displayed.

		@SuppressWarnings("resource")
		Scanner Scan = new Scanner(System.in);
		System.out.println("Enter start date of time period to check for contracts to parse(yyyymmdd)");
		String startDateStr = Scan.nextLine();
		int year = Integer.parseInt(startDateStr.substring(0, 4));
		System.out.println("Enter start date of time period to check for contracts to parse (yyyymmdd)");
		String endDateStr = Scan.nextLine();

		System.out.println("Enter quarter");
		String qtrStr = Scan.nextLine();
		int qtr = Integer.parseInt(qtrStr);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date sDate = new Date();
		sDate = sdf.parse(startDateStr);
		Calendar startDate = Calendar.getInstance();
		startDate.setTime(sDate);
		Date eDate = new Date();
		eDate = sdf.parse(endDateStr);
		Calendar endDate = Calendar.getInstance();
		endDate.setTime(eDate);
		String earliestDateStr = "19930101";
		Date firstDate = sdf.parse(earliestDateStr);
		Calendar badDate = Calendar.getInstance();
		badDate.setTime(firstDate);

		if (endDate.before(startDate)) {
			System.out.println("End date must be later than start date. Please re-enter.");
			return;
		}
		if (endDate.after(Calendar.getInstance())) {
			System.out.println("End date cannot be later than today. Please re-enter.");
			return;
		}

		// TODO: this will extract from secZipFiles already downloaded each
		// contract not filtered out. In order to download more files call
		// DownloadSecZipFiles

		// ContractParser cp = new ContractParser();
		// cp.parseAllFiles(contractsFolder_Raw);

		InsiderParser.getMasterIdx(year, qtr, endDate);
		File f = new File("c:/backtest/insider/" + year + "/qtr" + qtr + "/master.idx");
		System.out.println("f.exist=" + f.exists());
		if (f.exists()) {
			String folder = "c:/backtest/master/" + year + "/QTR" + qtr + "/";
			FileSystemUtils.createFoldersIfReqd(folder);
			File targetFilename = new File(folder + f.getName());
			FileSystemUtils.copyFile(f, targetFilename);
		}

		// THIS WILL RETRIEVE FROM secZipFiles any agreements specified.
		gK.dateRangeQuarters(startDate, endDate);
		// contract saved to folder in -
		// accno_#_cik_fileDate_description format

	}
}
