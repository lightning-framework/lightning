package com.augustl.pathtravelagent;

import java.util.List;

/**
 * <p>
 * The default implementation of taking a RouteTreeNode and a request and returning a response.
 * </p>
 *
 * @param <T_REQ> A request object, implementing IRequest.
 * @param <T_RES> The return value for the handler. Can be any type you want, not used for anything
 *        by PathTravelAgent.
 */
public class DefaultRouteMatcher<T_REQ extends IRequest, T_RES> {
  private T_RES match(RouteTreeNode<T_REQ, T_RES> node, T_REQ req, RouteMatchResult result,
      List<String> segments, int i) {
    // If we've reached the end of a possible expansion, try to create a result.
    if (i >= segments.size()) {
      if (node == null || node.getHandler() == null) {
        return null; // This exploration led to nowhere.
      }

      return node.getHandler().call(new RouteMatch<T_REQ>(req, result));
    }

    // Try to match the current path segment to the current node.
    String pathSegment = segments.get(i);

    // Try first an actual match.
    if (node.containsPathSegmentChildNodes(pathSegment)) {
      T_RES match = match(node.getPathSegmentChildNode(pathSegment), req, result, segments, i + 1);

      if (match != null) {
        return match;
      }
    }

    // Try second a parametric match.
    if (node.hasParametricChild()) {
      if (result.addParametricSegment(node.getParametricChildSegment(), pathSegment)) {
        T_RES match = match(node.getParametricChildNode(), req, result, segments, i + 1);

        if (match != null) {
          return match;
        } else {
          result.removeParametricSegment(node.getParametricChildSegment());
        }
      }
    }

    // Try last a wildcard match.
    if (node.hasWildcardChild()) {
      for (int j = i; j < segments.size(); j++) {
        result.addToWildcardMatches(segments.get(j));
      }

      return match(node.getWildcardChildNode(), req, result, segments, segments.size());
    }

    return null;
  }

  public T_RES match(final RouteTreeNode<T_REQ, T_RES> rootNode, T_REQ req) {
    try {
      return match(rootNode, req, new RouteMatchResult(), req.getPathSegments(), 0);
    } catch (PathFormatException e) {
      return null;
    }
  }

  public T_RES matchOld(final RouteTreeNode<T_REQ, T_RES> rootNode, T_REQ req) throws PathFormatException {
    List<String> pathSegments = req.getPathSegments();
    RouteTreeNode<T_REQ, T_RES> targetNode = rootNode;
    RouteMatchResult routeMatchResult = new RouteMatchResult();

    for (int i = 0; i < pathSegments.size(); i++) {
      String pathSegment = pathSegments.get(i);

      if (targetNode.containsPathSegmentChildNodes(pathSegment)) {
        targetNode = targetNode.getPathSegmentChildNode(pathSegment);
        continue;
      }

      if (targetNode.hasParametricChild()) {
        if (!routeMatchResult.addParametricSegment(targetNode.getParametricChildSegment(),
            pathSegment)) {
          return null;
        }
        targetNode = targetNode.getParametricChildNode();
        continue;
      }

      if (targetNode.hasWildcardChild()) {
        for (; i < pathSegments.size(); i++) {
          pathSegment = pathSegments.get(i);
          routeMatchResult.addToWildcardMatches(pathSegment);
        }
        targetNode = targetNode.getWildcardChildNode();
        break;
      }

      return null;
    }

    if (targetNode == null) {
      return null;
    }

    if (targetNode.getHandler() == null) {
      return null;
    }

    return targetNode.getHandler().call(new RouteMatch<T_REQ>(req, routeMatchResult));
  }
}
