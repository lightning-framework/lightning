package lightning.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import lightning.fn.RouteFilter;

/**
 * Used to annotate a route target. Allows the programmer to specify code snippet(s) that will run
 * before the route target is invoked.
 * 
 * If multiple filters are installed, the order in which they execute is undefined.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Filters.class)
public @interface Filter {
  /**
   * @return The class of the filter. A new instance is allocated for each incoming request matching the
   *         filter. The filter's constructor is injectable.
   */
  Class<? extends RouteFilter> value();
}