package lightning.json;

import java.io.InputStream;
import java.io.OutputStream;

import lightning.enums.JsonFieldNamingPolicy;

public interface JsonService {
  public static final String DEFAULT_XSSI_PREFIX = "')]}\n";
  public void writeJson(Object object, OutputStream outputStream, JsonFieldNamingPolicy policy) throws Exception;
  public <T> T readJson(Class<T> type, InputStream inputStream, JsonFieldNamingPolicy policy) throws Exception;
}
