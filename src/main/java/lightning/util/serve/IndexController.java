package lightning.util.serve;

import static lightning.server.Context.badRequestIf;
import static lightning.server.Context.config;
import static lightning.server.Context.notFoundIf;
import static lightning.server.Context.request;
import static lightning.server.Context.response;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import lightning.ann.Controller;
import lightning.ann.Route;
import lightning.enums.HTTPMethod;
import lightning.templates.FreeMarkerTemplateEngine;
import lightning.templates.TemplateEngine;

@Controller
public final class IndexController {
  @Route(path="*", methods={HTTPMethod.GET})
  public void showDirectoryIndex() throws Exception {
    Path path = Paths.get(config().server.staticFilesPath, request().path()).toAbsolutePath();
    File file = new File(path.toString());

    badRequestIf(!file.getAbsolutePath().startsWith(config().server.staticFilesPath));
    notFoundIf(!file.exists() || !file.isDirectory() || !file.canRead());

    List<Map<String, String>> files = new ArrayList<>();
    String basePath = request().path().endsWith("/")
        ? request().path().substring(0, request().path().length() - 1)
        : request().path();

    for (File f : file.listFiles()) {
      files.add(ImmutableMap.of(
          "name", f.getName() + (f.isDirectory() ? "/" : ""),
          "link", basePath + "/" + f.getName(),
          "type", f.isDirectory() ? "dir" : "file"
      ));
    }

    Map<String, Object> model = ImmutableMap.of("files", files);
    TemplateEngine te = new FreeMarkerTemplateEngine(getClass(), "/lightning");
    te.render("directory-index.ftl", model, response().writer());
  }
}