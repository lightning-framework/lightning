package lightning.http;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import lightning.crypt.SecureCookieManager;

public class InternalRequest extends Request {
  public InternalRequest(HttpServletRequest request, boolean trustLoadBalancerHeaders) {
    super(request, trustLoadBalancerHeaders);
  }

  public static InternalRequest makeRequest(HttpServletRequest request, boolean trustLoadBalancerHeaders) {
    return new InternalRequest(request, trustLoadBalancerHeaders);
  }
  
  public void setWildcards(List<String> wildcards) {
    this.wildcardMatches = wildcards;
  }
  
  public void setParams(Map<String, String> params) {
    this.parameters = params;
  }
  
  public void setCookieManager(SecureCookieManager manager) {
    this.cookies = manager;
  }
}
