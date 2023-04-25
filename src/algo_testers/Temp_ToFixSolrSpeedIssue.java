package algo_testers;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

import algo_testers.search_dependencies.SolrCollapsedExpandedResults;
import algo_testers.search_dependencies.SolrJClient;
import contracts.GoLaw;
import xbrl.Utils;

public class Temp_ToFixSolrSpeedIssue {

	public static void main(String[] arg) throws IOException, SQLException, SolrServerException, ParseException {
		// TODO -- put params just as I would in solr jetty
		GoLaw gl = new GoLaw();

		/* HERE IS FORMULA:
		 * if hTxt<min (7), don't use hTxt for anything. 
		 * if wCnt>4 and less than 10, then mm=80, else 70% (wCnt>10)
		 * if wCnt<=4 then proximity (100%). and fDate 1 year
		 * 
		 * ALWAYS only typ=3 
		 * ALWAYS wCnt range is 10 to 120
		 * ALWAYS txt is q
		 */
		String query = Utils.readTextFromFile("c:\\temp\\query.txt");

		boolean yesMM = true;
		int wCnt = query.split(" ").length;

		String wCntRange = "wCnt:[10 TO 120]";// leave static
		if (wCnt <= 4) {
			System.out.println("***proximity magic*****");
			query = "\"" + query + "\"~10";
			yesMM = false;
		}

		Map<String, String> otherParams = new HashMap<>();
		if (yesMM) {
			if (wCnt > 10 && wCnt <= 16)
				otherParams.put("mm", "70%");
			if (wCnt <= 10)
				otherParams.put("mm", "80%");
		}

		String fDate = "";// alwaysuse fDate last 12 mos
		if (wCnt <= 4) {
			fDate = "fDate:[20200601 TO 20210601]";

		}
		String[] fqs = { wCntRange, fDate, "typ:3" }; // always just typ=2

		String q = "txt:(" + query + ")"; //always txt

		fireQuery2Solr(q, fqs, otherParams);

	}

	private static void fireQuery2Solr(String q, String[] fqs, Map<String, String> otherParams)
			throws SolrServerException, IOException {
		String solrServer = "http://34.123.194.69:8984/solr/";
		String core = "fin_2010_2020";
		String collapseField = "hashTxtId";

		String queryText = "\"Attributable Liens\" means in connection with a sale and lease-back transaction the lesser of:";

		long start = System.currentTimeMillis();
		SolrJClient solrClient = new SolrJClient(solrServer, core);

		fqs = (null == fqs) ? new String[0] : fqs;
		// collapse field
		fqs = ArrayUtils.add(fqs, "{!collapse field=" + collapseField + "}");

		// params
		otherParams = (null == otherParams) ? new HashMap<>() : otherParams;
		otherParams.put("expand", "true");
		otherParams.put("expand.rows", "0");
		otherParams.put("rows", "3000");
		otherParams.put("defType", "edismax");
		otherParams.put("fl", "id,txt,hTxt,score,def,exh,sec,hashTxtId,hashHtxtId");

		String[] fields2Return = null;
		SolrQuery solrQuery = solrClient.getSolrQuery(q, fqs, fields2Return, otherParams);
		System.out.println(solrServer + core + "/select?" + solrQuery);

		// fire the solr query, and get results
		QueryResponse resp = solrClient.search(solrQuery);

		// aggregate CE results
		SolrCollapsedExpandedResults collapsedresults = new SolrCollapsedExpandedResults(solrQuery)
				.setNumFoundFieldName("txtCnt").setResultConsolidationRequired(true);
		collapsedresults.parseCollapsedExpandedResults(resp);

		// List<SolrDocument> allGroupedDocs =
		// collapsedresults.getCollapsedExpandedResults();
		long totalRecs = collapsedresults.getTotalNumFound();

		System.out.println("total results: " + totalRecs + ", millis : " + (System.currentTimeMillis() - start));
	}

}
