package algo_testers.search_dependencies;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;


public class LCSDiff extends TextDiffHelper {

	// These are "constants" which indicate a direction in the backtracking array.
	private static final int _zero = 0;
	private static final int _up = 1;
	private static final int _left = 2;
	private static final int _equal = 3;
	
	private static Pattern textStartsWithPunct_Regex = Pattern.compile("^[\\p{Punct}}]+");
	private static Pattern textStartsWithWord_Regex = Pattern.compile("^[^\\p{Punct}}]+");
	
	
	// configurations
	private boolean ignoreCase = false;
	private boolean ignorePunctuations = false;
	
	// post a diff, user can ask for LCS text
	private String longestCommonSequence;
	
	
	public LCSDiff() {}
	
	public LCSDiff(boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
	}
	
	public LCSDiff(boolean ignoreCase, boolean ignorePunctuations) {
		this.ignoreCase = ignoreCase;
		this.ignorePunctuations = ignorePunctuations;
	}
	
	
	/**
	 * Returns the 'List<DiffDetail>' with the diffs b/w 2 texts passed.
	 */
	@Override
	public List<DiffDetail> getDiff(String text1, String text2) {
		List<String[]> diffs = lcsDiff_Words(text1, text2);		//(text1.split(" "), text2.split(" "), true);
		List<DiffDetail> resp = convertNativeDiffList(diffs);
		return resp;
	}

	/**
	 * Pass-in a List<String[]> - the list that 'lcsDiff_Words()' returns, and this method will convert that list to standard 'List<DiffDetail>'.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<DiffDetail> convertNativeDiffList(List<? extends Object> diffList) {
		List<String[]> diffs = (List<String[]>) diffList;
		List<DiffDetail> resp = new ArrayList<>();
		for (String[] dif : diffs) {
			resp.add(new DiffDetail(dif[0], dif[2], Integer.parseInt(dif[1]) ));
		}
		return resp;
	}


	
	public int countDiffWords(String text1, String text2) {
		return countDiffWords(lcsDiff_Words(text1, text2));
	}
	
	public int countDiffWords(List<String[]> diff) {
		int dc = 0;
		for (String[] dif : diff) {
			if (! dif[0].equals("="))
				dc += dif[2].split(" ").length;
		}
		return dc;
	}
	

	public List<String[]> lcsDiff_Words(String text1, String text2) {
		return lcsDiff_Words(text1, text2, true);
	}

	public List<String[]> lcsDiff_Words(String text1, String text2, boolean mergeAdjacentDiffs) {
		String[] w1 = splitText(text1);
		String[] w2 = splitText(text2);
		return lcsDiff_Words(w1, w2, mergeAdjacentDiffs);
	}
	
	/**
	 * Returns the diff - [ ["op", "seq#", "words"], .... ]
	 * 	each block consists of:
	 * 		op can be one of:  -, +, =
	 *  	seq# is sequence number that tells the ordering of blocks
	 *  	words:	1 or more words in the block
	 * 	
	 * i.e.	[["=","0","mai execut trust power perform"],["+","5","either"],["=","5","duti"],["-","6","either"],["=","7","directli"]]
	 * 
	 * @param text1
	 * @param text2
	 * @param mergeAdjacentDiffs
	 * @return
	 */
	public List<String[]> lcsDiff_Words(String[] words1, String[] words2, boolean mergeAdjacentDiffs) {
		int n = words1.length;
		int m = words2.length;
		int lcs[][] = new int[n + 1][m + 1];
		int directions[][] = new int[n + 1][m + 1];
		int i, j, k;

		// It is important to use <=, not <. The next two for-loops are initialization
		for (i = 0; i <= n; ++i) {
			lcs[i][0] = 0;
			directions[i][0] = _zero;
		}
		for (j = 0; j <= m; ++j) {
			lcs[0][j] = 0;
			directions[0][j] = _zero;
		}

		int upLcs, leftLcs;
		// This is the main dynamic programming loop that computes the score and backtracking arrays.
		for (i = 1; i <= n; ++i) {
			for (j = 1; j <= m; ++j) {
				if ( doWordsMatch(words1[i - 1], words2[j - 1]) ) {		
					k = lcs[i - 1][j - 1] + 1;
					lcs[i][j] = k;
					directions[i][j] = _equal;
				} else {
					upLcs = lcs[i - 1][j];
					leftLcs = lcs[i][j - 1];
					lcs[i][j] = Math.max(upLcs, leftLcs);
					if (upLcs > leftLcs) {
						//lcs[i][j] = upLcs;
						directions[i][j] = _up;
					} else {
						//lcs[i][j] = leftLcs;
						directions[i][j] = _left;
					}
				}
			}
		}
		
		// lets print the lcs/direction matrixes
		//printMatrix(lcs, directions, w1, w2);
		
		String result = "";
		List<String[]> diff = new ArrayList<>();
		i = words1.length;
		j = words2.length;
		m = directions[i][j];
		
		while (m != _zero) {
			//System.out.println("i="+i+", j="+j+", lcs=" + lcs[i][j] +", w1="+w1[i-1] + ", w2="+w2[j-1]);
			if (m == _equal) {
				i--;
				j--;
				result = words1[i] +" "+ result;
				diff.add(new String[] {"=", i+"", words1[i]});
			} else if (m == _left) {
				j--;
				diff.add(new String[] {"+", j+"", words2[j]});
			} else if (m == _up) {
				i--;
				diff.add(new String[] {"-", i+"", words1[i]});
		    }
			m = directions[i][j];
		}
		
		while (j > 0) {
			j--;
			diff.add(new String[] {"+", j+"", words2[j]});
		}
		while (i > 0) {
			i--;
			diff.add(new String[] {"-", i+"", words1[i]});
		}

		// reverse the diff list
		Collections.reverse(diff);
		
		if (mergeAdjacentDiffs) {
			mergeAdjacentDiffs(diff);
		}
		
		// keep the LCS
		longestCommonSequence = result;
		// return the diff
		return diff;
		
	}

