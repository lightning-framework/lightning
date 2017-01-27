package lightning.enums;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import com.google.common.base.Joiner;

import javassist.Modifier;
import lightning.ann.OnEvent;
import lightning.ann.WebSocket;
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

  public void validate(Method method) {
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

  private void requireArgsStartWith(Method method, Class<?> ...args) {
    String error = method + ": Must begin with arguments " + Joiner.on(", ").join(args) + ".";

    if (method.getParameterCount() < args.length) {
      throw new IllegalArgumentException(error);
    }

    Parameter[] params = method.getParameters();

    for (int i = 0; i < args.length; i++) {
      if (!params[i].getType().equals(args[i])) {
        throw new IllegalArgumentException(error);
      }
    }
  }

  private void requireUniqueEventHandler(Method method) {
    for (Method m : ReflectionUtil.getMethodsAnnotatedWith(method.getDeclaringClass(), OnEvent.class)) {
      if (m.equals(method)) {
        continue;
      }

      OnEvent info = m.getAnnotation(OnEvent.class);

      if (info != null && info.value() == this) {
        throw new IllegalStateException("@OnEvent(" + this.toString() + ") must be unique on declaring class (and parents).\n    " + method + "\n    " + m);
      }
    }
  }

  private void requireOnClassAnnotatedWith(Method method, Class<? extends Annotation> annotation) {
    if (method.getDeclaringClass().getAnnotation(annotation) == null) {
      throw new IllegalStateException(method + ": Must declare " + this.toString() + " event handler on an @" +
                                      annotation.getSimpleName() + ".");
    }
  }

  private void requireIsPublicStaticWithReturnType(Method method, Class<?> returnType) {
    if (!Modifier.isPublic(method.getModifiers()) ||
        !Modifier.isStatic(method.getModifiers()) ||
        !method.getReturnType().equals(returnType)) {
      throw new IllegalStateException(method + ": Must be public, static, and return " + returnType + ".");
    }
  }

  private void requireIsPublicInstanceWithReturnType(Method method, Class<?> returnType) {
    if (!Modifier.isPublic(method.getModifiers()) ||
        Modifier.isStatic(method.getModifiers()) ||
        !method.getReturnType().equals(returnType)) {
      throw new IllegalStateException(method + ": Must be public, static, and return " + returnType + ".");
    }
  }
}
