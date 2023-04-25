	package xbrl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.util.EntityUtils;

public class EasyHttpClient {
	
	public static final String LineSeparator = System.getProperty("line.separator", "\n");

	private boolean closeConnectionAfterRequest = true;
	HttpClient httpClient = null;
	HttpResponse response = null;
	
	protected ProtocolVersion protocolVersion = HttpVersion.HTTP_1_1;
	protected boolean disableChunking = false;
	
	
	public EasyHttpClient(boolean closeConnectionAfterRequest) {
		this.closeConnectionAfterRequest = closeConnectionAfterRequest;
		httpClient = getNewApacheHttpClient();
	}

	public HttpClient getApacheHttpClient() {
		return httpClient;
	}
	
	public int getResponseCode() {
		if (null != response)
			return response.getStatusLine().getStatusCode();
		return -1;
	}
	
	
	public void disableChunking() {
		disableChunking = true;
	}
	public void useHttpProtocol_10() {
		protocolVersion = HttpVersion.HTTP_1_0;
	}
	public void useHttpProtocol_11() {
		protocolVersion = HttpVersion.HTTP_1_1;
	}

	
	
	/**
	 * call: ("localhost", 8080, "http", "user", "pswd");
	 * @param proxyHost
	 * @param proxyPort
	 * @param proxyProtocol
	 * @param proxyUserName - can be null if not required
	 * @param proxyPassword - can be null if not required
	 */
	public void setProxy(String proxyHost, int proxyPort, String proxyProtocol, String proxyUserName, String proxyPassword) {
		HttpHost proxy = new HttpHost(proxyHost, proxyPort, proxyProtocol);
        httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        if (null != proxyUserName)
        	((AbstractHttpClient) httpClient).getCredentialsProvider().setCredentials(
        			new AuthScope(proxyHost, proxyPort), new UsernamePasswordCredentials(proxyUserName, proxyPassword) );
	}
	
	public void setClientAuth(String targetServer, int serverPort, String userName, String password) {
		((AbstractHttpClient) httpClient).getCredentialsProvider().setCredentials( new AuthScope(targetServer, serverPort), new UsernamePasswordCredentials(userName, password) );
	}
	
	public String makeHttpRequest(String methodType, String url, InputStream contents, long contentLength, String contentType, Map<String, String> postNameValuePairs) throws IOException {
		//set InputStream==null then pass map.
		
		if (null == url)
			throw new RuntimeException("URL can't be null..");
		// Create a httpRequest instance - default is Get.
//		System.out.println("at method - makeHttpRequest - can print url here");
		// System.out.println(url);
		HttpRequestBase httpRequest = new HttpGet(url);
		if(contentType == null)
			contentType = "application/text";
		AbstractHttpEntity entity = null;
		if (null != methodType) {
			if (methodType.equalsIgnoreCase("put")) {
				httpRequest = new HttpPut(url);
				if (null != contents) {
					entity = new InputStreamEntity(contents, contentLength, org.apache.http.entity.ContentType.create(contentType));
					((HttpPut)httpRequest).setEntity(entity);
				}
			}
			else if (methodType.equalsIgnoreCase("post")) {
				httpRequest = new HttpPost(url);
				if (null != contents) {
					entity = new InputStreamEntity(contents, contentLength, org.apache.http.entity.ContentType.create(contentType));
					((HttpPost)httpRequest).setEntity(entity);
				}
				if (null != postNameValuePairs) {
					List <NameValuePair> nvps = new ArrayList <NameValuePair>();
					Set<String> keys = postNameValuePairs.keySet();
					for (String key : keys)
						nvps.add(new BasicNameValuePair(key, postNameValuePairs.get(key)));
					entity = new UrlEncodedFormEntity(nvps);
					((HttpPost)httpRequest).setEntity(entity );
				}
			}
		}
		if (null != entity)
			entity.setChunked(!disableChunking);
		httpRequest.setProtocolVersion(protocolVersion);
		
		StringBuilder resultStr = new StringBuilder();
		try {
			// Execute the httpRequest.	
			System.out.println("response = httpClient.execute(httpRequest)");
			response = httpClient.execute(httpRequest);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				System.out.println("Status code not OK for :" +url+ "   Status="+ response.getStatusLine());
			}
			if (null != response.getEntity()) {
				// Read the response body.
				BufferedReader brdr = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				String line = null;
				while( (line = brdr.readLine()) != null)
					resultStr.append(line).append(LineSeparator);
				// ensure response is fully consumed
				EntityUtils.consume(response.getEntity());
			}
		} catch (IOException e) {
			// In case of an IOException the connection will be released back to the connection manager automatically
			throw e;
		} catch (RuntimeException ex) {
			// In case of an unexpected exception you may want to abort the HTTP request in order to shut down the underlying connection immediately.
			httpRequest.abort();
			throw ex;
		} finally {
			// Release the connection, if required.
			if (closeConnectionAfterRequest) {
				httpRequest.releaseConnection();
				httpClient.getConnectionManager().shutdown();
			}
		}
		
//		System.out.println("resultStr.toString()=" + resultStr.toString());

		return resultStr.toString();
	}
	
	public void releaseConnection() {
		if (null != httpClient) {
			httpClient.getConnectionManager().shutdown();
		}
	}
	
    /**
     * Configure HttpClient with set of defaults.
     * 
     */
    public HttpClient getNewApacheHttpClient() {
    	HttpParams params = new SyncBasicHttpParams();
    	HttpClientParams.setConnectionManagerTimeout(params, 60000);
        HttpClient httpClient = new SystemDefaultHttpClient(params);
        return httpClient;
    }
	
	protected void finalize() {
		releaseConnection();
	}
}
