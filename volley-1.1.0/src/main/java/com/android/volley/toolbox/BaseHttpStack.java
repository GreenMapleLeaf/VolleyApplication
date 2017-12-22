/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.volley.toolbox;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Header;
import com.android.volley.Request;

import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An HTTP stack abstraction.
 */
@SuppressWarnings("deprecation") // for HttpStack
public abstract class BaseHttpStack implements HttpStack {

    /**
     * Performs an HTTP request with the given parameters.
     * <p>
     * <p>A GET request is sent if request.getPostBody() == null. A POST request is sent otherwise,
     * and the Content-Type header is set to request.getPostBodyContentType().
     *
     * @param request           the request to perform
     * @param additionalHeaders additional headers to be sent together with
     *                          {@link Request#getHeaders()}
     * @return the {@link HttpResponse}
     * @throws SocketTimeoutException if the request times out
     * @throws IOException            if another I/O error occurs during the request
     * @throws AuthFailureError       if an authentication failure occurs during the request
     */
    public abstract HttpResponse executeRequest(
            Request<?> request, Map<String, String> additionalHeaders)
            throws IOException, AuthFailureError;

    /**
     * @deprecated use {@link #executeRequest} instead to avoid a dependency on the deprecated
     * Apache HTTP library. Nothing in Volley's own source calls this method. However, since
     * {@link BasicNetwork#mHttpStack} is exposed to subclasses, we provide this implementation in
     * case legacy client apps are dependent on that field. This method may be removed in a future
     * release of Volley.
     */
    @Deprecated
    @Override
    public  org.apache.http.HttpResponse performRequest(
            Request<?> request, Map<String, String> additionalHeaders)
            throws IOException, AuthFailureError {
        HttpResponse response = executeRequest(request, additionalHeaders);
        return getHttpResponse(response);


    }

    protected org.apache.http.HttpResponse getHttpResponse0(HttpResponse response) {
        ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
        StatusLine statusLine = new BasicStatusLine(
                protocolVersion, response.getStatusCode(), "" /* reasonPhrase */);
        BasicHttpResponse apacheResponse = new BasicHttpResponse(statusLine);

        List<org.apache.http.Header> headers = new ArrayList<>();
        for (Header header : response.getHeaders()) {
            headers.add(new BasicHeader(header.getName(), header.getValue()));
        }
        apacheResponse.setHeaders(headers.toArray(new org.apache.http.Header[headers.size()]));

        InputStream responseStream = response.getContent();
        if (responseStream != null) {
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(responseStream);
            entity.setContentLength(response.getContentLength());
            apacheResponse.setEntity(entity);
        }

        return apacheResponse;
    }

    protected org.apache.http.HttpResponse getHttpResponse(HttpResponse response) {
        ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
        Log.i("volloy", "------------>getHttpResponse");
        StatusLine responseStatus = new BasicStatusLine(protocolVersion,
                response.getStatusCode(), "");
        BasicHttpResponse basicHttpResponse = new BasicHttpResponse(responseStatus);
        Map<String, List<String>> headerFields = getHeaderFields(response.getHeaders());
        basicHttpResponse.setEntity(entityFromConnection(response));
        for (Map.Entry<String, List<String>> header : headerFields.entrySet()) {
            if (header.getKey() != null) {
                String key = header.getKey();
                List<String> values = header.getValue();
                if (key.equalsIgnoreCase("set-cookie")) {
                    StringBuilder cookieString = new StringBuilder();
                    for (String value : values) {
                        cookieString.append(value).append("\n");//鐢╘n浣滀负鍒嗛殧绗︼紝cookie涓笉搴旇鏈夊洖杞︾鍙�
                    }
                    cookieString.deleteCharAt(cookieString.length() - 1);
                    BasicHeader h = new BasicHeader(header.getKey(), cookieString.toString());
                    basicHttpResponse.addHeader(h);
                } else {
                    BasicHeader h = new BasicHeader(header.getKey(), values.get(0));
                    basicHttpResponse.addHeader(h);
                }
            }
        }
        return basicHttpResponse;
    }

    /**
     * Initializes an {@link HttpEntity} from the given {@link HttpURLConnection}.
     *
     * @return an HttpEntity populated with data from <code>connection</code>.
     */
    protected static HttpEntity entityFromConnection(HttpResponse response) {
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(response.getContent());
        entity.setContentLength(response.getContentLength());
        return entity;
    }


    protected static Map<String, List<String>> getHeaderFields(List<Header> headers) {
        Map<String, List<String>> headerFields = new HashMap<>();
        for (Header header : headers) {
            if (headerFields.containsKey(header.getName())) {
                headerFields.get(header.getName()).add(header.getValue());
            } else {
                List<String> values = new ArrayList<>();
                values.add(header.getValue());
                headerFields.put(header.getName(), values);
            }
        }
        return headerFields;
    }

}
