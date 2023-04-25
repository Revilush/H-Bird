package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.ToXMLContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DocxParser {

	public String getDocxHTML(String docPath) throws IOException, SAXException, TikaException {
		ContentHandler handler = new ToXMLContentHandler();

		AutoDetectParser parser = new AutoDetectParser();
		Metadata metadata = new Metadata();
		try (InputStream stream = new FileInputStream(docPath)) {
			parser.parse(stream, handler, metadata);
			return handler.toString();
		}
	}

	public String readWordDocXFile(String filename) {
		StringBuilder sb = new StringBuilder();
		try {
			File file = new File(filename);
			FileInputStream fis = new FileInputStream(file.getAbsolutePath());

			XWPFDocument document = new XWPFDocument(fis);

			List<XWPFParagraph> paragraphs = document.getParagraphs();

			for (XWPFParagraph para : paragraphs) {
//				System.out.println(">> new para start::");
				List<String> runFormat = new ArrayList<>();
				// for each run within a para
				for (XWPFRun run : para.getRuns()) {
					runFormat.clear();
					if (run.isBold()) {
						runFormat.add("B");
						System.out.println(runFormat + " :: " + run.toString());
					}
					if (run.isItalic()) {
						runFormat.add("I");
						System.out.println(runFormat + " :: " + run.toString());
					}
					if (null != run.getUnderline()) {
						UnderlinePatterns up = run.getUnderline();
						if (UnderlinePatterns.valueOf(up.getValue()) != UnderlinePatterns.NONE) {
							runFormat.add("U");
							System.out.println(runFormat + " :: " + run.toString());
							}
					}
					// font details for the run
					// run.getFontSize();

//					System.out.println(runFormat + " :: " + run.toString());
				}

				sb.append(para.getText() + "\r\n");

			}
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	public static void main(String[] args) throws IOException, SAXException, TikaException {
		DocxParser dp = new DocxParser();
		String file = "C:\\Perkins_Matters\\Fox_Europcar\\Fox_Europcar 2021-1 - Base Indenture.DOCX";

		System.out.println(dp.getDocxHTML(file));

		String docx = dp.readWordDocXFile(file);
		System.out.println(docx);
	}
}
