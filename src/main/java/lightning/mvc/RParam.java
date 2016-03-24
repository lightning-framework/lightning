package lightning.mvc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
/**
 * Annotate an argument to a routing target with this
 * annotation to inject the value of the routing param
 * with the given name.
 * 
 * The framework will automatically attempt to convert
 * the parameter value to the type of the argument. If
 * conversion is not possible, a BadRequestException 
 * will be triggered.
 * 
 * Note: Do not prefix the name with a colon.
 */
public @interface RParam {
  String value();
}
