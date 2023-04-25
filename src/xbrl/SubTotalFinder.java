package xbrl;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

public class SubTotalFinder {

	public static Pattern TabNumberTabPattern = Pattern
			.compile("(\t[ ]{0,3}[\\$\\(\\d,_\\-\\)\\.]{1,50})[ ]{0,3}(\t[ ]{0,3}[\\$\\(\\d,_\\-\\)\\.]{1,50})+|"
					+ "(\t[ ]{0,3}[\\$\\(\\d,_\\-\\.\\)]{3,50})");

	public static Pattern Pnumber = Pattern.compile("[\\$\\(\\-]{0,2}\\d");

	public String removeGarbageLines(String tableText) {
		NLP nlp = new NLP();

		int i = 0;
		String[] lines = tableText.split("\r\n");
		for (i = 0; i < lines.length; i++) {
			if (//
			(lines[i].contains("audit")//
					|| (nlp.doesPatternMatch(lines[i], nlp.DecimalPattern) && !lines[i].toLowerCase().contains("shares")
							&& !lines[i].toLowerCase().contains("outstand")))
					// below qualifies everything
					&& !nlp.doesPatternMatch(lines[i], Pnumber)) {
				// System.Xut.println("removed line: " + lines[i]);
				lines = (String[]) ArrayUtils.remove(lines, i);
			}
		}
		return StringUtils.join(lines, "\r\n");
	}

	public String multilpleRowNameMergeUtility(String tableText) {

		int i = 0;
		String[] lines = tableText.split("\r\n");
		StringBuffer sb = new StringBuffer();

		boolean hasCaps = false;
		// , lineMerged = false;
		for (i = 0; i < lines.length; i++) {
			if (i + 1 < lines.length && (lines[i].equals("xx") || lines[i].equals("xx xx")
					|| lines[i].equals("xx xx xx") || lines[i].equals("xx xx xx xx"))) {
				lines = (String[]) ArrayUtils.remove(lines, i);
				continue;
			}

			if (i + 1 < lines.length && lines[i + 1].length() > 1) {
				hasCaps = Character.isUpperCase(lines[i + 1].charAt(0));
				// System.Xut.println("next line hasCaps=" + hasCaps);
				if (!lines[i].contains("\t") && !lines[i].contains(":") && lines[i].split(" ").length > 2
						&& hasCaps == false) {
					lines[i] = lines[i] + " " + lines[i + 1];
					// System.Xut.println("merged lines at" + i + "==" +
					// lines[i]);
					// merge lines whenever no tab in line 1 or ":" and next
					// line start with lower case letter
					lines = (String[]) ArrayUtils.remove(lines, i + 1);
					if (i + 1 < lines.length)
						i--;
				}
			}
		}
		// System.Xut.println(Arrays.toString(lines));
		for (String line : lines) {
			sb.append(line).append("\r\n");
		}

		// System.Xut.println("table after applicable rows merged:"
		// + sb.toString() + " :end");
		return sb.toString();
	}

