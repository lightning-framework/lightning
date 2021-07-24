package lightning.server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lightning.config.Config;
import lightning.db.MySQLDatabaseProvider;
import lightning.db.MySQLDatabaseProviderImpl;
import lightning.exceptions.LightningConfigException;
import lightning.inject.InjectorModule;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

public class LightningServer {
  private static final Logger logger = LoggerFactory.getLogger(LightningServer.class);
  private Server server;
  private MySQLDatabaseProvider dbp;

  public LightningServer(Config config) throws Exception {
    this(config, new InjectorModule());
  }

  public LightningServer(Config config, InjectorModule userModule) throws Exception {
    config.validate();

    if (config.autoReloadPrefixes != null &&
        userModule.hasInjectionsIn(config.autoReloadPrefixes)) {
      throw new LightningConfigException("You must not dependency inject classes in your auto-reload packages.");
    }

    server = createServer(config);
    this.dbp = new MySQLDatabaseProviderImpl(config.db);

    if (config.enableDebugMode) {
      logger.warn("Lightning Framework :: NOTICE: You are running this server in DEBUG MODE.");
      logger.warn("Lightning Framework :: NOTICE: Please do not enable debug mode on production systems as it may leak internals.");
      if (config.autoReloadPrefixes != null && !config.autoReloadPrefixes.isEmpty() && !config.isRunningFromJAR()) {
        logger.warn("Lightning Framework :: NOTICE: You have enabled code hot swaps in packages: " + Joiner.on(", ").join(config.autoReloadPrefixes));
      }
    }
    if (config.isRunningFromJAR()) {
      logger.info("Lightning Framework :: NOTICE: You are running this server from a JAR deployment.");
    }

    LightningHandler lightningHandler = new LightningHandler(config, dbp, userModule);
    server.setHandler(lightningHandler);
  }

  public LightningServer start() throws Exception {
    server.start();
    logger.info("Lightning Framework :: Ready for requests!");
    return this;
  }

  public LightningServer join() throws Exception {
    server.join();
    return this;
  }

  public LightningServer stop() throws Exception {
    if (server != null) {
      server.stop();
    }
    return this;
  }

  public MySQLDatabaseProvider getDatabasePool() {
    return dbp;
  }

  private Server createServer(Config config) throws Exception {
    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(config.server.maxQueuedRequests);
    QueuedThreadPool pool = new QueuedThreadPool(config.server.minThreads, config.server.maxThreads, config.server.threadTimeoutMs, queue);
    Server server = new Server(pool);

    if (config.ssl.isEnabled()) {
      if (config.ssl.redirectInsecureRequests) {
        makeConnector(config, server, config.server.port, false);
        logger.info("Lightning Framework :: Binding to HTTP @ {}:{}", config.server.host, config.server.port);
        logger.info("Lightning Framework :: Insecure requests will be redirected to their HTTPS equivalents.");
      }

      logger.info("Lightning Framework :: Binding to HTTPS @ {}:{}", config.server.host, config.ssl.port);
      makeConnector(config, server, config.ssl.port, true);
    } else {
      logger.info("Lightning Framework :: Binding to HTTP @ {}:{}", config.server.host, config.server.port);
      makeConnector(config, server, config.server.port, false);
    }

    server.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", config.server.maxPostBytes);
    server.setAttribute("org.eclipse.jetty.server.Request.maxFormKeys", config.server.maxQueryParams);

    return server;
  }

