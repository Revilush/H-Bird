package algo_testers.search_dependencies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.CursorMarkParams;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;

public class SolrJClient {
	private static final String SOLR_PASSWORD = "Jm2vY25H8O5b";

	private static final String SOLR_USERNAME = "solradmin";
	protected static Log log = LogFactory.getLog(SolrJClient.class);
	
	protected SolrClient solrClient;
	protected static final float defaultInputFieldBoost = 1;
	
	protected String connectionUrl;
	
	protected Integer commitWithinMillis;
	
	protected SolrQuery lastSearchedQuery;
	
	protected boolean paginatedSearchEnabled = false;
	protected boolean noMorePagedResults = true;
	protected String sortByUniqueField = "id asc";
	protected String nextCursorMark = null;
	
	
	// **  constructors **
	public SolrJClient(String baseSolrUrl, String coreName) {
		String urlString = StringUtils.removeEnd(baseSolrUrl, "/") +"/"+ StringUtils.removeStart(coreName, "/");
		this.connectionUrl = urlString;
		//solrClient = new HttpSolrClient(urlString);
		solrClient = new HttpSolrClient.Builder(urlString)
				.withConnectionTimeout(10000)
			    .withSocketTimeout(60000)
			    .build();
	}
	public SolrJClient(String baseSolrUrl, String coreName, boolean toPostOnly) {
		String urlString = StringUtils.removeEnd(baseSolrUrl, "/") +"/"+ StringUtils.removeStart(coreName, "/");
		this.connectionUrl = urlString;
		if (toPostOnly) {
			solrClient = new ConcurrentUpdateSolrClient.Builder(connectionUrl)
					.withConnectionTimeout(10000)
				    .withSocketTimeout(60000)
				    .build();
		} else {
			//solrClient = new HttpSolrClient(urlString);
			solrClient = new HttpSolrClient.Builder(urlString)
					.withConnectionTimeout(10000)
				    .withSocketTimeout(60000)
				    .build();
		}
	}
	
	public SolrJClient(String baseSolrUrl) {
		solrClient = new HttpSolrClient.Builder(baseSolrUrl)
				.withConnectionTimeout(10000)
			    .withSocketTimeout(60000)
			    .build();
	}
	
	public SolrClient getInternalSolrClient() {
		return solrClient;
	}
	
	public String getConnectionUrl() {
		return connectionUrl;
	}
	// **  builder  "with.... methods"
	public SolrJClient withCommitWithinMillis(int millis) {
		commitWithinMillis = millis;
		return this;
	}
	public void setCommitWithinMillis(Integer commitWithinMillis) {
		this.commitWithinMillis = commitWithinMillis;
	}
	
	// **  public methods
	public UpdateResponse commit() throws SolrServerException, IOException {
		return (UpdateResponse) new UpdateRequest()
        .setAction(UpdateRequest.ACTION.COMMIT, true, true)
        .setBasicAuthCredentials(SOLR_USERNAME, SOLR_PASSWORD)
        .process(solrClient, null);
	}
	
	// ************************************************************* //
	
	public void deleteDocsByQuery(String query) throws SolrServerException, IOException {
		int millis = (null != commitWithinMillis)? commitWithinMillis : 100;
		UpdateRequest req = new UpdateRequest();
	    req.deleteByQuery(query);
	    req.setCommitWithin(millis);
	    req.setBasicAuthCredentials(SOLR_USERNAME, SOLR_PASSWORD);
	    req.process(solrClient, null);
		commit();
	}
	public void deleteAllDocs() throws SolrServerException, IOException {
		deleteDocsByQuery("*:*");
	}
	
	// ************************************************************* //

	public UpdateResponse postMapsToSolr(List<Map<String, Object>> docs) throws SolrServerException, IOException {
		List<SolrInputDocument> siDocs = new ArrayList<>();
		for (Map<String, Object> m : docs)
			siDocs.add(getSolrInputDocument(m));
		return postDocsToSolr(siDocs);
	}

