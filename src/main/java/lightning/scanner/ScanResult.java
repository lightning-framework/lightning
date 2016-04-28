package lightning.scanner;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public class ScanResult {
  // Classes annotated with @Controller:
  public final Set<Class<?>> controllers;
  
  // Methods in @Controllers annotated with @Initialize:
  public final Map<Class<?>, Set<Method>> initializers;
  
  //Methods in @Controllers annotated with @Finalize:
  public final Map<Class<?>, Set<Method>> finalizers;
  
  // Static methods annotated with @ExceptionHandler:
  public final Map<Class<?>, Set<Method>> exceptionHandlers;
  
  // Methods in @Controllers annotated with @Route(s):
  public final Map<Class<?>, Set<Method>> routes;
  
  // Static methods annotated with @WebSocketFactory:
  public final Map<Class<?>, Set<Method>> websocketFactories;
  
  public ScanResult(
      Set<Class<?>> controllers,
      Map<Class<?>, Set<Method>> initializers,
      Map<Class<?>, Set<Method>> exceptionHandlers,
      Map<Class<?>, Set<Method>> routes,
      Map<Class<?>, Set<Method>> websocketFactories,
      Map<Class<?>, Set<Method>> finalizers) {
    this.controllers = controllers;
    this.initializers = initializers;
    this.exceptionHandlers = exceptionHandlers;
    this.routes = routes;
    this.websocketFactories = websocketFactories;
    this.finalizers = finalizers;
  }
  
  @Override
  public String toString() {
    return "ScanResult [controllers=" + controllers + ", initializers=" + initializers
        + ", exceptionHandlers=" + exceptionHandlers + ", routes=" + routes
        + ", websocketFactories=" + websocketFactories + ", finalizers=" + finalizers + "]";
  }
}
