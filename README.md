# myapitutorial
java API tutorial using jetty server

# setting application config
src/main/resources/
├── application.yml          # Base/fallback config
├── application-dev.yml      # Development config
├── application-prod.yml     # Production config
└── application-staging.yml  # Staging config (optional)

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