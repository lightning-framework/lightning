package lightning.crypt;

import lightning.crypt.Hasher.HashVerificationException;

import com.google.common.base.Optional;

/**
 * Provides utility methods for generating verification tokens.
 */
public class TokenSets {
  private final Hasher hasher;
  
  public TokenSets(Hasher hasher) {
    this.hasher = hasher;
  }
  
  /**
   * Creates a token set from a raw client token.
   * @param clientToken
   * @return
   */
  public TokenSet fromClientToken(String clientToken) {
    return new TokenSet(clientToken, hasher);
  }
  
  /**
   * Attempts to create a token set from a signed client token.
   * @param signedClientToken
   * @return
   */
  public Optional<TokenSet> fromSignedClientToken(String signedClientToken) {
    try {
      String clientToken = hasher.verify(signedClientToken);
      return Optional.of(new TokenSet(clientToken, hasher));
    } catch (HashVerificationException e) {
      return Optional.absent();
    }
  }
  
  /**
   * @return A new randomly generated token set.
   * @throws Exception
   */
  public TokenSet createNew() throws Exception {
    return new TokenSet(Hasher.generateToken(64, (x) -> false), hasher);
  }
  
  /**
   * Represents a set of affiliated tokens.
   */
  public static class TokenSet {
    private String clientToken;
    private final Hasher hasher;
    
    private TokenSet(String clientToken, Hasher hasher) {
      this.clientToken = clientToken;
      this.hasher = hasher;
    }
    
    /**
     * @return An unencrypted, raw version of the token.
     */
    public String getClientToken() {
      return clientToken;
    }
    
    /**
     * @return An unencrypted, raw version of the token signed with the server's HMAC key.
     */
    public String getSignedClientToken() {
      return hasher.sign(clientToken);
    }
    
    /**
     * @return A one-way encrypted version of the token.
     */
    public String getServerToken() {
      return Hasher.hash(clientToken);
    }
  }
}
