package lightning.util;

import java.lang.reflect.Method;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import lightning.exceptions.LightningMockThrowable;

public class DebugUtil {
  public static <T extends Throwable> T mockStackTrace(Method method, T cause, boolean first) {
    try {
      Throwable t = new LightningMockThrowable();
      ClassPool pool = ClassPool.getDefault();
      CtClass cc = pool.get(method.getDeclaringClass().getName());

      try {
        CtMethod m = cc.getDeclaredMethod(method.getName());
        t.setStackTrace(
            new StackTraceElement[]{
                new StackTraceElement(method.getDeclaringClass().getCanonicalName(),
                                      method.getName(),
                                      cc.getClassFile().getSourceFile(),
                                      m.getMethodInfo().getLineNumber(first ? 0 : Integer.MAX_VALUE))});
      } finally {
        cc.detach(); /* Force reload of class file from disk next time error occurs. */
      }

      cause.addSuppressed(t);
    } catch (Throwable e) {
      cause.addSuppressed(e);
    }

    return cause;
  }

  public static <T extends Throwable> T mockStackTrace(Class<?> clazz, T cause) {
    try {
      Throwable t = new LightningMockThrowable();
      ClassPool pool = ClassPool.getDefault();
      CtClass cc = pool.get(clazz.getName());

      try {
        t.setStackTrace(
            new StackTraceElement[]{
                new StackTraceElement(clazz.getCanonicalName(),
                                      "<init>",
                                      cc.getClassFile().getSourceFile(),
                                      0)});
      } finally {
        cc.detach(); /* Force reload of class file from disk next time error occurs. */
      }

      cause.addSuppressed(t);
    } catch (Throwable e) {
      cause.addSuppressed(e);
    }

    return cause;
  }
}
