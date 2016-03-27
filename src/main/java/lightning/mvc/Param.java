package lightning.mvc;

import java.util.regex.Pattern;

import javax.annotation.Nullable;

import lightning.http.BadRequestException;
import lightning.util.Enums;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;

/**
 * A wrapper for a (key, value) pair in which the value is optional.
 * 
 * Used all around the framework for convenience. You will find that
 * there are many convenience methods (e.g. intValue()). Methods such
 * as intValue() will either succeed (convert the value to an integer
 * and return it) or fail. On failure, a BadRequestException will be
 * thrown. We recommend not catching these and instead letting the
 * framework capture them and render a bad request page.
 * 
 * There are also methods for testing the contained value (e.g. isInteger)
 * that will not throw exceptions but simply return boolean whether or
 * not the value meets the constraints.
 */
public class Param {
  private final String key;
  private final Optional<String> value;
  
  private Param(String key, Optional<String> value) {
    this.key = key;
    this.value = value;
  }
  
  public <T> Object castTo(Class<T> type) {
    if (type.equals(Integer.class) || type.equals(int.class)) {
      return intValue();
    } else if (type.equals(Long.class) || type.equals(long.class)) {
      return longValue();
    } else if (type.equals(String.class)) {
      return stringValue();
    } else if (type.equals(Double.class) || type.equals(double.class)) {
      return doubleValue();
    } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
      return booleanValue();
    } else if (type.equals(Float.class) || type.equals(float.class)) {
      return floatValue();
    } else if (type.isEnum()) {
      return enumValue(type);
    } else if (type.equals(Param.class)) {
      return this;
    } else {
      throw new BadRequestException("Unable to translate " + key + " to type " + type.getSimpleName());
    }
  }
  
  public Object or(Object other) {
    return isPresent() ? value.get() : other;
  }
  
  public boolean exists() {
    return isPresent();
  }
  
  public String getKey() {
    return key;
  }
  
  public boolean isPresent() {
    return value.isPresent();
  }
  
  public boolean isNull() {
    return !value.isPresent();
  }
  
  public boolean isNotNull() {
    return value.isPresent();
  }
  
  public boolean isEmpty() {
    return isNull() || value.get().length() == 0;
  }
  
  public boolean isNotEmpty() {
    return isNotNull() && value.get().length() > 0;
  }
  
  public boolean isLong() {
    return longOption().isPresent();
  }
  
  public boolean isInteger() {
    return intOption().isPresent();
  }
  
  public boolean isDouble() {
    return doubleOption().isPresent();
  }
  
  public boolean isFloat() {
    return floatOption().isPresent();
  }
  
  public boolean isChecked() {
    return isNotEmpty();
  }
  
  public boolean isBoolean() {
    return booleanOption().isPresent();
  }
  
  public boolean isNotChecked() {
    return isEmpty();
  }
  
  public boolean isAtLeast(double value) {
    return isDouble() && doubleValue() >= value;
  }
  
  public boolean isAtMost(double value) {
    return isDouble() && doubleValue() <= value;
  }
  
  public boolean isInRange(double min, double max) {
    return isDouble() && doubleValue() >= min && doubleValue() <= max;
  }
  
  public boolean isAtLeast(long value) {
    return isLong() && longValue() >= value;
  }
  
  public boolean isAtMost(long value) {
    return isLong() && longValue() <= value;
  }
  
  public boolean isInRange(long min, long max) {
    return isLong() && longValue() >= min && longValue() <= max;
  }
  
  private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[]{"https", "http"});
  public boolean isURL() {
    return isNotNull() && URL_VALIDATOR.isValid(value.get());
  }
  
  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$");
  public boolean isEmail() {
    return matches(EMAIL_PATTERN);
  }
  
  public boolean matches(String regex) {
    return matches(Pattern.compile(regex));
  }
  
  public boolean matches(Pattern pattern) {
    return isNotNull() && pattern.matcher(value.get()).matches();
  }
  
  public boolean isPositiveNonZero() {
    return isAtLeast(0);
  }
  
  public boolean isPositive() {
    return isAtLeast(1);
  }
  
  public boolean isOneOf(Iterable<String> values) {
    return isNotNull() && Iterables.contains(values, value.get());
  }
  
  public boolean isAllUppercase() {
    return isNotNull() && StringUtils.isAllUpperCase(value.get());
  }
  
  public boolean isAllLowercase() {
    return isNotNull() && StringUtils.isAllLowerCase(value.get());
  }
  
  public boolean isAlphaNumericWithSpaces() {
    return isNotNull() && StringUtils.isAlphanumericSpace(value.get());
  }
  
  public boolean isAlphaNumeric() {
    return isNotNull() && StringUtils.isAlphanumeric(value.get());
  }
  
  public boolean isAlphaWithSpaces() {
    return isNotNull() && StringUtils.isAlphaSpace(value.get());
  }
  
  public boolean isAlpha() {
    return isNotNull() && StringUtils.isAlpha(value.get());
  }
  
  public boolean isShorterThan(long numChars) {
    return isNull() || value.get().length() <= numChars;
  }
  
  public boolean isLongerThan(long numChars) {
    return isNotNull() && value.get().length() >= numChars;
  }
  
  public boolean isEqualToCaseInsensitive(String requiredValue) {
    return isNotNull() && requiredValue.equalsIgnoreCase(value.get());
  }
  
  public boolean isEqualTo(String requiredValue) {
    return isNotNull() && requiredValue.equals(value.get());
  }
  
  public <T> boolean isEnum(Class<T> type) {
    return enumOption(type).isPresent();
  }
  
  public Optional<String> stringOption() {
    return value;
  }
  
  public Optional<Long> longOption() {
    if (isEmpty()) {
      return Optional.absent();
    }
    
    try {
      return Optional.of(Long.parseLong(value.get()));
    } catch (NumberFormatException e) {
      return Optional.absent();
    }
  }
  
  public Optional<Integer> intOption() {
    if (isEmpty()) {
      return Optional.absent();
    }
    
    try {
      return Optional.of(Integer.parseInt(value.get()));
    } catch (NumberFormatException e) {
      return Optional.absent();
    }
  }
  
  public Optional<Double> doubleOption() {
    if (isEmpty()) {
      return Optional.absent();
    }
    
    try {
      return Optional.of(Double.parseDouble(value.get()));
    } catch (NumberFormatException e) {
      return Optional.absent();
    }
  }
  
  public Optional<Float> floatOption() {
    if (isEmpty()) {
      return Optional.absent();
    }
    
    try {
      return Optional.of(Float.parseFloat(value.get()));
    } catch (NumberFormatException e) {
      return Optional.absent();
    }
  }
  
  public <T> Optional<T> enumOption(Class<T> type) {
    if (isEmpty()) {
      return Optional.absent();
    }
    
    return Enums.getValue(type, value.get());
  }
  
  public Optional<Boolean> booleanOption() {
    if (isEmpty()) {
      return Optional.absent();
    }
    
    switch (value.get().toLowerCase()) {
      case "1":
      case "true":
        return Optional.of(true);
      case "0":
      case "false":
        return Optional.of(false);
      default:
        return Optional.absent();
    }
  }
  
  public String stringValue() {
    require(value.isPresent(), "string");
    return value.get();
  }
  
  public long longValue() {
    Optional<Long> option = longOption();
    require(option.isPresent(), "long");
    return option.get();
  }
  
  public int intValue() {
    Optional<Integer> option = intOption();
    require(option.isPresent(), "integer");
    return option.get();
  }
  
  public double doubleValue() {
    Optional<Double> option = doubleOption();
    require(option.isPresent(), "double");
    return option.get();
  }
  
  public float floatValue() {
    Optional<Float> option = floatOption();
    require(option.isPresent(), "float");
    return option.get();
  }
  
  public boolean booleanValue() {
    Optional<Boolean> option = booleanOption();
    require(option.isPresent(), "boolean");
    return option.get();
  }
  
  public <T> T enumValue(Class<T> type) {
    Optional<T> option = enumOption(type);
    require(option.isPresent(), "enum " + type.getSimpleName());
    return option.get();
  }
  
  private void require(boolean condition, String type) {
    if (!condition) {
      String sv = value.isPresent() ? value.get() : "EMPTY";
      throw new BadRequestException("Invalid parameter value for '" + key + "', expected " + type + " but got " + sv + ".");
    }
  }
    
  public static Param wrap(String key, @Nullable String value) {
    return new Param(key, Optional.fromNullable(value));
  }

  public Object stringOr(Object other) {
    return stringOption().isPresent() ? stringOption().get() : other;
  }
  
  public Object intOr(Object other) {
    return intOption().isPresent() ? intOption().get() : other;
  }
  
  public Object longOr(Object other) {
    return longOption().isPresent() ? longOption().get() : other;
  }
  
  public Object doubleOr(Object other) {
    return doubleOption().isPresent() ? doubleOption().get() : other;
  }
  
  public Object floatOr(Object other) {
    return floatOption().isPresent() ? floatOption().get() : other;
  }
}
