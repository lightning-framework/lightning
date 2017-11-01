package lightning.inject;

import static lightning.util.ReflectionUtil.requireHasReturnValue;
import static lightning.util.ReflectionUtil.requireReturns;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;

import com.google.common.collect.ImmutableSet;

import lightning.ann.ExceptionHandler;
import lightning.ann.ExceptionHandlers;
import lightning.ann.Json;
import lightning.ann.JsonInput;
import lightning.ann.OnEvent;
import lightning.ann.QParam;
import lightning.ann.RParam;
import lightning.ann.Route;
import lightning.ann.Template;
import lightning.ann.WebSocket;
import lightning.cache.Cache;
import lightning.db.MySQLDatabase;
import lightning.enums.EventType;
import lightning.enums.HTTPMethod;
import lightning.exceptions.LightningValidationException;
import lightning.groups.Groups;
import lightning.http.Request;
import lightning.http.Response;
import lightning.mvc.HandlerContext;
import lightning.mvc.ModelAndView;
import lightning.mvc.URLGenerator;
import lightning.mvc.Validator;
import lightning.sessions.Session;
import lightning.users.User;
import lightning.users.Users;
import lightning.websockets.WebSocketHandlerContext;

public class InjectionValidator {
  public static final Set<Class<?>> REQUEST_CLASSES = ImmutableSet.of(
      Request.class,
      Response.class,
      Session.class,
      Validator.class,
      URLGenerator.class,
      Groups.class,
      Users.class,
      User.class,
      MySQLDatabase.class,
      HandlerContext.class,
      HttpServletRequest.class,
      HttpServletResponse.class,
      Injector.class,
      Cache.class);

  public static final Set<Class<?>> WEBSOCKET_CLASSES = ImmutableSet.of(
      WebSocketHandlerContext.class);

  private final Injector module;
  private final List<String> blacklistedPrefixes;

  public InjectionValidator(Injector module, List<String> blacklist) {
    this.module = module;
    this.blacklistedPrefixes = blacklist;
  }

  private void validate(Parameter p, Set<Class<?>> whitelistedClasses) throws LightningValidationException {
    Class<?> type = p.getType();

    {
      Class<?> t = type;
      while (t != null) {
        if (whitelistedClasses.contains(t)) {
          return;
        }
        t = t.getSuperclass();
      }
    }

    if (whitelistedClasses.contains(Request.class) &&
        (p.getAnnotation(QParam.class) != null ||
         p.getAnnotation(RParam.class) != null)) {
      return;
    }

    for (String prefix : blacklistedPrefixes) {
      if (type.getCanonicalName().startsWith(prefix)) {
        throw new LightningValidationException("Failed to injection for parameter: type must not be in autoreload prefixes: " + p);
      }
    }

    Object x = null;

    try {
      x = module.getInjectedArgument(p);
    } catch (Exception e) {
      throw new LightningValidationException("Failed to find injection for parameter: " + p, e);
    }

    if (x == null) {
      throw new LightningValidationException("Failed to find injection for parameter: " + p);
    }
  }

  public void validateConstructor(Class<?> c) throws LightningValidationException {
    if (c.getConstructors().length == 0) {
      return;
    }

    Set<Class<?>> classes = new HashSet<>(REQUEST_CLASSES);
    if (isWebSocket(c)) {
      classes.add(ServletUpgradeRequest.class);
      classes.add(ServletUpgradeResponse.class);
    }

    try {
      validate(c.getConstructors()[0].getParameters(), 0, classes);
    } catch (LightningValidationException e) {
      throw new LightningValidationException(c, "Failed to find injections for constructor: " + c.getCanonicalName(), e);
    }
  }

  public void validate(Method m) throws LightningValidationException {
    Parameter[] params = m.getParameters();
    int offset = 0;

    if (m.getAnnotation(OnEvent.class) != null) {
      // Skip reserved parameters.
      OnEvent e = m.getAnnotation(OnEvent.class);
      offset = e.value().getReservedArgCount();
    }

    Set<Class<?>> whitelist = new HashSet<>();

    if (m.getAnnotation(JsonInput.class) != null) {
      Route route;
      if ((route = m.getAnnotation(Route.class)) != null) {
        for (HTTPMethod httpMethod : route.methods()) {
          if (httpMethod == HTTPMethod.GET) {
            throw new LightningValidationException(m, "@JsonInput is not applicable to @Route containing methods {GET}.");
          }
        }
      }
    }

    if (m.getAnnotation(ExceptionHandler.class) != null ||
        m.getAnnotation(ExceptionHandlers.class) != null) {
      whitelist.addAll(REQUEST_CLASSES);
      whitelist.add(Throwable.class);
    }
    else if (isWebSocket(m.getDeclaringClass()) &&
             m.getAnnotation(OnEvent.class) != null) {
      OnEvent e = m.getAnnotation(OnEvent.class);

      if (e.value() == EventType.WEBSOCKET_HANDSHAKE) {
        whitelist.addAll(REQUEST_CLASSES);
        whitelist.add(ServletUpgradeRequest.class);
        whitelist.add(ServletUpgradeResponse.class);
      } else {
        whitelist.addAll(WEBSOCKET_CLASSES);
      }
    }
    else {
      whitelist.addAll(REQUEST_CLASSES);
    }

    JsonInput jsonInput;
    if ((jsonInput = m.getAnnotation(JsonInput.class)) != null) {
      whitelist.add(jsonInput.type());
    }

    try {
      validate(params, offset, whitelist);
    } catch (LightningValidationException e) {
      throw new LightningValidationException(m, "Failed to find injections for method: " + m, e);
    }
  }

  private void validate(Parameter[] params, int offset, Set<Class<?>> whitelist) throws LightningValidationException {
    while (offset < params.length) {
      validate(params[offset], whitelist);
      offset++;
    }
  }

  private boolean isWebSocket(Class<?> c) {
    while (c != null) {
      if (c.getAnnotation(WebSocket.class) != null) {
        return true;
      }
      c = c.getSuperclass();
    }
    return false;
  }


  public void validateRouteReturn(Method m) throws LightningValidationException {
    if (m.getAnnotation(Json.class) != null) {
      requireHasReturnValue(m); // Allow anything but void.
      return;
    }

    if (m.getAnnotation(Template.class) != null) {
      Template t = m.getAnnotation(Template.class);

      if (t.value() == null || t.value().isEmpty()) {
        requireReturns(m, ModelAndView.class);
      } else {
        requireHasReturnValue(m); // Allow anything but void.
      }

      return;
    }

    requireReturns(m,
                   ModelAndView.class,
                   File.class,
                   String.class,
                   void.class);
  }
}
