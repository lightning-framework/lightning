package lightning.sessions;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import lightning.crypt.SecureCookieManager;
import lightning.crypt.SecureCookieManager.InsecureCookieException;
import lightning.util.Iterables;
import lightning.util.Time;
import spark.Request;
import spark.Response;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;

/**
 * Implements a session system for the Spark web framework.
 * Improves upon Spark's native session functionality by adding extra security features and
 * (by allowing custom storage drivers) supporting distributed session management.
 * 
 * Session are loaded and saved lazily (no operations on storage unless necessary).
 * Any type of Serializable object can be stored in a session.
 * 
 * Example Usage:
 * Session.setStorageDriver(new MyStorageDriverClass());
 * Session session = Session.forRequest(Request request, Response response);
 */
public final class Session {
  private static final int SESSION_ID_BYTES = 1024;
  private static final int XSRF_BYTES = 48;
  private static final String SESSION_COOKIE_NAME = "_sessiond";
  private static final String SESSION_KEY_PREFIX = "$$session-";
  private static final String LAST_USE_KEY = SESSION_KEY_PREFIX + "lastuse";
  private static final String XSRF_KEY = SESSION_KEY_PREFIX + "xsrf";
  private static SessionStorageDriver sharedStorageDriver = null;
  private static long inactivityTimeoutSeconds = 60 * 60 * 24 * 14;
  
  /**
   * Sets the storage driver used for session storage.
   * @param driver
   */
  public static void setDriver(SessionStorageDriver driver) {
    sharedStorageDriver = driver;
  }
  
  /**
   * Sets the default session expiration time.
   * @param seconds
   */
  public static void setInactivityTimeoutSeconds(long seconds) {
    inactivityTimeoutSeconds = seconds;
  }
  
  /**
   * @return Default session expire time in seconds.
   */
  public static long getInactivityTimeoutSeconds() {
    return inactivityTimeoutSeconds;
  }
  
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
  public static Session forRequest(Request request, Response response) {
    if (sharedStorageDriver == null) {
      throw new RuntimeException("Error: Must call setStorageDriver before using Session.");
    }
    
    return new Session(request, response, sharedStorageDriver);
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
     * @param hashedId Hashed session idenfitifer.
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
  private Session(Request request, Response response, SessionStorageDriver storage) {
    this.request = request;
    this.response = response;
    this.storage = storage;
    isDirty = false;
    isLoaded = false;
    cookies = SecureCookieManager.forRequest(request, response);
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
  
  public Iterable<String> keys() throws SessionDriverException {
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
      return;    
    if (!(value instanceof Serializable)) // Note: Also prevents storing null pointers!
      throw new SessionException("Error: Unable to store nonserializable object in Session.");
    
    lazyLoad();
    isDirty = true;
    changedKeys.add(key);
    data.put(key, value);
  }
  
  public String getRawID() throws SessionDriverException {
    lazyLoad();
    return rawIdentifier;
  }
  
  public Map<String, Object> asMap() throws SessionDriverException {
    lazyLoad();
    return ImmutableMap.copyOf(data);
  }
  
  public Set<String> allKeys() throws SessionDriverException {
    lazyLoad();
    return asMap().keySet();
  }
  
  /**
   * Gets any stored information for the given key. Throws an exception if not found.
   * @param key
   * @return The data stored under key.
   * @throws SessionException On I/O failure.
   */
  public Object get(String key) throws SessionException {
    if (key.startsWith(SESSION_KEY_PREFIX))
      return null;
    if (!data.containsKey(key))
      throw new SessionException("Attempted to access data for non-existent key.");
    
    lazyLoad();
    return data.get(key);
  }
  
  public long getLong(String key) throws SessionException {
    return (Long) get(key);
  }
  
  public int getInt(String key) throws SessionException {
    return (Integer) get(key);
  }
  
  public String getString(String key) throws SessionException {
    return (String) get(key);
  }
  
  public char getChar(String key) throws SessionException {
    return (Character) get(key);
  }
  
  public boolean getBoolean(String key) throws SessionException {
    return (Boolean) get(key);
  }
  
  public double getDouble(String key) throws SessionException {
    return (Double) get(key);
  }
  
  public float getFloat(String key) throws SessionException {
    return (Float) get(key);
  }
  
  @SuppressWarnings("unchecked")
  public <T> List<T> getList(String key, Class<T> type) throws SessionException {
    return (List<T>) get(key);
  }
  
  @SuppressWarnings("unchecked")
  public <T> Set<T> getSet(String key, Class<T> type) throws SessionException {
    return (Set<T>) get(key);
  }
  
  @SuppressWarnings("unchecked")
  public <K,V> Map<K,V> getMap(String key, Class<K> keyType, Class<V> valueType) throws SessionException {
    return (Map<K,V>) get(key);
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
   * Saves the session, throwing an Exception on failure.
   * If the session is not dirty, has no effect.
   * @throws SessionException
   * @throws InsecureCookieException 
   */
  public void save() throws SessionException {
    if (!isLoaded || !isDirty) {
      if (rawIdentifier != null) {
        storage.keepAliveIfExists(hashToken(rawIdentifier));
      }
      return;
    }
    
    data.put(LAST_USE_KEY, Time.now());
    changedKeys.add(LAST_USE_KEY);
    storage.put(hashToken(rawIdentifier), data, changedKeys);
    cookies.set(SESSION_COOKIE_NAME, rawIdentifier);
    isDirty = false;
  }
  
  /**
   * @return An XSRF token for this session.
   * @throws Exception
   */
  public String getXSRFToken() throws Exception {
    lazyLoad();
    if (!data.containsKey(XSRF_KEY)) {
      data.put(XSRF_KEY, lightning.crypt.Hashing.generateToken(XSRF_BYTES, (x) -> false));
      changedKeys.add(XSRF_KEY);
      isDirty = true;
    }
    
    return (String) data.get(XSRF_KEY);
  }
  
  /**
   * Loads all data attached to this session.
   * @throws SessionDriverException On Failure.
   */
  private void lazyLoad() throws SessionDriverException {
    if (isLoaded) {
      return;
    }
        
    if (rawIdentifier == null) {
      data = new TreeMap<>();
      rawIdentifier = generateSessionId();
      isLoaded = true;
      return;
    }
    
    data = storage.get(hashToken(rawIdentifier));
    
    if (data == null) {
      // No record found, create new record with new ID to prevent fixation attacks.
      data = new TreeMap<>();
      rawIdentifier = generateSessionId();
    } else if (Time.now() - ((Long) data.getOrDefault(LAST_USE_KEY, 0)) > inactivityTimeoutSeconds) {
      // Session time-outs.
      storage.invalidate(hashToken(rawIdentifier));
      rawIdentifier = generateSessionId();
      data = new TreeMap<>();
    }
    
    isLoaded = true;
  }
  
  /**
   * Regenerates the session identifier; should be called periodically and on privilege escalation.
   * Requires a call to save() in order to take effect.
   * @throws SessionException On Failure.
   */
  public void regenerateId() throws SessionException {
    lazyLoad();
    storage.invalidate(hashToken(rawIdentifier));
    isDirty = true;
    rawIdentifier = generateSessionId();
  }
}
