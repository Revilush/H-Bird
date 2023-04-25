package xbrl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
//import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class SAXParserDef extends DefaultHandler {

	private String def_Link_role = null;
	private String def_Link_Role_Name = null;

	PrintWriter defRoleRef = null;
	PrintWriter defLink = null;
	PrintWriter defLoc = null;
	PrintWriter defArc = null;
	PrintWriter defArcRole = null;

	private String acceptedDate = null;
	private String accNo = null;
	private int arcRoleRowNo = 0;
	private int roleRowNo = 0;
	private int linkRowNo = 0;
	private int locRowNo = 0;
	private int arcRowNo = 0;

	// constructor method b/c same name as class name=>
	public SAXParserDef(String xmlFilePath, String acceptedDateStr, String year, String q) throws SQLException, FileNotFoundException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);

		String roleFileName = null;
		String linkFileName = null;
		String locFileName = null;
		String arcFileName = null;
		String arcRoleFileName = null;

		acceptedDate = acceptedDateStr;
		accNo = null;
		// String year = acceptedDate.substring(0,4);
		// int qtr = (Integer.parseInt(acceptedDate.replaceAll("-", "")
		// .substring(4, 6).replaceAll("^0", "")) + 2) / 3;
		//
		// String q = qtr+"";
		System.out.println("def q="+q+"\racceptedDate="+acceptedDate);
		

		try {
			File filePath = new File(xmlFilePath);
			String filename = filePath.getName().substring(0,
					filePath.getName().lastIndexOf("."));
			filename.substring(0, 20);
			accNo = filename.substring(0, 20);;
			Utils.createFoldersIfReqd(filePath.getParent() + "/csv/");

			roleFileName = filePath.getParent() + "/csv/" + filename
					+ "Role.csv";
			defRoleRef = new PrintWriter(roleFileName);

			linkFileName = filePath.getParent() + "/csv/" + filename
					+ "Link.csv";
			defLink = new PrintWriter(linkFileName);

			locFileName = filePath.getParent() + "/csv/" + filename + "Loc.csv";
			defLoc = new PrintWriter(locFileName);

			arcFileName = filePath.getParent() + "/csv/" + filename + "Arc.csv";
			defArc = new PrintWriter(arcFileName);

			arcRoleFileName = filePath.getParent() + "/csv/" + filename
					+ "arcRole.csv";
			defArcRole = new PrintWriter(arcRoleFileName);

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
			defRoleRef.close();
			if (null != defRoleRef) {
				defRoleRef.close();
				String query = "LOAD DATA INFILE '"
						+ roleFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+q+"_def_RoleRef FIELDS TERMINATED BY '||' "
						+ "(rowNo,accNo,acceptedDate,def_rr_roleURI,def_rr_roleURI_Name,def_rr_href,def_rr_type,def_rr_title); ";

//				try {
					MysqlConnUtils.executeQuery(query);
//				} catch (SQLException e) {
//					e.printStackTrace(System.out);
//
//				}
				File f = new File(roleFileName.replaceAll("\\\\", "/"));
				f.delete();
			}
			defLink.close();
			if (null != defLink) {
				defLink.close();
				String query = "LOAD DATA INFILE '"
						+ linkFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+q+"_def_Link FIELDS TERMINATED BY '||' "
						+ "(rowNo,accNo,acceptedDate,def_Link_role,def_Link_Role_Name,def_Link_type,def_Link_title); ";
				try {
					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
				File f = new File(linkFileName.replaceAll("\\\\", "/"));
				f.delete();
			}
			defLoc.close();
			if (null != defLoc) {
				defLoc.close();

				String query = "LOAD DATA INFILE '"
						+ locFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+q+"_def_Loc FIELDS TERMINATED BY '||'"
						+ " (rowNo,accNo,acceptedDate,def_Link_Role_Name,def_Loc_Label,def_Loc_href,def_Loc_href_prefix,def_Loc_href_name,def_Loc_type,def_Loc_title); ";
				try {
					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
				File f = new File(locFileName.replaceAll("\\\\", "/"));
				f.delete();
			}
			defArc.close();
			if (null != defArc) {
				defArc.close();

				String query = "LOAD DATA INFILE '"
						+ arcFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+q+"_def_arc FIELDS TERMINATED BY '||' "
						+ "(rowNo,accNo,acceptedDate,`def_Link_Role_Name`,`def_arc_parent`,`def_arc_child`,`def_arc_order`,"
						+ "`def_arc_type`,`def_arc_arcRole`,`def_arc_title`) ;";

				try {
					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
				File f = new File(arcFileName.replaceAll("\\\\", "/"));
				f.delete();
			}
			defArcRole.close();
			if (null != defArcRole) {
				defArcRole.close();

				String query = "LOAD DATA INFILE '"
						+ arcRoleFileName.replaceAll("\\\\", "/")
						+ "' ignore INTO TABLE TMPxbrl_"+year+"Q"+q+"_def_ArcRole FIELDS TERMINATED BY '||'"
						+ " (rowNo,accNo,acceptedDate,def_arcrr_roleURI,def_arcrr_roleURI_Name,def_arcrr_href,"
						+ "def_arcrr_href_name,def_arcrr_type,def_arcrr_title); ";

				try {
					MysqlConnUtils.executeQuery(query);
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
				File f = new File(arcRoleFileName.replaceAll("\\\\", "/"));
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
		
		if (localName.equalsIgnoreCase("roleRef")) {
			roleRowNo++;
			String def_rr_roleURI = attributes.getValue("roleURI");
			String def_rr_href = attributes.getValue("xlink:href");
			String def_rr_type = attributes.getValue("xlink:type");
			String def_rr_title = attributes.getValue("xlink:title");
			String def_rr_roleURI_Name = def_rr_roleURI.substring(
					def_rr_roleURI.lastIndexOf("/") + 1,
					def_rr_roleURI.length());
//			defRoleRef.println(roleRowNo + "||" + accNo + "||" + acceptedDate + "||"
//					+ def_rr_roleURI + "||" + def_rr_roleURI_Name
//					+ "||" + def_rr_href + "||" + def_rr_type + "||"
//					+ def_rr_title);
			sb.append(roleRowNo + "||" + accNo + "||" + acceptedDate + "||"
					+ def_rr_roleURI + "||" + def_rr_roleURI_Name
					+ "||" + def_rr_href + "||" + def_rr_type + "||"
					+ def_rr_title);
			defRoleRef.println(sb.toString().replaceAll("[\r\n]+", ""));
			sb.delete(0, sb.toString().length());
			
			
		} else if (localName.equalsIgnoreCase("arcroleRef")) {
			arcRoleRowNo++;
			
			String def_arcrr_roleURI = attributes.getValue("arcroleURI");
			String def_arcrr_roleURI_Name = def_arcrr_roleURI.substring(
					def_arcrr_roleURI.lastIndexOf("/") + 1,
					def_arcrr_roleURI.length());
			String def_arcrr_href = attributes.getValue("xlink:href");
			String def_arcrr_href_name = def_arcrr_href.substring(
					def_arcrr_href.lastIndexOf("#") + 1,
					def_arcrr_href.length());

			String def_arcrr_type = attributes.getValue("xlink:type");
			String def_arcrr_title = attributes.getValue("xlink:title");
//			defArcRole.println(arcRoleRowNo + "||" + accNo + "||"
//					+ acceptedDate + "||" + def_arcrr_roleURI + "||"
//					+ def_arcrr_roleURI_Name + "||" + def_arcrr_href
//					+ "||" + def_arcrr_href_name + "||"
//					+ def_arcrr_type + "||" + def_arcrr_title);
			
			sb.append(arcRoleRowNo + "||" + accNo + "||" + acceptedDate + "||" + def_arcrr_roleURI + "||"
					+ def_arcrr_roleURI_Name + "||" + def_arcrr_href + "||" + def_arcrr_href_name + "||"
					+ def_arcrr_type + "||" + def_arcrr_title);
			defArc.println(sb.toString().replaceAll("[\r\n]+", ""));
			sb.delete(0, sb.toString().length());
			
		} else if (localName.equalsIgnoreCase("definitionLink")) {
			
			linkRowNo++;
			def_Link_role = attributes.getValue("xlink:role");
			def_Link_Role_Name = null;
			if (def_Link_role != null) {
				def_Link_Role_Name = def_Link_role.substring(
						def_Link_role.lastIndexOf("/") + 1,
						def_Link_role.length());
			}
			String def_link_type = attributes.getValue("xlink:type");
			String def_link_title = attributes.getValue("xlink:title");

//			defLink.println(linkRowNo + "||" + accNo + "||" + acceptedDate + "||"
//					+ def_Link_role + "||" + def_Link_Role_Name
//					+ "||" + def_link_type + "||" + def_link_title);
			
			sb.append(linkRowNo + "||" + accNo + "||" + acceptedDate + "||" + def_Link_role + "||" + def_Link_Role_Name
					+ "||" + def_link_type + "||" + def_link_title);
			defLink.println(sb.toString().replaceAll("[\r\n]+", ""));
			sb.delete(0, sb.toString().length());
			
		} else if (localName.equalsIgnoreCase("loc")) {
			locRowNo++;
			String def_loc_label = attributes.getValue("xlink:label");
			String def_loc_href = attributes.getValue("xlink:href");
			String def_loc_href_prefix = "";
			if(def_loc_href.indexOf("#")+1<def_loc_href.indexOf("_"))
			def_loc_href_prefix = def_loc_href.substring(
					def_loc_href.lastIndexOf("#") + 1,
					def_loc_href.indexOf("_"));
			String def_loc_href_name = def_loc_href.substring(
					def_loc_href.lastIndexOf("_") + 1, def_loc_href.length());
			String def_loc_type = attributes.getValue("xlink:type");
			String def_loc_title = attributes.getValue("xlink:title");
//			defLoc.println(locRowNo + "||" + accNo + "||" + acceptedDate + "||"
//					+ def_Link_Role_Name + "||" + def_loc_label + "||"
//					+ def_loc_href + "||" + def_loc_href_prefix + "||"
//					+ def_loc_href_name + "||" + def_loc_type + "||"
//					+ def_loc_title);
			
			sb.append(locRowNo + "||" + accNo + "||" + acceptedDate + "||" + def_Link_Role_Name + "||" + def_loc_label
					+ "||" + def_loc_href + "||" + def_loc_href_prefix + "||" + def_loc_href_name + "||" + def_loc_type
					+ "||" + def_loc_title);
			defLoc.println(sb.toString().replaceAll("[\r\n]+", ""));
			sb.delete(0, sb.toString().length());

		} else if (localName.equalsIgnoreCase("definitionArc")) {
			arcRowNo++;
			String def_arc_parent = attributes.getValue("xlink:from");
			String def_arc_child = attributes.getValue("xlink:to");
			// String def_arc_preferredLabel = attributes
			// .getValue("preferredLabel");
			String def_arc_order = attributes.getValue("order");
			String def_arc_arcRole = attributes.getValue("xlink:arcrole");
			String def_arc_type = attributes.getValue("xlink:type");
			String def_arc_title = attributes.getValue("xlink:title");
//			defArc.println(arcRowNo + "||" + accNo + "||" + acceptedDate + "||"
//					+ def_Link_Role_Name + "||" + def_arc_parent
//					+ "||" + def_arc_child + "||" + def_arc_order
//					+ "||" + def_arc_type + "||" + def_arc_arcRole
//					+ "||" + def_arc_title);
			
			sb.append(arcRowNo + "||" + accNo + "||" + acceptedDate + "||" + def_Link_Role_Name + "||" + def_arc_parent
					+ "||" + def_arc_child + "||" + def_arc_order + "||" + def_arc_type + "||" + def_arc_arcRole + "||"
					+ def_arc_title);

			defArc.println(sb.toString().replaceAll("[\r\n]+", ""));
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
	}

	public void endDocument() throws SAXException {

	}

	public static void main(String[] arg) throws SQLException, FileNotFoundException {
		new SAXParserDef(
				"c:/backtest/xbrl/2013/QTR2/0001193125-13-222126_nmh-20130331_def.xml",
				"acceptedDateStr","2013","2");

	}
}
