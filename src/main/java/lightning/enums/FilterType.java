package lightning.enums;

public enum FilterType {
  URL,
  EMAIL,
  ALPHA,
  ALPHANUMERIC,
  ALPHA_SPACES,
  ALPHANUMERIC_SPACES,
  DECIMAL_NUMBER,
  NEGATIVE_DECIMAL_NUMBER, // Doesn't include zero.
  POSITIVE_DECIMAL_NUMBER, // Includes zero.
  POSITIVE_NONZERO_DECIMAL_NUMBER,
  INTEGER,
  NEGATIVE_INTEGER, // Doesn't include zero.
  POSITIVE_INTEGER, // Includes zero.
  POSITIVE_NONZERO_INTEGER,
  NOT_EMPTY,
  EMPTY,
  PRESENT, // Checks that value is not null.
  NOT_PRESENT,
  LOWERCASE_ONLY,
  UPPERCASE_ONLY,
  BOOLEAN,
  CHECKED, // Equivalent to PRESENT.
  NOT_CHECKED // Equivalent to NOT_PRESENT.
}
