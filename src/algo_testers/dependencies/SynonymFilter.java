package algo_testers.dependencies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import charting.JSonUtils;


public class SynonymFilter {

	public static final String remove4Javascript = "\\(\\?[ism]+\\)";
	
	
	public static List<List<String>> cleanSynonymsList4Javascript(List<List<String>> synonymsList, String... ignoreWords) throws IOException {
		return cleanSynonymsList4Javascript(synonymsList, false, ignoreWords);
	}
	/**
	 * Removes (?ism) flags, including brackets, from the patterns as they are not recognized in JS.
	 * @param synonymsList
	 * @return
	 * @throws IOException 
	 */
	@SuppressWarnings({ "unchecked"})
	public static List<List<String>> cleanSynonymsList4Javascript(List<List<String>> synonymsList, boolean sortEachSynListDesc, String... ignoreWords) throws IOException {
		String synStr = JSonUtils.object2JsonString(synonymsList);
		synStr = synStr.replaceAll(remove4Javascript, "");
		List<List<String>> list = (List<List<String>>) JSonUtils.json2Object(synStr);
		
		List<List<String>> respSynsList = cleanSynonymsList4Java(list, sortEachSynListDesc, ignoreWords);
		return respSynsList;
	}
	
	
	/**
	 * Removes (?ism) flags from the patterns, as they are not recognized in JS.
	 * @param synonymsList
	 * @return
	 * @throws IOException 
	 */
	@SuppressWarnings({ "unchecked" })
	public static List<String[]> cleanSynonymsArray4Javascript(List<String[]> synonymsList, String... ignoreWords) throws IOException {
		String synStr = JSonUtils.object2JsonString(synonymsList);
		synStr = synStr.replaceAll(remove4Javascript, "");
		List<List<String>> list = (List<List<String>>) JSonUtils.json2Object(synStr);
		
		List<List<String>> cleanedSynsList = cleanSynonymsList4Java(list, ignoreWords);
		List<String[]> respSynsList = new ArrayList<>();
		for (List<String> syns : cleanedSynsList) {
			respSynsList.add(syns.toArray(new String[0]));
		}
		return respSynsList;
	}
	
	
	public static List<List<String>> cleanSynonymsList4Java(List<List<String>> synonymsList, String... ignoreWords) {
		return cleanSynonymsList4Java(synonymsList, false, ignoreWords);
	}
	/**
	 * just cleans-up the synonyms list, ie removes empty syns  and the ignore words provided etc
	 * @param synonymsList
	 * @param ignoreWords
	 * @return
	 * @throws IOException
	 */
	public static List<List<String>> cleanSynonymsList4Java(List<List<String>> synonymsList, boolean sortEachSynListDesc, String... ignoreWords) {
		List<List<String>> respSynsList = new ArrayList<>();
		for (List<String> synsList : synonymsList) {
			if (null == synsList  ||  synsList.size() == 0)
				continue;
			List<String> syns = new ArrayList<>();
			for (String syn : synsList) {
				if (StringUtils.isBlank(syn)  ||  ArrayUtils.contains(ignoreWords, syn))
					continue;
				syns.add(syn);
			}
			if (syns.size() > 0) {
				if (sortEachSynListDesc) {
					// sort the list by word's length - DESC: bigger words first
					syns.sort(Comparator.comparing(String::length).reversed());
				}
				respSynsList.add(syns);
			}
		}
		return respSynsList;
	}

	public static List<String[]> cleanSynonymsArray4Java(List<String[]> synonymsList, String... ignoreWords) {
		return cleanSynonymsArray4Java(synonymsList, false, ignoreWords);
	}
	/**
	 * just cleans-up the synonyms list, ie removes empty syns  and the ignore words provided etc
	 * @param synonymsList
	 * @param ignoreWords
	 * @return
	 * @throws IOException
	 */
	public static List<String[]> cleanSynonymsArray4Java(List<String[]> synonymsList, boolean sortEachSynArrayDesc, String... ignoreWords) {
		List<String[]> respSynsList = new ArrayList<>();
		for (String[] synsList : synonymsList) {
			if (null == synsList  ||  synsList.length == 0)
				continue;
			List<String> syns = new ArrayList<>();
			for (String syn : synsList) {
				if (StringUtils.isBlank(syn)  ||  ArrayUtils.contains(ignoreWords, syn))
					continue;
				syns.add(syn);
			}
			if (syns.size() > 0) {
				String[] array = syns.toArray(new String[0]);
				if (sortEachSynArrayDesc) {
					// sort the array by word's length - DESC: bigger words first
					Arrays.sort(array, Comparator.comparing(String::length).reversed());
				}
				respSynsList.add(array);
			}
		}
		return respSynsList;
	}


	
	
	
}
