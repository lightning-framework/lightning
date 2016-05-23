package lightning.templates;

import java.io.Writer;

/**
 * An abstract interface for rendering templates.
 * 
 * Implementors must respect the semantics of debug mode. In particular:
 *   - render(...) should not perform ANY caching in debug mode
 *   - Templates should be loaded from the FILESYSTEM in debug mode (by searching for the
 *     templateFilesPath in ./src/main/java, ./src/main/resources, and ./.
 *   - Templates should be loaded from the CLASSPATH in production mode.
 *   
 * For reference implementation, see the provided FreeMarkerTemplateEngine.
 */
public interface TemplateEngine {
  /**
   * Renders the template with given name and view model to the given output stream.
   * @param templateName A template file name.
   * @param viewModel A view model.
   * @param outputStream A stream to write output to.
   * @throws Exception On failure.
   */
  public void render(String templateName, Object viewModel, Writer outputStream) throws Exception;
}
