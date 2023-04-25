package xbrl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.xml.sax.SAXException;

public class DivParser {

	public static String getTextSimple(String html, String fileType)
			throws IOException {
		// System.out.println("this is the html::" + html);
		Pattern Html2TextPattern = Pattern.compile("(?<=\\>)(.*?)(?=\\<)",
				Pattern.DOTALL);
		Matcher matcher = null;
		// System.out.println("fileType::: " + fileType);
		matcher = Html2TextPattern.matcher(html);
		String text = "";
		StringBuffer sb = new StringBuffer();

		while (matcher.find()) {
			text = new String(matcher.group()).replaceAll("Jan\\.", "January")
					.replaceAll("Feb\\.", "February")
					.replaceAll("Mar\\.", "March")
					.replaceAll("Apr\\.", "April").replaceAll("Jun.", "June")
					.replaceAll("Jul\\.", "July").replaceAll("Aug.", "August")
					.replaceAll("Augustst", "August")
					.replaceAll("Sep\\.", "September")
					.replaceAll("Oct\\.", "October")
					.replaceAll("Nov\\.", "November")
					.replaceAll("Dec\\.", "December")
					.replaceAll("\\&nbsp;", " ").replaceAll("\\xA0", " ")
					.replaceAll("\\&#160;", " ")
					.replaceAll("\\&#147;|\\&#148;", "\"")
					.replaceAll("\\&#146;", "'").replaceAll("\\&#188;", ".25")
					.replaceAll("\\&#189;|\\&#x2019;", ".5")
					.replaceAll("\\&#190;", ".75").replaceAll("\\&.{1,6};", "")
					.replaceAll("â€œ", "\"").replaceAll("â€™", "'")
					.replaceAll("â€?", "\"").replaceAll("“", "\"")
					.replaceAll("”", "\"").replaceAll("Â", "")
					.replaceAll("-1\\/4 cents| 1\\/4 cents", ".25 cents")
					.replaceAll("-1\\/3 cents| 1\\/3 cents", ".333 cents")
					.replaceAll("-1\\/2 cents| 1\\/2 cents", ".5 cents")
					.replaceAll("-2\\/3 cents| 2\\/3 cents", ".667 cents")
					.replaceAll("-3\\/4 cents| 3\\/4 cents", ".75 cents")
					.replaceAll(";", "\\.").replaceAll("\\&.{2,6};", "")
					.replaceAll("\\.A", ". A").replaceAll("\\.B", ". B")
					.replaceAll("\\.C", ". C").replaceAll("\\.D", ". D")
					.replaceAll("\\.E", ". E").replaceAll("\\.F", ". F")
					.replaceAll("\\.G", ". G").replaceAll("\\.H", ". H")
					.replaceAll("\\.I", ". I").replaceAll("\\.J", ". J")
					.replaceAll("\\.K", ". K").replaceAll("\\.L", ". L")
					.replaceAll("\\.M", ". M").replaceAll("\\.N", ". N")
					.replaceAll("\\.O", ". O").replaceAll("\\.P", ". P")
					.replaceAll("\\.Q", ". Q").replaceAll("\\.R", ". R")
					.replaceAll("\\.S", ". S").replaceAll("\\.T", ". T")
					.replaceAll("\\.U", ". U").replaceAll("\\.V", ". V")
					.replaceAll("\\.W", ". W").replaceAll("\\.X", ". X")
					.replaceAll("\\.Y", ". Y").replaceAll("\\.Z", ". Z")
					.replaceAll("U\\. S\\.", "U\\.S\\.")
					.replaceAll(" [ ]+", " ");
			sb.append(text);
		}
//		File file = new File("c:/backtest/temp.txt");
//		if(file.exists())
//			file.delete();
//		PrintWriter textPw = new PrintWriter(file);
//		textPw.append(sb.toString());
//		textPw.close();
//		System.out.println("this is the simplex text:: " + sb.toString());
		return sb.toString();
	}

