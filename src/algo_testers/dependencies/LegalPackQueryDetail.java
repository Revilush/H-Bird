package algo_testers.dependencies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import charting.JSonUtils;
import search.SolrJClient;

/**
 * fields to hold values of legalPack solr document in coreAdmin.
 * The field names match the actual field-name in the solr doc (so Jackson/JsonUtils can fill in automatically).
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)	
public class LegalPackQueryDetail {

	private String id;
	
	private List<String> core;
	private List<String> contractType;
	
	private String documentType = "legalPack";		//FIXME: look for some sort of constant/config!!
	private String legalTerm;
	private String legalTermComments;
	
	private String queryField;
	private String queryText;
	private List<String> altQueryTexts;
	
	private List<String> patterns;
	private List<String> patternComments;
	private List<String> patternCategories;
	
	private String clientId;
	private String clientRefQuery;
	private List<String> clientPatternsDesire;
	private List<List<String>> altFqs = new ArrayList<>();

	private List<String> commonFqs;
	//private String txtType
	
	public LegalPackQueryDetail(){			// default constructor - typically required for jackson/JsonUtils 
	}
	
	public LegalPackQueryDetail(String contractType, String core, String legalTerm) {
		this(contractType, core, legalTerm, null);
	}
	
	public LegalPackQueryDetail(String contractType, String core, String legalTerm, String clientId) {
		this.contractType = new ArrayList<String>(Arrays.asList(contractType));
		this.core = new ArrayList<String>(Arrays.asList(core));
		this.legalTerm = legalTerm;
		this.clientId = clientId;
	}
	
	@SuppressWarnings("unchecked")
	public static LegalPackQueryDetail getInstance(Map<?,?> objMap) {
		Map<Object, Object> doc = new LinkedHashMap<>();
		for (Object k : objMap.keySet()) {
			doc.put(k, objMap.get(k));
		}
		LegalPackQueryDetail lpqd = JSonUtils.map2Object(doc, LegalPackQueryDetail.class);
		List<String> altFq;
		for (int i=0; i >= 0; i++) {
			altFq = (List<String>) objMap.get("altFqs"+i);
			if (null == altFq)
				break;
			lpqd.altFqs.add(altFq);
		}
		return lpqd;
	}
	
	public SolrQuery getSolrQuery(Map<String, String> otherParams) {
		if (null == otherParams) {
			otherParams = new HashMap<>();
			otherParams.put("rows", "5000");
		}
		String[] FQs = new String[] {"documentType:(legalPack)"};
		if (StringUtils.isNotBlank(id))
			FQs = ArrayUtils.add(FQs, "id:("+id+")");
		if (null != contractType  &&  contractType.size() > 0)
			FQs = ArrayUtils.add(FQs, "contractType:("+contractType.get(0)+")");
//		if (StringUtils.isNotBlank(core))
//			FQs = ArrayUtils.add(FQs, "core:("+core.get(0)+")");
		if (StringUtils.isNotBlank(legalTerm))
			FQs = ArrayUtils.add(FQs, "legalTerm:("+legalTerm+")");
		if (StringUtils.isNotBlank(clientId))
			FQs = ArrayUtils.add(FQs, "clientId:("+clientId+")");
		
		return SolrJClient.makeQuery("*:*", FQs, null, otherParams);
	}
	
	/**
	 * 
	 * @return list of patterns where client-desire for the pattern is MB|MNB. These patterns only are eligible to generate combIds.
	 */
	@JsonIgnore
	public List<String> getComboIdEligiblePatterns() {
		List<String> ptrnResp = new ArrayList<>();
		if (null == patterns  ||  null == clientPatternsDesire)
			return null;
		for (int p=0; p < patterns.size()  &&  p < clientPatternsDesire.size(); p++) {
			if (StringUtils.equalsAnyIgnoreCase(clientPatternsDesire.get(p), "MB", "MNB"))
					ptrnResp.add(patterns.get(p));
		}
		return ptrnResp;
	}
	
	@JsonIgnore
	public Map<String, String> getComboIdEligiblePatternsWithClentDesires() {
		Map<String, String> ptrnResp = new LinkedHashMap<>();
		if (null == patterns  ||  null == clientPatternsDesire)
			return null;
		for (int p=0; p < patterns.size()  &&  p < clientPatternsDesire.size(); p++) {
			if (StringUtils.equalsAnyIgnoreCase(clientPatternsDesire.get(p), "MB", "MNB"))
					ptrnResp.put(patterns.get(p), clientPatternsDesire.get(p));
		}
		return ptrnResp;
	}
	
	

	// *************************************************************** //
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getDocumentType() {
		return documentType;
	}
	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

	public List<String> getCore() {
		return core;
	}

	public void setCore(List<String> core) {
		this.core = core;
	}

	public List<String> getContractType() {
		return contractType;
	}

	public void setContractType(List<String> contractType) {
		this.contractType = contractType;
	}

	public String getLegalTerm() {
		return legalTerm;
	}

	public void setLegalTerm(String legalTerm) {
		this.legalTerm = legalTerm;
	}

	public String getLegalTermComments() {
		return legalTermComments;
	}

	public void setLegalTermComments(String legalTermComments) {
		this.legalTermComments = legalTermComments;
	}

	public String getQueryField() {
		return queryField;
	}

	public void setQueryField(String queryField) {
		this.queryField = queryField;
	}

	public String getQueryText() {
		return queryText;
	}

	public void setQueryText(String queryText) {
		this.queryText = queryText;
	}

	public List<String> getAltQueryTexts() {
		return altQueryTexts;
	}

	public void setAltQueryTexts(List<String> altQueryTexts) {
		this.altQueryTexts = altQueryTexts;
	}

	public List<String> getPatterns() {
		return patterns;
	}

	public void setPatterns(List<String> patterns) {
		this.patterns = patterns;
	}

	public List<String> getPatternComments() {
		return patternComments;
	}

	public void setPatternComments(List<String> patternComments) {
		this.patternComments = patternComments;
	}

	public List<String> getPatternCategories() {
		return patternCategories;
	}

	public void setPatternCategories(List<String> patternCategories) {
		this.patternCategories = patternCategories;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientRefQuery() {
		return clientRefQuery;
	}

	public void setClientRefQuery(String clientRefQuery) {
		this.clientRefQuery = clientRefQuery;
	}

	public List<String> getClientPatternsDesire() {
		return clientPatternsDesire;
	}

	public void setClientPatternsDesire(List<String> clientPatternsDesire) {
		this.clientPatternsDesire = clientPatternsDesire;
	}

	public List<List<String>> getAltFqs() {
		return altFqs;
	}

	public void setAltFqs(List<List<String>> altFqs) {
		this.altFqs = altFqs;
	}

	public List<String> getCommonFqs() {
		return commonFqs;
	}

	public void setCommonFqs(List<String> commonFqs) {
		this.commonFqs = commonFqs;
	}
}
