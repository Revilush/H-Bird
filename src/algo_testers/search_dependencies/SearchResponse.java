package algo_testers.search_dependencies;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchResponse {

	@JsonProperty("clauseResults")
	private List<SearchResultsGroupSummary> clauseResults;
	
	@JsonProperty("queryDetails")
	private UserQueryDetails queryDetails;
	
	@JsonProperty("isMetaOnly")
	private Boolean isMetaOnly = null;
	
	@JsonProperty("isOnlyExhSearch")
	private Boolean is_only_exh_search = null;
	
	@JsonProperty("legalTerm")
	private String legalTerm = null;
	
	@JsonProperty("legalTermGuidance")
	private String legalTermGuidance = null;
	
	@JsonProperty("meta")
	private DocsMetaData metaData;
	
	public SearchResponse() {}
	public SearchResponse(UserQueryDetails queryDetails, List<SearchResultsGroupSummary> clauseResults, String legalTerm, String legalTermGuidance) {
		this.queryDetails = queryDetails;
		this.clauseResults = clauseResults;
		this.legalTerm = legalTerm;
		this.legalTermGuidance = legalTermGuidance;
		
	}
	
	// ----------------
	public List<SearchResultsGroupSummary> getClauseResults() {
		return clauseResults;
	}

	public void setClauseResults(List<SearchResultsGroupSummary> clauseResults) {
		this.clauseResults = clauseResults;
	}

	public UserQueryDetails getQueryDetails() {
		return queryDetails;
	}

	public void setQueryDetails(UserQueryDetails queryDetails) {
		this.queryDetails = queryDetails;
	}
	
	public Boolean getIsMetaOnly() {
		return isMetaOnly;
	}
	public void setIsMetaOnly(Boolean isMetaOnly) {
		this.isMetaOnly = isMetaOnly;
	}
	
	public DocsMetaData getMetaData() {
		return metaData;
	}
	public void setMetaData(DocsMetaData metaData) {
		this.metaData = metaData;
	}
	public Boolean getIs_only_exh_search() {
		return is_only_exh_search;
	}
	public void setIs_only_exh_search(Boolean is_only_exh_search) {
		this.is_only_exh_search = is_only_exh_search;
	}
	
}
