package lightning.config;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lightning.ann.Optional;
import lightning.ann.Required;
import lightning.exceptions.LightningException;
import lightning.mail.MailerConfig;
import lightning.util.Iterables;
import lightning.util.SimpleHTTPServer;

import com.google.common.collect.ImmutableList;

/**
 * Provides configuration options for most areas of Lightning.
 * See the official web documentation for more information.
 */
public class Config {
  public Config() {
    isRunningFromJar = Config.class.getResource("Config.class").toString().startsWith("jar:") ||
        Config.class.getResource("Config.class").toString().startsWith("rsrc:") ||
        Config.class.getProtectionDomain().getCodeSource().getLocation().getPath().contains(".jar");
  }

  /**
   * Specifies whether or not debug (development) mode should be enabled.
   * Recommended during development.
   *
   * Assumes the current working directory is set to the root of your project folder (where pom.xml is) and
   * that you are following the Maven directory structure conventions.
   *
   * WARNING:
   *   DO NOT ENABLE DEBUG MODE IN PRODUCTION! IT MAY EXPOSE SYSTEM INTERNALS!
   *   DEBUG MODE WILL NOT WORK WHEN DEPLOYED TO A JAR!
   *
   * NOTE:
   *   Debug Mode enables automatic code reloading without restarting the server, adds in-browser exception
   *   stack traces and template errors, disables caching of static files and template files, disables HTTP
   *   caching of static files, etc.
   */
  public @Optional boolean enableDebugMode = false;

  /**
   * If enableDebugMode, specifies the path on which a debug page that displays routes will be installed.
   * If set to null, the route map handler will not be installed.
   */
  public @Optional String debugRouteMapPath = "/~lightning/routes";

  /**
   * The root path of your project (the directory containing pom.xml).
   */
  public @Optional String projectRootPath = "./";

  /**
   * Specifies a path on which a simple health status page will be installed for use by load balancers and
   * other infrastructure.
   * If set to null, the handler will not be installed.
   */
  public @Optional String healthPath = null;

  /**
   * Specifies a list of Java package prefixes in which code can be safely reloaded on each incoming request.
   * Code reloading will ONLY OCCUR WHEN DEBUG MODE IS ENABLED.
   *
   * WARNING:
   *   Keep in mind that NOT ALL CODE CAN BE SAFELY RELOADED. If you incorrectly configure this option, you may
   *   experience some strange or undefined behavior when running the server.
   *   Keep in mind that you MAY NOT dependency inject any classes which are located within these prefixes.
   *   Keep in mind that you MAY NOT dependency inject classes with static state.
   */
  public @Optional List<String> autoReloadPrefixes = ImmutableList.of();

  /**
   * Specifies a list of Java package prefixes in which to search for routes, web sockets, exception handlers.
   * Example: ImmutableList.of("path.to.my.app")
   */
  public @Required List<String> scanPrefixes;

  /**
   * Specifies a list of paths in which to search for source code files to display in the debug screen when
   * operating in debug mode. Absolute OR relative to project root.
   */
  public @Optional List<String> codeSearchPaths = ImmutableList.of("./src/main/java", "./src/test/java");

  // TODO: Add options for Sessions and Auth.

  /**
   * Provides options for enabling SSL with the built-in server.
   */
  public @Required SSLConfig ssl = new SSLConfig();
  public static final class SSLConfig {
    /************************************
     * Java Key Store Options
     ************************************
     *
     * SSL will be enabled if you provide a keystore file and keystore password.
     * The specified keystore (.jks) should contain the server's SSL certificate.
     * You may wish to read more Java Key Store (JKS) format.
     */

    public @Optional String keyStoreFile;       // Required to enable SSL.
    public @Optional String keyStorePassword;   // Required to enable SSL.
    public @Optional String trustStoreFile;     // Optional.
    public @Optional String trustStorePassword; // Optional.
    public @Optional String keyManagerPassword; // Optional.

    /************************************
     * Server Options
     ***********************************/

    /**
     * Whether to redirect HTTP requests to their HTTPS equivalents.
     * If false, will only install an HTTPs server (on ssl.port).
     * If true, will also install an HTTP server (on server.port) that redirects insecure requests.
     */
    public @Optional boolean redirectInsecureRequests = true;

