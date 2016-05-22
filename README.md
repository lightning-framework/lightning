# Lightning

A simple, light-weight web framework for Java built on Jetty.

Our design goals are...

  * To provide the convenience of PHP-style save-and-refresh development in Java
  * To make web development simple for beginners by having simple, expressive APIs

This framework was written for use in a course I taught at Rice University.

# Features

  - Powerful type-safe HTTP abstractions
  - Built-in annotation-based request routing (w/ wildcards and parameters)
  - Built-in templating with Freemarker
  - Built-in email support (via SMTP)
  - Built-in SSL support (w/ option to redirect insecure requests)
  - Built-in MySQL support (w/ connection pooling, transactions)
  - Built-in distributed sessions (via MySQL or other driver)
  - Built-in authentication and users (via MySQL or other driver)
  - Built-in support for CAS authentication
  - Built-in support for HTTP multipart request processing and file uploads
  - Built-in validator for POST/GET parameters
  - Powerful development (debug) mode
    - **Develop without ever restarting the server** (PHP-style save and refresh)
    - **Detailed in-browser stack traces on errors**
    - Disables caching of static files
    - Disables caching of templates
  - Support for async request processing (via Servlet API)
  - Built-in support for websockets
  - Built-in support for HTTP/2
  - Built-in support for custom dependency injection
  - Annotation and path-based filters

# Getting Started

### 1) Create a Configuration

The first step is to build an instance of `lightning.config.Config`. You can build the instance in code anyway you like. A simple way is to parse it from a JSON-formatted file.

All of the options are well-documented; see the source of `lightning.config.Config` for descriptions.

In your config, you must define `scanPrefixes` - a list of packages in which the framework will look for controllers, exception handlers, and web sockets.

An example JSON-formatted configuration file might look like:

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

** NOTE ** You'll want to import the schema provided by lightning in order to have access to users and sessions - if you choose to utilize that functionality. You can find the SQL file to import the schema in the `src/main/java/resources/schema` folder of the lightning source code.

### 2) Write a launcher for your app.

The launcher is the main class you will run to start your app. Your launcher can be as simple or sophisticated as you want it to be.

Your launcher should do three things:
* Build a `lightning.config.Config` instance
* Build a custom dependency injection module (optional)
* Invoke `lightning.Lightning.launch(...)`

An example launcher follows:

```java
class MyApp {
  private static void parseConfig(File file) throws Exception {
    return JsonFactory.newJsonParser().fromJson(
        IOUtils.toString(new FileInputStream(file)),
        Config.class);
  }

  public static void main(String[] args) throws Exception {
    // Parse command-line flags.
    Flags.parse(args);

    // Parse the configuration from a JSON file.
    Config config = parseConfig(new File(args[0]));

    // Modify config values depending on flags.
    if (Flags.has("debug")) {
      config.enableDebugMode = true;
    }

    // To inject custom dependencies into your handlers, define an InjectorModule.
    // See the documentation for InjectorModule for more information.
    // Any dependencies you bind here will be injectable as arguments.
    InjectorModule injector = new InjectorModule();
    injector.bindClassToInstance(MyDependency.class, new MyDependency());
    injector.bindNameToInstance("my_string", "Hello!");
    injector.bindAnnotationToInstance(MyDepAnn.class, "Hello!");

    LightningServer server = Lightning.launch(config, injector);
    server.join();
  }
}
```

** NOTE ** If you are using debug mode, you will want to set the working directory to the root of your project folder (the folder that contains `/src/main/java`) so that code snippets, classes, templates, and files can be automatically reloaded from disk. Any working directory can be used if you are not running in debug mode.

### 3) Define your controllers.

A controller is simply a class that handles incoming requests. Controllers must be annotated with `@Controller` and defined within a subpackage of any of the packages provided in `scanPrefixes`.

The lifetime of a controller matches the lifetime of a request: a new instance is allocated for each incoming request and deallocated after the response is sent.

An example controller follows:

