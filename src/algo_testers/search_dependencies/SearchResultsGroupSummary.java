package algo_testers.search_dependencies;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.common.SolrDocument;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

import algo_testers.dependencies.SynonymApplier;
import algo_testers.search_dependencies.SolrDocumentFieldComparator.SolrDocumentIntFieldComparator;

@JsonInclude(Include.NON_NULL)
public class SearchResultsGroupSummary {
	protected static Log log = LogFactory.getLog(SearchResultsGroupSummary.class);
	
	public static final String SolrFieldName_HomogText = "hTxt";
	public static final String SolrFieldName_Text = "txt";

	@JsonProperty("id")
	private String id;
	@JsonProperty("text")
	private String text;
	@JsonProperty("hText")
	private String homogText;
	@JsonProperty("score")
	private Float avgSolrScore = 0F;
	@JsonProperty("count")
	private Long resultsCount = 0L;		// txtCount
	@JsonProperty("share")
	private Float percentOfTotal = null;
	@JsonProperty("meta")
	private Map<String, Object> meta = null;
	
	// [hTxtWord,orgWord,startIdx]		::  startIdx is the start-index of orgWord in 'text' (the literal/original text) 
	// we need a list since same hTxt word may come again
	@JsonProperty("ht2w")
	private List<Object[]> hTxtWord2OrgWordIdxList = new ArrayList<>();
	
	@JsonIgnore
	private String hTxtId;
	@JsonIgnore
	private String syndText;
	@JsonIgnore
	private String text2Deliver;
	@JsonIgnore
	private Integer wCnt = null;
	@JsonIgnore
	private List<SolrDocument> docs = new ArrayList<>();
	
	
	@JsonIgnore
	SolrDocument finalPickedDoc;
	
	@JsonIgnore
	SynonymApplier synonymApplier = null;
	@JsonIgnore
	Comparator<SolrDocument> txtCntComparator = new SolrDocumentIntFieldComparator(BaseHelper.solrField_txtCnt).reversed();
	@JsonProperty("hTxt")
	private String hTxt;
	
	private float simScore = 0;
	
	
	public float getSimScore() {
		return simScore;
	}

	public void setSimScore(float simScore) {
		this.simScore = simScore;
	}

	public String gethTxt() {
		return hTxt;
	}

	public void sethTxt(String hTxt) {
		this.hTxt = hTxt;
	}

	public SearchResultsGroupSummary() { }
	
	public SearchResultsGroupSummary(SolrDocument sd) {
		add2Group(sd);
		/*
		this.id = getDocId(sd);
		docs.add(sd);
		
		this.text = SolrUtils.getSolrDocFieldAsString(sd, "txt");
		this.avgSolrScore = getDocScore(sd);
		
		this.homogText = SolrUtils.getSolrDocFieldAsString(sd, SolrFieldName_HomogText);
		if (StringUtils.isBlank(this.homogText)) {
			log.debug("group.fill(): no HomogText found, calculating now.");
			try {
				if (null != synonymApplier)
					this.syndText = synonymApplier.getOneWaySynonymizedText(this.text);
				else
					this.syndText = this.text;
				this.homogText = getHomogenizedText(this.syndText);
			} catch (FileNotFoundException e) {
				log.error("Error while calculating homogText of:" + this.text, e);
			}
		}
		resultsCount = (Long) sd.getFirstValue(BaseHelper.solrField_txtCnt);
		*/
	}
	
