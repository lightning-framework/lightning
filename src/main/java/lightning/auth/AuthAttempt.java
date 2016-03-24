package lightning.auth;

/**
 * An immutable structure that holds information about an authentication attempt.
 */
public final class AuthAttempt {
  public final boolean wasSuccessful;
  public final boolean wasFraudulent;
  public final String ip;
  public final long timestamp;
  public final long userId;
  
  public AuthAttempt(boolean wasSuccessful, boolean wasFraudulent, String ip, long timestamp, long userId) {
    this.wasSuccessful = wasSuccessful;
    this.wasFraudulent = wasFraudulent;
    this.ip = ip;
    this.timestamp = timestamp;
    this.userId = userId;
  }
  
  public static AuthAttempt createFailed(String ip, long timestamp) {
    return new AuthAttempt(false, false, ip, timestamp, 0);
  }
  
  public static AuthAttempt createSuccessful(String ip, long timestamp) {
    return new AuthAttempt(true, false, ip, timestamp, 0);
  }
  
  public static AuthAttempt createFraudulent(String ip, long timestamp) {
    return new AuthAttempt(false, true, ip, timestamp, 0);
  }
  
  public static AuthAttempt createFailed(long userId, String ip, long timestamp) {
    return new AuthAttempt(false, false, ip, timestamp, userId);
  }
  
  public static AuthAttempt createSuccessful(long userId, String ip, long timestamp) {
    return new AuthAttempt(true, false, ip, timestamp, userId);
  }
  
  public static AuthAttempt createFraudulent(long userId, String ip, long timestamp) {
    return new AuthAttempt(false, true, ip, timestamp, userId);
  }
}
