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

//import com.sun.tools.sjavac.server.SysInfo;

//import algo_testers.GolawSearch;
import ucar.ma2.ArrayDouble.D3.IF;
import xbrl.MysqlConnUtils;
import xbrl.NLP;
import xbrl.Utils;

public class FilenameFetch {
	public static Pattern TRPattern = Pattern.compile("(?ism)(<tr[^>]*>|</tr[^>]*>)");
	public static Pattern hrefDescriptionPattern = Pattern.compile("(?ism)(?<= href=\").{1,255}\\.html?(?=\">)");
	public static Pattern hrefExternalFilenameAndDescriptionPattern = Pattern
			.compile("(?ism)(?<= href=\").{1,255}?\\.html?(?=\">).{1,500}?(</a>)");
	// if external it ends in .htm
	public static Pattern hrefFilenamePattern = Pattern.compile("(?ism)(?<= href=\").*?(?=\">)");
	public static Pattern hrefSimpleDescriptionPattern = Pattern.compile("(?ism)(?<=<a[^>]*>).*?(?=</a[^>]*>)");
	public static Pattern hrefSimpleFilenamePattern = Pattern
			.compile("(?ism)(?<=[\r\n ].{0,9000}?href=\")([^\"]*)(?=\")");

	public static boolean hrefInRow = true;

	public static TreeMap<String, List<String>> map_external_href_filenames = new TreeMap<String, List<String>>();
	public static TreeMap<String, List<String>> map_internal_href_filenames = new TreeMap<String, List<String>>();

	public static boolean filename_okay(String hrefStr) throws IOException, SQLException {
		NLP nlp = new NLP();
		GoLaw gl = new GoLaw();
		String href_filename = null, href_description = null;
//		System.out.println("exclude me?===" + hrefStr + "|");
		List<String> list_href_description = new ArrayList<String>();
		if (hrefStr.length() > 10000 || nlp.getAllIndexEndLocations(hrefStr,
				Pattern.compile("(?i)xlink|^#|href=\"#|javascript|\\.(xsd|xml|xbrl|pre|lab|cal)")).size() > 0) {
//			System.out.println("excluded===" + hrefStr + "|");
			List<String> listDescr = nlp.getAllMatchedGroups(hrefStr, hrefSimpleDescriptionPattern);
			List<String> listFilenm = nlp.getAllMatchedGroups(hrefStr, hrefSimpleFilenamePattern);

			if (listDescr.size() == 0 && listFilenm.size() == 0)
				return false;
			else {
				href_filename = gl.stripHtml(listFilenm.get(0)).replaceAll("xxPD", "\\.").replaceAll("[\r\n]", " ")
						.replaceAll("[ ]+", " ").trim();
				href_description = gl.stripHtml(listDescr.get(0)).replaceAll("xxPD", "\\.").replaceAll("[\r\n]", " ")
						.replaceAll("[ ]+", " ").trim();
//				System.out.println("internal? descr=" + href_description);
//				System.out.println("internal? filename=" + href_filename);
				if (map_internal_href_filenames.containsKey(href_filename)) {
					list_href_description = new ArrayList<String>();
					list_href_description = map_internal_href_filenames.get(href_filename);
					if (!href_filename.equals(href_description)) {
						list_href_description.add(href_description);
					}
					map_internal_href_filenames.put(href_filename, list_href_description);
				} else {
					list_href_description = new ArrayList<String>();
					if (!href_filename.equals(href_description)) {
						list_href_description.add(href_description);
						map_internal_href_filenames.put(href_filename, list_href_description);
					}

				}

			}

			return false;
		}

		return true;
	}

	public static String[] getDescription_and_Filename_in_Adjacent_Cells(String text) {
		String filename = null, description = null;

		String[] ary = { filename, description };

		return ary;
	}

