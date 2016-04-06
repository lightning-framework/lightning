package lightning;

import java.io.File;

import lightning.config.Config;
import lightning.inject.InjectorModule;
import lightning.server.LightningInstance;

// TODO(mschurr): Provide a way to stop the server after launching it.
public final class Lightning {
  /**
   * Starts the application server using the given configuration parameters.
   * @param config
   * @throws Exception On failure to initialize sever.
   */
  public static void launch(Config config) throws Exception {
    LightningInstance.start(config);
  }
  
  /**
   * Starts the application server by reading configuration from a given file.
   * @param file A JSON-formatted config file (see source code of Config for structure).
   * @throws Exception
   */
  public static void launch(File file) throws Exception {
    LightningInstance.start(file);
  }
  
  /**
   * Starts the application server by reading configuration from a given file.
   * Your custom class will be available in handlers (cast config() to an instance of your config class).
   * @param file A JSON-formatted config file (should contain all of the fields in Config + fields in your class).
   * @param clazz A custom class you defined which extends config and may contain additional options.
   * @throws Exception
   */
  public static void launch(File file, Class<? extends Config> clazz) throws Exception {
    LightningInstance.start(file, clazz);
  }
  
  public static void launch(Config config, InjectorModule injector) throws Exception {
    LightningInstance.start(config, injector);
  }
  
  public static void launch(File file, InjectorModule injector) throws Exception {
    LightningInstance.start(file, injector);
  }
  
  public static void launch(File file, Class<? extends Config> clazz, InjectorModule injector) throws Exception {
    LightningInstance.start(file, clazz, injector);
  }
}
