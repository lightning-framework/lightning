package lightning.flags.parsers;

public class FloatParser implements Parser<Float> {
  @Override
  public Float parse(String value) throws ParseException {
    try {
      return Float.parseFloat(value);
    } catch (Exception e) {
      throw new ParseException(e);
    }
  }
}
