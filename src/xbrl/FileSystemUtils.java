package xbrl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.channels.FileChannel;



public class FileSystemUtils {
	
	public static final String Charset_IO_UTF8 = "UTF8";
	public static File TempDir = new File(System.getProperty("java.io.tmpdir"));
	public static final String LineSeparator = System.getProperty("line.separator", "\n");
	public static final String UserHomeDir = System.getProperty("user.home");


	public static void replaceInFile(File file) throws IOException {

		String filename = file.toString()
				.substring(0, file.toString().length());
		String fn = filename.substring(0, filename.lastIndexOf("\\") + 1);
		System.out.println("filename: " + filename);
		System.out.println("fn: " + fn);

		FileWriter fw = new FileWriter(fn + "/csv/" + "xbrl.txt");

		Reader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);

		while (br.ready()) {
			fw.append(br.readLine().replaceAll("/", "-"));
			fw.append("\n");
		}

		while (br.ready()) {
			fw.append(br.readLine().replaceAll("\\\\", "-"));
			fw.append("\n");
		}

		fw.close();
		br.close();
		fr.close();
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
//		System.out.println("path=="+path);
		File dir = new File(path);
		if (!dir.exists())
			dir.mkdirs();
	}
	
	public static void backupFile(String sourcePath, String targetPath) throws IOException{

		File source = new File(sourcePath);
		File[] listOfSourceFiles = source.listFiles();

		for (int i = 0; i < listOfSourceFiles.length; i++) {
			File sourceFilename = new File(listOfSourceFiles[i].toString());

			// System.out.println("listOfFiles="+listOfSourceFiles[i]+
			// ", name="+listOfSourceFiles[i].getName()
			// + ", lastModified="+listOfSourceFiles[i].lastModified() +
			// ", filesize="+listOfSourceFiles[i].length());

			File targetFilename = new File(targetPath + "/"
					+ listOfSourceFiles[i].getName());

			// (!targetFilename.exists() ||
			// targetFilename.length()!=listOfSourceFiles[i].length()) &&
			if (!targetFilename.exists()
					|| sourceFilename.length() > targetFilename.length()) {
				
				// below just tell me whether it was copied b/c it doesn't
				// exist, more recent modification date or size is larger in
				// source

				if (targetFilename.exists()
						&& sourceFilename.length() > targetFilename.length()) {
					System.out
							.println("source file size is larger than target file. \rSource file size="
									+ sourceFilename.length()
									+ "\r and target file size="
									+ targetFilename.length());
				}

/*				if (targetFilename.exists()
						&& sourceFilename.lastModified() > targetFilename
								.lastModified()) {
					System.out
							.println("source file modified daet is after target file's. \rSource file modified date="
									+ sourceFilename.lastModified()
									+ "\r and target file modified date="
									+ targetFilename.lastModified());
				}*/

				else {
					System.out
							.println("File not in target. File is being copied to target. file="
									+ targetFilename.toString());
				}

				System.out.println("finished copying file "
						+ targetFilename.toString());

				copyFile(sourceFilename, targetFilename);

			}
		}
	}
	
	public static void copyFile(File sourceFile, File destFile)
			throws IOException {
		
		if (!sourceFile.exists()) {
			return;
		}
//		System.out.println("destFile=="+destFile.getAbsolutePath());
		if (!destFile.exists()) {
			destFile.createNewFile();
		}
		
		FileChannel source = null;
		FileChannel destination = null;
		source = new FileInputStream(sourceFile).getChannel();
		destination = new FileOutputStream(destFile).getChannel();
		
		if (destination != null && source != null) {
			destination.transferFrom(source, 0, source.size());
		}
		
		if (source != null) {
			source.close();
		}
		if (destination != null) {
			destination.close();
		}
		
		source.close();
		destination.close();
		
	}
	
	public static String ReadTextFromResource(String resourceName) throws IOException {
		InputStream is = FileSystemUtils.class.getClassLoader().getResourceAsStream(resourceName);
		return readTextFromReader(new InputStreamReader(is, Charset_IO_UTF8), Long.MAX_VALUE);
	}

	public static String readTextFromReader(Reader rd, long lines2Read) throws IOException {
		StringBuffer sb = new StringBuffer();
		BufferedReader br = null;
		try {
			br = new BufferedReader(rd);
			String line;
			long lineNo = 0;
			while ((line = br.readLine()) != null) {
				lineNo++;
				sb.append(line);
				sb.append(LineSeparator);
				if (lineNo >= lines2Read)
					break;
			}
		} finally {
			if (br != null)
				try {
					br.close();
				} catch (Exception e) {
				}
		}
		return sb.toString();
	}

	

	public static void main(String[] args) throws IOException {
//		backupFile("c:/backtest/seczipfiles", "e:/DataBackUp/SEC_ZipFiles");

	}
}
