# Lightning

An experimental light-weight web framework for Java built on Jetty.

The primary goal of this framework is to provide the convenience of PHP-style save-and-refresh development while enabling developers to leverage the type-safety and speed of Java. That said, the framework is still engineered to be fast!

The secondary goal of this framework is to make learning web development simple for beginners by having expressive yet simple APIs and powerful error reporting.

This framework was written for use in a course I taught at Rice University.

# Features

  - Built-in distributed sessions
  - Built-in authentication and user management (groups and permissions in-progress)
  - Built-in templating with Freemarker
  - Built-in MySQL support
  - Built-in email support (via SMTP)
  - Built-in SSL support (w/ option to redirect insecure requests)
  - Built-in support for CAS authentication
  - Built-in support for HTTP multipart request processing and file uploads
  - Built-in validator for POST/GET parameters
  - Built-in annotation-based routing with parameters and wildcards
  - Built-in security features
    - Passwords are encrypted with BCrypt
    - Cookies are signed and verified with HMAC
    - Only hashes stored in the database
  - Powerful development (debug) mode
    - **Develop without ever restarting the server** (PHP-style save and refresh)
    - **Detailed in-browser stack traces on errors**
    - Disables caching of static files
    - Disables caching of templates
  - Powerful type-safe HTTP abstractions
  - Support for async request processing
  - Built-in support for websockets
  - Built-in support for HTTP/2
  - Built-in support for argument injection

# Getting Started

1. Write a configuration file. See documentation of `lightning.config.Config` for a description of options.

In particular, you must define `scan_prefixes` so that the framework knows where to look for routes, exception handlers, and web sockets.

```json
{
    "enable_debug_mode": true,
    "auto_reload_prefixes": [
        "my.app.package"
    ],
    "scan_prefixes": [
        "my.app.package"
    ],
    "server": {
        "port": 80,
        "hmac_key": "LONG_RANDOM_SOMETHING",
        "static_files_path": "path/in/src/main/resources/",
        "template_files_path": "path/in/src/main/resources/"
    },
    "db": {
        "host": "localhost",
        "port": 3306,
        "username": "httpd",
        "password": "httpd",
        "name": "dbname"
    }
}
```

** NOTE ** You'll want to import the schema provided by lightning in order to have access to users and sessions. You can find this in the `src/main/java/resources/schema` folder of the lightning source code.

2. Write a launcher.

If you do not wish to use JSON-formatted configuration files, you may skip the above step and instead build your own instance of `Config` in code anyway you like and pass it to `Lightning::launch`.

```java
class MyApp {
  public static void main(String[] args) {
    // Optional: To inject custom dependencies into your handlers, use an InjectorModule.
    // See the documentation for InjectorModule for more information.
    InjectorModule injector = new InjectorModule();
    injector.bindClassToInstance(MyDependency.class, new MyDependency());
    injector.bindNameToInstance("my_string", "Hello!");
    injector.bindAnnotationToInstance(MyDepAnn.class, "Hello!");

    // args[0] contains the path to your config file.
    Lightning.launch(new File(args[0]), injector);
  }
}
```

To start the webserver, you can run the launcher you just created. If you are using debug mode, you will want to set the working directory to the root of your project folder (the folder that contains `./src`) so that classes, templates, and files can be automatically reloaded from disk.

3. Define your controllers (within the packages defined in `scan_prefixes` or any subpackage thereof).

