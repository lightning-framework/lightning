package lightning.mvc;

public class TypeConversionException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public TypeConversionException() {
    super();
  }
  
  public TypeConversionException(String message) {
    super(message);
  }
  
  public TypeConversionException(Exception cause) {
    super(cause);
  }
}