```java
// Provides access to request, response, session, auth, user, and other helpers.
import static lightning.server.Context.*;

// Provides the annotations used by the framework.
import lightning.ann.*;
import static lightning.enums.HTTPMethod.*;

// Any class which contains routes must be annotated with @Controller.
// This annotation is inherited by subclasses.
@Controller
class MyController {
  // Lightning will automatically scan for annotations to install this route.
  // Notice we have defined a route with a dynamic parameter (:username).
  @Route(path="/u/:username", methods={GET})
  // Indicates that this method returns a view model for profile.ftl.
  @Template("profile.ftl")
  // Inject the value of the route parameter "username" by
  // annotating the parameter with @RParam.
  // Lightning is flexible; your handler can accept any number
  // of injectable arguments and may return any object (or even
  // void) depending on what your handler does.
  public Map<String, ?> handleProfilePage(@RParam("username") String username)
      throws Exception {
    return ImmutableMap.of(); // Return the template view model.
  }

  @Route(path="/api/messages", methods={POST})
  // Requires that the XSRF token is present and valid under the query
  // parameter named "_xsrf".
  @RequireXsrfToken
  // Requires that there is an authenticated user.
  @RequireAuth
  // Indicates that the returned value should be JSONified with GSON
  // and that JSON HTTP response headers should be set.
  @Json
  // Inject query parameters in a type-safe manner using @QParam. If the user provides the wrong
  // type, then a BadRequestException is triggered. Thus, messageId is guaranteed to be an integer.
  // Exceptions thrown from a handler will generate an error page. For HTTP exceptions (those
  // defined in lightning.http), the corresponding HTTP error page will be shown with the status code
  // set. All other exceptions will trigger a generic 500 Internal Server Error in production, or a
  // in-browser stack trace in development mode. Lightning expects users to take advantage of these
  // exception handling features when writing code. Note that the error pages can be overriden - see
  // our notes about installing custom exception handlers.
  public Map<String, ?> handleMessagesApi(@QParam("mid") int messageId) throws Exception {
    Map<String, Object> message = new HashMap<>();

    // db() acquires and returns a database connection on its first invocation.
    // subsequent invocations will return the same connection.
    // the connection is automatically freed when the controller is deallocated.
    // Querying follows the typical JDBC syntax (with the convenient addition of named
    // parameters in prepared statements).
    try (NamedPreparedStatement query = db()
             .prepare("SELECT * FROM messages WHERE mid = :mid AND userid = :userid;")) {
      query.setLong("userid", user().getId());
      query.setInt("mid", messageId);
      try (ResultSet rs = query.executeQuery()) {
        notFoundIf(!rs.next()); // Generate a 404 page and return from handler via NotFoundException.
        message.setContent(rs.getString("content"));
        message.setTime(rs.getDate("time"));
      }
    }

    return message; // Will get JSONified.
  }

  @Initializer
  // An initializer will be called automatically before any request processing occurs - in other words,
  // serving the same purpose as a constructor. Initializers in parent classes will be invoked, too,
  // when the controller is allocated. Initializers may be preferred to constructors because they are
  // inheritable without requiring additional code in child classes.
  // All routes, exception handlers, web socket factories, and initializers are INJECTABLE.
  // This means the framework will automatically figure out the arguments to fill in to the function
  // based upon their types and annotations. For example, to inject the three dependencies you
  // defined in the launcher:
  public void initialize(MyDependency myDep, @MyDepAnn String myDep2, @Inject("my_string") String myDep3) {
    // In addition to the dependencies you defined in the launcher, the framework can be used to automatically
    // inject other request-specific things: Request, Response, Config, MySQLDatabase, etc.
    // Note that most of these things are also provided via the static methods on lightning.server.Context.
  }

  @Finalizer
  // A finalizer is always called before deallocating the controller. Finalizers serve the purpose of
  // destructors and may safely be used to clean up resources opened in an initializer or constructor.
  // Finalizers are guaranteed to execute no matter what happens during request processing.
  public void finalize() throws Exception {}
}
```

### 4) Define your websockets (if any).

Lightning includes out-of-the-box support for WebSockets. An example follows:

