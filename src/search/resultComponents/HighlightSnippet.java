package search.resultComponents;

import org.apache.commons.lang.StringUtils;

public class HighlightSnippet {

	private String snippet;
	private float hlScore = -1;

	public HighlightSnippet(String snippetWithHLScore) {
		setSnippet(snippetWithHLScore);
	}
	
	public String getSnippet() {
		return snippet;
	}
	public void setSnippet(String snippet) {
		if (StringUtils.isNotBlank(snippet)) {
			// remove [[11.481040954589844]]{{Infinity}}
			snippet = snippet.replaceAll("\\[\\[[\\d\\.]+?\\]\\]", "").replaceAll("\\{\\{Infinity.*?\\}\\}", "");
			String hlScoreStr = StringUtils.substringBetween(snippet, "[", "]");
			snippet = snippet.replaceAll("[\\{\\[\\d\\.\\]\\}]+?", "");
			if (StringUtils.isNotBlank(hlScoreStr)) {
				if (hlScoreStr.trim().matches("^[\\d\\.]+?$"))
					hlScore = Float.parseFloat(hlScoreStr.trim());
			}
		}
		this.snippet = snippet;
	}

	public float getHlScore() {
		return hlScore;
	}
	public void setHlScore(float hlScore) {
		this.hlScore = hlScore;
	}

	

}
