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
 * Exception handlers respect the class hierarchy.
 */
public @interface ExceptionHandler {
  Class<? extends Throwable> value();
}
