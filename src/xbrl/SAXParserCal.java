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

public class SAXParserCal extends DefaultHandler {

	private String cal_link_role = null;
	private String cal_link_role_name = null;

	private String acceptedDate = null;
	private String accNo = null;

	private int roleRowNo = 0;
	private int linkRowNo = 0;
	private int locRowNo = 0;
	private int arcRowNo = 0;

	Date start;

	PrintWriter calRoleRef = null;
	PrintWriter calLink = null;
	PrintWriter calLoc = null;
	PrintWriter calArc = null;

	// constructor method b/c same name as class name=>
	public SAXParserCal(String xmlFilePath, String acceptedDateStr,String year, String q) throws SQLException, FileNotFoundException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);

		acceptedDate = acceptedDateStr;
		accNo = null;
//		String year = acceptedDate.substring(0,4);
//		int qtr = (Integer.parseInt(acceptedDate.replaceAll("-", "")
//				.substring(4, 6).replaceAll("^0", "")) + 2) / 3;

//		String q = qtr+"";


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
			calRoleRef = new PrintWriter(roleFileName);

			linkFileName = filePath.getParent() + "/csv/" + filename
					+ "Link.csv";
			calLink = new PrintWriter(linkFileName);

			locFileName = filePath.getParent() + "/csv/" + filename + "Loc.csv";
			calLoc = new PrintWriter(locFileName);

			arcFileName = filePath.getParent() + "/csv/" + filename + "Arc.csv";
			calArc = new PrintWriter(arcFileName);

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
			calRoleRef.close();
			if (null != calRoleRef) {
				calRoleRef.close();
				String query = "LOAD DATA INFILE '"
						+ roleFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+q+"_cal_RoleRef FIELDS TERMINATED BY '||' "
						+ "(rowNo,accNo,acceptedDate,cal_rr_roleURI,cal_rr_roleURI_Name,cal_rr_href,cal_rr_type,cal_rr_title); ";

//				try {
					MysqlConnUtils.executeQuery(query);
//				} catch (SQLException e) {
//					e.printStackTrace(System.out);
//				}
				File f = new File(roleFileName.replaceAll("\\\\", "/"));
				f.delete();
			}
			calLink.close();
			if (null != calLink) {
				calLink.close();
				String query = "LOAD DATA INFILE '"
						+ linkFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+q+"_cal_Link FIELDS TERMINATED BY '||' "
						+ "(rowNo,accNo,acceptedDate,cal_link_role,cal_link_role_name,cal_link_type,cal_link_title); ";

				try {

					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
				File f = new File(linkFileName.replaceAll("\\\\", "/"));
				f.delete();
			}
			calLoc.close();
			if (null != calLoc) {
				calLoc.close();

				String query = "LOAD DATA INFILE '"
						+ locFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+q+"_cal_Loc FIELDS TERMINATED BY '||'"
						+ " (rowNo,accNo,acceptedDate,cal_link_role_name,cal_loc_Label,cal_loc_href,cal_loc_href_prefix"
						+ ",cal_loc_href_name,cal_loc_type,cal_loc_title); ";
				try {
					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
				File f = new File(locFileName.replaceAll("\\\\", "/"));
				f.delete();
			}
			calArc.close();
			if (null != calArc) {
				calArc.close();

				String query = "LOAD DATA INFILE '"
						+ arcFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+q+"_cal_arc FIELDS TERMINATED BY '||' "
						+ "(rowNo,accNo,acceptedDate,`cal_link_role_name`,`cal_arc_parent`,`cal_arc_child`,"
						+ "`cal_arc_order`,`cal_arc_weight`,`cal_arc_priority`,`cal_arc_use`,"
						+ "`cal_arc_type`,`cal_arc_arcRole`,`cal_arc_title`) ;";

				try {
					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
				File f = new File(arcFileName.replaceAll("\\\\", "/"));
				f.delete();
			}
		}
	}

	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		StringBuilder sb = new StringBuilder();

		if (localName.equalsIgnoreCase("roleRef")) {
			roleRowNo++;
			String cal_rr_roleURI = attributes.getValue("roleURI");
			String cal_rr_roleURI_Name = cal_rr_roleURI.substring(
					cal_rr_roleURI.lastIndexOf("/") + 1,
					cal_rr_roleURI.length());
			String cal_rr_href = attributes.getValue("xlink:href");
			String cal_rr_type = attributes.getValue("xlink:type");
			String cal_rr_title = attributes.getValue("xlink:title");
			
			sb.delete(0, sb.toString().length());
			sb.append(roleRowNo + "||" + accNo + "||" + acceptedDate
					+ "||" + cal_rr_roleURI + "||"
					+ cal_rr_roleURI_Name + "||" + cal_rr_href
					+ "||" + cal_rr_type + "||" + cal_rr_title);
			calRoleRef.println(sb.toString().replaceAll("[\r\n]+", ""));

//			calRoleRef.println(roleRowNo + "||" + accNo + "||" + acceptedDate
//					+ "||" + cal_rr_roleURI + "||"
//					+ cal_rr_roleURI_Name + "||" + cal_rr_href
//					+ "||" + cal_rr_type + "||" + cal_rr_title);
		}

		else if (localName.equalsIgnoreCase("calculationLink")) {
			linkRowNo++;
			cal_link_role = attributes.getValue("xlink:role");
			String cal_link_role_name = null;
			cal_link_role_name = cal_link_role.substring(
					cal_link_role.lastIndexOf("/") + 1, cal_link_role.length());
			String cal_link_type = attributes.getValue("xlink:type");
			String cal_link_title = attributes.getValue("xlink:title");
			
			sb.delete(0, sb.length());
			sb.append(linkRowNo + "||" + accNo + "||" + acceptedDate
					+ "||" + cal_link_role + "||"
					+ cal_link_role_name + "||" + cal_link_type
					+ "||" + cal_link_title);
			calLink.println(sb.toString().replaceAll("[\r\n]+", ""));

//			calLink.println(linkRowNo + "||" + accNo + "||" + acceptedDate
//					+ "||" + cal_link_role + "||"
//					+ cal_link_role_name + "||" + cal_link_type
//					+ "||" + cal_link_title);
		}

		else if (localName.equalsIgnoreCase("loc")) {
			locRowNo++;
			cal_link_role_name = null;
			cal_link_role_name = cal_link_role.substring(
					cal_link_role.lastIndexOf("/") + 1, cal_link_role.length());
			String cal_loc_label = attributes.getValue("xlink:label");
			String cal_loc_href = attributes.getValue("xlink:href");
			// System.out.println("cal_loc_href.len=" + cal_loc_href.length());
			// System.out.println("# idx loc" + cal_loc_href.indexOf("#") + " _ idx loc" + cal_loc_href.indexOf("#"));
			String cal_loc_href_prefix = "";
			if(cal_loc_href.indexOf("#")+1<cal_loc_href.indexOf("_"))
			cal_loc_href_prefix = cal_loc_href.substring(
					cal_loc_href.lastIndexOf("#") + 1,
					cal_loc_href.indexOf("_"));
			String cal_loc_href_name = cal_loc_href.substring(
					cal_loc_href.lastIndexOf("_") + 1, cal_loc_href.length());
			String cal_loc_type = attributes.getValue("xlink:type");
			String cal_loc_title = attributes.getValue("xlink:title");
			
			sb.delete(0, sb.toString().length());
			sb.append(locRowNo + "||" + accNo + "||" + acceptedDate + "||" + cal_link_role_name + "||" + cal_loc_label
					+ "||" + cal_loc_href + "||" + cal_loc_href_prefix + "||" + cal_loc_href_name + "||" + cal_loc_type
					+ "||" + cal_loc_title);
			calLoc.println(sb.toString().replaceAll("[\r\n]+", ""));

//			calLoc.println(locRowNo + "||" + accNo + "||" + acceptedDate + "||"
//					+ cal_link_role_name + "||" + cal_loc_label
//					+ "||" + cal_loc_href + "||"
//					+ cal_loc_href_prefix + "||" + cal_loc_href_name
//					+ "||" + cal_loc_type + "||" + cal_loc_title);

		} else if (localName.equalsIgnoreCase("calculationArc")) {
			arcRowNo++;
			cal_link_role_name = null;
			cal_link_role_name = cal_link_role.substring(
					cal_link_role.lastIndexOf("/") + 1, cal_link_role.length());
			String cal_arc_parent = attributes.getValue("xlink:from");
			String cal_arc_child = attributes.getValue("xlink:to");
			String cal_arc_order = attributes.getValue("order");
			String cal_arc_weight = attributes.getValue("weight");
			String cal_arc_priority = attributes.getValue("priority");
			String cal_arc_use = attributes.getValue("use");
			String cal_arc_arcRole = attributes.getValue("xlink:arcrole");
			String cal_arc_type = attributes.getValue("xlink:type");
			String title = attributes.getValue("xlink:title");

			sb.delete(0, sb.toString().length());
			sb.append(arcRowNo + "||" + accNo + "||" + acceptedDate + "||" + cal_link_role_name + "||" + cal_arc_parent
					+ "||" + cal_arc_child + "||" + cal_arc_order + "||" + cal_arc_weight + "||" + cal_arc_priority
					+ "||" + cal_arc_use + "||" + cal_arc_type + "||" + cal_arc_arcRole + "||" + title);
			calArc.println(sb.toString().replaceAll("[\r\n]+", ""));

//			calArc.println(arcRowNo + "||" + accNo + "||" + acceptedDate + "||"
//					+ cal_link_role_name + "||" + cal_arc_parent
//					+ "||" + cal_arc_child + "||" + cal_arc_order
//					+ "||" + cal_arc_weight + "||"
//					+ cal_arc_priority + "||" + cal_arc_use + "||"
//					+ cal_arc_type + "||" + cal_arc_arcRole + "||"
//					+ title);
		}
	}

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
		new SAXParserCal(
				"c:/backtest/xbrl/2013/QTR3/0001091818-13-000355_gler-20130531_cal.xml","acceptedDateStr","2013","3");
	}
}
