package lightning.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
/**
 * Use to indicate that a given routing target
 * accepts HTTP multipart input (typically for
 * file uploads).
 * 
 * Will initialize multipart handling on the
 * request automatically.
 */
public @interface Multipart {}