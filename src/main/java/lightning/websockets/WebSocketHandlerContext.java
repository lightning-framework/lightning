package lightning.websockets;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lightning.cache.Cache;
import lightning.config.Config;
import lightning.crypt.Hasher;
import lightning.crypt.TokenSets;
import lightning.db.MySQLDatabase;
import lightning.db.MySQLDatabaseProxy;
import lightning.enums.JsonFieldNamingPolicy;
import lightning.exceptions.LightningException;
import lightning.groups.Groups;
import lightning.http.NotAuthorizedException;
import lightning.inject.Injector;
import lightning.inject.InjectorModule;
import lightning.json.JsonService;
import lightning.mail.Mailer;
import lightning.mvc.HandlerContext;
import lightning.mvc.URLGenerator;
import lightning.templates.TemplateEngine;
import lightning.users.User;
import lightning.users.Users;

/**
 * TODO: Add API for sending with write callbacks?
 * TODO: Add API for sending json?
 * TODO: Add API for authentication?
 */
public class WebSocketHandlerContext {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketHandlerContext.class);
  private final MySQLDatabaseProxy db;
  private final JsonService json;
  private final TemplateEngine templates;
  private final Config config;
  private final Mailer mail;
  private final URLGenerator url;
  private final Cache cache;
  private final Injector injector;
  private final InjectorModule bindings;
  private Session session;
  private long userId;
  private User user;

  public WebSocketHandlerContext(HandlerContext context) throws Exception {
    db = new MySQLDatabaseProxy(context.dbPool());
    url = URLGenerator.forRequest(context.request());
    config = context.config();
    templates = context.templates();
    json = context.json();
    mail = config.mail.isEnabled() ? context.mail() : null;
    cache = context.cache();
    bindings = new InjectorModule();
    bindings.bindClassToInstance(WebSocketHandlerContext.class, this);
    injector = new Injector(context.globalBindings, context.userBindings, bindings);
  }

  public Groups groups() {
    // TODO
    throw new UnsupportedOperationException();
  }

  public Users users() {
    // TODO
    throw new UnsupportedOperationException();
  }

  public InetSocketAddress remoteAddress() {
    return session.getRemoteAddress();
  }

  public long idleTimeoutMs() {
    return session.getIdleTimeout();
  }

  public void setIdleTimeoutMs(long value) {
    session.setIdleTimeout(value);
  }

  public void sendBinary(ByteBuffer buffer) throws IOException {
    session.getRemote().sendBytes(buffer);
  }

  public void sendBinary(byte[] data, int offset, int length) throws IOException {
    sendBinary(ByteBuffer.wrap(data, offset, length));
  }

  public void sendBinary(byte[] data) throws IOException {
    sendBinary(data, 0, data.length);
  }

  public void sendBinary(InputStream stream) throws IOException {
    sendBinary(IOUtils.toByteArray(stream));
  }

  public void sendBinaryFragment(ByteBuffer buffer, boolean isLastFragment) throws IOException {
    session.getRemote().sendPartialBytes(buffer, isLastFragment);
  }

  public void sendBinaryFragment(byte[] data, int offset, int length, boolean isLastFragment) throws IOException {
    sendBinaryFragment(ByteBuffer.wrap(data, offset, length), isLastFragment);
  }

  public void sendBinaryFragment(byte[] data, boolean isLastFragment) throws IOException {
    sendBinaryFragment(data, 0, data.length, isLastFragment);
  }

  public void sendBinaryFragment(InputStream stream, boolean isLastFragment) throws IOException {
    sendBinaryFragment(IOUtils.toByteArray(stream), isLastFragment);
  }

  public Future<Void> sendBinaryAsync(ByteBuffer buffer) {
    return session.getRemote().sendBytesByFuture(buffer);
  }

  public Future<Void> sendBinaryAsync(byte[] data, int offset, int length) {
    return sendBinaryAsync(ByteBuffer.wrap(data, offset, length));
  }

  public Future<Void> sendBinaryAsync(byte[] data) {
    return sendBinaryAsync(data, 0, data.length);
  }

  public Future<Void> sendBinaryAsync(InputStream stream) throws IOException {
    return sendBinaryAsync(IOUtils.toByteArray(stream));
  }

  public void sendText(String message) throws IOException {
    session.getRemote().sendString(message);
  }

  public void sendTextFragment(String text, boolean isLastFragment) throws IOException {
    session.getRemote().sendPartialString(text, isLastFragment);
  }

  public Future<Void> sendTextAsync(String message) {
    return session.getRemote().sendStringByFuture(message);
  }

  public void disconnect() throws IOException {
    session.disconnect();
  }

  public void close() {
    if (session != null) {
      session.close();
    }
  }

  public void close(int status) {
    if (session != null) {
      session.close(status, null);
    }
  }

  public void close(int status, String reason) {
    if (session != null) {
      session.close(status, reason);
    }
  }

  public boolean isOpen() {
    return session != null && session.isOpen();
  }

  public boolean isSecure() {
    return session != null && session.isSecure();
  }

  /**
   * Prefer using the APIs provided by Lightning.
   * Only use this if you really know what you are doing.
   * No guarantees are made that this API will remain available (e.g. if we stop using Jetty).
   */
  @Deprecated
  public Session _session() {
    if (session == null) {
      throw new IllegalStateException();
    }

    return session;
  }

  /**
   * Prefer using the APIs provided by Lightning.
   * Only use this if you really know what you are doing.
   * No guarantees are made that this API will remain available (e.g. if we stop using Jetty).
   */
  @Deprecated
  public RemoteEndpoint _remote() {
    return _session().getRemote();
  }

  public Cache cache() {
    return cache;
  }

  public final String renderToString(String viewName, Object viewModel) throws Exception {
    StringWriter stringWriter = new StringWriter();
    templates.render(viewName, viewModel, stringWriter);
    return stringWriter.toString();
  }

  public final <T> T parseJson(String json, Class<T> clazz) throws Exception {
    return parseJson(json, clazz, JsonFieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
  }

  public final <T> T parseJson(String json, Class<T> clazz, JsonFieldNamingPolicy policy) throws Exception {
    return this.json.readJson(clazz, IOUtils.toInputStream(json, "UTF-8"), policy);
  }

  public String toJson(Object object) throws Exception {
    return toJson(object, JsonFieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
  }

  public String toJson(Object object, JsonFieldNamingPolicy policy) throws Exception {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    json.writeJson(object, stream, policy);
    return stream.toString("UTF-8");
  }

  public URLGenerator url() {
    return url;
  }

  public Mailer mail() throws Exception {
    if (mail == null) {
      throw new LightningException("Mail is not configured.");
    }

    return mail;
  }

  public TemplateEngine templates() {
    return templates;
  }

  public JsonService json() {
    return json;
  }

  public MySQLDatabase db() {
    return db;
  }

  /**
   * @return The user that was authenticated when the web socket connection was established.
   * @throws NotAuthorizedException If no logged in user.
   * @throws Exception On internal error.
   */
  public User user() throws Exception {
    if (!isLoggedIn()) {
      throw new NotAuthorizedException();
    }

    if (user == null) {
      user = users().getById(userId);
    }

    return user;
  }

  public boolean isLoggedIn() {
    return userId > 0;
  }

  public Config config() {
    return config;
  }

  public Hasher hasher() {
    return new Hasher(config().server.hmacKey);
  }

  public TokenSets tokens() {
    return new TokenSets(hasher());
  }

  Injector injector() {
    return injector;
  }

  void setSession(Session session) {
    this.session = session;
  }

  boolean hasSession() {
    return session != null;
  }

  void clearSession() {
    session = null;
  }

  void install() {
    WebSocketContext.setContext(this);
  }

  void uninstall() {
    WebSocketContext.clearContext();

    try {
      db.reallyClose();
    } catch (SQLException e) {
      LOGGER.warn("Failed to close database connection: ", e);
    }

    try {
      if (user != null) {
        user.save();
      }
    } catch (Exception e) {
      LOGGER.warn("Failed to save user: ", e);
    } finally {
      user = null;
    }
  }
}