	public String getLongestCommonSequence() {
		return longestCommonSequence;
	}


	
	/*
	public String getLongestCommonSequence_Chars(String a, String b) {
		int n = a.length();
		int m = b.length();
		int S[][] = new int[n + 1][m + 1];
		int R[][] = new int[n + 1][m + 1];
		int ii, jj;

		// It is important to use <=, not <. The next two for-loops are initialization
		for (ii = 0; ii <= n; ++ii) {
			S[ii][0] = 0;
			R[ii][0] = _zero;
		}
		for (jj = 0; jj <= m; ++jj) {
			S[0][jj] = 0;
			R[0][jj] = _zero;
		}

		// This is the main dynamic programming loop that computes the score and
		// backtracking arrays.
		for (ii = 1; ii <= n; ++ii) {
			for (jj = 1; jj <= m; ++jj) {

				if (a.charAt(ii - 1) == b.charAt(jj - 1)) {
					S[ii][jj] = S[ii - 1][jj - 1] + 1;
					R[ii][jj] = _diagonal_up_left;
				}

				else {
					S[ii][jj] = S[ii - 1][jj - 1] + 0;
					R[ii][jj] = _zero;
				}

				if (S[ii - 1][jj] >= S[ii][jj]) {
					S[ii][jj] = S[ii - 1][jj];
					R[ii][jj] = _up;
				}

				if (S[ii][jj - 1] >= S[ii][jj]) {
					S[ii][jj] = S[ii][jj - 1];
					R[ii][jj] = _left;
				}
			}
		}

		// The length of the longest substring is S[n][m]
		ii = n;
		jj = m;
		int pos = S[ii][jj] - 1;
		char lcs[] = new char[pos + 1];

		// Trace the backtracking matrix.
		while (ii > 0 || jj > 0) {
			if (R[ii][jj] == _diagonal_up_left) {
				ii--;
				jj--;
				lcs[pos--] = a.charAt(ii);
			}

			else if (R[ii][jj] == _up) {
				ii--;
			}

			else if (R[ii][jj] == _left) {
				jj--;
			}
		}

		return new String(lcs);
	}
	*/
	
	
	// ******************************************** //
	
	private String[] splitText(String text) {
		String[] w = text.replaceAll("[\\s]{2,}", " ").split("((?<= )|(?= ))");
		Matcher m;
		if (ignorePunctuations) {
			// split word w/ punct into 2+ words - so any extra punct (before/after word) will also come as deleted/inserted/equal in diffs and word possible match
			//String ptrnStr = "^([\\p{Punct}}]+[^\\p{Punct}}]+[\\p{Punct}} ]*)$|^([^\\p{Punct}}]+[\\p{Punct}}]+[[^\\p{Punct}}] ]*)$";		// this works enough well but not for some rare cases ie "claim's." .
			String ptrnStr = "^(.*?[\\p{Punct}}]+.*)$";			// tells if word has ANY punctuation anywhere
			Pattern p = Pattern.compile(ptrnStr);
			List<String> parts;
			for (int i=0; i < w.length; i++) {
				m = p.matcher(w[i]);
				if (m.find()) {
					parts = splitWordAndPunctuation(w[i]);
					// remove the word at 'k' index
					w = ArrayUtils.remove(w, i);
					// insert word's parts
					for (int k = parts.size() - 1; k >= 0; k--) {
						w = ArrayUtils.add(w, i, parts.get(k));
					}
					
					i += parts.size() -1;
				}
			}
		}
		return w;
	}
	
