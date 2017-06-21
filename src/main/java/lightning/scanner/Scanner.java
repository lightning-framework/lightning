package lightning.scanner;

import static lightning.util.ReflectionUtil.getMethodsAnnotatedWith;
import static lightning.util.ReflectionUtil.requireHasSinglePublicConstructor;
import static lightning.util.ReflectionUtil.requireIsInstantiateable;
import static lightning.util.ReflectionUtil.requireIsNotInterface;
import static lightning.util.ReflectionUtil.requireIsPublic;
import static lightning.util.ReflectionUtil.requireIsPublicInstance;
import static lightning.util.ReflectionUtil.requireIsPublicInstanceWithReturnType;
import static lightning.util.ReflectionUtil.requireIsPublicStaticWithReturnType;
import static lightning.util.ReflectionUtil.requireOnClassAnnotatedWith;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import lightning.ann.Before;
import lightning.ann.Befores;
import lightning.ann.Controller;
import lightning.ann.ExceptionHandler;
import lightning.ann.Finalizer;
import lightning.ann.Initializer;
import lightning.ann.OnEvent;
import lightning.ann.Route;
import lightning.ann.Routes;
import lightning.ann.WebSocket;
import lightning.classloaders.ExceptingClassLoader;
import lightning.classloaders.ExceptingClassLoader.PrefixClassLoaderExceptor;
import lightning.exceptions.LightningValidationException;
import lightning.inject.InjectionValidator;
import lightning.inject.Injector;

/**
 * Responsible for scanning the class path for annotations needed by the framework.
 */
public class Scanner {
  private final List<String> reloadPrefixes;
  private final List<String> scanPrefixes;
  private final boolean enableAutoReload;
  private final InjectionValidator iv;
  private final String path;

  /**
   * @param classLoader The class loader to use (default one in most cases)
   * @param scanPrefixes List of package prefixes to scan within (e.g. ["lightning.controllers"])
   * @param enableAutoReload
   */
  public Scanner(List<String> reloadPrefixes,
                 List<String> scanPrefixes,
                 boolean enableAutoReload,
                 Injector injector,
                 String path) {
    this.reloadPrefixes = reloadPrefixes != null ? reloadPrefixes : ImmutableList.of();
    this.scanPrefixes = scanPrefixes;
    this.enableAutoReload = enableAutoReload;
    this.iv = new InjectionValidator(injector, this.reloadPrefixes);
    this.path = path;
  }

  private static void putMethod(Map<Class<?>, Set<Method>> map, Method method) {
    Class<?> clazz = method.getDeclaringClass();

    if (!map.containsKey(clazz)) {
      map.put(clazz, new HashSet<>());
    }

    map.get(clazz).add(method);
  }

  public ScanResult scan() throws LightningValidationException {
    Set<Class<?>> controllers = new HashSet<>();
    Map<Class<?>, Set<Method>> initializers = new HashMap<>();
    Map<Class<?>, Set<Method>> finalizers = new HashMap<>();
    Map<Class<?>, Set<Method>> exceptionHandlers = new HashMap<>();
    Map<Class<?>, Set<Method>> routes = new HashMap<>();
    Set<Class<?>> websockets = new HashSet<>();
    Map<Class<?>, Set<Method>> beforeFilters = new HashMap<>();
    ClassLoader classLoader = enableAutoReload
        ? new ExceptingClassLoader(new PrefixClassLoaderExceptor(reloadPrefixes), path)
        : this.getClass().getClassLoader();

    for (Reflections scanner : reflections(classLoader)) {
      for (Method m : scanner.getMethodsAnnotatedWith(Initializer.class)) {
        iv.validate(m);
        requireOnClassAnnotatedWith(m, Controller.class);
        requireIsPublicInstanceWithReturnType(m, void.class);
        controllers.add(m.getDeclaringClass());
        putMethod(initializers, m);
      }

      for (Method m : scanner.getMethodsAnnotatedWith(Finalizer.class)) {
        iv.validate(m);
        requireOnClassAnnotatedWith(m, Controller.class);
        requireIsPublicInstanceWithReturnType(m, void.class);
        controllers.add(m.getDeclaringClass());
        putMethod(finalizers, m);
      }

      for (Method m : scanner.getMethodsAnnotatedWith(ExceptionHandler.class)) {
        iv.validate(m);
        requireIsPublicStaticWithReturnType(m, void.class);
        putMethod(exceptionHandlers, m);
      }

      for (Class<?> c : scanner.getTypesAnnotatedWith(WebSocket.class)) {
        requireIsPublic(c);
        requireIsInstantiateable(c);
        requireHasSinglePublicConstructor(c);
        iv.validateConstructor(c);

        for (Method method : getMethodsAnnotatedWith(c, OnEvent.class)) {
          method.getAnnotation(OnEvent.class).value().validate(method);
          iv.validate(method);
        }

        websockets.add(c);
      }

      for (Method m : Iterables.concat(scanner.getMethodsAnnotatedWith(Before.class),
                                       scanner.getMethodsAnnotatedWith(Befores.class))) {
        iv.validate(m);
        requireIsPublicStaticWithReturnType(m, void.class);
        putMethod(beforeFilters, m);
      }

      for (Method m : Iterables.concat(scanner.getMethodsAnnotatedWith(Route.class),
                                       scanner.getMethodsAnnotatedWith(Routes.class))) {
        iv.validate(m);
        iv.validateRouteReturn(m);
        requireOnClassAnnotatedWith(m, Controller.class);
        requireIsPublicInstance(m);
        controllers.add(m.getDeclaringClass());
        putMethod(routes, m);
      }
    }

    for (Class<?> c : controllers) {
      requireIsPublic(c);
      requireIsNotInterface(c);
      requireHasSinglePublicConstructor(c);
      iv.validateConstructor(c);
    }

    return new ScanResult(controllers,
                          initializers,
                          exceptionHandlers,
                          routes,
                          websockets,
                          finalizers,
                          beforeFilters);
  }

  private Reflections[] reflections(ClassLoader classLoader) {
    Reflections[] result = new Reflections[scanPrefixes.size()];

    int i = 0;
    for (String searchPath : scanPrefixes) {
      ConfigurationBuilder config = ConfigurationBuilder.build(
        searchPath,
        classLoader,
        new SubTypesScanner(),
        new TypeAnnotationsScanner(),
        new MethodAnnotationsScanner());
      result[i++] = new Reflections(config);
    }

    return result;
  }
}
