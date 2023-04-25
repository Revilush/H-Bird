package xbrl;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

//import org.apache.commons.net.ftp.FTPReply;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class XBRLInfoSecGov {

	protected EasyHttpClient httpClient = new EasyHttpClient(false);

	protected String siteBaseUrl = "https://www.sec.gov";

	public String calculationUrl;
	public String testUrl;

	public String acceptedDate = "";
	public String formItems = "";
	public String fye = "";
	public String formStr = "";
	public String xmlUrl = "";

	public String html;
	// we declare instance of class Document here called document. This stores
	// the 'html' page
	// for the entire class use. Its not stored here but by assigning the
	// variable outside the
	// constructor it is received - this is accomplished.
	public Document document;

	public XBRLInfoSecGov(String pageUrl) throws Exception {
		System.out.println("getting pageUrl:"+pageUrl);
		
		html = getPageHtml(pageUrl);
		document = Jsoup.parse(html, pageUrl);
		// pageUrl is the Url of the page and it used by jSoup to allow you to
		// gather all the internal links
		Pattern ptrnA = Pattern
				.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
		Elements dateElement = document.getElementsMatchingOwnText(ptrnA);
		if (dateElement.size() > 0)
			acceptedDate = dateElement.get(0).text();
		System.out.println("XBRInfoSecGov accepteddate="+acceptedDate);

		Pattern ptrn = Pattern.compile("\\.xml");
		Elements ptrnEle = document.getElementsMatchingOwnText(ptrn);

		Element rowEle = null;
//		System.out.println("XBRLInfoSecGov-1");
		for (int i = 0; i < ptrnEle.size(); i++) {
			Elements parentEle = ptrnEle.parents();
			for (Element eleFind : parentEle) {
				if (eleFind.tagName().equalsIgnoreCase("tr")) {
					rowEle = eleFind;
					break;
				}
			}
			if (null == rowEle)
				continue;

			xmlUrl = "https://www.sec.gov"
					+ rowEle.getElementsByAttribute("href").get(0).attr("href");

			// Pattern ptrnI = Pattern.compile("formGrouping"); //Results of
			// Operations
			Elements formGroups = document.getElementsByAttributeValue("class",
					"formGrouping");
			// html source is [<div class="formGrouping">]. class is
			// attribute of
			// div el and "formGrouping" is value. See tech notes for
			// getElementsByAttributeValue

//			System.out.println("XBRLInfoSecGov-2");

			for (Element ele : formGroups) {
				// loop thru each element of formGroups
				Elements children = ele.getElementsByTag("div");
				// where element name/tag is "div" provide all elements
				boolean rightGroupFound = false;
				for (Element child : children) {
					// System.out.println(JSoupXMLParser.getAttributeValue(child,"class")
					// + "::" + child.text());
					if ("infoHead".equalsIgnoreCase(JSoupXMLParser
							.getAttributeValue(child, "class"))
							&& "Items".equalsIgnoreCase(child.text())) {
						// sub routine then checks for attribute "class" to
						// see if it has value "infoHead" and to see if
						// child element value is equal to "Items"
						rightGroupFound = true;
					}
					if (rightGroupFound
							&& "info".equalsIgnoreCase(JSoupXMLParser
									.getAttributeValue(child, "class"))
							&& child.text().startsWith("Item")) {
						// the div that hold the items value...
						String items = child.text();
						// System.out.println("child.text::" + items);
						String[] item = items.split("Item \\d[\\.\\d:]{0,4}");
						String itemsStr = Arrays.toString(item);
						System.out.println("itemStr::" + itemsStr);

						formStr = itemsStr.substring(itemsStr.indexOf(",") + 2,
								itemsStr.lastIndexOf("]"));

						// System.out.println("i[]: " +
						// Arrays.toString(item));
						// System.out.println("itemsCsv: " + itemsCsv);
					}
				}
			}
//			System.out.println("XBRLInfoSecGov-3");

			Pattern ptrnFye = Pattern.compile("Fiscal Year End");
			Elements fyeElement = document.getElementsMatchingOwnText(ptrnFye);
			String fiscalYearEnd = null;
			try {
				fiscalYearEnd = fyeElement.get(0).text();
				// System.out.println("fiscalYearEnd=" + fiscalYearEnd);
				String[] fyeStr = fiscalYearEnd.split("\\|");
				fye = fyeStr[2].substring(fyeStr[2].lastIndexOf("nd:") + 3,
						fyeStr[2].lastIndexOf("nd:") + 9);
				// System.out.println("fye= " + fye);
			} catch (IndexOutOfBoundsException e) {
				System.out.println("Error fiscalYearEnd:" + e);
				e.printStackTrace(System.out);
			}
		}
	}

	// calculationUrl = doc
	// .getElementsByAttributeValueContaining("href", "_cal.xml")
	// .get(0).attr("href");
	//
	// System.out.println(calculationUrl);

	public String getAcceptedDate() {

		return acceptedDate;
	}

	public String getItems() {

		return formItems;
	}

	public String getFYE() {

		return fye;
	}

	public String getItemsStr() {

		return formStr;
	}

	protected static String getPageHtml(String pageUrl) throws IOException {
		EasyHttpClient httpClient = new EasyHttpClient(false);
		System.out.println("pageUrl="+pageUrl);
		return httpClient.makeHttpRequest("Get", pageUrl, null, -1, null, null);
	}

	// pageURL https://www.sec.gov/Archives/edgar/usgaap.rss.xml

	public static void main(String[] arg) throws Exception {

		// XBRLInfoSecGov info = new XBRLInfoSecGov(
		// "https://www.sec.gov/Archives/edgar/data/103198/000119312513385560/0001193125-13-385560-index.htm");
		// System.out.println(info.acceptedDate);
		// System.out.println("fye:: " + info.fye);
		// System.out.println("items::" + info.itemsCsv);

	}

}
