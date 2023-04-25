package algo_testers.search_dependencies;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import algo_testers.search_dependencies.BaseApiRequest;
import algo_testers.search_dependencies.JSonUtils;
import algo_testers.search_dependencies.LegalHttpClient;

public abstract class BaseMLApi {
	
	protected final String ContentType_Json = "application/json";

	protected static Log log = LogFactory.getLog(BaseMLApi.class);
	
	
	protected String serverBaseUrl;
	protected LegalHttpClient httpClient = new LegalHttpClient(false);
	
	protected BaseMLApi(String serverBaseUrl) {
		this.serverBaseUrl = serverBaseUrl;
		httpClient.useHttpProtocol_10();
		httpClient.disableChunking();
	}
	
	// *********************** //
	protected String postJson(String url, String inputJson) throws IOException {
		return httpClient.postContents(url, inputJson, ContentType_Json);
	}
	protected String postJson(String url, BaseApiRequest request) throws IOException {
		return postJson(url, JSonUtils.object2JsonString(request));
	}
	protected void postJsonAndWriteOutput(String url, BaseApiRequest request, OutputStream os) throws IOException {
		httpClient.postContents(url, JSonUtils.object2JsonString(request), ContentType_Json, os);
	}
	
	protected String getAbsoluteApiUrl(String relativeApiEndPoint) {
		return StringUtils.removeEnd(serverBaseUrl, "/") + relativeApiEndPoint;
	}

	
	protected void postJsonAndWriteOutput(String endPointUrl, BaseApiRequest request, String apiEndpoint, FileOutputStream writeResponseToFile, OutputStream writeResponseToStream) throws IOException {		//USESimilarityResponse
		// if no need to write anywhere, why to call the api?
		if (null == writeResponseToFile  &&  null == writeResponseToStream)
			return;
		OutputStream os1 = (null != writeResponseToFile)? writeResponseToFile: NullOutputStream.NULL_OUTPUT_STREAM;
		OutputStream os2 = (null != writeResponseToStream)? writeResponseToStream: NullOutputStream.NULL_OUTPUT_STREAM;
		TeeOutputStream tos = null;
		BufferedOutputStream bos = null;
		try {
			tos = new TeeOutputStream(os1, os2);
			bos = new BufferedOutputStream(tos, 8096);
			postJsonAndWriteOutput(endPointUrl, request, bos);
		} finally {
			if (null != bos)
				bos.close();
			if (null != tos)
				tos.close();
		}
	}
	
	
	// *********************** //
	
	@Override
	protected void finalize() {
		if (null != httpClient) {
			httpClient.releaseResources();
			httpClient = null;
		}
	}
	
}
