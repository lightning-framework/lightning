package lightning.enums;

import java.util.Map;

import lightning.http.AccessViolationException;
import lightning.http.BadRequestException;
import lightning.http.InternalServerErrorException;
import lightning.http.MethodNotAllowedException;
import lightning.http.NotAuthorizedException;
import lightning.http.NotFoundException;
import lightning.http.NotImplementedException;

import com.google.common.collect.ImmutableMap;

public enum HTTPStatus {
  BAD_REQUEST(400),
  UNAUTHORIZED(401),
  FORBIDDEN(403),
  METHOD_NOT_ALLOWED(405),
  NOT_FOUND(404),
  INTERNAL_SERVER_ERROR(500),
  NOT_IMPLEMENTED(501),
  OK(200),
  NOT_MODIFIED(304),
  FOUND(302);
  
  private final int code;
  private static final Map<Class<? extends Throwable>, HTTPStatus> translator = ImmutableMap.<Class<? extends Throwable>, HTTPStatus>builder()
      .put(AccessViolationException.class, HTTPStatus.FORBIDDEN)
      .put(BadRequestException.class, HTTPStatus.BAD_REQUEST)
      .put(InternalServerErrorException.class, HTTPStatus.INTERNAL_SERVER_ERROR)
      .put(MethodNotAllowedException.class, HTTPStatus.METHOD_NOT_ALLOWED)
      .put(NotAuthorizedException.class, HTTPStatus.UNAUTHORIZED)
      .put(NotFoundException.class, HTTPStatus.NOT_FOUND)
      .put(NotImplementedException.class, HTTPStatus.NOT_IMPLEMENTED)
      .build(); 
  
  private HTTPStatus(int code) {
    this.code = code;
  }
  
  public int getCode() {
    return code;
  }

  public static HTTPStatus fromException(Throwable e) {
    return fromExceptionClass(e.getClass());
  }
  
  public static HTTPStatus fromExceptionClass(Class<? extends Throwable> clazz) {
    if (translator.containsKey(clazz)) {
      return translator.get(clazz);
    }
    
    return HTTPStatus.INTERNAL_SERVER_ERROR;
  }
}
