package contracts;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

public class PosTagger {

	public PosTagger() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws IOException, SQLException {

		InputStream modelIn = null;

		try {
			modelIn = new FileInputStream(
					"c:/getContracts/en-pos-maxent.bin");
			POSModel model = new POSModel(modelIn);

			POSTaggerME tagger = new POSTaggerME(model);

			String sentence = "It could be a donkey horse or elephant.";
			
			String sent[] = sentence.split(" ");
			String tags[] = tagger.tag(sent);

			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < sent.length; i++) {
				sb.append(sent[i] + "-{" + tags[i] + "} ");
			}
			System.out.println(sb.toString());
		} catch (IOException e) {
			// Model loading failed, handle the error
			e.printStackTrace();
		} finally {
			if (modelIn != null) {
				try {
					modelIn.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
