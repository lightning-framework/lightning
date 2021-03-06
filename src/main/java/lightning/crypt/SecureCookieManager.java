package lightning.crypt;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lightning.enums.HTTPScheme;
import lightning.http.HeadersAlreadySentException;
import lightning.http.Request;
import lightning.http.Response;
import lightning.mvc.Param;

/**
 * Provides functionality for reading and writing secure HTTP cookies.
 * A 'secure' cookie is flagged with HTTPONLY, SECUREONLY, and signed with HMAC SHA256.
 * Only cookies that are not tampered with can read back via has(name) and get(name).
 * Can be used to safely set authentication and session cookies.
 */
public class SecureCookieManager {
  private static final Logger logger = LoggerFactory.getLogger(SecureCookieManager.class);
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final int LIFETIME_SECONDS = 60 * 60 * 24 * 14;
    
  /**
   * Signs a plain text value with an HMAC hash using secretKey.
   * @param plaintext The plain text value to be signed.
   * @param secretKey A private key for use with HMAC.
   * @return The base-64 encoded hash signature.
   */
  private static String sign(String plaintext, String secretKey) {
    SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), HMAC_ALGORITHM);
    Mac mac;
    
    try {
      mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(key);
    } catch (InvalidKeyException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e); // Shouldn't happen.
    }
    
    byte[] rawHmac = mac.doFinal(plaintext.getBytes());
    return Base64.getEncoder().encodeToString(rawHmac);
  }
  
  /**
   * Verifies the signature on a user-provided piece of plain text.
   * @param hash A base-64 encoded hash signature.
   * @param plaintext The alleged plain text value.
   * @param secretKey A private key to use with HMAC.
   * @return Whether or not the signature for (plain text, secretKey) matches the provided signature.
   */
  private static boolean verifySignature(String hash, String plaintext, String secretKey) {
    return sign(plaintext, secretKey).equals(hash);
  }
  
  /**
   * An exception that is thrown when attempting to read a secure cookie that does not exist or whose
   * signature is incorrect.
   */
  public static class InsecureCookieException extends Exception {
    private static final long serialVersionUID = 1L;

    public InsecureCookieException() { 
      super(); 
    }
    
    public InsecureCookieException(Exception e) {
      super(e);
    }
    
    public InsecureCookieException(String message) {
      super(message);
    }
  }
  
  /**
   * Creates and returns a new SecureCookieManager for the given (request, response) pair.
   * Requires a secret key to be set via setSecretKey() before calling.
   * @param request Spark HTTP request
   * @param response Spark HTTP response
   * @return A secure cookie manager for the given (request, response) pair.
   */
  public static SecureCookieManager forRequest(Request request, Response response, String secretKey, boolean alwaysSetSecureOnly) {
    if (secretKey == null) {
      throw new RuntimeException("Error: Must have set secret key before using SecureCookieManager.");
    }
    
    return new SecureCookieManager(request, response, secretKey, alwaysSetSecureOnly);
  }
  
  // ------------------------------------------------------------------------
  
  private final Request request;
  private final Response response;
  private int hashCharLen = 0;
  private String sharedSecretKey = null;
  private boolean alwaysSetSecureOnly = false;
  
  private SecureCookieManager(Request request, Response response, String secretKey, boolean alwaysSetSecureOnly) {
    this.request = request;
    this.response = response;
    this.sharedSecretKey = secretKey;
    this.alwaysSetSecureOnly = alwaysSetSecureOnly;
    this.hashCharLen = sign("UNUSED", secretKey).length(); 
  }
  
  /**
   * Creates and sets a cookie with the given name and value.
   * @param name Cookie name
   * @param value Cookie value (raw)
   */
  public void set(String name, String value)  throws HeadersAlreadySentException {
    set(name, value, "/", LIFETIME_SECONDS, true);
  }
  
  /**
   * Creates and sets a cookie with the given name and value.
   * @param name
   * @param value
   * @param path
   * @param maxAgeSec
   * @throws HeadersAlreadySentException
   */
  public void set(String name, String value, String path, int maxAgeSec) throws HeadersAlreadySentException {
    set(name, value, path, maxAgeSec, true);
  }
  
  /**
   * Creates and sets a cookie with the given name and value.
   * @param name
   * @param value
   * @param maxAgeSec
   * @throws HeadersAlreadySentException
   */
  public void set(String name, String value, int maxAgeSec) throws HeadersAlreadySentException {
    set(name, value, maxAgeSec);
  }
  
  /**
   * Creates and sets a cookie with the given name and value.
   * @param name
   * @param value
   * @param path
   * @param maxAgeSec
   * @param httpOnly
   */
  public void set(String name, String value, String path, int maxAgeSec, boolean httpOnly) throws HeadersAlreadySentException {
    if (response.hasSentHeaders()) {
      throw new HeadersAlreadySentException();
    }
    
    boolean secure = alwaysSetSecureOnly || request.scheme() == HTTPScheme.HTTPS;
    logger.debug("Setting Cookie: name={} path={} maxAge={} httpOnly={} secure={} value={}", name, path, maxAgeSec, httpOnly, secure, value);
    Cookie cookie = new Cookie(name, value + sign(name + value, sharedSecretKey));
    cookie.setPath(path);
    cookie.setMaxAge(maxAgeSec);
    cookie.setSecure(secure);
    cookie.setHttpOnly(httpOnly);
    response.raw().addCookie(cookie);
  }
  
  /**
   * Checks whether or not a cookie exists with that name.
   * Only returns true if the cookie was set by SecureCookieManager and the signature is valid.
   * @param name Cookie name.
   * @return Whether or not the request has a valid secure cookie with the given name.
   */
  public boolean has(String name) {
    try {
      get(name);
      return true;
    } catch (InsecureCookieException e) {
      return false;
    }
  }
  
  /**
   * Retrieves the raw value of a cookie that was set with SecureCookieManager.
   * Throws an exception if the cookie does not exist or its signature is invalid.
   * @param name Cookie name.
   * @return The value of the cookie (never null).
   * @throws InsecureCookieException On failure to find and validate the cookie.
   */
  public String get(String name) throws InsecureCookieException {
    Param value = request.rawCookie(name);
    
    // Ensure the cookie was set.
    if (!value.exists()) {
      throw new InsecureCookieException("No such cookie exists.");
    }
    
    // Ensure a signature can be present.
    if (value.stringValue().length() < hashCharLen) {
      throw new InsecureCookieException("No signature found.");
    }
    
    // Split the plain text value and signature.
    String plaintext = value.stringValue().substring(0, value.stringValue().length() - hashCharLen);
    String hash = value.stringValue().substring(value.stringValue().length() - hashCharLen, value.stringValue().length());
    
    // Verify the signature.
    if (!verifySignature(hash, name + plaintext, sharedSecretKey)) {
      throw new InsecureCookieException("Invalid signature.");
    }
    
    // Return the plain text value iff the signature was valid.
    return plaintext;
  }
  
  /**
   * @return Map of all valid cookies.
   */
  public Map<String, String> asMap() {
    Map<String, String> cookies = new HashMap<>();
    
    for (String cookie : request.rawCookies()) {
      try {
        cookies.put(cookie, get(cookie));
      } catch (InsecureCookieException e) {
        continue;
      }
    }
    
    return cookies;    
  }
  
  /**
   * @return Names of all valid cookies.
   */
  public Set<String> all() {
    return asMap().keySet();
  }
  
  /**
   * Deletes the cookie with the given name.
   * @param name Cookie name.
   */
  public void delete(String name) throws HeadersAlreadySentException {
    logger.debug("Deleting Cookie: name={}", name);
    response.removeCookie(name);
  }
}
