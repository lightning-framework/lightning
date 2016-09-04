package lightning.io;

import java.io.IOException;

import javax.servlet.WriteListener;

public class UnflushableLightningServletOutputStream extends LightningServletOutputStream {
  private final LightningServletOutputStream delegate;
  
  public UnflushableLightningServletOutputStream(LightningServletOutputStream delegate) {
    super();
    this.delegate = delegate;
  }
  
  @Override
  public void flush() throws IOException {
    // No-op.
  }
  
  public void reallyFlush() throws IOException {
    delegate.flush();
  }

  @Override
  public boolean isBuffering() {
    return delegate.isBuffering();
  }

  @Override
  public void setBuffering(boolean value) {
    delegate.setBuffering(value);
  }

  @Override
  public boolean isReady() {
    return delegate.isReady();
  }

  @Override
  public void setWriteListener(WriteListener writeListener) {
    delegate.setWriteListener(writeListener);
  }

  @Override
  public void write(int b) throws IOException {
    delegate.write(b);
  }
  
  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public void reset() {
    delegate.reset();
  }
}
