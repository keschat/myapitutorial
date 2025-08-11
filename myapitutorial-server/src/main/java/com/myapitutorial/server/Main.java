package com.myapitutorial.server;

import java.security.Security;

import org.conscrypt.OpenSSLProvider;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
// import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {

        Security.insertProviderAt(new OpenSSLProvider(), 1);

        Server server = new Server();
        server.setStopTimeout(5000);

        final int PORT = 9090;

        // Add graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (server.isRunning()) {
                    logger.info("Graceful shutdown initiated...");
                    server.stop();
                }
            } catch (Exception e) {
                logger.error("Error during graceful shutdown", e);
            }
        }));

        SecureRequestCustomizer src = new SecureRequestCustomizer();
        src.setSniHostCheck(false);
        src.setSniRequired(false);

        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendServerVersion(false);
        httpConfig.addCustomizer(src);
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(8443);
        httpConfig.setOutputBufferSize(32768);
        httpConfig.setSendXPoweredBy(false);

        HttpConnectionFactory http1 = new HttpConnectionFactory(httpConfig);
        HTTP2CServerConnectionFactory http2 = new HTTP2CServerConnectionFactory(httpConfig);
        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        alpn.setDefaultProtocol(http1.getProtocol());

        SslContextFactory.Server sslCtx = new SslContextFactory.Server();
        sslCtx.setKeyStorePath("keystore.p12");
        sslCtx.setKeyStorePassword("pass123");
        sslCtx.setKeyManagerPassword("pass123");

        SslConnectionFactory ssl = new SslConnectionFactory(sslCtx, alpn.getProtocol());

        // HTTPS connector
        ServerConnector httpsConnector = new ServerConnector(server, ssl, alpn, http2, http1);
        httpsConnector.setHost("0.0.0.0");
        httpsConnector.setPort(PORT + 1);
        httpsConnector.setIdleTimeout(30000);
        server.addConnector(httpsConnector);

        // HTTP connector
        ServerConnector httpConnector = new ServerConnector(server, http1);
        httpConnector.setHost("0.0.0.0");
        httpConnector.setPort(PORT);
        httpConnector.setIdleTimeout(30000);
        server.addConnector(httpConnector);

        logger.info("Connectors initialized successfully");

        try {
            server.start();
            logger.info("🚀🚀🚀🚀🚀🚀 Server started on HTTP: {} and HTTPS: {}", PORT, PORT + 1);
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}