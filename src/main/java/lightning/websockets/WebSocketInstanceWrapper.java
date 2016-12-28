package lightning.websockets;

import java.io.InputStream;

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
public final class WebSocketInstanceWrapper {
  private final Logger LOGGER = LoggerFactory.getLogger(WebSocketInstanceWrapper.class);
  private final Config config;
  private WebSocketHandler handler;
  private Session session;
  private Snapshot snapshot;
  
  public WebSocketInstanceWrapper(Config config, Injector injector, Class<? extends WebSocketHandler> type) throws Exception {
    this.config = config;
    this.handler = injector.newInstance(type);
    this.session = null;
    this.snapshot = config.enableDebugMode ? Snapshot.capture(config.autoReloadPrefixes) : null;
  }
  
  @OnWebSocketConnect
  public void connected(Session session) {
    try {
      if (!isCodeChanged()) {
        this.session = session;
        handler.onConnected(session);
      } else {
        session.close(1011, "");
      }
    } catch (Exception e) {
      error(session, e);
      session.close(1011, "");
    }
  }

  @OnWebSocketClose
  public void closed(Session session, int status, String reason) {
    try {
      if (this.session != null) {
        this.session = null;
        handler.onClose(session, status, reason);
      }
    } catch (Exception e) {
      // The session is closed already.
      // Suppress the exception and log it.
      LOGGER.warn("Exception in web socket close handler:", e);
    }
  }

  @OnWebSocketError
  public void error(Session session, Throwable error) {
    try {
      handler.onError(session, error);
    } catch (Exception e) {
      // The session is going to be closed anyways.
      // Suppress the exception and log it.
      LOGGER.warn("Exception in web socket error handler: ", e);
    }
  }

  @OnWebSocketMessage
  public void messageText(Session session, String message) {
    try {
      if (!isCodeChanged()) {
        handler.onTextMessage(session, message);
      } else {
        session.close(1011, "");
      }
    } catch (Exception e) {
      error(session, e);
      session.close(1011, "");
    }
  }
 
  @OnWebSocketMessage
  public void messageBinary(final Session session, InputStream message) {
    try {
      if (!isCodeChanged()) {
        handler.onBinaryMessage(session, message);
      } else {
        session.close(1011, "");
      }
    } catch (Exception e) {
      error(session, e);
      session.close(1011, "");
    }
  }
  
  private boolean isCodeChanged() throws Exception {
    return config.canReloadClass(handler.getClass()) && 
           !Snapshot.capture(config.autoReloadPrefixes).equals(snapshot);
  }
  
  public boolean shouldAccept(ServletUpgradeRequest request, ServletUpgradeResponse response) throws Exception {
    return handler.shouldAccept(request, response);
  }
}
