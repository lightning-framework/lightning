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
 * HTTP method(s).
 * 
 * The path may contain parameters or wild cards.
 * 
 * A parameter is indicated by prefixing a path segment with ":".
 * The remainder of the segment after the ":" specifies the name of the
 * parameter.
 * A parameter matches exactly one path segment.
 * 
 * A wildcard is indicated by having a path segment equal to "*".
 * A wildcard matches >=1 path segments and must be the last segment 
 * in the path.
 * 
 * Example Paths:
 *  /
 *  /my/path/
 *  /u/:username
 *  /static/*
 *  * (matches EVERYTHING)
 *  
 * Wildcards and parameters contained in the request will
 * be exposed on the Request in the handler.
 *  request().getWildcards()
 *  request().getWildcardPath()
 *  request().routeParam("name").stringValue()
 * 
 * Routes will be automatically installed based on the presence of 
 * these annotations, and automatically reloaded in debug mode.
 * 
 * Static files take precedence over routes. In this way, a route to
 * "/*" will not prevent static files from being shown.
 * 
 * Routes must be instance methods and declared public.
 * 
 * Routing conflicts are allowed in cases where the conflicts do not
 * occur in all cases. For example, these are allowed:
 *  /:a/a
 *  /b/a
 *  /* 
 *  
 * But, these are not:
 *  /:a/a
 *  /:b/a
 * 
 * Routes are resolved by crawling the routing radix tree searching for a 
 * match with priority given to exact matches, then parametric matches, 
 * then wildcard matches.
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
 *      An exception will be thrown if a use cannot be found for the returned object.
 *      
 * Route handlers may take an arbitrary number of inputs.
 * These inputs will be automatically filled in the the dependency injector.
 * The dependency injector will inject (a) user-provided dependencies (that you defined
 * when you invoked Lightning::launch), (b) global framework-defined dependencies (Config,
 * MySQLDatabaseProvider, etc.), and (c) request-specific framework-defined dependencies 
 * (Validator, HandlerContext, Session, URLGenerator, User, Request, Response, etc.).
 * 
 * To inject your own (custom) dependencies, you must provide an instance of InjectorModule
 * to lightning.launch. InjectorModule will allow you to inject dependencies by name, type
 * (class), or by the presence of an annotation. Custom injected dependences MUST NOT be in
 * the autoreload package prefixes specified in configuration. Examples:
 *   InjectorModule m = new InjectorModule();
 *   m.bindNameToInstance("mydep", new MyDep());
 *   m.bindClassToInstance(MyDep.class, new MyDep());
 *   m.bindAnnotationToInstance(MyDepAnn.class, new MyDep()); *   
 *   public void handle(@Inject("mydep") MyDep dep1, MyDep dep2, @MyDepAnn MyDep dep3)
 * See lightning.inject.Injector for more information.
 * 
 * A single method may be annotated with @Route multiple times. 
 */
public @interface Route {
  // The path to match (see above for details).
  String path();
  
  // The HTTP methods accepted.
  HTTPMethod[] methods() default {HTTPMethod.GET};
}