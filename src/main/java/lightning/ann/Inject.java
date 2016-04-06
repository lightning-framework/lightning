package lightning.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
/**
 * When annotating a parameter on a method that is a route
 * target, exception handler, initializer, or websocket factory,
 * specifies that the annotated parameter should be injected
 * with the parameter bound to value via a call to InjectorModule
 * bindNameToInstance.
 */
public @interface Inject {
  String value();
}
