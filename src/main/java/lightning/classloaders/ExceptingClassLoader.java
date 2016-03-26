package lightning.classloaders;

import java.util.List;

public class ExceptingClassLoader extends DynamicClassLoader {
  @FunctionalInterface
  public static interface ClassLoaderExceptor {
    public boolean except(String className);
  }
  
  public static class PrefixClassLoaderExceptor implements ClassLoaderExceptor {
    private List<String> prefixes;
    
    public PrefixClassLoaderExceptor(List<String> prefixes) {
      this.prefixes = prefixes;
    }
    
    @Override
    public boolean except(String className) {
      for (String prefix : prefixes) {
        if (className.startsWith(prefix)) {
          return false;
        }
      }
      
      return true;
    }
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
