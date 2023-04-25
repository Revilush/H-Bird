package algo_testers.search_dependencies;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.deser.DataFormatReaders.Match;

import algo_testers.GolawSearch;
import algo_testers.dependencies.SynonymApplier;
import algo_testers.dependencies.TextDiffHelper.DiffDetail;
import algo_testers.search_dependencies.AppConfig.MLConfig;
import algo_testers.search_dependencies.AppConfig.SearchAppConfig;
import algo_testers.search_dependencies.AppConfig.SearchConfig;
import algo_testers.search_dependencies.USESimilarityResponse.MatchResult;
import contracts.GoLaw;
public class SearchFilterHelper extends BaseHelper {

	protected static Log log = LogFactory.getLog(SearchFilterHelper.class);

	private AppConfig.SearchFilterConfig sfCfg; //add val
	private SearchConfig searchConfig; //add val

	private List<List<String>> synonyms;
	private SynonymApplier synonymApplier = null;
	private MLConfig mlApiConfig; // add val
	private MLApi mlApi = null;
	private SearchQuery query;
	
	private int finalResultsCount = 0;
	private long totalLiteralTxtCount = -1;
	private boolean similarityApiApplied = false;
	private GolawSearch searchHelper;
	private SpeedTestReport speedTestReport;

	public SearchFilterHelper(SearchAppConfig searchConfigContainer, SearchQuery query, GolawSearch golawSearch, MLConfig mlApiConfig, SpeedTestReport speedTestReport) {
		super(searchConfigContainer);
		this.speedTestReport = speedTestReport;
		this.query = query;		
		finalResultsCount = query.getFinalResultsCount();
		
		sfCfg = this.searchConfigContainer.getSearchFilterConfig();
		searchConfig = this.searchConfigContainer.getSearchConfig();
		
		if(finalResultsCount == 0) {
			   finalResultsCount = sfCfg.getFinalResultsCount();
		}

		this.mlApiConfig = mlApiConfig;
		mlApi = new MLApi(mlApiConfig.getBaseServerUrl());

		this.synonyms = SynonymController.getSynonyms(searchConfigContainer, false);
		if (null != this.synonyms && this.synonyms.size() > 0) {
			synonymApplier = new SynonymApplier();
			synonymApplier.setSynonymsList(synonyms);
		}
		
		this.searchHelper = golawSearch;
	}
	
