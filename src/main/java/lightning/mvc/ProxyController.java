package lightning.mvc;

import lightning.config.Config;
import lightning.db.MySQLDatabaseProvider;
import spark.Request;
import spark.Response;
import spark.template.freemarker.FreeMarkerEngine;

public class ProxyController extends lightning.mvc.old.Controller {
  public ProxyController(Request rq, Response re, MySQLDatabaseProvider dbp, Config c,
      FreeMarkerEngine te) {
    super(rq, re, dbp, c, te);
  }

  @Override
  public Object handleRequest() throws Exception {
    // NOT USED!
    return null;
  }
}
