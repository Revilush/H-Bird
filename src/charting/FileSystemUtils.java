package charting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FileSystemUtils {

	public static byte[] readByteArrayFromFile(String fileAbsoluteName) throws FileNotFoundException, IOException {
		File file = new File(fileAbsoluteName);
		FileInputStream fin = new FileInputStream(file);
		byte[] content = new byte[(int) file.length()];
		fin.read(content);
		fin.close();
		return content;
	}
	
	public static String readTextFromFile2(String filePath) throws IOException {
		StringBuilder results = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(
					filePath)));
			try {
				String line = null;
				while ((line = reader.readLine()) != null) {
					results.append(line).append("\r\n");
				}
			} finally {
				reader.close();
			}
			//System.out.println(results);
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return results.toString();
	}

	
	public static void writeToAsciiFile(String fileName, String contents) {
		File file = new File(fileName);
		createFoldersIfReqd(file.getParent());
		PrintWriter out = null;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
			out.print(contents);
		} catch (IOException e) {
			System.err.println(e);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
	
	public static void createFoldersIfReqd(String path) {
		File dir = new File(path);
		if (!dir.exists())
			dir.mkdirs();
	}

	
}
