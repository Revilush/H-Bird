package algo_testers.search_dependencies;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;


public class MLApi extends BaseMLApi {
	
	private String USE_Similarity_EndPoint = "/api/use/similarity/";
	
	private String Find_Synonym_EndPoint = "/api/synonyms/";
	
	
	/**
	 * ML server url: ie. http://127.0.0.1:8000
	 * @param serverBaseUrl
	 */
	public MLApi(String serverBaseUrl) {
		super(serverBaseUrl);
	}


	
	/**
	 * Default api end-point is:  "/api/use/similarity/"
	 * 
	 * @param request
	 * @return the result returned by the API
	 * @throws IOException
	 */
	public String getUSESimilarity(USESimilarityRequest request) throws IOException {
		return getUSESimilarity(request, USE_Similarity_EndPoint);
	}
	public String getUSESimilarity(USESimilarityRequest request, String apiEndpoint) throws IOException {		//USESimilarityResponse
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		getUSESimilarity(request, apiEndpoint, null, os);
		return new String(os.toByteArray());
	}
	/**
	 * Calls the API, and writes the returned results to the file provided.
	 * @param request
	 * @param writeResponseToFile
	 * @throws IOException
	 */
	public void getUSESimilarity(USESimilarityRequest request, File writeResponseToFile) throws IOException {
		getUSESimilarity(request, USE_Similarity_EndPoint, new FileOutputStream(writeResponseToFile), null);
	}
	/**
	 * Calls the API, and writes the returned results to both - the file and the stream provided. You can provide null for any of the file/stream.
	 * @param request
	 * @param writeResponseToFile
	 * @param writeResponseToStream
	 * @throws IOException
	 */
	public void getUSESimilarity(USESimilarityRequest request, File writeResponseToFile, OutputStream writeResponseToStream) throws IOException {
		getUSESimilarity(request, USE_Similarity_EndPoint, new FileOutputStream(writeResponseToFile), writeResponseToStream);
	}
	/**
	 * Calls the similarity api endpoint and writes returned output to a file and another stream. You can provide null for any stream if writing to multiple streams is not required.
	 * @param request
	 * @param apiEndpoint
	 * @param writeResponseToFile
	 * @param writeResponseToStream
	 * @throws IOException
	 */
	public void getUSESimilarity(USESimilarityRequest request, String apiEndpoint, FileOutputStream writeResponseToFile, OutputStream writeResponseToStream) throws IOException {		//USESimilarityResponse
		postJsonAndWriteOutput(getAbsoluteApiUrl(apiEndpoint), request, apiEndpoint, writeResponseToFile, writeResponseToStream);
		/*
		// if no need to write anywhere, why to call the api?
		if (null == writeResponseToFile  &&  null == writeResponseToStream)
			return;
		String url = getApiUrl(apiEndpoint);
		OutputStream os1 = (null != writeResponseToFile)? writeResponseToFile: NullOutputStream.NULL_OUTPUT_STREAM;
		OutputStream os2 = (null != writeResponseToStream)? writeResponseToStream: NullOutputStream.NULL_OUTPUT_STREAM;
		TeeOutputStream tos = null;
		BufferedOutputStream bos = null;
		try {
			tos = new TeeOutputStream(os1, os2);
			bos = new BufferedOutputStream(tos);
			postJsonAndWriteOutput(url, request, bos);
		} finally {
			if (null != bos)
				bos.close();
			if (null != tos)
				tos.close();
		}
		*/
	}

	
	
