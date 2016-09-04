package lightning.debugscreen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public class LocalSourceFile implements SourceFile {
  private final File file;
  
  public LocalSourceFile(File file) {
    this.file = file;
  }

  @Override
  public String getPath() {
    return file.getPath();
  }

  @Override
  public Optional<Map<Integer, String>> getLines(StackTraceElement frame) {
    // If no line number is given, we can't fetch lines.
    if (frame.getLineNumber() == -1) {
        return Optional.absent();
    }

    // Otherwise, fetch 20 lines centered on the number provided in the trace.
    ImmutableMap.Builder<Integer, String> lines = ImmutableMap.builder();
    int start = Math.max(frame.getLineNumber() - 10, 0);
    int end = start + 20;
    int current = 0;

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
        String line;
        while ((line = br.readLine()) != null) {
            if (current < start) {
                current += 1;
                continue;
            }

            if (current > end) {
                break;
            }

            lines.put(current, line);
            current += 1;
        }
    } catch (Exception e) {
        // If we get an IOException, not much we can do... just ignore it and move on.
        return Optional.absent();
    }

    return Optional.of(lines.build());
  }
}
