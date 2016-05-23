package lightning.templates;

import java.io.File;
import java.io.Writer;

import lightning.config.Config;
import lightning.util.Iterables;

import com.google.common.collect.ImmutableList;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

public final class FreeMarkerTemplateEngine implements TemplateEngine {
  private static final Version FREEMARKER_VERSION = new Version(2, 3, 20);
  private final Config config;
  private final Configuration configuration;
  
  public FreeMarkerTemplateEngine(Config config) throws Exception {
    this.config = config;
    this.configuration = new Configuration(FREEMARKER_VERSION);
    this.configuration.setSharedVariable("__LIGHTNING_DEV", config.enableDebugMode);
    this.configuration.setShowErrorTips(config.enableDebugMode);
    this.configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);  
    
    if (config.server.templateFilesPath != null) {
      File templatePath = Iterables.firstOr(Iterables.filter(ImmutableList.of(
          new File("./src/main/java/" + config.server.templateFilesPath),
          new File("./src/main/resources/" + config.server.templateFilesPath)
      ), f -> f.exists()), new File(config.server.templateFilesPath));
      
      if (templatePath.exists() && config.enableDebugMode) {
        this.configuration.setDirectoryForTemplateLoading(templatePath);
      } else {
        this.configuration.setClassForTemplateLoading(getClass(), "/" + config.server.templateFilesPath);
      }
    }
  }
  
  @Override
  public void render(String templateName, Object viewModel, Writer outputStream) throws Exception {
    if (config.enableDebugMode) {
      configuration.clearTemplateCache();
    }
    
    configuration.getTemplate(templateName).process(viewModel, outputStream);
  }
}
