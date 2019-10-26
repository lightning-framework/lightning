package lightning.sessions;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import lightning.config.Config;
import lightning.crypt.SecureCookieManager;
import lightning.crypt.SecureCookieManager.InsecureCookieException;
import lightning.http.HeadersAlreadySentException;
import lightning.http.Request;
import lightning.http.Response;
import lightning.mvc.ObjectParam;
import lightning.util.Iterables;
import lightning.util.Time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

/**
 * Implements a session system.
 * Improves upon native session functionality by adding extra security features and
 * (by allowing custom storage drivers) supporting distributed session management.
 *
 * Session are loaded and saved lazily (no operations on storage unless necessary).
 * Any type of Serializable object can be stored in a session.
 *
 * TODO: Session will not get saved automatically when user handler sends HTTP content before the
 * session is saved (since after content is sent the session manager is not able to write the session
 * ID cookie). Happens because HandlerContext is closed (which invokes session save) after the user
 * handler is invoked. Could be fixed by output buffering.
 * TODO: People that don't have cookies enabled will create tons of rows, need way to purge these first.
 *       Maybe by having a committed flag and dropping sessions not committed within X seconds?
 * TODO: we should optimize db access (e.g. if no session cookie exists -> no db hit)
 */
public final class Session {
  private static final Logger logger = LoggerFactory.getLogger(Session.class);
  private static final int SESSION_ID_BYTES = 128;
  private static final int XSRF_BYTES = 48;
  private static final String SESSION_COOKIE_NAME = "_sessiond";
  private static final String SESSION_KEY_PREFIX = "$$session-";
  private static final String LAST_USE_KEY = SESSION_KEY_PREFIX + "lastuse";
  private static final String XSRF_KEY = SESSION_KEY_PREFIX + "xsrf";

  private long inactivityTimeoutSeconds = 60 * 60 * 24 * 14; // TODO: An option for this.

  /**
   * Hashes a session token.
   * @param plaintextValue Raw session token.
   * @return Hashes session token.
   */
  private static String hashToken(String plaintextValue) {
    return Hashing.sha256().hashString(plaintextValue, Charsets.UTF_8).toString();
  }

  /**
   * @param request An incoming HTTP request.
   * @return A session object for that request.
   */
  public static Session forRequest(Request request, Response response, Config config, SessionStorageDriver driver) {
    if (driver == null) {
      throw new RuntimeException("Error: Must install a session driver before using Session.");
    }

    return new Session(request, response, config, driver);
  }

  /**
   * An exception thrown when the session library fails to complete an operation.
   */
  public static class SessionException extends Exception {
    private static final long serialVersionUID = 1L;

    public SessionException(Exception e) {
      super(e);
    }

    public SessionException(String message) {
      super(message);
    }
  }

  /**
   * An exception thrown when the session library fails to complete an operation.
   */
  public static class SessionDriverException extends SessionException {
    private static final long serialVersionUID = 1L;

    public SessionDriverException(Exception e) {
      super(e);
    }

    public SessionDriverException(String message) {
      super(message);
    }
  }

  /**
   * Defines an interface for storage session information.
   * The storage driver may choose to store extra metadata (e.g. last use times) and prune the
   * session database periodically.
   */
  public static interface SessionStorageDriver {
    /**
     * Fetches information about a session from the storage system.
     * @param hashedId Hashed session identifier.
     * @return Map of information attached to that session. Null if not found.
     * @throws SessionDriverException On failure.
     */
    public Map<String, Object> get(String hashedId) throws SessionDriverException;

    /**
     * Stores information about a session in the storage system. Only called when some differences exist.
     * @param hashedId Hashed session identifier.
     * @param data Map of information attached to that session.
     * @param changedKeys Set of keys in data that have changed.
     * @throws SessionDriverException On failure.
     */
    public void put(String hashedId, Map<String, Object> data, Set<String> changedKeys) throws SessionDriverException;

    /**
     * @param hashedId Hashed session identifier
     * @return Whether or not that session exists in DB.
     * @throws SessionDriverException On failure.
     */
    public boolean has(String hashedId) throws SessionDriverException;

    /**
     * Invalidates a session.
     * @param hashedId Hashed session identifier.
     * @throws SeessionDriverException On failure.
     */
    public void invalidate(String hashedId) throws SessionDriverException;

