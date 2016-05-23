package lightning.cache.driver;

import lightning.cache.CacheDriver;
import lightning.cache.CacheException;
import lightning.cache.CacheResult;

public class ExceptingCacheDriver implements CacheDriver {
  @Override
  public void set(String key, Object value, long expiration) throws CacheException {
    throw new CacheException("You must configure a driver.");
  }

  @Override
  public Object get(String key) throws CacheException {
    throw new CacheException("You must configure a driver.");
  }

  @Override
  public CacheResult gets(String key) throws CacheException {
    throw new CacheException("You must configure a driver.");
  }

  @Override
  public boolean delete(String key) throws CacheException {
    throw new CacheException("You must configure a driver.");
  }

  @Override
  public long incrdecr(String key, long amount, long initial, long expiration)
      throws CacheException {
    throw new CacheException("You must configure a driver.");
  }

  @Override
  public boolean clear() throws CacheException {
    throw new CacheException("You must configure a driver.");
  }

  @Override
  public boolean touch(String key, long expiration) throws CacheException {
    throw new CacheException("You must configure a driver.");
  }

  @Override
  public boolean cas(String key, Object token, Object value, long expiration) throws CacheException {
    throw new CacheException("You must configure a driver.");
  }
}
