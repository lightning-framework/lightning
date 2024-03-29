package lightning.mvc;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import lightning.auth.Auth;
import lightning.auth.AuthException;
import lightning.auth.drivers.MySQLAuthDriver;
import lightning.cache.Cache;
import lightning.config.Config;
import lightning.crypt.Hasher;
import lightning.crypt.SecureCookieManager;
import lightning.crypt.TokenSets;
import lightning.db.MySQLDatabase;
import lightning.db.MySQLDatabaseProvider;
import lightning.db.MySQLDatabaseProxy;
import lightning.enums.CacheControl;
import lightning.enums.HTTPHeader;
import lightning.enums.HTTPMethod;
import lightning.enums.HTTPScheme;
import lightning.enums.HTTPStatus;
import lightning.enums.JsonFieldNamingPolicy;
import lightning.exceptions.LightningException;
import lightning.groups.Groups;
import lightning.groups.Groups.GroupsException;
import lightning.groups.drivers.MySQLGroupDriver;
import lightning.http.AccessViolationException;
import lightning.http.BadRequestException;
import lightning.http.HaltException;
import lightning.http.MethodNotAllowedException;
import lightning.http.NotAuthorizedException;
import lightning.http.NotFoundException;
import lightning.http.NotImplementedException;
import lightning.http.Request;
import lightning.http.Response;
import lightning.inject.Injector;
import lightning.inject.InjectorModule;
import lightning.io.FileServer;
import lightning.json.JsonService;
import lightning.mail.Mailer;
import lightning.mvc.Validator.FieldValidator;
import lightning.server.LightningHandler;
import lightning.sessions.Session;
import lightning.sessions.Session.SessionException;
import lightning.sessions.drivers.MySQLSessionDriver;
import lightning.templates.TemplateEngine;
import lightning.users.User;
import lightning.users.Users;
import lightning.users.Users.UsersException;
import lightning.users.drivers.MySQLUserDriver;
import lightning.util.Time;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.jetty.server.MultiParts;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * A controller is a class that is used to process a single HTTP request and should be sub-classed.
 * Each controller lives only on a single thread to service a single request. Instances are
 * allocated to service a single request and destroyed after a response to that request is sent.
 * TODO: Built-in support for HTTP head (incl static files).
 */
public class HandlerContext implements AutoCloseable, MySQLDatabaseProvider {
  private static final Logger logger = LoggerFactory.getLogger(HandlerContext.class);
  public final Request request;
  public final Response response;
  public final Config config;
  public final TemplateEngine templateEngine;
  public final SecureCookieManager cookies;
  public final URLGenerator url;
  public final Validator validator;
  public final Session session;
  public final Auth auth;
  public final MySQLDatabase db;
  private final @Nullable Mailer mail;
  private final FileServer fs;
  private final MySQLDatabaseProxy dbProxy;
  private final Groups groups;
  private final Users users;
  private boolean isClosed;
  private final JsonService jsonifier;
  private final Cache cache;
  private final Injector injector;
  private final InjectorModule bindings;
  public final InjectorModule globalBindings;
  public final InjectorModule userBindings;
  private final MySQLDatabaseProvider dbp;
  public static final String ATTRIBUTE = HandlerContext.class.getCanonicalName();
  private AsyncContext asyncContext;

