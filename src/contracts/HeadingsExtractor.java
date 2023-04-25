package contracts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xbrl.FileSystemUtils;
import xbrl.NLP;

public class HeadingsExtractor {

	private static Pattern hdgPattern = Pattern.compile(

			"\"(exh|sec|hdg|mHd|sub|def|contractNameAlgo|exhL|secL|hdgL|mHdL|subL|defL|txt)\""
					+ "(?> {0,2}: {0,2}\")(.+?)(?=[\"\r\n]{1,2})");

	/**
	 * inner list contains <headingField,value>, outer list contains all such
	 * occurrences in the file/s.
	 */

	public List<List<String>> allHdgs = new ArrayList<>();

	public List<List<String>> exractHeadingsFromFile(File fileOrFolder) throws FileNotFoundException, IOException {
		
		String json = FileSystemUtils.readTextFromReader(new FileReader(fileOrFolder), Long.MAX_VALUE);
		
		List<List<String>> hdgs = getAllMatchedGroups(json, hdgPattern);
		
		return hdgs;
		
	}

	public void extractHeadingsFromFileOrFolder(File fileOrFolder) throws FileNotFoundException, IOException {
		
		if (fileOrFolder.isFile()) {
			System.out.println("extracting headers="+fileOrFolder.getAbsolutePath());
			allHdgs.addAll(exractHeadingsFromFile(fileOrFolder));
			return;
		}
		File[] files = fileOrFolder.listFiles();
		for (File f : files) {
			if (f.isDirectory())
				extractHeadingsFromFileOrFolder(f);
			else
				extractHeadingsFromFileOrFolder(f);
		}
		
	}

	public List<List<String>> getAllMatchedGroups(String text, Pattern pattern) {
		Matcher matcher = pattern.matcher(text);
		List<List<String>> finds = new ArrayList<>();
		while (matcher.find()) {
			List<String> groups = new ArrayList<>();
			for (int i = 1; i <= matcher.groupCount(); i++) {
				groups.add(matcher.group(i));
			}
			if (groups.size() > 0)
				finds.add(groups);
		}
		return finds;
	}
	
	public static List<String> getAllHdgs(String text) throws IOException{
		NLP nlp = new NLP();
		
		List<String> list = nlp.getAllMatchedGroups(text, hdgPattern);
		
		return list;
	}
	

	public static void main(String[] args) throws FileNotFoundException, IOException {
		// NOTE: ES cannot now form collapse/expand on multivalued fields or analyze
		// fields unfortunately the house is a multivalued field and the headings are
		// analyzed.

		File jsonFileOrFolder = new File("e:/temp/"); // it can be a folder as well

		HeadingsExtractor extractor = new HeadingsExtractor();
		extractor.extractHeadingsFromFileOrFolder(jsonFileOrFolder);
		System.out.println("finished");
		System.out.println(extractor.allHdgs);
	}

}
