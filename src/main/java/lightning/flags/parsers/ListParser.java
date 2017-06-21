package lightning.flags.parsers;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import lightning.flags.FlagSpecException;

public class ListParser<T> extends CollectionParser<T> implements Parser<List<T>> {
  public ListParser(Type type) throws FlagSpecException {
    super(type);
  }

  @Override
  public List<T> parse(String value) throws ParseException {
    List<T> items = new ArrayList<>();
    parseInto(items, value);
    return items;
  }
}
