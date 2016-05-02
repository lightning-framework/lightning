package lightning.server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lightning.Lightning;
import lightning.ann.Before;
import lightning.ann.Befores;
import lightning.ann.ExceptionHandler;
import lightning.ann.Json;
import lightning.ann.Multipart;
import lightning.ann.RequireAuth;
import lightning.ann.RequireXsrfToken;
import lightning.ann.Route;
import lightning.ann.Routes;
import lightning.ann.Template;
import lightning.ann.WebSocketFactory;
import lightning.config.Config;
import lightning.db.MySQLDatabase;
import lightning.db.MySQLDatabaseProvider;
import lightning.debugscreen.DebugScreen;
import lightning.enums.HTTPHeader;
import lightning.enums.HTTPMethod;
import lightning.enums.HTTPStatus;
import lightning.exceptions.LightningException;
import lightning.fn.ExceptionViewProducer;
import lightning.fn.Filter;
import lightning.groups.Groups;
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
import lightning.inject.Injector;
import lightning.inject.InjectorModule;
import lightning.inject.Resolver;
import lightning.io.FileServer;
import lightning.json.JsonFactory;
import lightning.mail.Mailer;
import lightning.mvc.DefaultExceptionViewProducer;
import lightning.mvc.HandlerContext;
import lightning.mvc.ModelAndView;
import lightning.mvc.URLGenerator;
import lightning.mvc.Validator;
import lightning.routing.ExceptionMapper;
import lightning.routing.RouteMapper;
import lightning.routing.RouteMapper.Match;
import lightning.scanner.ScanResult;
import lightning.scanner.Scanner;
import lightning.sessions.Session;
import lightning.users.User;
import lightning.users.Users;
import lightning.util.Iterables;

import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.MultiPartInputStreamParser;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

/**
 * TODO: Output is not buffered - this can lead to some strange looking pages if an exception is thrown
 * after some output has already been sent (both in production and debug mode). May want to come up with
 * a better solution to this.
 */
public class LightningHandler extends AbstractHandler {
  private static final Version FREEMARKER_VERSION = new Version(2, 3, 20);
  private static final Logger logger = LoggerFactory.getLogger(LightningHandler.class);
  
  private Config config;
  private MySQLDatabaseProvider dbp;
  private Configuration userTemplateConfig;
  private Configuration internalTemplateConfig;
  private ExceptionMapper<Method> exceptionHandlers;
  private Scanner scanner;
  private ResourceFactory staticFileResourceFactory;
  private ScanResult scanResult;
  private DebugScreen debugScreen;
  private ExceptionViewProducer exceptionViewProducer; 
  private FileServer staticFileServer;
  private RouteMapper<Method> routeMapper;
  private InjectorModule globalModule;
  private InjectorModule userModule;
  private Mailer mailer;
  
