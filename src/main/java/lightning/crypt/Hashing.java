package lightning.crypt;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

import com.google.common.base.Charsets;

/**
 * Provides convenient macros for hashing values.
 */
public class Hashing {
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static String sharedSecretKey = null;
  private static int hashCharLen = 0;
  
  public static void main(String[] args) throws Exception {
    Hashing.setSecretKey("swaggggg");
    
    System.out.println("HashCharLen = " + hashCharLen);
    
    String mySecret = generateToken(16, (x) -> false);
    System.out.println("SECRET = " + mySecret + " (" + mySecret.length() + ")");

    String signature = createSignature(mySecret);
    System.out.println("SIGNATURE = " + signature + " (" + signature.length() + ")");
    
    String mySignedSecret = sign(mySecret);
    System.out.println("SIGNED = " + mySignedSecret + " (" + mySignedSecret.length() + ")");
    
    String mySecret2 = verify(mySignedSecret);
    System.out.println("VERIFIED = " + mySecret2 + " (" + mySecret2.length() + ")");
  }

  /**
   * Sets the secret key used to encrypt cookies. Must be called before using SecureCookieManager.
   * Should be called once before starting Spark and must be called with the same value across all machines.
   * @param secretKey
   */
  public static void setSecretKey(String secretKey) {
    sharedSecretKey = secretKey;
    
    // Update the character length of the hash algorithm output.
    hashCharLen = createSignature("UNUSED").length(); 
  }
  
  private static void requireKey() {
    if (sharedSecretKey == null) {
      throw new IllegalStateException("Error: Must call Hashing.setSecretKey before use.");
    }
  }
  
  @FunctionalInterface
  public static interface InUseChecker {
    public boolean isInUse(String token) throws Exception;
  }
  
  /**
   * Hashes a token, returning an encoded string.
   * @param plaintextValue Raw authentication token
   * @return Hashed authentication token.
   */
  public static String hash(String plaintextToken) {
    return com.google.common.hash.Hashing.sha256().hashString(plaintextToken, Charsets.UTF_8).toString();
  }
  
  /**
   * Generates a random token.
   * @param numBytes The number of bytes
   * @param checker Checks whether or not a token is in use.
   * @return Raw (un-encrypted) token.
   */
  public static String generateToken(int numBytes, InUseChecker checker) throws Exception {
    while (true) {
      SecureRandom generator = new SecureRandom();
      byte bytes[] = new byte[numBytes];
      generator.nextBytes(bytes);
      String token = Hex.encodeHexString(bytes);
      if (checker.isInUse(token)) {
        continue; // Pick again.
      }
      
      return token;
    }
  }
  
  /**
   * @param hash A signed value.
   * @return The raw value.
   * @throws HashVerificationException If not valid.
   */
  public static String verify(String hash) throws HashVerificationException {
    requireKey();
    if (hash == null) {
      throw new HashVerificationException("No hash found.");
    }
    
    // Ensure a signature can be present.
    if (hash.length() < hashCharLen) {
      throw new HashVerificationException("No signature found (too short).");
    }
    
    // Split the plain text value and signature.
    String plaintext = hash.substring(0, hash.length() - hashCharLen);
    String signature = hash.substring(hash.length() - hashCharLen, hash.length());
    
    // Verify the signature.
    if (!verifySignature(signature, plaintext)) {
      throw new HashVerificationException(String.format("Signature is incorrect: got %d, expected %d.",
          hashCharLen, sign(plaintext).length()));
    }
    
    // Return the plain text value iff the signature was valid.
    return plaintext;
  }
  
  public static final class HashVerificationException extends Exception {
    private static final long serialVersionUID = 1L;

    public HashVerificationException(String message) {
      super(message);
    }
  }
  
  /**
   * @param plaintext A plain text string to hash.
   * @return A signed version of plain text.
   */
  public static String sign(String plaintext) {
    requireKey();
    return plaintext + createSignature(plaintext);
  }
  
  /**
   * Signs a plain text value with an HMAC hash using secretKey.
   * @param plaintext The plain text value to be signed.
   * @return The base-64 encoded hash signature.
   */
  private static String createSignature(String plaintext) {
    requireKey();
    SecretKeySpec key = new SecretKeySpec(sharedSecretKey.getBytes(), HMAC_ALGORITHM);
    Mac mac;
    
    try {
      mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(key);
    } catch (InvalidKeyException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e); // Shouldn't happen.
    }
    
    byte[] rawHmac = mac.doFinal(plaintext.getBytes());
    return Hex.encodeHexString(rawHmac);
  }
  
  /**
   * Verifies the signature on a user-provided piece of plain text.
   * @param signature A base-64 encoded hash signature.
   * @param plaintext The alleged plain text value.
   * @return Whether or not the signature for (plain text, secretKey) matches the provided signature.
   */
  private static boolean verifySignature(String signature, String plaintext) {
    requireKey();
    return createSignature(plaintext).equals(signature);
  }
}
