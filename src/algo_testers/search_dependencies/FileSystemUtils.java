package algo_testers.search_dependencies;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileSystemUtils {

	private static final Log log = LogFactory.getLog(FileSystemUtils.class);

	public static final String Charset_IO_UTF8 = "UTF8";
	public static final String Charset_NIO_UTF8 = "UTF-8";

	public static File TempDir = new File(System.getProperty("java.io.tmpdir"));
	public static final String LineSeparator = System.getProperty("line.separator", "\n");
	public static final String UserHomeDir = System.getProperty("user.home");

	public static void renameFile(String srcFilePath, String tgtFileName) {
		File f = new File(srcFilePath);
		f.renameTo(new File(tgtFileName));
	}

	public static byte[] readFromFile(String fileAbsoluteName) throws FileNotFoundException, IOException {
		return readFromFile(new File(fileAbsoluteName));
	}
	public static byte[] readFromFile(File file) throws FileNotFoundException, IOException {
		FileInputStream fin = new FileInputStream(file);
		byte[] content = new byte[(int) file.length()];
		fin.read(content);
		fin.close();
		return content;
	}

	public static String readTextFromFile(String fn) throws IOException {
		return new String(readFromFile(fn));
	}
	public static String readTextFromFile(File file) throws IOException {
		return new String(readFromFile(file));
	}

	public static String readTextLinesFromFile(String fn, int lines2Read) throws IOException {
		return readTextFromReader(new FileReader(fn), lines2Read);
	}

	public static String readTextLinesFromFile(File fn, int lines2Read) throws IOException {
		return readTextFromReader(new FileReader(fn), lines2Read);
	}

	public static String ReadTextFromResource(String resourceName) throws IOException {
		InputStream is = FileSystemUtils.class.getClassLoader().getResourceAsStream(resourceName);
		BufferedInputStream bis = new BufferedInputStream(is);
		
		byte[] buffer = new byte[0];
		byte[] bytes = new byte[1024];
		int bytesRead;
		while ( (bytesRead = bis.read(bytes)) > 0 ) {
			buffer = ArrayUtils.addAll(buffer, ArrayUtils.subarray(bytes, 0, bytesRead) );
		}
		return new String(buffer);
		
		//return readTextFromReader(new InputStreamReader(is, Charset_IO_UTF8), Long.MAX_VALUE);
	}

	public static String ReadTextLinesFromResource(String resourceName, int lines2Read) throws IOException {
		InputStream is = FileSystemUtils.class.getClassLoader().getResourceAsStream(resourceName);
		return readTextFromReader(new InputStreamReader(is, Charset_IO_UTF8), lines2Read);
	}

	public static void writeTextToFile(File fn, String text)
			throws FileNotFoundException, UnsupportedEncodingException {
		if (StringUtils.isBlank(text))
			return;
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(fn, "UTF-8");	//Charset_IO_UTF8);
			pw.write(text);
		} finally {
			if (pw != null)
				try {
					pw.close();
				} catch (Exception e) {
					log.error("", e);
				}
		}
	}

	public static void writeTextToFile(String fn, String text)
			throws FileNotFoundException, UnsupportedEncodingException {
		writeTextToFile(new File(fn), text);
	}

	public static void writeTextToFile(String fn, String text, boolean createFileAsNecessary)
			throws FileNotFoundException, UnsupportedEncodingException {
		if (StringUtils.isBlank(text))
			return;
		File file = new File(fn);
		if (!file.exists() && createFileAsNecessary) {
			File folderP = file.getParentFile();
			if (!folderP.exists())
				folderP.mkdirs();
		}
		writeTextToFile(file, text);
	}

	public static void writeJsonsToFile(String fileName, Object... texts) {
		if (StringUtils.isBlank(fileName))
			return;
		writeJsonsToFile(new File(fileName), texts);
	}
	
	public static void writeJsonsToFile(File file, Object... texts) {
		writeJsonsToFile_AppendOverwrite(file, false, texts);
	}

	public static void writeJsonsToFile_AppendOverwrite(File file, boolean append2File, Object... texts) {
		if (! file.getParentFile().exists())
			file.getParentFile().mkdirs();
		PrintWriter pw = null;
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			
			fw = new FileWriter(file, append2File); 
			bw = new BufferedWriter(fw); 
			pw = new PrintWriter(bw);
			//pw = new PrintWriter(file);
			for (Object txt : texts) {
				if (txt instanceof String)
					pw.append(txt.toString());
				else {
					try {
						pw.append(JSonUtils.prettyPrint(txt));
					} catch (Exception e) {
						pw.append("Error while converting object to json:" + e.getMessage());
						pw.append(txt.toString());
					}
				}
			}
		} catch (IOException e) {
			log.warn("", e);
		} finally {
			if (null != pw)
				pw.close();
			if (null != bw) {
				try {
					bw.close();
				} catch (IOException e) {
					log.warn("", e);
				}
			}
			if (null != fw) {
				try {
					fw.close();
				} catch (IOException e) {
					log.warn("", e);
				}
			}
		}
	}
	
	public static void writeToOutputStream(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[2048];
		int length;
		while ((length = in.read(buffer)) > 0) {
			out.write(buffer, 0, length);
		}
	}

	public static void copyFileTo(InputStream is, File targetFilePath) throws IOException {
		targetFilePath.createNewFile();
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFilePath));
		BufferedInputStream bis = new BufferedInputStream(is);
		byte[] bytes = new byte[1024];
		int bytesRead;
		while ((bytesRead = bis.read(bytes)) > 0) {
			bos.write(bytes, 0, bytesRead);
		}
		bos.close();
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

	/**
	 * 
	 * @param parentFolderPath
	 * @param filter
	 *            - can be null in which case, no filter will be applied on
	 *            children files/dirs
	 * @return
	 */
	public static String[] getChildrenOfFolder(String parentFolderPath, FilenameFilter filter) {
		File parent = new File(parentFolderPath);
		String[] children = parent.list(filter);
		return children;
	}

	public static String getExtension(String fileName, boolean withDot) {
		if (StringUtils.isBlank(fileName))
			return null;
		return withDot ? fileName.substring(fileName.lastIndexOf("."))
				: fileName.substring(fileName.lastIndexOf(".") + 1);
	}

	public static String removeExtension(String fileName) {
		if (StringUtils.isBlank(fileName))
			return null;
		int pos = fileName.lastIndexOf(".");
		return pos > 0 ? fileName.substring(0, pos) : fileName;
	}

	public static void createFoldersIfReqd(String path) {
		File dir = new File(path);
		createFoldersIfReqd(dir);
	}
	public static void createFoldersIfReqd(File uptoFolder) {
		if (!uptoFolder.exists())
			uptoFolder.mkdirs();
	}
	
}
