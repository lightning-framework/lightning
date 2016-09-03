package lightning.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import lightning.config.Config;
import lightning.exceptions.LightningRuntimeException;
import lightning.util.MimeMatcher;

public class LightningHttpServletResponse extends HttpServletResponseWrapper {  
  private static abstract class LightningServletOutputStream extends ServletOutputStream {
    public abstract boolean isBuffering();
    public abstract void setBuffering(boolean value);
    public abstract void reset();
  }
  
  private static class LightningServletOutputStreamImpl extends LightningServletOutputStream {
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
    
    public LightningServletOutputStreamImpl(ServletOutputStream delegate, int limit) {
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
  
  private static final class UnflushableDecorator extends LightningServletOutputStream {
    private final LightningServletOutputStream delegate;
    
    public UnflushableDecorator(LightningServletOutputStream delegate) {
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

  private static final class LightningPrintWriter extends PrintWriter {
    private final UnflushableDecorator delegate;
    
    public LightningPrintWriter(UnflushableDecorator delegate) {
      super(delegate);
      this.delegate = delegate;
    }
    
    @Override
    public PrintWriter append(char c) {
      super.append(c);
      flushInternal();
      return this;
    }
    
    @Override
    public PrintWriter append(CharSequence csq) {
      super.append(csq);
      flushInternal();
      return this;
    }
    
    @Override
    public PrintWriter append(CharSequence csq, int start, int end) {
      super.append(csq, start, end);
      flushInternal();
      return this;
    }
    
    @Override
    public PrintWriter format(Locale l, String format, Object... args) {
      super.format(l, format, args);
      flushInternal();
      return this;
    }
    
    @Override
    public PrintWriter format(String format, Object... args) {
      super.format(format, args);
      flushInternal();
      return this;
    }
    
    @Override
    public void print(boolean b) {
      super.print(b);
      flushInternal();
    }
    
    @Override
    public void print(char c) {
      super.print(c);
      flushInternal();
    }
    
    @Override
    public void print(char[] s) {
      super.print(s);
      flushInternal();
    }
    
    @Override
    public void print(double d) {
      super.print(d);
      flushInternal();
    }
    
    @Override
    public void print(float f) {
      super.print(f);
      flushInternal();
    }
    
    @Override
    public void print(int i) {
      super.print(i);
      flushInternal();
    }
    
    @Override
    public void print(long l) {
      super.print(l);
      flushInternal();
    }
    
    @Override
    public void print(Object obj) {
      super.print(obj);
      flushInternal();
    }
    
    @Override
    public void print(String s) {
      super.print(s);
      flushInternal();
    }
    
    @Override
    public PrintWriter printf(Locale l, String format, Object ...args) {
      super.printf(l, format, args);
      flushInternal();
      return this;
    }
    
    @Override
    public PrintWriter printf(String format, Object ...args) {
      super.printf(format, args);
      flushInternal();
      return this;
    }
    
    @Override
    public void println() {
      super.println();
      flushInternal();
    }
    
    @Override
    public void println(boolean x) {
      super.println(x);
      flushInternal();
    }
    
    @Override
    public void println(char x) {
      super.println(x);
      flushInternal();
    }
    
    @Override
    public void println(char[] x) {
      super.println(x);
      flushInternal();
    }
    
    @Override
    public void println(double x) {
      super.println(x);
      flushInternal();
    }
    
    @Override
    public void println(float x) {
      super.println(x);
      flushInternal();
    }
    
    @Override
    public void println(int x) {
      super.println(x);
      flushInternal();
    }
    
    @Override
    public void println(long x) {
      super.println(x);
      flushInternal();
    }
    
    @Override
    public void println(Object x) {
      super.println(x);
      flushInternal();
    }
    
    @Override
    public void println(String x) {
      super.println(x);
      flushInternal();
    }
    
    @Override
    public void write(char[] buf) {
      super.write(buf);
      flushInternal();
    }
    
    @Override
    public void write(char[] buf, int off, int len) {
      super.write(buf, off, len);
      flushInternal();
    }
    
    @Override
    public void write(int c) {
      super.write(c);
      flushInternal();
    }
    
    @Override
    public void write(String s) {
      super.write(s);
      flushInternal();
    }
    
    @Override
    public void write(String s, int off, int len) {
      super.write(s, off, len);
      flushInternal();
    }
    
    public void flushInternal() {
      super.flush();
    }
    
    @Override
    public void flush() {
      super.flush();
      try {
        delegate.reallyFlush();
      } catch (IOException e) {
        throw new LightningRuntimeException(e);
      }
    }
  }
  
  private final Config config;
  private final MimeMatcher bufferingMatcher;
  private LightningServletOutputStream stream;
  private LightningPrintWriter writer;
  
  public LightningHttpServletResponse(HttpServletResponse response, Config config, 
      MimeMatcher bufferingMatcher) throws IOException {
    super(response);
    this.config = config;
    this.bufferingMatcher = bufferingMatcher;
    this.stream = new LightningServletOutputStreamImpl(super.getOutputStream(), config.server.outputBufferingLimitBytes);
    this.writer = new LightningPrintWriter(new UnflushableDecorator(this.stream));
  }
  
  public boolean isBuffered() {
    return stream.isBuffering();
  }
  
  private void updateBufferingState() {
    stream.setBuffering(
        config.server.enableOutputBuffering &&
        bufferingMatcher.matches(getContentType())
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