	public List<SearchResultsGroupSummary> filter(List<SolrDocument> docs) throws IOException {
		if (null == docs || docs.size() == 0)
			return null;
		long startMilis = System.currentTimeMillis();

		// Merge docs with same score - Merge:= add txtCnt to another one (with higher
		// txtCnt) and discard one doc.
		// Also keep only upto docs that have acceptable scores - discard ones with
		// lower score
		/* - tesing with LInc to see if not merging by solr score helps getting more results in later stages of the process.
		mergeSameScoreDocs_AndDiscardLowScoreDocs(docs);
		writeAuditInfo("search_results-min-score.txt",
				"Total docs - after same-score-merged and having desired min score:" + docs.size(),
				"\r\n\r\n---------------\r\n Picked docs:", docs);
		*/
		if (log.isInfoEnabled())
			log.info("Records after merge-same-score & discard-low-score :" + docs.size() +", "+System.currentTimeMillis());

		// keep top/1st result
		// SolrDocument topResultDoc = docs.get(0);
		// float maxScore = getDocScore(topResultDoc);

		// group the docs based on their hTxtId
		List<SearchResultsGroupSummary> groups = null;
		
		if (searchHelper.getHeadingOnlySearch()) {
			groups = groupDocsByHashHdgId(docs, searchHelper.getHeadingFieldName());
		} else {
			speedTestReport.setBeforeGroupHtxtCnt(docs.size());
			groups = groupDocsByHTxtId(docs);
			speedTestReport.setAfterGroupHtxtCnt(groups.size());
		}
		
		// write audit info to file
		writeAuditInfo("search_results_htxtId_groups.txt", "Similar hTxtId groups:" + groups.size(), "\r\n", groups);

		// sort this list of group by results-count. Will also help in picking top-x
		// results if similarity test was done.
		// DESC order - max counts on top
		Collections.sort(groups, new SearchResultsGroupSummary.SortByResultsCount(-1));
		
		// Now we have our results sorted by txtCnt - highest counts on top.
		
		// calculate share of each group's docs' count against total records
		setGroupsPercentOfTotal(groups, totalLiteralTxtCount);
		
		if (log.isInfoEnabled())
			log.info("groups made after merge on hTxtId and sort on txtCnt:" + groups.size() +", "+System.currentTimeMillis());

		// pick top x results to further filter by similarity if needed
		Float factor = sfCfg.getFactorToPickResultsB4ApplyingSim();
		List<SearchResultsGroupSummary> groups2Sim = new ArrayList<>();
		
		if (null != factor && factor > 0) {
			groups2Sim = pickFinalResultsFromTop(groups, groups2Sim, (int) (finalResultsCount * factor));
		} else {
			groups2Sim.addAll(groups);
		}
		
		if (log.isInfoEnabled())
			log.info("groups picked to apply sim etc:" + groups2Sim.size() +", "+System.currentTimeMillis());

		// filter by similarity - only if client sent is of sufficient length (ie
		// similarity won't apply when query is smaller ie "prudent person").
		//String hTxt = Utilities.tokenized_def_stop_stem(query.getQ());
		GoLaw gl = new GoLaw();
		String hTxt = gl.goLawGetHtxt(query.getQ());
		// do not use sim-api when: broadening, false in config, or not enough words in q
		if (!query.getIs_broadining()  &&  sfCfg.getSimilarityConfig().getApplySimilarity()  &&  hTxt.split(" ").length >= searchConfig.getMinQHtxtCountToApplyCountRangeFilter()) {
			similarityApiApplied = true;
			int timeBeforeSim = (int)System.currentTimeMillis();
			groups2Sim = filterBySimilarity(groups2Sim);
			int timeAfterSim = (int)System.currentTimeMillis();
			speedTestReport.setSimTime(timeAfterSim - timeBeforeSim);
			/*
			 * writeAuditInfo("search_results_post_similarity.txt", "similarity applied:" +
			 * groups2Sim.size(), "\r\n", getGroupMaps(groups2Sim));
			 */
			if (log.isInfoEnabled())
				log.info("(just bef applying syn) after applying similarity:" + groups2Sim.size() +", "+System.currentTimeMillis());
		} else {
			log.info("similarity api was not applied: isApplySim="+(sfCfg.getSimilarityConfig().getApplySimilarity()) +", hTxt="+ hTxt +", "+System.currentTimeMillis());
		}

		// So far groups were made using pre-calculated hTxtId, lets apply synonyms to
		// groups' text and re-calculate hTxt and Re-Group again on newly found hTxt.
		// 26-6-20: commented: syn-application and hTxt calculation was taking more
		// time, thus commented for now. We may want to put syn'd hTxt itself in solr.
		//24-Jul: post cutting required results, apply syns
		// syns-application is still controlled by config. 
		if (searchConfigContainer.isApplySynonyms()) {
			// set synonymsApplier to each group
			int timeBeforeSyn = (int)System.currentTimeMillis();
			if (null != synonymApplier) {
				for (SearchResultsGroupSummary grup : groups2Sim) {
					grup.setSynonymsApplier(synonymApplier);
				}
			}
			groups2Sim = reConsolidateGroupsByHomogText(groups2Sim, true);
			if (log.isInfoEnabled())
				log.info("sents/grps after applying synonyms and re-cdn:" + groups2Sim.size() +", "+System.currentTimeMillis());
			writeAuditInfo("search_results_syn_applied_regrouped.txt",
					"synonyms applied and re-consolidated:" + groups2Sim.size(), "\r\n", getGroupMaps(groups2Sim));
			
			// re-calculate the share% since some sents might have been merged (their
			// sent-Cnt were summed but share% was not changed). The denominator (total txtCnt) will not
			// change but the re-consolidated result will have a new numerator (txtCnt).
			setGroupsPercentOfTotal(groups2Sim, totalLiteralTxtCount);
			int timeAfterSim = (int)System.currentTimeMillis();
			speedTestReport.setSynonymTime(timeAfterSim - timeBeforeSyn);
		}

		int finalResultsCount2Pick = finalResultsCount;
		if (searchConfigContainer.isConformFinalResultsToQuery()) {
			finalResultsCount2Pick = (int) (finalResultsCount * factor);
		}
		// pick desired groups from top
		List<SearchResultsGroupSummary> finalResults = new ArrayList<>();
		
		if (similarityApiApplied) {
			/*
			1. remove those w/ low txt cnt% below var=%. Total=3309. var=0.1% which is 0.001*3309 = 3.3 - or if txt cnt below 3 then discard.
			2. take top txt cnt (var#) - top 3 txt cnt results (as you have now).
			3. for scores above 85 (var#) take top txtCnts (if score >85, top txt cnt wins)
			4. if this doesn't fill required results # - go and take remaining based on sim score -- start from 100 and going down till result # is satisfied.
			*/
			finalResults = pickFinalResults_TopTxtCntWithSimScores(groups2Sim, finalResults, finalResultsCount2Pick);
			speedTestReport.setSimResult(groups2Sim.size() - finalResults.size());
			speedTestReport.setSimilarityApiApplied(true);
		} else {
			// sort again based on txtCnt since syn/re-grouping might have changed the order
			//, or the order changes after the similarity application (becomes order by sim-score)
			// DESC order - max txt counts on top
			Collections.sort(groups2Sim, new SearchResultsGroupSummary.SortByResultsCount(-1));
			finalResults = pickFinalResultsFromTop(groups2Sim, finalResults, finalResultsCount2Pick);
		}

		// sort final docs by desired field
		String finalSortBy = sfCfg.getFinalSortBy();		//"txtCnt/simScore/diffCnt"
		sortFinalDocs(finalResults, query.getQ());
		
		if (log.isDebugEnabled())
			log.debug("Results finally sorted by: " + finalSortBy +", "+System.currentTimeMillis());
		
		// TODO: b/c in searchUI we aren't trying to be really precise we don't need to
		// run both sim AND dif meas - it will be one or the other or none. Right now it
		// looks good with sim - but I don't know clock speed. Note for word api we may
		// use one or the other or both but in searchUI we have concerns re speed and so
		// lets go with 1 or other as noted.
		if (searchConfigContainer.isConformFinalResultsToQuery()  &&  StringUtils.isNotBlank(hTxt) ) {
			// re-adjust data/variation sents as per GoLawDiff redliner
			long start = System.currentTimeMillis();
			String clientSent = query.getQ();
			TextDiffHelper tdh = new GoLawDiff(false, true);
			List<algo_testers.search_dependencies.TextDiffHelper.DiffDetail> diffs;
			for (SearchResultsGroupSummary srgs : finalResults) {
				diffs = tdh.getDiff(clientSent, srgs.getText());
				srgs.setText2Deliver(tdh.getFinalPlainText(diffs));			// set it as final text to deliver
				String hashHtxtId = (String) srgs.getDocs().get(0).get("hashHtxtId");
				srgs.sethTxtId(hashHtxtId);
			}
			
			if (log.isDebugEnabled())
				log.debug("GoLawDiff: conforming results to query - millis taken: " + (System.currentTimeMillis() - start) + ",  results# " + finalResults.size() +", "+System.currentTimeMillis());

			// merge again if possible, based on hTxt (NOT applying synonyms now)
			start = System.currentTimeMillis();
			// re-calcu;ate homog texts
			for (SearchResultsGroupSummary grup : finalResults) {
				grup.setHomogText(grup.getHomogenizedText(grup.getText2Deliver()));
			}
			// regroup and re-assess share %
			finalResults = reConsolidateGroupsByHomogText(finalResults, false);
			setGroupsPercentOfTotal(finalResults, totalLiteralTxtCount);
			// restore original texts of variations
			for (SearchResultsGroupSummary srgs : finalResults) {
				srgs.useMostOccurredText();
			}
			
			List<SearchResultsGroupSummary> results2return = new ArrayList<>();
			// pick final number of results from top now
			results2return = pickFinalResultsFromTop(finalResults, results2return, finalResultsCount);
			int numberOfResultsChanged = finalResults.size() - results2return.size();
			
			finalResults = results2return;		// re-assign so further/later code can take same
			// sort final docs again by desired field
			sortFinalDocs(finalResults, query.getQ());
			if (log.isDebugEnabled())
				log.debug("GoLawDiff: conformed results to query and re-merged - millis taken: " + (System.currentTimeMillis() - start) + ",  final-results# " + finalResults.size() +", "+System.currentTimeMillis());
		}
		
		// check if any results are from same 'para' (ie we get a para and a sent in
				// that same para in final results).
				// if so, need to pick only the result that has higher sim-score to client sent
				
				if (null != finalResults && finalResults.size() > 0 ) {
					int timeBeforePara = (int)System.currentTimeMillis();
					String[] idParts;
					String paraId;
					Map<String, List<SearchResultsGroupSummary>> paraIds2ResultsMap = new LinkedHashMap<>();
					List<SearchResultsGroupSummary> sameParaResults;
					for (SearchResultsGroupSummary srgs : finalResults) {
						/*
						 * NOTE FOR KESHAV ?;if first 20 chars/digits of ID are same - then same filing,
						 * if all are same up to first _ then same kId, if same till 2nd _ same para or
						 * if para Id is contained in sentence Id it is same para
						 */
						// group results by their paraId - groups with same paraId come together
						idParts = srgs.getId().split("_");
						paraId = idParts[0] + "_" + idParts[1] + "_" + idParts[2] + "_" + idParts[3];
						sameParaResults = paraIds2ResultsMap.get(paraId);
						if (null == sameParaResults) {
							sameParaResults = new ArrayList<>();
							paraIds2ResultsMap.put(paraId, sameParaResults);
						}
						sameParaResults.add(srgs);
					}
					List<SearchResultsGroupSummary> uniqParaIdResults = new ArrayList<>();
					List<String> tableContent = new ArrayList<String>();
					for (List<SearchResultsGroupSummary> resultsWithSameParaId : paraIds2ResultsMap.values()) {
						if (resultsWithSameParaId.size() > 1) {
							// TODO: we found multiple results having same paraId - collect them and run
							// simi-api
							tableContent.add("<tr><td style=\"border:1px solid black;\">"+resultsWithSameParaId.get(0)+"</td> <td style=\"border:1px solid black;\">"+resultsWithSameParaId.size()+"</td> </tr>");
							uniqParaIdResults.add(getClosestGroup(query.getQ(), resultsWithSameParaId));
						} else {
							uniqParaIdResults.add(resultsWithSameParaId.get(0));
						}
					}
					
					// put ths unique list into 'finalResults' as this variable is used later
					finalResults = uniqParaIdResults;
					int timeAfterPara = (int)System.currentTimeMillis();
					speedTestReport.setResultFromParaTime(timeAfterPara - timeBeforePara); 
					
				}
		
		// set Meta-Data (edgarLink, .... etc for results)
		if (null != finalResults  &&  finalResults.size() > 0) {
			
			Map<String, SolrDocument> doc0KId2KMap = null;
			if (null != searchHelper.getDoc0KId2KMap()  &&  searchHelper.getDoc0KId2KMap().size() > 0) {
				doc0KId2KMap = searchHelper.getDoc0KId2KMap();
			} else {
				// search all doc0 based on kIds of top-doc-in-group
				Set<String> kIds = new HashSet<>();
				String[] idParts;
				for (SearchResultsGroupSummary srgs : finalResults) {
					idParts = SolrUtils.getSolrDocFieldAsString(srgs.getFinalPickedDoc(), "id").split("_");
					kIds.add(idParts[0]+"_"+idParts[1]);
				}
				try {
					int timeBeforeKid = (int)System.currentTimeMillis();
					doc0KId2KMap = searchHelper.searchDoc0ForKIds(kIds, speedTestReport);
					int timeAfterKid = (int)System.currentTimeMillis();
					speedTestReport.setEdgarLinkQueryTime(timeAfterKid - timeBeforeKid);
				} catch (SolrServerException | IOException e) {
					log.error("Error while getting doc0kId in filter : ", e);
					doc0KId2KMap = null;
				}
			}
			
			if (null != doc0KId2KMap) {
				String[] metaFields = {"edgarLink"};
				String[] idParts;
				for (SearchResultsGroupSummary srgs : finalResults) {
					idParts = SolrUtils.getSolrDocFieldAsString(srgs.getFinalPickedDoc(), "id").split("_");
					srgs.setMetaData(doc0KId2KMap.get(idParts[0]+"_"+idParts[1]), metaFields);
					// another field from doc as meta-data
					srgs.setMetaData(null, "sec");
					srgs.setMetaData(null, "exh");
					srgs.setMetaData(null, "def");
				}
			}
		}
		
		// calculate hTxt words to corresponding orgWord+Idx
		try {
			createHtxtWordToOrgWordMap(finalResults);
		} catch (FileNotFoundException e) {
			log.warn("", e);
		}

		if (log.isInfoEnabled())
			log.info("total millis taken in filtering search results: " + (System.currentTimeMillis() - startMilis)
					+ "total results returned: " + finalResults.size() +", "+System.currentTimeMillis());
		searchHelper.setDoc0KId2KMap(null);
		return finalResults;
	}
	
