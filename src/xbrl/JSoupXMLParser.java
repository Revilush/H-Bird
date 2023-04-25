package xbrl;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class JSoupXMLParser {

	String issuerCIK;
	String issuerName;
	String issuerTradingSymbol;

	public static Document parse(String xml) {
		Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
		for (Element e : doc.select("test")) {
			System.out.println("Element: " + e);
		}
		return doc;
	}

	public static Document parseFile(String filePath) throws IOException {
		return parse(Utils.readTextFromFileWithSpaceSeparator(filePath));
	}

	public static String getAttributeValue(Element element, String attrName) {
		Attributes attrs = element.attributes();
		if (attrs != null) {
			for (Attribute attr : attrs) {
				if (attr.getKey().equalsIgnoreCase(attrName))
					// System.out.println("attr value: " + attr.getValue());
					return attr.getValue();
			}
		}
		return null;
	}

	public static void printChildElements(Elements elements) {
		for (Element ele : elements) {
			printElement(ele);
			if (ele.children() != null)
				printChildElements(ele.children());
			System.out.println("child ele: " + ele.children().text());
		}
	}

	public static void printParentElements(Elements elements) {
		for (Element ele : elements) {
			printElement(ele);
			if (ele.parent() != null)
				printParentElements(ele.parents());
		}
	}

	public static void printElement(Element element) {
		System.out.println("Node Name: " + element.nodeName());
		if (element.nodeName().contains("issuercik")) {
			// System.out.println("cik: "+element.ownText());
			// System.out.println("parent:"+ element.parent());
		}

		System.out.println("Node Value: " + element.val());
		// System.out.println("element text: " + element.text());
		Attributes attrs = element.attributes();
		if (attrs != null) {
			// System.out.println("Attributes:");
			for (Attribute attr : attrs) {
				System.out.println("key=val:" + attr.getKey() + "="
						+ attr.getValue());
			}
		}
	}

	public static void main(String[] arg) throws IOException {
		Document doc = parseFile("c:/backtest/testform4.xml");
		printParentElements(doc.parents());
		printChildElements(doc.children());

	}
}
