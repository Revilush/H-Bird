package search.resultComponents;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * Holds HL fields/snippets for one Document only.
 * @author admin
 *
 */
public class DocHighlightingResult {

	private RawDocument document;
	
	private String docId;
	
	private Map<String, List<HighlightSnippet>> hlSnippets = new LinkedHashMap<String, List<HighlightSnippet>>();
	
	/**
	 * 
	 * @param hlResultsFor1Doc
	 */
	@SuppressWarnings("unchecked")
	public DocHighlightingResult(String docId, Map<String, Object> hlFieldsSnippets) {
		this.docId = docId;
		for (String hlField : hlFieldsSnippets.keySet()) {
			List<HighlightSnippet> hlSnippetsList = new ArrayList<HighlightSnippet>();
			List<String> snippetsStr = (List<String>) hlFieldsSnippets.get(hlField);
			for (String ss: snippetsStr)
				if (StringUtils.isNotBlank(ss))
					hlSnippetsList.add(new HighlightSnippet(ss));
			hlSnippets.put(hlField, hlSnippetsList);
		}
	}

	
	public String getDocId() {
		return docId;
	}
	public void setDocId(String docId) {
		this.docId = docId;
	}

	public Map<String, List<HighlightSnippet>> getHlSnippets() {
		return hlSnippets;
	}
	public void setHlSnippets(Map<String, List<HighlightSnippet>> hlSnippets) {
		this.hlSnippets = hlSnippets;
	}

	public RawDocument getDocument() {
		return document;
	}
	public void setDocument(RawDocument document) {
		this.document = document;
	}

	
}
