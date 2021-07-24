package lightning.util.serve;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import com.google.common.collect.ImmutableList;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import lightning.config.Config;
import lightning.flags.Flag;
import lightning.flags.FlagSpec;
import lightning.server.LightningServer;

/**
 * A simple class that starts an HTTP server for serving static files from a directory.
 * For usage instructions, run 'SimpleHTTPServer --help'.
 */
public class SimpleHTTPServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleHTTPServer.class);
  private static boolean isMainClass = false;

  public static boolean isMainClass() {
    return isMainClass;
  }

  @FlagSpec(
      description="Runs a HTTP server that serves static files from a directory.\n" +
                  "e.g. SimpleHTTPServer --port 8080 --path /path/to/serve --optimal."
  )
  private static class Options {
    @Flag(
        names = {"directory-indexes"},
        description="Enables directory indexes."
    )
    public static boolean directoryIndexes = false;

    @Flag(
        names = {"port"},
        description="Specifies TCP port on which to listen."
    )
    public static int port = 8080;

    @Flag(
        names = {"path"},
        description="Specifies directory from which to serve files."
    )
    public static String path = "./";

    @Flag(
        names = {"optimal"},
        description="Enables HTTP and memory caching of static files."
    )
    public static boolean optimal = false;

    @Flag(
        names = {"http2"},
        description="Enables HTTP2 support (must be using SSL)."
    )
    public static boolean http2 = false;

    @Flag(
        names = {"http2c"},
        description="Enables HTTP2C support (must not be using SSL)."
    )
    public static boolean http2c = false;

    @Flag(
        names = {"keystore"},
        description="Enables SSL support using given keystore which contains server certificate and private key."
    )
    public static String keystore = null;

    @Flag(
        names = {"keystore-password"},
        description="Specifies keystore password (requires keystore)."
    )
    public static String keystorePassword = null;

    @Flag(
        names = {"key-manager-password"},
        description="Specifies key manager password."
    )
    public static String keyManagerPassword = null;

    @Flag(
        names = {"truststore"},
        description="Specifies truststore."
    )
    public static String truststore = null;

    @Flag(
        names = {"truststore-password"},
        description="Specifies truststore password (requires truststore)."
    )
    public static String truststorePassword = null;
  }

  public static void main(String[] args) throws Exception {
    setUpLogging();
    isMainClass = true;
    args = lightning.flags.Flags.parse(Options.class, System.out, args);

    File root = new File(Options.path);

    if (!root.exists() || !root.isDirectory() || !root.canRead()) {
      throw new IOException(String.format("Unable to read directory '%s'.", root.getAbsolutePath()));
    }

    Config config = new Config();
    config.enableDebugMode = !Options.optimal;
    config.debugRouteMapPath = null;
    if (Options.directoryIndexes) {
      config.scanPrefixes = ImmutableList.of("lightning.util.serve");
    } else {
      config.scanPrefixes = ImmutableList.of();
    }
    config.server.enableHttp2 = Options.http2;
    config.server.enableHttp2C = Options.http2c;
    config.server.hmacKey = UUID.randomUUID().toString();
    config.server.port = Options.port;
    config.server.staticFilesPath = root.getAbsolutePath();
    config.server.staticFilesOutsideClasspath = true;
    config.ssl.port = Options.port;
    config.ssl.redirectInsecureRequests = false;
    config.ssl.keyManagerPassword = Options.keyManagerPassword;
    config.ssl.keyStoreFile = Options.keystore;
    config.ssl.keyStorePassword = Options.keystorePassword;
    config.ssl.trustStoreFile = Options.truststore;
    config.ssl.trustStorePassword = Options.truststorePassword;

    LOGGER.info("Serving From Directory: {}", config.server.staticFilesPath);

    LightningServer server = new LightningServer(config);
    server.start();
    server.join();
  }

  private static void setUpLogging() throws Exception {
    StaticLoggerBinder loggerBinder = StaticLoggerBinder.getSingleton();
    LoggerContext loggerContext = (LoggerContext) loggerBinder.getLoggerFactory();
    loggerContext.reset();
    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(loggerContext);
    configurator.doConfigure(SimpleHTTPServer.class.getResourceAsStream("/lightning/logback.xml"));
  }
}
