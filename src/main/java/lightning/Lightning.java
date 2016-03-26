package lightning;

import lightning.config.Config;
import lightning.server.LightningInstance;

public final class Lightning {
  public static void launch(Config config) throws Exception {
    LightningInstance.start(config);
  }
}
