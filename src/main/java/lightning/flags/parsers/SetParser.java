package lightning.flags.parsers;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import lightning.flags.FlagSpecException;

public class SetParser<T> extends CollectionParser<T> implements Parser<Set<T>> {
  public SetParser(Type type) throws FlagSpecException {
    super(type);
  }

  @Override
  public Set<T> parse(String value) throws ParseException {
    Set<T> items = new HashSet<>();
    parseInto(items, value);
    return items;
  }
}
