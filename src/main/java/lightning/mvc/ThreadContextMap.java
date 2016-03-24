package lightning.mvc;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides a type-safe thread-local context for storing objects.
 * TODO(mschurr): Should probably move away from hash maps and just store a custom class for speed.
 */
public class ThreadContextMap {
  private static final ThreadLocal<Map<Class<?>, Object>> scope = new ThreadLocal<>();
  
  @SuppressWarnings("unchecked")
  public static <T> T get(Class<T> type) {    
    if (scope.get() == null) {
      return null;
    }
    
    if (scope.get().get(type) == null ||
        !type.isInstance(scope.get().get(type))) {
      return null;
    }
    
    return (T) scope.get().get(type);
  }
  
  public static <T> boolean has(Class<T> type) {
    return scope.get() != null && scope.get().containsKey(type);
  }
  
  public static <T> void set(Class<T> type, T data) {
    if (scope.get() == null) {
      scope.set(new HashMap<>());
    }
    
    scope.get().put(type, data);
  }
  
  public static void clear() {
    scope.remove();
  }
}
