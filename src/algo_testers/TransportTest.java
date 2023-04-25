package algo_testers;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import algo_testers.search_dependencies.JSonUtils;
import algo_testers.search_dependencies.LegalHttpClient;
import algo_testers.search_dependencies.SearchQuery;

public class TransportTest {

	public static void runSearchQueries(String[] years, String[] MM, Float[][] wCnt, String contractType,
			String searchQueryFilePath) throws JsonProcessingException, IOException, SQLException {

		SearchQuery query = new SearchQuery();
		LegalHttpClient httpCl = new LegalHttpClient(false);
		File file = new File(searchQueryFilePath);
		Scanner input = new Scanner(file);

		while (input.hasNextLine()) {

			String queryString = input.nextLine();
			if (StringUtils.isEmpty(queryString)) {
				continue;
			}

			for (String yearRange : years) {
				query.setWithinLastYears(yearRange);
				query.setContractType(contractType);
				query.setQ(queryString);
				for (Float[] wcnt : wCnt) {
					query.setWordCntRange(wcnt);
					for (String mm : MM) {
						query.setMm(mm);
						String searchApiUrl = "http://35.222.158.51:8081/legalSearch8.6/api/search.json?core="
								+ contractType;
						int start = (int)System.currentTimeMillis();
						String response = httpCl.postContents(searchApiUrl, JSonUtils.object2JsonString(query));
						int end = (int)System.currentTimeMillis();
						System.out.println(end-start);
					}
				}
			}
		}
	}

	public static void main(String[] args) throws JsonProcessingException, IOException, SQLException {

		String[] years = { "2020-2020", "2018-2020", "2015-2020" };

		String[] mm = {
//				"55%"
//				"65%"
				"70%" };

		Float[] wCnt1 = new Float[] { 0.5f, 2.2f };
		Float[] wCnt2 = new Float[] { 0.3f, 3.0f };
		Float[] wCnt3 = new Float[] { 0.3f, 4f };

		Float[][] wCnt = new Float[][] { wCnt1
//			, wCnt2	,wCnt3
		};

		String contractType = "Indentures";
		String searchQueryFilePath = "E:\\testers\\BigLoop\\query\\big_loop_search_queries.txt";

		System.out.println("process Start");
		runSearchQueries(years, mm, wCnt, contractType, searchQueryFilePath);
		System.out.println("process Done");
	}

}