	/**
	 * Splits the given word such as puncts (before/after) and text are separated.
	 * @param word
	 * @return
	 */
	private List<String> splitWordAndPunctuation(String word) {
		List<String> words = new ArrayList<>();
		Matcher m = textStartsWithWord_Regex.matcher(word);
		if (m.find()) {
			// starts with word - we have puncts at the end
			words.add(m.group());
			if (m.end() < word.length())
				words.add(word.substring(m.end()));
		} else {
			// we have puncts at the start
			m = textStartsWithPunct_Regex.matcher(word);
			if (m.find()) {
				words.add(m.group());
				words.addAll(splitWordAndPunctuation(word.substring(m.end())));
			}
		}
		return words;
	}
	
	private boolean doWordsMatch(String word1, String word2) {
		if (ignoreCase)
			return StringUtils.equalsIgnoreCase(word1, word2);
		else
			return StringUtils.equals(word1, word2);
	}
	
	private void mergeAdjacentDiffs(List<String[]> diffs) {
		String[] dif, prvDif;
		for (int i=1; i < diffs.size(); i++) {
			prvDif = diffs.get(i-1);
			dif = diffs.get(i);
			if (! dif[0].equals(prvDif[0]))
				continue;
			// merge the diffs texts
			prvDif[2] += dif[2];
			diffs.remove(i);
			i--;
		}
	}
	
	
	@SuppressWarnings("unused")
	private void printMatrix(int[][] lcs, int[][] directions, String[]w1, String[] w2) {
		System.out.println("");
		System.out.print("\t");
		for (int j=0; j < w2.length; j++)
			System.out.print(w2[j]+"\t");
		System.out.println("");
		for (int i=0; i <= w1.length; i++) {
			//System.out.print(w1[i]+"\t");
			for (int j=0; j <= w2.length; j++) {
				System.out.print(directions[i][j]+"("+lcs[i][j]+")\t");
			}
			System.out.println("");
		}
		System.out.println("");
	}
	
	
	


