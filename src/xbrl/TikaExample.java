package xbrl;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.WriteOutContentHandler;
import org.xml.sax.SAXException;

import charting.FileSystemUtils;

public class TikaExample {

	public static String parseToStringExample(String sourceFile) throws IOException, SAXException, TikaException {
	    byte[] contents = FileSystemUtils.readByteArrayFromFile(sourceFile);
	    InputStream stream = new ByteArrayInputStream(contents);
	    Tika tika = new Tika();
	    try {
	        return tika.parseToString(stream);
	    } finally {
	        stream.close();
	    }
	}

	public static String parseToPlainText(String sourceFile) throws IOException, SAXException, TikaException {
	    BodyContentHandler handler = new BodyContentHandler();
	    byte[] contents = FileSystemUtils.readByteArrayFromFile(sourceFile);
	    InputStream stream = new ByteArrayInputStream(contents);
	    AutoDetectParser parser = new AutoDetectParser();
	    Metadata metadata = new Metadata();
	    try {
	        parser.parse(stream, handler, metadata);
	        return handler.toString();
	    } finally {
	        stream.close();
	    }
	}
	
	public static void OCR(String sourceFile) throws FileNotFoundException, IOException, SAXException,
	TikaException {
		
		TesseractOCRConfig cfg = new TesseractOCRConfig();
		cfg.setTesseractPath("c:/Program Files (x86)/Tesseract-OCR");
		ParseContext pc = new ParseContext();
		pc.set(TesseractOCRConfig.class, cfg);
		
		 byte[] contents = FileSystemUtils.readByteArrayFromFile(sourceFile);
		TesseractOCRParser tp = new TesseractOCRParser();
		StringWriter writer = new StringWriter();
		
		WriteOutContentHandler handler = new WriteOutContentHandler(writer);
		Metadata metadata = new Metadata();
		
		tp.parse(new ByteArrayInputStream(contents), handler, metadata, pc);
		
		System.out.println("done: text is: "+writer.toString());
	}
	
	public static void main(String[] arg) throws IOException, SAXException, TikaException {

		//this will parse to plain text any: .doc, docx, xlsx, xls, pdf (not image), ppt, pptx
		
		//tiff, jpeg, image pdf
		
		//System.out.println(parseToPlainText("c:/temp/guidanceTest.docx"));
		OCR("C:/stuff/jobSearch/perkins/Partnership Agreement 2017_1101.PDF");
		
	}
	
}