	public static List<String[]> getHref_Filename_and_Description_in_Table_Row(String text)
			throws IOException, SQLException {
		NLP nlp = new NLP();
		GoLaw gl = new GoLaw();
		List<String[]> list = new ArrayList<String[]>();
		List<String> listOfRows = nlp.getAllMatchedGroups(text, Pattern.compile("(?ism)<tr[^>]*>.*?</tr[^>]*>"));
		System.out.println(listOfRows.size());
		for (int i = 0; i < listOfRows.size(); i++) {
			if (nlp.getAllMatchedGroups(listOfRows.get(i), hrefDescriptionPattern).size() > 0) {
				System.out.println(
						"filename==" + nlp.getAllMatchedGroups(listOfRows.get(i), hrefDescriptionPattern).get(0));
				System.out.println("row==" + listOfRows.get(i));
				System.out.println("stripped row==" + gl.stripHtml(listOfRows.get(i)) + "||||");
				hrefInRow = true;
				getHref_External_Filename_and_Description(listOfRows.get(i));
				hrefInRow = false;
			}
		}

		return list;
	}

	public static List<String[]> getHrefExternal_Filenames_and_Published_Descriptions(String text)
			throws IOException, SQLException {

		// TODO:
		// when there is an href match, I need to determine if it is in a row. then if
		// there's only 1 href filename all that text belongs to it. if there are 2 then
		// we can only grab txt subsequent to each. This is distinct although maybe same
		// nlp when href is in cell 1 row 1 and display name in cell 2 row 2. in both
		// cases we want to associate all text in cell2 with the href of that instance.
		// Secondly there may be multi display names with each href (such as a form of
		// note in an indenture) so in the second instance of same href we get the new
		// display name by seeing it is in a separate row.
		GoLaw gl = new GoLaw();
		NLP nlp = new NLP();
		List<String[]> list = new ArrayList<String[]>();
//		System.out.println("text.len==" + text.length());
		List<String[]> listHref = nlp.getAllStartIdxLocsAndMatchedGroups(text,
				hrefExternalFilenameAndDescriptionPattern);
		String hrefStr = null, hrefStrStrip = null, href_filename = null, href_description = null,
				href_filename_prior = null;

//		System.out.println("listHref.size()==" + listHref.size());
		int sIdx = 0;
		String textSnip = "";
		for (int i = 0; i < listHref.size(); i++) {
			hrefStr = listHref.get(i)[1];
			hrefStrStrip = gl.stripHtml(hrefStr).replaceAll("xxPD", "\\.").replaceAll("[\r\n]+", " ").replaceAll("[ ]+",
					" ");
			sIdx = Integer.parseInt(listHref.get(i)[0]);
			// add rule that if href name is less than 4 characters after replacing
			// [\\.\\d\\(\\)]|EXHIBIT|EXH (type name) then strip all text that immediately
			// follows the type name.

			if (hrefStrStrip.split("\">").length < 2)
				continue;
			href_filename = hrefStrStrip.split("\">")[0];
			if (href_filename.equals(href_filename_prior))
				continue;

			href_description = hrefStrStrip.split("\">")[1];

			if (href_description.replaceAll("(?ism)exhibits?|exh|[\\d\\.\\(\\)#]+|OTHER|OTH", "").length() < 5) {

//				System.out.println("getting description on this line===" + href_description);
				textSnip = text.substring(Math.max(sIdx - 5000, 0), Math.min(sIdx + 5000, text.length()));
				if (textSnip.contains("discountedgar") || href_filename.contains("discountedgar") || href_description.contains("discountedgar"))
					continue;

				if (nlp.getAllMatchedGroups(textSnip,
						Pattern.compile("(?ism)(?<=" + href_filename + ".{1,25}"
								+ href_description.replaceAll("[^a-zA-Z\\d]", "") + ").{20,500}(\r\n|$|</(div|tr|p)>)"))
						.size() > 0) {
					href_description = nlp.getAllMatchedGroups(textSnip,
							Pattern.compile("(?ism)(?<=" + href_filename + ".{1,25}"
									+ href_description.replaceAll("[^a-zA-Z\\d]", "")
									+ ").{20,500}(\r\n|$|</(div|tr|p)>)"))
							.get(0);
					href_description = gl.stripHtml(href_description).replaceAll("xxPD", "\\.")
							.replaceAll("[\r\n]+", " ").replaceAll("[ ]+", " ");
//					System.out.println("2. getting description on this line===" + href_description);
				}
			}

			if (href_description.replaceAll("[^A-Za-z]+", "")
					.replaceAll("(?ism)exhibits?|exh" + "|[\\d\\.\\(\\)#]+" + "|OTHER|OTH", "").length() < 5)
				continue;

//			System.out.println("#" + i + " final=" + href_filename + " disaplay=" + href_description);
			String[] ary_filename_displayName = { href_filename, href_description };
			list.add(ary_filename_displayName);
			href_filename_prior = href_filename;
			href_filename = "";
			href_description = "";

		}

		return list;
	}

