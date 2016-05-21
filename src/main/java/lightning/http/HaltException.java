package lightning.http;

/**
 * An exception that, when thrown from a filter or route handler, immediately halts any further
 * processing of the request and commits the response.
 */
public class HaltException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public HaltException() {
    super();
  }
}
