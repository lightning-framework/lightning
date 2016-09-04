package lightning.io;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import lightning.exceptions.LightningRuntimeException;

public final class LightningPrintWriter extends PrintWriter {
  private final UnflushableLightningServletOutputStream delegate;
  
  public LightningPrintWriter(UnflushableLightningServletOutputStream delegate) {
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