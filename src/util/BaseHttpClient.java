package util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.SerializableEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;


public abstract class BaseHttpClient {

	protected static final Log log = LogFactory.getLog(BaseHttpClient.class);
	
	public Charset defaultCharset = StandardCharsets.UTF_8;
	protected String defaultContentType = "text/html";
	
	protected String userAgentString = "";
	protected Map<String, String> requestHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
	// headers that live only for the next request
	protected Map<String, String> requestScopedRequestHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
	
	protected boolean closeConnectionAfterRequest = true, compressionEnabled = true;
	protected CloseableHttpClient httpClient = null;
	protected CloseableHttpResponse response = null;
	protected HttpClientContext httpContext = HttpClientContext.create();
	
	protected ProtocolVersion protocolVersion = HttpVersion.HTTP_1_1;
	protected boolean disableChunking = false;

	
	public BaseHttpClient(boolean closeConnectionAfterRequest) {
		this(closeConnectionAfterRequest, false);
	}
	public BaseHttpClient(boolean closeConnectionAfterRequest, boolean compressionEnabled) {
		this.compressionEnabled = compressionEnabled;
		this.closeConnectionAfterRequest = closeConnectionAfterRequest;
		httpClient = getNewApacheHttpClient();
		httpContext.setCookieStore(new BasicCookieStore());

	}

	public void writeBytesToStream(String url, OutputStream os) throws IOException {
		makeHttpRequest("Get", url, null, -1, null, null, os);
	}

	public byte[] fetchBytes(String url) throws IOException {
		return makeHttpRequest("Get", url, null, -1, null, null);
	}
	
	public String fetchHtml(String url) throws IOException {
		return fetchHtml(url, defaultCharset);
	}
	public String fetchHtml(String url, Charset charset) throws IOException {
		byte[] bytes = fetchBytes(url);
		setReferer(url);
		if (null != bytes)
			return new String(bytes, charset);
		return null;
	}
	
	
	public String postForm(String url, Map<String, ? extends Object> postNameValuePairs) throws IOException {
		return postForm(url, postNameValuePairs, defaultCharset); 
	}
	public String postForm(String url, Map<String, ? extends Object> postNameValuePairs, Charset charset) throws IOException {
		byte[] bytes = makeHttpRequest("Post", url, null, -1, null, postNameValuePairs);
		setReferer(url);
		if (null != bytes)
			return new String(bytes, charset);
		return null;
	}
	

	public String postContents(String url, String contents) throws IOException {
		return postContents(url, contents, defaultCharset); 
	}
	public String postContents(String url, String contents, Charset charset) throws IOException {
		return postContents(url, contents, charset, null);
	}
	public String postContents(String url, String contents, Charset charset, OutputStream os) throws IOException {
		return postContents(url, contents, defaultContentType, charset, os);
	}
	public String postContents(String url, String contents, String contentType) throws IOException {
		return postContents(url, contents, contentType, null); 
	}
	public String postContents(String url, String contents, String contentType, OutputStream os) throws IOException {
		return postContents(url, contents, contentType, defaultCharset, os); 
	}
	public String postContents(String url, String contents, String contentType, Charset charset, OutputStream os) throws IOException {
		/*
		InputStream is = null;
		if (null != contents)
			is = new ByteArrayInputStream(contents.getBytes());
		*/
		if (null == os) {
			byte[] bytes = makeHttpRequest("Post", url, contents, (null!=contents)?contents.length():-1, contentType, null);
			if (null != bytes)
				return new String(bytes, charset);
		}
		// write results to outputStream
		makeHttpRequest("Post", url, contents, (null!=contents)?contents.length():-1, contentType, null, os);
		return null;
	}
	
	
	
	
	
	
	
	
	
	

