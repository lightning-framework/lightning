package lightning.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import lightning.util.MimeType.MimeParseException;

public class MimeMatcherTest {
  @Test
  public void testExceptionMapper() throws MimeParseException {
    MimeMatcher matcher = new MimeMatcher(ImmutableList.of("a/b", "c/*", "*/d"));
    assertTrue(matcher.matches("a/b"));
    assertTrue(matcher.matches("c/d"));
    assertTrue(matcher.matches("c/f"));
    assertTrue(matcher.matches("f/d"));
    assertFalse(matcher.matches("g/a"));
  }
}
