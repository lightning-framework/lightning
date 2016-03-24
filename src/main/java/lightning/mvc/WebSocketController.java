package lightning.mvc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
/**
 * Use to indicate that a class implements a websocket
 * on the given path (the route will be installed for
 * you).
 * 
 * The class must also have the required Jetty websocket
 * annotations that spark requires.
 */
public @interface WebSocketController {
  String path();
}
