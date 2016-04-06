package lightning.server;

import java.io.File;
import java.io.FileInputStream;

import lightning.config.Config;
import lightning.db.MySQLDatabaseProvider;
import lightning.db.MySQLDatabaseProviderImpl;
import lightning.inject.InjectorModule;
import lightning.json.JsonFactory;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.util.log.Log;

public class LightningInstance {
  private static LightningServer server;
  private static Config config;
  private static MySQLDatabaseProvider dbp;
  
  public static void start(File file, InjectorModule injector) throws Exception {
    Config cfg = JsonFactory.newJsonParser().fromJson(IOUtils.toString(new FileInputStream(file)), Config.class);
    start(cfg, injector);
  }
  
  public static void start(File file, Class<? extends Config> clazz, InjectorModule injector) throws Exception {
    Config cfg = JsonFactory.newJsonParser().fromJson(IOUtils.toString(new FileInputStream(file)), clazz);
    start(cfg, injector);
  }
  
  public static void start(File file) throws Exception {
    start(file, new InjectorModule());
  }
  
  public static void start(Config cfg) throws Exception {
    start(cfg, new InjectorModule());
  }
  
  public static void start(File file, Class<? extends Config> clazz) throws Exception {
    start(file, clazz, new InjectorModule());
  }
  
  public static void start(Config cfg, InjectorModule injector) throws Exception {
    config = cfg;
    Log.setLog(null);    
    
    dbp = new MySQLDatabaseProviderImpl(config);
        
    server = new LightningServer();
    server.configure(config, dbp, injector);
    server.start();
  }
}
