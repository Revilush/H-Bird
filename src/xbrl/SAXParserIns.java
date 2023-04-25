package xbrl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Arrays;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;

//import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
//import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

//SEE XX - LAST SEEN ELEMENT FOR INS_DATA FILE WILL NOT CAPTURE ANY VALUES THAT CONTAIN TEXT - I.E., NO TEXTBLOCKS/DISCLOSURES
public class SAXParserIns extends DefaultHandler {

	PrintWriter context = null;
	PrintWriter unit = null;
	PrintWriter data = null;
	// PrintWriter textB = null;
	// PrintWriter qAtr = null;

	private String contextId = null;
	private String dimension = null;
	private String dimensionValue = null;
	private int segment = 0;
	private String startDate = null;
	private String endDate = null;
	private String instant = null;
	private String identifier = null;

	// @SuppressWarnings("unused")
	// private String textBlock = null;

	private String unitId = null; // unit id [varchar]
	private String measure = null; // [last seen el - varchar]
	private int divide = 0; // [0/1]
	private int multiply = 0; // [0/1]
	private int unitDenominator = 0; // [0/1
	private int unitNumerator = 0; // [0/1

	private String prefix = null;
	private String dataId = null;
	private String unitRef = null;
	private String contextRef = null;
	private String decimals = null;

	// private String localName2 = null;

	private String lastSeenElementName;
	private String lastSeenElementValue;
	private String ticker;

	private String acceptedDate = null;
	private String accNo = null;
	private int dataRowNo = 0;
	private int contextRowNo = 0;
	private int unitRowNo = 0;

	// private int textRowNo = 0;

	// constructor method b/c same name as class name=>
	public SAXParserIns(String xmlFilePath, String acceptedDateStr, String yYear, String qQtr) throws SQLException, FileNotFoundException {
		System.out.println("saxParserIns xmlFilePath: " + xmlFilePath);
		// rare # cases ="_" or =null.
		ticker = "Error";
		System.out.println("xmlFilePath=" + xmlFilePath);
		
		String tickerHelper = xmlFilePath.substring(xmlFilePath.lastIndexOf("/") + 22, xmlFilePath.length());

		System.out.println("tickerHelper=" + tickerHelper);
		if (tickerHelper.lastIndexOf("-") >= 0) {

			ticker = xmlFilePath.substring(xmlFilePath.lastIndexOf("/") + 22, xmlFilePath.lastIndexOf("-"));

			System.out.println("ticker::" + ticker);
		}

		else if (tickerHelper.lastIndexOf("_") >= 0) {
			ticker = xmlFilePath.substring(xmlFilePath.lastIndexOf("/") + 22, xmlFilePath.lastIndexOf("_"));
			System.out.println("ticker::" + ticker);
		}

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);

		String contextFileName = null;
		String unitFileName = null;
		String dataFileName = null;
		// String textBlockFileName = null;
		// String qAtrFileName = null;
		// start = new Date();

		acceptedDate = acceptedDateStr;
		accNo = null;

		// String year = acceptedDate.substring(0, 4);
		// int qtr = (Integer.parseInt(acceptedDate.replaceAll("-", "")
		// .substring(4, 6).replaceAll("^0", "")) + 2) / 3;
		// System.out.println("quarter="+qtr+" acceptedDate="+acceptedDate);

		// String q = qtr + "";

