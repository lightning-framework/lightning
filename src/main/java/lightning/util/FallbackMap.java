package lightning.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A fallback map is an immutable implementation of the Map interface which
 * wraps an ordered list of maps. The fallback map acts as a normal map but
 * falls back to the next map in the ordered list if a particular key is
 * not in the first map. If a key is in an earlier map, its existence in a
 * later map in the list is masked.
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class FallbackMap<K, V> implements Map<K, V> {
  private final Map<K, V>[] maps;
  
  private FallbackMap(Map<K, V>[] maps) {
    this.maps = maps;
  }
  
  @SafeVarargs
  public static <K, V> FallbackMap<K, V> of(Map<K, V> ...maps) {
    return new FallbackMap<>(maps);
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    HashSet<K> visited = new HashSet<>();
    HashSet<Map.Entry<K, V>> entries = new HashSet<>();
    
    for (Map<K, V> map : maps) {
      for (Map.Entry<K, V> entry : map.entrySet()) {
        if (!visited.contains(entry.getKey())) {
          visited.add(entry.getKey());
          entries.add(entry);
        }
      }
    }
    
    return entries;
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsKey(Object key) {
    for (Map<K, V> map : maps) {
      if (map.containsKey(key)) {
        return true;
      }
    }
    
    return false;
  }

  @Override
  public boolean containsValue(Object value) {
    for (Map<K, V> map : maps) {
      if (map.containsValue(value)) {
        return true;
      }
    }
    
    return false;
  }

  @Override
  public V get(Object key) {
    for (Map<K, V> map : maps) {
      if (map.containsKey(key)) {
        return map.get(key);
      }
    }
    
    return null;
  }

  @Override
  public Set<K> keySet() {
    HashSet<K> keys = new HashSet<>();
    
    for (Map<K, V> map : maps) {
      keys.addAll(map.keySet());
    }
    
    return keys;
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public V put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V remove(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    return keySet().size();
  }

  @Override
  public Collection<V> values() {
    HashSet<K> visited = new HashSet<>();
    List<V> values = new ArrayList<>();
    
    for (Map<K, V> map : maps) {
      for (Map.Entry<K, V> entry : map.entrySet()) {
        if (!visited.contains(entry.getKey())) {
          visited.add(entry.getKey());
        }
      }
    }
    
    return values;
  }
}
