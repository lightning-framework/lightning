package lightning.routing;

import static lightning.enums.HTTPMethod.GET;
import static lightning.enums.HTTPMethod.POST;
import static org.junit.Assert.*;
import lightning.enums.HTTPMethod;
import lightning.http.Request;
import lightning.routing.RouteMapper.Match;
import lightning.routing.RouteMapper.RouteFormatException;

import org.junit.Test;
import org.mockito.Mockito;

import com.augustl.pathtravelagent.PathFormatException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class RouteMapperTest {
  
  @Test
  public void testRouteMatching() throws RouteFormatException, PathFormatException {
    RouteMapper<String> router = new RouteMapper<>();
    router.map(GET, "/", "ACTION1");
    router.map(GET, "/u/matt", "ACTION2");
    router.map(GET, "/u/:username", "ACTION3");
    router.map(GET, "/u/*", "ACTION4"); 
    router.map(GET, "/u", "ACTION5");
    router.map(POST, "/u", "ACTION6");
    router.map(GET, "/a/:b/:c", "ACTION7");
    router.compile();
    
    Match<String> match;
    
    match = router.lookup(mockRequest(GET, "/"));
    assertEquals("ACTION1", match.getData());
    assertEquals(ImmutableList.of(), match.getWildcards());
    assertEquals(ImmutableMap.of(), match.getParams());
    
    match = router.lookup(mockRequest(GET, "/u/matt"));
    assertEquals("ACTION2", match.getData());
    assertEquals(ImmutableList.of(), match.getWildcards());
    assertEquals(ImmutableMap.of(), match.getParams());
    
    match = router.lookup(mockRequest(GET, "/u/bob"));
    assertEquals("ACTION3", match.getData());
    assertEquals(ImmutableList.of(), match.getWildcards());
    assertEquals(ImmutableMap.of("username", "bob"), match.getParams());
    
    match = router.lookup(mockRequest(GET, "/u/bob/asdf"));
    assertEquals("ACTION4", match.getData());
    assertEquals(ImmutableList.of("bob", "asdf"), match.getWildcards());
    assertEquals(ImmutableMap.of(), match.getParams());
    
    match = router.lookup(mockRequest(GET, "/u"));
    assertEquals("ACTION5", match.getData());
    assertEquals(ImmutableList.of(), match.getWildcards());
    assertEquals(ImmutableMap.of(), match.getParams());
    
    match = router.lookup(mockRequest(GET, "/u/")); // Trailing slash shouldn't matter.
    assertEquals("ACTION5", match.getData());
    assertEquals(ImmutableList.of(), match.getWildcards());
    assertEquals(ImmutableMap.of(), match.getParams());
    
    match = router.lookup(mockRequest(POST, "/u"));
    assertEquals("ACTION6", match.getData());
    assertEquals(ImmutableList.of(), match.getWildcards());
    assertEquals(ImmutableMap.of(), match.getParams());

    // Make sure double slashes are just straight disallowed.
    assertNull(router.lookup(mockRequest(GET, "/u//bob")));
    assertNull(router.lookup(mockRequest(GET, "/u//")));
    
    match = router.lookup(mockRequest(GET, "/a/first/second/"));
    assertEquals("ACTION7", match.getData());
    assertEquals(ImmutableList.of(), match.getWildcards());
    assertEquals(ImmutableMap.of("b", "first", "c", "second"), match.getParams());
    
    assertNull(router.lookup(mockRequest(POST, "/asdf")));
    
    router.clear();
  }
  
  @Test
  public void testConflictingRoutes() throws PathFormatException {
    try {
      RouteMapper<String> router = new RouteMapper<>();
      router.map(GET, "/a/:b", "AB");
      router.map(GET, "/a/:c", "AC");
      router.compile();
      fail("Expected RouteFormatException");
    } catch (RouteFormatException e) {}
    
    try {
      RouteMapper<String> router = new RouteMapper<>();
      router.map(GET, "/a/*", "A");
      router.map(GET, "/a/*", "B");
      router.compile();
      fail("Expected RouteFormatException");
    } catch (RouteFormatException e) {}
    
    try {
      RouteMapper<String> router = new RouteMapper<>();
      router.map(GET, "/a", "A");
      router.map(GET, "/a", "B");
      router.compile();
      fail("Expected RouteFormatException");
    } catch (RouteFormatException e) {}
  }
  
  @Test
  public void testMalformattedRoutes() throws PathFormatException {
    try {
      RouteMapper<String> router = new RouteMapper<>();
      router.map(GET, "/$abc", "ABC");
      router.compile();
      fail("Expected RouteFormatException");
    } catch (RouteFormatException e) {}
    
    try {
      RouteMapper<String> router = new RouteMapper<>();
      router.map(GET, "/a/*/b", "ABC");
      router.compile();
      fail("Expected RouteFormatException");
    } catch (RouteFormatException e) {}
  }
  
  public Request mockRequest(HTTPMethod method, String path) {
    Request request = Mockito.mock(Request.class);
    Mockito.when(request.method()).thenReturn(method);
    Mockito.when(request.path()).thenReturn(path);
    return request;
  }
}
