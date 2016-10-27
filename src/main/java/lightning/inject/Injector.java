package lightning.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import lightning.ann.Inject;
import lightning.ann.QParam;
import lightning.ann.RParam;
import lightning.http.Request;

import com.google.common.collect.ImmutableMap;

/**
 * A simple dependency injector that works at the method invocation level.
 * 
 * This is different from libraries like Guice and Dagger (which tend to operate
 * on constructors or instance properties). Further, this library does not have
 * any notion of a dependency/injection graph.
 * 
 * InjectorModules specify a set of injection bindings. There are three types of
 * bindings. Injections can be triggered on method parameters either by (in order
 * of resolution priority):
 *  Name (by annotating the parameter with @Inject(name))
 *  Annotation (by annotating the parameter with that annotation)
 *  Type (automatically inferred)
 *  
 * The bound values can be either Resolvers (which produce the injected value at
 * a later point in time when it is needed) or a single instance of an object.
 * 
 * Please keep in mind that Resolvers were not intended to be factories; rather,
 * Resolvers were intended to be used for lazy instantiation.
 *  
 * Our goal is for a single instance to be bound to be injected always for any of
 * the above triggers. Thus, our recommendation if you need factory-like behavior 
 * is to inject a factory into your method and then invoke the factory within your 
 * method.
 * 
 * For example, for a route handler, you might inject a database connection pool and 
 * then lease connections from the pooler within the handler.
 *  
 * Injections are bound as a list of modules which enable bindings based on the above 
 * triggers. Resolution of injections occurs by prioritizing the modules in the order 
 * they are provided to the Injector.
 * 
 * NOTE: Injected dependencies MUST NOT be within the autoreload packages.
 *       Otherwise, dependency injection WILL FAIL in debug mode.
 * TODO: Should show errors at start-up when attempting to inject something in an autoreload package.
 */
public class Injector {
  private final InjectorModule[] modules;
 
  /**
   * @param modules The prioritized list of injection modules to use for resolving parameters.
   */
  public Injector(InjectorModule... modules) {
    this.modules = modules;
  }
  
  /**
   * @param m A method.
   * @return The parameters that should be used to invoke the given method via reflection. 
   *         These parameters were produced by the InjectorModules provided at instantiation.
   * @throws Exception On failure (for example, if unable to resolve all parameters).
   */
  public Object[] getInjectedArguments(Method m) throws Exception {
    try {
      return getInjectedArguments(m.getParameters());
    } catch (InjectionException e) {
      throw new InjectionException("Failed to inject dependencies for method " + m + ": " + e.getMessage());
    }
  }
  
  /**
   * @param m A constructor.
   * @return The parameters that should be used to invoke the given constructor via reflection. 
   *         These parameters were produced by the InjectorModules provided at instantiation.
   * @throws Exception On failure (for example, if unable to resolve all parameters).
   */
  public Object[] getInjectedArguments(Constructor<?> m) throws Exception {
    try {
      return getInjectedArguments(m.getParameters());
    } catch (InjectionException e) {
      throw new InjectionException("Failed to inject dependencies for method " + m + ": " + e.getMessage());
    }
  }
  
  /**
   * @param type A class with a single constructor.
   * @return An instance of the given class produced by invoking its only constructor using the parameters
   *         produced by getInjectedArguments on the constructor.
   * @throws Exception On failure (more than one constructor, if unable to resolve all parameters).
   */
  public <T> T newInstance(Class<T> type) throws Exception {
    int numConstructors = type.getConstructors().length;
    
    if (numConstructors > 1) {
      throw new IllegalStateException("May only inject on classes with a single constructor (found " + numConstructors + ").");
    }
    
    if (numConstructors == 0) {
      return (T) type.newInstance();
    }
    
    @SuppressWarnings("unchecked")
    Constructor<T> constructor = (Constructor<T>) type.getConstructors()[0];
    Object[] arguments = getInjectedArguments(constructor);
    return (T) constructor.newInstance(arguments);
  }
  
