package lightning.sessions.drivers;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lightning.sessions.Session.SessionDriverException;
import lightning.sessions.Session.SessionStorageDriver;

/**
 * A simple driver which stores session data in the memory of the machine.
 * This is not maintainable for scalable, distributed applications and is intended primarily for testing.
 */
public class InMemorySessionDriver implements SessionStorageDriver {
  private ConcurrentHashMap<String, Map<String, Object>> storage;
  
  public InMemorySessionDriver() {
    storage = new ConcurrentHashMap<>();
  }

  @Override
  public Map<String, Object> get(String hashedId) throws SessionDriverException {
    return storage.get(hashedId);
  }

  @Override
  public void put(String hashedId, Map<String, Object> data, Set<String> deltaKeys) throws SessionDriverException {
    storage.put(hashedId, data);
  }

  @Override
  public boolean has(String hashedId) throws SessionDriverException {
    return storage.containsKey(hashedId);
  }

  @Override
  public void invalidate(String hashedId) throws SessionDriverException {
    storage.remove(hashedId);
  }

  @Override
  public void keepAliveIfExists(String hashedId) throws SessionDriverException {
    // No-op.
  }
}
