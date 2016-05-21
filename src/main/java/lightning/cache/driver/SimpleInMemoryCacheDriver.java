package lightning.cache.driver;

import java.util.concurrent.ConcurrentHashMap;

import lightning.cache.CacheDriver;
import lightning.cache.CacheException;

/**
 * A horrible implementation of an in-memory cache with no expiration or size limit.
 */
public class SimpleInMemoryCacheDriver implements CacheDriver {
  public ConcurrentHashMap<String, Object> map;
  
  public SimpleInMemoryCacheDriver() {
    map = new ConcurrentHashMap<>();
  }

  @Override
  public void put(String key, Object value) throws CacheException {
    map.put(key, value);
  }

  @Override
  public Object get(String key) throws CacheException {
    return map.get(key);
  }

  @Override
  public boolean has(String key) throws CacheException {
    return map.containsKey(key);
  }

  @Override
  public boolean delete(String key) throws CacheException {
    return map.remove(key) != null;
  }
}
