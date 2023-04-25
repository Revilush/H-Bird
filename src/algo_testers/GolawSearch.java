package algo_testers;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.GroupCommand;
import org.apache.solr.client.solrj.response.GroupResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.fasterxml.jackson.core.JsonProcessingException;

import algo_testers.search_dependencies.AppConfig;
import algo_testers.search_dependencies.AppConfig.SearchAppConfig;
import algo_testers.search_dependencies.AppConfig.SearchConfig;
import algo_testers.search_dependencies.AppConfig.SolrConfig.SolrCoreConfig;
import algo_testers.search_dependencies.DocsMetaData;
import algo_testers.search_dependencies.DocsMetaData.DocMetaData;
import algo_testers.search_dependencies.JSonUtils;
import algo_testers.search_dependencies.SearchFilterHelper;
import algo_testers.search_dependencies.SearchQuery;
import algo_testers.search_dependencies.SearchResponse;
import algo_testers.search_dependencies.SearchResultsGroupSummary;
import algo_testers.search_dependencies.SolrCollapsedExpandedResults;
import algo_testers.search_dependencies.SolrDocumentFieldComparator.SolrDocumentIntFieldComparator;
import algo_testers.search_dependencies.SolrJClient;
import algo_testers.search_dependencies.SpeedTestReport;
import algo_testers.search_dependencies.UserQueryDetails;
import algo_testers.search_dependencies.Utils;
import algo_testers.search_dependencies.WriteLogs;
import algo_testers.search_dependencies.YearToFdateConverter;
import contracts.GoLaw;

public class GolawSearch  {
	protected static Log log = LogFactory.getLog(GolawSearch.class);
	
	public static String contractType = null;
	public static SolrCoreConfig solrCoreCfg;
	public static SearchConfig solrSearchCfg;
	public static SearchAppConfig searchAppConfig;
	public static AppConfig appConfigInstance;

	static {
		appConfigInstance = new AppConfig().setConfigFile(new File(GolawSearch.class.getClassLoader().getResource("app-config.json").getPath()));
		solrCoreCfg = getSolrConfig4Core(contractType);
		searchAppConfig = appConfigInstance.getApiConfig();
		solrSearchCfg = searchAppConfig.getSearchConfig();
	}
	
	public static Map<String, Object> searchConfigParams = new LinkedHashMap<>();
	
	
	private static final String ID_POOL_COLLECTION = "inventory";
	private static final String MAX_ID_POOL_ROW_VALUE = "50000";
	public static final String kidFieldname = "kId";
	public static final String accFieldname = "acc";

	private static Map<String, Set<String>> idPoolCache = new HashMap<>();

	private Boolean isMetaOnly = false;
	private String collections = null;
	public static String solrField_txtCnt = "txtCnt";

	public Boolean headingOnlySearch = false;
	public String headingFieldName = null;
	public Map<String, SolrDocument> doc0KId2KMap;
	
	//Search query parameters //Report them 
	private static String queryString = null;
	private static String yearRange = "2012-2020";
	private static Float[] wordCount = new Float[] {0.5f,2.2f};
	private static String  mm = "60%";
	
	public SearchResponse doTheSearch(String queryText, String contractType) throws IOException, SQLException {
		GolawSearch gs = new GolawSearch();
		SearchQuery searchQuery = new SearchQuery();
		searchQuery.setQ(queryText);
		searchQuery.setContractType(contractType);
		searchQuery.setWithinLastYears(yearRange);
		searchQuery.setWordCntRange(wordCount);
		searchQuery.setMm(mm);
				 
		SpeedTestReport testResponse = gs.searchDo(searchQuery, false, new SpeedTestReport());
		System.out.print(testResponse.getResponse());
		return testResponse.getResponse();
	}

	public static void main(String[] args) throws IOException, SQLException {
		System.out.println("search start");
		GolawSearch gs = new GolawSearch();
		SearchQuery searchQuery = new SearchQuery();
		searchQuery.setQ(queryString);
		searchQuery.setContractType(contractType);
		searchQuery.setWithinLastYears(yearRange);
		searchQuery.setWordCntRange(wordCount);
		searchQuery.setMm(mm);
				 
		SpeedTestReport testResponse = gs.searchDo(searchQuery, false, new SpeedTestReport());
		System.out.print(testResponse.getResponse());
			
	}
	
	public static void setContractType(String contractType) {
		GolawSearch.contractType = contractType;
		solrCoreCfg = getSolrConfig4Core(contractType);
		searchAppConfig = appConfigInstance.getApiConfig();
		solrSearchCfg = searchAppConfig.getSearchConfig();
	}
	
