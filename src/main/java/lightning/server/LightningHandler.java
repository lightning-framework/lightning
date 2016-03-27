package lightning.server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lightning.Lightning;
import lightning.ann.ExceptionHandler;
import lightning.ann.Json;
import lightning.ann.Multipart;
import lightning.ann.QParam;
import lightning.ann.RParam;
import lightning.ann.RequireAuth;
import lightning.ann.RequireXsrfToken;
import lightning.ann.Route;
import lightning.ann.Routes;
import lightning.ann.Template;
import lightning.ann.WebSocketFactory;
import lightning.config.Config;
import lightning.db.MySQLDatabaseProvider;
import lightning.debugscreen.DebugScreen;
import lightning.enums.HTTPMethod;
import lightning.enums.HTTPStatus;
import lightning.exceptions.LightningException;
import lightning.fn.ExceptionViewProducer;
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
import lightning.http.Request;
import lightning.http.Response;
import lightning.io.FileServer;
import lightning.json.JsonFactory;
import lightning.mvc.DefaultExceptionViewProducer;
import lightning.mvc.HandlerContext;
import lightning.mvc.ModelAndView;
import lightning.routing.ExceptionMapper;
import lightning.routing.RouteMapper;
import lightning.routing.RouteMapper.Match;
import lightning.scanner.ScanResult;
import lightning.scanner.Scanner;
import lightning.util.Iterables;

import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import freemarker.template.Configuration;
import freemarker.template.Version;

public class LightningHandler extends AbstractHandler {
  private static final Version FREEMARKER_VERSION = new Version(2, 3, 20);
  private static final Logger logger = LoggerFactory.getLogger(LightningHandler.class);
  
  private Config config;
  private MySQLDatabaseProvider dbp;
  private Configuration userTemplateConfig;
  private Configuration internalTemplateConfig;
  private ExceptionMapper exceptionHandlers;
  private Scanner scanner;
  private ResourceFactory staticFileResourceFactory;
  private ScanResult scanResult;
  private DebugScreen debugScreen;
  private ExceptionViewProducer exceptionViewProducer; 
  private FileServer staticFileServer;
  private RouteMapper<Method> routeMapper;
  
  public LightningHandler(Config config, MySQLDatabaseProvider dbp) throws Exception {
    this.config = config;
    this.dbp = dbp;
    this.debugScreen = new DebugScreen();
    this.internalTemplateConfig = new Configuration(FREEMARKER_VERSION);
    this.internalTemplateConfig.setClassForTemplateLoading(Lightning.class, "templates");
    this.userTemplateConfig = new Configuration(FREEMARKER_VERSION);
    this.userTemplateConfig.setDirectoryForTemplateLoading(Iterables.firstOr(Iterables.filter(ImmutableList.of(
        new File("./src/main/java/" + config.server.templateFilesPath),
        new File("./src/main/resources/" + config.server.templateFilesPath)
    ), f -> f.exists()), new File(config.server.templateFilesPath)));
    this.exceptionHandlers = new ExceptionMapper();
    this.scanner = new Scanner(config.autoReloadPrefixes, config.scanPrefixes);
    this.staticFileResourceFactory = config.enableDebugMode
        ? new ResourceCollection(getResourcePaths())
        : Resource.newClassPathResource(config.server.staticFilesPath);
    this.exceptionViewProducer = new DefaultExceptionViewProducer();
    this.staticFileServer = new FileServer(this.staticFileResourceFactory);
    this.staticFileServer.setMaxCachedFiles(config.server.maxCachedStaticFiles);
    this.staticFileServer.setMaxCachedFileSize(config.server.maxCachedStaticFileSizeBytes);
    this.staticFileServer.setMaxCacheSize(config.server.maxStaticFileCacheSizeBytes);
    if (config.enableDebugMode) {
      this.staticFileServer.disableCaching();
    }
    this.routeMapper = new RouteMapper<>();
    rescan();
  }
  
  private Resource[] getResourcePaths() {
    File[] files = new File[] {
        new File("./src/main/resources", config.server.staticFilesPath),
        new File("./src/main/java", config.server.staticFilesPath),
        new File(config.server.staticFilesPath)
    };
    
    ArrayList<Resource> resources = new ArrayList<>();
    
    for (File f : files) {
      if (f.exists() && f.isDirectory() && f.canRead()) {
        resources.add(Resource.newResource(f));
      }
    }
    
    return resources.toArray(new Resource[resources.size()]);
  }
  
  @SuppressWarnings("unchecked")
  private ImmutableSet<Class<? extends Throwable>> ignoredExceptions = ImmutableSet.<Class<? extends Throwable>>of(
      NotFoundException.class, BadRequestException.class, NotAuthorizedException.class,
      AccessViolationException.class, InternalServerErrorException.class, MethodNotAllowedException.class,
      NotImplementedException.class);


