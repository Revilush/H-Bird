package xbrl;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;

import contracts.GetContracts;

public class DownloadSecZipFile {

	public static String secZipFilesDownloads = "e:/secZipFiles/ready_for_metaParsing//";

	public static String badFilesRelativeFolder = "bad";
	public static String goodFilesRelativeFolder = "good_validated";

//	20120410,20120427,20120430,20130515,20131231
	public static void getSecZipFile(Calendar startDate, Calendar endDate) throws Exception {

		int startYear = startDate.get(Calendar.YEAR);
		int endYear = endDate.get(Calendar.YEAR);
		int startQtr = InsiderParser.getQuarter(startDate);
		int endQtr = InsiderParser.getQuarter(endDate);

		int QtrYrs = (endYear - startYear) * 4;
		int iQtr = (endQtr - startQtr) + 1;
		int totalQtrs = QtrYrs + iQtr;
		int startYr = startYear;

		iQtr = startQtr;
		Calendar cal = Calendar.getInstance();
		// https://www.sec.gov
		// replace ftp.sec.gov
		// EasyFTPClient ftpClient = new
		// EasyFTPClient("https://www.sec.gov/archives/", 0,
		// "anonymous", "p@pff.com", false);
		for (int i = 1; i <= totalQtrs; i++) {

			// EasyFTPFileFilter ftpFilter = new EasyFTPFileFilter(startYr + "",
			// null, null, null, null);
			cal.set(Calendar.YEAR, startYr);
			cal.set(Calendar.MONTH, (iQtr * 3) - 1);
			// https://www.sec.gov/Archives/edgar/Feed/2017/QTR1/
			String secFolderAddress = "/edgar/Feed/" + startYr + "/QTR" + iQtr;
			System.out.println(secFolderAddress);

			String localPath = secZipFilesDownloads;
			Utils.createFoldersIfReqd(localPath);
// 			https://www.sec.gov/Archives/edgar/Feed/2000/QTR1/
			HttpDownloadUtility.downloadFile("https://www.sec.gov/Archives" + secFolderAddress + "/20160104.nc.tar.gz",
					secZipFilesDownloads);

			// FTPFile[] ftpListOfFiles = ftpClient.listFiles(secFolderAddress,
			// ftpFilter);
			// System.out.println(Arrays.toString(ftpListOfFiles));

			File f = new File(localPath);
			List<String> localFileNames = new ArrayList<String>();
			for (File localF : f.listFiles())
				localFileNames.add(localF.getName());

			System.out.println(localFileNames);

		}

		iQtr++;
		if (iQtr > 4) {
			startYr++;
			iQtr = 1;
		}
	}

