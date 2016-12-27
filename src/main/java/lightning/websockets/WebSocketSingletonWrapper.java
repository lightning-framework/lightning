package lightning.websockets;

import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lightning.config.Config;
import lightning.inject.Injector;
import lightning.scanner.Snapshot;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket
public final class WebSocketSingletonWrapper {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketSingletonWrapper.class);
  private final Config config;
  private WebSocketHandler handler;
  private Set<Session> sessions;
  private Snapshot snapshot;
  private volatile boolean isCodeChanged;
  
  public WebSocketSingletonWrapper(Config config, Injector injector, Class<? extends WebSocketHandler> type) throws Exception {
    this.config = config;
    this.handler = injector.newInstance(type);
    this.sessions = config.enableDebugMode ? Collections.newSetFromMap(new ConcurrentHashMap<>()) : null;
    this.snapshot = config.enableDebugMode ? Snapshot.capture(config.autoReloadPrefixes) : null;
    this.isCodeChanged = false;
  }
  
  public boolean isCodeChanged() throws Exception {
    if (config.enableDebugMode) {
      synchronized (this) {
        if (this.isCodeChanged) {
          return this.isCodeChanged;
        }
        
        if (config.canReloadClass(handler.getClass()) &&
            !Snapshot.capture(config.autoReloadPrefixes).equals(snapshot)) {
          this.isCodeChanged = true;
        }
        
        return this.isCodeChanged;
      }
    }
    
    return false;
  }  
  
  private void unregisterSession(Session session) {
    if (config.enableDebugMode) {
      synchronized (this) {
        sessions.remove(session);
      }
    }
  }
  
  private void registerSession(Session session) {
    if (config.enableDebugMode) {
      synchronized (this) {
        sessions.add(session);
      }
    }
  }
  
  private void dropAllConnections() {
    synchronized (this) {
      for (Session session : sessions) {
          // This also fires the OnWebSocketClose handler.
          session.close(1011 /* UNEXPECTED_CONDITION */, "");
      }
      
      sessions.removeIf((session) -> true);
    }
  }
    
  public boolean shouldAccept(ServletUpgradeRequest request, ServletUpgradeResponse response) throws Exception {
    // We should never encounter a code change here since re-scans occur in LightningHandler for each incoming request.
    return handler.shouldAccept(request, response);
  }
  
  @OnWebSocketConnect
  public void connected(Session session) {
    // We should never encounter a code change here since re-scans occur in LightningHandler for each incoming request.
    registerSession(session);
    try {
      handler.onConnected(session);
    } catch (Exception e) {
      error(session, e);
      session.close(1011, "");
    }
  }

  @OnWebSocketClose
  public void closed(Session session, int status, String reason) {
    try {
      if (config.enableDebugMode) {
        synchronized (this) {
          if (sessions.contains(session)) {
            unregisterSession(session);
            handler.onClose(session, status, reason);
          }
        }
      } else {
        handler.onClose(session, status, reason);
      }
    } catch (Exception e) {
      LOGGER.warn("Exception during websocket close handler: ", e);
    }
  }

  @OnWebSocketError
  public void error(Session session, Throwable error) {
    try {
      if (config.enableDebugMode) {
        synchronized (this) {
          if (sessions.contains(session)) {
            handler.onError(session, error);
          }
        }
      } else {
        handler.onError(session, error);
      }
    } catch (Exception e) {
      // Session will be closed.
      LOGGER.warn("Exception during web socket error handler:", e);
    }
  }

  @OnWebSocketMessage
  public void messageText(Session session, String message) {
    if (config.enableDebugMode) {
      synchronized (this) {
        try {
          if (isCodeChanged()) {
            dropAllConnections();
            return;          
          }
          
          if (sessions.contains(session)) {
            handler.onTextMessage(session, message);
          }
        } catch (Exception e) {
          error(session, e);
          session.close(1011, "");
        }
      }
    } else {
      try {
        handler.onTextMessage(session, message);
      } catch (Exception e) {
        error(session, e);
        session.close(1011, "");
      }
    }
  }
 
  @OnWebSocketMessage
  public void messageBinary(final Session session, InputStream message) {
    if (config.enableDebugMode) {
      synchronized (this) {
        try {
          if (isCodeChanged()) {
            dropAllConnections();
            return;          
          }
          
          if (sessions.contains(session)) {
            handler.onBinaryMessage(session, message);
          }
        } catch (Exception e) {
          error(session, e);
          session.close(1011, "");
        }
      }
    } else {
      try {
        handler.onBinaryMessage(session, message);
      } catch (Exception e) {
        error(session, e);
        session.close(1011, "");
      }
    }
  }
}
