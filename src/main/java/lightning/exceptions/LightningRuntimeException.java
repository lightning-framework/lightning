package lightning.exceptions;

public class LightningRuntimeException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public LightningRuntimeException(Exception e) {
    super(e);
  }
  
  public LightningRuntimeException(String m) {
    super(m);
  }
}
