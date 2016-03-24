package lightning.classloaders;

public class ExceptingClassLoader extends DynamicClassLoader {
  @FunctionalInterface
  public static interface ClassLoaderExceptor {
    public boolean except(String className);
  }
  
  private final ClassLoaderExceptor filter;
  
  public ExceptingClassLoader(ClassLoaderExceptor filter, String... paths) {
    super(paths);
    this.filter = filter;
  }
  
  @Override
  protected byte[] loadNewClass(String className) {
    if (filter.except(className)) {
      return null;
    }
    
    return super.loadNewClass(className);
  }
}
