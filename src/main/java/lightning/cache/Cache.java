package lightning.cache;

import lightning.mvc.ObjectParam;

/**
 * Defines an interface for a cache.
 * Caches may store any type of serializable object.
 */
public final class Cache {
  private final CacheDriver driver;
  
  public Cache(CacheDriver driver) {
    this.driver = driver;
  }
  
  /**
   * Gets the value for a key.
   * @param key
   * @return
   * @throws CacheException
   */
  public ObjectParam get(String key) throws CacheException {
    return new ObjectParam(driver.get(key));
  }
  
  /**
   * Gets the value for a key.
   * @param key
   * @return
   * @throws CacheException
   */
  public CacheResult gets(String key) throws CacheException {
    return driver.gets(key);
  }
  
  /**
   * Sets the value for a key.
   * @param key
   * @param value
   * @param expiration
   * @throws CacheException
   */
  public void set(String key, Object value, long expiration) throws CacheException {
    driver.set(key, value, expiration);
  }
  
  /**
   * Deletes the value stored by a key.
   * @param key
   * @return
   * @throws CacheException
   */
  public boolean delete(String key) throws CacheException {
    return driver.delete(key);
  }
  
  /**
   * Increments the value stored by a key atomically.
   * @param key
   * @param amount
   * @param initial
   * @param expiration
   * @return 
   * @throws CacheException
   */
  public long increment(String key, long amount, long initial, long expiration) throws CacheException {
    return driver.incrdecr(key, amount, initial, expiration);
  }
  
  /**
   * Decrements the value stored by a key atomically.
   * @param key
   * @param amount
   * @param initial
   * @param expiration
   * @return
   * @throws CacheException
   */
  public long decrement(String key, long amount, long initial, long expiration) throws CacheException {
    return driver.incrdecr(key, -amount, initial, expiration);
  }
  
  /**
   * Performs check-and-set.
   * @param key
   * @param token
   * @param value
   * @param expiration
   * @return
   * @throws CacheException
   */
  public boolean cas(String key, Object token, Object value, long expiration) throws CacheException {
    return driver.cas(key, token, value, expiration);
  }
  
  /**
   * Touches a key.
   * @param key
   * @param expiration
   * @return
   * @throws CacheException
   */
  public boolean touch(String key, long expiration) throws CacheException {
    return driver.touch(key, expiration);
  }
  
  /**
   * Clears all items in the cache.
   * @throws CacheException
   */
  public void clear() throws CacheException {
    driver.clear();
  }
}
