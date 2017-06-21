package lightning.flags.parsers;

public class ParseException extends Exception {
  private static final long serialVersionUID = 1L;

  public ParseException(Exception cause) {
    super(cause);
  }

  public ParseException(String message) {
    super(message);
  }
}