  public HandlerContext(Request rq, Response re, MySQLDatabaseProvider dbp, Config c, TemplateEngine te, FileServer fs, @Nullable Mailer mailer, JsonService jsonifier, Cache cache, InjectorModule globalModule, InjectorModule userModule) {
    logger.debug("context created");
    isClosed = false;
    this.request = rq;
    this.response = re;
    this.config = c;
    this.mail = mailer;
    this.dbp = dbp;
    this.templateEngine = te;
    this.cookies = SecureCookieManager.forRequest(request, response, config.server.hmacKey, config.ssl.isEnabled());
    this.url = URLGenerator.forRequest(request);
    this.validator = Validator.create(this);
    this.dbProxy = new MySQLDatabaseProxy(dbp);
    this.db = dbProxy;
    this.fs = fs;
    this.session = Session.forRequest(rq, re, config, new MySQLSessionDriver(this));
    this.groups = new Groups(new MySQLGroupDriver(this));
    this.users = new Users(new MySQLUserDriver(this, groups), groups);
    this.auth = Auth.forSession(session, new MySQLAuthDriver(this), users);
    this.jsonifier = jsonifier;
    this.cache = cache;
    this.bindings = new InjectorModule();
    this.bindings.bindClassToInstance(Validator.class, this.validator);
    this.bindings.bindClassToInstance(HandlerContext.class, this);
    this.bindings.bindClassToInstance(Session.class, this.session);
    this.bindings.bindClassToInstance(Request.class, this.request);
    this.bindings.bindClassToInstance(Response.class, this.response);
    this.bindings.bindClassToInstance(HttpServletRequest.class, this.request.raw());
    this.bindings.bindClassToInstance(HttpServletResponse.class, this.response.raw());
    this.bindings.bindClassToInstance(URLGenerator.class, this.url);
    this.bindings.bindClassToInstance(Groups.class, this.groups());
    this.bindings.bindClassToInstance(Users.class, this.users());
    this.bindings.bindClassToInstance(Cache.class, this.cache());
    this.bindings.bindClassToResolver(User.class, () -> this.user());
    this.bindings.bindClassToResolver(MySQLDatabase.class, () -> this.db());
    this.userBindings = userModule;
    this.globalBindings = globalModule;
    this.injector = new Injector(globalModule, userModule, this.bindings);
    this.bindings.bindClassToInstance(Injector.class, this.injector);
  }

  public Injector injector() {
    return this.injector;
  }

  public Injector globalInjector() {
    return new Injector(globalBindings, userBindings);
  }

  public InjectorModule bindings() {
    return this.bindings;
  }

  @Override
  public MySQLDatabase getDatabase() throws SQLException {
    return db();
  }

  @Override
  public Connection getConnection() throws SQLException {
    return db().raw();
  }

  public Hasher hasher() {
    return new Hasher(config.server.hmacKey);
  }

  public TokenSets tokens() {
    return new TokenSets(hasher());
  }

  public Mailer mail() throws LightningException {
    if (mail == null) {
      throw new LightningException("Mail is not configured.");
    }

    return mail;
  }

  /**
   * Starts asynchronous handling for the current request and returns a context.
   * You MUST manually close() this context in order to avoid leaking resources.
   * Methods on lightning.server.Context.* are unsafe to use after invoking this.
   * @return
   * @throws Exception
   */
  public HandlerContext goAsync() throws Exception {
    if (!request.raw().isAsyncSupported()) {
      throw new LightningException("Async is not supported on your platform.");
    }

    if (asyncContext != null || request.raw().isAsyncStarted() || isClosed) {
      throw new LightningException("Async has already been started on this request.");
    }

    this.asyncContext = request.raw().startAsync();
    return this;
  }

  public void halt() {
    throw new HaltException();
  }

  /**
   * Always called when the instance is destroyed; use to free resources.
   * This method may be overridden in a subclass if needed.
   * @see {@link java.util.AutoCloseable}
   */
  @Override
  public void close() {
    close(true);
  }

