package xbrl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class Word {

	public Word() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		String text = Utils.readTextFromFile("c:/backup/data.adv").replaceAll(" ", "\t");
		PrintWriter pt = new PrintWriter(new File ("c:/backup/data_adv.txt"));
		String [] lines = text.split("\r\n");
//		lines.length;
		for(int i=0; i<lines.length;  i++){
			String [] word = lines[i].split("\t");
			System.out.println();			
			pt.append(word[4]+"\r\n");
		}
		pt.close();

	}

}
