package lightning.debugmap;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.augustl.pathtravelagent.DefaultPathToPathSegments;
import com.augustl.pathtravelagent.PathFormatException;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import lightning.ann.Before;
import lightning.ann.Controller;
import lightning.ann.ExceptionHandler;
import lightning.ann.Json;
import lightning.ann.Route;
import lightning.ann.WebSocket;
import lightning.config.Config;
import lightning.enums.HTTPMethod;
import lightning.mvc.HandlerContext;
import lightning.routing.FilterMapper.FilterMatch;
import lightning.routing.RouteMapper;
import lightning.routing.RouteMapper.Match;
import lightning.scanner.ScanResult;
import lightning.server.LightningHandler;
import lightning.util.ReflectionUtil;
import lightning.websockets.WebSocketHolder;

/**
 * Renders an in-browser table displaying all installed handlers.
 * Includes routes, web sockets, filters, exception handlers, etc.
 */
@Controller
public class DebugMapController {
  public static void map(RouteMapper<Object> mapper, Config config) throws Exception {
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
    templateEngine.setClassForTemplateLoading(getClass(), "/lightning");
  }

  public void handle(LightningHandler handler, HandlerContext ctx) throws Exception {
    Map<String, Object> model = new LinkedHashMap<String, Object>();

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
    exceptionHandlers.sort(new ExceptionComparator());
    model.put("exception_handlers", exceptionHandlers);

    // TODO: We could package in a debug web socket client.
    List<Object> websockets = new ArrayList<>();
    for (Class<?> type : result.websockets) {
        WebSocket ws = notNull(type.getAnnotation(WebSocket.class));
        websockets.add(ImmutableMap.<String, Object>of(
          "path", ws.path(),
          "target", type.getCanonicalName()
        ));
    }
    websockets.sort(new PathComparator());
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
    beforeFilters.sort(new PathComparator());
    model.put("before_filters", beforeFilters);

    List<Object> routes = new ArrayList<>();
    for (Class<?> type : result.routes.keySet()) {
      for (Method target : result.routes.get(type)) {
        Route r = notNull(target.getAnnotation(Route.class));
        routes.add(ImmutableMap.<String, Object>of(
          "path", r.path(),
          "methods", Joiner.on(", ").join(r.methods()),
          "target", type.getCanonicalName() + "@" + target.getName(),
          "has_params", r.path().contains(":") || r.path().contains("*")
        ));
      }
    }
    routes.sort(new PathComparator());
    model.put("routes", routes);

    model.put("http_methods", HTTPMethod.values());

    templateEngine.getTemplate("debugmap.ftl")
                  .process(model, ctx.response.getWriter());
  }

  private static final class ExceptionComparator implements Comparator<Object> {
    @SuppressWarnings("unchecked")
    @Override
    public int compare(Object a, Object b) {
      Map<String, Object> ma = (Map<String, Object>)a;
      Map<String, Object> mb = (Map<String, Object>)b;
      return ((String)ma.get("exception")).compareTo((String)mb.get("exception"));
    }
  }

  private static final class PathComparator implements Comparator<Object> {
    @SuppressWarnings("unchecked")
    @Override
    public int compare(Object a, Object b) {
      Map<String, Object> ma = (Map<String, Object>)a;
      Map<String, Object> mb = (Map<String, Object>)b;
      return comparePath((String)ma.get("path"), (String)mb.get("path"));
    }

    public int comparePath(String a, String b) {
      if (a.equals("*") && b.equals("*")) {
        return 0;
      }
      else if (a.equals("*") && !b.equals("*")) {
        return -1;
      }
      else if (b.equals("*") && !a.equals("*")) {
        return 1;
      }

      try {
        Iterator<String> as = DefaultPathToPathSegments.parse(a).iterator();
        Iterator<String> bs = DefaultPathToPathSegments.parse(b).iterator();

        while (as.hasNext() && bs.hasNext()) {
          int cmp = compareSegment(as.next(), bs.next());

          if (cmp != 0) {
            return cmp;
          }
        }

        if (as.hasNext()) {
          return 1;
        }
        else if (bs.hasNext()) {
          return -1;
        }
        else {
          return 0;
        }
      } catch (PathFormatException e) {
        e.printStackTrace(System.err);
        return 0;
      }
    }

    public int compareSegment(String a, String b) {
      if (a.equals("*") && !b.equals("*")) {
        return -1;
      }

      if (b.equals("*") && !a.equals("*")) {
        return 1;
      }

      if (a.startsWith(":") && !b.startsWith(":")) {
        return 1;
      }

      if (b.startsWith(":") && !a.startsWith(":")) {
        return -1;
      }

      return a.compareTo(b);
    }
  }

  @Json
  public Object handlePost(LightningHandler handler, HandlerContext ctx) {
    String path = ctx.queryParam("path").stringValue();
    HTTPMethod method = ctx.queryParam("method").enumValue(HTTPMethod.class);

    try {
      return ImmutableMap.of("status", "success",
                             "matches", getMatches(handler, path, method));
    } catch (Exception e) {
      return ImmutableMap.of("status", "error",
                             "type", e.getClass().getCanonicalName(),
                             "message", e.getMessage());
    }
  }

  private static List<String> getMatches(LightningHandler handler, String path, HTTPMethod method) throws PathFormatException {
    List<String> matches = new ArrayList<>();
    Match<Object> route = handler.getRouteMatch(path, method);

    if (route != null) {
      if (route.getData() instanceof WebSocketHolder) {
        WebSocketHolder socket = (WebSocketHolder)route.getData();
        matches.add(socket.getType().getCanonicalName());
      }

      if (route.getData() instanceof Method) {
        FilterMatch<Method> filters = handler.getFilterMatch(path, method);
        Method m = (Method)route.getData();

        for (lightning.routing.FilterMapper.Filter<Method> filter : filters.beforeFilters()) {
          matches.add(filter.handler.getDeclaringClass().getCanonicalName() + "@" + filter.handler.getName());
        }

        matches.add(m.getDeclaringClass().getCanonicalName() + "@" + m.getName());
      }
    }

    return matches;
  }

  private static <T> T notNull(T item) {
    if (item == null) {
      throw new IllegalStateException();
    }

    return item;
  }
}
