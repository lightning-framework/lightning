package lightning.util;

import java.lang.reflect.Method;

public class ReflectionUtil {
  public static Method getMethod(Class<?> clazz, String name) throws NoSuchMethodException {
    Method r = null;

    for (Method m : clazz.getMethods()) {
      if (m.getName().equals(name)) {
        if (r != null) {
          // Method name must be unique on the class.
          throw new IllegalArgumentException();
        }

        r = m;
      }
    }

    if (r == null) {
      throw new NoSuchMethodException();
    }

    return r;
  }
}