  public LightningHandler(Config config, MySQLDatabaseProvider dbp, InjectorModule globalModule, InjectorModule userModule) throws Exception {
    this.config = config;
    this.dbp = dbp;
    this.globalModule = globalModule;
    this.userModule = userModule;
    this.debugScreen = new DebugScreen();
    this.internalTemplateConfig = new Configuration(FREEMARKER_VERSION);
    this.internalTemplateConfig.setClassForTemplateLoading(Lightning.class, "templates");
    this.internalTemplateConfig.setShowErrorTips(config.enableDebugMode);
    this.internalTemplateConfig.setTemplateExceptionHandler(/*config.enableDebugMode ?
        TemplateExceptionHandler.HTML_DEBUG_HANDLER :*/
        TemplateExceptionHandler.RETHROW_HANDLER);
    
    this.userTemplateConfig = new Configuration(FREEMARKER_VERSION);
    this.userTemplateConfig.setSharedVariable("__LIGHTNING_DEV", config.enableDebugMode);
    File templatePath = Iterables.firstOr(Iterables.filter(ImmutableList.of(
        new File("./src/main/java/" + config.server.templateFilesPath),
        new File("./src/main/resources/" + config.server.templateFilesPath)
    ), f -> f.exists()), new File(config.server.templateFilesPath));
    if (templatePath.exists()) {
      this.userTemplateConfig.setDirectoryForTemplateLoading(templatePath);
    } else {
      this.userTemplateConfig.setClassForTemplateLoading(getClass(), "/" + config.server.templateFilesPath);
    }
    this.userTemplateConfig.setShowErrorTips(config.enableDebugMode);
    this.userTemplateConfig.setTemplateExceptionHandler(/*config.enableDebugMode ?
        TemplateExceptionHandler.HTML_DEBUG_HANDLER :*/
        TemplateExceptionHandler.RETHROW_HANDLER);    
    
    this.exceptionHandlers = new ExceptionMapper<>();
    this.scanner = new Scanner(config.autoReloadPrefixes, config.scanPrefixes, config.enableDebugMode);
    if (config.server.staticFilesPath != null) {
      this.staticFileResourceFactory = config.enableDebugMode
          ? new ResourceCollection(getResourcePaths())
          : Resource.newClassPathResource(config.server.staticFilesPath);
    } else {
      this.staticFileResourceFactory = new ResourceFactory() {
        @Override
        public Resource getResource(String path) {
          return null;
        }
      };
    }
    this.exceptionViewProducer = new DefaultExceptionViewProducer();
    this.staticFileServer = new FileServer(this.staticFileResourceFactory);
    this.staticFileServer.setMaxCachedFiles(config.server.maxCachedStaticFiles);
    this.staticFileServer.setMaxCachedFileSize(config.server.maxCachedStaticFileSizeBytes);
    this.staticFileServer.setMaxCacheSize(config.server.maxStaticFileCacheSizeBytes);
    this.staticFileServer.usePublicCaching();
    if (config.enableDebugMode) {
      this.staticFileServer.disableCaching();
    }
    this.routeMapper = new RouteMapper<>();
    
    if (config.mail.isEnabled()) {
      this.mailer = new Mailer(config.mail);
      globalModule.bindClassToInstance(Mailer.class, this.mailer);
    }
    
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
    Injector injector = null;
    InjectorModule requestModule = null;
        
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
      if (request.method() == HTTPMethod.GET && staticFileServer.couldConsume(_request)) {
        staticFileServer.handle(_request, _response);
        return;
      }      
      
      // Create context.
      ctx = new HandlerContext(request, response, dbp, config, userTemplateConfig, this.staticFileServer, this.mailer);
      requestModule = requestSpecificInjectionModule(ctx);
      injector = new Injector(
          globalModule, requestModule, userModule);
      Context.setContext(ctx);
      request.setCookieManager(ctx.cookies);
      response.setCookieManager(ctx.cookies);
      
      // Enable multi-part support.
      if (request.isMultipart()) {
        if (!config.server.multipartEnabled) {
          throw new BadRequestException("Multipart requests are disallowed.");
        }
        
        request.raw().setAttribute(org.eclipse.jetty.server.Request.__MULTIPART_CONFIG_ELEMENT,
          new MultipartConfigElement(
              config.server.multipartSaveLocation, 
              config.server.multipartPieceLimitBytes, 
              config.server.multipartRequestLimitBytes, 
              config.server.multipartPieceLimitBytes));
      }
      
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
        
        Object controller = null;
        Method m = match.getData();
        
        try {
          // TODO: Not sure how bad the performance is going to be with reflection here.
          
          if (m == null) {
            // Null indicates a web socket handler is installed at this path.
            // Set the request as not handled and return so that Jetty will invoke the next
            // handler in the chain - the web socket handler.
            logger.debug("WebSocket Handler Detected, skip");
            baseRequest.setHandled(false);
            if (config.enableDebugMode) {
              logger.info("INCOMING REQUEST ({}): {} {} -> WEBSOCKET", request.ip(), request.method(), request.path());
            }
            return;
          }
          
          if (config.enableDebugMode) {
            logger.info("INCOMING REQUEST ({}): {} {} -> {}", request.ip(), request.method(), request.path(), m);
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
          
          if (m.getAnnotation(Befores.class) != null) {
            for (Before filter : m.getAnnotation(Befores.class).value()) {
              Filter instance = (Filter) injector.newInstance(filter.value());
              instance.execute();
            }
          } else if (m.getAnnotation(Before.class) != null) {
            Filter instance = (Filter) injector.newInstance(m.getAnnotation(Before.class).value());
            instance.execute();
          }
          
          // Instantiate the controller.
          Class<?> clazz = m.getDeclaringClass();          
          controller = injector.newInstance(clazz);
          
          // Run initializers.
          Class<?> currentClass = m.getDeclaringClass();
          
          while (currentClass != null) {
            if (scanResult.initializers.containsKey(currentClass)) {
              for (Method i : scanResult.initializers.get(currentClass)) {
                i.invoke(controller, injector.getInjectedArguments(i));
              }
            }
            
            currentClass = currentClass.getSuperclass();
          }
          
          // Build invocation arguments.
          Object[] args = injector.getInjectedArguments(m);
  
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
              ctx.render((ModelAndView) output);
            }
            
            Template info = m.getAnnotation(Template.class);
            
            if (info.value() == null) {
              throw new LightningException("Unable to process output of handler: " + match.getData().toString());
            }
            
            ctx.render(new ModelAndView(info.value(), output));
          } else if (output instanceof String) {
            if (response.raw().getHeader(HTTPHeader.CONTENT_TYPE.getHeaderName()) == null) {
              response.header(HTTPHeader.CONTENT_TYPE, "text/html; charset=UTF-8");
            }
            
            response.raw().getWriter().print(output);
          } else if (output instanceof File) {
            ctx.sendFile((File) output);
          } else if (output instanceof ModelAndView) {
            ctx.render((ModelAndView) output);
          } else {
            throw new LightningException("Unable to process output of handler: " + match.getData().toString());
          }
        } catch (HaltException e) {
          // Halt exception just says to jump to here.
        } finally {
          // Run any finalizers.
          if (m != null && controller != null) {
            Class<?> currentClass = m.getDeclaringClass();
            
            while (currentClass != null) {
              if (scanResult.finalizers.containsKey(currentClass)) {
                for (Method i : scanResult.finalizers.get(currentClass)) {
                  try {
                    i.invoke(controller, injector.getInjectedArguments(i));
                  } catch (Throwable e) {
                    logger.error("An error occured executing a finalizer {}: {}", i, e);
                  }
                }
              }
              
              currentClass = currentClass.getSuperclass();
            }
          }
        }
        
