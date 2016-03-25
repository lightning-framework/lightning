package lightning.examples;

import java.io.FileInputStream;

import spark.utils.IOUtils;
import lightning.Lightning;
import lightning.config.Config;
import lightning.mvc.Controller;
import lightning.mvc.HTTPMethod;
import lightning.mvc.Initializer;
import lightning.mvc.Json;
import lightning.mvc.QParam;
import lightning.mvc.RParam;
import lightning.mvc.RequireAuth;
import lightning.mvc.Route;
import lightning.mvc.Template;
import lightning.util.Flags;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static lightning.Context.*;

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
    
    @Route(path="/assetdemo", methods={HTTPMethod.GET})
    public String assetHandler() throws Exception {
      return "<img src=\"smile.png\" alt=\"FAILED\" />";
    }
    
    @Initializer
    public void hello() {
      
    }
  }
  
  public static void main(String[] args) throws Exception {
    Flags.parse(args);
    Config config = Lightning.newGson().create().fromJson(IOUtils.toString(new FileInputStream(Flags.getFile("config"))), Config.class);
    Lightning.launch(config);
  }
}