```java
class MyWebsockets {
  // Provide a factory method that creates an instance of a websocket each time a request
  // is incoming to the given path. WebSocket paths may not use wildcards or parameters,
  // and must return an object that conforms to the Jetty WebSocket API (as demonstrated).
  // If you wish to have a singleton websocket object, simply return the same object every
  // time (but make sure your implementation is stateless). Otherwise, a new object will
  // be created for every new connection. You will notice that you can dependency inject
  // both framework objects (such as `Config` and `MySQLDatabaseProvider`) as well as any
  // custom dependencies you defined before launching the server.
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
    public void message(final Session session, final String message) throws IOException {
      session.getRemote().sendString("THANKS!");
    }

    @OnWebSocketError
    public void error(final Session session, final Throwable error) {}
  }
}
```
Unfortunately, web sockets currently cannot be automatically reloaded in debug mode (due to some limitations in Jetty) so you'll need to restart the server to see changes to websocket handlers.

### 5) Define your custom exception handlers (if any).

You may define handlers that are invoked when a route throws an Exception in order to "recover" from the error (probably by sending the appropriate error page or redirect). Please keep in mind that the framework, for efficiency, does not buffer output. Thus, HTTP headers (and some content) may already have been sent before the exception handler is invoked (for example, if a controller threw an exception mid-execution).

An example of installing an exception handler follows:

```java
class MyExceptionHandlers {
  // Must specify the class of the exception that this handler matches.
  // Exception handlers respect the class hierarchy; thus, registering a handler for Throwable.class will
  // give you a catch-all handler. When matching handlers, the most specific handler is used (for example,
  // NotFoundException would match this handler even if you also installed a catch-all handler for Throwable).
  // You may override the framework default error pages by installing handlers for the exceptions in
  // lightning.http as well as a catch-all handler for Throwable. In this example, installing a handler
  // for lightning.http.NotFoundException allows you to have custom 404 error pages. You'll notice that the
  // thrown exception is injectable in addition to global and request-specific framework objects (such as
  // `Response` and `Config`).
  @ExceptionHandler(NotFoundException.class)
  public static void handleException(Response response, NotFoundException e) throws Exception {
    // If your exception handler throws an exception, the default framework error page will be shown instead.
    response.status(404);
  }
}
```

# Features

### Threading and Context

A thread is allocated from a pool to each incoming request. The thread is allocated only to that request until either (a) your handler exits normally or exceptionally or (b) the async API is utilized.

Inside of a request handler, you may access information about the incoming request by either (a) invoking the static methods on `lightning.server.Context` (which uses `ThreadLocal` under the hood) or (b) dependency-injecting request-specific objects into your controller. Please note that option (a) does not work with async.

`lightning.mvc.HandlerContext` defines objects which encapsulate all request-specific information for a request. The static methods on `lightning.server.Context` delegate to methods on the `ThreadLocal` `HandlerContext`. You can dependency inject `HandlerContext` for use with the async API.

### Dependency Injection

See `lightning.inject.Injector` for an explanation of dependency injection in Lightning.

### SQL Database Access

By default, the framework uses a MySQL database - but using this is entirely optional. To use this, you will need to make sure you configure the `db` property in your config.

Lightning will automatically establish a connection pool to your database; if you are not familiar with connection pooling, you should read up on it!

You can invoke `lightning.server.Context.db()` to obtain an instance of `MySQLDatabase` which will let you access your the database you configured. Please be aware of the following (which may differ from connection pools you have used in the past):
* A database connection is not allocated to your thread from the pool until the first invocation of `db()`
* The database connection will be automatically freed (returned to the pool) when you return from your request handler. Thus, you never need to invoke close() or use try-with-resources, but doing so has no effect. You do, however, still need to close any `NamedPreparedStatement`s, `PreparedStatement`s, and `ResultSet`s that you create.

Lightning provides several extensions on the default `java.sql.*` APIs that make life easier. In particular, we provide `NamedPreparedStatement` which allows you to use named parameters in prepared SQL queries as well as some additional convenience methods. We also provide `MySQLDatabase` which provides some powerful features including easily creating `NamedPreparedStatements`for common operations and re-entrant transactions.

An example of database usage follows:

```java
// Note the usage of Java's try-with-resources to ensure proper freeing of resources.
try (NamedPreparedStatement query = db().prepare("SELECT * FROM users WHERE age >= :age;")) {
  query.setInt("age", 21);
  try (ResultSet result = query.executeQuery()) {
    while (result.next()) {
       String username = result.getString("username");
       doSomethingWith(username);
    }
  }
}
```

