package lightning.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lightning.crypt.SecureCookieManager;
import lightning.crypt.SecureCookieManager.InsecureCookieException;
import lightning.enums.HTTPHeader;
import lightning.enums.HTTPMethod;
import lightning.enums.HTTPScheme;
import lightning.mvc.Param;

public class Request {  
  protected final HttpServletRequest request;
  protected List<String> wildcardMatches;
  protected Map<String, String> parameters;
  protected SecureCookieManager cookies;
  protected final boolean trustLoadBalancerHeaders;
  
  public Request(HttpServletRequest request, boolean trustLoadBalancerHeaders) {
    this.request = request;
    wildcardMatches = ImmutableList.of();
    parameters = ImmutableMap.of();
    cookies = null;
    this.trustLoadBalancerHeaders = trustLoadBalancerHeaders;
  }
  
  /**
   * Returns a list of all cookies names attached to the request whose cryptographic
   * signatures could be verified.
   * @return
   */
  public Iterable<String> cookies() {
    return cookies.all();
  }
  
  /**
   * Returns the value of a cookie.
   * 
   * The framework uses cryptographic hashing to verify the integrity of cookies
   * sent by the user. If the framework receives a cookie from a user that is not
   * signed correctly, the framework will ignore that cookie (act as if it does
   * not exist).
   * 
   * If you wish to read cookies that are unsigned, use the unencryptedCookie(s)
   * methods.
   * 
   * @param name A cookie name.
   * @return 
   */
  public Param cookie(String name) {
    try {
      return Param.wrap(name, cookies.get(name));
    } catch (InsecureCookieException e) {
      return Param.wrap(name, null);
    }
  }
  
  /**
   * @return The underlying HttpServletRequest.
   */
  public HttpServletRequest raw() {
    return request;
  }
  
  /**
   * Fetches the value of an HTTP header.
   * @param header An HTTP header.
   * @return
   */
  public Param header(HTTPHeader header) {
    return header(header.getHeaderName());
  }
  
  /**
   * Fetches the value of an HTTP header.
   * @param headerName An HTTP header name.
   * @return
   */
  public Param header(String headerName) {
    return Param.wrap(headerName, request.getHeader(headerName));
  }
    
  /**
   * @return The method of the current request.
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
   * Fetches the value of a route parameter.
   * @param name A route parameter name (do not prefix with :).
   * @return
   */
  public Param routeParam(String name) {
    return Param.wrap(name, parameters.get(name));
  }
  
  /**
   * @return A set of all route parameter names.
   */
  public Set<String> routeParams() {
    return parameters.keySet();
  }
  
  /**
   * Returns the wildcard path of the matched route.
   * This is the path starting from the location of the wildcard.
   * For example, a route to "/u/*" for path "/u/my/page" would
   * reutrn "my/page"
   * @return
   */
  public String wildcardPath() {
    return Joiner.on("/").join(wildcardMatches);
  }
  
  /**
   * Returns the wildcard segments of the matched route.
   * For example, a route to "/u/*" for path "/u/my/page"
   * would return ["my", "page"].
   * @return
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
   * Returns the value of a query parameter.
   * A query parameter may be provided either through HTTP POST data or GET parameters.
   * @param name A query parameter name.
   * @return
   */
  public Param queryParam(String name) {    
    return Param.wrapList(name, request.getParameterValues(name));
  }
  
  /**
   * Returns the scheme (HTTP or HTTPS).
   * NOTE: Respects X-Forwarded-Proto if server.trustLoadBalancerHeaders is true.
   * @return
   */
  public HTTPScheme scheme() {
    if (trustLoadBalancerHeaders && header(HTTPHeader.X_FORWARDED_PROTO).exists()) {
      return parseScheme(header(HTTPHeader.X_FORWARDED_PROTO).stringValue());  
    }
    
    return parseScheme(request.getScheme());
  }
  
  private HTTPScheme parseScheme(String scheme) {
    switch (scheme.toLowerCase()) {
      case "http": return HTTPScheme.HTTP;
      case "https": return HTTPScheme.HTTPS;
      default: return HTTPScheme.UNKNOWN;
    }
  }

  /**
   * NOTE: Prefer using cookies().
   * @return A map of all cookie names to cookie values.
   */
  public Map<String, String> unencryptedCookies() {
    // TODO: Make O(1) by caching
    Map<String, String> result = new HashMap<String, String>();
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            result.put(cookie.getName(), cookie.getValue());
        }
    }
    return result;
  }

  /**
   * NOTE: Prefer using cookie(name).
   * @param name A cookie name.
   * @return The value of the cookie with given name, or null if it doesn't exist.
   */
  public String unencryptedCookie(String name) {
    // TODO: Make O(1) by caching
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }
    }
    return null;
  }

  /**
   * @return The host name attached to the request.
   */
  public String host() {
    return request.getHeader(HTTPHeader.HOST.getHeaderName());
  }

  /**
   * NOTE: Respectes X-Forwarded-For if server.setLoadBalancerHeaders is true.
   * @return The IP address of the client.
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
    return request.getParameterMap().keySet();
  }
  
  /**
   * @return Whether or not the request was received securely.
   */
  public boolean isSecure() {
    return scheme().isSecure();
  }
  
  /**
   * @return Content type of incoming request (maybe null).
   */
  public String contentType() {
    return request.getContentType();
  }
  
  /**
   * @return the part of this request's URL from the protocol name up to the query string in the first line of the HTTP request.
   */
  public String uri() {
      return request.getRequestURI();
  }
}
