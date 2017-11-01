package lightning.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

public class ExceptionMapper<T> {
    private Map<Class<?>, T> map;
    private Map<Class<?>, T> cache;

    public ExceptionMapper() {
      this.map = new HashMap<>();
      this.cache = new ConcurrentHashMap<>();
    }

    public void clear() {
      this.map = new HashMap<>();
      this.cache = new ConcurrentHashMap<>();
    }

    public void map(Class<? extends Throwable> type, T handler) {
      if (map.containsKey(type)) {
        throw new IllegalStateException();
      }
      map.put(type, handler);
    }

    public boolean has(Class<? extends Throwable> type) {
      return get(type) != null;
    }

    public @Nullable T get(Throwable exception) {
      return get(exception.getClass());
    }

    public @Nullable T get(Class<? extends Throwable> throwableType) {
      Class<?> type = throwableType;

      T handler = cache.get(type);
      if (handler != null) {
        return handler;
      }

      while (type != null) {
        handler = map.get(type);

        if (handler != null) {
          cache.put(type, handler);
          return handler;
        }

        type = type.getSuperclass();
      }

      return null;
    }
}