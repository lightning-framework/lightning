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
 * This is slightly different from libraries like Guice and Dagger (which tend to
 * operate on constructors).
 * 
 * Injections can be triggered on method parameters either by...
 *  Type (automatically)
 *  Name (via @Inject(name))
 *  Annotation (by annotating the parameter with that annotation)
 *  
 * Injections are bound as a list of modules which enable bindings based
 * on the above triggers. Modules are priotized in the order they are
 * provided.
 * 
 * NOTE: Injected dependencies MUST NOT be within the autoreload packages.
 *       Otherwise, dependency injection WILL FAIL in debug mode.
 * TODO: Should show errors at start-up when attempting to inject something in an autoreload package.
 */
public class Injector {
  private final InjectorModule[] modules;
  
  public Injector(InjectorModule... modules) {
    this.modules = modules;
  }
  
  public Object[] getInjectedArguments(Method m) throws Exception {
    try {
      return getInjectedArguments(m.getParameters());
    } catch (InjectionException e) {
      throw new InjectionException("Failed to inject dependencies for method " + m + ": " + e.getMessage());
    }
  }
  
  public Object[] getInjectedArguments(Constructor<?> m) throws Exception {
    try {
      return getInjectedArguments(m.getParameters());
    } catch (InjectionException e) {
      throw new InjectionException("Failed to inject dependencies for method " + m + ": " + e.getMessage());
    }
  }
  
  public <T> T newInstance(Class<T> type) throws Exception {
    if (type.getConstructors().length != 1) {
      throw new IllegalStateException("May only inject on classes with a single constructor.");
    }
    
    @SuppressWarnings("unchecked")
    Constructor<T> constructor = (Constructor<T>) type.getConstructors()[0];
    Object[] arguments = getInjectedArguments(constructor);
    return (T) constructor.newInstance(arguments);
  }
  
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
  
  public Object getInjectedArgumentForName(String name) throws Exception {
    for (InjectorModule m : modules) {
      Object result = m.getBindingForName(name);
      
      if (result != null) {
        return result;
      }
    }
    
    return null;
  }
  
  public <T> T getInjectedArgumentForClass(Class<T> clazz) throws Exception {
    for (InjectorModule m : modules) {
      T result = m.getBindingForClass(clazz);
      
      if (result != null) {
        return result;
      }
    }
    
    return null;
  }
  
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
