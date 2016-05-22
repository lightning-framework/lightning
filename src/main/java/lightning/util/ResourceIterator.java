package lightning.util;

public interface ResourceIterator<T> extends AutoCloseable {
  public boolean hasNext() throws Exception;
  public T next() throws Exception;
}
