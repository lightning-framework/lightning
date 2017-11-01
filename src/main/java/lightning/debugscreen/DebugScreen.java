package lightning.debugscreen;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import freemarker.template.TemplateException;
import lightning.config.Config;
import lightning.enums.HTTPHeader;
import lightning.enums.HTTPStatus;
import lightning.http.Request;
import lightning.mvc.HandlerContext;
import lightning.routing.RouteMapper.Match;
import lightning.templates.FreeMarkerTemplateEngine;
import lightning.templates.TemplateEngine;
import lightning.util.Iterables;

/**
 * Renders an interactive in-browser stack trace when a controller throws an uncaught exception.
 */
public class DebugScreen {
  protected final TemplateEngine templates;
  protected final SourceLocator[] sourceLocators;

  public DebugScreen(Config config) throws Exception {
    this(config,
         new LocalSourceLocator("./src/main/java"),
         new LocalSourceLocator("./src/test/java"));
  }

  public DebugScreen(Config config, SourceLocator... sourceLocators) throws Exception {
    this.templates = new FreeMarkerTemplateEngine(getClass(), "/lightning");
    this.sourceLocators = sourceLocators;
  }

  private void addToChain(Throwable throwable,
                          ArrayList<LinkedHashMap<String, Object>> exceptionChain,
                          boolean isSuppressed) {
    LinkedHashMap<String, Object> exceptionInfo = new LinkedHashMap<>();
    String trace = traceToString(throwable);
    exceptionInfo.put("frames", parseFrames(throwable));
    exceptionInfo.put("short_message", StringUtils.abbreviate(Optional.fromNullable(throwable.getMessage()).or(""), 100));
    exceptionInfo.put("full_trace", trace);
    exceptionInfo.put("show_full_trace", trace.length() > 100);
    exceptionInfo.put("message", Optional.fromNullable(throwable.getMessage()).or(""));
    exceptionInfo.put("plain_exception", ExceptionUtils.getStackTrace(throwable));
    exceptionInfo.put("name", throwable.getClass().getCanonicalName().split("\\."));
    exceptionInfo.put("basic_type", throwable.getClass().getSimpleName());
    exceptionInfo.put("type", throwable.getClass().getCanonicalName());
    exceptionInfo.put("suppressed", isSuppressed);
    exceptionChain.add(exceptionInfo);
  }

  private void crawlChain(Throwable throwable,
                          ArrayList<LinkedHashMap<String, Object>> exceptionChain,
                          boolean isSuppressed) {
    addToChain(throwable, exceptionChain, isSuppressed);

    for (Throwable suppressedThrowable : throwable.getSuppressed()) {
      crawlChain(suppressedThrowable, exceptionChain, true);
    }

    if (throwable.getCause() != null) {
      crawlChain(throwable.getCause(), exceptionChain, false);
    }
  }

  public final void handle(Throwable throwable,
                           HandlerContext ctx,
                           Match<Object> match) throws IOException {
    // Set the response status (important so that AJAX requests will fail).
    ctx.response.raw().setStatus(HTTPStatus.INTERNAL_SERVER_ERROR.getCode());

    try {
      LinkedHashMap<String, Object> model = new LinkedHashMap<>();
      ArrayList<LinkedHashMap<String, Object>> exceptionChain = new ArrayList<>();
      crawlChain(throwable, exceptionChain, false);
      model.put("exceptions", exceptionChain);
      model.put("route_map_path", ctx.config.debugRouteMapPath);

      LinkedHashMap<String, Map<String, ? extends Object>> tables = new LinkedHashMap<>();
      tables.put("Request Handler", getHandlerInfo(ctx, match));
      installTables(tables, ctx);
      model.put("tables", tables);

      ctx.response.raw().setHeader(HTTPHeader.CONTENT_TYPE.httpName(), "text/html; charset=UTF-8");
      templates.render("debugscreen.ftl", model, ctx.response.raw().getWriter());
    } catch (Exception e) {
      // A simple fallback in case an error occurs trying to generate the error page.
      ctx.response.raw().getWriter().println(
                "<html>"
              + "  <head>"
              + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
              + "  </head>"
              + "  <body>"
              + "    <h1>Caught Exception:</h1>"
              + "    <pre>"
              +        ExceptionUtils.getStackTrace(throwable)
              + "    </pre>"
              + "    <h1>Caught Exception (while rendering debug screen):</h1>"
              + "    <pre>"
              +        ExceptionUtils.getStackTrace(e)
              + "    </pre>"
              + "  </body>"
              + "</html>");
    }
  }