	public static String columnUtility(String tableText, int columnHeadingCount) throws IOException {

		NLP nlp = new NLP();

		// System.Xut.println("ColumnHeading Count:" + columnHeadingCount);
		String[] lines = tableText.split("\r\n"), lineCols;
		List<String> numberLocs;
		// ^ represents from start. if inside[ ] it is negate
		// can't include \\. b/c it could be #.# or .#
		int idxSpace;
		String col1, col2, joinChar;

		for (int i = 0; i < lines.length; i++) {
			// System.Xut.println("lines[i]" + lines[i]);
			lineCols = lines[i].split("\t");
			// System.Xut.println("lineCols Array after split"
			// + ArrayUtils.toString(lineCols));
			// System.Xut.println("# of tabs in lines[i]: " + lineCols.length);
			if (lineCols.length < 2) // if rowname skip
				continue;
			// <=if last col does not have # it must be wrong.
			// to few dataCols =>
			if (lineCols.length < columnHeadingCount + 1) {
				// System.Xut.println("line cols < FH.. -start:" + lines[i]);
				numberLocs = nlp.getAllMatchedGroups(lineCols[lineCols.length - 1], Pnumber);
				// System.Xut.println("value passed to Pnumber pattern:"
				// + lineCols[lineCols.length - 1]);
				if (numberLocs.size() < 1) {
					// System.Xut.println("no number");
					// no number in last col column then skip
					continue; // can't use if too many
				}

				// this will find instances of \\d\\s\\d
				while (columnHeadingCount + 1 > lineCols.length) {
					// System.Xut.println("too few cols - lines[i]: " +
					// lines[i]);
					idxSpace = lines[i].lastIndexOf(" ");
					// finds lastIndex of space in line which is likely to be at
					// word before end of rowName
					if (idxSpace < 0) {
						// System.out.println("lines[i] lineCols<FH but no space..:" + lines[i]);
						break;
					}
					col1 = lines[i].substring(0, idxSpace).trim(); // bef space
					// System.Xut.println("col1: " + col1);
					col2 = lines[i].substring(idxSpace + 1).trim(); // aft space
					// System.Xut.println("col2: " + col2);
					joinChar = "\t";
					if (!nlp.doesPatternMatch(col2, Pnumber)
					// && !col2.contains("-") && !col2.contains("_")
					) {
						// /lineCols: rowname, 123, 345
						if (!lineCols[0].endsWith("[?]")) {
							lineCols[0] += "[?]";
						}
						// /lineCols: rowname[?], 123, 345
						// lineCols[0]=rowname. This simply add [?] to rowname
						// below adds the new column to adjust to correct col
						// count
						lineCols = (String[]) ArrayUtils.add(lineCols, "?");
						// lineCols: rowname[?], 123, 345, ?:
						// this adds a question mark to the array.
						// rowName[?]<tab>123<tab>345<tab>?

						/*
						 * joinChar = "\t?"; lines[i] = col1 + "[?]" + " " + col2 + joinChar; //lines[i]
						 * = lines[i].split(arg0)
						 */

						// System.Xut.println("line to mark rowname with[?]: "
						// + lines[i]);
					} else {
						lines[i] = col1 + joinChar + col2;
						lineCols = lines[i].split("\t");
					}
					// System.out.println("line to mark tab prior to # in rowname: " + lines[i]);
					// rejoin
				}
				lines[i] = StringUtils.join(lineCols, "\t");
				// System.Xut.println("line cols < FH.. - end" + lines[i]);

			} else if (lineCols.length > columnHeadingCount + 1) {
				// too many tabs. 1st ck if any tabs rowname
				// System.Xut.println("line cols >> FH.. - start: " +
				// lines[i]);
				while (lineCols.length > (columnHeadingCount + 1)) {
					lineCols[0] += " " + lineCols[1];
					lineCols = (String[]) ArrayUtils.remove(lineCols, 1);
				}
				lines[i] = StringUtils.join(lineCols, "\t");
				// System.Xut.println("line cols >> FH.. - end");
			}
		}
		return StringUtils.join(lines, "\r\n");
	}

