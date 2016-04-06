package lightning.http;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import lightning.crypt.SecureCookieManager;
import lightning.enums.HTTPHeader;
import lightning.enums.HTTPStatus;
import lightning.util.Mimes;

public class Response {
  private final HttpServletResponse response;
  protected SecureCookieManager cookies;
  
  public Response(HttpServletResponse response) {
    this.response = response;
    cookies = null;
  }
  
  public HttpServletResponse raw() {
    return response;
  }
  
  public void status(HTTPStatus status) {
    status(status.getCode());
  }
  
  public void status(int status) {
    response.setStatus(status);
  }
  
  public void write(String text) throws IOException {
    response.getWriter().write(text);
  }

  /**
   * Sets up a redirect to the given url.
   * @param logoutUrl
   * @param statusCode
   */
  public void redirect(String url, int statusCode) {
    status(HTTPStatus.FOUND);
    header(HTTPHeader.LOCATION, url);
  }
  
  public void redirect(String url, HTTPStatus statusCode) {
    redirect(url, statusCode.getCode());
  }
  
  public void redirect(String url) {
    redirect(url, HTTPStatus.FOUND);
  }
  
  /**
   * Removes an HTTP cookie (by overwriting it with a new blank cookie that expires immediately).
   * Be aware that all clients may not support cookies, or may not have them enabled.
   * @param name
   */
  public void removeCookie(String name) {
    Cookie cookie = new Cookie(name, "");
    cookie.setMaxAge(0);
    response.addCookie(cookie);
  }
  
  /**
   * Sets an HTTP cookie.
   * Be aware that all clients may not support cookies, or may not have them enabled.
   * Cookie will be signed with a cryptographic hash (HMAC) to guarantee integrity.
   * Secure only will automatically be set if SSL is enabled in your config.
   * 
   * Want to set unsigned cookies? -> Use the HttpServletResponse API directly.
   * 
   * @param name
   * @param value
   * @param path
   * @param maxAgeSec
   * @param httpOnly
   */
  public void setCookie(String name, String value, String path, int maxAgeSec, boolean httpOnly) {
    cookies.set(name, value, path, maxAgeSec, httpOnly);
  }
  
  /**
   * See above.
   */
  public void setCookie(String name, String value) {
    cookies.set(name, value);
  }
  
  /**
   * See above.
   */
  public void setCookie(String name, String value, int maxAgeSec) {
    setCookie(name, value, "/", maxAgeSec, true);
  }

  public void type(String contentType) {
    response.setContentType(contentType);
  }
  
  public void typeForFileExtension(String extension) {
    type(Mimes.forExtension(extension));
  }

  public void header(String header, String value) {
    response.addHeader(header, value);
  }
  
  public void header(String header, long value) {
    header(header, Long.toString(value));
  }
  
  public void header(HTTPHeader header, String value) {
    header(header.getHeaderName(), value);
  }
  
  public void header(HTTPHeader header, long value) {
    header(header.getHeaderName(), value);
  }
  
  public Writer getWriter() throws IOException {
    return response.getWriter();
  }  
  
  public ServletOutputStream getOutputStream() throws IOException {
    return response.getOutputStream();
  }

  public ServletOutputStream outputStream() throws IOException {
    return response.getOutputStream();
  }
}
