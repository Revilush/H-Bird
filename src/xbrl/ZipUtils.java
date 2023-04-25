package xbrl;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public class ZipUtils {

	public static boolean isTarGzipFileValid(String zipFilePath) throws IOException {
		return isTarGzipFileExtractable(zipFilePath);
	}

	public static boolean isTarGzipFileExtractable(String zipFilePath) throws IOException {
		try {
			if (zipFilePath.endsWith(".tar.gz")) {
				// see if tar file is good to extract data from
				return isTarGzFileExtractable(zipFilePath);
			} else if (zipFilePath.endsWith(".gz")) {
				return isGzFileExtractable(zipFilePath);
			} else if (zipFilePath.endsWith(".zip")) {
				return isZipFileExtractable(zipFilePath);
			}
		} catch (Exception e) {
			// e.printStackTrace(System.err);
			System.err.println("Exception while deflating zip: " + zipFilePath + ",  err: " + e.getLocalizedMessage());
		}
		return false;
	}

	private static boolean isTarGzFileExtractable(String zipFilePath) throws IOException {
		// see if tar file is good to read
		if (!isTarGzipFileValidAtStart(zipFilePath))
			return false;
		// extract contents in a temp folder, and if any error is generated, its not
		// valid, else good. In any case, delete the temp folder
		long now = System.currentTimeMillis();
		Random r = new Random();
		// r.nextInt()  => to add li'l randomness to overcome >1 calls at same millis
		File tmpFolder = new File(System.getProperty("java.io.tmpdir"), now + "_" + Math.abs(r.nextInt())); 
		try {
			if (!tmpFolder.exists())
				tmpFolder.mkdirs();
			// System.out.println("created folder: " + tmpFolder.getAbsolutePath());
			deflateTarGzipFile(zipFilePath, tmpFolder.getAbsolutePath());
			FileUtils.deleteDirectory(tmpFolder);
			return true;
		} catch (EOFException e) {
			// zip file looks to be bad towards end
			return false;
		} finally {
			// cleanup the tmp folder - delete
			try {
				if (tmpFolder.exists())
					FileUtils.deleteDirectory(tmpFolder);
			} catch (IOException ioe) {
				if (tmpFolder.exists())
					tmpFolder.delete();
				else
					FileUtils.deleteDirectory(tmpFolder);
			}
		}
	}

	private static boolean isGzFileExtractable(String zipFilePath) throws IOException {
		// see if tar file is good to read
		if (!isTarGzipFileValidAtStart(zipFilePath))
			return false;
		long now = System.currentTimeMillis();
		Random r = new Random();
		File tmpFolder = new File(System.getProperty("java.io.tmpdir"), now + "_" + Math.abs(r.nextInt())); // a li'l randomness to
																									// overcome >1 calls
																									// at same milli
		try {
			if (!tmpFolder.exists())
				tmpFolder.mkdirs();
			// System.out.println("created folder: " + tmpFolder.getAbsolutePath());
			deflateGzipFile(zipFilePath, tmpFolder.getAbsolutePath());
			FileUtils.deleteDirectory(tmpFolder);
			return true;
		} catch (EOFException e) {
			// zip file looks to be bad towards end
			return false;
		} finally {
			// cleanup the tmp folder - delete
			try {
				if (tmpFolder.exists())
					FileUtils.deleteDirectory(tmpFolder);
			} catch (IOException ioe) {
				if (tmpFolder.exists())
					tmpFolder.delete();
				else
					FileUtils.deleteDirectory(tmpFolder);
			}
		}
	}

	private static boolean isZipFileExtractable(String zipFilePath) throws IOException {
		// see if tar file is good to read
		if (!isTarGzipFileValidAtStart(zipFilePath))
			return false;
		long now = System.currentTimeMillis();
		Random r = new Random();
		File tmpFolder = new File(System.getProperty("java.io.tmpdir"), now + "_" + Math.abs(r.nextInt()) ); // a li'l randomness to
																									// overcome >1 calls
																									// at same milli
		try {
			if (!tmpFolder.exists())
				tmpFolder.mkdirs();
			// System.out.println("created folder: " + tmpFolder.getAbsolutePath());
			deflateZipFile(zipFilePath, tmpFolder.getAbsolutePath());
			FileUtils.deleteDirectory(tmpFolder);
			return true;
		} catch (EOFException e) {
			// zip file looks to be bad towards end
			return false;
		} finally {
			// cleanup the tmp folder - delete
			try {
				if (tmpFolder.exists())
					FileUtils.deleteDirectory(tmpFolder);
			} catch (IOException ioe) {
				if (tmpFolder.exists())
					tmpFolder.delete();
				else
					FileUtils.deleteDirectory(tmpFolder);
			}
		}
	}

	public static boolean isTarGzipFileValidAtStart(String zipFilePath) throws IOException {
		try {
			boolean validFile = false;
			File tar = new File(zipFilePath);
			FileInputStream fis = new FileInputStream(tar);
			if (zipFilePath.endsWith(".tar.gz")) {
				GZIPInputStream gis = new GZIPInputStream(fis);
				TarArchiveInputStream tis = new TarArchiveInputStream(gis);
				TarArchiveEntry tae = tis.getNextTarEntry();
				if (tae != null) // && tae.isCheckSumOK()
					validFile = true;
				tis.close();
				gis.close();
			} else if (zipFilePath.endsWith(".gz")) {
				GZIPInputStream gis = new GZIPInputStream(fis);
				gis.read();
				// close resources
				gis.close();
				validFile = true;
			} else if (zipFilePath.endsWith(".zip")) {
				validFile = ZipUtilsUnZip.isValid(tar);
			}
			fis.close();
			return validFile;
		} catch (IOException e) {
			return false;
		}
	}

	public static void deflateZipFile(String zipFilePath, String extractToFolder) throws IOException {
		System.out.println("zipFilePath: " + zipFilePath + ", extractToFolder: " + extractToFolder);
		FileInputStream fis = null;
		ZipInputStream zipIs = null;
		ZipEntry zEntry = null;
		try {
			fis = new FileInputStream(zipFilePath);
			zipIs = new ZipInputStream(new BufferedInputStream(fis));
			while ((zEntry = zipIs.getNextEntry()) != null) {
				byte[] tmp = new byte[4 * 1024];
				FileOutputStream fos = null;
				fos = new FileOutputStream(new File(extractToFolder, zEntry.getName()));
				int size = 0;
				while ((size = zipIs.read(tmp)) != -1) {
					fos.write(tmp, 0, size);
				}
				fos.flush();
				fos.close();
			}
		} finally {
			if (null != zipIs)
				zipIs.close();
			if (null != fis)
				fis.close();
		}
	}

	// below can be used used when unzipping an SEC xbrl zip.
	public static List<String> deflateZipFilewithAccNo(String zipFilePath, String extractToFolder,
			String accessionNumber) throws IOException {
		FileInputStream fis = null;
		ZipInputStream zipIs = null;
		ZipEntry zEntry = null;
		List<String> fileNamesInZip = new ArrayList<String>();
		try {

			File zipFile = new File(zipFilePath);
			if (!zipFile.exists())
				return fileNamesInZip;
			fis = new FileInputStream(zipFilePath);
			zipIs = new ZipInputStream(new BufferedInputStream(fis));
			while ((zEntry = zipIs.getNextEntry()) != null) {
				File f = new File(extractToFolder, accessionNumber + "_" + zEntry.getName());
				fileNamesInZip.add(f.getName());
				// does not extract if filename it would extract to already
				// exists
				if (!f.exists()) {
					byte[] tmp = new byte[4 * 1024];
					FileOutputStream fos = null;
					fos = new FileOutputStream(new File(extractToFolder, accessionNumber + "_" + zEntry.getName()));
					int size = 0;
					while ((size = zipIs.read(tmp)) != -1) {
						fos.write(tmp, 0, size);
					}
					fos.flush();
					fos.close();
				}

			}
		} finally {
			if (null != zipIs)
				zipIs.close();
			if (null != fis)
				fis.close();
		}
		return fileNamesInZip;
	}

	public static String deflateGzipFile(String gzFile, String targetFolderPath) throws IOException {
		Utils.createFoldersIfReqd(targetFolderPath);

		System.out.println("unzipping: " + gzFile + " to targetFilePath=" + targetFolderPath);
		File src = new File(gzFile);
		File outputF = new File(targetFolderPath, src.getName().substring(0, src.getName().indexOf(".")));

		FileInputStream fis = null;
		GZIPInputStream gis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(src);
			gis = new GZIPInputStream(fis);
			fos = new FileOutputStream(outputF);
			byte[] buffer = new byte[1024];
			int len = 0;
			while ((len = gis.read(buffer)) > 0) {
				fos.write(buffer, 0, len);
			}
		} finally {
			// close resources
			if (null != fos)
				fos.close();
			if (null != gis)
				gis.close();
			if (null != fis)
				fis.close();
		}
		return outputF.getAbsolutePath();
	}

	public static List<String> deflateTarGzipFile(String tarGzFile, String extractToFolder) throws IOException {
		List<String> fileNamesInZip = new ArrayList<String>();

		System.out.println("unzipping tarGZip: " + tarGzFile + " into " + extractToFolder);
		File src = new File(tarGzFile);

		FileInputStream fis = null;
		GZIPInputStream gis = null;
		TarArchiveInputStream tis = null;
		ArchiveEntry tEntry = null;
		FileOutputStream fos = null;

		try {
			fis = new FileInputStream(src);
			gis = new GZIPInputStream(fis);
			tis = new TarArchiveInputStream(gis);

			while ((tEntry = tis.getNextTarEntry()) != null) {
				File f = new File(extractToFolder, tEntry.getName());
				Utils.createFoldersIfReqd(f.getParent());
				fileNamesInZip.add(f.getName());
				// does not extract if filename it would extract to already
				// exists
				if (!f.exists()) {
					byte[] tmp = new byte[4 * 1024];
					fos = new FileOutputStream(new File(extractToFolder, tEntry.getName()));
					tEntry.getSize();
					int size = 0;
					while ((size = tis.read(tmp)) != -1) {
						fos.write(tmp, 0, size);
					}
					fos.flush();
					fos.close();
				}
			}
		} finally {
			if (null != tis)
				tis.close();
			if (null != gis)
				gis.close();
			if (null != fis)
				fis.close();
			if (null != fos)
				fos.close();
		}

		return fileNamesInZip;
	}

	public static void zipAllFilesInFolder(String folderToZip, String zipPathAndFileName) throws IOException {

		try {

			// Initiate ZipFile object with the path/name of the zip file.
			ZipFile zipFile = new ZipFile(zipPathAndFileName);

			File zF = new File(zipPathAndFileName);
			if (zF.exists()) {
				zF.delete();
			}

			String folderToAdd = folderToZip;

			// Initiate Zip Parameters which define various properties such
			// as compression method, etc.
			ZipParameters parameters = new ZipParameters();

			// set compression method to store compression
			parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);

			// Set the compression level
			parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);

			// Add folder to the zip file

			Utils.createFoldersIfReqd(folderToAdd);
			zipFile.addFolder(folderToAdd, parameters);
			folderToAdd = folderToZip + "//htm";
			zipFile.addFolder(folderToAdd, parameters);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void zipAFile(File file) throws IOException {

		byte[] buffer = new byte[1024];

		FileOutputStream fos = null;
		ZipOutputStream zos = null; 
		FileInputStream in = null;
		try {

			System.out.println("FileOutputStream="
					+ file.getAbsolutePath().substring(0, file.getAbsolutePath().indexOf(".")) + ".zip");

			fos = new FileOutputStream(
					file.getAbsolutePath().substring(0, file.getAbsolutePath().indexOf(".")) + ".zip");

			zos = new ZipOutputStream(fos);
			ZipEntry ze = new ZipEntry("master.idx");
			zos.putNextEntry(ze);
			in = new FileInputStream(
					file.getAbsolutePath().substring(0, file.getAbsolutePath().indexOf(".")) + ".idx");
			System.out.println("FileInputStream="
					+ file.getAbsolutePath().substring(0, file.getAbsolutePath().indexOf(".")) + ".idx");

			int len;
			while ((len = in.read(buffer)) > 0) {
				zos.write(buffer, 0, len);
			}

			in.close();
			

			System.out.println("Done");

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			// remember close all
			if (null != in)
				in.close();
			if (null != zos) {
				zos.closeEntry();
				zos.close();
			}
			if (null != fos)
				fos.close();
		}
	}


	
	
	public static void main(String[] arg) throws IOException {
		String path = "E:\\secZipFiles\\parsed\\20200626.nc.tar.gz"; // 0629, 0626
		path = "E:\\secZipFiles\\parsed\\20200625.nc.tar.gz";

		path = "e:\\secZipFiles\\20190501.nc.tar.gz";

		System.out.println(isTarGzipFileExtractable(path));

	}

}