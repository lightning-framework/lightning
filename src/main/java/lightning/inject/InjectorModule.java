package lightning.inject;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;

/**
 * A dependency injection module specifies a set of injection bindings.
 * For more information, see the documentation for Injector.
 */
public class InjectorModule {
  private final Map<Class<?>, Object> classBindings;
  private final Map<String, Object> nameBindings;
  private final Map<Class<? extends Annotation>, Object> annotationBindings;
  private final Map<Class<?>, Resolver<?>> classResolverBindings;
  private final Map<String, Resolver<?>> nameResolverBindings;
  private final Map<Class<? extends Annotation>, Resolver<?>> annotationResolverBindings;

  public InjectorModule() {
    classBindings = new HashMap<>();
    nameBindings = new HashMap<>();
    annotationBindings = new HashMap<>();
    classResolverBindings = new HashMap<>();
    nameResolverBindings = new HashMap<>();
    annotationResolverBindings = new HashMap<>();
  }

  /**
   * Binds parameters of the given type to the given instance.
   * Correctly respects the type hierarchy.
   * @param clazz A type.
   * @param instance An object of given type.
   */
  public <T> void bindClassToInstance(Class<T> clazz, T instance) {
    Class<?> currentClass = clazz;

    while (currentClass != null && !classBindings.containsKey(currentClass)) {
      classBindings.put(currentClass, instance);
      currentClass = currentClass.getSuperclass();
    }
  }

  /**
   * Binds parameters of the given type to the instance produced by the given resolver.
   * Correctly respects the type hierarchy.
   * @param clazz A type.
   * @param resolver A resolver.
   */
  public <T> void bindClassToResolver(Class<T> clazz, Resolver<T> resolver) {
    Class<?> currentClass = clazz;

    while (currentClass != null && !classBindings.containsKey(currentClass)) {
      classResolverBindings.put(currentClass, resolver);
      currentClass = currentClass.getSuperclass();
    }
  }

  /**
   * Binds parameters annotated with @Inject(name) to the given instance.
   * @param name A unique name.
   * @param instance An object.
   */
  public <T> void bindNameToInstance(String name, T instance) {
    nameBindings.put(name, instance);
  }

  /**
   * Binds parameters annotated with @Inject(name) to the instance produced by the given resolver.
   * @param name A unique name.
   * @param resolver A resolver.
   */
  public <T> void bindNameToResolver(String name, Resolver<T> resolver) {
    nameResolverBindings.put(name, resolver);
  }

  /**
   * Binds parameters annotated with the given annotation to the given instance.
   * @param annotation An annotation for method parameters.
   * @param instance An object.
   */
  public <T> void bindAnnotationToInstance(Class<? extends Annotation> annotation, T instance) {
    annotationBindings.put(annotation, instance);
  }

  /**
   * Binds parameters annotated with the given annotation to the instance produced by the given resolver.
   * @param annotation An annotation for method parameters.
   * @param resolver A resolver.
   */
  public <T> void bindAnnotationToResolver(Class<? extends Annotation> annotation, Resolver<T> resolver) {
    annotationResolverBindings.put(annotation, resolver);
  }

  /**
   * Returns the value currently bound for the given class (via bindClassTo{Resolver|Instance}).
   * Correctly functions with inheritance.
   * @param clazz A class.
   * @return The bound value or null if no bound value.
   * @throws Exception On failure.
   */
  @SuppressWarnings("unchecked")
  public <T> T getBindingForClass(Class<T> clazz) throws Exception {
    T result = (T) classBindings.get(clazz);

    if (result != null) {
      return result;
    }

    if (classResolverBindings.containsKey(clazz)) {
      return (T) classResolverBindings.get(clazz).resolve();
    }

    return null;
  }

  /**
   * Returns the value currently bound for the given name (via bindNameTo{Resolver|Instance}).
   * @param name A unique name.
   * @return The bound value or null if no bound value.
   * @throws Exception On failure.
   */
  public Object getBindingForName(String name) throws Exception {
    Object result = nameBindings.get(name);

    if (result != null) {
      return result;
    }

    if (nameResolverBindings.containsKey(name)) {
      return nameResolverBindings.get(name).resolve();
    }

    return null;
  }

  /**
   * Returns the value currently bound for the given annotation (via bindAnnotationTo{Instance|Resolver}).
   * @param annotation An annotation type.
   * @return The bound value or null if no bound value.
   * @throws Exception On failure.
   */
  public Object getBindingForAnnotation(Class<? extends Annotation> annotation) throws Exception {
    Object result = annotationBindings.get(annotation);

    if (result != null) {
      return result;
    }

    if (annotationResolverBindings.containsKey(annotation)) {
      return annotationResolverBindings.get(annotation).resolve();
    }

    return null;
  }

  /**
   * Binds the given throwable object to be injectable.
   * @param e Any throwable object.
   */
  public void bindToClass(Throwable e) {
    Class<?> currentClass = e.getClass();

    while (currentClass != null && !classBindings.containsKey(currentClass)) {
      classBindings.put(currentClass, e);
      currentClass = currentClass.getSuperclass();
    }
  }

  private Iterable<Object> allBoundObjects() {
    return Iterables.filter(
             Iterables.concat(
               Iterables.transform(
                 Iterables.concat(
                   classResolverBindings.values(),
                   nameResolverBindings.values(),
                   annotationResolverBindings.values()
                 ),
                 x -> {
                   try {
                     return x.resolve();
                   } catch (Exception e) {
                     return null;
                   }
                 }
               ),
               classBindings.values(),
               annotationBindings.values(),
               nameBindings.values()
             ),
             x -> x != null
           );
  }

  @SuppressWarnings("unchecked")
  private Iterable<Class<?>> allBoundClasses() {
    return Iterables.concat(Iterables.transform(allBoundObjects(),
                                                x -> x.getClass()),
                            classBindings.keySet(),
                            annotationBindings.keySet(),
                            classResolverBindings.keySet(),
                            annotationResolverBindings.keySet());
  }

  public boolean hasInjectionsIn(List<String> prefixes) {
    for (Class<?> clazz : allBoundClasses()) {
      for (String prefix : prefixes) {
        if (clazz.getCanonicalName().startsWith(prefix)) {
          return true;
        }
      }
    }

    return false;
  }
}
