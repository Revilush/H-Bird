package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import contracts.PatternsDif;
import xbrl.MysqlConnUtils;
import xbrl.NLP;
import xbrl.Utils;

public class TermFinder {

	public static Pattern patternEndOfSentenceOrClause = Pattern
			.compile("((?<=[A-Za-z\\)]{2}|\\]|\\d)(\\.|;)(?![ \r\n]{1,4}provided))");

	public static List<int[]> findTwoPatternsCloseToEachOther(Pattern pattern1,
			Pattern pattern2, String text, int distance,
			boolean secondPatternAfter) {
		// takes 2 patters and sees if they are within the distance specified
		// from each other based on each start location. If secondWord
		NLP nlp = new NLP();
		List<int[]> listTwoPatterns = new ArrayList<>();
		List<Integer> listPattern1 = nlp.getAllIndexStartLocations(text,
				pattern1);
		List<Integer> listPattern2 = nlp.getAllIndexStartLocations(text,
				pattern2);
		// NLP.pwNLP.append(NLP.println("listPattern1.size=" ,""+
		// listPattern1.size()));
		// NLP.pwNLP.append(NLP.println("listPattern2.size=" ,""+
		// listPattern2.size()));
		if (listPattern1.size() < 1 || listPattern2.size() < 1)
			return null;
		int p1 = 0, p2 = 0;
		for (int i = 0; i < listPattern1.size(); i++) {
			p1 = listPattern1.get(i);
			// NLP.pwNLP.append(NLP.println("list p1=" ,""+ p1));
		}
		for (int i = 0; i < listPattern2.size(); i++) {
			p2 = listPattern2.get(i);
			// NLP.pwNLP.append(NLP.println("list p2=" ,""+ p2));
		}
		p2 = 0;
		p1 = 0;
		for (int i = 0; i < listPattern1.size(); i++) {
			p1 = listPattern1.get(i);
			// NLP.pwNLP.append(NLP.println("p1=" ,""+ p1));
			for (int n = 0; n < listPattern2.size(); n++) {
				p2 = listPattern2.get(n);
				// NLP.pwNLP.append(NLP.println("p2=" ,""+ p2));
				if (secondPatternAfter && (p2 < p1))
					continue;
				if (secondPatternAfter && p2 - p1 > distance)
					break;
				if (!secondPatternAfter && p2 > p1)
					continue;
				if (!secondPatternAfter && p1 - p2 > distance)
					break;
				// NLP.pwNLP.append(NLP.println("adding p1=" ,""+ p1 + " p2="
				// + (p2 - distance)));
				int[] p1Ary = { 1, p1 };
				int[] p2Ary = { 2, p2 - distance };
				listTwoPatterns.add(p1Ary);
				listTwoPatterns.add(p2Ary);
			}
		}
		return listTwoPatterns;
	}

	public static String replaceUsingTwoStringArrays(String[] wordsToReplace,
			String[] replacedWith, String text) {
		for (int i = 0; i < wordsToReplace.length; i++) {
			text = text.replaceAll("(?i)" + wordsToReplace[i], replacedWith[i]);
		}
		return text;
	}

	public static void getStrippedKs(String folderStrippedKs)
			throws SQLException, FileNotFoundException {

		NLP nlp = new NLP();
		File files = new File(folderStrippedKs);
		File[] listOfFiles = files.listFiles();
		String kId = "", contractName = "";
		StringBuffer sb = new StringBuffer();
		for (File file : listOfFiles) {
			if (!file.isFile())
				NLP.pwNLP.append(NLP.println("!file=",
						"" + file.getAbsolutePath()));

			kId = file.getName().substring(0, 24);
			kId = kId.substring(0, kId.lastIndexOf("_"));
			contractName = file.getName().replaceAll(kId + "[_]+|.txt",
					"");
			if (kId.length() > 22
					&& kId.substring(22, 23).equals("_")) {
				kId = kId.substring(0, 22);
			}
			if (file.isFile()) {
				sb.append(kId + "||" + contractName + "\r\n");
			}
		}

		String filename = "c:/getContracts/marked/strippedKs.txt";
		File file = new File(filename);
		if (file.exists())
			file.delete();
		PrintWriter pw = new PrintWriter(file);
		// System.out.println("b sb.toString=" + sb.toString());
		pw.append(sb.toString());
		pw.close();
		// System.out.println("filename=" + filename);
		String query = "LOAD Data INFILE '" + filename
				+ "' ignore INTO TABLE nlp_strippedKs"
				+ " FIELDS TERMINATED BY '||' lines terminated by '\\n';";
		MysqlConnUtils.executeQuery(query);

	}