  private void close(boolean flush) {
    if (isClosed) {
      return;
    }

    logger.debug("context closed");

    if (asyncContext != null) {
      MultiParts parts = (MultiParts)request.raw().getAttribute(
          org.eclipse.jetty.server.Request.MULTIPARTS);
      if (parts != null) {
        try {
          parts.close();
        } catch (IOException e) {
          logger.warn("Error closing handler context:", e);
        }
      }
    }

    isClosed = true;

    try {
      // If the session was modified, save it.
      try {
        if (session != null && session.isDirty()) {
          try {
            session.save();
          } catch (SessionException e) {
            logger.warn("Error closing handler context:", e);
          }
        }
      } finally {
        // If a database connection was opened, free it.
        try {
          db.close();
        } catch (SQLException e) {
          logger.warn("Error closing handler context:", e);
        }
        try {
          dbProxy.reallyClose();
        } catch (SQLException e) {
          logger.warn("Error closing handler context:", e);
        }
      }

      if (flush) {
        try {
          logger.debug("flushing buffer");
          response.raw().flushBuffer();
        } catch (IOException e) {
          logger.warn("Error closing handler context:", e);
        }
      }
    } finally {
      request.raw().removeAttribute(HandlerContext.ATTRIBUTE);
      if (asyncContext != null) {
        asyncContext.complete();
      }
    }
  }

  /**
   * @return A database connection for use by this controller. Clients SHOULD NOT
   * close this connection, but should close any PreparedStatements and ResultSets
   * that they create. Returns the same connection instance every time.
   * @throws SQLException On failure.
   */
  public final MySQLDatabase getDB() throws SQLException {
    return db;
  }

  /**
   * @return A database connection for use by this controller. Clients SHOULD NOT
   * close this connection, but should close any PreparedStatements and ResultSets
   * that they create. Returns the same connection instance every time.
   * @throws SQLException On failure.
   */
  public final MySQLDatabase db() throws SQLException {
    return getDB();
  }

  /**
   * @return
   */
  public final Config getConfig() {
    return config;
  }

  public final Config config() {
    return config;
  }

  /**
   * @return The user account that the client of this request is authenticated to.
   * @throws SessionException On internal error.
   * @throws AuthException On internal error.
   * @throws NotAuthorizedException If no user is logged in.
   */
  public final User getUser() throws SessionException, AuthException, NotAuthorizedException {
    requireAuth();
    return getAuth().getUser();
  }

  /**
   * @return The user account that the client of this request is authenticated to.
   * @throws SessionException On internal error.
   * @throws AuthException On internal error.
   * @throws NotAuthorizedException If no user is logged in.
   */
  public final User user() throws SessionException, AuthException, NotAuthorizedException {
    return getUser();
  }

  /**
   * @return The authentication handler for this request.
   */
  public final Auth getAuth() {
    return auth;
  }

  /**
   * @return The authentication handler for this request.
   */
  public final Auth auth() {
    return getAuth();
  }

  /**
   * @return A session for the client of this request.
   */
  public final Session getSession() {
    return session;
  }

  /**
   * @return A session for the client of this request.
   */
  public final Session session() {
    return getSession();
  }

  /**
   * @return HTTP request from Spark.
   */
  public final Request getRequest() {
    return request;
  }

  /**
   * @return HTTP response from Spark.
   */
  public final Response getResponse() {
    return response;
  }

  /**
   * Aborts and displays an HTTP 501 Not Implemented error to clients.
   * @throws NotImplementedException Always.
   */
  public final void notImplemented() throws NotImplementedException {
    throw new NotImplementedException();
  }

  /**
   * Aborts and displays an HTTP 501 Not Implemented error to clients.
   * @param reason To display to user.
   * @throws NotImplementedException Always.
   */
  public final void notImplemented(String reason) throws NotImplementedException {
    throw new NotImplementedException(reason);
  }

  /**
   * Aborts and displays an HTTP 403 Forbidden error to clients.
   * @throws AccessViolationException Always.
   */
  public final void accessViolation() throws AccessViolationException {
    throw new AccessViolationException();
  }

  /**
   * Aborts and displays an HTTP 403 Forbidden error to clients.
   * @param reason To display to user.
   * @throws AccessViolationException Always.
   */
  public final void accessViolation(String reason) throws AccessViolationException {
    throw new AccessViolationException(reason);
  }

