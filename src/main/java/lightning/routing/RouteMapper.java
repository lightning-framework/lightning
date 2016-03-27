package lightning.routing;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lightning.enums.HTTPMethod;
import lightning.http.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustl.pathtravelagent.DefaultPathToPathSegments;
import com.augustl.pathtravelagent.DefaultRouteMatcher;
import com.augustl.pathtravelagent.IRequest;
import com.augustl.pathtravelagent.IRouteHandler;
import com.augustl.pathtravelagent.ParametricChild;
import com.augustl.pathtravelagent.RouteMatch;
import com.augustl.pathtravelagent.RouteTreeNode;
import com.augustl.pathtravelagent.segment.IParametricSegment;
import com.augustl.pathtravelagent.segment.StringSegment;

/**
 * Performs route mapping and matching using a variant of a radix tree.
 * Matching time is always proportional to the number of path segments in the request regardless of how many routes are present.
 * 
 * To use, add all of the routes you wish to have and then call compile().
 * Compile assembles the radix tree to be used for route matching (very fast).
 * Compilation is only necessary once (after all routes have been added). 
 * @param <T>
 */
public class RouteMapper<T> {  
  private static final Logger logger = LoggerFactory.getLogger(RouteMapper.class);
  
  static final class RouteRequest implements IRequest {
    private final Request request;
    
    public RouteRequest(Request request) {
      this.request = request;
    }
    
    public Request getRequest() {
      return request;
    }
    
    @Override
    public List<String> getPathSegments() {
      return DefaultPathToPathSegments.parse(request.raw().getPathInfo());
    }
  }
  
  /**
   * Represents a route match (in the format it will be returned to clients).
   * @param <T>
   */
  public static final class Match<T> {
    private final T data;
    private final Map<String, String> params;
    private final List<String> wildcards;
    
    public Match(T data, Map<String, String> params, List<String> wildcards) {
      this.data = data;
      this.params = params;
      this.wildcards = wildcards;
    }
    
    public T getData() {
      return data;
    }
    
    public Map<String, String> getParams() {
      return params;
    }
    
    public List<String> getWildcards() {
      return wildcards;
    }
  }
  
  static final class RouteMatchHandler<T> implements IRouteHandler<RouteRequest, Match<T>> {
    protected final PendingRoute<T> route;
    protected final List<String> routePath;
    
    public RouteMatchHandler(PendingRoute<T> route) {
      this.route = route;
      this.routePath = DefaultPathToPathSegments.parse(route.path);
    }
    
    @Override
    public IRouteHandler<RouteRequest, Match<T>> merge(
        IRouteHandler<RouteRequest, Match<T>> other) {
      throw new IllegalStateException("Found duplicate/incompatible routes for path: " + route.method + " " + route.path);
    }

    @Override
    public Match<T> call(RouteMatch<RouteRequest> match) {
      List<String> wildcards = match.getWildcardRouteMatchResult();
      Iterator<String> results = match.getRouteMatchResult().getStringMatchesInOrder().iterator();
      
      // Build the mapping of parameter names to values for this match.
      Map<String, String> params = new HashMap<>();
      
      for (String segment : routePath) {
        if (segment.startsWith(":")) {
          params.put(segment.substring(1), match.getRouteMatchResult().getStringMatch(results.next()));
        }
      }
      
      logger.debug("Found Match: data={} segments={} wildcards={}, params={}", 
          route.action, match.getRequest().getPathSegments(), wildcards, params);
      
      return new Match<>(route.action, params, wildcards);
    }
  }
  
  /**
   * Represents the route (in the format that it will be provided by clients).
   * @param <T>
   */
  static final class PendingRoute<T> {
    public final HTTPMethod method;
    public final String path;
    public final T action;
    
    public PendingRoute(HTTPMethod method, String path, T action) {
      this.method = method;
      this.path = path;
      this.action = action;
    }
  }
  
  @SuppressWarnings("serial")
  static final class RouteFormatException extends Exception {
    public RouteFormatException(String message, Exception cause) {
      super(message, cause);
    }
    
    public RouteFormatException(String message) {
      super(message);
    }
  }
  
  /**
   * A builder used to help assemble the routing radix tree.
   * @param <T_REQ>
   * @param <T_RES>
   */
  static final class RouteTreeNodeBuilder<T_REQ extends IRequest, T_RES> {
    private final String pathPrefix = "/";
    private final Pattern validSegmentChars = Pattern.compile("[\\w\\-\\._~]+");
    public Map<String, RouteTreeNodeBuilder<T_REQ, T_RES>> pathChildren = new HashMap<>();
    public RouteTreeNodeBuilder<T_REQ, T_RES> wildcardChild;
    public RouteTreeNodeBuilder<T_REQ, T_RES> parametricChild;
    public IParametricSegment parametricSegment;
    public IRouteHandler<T_REQ, T_RES> handler;
    
    public RouteTreeNodeBuilder<T_REQ, T_RES> path(String pathName) {
      pathName = pathName.startsWith(pathPrefix) ? pathName.substring(pathPrefix.length()) : pathName;
      ensureContainsValidSegmentChars(pathName);
      if (pathChildren.containsKey(pathName)) {
        return pathChildren.get(pathName);
      } else {
        RouteTreeNodeBuilder<T_REQ, T_RES> child = new RouteTreeNodeBuilder<>();
        pathChildren.put(pathName, child);
        return child;
      }
    }
    
