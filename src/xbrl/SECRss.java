package xbrl;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class SECRss {

	private static String secSite = "https://www.sec.gov/cgi-bin/browse-edgar?company=&dateb=&output=atom&CIK=&action=getcurrent&type=";

	// https://www.sec.gov/cgi-bin/browse-edgar?company=&CIK=&type=
	// &owner=only&count=100&action=getcurrent
	// https://www.sec.gov/cgi-bin/browse-edgar?company=&CIK=&type=
	// =include&count=100&a8-k&ownerction=getcurrent
	public static void insiderRss() throws Exception {
		String url = secSite + "&owner=only";
		downloadAndParse(url, "insider");
		// https://www.sec.gov/cgi-bin/browse-edgar?action=getcurrent&type=&company=&dateb=&owner=only&start=0&count=100&output=atom
		// https://www.sec.gov/cgi-bin/browse-edgar?action=getcurrent&type=&company=&dateb=&owner=only&start=100&count=100&output=atom
	}

	public static void eightKRss() throws Exception {
		String url = secSite + "8-K&owner=exclude";
		downloadAndParse(url, "8k");
	}

	private static void downloadAndParse(String url, String type) {

		int start = 0;
		int count = 100;
		String url2Go;
		int idx = "https://www.sec.gov/Archives/edgar/data/".length();
		String acc;
		while (true) {
			url2Go = url + "&start=" + start + "&count=" + count;
			System.out.println("url2Go::" + url2Go);
			// download the xml/rss
			try {
				String xml = getPageHtml(url2Go);
				// get all entry/link@href
				Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
				// entry is the tag name for the element (the 'record' which
				// contains updated, href, etc.).
				Elements entries = doc.getElementsByTag("entry");
				// each Entry element is the record. when we get to the page
				// after the last page sec.gov returns no entries - so this
				// element 'entries' will be empty and the 'break' will get us
				// out of the while loop and stop the method.
				if (entries.size() == 0)
					break;
				
				for (Element entry : entries) {
					String href = entry.getElementsByAttribute("href").get(0)
							.attr("href");
					idx = href.lastIndexOf("/") + 1;
					acc = href.substring(idx, idx + 20);

					if (type.equals("insider")) {
						if(href==null || acc==null)
							continue;
						InsiderParser.insiderRSS(href, acc);
						// System.out.println("href2:: "+ href);
					}

					else if (type.equals("8k")) {
						// EightK.xxxxx(href, acc);
					}

					// System.out.println(href);
				}
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
			start += count;
		}
	}

	
	public static String getPageHtml(String pageUrl) throws IOException {
		EasyHttpClient httpClient = new EasyHttpClient(false);
		return httpClient.makeHttpRequest("Get", pageUrl, null, -1, null, null);
	}

	
	public static void main(String[] args) throws Exception {
		
		SECRss.insiderRss();
		
	}
}
