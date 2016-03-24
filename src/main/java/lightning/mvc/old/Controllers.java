package lightning.mvc.old;

import lightning.classloaders.ExceptingClassLoader;
import lightning.config.Config;
import lightning.db.MySQLDatabaseProvider;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.TemplateViewRoute;
import spark.template.freemarker.FreeMarkerEngine;

import com.google.common.collect.ImmutableList;

/**
 * An interface which makes it easy to install Controllers using Spark's routing API.
 * 
 * Example: 
 *   get("/test", Controllers.wrapRoute(ExampleController.class, dbProvider));
 *   get("/test", Controllers.wrapRoute((rq,rs,db) -> { return new ExampleController(rq, rs, db); }, dbProvider));
 *   
 * The usage is similar for TemplateViewRoutes, but call wrapTemplateViewRoute instead of wrapRoute.
 */
@Deprecated
public final class Controllers {
  @FunctionalInterface
  public static interface ControllerFactory {
    /**
     * @return A new controller instance.
     */
    public Controller get(Request request, Response response, MySQLDatabaseProvider provider, 
        Config config, FreeMarkerEngine templateEngine) throws Exception;
  }
  
  private static boolean isDebugMode = false;
  private static ImmutableList<String> autoReloadPrefixes;
  
  public static void setDebugMode(boolean isDebugMode, ImmutableList<String> autoReloadPrefixes) {
    Controllers.isDebugMode = isDebugMode;
    Controllers.autoReloadPrefixes = autoReloadPrefixes;
  }
  
  private static ControllerFactory createFactory(final Class<? extends Controller> clazz) {
    if (isDebugMode) {
      // Use the automatically-reloading class loader.
      return (request, response, dbProvider, config, templateEngine) -> {
        ExceptingClassLoader loader = new ExceptingClassLoader((className) -> {
          for (String prefix : autoReloadPrefixes) {
            if (className.startsWith(prefix)) {
              return false;
            }
          }
          
          return true;
        }, "target/classes");
        
        Class<?> loadedClass = loader.load(clazz.getName());
        
        return (Controller) loadedClass.getConstructor(Request.class, Response.class, MySQLDatabaseProvider.class, Config.class, FreeMarkerEngine.class)
            .newInstance(request, response, dbProvider, config, templateEngine);
      };
    }
    
    // Use the production class loader.
    return (request, response, dbProvider, config, templateEngine) -> {      
      return clazz.getConstructor(Request.class, Response.class, MySQLDatabaseProvider.class, Config.class, FreeMarkerEngine.class)
          .newInstance(request, response, dbProvider, config, templateEngine);
    };
  }
  
  public static Route wrapRoute(final Class<? extends Controller> clazz, final MySQLDatabaseProvider databaseProvider, final Config config, final FreeMarkerEngine templateEngine) {
    return wrapRoute(createFactory(clazz), databaseProvider, config, templateEngine);
  }
  
  public static Route wrapRoute(ControllerFactory factory, final MySQLDatabaseProvider databaseProvider, final Config config, final FreeMarkerEngine templateEngine) {
    return (Request request, Response response) -> {
      try (Controller c = factory.get(request, response, databaseProvider, config, templateEngine)) {
        c.beforeRequest();
        Object result = c.handleRequest();
        c.afterRequest();
        return result;
      }
    };
  }
  
  public static TemplateViewRoute wrapTemplateViewRoute(final Class<? extends Controller> clazz, final MySQLDatabaseProvider databaseProvider, final Config config, final FreeMarkerEngine templateEngine) {
    return wrapTemplateViewRoute(createFactory(clazz), databaseProvider, config, templateEngine);
  }
  
  public static TemplateViewRoute wrapTemplateViewRoute(ControllerFactory factory, final MySQLDatabaseProvider databaseProvider, final Config config, final FreeMarkerEngine templateEngine) {
    return (Request request, Response response) -> {
      return (ModelAndView) wrapRoute(factory, databaseProvider, config, templateEngine).handle(request, response);
    };
  }
}
