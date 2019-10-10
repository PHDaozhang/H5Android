package com.game.web;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.annotation.JSONType;

public class ServeConfig {
    private String gameURL;
    private boolean gameUseSDK;

    private String resourceURL;
    private boolean resourceUseSDK;

    private List<String> serverURLs = new ArrayList<String>();
    private List<String> backupServerURLs = new ArrayList<String>();

    public ServeConfig()
    {
        serverURLs = new ArrayList<String>();
        backupServerURLs = new ArrayList<String>();
    }

    public ServeConfig(String json)
    {
        JSONObject a = JSON.parseObject(json);
        gameURL = a.getString("gameURL");
        gameUseSDK = a.getBoolean("gameUseSDK");
        resourceURL = a.getString("resourceURL");
        resourceUseSDK = a.getBoolean("resourceUseSDK");

        serverURLs = a.getJSONArray("serverURLs").toJavaList(String.class);
        backupServerURLs = a.getJSONArray("backupServerURLs").toJavaList(String.class);
    }

    public List<String> getServerURLs() {
        return serverURLs;
    }

    public List<String> getBackupServerURLs() {
        return backupServerURLs;
    }

    public String getGameURL() {
        return gameURL;
    }

    public boolean getGameUseSDK() {
        return gameUseSDK;
    }

    public String getResourceURL() {
        return resourceURL;
    }

    public boolean getResourceUseSDK() {
        return resourceUseSDK;
    }
}