	public static void getShingles(String folderStoppedAndStemmed,
			String termType, List<String[]> listShingles,
			int distanceBetwShingles, List<String[]> listShinglesNotPermitted,
			int distanceBetweenShinglesNotPermitted, boolean sameSentence)
			throws IOException, SQLException {

		// each [] shingles has at least two shingles - which must be a certain
		// distance from each other and/or in same sentence. I haven't created
		// method to only require certain distance from each other w/o also
		// being a requirement to be in same sentence.

		// TODO: will need to see how to loop through 3 or 4 shingles

		NLP nlp = new NLP();
		File files = new File(folderStoppedAndStemmed);
		File[] listOfFiles = files.listFiles();
		// create contract Id here - insert id (cnt) and accno_id.
		String contractText = "";
		@SuppressWarnings("unused")
		int sIdx, cnt = 0;
		String kId = "";
		StringBuffer sb = new StringBuffer();
		for (File file : listOfFiles) {
			if (!file.isFile())

				NLP.pwNLP.append(NLP.println("!file=",
						"" + file.getAbsolutePath()));

			// NLP.pwNLP.append(NLP.println("filename=", file.getName()));
			kId = file.getName().substring(0, 24);
			kId = kId.substring(0, kId.lastIndexOf("_"));
			contractText = Utils.readTextFromFile(file.getAbsolutePath());
			NLP.pwNLP.append(NLP.println("contractText.len=",
					"" + contractText.length()));

			if (file.isFile()) {

				if (sameSentence) {
					String sentence = "";
					cnt = 0;
					List<String[]> listSentence = nlp
							.getAllStartIdxLocsAndMatchedGroups(
									contractText,
									Pattern.compile("(?sm)[\\d]{1,7}\\|\\|.*?(?=[\\d]{1,7}\\|\\|)"));
					NLP.pwNLP.append(NLP.println("listSentence.size=", ""
							+ listSentence.size()));
					// TODO: to do more than 2 shingles - I could loop through
					// shingle[] - starting with 1 and 2 then 2 and 3 and so on.
					for (int i = 0; i < listSentence.size(); i++) {
						sentence = listSentence.get(i)[1];
						// this will take the sentence and see if each shingle
						// is present and if it meets the idx distance
						// requirement (not number of words apart).
						for (int n = 0; n < listShingles.size(); n++) {

							sb.append(getSentenceTerm(sentence, kId,
									termType, listShingles.get(n),
									distanceBetwShingles,
									listShinglesNotPermitted,
									distanceBetweenShinglesNotPermitted));
						}

					}
				}
			}
		}

		String filename = "c:/getContracts/marked/markedSenteces.txt";
		PrintWriter pw = new PrintWriter(new File(filename));
		// System.out.println("a sb.toString=" + sb.toString());
		pw.append(sb.toString());
		pw.close();
		// System.out.println("filename=" + filename);
		String query = "LOAD Data INFILE '" + filename
				+ "' ignore INTO TABLE nlp_terms"
				+ " FIELDS TERMINATED BY '||' lines terminated by '\\n';";
		MysqlConnUtils.executeQuery(query);

	}