    /**
     * Specifies the port on which to listen for HTTPS connections.
     */
    public @Optional int port = 443;

    public boolean isEnabled() {
      return keyStorePassword != null && keyStoreFile != null;
    }
  }

  /**
   * Provides options for configuring the built-in HTTP server.
   *
   * NOTE:
   *   For custom HTTP error pages, see the documentation for lightning.ann.ExceptionHandler.
   */
  public @Required ServerConfig server = new ServerConfig();
  public static final class ServerConfig {
    /**
     * Enables server-side HTTP/2 support over SSL via ALPN.
     * Clients that do not support HTTP/2 will fall back to HTTP/1.1 or HTTP/1.0.
     *
     * IMPORTANT:
     *   Due to changes to SSL protocol negotiation in HTTP/2 (via ALPN), you must replace your JDK's implementation of
     *   SSL with an implementation that adds support for ALPN (Application-Layer Protocol Negotiation).
     *
     *   This is MANDATORY for enabling HTTP/2 with JRE <= 8. JRE 9+ should have built-in support for ALPN.
     *
     *   You may add ALPN support by placing the ALPN library in your JVM boot path as follows:
     *   1) Determine the SPECIFIC VERSION of ALPN required for YOUR JVM VERSION.
     *      The ALPN version MUST MATCH the version of your JVM.
     *      SEE: https://www.eclipse.org/jetty/documentation/current/alpn-chapter.html#alpn-versions
     *   2) Download the SPECIFIC VERSION of ALPN for YOUR JVM version as a JAR.
     *      SEE: http://mvnrepository.com/artifact/org.mortbay.jetty.alpn/alpn-boot
     *   3) You must launch your JVM with the following command line arguments to add ALPN to your JVM boot path:
     *      -Xbootclasspath/p:/path/to/alpn-boot-${alpn-version}.jar
     */
    public @Optional boolean enableHttp2 = false;

    /**
     * Enables server-side HTTP/2 clear text upgrade support (H2C) for un-encrypted connections.
     * Clients that do not support H2C will fall back to HTTP/1.1 or HTTP/1.0.
     *
     * NOTE: No browsers currently implement H2C, so enabling this is somewhat pointless unless you
     *       expect API clients that will utilize H2C.
     */
    public @Optional boolean enableHttp2C = false;

    /**
     * The initial flow control window size for a new stream (default 65535).
     * Larger values may allow greater throughput but also risk head of line
     * blocking if TCP/IP flow control is triggered.
     */
    public @Optional int http2InitialStreamSendWindowBytes = 65535;

    /**
     * The maximum number of concurrently open streams allowed on a single HTTP/2 connection (default 1024).
     * Larger values increase parallelism but cost a memory commitment.
     */
    public @Optional int http2MaxConcurrentStreams = 1024;

    /**
     * Sets the private encryption key used for verifying the integrity of hashes
     * generated by the server (for example, cookie signatures).
     *
     * NOTE:
     *   Must be something long, random, secret, and unique to your app.
     *   In clustered environments, all app servers must use the same key.
     *   It is safe to change this key, but any outstanding cookies will be invalidated.
     */
    public @Required String hmacKey;

    /**
     * Sets the port on which the server will listen for incoming HTTP connections.
     */
    public @Optional int port = 80;

    /**
     * Sets the minimum size of the server request processing thread pool.
     */
    public @Optional int minThreads = 40;

    /**
     * Sets the maximum size of the server request processing thread pool.
     */
    public @Optional int maxThreads = 250;

    /**
     * Sets the maximum amount of time that server thread may be idle before
     * being removed from the server request processing thread pool.
     */
    public @Optional int threadTimeoutMs = (int) TimeUnit.SECONDS.toMillis(60);

    /**
     * Sets the maximum amount of time that a websocket connection may be idle
     * before the server forcibly closes the connection.
     */
    public @Optional int websocketTimeoutMs = (int) TimeUnit.SECONDS.toMillis(60);

    /**
     * Sets the maximum binary message siez that the server will accept over websocket
     * connections.
     */
    public @Optional int websocketMaxBinaryMessageSizeBytes = 65535;

    /**
     * Sets the maximum text message size that the server will accept over websocket
     * connections.
     */
    public @Optional int websocketMaxTextMessageSizeBytes = 65535;

