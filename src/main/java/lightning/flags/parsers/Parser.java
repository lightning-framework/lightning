package lightning.flags.parsers;

public interface Parser<T> {
  public T parse(String value) throws ParseException;
}
