package algo_testers.search_dependencies;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * This class parses collapsed-expanded (C/E) solr results, keeps them in separate structures, 
 * and offers method to merge both (effectively "numFound" only - adds numFound param value from expanded results to collapsed results, based on collapsedFieldName).
 * 
 *
 */
public class SolrCollapsedExpandedResults extends SolrSearchResults {
	

	protected String collapsedFieldName = null;
	protected List<SolrDocument> mergedResults;
	protected Map<String, SolrDocumentList> expandedDocs;
	
	protected String numFoundFieldName = "numFound";		// default numFound fieldName would be 'numFound', but user can change
	
	// on solr-cloud with >1 shards, C/E results brings one result from each shard. Need to merge/consolidate them based on collapsed field.
	protected boolean resultConsolidationRequired = false;
	public int expandedDocCount = 0;

	/**
	 * query param is used to identify collapsed field
	 * @param query
	 */
	public SolrCollapsedExpandedResults(SolrQuery query) {
		// identify the collapsed field
		findCollapsedField(query.getFilterQueries());
	}
	
	
	public boolean isResultConsolidationRequired() {
		return resultConsolidationRequired;
	}
	@JsonIgnore
	public SolrCollapsedExpandedResults setResultConsolidationRequired(boolean resultConsolidationRequired) {
		this.resultConsolidationRequired = resultConsolidationRequired;
		return this;
	}
	@JsonIgnore
	public SolrCollapsedExpandedResults setNumFoundFieldName(String fieldName) {
		numFoundFieldName = fieldName;
		return this;
	}
	
	public void parseCollapsedExpandedResults(QueryResponse response) {
		this.mergedResults = null;
		expandedDocs = new LinkedHashMap<>();
		long expandedDocCount = 0;
		NamedList<Object> objs = response.getResponse();
		if (null != objs) {
			SimpleOrderedMap<?> expandResp = (SimpleOrderedMap<?>)objs.get("expanded");
			if (null != expandResp  &&  expandResp.size() > 0) {
				int expandSize = expandResp.size();
				SolrDocumentList sdlNew, sdl0;
				for (int i=0; i < expandSize; i++) {
					sdlNew = (SolrDocumentList) expandResp.getVal(i);
					// see if we already have this expanded field/value name - we may get multiple results in 'expanded' with >1 shards in solr-cloud.
					sdl0 = expandedDocs.get(expandResp.getName(i));
					if (null == sdl0) {
						sdl0 = sdlNew;
						expandedDocCount = expandedDocCount + sdl0.getNumFound();
					} else {
						//sdl0.setMaxScore(?);		// do we merge/avg score also? ideally both scores should be same as C/E value is same.
						//sdl0.setStart(?);			// can start be different?
						sdl0.setNumFound(sdl0.getNumFound() + sdlNew.getNumFound());		// merge numFound
						sdl0.addAll(sdlNew);												// merge list of docs
						expandedDocCount = expandedDocCount + sdlNew.getNumFound();
					}
					expandedDocs.put(expandResp.getName(i), sdl0);
				}
			}
		}
		// pick the collapsed results - comes in 'response' param as comes for a regular search 
		super.parseSearchResults(response);
		this.expandedDocCount = (int) expandedDocCount;
		// put 'numFound' in each result found
		// by default - put numFound as 1 since collapsed result gives only a single doc per group, if "expand" is not applied
		for (SolrDocument sd : super.results) {
			sd.addField(numFoundFieldName, 1L);			// long type
		}
		
		if (resultConsolidationRequired  &&  StringUtils.isNotBlank(collapsedFieldName)) {
			/*
			 * when there are multiple shards (solr-cloud) we may get multiple docs with same value in collapsedField. 
			 * Ideally they should have been merged/consolidated with those many numFound. 
			 * Lets do that now.
			 */
			Map<Object, SolrDocument> ceField2SolrDocMap = new LinkedHashMap<>();		// imp: keep the order solr returns results in
			Object ceVal;
			SolrDocument sd0;
			for (SolrDocument sd : super.results) {
				ceVal = sd.getFieldValue(collapsedFieldName);
				sd.setField("score", 1f);
				sd0 = ceField2SolrDocMap.get(ceVal);
				if (null == sd0) {
					ceField2SolrDocMap.put(ceVal, sd);
				} else {
					// sum/merge numFound field
					sd0.setField(numFoundFieldName, ((Long)sd0.getFieldValue(numFoundFieldName)) + ((Long)sd.getFieldValue(numFoundFieldName)) );
				}
			}
			// make a new solrDocList and assign to super 'result'
			SolrDocumentList newRes = new SolrDocumentList();
			newRes.addAll(ceField2SolrDocMap.values());
			super.results.clear();			// allow for GC
			super.results = newRes;
			super.results.size();
		}
		
	}
	
	/**
	 * Merges the collapsed results with expanded results - it'd affect "numFound" for each doc.
	 * It tries to identify collapsed field name from FQs given in query (provided in constructor), but better to set 'collapsedFieldName' manually when possible, before calling this method.
	 * @return
	 */
	public List<SolrDocument> getCollapsedExpandedResults() {
		if (null == mergedResults) {
			// all collapsed results
			if (StringUtils.isBlank(collapsedFieldName)) {
				log.warn("was asked to merge collapsed+expanded results, but I could not identify collapsed fieldName! Pl provide it before calling method 'getMergedResults'");
			}
			if (expandedDocs.size() > 0  &&  StringUtils.isNotBlank(collapsedFieldName)) {
				mergedResults = new ArrayList<>();
				SolrDocumentList expandedDocsList;
				Object fldValObj;
				long numFound;
				for (SolrDocument sd : results) {
					sd.setField("score", 1f);
					numFound = 1;			// default numFound is 1 - the only/single result of collapsed docs. We may get multiple collapsed docs for >1 shards.
					if (sd.containsKey(numFoundFieldName))
						numFound = ((Long)sd.getFieldValue(numFoundFieldName));
					fldValObj = sd.getFieldValue(collapsedFieldName);
					if (null != fldValObj) {
						expandedDocsList = expandedDocs.get(fldValObj.toString());
						if (null != expandedDocsList) {
							numFound += expandedDocsList.getNumFound();		// real numFound for this field value is: expanded docs count + collapsed doc's numFound 
						}
					}
					sd.setField(numFoundFieldName, numFound);
					mergedResults.add(sd);
				}
			} else {
				// in the absence of expanded results or if we could not identify collapsedField, treat that we found a single doc only for each collapsed field value
				// we have already put 1 as numFound in each doc in super.parse method
				mergedResults = results;
			}
		}
		return mergedResults;
	}
	
	// ********************************* 
	
	public String getCollapsedFieldName() {
		return collapsedFieldName;
	}

	public void setCollapsedFieldName(String collapsedFieldName) {
		this.collapsedFieldName = collapsedFieldName;
	}

	public Map<String, SolrDocumentList> getExpandedDocs() {
		return expandedDocs;
	}
	
	public SolrDocumentList getCollapsedDocs() {
		return results;
	}

	

	// *********************************
	
	private String findCollapsedField(String[] FQs) {
		String clpFldRegex = "\\{\\!collapse[ ]{1,5}field[ =]{1,3}(\\w+).+";
		for (String fq : FQs) {
			if (fq.matches(clpFldRegex)) {
				Matcher matcher = Pattern.compile(clpFldRegex).matcher(fq);
				if (matcher.find()) {
					collapsedFieldName = matcher.group(1);
					//System.out.println("collapsedField="+collapsedField);
					break;
				}
			}
		}
		return collapsedFieldName;
	}
	
}