	// *************************************** //
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void main(String args[]) throws IOException {
		boolean applySynonyms = false;
		File tgtFile = new File("c:/temp/diff_compare.htm");
		
		
		PrintWriter pw = new PrintWriter(tgtFile);
		
		// write style to html file		<style>....
		TextDiffHelper dh = new LCSDiff();
		StringBuilder sb = new StringBuilder();
		sb.append("<style>").append(dh.getCSS()).append("</style>");
		pw.println(sb.toString());
		
		
		List<List<String>> synonyms = null;
		if (applySynonyms) {
			String json = FileSystemUtils.readTextFromFile("c:/temp/synonyms.json");
			synonyms = (List)JSonUtils.json2List(json, List.class);
		}
		
		
		int loop = 1;
		String text1 = "Notwithstanding any other provision of this Indenture, the right of any Holder to receive payment of principal of and interest on the Notes held by such Holder, on or after the respective due dates expressed or provided for in the Notes, or to bring suit for the enforcement of any such payment on or after such respective dates, shall not be impaired or affected without the consent of such Holder.";
		String text2 = "Rights of Holders To Receive Payment. Notwithstanding any other provision of this Indenture (including, without limitation, Section 6.6), the right of any Holder to receive payment of principal of, premium, if any, or interest, including Additional Interest, if any, on the Notes held by such Holder, on or after the respective due dates expressed or provided for in the Notes, or to bring suit for the enforcement of any such payment on or after such respective dates, shall not be impaired or affected without the consent of such Holder.";
		
		text1 = "Notwithstanding any other provision of this Indenture, the right of any Holder of a Note to receive payment of the principal of, premium, if any, or interest on, such Note or to bring suit for the enforcement of any such payment, on or after the due date expressed in the Notes, shall not be impaired or affected without the consent of such Holder.";
		text2 = "Notwithstanding any other provision of this Indenture, the right of any Holder of a Note to receive payment of principal and interest and premium, if any, on the Note, on or after the respective due dates expressed in the Note (including in connection with an offer to purchase), or to bring suit for the enforcement of any such payment on or after such respective dates, shall not be impaired or affected without the consent of such Holder; provided that a Holder shall not have the right to institute any such suit for the enforcement of payment if and to the extent that the institution or prosecution thereof or the entry of judgment therein would, under applicable law, result in the surrender, impairment, waiver or loss of the Lien of this Indenture upon any property subject to such Lien.";
		
		text1 = "The Trustee, may file such proofs of claim, statements of interest, and other papers or documents as may be necessary or advisable.";
		text2 = "Claim. The Trustee may file such proofs of claim and other papers or documents, as may be necessary or advisable in order to have the claims.";
		
		text1 = "Notwithstanding any other provision of this Indenture, the right of any Holder of a Note to receive payment of the principal of, premium, if any, or interest on, such Note or to bring suit for the enforcement of any such payment, on or after the due date expressed in the Notes, shall not be impaired or affected without the consent of such Holder.";
		text2 = "Notwithstanding any other provision of this Indenture, the right of any Noteholder of a Bond to receive payment of principal and interest and premium, if any, on the Note, on or after the respective due dates expressed in the Note (including in connection with an offer to purchase), or to bring suit for the enforcement of any such payment on or after such respective dates, shall not be impaired or affected without the consent of such Holder; provided that a Holder shall not have the right to institute any such suit for the enforcement of payment if and to the extent that the institution or prosecution thereof or the entry of judgment therein would, under applicable law, result in the surrender, impairment, waiver or loss of the Lien of this Indenture upon any property subject to such Lien.";
		

		String t1 = "The shall be under no obligation to any of the rights or powers in it by this  at the request or  of any of the Holders of Securities unless such Holders shall have offered to the Trustee security or indemnity satisfactory to it against the costs, expenses and liabilities which might be incurred by it in compliance with such request or direction.";
		String t2 = "The Indenture at the request, order or direction of any of the Holders pursuant to the provisions of this Indenture, unless such shall have offered (and if requested, provided) to the Trustee indemnity satisfactory to it against the costs, expenses, claims and liabilities that may be incurred therein or thereby";
		
		
		SynonymApplier synApplier = null;
		if (applySynonyms) {
			synApplier = new SynonymApplier(synonyms);
		}
		
		long startMillis = System.currentTimeMillis();
		
		LCSDiff lcsDiff = new LCSDiff(false, true);		//new LCSDiff(true, true);
		List<DiffDetail> diff = null;
		String html;
		sb = new StringBuilder();
		for (int i=0; i < loop; i++) {
			if (applySynonyms) {
				sb.append("<p>").append(t1).append("</p> <p>").append(t2).append("</p>");
				String[] syndTxts = synApplier.applySynonyms(t1, t2);
				t1 = syndTxts[0];
				t2 = syndTxts[1];
			}
			diff = lcsDiff.getDiff(t1, t2);
			if (applySynonyms) {
					diff = synApplier.unApplySynonyms(diff);
			}
			html = lcsDiff.getDiffHtml(diff);
			
			// write both sentences to file
			sb.append("<p>").append(t1).append("</p> <p>").append(t2).append("</p>");
			sb.append("<P>").append(html).append("</P>");
			pw.println(sb.toString());
		}
		pw.close();
		
		
		System.out.println("lcs: " + (System.currentTimeMillis() - startMillis) +"ms, diffCount:"+lcsDiff.getDiffWordsCount(diff));
		System.out.println(JSonUtils.object2JsonString(diff));
		List<String[]> lcsNativeDiff = lcsDiff.lcsDiff_Words(t1.split(" "), t2.split(" "), true);
		System.out.println(JSonUtils.object2JsonString(lcsNativeDiff));
		System.out.println("lcs: native diffCount:"+lcsDiff.countDiffWords(lcsNativeDiff) );
		System.out.println(lcsDiff.getDiffHtml(diff));
		
		
		
		startMillis = System.currentTimeMillis();
		DiffMatchPatch dmp = new DiffMatchPatch();
		diff = null; 
		for (int i=0; i < loop; i++) {
			dmp = new DiffMatchPatch();
			diff = dmp.getDiff(t1, t2);
		}
		System.out.println("dmp: " + (System.currentTimeMillis() - startMillis) +"ms, diffCount:"+dmp.getDiffWordsCount(diff));
		System.out.println(JSonUtils.object2JsonString(diff));
		System.out.println("dmp: native diffCount:"+dmp.countDiffWords(t1, t2) );
		System.out.println("xxxxxxxxxxxx"+dmp.getDiffHtml(diff));
		
	}
	
	
}
