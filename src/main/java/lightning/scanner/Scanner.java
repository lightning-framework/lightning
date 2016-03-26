package lightning.scanner;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lightning.ann.Controller;
import lightning.ann.ExceptionHandler;
import lightning.ann.Initializer;
import lightning.ann.Route;
import lightning.ann.Routes;
import lightning.ann.WebSocketFactory;

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
 */
public class Scanner {
  private final ClassLoader classLoader;
  private final List<String> prefixes;
  private static final Logger logger = LoggerFactory.getLogger(Scanner.class);
  
  /**
   * @param classLoader The class loader to use (default one in most cases)
   * @param scanPrefixes List of package prefixes to scan within (e.g. ["lightning.controllers"])
   */
  public Scanner(ClassLoader classLoader, List<String> scanPrefixes) {
    this.classLoader = classLoader;
    this.prefixes = scanPrefixes;
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
    Map<Class<?>, Set<Method>> exceptionHandlers = new HashMap<>();
    Map<Class<?>, Set<Method>> routes = new HashMap<>();
    Map<Class<?>, Set<Method>> websocketFactories = new HashMap<>();
    
    for (Reflections scanner : reflections()) {      
      for (Method m : scanner.getMethodsAnnotatedWith(Initializer.class)) {
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
      
      for (Method m : scanner.getMethodsAnnotatedWith(ExceptionHandler.class)) {
        // TODO: verify argument is subclass of exception, returns void
        if (Modifier.isStatic(m.getModifiers()) &&
            Modifier.isPublic(m.getModifiers()) &&
            !Modifier.isAbstract(m.getModifiers()) &&
            m.getParameterCount() == 1) {
          putMethod(exceptionHandlers, m);
        } else {
          logger.error(
              "ERROR: Could not install @ExceptionHandler for {}. "
            + "ExceptionHandlers must be public, concrete, static, and accept a single Exception as input.", m);
        }
      }
      
      for (Method m : scanner.getMethodsAnnotatedWith(WebSocketFactory.class)) {
        // TODO: verify number of parameters and parameter types, return value
        if (Modifier.isStatic(m.getModifiers()) &&
            Modifier.isPublic(m.getModifiers()) &&
            !Modifier.isAbstract(m.getModifiers())) {
          putMethod(websocketFactories, m);
        } else {
          logger.error(
              "ERROR: Could not install @WebSocketFactory for {}. "
            + "WebSocketFactory must be public, concrete, static.", m);
        }
      }
      
      for (Method m : Iterables.concat(scanner.getMethodsAnnotatedWith(Route.class), scanner.getMethodsAnnotatedWith(Routes.class))) {
        // TODO: verify parameter types
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
    
    return new ScanResult(controllers, initializers, exceptionHandlers, routes, websocketFactories);
  }
  
  private Reflections[] reflections() {
    Reflections[] result = new Reflections[prefixes.size()];
    
    int i = 0;
    for (String searchPath : prefixes) {
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
