package contracts;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.solr.client.solrj.SolrServerException;

import com.fasterxml.jackson.databind.ObjectMapper;

import xbrl.Utils;

public class DocumentParser {
	public List<Map> parseDocument(String inputText) throws Exception {
		
		
		TreeMap<String, List<String>> mapParse = new TreeMap<String, List<String>>();
		List<String> listParse = new ArrayList<String>();
		listParse.add("solrCore=indenture||contractType=settlement||keyWordsInContractName=Manifesto||fsize=60000||");
		mapParse.put("C", listParse);

		String date="31/12/2015";
		List<String[]> secMetaData = new ArrayList<String[]>();
		String [] ary={"0123456789-99-1234567","T1","abc","123456",date};
		secMetaData.add(ary);
		String txt =  Utils.readTextFromFile( "/home/wellwin/Downloads/test.txt");
		//fix docx captured from w/n Word b/c they use \r and not \r\n.
		txt = txt.replaceAll("\r\n", "zcxzcx");
		txt = txt.replaceAll("\r", "zcxzcx");
		txt = txt.replaceAll("zcxzcx","\r\n");
		
		GetContracts docsParser = new GetContracts();
		
		String json = docsParser.getContract(txt, secMetaData, false, true,  false, true,  false, mapParse,date);
		elasticDocRectifier rectifier = new elasticDocRectifier();
		List<Map> map =  rectifier.cleanJsonFileOrFolder(json,date);
		writeToFile(map);  
		return map;
		
	}
	
	
	
	private void writeToFile(List<Map> map) {

		try {
			ObjectMapper obj = new ObjectMapper();
			String json = obj.writeValueAsString(map);
			File file = new File("/home/wellwin/temp/contracts_test.json");
			PrintWriter pw = new PrintWriter(file);
			pw.write(json);
			pw.close();
			System.out.println("File write completed!!!!!!!!");
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	public static void main(String[] args) throws Exception {
		DocumentParser parser = new DocumentParser();
		List<Map> docs = parser.parseDocument("1. We are an institutional “accredited investor” (as defined in Rule 501(a)(1), (2), (3) or (7) under the Securities Act of 1933, as amended (the “Securities Act”)), purchasing for our own account or for the account of such an institutional “accredited investor” at least $100,000 principal amount of the Notes, and we are acquiring the Notes not with a view to, or for offer or sale in connection with, any distribution in violation of the Securities Act. We have such knowledge and experience in financial and business matters as to be capable of evaluating the merits and risks of our investment in the Notes, and we invest in or purchase securities similar to the Notes in the normal course of our business. We, and any accounts for which we are acting, are each able to bear the economic risk of our or its investment.");
	}

}
