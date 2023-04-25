package search.resultComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class SearchResult {

	public String queryStr;
	
	public String finalSearchUrl;
	
	@JsonProperty("responseHeader")
	public RawResponseHeader responseHeader;
	
	@JsonProperty("response")
	public RawResponse response;
	
	@JsonProperty("facet_counts")
	public RawFacets facets;
	
	public List<DocHighlightingResult> highlighting = new ArrayList<DocHighlightingResult>();
	
	
	public void discardDocs(DiscardDocsCriteria criteria) {
		if (null == response)
			return;
		response.discardDocs(criteria);
	}

	public List<RawDocument> getActiveDocs() {
		if (null == response)
			return null;
		return response.docs;
	}
	
	public void mapDocs2Highlighting() {
		List<RawDocument> docs = getActiveDocs();
		if (null == docs)
			return;
		for (RawDocument rd : docs) {
			for (DocHighlightingResult hlResult : highlighting) {
				if (! hlResult.getDocId().equals(rd.id))
					continue;
				hlResult.setDocument(rd);
				rd.highlightingSnippets = hlResult;
				break;
			}
		}
	}
	
	public List<DocHighlightingResult> getActiveHighlightingResults() {
		List<DocHighlightingResult> activeHLs = new ArrayList<DocHighlightingResult>();
		List<RawDocument> docs = getActiveDocs();
		if (null == docs)
			return activeHLs;
		int hlIdx = 0;
		DocHighlightingResult hlResult;
		for (RawDocument rd : docs) {
			for (int i = hlIdx; i < highlighting.size(); i++) {
				hlResult = highlighting.get(i);
				if (! hlResult.getDocId().equals(rd.id))
					continue;
				activeHLs.add(hlResult);
				hlIdx = i+1;
				break;
			}
		}
		return activeHLs;
	}
	
	
	
	
	
	@SuppressWarnings("unchecked")
	@JsonProperty("highlighting")
	public void setHighlightingResults(Map<String, Object> hlResults) {
		if (null == hlResults  ||  hlResults.size() == 0)
			return;
		String docId;
		Map<String, Object> hlResultsFor1Doc;
		for (String key : hlResults.keySet()) {
			docId = key;
			hlResultsFor1Doc = (Map<String, Object>)hlResults.get(key);
			highlighting.add(new DocHighlightingResult(docId, hlResultsFor1Doc));
		}
	}
	
	
	
	
	
	public static class DiscardDocsCriteria {
		
		public Float docScoreDeltaWithinPercent = null;
		
		
	}
	
	
	
}
