package lightning.exceptions;

public class LightningRuntimeException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public LightningRuntimeException() {
    super();
  }

  public LightningRuntimeException(Throwable e) {
    super(e);
  }

  public LightningRuntimeException(String m) {
    super(m);
  }

  public LightningRuntimeException(String m, Throwable e) {
    super(m, e);
  }
}
