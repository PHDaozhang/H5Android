package com.game.web;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cf.msc.sdk.AppVest;
import com.cf.msc.sdk.SecurityConnection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainLoigc implements CheckListener {
    static final String TAG = "MainLoigc";

    static MainLoigc sSingleton = null;

    boolean mIsInitSDK = false;
    ServeConfig mServerConfig;

    String mGameConfig = null;

    String mKey = "";
    static int[] mBuildKeys = new int[]{0x12f9321d, 0x9284a0c1, 0x32b1f921, 0x99dd0184};

    public static MainLoigc getSingleton() {
        if (sSingleton == null) {
            sSingleton = new MainLoigc();
        }
        return sSingleton;
    }

    MainLoigc() {
        mKey = buildKey(0x9174d321, 16);
    }

    String buildKey(int key, int length) {
        int seed = key;
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++)
        {
            int j = i%mBuildKeys.length;
            seed = (seed * mBuildKeys[j] + mBuildKeys[j]);
            byte b = (byte)(seed%111);
            if (b < 0)
            {
                b += 111;
            }
            bytes[i] = b;
        }
        String str = null;
        try {
            str = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }

    public void start() {
        Log.d(TAG, "start");
        initGameConfig();

        String accessKey = Aes.decrypt("AD563057F4C674ED882CE3FD541D52924143D9967F5B17BEA99B1DB9D4473883C20656090BA350040B7482129500C7A7", mKey);
        String uuid = Aes.decrypt("E38A24F0284550FF50B846DA5A3294182FF17F504A4ACB32B3188FAE6679D5F15E4A0F1E2204D1D4F2C80BB9D77A4FA175CAF8748E92892BF9874E00A33BBDE7C20656090BA350040B7482129500C7A7", mKey);
        //initSDK("1b5a01b149b7a015094bbf08d1dde088", "yc4m3piSt9tgJfceTk4xXisxeIKQohkFiveoHs3Re+mfJUzbVnODjH6QxV35Rp8=");
        initSDK(accessKey, uuid);

        startCheck();
    }

    public void startCheck()
    {
        Log.d(TAG, "startCheck");
        BaseConfig config = new BaseConfig();
        config.loadConfig();

        HttpTaskMgr taskMgr = new HttpTaskMgr(config.getServerURLs(), "web/ServerConfig.aspx", null, 30000);
        taskMgr.setCompleteListener(this);
        taskMgr.start();
    }

    @Override
    public void onComplete(boolean success, String result)
    {
        Log.d(TAG, "onComplete");
        if (success) {
            mServerConfig = new ServeConfig(result);
            if (mServerConfig.getServerURLs().size() > 0) {
                BaseConfig config = new BaseConfig();
                config.resetServerURLs(mServerConfig);
                config.saveUpdateFile();

                startLoadGame();
            }
            else
            {
                alertDialog("服务器地址不正确,是否重试?");
            }
        }
        else
        {
            alertDialog("服务器访问失败,是否重试?");
        }
    }

    private void alertDialog(String info)
    {
        AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.GetMainActivity());
        builder.setTitle("提示");
        builder.setMessage(info);
        builder.setPositiveButton("是",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startCheck();
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

    public void startLoadGame(){
        Log.d(TAG, "startLoadGame");
        MainActivity.GetMainActivity().loadUrl(mServerConfig.getResourceURL(), mServerConfig.getResourceUseSDK());
    }

    void initGameConfig() {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            AssetManager assetManager = MainActivity.GetMainActivity().getAssets();
            BufferedReader bf = new BufferedReader(new InputStreamReader(assetManager.open("config/config.json")));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mGameConfig = stringBuilder.toString();
    }

    public boolean initSDK(String accessKey, String uuid) {
        if (mIsInitSDK) {
            return true;
        }
        int res = AppVest.init(accessKey, uuid);
        if (res == -1)
        {
            Log.d(TAG, "android init sdk error" + String.valueOf(res));
            return false;
        }
        res = AppVest.setTimeouts(120, 5);
        if (res == -1)
        {
            Log.d(TAG, "setTimeouts false");
            return false;
        }
        Log.d(TAG, "android init success");
        mIsInitSDK = true;
        return true;
    }

    public String getClientIP() {
        return AppVest.getClientIP();
    }

    public String getPort(String host, int port) {
        SecurityConnection conn = AppVest.getServerIPAndPort(host, port);
        if (conn.getServerPort() == -1)
        {
            return String.format("%s:%d", host, port);
        }
        else
        {
            return String.format("%s:%d", conn.getServerIp(), conn.getServerPort());
        }
    }

    public String getDeviceID() {
        String deviceid = "";
        try
        {
            deviceid = MainActivity.GetMainActivity().getDeviceID();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "deviceid=" + deviceid);
        return deviceid;
    }

    public String getGameConfig() {
        return mGameConfig;
    }

    public boolean isResourceUseSDK() {
        return mServerConfig.getResourceUseSDK();
    }

    public String getResourceURL() {
        return mServerConfig.getResourceURL();
    }

    public boolean isGameUseSDK() {
        return mServerConfig.getGameUseSDK();
    }

    public String getGameURL() {
        return mServerConfig.getGameURL();
    }

    public String convertURL(String url) {
        String tarUrl = url;
        if (mIsInitSDK)
        {
            String pattern = "([\\w.]+):([\\w]+)";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(tarUrl);
            if (m.find() && m.groupCount() >= 2) {
                String host = m.group(1);
                int port = Integer.parseInt(m.group(2));

                Log.d(TAG, host);
                String tarHost = getPort(host, port);
                Log.d(TAG, tarHost);
                tarUrl = tarUrl.replaceFirst(pattern, tarHost);
            }
        }
        return tarUrl;
    }

    public boolean openURL(String url) {
        boolean ret = false;
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            MainActivity.GetMainActivity().startActivity(i);
            ret = true;
        } catch (Exception e) {
        }
        return ret;
    }

    public void openSubWebView(final String json) {
        final String jsonString = json;
        MainActivity.GetMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.GetMainActivity().openSubWebView(jsonString);
            }
        });
    }

    public void closeSubWebView() {
        MainActivity.GetMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.GetMainActivity().closeSubWebView();
            }
        });
    }
}