  protected void installTables(LinkedHashMap<String, Map<String, ? extends Object>> tables,
                               HandlerContext ctx) {
    tables.put("Request", getRequestInfo(ctx));

    tables.put("Request Headers",
               setToLinkedHashMap(ctx.request.headers(),
                                  h -> h,
                                  h -> ctx.request.header(h).stringOption().or("-")));
    tables.get("Request Headers").remove("Cookie"); // Will show up under 'Cookies' table.

    tables.put("Request Route Parameters",
               setToLinkedHashMap(ctx.request.routeParams(),
                                  k -> k,
                                  k -> ctx.request.routeParam(k).stringOption().or("-")));

    tables.put("Request Query Parameters",
               setToLinkedHashMap(ctx.request.queryParams(),
                                  k -> k,
                                  k -> ctx.request.queryParam(k).stringOption().or("-")));

    tables.put("Request Properties",
               setToLinkedHashMap(ctx.request.properties(),
                                  k -> k,
                                  k -> ctx.request.property(k).stringOption().or("-")));

    if (ctx.config.server.hmacKey != null) {
      tables.put("Request Cookies (Signed)",
                 setToLinkedHashMap(ctx.request.cookies(),
                                    k -> k,
                                    k -> ctx.request.cookie(k).stringOption().or("-")));
    } else {
      tables.put("Request Cookies (Signed)", ImmutableMap.of());
    }

    try {
      tables.put("Session", ctx.session().asMap());
      tables.put("Auth",
                 ctx.auth().isLoggedIn()
                   ? ImmutableMap.of("user", ctx.user().getId() + ":" + ctx.user().getUserName())
                   : ImmutableMap.of());
    } catch (Exception e) {
      tables.put("Session", ImmutableMap.of());
      tables.put("Auth", ImmutableMap.of());
    }

    tables.put("Lightning Environment", getEnvironmentInfo(ctx));
    tables.put("System Environment", System.getenv());
  }

  private LinkedHashMap<String, Object> getHandlerInfo(HandlerContext ctx, Match<Object> match) {
    LinkedHashMap<String, Object> data = new LinkedHashMap<>();

    if (match == null) {
      data.put("Controller", "N/A");
      data.put("Method", "N/A");
    }
    else if (match.getData() instanceof Method) {
      Method method = (Method)match.getData();
      data.put("Controller", method.getDeclaringClass().getCanonicalName());
      data.put("Method", method.getName());
    }
    else if (match.getData() instanceof Class) {
      Class<?> clazz = (Class<?>)match.getData();
      data.put("Controller", clazz.getCanonicalName());
      data.put("Method", "N/A");
    }
    else {
      data.put("Controller", "N/A");
      data.put("Method", "N/A");
    }

    return data;
  }

  private LinkedHashMap<String, Object> getEnvironmentInfo(HandlerContext ctx) {
    LinkedHashMap<String, Object> environment = new LinkedHashMap<>();
    environment.put("Thread ID", Thread.currentThread().getId());
    environment.put("Debug Mode", Boolean.toString(ctx.config.enableDebugMode));
    environment.put("Scan Prefixes", ctx.config.scanPrefixes.toString());
    environment.put("Auto-Reload Prefixes", ctx.config.autoReloadPrefixes.toString());
    environment.put("SSL Enabled", Boolean.toString(ctx.config.ssl.isEnabled()));
    environment.put("HTTP2 Enabled", Boolean.toString(ctx.config.server.enableHttp2));
    environment.put("HTTP2C Enabled", Boolean.toString(ctx.config.server.enableHttp2C));
    environment.put("Multipart Enabled", Boolean.toString(ctx.config.server.multipartEnabled));
    environment.put("Output Buffering", Boolean.toString(ctx.config.server.enableOutputBuffering));
    environment.put("Template File Path", ctx.config.server.templateFilesPath);
    environment.put("Static File Path", ctx.config.server.staticFilesPath);
    return environment;
  }

