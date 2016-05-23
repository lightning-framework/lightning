package lightning.json;

import java.lang.reflect.Type;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public final class GsonFactory {
  public static Gson newJsonParser() {
    return newJsonParserBuilder().create();
  }
  
  public static Gson newJsonParser(FieldNamingPolicy namePolicy) {
    return newJsonParserBuilder(namePolicy).create();
  }
  
  public static GsonBuilder newJsonParserBuilder() {
    return newJsonParserBuilder(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
  }

  @SuppressWarnings("rawtypes")
  public static GsonBuilder newJsonParserBuilder(FieldNamingPolicy namePolicy) {
    return new GsonBuilder()
    .setPrettyPrinting()
    .setFieldNamingPolicy(namePolicy)
    .disableHtmlEscaping()
    .serializeNulls()
    // Serialize ENUMs to their ordinal.
    .registerTypeHierarchyAdapter(Enum.class, new JsonSerializer<Enum>() {
      @Override
      public JsonElement serialize(Enum item, Type type, JsonSerializationContext ctx) {
        return ctx.serialize(item.ordinal());
      }
    })
    // Deserialize ENUMs from their ordinal.
    .registerTypeHierarchyAdapter(Enum.class, new JsonDeserializer<Enum>() {
      @Override
      public Enum deserialize(JsonElement item, Type type, JsonDeserializationContext ctx)
          throws JsonParseException {
        if (!(type instanceof Class)) {
          throw new JsonParseException("Unable to tranlate enum " + type.getTypeName());
        }
        
        Class enumType = (Class) type;
        Object[] values = enumType.getEnumConstants();
        int index = item.getAsInt();
        if (index >= 0 && index < values.length) {
          return (Enum) values[index];
        } else {
          throw new JsonParseException("Unable to tranlate enum " + type.getTypeName());
        }
      }
    });
  }
}
