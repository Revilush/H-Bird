package contracts;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrServerException;

import xbrl.ContractNLP;
import xbrl.ContractParser;
import xbrl.NLP;
import xbrl.Utils;

public class Regex {
	
	public static Pattern patternDatedMoDayYearLong = Pattern.compile("(?ism)(?<=dated.{1,15})("
			+ "Jan\\.? ?|Feb\\.? ?|Mar\\.? ?|Apr\\.? ?|May\\.? ?|Jun\\.? ?|Jul\\.? ?|Aug\\.? ?|Sep\\.? ?|Oct\\.? ?|Nov\\.? ?|Dec\\.? ?"
			+ ")" + "([A-Za-z]{0,10})" + " [\\d]{1,2}" + ", [12]{1}[09]{1}[\\d]{2}");

	public static Pattern patternInterest360 = Pattern.compile("36[056]{1}");
	public static Pattern patternActual = Pattern.compile("actual (number of days|days) elapsed|number of days actually elapsed"
			+ "|actual number of days");
	public static Pattern patternInterestDayCountMonth = Pattern.compile("(?ism) twelve 30-? ?(calendar )?days? months?"
			+ "|twelve.{1,4}12.{1,4}thirty.{1,4}30.{1,4}day.{1,4}months?");

	public static Pattern patternInterestCalcMethod = Pattern.compile(
			patternInterestDayCountMonth + "|" + patternInterest360.pattern() + "|" + patternActual.pattern());

	public static Pattern patternMoDay = Pattern.compile("(?ism)("
			+ "Jan\\.? ?|Feb\\.? ?|Mar\\.? ?|Apr\\.? ?|May\\.? ?|Jun\\.? ?|Jul\\.? ?|Aug\\.? ?|Sep\\.? ?|Oct\\.? ?|Nov\\.? ?|Dec\\.? ?"
			+ ")" + "([A-Za-z]{0,10})" + " [\\d]{1,2}");

	public static Pattern patternFrequency = Pattern.compile("(?ism)semi.{0,1}annual[ly]{0,2}|quarter[ly]{0,2}|each (calendar )?month"
			+ "|monthly");

	public static Pattern patternInterestPaymentDatesFrequency = Pattern.compile(
			patternMoDay + "|" + patternFrequency.pattern());

	public static String getDateOfAgreement(String text) {

//		TO RUN FROM A MAIN METHOD====>
//		NLP nlp = new NLP();
//		String text = Utils.readTextFromFile("d:/getContracts/temp/openingParas.txt");
//
//		List<String> listOfOpenParas = nlp.getAllMatchedGroups(text, Pattern.compile("(?sm)\"txt\".*?(?=\"txt\")"));
////		NLP.printListOfString("", listOfOpenParas);
//		System.out.println(listOfOpenParas.size());
//		StringBuilder sb = new StringBuilder();
//		sb.append("<html>");
//		for (int i = 0; i < listOfOpenParas.size(); i++) {
//
//			System.out.println("listOfOpenParas.get(i)" + listOfOpenParas.get(i));
//			sb.append(getDateOfAgreement(listOfOpenParas.get(i))+"<br><br>");
//		}
//		sb.append("</html>");
//		
//		File fn = new File("d:/getContracts/temp/openingParasDate.html");
//		Utils.writeTextToFile(fn, sb.toString());
//<===  TO RUN FROM A MAIN METHOD

		NLP nlp = new NLP();

		List<String[]> listOfDates = nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(text, patternDatedMoDayYearLong);
		System.out.println("listOfDates.size=" + listOfDates.size());
		int eIdx = 0, sIdx = 0;
		String date = "";
		for (int i = 0; i < listOfDates.size(); i++) {
			sIdx = Integer.parseInt(listOfDates.get(i)[1]);
			eIdx = Integer.parseInt(listOfDates.get(i)[2]);
			date = text.substring(sIdx, eIdx);
			System.out.println("date==" + date);
			text = text.replaceAll("(" + date + ")", "<b>" + date + "</b>");
		}

		return text;
	}
	