	public static String getTableRowOrCellContents(String text, String type, int idx) {

		// get the last instance of <tr or <td prior to idx
		String textFirstHalf = text.substring(0, idx + 1);

//		System.out.println("type====" + type);
		textFirstHalf = textFirstHalf.substring(textFirstHalf.lastIndexOf("<" + type));
//		System.out.println("textFirstHalf==" + textFirstHalf + "\r\n\r\n");
		// get the first instance of </tr or </td of remaining txt
		String textSecondHalf = text.substring(idx);
//		System.out.println("textSecondHalf===" + textSecondHalf);
		textSecondHalf = textSecondHalf.substring(idx, textSecondHalf.indexOf("</" + type));

		text = textFirstHalf + textSecondHalf;
//		System.out.println("cell/row text==" + text);

		return text;
	}

	public static List<String[]> getHref_External_Filename_and_Description(String text)
			throws IOException, SQLException {

		GoLaw gl = new GoLaw();
		NLP nlp = new NLP();
		List<String[]> list = new ArrayList<String[]>();
		List<String> list_href_description = new ArrayList<String>();
		List<String> listHref = nlp.getAllMatchedGroups(text, hrefDescriptionPattern);
		String hrefStr = null, hrefStrStrip = null, href_filename = null, href_description = null;

//		System.out.println("text.len==" + text.length()
//		+" text=="+text
//		);
//		System.out.println("listHref.size()==" + listHref.size());
		for (int i = 0; i < listHref.size(); i++) {

			hrefStr = listHref.get(i);
			System.out.println("....hrefStr=" + hrefStr);
			hrefStrStrip = gl.stripHtml(text).replaceAll("xxPD", "\\.").replaceAll("[\r\n]+", " ").replaceAll("[ ]+",
					" ");
			System.out.println("hrefStrStrip=" + hrefStrStrip);
			if (!filename_okay(hrefStr)) {
				System.out.println("1. continue....");
				continue;
			}

			if (hrefStrStrip.length() < 7 && !hrefInRow) {
				System.out.println("continue, too small");
				continue;
			}

			if (hrefInRow) {
				href_filename = gl.stripHtml(nlp.getAllMatchedGroups(text, hrefFilenamePattern).get(0)
						.replaceAll("xxPD", "\\.").replaceAll("[\r\n]+", " ").replaceAll("[ ]+", " ").trim());
				href_description = gl.stripHtml(text).replaceAll("xxPD", "\\.").replaceAll("[\r\n]+", " ")
						.replaceAll("[ ]+", " ").trim();
			}
			System.out.println("hrefInRow=" + hrefInRow + " href_filename=" + href_filename);
			if (hrefInRow && !filename_okay(hrefStr)) {
				System.out.println("C. hrefInRow=" + hrefInRow + " href_filename==" + href_filename);
//				System.out.println("internal hyperlink");
				continue;
			}

			if (hrefStrStrip.length() < 7 && hrefInRow) {
				System.out.println("description in row=" + href_description);
				System.out.println("filename in row=" + href_filename);
				if (map_external_href_filenames.containsKey(href_filename)) {
					list_href_description = new ArrayList<String>();
					list_href_description = map_external_href_filenames.get(href_filename);
					if (!href_filename.equals(href_description)) {
						list_href_description.add(href_description);
					}
					map_external_href_filenames.put(href_filename, list_href_description);
				} else {

					list_href_description = new ArrayList<String>();
					if (!href_filename.equals(href_description)) {
						list_href_description.add(href_description);
						map_external_href_filenames.put(href_filename, list_href_description);
					}

				}

			} else {
				// if not row then .....
				System.out.println("1.hrefStr==" + hrefStr);
				List<String> listDescr = nlp.getAllMatchedGroups(hrefStr, hrefSimpleDescriptionPattern);
				List<String> listFilenm = nlp.getAllMatchedGroups(hrefStr, hrefSimpleFilenamePattern);

				if (listDescr.size() == 0 && listFilenm.size() == 0)
					continue;
				href_description = gl.stripHtml(listDescr.get(0)).replaceAll("xxPD", "\\.").replaceAll("[\r\n]+", " ")
						.replaceAll("[ ]+", " ");
				href_filename = gl.stripHtml(listFilenm.get(0)).replaceAll("xxPD", "\\.").replaceAll("[\r\n]+", " ")
						.replaceAll("[ ]+", " ");
//				System.out.println("D. href_filename=" + href_filename);
//				System.out.println("D. href_description=" + href_description);
				if (!filename_okay(hrefStr))
					continue;
				if (map_external_href_filenames.containsKey(href_filename)) {
					list_href_description = new ArrayList<String>();
					list_href_description = map_external_href_filenames.get(href_filename);
					if (!href_filename.equals(href_description)) {
						list_href_description.add(href_description);
					}
					map_external_href_filenames.put(href_filename, list_href_description);
				} else {
					list_href_description = new ArrayList<String>();
					if (!href_filename.equals(href_description)) {
						list_href_description.add(href_description);
//						System.out.println("5.put=" + href_filename);
						map_external_href_filenames.put(href_filename, list_href_description);
					}

				}
				System.out.println("      Description=" + hrefStrStrip);

			}
		}
		return list;

	}

