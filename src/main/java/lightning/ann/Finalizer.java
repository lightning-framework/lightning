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
 * annotated with @Finalizer invoked after the incoming request is serviced.
 * 
 * Thus, finalizers are executed AFTER all @Initializer methods and AFTER the @Route
 * that instantiated the controller is finished.
 * 
 * Finalizers will ALWAYS be executed, regardless of whether or not an exception was
 * thrown by an initializer or route target during the servicing of the request.
 * 
 * Finalizers MAY throw exceptions. These exceptions will be logged, but will not be
 * visible to the user either as a debug page or a 500 internal server error as
 * presumably the response body and headers would already be sent before finalizers
 * execute. All finalizers will always execute, regardless of whether or not any
 * of them throw exceptions.
 * 
 * Finalizers MUST return void.
 * Finalizers are not executed in any particular order.
 * Finalizers are dependency-injectable.
 */
public @interface Finalizer {}