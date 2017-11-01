package lightning.flags.parsers;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import lightning.flags.FlagSpecException;

public class ArrayParser<T> extends CollectionParser<T> implements Parser<Object> {
  public ArrayParser(Type type) throws FlagSpecException {
    super(type);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object parse(String value) throws ParseException {
    List<T> items = new ArrayList<>();
    parseInto(items, value);

    Object result = Array.newInstance((Class<?>)type, items.size());

    // The type system really falls apart here... :(
    // Arrays of primitives cannot conform to T[].
    if (type == int.class) {
      int[] dst = (int[])result;
      int i = 0;
      for (Integer item : (List<Integer>)items) {
        dst[i++] = item;
      }
    }
    else if (type == long.class) {
      long[] dst = (long[])result;
      int i = 0;
      for (Long item : (List<Long>)items) {
        dst[i++] = item;
      }
    }
    else if (type == float.class) {
      float[] dst = (float[])result;
      int i = 0;
      for (Float item : (List<Float>)items) {
        dst[i++] = item;
      }
    }
    else if (type == double.class) {
      double[] dst = (double[])result;
      int i = 0;
      for (double item : (List<Double>)items) {
        dst[i++] = item;
      }
    }
    else if (type == short.class) {
      short[] dst = (short[])result;
      int i = 0;
      for (Short item : (List<Short>)items) {
        dst[i++] = item;
      }
    }
    else if (type == boolean.class) {
      boolean[] dst = (boolean[])result;
      int i = 0;
      for (Boolean item : (List<Boolean>)items) {
        dst[i++] = item;
      }
    }
    else if (type == char.class) {
      char[] dst = (char[])result;
      int i = 0;
      for (Character item : (List<Character>)items) {
        dst[i++] = item;
      }
    }
    else if (type == byte.class) {
      byte[] dst = (byte[])result;
      int i = 0;
      for (Byte item : (List<Byte>)items) {
        dst[i++] = item;
      }
    }
    else {
      items.toArray((T[])result);
    }

    return result;
  }
}
