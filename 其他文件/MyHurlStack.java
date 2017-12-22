/**
 * 
 */
package com.huawei.fans.myVolley;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.toolbox.HurlStack;
import com.huawei.fans.fanscommon.FansLog;
import com.huawei.fans.fanscommon.LogUtils;
import com.huawei.fans.myVolley.antiattack.AntiAttackAuthError;
import com.huawei.fans.myVolley.antiattack.AntiAttackAuthPolicy;
import com.huawei.fans.myVolley.multipart.MultipartRequest;

/**
 * @author d00214274
 * 
 */
public class MyHurlStack extends HurlStack {
	// private static final String HEADER_CONTENT_TYPE = "Content-Type";
	// private Context mContext;
	private final UrlRewriter mSubUrlRewriter;
	private final SSLSocketFactory mSubSslSocketFactory;

	/**
	 * The default anti acttack auth policy for all request.
	 */
	private AntiAttackAuthPolicy mDefaultAuthPolicy;

	public MyHurlStack() {
		// TODO Auto-generated constructor stub
		super(null);
		mSubUrlRewriter = null;
		mSubSslSocketFactory = null;
	}

	/**
	 * @param urlRewriter
	 *            Rewriter to use for request URLs
	 */
	public MyHurlStack(UrlRewriter urlRewriter) {
		super(urlRewriter, null);
		// this.mContext = context;
		mSubUrlRewriter = urlRewriter;
		mSubSslSocketFactory = null;
	}

	/**
	 * @param urlRewriter
	 *            Rewriter to use for request URLs
	 * @param sslSocketFactory
	 *            SSL factory to use for HTTPS connections
	 */
	public MyHurlStack(UrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory) {
		super(urlRewriter, sslSocketFactory);
		mSubUrlRewriter = urlRewriter;
		mSubSslSocketFactory = sslSocketFactory;
		// this.mContext = context;
	}

	@Override
	protected HttpURLConnection createConnection(URL url) throws IOException {
		HttpURLConnection connection = super.createConnection(url);

		return connection;
	}