  /**
   * @param parameters A list of method parameters.
   * @return A list of objects corresponding to the given parameters (1:1) by resolving the parameters
   *         using the InjectorModules provided at instantiation.
   * @throws Exception On failure (for example, if unable to resolve all parameters).
   */
  public Object[] getInjectedArguments(Parameter[] parameters) throws Exception {
    Object[] args = new Object[parameters.length];
    
    for (int i = 0; i < parameters.length; i++) {
      Parameter p = parameters[i];
      args[i] = getInjectedArgument(p);
      
      if (args[i] == null) {
        throw new InjectionException("Unable to resolve parameter " + p);
      }
      
      if (!typeCompatible(p.getType(), args[i].getClass())) {
        throw new InjectionException("Unable to resolve parameter " + p + " (wrong type " + args[i].getClass() + ")");
      }
    }
    
    return args;
  }
  
  /**
   * Returns whether or not two types are compatible.
   * @param expected The parameter type.
   * @param actual The actual type.
   * @return Whether or not actual can be assigned to type expected.
   */
  public boolean typeCompatible(Class<?> expected, Class<?> actual) {
    return objectType(expected).isAssignableFrom(actual);
  }
  
  private Map<Class<?>, Class<?>> conversions = ImmutableMap.<Class<?>, Class<?>>builder()
      .put(int.class, Integer.class)
      .put(float.class, Float.class)
      .put(long.class, Long.class)
      .put(boolean.class, Boolean.class)
      .put(double.class, Double.class)
      .build();
  
  private Class<?> objectType(Class<?> type) {
    if (conversions.containsKey(type)) {
      return conversions.get(type);
    }
    
    return type;
  }
  
  /**
   * Returns the value to be bound to a given method parameter.
   * @param p A method parameter.
   * @return The instance to be bound to the given parameter based on the InjectorModules provided
   *         at instantiation.
   * @throws Exception On failure (for example, if unable to resolve).
   */
  public Object getInjectedArgument(Parameter p) throws Exception {
    if (p.getAnnotations().length > 0) {
      if (p.isAnnotationPresent(Inject.class)) {
        String name = p.getAnnotation(Inject.class).value();
        return getInjectedArgumentForName(name);
      }
      
      if (p.isAnnotationPresent(QParam.class)) {
        String name = p.getAnnotation(QParam.class).value();
        return getInjectedArgumentForClass(Request.class).queryParam(name).castTo(p.getType());
      }
      
      if (p.isAnnotationPresent(RParam.class)) {
        String name = p.getAnnotation(RParam.class).value();
        return getInjectedArgumentForClass(Request.class).routeParam(name).castTo(p.getType());
      }
      
      for (Annotation a : p.getAnnotations()) {
        Object result = getInjectedArgumentForAnnotation(a.annotationType());
        if (result != null) {
          return result;
        }
      }
    }
    
    return getInjectedArgumentForClass(p.getType());
  }
  
  /**
   * Returns the object bound to the given name (if any) using the InjectorModules provided
   * at instantiation.
   * @param name A unique name.
   * @return The value bound to the given name, or null if none.
   * @throws Exception
   */
  public Object getInjectedArgumentForName(String name) throws Exception {
    for (InjectorModule m : modules) {
      Object result = m.getBindingForName(name);
      
      if (result != null) {
        return result;
      }
    }
    
    return null;
  }
  
  /**
   * Returns the object bound to the given class (if any) using the InjectorModules provided
   * at instantiation.
   * @param clazz A type.
   * @return The value bound to the given type, or null if none.
   * @throws Exception
   */
  public <T> T getInjectedArgumentForClass(Class<T> clazz) throws Exception {
    for (InjectorModule m : modules) {
      T result = m.getBindingForClass(clazz);
      
      if (result != null) {
        return result;
      }
    }
    
    return null;
  }
  
  /**
   * Returns the object bound to the given annotation (if any) using the InjectorModules provided
   * at instantation.
   * @param annotation An annotation for method parameters.
   * @return The value bound to the given annotation, or null if none.
   * @throws Exception
   */
  public Object getInjectedArgumentForAnnotation(Class<? extends Annotation> annotation) throws Exception {
    for (InjectorModule m : modules) {
      Object result = m.getBindingForAnnotation(annotation);
      
      if (result != null) {
        return result;
      }
    }
    
    return null;
  }
}
