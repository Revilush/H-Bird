package xbrl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.SAXException;

public class DividendTextParser {

	public static String PaymentDateRegex = "payable on|payment date|payable|paid on";
	public static String RecordDateRegex = "(record )(?=date|at|on|as)";
	
	public static Pattern ShareUnitPattern = Pattern.compile("(?i)(per share of common stock|per share|per unit)");
	public static Pattern AmountPattern =	Pattern.compile("\\$([\\d]{0,4}\\.[\\d]{0,6})");
	
	public static Pattern DatePattern1 =	Pattern.compile("([a-zA-Z]+)[^\\p{ASCII}|[ ]]*(\\d{1,2}),\\s*(\\d{2,4})");	//("([a-zA-Z]+.*\\d{1,2},.*\\d{2,4})");
	public static Pattern DatePattern2 =	Pattern.compile("(\\d{1,2})[^\\p{ASCII}|[\\s]]*([a-zA-Z]+)[^\\p{ASCII}|[\\s]]*(\\d{2,4})");

	public static Pattern FrequencyPattern = Pattern.compile("(quarterly|per quarter|semi.{0,3}annual|annual|per annum|annually|yearly|per year|per month|monthly)");
	
	public static Pattern MonthNamesPattern = Pattern.compile("(January|February|March|April|May|June|July|August|September|October|November|December)");
	
	public static Pattern SearchPattern =  Pattern.compile("("+PaymentDateRegex+")|" +
			RecordDateRegex+"|" + AmountPattern +"|"+ ShareUnitPattern +"|"+ FrequencyPattern);
	
	public static int Amount2ShareUnitDistance = 75;
	public static int Amount2FrequencyDistance = 75;
	public static int Amount2PayDateDistance = 150;
	public static int PayDate2RecordDateDistance = 150;
	
	public static String[] getDividendAnnouncementDetailText(String text) {
		Matcher matcher = SearchPattern.matcher(text);
		/*
		 * Group-1: paymentDate
		 * Group-2: recordDate
		 * Group-3: Amt
		 * Group-4: shareUnit
		 * Group-5: Frqeuency
		 */
		Map<Integer, List<Integer>> indices = new HashMap<Integer, List<Integer>>();
		
		int idx;
		List<Integer> idxs = null;
		while (matcher.find()) {
			for (int i=1; i <= matcher.groupCount(); i++) {
				idx = matcher.start(i);
				if (idx >= 0) {
					//System.out.println("group#= " + i +" ::"+ matcher.group(i) +"::"+ idx);
					idxs = indices.get(i);
					if (null == idxs) {
						idxs = new ArrayList<Integer>();
						indices.put(i, idxs);
					}
					idxs.add(idx);
				}
			}
		}
		// get paymentDates near recordDates
//		idxs = getClosestIndicesFromFirstList(indices.get(1), indices.get(2), PayDate2RecordDateDistance);	
		if (idxs.size() == 0)
			return null;		// no payDate close by recordDate
		indices.put(1, idxs);	// keep only closer payDates
		
		//discard any far-away recordDate from selected payDates
//		idxs = getClosestIndicesFromFirstList(indices.get(2), indices.get(1), PayDate2RecordDateDistance);	
		indices.put(2, idxs);	// keep only closer recordDates
		
		// get amounts near share-units
//		idxs = getClosestIndicesFromFirstList(indices.get(3), indices.get(4), Amount2ShareUnitDistance);
		if (idxs.size() == 0)
			return null;		// no amount close by units
		indices.put(3, idxs);	// keep only these amounts
		//get amounts near frequency
//		idxs = getClosestIndicesFromFirstList(indices.get(3), indices.get(5), Amount2FrequencyDistance);	
		if (idxs.size() == 0)
			return null;		// no amount close by freq
		indices.put(3, idxs);	// keep only these amounts

		//get amounts near payDate
//		idxs = getClosestIndicesFromFirstList(indices.get(3), indices.get(1), Amount2PayDateDistance);	
		if (idxs.size() == 0)
			return null;		// no amount close by payDate
		indices.put(3, idxs);	// keep only these amounts

		// we are here, means we have amount/payDate/recordDate in desired proximities.. 
		// we may have multiple such proximities, so lets find proximity groups
		// Maximum no of proximity groups will be the size of smallest list among amt/PD/RD indeices lists. 
		
		// which listGroup has least indices
		@SuppressWarnings("unchecked")
		int smallestIdxListGroup = orderListsBySize(indices.get(1), indices.get(2), indices.get(3))[0];
		
		int maxLen = indices.get(smallestIdxListGroup).size();
		int[][] proximityGroups = new int[maxLen][];
		
		if (smallestIdxListGroup == 1) {	// least occurrences of payDate texts
			for (int i=0; i < maxLen; i++) {
				idx = indices.get(1).get(i);
				proximityGroups[i] = getClosestDateAmountIdxs(idx, indices.get(2), indices.get(3));
			}
		} else if (smallestIdxListGroup == 2) {	// least occurrences of recordDate texts
			for (int i=0; i < maxLen; i++) {
				idx = indices.get(1).get(i);
				proximityGroups[i] = getClosestDateAmountIdxs(idx, indices.get(1), indices.get(3));
			}
		} else if (smallestIdxListGroup == 3) {	//// least occurrences of amounts - should hardly be the case
			for (int i=0; i < maxLen; i++) {
				idx = indices.get(1).get(i);
				proximityGroups[i] = getClosestDateAmountIdxs(idx, indices.get(1), indices.get(2));
			}
		}
		
		String[] dividendTexts = new String[maxLen];
		for (int i=0; i < maxLen; i++) {
			Arrays.sort(proximityGroups[i]);
			System.out.println(Arrays.toString(proximityGroups[i]));
			int lastPeriodIdx = text.lastIndexOf(".", proximityGroups[i][0]);
			int nextPeriodIdx = text.indexOf(".", proximityGroups[i][2]);
			dividendTexts[i] = text.substring(Math.max(0, lastPeriodIdx + 1), 
								Math.min(text.length(), nextPeriodIdx + 1));
			System.out.println(dividendTexts[i]);
		}
		
		return dividendTexts;
	}

