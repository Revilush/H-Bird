package algo_testers.dependencies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import charting.JSonUtils;
import contracts.GoLaw;

public class GoLawDiff extends TextDiffHelper {
	
	protected static GoLaw glaw = new GoLaw();
	
	//private static final Pattern spacePuncts = Pattern.compile("[ \\p{Punct}}]+");
	//private static final String spacePuncts = "[ \\p{Punct}}]+| ?and ?| ?or ?";
	private static final String spacePuncts = "[ \\p{Punct}}]+";
	
	private boolean ignoreCase = false;
	private boolean ignorePunctuations = false;
	
	private List<List<DiffDetail>> consecutiveDiffsList = null;
	List<DiffDetail> loneDiffsList = null;
	
	public GoLawDiff() {}
	
	public GoLawDiff(boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
	}
	
	public GoLawDiff(boolean ignoreCase, boolean ignorePunctuations) {
		this.ignoreCase = ignoreCase;
		this.ignorePunctuations = ignorePunctuations;
	}
		
		

	@Override
	public List<DiffDetail> getDiff(String text1, String text2) {
		long start = System.currentTimeMillis();
		TextDiffHelper tdh = new LCSDiff(ignoreCase, ignorePunctuations);
		List<DiffDetail> orgDiffs = tdh.getDiff(text1, text2);
		if (log.isTraceEnabled())
			log.trace("\n GoLawDiff start: lcsDiff millis:" + (System.currentTimeMillis() - start) +" :: diffs= "+ orgDiffs);
		
		
		// now apply the rules/algo:
		
		// get the scattered word-lists if there:
		//List<String> scatteredWordLists = getScatteredLists(text1, text2);
		
		// get the consecutive diffs (2 or more diffs together : next to each other)
		getConsecutiveAndLoneDiffs(orgDiffs);
		/*
		 * for each set of consecutive diffs: do the following
		 * 		= Rule # 1 : equal pairs
		 * 		- if the set has 2+ diffs, merge deletions/insertions together so we have only 2 diffs - 1 deletion and 1 insertion
		 * 		- remove all stop-words from each diff. 
		 * 			- If no text is left in each diff then they are same/equal thus revert the original diffs and treat them equal
		 * 			- If some words are left (then these are important words survived stemming etc) then see if both sides have the same words (regardless of their order)
		 * 			- If still not same, apply synonyms to texts of both diffs and re-apply the rule. 
		 * 
		 * 		= Rule # 2 : Undo Single Blanks
		 * 		- Discard when lone Insertion or Deletion (only 1 insertion or 1 deletion) has ONLY Defined-Terms or Stop-Words: 
		 * 			- If the diff is a defined-term with or without stopwords. If so, revert the red line.
		 * 
		 * 		= Rule # 3 : Undo Moves
		 * 		- two or more important words are in different locations of the paragraph or sentence and in one case deleted and in another case inserted. 
		 * 			These are treated as equal and the red line is undone.
		 * 
		 * 		= Rule # 4 : Undo Equal Words Different Case
		 * 		- consider words with 1st letter lower/capital as equal (ie security/Security | person/Person | .....)
		 * 		
		 * 		= Rule # 5 : Scattered List :- ie  [destroyed, mutilated, replaced, canceled, lost or stolen]  v/s  [mutilated, destroyed, canceled, replaced, lost or stolen] etc
		 * 		- list of words that are not in same order, goal is to eliminate or minimize the diffs
		 * 		- We find lists using regex (grab 2 words before comma and 2 words after "and / or" etc). No items encased/sandwich between 2 commas can be more than 2 words. (If it is - list fails and we keep the diffs). 
		 * 			- Then you reorder the list in alphabetical order to retain the RL from that.
		 * 
		 */
		List<DiffDetail> twoDiffs;
		String stem1, stem2;
		DiffDetail dd;
		QuickSynonymApplier qSynApplier = new QuickSynonymApplier();
		String[] syndSents;
		
		// Rule # 1, 4
		// Partial Rule # 5 (scattered list): will also be handled here if words of list are same but just re-ordered.
		for (List<DiffDetail> consecDiffs : consecutiveDiffsList) {
			// copy the diffs and merge into 2 possibly
			twoDiffs = copyAndMergeIfRequired(consecDiffs);
			// Rule#4: check if any word is same in both side, regardless case (ie 1 side has lower case and other has upper case letter) :
			// 	make the corresponding word in 2nd text equal to first word, to be treated same in later steps
			ignoreCaseForSameWords(twoDiffs);
			// remove all stop-words from each diff. If nothing left in each diff then they are same/equal and revert
			stem1 = tokenized_def_stop_stem(twoDiffs.get(0).text); 
			stem2 = tokenized_def_stop_stem(twoDiffs.get(1).text);
			boolean isSameText = areTextsHaveSameWords(stem1, stem2);
			if (! isSameText) {
				// Rule # 1, 4
				if (log.isTraceEnabled())
					log.trace("texts were not same, applying synonyms and trying again: stem1="+stem1+", stem2="+stem2 +" :: 2-diffs: "+ twoDiffs);
				// here we can look to apply synonyms and check again if they are same post-syn-application.
				syndSents = qSynApplier.applyQuickSynonyms(twoDiffs.get(0).text.trim(), twoDiffs.get(1).text.trim());
				stem1 = tokenized_def_stop_stem(syndSents[0]);
				stem2 = tokenized_def_stop_stem(syndSents[1]);
				isSameText = areTextsHaveSameWords(stem1, stem2);
				if (!isSameText)
					if (log.isTraceEnabled())
						log.trace("texts were different even after syn-application: stem1="+stem1+", stem2="+stem2 + ", syndSents=" + ArrayUtils.toString(syndSents));
			} 
			if (isSameText) {
				if (log.isTraceEnabled())
					log.trace("found same texts: " + twoDiffs.get(0).text +" :: " + twoDiffs.get(1).text + ", stem1="+stem1+", stem2="+stem2+ " ::: " + consecDiffs );
				// revert original diffs back to equal - remove insertions and change deletions to equals
				for (int i=0; i < consecDiffs.size(); i++) {
					dd = consecDiffs.get(i);
					if (dd.op == DiffOperation.INS) {
						orgDiffs.remove(consecDiffs.remove(i));
						i--;
					} else {
						dd.op = DiffOperation.EQ;
					}
				}
				if (log.isTraceEnabled())
					log.trace("found same texts so diffs are reverted: " + consecDiffs );
			} else {
				/*
				 * Rule # 5 : Scattered List :- find a list in remaining diffs.
				 * 
				 * If list found, it means the 1/both sides of words-list have some extra word  (ie. [destroyed, mutilated, canceled, lost or stolen]  v/s  [mutilated, destroyed, replaced, lost or stolen] etc) 
				 * 		that are missing in other side and not just re-ordered (a simple reorder of list-words would have been handled in Rule#1 above).
				 * 		Try to minimize the diffs now.
				 * 
				 * If no list found:  keep all original diffs
				 */
				/*
				if (null != scatteredWordLists  &&  scatteredWordLists.size() >= 2) {
					// if there should a list, it must be in both sents
					DiffDetail delDif = (twoDiffs.get(0).op == DiffOperation.DEL)? twoDiffs.get(0): twoDiffs.get(1);
					DiffDetail insDif = (twoDiffs.get(0).op == DiffOperation.INS)? twoDiffs.get(0): twoDiffs.get(1);
					stem1 = delDif.text;		// delete text 
					stem2 = insDif.text;		// insert text
					// see if 'delDif' text words are in list1, and 'insDif' words are in list2. If so, we want to minimize the diffs here
					if (scatteredWordLists.get(0).contains(stem1)  &&  scatteredWordLists.get(1).contains(stem2)) {
						System.out.println("word list found in diffs!!! " + stem1 +" :: "+ stem2);
						// if any word is in both - deletion and insertion block, make them equal
						List<DiffDetail> delDiffList = new ArrayList<>();
						List<DiffDetail> insDiffList = new ArrayList<>();
						for (DiffDetail cd : consecDiffs) {
							if (cd.op == DiffOperation.DEL)
								delDiffList.add(cd);
							else if (cd.op == DiffOperation.INS)
								insDiffList.add(cd);
						}
						String[] words;
						boolean delTxtTakenCareOf = false;
						for (DiffDetail delD : delDiffList) {
							delTxtTakenCareOf = false;
							stem1 = delD.text;
							//System.out.println("delD = " + delD);
							// if this whole deleted text is in some insertion, revert both 
							for (DiffDetail insD : insDiffList) {
								if (insD.text.contains(stem1)) {
									System.out.println("whole delD text found in an insert= " + delD +" :: "+ insD);
									delD.op = DiffOperation.EQ;			// make deletion as equal
									if (insD.text.equals(stem1)) {
										orgDiffs.remove(insD);
									} else {
										insD.text.replace(stem1, "");
									}
									delTxtTakenCareOf = true;
								}
							}
							if (! delTxtTakenCareOf) {
								System.out.println("delD text needs W-by-W checks= " + delD);
								// word-by-word replacement
								words = stem1.split("(?=[\\s\\h])");
								for(String w : words) {
									for (DiffDetail insD : insDiffList) {
										if (insD.text.contains(w)) {
											System.out.println("a del word was found in insertion: " + w + " :: " + insD);
										}
									}
								}
							}
							
						}
					}
				}
				*/
				
			}
		}
		
		// Rule # 2
		for (DiffDetail loneDiff : loneDiffsList) {
			stem1 = tokenized_def_stop_stem(loneDiff.text);
			if (StringUtils.isBlank(stem1)) {
				if (log.isTraceEnabled())
					log.trace("non-imp lone diff found: " + loneDiff );
				// this diff is of no importance - treat as equal
				if (loneDiff.op == DiffOperation.INS) {
					orgDiffs.remove(loneDiff);
				} else {
					loneDiff.op = DiffOperation.EQ;
				}
			}
		}

		/*
		 * Rule # 3
		 * 		two or more important words are in different locations of the paragraph or sentence and in one case deleted and in another case inserted. 
		 * 		These are treated as equal and the red line is undone.
		 */
		List<DiffDetail> diffs = new ArrayList<>();
		for (DiffDetail dif : orgDiffs) {
			if (dif.op != DiffOperation.EQ)
				diffs.add(dif);
		}
		handleMoves(diffs, orgDiffs, 2);
		
		/* Rule # 5: word-lists
		if (null != scatteredWordLists  &&  scatteredWordLists.size() >= 2) {
			// find the diffs that corresponds to words of the lists.
			List<DiffDetail> delDiffs = findDiffsCorresponding2WordsList(scatteredWordLists.get(0), orgDiffs, DiffOperation.INS);
			List<DiffDetail> insDiffs = findDiffsCorresponding2WordsList(scatteredWordLists.get(1), orgDiffs, DiffOperation.DEL);
			diffs = new ArrayList<>(delDiffs);
			diffs.addAll(insDiffs);
			handleMoves(diffs, orgDiffs, 1);
		}
		*/
		
		// return the final diffs now
		return orgDiffs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DiffDetail> convertNativeDiffList(List<? extends Object> diffs) {
		return (List<DiffDetail>) diffs;
	}
	
	
	// ------------------------------------
	/**
	 * Rule#4: 
	 * @param twoDiffs
	 */
	private void ignoreCaseForSameWords(List<DiffDetail> twoDiffs) {
		String spacePunctPtrn = "(?<=[ \\p{Punct}}])|(?=[ \\p{Punct}}])";
		String[] W0 = twoDiffs.get(0).text.split(spacePunctPtrn);
		String[] W1 = twoDiffs.get(1).text.split(spacePunctPtrn);
		String w0, w1;
		int lastJ=0;
		for (int i=0; i < W0.length; i++) {
			w0 = W0[i];
			if (w0.matches(spacePuncts))
				continue;
			for (int j=lastJ; j < W1.length; j++) {
				w1 = W1[j];
				if (w0.equalsIgnoreCase(w1)) {		// same word regardless of case, and no space/punct
					W1[j] = w0;			// words match, lets make the 2nd word equal to first word, so they'll be treated same while stemming etc
					lastJ = j+1;		// since word at 'j' is already seen/matched, next word should match at later indexes
					break;
				}
			}
		}

		// since only the words of 2nd diff were changed, lets pick its text again 
		twoDiffs.get(1).text = StringUtils.join(W1, "");
	}
	
	private List<DiffDetail> findDiffsCorresponding2WordsList(String listTxt, List<DiffDetail> orgDiffs, DiffOperation discardThisOp) {
		// find the diffs that corresponds to words of the lists.
		
		String[] words;
		int wordsFoundSoFar = 0;
		Set<DiffDetail> uniqDiffs = new LinkedHashSet<>();
		// min 3/4 words should be found in diffs that are in the list to say the diffs correspond to list.
		int listWordsCount = listTxt.split("[\\s\\h]").length;
		int desiredWord2BFound = (int) (listWordsCount * 0.75);
		int lastIndex = -1, idx;
		for (DiffDetail dif : orgDiffs) {
			if (dif.op == discardThisOp)
				continue;
			// only deletion/equal words should be in listTxt1
			words = dif.text.trim().split("[\\s\\h]");
			for (String w : words) {
				if (w.matches("[\\p{Punct}}]"))
					continue;
				if (listTxt.matches(".*?\\b"+Pattern.quote(w)+".*")) {		// TODO: FIXME: track the start/end Index of word to break when all words are found - this avoids picking extra words/diffs which are out of word-list
					idx = listTxt.indexOf(w);
					if (idx > lastIndex)
						lastIndex = idx;
					else
						break;
					wordsFoundSoFar ++;
					if (dif.op != DiffOperation.EQ)
						uniqDiffs.add(dif);
				} else {
					// this word was not found in the list - either we have enough/actual words found in the list and no more required, or
					// some random word must have been found earlier. but not enough words.
					if (wordsFoundSoFar >= desiredWord2BFound) {
						break;
					}
					lastIndex = 0;
					wordsFoundSoFar = 0;
					uniqDiffs.clear();
				}
			}
			if (wordsFoundSoFar >= listWordsCount) {
				break;
			}
		}
		List<DiffDetail> diffs = new ArrayList<>(uniqDiffs);
		if (log.isTraceEnabled())
			log.trace("Diff-words found in the list: wordsFoundSoFar="+wordsFoundSoFar+ " -:- op-2-discard:"+discardThisOp +" :: listText="+ listTxt + " :: diffs=" + diffs);
		return diffs;
	}
	
	
	private List<String> getScatteredLists(String text1, String text2) {
		////  [destroyed, mutilated, canceled, lost or stolen]  v/s  [mutilated, destroyed, replaced, lost or stolen] 
		
		// how to identify existence of a list:  min 3 commas must be present in the sent
		// pick first 2 words from start of list, and 2 words after "and/or".
		//String scatteredListPtrn = "(?i)([\\w]+\\b [\\w]+\\b,[^,]+,[^,]+,[^,]*?\\b[and/or ]+[\\w]+\\b ?[\\w]*\\b?)";
		String scatteredListPtrn = "(?i)([\\w]+\\b [\\w]+\\b,[\\w ]+,[\\w ,]+?\\b[and/or]+ [\\w]+\\b ?[\\w]*\\b?)";
		// list from 1st sent
		List<String> list1 = getAllMatchedGroups(text1, Pattern.compile(scatteredListPtrn));
		if (null == list1  ||  list1.size() == 0)
			return null;
		// list from 2nd sent
		List<String> list2 = getAllMatchedGroups(text2, Pattern.compile(scatteredListPtrn));
		if (null == list2  ||  list2.size() == 0)
			return null;
		if(log.isDebugEnabled())
			log.debug("GoLawDiff: word-lists found:" + list1 +" :: " + list2);
		// return the 2 lists
		List<String> scatteredList = new ArrayList<>();
		scatteredList.add(list1.get(0));
		scatteredList.add(list2.get(0));
		return scatteredList;
	}
	
	private void handleMoves(List<DiffDetail> insDelDiffs, List<DiffDetail> orgDiffs, int minWords) {
		Map<String, DiffDetail> stemW2DiffMap = new HashMap<>();
		Set<String> uniqWords = new TreeSet<>();
		String[] words;
		String sortedWords;
		String stemWords;
		for (DiffDetail dif : insDelDiffs) {
			// the diff is either deletion or insertion
			stemWords = tokenized_def_stop_stem(dif.text.toLowerCase());
			if (StringUtils.isBlank(stemWords))
				continue;
			words = stemWords.split(" ");
			if (words.length < minWords)			// we need minimum 'x' important words
				continue;
			// this diff has min 2 important words
			uniqWords.clear();
			for (String w : words)
				uniqWords.add(w);
			sortedWords = StringUtils.join(uniqWords, " ");
			if (stemW2DiffMap.containsKey(sortedWords)) {
				// we found same imp words in 2 diffs - 1 earlier who put into map and this one who found
				// test if one diff is deletion and another is insertion.
				DiffDetail dif1 = stemW2DiffMap.get(sortedWords);
				if (dif1.op != dif.op) {
					// revert both diffs - deletion to equal and remove insertion
					DiffDetail difIns = (dif.op == DiffOperation.INS? dif: dif1);
					DiffDetail difDel = (dif.op == DiffOperation.DEL? dif: dif1);
					// restore the deletion
					difDel.op = DiffOperation.EQ;
					// remove insertion from list of diffs
					orgDiffs.remove(difIns);
					stemW2DiffMap.remove(sortedWords);		// remove the dif from map - its been taken care of
					log.info("GoLawDiff. Rule#3 found: " + difIns + " :: " + difDel);
				}
				continue;
			}
			stemW2DiffMap.put(sortedWords, dif);
		}
		stemW2DiffMap.clear();
	}
	
	
	private boolean areTextsHaveSameWords(String text1, String text2) {
		// sort text1 words
		Set<String> s1 = new TreeSet<>();
		String[] words = text1.split(" ");
		for (String w : words)
			s1.add(w);
		// sort text2 words		
		Set<String> s2 = new TreeSet<>();
		words = text2.split(" ");
		for (String w : words)
			s2.add(w);
		
		// see if they make same string
		return (StringUtils.join(s1, " ").equals(StringUtils.join(s2, " ")));
	}
	
	private List<List<DiffDetail>> getConsecutiveAndLoneDiffs(List<DiffDetail> diffs) {
		DiffDetail dif;
		this.loneDiffsList = new ArrayList<>();
		this.consecutiveDiffsList = new ArrayList<>();
		List<DiffDetail> sublist = new ArrayList<>();
		for (int i=0; i < diffs.size(); i++) {
			dif = diffs.get(i);
			if (dif.op == DiffOperation.EQ  &&  !dif.text.matches(spacePuncts) ) {
				if (sublist.size() > 1)	{			// we need minimum 2 RLs next to each other
					consecutiveDiffsList.add(sublist);
				} else if (sublist.size() == 1) {
					dif = sublist.get(0);
					if (! dif.text.matches(spacePuncts))
						loneDiffsList.add(dif);
				}
				sublist = new ArrayList<>();
			} else {
				sublist.add(dif);
			}
		}
		// at the end
		if (sublist.size() > 1)
			consecutiveDiffsList.add(sublist);
		return consecutiveDiffsList;
	}

	private List<DiffDetail> copyAndMergeIfRequired(List<DiffDetail> diffs) {
		List<DiffDetail> copy = new ArrayList<>();
		// copy the list, so references are broken from original diff list when we manipulate it later
		for (DiffDetail dd : diffs) {
			copy.add(new DiffDetail(dd.op, dd.text, dd.seq, dd.index));
		}
		// see if we need to merge some diffs together (join all deletions together and all insertions together
		DiffDetail diff1 = new DiffDetail(DiffOperation.DEL, "");
		DiffDetail diff2 = new DiffDetail(DiffOperation.INS, "");
		DiffDetail dif;
		for (int i=0; i < copy.size(); i++) {
			dif = copy.get(i);
			if (dif.op == DiffOperation.EQ) {
				diff1.text += dif.text;
				diff2.text += dif.text;
			} else {
				if (dif.op == diff1.op) {
					//if (!diff1.text.endsWith(" ")  &&  !dif.text.startsWith(" "))
					//	diff1.text += " ";
					diff1.text += dif.text;
				} else {
					//if (!diff2.text.endsWith(" ")  &&  !dif.text.startsWith(" "))
					//	diff2.text += " ";
					diff2.text += dif.text;
				}
			}
		}
		copy.clear();
		copy.add(diff1);
		copy.add(diff2);
		return copy;
	}
	
	
	public static void main(String[] arg) throws JsonProcessingException {
		String t1 = "If an Event of Default has occurred and is continuing, the Trustee shall exercise such of the rights and powers vested in it by this Indenture, and use the same degree of care and skill in its exercise, as a prudent person would exercise or use under the circumstances in the conduct of such person's own affairs.";
		String t2 = "In case an Event of Default actually known to a Responsible Officer of the Trustee has occurred and is continuing, the Trustee shall exercise such of the rights and powers vested in it by this Indenture, and use the same degree of care and skill in their exercise, as a prudent person would exercise or use under the circumstances in the conduct of such person's own affairs.";

		t1="(a) If an Event of Default has occurred and is continuing, the Trustee shall have exercise such of the rights and powers vested in it by this Indenture, and use the same degree of care and skill in its exercise, as a prudent person would exercise or use under the circumstances in the conduct of such person's own affairs.";
		t2="(a) If an Event of Default has occurred and is continuing, the Trustee has exercise such of the rights and powers vested in it by this Indenture, and use the same degree of care and skill in its exercise, as a prudent person would exercise or use under the circumstances in the conduct of such person's own affairs.";
		
		t2="(b) In case an Event of Default has occurred and is continuing, the Trustee shall exercise such of the rights and powers vested in it by this Indenture, and use the same degree of care and skill in their exercise, as a prudent man would exercise or use under the circumstances in the conduct of his own affairs.";
		
		t2="(b) If an Event of Default known to the Trustee has occurred and is continuing, the Trustee shall, prior to the receipt of directions, if any, from the Holders of at least a majority in aggregate principal amount of the Outstanding Securities, exercise such of the rights and powers vested in it by this Indenture, and use the same degree of care and skill in its exercise, as a prudent person would exercise or use under the circumstances in the conduct of such person's own affairs.";
		
		
		t1 = "Except as otherwise provided with respect to the replacement or payment of destroyed, mutilated, cancelled, lost or stolen Notes in Section 2.07, no right or remedy herein conferred upon or reserved to the Trustee or to the Holders is intended to be exclusive of any other right or remedy, and every right and remedy are, to the extent permitted by law, cumulative and in addition to every other right and remedy given hereunder or now or hereafter existing at law or in equity or otherwise.";
		
		t2 = "Except as otherwise provided with respect to the placement or payment of mutilated, destroyed, lost or stolen Notes in Section 2.07 hereof, no right or remedy herein conferred upon or reserved to the Trustee or to the Holders is intended to be exclusive of any other right or remedy, and every right and remedy shall, to the extent permitted by law, be cumulative and in addition to every other right and remedy given hereunder or now or hereafter existing at law or in equity or otherwise.";
		t2 = "Except as otherwise provided with respect to the replacement or payment of mutilated, destroyed, lost or stolen Subordinated Debt Securities in the last paragraph of Section 3.06 and without prejudice to Section 5.02, no right or remedy herein conferred upon or reserved to the Trustee or to the Holders of Subordinated Debt Securities is intended to be exclusive of any other right or remedy, and every right and remedy shall, to the extent permitted by law, be cumulative and in addition to every other right and remedy given hereunder or now or hereafter existing at law or in equity or otherwise.";
		
		t1 = "If an Event of Default has occurred and is continuing, the Trustee shall exercise the rights and powers vested in it by this Indenture and use the same degree of care and skill in its exercise as a prudent Person would exercise or use under the circumstances in the conduct of such person's own affairs.";
		t2 = "If an Event of Default has occurred (that has not been cured or waived), the Trustee shall exercise the rights and powers vested in it by this Indenture, and use the same degree of care and skill in its exercise, as a prudent man would exercise or use under the circumstances in the conduct of his own affairs.";
		
		t1 = " If an Event of Default has occurred and is continuing, the Trustee shall exercise the rights and powers vested in it by this Indenture and use the same degree of care and skill in its exercise as a prudent Person would exercise or use under the circumstances in the conduct of such person’s own affairs.";
		t2 = "(b) In case an Event of Default with respect to Junior Subordinated Notes of any series has occurred and is continuing, the Trustee shall exercise, with respect to Junior Subordinated Notes of such series, such of the rights and powers vested in it by this Indenture, and use the same degree of care and skill in their exercise, as a prudent man would exercise or use under the circumstances in the conduct of his own affairs.";
		t2 = "If an Event of Default has occurred and is continuing, the Trustee shall exercise the rights and powers vested in it by this Indenture, and use the same degree of care and skill in its exercise, as a prudent man would exercise or use under the circumstances in the conduct of his own affairs.";
		
		t1 = "in case a Person who is Indenture trustee";
		t2 = "if a person who is Agreement trustee";
		
		t1="“Opinion of Counsel” means a written opinion from legal counsel reasonably satisfactory to the Trustee. The counsel may be an employee of or counsel to Holdings, the Issuer, any of its Subsidiaries or the Trustee.";
		t2="“Opinion of Counsel” means an opinion in writing subject to customary exceptions of legal counsel may be an employee of or counsel to Holdings, the Company, that is delivered to the Trustee in accordance with the terms hereof.";
		
		// "In case a Default ...."
		TextDiffHelper tdh = new GoLawDiff(false, true);
		List<DiffDetail> diffs = tdh.getDiff(t1, t2);
		System.out.println(JSonUtils.object2JsonString(diffs));
		//System.out.println(tdh.getDiffHtml(diffs));
		System.out.println(tdh.getFinalPlainText(diffs));
		
		tdh = new LCSDiff(false, true);
		diffs = tdh.getDiff(t1, t2);
		System.out.println(JSonUtils.object2JsonString(diffs));
	}
	
	private String tokenized_def_stop_stem(String txt) {
		return glaw.goLawGetHtxt(txt);
	}
	
	
	public static void main2(String[] arg) throws JsonProcessingException {
		GoLawDiff gld = new GoLawDiff(false, true);
		String t1 = "The Trustee shall be under no obligation to exercise any of the rights or powers vested in it by this Indenture at the request or direction of any of the Holders of Securities unless such Holders shall have offered to the Trustee security or indemnity satisfactory to it against the costs, expenses and liabilities which might be incurred by it in compliance with such request or direction.";
		String t2 = "The Trustee shall be under no obligation to exercise any of the rights or powers vested in it by this Indenture at the request, order or direction of any of the Holders pursuant to the provisions of this Indenture, unless such shall have offered (and if requested, provided) to the Trustee indemnity satisfactory to it against the costs, expenses, claims and liabilities that may be incurred therein or thereby";
		System.out.println(JSonUtils.object2JsonString(gld.getScatteredLists(t1, t2)));
	}
	
	
}
