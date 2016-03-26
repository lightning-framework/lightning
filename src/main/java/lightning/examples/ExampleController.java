package lightning.examples;

import lightning.ann.Controller;
import lightning.ann.Initializer;
import lightning.ann.Json;
import lightning.ann.QParam;
import lightning.ann.Route;
import lightning.enums.HTTPMethod;

import com.google.common.collect.ImmutableList;

@Controller
public class ExampleController {
  @Route(path="/", methods={HTTPMethod.GET})
  public Object handle() throws Exception {
    return "Hello World!";
  }
  
  @Initializer
  public void HelloThere() {
    
  }
  
  @Route(path="/help", methods={HTTPMethod.GET})
  public Object test(@QParam("name") String name, @QParam("age") int age) {
    return "Hello, " + name;
  }
  

  @Route(path="/wat", methods={HTTPMethod.GET})
  @Route(path="/login", methods={HTTPMethod.POST})
  public Object handle2() throws Exception {
    return "Hello World!";
  }
  
  @Route(path="/json", methods={HTTPMethod.GET})
  @Json
  public Object exampleJson() {
    return ImmutableList.of("Joe", "Bob", "Matt");
  }
}