	public UpdateResponse postDocsToSolr(List<SolrInputDocument> docs) throws SolrServerException, IOException {
		if (null != commitWithinMillis) {
			return postDocsToSolrWithAuth(docs, commitWithinMillis);
		}else {
			return postDocsToSolrWithAuth(docs, -1);
		}	
	}
	
	public UpdateResponse postDocsToSolrWithAuth(List<SolrInputDocument> docs, Integer millis) throws SolrServerException, IOException {
		UpdateRequest req = new UpdateRequest();
        req.add(docs);
        req.setCommitWithin(millis);
        req.setBasicAuthCredentials(SOLR_USERNAME, SOLR_PASSWORD);
        return req.process(solrClient, null);
	}
	
	public SolrInputDocument getSolrInputDocument(Map<String, Object> nameValues) {
		return getSolrInputDocument(nameValues, null);
	}
	public SolrInputDocument getSolrInputDocument(Map<String, Object> nameValues, Float boost) {
		SolrInputDocument doc = new SolrInputDocument();
		for (String field : nameValues.keySet()) {
			doc.addField(field, nameValues.get(field));
		}
		return doc;
	}
	
	public static SolrInputDocument addValueToDoc(SolrInputDocument doc, String fieldName, Object value, Float boost) {
		SolrInputField fld = doc.getField(fieldName);
		if (null == fld)
			doc.addField(fieldName, value);
		else
			fld.addValue(value);
		return doc;
	}

	// ************************************************************* //
	
	public QueryResponse search(SolrQuery query) throws SolrServerException, IOException {
		lastSearchedQuery = new SolrQuery(query.getQuery());
		lastSearchedQuery.add(query);
		QueryRequest  queryRequest = new QueryRequest(lastSearchedQuery);
		queryRequest.setBasicAuthCredentials(SOLR_USERNAME, SOLR_PASSWORD);
		
		initializePagination();
		QueryResponse response = queryRequest.process(solrClient);
		prepareForNextPage(response);
	    return response;
	}
	
	public QueryResponse searchWithMultipleCollections(SolrQuery query, String collections) throws SolrServerException, IOException {
		lastSearchedQuery = new SolrQuery(query.getQuery());
		lastSearchedQuery.add(query);
		QueryRequest  queryRequest = new QueryRequest(lastSearchedQuery, METHOD.POST);
		queryRequest.setBasicAuthCredentials(SOLR_USERNAME, SOLR_PASSWORD);
		
		initializePagination();
		QueryResponse response = queryRequest.process(solrClient,collections);
		prepareForNextPage(response);
		return response;
	}

	public QueryResponse search(Map<String,String> fieldName_Value) throws SolrServerException, IOException {
		SolrParams params = new MapSolrParams(fieldName_Value);
		lastSearchedQuery = new SolrQuery(params.toQueryString());
		initializePagination();
	    QueryResponse response = solrClient.query(lastSearchedQuery);			//solrClient.query(params);
	    prepareForNextPage(response);
	    return response;
	}
	public SolrQuery getSolrQuery(String q, String[] filterQs, String[] fields2Return, Map<String, String> otherParams) {
		lastSearchedQuery = getSolrQueryInstance(q, filterQs, fields2Return, otherParams);
		initializePagination();
		/*
	    lastSearchedQuery.setQuery(q);
	    if (null != filterQs  &&  filterQs.length > 0)
	    	lastSearchedQuery.addFilterQuery(filterQs);
	    if (null != fields2Return  &&  fields2Return.length > 0)
	    	lastSearchedQuery.setFields(fields2Return);
	    if (null != otherParams  &&  otherParams.size() > 0) {
	    	for (String fld : otherParams.keySet())
	    	    lastSearchedQuery.set(fld, otherParams.get(fld));
	    }
	    */
	    return lastSearchedQuery;
	}

