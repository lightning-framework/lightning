package lightning.inject;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * A dependency injection module specifies injection bindings.
 * See documentation for Injector.
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
  
  public <T> void bindClassToInstance(Class<T> clazz, T instance) {    
    Class<?> currentClass = clazz;
    
    while (currentClass != null && !classBindings.containsKey(currentClass)) {
      classBindings.put(currentClass, instance);
      currentClass = currentClass.getSuperclass();
    }
  }
  
  public <T> void bindClassToResolver(Class<T> clazz, Resolver<T> resolver) {    
    Class<?> currentClass = clazz;
    
    while (currentClass != null && !classBindings.containsKey(currentClass)) {
      classResolverBindings.put(currentClass, resolver);
      currentClass = currentClass.getSuperclass();
    }
  }
  
  public <T> void bindNameToInstance(String name, T instance) {
    nameBindings.put(name, instance);
  }
  
  public <T> void bindNameToResolver(String name, Resolver<T> resolver) {
    nameResolverBindings.put(name, resolver);
  }
  
  public <T> void bindAnnotationToInstance(Class<? extends Annotation> annotation, T instance) {
    annotationBindings.put(annotation, instance);
  }
  
  public <T> void bindAnnotationToResolver(Class<? extends Annotation> annotation, Resolver<T> resolver) {
    annotationResolverBindings.put(annotation, resolver);
  }
  
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

  public void bindToClass(Throwable e) {
    Class<?> currentClass = e.getClass();
    
    while (currentClass != null && !classBindings.containsKey(currentClass)) {
      classBindings.put(currentClass, e);
      currentClass = currentClass.getSuperclass();
    }
  }
}
