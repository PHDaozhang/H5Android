package com.game.web;

import android.util.Log;
import android.webkit.JavascriptInterface;

import com.cf.msc.sdk.AppVest;
import com.cf.msc.sdk.SecurityConnection;

public class JSLogModel {
    private static final String TAG = "JSLogModel";

    @JavascriptInterface
    public void log(String log) {
        Log.i(TAG, log);
    }

    @JavascriptInterface
    public void info(String log) {
        Log.i(TAG, log);
    }
}
