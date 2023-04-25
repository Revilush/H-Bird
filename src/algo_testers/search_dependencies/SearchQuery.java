package algo_testers.search_dependencies;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchQuery {

	private String q;
	//private String allWords;			// MUST-Present words
	//private String mustNotWords;
	private String legalTerm;
	private String legalTermGuidance;
	
	private String keywords;
	private String clause;
	private String withinContracts;
	private String withinParties;
	private String exh;
	
	private String contractType;
	private String cik;			// list of CIKs
	
	private boolean is_broadining;
	
	private String contractNameAlgo;
	
	@JsonProperty("totalResults")
	private int finalResultsCount;
	@JsonProperty("minimumMatch")
	private String mm;
	@JsonProperty("wordCnt")
	private Float[] wordCntRange;
	@JsonProperty("searchByContractType")
	private boolean isContractTypeSearch;
	@JsonProperty("groupByFQ")
	private boolean isGroupFQ;
	
	
	public boolean isContractTypeSearch() {
		return isContractTypeSearch;
	}
	public void setContractTypeSearch(boolean isContractTypeSearch) {
		this.isContractTypeSearch = isContractTypeSearch;
	}
	
	public boolean isGroupFQ() {
		return isGroupFQ;
	}
	public void setGroupFQ(boolean isGroupFQ) {
		this.isGroupFQ = isGroupFQ;
	}

	/**
	 * Any custom filters being sent by client
	 */
	@JsonProperty("filters")
	private Map<String, Object> customFilters;
	@JsonProperty("filters0")
	private Map<String, Object> customDoc0Filters;
	
	
	/*
	private String terms;
	private List<String> withinDefinedTerms;
	private List<String> withinSections;
	private List<String> withinExhibits;
	*/
	
	//private Integer withinLastYears;
	private String withinLastYears;
	
	public int getFinalResultsCount() {
		return finalResultsCount;
	}
	public void setFinalResultsCount(int finalResultsCount) {
		this.finalResultsCount = finalResultsCount;
	}
	public String getMm() {
		return mm;
	}
	public void setMm(String mm) {
		this.mm = mm;
	}
	public Float[] getWordCntRange() {
		return wordCntRange;
	}
	public void setWordCntRange(Float[] wordCntRange) {
		this.wordCntRange = wordCntRange;
	}
	public String getQ() {
		return q;
	}
	public void setQ(String q) {
		this.q = q;
	}
	/*
	public String getAllWords() {
		return allWords;
	}
	public void setAllWords(String allWords) {
		this.allWords = cleanFQs(allWords);
	}
	public String getMustNotWords() {
		return mustNotWords;
	}
	public void setMustNotWords(String mustNotWords) {
		this.mustNotWords = cleanFQs(mustNotWords);
	}
	*/
	public String getWithinLastYears() {
		return withinLastYears;
	}
	public void setWithinLastYears(String withinLastYears) {
		this.withinLastYears = withinLastYears;
	}
	/*
	public List<String> getWithinDefinedTerms() {
		return withinDefinedTerms;
	}
	public void setWithinDefinedTerms(List<String> withinDefinedTerms) {
		this.withinDefinedTerms = withinDefinedTerms;
	}
	public void setWithinDefinedTerms(String withinDefinedTerm) {
		if (null == this.withinDefinedTerms)
			this.withinDefinedTerms = new ArrayList<>();
		else
			this.withinDefinedTerms.clear();
		if (StringUtils.isNotBlank(withinDefinedTerm)) {
			this.withinDefinedTerms.addAll(splitIntoList(withinDefinedTerm));
		}
	}
	
	public List<String> getWithinSections() {
		return withinSections;
	}
	public void setWithinSections(List<String> withinSections) {
		this.withinSections = withinSections;
	}
	public void setWithinSections(String withinSection) {
		if (null == this.withinSections)
			this.withinSections = new ArrayList<>();
		else
			this.withinSections.clear();
		if (StringUtils.isNotBlank(withinSection)) {
			this.withinSections.addAll(splitIntoList(withinSection));
		}
	}
	*/
	
	
	
	public boolean getIs_broadining() {
		return is_broadining;
	}
	public String getContractNameAlgo() {
		return contractNameAlgo;
	}
	public void setContractNameAlgo(String contractNameAlgo) {
		this.contractNameAlgo = contractNameAlgo;
	}
	public void setIs_broadining(boolean is_broadining) {
		this.is_broadining = is_broadining;
	}
	
	public String getKeywords() {
		return keywords;
	}

	
	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}
	public String getClause() {
		return clause;
	}
	public void setClause(String clause) {
		this.clause = clause;
	}
	public String getWithinContracts() {
		return withinContracts;
	}
	public void setWithinContracts(String withinContracts) {
		this.withinContracts = withinContracts;
	}
	public String getWithinParties() {
		return withinParties;
	}
	public void setWithinParties(String withinParties) {
		this.withinParties = withinParties;
	}
	
	public String getExh() {
		return exh;
	}
	public void setExh(String exh) {
		this.exh = exh;
	}
	public String getContractType() {
		return contractType;
	}
	public void setContractType(String contractType) {
		this.contractType = contractType;
	}
	public String getCik() {
		return cik;
	}
	public void setCik(String cik) {
		this.cik = cik;
		if (StringUtils.isNotBlank(cik)) {
			cik = cik.replaceAll("[;: ,]+", " ").trim();
			this.cik = cik;
		}
	}
	public Map<String, Object> getCustomFilters() {
		return customFilters;
	}
	public void setCustomFilters(Map<String, Object> customFilters) {
		this.customFilters = customFilters;
	}
	public Map<String, Object> getCustomDoc0Filters() {
		return customDoc0Filters;
	}
	public void setCustomDoc0Filters(Map<String, Object> customDoc0Filters) {
		this.customDoc0Filters = customDoc0Filters;
	}
	
	/*
	public String getTerms() {
		return terms;
	}
	public void setTerms(String terms) {
		this.terms = terms;
	}
	public List<String> getWithinExhibits() {
		return withinExhibits;
	}
	public void setWithinExhibits(String withinExhibits) {
		if (null == this.withinExhibits)
			this.withinExhibits = new ArrayList<>();
		else
			this.withinExhibits.clear();
		if (StringUtils.isNotBlank(withinExhibits)) {
			this.withinExhibits.addAll(splitIntoList(withinExhibits));
		}
	}
	public void setWithinExhibits(List<String> withinExhibits) {
		this.withinExhibits = withinExhibits;
	}
	*/
	
	
	
	// *********************************************** //
	
	@SuppressWarnings("unused")
	private List<String> splitIntoList(String text) {
		List<String> terms = new ArrayList<>();
		if (StringUtils.isBlank(text))
			return terms;
		String[] parts = text.trim().split(",");
		for (String t : parts)
			if (StringUtils.isNotBlank(t))
				terms.add(t.trim());
		return terms;
	}
	
	@SuppressWarnings("unused")
	private String cleanFQs(String words) {
		if (null == words)
			return words;
		// remove all chars that will create trouble when used in solr FQ
		return words.replaceAll("[\\:\\(\\)\\[\\]]+", " ").replaceAll("[\\s]{2,}", " ").trim();
	}
	public String getLegalTerm() {
		return legalTerm;
	}
	public void setLegalTerm(String legalTerm) {
		this.legalTerm = legalTerm;
	}
	public String getLegalTermGuidance() {
		return legalTermGuidance;
	}
	public void setLegalTermGuidance(String legalTermGuidance) {
		this.legalTermGuidance = legalTermGuidance;
	}
	
}
