package lightning.server;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;

import lightning.config.Config;
import lightning.db.MySQLDatabaseProvider;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;

public class LightningServer {
  private Server server;
  private ServerConnector connector;
    
  public void configure(Config config, MySQLDatabaseProvider dbp) throws Exception {
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
    
    for(Connector y : server.getConnectors()) {
      for(ConnectionFactory x  : y.getConnectionFactories()) {
          if(x instanceof HttpConnectionFactory) {
              ((HttpConnectionFactory)x).getHttpConfiguration().setSendServerVersion(false);
              ((HttpConnectionFactory)x).getHttpConfiguration().setSendXPoweredBy(false);
          }
      }
    }
    
    ServletContextHandler httpHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
    httpHandler.setResourceBase("/");
    httpHandler.setMaxFormContentSize(config.server.maxPostBytes);
    httpHandler.setMaxFormKeys(config.server.maxQueryParams);
    httpHandler.setAttribute("lightning_cfg", config);
    httpHandler.setAttribute("lightning_dbp", dbp);
    
    /*ServletHolder ssh = new ServletHolder("default", DefaultServlet.class);
    ssh.setInitParameter("acceptRanges", "true");
    ssh.setInitParameter("dirAllowed", "false");
    ssh.setInitParameter("welcomeServlets", "false");
    ssh.setInitParameter("redirectWelcome", "false");
    ssh.setInitParameter("gzip", "true");
    ssh.setInitParameter("etags", "true");
    ssh.setInitParameter("cacheControl", "public, max-age=3600, must-revalidate");
    ssh.setInitParameter("useFileMappedBuffer", "true");
    ssh.setInitParameter("maxCachedFiles", "500");
    ssh.setInitParameter("maxCachedFileSize", "100000");
    ssh.setInitParameter("maxCacheSize", "5000000");
    ssh.setInitParameter("pathInfoOnly", "true");
    ssh.setInitParameter("resourceBase", config.server.staticFilesPath);
    httpHandler.addServlet(ssh, "/*");*/
    
    ServletHolder ssh = new ServletHolder("lightning", LightningServlet.class);
    httpHandler.addServlet(ssh, "/*");
    
    ServletContextHandler websocketHandler = new ServletContextHandler(null, "/", false, false);
    WebSocketUpgradeFilter websocketFilter = WebSocketUpgradeFilter.configureContext(websocketHandler);
    websocketFilter.getFactory().getPolicy().setIdleTimeout(config.server.websocketTimeoutMs);
    
    // TODO: Websockets
    // TODO: Filter for redirecting HTTP to HTTPS
    
     /*for (String path : webSocketHandlers.keySet()) {
        WebSocketCreator webSocketCreator = WebSocketCreatorFactory.create(webSocketHandlers.get(path));
        webSocketUpgradeFilter.addMapping(new ServletPathSpec(path), webSocketCreator);
        
    }*/
     
    
    /*FilterHolder fh = new FilterHolder(new Filter() {
      @Override
      public void destroy() {
        // TODO Auto-generated method stub
      }

      @Override
      public void doFilter(ServletRequest _req, ServletResponse _res, FilterChain chain)
          throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) _req;
        HttpServletResponse res = (HttpServletResponse) _res;
        // IF SSL AND REDIRECT SSL -> REDIRECT
        
        if (req.getPathInfo().equals("/hi")) {
          res.getWriter().println("OMG!");
        } else {
          chain.doFilter(_req, _res);
        }
      }

      @Override
      public void init(FilterConfig arg0) throws ServletException {
        // TODO Auto-generated method stub
        
      }
    });
    httpHandler.addFilter(fh, "/*", EnumSet.of(DispatcherType.REQUEST));*/
    
    httpHandler.setErrorHandler(new ErrorHandler() {
      @Override
      protected void handleErrorPage(HttpServletRequest request, Writer writer, int code, String message) throws IOException {
        writer.write(String.format("%d %s", code, message));
      }
    });
    
    HandlerCollection handlers = new HandlerCollection();
    handlers.addHandler(httpHandler);
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