  /**
   * Aborts and displays an HTTP 403 Forbidden error to clients if the condition is met.
   * @param condition On which to throw an error.
   * @throws AccessViolationException If condition is true.
   */
  public final void accessViolationIf(boolean condition) throws AccessViolationException {
    if (condition) {
      throw new AccessViolationException();
    }
  }

  /**
   * Aborts and displays an HTTP 403 Forbidden error to clients if the condition is met.
   * @param condition On which to throw on error.
   * @param reason To display to user.
   * @throws AccessViolationException If condition is true.
   */
  public final void accessViolationIf(boolean condition, String reason) throws AccessViolationException {
    if (condition) {
      throw new AccessViolationException(reason);
    }
  }

  /**
   * Aborts and displays an HTTP 404 Not Found error to clients.
   * @throws NotFoundException Always.
   */
  public final void notFound() throws NotFoundException {
    throw new NotFoundException();
  }

  /**
   * Aborts and displays an HTTP 404 Not Found error to clients.
   * @param reason To display to user.
   * @throws NotFoundException Always.
   */
  public final void notFound(String reason) throws NotFoundException {
    throw new NotFoundException(reason);
  }

  /**
   * Aborts and displays an HTTP 404 Not Found error to clients.
   * @param condition On which to display not found error.
   * @throws NotFoundException If condition is true.
   */
  public final void notFoundIf(boolean condition) throws NotFoundException {
    if (condition) {
      throw new NotFoundException();
    }
  }

  /**
   * Aborts and displays an HTTP 404 Not Found error to clients.
   * @param condition On which to display not found error.
   * @param reason To display to user.
   * @throws NotFoundException If condition is true.
   */
  public final void notFoundIf(boolean condition, String reason) throws NotFoundException {
    if (condition) {
      throw new NotFoundException(reason);
    }
  }

  /**
   * Aborts and displays an HTTP 400 Bad Request error to clients.
   * @param reason To display to user.
   * @throws BadRequestException Always.
   */
  public final void badRequest(String reason) throws BadRequestException {
    throw new BadRequestException(reason);
  }

  /**
   * Aborts and displays an HTTP 400 Bad Request error to clients.
   * @param condition On which to display the error.
   * @throws BadRequestException If condition is true.
   */
  public final void badRequestIf(boolean condition) throws BadRequestException {
    if (condition) {
      throw new BadRequestException();
    }
  }

  /**
   * Aborts and displays an HTTP 500 Bad Request error to clients.
   * @param condition On which to display the error.
   * @throws Exception If condition is true.
   */
  public final void illegalStateIf(boolean condition) throws Exception {
    if (condition) {
      throw new Exception();
    }
  }

  /**
   * Aborts and displays an HTTP 500 Bad Request error to clients.
   * @param condition On which to display the error.
   * @throws Exception If condition is true.
   */
  public final void illegalStateIf(boolean condition, String message) throws Exception {
    if (condition) {
      throw new Exception(message);
    }
  }

  /**
   * Aborts and displays an HTTP 400 Bad Request error to clients.
   * @param condition On which to display the error.
   * @param reason To display to user.
   * @throws BadRequestException If condition is true.
   */
  public final void badRequestIf(boolean condition, String reason) throws BadRequestException {
    if (condition) {
      throw new BadRequestException(reason);
    }
  }

  /**
   * @return Whether or not the client of this request is logged in.
   * @throws SessionException On internal failure.
   * @throws AuthException On internal failure.
   */
  public final boolean isLoggedIn() throws SessionException, AuthException {
    return getAuth().isLoggedIn();
  }

  /**
   * Requires that the client is logged in; aborts the request and displays an HTTP 401 Unauthorized error if not.
   * @throws NotAuthorizedException If client is not logged in.
   * @throws SessionException On internal failure.
   * @throws AuthException On internal failure.
   */
  public final void requireAuth() throws NotAuthorizedException, SessionException, AuthException {
    if (!getAuth().isLoggedIn()) {
      throw new NotAuthorizedException();
    }
  }

