package lightning.templates;

import java.io.StringWriter;

public final class TemplateUtils {
  public static final String renderToString(TemplateEngine templateEngine, String viewName, Object viewModel) throws Exception {
    StringWriter stringWriter = new StringWriter();
    templateEngine.render(viewName, viewModel, stringWriter);
    return stringWriter.toString();
  }
}
