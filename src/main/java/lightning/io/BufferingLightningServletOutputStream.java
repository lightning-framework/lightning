package lightning.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import lightning.exceptions.LightningRuntimeException;

public class BufferingLightningServletOutputStream extends LightningServletOutputStream {
  private final ServletOutputStream delegate;
  private final int limit;
  private ByteArrayOutputStream buffer;
  private boolean hasFlushed;
  
  public void setBuffering(boolean value) {
    if (hasFlushed || value == isBuffering()) {
      return;
    }
    
    if (!value) {
      try {
        flush();
      } catch (IOException e) {
        throw new LightningRuntimeException(e);
      }
    } else {
      // TODO: We should use a buffer pool here for performance.
      //       It would also be nice to avoid having an extra buffer copy here.
      buffer = new ByteArrayOutputStream();
    }
  }
  
  @Override
  public void write(int b) throws IOException {
    if (buffer != null) {
      buffer.write(b);
      
      if (limit > 0 && buffer.size() == limit) {
        flush();
      }
    } else {
      delegate.write(b);
    }
  }  
  
  public void reset() {
    if  (buffer != null) {
      buffer.reset();
    }
  }
  
  @Override
  public void close() throws IOException {
    flush();
    super.close();
    delegate.close();
  }
  
  @Override
  public void flush() throws IOException {
    hasFlushed = true;
    
    if (this.buffer != null) {
      ByteArrayOutputStream buffer = this.buffer;
      this.buffer = null;
    
      buffer.writeTo(delegate);
      buffer.reset();
    }
    
    delegate.flush();
  }
  
  public BufferingLightningServletOutputStream(ServletOutputStream delegate, int limit) {
    super();
    this.limit = limit;
    this.delegate = delegate;
    this.buffer = null;
    this.hasFlushed = false;
  }
  
  @Override
  public void setWriteListener(WriteListener writeListener) {
    delegate.setWriteListener(writeListener);
  }
  
  public boolean isBuffering() {
    return buffer != null;
  }

  @Override
  public boolean isReady() {
    return buffer != null || delegate.isReady();
  }
}
