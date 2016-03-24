package lightning.util;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Provides utility functions for manipulating HTML code.
 */
public class HTML {
  /**
   * Escapes an HTML string, replacing characters which have significance in
   * HTML with their HTML entity equivalents.
   * @param html
   * @return
   */
  public static String escape(String html) {
    return StringEscapeUtils.escapeHtml4(html);
  }
  
  /**
   * Un-escapes an HTML string, replacing HTML entities with their single
   * character equivalents.
   * @param escapedHtml
   * @return
   */
  public static String unescape(String escapedHtml) {
    return StringEscapeUtils.unescapeHtml4(escapedHtml);
  }
  
  /**
   * Given HTML code and CSS code, applies the CSS to the HTML to produce a version
   * of the HTML with equivalent appearance by in-lining all of the styles. Useful
   * for formatting web mail since the majority of clients do not support style and
   * link tags.
   * @param htmlCode HTML source code.
   * @param cssCode CSS source code.
   * @return The input HTML code with the styles from the provided CSS attached to
   *         elements in-line view the style attribute.
   */
  public static String inlineCss(String htmlCode, String cssCode) {
    // TODO: Implement this.
    throw new RuntimeException("Operation is not yet implemented.");
  }
}