	@Override
	public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders)
			throws IOException, AuthFailureError {
		int authRetryCount = 0;
		while (true) {
			HttpResponse response = null;
			AntiAttackAuthPolicy policy = getRequestAuthPolicy(request);
			if (policy != null) {
				Map<String, String> authHeaders = policy.onObtainAuthHeaders(request);
				if (authHeaders != null && !authHeaders.isEmpty()) {
					if (additionalHeaders == null) {
						additionalHeaders = new HashMap<String, String>();
					}
					additionalHeaders.putAll(authHeaders);
				}
			}
			if (request instanceof MultipartRequest) {
				response = performMultipartRequest((MultipartRequest) request, additionalHeaders);
			} else {
				response = super.performRequest(request, additionalHeaders);
			}
			// add anti attack auth headers
			if (policy != null && authRetryCount <= 1) {
				try {
					policy.authRequest(request, convertHeaders(response.getAllHeaders()));
				} catch (AntiAttackAuthError error) {
					if (error.isNeedRetry()) {
						authRetryCount++;
						continue;
					}
				}
			}

			return response;
		}
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString();
	}

	public HttpResponse performMultipartRequest(MultipartRequest request, Map<String, String> additionalHeaders)
			throws IOException, AuthFailureError {
		FansLog.v("performMultipartRequest begin");
		String url = request.getUrl();
		HashMap<String, String> map = new HashMap<String, String>();
		map.putAll(request.getHeaders());
		map.putAll(additionalHeaders);
		if (mSubUrlRewriter != null) {
			String rewritten = mSubUrlRewriter.rewriteUrl(url);
			if (rewritten == null) {
				throw new IOException("URL blocked by rewriter: " + url);
			}
			url = rewritten;
		}
		URL parsedUrl = new URL(url);
		HttpURLConnection connection = createConnection(parsedUrl);

		int timeoutMs = request.getTimeoutMs();
		connection.setConnectTimeout(timeoutMs);
		connection.setReadTimeout(timeoutMs);
		connection.setUseCaches(false);
		connection.setDoInput(true);

		// use caller-provided custom SslSocketFactory, if any, for HTTPS
		if ("https".equals(parsedUrl.getProtocol()) && mSubSslSocketFactory != null) {
			((HttpsURLConnection) connection).setSSLSocketFactory(mSubSslSocketFactory);
		}

		// for (String headerName : map.keySet()) {
		// connection.addRequestProperty(headerName, map.get(headerName));
		// }
		for (Entry<String, String> e : map.entrySet()) {
			connection.addRequestProperty(e.getKey(), e.getValue());
		}

		setConnectionParametersForRequest(connection, request);
		// Initialize HttpResponse with data from the HttpURLConnection.
		ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
		int responseCode = connection.getResponseCode();
		if (responseCode == -1) {
			// -1 is returned by getResponseCode() if the response code could
			// not be retrieved.
			// Signal to the caller that something was wrong with the
			// connection.
			throw new IOException("Could not retrieve response code from HttpUrlConnection.");
		}
		StatusLine responseStatus = new BasicStatusLine(protocolVersion, connection.getResponseCode(),
				connection.getResponseMessage());
		BasicHttpResponse response = new BasicHttpResponse(responseStatus);
		response.setEntity(entityFromConnection(connection));
		for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
			if (header.getKey() != null) {
				String key = header.getKey();
				List<String> values = header.getValue();
				if (key.equalsIgnoreCase("set-cookie")) {
					StringBuilder cookieString = new StringBuilder();
					for (String value : values) {
						cookieString.append(value).append("\n");// 用\n作为分隔符，cookie中不应该有回车符号
					}
					cookieString.deleteCharAt(cookieString.length() - 1);
					Header h = new BasicHeader(header.getKey(), cookieString.toString());
					response.addHeader(h);
				} else {
					Header h = new BasicHeader(header.getKey(), values.get(0));
					response.addHeader(h);
				}
			}
		}
		FansLog.v("performMultipartRequest end");
		return response;
	}

	@SuppressWarnings("deprecation")
	/* package */static void setConnectionParametersForRequest(HttpURLConnection connection, MultipartRequest request)
			throws IOException, AuthFailureError {
		FansLog.v("setConnectionParametersForRequest begin");
		switch (request.getMethod()) {
		case Method.DEPRECATED_GET_OR_POST:
			// This is the deprecated way that needs to be handled for backwards
			// compatibility.
			// If the request's post body is null, then the assumption is that
			// the request is
			// GET. Otherwise, it is assumed that the request is a POST.
			byte[] postBody = request.getPostBody();
			if (postBody != null) {
				// Prepare output. There is no need to set Content-Length
				// explicitly,
				// since this is handled by HttpURLConnection using the size of
				// the prepared
				// output stream.
				connection.setRequestMethod("POST");
				addBodyIfExists(connection, request);
			}
			break;
		case Method.GET:
			// Not necessary to set the request method because connection
			// defaults to GET but
			// being explicit here.
			connection.setRequestMethod("GET");
			break;
		case Method.DELETE:
			connection.setRequestMethod("DELETE");
			break;
		case Method.POST:
			connection.setRequestMethod("POST");
			addBodyIfExists(connection, request);
			break;
		case Method.PUT:
			connection.setRequestMethod("PUT");
			addBodyIfExists(connection, request);
			break;
		case Method.HEAD:
			connection.setRequestMethod("HEAD");
			break;
		case Method.OPTIONS:
			connection.setRequestMethod("OPTIONS");
			break;
		case Method.TRACE:
			connection.setRequestMethod("TRACE");
			break;
		case Method.PATCH:
			addBodyIfExists(connection, request);
			connection.setRequestMethod("PATCH");
			break;
		default:
			throw new IllegalStateException("Unknown method type.");
		}

		FansLog.v("setConnectionParametersForRequest end");
	}

	private static void addBodyIfExists(HttpURLConnection connection, MultipartRequest request)
			throws IOException, AuthFailureError {
		FansLog.v("addBodyIfExists begin");
		connection.setDoOutput(true);
		OutputStream out = null;
		try {
			out = connection.getOutputStream();
			request.writePostBody(out);
		} finally {
			if (out != null)
				out.close();
		}

		FansLog.v("addBodyIfExists end");
	}

	/**
	 * Initializes an {@link HttpEntity} from the given
	 * {@link HttpURLConnection}.
	 * 
	 * @param connection
	 * @return an HttpEntity populated with data from <code>connection</code>.
	 */
	private static HttpEntity entityFromConnection(HttpURLConnection connection) {
		BasicHttpEntity entity = new BasicHttpEntity();
		InputStream inputStream;
		try {
			inputStream = connection.getInputStream();
		} catch (IOException ioe) {
			inputStream = connection.getErrorStream();
		}
		entity.setContent(inputStream);
		entity.setContentLength(connection.getContentLength());
		entity.setContentEncoding(connection.getContentEncoding());
		entity.setContentType(connection.getContentType());
		return entity;
	}

	/**
	 * 
	 * 获取默认的防攻击鉴权策略
	 * 
	 * @return 默认的防攻击鉴权策略
	 */
	public AntiAttackAuthPolicy getDefaultAuthPolicy() {
		return mDefaultAuthPolicy;
	}

	/**
	 * 
	 * 设置默认的防攻击鉴权策略
	 * 
	 * @param mDefaultAuthPolicy
	 *            默认的防攻击鉴权策略
	 */
	public void setDefaultAuthPolicy(AntiAttackAuthPolicy mDefaultAuthPolicy) {
		this.mDefaultAuthPolicy = mDefaultAuthPolicy;
	}

	/**
	 * 
	 * 获取request对应的防攻击鉴权策略
	 * 
	 * @param request
	 *            volley请求
	 * @return 若request本身实现了AntiAttackAuthPolicy接口,返回自身;否则,返回默认的防攻击鉴权策略
	 */
	private AntiAttackAuthPolicy getRequestAuthPolicy(Request<?> request) {
		if (request instanceof AntiAttackAuthPolicy) {
			return (AntiAttackAuthPolicy) request;
		} else {
			return getDefaultAuthPolicy();
		}
	}

	/**
	 * Converts Headers[] to Map<String, String>.
	 */
	private static Map<String, String> convertHeaders(Header[] headers) {
		Map<String, String> result = new HashMap<String, String>();
		for (int i = 0; i < headers.length; i++) {
			result.put(headers[i].getName(), headers[i].getValue());
		}
		return result;
	}
}
