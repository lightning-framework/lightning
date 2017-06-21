package lightning.flags;

public class FlagException extends Exception {
  private static final long serialVersionUID = 1L;

  public FlagException(String message) {
    super(message);
  }

  public FlagException(Exception cause) {
    super(cause);
  }

  public FlagException(String message, Exception cause) {
    super(message);
    this.addSuppressed(cause);
  }
}
