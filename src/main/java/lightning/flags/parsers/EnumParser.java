package lightning.flags.parsers;

public class EnumParser<T extends Enum<T>> implements Parser<T> {
  private final Class<T> type;

  public EnumParser(Class<T> type) {
    this.type = type;
  }

  @Override
  public T parse(String value) throws ParseException {
    try {
      return Enum.valueOf(type, value);
    } catch (Exception e) {
      throw new ParseException(String.format("'%s' is not a valid value in enum '%s'.", value, type.getSimpleName()));
    }
  }

  private static enum VoidEnumType {}

  @SuppressWarnings("unchecked")
  public static EnumParser<?> create(Class<?> type) {
    return new EnumParser<VoidEnumType>((Class<VoidEnumType>)type);
  }
}
