package lightning.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
/**
 * Produces an instance of a websocket handler and installs that
 * handler on the given path.
 * 
 * An example method signature might be:
 *   public static ExampleWebsocket produce(Config config, MySQLDatabaseProvider db) throws Exception {
 *     return new ExampleWebsocket(config, db);
 *   }
 *   
 * The websocket factory method is INJECTABLE (see @Route). In addition to all of the standard and
 * user injected type, HttpServletUpgradeRequest and HttpServletUpgradeResponse are injectable.
 * 
 * The class of the returned object must be annotated with @WebSocket
 * and must contain methods annotated with (see ExampleWebSocket):
 *   @OnWebSocketConnect 
 *   @OnWebSocketClose 
 *   @OnWebSocketMessage 
 *   @OnWebSocketError
 *   
 * This is consistent with the Jetty websocket API.
 * Note that websockets are SINGLETONS. That is, a single instance of your
 * class will be allocated to handle multiple incoming connections.
 * Note that websockets will not be reloaded when debug mode is enabled
 * due to limitations in Jetty (you'll need to restart the server to see
 * changes reflected).
 * 
 * For an example:
 * @see lightning.examples.websockets.ExampleWebsocket
 */
public @interface WebSocketFactory {
  // Specifies the path on which the websocket will be installed.
  String path();
}