	public static String getSentenceTerm(String sentence, String kId,
			String termType, String[] shingles, int distanceBetwShingles,
			List<String[]> listShinglesNotPermitted,
			int distanceBetwShinglesNotPermitted) {

		NLP nlp = new NLP();
		int idxFirst, idxSecond, sentNo;
		String sentenceTerm = "";

		// TODO: ADD LOOP HERE - OF List<String[]>Permitted and require both to
		// be found if 2 or more. Loop would break where both not found - then
		// loop through List<String[]>NotPermitted if sentenceTerm is return and
		// exclude if fails on any of list str ary.

		List<Integer> listFirstShingle = nlp.getAllIndexStartLocations(
				sentence, Pattern.compile(shingles[0]));
		List<Integer> listSecondShingle = nlp.getAllIndexStartLocations(
				sentence, Pattern.compile(shingles[1]));

		NLP.pwNLP.append(NLP.println(
				"must be at least 1 first & second shingle", ""));
		NLP.pwNLP.append(NLP.println("listFirstShingle.size=", ""
				+ listFirstShingle.size()));
		NLP.pwNLP.append(NLP.println("listSecondShingle.size=", ""
				+ listSecondShingle.size()));

		boolean isSentenceTerm = true;
		if (listFirstShingle.size() > 0 && listSecondShingle.size() > 0) {
			NLP.pwNLP.append(NLP.println("sentence=", sentence));

			if (isSentenceTerm && listFirstShingle.size() > 0
					&& listSecondShingle.size() > 0) {
				{

					for (int n = 0; n < listFirstShingle.size(); n++) {

						idxFirst = listFirstShingle.get(n);

						for (int c = 0; c < listSecondShingle.size(); c++) {

							idxSecond = listSecondShingle.get(c);
							if (Math.abs(idxSecond - idxFirst) < distanceBetwShingles) {

								sentNo = Integer.parseInt(sentence.substring(0,
										sentence.indexOf("|")));

								// kId,sentNo,termType,stemmedSent
								sentenceTerm = kId
										+ "||"
										+ sentNo
										+ "||"
										+ termType
										+ "||"
										+ sentence
												.substring(
														sentence.indexOf("|") + 2)
												.replaceAll("[\r\n]+", " ")
												.replaceAll("[ ]+", " ").trim()
										+ "||";
								// checks list of shingles and if any return
								// false (not permitted language is present) it
								// will break and return blank sentence term
								for (int a = 0; a < listShinglesNotPermitted
										.size(); a++) {
									isSentenceTerm = excludeFalsePositives(
											sentenceTerm,
											listShinglesNotPermitted.get(a),
											distanceBetwShinglesNotPermitted);
									if (!isSentenceTerm) {
										// System.out.println("not a sentenceTerm="+sentenceTerm);
										sentenceTerm = "";
										break;
									}
								}
							}
						}
					}
				}
			}
		}

		if (sentenceTerm.length() > 1) {
			System.out.println("         found!! sentenceTerm=" + sentenceTerm);
		}
		return sentenceTerm;

	}

	public static boolean excludeFalsePositives(String sentenceTerm,
			String[] shinglesNotPermitted, int distanceBetwShinglesNotPermitted) {

		NLP nlp = new NLP();
		int idxFirst, idxSecond, sentNo;
		List<Integer> listFirstShingleNotPermitted = nlp
				.getAllIndexStartLocations(sentenceTerm,
						Pattern.compile(shinglesNotPermitted[0]));
		List<Integer> listSecondShingleNotPermitted = nlp
				.getAllIndexStartLocations(sentenceTerm,
						Pattern.compile(shinglesNotPermitted[1]));
		// System.out.println("shingle not permitted #1="+shinglesNotPermitted[0]+"\r#2="+shinglesNotPermitted[1]);
		NLP.pwNLP.append(NLP.println(
				"must be at least 1 first & second shingle", ""));
		NLP.pwNLP.append(NLP.println("listFirstShingleNotPermitted.size=", ""
				+ listFirstShingleNotPermitted.size()));
		NLP.pwNLP.append(NLP.println("listSecondShingleNotPermitted.size=", ""
				+ listSecondShingleNotPermitted.size()));

		if (listFirstShingleNotPermitted.size() > 0
				&& listSecondShingleNotPermitted.size() > 0) {
			NLP.pwNLP.append(NLP.println("sentenceTerm=", sentenceTerm));

			if (listFirstShingleNotPermitted.size() > 0
					&& listSecondShingleNotPermitted.size() > 0) {
				{

					for (int n = 0; n < listFirstShingleNotPermitted.size(); n++) {

						idxFirst = listFirstShingleNotPermitted.get(n);

						for (int c = 0; c < listSecondShingleNotPermitted
								.size(); c++) {

							idxSecond = listSecondShingleNotPermitted.get(c);
							if (Math.abs(idxSecond - idxFirst) < distanceBetwShinglesNotPermitted) {
								System.out.println("has shinglesNotPermitted="
										+ sentenceTerm);
								return false;
							}
						}
					}
				}
			}
		}

		return true;
	}

