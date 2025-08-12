package com.myapitutorial.core.config;

public enum ConfigKey {

    SSL_ENABLED("server.ssl.enabled"),
    SSL_KEYSTORE("server.ssl.keystore"),
    SSL_KEYSTORE_PATH("server.ssl.keystore.path"),
    SSL_KEYSTORE_PASSWORD("server.ssl.keystore.password"),
    SSL_KEYSTORE_TYPE("server.ssl.keystore.type"),
    SSL_PORT("server.ssl.port"),
    // SSL_KEYMANAGER_FACTORY("server.ssl.keymanager.factory"),

    SERVER_PORT("server.port"),
    SERVER_MODE("server.mode"),
    DB_URL("db.url"),
    DB_USER("db.user"),
    DB_PASSWORD("db.password");

    private final String key;

    ConfigKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
