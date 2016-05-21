package lightning.http;

/**
 * An exception which corresponds to an HTTP 401 error.
 */
public final class NotAuthorizedException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public NotAuthorizedException() {
    super();
  }
  
  public NotAuthorizedException(String e) {
    super(e);
  }
  
  public NotAuthorizedException(Exception e) {
    super(e);
  }
}