package xbrl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class SAXParserXSD extends DefaultHandler {

	private String lastSeenElementName;
	private String lastSeenElementValue;
	private String roleURI;
	private String roleURI_Role_Name;
	private String id;

	@SuppressWarnings("unused")
	private String usedOn;
	private String usedOnCal;
	private String usedOnDef;
	private String usedOnPre;
	private String definition;

	PrintWriter xsd_roleType = null;

	private String acceptedDate = null;
	private String accNo = null;
	private int roleRowNo = 0;

	// constructor method b/c same name as class name=>
	public SAXParserXSD(String xmlFilePath, String acceptedDateStr,String year, String q) throws SQLException, FileNotFoundException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		String roleType = null;
		acceptedDate = acceptedDateStr.trim();
		accNo = null;
		
		// String year = acceptedDate.substring(0,4);
		// int qtr = (Integer.parseInt(acceptedDate.replaceAll("-", "")
		// .substring(4, 6).replaceAll("^0", "")) + 2) / 3;
		// String q = qtr + "";

		try {
			File filePath = new File(xmlFilePath);
			String filename = filePath.getName().substring(0,
					filePath.getName().lastIndexOf("."));
			filename.substring(0, 20);
			accNo = filename.substring(0, 20).trim();
			Utils.createFoldersIfReqd(filePath.getParent() + "/csv/");

			roleType = filePath.getParent() + "/csv/" + filename
					+ "xsdroleType.csv";
			xsd_roleType = new PrintWriter(roleType);

			SAXParser parser = factory.newSAXParser();
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
			xsd_roleType.close();
			if (null != xsd_roleType) {
				xsd_roleType.close();
				String query = "LOAD DATA INFILE '"
						+ roleType.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+q+"_xsd_roleType FIELDS TERMINATED BY '||' "
						+ "(rowNo,accNo,acceptedDate,xsd_rt_roleURI,roleURI_Role_Name,xsd_rt_Id,xsd_rt_definition,xsd_rt_usedOnPre" +
						",xsd_rt_usedOnCal,xsd_rt_usedOnDef); ";

//				try {
					MysqlConnUtils.executeQuery(query);

//				} catch (SQLException e) {
//					e.printStackTrace(System.out);
//				}

				File f = new File(roleType.replaceAll("\\\\", "/"));
				f.delete();
			}
		}
	}

	/*
	 * gets called at the 'start' of each element.
	 */
	
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		lastSeenElementName = localName;

		if (localName.equalsIgnoreCase("roleType")) {
			roleURI = attributes.getValue("roleURI").trim();
			roleURI_Role_Name = roleURI.substring(roleURI.lastIndexOf("/") + 1,
					roleURI.length()).trim();
			id = attributes.getValue("id").trim();
			// System.out.println("roleURI: " + roleURI + " id:" + id);

			@SuppressWarnings("unused")
			String eleAttrs = localName;
			for (int i = 0; i < attributes.getLength(); i++) {
				eleAttrs += "||" + attributes.getLocalName(i);
			}
		}
	}

	/**
	 * gets called at the 'end' of each element.
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		
		StringBuilder sb = new StringBuilder();

		if (localName.equalsIgnoreCase("usedOn")) {

			if (lastSeenElementValue.contains("presentationLink")) {
				usedOnPre = lastSeenElementValue.trim();
				// System.out.println("usedOnPre: " + usedOnPre);
			}
			if (lastSeenElementValue.contains("calculationLink")) {
				usedOnCal = lastSeenElementValue.trim();
				// System.out.println("usedOnCal: " + usedOnCal);
			}
			if (lastSeenElementValue.contains("definitionLink")) {
				usedOnDef = lastSeenElementValue.trim();
				// System.out.println("usedOnDef: " + usedOnDef);
			}
		}

		if (localName.equalsIgnoreCase("definition")) {
			roleRowNo++;
			definition = lastSeenElementValue.trim();
			// System.out.println("Def: " + definition);
//			xsd_roleType.println(roleRowNo + "||" + accNo + "||" + acceptedDate
//					+ "||" + roleURI + "||" + roleURI_Role_Name + "||" + id +
//					 "||" + definition + "||" + usedOnPre + "||" + usedOnCal +
//					"||" + usedOnDef );
			

			sb.append(roleRowNo + "||" + accNo + "||" + acceptedDate + "||" + roleURI + "||" + roleURI_Role_Name + "||"
					+ id + "||" + definition + "||" + usedOnPre + "||" + usedOnCal + "||" + usedOnDef);
			xsd_roleType.println(sb.toString().replaceAll("[\r\n]+", ""));
			sb.delete(0, sb.toString().length());

		}

		if (localName.equalsIgnoreCase(lastSeenElementName)) {
			lastSeenElementName = "";
			lastSeenElementValue = "";
		}
	}

	/**
	 * Use it to receive the node value: i.e. <nodeName>value</nodeName>
	 */
	public void characters(char ch[], int start, int length)
			throws SAXException {

		if (lastSeenElementName.length() == 0)
			return;

		lastSeenElementValue += new String(Arrays.copyOfRange(ch, start, start
				+ length)).trim();

		if (lastSeenElementName.equalsIgnoreCase("usedOn")) {
			usedOn = lastSeenElementValue.trim();
		}

		if (lastSeenElementName.equalsIgnoreCase("definition")) {
			definition = lastSeenElementValue.trim();
		}
	}

	/**
	 * gets called at start of document
	 */
	public void startDocument() throws SAXException {

	}

	public void endDocument() throws SAXException {
		// System.out.println("total time taken::"
		// + (new Date().getTime() - start.getTime()));
	}

	public static void main(String[] arg) throws SQLException, FileNotFoundException {
		new SAXParserXSD(
				"c:/backtest/xbrl/2013/QTR4/0000097472-13-000026_txi-20130831.xsd",
				"acceptedDateStr","2013","4");
	}
}
