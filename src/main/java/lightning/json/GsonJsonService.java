package lightning.json;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import com.google.gson.FieldNamingPolicy;

import lightning.enums.JsonFieldNamingPolicy;

public class GsonJsonService implements JsonService {
  @Override
  public void writeJson(Object object, OutputStream outputStream, JsonFieldNamingPolicy policy)
      throws Exception {
    GsonFactory.newJsonParser(convertPolicy(policy)).toJson(object, new OutputStreamWriter(outputStream));
  }

  @Override
  public <T> T readJson(Class<T> type, InputStream inputStream, JsonFieldNamingPolicy policy)
      throws Exception {
    return GsonFactory.newJsonParser(convertPolicy(policy)).fromJson(new InputStreamReader(inputStream), type);
  }
  
  private FieldNamingPolicy convertPolicy(JsonFieldNamingPolicy policy) {
    switch (policy) {
      case IDENTITY:
        return FieldNamingPolicy.IDENTITY;
      case LOWER_CASE_WITH_DASHES:
        return FieldNamingPolicy.LOWER_CASE_WITH_DASHES;
      default:
      case LOWER_CASE_WITH_UNDERSCORES:
        return FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
      case UPPER_CAMEL_CASE:
        return FieldNamingPolicy.UPPER_CAMEL_CASE;
    }
  }
}
