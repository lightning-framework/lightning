package lightning.config;

import javax.annotation.Nullable;

import lightning.mail.Mail;

import com.google.common.collect.ImmutableList;

public final class Config {  
  /**
   * Whether or not to enable debug mode.
   * In debug mode, you will be shown stack traces in browser and assets/templates
   * will automatically reload for each incoming request (no caching).
   */
  public final boolean enableDebugMode;
  
  /**
   * A list of class path prefixes. When debug mode is enabled, classes in these
   * paths will be automatically reloaded for each incoming request (essentially
   * giving the instant reloading you get when developing in Python or PHP). Be
   * careful! Not all classes can be safely reloaded - this is intended for users
   * who are familiar with the class loading system. Classes that are not stateless
   * (e.g. have static static) should NOT be reloaded.
   * Example: ImmutableList.of("com.myname.controllers")
   */
  public final ImmutableList<String> autoReloadPrefixes;
  
  public final ImmutableList<String> scanPrefixes;
  
  public final SSLConfig ssl;
  public final ServerConfig server;
  public final MailConfig mail;
  public final DBConfig db;
  
  private Config(boolean enableDebugMode, ImmutableList<String> autoReloadPrefixes, ImmutableList<String> scanPrefixes, SSLConfig ssl, 
      ServerConfig server, MailConfig mail, DBConfig db) {
    this.enableDebugMode = enableDebugMode;
    this.autoReloadPrefixes = autoReloadPrefixes;
    this.scanPrefixes = scanPrefixes;
    this.ssl = ssl;
    this.server = server;
    this.mail = mail;
    this.db = db;
  }
  
  /**
   * Controllers whether or not the server should serve over SSL (HTTPS) and what
   * certificate should be used when serving. This makes use of Java's key store
   * system, not your traditional SSL certificates. You'll need to encapsulate 
   * your SSL certificate in a key store in order to serve over SSL - you can find
   * instructions online.
   */
  public static final class SSLConfig {
    public final @Nullable String keyStoreFile;       // Required to enable SSL.
    public final @Nullable String keyStorePassword;   // Required to enable SSL.
    public final @Nullable String trustStoreFile;     // Optional.
    public final @Nullable String trustStorePassword; // Optional.
    
    private SSLConfig(String keyStoreFile, String keyStorePassword, String trustStoreFile, String trustStorePassword) {
      this.keyStoreFile = keyStoreFile;
      this.keyStorePassword = keyStorePassword;
      this.trustStoreFile = trustStoreFile;
      this.trustStorePassword = trustStorePassword;
    }
    
    public boolean isEnabled() {
      return keyStorePassword != null && keyStoreFile != null;
    }
    
    public static final class Builder {
      private final Config.Builder parent;
      private String keyStoreFile = null;
      private String keyStorePassword = null;
      private String trustStoreFile = null;
      private String trustStorePassword = null;
      
      private Builder(Config.Builder parent) {
        this.parent = parent;
      }
      
      public Config.Builder setKeyStoreFile(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
        return parent;
      }
      
      public Config.Builder setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
        return parent;
      }
      
      public Config.Builder setTrustStoreFile(String trustStoreFile) {
        this.trustStoreFile = trustStoreFile;
        return parent;
      }
      
      public Config.Builder setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
        return parent;
      }
      
