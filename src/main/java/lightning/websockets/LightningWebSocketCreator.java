package lightning.websockets;

import javax.annotation.Nullable;

import lightning.config.Config;
import lightning.inject.Injector;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LightningWebSocketCreator implements WebSocketCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(LightningWebSocketCreator.class);
    private final @Nullable WebSocketSingletonWrapper socket;
    private final Config config;
    private final Injector injector;
    private final Class<? extends WebSocketHandler> type;
    
    public LightningWebSocketCreator(Config config, Injector injector, Class<? extends WebSocketHandler> type, boolean isSingleton) throws Exception {
      this.config = config;
      this.injector = isSingleton ? null : injector;
      this.type = type;
      this.socket = isSingleton ? new WebSocketSingletonWrapper(config, injector, type) : null;
    }
    
    public Class<? extends WebSocketHandler> getType() {
      return type;
    }
    
    public boolean isCodeChanged() throws Exception {
      return socket != null && socket.isCodeChanged();
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest request, ServletUpgradeResponse response) {      
      try {
        if (socket != null) {
          if (socket.shouldAccept(request, response)) {
            return socket;
          }
        }
        else {
          WebSocketInstanceWrapper instance = new WebSocketInstanceWrapper(config, injector, type);
          
          if (instance.shouldAccept(request, response)) {
            return instance;
          }
        }
      } catch (Exception e) {
        LOGGER.warn("Failed to create web socket:", e);
      }
      
      return null;
    }
  }