	private SearchResultsGroupSummary getClosestGroup(String clientText,
			List<SearchResultsGroupSummary> resultsWithSameParaId) throws IOException {

		Map<String, String> id2TextMap = new LinkedHashMap<>();
		int index2Return = 0;
		for (int k = 0; k < resultsWithSameParaId.size(); k++) {
			id2TextMap.put(resultsWithSameParaId.get(k).getId(), resultsWithSameParaId.get(k).getText());
		}
		USESimilarityRequest request = new USESimilarityRequest();
		request.setList1(new ArrayList<String>(Arrays.asList(query.getQ()))); // list1: user's text/sent
		request.setList2(id2TextMap); // list2: groups' texts
		request.setMinScore(sfCfg.getSimilarityConfig().getMinSimilarityScore()); // min similarity score
		String apiResp = mlApi.getUSESimilarity(request);

		USESimilarityResponse simResponse = JSonUtils.json2Object(apiResp, USESimilarityResponse.class);
		String srgsID = simResponse.getMatchResults().get(0).getMatches().get(0).getId();
		float simScore = simResponse.getMeta().getMaxScore();
		
		for (int k = 0; k < resultsWithSameParaId.size(); k++) {
			if (resultsWithSameParaId.get(k).getId().equals(srgsID)) {
				index2Return = k;
				break;
			}
		}

		SearchResultsGroupSummary  doc =  resultsWithSameParaId.get(index2Return);
		doc.setSimScore(simScore);
		return doc;
	}

	
	private void sortFinalDocs(List<SearchResultsGroupSummary> finalResults, String query) throws JsonProcessingException {
		String finalSortBy = sfCfg.getFinalSortBy();		//"txtCnt/simScore/diffCnt"
		/*
		 * if (StringUtils.equalsIgnoreCase(finalSortBy, "simScore")) { //IMP: if
		 * similarity was not applied and final-sort is given as sim-score, it'll sort
		 * by solr-score Collections.sort(finalResults, new
		 * SearchResultsGroupSummary.SortByAvgScore(-1)); } else
		 */ if (StringUtils.equalsIgnoreCase(finalSortBy, "diffCnt")) {
			// sort by dif-count against client-sent
			finalResults = sortByDiffCountAgainstUserSent(query, finalResults);
		} else if (StringUtils.equalsIgnoreCase(finalSortBy, "txtCnt")) {
			// the results are already sorted by txtCnt - no need to do anything more 
			Collections.sort(finalResults, new SearchResultsGroupSummary.SortByResultsCount(-1));
		}
	}

	
	
