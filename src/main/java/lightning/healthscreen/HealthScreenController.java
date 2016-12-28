package lightning.healthscreen;

import java.util.Map;

import lightning.ann.Controller;
import lightning.config.Config;
import lightning.enums.HTTPMethod;
import lightning.mvc.HandlerContext;
import lightning.routing.RouteMapper;
import lightning.util.ReflectionUtil;

import com.google.common.collect.ImmutableMap;

@Controller
public class HealthScreenController {
  public static void map(RouteMapper<Object> mapper, Config config) throws Exception {
    if (config.healthPath != null) {
      mapper.map(HTTPMethod.GET,
                 config.healthPath,
                 ReflectionUtil.getMethod(HealthScreenController.class, "handleRequest"));
    }
  }

  public Map<String, Object> buildStatusModel() {
    return new ImmutableMap.Builder<String, Object>()
        .put("status", "ok")
        .build();
  }

  public void handleRequest(HandlerContext ctx) throws Exception {
    ctx.sendJson(buildStatusModel());
  }
}
