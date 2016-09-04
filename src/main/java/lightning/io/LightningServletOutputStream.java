package lightning.io;

import javax.servlet.ServletOutputStream;

public abstract class LightningServletOutputStream extends ServletOutputStream {
  public abstract boolean isBuffering();
  public abstract void setBuffering(boolean value);
  public abstract void reset();
}