	public void makeHttpRequest(String methodType, String url, Object contents, long contentLength, String contentType, Map<String, ? extends Object> postNameValuePairs
										, OutputStream writeTo) throws IOException {
		if (null == url)
			throw new RuntimeException("URL can't be null..");
		HttpRequestBase httpRequest = getMethod(methodType, url, contents, contentLength, contentType, postNameValuePairs);
		try {
			preFireHttpRequest();
			// Execute the httpRequest.
			makeHttpRequest(httpRequest, httpContext, writeTo);
		} catch (Exception ex) {
			String exMsg = ex.getMessage();
			log.warn(exMsg+", url:"+url);
			// notify error listeners, if any
			//notifyErrorListeners(ex);
			
			// In case of an unexpected exception you may want to abort the HTTP request in order to shut down the underlying connection immediately.
			httpRequest.abort();
			throw ex;
		} finally {
			if (null != response  &&  null != response.getEntity()) {
				EntityUtils.consumeQuietly(response.getEntity());
			}
			// Release the connection, if required.
			if (closeConnectionAfterRequest) {
				httpRequest.releaseConnection();
				releaseResources();
			}
		}
	}


	public byte[] makeHttpRequest(String methodType, String url, Object contents, long contentLength, String contentType, Map<String, ? extends Object> postNameValuePairs) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		makeHttpRequest(methodType, url, contents, contentLength, contentType, postNameValuePairs, bos);
		return bos.toByteArray();
		
