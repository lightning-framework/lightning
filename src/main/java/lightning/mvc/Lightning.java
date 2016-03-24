package lightning.mvc;

import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.ipAddress;
import static spark.Spark.port;
import static spark.Spark.secure;
import static spark.Spark.staticFileLocation;
import static spark.Spark.threadPool;
import static spark.Spark.webSocketIdleTimeoutMillis;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lightning.auth.Auth;
import lightning.auth.drivers.MySQLAuthDriver;
import lightning.classloaders.ExceptingClassLoader;
import lightning.config.Config;
import lightning.crypt.Hashing;
import lightning.crypt.SecureCookieManager;
import lightning.db.MySQLDatabaseProvider;
import lightning.debugscreen.DebugScreen;
import lightning.groups.Groups;
import lightning.groups.drivers.MySQLGroupDriver;
import lightning.http.BadRequestException;
import lightning.http.Exceptions;
import lightning.http.NotAuthorizedException;
import lightning.http.NotFoundException;
import lightning.io.WrappingReader;
import lightning.mail.Mail;
import lightning.sessions.Session;
import lightning.sessions.drivers.MySQLSessionDriver;
import lightning.users.Users;
import lightning.users.drivers.MySQLUserDriver;
import lightning.util.Enums;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.ModelAndView;
import spark.Request;
import spark.RequestResponseFactory;
import spark.Response;
import spark.Spark;
import spark.route.SimpleRouteMatcher;
import spark.routematch.RouteMatch;
import spark.template.freemarker.FreeMarkerEngine;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.gson.GsonBuilder;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;

// TODO: After/Afters, Before/Befores, Cacheable, Compress, Require(R|Q)Param(s)
// TODO: better url gen w/ reverse routes
// TODO: static files in debug mode?
public class Lightning {
  private static final Logger logger = LoggerFactory.getLogger(Lightning.class);
  private static final Map<RouteTarget, RouteAction> routes = new HashMap<>();
  private static final Map<Class<?>, List<Method>> initializers = new HashMap<>();
  private static final Set<Class<?>> controllerClasses = new HashSet<>();
  private static FreeMarkerEngine templateEngine = null;
  private static Config config = null;
  private static MySQLDatabaseProvider dbp = null;
  private static final SimpleRouteMatcher matcher = new SimpleRouteMatcher();
  
  //NOTE: Prefix and suffix the templates to prevent XSS by default. If you need to print unescaped string
  // in templates, use <#noescape>.
  // @see http://freemarker.incubator.apache.org/docs/dgui_template_valueinsertion.html#autoid_15
  private static final String TEMPLATE_PREFIX = "<#ftl strip_whitespace=true><#escape x as x?html>";
  private static final String TEMPLATE_SUFFIX = "</#escape>";
  
