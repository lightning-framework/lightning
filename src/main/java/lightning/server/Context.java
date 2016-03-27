package lightning.server;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import lightning.auth.Auth;
import lightning.auth.AuthException;
import lightning.config.Config;
import lightning.crypt.SecureCookieManager;
import lightning.db.MySQLDatabase;
import lightning.enums.HTTPMethod;
import lightning.groups.Groups.GroupsException;
import lightning.http.AccessViolationException;
import lightning.http.BadRequestException;
import lightning.http.MethodNotAllowedException;
import lightning.http.NotAuthorizedException;
import lightning.http.NotFoundException;
import lightning.http.NotImplementedException;
import lightning.http.Request;
import lightning.http.Response;
import lightning.mvc.HandlerContext;
import lightning.mvc.ModelAndView;
import lightning.mvc.Param;
import lightning.mvc.ParamTester;
import lightning.mvc.URLGenerator;
import lightning.mvc.Validator;
import lightning.mvc.Validator.FieldValidator;
import lightning.sessions.Session;
import lightning.sessions.Session.SessionException;
import lightning.users.User;
import lightning.users.Users.UsersException;

import com.google.gson.FieldNamingPolicy;

import freemarker.template.Configuration;

/**
 * Provides a thread-specific context for incoming requests.
 * Controllers will want to import static Context.* and use these methods.
 * Methods delegate to an instance of HandlerContext; see the source of HandlerContext for documentation.
 */
public class Context {
  private static final ThreadLocal<HandlerContext> controller = new ThreadLocal<>();
  
  static final void setContext(HandlerContext c) {
    controller.set(c);
  }
  
  static final HandlerContext context() {
    return controller.get();
  }
  
  static final void clearContext() {
    controller.remove();
  }
  
  /**
   * Returns a fixed snapshot of the context that can be used asynchronously.
   * Does not make the underlying request asynchronous; you must do this on your own.
   * 
   * IF YOU GO ASYNCHRONOUS, BE AWARE:
   *   - You MUST close the HandlerContext by invoking close() to avoid leaking resources.
   *   - Methods on HandlerContext were designed with the assumption the request is being handled synchronously.
   *     Some of them may not work correctly while going asynchronous.
   *   - Static methods on Context are no longer safe to use in your async handlers; use the instance returned
   *     by this function instead.
   * @return
   */
  public static final HandlerContext goAsync() {
    context().goAsync();
    return context();
  }
  
  public static final boolean contextExists() {
    return controller.get() != null;
  }
  
  public static final Request request() {
    return context().request;
  }
  
  public static final Response response() {
    return context().response;
  }
  
  public static final Config config() {
    return context().config;
  }
  
  public static final Configuration templateEngine() {
    return context().templateEngine;
  }
  
  public static final SecureCookieManager cookies() {
    return context().cookies;
  }
  
  public static final URLGenerator url() {
    return context().url;
  }
  
  public static final Validator validator() {
    return context().validator;
  }
  
  public static final Session session() {
    return context().session;
  }
  
  public static final Auth auth() {
    return context().auth;
  }
  
  public static final User user() throws SessionException, AuthException {
    return context().getUser();
  }
  
  public static final MySQLDatabase db() throws SQLException {
    return context().db();
  }

  public static final void notImplemented() throws NotImplementedException {
    context().notImplemented();
  }

  public static final void notImplemented(String reason) throws NotImplementedException {
    context().notImplemented(reason);
  }
  
  public static final void accessViolation() throws AccessViolationException {
    context().accessViolation();
  }

  public static final void accessViolation(String reason) throws AccessViolationException {
    context().accessViolation(reason);
  }

  public static final void accessViolationIf(boolean condition) throws AccessViolationException {
    context().accessViolationIf(condition);
  }

  public static final void accessViolationIf(boolean condition, String reason) throws AccessViolationException {
    context().accessViolationIf(condition, reason);
  }

  public static final void notFound() throws NotFoundException {
    context().notFound();
  }

  public static final void notFound(String reason) throws NotFoundException {
    context().notFound(reason);
  }

  public static final void notFoundIf(boolean condition) throws NotFoundException {
    context().notFoundIf(condition);
  }

  public static final void notFoundIf(boolean condition, String reason) throws NotFoundException {
    context().notFoundIf(condition, reason);
  }

  public static final void badRequest(String reason) throws BadRequestException {
    context().badRequest(reason);
  }

