package algo_testers;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.util.CollectionUtil;

import com.fasterxml.jackson.core.JsonProcessingException;

import algo_testers.search_dependencies.AppConfig.SearchAppConfig;
import algo_testers.search_dependencies.AppConfig.SearchConfig;
import algo_testers.search_dependencies.AppConfig.SearchFilterConfig;
import algo_testers.search_dependencies.SearchQuery;
import algo_testers.search_dependencies.SpeedTestReport;
import algo_testers.search_dependencies.WriteLogs;
import edu.stanford.nlp.util.CollectionUtils;
import xbrl.NLP;

public class BigLoop {

	public static void runSearchQueries(String[] years, String[] MM, Float[][] wCnt, String contractType,
			int createNewFileAfter, boolean useContractTypeQuery, String reportFolderpath, String searchQueryFilePath,
			boolean applyExtendedSearchFilter, boolean groupResultUsingGroupByClause_Insteadof_exapandCollapse)
			throws JsonProcessingException, IOException, SQLException {
		SpeedReportGenerator.setContractType(contractType); // set correct contractType in searcher
		setSearchConfigParams(SpeedReportGenerator.searchAppConfig,
				groupResultUsingGroupByClause_Insteadof_exapandCollapse); // override config params, with those set in
																			// this
		// class

		SpeedReportGenerator gs = new SpeedReportGenerator();
		// TODO: when re-running delete prior files??

		SearchQuery query = new SearchQuery();
		query.setIs_broadining(applyBroadening);
		query.setGroupFQ(groupResultUsingGroupByClause_Insteadof_exapandCollapse);

		List<SpeedTestReport> reportlist = new ArrayList<SpeedTestReport>();

		// File file = new File("/home/rsa-key-20210127/big_loop_search_queries.txt");
		File file = new File(searchQueryFilePath);
		// E:\\testers\\Big_Loop\\big_loop_search_queries.txt
		Scanner input = new Scanner(file);

		int count = 0;
		int fileCount = 1;
		int reportCount = 0;
		int queryCount = 0;
		List<String> lines;
		while (input.hasNextLine()) {

			String queryString = input.nextLine();
			if (StringUtils.isEmpty(queryString)) {
				continue;
			}

//			System.out.println("query Before sentence parser=="+queryString);
			lines = fullDocAnalyzer.breakPara("\r\n\r\n" + queryString + "\r\n\r\n");
//			NLP.printListOfString("AFTER breaking int sentences. ===", lines);

			for (String line : lines) { // multi-lines in para
				if (line.split(" ").length < 10) {
					continue;
				}

				queryCount++;
				if (applyExtendedSearchFilter) {
					query = getExtendedParam(query, "Search String " + queryCount, line);
				}

				// TODO: run whatever it is we do when it is keyword search

				for (String yearRange : years) {
					query.setWithinLastYears(yearRange);
					query.setContractType(contractType);
					query.setQ(line);
					query.setContractTypeSearch(useContractTypeQuery);
					// query.setGroupFQ(false);

					for (Float[] wcnt : wCnt) {

						query.setWordCntRange(wcnt);
						for (String mm : MM) {
							query.setMm(mm);
							SpeedTestReport speedTestReport = new SpeedTestReport();
							speedTestReport.setQueryString(line);
							speedTestReport.setContractType(contractType);
							speedTestReport.setYearRange(yearRange);
							speedTestReport.setWcnt("[" + wcnt[0] + "," + wcnt[1] + "]");
							speedTestReport.setMm(mm);
							if (applyBroadening) {
								speedTestReport.setWcnt(Arrays.toString(broadeningWordCntFilterRange));
								speedTestReport.setMm(broadeningMM);
							}

							count++;
							reportCount++;

							speedTestReport.setReportCount(String.valueOf(reportCount));

							// write query to file for jMeter
							String searchApiUrl = "curl -XPOST -H \"content-type:application/json\" https://sandbox.lawhorizonsai.com/legalSearch8.6/api/search.json?core="
									+ contractType;
							String legalSearchAPICall = searchApiUrl + " -d '"
									+ algo_testers.search_dependencies.JSonUtils.object2JsonString(query) + "'";

							String jMeterFileName = "jMeter_Query_" + reportCount + ".txt";
							WriteLogs.deleteOldFile(reportFolderpath + "jMeter_Querys/", jMeterFileName);
							WriteLogs.writeSolrQuery(reportFolderpath + "jMeter_Querys/", jMeterFileName,
									legalSearchAPICall);

							// Solr search
							SpeedTestReport speedTestResponse = gs.searchDo(query, false, speedTestReport);

							if (speedTestResponse.getNumFound() > 3000) {
								speedTestResponse.setReportCount(String.valueOf(reportCount) + " parent");
								reportlist.add(speedTestResponse);

								SpeedTestReport childSpeedReport = new SpeedTestReport();
								childSpeedReport.setQueryString(line);
								childSpeedReport.setContractType(contractType);
								childSpeedReport.setWcnt("[" + wcnt[0] + "," + wcnt[1] + "]");
								childSpeedReport.setMm(mm);
								if (applyBroadening) {
									childSpeedReport.setWcnt(Arrays.toString(broadeningWordCntFilterRange));
									childSpeedReport.setMm(broadeningMM);
								}

								childSpeedReport.setShrinkQuery(true);
								String yearFQ = getShrinkQueryFQ(speedTestResponse.getNumFound(),
										speedTestResponse.getYearCount());
								childSpeedReport.setYearRange(yearFQ.replace("fDate:", ""));
								childSpeedReport.setShrinkDaysFQ(yearFQ);

								SpeedTestReport shrinkSpeedTestResponse = gs.searchDo(query, false, childSpeedReport);
								childSpeedReport.setReportCount(String.valueOf(reportCount) + " child");
								reportlist.add(childSpeedReport);

							} else {
								reportlist.add(speedTestResponse);
							}

							if (count == createNewFileAfter) {
								gs.writeToReport(reportlist, reportFolderpath, fileCount);
								count = 0;
								fileCount++;
								reportlist.clear();
							}
						} // end of mm loop
					} /// end of wCnt loop
				} // end of year loop
			}

		} // end of while loop
		if (reportlist.size() > 0) {
			gs.writeToReport(reportlist, reportFolderpath, fileCount);
		}
	}