	public String findSynonyms(SynonymApiRequest request) throws IOException {
		return findSynonyms(request, Find_Synonym_EndPoint);
	}
	public String findSynonyms(SynonymApiRequest request, String apiEndpoint) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		postJsonAndWriteOutput(getAbsoluteApiUrl(Find_Synonym_EndPoint), request, apiEndpoint, null, os);
		return new String(os.toByteArray());
	}
	
	
	/*
	private static final String Similarity_EndPoint = "use_find_similarity/";
	private static final String Use_Train_EndPoint = "use_train/";
	private static final String Use_Test_EndPoint = "use_test/";
	private static final String doc2Vec_Train_EndPoint = "doc2vec_train/";
	private static final String doc2Vec_Test_EndPoint = "doc2vec_test/";
	*/
	/*
	public String findSimilarity(String inputJson) throws IOException {
		String url = serverBaseUrl + Similarity_EndPoint;
		return postJson(url, inputJson);
	}
	public String findSimilarity(Map<String, ?> testData) throws IOException {
		return findSimilarity(JSonUtils.object2JsonString(testData));
	}
	
	public String trainClause_USE(String trainingJson) throws IOException {
		String url = serverBaseUrl + Use_Train_EndPoint;
		return postJson(url, trainingJson);
	}
	public String trainClause_USE(Map<String, ?> testData) throws IOException {
		return trainClause_USE(JSonUtils.object2JsonString(testData));
	}
	
	public String testClause_USE(String trainingJson) throws IOException {
		String url = serverBaseUrl + Use_Test_EndPoint;
		return postJson(url, trainingJson);
	}
	public String testClause_USE(Map<String, ?> testData) throws IOException {
		return testClause_USE(JSonUtils.object2JsonString(testData));
	}
	
//	/ **
//	 * Training a Doc2Vec model. Look at "Doc2Vec_PrudentPerson_Train.json" for input format.
//	 * @param trainingJson Json containing training data (fieldName: 'train_data') and output-filename for trained model (fieldName: 'trained_model').
//	 * @return
//	 * @throws IOException
//	 * /
//	/ *
	public String trainClause_Doc2Vec(String trainingJson) throws IOException {
		String url = serverBaseUrl + doc2Vec_Train_EndPoint;
		return postJson(url, trainingJson);
	}
	public String trainClause_Doc2Vec(Map<String, ?> trainingData) throws IOException {
		return trainClause_Doc2Vec(JSonUtils.object2JsonString(trainingData));
	}
	
	public String testClause_Doc2Vec(String testJson) throws IOException {
		String url = serverBaseUrl + doc2Vec_Test_EndPoint;
		return postJson(url, testJson);
	}
	public String testClause_Doc2Vec(Map<String, ?> testData) throws IOException {
		return testClause_Doc2Vec(JSonUtils.object2JsonString(testData));
	}
	*/
	

	// ********************************************* //
	
	
	// ********************************************* //
	
	public static void main(String[] arg) throws IOException {
		//SpringContext.loadApplicationContext();
		MLApi api = new MLApi("http://127.0.0.1:8000");
		
		/*
		 * USESimilarityRequest request = new USESimilarityRequest();
		 * 
		 * String sents =
		 * FileSystemUtils.readTextFromFile("c:/temp/similarity-test.json"); long millis
		 * = System.currentTimeMillis(); System.out.println("calling Sim API:" + new
		 * Date()); Object sentObj = JSonUtils.json2Object(sents);
		 * 
		 * request.setList1(sentObj);
		 * 
		 * String resp = api.getUSESimilarity(request);
		 * 
		 * 
		 * System.out.println(System.currentTimeMillis() - millis);
		 * USESimilarityResponse response = JSonUtils.json2Object(resp,
		 * USESimilarityResponse.class); //System.out.println(resp);
		 * System.out.println(JSonUtils.object2JsonString(response));
		 */
		
		//**********************************************************//
		
		File reqFile = new File("E:\\vihnu_work\\synonms_request.txt");
		 //readInputFileContents(reqFile);
		 String fileContent = FileSystemUtils.readTextFromFile(reqFile);
		
		
		
		SynonymApiRequest synoymRequest = JSonUtils.json2Object(fileContent, SynonymApiRequest.class);		//new SynonymApiRequest();
		
		//TODO: Set input requet values
		
		String synonmResp = api.findSynonyms(synoymRequest);
		//System.out.println(System.currentTimeMillis() - millis);
		//SynonymApiResponse synonmResponse = JSonUtils.json2Object(synonmResp, SynonymApiResponse.class);
		
		//System.out.println(JSonUtils.object2JsonString(synonmResponse));
	}
	
	
	
}
