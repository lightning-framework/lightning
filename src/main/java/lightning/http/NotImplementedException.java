package lightning.http;

public final class NotImplementedException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public NotImplementedException() {
    super();
  }
  
  public NotImplementedException(String e) {
    super(e);
  }
  
  public NotImplementedException(Exception e) {
    super(e);
  }
}