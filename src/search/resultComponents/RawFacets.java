package search.resultComponents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class RawFacets {

	@JsonProperty("facet_queries")
	private Map<String, String> facetQueries;
	
	@JsonProperty("facet_fields")
	private Map<String, List<Object>> facetFieldsInternal;

	private Map<String, Map<String, Integer>> facetFields = new HashMap<String, Map<String, Integer>>();

	@JsonProperty("facet_dates")
	private Map<String, String> facetDates;
	
	@JsonProperty("facet_ranges")
	private Map<String, String> facetRanges;
	
	@JsonProperty("facet_intervals")
	private Map<String, String> facetIntervals;
	
	@JsonProperty("facet_heatmaps")
	private Map<String, String> facetHeatmaps;

	
	public Map<String, Map<String, Integer>> getFacetFields() {
		return facetFields;
	}
	public void setFacetFieldsInternal(Map<String, List<Object>> facetFieldsInternal) {
		this.facetFieldsInternal = facetFieldsInternal;
		if (null == facetFieldsInternal || facetFieldsInternal.size() == 0)
			return;
		List<Object> facets;
		String term;
		int termFreq;
		for (String key : facetFieldsInternal.keySet()) {
			//"Field-Name": ["Acts  of Holders", 1, "Company  as Agent", .......]
			facets = facetFieldsInternal.get(key);
			if (null == facets || facets.size() == 0)
				continue;
			Map<String, Integer> value = new TreeMap<String, Integer>();
			for (int i=0; i < facets.size(); i+=2) {
				term = (String) facets.get(i);
				termFreq = (int) facets.get(i+1);
				if (termFreq > 0)
					value.put(term, termFreq);
			}
			if (value.size() > 0)
				facetFields.put(key, value);
		}
	}

}
