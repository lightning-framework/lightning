package lightning.examples.websockets;

import java.io.IOException;

import lightning.Lightning;
import lightning.ann.WebSocketFactory;
import lightning.config.Config;
import lightning.util.Flags;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * A web socket is a class which handles incoming requests through
 * event handlers.
 */
@WebSocket
public class ExampleWebsocket {
  private final static Logger logger = LoggerFactory.getLogger(ExampleWebsocket.class);
  
  public static void main(String[] args) throws Exception {
    Flags.parse(args);
    Config config = new Config();
    config.scanPrefixes = ImmutableList.of("lightning.examples.websockets");
    config.server.hmacKey = "ABCDEFG";
    config.server.staticFilesPath = ".";
    config.server.templateFilesPath = ".";
    //config.server.enableHttp2 = true;
    //config.ssl.keyStoreFile = "";
    //config.ssl.keyStorePassword = "";
    Lightning.launch(config);
  }
  
  // Returns an instance of the web socket for the incoming connection.
  // This method is injectable - see the documentation for @WebSocketFactory.
  @WebSocketFactory(path = "/mywebsocket")
  public static ExampleWebsocket produce() {
    return new ExampleWebsocket();
  }

  public ExampleWebsocket() {
    logger.info("Websocket Created.");
  }
  
  @OnWebSocketConnect
  public void connected(final Session session) throws IOException {
    logger.info("Connected: {}", session.getRemoteAddress().toString());
    session.getRemote().sendString("HELLO!");
  }

  @OnWebSocketClose
  public void closed(final Session session, final int statusCode, final String reason) {
    logger.info("Disconnected: {} ({} - {})", session.getRemoteAddress().toString(), statusCode, reason);
  }

  @OnWebSocketMessage
  public void message(final Session session, String message) throws IOException {
    logger.info("Received: {} -> {}", session.getRemoteAddress().toString(), message);
    session.getRemote().sendString("THANKS!");
  }
  
  @OnWebSocketError
  public void error(final Session session, Throwable error) {
    logger.info("Error: " + session.getRemoteAddress().toString(), error);
  }
}
