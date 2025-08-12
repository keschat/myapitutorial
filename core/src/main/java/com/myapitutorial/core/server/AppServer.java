package com.myapitutorial.core.server;

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

import com.myapitutorial.core.config.AppConfig;
import com.myapitutorial.rest.ApiApplication;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Main class to initialize and start the application server.
 * This class sets up the server with HTTP and HTTPS connectors,
 * configures SSL, and sets up the API context.
 *
 * @link https://northcoder.com/post/jetty-11-secure-connections/
 */
public class AppServer {

    private static final Logger logger = LoggerFactory.getLogger(AppServer.class);

    private static final String ROOT_CONTEXT = "/";
    private static final String API_PATH = "/api/*";

    public void initialize() throws Exception {

        Security.insertProviderAt(new OpenSSLProvider(), 1);

        ServiceLocator serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        
        // Register configuration service
        ServiceLocatorUtilities.bind(serviceLocator, binder -> {
            ServiceLocatorUtilities.addClasses(serviceLocator, com.myapitutorial.core.config.AppConfig.class);
        });
        
        // Get configuration instance
        AppConfig appConfig = serviceLocator.getService(AppConfig.class);
        
        final var SECURE_PORT = appConfig.getInt("server.ssl.port");
        final var KEYSTORE_PATH = appConfig.getString("server.ssl.keystore.path");
        final var KEYSTORE_PASS = appConfig.getString("server.ssl.keystore.password");

        Server server = new Server();

        GracefulHandler gracefulHandler = new GracefulHandler();
        server.setHandler(gracefulHandler);
        // Set the Server stopTimeout to wait at most
        // 10 seconds for existing requests to complete.
        server.setStopTimeout(10_000);

        // The plain HTTP configuration.
        HttpConfiguration httpConfig = http_config();
        // The secure HTTP configuration.
        HttpConfiguration secureConfig = https_config(SECURE_PORT);

        // Connection factories
        SslContextFactory.Server sslCtx = ssl_ctx_factory(KEYSTORE_PATH, KEYSTORE_PASS);
        HttpConnectionFactory https = https_conn_factory(secureConfig);
        HTTP2CServerConnectionFactory http2 = http2_conn_factory(secureConfig);
        ALPNServerConnectionFactory alpn = alpn_conn_factory();
        SslConnectionFactory ssl = ssl_conn_factory(sslCtx, alpn.getProtocol());
        ServerConnector secureConnector = secure_connector(server, ssl, alpn, http2, https, secureConfig.getSecurePort());

        // Second, create the plain connector for HTTP.
        HttpConnectionFactory http = http_conn_factory(httpConfig);
        ServerConnector httpConnector = http_connector(server, http, appConfig.getInt("server.port"));

        server.setConnectors(new Connector[] { secureConnector, httpConnector });
        logger.info("Connectors set successfully");

        // Set up a listener so that when the secure connector starts,
        // it configures the other connectors that have not started yet.
        secureConnector.addEventListener(new NetworkConnector.Listener() {
            @Override
            public void onOpen(NetworkConnector connector) {
                var port = connector.getLocalPort();
                // Configure the plain connector for secure redirects from http to https.
                httpConfig.setSecurePort(port);
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
        apiContext.setContextPath(ROOT_CONTEXT);
        apiContext.setBaseResource(apiContext.newResource(AppServer.class.getClassLoader().getResource("www")));
        apiContext.addServlet(DefaultServlet.class, ROOT_CONTEXT);

        ServletHolder apiServletHolder = apiContext.addServlet(ServletContainer.class, API_PATH);
        apiServletHolder.setInitParameter("jakarta.ws.rs.Application", ApiApplication.class.getName());

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[] { context, apiContext });
        gracefulHandler.setHandler(contexts);

        try {
            server.start();
            logger.info("🚀🚀🚀🚀🚀🚀 Server started on HTTP: {} and HTTPS: {}", httpConnector.getLocalPort(),
                    secureConnector.getLocalPort());
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // private static Server createJettyServer() {
      
    // }

    private static ServerConnector secure_connector(Server server, SslConnectionFactory ssl, ALPNServerConnectionFactory alpn,
        HTTP2CServerConnectionFactory http2, HttpConnectionFactory https, int securePort) {
        var sConn = new ServerConnector(server, 1, 1, ssl, alpn, http2, https);
        sConn.setName("HTTP-Secure");
        sConn.setPort(securePort);
        sConn.setIdleTimeout(30000);
        return sConn;
    }

    private static ServerConnector http_connector(Server server, HttpConnectionFactory http, int httpPort) {
        var pConn = new ServerConnector(server, http);
        pConn.setName("HTTP-Plain");
        pConn.setPort(httpPort);
        pConn.setIdleTimeout(30000);
        return pConn;
    }

    private static SslConnectionFactory ssl_conn_factory(SslContextFactory.Server factory, String nextProtocol) {
        return new SslConnectionFactory(factory, nextProtocol);
    }

    private static ALPNServerConnectionFactory alpn_conn_factory() {
        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        alpn.setDefaultProtocol("h2");
        // alpn.setDefaultProtocol(https.getProtocol());
        return alpn;
    }

    private static HTTP2CServerConnectionFactory http2_conn_factory(HttpConfiguration secureConfig) {
        return new HTTP2CServerConnectionFactory(secureConfig);
    }

    private static HttpConnectionFactory https_conn_factory(HttpConfiguration https_config) {
       return new HttpConnectionFactory(https_config);
    }

    private static HttpConnectionFactory http_conn_factory(HttpConfiguration httpConfig) {
        return new HttpConnectionFactory(httpConfig);
    }

    private static HttpConfiguration https_config(int port) {
        HttpConfiguration config = new HttpConfiguration(http_config());
        config.setSecureScheme("https");
        config.setSecurePort(port);
        config.addCustomizer(secure_req_customizer());
        return config;
    }

    private static HttpConfiguration http_config() {
        HttpConfiguration config = new HttpConfiguration();
        config.setSendServerVersion(true);
        config.setOutputBufferSize(32768);
        config.setSendXPoweredBy(true);
        config.setSendDateHeader(false);
        return config;
    }

    private static SecureRequestCustomizer secure_req_customizer() {
        // For the SecureRequestCustomizer, Jetty defaults are:
        //  - isSniRequired()          -> false
        //  - isSniHostCheck()         -> true
        //  - getStsMaxAge()           -> -1 (no max age)
        //  - isStsIncludeSubDomains() -> false
        var sReqCust = new SecureRequestCustomizer();
        sReqCust.setStsMaxAge(31536000); // 1 year in seconds
        sReqCust.setSniHostCheck(false);
        sReqCust.setSniRequired(false);
        return sReqCust; // just use the defaults.
    }

    private static SslContextFactory.Server ssl_ctx_factory(String path, String pass) {
        var sslCtx = new SslContextFactory.Server();
        sslCtx.setKeyStorePath(path);
        sslCtx.setKeyStorePassword(pass);
        sslCtx.setKeyManagerPassword(pass);
        // sslCtx.setCipherComparator(HTTP2Cipher.COMPARATOR);
        // These are weak, according to Jetty and https://www.ssllabs.com/ssltest/analyze.html:
        // sslCtx.setExcludeCipherSuites(
        //         "^TLS_RSA_.*$",
        //         "^.*_RSA_.*_(MD5|SHA|SHA1)$",
        //         "^.*_DHE_RSA_.*$",
        //         "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
        //         "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
        //         "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
        //         "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
        //         "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
        //         "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
        //         "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
        //         "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"
        // );

        return sslCtx;
    }
}