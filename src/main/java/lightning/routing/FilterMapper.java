package lightning.routing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import lightning.enums.FilterPriority;
import lightning.enums.HTTPMethod;

import com.augustl.pathtravelagent.DefaultPathToPathSegments;
import com.augustl.pathtravelagent.PathFormatException;
import com.google.common.collect.ImmutableList;

/**
 * Handles mapping of filters.
 * @param <T> The target type.
 */
public class FilterMapper<T> { 
  
  public static final class Filter<T> {
    public final T handler;
    public final FilterPriority priority;
    public final FilterType type;
    public final List<String> segments;
    
    public Filter(T handler, FilterPriority priority, FilterType type, List<String> segments) {
      this.handler = handler;
      this.priority = priority;
      this.type = type;
      this.segments = ImmutableList.copyOf(segments);
    }
    
    public Map<String, String> params(String path) throws PathFormatException {
      List<String> parts = DefaultPathToPathSegments.parse(path);
      Map<String, String> params = new HashMap<>();
      
      for (int i = 0; i < Math.min(segments.size(), parts.size()); i++) {
        if (segments.get(i).startsWith(":")) {
          params.put(segments.get(i).substring(1), parts.get(i));
        }
      }
      
      return params;
    }
    
    public List<String> wildcards(String path) throws PathFormatException {
      List<String> parts = DefaultPathToPathSegments.parse(path);
      List<String> wildcards = new ArrayList<>();
      
      if (segments.get(segments.size() - 1).equals("*")) {
        for (int i = segments.size() - 1; i < parts.size(); i++) {
          wildcards.add(parts.get(i));
        }
      }
      
      return wildcards;
    }
    
    @Override
    public String toString() {
      return String.format("Filter[handler=%s, priority=%s, type=%s, segments=%s]", handler, priority, type, segments);
    }
    
    @Override
    public int hashCode() {
      if (handler == null)
        return 0;
      return handler.hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
      if (!(other instanceof Filter)) {
        return false;
      }
      
      Filter<?> o = (Filter<?>) other;
      
      if (handler == null && o.handler != null) {
        return false;
      }
      
      if (handler != null && o.handler == null) {
        return false;
      }
      
      if (handler != null && o.handler != null && !handler.equals(o.handler)) {
        return false;
      }
      
      return true;
    }
  }
  
  public static final class FilterMatch<T> {
    private List<Filter<T>> before;
    private List<Filter<T>> after;
    
    public FilterMatch() {
      before = new ArrayList<>();
      after = new ArrayList<>();
    }
    
    public void addFilter(FilterType type, T handler, FilterPriority priority, List<String> segments) {
      Filter<T> filter = new Filter<>(handler, priority, type, segments);
      addFilter(filter);
    }
    
    public void addFilter(Filter<T> filter) {
      switch (filter.type) {
        case AFTER:
          after.add(filter);
          break;
        case BEFORE:
          before.add(filter);
          break;
      }
    }
    
    public void addAll(FilterMatch<T> match) {
      before.addAll(match.before);
      after.addAll(match.after);
    }
    
    public boolean hasMatches() {
      return !before.isEmpty() || !after.isEmpty();
    }
    
    public List<Filter<T>> beforeFilters() {
      before.sort(FILTER_CMP);
      return before;
    }
    
    public List<Filter<T>> afterFilters() {
      after.sort(FILTER_CMP);
      return after;
    }
    
    @Override
    public String toString() {
      return String.format("Match[before=%s, after=%s]", before, after);
    }
  }
  
  public static final class FilterComparator implements Comparator<Filter<?>> {
    @Override
    public int compare(Filter<?> arg0, Filter<?> arg1) {
      return arg1.priority.ordinal() - arg0.priority.ordinal();
    }
  }
  
  public static final FilterComparator FILTER_CMP = new FilterComparator();
  
  public static final class RadixNode<T> {
    private @Nullable FilterMatch<T> handlers = new FilterMatch<>();
    private @Nullable FilterMatch<T> wildcardHandlers = new FilterMatch<>();
    private @Nullable Map<String, RadixNode<T>> segmentChildren;
    private @Nullable RadixNode<T> parametricChild;
    
    public boolean hasWildcardHandlers() {
      return wildcardHandlers != null && wildcardHandlers.hasMatches();
    }
    
    public boolean hasHandlers() {
      return handlers != null && handlers.hasMatches();
    }
    
    public FilterMatch<T> handlers() {
      return handlers;
    }
    
    public FilterMatch<T> wildcardHandlers() {
      return wildcardHandlers;
    }
    
