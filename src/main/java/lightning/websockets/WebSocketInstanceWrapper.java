package lightning.websockets;

import java.net.SocketTimeoutException;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lightning.scanner.Snapshot;
import lightning.websockets.WebSocketHolder.WebSocketBindings;

@WebSocket
public final class WebSocketInstanceWrapper implements WebSocketListener {
  private final Logger LOGGER = LoggerFactory.getLogger(WebSocketInstanceWrapper.class);
  private final WebSocketBindings bindings;
  private final WebSocketHandlerContext context;
  private final Object handler;
  private Snapshot snapshot;

  public WebSocketInstanceWrapper(WebSocketBindings bindings,
                                  WebSocketHandlerContext context,
                                  Object handler) throws Exception {
    this.handler = handler;
    this.bindings = bindings;
    this.context = context;
    this.snapshot = context.config().enableDebugMode && !context.config().isRunningFromJAR()
        ? Snapshot.capture(context.config().autoReloadPrefixes)
        : null;
  }

  private boolean isCodeChanged() throws Exception {
    return context.config().canReloadClass(handler.getClass()) &&
           !Snapshot.capture(context.config().autoReloadPrefixes).equals(snapshot);
  }

  @Override
  public void onWebSocketClose(int status, String reason) {
    try {
      if (context.hasSession()) {
        if (bindings.close != null) {
          try {
            context.install();
            context.injector().invoke(bindings.close, handler, status, reason);
          } finally {
            context.uninstall();
          }
        }
      }
    } catch (Exception e) {
      // The session is closed already.
      // Suppress the exception and log it.
      LOGGER.warn("Exception in web socket close handler:", e);
    } finally {
      context.clearSession();
    }
  }

  @Override
  public void onWebSocketConnect(Session session) {
    try {
      if (!isCodeChanged()) {
        context.setSession(session);
        if (bindings.open != null) {
          try {
            context.install();
            context.injector().invoke(bindings.open, handler);
          } finally {
            context.uninstall();
          }
        }
      } else {
        session.close(1011, "Code Reloaded");
      }
    } catch (Exception e) {
      onWebSocketError(e);
    }
  }

  @Override
  public void onWebSocketError(Throwable error) {
    try {
      if (bindings.error != null) {
        context.injector().invoke(bindings.error, handler, error);
      } else {
        if (!(error instanceof SocketTimeoutException)) {
          LOGGER.warn("A web socket error occured: ", error);
        }
      }
    } catch (Exception e) {
      // The session is going to be closed anyways.
      // Suppress the exception and log it.
      LOGGER.warn("Exception in web socket error handler: ", e);
    } finally {
      if (context.hasSession()) {
        if (error instanceof SocketTimeoutException) {
          // Has no effect (Jetty just drops the TCP connection).
          context.close(1000, "Timed Out");
          // Jetty apparently doesn't fire the close event on all errors (e.g. timeouts).
          onWebSocketClose(1000, "Timed Out");
        } else if (context.config().enableDebugMode) {
          context.close(1011, error.getClass().getCanonicalName());
          onWebSocketClose(1011, error.getClass().getCanonicalName());
        } else {
          context.close(1011, "Internal Error");
          onWebSocketClose(1011, "Internal Error");
        }
      }
    }
  }

  @Override
  public void onWebSocketBinary(byte[] payload, int off, int len) {
    try {
      if (!isCodeChanged()) {
        if (bindings.binaryMessage != null) {
          try {
            context.install();
            context.injector().invoke(bindings.binaryMessage, handler, payload, off, len);
          } finally {
            context.uninstall();
          }
        } else {
          context.close(1003, "Unsupported Message Type");
        }
      } else {
        context.close(1011, "Code Reloaded");
      }
    } catch (Exception e) {
      onWebSocketError(e);
    }
  }

  @Override
  public void onWebSocketText(String message) {
    try {
      if (!isCodeChanged()) {
        if (bindings.textMessage != null) {
          try {
            context.install();
            context.injector().invoke(bindings.textMessage, handler, message);
          } finally {
            context.uninstall();
          }
        } else {
          context.close(1003, "Unsupported Message Type");
        }
      } else {
        context.close(1011, "Code Reloaded");
      }
    } catch (Exception e) {
      onWebSocketError(e);
    }
  }
}
