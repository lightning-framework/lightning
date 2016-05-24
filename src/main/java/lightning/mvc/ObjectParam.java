package lightning.mvc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Optional;

/**
 * Wraps a null-able object of arbitrary type and provides convenience methods for casting
 * the object to other types in a type-safe manner.
 */
public class ObjectParam {
  private final @Nullable Object value;
  
  public ObjectParam(@Nullable Object value) {
    this.value = value;
  }
  
  private final <T> T fetch(Optional<T> option) throws TypeConversionException {
    if (!option.isPresent()) {
      throw new TypeConversionException();
    }
    
    return option.get();
  }
  
  public Object objectValue() throws TypeConversionException {
    return fetch(objectOption());
  }
  
  public Optional<Object> objectOption() {
    return Optional.fromNullable(value);
  }
  
  public <T> T castTo(Class<T> type) throws TypeConversionException {
    return fetch(castToOption(type));
  }
  
  public <T> List<T> listValue(Class<T> type) throws TypeConversionException {
    return fetch(listOption(type));
  }
  
  public <T> Set<T> setValue(Class<T> type) throws TypeConversionException {
    return fetch(setOption(type));
  }
  
  public <K, V> Map<K, V> mapValue(Class<K> keyType, Class<V> valueType) throws TypeConversionException {
    return fetch(mapOption(keyType, valueType));
  }
  
  public boolean exists() {
    return value != null;
  }
  
  public Object or(Object other) {
    return (value != null) ? value : other;
  }
  
  public long longValue() throws TypeConversionException {
    return fetch(longOption());
  }
  
  public int intValue() throws TypeConversionException {
    return fetch(intOption());
  }
  
  public float floatValue() throws TypeConversionException {
    return fetch(floatOption());
  }
  
  public double doubleValue() throws TypeConversionException {
    return fetch(doubleOption());
  }
  
  public String stringValue() throws TypeConversionException {
    return fetch(stringOption());
  }
  
  public char charValue() throws TypeConversionException {
    return fetch(charOption());
  }
  
  public boolean booleanValue() throws TypeConversionException {
    return fetch(booleanOption());
  }
  
  public <T> T enumValue(Class<T> type) throws TypeConversionException {
    return fetch(enumOption(type));
  }
  
  public boolean isLong() {
    return longOption().isPresent();
  }
  
  public boolean isInt() {
    return intOption().isPresent();
  }
  
  public boolean isFloat() {
    return floatOption().isPresent();
  }
  
  public boolean isDouble() {
    return doubleOption().isPresent();
  }
  
  public boolean isString() {
    return stringOption().isPresent();
  }
  
  public boolean isChar() {
    return charOption().isPresent();
  }
  
  public <K, V> boolean isMapOf(Class<K> keyType, Class<V> valueType) {
    return mapOption(keyType, valueType).isPresent();
  }
  
  public <T> boolean isListOf(Class<T> type) {
    return listOption(type).isPresent();
  }
  
  public <T> boolean isSetOf(Class<T> type) {
    return setOption(type).isPresent();
  }
  
  public <T> boolean isEnumValue(Class<T> type) {
    return enumOption(type).isPresent();
  }
  
  public boolean isBoolean() {
    return booleanOption().isPresent();
  }
  
  public <T> boolean is(Class<T> type) {
    return castToOption(type).isPresent();
  }
  
  public <T> Optional<T> enumOption(Class<T> type) {
    if (!(value instanceof Enum<?>)) {
      return Optional.absent();
    }
    
    if (type.isInstance(type)) {
      return Optional.of(type.cast(value));
    }
    
    return Optional.absent();
  }
  
  public Optional<Long> longOption() {
    if (value instanceof Long) {
      return Optional.of((Long) value);
    }
    
    return Optional.absent();
  }
  
  public Optional<Integer> intOption() {
    if (value instanceof Integer) {
      return Optional.of((Integer) value);
    }
    
    return Optional.absent();
  }
  
  public Optional<Float> floatOption() {
    if (value instanceof Float) {
      return Optional.of((Float) value);
    }
    
    return Optional.absent();
  }
  
  public Optional<Double> doubleOption() {
    if (value instanceof Double) {
      return Optional.of((Double) value);
    }
    
    return Optional.absent();
  }
  
  public Optional<String> stringOption() {
    if (value instanceof String) {
      return Optional.of((String) value);
    }
    
    return Optional.absent();
  }
  
  public Optional<Character> charOption() {
    if (value instanceof Character) {
      return Optional.of((Character) value);
    }
    
    return Optional.absent();
  }
  
  public Optional<Boolean> booleanOption() {
    if (value instanceof Boolean) {
      return Optional.of((Boolean) value);
    }
    
    return Optional.absent();
  }
  
  public <T> Optional<T> castToOption(Class<T> type) {
    if (type.isInstance(value)) {
      return Optional.of(type.cast(value));
    }
    
    return Optional.absent();
  }
  
  @SuppressWarnings("unchecked")
  public <K, V> Optional<Map<K, V>> mapOption(Class<K> keyType, Class<V> valueType) {
    if (value instanceof Map) {
      // Sufficient to say it conforms ot the type iff the map is empty
      // or the first entry is of required type.
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
        if (!keyType.isInstance(entry.getKey())) {
          throw new TypeConversionException();
        }
        
        if (!valueType.isInstance(entry.getValue())) {
          throw new TypeConversionException();
        }
        
        break;
      }
      
      return Optional.of((Map<K, V>) value);
    }
    
    return Optional.absent();
  }
  
  @SuppressWarnings("unchecked")
  public <T> Optional<List<T>> listOption(Class<T> type) {
    if (value instanceof List) {
      for (Object item : (List<?>) value) {
        // Sufficient to say it conforms to the type iff the list is empty
        // or the first item is of required type.
        if (!type.isInstance(item)) {
          throw new TypeConversionException();
        }
        
        break;
      }
      
      return Optional.of((List<T>) value);
    }
    
    return Optional.absent();
  }
  
  @SuppressWarnings("unchecked")
  public <T> Optional<Set<T>> setOption(Class<T> type) {
    if (value instanceof Set) {
      for (Object item : (Set<?>) value) {
        // Sufficient to say it conforms to the type iff the set is empty
        // or the first item is of required type.
        if (!type.isInstance(item)) {
          throw new TypeConversionException();
        }
        
        break;
      }
      
      return Optional.of((Set<T>) value);
    }
    
    return Optional.absent();
  }
  
  @Override
  public String toString() {
    if (value == null) {
      return "null";
    }
    
    return value.toString();
  }
}
