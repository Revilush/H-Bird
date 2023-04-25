package search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import xbrl.NLP;

public class SearchFilter {

	
	private static final String[] SOLR_SPECIAL_CHARACTERS = new String[] {"+", "-", "&", "|", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~", "*", "?", ":", "\\"}; 
    private static final String[] SOLR_REPLACEMENT_CHARACTERS = new String[] {"\\+", "\\-", "\\&", "\\|", "\\!", "\\(", "\\)", "\\{", "\\}", "\\[", "\\]", "\\^", "\\\"", "\\~", "\\*", "\\?", "\\:", "\\\\"}; 

    
	public static String facetFieldnamePrefix = "facet_";
	public static String filterQueryFieldnamePrefix = "fq_";
	
	public String q=null, rawQ=null;
	public String filer;
	public String contractLongName;
	public String contractType;
	
	public String definitionHeading;
	public String sectionHeading;
	public String exhibitHeading;
	public String fromDate;
	public String toDate;
	public String[] docType;
	
	public Map<String, String[]> extraFilterQueries = new HashMap<String, String[]>();
	
	public int minWordCountPercent = 0;
	public int maxWordCountPercent = 0;
	
	public Map<String, String[]> facetFilters = new HashMap<String, String[]>();
	
	public List<String> searchInFields = new ArrayList<String>();
	
	public String rawQueryFields;
	
	public List<String> sentences = new ArrayList<String>();
	
	public static SearchFilter getFilter(Map<String, String[]> params) throws UnsupportedEncodingException {
		Map<String, String[]> parameters = new HashMap<String, String[]>(params);
		SearchFilter sf = new SearchFilter();
		sf.q = parameters.remove("query")[0];
		sf.sentences = getSentences(sf.q);
		
		// NLP.printMapStrStrAry("param=", params);
		
		/*if (StringUtils.isNotBlank(sf.q))
			sf.q = URLEncoder.encode(sf.q, "UTF-8");*/
		if (parameters.containsKey("filer"))
			sf.filer = parameters.remove("filer")[0];
		if (parameters.containsKey("contractLongName"))
			sf.contractLongName = parameters.remove("contractLongName")[0];
		if (parameters.containsKey("contract"))
			sf.contractType = parameters.remove("contract")[0];
		if (parameters.containsKey("definitionHeading"))
			sf.definitionHeading = parameters.remove("definitionHeading")[0];
		if (parameters.containsKey("sectionHeading"))
			sf.sectionHeading = parameters.remove("sectionHeading")[0];
		if (parameters.containsKey("exhibitHeading"))
			sf.exhibitHeading = parameters.remove("exhibitHeading")[0];
		if (parameters.containsKey("fromDate"))
			sf.fromDate = parameters.remove("fromDate")[0];
		if (parameters.containsKey("toDate"))
			sf.toDate = parameters.remove("toDate")[0];
		if (parameters.containsKey("minWordCountPercent")) {
			String[] mwcp = parameters.remove("minWordCountPercent");
			if (null != mwcp  &&  mwcp.length > 0  &&  StringUtils.isNotBlank(mwcp[0]) )
				sf.minWordCountPercent = Integer.parseInt(mwcp[0].trim());
		}
		
		if (parameters.containsKey("maxWordCountPercent")) {
			String[] mwcp = parameters.remove("maxWordCountPercent");
			if (null != mwcp  &&  mwcp.length > 0  &&  StringUtils.isNotBlank(mwcp[0]) )
				sf.maxWordCountPercent = Integer.parseInt(mwcp[0].trim());
		}
		
		if (parameters.containsKey("rawQueryFields") && null != parameters.get("rawQueryFields")  &&  parameters.get("rawQueryFields").length > 0) {
			sf.rawQueryFields = parameters.remove("rawQueryFields")[0];
			if (StringUtils.startsWithIgnoreCase(sf.rawQueryFields, "q=")  ||  StringUtils.containsIgnoreCase(sf.rawQueryFields, "&q=")) {
				String[] Qs = sf.rawQueryFields.split("\\&");
				for (String rq : Qs)
					if (StringUtils.startsWithIgnoreCase(rq.trim(), "q=")) {
						sf.rawQ = rq.trim().substring(2);	// remove "q="
						
						Qs = (String[]) ArrayUtils.removeElement(Qs, rq);
						sf.rawQueryFields = StringUtils.join(Qs, "&");		//sf.rawQueryFields.replace(sf.rawQ, "");		// remove 'q=...' from other raq query params.
						break;
					}
			}
		}
		
		if (parameters.containsKey("docType"))
			sf.docType = parameters.remove("docType");
		
		
		String[] val;
		for (String param : parameters.keySet()) {
			if (param.startsWith(facetFieldnamePrefix)) {
				val = parameters.remove(param);
				if (null != val && val.length > 0)		// && null != val[0] && val[0].trim().length() > 0
					sf.facetFilters.put(param.substring(facetFieldnamePrefix.length()), val);
			} else if (param.startsWith(filterQueryFieldnamePrefix)) {
				val = parameters.remove(param);
				if (null != val && val.length > 0  &&  StringUtils.isNotBlank(val[0])) {
					sf.extraFilterQueries.put(param.substring(filterQueryFieldnamePrefix.length()) , val);
				}
			}
		}
		return sf;
	}

	public List<String> appendFiltersToSearchUrl(String solrSearchUrl) throws UnsupportedEncodingException {
		List<String> urls = new ArrayList<String> ();
		for (String q: sentences) {
			urls.add(appendFiltersToSearchUrl(solrSearchUrl, q));
		}
		return urls;
	}

	@SuppressWarnings("deprecation")
	public String appendFiltersToSearchUrl(String solrSearchUrl, String q) throws UnsupportedEncodingException {
		/*
		 fq=[2012-09-24%20TO%20NOW]
		 &fq=published_date:[2012-09-24T00:00:00Z TO 2012-09-24T23:59:99.999Z]
		 &fq=published_date:[NOW-7DAY/DAY TO NOW]
		 [* TO 2014-12-01]
		 
		 Range queries: The brackets around a query determine its inclusiveness.
				Square brackets [ ] denote an inclusive range query that matches values including the upper and lower bound.
				Curly brackets { } denote an exclusive range query that matches values between the upper and lower bounds, but excluding the upper and lower bounds themselves.
				You can mix these types so one end of the range is inclusive and the other is exclusive.  Here's an example: count:{1 TO 10]
		 */
		/*
		 * q.op = AND  or  OR
		 */
		/*
		 df = Specifies a default field, overriding the definition of a default field in the schema.xml file.
		 */

		String fields2Fetch = "score,id,sentence";
		
		StringBuffer queryStr = new StringBuffer();
		
		StringBuffer queryFilter = new StringBuffer();
		if (StringUtils.isNotBlank(filer))
			queryFilter.append(" AND filer:(").append(filer).append(")");
		if (StringUtils.isNotBlank(contractLongName)) {
			queryFilter.append(" AND contractLongName:(").append(contractLongName).append(")");
			fields2Fetch += ",contractLongName";
		}
		if (StringUtils.isNotBlank(contractType))
			queryFilter.append(" AND contractType:(").append(contractType).append(")");
		if (StringUtils.isNotBlank(definitionHeading)) {
			queryFilter.append(" AND definitionHeading:(").append(definitionHeading).append(")");
			fields2Fetch += ",definitionHeading";
		}
		if (StringUtils.isNotBlank(sectionHeading)) {
			queryFilter.append(" AND sectionHeading:(").append(sectionHeading).append(")");
			fields2Fetch += ",sectionHeading";
		}
		if (StringUtils.isNotBlank(exhibitHeading)) {
			queryFilter.append(" AND exhibitHeading:(").append(exhibitHeading).append(")");
			fields2Fetch += ",exhibitHeading";
		}
		if (StringUtils.isNotBlank(fromDate) || StringUtils.isNotBlank(toDate)) {
			String fromDt = fromDate;
			if (StringUtils.isBlank(fromDt))
					fromDate = "*";
			else if (! StringUtils.endsWith(fromDt, "Z"))
				fromDt += "T00:00:00Z";
			
			String toDt = toDate;
			if (StringUtils.isBlank(toDt))
				toDt = "*";
			else if (! StringUtils.endsWith(toDt, "Z"))
				toDt += "T00:00:00Z";
			System.out.println("from:" + fromDt + ", toDt:" + toDt);
			queryFilter.append(" AND fileDate:").append("[").append(fromDt).append(" TO ").append(toDt).append("]");
		}
		StringBuffer qry = new StringBuffer();
		
		// need to escape all special chars that Solr treats as commands etc, to hide them from solr
		q = URLEncoder.encode(StringUtils.replaceEach(q, SOLR_SPECIAL_CHARACTERS, SOLR_REPLACEMENT_CHARACTERS), "UTF-8");
		// add any rawQ if specified..
		if (StringUtils.isNotBlank(rawQ))
			q = q + URLEncoder.encode(rawQ);
		//System.out.println(q);
		
		if (searchInFields.size() > 0) {
			for (String fld : searchInFields) {
				if (qry.length() > 0)
					qry.append(" OR ");
				qry.append(fld).append(":(").append(q).append(")");
			}
			//qry = new StringBuffer(URLEncoder.encode(qry.toString()));
			queryStr.append("&hl.requireFieldMatch=true&hl.usePhraseHighlighter=true");
		} else
			qry.append("(").append(q).append(")");
		queryStr.append("&q=").append(qry).append(URLEncoder.encode(queryFilter.toString(), "UTF-8"));
		
		String hlfl=" text", dynFieldName="", fq="";
		if (facetFilters.size() > 0) {
			fields2Fetch = "id";
			hlfl = "";
			String[] facetValues;
			for (String facetField : facetFilters.keySet()) {
				facetValues = facetFilters.get(facetField);
				for (String value : facetValues) {
					dynFieldName = "";
					if (facetField.equalsIgnoreCase("sectionHeading")) {
						dynFieldName = "section_";
					} else if (facetField.equalsIgnoreCase("contractDefinedTerm")) {
						dynFieldName = "definedTerm_";
					}
					dynFieldName += sectionHeadingValue2SectionName(value.trim());
					fields2Fetch += " "+dynFieldName;
					fq += URLEncoder.encode(" OR " + dynFieldName + ":" + q);
					System.out.println("fq="+" OR " + dynFieldName + ":" + q);
					///queryStr.append("&fq=").append(dynFieldName).append(":").append(value);
					hlfl +=  " " + dynFieldName;
				}
			}
		}
		
		if (fq.length() > 4) {
			queryStr.append("&fq=").append(fq.substring(4)); // remove leading " OR "
			System.out.println("&fq="+fq.substring(4));
		}
		if (fields2Fetch.length() > 0)
			queryStr.append("&fl=").append(URLEncoder.encode(fields2Fetch));
		
		if (minWordCountPercent > 0  ||  maxWordCountPercent > 0) {
			// add range filter query based on word-count..
			int qWordCount = q.split("[\\+ \t]+").length;		// all spaces comes as '+' (url-encoded)
			String mincount = "*";
			if (minWordCountPercent > 0)
				mincount = ((int) (qWordCount * (minWordCountPercent / 100.0))) + "";
			String maxcount = "*";
			if (maxWordCountPercent > 0)
				maxcount = ((int) (qWordCount * (maxWordCountPercent / 100.0))) + "";
			queryStr.append("&fq=wordCount:[").append(mincount).append("%20TO%20").append(maxcount).append("]");
		}
		if (extraFilterQueries.size() > 0) {
			String[] vals;
			for (String fieldNm : extraFilterQueries.keySet()) {
				vals = extraFilterQueries.get(fieldNm);
				queryStr.append("&fq=").append(fieldNm).append(":(").append(StringUtils.join(vals, URLEncoder.encode(" OR "))).append(")");
				System.out.println("&fq=" + fieldNm + ":(" + StringUtils.join(vals, URLEncoder.encode(" OR ")));
			}
		}
		
		// docType filter
		if (null != docType  &&  docType.length > 0) {
			String dcQ = "";		//docType:(0 OR 2)
			for (String dc : docType) {
				if (StringUtils.isBlank(dc))
					continue;
				if (dcQ.length() > 0)
					dcQ += "%20OR%20";
				dcQ +=  "%22" + dc.trim() + "%22";
			}
			dcQ = "&fq=contractType:(" + dcQ + ")";		//docType:
			System.out.println("fq="+dcQ);
			queryStr.append(dcQ);
		}
		
		// add raw query fields if available
		if (StringUtils.isNotBlank(rawQueryFields)) {
			queryStr.append("&").append(StringUtils.removeStart(rawQueryFields, "&"));
		}
		
		if (hlfl.length() > 1)
			queryStr.append("&hl=on&hl.fl=").append(URLEncoder.encode(hlfl.substring(1)));
		else
			System.out.println("no field to highlight!!");
		
		return solrSearchUrl + queryStr;
	}
	
	private String sectionHeadingValue2SectionName(String fieldValue) {
		return fieldValue.replaceAll("[^0-9a-zA-Z]", "");
	}
	
	
	private static List<String> getSentences(String query) {
		
		List<String> sents = new ArrayList<String>();
		NLP nlp = new NLP();
		List<Integer> sentEnds = nlp.getAllIndexEndLocations(query, NLP.patternSentenceEnd);
		int start = 0, end=0;
		String sent;
		for (int i=0; i < sentEnds.size(); i++) {
			end = sentEnds.get(i);
			sent = query.substring(start, end);
			
			if (StringUtils.isNotBlank(sent) && sent.trim().length() > 4) {
				sents.add(sent.trim());
				System.out.println("sent=" + sent);
			}

			start = end;
		}
		sent = query.substring(end);
		if (StringUtils.isNotBlank(sent)  &&  sent.trim().length() > 4) {
			sents.add(sent.trim());
		}
		
		NLP.printListOfString("listSents=", sents);
		
		return sents;
		
	}
	
}