        return;
      }
      
      // Trigger a 404 page.
      if (config.enableDebugMode) {
        logger.info("INCOMING REQUEST ({}): {} {} -> NOT FOUND", request.ip(), request.method(), request.path());
      }
      
      throw new NotFoundException();
    } catch (Throwable e) {
      if (!ignoredExceptions.contains(e.getClass())) {
        logger.warn("Request handler returned exception:", e);
      }
      
      try {
        Method handler = exceptionHandlers.getHandler(e);
        requestModule.bindToClass(e);
        if (handler != null) {
          handler.invoke(null, injector.getInjectedArguments(handler));
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
          logger.debug("Closing Context");
          ctx.closeIfNotAsync();
        }
      } catch (Exception e) {
        logger.warn("Exception closing context:", e);
      }
      Context.clearContext();
      
      if (request.isMultipart() && config.server.multipartEnabled) {
        MultiPartInputStreamParser multipartInputStream = (MultiPartInputStreamParser) 
            request.raw().getAttribute(org.eclipse.jetty.server.Request.__MULTIPART_INPUT_STREAM);
        if (multipartInputStream != null) {
          try {
            multipartInputStream.deleteParts();
          } catch (MultiException e) {
            logger.warn("Error cleaning multiparts:", e);
          }
        }
      }
    } 
  }
    
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
        routeMapper.map(HTTPMethod.GET, info.path(), null); // Indicate to pass to next handler in chain.
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
      // This should basically never happen, but just in case everything that can go wrong goes wrong.
      logger.warn("Failed to render critical error page with exception: ", e2);
      response.status(HTTPStatus.INTERNAL_SERVER_ERROR);
      response.raw().getWriter().println("500 INTERNAL SERVER ERROR");
    }
  }
  
  protected void renderInternalTemplate(Response response, ModelAndView modelAndView) throws Exception {
    renderInternalTemplate(response, modelAndView.viewName, modelAndView.viewModel);
  }
  
  protected void renderInternalTemplate(Response response, String name, Object model) throws Exception {
    renderTemplate(response, internalTemplateConfig, name, model);
  }
  
  protected void renderTemplate(Response response, Configuration tplConfig, String name, Object model) throws Exception {
    response.header(HTTPHeader.CONTENT_TYPE, "text/html; charset=UTF-8");
    tplConfig.getTemplate(name).process(model, response.raw().getWriter());
  }
  
  protected InjectorModule requestSpecificInjectionModule(HandlerContext context) {
    InjectorModule m = new InjectorModule();
    m.bindClassToInstance(Validator.class, context.validator);
    m.bindClassToInstance(HandlerContext.class, context);
    m.bindClassToInstance(Session.class, context.session);
    m.bindClassToInstance(Request.class, context.request);
    m.bindClassToInstance(Response.class, context.response);
    m.bindClassToInstance(HttpServletRequest.class, context.request.raw());
    m.bindClassToInstance(HttpServletResponse.class, context.response.raw());
    m.bindClassToInstance(URLGenerator.class, context.url);
    m.bindClassToInstance(Groups.class, context.groups());
    m.bindClassToInstance(Users.class, context.users());
    m.bindClassToResolver(User.class, new Resolver<User>() {
      @Override
      public User resolve() throws Exception {
        return context.user();
      }
    });
    m.bindClassToResolver(MySQLDatabase.class, new Resolver<MySQLDatabase>() {
      @Override
      public MySQLDatabase resolve() throws Exception {
        return context.db();
      }
    });
    return m;
  }
}