		try {
			File fileAndPath = new File(xmlFilePath);
			String filename = fileAndPath.getName().substring(0, fileAndPath.getName().lastIndexOf("."));
			filename.substring(0, 20);
			accNo = filename.substring(0, 20);
			System.out.println("");
			Utils.createFoldersIfReqd(fileAndPath.getParent() + "/csv/");

			contextFileName = fileAndPath.getParent() + "/csv/" + filename + "context.csv";
			context = new PrintWriter(contextFileName);

			unitFileName = fileAndPath.getParent() + "/csv/" + filename + "unit.csv";
			// fileAndPath.getParent gets location of file

			unit = new PrintWriter(unitFileName);

			// assign to variable 'unit' the behavior of the PrintWriter and PW
			// must have path to complete its behavior

			dataFileName = fileAndPath.getParent() + "/csv/" + filename + "data.csv";
			data = new PrintWriter(dataFileName);

			// textBlockFileName = fileAndPath.getParent() + "/csv/" + filename
			// + "TextBlock.csv";
			// textB = new PrintWriter(textBlockFileName);

			// qAtrFileName = filePath.getParent() + "/csv/" + filename
			// + "InsQAtr.csv";
			// qAtr = new PrintWriter(qAtrFileName);

			SAXParser parser = factory.newSAXParser();
			// SAXParser is the tractor.
			parser.parse(new File(xmlFilePath), this);
			// .parse parses file using 'this' which applies Default Handler
			// (see @start of Class). To instantiate '.parse' you need file and
			// handler

		} catch (ParserConfigurationException e) {
			e.printStackTrace(System.out);
		} catch (SAXParseException spe) {
			spe.printStackTrace(System.out);
		} catch (SAXException e) {
			e.printStackTrace(System.out);
		} catch (IOException e) {
			e.printStackTrace(System.out);
		} finally {
			context.close();
			if (null != context) {
				context.close();
				String query = "LOAD DATA INFILE '" + contextFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_" + yYear + "Q" + qQtr
						+ "_ins_context FIELDS TERMINATED BY '||' "
						+ "(rowNo,accNo,acceptedDate,id,startDate,endDate,instant,segment,dimension,dimensionValue,CIK); ";

				// try {
				MysqlConnUtils.executeQuery(query);
				// } catch (SQLException e) {
				// e.printStackTrace(System.out);
				// }
				File f = new File(contextFileName.replaceAll("\\\\", "/"));
				 f.delete();
			}
			unit.close();
			if (null != unit) {
				unit.close();
				String query = "LOAD DATA INFILE '" + unitFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_" + yYear + "Q" + qQtr + "_ins_unit FIELDS TERMINATED BY '||' "
						+ "(rowNo,accNo,acceptedDate,id,measure,divide,Denominator,Numerator); ";

				try {
					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
				File f = new File(unitFileName.replaceAll("\\\\", "/"));
				 f.delete();
			}
			data.close();
			if (null != data) {
				data.close();

				String query = "LOAD DATA INFILE '" + dataFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_" + yYear + "Q" + qQtr + "_ins_data FIELDS TERMINATED BY '||' "
						+ "(rowNo,accNo,acceptedDate,prefix,Name,value,contextRef,unitRef,id,decimals); ";
				try {
					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
				File f = new File(dataFileName.replaceAll("\\\\", "/"));
				 f.delete();
			}
		}
		// textB.close();
		// if (null != textB) {
		// textB.close();
		// String query = "LOAD DATA INFILE '"
		// + textBlockFileName.replaceAll("\\\\", "/")
		// + "' ignore INTO TABLE TMPxbrl"+year+"Q"+q+"_ins_data FIELDS TERMINATED BY
		// '||' "
		// +
		// "(rowNo,accNo,acceptedDate,prefix,Name,value,contextRef,unitRef,id,decimals);
		// ";
		// try {
		// MysqlConnUtils.executeQuery(query);
		// } catch (SQLException e) {
		// e.printStackTrace(System.out);
		// }
		// File f = new File(textBlockFileName.replaceAll("\\\\", "/"));
		// f.delete();
		// }
	}

	/*
	 * gets called at the 'start' of each element.
	 */

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		if (qName == null) {
			qName = "";
		}
		// captures name of element which we need later for purposes of
		// identifying element - see characters method. It is probably best to
		// put it here b/c this is where it reads the element names.

		if (qName.endsWith("TextBlock") || qName.endsWith("Policy") || qName.endsWith("Disclosure")) {
			return;
		}

		lastSeenElementName = localName;
		if (qName.toLowerCase().startsWith("ix")) {
			lastSeenElementName = attributes.getValue("name");
			if (null != lastSeenElementName) {
				String[] nameParts = lastSeenElementName.split(":");
				prefix = nameParts[0];
				if (nameParts.length > 1) {
					lastSeenElementName = nameParts[1];
				}
			}
		}
		// textBlock = "";

		if (localName.equalsIgnoreCase("context")) {
			contextId = attributes.getValue("id");
			segment = 0;
			dimensionValue = null;
			identifier = null;
			startDate = null;
			endDate = null;
			instant = null;
			dimension = "";// reset the startDate,endDate and instant to null
			unitId = null;
			measure = null;
			divide = 0;
			multiply = 0;
			unitDenominator = 0;
			unitDenominator = 0;

		} else if (localName.equalsIgnoreCase("segment")) {
			segment = 1;
		} else if (localName.equalsIgnoreCase("explicitMember")) {
			dimension += attributes.getValue("dimension") + "]]";
			// System.out.println("dimension: " + dimension);
		}

		if (localName.equalsIgnoreCase("unit")) {
			unitId = attributes.getValue("id");
			measure = null;
		} else if (localName.equalsIgnoreCase("divide")) {
			divide = 1;
		} else if (localName.equalsIgnoreCase("multiply")) {
			multiply = 1;
		} else if (localName.equalsIgnoreCase("unitDenominator")) {
			unitDenominator = 1;
		} else if (localName.equalsIgnoreCase("unitNumerator")) {
			unitDenominator = 1;
		}

		dataId = null;

		if (qName.toLowerCase().startsWith("us-gaap") || qName.toLowerCase().startsWith("dei")
				|| qName.toLowerCase().startsWith(ticker)) {
			dataId = attributes.getValue("id");
		}

		contextRef = attributes.getValue("contextRef");
		decimals = attributes.getValue("decimals");
		unitRef = attributes.getValue("unitRef");

		// b/c this is startElement method - entire element not seen. Value is
		// determined at end
	}