    /**
     * Keeps a session from timing out. The provided identifier may not correspond to an
     * identifier in storage; in this case, do nothing.
     * @param hashedId Hashed session identifier.
     * @throws SessionDriverException On failure.
     */
    public void keepAliveIfExists(String hashedId) throws SessionDriverException;
  }

  // -----------------------------------------------------------------

  private final SessionStorageDriver storage;
  private final SecureCookieManager cookies;
  private final Request request;
  private final Response response;
  private boolean isLoaded;
  private boolean isDirty;
  private String rawIdentifier;
  private Map<String, Object> data;
  private Set<String> changedKeys;

  /**
   * @param request Spark HTTP request.
   * @param response Spark HTTP response.
   * @param storage Driver used to store session information.
   */
  private Session(Request request, Response response, Config config, SessionStorageDriver storage) {
    this.request = request;
    this.response = response;
    this.storage = storage;
    isDirty = false;
    isLoaded = false;
    cookies = SecureCookieManager.forRequest(request, response, config.server.hmacKey, config.ssl.isEnabled());
    changedKeys = new TreeSet<>();

    try {
      rawIdentifier = cookies.get(SESSION_COOKIE_NAME);
    } catch (InsecureCookieException e) {
      rawIdentifier = null;
    }
  }

  /**
   * @return Cookie manager for this session.
   */
  public SecureCookieManager getCookieManager() {
    return cookies;
  }

  /**
   * @return Spark HTTP request for this session.
   */
  public Request getRequest() {
    return request;
  }

  /**
   * @return Spark HTTP response for this session.
   */
  public Response getResponse() {
    return response;
  }

  /**
   * @return A new, unused session identifier.
   */
  private String generateSessionId() throws SessionDriverException {
    while (true) {
      SecureRandom generator = new SecureRandom();
      byte bytes[] = new byte[SESSION_ID_BYTES];
      generator.nextBytes(bytes);
      String id = Base64.getEncoder().encodeToString(bytes);

      if (storage.has(hashToken(id))) {
        continue; // Pick again.
      }

      return id;
    }
  }

  /**
   * @return Whether or not this session has any unsaved data.
   */
  public boolean isDirty() {
    return isDirty;
  }

  /**
   * @return All valid keys stored on the session.
   * @throws SessionException
   */
  private Iterable<String> userKeys() throws SessionException {
    lazyLoad();
    return Iterables.filter(data.keySet(), (x) -> !x.startsWith(SESSION_KEY_PREFIX));
  }

  /**
   * Sets the stored data for key to value.
   * @param key
   * @param value A non-null object.
   * @throws SessionException On I/O failure.
   */
  public void set(String key, Object value) throws SessionException {
    if (key.startsWith(SESSION_KEY_PREFIX))
      throw new SessionException("Error: Key is reserved by session manager.");
    if (!(value instanceof Serializable)) // Note: Also prevents storing null pointers!
      throw new SessionException("Error: Unable to store nonserializable object in Session.");

    lazyLoad();
    isDirty = true;
    changedKeys.add(key);
    data.put(key, value);
  }

  public String getRawID() throws SessionException {
    lazyLoad();
    return rawIdentifier;
  }

  public Map<String, ObjectParam> asMap() throws SessionException {
    lazyLoad();

    Map<String, ObjectParam> map = new HashMap<>();

    for (String key : userKeys()) {
      map.put(key, get(key));
    }

    return map;
  }

  public Set<String> keys() throws SessionException {
    lazyLoad();
    return asMap().keySet();
  }

  /**
   * Gets any stored information for the given key. Throws an exception if not found.
   * @param key
   * @return The data stored under key.
   * @throws SessionException On I/O failure.
   */
  public ObjectParam get(String key) throws SessionException {
    if (key.startsWith(SESSION_KEY_PREFIX))
      return null;
    lazyLoad();
    if (!data.containsKey(key))
      return new ObjectParam(null);
    return new ObjectParam(data.get(key));
  }

  /**
   * Removes any stored information for the given key.
   * @param key
   * @throws SessionException On I/O failure.
   */
  public void forget(String key) throws SessionException {
    if (key.startsWith(SESSION_KEY_PREFIX))
      return;

    lazyLoad();
    isDirty = true;
    changedKeys.add(key);
    data.remove(key);
  }

  /**
   * Returns whether or not the session has any data for a key.
   * @param key
   * @return
   * @throws SessionException On I/O failure.
   */
  public boolean has(String key) throws SessionException {
    if (key.startsWith(SESSION_KEY_PREFIX))
      return false;

    lazyLoad();
    return data.containsKey(key);
  }

