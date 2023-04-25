package algo_testers.search_dependencies;

import java.io.FileNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Holds details about the query/text user searched.
 *
 */
public class UserQueryDetails {
	
	protected static Log log = LogFactory.getLog(UserQueryDetails.class);
	
	/*
	@JsonProperty("searchQuery")
	private SearchQuery searchQuery;
	*/
	
	@JsonProperty("qSummary")
	private SearchResultsGroupSummary qSummary;

	
	public SearchResultsGroupSummary getqSummary() {
		return qSummary;
	}
	public void setqSummary(SearchResultsGroupSummary qSummary) {
		this.qSummary = qSummary;
	}
	
	public UserQueryDetails setUserQuery(String query) throws FileNotFoundException {
		if (null == qSummary)
			qSummary = new SearchResultsGroupSummary();
		qSummary.setText(query);		
		qSummary.setHomogText(qSummary.getHomogenizedText(query));
		qSummary.createHtxtWordToOrgWordMap();
		qSummary.setwCnt(qSummary.getHomogText().split(" ").length);
		return this;
	}


	
	
}
