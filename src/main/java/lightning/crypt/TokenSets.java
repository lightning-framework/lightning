package lightning.crypt;

import lightning.crypt.Hashing.HashVerificationException;

import com.google.common.base.Optional;

/**
 * A TokenSet contains three things:
 *  - An unencrypted, random token
 *  - A hmac-signed token that should be sent to/stored with the client
 *  - A one-way sha256 hashed token that should be stored with the server
 */
public class TokenSets {
  public static TokenSet fromClientToken(String clientToken) {
    return new TokenSet(clientToken);
  }
  
  public static Optional<TokenSet> fromSignedClientToken(String signedClientToken) {
    try {
      String clientToken = Hashing.verify(signedClientToken);
      return Optional.of(new TokenSet(clientToken));
    } catch (HashVerificationException e) {
      return Optional.absent();
    }
  }
  
  public static TokenSet createNew() throws Exception {
    return new TokenSet(Hashing.generateToken(64, (x) -> false));
  }
  
  public static class TokenSet {
    private String clientToken;
    
    private TokenSet(String clientToken) {
      this.clientToken = clientToken;
    }
    
    public String getClientToken() {
      return clientToken;
    }
    
    public String getSignedClientToken() {
      return Hashing.sign(clientToken);
    }
    
    public String getServerToken() {
      return Hashing.hash(clientToken);
    }
  }
}
