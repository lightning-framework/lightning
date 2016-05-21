package lightning.http;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import lightning.crypt.SecureCookieManager;
import lightning.crypt.SecureCookieManager.InsecureCookieException;
import lightning.enums.HTTPHeader;
import lightning.enums.HTTPMethod;
import lightning.enums.HTTPScheme;
import lightning.mvc.ObjectParam;
import lightning.mvc.Param;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Lightning's representation of an incoming HTTP request.
 */
public class Request {
  protected final HttpServletRequest request;
  protected List<String> wildcardMatches;
  protected Map<String, String> parameters;
  protected SecureCookieManager cookies;
  protected final boolean trustLoadBalancerHeaders;
  protected Map<String, Object> properties;

  public Request(HttpServletRequest request, boolean trustLoadBalancerHeaders) {
    this.request = request;
    wildcardMatches = ImmutableList.of();
    parameters = ImmutableMap.of();
    cookies = null;
    this.trustLoadBalancerHeaders = trustLoadBalancerHeaders;
    properties = null;
  }
  
  /**
   * Returns a property attached to the request.
   * @param name
   * @return
   */
  public ObjectParam property(String name) {
    if (properties == null) {
      return new ObjectParam(null);
    }
    
    return new ObjectParam(properties.get(name));
  }
  
  /**
   * Sets a property attached to the request.
   * @param name
   * @param value
   */
  public void property(String name, Object value) {
    if (properties == null) {
      properties = new HashMap<>();
    }
    
    properties.put(name, value);
  }
  
  /**
   * Returns all of the properties attached to the request.
   * @return
   */
  public Set<String> properties() {
    if (properties == null) {
      return ImmutableSet.of();
    }
    
    return Collections.unmodifiableSet(properties.keySet());
  }

  /**
   * @return The names of all valid, signed cookies attached to the request.
   */
  public Set<String> cookies() {
    return Collections.unmodifiableSet(cookies.all());
  }

  /**
   * Returns the value of a cookie.
   * 
   * The framework uses cryptographic hashing to verify the integrity of cookies sent by the user.
   * If the framework receives a cookie from a user that is not signed correctly, the framework will
   * ignore that cookie (act as if it does not exist).
   * 
   * If you wish to read cookies that are unsigned, use the unencryptedCookie(...) method.
   * 
   * @param name The name of a cookie.
   * @return The value of the cookie (expressed as a Param).
   */
  public Param cookie(String name) {
    try {
      return Param.wrap(name, cookies.get(name));
    } catch (InsecureCookieException e) {
      return Param.wrap(name, null);
    }
  }

  /**
   * @return Returns the underlying Jetty request.
   */
  public HttpServletRequest raw() {
    return request;
  }

  /**
   * @param header An HTTP header.
   * @return The value of the given header.
   */
  public Param header(HTTPHeader header) {
    return header(header.getHeaderName());
  }

  /**
   * @param headerName An HTTP header name.
   * @return The value of the given header.
   */
  public Param header(String headerName) {
    return Param.wrap(headerName, request.getHeader(headerName));
  }

  /**
   * @return The HTTP method of the current request.
   */
  public HTTPMethod method() {
    return HTTPMethod.valueOf(request.getMethod().toUpperCase());
  }

  /**
   * @return The path of the current request (excludes query string).
   */
  public String path() {
    return request.getPathInfo();
  }

  /**
   * @param name A route parameter name (do not prefix with :).
   * @return The value of the given route parameter.
   */
  public Param routeParam(String name) {
    return Param.wrap(name, parameters.get(name));
  }

  /**
   * @return Returns the list of all set route parameter names.
   */
  public Set<String> routeParams() {
    return Collections.unmodifiableSet(parameters.keySet());
  }

  /**
   * @return The wildcard path of the matched route. This is the path starting from the location of
   *         the wildcard. For example, a route to "/u/*" for path "/u/my/page" would return "my/page"
   */
  public String wildcardPath() {
    return Joiner.on("/").join(wildcardMatches);
  }

  /**
   * @return The wildcard segments of the matched route. For example, a route to "/u/*" for path
   *         "/u/my/page" would return ["my", "page"].
   */
  public List<String> wildcards() {
    return Collections.unmodifiableList(wildcardMatches);
  }

