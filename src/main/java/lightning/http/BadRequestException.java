package lightning.http;

public final class BadRequestException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public BadRequestException() {
    super();
  }
  
  public BadRequestException(String e) {
    super(e);
  }
  
  public BadRequestException(Exception e) {
    super(e);
  }
}