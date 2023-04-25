package algo_testers.search_dependencies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import algo_testers.dependencies.SynonymFilter;

@SuppressWarnings("unchecked")
public class SynonymReader {
	protected static Log log = LogFactory.getLog(SynonymReader.class);
	
	private static List<String> uniqueSynonymWords;
	
	private static List<List<String>> synonymsRaw;
	
	//private static List<String[]> synonymsArray4Javascript = null;
	//private static List<String[]> synonymsArray4Java = null;
	
	private static List<List<String>> synonyms4Javascript = null;
	private static List<List<String>> synonyms4Java = null;
	
	private static List<List<String>> synonyms4JavascriptSorted = null;
	private static List<List<String>> synonyms4JavaSorted = null;
	
	static {
		String json = null;
		try {
			json = FileSystemUtils.ReadTextFromResource(RefreshSynonyms.SynResourceFilename);
			Map<String, Object> synsMap = JSonUtils.json2Map(json);
			synonymsRaw = (List<List<String>>)synsMap.get(RefreshSynonyms.SynTypeKey_Raw);			//JSonUtils.json2Object(json, List.class);
			
			// gather all individual words/phrases of all syns
			Set<String> synSet = new HashSet<>();
			for (List<String> syns : synonymsRaw) {
				for (String s : syns) {
					synSet.add(s);
				}
			}
			uniqueSynonymWords = new ArrayList<>(synSet);
			uniqueSynonymWords.sort(Comparator.comparing(String::length).reversed());		// sort by length - DESC
			
			List<List<String>> synonymsRegex = (List<List<String>>)synsMap.get(RefreshSynonyms.SynTypeKey_Regex);
			if (null != synonymsRegex  &&  synonymsRegex.size() > 0) {
				// syns 4 javascript
				synonyms4Javascript = SynonymFilter.cleanSynonymsList4Javascript(synonymsRegex, false, new String[]{""});
				synonyms4JavascriptSorted = SynonymFilter.cleanSynonymsList4Javascript(synonymsRegex, true, new String[]{""});
				/*
				synonymsArray4Javascript = new ArrayList<>();
				for (List<String> syns : synonymsList4Javascript) {
					synonymsArray4Javascript.add(syns.toArray(new String[syns.size()]));
				}
				*/
				
				// syns 4 java
				synonyms4Java = SynonymFilter.cleanSynonymsList4Java(synonymsRegex, false, new String[]{""});
				synonyms4JavaSorted = SynonymFilter.cleanSynonymsList4Java(synonymsRegex, true, new String[]{""});
				/*
				synonymsArray4Java = new ArrayList<>();
				for (List<String> syns : synonymsList4Java) {
					synonymsArray4Java.add(syns.toArray(new String[syns.size()]));
				}
				*/
			}
		} catch (IOException e) {
			System.err.println("Error while reading synonyms from json: " + e.getMessage());
			log.error("Error while reading synonyms from json: ", e);
		}
	}

	public static List<List<String>> getSynonymsRaw() {
		try {
			return (List<List<String>>) JSonUtils.json2Object(JSonUtils.object2JsonString(synonymsRaw));
		} catch (IOException e) {
			log.warn("", e);
		}
		return null;
	}

//	public static List<String[]> getSynonymsArray4Javascript() {
//		return synonymsArray4Javascript;
//	}
//
//	public static List<String[]> getSynonymsArray4Java() {
//		return synonymsArray4Java;
//	}

	public static List<List<String>> getSynonyms4Javascript() {
		return synonyms4Javascript;
	}

	public static List<List<String>> getSynonyms4Java() {
		return synonyms4Java;
	}

	public static List<List<String>> getSynonyms4JavascriptSorted() {
		return synonyms4JavascriptSorted;
	}

	public static List<List<String>> getSynonyms4JavaSorted() {
		return synonyms4JavaSorted;
	}

	public static List<String> getUniqueSynonymWords() {
		return uniqueSynonymWords;
	}
	
	
	
	public static void main(String[] arg) throws JsonProcessingException {
		System.out.println(JSonUtils.object2JsonString(SynonymReader.getSynonymsRaw()));
	}
}
