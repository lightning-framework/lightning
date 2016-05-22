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
 * When annotating a parameter on a method that is dependency injectable,
 * this annotation specifies that the annotated parameter should be injected
 * with the parameter bound to value via a call to InjectorModule::bindNameToInstance
 * where the name matches the value provided in the annotation.
 */
public @interface Inject {
  String value();
}
