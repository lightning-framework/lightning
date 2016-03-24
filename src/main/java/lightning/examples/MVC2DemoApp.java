package lightning.examples;

import lightning.config.Config;
import lightning.mvc.Controller;
import lightning.mvc.HTTPMethod;
import lightning.mvc.Initializer;
import lightning.mvc.Json;
import lightning.mvc.Lightning;
import lightning.mvc.QParam;
import lightning.mvc.RParam;
import lightning.mvc.RequireAuth;
import lightning.mvc.Route;
import lightning.mvc.Template;
import lightning.util.Flags;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static lightning.mvc.Context.*;

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
    
    @Initializer
    public void hello() {
      
    }
  }
  
  public static void main(String[] args) throws Exception {
    Flags.parse(args);
    
    Config config = Config.newBuilder()
        .setEnableDebugMode(true) // Don't enable in production (use Flags).
        .setAutoReloadPrefixes(ImmutableList.of(
            "lightning.examples.MVC2DemoApp"
        ))
        .setScanPrefixes(ImmutableList.of(
            "lightning.examples.MVC2DemoApp",
            "lightning.examples.cas"
        ))
        .server.setPort(80)
        .server.setHmacKey("Kas8FJsa01kfF")
        .server.setStaticFilesPath("lightning/examples")
        .server.setTemplateFilesPath("lightning/examples")
        .db.setHost("localhost")
        .db.setPort(3306)
        .db.setUsername("httpd")
        .db.setPassword("httpd")
        .db.setName("exampledb")
        .mail.setAddress("donotreply@riceapps.org")
        .mail.setHost("smtp.riceapps.org")
        .mail.setPort(465)
        .mail.setUseSSL(true)
        .mail.setUsername("donotreply@riceapps.org")
        .mail.setPassword("")
        .build();
    
    Lightning.launch(config);
  }
}