	/**
	 * Merges solr docs that have same score. Merge means adding txtCnt and
	 * discarding one that has lower txtCnt. Also discards docs with low score (less
	 * than certain number - config: "cut-off-percent-score-againt-top-solr-score")
	 * 
	 * @param docs
	 * @return
	 */
	@SuppressWarnings("unused")
	private List<SolrDocument> mergeSameScoreDocs_AndDiscardLowScoreDocs(List<SolrDocument> docs) {
		SolrDocument sd, prvSd;
		long tc, prvTc;
		float score, prvScore, maxSolrScore = SolrUtils.getSolrDocFieldAsFloat(docs.get(0), SolrFieldName_Score);
		float minSolrScoreDesired = (maxSolrScore * sfCfg.getCutOffPercentScoreAgaintTopSolrScore()) / 100;

		for (int del, i = 1; i < docs.size(); i++) {
			prvSd = docs.get(i - 1);
			sd = docs.get(i);
			score = SolrUtils.getSolrDocFieldAsFloat(sd, SolrFieldName_Score);
			if (score < minSolrScoreDesired) {
				// very low score doc found - delete rest of docs from list and quit
				List<SolrDocument> docs2Del = docs.subList(i, docs.size());
				log.debug("results discarded due to less-than desired score (" + minSolrScoreDesired + "):"
						+ docs2Del.size() +", "+System.currentTimeMillis());
				docs2Del.clear();
				break;
			}
			// compare against last doc's score
			prvScore = SolrUtils.getSolrDocFieldAsFloat(prvSd, SolrFieldName_Score);
			if (score == prvScore) {
				// found same score docs - merge
				prvTc = (long) prvSd.getFirstValue(solrField_txtCnt);
				tc = (long) sd.getFirstValue(solrField_txtCnt);
				if (prvTc > tc) {
					// last doc has bigger txtCnt - keep it and add new txtCnt and delete new doc
					del = i;
					prvSd.setField(solrField_txtCnt, prvTc + tc);
				} else {
					// this doc has bigger txtCnt - keep it and add old txtCnt and delete old doc
					del = i - 1;
					sd.setField(solrField_txtCnt, prvTc + tc);
				}
				docs.remove(del);
				i--;
			}
		}
		return docs;
	}

