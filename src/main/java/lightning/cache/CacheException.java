package lightning.cache;

public class CacheException extends Exception {
  private static final long serialVersionUID = 1L;

  public CacheException() {
    super();
  }
  
  public CacheException(String message) {
    super(message);
  }
  
  public CacheException(Exception parent) {
    super(parent);
  }
}
