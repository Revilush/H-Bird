package xbrl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class SAXParserLab extends DefaultHandler {

	private String lab_link_role = null;

	private String lab_value;
	private String label;
	private String roleHttp;
	private String lang;
	private String labelId;
	private String type;

	private String lastSeenElementName;
	private String lastSeenElementValue;
	Date start;

	PrintWriter labRoleRef = null;
	PrintWriter labLink = null;
	PrintWriter labLoc = null;
	PrintWriter labArc = null;
	PrintWriter labLab = null;

	private String acceptedDate = null;
	private String accNo = null;
	private int roleRowNo = 0;
	private int linkRowNo = 0;
	private int locRowNo = 0;
	private int arcRowNo = 0;
	private int labRowNo = 0;

	// constructor method b/c same name as class name=>
	public SAXParserLab(String xmlFilePath, String acceptedDateStr,String year, String q) throws SQLException, FileNotFoundException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		String roleFileName = null;
		String linkFileName = null;
		String locFileName = null;
		String arcFileName = null;
		String labFileName = null;

		acceptedDate = acceptedDateStr;
		accNo = null;
		
//		String year = acceptedDate.substring(0,4);
//		int qtr = (Integer.parseInt(acceptedDate.replaceAll("-", "")
//				.substring(4, 6).replaceAll("^0", "")) + 2) / 3;
//		String q = qtr + "";

		try {
			File filePath = new File(xmlFilePath);
			String filename = filePath.getName().substring(0,
					filePath.getName().lastIndexOf("."));
			filename.substring(0, 20);
			accNo = filename.substring(0, 20);

			Utils.createFoldersIfReqd(filePath.getParent() + "/csv/");

			roleFileName = filePath.getParent() + "/csv/" + filename
					+ "Role.csv";
			labRoleRef = new PrintWriter(roleFileName);

			linkFileName = filePath.getParent() + "/csv/" + filename
					+ "Link.csv";
			labLink = new PrintWriter(linkFileName);

			locFileName = filePath.getParent() + "/csv/" + filename + "Loc.csv";
			labLoc = new PrintWriter(locFileName);

			arcFileName = filePath.getParent() + "/csv/" + filename + "Arc.csv";
			labArc = new PrintWriter(arcFileName);

			labFileName = filePath.getParent() + "/csv/" + filename + "lab.csv";
			labLab = new PrintWriter(labFileName);

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
			labRoleRef.close();
			if (null != labRoleRef) {
				labRoleRef.close();
				String query = " LOAD DATA INFILE '"
						+ roleFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+q+"_lab_RoleRef FIELDS TERMINATED BY '||' "
						+ "(rowNo,accNo,acceptedDate,lab_rr_roleURI,lab_rr_roleURI_Name,lab_rr_href,lab_rr_type,lab_rr_title); ";
//				try {
					MysqlConnUtils.executeQuery(query);
//				} catch (SQLException e) {
//					e.printStackTrace(System.out);
//				}
				File f = new File(roleFileName.replaceAll("\\\\", "/"));
				f.delete();
			}
			labLink.close();
			if (null != labLink) {
				labLink.close();
				String query = " LOAD DATA INFILE '"
						+ linkFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+q+"_lab_Link FIELDS TERMINATED BY '||' "
						+ "(rowNo,accNo,acceptedDate,lab_link_role,lab_link_role_name,lab_Link_type,lab_Link_title); ";
				try {
					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
				File f = new File(linkFileName.replaceAll("\\\\", "/"));
				f.delete();
			}
			labLoc.close();
			if (null != labLoc) {
				labLoc.close();
				String query = "LOAD DATA INFILE '"
						+ locFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+q+"_lab_Loc FIELDS TERMINATED BY '||'"
						+ " (rowNo,accNo,acceptedDate,lab_link_role_name,lab_loc_Label,lab_loc_href,lab_loc_href_prefix,"
						+ "lab_loc_href_name,lab_loc_type,lab_loc_title); ";
				try {
					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
				File f = new File(locFileName.replaceAll("\\\\", "/"));
				f.delete();
			}
			labArc.close();
			if (null != labArc) {
				labArc.close();
				String query = " LOAD DATA INFILE '"
						+ arcFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+q+"_lab_arc FIELDS TERMINATED BY '||' "
						+ "(rowNo,accNo,acceptedDate,`lab_link_role_name`,`lab_arc_parent`,`lab_arc_child`,`lab_arc_type`,"
						+ "`lab_arc_arcrole`,`lab_arc_title`) ;";
				try {
					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
				File f = new File(arcFileName.replaceAll("\\\\", "/"));
				f.delete();
			}
			labLab.close();
			if (null != labLab) {
				labLab.close();

				String query = " LOAD DATA INFILE '"
						+ labFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+q+"_lab_Lab FIELDS TERMINATED BY '||'"
						+ " (rowNo,accNo,acceptedDate,lab_link_role_name,lab_lab_role,lab_Lab_value,lab_Lab_label,"
						+ "lab_Lab_lang,lab_Lab_Id,lab_Lab_type); ";
				try {
					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
				File f = new File(labFileName.replaceAll("\\\\", "/"));
				f.delete();
			}
		}
	}

	/*
	 * gets called at the 'start' of each element.
	 */
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		
		StringBuilder sb = new StringBuilder();
		
		lastSeenElementName = localName;

		@SuppressWarnings("unused")
		String eleAttrs = localName;
		for (int i = 0; i < attributes.getLength(); i++) {
			eleAttrs += "||" + attributes.getLocalName(i);
			// b/c we use + = -- we are appending to existing value in eleAttrs
			// (localName) all the attributes.
		}
		if (localName.equalsIgnoreCase("roleRef")) {
			roleRowNo++;
			String lab_rr_roleURI = attributes.getValue("roleURI");
			String lab_rr_roleURI_Name = lab_rr_roleURI.substring(
					lab_rr_roleURI.lastIndexOf("/") + 1,
					lab_rr_roleURI.length());
			String lab_rr_href = attributes.getValue("xlink:href");
			String lab_rr_type = attributes.getValue("xlink:type");
			String lab_rr_title = attributes.getValue("xlink:title");
//			labRoleRef.println(roleRowNo + "||" + accNo + "||" + acceptedDate + "||"
//					+ lab_rr_roleURI + "||" + lab_rr_roleURI_Name
//					+ "||" + lab_rr_href + "||" + lab_rr_type + "||"
//					+ lab_rr_title);

			sb.append(roleRowNo + "||" + accNo + "||" + acceptedDate + "||" + lab_rr_roleURI + "||"
					+ lab_rr_roleURI_Name + "||" + lab_rr_href + "||" + lab_rr_type + "||" + lab_rr_title);
			labRoleRef.println(sb.toString().replaceAll("[\r\n]+", ""));
			sb.delete(0, sb.toString().length());

		} else if (localName.equalsIgnoreCase("labelLink")) {
			linkRowNo++;
			lab_link_role = attributes.getValue("xlink:role");
			String lab_Link_Role_Name = lab_link_role.substring(
					lab_link_role.lastIndexOf("/") + 1, lab_link_role.length());
			String lab_link_lab_type = attributes.getValue("xlink:type");
			String lab_link_lab_title = attributes.getValue("xlink:title");
//			labLink.println(linkRowNo + "||" + accNo + "||" + acceptedDate
//					+ "||" + lab_link_role + "||" + lab_Link_Role_Name + "||"
//					+ lab_link_lab_type + "||" + lab_link_lab_title);

			sb.append(linkRowNo + "||" + accNo + "||" + acceptedDate + "||" + lab_link_role + "||" + lab_Link_Role_Name
					+ "||" + lab_link_lab_type + "||" + lab_link_lab_title);
			labLink.println(sb.toString().replaceAll("[\r\n]+", ""));
			sb.delete(0, sb.toString().length());
			
		} else if (localName.equalsIgnoreCase("loc")) {
			locRowNo++;
			String lab_link_role_name = lab_link_role.substring(
					lab_link_role.lastIndexOf("/") + 1, lab_link_role.length());
			String lab_loc_label = attributes.getValue("xlink:label");
			String lab_loc_href = attributes.getValue("xlink:href");
			String lab_loc_href_prefix = "";
			if (lab_loc_href.indexOf("#") + 1 < lab_loc_href.indexOf("_"))
				lab_loc_href_prefix = lab_loc_href.substring(lab_loc_href.lastIndexOf("#") + 1,
						lab_loc_href.indexOf("_"));
			String lab_loc_href_name = lab_loc_href.substring(
					lab_loc_href.lastIndexOf("_") + 1, lab_loc_href.length());
			String lab_loc_type = attributes.getValue("xlink:type");
			String lab_loc_title = attributes.getValue("xlink:title");
			
//			labLoc.println(locRowNo + "||" + accNo + "||" + acceptedDate + "||"
//					+ lab_link_role_name + "||" + lab_loc_label
//					+ "||" + lab_loc_href + "||"
//					+ lab_loc_href_prefix + "||" + lab_loc_href_name
//					+ "||" + lab_loc_type + "||" + lab_loc_title);
			
			sb.append(locRowNo + "||" + accNo + "||" + acceptedDate + "||" + lab_link_role_name + "||" + lab_loc_label
					+ "||" + lab_loc_href + "||" + lab_loc_href_prefix + "||" + lab_loc_href_name + "||" + lab_loc_type
					+ "||" + lab_loc_title);
			labLoc.println(sb.toString().replaceAll("[\r\n]+", ""));
			sb.delete(0, sb.toString().length());

		} else if (localName.equalsIgnoreCase("labelArc")) {
			arcRowNo++;
			String lab_link_role_name = lab_link_role.substring(
					lab_link_role.lastIndexOf("/") + 1, lab_link_role.length());
			String lab_arc_parent = attributes.getValue("xlink:from");
			String lab_arc_child = attributes.getValue("xlink:to");
			String lab_arc_arcRole = attributes.getValue("xlink:arcrole");
			String lab_arc_type = attributes.getValue("xlink:type");
			String lab_arc_title = attributes.getValue("xlink:title");
//			labArc.println(arcRowNo + "||" + accNo + "||" + acceptedDate + "||"
//					+ lab_link_role_name + "||" + lab_arc_parent
//					+ "||" + lab_arc_child + "||" + lab_arc_type
//					+ "||" + lab_arc_arcRole + "||"
//					+ lab_arc_title);
			
			sb.append(arcRowNo + "||" + accNo + "||" + acceptedDate + "||" + lab_link_role_name + "||" + lab_arc_parent
					+ "||" + lab_arc_child + "||" + lab_arc_type + "||" + lab_arc_arcRole + "||" + lab_arc_title);
			labArc.println(sb.toString().replaceAll("[\r\n]+", ""));
			sb.delete(0, sb.toString().length());

		} else if (localName.equalsIgnoreCase("label")) {
			label = attributes.getValue("xlink:label");
			roleHttp = attributes.getValue("xlink:role");
			lang = attributes.getValue("xml:lang");
			labelId = attributes.getValue("id");
			type = attributes.getValue("xlink:type");
		}
	}

	/**
	 * gets called at the 'end' of each element.
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		
		StringBuilder sb = new StringBuilder();

		if (localName.equalsIgnoreCase("label")) {
			labRowNo++;
			String lab_link_role_name = lab_link_role.substring(lab_link_role.lastIndexOf("/") + 1,
					lab_link_role.length());

//			labLab.println(
//					labRowNo + "||" + accNo + "||" + acceptedDate + "||" + lab_link_role_name
//							+ "||" + roleHttp + "||" + lab_value
//							+ "||" + label + "||" + lang + "||"
//							+ labelId + "||" + type);
			
			sb.append(labRowNo + "||" + accNo + "||" + acceptedDate + "||" + lab_link_role_name + "||" + roleHttp + "||"
					+ lab_value + "||" + label + "||" + lang + "||" + labelId + "||" + type);
			labLab.println(sb.toString().replaceAll("[\r\n]+", ""));
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

		// Keep getting all the values until you see end element tag (+ addition
		// of each val)
		lastSeenElementValue += new String(Arrays.copyOfRange(ch, start, start
				+ length));

		if (lastSeenElementName.equalsIgnoreCase("label")) {
			lab_value = lastSeenElementValue;
		}
	}

	/**
	 * gets called at start of document
	 */
	public void startDocument() throws SAXException {
		start = new Date();
	}

	public void endDocument() throws SAXException {
		// System.out.println("total time taken::"
		// + (new Date().getTime() - start.getTime()));
	}

	public static void main(String[] arg) throws SQLException, FileNotFoundException {
		new SAXParserLab(
				"c:/backtest/xbrl/2013/QTR3/0001091818-13-000355_gler-20130531_lab.xml",
				"acceptedDateStr","2013","3");
	}
}
