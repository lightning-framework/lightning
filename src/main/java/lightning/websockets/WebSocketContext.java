package lightning.websockets;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.concurrent.Future;

import lightning.cache.Cache;
import lightning.config.Config;
import lightning.crypt.Hasher;
import lightning.crypt.TokenSets;
import lightning.db.MySQLDatabase;
import lightning.enums.JsonFieldNamingPolicy;
import lightning.groups.Groups;
import lightning.json.JsonService;
import lightning.mail.Mailer;
import lightning.mvc.URLGenerator;
import lightning.templates.TemplateEngine;
import lightning.users.User;
import lightning.users.Users;

/**
 * Provides convenience methods within a web socket event handler.
 * These methods may only be accessed in @OnEvent handlers for WEBSOCKET_* events.
 */
public class WebSocketContext {
  private static final ThreadLocal<WebSocketHandlerContext> context = new ThreadLocal<>();

  static final void setContext(WebSocketHandlerContext context) {
    WebSocketContext.context.set(context);
  }

  static final void clearContext() {
    context.remove();
  }

  public static final JsonService json() {
    return context.get().json();
  }

  public static final Hasher hasher() {
    return context.get().hasher();
  }

  public static final TokenSets tokens() {
    return context.get().tokens();
  }

  public static final String toJson(Object object) throws Exception {
    return context.get().toJson(object);
  }

  public static final String toJson(Object object, JsonFieldNamingPolicy policy) throws Exception {
    return context.get().toJson(object, policy);
  }

  public static final <T> T parseJson(String json, Class<T> clazz) throws Exception {
    return context.get().parseJson(json, clazz);
  }

  public static final <T> T parseJson(String json, Class<T> clazz, JsonFieldNamingPolicy policy) throws Exception {
    return context.get().parseJson(json, clazz, policy);
  }

  public static final String renderToString(String viewName, Object viewModel) throws Exception {
    return context.get().renderToString(viewName, viewModel);
  }

  public static final boolean isLoggedIn() throws Exception {
    return context.get().isLoggedIn();
  }

  public static final User user() throws Exception {
    return context.get().user();
  }

  public static final Mailer mail() throws Exception {
    return context.get().mail();
  }

  public static final MySQLDatabase db() throws SQLException {
    return context.get().db();
  }

  public static final URLGenerator url() {
    return context.get().url();
  }

  public static final Config config() {
    return context.get().config();
  }

  public static final TemplateEngine templates() {
    return context.get().templates();
  }

  public static final Cache cache() {
    return context.get().cache();
  }

  public static final Groups groups() {
    return context.get().groups();
  }

  public static final Users users() {
    return context.get().users();
  }

  public static final InetSocketAddress remoteAddress() {
    return context.get().remoteAddress();
  }

  public static final long idleTimeoutMs() {
    return context.get().idleTimeoutMs();
  }
  public static final void setIdleTimeoutMs(long value) {
    context.get().setIdleTimeoutMs(value);
  }

  public static final void sendBinary(ByteBuffer buffer) throws IOException {
    context.get().sendBinary(buffer);
  }

  public static final void sendBinary(byte[] data, int offset, int length) throws IOException {
    context.get().sendBinaryAsync(data, offset, length);
  }

  public static final void sendBinary(byte[] data) throws IOException {
    context.get().sendBinary(data);
  }

  public static final void sendBinary(InputStream stream) throws IOException {
    context.get().sendBinary(stream);
  }

  public static final void sendBinaryFragment(ByteBuffer buffer, boolean isLastFragment) throws IOException {
    context.get().sendBinaryFragment(buffer, isLastFragment);
  }

  public static final void sendBinaryFragment(byte[] data, int offset, int length, boolean isLastFragment) throws IOException {
    context.get().sendBinaryFragment(data, offset, length, isLastFragment);
  }

  public static final void sendBinaryFragment(byte[] data, boolean isLastFragment) throws IOException {
    context.get().sendBinaryFragment(data, isLastFragment);
  }

  public static final void sendBinaryFragment(InputStream stream, boolean isLastFragment) throws IOException {
    context.get().sendBinaryFragment(stream, isLastFragment);
  }

  public static final Future<Void> sendBinaryAsync(ByteBuffer buffer) {
    return context.get().sendBinaryAsync(buffer);
  }

  public static final Future<Void> sendBinaryAsync(byte[] data, int offset, int length) {
    return context.get().sendBinaryAsync(data, offset, length);
  }

  public static final Future<Void> sendBinaryAsync(byte[] data) {
    return context.get().sendBinaryAsync(data);
  }

  public static final Future<Void> sendBinaryAsync(InputStream stream) throws IOException {
    return context.get().sendBinaryAsync(stream);
  }

  public static final void sendText(String message) throws IOException {
    context.get().sendText(message);
  }

  public static final void sendTextFragment(String text, boolean isLastFragment) throws IOException {
    context.get().sendTextFragment(text, isLastFragment);
  }

  public static final Future<Void> sendTextAsync(String message) {
    return context.get().sendTextAsync(message);
  }

  public static final void disconnect() throws IOException {
    context.get().disconnect();
  }

  public static final void close() {
    context.get().close();
  }

  public static final void close(int status) {
    context.get().close(status);
  }

  public static final void close(int status, String reason) {
    context.get().close(status, reason);
  }

  public static final boolean isOpen() {
    return context.get().isOpen();
  }

  public static final boolean isSecure() {
    return context.get().isSecure();
  }
}
