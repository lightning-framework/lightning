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
    protected final T data;
    protected final PendingRoute<T> route;
    protected final List<String> routePath;
    
    public RouteMatchHandler(T data, PendingRoute<T> route) {
      this.data = data;
      this.route = route;
      this.routePath = DefaultPathToPathSegments.parse(route.path);
    }
    
    @Override
    public IRouteHandler<RouteRequest, Match<T>> merge(
        IRouteHandler<RouteRequest, Match<T>> other) {
      throw new IllegalStateException("Cannot have duplicate handlers for same (path, method).");
    }

    @Override
    public Match<T> call(RouteMatch<RouteRequest> match) {
      List<String> wildcards = match.getWildcardRouteMatchResult();
      Iterator<String> results = match.getRouteMatchResult().getStringMatchesInOrder().iterator();
      Map<String, String> params = new HashMap<>();
      
      for (String segment : routePath) {
        logger.debug("Checking segment: {}", segment);
        if (segment.startsWith(":")) {
          logger.debug("Finding param for segment: {}", segment);
          params.put(segment.substring(1), match.getRouteMatchResult().getStringMatch(results.next()));
        }
      }
      
      logger.debug("Found Match: data={} segments={} wildcards={}, params={}", 
          data, match.getRequest().getPathSegments(), wildcards, params);
      
      return new Match<>(data, params, wildcards);
    }
  }
  
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
      if (parametricChild == null) {
        parametricChild = new RouteTreeNodeBuilder<T_REQ, T_RES>();
        parametricSegment = new StringSegment(UUID.randomUUID().toString());
      }
      
      return parametricChild;
    }
    
    public RouteTreeNodeBuilder<T_REQ, T_RES> wildcard() {
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

  public void map(HTTPMethod method, String path, T action) {
    if (!pending.containsKey(method)) {
      pending.put(method, new ArrayList<>());
    }
    
    pending.get(method).add(new PendingRoute<>(method, path, action));
  }
  
  public void clear() {
    pending.clear();
    routes.clear();
  }
  
  public Match<T> lookup(Request request) {
    if (!routes.containsKey(request.method())) {
      return null; 
    }
    
    return matcher.match(routes.get(request.method()), new RouteRequest(request));
  }
    
  private void buildTree(PendingRoute<T> route, RouteTreeNodeBuilder<RouteRequest, Match<T>> node, Iterator<String> components, T action) throws RouteFormatException {
    if (!components.hasNext()) {
      node.handler(new RouteMatchHandler<T>(action, route));
      return;
    }
    
    String component = components.next();
    RouteTreeNodeBuilder<RouteRequest, Match<T>> newChild;
    
    if (component.startsWith(":")) {
      newChild = node.parametric();
    } else if (component.equals("*")) {
      if (components.hasNext()) {
        throw new RouteFormatException("Wildcards may only occur at the end of paths.");
      }
      
      newChild = node.wildcard();
    } else {
      newChild = node.path(component);
    }
    
    buildTree(route, newChild, components, action);
  }
  
  public void compile() throws RouteFormatException {
    for (HTTPMethod method : pending.keySet()) {
      RouteTreeNodeBuilder<RouteRequest, Match<T>> root = new RouteTreeNodeBuilder<>();
      
      for (PendingRoute<T> route : pending.get(method)) {
        logger.debug("Installing Route: {} {} -> {}", route.method, route.path, route.action);
        List<String> components = DefaultPathToPathSegments.parse(route.path);
        
        try {
          buildTree(route, root, components.iterator(), route.action);
        } catch (IllegalArgumentException e) {
          throw new RouteFormatException("Routing path " + method + " " + route.path + " contains illegal characters.", e);
        } catch (IllegalStateException e) {
          throw new RouteFormatException("Duplicate routing path " + method + " " + route.path + ".", e);
        }
      }

      logger.debug("Built Route Tree: {} -> {}" , method, root.build());
      routes.put(method, root.build());
    }    
  }  
}