You may also be interested in MySQLDatabase's `paginate`, `prepareReplace`, and `prepareInsert` methods. You can also access the underlying `java.sql.Connection` by invoking `getConnection()`. `lightning.db.ResultSets` provides some convenience macros for dealing with `java.sql.ResultSet`s (particularly their handling of null values). `NamedPreparedStatement` has convenience methods for setting values from a map, fetching generated keys, and other common tasks. `SQLNull` provides typed nulls for use with `NamedPreparedStatement.setFromMap`.

An example of transactions follows:

```java
int value = db().transaction(() -> {
  // ... perform your queries here ...

  db().transaction(() -> {
    // You can execute transactions within transactions.
    // Transactions are "re-entrant" on the same database connection.
    // Thus, the queries on this transaction are simply made part of the larger
    // containing transaction and will not be committed until the containing
    // transaction is committed.
  });

  // Whatever is returned from the closure is returned by the invocation
  // of db().transaction(). Returning a value is optional; transaction
  // closures may also be void-returning methods.
  return 1;
});
```

### Non-SQL Database Access

We recommend doing the following:

  1) Define a custom configuration format (subclass `lightning.config.Config`) which includes additional config options for your database system.
  2) Configure custom dependency injection for your custom config class.
  3) Use your config to configure dependency injection for a connection pooler for your database system.
  4) Create an `AbstractController` from which all your controllers will inherit. Provide convenience methods here for accessing your database by injecting the database pooler into an `@Initializer`.

