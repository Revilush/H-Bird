package algo_testers;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import algo_testers.search_dependencies.SearchQuery;
import algo_testers.search_dependencies.SpeedTestReport;
import algo_testers.search_dependencies.WriteLogs;

public class SpeedReportGenerator  extends GolawSearch {
	

	public static void main(String[] args) throws IOException, SQLException {
        List<SpeedTestReport> reportlist = new ArrayList<SpeedTestReport>();
        
        String queryString = "The Trustee in its individual or any other capacity may become the owner or pledgee of Notes and may otherwise deal with the Company or its Affiliates with the same rights it would have if it were not Trustee. Any Paying Agent or Registrar may do the same with like rights. However, the Trustee must comply with Sections 7.10 and 7.11.";
    	String yearRange = "2018-2020";
    	Float[] wordCount = new Float[] {0.5f,2.5f};
    	String  mm = "60%";
		
		File file = new File("F:/deepak/search_queries.txt");
		Scanner input = new Scanner(file);
		while (input.hasNextLine()) {
			queryString = input.nextLine();
			 
			SpeedReportGenerator gs = new SpeedReportGenerator();
			SearchQuery searchQuery = new SearchQuery();
			searchQuery.setQ(queryString);
			searchQuery.setContractType(contractType);
			searchQuery.setWithinLastYears(yearRange);
			searchQuery.setWordCntRange(wordCount);
			searchQuery.setMm(mm);
			 
			SpeedTestReport speedTestReport = new SpeedTestReport();
			 speedTestReport.setQueryString(queryString);
			 speedTestReport.setContractType(contractType);
			 speedTestReport.setYearRange(yearRange);
			 speedTestReport.setWcnt("["+wordCount[0]+","+wordCount[1]+"]");
			 speedTestReport.setMm(mm);
			    
			 reportlist.add(gs.searchDo(searchQuery, false, speedTestReport));
		  }
		
		writeToReport(reportlist);
		System.out.print("Process Done");
		
	}
}