package xbrl;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestRegex {

	public static void main(String[] arg) throws IOException {
		
		String tmp = "<TD >686</TD><TD >.5</TD>";
//		String tmp = Utils.readTextFromFile("c:/temp2/t.htm");
		
		Pattern patternTmp = Pattern.compile("(?ism)<TD.*\\$?[\\d]{1,15}[^</TD]</TD><TD.*\\$?\\.\\d\\d?<");
		Matcher matchTmp = patternTmp.matcher(tmp);

		while (matchTmp.find()) {
			System.out.println(matchTmp.group());
		}
	}
}
