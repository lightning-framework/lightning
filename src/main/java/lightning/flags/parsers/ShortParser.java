package lightning.flags.parsers;

public class ShortParser implements Parser<Short> {
  @Override
  public Short parse(String value) throws ParseException {
    try {
      return Short.parseShort(value);
    } catch (Exception e) {
      throw new ParseException(e);
    }
  }
}
