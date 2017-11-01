package lightning.debugscreen;

import com.google.common.base.Optional;

/**
 * Defines an interface for locating source files.
 */
public interface SourceLocator {
  /**
   * @param frame A stack trace frame.
   * @return A file containing the code referenced by the StackTraceElement.
   */
  public Optional<SourceFile> findFileForFrame(StackTraceElement frame);
}
