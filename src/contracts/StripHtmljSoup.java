package contracts;

import java.io.File;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import xbrl.Utils;

public class StripHtmljSoup {

	private void printAllNodes(String txt, Elements eles) {
		System.out.println(txt);
		for (Element e : eles)
			System.out.println(e.text());
	}
	
	private void printTable(String txt, Element table) {
		Elements TDs, TRs = table.select("TR");
		for (Element tr : TRs) {
			TDs = tr.select("TD");
			System.out.println();
			for (Element td : TDs) {
				System.out.print(td.text());
				System.out.print("\t");
			}
		}
		System.out.println();
	}
	
	public static String table2Text(Element table, String rowSeperator, String cellSeperator) {
		Elements TDs, TRs = table.select("TR");
		StringBuilder sb = new StringBuilder();
		for (Element tr : TRs) {
			TDs = tr.select("TD");
			sb.append(rowSeperator);
			for (Element td : TDs) {
				sb.append(td.text()).append(cellSeperator);
			}
		}
		return sb.toString();
	}

	public static String convertHtml2Text(String html, boolean retainNewLinesBrAndP) {
		//create Jsoup document from HTML
        Document jsoupDoc = Jsoup.parse(html);
        
        if (retainNewLinesBrAndP) {
            //set pretty print to false, so \n is not removed
            jsoupDoc.outputSettings(new OutputSettings().prettyPrint(false));
//            org.jsoup.parser.Parser.unescapeEntities(jsoupDoc.body().html(), true);
//            unescapeEntities
            //select all <br> tags and append \n after that
            jsoupDoc.select("br").after("\\n");
            
            //select all <p> tags and prepend \n before that
            jsoupDoc.select("p").before("\\n");
                    
            //select all <p> tags and prepend \n before that
            jsoupDoc.select("d").before("\\n");

            //get the HTML from the document, and retaining original new lines
            String str = jsoupDoc.html().replaceAll("\\\\n", "\n");
            
            html = Jsoup.clean(str, "", Whitelist.none(), new OutputSettings().prettyPrint(false));
            html = Parser.unescapeEntities(html, true);
        } else
        	html = jsoupDoc.text();
        
        return html;
		
	}
	
	
	public static void main(String[] arg) throws IOException {
		String html = Utils.readTextFromFile("c:/temp/tmpHtml.htm");		//"e:/pk/temp/s126467_485bpos.htm");
		Document doc = Jsoup.parse(html);
		Elements eles;
		
		//StripHtmljSoup sH = new StripHtmljSoup();
		
		// titles
		//Elements eles = doc.select("p[style~=font: ?24pt]");
		//bp.printAllNodes("titles::", eles);
		
		// headers
		//eles = doc.select("p[style~=font: ?10pt] b");
		//bp.printAllNodes("headers::", eles);
		
		// TABLEs of 'Shareholder Fees'
//		eles = doc.select("table:contains(Shareholder Fees)");
//		System.out.println(eles.size());
		//bp.printTable("shareholder fees", eles.get(0));
		
		// remove all <img....> tags
		doc.select("img").remove();
		
		//or you can pick ALL tables like   eles = doc.select("table");    
		eles = doc.select("table:contains(Original Lower-Tier)");
		System.out.println(eles.size());
		String tableHtml;
		for (Element table : eles) {
			tableHtml = "<p>"+table2Text(table, "<br>", " &nbsp; &nbsp;")+"</p>";
			table.before(tableHtml);
			table.remove();
		}
		
		
		Utils.writeTextToFile(new File("c:/temp/tmpHtml.txt"), convertHtml2Text(doc.outerHtml(), true)
				.replaceAll("\n", "\r\n")
//				.replaceAll("(?sm)[ ]+\r\n", "\r\n")
				.replaceAll("[\\x00-\\x1f\\x80-\\x9f&&[^\r\n]]", "")
//				.replaceAll("�", "\"")
//				.replaceAll("�", "\'")
//				.replaceAll("&nbsp;", "")//may be needed??
				
				);
		
		// *********************
		
//		String t = "<DIV style=\"color:#000000;font-family:Arial;font-size:10pt;font-style:Normal;font-weight:bold;line-height:13pt;margin-top:10pt;text-align:left;text-decoration:none;text-transform:none;\">Delivery of Shareholder Documents <FONT style=\"padding-left:1%;\"></FONT>&#8211;<FONT style=\"padding-left:1%;\"></FONT> Householding</DIV>";
//		doc = Jsoup.parseBodyFragment(t);
//		System.out.println(doc.select("div").text());
		
	}
	
	
	
	
}
