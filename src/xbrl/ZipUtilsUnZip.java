package xbrl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.compress.utils.IOUtils;

public class ZipUtilsUnZip {

	public static void unzip(String zipPath, String extractToFolder)
			throws IOException {
		File zip = new File(zipPath);
		File extractTo = new File(extractToFolder);
		ZipFile archive = new ZipFile(zip);

		Enumeration<? extends ZipEntry> e = archive.entries();
		while (e.hasMoreElements()) {
			ZipEntry entry = e.nextElement();
			File file = new File(extractTo, entry.getName());
			if (entry.isDirectory()) {
				file.mkdirs();
			} else {
				if (!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				java.io.InputStream in = archive.getInputStream(entry);
				BufferedOutputStream out = new BufferedOutputStream(
						new FileOutputStream(file));
				IOUtils.copy(in, out);
				in.close();
				out.close();
			}
		}
	}
	
	
	public static boolean isValid(final File file) {
	    ZipFile zipfile = null;
	    try {
	        zipfile = new ZipFile(file);
	        return true;
	    } catch (IOException e) {
	        return false;
	    } finally {
	        try {
	            if (zipfile != null) {
	                zipfile.close();
	                zipfile = null;
	            }
	        } catch (IOException e) {
	        }
	    }
	}


	public static void main(String[] args) {

		// READ THIS! -- this will unzip ZIP files. For some reason some zip
		// files I can't unzip without an error in ZipUtils class - they'll work
		// here though.
		// However I can't place this in the ZipUtils class because of conflict
		// between apache and java jar api libraries

	}

}
