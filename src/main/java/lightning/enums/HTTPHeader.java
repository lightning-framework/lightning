package lightning.enums;

public enum HTTPHeader {
  ACCEPT("Accept"),
  LOCATION("Location"),
  HOST("Host"),
  X_FORWARDED_FOR("X-Forwarded-For"),
  X_FORWARDED_PROTO("X-Forwarded-Proto"),
  USER_AGENT("User-Agent"),
  CONTENT_TYPE("Content-Type"),
  CONTENT_LENGTH("Content-Length"),
  CONTENT_DISPOSITION("Content-Disposition"),
  DATE("Date"),
  ACCEPT_ENCODING("Accept-Encoding"),
  CACHE_CONTROL("Cache-Control"),
  CONNECTION("Connection"),
  IF_NONE_MATCH("If-None-Match"),
  IF_MODIFIED_SINCE("If-Modified-Since"),
  DO_NOT_TRACK("DNT"),
  CONTENT_ENCODING("Content-Encoding"),
  ETAG("Etag"),
  EXPIRES("Expires"),
  SERVER("Server"),
  UPGRADE_INSECURE_REQUESTS("Upgrade-Insecure-Requests"),
  LAST_MODIFIED("Last-Modified"),
  X_XSS_PROTECTION("X-XSS-Protection"),
  CONTENT_SECURITY_POLICY("Content-Security-Policy"),
  STRICT_TRANSPORT_SECURITY("Strict-Transport-Security"),
  PUBLIC_KEY_PINS("Public-Key-Pins"),
  X_FRAME_OPTIONS("X-Frame-Options"),
  X_CONTENT_TYPE_OPTIONS("X-Content-Type-Options"),
  REFERRER_POLICY("Referrer-Policy");

  private final String headerName;

  private HTTPHeader(String headerName) {
    this.headerName = headerName;
  }

  public String getHeaderName() {
    return headerName;
  }

  public String httpName() {
    return headerName;
  }
}
