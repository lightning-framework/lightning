package lightning;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;

import lightning.config.Config;
import lightning.json.JsonFactory;
import lightning.server.LightningInstance;

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
    launch(JsonFactory.newJsonParser().fromJson(IOUtils.toString(new FileInputStream(file)), Config.class));
  }
  
  /**
   * Starts the application server by reading configuration from a given file.
   * Your custom class will be available in handlers (cast config() to an instance of your config class).
   * @param file A JSON-formatted config file (should contain all of the fields in Config + fields in your class).
   * @param clazz A custom class you defined which extends config and may contain additional options.
   * @throws Exception
   */
  public static void launch(File file, Class<? extends Config> clazz) throws Exception {
    launch(JsonFactory.newJsonParser().fromJson(IOUtils.toString(new FileInputStream(file)), clazz));
  }
}
