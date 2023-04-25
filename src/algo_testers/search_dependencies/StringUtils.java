package algo_testers.search_dependencies;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.ArrayUtils;
import org.w3c.dom.Document;

public class StringUtils {

	public static String appendIfNotIn(String baseStr, String appendStr) {
		return appendIfNotIn(baseStr, appendStr, ",");
	}
	public static String appendIfNotIn(String baseStr, String appendStr, String separator) {
		if (baseStr.indexOf(appendStr) < 0)
			return baseStr +separator+ appendStr;
		return baseStr;
	}
	public static StringBuffer appendIfNotIn(StringBuffer baseStr, String appendStr, String separator) {
		if (baseStr.indexOf(appendStr) < 0)
			return baseStr.append(separator).append(appendStr);
		return baseStr;
	}

	public static String[] getGroups(String str, String regExPattern) {
		Pattern p = Pattern.compile(regExPattern);
		Matcher m = p.matcher(str);
		if (m.lookingAt()) {
			String[] ary = new String[m.groupCount()];
			for (int i=1; i <= m.groupCount(); i ++)
				ary[i-1] = m.group(i);
			return ary;
		}
		return new String[0];
	}

	public static String replaceAllTokens(String srcStr, Map<String, String> token_Values) {
		String patternString = "(" + org.apache.commons.lang3.StringUtils.join(token_Values.keySet(), "|") + ")";
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(srcStr);
		StringBuffer sb = new StringBuffer();
		while(matcher.find()) {
		    matcher.appendReplacement(sb, token_Values.get(matcher.group(1)));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	public static String replaceAllTokens(String srcStr, String regExToReplace, String... replaceWith) {
		Matcher matcher = Pattern.compile("("+regExToReplace+")").matcher(srcStr);
	    ///matcher.reset(srcStr);  // preferable if pattern is constant and string value keeps changing - its less taxing this way..
	    StringBuffer sb = new StringBuffer();
	    int i = 0;
	    int len = replaceWith.length;
	    while (matcher.find()) {
	      ///String text = matcher.group(1);	// the srcStr portion that matched the pattern..
	      matcher.appendReplacement(sb, replaceWith[i++ % len]);
	    }
	    matcher.appendTail(sb);
	    return sb.toString();
	}

	public static List<Integer> getAllIndexLocations(String text, Pattern pattern) {
		Matcher matcher = pattern.matcher(text);
		int idx;
		List<Integer> idxs = new ArrayList<Integer>();
		while (matcher.find()) {
			for (int i = 1; i <= matcher.groupCount(); i++) {
				idx = matcher.start(i);
				if (idx >= 0) {
					idxs.add(idx);
				}
			}
		}
		return idxs;
	}

	public static List<String> getAllMatchedGroups(String text, String pattern) {
		return getAllMatchedGroups(text, Pattern.compile(pattern));
	}
	public static List<String> getAllMatchedGroups(String text, Pattern pattern) {
		Matcher matcher = pattern.matcher(text);
		int idx;
		List<String> idxs = new ArrayList<String>();
		while (matcher.find()) {
			for (int i = 1; i <= matcher.groupCount(); i++) {
				idx = matcher.start(i);
				if (idx >= 0) {
					idxs.add(matcher.group());
				}
			}
		}
		return idxs;
	}
	
	public static Map<Integer, String> getAllMatchedLocationsAndGroups(String text, Pattern pattern) {
		Matcher matcher = pattern.matcher(text);
		int idx;
		Map<Integer, String> idxs = new TreeMap<Integer, String>();
		while (matcher.find()) {
			for (int i = 1; i <= matcher.groupCount(); i++) {
				idx = matcher.start(i);
				if (idx >= 0) {
					idxs.put(idx, matcher.group());
				}
			}
		}
		return idxs;
	}

	public static String mapToString(Map<? extends Object, ? extends Object> map) {
		StringBuilder stringBuilder = new StringBuilder();
		for (Object key : map.keySet()) {
			if (stringBuilder.length() > 0)
				stringBuilder.append(", ");
			Object value = map.get(key);
			try {
				stringBuilder.append((key != null ? URLEncoder.encode(key.toString(),
						"UTF-8") : ""));
				stringBuilder.append("=");
				stringBuilder.append(value != null ? URLEncoder.encode(value.toString(),
						"UTF-8") : "");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(
						"This method requires UTF-8 encoding support", e);
			}
		}

		return stringBuilder.toString();
	}
	
	/**
	 * Convert an XML Document to an XML String
	 * @param doc, XML Document
	 * @return string representation of the XML document
	 * @throws TransformerException
	 */
	public static String xmlToString(Document doc) throws TransformerException {
		Source source = new DOMSource(doc);
		StringWriter stringWriter = new StringWriter();
		Result result = new StreamResult(stringWriter);
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer();
		transformer.transform(source, result);
		return stringWriter.getBuffer().toString();
	}

	public static String getArrayElementStartingWith(String[] array, String str) {
		if (null == array || null == str)
			return null;
		for (String s : array)
			if (s.startsWith(str))
				return s;
		return null;
	}

	public static boolean isEqual(String str1, String str2) {
		if (org.apache.commons.lang3.StringUtils.isNotBlank(str1) &&
				org.apache.commons.lang3.StringUtils.isNotBlank(str2) )
			return str1.equals(str2);
		return false;
	}

	public static String getLastNChars(String str, int lastN) {
		return (str == null || str.length() < lastN)? str: str.substring(str.length() - lastN);
	}

	public static List<String> getNGramsWords(String text, int minWords, int maxWords) {
		return getNGramsWords(text.split(" "), minWords, maxWords);
	}
	public static List<String> getNGramsWords(String[] words, int minWords, int maxWords) {
		if (words.length <= minWords)
			return new ArrayList<>(Arrays.asList(words));
		List<String> grams = new ArrayList<>();
		int start;
		for (int wc = minWords; wc <= maxWords; wc++) {
			start=0;
			for (int end = wc; end <= words.length; end++, start++) {
				grams.add(org.apache.commons.lang3.StringUtils.join(ArrayUtils.subarray(words, start, end), " "));
			}
		}
		return grams;
	}
	
	
//	public static void main(String[] arg) {
//		System.out.println(getNGramsWords("Hello this is a sample text to see nGrams.", 2, 4));
//	}
	
}
