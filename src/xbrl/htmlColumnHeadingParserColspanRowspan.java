package xbrl;

import java.io.IOException;
import java.sql.SQLException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

//put in nlp if it works
public class htmlColumnHeadingParserColspanRowspan {
	
	public void htmlColumnHeadingParser() throws SQLException, IOException{
		//put colheading rows here
		//this pickups at point ch data rows are identified
//		NLP nlp = new NLP();
//		TableParser tp = new TableParser();
//		Document doc = Jsoup.parseBodyFragment(Utils
//				.readTextFromFile("c:/temp/htmlCH.html").replaceAll("(?i)<TH", "<td").replaceAll("(?i)</TH", "</td"));
		Document doc = Jsoup.parseBodyFragment(Utils
				.readTextFromFile("c:/temp/t.html").replaceAll("(?i)<TH", "<td").replaceAll("(?i)</TH", "</td"));

		Elements chRows = new Elements();
		
		Elements TRs = doc.getElementsByTag("tr");
		Element tr, td ;
		Elements TDs;
		int rowspan=0,colspan=0, lastCount=0; 
		// assign a col idx loc for each td equal to lastCount+(colspan-1) where
		// lastCount equals td count in tr unless rowspan
		// then ignore next number of tr equal to (rowspan-1). lastCount reset
		// each tr.

		for (int i = 0; i < 4/*TRs.size()*/; i++) {
			
			if (rowspan == 0) {
				lastCount = 0;
			}

			tr = TRs.get(i);
			TDs = tr.getElementsByTag("td");
			
			
			for (int n = 0; n < TDs.size(); n++) {
				td = TDs.get(n);
				lastCount = lastCount+1;
				colspan =0;
				if(td.attr("colspan").length()>0){
					colspan = Integer.parseInt(td.attr("colspan"));
					lastCount = lastCount+(colspan-1);
				}
				
				if (rowspan!=1 && td.attr("rowspan").length() > 0) {
					rowspan = Integer.parseInt(td.attr("rowspan")) - 1;
					System.out.println("rowspan="+rowspan+" td.text="+td.text());
				}

				if(td.text().length()>0){
					System.out.println("lastCount="+lastCount+" td.text()="+td.text());
				}
				
			}

			chRows.add(tr);
			//perform cnt - but don't resent at next Tr if rowspan
			
		}
	}

	
	public static void main(String[] args) throws SQLException, IOException {
		htmlColumnHeadingParserColspanRowspan h = new htmlColumnHeadingParserColspanRowspan();
		
		h.htmlColumnHeadingParser();
		
	}
}
