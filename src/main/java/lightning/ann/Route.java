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
 * The path may contain parameters or wild cards.
 * 
 * Example Paths:
 *  /
 *  /my/path/
 *  /u/:username
 *  /static/*
 *  
 * Wildcards and parameters contained in the request will
 * be exposed on the Request in the handler.
 *  request.getWildcards()
 *  request.getWildcardPath()
 *  request.routeParam("name")
 * 
 * Routes will be automatically installed based on the 
 * presence of these annotations.
 * 
 * Static files take precedence over routes.
 * 
 * Routes must be instance methods and declared public.
 * 
 * Routing conflicts are allowed. Routes are resolved by crawling
 * the routing radix tree searching for a match with priority given
 * to exact matches, then parametric matches, then wildcard matches.
 * 
 * As a simple example, consider the following routes:
 *   /
 *   /something
 *   /*
 *   /:something
 *   /u/:something
 *   /u/*
 *   /z/:something
 *  
 * For the above routes, the given URLs (LHS) will be matched to (RHS):
 *   / => /
 *   /something => /something
 *   /anything => /:something
 *   /anything/more => /*
 *   /u => /:something
 *   /u/h => /u/:something
 *   /u/h/z => /u/*
 *   /z/h/u => /*
 *   
 * Route matching is tree-based and incredibly fast - O(n) w.r.t. the number
 * of segments in the path regardless of the number of routes installed.
 *   
 * Route handlers may return:
 *   void
 *   null (equivalent to returning void)
 *   String: to output it
 *   ModelAndView: to render it
 *   File: to pass it through
 *   Any other Object:
 *      To be converted to JSON and written out if @Json is present
 *      To be used as a view model to render a template if @Template is present
 */
public @interface Route {
  // The path to match (see above for details).
  String path();
  
  // The HTTP methods accepted.
  HTTPMethod[] methods() default {HTTPMethod.GET};
}