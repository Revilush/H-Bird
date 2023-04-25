package xbrl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
//import java.util.Date;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class SAXParserRSS extends DefaultHandler {

	@SuppressWarnings("unused")
	private String lastSeenElementName;
	private String lastSeenElementValue;
	private String acceptedDate;
	private String zipUrl;
	private String formType;

	PrintWriter pwRss = null;

	// Date start;

	public SAXParserRSS(String xmlFilePath) {
		// define printerWriter
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		SAXParser parser;
		try {
			String folderPath = "c:/backtest/xbrl/RSS";
			FileSystemUtils.createFoldersIfReqd(folderPath);
			// System.out.println(folderPath+"/allxbrlrss.txt");
			pwRss = new PrintWriter(folderPath + "/allxbrlrss.txt");
			parser = factory.newSAXParser();
			parser.parse(new File(xmlFilePath), this);
		} catch (ParserConfigurationException e) {
			e.printStackTrace(System.out);
		} catch (SAXParseException spe) {
			spe.printStackTrace(System.out);
		} catch (SAXException e) {
			e.printStackTrace(System.out);
		} catch (IOException e) {
			e.printStackTrace(System.out);
		} finally {
			if (null != pwRss) {
				pwRss.close();
			}
		}
	}

	/**
	 * gets called at the 'start' of each element.
	 */
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		// System.out.println(uri+" -- "+localName +" -- "+ qName);
		lastSeenElementName = localName.trim();

	}

	/**
	 * gets called at the 'end' of each element.
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (localName.equalsIgnoreCase("guid")) {
			zipUrl = lastSeenElementValue.trim();
			// System.out.println(zipUrl);
		}

		if (localName.equalsIgnoreCase("formType")) {
			formType = lastSeenElementValue.trim();
			// System.out.println(formType);
		}

		if (localName.equalsIgnoreCase("acceptanceDatetime")) {
			acceptedDate = lastSeenElementValue.trim();
			// System.out.println(acceptedDate);
			pwRss.println(zipUrl + "||" + acceptedDate + "||" + formType);
		}

		lastSeenElementName = "";
		lastSeenElementValue = "";
		// need to reset when element is done "endElement"

	}

	/**
	 * Use it to receive the node value: i.e. <nodeName>value</nodeName>
	 */
	public void characters(char ch[], int start, int length)
			throws SAXException {

		lastSeenElementValue += new String(Arrays.copyOfRange(ch, start, start
				+ length)).trim();
	}

	/**
	 * gets called at start of document
	 */
	public void startDocument() throws SAXException {
		// start = new Date();
	}

	public void endDocument() throws SAXException {
		// System.out.println("total time taken::"
		// + (new Date().getTime() - start.getTime()));
	}

	public static void main(String[] arg) {

		new SAXParserRSS("c:/backtest/xbrl/rss/allxbrlrss.xml");
	}
}
