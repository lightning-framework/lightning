package lightning.http;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public class InternalRequest extends Request {
  public InternalRequest(HttpServletRequest request) {
    super(request);
  }

  public static InternalRequest makeRequest(HttpServletRequest request) {
    return new InternalRequest(request);
  }
  
  public void setWildcards(List<String> wildcards) {
    this.wildcardMatches = wildcards;
  }
  
  public void setParams(Map<String, String> params) {
    this.parameters = params;
  }
}
