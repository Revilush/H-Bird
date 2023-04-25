package xbrl;

//#transactionFormType is only recorded in the first row of t_id - should be in all.fix. form 3 seems to always be blank. See where its='FB'

//TODO: there are a very small number # of erroneous filings with very large sharesOwnedFollowingTransaction or transactionAmount (in the billions).
//add some sort of tool to the i_cleanup utility to dump these accNo into a table to check or remove from i_trans, i_owner, i_issuer,
//i_schema and i_fn

//TODO: There are a large number of errors - typically related to 'fiscal year end' - try to fix

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class InsiderParser {

	private int transId = 0;
	private String issuerCik = "";
	private String issuerTradingSymbol = "";
	private String rptOwnerCik = "";
	private String isDirector = "";
	private String isOfficer = "";
	private String isTenPercentOwner = "";
	private String isOther = "";
	private static String acceptedDate = "";

	private String schemaFile = null;
	private static String issuerFile = null;
	private static String ownerFile = null;
	private static String transactionsFile = null;
	private static String footnoteFile = null;

	static PrintWriter schemaPw = null;
	static PrintWriter issuerPw = null;
	static PrintWriter ownerPw = null;
	static PrintWriter transactionsPw = null;
	static PrintWriter footnotePw = null;

	public void getInsider(String xmlFile, String accNo, String local)
			throws Exception {

		if (local.equals("yes")) {
			System.out.println("parsing local");
			String localPath = xmlFile.substring(0, xmlFile.lastIndexOf("\\"));
			String accHtmPath = localPath + "/htm/" + accNo + "-index.htm";
			// System.out.println("accHtmPath::" + accHtmPath);

			String html = Utils.readTextFromFileWithSpaceSeparator(accHtmPath);
			Document document = Jsoup.parse(html, accHtmPath);
			Pattern ptrnA = Pattern
					.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
			Elements dateElement = document.getElementsMatchingOwnText(ptrnA);

			if (dateElement.size() < 1) {
				acceptedDate = "0";
				// System.out.println("accNo in error::" + accNo);
			} else
				acceptedDate = dateElement.get(0).text();
		}

		// reads xml file
		String html = Utils.readTextFromFileWithSpaceSeparator(xmlFile);
		
		// Jsoup grabs url into document
		Document xml = Jsoup.parse(html, xmlFile);

		File filePath = new File(xmlFile);
		// System.out.println(xmlFile);
		String filename = filePath.getName().substring(0,
				filePath.getName().lastIndexOf("."));
		filename.substring(0, 20);
		Utils.createFoldersIfReqd(filePath.getParent() + "/csv/");
		// System.out.println(xmlFile);

		schemaFile = filePath.getParent() + "/csv/" + filename + "_schema.csv";
		schemaPw = new PrintWriter(schemaFile);

		issuerFile = filePath.getParent() + "/csv/" + filename + "_issuer.csv";
		issuerPw = new PrintWriter(issuerFile);

		ownerFile = filePath.getParent() + "/csv/" + filename + "_owner.csv";
		ownerPw = new PrintWriter(ownerFile);

		transactionsFile = filePath.getParent() + "/csv/" + filename
				+ "_trans.csv";
		transactionsPw = new PrintWriter(transactionsFile);

		footnoteFile = filePath.getParent() + "/csv/" + filename + "_fn.csv";
		footnotePw = new PrintWriter(footnoteFile);

		Elements notSubjectToSection16 = xml
				.getElementsByTag("notSubjectToSectoin16");
		Elements schemaVersion = xml.getElementsByTag("schemaVersion");
		Elements documentType = xml.getElementsByTag("documentType");
		Elements periodOfReport = xml.getElementsByTag("periodOfReport");
		Elements form3HoldingsReported = xml
				.getElementsByTag("form3HoldingsReported");
		Elements form4TransactionsReported = xml
				.getElementsByTag("form4TransactionsReported");
		Elements dateOfOriginalSubmission = xml
				.getElementsByTag("dateOfOriginalSubmission");

		// System.out.println(notSubjectToSection16.text() + "||"
		// + schemaVersion.text() + "||" + documentType.text() + "||"
		// + periodOfReport.text() + "||" + form3HoldingsReported.text()
		// + "||" + form4TransactionsReported.text());

		schemaPw.println(accNo + "||" + notSubjectToSection16.text() + "||"
				+ schemaVersion.text() + "||" + documentType.text() + "||"
				+ periodOfReport.text() + "||" + form3HoldingsReported.text()
				+ "||" + form4TransactionsReported.text() + "||"
				+ dateOfOriginalSubmission.text());

		schemaPw.close();
		schemaFile = schemaFile.replace("\\", "/");
		DividendSentenceExtractor.loadIntoMysql(schemaFile, "i_schema");

		Elements issuer = xml.getElementsByTag("issuer");
		Elements reportingOwner = xml.getElementsByTag("reportingOwner");
		Elements nonDerivativeTransaction = xml
				.getElementsByTag("nonDerivativeTransaction");
		Elements nonDerivativeSecurity = xml
				.getElementsByTag("nonDerivativeSecurity");
		Elements nonDerivativeHolding = xml
				.getElementsByTag("nonDerivativeHolding");
		Elements derivativeTransaction = xml
				.getElementsByTag("DerivativeTransaction");
		Elements derivativeSecurity = xml
				.getElementsByTag("derivativeSecurity");

		Elements derivativeHolding = xml.getElementsByTag("DerivativeHolding");
		Elements footNotes = xml.getElementsByTag("footnote");

		for (Element ele : issuer) {
			System.out.println("getIssuerDetails");
			getIssuerDetails(ele, footNotes, accNo);
			issuerPw.close();
		}
		
		for (Element ele : reportingOwner) {
			getReportingOwnerDetails(ele, footNotes, accNo);
			ownerPw.close();
		}

		for (Element ele : nonDerivativeTransaction) {
			getNonDerivativeTransaction(ele, footNotes, accNo);
		}
		for (Element ele : nonDerivativeSecurity) {
			getNonDerivativeTransaction(ele, footNotes, accNo);
		}

		for (Element ele : nonDerivativeHolding) {
			getNonDerivativeHolding(ele, footNotes, accNo);
		}
		for (Element ele : derivativeTransaction) {
			getDerivativeTransaction(ele, footNotes, accNo);
		}

		for (Element ele : derivativeSecurity) {
			getDerivativeTransaction(ele, footNotes, accNo);
		}
		for (Element ele : derivativeHolding) {
			getDerivativeHolding(ele, footNotes, accNo);
		}
		
		transactionsPw.close();
		transId = 0;
		footnotePw.close();
	}

	private void getIssuerDetails(Element e, Elements footNotes, String accNo) {
		
		issuerTradingSymbol = "";
		issuerCik = "";

		Element node = e.getElementsByTag("issuerCik").first();
		issuerCik = getElementValue(node, "issuerCik");
		getFootNote(node, footNotes, accNo);
		
		node = e.getElementsByTag("issuerName").first();
		String issuerName = getElementValue(node, "issuerName");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("issuerTradingSymbol").first();
		issuerTradingSymbol = getElementValue(node, "issuerTradingSymbol");
		getFootNote(node, footNotes, accNo);

		System.out.println(accNo + "||" + issuerCik + "||" + issuerName + "||"
				+ issuerTradingSymbol);

		issuerPw.println(accNo + "||" + issuerCik + "||" + issuerName + "||"
				+ issuerTradingSymbol + "\r");
		
	}

	private void getReportingOwnerDetails(Element e, Elements footNotes,
			String accNo) {

		isDirector = "";
		isOfficer = "";
		isTenPercentOwner = "";
		isOther = "";
		rptOwnerCik = "";

		Element node = e.getElementsByTag("rptOwnerCik").first();
		rptOwnerCik = getElementValue(node, "rptOwnerCik");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("rptOwnerName").first();
		String rptOwnerName = getElementValue(node, "rptOwnerName");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("rptOwnerStreet1").first();
		String rptOwnerStreet1 = getElementValue(node, "rptOwnerStreet1");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("rptOwnerStreet2").first();
		String rptOwnerStreet2 = "";
		if (null != node && null != getElementValue(node, "rptOwnerStreet2")
				&& null != node.tagName()) {
			rptOwnerStreet2 = getElementValue(node, "rptOwnerStreet2");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("rptOwnerCity").first();
		String rptOwnerCity = getElementValue(node, "rptOwnerCity");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("rptOwnerState").first();
		String rptOwnerState = getElementValue(node, "rptOwnerState");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("rptOwnerZipCode").first();
		String rptOwnerZipCode = getElementValue(node, "rptOwnerZipCode");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("isDirector").first();
		if (null != node && null != getElementValue(node, "isDirector")
				&& null != node.tagName()) {
			isDirector = getElementValue(node, "isDirector");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("isOfficer").first();
		if (null != node && null != getElementValue(node, "isOfficer")
				&& null != node.tagName()) {
			isOfficer = getElementValue(node, "isOfficer");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("isTenPercentOwner").first();
		if (null != node && null != getElementValue(node, "isTenPercentOwner")
				&& null != node.tagName()) {
			isTenPercentOwner = getElementValue(node, "isTenPercentOwner");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("isOther").first();
		if (null != node && null != getElementValue(node, "isOther")
				&& null != node.tagName()) {
			isOther = getElementValue(node, "isOther");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("officerTitle").first();
		String officerTitle = "";
		if (null != node && null != getElementValue(node, "officerTitle")
				&& null != node.tagName()) {
			officerTitle = getElementValue(node, "officerTitle");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("otherText").first();
		String otherText = "";
		if (null != node && null != getElementValue(node, "otherText")
				&& null != node.tagName()) {
			otherText = getElementValue(node, "otherText");
			getFootNote(node, footNotes, accNo);
		}

		// System.out.println(accNo + "||" + rptOwnerCik + "||" + rptOwnerName
		// + "||" + rptOwnerStreet1 + "||" + rptOwnerStreet2 + "||"
		// + rptOwnerCity + "||" + rptOwnerState + "||" + rptOwnerZipCode
		// + "||" + isDirector + "||" + isOfficer + "||"
		// + isTenPercentOwner + "||" + isOther + "||" + officerTitle
		// + "||" + otherText);

		ownerPw.println(accNo + "||" + rptOwnerCik + "||" + rptOwnerName + "||"
				+ rptOwnerStreet1 + "||" + rptOwnerStreet2 + "||"
				+ rptOwnerCity + "||" + rptOwnerState + "||" + rptOwnerZipCode
				+ "||" + isDirector + "||" + isOfficer + "||"
				+ isTenPercentOwner + "||" + isOther + "||" + officerTitle
				+ "||" + otherText + "\n");
		
	}

	private void getNonDerivativeTransaction(Element e, Elements footNotes,
			String accNo) throws FileNotFoundException {

		transId++;
		// System.out.println("nonDT accepteddate:: " + acceptedDate);
		Element node = e.getElementsByTag("securityTitle").first();
		String securityTitle = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("transactionDate").first();
		String transactionDate = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("deemedExecutionDate").first();
		String deemedExecutionDate = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			deemedExecutionDate = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("transactionCoding").first();
		getFootNote(node, footNotes, accNo);
		String transactionFormType = getElementValue(node,
				"transactionFormType");
		// System.out.println("TC: " + getElementValue(node,
		// "transactionCode"));

		// node = e.getElementsByTag("transactionCode").first();
		// String transactionCode = "";
		// if (node != null && null != getElementValue(node, "value")
		// && null != node.tagName()
		// && !getElementValue(node, "value").isEmpty()) {
		// transactionCode = getElementValue(node, "transactionCode");
		// }
		//
		// node = e.getElementsByTag("equitySwapInvolved").first();
		// String equitySwapInvolved = "";
		// if (node != null && null != getElementValue(node, "value")
		// && null != node.tagName()
		// && !getElementValue(node, "value").isEmpty()) {
		// equitySwapInvolved = getElementValue(node, "equitySwapInvolved");
		// }

		String transactionCode = "";
		if (getElementValue(node, "transactionCode") != null) {
			transactionCode = getElementValue(node, "transactionCode");
		}

		String equitySwapInvolved = "";
		if (getElementValue(node, "equitySwapInvolved") != null) {
			equitySwapInvolved = getElementValue(node, "transactionCode");
		}

		node = e.getElementsByTag("transactionTimeliness").first();
		String transactionTimeliness = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			transactionTimeliness = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("transactionShares").first();
		String transactionShares = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("transactionPricePerShare").first();
		String transactionPricePerShare = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("transactionAcquiredDisposedCode").first();
		String transactionAcquiredDisposedCode = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("sharesOwnedFollowingTransaction").first();
		String sharesOwnedFollowingTransaction = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("directOrIndirectOwnership").first();
		String directOrIndirectOwnership = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("natureOfOwnership").first();
		String natureOfOwnership = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			natureOfOwnership = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		// System.out.println(accNo + "||" + transId + "||" + "NDT" + "||"
		// + acceptedDate + "||" + issuerTradingSymbol + "||" + isDirector
		// + "||" + isOfficer + "||" + isTenPercentOwner + "||" + isOther
		// + "||" + rptOwnerCik + "||" + issuerCik + "||" + securityTitle
		// + "||" + transactionDate + "||" + deemedExecutionDate + "||"
		// + transactionFormType + "||" + transactionCode + "||"
		// + equitySwapInvolved + "||" + transactionTimeliness + "||"
		// + transactionShares + "||" + transactionPricePerShare + "||"
		// + transactionAcquiredDisposedCode + "||"
		// + sharesOwnedFollowingTransaction + "||"
		// + directOrIndirectOwnership + "||" + natureOfOwnership + "\n");

		transactionsPw.println(accNo + "||" + transId + "||" + "NDT" + "||"
				+ acceptedDate + "||" + issuerTradingSymbol + "||" + isDirector
				+ "||" + isOfficer + "||" + isTenPercentOwner + "||" + isOther
				+ "||" + rptOwnerCik + "||" + issuerCik + "||" + securityTitle
				+ "||||" + transactionDate + "||" + deemedExecutionDate + "||"
				+ transactionFormType + "||" + transactionCode + "||"
				+ equitySwapInvolved + "||" + transactionTimeliness + "||"
				+ transactionShares + "||" + transactionPricePerShare + "||"
				+ transactionAcquiredDisposedCode + "||||||||||"
				+ sharesOwnedFollowingTransaction + "||"
				+ directOrIndirectOwnership + "||" + natureOfOwnership + "\n");
	}

	private void getNonDerivativeHolding(Element e, Elements footNotes,
			String accNo) {
		transId++;

		Element node = e.getElementsByTag("securityTitle").first();
		String securityTitle = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("sharesOwnedFollowingTransaction").first();
		String sharesOwnedFollowingTransaction = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("directOrIndirectOwnership").first();
		String directOrIndirectOwnership = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("natureOfOwnership").first();
		String natureOfOwnership = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			natureOfOwnership = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		// System.out.println(accNo + "||" + transId + "||" + "NDH" + "||"
		// + acceptedDate + "||" + issuerTradingSymbol + "||" + isDirector
		// + "||" + isOfficer + "||" + isTenPercentOwner + "||" + isOther
		// + "||" + rptOwnerCik + "||" + issuerCik + "||" + securityTitle
		// + "||" + sharesOwnedFollowingTransaction + "||"
		// + directOrIndirectOwnership + "||" + natureOfOwnership + "\n");

		transactionsPw.println(accNo + "||" + transId + "||" + "NDH" + "||"
				+ acceptedDate + "||" + issuerTradingSymbol + "||" + isDirector
				+ "||" + isOfficer + "||" + isTenPercentOwner + "||" + isOther
				+ "||" + rptOwnerCik + "||" + issuerCik + "||" + securityTitle
				+ "||||||||||||||||||||||||||||||"
				+ sharesOwnedFollowingTransaction + "||"
				+ directOrIndirectOwnership + "||" + natureOfOwnership + "\n");
	}

	private void getDerivativeTransaction(Element e, Elements footNotes,
			String accNo) {
		transId++;

		Element node = e.getElementsByTag("securityTitle").first();
		String securityTitle = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("conversionOrExercisePrice").first();
		String conversionOrExercisePrice = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			conversionOrExercisePrice = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("transactionDate").first();
		String transactionDate = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("deemedExecutionDate").first();
		String deemedExecutionDate = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			deemedExecutionDate = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("transactionCoding").first();
		getFootNote(node, footNotes, accNo);
		String transactionFormType = getElementValue(node,
				"transactionFormType");

		// node = e.getElementsByTag("transactionCode").first();
		// String transactionCode = "";
		// if (node != null && null != getElementValue(node, "value")
		// && null != node.tagName()
		// && !getElementValue(node, "value").isEmpty()) {
		// transactionCode = getElementValue(node, "transactionCode");
		// }
		//
		// node = e.getElementsByTag("equitySwapInvolved").first();
		// String equitySwapInvolved = "";
		// if (node != null && null != getElementValue(node, "value")
		// && null != node.tagName()
		// && !getElementValue(node, "value").isEmpty()) {
		// equitySwapInvolved = getElementValue(node, "equitySwapInvolved");
		// }
		String transactionCode = "";
		if (getElementValue(node, "transactionCode") != null) {
			transactionCode = getElementValue(node, "transactionCode");
		}

		String equitySwapInvolved = "";
		if (getElementValue(node, "equitySwapInvolved") != null) {
			equitySwapInvolved = getElementValue(node, "transactionCode");
		}

		node = e.getElementsByTag("transactionTimeliness").first();
		String transactionTimeliness = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			transactionTimeliness = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("transactionShares").first();
		String transactionShares = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("transactionPricePerShare").first();
		String transactionPricePerShare = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("transactionAcquiredDisposedCode").first();
		String transactionAcquiredDisposedCode = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("exerciseDate").first();
		String exerciseDate = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			exerciseDate = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("expirationDate").first();
		String expirationDate = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			expirationDate = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("underlyingSecurity").first();
		String underlyingSecurity = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			underlyingSecurity = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("underlyingSecurityShares").first();
		String underlyingSecurityShares = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			underlyingSecurityShares = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("sharesOwnedFollowingTransaction").first();
		String sharesOwnedFollowingTransaction = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("directOrIndirectOwnership").first();
		String directOrIndirectOwnership = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("natureOfOwnership").first();
		String natureOfOwnership = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			natureOfOwnership = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		// System.out.println(accNo + "||" + transId + "||" + "DT" + "||"
		// + acceptedDate + "||" + issuerTradingSymbol + "||" + isDirector
		// + "||" + isOfficer + "||" + isTenPercentOwner + "||" + isOther
		// + "||" + rptOwnerCik + "||" + issuerCik + "||" + securityTitle
		// + "||" + conversionOrExercisePrice + "||" + transactionDate
		// + "||" + deemedExecutionDate + "||" + transactionFormType
		// + "||" + transactionCode + "||" + equitySwapInvolved + "||"
		// + transactionTimeliness + "||" + transactionShares + "||"
		// + transactionPricePerShare + "||"
		// + transactionAcquiredDisposedCode + "||" + exerciseDate + "||"
		// + expirationDate + "||" + underlyingSecurity + "||"
		// + underlyingSecurityShares + "||"
		// + sharesOwnedFollowingTransaction + "||"
		// + directOrIndirectOwnership + "||" + natureOfOwnership + "\n");

		transactionsPw.println(accNo + "||" + transId + "||" + "DT" + "||"
				+ acceptedDate + "||" + issuerTradingSymbol + "||" + isDirector
				+ "||" + isOfficer + "||" + isTenPercentOwner + "||" + isOther
				+ "||" + rptOwnerCik + "||" + issuerCik + "||" + securityTitle
				+ "||" + conversionOrExercisePrice + "||" + transactionDate
				+ "||" + deemedExecutionDate + "||" + transactionFormType
				+ "||" + transactionCode + "||" + equitySwapInvolved + "||"
				+ transactionTimeliness + "||" + transactionShares + "||"
				+ transactionPricePerShare + "||"
				+ transactionAcquiredDisposedCode + "||" + exerciseDate + "||"
				+ expirationDate + "||" + underlyingSecurity + "||"
				+ underlyingSecurityShares + "||"
				+ sharesOwnedFollowingTransaction + "||"
				+ directOrIndirectOwnership + "||" + natureOfOwnership + "\n");
	}

	private void getDerivativeHolding(Element e, Elements footNotes,
			String accNo) {
		transId++;

		Element node = e.getElementsByTag("securityTitle").first();
		String securityTitle = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("conversionOrExercisePrice").first();
		String conversionOrExercisePrice = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			conversionOrExercisePrice = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("deemedExecutionDate").first();
		String deemedExecutionDate = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			deemedExecutionDate = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("exerciseDate").first();
		String exerciseDate = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			exerciseDate = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("expirationDate").first();
		String expirationDate = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			expirationDate = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("underlyingSecurity").first();
		String underlyingSecurity = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			underlyingSecurity = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("underlyingSecurityShares").first();
		String underlyingSecurityShares = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			underlyingSecurityShares = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		node = e.getElementsByTag("sharesOwnedFollowingTransaction").first();
		String sharesOwnedFollowingTransaction = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("directOrIndirectOwnership").first();
		String directOrIndirectOwnership = getElementValue(node, "value");
		getFootNote(node, footNotes, accNo);

		node = e.getElementsByTag("natureOfOwnership").first();
		String natureOfOwnership = "";
		if (null != node && null != getElementValue(node, "value")
				&& null != node.tagName()) {
			natureOfOwnership = getElementValue(node, "value");
			getFootNote(node, footNotes, accNo);
		}

		// System.out.println(accNo + "||" + transId + "||" + "DH" + "||"
		// + acceptedDate + "||" + issuerTradingSymbol + "||" + isDirector
		// + "||" + isOfficer + "||" + isTenPercentOwner + "||" + isOther
		// + "||" + rptOwnerCik + "||" + issuerCik + "||" + securityTitle
		// + "||" + conversionOrExercisePrice + "||" + deemedExecutionDate
		// + "||" + "||" + exerciseDate + "||" + expirationDate + "||"
		// + underlyingSecurity + "||" + underlyingSecurityShares + "||"
		// + sharesOwnedFollowingTransaction + "||"
		// + directOrIndirectOwnership + "||" + natureOfOwnership + "\n");

		transactionsPw.println(accNo + "||" + transId + "||" + "DH" + "||"
				+ acceptedDate + "||" + issuerTradingSymbol + "||" + isDirector
				+ "||" + isOfficer + "||" + isTenPercentOwner + "||" + isOther
				+ "||" + rptOwnerCik + "||" + issuerCik + "||" + securityTitle
				+ "||" + conversionOrExercisePrice + "||||"
				+ deemedExecutionDate + "||||||||||||||||" + exerciseDate
				+ "||" + expirationDate + "||" + underlyingSecurity + "||"
				+ underlyingSecurityShares + "||"
				+ sharesOwnedFollowingTransaction + "||"
				+ directOrIndirectOwnership + "||" + natureOfOwnership + "\n");
	}

	private String getElementValue(Element node, String value) {
		if (null != node && node.hasText()/* && value.length()>0 */) {
			// b/c element is null we 'define' element then check if it is null
			// in order to avoid null exception
			Elements eles = node.getElementsByTag(value);
			if (null != eles && null != eles.first())
				return eles.first().text();
		}
		return "";
	}

	private String getFootNote(Element node, Elements footNotes, String accNo) {
		if (null == node)
			return "";
		Elements nodes = node.getElementsByTag("footnoteId");
		if (null != nodes && nodes.size() > 0 && null != footNotes) {
			Element fnNode = nodes.first();
			String fnId = fnNode.attr("id");
			for (Element fn : footNotes) {
				if (fn.attr("id").equals(fnId)) {
					// System.out.println("Footnote: transId=" + transId +
					// ", id="
					// + fnId + ":parent=" + node.tagName()
					// + "footnote text=" + fn.text());

					footnotePw.println(accNo + "||" + transId + "||" + fnId
							+ "||" + node.tagName() + "||" + fn.text() + "\n");
					return fn.text();
					// do we need the return?
				}
			}
		}
		return "";
	}

	public static int getQuarter(Calendar date) {
		return ((date.get(Calendar.MONTH) / 3) + 1);
	}

	public static String baseFolder = "c:/backtest/insider/";

	public static void dateRangeQuarters(Calendar startDate, Calendar endDate)
			throws SocketException, IOException, SQLException {

		int startYear = startDate.get(Calendar.YEAR);
		int endYear = endDate.get(Calendar.YEAR);
		int startQtr = getQuarter(startDate);
		int endQtr = getQuarter(endDate);

		int QtrYrs = (endYear - startYear) * 4;
		int iQtr = (endQtr - startQtr) + 1;
		int totalQtrs = QtrYrs + iQtr;
		int startYr = startYear;
		iQtr = startQtr;
		Calendar cal = Calendar.getInstance();

		for (int i = 1; i <= totalQtrs; i++) {
			cal.set(Calendar.YEAR, startYr);
			cal.set(Calendar.MONTH, (iQtr * 3) - 1);
			System.out.println("getMasterIdx");
			getMasterIdx(startYr, iQtr, cal);
			String localPath = baseFolder + "/" + startYr + "/QTR" + iQtr;
			 System.out.println("lclpth1: " + localPath);
			File file = new File(localPath + "/master.idx");
			if (!file.exists()) {
				ZipUtils.deflateZipFile(localPath + "/master.zip", localPath
						+ "/");
			}
			
			InsiderParserLocal.createMysqlCsv(localPath+"/",startYr,iQtr);
			downloadFilingDetails(localPath);
			iQtr++;
			if (iQtr > 4) {
				startYr++;
				iQtr = 1;
			}
		}
	}
	

	public static String getFolderForDate(Calendar date) {
		return baseFolder + "/" + date.get(Calendar.YEAR) + "/QTR"
				+ getQuarter(date);
	}

	public static void getMasterIdx(int year, int qtr, Calendar endDate)
			throws SocketException, IOException {

		String localPath = baseFolder + "/" + year + "/QTR" + qtr;
		 System.out.println("localPath mIdx:: " + localPath);
		FileSystemUtils.createFoldersIfReqd(localPath);
		// create folder to save files to
		File f = new File(localPath + "/master.idx");
		// identify if file 'master.zip' is in local path.
//		if (f.exists() && !EightK.isCurrentQuarter(endDate)) {
//			System.out.println("not downloading master.idx exists="+f.getAbsolutePath());
//			return;
//		}
//		if (f.exists() && EightK.isCurrentQuarter(endDate)) {
//			 System.out.println("current-redownloading="+f.getAbsolutePath());
//			QuarterlyMasterIdx.download(year, qtr, localPath, "/master.zip");
////			ZipUtils.deflateZipFile(localPath + "/master.zip", localPath + "/");
//		}
//		if (!f.exists()) {
			
//			File f2 = new File("c:/backtest/master"+ "/" + year + "/QTR" + qtr+"/master.idx");
//			if(f2.exists()){
//				String str = Utils.readTextFromFile(f2.getAbsolutePath());
//				PrintWriter pw = new PrintWriter(f);
//				pw.println(str);
//				pw.close();
//				return;
//			}
			
			System.out.println("downloading master.idx="+f.getAbsolutePath());
			QuarterlyMasterIdx.download(year, qtr, localPath, "/master.zip");
//			ZipUtils.deflateZipFile(localPath + "/master.zip", localPath + "/");
//		}
	}

	public static void downloadFilingDetails(String localPath) {

		String file = localPath + "/master.csv";
		System.out.println("master.csv localPath="+localPath);
		
		BufferedReader rdr = null;
		try {
			rdr = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		String line;

		try {
			while ((line = rdr.readLine()) != null) {
				String[] items = line.split("\\|\\|");
//				if (items.length < 5)
//					continue;
				if (items[4].contains("edgar")
						&& (items[2].equals("3") || items[2].equals("4")
								|| items[2].equals("5")
								|| items[2].equals("3/A")
								|| items[2].equals("4/A") || items[2]
									.equals("5/A"))) {
					System.out.println("start going thru master.csv file");
					String fileDate = items[3];
					String secFile = items[4];
					String acc = secFile.substring(
							secFile.lastIndexOf("/") + 1,
							secFile.lastIndexOf("."));
					// System.out.println("accNO:: " + acc);
					String accHtm = acc + "-index.htm";
					String accNoHyphens = acc.replace("-", "");
					File fileHtm = new File(localPath + "/htm/" + accHtm);

					String suffix = secFile.substring(0,
							secFile.lastIndexOf("/"));

					String wwwSecGovPath = (suffix + "/" + accNoHyphens + "/" + accHtm);
					String wwwSecGovPathHtm = (suffix + "/" + accNoHyphens
							+ "/" + acc + "-index.htm");

					Utils.createFoldersIfReqd(localPath + "/htm");
					System.out.println("secGov" + wwwSecGovPathHtm);
					String xmlFileName = accHtm.substring(0, 20) + ".xml";
					File xmlFile = new File(localPath + "/"
							+ xmlFileName);
					if (!xmlFile.exists() || !fileHtm.exists()
							|| fileHtm.length() < 1100
							|| xmlFile.length() < 1100) {
						
						if(fileHtm.exists())
						fileHtm.delete();
						
						if(xmlFile.exists())
						xmlFile.delete();

						Xbrl.download(wwwSecGovPath, localPath + "/htm", accHtm);
						XBRLInfoSecGov info = new XBRLInfoSecGov(
								"https://www.sec.gov/Archives/"
										+ wwwSecGovPathHtm);

						if (info.acceptedDate == null)
							continue;
						if (info.acceptedDate.length() > 0)
							acceptedDate = (info.acceptedDate);
						String xmlUrl = info.xmlUrl;
						// System.out.println("xmlUrl:" + xmlUrl);

						String acceptD = (acceptedDate.substring(0, 7));
						fileDate = fileDate.substring(0, 7);

						if (!acceptD.equals(fileDate)) {
							acceptedDate = fileDate;
						}
						// System.out.println("a-date::" + acceptedDate);
						// System.out.println("xmlUrl::" + xmlUrl);
						// String fye = (info.fye);
						if (xmlUrl.length() > 1)
							xmlUrl = xmlUrl.substring(28, xmlUrl.length());

						// System.out.println("xmlUrlstub::" + xmlUrl);
						// in xbrl.download "https://www.sec.gov/Archives/" is
						// server - so this deletes server from string

						// xx - see 8k
						xmlFileName = accHtm.substring(0, 20) + ".xml";
						// System.out.println("xmlFileName:: " + xmlFileName);
						Xbrl.download(xmlUrl, localPath, xmlFileName);
						String xmlFilePathStr = xmlFile.getAbsolutePath();
						xmlFilePathStr = xmlFilePathStr.replaceAll("\\\\", "/");
						// System.out.println("fileHtmStr::" + xmlFilePathStr);

						InsiderParser parser = new InsiderParser();
						// if not local parsing as here anything other than
						// "yes" for 3rd parameter. here I put "no"
						parser.getInsider(xmlFilePathStr, acc, "no");
						loadIntoMysql();
						
					}
				}
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			try {
				rdr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void insiderRSS(String url, String acc) throws FileNotFoundException {
		XBRLInfoSecGov info = null;
		try {
			info = new XBRLInfoSecGov(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// System.out.println("acceptedDate:: " + acceptedDate);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date aDate = new Date();
		try {
			acceptedDate = (info.acceptedDate);
			if (acceptedDate == null)
				return;
			aDate = sdf.parse(acceptedDate);
			// System.out.println("aDate:: " + aDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Calendar acceptedDate1 = Calendar.getInstance();
		acceptedDate1.setTime(aDate);
		// System.out.println("acceptedDate1:: " + aDate);
		String localPath = InsiderParser.getFolderForDate(acceptedDate1);
		// System.out.println("localPath:: " + localPath);
		// System.out.println("url:: " + url);
		String url2 = url.substring(28, url.length());
		// System.out.println("url2:: " + url2);
		String accHtm = acc + "-index.htm";

		File file = new File(localPath + "/htm/" + accHtm);
		// System.out.println("file:: " + file);

		Utils.createFoldersIfReqd(localPath);
		if (file.exists()) {
			return;
		}
		if (!file.exists()) {

			try {
				Xbrl.download(url2, localPath + "/htm", accHtm);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			String xmlUrl = info.xmlUrl;

			xmlUrl = xmlUrl.substring(28, xmlUrl.length());
			// System.out.println("xmlUrlstub::" + xmlUrl);
			// in xbrl.download "https://www.sec.gov/Archives/" is
			// server - so this deletes server from string
			String xmlFileName = accHtm.substring(0, 20) + ".xml";
			// System.out.println("xmlFileName:: " + xmlFileName);
			try {
				Xbrl.download(xmlUrl, localPath, xmlFileName);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			File xmlFilePath = new File(localPath + "/" + xmlFileName);
			String xmlFilePathStr = xmlFilePath.getAbsolutePath();
			xmlFilePathStr = xmlFilePathStr.replaceAll("\\\\", "/");
			// System.out.println("fileHtmStr::" + xmlFilePathStr);

			InsiderParser parser = new InsiderParser();
			try {
				// "no" is b/c not parsing locally
				parser.getInsider(xmlFilePathStr, acc, "no");
			} catch (Exception e) {
				e.printStackTrace();
			}
			loadIntoMysql();
		}
	}

	public static void loadIntoMysql() throws FileNotFoundException {

		transactionsPw.close();
		issuerPw.close();
		ownerPw.close();
		footnotePw.close();

		issuerFile = issuerFile.replace("\\", "/");
		DividendSentenceExtractor.loadIntoMysql(issuerFile, "i_issuer");

		ownerFile = ownerFile.replace("\\", "/");
		DividendSentenceExtractor.loadIntoMysql(ownerFile, "i_owner");

		footnoteFile = footnoteFile.replace("\\", "/");
		DividendSentenceExtractor.loadIntoMysql(footnoteFile, "i_fn");

		transactionsFile = transactionsFile.replace("\\", "/");
		DividendSentenceExtractor.loadIntoMysql(transactionsFile, "i_trans");
	}

	public static void main(String[] args) throws Exception {
		
		String startDateStr = "20190101";
		String endDateStr =   "20190101";
		
		/*
		Scanner Scan = new Scanner(System.in);
		System.out
				.println("Enter start date for time period to get insider filings (yyyymmdd)");
		startDateStr = Scan.nextLine();

		System.out
				.println("Enter end date for time period to get insider filings (yyyymmdd)");
		String endDateStr = Scan.nextLine();
*/
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date sDate = new Date();
		sDate = sdf.parse(startDateStr);
		Calendar startDate = Calendar.getInstance();
		startDate.setTime(sDate);
		Date eDate = new Date();
		eDate = sdf.parse(endDateStr);
		Calendar endDate = Calendar.getInstance();
		endDate.setTime(eDate);
		String earliestDateStr = "20030501";
		Date firstDate = sdf.parse(earliestDateStr);
		Calendar badDate = Calendar.getInstance();
		badDate.setTime(firstDate);
/*
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
		if (startDate.after(firstDate)) {
			System.out
					.println("Insider filings are not available prior to May 2003. Please re-enter.");
		}
*/		
		dateRangeQuarters(startDate, endDate);
	}
}
