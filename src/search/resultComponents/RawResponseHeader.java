package search.resultComponents;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class RawResponseHeader {

	@JsonProperty("status")
	public int status;
	
	@JsonProperty("QTime")
	public int QTime;
	
	@JsonProperty("params")
	public Map<String, Object> params;
	
	
}
