package lightning.flags.parsers;

import java.lang.reflect.Type;
import java.util.Collection;

import lightning.flags.FlagSpecException;
import lightning.flags.Flags;

public abstract class CollectionParser<T> {
  protected final Type type;
  protected final Parser<T> itemParser;

  @SuppressWarnings("unchecked")
  public CollectionParser(Type type) throws FlagSpecException {
    this.type = type;
    this.itemParser = (Parser<T>)Flags.getParser(type);
  }

  protected void parseInto(Collection<T> items, String value) throws ParseException {
    String[] values = value.split(",");
    for (String v : values) {
      items.add(itemParser.parse(v));
    }
  }
}
