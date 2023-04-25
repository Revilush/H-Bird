package algo_testers.search_dependencies;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import xbrl.Utils;

public class WriteLogs {
	
	private static String reportFilePath = "e:/deepak/report/";
	
	public static void writeSpeedReportToHtml(String log) {
		Utils.createFoldersIfReqd("e:/deepak/report/");
		try{ 
		      BufferedWriter out = new BufferedWriter(new FileWriter(reportFilePath+"golaw_search_new.html", true)); 
			  out.write(log);
			  out.close(); 
			  }catch(Exception e){
			      System.out.println(e);
		  }
		
	}
	
	public static void writeBigloopReportToHtml(String folderpath, String filename, String log) {
		try {
			Utils.createFoldersIfReqd(folderpath);
			BufferedWriter out = new BufferedWriter(new FileWriter(folderpath + filename + "", true));
			out.write(log);
			out.close();
		} catch (Exception e) {
			System.out.println(e);
		}

	}
	
	
	public static void writeSolrQuery(String folderPath, String filename, String query) {
		try{ 
			Utils.createFoldersIfReqd(folderPath);
		      BufferedWriter out = new BufferedWriter(new FileWriter(folderPath+filename+"", true)); 
			  out.write(query);
			  out.close(); 
			  }catch(Exception e){
			      System.out.println(e);
		  }
		
	}
	
	public static void deleteOldFile(String folderPath, String fileName) {
		File file = new File(folderPath+fileName);
		if(file.exists()) {
			file.delete();
		}
	}
	
	public static void deleteFileFromFoder(String folderPath) {
		Utils.createFoldersIfReqd(folderPath);
		File folder = new File(folderPath);
		for (File file : folder.listFiles()) {
			if(file.isDirectory()) {
				deleteFileFromFoder(file.getAbsolutePath());
			}else if (file.exists()) {
				file.delete();
			}
			
		}
	}
	
	
	public static void main(String[] arg) {
		System.out.println("limited partnership interests, as applicable, are owned or controlled, directly or indirectly, by such Person or one or more of the other Subsidiaries of that Person or a combination thereof, whether in the form of membership, general, special or limited partnership interests or otherwise, and (b) such Person or any Subsidiary of such Person is a controlling general partner or otherwise controls such entity.".split(" ").length);
	}
}