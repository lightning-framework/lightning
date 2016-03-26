package lightning.http;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import lightning.enums.HTTPHeader;
import lightning.enums.HTTPStatus;

public class Response {
  private final HttpServletResponse response;
  
  public Response(HttpServletResponse response) {
    this.response = response;
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

  public void redirect(String logoutUrl, int statusCode) {
    response.setStatus(statusCode);
    response.setHeader(HTTPHeader.LOCATION.getHeaderName(), logoutUrl);
  }

  public void removeCookie(String name) {
    Cookie cookie = new Cookie(name, "");
    cookie.setMaxAge(0);
    response.addCookie(cookie);
  }

  public void type(String contentType) {
    response.setContentType(contentType);
  }

  public void header(String header, String value) {
    response.addHeader(header, value);
  }
}
