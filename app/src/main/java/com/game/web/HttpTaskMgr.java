package com.game.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;

public class HttpTaskMgr implements HttpListener {
    List<String> mServerURLs;
    String mURLPath;
    String mPostData;
    long mOverTime;

    boolean mComplete = false;
    CheckListener mCheckListener;

    public HttpTaskMgr(List<String> serverURLs, String urlPath, String postData, int overTime)
    {
        mServerURLs = serverURLs;
        mURLPath = urlPath;
        mPostData = postData;
        mOverTime = System.currentTimeMillis() + overTime;
    }

    public  void start() {
        for (int i = 0; i < mServerURLs.size(); i++) {
            String url = mServerURLs.get(i) + mURLPath;
            HttpTask task1 = new HttpTask(url, mPostData, true);
            task1.setHttpListener(this);
            task1.request();

            HttpTask task2 = new HttpTask(url, mPostData, false);
            task2.setHttpListener(this);
            task2.request();
        }
    }

    public void setCompleteListener(CheckListener callback)
    {
        mCheckListener = callback;
    }

    @Override
    public void onComplete(HttpTask task) {
        checkComplete(task);
    }

    synchronized void checkComplete(HttpTask task) {
        if (mComplete) {
            return;
        }
        if (task.getSuccess()) {
            mComplete = true;
            complete(task.getSuccess(), task.getResult());
        }
        else if (System.currentTimeMillis() > mOverTime) {
            mComplete = true;
            complete(false, "");
        }
        else {
            task.request();
        }
    }

    void complete(final boolean success, final String result)
    {
        MainActivity.GetMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCheckListener.onComplete(success, result);
            }
        });
    }
}
