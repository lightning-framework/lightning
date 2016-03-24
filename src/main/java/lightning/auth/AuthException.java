package lightning.auth;

/**
 * A type of exception thrown in response to some authentication failure.
 */
public class AuthException extends Exception {
  public enum Type {
    NOT_SUPPORTED,
    NOT_IMPLEMENTED,
    MISCONFIGURED,
    INVALID_USERNAME,
    INVALID_PASSWORD,
    USER_THROTTLED,
    IP_THROTTLED,
    USER_BANNED,
    NO_USER,
    DRIVER_ERROR
  }
  
  private static final long serialVersionUID = 1L;
  private final Type type;

  public AuthException (Type type, String message) {
    super(message);
    this.type = type;
  }
  
  public AuthException(Type type, Exception exception) {
    super(exception);
    this.type = type;
  }
  
  public Type getType() {
    return this.type;
  }
}