  /**
   * Requires that the client has the given privileges; aborts the request and displays an HTTP 401 Unauthorized error
   * if not logged in and an HTTP 403 Forbidden error if the user does not hold the privileges.
   * @param permissions To require.
   * @throws GroupsException On internal failure.
   * @throws UsersException On internal failure.
   * @throws SessionException On internal failure.
   * @throws AuthException On internal failure.
   * @throws NotAuthorizedException If not logged in.
   * @throws AccessViolationException If permissions not met.
   */
  public final void requirePermissions(List<Long> permissions) throws GroupsException, UsersException, SessionException, AuthException, NotAuthorizedException, AccessViolationException {
    requireAuth();

    if (!getAuth().getUser().hasPrivileges(permissions)) {
      throw new AccessViolationException();
    }
  }

  /**
   * Requires that the request was made over HTTPS; aborts and displays a 400 bad request error if not.
   * @throws BadRequestException If not over HTTPS.
   */
  public final void requireSecure() throws BadRequestException {
    if (!request.scheme().equals(HTTPScheme.HTTPS)) {
      throw new BadRequestException("This website requires requests be made over HTTPS.");
    }
  }

  /**
   * Requires that the request is made with the given HTTP method; aborts and displays  a 405 Method Not allowed
   * error if not.
   * @param method An HTTP method (e.g. 'POST').
   * @throws MethodNotAllowedException If the given method does not match that of the request.
   */
  public final void requireMethod(HTTPMethod method) throws MethodNotAllowedException {
    if (request.method() != method) {
      throw new MethodNotAllowedException();
    }
  }

  /**
   * @return Whether or not this request is HTTP multi-part.
   */
  public final boolean isMultipart() {
    return request.isMultipart();
  }

  /**
   * Requires that the given request is made using HTTP Multi-Part Encoding. If not, throws a 400 Bad Request error
   * and aborts. If so, then installs the necessary information in the servlet to read the multipart request.
   * NOTE: For multipart requests, use getRequest().raw().getPart() to read parts; getRequest().queryParams() also
   * works for provided query parameters. Utilize for file uploads.
   * @throws BadRequestException If the request is not multipart.
   */
  public final void requireMultipart() throws BadRequestException {
    if (!isMultipart()) {
      throw new BadRequestException("Incorrect request type; expected multipart/form-data.");
    }
  }

  /**
   * @return Whether or not servicing a POST request.
   */
  public final boolean isPOST() {
    return request.method() == HTTPMethod.POST;
  }

  /**
   * @return Whether or not servicing a GET request.
   */
  public final boolean isGET() {
    return request.method() == HTTPMethod.GET;
  }

  /**
   * Aborts and sends an HTTP 302 redirect to the given location.
   * @param path To redirect to.
   * @return null (So that you return this in handleRequest and be type safe).
   */
  public final void redirect(String path) {
    response.redirect(path, 302);
    halt();
  }

  /**
   * Aborts and sends an HTTP 302 redirect to the given location if the client is logged in.
   * @param path To redirect to.
   * @throws SessionException On internal failure.
   * @throws AuthException On internal failure.
   */
  public final void redirectIfLoggedIn(String path) throws SessionException, AuthException {
    if (getAuth().isLoggedIn()) {
      response.redirect(path, 302);
      halt();
    }
  }

  /**
   * Aborts and sends an HTTP 302 redirect to the given location if the client is not logged in.
   * @param path To redirect to.
   * @throws SessionException On internal failure.
   * @throws AuthException On internal failure.
   */
  public final void redirectIfNotLoggedIn(String path) throws SessionException, AuthException {
    if (!getAuth().isLoggedIn()) {
      response.redirect(path, 302);
      halt();
    }
  }

