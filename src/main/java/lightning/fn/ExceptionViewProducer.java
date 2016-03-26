package lightning.fn;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lightning.mvc.ModelAndView;

@FunctionalInterface
public interface ExceptionViewProducer {
  public ModelAndView produce(Class<? extends Throwable> clazz, Throwable e, HttpServletRequest request, HttpServletResponse response);
}