	public static int getEndOfContract(String fullContract) {

		NLP nlp = new NLP();
		String contractWithoutExhibits = "";
		int firstPartofFullContractToSkip = (int) (fullContract.length() * .05);
		System.out.println("firstPartofFullContractToSkip="
				+ firstPartofFullContractToSkip);

		List<Integer> listContractClosingParagraph = nlp
				.getAllIndexEndLocations(
						fullContract.substring(firstPartofFullContractToSkip),
						contracts.PatternsDif.patternEndOfContract);
		System.out.println("listContractClosingParagraph.size="
				+ +listContractClosingParagraph.size());
		// NLP.pwNLP.append(NLP.println("listContractClosingParagraph.size=", ""
		// + listContractClosingParagraph.size()));

		// require end of contract to be at least 65% of full contract length
		double ratioK = 0.0, kLen =0.0, endLoc = 0.0, ratioToBeat = 0.5;
		for (int i = 0; i < listContractClosingParagraph.size(); i++) {
			endLoc = (firstPartofFullContractToSkip + listContractClosingParagraph.get(i));
			kLen = fullContract.length();
			ratioK = endLoc/kLen;
			
			System.out
			.println("listContractClosingParagragh.get(i)="
					+ +(firstPartofFullContractToSkip + listContractClosingParagraph
							.get(i)) + " contract.len="
					+ fullContract.length()+" ratioK="+ratioK);
			

			if ( ratioK > ratioToBeat) {

				// System.out
				// .println("fullContractLoc of exhibit="
				// + fullContract
				// .substring(
				// (firstPartofFullContractToSkip + listContractClosingParagraph
				// .get(i)),
				// (firstPartofFullContractToSkip
				// + listContractClosingParagraph
				//								.get(i) + 20)));

				// System.out
				// .println("\rit is greater than 60%. fullContract.len="
				// + fullContract.length()
				// + " contractWithoutExhibits.len="
				// + contractWithoutExhibits.length());
				return (int) endLoc;
			}
		}
		
		System.out.println("returning fullContract - no exhibits.");
		return (int) kLen;
	}

