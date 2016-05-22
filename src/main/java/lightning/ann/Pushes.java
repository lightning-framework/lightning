package lightning.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
/**
 * May be used to annotate a @Route target.
 * 
 * Indicates that the given resources (paths) should be pushed to the client via HTTP/2 push
 * with the response to incoming requests.
 * 
 * Has no effect if HTTP/2 is not enabled.
 * 
 * Example:
 *   @Push({"/styles/style.css"})
 * 
 * TODO: Resources may also be pushed by invoking push(...) on lightning.server.Context or lightning.mvc.HandlerContext.
 * TODO: Not currently implemented.
 */
@Deprecated
public @interface Pushes {
  public String[] value();
}
