package algo_testers.search_dependencies;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;

public class SolrUtils {

	public static String getSolrDocFieldAsString(SolrDocument doc, String fieldName) {
		Object fieldVal = doc.getFieldValue(fieldName);
		if (null == fieldVal)
			return null;
		if (fieldVal instanceof String)
			return (String)fieldVal;
		else if (fieldVal instanceof List)
			return ((List<?>)fieldVal).get(0).toString();
		return fieldVal.toString();
	}
	
	public static Float getSolrDocFieldAsFloat(SolrDocument doc, String fieldName) {
		String val = getSolrDocFieldAsString(doc, fieldName);
		if (StringUtils.isBlank(val))
			return null;
		return Float.parseFloat(val.trim());
	}
	
	public static Double getSolrDocFieldAsDouble(SolrDocument doc, String fieldName) {
		String val = getSolrDocFieldAsString(doc, fieldName);
		if (StringUtils.isBlank(val))
			return null;
		return Double.parseDouble(val.trim());
	}
	
	public static Integer getSolrDocFieldAsInteger(SolrDocument doc, String fieldName) {
		String val = getSolrDocFieldAsString(doc, fieldName);
		if (StringUtils.isBlank(val))
			return null;
		return Integer.parseInt(val.trim());
	}
	
	public static Long getSolrDocFieldAsLong(SolrDocument doc, String fieldName) {
		String val = getSolrDocFieldAsString(doc, fieldName);
		if (StringUtils.isBlank(val))
			return null;
		return Long.parseLong(val.trim());
	}
	
}