	@JsonIgnore
	public SolrDocument getFinalPickedDoc() {
		return this.finalPickedDoc;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getHTxtId() {
		return hTxtId;
	}
	public void sethTxtId(String hTxtId) {
		this.hTxtId = hTxtId;
	}

	public List<SolrDocument> getDocs() {
		return docs;
	}
	public void setDocs(List<SolrDocument> docs) {
		this.docs = docs;
	}


	public String getSyndText() {
		return syndText;
	}

	public void setSyndText(String syndText) {
		this.syndText = syndText;
	}

	public String getHomogText() {
		return homogText;
	}

	public void setHomogText(String homogText) {
		this.homogText = homogText;
	}

	public Float getAvgSolrScore() {
		return avgSolrScore;
	}

	public void setAvgSolrScore(Float avgSolrScore) {
		this.avgSolrScore = avgSolrScore;
	}

	public Long getResultsCount() {
		return resultsCount;
	}

	public void setResultsCount(Long resultsCount) {
		this.resultsCount = resultsCount;
	}

	public Float getPercentOfTotal() {
		return percentOfTotal;
	}

	public void setPercentOfTotal(Float percentOfTotal) {
		this.percentOfTotal = percentOfTotal;
	}

	@JsonIgnore
	public String getText2Deliver() {
		return text2Deliver;
	}
	@JsonIgnore
	public void setText2Deliver(String text2Deliver) {
		this.text2Deliver = text2Deliver;
		this.setText(text2Deliver);
	}

	public Integer getwCnt() {
		return wCnt;
	}

	public void setwCnt(Integer wCnt) {
		this.wCnt = wCnt;
	}


	// ********************************

	public void setSynonyms(List<String[]> synonyms) {
		if (null == synonymApplier) {
			synonymApplier = new SynonymApplier();
			synonymApplier.setSynonymsArray(synonyms);	
		}
	}
	public void setSynonymsApplier(SynonymApplier synonymApplier) {
		if (null == this.synonymApplier  &&  null != synonymApplier) {
			this.synonymApplier = synonymApplier;
		}
	}
	
	public void setMetaData(SolrDocument srcDoc, String... fields) {
		if (null == srcDoc)
			srcDoc = docs.get(0);
		if (null == meta)
			meta = new HashMap<>();
		for (String fld : fields) {
			meta.put(fld, srcDoc.getFieldValue(fld));
		}
	}
	
	
	public Map<String, Object> getMeta() {
		return meta;
	}

	public void applySynonymsAndCalculateHtxt() {
		if (null != synonymApplier)
			this.syndText = QuickSynonymApplier.applyQuickSynonyms2Sent(this.text);			//synonymApplier.getOneWaySynonymizedText(this.text);
		else {
			log.warn("I was asked to apply synonyms, but no syns were provided! Using pre-calculated hTxt only or using text to get hTxt.");
			if (StringUtils.isNotBlank(this.homogText))
				return;
			this.syndText = this.text;
		}
		this.homogText = getHomogenizedText(this.syndText);
	}
	
	/*
	public SearchResultsGroupSummary copyFrom(SearchResultsGroupSummary srgs) {
		this.id = srgs.id;
		this.text = srgs.text;
		this.syndText = srgs.syndText;
		this.homogText = srgs.homogText;
		//this.hTextEncrypted = srgs.hTextEncrypted;
		this.avgSolrScore = srgs.avgSolrScore;
		this.resultsCount =	srgs.resultsCount;
		this.sd = srgs.sd;
		
		this.literalTextTo_OccurenceCount_Map.clear();
		this.literalTextTo_OccurenceCount_Map.putAll(srgs.literalTextTo_OccurenceCount_Map);
		
		docIdsInGroup.clear();
		docIdsInGroup.addAll(srgs.docIdsInGroup);
		return this;
	}
	*/
	
	public boolean isDocInGroup(SolrDocument sd) {
		//String id = getDocId(sd);
		return docs.contains(sd);
	}
	
	/**
	 * New document to be added to this group - this incoming doc's homogText should be same as existing. Just avg the score (if not equal) and increase the count.
	 * @param sd
	 */
	public void add2Group(SolrDocument sd) {
		// FIXME::  Temporary
		add2GroupOnDocField(sd, SolrFieldName_Text);
		/*
		String t = SolrUtils.getSolrDocFieldAsString(sd, SolrFieldName_Text);
		if (StringUtils.isBlank(t)) {
			try {
				log.warn("No txt field found in doc: " + JSonUtils.object2JsonString(sd));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			return;		// FIXME: ignore this doc if it has no txt??
		}
		
		int ttlDocs = docs.size();
		float score = getDocScore(sd);
		if (score != avgSolrScore) {
			this.avgSolrScore = ((avgSolrScore * ttlDocs) + score) / (ttlDocs + 1);
		}
		resultsCount += (Long) sd.getFirstValue(BaseHelper.solrField_txtCnt);
		docs.add(sd);
		
		// and take the doc with highest txtCnt
		useMostOccurredText();
		*/
	}
	
	public void add2GroupOnDocField(SolrDocument sd, String docField) {
		String t = SolrUtils.getSolrDocFieldAsString(sd, docField);
		if (StringUtils.isBlank(t)) {
			try {
				log.warn("No "+docField+" field found in doc: " + JSonUtils.object2JsonString(sd));
			} catch (JsonProcessingException e) {
				log.warn("", e);
			}
			return;		// TODO: ignore this doc if doc has no value for field?
		}
		int ttlDocs = docs.size();
		float score = getDocScore(sd);
		if (score != avgSolrScore) {
			this.avgSolrScore = ((avgSolrScore * ttlDocs) + score) / (ttlDocs + 1);
		}
		resultsCount += (Long) sd.getFirstValue(BaseHelper.solrField_txtCnt);
		docs.add(sd);
		
		// and take the doc with highest txtCnt
		useMostOccurredText();
	}
	
	
	
	public SearchResultsGroupSummary merge(SearchResultsGroupSummary srgs) {
		docs.addAll(srgs.docs);
		if (this.avgSolrScore != srgs.avgSolrScore)
			this.avgSolrScore = ((avgSolrScore * resultsCount) + (srgs.avgSolrScore * srgs.resultsCount) ) / (resultsCount + srgs.resultsCount);
		resultsCount += srgs.resultsCount;
		// if 2-b-delivered text is provided by external means, do not chanve/override that
		if (StringUtils.isBlank(getText2Deliver())) {
			// and take/use the most used/occurred text
			useMostOccurredText();
		}
		
		return this;
	}
	
	
	public Map<Object, Object> toDebugMap() {
		Map<Object, Object> map = new LinkedHashMap<>();
		map.put("id", getId());
		map.put("score", getAvgSolrScore());
		map.put("hTxt", getHomogText());
		map.put("txt", getText());
		map.put("count", getResultsCount());
		map.put("hTxtId", getHTxtId());
		map.put("synonymizedText", getSyndText());
		map.put("text2Deliver", getText2Deliver());
		map.put("docs", docs);
		map.put("percentOfTotal", percentOfTotal);
		return map;
	}
	
	public void calculatePercentOfTotal(long totalRecords) {
		setPercentOfTotal((resultsCount * 100.0F)/totalRecords);
	}

	public void createHtxtWordToOrgWordMap() {
		//List<Object[]> hTxtWord2OrgWordIdxList	=	[hTxtWord,orgWord,startIdx]
		hTxtWord2OrgWordIdxList.clear();
		String orgTxt = getText();
		List<KeyValuePair<String, String>> hw2sw = Stemmer.removeDefsStopwordsStem_IncludeSourceWord(orgTxt);
		Stemmer.ensureSourceWordStartsWithStemmedWord(hw2sw);
		int sIdx = -1;
		for (KeyValuePair<String, String> kv : hw2sw) {
			sIdx = orgTxt.indexOf(kv.getValue(), sIdx+1);
			Object[] ary = {kv.getKey(), kv.getValue(), sIdx};
			hTxtWord2OrgWordIdxList.add(ary);
		}
		
		/*
		String[] orgWords = getText().split(" ");
		String htW;
		int sIdx = 0;
		// loop thru each word, and make a list of hTxt word with its orgWord and startIdx
		for (String w : orgWords) {
			htW = getHomogenizedText(w);
			if (StringUtils.isNotBlank(htW)) {
				Object[] ary = {htW, w, sIdx};
				hTxtWord2OrgWordIdxList.add(ary);
			}
			sIdx += w.length() + 1;			// start-index of next word = this word's length + 1 space
		}
		*/
	}
	
	public List<Object[]> gethTxtWord2OrgWordIdxList() {
		return hTxtWord2OrgWordIdxList;
	}
	public void sethTxtWord2OrgWordIdxList(List<Object[]> hTxtWord2OrgWordIdxList) {
		this.hTxtWord2OrgWordIdxList = hTxtWord2OrgWordIdxList;
	}

	public String getHomogenizedText(String text) {
		return Stemmer.tokenized_def_stop_stem(text);
		//return Utilities.tokenized_def_stop_stem(text);
	}
	
	// ***********************************
	
	/*
	private void encryptHomogText() {
		if (StringUtils.isBlank(homogText))
			return;
		this.hTextEncrypted = "";
		char[] chars = homogText.toCharArray();
		int ascii = 0;
		for (char c : chars) {
			if (c == 32) {
				this.hTextEncrypted += String.valueOf((char)ascii);
				ascii = 0;
				continue;
			}
			ascii += c;
		}
		// last word
		if (ascii > 0)
			this.hTextEncrypted += String.valueOf((char)ascii);
	}
	*/
	
	private String getDocId(SolrDocument sd) {
		return (String) sd.getFieldValue("id");
	}
	private static float getDocScore(SolrDocument sd) {
		return SolrUtils.getSolrDocFieldAsFloat(sd, "score");
	}
	
	public void useMostOccurredText() {
		// sort "docs" by txtCnt, and pick the top (highest count)
		if (docs.size() > 1)
			docs.sort(txtCntComparator) ;
		SolrDocument topDoc = docs.get(0);
		this.finalPickedDoc = topDoc;
		this.id = getDocId(topDoc);
		this.text = SolrUtils.getSolrDocFieldAsString(topDoc, SolrFieldName_Text);
		this.homogText = SolrUtils.getSolrDocFieldAsString(topDoc, SolrFieldName_HomogText);
		this.wCnt = this.text.split(" ").length;
		
		/*
		Integer lastC = null;
		String mostUsedTxt = null;
		SearchResultsGroupSummary doc, mostUsedDoc = null;
		for (String txt : literalTextTo_OccurenceCount_Map.keySet()) {
			doc = literalTextTo_OccurenceCount_Map.get(txt);
			if (null == lastC) {
				mostUsedDoc = doc;
				lastC = doc.resultsCount;		// initialize 'lastC' with first doc's count
				continue;
			}
			// from 2nd instance onwards
			if (doc.resultsCount > lastC) {
				lastC = doc.resultsCount;
				mostUsedTxt = doc.text;
				mostUsedDoc = doc;
			}
		}
		if (StringUtils.isNotBlank(mostUsedTxt))
			this.text = mostUsedTxt;

		this.id = mostUsedDoc.id;
		// pick ID from latest year K (doc.docIdsInGroup)
		// year is in KId::  0000950135-96-002763_3_60_351		=>  -96-
		Map<Integer, String> year2IdMap = new TreeMap<>(Collections.reverseOrder());
		int yr, maxYr = -1;
		for (String kid : mostUsedDoc.docIdsInGroup) {
			yr = Integer.parseInt(kid.substring(11, 13));
			if (yr >= 75  &&  yr <= 99)
				yr = 1900 + yr;
			else
				yr = 2000 + yr;
			year2IdMap.put(yr, kid);
			maxYr = (yr > maxYr)? yr: maxYr;
		}
		if (year2IdMap.containsKey(maxYr))
			this.id = year2IdMap.get(maxYr);
		*/
	}
	
	
	/**
	 * normalize - remove initial "(a), initial-caps, Section (..)"  etc..
	 * @param txt
	 * @return
	 */
	/*
	private String normalizeTxt(String txt) {
		txt = txt.trim().replaceAll("^\\([A-Za-z0-9]+\\)", "").replaceAll("(([Ss]ection|[aA]rticle)[ 0-9\\.\\-ivxmIVXM]+[\\(\\)\\[\\]a-z0-9\\.\\-ivxmIVXM]*)", "");
		return txt.trim();
	}
	*/
	
	
	// ***********************************
	
	public static class SortByResultsCount implements Comparator<SearchResultsGroupSummary> {
		int direction = 1;
		
		public SortByResultsCount() {}
		public SortByResultsCount(int direction) {
			this.direction = direction;
		}
		@Override
		public int compare(SearchResultsGroupSummary o1, SearchResultsGroupSummary o2) {
			return ( o1.getResultsCount().compareTo(o2.getResultsCount()) * direction );
		}
	}

	public static class SortByAvgScore implements Comparator<SearchResultsGroupSummary> {
		int direction = 1;
		
		public SortByAvgScore() {}
		public SortByAvgScore(int direction) {
			this.direction = direction;
		}
		@Override
		public int compare(SearchResultsGroupSummary o1, SearchResultsGroupSummary o2) {
			return ( o1.getAvgSolrScore().compareTo(o2.getAvgSolrScore()) * direction );
		}
	}

	
}