	private static String getShrinkQueryFQ(int numFound, int years) {
		double foundDays = numFound / (252 * years);
		int rows = 3000;
		double fudgeFactor = 0.65;
		double queryBusinessDays = (rows * fudgeFactor) / foundDays;
		long queryWeekDays = (long) Math.ceil((queryBusinessDays / 5) * 7);

		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		LocalDate currentDate = LocalDate.now();
		String endDate = currentDate.format(dateFormatter);
		String startDate = currentDate.minusDays(queryWeekDays).format(dateFormatter);

		String fDate = "fDate:[" + startDate + " TO " + endDate + "]";
		return fDate;
	}

	private static SearchQuery getExtendedParam(SearchQuery query, String reportTitle, String searchQuery) {
		JPanel exParamPanel = new JPanel(new GridLayout(8, 2, 5, 5));

		JLabel titile = new JLabel(reportTitle);

		JLabel searchQ = new JLabel(searchQuery);

		JTextArea textArea = new JTextArea(20, 30);
		textArea.setText(searchQuery);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		JScrollPane scrollPane = new JScrollPane(textArea);
		textArea.setEditable(false);

		JLabel withinContractsLable = new JLabel("contractNameAlgo");
		JTextField withinContracts = new JTextField(20);
		withinContracts.setPreferredSize(new Dimension(150, 20));

		JLabel withinPartiesLable = new JLabel("openPara");
		JTextField withinParties = new JTextField(20);

		JLabel keywordLable = new JLabel("Keyword");
		JTextField keyword = new JTextField(20);

		JLabel clousesLable = new JLabel("Clause Name");
		JTextField clouse = new JTextField(20);

		JLabel exhibitsLable = new JLabel("Exhibits");
		JTextField exhibits = new JTextField(20);

		JCheckBox is_broadining = new JCheckBox("Apply broadening");

		exParamPanel.add(keywordLable);
		exParamPanel.add(keyword);
		exParamPanel.add(clousesLable);
		exParamPanel.add(clouse);
		exParamPanel.add(exhibitsLable);
		exParamPanel.add(exhibits);
		exParamPanel.add(withinContractsLable);
		exParamPanel.add(withinContracts);
		exParamPanel.add(withinPartiesLable);
		exParamPanel.add(withinParties);
		exParamPanel.add(is_broadining);

		JPanel qStringPanel = new JPanel(new GridLayout(1, 3, 10, 10));

		qStringPanel.add(titile);
		qStringPanel.add(scrollPane);

		JSplitPane panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, qStringPanel, exParamPanel);

