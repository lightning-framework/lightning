package lightning.mvc;

import javax.annotation.Nullable;

import lightning.http.BadRequestException;

/**
 * A wrapper for a (key, value) pair which can be used to chain assertions
 * about the pair.
 */
public class ParamTester {
  public static ParamTester create(String key, @Nullable String value) {
    return new ParamTester(Param.wrap(key, value));
  }
  
  private final Param value;
  
  private ParamTester(Param value) {
    this.value = value;
  }
  
  private <T extends Exception> void errorIf(boolean condition, Class<T> type, String format, Object ...args) throws T {
    if (condition) {
      String message = String.format(format, args);
      T e;
      try {
        e = (T) type.getConstructor(String.class).newInstance(message);
      } catch (Exception err) {
        throw new RuntimeException(err); // Shouldn't ever happen.
      }
      throw e;
    }
  }
  
  public ParamTester isNull() {
    errorIf(!value.isNull(), BadRequestException.class, "A value must be provided for %s.", value.getKey());
    return this;
  }
  
  public ParamTester isNotNull() {
    errorIf(!value.isNotNull(), BadRequestException.class, "A value must not be provided for %s.", value.getKey());
    return this;
  }
  
  public ParamTester isEmpty() {
    errorIf(!value.isEmpty(), BadRequestException.class, "A non-empty value must be provided for %s.", value.getKey());
    return this;
  }
  
  public ParamTester isNotEmpty() {
    errorIf(!value.isNotEmpty(), BadRequestException.class, "A non-empty value must not be provided for %s.", value.getKey());
    return this;
  }
  
  public ParamTester isLong() {
    errorIf(!value.isLong(), BadRequestException.class, "A long value must be provided for %s.", value.getKey());
    return this;
  }
  
  public ParamTester isInteger() {
    errorIf(!value.isInteger(), BadRequestException.class, "An integer value must be provided for %s.", value.getKey());
    return this;
  }
  
  public ParamTester isDouble() {
    errorIf(!value.isDouble(), BadRequestException.class, "A double value must be provided for %s.", value.getKey());
    return this;
  }
  
  public ParamTester isFloat() {
    errorIf(!value.isFloat(), BadRequestException.class, "A float value must be provided for %s.", value.getKey());
    return this;
  }
  
  public ParamTester isChecked() {
    errorIf(!value.isChecked(), BadRequestException.class, "%s must be checked..", value.getKey());
    return this;
  }
  
  public ParamTester isBoolean() {
    errorIf(!value.isBoolean(), BadRequestException.class, "A boolean value must be provided for %s.", value.getKey());
    return this;
  }
  
  public ParamTester isNotChecked() {
    errorIf(!value.isNotChecked(), BadRequestException.class, "%s must not be checked.", value.getKey());
    return this;
  }
  
  public <T extends Enum<T>> ParamTester isEnum(Class<T> type) {
    errorIf(!value.isEnum(type), BadRequestException.class, "An enum %s value must be provided for %s.", type.getSimpleName(), value.getKey());
    return this;
  }
}
