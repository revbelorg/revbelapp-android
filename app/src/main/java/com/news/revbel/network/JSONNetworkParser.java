package com.news.revbel.network;


import android.util.Log;

import com.news.revbel.RevApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class JSONNetworkParser {
    public static final MediaType TEXT_JSON
            = MediaType.parse("text/json; charset=utf-8");
    interface JSONErrorCallback {
        void onFailure(Exception e);

        void onResponse(Object jsonResponse);
    }

    interface JSONCallback {
        void onFailure();
        void onTimeout();
        void onResponse(Object jsonResponse);
    }

    private OkHttpClient client;
    private HashMap<String, Call> requests = new HashMap<>();

    JSONNetworkParser(OkHttpClient client) {
        this.client = client;
    }

    void postJSONToURL(String toUrl, RequestBody postBody, final JSONErrorCallback jsonCallback) {
        Request request = new Request.Builder().url(toUrl).post(postBody).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                jsonCallback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) jsonCallback.onFailure(new IOException("Unexpected code " + response));

                // try parse the string to a JSON object
                String responseString = response.body().string();
                try {
                    JSONArray jObj = new JSONArray(responseString);
                    jsonCallback.onResponse(jObj);
                } catch (JSONException e) {
                    try {
                        JSONObject jObj = new JSONObject(responseString);
                        jsonCallback.onResponse(jObj);
                    } catch (JSONException exception) {
                        Log.e("JSON Parser", "Error parsing data  with error:" + e.getMessage()
                                + "additional info: " + e.toString());
                        jsonCallback.onFailure(exception);
                    }
                }
            }
        });

    }

    void getJSONArrayFromUrl(String toUrl,  final JSONCallback jsonCallback) {
        getJSONArrayFromUrl(toUrl, null, jsonCallback);
    }

    void getJSONArrayFromUrl(String toUrl, String label,  final JSONCallback jsonCallback) {
        Request request = new Request.Builder().url(toUrl).build();
        Call call = client.newCall(request);

        if (label != null) {
            Call oldCall = requests.get(label);
            if (oldCall != null) {
                oldCall.cancel();
            }
            requests.put(label, call);
        }

        call.enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (!call.isCanceled()) {
                    if (requests.containsValue(call)) {
                        requests.remove(label);
                    }
                    if (e instanceof SocketTimeoutException) {
                        RevApplication.runOnUI(jsonCallback::onTimeout);
                    } else {
                        jsonCallback.onFailure();
                    }

                }
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                requests.remove(label);
                if (!response.isSuccessful()) {
                    jsonCallback.onFailure();
                    throw new IOException("Unexpected code " + response);
                }

                // try parse the string to a JSON object
                String responseString = response.body().string();
                try {
                    JSONArray jObj = new JSONArray(responseString);
                    jsonCallback.onResponse(jObj);
                } catch (JSONException e) {
                    try {
                        JSONObject jObj = new JSONObject(responseString);
                        jsonCallback.onResponse(jObj);
                    } catch (JSONException exception) {
                        Log.e("JSON Parser", "Error parsing data  with error:" + e.getMessage()
                                + "additional info: " + e.toString());
                        jsonCallback.onFailure();
                    }
                }
            }
        });
    }
}