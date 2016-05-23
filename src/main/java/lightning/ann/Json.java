package lightning.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import lightning.enums.JsonFieldNamingPolicy;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
/**
 * Indicates that a @Route target returns JSON.
 * 
 * The value returned from the target will be 
 * automatically JSONified (via GSON) and 
 * appropriate headers will be set.
 */
public @interface Json {
  // An optional prefix that will be prepended to the JSON.
  // Use to set an XSSI prefix e.g. ')]}\n
  String prefix() default "";
  
  // An optional naming policy to use for GSON:
  JsonFieldNamingPolicy names() default JsonFieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
}
