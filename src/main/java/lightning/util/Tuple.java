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

    @Override
    public String toString() {
      return "Tuple[" + _0 + "]";
    }
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((_0 == null) ? 0 : _0.hashCode());
      return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Tuple1D other = (Tuple1D) obj;
      if (_0 == null) {
        if (other._0 != null)
          return false;
      } else if (!_0.equals(other._0))
        return false;
      return true;
    }
  }
  
  public static final class Tuple2D<T1, T2> {
    public final T1 _0;
    public final T2 _1;
    
    private Tuple2D(T1 item1, T2 item2) {
      _0 = item1;
      _1 = item2;
    }
    
    @Override
    public String toString() {
      return "Tuple[" + _0 + ", " + _1 + "]";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((_0 == null) ? 0 : _0.hashCode());
      result = prime * result + ((_1 == null) ? 0 : _1.hashCode());
      return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Tuple2D other = (Tuple2D) obj;
      if (_0 == null) {
        if (other._0 != null)
          return false;
      } else if (!_0.equals(other._0))
        return false;
      if (_1 == null) {
        if (other._1 != null)
          return false;
      } else if (!_1.equals(other._1))
        return false;
      return true;
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
    
    @Override
    public String toString() {
      return "Tuple[" + _0 + ", " + _1 + ", " + _2 + "]";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((_0 == null) ? 0 : _0.hashCode());
      result = prime * result + ((_1 == null) ? 0 : _1.hashCode());
      result = prime * result + ((_2 == null) ? 0 : _2.hashCode());
      return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Tuple3D other = (Tuple3D) obj;
      if (_0 == null) {
        if (other._0 != null)
          return false;
      } else if (!_0.equals(other._0))
        return false;
      if (_1 == null) {
        if (other._1 != null)
          return false;
      } else if (!_1.equals(other._1))
        return false;
      if (_2 == null) {
        if (other._2 != null)
          return false;
      } else if (!_2.equals(other._2))
        return false;
      return true;
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
    
    @Override
    public String toString() {
      return "Tuple[" + _0 + ", " + _1 + ", " + _2 + ", " + _3 + "]";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((_0 == null) ? 0 : _0.hashCode());
      result = prime * result + ((_1 == null) ? 0 : _1.hashCode());
      result = prime * result + ((_2 == null) ? 0 : _2.hashCode());
      result = prime * result + ((_3 == null) ? 0 : _3.hashCode());
      return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Tuple4D other = (Tuple4D) obj;
      if (_0 == null) {
        if (other._0 != null)
          return false;
      } else if (!_0.equals(other._0))
        return false;
      if (_1 == null) {
        if (other._1 != null)
          return false;
      } else if (!_1.equals(other._1))
        return false;
      if (_2 == null) {
        if (other._2 != null)
          return false;
      } else if (!_2.equals(other._2))
        return false;
      if (_3 == null) {
        if (other._3 != null)
          return false;
      } else if (!_3.equals(other._3))
        return false;
      return true;
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
