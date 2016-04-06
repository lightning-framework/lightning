package lightning.inject;

public class InjectionException extends Exception {
  private static final long serialVersionUID = 1L;

  public InjectionException(String message) {
    super(message);
  }
  
  public InjectionException(Exception parent) {
    super(parent);
  }
}
