package lightning.auth;

/**
 * An immutable representation of an authentication token.
 */
public final class AuthToken {
  public final String hashedToken;
  public final long userId;
  public final long expireTime;
  
  public AuthToken(String hashedToken, long userId, long expireTime) {
    this.hashedToken = hashedToken;
    this.userId = userId;
    this.expireTime = expireTime;
  }
  
  public AuthToken(String hashedToken) {
    this.hashedToken = hashedToken;
    this.userId = -1;
    this.expireTime = 0;
  }
}
