package lightning.server;

import java.io.File;
import java.io.FileInputStream;

import lightning.auth.Auth;
import lightning.auth.drivers.MySQLAuthDriver;
import lightning.config.Config;
import lightning.crypt.Hashing;
import lightning.crypt.SecureCookieManager;
import lightning.db.MySQLDatabaseProvider;
import lightning.groups.Groups;
import lightning.groups.drivers.MySQLGroupDriver;
import lightning.json.JsonFactory;
import lightning.mail.Mail;
import lightning.sessions.Session;
import lightning.sessions.drivers.MySQLSessionDriver;
import lightning.users.Users;
import lightning.users.drivers.MySQLUserDriver;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.util.log.Log;

public class LightningInstance {
  private static LightningServer server;
  private static Config config;
  private static MySQLDatabaseProvider dbp;
  
  public static void start(File file) throws Exception {
    Config cfg = JsonFactory.newJsonParser().fromJson(IOUtils.toString(new FileInputStream(file)), Config.class);
    start(cfg);
  }
  
  public static void start(File file, Class<? extends Config> clazz) throws Exception {
    Config cfg = JsonFactory.newJsonParser().fromJson(IOUtils.toString(new FileInputStream(file)), clazz);
    start(cfg);
  }
  
  public static void start(Config cfg) throws Exception {
    // TODO(mschurr): Eliminate global state on SecureCookieManager, Hashing, Mail, Session, Groups, Users, Auth.
    config = cfg;
    Log.setLog(null);    
    SecureCookieManager.setSecretKey(config.server.hmacKey);
    Hashing.setSecretKey(config.server.hmacKey);
    
    if (config.ssl.isEnabled()) {
      SecureCookieManager.alwaysSetSecureOnly();
    }
    
    if (config.mail.isEnabled()) {
      Mail.configure(config.mail);
    }
    
    dbp = new MySQLDatabaseProvider(config);
    
    Session.setDriver(new MySQLSessionDriver(dbp));
    Groups.setDriver(new MySQLGroupDriver(dbp));
    Users.setDriver(new MySQLUserDriver(dbp));
    Auth.setDriver(new MySQLAuthDriver(dbp));
    
    server = new LightningServer();
    server.configure(config, dbp);
    server.start();
  }
}