	// to receive node value (element val): i.e. <nodeName>value</nodeName>. ==>
	public void characters(char ch[], int start, int length) throws SAXException {

		if (null == lastSeenElementName || lastSeenElementName.length() == 0)
			return;

		lastSeenElementValue += new String(Arrays.copyOfRange(ch, start, start + length));

		// this captures instances where starting value is less than ('<') which
		// indicates html in the element value (this is a contingency for
		// instances not caught by 'textBlock' rule).
		if (lastSeenElementValue.startsWith("<")) {
			lastSeenElementName = "";
			lastSeenElementValue = "";
			return;
		}

		/*
		 * Arrays.copyOfRange returns a char array - this method takes a specified range
		 * of another array and reads and assigns it to its array. The String class can
		 * accept a character array and assign it to another string variable.
		 */

		if (lastSeenElementName.equalsIgnoreCase("explicitMember")) {
			dimensionValue = lastSeenElementValue;
		}

		if (lastSeenElementName.endsWith("explicitMember")) {
			dimensionValue = lastSeenElementValue;
		}
		if (lastSeenElementName.equalsIgnoreCase("identifier")) {
			identifier = lastSeenElementValue;
		}

		if (lastSeenElementName.equalsIgnoreCase("startDate")) {
			startDate = lastSeenElementValue;
		}
		if (lastSeenElementName.equalsIgnoreCase("endDate")) {
			endDate = lastSeenElementValue;
		}

		if (lastSeenElementName.equalsIgnoreCase("instant")) {
			instant = lastSeenElementValue;
		}
		if (lastSeenElementName.equalsIgnoreCase("measure")) {
			measure = lastSeenElementValue;
		}
		if (lastSeenElementName.toLowerCase().startsWith(ticker)) {
			ticker = lastSeenElementValue;
			// System.out.println(lastSeenElementValue);
		}
	}

	/**
	 * gets called at the 'end' of each element.
	 */