  private LinkedHashMap<String, Object> getRequestInfo(HandlerContext ctx) {
    Request request = ctx.request;
    LinkedHashMap<String, Object> req = new LinkedHashMap<>();
    req.put("Protocol", Optional.fromNullable(request.raw().getProtocol()).or("UNKNOWN"));
    req.put("Scheme", request.scheme().toString().toUpperCase());
    req.put("Method", request.method().toString());
    req.put("URL", Optional.fromNullable(request.url()).or("-"));
    req.put("Remote IP", Optional.fromNullable(request.ip()).or("-"));
    req.put("Path", Optional.fromNullable(request.path()).or("-"));
    req.put("Host", Optional.fromNullable(request.host()).or("-"));
    req.put("Port", Optional.fromNullable(Integer.toString(ctx.config.server.port)).or("-"));
    req.put("URI", Optional.fromNullable(request.uri()).or("-"));
    return req;
  }

  private LinkedHashMap<String, String> setToLinkedHashMap(Set<String> set,
                                                           Function<String, String> keyMapper,
                                                           Function<String, String> valueMapper) {
    return set.stream().collect(Collectors.toMap(keyMapper,
                                                 valueMapper,
                                                 (k, v) -> k,
                                                 LinkedHashMap::new));
  }

  private String traceToString(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    if (t instanceof TemplateException) {
      ((TemplateException) t).printStackTrace(pw, false, true, false);
    } else {
      t.printStackTrace(pw);
    }
    pw.close();
    return sw.toString();
  }

  /**
   * Parses all stack frames for an exception into a view model.
   *
   * @param e An exception.
   * @return A view model for the frames in the exception.
   */
  private List<Map<String, Object>> parseFrames(Throwable e) {
    ImmutableList.Builder<Map<String, Object>> frames = ImmutableList.builder();

    for (StackTraceElement frame : e.getStackTrace()) {
      frames.add(parseFrame(frame));
    }

    return frames.build();
  }

  /**
   * Parses a stack frame into a view model.
   *
   * @param sframe A stack trace frame.
   * @return A view model for the given frame in the template.
   */
  private Map<String, Object> parseFrame(StackTraceElement sframe) {
    ImmutableMap.Builder<String, Object> frame = ImmutableMap.builder();
    frame.put("file", Optional.fromNullable(sframe.getFileName()).or("<#unknown>"));
    frame.put("class", Optional.fromNullable(sframe.getClassName()).or(""));
    frame.put("line", Optional.fromNullable(Integer.toString(sframe.getLineNumber())).or(""));
    frame.put("function", Optional.fromNullable(sframe.getMethodName()).or(""));
    frame.put("comments", ImmutableList.of());

    // Try to find the source file corresponding to this exception stack frame.
    // Go through the locators in order until the source file is found.
    Optional<SourceFile> file = Optional.absent();
    for (SourceLocator locator : sourceLocators) {
      file = locator.findFileForFrame(sframe);

      if (file.isPresent()) {
        break;
      }
    }

    if (file.isPresent()) {
      // Fetch +-10 lines from the triggering line.
      Optional<Map<Integer, String>> codeLines = file.get().getLines(sframe);

      if (codeLines.isPresent()) {
        // Write the starting line number (1-indexed).
        frame.put("code_start",
                  Iterables.reduce(codeLines.get().keySet(), Integer.MAX_VALUE, Math::min) + 1);

        // Write the code as a single string, replacing empty lines with a " ".
        frame.put("code",
                  Joiner.on("\n").join(Iterables.map(codeLines.get().values(),
                                                     (x) -> x.length() == 0 ? " " : x)));

        // Write the canonical path.
        try {
          frame.put("canonical_path", file.get().getPath());
        } catch (Exception e) {
          // Not much we can do, so ignore and just don't have the canonical path.
        }
      }
    }

    return frame.build();
  }
}
