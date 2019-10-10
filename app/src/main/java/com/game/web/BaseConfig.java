package com.game.web;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;

public class BaseConfig {
    private List<String> serverURLs = new ArrayList<String>();
    private List<String> backupServerURLs = new ArrayList<String>();

    public List<String> getServerURLs() {
        return serverURLs;
    }
    public List<String> getBackupServerURLs() {
        return  backupServerURLs;
    }

    public void loadConfig() {
        String json1 = loadLocalFile();
        BaseConfig config1 = JSON.parseObject(json1, BaseConfig.class);

        String json2 = loadUpdateFile();
        BaseConfig config2 = null;
        if (json2 != null && !json2.isEmpty()) {
            config2 = JSON.parseObject(json2, BaseConfig.class);
        }

        if (config2 != null && config2.serverURLs.size() > 0) {
            serverURLs = config2.serverURLs;
        }
        else {
            serverURLs = config1.serverURLs;
        }
        if (config2 != null && config2.backupServerURLs.size() > 0) {
            backupServerURLs = config2.backupServerURLs;
        }
        else {
            backupServerURLs = config1.backupServerURLs;
        }
    }

    private String loadLocalFile() {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            AssetManager assetManager = MainActivity.GetMainActivity().getAssets();
            BufferedReader bf = new BufferedReader(new InputStreamReader(assetManager.open("server.json")));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    private String loadUpdateFile() {
        String fileName = MainActivity.GetMainActivity().getFilesDir().getAbsolutePath() + "/server.json";
        StringBuilder stringBuilder = new StringBuilder();
        try {
            FileInputStream inputStream = new FileInputStream(fileName);
            byte[] buffer = new byte[1024];
            while (true) {
                int length = inputStream.read(buffer);
                if (length > 0) {
                    stringBuilder.append(new String(buffer, 0, length));
                }
                else {
                    break;
                }
            }
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    public void saveUpdateFile() {
        String fileName = MainActivity.GetMainActivity().getFilesDir().getAbsolutePath() + "/server.json";
        String jsonString = JSON.toJSONString(this);
        try {
            FileOutputStream outputStream = new FileOutputStream(fileName, false);
            outputStream.write(jsonString.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetServerURLs(ServeConfig config) {
        serverURLs = config.getServerURLs();
        backupServerURLs = config.getBackupServerURLs();
    }
}
