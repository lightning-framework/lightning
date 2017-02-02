package lightning.exceptions;

import java.lang.reflect.Method;

import lightning.util.DebugUtil;

public class LightningValidationException extends LightningException {
  private static final long serialVersionUID = 1L;
  private final Class<?> clazz;
  private final Method method;

  public LightningValidationException(Class<?> clazz, String message) {
    super(message + " (" + clazz.getCanonicalName() + ")");
    this.clazz = clazz;
    this.method = null;
    DebugUtil.mockStackTrace(clazz, this);
  }

  public LightningValidationException(Method method, String message) {
    super(message + " (" + method + ")");
    this.clazz = method.getDeclaringClass();
    this.method = method;
    DebugUtil.mockStackTrace(method, this, true);
  }

  public LightningValidationException(Class<?> clazz, String message, Throwable e) {
    super(message + " (" + clazz.getCanonicalName() + ")", e);
    this.clazz = clazz;
    this.method = null;
    DebugUtil.mockStackTrace(clazz, this);
  }

  public LightningValidationException(Method method, String message, Throwable e) {
    super(message + " (" + method + ")", e);
    this.clazz = method.getDeclaringClass();
    this.method = method;
    DebugUtil.mockStackTrace(method, this, true);
  }

  public LightningValidationException(String message) {
    super(message);
    this.clazz = null;
    this.method = null;
  }

  public LightningValidationException(String message, Throwable cause) {
    super(message, cause);
    this.clazz = null;
    this.method = null;
  }

  public Class<?> getOffendingClass() {
    return clazz;
  }

  public Method getOffendingMethod() {
    return method;
  }
}
