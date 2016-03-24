package lightning.util;

import com.google.common.collect.ImmutableMap;

/**
 * Provides functionality for dealing with Internet MIME types.
 */
public class Mimes {
  private static final ImmutableMap<String, String> EXT_TO_MIME = ImmutableMap.<String, String>builder()
      .put("txt", "text/plain")
      .put("css", "text/css")
      .put("js", "application/javascript")
      .put("png", "image/png")
      .put("jpg", "image/jpeg")
      .put("jpeg", "image/jpeg")
      .put("gif", "image/gif")
      .put("json", "application/json")
      .put("svg", "image/svg+xml")
      .build();
  
  private static final String DEFAULT_MIME = "application/octet-stream";
  
  /**
   * @param extension A file extension (e.g. 'css').
   * @return The MIME type for the given file extension (defaults to binary).
   */
  public static String forExtension(String extension) {
    return EXT_TO_MIME.getOrDefault(extension.toLowerCase(), DEFAULT_MIME);
  }
}