	public static void loadIntoMysqlHref(String filename, String table) throws FileNotFoundException, SQLException {

//		sbHref.append(acc + "||" + cik + "||"
//				+ listFileName_Displayed_Description.get(i)[0] + "||" + listFileName_Displayed_Description.get(i)[1]
//				+ "\r\n");  -- once new data is generated I will have the fields above and will need to fix metadata_href table. but for now the table is these fields : sbHref.append(fileDate + "||" + acc + "||" + cik + "||" + formType + "||"
//+ listFileName_Displayed_Description.get(i)[0] + "||" + listFileName_Displayed_Description.get(i)[1]
//need to make metadata_href table partitioned on fdate and I need to create primary key by combining accno of link and filename link. put this in href txt files I save.

		File file = new File(filename);
		long size = file.length();
		// System.out.println("loadFileIntoMysql fileSize:: " + size);
		String query = "SET GLOBAL local_infile =1;"

				+ "\r\n" + "LOAD DATA INFILE '" + filename.replaceAll("\\\\", "//") + "'IGNORE INTO TABLE " + table
				+ " FIELDS TERMINATED BY '|' "
//				+ "lines terminated by " + "'\\r\\n'" 
				+ ";";
//		 System.out.println("mysql query::" + query);
//		try {
		MysqlConnUtils.executeQuery(query);
//		} catch (SQLException e) {
//			e.printStackTrace(System.out);
//		}
	}

