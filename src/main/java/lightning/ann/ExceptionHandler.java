package lightning.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ExceptionHandlers.class)
/**
 * Indicates that a given method handles Exceptions of the given type.
 * User-defined exception handlers will take precedence over framework defined handlers.
 * 
 * When matching exception handlers, the framework respects the class hierarchy.
 * Thus, to have a catch-all handler, you could declare a handler for Throwable.class. 
 * 
 * To have custom HTTP error pages, add exception handlers for the exceptions in lightning.http.
 *  
 * A correct function signature for an @ExceptionHandler might look like:
 *  public static void handleMyException(HandlerContext ctx, MyException e) throws Exception;
 *  
 * Exception handler parameters are injectable (see @Route documentation).
 * In addition, the causing exception can be injected as a parameter.
 */
public @interface ExceptionHandler {
  Class<? extends Throwable> value();
}
