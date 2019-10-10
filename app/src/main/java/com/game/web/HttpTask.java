package com.game.web;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class HttpTask {
    static final String TAG = "MainLoigc";

    HttpTask mSelf;
    String mURL;
    String mData;
    boolean mUseSDK;

    HttpListener mHttpListener;
    boolean mSuccess;
    String mResult;

    public HttpTask(String url, String data, boolean useSDK)
    {
        mSelf = this;
        mURL = url;
        mData = data;
        mUseSDK = useSDK;
    }

    public void setHttpListener(HttpListener callback)
    {
        mHttpListener = callback;
    }

    public boolean getSuccess() {
        return mSuccess;
    }

    public String getResult() {
        return mResult;
    }

    public void request() {
        //开启线程，发送请求
        new Thread(new Runnable() {
            @Override
            public void run() {
                mSuccess = false;
                mResult = "";
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try {
                    String urlString = mURL;
                    if (mUseSDK) {
                        urlString = MainLoigc.getSingleton().convertURL(mURL);
                    }
                    Log.d(TAG, "httptask url=" + urlString);
                    URL url = new URL(urlString);
                    connection = (HttpURLConnection) url.openConnection();
                    //设置请求方法
                    connection.setRequestMethod("GET");
                    //设置连接超时时间（毫秒）
                    connection.setConnectTimeout(5000);
                    //设置读取超时时间（毫秒）
                    connection.setReadTimeout(5000);
                    //返回输入流
                    int responseCode = connection.getResponseCode();
                    if(responseCode == 200) {
                        //返回输入流
                        InputStream in = connection.getInputStream();
                        //读取输入流
                        reader = new BufferedReader(new InputStreamReader(in));
                        StringBuilder result = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            result.append(line);
                        }
                        mSuccess = true;
                        mResult = result.toString();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (connection != null) {//关闭连接
                        connection.disconnect();
                    }
                }
                mHttpListener.onComplete(mSelf);
            }
        }).start();
    }
}