```java
// Provides access to request, response, session, auth, user, and other helpers.
import static lightning.server.Context.*;

import static lightning.enums.HTTPMethod.*;
import lightning.ann.*;

@Controller
class MyController {
  @Route(path="/u/:username", methods={GET})
  @Template("profile.ftl")
  // Inject the value of the route parameter "username".
  public Map<String, ?> handleProfilePage(@RParam("username") String username) throws Exception {
    return ImmutableMap.of(); // Return the template view model.
  }

  @Route(path="/api/messages", methods={POST})
  @RequireXsrfToken
  @RequireAuth
  @Json
  // Inject query parameters in a type-safe manner using @QParam. If the user provides the wrong
  // type, then a BadRequestException is triggered. Thus, messageId is guaranteed to be an integer.
  public Map<String, ?> handleMessagesApi(@QParam("mid") int messageId) throws Exception {
    Map<String, Object> message = new HashMap<>();

    try (NamedPreparedStatement query = db()
             .prepare("SELECT * FROM messages WHERE mid = :mid AND userid = :userid;")) {
      query.setLong("userid", user().getId());
      query.setInt("mid", messageId);
      try (ResultSet rs = query.executeQuery()) {
        notFoundIf(!rs.next());
        message.setContent(rs.getString("content"));
        message.setTime(rs.getDate("time"));
      }
    }

    return message; // Will get JSONified.
  }

  @Initializer
  // All routes, exception handlers, web socket factories, and initializers are INJECTABLE.
  // This means the framework will automatically figure out the arguments to fill in to the function
  // based upon their types and annotations. For example, to inject the three dependencies you
  // defined in the launcher:
  public void initialize(MyDependency myDep, @MyDepAnn String myDep2, @Inject("my_string") String myDep3) {
    // Called automatically when the controller is instantied but before invoking any routes.
    // In addition to the dependencies you defined in the launcher, the framework can be used to automatically
    // inject other things: Request, Response, Config, MySQLDatabase, etc.
    // Note that most of these things are also provided via static methods on lightning.server.Context.
  }
}
```

4. Define your websockets (if any).

Websockets are singletons (a single instance is created to service all incoming requests) and utilize the Jetty API. Unfortunately, web sockets currently cannot be automatically reloaded in debug mode (due to some limitations in Jetty) so you'll need to restart the server to see changes to websocket handlers.

```java
class MyWebsockets {
  @WebSocketFactory(path="/mysocket")
  public static MyWebsocket produceWebsocket(Config config, MySQLDatabaseProvider db) {
    return new MyWebsocket();
  }

  @WebSocket
  public static final class MyWebsocket {
    @OnWebSocketConnect
    public void connected(final Session session) throws IOException {
      session.getRemote().sendString("HELLO!");
    }

    @OnWebSocketClose
    public void closed(final Session session, final int statusCode, final String reason) {}

    @OnWebSocketMessage
    public void message(final Session session, String message) throws IOException {
      session.getRemote().sendString("THANKS!");
    }

    @OnWebSocketError
    public void error(final Session session, Throwable error) {}
  }
}
```

5. Define your custom exception handlers (if any).

```java
class MyExceptionHandlers {
  @ExceptionHandler(NotFoundException.class)
  public static void handleException(HandlerContext ctx, Exception e) throws Exception {
    // Called when a route handler throws an exception of type NotFoundException.class (or any subclass
    // thereof unless a more specific exception handler is installed upon that subclass).
    // Use ctx to generate the appropriate error page.
    // If your exception handler throws an exception, the default error page will be shown instead.
  }
}
```

You can override the default error pages (e.g. for 404 not found) by installing an exception handler for the corresponding exception (e.g. `lightning.http.NotFoundException`).

# Error Page

In debug mode, whenever a request handler throws an Error or Exception you will see a detailed stack trace in your browser (in addition to the console):

![Debug Page](https://cloud.githubusercontent.com/assets/3498024/14005744/3fa323ba-f134-11e5-9f72-00da49a46ab7.png "Debug Page")

# Examples

The following applications are built using Lightning:

* [Rice Schedule Planner](https://github.com/rice-apps/scheduleplanner)
* [Tiny URL Demo](https://github.com/mschurr/coll144-assignment6/tree/master/src/main/java/demos/tinyurl)
* [File Share Demo](https://github.com/mschurr/coll144-assignment6/tree/master/src/main/java/demos/fileshare)

