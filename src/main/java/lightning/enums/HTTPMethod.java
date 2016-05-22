package lightning.enums;

public enum HTTPMethod {
  GET,
  POST,
  HEAD,
  DELETE,
  PUT,
  OPTIONS,
  TRACE;
  
  public static final HTTPMethod[] ALL = new HTTPMethod[]{GET, POST, HEAD, DELETE, PUT, OPTIONS, TRACE};
}