	public static void writeToReport(List<SpeedTestReport> speedTestReports, String folderPath, int fileCount) {
		String filename = "bigloop_report_"+fileCount+".html";
		
		WriteLogs.deleteOldFile(folderPath, filename);
		
		WriteLogs.writeBigloopReportToHtml(folderPath, filename, "<html> <title>GolawSearch</title><body><style>table, th, td { border: 1px solid black;}</style>");
		
		// search config params
		if (searchConfigParams.size() > 0) {
			StringBuilder sb = new StringBuilder("<tr>");
			int col = 0;
			for (String key : searchConfigParams.keySet()) {
				if(key.equals("Note")) {
					sb.append("<table><tr><td style=\"background-color:yellow;\">").append(key).append(" = ").append(searchConfigParams.get(key)).append("</td></tr></table>");
				}else {
				   sb.append("<td>").append(key).append(" = ").append(searchConfigParams.get(key)).append("</td>");
				}
				col++;
				if (col == 4) {
					sb.append("</tr><tr>");
					col = 0;
				}
			}
			sb.append("</tr>");
			WriteLogs.writeBigloopReportToHtml(folderPath,filename, "<table style=\"width:95%; border-collapse: collapse;\"> <caption>Search Configs</caption>" + sb.toString() +  "</table>");
		}
		
		for(SpeedTestReport speedTestReport : speedTestReports ) {
			
			WriteLogs.writeBigloopReportToHtml(folderPath, filename, "<h3 style=\"margin-bottom:10px; text-align: center;\">Report "+speedTestReport.getReportCount()+"</h3>");
			
			String beforgp = "style=\"border:1px solid black;\"";
			String beforgp2 = "style=\"text-align: center;\"";
			
			if(speedTestReport.getBeforeGroupHtxtCnt() >=3000 ) {
				beforgp = "style=\"border:1px solid black; background-color:red; \"";
				beforgp2 = "style=\"border:1px solid black; text-align: center; background-color:red; \"";
			}
			
			WriteLogs.writeBigloopReportToHtml(folderPath, filename, "<table style=\"width:95%; border:1px solid black;\">");
			WriteLogs.writeBigloopReportToHtml(folderPath,filename, "<tr><th style=\"border:1px solid black;\">Speed Solr</th>"
					+ "<th style=\"border:1px solid black;\">Speed Java</th> "
					+ "<th style=\"border:1px solid black;\">Total Time</th>"
					+ "<th style=\"border:1px solid black;\">wCnt Range</th>"
					+ "<th style=\"border:1px solid black;\">MM%</th>"
					+ "<th style=\"border:1px solid black;\">Year Range</th>"
					+ "<th style=\"border:1px solid black;\">Contract</th>"
					+ "<th style=\"border:1px solid black;\">Id Pool Size</th>"
					+ "<th style=\"border:1px solid black;\">Total Results (numFound)</th>"
					+ "<th "+beforgp+">Unique txtCnt</th>"
					+ "<th style=\"border:1px solid black;\">Unique hTxtCnt</th>"
					+ "<th style=\"border:1px solid black;\"># Of Sim Result</th>"
					+ "<th style=\"border:1px solid black;\">UI Results Displayed</th></tr>");
			
			int uiResultCount = 0;
			try {
			if(null != speedTestReport.getResponse().getClauseResults()) {
				uiResultCount = speedTestReport.getResponse().getClauseResults().size();
			}
			}catch(Exception e) {
				
			}
			
			WriteLogs.writeBigloopReportToHtml(folderPath, filename, "<tr><td style=\"text-align: center;\">"+speedTestReport.getSolrSpeed()+"</td> "
					+ "<td style=\"text-align: center;\">"+speedTestReport.getJavaSpeed()+"</td>"
					+ "<td style=\"text-align: center;\">"+speedTestReport.getTotalTime()+"</td> "
					+ "<td style=\"text-align: center;\">"+speedTestReport.getWcnt()+"</td>"
					+ "<td style=\"text-align: center;\"> "+speedTestReport.getMm()+"</a> </td>"
					+ " <td style=\"text-align: center;\">"+speedTestReport.getYearRange()+"</td> "
					+ "<td style=\"text-align: center;\">"+speedTestReport.getContractType()+"</td>"
					+ "<td style=\"text-align: center;\">"+speedTestReport.getIdPoolSize()+"</td>"
							+ "<td style=\"text-align: center;\">"+speedTestReport.getDocFromSolr()+"</td>"
					+ "<td  "+beforgp2+" >"+speedTestReport.getBeforeGroupHtxtCnt()+"</td>"
					+ "<td style=\"text-align: center;\">"+speedTestReport.getAfterGroupHtxtCnt()+"</td>"
					+ "<td style=\"text-align: center;\">"+speedTestReport.getSimResult()+"</td>"
					+ "<td style=\"text-align: center;\">"+uiResultCount+"</td></tr> </table>");
			
			//time report 
			WriteLogs.writeBigloopReportToHtml(folderPath,filename, "<table style=\"width:95%; \"> "
					+ "<tr><th style=\"\">ID Pool QTime</th>"
				//	+ "<th style=\"\">CE Time</th> "
					+ "<th style=\"\">Sim Time</th>"
				//	+ "<th style=\"\">Synonym Time</th>"
					+ "<th style=\"\">Result from Same Para</th>"
					+ "<th style=\"\">edgarLink QTime</th></tr>");
			
			WriteLogs.writeBigloopReportToHtml(folderPath, filename, "<tr><td style=\" text-align: center;\">"+speedTestReport.getIdPoolQueyTime()+"</td> "
				//	+ "<td style=\" text-align: center;\">"+speedTestReport.getCollapseExpandTime()+"</td>"
					+ "<td style=\" text-align: center;\">"+speedTestReport.getSimTime()+"</td> "
				//	+ "<td style=\" text-align: center;\">"+speedTestReport.getSynonymTime()+"</td>"
					+ "<td style=\" text-align: center;\"> "+speedTestReport.getResultFromParaTime()+"</a> </td>"
					+ "<td style=\" text-align: center;\">"+speedTestReport.getEdgarLinkQueryTime()+"</td></tr> </table>");
			
			if (speedTestReport.getOtherProperties().size() > 0) {
				StringBuilder sb = new StringBuilder();
				for (String key : speedTestReport.getOtherProperties().keySet()) {
					sb.append("<td>").append(key).append(" = ").append(speedTestReport.getOtherProperties().get(key)).append("</td>");
				}
				WriteLogs.writeBigloopReportToHtml(folderPath,filename, "<table style=\"width:95%; \"> " + sb.toString() +  "</table>");
			}
			
			
			//end time report
			
			WriteLogs.writeBigloopReportToHtml(folderPath, filename, "<table style=\"width:95%;  margin-top:0px;\">"
					+ "<tr><td style=\"\">Solr Query Link : <a href=\""+speedTestReport.getSolrQuerLink()+"\" target='_blank'> Check Query In Solr</a></td></tr>");
			
			
			WriteLogs.writeBigloopReportToHtml(folderPath, filename, "<tr><td style=\"\"> Query String : "+speedTestReport.getQueryString()+"</td></tr>");
			
			WriteLogs.writeBigloopReportToHtml(folderPath, filename, "</table>");
			
			WriteLogs.writeBigloopReportToHtml(folderPath, filename, "<table style=\"width:95%;  margin-top:0px; margin-bottom:50px;\">");
			
			
			//write solr query to file 
			String solrQueryFileName = "Report_"+speedTestReport.getReportCount()+"_Solr_Query.txt";
			WriteLogs.deleteOldFile(folderPath+"Solr_Querys/", solrQueryFileName);
			WriteLogs.writeSolrQuery(folderPath+"Solr_Querys/", solrQueryFileName, (null == speedTestReport.getSolrQuerLink())?"No query generated":speedTestReport.getSolrQuerLink());
			
			List<SearchResultsGroupSummary> response = null;
			try {
				response = speedTestReport.getResponse().getClauseResults();
			}catch(Exception e) {
				
			}
			
			/*if(speedTestReport.isSimilarityApiApplied()) {
		    	WriteLogs.writeBigloopReportToHtml(folderPath, filename, "<tr><th style=\"\">Text Count(txtCnt)</th><th>Sim Score</th><th style=\"\">Text</th></tr>");
			}else {
			    WriteLogs.writeBigloopReportToHtml(folderPath, filename, "<tr><th style=\"\">Text Count(txtCnt)</th><th style=\"\">Text</th></tr>");
			}*/
			WriteLogs.writeBigloopReportToHtml(folderPath, filename, "<tr><th style=\"\">Text Count(txtCnt)</th><th style=\"\">Text</th></tr>");
			
			try {
				for(int i=0; i<=2; i++) {
					SearchResultsGroupSummary document = response.get(i);
					
					WriteLogs.writeBigloopReportToHtml(folderPath, filename, "<tr><td style=\" text-align: center;\">"+document.getResultsCount()+"</td>"
							//+"<td style=\" text-align: center;\">"+document.getSimScore()+"</td>"
							+ "<td style=\"\">"
							+ "<a href='"+document.getMeta().get("edgarLink")+"' target='_blank'> "+document.getText()+"</a></td></tr>");
				}
			}catch (Exception e) {
				// TODO: handle exception
			}
			if(speedTestReport.getBeforeGroupHtxtCnt() >=3000 ) {
				WriteLogs.writeBigloopReportToHtml(folderPath, filename, "<tr><td>NOTE</td><td style=\"background-color:red;\"> solr finds >3k results, the displayed \"total\" for C/E may still not be same as it'd show for grouping.</td></tr></table>");
			}
			WriteLogs.writeBigloopReportToHtml(folderPath, filename, "</table>");
			
		}
		
		
		speedTestReports.forEach(speedTestReport->{
			
			
		});
		WriteLogs.writeBigloopReportToHtml(folderPath, filename,"</body></html>");
	}

	


