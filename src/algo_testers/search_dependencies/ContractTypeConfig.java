package algo_testers.search_dependencies;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContractTypeConfig {
	@JsonProperty("contractType-config")
	public List<Map<String, ContractConfig>> contractTypes;
	
	@JsonProperty("default-config")
	public ContractConfig defaultContractTypeConfig;

	public List<Map<String, ContractConfig>> getContractTypes() {
		return contractTypes;
	}

	public void setContractTypes(List<Map<String, ContractConfig>> contractTypes) {
		this.contractTypes = contractTypes;
	}
	
	public ContractConfig getDefaultContractTypeConfig() {
		return defaultContractTypeConfig;
	}

	public void setDefaultContractTypeConfig(ContractConfig defaultContractTypeConfig) {
		this.defaultContractTypeConfig = defaultContractTypeConfig;
	}



	public static class ContractConfig {
		@JsonProperty("base-server-url")
		private String baseServerUrl;
		@JsonProperty("collection-name")
		private String collectionName;
		@JsonProperty("year-to-ids")
		private List<Map<String, List<String>>> yearToIds;

		public String getBaseServerUrl() {
			return baseServerUrl;
		}

		public void setBaseServerUrl(String baseServerUrl) {
			this.baseServerUrl = baseServerUrl;
		}

		public String getCollectionName() {
			return collectionName;
		}

		public void setCollectionName(String collectionName) {
			this.collectionName = collectionName;
		}

		public List<Map<String, List<String>>> getYearToIds() {
			return yearToIds;
		}

		public void setYearToIds(List<Map<String, List<String>>> yearToIds) {
			this.yearToIds = yearToIds;
		}

	}

}
