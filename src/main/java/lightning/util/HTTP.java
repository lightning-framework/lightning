package lightning.util;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.io.IOUtils;

/**
 * Provides quick macros for fetching data over HTTP.
 * Supports SSL.
 */
public class HTTP {
  /**
   * @param data A map of POST parameters.
   * @return The bytes of the x-www-form-urlencoded parameter map.
   * @throws Exception
   */
  private static byte[] buildUrlParametersBytes(Map<String, Object> data) throws Exception {
    return buildUrlParametersString(data).getBytes("UTF-8");
  }

  /**
   * @param data A map of POST parameters.
   * @return The String-encoded x-www-form-urlencoded parameter map.
   * @throws Exception
   */
  private static String buildUrlParametersString(Map<String, Object> data) throws Exception {
    StringBuilder postData = new StringBuilder();

    for (Map.Entry<String, Object> param : data.entrySet()) {
        if (postData.length() != 0) {
          postData.append('&');
        }

        postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
        postData.append('=');
        postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
    }

    return postData.toString();
  }

  /**
   * @param URL A url.
   * @return The response body as a string.
   * @throws Exception On network failure or non-200 response.
   */
  public static String GET(String URL) throws Exception {
    URL url = new URL(URL);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setRequestMethod("GET");
    con.setRequestProperty("User-Agent", "java");

    int responseCode = con.getResponseCode();

    if (responseCode != 200) {
      throw new Exception("The server experienced an internal error.");
    }

    return IOUtils.toString(con.getInputStream(), Charset.defaultCharset());
  }

  /**
   * Executes a POST request on the given URL.
   * See http://stackoverflow.com/questions/2793150/using-java-net-urlconnection-to-fire-and-handle-http-requests.
   * @param URL To send request to.
   * @param data To send in the request (x-www-form-urlencoded).
   * @return The response body on success.
   * @throws Exception On network failure or non-200 response.
   */
  public static String POST(String URL, Map<String, Object> data) throws Exception {
    URL url = new URL(URL);
    byte[] postData = buildUrlParametersBytes(data);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setRequestMethod("POST");
    con.setRequestProperty("User-Agent", "java");
    con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    con.setRequestProperty("Content-Length", Integer.toString(postData.length));
    con.setDoOutput(true);
    con.getOutputStream().write(postData);

    int responseCode = con.getResponseCode();

    if (responseCode != 200) {
      throw new Exception("The server experienced an internal error.");
    }

    return IOUtils.toString(con.getInputStream(), Charset.defaultCharset());
  }
}
