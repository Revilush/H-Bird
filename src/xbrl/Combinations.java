package xbrl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Combinations {

	
/*	public List<String>  getCHbyIndexLocation(Map<Integer, List<String[]>>  mapAllCh) throws SQLException{

		if(mapAllCh.size()<2)
			return null;
		
		System.out.println("mapAllCh=====");
		NLP.printMapIntListOfStringAry(mapAllCh);
		
		Map<Integer, List<String[]>> mapAllChBySize = new TreeMap<Integer, List<String[]>>();
		Map<Integer, String[]> mapChIdx = new TreeMap<Integer, String[]>();
		Map<Integer, String[]> mapChIdx2 = new TreeMap<Integer, String[]>();

		int key=0;
		
		 * reorder the map of lists so largest list will be first followed by
		 * next largest - treeMap orders by key in asc order. if three lists of
		 * 3,5,4 - the keys=-300,-501,-402. smallest val is -501. The remainder
		 * of key/100 is the row#. Below is map of Lists - each list is a row -
		 * each list contains in string ary all matched CHs
		 
		for (int d=0; d<mapAllCh.size(); d++) {
			List<String[]> tmpL = mapAllCh.get(d);
			key = tmpL.size() * -100 + -d;
			for(int a=0; a< tmpL.size(); a++){
				System.out.println("REORDERING THE mapAllCh="+Arrays.toString(tmpL.get(a))+ " key="+key);
			}
			mapAllChBySize.put(key, tmpL);
		}
		
		List<String[]> listLargest = new ArrayList<>();

		// get largest list (row with most CH matches).
		int cnt = 0, cnt2=0;
		for (Map.Entry<Integer, List<String[]>> entry : mapAllChBySize
				.entrySet()) {
			if (cnt == 0) {
				listLargest = entry.getValue();
				break;
			}
		}
		
		if(listLargest.size()>8)
			return null;
					
		System.out.println("largest list from reordered map");
		NLP.printListOfStringArray(listLargest);
		
		List<Double[]> listLargeRowMidpoints = getAllMidpoints(listLargest);
		System.out.println("listLargeRowMidpoints");
		NLP.printListOfDoubleArray(listLargeRowMidpoints);
		
		// <== retrive - Double[]:[0]=startCol#, [1]=endCol# and [2]=midpoint of
		// startCol and endCol. Used to meas dist of midPt of CHs of row being
		// compared
		cnt2=0;
		cnt = 0;
		String row;
		int rowSmNo, rowLNo = 0;
		
		double mpSm,mpD,startColumn,endColumn,dist,prevDist = 100,maxDist = 100,eIdxSm, eIdxLg;
		String colHdgSml, colHdgLg = null; int startColMp, endColMp;
		// Matches largest list mps against each row.

		@SuppressWarnings("unused")
		List<String[]> tmpLarge = new ArrayList<>();
		for (Map.Entry<Integer, List<String[]>> entry : mapAllChBySize
				.entrySet()) {
			int k = entry.getKey();
			tmpLarge = entry.getValue();
			row = (k + "").substring((k + "").length() - 1);
			if (cnt == 0) {
				rowLNo=Integer.parseInt(row);;//get row for largeList
				cnt++;//go to next row. rowL will be in each ary
				continue;
			}
			rowSmNo =Integer.parseInt(row);
			List<String[]> smalllistRow = new ArrayList<>();
			// next largest list/row in mapAllChBysize==>
			smalllistRow = entry.getValue();
			if(smalllistRow.size()>8)
				return null;
			cnt2++;
			// eIdx and colHdgTxt of each CH in row (listRow). Outer loop is map
			// of rows (k, loop abv).
			for (int i = 0; i < smalllistRow.size(); i++) {
				boolean sameColNo = false;
				 System.out.println("next largest row in map k=" + k + " record=" + i
				 + " next small listRow="
				 + Arrays.toString(smalllistRow.get(i)));
				// compare eIdx of each CH in cur row (i) to row with most CHs (c).
				for (int c = 0; c < listLargeRowMidpoints.size(); c++) {
					//listLargestRowMidpoints=[0]startColMp,[1]endColMp,[2]mp
					eIdxSm = Double.parseDouble(smalllistRow.get(i)[0]);//
					colHdgSml = smalllistRow.get(i)[1];
					mpSm = (eIdxSm + eIdxSm - colHdgSml.trim().length()) / 2;
					//mpD is midpoint of midPoints of two cols in large list
					mpD = listLargeRowMidpoints.get(c)[2];
					startColumn = listLargeRowMidpoints.get(c)[0];
					endColumn=listLargeRowMidpoints.get(c)[1];
					System.out.println("startColumn="+startColumn+ " endColumn="+endColumn);
					startColMp = (int) startColumn;
					endColMp = (int) endColumn;
					System.out.println("colHdgSml="+colHdgSml+ " mpSm="+mpSm+" endCol Hdg lg="+listLargest.get(endColMp)[1]);
					System.out.println("mpSm="+mpSm+ " mpD="+mpD);
					dist = Math.abs(mpSm - mpD);
					//get CHs from largest list
					cnt = 0;
					// below loop is to go through span of large CHs the small
					// CH relates to. If 2 or more CHs from large row relate to
					// 1 CH in small row - this loop will then assign small CH
					// to each large CH in loop. If there are two possible
					// matches of large CHs to a small CH - the one that is
					// furtherst will be used as that will produce the largest
					// span and is most likely the match (not 100%). In
					// addition, b/c we stay w/n a span (leave at least as many
					// large CHs as small CHs that remain to be paired this
					// should work the vast majority of the time)
					
					// need to set end col to be no greater than # of small CHs
					// remaining to be paired. Need to set min startColMp to be
					// no smaller than # of small CHs already paired.
					
					System.out.println("cnt2="+cnt2);
					for (int n = startColMp Math.max(cnt2, startColMp); n <= Math.min(
							endColMp,
							(listLargest.size() - (smalllistRow.size() - (i + 1)))); n++)
					// for(int n=startColMp; n<endColMp; n++)
					{
						System.out.println("is endColMp> remaining. endColMp="+
							endColMp+ " largestList.size-remainder of smallList left="+
							(listLargest.size() - (smalllistRow.size() - (i + 1))));
						
						eIdxLg=Double.parseDouble(listLargest.get(n)[0]);
						colHdgLg=listLargest.get(n)[1]; cnt++;
						 System.out.println("cnt2=" + cnt2);
						System.out.println("startColMp=" + startColMp
								+ " endColMp=" + endColMp);
						System.out.println("eIdxLg=" + eIdxLg + " lgCH="
								+ listLargest.get(n)[1] + " eIdxSm=" + eIdxSm
								+ " smCH=" + smalllistRow.get(i)[1]);
						System.out
								.println("colLgNo="
										+ n
										+ " mpD="
										+ mpD
										+ " lgMidPt="
										+ (eIdxLg - (listLargest.get(n)[1]
												.length() / 2)));
						System.out
								.println("colSmNo="
										+ i
										+ " mpSm="
										+ mpSm
										+ " smMidPt="
										+ (eIdxSm - (smalllistRow.get(i)[1]
												.length() / 2)));						 

						// mpLg = (eIdxL + eIdxL - chL.trim().length()) / 2;
						// primary key=rowS,sColNo,lColNo1,lColNo2
						if(dist<3 && !sameColNo){
							int colSmNo=(i+1);
							int colLgNo=(n+1);
							//ensures smallest distance is inputted first.
							String keyStr = colLgNo+""+rowSmNo+"";
							// primary key is ultimatelhy col#,row#. only
							// relevant col# is lg row (most cols) as each row
							// with less is assigned to each lg col#. if lg
							// col#=4 and row=2 pkey=24. But b/c we only want
							// the pair that has smallest dist (there will be
							// multiple matches for lg col#4 row2 for example)-
							// we create a temp pkey that utilizes dist of sm
							// col eIdx and lg col eIdx. The larger the dist the
							// bigger the key - which I then take a negative of
							// to make it the smallest
							int ky = (int) ((Integer.parseInt(keyStr)) * 100 + Math
									.abs(eIdxLg - eIdxSm));
							String[] ary = { dist + "", eIdxLg + "", eIdxSm + "",
									rowSmNo + "", colHdgSml, colSmNo + "", rowLNo + "", colHdgLg,
									colLgNo + "", (startColMp + 1) + "",
									(endColMp + 1) + "", cnt + "",
									listLargest.size() + "" };
							
							mapChIdx.put(-ky, ary);
							// if colLgNo==colSmNo = then there's a CH in each
							// Col on both rows (sm/lg)
							if(Math.abs(mpSm-mpD)<3 && colLgNo==colSmNo && i<(smalllistRow.size()-1))
								sameColNo=true;

							System.out.println("dist=" + dist +" pkey="+-ky+ " eIdxL="
									+ eIdxLg + " eIdxSm=" + eIdxSm
									+ " eIdxLg-eIdxSm="
									+ Math.abs(eIdxLg - eIdxSm) + " rowSnumb="
									+ rowSmNo + " colHdgSm=" + colHdgSml
									+ " colSmNo=" + colSmNo + " rowLno="
									+ rowLNo + " colHdgLg=" + colHdgLg
									+ " colLgNo=" + colLgNo + " startColMp="
									+ (startColMp +1) + " endColMp="
									+ (endColMp +1) + " cnt=" + cnt
									+ " #OfCols=" + listLargest.size());
							
							// finds greatest distance between mp of CHs across
							// rows in same col
							if(prevDist==100){
								maxDist=dist;
								//get maxDist - and add to col string
							}
							else if(dist>maxDist){
								maxDist=dist;
							}
							prevDist = dist;
						}
					}
				}
				System.out.println("maxDist="+maxDist);
			}
		}
		
		// abs(t1.eidx_l-t1.eidx_s) dif,@pk:=concat(col_l,row_s)
		// pk2,@pk*100+abs(t1.eidx_l-t1.eidx_s) pk1
		for (Map.Entry<Integer, String[]> entry : mapChIdx.entrySet()){
			String[] allCHs = entry.getValue();
			int ky = Integer.parseInt((allCHs[8] + allCHs[3]));
			System.out.println("allChs by Idx ky="+ky+" "+Arrays.toString(allCHs));
			mapChIdx2.put(ky, allCHs);
	}
		for (Map.Entry<Integer, String[]> entry : mapChIdx2.entrySet()){
			System.out.println("FINAL ky="+entry.getKey()+" val"+Arrays.toString(entry.getValue()));
		}
		
		Map<Integer, String[]> mapByIdxRowColNoCH = new TreeMap<Integer, String[]>();
		String [] ary, prevAry;
		String k, prevK = "", chSm,chLg,colHdg;
		int rowSm, rowLg, chId, colSm,colNo,ky; cnt=0;
		for (Map.Entry<Integer, String[]> entry : mapChIdx2.entrySet()) {
			// 0=dist|1=eIdxL|2=eIdxS|3=rowS|4=chSm|5=colS|6=rowL|7=chLg|8=colL
			// |9=(col1 + 1)|10=(col2 + 1)|11=cnt|11=listLargest.size()
		ary = entry.getValue();
		System.out.println("mapByIdxRowColNoCH="+Arrays.toString(ary));
		chId= Integer.parseInt((entry.getKey()+"").substring(0, 1));
		k = (entry.getKey()+"").substring(0,1);

		if (!k.equals(prevK)) {
			rowSm = Integer.parseInt(ary[3])+1;
			chSm = ary[4];
			colNo = Integer.parseInt(ary[8]);
			ky = Integer.parseInt( colNo+ "" +rowSm);
			String [] arry = {colNo+"",chSm};
			System.out.println("ky="+ky+"rowSm=" + rowSm + " colSm=" + colNo
					+ " chSm=" + chSm);
			mapByIdxRowColNoCH.put(ky, arry);
			
			rowLg = Integer.parseInt(ary[6])+1;
			chLg = ary[7];
			colNo = Integer.parseInt(ary[8]);
			ky = Integer.parseInt( colNo+ "" +rowLg);
			System.out.println("ky="+ky+"rowLg=" + rowLg + " colLg=" + colNo
					+ " chLg=" + chLg);
			String [] arryLg = {colNo+"",chLg};
			mapByIdxRowColNoCH.put(ky, arryLg);

		}
		
		else if (k.equals(prevK)) {
				rowLg = Integer.parseInt(ary[6])+1;
				chLg = ary[7];
				colNo = Integer.parseInt(ary[8]);
				ky = Integer.parseInt( colNo+ "" +rowLg);
				System.out.println("ky="+ky+"rowLg=" + rowLg + " colLg=" + colNo
						+ " chLg=" + chLg);
				String [] arry = {colNo+"",chLg};
				mapByIdxRowColNoCH.put(ky, arry);

				rowSm = Integer.parseInt(ary[3])+1;
				chSm = ary[4];
				ky = Integer.parseInt( colNo+ "" +rowSm);
				System.out.println("ky="+ky+"rowSm="+rowSm+" colSm="+colNo +" chSm="+chSm);
				String [] arry2 = {colNo+"",chSm};
				mapByIdxRowColNoCH.put(ky, arry2);
				continue;
			} 
		
		prevK=k;
		}

		int colNum, priorColNum=-1;
		String colHeading = null;
		List<String> listColHandDistances = new ArrayList<>();
		int stop = mapByIdxRowColNoCH.size(); cnt=0;
		for (Map.Entry<Integer, String[]> entry : mapByIdxRowColNoCH.entrySet()) {
			cnt++;
			System.out.println("column hdg="+Arrays.toString(entry.getValue()));
			colNum= Integer.parseInt(entry.getValue()[0]);
			
			if(priorColNum!=colNum){
				
				listColHandDistances.add(colHeading);
				colHeading = entry.getValue()[1].trim();
			} else if(cnt==stop){
				colHeading= colHeading.trim()+" " +entry.getValue()[1].trim();
				listColHandDistances.add(colHeading);
			}
			
			else if (priorColNum==colNum){
				colHeading= colHeading.trim()+" " +entry.getValue()[1].trim();
			}
			priorColNum=colNum;
			System.out.println(" get key=="+entry.getKey()+ " get val="+Arrays.toString(entry.getValue()));
		}
		
		for (int i=0; i<listColHandDistances.size(); i++){
			if(null!=listColHandDistances.get(i) && listColHandDistances.get(i).length()>0)
			System.out.println("List of string of final CHs formulated by idx loc="+listColHandDistances.get(i));
		}
		listColHandDistances.add(maxDist+"");
		return listColHandDistances;
	}*/

	//if I have CH row w/ most CHs - then CHs on other rows will only need their individual mid-point measured.
	
	public List<Double[]> getAllMidpoints (List<String[]> list){
		//for 4 cols: all possible midpoints are:
		//1,12,13,14
		//2,23,24
		//3,34
		//4
		List<Double[]> listMidpoints = new ArrayList<>();
		double mpC, mpEndCol, eIdxStartCol,eIdxEndCol,mp;
		String colHdgTxt, colTextendCol;
		for (int startCol = 0; startCol < list.size(); startCol++) {
			eIdxStartCol = Double.parseDouble(list.get(startCol)[0]);
			colHdgTxt = list.get(startCol)[1];
			mpC = (eIdxStartCol+eIdxStartCol-colHdgTxt.trim().length())/2;
			for(int endCol=startCol; endCol<list.size();endCol++){
				eIdxEndCol = Double.parseDouble(list.get(endCol)[0]);
				colTextendCol = list.get(endCol)[1];
				mpEndCol = (eIdxEndCol+eIdxEndCol-colTextendCol.trim().length())/2;
				mp = (mpEndCol*.5+mpC*.5);
				// System.out.println("startCol="+startCol+" colHdgTxt="+colHdgTxt+" endCol="+endCol+ " mp="+mp);
				//ch1,ch2,midpoint
				Double [] intAry = {(double) startCol,(double) endCol,mp};
				listMidpoints.add(intAry);
			}
		}
		return listMidpoints;
	}

	public List<String> getColumnsToMeasureMidPointsFor(String str) {
		List<String> list = new ArrayList<String>();
		for (int c = 1; c <= str.length(); c++) {
//			list.add("" + c);
			if (c < str.length()) {
				for(int n=c; n<=str.length(); n++) {
					if(n!=c){
//					System.out.println("B "+c + "+" + n);
//					list.add("" + i + "" + n);
					}
				}
			}
		}
//		 System.out.println("spans="+list.toString());
		return list;
	}

	public List<Object[]> getMidpointsOfBws(List<Object[]> listOfObjectArray,
			double swMidpoint) {

		List<Object[]> bwsAndDistance = new ArrayList<Object[]>();
		double mpOfBws = 0, distanceFromSw;
		String bwsInSpan = null;
		StringBuffer sb = new StringBuffer();
		for (int q = 0; q < listOfObjectArray.size(); q++) {
			sb.append("" + (q + 1));
		}

		String bwCols = sb.toString();
//		System.out.println("bwCols are=" + bwCols);

		List<String> listOfColumnsToMeasureMidPoints = new ArrayList<String>();
		listOfColumnsToMeasureMidPoints = getColumnsToMeasureMidPointsFor(bwCols);

		for (int a = 0; a < listOfColumnsToMeasureMidPoints.size(); a++) {
			String ofA = listOfColumnsToMeasureMidPoints.get(a);
			int spanStart = Integer.parseInt(ofA.substring(0, 1));
			int spanEnd = 0;
			if (ofA.length() == 1) {
				bwsInSpan = spanStart + "";
				mpOfBws = ((double) listOfObjectArray.get(spanStart - 1)[0]);
				distanceFromSw = Math.abs(swMidpoint - mpOfBws);
				Object[] midDist = { bwsInSpan, distanceFromSw };
				bwsAndDistance.add(midDist);
//				System.out.println(" 1 bwsInSpan=" + bwsInSpan + "|| 1 mpOfBws="
//						+ mpOfBws + "|| 1 swMidpoint=" + swMidpoint
//						+ "|| 1 distanceFromSw=" + distanceFromSw);
			} else {
				spanEnd = Integer.parseInt(ofA.substring(1));
				if(spanEnd>listOfObjectArray.size()){
//					bwsAndDistance
					break;
				}
					
				bwsInSpan = spanStart + "" + spanEnd;
				
//				System.out.println("listOfObjectArray.Size"
//						+ listOfObjectArray.size() + "spanStart=" + spanStart
//						+ "spanEnd=" + spanEnd);
				
				mpOfBws = ((double) listOfObjectArray.get(spanStart - 1)[0] + (double) listOfObjectArray
						.get(spanEnd - 1)[0]) / 2;
				distanceFromSw = Math.abs(swMidpoint - mpOfBws);
				Object[] midDist = { bwsInSpan, distanceFromSw };
				bwsAndDistance.add(midDist);
			}
		}

		return bwsAndDistance;
	}

	public static void main(String args[]) throws IOException, SQLException {
	}
}
