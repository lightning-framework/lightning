package lightning;

import java.io.File;

import lightning.config.Config;
import lightning.inject.InjectorModule;
import lightning.server.LightningInstance;
import lightning.server.LightningServer;

/**
 * Provides static convenience methods for initializing the framework.
 */
public final class Lightning {
  /**
   * Starts the application server using the given configuration parameters.
   * @param config
   * @throws Exception On failure to initialize sever.
   */
  public static LightningServer launch(Config config) throws Exception {
    return LightningInstance.start(config).join();
  }
  
  /**
   * Starts the application server by reading configuration from a given file (JSON).
   * @param file A JSON-formatted config file (see source code of Config for structure).
   * @throws Exception
   */
  public static LightningServer launch(File file) throws Exception {
    return LightningInstance.start(file).join();
  }
  
  /**
   * Starts the application server by reading configuration from a given file.
   * Your custom class will be available in handlers (cast config() to an instance of your config class).
   * @param file A JSON-formatted config file (should contain all of the fields in Config + fields in your class).
   * @param clazz A custom class you defined which extends config and may contain additional options.
   * @throws Exception
   */
  public static LightningServer launch(File file, Class<? extends Config> clazz) throws Exception {
    return LightningInstance.start(file, clazz).join();
  }
  
  /**
   * Starts the application server with the given configuration and dependency injection module.
   * @param config
   * @param injector
   * @return
   * @throws Exception
   */
  public static LightningServer launch(Config config, InjectorModule injector) throws Exception {
    return LightningInstance.start(config, injector).join();
  }
  
  /**
   * Starts the application server by reading config from the given JSON file and using the given
   * dependency injection module.
   * @param file
   * @param injector
   * @return
   * @throws Exception
   */
  public static LightningServer launch(File file, InjectorModule injector) throws Exception {
    return LightningInstance.start(file, injector).join();
  }
  
  /**
   * Starts the application server by reading config from the given JSON file and using the given
   * dependency injection module.
   * @param file The JSON-formatted config file
   * @param clazz The class used to parse the config file (a subclass of lightning.config.Config).
   * @param injector The dependency injection module.
   * @return An instance of the server.
   * @throws Exception On failure.
   */
  public static LightningServer launch(File file, Class<? extends Config> clazz, InjectorModule injector) throws Exception {
    return LightningInstance.start(file, clazz, injector).join();
  }
}
