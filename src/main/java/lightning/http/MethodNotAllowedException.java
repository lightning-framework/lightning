package lightning.http;

public final class MethodNotAllowedException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public MethodNotAllowedException() {
    super();
  }
  
  public MethodNotAllowedException(String e) {
    super(e);
  }
  
  public MethodNotAllowedException(Exception e) {
    super(e);
  }
}