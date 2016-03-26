package lightning.config;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import lightning.mail.Mail;

public class Config {
  @Override
  public String toString() {
    return "Config [autoReloadPrefixes=" + autoReloadPrefixes + ", scanPrefixes=" + scanPrefixes
        + ", ssl=" + ssl + ", server=" + server + ", mail=" + mail + ", db=" + db
        + ", enableDebugMode=" + enableDebugMode + "]";
  }

  public List<String> autoReloadPrefixes = ImmutableList.of();
  public List<String> scanPrefixes = ImmutableList.of();
  public SSLConfig ssl = new SSLConfig();
  public ServerConfig server = new ServerConfig();
  public MailConfig mail = new MailConfig();
  public DBConfig db = new DBConfig();
  public boolean enableDebugMode = false;
  
  public static final class SSLConfig {
    @Override
    public String toString() {
      return "SSLConfig [keyStoreFile=" + keyStoreFile + ", keyStorePassword=" + keyStorePassword
          + ", trustStoreFile=" + trustStoreFile + ", trustStorePassword=" + trustStorePassword
          + "]";
    }

    public @Nullable String keyStoreFile;       // Required to enable SSL.
    public @Nullable String keyStorePassword;   // Required to enable SSL.
    public @Nullable String trustStoreFile;     // Optional.
    public @Nullable String trustStorePassword; // Optional.
    public boolean redirectInsecureRequests = true;
    
    public boolean isEnabled() {
      return keyStorePassword != null && keyStoreFile != null;
    }
  }
  
  public static final class ServerConfig {
    @Override
    public String toString() {
      return "ServerConfig [hmacKey=" + hmacKey + ", port=" + port + ", minThreads=" + minThreads
          + ", maxThreads=" + maxThreads + ", threadTimeoutMs=" + threadTimeoutMs
          + ", websocketTimeoutMs=" + websocketTimeoutMs + ", staticFilesPath=" + staticFilesPath
          + ", templateFilesPath=" + templateFilesPath + ", trustLoadBalancerHeaders="
          + trustLoadBalancerHeaders + "]";
    }
    public String hmacKey; // Should be long, random, and unique.
    public int port = 80;
    public int minThreads = 40;
    public int maxThreads = 250;
    public int threadTimeoutMs = (int) TimeUnit.SECONDS.toMillis(60);
    public int websocketTimeoutMs = (int) TimeUnit.SECONDS.toMillis(3); // For websockets.
    public int maxPostBytes = 2000000; // In bytes
    public int maxQueryParams = 100; // In number of params.
    public int connectionIdleTimeoutMs = (int) TimeUnit.MINUTES.toMillis(3);
    public String staticFilesPath; // Relative to src/main/java in your eclipse project folder.
    public String templateFilesPath; // Relative to src/main/java in your eclipse project folder.
    public String host = "localhost";
    public boolean trustLoadBalancerHeaders = false;
    public int maxCachedStaticFileSizeBytes = 1024 * 50; // 50KB
    public int maxStaticFileCacheSizeBytes = 1024 * 1024 * 30; // 30MB
    public int maxCachedStaticFiles = 500;
  }
  
  public static final class MailConfig implements Mail.MailConfig {
    @Override
    public String toString() {
      return "MailConfig [useSSL=" + useSSL + ", address=" + address + ", username=" + username
          + ", password=" + password + ", host=" + host + ", port=" + port + "]";
    }

    public boolean useSSL = true;
    public @Nullable String address;
    public @Nullable String username;
    public @Nullable String password;
    public @Nullable String host;
    public int port = 465;
    
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
  }
  
  public static final class DBConfig {
    @Override
    public String toString() {
      return "DBConfig [host=" + host + ", port=" + port + ", username=" + username + ", password="
          + password + ", name=" + name + "]";
    }
    public String host = "localhost";
    public int port = 3306;
    public String username = "httpd";
    public String password = "httpd";
    public String name;
  }
}
