package algo_testers.search_dependencies;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

public class SynonymApiRequest extends BaseApiRequest {

	@JsonProperty("text_list")
	private Object textList;
	
	@JsonProperty("pattern")
	private List<Map<String, String>> pattern;
	
	@JsonProperty("test_similarity")
	private Boolean testSimilarity;
	
	@JsonProperty("min_sentence_similarity_score")
	private Integer minSentenceSimilarityScore;
	
	@JsonProperty("use_stemming")
	private Boolean useStemming;
	
	@JsonProperty("min_synonym_similarity_score")
	private Integer minSynonymSimilarityScore;
	
	@JsonProperty("synonym_scoring_method_spacy")
	private Boolean synonymScoringMethodSpacy;
	
	@JsonProperty("pos_method_spacy")
	private Boolean posMethodSpacy;
	
	
	public Object getTextList() {
		return textList;
	}

	public void setTextList(Object textList) {
		this.textList = textList;
	}

	public List<Map<String, String>> getPattern() {
		return pattern;
	}

	public void setPattern(List<Map<String, String>> pattern) {
		this.pattern = pattern;
	}

	public Boolean getTestSimilarity() {
		return testSimilarity;
	}

	public void setTestSimilarity(Boolean testSimilarity) {
		this.testSimilarity = testSimilarity;
	}

	public Integer getMinSentenceSimilarityScore() {
		return minSentenceSimilarityScore;
	}

	public void setMinSentenceSimilarityScore(Integer minSentenceSimilarityScore) {
		this.minSentenceSimilarityScore = minSentenceSimilarityScore;
	}

	public Boolean getUseStemming() {
		return useStemming;
	}

	public void setUseStemming(Boolean useStemming) {
		this.useStemming = useStemming;
	}

	public Integer getMinSynonymSimilarityScore() {
		return minSynonymSimilarityScore;
	}

	public void setMinSynonymSimilarityScore(Integer minSynonymSimilarityScore) {
		this.minSynonymSimilarityScore = minSynonymSimilarityScore;
	}

	public Boolean getSynonymScoringMethodSpacy() {
		return synonymScoringMethodSpacy;
	}

	public void setSynonymScoringMethodSpacy(Boolean synonymScoringMethodSpacy) {
		this.synonymScoringMethodSpacy = synonymScoringMethodSpacy;
	}

	public Boolean getPosMethodSpacy() {
		return posMethodSpacy;
	}

	public void setPosMethodSpacy(Boolean posMethodSpacy) {
		this.posMethodSpacy = posMethodSpacy;
	}
	
	//********************************************************//
	
	public static void main(String[] arg) throws JsonProcessingException {
		System.out.println(JSonUtils.object2JsonString(new SynonymApiRequest()));
	}
	
	
}
