package lightning.http;

/**
 * An exception which corresponds to an HTTP 404 error.
 */
public final class NotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public NotFoundException() {
    super();
  }
  
  public NotFoundException(String e) {
    super(e);
  }
  
  public NotFoundException(Exception e) {
    super(e);
  }
}