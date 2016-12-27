package lightning.exceptions;

public class LightningException extends Exception {
  private static final long serialVersionUID = 1L;

  public LightningException(Exception e) {
    super(e);
  }
  
  public LightningException(String m) {
    super(m);
  }
  
  public LightningException() {
    super();
  }
}
