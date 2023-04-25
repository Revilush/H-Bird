package xbrl;

import java.util.regex.Pattern;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

public class EasyFTPFileFilter implements FTPFileFilter {

	private Pattern includePattern = null;
	private Pattern excludePattern = null;
	private Boolean onlyFiles = null;
	private Boolean onlyFolder = null;

	public EasyFTPFileFilter(String includePattern, String excludePattern,
			Boolean onlyFiles, Boolean onlyFolder) {
		if (null != includePattern)
			this.includePattern = Pattern.compile(includePattern);
		if (null != excludePattern)
			this.excludePattern = Pattern.compile(excludePattern);
		this.onlyFiles = onlyFiles; // see local class variables above
		this.onlyFolder = onlyFolder;
	}

	/*
	 * Whenever method name is the same as class name it is a constructor. Like
	 * array list - this method name is used multiple times - however each has
	 * different parameters so java will know which to implement based on
	 * parameters and order. The parameters being passed are
	 * EasyFTPFileFilter("master", null, null, true, null);
	 */
	public EasyFTPFileFilter(String fnStartsWith, String fnEndsWith,
			String allowedExtn, Boolean onlyFilesAllowed,
			Boolean onlyFolderAllowed) {
		this.onlyFiles = onlyFilesAllowed;
		// this.onlyFiles is the class variable. '= onlyFilesAllowed' is the
		// parameter passed. Sets class varialble instance equal to the
		// parameter. In order to save the object/variable instance outside the
		// method we had to set it equal to a class variable instance. Once it
		// is then defined in this class with its state/behavior and b/c it is
		// defined outside this method as a class we can then use it in other
		// methods outside this one. onlyFiles is the class variable and we set
		// the instance here which b/c it is a class object it can be used
		// outside this method.
		this.onlyFolder = onlyFolderAllowed;
		if (null != allowedExtn) // if allowedExtn (parameter) is not null
			allowedExtn = allowedExtn.replaceAll(",", "|")
			// this is just allow program to understand that if parameter input
			// is '.pdf,.txt,.idx' that each extension is separate by a comma
			// and in order to remove regular express of ',' we replace it with
			// '|'. This is theoretical - we never need it.
					.replaceAll("\\.", ""); //for now we never need it.

		String ptrnStr = "";
		if (null != fnStartsWith)//if f/n/s/w is not null
			ptrnStr = "^(" + fnStartsWith + ")";// then ptrnStr = ^(master)
		ptrnStr += ".*"; //^(master).*
		if (null != fnEndsWith)//in our case it is null - so this is not true
			ptrnStr += "(" + fnEndsWith + ")$";
		else if (null != allowedExtn)//also not true. iteration stops here and goes to this.includePattern
			ptrnStr += "(\\.(" + allowedExtn + "))$"; 
		this.includePattern = Pattern.compile(ptrnStr);
		//this.includePattern will be compiled to a regular express equal to ^(master).*
	}

	// if for example the list from the ftp site has the following:
	// master.20130908.idx, form.20130908.idx
	//

	@Override
	/*
	 * for class FTPFileFilter you have to implement method 'accept' (similar to
	 * 'run' in thread class). accept returns a boolean value - true and false
	 * for each iteration.
	 */
	public boolean accept(FTPFile file) {
		boolean accepted = false;
		if (null != onlyFiles && onlyFiles && file.isDirectory())
			return false;
		if (null != onlyFolder && onlyFolder && file.isFile())
			return false;
		if (null != includePattern) {
			accepted = includePattern.matcher(file.getName()).matches();
			if (null != excludePattern)
				accepted = accepted
						&& !excludePattern.matcher(file.getName()).matches();
		} else if (null != excludePattern) {
			accepted = !excludePattern.matcher(file.getName()).matches();
		} else
			// when no criteria is given
			return true;//rejects if false
		return accepted;//accepts true instances
	}
}
