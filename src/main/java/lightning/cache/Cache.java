package lightning.cache;

import java.io.Serializable;

public final class Cache {
  private final CacheDriver driver;
  
  public Cache(CacheDriver driver) {
    this.driver = driver;
  }
  
  @SuppressWarnings("unchecked")
  public <T extends Serializable> T get(String key, Class<T> clazz) throws CacheException {
    return (T) driver.get(key);
  }
  
  public void put(String key, Object value) throws CacheException {
    driver.put(key, value);
  }
  
  public boolean has(String key) throws CacheException {
    return driver.has(key);
  }
  
  public boolean delete(String key) throws CacheException {
    return driver.delete(key);
  }
}
