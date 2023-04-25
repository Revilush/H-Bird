package util;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;


public class EasyFileFilter implements FileFilter {

	private Pattern includePattern = null;
	private Pattern excludePattern = null;
	private Boolean onlyFiles = null;
	private Boolean onlyFolder = null;
	
	public EasyFileFilter(String includePattern, String excludePattern, Boolean onlyFiles, Boolean onlyFolder) {
		if (null != includePattern)
			this.includePattern = Pattern.compile(includePattern);
		if (null != excludePattern)
			this.excludePattern = Pattern.compile(excludePattern);
		this.onlyFiles = onlyFiles;
		this.onlyFolder = onlyFolder;
	}
	
	public EasyFileFilter(String fnStartsWith, String fnEndsWith, String allowedExtn, Boolean onlyFiles, Boolean onlyFolder) {
		this.onlyFiles = onlyFiles;
		this.onlyFolder = onlyFolder;
		if (null != allowedExtn)
			allowedExtn = allowedExtn.replaceAll(",", "|").replaceAll("\\.", "");
		
		String ptrnStr = "";
		if (null != fnStartsWith)
			ptrnStr = "^("+fnStartsWith+")";
		ptrnStr += ".*";
		if (null != fnEndsWith)
			ptrnStr += "("+fnEndsWith+")$";
		else if (null != allowedExtn)
			ptrnStr += "(\\.("+allowedExtn+"))$";
		this.includePattern = Pattern.compile(ptrnStr);
	}

	@Override
	public boolean accept(File file) {
		boolean accept = false;
		if (null != onlyFiles  &&  onlyFiles  &&  file.isDirectory())
			return false;
		if (null != onlyFolder  &&  onlyFolder  &&  file.isFile())
			return false;
		if (null != includePattern) {
			accept = includePattern.matcher(file.getName()).matches();
			if (null != excludePattern)
				accept = accept && !excludePattern.matcher(file.getName()).matches();
		} else if (null != excludePattern) {
			accept = !excludePattern.matcher(file.getName()).matches();
		} else	// when no criteria is given
			return true;
		return accept;
	}

}
