package com.myapitutorial.server;

import java.io.InputStream;
import java.security.Security;
import java.util.Map;
import java.util.Optional;

import org.conscrypt.OpenSSLProvider;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.GracefulHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.myapitutorial.rest.ApiApplication;

import io.github.cdimascio.dotenv.Dotenv;

public class Main {

    final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {

        Security.insertProviderAt(new OpenSSLProvider(), 1);

        // Load environment-specific configuration
        String environment = Optional.ofNullable(System.getenv("APP_ENV"))
                .orElse(Optional.ofNullable(System.getProperty("app.env"))
                .orElse("dev"));
        
        String configFile = "application-" + environment + ".yml";
        logger.info("Loading configuration for environment: {} from {}", environment, configFile);
        
        Yaml yaml = new Yaml();
        InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(configFile);
        
        if (inputStream == null) {
            logger.warn("Configuration file {} not found, falling back to application.yml", configFile);
            inputStream = Main.class.getClassLoader().getResourceAsStream("application.yml");
        }
        
        Map<String, Object> config = null;
        if (inputStream != null) {
            config = yaml.load(inputStream);
            logger.info("Loaded configuration: {}", config);
        } else {
            logger.error("No configuration file found!");
        }
        
        ServiceLocator serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        System.out.println(serviceLocator);

        Server server = new Server();

        Dotenv dotenv = Dotenv.load();

        int PORT = Optional.ofNullable(System.getProperty("PORT")).map(Integer::parseInt)
                .orElse(Integer.parseInt(dotenv.get("PORT")));

        GracefulHandler gracefulHandler = new GracefulHandler();
        server.setHandler(gracefulHandler);

        // Set the Server stopTimeout to wait at most
        // 10 seconds for existing requests to complete.
        server.setStopTimeout(10_000);

        // Http configuration
        // This is used to configure the HTTP and HTTPS connectors
        // and to set up the SSL context for secure connections.
        // It includes settings for server version, secure scheme, secure port,
        // output buffer size, and whether to send the X-Powered-By header.
        // The SecureRequestCustomizer is used to customize the secure request handling.
        // The HttpConfiguration is then used to create connection factories for
        // HTTP/1.1 and
        // HTTP/2 over cleartext (HTTP/2C) protocols, as well as
        // an ALPN (Application-Layer Protocol Negotiation) connection factory for
        // handling
        // protocol negotiation between HTTP/1.1 and HTTP/2.

        // The plain HTTP configuration.
        HttpConfiguration plainConfig = new HttpConfiguration();
        plainConfig.setSendServerVersion(true);
        plainConfig.setOutputBufferSize(32768);
        plainConfig.setSendXPoweredBy(true);
        plainConfig.setSendDateHeader(false);

        // The secure HTTP configuration.
        HttpConfiguration secureConfig = new HttpConfiguration(plainConfig);
        secureConfig.setSecureScheme("https");
        secureConfig.setSecurePort(PORT);
        SecureRequestCustomizer secureRequestCustomizer = new SecureRequestCustomizer();
        secureRequestCustomizer.setStsMaxAge(31536000); // 1 year in seconds
        secureRequestCustomizer.setSniHostCheck(false);
        secureRequestCustomizer.setSniRequired(false);
        secureConfig.addCustomizer(secureRequestCustomizer);

        // Connection factories
        // These are used to create the connectors for the HTTP and HTTPS servers.
        // They are used to handle incoming requests and to manage the
        // communication between the server and the client.
        // The HttpConnectionFactory is used for HTTP/1.1 connections,
        // while the HTTP2CServerConnectionFactory is used for HTTP/2 connections over
        // cleartext.
        // The ALPNServerConnectionFactory is used to negotiate the protocol between
        // HTTP/1.1 and HTTP/2.
        // The SslConnectionFactory is used to handle SSL/TLS connections,
        // and it is configured with the SslContextFactory that contains the SSL
        // settings
        // such as the keystore path and password.
        // The server is then configured with two connectors: one for HTTP and one for
        // HTTPS.
        SslContextFactory.Server sslCtx = new SslContextFactory.Server();
        sslCtx.setKeyStorePath("keystore.p12");
        sslCtx.setKeyStorePassword("pass123");
        sslCtx.setKeyManagerPassword("pass123");

        // First, create the secure connector for HTTPS and HTTP/2.
        HttpConnectionFactory https = new HttpConnectionFactory(secureConfig);
        HTTP2CServerConnectionFactory http2 = new HTTP2CServerConnectionFactory(secureConfig);
        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        alpn.setDefaultProtocol(https.getProtocol());
        SslConnectionFactory ssl = new SslConnectionFactory(sslCtx, alpn.getProtocol());
        ServerConnector secureConnector = new ServerConnector(server, 1, 1, ssl, alpn, http2, https);
        secureConnector.setName("HTTP-Secure");
        secureConnector.setPort(secureConfig.getSecurePort());
        secureConnector.setIdleTimeout(30000);

        // Second, create the plain connector for HTTP.
        HttpConnectionFactory http = new HttpConnectionFactory(plainConfig);
        ServerConnector plainConnector = new ServerConnector(server, 1, 1, http);
        plainConnector.setName("HTTP-Plain");
        plainConnector.setPort(9090);
        plainConnector.setIdleTimeout(30000);

        server.setConnectors(new Connector[] { secureConnector, plainConnector });
        logger.info("Connectors initialized successfully");

        // Set up a listener so that when the secure connector starts,
        // it configures the other connectors that have not started yet.
        secureConnector.addEventListener(new NetworkConnector.Listener() {
            @Override
            public void onOpen(NetworkConnector connector) {
                int port = connector.getLocalPort();

                // Configure the plain connector for secure redirects from http to https.
                plainConfig.setSecurePort(port);
            }
        });

        ContextHandler context = new ContextHandler(new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) {
                callback.succeeded();
                return true;
            }
        }, "/blank");

        ServletContextHandler apiContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        apiContext.setContextPath("/");
        apiContext.setBaseResource(apiContext.newResource(Main.class.getClassLoader().getResource("www")));
        apiContext.addServlet(DefaultServlet.class, "/");

        ServletHolder apiServletHolder = apiContext.addServlet(ServletContainer.class, "/api/*");
        apiServletHolder.setInitParameter("jakarta.ws.rs.Application", ApiApplication.class.getName());

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[] { context, apiContext });
        gracefulHandler.setHandler(contexts);

        try {
            server.start();
            logger.info("🚀🚀🚀🚀🚀🚀 Server started on HTTP: {} and HTTPS: {}", plainConnector.getLocalPort(),
                    secureConnector.getLocalPort());
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}