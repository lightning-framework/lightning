package lightning.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
/**
 * Controller classes (annotated with @Controller) will have any methods
 * annotated with @Initializer invoked before receiving any traffic.
 * 
 * Initializer methods may throw exceptions. If an initializer throws an 
 * exception, further processing of the request is halted (any unexecuted
 * initializers and the route handler may not execute, though finalizers
 * will still execute).
 * 
 * Initializer methods are not executed in any particular order.
 * Initializer methods must return void.
 * Initializer methods are inherited from parent classes.
 * Initializer methods are dependency-injectable (see @Route).
 */
public @interface Initializer {}
