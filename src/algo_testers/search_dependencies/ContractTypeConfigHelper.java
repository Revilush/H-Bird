package algo_testers.search_dependencies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import algo_testers.search_dependencies.ContractTypeConfig.ContractConfig;

public class ContractTypeConfigHelper {
	private static final String CONFIG_FILE = "contractType-config.json";
	private static ContractTypeConfig contractTypeConfig = null;
	private static Log log = LogFactory.getLog(ContractTypeConfigHelper.class);

	static {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			contractTypeConfig = objectMapper.readValue(
					ContractTypeConfigHelper.class.getClassLoader().getResourceAsStream(CONFIG_FILE),
					ContractTypeConfig.class);
		} catch (IOException e) {
			log.error("Error reading config text from resource file : ", e);
		}
	}

	public static ContractTypeConfig getContractTypeConfig() {
		return contractTypeConfig;
	}

	public static List<String> getDocumentTdsByContractType(String contractType, List<String> years) {
		List<String> documentids = new ArrayList<String>();
		ContractConfig contractConfig = getContractConfigByContractName(contractType);

		if (null == contractConfig) {
			return documentids;
		}

		Map<String, List<String>> documentIdsByYear = getDocumentIdsByYearMap(contractConfig.getYearToIds());

		years.forEach(yr -> {
			List<String> yearFQ = documentIdsByYear.get(yr);
			if (null != yearFQ) {
				documentids.addAll(yearFQ);
			}
		});

		return documentids;
	}

	public static ContractConfig getContractConfigByContractName(String contractName) {
		return getContractConfigs().get(contractName);
	}

	public static Map<String, ContractConfig> getContractConfigs() {
		Map<String, ContractConfig> contractConfigByContractName = new HashMap<>();

		contractTypeConfig.getContractTypes().forEach(map -> {
			contractConfigByContractName.putAll(map);
		});
		return contractConfigByContractName;
	}

	public static Map<String, List<String>> getDocumentIdsByYearMap(
			List<Map<String, List<String>>> documentIdsByYearList) {
		Map<String, List<String>> documentIdsByYearMap = new HashMap<>();

		documentIdsByYearList.forEach(map -> {
			map.entrySet().forEach(docIdsByYear -> {
				documentIdsByYearMap.put(docIdsByYear.getKey(), docIdsByYear.getValue());
			});
		});
		return documentIdsByYearMap;
	}
	
	public static ContractConfig getDefaultConfig() {
		return contractTypeConfig.getDefaultContractTypeConfig();
	}

}
