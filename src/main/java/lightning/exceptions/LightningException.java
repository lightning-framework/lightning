package lightning.exceptions;

public class LightningException extends Exception {
  private static final long serialVersionUID = 1L;

  public LightningException() {
    super();
  }

  public LightningException(Throwable cause) {
    super(cause);
  }

  public LightningException(String message) {
    super(message);
  }

  public LightningException(String message, Throwable cause) {
    super(message, cause);
  }
}
