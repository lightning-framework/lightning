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
 * Indicates that a routing target returns JSON.
 * 
 * The value returned from the target will be 
 * automatically JSONified and appropriate
 * headers will be set.
 */
public @interface Json {
  String prefix() default "";
}
