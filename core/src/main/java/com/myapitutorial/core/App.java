package com.myapitutorial.core;

import com.myapitutorial.core.server.AppServer;

public class App {

    public static void main(String[] args) {
        try {
            AppServer appServer = new AppServer();
            appServer.initialize();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
