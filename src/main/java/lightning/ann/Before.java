package lightning.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import lightning.enums.FilterPriority;
import lightning.enums.HTTPMethod;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Befores.class)
@Deprecated // TODO: NOT YET IMPLEMENTED
public @interface Before {
  public String value(); // Request path (follows same syntax as @Route).
  HTTPMethod[] methods() default {HTTPMethod.GET};
  FilterPriority priority() default FilterPriority.NORMAL;
}
