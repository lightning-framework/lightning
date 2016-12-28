package lightning.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates the declaration of a class that implements WebSocketHandler.
 * 
 * The annotated class will be installed as a web socket end point on the path
 * specified in the annotation if the class is declared within the scanPrefixes
 * specified in your configuration.
 * 
 * A web socket handler's code may be reloaded automatically in debug mode if the 
 * class is defined within the autoReloadPrefixes specified in your configuration. 
 * Web socket code reloads will occur the next time any web socket event occurs on 
 * the socket if (and only if) the code has actually changed.
 * 
 * When a code reload is triggered on a web socket handler, all active connections
 * will be terminated before servicing the triggering event. The onClose method on
 * the web socket handler (running the old code) will still be invoked. After the 
 * reload, all new connections will utilize the updated handler code.
 * 
 * This code reloading process is implemented with the assumption that web sockets
 * are typically used for long-lived connections and that clients will automatically
 * attempt to reconnect when the connection goes down.
 *  * 
 * The constructor of a web socket is dependency-injectable.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebSocket {
  /**
   * Specifies the path on which to install the web socket.
   */
  public String path();
  
  /**
   * Specifies whether or not the web socket is a singleton.
   * 
   * If the socket is a singleton, only one instance will be
   * created. You should use this if you are writing a socket
   * that handles stateless messages.
   * 
   * If the socket is not a singleton, one instance will be
   * created for each new incoming connection. In this mode,
   * you may safely save information about the peer on the
   * socket.
   */
  public boolean isSingleton() default true;
}
