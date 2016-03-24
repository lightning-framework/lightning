package lightning.classloaders;

import java.util.HashSet;
import java.util.Set;

public abstract class AggressiveClassLoader extends ClassLoader {
  Set<String> loadedClasses = new HashSet<>();
  Set<String> unavaiClasses = new HashSet<>();
  private ClassLoader parent = AggressiveClassLoader.class.getClassLoader();

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    if (loadedClasses.contains(name) || unavaiClasses.contains(name)) {
      return super.loadClass(name); // Use default class loader cache.
    }

    byte[] newClassData = loadNewClass(name);
    if (newClassData != null) {
      loadedClasses.add(name);
      return loadClass(newClassData, name);
    } else {
      unavaiClasses.add(name);
      return parent.loadClass(name);
    }
  }
  
  public Class<?> load(String name) throws ClassNotFoundException {
    return loadClass(name);
  }
  
  protected abstract byte[] loadNewClass(String name);

  public Class<?> loadClass(byte[] classData, String name) {
    Class<?> clazz = defineClass(name, classData, 0, classData.length);
    if (clazz != null) {
      if (clazz.getPackage() == null) {
          definePackage(name.replaceAll("\\.\\w+$", ""), null, null, null, null, null, null, null);
      }
      resolveClass(clazz);
    }
    return clazz;
  }

  public static String toFilePath(String name) {
      return name.replaceAll("\\.", "/") + ".class";
  }
}
