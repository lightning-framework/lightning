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
 * Annotates a public static factory method that produces @WebSocket objects in response to upgrade requests.
 * Lightning will automatically install the factory of the given path.
 * The method must return an instance of @WebSocket when invoked.
 * The method will be invoked once for every incoming upgrade request. You may choose to return a
 * singleton instance each time, or to have a separate instance per socket connection.
 * 
 * An example method signature might be:
 *   public static ExampleWebsocket produce(Config config, MySQLDatabaseProvider db) throws Exception {
 *     return new ExampleWebsocket(config, db);
 *   }
 *   
 * The websocket factory method is INJECTABLE (see @Route). In addition to all of the standard and
 * user injected types, HttpServletUpgradeRequest and HttpServletUpgradeResponse are injectable.
 * 
 * The class of the returned object must be annotated with @WebSocket
 * and must contain methods annotated with (see ExampleWebSocket):
 *   @OnWebSocketConnect 
 *   @OnWebSocketClose 
 *   @OnWebSocketMessage 
 *   @OnWebSocketError
 *   
 * This is consistent with the Jetty websocket API.
 * 
 * By default, web sockets are not singletons. That is, your factory will be invoked
 * to create a new instance for each incoming websocket connection. If you wish to use
 * a singleton, your factory may simply return the same instance at each invocation.
 * 
 * Note that websockets will not be reloaded when debug mode is enabled due to limitations 
 * in Jetty (you'll need to restart the server to see changes reflected). However, if you
 * are clever in the implementation of your factory, you can use custom classloading to
 * make sure that the socket's code is reloaded for each new incoming connection (probably
 * only in debug mode) or even for each new incoming message/event.
 * 
 * For an example:
 * @see lightning.examples.websockets.ExampleWebsocket
 */
public @interface WebSocketFactory {
  // Specifies the path on which the websocket will be installed.
  // This path must be exact; wildcards and parameters are disallowed.
  String path();
}
