package lightning.examples;

import com.google.common.collect.ImmutableList;

import lightning.Lightning;
import lightning.ann.Controller;
import lightning.ann.Route;
import lightning.config.Config;
import lightning.enums.HTTPMethod;

/**
 * A demonstration of how routing priority works.
 */
public class RoutingDemo {
  public static void main(String[] args) throws Exception {
    Config config = new Config();
    config.server.port = 80;
    config.server.hmacKey = "ASDF";
    config.server.staticFilesPath = ".";
    config.server.templateFilesPath = ".";
    config.enableDebugMode = true;
    config.autoReloadPrefixes = ImmutableList.of("lightning.examples.ExampleRoute");
    config.scanPrefixes = config.autoReloadPrefixes;
    Lightning.launch(config);
  }
  
  @Controller
  public static final class ExampleController {
    @Route(path="/*", methods={HTTPMethod.GET})
    public String handleOne() {
      return "/*";
    }
    
    @Route(path="/:something", methods={HTTPMethod.GET})
    public String handleThree() {
      return "/:something";
    }
    
    @Route(path="/u/:something", methods={HTTPMethod.GET})
    public String handleFour() {
      return "/u/:something";
    }
    
    @Route(path="/u/*", methods={HTTPMethod.GET})
    public String handleFive() {
      return "/u/*";
    }
    
    @Route(path="/", methods={HTTPMethod.GET})
    public String handleTwo() {
      return "/";
    }
  }
}
