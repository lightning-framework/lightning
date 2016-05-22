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
 * Used to indicate that a route handler returns a value that should be used in
 * rendering a template.
 * 
 * If value is omitted in the annotation, the handler should return a ModelAndView.
 * Utilize this functionality for cases where the template name must be determined
 * dynamically.
 * 
 * If value is provided in the annotation, the value should be the name of the template
 * and the route handler should return ONLY the view model for that template. Use this
 * functionality for cases where the template name is known ahead of time.
 */
public @interface Template {
  String value() default "";
}