	public void endElement(String uri, String localName, String qName) throws SAXException {
		
		StringBuilder sb = new StringBuilder();

		if (localName.equals("context")) {
			contextRowNo++;
//			context.println(contextRowNo + "||" + accNo + "||" + acceptedDate + "||"
//					+ contextId + "||" + startDate + "||"
//					+ endDate + "||" + instant + "||" + segment
//					+ "||" + dimension + "||" + dimensionValue
//					+ "||" + identifier);
			sb.append(contextRowNo + "||" + accNo + "||" + acceptedDate + "||"
					+ contextId + "||" + startDate + "||"
					+ endDate + "||" + instant + "||" + segment
					+ "||" + dimension + "||" + dimensionValue
					+ "||" + identifier);
			
			context.println(sb.toString().replaceAll("[\r\n]+", ""));
			sb.delete(0, sb.toString().length());
			contextId = null;
		}
		if (localName.equals("measure")) {
			unitRowNo++;
			// save one unit record...
//			unit.println(unitRowNo + "||" + accNo + "||" + acceptedDate + "||" + unitId + "||"
//					+ measure + "||" + divide + "||" + multiply + "||" + unitDenominator
//					+ "||" + unitNumerator);
			
			sb.append(unitRowNo + "||" + accNo + "||" + acceptedDate + "||" + unitId + "||" + measure + "||" + divide
					+ "||" + multiply + "||" + unitDenominator + "||" + unitNumerator);
			unit.println(sb.toString().replaceAll("[\r\n]+", ""));
			sb.delete(0, sb.toString().length());
			
		}
		if (localName.equals("unit")) {

			unitId = null; // unit id [varchar]
			measure = null; // [last seen el - varchar]
			divide = 0; // [0/1]
			multiply = 0; // [0/1]
			unitDenominator = 0; // [0/1
			unitNumerator = 0;
		}

		if (localName.equals("unit")) {
		}

		if ((qName.toLowerCase().startsWith("us-gaap") || qName.toLowerCase().startsWith("dei")
				|| qName.toLowerCase().startsWith(ticker))) {
			try {
				// xxxx
				prefix = qName.substring(0, qName.lastIndexOf(":"));

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		// lastSeenElementValue = doc.text();
		/*
		 * This was to address/strip out html garbage in textBlock. But this will apply
		 * to all element values - which is okay. Document is a class of Jsoup and parse
		 * is a method of doc. Parse is then applied to the lastSeenElementValue (i.e.,
		 * text). The results are then of course in the instance class variable of doc
		 * and then using 'text' method (doc.text) we are able to produce the value of
		 * the lastSeenElement in the data.println below.
		 */
		/*
		 * data.println(prefix + "||" + lastSeenElementName + "||" + doc.text() + "||" +
		 * contextRef + "||" + unitRef + "||" + dataId + "||" + decimals); }
		 */
		// //XX - LAST SEEN ELEMENT FOR INS_DATA FILE WILL NOT CAPTURE ANY
		// VALUES THAT CONTAIN TEXT - I.E., NO TEXTBLOCKS/DISCLOSURES

		if (contextId == null && unitId == null && lastSeenElementValue != null
				&& (lastSeenElementValue.endsWith("0") || lastSeenElementValue.endsWith("1")
						|| lastSeenElementValue.endsWith("2") || lastSeenElementValue.endsWith("3")
						|| lastSeenElementValue.endsWith("4") || lastSeenElementValue.endsWith("5")
						|| lastSeenElementValue.endsWith("6") || lastSeenElementValue.endsWith("7")
						|| lastSeenElementValue.endsWith("8") || lastSeenElementValue.endsWith("9")
						|| qName.toLowerCase().startsWith("dei"))) {
			try {
				dataRowNo++;
				Document doc = Jsoup.parse(lastSeenElementValue, "");
				lastSeenElementValue = doc.text();;
				// System.out.println("ins data::"+dataRowNo + "||" + accNo +
				// "||" + acceptedDate
				// + "||" + prefix + "||" + lastSeenElementName + "||"
				// + doc.text() + "||" + contextRef + "||" + unitRef
				// + "||" + dataId + "||" + decimals);
				data.println(dataRowNo + "||" + accNo + "||" + acceptedDate + "||" + prefix
						+ "||" + lastSeenElementName + "||"
						+ doc.text() + "||" + contextRef + "||"
						+ unitRef + "||" + dataId + "||"
						+ decimals);

			} catch (NumberFormatException e) {
				// value is not a number..

				/*
				 * Document doc = Jsoup.parse(lastSeenElementValue, ""); lastSeenElementValue =
				 * doc.text(); textBlock.println(prefix + "||" + lastSeenElementName + "||" +
				 * doc.text() + "||" + contextRef + "||" + unitRef + "||" + dataId + "||" +
				 * decimals);
				 */
			}
		}
		/*
		 * if (localName.equalsIgnoreCase(lastSeenElementName)) { lastSeenElementName =
		 * ""; lastSeenElementValue = ""; }
		 */
		lastSeenElementName = "";
		lastSeenElementValue = "";

	}

	// gets called at start of document

	public void startDocument() throws SAXException {
		// System.out.println("startDoc :"
		// + (new Date().getTime() - start.getTime()));
	}

	public void endDocument() throws SAXException {
		// System.out.println("total time taken::"
		// + (new Date().getTime() - start.getTime()));
	}

	public static void main(String[] arg) throws SQLException, FileNotFoundException {
		new SAXParserIns(

				"c:/backtest/xbrl/2018/QTR3/0000000000-00-000000_pfe-07022017x10q_htm.xml", "acceptedDateStr", "2018",
				"3");
	}
}
