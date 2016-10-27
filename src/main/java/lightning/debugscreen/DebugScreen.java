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

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import lightning.enums.HTTPHeader;
import lightning.http.Request;
import lightning.mvc.HandlerContext;
import lightning.routing.RouteMapper.Match;
import lightning.util.Iterables;

/**
 * Displays a stack trace in-browser to users.
 */
public class DebugScreen {
    protected final Configuration templateConfig;
    protected final SourceLocator[] sourceLocators;

    public DebugScreen() {
        this(
                new LocalSourceLocator("./src/main/java"),
                new LocalSourceLocator("./src/test/java")
        );
    }

    public DebugScreen(SourceLocator... sourceLocators) {
        templateConfig = new Configuration(new Version(2, 3, 23));
        templateConfig.setClassForTemplateLoading(getClass(), "/lightning");
        this.sourceLocators = sourceLocators;
    }
    
    private void addToChain(Throwable throwable, ArrayList<LinkedHashMap<String, Object>> exceptionChain) {
      LinkedHashMap<String, Object> exceptionInfo = new LinkedHashMap<>();
      exceptionInfo.put("frames", parseFrames(throwable));
      exceptionInfo.put("short_message", Optional.fromNullable(StringUtils.abbreviate(throwable.getMessage(), 100)).or(""));
      exceptionInfo.put("full_trace", traceToString(throwable));
      exceptionInfo.put("message", Optional.fromNullable(throwable.getMessage()).or(""));
      exceptionInfo.put("plain_exception", ExceptionUtils.getStackTrace(throwable));
      exceptionInfo.put("name", throwable.getClass().getCanonicalName().split("\\."));
      exceptionInfo.put("basic_type", throwable.getClass().getSimpleName());
      exceptionInfo.put("type", throwable.getClass().getCanonicalName());
      exceptionChain.add(exceptionInfo);
    }
    
    public final void handle(Throwable throwable, HandlerContext ctx, Match<Method> match) throws IOException {
        ctx.response.raw().setStatus(500); // Internal Server Error
        
        try {
            LinkedHashMap<String, Object> model = new LinkedHashMap<>();
            ArrayList<LinkedHashMap<String, Object>> exceptionChain = new ArrayList<>();
            
            while (true) {
              addToChain(throwable, exceptionChain);
              
              for (Throwable s : throwable.getSuppressed()) {
                addToChain(s, exceptionChain);
              }
              
              // Process the next extension in the chain.
              if (throwable.getCause() == null) {
                break;
              } else {
                throwable = throwable.getCause();
              }
            }
            
            model.put("exceptions", exceptionChain);
          
            LinkedHashMap<String, Map<String, ? extends Object>> tables = new LinkedHashMap<>();
            tables.put("Request Handler", getHandlerInfo(ctx, match));
            installTables(tables, ctx);
            model.put("tables", tables);
            
            ctx.response.raw().setHeader(HTTPHeader.CONTENT_TYPE.httpName(), "text/html; charset=UTF-8");
            templateConfig.getTemplate("debugscreen.ftl").process(model, ctx.response.raw().getWriter());
        } catch (Exception e) {
            // In case we encounter any exceptions trying to render the error page itself,
            // have this simple fallback.
            ctx.response.raw().getWriter().println(
                              "<html>"
                            + "  <head>"
                            + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                            + "  </head>"
                            + "  <body>"
                            + "    <h1>Caught Exception:</h1>"
                            + "    <pre>" + ExceptionUtils.getStackTrace(throwable) + "</pre>"
                            + "    <h1>Caught Exception Rendering DebugScreen:</h1>"
                            + "    <pre>" + ExceptionUtils.getStackTrace(e) + "</pre>"
                            + "  </body>"
                            + "</html>"
            );
        }
    }

    /**
     * Install any tables you want to be shown in environment details.
     *
     * @param tables the map containing the tables to display on the debug screen
     */
    protected void installTables(LinkedHashMap<String, Map<String, ? extends Object>> tables, HandlerContext ctx) {
        tables.put("Request", getRequestInfo(ctx));
        
        tables.put("Request Headers", setToLinkedHashMap(ctx.request.headers(), h -> h, h -> ctx.request.header(h).stringOption().or("-")));
        tables.get("Request Headers").remove("Cookie");

        tables.put("Request Route Parameters", setToLinkedHashMap(ctx.request.routeParams(), k -> k, k -> ctx.request.routeParam(k).stringOption().or("-")));
        
        tables.put("Request Query Parameters", setToLinkedHashMap(ctx.request.queryParams(), k -> k, k -> ctx.request.queryParam(k).stringOption().or("-")));
        
        tables.put("Request Properties", setToLinkedHashMap(ctx.request.properties(), k -> k, k -> ctx.request.property(k).stringOption().or("-")));
        
        tables.put("Request Cookies", setToLinkedHashMap(ctx.request.cookies(), k -> k, k -> ctx.request.cookie(k).stringOption().or("-")));
        
        try {
          tables.put("Session", ctx.session().asMap());
          tables.put("Auth", 
              ctx.auth().isLoggedIn() ? 
                  ImmutableMap.of("user", ctx.user().getId() + ":" + ctx.user().getUserName()) : 
                  ImmutableMap.of());
        } catch (Exception e) {
          tables.put("Session", ImmutableMap.of());
          tables.put("Auth", ImmutableMap.of());
        }

        tables.put("Environment", getEnvironmentInfo(ctx));
    }

    private LinkedHashMap<String, Object> getHandlerInfo(HandlerContext ctx, Match<Method> match) {
      LinkedHashMap<String, Object> data = new LinkedHashMap<>();
      data.put("Controller", (match.getData() != null ? match.getData().getDeclaringClass().getCanonicalName() : "N/A"));
      data.put("Method", (match.getData() != null ? match.getData().getName() : "N/A"));
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
        return set.stream().collect(Collectors.toMap(keyMapper, valueMapper, (k, v) -> k, LinkedHashMap::new));
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
                frame.put("code_start", Iterables.reduce(codeLines.get().keySet(), Integer.MAX_VALUE, Math::min) + 1);
    
                // Write the code as a single string, replacing empty lines with a " ".
                frame.put("code", Joiner.on("\n").join(
                        Iterables.map(codeLines.get().values(), (x) -> x.length() == 0 ? " " : x))
                );
    
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
