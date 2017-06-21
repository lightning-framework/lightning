package lightning.flags.parsers;

public class IntegerParser implements Parser<Integer> {
  @Override
  public Integer parse(String value) throws ParseException {
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
      throw new ParseException(e);
    }
  }
}
