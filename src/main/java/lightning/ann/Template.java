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
 * Used to indicate that a route handler returns a value
 * that should be placed in a template.
 * 
 * If value is ommitted, the handler should return a ModelAndView.
 * 
 * If value is provided, the value should be the name of the template
 * and the route handler should return ONLY the view model.
 */
public @interface Template {
  String value() default "";
}
