package lightning.cache;

import lightning.mvc.ObjectParam;

public final class CacheResult {
  public final Object token;
  public final ObjectParam value;
  
  public CacheResult(Object token, Object value) {
    this.token = token;
    this.value = new ObjectParam(value);
  }
}
