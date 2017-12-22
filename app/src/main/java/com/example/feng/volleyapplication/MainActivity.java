package com.example.feng.volleyapplication;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.HttpResponse;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class MainActivity extends Activity {
    String Action = "aaaaaa";
    String Action2 = "aaaaaa2";
    Rec rec1 = new Rec();
    Rec rec2 = new Rec();
    LocalBroadcastManager localBroadcastManager;
    Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        toast = Toast.makeText(getApplicationContext(), "jfiej", Toast.LENGTH_SHORT);
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startRequest3();
                    }
                }).start();
            }
        });
        findViewById(R.id.btn2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startRequest4();
                    }
                }).start();
            }
        });
        findViewById(R.id.toast).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toast.setText("jfeijfe11111111111");
                toast.show();
            }
        });
        findViewById(R.id.toast2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toast.cancel();
                toast.setText("2222222222");
                toast.show();
            }
        });
        findViewById(R.id.btn_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                localBroadcastManager.sendBroadcast(new Intent(Action));
            }
        });
        findViewById(R.id.btn_send2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast(new Intent(Action2));
            }
        });
        findViewById(R.id.btn_regist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                localBroadcastManager.registerReceiver(rec1, new IntentFilter(Action));
                registerReceiver(rec1, new IntentFilter(Action2));
//                registerReceiver(rec2, new IntentFilter(Action));
            }
        });
        findViewById(R.id.btn_unregist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                localBroadcastManager.unregisterReceiver(rec1);
//                unregisterReceiver(rec2);
            }
        });
    }

    private void startRequest1() {
        try {
//            (new Request<String>(Request.Method.GET, "http://www.weather.com.cn/data/sk/101010100.html", new Response.Listener<String>() {
//                @Override
//                public void onResponse(String response) {
//                    Log.i("----->", " onResponse ");
//                }
//            }, new Response.ErrorListener() {
//                @Override
//                public void onErrorResponse(VolleyError error) {
//                    Log.i("----->", " onErrorResponse ");
//                }
//            })

            HttpResponse response = new HurlStack().executeRequest(new Request<String>(Request.Method.GET, "http://www.weather.com.cn/data/sk/101010100.html", new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {
                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    return null;
                }

                @Override
                protected void deliverResponse(String response) {

                }
            }, new HashMap<String, String>());
            InputStreamReader reader = new InputStreamReader(response.getContent());
            StringBuffer buffer = new StringBuffer();
            char[] chars = new char[1024];
            int len = 0;
            while ((len = reader.read(chars)) != -1) {
                buffer.append(chars, 0, len);
            }
            String result = buffer.toString();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
        }
    }

    private void startRequest() {
        VolleyUtils.start(new Request<String>(Request.Method.GET, "http://www.weather.com.cn/data/sk/101010100.html", new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                Cache.Entry cacheEntry = new Cache.Entry();
                cacheEntry.allResponseHeaders = response.allHeaders;
                cacheEntry.data = response.data;
                cacheEntry.responseHeaders = response.headers;
                String result = "";
                if (response.statusCode == 200) {
                    try {
                        result = new String(response.data, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                return Response.success(result, cacheEntry);
            }

            @Override
            protected void deliverResponse(String response) {

            }
        });
    }

    private void startRequest3() {
        VolleyUtils.start(new StringRequest(Request.Method.GET, "http://www.weather.com.cn/data/sk/101010100.html", new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }));

    }

    private void startRequest4() {
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

    public static class Rec extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }
}
