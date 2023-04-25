package xbrl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class DividendYield {

	public static List<String[]> listDividendFrequency = new ArrayList<>();
	public static List<String[]> listDividendYoYChange = new ArrayList<>();
	public static List<String[]> listDividendConsecutiveChange = new ArrayList<>();

	public static void expectedDividend() throws IOException, SQLException, ParseException {

		File f = new File("c:/backtest/dividends/listDividendFrequency.csv");
		File file = new File("c:/backtest/dividends/divs.csv");

		if (f.exists())
			f.delete();
		if (file.exists())
			file.delete();

		PrintWriter pw = new PrintWriter(f);

		MysqlConnUtils.executeQuery("select replace(exdivdt,'-',''),@its:=its its  ,dividend \r"
				+ "INTO OUTFILE 'c:/backtest/dividends/divs.csv'\r" + "FIELDS TERMINATED BY ',' \r"
				+ "LINES TERMINATED BY '\n'\r"
				// TODO: get rid of where clause
				+ "FROM stockdividend where dividend>0 \r" + " and ( " + " its='bnd' or " + " its='ge' or" + " its='c' "
				// + "or its='bac' or its='aapl' or its='msft' "
				+ ") and exdivdt>'2001-01-01' order by its,exdivdt desc;\r\r");

		String text = Utils.readTextFromFile(file.getAbsolutePath());
		String[] lines = text.split("\r\n");
		String line = "", line2 = "";
		String its = "", priorIts = "", chgStr = "";

		String sDateStr = "19010101", pDateStr = "19010101";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

		Date sDate = new Date();
		sDate = sdf.parse(sDateStr);

		Date pDate = new Date();
		pDate = sdf.parse(pDateStr);

		double diff = 0.0, chg = 0.00000, div = 0.0000, priorDiv = 0.000;
		List<String[]> listItsDividends = new ArrayList<String[]>();
		for (int i = 0; i < lines.length; i++) {
			line = lines[i];
			String fields[] = line.split(",");
			div = Double.parseDouble(fields[2]);

			if (i + 1 < lines.length) {
				line2 = lines[i + 1];
				priorDiv = Double.parseDouble(line2.split(",")[2]);

			}

			sDateStr = fields[0];
			its = fields[1];
			sDate = sdf.parse(sDateStr);
			// reset firstDate
			if (i == 0 || !priorIts.equals(its)) {
				pDateStr = sDateStr;
				pDate = sdf.parse(pDateStr);
			}

			diff = (pDate.getTime() - sDate.getTime()) / (24 * 60 * 60 * 1000);
			if (diff < 0) {
				diff = 0;
			}

			chg = 0.000;
			chgStr = "0.0";
			// System.out.println("i="+i+" lines.len="+lines.length);

			if (i + 1 < lines.length && (priorIts.equalsIgnoreCase(its) || i == 0) && line2.split(",")[1].equals(its)) {
				chg = Math.abs((div - priorDiv) / priorDiv);
				chgStr = String.format("%.4f", chg);
			}

			// System.out.println("its=" + its + " priorIts=" + priorIts + " pDate=" +
			// pDateStr + " sDate=" + sDateStr
			// + " dif=" + diff + " chgStr=" + chgStr + " div=" + fields[2] + " priorDiv=" +
			// line2.split(",")[2]);

			String[] sAry = { fields[0], fields[1], fields[2], ((int) diff + ""), chgStr };
			pDateStr = sDateStr;
			pDate = sDate;
			// System.out.println("ary=" + Arrays.toString(sAry));
			if (!priorIts.equals(its) && i > 0) {

				// System.out.println("calculateFrequency for priorIts=" + priorIts + " Its=" +
				// its);
				listDividendFrequency = calculateFrequency(listItsDividends);

				for (int c = 0; c < listDividendFrequency.size(); c++) {
					// System.out.println("listDividendFrequency=" +
					// Arrays.toString(listDividendFrequency.get(c)));
					pw.append(Arrays.toString(listDividendFrequency.get(c)).replaceAll("\\[|\\]", "") + "\r");
				}

				pDateStr = "";
				sDateStr = "";
				listItsDividends = new ArrayList<String[]>();
				listDividendFrequency = new ArrayList<>();
			}

			listItsDividends.add(sAry);
			priorIts = its;
			pDate = sDate;
		}

		listDividendFrequency = calculateFrequency(listItsDividends);

		for (int c = 0; c < listDividendFrequency.size(); c++) {
			// System.out.println("listDividendFrequency=" +
			// Arrays.toString(listDividendFrequency.get(c)));
			pw.append(Arrays.toString(listDividendFrequency.get(c)).replaceAll("\\[|\\]", "") + "\r");
		}

		listDividendFrequency = new ArrayList<>();

		pw.close();

	}

	public static List<String[]> calculateFrequency(List<String[]> list) throws ParseException {

		List<String[]> listFrequency = new ArrayList<String[]>();

		// if (list.size() < 4)
		// return list;

		for (int c = 0; c < list.size(); c++) {
			// this loop goes back 12 months and record number of dividends. Then break
			// and go to outer loop to get the next date and repeat this loop
			// System.out.println("c=" + c + " get ary list=" +
			// System.out.println("getStringAryToAddToList=" +
			// Arrays.toString(list.get(c)));
			String[] sAry = getStringAryToAddToList(list, c);
			if (sAry != null) {
				listFrequency.add(sAry);
			}
		}

		return listFrequency;

	}

	public static String[] getStringAryToAddToList(List<String[]> list, int i) throws ParseException {

		// at next ex div dt - checking each ex div dt that comes before it until up to
		// 1 yr and a little has passed - list is in descending order so i=0 is ex div
		// dt to ck for all divs that occur one year prior - e.g, i+1 is next ex div dt
		// before i=0

		String sDateStr = list.get(i)[0], pDateStr = "19010101", trailingOneYearDividendsStr = "", its = "",
				chgStr = "", chgStrOneBehind = "", chgStrTwoBehind = "", daysBetConsecDivs = "", div = "",
				specialDividend = "";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date pDate, sDate = new Date();

		double daysBetweenDividends = 0.0, trailingOneYearDividends = 0.00, sumDivChgsForPast12Mos = 0,
				avgDivChgForPast12Mos = 0, chgOneBehind = 0.0, chgTwoBehind = 0.0;
		int dividendCount = 0, volatilityCnt = 0, numbOfDivsPast12Mos = 0, adj = 1, sIdx = i;
		sDate = sdf.parse(sDateStr);// cur ex div dt

		// String tmpStr = Arrays.toString(list.get(i));
		// System.out.println("tmpStrAry=" + tmpStr);

		for (; i < list.size(); i++) {
			// System.out.println("2getStringAryToAddToList - list.get(i)" +
			// Arrays.toString(list.get(i)));
			if (Double.parseDouble(list.get(i)[4]) > 0.04) {
				volatilityCnt++;
			}

			dividendCount++;
			numbOfDivsPast12Mos = dividendCount;
			pDateStr = list.get(i)[0];
			pDate = sdf.parse(pDateStr);
			daysBetweenDividends = (sDate.getTime() - pDate.getTime()) / (24 * 60 * 60 * 1000);
			trailingOneYearDividends = trailingOneYearDividends + Double.parseDouble(list.get(i)[2]);
			its = list.get(i)[1];
			// +1 b/c i starts at 0 and numberOfDividendsPast12Months starts at 1
			chgStr = list.get(i - numbOfDivsPast12Mos + adj)[4];
			chgStrOneBehind = "0.0";
			chgStrTwoBehind = "0.0";

			if (i + 1 < list.size()) {
				chgStrOneBehind = list.get(i - numbOfDivsPast12Mos + adj + 1)[4];
			}
			if (i + 2 < list.size()) {
				chgStrTwoBehind = list.get(i - numbOfDivsPast12Mos + adj + 2)[4];
			}

			sumDivChgsForPast12Mos = Math.abs(Double.parseDouble(list.get(i)[4])) + sumDivChgsForPast12Mos;
			// sumDivChgsForPast12Mos - add abs val of each div over div chg

			daysBetConsecDivs = list.get(i - numbOfDivsPast12Mos + adj)[3];
			div = list.get(i - numbOfDivsPast12Mos + adj)[2];
			trailingOneYearDividendsStr = String.format("%.4f", trailingOneYearDividends);
			avgDivChgForPast12Mos = (sumDivChgsForPast12Mos) / numbOfDivsPast12Mos;

			String[] sAry = { "1xx" + daysBetweenDividends + "", sDateStr, pDateStr, its, chgStr, daysBetConsecDivs,
					div, trailingOneYearDividendsStr, numbOfDivsPast12Mos + "", volatilityCnt + "",
					String.format("%.4f", avgDivChgForPast12Mos) };
			// sAry =
			// [0]=daysBetweenDividends,[1]=sDateStr,[2]=pDateStr[3]=its,[4]=chgStr,[5]=daysBetConsecDivs,[6]=div,[7]trailingOneYearDividendsStr,
			// [8]=numberOfDividendsPast12Months,[9]volatilityCnt,[10]avgDivChgForPast12Mos
			// System.out.println("default/starting sAry=" + Arrays.toString(sAry));

			if (

			(daysBetweenDividends >= 350 && daysBetweenDividends < 410) ||

					(daysBetweenDividends > 409 && daysBetweenDividends < 480 && dividendCount == 5)

					|| (daysBetweenDividends >= 335 && daysBetweenDividends < 350 && dividendCount == 5)

			) {

				// # of divs is current count of dividends less 1 (if at 350 there are 5 divs -
				// this returns dividend count of 4) and b/c less 1 modify default value

				numbOfDivsPast12Mos = dividendCount - 1;
				volatilityCnt = volatilityCnt - 1;
				trailingOneYearDividends = trailingOneYearDividends - Double.parseDouble(list.get(i)[2]);
				pDateStr = list.get(i - 1)[0];
				chgStr = list.get(i - numbOfDivsPast12Mos)[4];
				if (i + 1 < list.size()) {
					chgStrOneBehind = list.get(i - numbOfDivsPast12Mos + 1)[4];
				}
				if (i + 2 < list.size()) {
					chgStrTwoBehind = list.get(i - numbOfDivsPast12Mos + 2)[4];
				}
				daysBetConsecDivs = list.get(i - numbOfDivsPast12Mos)[3];
				div = list.get(i - numbOfDivsPast12Mos)[2];
				trailingOneYearDividendsStr = String.format("%.4f", trailingOneYearDividends);

				avgDivChgForPast12Mos = (sumDivChgsForPast12Mos - Math.abs(Double.parseDouble(list.get(i)[4])))
						/ numbOfDivsPast12Mos;

				daysBetweenDividends = (sDate.getTime() - sdf.parse(pDateStr).getTime()) / (24 * 60 * 60 * 1000);
				String[] sAry2 = { "2xx" + daysBetweenDividends + "", sDateStr, pDateStr, its, chgStr,
						daysBetConsecDivs, div, trailingOneYearDividendsStr, numbOfDivsPast12Mos + "",
						volatilityCnt + "", String.format("%.4f", avgDivChgForPast12Mos) };

				// System.out.println("1 sAry=" + Arrays.toString(sAry));
				specialDividend = getSpecialDividend(list, sAry2, sIdx);
				return sAry2;

			}

			if (daysBetweenDividends > 225 && daysBetweenDividends < 349 && (dividendCount >= 1 && dividendCount < 5)) {
				// this returns cur count of divs (if at 300 days and there are 2 divs for
				// period - this returns count of 2 divs for 1 yr per) = so use default ary vals
				// System.out.println("3 sAry=" + Arrays.toString(sAry));
				specialDividend = getSpecialDividend(list, sAry, sIdx);
				return sAry;

			}
		}

		// remove==>
		String[] sAry = { "remove3xx" + daysBetweenDividends + "", sDateStr, pDateStr, its, chgStr, daysBetConsecDivs,
				div, trailingOneYearDividendsStr, numbOfDivsPast12Mos + "", volatilityCnt + "",
				String.format("%.4f", avgDivChgForPast12Mos) };
		// specialDividend = getSpecialDividend(list, sAry, i -
		// numberOfDividendsPast12Months);
		// <===remove
		// System.out.println("3 sAry=" + Arrays.toString(sAry));

		return sAry;

	}

	public static String getSpecialDividend(List<String[]> list, String[] ary, int i) {

		// sAry =
		// [0]=daysBetweenDividends,[1]=sDateStr,[2]=pDateStr[3]=its,[4]=chgStr,[5]=daysBetConsecDivs
		// ,[6]=div,[7]trailingOneYearDividendsStr,[8]=numberOfDividendsPast12Months,
		// [9]volatilityCnt, [10]avgDivChgForPast12Mos

		String chgStr = list.get(i)[4], chgStrOneBehind = "0", chgStrTwoBehind = "0", chgOneAheadStr = "0";

		int numberOfDividendsPast12Months = Integer.parseInt(ary[8]), cnt = 0, frequency = Integer.parseInt(ary[8]),
				volatilityCnt = Integer.parseInt(ary[9]);

		double chg = Double.parseDouble(chgStr), chgOneBehind = 0.0, chgTwoBehind = 0.0, chgOneAhead = 0.0,
				avgDivChgForPast12Mos = Double.parseDouble(ary[10]), shavedDiv = 0.0, // amt to remove from spec;
				trailingOneYearDividends = Double.parseDouble(ary[7]),
				numberOfDividendsPast12Month = Double.parseDouble(ary[8]), div = Double.parseDouble(ary[6]),
				avgDiv = trailingOneYearDividends / numberOfDividendsPast12Month;

		/*
		 * ck cur div against prior/bef, avg12mo, etc to see if special
		 */

		// System.out.println("getSpecial - ary=" + Arrays.toString(ary));
		// System.out.println("i=" + i + " list.get(i)=" +
		// Arrays.toString(list.get(i)));
		chgOneAheadStr = "0";
		chgStrOneBehind = "0";
		chgStrTwoBehind = "0";
		shavedDiv = 0.0;

		if (i > 0) {
			chgOneAheadStr = list.get(i - 1)[4];
			chgOneAhead = Double.parseDouble(chgOneAheadStr);
		}
		if (i + 1 < list.size()) {
			chgStrOneBehind = list.get(i + 1)[4];
			chgOneBehind = Double.parseDouble(chgStrOneBehind);
		}
		if (i + 2 < list.size()) {
			chgStrTwoBehind = list.get(i + 2)[4];
			chgTwoBehind = Double.parseDouble(chgStrTwoBehind);
		}

		// formula to guess if it is special.
		if ((i == 0 && chg > avgDivChgForPast12Mos * 2)
				|| (chg > avgDivChgForPast12Mos * 2 && chg > chgOneBehind * 2 && chg > chgTwoBehind * 2)) {
			// shave = excess of special: (1+avgDivChgForPast12Mos)*(totalDiv/fre)

			if (chg > 0) {
				shavedDiv = (1 + avgDivChgForPast12Mos) * avgDiv;
				System.out.println(">0 special Div. i=" + i + " shaveDiv=" + String.format("%.4f", shavedDiv)
						+ " avgDiv=" + String.format("%.4f", avgDiv) + Arrays.toString(ary));
				return String.format("%.4f", shavedDiv);
			}

			if (chg < 0) {
				shavedDiv = (1 - avgDivChgForPast12Mos) * avgDiv;
				System.out.println("<0 special Div. i=" + i + " shaveDiv=" + String.format("%.4f", shavedDiv)
						+ " avgDiv=" + String.format("%.4f", avgDiv) + Arrays.toString(ary));
				return String.format("%.4f", shavedDiv);
			}
		}

		return "not special";

	}

	public static void main(String[] args) throws IOException, SQLException, ParseException {
		// TODO Auto-generated method stub

		// how do I know if I'm dealing with dividends that adjusted in a way
		// that's consistent with my price history when there is a stocksplit
		// for example? Stockdividends are generally not split adjusted. It reflects the
		// amount paid at the time

		// 1. roll thru each 12 mo period based on inner/outer loop to calculate
		// frequency and avg div.
		// 2. then based on any avg div calc - look back over the prior 12 month period
		// and kick out or mark as bonus any extra div that strays from avg div so that
		// I can than calculate exp div v trailing actual

		expectedDividend();

	}

}
