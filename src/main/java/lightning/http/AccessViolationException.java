package lightning.http;

public final class AccessViolationException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public AccessViolationException() {
    super();
  }
  
  public AccessViolationException(String e) {
    super(e);
  }
  
  public AccessViolationException(Exception e) {
    super(e);
  }
}