		JOptionPane.showMessageDialog(null, panel, "Extended Param", 3);

		String within_contract = withinContracts.getText();
		String within_parties = withinParties.getText();
		String with_keyword = keyword.getText();
		String with_clouse = clouse.getText();
		String with_exhibits = exhibits.getText();
		boolean broadning = is_broadining.isSelected();

		if (StringUtils.isNoneBlank(within_contract)) {
			query.setWithinContracts(within_contract);
		}

		if (StringUtils.isNoneBlank(within_parties)) {
			query.setWithinParties(within_parties);
		}

		if (StringUtils.isNoneBlank(with_keyword)) {
			query.setKeywords(with_keyword);
		}

		if (StringUtils.isNoneBlank(with_clouse)) {
			query.setClause(with_clouse);
		}

		if (StringUtils.isNoneBlank(with_exhibits)) {
			query.setExh(with_exhibits);
		}
		query.setIs_broadining(broadning);

		return query;
	}

	private static Boolean conformFinalResultsToQuery = false;

//	"search-config":
	private static String searchAgainstField = "hTxt";
	private static Boolean wildcardQueryWords = false;
	private static String wordCountFieldName = "wCnt";

	private static Integer minQHtxtCountToApplyCountRangeFilter = 7;

	private static Boolean applyBroadening = false;
// 		"broadening-config":
	private static Float[] broadeningWordCntFilterRange = { 0.5F, 3.5F };
	private static String broadeningMM = "65%";

//		"raw-query-params":
	private static int rows = 3000;

//	"search-filter-config":
	private static int finalResultsCount = 15;
