package lightning.examples;

import static lightning.server.Context.redirect;
import static lightning.server.Context.redirectIfLoggedIn;
import static lightning.server.Context.url;
import static lightning.server.Context.user;

import java.io.IOException;

import lightning.Lightning;
import lightning.ann.Controller;
import lightning.ann.ExceptionHandler;
import lightning.ann.Initializer;
import lightning.ann.Json;
import lightning.ann.QParam;
import lightning.ann.RParam;
import lightning.ann.RequireAuth;
import lightning.ann.Route;
import lightning.ann.Template;
import lightning.config.Config;
import lightning.enums.HTTPMethod;
import lightning.http.BadRequestException;
import lightning.mvc.HandlerContext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class MVC2DemoApp {  
  @Controller
  public static final class ExampleController {
    @Route(path="/u/:id")
    public Object index(@RParam("id") int id) {
      return "Hello, " + id;
    }
    
    @Route(path="/loginx", methods={HTTPMethod.GET})
    @Template("login.ftl")
    public Object handleLogin() throws Exception {
      redirectIfLoggedIn(url().to("/"));
      return ImmutableMap.of(); // Empty view model.
    }
    
    @ExceptionHandler(BadRequestException.class)
    public static void handleException(HandlerContext ctx, BadRequestException e) throws IOException {
      ctx.response.raw().getWriter().write("custom ex handler");
    }
    
    @Route(path="/loginx", methods={HTTPMethod.POST})
    @Template("login.ftl")
    public Object handleLoginAction() throws Exception {
      redirectIfLoggedIn(url().to("/"));
      // Do processing.
      return redirect(url().to("/profile"));
    }
    
    @Route(path="/profile", methods={HTTPMethod.GET})
    @RequireAuth
    @Template("profile.ftl")
    public Object handleProfile() throws Exception {
      return ImmutableMap.of("username", user().getUserName());
    }
    
    @Route(path="/fetch", methods={HTTPMethod.GET})
    @Json
    public Object handle(@QParam("id") HTTPMethod id) {
      return ImmutableList.of("Bob", "Joe", "Jane", "Hector");
    }
    
    @Route(path="/", methods={HTTPMethod.GET})
    public Object handleMe() throws Exception {
      return "Welcome, " + user().getUserName();
    }
    
    @Route(path="/errordemo", methods={HTTPMethod.GET})
    public Object errorProducingHandler() throws Exception {
      throw new Exception("Something went horribly wrong!");
    }
    
    @Route(path="/assetdemo", methods={HTTPMethod.GET})
    public String assetHandler() throws Exception {
      return "<img src=\"smile.png\" alt=\"FAILED\" />";
    }
    
    @Route(path="/dynamicadd", methods={HTTPMethod.GET})
    public String dynamicAdd() throws Exception {
      return "TEST";
    }
    
    @Initializer
    public void hello() {
      
    }
  }
  
  public static void main(String[] args) throws Exception {
    Config config = new Config();
    config.scanPrefixes = ImmutableList.of(
        "lightning.examples.MVC2DemoApp",
        "lightning.examples.websockets"
      );
    config.autoReloadPrefixes = ImmutableList.of("lightning.examples.MVC2DemoApp");
    config.server.hmacKey = "ABCDEF";
    config.server.templateFilesPath = "./";
    config.server.staticFilesPath = "./";
    config.enableDebugMode = true;
    Lightning.launch(config);
  }
}
