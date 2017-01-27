package lightning.mvc;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import lightning.enums.HTTPScheme;
import lightning.http.Request;
import lightning.util.Iterables;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Joiner;

// TODO(mschurr): It'd be cool if we could do reverse routing (e.g. generate URL for a class).
public class URLGenerator {
  private final HTTPScheme scheme;
  private final String host;

  public static URLGenerator forRequest(Request request) {
    return new URLGenerator(request.scheme(), request.host());
  }

  private URLGenerator(HTTPScheme scheme, String host) {
    this.scheme = scheme;
    this.host = host;
  }

  public String to(String path) {
    path = StringUtils.strip(path, "/");
    return scheme + "://" + host + "/" + path + (path.length() > 0 ? "/" : "");
  }

  public String to(String path, Map<String, Object> params) {
    return to(path) + "?" + buildParamString(params);
  }

  private String buildParamString(Map<String, Object> params) {
    return Joiner.on("&").join(Iterables.map(params.entrySet(), (e) -> encode(e.getKey()) + "=" + encode(e.getValue().toString())));
  }

  private String encode(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return s; // Fall-back (shouldn't happen).
    }
  }
}
