package search;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import charting.JSonUtils;
import search.resultComponents.SearchResult;
import xbrl.EasyHttpClient;
import xbrl.FileSystemUtils;


public class SearchHandler {

	private static final Logger logger = Logger.getLogger(SearchHandler.class.getName());

	public List<SearchResult> handleSearch(SearchFilter filter, int start,
			SearchConstants.MODE mode)
			throws IOException {
		
		/*
		 * PropertiesManager mgr = PropertiesManager.getInstance(); StringBuffer
		 * finalURL = new StringBuffer(mgr.getCommonProperty("solr.server.url"));
		 * finalURL.append(mgr.getCommonProperty("select.request.url.path")).append("?")
		 * ;? //if (mode == SearchConstants.MODE.AND) // finalURL.append("&q.op=AND");
		 * finalURL.append(mgr.getCommonProperty("search.query.params"));
		 */
		
		StringBuffer finalURL = new StringBuffer(getSolrServerUrl());
		
		List<String> finalSearchUrls = filter.appendFiltersToSearchUrl(finalURL.toString());
		System.out.println("finalSearchUrls=" + finalSearchUrls);
		List<SearchResult> responses = new ArrayList<SearchResult>();
		for (int i=0; i < finalSearchUrls.size(); i++) {
			String finalSearchUrl = finalSearchUrls.get(i);
			System.out.println("finalSearchUrls=" + finalSearchUrls.get(i));
			SearchResult searchResult = getSearchResults(finalSearchUrl, "");
			if (null == searchResult)		// no result for this query
				continue;
			/*
			// if no facet filter has been applied yet (i.e. its an initial search), we need to search 2nd time within the hl fields only, to get only those sections that satisfy the query terms
			if (filter.facetFilters.size() == 0  &&  response.highlighting.size() > 0) {
				// now get ALL "HL" fields - from all documents returned, and search again in those dynamic fields and adjust facets as per the hl fields returned now
				Set<String> allHlFields = new TreeSet<String>();
				Iterator<String> itr = response.highlighting.keySet().iterator();
				Map<String, List<String>> fieldNValues;
				while(itr.hasNext()) {
					fieldNValues = (Map<String, List<String>>)response.highlighting.get(itr.next());
					allHlFields.addAll(fieldNValues.keySet());
				}
				
				//TODO: search once again - post form this time
				filter.searchInFields.addAll(allHlFields);
				finalSearchUrl = filter.appendFiltersToSearchUrl(finalURL.toString());
				System.out.println("finalSearchUrl in 2nd time=" + finalSearchUrl);
				response = getSearchResults_PostForm(finalSearchUrl);
			}*/

			if (logger.isDebugEnabled())
				logger.debug("Search raw response parsed.." + searchResult);
			//System.out.println("finalSearchUrl=" + finalSearchUrl);
			/*if (null == searchResult)
				searchResult = new SearchResult();*/
			searchResult.queryStr = filter.sentences.get(i);
			searchResult.finalSearchUrl = finalSearchUrl;
			responses.add(searchResult);
			System.out.println("searchResult=" + searchResult);
		}
		
		return responses;
	}

	public SearchResult handleSearch(String query, int start, SearchConstants.MODE mode) throws IOException {
		PropertiesManager mgr = PropertiesManager.getInstance();
		String serverUrl = mgr.getCommonProperty("solr.server.url");
		String selectPath = mgr.getCommonProperty("select.request.url.path");
		StringBuffer finalURL = new StringBuffer(serverUrl);
		finalURL.append(selectPath);
		try {
			query = URLEncoder.encode(query, "UTF-8");
		} catch(Exception ex) { }
		StringBuffer queryString = new StringBuffer("?q=").append(query);
		if (mode == SearchConstants.MODE.AND)
			queryString.append("&q.op=AND");
		queryString.append("&start=").append(start);
		queryString.append(mgr.getCommonProperty("search.query.params"));
		if (logger.isDebugEnabled())
			logger.debug("About to search. url:" + finalURL + ":: Query string:" + queryString);
		// call Solr, get search result xml, and parse it  
		SearchResult response = getSearchResults(finalURL.toString(), queryString.toString());
		if (logger.isDebugEnabled())
			logger.debug("Search raw response parsed.." + response);
		if (null != response)
			response.queryStr = query;
		return response;
	}
	
