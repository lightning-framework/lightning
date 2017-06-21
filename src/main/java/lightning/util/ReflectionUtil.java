package lightning.util;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Joiner;

import lightning.exceptions.LightningValidationException;

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

  public static <T extends Annotation> Iterable<T> annotations(Method target,
                                                               Class<T> annBase)
  {
    Repeatable rep = annBase.getAnnotation(Repeatable.class);
    if (rep != null) {
      Annotation annRep = target.getAnnotation(rep.value());
      if (annRep != null) {
        try {
          // NOTE: The Java compiler will guarantee this never fails since it's impossible
          // to incorrectly annotate something with @Repeatable.
          Method getter = rep.value().getMethod("value");
          @SuppressWarnings("unchecked")
          T[] values = (T[])getter.invoke(annRep);
          return Arrays.asList(values);
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
      }
    }

    if (target.getAnnotation(annBase) != null) {
      return Arrays.asList(target.getAnnotation(annBase));
    }

    return Iterables.empty();
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

  public static void requireArgsStartWith(Method method, Class<?> ...args) throws LightningValidationException {
    String error = "Must begin with arguments " + Joiner.on(", ").join(Iterables.map(Arrays.asList(args), c -> c.getCanonicalName())) + ".";

    if (method.getParameterCount() < args.length) {
      throw new LightningValidationException(method, error);
    }

    Parameter[] params = method.getParameters();

    for (int i = 0; i < args.length; i++) {
      if (!params[i].getType().equals(args[i])) {
        throw new LightningValidationException(method, error);
      }
    }
  }

  public static void requireOnClassAnnotatedWith(Method method, Class<? extends Annotation> annotation) throws LightningValidationException {
    if (method.getDeclaringClass().getAnnotation(annotation) == null) {
      throw new LightningValidationException(method, "Must be declared on a class annotated with @" +
                                             annotation.getSimpleName() + ".");
    }
  }

  public static void requireIsPublicInstance(Method method) throws LightningValidationException {
    if (!Modifier.isPublic(method.getModifiers()) ||
        Modifier.isStatic(method.getModifiers()) ||
        Modifier.isAbstract(method.getModifiers())) {
      throw new LightningValidationException(method, "Must be public, non-static, non-abstract.");
    }
  }

  public static void requireIsPublicStatic(Method method) throws LightningValidationException {
    if (!Modifier.isPublic(method.getModifiers()) ||
        !Modifier.isStatic(method.getModifiers()) ||
        Modifier.isAbstract(method.getModifiers())) {
      throw new LightningValidationException(method, "Must be public, static, non-abstract.");
    }
  }

  public static void requireIsPublicStaticWithReturnType(Method method, Class<?> returnType) throws LightningValidationException {
    if (!Modifier.isPublic(method.getModifiers()) ||
        !Modifier.isStatic(method.getModifiers()) ||
        Modifier.isAbstract(method.getModifiers()) ||
        !method.getReturnType().equals(returnType)) {
      throw new LightningValidationException(method, "Must be public, static, non-abstract, and return " + returnType + ".");
    }
  }

  public static void requireIsPublicInstanceWithReturnType(Method method, Class<?> returnType) throws LightningValidationException {
    if (!Modifier.isPublic(method.getModifiers()) ||
        Modifier.isStatic(method.getModifiers()) ||
        Modifier.isAbstract(method.getModifiers()) ||
        !method.getReturnType().equals(returnType)) {
      throw new LightningValidationException(method, "Must be declared public, instance, non-abstract, and return " + returnType + ".");
    }
  }

  public static void requireIsPublic(Class<?> clazz) throws LightningValidationException {
    if (!Modifier.isPublic(clazz.getModifiers())) {
      throw new LightningValidationException(clazz, "Must be declared public.");
    }
  }

  public static void requireIsInstantiateable(Class<?> clazz) throws LightningValidationException {
    if (Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface()) {
      throw new LightningValidationException(clazz, "Must be instantiateable (non-abstract, non-interface).");
    }
  }

  public static void requireIsNotInterface(Class<?> clazz) throws LightningValidationException {
    if (clazz.isInterface()) {
      throw new LightningValidationException(clazz, "Must not be an interface.");
    }
  }

  public static void requireHasSinglePublicConstructor(Class<?> clazz) throws LightningValidationException {
    if (clazz.getDeclaredConstructors().length > 1) {
      throw new LightningValidationException(clazz, "Must have a single public constructor.");
    }

    if (clazz.getDeclaredConstructors().length == 1 &&
        !Modifier.isPublic(clazz.getDeclaredConstructors()[0].getModifiers())) {
      throw new LightningValidationException(clazz, "Must have a single public constructor.");
    }
  }

  public static void requireHasReturnValue(Method m) throws LightningValidationException {
    if (m.getReturnType().equals(void.class) ||
        m.getReturnType().equals(Void.class)) {
      throw new LightningValidationException(m, "Must not return void.");
    }
  }

  public static void requireReturns(Method m, Class<?> ...types) throws LightningValidationException {
    for (Class<?> type : types) {
      if (m.getReturnType().equals(type)) {
        return;
      }
    }

    throw new LightningValidationException(m, "Must return one of [" + Joiner.on(", ").join(types) + "].");
  }
}
