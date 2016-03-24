package lightning.mvc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
/**
 * Requires the presence of the XSRF
 * token as the value for the query
 * parameter with given name.
 * 
 * A BadRequestException is thrown if
 * the constraint is not met.
 */
public @interface RequireXsrfToken {
  String inputName() default "_xsrf";
}
