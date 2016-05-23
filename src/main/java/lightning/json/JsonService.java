package lightning.json;

import java.io.InputStream;
import java.io.OutputStream;

import lightning.enums.JsonFieldNamingPolicy;

public interface JsonService {
  public void writeJson(Object object, OutputStream outputStream, JsonFieldNamingPolicy policy) throws Exception;
  public <T> T readJson(Class<T> type, InputStream inputStream, JsonFieldNamingPolicy policy) throws Exception;
}
