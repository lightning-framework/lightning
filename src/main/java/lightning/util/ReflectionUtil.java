package lightning.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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

  public static List<Method> getMethodsAnnotatedWith(Class<?> clazz, Class<? extends Annotation> annotation) {
    List<Method> results = new ArrayList<>();

    while (clazz != null) {
      for (Method method : clazz.getDeclaredMethods()) {
        if (!method.isAnnotationPresent(annotation)) {
          continue;
        }

        results.add(method);
      }

      clazz = clazz.getSuperclass();
    }

    return results;
  }
}
