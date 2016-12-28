package lightning.scanner;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lightning.ann.Before;
import lightning.ann.Befores;
import lightning.ann.Controller;
import lightning.ann.ExceptionHandler;
import lightning.ann.Finalizer;
import lightning.ann.Initializer;
import lightning.ann.Route;
import lightning.ann.Routes;
import lightning.ann.WebSocket;
import lightning.classloaders.ExceptingClassLoader;
import lightning.classloaders.ExceptingClassLoader.PrefixClassLoaderExceptor;
import lightning.websockets.WebSocketHandler;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

/**
 * Responsible for scanning the class path for annotations needed by the framework.
 * TODO: Can we speed this up to make debug mode faster? Not going to be scalable to MASSIVE applications.
 */
public class Scanner {
  private final List<String> reloadPrefixes;
  private final List<String> scanPrefixes;
  private final boolean enableAutoReload;
  private static final Logger logger = LoggerFactory.getLogger(Scanner.class);
  
  /**
   * @param classLoader The class loader to use (default one in most cases)
   * @param scanPrefixes List of package prefixes to scan within (e.g. ["lightning.controllers"])
   * @param enableAutoReload 
   */
  public Scanner(List<String> reloadPrefixes, List<String> scanPrefixes, boolean enableAutoReload) {
    this.reloadPrefixes = reloadPrefixes;
    this.scanPrefixes = scanPrefixes;
    this.enableAutoReload = enableAutoReload;
  }
  
  private static void putMethod(Map<Class<?>, Set<Method>> map, Method method) {
    Class<?> clazz = method.getDeclaringClass();
    
    if (!map.containsKey(clazz)) {
      map.put(clazz, new HashSet<>());
    }
    
    map.get(clazz).add(method);
  }
  
  public ScanResult scan() {
    Set<Class<?>> controllers = new HashSet<>();
    Map<Class<?>, Set<Method>> initializers = new HashMap<>();
    Map<Class<?>, Set<Method>> finalizers = new HashMap<>();
    Map<Class<?>, Set<Method>> exceptionHandlers = new HashMap<>();
    Map<Class<?>, Set<Method>> routes = new HashMap<>();
    Set<Class<? extends WebSocketHandler>> websockets = new HashSet<>();
    Map<Class<?>, Set<Method>> beforeFilters = new HashMap<>();
    ClassLoader classLoader = enableAutoReload
        ? new ExceptingClassLoader(new PrefixClassLoaderExceptor(reloadPrefixes), "target/classes")
        : this.getClass().getClassLoader();
    
    for (Reflections scanner : reflections(classLoader)) {
      for (Method m : scanner.getMethodsAnnotatedWith(Initializer.class)) {
        // TODO: check returns void, can inject
        if (m.getDeclaringClass().getAnnotation(Controller.class) != null &&
            !Modifier.isStatic(m.getModifiers()) &&
            !Modifier.isAbstract(m.getModifiers()) &&
            Modifier.isPublic(m.getModifiers())) {
          controllers.add(m.getDeclaringClass());
          putMethod(initializers, m);
        } else {
          logger.error(
                "ERROR: Could not install @Initializer for {}. "
              + "Initializers must be public, concrete, non-static, and declared inside of an @Controller.", m);
        }
      }
      
      for (Method m : scanner.getMethodsAnnotatedWith(Finalizer.class)) {
        // TODO: check returns void, can inject
        if (m.getDeclaringClass().getAnnotation(Controller.class) != null &&
            !Modifier.isStatic(m.getModifiers()) &&
            !Modifier.isAbstract(m.getModifiers()) &&
            Modifier.isPublic(m.getModifiers())) {
          controllers.add(m.getDeclaringClass());
          putMethod(finalizers, m);
        } else {
          logger.error(
                "ERROR: Could not install @Finalizer for {}. "
              + "Finalizers must be public, concrete, non-static, and declared inside of an @Controller.", m);
        }
      }
      
      for (Method m : scanner.getMethodsAnnotatedWith(ExceptionHandler.class)) {
        // TODO: check returns void, can inject
        if (Modifier.isStatic(m.getModifiers()) &&
            Modifier.isPublic(m.getModifiers()) &&
            !Modifier.isAbstract(m.getModifiers())) {
          putMethod(exceptionHandlers, m);
        } else {
          logger.error(
              "ERROR: Could not install @ExceptionHandler for {}. "
            + "ExceptionHandlers must be public, concrete, static.", m);
        }
      }
      
      for (Class<?> c : scanner.getTypesAnnotatedWith(WebSocket.class)) {
        if (!Modifier.isPublic(c.getModifiers())) {
          logger.error("ERROR: @WebSocket {} must be public.", c.getCanonicalName());
          continue;
        }
        
        if (Modifier.isAbstract(c.getModifiers()) || c.isInterface()) {
          logger.error("ERROR: @WebSocket {} must be instantiatable.", c.getCanonicalName());
          continue;
        }
        
        if (!WebSocketHandler.class.isAssignableFrom(c)) {
          logger.error("ERROR: @WebSocket {} must implement interface WebSocketHandler.", c.getCanonicalName());
          continue;
        }
        
        if (c.getConstructors().length > 1) {
          // TODO: Should also check the constructor is public and injectable.
          logger.error("ERROR: @WebSocket {} must have one public constructor.", c.getCanonicalName());
          continue;
        }
        
        @SuppressWarnings("unchecked")
        Class<? extends WebSocketHandler> cl = (Class<? extends WebSocketHandler>)c;
        websockets.add(cl);
      }
      
      for (Method m : Iterables.concat(scanner.getMethodsAnnotatedWith(Before.class), scanner.getMethodsAnnotatedWith(Befores.class))) {
        // TODO: verify return value, can inject
        if (Modifier.isStatic(m.getModifiers()) &&
            Modifier.isPublic(m.getModifiers()) &&
            !Modifier.isAbstract(m.getModifiers())) {
          putMethod(beforeFilters, m);
        } else {
          logger.error(
              "ERROR: Could not install @Before filter for {}. "
            + "Before filters must be public, concrete, static.", m);
        }
      }
      
      for (Method m : Iterables.concat(scanner.getMethodsAnnotatedWith(Route.class), scanner.getMethodsAnnotatedWith(Routes.class))) {
        // TODO: verify can inject, verify any @Filter(s) constructors are injectable
        if (m.getDeclaringClass().getAnnotation(Controller.class) != null &&
            !Modifier.isStatic(m.getModifiers()) &&
            !Modifier.isAbstract(m.getModifiers()) &&
            Modifier.isPublic(m.getModifiers())) {
          controllers.add(m.getDeclaringClass());
          putMethod(routes, m);
        } else {
          logger.error(
                "ERROR: Could not install @Route for {}. "
              + "Routes must be public, concrete, non-static, and declared inside of an @Controller.", m);
        }
      }
    }
    
    // TODO: Should check all controller constructors are injectable.
    
    return new ScanResult(controllers, initializers, exceptionHandlers, routes, websockets, finalizers, beforeFilters);
  }
  
  private Reflections[] reflections(ClassLoader classLoader) {
    Reflections[] result = new Reflections[scanPrefixes.size()];
    
    int i = 0;
    for (String searchPath : scanPrefixes) {
      ConfigurationBuilder config = ConfigurationBuilder.build(
          searchPath, classLoader, 
          new SubTypesScanner(), 
          new TypeAnnotationsScanner(), 
          new MethodAnnotationsScanner());
      result[i] = new Reflections(config);
      i++;
    }
    
    return result;
  }
}
