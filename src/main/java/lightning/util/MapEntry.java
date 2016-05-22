package lightning.util;

import java.util.Map;

/**
 * Provides a simple, immutable Map.Entry implementation.
 * @param <K>
 * @param <V>
 */
public final class MapEntry<K, V> implements Map.Entry<K, V> {
  private final K key;
  private final V value;
  
  public MapEntry(K key, V value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public K getKey() {
    return key;
  }

  @Override
  public V getValue() {
    return value;
  }

  @Override
  public V setValue(V value) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public int hashCode() {
    // Defined by Java spec.
    return (this.getKey()==null ? 0 : this.getKey().hashCode()) ^
        (this.getValue()==null ? 0 : this.getValue().hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    // Defined by Java spec.
    if (!(obj instanceof MapEntry)) {
      return false;
    }
    
    MapEntry<?,?> e = (MapEntry<?,?>) obj;
    
    return (this.getKey()==null ?
        e.getKey()==null : this.getKey().equals(e.getKey()))  &&
        (this.getValue()==null ?
            e.getValue()==null : this.getValue().equals(e.getValue()));
  }
}
