package com.game.web;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashSet;

public class MainWebView {
    private WebView mWebView;
    private MainActivity mActivity;

    private HashSet<String> mlocalFiles = new HashSet<>();
    private String mUpdateDirectory = null;
    private String mLocalDirectory = "local";
    private String mTempDirectory = null;

    private String mMainUrl = "";
    private  boolean mUseSDK = false;

    private String TAG = "MainWebView";

    public MainWebView(MainActivity activity) {
        mActivity = activity;

        initWebView();
        initLocalFile();
    }

    public void loadUrl(String mainUrl, boolean useSDK) {
        mMainUrl = mainUrl;
        mUseSDK = useSDK;
        mWebView.loadUrl(mMainUrl + "index.html?or_src=recharge");
    }

    //web init
    void initWebView() {
        mWebView = (WebView)mActivity.findViewById(R.id.webview);
        WebSettings localWebSettings = mWebView.getSettings();

        localWebSettings.setAllowFileAccess(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            localWebSettings.setAllowFileAccessFromFileURLs(true);
        }
        localWebSettings.setJavaScriptEnabled(true);
        localWebSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        mWebView.addJavascriptInterface(new JSModel(), "nativeInterface");
        mWebView.addJavascriptInterface(new JSLogModel(), "console");

        localWebSettings.setDomStorageEnabled(true);
        localWebSettings.setUseWideViewPort(true);
        localWebSettings.setLoadWithOverviewMode(true);

        mWebView.setWebViewClient(new MainWebView.MainWebViewClient());
        mWebView.setWebChromeClient(new MainWebView.MainWebChromeClient());
    }

    //file init
    void initLocalFile() {
        try {
            mUpdateDirectory = mActivity.getFilesDir().getAbsolutePath() + "update/";
            mTempDirectory = mActivity.getFilesDir().getAbsolutePath() + "temp/";

            fillFiles(mLocalDirectory);
        }
        catch (Exception e) {
        }
    }

    void fillFiles(String path) throws IOException {
        String[] files = mActivity.getAssets().list(path);
        for (String file : files) {
            String fullName = path + "/" + file;
            if (fullName.contains(".")){
                mlocalFiles.add(fullName);
            }
            else {
                fillFiles(fullName);
            }
        }
    }

    InputStream getLocalFile(String fileName) {
        InputStream in = null;
        String fullFileName = mLocalDirectory + "/" + fileName;
        if (mlocalFiles.contains(fullFileName)) {
            try {
                in = mActivity.getAssets().open(fullFileName);
                Log.i(TAG, String.format("getLocalFile %s", fileName));
            } catch (IOException e) {
                Log.i(TAG, String.format("getLocalFile %s failed %s", fileName, e.getMessage()));
            }
        }
        return in;
    }

    InputStream getUpdateFile(String fileName) {
        InputStream in = null;
        try {
            String fullFileName = mUpdateDirectory + "/" + fileName;
            File file = new File(fullFileName);
            if (file.exists()) {
                in = new FileInputStream(fullFileName);
                Log.i(TAG, String.format("getUpdateFile %s", fileName));
            }
        }
        catch (Exception e) {
            Log.i(TAG, String.format("getUpdateFile %s failed %s", fileName, e.getMessage()));
        }
        return in;
    }

    boolean saveUpdateFile(String fileName, InputStream in) {
        Log.i(TAG, String.format("saveUpdateFile %s", fileName));
        String tempFullFileName = mTempDirectory + "/" + fileName;
        String updateFullFileName = mUpdateDirectory + "/" + fileName;
        Log.i(TAG, tempFullFileName);
        Log.i(TAG, updateFullFileName);
        try {
            checkDirectory(tempFullFileName);
            checkDirectory(updateFullFileName);

            FileOutputStream out = new FileOutputStream(tempFullFileName);
            byte[] buffer = new byte[1024];
            while (true) {
                int length = in.read(buffer);
                if (length > 0) {
                    out.write(buffer, 0, length);
                    Log.i(TAG, String.format("saveUpdateFile read %s length %d", fileName, length));
                }
                else {
                    break;
                }
            }
            out.flush();
            out.close();
            return copyFile(tempFullFileName, updateFullFileName);
        }
        catch (Exception e) {
            Log.e(TAG, String.format("saveUpdateFile %s failed %s", fileName, e.getMessage()));
            //如果写入出错，删除文件
            File file = new File(tempFullFileName);
            if (file.exists()) {
                file.delete();
            }
        }
        return false;
    }

