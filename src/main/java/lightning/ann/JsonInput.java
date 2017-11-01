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
public @interface JsonInput {
  Class<? extends Object> type();
  JsonFieldNamingPolicy names() default JsonFieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
}
