package lightning.cache;

@FunctionalInterface
public interface CacheProducer<T> {
  public T yield() throws Exception;
}