	/*
	public static void getSampleContractsReady(File files,
			String folderToSaveStrippedFiles, Pattern patternContractNameFilter)
			throws IOException {
		// reads html files from folder specified in files, removes html code
		// and removes all exhibits then saves it to unstripped folder.
		NLP nlp = new NLP();
		String contract = "";
		File[] listOfFiles = files.listFiles();
		for (File file : listOfFiles) {
			if (file.isFile()) {
				System.out.println("getting contracts ready - file="
						+ file.getAbsolutePath());

				if (nlp.getAllIndexEndLocations(file.getAbsolutePath(),
						patternContractNameFilter).size() > 0) {
					file.delete();
					continue;
				}
				contract = Utils.readTextFromFile(file.getAbsolutePath());
				if (contract.contains(".pdf") || contract.contains(".PDF")) {
					file.delete();
					continue;
				}
				// File file2 = new File(fileName.replaceAll(".htm", ".txt"));
				// file.renameTo(file2);
				PrintWriter pw = new PrintWriter(folderToSaveStrippedFiles
						+ file.getName());
				contract = getContractEnd(contract);
				contract = ContractParser.stripHtmlTags(contract);
				pw.println(contract);
				pw.close();
			}
		}
	}
*/
	public static String[] sentenceAndWordTokenizer(File file)
			throws SQLException, IOException {

		NLP nlp = new NLP();

		String contractName = file.getAbsolutePath().substring(
				file.getAbsolutePath().lastIndexOf("\\") + 1);

		// System.out.println("filename" + file.getAbsolutePath()
		// + " contractName=" + contractName);

		String kId = file.getName().substring(0, 24);
		kId = kId.substring(0, kId.lastIndexOf("_"));
		contractName = file.getName().replaceAll(kId + "[_]+|.txt", "");

		System.out.println("kId=" + kId);
		// System.out.println("contractName=" + contractName);

		String text = Utils.readTextFromFile(file.getAbsolutePath());
		List<Integer> listSentenceEnd = nlp.getAllIndexEndLocations(text,
				NLP.patternSentenceEnd);

		String sentence = "", gap = "", word;
		int sIdx = 0, eIdx = 0, cnt = 0;

		// System.out.println("listSentenceEnd.size=" + listSentenceEnd.size());
		StringBuffer sb2 = new StringBuffer();
		StringBuffer sb = new StringBuffer();
		for (int i = 1; i < listSentenceEnd.size(); i++) {
			sIdx = listSentenceEnd.get(i - 1);
			// System.out.println("sidx=" + sIdx + " eIdx=" + eIdx);
			if (Math.abs(sIdx - eIdx) > 50
					|| (Math.abs(sIdx - eIdx) <= 50 && sIdx != eIdx && text
							.substring(eIdx, sIdx)
							.replaceAll("[\r\n\t\\s]{2}", "").length() > 1)) {

				gap = text.substring(eIdx, sIdx).replaceAll("[\r\n]", " ")
						.replaceAll("[ ]+", " ");
				cnt++;
				sb.append(kId + "||" + cnt + "||" + gap + "||\r\n");
				// remove stop words and stem words prior to tokenizing by word
				List<String> listWord = nlp.getAllMatchedGroups(
						Stemmer.stemmedOutPut(StopWords.removeStopWords(gap),true),
						NLP.patternWord);
				for (int n = 0; n < listWord.size(); n++) {
					word = listWord.get(n);
					sb2.append(kId + "||" + cnt + "||" + n + "||" + word
							+ "||\r\n");
				}
			}

			eIdx = listSentenceEnd.get(i);
			sentence = text.substring(sIdx, eIdx).replaceAll("[\r\n]", " ")
					.replaceAll("[ ]+", " ");
			cnt++;
			sb.append(kId + "||" + cnt + "||" + sentence + "||\r\n");

			List<String> listWord = nlp.getAllMatchedGroups(
					Stemmer.stemmedOutPut(StopWords.removeStopWords(sentence),true),
					NLP.patternWord);
			for (int n = 0; n < listWord.size(); n++) {
				word = listWord.get(n);
				sb2.append(kId + "||" + cnt + "||" + n + "||" + word
						+ "||\r\n");
			}
		}

		String[] sAry = { sb.toString(), sb2.toString() };
		return sAry;

	}