	public static void downloadSecZipFiles(int sY, int eY, int sQ, int eQ, String secZipfolder,
			String ready_Metadata_parsing_folder) throws IOException, ParseException {

		int month = 0, cnt = 0;
		String day = "", moStr = "", url = "";
		String folderToSaveDownloadedFileTo = ""; // input "c:/path below:
		String secSite = "https://www.sec.gov/Archives/edgar/Feed/";
		String fileExt = "nc.tar.gz";
		String filename;
		File badFilesFolder, goodFilesFolder, goodFile;

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String today = sdf.format(new Date());

		File file = new File("");
		for (; sY <= eY; sY++) {

			for (; sQ <= eQ; sQ++) {
				cnt = 0;
				for (int b = 1; b <= 3; b++) {
					month = sQ * 4 - sQ - cnt;
					cnt++;
					if (sQ < 4)
						moStr = "0" + month;
					else
						moStr = "" + month;

					for (int i = 1; i < 32; i++) {
						folderToSaveDownloadedFileTo = secZipfolder;
						if (i < 10)
							day = "0" + i;
						else
							day = "" + i;

						if (!isWeekday(sY + moStr + day))
							continue;
						if (today.compareTo(sY + moStr + day) < 0) // if the date is > today, then there would be no
																	// file - go back
							continue;

						filename = sY + moStr + day + "." + fileExt;
						url = secSite + sY + "/QTR" + sQ + "/" + filename;

						if (!folderToSaveDownloadedFileTo.toLowerCase().contains("seczipfiles")) {
//							System.out.println("1");
							folderToSaveDownloadedFileTo = secZipfolder + sY + "/QTR" + sQ;
						}
						badFilesFolder = new File(folderToSaveDownloadedFileTo, badFilesRelativeFolder);
						goodFilesFolder = new File(folderToSaveDownloadedFileTo, goodFilesRelativeFolder);

						// if file already exists in good folder, then nothing more to do
						goodFile = new File(goodFilesFolder, filename);
						if (goodFile.exists()) {
//							System.out.println("validated/good file already exists: " + goodFile.getAbsolutePath());
							continue;
						}

						// creating file here - which is killing old.
						file = new File(folderToSaveDownloadedFileTo, filename);
						if (!file.exists()) {
							// download file from edgar
							System.out.println("downloading from url=" + url + "\rsave file to folder="
									+ folderToSaveDownloadedFileTo);
							if (!HttpDownloadUtility.downloadFile(url, folderToSaveDownloadedFileTo)) {
								// if file was not downloaded - either not available etc then go back
								continue;
							} else {
								// save copy to metadata parser
								System.out.println(
										"file exists. Copying to: ready for metadata parsing folder. copying file="
												+ file.getAbsolutePath());
								FileSystemUtils.copyFile(new File(folderToSaveDownloadedFileTo + "/" + filename),
										new File(ready_Metadata_parsing_folder + "/" + filename));
							}
						}

						// file now exists
						if (ZipUtils.isTarGzipFileExtractable(file.getAbsolutePath())) {
							// file exists and good - dont download again
							System.out.println("continuing b/c good file=" + file.getAbsolutePath());
							// move to good folder
							FileUtils.moveFileToDirectory(file, goodFilesFolder, true);
							continue;
						} else {
							// file exists but not extractable - move to bad folder and download again
							// but if file already exists in bad folder, means last-2-last download was also
							// bad, and current file being-looked/downloaded is also bad. Mostly the file
							// itself may be bad on edgar or so, but report so..
							File badF = new File(badFilesFolder, file.getName());
							if (badF.exists()) {
								System.out.println("**** File " + file.getName()
										+ " is bad, and another bad file already exists. Looks like source file is bad. pl verify");
								// delete current bad file
								file.delete();
								continue;
							}
							System.out.println("# moving bad file: " + file.getName() + " to " + badFilesFolder);
							// move this file to 'bad' folder, and continue to download again
							FileUtils.moveFileToDirectory(file, badFilesFolder, true);
						}

//						if (file.exists() && ZipUtils.isTarGzipFileValid(file.getAbsolutePath())) {
//							System.out.println("continuing b/c good file=" + file.getAbsolutePath());
//							continue;
//						}
//						System.out.println("file=" + file.getName() + "\r is good?="
//								+ ZipUtils.isTarGzipFileValid(file.getAbsolutePath()));
//						if (file.exists() && !ZipUtils.isTarGzipFileValid(file.getAbsolutePath())) {
//							System.out.println("deleting bad file. file=" + file.getAbsolutePath());
//							file.delete();
//						}

						try {
							System.out.println("-- downloading AGAIN from url=" + url + "\rsave file to folder="
									+ folderToSaveDownloadedFileTo);
							if (!HttpDownloadUtility.downloadFile(url, folderToSaveDownloadedFileTo)) // file was not
																										// downloaded -
																										// either not
																										// available etc
								continue;

							// check/validate file again
							file = new File(folderToSaveDownloadedFileTo, filename);
//							if (!file.exists()) {
//								// this means file was not downloaded!! report
//								System.out.println("### File was downloaded but cant be found!! "+ file.getAbsolutePath());
//								continue;
//							}

							// validate the zip file is valid/good
							if (ZipUtils.isTarGzipFileExtractable(file.getAbsolutePath())) {
								System.out.println("good file moving to validated folder: " + file.getName());
								FileUtils.moveFileToDirectory(file, goodFilesFolder, true);
							} else {
								// file exists but not extractable - move to bad folder and download again
								// but if file already exists in bad folder, means last-2-last download was also
								// bad, and current file being looked is also bad. Mostly the file itself may be
								// bad on edgar or so, but report so..
								File badF = new File(badFilesFolder, file.getName());
								if (badF.exists()) {
									System.out.println("**** File " + file.getName()
											+ " is bad, and another bad file already exists. Looks like source file is bad. pl verify");
									// delete current bad file
									file.delete();
									continue;
								}
								System.out.println("# moving bad file: " + file.getName() + " to " + badFilesFolder);
								// move this file to 'bad' folder, and continue to download again
								FileUtils.moveFileToDirectory(file, badFilesFolder, true);
							}

						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				}
			}
			sQ = 1;
		}
	}

	public static boolean isWeekday(String dateStr) throws ParseException {
		boolean isWeekDay = true;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date sDate = new Date();
		sDate = sdf.parse(dateStr);
		Calendar startDate = Calendar.getInstance();
		startDate.setTime(sDate);

//		System.out.println("day of the week is==" + startDate.get(Calendar.DAY_OF_WEEK));

		if (startDate.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
				|| startDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

		) {
			System.out.println("is not weekday==" + startDate.get(Calendar.DAY_OF_WEEK));
			isWeekDay = false;
		}
		return isWeekDay;
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {

		boolean itIsWeekday = isWeekday("20220613");
		System.out.println(itIsWeekday);
		int sY = 2022, eY = 2022, sQ = 3, eQ = sQ;
		for (; sY <= eY; sY++) {
			for (; sQ <= eQ; sQ++) {
				downloadSecZipFiles(sY, sY, sQ, sQ, GetContracts.secZipFolder,
						"E:\\secZipFiles\\ready_for_metaParsing\\");
			}
			sQ = 1;
		}

		// Determine how to integrate downloader/scheduler and checking of bad files
		// into process
		// checks for bad files:
//		File zipFilesFolder = new File("c:/secZipFiles/");
//		File[] tarFiles = zipFilesFolder.listFiles(new EasyFileFilter(null, ".tar.gz", null, true, null)); 
//		for (File zipF : tarFiles) {
////			System.out.println("checking if zip is bad. "+zipF.getName());
//			if (!ZipUtils.isTarGzipFileExtractable(zipF.getAbsolutePath())) {
//				// this tar zip file is not valid/extractable
//				System.out.println(zipF.getAbsolutePath() + " is invalid file!");
//
//				File targetFilename = new File("E:\\secZipFiles\\bad\\" + zipF.getName());
//				FileSystemUtils.copyFile(zipF, targetFilename);
//
//				if (zipF.exists()) {
//					System.out.println("invalid file exists - can it be deleted?? zipF="+zipF.getAbsolutePath());
//					zipF.delete();
//				}
//			}
//		}

		/*
		 * Scanner Scan = new Scanner(System.in); System.out
		 * .println("download 1 Qtr at a time.\r\n"); System.out
		 * .println("Enter YYYY to download secZipFie");
		 * 
		 * String year = Scan.nextLine().substring(0, 4);
		 * System.out.println("Enter Qtr to download"); int qtr =
		 * Integer.parseInt(Scan.nextLine());
		 * 
		 * // NOTE: CAN DOWNLOAD ANY FILE IN YYYYMMDD FORMAT. // 1. chg folder to save
		 * downloaded file to - // folderToSaveDownloadedFileTo. // 2. chg secSite to
		 * loc file to download is at // 3. chg file extension so it is same as file ext
		 * at secSite // multi-thread by running this multiple times
		 * 
		 * int month = 0, cnt = 0; String day = "", moStr = "", url = ""; String
		 * folderToSaveDownloadedFileTo = ""; //input "c:/path below: String secSite =
		 * "https://www.sec.gov/Archives/edgar/Feed/"; String fileExt = "nc.tar.gz";
		 * 
		 * File file = new File (""); for (int n = 1; n < 4; n++) { month = qtr * 4 -
		 * qtr - cnt; cnt++; if(qtr<4) moStr = "0"+month; else moStr = ""+month;
		 * 
		 * for (int i = 1; i < 32; i++) { folderToSaveDownloadedFileTo =
		 * secZipFilesDownloads; if (i < 10) day = "0" + i; else day = "" + i; url =
		 * secSite + year + "/QTR" + qtr + "/" + year + moStr + day + "." + fileExt;
		 * 
		 * if (!folderToSaveDownloadedFileTo.toLowerCase().contains( "seczipfiles")) {
		 * System.out.println("1"); folderToSaveDownloadedFileTo =
		 * secZipFilesDownloads+year+"/QTR"+qtr; }
		 * 
		 * // creating file here - which is killing old. file = new
		 * File(folderToSaveDownloadedFileTo + "/" + year + moStr + day + "." +
		 * fileExt);
		 * 
		 * System.out.println("created this new file=" + file.getAbsolutePath() +
		 * "\ris valide tar.gz file?=" +
		 * ZipUtils.isTarGzipFileValid(file.getAbsolutePath()));
		 * 
		 * if(file.exists() && ZipUtils.isTarGzipFileValid(file.getAbsolutePath())){
		 * System.out.println("continuing b/c good file="+file.getAbsolutePath());
		 * continue; }
		 * 
		 * System.out.println("file="+file.getName()+"\r is good?="+ZipUtils.
		 * isTarGzipFileValid(file.getAbsolutePath()));
		 * 
		 * if(file.exists() && !ZipUtils.isTarGzipFileValid(file.getAbsolutePath())){
		 * System.out.println("deleting bad file. file="+file.getAbsolutePath());
		 * file.delete(); }
		 * 
		 * try { System.out.println("url="+url+"\rsave file to folder="
		 * +folderToSaveDownloadedFileTo);
		 * 
		 * HttpDownloadUtility.downloadFile( url, folderToSaveDownloadedFileTo); } catch
		 * (IOException ex) { ex.printStackTrace(); } } }
		 */}
}
