package lightning.http;

/**
 * An exception which corresponds to an HTTP 403 error.
 */
public final class AccessViolationException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public AccessViolationException() {
    super();
  }
  
  public AccessViolationException(String e) {
    super(e);
  }
  
  public AccessViolationException(Exception e) {
    super(e);
  }
}