	/**
	 * 
	 * @param docs
	 * @return
	 */
	private List<SearchResultsGroupSummary> groupDocsByHTxtId(List<SolrDocument> docs) {
		// group the docs based on their hTxtId
		long start = System.currentTimeMillis();
		Map<String, SearchResultsGroupSummary> hTxtId2GroupMap = new LinkedHashMap<>();
		String hTxtId;
		// when hashHtxtId field is of string type in solr
		//Map<String, SearchResultsGroupSummary> hTxtId2GroupMap = new LinkedHashMap<>();
		//String hTxtId;
		SearchResultsGroupSummary grup;
		for (SolrDocument sd : docs) {
			hTxtId = (String) sd.getFirstValue(solrField_hashHtxtId);
			//hTxtId = (String) sd.getFirstValue(solrField_hashHtxtId);
			grup = hTxtId2GroupMap.get(hTxtId);
			if (null == grup) {
				// put a new group
				grup = new SearchResultsGroupSummary(sd);
				hTxtId2GroupMap.put(hTxtId, grup);
			} else {
				// add doc to existing group
				grup.add2Group(sd);
			}
		}
		List<SearchResultsGroupSummary> groups = new ArrayList<>(hTxtId2GroupMap.values());
		if (log.isInfoEnabled())
			log.info("SearchFilterHelper.filter(): grouped docs w/ similar hTxtId in millis:"
					+ (System.currentTimeMillis() - start) +", "+System.currentTimeMillis());
		return groups;
	}
	