//		"similarity-config":
	private static Boolean applySimilarity = false;
	private static Integer minSimilarityScore = 70;

	public static void setSearchConfigParams(SearchAppConfig searchAppConfig,
			boolean groupResultUsingGroupByClause_Insteadof_exapandCollapse) {
		searchAppConfig.setConformFinalResultsToQuery(conformFinalResultsToQuery);

		// search configs
		SearchConfig sCfg = searchAppConfig.getSearchConfig();
		sCfg.setSearchAgainstField(searchAgainstField);
		sCfg.setWildcardQueryWords(wildcardQueryWords);
		sCfg.setWordCountFieldName(wordCountFieldName);
		sCfg.setMinQHtxtCountToApplyCountRangeFilter(minQHtxtCountToApplyCountRangeFilter);

		sCfg.getBroadeningConfig().setWordCntFilterRange(broadeningWordCntFilterRange);
		sCfg.getBroadeningConfig().setMm(broadeningMM);

		sCfg.getRawQueryParams().put("rows", rows);

		// filter configs
		SearchFilterConfig fCfg = searchAppConfig.getSearchFilterConfig();
		fCfg.setFinalResultsCount(finalResultsCount);
		fCfg.getSimilarityConfig().setApplySimilarity(applySimilarity);
		fCfg.getSimilarityConfig().setMinSimilarityScore(minSimilarityScore);

		// configs to report
		GolawSearch.searchConfigParams.put("conformFinalResultsToQuery", conformFinalResultsToQuery);
		GolawSearch.searchConfigParams.put("searchAgainstField", searchAgainstField);
		GolawSearch.searchConfigParams.put("wildcardQueryWords", wildcardQueryWords);
		GolawSearch.searchConfigParams.put("wordCountFieldName", wordCountFieldName);
		GolawSearch.searchConfigParams.put("minQHtxtCountToApplyCountRangeFilter",
				minQHtxtCountToApplyCountRangeFilter);
		GolawSearch.searchConfigParams.put("applyBroadening", applyBroadening);
		GolawSearch.searchConfigParams.put("broadeningWordCntFilterRange",
				Arrays.toString(broadeningWordCntFilterRange));
		GolawSearch.searchConfigParams.put("broadeningMM", broadeningMM);
		GolawSearch.searchConfigParams.put("rows", rows);
		GolawSearch.searchConfigParams.put("finalResultsCount", finalResultsCount);
		GolawSearch.searchConfigParams.put("applySimilarity", applySimilarity);
		GolawSearch.searchConfigParams.put("minSimilarityScore", minSimilarityScore);
		if (!groupResultUsingGroupByClause_Insteadof_exapandCollapse) {
			GolawSearch.searchConfigParams.put("Note",
					"Total results in collapse and expand are not reported by solr but are calculated by adding the expanded results plus the collapsed results.");
		}

//		row3000 method getShrinkQueryFQ(
		// TODO - test w/ sim. add sim functionality. and when sim==true, report score.
	}

	public static void main(String[] args) throws JsonProcessingException, IOException, SQLException {

		/*
		 * NOTE: READ THIS--------> Once testing is done and code is to be promoted have
		 * Mythiraye test by mirroring query text big loop uses on web/word api AFTER
		 * this local -
		 * \contract-analysis\Linc_Downloader_test\resources\app-config.json is
		 * conformed to linux server. Mythiraye then compares the final results on front
		 * end against the reports generated by this BigLoop method
		 */

		// Praveen TODO: 1. Allow var setting of # of query results to include in report
		// html and 2. see other config params in app-config.json
		// Praveen ---> Keshav TODO: put sim api on dell laptop (and Lenovo laptop)

		String[] years = {
//				"2020-2020",
				"2018-2020"
//				,"2015-2020"
//				, 
//				"2020-2020"
		};// "startYr-endYr"
		String[] mm = {
//				"55%"
				"65%"
//				"70%" 
		};

		Float[] wCnt1 = new Float[] { 0.5f, 2.2f };
		Float[] wCnt2 = new Float[] { 0.3f, 3.0f };
		Float[] wCnt3 = new Float[] { 0.3f, 4f };

		Float[][] wCnt = new Float[][] { wCnt1
//			, 
//			wCnt2
//			,
//			wCnt3
		};

		String contractType = "Indentures";
		/*
		 * contractType NOTE1: in order to query w/o any IDs set contractType to empty.
		 * To query contractNameAlgo input a name that doesn't exist as a query pool
		 * contract type.
		 */

		int createNewFileAfter = 10;// writes report after this many queries
		boolean queryOnContractType_InsteadOfIDPool = false;// if to not have any IDs at all.
		String reportFolderpath = "E:\\testers\\BigLoop\\report\\";
		String searchQueryFilePath = "E:\\testers\\BigLoop\\query\\big_loop_search_queries.txt";

		/*
		 * Transmission speed to the user isn't currently captured, so times may feel
		 * slower. TODO: measure and record estimate speed in report.
		 */

		System.out.println("process Start");

		boolean applyExtendedSearchFilter = false;
		boolean groupResultUsingGroupByClause_Insteadof_exapandCollapse = false;

		int dialogResult = JOptionPane.showConfirmDialog(null, "Run querys with extended search filters?", "Warning",
				JOptionPane.YES_NO_OPTION);
		if (dialogResult == JOptionPane.YES_OPTION) {
			applyExtendedSearchFilter = true;
		} else {
			applyExtendedSearchFilter = false;
		}

		runSearchQueries(years, mm, wCnt, contractType, createNewFileAfter, queryOnContractType_InsteadOfIDPool,
				reportFolderpath, searchQueryFilePath, applyExtendedSearchFilter,
				groupResultUsingGroupByClause_Insteadof_exapandCollapse);
		System.out.println("process Done");

		// FIX: SIM API
		// NOTE: getShrinkQueryFQ( == ROW3000 method
	}
}
