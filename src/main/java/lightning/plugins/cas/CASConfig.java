package lightning.plugins.cas;

public final class CASConfig {
  public final String host;
  public final String path;
  
  private CASConfig(String host, String path) {
    this.host = host;
    this.path = path;
  }
  
  public static CASConfig.Builder newBuilder() {
    return new Builder();
  }
  
  public static final class Builder {
    private String host;
    private String path = "/cas";
    
    private Builder() {}
    
    public Builder setHost(String host) {
      this.host = host;
      return this;
    }
    
    public Builder setPath(String path) {
      this.path = path;
      return this;
    }
    
    public CASConfig build() {
      if (host == null) {
        throw new CASRuntimeException("Cannot build invalid CAS configuration.");
      }
      
      return new CASConfig(host, path);
    }
  }
}
