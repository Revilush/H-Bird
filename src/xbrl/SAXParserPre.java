package xbrl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class SAXParserPre extends DefaultHandler {

	private String pre_link_role = null;
	private String pre_link_role_name = null;
	private String pre_roleURI_name = null;

	private String ticker = null;
	private String formType = null;

	private String acceptedDate = null;
	private String accNo = null;
	private int roleRowNo = 0;
	private int linkRowNo = 0;
	private int locRowNo = 0;
	private int arcRowNo = 0;

	Date start;

	PrintWriter preRoleRef = null;
	PrintWriter preLink = null;
	PrintWriter preLoc = null;
	PrintWriter preArc = null;

	// constructor method b/c same name as class name=>
	public SAXParserPre(String xmlFilePath, String acceptedDateStr,
			String form, String symbol, String year, String quarter) throws SQLException, FileNotFoundException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);

		acceptedDate = acceptedDateStr;
		accNo = null;
		ticker = symbol;
		formType = form;

//		String year = acceptedDate.substring(0,4);
//		int qtr = (Integer.parseInt(acceptedDate.replaceAll("-", "")
//				.substring(4, 6).replaceAll("^0", "")) + 2) / 3;
//		String q = qtr + "";
	
		String roleFileName = null;
		String linkFileName = null;
		String locFileName = null;
		String arcFileName = null;
		
		try {
			File filePath = new File(xmlFilePath);
			String filename = filePath.getName().substring(0,
					filePath.getName().lastIndexOf("."));
			filename.substring(0, 20);
			accNo = filename.substring(0, 20);
			Utils.createFoldersIfReqd(filePath.getParent() + "/csv/");

			roleFileName = filePath.getParent() + "/csv/" + filename
					+ "Role.csv";
			preRoleRef = new PrintWriter(roleFileName);

			linkFileName = filePath.getParent() + "/csv/" + filename
					+ "Link.csv";
			preLink = new PrintWriter(linkFileName);

			locFileName = filePath.getParent() + "/csv/" + filename + "Loc.csv";
			preLoc = new PrintWriter(locFileName);

			arcFileName = filePath.getParent() + "/csv/" + filename + "Arc.csv";
			preArc = new PrintWriter(arcFileName);

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
			preLink.close();
			if (null != preLink) {
				preLink.close();
				String query = "LOAD DATA INFILE '"
						+ linkFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+quarter+"_pre_Link FIELDS TERMINATED BY '||' "
						+ "(rowNo,accNo,acceptedDate,formType,its,pre_link_role,pre_link_role_name,pre_link_type,pre_link_title); ";

//				try {
					MysqlConnUtils.executeQuery(query);
//				} catch (SQLException e) {
//					e.printStackTrace(System.out);
//				}
				File f = new File(linkFileName.replaceAll("\\\\", "/"));
				 f.delete();
			}
			preRoleRef.close();
			if (null != preRoleRef) {
				preRoleRef.close();
				String query = " LOAD DATA INFILE '"
						+ roleFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+quarter+"_pre_RoleRef FIELDS TERMINATED BY '||' "
						+ "(rowNo,accNo,acceptedDate,pre_rr_roleURI,pre_rr_roleURI_name,pre_rr_href,pre_rr_type,pre_rr_title);";
				try {
					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
				File f = new File(roleFileName.replaceAll("\\\\", "/"));
				 f.delete();
			}
			preLoc.close();
			if (null != preLoc) {
				preLoc.close();
				String query = " LOAD DATA INFILE '"
						+ locFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+quarter+"_pre_Loc FIELDS TERMINATED BY '||'"
						+ " (rowNo,accNo,acceptedDate,pre_link_role_name,pre_loc_Label,pre_loc_href,pre_loc_href_prefix,pre_loc_href_Name,pre_loc_type,pre_loc_title); ";
				try {
					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
				File f = new File(locFileName.replaceAll("\\\\", "/"));
				 f.delete();
			}
			preArc.close();
			if (null != preArc) {
				preArc.close();
				String query = " LOAD DATA INFILE '"
						+ arcFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+quarter+"_pre_arc FIELDS TERMINATED BY '||' "
						+ "(rowNo,accNo,acceptedDate,`pre_link_Role_name`,`pre_arc_parent`,`pre_arc_child`,"
						+ "`pre_arc_preferredLabel`,`pre_arc_pLabel`,`pre_arc_order`,`"
						+ "pre_arc_type`,`pre_arc_arcRole`,`pre_arc_title`) ;";
				try {
					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
			}
		}
		File f = new File(arcFileName.replaceAll("\\\\", "/"));
		 f.delete();
	}

	/*
	 * gets called at the 'start' of each element.
	 */
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		
		StringBuilder sb= new StringBuilder();

		if (localName.equalsIgnoreCase("roleRef")) {
			roleRowNo++;
			String pre_rr_roleURI = attributes.getValue("roleURI");
			if (pre_rr_roleURI != null) {
				pre_roleURI_name = pre_rr_roleURI.substring(
						pre_rr_roleURI.lastIndexOf("/") + 1,
						pre_rr_roleURI.length());
			}
			String pre_rr_href = attributes.getValue("xlink:href");
			String pre_rr_type = attributes.getValue("xlink:type");
			String pre_rr_title = attributes.getValue("xlink:title");
//			preRoleRef.append(roleRowNo + "||" + accNo + "||" + acceptedDate
//					+ "||" + pre_rr_roleURI + "||"
//					+ pre_roleURI_name + "||" + pre_rr_href + "||"
//					+ pre_rr_type + "||" + pre_rr_title	+ "\n");
			
			sb.append(roleRowNo + "||" + accNo + "||" + acceptedDate + "||" + pre_rr_roleURI + "||" + pre_roleURI_name
					+ "||" + pre_rr_href + "||" + pre_rr_type + "||" + pre_rr_title);
			preRoleRef.append(sb.toString().replaceAll("[\r\n]+", "")+ "\n");
			sb.delete(0, sb.toString().length());

		} else if (localName.equalsIgnoreCase("presentationLink")) {
			linkRowNo++;
			pre_link_role = attributes.getValue("xlink:role");
			String pre_link_role_name = null;
			pre_link_role_name = pre_link_role.substring(
					pre_link_role.lastIndexOf("/") + 1, pre_link_role.length());

			String pre_link_type = attributes.getValue("xlink:type");
			String pre_link_title = attributes.getValue("xlink:title");
//			preLink.append(linkRowNo + "||" + accNo + "||" + acceptedDate + "||" + formType
//					+ "||" + ticker + "||" + pre_link_role + "||"
//					+ pre_link_role_name + "||" + pre_link_type
//					+ "||" + pre_link_title + "\n");
			
			sb.append(linkRowNo + "||" + accNo + "||" + acceptedDate + "||" + formType + "||" + ticker + "||"
					+ pre_link_role + "||" + pre_link_role_name + "||" + pre_link_type + "||" + pre_link_title);
			preLink.append(sb.toString().replaceAll("[\r\n]+", "")+ "\n");
			sb.delete(0, sb.toString().length());

			
		} else if (localName.equalsIgnoreCase("loc")) {
			locRowNo++;
			String pre_loc_label = attributes.getValue("xlink:label");
			String pre_loc_href = attributes.getValue("xlink:href");
			if (attributes.getValue("xlink:href") == null
					|| attributes.getValue("xlink:href").length() < 1) {
				pre_loc_href = pre_loc_href+"blank#ignore_";
			}

//			System.out.println("pre_loc_href:: " + pre_loc_href);
//			System.out
//					.println("pre_loc_href.length:: " + pre_loc_href.length());
			// String pre_loc_href_prefix = "";

			String pre_loc_href_prefix = "";
			if (pre_loc_href.indexOf("#") + 1 < pre_loc_href.indexOf("_"))
			pre_loc_href_prefix = pre_loc_href.substring(
					pre_loc_href.indexOf("#") + 1,
					pre_loc_href.indexOf("_"));

			String pre_loc_href_name = pre_loc_href.substring(
					pre_loc_href.lastIndexOf("_") + 1, pre_loc_href.length());
			String pre_loc_type = attributes.getValue("xlink:type");
			String pre_loc_title = attributes.getValue("xlink:title");
			pre_link_role_name = pre_link_role.substring(
					pre_link_role.lastIndexOf("/") + 1, pre_link_role.length());
//			preLoc.append(locRowNo + "||" + accNo + "||" + acceptedDate + "||"
//					+ pre_link_role_name + "||" + pre_loc_label + "||"
//					+ pre_loc_href + "||" + pre_loc_href_prefix + "||"
//					+ pre_loc_href_name + "||" + pre_loc_type + "||"
//					+ pre_loc_title + "\n");
			
			sb.append(locRowNo + "||" + accNo + "||" + acceptedDate + "||" + pre_link_role_name + "||" + pre_loc_label
					+ "||" + pre_loc_href + "||" + pre_loc_href_prefix + "||" + pre_loc_href_name + "||" + pre_loc_type
					+ "||" + pre_loc_title);
			preLoc.append(sb.toString().replaceAll("[\r\n]+", "")+ "\n");
			sb.delete(0, sb.toString().length());

		} else if (localName.equalsIgnoreCase("presentationArc")) {
			arcRowNo++;
			String pre_arc_parent = attributes.getValue("xlink:from");
			String pre_arc_child = attributes.getValue("xlink:to");
			String pre_arc_preferredLabel = attributes
					.getValue("preferredLabel");
			String pre_arc_pLabel = null;
			pre_link_role_name = pre_link_role.substring(
					pre_link_role.lastIndexOf("/") + 1, pre_link_role.length());
			if (pre_arc_preferredLabel != null) {
				pre_arc_pLabel = pre_arc_preferredLabel.substring(
						pre_arc_preferredLabel.lastIndexOf("/") + 1,
						pre_arc_preferredLabel.length());
			}
			String pre_arc_order = attributes.getValue("order");
			String pre_arc_arcRole = attributes.getValue("xlink:arcrole");
			String pre_arc_type = attributes.getValue("xlink:type");
			String pre_arc_title = attributes.getValue("xlink:title");
//			preArc.append(arcRowNo + "||" + accNo + "||" + acceptedDate + "||"
//					+ pre_link_role_name + "||" + pre_arc_parent
//					+ "||" + pre_arc_child + "||"
//					+ pre_arc_preferredLabel + "||" + pre_arc_pLabel
//					+ "||" + pre_arc_order + "||" + pre_arc_type
//					+ "||" + pre_arc_arcRole + "||" + pre_arc_title
//					+ "\n");

			sb.append(arcRowNo + "||" + accNo + "||" + acceptedDate + "||" + pre_link_role_name + "||" + pre_arc_parent
					+ "||" + pre_arc_child + "||" + pre_arc_preferredLabel + "||" + pre_arc_pLabel + "||"
					+ pre_arc_order + "||" + pre_arc_type + "||" + pre_arc_arcRole + "||" + pre_arc_title);
			preArc.append(sb.toString().replaceAll("[\r\n]+", "") + "\n");
			sb.delete(0, sb.toString().length());

		}
	}

	/**
	 * gets called at the 'end' of each element.
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

	}

	/**
	 * Use it to receive the node value: i.e. <nodeName>value</nodeName>
	 */
	public void characters(char ch[], int start, int length)
			throws SAXException {
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

	public static void main(String[] arg) throws SQLException, FileNotFoundException {
		 new SAXParserPre(
				 
		 "c:/backtest/xbrl/2015/QTR1/abt-20141231_pre.xml",
		 "20130101","10-K","ABT","2015", "1");

	}
}
