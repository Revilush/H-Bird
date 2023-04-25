package xbrl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Guidance {

	// these are required patterns==>
	public static Pattern guidanceKeyWordsPattern = Pattern
			.compile("(?i)(guidance|estimate|forecast|outlook|expect|anticipate|project)");

	public static Pattern guidanceTypePattern = Pattern
			.compile("(?i)(sales|revenue|earning|net income|net loss)");

	public static Pattern periodPattern = Pattern
			.compile("([12]{1}[09]{1}[0-9]{2})|(?i)(quarter)");

	public static Pattern valuePattern = Pattern
			.compile("(\\$[\\( ]{0,2}[\\d\\.,\\)]{1,})");
	// remember to always keep it in group!!!!! otherwise it won't pick it up as
	// a group- the group is defined by open and close parens - without it
	// there's no group picked up

	// <<==these are required patterns

	// non required - but will be picked up in final results.
	public static Pattern decPattern = Pattern
			.compile("(?i)(million|billion|thousand)");

	public static Pattern rangePattern = Pattern.compile("range");

	public static Pattern gaapPattern = Pattern
			.compile("((?i)(non.{1})?gaap|gaap)");

	public void guidanceKeyWord(String html) {
		// In 2015, the Company estimates total revenue in the range of $940 to
		// $960 million
		// output I want is: endDate: 2015,period=12, estimate vale (hi/lo):
		// lo:940, hi:960, type: revenue/sales

		// System.out.println("full text="+text);
		NLP nlp = new NLP();

		Document doc = Jsoup.parse(html);
		Elements Ps = doc.select("p:matchesOwn(" + guidanceKeyWordsPattern
				+ "),div:matchesOwn(" + guidanceKeyWordsPattern + ")"); // p|div
																		// contains
																		// guidanceKeyWordsPattern
		// this will match
		if (Ps.size() == 0) {
			System.out.println("no P/DiV matches pattern.. looking outside:");
			// div:not(:has(div))
			Ps = doc.select("p:not(:matches(" + guidanceKeyWordsPattern + "))"); // p|div
																					// contains
																					// guidanceKeyWordsPattern
			System.out.println("no P/DiV matches pattern.. looking outside:"
					+ Ps.size());
		}

		for (Element p : Ps) {
			String[] lines = p.text().split("<br");
			for (String line : lines) {

				List<Integer> guidanceKWList = nlp.getAllIndexStartLocations(line,
						guidanceKeyWordsPattern);
				List<Integer> typePatternIdxLocList = nlp.getAllIndexStartLocations(
						line, guidanceTypePattern);
				List<Integer> valuePatternIdxLocList = nlp.getAllIndexStartLocations(
						line, valuePattern);
				List<Integer> periodPatternIdxLocList = nlp.getAllIndexStartLocations(line, periodPattern);
//				List<Integer> rangePatternIdxLocList = nlp.getAllIndexStartLocations(
//						line, rangePattern);

				int guidanceIdx, start, end, closestIdx, closestGuidanceTypeIdx, closestRangeIdx, closestTypeIdx, closestValueIdx, closestPeriodIdx, dist = 200;
				String cutText;
				List<Integer> indexes = new ArrayList<Integer>();

				for (int i = 0; i < guidanceKWList.size(); i++) {
					indexes.clear();
					guidanceIdx = guidanceKWList.get(i);
					System.out.println("loop=" + i + ", guidance idx="
							+ guidanceIdx);

					closestTypeIdx = DividendTextParser.getClosestIdx(
							guidanceIdx, typePatternIdxLocList);
					System.out.println("guidance to type dist="
							+ Math.abs(closestTypeIdx - guidanceIdx)
							+ " closest typeIdx=" + closestTypeIdx);
					if (closestTypeIdx < 0
							|| Math.abs(closestTypeIdx - guidanceIdx) > dist)
						continue;

					indexes.add(closestTypeIdx);

					closestValueIdx = DividendTextParser.getClosestIdx(
							guidanceIdx, valuePatternIdxLocList);
					System.out.println("guidance to value dist="
							+ Math.abs(closestValueIdx - guidanceIdx)
							+ " closest valueIdx=" + closestValueIdx);
					if (closestValueIdx < 0
							|| Math.abs(closestValueIdx - guidanceIdx) > dist)
						continue;

					indexes.add(closestValueIdx);

					closestPeriodIdx = DividendTextParser.getClosestIdx(
							guidanceIdx, periodPatternIdxLocList);
					System.out.println("guidance to period dist="
							+ Math.abs(closestPeriodIdx - guidanceIdx));
					if (closestPeriodIdx < 0
							|| Math.abs(closestPeriodIdx - guidanceIdx) > dist)
						continue;

					indexes.add(closestPeriodIdx);

					System.out.println("indexes=" + indexes);
					Collections.sort(indexes);
					start = Math.max(0, indexes.get(0) - 100);
					end = Math.min(line.length(),
							indexes.get(indexes.size() - 1) + 100);
					cutText = line.substring(start, end);
					// System.out.println("start|\r\r" + cutText +
					// "\r\r|end\r");
					
					
					//have to get end of pattern match

					//need to get start and end of each pattern matched.
					String closestType  = line.substring(closestTypeIdx, Math.min(line.indexOf(" ", closestTypeIdx),line.length()));
					String closestPeriod  = line.substring(closestPeriodIdx, Math.min(line.indexOf(" ", closestPeriodIdx),line.length()));
					
					System.out.println("closestTypeIdx="+closestTypeIdx+" type=" + closestType+" closestPeriodIdx="+closestPeriodIdx+" type=" + closestPeriod);
					// generally guidance key word will be first key word (e.g.,
					// expect,forecast, etc.)

				}

			}
		}

	}

	public static void main(String[] args) throws IOException {

		Guidance g = new Guidance();
		String text = Utils.readTextFromFile("c:/temp/guidance4.html");
		g.guidanceKeyWord(text);

	}
}