	public static void insertHrefFilesIntoMySQL(String folder, int comitCount, int yr, int qtr)
			throws IOException, SQLException {

		NLP nlp = new NLP();
		String text = null, acc, href_description, formType = "", cik = "", acc_that_had_href, table = "metadata_href",
				filedate = "", href_filename = "";

		StringBuilder sb = new StringBuilder();
		folder = folder + "/" + yr + "/QTR" + qtr + "/";
		File files = new File(folder);
		File[] listOfFiles = files.listFiles();

		String href_csv_folder = GetContracts.href_csv + "/" + yr + "/QTR" + qtr + "/";
		System.out.println("href_csv_folder===" + href_csv_folder);
		String edgar_link = null, line;
		Utils.createFoldersIfReqd(href_csv_folder);

		File file = new File(href_csv_folder + "/tmp.txt");
		PrintWriter pw = new PrintWriter(file);
		int cnt = 0, qtrCnt = 0;
		int i = 0;
		for (; i < listOfFiles.length; i++) {
//			System.out.println("filename...=" + listOfFiles[i].getAbsolutePath());
			cnt++;
			text = Utils.readTextFromFile(listOfFiles[i].getAbsolutePath());
			String[] lines = text.split("\r\n");
			for (int n = 0; n < lines.length; n++) {
				qtrCnt++;
				// filedate,acc,acc_that_had_href,filename,cik,edgarLink,description
				line = lines[n];
				String[] fields = line.split("\\|\\|");
				filedate = fields[0];
				acc_that_had_href = fields[1];
				cik = fields[2];
				formType = fields[3];
				acc = acc_that_had_href;
//				System.out.println("line++=" + line);
				edgar_link = fields[4];
				href_description = fields[5];
				href_filename = edgar_link;

				if (!edgar_link.contains("http")) {
					edgar_link = "https://www.sec.gov/Archives/edgar/data/" + cik + "/" + acc.replaceAll("-", "") + "/"
							+ edgar_link;
				}

				if (!edgar_link.contains("/"))
					continue;

				else if (nlp.getAllMatchedGroups(edgar_link, Pattern.compile("[\\d]{18}")).size() > 0
						&& edgar_link.contains("http")) {

//					System.out.println("acc that had href=="+acc_that_had_href+"\r\nedgar_link="+edgar_link);
					acc = edgar_link.substring(0, edgar_link.lastIndexOf("/"));
					acc = acc.substring(acc.length() - 18, acc.length());
					acc = acc.substring(0, 10) + "-" + acc.substring(10, 12) + "-" + acc.substring(12, 18);
//					System.out.println("acc=" + acc);
					href_filename = edgar_link.substring(edgar_link.lastIndexOf("/") + 1);
				}
//				System.out.println("acc_for_link=" + acc_for_link);
//				System.out.println("edgar link==" + edgar_link);
//				System.out.println("n=="+n+" "+filedate + "|" + acc + "|" + acc_that_had_href + "|" + cik + "|" + formType
//						+ "|" + href_filename + "|" + edgar_link + "|" + href_description);

				sb.append(filedate + "|" + acc + "|" + acc_that_had_href + "|" + cik + "|" + formType + "|"
						+ href_filename + "|" + edgar_link + "|" + href_description + "\r\n");
			}

			if (cnt > comitCount || i + 1 == listOfFiles.length) {
				file = new File(href_csv_folder + yr + "QTR" + qtr + "_" + i + ".csv");
				pw = new PrintWriter(file);
				pw.append(sb.toString());
				pw.close();
				loadIntoMysqlHref(file.getAbsolutePath(), table);
				cnt = 0;
				sb = new StringBuilder();
				System.out.println("yr=" + yr + " qtr=" + qtr + " herf count inserted into MySQL=" + qtrCnt);
			}
		}

		if (sb.length() > 0) {

			file = new File(href_csv_folder + yr + "QTR" + qtr + "_" + i + ".csv");
			pw = new PrintWriter(file);
			pw.append(sb.toString());
			pw.close();
			loadIntoMysqlHref(file.getAbsolutePath(), table);
			cnt = 0;
			sb = new StringBuilder();
			System.out.println("yr=" + yr + " qtr=" + qtr + " herf count inserted into MySQL=" + qtrCnt);
		}

		MysqlConnUtils.executeQuery("delete from metadata_href where \r\n" + "\r\n"
				+ "trim(acc) not rlike '[0-9]{10}-[0-9]{2}-[0-9]{6}'\r\n" + "or\r\n" + "\r\n"
				+ "trim( acc_that_had_href) not rlike '[0-9]{10}-[0-9]{2}-[0-9]{6}'\r\n" + "\r\n" + "or\r\n" + "\r\n"
				+ "trim( edgar_link) not rlike 'http'\r\n" + "\r\n" + "or\r\n" + "\r\n" + "length(trim(filename))<1\r\n"
				+ "\r\n" + "or\r\n" + "\r\n" + "length(trim( href_description))<1\r\n" + "\r\n"
				+ "or href_description is null ;");
	}