  /**
   * @return An XSRF token for this session.
   * @throws Exception
   */
  public String getXSRFToken() throws Exception {
    lazyLoad();
    if (!data.containsKey(XSRF_KEY)) {
      data.put(XSRF_KEY, lightning.crypt.Hasher.generateToken(XSRF_BYTES, (x) -> false));
      changedKeys.add(XSRF_KEY);
      isDirty = true;
    }

    return (String) data.get(XSRF_KEY);
  }

  public String xsrfToken() throws Exception {
    return getXSRFToken();
  }

  /**
   * Sets a new XSRF token and returns it.
   * @return
   * @throws Exception
   */
  public String newXSRFToken() throws Exception {
    lazyLoad();

    data.put(XSRF_KEY, lightning.crypt.Hasher.generateToken(XSRF_BYTES, (x) -> false));
    changedKeys.add(XSRF_KEY);
    isDirty = true;

    return (String) data.get(XSRF_KEY);
  }

  /**
   * Saves the session, throwing an Exception on failure.
   * If the session is not dirty, has no effect.
   * @throws SessionException
   * @throws InsecureCookieException
   */
  public void save() throws SessionException {
    if (!isLoaded) {
      return;
    }

    if (!isDirty && rawIdentifier != null) {
      // No need to do a full save, but should ensure the session doesn't expire.
      storage.keepAliveIfExists(hashToken(rawIdentifier));
      return;
    }

    data.put(LAST_USE_KEY, Time.now());
    changedKeys.add(LAST_USE_KEY);
    storage.put(hashToken(rawIdentifier), data, changedKeys);
    try {
      String existingCookie = cookies.get(SESSION_COOKIE_NAME);
      if (existingCookie != null && !existingCookie.equals(rawIdentifier)) {
        cookies.set(SESSION_COOKIE_NAME, rawIdentifier);
      }
    } catch (InsecureCookieException e) {
      try {
        cookies.set(SESSION_COOKIE_NAME, rawIdentifier);
      } catch (HeadersAlreadySentException e2) {
        logger.warn("Couldn't save session: HTTP headers already committed.");
      }
    }
    logger.debug("Wrote session to storage: {}", rawIdentifier);
    logger.debug("Session data was: {}", data);
    isDirty = false;
  }

  /**
   * Loads all data attached to this session.
   * @throws SessionException
   */
  private void lazyLoad() throws SessionException {
    if (isLoaded) {
      return;
    }

    if (rawIdentifier == null) {
      data = new HashMap<>();
      rawIdentifier = generateSessionId();
      cookies.set(SESSION_COOKIE_NAME, rawIdentifier);
      logger.debug("Created new session: {}", rawIdentifier);
    } else {
      data = storage.get(hashToken(rawIdentifier));
      logger.debug("Loaded session from database: {}", rawIdentifier);
      if (data == null) {
        // No record found, create new record with new ID to prevent fixation attacks.
        data = new HashMap<>();
        rawIdentifier = generateSessionId();
        cookies.set(SESSION_COOKIE_NAME, rawIdentifier);
        logger.debug("Session invalidated due to fixation attempt; regenerated as {}.", rawIdentifier);
      } else if (Time.now() - ((Long) data.getOrDefault(LAST_USE_KEY, 0)) > inactivityTimeoutSeconds) {
        // Session time-outs.
        storage.invalidate(hashToken(rawIdentifier));
        rawIdentifier = generateSessionId();
        cookies.set(SESSION_COOKIE_NAME, rawIdentifier);
        data = new HashMap<>();
        logger.debug("Session invalidated due to timeout; regenerated as {}.", rawIdentifier);
      }
    }

    logger.debug("Data loaded was: {}", data);
    isLoaded = true;
    save();
  }

  /**
   * Regenerates the session identifier; should be called periodically and on privilege escalation.
   * Requires a call to save() in order to take effect.
   * @throws SessionException On Failure.
   */
  public void regenerateId() throws SessionException {
    lazyLoad();
    logger.debug("Regenerating session: {}", rawIdentifier);
    storage.invalidate(hashToken(rawIdentifier));
    isDirty = true;
    rawIdentifier = generateSessionId();
    cookies.set(SESSION_COOKIE_NAME, rawIdentifier);
    logger.debug("Regenerated with new identifier: {}", rawIdentifier);
    save();
  }
}
