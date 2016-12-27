package lightning.websockets;

import java.io.InputStream;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;

/**
 * See lightning.ann.WebSocket for more information.
 */
public interface WebSocketHandler {
  /**
   * Invoked when a web socket upgrade request is received by the server.
   * You may utilize this method to influence subprotocol selection, bypass origin validation,
   * inspect extra HTTP headers, or specify a custom response code.
   * @param request The web socket upgrade request.
   * @param response The web socket upgrade response (not yet finalized).
   * @return True if the upgrade should be accepted, false otherwise
   * @throws Exception if something goes wrong (equivalent to returning false)
   */
  public boolean shouldAccept(ServletUpgradeRequest request, ServletUpgradeResponse response) throws Exception;
  
  /**
   * Invoked when a web socket session is terminated.
   * This method will be invoked EXACTLY ONCE for every session passed to onConnected.
   * This method is still called if onConnected throws an exception.
   * This method will still be invoked if the close was initiated server-side.
   * @param session The session that was terminated
   * @param status The status code (see RFC 6455) received/sent on the wire
   * @param reason The reason (see RFC 6455) received/sent on the wire (optional)
   * @throws Exception If an error occurs processing the event (will be logged)
   */
  public void onClose(Session session, int status, String reason) throws Exception;
  
  /**
   * Invoked EXACTLY ONCE for each opened connection.
   * @param session The session that connected
   * @throws Exception If an error occurs (will cause closure of connection)
   */
  public void onConnected(Session session) throws Exception;
  
  /**
   * Invoked upon receiving a text message.
   * @param session The session that received a message.
   * @param message The message received.
   * @throws Exception If an error occurs (will cause closure of connection)
   */
  public void onTextMessage(Session session, String message) throws Exception;
  
  /**
   * Invoked upon receiving a binary message.
   * @param session The session that received a message.
   * @param message The message received.
   * @throws Exception If an error occurs (will cause closure of connection)
   */
  public void onBinaryMessage(Session session, InputStream message) throws Exception;
  
  /**
   * Invoked when something goes wrong internally in the web socket implementation.
   * An error results in the session being closed (and onClose will ALWAYS be invoked after).
   * This handler is simply invoked to inform you of the error (e.g. for logging).
   * A reasonable default implementation is to do nothing or log the exception.
   * @param session The session that experienced an error
   * @param error The error that occurred
   * @throws Exception If an error occurs processing the event (will be logged)
   */
  public void onError(Session session, Throwable error) throws Exception;
}
