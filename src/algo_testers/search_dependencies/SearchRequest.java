package algo_testers.search_dependencies;

public class SearchRequest {

	private String responseDetail;
	private String requestDetail;
	private Long searchTime;
	private Long analysisTime;
	private Long totalResponseTime;
	private Long errorId;
	private String targetApi;
	private String actionSource;
	private String entryType;
	private String activityDetail;
	private long timeStamp;
	private String userAgent;

	public String getResponseDetail() {
		return responseDetail;
	}

	public void setResponseDetail(String responseDetail) {
		this.responseDetail = responseDetail;
	}

	public String getRequestDetail() {
		return requestDetail;
	}

	public void setRequestDetail(String requestDetail) {
		this.requestDetail = requestDetail;
	}

	public Long getSearchTime() {
		return searchTime;
	}

	public void setSearchTime(Long searchTime) {
		this.searchTime = searchTime;
	}

	public Long getAnalysisTime() {
		return analysisTime;
	}

	public void setAnalysisTime(Long analysisTime) {
		this.analysisTime = analysisTime;
	}

	public Long getTotalResponseTime() {
		return totalResponseTime;
	}

	public void setTotalResponseTime(Long totalResponseTime) {
		this.totalResponseTime = totalResponseTime;
	}

	public Long getErrorId() {
		return errorId;
	}

	public void setErrorId(Long errorId) {
		this.errorId = errorId;
	}

	public String getTargetApi() {
		return targetApi;
	}

	public void setTargetApi(String targetApi) {
		this.targetApi = targetApi;
	}

	public String getActionSource() {
		return actionSource;
	}

	public void setActionSource(String actionSource) {
		this.actionSource = actionSource;
	}

	public String getEntryType() {
		return entryType;
	}

	public void setEntryType(String entryType) {
		this.entryType = entryType;
	}

	public String getActivityDetail() {
		return activityDetail;
	}

	public void setActivityDetail(String activityDetail) {
		this.activityDetail = activityDetail;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
}
