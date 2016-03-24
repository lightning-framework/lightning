package lightning.mvc;

import spark.Request;
import spark.Response;

@FunctionalInterface
public interface RequestHandler {
  public Object execute(Request req, Response res) throws Exception;
}