	public static SolrQuery getSolrQueryInstance(String q, String[] filterQs, String[] fields2Return, Map<String, String> otherParams) {
		SolrQuery solrQueryInst = new SolrQuery();
	    solrQueryInst.setQuery(q);
	    if (null != filterQs  &&  filterQs.length > 0)
	    	solrQueryInst.addFilterQuery(filterQs);
	    if (null != fields2Return  &&  fields2Return.length > 0)
	    	solrQueryInst.setFields(fields2Return);
	    if (null != otherParams  &&  otherParams.size() > 0) {
	    	for (String fld : otherParams.keySet())
	    	    solrQueryInst.set(fld, otherParams.get(fld));
	    }
	    return solrQueryInst;
	}

	public QueryResponse search(String q, String[] filterQs, String[] fields2Return, Map<String, String> otherParams) throws SolrServerException, IOException {
		lastSearchedQuery = getSolrQuery(q, filterQs, fields2Return, otherParams);
	    //lastSearchedQuery.setStart(0);
	    QueryResponse response = solrClient.query(lastSearchedQuery);
	    prepareForNextPage(response);
	    return response;
	}
	
	
	public QueryResponse searchByParams(SolrParams params) throws SolrServerException, IOException {
		lastSearchedQuery = new SolrQuery();
		initializePagination();
		lastSearchedQuery.setQuery(params.toQueryString());
		QueryResponse response = solrClient.query(lastSearchedQuery);
		prepareForNextPage(response);
		return response;
	}
	
	public QueryResponse getNextPage() throws SolrServerException, IOException {
		if (null == lastSearchedQuery  ||  null == this.nextCursorMark)
			throw new RuntimeException("No query was run before calling getNextPage, and/or search pagination was not enabled!");
		if (noMorePagedResults)
			return null;
		lastSearchedQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, nextCursorMark);
		QueryResponse response = solrClient.query(lastSearchedQuery);
		prepareForNextPage(response);
		return response;
	}
	
	public SolrQuery getLastSearchedQuery() {
		return lastSearchedQuery;
	}
	
	
	public boolean isPaginatedSearchEnabled() {
		return paginatedSearchEnabled;
	}
	/**
	 * When pagination is enabled, Solr needs to know how it'll sort the results (ie basis on which field value) to support pagination.
	 * The sort field list must have atleast one UNIQUE value field (i.e. id etc).
	 
	 * //The field provided must be a document's field with UNIQUE values and not a pseudo/generated field (ie score) of solr.
	 * @param paginatedSearchEnabled
	 * @param sortByUniqueField	one or more fields and corresponding sort-order (asc/desc) in the format:  "field order, field order" etc. For ex: "score asc, id asc"
	 */
	public void setPaginatedSearchEnabled(boolean paginatedSearchEnabled, String sortByUniqueField) {
		this.paginatedSearchEnabled = paginatedSearchEnabled;
		this.sortByUniqueField = sortByUniqueField;
		if (paginatedSearchEnabled  &&  StringUtils.isBlank(sortByUniqueField))
			throw new RuntimeException("Pagination requires to sort on ");
	}
	
	public String getNextCursorMark() {
		return nextCursorMark;
	}
	
	public void releaseResources() {
		if (null != solrClient) {
			try {
				solrClient.close();
				solrClient = null;
			} catch (IOException e) {
				log.warn("", e);
			}
		}
	}
	// ************************************************************* //
	
	@Override
	protected void finalize() {
		releaseResources();
	}
	
	// ************************************************************* //
	
	private void initializePagination() {
		this.nextCursorMark = null;
		if (paginatedSearchEnabled) {
			
			String[] parts, sortBy = sortByUniqueField.replaceAll("[ ]{1,}", " ").trim().split(",");		//"score desc, id asc"
			lastSearchedQuery.clearSorts();
			for (String field_order : sortBy) {
				parts = field_order.trim().split(" ");
				lastSearchedQuery.addSort(parts[0].trim(), ORDER.valueOf(parts[1].trim()));
			}
			//lastSearchedQuery.setSort(sortByUniqueField);
			lastSearchedQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START);
			noMorePagedResults = false;
		}
	}
	
	private void prepareForNextPage(QueryResponse response) {
		if (StringUtils.equals(this.nextCursorMark, response.getNextCursorMark())) {
			noMorePagedResults = true;
		}
		this.nextCursorMark = response.getNextCursorMark();
	}
	
}