	public void searchFieldValues(String query, String field) throws IOException {
		SearchFilter filter = new SearchFilter();
		filter.q = query;
		filter.searchInFields.add(field);
		
		StringBuffer finalURL = new StringBuffer(getSolrServerUrl());
		List<String> finalSearchUrls = filter.appendFiltersToSearchUrl(finalURL.toString());

		List<SearchResult> responses = new ArrayList<SearchResult>();
		for (int i=0; i < finalSearchUrls.size(); i++) {
			String finalSearchUrl = finalSearchUrls.get(i);
			SearchResult searchResult = getSearchResults(finalSearchUrl, "");
			if (null == searchResult)		// no result for this query
				continue;
			//System.out.println("finalSearchUrl=" + finalSearchUrl);
			searchResult.queryStr = filter.sentences.get(i);
			searchResult.finalSearchUrl = finalSearchUrl;
			responses.add(searchResult);
		}
	}
	
	private String getSolrServerUrl() {
		PropertiesManager mgr = PropertiesManager.getInstance();
		StringBuffer finalURL = new StringBuffer(mgr.getCommonProperty("solr.server.url"));
		finalURL.append(mgr.getCommonProperty("select.request.url.path")).append("?");
		finalURL.append(mgr.getCommonProperty("search.query.params"));
		return finalURL.toString();
	}

	public static SearchResult getSearchResults(String url, String queryStr) throws IOException {
		return getSearchResults(url, queryStr, null);
	}
	
	public static SearchResult getSearchResults(String url, String queryStr, String saveResultTo) throws IOException {
		 
		String json = getPage(url + queryStr);
		// System.out.println("json="+json);
		// System.out.println("getPage-url+queryStr="+url+queryStr);
		if (StringUtils.isNotBlank(json)) {
			if (StringUtils.isNotBlank(saveResultTo)) {
				
				/*
				 * save the json into the file: saveResultTo NOTE: I COMMENTED OUT THE WRITE TO
				 * FILE - THIS MAY CAUSE ISSUES WITH OTHER METHODS THAT EXPECTED A FILE TO HAVE
				 * BEEN WRITTEN
				 */
				
				System.out.println("save search results to="+saveResultTo);
				FileSystemUtils.writeToAsciiFile(saveResultTo, json);
				
			}

			return JSonUtils.json2Object(json, SearchResult.class);

		}
		return null;
	}

/*	private SearchResult getSearchResults_PostForm(String url) throws IOException {
		String[] parts = url.split("\\&");
		url = parts[0];
		Map<String, String> params = new HashMap<String, String>();
		String[] paramParts;
		for (int i=1; i < parts.length; i++) {
			paramParts = parts[i].split("=");
			if (! params.containsKey(paramParts[0]) && paramParts.length > 1)
				params.put(paramParts[0], paramParts[1]);
			else
				url += "&"+parts[i];
		}
		params.put("hl.simple.pre", "<b>");
		params.put("hl.simple.post", "</b>");
		String json = postPage(url, params);
		return JSonUtils.json2Object(json, SearchResult.class);
	}
*/
	
	public static String getPage(String url) throws IOException {
		EasyHttpClient httpClient = new EasyHttpClient(true);
		String html = httpClient.makeHttpRequest("Get", url, null, -1, null,
				null);
		return html;
	}
	public static String postPage(String url, Map<String, String> params) throws IOException {
		EasyHttpClient httpClient = new EasyHttpClient(true);
		String html = httpClient.makeHttpRequest("Post", url, null, -1, null,
				params);
		return html;
	}


	
	public static void main(String[] args) throws IOException {
		SearchHandler handler = new SearchHandler();
		String searchQuery = "law";
		SearchResult res = handler.handleSearch(searchQuery, 0, SearchConstants.MODE.AND);
		
//		System.out.println("JSonUtils="+JSonUtils.object2JsonString(res));
	}

}
