package algo_testers.search_dependencies;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DocsMetaData {
	
	@JsonProperty("totalDocs")
	private Integer totalContracts;
	
	private List<DocMetaData> metaData;

	
	
	public Integer getTotalContracts() {
		return totalContracts;
	}
	public void setTotalContracts(Integer totalContracts) {
		this.totalContracts = totalContracts;
	}

	public List<DocMetaData> getMetaData() {
		return metaData;
	}
	public void setMetaData(List<DocMetaData> metaData) {
		this.metaData = metaData;
		this.totalContracts = null;
		if (null != metaData)
			totalContracts = metaData.size();
			
	}
	public void addMetaData(DocMetaData metaData) {
		if (null == this.metaData)
			this.metaData = new ArrayList<>();
		this.metaData.add(metaData);
		if (null == totalContracts)
			totalContracts = 0;
		totalContracts ++;
	}




	// *******************************************

	public static class DocMetaData {
		private String contractName;
		private String kid;
		private Integer cik;
		private String edgarLink;
		private String oPara;		// openingPara
		
		public DocMetaData(SolrDocument doc) {
			this.contractName = SolrUtils.getSolrDocFieldAsString(doc, "contractLongName");
			this.kid = SolrUtils.getSolrDocFieldAsString(doc, "kId");
			this.cik = SolrUtils.getSolrDocFieldAsInteger(doc, "cik");
			this.edgarLink = SolrUtils.getSolrDocFieldAsString(doc, "edgarLink");
			this.oPara = SolrUtils.getSolrDocFieldAsString(doc, "openingParagraph");
			if (StringUtils.isNotBlank(this.oPara)  &&  this.oPara.startsWith("^"))
				this.oPara = this.oPara.substring(1);
		}
		public DocMetaData(String contractName, String edgarLink, String openingPara) {
			this(contractName, null, null, edgarLink, openingPara);
		}
		public DocMetaData(String contractName, String kid, Integer cik, String edgarLink, String openingPara) {
			this.contractName = contractName;
			this.kid = kid;
			this.cik = cik;
			this.edgarLink = edgarLink;
			this.oPara = openingPara;
		}
		
		public String getEdgarLink() {
			return edgarLink;
		}
		public void setEdgarLink(String edgarLink) {
			this.edgarLink = edgarLink;
		}
		public String getContractName() {
			return contractName;
		}
		public void setContractName(String contractName) {
			this.contractName = contractName;
		}
		public String getKid() {
			return kid;
		}
		public void setKid(String kid) {
			this.kid = kid;
		}
		public Integer getCik() {
			return cik;
		}
		public void setCik(Integer cik) {
			this.cik = cik;
		}
		public String getOPara() {
			return oPara;
		}
		public void setOPara(String oPara) {
			this.oPara = oPara;
		}
		
	}
	
	
}