		/*
		if (null == url)
			throw new RuntimeException("URL can't be null..");
		HttpRequestBase httpRequest = getMethod(methodType, url, contents, contentLength, contentType, postNameValuePairs);
		byte[] result = null;
		try {
			preFireHttpRequest();
			// Execute the httpRequest.
			result = makeHttpRequest(httpRequest, httpContext);
		} catch (Exception ex) {
			String exMsg = ex.getMessage();
			log.warn(exMsg+", url:"+url);
			// notify error listeners, if any
			//notifyErrorListeners(ex);
			
			// In case of an unexpected exception you may want to abort the HTTP request in order to shut down the underlying connection immediately.
			httpRequest.abort();
			throw ex;
		} finally {
			if (null != response  &&  null != response.getEntity()) {
				EntityUtils.consumeQuietly(response.getEntity());
			}
			// Release the connection, if required.
			if (closeConnectionAfterRequest) {
				httpRequest.releaseConnection();
				releaseResources();
			}
		}
		return result;
		*/
	}
	
	public byte[] makeHttpRequest(HttpRequestBase httpRequest, HttpClientContext httpContext) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		makeHttpRequest(httpRequest, httpContext, bos);
		return bos.toByteArray();
		/*
		try {
			// Execute the httpRequest.
			if (log.isDebugEnabled())
				log.debug("making http request to: " + httpRequest.getRequestLine().getUri());
			response = httpClient.execute(httpRequest, httpContext);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.warn("Status code not OK: '" + response.getStatusLine() + "'   for :" +httpRequest.getURI());
			}
			if (null != response.getEntity()) {
				// Read the response body.
				//InputStream is = response.getEntity().getContent();
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				try {
					response.getEntity().writeTo(bos);
					result = bos.toByteArray();
				} finally {
					bos.close();
					//is.close();
				}
			}
		} finally {
			if (null != response  &&  null != response.getEntity()) {
				EntityUtils.consumeQuietly(response.getEntity());
			}
			// Release the connection, if required.
			if (closeConnectionAfterRequest) {
				httpRequest.releaseConnection();
				releaseResources();
			}
		}
		return result;
		*/
	}
	
	public void makeHttpRequest(HttpRequestBase httpRequest, HttpClientContext httpContext, OutputStream bos) throws IOException {
		try {
			// Execute the httpRequest.
			if (log.isDebugEnabled())
				log.debug("making http request to: " + httpRequest.getRequestLine().getUri());
			response = httpClient.execute(httpRequest, httpContext);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				log.warn("Status code not OK: '" + response.getStatusLine() + "'   for :" +httpRequest.getURI());
			}
			if (null != response.getEntity()) {
				// Read the response body.
				//InputStream is = response.getEntity().getContent();
				try {
					response.getEntity().writeTo(bos);
				} finally {
					bos.close();
				}
			}
		} finally {
			if (null != response  &&  null != response.getEntity()) {
				EntityUtils.consumeQuietly(response.getEntity());
			}
			// Release the connection, if required.
			if (closeConnectionAfterRequest) {
				httpRequest.releaseConnection();
				releaseResources();
			}
		}
	}
	
	
	
	
	public HttpResponse getResponse() {
		return response;
	}

	public String getReferer() {
		return requestHeaders.get(Header_Referer);
	}
	public void setReferer(String url) {
		setRequestHeader(Header_Referer, url);
	}
	public void setRequestHeader(String hdrName, String hdrValue) {
		requestHeaders.put(hdrName, hdrValue);
	}
	
	public void addRequestScopedRequestHeader(String hdrName, String hdrValue) {
		requestScopedRequestHeaders.put(hdrName, hdrValue);
	}

	public Charset getDefaultCharset() {
		return defaultCharset;
	}
	public void setDefaultCharset(Charset defaultCharset) {
		this.defaultCharset = defaultCharset;
	}
	
	public int getResponseCode() {
		if (null != response)
			return response.getStatusLine().getStatusCode();
		return -1;
	}
	
	public String getRedirectUrl() {
		Header hdr = response.getFirstHeader("location");
		if (null != hdr)
			return hdr.getValue();
		return null;
	}
	
	public String getUserAgentString() {
		return userAgentString;
	}
	public void setUserAgentString(String userAgentString) {
		this.userAgentString = userAgentString;
	}
	
	public Map<String, String> getRequestHeaders() {
		return requestHeaders;
	}
	public void setRequestHeaders(Map<String, String> requestHeaders) {
		this.requestHeaders = requestHeaders;
	}

	public HttpClientContext getHttpContext() {
		return httpContext;
	}
	public void setHttpContext(HttpClientContext httpContext) {
		this.httpContext = httpContext;
	}
	
	public void useHttpProtocol_10() {
		protocolVersion = HttpVersion.HTTP_1_0;
	}
	public void useHttpProtocol_11() {
		protocolVersion = HttpVersion.HTTP_1_1;
	}
	public void disableChunking() {
		disableChunking = true;
	}
	
	
	// ------------------------------------------------- //
	
	protected void setRequestHeaders(HttpRequestBase httpRequest, Map<String, String> requestHdrs) {
		for (String hdrName : requestHdrs.keySet())
			httpRequest.setHeader(hdrName, requestHdrs.get(hdrName));
		// set request Scoped Request Headers
		for (String hdrName : requestScopedRequestHeaders.keySet())
			httpRequest.setHeader(hdrName, requestScopedRequestHeaders.get(hdrName));
		// once set in this request, clear these temp headers
		requestScopedRequestHeaders.clear();
	}
	

	protected HttpRequestBase getMethod(String methodType, String url, Object contents, long contentLength,
			String contentType, Map<String, ? extends Object> postNameValuePairs) throws UnsupportedEncodingException {
		// Create a httpRequest instance - default is Get.
		HttpRequestBase httpRequest = new HttpGet(url);
		if (contentType == null)
			contentType = defaultContentType;
		if (null != methodType) {
			if (methodType.equalsIgnoreCase("put")) {
				httpRequest = new HttpPut(url);
			} else if (methodType.equalsIgnoreCase("delete")) {
				httpRequest = new HttpDelete(url);
			} else if (methodType.equalsIgnoreCase("post")) {
				httpRequest = new HttpPost(url);
			}
			AbstractHttpEntity entity = null;
			if (null != contents) {
				org.apache.http.entity.ContentType cType = org.apache.http.entity.ContentType.create(contentType);
				if (contents instanceof InputStream) {
					entity = new InputStreamEntity((InputStream)contents, contentLength, cType);
				} else if (contents instanceof byte[]) {
					entity = new ByteArrayEntity(((byte[])contents), cType);				
				} else if (contents instanceof String) {
					entity = new StringEntity((String)contents, cType);
				} else if (contents instanceof File) {
					entity = new FileEntity((File)contents, cType);
				} else if (contents instanceof Serializable) {
					entity = new SerializableEntity((Serializable)contents);
				}
				((HttpEntityEnclosingRequest) httpRequest).setEntity(entity);
			}
			if (null != postNameValuePairs) {
				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				Set<String> keys = postNameValuePairs.keySet();
				Object val;
				for (String key : keys) {
					val = postNameValuePairs.get(key);
					if (null != val) {
						if (val instanceof String[]) {
							String[] vals = (String[]) val;
							for (String v : vals)
								nvps.add(new BasicNameValuePair(key, v));
						} else {
							nvps.add(new BasicNameValuePair(key, postNameValuePairs.get(key).toString()));
						}
					} else {
						nvps.add(new BasicNameValuePair(key, null));
					}
				}
				entity = new UrlEncodedFormEntity(nvps);
				((HttpPost) httpRequest).setEntity(entity);
			}
			if (null != entity  &&  contentLength > 0)
				entity.setChunked(!disableChunking);
		}
		if (StringUtils.isNotBlank(userAgentString))
			httpRequest.setHeader(Header_UserAgent, userAgentString); // ApiConfig.getInstance().getRandomUserAgent()

		// set requestHeaders..
		setRequestHeaders(httpRequest, requestHeaders);

		// identifying chunking resulted in lots of chunked-header/size etc
		// related exceptions.
		// a mis-behaving server may indicate the response is chunked but send
		// plain text etc.. A well-behaved server should not send chunked
		// response for http-1.0
		httpRequest.setProtocolVersion(protocolVersion);

		return httpRequest;
	}
	
	//// ********************************************************** ////
	
	/**
	 * A no-op method, called by 'getNewApacheHttpClient()' before building httpClient. This can be overridden by sub-classes and set custom http-client parts like retryHandler/redirectStrategy... etc. 
	 */
	protected void beforeBuildingHttpClient() {
	}
	/**
	 * A no-op method, fired just before making the http-request (original call made by external callers, not by some internal handler etc).
	 * sub-classes can set request-specific handlers etc
	 */
	protected void preFireHttpRequest() {
	}
	
	/**
     * Configure HttpClient with set of defaults.
     * 
     */
	public CloseableHttpClient getNewApacheHttpClient() {
		beforeBuildingHttpClient();
		
    	SSLContext sslcontext = SSLContexts.createSystemDefault();
    	SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
    	        sslcontext, new String[] {"TLSv1.2", "TLSv1.1", "TLSv1", "SSLv3" }, null,
    	        SSLConnectionSocketFactory.getDefaultHostnameVerifier());
    	Registry<ConnectionSocketFactory> sockConnRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
    	        .register("http", PlainConnectionSocketFactory.INSTANCE)
    	        .register("https", sslConnectionSocketFactory)
    	        .build();
    	
    	PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(sockConnRegistry);
    	// Increase max total connection
    	connMgr.setMaxTotal(100);
    	// Increase default max connection per route to 10
    	connMgr.setDefaultMaxPerRoute(10);
    	
    	// request configs: connection/socket timeouts, cookie mgmt etc
		RequestConfig globalRequestConfig = RequestConfig.custom()
				.setSocketTimeout(120000)
				.setConnectTimeout(120000)
				.setConnectionRequestTimeout(120000)
				//Cookie policy can be set at the HTTP client and overridden on the HTTP request-level or context-level if required.
				.setCookieSpec(CookieSpecs.STANDARD)
				.setContentCompressionEnabled(compressionEnabled)			// or use request-header: Accept-Encoding
				.build();
		
		CloseableHttpClient httpClient = HttpClientBuilder.create()
	    	.setConnectionManager(connMgr)
	    	//.setRoutePlanner(routePlanner)
	    	.setDefaultRequestConfig(globalRequestConfig)
	    	//.setDefaultCookieStore(cookieStore)
	        //Redirects: as per Http specs: a redirection on Post must require user intervention thus can't be handled automatically..
	        // however, the LaxStrategy seems to be taking care of redirects on "POST" as well..
	    	.setRedirectStrategy(new LaxRedirectStrategy())
	    	.setUserAgent(userAgentString)
	    	//.setDefaultCredentialsProvider(credentialsProvider)
	    	//.setRetryHandler(retryHandler)
	    	.build();
        
        return httpClient;
    }
	
	
	public void releaseResources() {
		if (null != httpClient) {
			try {
				httpClient.close();
			} catch (IOException e) {
				log.warn("", e);
			}
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		releaseResources();
		super.finalize();
	}
	

	public static String Header_Referer = "Referer";
	public static String Header_UserAgent = "User-Agent";
	public static String Header_Cookie = "Cookie";
	public static String Header_Origin = "Origin";
	public static String Header_Host = "Host";
	public static String Header_DoNotTrack = "DNT";
	
	
	
}
