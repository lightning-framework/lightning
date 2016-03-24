package lightning.plugins.cas;

public class CASRuntimeException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public CASRuntimeException(String m) {
    super(m);
  }
  
  public CASRuntimeException(Exception e) {
    super(e);
  }
}