  @SuppressWarnings("deprecation")
  public static void launch(Config cfg) throws Exception {
    config = cfg;
    logger.info("Working Directory: " + (new java.io.File(".").getCanonicalPath()));
    
    // Configure spark.
    logger.debug("Configuring Spark");
    ipAddress("0.0.0.0");
    webSocketIdleTimeoutMillis(config.server.timeoutMs);
    threadPool(config.server.threads);
    port(config.server.port);
    SecureCookieManager.setSecretKey(config.server.hmacKey);
    Hashing.setSecretKey(config.server.hmacKey);
    
    if (config.ssl.isEnabled()) {
      secure(
          config.ssl.keyStoreFile,
          config.ssl.keyStorePassword,
          config.ssl.trustStoreFile,
          config.ssl.trustStorePassword
      );
      SecureCookieManager.alwaysSetSecureOnly(); 
    }
    
    if (config.mail.isEnabled()) {
      Mail.configure(config.mail);
    }

    dbp = new MySQLDatabaseProvider(
        config.db.host, 
        config.db.port, 
        config.db.username, 
        config.db.password, 
        config.db.name);
    
    Session.setDriver(new MySQLSessionDriver(dbp));
    Groups.setDriver(new MySQLGroupDriver(dbp));
    Users.setDriver(new MySQLUserDriver(dbp));
    Auth.setDriver(new MySQLAuthDriver(dbp));
    
    // Configure template loaders.
    FreeMarkerEngine privateTemplateEngine = new FreeMarkerEngine();
    Configuration privateConfig = new Configuration();
    privateConfig.setTemplateLoader(new ClassTemplateLoader(Lightning.class, "../templates") {
      @Override
      public Reader getReader(Object templateSource, String encoding) throws IOException {
        return new WrappingReader(super.getReader(templateSource, encoding), TEMPLATE_PREFIX, TEMPLATE_SUFFIX);
      }
    });
    privateTemplateEngine.setConfiguration(privateConfig);
    
    FreeMarkerEngine userTemplateEngine = new FreeMarkerEngine();
    Configuration userConfig = new Configuration();
    File templateFolder = (new File("./src/main/java/" + config.server.templateFilesPath)).exists() ?
        new File("./src/main/java/" + config.server.templateFilesPath) :
        new File(config.server.templateFilesPath);
        
    userConfig.setTemplateLoader(new FileTemplateLoader(templateFolder) {
      @Override
      public Reader getReader(Object templateSource, String encoding) throws IOException {
        return new WrappingReader(super.getReader(templateSource, encoding), TEMPLATE_PREFIX, TEMPLATE_SUFFIX);
      }
    });
    
    userTemplateEngine.setConfiguration(userConfig);
    templateEngine = userTemplateEngine;
        
    // Find all web sockets.
    Set<Class<?>> wsClasses = new HashSet<>();
    for (String searchPath : config.scanPrefixes) {
      Reflections wsReflections = new Reflections(searchPath, new SubTypesScanner(), new TypeAnnotationsScanner(), new MethodAnnotationsScanner());
      wsClasses.addAll(wsReflections.getTypesAnnotatedWith(WebSocketController.class));
    }
    
    logger.debug("Found WebSocket Controller Classes: {}", wsClasses);
    
    // Instantiate web sockets.    
    logger.debug("Installing Websockets");
    for (Class<?> ws : wsClasses) {
      String path = ws.getAnnotation(WebSocketController.class).path();
      logger.debug("Added Websocket: {} -> {}", path, ws);
      Spark.webSocket(path, ws);
      routes.put(new RouteTarget(HTTPMethod.GET, path), null);
    }
    
    // Find all controllers.    
    rescanAnnotations();
    
    if (!config.enableDebugMode) {
      // Serve static files from Jetty (cached) for production.
      staticFileLocation(config.server.staticFilesPath);
    }
    
    if (config.enableDebugMode) {
      spark.Route debugHandler = (req, res) -> {
        // Remove all routes.
        matcher.clearRoutes();
        
        // Rebuild the routing map on-the-fly.
        // Use dynamic class reloading to re-process annotations.
        rescanAnnotationsForDebug();
        
        // Resolve the route.
        spark.route.HttpMethod method = Enums.getValue(spark.route.HttpMethod.class, req.requestMethod()).get();
        RouteMatch match = matcher.findTargetForRequestedRoute(
            method, 
            req.raw().getPathInfo(), 
            Optional.fromNullable(req.headers("Accept")).or("*/*"));
        
        if (match == null) {
          logger.debug("DebugMode: No route match was found for {} {}", method, req.raw().getPathInfo());
          throw new NotFoundException();
        }
        
        req = RequestResponseFactory.create(match, req.raw());
        spark.Route action = (spark.Route) match.getTarget();
        
        try {
          Object result = action.handle(req, res);
          return result instanceof ModelAndView ?
              templateEngine.render((ModelAndView) result) :
              result;
        } catch (Error error) { // Since Spark can only catch java.lang.Exceptions.
          DebugScreen screen = new DebugScreen();
          screen.handleThrowable(error, req, res);
          return null;
        }
      };
      
      Spark.get("*", debugHandler);
      Spark.post("*", debugHandler);
      Spark.put("*", debugHandler);
      Spark.patch("*", debugHandler);
      Spark.delete("*", debugHandler);
      Spark.options("*", debugHandler);
      Spark.head("*", debugHandler);
      Spark.trace("*", debugHandler);
    }
    
    Exceptions.installExceptionHandlers(config, privateTemplateEngine);
    
    // Install static file handling.
    if (config.enableDebugMode) {
      // Serve static files from a controller for instant reloads after each request.
      /*get("/static/*", () -> {
        File f = new File(base, "static/" + request.splat()[0]);
        notFoundIf(!f.exists() || !f.canRead() || f.isDirectory());
        badRequestIf(!FilenameUtils.directoryContains(base.getCanonicalPath(), f.getCanonicalPath()));
        
        response.status(200);
        response.header("Cache-Control", "public, max-age=0, must-revalidate");
        response.header("Content-Type", Mimes.forExtension(FilenameUtils.getExtension(f.getName())));
        response.header("Content-Disposition", "inline; filename=" + f.getName());
        response.header("Content-Length", Long.toString(f.length()));    
        try (FileInputStream stream = new FileInputStream(f)) {
          IOUtils.copy(stream, response.raw().getOutputStream());
        }
        return null;
      });*/
      
      // Schedule templates to automatically reload after each request.
      before((request, response) -> { userConfig.clearTemplateCache(); });
      after((request, response) -> { userConfig.clearTemplateCache(); });
    }
    
    // Initialize Spark.
    logger.info("Initialization complete; starting Spark/Jetty [debug={}].", config.enableDebugMode);
    Spark.init();
  }
  
