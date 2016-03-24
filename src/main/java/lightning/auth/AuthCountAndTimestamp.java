package lightning.auth;

public final class AuthCountAndTimestamp {
  private final long rowCount;
  private final long maxTimestamp;
  
  public static AuthCountAndTimestamp create(long rowCount, long maxTimestamp) {
    return new AuthCountAndTimestamp(rowCount, maxTimestamp);
  }

  public AuthCountAndTimestamp(
    long rowCount,
    long maxTimestamp) {
    this.rowCount = rowCount;
    this.maxTimestamp = maxTimestamp;
  }

  public long rowCount() {
    return rowCount;
  }

  public long maxTimestamp() {
    return maxTimestamp;
  }

  @Override
  public String toString() {
    return "CountAndTimestamp{"
        + "rowCount=" + rowCount + ", "
        + "maxTimestamp=" + maxTimestamp
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof AuthCountAndTimestamp) {
      AuthCountAndTimestamp that = (AuthCountAndTimestamp) o;
      return (this.rowCount == that.rowCount())
           && (this.maxTimestamp == that.maxTimestamp());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= (rowCount >>> 32) ^ rowCount;
    h *= 1000003;
    h ^= (maxTimestamp >>> 32) ^ maxTimestamp;
    return h;
  }

}
