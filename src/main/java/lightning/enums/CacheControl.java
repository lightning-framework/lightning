package lightning.enums;

public enum CacheControl {
  PUBLIC,
  PRIVATE,
  NO_CACHE;

  public String toHttpString() {
    switch (this) {
      case PRIVATE:
        return "private";
      case PUBLIC:
        return "public";
      case NO_CACHE:
      default:
        return "no-cache, no-store";
    }
  }
}
