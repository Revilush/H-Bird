package algo_testers.search_dependencies;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * 
 * 
 * 
{	"input_map":{"list_1":{"id1":"sent1", ...},"list_2":{"id1":"sent1", ...}}
	,"meta":{"max-score":94.002,"total-matches":10}
	,"match_results":[
		{"id":"1","max_score":52.646,"matches":[{"id":"10","score":52.646}]},
		{"id":"2","max_score":94.002,"matches":[{"id":"21","score":94.002},{"id":"20","score":54.876}]}
	],
	"non_match_ids":[6","8"]
}
 *
 */
public class USESimilarityResponse extends BaseApiResponse {

	@JsonProperty("input_map")
	private ApiInput input;
	
	@JsonProperty("meta")
	private SimilarityResultMetadata meta;
	
	@JsonProperty("match_results")
	private List<MatchResult> matchResults;
	
//	@JsonProperty("non_match_results")
//	private List<MatchResult> nonMatchResults;
	
	@JsonProperty("non_match_ids")
	private List<String> nonMatchIds;
	
	
	
	public static class ApiInput {
		@JsonProperty("list_1")
		private Map<String, String> list1;
		@JsonProperty("list_2")
		private Map<String, String> list2;
		
		public Map<String, String> getList1() {
			return list1;
		}
		public Map<String, String> getList2() {
			return list2;
		}
		
	}
		
	
	public static class SimilarityResultMetadata {
		@JsonProperty("max_score")
		private float maxScore;
		@JsonProperty("total_matches")
		private int totalMatches;
		
		public float getMaxScore() {
			return maxScore;
		}
		public int getTotalMatches() {
			return totalMatches;
		}
	}
	
	public static class MatchResult {
		@JsonProperty("id")
		private String id;
		@JsonProperty("max_score")
		private float maxScore;
		@JsonProperty("matches")
		private List<Match> matches;
		
		public String getId() {
			return id;
		}
		public float getMaxScore() {
			return maxScore;
		}
		public List<Match> getMatches() {
			return matches;
		}
	}

	public static class Match {
		@JsonProperty("id")
		private String id;
		@JsonProperty("score")
		private float score;
		@JsonProperty("same_score_matches")
		private List<String> sameScoreMatches;
		
		@JsonIgnore
		private String matchText;

		public String getId() {
			return id;
		}
		public float getScore() {
			return score;
		}
		public List<String> getSameScoreMatches() {
			return sameScoreMatches;
		}
		public void setSameScoreMatches(List<String> list) {
			sameScoreMatches = list;
		}
		public String getMatchText() {
			return matchText;
		}
		public void setMatchText(String matchText) {
			this.matchText = matchText;
		}
	}

	
	public ApiInput getInput() {
		return input;
	}

	public SimilarityResultMetadata getMeta() {
		return meta;
	}

	public List<MatchResult> getMatchResults() {
		return matchResults;
	}
	
	public List<String> getNonMatchIds() {
		return nonMatchIds;
	}

//	public List<MatchResult> getNonMatchResults() {
//		return nonMatchResults;
//	}


	// ********************************************************* //
	

	public static void main(String[] arg) throws JsonProcessingException {
		System.out.println(JSonUtils.object2JsonString(new USESimilarityResponse()));
	}
}
