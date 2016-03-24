package lightning.http;

public final class NotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public NotFoundException() {
    super();
  }
  
  public NotFoundException(String e) {
    super(e);
  }
  
  public NotFoundException(Exception e) {
    super(e);
  }
}