package lightning.enums;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.PreEncodedHttpField;

public enum CacheControl {
  PUBLIC,
  PRIVATE,
  NO_CACHE;

  public String toHttpString() {
    switch (this) {
      case PRIVATE:
        return "private";
      case PUBLIC:
        return "public";
      case NO_CACHE:
      default:
        return "no-cache, no-store";
    }
  }
  
  public HttpField toHttpField() {
    switch (this) {
      case PRIVATE:
        return new PreEncodedHttpField(HttpHeader.CACHE_CONTROL, "private, max-age=3600");
      case PUBLIC:
        return  new PreEncodedHttpField(HttpHeader.CACHE_CONTROL, "public, max-age=3600");
      case NO_CACHE:
      default:
        return new PreEncodedHttpField(HttpHeader.CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate");
    }
  }
}
