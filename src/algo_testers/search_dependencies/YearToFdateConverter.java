package algo_testers.search_dependencies;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import algo_testers.search_dependencies.ContractTypeConfig.ContractConfig;


public class YearToFdateConverter {
	private static String START_YEAR_DATE_AND_MONTH = "0101";
	private static String END_YEAR_DATE_AND_MONTH = "1231";

	public static String getFdateRangeClause(int startYr, int endYr) {
		String startRange = String.valueOf(startYr) + START_YEAR_DATE_AND_MONTH;
		String endRange = String.valueOf(endYr) + END_YEAR_DATE_AND_MONTH;
		return "fDate:[" + startRange + " TO " + endRange + "]";
	}

	public static String getCollectionName(ContractConfig contractConfig, List<String> years) {
		List<String> collectionNames = new ArrayList<String>();
		String baseCollectionName = contractConfig.getCollectionName();
		if (contractConfig.getYearToIds().isEmpty()) {
			years.forEach(yr -> {
				collectionNames.add(baseCollectionName + "_" + yr);
			});
		} else {
			Map<String, List<String>> documentIdsByYear = ContractTypeConfigHelper.getDocumentIdsByYearMap(contractConfig.getYearToIds());
			years.forEach(yr -> {
				if (documentIdsByYear.containsKey(yr)) {
					collectionNames.add(baseCollectionName + "_" + yr);
				}
			});
		}
		return StringUtils.join(collectionNames, ",");
	}

	public static String getCollectionName(String baseCollectionName, List<String> years) {
		List<String> collectionNames = years.stream().map(yr -> baseCollectionName + "_" + yr)
				.collect(Collectors.toList());

		return StringUtils.join(collectionNames, ",");
	}

	public static List<Integer> getDefaultYearRangeCollectionName() {
		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		List<Integer> collectionNames = new ArrayList<Integer>();

		for (int i = 0; i < 5; i++) {
			collectionNames.add(currentYear);
			currentYear--;
		}
        Collections.reverse(collectionNames);
		return collectionNames;
	}

	public static List<Integer> getNextFiveYearCollectionName(int fromYear) {
		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		List<Integer> collectionNames = new ArrayList<Integer>();

		for (int i = 0; i < 5; i++) {
			if (fromYear == currentYear) {
				collectionNames.add(fromYear);
				break;
			} else {
				collectionNames.add(fromYear);
			}
			fromYear++;
		}

		return collectionNames;
	}

	public static List<Integer> getLastFiveYearCollectionName(int fromYear) {
		List<Integer> collectionNames = new ArrayList<Integer>();

		for (int i = 0; i < 5; i++) {
			collectionNames.add(fromYear);
			fromYear--;
		}
		Collections.reverse(collectionNames);
		return collectionNames;
	}

	public static List<Integer> getCollectionName(int startYear, int endYear) {
		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		List<Integer> collectionNames = new ArrayList<Integer>();

		if (startYear <= endYear) {
			for (int yr = startYear; yr <= endYear && yr <= currentYear; yr++) {
				collectionNames.add(yr);
			}
		} else {
			return getDefaultYearRangeCollectionName();
		}
		return collectionNames;
	}

}
