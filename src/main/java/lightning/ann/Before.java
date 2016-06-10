package lightning.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import lightning.enums.FilterPriority;
import lightning.enums.HTTPMethod;

/**
 * A @Before filter annotates a static method that will be invoked before a request on the given path
 * for the given HTTP methods. 
 * 
 * Before filters may defined within any class in the scan prefixes defined in your config.
 * 
 * Before (path-based) filters execute before @Filter (annotation-based) filters.
 * 
 * A method may be annotated by multiple @Before annotations.
 * 
 * You may control the order in which filters execute by changing the priority in the annotation. Filters
 * with higher priority will execute first. Filters with the same priority may execute in any order.
 * 
 * Filter methods are injectable with both global and request-specific objects.
 * 
 * Filters may invoke halt() to prevent further processing of the request.
 * 
 * If a filter halts or throws an exception, it will prevent execution of filters that have not yet executed.
 * 
 * Filters will only execute if the given path/method also matches an @Route.
 * 
 * Route params and wildcards are available within filter methods (set according to the path specified in
 * the filter).
 * 
 * Filter matching is fast (exact asymptotic behavior depends on how your filters are structured), but matching
 * is mostly proportional to the length of the request path * the number of matched filters.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Befores.class)
public @interface Before {
  String path(); // Request path (follows same syntax as @Route).
  HTTPMethod[] methods() default {HTTPMethod.GET};
  FilterPriority priority() default FilterPriority.NORMAL;
}
