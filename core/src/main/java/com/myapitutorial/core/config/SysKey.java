package com.myapitutorial.core.config;

public enum SysKey {
    PORT("server.port"),
    MODE("server.mode");

    private final String key;

    SysKey(String key) {
        this.key = key;
    }
    
    public String getKey() {
        return key;
    }
}