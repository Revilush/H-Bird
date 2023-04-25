package algo_testers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;

import com.fasterxml.jackson.core.JsonProcessingException;

import contracts.GoLaw;
import contracts.Stemmer;
import contracts.StopWords;
import xbrl.NLP;

public class Simple_HL_Mapped_Only {

	public static String getStyles() {
		return "<style>"
				+ "red{color:red;}"
				+ "</style>";
	}
	public static List<String> HLSentences(String clientSent, String goSent) throws FileNotFoundException  {
		List<List<String>> hlResp = simpleHL(clientSent, goSent);
		List<String> words2HL = hlResp.get(0);
		// HL client sent words
		for (String w : words2HL) {
			clientSent = clientSent.replaceAll(w, "<red>"+w+"</red>");
		}
		// HL go sent words
		words2HL = hlResp.get(1);
		for (String w : words2HL) {
			goSent = goSent.replaceAll(w, "<red>"+w+"</red>");
		}
		return new ArrayList<>(Arrays.asList(clientSent, goSent));
	}
	
	
	public static List<List<String>> simpleHL(String clientSent, String goSent) throws FileNotFoundException  {

		//GoLaw gl = new GoLaw();

		HashMap<String, String> mapClient = new HashMap<String, String>();
		HashMap<String, String> mapGo = new HashMap<String, String>();

		String key = "";

		String clientStopped = StopWords.removeStopWords(clientSent.replaceAll("(?ism)\\([a-z\\d]{1,6}\\)", "")).trim();
		String clientStoppedNoDef = StopWords.removeDefinedTerms(clientStopped).trim();// 1

		clientStoppedNoDef = clientStoppedNoDef.replaceAll("[^a-zA-Z\\d ]", "").trim();
		String[] clientWordSplit = clientStoppedNoDef.split(" ");

		String clientHtxt = Stemmer.stemmedOutPutWithPunctuationRemoved(clientStoppedNoDef); // 2		//PA:TODO: should this be  GoLaw.getHtxt...()
		String[] clientHtxtSplit = clientHtxt.split(" ");

		for (int i = 0; i < clientHtxtSplit.length; i++) {
			key = clientHtxtSplit[i];
			mapClient.put(key, clientWordSplit[i]);					//PA: TODO: is this correct? what if hTxt removes a word?
		}

		String goStopped = StopWords.removeStopWords(goSent.replaceAll("(?ism)\\([a-z\\d]{1,6}\\)", "")).trim();
		String goStoppedNoDef = StopWords.removeDefinedTerms(goStopped).trim();// 1

		goStoppedNoDef = goStoppedNoDef.replaceAll("[^a-zA-Z\\d ]", "").trim();
		String[] goWordSplit = goStoppedNoDef.split(" ");

		String goHtxt = Stemmer.stemmedOutPutWithPunctuationRemoved(goStoppedNoDef); // 2
		String[] goHtxtSplit = goHtxt.split(" ");

		for (int i = 0; i < goHtxtSplit.length; i++) {
			key = goHtxtSplit[i];
			mapGo.put(key, goWordSplit[i]);
		}
		int cnt = 0;

		List<String> listClientWordsToHL = new ArrayList<String>();
		cnt = 0;
		for (Map.Entry<String, String> entry : mapClient.entrySet()) {
			key = entry.getKey();
//			System.out.println("client htxt==="+key);
			if (!mapGo.containsKey(key)) {
//				System.out.println("client msg==="+key);
				// hl
				listClientWordsToHL.add(entry.getValue());
				System.out.println("cnt=" + cnt + " client-msg===" + key + " msg-word==" + entry.getValue());
			}
			cnt++;
		}

		List<String> listGoWordsToHL = new ArrayList<String>();
		cnt = 0;
		for (Map.Entry<String, String> entry : mapGo.entrySet()) {
			key = entry.getKey();
			if (!mapClient.containsKey(key)) {
				// hl
				listGoWordsToHL.add(entry.getValue());
				System.out.println("cnt=" + cnt + " go-msg===" + key + " msg-word==" + entry.getValue());
			}
			cnt++;
		}

//		System.out.println("CIENT TEXT");
//		System.out.println(clientStopped + "\r\n");
//		System.out.println(clientStoppedNoDef + "\r\n");
//		System.out.println(clientStoppedNoDef.split(" ").length + "\r\n");
//		System.out.println(clientHtxt + "\r\n");
//		System.out.println(clientHtxt.split(" ").length);
//		System.out.println("GO TEXT");
//		System.out.println(goStopped + "\r\n");
//		System.out.println(goStoppedNoDef + "\r\n");
//		System.out.println(goStoppedNoDef.split(" ").length + "\r\n");
//		System.out.println(goHtxt + "\r\n");
//		System.out.println(goHtxt.split(" ").length);

		//word=use, but useful - so below regex addresses.
		NLP.printListOfString("listClientWordsToHL====", listClientWordsToHL);
		NLP.printListOfString("listgoWordsToHL====", listGoWordsToHL);
		
		//search and replace.("^|[\\( ])+word+($|[,\\);: ]) 	todo: PA: 

		List<List<String>> hlResp = new ArrayList<>();
		hlResp.add(listClientWordsToHL);
		hlResp.add(listGoWordsToHL);
		return hlResp;
	}

	
	public static void main(String[] args) throws JsonProcessingException, IOException, SQLException {
		String clientSent = "(2) in the absence of willful misconduct on its part, such Trustee may conclusively rely, as to the truth of the statements and the correctness of the opinions expressed therein, upon certificates or opinions furnished to such Trustee and conforming to the requirements of this Indenture; but in the case of any such certificates or opinions which by any provisions hereof are specifically required to be furnished to such Trustee, such Trustee shall be under a duty to examine the same to determine whether or not they conform to the requirements of this Indenture (but need not confirm or investigate the accuracy of mathematical calculations or other facts stated therein).";
		String goSent = "(2) in the absence of bad faith on its part, the Trustee may conclusively rely, as to the truth of the statements and the correctness of the opinions expressed therein, upon certificates or opinions furnished to the Trustee and conforming to the requirements of this Indenture. However, in the case of any such certificates or opinions which by any provision hereof are specifically required to be furnished to the Trustee, the Trustee shall examine the certificates and opinions to determine whether or not they conform to the requirements of this Indenture (but need not confirm or investigate the accuracy of mathematical calculations or other facts stated therein).";		
		Simple_HL_Mapped_Only.simpleHL(clientSent, goSent);
	}
	
	
}
