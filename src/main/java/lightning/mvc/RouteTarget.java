package lightning.mvc;

import lightning.enums.HTTPMethod;

/**
 * Used to indicate a route exists on a given path with a given method.
 */
public final class RouteTarget {
  @Override
  public String toString() {
    return "RouteTarget [method=" + method + ", path=" + path + "]";
  }

  public final HTTPMethod method;
  public final String path;
  
  public RouteTarget(HTTPMethod method, String path) {
    this.path = path;
    this.method = method;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((method == null) ? 0 : method.hashCode());
    result = prime * result + ((path == null) ? 0 : path.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    RouteTarget other = (RouteTarget) obj;
    if (method != other.method)
      return false;
    if (path == null) {
      if (other.path != null)
        return false;
    } else if (!path.equals(other.path))
      return false;
    return true;
  }
}
