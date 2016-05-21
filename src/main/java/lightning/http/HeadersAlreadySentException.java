package lightning.http;

/**
 * An exception thrown when users attempt to write HTTP header content after
 * part of the response has been sent.
 */
public class HeadersAlreadySentException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public HeadersAlreadySentException() {
    super();
  }
}