	public static void main(String[] args) throws IOException, SolrServerException, SQLException {
		NLP nlp = new NLP();

		String text = Utils.readTextFromFile("c:\\temp\\t2.txt");
		hrefInRow = false;
		getHrefExternal_Filenames_and_Published_Descriptions(text);// USE THIS ONE
		// if href is in cell and description is 31.1 or EXH then jump to next cell to
		// grab text. OR grab all text in that row that follows.
		int idx = nlp.getAllIndexEndLocations(text, Pattern.compile(" href")).get(0);
		System.out.println("..idx==" + idx);
//		getTableRowOrCellContents(text, "tr", idx);

		int sY = 2022, eY = 2022, sQ = 1, eQ = 1;
		for (; sY <= eY; sY++) {
			for (; sQ <= eQ; sQ++) {
				GetContracts.importMetaDataIntoMySQL(GetContracts.metadataFolder, sY, eY, sQ, eQ, 1000);
//				insertHrefFilesIntoMySQL(GetContracts.href_data, 5000, sY, sQ);
			}
			sQ = 1;
		}
		/*
		 * 1. check each table and see if any href in row. if so apply ruleset to get
		 * description and filename. 2. check all text and apply normal rule. Same
		 * filename will be found as in 1, but description will be short length and in
		 * that case take 1 else keep what is found w/ 2. make a map of filenames as key
		 * and val is descriptions
		 */
		// fetch href
//		List<String[]> listDescrInTable = getHref_Filename_and_Description_in_Table_Row(text);
//		System.out.println("listDescrInTable.size=="+listDescrInTable.size());
//		NLP.printListOfStringArray("listDescrInTable==", listDescrInTable);

		// TODO: Use getHrefExternal_Filenames_and_Published_Descriptions for external
		// href links w/ descriptions. Simple href parsing. More complex href parsing is
		// where href filename is in separate cell in row
		// and description to the right OR there isn't a table but a tab. try redoing
		// completely and focus on finding filename using href=\".*?\\.htm which is
		// already embedded in patterns. Then once that is found take the txt that
		// follows as the descr

//		getHref_External_Filename_and_Description(text);
//		NLP.printMapStrListOfString("map_external_href_filenames", map_external_href_filenames);
		NLP.printMapStrListOfString("map_internal_href_filenames", map_internal_href_filenames);

		// existing works for this --
		// https://www.sec.gov/Archives/edgar/data/3499/0000003499-21-000020-index.htm
		// but not for this
		// https://www.sec.gov/Archives/edgar/data/1022652/000102265221000068/0001022652-21-000068.txt
		// see temp folder java code file 'fetch href.txt' which will work for above.
		// reconcile. I believe it works b/c descr is w/n <a>...</a> and not in row??

//		System.out.println(nlp.getAllMatchedGroups(text, hrefSimpleFilenamePattern).get(0));
//		System.out.println(nlp.getAllMatchedGroups(text, hrefSimpleDescriptionPattern).get(0));

		// TODO: Need to record filesize AFTER it is stripped!!! else I'm not filtering
		// out f/p due to large html nit zero text! NOTE - exhibit names in hyper links
		// seems to be incredibly misleading! See:
		// https://www.sec.gov/Archives/edgar/data/1552947/000158064221003811/apex485b.htm

		// TODO: Ck where opening para occurs in json for forms where it routinely
		// happens in the middle and then do oP search and then cut <doc> prior to hits

		// TODO: Another kTyp=ESPP

		// TODO: if internal cross ref rlike 'agreement|.....' then take from start of
		// 2nd instance of internal x-ref hyperlink to next hyperlink and last till the
		// end.

		// TODO: Use internal hyperlinks as primary hdgs! These are typically the TOC of
		// agts. But they will conflict with agt hyperlinks - how to go end of agt w/o
		// tripping up at internal hdg? Note map is putting alph but i want to keep
		// natural order. add idx as preface in list to reorder later!

	}
}
