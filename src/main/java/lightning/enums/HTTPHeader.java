package lightning.enums;

public enum HTTPHeader {
  ACCEPT("Accept"),
  LOCATION("Location"),
  HOST("host");
  
  private final String headerName;
  
  private HTTPHeader(String headerName) {
    this.headerName = headerName;
  }
  
  public String getHeaderName() {
    return headerName;
  }
}