	private List<SearchResultsGroupSummary> groupDocsByHashHdgId(List<SolrDocument> docs, String solrFieldName) {
		// group the docs based on their hTxtId
		long start = System.currentTimeMillis();
		Map<Long, SearchResultsGroupSummary> HashHdgId2GroupMap = new LinkedHashMap<>();
		List<Long> hashHdgId;
		List<String> hdgOrd;
		String solrFieldValue;
		int hdgIdx;
		SearchResultsGroupSummary grup;
		for (SolrDocument sd : docs) {
			solrFieldValue = (String) sd.get(solrFieldName);
			if(StringUtils.isNotBlank(solrFieldValue)) {
				hashHdgId = (List<Long>)sd.get(solrField_hashHdgId);
				hdgOrd = (List<String>)sd.get(solrField_hdgOrd);
				Long thisSolrFieldHTxtHashValue;
				if(hashHdgId == null || hdgOrd == null) {
					GoLaw gl = new GoLaw();
					solrFieldValue = gl.goLawGetHtxt(solrFieldValue.toLowerCase());
					solrFieldValue = StringUtils.join(solrFieldValue.split(" "), "* ").trim() + "*";
					solrFieldValue = solrFieldValue.toUpperCase();
					thisSolrFieldHTxtHashValue = (long) solrFieldValue.hashCode();
				}
				else {
					hdgIdx = hdgOrd.indexOf(solrFieldName);
					if (hdgIdx < 0)		// if doc does not have heading field, ignore this doc. Ideally this should not happen
						continue;
					
					thisSolrFieldHTxtHashValue = hashHdgId.get(hdgIdx);
					
				}
				grup = HashHdgId2GroupMap.get(thisSolrFieldHTxtHashValue);
				if (null == grup) {
					// put a new group
					grup = new SearchResultsGroupSummary(sd);
					HashHdgId2GroupMap.put(thisSolrFieldHTxtHashValue, grup);
				} else {
					// add doc to existing group
					grup.add2GroupOnDocField(sd, solrFieldName);
				}	
			}
		}
		List<SearchResultsGroupSummary> groups = new ArrayList<>(HashHdgId2GroupMap.values());
		if (log.isInfoEnabled())
			log.info("SearchFilterHelper.filter(): grouped docs w/ similar hTxtId in millis:"
					+ (System.currentTimeMillis() - start) +", "+System.currentTimeMillis());
		return groups;
	}

	/**
	 * Applies synonyms to all groups' literal text, re-calculate hTxt and re-group
	 * (and merge as needed) groups based on newly calculated hTxt now.
	 * 
	 * @param groups
	 * @return
	 */
	private List<SearchResultsGroupSummary> reConsolidateGroupsByHomogText(List<SearchResultsGroupSummary> groups,
			boolean applySynonymsAndReCalculateHtxt) {
		long startMillis = System.currentTimeMillis();
		Map<String, List<SearchResultsGroupSummary>> hTxt2GroupsMap = new LinkedHashMap<>();
		List<SearchResultsGroupSummary> grupList;
		for (SearchResultsGroupSummary grup : groups) {
			if (applySynonymsAndReCalculateHtxt)
				grup.applySynonymsAndCalculateHtxt();
			grupList = hTxt2GroupsMap.get(grup.getHomogText());
			if (null == grupList) {
				grupList = new ArrayList<>();
				hTxt2GroupsMap.put(grup.getHomogText(), grupList);
			}
			grupList.add(grup);
		}
		List<SearchResultsGroupSummary> respGroups = new ArrayList<>();
		int i = 1;
		for (List<SearchResultsGroupSummary> grups2Merge : hTxt2GroupsMap.values()) {
			SearchResultsGroupSummary grup = grups2Merge.get(0);
			for (i = 1; i < grups2Merge.size(); i++)
				grup.merge(grups2Merge.get(i));
			respGroups.add(grup);
		}
		if (log.isInfoEnabled())
			log.info("Synonyms " + (applySynonymsAndReCalculateHtxt ? "applied" : "Not applied")
					+ " and re-grouped - in millis:" + (System.currentTimeMillis() - startMillis) +", "+System.currentTimeMillis());
		return respGroups;
	}

	private List<SearchResultsGroupSummary> pickFinalResultsFromTop(List<SearchResultsGroupSummary> groups,
			List<SearchResultsGroupSummary> finalResults, int finalResultsCount) {
		// now pick top 'x' results
		for (SearchResultsGroupSummary grup : groups) {
			finalResults.add(grup);
			if (finalResults.size() >= finalResultsCount)
				break;
		}
		return finalResults;
	}
	
