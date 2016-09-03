package lightning.http;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;

import lightning.crypt.SecureCookieManager;
import lightning.enums.HTTPHeader;
import lightning.enums.HTTPStatus;
import lightning.util.Mimes;

/**
 * Lightning's representation of an HTTP response.
 */
public class Response {
  protected final LightningHttpServletResponse response;
  protected SecureCookieManager cookies;
  
  public Response(LightningHttpServletResponse response) {
    this.response = response;
    cookies = null;
  }
  
  /**
   * @return Whether or not HTTP headers have already been sent.
   */
  public boolean hasSentHeaders() {
    return response.isCommitted();
  }
  
  /**
   * @return The underlying Jetty response.
   */
  public LightningHttpServletResponse raw() {
    return response;
  }
  
  /**
   * @return Whether or not output buffering is enabled on this response.
   */
  public boolean isBuffered() {
    return response.isBuffered();
  }
  
  /**
   * Sets the HTTP response status.
   * @param status The status code.
   * @throws HeadersAlreadySentException 
   */
  public void status(HTTPStatus status) throws HeadersAlreadySentException {
    status(status.getCode());
  }
  
  /**
   * Sets the HTTP response status.
   * @param status The status code.
   * @throws HeadersAlreadySentException 
   */
  public void status(int status) throws HeadersAlreadySentException {
    if (hasSentHeaders()) {
      throw new HeadersAlreadySentException();
    }
    
    response.setStatus(status);
  }
  
  /**
   * Writes the given text to the HTTP response.
   * @param text To write.
   * @throws IOException
   */
  public void write(String text) throws IOException {
    response.getWriter().write(text);
  }

  /**
   * Sets up a redirect to the given URL with the given status code.
   * @param logoutUrl
   * @param statusCode
   * @throws HeadersAlreadySentException 
   */
  public void redirect(String url, int statusCode) throws HeadersAlreadySentException {
    status(statusCode);
    header(HTTPHeader.LOCATION, url);
  }
  
  /**
   * Sets up a redirect to the given URL with the given status code.
   * @param url
   * @param statusCode
   * @throws HeadersAlreadySentException
   */
  public void redirect(String url, HTTPStatus statusCode) throws HeadersAlreadySentException {
    redirect(url, statusCode.getCode());
  }
  
  /**
   * Sets up a redirect to the given URL with HTTP 302 Found status code.
   * @param url
   * @throws HeadersAlreadySentException
   */
  public void redirect(String url) throws HeadersAlreadySentException {
    redirect(url, HTTPStatus.FOUND);
  }
  
  public void rawCookie(String name, String value, String path, int maxAgeSec, boolean httpOnly, boolean secureOnly) throws HeadersAlreadySentException {
    if (hasSentHeaders()) {
      throw new HeadersAlreadySentException();
    }
    
    Cookie cookie = new Cookie(name, value);
    cookie.setPath(path);
    cookie.setMaxAge(maxAgeSec);
    cookie.setHttpOnly(httpOnly);
    cookie.setSecure(secureOnly);
    response.addCookie(cookie);
  }
  
  /**
   * Removes an HTTP cookie (by overwriting it with a new blank cookie that expires immediately).
   * Be aware that all clients may not support cookies, or may not have them enabled.
   * @param name
   */
  public void removeCookie(String name) throws HeadersAlreadySentException {
    if (hasSentHeaders()) {
      throw new HeadersAlreadySentException();
    }
    
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
  public void cookie(String name, String value, String path, int maxAgeSec, boolean httpOnly) throws HeadersAlreadySentException {
    cookies.set(name, value, path, maxAgeSec, httpOnly);
  }
  
  /**
   * See above.
   */
  public void cookie(String name, String value) throws HeadersAlreadySentException {
    cookies.set(name, value);
  }
  
  /**
   * See above.
   */
  public void cookie(String name, String value, int maxAgeSec) throws HeadersAlreadySentException {
    cookie(name, value, "/", maxAgeSec, true);
  }

  /**
   * Sets the Content-Type header.
   * @param contentType
   */
  public void type(String contentType) throws HeadersAlreadySentException {
    if (hasSentHeaders()) {
      throw new HeadersAlreadySentException();
    }
    
    response.setContentType(contentType);
  }
  
  /**
   * Sets the Content-Type based on the given file extension.
   * @param extension
   */
  public void typeForFileExtension(String extension) throws HeadersAlreadySentException {
    type(Mimes.forExtension(extension));
  }
  
  /**
   * Sets the Content-Type based on the given file path.
   * @param extension
   */
  public void typeForFilePath(String path) throws HeadersAlreadySentException {
    type(Mimes.forPath(path));
  }

  /**
   * Sets an HTTP header.
   * @param header
   * @param value
   * @throws HeadersAlreadySentException
   */
  public void header(String header, String value) throws HeadersAlreadySentException {
    if (hasSentHeaders()) {
      throw new HeadersAlreadySentException();  
    }
    
    response.addHeader(header, value);
  }
  
  /**
   * Sets an HTTP header.
   * @param header
   * @param value
   * @throws HeadersAlreadySentException
   */
  public void header(String header, long value) throws HeadersAlreadySentException {
    header(header, Long.toString(value));
  }
  
  /**
   * Sets an HTTP header.
   * @param header
   * @param value
   */
  public void header(HTTPHeader header, String value) throws HeadersAlreadySentException {
    header(header.getHeaderName(), value);
  }
  
  /**
   * Sets an HTTP header.
   * @param header
   * @param value
   * @throws HeadersAlreadySentException
   */
  public void header(HTTPHeader header, long value) throws HeadersAlreadySentException {
    header(header.getHeaderName(), value);
  }
  
  /**
   * @return A writer which wraps the output stream to the response body.
   * @throws IOException
   */
  public Writer getWriter() throws IOException {
    return response.getWriter();
  }  
  
  /**
   * @return A writer which wraps the output stream to the response body.
   * @throws IOException
   */
  public Writer writer() throws IOException {
    return getWriter();
  }
  
  /**
   * @return An output stream to write to the response body.
   * @throws IOException
   */
  public ServletOutputStream getOutputStream() throws IOException {
    return response.getOutputStream();
  }

  /**
   * @return An output stream to write to the response body.
   * @throws IOException
   */
  public ServletOutputStream outputStream() throws IOException {
    return response.getOutputStream();
  }
}