    /**
     * Whether or not to enable message compression (via permessage-deflate/deflate-frame)
     * on websocket connections. Will save server memory per connection at the cost of
     * additional bandwidth usage.
     * NOTE: For high scalability, this should probably be disabled. Jetty doesn't expose
     * APIs to control the negotiation of extension parameters. Therefore, it's possible
     * for each websocket connection to reserve 64kb of server-side memory for the duration
     * of the connection if enabled.
     */
    public @Optional boolean websocketEnableCompression = true;

    /**
     * Sets the default timeout of async write operations on websocket connections.
     */
    public @Optional long websocketAsyncWriteTimeoutMs = TimeUnit.SECONDS.toMillis(60);

    /**
     * Sets the maximum size of an incoming non-multipart request.
     * Requests in excess of this limit will be dropped.
     */
    public @Optional int maxPostBytes = 1024 * 1024 * 20; // 20 MB

    /**
     * Sets the maximum number of query parameters that a request may contain.
     * Requests in excess of this limit will be dropped.
     */
    public @Optional int maxQueryParams = 100; // In number of params.

    /**
     * Sets the maximum amount of time that an HTTP connection may be idle before the
     * server forcibly closes it.
     */
    public @Optional int connectionIdleTimeoutMs = (int) TimeUnit.MINUTES.toMillis(3);

    /**
     * Specifies the path containing static files that should be served.
     * This path is relative to ${project}/src/main/resources.
     *
     * Static files will be served on their path relative to their location within the specified folder.
     * For example, "style.css" in the specified folder will be served on "/style.css".
     *
     * Static files are served optimally (memory caching, HTTP caching) and support the entire HTTP spec,
     * including range requests.
     *
     * Static files will be served gzipped if the client supports it and a pre-gzipped version of the file
     * exists (e.g. styles.css.gz).
     */
    public @Optional String staticFilesPath;

    /**
     * Specifies the path in which template files are located.
     * This path is relative to ${project}/src/main/resources.
     */
    public @Optional String templateFilesPath;

    /**
     * Sets the host on which the server should listen.
     * NOTE: "0.0.0.0" matches any host.
     */
    public @Optional String host = "0.0.0.0";

    /**
     * Whether or not to trust load balancer headers (X-Forwarded-For, X-Forwarded-Proto).
     *
     * NOTE:
     *   Enabling this option will cause Lightning APIs (e.g. request.scheme() and request.method())
     *   to utilize this header information. This option should be enabled only if your app servers
     *   are safely firewalled behind a load balancer (such as Amazon ELB).
     */
    public @Optional boolean trustLoadBalancerHeaders = false;

    /**
     * Sets the maximum size at which an individual static file may be RAM-cached.
     */
    public @Optional int maxCachedStaticFileSizeBytes = 1024 * 50; // 50KB

    /**
     * Sets the maximum size of the RAM-cache for static files.
     */
    public @Optional int maxStaticFileCacheSizeBytes = 1024 * 1024 * 30; // 30MB

    /**
     * Sets the maximum number of static files that may be RAM-cached.
     */
    public @Optional int maxCachedStaticFiles = 500;

    /**
     * Whether or not to enable server support for HTTP multipart requests.
     * If not enabled, multipart requests will be dropped.
     */
    public @Optional boolean multipartEnabled = true;

    /**
     * A temporary directory in which multipart pieces that exceed the flush size may
     * be written to disk in order to avoid consuming significant RAM.
     */
    public @Optional String multipartSaveLocation = System.getProperty("java.io.tmpdir");

    /**
     * Size at which a multipart piece will be flushed to disk.
     * Pieces less than this size will be stored in RAM.
     */
    public @Optional int multipartFlushSizeBytes = 1024 * 1024 * 1; // 1 MB

    /**
     * Maximum allowed size of a individual multipart piece.
     * Requests containing pieces larger than this size will be dropped.
     * NOTE: This will limit the maximum file upload size.
     */
    public @Optional int multipartPieceLimitBytes = 1024 * 1024 * 100; // 100 MB

    /**
     * Maximum allowed size of an entire multipart request.
     * Requests in excess of this size will be dropped.
     * NOTE: This will limit the maximum file upload size.
     */
    public @Optional int multipartRequestLimitBytes = 1024 * 1024 * 100; // 100 MB