	public static void downloadEx99(String divUrl, String acceptedDate,
			String acc, String fileType) throws IOException, ParseException {
		OutputStream out = null;
		URLConnection conn = null;
		InputStream in = null;
		String extn = ".html";
		if (fileType.contains("Complete"))
			extn = ".txt";
		File fileOrig = new File(EightK.getFolderForAcceptedDate(acceptedDate)
				+ "/" + acc + "_" + fileType + extn);
		if (!fileOrig.exists()) {
			String localFileName = EightK
					.getFolderForAcceptedDate(acceptedDate)
					+ "/"
					+ acc
					+ "_"
					+ fileType + extn;
			// System.out.println("localFileName::" + localFileName);
			try {
				URL url = new URL(divUrl);
				out = new BufferedOutputStream(new FileOutputStream(
						localFileName));
				conn = url.openConnection();
				in = conn.getInputStream();
				byte[] buffer = new byte[1024];
				int numRead;
				@SuppressWarnings("unused")
				long numWritten = 0;
				while ((numRead = in.read(buffer)) != -1) {
					out.write(buffer, 0, numRead);
					numWritten += numRead;
				}

				// System.out.println(localFileName + "\t" + numWritten);
			} catch (Exception exception) {
				exception.printStackTrace();
			} finally {
				try {
					if (in != null) {
						in.close();
					}
					if (out != null) {
						out.close();
					}
				} catch (IOException ioe) {
				}
			}
		}
		parseHtml(acceptedDate, acc, divUrl, fileType, extn);
		// System.out.println("parseHtml");
	}

	@SuppressWarnings("deprecation")
	public static void parseHtml(String acceptedDate, String acc,
			String address, String fileType, String extn) throws IOException,
			ParseException {

		if (fileType.equalsIgnoreCase("bsnWire") && !address.contains("Text")) {

			String localFileName = EightK
					.getFolderForAcceptedDate(acceptedDate)
					+ "/"
					+ acc
					+ "_"
					+ fileType + extn;
			String html = Utils.readTextFromFile(localFileName);
			Document doc = Jsoup.parse(html);
			Element time = null;
			// Element time = doc.getElementsByTag("time").get(0);
			if (doc.getElementsByTag("time").hasText()

			// && doc.getElementsByTag("time").size() > 0
			) {
				time = doc.getElementsByTag("time").get(0);

				// some date formats in bsnWire are yyyy:mm:dd hh:mm
				String date = time.text();
				if (date.substring(0, 3).contains("201")) {
					date = date.replaceAll("[^\\p{ASCII}|[ ]]", "-")
							.replaceAll("---", " ");
					acceptedDate = date.substring(0, 16) + ":00";
					// System.out.println("acceptedDate if non-ascii::"+
					// acceptedDate);
				} else {
					// December 13, 2013 07:00 PM Eastern Standard Time
					SimpleDateFormat sdf = new SimpleDateFormat(
							"MMM dd, yyyy HH:mm a z");
					Date dt = sdf.parse(date);
					if (date.contains(" PM "))
						dt.setHours(dt.getHours() + 12);
					SimpleDateFormat sdf2 = new SimpleDateFormat(
							"yyyy-MM-dd HH:mm");
					// System.out.println("date (sdf2.format(dt)::"
					// + sdf2.format(dt) + ":00");
					acceptedDate = sdf2.format(dt) + ":00";
				}
			}
		}

		String fileOriginal = null;

		if (address.contains("Text")) {
			fileOriginal = address;
			// System.out.println(fileOriginal);
		} else {
			fileOriginal = EightK.getFolderForAcceptedDate(acceptedDate) + "/"
					+ acc + "_" + fileType + extn;
		}

		// System.out.println("parseHtml fileOrig::" + fileOriginal);

		File fileReplaceAll = new File(
				EightK.getFolderForAcceptedDate(acceptedDate) + "/" + acc
						+ "Link.html");
		String ex99html = "";

		if (fileReplaceAll.exists()) {
			fileReplaceAll.delete();
		}

		if (!fileReplaceAll.exists()) {
			FileWriter writer = new FileWriter(fileReplaceAll);

			try {
				ex99html = Utils.readTextFromFile(fileOriginal);
				ex99html = ex99html.replaceAll(
						"(?<=[ \\w\\.]{1})(\\r\\n)(?=[ \\w\\.]{1})", " ");
				writer.append(ex99html);
				writer.flush();
				writer.close();
				// System.out.println("read ex99html - non simple:: " +
				// ex99html);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		fileReplaceAll.delete();
		ex99html = getTextSimple(ex99html, fileType);
		DividendSentenceExtractor.getTextBlock(ex99html, acceptedDate, acc,
				fileType);
	}

	public static void main(String[] arg) throws MalformedURLException,
			IOException, SAXException, ParseException {
		String html = Utils
				.readTextFromFile("c:/backtest/THE GOLDMAN SACHS GROUP, INC.htm");
//		 String html =
//		 Utils.readTextFromFileWithSpaceSeparator("c:/backtest/THE GOLDMAN SACHS GROUP, INC.htm");
		DivParser.getTextSimple(html, "tmp");
		// DivParser.parseHtml("2013-12-11", "20131205006374", "20131205006374",
		// "bsnWire", ".html");
	}
}