package algo_testers.search_dependencies;

import java.util.LinkedHashMap;
import java.util.Map;

public class SpeedTestReport {
	private int solrSpeed;
	private int javaSpeed;
	private String wcnt;
	private String yearRange;
	private String mm;
	private String queryString;
	private String contractType;
	private int beforeGroupHtxtCnt;
	private int afterGroupHtxtCnt;
	private int simResult;
	private int totalTxtCnt;
	private String solrQuerLink;
	private SearchResponse response;
	private int totalTime;
	private int idPoolSize;
	private int idPoolQueyTime;
	private int doc0QueyTime;
	private int edgarLinkQueryTime;
	private int collapseExpandTime;
	private int simTime;
	private int synonymTime;
	private int resultFromParaTime;
	private int addMetaTime;
	private String reportCount;
	private long docFromSolr;
	private boolean similarityApiApplied = false;
	private int numFound;
	private boolean shrinkQuery = false;
	private String shrinkDaysFQ ;
	private int yearCount;
	
	
	public int getYearCount() {
		return yearCount;
	}
	public void setYearCount(int yearCount) {
		this.yearCount = yearCount;
	}
	public String getShrinkDaysFQ() {
		return shrinkDaysFQ;
	}
	public void setShrinkDaysFQ(String shrinkDaysFQ) {
		this.shrinkDaysFQ = shrinkDaysFQ;
	}
	public boolean isShrinkQuery() {
		return shrinkQuery;
	}
	public void setShrinkQuery(boolean shrinkQuery) {
		this.shrinkQuery = shrinkQuery;
	}
	public int getNumFound() {
		return numFound;
	}
	public void setNumFound(int numFound) {
		this.numFound = numFound;
	}
	public boolean isSimilarityApiApplied() {
		return similarityApiApplied;
	}
	public void setSimilarityApiApplied(boolean similarityApiApplied) {
		this.similarityApiApplied = similarityApiApplied;
	}
	public long getDocFromSolr() {
		return docFromSolr;
	}
	public void setDocFromSolr(long docFromSolr) {
		this.docFromSolr = docFromSolr;
	}
	
	private Map<String, Object> otherProperties = new LinkedHashMap<>();
	
	
	public void addOtherProperty(String key, Object value) {
		otherProperties.put(key, value);
	}
	public Map<String, Object> getOtherProperties() {
		return otherProperties;
	}
	public void setOtherProperties(Map<String, Object> otherProperties) {
		this.otherProperties = otherProperties;
	}


	public String getReportCount() {
		return reportCount;
	}

	public void setReportCount(String reportCount) {
		this.reportCount = reportCount;
	}
	
	public int getIdPoolQueyTime() {
		return idPoolQueyTime;
	}

	public void setIdPoolQueyTime(int idPoolQueyTime) {
		this.idPoolQueyTime = idPoolQueyTime;
	}

	public int getDoc0QueyTime() {
		return doc0QueyTime;
	}

	public void setDoc0QueyTime(int doc0QueyTime) {
		this.doc0QueyTime = doc0QueyTime;
	}

	public int getEdgarLinkQueryTime() {
		return edgarLinkQueryTime;
	}

	public void setEdgarLinkQueryTime(int edgarLinkQueryTime) {
		this.edgarLinkQueryTime = edgarLinkQueryTime;
	}

	public int getCollapseExpandTime() {
		return collapseExpandTime;
	}

	public void setCollapseExpandTime(int collapseExpandTime) {
		this.collapseExpandTime = collapseExpandTime;
	}

	public int getSimTime() {
		return simTime;
	}

	public void setSimTime(int simTime) {
		this.simTime = simTime;
	}

	public int getSynonymTime() {
		return synonymTime;
	}

	public void setSynonymTime(int synonymTime) {
		this.synonymTime = synonymTime;
	}

	public int getResultFromParaTime() {
		return resultFromParaTime;
	}

	public void setResultFromParaTime(int resultFromParaTime) {
		this.resultFromParaTime = resultFromParaTime;
	}

	public int getAddMetaTime() {
		return addMetaTime;
	}

	public void setAddMetaTime(int addMetaTime) {
		this.addMetaTime = addMetaTime;
	}

	public int getIdPoolSize() {
		return idPoolSize;
	}

	public void setIdPoolSize(int idPoolSize) {
		this.idPoolSize = idPoolSize;
	}

	public int getSolrSpeed() {
		return solrSpeed;
	}

	public void setSolrSpeed(int solrSpeed) {
		this.solrSpeed = solrSpeed;
	}

	public int getJavaSpeed() {
		return javaSpeed;
	}

	public void setJavaSpeed(int javaSpeed) {
		this.javaSpeed = javaSpeed;
	}

	public String getWcnt() {
		return wcnt;
	}

	public void setWcnt(String wcnt) {
		this.wcnt = wcnt;
	}

	public String getYearRange() {
		return yearRange;
	}

	public void setYearRange(String yearRange) {
		this.yearRange = yearRange;
	}

	public String getMm() {
		return mm;
	}

	public void setMm(String mm) {
		this.mm = mm;
	}

	public String getQueryString() {
		return queryString;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public String getContractType() {
		return contractType;
	}

	public void setContractType(String contractType) {
		this.contractType = contractType;
	}

	public int getBeforeGroupHtxtCnt() {
		return beforeGroupHtxtCnt;
	}

	public void setBeforeGroupHtxtCnt(int beforeGroupHtxtCnt) {
		this.beforeGroupHtxtCnt = beforeGroupHtxtCnt;
	}

	public int getAfterGroupHtxtCnt() {
		return afterGroupHtxtCnt;
	}

	public void setAfterGroupHtxtCnt(int afterGroupHtxtCnt) {
		this.afterGroupHtxtCnt = afterGroupHtxtCnt;
	}

	public int getSimResult() {
		return simResult;
	}

	public void setSimResult(int simResult) {
		this.simResult = simResult;
	}

	public int getTotalTxtCnt() {
		return totalTxtCnt;
	}

	public void setTotalTxtCnt(int totalTxtCnt) {
		this.totalTxtCnt = totalTxtCnt;
	}

	public String getSolrQuerLink() {
		return solrQuerLink;
	}

	public void setSolrQuerLink(String solrQuerLink) {
		this.solrQuerLink = solrQuerLink;
	}

	public SearchResponse getResponse() {
		return response;
	}

	public void setResponse(SearchResponse response) {
		this.response = response;
	}

	public int getTotalTime() {
		return totalTime;
	}

	public void setTotalTime(int totalTime) {
		this.totalTime = totalTime;
	}
    
}
