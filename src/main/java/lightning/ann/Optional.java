package lightning.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
/**
 * Indicates that a field is optional in an object that is converted to/from JSON.
 * Optional values should either have a default value (defined in-line) or be nullable.
 * TODO: Add enforcement to the default JSON parser.
 */
public @interface Optional {}
