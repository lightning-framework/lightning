package lightning.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class MimeTypeTest {
  @Test
  public void testBadMimeType1() {
    try {
      MimeType.parse("abc");
      fail("Expected exception.");
    } catch (Exception e) {}
  }
  
  @Test
  public void testBadMimeType2() {
    try {
      MimeType.parse("abc/");
      fail("Expected exception.");
    } catch (Exception e) {}
  }
  
  @Test
  public void testBadMimeType3() {
    try {
      MimeType.parse("abc/def;");
      fail("Expected exception.");
    } catch (Exception e) {}
  }
  
  @Test
  public void testBadMimeType4() {
    try {
      MimeType.parse("abc/def+");
      fail("Expected exception.");
    } catch (Exception e) {}
  }
  
  @Test
  public void testBadMimeType5() {
    try {
      MimeType.parse("abc/def; ab=\"cd");
      fail("Expected exception.");
    } catch (Exception e) {}
  }
  
  @Test
  public void testMime1() throws Exception {
    MimeType.parse("abc/def");
  }
  
  @Test
  public void testMime2() throws Exception {
    MimeType.parse("abc/def.ghi");
  }
  
  @Test
  public void testMime3() throws Exception {
    MimeType.parse("abc/def.ghi.jkl");
  }
  
  @Test
  public void testMime4() throws Exception {
    MimeType.parse("abc/def+suffix");
  }
  
  @Test
  public void testMime5() throws Exception {
    MimeType.parse("abc/def.ghi+suffix");
  }
  
  @Test
  public void testMime6() throws Exception {
    MimeType.parse("abc/def.ghi.jkl+suffix");
  }
  
  @Test
  public void testMime7() throws Exception {
    MimeType.parse("abc/def; param1");
  }
  
  @Test
  public void testMime8() throws Exception {
    MimeType.parse("abc/def; param1; param2");
  }
  
  @Test
  public void testMime9() throws Exception {
    MimeType.parse("abc/def; param1=Bob; param2");
  }
  
  @Test
  public void testMime10() throws Exception {
    MimeType.parse("abc/def; param1=Bob; param2=Joe");
  }
  
  @Test
  public void testMime11() throws Exception {
    MimeType.parse("abc/def; param1; param2=Joe");
  }
  
  @Test
  public void testMime12() throws Exception {
    MimeType.parse("abc/def; param1=\"Bob\"; param2");
  }
  
  @Test
  public void testMime13() throws Exception {
    MimeType.parse("abc/def; param1=\"Bob\"; param2=\"Joe\"");
  }
  
  @Test
  public void testMime14() throws Exception {
    MimeType.parse("abc/def; param1=Bob; param2=\"Joe\"");
  }
  
  @Test
  public void testMime15() throws Exception {
    MimeType.parse("abc/def; param1=\"Bob\"; param2=Joe");
  }
  
  @Test
  public void testMime16() throws Exception {
    MimeType.parse("abc/def; param1; param2=\"Joe\"");
  }
  
  @Test
  public void testMime17() throws Exception {
    MimeType.parse("abc/def+suffix; param1; param2");
  }
  
  @Test
  public void testMime18() throws Exception {
    MimeType.parse("abc/def+suffix; param1=Bob; param2");
  }
  
  @Test
  public void testMime19() throws Exception {
    MimeType.parse("abc/def+suffix; param1=Bob; param2=Joe");
  }
  
  @Test
  public void testMime20() throws Exception {
    MimeType.parse("abc/def+suffix; param1; param2=Joe");
  }
  
  @Test
  public void testMime21() throws Exception {
    MimeType.parse("abc/def+suffix; param1=\"Bob\"; param2");
  }
  
  @Test
  public void testMime22() throws Exception {
    MimeType.parse("abc/def+suffix; param1=\"Bob\"; param2=\"Joe\"");
  }
  
  @Test
  public void testMime23() throws Exception {
    MimeType.parse("abc/def+suffix; param1=Bob; param2=\"Joe\"");
  }
  
  @Test
  public void testMime24() throws Exception {
    MimeType.parse("abc/def+suffix; param1=\"Bob\"; param2=Joe");
  }
  
  @Test
  public void testMime25() throws Exception {
    MimeType.parse("abc/def+suffix; param1; param2=\"Joe\"");
  }
  
  @Test
  public void testMime26() throws Exception {
    MimeType.parse("abc/def.ghi+suffix; param1; param2");
  }
  
  @Test
  public void testMime27() throws Exception {
    MimeType.parse("abc/def.ghi+suffix; param1=Bob; param2");
  }
  
  @Test
  public void testMime28() throws Exception {
    MimeType.parse("abc/def.ghi+suffix; param1=Bob; param2=Joe");
  }
  
  @Test
  public void testMime29() throws Exception {
    MimeType.parse("abc/def.ghi+suffix; param1; param2=Joe");
  }
  
  @Test
  public void testMime30() throws Exception {
    MimeType.parse("abc/def.ghi+suffix; param1=\"Bob\"; param2");
  }
  
  @Test
  public void testMime31() throws Exception {
    MimeType.parse("abc/def.ghi+suffix; param1=\"Bob\"; param2=\"Joe\"");
  }
  
  @Test
  public void testMime32() throws Exception {
    MimeType mime = MimeType.parse("abc/def.ghi+suffix; param1=Bob; param2=\"Joe\"; param3");
    assertEquals("abc", mime.getTopLevelTypeName());
    assertEquals("ghi", mime.getSubTypeName());
    assertEquals("def.ghi", mime.getCompleteSubTypeName());
    assertEquals("suffix", mime.getSuffix());
    assertEquals("Bob", mime.getParameters().get("param1"));
    assertEquals("Joe", mime.getParameters().get("param2"));
    assertTrue(mime.getParameters().containsKey("param3"));
  }
  
  @Test
  public void testMime33() throws Exception {
    MimeType.parse("abc/def.ghi+suffix; param1=\"Bob\"; param2=Joe");
  }
  
  @Test
  public void testMime34() throws Exception {
    MimeType.parse("abc/def.ghi+suffix; param1; param2=\"Joe\"");
  }
  
  @Test
  public void testMime35() throws Exception {
    MimeType.parse("abc/def.ghi; param1; param2");
  }
  
  @Test
  public void testMime36() throws Exception {
    MimeType.parse("abc/def.ghi; param1=Bob; param2");
  }
  
  @Test
  public void testMime37() throws Exception {
    MimeType.parse("abc/def.ghi; param1=Bob; param2=Joe");
  }
  
  @Test
  public void testMime38() throws Exception {
    MimeType.parse("abc/def.ghi; param1; param2=Joe");
  }
  
  @Test
  public void testMime39() throws Exception {
    MimeType.parse("abc/def.ghi; param1=\"Bob\"; param2");
  }
  
  @Test
  public void testMime40() throws Exception {
    MimeType.parse("abc/def.ghi; param1=\"Bob\"; param2=\"Joe\"");
  }
  
  @Test
  public void testMime41() throws Exception {
    MimeType.parse("abc/def.ghi; param1=Bob; param2=\"Joe\"");
  }
  
  @Test
  public void testMime42() throws Exception {
    MimeType.parse("abc/def.ghi; param1=\"Bob\"; param2=Joe");
  }
  
  @Test
  public void testMime43() throws Exception {
    MimeType.parse("abc/def.ghi; param1; param2=\"Joe\"");
  }
}
