package contracts;


import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import algo_testers.search_dependencies.JSonUtils;
import xbrl.NLP;
import xbrl.TikaExample;
import xbrl.Utils;

public class GetClientDocs {

	@SuppressWarnings("resource")

	public static String strTxt = "";

	public static String clientParentGroup = "", clientContractType = "";

	@SuppressWarnings("resource")
	public static void makeDocTextFile(File fileOrFolder) throws IOException, SAXException, TikaException {

		File file = new File("c:/temp/tmp.txt");
		String clientTxt = "";
		PrintWriter pw = new PrintWriter(file);
		if (fileOrFolder.isFile()) {
			System.out.println("is file1");

			if (fileOrFolder.toString().toLowerCase().contains(".pdf")) {
				clientTxt = TikaExample.parseToStringExample(fileOrFolder.getAbsolutePath());
				file = new File(fileOrFolder.getAbsoluteFile().toString().replaceAll("(?ism)\\.pdf", "\\.txt"));
				if (file.exists())
					file.delete();
				pw = new PrintWriter(file);
				pw.append(clientTxt.replaceAll("\n", "\r\n").replaceAll("\n\n", "\n"));
				pw.close();
			}

			if (fileOrFolder.toString().toLowerCase().contains(".docx")) {
				clientTxt = Utils.readWordDocXFile(fileOrFolder.getAbsolutePath());
				file = new File(fileOrFolder.getAbsoluteFile().toString().replaceAll("(?ism)\\.docx", "\\.txt"));
				if (file.exists())
					file.delete();
				pw = new PrintWriter(file);
				pw.append(clientTxt.replaceAll("\n", "\r\n").replaceAll("\n\n", "\n"));
				pw.close();
			}

			if (fileOrFolder.toString().toLowerCase().contains(".doc")
					&& !fileOrFolder.toString().toLowerCase().contains(".docx")) {
				clientTxt = Utils.readMSWord97Format(fileOrFolder.getAbsolutePath());
				file = new File(fileOrFolder.getAbsoluteFile().toString().replaceAll("(?ism)\\.doc", "\\.txt"));
				if (file.exists())
					file.delete();
				pw = new PrintWriter(file);
				pw.append(clientTxt.replaceAll("\n", "\r\n").replaceAll("\n\n", "\n"));
				pw.close();
			}
		}

		File[] files = fileOrFolder.listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				makeDocTextFile(f);
			} else {
				System.out.println("is file2=" + f.getAbsolutePath());
				if (f.toString().contains(".pdf")) {
					clientTxt = TikaExample.parseToStringExample(f.getAbsolutePath());
					file = new File(f.getAbsoluteFile().toString().replaceAll("(?ism)\\.pdf", "\\.txt"));
					if (file.exists())
						file.delete();
					pw = new PrintWriter(file);
					System.out.println("file==" + file.getAbsolutePath());
					pw.append(clientTxt.replaceAll("\n", "\r\n").replaceAll("\n\n", "\n"));
					pw.close();
				}

				if (f.toString().toLowerCase().contains(".docx")) {
					clientTxt = Utils.readWordDocXFile(f.getAbsolutePath());
					file = new File(f.getAbsoluteFile().toString().replaceAll("(?ism)\\.docx", "\\.txt"));
					if (file.exists())
						file.delete();
					pw = new PrintWriter(file);
					pw.append(clientTxt.replaceAll("\n", "\r\n").replaceAll("\n\n", "\n"));
					pw.close();
				}

				if (f.toString().toLowerCase().contains(".doc") && !f.toString().toLowerCase().contains(".doc")) {
					clientTxt = Utils.readMSWord97Format(f.getAbsolutePath());
					file = new File(f.getAbsoluteFile().toString().replaceAll("(?ism)\\.doc", "\\.txt"));
					if (file.exists())
						file.delete();
					pw = new PrintWriter(file);
					pw.append(clientTxt.replaceAll("\n", "\r\n").replaceAll("\n\n", "\n"));
					pw.close();
				}
			}
		}
	}

	public static String getContractTypeForClient(List<String[]> list, File file) throws IOException {
		NLP nlp = new NLP();

		String filename = file.getName();

		for (int i = 0; i < list.size(); i++) {
//			System.out.println("cKtyp=" + list.get(i)[0] + " fname=" + filename);

			if (nlp.getAllMatchedGroups(filename, Pattern.compile("(?ism)" + list.get(i)[0])).size() > 0) {
				return list.get(i)[1];
			}
		}
		return "";

	}

	public static String renameClientTextFiles(String rootFolderNameId, File file) throws IOException {

		NLP nlp = new NLP();
		String acc = "";
		System.out.println(
				"getting acc/kid. file=" + file.getAbsolutePath().toString() + " folder kid=" + rootFolderNameId);
		if (nlp.getAllMatchedGroups(file.getAbsolutePath().toString().toLowerCase(),
				Pattern.compile("(?sm)" + rootFolderNameId.toLowerCase() + "[\\d]+")).size() > 0) {
			acc = nlp.getAllMatchedGroups(file.getAbsolutePath().toString().toLowerCase(),
					Pattern.compile("(?sm)" + rootFolderNameId.toLowerCase() + "[\\d]+")).get(0);
		}
		return acc;
	}

	public static void main(String[] args) throws IOException, SQLException, ParseException, XPathExpressionException,
			ParserConfigurationException, SAXException, SolrServerException, TikaException, InterruptedException {

		// [{a.txt, true},{b.txt, false}]

		// System.out.print("Enter the name of index: "); 
		// BufferedReader inpstr = new BufferedReader (new InputStreamReader(System.in));
		// String index_name = inpstr.readLine();
		String index_name = "contracts_2021";
		
		//StringBuilder sb1 = new StringBuilder(index_name);
		//String test = "name is " +index_name + "rr";
		//System.out.println(test);
		
		
		//taking input of path of document from user
		System.out.print("Enter the local path of document: ");
		BufferedReader inp = new BufferedReader (new InputStreamReader(System.in));
		
		//String str_path = inp.readLine();

		// for loop to loop through all the docs in list
		
		String str_path1 = "../../../../upload/";
		//for local
		System.out.println("-----------------------arg0------------------->");
		System.out.print(args[1]);
		// System.out.print(join(args));
		String str_path = str_path1+args[1];
		String isPrivate = args[0];
		boolean boolPrivate = Boolean.parseBoolean(isPrivate);
		// String tmp = join(args);
		// String str_path = str_path1+tmp;

		//File fl = new File (str_path1);
		//String content[] = fl.list();
		//String str_path = "../../../../upload/" + content[0];
		//System.out.println(str_path);
		
		//String index_name = inp.readLine();
		//String str_path = inp.readLine();
		

		
		// TODO: Input is string and output is json str.

		GetContracts gk = new GetContracts();

		TreeMap<String, List<String>> mapParse = new TreeMap<String, List<String>>();
		List<String> listParse = new ArrayList<String>();
		listParse.add("solrCore=indenture||contractType=settlement||keyWordsInContractName=Manifesto||fsize=60000||");
		mapParse.put("C", listParse);

		List<String[]> secMetaData = new ArrayList<String[]>();
		String[] ary = { "0123456789-99-1234567", "T1", "abc", "123456", "1701-01-01" };
		secMetaData.add(ary);
		String txt = Utils.readTextFromFile(str_path);
		String json = gk.getContract(txt, secMetaData, false, true, false, true, false, mapParse,"1707-01-01");

		//System.out.println("json.len==" + json.length());
		//System.out.println(json);
		
		elasticDocRectifier rectifier = new elasticDocRectifier();
		//System.out.println(JSonUtils.prettyPrint(rectifier.cleanJsonFileOrFolder(json,"01/01/1707")));
		
		String json1 = JSonUtils.prettyPrint(rectifier.cleanJsonFileOrFolder(json,"01/01/2021"));
		String json2 = json1.replaceAll("([{])", "\n$1").replaceAll("]$", "\n]").replaceAll("(?m)^[\\s&&[^\\n]]+|[\\s+&&[^\\n]]+$", "");
		
		
		//System.out.println(json2);
		
		Path fileName = Path.of("docs_json_05_dt.txt");

		Files.writeString(fileName, json2);
		
		System.out.println("json is written to .txt --");
		
		
		
		// json to ndjson
	
		// docker
		File file = new File("docs_json_05_dt.txt");

        BufferedReader br= new BufferedReader(new FileReader(file));
        
        String st;
        String prevst = null;
        String prev2st= null;

		// for local run
        //FileWriter fw = new FileWriter("C:\\LincParser_2nd_final_Command_new_java_files\\output\\docs_ndjson_05_dt.txt");
		
		// for docker
		FileWriter fw = new FileWriter("docs_ndjson_05_dt.txt");
	// } => ,"private": true}
        while ( (st=br.readLine()) != null){
        	// System.out.println(st);
            for (int i=0; i<st.length();i++){
			String curr=String.valueOf(st.charAt(i));
		    	if (curr != null && !(prev2st == null && curr.equals("["))){
				if (prev2st == null){
					prev2st= curr;
					fw.write("{\"index\":{\"_index\":\"" + index_name +"\",\"_type\":\"_doc\"}}");
					fw.write("\r\n");
					fw.write(prev2st); // coment this
					/* PPC:
					if(curr.equals('}')){
						fw.write(",\"private\": " +  isPrivate + "}");
					// }
					else{
						fw.write(prev2st);
					}*/
				//    System.out.println(st+"---1"+"--"+prev2st+ "   "+curr.equals("["));
				} else if (prevst == null) {
					prevst= curr;
				//      System.out.println(st+"---2");
				}
				else {
					// if(curr == '}'){

					// }
					if (prev2st.equals("}") && prevst.equals(",") && curr.equals("{")){
						fw.write("\r\n");
						fw.write("{\"index\":{\"_index\":\"" + index_name +"\",\"_type\":\"_doc\"}}");
						fw.write("\r\n");
					//     System.out.println(st+"---3");
					}else if(prevst.equals("}")){
						fw.write(",\"private\": " + boolPrivate + "}");
					}
					else {

						fw.write(prevst); // coment this
						/* PPC:
						if(curr.equals('}')){
							fw.write(",\"private\": " +  isPrivate + "}");
						}
						else{
							fw.write(prevst);
						}*/
					//    System.out.println(st+"---4");
					}
					prev2st = prevst;
					prevst = curr;
				}
                	};
            }

        }
        fw.write("\r\n");
        if (!(prevst == null || prevst.equals(",") || prevst.equals("[") || prevst.equals("]"))) {
            fw.write(prevst);
        }


        fw.close();
		
        System.out.println("JSON converted to NDJSON --");
		
        // Pushing to Elastic Search using Curl

		// java command execution




		//String cmdGetind = "curl -i -XPOST \"localhost:9200/test";
		//String cmdGetind = "curl -XPUT 172.17.0.2:9200/zzzz?pretty";
		//String cmdGetind = "curl -XPOST \"http://172.17.0.2:9200/test\"";
		//Process process1 = Runtime.getRuntime().exec(cmdGetind);
		//int resultCode = process1.waitFor();
        //InputStream inputStream1 = process1.getInputStream();
        //BufferedReader reader1 = new BufferedReader(new InputStreamReader(process1.getInputStream()));
		//BufferedReader stdError1 = new BufferedReader(new InputStreamReader(process1.getErrorStream()));
		//System.out.println("Here is the standard output of the command:\n");
		//String s = null;
		//while ((s = reader1.readLine()) != null) {
    	//	System.out.println(s);
		//}

		//Read any errors from the attempted command
		//System.out.println("Here is the standard error of the command (if any):\n");
		//while ((s = stdError1.readLine()) != null) {
    	//System.out.println(s);
		//}
		//System.out.println(reader1);
        //System.out.println("test index created ");
		
		// for docker

		String cmdGetDocId = "curl -H Content-Type:application/x-ndjson -XPOST 172.18.0.2:9200/"+index_name+"/_bulk?pretty --data-binary @docs_ndjson_05_dt.txt";
		Process process = Runtime.getRuntime().exec(cmdGetDocId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		System.out.println("Here is the standard output of the command:\n");
		String ss = null;
		while ((ss = reader.readLine()) != null) {
    		//System.out.println(ss);
		}

		//Read any errors from the attempted command
		System.out.println("Here is the standard error of the command (if any):\n");
		while ((ss = stdError.readLine()) != null) {
    	System.out.println(ss);
		}

		System.out.print(args[0]);
		System.out.print(args[1]);
		// System.out.print(args);

        System.out.println("output: Document pushed to ES");
        //Thread.sleep(2000);
        /*
        while(process.isAlive()) Thread.sleep(100);
        System.out.println("return value: " + process.exitValue());
        reader.lines().forEach(System.out::println);
        reader = new BufferedReader(new InputStreamReader(process.getErrorStream())); 
        reader.lines().forEach(System.err::println);
        
        System.out.println("Pushed to Elastic search");
		*/
        
        //    C:\Users\soitb\Downloads\edgar_docs\Older\1.txt
        
	}
}
