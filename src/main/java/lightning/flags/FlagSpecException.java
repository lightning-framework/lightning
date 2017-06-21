package lightning.flags;

public class FlagSpecException extends FlagException {
  private static final long serialVersionUID = 1L;

  public FlagSpecException(String message) {
    super(message);
  }

  public FlagSpecException(String message, Exception cause) {
    super(message);
    this.addSuppressed(cause);
  }
}
