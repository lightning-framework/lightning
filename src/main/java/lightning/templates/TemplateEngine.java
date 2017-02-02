package lightning.templates;

import java.io.Writer;

/**
 * An abstract interface for rendering templates.
 *
 * Implementors must respect the semantics of debug mode. In particular:
 *   - render(...) should not perform ANY caching in debug mode
 *   - Templates should be loaded from the FILESYSTEM in debug mode (by searching for the
 *     templateFilesPath in ./src/main/java, ./src/main/resources, and ./.
 *   - If the file system directory does not exist in debug mode, fall back to using the
 *     class path.
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

  /**
   * Throws an exception if a given template does not exist.
   * @param templateName A template file name.
   * @throws Exception iff the template with given name does not exist.
   */
  public void requireValid(String templateName) throws Exception;
}
