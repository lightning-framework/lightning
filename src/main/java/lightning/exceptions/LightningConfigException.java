package lightning.exceptions;

public class LightningConfigException extends LightningException {
  private static final long serialVersionUID = 1L;

  public LightningConfigException(Throwable cause) {
    super(cause);
  }

  public LightningConfigException(String message) {
    super(message);
  }

  public LightningConfigException(String message, Throwable cause) {
    super(message, cause);
  }
}