  public static final void badRequestIf(boolean condition) throws BadRequestException {
    context().badRequestIf(condition);
  }

  public static final void illegalStateIf(boolean condition) throws Exception {
    context().illegalStateIf(condition);
  }

  public static final void illegalStateIf(boolean condition, String message) throws Exception {
    context().illegalStateIf(condition, message);
  }

  public static final void badRequestIf(boolean condition, String reason) throws BadRequestException {
    context().badRequestIf(condition, reason);
  }

  public static final boolean isLoggedIn() throws SessionException, AuthException {
    return context().isLoggedIn();
  }

  public static final void requireAuth() throws NotAuthorizedException, SessionException, AuthException {
    context().requireAuth();
  }

  public static final void requirePermissions(List<Long> permissions) throws GroupsException, UsersException, SessionException, AuthException, NotAuthorizedException, AccessViolationException {
    context().requirePermissions(permissions);
  }

  public static final void requireSecure() throws BadRequestException {
    context().requireSecure();
  }

  public static final void requireMethod(HTTPMethod method) throws MethodNotAllowedException {
    context().requireMethod(method);
  }

  public static final boolean isMultipart() {
    return context().isMultipart();
  }

  public static final void requireMultipart() throws BadRequestException {
    context().requireMultipart();
  }

  public static final boolean isPOST() {
    return context().isPOST();
  }

  public static final boolean isGET() {
    return context().isGET();
  }

  public static final Object redirect(String path) {
    return context().redirect(path);
  }

  public static final void redirectIfLoggedIn(String path) throws SessionException, AuthException {
    context().redirectIfLoggedIn(path);
  }

  public static final void redirectIfNotLoggedIn(String path) throws SessionException, AuthException {
    context().redirectIfNotLoggedIn(path);
  }
  
  public static final ParamTester requireQueryParam(String name) throws BadRequestException {
    return context().requireQueryParam(name);
  }

  public static final ParamTester requireParam(String name) throws BadRequestException {
    return context().requireParam(name);
  }

  public static final void requirePart(String name) throws BadRequestException, IOException, ServletException {
    context().requirePart(name);
  }

  public static final Param queryParam(String name) {
    return context().queryParam(name);
  }

  public static final Param routeParam(String name) {
    return context().routeParam(name);
  }
  
  public static final boolean passesValidation() {
    return context().passesValidation();
  }

  public static final FieldValidator validate(String queryParamName) {
    return context().validate(queryParamName);
  }
  
  public static final void requireXsrf(String queryParamName) throws Exception {
    context().requireXsrf(queryParamName);
  }
  
  public static final void requireXsrf() throws Exception {
    context().requireXsrf();
  }
  
  public static final void validateXsrf(String queryParamName) throws Exception {
    context().validateXsrf(queryParamName);
  }
  
  public static final void validateXsrf() throws Exception {
    context().validateXsrf();
  }

  public static final Map<String, Param> queryParamsExcepting(Collection<String> names) {
    return context().queryParamsExcepting(names);
  }

  public static final Map<String, Param> queryParams() {
    return context().queryParams();
  }
  
  public static final ModelAndView modelAndView(String viewName, Object viewModel) {
    return context().modelAndView(viewName, viewModel);
  }
  
  public static final String renderToString(String viewName, Object viewModel) throws Exception {
    return context().renderToString(viewName, viewModel);
  }
  
  public static final void sendFile(File file) throws Exception {
    context().sendFile(file);
  }
  
  public final void sendJson(Object object) throws IOException {
    context().sendJson(object);
  }
  
  public final void sendJson(Object object, FieldNamingPolicy policy) throws IOException {
    context().sendJson(object, policy);
  }
  
  public final void sendJson(Object object, String prefix) throws IOException {
    context().sendJson(object, prefix);
  }
  
  public final void sendJson(Object object, String prefix, FieldNamingPolicy policy) throws IOException {
    context().sendJson(object, prefix, policy);
  }
  
  public final String toJson(Object object) {
    return context().toJson(object);
  }
  
  public final String toJson(Object object, FieldNamingPolicy policy) {
    return context().toJson(object, policy);
  }
  
  public final <T> T parseJson(String json, Class<T> clazz) {
    return context().parseJson(json, clazz);
  }
  
  public final <T> T parseJson(String json, Class<T> clazz, FieldNamingPolicy policy) {
    return context().parseJson(json, clazz, policy);
  }
}