  private static void rescanAnnotations() {
    rescanAnnotations(Lightning.class.getClassLoader());
  }
  
  private static void rescanAnnotationsForDebug() {
    ExceptingClassLoader loader = new ExceptingClassLoader((className) -> {
      for (String prefix : config.autoReloadPrefixes) {
        if (className.startsWith(prefix)) {
          return false;
        }
      }
      
      return true;
    }, "target/classes");
    
    rescanAnnotations(loader);
  }
  
  private static Reflections[] getReflections(ClassLoader loader) {
    Reflections[] result = new Reflections[config.scanPrefixes.size()];
    
    int i = 0;
    for (String searchPath : config.scanPrefixes) {
      ConfigurationBuilder config = ConfigurationBuilder.build(searchPath, loader, new SubTypesScanner(), new TypeAnnotationsScanner(), new MethodAnnotationsScanner());
      result[i] = new Reflections(config);
      i++;
    }
    
    return result;
  }
  
  private static synchronized void rescanAnnotations(ClassLoader loader) {
    logger.info("Rescanning Annotations...");
    controllerClasses.clear();

    for (Reflections reflections : getReflections(loader)) {
      controllerClasses.addAll(reflections.getTypesAnnotatedWith(Controller.class));
    }
    
    logger.debug("Found Controller Classes: {}", controllerClasses);
    
    // Find initializers.
    initializers.clear();
    Set<Method> initializerMethods = new HashSet<>();
    for (Reflections reflections : getReflections(loader)) {
      initializerMethods.addAll(reflections.getMethodsAnnotatedWith(Initializer.class));
    }    
    
    for (Method m : initializerMethods) {
      if (m.getDeclaringClass().getAnnotation(Controller.class) == null) {
        continue;
      }
      
      m.setAccessible(true);
      
      if (!initializers.containsKey(m.getDeclaringClass())) {
        initializers.put(m.getDeclaringClass(), new ArrayList<>());
      }
      
      initializers.get(m.getDeclaringClass()).add(m);
    }
    
    logger.debug("Found Initializers: {}", initializers);
    
    // Initialize routes.    
    logger.debug("Installing Routes");
    
    routes.clear();
    for (Reflections reflections : getReflections(loader)) {      
      for (Method m : reflections.getMethodsAnnotatedWith(Route.class)) {
        initRoute(m, m.getAnnotation(Route.class));
      }
      
      for (Method m : reflections.getMethodsAnnotatedWith(Routes.class)) {
        for (Route r : m.getAnnotation(Routes.class).value()) {
          initRoute(m, r);
        }
      }
    }
  }
  
  public static ProxyController newContext(Request req, Response res) {
    return new ProxyController(req, res, dbp, config, templateEngine);
  }
  