    public boolean hasParametricChild() {
      return parametricChild != null;
    }
    
    public boolean hasSegmentChild(String name) {
      return segmentChildren != null && segmentChildren.containsKey(name);
    }
    
    public RadixNode<T> parametricChild() {
      if (parametricChild == null) {
        parametricChild = new RadixNode<>();
      }
      
      return parametricChild;
    }
    
    public RadixNode<T> segmentChild(String name) {
      if (segmentChildren == null) {
        segmentChildren = new HashMap<>();
      }
      
      if (!segmentChildren.containsKey(name)) {
        segmentChildren.put(name, new RadixNode<>());
      }
      
      return segmentChildren.get(name);
    }
    
    public void addHandler(FilterType type, T handler, FilterPriority priority, List<String> segments) {
      if (handlers == null) {
        handlers = new FilterMatch<>();
      }
      
      handlers.addFilter(type, handler, priority, segments);
    }
    
    public void addWildcardHandler(FilterType type, T handler, FilterPriority priority, List<String> segments) {
      if (wildcardHandlers == null) {
        wildcardHandlers = new FilterMatch<>();
      }
      
      wildcardHandlers.addFilter(type, handler, priority, segments);
    }
    
    @Override
    public String toString() {
      return String.format("RadixNode[handlers=%s, wildcardHandlers=%s, segmentChildren=%s, parametricChild=%s]", 
          handlers, wildcardHandlers, segmentChildren, parametricChild);
    }
  }
  
  public static enum FilterType {
    BEFORE,
    AFTER;
  }
  
  private Map<HTTPMethod, RadixNode<T>> mapping;
  
  public FilterMapper() {
    mapping = new EnumMap<>(HTTPMethod.class);
  }
  
  /**
   * Removes all added filters.
   */
  public synchronized void clear() {
    mapping.clear();
  }
  
  public synchronized void addFilterBefore(String path, HTTPMethod[] methods, FilterPriority priority, T handler) throws PathFormatException {
    addFilter(FilterType.BEFORE, path, methods, priority, handler);
  }  

  public synchronized void addFilterAfter(String path, HTTPMethod[] methods, FilterPriority priority, T handler) throws PathFormatException {
    addFilter(FilterType.AFTER, path, methods, priority, handler);
  }
  
  public synchronized void addFilter(FilterType type, String path, HTTPMethod[] methods, FilterPriority priority, T handler) throws PathFormatException {
    List<String> segments = DefaultPathToPathSegments.parse(path);
    
    for (HTTPMethod method : methods) {
      if (!mapping.containsKey(method)) {
        mapping.put(method, new RadixNode<>());
      }
      
      RadixNode<T> current = mapping.get(method);
      boolean isWildcard = false;
      
      for (String segment : segments) {
        if (segment.startsWith(":")) {
          current = current.parametricChild();
        } else if (segment.equals("*")) {
          isWildcard = true;
          break;
        } else {
          current = current.segmentChild(segment);
        }
      }
      
      if (isWildcard) {
        current.addWildcardHandler(type, handler, priority, segments);
      } else {
        current.addHandler(type, handler, priority, segments);
      }
    }
  }
  
  /**
   * Looks up the filters for a given request path and method.
   * Lookup time is O(M*N) for M path segments and N matching filters.
   * @param path A request path.
   * @param method A request method.
   * @return A tuple in which the first item contains before filters (in the order they should execute)
   *         and the second item contains after filters (in the order they should execute).
   */
  public FilterMatch<T> lookup(String path, HTTPMethod method) throws PathFormatException {
    if (!mapping.containsKey(method)) {
      return new FilterMatch<>();
    }
    
    FilterMatch<T> matches = new FilterMatch<>();
    List<String> segments = DefaultPathToPathSegments.parse(path);
    RadixNode<T> root = mapping.get(method);
    lookup(root, segments, segments.size(), matches);
    return matches;
  }
  
  private void lookup(RadixNode<T> node, List<String> segments, int remaining, FilterMatch<T> matches) {
    if (remaining == 0) {
      matches.addAll(node.handlers());
      return;
    }
    
    String segment = segments.get(segments.size() - remaining);
    
    if (node.hasWildcardHandlers() && remaining > 0) {
      matches.addAll(node.wildcardHandlers());
    }
        
    if (node.hasSegmentChild(segment)) {
      lookup(node.segmentChild(segment), segments, remaining - 1, matches);
    }
    
    if (node.hasParametricChild()) {
      lookup(node.parametricChild(), segments, remaining - 1, matches);
    }
  }
}
