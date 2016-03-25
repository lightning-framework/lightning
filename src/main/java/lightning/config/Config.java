package lightning.config;

import java.util.List;

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
    
    public boolean isEnabled() {
      return keyStorePassword != null && keyStoreFile != null;
    }
  }
  
  public static final class ServerConfig {
    @Override
    public String toString() {
      return "ServerConfig [hmacKey=" + hmacKey + ", port=" + port + ", threads=" + threads
          + ", timeoutMs=" + timeoutMs + ", staticFilesPath=" + staticFilesPath
          + ", templateFilesPath=" + templateFilesPath + ", trustLoadBalancerHeaders="
          + trustLoadBalancerHeaders + "]";
    }
    public String hmacKey; // Should be long, random, and unique.
    public int port = 80;
    public int threads = 250;
    public int timeoutMs = 10000; // For websockets.
    public String staticFilesPath; // Relative to src/main/java in your eclipse project folder.
    public String templateFilesPath; // Relative to src/main/java in your eclipse project folder.
    public boolean trustLoadBalancerHeaders = false;
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
