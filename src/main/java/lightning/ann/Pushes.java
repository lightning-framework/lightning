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
 * May be used to annotate a route target.
 * 
 * Indicates that the given resources should be pushed to the client via HTTP/2 push
 * with the response to incoming requests.
 * 
 * Has no effect if HTTP/2 is not enabled.
 * 
 * Example:
 *   @Push({"/styles/style.css"})
 * 
 * TODO: Implement
 */
@Deprecated
public @interface Pushes {
  public String[] value();
}
