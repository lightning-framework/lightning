package lightning.websockets;

import java.lang.reflect.Method;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lightning.ann.OnEvent;
import lightning.enums.EventType;
import lightning.mvc.HandlerContext;
import lightning.util.ReflectionUtil;

public class WebSocketHolder {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketHolder.class);
  private final WebSocketBindings bindings;

  public WebSocketHolder(Class<?> type) throws Exception {
    this.bindings = new WebSocketBindings(type);
  }

  public static final class WebSocketBindings {
    public final Class<?> type;
    public final Method handshake;
    public final Method open;
    public final Method close;
    public final Method error;
    public final Method textMessage;
    public final Method binaryMessage;

    public WebSocketBindings(Class<?> type) {
      this.type = type;
      this.handshake = getHandler(type, EventType.WEBSOCKET_HANDSHAKE);
      this.open = getHandler(type, EventType.WEBSOCKET_CONNECT);
      this.close = getHandler(type, EventType.WEBSOCKET_CLOSE);
      this.error = getHandler(type, EventType.WEBSOCKET_ERROR);
      this.textMessage = getHandler(type, EventType.WEBSOCKET_TEXT_MESSAGE);
      this.binaryMessage = getHandler(type, EventType.WEBSOCKET_BINARY_MESSAGE);
    }

    private static final Method getHandler(Class<?> clazz, EventType type) {
      for (Method m : ReflectionUtil.getMethodsAnnotatedWith(clazz, OnEvent.class)) {
        OnEvent e = m.getAnnotation(OnEvent.class);
        if (e.value() == type) {
          return m;
        }
      }

      return null;
    }
  }

  private static final class Creator implements WebSocketCreator {
    private final HandlerContext context;
    private final WebSocketBindings bindings;

    public Creator(HandlerContext context,
                   WebSocketBindings bindings) {
      this.context = context;
      this.bindings = bindings;
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest request,
                                  ServletUpgradeResponse response) {
      // TODO: We should replace these with lightning wrappers.
      context.bindings().bindClassToInstance(ServletUpgradeRequest.class, request);
      context.bindings().bindClassToInstance(ServletUpgradeResponse.class, response);

      try {
        boolean isAccepted = (bindings.handshake == null) ? true : (boolean)context.injector().invoke(bindings.handshake, null);

        if (!isAccepted) {
          return null;
        }

        return new WebSocketInstanceWrapper(bindings,
                                            new WebSocketHandlerContext(context),
                                            context.injector().newInstance(bindings.type));
      } catch (Exception e) {
        LOGGER.warn("Failed to create web socket:", e);
        return null;
      }
    }
  }

  public Class<?> getType() {
    return bindings.type;
  }

  public WebSocketCreator getCreator(HandlerContext context) {
    return new Creator(context, bindings);
  }
}
