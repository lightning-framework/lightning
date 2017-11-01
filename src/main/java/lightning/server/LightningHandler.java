package lightning.server;

import static lightning.util.ReflectionUtil.annotations;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.MappedByteBufferPool;
import org.eclipse.jetty.io.RuntimeIOException;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.MultiPartInputStreamParser;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustl.pathtravelagent.PathFormatException;
import com.google.common.collect.ImmutableSet;

import lightning.ann.Before;
import lightning.ann.ExceptionHandler;
import lightning.ann.Json;
import lightning.ann.JsonInput;
import lightning.ann.Multipart;
import lightning.ann.RequireAuth;
import lightning.ann.RequireXsrfToken;
import lightning.ann.Route;
import lightning.ann.Template;
import lightning.ann.WebSocket;
import lightning.cache.Cache;
import lightning.cache.CacheDriver;
import lightning.cache.driver.ExceptingCacheDriver;
import lightning.config.Config;
import lightning.db.MySQLDatabaseProvider;
import lightning.debugmap.DebugMapController;
import lightning.debugscreen.DebugScreen;
import lightning.debugscreen.LocalSourceLocator;
import lightning.debugscreen.SourceLocator;
import lightning.enums.HTTPMethod;
import lightning.enums.HTTPStatus;
import lightning.exceptions.LightningConfigException;
import lightning.exceptions.LightningException;
import lightning.exceptions.LightningValidationException;
import lightning.healthscreen.HealthScreenController;
import lightning.http.AccessViolationException;
import lightning.http.BadRequestException;
import lightning.http.HaltException;
import lightning.http.InternalRequest;
import lightning.http.InternalResponse;
import lightning.http.InternalServerErrorException;
import lightning.http.MethodNotAllowedException;
import lightning.http.NotAuthorizedException;
import lightning.http.NotFoundException;
import lightning.http.NotImplementedException;
import lightning.inject.Injector;
import lightning.inject.InjectorModule;
import lightning.io.BufferingHttpServletResponse;
import lightning.io.FileServer;
import lightning.json.GsonJsonService;
import lightning.json.JsonService;
import lightning.mail.Mailer;
import lightning.mvc.DefaultExceptionViewProducer;
import lightning.mvc.HandlerContext;
import lightning.mvc.ModelAndView;
import lightning.routing.ExceptionMapper;
import lightning.routing.FilterMapper;
import lightning.routing.FilterMapper.FilterMatch;
import lightning.routing.RouteMapper;
import lightning.routing.RouteMapper.Match;
import lightning.scanner.ScanResult;
import lightning.scanner.Scanner;
import lightning.templates.FreeMarkerTemplateEngine;
import lightning.templates.TemplateEngine;
import lightning.util.DebugUtil;
import lightning.util.MimeMatcher;
import lightning.websockets.WebSocketHolder;

