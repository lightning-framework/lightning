package lightning.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Defines functionality for dealing with times as UNIX time (in seconds).
 */
public class Time {
  static {
    TimeZone.setDefault(TimeZone.getTimeZone("US/Central"));
  }
  
  /**
   * @return The current timestamp.
   */
  public static long now() {
    return Instant.now().getEpochSecond();
  }
  
  /**
   * Returns the timestamp for a Date.
   * @param date A date object.
   * @return A timestamp.
   */
  public static long fromDate(Date date) {
    return date.getTime() / 1000;
  }
  
  /**
   * Returns the Date object for a timestamp.
   * @param timestamp A timestamp.
   * @return A date object for a timestamp.
   */
  public static Date toDate(long timestamp) {
    return new Date(timestamp * 1000);
  }
  
  /**
   * Formats a timestamp.
   * @param timestamp A timestamp.
   * @return A human-readable timestamp.
   */
  public static String format(Long timestamp) {
    if (timestamp == null) {
      return "-";
    }
    
    SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss a z");
    sdf.setTimeZone(TimeZone.getTimeZone("US/Central"));
    return sdf.format(toDate(timestamp));
  }
  
  public static String formatForHttp(long timestamp) {
    SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    return sdf.format(new Date(timestamp * 1000));
  }
  
  public static long parseFromHttp(String timestamp) throws ParseException {
    SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    return Time.fromDate(format.parse(timestamp));
  }
  
  /**
   * Formats the difference between two timestamps.
   * @param timestampA A timestamp.
   * @param timestampB A timestamp.
   * @return The formatted time difference (e.g. '2hr').
   */
  public static String formatDifference(long timestampA, long timestampB) {
    long seconds = (timestampA > timestampB) 
        ? timestampA - timestampB 
        : timestampB - timestampA;
    
    if (seconds < 60) {
      return Long.toString(seconds) + " sec";
    }
    
    if (seconds < 3600) {
      return Long.toString(seconds / 60) + " min";
    }
    
    if (seconds < 86400) {
      return Long.toString(seconds / 3600) + " hr";
    }
    
    if (seconds < (86400 * 365)) {
      return Long.toString(seconds / 86400) + " day";
    }

    return Long.toString(seconds / (86400 * 365)) + " yr";
  }
}
