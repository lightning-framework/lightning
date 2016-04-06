package lightning.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
/**
 * All classes which serve as a controller in the
 * application must be annotated with @Controller.
 * 
 * A new instance of each controller is allocated to
 * handle each incoming request.
 * 
 * Controllers must have a single public constructor.
 * Controller constructors are injectable (see @Route).
 */
public @interface Controller {}