  @Override
  public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest _request,
      HttpServletResponse _response) throws IOException, ServletException {
    baseRequest.setHandled(true);
    InternalRequest request = InternalRequest.makeRequest(_request, config.server.trustLoadBalancerHeaders);
    InternalResponse response = InternalResponse.makeResponse(_response);
    HandlerContext ctx = null;    
    
    try {      
      // Redirect insecure requests.
      if (config.ssl.isEnabled() && 
          config.ssl.redirectInsecureRequests &&
          !_request.getScheme().toLowerCase().equals("https")) {
          URI oldUri = new URI(_request.getRequestURL().toString());
          URI newUri = new URI("https",
                             oldUri.getUserInfo(),
                             oldUri.getHost(),
                             config.ssl.port,
                             oldUri.getPath(),
                             oldUri.getQuery(),
                             null);

          _response.sendRedirect(newUri.toString());
          return;
      }
      
      // Try to pass through a static file (if any).
      if (staticFileServer.couldConsume(_request)) {
        staticFileServer.handle(_request, _response);
        return;
      }
      
      // Perform routing.
      ctx = new HandlerContext(request, response, dbp, config, userTemplateConfig, this.staticFileServer);
      Context.setContext(ctx);
      request.setCookieManager(ctx.cookies);
      response.setCookieManager(ctx.cookies);
      
      if (config.enableDebugMode) {
        rescan();
        userTemplateConfig.clearTemplateCache();
        internalTemplateConfig.clearTemplateCache();
      }
      
      // Try to execute a route handler (if any).
      Match<Method> match = routeMapper.lookup(request);
      if (match != null) {
        logger.debug("Found route match: data={} params={} wildcards={}", match.getData(), match.getParams(), match.getWildcards());
        
        // Mutate the request.
        request.setWildcards(match.getWildcards());
        request.setParams(match.getParams());
        
        try {
          // TODO: Not sure how bad the performance is going to be with reflection here.
          Method m = match.getData();
          
          if (m == null) {
            logger.debug("WebSocket Handler Detected, skip");
            baseRequest.setHandled(false);
            return;
          }
          
          // Perform pre-processing.          
          if (m.getAnnotation(Multipart.class) != null) {
            ctx.requireMultipart();
          }
          
          if (m.getAnnotation(RequireAuth.class) != null) {
            ctx.requireAuth();
          }
          
          if (m.getAnnotation(RequireXsrfToken.class) != null) {
            RequireXsrfToken info = m.getAnnotation(RequireXsrfToken.class);
            ctx.requireXsrf(info.inputName());
          }
          
          // Instantiate the controller.
          Class<?> clazz = m.getDeclaringClass();          
          Object controller = clazz.newInstance();
          
          // Run initializers.
          if (scanResult.initializers.containsKey(m.getDeclaringClass())) {
            for (Method i : scanResult.initializers.get(m.getDeclaringClass())) {
              i.invoke(controller);
            }
          }
          
          // Build invocation arguments.
          Object[] args = new Object[m.getParameterCount()];
          
          Parameter[] params = m.getParameters();
          for (int i = 0; i < params.length; i++) {
            if (params[i].getAnnotation(QParam.class) != null) {
              String name = params[i].getAnnotation(QParam.class).value();
              args[i] = request.queryParam(name).castTo(params[i].getType());
            } else if(params[i].getAnnotation(RParam.class) != null) {
              String name = params[i].getAnnotation(RParam.class).value();
              args[i] = request.routeParam(name).castTo(params[i].getType());
            } else {
              throw new LightningException("Cannot figure out how to inject arguments for route target " + m);
            }
          }
  
          // Invoke the controller.
          Object output = null;
          try {
            output = m.invoke(controller, args);
          } catch (InvocationTargetException e) {
            if (e.getCause() != null)
              throw e.getCause();
            throw e;
          }
          
          // Perform post-processing.
          if (output == null) {} // Assume the handler returned void or null because it did its work.
          else if (m.getAnnotation(Json.class) != null) {
            Json info = m.getAnnotation(Json.class);
            response.status(HTTPStatus.OK);
            response.type("application/json; charset=UTF-8");
            response.raw().getWriter().print(info.prefix());
            JsonFactory.newJsonParser(info.names()).toJson(output, response.raw().getWriter());
          } else if (m.getAnnotation(Template.class) != null) {
            if (output instanceof ModelAndView) {
              renderUserTemplate(response, (ModelAndView) output);
            }
            
            Template info = m.getAnnotation(Template.class);
            
            if (info.value() == null) {
              throw new LightningException("Unable to process output of handler: " + match.getData().toString());
            }
            
            renderUserTemplate(response, new ModelAndView(info.value(), output));
          } else if (output instanceof String) {
            response.raw().getWriter().print(output);
          } else if (output instanceof File) {
            ctx.sendFile((File) output);
          } else if (output instanceof ModelAndView) {
            renderUserTemplate(response, (ModelAndView) output);
          } else {
            throw new LightningException("Unable to process output of handler: " + match.getData().toString());
          }
        } catch (HaltException e) {} // Halt exception just says to jump to here.
        
        return;
      }
      
      // Trigger a 404 page.
      throw new NotFoundException();
    } catch (Throwable e) {
      if (!ignoredExceptions.contains(e.getClass())) {
        logger.warn("Request handler returned exception:", e);
      }
      
      try {
        Method handler = exceptionHandlers.getHandler(e);
        
        if (handler != null) {
          handler.invoke(null, ctx, e);
          return;
        }
        
        sendCriticalErrorPage(request, response, e);
      } catch (Throwable e2) {
        logger.warn("Exception handler returned exception:", e2);
        sendCriticalErrorPage(request, response, e);
      }
    } finally {
      try {
        if (ctx != null) {
          ctx.closeIfNotAsync();
        }
      } catch (Exception e) {
        logger.warn("Exception closing context:", e);
      }
      Context.clearContext();
    } 
  }
  
  // FINISHED
  
  protected synchronized void rescan() throws Exception {
    scanResult = scanner.scan();
    logger.debug("Scanned Annotations: {}", scanResult);
    
    // Map exception handlers.
    exceptionHandlers.clear();
    for (Class<?> clazz : scanResult.exceptionHandlers.keySet()) {
      for (Method method : scanResult.exceptionHandlers.get(clazz)) {
        ExceptionHandler annotation = method.getAnnotation(ExceptionHandler.class);
        logger.debug("Added Exception Handler: {} -> {}", annotation.value(), method);
        exceptionHandlers.map(annotation.value(), method);
      }
    }
    
    // Map routes.
    routeMapper.clear();
    
    for (Class<?> clazz : scanResult.routes.keySet()) {
      for (Method method : scanResult.routes.get(clazz)) {
        if (method.getAnnotation(Route.class) != null) {
          Route route = method.getAnnotation(Route.class);
          for (HTTPMethod httpMethod : route.methods()) {
            routeMapper.map(httpMethod, route.path(), method);
          }
        }
        
        if (method.getAnnotation(Routes.class) != null) {
          for (Route route : method.getAnnotation(Routes.class).value()) {
            for (HTTPMethod httpMethod : route.methods()) {
              routeMapper.map(httpMethod, route.path(), method);
            }
          }
        }
      }
    }
    
    // Map websockets to a null handler.
    for (Class<?> clazz : scanResult.websocketFactories.keySet()) {
      for (Method m : scanResult.websocketFactories.get(clazz)) {
        WebSocketFactory info = m.getAnnotation(WebSocketFactory.class);
        routeMapper.map(HTTPMethod.GET, info.path(), null);
      }
    }
    
    routeMapper.compile();
  }
  
  protected void sendCriticalErrorPage(Request request, Response response, Throwable e) throws IOException {
    try {
      ModelAndView mv = exceptionViewProducer.produce(e.getClass(), e, request.raw(), response.raw());
      if (mv != null) {
        response.status(HTTPStatus.fromException(e));
        renderInternalTemplate(response, mv);
        return;
      }
      
      if (config.enableDebugMode) {
        debugScreen.handle(e, request.raw(), response.raw());
        return;
      }
      
      response.status(HTTPStatus.INTERNAL_SERVER_ERROR);
      renderInternalTemplate(response, exceptionViewProducer.produce(
          InternalServerErrorException.class, e, request.raw(), response.raw()));
    } catch (Throwable e2) {
      logger.warn("Failed to render critical error page with exception: ", e2);
      response.status(HTTPStatus.NOT_IMPLEMENTED);
      response.raw().getWriter().println("500 INTERNAL SERVER ERROR - SEE LOGS!");
    }
  }
  
  protected void renderUserTemplate(Response response, ModelAndView modelAndView) throws Exception {
    renderUserTemplate(response, modelAndView.viewName, modelAndView.viewModel);
  }
  
  protected void renderInternalTemplate(Response response, ModelAndView modelAndView) throws Exception {
    renderInternalTemplate(response, modelAndView.viewName, modelAndView.viewModel);
  }
  
  protected void renderUserTemplate(Response response, String name, Object model) throws Exception {
    renderTemplate(response, userTemplateConfig, name, model);
  }
  
  protected void renderInternalTemplate(Response response, String name, Object model) throws Exception {
    renderTemplate(response, internalTemplateConfig, name, model);
  }
  
  protected void renderTemplate(Response response, Configuration tplConfig, String name, Object model) throws Exception {
    tplConfig.getTemplate(name).process(model, response.raw().getWriter());
  }
}
