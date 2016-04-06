package lightning.inject;

/**
 * Used to inject a dependency of type T that is resolved to an instance at a later point in time.
 * @param <T>
 */
public interface Resolver<T> {
  public T resolve() throws Exception;
}
