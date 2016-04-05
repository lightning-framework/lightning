package lightning.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableMap;

import lightning.fn.ExceptionViewProducer;
import lightning.http.AccessViolationException;
import lightning.http.BadRequestException;
import lightning.http.InternalServerErrorException;
import lightning.http.MethodNotAllowedException;
import lightning.http.NotAuthorizedException;
import lightning.http.NotFoundException;
import lightning.http.NotImplementedException;

public class DefaultExceptionViewProducer implements ExceptionViewProducer {
  private static final String VIEW_NAME = "error.ftl";
  private static final ImmutableMap<Class<? extends Throwable>, String> status = ImmutableMap.<Class<? extends Throwable>, String>builder()
      .put(AccessViolationException.class, "403")
      .put(BadRequestException.class, "400")
      .put(InternalServerErrorException.class, "500")
      .put(MethodNotAllowedException.class, "405")
      .put(NotAuthorizedException.class, "401")
      .put(NotFoundException.class, "404")
      .put(NotImplementedException.class, "501")
      .build();
  private static final ImmutableMap<Class<? extends Throwable>, String> statusText = ImmutableMap.<Class<? extends Throwable>, String>builder()
      .put(AccessViolationException.class, "Forbidden")
      .put(BadRequestException.class, "Bad Request")
      .put(InternalServerErrorException.class, "Internal Server Error")
      .put(MethodNotAllowedException.class, "Method Not Allowed")
      .put(NotAuthorizedException.class, "Unauthorized")
      .put(NotFoundException.class, "Not Found")
      .put(NotImplementedException.class, "Not Implemented")
      .build();
  private static final ImmutableMap<Class<? extends Throwable>, String> explanationText = ImmutableMap.<Class<? extends Throwable>, String>builder()
      .put(AccessViolationException.class, "You do not have permission to view this page.")
      .put(BadRequestException.class, "Your request could not be serviced because it did not contain the required or "
          + "expected information. You should hit the back button on your browser and try again.")
      .put(InternalServerErrorException.class, "The feature you are trying to access is currently under construction. "
          + "Please try again later.")
      .put(MethodNotAllowedException.class, "The method you have requested is not supported.")
      .put(NotAuthorizedException.class, "You must authenticate to proceed. If you were previously authenticated, "
              + "your session may have expired.")
      .put(NotFoundException.class, "We couldn't find the page you requested on our servers.")
      .put(NotImplementedException.class, "The feature you are trying to access is currently under construction.")
      .build();
  private static final ImmutableMap<Class<? extends Throwable>, Boolean> includeMessage = ImmutableMap.<Class<? extends Throwable>, Boolean>builder()
      .put(BadRequestException.class, true)
      .build();

  @Override
  public ModelAndView produce(Class<? extends Throwable> clazz, Throwable e, HttpServletRequest request, HttpServletResponse response) {
    if (!status.containsKey(clazz)) {
      return null;
    }
    
    ImmutableMap.Builder<String, Object> data = ImmutableMap.builder();
    data.put("status", status.get(clazz));
    data.put("status_text", statusText.get(clazz));
    data.put("explanation", explanationText.get(clazz));
    
    if (includeMessage.getOrDefault(clazz, false) && e.getMessage() != null) {
      data.put("message", e.getMessage());
    }
    
    return new ModelAndView(VIEW_NAME, data.build());
  }
}
