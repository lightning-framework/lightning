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
  
  @Override
  public String toString() {
    switch (this) {
      case HTTP: return "http";
      case HTTPS: return "https";
      default: return "http";
    }
  }
}
