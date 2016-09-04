package lightning.debugscreen;

import java.util.Map;

import com.google.common.base.Optional;

public interface SourceFile {
  public String getPath();
  public Optional<Map<Integer, String>> getLines(StackTraceElement frame);
}
