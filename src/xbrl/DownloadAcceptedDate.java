package xbrl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TODO: RUN ONCE ALL OF TP_RAW QUARTERLY TABLE IS COMPLETE.

public class DownloadAcceptedDate {

	public static String folder = "c:/backtest/acceptedDate/";

	public static void getAcceptedDateFyeLocal(String tmpFolder)
			throws FileNotFoundException, IOException, SQLException {

		NLP nlp = new NLP();
		//tmpFolder=c:/backtest/acceptedDate/year/Qtr#/
		String html, fye = null, acceptedDate = null, acc = null;
//		TableParser tp = new TableParser();
		File f = new File(tmpFolder);
		for (File localF : f.listFiles()
		// f.listFiles()
		) {
			if (localF.getAbsolutePath().contains(".htm")) {

				System.out.println("localF=" + localF + " acc="
						+ localF.getAbsolutePath());
				acc = localF.getAbsolutePath().substring(34, 54);
				System.out.println("acc=" + acc);

				html = Utils.readTextFromFile(localF.getAbsolutePath());
				html = nlp.removeExtraBlanks(nlp.stripHtmlTags(html));
				System.out.println("-index.htm local file start=" + html);

				Pattern ptrnFye = Pattern
						.compile("(?<=(Fiscal Year End:)).+(?=Type)");
				Matcher matchFye = ptrnFye.matcher(html);
				int cnt = 0;
				while (matchFye.find()) {
					fye = matchFye.group().replaceAll(" ", "");
					System.out.println("fye=" + fye);
					cnt++;
					if (cnt >= 0)
						break;
				}

				Pattern ptrnA = Pattern
						.compile("(?s)(?<=Accepted).{0,5}\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");

				Matcher matchA = ptrnA.matcher(html);
				cnt = 0;
				while (matchA.find()) {
					acceptedDate = matchA.group().replaceAll("\r\n", "");
					System.out.println("acceptedDate=" + acceptedDate);
					cnt++;
					if (cnt >= 0)
						break;
				}

				MysqlConnUtils
						.executeQuery("insert ignore into acc_aDate_fye select '"
								+ acc
								+ "','"
								+ acceptedDate
								+ "','"
								+ fye
								+ "'");
				String parsedFolder = folder + "parsed/";
				System.out.println("parsedFolder=" + parsedFolder
						+ " localF.getName()=" + localF.getName());
				File file = new File(parsedFolder + localF.getName());

				if (file.exists()) {
					file.delete();
				}
				
				localF.renameTo(file);
			}
		}
	}

	protected static String getPageHtml(String pageUrl) throws IOException {
		EasyHttpClient httpClient = new EasyHttpClient(false);
		return httpClient.makeHttpRequest("Get", pageUrl, null, -1, null, null);
	}

	public static String exportTP_RawTableAccNo(int year, int qtr)
			throws SQLException, FileNotFoundException {

		String table = "tp_raw" + year + "qtr" + qtr;
		String filename = folder + year + "qtr" + qtr
				+ "accno_acceptedDate.txt";

		File f = new File(filename);
		if (f.exists())
			f.delete();

		String query = "\r\rdrop table if exists tmp_acc_cik ;\r create table tmp_acc_cik engine=myisam "
				+ "\rselect accno,cik from "
				+ table
				+ " group by accno; "
				+ "\ralter table tmp_acc_cik add key (accno), add key(cik); \r\rcall getAcceptedDateFye(); "
				+ "\r\rselect accno,cik INTO OUTFILE '"
				+ filename
				+ "' \rFIELDS TERMINATED BY ',' from tmp_acc_cik t1;"
				+ "\r\rdrop table if exists tmp_acc_cik ; \rdrop table if exists tmp_cnt2;";

		MysqlConnUtils.executeQuery(query);
		return (filename);

	}

	public static void getAcceptedDateFye(Calendar startDate, Calendar endDate,
			String tablePrefix) throws Exception {

		int startYear = startDate.get(Calendar.YEAR);
		int endYear = endDate.get(Calendar.YEAR);
		int startQtr = EightK.getQuarter(startDate);
		int endQtr = EightK.getQuarter(endDate);

		int QtrYrs = (endYear - startYear) * 4;
		int iQtr = (endQtr - startQtr) + 1;
		int totalQtrs = QtrYrs + iQtr;
		int startYr = startYear;
		iQtr = startQtr;
		Calendar cal = Calendar.getInstance();
		String filename = null;
		String[] csvLines;

		for (int i = 1; i <= totalQtrs; i++) {
			cal.set(Calendar.YEAR, startYr);
			cal.set(Calendar.MONTH, (iQtr * 3) - 1);

			if (tablePrefix.toLowerCase().contains("tp_raw")) {
				// finds accno not in acc_adate_fye
				filename = exportTP_RawTableAccNo(startYr, iQtr);
			}

			csvLines = Utils.readTextFromFile(filename).split(
					Utils.LineSeparator);

			// fetches acc to be downloaded from sec.gov

			String tmpFolder = folder + "/" + startYr + "qtr" + iQtr + "/";

			Utils.createFoldersIfReqd(tmpFolder);

			if (csvLines.length > 1) {

				for (String line : csvLines) {
					String accno = line.split(",")[0];
					String cikStr = line.split(",")[1];
					int cik = Integer.parseInt(cikStr);
					// System.out.println("accno=" + accno);

					String secAddress = "https://www.sec.gov/Archives/edgar/data/"
							+ cik
							+ "/"
							+ accno.replaceAll("-", "")
							+ "/"
							+ accno + "-index.htm";
					// System.out.println("secAddress based on accno=" +
					// secAddress);

					// downloads page
					String html = getPageHtml(secAddress);
					File fn = new File(tmpFolder + accno + ".htm");
					Utils.writeTextToFile(fn, html);
					// reads file from tmpFolder and then moves it to 'parsed'
					// sub
					// tmpFolder.
					getAcceptedDateFyeLocal(tmpFolder);
				}
			}
			iQtr++;
			if (iQtr > 4) {
				startYr++;
				iQtr = 1;
			}
		}
	}

	public static void main(String[] args) throws Exception {
		@SuppressWarnings("resource")
		Scanner Scan = new Scanner(System.in);
		System.out
				.println("Enter start date for time period to get acceptedDates for accno (yyyymmdd)");
		String startDateStr = Scan.nextLine();

		System.out
				.println("Enter start date for time period to get acceptedDates for accno (yyyymmdd)");
		String endDateStr = Scan.nextLine();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date sDate = new Date();
		sDate = sdf.parse(startDateStr);
		Calendar startDate = Calendar.getInstance();
		startDate.setTime(sDate);
		Date eDate = new Date();
		eDate = sdf.parse(endDateStr);
		Calendar endDate = Calendar.getInstance();
		endDate.setTime(eDate);

		if (endDate.before(startDate)) {
			System.out
					.println("End date must be later than start date. Please re-enter.");
			return;
		}
		if (endDate.after(Calendar.getInstance())) {
			System.out
					.println("End date cannot be later than today. Please re-enter.");
			return;
		}

		getAcceptedDateFye(startDate, endDate, "tp_raw");

		// <<==above will get from sec.gov all accno in tp_raw folders but not
		// in acc_adate_fye (can change to compare against tpIdx table if I just
		// want to get all adate/fye.

		// ==>>below will parse any index-htm in this folder
		// getAcceptedDateFyeLocal("c:/backtest/acceptedDate/");

	}
}
