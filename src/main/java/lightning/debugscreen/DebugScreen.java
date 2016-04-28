package lightning.debugscreen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lightning.util.Iterables;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.Version;

/**
 * Displays a stack trace in-browser to users.
 */
public class DebugScreen {
    protected final Configuration templateConfig;
    protected final SourceLocator[] sourceLocators;

    public DebugScreen() {
        this(
                new FileSearchSourceLocator("./src/main/java"),
                new FileSearchSourceLocator("./src/test/java")
        );
    }

    public DebugScreen(SourceLocator... sourceLocators) {
        templateConfig = new Configuration(new Version(2, 3, 23));
        templateConfig.setClassForTemplateLoading(getClass(), "/");
        this.sourceLocators = sourceLocators;
    }
    
    public final void handle(Throwable throwable, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(500); // Internal Server Error
        
        // Find the original causing throwable; this will contain the most relevant information to 
        // display to the user. 
        while (throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        
        try {
            List<Map<String, Object>> frames = parseFrames(throwable);
  
            LinkedHashMap<String, Object> model = new LinkedHashMap<>();
            model.put("short_message", Optional.fromNullable(StringUtils.abbreviate(throwable.getMessage(), 100)).or(""));
            model.put("full_trace", traceToString(throwable));
            model.put("message", Optional.fromNullable(throwable.getMessage()).or(""));
            model.put("plain_exception", ExceptionUtils.getStackTrace(throwable));
            model.put("frames", frames);
            model.put("name", throwable.getClass().getCanonicalName().split("\\."));
            model.put("basic_type", throwable.getClass().getSimpleName());
            model.put("type", throwable.getClass().getCanonicalName());
  
            LinkedHashMap<String, Map<String, ? extends Object>> tables = new LinkedHashMap<>();
            installTables(tables, request, response);
            model.put("tables", tables);
            response.setHeader("Content-Type", "text/html; charset=UTF-8");
            templateConfig.getTemplate("debugscreen.ftl").process(model, response.getWriter());
        } catch (Exception e) {
            // In case we encounter any exceptions trying to render the error page itself,
            // have this simple fallback.
            response.getWriter().println(
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
    protected void installTables(LinkedHashMap<String, Map<String, ? extends Object>> tables, HttpServletRequest request, HttpServletResponse response) {
        // TODO: Add more tables here.
        tables.put("Request", getRequestInfo(request));
        tables.put("Environment", getEnvironmentInfo());
        /*tables.put("Headers", setToLinkedHashMap(request.headers(), h -> h, request::headers));
        tables.get("Headers").remove("Cookie");
        tables.put("Route Parameters", request.params());
        tables.put("Query Parameters", setToLinkedHashMap(request.queryParams(), p -> p, request::queryParams));
        //tables.put("Session Attributes", setToLinkedHashMap(request.session().attributes(), a -> a, request.session()::attribute));
        tables.put("Request Attributes", setToLinkedHashMap(request.attributes(), a -> a, request::attribute));
        //tables.put("Cookies (Raw)", request.cookies());
        
        
        tables.put("Cookies", context.cookies.asMap());
        
        try {
          tables.put("Session", context.session.asMap());
          tables.put("Auth", context.auth.isLoggedIn() ? ImmutableMap.of("user", context.user().getId() + ":" + context.user().getUserName()) : ImmutableMap.of());
        } catch (SessionException | AuthException e) {
          tables.put("Session", ImmutableMap.of());
          tables.put("Auth", ImmutableMap.of());
        }*/
    }

    @SuppressWarnings("unused")
    private LinkedHashMap<String, String> setToLinkedHashMap(Set<String> set,
                                                             Function<String, String> keyMapper,
                                                             Function<String, String> valueMapper) {
        return set.stream().collect(Collectors.toMap(keyMapper, valueMapper, (k, v) -> k, LinkedHashMap::new));
    }

    private LinkedHashMap<String, Object> getEnvironmentInfo() {
        LinkedHashMap<String, Object> environment = new LinkedHashMap<>();
        environment.put("Thread ID", Thread.currentThread().getId());
        return environment;
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

    private LinkedHashMap<String, Object> getRequestInfo(HttpServletRequest request) {
        LinkedHashMap<String, Object> req = new LinkedHashMap<>();
        //req.put("URL", Optional.fromNullable(request.url()).or("-"));
        //req.put("Scheme", Optional.fromNullable(request.scheme()).or("-"));
        //req.put("Method", Optional.fromNullable(request.requestMethod()).or("-"));
        //req.put("Protocol", Optional.fromNullable(request.protocol()).or("-"));
        //req.put("Remote IP", Optional.fromNullable(request.ip()).or("-"));
        //req.put("Path Info", Optional.fromNullable(request.pathInfo()).or("-"));
        //req.put("Query String", Optional.fromNullable(request.queryString()).or("-"));
        //req.put("Host", Optional.fromNullable(request.host()).or("-"));
        //req.put("Port", Optional.fromNullable(Integer.toString(request.port())).or("-"));
        //req.put("URI", Optional.fromNullable(request.uri()).or("-"));
        //req.put("Content Type", Optional.fromNullable(request.contentType()).or("-"));
        //req.put("Content Length", request.contentLength() == -1 ? "-" : Integer.toString(request.contentLength()));
        //req.put("Context Path", Optional.fromNullable(request.contextPath()).or("-"));
        //req.put("Body", Optional.fromNullable(request.body()).or("-"));
        //req.put("User-Agent", Optional.fromNullable(request.userAgent()).or("-"));
        return req;
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
        Optional<File> file = Optional.absent();
        for (SourceLocator locator : sourceLocators) {
            file = locator.findFileForFrame(sframe);

            if (file.isPresent()) {
                break;
            }
        }

        // Fetch +-10 lines from the triggering line.
        Optional<Map<Integer, String>> codeLines = fetchFileLines(file, sframe);

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

        return frame.build();
    }

    /**
     * Fetches the lines of the source file corresponding to a StackTraceElement (fetches 20 lines total
     * centered on the line number given in the trace).
     *
     * @param file  An optional text file.
     * @param frame A stack trace frame.
     * @return An optional map of line numbers to the content of the lines (not terminated with \n).
     */
    private Optional<Map<Integer, String>> fetchFileLines(Optional<File> file, StackTraceElement frame) {
        // If no line number is given or no file exists, we can't fetch lines.
        if (!file.isPresent() || frame.getLineNumber() == -1) {
            return Optional.absent();
        }

        // Otherwise, fetch 20 lines centered on the number provided in the trace.
        ImmutableMap.Builder<Integer, String> lines = ImmutableMap.builder();
        int start = Math.max(frame.getLineNumber() - 10, 0);
        int end = start + 20;
        int current = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file.get()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (current < start) {
                    current += 1;
                    continue;
                }

                if (current > end) {
                    break;
                }

                lines.put(current, line);
                current += 1;
            }
        } catch (Exception e) {
            // If we get an IOException, not much we can do... just ignore it and move on.
            return Optional.absent();
        }

        return Optional.of(lines.build());
    }
}