	public SpeedTestReport searchDo(SearchQuery searchQuery, Boolean ht2w, SpeedTestReport speedTestReport) throws IOException, SQLException {
		Object apiResp = null;
		SearchQuery sq = null;
		try {
			int SearchStart = (int)System.currentTimeMillis();
			SearchResponse resp = searchNow(searchQuery, speedTestReport);
			resp = prepareForApiResponse(resp, ht2w);
			int processEnd = (int)System.currentTimeMillis();
			int totalTime = processEnd - SearchStart;
			speedTestReport.setTotalTime(totalTime);
			speedTestReport.setJavaSpeed(totalTime - speedTestReport.getSolrSpeed());
			
			speedTestReport.setResponse(resp);
			return speedTestReport;
			//apiResp = resp;
		} catch (Exception e) {
			log.error("Error while search-api: " + JSonUtils.object2JsonString(sq), e);
			System.out.println(e);
		}
		//return JSonUtils.object2JsonString(apiResp);
		return new SpeedTestReport();
	}

	private SearchResponse prepareForApiResponse(SearchResponse resp, Boolean ht2w) {
		SearchResultsGroupSummary srgs = resp.getQueryDetails().getqSummary();
		resetSearchResultGroup(srgs, ht2w);
		for (SearchResultsGroupSummary grp : resp.getClauseResults()) {
			resetSearchResultGroup(grp, ht2w);
		}
		return resp;
	}

	private void resetSearchResultGroup(SearchResultsGroupSummary srgs, Boolean ht2w) {
		srgs.setId(null);
		srgs.setHomogText(null);
		srgs.setAvgSolrScore(null);
		if (Utils.isFalseOrNull(ht2w)) {
			srgs.sethTxtWord2OrgWordIdxList(null);
		}
	}

	public SearchResponse searchNow(SearchQuery query, SpeedTestReport speedTestReport) throws IOException {
		List<SolrDocument> docs = null;
		SearchResponse searchResponse = null;
		List<SearchResultsGroupSummary> resultsGrupSummary = null;
		try {
			docs = search(query, speedTestReport);
		} catch (Exception e) {
			String queryJson = query.getQ();
			try {
				queryJson = JSonUtils.object2JsonString(query);
			} catch (JsonProcessingException e1) {
				log.warn("", e1);
			}
			log.warn("Error while search in solr:" + queryJson, e);
			return null;
		}

		UserQueryDetails uqd = new UserQueryDetails().setUserQuery(query.getQ());

		if (null == docs || isMetaOnly) {
			searchResponse = new SearchResponse(uqd, null, null, null);
			if (isMetaOnly && null != doc0KId2KMap) {
				DocsMetaData dmd = new DocsMetaData();

				List<SolrDocument> doc0s = new ArrayList<>(doc0KId2KMap.values());
				Collections.sort(doc0s, new SolrDocumentIntFieldComparator("fDate", -1));
				for (SolrDocument sd : doc0s) {
					dmd.addMetaData(new DocMetaData(sd));
				}
				searchResponse.setMetaData(dmd);
				searchResponse.setIsMetaOnly(true);
			}
		} else {
			SearchFilterHelper sfh = new SearchFilterHelper(appConfigInstance.getApiConfig(), query, this, appConfigInstance.getMlConfig(), speedTestReport);
			resultsGrupSummary = sfh.filter(docs);
			if (null == resultsGrupSummary)
				resultsGrupSummary = new ArrayList<>();
			searchResponse = new SearchResponse(uqd, resultsGrupSummary, null, null);
		}
        
		return searchResponse;
	}