	public static Integer[] orderListsBySize(@SuppressWarnings("unchecked") List<Integer>... listIdxs) {
		Map<Integer, Integer> sizesOfGroups = new TreeMap<Integer, Integer>();
		for (int i=0; i < listIdxs.length; i++)
			sizesOfGroups.put(listIdxs[i].size(), i+1);
		Integer[] idxs = sizesOfGroups.values().toArray(new Integer[0]);
		return idxs;
	}
			
	public static List<Integer> getClosestIndicesFromFirstList(List<Integer> listIdxs1, List<Integer> listIdxs2) {
		List<Integer> result = new ArrayList<Integer>();
		for (Integer idx1 : listIdxs1) {
			for (@SuppressWarnings("unused") Integer idx2 : listIdxs2) {
	
					result.add(idx1);
					break;	// since this idx1 is selected now, go to next idx1
			
			}
		}
		return result;
	}
	
	public static int[] getClosestDateAmountIdxs(int idx, List<Integer> listIdxs1, List<Integer> listIdxs2) {
		int[] idxs = new int[3];
		idxs[0] = idx;
		idxs[1] = getClosestIdx(idx, listIdxs1);
		idxs[2] = getClosestIdx(idx, listIdxs2);;
		return idxs;
	}
	
	public static int getClosestIdx(int idx, List<Integer> listIdxs) {
		int distFromLastIdx = Integer.MAX_VALUE;
		int dist;
		int idxLoc = -1;
		for (Integer i : listIdxs) {
			dist = Math.abs(i - idx);
			if (dist < distFromLastIdx) {
				distFromLastIdx = dist;
				idxLoc = i;
			}
		}
		return idxLoc;
	}
	
	
	public static void main(String[] arg) throws MalformedURLException, IOException, SAXException, ParseException {
		String text = Utils.readTextFromFileWithSpaceSeparator("c:/pk/Eclipse_Indigo/workspace/Linc_Client/samples/a4753597ex99.txt");
		getDividendAnnouncementDetailText(text);
	}
	
}
