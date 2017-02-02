package lightning.enums;

import static lightning.util.ReflectionUtil.requireArgsStartWith;
import static lightning.util.ReflectionUtil.requireIsPublicInstanceWithReturnType;
import static lightning.util.ReflectionUtil.requireIsPublicStaticWithReturnType;
import static lightning.util.ReflectionUtil.requireOnClassAnnotatedWith;

import java.lang.reflect.Method;

import lightning.ann.OnEvent;
import lightning.ann.WebSocket;
import lightning.exceptions.LightningValidationException;
import lightning.util.ReflectionUtil;

/**
 * Annotate a method with @OnEvent to register it as an event handler.
 */
public enum EventType {
  WEBSOCKET_HANDSHAKE,
  WEBSOCKET_TEXT_MESSAGE,
  WEBSOCKET_BINARY_MESSAGE,
  WEBSOCKET_CONNECT,
  WEBSOCKET_CLOSE,
  WEBSOCKET_ERROR;

  public void validate(Method method) throws LightningValidationException {
    requireUniqueEventHandler(method);

    switch (this) {
      case WEBSOCKET_BINARY_MESSAGE:
        requireOnClassAnnotatedWith(method, WebSocket.class);
        requireIsPublicInstanceWithReturnType(method, void.class);
        requireArgsStartWith(method, byte[].class, int.class, int.class);
        break;
      case WEBSOCKET_CLOSE:
        requireOnClassAnnotatedWith(method, WebSocket.class);
        requireIsPublicInstanceWithReturnType(method, void.class);
        requireArgsStartWith(method, int.class, String.class);
        break;
      case WEBSOCKET_CONNECT:
        requireOnClassAnnotatedWith(method, WebSocket.class);
        requireIsPublicInstanceWithReturnType(method, void.class);
        break;
      case WEBSOCKET_ERROR:
        requireOnClassAnnotatedWith(method, WebSocket.class);
        requireIsPublicInstanceWithReturnType(method, void.class);
        requireArgsStartWith(method, Throwable.class);
        break;
      case WEBSOCKET_HANDSHAKE:
        requireOnClassAnnotatedWith(method, WebSocket.class);
        requireIsPublicStaticWithReturnType(method, boolean.class);
        break;
      case WEBSOCKET_TEXT_MESSAGE:
        requireOnClassAnnotatedWith(method, WebSocket.class);
        requireIsPublicInstanceWithReturnType(method, void.class);
        requireArgsStartWith(method, String.class);
        break;
    }
  }

  public int getReservedArgCount() {
    switch (this) {
      case WEBSOCKET_BINARY_MESSAGE:
        return 3;

      case WEBSOCKET_CLOSE:
        return 2;

      case WEBSOCKET_ERROR:
      case WEBSOCKET_TEXT_MESSAGE:
        return 1;

      default:
        return 0;
    }
  }

  private void requireUniqueEventHandler(Method method) throws LightningValidationException {
    for (Method m : ReflectionUtil.getMethodsAnnotatedWith(method.getDeclaringClass(), OnEvent.class)) {
      if (m.equals(method)) {
        continue;
      }

      OnEvent info = m.getAnnotation(OnEvent.class);

      if (info != null && info.value() == this) {
        throw new LightningValidationException(method, "@OnEvent(" + this.toString() + ") must be unique on declaring class (and parents).\n");
      }
    }
  }
}