	public List<SolrDocument> search(SearchQuery query, SpeedTestReport speedTestReport) throws SolrServerException, IOException {
		long start = System.currentTimeMillis();

		Map<String, String> otherParams = new HashMap<>();
		String q = query.getQ();
		Map<String, String> fqsMap = new HashMap<String, String>();

		String yearFq = null;
		Integer defaultFQ = 10;
		String lastQYears = query.getWithinLastYears();
		int difYears = 5;
		int endYear = Calendar.getInstance().get(Calendar.YEAR);
		int startYear = endYear - difYears + 1;
		
		if (StringUtils.isNotBlank(lastQYears)) {
			lastQYears = lastQYears.replaceAll("[ \t\r\n]+", "");
			String[] yrs = lastQYears.split("[-_to]+");
			if (yrs[0].trim().length() == 4)
				startYear = Integer.parseInt(yrs[0].trim());
			if (yrs.length > 1 && yrs[1].trim().length() == 4)
				endYear = Integer.parseInt(yrs[1].trim());
		} else if (null != defaultFQ) {
			Integer ttlYrs = defaultFQ;
			if (null != ttlYrs && ttlYrs >= 0) {
				difYears = ttlYrs;
				startYear = endYear - difYears + 1; // 5 years from 2020: 2016-2020
			}
		}
		
		List<String> queryYears = new ArrayList<>();
		if (startYear <= endYear) {
			for (int year = startYear; year <= endYear; year++) {
				queryYears.add(String.valueOf(year));
			}
			fqsMap.put("fDate", YearToFdateConverter.getFdateRangeClause(startYear, endYear));
		}
		speedTestReport.setYearCount(queryYears.size());

		if(StringUtils.isNoneBlank(speedTestReport.getShrinkDaysFQ()) && speedTestReport.isShrinkQuery()) {
			fqsMap.put("fDate", speedTestReport.getShrinkDaysFQ());
		}
		
        Set<String> documentids = null;
		
		if(query.isContractTypeSearch()) {
			if (StringUtils.isNotBlank(query.getContractType())) {
				fqsMap.put("contractType", "contractType:"+query.getContractType()+"");
			}
		}else {
			String kType = StringUtils.defaultIfBlank(query.getContractType(), query.getContractNameAlgo());
			if(StringUtils.isNotBlank(kType)) {
				Map<String, SolrCoreConfig>  contractTypeConfig = appConfigInstance.getSolrConfig().getContractTypeToCoreConfig();
				
				if(contractTypeConfig.containsKey(kType)) {
					int timeBeforeIdPool = (int)System.currentTimeMillis();
					//get document id pool from solr
					if(StringUtils.isNoneBlank(speedTestReport.getShrinkDaysFQ()) && speedTestReport.isShrinkQuery()) {
						documentids = getDocumentIdPool(kType, speedTestReport.getShrinkDaysFQ(), speedTestReport);
					}else {
						documentids = getDocumentIdPool(kType, YearToFdateConverter.getFdateRangeClause(startYear, endYear), speedTestReport);
					}
			    	int timeAfterIdPool = (int)System.currentTimeMillis();
			    	speedTestReport.setIdPoolQueyTime(timeAfterIdPool - timeBeforeIdPool);
			    	speedTestReport.setIdPoolSize(documentids.size());
			    	speedTestReport.setContractType("ContractType : "+ kType);
				}else {
					String contractNameText = makeInitialAllCaps(kType);
					if(org.apache.commons.lang.StringUtils.isNotBlank(contractNameText)) {
					  fqsMap.put("contractNameAlgo", "contractNameAlgo:" +contractNameText+ "");
					  speedTestReport.setContractType("contractNameAlgo : "+ makeInitialAllCaps(kType));
					}
				}
			} 
		}

		// get collection name
		//collections = YearToFdateConverter.getCollectionName(solrCoreCfg.getCoreName(), queryYears);
		collections = "fin_2010_2020";
		
		
		if (StringUtils.isBlank(collections)) {
			log.warn("There is no collection for given year range");
			return new ArrayList<SolrDocument>();
		}

		if (StringUtils.isNotBlank(query.getWithinContracts()) || StringUtils.isNotBlank(query.getWithinParties())
				|| StringUtils.isNotBlank(query.getCik())) {
			// first search only doc0 that contain given contracts/parties, then resume with
			// next step of search.
			// doc0 should also be restricted within years desired
			String ids = getDoc0IDs(query, yearFq, documentids);

			if (StringUtils.isBlank(ids)) {
				return new ArrayList<SolrDocument>();
			} else {
				String fq = "kId:(" + ids + ")";
				fqsMap.put("kId", fq);
			}
		} else {
			if (null != documentids && !documentids.isEmpty()) {
				String fq = "kId:(" + StringUtils.join(documentids, " ") + ")";
				fqsMap.put("kId", fq);
			}
		}

		// if it is only doc0 search - q/kwds/clause etc are all null/empty
		if (StringUtils.isBlank(query.getKeywords()) && StringUtils.isBlank(query.getClause())
				&& StringUtils.isBlank(query.getQ()) && StringUtils.isBlank(query.getExh())
				&& StringUtils.isBlank(query.getContractNameAlgo())) {
			isMetaOnly = true;
			return null;
		}

		if (StringUtils.isNotBlank(query.getKeywords())) {
			String k = query.getKeywords().replaceAll("[\\s]+", " ");
			// Proximity FQ: Solr uses "ab cd"~5 to indicate ab and cd are within 5 words
			// distance.
			// user can type: "ab cd"5 and we need to change to what Solr expects.
			k = k.replaceAll("\"\\s*(\\d+)", "\"~$1");
			String fq = "txt:(" + k + ")";
			fqsMap.put("txt", fq);
		}

		// any custom filters
		if (null != query.getCustomFilters()) {
			for (String field : query.getCustomFilters().keySet()) {
				String fq = field + ":(" + query.getCustomFilters().get(field) + ")";
				fqsMap.put(field, fq);
			}
		}

		// clauses
		if (StringUtils.isNotBlank(query.getClause())) {
			// search in ANY of these fields: sec/hdg/mHd/sub/def/exh
			// change user's proximity-term to what solr understands:: "foo bar"5 >= "foo
			// bar"~5
			String c = query.getClause().replaceAll("\"\\s*(\\d+)", "\"~$1");

			if (c.trim().charAt(0) != '"' && !(c.contains(" AND ") || c.contains(" OR ") || c.contains(" NOT "))) {
				c = c.trim().replaceAll("\\s+", " AND ");
				log.trace("Clause with quotes---> " + c);
			}

			// hdgs are in Initial caps or ALL caps (therefore if user types in lowercase -
			// please convert).
			// Eg force majeure to be converted to: (Force Majeure) OR (FORCE MAJEURE)
			// NOTE: do not change the words within dbl-quotes
			c = makeInitialAllCaps(c);

			String fq = "sec:(" + c + ") OR hdg:(" + c + ") OR mHd:(" + c + ") OR sub:(" + c + ") OR def:(" + c + ")"; 
			fqsMap.put("sec", fq);
			
			// extra FQ for 'typ' to indicate we need only sent type results
			fqsMap.put("type", "typ:(1)");
			// min-wCnt filter as well
			if (null != solrSearchCfg.getMinWcntForClauseSearch()) {
				String wCntField = StringUtils.defaultIfBlank(solrSearchCfg.getWordCountFieldName(), "wCnt");
				fqsMap.put("Wcnt", wCntField + ":[" + solrSearchCfg.getMinWcntForClauseSearch() + " TO *]");
			}
		} else {
			// No clause is provided - put typ:[1,3]
			fqsMap.put("typ", "typ:(1 OR 3)");
		}
		if (StringUtils.isNotBlank(query.getExh())) {
			String exh = query.getExh().replaceAll("\"\\s*(\\d+)", "\"~$1");

			String fq = "exh:(" + query.getExh() + ")";
			fqsMap.put("exh", fq);
			
			if (StringUtils.isBlank(query.getClause()) && StringUtils.isBlank(query.getQ())
					&& StringUtils.isBlank(query.getKeywords())) {
				// extra FQ for 'typ' to indicate we need only sent type results
				// fqs = ArrayUtils.add(fqs, "typ:(1)");
				this.setHeadingOnlySearch(true);
				this.setHeadingFieldName("exh");
			}

		}

		boolean isAllUpperCase = (StringUtils.isNotBlank(q) && q.equals(q.toUpperCase()));

		// we use lancaster stemmer to get stemmed-words (hTxt)
		int hCnt = 0;
		String hTxt = q;
		if (Utils.isTrue(solrSearchCfg.isHomogenizeQueryText())) {
			// use new stemmer - GoLawHtxt
			GoLaw gl = new GoLaw();
			hTxt = gl.goLawGetHtxt(q); // removes all caps letter (any word starting with cap removed)
			if (isAllUpperCase) {
				hTxt = gl.goLawGetHtxt(q.toLowerCase());
			}
			if (Utils.isTrue(solrSearchCfg.getWildcardQueryWords())) {
				hTxt = StringUtils.join(hTxt.split(" "), "* ") + "*";
			}
			hCnt = hTxt.split(" ").length;
		}

		// see if we need to apply wCnt filter as well - depending on number of query's
		// hTxt words.
		if (hCnt >= solrSearchCfg.getMinQHtxtCountToApplyCountRangeFilter()) {
			int len = hCnt;

			int minW;
			int maxW;
			if (solrSearchCfg.getWordCountFieldName().equalsIgnoreCase("wCnt")) {
				len = q.split(" ").length;
			}

			Float[] wCntRange;

			if (ArrayUtils.isNotEmpty(query.getWordCntRange())) {
				wCntRange = query.getWordCntRange();
			} else {
				wCntRange = solrSearchCfg.getWordCntFilterRange();
			}

			if (query.getIs_broadining()) {
				wCntRange = solrSearchCfg.getBroadeningConfig().getWordCntFilterRange();
			}
			minW = (int) (len * wCntRange[0]);
			maxW = (int) (len * wCntRange[1]);

			String fq = solrSearchCfg.getWordCountFieldName() + ":[" + minW + " TO " + maxW + "]";
			fqsMap.put("wCnt", fq);

			speedTestReport.getOtherProperties().put("wCnt FQ", fq);
		} else {
			speedTestReport.getOtherProperties().put("wCnt fq NOT applied, hCnt", hCnt);
		}
		
		// extra/raw query parameters from config
		if (null != solrSearchCfg.getRawQueryParams()) {
			for (String field : solrSearchCfg.getRawQueryParams().keySet()) {
				otherParams.put(field, solrSearchCfg.getRawQueryParams().get(field) + "");
			}
		}
		if (StringUtils.isNotEmpty(query.getMm())) {
			otherParams.replace("mm", query.getMm());
		}
		
		if(! query.isGroupFQ()) {
			// needed for Collapse/Expand to work
		     if (null != solrSearchCfg.getReadyFqs()) {
			  String CE = solrSearchCfg.getReadyFqs().toArray(new String[0])[0];
			  fqsMap.put("CE", CE);
	    	}
		} else if(query.isGroupFQ()) {
			otherParams.remove("expand");
			otherParams.remove("expand.rows");
			otherParams.put("group","true");
			otherParams.put("group.field","hashTxtId");
		}

		if (query.getIs_broadining()) {
			otherParams.put("mm", (String) solrSearchCfg.getBroadeningConfig().getMm());
		}

		String searchAgainstField = solrSearchCfg.getSearchAgainstField();
		if (isAllUpperCase) { // user has given a sent and it is all upper case
			// lower-case the Q, hTxt it, suffix * to each word, and make upper again and
			// search against txt
			GoLaw gl = new GoLaw();
			q = gl.goLawGetHtxt(q.toLowerCase());
			q = StringUtils.join(q.split(" "), "* ").trim() + "*";
			q = q.toUpperCase();

			searchAgainstField = "txt";
		} else if (Utils.isTrue(solrSearchCfg.isHomogenizeQueryText())) {
			q = hTxt;
		}

		if (StringUtils.isBlank(q)) {
			q = "*:*"; // txt:(*) takes a very very long time (30+ seconds) hence it should be *:*
			// searchAgainstField = "*";
		} else {
			q = searchAgainstField + ":(" + q + ")";
		}
		
		if(StringUtils.isBlank(query.getClause()) && StringUtils.isBlank(query.getKeywords()) && hCnt < solrSearchCfg.getMinQHtxtCountToApplyCountRangeFilter()) {
			boolean applyMM = true;
			String wCntRange = "wCnt:[10 TO 120]";
			fqsMap.put("wCnt", wCntRange);
			
			if (hCnt <= 4) {
				hTxt = hTxt + "~7";
				q = "txt:(" + hTxt + ")";
				applyMM = false;
			}

			if (applyMM) {
				if (hCnt > 10 && hCnt <= 16)
					otherParams.put("mm", "70%");
				if (hCnt <= 10)
					otherParams.put("mm", "80%");
			}

			String fDate = "";
			if (hCnt <= 4) {
				DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
				LocalDate currentDate = LocalDate.now();
				String currentYearDate = currentDate.format(dateFormatter);
				String lastYearDate = currentDate.minusYears(1l).format(dateFormatter);
				
				fDate = "fDate:[" + lastYearDate + " TO " + currentYearDate + "]";
				
				fqsMap.put("fDate", fDate);
			}
			
			fqsMap.put("typ","typ:(3)");
			
		}
		String[] fqs = fqsMap.values().toArray(new String[0]);
		// search solr core now
		List<SolrDocument> docs = fetchResultsAndFilterByScore(query, q, fqs, null, otherParams, collections, speedTestReport);

		if (log.isInfoEnabled())
			log.info("SolrSearchHelper.search(): solr search done in millis:" + (System.currentTimeMillis() - start)
					+ ", " + System.currentTimeMillis());

		return docs;
	}

