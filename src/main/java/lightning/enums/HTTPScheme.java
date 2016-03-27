package lightning.enums;

public enum HTTPScheme {
  HTTP,
  HTTPS,
  UNKNOWN;
  
  public boolean isSecure() {
    switch(this) {
      case HTTPS: return true;
      default: return false;
    }
  }
}
