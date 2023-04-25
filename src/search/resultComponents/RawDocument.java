package search.resultComponents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class RawDocument {

	@JsonProperty("id")
	public String id;
	
	@JsonProperty("contractType")
	public String contractType;

	@JsonProperty("filer")
	public String filer;
	
	@JsonProperty("contractLongName")
	public String contractLongName;
	
	@JsonProperty("definitionHeading")
	public List<String> definitionHeading;
	
	@JsonProperty("sectionHeading")
	public List<String> sectionHeading;
	
	@JsonProperty("exhibitHeading")
	public List<String> exhibitHeading;

	@JsonProperty("text")
	public String text;
	
	@JsonProperty("sentence")
	public List<String> sentences;

	@JsonProperty("fileDate")
	public String fileDate;

	@JsonProperty("score")
	public float score;
	
	@JsonProperty("wordCount")
	public Integer wordCount;
	
	@JsonIgnore
	public DocHighlightingResult highlightingSnippets;
	
	@JsonIgnore
	public int discardedDocsCount = 0;
	
	
	@JsonIgnore
	public Map<String, Object> additionalProperties = new HashMap<String, Object>();
	
	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}
	@JsonAnySetter
	public void setAdditionalProperty(String name, Object value) {
		this.additionalProperties.put(name, value);
	}
	public RawDocument withAdditionalProperty(String name, Object value) {
		this.additionalProperties.put(name, value);
		return this;
	}
}
