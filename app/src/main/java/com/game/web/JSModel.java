package com.game.web;

import android.util.Log;
import android.webkit.JavascriptInterface;

import com.cf.msc.sdk.AppVest;
import com.cf.msc.sdk.SecurityConnection;

public class JSModel {
    private static final String TAG = "JSModel";

    @JavascriptInterface
    public String getGameURL() {
        return MainLoigc.getSingleton().getGameURL();
    }

    @JavascriptInterface
    public boolean isGameUseSDK() {
        return MainLoigc.getSingleton().isGameUseSDK();
    }

    @JavascriptInterface
    public String getClientIP() {
        return MainLoigc.getSingleton().getClientIP();
    }

    @JavascriptInterface
    public String getPort(String host, int port) {
        return MainLoigc.getSingleton().getPort(host, port);
    }

    @JavascriptInterface
    public String getDeviceID() {
        return MainLoigc.getSingleton().getDeviceID();
    }

    @JavascriptInterface
    public String getGameConfig() {
        return MainLoigc.getSingleton().getGameConfig();
    }

    @JavascriptInterface
    public void openURL(String url) {
        MainLoigc.getSingleton().openURL(url);
    }

    @JavascriptInterface
    public void openWebView(String json) {
        MainLoigc.getSingleton().openSubWebView(json);
    }

    @JavascriptInterface
    public void closeWebView() {
        MainLoigc.getSingleton().closeSubWebView();
    }
}
