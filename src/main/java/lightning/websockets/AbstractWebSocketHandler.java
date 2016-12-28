package lightning.websockets;

import java.io.InputStream;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractWebSocketHandler implements WebSocketHandler {
  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractWebSocketHandler.class);
  
  /**
   * Adds default: accept all connections.
   */
  @Override
  public boolean shouldAccept(ServletUpgradeRequest request, ServletUpgradeResponse response) throws Exception {
    return true;
  }

  /**
   * Adds default: do nothing.
   */
  @Override
  public void onClose(Session session, int status, String reason) throws Exception {}

  /**
   * Adds default: do nothing.
   */
  @Override
  public void onConnected(Session session) throws Exception {}

  /**
   * Adds default: reject and close session with code 1003 (message type not supported).
   */
  @Override
  public void onTextMessage(Session session, String message) throws Exception {
    session.close(1003, "Unsupported Message Type");
  }

  /**
   * Adds default: reject and close session with code 1003 (message type not supported).
   */
  @Override
  public void onBinaryMessage(Session session, InputStream message) throws Exception {
    session.close(1003, "Unsupported Message Type");
  }

  /**
   * Adds default: log the error.
   */
  @Override
  public void onError(Session session, Throwable error) throws Exception {
    LOGGER.warn("Error occured on WebSocket {}:", getClass(), error);
  }
}
