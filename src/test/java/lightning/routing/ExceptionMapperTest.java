package lightning.routing;

import static org.junit.Assert.*;
import lightning.http.BadRequestException;
import lightning.http.NotFoundException;

import org.junit.Test;

public class ExceptionMapperTest {

  @Test
  public void testExceptionMapper() {
    ExceptionMapper<String> mapper = new ExceptionMapper<String>();

    assertFalse(mapper.has(BadRequestException.class));
    assertFalse(mapper.has(Exception.class));

    mapper.map(NotFoundException.class, "NotFoundException");
    mapper.map(Exception.class, "Exception");
    mapper.map(Throwable.class, "Throwable");

    //assertTrue(mapper.has(BadRequestException.class));
    assertTrue(mapper.has(Error.class));
    assertTrue(mapper.has(NotFoundException.class));

    assertEquals("Throwable", mapper.get(Error.class));
    assertEquals("Exception", mapper.get(Exception.class));
    //assertEquals("Exception", mapper.getHandler(BadRequestException.class));
    assertEquals("NotFoundException", mapper.get(NotFoundException.class));

    mapper.clear();

    assertFalse(mapper.has(BadRequestException.class));
    assertFalse(mapper.has(Error.class));
    assertFalse(mapper.has(NotFoundException.class));
  }
}
