package lightning.server;

import java.lang.reflect.Method;

import lightning.ann.WebSocketFactory;
import lightning.config.Config;
import lightning.db.MySQLDatabaseProvider;
import lightning.inject.Injector;
import lightning.inject.InjectorModule;
import lightning.scanner.ScanResult;
import lightning.scanner.Scanner;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NegotiatingServerConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;
import org.eclipse.jetty.websocket.server.pathmap.ServletPathSpec;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LightningServer {
  private static final Logger logger = LoggerFactory.getLogger(LightningServer.class);
  private Server server;
    
  public void configure(Config config, MySQLDatabaseProvider dbp, InjectorModule userModule) throws Exception {
    server = createServer(config);
        
    ServletContextHandler websocketHandler = new ServletContextHandler(null, "/", false, false);
    WebSocketUpgradeFilter websocketFilter = WebSocketUpgradeFilter.configureContext(websocketHandler);
    websocketFilter.getFactory().getPolicy().setIdleTimeout(config.server.websocketTimeoutMs);
    
    Scanner scanner = new Scanner(config.autoReloadPrefixes, config.scanPrefixes, config.enableDebugMode);
    ScanResult result = scanner.scan();
    
    InjectorModule globalModule = new InjectorModule();
    globalModule.bindClassToInstance(Config.class, config);
    globalModule.bindClassToInstance(MySQLDatabaseProvider.class, dbp);
    
    boolean hasWebSockets = false;
    
    for (Class<?> clazz : result.websocketFactories.keySet()) {
      for (Method m : result.websocketFactories.get(clazz)) {
        WebSocketFactory info = m.getAnnotation(WebSocketFactory.class);
        // TODO: Should verify path contains no wildcards, parameters.
        logger.info("Lightning Framework :: Registered Web Socket @ {} -> {}", info.path(), m);
        hasWebSockets = true;
        WebSocketCreator creator = new WebSocketCreator() {
          @Override
          public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse res) {
            try {
              InjectorModule wsModule = new InjectorModule();
              wsModule.bindClassToInstance(ServletUpgradeRequest.class, req);
              wsModule.bindClassToInstance(ServletUpgradeResponse.class, res);
              return m.invoke(null, new Injector(globalModule, userModule, wsModule).getInjectedArguments(m));
            } catch (Exception e) {
              logger.warn("Failed to create websocket:", e);
              return null;
            }
          }
        };
        websocketFilter.addMapping(new ServletPathSpec(info.path()), creator);
      }
    }
    
    if (config.enableDebugMode) {
      logger.warn("Lightning Framework :: NOTICE: You are running this server in DEBUG MODE.");
      logger.warn("Lightning Framework :: Please do not enable debug mode on production systems as it may leak internals.");
      if (hasWebSockets) {
        logger.warn("Lightning Framework :: NOTICE: Websocket handlers can not be automatically reloaded; you will need to "
            + "restart the server to load websocket handler code changes.");
      }
    }
    
    HandlerCollection handlers = new HandlerCollection();
    handlers.addHandler(new LightningHandler(config, dbp, globalModule, userModule));
    handlers.addHandler(websocketHandler);
    server.setHandler(handlers);
  }
  
  public void start() throws Exception {
    server.start();
    logger.info("Lightning Framework :: Ready for requests!");
    server.join();
  }
  
  public void stop() throws Exception {
    if (server != null) {
      server.stop();
    }
  }
  
  private Server createServer(Config config) throws Exception {
    Server server = new Server(new QueuedThreadPool(config.server.minThreads, config.server.maxThreads, config.server.threadTimeoutMs));
        
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
    
    // TODO(mschurr): Expose additional Jetty options if needed.
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
    
    if (config.server.enableHttp2) {
      http2 = new HTTP2ServerConnectionFactory(httpConfig);
      http2c = new HTTP2CServerConnectionFactory(httpConfig);
      NegotiatingServerConnectionFactory.checkProtocolNegotiationAvailable();
      alpn = new ALPNServerConnectionFactory();
      alpn.setDefaultProtocol(http1.getProtocol());
    }
    
    final SslContextFactory ssl = makeSslFactory(config, config.server.enableHttp2 ? alpn.getProtocol() : null);
    
    ConnectionFactory[] cfs = new ConnectionFactory[]{http1};
    
    if (config.server.enableHttp2) {
      if (isSSL) {
        cfs = new ConnectionFactory[]{alpn, http2, http1};
      } else {
        cfs = new ConnectionFactory[]{http1, http2c};
      }
    }
    
    ServerConnector connector = isSSL
        ?  new ServerConnector(server, ssl, cfs)
        :  new ServerConnector(server, cfs);
        
    connector.setIdleTimeout(config.server.connectionIdleTimeoutMs);
    connector.setSoLingerTime(-1);
    connector.setHost(config.server.host);
    connector.setPort(port);
    server.addConnector(connector);
    
    return connector;
  }
  
  private SslContextFactory makeSslFactory(Config config, String protocol) {
    if (!config.ssl.isEnabled()) {
      return null;
    }
    
    SslContextFactory ssl = new SslContextFactory(config.ssl.keyStoreFile);
    
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
    return hc;
  }
}
