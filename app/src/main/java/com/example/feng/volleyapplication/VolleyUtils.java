package com.example.feng.volleyapplication;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Network;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Created by Feng on 2017/12/21.
 */

public class VolleyUtils {
    private static VolleyUtils instance;
    private static final String DEFAULT_CACHE_DIR = "fans_volley";
    private static final int DEFAULT_DISK_USAGE_BYTES = 90 * 1024 * 1024;

    private RequestQueue queue;

    private VolleyUtils() {
    }


    public static Context getContext() {
        return ThisApplication.getInstance();
    }

    public static VolleyUtils getInstance() {
        if (instance == null) {
            synchronized (VolleyUtils.class) {
                if (instance == null) {
                    instance = new VolleyUtils();
                }
            }
        }
        return instance;
    }

    private synchronized static void init() {
        if (getInstance().queue == null) {
            File cacheDir = new File(getContext().getCacheDir(), DEFAULT_CACHE_DIR);
            HurlStack stack = new HurlStack();
            Network network = new BasicNetwork(stack);
            getInstance().queue = new RequestQueue(new DiskBasedCache(cacheDir, DEFAULT_DISK_USAGE_BYTES), network);
            getInstance().queue.start();
        }
    }

    public static void start(Request request) {
        init();
        getInstance().queue.add(request);
    }

    public static void startRequest() {
        VolleyUtils.start(new StringRequest(Request.Method.GET, "http://www.weather.com.cn/data/sk/101010100.html", new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String parsed;
                try {
                    parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "utf-8"));
                } catch (UnsupportedEncodingException e) {
                    parsed = new String(response.data);
                }
                return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
            }
        });
    }

    public static Request createJsonObjectRequest(int method, String url, JSONObject jsonRequest, RetryPolicy policy,
                                                  Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        if (policy == null) {
            policy = getDefalutRetryPolicy(jsonRequest);
        }
        return new JsonObjectRequest(method, url, jsonRequest, listener, errorListener).setRetryPolicy(policy);
    }

    public static Request createJsonObjectRequest(int method, String url, Map<String, Object> params, RetryPolicy policy,
                                                  Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        JSONObject jsonRequest = encodeJsonRequestParameters(params);
        return createJsonObjectRequest(method, url, jsonRequest, policy, listener, errorListener);
    }

    public static Request createJsonObjectRequest(int method, String url, Map<String, Object> params,
                                                  Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        return createJsonObjectRequest(method, url, params, null, listener, errorListener);
    }

    public static Request createGetJsonObjectRequest(String url, Map<String, Object> params,
                                                     Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        return createJsonObjectRequest(Request.Method.GET, url, params, null, listener, errorListener);
    }

    public static Request createPostJsonObjectRequest(String url, Map<String, Object> params,
                                                      Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        return createJsonObjectRequest(Request.Method.POST, url, params, null, listener, errorListener);
    }


    private static RetryPolicy getDefalutRetryPolicy(JSONObject jsonRequest) {
        RetryPolicy policy;
        if (jsonRequest == null) {
            policy = new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        } else {
            policy = new DefaultRetryPolicy(10000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        }
        return policy;
    }

    public static JSONObject encodeJsonRequestParameters(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        // 消息头
        JSONObject param = new JSONObject(params);
        //	for (Map.Entry<String, Object> entry : params.entrySet()) {
        //		param.put(entry.getKey(), entry.getValue());
        //	}
        return param;
    }
}
