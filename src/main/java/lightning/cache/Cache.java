package lightning.cache;

import java.io.Serializable;

public final class Cache {
  private static CacheDriver driver;
  
  public static void setDriver(CacheDriver driver) {
    Cache.driver = driver;
  }
  
  public static boolean hasDriver() {
    return driver != null;
  }
  
  private static void requireDriver() {
    if (!hasDriver()) {
      throw new RuntimeException("You must call Cache.setDriver before using.");
    }
  }
  
  @SuppressWarnings("unchecked")
  public static <T extends Serializable> T get(String key, Class<T> clazz) throws CacheException {
    requireDriver();
    return (T) driver.get(key);
  }
  
  public static void put(String key, Object value) throws CacheException {
    requireDriver();
    driver.put(key, value);
  }
  
  public static boolean has(String key) throws CacheException {
    requireDriver();
    return driver.has(key);
  }
  
  public static boolean delete(String key) throws CacheException {
    requireDriver();
    return driver.delete(key);
  }
}
