package xbrl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A utility that downloads a file from a URL.
 * 
 * @author www.codejava.net
 *
 */
public class HttpDownloadUtility {
	private static final int BUFFER_SIZE = 4096;

	/**
	 * Downloads a file from a URL
	 * 
	 * @param fileURL HTTP URL of the file to be downloaded
	 * @param saveDir path of the directory to save the file
	 * @throws IOException
	 */

	public static boolean downloadFile(String fileURL, String saveDir) throws IOException {
		URL url = new URL(fileURL);
		System.out.println("downloadFiledownloadFiledownloadFile");
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setRequestProperty("host", "www.sec.gov");
		httpConn.setRequestProperty("Accept-Encoding", "gzip, deflate");
		httpConn.setRequestProperty("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.61 Safari/537.36");
		httpConn.setRequestProperty("sec-ch-ua-platform", "Windows");
//        System.out.println(httpConn.getRequestProperty(key))
		int responseCode = httpConn.getResponseCode();
		System.out.println("going to url=" + fileURL);

		// always check HTTP response code first
		boolean fileDownloaded = true;
		if (responseCode == HttpURLConnection.HTTP_OK) {
			String fileName = "";
			String disposition = httpConn.getHeaderField("Content-Disposition");
//            String contentType = httpConn.getContentType();
//            int contentLength = httpConn.getContentLength();

			if (disposition != null) {
				// extracts file name from header field
				int index = disposition.indexOf("filename=");
				if (index > 0) {
					fileName = disposition.substring(index + 10, disposition.length() - 1);
				}
			} else {
				// extracts file name from URL
				fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1, fileURL.length());
			}

			System.out.println("Content-Disposition = " + disposition);
			System.out.println("final fileName = " + fileName);

			// opens input stream from the HTTP connection
			InputStream inputStream = httpConn.getInputStream();
			File saveFilePath = new File(saveDir, fileName);
			System.out.println("saveFilePath=" + saveFilePath.getAbsolutePath());
			// opens an output stream to save into file
			FileSystemUtils.createFoldersIfReqd(saveDir);
			FileOutputStream outputStream = new FileOutputStream(saveFilePath);

			int bytesRead = -1;
			byte[] buffer = new byte[BUFFER_SIZE];
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}

			outputStream.close();
			inputStream.close();

			System.out.println("File downloaded");
		} else {
			System.out.println("No file to download. Server replied HTTP code: " + responseCode);
			System.out.println("this file could not be downloaded=" + fileURL);
			fileDownloaded = false;
		}
		
		httpConn.disconnect();
		return fileDownloaded;
	}

	public static void main(String[] args) {
		String[] saveDir = { "c:/backtest/FSDataSets/", "c:/backtest/master/", "c:/backtest/master/" };
		String[] sites = { "https://www.sec.gov/data/financial-statements/"
				// "https://www.sec.gov/data/financial-statements/2016q4.zip",
				, "https://www.sec.gov/Archives/edgar/full-index/", "https://www.sec.gov/Archives/edgar/full-index/" };
		// "https://www.sec.gov/Archives/edgar/full-index/2016/QTR4/"
		String site = "";
		String[] filenameExtension = { ".zip", "master.zip", "xbrl.zip" };
		int startYr = 2021, startQtr = 1;
		int endYr = 2021;

		// search for file - and if exists - skip.
		File file = new File("");

		for (int i = 0; i < sites.length; i++) {

			for (int yr = startYr; yr <= endYr; yr++) {
				for (int q = startQtr; q < 5; q++) {

					try {
						if (i == 0) {
							// add validator
							String path = saveDir[i] + "/" + yr + "/QTR" + q + "/";
							FileSystemUtils.createFoldersIfReqd(path);
							file = new File(path + yr + "q" + q + filenameExtension[i]);
							site = sites[i] + yr + "q" + q + filenameExtension[i];
							System.out.println("file=" + file + "\rdownload file site=" + site);
							System.out.println("is valid?=" + ZipUtils.isTarGzipFileValid(file.getAbsolutePath()));
							if ((file.exists() && ZipUtils.isTarGzipFileValid(file.getAbsolutePath())) || yr < 2009) {
								System.out.println("skip this download");
								continue;
							} else {
								HttpDownloadUtility.downloadFile(site, path);
							}
						}
						if (i > 0) {
							String path = saveDir[i] + "/" + yr + "/QTR" + q + "/";
							System.out.println(
									"valid master/xbrl zip?=" + ZipUtils.isTarGzipFileValid(file.getAbsolutePath()));
							FileSystemUtils.createFoldersIfReqd(path);
							site = sites[i] + "/" + yr + "/QTR" + q + "/" + filenameExtension[i];
							file = new File(path + filenameExtension[i]);
							System.out.println("file=" + file + "\rdownload file site=" + site);
							if (file.exists() && ZipUtils.isTarGzipFileValid(file.getAbsolutePath())) {
								System.out.println("skip this download");
								continue;
							} else {
								HttpDownloadUtility.downloadFile(site, path);
							}
						}

					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
		}
	}
}