    /**
     * Sets the maximum number of queued requests.
     * Requests past this limit will be dropped.
     */
    public @Optional int maxQueuedRequests = 10000;

    /**
     * Sets the maximum number of queued unaccepted connections.
     * Connection attempts past this limit will be dropped.
     */
    public @Optional int maxAcceptQueueSize = 0;

    /**
     * Whether or not to enable server support for HTTP persistent connections.
     */
    public @Optional boolean enablePersistentConnections = true;

    public @Optional int inputBufferSizeBytes = 8192;
    public @Optional int outputAggregationSizeBytes = 8192;
    public @Optional int outputBufferSizeBytes = 8192;
    public @Optional int maxRequestHeaderSizeBytes = 2048;
    public @Optional int maxResponseHeaderSizeBytes = 2048;

    /**
     * Whether or not to enable output buffering. If you enable output buffering, all
     * writes to HTTP responses in filters, controllers, and exception handlers will
     * be buffered if they content type matches an output buffering type (set below).
     * The response will not be committed (the first byte will not be written to the
     * underlying socket) until control returns from the last handler executed for a
     * given request (or, for async handlers, when the async context is closed).
     *
     * PROS: Response is not committed when you write the first byte and can therefore
     *       be mutated after you have written to it. Error/debug pages will render
     *       cleanly if error is thrown after some output has been written since it's
     *       possible to reset the response and avoid mangling the output of the error
     *       page with the output already written.
     * CONS: Increased memory usage and decreased performance (extra buffer copy).
     *
     * Our recommendation:
     *   - Enable this when you are developing/debugging with debug mode for cleaner error pages
     *   - Disable this in production for increased performance/scalability
     */
    public @Optional boolean enableOutputBuffering = false;

    /**
     * A list of MIME types for which responses should be buffered (if enableOutputBuffering).
     * This setting only affects output written by registered controllers, filters, exception handlers.
     * We recommend keeping the default setting (only buffer HTML pages).
     * NOTE: You may use wildcards on either component (e.g. 'text/*').
     */
    public @Optional List<String> outputBufferingTypes = ImmutableList.of("text/html");

    /**
     * The maximum amount of output that will be buffered (if enableOutputBuffering). Once this
     * limit is reached, the output buffer will start automatically flushing to the underlying
     * socket. You may set to -1 to have no limit (but be careful).
     */
    public @Optional int outputBufferingLimitBytes = 1024 * 64;
  }

  /**
   * Provides options for configuring the sending of emails over SMTP.
   */
  public @Required MailConfig mail = new MailConfig();
  public static final class MailConfig implements MailerConfig {
    /**
     * Forces messages to be logged (via the SLF4J facade) instead of attempting
     * to deliver them over the network. Useful for development.
     */
    public @Optional boolean useLogDriver = false;

    /**
     * Whether or not to use SMTP+SSL.
     */
    public @Optional boolean useSsl = true;

    /**
     * The email address from which the server will deliver messages.
     */
    public @Optional String address;

    /**
     * The username required to authenticate with the SMTP server.
     */
    public @Optional String username;

    /**
     * The password required to authenticate with the SMTP server.
     * Set to NULL if no password is required.
     */
    public @Optional String password;

    /**
     * The host of the SMTP server.
     */
    public @Optional String host;

    /**
     * The port of the SMTP server.
     */
    public @Optional int port = 465;

    public boolean isEnabled() {
      return useLogDriver ||  (host != null && username != null);
    }

    @Override
    public boolean useLogDriver() {
      return useLogDriver;
    }

    @Override
    public String getAddress() {
      return address;
    }

    @Override
    public int getPort() {
      return port;
    }

    @Override
    public boolean useSSL() {
      return useSsl;
    }

    @Override
    public String getHost() {
      return host;
    }

    @Override
    public String getUsername() {
      return username;
    }

    @Override
    public String getPassword() {
      return password;
    }
  }

  /**
   * Provides options for configuring a connection pool to an SQL database.
   */
  public @Required DBConfig db = new DBConfig();
  public static final class DBConfig {
    /*********************************************
     * Database Information
     *********************************************/

