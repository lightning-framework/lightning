package lightning.flags;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Flag {
  // Names
  public String[] names();

  // Description of what the flag does.
  public String description();

  // Name of an environment variable.
  // Will try to use this to set the flag value (if present and set).
  // Command line parameters will override the environment variable.
  public String environment() default "";

  // Whether to hide from --help.
  public boolean hidden() default false;

  // Whether or not a value is required.
  public boolean required() default false;
}