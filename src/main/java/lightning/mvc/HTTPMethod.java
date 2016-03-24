package lightning.mvc;

import spark.Spark;
import spark.TemplateEngine;
import spark.TemplateViewRoute;

public enum HTTPMethod {
  GET,
  POST,
  HEAD,
  DELETE,
  PUT,
  OPTIONS,
  TRACE;
  
  public void installRoute(String path, String accepts, TemplateViewRoute route, TemplateEngine engine) {
    switch (this) {
      case DELETE:
        Spark.delete(path, accepts, route, engine);
        break;
      case GET:
        Spark.get(path, accepts, route, engine);
        break;
      case HEAD:
        Spark.head(path, accepts, route, engine);
        break;
      case OPTIONS:
        Spark.options(path, accepts, route, engine);
        break;
      case POST:
        Spark.post(path, accepts, route, engine);
        break;
      case PUT:
        Spark.put(path, accepts, route, engine);
        break;
      case TRACE:
        Spark.trace(path, accepts, route, engine);
        break;      
    }
  }
  
  public void installRoute(String path, String accepts, spark.Route route) {
    switch (this) {
      case DELETE:
        Spark.delete(path, accepts, route);
        break;
      case GET:
        Spark.get(path, accepts, route);
        break;
      case HEAD:
        Spark.head(path, accepts, route);
        break;
      case OPTIONS:
        Spark.options(path, accepts, route);
        break;
      case POST:
        Spark.post(path, accepts, route);
        break;
      case PUT:
        Spark.put(path, accepts, route);
        break;
      case TRACE:
        Spark.trace(path, accepts, route);
        break;      
    }
  }
}
