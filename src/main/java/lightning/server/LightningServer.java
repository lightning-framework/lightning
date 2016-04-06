package lightning.server;

import java.lang.reflect.Method;

import lightning.ann.WebSocketFactory;
import lightning.config.Config;
import lightning.db.MySQLDatabaseProvider;
import lightning.inject.Injector;
import lightning.inject.InjectorModule;
import lightning.scanner.ScanResult;
import lightning.scanner.Scanner;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
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
  private ServerConnector connector;
    
  public void configure(Config config, MySQLDatabaseProvider dbp, InjectorModule userModule) throws Exception {
    server = new Server(new QueuedThreadPool(config.server.minThreads, config.server.maxThreads, config.server.threadTimeoutMs));
    
    if (config.ssl.isEnabled()) {
      SslContextFactory sslContextFactory = new SslContextFactory(config.ssl.keyStoreFile);

      if (config.ssl.keyStorePassword != null) {
          sslContextFactory.setKeyStorePassword(config.ssl.keyStorePassword);
      }

      if (config.ssl.trustStoreFile != null) {
          sslContextFactory.setTrustStorePath(config.ssl.trustStoreFile);
      }

      if (config.ssl.trustStorePassword != null) {
          sslContextFactory.setTrustStorePassword(config.ssl.trustStorePassword);
      }
      
      if (config.ssl.redirectInsecureRequests) {
        ServerConnector connector2 = new ServerConnector(server);
        connector2.setIdleTimeout(config.server.connectionIdleTimeoutMs);
        connector2.setSoLingerTime(-1);
        connector2.setHost(config.server.host);
        connector2.setPort(config.server.port);
        server.addConnector(connector2);    
      }
      
      connector = new ServerConnector(server, sslContextFactory);
    } else {
      connector = new ServerConnector(server);
    }
    
    connector.setIdleTimeout(config.server.connectionIdleTimeoutMs);
    connector.setSoLingerTime(-1);
    connector.setHost(config.server.host);
    connector.setPort(config.ssl.isEnabled() ? config.ssl.port : config.server.port);
    server.addConnector(connector);    
    
    // TODO(mschurr): Expose additional Jetty options if needed.
    server.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", config.server.maxPostBytes);
    server.setAttribute("org.eclipse.jetty.server.Request.maxFormKeys", config.server.maxQueryParams);
    
    for(Connector y : server.getConnectors()) {
      for(ConnectionFactory x  : y.getConnectionFactories()) {
          if(x instanceof HttpConnectionFactory) {
              ((HttpConnectionFactory)x).getHttpConfiguration().setSendServerVersion(false);
              ((HttpConnectionFactory)x).getHttpConfiguration().setSendXPoweredBy(false);
          }
      }
    }
        
    ServletContextHandler websocketHandler = new ServletContextHandler(null, "/", false, false);
    WebSocketUpgradeFilter websocketFilter = WebSocketUpgradeFilter.configureContext(websocketHandler);
    websocketFilter.getFactory().getPolicy().setIdleTimeout(config.server.websocketTimeoutMs);
    
    Scanner scanner = new Scanner(config.autoReloadPrefixes, config.scanPrefixes, config.enableDebugMode);
    ScanResult result = scanner.scan();
    
    InjectorModule globalModule = new InjectorModule();
    globalModule.bindClassToInstance(Config.class, config);
    globalModule.bindClassToInstance(MySQLDatabaseProvider.class, dbp);
    
    for (Class<?> clazz : result.websocketFactories.keySet()) {
      for (Method m : result.websocketFactories.get(clazz)) {
        WebSocketFactory info = m.getAnnotation(WebSocketFactory.class);
        logger.debug("Installed WS Handler: {} -> {}", info.path(), m);
        WebSocketCreator creator = new WebSocketCreator() {
          @Override
          public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse res) {
            try {
              // TODO: Better dependency injection support - maybe with Guice?
              return m.invoke(null, new Injector(globalModule, userModule).getInjectedArguments(m));
            } catch (Exception e) {
              logger.warn("Failed to create websocket:", e);
              return null;
            }
          }
        };
        websocketFilter.addMapping(new ServletPathSpec(info.path()), creator);
      }
    }
    
    HandlerCollection handlers = new HandlerCollection();
    handlers.addHandler(new LightningHandler(config, dbp, globalModule, userModule));
    handlers.addHandler(websocketHandler);
    server.setHandler(handlers);
  }
  
  public void start() throws Exception {
    server.start();
    server.join();
  }
  
  public void stop() throws Exception {
    if (server != null) {
      server.stop();
    }
  }
}
