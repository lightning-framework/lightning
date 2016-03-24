package lightning.mvc;

@FunctionalInterface
public interface Filter {
  public void execute() throws Exception;
}
