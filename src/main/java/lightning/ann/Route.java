package lightning.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import lightning.enums.HTTPMethod;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Routes.class)
/**
 * Use to indicate that a given method on a @Controller
 * should handle traffic on the given path for the given
 * methods.
 * 
 * The path may contain parameters or wildcards as is
 * consistent with the format accepted by Spark.get().
 * 
 * Routes will be automatically installed for you.
 */
public @interface Route {
  String path();
  HTTPMethod[] methods() default {HTTPMethod.GET};
}