    public @Optional String host = "localhost";
    public @Optional int port = 3306;
    public @Optional String username = "httpd";

    /**
     * NOTE: Leave NULL if no password is required (not recommended).
     */
    public @Optional String password = "httpd";

    /**
     * The name of the default database to use.
     */
    public @Optional String name;

    /**
     * Whether to connect to the database using SSL.
     *
     * NOTE:
     *   The server certificate must be trusted by your JVM by default or explicitly placed
     *   in your JVM trust store.
     */
    public @Optional boolean useSsl = false;

    /*********************************************
     * Connection Pool Options
     *********************************************/

    public @Optional int minPoolSize = 5;
    public @Optional int maxPoolSize = 100;
    public @Optional int acquireIncrement = 5;
    public @Optional int acquireRetryAttempts = 3;
    public @Optional int acquireRetryDelayMs = 1000;
    public @Optional boolean autoCommitOnClose = true;
    public @Optional int maxConnectionAgeS = 50000;
    public @Optional int maxIdleTimeS = 50000;
    public @Optional int maxIdleTimeExcessConnectionsS = 50000;
    public @Optional int unreturnedConnectionTimeoutS = 50000;
    public @Optional int idleConnectionTestPeriodS = 600;
    public @Optional int maxStatementsCached = 500;
    public @Optional int acquireTimeoutMs = -1;

    public boolean isEnabled() {
      return name != null && !name.isEmpty();
    }
  }

  private static void notNull(String key, Object value) throws LightningException {
    if (value == null) {
      throw new LightningException("Your configuration is invalid: You must set a value for config." + key + ".");
    }
  }

  private static void badIf(boolean condition, String message) throws LightningException {
    if (condition) {
      throw new LightningException("Your configuration is invalid: " + message);
    }
  }

  private final boolean isRunningFromJar;

  public boolean isRunningFromJAR() {
    return isRunningFromJar;
  }

  public String resolveProjectPath(String path) {
    if (Paths.get(path).isAbsolute()) {
      return path;
    }

    return Paths.get(projectRootPath, path).toAbsolutePath().toString();
  }

  public String resolveProjectPath(String part1, String part2) {
    return Paths.get(projectRootPath, part1, part2).toAbsolutePath().toString();
  }

  public void validate() throws LightningException {
    notNull("projectRootPath", projectRootPath);
    notNull("scanPrefixes", scanPrefixes);
    notNull("server.hmacKey", server.hmacKey);
    badIf(server.templateFilesPath != null && Paths.get(server.templateFilesPath).isAbsolute(), "templateFilesPath must not be absolute.");
    badIf(server.staticFilesPath != null && Paths.get(server.staticFilesPath).isAbsolute() && !SimpleHTTPServer.isMainClass(), "staticFilesPath must not be absolute.");
    badIf(server.enableHttp2 && !ssl.isEnabled(), "You must enable SSL to enable HTTP2.");
    badIf(autoReloadPrefixes != null &&
          Iterables.reduce(Iterables.map(autoReloadPrefixes,
                                         /*
                                          * Try to protect against some mistakes... this is
                                          * by no means a comprehensive list.
                                          */
                                         i -> (i.startsWith("java.") ||
                                               i.startsWith("lightning.") ||
                                               i.startsWith("com.augustl.pathtravelagent."))),
                           false,
                           (a, i) -> a || i),
          "You may not reload packages java.*, lightning.*, com.augustl.pathtravelagent.*.");

    if (enableDebugMode && !isRunningFromJAR()) {
      File root = new File(projectRootPath);
      if (!root.exists() || !root.isDirectory() || !root.canRead()) {
        throw new LightningException("Your configuration is invalid: projectRootPath must exist, be directory, be readable when debug mode is enabled.");
      }
      File pom = new File(root, "pom.xml");
      if (!pom.exists() || !pom.canRead()) {
        throw new LightningException("Your configuration is invalid: projectRootPath must be a Maven project directory.");
      }
    }
  }

  public boolean canReloadClass(Class<?> type) {
    if (!enableDebugMode || isRunningFromJAR()) {
      return false;
    }

    for (String prefix : autoReloadPrefixes) {
      if (type.getCanonicalName().startsWith(prefix)) {
        return true;
      }
    }

    return false;
  }
}