	private List<SearchResultsGroupSummary> pickFinalResults_TopTxtCntWithSimScores(List<SearchResultsGroupSummary> groups,
			List<SearchResultsGroupSummary> finalResults, int finalResultsCount) {
		/*
		1. remove those w/ low txt cnt% below var=%. Total=3309. var=0.1% which is 0.001*3309 = 3.3 - or if txt cnt below 3 then discard.
		2. take top txt cnt (var#) - top 3 txt cnt results (as you have now).
		3. for scores above 85 (var#) take top txtCnts (if score >85, top txt cnt wins)
		4. if this doesn't fill required results # - go and take remaining based on sim score -- start from 100 and going down till result # is satisfied.
		*/
		
		List<SearchResultsGroupSummary> sourceGroups = new ArrayList<>(groups);
		Collections.sort(sourceGroups, new SearchResultsGroupSummary.SortByResultsCount(-1));

		SearchResultsGroupSummary grup;
		// 1. remove those w/ low txt cnt% below var=%. Total=3309. var=0.1% which is 0.001*3309 = 3.3 - or if txt cnt below 3 then discard.
		float minDocCountPercent = sfCfg.getDiscardGroupsHavingDocCountsPercentBelow();
		// start from bottom and remove doc if share% is less than desired
		for (int i = sourceGroups.size() - 1; i >= 0; i--) {
			grup = sourceGroups.get(i);
			if (grup.getPercentOfTotal() <= minDocCountPercent) {
				sourceGroups.remove(i);
			}
		}
		if (log.isInfoEnabled())
			log.info("pickFinalResults_TopTxtCntWithSimScores: bottom results discarded due to low share% :" + (groups.size() - sourceGroups.size()) +", "+System.currentTimeMillis() );
		
		//2. take top txt cnt (var#) - top 3 txt cnt results (as you have now).
		Integer pickTop = sfCfg.getTopTxtcntResultsAlwaysPickedRegardlessLastMinuteFilter();
		if (null == pickTop)
			pickTop = 0;
		for (int i=0; i < pickTop  &&  sourceGroups.size() > 0; i++) {
			finalResults.add(sourceGroups.remove(0));
		}

		final int minSimScore2PickTopTxtCnt = 85;
		//for scores above 85 (var#) take top txtCnts (if score >85, top txt cnt wins)
		
		int g=0;
		while (finalResults.size() < finalResultsCount) {
			for (; g < sourceGroups.size(); g++) {
				grup = sourceGroups.get(g);
				if (grup.getAvgSolrScore() >= minSimScore2PickTopTxtCnt) {
					finalResults.add(sourceGroups.remove(g));
					break;
				}
			}
			if (g >= sourceGroups.size()) {
				// we have seen all rest of the groups for  85% + topTxtCnt  - out now
				break;
			}
		}
		
		//if this doesn't fill required results # - go and take remaining based on sim score -- start from 100 and going down till result # is satisfied.
		if (finalResults.size() < finalResultsCount) {
			g=0;
			// sort rest of results by score, and we pick from top
			//Collections.sort(finalResults, new SearchResultsGroupSummary.SortByAvgScore(-1));
			while (finalResults.size() < finalResultsCount  &&  sourceGroups.size() > 0) {
				finalResults.add(sourceGroups.remove(0));
			}
		}
		return finalResults;
	}
	

	private void setGroupsPercentOfTotal(List<SearchResultsGroupSummary> groups, Long totalRecords) {
		if (null == totalRecords || totalRecords <= 0) {
			totalRecords = 0L;
			for (SearchResultsGroupSummary grup : groups) {
				totalRecords += grup.getResultsCount();
			}
			this.totalLiteralTxtCount = totalRecords;
		}
		if (log.isDebugEnabled())
			log.debug("total literal txt-cnt records:" + totalRecords +", "+System.currentTimeMillis());
		for (SearchResultsGroupSummary grup : groups) {
			grup.calculatePercentOfTotal(totalRecords);
		}
	}

	private void createHtxtWordToOrgWordMap(List<SearchResultsGroupSummary> groups) throws FileNotFoundException {
		long startMillis = System.currentTimeMillis();
		for (SearchResultsGroupSummary grup : groups) {
			grup.createHtxtWordToOrgWordMap();
		}
		if (log.isInfoEnabled())
			log.info("groups' map of <hTxtWord=orgWord> made - in millis:" + (System.currentTimeMillis() - startMillis) +", "+System.currentTimeMillis());
	}

