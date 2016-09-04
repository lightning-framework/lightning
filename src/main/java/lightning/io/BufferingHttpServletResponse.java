package lightning.io;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import lightning.config.Config;
import lightning.util.MimeMatcher;

public class BufferingHttpServletResponse extends HttpServletResponseWrapper {  
  private final Config config;
  private final MimeMatcher bufferingMatcher;
  private LightningServletOutputStream stream;
  private LightningPrintWriter writer;
  
  public BufferingHttpServletResponse(HttpServletResponse response, Config config, 
      MimeMatcher bufferingMatcher) throws IOException {
    super(response);
    this.config = config;
    this.bufferingMatcher = bufferingMatcher;
    this.stream = new BufferingLightningServletOutputStream(super.getOutputStream(), config.server.outputBufferingLimitBytes);
    this.writer = new LightningPrintWriter(new UnflushableLightningServletOutputStream(this.stream));
  }
  
  public boolean isBuffered() {
    return stream.isBuffering();
  }
  
  private void updateBufferingState() {
    stream.setBuffering(
        (config.enableDebugMode) ||
        (config.server.enableOutputBuffering && bufferingMatcher.matches(getContentType()))
    );
  }
  
  public void setHeader(String name, String value) {
    super.setHeader(name, value);
    updateBufferingState();
  }
  
  @Override
  public void addHeader(String name, String value) {
    super.addHeader(name, value);
    updateBufferingState();
  }
  
  @Override
  public void setCharacterEncoding(String charset) {
    super.setCharacterEncoding(charset);
    updateBufferingState();
  }
  
  @Override
  public void setContentType(String type) {
    super.setContentType(type);
    updateBufferingState();
  }
  
  @Override
  public void setLocale(Locale locale) {
    super.setLocale(locale);
    updateBufferingState();
  }
  
  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    return stream;
  }
  
  @Override
  public PrintWriter getWriter() throws IOException {
    return writer;
  }
  
  @Override
  public void flushBuffer() throws IOException {
    writer.flush();
    stream.flush();
    super.flushBuffer();
  }
  
  @Override
  public void reset() {
    stream.reset();
    super.reset();
  }
  
  @Override
  public void resetBuffer() {
    stream.reset();
    super.resetBuffer();
  }
}