  /**
   * Requires that the request has data attached for the given name (as either a GET or POST parameter).
   * @param name Of the parameter.
   * @return An object that can be used to chain further tests about the param (e.g. requireData("name").isNotEmpty()).
   * @throws BadRequestException If the parameter is not present.
   */
  public final ParamTester requireQueryParam(String name) throws BadRequestException {
    return ParamTester.create(request.queryParam(name)).isNotNull();
  }

  /**
   * Requires that the request has a parameter attached for the given name (from the Spark URL route matcher).
   * Aborts and throws a 400 Bad Request error if the parameter is not present.
   * @param name Of the parameter.
   * @return An object that can be used to chain further tests about the param (e.g. requireParam("name").isNotEmpty()).
   * @throws BadRequestException If the parameter is not present.
   */
  public final ParamTester requireParam(String name) throws BadRequestException {
    return ParamTester.create(request.routeParam(name)).isNotNull();
  }

  /**
   * Requires the presence of a part (identified by its name) in a multi-part request.
   * Aborts and throws a 400 Bad Request error if the part is not present or request is not multi-part.
   * @param name Of the part.
   * @throws BadRequestException If not found.
   * @throws IOException On failure.
   * @throws ServletException On failure.
   */
  public final void requirePart(String name) throws BadRequestException, IOException, ServletException {
    Part part = request.raw().getPart(name);

    if (part == null) {
      throw new BadRequestException("Request missing mandatory part '" + name + "'.");
    }
  }

  /**
   * @param name A query parameter name.
   * @return A wrapper object with useful functionality for reading the query parameter.
   */
  public final Param queryParam(String name) {
    return request.queryParam(name);
  }

  /**
   * @param name A route parameter name.
   * @return A wrapper object with useful functionality for reading the route parameter.
   */
  public final Param routeParam(String name) {
    return request.routeParam(name);
  }

  /**
   * @return A form validator.
   */
  public final Validator getValidator() {
    return validator;
  }

  /**
   * @return Whether or not the validator has no errors.
   */
  public final boolean passesValidation() {
    return !getValidator().hasErrors();
  }

  /**
   * Returns a validator for the given query parameter.
   * @param queryParamName A query parameter name.
   * @return A validator for the given queryParamName.
   */
  public final FieldValidator validate(String queryParamName) {
    return getValidator().check(queryParamName);
  }

  /**
   * Requires the presence of the XSRF token under the given parameter name.
   * @param queryParamName
   * @throws Exception
   */
  public final void requireXsrf(String queryParamName) throws Exception {
    if (!queryParam(queryParamName).stringValue().equals(session().getXSRFToken())) {
      throw new BadRequestException("An cross-site request forgery attack was detected and prevented.");
    }
  }

  /**
   * Requires the presence of the XSRF token under the parameter name "_xsrf".
   * @throws Exception
   */
  public final void requireXsrf() throws Exception {
    requireXsrf("_xsrf");
  }

  /**
   * Validates that the XSRF token (provided by given query param name) is correct.
   * @param queryParamName
   * @throws Exception
   */
  public final void validateXsrf(String queryParamName) throws Exception {
    getValidator().check(queryParamName).is(getSession().getXSRFToken(),
        "You entered an incorrect XSRF token.");
  }

  /**
   * Validates that the XSRF token provided by the forms.ftl library is correct.
   * @throws Exception
   */
  public final void validateXsrf() throws Exception {
    validateXsrf("_xsrf");
  }

  /**
   * @param names A list of query parameters to not include in the output.
   * @return A map of keys to values for all query parameters excepting those in names.
   */
  public final Map<String, Param> queryParamsExcepting(Collection<String> names) {
    Map<String, Param> result = new HashMap<>();

    for (String s : request.queryParams()) {
      if (names.contains(s)) {
        continue;
      }

      result.put(s, request.queryParam(s));
    }

    return result;
  }

  /**
   * @return A map of keys to values for all query parameters.
   */
  public final Map<String, Param> queryParams() {
    return queryParamsExcepting(ImmutableList.of());
  }

