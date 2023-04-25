package algo_testers.search_dependencies;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import algo_testers.search_dependencies.AppConfig.SolrConfig.SolrCoreConfig;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {

	protected static Log log = LogFactory.getLog(AppConfig.class);
	
	private static AppConfig instance = null;
	
	@JsonProperty("solr-config")
	private SolrConfig solrConfig;
	
	@JsonProperty("ml-config")
	private MLConfig mlConfig;
	
	@JsonProperty("query-builder-config")
	private QueryBuilderConfig queryBuilderConfig;
	
	private SearchAppConfig uiConfig = null;
	
	private SearchAppConfig apiConfig = null;
	
	@JsonIgnore
	public static Map<String, SolrCoreConfig> contractType2CoreMap = new TreeMap<>();
	
	public static Map<String, SolrCoreConfig> getContractType2CoreMap() {
		return contractType2CoreMap;
	}


	public static void setContractType2CoreMap(Map<String, SolrCoreConfig> contractType2CoreMap) {
		AppConfig.contractType2CoreMap = contractType2CoreMap;
	}



	public AppConfig() {
	}
	
	public QueryBuilderConfig getQueryBuilderConfig() {
		return queryBuilderConfig;
	}
	public void setQueryBuilderConfig(QueryBuilderConfig queryBuilderConfig) {
		this.queryBuilderConfig = queryBuilderConfig;
	}	

	public SearchAppConfig getUiConfig() {
		return uiConfig;
	}

	public SearchAppConfig getApiConfig() {
		return apiConfig;
	}

	public SolrConfig getSolrConfig() {
		return solrConfig;
	}
	public void setSolrConfig(SolrConfig solrConfig) {
		this.solrConfig = solrConfig;
	}

	public MLConfig getMlConfig() {
		return mlConfig;
	}
	public void setMlConfig(MLConfig mlConfig) {
		this.mlConfig = mlConfig;
	}


	// ************************************************
	// ************************************************
	public static AppConfig getInstance() {
		return instance;
	}

	/**
	 * Called by external config tool (ie Spring).
	 * @return 
	 */
	public AppConfig setConfigFile(File file) {
		String json = null;
		try {
			json = FileSystemUtils.readTextFromFile(file);
			instance = JSonUtils.json2Object(json, AppConfig.class);
			
			// UI-cfg
			Map<String, Object> cfg = JSonUtils.json2Map(json);
			Map<String, Object> commonCfg = (Map<String, Object>) cfg.get("common-config");
			Map<String, Object> uiCfg = (Map<String, Object>) cfg.get("UI-config");
			uiCfg = ObjectUtils.deepMergeMaps(uiCfg, commonCfg, true);
			instance.uiConfig = JSonUtils.map2Object(uiCfg, SearchAppConfig.class);
			// API-cfg
			cfg = JSonUtils.json2Map(json);
			commonCfg = (Map<String, Object>) cfg.get("common-config");
			Map<String, Object> apiCfg = (Map<String, Object>) cfg.get("API-config");
			apiCfg = ObjectUtils.deepMergeMaps(apiCfg, commonCfg, true);
			instance.apiConfig = JSonUtils.map2Object(apiCfg, SearchAppConfig.class);
			
			return instance;
			
		} catch (IOException e) {
			System.out.print(e);
			log.warn("Error reading config text from resource file:", e);
		}
		return null;
	}
	

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SearchAppConfig {
		@JsonProperty("show-debug-info-on-ui")
		private boolean showDebugInfoOnUi = false;

		@JsonProperty("enable-auditing_write-to-files")
		private boolean enableAuditingWriteToFiles = false;
		@JsonProperty("folder-for-audit-files")
		private String folderForAuditFiles;
		
		@JsonProperty("apply-synonyms")
		private boolean applySynonyms = false;
		@JsonProperty("conform-final-results-2-query")
		private Boolean conformFinalResultsToQuery;
		
		@JsonProperty("enable-HL1")
		private Boolean enableHL1;
		@JsonProperty("enable-HL2")
		private Boolean enableHL2;
		@JsonProperty("enable-RL")
		private Boolean enableRL;
		
		@JsonProperty("search-config")
		private SearchConfig searchConfig;
		@JsonProperty("search-filter-config")
		private SearchFilterConfig searchFilterConfig;
		
		public boolean isShowDebugInfoOnUi() {
			return showDebugInfoOnUi;
		}
		public void setShowDebugInfoOnUi(boolean showDebugInfoOnUi) {
			this.showDebugInfoOnUi = showDebugInfoOnUi;
		}
		public boolean isEnableAuditingWriteToFiles() {
			return enableAuditingWriteToFiles;
		}
		public void setEnableAuditingWriteToFiles(boolean enableAuditingWriteToFiles) {
			this.enableAuditingWriteToFiles = enableAuditingWriteToFiles;
		}
		public String getFolderForAuditFiles() {
			return folderForAuditFiles;
		}
		public void setFolderForAuditFiles(String folderForAuditFiles) {
			this.folderForAuditFiles = folderForAuditFiles;
		}
		public boolean isApplySynonyms() {
			return applySynonyms;
		}
		public void setApplySynonyms(boolean applySynonyms) {
			this.applySynonyms = applySynonyms;
		}
		public Boolean isConformFinalResultsToQuery() {
			return conformFinalResultsToQuery;
		}
		public void setConfirmFinalResultsToQuery(Boolean conformFinalResultsToQuery) {
			this.conformFinalResultsToQuery = conformFinalResultsToQuery;
		}
		public Boolean getEnableHL1() {
			return enableHL1;
		}
		public void setEnableHL1(Boolean enableHL1) {
			this.enableHL1 = enableHL1;
		}
		public Boolean getEnableHL2() {
			return enableHL2;
		}
		public void setEnableHL2(Boolean enableHL2) {
			this.enableHL2 = enableHL2;
		}
		public Boolean getEnableRL() {
			return enableRL;
		}
		public void setEnableRL(Boolean enableRL) {
			this.enableRL = enableRL;
		}
		public Boolean getConformFinalResultsToQuery() {
			return conformFinalResultsToQuery;
		}
		public void setConformFinalResultsToQuery(Boolean conformFinalResultsToQuery) {
			this.conformFinalResultsToQuery = conformFinalResultsToQuery;
		}
		public SearchConfig getSearchConfig() {
			return searchConfig;
		}
		public void setSearchConfig(SearchConfig searchConfig) {
			this.searchConfig = searchConfig;
		}
		public SearchFilterConfig getSearchFilterConfig() {
			return searchFilterConfig;
		}
		public void setSearchFilterConfig(SearchFilterConfig searchFilterConfig) {
			this.searchFilterConfig = searchFilterConfig;
		}
	}
	
//	@JsonIgnoreProperties(ignoreUnknown = true)
//	public static class SearchUIConfig extends SearchCommonConfig {
//		
//	}
//	
//	@JsonIgnoreProperties(ignoreUnknown = true)
//	public static class SearchApiConfig extends SearchCommonConfig {
//		
//	}
	
	
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SolrConfig {
		
		@JsonProperty("default-contract-type")
		private String defaultContractType;
		
		@JsonProperty("default-core-config")
		private SolrCoreConfig defaultCoreConfig;
		
		@JsonProperty("available-contract-types")
		private List<String> availableContractTypes;
		
		@JsonProperty("contract-type-2-core-config")
		private Map<String, SolrCoreConfig> contractTypeToCoreConfig;
				
		@JsonProperty("all-cores-in-use")
		private List<CoreInUseConfig> allCoresInUse;
	
		@JsonProperty("client-core-config")
		private SolrCoreConfig clientCoreConfig;
		
		//For term library
		@JsonProperty("term-library-config")
		private SolrCoreConfig libConfig;
		@JsonProperty("admin-config")
		private SolrCoreConfig adminConfig;
		
		public SolrCoreConfig getLibConfig() {
			return libConfig;
		}
		public void setLibConfig(SolrCoreConfig libConfig) {
			this.libConfig = libConfig;
		}
		
		public SolrCoreConfig getAdminConfig() {
			return adminConfig;
		}
		public void setAdminConfig(SolrCoreConfig adminConfig) {
			this.adminConfig = adminConfig;
		}
		
		public SolrCoreConfig getCoreConfig(String coreName) {
			
			SolrCoreConfig scc = contractType2CoreMap.get(coreName);
//			SolrCoreConfig scc = contractTypeToCoreConfig.get(coreName);
			if (null != scc) {
				if (StringUtils.isBlank(scc.getBaseServerUrl()))
					scc.setBaseServerUrl(defaultCoreConfig.getBaseServerUrl());
				return scc;
			}
			return defaultCoreConfig;
		}
		
		public List<String> getAvailableContractTypes() {
			return availableContractTypes;
		}
		public String getDefaultContractType() {
			return defaultContractType;
		}

		public void setDefaultContractType(String defaultContractType) {
			this.defaultContractType = defaultContractType;
		}

		public void setAvailableContractTypes(List<String> availableContractTypes) {
			this.availableContractTypes = availableContractTypes;
		}

		public Map<String, SolrCoreConfig> getContractTypeToCoreConfig() {
			return contractTypeToCoreConfig;
		}

		public void setContractTypeToCoreConfig(Map<String, SolrCoreConfig> contractTypeToCoreConfig) {
			this.contractTypeToCoreConfig = contractTypeToCoreConfig;
		}

		public SolrCoreConfig getDefaultCoreConfig() {
			return defaultCoreConfig;
		}

		public SolrCoreConfig getClientCoreConfig() {
			return clientCoreConfig;
		}
		
		public List<CoreInUseConfig> getAllCoresInUse() {
			return allCoresInUse;
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class SolrCoreConfig {
			@JsonProperty("base-server-url")
			private String baseServerUrl;
			@JsonProperty("core-name")
			private String coreName;
			
			public String getBaseServerUrl() {
				return baseServerUrl;
			}
			public void setBaseServerUrl(String baseServerUrl) {
				this.baseServerUrl = baseServerUrl;
			}
			public String getCoreName() {
				return coreName;
			}
			public void setCoreName(String coreName) {
				this.coreName = coreName;
			}			
		}
		
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class CoreInUseConfig {
			@JsonProperty("base-server-url")
			private String baseServerUrl;
			@JsonProperty("core-names")
			private List<String> coreNames;
			
			public String getBaseServerUrl() {
				return baseServerUrl;
			}
			public void setBaseServerUrl(String baseServerUrl) {
				this.baseServerUrl = baseServerUrl;
			}
			public List<String> getCoreNames() {
				return coreNames;
			}
			public void setCoreNames(List<String> coreNames) {
				this.coreNames = coreNames;
			}
			
		}
		
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SimilarityConfig {
		@JsonProperty("apply-similarity")
		private Boolean applySimilarity = false;
		@JsonProperty("min-similarity-score")
		private Integer minSimilarityScore = null;
		
		public Boolean getApplySimilarity() {
			return applySimilarity;
		}
		public void setApplySimilarity(Boolean applySimilarity) {
			this.applySimilarity = applySimilarity;
		}
		public Integer getMinSimilarityScore() {
			return minSimilarityScore;
		}
		public void setMinSimilarityScore(Integer minSimilarityScore) {
			this.minSimilarityScore = minSimilarityScore;
		}
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class MLConfig {
		@JsonProperty("base-server-url")
		private String baseServerUrl;
		
		@JsonProperty("USE-similarity-endpoint")
		private String USESimilarityEndpoint;
		@JsonProperty("find-synonym-endPoint")
		private String findSynonymEndPoint;
		
		public String getBaseServerUrl() {
			return baseServerUrl;
		}
		public void setBaseServerUrl(String baseServerUrl) {
			this.baseServerUrl = baseServerUrl;
		}
		public String getUSESimilarityEndpoint() {
			return USESimilarityEndpoint;
		}
		public void setUSESimilarityEndpoint(String uSESimilarityEndpoint) {
			USESimilarityEndpoint = uSESimilarityEndpoint;
		}
		public String getFindSynonymEndPoint() {
			return findSynonymEndPoint;
		}
		public void setFindSynonymEndPoint(String findSynonymEndPoint) {
			this.findSynonymEndPoint = findSynonymEndPoint;
		}
	}
	

	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class DefaultFQs {
		@JsonProperty("within-last-years")
		private Integer withinLastYears;

		public Integer getWithinLastYears() {
			return withinLastYears;
		}

		public void setWithinLastYears(Integer withinLastYears) {
			this.withinLastYears = withinLastYears;
		}
	}
	
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SearchConfig {
		@JsonProperty("search-against-field")
		private String searchAgainstField = "txt";
		@JsonProperty("homogenize-query-text")
		private boolean homogenizeQueryText = false;
		@JsonProperty("wildcard-query-words")
		private Boolean wildcardQueryWords;
		
		@JsonProperty("word-count-field-name")
		private String wordCountFieldName;		//ie "wCnt"
		@JsonProperty("min-Q-hTxt-count-to-apply-word-count-range-filter")
		private Integer minQHtxtCountToApplyCountRangeFilter;		// ie 7
		@JsonProperty("wordCnt-filter-range")
		private Float[] wordCntFilterRange;		//ie [0.5, 2.5]
		
		@JsonProperty("broadening-config")
		private BroadeningConfig broadeningConfig;		//ie [0.5, 3.5]
					
		
		
		@JsonProperty("mm-value-for-broadining")
		private String mmValueForBroadining;	
		
		@JsonProperty("default-FQs")
		private DefaultFQs defaultFQs;
		@JsonProperty("fields-to-fetch")
		private List<String> fieldsToFetch;
		@JsonProperty("raw-query-params")
		private Map<String, Object> rawQueryParams;
		@JsonProperty("ready-fqs")
		private List<String> readyFqs;

		@JsonProperty("min-wcnt-for-clause-search")
		private Integer minWcntForClauseSearch;
		@JsonProperty("doc0-search_raw-query-params")
		private Map<String, Object> doc0RawQueryParams;
		@JsonProperty("doc0-search_default-FQs")
		private List<String> doc0DefaultFQs;
		
		
		
		public BroadeningConfig getBroadeningConfig() {
			return broadeningConfig;
		}
		public void setBroadeningConfig(BroadeningConfig broadeningConfig) {
			this.broadeningConfig = broadeningConfig;
		}
		public String getMmValueForBroadining() {
			return mmValueForBroadining;
		}
		public void setMmValueForBroadining(String mmValueForBroadining) {
			this.mmValueForBroadining = mmValueForBroadining;
		}
		public String getSearchAgainstField() {
			return searchAgainstField;
		}
		public void setSearchAgainstField(String searchAgainstField) {
			this.searchAgainstField = searchAgainstField;
		}
		public boolean isHomogenizeQueryText() {
			return homogenizeQueryText;
		}
		public void setHomogenizeQueryText(boolean homogenizeQueryText) {
			this.homogenizeQueryText = homogenizeQueryText;
		}
		public Boolean getWildcardQueryWords() {
			return wildcardQueryWords;
		}
		public void setWildcardQueryWords(Boolean wildcardQueryWords) {
			this.wildcardQueryWords = wildcardQueryWords;
		}
		public String getWordCountFieldName() {
			return wordCountFieldName;
		}
		public void setWordCountFieldName(String wordCountFieldName) {
			this.wordCountFieldName = wordCountFieldName;
		}
		public Integer getMinQHtxtCountToApplyCountRangeFilter() {
			return minQHtxtCountToApplyCountRangeFilter;
		}
		public void setMinQHtxtCountToApplyCountRangeFilter(Integer minQHtxtCountToApplyCountRangeFilter) {
			this.minQHtxtCountToApplyCountRangeFilter = minQHtxtCountToApplyCountRangeFilter;
		}
		public Float[] getWordCntFilterRange() {
			return wordCntFilterRange;
		}
		public void setWordCntFilterRange(Float[] wordCntFilterRange) {
			this.wordCntFilterRange = wordCntFilterRange;
		}
		public DefaultFQs getDefaultFQs() {
			return defaultFQs;
		}
		public void setDefaultFQs(DefaultFQs defaultFQs) {
			this.defaultFQs = defaultFQs;
		}
		public List<String> getFieldsToFetch() {
			return fieldsToFetch;
		}
		public void setFieldsToFetch(List<String> fieldsToFetch) {
			this.fieldsToFetch = fieldsToFetch;
		}
		public Map<String, Object> getRawQueryParams() {
			return rawQueryParams;
		}
		public void setRawQueryParams(Map<String, Object> rawQueryParams) {
			this.rawQueryParams = rawQueryParams;
		}
		public List<String> getReadyFqs() {
			return readyFqs;
		}
		public void setReadyFqs(List<String> readyFqs) {
			this.readyFqs = readyFqs;
		}
		public Integer getMinWcntForClauseSearch() {
			return minWcntForClauseSearch;
		}
		public void setMinWcntForClauseSearch(Integer minWcntForClauseSearch) {
			this.minWcntForClauseSearch = minWcntForClauseSearch;
		}
		public Map<String, Object> getDoc0RawQueryParams() {
			return doc0RawQueryParams;
		}
		public void setDoc0RawQueryParams(Map<String, Object> doc0RawQueryParams) {
			this.doc0RawQueryParams = doc0RawQueryParams;
		}
		public List<String> getDoc0DefaultFQs() {
			return doc0DefaultFQs;
		}
		public void setDoc0DefaultFQs(List<String> doc0DefaultFQs) {
			this.doc0DefaultFQs = doc0DefaultFQs;
		}
	}
	
	
	public static class BroadeningConfig {
		@JsonProperty("wordCnt-filter-range")
		private Float[] wordCntFilterRange;		//ie [0.5, 3.5]
		
		@JsonProperty("mm")
		private String mm;

		public Float[] getWordCntFilterRange() {
			return wordCntFilterRange;
		}

		public void setWordCntFilterRange(Float[] wordCntFilterRange) {
			this.wordCntFilterRange = wordCntFilterRange;
		}

		public String getMm() {
			return mm;
		}

		public void setMm(String mm) {
			this.mm = mm;
		}

				
				
	}
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SearchFilterConfig {
		@JsonProperty("similarity-config")
		private SimilarityConfig similarityConfig;
		
		@JsonProperty("cut-off-percent-score-againt-top-solr-score")
		private int cutOffPercentScoreAgaintTopSolrScore = 10;
		
		@JsonProperty("discard-groups-having-doc-counts-percent-below")
		private float discardGroupsHavingDocCountsPercentBelow;
		
		@JsonProperty("factor-2-pick-results-b4-applying-sim")
		private Float factorToPickResultsB4ApplyingSim;
		@JsonProperty("top-txtcnt-results-always-picked-regardless-last-minute-filter")
		private Integer topTxtcntResultsAlwaysPickedRegardlessLastMinuteFilter;
		
		@JsonProperty("final-sort-by")
		private String finalSortBy;
		@JsonProperty("final-results-count")
		private int finalResultsCount;		
		
		
		public int getFinalResultsCount() {
			return finalResultsCount;
		}
		public void setFinalResultsCount(int finalResultsCount) {
			this.finalResultsCount = finalResultsCount;
		}
		public Float getFactorToPickResultsB4ApplyingSim() {
			return factorToPickResultsB4ApplyingSim;
		}
		public void setFactorToPickResultsB4ApplyingSim(Float factorToPickResultsB4ApplyingSim) {
			this.factorToPickResultsB4ApplyingSim = factorToPickResultsB4ApplyingSim;
		}
		public String getFinalSortBy() {
			return finalSortBy;
		}
		public void setFinalSortBy(String finalSortBy) {
			this.finalSortBy = finalSortBy;
		}
		public Integer getTopTxtcntResultsAlwaysPickedRegardlessLastMinuteFilter() {
			return topTxtcntResultsAlwaysPickedRegardlessLastMinuteFilter;
		}
		public void setTopTxtcntResultsAlwaysPickedRegardlessLastMinuteFilter(
				Integer topTxtcntResultsAlwaysPickedRegardlessLastMinuteFilter) {
			this.topTxtcntResultsAlwaysPickedRegardlessLastMinuteFilter = topTxtcntResultsAlwaysPickedRegardlessLastMinuteFilter;
		}
		public int getCutOffPercentScoreAgaintTopSolrScore() {
			return cutOffPercentScoreAgaintTopSolrScore;
		}
		public void setCutOffPercentScoreAgaintTopSolrScore(int cutOffPercentScoreAgaintTopSolrScore) {
			this.cutOffPercentScoreAgaintTopSolrScore = cutOffPercentScoreAgaintTopSolrScore;
		}
		public float getDiscardGroupsHavingDocCountsPercentBelow() {
			return discardGroupsHavingDocCountsPercentBelow;
		}
		public void setDiscardGroupsHavingDocCountsPercentBelow(float discardGroupsHavingDocCountsPercentBelow) {
			this.discardGroupsHavingDocCountsPercentBelow = discardGroupsHavingDocCountsPercentBelow;
		}
		public SimilarityConfig getSimilarityConfig() {
			return similarityConfig;
		}
		public void setSimilarityConfig(SimilarityConfig similarityConfig) {
			this.similarityConfig = similarityConfig;
		}
		
	}

	public static class QueryBuilderConfig {
		@JsonProperty("template-folder")
		private String templateFolder;
		@JsonProperty("reports-folder")
		private String reportsFolder;
		@JsonProperty("term-library-folder")
		private String termLibraryFolder;
		@JsonProperty("legal-pack-query-folder")
		private String legalPackQueryFolder;
		@JsonProperty("htxt-synonym-file")
		private String htxtSynonymFile;
		
		@JsonProperty("raw-solr-query-params")
		private Map<String, String> rawSolrQueryParams;
		
		@JsonProperty("doc0-search-raw-params")
		private Map<String, String> doc0SearchRawParams;
		
		@JsonProperty("default-values")
		private DefaultValues defaultValues;
		
		@JsonProperty("similarity-range-settings")
		Map<String, SimilarityRangeSetting> similarityRangeSettings;
		
		public String getTemplateFolder() {
			return templateFolder;
		}
		public void setTemplateFolder(String templateFolder) {
			this.templateFolder = templateFolder;
		}
		public String getReportsFolder() {
			return reportsFolder;
		}
		public void setReportsFolder(String reportsFolder) {
			this.reportsFolder = reportsFolder;
		}
		public String getTermLibraryFolder() {
			return termLibraryFolder;
		}
		public void setTermLibraryFolder(String termLibraryFolder) {
			this.termLibraryFolder = termLibraryFolder;
		}
		public String getLegalPackQueryFolder() {
			return legalPackQueryFolder;
		}
		public void setLegalPackQueryFolder(String legalPackQueryFolder) {
			this.legalPackQueryFolder = legalPackQueryFolder;
		}
		public String getHtxtSynonymFile() {
			return htxtSynonymFile;
		}
		public void setHtxtSynonymFile(String htxtSynonymFile) {
			this.htxtSynonymFile = htxtSynonymFile;
		}
		public Map<String, String> getRawSolrQueryParams() {
			return rawSolrQueryParams;
		}
		public void setRawSolrQueryParams(Map<String, String> rawSolrQueryParams) {
			this.rawSolrQueryParams = rawSolrQueryParams;
		}
		public DefaultValues getDefaultValues() {
			return defaultValues;
		}
		public void setDefaultValues(DefaultValues defaultValues) {
			this.defaultValues = defaultValues;
		}
		
		public Map<String, SimilarityRangeSetting> getSimilarityRangeSettings() {
			return similarityRangeSettings;
		}
		public void setSimilarityRangeSettings(Map<String, SimilarityRangeSetting> similarityRangeSettings) {
			this.similarityRangeSettings = similarityRangeSettings;
		}
		public Map<String, String> getDoc0SearchRawParams() {
			return doc0SearchRawParams;
		}
		public void setDoc0SearchRawParams(Map<String, String> doc0SearchRawParams) {
			this.doc0SearchRawParams = doc0SearchRawParams;
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class DefaultValues {
			@JsonProperty("core")
			private List<String> core;
			
			@JsonProperty("contractType")
			private List<String> contractType;
			
			@JsonProperty("wCntRangeTimes")
			private List<Float> wCntRangeTimes;
			@JsonProperty("wCntField")
			private String wCntField;
			
			@JsonProperty("txtType")
			private List<Integer> txtType;

			public List<String> getCore() {
				return core;
			}

			public void setCore(List<String> core) {
				this.core = core;
			}

			public List<String> getContractType() {
				return contractType;
			}

			public void setContractType(List<String> contractType) {
				this.contractType = contractType;
			}

			public String getwCntField() {
				return wCntField;
			}

			public void setwCntField(String wCntField) {
				this.wCntField = wCntField;
			}

			public List<Float> getwCntRangeTimes() {
				return wCntRangeTimes;
			}

			public void setwCntRangeTimes(List<Float> wCntRangeTimes) {
				this.wCntRangeTimes = wCntRangeTimes;
			}

			public List<Integer> getTxtType() {
				return txtType;
			}

			public void setTxtType(List<Integer> txtType) {
				this.txtType = txtType;
			}
		}
		
		
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class SimilarityRangeSetting {
			@JsonProperty("wCntRangeTimes")
			private List<Float> wCntRangeTimes;		// [0.5, 2]
			
			@JsonProperty("mm")
			private String mm;
			
			public List<Float> getwCntRangeTimes() {
				return wCntRangeTimes;
			}
			public void setwCntRangeTimes(List<Float> wCntRangeTimes) {
				this.wCntRangeTimes = wCntRangeTimes;
			}
			public String getMm() {
				return mm;
			}
			public void setMm(String mm) {
				this.mm = mm;
			}
		}
	}
	
	
}
