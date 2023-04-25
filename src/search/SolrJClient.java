package search;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;

public class SolrJClient {

	private SolrClient solrClient;
	private static final float defaultInputFieldBoost = 1;
	
	protected String connectionUrl;
	private Integer commitWithinMillis;
	
	
	// **  constructors **
	public SolrJClient(String baseSolrUrl, String coreName) {
		String urlString = StringUtils.removeEnd(baseSolrUrl, "/") +"/"+ StringUtils.removeStart(coreName, "/");
		this.connectionUrl = urlString;
		solrClient = new HttpSolrClient.Builder(urlString)
				.withConnectionTimeout(10000)
			    .withSocketTimeout(60000)
			    .build();
	}
	public SolrJClient(String baseSolrUrl, String coreName, boolean toPostOnly) {
		String urlString = StringUtils.removeEnd(baseSolrUrl, "/") +"/"+ StringUtils.removeStart(coreName, "/");
		this.connectionUrl = urlString;
		if (toPostOnly) {
			/*SSLContext sslcontext = SSLContexts.createSystemDefault();
	    	SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
	    	        sslcontext, new String[] {"TLSv1.2", "TLSv1.1", "TLSv1", "SSLv3" }, null,
	    	        SSLConnectionSocketFactory.getDefaultHostnameVerifier());
	    	Registry<ConnectionSocketFactory> sockConnRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
	    	        .register("http", PlainConnectionSocketFactory.INSTANCE)
	    	        .register("https", sslConnectionSocketFactory)
	    	        .build();
	    	PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(sockConnRegistry);
			CloseableHttpClient httpClient = HttpClientBuilder.create()
			    	.setConnectionManager(connMgr)
			    	.build();*/
			//solrClient = new ConcurrentUpdateSolrClient(urlString, 100, 4);
			solrClient = new ConcurrentUpdateSolrClient.Builder(connectionUrl)
					.withConnectionTimeout(10000)
				    .withSocketTimeout(60000)
				    .build();
		} else {
			solrClient = new HttpSolrClient.Builder(urlString)
			.withConnectionTimeout(10000)
		    .withSocketTimeout(60000)
		    .build();
		}
	}
	/*
	public SolrJClient(String solrCloudUrl) {
		solrClient = new CloudSolrClient(solrCloudUrl);
	}
	*/
	
	
	// **  builder  "with.... methods"
	public SolrJClient withCommitWithinMillis(int millis) {
		commitWithinMillis = millis;
		return this;
	}
	
	
	public String getConnectionUrl() {
		return connectionUrl;
	}
	// **  public methods
	public UpdateResponse commit() throws SolrServerException, IOException {
		return solrClient.commit();
	}
	
	public void deleteDocsByQuery(String query) throws SolrServerException, IOException {
		int millis = (null != commitWithinMillis)? commitWithinMillis : 100;
		solrClient.deleteByQuery(query, millis);
		commit();
	}
	public void deleteAllDocs() throws SolrServerException, IOException {
		deleteDocsByQuery("*:*");
	}
	
	
	public UpdateResponse postDocsToSolr(List<SolrInputDocument> docs) throws SolrServerException, IOException {
		if (null != commitWithinMillis)
			return solrClient.add(docs, commitWithinMillis);
		else
			return solrClient.add(docs);
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

	public QueryResponse search(Map<String,String> fieldName_Value) throws SolrServerException, IOException {
		SolrParams params = new MapSolrParams(fieldName_Value);
	    QueryResponse response = solrClient.query(params);
	    return response;
	}
	public QueryResponse search(String q, String[] filterQs, String[] fields2Return, Map<String, String> otherParams) throws SolrServerException, IOException {
		SolrQuery query = makeQuery(q, filterQs, fields2Return, otherParams);
	    /*	example...
	    query.setQuery("sony digital camera");
	    query.addFilterQuery("cat:electronics","store:amazon.com");
	    query.setFields("id","sentence","category","score");
	    query.setStart(0);    
	    query.set("defType", "edismax");
	    */
	    QueryResponse response = solrClient.query(query);
	    return response;
	}
	
	public QueryResponse search(SolrQuery query) throws SolrServerException, IOException {
		 QueryResponse response = solrClient.query(query);
		 return response;
	}
	
	public static SolrQuery makeQuery(String q, String[] filterQs, String[] fields2Return, Map<String, String> otherParams) {
		SolrQuery query = new SolrQuery();
	    query.setQuery(q);
	    if (null != filterQs  &&  filterQs.length > 0)
	    	query.addFilterQuery(filterQs);
	    if (null != fields2Return  &&  fields2Return.length > 0)
	    	query.setFields(fields2Return);
	    if (null != otherParams  &&  otherParams.size() > 0) {
	    	for (String fld : otherParams.keySet())
	    	    query.set(fld, otherParams.get(fld));
	    }
	    query.setStart(0);
	    return query;
	}
	
}
