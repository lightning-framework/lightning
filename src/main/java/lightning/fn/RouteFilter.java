package lightning.fn;

@FunctionalInterface
public interface RouteFilter {
  public void execute() throws Exception;
}
