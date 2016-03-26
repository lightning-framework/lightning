package lightning.http;

import javax.servlet.http.HttpServletResponse;

public class InternalResponse extends Response {
  public InternalResponse(HttpServletResponse response) {
    super(response);
  }

  public static InternalResponse makeResponse(HttpServletResponse response) {
    return new InternalResponse(response);
  }
}
