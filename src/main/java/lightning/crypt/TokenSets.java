package lightning.crypt;

import lightning.crypt.Hasher.HashVerificationException;

import com.google.common.base.Optional;

/**
 * A TokenSet contains three things:
 *  - An unencrypted, random token
 *  - A hmac-signed token that should be sent to/stored with the client
 *  - A one-way sha256 hashed token that should be stored with the server
 */
public class TokenSets {
  private final Hasher hasher;
  
  public TokenSets(Hasher hasher) {
    this.hasher = hasher;
  }
  
  public TokenSet fromClientToken(String clientToken) {
    return new TokenSet(clientToken, hasher);
  }
  
  public Optional<TokenSet> fromSignedClientToken(String signedClientToken) {
    try {
      String clientToken = hasher.verify(signedClientToken);
      return Optional.of(new TokenSet(clientToken, hasher));
    } catch (HashVerificationException e) {
      return Optional.absent();
    }
  }
  
  public TokenSet createNew() throws Exception {
    return new TokenSet(Hasher.generateToken(64, (x) -> false), hasher);
  }
  
  public static class TokenSet {
    private String clientToken;
    private final Hasher hasher;
    
    private TokenSet(String clientToken, Hasher hasher) {
      this.clientToken = clientToken;
      this.hasher = hasher;
    }
    
    public String getClientToken() {
      return clientToken;
    }
    
    public String getSignedClientToken() {
      return hasher.sign(clientToken);
    }
    
    public String getServerToken() {
      return Hasher.hash(clientToken);
    }
  }
}
