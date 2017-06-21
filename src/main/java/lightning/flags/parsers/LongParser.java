package lightning.flags.parsers;

public class LongParser implements Parser<Long> {
  @Override
  public Long parse(String value) throws ParseException {
    try {
      return Long.parseLong(value);
    } catch (Exception e) {
      throw new ParseException(e);
    }
  }
}
