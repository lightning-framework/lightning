package lightning.util;

import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

/**
 * Provides utility functions for dealing with enums.
 */
public class Enums {
  /**
   * Attempts to parse a string into an integer, returning
   * an option that resolves if successful.
   * @param value A string (hopefully representing an integer).
   * @return An optional integer (resolved if the string could be parsed).
   */
  private static Optional<Integer> intOption(String value) {
    if (value == null) {
      return Optional.absent();
    }

    try {
      return Optional.of(Integer.parseInt(value));
    } catch (NumberFormatException e) {
      return Optional.absent();
    }
  }
  
  /**
   * @param type The enum class.
   * @param value A string representing an enum value (case-insensitive name or ordinal).
   * @return An optional which resolves to an enum value if one could be parsed from the string.
   */
  public static <T> Optional<T> getValue(Class<T> type, String value) {
    if (value == null) {
      return Optional.absent();
    }
    
    T[] constants = type.getEnumConstants();
    
    Optional<Integer> intValue = intOption(value);
    if (intValue.isPresent()) {
      int offset = intValue.get();
      
      if (offset >= 0 && offset < constants.length) {
        return Optional.of(constants[offset]);
      } else {
        return Optional.absent();
      }
    } else {        
      for (T constant : constants) {
        if (value.equalsIgnoreCase(constant.toString())) {
          return Optional.of(constant);
        }
      }
      
      return Optional.absent();
    }
  }
  
  /** 
   * Parses a string listing enum values into a set of native enum values.
   * @param type The enum type.
   * @param values A list of enum values (comma separated, values may be ordinals or case insensitive names).
   * @return A set of enum values parsed from the string (possibly empty).
   */
  public static <T extends Enum<T>> Set<T> getValues(Class<T> type, String values) {
    if (values == null) {
      return ImmutableSet.of();
    }
    
    ImmutableSet.Builder<T> builder = new ImmutableSet.Builder<T>();
    String[] parts = values.split(",");
    
    for (String p : parts) {
      Optional<T> value = getValue(type, p);
      if (value.isPresent()) {
        builder.add(value.get());
      }
    }
    
    return builder.build();
  }
}
