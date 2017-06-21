package lightning.flags.parsers;

public class StringParser implements Parser<String> {
  @Override
  public String parse(String value) throws ParseException {
    if (value == null || value.length() == 0) {
      throw new ParseException("Expected string, found <empty>.");
    }
    return value;
  }
}
