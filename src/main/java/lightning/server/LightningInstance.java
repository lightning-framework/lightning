package lightning.server;

import java.io.File;
import java.io.FileInputStream;

import lightning.config.Config;
import lightning.db.MySQLDatabaseProvider;
import lightning.db.MySQLDatabaseProviderImpl;
import lightning.inject.InjectorModule;
import lightning.json.GsonFactory;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.util.log.Log;

public class LightningInstance {
  private static LightningServer server;
  private static Config config;
  private static MySQLDatabaseProvider dbp;
  
  public static LightningServer start(File file, InjectorModule injector) throws Exception {
    Config cfg = GsonFactory.newJsonParser().fromJson(IOUtils.toString(new FileInputStream(file)), Config.class);
    return start(cfg, injector);
  }
  
  public static LightningServer start(File file, Class<? extends Config> clazz, InjectorModule injector) throws Exception {
    Config cfg = GsonFactory.newJsonParser().fromJson(IOUtils.toString(new FileInputStream(file)), clazz);
    return start(cfg, injector);
  }
  
  public static LightningServer start(File file) throws Exception {
    return start(file, new InjectorModule());
  }
  
  public static LightningServer start(Config cfg) throws Exception {
    return start(cfg, new InjectorModule());
  }
  
  public static LightningServer start(File file, Class<? extends Config> clazz) throws Exception {
    return start(file, clazz, new InjectorModule());
  }
  
  public static LightningServer start(Config cfg, InjectorModule injector) throws Exception {
    config = cfg;
    Log.setLog(null);    
    
    dbp = new MySQLDatabaseProviderImpl(config.db);
        
    server = new LightningServer(config, dbp, injector);
    server.start();
    return server;
  }
}