      public SSLConfig build() {
        return new SSLConfig(keyStoreFile, keyStorePassword, trustStoreFile, trustStorePassword);
      }
    }
  }
  
  /**
   * Configures additional options related to the webserver.
   */
  public static final class ServerConfig {
    public final String hmacKey; // Should be long, random, and unique.
    public final int port;
    public final int threads;
    public final int timeoutMs; // For websockets.
    public final String staticFilesPath; // Relative to src/main/java in your eclipse project folder.
    public final String templateFilesPath; // Relative to src/main/java in your eclipse project folder.
    public final boolean trustLoadBalancerHeaders;
    
    private ServerConfig(String hmacKey, int port, int threads, int timeoutMs, String staticFilesPath,
        String templateFilesPath, boolean trustLoadBalancerHeaders) {
      this.hmacKey = hmacKey;
      this.port = port;
      this.threads = threads;
      this.timeoutMs = timeoutMs;
      this.staticFilesPath = staticFilesPath;
      this.templateFilesPath = templateFilesPath;
      this.trustLoadBalancerHeaders = trustLoadBalancerHeaders;
    }
    
    public static final class Builder {
      private final Config.Builder parent;
      private String hmacKey;
      private int port = 8080;
      private int threads = 40;
      private int timeoutMs = 600;
      private String staticFilesPath;
      private String templateFilesPath;
      private boolean trustLoadBalancerHeaders;
      
      private Builder(Config.Builder parent) {
        this.parent = parent;
      }
      
      public Config.Builder setHmacKey(String hmacKey) {
        this.hmacKey = hmacKey;
        return parent;
      }
      
      public Config.Builder setPort(int port) {
        this.port = port;
        return parent;
      }
      
      public Config.Builder setThreads(int threads) {
        this.threads = threads;
        return parent;
      }
      
      public Config.Builder setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
        return parent;
      }
      
      public Config.Builder setStaticFilesPath(String staticFilesPath) {
        this.staticFilesPath = staticFilesPath;
        return parent;
      }
      
      public Config.Builder setTemplateFilesPath(String templateFilesPath) {
        this.templateFilesPath = templateFilesPath;
        return parent;
      }
      
      public Config.Builder setTrustLoadBalancerHeaders(boolean trustLoadBalancerHeaders) {
        this.trustLoadBalancerHeaders = trustLoadBalancerHeaders;
        return parent;
      }
      
      public ServerConfig build() {
        return new ServerConfig(hmacKey, port, threads, timeoutMs, staticFilesPath, templateFilesPath, 
            trustLoadBalancerHeaders);
      }
    }
  }
  
  /**
   * Configures email sending via an SMTP server.
   */
  public static final class MailConfig implements Mail.MailConfig {
    public final boolean useSSL;
    public final String address;
    public final String username;
    public final String password;
    public final String host;
    public final int port;
    
    private MailConfig(boolean useSSL, String address, String username, String password, String host, int port) {
      this.useSSL = useSSL;
      this.address = address;
      this.username = username;
      this.password = password;
      this.host = host;
      this.port = port;
    }
    
    public boolean isEnabled() {
      return host != null && username != null;
    }
    
    @Override
    public String getAddress() {
      return address;
    }
    
    @Override
    public int getPort() {
      return port;
    }
    
    @Override
    public boolean useSSL() {
      return useSSL;
    }
    
    @Override
    public String getHost() {
      return host;
    }
    
    @Override
    public String getUsername() {
      return username;
    }
    
    @Override
    public String getPassword() {
      return password;
    }
    
    public static final class Builder {
      private final Config.Builder parent;
      private boolean useSSL = true;
      private String address;
      private String username;
      private String password;
      private String host;
      private int port = 465;
      
      private Builder(Config.Builder parent) {
        this.parent = parent;
      }
      
      public Config.Builder setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
        return parent;
      }
      
      public Config.Builder setAddress(String address) {
        this.address = address;
        return parent;
      }
      
      public Config.Builder setUsername(String username) {
        this.username = username;
        return parent;
      }
      
      public Config.Builder setPassword(String password) {
        this.password = password;
        return parent;
      }
      
      public Config.Builder setHost(String host) {
        this.host = host;
        return parent;
      }
      
      public Config.Builder setPort(int port) {
        this.port = port;
        return parent;
      }
      
      public MailConfig build() {
        return new MailConfig(useSSL, address, username, password, host, port);
      }
    }
  }
  
  /**
   * Configures connection to a MySQL database server.
   */
  public static final class DBConfig {
    public final String host;
    public final int port;
    public final String username;
    public final String password;
    public final String name;
    
    private DBConfig(String host, int port, String username, String password, String name) {
      this.host = host;
      this.port = port;
      this.username = username;
      this.password = password;
      this.name = name;
    }
    
    public boolean isEnabled() {
      return host != null && name != null;
    }
    
    public static final class Builder {
      private final Config.Builder parent;
      private String host = null;
      private int port = 3306;
      private String username = "root";
      private String password = null;
      private String name = null;
      
      private Builder(Config.Builder parent) {
        this.parent = parent;
      }
      
      public Config.Builder setHost(String host) {
        this.host = host;
        return parent;
      }
      
      public Config.Builder setPort(int port) {
        this.port = port;
        return parent;
      }
      
      public Config.Builder setUsername(String username) {
        this.username = username;
        return parent;
      }
      
      public Config.Builder setPassword(String password) {
        this.password = password;
        return parent;
      }
      
      public Config.Builder setName(String name) {
        this.name = name;
        return parent;
      }
      
      public DBConfig build() {
        return new DBConfig(host, port, username, password, name);
      }
    }
  }
  
  public static final class Builder {
    private boolean enableDebugMode = false;
    private ImmutableList<String> autoReloadPrefixes = ImmutableList.of();
    private ImmutableList<String> scanPrefixes = ImmutableList.of();
    public final SSLConfig.Builder ssl = new SSLConfig.Builder(this);
    public final ServerConfig.Builder server = new ServerConfig.Builder(this);
    public final MailConfig.Builder mail = new MailConfig.Builder(this);
    public final DBConfig.Builder db = new DBConfig.Builder(this);
    
    private Builder() {}
    
    public Builder setEnableDebugMode(boolean enableDebugMode) {
      this.enableDebugMode = enableDebugMode;
      return this;
    }
    
    public Builder setAutoReloadPrefixes(ImmutableList<String> autoReloadPrefixes) {
      this.autoReloadPrefixes = autoReloadPrefixes;
      return this;
    }
    
    public Builder setScanPrefixes(ImmutableList<String> scanPrefixes) {
      this.scanPrefixes = scanPrefixes;
      return this;
    }
    
    public Config build() {
      return new Config(enableDebugMode, autoReloadPrefixes, scanPrefixes, ssl.build(), server.build(), mail.build(), db.build());
    }
  }
  
  public static Builder newBuilder() {
    return new Builder();
  }
}
