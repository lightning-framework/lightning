package lightning.cache;

import lightning.mvc.ObjectParam;

public final class Cache {
  private final CacheDriver driver;
  
  public Cache(CacheDriver driver) {
    this.driver = driver;
  }
  
  public ObjectParam get(String key) throws CacheException {
    return new ObjectParam(driver.get(key));
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
