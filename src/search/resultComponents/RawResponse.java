package search.resultComponents;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import search.resultComponents.SearchResult.DiscardDocsCriteria;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class RawResponse {

	@JsonProperty("numFound")
	public int numFound;
	
	@JsonProperty("start")
	public int start;
	
	@JsonProperty("maxScore")
	public float maxScore;
	
	@JsonProperty("docs")
	public List<RawDocument> docs;
	
	
	public List<RawDocument> discardedDocs = new ArrayList<RawDocument>();
	
	
	public void discardDocs(DiscardDocsCriteria criteria) {
		if (null != criteria.docScoreDeltaWithinPercent)
			discardDocsOnScoreDelta(criteria.docScoreDeltaWithinPercent);
	}

	
	
	private void discardDocsOnScoreDelta(Float docScoreDeltaWithinPercent) {
		if (null == docs || docs.size() == 0)
			return;
		RawDocument firstDocInGroup=null, rd = docs.get(0);
		float lastScore = Float.MAX_VALUE, delta = 0;
		if (null != docScoreDeltaWithinPercent)
			delta = rd.score * (docScoreDeltaWithinPercent/100);
		int groupCount = 0;
		Iterator<RawDocument> docsItr = docs.iterator();
		while (docsItr.hasNext()) {
			rd = docsItr.next();
			if (rd.score >= (lastScore - delta) ) {
				// doc's score is within the discarding range
				docsItr.remove();
				discardedDocs.add(rd);
				groupCount ++;
			} else {
				if (groupCount > 0  &&  null != firstDocInGroup)
					firstDocInGroup.discardedDocsCount = groupCount;
				// this doc's score is lesser to be discarded - we now need to adjust the score and delta..
				lastScore = rd.score;
				firstDocInGroup = rd;
				groupCount = 0;
				/* should we reset the delta also ??
				if (null != docScoreDeltaWithinPercent)
					delta = rd.score * (docScoreDeltaWithinPercent/100);*/
			}
		}
	}
	
	
	
	
}
