package lightning.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import lightning.enums.CacheType;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
/**
 * NOT YET IMPLEMENTED!
 */
public @interface Cacheable {
  CacheType type() default CacheType.PUBLIC;
  long expireMs() default 60000;
}
