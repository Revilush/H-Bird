package algo_testers.search_dependencies;

import java.util.ArrayList;
import java.util.List;

import algo_testers.search_dependencies.AppConfig.SearchAppConfig;


public class SynonymController {

	/*
	@RequestMapping(value = "/applySynonymTest.*", method = RequestMethod.GET)
	public String applySynonymTest(HttpServletRequest request, HttpServletResponse response, Model model) throws SolrServerException, IOException {
		return "test/applySynonymTest";
	}
	*/
	
	
	
	public static List<List<String>> getSynonyms(SearchAppConfig seahcAppConfig, boolean format4Javascript) {
		List<List<String>> validSyns = null;
		try {
			if (seahcAppConfig.isApplySynonyms()) {
				validSyns = new ArrayList<>();
				//List<String[]> synsInDb = Synonyms.getListOfSynonyms();
				// remove empty/Filler etc synonyms
				if (format4Javascript) {
					validSyns = SynonymReader.getSynonyms4JavascriptSorted();		// SynonymFilter.cleanSynonymsArray4Javascript(synsInDb,  "","Filler");
				} else {
					validSyns = SynonymReader.getSynonyms4JavaSorted();				// SynonymFilter.cleanSynonymsArray4Java(synsInDb,  "","Filler");
				}
			}
		} catch(Exception e) {
			
		}
		return validSyns;
	}
	
	
	
}
