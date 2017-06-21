package lightning.flags.parsers;

public class DoubleParser implements Parser<Double> {
  @Override
  public Double parse(String value) throws ParseException {
    try {
      return Double.parseDouble(value);
    } catch (Exception e) {
      throw new ParseException(e);
    }
  }
}
