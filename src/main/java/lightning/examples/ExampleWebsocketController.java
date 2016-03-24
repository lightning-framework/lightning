package lightning.examples;

import lightning.mvc.WebSocketController;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocketController(path="/ws")
@WebSocket
public class ExampleWebsocketController {
  public ExampleWebsocketController() {
    // Initialization; Spark doesn't allow custom constructors for web sockets.
  }
  
  @OnWebSocketConnect
  public void connected(Session session) {
    
  }

  @OnWebSocketClose
  public void closed(Session session, int statusCode, String reason) {
    
  }

  @OnWebSocketMessage
  public void message(Session session, String message) {
    
  }
  
  @OnWebSocketError
  public void error(Session session, Throwable error) {
    
  }
}