  private static void initRoute(Method method, Route route) {
    if (!controllerClasses.contains(method.getDeclaringClass())) {
      return;
    }
    
    logger.debug("Adding Route: {} -> {}", route, method);
    
    final boolean requireAuth = method.getAnnotation(RequireAuth.class) != null;
    final boolean makeJson = method.getAnnotation(Json.class) != null;
    final boolean requireXsrf = method.getAnnotation(RequireXsrfToken.class) != null;
    final String xsrfTokenName = requireXsrf ? method.getAnnotation(RequireXsrfToken.class).inputName() : null;
    final boolean producesTemplate = method.getAnnotation(Template.class) != null;
    final String templateName = producesTemplate ? method.getAnnotation(Template.class).value() : null;
    final boolean requireMultipart = method.getAnnotation(Multipart.class) != null;
    final String accepts = method.getAnnotation(Accepts.class) != null ? method.getAnnotation(Accepts.class).value() : "*/*";
    
    spark.Route action = (request, response) -> {
      Object value = null;
      try (ProxyController c = new ProxyController(request, response, dbp, config, templateEngine)) {
        Context.setContext(c);

        if (requireMultipart) {
          c.requireMultipart();
        }
        
        if (requireAuth) {
          if (!Context.auth().isLoggedIn()) {
            throw new NotAuthorizedException();
          }
        }
        
        if (requireXsrf) {
          if (!Context.session().getXSRFToken().equals(request.queryParams(xsrfTokenName))) {
            throw new BadRequestException("An cross-site request forgery attack was detected and prevented.");
          }
        }
        
        Object controller = null;
        
        /*if (config.enableDebugMode) {
          ExceptingClassLoader loader = new ExceptingClassLoader((className) -> {
            for (String prefix : config.autoReloadPrefixes) {
              if (className.startsWith(prefix)) {
                return false;
              }
            }
            
            return true;
          }, "target/classes");
          
          Class<?> loadedClass = loader.load(method.getDeclaringClass().getName());
          
          controller = loadedClass.newInstance();
        } else {*/
          controller = method.getDeclaringClass().newInstance();
        /*}*/
        
        if (initializers.containsKey(method.getDeclaringClass())) {
          for (Method init : initializers.get(method.getDeclaringClass())) {
            if (config.enableDebugMode) {
              init = controller.getClass().getMethod(init.getName());
            }
            
            try {
              init.invoke(controller);
            } catch (InvocationTargetException e) {
              if (e.getCause() != null && e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
              }
              
              throw e;
            }
          }
        }
        
        Method realMethod = config.enableDebugMode ? controller.getClass().getMethod(method.getName(), method.getParameterTypes()) : method;
        
        try {
          if (method.getParameterCount() >= 1) {
            Object[] args = new Object[method.getParameterCount()];
            Parameter[] params = method.getParameters();
            for (int i = 0; i < params.length; i++) {
              if (params[i].getAnnotation(QParam.class) != null) {
                String name = params[i].getAnnotation(QParam.class).value();
                
                if (request.queryParams(name) != null) {
                  args[i] = Param.wrap(name, request.queryParams(name)).castTo(params[i].getType());
                  continue;
                }
                
                throw new BadRequestException("Required query parameter " + name + " was not present.");
              } else if(params[i].getAnnotation(RParam.class) != null) {
                String name = params[i].getAnnotation(RParam.class).value();
    
                if (request.params(":" + name) != null) {
                  args[i] = Param.wrap(name, request.params(":" + name)).castTo(params[i].getType());
                  continue;
                }
                
                throw new BadRequestException("Required path parameter " + name + " was not present.");
              } else {
                throw new IllegalStateException("Cannot figure out how to inject arguments for route target " + method);
              }
            }
    
            value = realMethod.invoke(controller, args);
          } else {
            value = realMethod.invoke(controller);
          }
        } catch (InvocationTargetException e) {
          if (e.getCause() != null && e.getCause() instanceof Exception) {
            throw (Exception) e.getCause();
          }
          
          if (e.getCause() != null) {
            logger.error("An error occured: ", e);
            DebugScreen screen = new DebugScreen();
            screen.handleThrowable(e.getCause(), request, response);
            return null;
          }
          
          throw e;
        }
        
        if (makeJson) {
          response.status(200);
          response.type("application/json; charset=UTF-8");
          return new GsonBuilder().setPrettyPrinting().create().toJson(value);
        }
        
        if (producesTemplate && !(value instanceof ModelAndView)) {
          return Spark.modelAndView(value, templateName);
        }
        
        return value;
      } finally {
        Context.clearContext();
      }
    };
    
    for (HTTPMethod httpMethod : route.methods()) {
      for (String path : ImmutableList.of(route.path(), route.path() + "/")) {
        RouteTarget target = new RouteTarget(httpMethod, path);
        
        if (routes.containsKey(target)) {
          throw new IllegalStateException("Failed to initialize routes: duplicate " + target);
        }
        
        routes.put(target, null);

        if (config.enableDebugMode) {
          matcher.parseValidateAddRoute(httpMethod.toString().toUpperCase() + " '" + path +"'", accepts, action);
        } else if (producesTemplate) {
          httpMethod.installRoute(path, accepts, (req, res) -> {
            return (ModelAndView) action.handle(req, res);
          }, templateEngine);
        } else {
          httpMethod.installRoute(path, accepts, action);
        }
      }
    }
  }
}