    public RouteTreeNodeBuilder<T_REQ, T_RES> parametric() {
      if (wildcardChild != null) {
        throw new IllegalStateException("Incompatible handler for path.");
      }
      
      if (parametricChild == null) {
        parametricChild = new RouteTreeNodeBuilder<T_REQ, T_RES>();
        parametricSegment = new StringSegment(UUID.randomUUID().toString());
      }
      
      return parametricChild;
    }
    
    public RouteTreeNodeBuilder<T_REQ, T_RES> wildcard() {
      if (parametricChild != null) {
        throw new IllegalStateException("Incompatible handler for path.");
      }
      
      if (wildcardChild == null) {
        wildcardChild = new RouteTreeNodeBuilder<T_REQ, T_RES>();
      }
      
      return wildcardChild;
    }
    
    public void handler(IRouteHandler<T_REQ, T_RES> handler) {
      if (this.handler != null) {
        throw new IllegalStateException("Duplicate handler for path.");
      }
      
      this.handler = handler;
    }
    
    public RouteTreeNode<T_REQ, T_RES> build() {
      return build("::ROOT::");
    }
    
    private RouteTreeNode<T_REQ, T_RES> build(String nodeName) {
      HashMap<String, RouteTreeNode<T_REQ, T_RES>> children = new HashMap<>();
      
      for (String key : this.pathChildren.keySet()) {
        children.put(key, this.pathChildren.get(key).build(key));
      }
            
      return new RouteTreeNode<>(
          nodeName,
          this.handler,
          children,
          this.parametricChild != null ? new ParametricChild<>(parametricSegment, this.parametricChild.build("::PARAM:" + parametricSegment.getParamName() + "::")) : null,
          this.wildcardChild != null ? this.wildcardChild.build("::WILDCARD::") : null);
    }
    
    private void ensureContainsValidSegmentChars(String str) {
      Matcher matcher = validSegmentChars.matcher(str);
      if (!matcher.matches()) {
        throw new IllegalArgumentException("Param " + str + " contains invalid characters");
      }
    }
  }
  
  private DefaultRouteMatcher<RouteRequest, Match<T>> matcher;
  private Map<HTTPMethod, RouteTreeNode<RouteRequest, Match<T>>> routes;
  private Map<HTTPMethod, List<PendingRoute<T>>> pending;
  
  public RouteMapper() {
    matcher = new DefaultRouteMatcher<>();
    routes = new EnumMap<>(HTTPMethod.class);
    pending = new EnumMap<>(HTTPMethod.class);
  }

  /**
   * @param method An HTTP method.
   * @param path A routing path (e.g. /path/:variable/*).
   * @param action An object to be returned when this route is matched.
   */
  public void map(HTTPMethod method, String path, T action) {
    if (!pending.containsKey(method)) {
      pending.put(method, new ArrayList<>());
    }
    
    pending.get(method).add(new PendingRoute<>(method, path, action));
  }
  
  /**
   * Clears all existing routes.
   */
  public void clear() {
    pending.clear();
    routes.clear();
  }
  
  /**
   * Attempts to match an HTTP request to a route.
   * @param request An HTTP request.
   * @return A routing match (if one exists) or null otherwise.
   */
  public Match<T> lookup(Request request) {
    if (!routes.containsKey(request.method())) {
      return null; 
    }
    
    return matcher.match(routes.get(request.method()), new RouteRequest(request));
  }
    
  /**
   * A recursive helper function for assembling the routing radix tree.
   * @param route The route being processed.
   * @param node The current node in the tree.
   * @param components An iterator over components of the path.
   * @param action The action to 
   * @throws RouteFormatException
   */
  private void buildTree(PendingRoute<T> route, RouteTreeNodeBuilder<RouteRequest, Match<T>> node, Iterator<String> components) throws RouteFormatException {
    if (!components.hasNext()) {
      node.handler(new RouteMatchHandler<T>(route));
      return;
    }
    
    String component = components.next();
    RouteTreeNodeBuilder<RouteRequest, Match<T>> newChild;
        
    if (component.startsWith(":")) {
      newChild = node.parametric();
    } else if (component.equals("*")) {
      if (components.hasNext()) {
        throw new RouteFormatException("Invalid route format (wildcards may only appear at the end of paths): " + route.method + " " + route.path);
      }
      
      newChild = node.wildcard();
    } else {
      newChild = node.path(component);
    }
    
    buildTree(route, newChild, components);
  }
  
  /**
   * Compiles the routing radix tree. 
   * This needs to be called at least once after all routes are added.
   * Must re-compile the radix tree each time routes are modified.
   * @throws RouteFormatException
   */
  public void compile() throws RouteFormatException {
    routes.clear();
    
    for (HTTPMethod method : pending.keySet()) {
      RouteTreeNodeBuilder<RouteRequest, Match<T>> root = new RouteTreeNodeBuilder<>();
      
      for (PendingRoute<T> route : pending.get(method)) {
        logger.debug("Installing Route: {} {} -> {}", route.method, route.path, route.action);
        List<String> components = DefaultPathToPathSegments.parse(route.path);
        
        try {
          buildTree(route, root, components.iterator());
        } catch (IllegalArgumentException e) {
          routes.clear();
          throw new RouteFormatException("Routing path " + method + " " + route.path + " contains illegal characters.");
        } catch (IllegalStateException e) {
          routes.clear();
          throw new RouteFormatException("Duplicate/incompatible routing path " + method + " " + route.path + ".");
        }
      }

      logger.debug("Built Route Tree: {} -> {}" , method, root.build());
      routes.put(method, root.build());
    }    
  }  
}
