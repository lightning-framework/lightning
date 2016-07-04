package lightning.server;

import java.io.File;
import java.io.FileInputStream;

import lightning.config.Config;
import lightning.inject.InjectorModule;
import lightning.json.GsonFactory;

import org.apache.commons.io.IOUtils;

public class LightningFactory {  
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
    LightningServer server = new LightningServer(cfg, injector);
    server.start();
    return server;
  }
}
