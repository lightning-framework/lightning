package lightning.flags.parsers;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class BooleanParser implements Parser<Boolean> {
  public static final Set<String> TRUE_VALUES = ImmutableSet.of("true", "1", "yes");
  public static final Set<String> FALSE_VALUES = ImmutableSet.of("false", "0", "no");

  @Override
  public Boolean parse(String value) throws ParseException {
    if (value == null) {
      return true;
    }

    value = value.toLowerCase();

    if (TRUE_VALUES.contains(value)) {
      return true;
    }

    if (FALSE_VALUES.contains(value)) {
      return false;
    }

    throw new ParseException(String.format("'%s' is not boolean.", value));
  }
}
