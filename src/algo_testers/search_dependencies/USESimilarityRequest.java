package algo_testers.search_dependencies;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

public class USESimilarityRequest extends BaseApiRequest {

	@JsonProperty("list_1")
	private Object list1;
	@JsonProperty("list_2")
	private Object list2;
	
	@JsonProperty("min_score")
	private Integer minScore;
	
	@JsonProperty("max_score")
	private Integer maxScore;
	
	@JsonProperty("max_count")
	private Integer maxCount;

	@JsonProperty("find_best_matches")
	private Boolean findBestMatches;
	
	@JsonProperty("keep_not_matched")
	private Boolean keepNotMatched;

	
	public Object getList1() {
		return list1;
	}

	public void setList1(Object list1) {
		this.list1 = list1;
	}

	public Object getList2() {
		return list2;
	}

	public void setList2(Object list2) {
		this.list2 = list2;
	}

	public Integer getMinScore() {
		return minScore;
	}

	public void setMinScore(Integer minScore) {
		this.minScore = minScore;
	}

	public Integer getMaxScore() {
		return maxScore;
	}

	public void setMaxScore(Integer maxScore) {
		this.maxScore = maxScore;
	}

	public Integer getMaxCount() {
		return maxCount;
	}

	public void setMaxCount(Integer maxCount) {
		this.maxCount = maxCount;
	}

	public Boolean getFindBestMatches() {
		return findBestMatches;
	}

	public void setFindBestMatches(Boolean findBestMatches) {
		this.findBestMatches = findBestMatches;
	}

	public Boolean getKeepNotMatched() {
		return keepNotMatched;
	}

	public void setKeepNotMatched(Boolean keepNotMatched) {
		this.keepNotMatched = keepNotMatched;
	}

	// ********************************************************* //
	
	
	public static void main(String[] arg) throws JsonProcessingException {
		System.out.println(JSonUtils.object2JsonString(new USESimilarityRequest()));
	}
	
}
