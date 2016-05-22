package lightning.util;

public interface ResourceIterable<T> {
  public ResourceIterator<T> iterator() throws Exception;
}
