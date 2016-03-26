package lightning.fn;

import lightning.http.Request;
import lightning.http.Response;

@FunctionalInterface
public interface Transformer {
  public void transform(Request request, Response response, Object item) throws Exception;
}
