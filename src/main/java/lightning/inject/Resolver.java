package lightning.inject;

/**
 * Used to inject a dependency of type T that is resolved to an instance at a later point in time.
 * Allows for lazy instantiation of injected values.
 * @param <T> The type of injected value.
 */
public interface Resolver<T> {
  public T resolve() throws Exception;
}
