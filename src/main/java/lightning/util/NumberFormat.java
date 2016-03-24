package lightning.util;

import java.text.DecimalFormat;

/**
 * Provides various utility functions for formatting numbers.
 */
public class NumberFormat {
  private final static String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
  
  /**
   * Formats a size (in bytes) into human-readable string.
   * @param bytes
   * @return A human readable string e.g. "2 KB"
   */
  public static String formatFileSize(long bytes) {
    if (bytes <= 0) {
      return "0";
    }
    
    int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
    return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
  }
}
