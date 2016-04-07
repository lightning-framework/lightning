package lightning.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import lightning.enums.CacheControl;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Deprecated // TODO: NOT YET IMPLEMENTED
public @interface Cacheable {
  CacheControl type() default CacheControl.PUBLIC;
  long expireMs() default 60000;
}
