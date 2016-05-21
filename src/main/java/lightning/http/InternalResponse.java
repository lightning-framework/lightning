package lightning.http;

import javax.servlet.http.HttpServletResponse;

import lightning.crypt.SecureCookieManager;

/**
 * Lightning's internal representation of an HTTP response.
 * Used internally to prevent users from referencing these APIs.
 */
public class InternalResponse extends Response {
  public InternalResponse(HttpServletResponse response) {
    super(response);
  }

  public static InternalResponse makeResponse(HttpServletResponse response) {
    return new InternalResponse(response);
  }
  
  public void setCookieManager(SecureCookieManager manager) {
    this.cookies = manager;
  }
}
