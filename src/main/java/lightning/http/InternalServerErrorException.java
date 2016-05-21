package lightning.http;

/**
 * An exception which corresponds to an HTTP 500 error.
 */
public final class InternalServerErrorException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public InternalServerErrorException() {
    super();
  }
  
  public InternalServerErrorException(String e) {
    super(e);
  }
  
  public InternalServerErrorException(Exception e) {
    super(e);
  }
}