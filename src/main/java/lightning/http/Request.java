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

import lightning.enums.HTTPHeader;
import lightning.enums.HTTPMethod;
import lightning.mvc.Param;

public class Request {  
  protected final HttpServletRequest request;
  protected List<String> wildcardMatches;
  protected Map<String, String> parameters;
  
  public Request(HttpServletRequest request) {
    this.request = request;
    wildcardMatches = ImmutableList.of();
    parameters = ImmutableMap.of();
  }
  
  public HttpServletRequest raw() {
    return request;
  }
  
  public Param header(HTTPHeader header) {
    return header(header.getHeaderName());
  }
  
  public Param header(String headerName) {
    return Param.wrap(headerName, request.getHeader(headerName));
  }
  
  public HTTPMethod method() {
    return HTTPMethod.valueOf(request.getMethod().toUpperCase());
  }
  
  public String path() {
    return request.getPathInfo();
  }
  
  public Param routeParams(String name) {
    return Param.wrap(name, parameters.get(name));
  }
  
  public Iterable<String> routeParams() {
    return parameters.keySet();
  }
  
  public String wildcardPath() {
    return Joiner.on("/").join(wildcardMatches);
  }
  
  public List<String> wildcards() {
    return Collections.unmodifiableList(wildcardMatches);
  }

  public String url() {
    return request.getRequestURL().toString();
  }

  public Param queryParams(String name) {
    return Param.wrap(name, request.getParameter(name));
  }

  public String scheme() {
    return request.getScheme();
  }

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

  public String host() {
    return request.getHeader(HTTPHeader.HOST.getHeaderName());
  }

  public String ip() {
    return request.getRemoteAddr();
  }

  public String requestMethod() {
    return request.getMethod();
  }

  public Set<String> queryParams() {
    return request.getParameterMap().keySet();
  }

  public String headers(String header) {
    return request.getHeader(header);
  }
}
