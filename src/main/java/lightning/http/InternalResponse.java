package lightning.http;

import lightning.crypt.SecureCookieManager;

/**
 * Lightning's internal representation of an HTTP response.
 * Used internally to prevent users from referencing these APIs.
 */
public class InternalResponse extends Response {
  public InternalResponse(LightningHttpServletResponse response) {
    super(response);
  }

  public static InternalResponse makeResponse(LightningHttpServletResponse response) {
    return new InternalResponse(response);
  }
  
  public void setCookieManager(SecureCookieManager manager) {
    this.cookies = manager;
  }
}
