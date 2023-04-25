package search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import xbrl.NLP;

public class SearchFilter_old {

	
	private static final String[] SOLR_SPECIAL_CHARACTERS = new String[] {"+", "-", "&", "|", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~", "*", "?", ":", "\\"}; 
    private static final String[] SOLR_REPLACEMENT_CHARACTERS = new String[] {"\\+", "\\-", "\\&", "\\|", "\\!", "\\(", "\\)", "\\{", "\\}", "\\[", "\\]", "\\^", "\\\"", "\\~", "\\*", "\\?", "\\:", "\\\\"}; 

    
	public static String facetFieldnamePrefix = "facet_";
	
	public String q;
	public String company;
	public String contractType;
	public String fromDate;
	public String toDate;
	public String[] docType;
	
	public int minWordCountPercent = 50;
	public int maxWordCountPercent = 200;
	
	public Map<String, String[]> facetFilters = new HashMap<String, String[]>();
	
	public List<String> searchInFields = new ArrayList<String>();
	
	public String rawQueryFields;
	
	public List<String> sentences = new ArrayList<String>();
	
	public static SearchFilter getFilter(Map<String, String[]> parameters) throws UnsupportedEncodingException {
		SearchFilter sf = new SearchFilter();
		sf.q = parameters.get("query")[0];
		sf.sentences = getSentences(sf.q);
		
		/*if (StringUtils.isNotBlank(sf.q))
			sf.q = URLEncoder.encode(sf.q, "UTF-8");*/
		if (parameters.containsKey("company"))
			sf.filer = parameters.get("company")[0];
		if (parameters.containsKey("contract"))
			sf.contractType = parameters.get("contract")[0];
		if (parameters.containsKey("fromDate"))
			sf.fromDate = parameters.get("fromDate")[0];
		if (parameters.containsKey("toDate"))
			sf.toDate = parameters.get("toDate")[0];
		if (parameters.containsKey("minWordCountPercent"))
			sf.minWordCountPercent = Integer.parseInt(parameters.get("minWordCountPercent")[0].trim());
		if (parameters.containsKey("maxWordCountPercent"))
			sf.maxWordCountPercent = Integer.parseInt(parameters.get("maxWordCountPercent")[0].trim());
		if (parameters.containsKey("rawQueryFields"))
			sf.rawQueryFields = parameters.get("rawQueryFields")[0];
		
		if (parameters.containsKey("docType"))
			sf.docType = parameters.get("docType");
		
		
		String[] val;
		for (String param : parameters.keySet()) {
			if (param.startsWith(facetFieldnamePrefix)) {
				val = parameters.get(param);
				if (null != val && val.length > 0)		// && null != val[0] && val[0].trim().length() > 0
				sf.facetFilters.put(param.substring(facetFieldnamePrefix.length()), val);
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

		StringBuffer queryStr = new StringBuffer();
		
		StringBuffer queryFilter = new StringBuffer();
		if (StringUtils.isNotBlank(company))
			queryFilter.append(" AND company:\"").append(company).append("\"");
		if (StringUtils.isNotBlank(contractType))
			queryFilter.append(" AND contractType:\"").append(contractType).append("\"");
		if (StringUtils.isNotBlank(fromDate) || StringUtils.isNotBlank(toDate)) {
			String fromDt = StringUtils.defaultIfEmpty(fromDate, "*");
			String toDt = StringUtils.defaultIfEmpty(toDate, "*");
			queryFilter.append(" AND fileDate:").append("[").append(fromDt).append(" TO ").append(toDt).append("]");
		}
		StringBuffer qry = new StringBuffer();
		
		// need to escape all special chars that Solr treats as commands etc, to hide them from solr
		q = URLEncoder.encode(StringUtils.replaceEach(q, SOLR_SPECIAL_CHARACTERS, SOLR_REPLACEMENT_CHARACTERS), "UTF-8"); 
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
		
		String fl = "score,id", hlfl=" text", dynFieldName="", fq="";
		if (facetFilters.size() > 0) {
			fl = "id";
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
					fl += " "+dynFieldName;
					fq += URLEncoder.encode(" OR " + dynFieldName + ":" + q);
					///queryStr.append("&fq=").append(dynFieldName).append(":").append(value);
					hlfl +=  " " + dynFieldName;
				}
			}
		}
		if (fq.length() > 4)
			queryStr.append("&fq=").append(fq.substring(4));		// remove leading " OR "
		if (fl.length() > 0)
			queryStr.append("&fl=").append(URLEncoder.encode(fl));
		
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
		
		// docType filter
		if (null != docType  &&  docType.length > 0) {
			String dcQ = "";		//docType:(0 OR 2)
			for (String dc : docType) {
				if (dcQ.length() > 0)
					dcQ += "%20OR%20";
				dcQ += dc;
			}
			dcQ = "&fq=docType:(" + dcQ + ")";
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
			if (StringUtils.isNotBlank(sent)  &&  sent.trim().length() > 4)
				sents.add(sent.trim());
			start = end;
		}
		sent = query.substring(end);
		if (StringUtils.isNotBlank(sent)  &&  sent.trim().length() > 4)
			sents.add(sent.trim());
		return sents;
	}
	
}
