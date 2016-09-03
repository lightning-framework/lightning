package lightning.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Deprecated // TODO: NOT YET IMPLEMENTED
/**
 * Provides the framework with a hint that you would not like the
 * output of a handler to be compressed.
 */
public @interface NoCompress {}
