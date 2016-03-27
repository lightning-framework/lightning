# Lightning

An experimental light-weight web framework for Java built on Jetty.

The primary goal of this framework is to provide the convenience of PHP-style save-and-refresh development while enabling developers to leverage the type-safety and speed of Java. That said, the framework is still engineered to be fast!

The secondary goal of this framework is to make learning web development simple for beginners by having expressive yet simple APIs and powerful error reporting.

This framework was written for use in a course I taught at Rice University.

# Features

  - Built-in distributed sessions
  - Built-in authentication and user management (groups and permissions in-progress)
  - Built-in templating with Freemarker
  - Built-in MySQL support (JOOQ coming soon)
  - Built-in email support (via Mail)
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

# Getting Started

1. Write a configuration file. See `lightning.config.Config` for options.

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
        "static_files_path": "./",
        "template_files_path": "./"
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

```java
class MyApp {
  public static void main(String[] args) {
    // args[0] contains the path to your config file.
    Lightning.launch(new File(args[0]));
  }
}
```

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
  public Map<String, ?> handleProfilePage(@RParam("username") String username) throws Exception {
    return ImmutableMap.of(); // Return the template view model.
  }

  @Route(path="/api/messages", methods={POST})
  @RequireXsrfToken
  @RequireAuth
  @Json
  // Inject route parameters in a type-safe manner using @RParam. If the user provides the wrong
  // type, then a BadRequestException is triggered.
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
}
```

4. Define your websockets (if any).

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
    // Called when a route handler throws an exception of type NotFoundException.class.
    // Use ctx to generate the appropriate error page.
    // If your exception handler throws an exception, the default error page will be shown instead.
  }
}
```

# Error Page

In debug mode, whenever a request handler throws an Error or Exception you will see a detailed stack trace in your browser (in addition to the console):

![Debug Page](https://cloud.githubusercontent.com/assets/3498024/14005744/3fa323ba-f134-11e5-9f72-00da49a46ab7.png "Debug Page")


