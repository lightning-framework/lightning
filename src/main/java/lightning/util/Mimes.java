package lightning.util;

import org.apache.commons.io.FilenameUtils;

import com.google.common.collect.ImmutableMap;

/**
 * Provides functionality for dealing with Internet MIME types.
 */
public class Mimes {
  private static final ImmutableMap<String, String> EXT_TO_MIME;
  
  static {
    EXT_TO_MIME = ImmutableMap.<String, String>builder()
      .put("txt", "text/plain")
      .put("css", "text/css")
      .put("html", "text/html")
      .put("js", "application/javascript")
      .put("png", "image/png")
      .put("jpg", "image/jpeg")
      .put("jpeg", "image/jpeg")
      .put("gif", "image/gif")
      .put("svg", "image/svg+xml")
      .put("pdf", "application/pdf")
      .put("zip", "application/zip")
      .put("rar", "application/x-rar-compressed")
      .put("ttf", "application/x-font-ttf")
      .put("woff", "application/font-woff")
      .put("eot", "application/vnd.ms-fontobject")
      .put("otf", "application/octet-stream")
      .put("ico", "image/vnd.microsoft.icon")
      .put("swf", "application/x-shockwave-flash")
      .put("doc", "application/msword")
      .put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
      .put("xls", "application/vnd.ms-excel")
      .put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
      .put("csv", "text/csv")
      .put("ppt", "application/vnd.ms-powerpoint")
      .put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation")
      .put("xml", "application/xml")
      .put("xslt", "application/xslt+xml")
      .put("py", "application/x-python")
      .put("php", "text/plain")
      .put("log", "text/plain")
      .put("json", "application/json")
      .build();
  }
  
  private static final String DEFAULT_MIME = "application/octet-stream";
  
  /**
   * @param extension A file extension (e.g. 'css').
   * @return The MIME type for the given file extension (defaults to binary).
   */
  public static String forExtension(String extension) {
    return EXT_TO_MIME.getOrDefault(extension.toLowerCase(), DEFAULT_MIME);
  }
  
  public static String forPath(String path) {
    return forExtension(FilenameUtils.getExtension(path));
  }
}