  private ServerConnector makeConnector(Config config, Server server, int port, boolean isSSL) {
    final HttpConfiguration httpConfig = isSSL ? makeHttpsConfig(config) : makeHttpConfig(config);
    final HttpConnectionFactory http1 = new HttpConnectionFactory(httpConfig);
    HTTP2ServerConnectionFactory http2 = null;
    HTTP2CServerConnectionFactory http2c = null;
    ALPNServerConnectionFactory alpn = null;

    http1.setInputBufferSize(config.server.inputBufferSizeBytes);

    if (config.server.enableHttp2C && !isSSL) {
      http2c = new HTTP2CServerConnectionFactory(httpConfig);
      http2c.setInputBufferSize(config.server.inputBufferSizeBytes);
      http2c.setMaxConcurrentStreams(config.server.http2MaxConcurrentStreams);
      http2c.setInitialStreamRecvWindow(config.server.http2InitialStreamSendWindowBytes);
    }

    String protocol = null;

    if (config.server.enableHttp2 && isSSL) {
      http2 = new HTTP2ServerConnectionFactory(httpConfig);
      http2.setInitialStreamRecvWindow(config.server.http2InitialStreamSendWindowBytes);
      http2.setInputBufferSize(config.server.inputBufferSizeBytes);
      http2.setMaxConcurrentStreams(config.server.http2MaxConcurrentStreams);
      alpn = new ALPNServerConnectionFactory();
      alpn.setDefaultProtocol(http1.getProtocol());
      alpn.setInputBufferSize(config.server.inputBufferSizeBytes);
      protocol = alpn.getProtocol();
    }

    SslContextFactory ssl = isSSL ? makeSslFactory(config, protocol) : null;

    ConnectionFactory[] cfs;

    if (config.server.enableHttp2 && isSSL) {
      cfs = new ConnectionFactory[]{alpn, http2, http1};
    } else if (config.server.enableHttp2C && !isSSL) {
      cfs = new ConnectionFactory[]{http1, http2c};
    } else {
      cfs = new ConnectionFactory[]{http1};
    }

    ServerConnector connector = isSSL
        ?  new ServerConnector(server, ssl, cfs)
        :  new ServerConnector(server, cfs);

    connector.setIdleTimeout(config.server.connectionIdleTimeoutMs);
    connector.setHost(config.server.host);
    connector.setPort(port);
    connector.setAcceptQueueSize(config.server.maxAcceptQueueSize);

    server.addConnector(connector);
    return connector;
  }

  private SslContextFactory makeSslFactory(Config config, String protocol) {
    if (!config.ssl.isEnabled()) {
      return null;
    }

    SslContextFactory ssl = new SslContextFactory.Server();
    ssl.setKeyStorePath(config.ssl.keyStoreFile);

    if (config.ssl.keyStorePassword != null) {
      ssl.setKeyStorePassword(config.ssl.keyStorePassword);
    }

    if (config.ssl.trustStoreFile != null) {
      ssl.setTrustStorePath(config.ssl.trustStoreFile);
    }

    if (config.ssl.trustStorePassword != null) {
      ssl.setTrustStorePassword(config.ssl.trustStorePassword);
    }

    if (config.ssl.keyManagerPassword != null) {
      ssl.setKeyManagerPassword(config.ssl.keyManagerPassword);
    }

    /* Prevent weak protocols: require TLS > 1.0. */
    ssl.addExcludeProtocols("SSL",
                            "SSLv2",
                            "SSLv2Hello",
                            "SSLv3",
                            "TLSv1.0");

    /* Prevent weak cipher suites. */
    ssl.addExcludeCipherSuites(".*_anon_.*",
                               ".*_WITH_NULL.*",
                               ".*_WITH_RC4.*",
                               ".*_DSS_.*",
                               ".*_DES_.*",
                               ".*_SHA$",
                               ".*_MD5$",
                               ".*_SHA1$");

    /* Prevent re-negotiation.*/
    ssl.setRenegotiationAllowed(false);

    if (config.server.enableHttp2) {
      ssl.setCipherComparator(HTTP2Cipher.COMPARATOR);
      ssl.setUseCipherSuitesOrder(true);
      //ssl.setProtocol(protocol);
    }

    return ssl;
  }

  private HttpConfiguration makeHttpsConfig(Config config) {
    HttpConfiguration hc = makeHttpConfig(config);
    hc.setSecurePort(config.ssl.port);
    hc.setSecureScheme("https");
    hc.addCustomizer(new SecureRequestCustomizer());
    return hc;
  }

  private HttpConfiguration makeHttpConfig(Config config) {
    HttpConfiguration hc = new HttpConfiguration();
    hc.setSendServerVersion(false);
    hc.setSendXPoweredBy(false);
    hc.setSendDateHeader(true);
    hc.setPersistentConnectionsEnabled(config.server.enablePersistentConnections);
    hc.setOutputAggregationSize(config.server.outputAggregationSizeBytes);
    hc.setOutputBufferSize(config.server.outputBufferSizeBytes);
    hc.setRequestHeaderSize(config.server.maxRequestHeaderSizeBytes);
    hc.setResponseHeaderSize(config.server.maxResponseHeaderSizeBytes);
    return hc;
  }
}
