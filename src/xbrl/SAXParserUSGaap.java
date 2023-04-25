package xbrl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
//import java.util.Arrays;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SAXParserUSGaap extends DefaultHandler {

	@SuppressWarnings("unused")
	private String lastSeenElementName;
	// private String lastSeenElementValue;

	PrintWriter usGaapElts = null;
	PrintWriter qAtrElts = null;

	public SAXParserUSGaap(String xmlFilePath) throws FileNotFoundException {

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		SAXParser parser;
		try {
			String folderPath = "c:/backtest/xbrl/us-gaap";
			FileSystemUtils.createFoldersIfReqd(folderPath);

			usGaapElts = new PrintWriter(folderPath
					+ "/us-gaap-elts-2012-01-31.txt");
			qAtrElts = new PrintWriter(folderPath
					+ "/us-gaap-elts-2012-01-31_qAtr.txt");
			parser = factory.newSAXParser();
			parser.parse(new File(xmlFilePath), this);
		} catch (ParserConfigurationException e) {
			e.printStackTrace(System.out);
		} catch (SAXException e) {
			e.printStackTrace(System.out);
		} catch (IOException e) {
			e.printStackTrace(System.out);
		} finally {
			if (null != usGaapElts) {
				usGaapElts.close();
				String query = "TRUNCATE `US-GAAP-ELTS-2013`;";
				String query1 = "LOAD DATA INFILE 'c:/backtest/xbrl/us-gaap/us-gaap-elts-2012-01-31.txt' " +
						"ignore INTO TABLE `US-GAAP-ELTS-2013` FIELDS TERMINATED BY '||' "
						+ "(id,abstract,name,nillable,substitutionGroup,periodType,type,balance); ";

				try {

					MysqlConnUtils.executeQuery(query + query1);
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
			}
		}

		if (null != qAtrElts) {
			qAtrElts.close();
		}
	}

	/**
	 * gets called at the 'start' of each element.
	 */
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		// System.out.println(uri+" -- "+localName +" -- "+ qName);
		lastSeenElementName = localName.trim();

		String eleAttrs;

		for (int i = 0; i < attributes.getLength(); i++) {
			eleAttrs = attributes.getLocalName(i) + "||" + qName + "||"
					+ localName + "||" + uri;
			qAtrElts.println(eleAttrs);
		}

		if (localName.equalsIgnoreCase("element")) {
			String id = attributes.getValue("id");
			String abstr = attributes.getValue("abstract");
			String name = attributes.getValue("name");
			String nillable = attributes.getValue("nillable");
			String substitutionGroup = attributes.getValue("substitutionGroup");
			String periodType = attributes.getValue("xbrli:periodType");
			String type = attributes.getValue("type");
			String balance = attributes.getValue("xbrli:balance");
			
			usGaapElts.println(id + "||" + abstr + "||" + name + "||"
					+ nillable + "||" + substitutionGroup + "||"
					+ periodType + "||" + type + "||" + balance);
		}

	}

	/**
	 * gets called at the 'end' of each element.
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		lastSeenElementName = "";
		// lastSeenElementValue = "";
		// need to reset when element is done "endElement"

	}

	/**
	 * Use it to receive the node value: i.e. <nodeName>value</nodeName>
	 */
	public void characters(char ch[], int start, int length)
			throws SAXException {

		// lastSeenElementValue += new String(Arrays.copyOfRange(ch, start,
		// start
		// + length)).trim();

	}

	/**
	 * gets called at start of document
	 */
	public void startDocument() throws SAXException {

	}

	public void endDocument() throws SAXException {

	}

	public static void main(String[] arg) throws FileNotFoundException {

		new SAXParserUSGaap(
				"c:/backtest/xbrl/us-gaap/us-gaap-2013-01-31.xsd.xml");
	}
}
