package lightning.flags.parsers;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import lightning.flags.FlagSpecException;
import lightning.flags.Flags;

public class MapParser<K, V> implements Parser<Map<K, V>> {
  protected final Type keyType;
  protected final Type valueType;
  protected final Parser<K> keyParser;
  protected final Parser<V> valueParser;

  @SuppressWarnings("unchecked")
  public MapParser(Type keyType, Type valueType) throws FlagSpecException {
    this.keyType = keyType;
    this.valueType = valueType;
    this.keyParser = (Parser<K>)Flags.getParser(keyType);
    this.valueParser = (Parser<V>)Flags.getParser(valueType);
  }

  @Override
  public Map<K, V> parse(String value) throws ParseException {
    Map<K, V> map = new HashMap<>();

    String[] entries = value.split(",");
    for (String entry : entries) {
      String[] parts = value.split("=");

      if (parts.length != 2) {
        throw new ParseException(String.format("Unexpected map entry '%s'.", entry));
      }

      map.put(keyParser.parse(parts[0]),
              valueParser.parse(parts[1]));
    }

    return map;
  }
}