See the [RethinkDB Demo](https://github.com/lightning-framework/examples/tree/master/rethinkdb-demo) for an example.

### Annotation-Based Before Filters

You may specify code to execute before a request by defining a filter and using the `@Filter` annotation. Filters are similar to controllers: a new instance is allocated for each incoming request, used, and then discarded.

```java
// Define a filter.
public class MyFilter implements RouteFilter {
  // Constructor parameters are injectable.
  public DenialOfServiceFilter(Config config) {
    this.config = config;
  }

  // Executes the filter. Calling halt() or throwing an exception will
  // prevent the triggering route from executing.
  @Override
  public void execute() throws Exception {
      halt();
    }
  }
}
```

We require an `@Filter` annotation to be present on each route method using the filter because our design goal was for programmers to be able to look at a route method and see exactly what will happen.

### Path-Based Before Filters

You may specify code snippets to execute before routes on given paths.

```java
import lightning.ann.Before;
import lightning.http.AccessViolationException;
import lightning.http.NotAuthorizedException;

import static lightning.enums.FilterPriority.*;
import static lightning.enums.HTTPMethod.*;
import static lightning.server.Context.*;

public class AccessControlFilters {
  @Before(path="/admin/*", methods={GET, POST}, priority=HIGH)
  public static void doFilter(HandlerContext context) throws Exception {
    if (!user().hasPrivilege(Privilege.ADMIN)) {
      throw new AccessViolationException();
    }
  }

  @Before(path="/admin/*", methods={GET, POST}, priority=HIGHEST)
  @Before(path="/account/*", methods={GET, POST}, priority=HIGHEST)
  public static void doFilter(HandlerContext context) throws Exception {
    if (!auth().isLoggedIn()) {
      throw new NotAuthorizedException();
    }
  }
}
```

For more information on the semantics of path-based filters, see `lightning.ann.Before`.

### Halt

Invoking `lightning.server.Context.halt()` will stop request processing by throwing a `HaltException`. Invoking `halt()` is equivalent to returning from the handler (for handlers that return void or null). Invoking `halt()` in a filter will prevent the route from being invoked at all.

### Request/Response

Invoking `lightning.server.Context.request()` and `lightning.server.Context.response()` will return objects representing the HTTP request and HTTP response. See `lightning.http.Request` and `lightning.http.Response`.

### Understanding Parameter Handling

In Lightning, most methods that return a user-provided string value return an instance of `lightning.mvc.Param` instead of a nullable string or `Optional<String>`. For example, this is utilized with HTTP query parameters and headers. In practice, this provides a much more convenient interface for dealing with user input that also catches almost all data type errors.

For example, consider a query parameter for which you want to accept an integer:

```java
int userValue = queryParam("name").intValue();
```

If the user did not provide an integer, invoking `intValue` throws a `BadRequestException` which will in turn exit the request handler and generate an HTTP 400 error page. This isn't neccesarily the most user-friendly way of handling this error (should be used in combination with a validator for that), but it does ensure that the given value is *always* an integer so that you don't end up accidently storing incorrect data types into your database system.

`Param` provides many more convenience methods, too - check them out! `Param` can make your code more readable when dealing with array input, enums, and checkboxes, too.

A similar variant (`ObjectParam`) exists for reading values from type-unsafe server-side data stores (session properties, user properties, cache properties, etc.) and casting them to the desired types in a fail-safe manner.

```java
// Exception thrown if property does not exist or is not a List<String>.
List<String> data = session().get("data").listValue(String.class);
```

### Cache

TODO: COMING SOON!

** NOTE ** Usage of these APIs is entirely optional. Requires you to have configured a cache driver.

### Validators

No framework could be complete without built-in form validation - and lightning has it, too! The usage of a validator is as follows:

```java
validate("agreement").isChecked();
validate("email").isEmail().isNotEmpty();
validate("year").isNumberBetween(2000, 2100);
validate("file").isFileSmallerThan(1024);
validate("answer").isOneOf("A","B","C","D");

if (validator().passes()) {
  // Make changes to the database.
  saveData(request);
  showSuccessPage();
} else {
  // Show errors to user (map of query param names to associated error(s)).
  Map<String, String> errors = validator.getErrors();
  showErrorPage(errors);
}

```

There are many more validators than those shown in the example - check them out! You can also write your own validators or manually add errors. `lightning.context.validator()` will return an instance of a `lightning.mvc.Validator` for the current request - read the documentation!

### Routes

Routes are indicated by annotating a method in an `@Controller` with `@Route`. Routes are automatically installed by the framework by scanning the annotations - it's that easy. Routing is always fast - O(n) with respect to the string length of the path regardless of the number of routes installed.

For more information on route syntax and what route targets may return, see `lightning.ann.Route`.

### Multipart Requests

To service a multipart request, you must annotate the receiving route with `@Mutlipart`.

```java
@Controller
class FileUploadController {
  @Route(path="/upload", methods={POST})
  @Multipart
  public void handleUpload() throws Exception {
    badRequestIf(request().getPart("file").getSize() == 0);
    InputStream data = request().getPart("file").getInputStream();
    long size = request().getPart("file").getSize();
    String fileName = request().getPart("file").getSubmittedFileName();
    // ... store the file.
  }
}
```

You will probably want to check out the configuration options related to multipart in `lightning.config.Config`.

### Authentication

You may invoke `lightning.server.Context.auth()` to get a request-specific object that can perform authentication, log outs, or give access the authenticated user. In addition, `lightning.server.Context` defines the `isLoggedIn()` and `user()` convenience methods. These methods should be fairly self explanatory; refer to the documentation for `lightning.auth` for more information. Attempting to invoke `user()` when no user is authenticated triggers a NotAuthorizedException which by default generates an HTTP 401 error page.

** NOTE ** Usage of these APIs is entirely optional. Requires you to have configured an auth driver.

### Users/Groups/Privileges

You may invoke `lightning.server.Context.groups()` and `lightning.server.Context.users()` to get objects for interacting with the sets of all groups and users respectively in aggregate.

Once you have obtained a reference to a `lightning.users.User` or `lightning.users.Group`, you may mutate it and invoke save() to push the changes to the database. `User`s may also serve as key-value stores (storing any type of serializable objects), though using your own storage is preferred to prevent potential data races, in addition to having some predefined properties (id, username, email, password, etc.). Passwords are encrypted and salted using BCrypt.

Users and groups may have privileges. Users may be a member of one or more groups. A privilege is simply an integer - you as a programmer must decide its meaning (probably use an enum) and enforce the access constraints it implies in your code (filters and the lightning annotations are good for this).

See `lightning.users` and `lightning.groups` for more details.

** NOTE ** Usage of these APIs is entirely optional. Requires you to have configured a user driver and groups driver.

### Sessions

You may invoke the `lightning.server.Context.session()` method to get an object for reading and setting session data. Sessions are created on-demand and are lazy-loaded. Sessions serve as a key-value store. Keys are strings, and values may be any object that implements `java.io.Serializable`. Sessions expire after a period of inactivity. Sessions also expose an XSRF token.

** NOTE ** Usage of these APIs is entirely optional. Requires you to have configured a session driver.

### Cookies

You may invoke the `lightning.server.Context.cookies()` method to get an API for reading and setting cookies. Please note that our cookie manager will automatically sign the cookie values you set with HMAC SHA256 and verify the signature before reading them back. If the signature does not match, the framework will act as if the cookie was not sent at all. This is an effective way to prevent users form modifying or forging cookies (though keep in mind this does not protect against copying another user's cookies).

If you do not wish to utilize the signed cookies API, you may set and read raw cookies by using the unencrypted cookie methods on `request()` and `response()` directly.

** NOTE ** Using the signed cookies API requires you to set an HMAC key in config.

### Mail

You may invoke the `lightning.server.Context.mail()` method to get an API for sending mail. Usage is as follows:

```java
Message message = mail().createMessage();
message.addRecipient("hello@world.com");
message.setSubject("Hello World!");
message.setHTMLText("Hello World!");
mail().send(message); // Blocks until delivery is successful.
```

If a message fails to send, an exception is thrown.

The API also supports file attachments and other features. See `lightning.mail.Message` for details.

You may use templates to create emails by invoking `lightning.server.Context.renderToString(template, model)` to render a freemarker template to a string and then using `setHTMLText` on the result.

** NOTE ** Requires you to have configured SMTP or the logging driver in `lightning.config.Config`.

### Asynchronous Handlers

For now, you can make asynchronous handlers using the Servlet Async API.

```java
import static lightning.enums.HTTPMethod.*;
import static lightning.server.Context.*;
import lightning.ann.Route;
import lightning.ann.Controller;
import lightning.mvc.HandlerContext;

@Controller
class MyController {
  @Route(path="/", methods={GET})
  public void handle() throws Exception {
    // Inform the context you wish to go async and get an object that holds lightning's request-specific context.
    HandlerContext context = goAsync();

    // You may now invoke the servlet async API.
    if (context.request().raw().isAsyncSupported()) {
      final AsyncContext asyncContext = request.startAsync();

      scheduleAsyncCallback(() -> {
        // ... DO WHATEVER TASKS YOU NEED TO DO ...

        // IMPORTANT: MUST CLOSE CONTEXT TO AVOID MEMORY LEAK!
        context.close(); // Close lightning's context first.
        asyncContext.complete();
      });
    }
  }
}
```

### Configuring Drivers

You may need to manually configure drivers for Sessions, Groups, Users, Auth, and the Cache.

Lightning ships with the following drivers:
  * Cache: NONE
  * Sessions: MySQL
  * Groups: MySQL
  * Users: MySQL
  * Auth: MySQL

By default, Lightning will attempt to utilize the MySQL drivers if you have configured a `db` in `Config`. You will need to manually import the schema (contained in `src/main/resources/lightning/schema`) into your database.

TODO: MORE FLEXIBLE OPTIONS COMING SOON!

### Debug Mode

Lightning features a powerful debug mode. Debug mode can be enabled by setting `lightning.config.Config`'s `enableDebugMode` property.

To use debug mode, you must set your working directory the project root (the folder containing Maven's `pom.xml` and `src/main/java`) and follow the Maven directory structure conventions. You cannot use debug mode when deployed to a JAR.

You should ONLY use debug mode for development. Do not leave it on in production as it exposes system internals.

Enabling debug mode will...

  * Enable automatic reloading of handlers, routes, filters, etc.
  * Enable exception stack traces in the browser (in production a generic 500 Internal Server Error page is shown)
  * Enable template errors in the browser
  * Disable all HTTP caching
  * Force reloads of static files and templates from disk each request

An example in-browser stack trace (errors are also shown in console):

![Debug Page](https://cloud.githubusercontent.com/assets/3498024/14005744/3fa323ba-f134-11e5-9f72-00da49a46ab7.png "Debug Page")

### Examples

You can find examples of Lightning applications in our [Lightning Examples](https://github.com/lightning-framework/examples) repo.
