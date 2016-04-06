package lightning.examples;

import lightning.Lightning;
import lightning.ann.Controller;
import lightning.ann.ExceptionHandler;
import lightning.ann.Inject;
import lightning.ann.Route;
import lightning.config.Config;
import lightning.db.MySQLDatabase;
import lightning.enums.HTTPMethod;
import lightning.examples.ExampleDependency.MyType;
import lightning.http.NotFoundException;
import lightning.http.Request;
import lightning.http.Response;
import lightning.inject.InjectorModule;

import com.google.common.collect.ImmutableList;

/**
 * A demonstration of how routing priority works.
 */
public class InjectionDemo {
  public static void main(String[] args) throws Exception {
    Config config = new Config();
    config.server.port = 80;
    config.server.hmacKey = "ASDF";
    config.server.staticFilesPath = ".";
    config.server.templateFilesPath = ".";
    config.enableDebugMode = true;
    config.autoReloadPrefixes = ImmutableList.of("lightning.examples.InjectionDemo");
    config.scanPrefixes = config.autoReloadPrefixes;
    
    // You can bind dependencies to be resolved here.
    // Resolution can occur either by name, type (class), or the presence of an annotation.
    InjectorModule injector = new InjectorModule();
    injector.bindNameToInstance("message", "OMG");
    injector.bindClassToInstance(ExampleDependency.class, new ExampleDependency());
    injector.bindAnnotationToInstance(MyType.class, new ExampleDependency());
    injector.bindClassToInstance(ReloadableDependency.class, new ReloadableDependency());
    
    Lightning.launch(config, injector);
  }
  
  public static final class ReloadableDependency {}
  
  @Controller
  public static final class ExampleController {
    @Route(path="/", methods={HTTPMethod.GET})
    public String exampleHandler(MySQLDatabase db, 
        Request request, @Inject("message") String message, ExampleDependency mydep, @MyType ExampleDependency mydep2) {
      return "YAY! "+ mydep + " " + mydep2;
    }
    

    @Route(path="/reload-test", methods={HTTPMethod.GET})
    public String willCauseError(ReloadableDependency rd) throws Exception {
      // In debug mode, ReloadableDependency will fail to be injected due to how class hotswapping works.
      // In production mode, this works fine.
      // KEY POINT: Do not inject classes that are within the autoreload prefixes.
      return "YAY!";
    }
    
    @ExceptionHandler(NotFoundException.class)
    public static void handleException(Response response, Exception e) throws Exception {
      response.getWriter().write("Ex Handler: " + e.toString());
    }
    
    public ExampleController(ExampleDependency dep) {
      
    }
  }
}