public final class LightningHandler extends AbstractHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(LightningHandler.class);
  private static final ImmutableSet<Class<? extends Throwable>> INTERNAL_EXCEPTIONS = ImmutableSet.of(
      NotFoundException.class,
      BadRequestException.class,
      NotAuthorizedException.class,
      AccessViolationException.class,
      InternalServerErrorException.class,
      MethodNotAllowedException.class,
      NotImplementedException.class);

  private final Scanner scanner;
  private final Mailer mailer;
  private final Config config;
  private final MySQLDatabaseProvider dbProvider;
  private final TemplateEngine userTemplateEngine;
  private final TemplateEngine internalTemplateEngine;
  private final ExceptionMapper<Method> exceptionHandlers;
  private final InjectorModule userInjectorModule;
  private final InjectorModule globalInjectorModule;
  private final FileServer fileServer;
  private final RouteMapper<Object> routes;
  private final FilterMapper<Method> filters;
  private final JsonService jsonService;
  private final Cache cache;
  private final DebugScreen debugScreen;
  private final DefaultExceptionViewProducer exceptionViews;
  private final MimeMatcher outputBufferMatcher;

  private WebSocketServerFactory webSocketFactory;
  private ScanResult scanResult;

  public LightningHandler(Config config,
                          MySQLDatabaseProvider dbProvider,
                          InjectorModule userInjectorModule) throws Exception {
    this.config = config;
    this.dbProvider = dbProvider;
    this.userInjectorModule = userInjectorModule;
    this.globalInjectorModule = new InjectorModule();
    this.scanResult = null;
    this.filters = new FilterMapper<>();
    this.exceptionHandlers = new ExceptionMapper<>();
    this.scanner = new Scanner(config.autoReloadPrefixes,
                               config.scanPrefixes,
                               config.enableDebugMode && !config.isRunningFromJAR(),
                               new Injector(this.userInjectorModule,
                                            this.globalInjectorModule),
                               config.resolveProjectPath("target/classes"));
    this.routes = new RouteMapper<>();
    this.internalTemplateEngine = new FreeMarkerTemplateEngine(getClass(), "/lightning");
    this.exceptionViews = new DefaultExceptionViewProducer();

    // Set up user template engine.
    {
      TemplateEngine engine = userInjectorModule.getBindingForClass(TemplateEngine.class);
      this.userTemplateEngine = (engine != null) ? engine : new FreeMarkerTemplateEngine(config);
    }

    // Set up output buffering.
    {
      if (config.server.enableOutputBuffering) {
        this.outputBufferMatcher = new MimeMatcher(config.server.outputBufferingTypes);
      } else {
        this.outputBufferMatcher = null;
      }
    }

    // Set up user json service.
    {
      JsonService service = userInjectorModule.getBindingForClass(JsonService.class);
      this.jsonService = (service != null) ? service : new GsonJsonService();
    }

    // Set up cache driver.
    {
      CacheDriver driver = userInjectorModule.getBindingForClass(CacheDriver.class);
      this.cache = new Cache((driver != null) ? driver : new ExceptingCacheDriver());
    }

    // Set up debug screen.
    {
      SourceLocator[] locators = new SourceLocator[config.codeSearchPaths.size()];
      int i = 0;
      for (String path : config.codeSearchPaths) {
        locators[i++] = new LocalSourceLocator(config.resolveProjectPath(path));
      }
      this.debugScreen = new DebugScreen(config, locators);
    }

    // Set up mail.
    if (config.mail.isEnabled()) {
      this.mailer = new Mailer(config.mail);
    } else {
      this.mailer = null;
    }

    // Set up static files.
    {
      if (config.server.staticFilesPath != null) {
        ResourceFactory factory = (config.enableDebugMode && !config.isRunningFromJAR())
            ? new ResourceCollection(getStaticFileResourcePaths())
            : Resource.newClassPathResource("/" + config.server.staticFilesPath);
        // NOTE: We disable MMAP in debug mode because MMAP will lock files preventing people from
        // making changes to them.
        if (factory == null) {
          throw new LightningException("Your configured staticFilesPath does not exist within the classpath.");
        }
        this.fileServer = new FileServer(factory, !config.enableDebugMode);
        this.fileServer.setMaxCachedFiles(config.server.maxCachedStaticFiles);
        this.fileServer.setMaxCachedFileSize(config.server.maxCachedStaticFileSizeBytes);
        this.fileServer.setMaxCacheSize(config.server.maxStaticFileCacheSizeBytes);

        if (config.enableDebugMode) {
          this.fileServer.disableCaching();
        }
      } else {
        this.fileServer = null;
      }
    }

    // Set up the global injection module.
    {
      this.globalInjectorModule.bindClassToInstance(LightningHandler.class, this);
      this.globalInjectorModule.bindClassToInstance(Config.class, this.config);
      this.globalInjectorModule.bindClassToInstance(MySQLDatabaseProvider.class, this.dbProvider);
      this.globalInjectorModule.bindClassToInstance(Mailer.class, this.mailer);
      this.globalInjectorModule.bindClassToInstance(TemplateEngine.class, this.userTemplateEngine);
      this.globalInjectorModule.bindClassToInstance(Cache.class, this.cache);
      this.globalInjectorModule.bindClassToInstance(JsonService.class, this.jsonService);
    }

    try {
      rescan();
    } catch (Throwable t) {
      if (config.enableDebugMode && !config.isRunningFromJAR()) {
        LOGGER.error("Lightning has failed to process your routing configuration. The server will start (in debug mode), but you must correct these errors.", t);
      } else {
        throw new LightningConfigException("Lightning has failed to process your routing configuration. You must correct these errors to start the server.", t);
      }
    }

    if (config.debugRouteMapPath != null && config.enableDebugMode) {
      LOGGER.info("A route overview is available at {}://localhost:{}{}",
                  config.ssl.isEnabled() ? "https" : "http",
                  config.server.port,
                  config.debugRouteMapPath);
    }
  }

  private boolean isIgnorableException(Throwable e) {
    return (e instanceof IOException) || (e instanceof RuntimeIOException);
  }

  private Resource[] getStaticFileResourcePaths() {
    assert (config.server.staticFilesPath != null && config.enableDebugMode);
    ArrayList<Resource> resources = new ArrayList<>();
    List<File> possible = new ArrayList<>();

    if (Paths.get(config.server.staticFilesPath).isAbsolute()) {
      possible.add(new File(config.server.staticFilesPath));
    }
    else {
      possible.add(new File(config.resolveProjectPath("src/main/resources", config.server.staticFilesPath)));
      possible.add(new File(config.resolveProjectPath("src/main/java", config.server.staticFilesPath)));
    }

    for (File f : possible) {
      if (f.exists() && f.isDirectory() && f.canRead()) {
        resources.add(Resource.newResource(f));
      }
    }

    return resources.toArray(new Resource[resources.size()]);
  }

  public ScanResult getLastScanResult() {
    // For use in debug map page.
    return scanResult;
  }

  public Match<Object> getRouteMatch(String path, HTTPMethod method) throws PathFormatException {
    // For use in debug map page.
    return routes.lookup(path, method);
  }

  public FilterMatch<Method> getFilterMatch(String path, HTTPMethod method) throws PathFormatException {
    // For use in debug map page.
    return filters.lookup(path, method);
  }

  @Override
  public void destroy() {
   // TODO: Probably a few other things that need to be cleaned up.
   fileServer.destroy();
   super.destroy();
  }

  @Override
  protected void doStart() throws Exception {
    // Set up web socket factory.
    {
      WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
      policy.setIdleTimeout(config.server.websocketTimeoutMs);
      policy.setMaxBinaryMessageSize(config.server.websocketMaxBinaryMessageSizeBytes);
      policy.setMaxTextMessageSize(config.server.websocketMaxTextMessageSizeBytes);
      policy.setAsyncWriteTimeout(config.server.websocketAsyncWriteTimeoutMs);
      policy.setInputBufferSize(config.server.inputBufferSizeBytes);

      Constructor<WebSocketServerFactory> c =
          WebSocketServerFactory.class.getDeclaredConstructor(
              WebSocketPolicy.class, Executor.class, ByteBufferPool.class);
      c.setAccessible(true);
      webSocketFactory = c.newInstance(policy, getServer().getThreadPool(), new MappedByteBufferPool());

      if(!config.server.websocketEnableCompression) {
        this.webSocketFactory.getExtensionFactory().unregister("permessage-deflate");
        this.webSocketFactory.getExtensionFactory().unregister("deflate-frame");
        this.webSocketFactory.getExtensionFactory().unregister("x-webkit-deflate-frame");
      }
    }

    addBean(webSocketFactory);
    super.doStart();
  }

  public void sendErrorPage(HttpServletRequest request,
                             HttpServletResponse response,
                             Throwable error,
                             Match<Object> route) throws ServletException, IOException {
    if (response.isCommitted() && !isIgnorableException(error)) {
      LOGGER.warn("Failed to render an error page (response already committed).");
      return; // We can't render an error page if the response is committed.
    }

    Method exceptionHandler = exceptionHandlers.get(error);

    if (exceptionHandler == null) {
      sendBuiltInErrorPage(request, response, error, route);
      return;
    }

    logRequest(request, "@ExceptionHandler " + exceptionHandler);

    response.reset();
    response.addHeader("Content-Type", "text/html; charset=UTF-8");

    try {
      HandlerContext context = context(request, response);
      context.bindings().bindToClass(error);
      exceptionHandler.invoke(null, context.injector().getInjectedArguments(exceptionHandler));
    } catch (Throwable exceptionHandlerError) {
      if ((exceptionHandlerError instanceof InvocationTargetException) &&
          exceptionHandlerError.getCause() != null) {
        exceptionHandlerError = exceptionHandlerError.getCause();
      }

      if (exceptionHandlerError != error) { // If the exception handler re-threw the exception we passed it.
        exceptionHandlerError.addSuppressed(error);
      }

      if (exceptionHandlerError != error) {
        /* Don't log on re-throw. */
        LOGGER.warn("An exception handler returned an exception:", exceptionHandlerError);
      }

      // If the user exception handler threw an exception (not unlikely), we can try to render
      // the built-in page instead.
      sendBuiltInErrorPage(request, response, exceptionHandlerError, route);
    }
  }

  public void sendBuiltInErrorPage(HttpServletRequest request,
                                   HttpServletResponse response,
                                   Throwable error,
                                   Match<Object> route) throws ServletException, IOException {
    try {
      if (response.isCommitted() && !isIgnorableException(error)) {
        LOGGER.warn("Failed to render an error page (response already committed).");
        return; // We can't render an error page if the response is committed.
      }

      response.reset();
      response.addHeader("Content-Type", "text/html; charset=UTF-8");

      // For built-in exception types (e.g. 404 Not Found):
      ModelAndView view = exceptionViews.produce(error.getClass(), error, request, response);
      if (view != null) {
        response.setStatus(HTTPStatus.fromException(error).getCode());
        internalTemplateEngine.render(view.viewName, view.viewModel, response.getWriter());
        return;
      }

      logRequest(request, "@ExceptionHandler lightning.server.DefaultExceptionHandler");

      // For all other exception types:
      if (config.enableDebugMode) {
        // Show the debug screen in debug mode.
        debugScreen.handle(error, context(request, response), route);
      } else {
        // Otherwise show a generic internal server error page.
        response.setStatus(HTTPStatus.INTERNAL_SERVER_ERROR.getCode());
        view = exceptionViews.produce(InternalServerErrorException.class, error, request, response);
        internalTemplateEngine.render(view.viewName, view.viewModel, response.getWriter());
      }
    } catch (Throwable e2) {
      // This should be very rare.
      response.setStatus(HTTPStatus.INTERNAL_SERVER_ERROR.getCode());
      response.getWriter().println("500 INTERNAL SERVER ERROR");
    }
  }

  private void logRequest(HttpServletRequest request, String target) {
    if (config.enableDebugMode) {
      LOGGER.info("REQUEST ({}): {} {} -> {}",
          request.getRemoteAddr(),
          request.getMethod().toUpperCase(),
          request.getPathInfo(),
          target);
    }
  }

  private boolean acceptWebSocket(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Match<Object> route) throws IOException {
    if (!(route.getData() instanceof WebSocketHolder)) {
      return false;
    }

    WebSocketHolder wrapper = (WebSocketHolder)route.getData();

    logRequest(request, "@WebSocket " + wrapper.getType().getCanonicalName());

    if (!webSocketFactory.isUpgradeRequest(request, response)) {
      throw new BadRequestException();
    }

    // The request is consumed regardless of what acceptWebSocket returns.
    // If it fails, the factory commits the response with an error code.
    webSocketFactory.acceptWebSocket(wrapper.getCreator(context(request, response)), request, response);
    return true;
  }

  private boolean sendStaticFile(HttpServletRequest request,
                                 HttpServletResponse response) throws ServletException, IOException {
    if (config.server.staticFilesPath != null &&
        request.getMethod().equalsIgnoreCase("GET") &&
        fileServer.couldConsume(request, response)) {
      logRequest(request, "lightning.server.StaticFileHandler");
      fileServer.handle(request, response);
      return true;
    }

    return false;
  }

  private boolean redirectInsecureRequest(HttpServletRequest request,
                                          HttpServletResponse response) throws URISyntaxException, IOException {
    if (!config.ssl.isEnabled() ||
        !config.ssl.redirectInsecureRequests ||
        !request.getScheme().equalsIgnoreCase("https")) {
      return false;
    }

    logRequest(request, "lightning.server.HttpToHttpsRedirect");
    URI httpUri = new URI(request.getRequestURL().toString());
    URI httpsUri = new URI("https",
                           httpUri.getUserInfo(),
                           httpUri.getHost(),
                           config.ssl.port,
                           httpUri.getPath(),
                           httpUri.getQuery(),
                           null);

    response.sendRedirect(httpsUri.toString());
    return true;
  }

  private void rescan() throws Exception {
    routes.clear();
    exceptionHandlers.clear();
    filters.clear();

    // Built-ins.
    DebugMapController.map(routes, config);
    HealthScreenController.map(routes, config);
    routes.compile();

    scanResult = scanner.scan();

    // Exception Handlers
    for (Class<?> clazz : scanResult.exceptionHandlers.keySet()) {
      for (Method method : scanResult.exceptionHandlers.get(clazz)) {
        for (ExceptionHandler eh : annotations(method, ExceptionHandler.class)) {
          try {
            exceptionHandlers.map(eh.value(), method);
          } catch (IllegalStateException e) {
            throw new LightningValidationException(method,
                "Duplicate @ExceptionHandler for type " + eh.value().getCanonicalName() +
                " (first discovered on " + exceptionHandlers.get(eh.value()) + ").");
          }
        }
      }
    }

    // Routes
    for (Class<?> clazz : scanResult.routes.keySet()) {
      for (Method method : scanResult.routes.get(clazz)) {
        for (Route route : annotations(method, Route.class)) {
          for (Template template : annotations(method, Template.class)) {
            if (template.value() != null && !template.value().isEmpty()) {
              try {
                userTemplateEngine.requireValid(template.value());
              } catch (Exception e) {
                throw DebugUtil.mockStackTrace(method,
                    new IllegalStateException(method + ": Refers to a @Template that does not exist.", e), true);
              }
            }
          }

          for (HTTPMethod httpMethod : route.methods()) {
            routes.map(httpMethod, route.path(), method);
          }
        }
      }
    }

    // TODO: Drop active web socket connections immediately when code changes (instead of on next event).
    // Web Sockets
    for (Class<?> clazz : scanResult.websockets) {
      @SuppressWarnings("unchecked")
      Class<? extends WebSocketHolder> ws = (Class<? extends WebSocketHolder>)clazz;
      WebSocket info = clazz.getAnnotation(WebSocket.class);
      routes.map(HTTPMethod.GET, info.path(), new WebSocketHolder(ws));
    }

    // Filters
    for (Class<?> clazz : scanResult.beforeFilters.keySet()) {
      for (Method m : scanResult.beforeFilters.get(clazz)) {
        for (Before info : annotations(m, Before.class)) {
          filters.addFilterBefore(info.path(), info.methods(), info.priority(), m);
        }
      }
    }

    try {
      routes.compile();
    } catch (RouteMapper.RouteFormatException e) {
      if (e.handler instanceof Method) {
        throw DebugUtil.mockStackTrace((Method)e.handler, e, true);
      }
      throw e;
    }
  }

  @Override
  public void handle(String target,
                     org.eclipse.jetty.server.Request baseRequest,
                     HttpServletRequest sRequest,
                     HttpServletResponse sResponse) throws IOException, ServletException {
    baseRequest.setHandled(true);

    if (sendStaticFile(sRequest, sResponse)) {
      return;
    }

    if (config.enableDebugMode) {
      // Serialize requests in debug mode to prevent concurrency issues due to rescans.
      // TODO: Find a more performant solution for larger projects.
      synchronized (this) {
        handleRequest(target, baseRequest, sRequest, sResponse);
      }
    } else {
      handleRequest(target, baseRequest, sRequest, sResponse);
    }
  }

  private void handleRequest(String target,
                             org.eclipse.jetty.server.Request baseRequest,
                             HttpServletRequest sRequest,
                             HttpServletResponse sResponse) throws IOException, ServletException {
    Match<Object> route = null;

    try {
      if (redirectInsecureRequest(sRequest, sResponse)) {
        return;
      }

      if (config.enableDebugMode && !config.isRunningFromJAR()) {
        try {
          rescan(); // Reloads all routes, exception handlers, filters, etc.
        } catch (Throwable t) {
          throw new LightningConfigException("Errors exist in your routing configuration - you must correct these to continue.", t);
        }
      }

      route = routes.lookup(sRequest);

      if (route != null) {
        if (acceptWebSocket(sRequest, sResponse, route)) {
          return;
        }

        if (config.server.enableOutputBuffering) {
          sResponse = new BufferingHttpServletResponse(sResponse, config, outputBufferMatcher);
        }

        if (processRoute(sRequest, sResponse, route)) {
          return;
        }
      }

      throw new NotFoundException();
    } catch (Throwable error) {
      if (!INTERNAL_EXCEPTIONS.contains(error.getClass()) && !isIgnorableException(error)) {
        LOGGER.warn("A request handler returned an exception: ", error);
      }

      sendErrorPage(sRequest, sResponse, error, route);
    } finally {
      HandlerContext context = (HandlerContext)sRequest.getAttribute(HandlerContext.ATTRIBUTE);

      if (context != null && !context.isAsync()) {
        context.close();
        sRequest.removeAttribute(HandlerContext.ATTRIBUTE);
      }

      MultiPartInputStreamParser multipartInputStream = (MultiPartInputStreamParser)sRequest.getAttribute(
          org.eclipse.jetty.server.Request.__MULTIPART_INPUT_STREAM);
      if (multipartInputStream != null) {
        if (!sRequest.isAsyncStarted()) {
          try {
            multipartInputStream.deleteParts();
          } catch (MultiException e){
            LOGGER.warn("Error cleaning multiparts:", e);
          }
        } else if (context == null || !context.isAsync()) {
          // If you get this error, it's because you invoked request().raw().startAsync() instead of goAsync on
          // lightning.server.Context.
          LOGGER.warn("Using servlet async with multipart request may not clean up properly - use Lightning's goAsync instead.");
        }
      }

      Context.clearContext();
    }
  }

  private void runControllerInitializers(HandlerContext context, Object controller) throws Throwable {
    assert (controller != null);

    Class<?> currentClass = controller.getClass();

    while (currentClass != null) {
      if (scanResult.initializers.containsKey(currentClass)) {
        for (Method i : scanResult.initializers.get(currentClass)) {
          i.invoke(controller, context.injector().getInjectedArguments(i));
        }
      }

      currentClass = currentClass.getSuperclass();
    }
  }

  private void runControllerFinalizers(HandlerContext context, Object controller) {
    assert (controller != null);
    Class<?> currentClass = controller.getClass();

    while (currentClass != null) {
      if (scanResult.finalizers.containsKey(currentClass)) {
        for (Method i : scanResult.finalizers.get(currentClass)) {
          try {
            i.invoke(controller, context.injector().getInjectedArguments(i));
          } catch (Throwable e) {
            LOGGER.error("An error occured executing a finalizer {}: {}", i, e);
          }
        }
      }

      currentClass = currentClass.getSuperclass();
    }
  }

  private void processControllerOutput(HandlerContext context,
                                       Method target,
                                       Object output) throws Throwable {
    assert (output != null);

    try {
      Json json = target.getAnnotation(Json.class);
      if (json != null) {
       context.sendJson(output, json.prefix(), json.names());
       return;
      }

      if (output instanceof ModelAndView) {
        context.render((ModelAndView)output);
        return;
      }

      Template template = target.getAnnotation(Template.class);
      if (template != null) {
        if (template.value() != null && !template.value().isEmpty()) {
          context.render(template.value(), output);
          return;
        }

        throw new LightningException("Improper use of @Template annotation - refer to documentation.");
      }

      if (output instanceof File) {
        context.sendFile((File)output);
        return;
      }

      if (output instanceof String) {
        context.response.write((String)output);
        return;
      }

      throw new LightningException("Unable to process return value of @Route - refer to documentation.");
    } catch (Throwable e) {
      if (config.enableDebugMode) {
        /* We can mock a stack trace for the target method so that the debug screen can show the code. */
        throw DebugUtil.mockStackTrace(target, e, false);
      }

      throw e;
    }
  }

  private void processBeforeFilters(HandlerContext context) throws Throwable {
    FilterMatch<Method> filters = this.filters.lookup(context.request.path(),
                                                      context.request.method());

    for (lightning.routing.FilterMapper.Filter<Method> filter : filters.beforeFilters()) {
      ((InternalRequest)context.request).setWildcards(filter.wildcards(context.request.path()));
      ((InternalRequest)context.request).setParams(filter.params(context.request.path()));
      filter.handler.invoke(null, context.injector().getInjectedArguments(filter.handler));
    }
  }

  private boolean processRoute(HttpServletRequest request,
                               HttpServletResponse response,
                               Match<Object> route) throws Throwable {
    if (!(route.getData() instanceof Method)) {
      return false;
    }

    Method target = (Method)route.getData();
    Object controller = null;
    HandlerContext context = context(request, response);
    logRequest(request, "@Route " + target.toString());

    if (context.request.isMultipart()) {
      if (!config.server.multipartEnabled) {
        throw new BadRequestException("Multipart requests are disallowed.");
      }

      request.setAttribute(
          org.eclipse.jetty.server.Request.__MULTIPART_CONFIG_ELEMENT,
          new MultipartConfigElement(
              config.server.multipartSaveLocation,
              config.server.multipartPieceLimitBytes,
              config.server.multipartRequestLimitBytes,
              config.server.multipartPieceLimitBytes));
    }

    try {
      try {
        // Set the default content type for all route targets.
        response.setContentType("text/html; charset=UTF-8");

        // Execute @Before filters.
        processBeforeFilters(context);

        ((InternalRequest)context.request).setWildcards(route.getWildcards());
        ((InternalRequest)context.request).setParams(route.getParams());

        // Perform pre-processing.
        if (target.getAnnotation(Multipart.class) != null) {
          context.requireMultipart();
        }

        if (target.getAnnotation(RequireAuth.class) != null) {
          context.requireAuth();
        }

        {
          RequireXsrfToken info = target.getAnnotation(RequireXsrfToken.class);
          if (info != null) {
            context.requireXsrf(info.inputName());
          }
        }

        {
          JsonInput info = target.getAnnotation(JsonInput.class);
          if (info != null) {
            Object value = context.parseJson(info.type(), info.names());
            context.badRequestIf(value == null,
                context.isDebug()
                  ? "Failed to parse JSON '" + info.type().getCanonicalName() + "' from request body."
                  : "Failed to parse JSON from request body.");
            context.bindings().bindClassToInstanceUnsafe(info.type(), value);
          }
        }

        // Instantiate the controller.
        controller = context.injector().newInstance(target.getDeclaringClass());

        // Run @Initializers.
        runControllerInitializers(context, controller);

        // Execute the @Route.
        Object output = target.invoke(controller, context.injector().getInjectedArguments(target));

        // Try to save the session here if we need to since post-processing may commit the response.
        context.maybeSaveSession();

        // Perform post-processing.
        if (output != null) {
          processControllerOutput(context, target, output);
        }
      } catch (InvocationTargetException e) {
        // Leads to better error pages.
        // Also: Important so that exception handlers map correctly.
        if (e.getCause() != null) {
          throw e.getCause();
        }
        throw e;
      }
    } catch (HaltException e) {
      // A halt exception says to jump to here in the life cycle.
    } finally {
      // Run @Finalizers.
      if (controller != null) {
        runControllerFinalizers(context, controller);
      }
    }

    return true;
  }

  private HandlerContext context(HttpServletRequest request, HttpServletResponse response) {
    HandlerContext context = (HandlerContext)request.getAttribute(HandlerContext.ATTRIBUTE);

    if (context == null) {
      InternalRequest lRequest = InternalRequest.makeRequest(request, config.server.trustLoadBalancerHeaders);
      InternalResponse lResponse = InternalResponse.makeResponse(response);
      context = new HandlerContext(lRequest, lResponse, dbProvider, config, userTemplateEngine, fileServer,
                                   mailer, jsonService, cache, globalInjectorModule, userInjectorModule);
      lRequest.setCookieManager(context.cookies);
      lResponse.setCookieManager(context.cookies);
      request.setAttribute(HandlerContext.ATTRIBUTE, context);
      Context.setContext(context);
    }

    return context;
  }
}