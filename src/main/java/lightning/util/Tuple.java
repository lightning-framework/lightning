package lightning.util;

/**
 * Provides parametric typed tuples of up to size four for convenience.
 * Enables type-safe return of multiple values from functions without having to define a class for the return type.
 * 
 * Example:
 *   Tuple2D<String, Integer> tuple = Tuple.of("hello", 1);
 *   tuple._0 (yields "hello")
 *   tuple._1 (yield 1)
 */
public class Tuple {
  public static final class Tuple1D<T1> {
    public final T1 _0;
    
    private Tuple1D(T1 item) {
      _0 = item;
    }
  }
  
  public static final class Tuple2D<T1, T2> {
    public final T1 _0;
    public final T2 _1;
    
    private Tuple2D(T1 item1, T2 item2) {
      _0 = item1;
      _1 = item2;
    }
  }
  
  public static final class Tuple3D<T1, T2, T3> {
    public final T1 _0;
    public final T2 _1;
    public final T3 _2;
    
    public Tuple3D(T1 item1, T2 item2, T3 item3) {
      _0 = item1;
      _1 = item2;
      _2 = item3;
    }
  }
  
  public static final class Tuple4D<T1, T2, T3, T4> {
    public final T1 _0;
    public final T2 _1;
    public final T3 _2;
    public final T4 _3;
    
    public Tuple4D(T1 item1, T2 item2, T3 item3, T4 item4) {
      _0 = item1;
      _1 = item2;
      _2 = item3;
      _3 = item4;
    }
  }
  
  public static <T1> Tuple1D<T1> of(T1 item) {
    return new Tuple1D<>(item);
  }
  
  public static <T1, T2> Tuple2D<T1, T2> of(T1 item1, T2 item2) {
    return new Tuple2D<>(item1, item2);
  }
  
  public static <T1, T2, T3> Tuple3D<T1, T2, T3> of(T1 item1, T2 item2, T3 item3) {
    return new Tuple3D<>(item1, item2, item3);
  }
  
  public static <T1, T2, T3, T4> Tuple4D<T1, T2, T3, T4> of(T1 item1, T2 item2, T3 item3, T4 item4) {
    return new Tuple4D<>(item1, item2, item3, item4);
  }
}
