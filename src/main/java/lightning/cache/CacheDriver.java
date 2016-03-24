package lightning.cache;

public interface CacheDriver {
  public void put(String key, Object value) throws CacheException;
  public Object get(String key) throws CacheException;
  public boolean has(String key) throws CacheException;
  public boolean delete(String key) throws CacheException;
}
