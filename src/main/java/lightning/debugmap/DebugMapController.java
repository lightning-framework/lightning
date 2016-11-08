package lightning.debugmap;

import static lightning.server.Context.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import lightning.ann.Before;
import lightning.ann.Controller;
import lightning.ann.ExceptionHandler;
import lightning.ann.Filter;
import lightning.ann.Filters;
import lightning.ann.Json;
import lightning.ann.Route;
import lightning.ann.WebSocketFactory;
import lightning.config.Config;
import lightning.enums.HTTPMethod;
import lightning.mvc.HandlerContext;
import lightning.routing.RouteMapper;
import lightning.scanner.ScanResult;
import lightning.server.LightningHandler;
import lightning.util.ReflectionUtil;

@Controller
public class DebugMapController {
  public static void map(RouteMapper<Method> mapper, Config config) throws Exception {
    if (config.debugRouteMapPath != null && config.enableDebugMode) {
      mapper.map(HTTPMethod.GET,
                 config.debugRouteMapPath,
                 ReflectionUtil.getMethod(DebugMapController.class, "handle"));
      mapper.map(HTTPMethod.POST,
                 config.debugRouteMapPath,
                 ReflectionUtil.getMethod(DebugMapController.class, "handlePost"));
    }
  }

  protected final Configuration templateEngine;

  public DebugMapController() {
    templateEngine = new Configuration(new Version(2, 3, 23));
    templateEngine.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    File path = new File("../lightning/src/main/resources/lightning");
    if (config().enableDebugMode && path.exists()) {
      try {
        templateEngine.setDirectoryForTemplateLoading(path);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      templateEngine.setClassForTemplateLoading(getClass(), "/lightning");
    }
  }

  public void handle(LightningHandler handler, HandlerContext ctx) throws Exception {
    Map<String, Object> model = new HashMap<String, Object>();

    ScanResult result = handler.getLastScanResult();

    List<Object> exceptionHandlers = new ArrayList<>();
    for (Class<?> type : result.exceptionHandlers.keySet()) {
      for (Method target : result.exceptionHandlers.get(type)) {
        ExceptionHandler eh = notNull(target.getAnnotation(ExceptionHandler.class));
        exceptionHandlers.add(ImmutableMap.<String, Object>of(
          "exception", eh.value().getCanonicalName(),
          "target", type.getCanonicalName() + "@" + target.getName()
        ));
      }
    }
    model.put("exception_handlers", exceptionHandlers);

    List<Object> websockets = new ArrayList<>();
    for (Class<?> type : result.websocketFactories.keySet()) {
      for (Method target : result.websocketFactories.get(type)) {
        WebSocketFactory ws = notNull(target.getAnnotation(WebSocketFactory.class));
        websockets.add(ImmutableMap.<String, Object>of(
          "path", ws.path(),
          "target", type.getCanonicalName() + "@" + target.getName()
        ));
      }
    }
    model.put("websockets", websockets);

    List<Object> beforeFilters = new ArrayList<>();
    for (Class<?> type : result.beforeFilters.keySet()) {
      for (Method target : result.beforeFilters.get(type)) {
        Before b = notNull(target.getAnnotation(Before.class));
        beforeFilters.add(ImmutableMap.<String, Object>of(
          "path", b.path(),
          "priority", b.priority(),
          "methods", Joiner.on(", ").join(b.methods()),
          "target", type.getCanonicalName() + "@" + target.getName()
        ));
      }
    }
    model.put("before_filters", beforeFilters);

    List<Object> routes = new ArrayList<>();
    for (Class<?> type : result.routes.keySet()) {
      for (Method target : result.routes.get(type)) {
        Route r = notNull(target.getAnnotation(Route.class));
        routes.add(ImmutableMap.<String, Object>of(
          "path", r.path(),
          "filters", parseFilters(target),
          "methods", Joiner.on(", ").join(r.methods()),
          "target", type.getCanonicalName() + "@" + target.getName()
        ));
      }
    }
    model.put("routes", routes);

    model.put("http_methods", HTTPMethod.values());

    templateEngine.clearTemplateCache();
    templateEngine.getTemplate("debugmap.ftl")
                  .process(model, ctx.response.getWriter());
  }

  @Json
  public Object handlePost(LightningHandler handler, HandlerContext ctx) {
    String path = ctx.queryParam("path").stringValue();
    HTTPMethod method = ctx.queryParam("method").enumValue(HTTPMethod.class);

    try {
      return ImmutableMap.of("status", "success",
                             "matches", handler.getMatches(path, method));
    } catch (Exception e) {
      return ImmutableMap.of("status", "error",
                             "type", e.getClass().getCanonicalName(),
                             "message", e.getMessage());
    }
  }

  private static <T> T notNull(T item) {
    if (item == null) {
      throw new IllegalStateException();
    }

    return item;
  }

  private static List<Object> parseFilters(Method target) {
    List<Object> filters = new ArrayList<>();

    Filters f2 = target.getAnnotation(Filters.class);
    Filter f1 = target.getAnnotation(Filter.class);

    if (f2 != null) {
      for (Filter f : f2.value()) {
        filters.add(f.value().getCanonicalName());
      }
    }

    if (f1 != null) {
      filters.add(f1.value().getCanonicalName());
    }

    return filters;
  }
}