	public String replaceSubTotals(String tableText, String tableName) throws ParseException {

		NLP nlp = new NLP();

		int maxLinesToHaveSubTotal = 25;

		// takes 1 line each at a time
		String[] lines = tableText.split("\r\n");

		int lineNo = 0, noSubtotalLines = 0, lastRnhLineNo = 0, rnhWords = 0;
		String line = null, rnh = null, srnh = null, colAmtStr;
		String[] lineCols;
		double colTotal = 0, colAmt = 0;
		// if blank row sum is not equal to carried/tolling sum - fails
		// if 10 lines checked and no blank row found - fails.
		// in both instances go back to rnh and move ahead 1 line - and start
		// loop again

		for (; lineNo < lines.length; lineNo++) {
			// for each line continue in for loop. starting with lineNo=0
			line = lines[lineNo];
			// if it starts with ( skop
			if (line.startsWith("(") && line.indexOf("\t") < 0) {
				// -1 is when there is no indexOf
				// System.Xut.println("tab and startsWith ( ");
				continue;
			}

			lineCols = line.split("\t");
			rnhWords = 4;
			if (lineCols.length == 0) {
				// System.out.println("rnh lineCols==0");
				continue;
			}
			if (lineCols[0].toLowerCase().contains("cash") && !line.contains("\t")
					&& tableName.toLowerCase().contains("cash")) {
				rnhWords = 15;
				// System.out.println("rnh words are now 15..");
			}
			// string array of columns
			if ((StringUtils.isBlank(rnh) && lineCols[0].split(" ").length > rnhWords
			// && lineCols[0].split(" ") != null
			)) {
				// System.Xut.println("::A1::");
				continue;

			}

			// if rnh is blank && there are more than 4 words - skip
			if (StringUtils.isBlank(rnh)) { // RNH not yet found..
				// if next 2 lines don't have tab$ pattern unlikely it is
				// RNH, ignore
				String next2Lines = getNextFewLines(lines, lineNo, 2);
				if (!nlp.doesPatternMatch(next2Lines, TabNumberTabPattern))
					// System.Xut
					// .println("!tp.DoesPatternMatch(next2Lines,TableParser.TabNumberTabPattern)");
					continue;
			}
			if (StringUtils.isBlank(srnh) && lineCols.length == 1 && lineNo == (lastRnhLineNo + 1)) {
				// System.Xut.println("lineA:" + line);
				srnh = lineCols[0];
			}
			// we probably don't need if 'isBlank' b/c we reset at each rnh
			// and can't have srnh w/o first rnh

			else if (lineCols.length == 1) {
				// isBlank is null or 'empty'
				lastRnhLineNo = lineNo;
				rnh = lineCols[0];
				// System.Xut.println("rnh found:" + rnh);
				srnh = null;
				colTotal = 0;
				noSubtotalLines = 0;
				// reset if new rnh provided prior if condition is not true (eg,
				// not a srnh)

			}
			// if (TableParser.DoesPatternMatch(line + "\r\n",
			// TableParser.YearPattern))
			// continue;
			if (lineCols.length < 2)
				continue;
			colAmtStr = lineCols[1];
			// System.Xut.println("colAmtStr:" + colAmtStr);
			try {
				colAmt = parseToAmount(colAmtStr);
			} catch (ParseException e1) {
				// System.Xut.println("colVal could not be parsed as amt: "
				// + colAmtStr);
				// System.Xut.println("exception in parsing colAmt:"
				// + e1.getMessage());
				rnh = srnh = null; // reset the rnh, colsum etc
				colTotal = 0;
				noSubtotalLines = 0;
				// this will catch instances of unparsable data col amounts
				continue;
			}
			if (((colTotal == colAmt) & colTotal != 0) || line.startsWith("BLANK ROW")) {
				// this is a place holder put for sub-total rows also..
				// System.Xut
				// .println(" colTotal == colAmt) & colTotal != 0. line: "
				// + line);
				if (colTotal == colAmt) {
					// if sum till now doesn't match the 1st col value
					// System.Xut.println("sub total found:" + rnh);
					// System.Xut.println("line: " + line);
					// #in subtotal row put [rnh:rnhValue]subtotalValue
					if (line.startsWith("BLANK ROW")) {
						lines[lineNo] = lines[lineNo].replace("BLANK ROW",
								"[rnh: " + rnh + "]" + (null != srnh ? "[srnh:" + srnh + "]" : ""));
						// embedded if then conditoin - the if is '?' and the
						// else is ':'. If null then "". Tertiary operator
					}

					else {
						String tmp = "";
						tmp = "[rnh: " + rnh + "] ";
						if (null != srnh)
							tmp += "[srnh:" + srnh + "]";
						lines[lineNo] = tmp + lines[lineNo];
					}
				}
				// System.Xut.println("sub-total found. line=" + lines[lineNo]);
				rnh = srnh = null; // reset the rnh, colsum etc
				colTotal = 0;
				noSubtotalLines = 0;
				continue;
			} else {
				noSubtotalLines++;
				colTotal += colAmt;
				// System.Xut.println("colTotal till now: " + colTotal);
			}
			if (noSubtotalLines > maxLinesToHaveSubTotal) {
				// System.out.println("maxLinesToHaveSubTotal reached. going back...");
				rnh = srnh = null; // reset the rnh, colsum etc
				colTotal = 0;
				noSubtotalLines = 0;
				lineNo = lastRnhLineNo;
				// go back where last RNH was found (+1 will be added in the
				// loop itself)
				break;
			}
		}

		return StringUtils.join(lines, "\r\n");
	}