	public static String makeInitialAllCaps(String c) {
		// split words by punct/space that are to be converted to this style - ignore
		// ones in ""
		String spacePunctPtrn = "(?<=[\\s\\p{Punct}}&&[^\\*]])|(?=[\\s\\p{Punct}}&&[^\\*]])";
		String[] parts = c.trim().split(spacePunctPtrn);

		// String punctReserves = "[\\p{Punct}}\s\\d]+|AND|OR|NOT";
		String not2Modify = "(?i)([\\p{Alpha}}]|AND|OR|NOT)"; // ignore case - when 'and/or/not' etc comes in query text
																// then it should go as it is.
		String ht;

		GoLaw gl = new GoLaw();
		StringBuilder resp = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			if (parts[i].equals("\"")) {
				// we hit a double-quote - DO NOT CHANGE anything inside double-quotes
				resp.append(parts[i]);
				// keep upto next " as-it-is
				for (i++; i < parts.length; i++) {
					resp.append(parts[i]);
					if (parts[i].equals("\""))
						break;
				}
			} else {
				// if word/part is a letter/AND/OR/NOT - keep as it is
				if (parts[i].matches(not2Modify)) // punctReserves))
					resp.append(parts[i]);
				else {
					if (parts[i].length() > 1) {
						String p = parts[i].toLowerCase();
						// if its a stop word (can't survive in hTxt) then ignore
						// need to lower case since hTxt stemmer will remove any word starting with
						// capital letter
						ht = gl.goLawGetHtxt(p);
						if (StringUtils.isBlank(ht))
							continue;
						// this word is standing single/on-its-own - convert to initial/ALL caps
						resp.append("(").append(p.substring(0, 1).toUpperCase()).append(p.substring(1)).append(" OR ")
								.append(p.toUpperCase()).append(")");
					} else {
						// single letter word!! add as it is - ie spaces/punctuations etc that comes
						// from split
						resp.append(parts[i]);
					}
				}
			}
		}
		String respStr = resp.toString();
		respStr = respStr.replaceAll("(AND|OR|NOT)[\\s]+(AND|OR)", "$1");
		return respStr;
	}

	public Set<String> getDocumentIdPool(String contractType, String yearFq, SpeedTestReport speedTestReport) throws SolrServerException {
		String idPoolCacheKey = contractType + "_" + yearFq;
		if (idPoolCache.containsKey(idPoolCacheKey)) {
			return idPoolCache.get(idPoolCacheKey);
		}
		
		String[] fqs = null;

		if (StringUtils.isNotBlank(contractType)) {
			fqs = ArrayUtils.add(fqs, "contractType:\"" + contractType + "\"");
		}

		if (StringUtils.isNotBlank(yearFq)) {
			fqs = ArrayUtils.add(fqs, yearFq);
		}

		String q = "*:*";
		String[] fields2Return = null;
		Map<String, String> otherParams = new HashMap<>();

		otherParams.put("rows", MAX_ID_POOL_ROW_VALUE);

		SolrJClient solrClient = new SolrJClient(solrCoreCfg.getBaseServerUrl(), ID_POOL_COLLECTION);

		SolrQuery solrQuery = solrClient.getSolrQuery(q, fqs, fields2Return, otherParams);
		log.info(solrCoreCfg.getBaseServerUrl() + solrCoreCfg.getCoreName() + "/select?" + solrQuery + ", "
				+ System.currentTimeMillis());

		QueryResponse resp = null;
		try {
			resp = solrClient.search(solrQuery);
		} catch (Exception e) {
			log.error("Error while geting id pool " + e);
			throw new SolrServerException(e.getMessage());
		}

		speedTestReport.addOtherProperty("idPoolSolrQRT",  resp.getQTime());		//(Integer) resp.getHeader().get("QTime") +"/"+
		
		SolrDocumentList docs = resp.getResults();
		Set<String> ids = docs.stream().map(doc -> (String) doc.getFieldValue(kidFieldname))
				.collect(Collectors.toSet());

		log.info("kIds found for id pool=" + ids.size() + ", fqs=" + ArrayUtils.toString(fqs));
		
		idPoolCache.put(idPoolCacheKey, ids);

		return ids;
	}
	
	public String kIdToId(String kid) {
		return kid+"_0_0_0_0_0";
	}

	public Map<String, SolrDocument> searchDoc0ForKIds(Collection<String> kIds, SpeedTestReport speedTestReport)
			throws SolrServerException, IOException {
		this.doc0KId2KMap = new LinkedHashMap<>();

		String[] fqs = null;
		Set<String> ids = new TreeSet<>();
		for (String kid : kIds) {
			ids.add(kIdToId(kid));		// make full ID from kId
		}
		fqs = ArrayUtils.add(fqs, "id:(" + StringUtils.join(ids, " OR ") + ")");
		
		Map<String, String> otherParams = new HashMap<>();
		otherParams.put("rows", kIds.size() + "");

		SolrJClient solrClient;

		if (null != collections) {
			solrClient = new SolrJClient(solrCoreCfg.getBaseServerUrl());
		} else {
			solrClient = new SolrJClient(solrCoreCfg.getBaseServerUrl(), solrCoreCfg.getCoreName());
		}
		//fqs = ArrayUtils.add(fqs, accFieldname + ":(*)");

		SolrQuery solrQuery = solrClient.getSolrQuery("*:*", fqs, new String[] { "*" }, otherParams);
		QueryResponse resp;

		if (null != collections) {
			resp = solrClient.searchWithMultipleCollections(solrQuery, collections);
		} else {
			resp = solrClient.search(solrQuery);
		}

		SolrDocumentList docs = resp.getResults();
		resp.getResults();
		speedTestReport.addOtherProperty("edgarLinkSolrQRT",  resp.getQTime());		//(Integer) resp.getHeader().get("QTime") +"/"+
		
		Object kid;
		// sometime we may get docs other than doc0 also in result. if so, ignore them
		for (SolrDocument sd : docs) {
			kid = sd.getFieldValue(kidFieldname);
			doc0KId2KMap.put(kid.toString(), sd);
		}
		return doc0KId2KMap;
	}

	private String getDoc0IDs(SearchQuery query, String yearFq, Set<String> documentids)
			throws SolrServerException, IOException {
		doc0KId2KMap = new LinkedHashMap<>();

		String[] fqs = null;
		if (StringUtils.isNotBlank(query.getWithinContracts())) {
			String c = makeInitialAllCaps(query.getWithinContracts().replaceAll("\"\\s*(\\d+)", "\"~$1"));
			fqs = ArrayUtils.add(fqs, "(contractNameAlgo:(" + c + ") OR contractLongName:(" + c + "))");
		}
		if (StringUtils.isNotBlank(query.getWithinParties())) {
			String p = makeInitialAllCaps(query.getWithinParties().replaceAll("\"\\s*(\\d+)", "\"~$1"));

			if (StringUtils.isNotBlank(p)) {
				fqs = ArrayUtils.add(fqs, "openingParagraph:(" + p + ")");
			}
		}
		if (StringUtils.isNotBlank(query.getCik())) {
			fqs = ArrayUtils.add(fqs, "cik:(" + query.getCik() + ")");
		}
		if (null != documentids  &&  !documentids.isEmpty()) {
			Set<String> ids = new TreeSet<>();
			for (String kid : documentids) {
				ids.add(kIdToId(kid));
			}
			String fq = "id:(" + StringUtils.join(ids, " ") + ")";		// exact ID match - only for doc0 ids
			fqs = ArrayUtils.add(fqs, fq);
		}
		if (null != solrSearchCfg.getDoc0DefaultFQs()) {
			for (String fq : solrSearchCfg.getDoc0DefaultFQs()) {
				fqs = ArrayUtils.add(fqs, fq);
			}
		}
		if (StringUtils.isNotBlank(yearFq))
			fqs = ArrayUtils.add(fqs, yearFq);
		// any custom filters
		if (null != query.getCustomDoc0Filters()) {
			for (String field : query.getCustomDoc0Filters().keySet()) {
				String fq = field + ":(" + query.getCustomDoc0Filters().get(field) + ")";
				fqs = ArrayUtils.add(fqs, fq);
			}
		}
		// telling solr to send only doc0 (where acc field has a value)
		fqs = ArrayUtils.add(fqs, accFieldname + ":(*)");

		String q = "*:*";
		String[] fields2Return = null; // {kidFieldname}; indicate we need all fields from doc0
		Map<String, String> otherParams = new HashMap<>();
		if (null != solrSearchCfg.getDoc0RawQueryParams()) {
			for (String field : solrSearchCfg.getDoc0RawQueryParams().keySet()) {
				otherParams.put(field, solrSearchCfg.getRawQueryParams().get(field) + "");
			}
		}

		SolrJClient solrClient;

		if (null != collections) {
			solrClient = new SolrJClient(solrCoreCfg.getBaseServerUrl());
		} else {
			solrClient = new SolrJClient(solrCoreCfg.getBaseServerUrl(), solrCoreCfg.getCoreName());
		}

		SolrQuery solrQuery = solrClient.getSolrQuery(q, fqs, fields2Return, otherParams);
		log.info(solrCoreCfg.getBaseServerUrl() + solrCoreCfg.getCoreName() + "/select?" + solrQuery + ", "
				+ System.currentTimeMillis());

		QueryResponse resp;

		if (null != collections) {
			resp = solrClient.searchWithMultipleCollections(solrQuery, StringUtils.join(collections, ","));
		} else {
			resp = solrClient.search(solrQuery);
		}

		SolrDocumentList docs = resp.getResults();
		Set<String> ids = new HashSet<>();
		Object kid;
		for (SolrDocument sd : docs) {
			if (null == sd || sd.size() == 0)
				continue;
			kid = sd.getFieldValue(kidFieldname);
			if (null == kid)
				continue;
			ids.add(kid.toString());
			doc0KId2KMap.put(kid.toString(), sd);
		}
		log.info("IDs found=" + ids.size() + ", solrDocs:" + docs.size() + ", fqs=" + ArrayUtils.toString(fqs));
		if (!ids.isEmpty()) {
			return new StringBuilder(StringUtils.join(ids, " ")).toString();
		}
		return StringUtils.EMPTY;
	}

	private List<SolrDocument> fetchResultsAndFilterByScore(SearchQuery query, String q, String[] fqs,
			String[] fields2Return, Map<String, String> otherParams, String collections, SpeedTestReport speedTestReport)
			throws SolrServerException, IOException {
		if (log.isInfoEnabled() && null != query) {
			log.debug("starting search: " + System.currentTimeMillis());
			log.info("userQuery= " + query.getQ() + " ::: q=" + q);
		}

		SolrJClient solrClient;

		if (null != collections) {
			solrClient = new SolrJClient(solrCoreCfg.getBaseServerUrl());
		} else {
			solrClient = new SolrJClient(solrCoreCfg.getBaseServerUrl(), solrCoreCfg.getCoreName());
		}

		SolrQuery solrQuery = solrClient.getSolrQuery(q, fqs, fields2Return, otherParams);
		log.info(solrCoreCfg.getBaseServerUrl() + collections + "/select?" + solrQuery + ", "+ System.currentTimeMillis());
		
		speedTestReport.setSolrQuerLink(solrCoreCfg.getBaseServerUrl() + collections + "/select?" + solrQuery);
		

		QueryResponse resp;

		if (null != collections) {
			int timeBeforeSolr = (int)System.currentTimeMillis();
			resp = solrClient.searchWithMultipleCollections(solrQuery, collections);
			int solrTime = (int)System.currentTimeMillis();
			speedTestReport.setSolrSpeed(solrTime-timeBeforeSolr);
		} else {
			resp = solrClient.search(solrQuery);
		}
		
        if(null != resp) {
			if(query.isGroupFQ()) {
				speedTestReport.setDocFromSolr(resp.getGroupResponse().getValues().get(0).getMatches());
			}else {
				speedTestReport.setDocFromSolr(resp.getResults().getNumFound());		// collapsed numFound
				speedTestReport.setNumFound((int)resp.getResults().getNumFound());
			}
        }
		if(query.isGroupFQ()) {
			GroupResponse grupResp = resp.getGroupResponse();
			if (null == grupResp) {
				return new ArrayList<>();
			}

			List<Group> grups;
			SolrDocumentList docs;
			List<SolrDocument> allGroupedDocs = new ArrayList<>();
			long numFound;
			SolrDocument sd;
			int total = 0;
			for (GroupCommand gc : grupResp.getValues()) {
				total += gc.getMatches(); // total matches/numFound for the group
				gc.getName(); // group-field name (ie hashTxtId)
				grups = gc.getValues();
				for (Group gr : grups) {
					gr.getGroupValue(); // id (group field value)
					docs = gr.getResult();
					numFound = docs.getNumFound();
					sd = docs.get(0);
					// add txtCnt to doc
					sd.addField(solrField_txtCnt, numFound);
					allGroupedDocs.add(sd);
				}
			}
			return allGroupedDocs;
			
		}else {
			int timeBeforeCE = (int)System.currentTimeMillis();
			SolrCollapsedExpandedResults collapsedresults = new SolrCollapsedExpandedResults(solrQuery)
					.setNumFoundFieldName(solrField_txtCnt).setResultConsolidationRequired(true);
			collapsedresults.parseCollapsedExpandedResults(resp);
			List<SolrDocument> allGroupedDocs = collapsedresults.getCollapsedExpandedResults();
			int timeAfterCE = (int)System.currentTimeMillis();
			speedTestReport.setCollapseExpandTime(timeAfterCE - timeBeforeCE);
			long total = collapsedresults.getTotalNumFound();
			long totalDocCountAfterCE = speedTestReport.getDocFromSolr() + collapsedresults.expandedDocCount;
			speedTestReport.setDocFromSolr(totalDocCountAfterCE);

			//System.out.println("CE count :"+allGroupedDocs.size());
			
			return allGroupedDocs;
		}


	}

	public Boolean getHeadingOnlySearch() {
		return headingOnlySearch;
	}

	public void setHeadingOnlySearch(Boolean headingOnlySearch) {
		this.headingOnlySearch = headingOnlySearch;
	}

	public String getHeadingFieldName() {
		return headingFieldName;
	}

	public void setHeadingFieldName(String headingFieldName) {
		this.headingFieldName = headingFieldName;
	}

	public Map<String, SolrDocument> getDoc0KId2KMap() {
		return doc0KId2KMap;
	}

	public void setDoc0KId2KMap(Map<String, SolrDocument> doc0kId2KMap) {
		doc0KId2KMap = doc0kId2KMap;
	}
	
	public static SolrCoreConfig getSolrConfig4Core(String contractType) {
		SolrCoreConfig solrCoreCfg = null;
        if (null != contractType && StringUtils.isNotBlank(contractType)) {
			solrCoreCfg = appConfigInstance.getSolrConfig().getCoreConfig(contractType);
		}
		if (null == solrCoreCfg) {
			solrCoreCfg = appConfigInstance.getSolrConfig()
					.getCoreConfig(AppConfig.getInstance().getSolrConfig().getDefaultContractType());
		}
		return solrCoreCfg;
	}

	
	
	public static void writeToReport(List<SpeedTestReport> speedTestReports) {
		WriteLogs.writeSpeedReportToHtml("<html> <title>GolawSearch</title><body>");
		
		for(SpeedTestReport speedTestReport : speedTestReports ) {
            
			WriteLogs.writeSpeedReportToHtml("<h3 style=\"margin-bottom:10px; text-align: center;\">Report "+(speedTestReports.indexOf(speedTestReport)+ 1)+"</h3>");
			
			WriteLogs.writeSpeedReportToHtml("<table style=\"width:95%; border:1px solid black;\">");
			WriteLogs.writeSpeedReportToHtml("<tr><th style=\"border:1px solid black;\">Speed Solr</th>"
					+ "<th style=\"border:1px solid black;\">Speed Java</th> "
					+ "<th style=\"border:1px solid black;\">Total Time</th>"
					+ "<th style=\"border:1px solid black;\">wCnt Range</th>"
					+ "<th style=\"border:1px solid black;\">MM%</th>"
					+ "<th style=\"border:1px solid black;\">Year Range</th>"
					+ "<th style=\"border:1px solid black;\">ContractType</th>"
					+ "<th style=\"border:1px solid black;\">Id Pool Size</th>"
					+ "<th style=\"border:1px solid black;\">Before Group Htxt count</th>"
					+ "<th style=\"border:1px solid black;\">After Group Htxt count</th>"
					+ "<th style=\"border:1px solid black;\"># Of Sim Result</th>"
					+ "<th style=\"border:1px solid black;\">Total txtCnt</th></tr>");
			
			WriteLogs.writeSpeedReportToHtml("<tr><td style=\"border:1px solid black;\">"+speedTestReport.getSolrSpeed()+"</td> "
					+ "<td style=\"border:1px solid black;\">"+speedTestReport.getJavaSpeed()+"</td>"
					+ "<td style=\"border:1px solid black;\">"+speedTestReport.getTotalTime()+"</td> "
					+ "<td style=\"border:1px solid black;\">"+speedTestReport.getWcnt()+"</td>"
					+ "<td style=\"border:1px solid black;\"> "+speedTestReport.getMm()+"</a> </td>"
					+ " <td style=\"border:1px solid black;\">"+speedTestReport.getYearRange()+"</td> "
					+ "<td style=\"border:1px solid black;\">"+speedTestReport.getContractType()+"</td>"
					+ "<td style=\"border:1px solid black;\">"+speedTestReport.getIdPoolSize()+"</td>"
					+ "<td style=\"border:1px solid black;\">"+speedTestReport.getBeforeGroupHtxtCnt()+"</td>"
					+ "<td style=\"border:1px solid black;\">"+speedTestReport.getAfterGroupHtxtCnt()+"</td>"
					+ "<td style=\"border:1px solid black;\">"+speedTestReport.getSimResult()+"</td>"
					+ "<td style=\"border:1px solid black;\">"+speedTestReport.getResponse().getClauseResults().size()+"</td></tr>");
			
			WriteLogs.writeSpeedReportToHtml("<tr><td style=\"border:1px solid black;\">Solr Query Link : <a href=\""+speedTestReport.getSolrQuerLink()+"\"> Check Query In Solr</a></td></tr>");
			WriteLogs.writeSpeedReportToHtml("<tr><td style=\"border:1px solid black;\"> Query String : "+speedTestReport.getQueryString()+"</td></tr>");
			
			WriteLogs.writeSpeedReportToHtml("</table>");
			
			WriteLogs.writeSpeedReportToHtml("<table style=\"width:95%; border:1px solid black; margin-top:0px; margin-bottom:50px;\">");
			WriteLogs.writeSpeedReportToHtml("<tr><th style=\"border:1px solid black;\">hTxtId</th>"
					+ "<th style=\"border:1px solid black;\">Text Count(txtCnt)</th>"
					+ "<th style=\"border:1px solid black;\">Text Link</th>"
					+ "<th style=\"border:1px solid black;\">Text</th></tr>");
			
			
			List<SearchResultsGroupSummary> rsponse = speedTestReport.getResponse().getClauseResults(); 
			try {
				for(int i=0; i<=2; i++) {
					SearchResultsGroupSummary document = rsponse.get(i);
					
					WriteLogs.writeSpeedReportToHtml("<tr><td style=\"border:1px solid black;\">"+document.getHTxtId()+"</td> "
							+ "<td style=\"border:1px solid black;\">"+document.getResultsCount()+"</td>"
							+ "<td style=\"border:1px solid black;\"><a href='"+document.getMeta().get("edgarLink")+"' target='_blank'> "+document.getMeta().get("edgarLink")+"</a></td>"
							+ "<td style=\"border:1px solid black;\">"+document.getText()+"</td></tr>");
				}
			}catch (Exception e) {
				// TODO: handle exception
			}
			
			WriteLogs.writeSpeedReportToHtml("</table>");	
		}
		
		
		speedTestReports.forEach(speedTestReport->{
			
			
		});
		WriteLogs.writeSpeedReportToHtml("</body></html>");
	}

}
