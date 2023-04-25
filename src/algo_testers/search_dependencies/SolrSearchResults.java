package algo_testers.search_dependencies;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;


public class SolrSearchResults {
	
	protected static Log log = LogFactory.getLog(SolrSearchResults.class);


	protected long totalNumFound;
	protected long start;
	protected Float maxScore;
	protected boolean isNumFoundExact = true;
	
	protected SolrDocumentList results;

	
	public SolrDocumentList parseSearchResults(QueryResponse response) {
		results = response.getResults();
		maxScore = results.getMaxScore();
		totalNumFound = results.getNumFound();
		isNumFoundExact = Utils.isTrue(results.getNumFoundExact());	
		start = results.getStart();
		
		return this.results;
	}
	
	public long getTotalNumFound() {
		return totalNumFound;
	}

	public long getStart() {
		return start;
	}

	public Float getMaxScore() {
		return maxScore;
	}

	public boolean isNumFoundExact() {
		return isNumFoundExact;
	}

	public SolrDocumentList getResults() {
		return results;
	}
	
	
}
