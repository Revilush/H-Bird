package algo_testers.search_dependencies;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;


public class RefreshSynonyms {
	
	public static final String SynResourceFilename = "syns_DO_NOT_CHANGE.json";
	public static final String SynTypeKey_Raw = "raw";
	public static final String SynTypeKey_Regex = "regex";
	

	private static void refreshSynonyms() throws IOException {
		File synF = new File("synonyms.txt");
		if (!synF.exists()) {
			System.out.println("synonyms.txt file was not found!");
			return;
		}
		//System.out.println(synF.getAbsolutePath());
		String synText = FileSystemUtils.readTextFromFile(synF);
		// remove single line and block comments
		synText = synText.replaceAll("(?ism)(?=[\r\n\\s\\h]*)(\\[.+?\\])", "");		// block comments:  [ .... ]
		synText = synText.replaceAll("(?ism)(?=[\r\n\\s\\h]*)(/+.+?[\r\n]+)", "");	// single lines:  //.......
		
		String[] synLines = synText.split("[\r\n]+");
		List<List<String>> synonymsList = new ArrayList<>();
		List<String> synL;
		for (String line : synLines) {
			synL = parseSynonymLine(line);
			if (null != synL  &&  synL.size() > 0)
				synonymsList.add(synL);
		}
		System.out.println("synonyms lines found: " + synonymsList.size());

		// keep the 'raw' syns
		Map<String, Object> finalSynsList = new LinkedHashMap<>();
		finalSynsList.put(SynTypeKey_Raw, JSonUtils.json2Object(JSonUtils.object2JsonString(synonymsList)));
		
		// converts syns to regex
		for (List<String> synList : synonymsList) {
			for (int i=0; i < synList.size(); i++) {
				synList.set(i, convertToSynRegex(synList.get(i)));
			}
		}
		finalSynsList.put(SynTypeKey_Regex, synonymsList);
				
		// write the syns list to a json file
		File folder = new File(".", "src/main/resources");
		JSonUtils.prettyPrintInto(finalSynsList, new File(folder, SynResourceFilename));
//		String json = JSonUtils.object2JsonString(finalSynsList);
//		FileSystemUtils.writeTextToFile(new File(folder, SynResourceFilename), json);
	}
	
	private static List<String> parseSynonymLine(String line) {
		//System.out.println(line);			//trustee, "indenture trustee", .......
		if (line.trim().startsWith("//"))			// comment
			return null;
		char[] chars = line.toCharArray();
		char c;
		List<String> syns = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		boolean inDQ = false;
		for (int i=0; i < chars.length; i++) {
			c = chars[i];
			if (c == '"') {
				inDQ = !inDQ;		// if its the 1st " then its the start (in double quote), for the 2nd one its the end.
				// pick the word/phrase read so far
				addWord2List(sb, syns);
			} else if (c == ','  &&  !inDQ) {
				// pick the word
				addWord2List(sb, syns);
			} else
				sb.append(c);
		}
		addWord2List(sb, syns);
		
		//System.out.println(syns);
		return syns;
	}
	
	private static void addWord2List(StringBuilder sb, List<String> syns) {
		String word = sb.toString();
		if (StringUtils.isNotBlank(word)) {
			syns.add(word);
			//syns.add(convertToSynRegex(word));
			/*
			String c1 = word.substring(0, 1);
			word = word.substring(1);
			syns.add("\\b(["+c1.toLowerCase()+c1.toUpperCase()+"]" + word + ")\\b");
			*/
		}
		sb.delete(0, sb.length());
	}
	
	/**
	 * Converts a word or each word of phrase into a regex to satisfy following conditions:
	 * 	- 1st letter of each word (or words in phrase) should match to lower/upper case provided rest of word is same.
	 * 	- each word or phrase must match to word boundaries (no part of word - ie 'will' should not match 'willing').
	 * @param synonym	synonym word or phrase
	 * @return
	 */
	public static String convertToSynRegex(String synonym) {
		String[] words = synonym.split(" ");
		String c1;
		for (int i=0; i < words.length; i++) {
			c1 = words[i].substring(0, 1);
			words[i] = "["+c1.toLowerCase()+c1.toUpperCase()+"]"+ words[i].substring(1);
		}
		return ("\\b("+ StringUtils.join(words, " ") + ")\\b");
	}
	
	@SuppressWarnings("unused")
	private static void readSyns() throws IOException {
//		String json = FileSystemUtils.ReadTextFromResource(SynResourceFilename);
//		System.out.println(json);
		System.out.println(JSonUtils.object2JsonString(SynonymReader.getSynonyms4Java()));
	}
	
	/*
	 * public static void main(String[] arg) throws SolrServerException, IOException
	 * { System.out.println("Refreshing synonyms into code"); //
	 * SystemUtils.setCatalinaPathIfNotSet(); refreshSynonyms();
	 * 
	 * //readSyns(); }
	 */
}
