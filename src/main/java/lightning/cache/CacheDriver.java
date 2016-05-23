package lightning.cache;

public interface CacheDriver {
  public void set(String key, Object value, long expiration) throws CacheException;
  public Object get(String key) throws CacheException;
  public CacheResult gets(String key) throws CacheException;
  public boolean delete(String key) throws CacheException;
  public long incrdecr(String key, long amount, long initial, long expiration) throws CacheException;
  public boolean clear() throws CacheException;
  public boolean touch(String key, long expiration) throws CacheException;
  public boolean cas(String key, Object token, Object value, long expiration) throws CacheException;
}