  /**
   * @return The complete URL of the current request (scheme and host included).
   */
  public String url() {
    return request.getRequestURL().toString();
  }

  /**
   * Returns the value of a query parameter. A query parameter may be provided either through HTTP
   * POST data or GET parameters.
   * 
   * @param name A query parameter name.
   * @return The value of the given query parameter.
   */
  public Param queryParam(String name) {
    return Param.wrapList(name, request.getParameterValues(name));
  }

  /**
   * @return Returns the HTTP scheme (HTTP or HTTPS). Respects the X-Forwarded-Proto header if the
   *         server is configured to honor it.
   */
  public HTTPScheme scheme() {
    if (trustLoadBalancerHeaders && header(HTTPHeader.X_FORWARDED_PROTO).exists()) {
      return parseScheme(header(HTTPHeader.X_FORWARDED_PROTO).stringValue());
    }

    return parseScheme(request.getScheme());
  }

  private HTTPScheme parseScheme(String scheme) {
    switch (scheme.toLowerCase()) {
      case "http":
        return HTTPScheme.HTTP;
      case "https":
        return HTTPScheme.HTTPS;
      default:
        return HTTPScheme.UNKNOWN;
    }
  }

  /**
   * @return A map of all cookie names to cookie values. Prefer using cookies().
   */
  public Map<String, Param> unencryptedCookies() {
    Map<String, Param> result = new HashMap<>();
    Cookie[] cookies = request.getCookies();
    
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        result.put(cookie.getName(), Param.wrap(cookie.getName(), cookie.getValue()));
      }
    }
    
    return result;
  }

  /**
   * @param name A cookie name.
   * @return The raw value of the cookie with given name, or null if it doesn't exist. Prefer using
   *         cookie(...).
   */
  public Param unencryptedCookie(String name) {
    Cookie[] cookies = request.getCookies();
    
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals(name)) {
          return Param.wrap(name, cookie.getValue());
        }
      }
    }
    
    return Param.wrap(name, null);
  }

  /**
   * @return The host name attached to the request.
   */
  public String host() {
    return request.getHeader(HTTPHeader.HOST.getHeaderName());
  }

  /**
   * @return The IP address of the client (as a String). Respects the X-Forwarded-For header
   *         if the server is configured to honor it.
   */
  public String ip() {
    if (trustLoadBalancerHeaders && header(HTTPHeader.X_FORWARDED_FOR).exists()) {
      // X_FORWARDED_FOR is of the form: OriginIP[, ProxyIP1[, ...]
      String value = header(HTTPHeader.X_FORWARDED_FOR).stringValue();

      if (!value.contains(",")) {
        return value;
      } else {
        return value.split(",")[0];
      }
    }

    return request.getRemoteAddr();
  }

  /**
   * @return The set of all query parameter names attached to the request.
   */
  public Set<String> queryParams() {
    return Collections.unmodifiableSet(request.getParameterMap().keySet());
  }

  /**
   * @return Whether or not the request was received securely (via HTTPS).
   */
  public boolean isSecure() {
    return scheme().isSecure();
  }

  /**
   * @return Content type of incoming request (may not exist).
   */
  public Param contentType() {
    return Param.wrap(request.getContentType());
  }

  /**
   * @return The part of this request's URL from the protocol name up to the query string in the
   *         first line of the HTTP request.
   */
  public String uri() {
    return request.getRequestURI();
  }

  /**
   * @return Whether or not the incoming request is an HTTP multi-part request.
   */
  public boolean isMultipart() {
    return method() == HTTPMethod.POST 
        && request.getContentType() != null
        && request.getContentType().startsWith("multipart/form-data");
  }

  /**
   * @param name A part name.
   * @return The part with the given name in a multipart-request.
   * @throws BadRequestException If incoming request is not multipart or part is missing.
   * @throws IOException
   * @throws ServletException
   */
  public Part getPart(String name) throws BadRequestException, IOException, ServletException {
    if (!isMultipart()) {
      throw new BadRequestException("Expected a multipart request.");
    }

    Part p = request.getPart(name);

    if (p == null) {
      throw new BadRequestException("Missing multipart '" + name + "'.");
    }

    return p;
  }
}
