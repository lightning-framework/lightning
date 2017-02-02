package lightning.exceptions;

public class LightningMockThrowable extends Throwable {
  private static final long serialVersionUID = 1L;

  public LightningMockThrowable() {
    super("The code that caused the above exception:");
  }
}
