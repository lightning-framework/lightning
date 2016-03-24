package lightning.plugins.cas;

public class CASException extends Exception {
  private static final long serialVersionUID = 1L;

  public CASException(String m) {
    super(m);
  }
  
  public CASException(Exception e) {
    super(e);
  }
}