	public static void main(String[] args) throws IOException, SQLException {

		// ContractParser(file) is largley in good working order. It will mark
		// toc, toc sections, contract sections, definitions. I should also mark
		// start of each contract and end of each contract as distinct from
		// exhibits. End result is to have database of contracts that can also
		// list related exhibits and call just a particular exhibit as needed.
		// In addition, ability to call definedTerms and with lesser accuracy
		// sections!

		// Steps 1 gto 3 below are for purposes of testing - building shingles
		// that work.

		// TODO: Need to see how to automate creation of shingles.

		// TODO: Need to build into method the ability to run more than 2
		// shingles whereby 1st 2 must be present then find 3rd in relation to
		// prior 2. If same sentence it is just apply third shingle to sentence.

		// Step1: getSampleContractsReady - strips Html (stripHtml), grabs just
		// body of K. This method also calls - getContractWithoutExhibits. These
		// methods work on all files in folder.

		// Step2: sentence and word tokenizer run against contract w/o exhibits
		// and each sentence and word inputted into mysql nlp tables. Each
		// sentence is numbered.

		// Step3: I now run stop and stemmer on contract that retains each
		// sentence number. And I input into mysql the sentence number that has
		// the term I'm looking for.

		// Step4: run analysis in mysql based on common words in term but not
		// elsewhere in doc.

		// TODO: for html files I'm not skipping table of contents. Fix by
		// skipping first 5% of a file based on text length.

		// TODO: move below to its own method

		NLP.pwNLP = new PrintWriter("c:/getContracts/tmp.txt");
		File files = new File("c:/getContracts/unstrippedKs/");
		String folderStrippedKs = "c:/getContracts/strippedKs/";
		String folderStoppedAndStemmed = "c:/getContracts/stoppedAndStemmed/";
		String folderMarked = "c:/getContracts/marked/";
		String folderSentencesWords = "c:/getContracts/sentencesWords/";

		Pattern patternContractNameFilter = Pattern
				.compile("(?i)certificat|sarba|filing|eligibili|advisor|form_t_|liquidat");

		File filesStrippedKs = new File(folderStrippedKs);
		// Utils.deleteDirectory(filesStrippedKs);
		// Utils.createFoldersIfReqd(folderStrippedKs);

		File filesStoppedAndStemmed = new File(folderStoppedAndStemmed);
		// Utils.deleteDirectory(filesStoppedAndStemmed);
		// Utils.createFoldersIfReqd(folderStoppedAndStemmed);

		File filesMarked = new File(folderMarked);
		// Utils.deleteDirectory(filesMarked);
		// Utils.createFoldersIfReqd(folderMarked);

		File filesSentencesWords = new File(folderSentencesWords);
		// Utils.deleteDirectory(filesSentencesWords);
		// Utils.createFoldersIfReqd(folderSentencesWords);

		// grabs just body of contract (no exhibits, toc, etc.). This method is
		// only need for testing.

//		getSampleContractsReady(files, folderStrippedKs,
//				patternContractNameFilter);

		// TODO: move this to existing or new method

		System.out.println("folder=" + folderStrippedKs);
		File folder = new File(folderStrippedKs);
		File[] listOfFiles = folder.listFiles();

		NLP.pwNLP.append(NLP.println("listOfFiles.len=", ""
				+ listOfFiles.length));

		for (File file : listOfFiles) {
			System.out.println("sentenaceAndWordTokenizer - file="
					+ file.getName());
			String[] sentWordAry = sentenceAndWordTokenizer(file);

			String filename = "c:/getContracts/sentencesWords/sentences_"
					+ file.getName();

			PrintWriter pw = new PrintWriter(new File(filename));
			pw.append(sentWordAry[0]);
			pw.close();

			String query = "LOAD Data INFILE '" + filename
					+ "' ignore INTO TABLE nlp_sentences"
					+ " FIELDS TERMINATED BY '||' lines terminated by '\\n';";
			MysqlConnUtils.executeQuery(query);

			filename = "c:/getContracts/sentencesWords/words_" + file.getName();
			pw = new PrintWriter(new File(filename));
			pw.append(sentWordAry[1]);
			pw.close();

			query = "LOAD Data INFILE '" + filename
					+ "' ignore INTO TABLE nlp_words"
					+ " FIELDS TERMINATED BY '||' lines terminated by '\\n';";
			MysqlConnUtils.executeQuery(query);

			// save files sentence file w/o kId and run shingles against
			// that. But keep sentence no.

			StringBuffer sb = new StringBuffer();
			String[] contractSentSplit = sentWordAry[0].split("[\r\n]");

			for (int i = 0; i < contractSentSplit.length; i++) {
				String[] sentNoAndsentence = contractSentSplit[i]
						.split("\\|\\|");
				for (int n = 0; n < sentNoAndsentence.length; n++) {
					if (n == 0) {
						sb.append("\r\n");
						continue;
					}
					if (n == 1) {
						sb.append(sentNoAndsentence[n] + "||");
						// System.out.println("append="+sentNoAndsentence[n]+"||");
					}

					if (n == 2) {
						sb.append(Stemmer.stemmedOutPut(StopWords
								.removeStopWords(sentNoAndsentence[n]),true));
						// System.out.println("stopped words removed and stemmed - append="
						// + Stemmer.stemmedOutPut(StopWords
						// .removeStopWords(sentNoAndsentence[n])));
					}

				}
			}

			PrintWriter pwSt = new PrintWriter(new File(folderStoppedAndStemmed
					+ file.getName()));
			pwSt.append(sb.toString());
			pwSt.close();
		}

		// shingle must must be stopped / stemmed

		// String[] shinglesCombo1 = {
		// "(?ism)([\\r\\n\\t ]{1,3}|\\|)law[\\r\\n\\t ]{1,3}" + // required
		// "(State[\\r\\n\\t ]{1,3})?" + // optional
		// "([a-z]{2}|commonwealth|provinc|feder|republ|said.{1,3}State)[\\.,\\r\\n\\t ]{1,3}"
		// // required
		// , "(?ism)govern|constru.{1,15}accord|enforc|determin.{1,10}accord" };
		// String termType = "gLaw";

		// shinglesCombo have to developed on a sentence by sentence basis.
		// Current adding limit law will get another sentence when paired with
		// comp. Its possible some sentences will combine the 2 but then I'll
		// get 2 hits of the same sentence which is fine.

		boolean sameSentence = true;
		// list of all stripped contracts in /strippedKs folder
		getStrippedKs(folderStoppedAndStemmed);

		// getShingles run through stopped and stemmed files and check each
		// sentence by calling getSentenceTerms where each shingle is determined
		// if in sentence and w/n the distance specified w/n that sentence. If
		// sameSentence=true in getSentenceTerms restricts pairing of shingles
		// to w/n sentence. To determine if getSentenceTerms
		// is a false positive excludeFalsePositives is called and if
		// shinglesNotPermitted is in sentenceTerm based on specified distance
		// that sentenceTerm is then excluded b/c it is a f/o.

		// TODO: If I have two confirmed sentenceTerm but they skip a sentence
		// that has related sentenceTerm terms how do I wish to address?

		// TODO: create Shingle for each sentence with own termType. If if two
		// termTypes in same Shingle then when I should return same sentence for
		// each termType looped thru and I would record the two termTypes that
		// relate to that single sentence.

		// Loop thru related termTypes that are often sentence specific

		// TODO: mark each contract with found term AND save contract to same
		// location so that when next term is marked it uses previously marked
		// contract. This is necessary after I've finalized term shingles and
		// want to mark contract for solr ingestion. Need to figure out order
		// etc.

		// TODO: for shingles and each substring create 'public' string that
		// equals the term type

		// TODO: BOTH SETS OF SHINGS (PERMITTED AND NOT PERMITTED) SHOULD BE
		// ADDED TO A List<String[]> so that if 2 or more need to present or not
		// present - I can add and check each. Change getSentenceTerm to a loop
		// through that list.

		NLP.pwNLP.close();

		System.out.println("pwNLP closed");

		// TODO: will need to develop logic to tie 'survival of this section' to
		// whaT substance of those terms are. That should be relatively easy
		// after through Id of various terms that precede it - e.g., indemnity.

		// TODO: some terms will either be in 1 or 2 sentences but they need to
		// be non-separable. In those instances shingles will operate on
		// consecutive sentences if all not found in single.

		// TODO: when it is a lead in clause that connect to other clauses via
		// (i), (ii) and so on - in order to create full sentences I'll want to
		// play with the idea of connecting them - or certainly later when I
		// attempt to run redlines - else variation can be very high and make it
		// difficult to run intelligent redlines.

		// TODO: redline should also be taught to ignore company v issuer etc or
		// before running redline it asks who is who and that then gets
		// substituted in library. Of those terms in library are marked in such
		// a way as to be able to unredline results.

		// TODO: once I get term identifier - see what terms are in a section
		// and see if section heading is correctly labeled. E.g., Compensation
		// and Reimbursement which should really be Compensation, Reimbursement
		// and Indemnity

		// TODO: create method that can quickly identify fully justified text.
		// And then makes it left aligned - see e.g.,
		// 0000849213-05-000062_1_EX_FORM_OF_DEBT_SECURITIES_INDENTURE.txt
		// run pattern matcher of multiple whitespaces sandwhiched between lower
		// case alpha text and if that occurs more than 100 times - I know.

	}
}
