package com.game.web;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashSet;

public class MainActivity extends Activity {
    private MainWebView mMainWebView;
    private SubWebView mSubWebView;
    private Object mLock = new Object();

    private boolean mPermission = true;
    static MainActivity sActivity = null;

    static {
        System.loadLibrary("cf-msc");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NavigationBarUtil.hideNavigationBar(getWindow());
        setContentView(R.layout.activity_main);

        sActivity = this;
        //Set the format of window
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   //应用运行时，保持屏幕高亮，不锁屏

        mMainWebView = new MainWebView(this);
        MainLoigc.getSingleton().start();
        Log.d("MainActivity", "onCreate");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mSubWebView != null)
            mSubWebView.onActivityResult(requestCode, resultCode, data);
    }

    public boolean openSubWebView(String json) {
        if (mSubWebView != null)
            return false;

        FrameLayout frameLayout = getWindow().getDecorView().findViewById(android.R.id.content);
        mSubWebView = new SubWebView(this, frameLayout);
        mSubWebView.open(json);
        return true;
    }

    public void closeSubWebView() {
        if (mSubWebView != null) {
            mSubWebView.close();
            mSubWebView = null;
        }
    }

    public void setOrientation(int orientation){
        sActivity.setRequestedOrientation(orientation);
    }

    public void resetOrientation() {
        sActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    public static MainActivity GetMainActivity() {
        return sActivity;
    }

    public void loadUrl(String mainUrl, boolean useSDK)
    {
        mMainWebView.loadUrl(mainUrl, useSDK);
    }

    //获取设备号
    public String getDeviceID() {
        String deviceid = "";
        try
        {
            Log.d("MainActivity", "checkPermission");
            //动态权限
            if (!checkPermission(Manifest.permission.READ_PHONE_STATE))
            {
                synchronized (mLock) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 100);
                    mLock.wait();
                }
                if (!mPermission) {
                    return deviceid;
                }
            }
            Log.d("MainActivity", "getSystemService");
            TelephonyManager tm = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
            deviceid = tm.getDeviceId();
            return deviceid;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return deviceid;
    }

    private boolean checkPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = ContextCompat.checkSelfPermission(this, permission);
            Log.d("MainActivity", "checkPermission=" + result);
            if (result == PackageManager.PERMISSION_GRANTED){
                return true;
            } else {
                return false;
            }
        } else {
            Log.d("MainActivity", "checkPermission true");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d("MainActivity", "onRequestPermissionsResult");
        if (requestCode == 100) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPermission = true;
                } else {
                    mPermission = false;
                    //如果拒绝授予权限,且勾选了再也不提醒
                    if (!shouldShowRequestPermissionRationale(permissions[0])) {
                        showTipGoSetting();
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        synchronized (mLock) {
            mLock.notify();
        }
    }

    private void showTipGoSetting() {
        new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setTitle("获取手机识别码失败")
                .setMessage("请在-应用设置-权限-中，允许APP使用电话权限来获取手机识别码。")
                .setPositiveButton("立即开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 跳转到应用设置界面
                        goToAppSetting();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).setCancelable(false).show();
    }

    private void goToAppSetting() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 123);
    }
}