	public static List<String[]> getMetadata(String text, Pattern pattern) {
		
		NLP nlp =  new NLP();
		
		List<String[]> list= nlp.getAllMatchedGroupsAndStartAndEndIdxLocations(text, pattern);
		List<String[]> listBold = new ArrayList<String[]>();
		int eIdx =0, sIdx=0; String str ="";
		for(int i=0; i<list.size(); i++) {
			sIdx = Integer.parseInt(list.get(i)[1]);
			eIdx = Integer.parseInt(list.get(i)[2]);
			str = text.substring(sIdx,eIdx);
			String [] ary= {str,sIdx+"",eIdx+""};
			listBold.add(ary);
		}
		
		return listBold;
	}
	
	public static String boldMetadata(List<String[]> list, String text) throws IOException {

		String entity = "",sIdxStr = "", eIdxStr = "", str="",strPrior ="", gap ="";
		int eIdx =0, sIdx =0, eIdxPrior=0;
		for(int i=0; i<list.size(); i++) {
			
			entity = list.get(i)[0];
			sIdxStr = list.get(i)[1];
			eIdxStr = list.get(i)[2];
//			System.out.println("entity to bold="+entity);
			sIdx = Integer.parseInt(sIdxStr);//start of entity
			eIdx = Integer.parseInt(eIdxStr);//end of entity
			
			gap = text.substring(eIdxPrior,sIdx);//gap
//			System.out.println("eIdxPrior="+eIdxPrior+" sIdx="+sIdx);
//			System.out.println("1 str="+str);
			entity = "<b>"+entity+"</b>";
			entity=entity
					.replaceAll(", </b>", "</b>, ")
					.replaceAll("<b>\\), ", "\\), <b>")
					.replaceAll("\\)</b>","</b>\\)")
					.replaceAll("<b> ", " <b>")
					;
			str = strPrior + gap+entity;
					;
//			System.out.println("2 str="+str);
			
			strPrior = str;
			eIdxPrior = eIdx;

		}
		
		str = str+text.substring(eIdx,text.length());
		
		return str;
	}
	


	public static void main(String[] args) throws IOException, SQLException, SolrServerException {

		NLP nlp = new NLP();
		String text = Utils.readTextFromFile("c:/temp/testItWorks.txt");
		Pattern patternHdg1 = Pattern.compile("(?ism)" + "((?<=[\r\n]{2}[ ]{0,3})" + "(Qx).{1," + 175 + "}?(xq)"
				+ "((\\.|:|(?= ?- ?))|(?=(\r\n)))" + ")"

				+ "|"

				+ "(?<=[\r\n]{2} ?)(\\(?[\\dA-Za-z]{1,3}" + "(" + "\\.\\d?\\d?\\.?\\d?|\\)" + ") "
				+ ")? ? ?\\[?[A-Z].{1," + 175 + "}?((\\.|:)|(?=(\r\n)))" + "");

		Pattern patternHdg2 = Pattern.compile(
				"(?<=\r\n)(\\([a-zA-Z\\d]{1,2}\\)|[\\d\\.]{2,4}).{1,3}[A-Z]{1}.{4," + 175 + "}?(\r\n|[\\.:]{1})");

		String paraNmbStr = "((?<=[\r\n]{2}[ ]{0,10})" + "(" + "(SECTION|Section)( (\\(?[\\d]+\\)?)\\.?"
				+ "(\\(?[\\d]+\\.?\\d?\\d?|\\(?[a-zA-Z]{1,4}[\\)\\.]{1,2})? ?)" + "|(\\(?[a-zA-Z\\d]{1,7}\\)) ?"
				+ "|[\\dA-Za-z]{1,3}\\.([\\d]+\\.?)+ " // 1.10. OR 7.2.9 or 7.2.9.
				+ "|[\\d]{1,3}\\.([\\d]+)?(\\.[\\d]+\\.?)? ?" + "|[A-Za-z]{1}\\. ?(?=[A-Za-z]{2})"// 1.1.1
				+ "|(SECTION|Section)[\\d]+\\.([\\d]+)?(?=[A-Z]{1})" + "))";

		Pattern patternHdg3 = Pattern.compile(paraNmbStr + "(.*?\\.[ ]{0,5}(?=\r\n|[A-Z]))");

		Pattern patternHdg = Pattern
				.compile(patternHdg3.pattern() + "|" + patternHdg1.pattern() + "|" + patternHdg2.pattern());
		
		List<String> list = nlp.getAllMatchedGroups(text,patternHdg3);
//		NLP.printListOfString("", list);

		list = nlp.getAllMatchedGroups(text,patternHdg);
		NLP.printListOfString("", list);

		
	}
}
