package lightning.scanner;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import lightning.websockets.WebSocketHandler;

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
  
  // Static methods annotated with @Before:
  public final Map<Class<?>, Set<Method>> beforeFilters;
  
  // Static methods annotated with @WebSocketFactory:
  public final Set<Class<? extends WebSocketHandler>> websockets;
  
  public ScanResult(
      Set<Class<?>> controllers,
      Map<Class<?>, Set<Method>> initializers,
      Map<Class<?>, Set<Method>> exceptionHandlers,
      Map<Class<?>, Set<Method>> routes,
      Set<Class<? extends WebSocketHandler>> websockets,
      Map<Class<?>, Set<Method>> finalizers,
      Map<Class<?>, Set<Method>> beforeFilters) {
    this.controllers = controllers;
    this.initializers = initializers;
    this.exceptionHandlers = exceptionHandlers;
    this.routes = routes;
    this.websockets = websockets;
    this.finalizers = finalizers;
    this.beforeFilters = beforeFilters;
  }
  
  @Override
  public String toString() {
    return "ScanResult [controllers=" + controllers + ", initializers=" + initializers
        + ", exceptionHandlers=" + exceptionHandlers + ", routes=" + routes
        + ", websockets=" + websockets + ", finalizers=" + finalizers + ""
        + ", beforeFilters=" + beforeFilters + "]";
  }
}