  public final void sendJson(Object object) throws Exception {
    sendJson(object, null, JsonFieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
  }

  public final void sendJson(Object object, JsonFieldNamingPolicy policy) throws Exception {
    sendJson(object, null, policy);
  }

  public final void sendJson(Object object, String prefix) throws Exception {
    sendJson(object, prefix, JsonFieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
  }

  public final void sendJson(Object object, String prefix, JsonFieldNamingPolicy policy) throws Exception {
    response.status(HTTPStatus.OK);
    response.type("application/json; charset=UTF-8");

    if (prefix != null && prefix.length() > 0) {
      response.outputStream().print(prefix);
    }

    jsonifier.writeJson(object, response.outputStream(), policy);
  }

  public final String toJson(Object object) throws Exception {
    return toJson(object, JsonFieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
  }

  public final String toJson(Object object, JsonFieldNamingPolicy policy) throws Exception {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    jsonifier.writeJson(object, stream, policy);
    return stream.toString("UTF-8");
  }

  public final <T> T parseJson(String json, Class<T> clazz) throws Exception {
    return parseJson(json, clazz, JsonFieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
  }

  public final <T> T parseJson(String json, Class<T> clazz, JsonFieldNamingPolicy policy) throws Exception {
    return jsonifier.readJson(clazz, IOUtils.toInputStream(json, "UTF-8"), policy);

  }

  public final <T> T parseJson(Class<T> clazz) throws Exception {
   return parseJson(clazz, JsonFieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
  }

  public final <T> T parseJson(Class<T> clazz, JsonFieldNamingPolicy policy) throws Exception {
    return jsonifier.readJson(clazz, request.raw().getInputStream(), policy);
  }

  public final <T> T parseJsonFromParam(String queryParamName, Class<T> clazz, JsonFieldNamingPolicy policy) throws Exception {
    if (isMultipart()) {
      return jsonifier.readJson(clazz, request.getPart(queryParamName).getInputStream(), policy);
    } else {
      return jsonifier.readJson(clazz, IOUtils.toInputStream(queryParam(queryParamName).stringValue(), "UTF-8"), policy);
    }
  }

  public final <T> T parseJsonFromParam(String queryParamName, Class<T> clazz) throws Exception {
    return parseJsonFromParam(queryParamName, clazz, JsonFieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
  }

  /**
   * @param model
   * @param viewName
   * @return
   */
  public final ModelAndView modelAndView(String viewName, Object viewModel) {
    return new ModelAndView(viewName, viewModel);
  }

  /**
   * @param model
   * @param viewName
   * @return
   */
  public final String renderToString(String viewName, Object viewModel) throws Exception {
    StringWriter stringWriter = new StringWriter();
    templateEngine.render(viewName, viewModel, stringWriter);
    return stringWriter.toString();
  }

  /**
   * @param modelAndView
   * @return
   * @throws Exception
   */
  public final String renderToString(ModelAndView modelAndView) throws Exception {
    return renderToString(modelAndView.viewName, modelAndView.viewModel);
  }

  /**
   * @param viewName
   * @param model
   * @throws Exception
   */
  public final void render(String viewName, Object viewModel) throws Exception {
    response.header(HTTPHeader.CONTENT_TYPE, "text/html; charset=UTF-8");
    templateEngine.render(viewName, viewModel, response.raw().getWriter());
  }

  /**
   * @param modelAndView
   * @throws Exception
   */
  public final void render(ModelAndView modelAndView) throws Exception {
    response.header(HTTPHeader.CONTENT_TYPE, "text/html; charset=UTF-8");
    templateEngine.render(modelAndView.viewName, modelAndView.viewModel, response.raw().getWriter());
  }

  /**
   * See sendFile(File, CacheType)
   * CAUTION: File will be sent with Cache-Control PUBLIC.
   * @param file
   * @throws Exception
   */
  public void sendFile(File file) throws Exception {
    sendFile(file, CacheControl.PUBLIC);
  }

  /**
   * Writes the contents of a file into the HTTP response, setting headers appropriately.
   * IMPORTANT: When a handler calls sendFile(), that handler essentially acts as if it exposes
   *            the given file as if it were a static file. Thus, the entire HTTP specification
   *            is supported (partials, caching, etc) as if it were a static file being served.
   *            Takes advantage of async IO for speed.
   *            File name may be exposed to user.
   *            Will be sent with specified cache control.
   *            TODO: Can this method cache the file contents using resource cache + mmap-ing
   *                  like DefaultServlet does?
   *            TODO: Can this take advantage of GZIP?
   * @param file A file.
   */
  public void sendFile(File file, CacheControl cacheType) throws Exception {
    if (!file.exists()) {
      throw new NotFoundException();
    }

    if (file.isDirectory()) {
      throw new LightningException("sendFile cannot accept directories.");
    }

    if (!file.canRead()) {
      throw new LightningException("sendFile cannot read the passed file: " + file.getCanonicalPath());
    }

    if (response.hasSentHeaders()) {
      throw new IOException("Cannot send file to committed response.");
    }

    fs.sendResource(request.raw(), response.raw(), Resource.newResource(file), cacheType);
    response.raw().flushBuffer();
    return;
  }

  public boolean isAsync() {
    return asyncContext != null;
  }

  public Groups groups() {
    return groups;
  }

  public Users users() {
    return users;
  }

  public final void enableHttpCaching(CacheControl type, String etag, long lastModifiedTime) {
    if (config.enableDebugMode || type == CacheControl.NO_CACHE) {
      return;
    }

    if (request.header(HTTPHeader.IF_NONE_MATCH).exists() &&
        request.header(HTTPHeader.IF_NONE_MATCH).isEqualTo(etag)) {
      response.status(HTTPStatus.NOT_MODIFIED);
      halt();
    }

    if (request.header(HTTPHeader.IF_MODIFIED_SINCE).exists()) {
      try {
        long time = Time.parseFromHttp(request.header(HTTPHeader.IF_MODIFIED_SINCE).stringValue());

        if (time >= lastModifiedTime) {
          response.status(HTTPStatus.NOT_MODIFIED);
          halt();
        }
      } catch (ParseException e) {
        badRequest("HTTP If-Modified-Since header does not conform to spec.");
      }
    }

    response.header(HTTPHeader.CACHE_CONTROL, type.toHttpString() + ", max-age=3600");
    response.header(HTTPHeader.LAST_MODIFIED, Time.formatForHttp(lastModifiedTime));
    response.header(HTTPHeader.ETAG, etag);
    response.header(HTTPHeader.EXPIRES, Time.formatForHttp(Time.now() + 3600));
  }

  public Cache cache() {
    return cache;
  }

  public void maybeSaveSession() throws SessionException {
    if (isClosed) {
      return;
    }

    if (session != null && session.isDirty()) {
      session.save();
    }
  }

  public void handleException(Throwable error) {
    if (!(error instanceof IOException)) {
      logger.warn("Route handler returned exception: ", error);
    }
    try {
      LightningHandler handler = injector().getInjectedArgumentForClass(LightningHandler.class);
      handler.sendErrorPage(request.raw(),
                            response.raw(),
                            error,
                            handler.getRouteMatch(request.path(), request.method()));
    } catch (Exception e) {
      // Nothing we can do.
    }
  }

  public Response response() {
    return response;
  }

  public Request request() {
    return request;
  }

  public TemplateEngine templates() {
    return templateEngine;
  }

  public JsonService json() {
    return jsonifier;
  }

  public MySQLDatabaseProvider dbPool() {
    return dbp;
  }

  public boolean isDebug() {
    return config.enableDebugMode;
  }
}
