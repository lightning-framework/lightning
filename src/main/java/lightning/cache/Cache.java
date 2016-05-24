package lightning.cache;

import lightning.mvc.ObjectParam;

/**
 * Defines an interface for a cache.
 * Caches may store any type of serializable object.
 */
public final class Cache {
  private static final long DEFAULT_EXPIRATION = 0;
  private final CacheDriver driver;
  
  public Cache(CacheDriver driver) {
    this.driver = driver;
  }
  
  /**
   * Attempts to get the value of a key if it exists. If no value exists, invokes the producer to produce
   * a value, sets the value, and returns the set value.
   * @param key
   * @param type
   * @param producer
   * @return
   * @throws CacheException
   */
  public <T> T get(String key, Class<T> type, CacheProducer<T> producer) throws CacheException {
    return get(key, type, producer, DEFAULT_EXPIRATION);
  }
  
  /**
   * Attempts to get the value of a key if it exists. If no value exists, invokes the producer to produce
   * a value, sets the value, and returns the set value.
   * @param key
   * @param type
   * @param producer
   * @param expiration
   * @return
   * @throws CacheException
   */
  public <T> T get(String key, Class<T> type, CacheProducer<T> producer, long expiration) throws CacheException {
    ObjectParam result = get(key);
    
    if (result.exists()) {
      return result.castTo(type);
    }
    
    T item = null;
    try {
      item = producer.yield();
    } catch (Exception e) {
      throw new CacheException(e);
    }
    
    set(key, item, expiration);
    return item;
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
   * Gets the value for a key and its check-and-set token.
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
   * Sets the value for a key.
   * @param key
   * @param value
   * @throws CacheException
   */
  public void set(String key, Object value) throws CacheException {
    set(key, value, DEFAULT_EXPIRATION);
  }
  
  /**
   * Deletes the value stored for a key.
   * @param key
   * @return
   * @throws CacheException
   */
  public boolean delete(String key) throws CacheException {
    return driver.delete(key);
  }
  
  /**
   * Increments the value stored for a key atomically.
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
   * Increments the value stored for a key atomically.
   * @param key
   * @param amount
   * @param initial
   * @return
   * @throws CacheException
   */
  public long increment(String key, long amount, long initial) throws CacheException {
    return increment(key,  amount, initial, DEFAULT_EXPIRATION);
  }
  
  /**
   * Decrements the value stored for a key atomically.
   * @param key
   * @param amount
   * @param initial
   * @param expiration
   * @return The new value stored for the key.
   * @throws CacheException
   */
  public long decrement(String key, long amount, long initial, long expiration) throws CacheException {
    return driver.incrdecr(key, -amount, initial, expiration);
  }
  
  /**
   * Decrements the value stored for a key atomically.
   * @param key
   * @param amount
   * @param initial
   * @return The new value stored for the key.
   * @throws CacheException
   */
  public long decrement(String key, long amount, long initial) throws CacheException {
    return decrement(key, amount, initial, DEFAULT_EXPIRATION);
  }
  
  /**
   * Performs check-and-set for a key.
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
   * Performs check-and-set for a key.
   * @param key
   * @param token
   * @param value
   * @return
   * @throws CacheException
   */
  public boolean cas(String key, Object token, Object value) throws CacheException {
    return cas(key, token, value, DEFAULT_EXPIRATION);
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
   * Touches a key.
   * @param key
   * @return
   * @throws CacheException
   */
  public boolean touch(String key) throws CacheException {
    return touch(key, DEFAULT_EXPIRATION);
  }
  
  /**
   * Clears all items in the cache.
   * @throws CacheException
   */
  public void clear() throws CacheException {
    driver.clear();
  }
}