	private List<SearchResultsGroupSummary> filterBySimilarity(List<SearchResultsGroupSummary> groups)
			throws IOException {
		System.out.println("sim-api called:");
		Map<String, SearchResultsGroupSummary> id2GrupMap = new LinkedHashMap<>();
		Map<String, String> id2TextMap = new LinkedHashMap<>();
		for (SearchResultsGroupSummary grup : groups) {
			id2TextMap.put(grup.getId(), grup.getText());
			id2GrupMap.put(grup.getId(), grup);
		}

		// 1-to-Many similarity match
		long startMillis = System.currentTimeMillis();
		USESimilarityRequest request = new USESimilarityRequest();
		request.setList1(new ArrayList<String>(Arrays.asList(query.getQ()))); // list1: user's text/sent
		request.setList2(id2TextMap); // list2: groups' texts
		request.setMinScore(sfCfg.getSimilarityConfig().getMinSimilarityScore()); // min similarity score
		String apiResp = mlApi.getUSESimilarity(request);
		
		if (log.isDebugEnabled())
			log.debug("similarity api call millis:" + (System.currentTimeMillis() - startMillis) +", "+System.currentTimeMillis());

		// collect results
		startMillis = System.currentTimeMillis();
		USESimilarityResponse simResponse = null;
		try {
			simResponse = JSonUtils.json2Object(apiResp, USESimilarityResponse.class);
		} catch(JsonParseException e) {
			log.warn("Error in similarity:", e);
			log.warn("Sim-API request:");
			log.warn(JSonUtils.object2JsonString(request));
			log.warn("Sim-API Response:");
			log.warn(apiResp);
			throw e;
		}
		
		List<SearchResultsGroupSummary> results = new ArrayList<>();
		List<MatchResult> matches = simResponse.getMatchResults();
		String id;
		SearchResultsGroupSummary grup;
		// since its a 1-to-many result, we get 1 group with all similar sents,
		// desc-ordered by sim-score
		for (MatchResult mr : matches) {
			for (algo_testers.search_dependencies.USESimilarityResponse.Match mat : mr.getMatches()) {
				id = mat.getId();
				grup = id2GrupMap.get(id);
				grup.setAvgSolrScore(mat.getScore()); // override solr score with similarity score
				results.add(grup);
			}
		}
		
		writeAuditInfo("search_results_similarity_response.txt", "similarity applied to:" + groups.size(), "\r\n",
				"Sents passed sim-score condition:", results.size(), 
				"similarity response: \r\n", simResponse, "\r\n\r\n results returned post sim-application:", results);
		
		return results;
	}

	private List<SearchResultsGroupSummary> sortByDiffCountAgainstUserSent(String userSent,
			List<SearchResultsGroupSummary> groups) throws JsonProcessingException {
		if (null == groups || groups.size() == 1)
			return groups;
		long startMillis = System.currentTimeMillis();
		List<SearchResultsGroupSummary> sortedDocs = new ArrayList<>();
		// compare this client-sent with groups' text for diff-measure
		int dc;
		DiffMatchPatch dmp = new DiffMatchPatch();
		List<KeyValuePair<Integer, SearchResultsGroupSummary>> difCount2Group = new ArrayList<>();
		for (SearchResultsGroupSummary grup : groups) {
			dc = dmp.getDiffLettersCount(dmp.getDiff(userSent, grup.getText()));
			difCount2Group.add(new KeyValuePair<>(dc, grup));
		}
		// sort groups by diff-count
		Collections.sort(difCount2Group, new Comparator<KeyValuePair<Integer, SearchResultsGroupSummary>>() {
			@Override
			public int compare(KeyValuePair<Integer, SearchResultsGroupSummary> o1,
					KeyValuePair<Integer, SearchResultsGroupSummary> o2) {
				return o1.getKey().compareTo(o2.getKey());
			}
		});
		for (KeyValuePair<Integer, SearchResultsGroupSummary> kv : difCount2Group) {
			sortedDocs.add(kv.getValue());
		}
		if (log.isDebugEnabled()) {
			log.debug("Groups sorted by diff-count - in millis:" + (System.currentTimeMillis() - startMillis) +", "+System.currentTimeMillis());
			//log.debug(JSonUtils.object2JsonString(difCount2Group));
			writeAuditInfo("search_results-diff-count-sorted.txt",
					"Total docs - after sorted by dif-count:" + sortedDocs.size(),
					"\r\n\r\n---------------\r\n dif-count sorted docs:", sortedDocs);
		}
		return sortedDocs;
	}

	private List<Object> getGroupMaps(List<SearchResultsGroupSummary> groups) {
		List<Object> maps = new ArrayList<>();
		for (SearchResultsGroupSummary grup : groups) {
			maps.add(grup.toDebugMap());
		}
		return maps;
	}
}
	