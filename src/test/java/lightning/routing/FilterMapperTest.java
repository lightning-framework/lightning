package lightning.routing;

import static lightning.enums.FilterPriority.HIGH;
import static lightning.enums.FilterPriority.LOW;
import static lightning.enums.FilterPriority.LOWEST;
import static lightning.enums.FilterPriority.NORMAL;
import static org.junit.Assert.assertEquals;
import lightning.enums.HTTPMethod;
import lightning.routing.FilterMapper.FilterMatch;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class FilterMapperTest {
  public static HTTPMethod[] GET = new HTTPMethod[]{HTTPMethod.GET};
  public static HTTPMethod[] POST = new HTTPMethod[]{HTTPMethod.POST};
  public static HTTPMethod[] GET_POST = new HTTPMethod[]{HTTPMethod.GET, HTTPMethod.POST};
    
  @Test
  public void testRootWildcard() throws Exception {
    FilterMapper<String> mapper = new FilterMapper<>();
    mapper.addFilterBefore("*", GET, NORMAL, "handler");
    
    FilterMatch<String> match;
    match = mapper.lookup("/", HTTPMethod.GET);
    assertEquals(1, match.beforeFilters().size());
    
    match = mapper.lookup("/lol", HTTPMethod.GET);
    assertEquals(1, match.beforeFilters().size());
  }
  
  @Test
  public void testRealistic() throws Exception {
    FilterMapper<String> mapper = new FilterMapper<>();
    mapper.addFilterBefore("/account/*", GET, HIGH, "auth");
    mapper.addFilterBefore("/*", GET, LOW, "cors");
    mapper.addFilterBefore("/account/password", GET, NORMAL, "recentauth");
    
    FilterMatch<String> match;
    
    match = mapper.lookup("/account/password", HTTPMethod.GET);
    assertEquals(3, match.beforeFilters().size());
    
    match = mapper.lookup("/account/email", HTTPMethod.GET);
    assertEquals(2, match.beforeFilters().size());
    
    match = mapper.lookup("/account", HTTPMethod.GET);
    assertEquals(1, match.beforeFilters().size());
    
    match = mapper.lookup("/profile", HTTPMethod.GET);
    assertEquals(1, match.beforeFilters().size());
  }
  
  @Test
  public void testComplex() throws Exception {
    FilterMapper<String> mapper = new FilterMapper<>();
    mapper.addFilterAfter("/a/b", GET, NORMAL, "handler1");
    mapper.addFilterAfter("/:a/:b", GET, HIGH, "handler2");
    mapper.addFilterAfter("/:c/:d", GET, NORMAL, "handler3");
    mapper.addFilterAfter("/*", GET, LOW, "handler4");
    mapper.addFilterAfter("/a/*", GET, LOW, "handler5");
    mapper.addFilterAfter("/:a/*", GET, LOWEST, "handler6");
    
    FilterMatch<String> match = mapper.lookup("/a/b", HTTPMethod.GET);
    assertEquals(6, match.afterFilters().size());
  }
  
  @Test
  public void testRootMatching() throws Exception {
    FilterMapper<String> mapper = new FilterMapper<>();
    mapper.addFilterAfter("/", GET, NORMAL, "handler1");
    mapper.addFilterAfter("/*", GET, NORMAL, "handler2");
    mapper.addFilterAfter("/:a", GET, NORMAL, "handler3");
    
    FilterMatch<String> match = mapper.lookup("/", HTTPMethod.GET);
    assertEquals(1, match.afterFilters().size());
    assertEquals("handler1", match.afterFilters().get(0).handler);
  }
  
  @Test
  public void testDuplicates() throws Exception {
    Object HANDLER = "ABCDEFG";
    
    FilterMapper<Object> mapper = new FilterMapper<>();
    mapper.addFilterAfter("/a", GET, NORMAL, HANDLER);
    mapper.addFilterAfter("/:a", GET, NORMAL, HANDLER);
    mapper.addFilterAfter("/*", GET, NORMAL, HANDLER);
    
    FilterMatch<Object> match = mapper.lookup("/a", HTTPMethod.GET);
    System.out.println(match.afterFilters());
    assertEquals(3, match.afterFilters().size()); // Duplicates are allowed.
  }
  
  @Test
  public void testMethods() throws Exception {
    FilterMapper<String> mapper = new FilterMapper<>();
    mapper.addFilterAfter("/a", GET, NORMAL, "handler1");
    mapper.addFilterAfter("/a", POST, NORMAL, "handler2");
    
    FilterMatch<String> match = mapper.lookup("/a", HTTPMethod.GET);
    assertEquals(1, match.afterFilters().size());
    
    match = mapper.lookup("/a", HTTPMethod.POST);
    assertEquals(1, match.afterFilters().size());
  }
  
  @Test
  public void testMethods2() throws Exception {
    FilterMapper<String> mapper = new FilterMapper<>();
    mapper.addFilterAfter("/a", GET_POST, NORMAL, "handler1");
    
    FilterMatch<String> match = mapper.lookup("/a", HTTPMethod.GET);
    assertEquals(1, match.afterFilters().size());
    
    match = mapper.lookup("/a", HTTPMethod.POST);
    assertEquals(1, match.afterFilters().size());
  }
  
  @Test
  public void testPriorities() throws Exception {
    FilterMapper<String> mapper = new FilterMapper<>();
    mapper.addFilterAfter("/a", GET, NORMAL, "handler1");
    mapper.addFilterAfter("/a", GET, HIGH, "handler2");
    mapper.addFilterAfter("/a", GET, LOW, "handler3");
    mapper.addFilterAfter("/a", GET, NORMAL, "handler4");
    
    FilterMatch<String> match = mapper.lookup("/a", HTTPMethod.GET);
    assertEquals(4, match.afterFilters().size());
    assertEquals("handler2", match.afterFilters().get(0).handler);
    assertEquals("handler3", match.afterFilters().get(3).handler);
  }
  
  @Test
  public void testBeforeAfter() throws Exception {
    FilterMapper<String> mapper = new FilterMapper<>();
    mapper.addFilterAfter("/a", GET, NORMAL, "handler1");
    mapper.addFilterBefore("/a", GET, NORMAL, "handler2");
    
    FilterMatch<String> match = mapper.lookup("/a", HTTPMethod.GET);
    assertEquals(1, match.beforeFilters().size());
    assertEquals(1, match.afterFilters().size());
    assertEquals("handler1", match.afterFilters().get(0).handler);
    assertEquals("handler2", match.beforeFilters().get(0).handler);
  }
  
  @Test
  public void testParamParsing1() throws Exception {
    FilterMapper<String> mapper = new FilterMapper<>();
    mapper.addFilterBefore("/:a", GET, NORMAL, "handler1");
    
    FilterMatch<String> match = mapper.lookup("/test", HTTPMethod.GET);
    assertEquals(1, match.beforeFilters().size());
    assertEquals("handler1", match.beforeFilters().get(0).handler);
    assertEquals(ImmutableMap.of("a", "test"), match.beforeFilters().get(0).params("/test"));
  }
  
  @Test
  public void testParamParsing2() throws Exception {
    FilterMapper<String> mapper = new FilterMapper<>();
    mapper.addFilterBefore("/:a/:b", GET, NORMAL, "handler1");
    
    FilterMatch<String> match = mapper.lookup("/test/test2", HTTPMethod.GET);
    assertEquals(1, match.beforeFilters().size());
    assertEquals("handler1", match.beforeFilters().get(0).handler);
    assertEquals(ImmutableMap.of("a", "test", "b", "test2"), match.beforeFilters().get(0).params("/test/test2"));
  }
  
  @Test
  public void testParamParsing3() throws Exception {
    FilterMapper<String> mapper = new FilterMapper<>();
    mapper.addFilterBefore("/:a/*", GET, NORMAL, "handler1");
    
    FilterMatch<String> match = mapper.lookup("/test/test2", HTTPMethod.GET);
    assertEquals(1, match.beforeFilters().size());
    assertEquals("handler1", match.beforeFilters().get(0).handler);
    assertEquals(ImmutableMap.of("a", "test"), match.beforeFilters().get(0).params("/test/test2"));
    assertEquals(ImmutableList.of("test2"), match.beforeFilters().get(0).wildcards("/test/test2"));
  }
  
  @Test
  public void testWildcardParsing1() throws Exception {
    FilterMapper<String> mapper = new FilterMapper<>();
    mapper.addFilterBefore("/*", GET, NORMAL, "handler1");
    
    FilterMatch<String> match = mapper.lookup("/test", HTTPMethod.GET);
    assertEquals(1, match.beforeFilters().size());
    assertEquals("handler1", match.beforeFilters().get(0).handler);
    assertEquals(ImmutableList.of("test"), match.beforeFilters().get(0).wildcards("/test"));
  }
  
  @Test
  public void testWildcardParsing2() throws Exception {
    FilterMapper<String> mapper = new FilterMapper<>();
    mapper.addFilterBefore("/*", GET, NORMAL, "handler1");
    
    FilterMatch<String> match = mapper.lookup("/test/test2", HTTPMethod.GET);
    assertEquals(1, match.beforeFilters().size());
    assertEquals("handler1", match.beforeFilters().get(0).handler);
    assertEquals(ImmutableList.of("test", "test2"), match.beforeFilters().get(0).wildcards("/test/test2"));
  }
  
  @Test
  public void testWildcardParsing3() throws Exception {
    FilterMapper<String> mapper = new FilterMapper<>();
    mapper.addFilterBefore("/test/*", GET, NORMAL, "handler1");
    
    FilterMatch<String> match = mapper.lookup("/test/test2", HTTPMethod.GET);
    assertEquals(1, match.beforeFilters().size());
    assertEquals("handler1", match.beforeFilters().get(0).handler);
    assertEquals(ImmutableList.of("test2"), match.beforeFilters().get(0).wildcards("/test/test2"));
  }
  
  @Test
  public void testWildcardParsing4() throws Exception {
    FilterMapper<String> mapper = new FilterMapper<>();
    mapper.addFilterBefore("/:param/*", GET, NORMAL, "handler1");
    
    FilterMatch<String> match = mapper.lookup("/test/test2/test3", HTTPMethod.GET);
    assertEquals(1, match.beforeFilters().size());
    assertEquals("handler1", match.beforeFilters().get(0).handler);
    assertEquals(ImmutableList.of("test2", "test3"), match.beforeFilters().get(0).wildcards("/test/test2/test3"));
  }
}
