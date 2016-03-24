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
 * Use to indicate that a given routing target
 * requires an authenticated user. Requests
 * with no authenticated user will be filtered.
 * 
 * May optionally specify a redirect URL. If no
 * URL is specified, the user will be shown a
 * 401 Unauthorized page.
 */
public @interface RequireAuth {
  String redirectTo() default "";
}
