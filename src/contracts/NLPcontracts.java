package contracts;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import xbrl.NLP;
import xbrl.Utils;

public class NLPcontracts {


	/*
	 * 1: U.S. Bank Trust National Association 2: The Bank of New York 3:
	 * Computershare Trust Company of Canada 4: WELLS FARGO BANK, NATIONAL
	 * ASSOCIATION 5: WILMINGTON TRUST COMPANY 5: THE BANK OF NEW YORK MELLON 6:
	 * ZIONS FIRST NATIONAL BANK 6: Deutsche Bank National Trust Company
	 */
	
	/*
	 * 1: in its capacity as trustee 2: as trustee 2: acting in its capacity as
	 * trustee 3: [(] not in its individual capacity[,] but solely as [trustee]
	 */	
	
//	WILMINGTON TRUST COMPANY,
//	 as Owner Trustee
//	 
//	and Computershare Trust Company of Canada, as Trustee (the "Trustee").  The 
	//must all be on 1 line:
	//TODO: fix f/p == not contain 'at the' || Company and 
	public static Pattern patternTrustee = Pattern.compile("(?sm)([A-Z]{1}[\\.A-Za-z]{1,18}[ \r\n]{1,2})"
			+ "(([A-Z]{1}[\\.A-Za-z]{1,15}|[a-z]{1,3})[ \r\n]{1,2})?"
			+ "(([A-Z]{1}[\\.A-Za-z]{1,15}|[a-z]{1,3})[ \r\n]{1,2})?"
			+ "(([A-Z]{1}[\\.A-Za-z]{1,15}|[a-z]{1,3})[ \r\\n]{1,2})?"
			+ "(([A-Z]{1}[\\.A-Za-z]{1,15}|[a-z]{1,3})[ \r\n]{1,2})?"
			+ "(([A-Z]{1}[\\.A-Za-z]{1,15}|[a-z]{1,3}))"
			+ "[\r\n,]{0,6}[, ]{0,40}[, ]{1,2}"
			+ "(as|AS) ((Delaware |Owner |Indenture |DELAWARE |OWNER |INDENTURE )?(TRUSTEE|Trustee))");
	
//			+ "[,\r\n]{1,10}[ ]{1,50}[, ]{1,2}(as "
//			+ "(Delaware |Owner |Indenture )?Trustee|AS (DELAWARE |OWNER |INDENTURE )?TRUSTEE) ?(\\(the \"Trustee\"\\))?");
	
	
	public static 	Pattern patternGetBondNameAndInterestRateAndMaturity = Pattern
			.compile("[\\d]{1,2}\\.[\\d]{1,2}\\%( [A-Z]{1}[a-z]{1,20})+.*?( [dD]ue|[mM]atur.*?) [12][09][\\d]{2}");
	
	public static Pattern patternParties = Pattern.compile(" (\\b([A-Z]\\w*)\\b |[a-z]{1,3} )+\\(the \".*\"\\)");
//			+ "[ ,](AS|as)"
//			+ " (\b[A-Z]{1}[A-Za-z]{1,30} )+\\(the \".*\"");
	

	// 1 - deviation - f/k/a - remove. remove -->, a . . .. ., as trustee (the "Trustee")
	// and THE BANK OF NEW YORK MELLON (formerly known as The Bank of New York), a
	// New York banking corporation, as trustee (the "Trustee")

	// THE BANK OF NEW YORK MELLON, a New York banking corporation, as trustee
	// (herein called the "Trustee")
	
	public static void main(String[] args) throws IOException, SQLException {
		
		String text = "";/*Utils.readTextFromFile("c:/temp/test.txt");*/
		// text = text.replaceAll("xxPD", "\\.");
		// System.out.println("text.len="+text.length());

		// TODO: Machine learning: run simple query in Solr - 'as Trustee' -- see before
		// and after text. Select those that are matches. put them in a 'blob' pattern.
		// Automate the process of pattern inclusion when I select. To find other
		// Trustees - use solr to search definitions. Use that text to also develop
		// queries for machine learning and later pattern blobs.
		
		NLP nlp = new  NLP();
		
		List<String[]>list = new ArrayList<String[]>(); 
				
		
		File folder = new File("c:/temp2/");
		File[] listOfFiles = folder.listFiles();
		for (int c = 0; c < listOfFiles.length; c++) {
			System.out.println("file=" + listOfFiles[c]);
			text = Utils.readTextFromFile(listOfFiles[c].getAbsolutePath());
//			text = Utils.readTextFromFile("c:/temp3/tmp.txt");
			text = text.replaceAll("xxPD", "\\.");
//			System.out.println("text.len="+text.length());
			list = nlp.getAllStartIdxLocsAndMatchedGroups(text, patternTrustee);
//			list = nlp.getAllStartIdxLocsAndMatchedGroups(text, patternParties);
			
			for (int i = 0; i < list.size(); i++) {
//				System.out.println("text.len="+text.length());
				System.out.println("i=" + i + " Tee=" + list.get(i)[1] + "||");
				int sIdx = Integer.parseInt(list.get(i)[0]);
				int mn = Math.max(0, sIdx - 80);
				int mx = Math.min(text.length(), sIdx + 870);
//					System.out.println("surrounding tee text=" + text.substring(mn, mx));
			}
		}
	}
//	TODO: clean up: tee="New York,|NATIONAL ASSOCIATION,"
}