	private String getNextFewLines(String[] lines, int currentLineNo, int howManyLines) {
		int nextlineNo = currentLineNo + 1;
		if ((nextlineNo + howManyLines) < lines.length)
			return StringUtils.join(ArrayUtils.subarray(lines, nextlineNo, nextlineNo + howManyLines), "\r\n");
		else if (nextlineNo == lines.length)
			return "";
		return StringUtils.join(ArrayUtils.subarray(lines, nextlineNo, lines.length), "\r\n") + "\r\n";
	}

	public static double parseToAmount(String amtStr) throws ParseException {

		// System.Xut.println("amtStr to parse:" + amtStr+ "\r
		// amtStr.length="+amtStr.length());
		double amt = 0.0;
		amtStr = amtStr.replaceAll("[\\$ =]{1,}", "");
		if (amtStr.replaceAll("-", "").length() == 4 && !amtStr.contains(",")
				&& (amtStr.contains("199") || amtStr.contains("200"))) {
			// System.Xut.println("1.returned amt="+amt);
			return amt = 0.0;
		}

		int sign = 1;
		Pattern digit = Pattern.compile("[\\d]");
		Matcher matchDigit = digit.matcher(amtStr);
		if ((amtStr.indexOf("\\-") >= 0 && matchDigit.find()) || amtStr.indexOf("(") >= 0)
			sign = -1;
		amtStr = amtStr.replaceAll("(\\$.\\()", "\\(").trim();
		// $.0.12
		amtStr = amtStr.replaceAll("\\.$", "\\.").trim();
		amtStr = amtStr.replaceAll("[\\€\\£\\$\\(\\),\\%]", "").trim();
		// System.Xut.println("amtStr aft replaceALL:" + amtStr);
		Matcher matchDigit2 = digit.matcher(amtStr);

		if ((amtStr.contains("—") || amtStr.contains("_") || amtStr.contains("-") || amtStr.contains("=")
				|| amtStr.contains(".") || amtStr == "0") && !matchDigit2.find()) {
			amt = 0.0;
			// System.Xut.println("amtStr1:" + amtStr);
			// System.Xut.println("2.returned amt="+amt);
			return amt;
		}

		// System.Xut.println("amtStr.length():" + amtStr.length());
		if (amtStr.length() == 0) {
			amt = 0.0;
			// System.Xut.println("3.returned amt="+amt+" amtStr="+amtStr);
			return amt;
		}

		if (amtStr.contains("?")) {
			amt = 0.0;
			// System.out.println("4a.returned amt=" + amt);
			return amt;
		}

		// System.Xut.println("amtStr to parse to amt:" + amtStr);
		Pattern letter = Pattern.compile("[^\\-0-9\\.]");
		Matcher matchLetter = letter.matcher(amtStr);
		if ((matchLetter.find() && !amtStr.contains("?") && amtStr.trim() != "0") || amtStr.isEmpty()) {
			amt = 0.0;
			// System.Xut.println("4. amtS="+amtStr+" 4.returned amt="+amt);
			return amt;
		}

		// DecimalFormat df = new DecimalFormat("###,###,###,###,###.###");
		// df.setRoundingMode(RoundingMode.UNNECESSARY);
		// System.Xut.println(df.parse(amtStr).doubleValue() * sign);

		// NumberFormatException - create try catch
		try {
			amt = Double.parseDouble(amtStr);
			// System.Xut.println("amt double: " + amt);
			// System.Xut.println("5.returned amt="+amt*sign);
			return amt * sign;
		} catch (NumberFormatException e1) {
			// System.Xut.println("colVal could not be parsed as amt: "
			// + colAmtStr);
			// System.Xut
			// .println("exception in parsing colAmt:" + e1.getMessage());
			amt = 0.0;
		}
		// System.Xut.println("6.returned amt="+amt);
		return amt;

	}

	public static String getAmtStr(String amtStr) throws ParseException {
		return amtStr;
	}

	public static void main(String[] arg) throws IOException, ParseException {

		String text = Utils.readTextFromFile("c:/backtest/regex2.txt");
		String tableText = columnUtility(text, 4);
		// System.out.println("tableText: " + tableText);

	}
}