    void checkDirectory(String fullFileName) {
        int index = fullFileName.lastIndexOf("/");
        String fullPath = fullFileName.substring(0, index);
        File file = new File(fullPath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    boolean copyFile(String srcFileName, String tarFileName) {
        try {
            File oldFile = new File(srcFileName);
            if (!oldFile.exists()) {
                Log.e(TAG, "copyFile:  oldFile not exist.");
                return false;
            } else if (!oldFile.isFile()) {
                Log.e(TAG, "copyFile:  oldFile not file.");
                return false;
            } else if (!oldFile.canRead()) {
                Log.e(TAG, "copyFile:  oldFile cannot read.");
                return false;
            }

            FileInputStream fileInputStream = new FileInputStream(srcFileName);
            FileOutputStream fileOutputStream = new FileOutputStream(tarFileName);
            byte[] buffer = new byte[1024];
            int byteRead;
            while (true) {
                byteRead = fileInputStream.read(buffer);
                if (byteRead > 0) {
                    fileOutputStream.write(buffer, 0, byteRead);
                }
                else {
                    break;
                }
            }
            fileInputStream.close();
            fileOutputStream.flush();
            fileOutputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    boolean needIntercept(String url) {
        //不需要缓存,走正常流程
        if (!url.startsWith(mMainUrl)) {
            return false;
        }
        if (url.indexOf("?") >= 0) {
            return false;
        }
        return true;
    }

    InputStream checkFileExists(String fileName) {
        Log.i(TAG, String.format("checkFileExists %s", fileName));
        InputStream in = null;
        in = getUpdateFile(fileName);
        if (in == null) {
            in = getLocalFile(fileName);
        }
        return in;
    }

    String getFileName(String url) {
        int index = url.indexOf(mMainUrl);
        String fileName = url.substring(index + mMainUrl.length(), url.length());
        return fileName;
    }

    String getMimeType(String fileName) {
        String mimeType;
        if(fileName.endsWith(".mc") || fileName.endsWith(".fnt") || fileName.endsWith(".db") || fileName.endsWith(".st")) {
            mimeType = "application/octet-stream";
        } else if(fileName.endsWith(".png")) {
            mimeType = "application/png";
        } else if(fileName.endsWith(".mp3")) {
            mimeType = "audio/mpeg";
        } else if(fileName.endsWith(".jpg")) {
            mimeType = "application/jpeg";
        } else if(fileName.endsWith(".js")) {
            mimeType = "application/javascript";
        } else if(fileName.endsWith(".json")) {
            mimeType = "application/json";
        } else {
            mimeType = "text/html";
        }
        return mimeType;
    }

    private void alertDialog()
    {
        AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.GetMainActivity());
        builder.setTitle("提示");
        builder.setMessage("页面加载失败,是否重新加载?");
        builder.setPositiveButton("是",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mWebView.loadUrl(mMainUrl + "index.html?or_src=recharge");
                    }
                });
        builder.setNegativeButton("否",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MainActivity.GetMainActivity().finish();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    class MainWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);

            Log.d(TAG, "onProgressChanged " + newProgress);
        }

        @Override
        public boolean onJsBeforeUnload(WebView view, String url, String message, final JsResult result) {
            return super.onJsBeforeUnload(view, url, message, result);
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Log.d(TAG, "onJsAlert");
            return super.onJsAlert(view, url, message, result);
        }
    }

    class MainWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String urlString) {
            Log.d(TAG, "shouldOverrideUrlLoading " + urlString);
            return false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d(TAG, "onPageFinished " + url);
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Log.d(TAG, "failingUrl" + failingUrl);
            super.onReceivedError(view, errorCode, description, failingUrl);
            alertDialog();
        }
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            //不需要拦截,走正常流程
            if (!MainWebView.this.needIntercept(url)) {
                Log.d(TAG, "shouldInterceptRequest " + url);
                return super.shouldInterceptRequest(view, request);
            }
            Log.d(TAG, "shouldInterceptRequest check " + url);
            //缓存是否存在
            String fileName = getFileName(url);
            InputStream in = MainWebView.this.checkFileExists(fileName);
            if (in != null) {
                return new WebResourceResponse(MainWebView.this.getMimeType(url), "UTF-8", in);
            }
            //去请求资源
            if (mUseSDK) {
                url = MainLoigc.getSingleton().convertURL(url);
            }
            Log.d(TAG, "shouldInterceptRequest download " + url);
            HttpURLConnection connection = null;
            try
            {
                URL httpURL = new URL(url);
                connection = (HttpURLConnection) httpURL.openConnection();
                //设置请求方法
                connection.setRequestMethod("GET");
                //设置连接超时时间（毫秒）
                connection.setConnectTimeout(10000);
                //设置读取超时时间（毫秒）
                connection.setReadTimeout(60000);
                //返回输入流
                int responseCode = connection.getResponseCode();
                if(responseCode == 200) {
                    in = connection.getInputStream();
                    if (saveUpdateFile(fileName, in)) {
                        Log.d(TAG, "shouldInterceptRequest downloadComplete " + url);
                        in = getUpdateFile(fileName);
                        if (in != null)
                            return new WebResourceResponse(connection.getContentType(), "UTF-8", in);
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "shouldInterceptRequest download error " + url);
                e.printStackTrace();
            }
            Log.d(TAG, "shouldInterceptRequest downloadfailed " + url);
            return super.shouldInterceptRequest(view, request);
        }
    }
}
