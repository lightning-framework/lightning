package lightning.flags;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import lightning.flags.parsers.ArrayParser;
import lightning.flags.parsers.BooleanParser;
import lightning.flags.parsers.DoubleParser;
import lightning.flags.parsers.EnumParser;
import lightning.flags.parsers.FloatParser;
import lightning.flags.parsers.IntegerParser;
import lightning.flags.parsers.ListParser;
import lightning.flags.parsers.LongParser;
import lightning.flags.parsers.MapParser;
import lightning.flags.parsers.ParseException;
import lightning.flags.parsers.Parser;
import lightning.flags.parsers.SetParser;
import lightning.flags.parsers.ShortParser;
import lightning.flags.parsers.StringParser;

/**
 * Provides utility functions for parsing command-line flags.
 *
 * Usage Example:
 *
 * class MyApplication {
 *   @FlagSpec(description="A program that does something.")
 *   static class Options {
 *      // Will parse --port <int> from the command line.
 *      // If not present on the command line, tries to fetch from $SERVER_PORT.
 *      // If not present in either, Flags.parse() will error.
 *      // If not an integer, Flags.parse() will error.
 *      @Flag(names={"port"},
 *            description="Specifies the port to listen on.",
 *            environment="SERVER_PORT",
 *            required=true)
 *      static int port = 8080; // Specify default value.
 *
 *      // Will parse --port port1,port2,port3,etc from the command line.
 *      @Flag(names={"ports"},
 *            decription="Specifies ports to listen on",
 *            required=true)
 *      // Can parse native arrays, lists, and sets.
 *      static int[] ports = new int[]{8080}; // Specify default value.
 *      static List<Integer> ports = ImmutableList.of(8080);
 *      static Set<Integer> ports = ImmutableList.of(8080);
 *
 *      // Will parse --users uid1=username1,uid2=username2
 *      @Flag(names={"users"},
 *            description="Specifies map of user ids to usernames")
 *      static Map<Integer, String> users = ImmutableMap.of(); // Specify default value.
 *   }
 *
 *   public static void main(String[] args) throws Exception {
 *     args = Flags.parse(Options.class, System.out, args);
 *     // args contains anything left after the flags are parsed
 *     // e.g. MyApplication --port 8080 arg1 arg2
 *     //      MyApplication arg1 arg2 --port 8080
 *     //      In both cases, args will be [arg1, arg2]
 *     // Passing --help will print usage info and exit.
 *   }
 * }
 *
 * The supported basic types are:
 *   Boolean, boolean
 *   Integer, int
 *   Long, long
 *   Float, float
 *   Double, double
 *   Short, short
 *   String
 *   Enum
 *
 * The supported composite types are:
 *   List<T>
 *   Set<T>
 *   Map<K,V>
 *   T[]
 *   where T, K, V are any of the basic types above.
 *
 */
public class Flags {
  private static final String FLAG_PREFIX = "--";

  private static final Set<String> HELP_FLAGS = ImmutableSet.of("help");

  private static final Map<Class<?>, Parser<?>> PARSERS =
      new ImmutableMap.Builder<Class<?>, Parser<?>>()
          .put(Boolean.class, new BooleanParser())
          .put(boolean.class, new BooleanParser())
          .put(Integer.class, new IntegerParser())
          .put(int.class, new IntegerParser())
          .put(Float.class, new FloatParser())
          .put(float.class, new FloatParser())
          .put(String.class, new StringParser())
          .put(Double.class, new DoubleParser())
          .put(double.class, new DoubleParser())
          .put(Long.class, new LongParser())
          .put(long.class, new LongParser())
          .put(Short.class, new ShortParser())
          .put(short.class, new ShortParser())
          // TODO: Byte/byte?
          // TODO: Character/char?
          // TODO: File?
          .build();

  private static final Map<Class<?>, String> TYPE_NAME_OVERRIDES =
      new ImmutableMap.Builder<Class<?>, String>()
          .put(Boolean.class, "boolean")
          .put(Byte.class, "byte")
          .put(Character.class, "char")
          .put(Short.class, "short")
          .put(Integer.class, "int")
          .put(Long.class, "long")
          .put(Float.class, "float")
          .put(Double.class, "double")
          .put(String.class, "string")
          .build();

  public static String[] parse(Class<?> spec, PrintStream out, String[] args) throws FlagException {
    Map<String, FlagSpecInfo> flags = parseFlagSpec(spec);
    Map<String, String> map = new HashMap<>();
    args = parseArgs(map, args);

    for (String key : HELP_FLAGS) {
      if (map.containsKey(key)) {
        printUsage(spec, flags, out);
        System.exit(0);
      }
    }

    for (Map.Entry<String, String> entry : map.entrySet()) {
      if (!flags.containsKey(entry.getKey())) {
        throw new FlagParseException(String.format("Unable to parse unknown flag '%s'.", entry.getKey()));
      }

      FlagSpecInfo flag = flags.get(entry.getKey());

      if (entry.getValue() == null && !(flag.parser instanceof BooleanParser)) {
        throw new FlagParseException(String.format("Value is required for flag '%s' (declared on %s.%s) of type '%s'.",
            flag.spec.names()[0], flag.field.getDeclaringClass().getSimpleName(), flag.field.getName(), formatTypeName(flag.field.getGenericType())));
      }

      flag.parseAndSetValue(entry.getValue());
    }

    for (FlagSpecInfo flag : flags.values()) {
      if (flag.didSetValue()) {
        continue;
      }

      if (flag.spec.environment() != null &&
          flag.spec.environment().length() > 0) {
        String environmentValue = System.getenv(flag.spec.environment());
        if (environmentValue != null) {
          flag.parseAndSetValue(environmentValue);
        }
      }
    }

    for (FlagSpecInfo flag : flags.values()) {
      if (flag.spec.required() && !flag.didSetValue()) {
        throw new FlagParseException(String.format("No value provided for required flag '%s' (declared on %s.%s).",
            flag.spec.names()[0], flag.field.getDeclaringClass().getSimpleName(), flag.field.getName()));
      }
    }

    return args;
  }

  private static void printUsage(Class<?> spec, Map<String, FlagSpecInfo> flags, PrintStream out) {
    FlagSpec fspec = spec.getAnnotation(FlagSpec.class);
    System.out.format("%s\n\n", fspec.description());

    System.out.format("Options:\n\n");

    System.out.format("  --help\n");
    System.out.format("      Displays usage instructions.\n\n");

    for (FlagSpecInfo flag : flags.values()) {
      if (flag.spec.hidden()) {
        continue;
      }

      System.out.format("  --%s <%s>\n", flag.spec.names()[0], formatTypeName(flag.field.getGenericType()));
      System.out.format("      %s\n", flag.spec.description());

      boolean hasEnviron = false;

      if (flag.spec.environment() != null &&
          flag.spec.environment().length() > 0) {
        hasEnviron = true;
        System.out.format("      If not specified, will take the value of environment variable $%s", flag.spec.environment());

        String env = System.getenv(flag.spec.environment());
        if (env == null) {
          System.out.format(" (currently not set).\n");
        } else {
          System.out.format(".\n", env);
          System.out.format("      $%s is currently set to '%s'.\n", flag.spec.environment(), env);
        }
      }

      if (flag.spec.required()) {
        if (hasEnviron) {
          System.out.format("      If the environment variable is not set, then you must specify a value.\n");
        } else {
          System.out.format("      You must specify a value (required).\n");
        }
      }
      else {
        try {
          if (hasEnviron) {
            System.out.format("      Otherwise, defaults to '%s'.\n", formatDefaultValue(flag.field));
          } else {
            System.out.format("      Defaults to '%s'.\n", formatDefaultValue(flag.field));
          }
        } catch (Exception e) {}
      }

      System.out.format("\n");
    }
  }

  private static String formatDefaultValue(Field field) throws IllegalArgumentException, IllegalAccessException {
    field.setAccessible(true);
    Object value = field.get(null);

    if (value == null) {
      return "None";
    }

    return value.toString();
  }

  private static String formatTypeName(Type type) {
    if (type instanceof Class) {
      Class<?> ctype = (Class<?>)type;

      if (ctype.isArray()) {
        return formatTypeName(ctype.getComponentType()) + ",...";
      }
      else if (ctype.isEnum()) {
        return "{" + Joiner.on("|").join(ctype.getEnumConstants()) + "}";
      }
      else if (TYPE_NAME_OVERRIDES.containsKey(ctype)) {
        return TYPE_NAME_OVERRIDES.get(ctype);
      }

      return ((Class<?>)type).getSimpleName();
    }

    else if (type instanceof ParameterizedType) {
      ParameterizedType ptype = (ParameterizedType)type;

      if (ptype.getRawType() == List.class) {
        if (ptype.getActualTypeArguments().length == 1) {
          return String.format("<%s>,...",
              formatTypeName(ptype.getActualTypeArguments()[0]));
        }
      }
      else if (ptype.getRawType() == Set.class) {
        if (ptype.getActualTypeArguments().length == 1) {
          return String.format("<%s>,...",
              formatTypeName(ptype.getActualTypeArguments()[0]));
        }
      }
      else if (ptype.getRawType() == Map.class) {
        if (ptype.getActualTypeArguments().length == 2) {
          return String.format("<%s>=<%s>,...",
              formatTypeName(ptype.getActualTypeArguments()[0]),
              formatTypeName(ptype.getActualTypeArguments()[1]));
        }
      }
    }

    return type.getTypeName();
  }

  private static String[] parseArgs(Map<String, String> map, String[] args) {
    List<String> remainder = new ArrayList<>();

    int i = 0;
    while (i < args.length) {
      if (args[i].startsWith(FLAG_PREFIX)) {
        if (i == args.length - 1) {
          map.put(args[i].substring(FLAG_PREFIX.length(), args[i].length()), null);
        } else if (args[i+1].startsWith(FLAG_PREFIX)) {
          map.put(args[i].substring(FLAG_PREFIX.length(), args[i].length()), null);
        } else {
          map.put(args[i].substring(FLAG_PREFIX.length(), args[i].length()), args[i+1]);
          i++;
        }

        i++;
      } else {
        remainder.add(args[i]);
        i++;
      }
    }

    return remainder.toArray(new String[remainder.size()]);
  }

  private static class FlagSpecInfo {
    public final Flag spec;
    public final Field field;
    public final Parser<?> parser;
    private boolean didSetValue;

    public FlagSpecInfo(Flag spec, Field field, Parser<?> parser) {
      this.spec = spec;
      this.field = field;
      this.parser = parser;
      this.didSetValue = false;
    }

    public boolean didSetValue() {
      return didSetValue;
    }

    public void setValue(Object value) throws FlagException {
      try {
        field.set(null, value);
        this.didSetValue = true;
      } catch (Exception e) {
        throw new FlagException(e);
      }
    }

    public void parseAndSetValue(String value) throws FlagException {
      try {
        setValue(parser.parse(value));
      } catch (ParseException e) {
        throw new FlagException(String.format("Failed to parse value for flag '%s' (declared on %s.%s) of type '%s'.",
            spec.names()[0], field.getDeclaringClass().getSimpleName(), field.getName(), formatTypeName(field.getGenericType())), e);
      }
    }
  }

  private static Map<String, FlagSpecInfo> parseFlagSpec(Class<?> spec) throws FlagException {
    if (!spec.isAnnotationPresent(FlagSpec.class)) {
      throw new FlagSpecException(String.format("FlagSpec class '%s' must be annotated with @FlagSpec.",
                                                spec.toString()));
    }

    FlagSpec fspec = spec.getAnnotation(FlagSpec.class);
    if (fspec.description() == null || fspec.description().length() == 0) {
      throw new FlagSpecException(String.format("FlagSpec class '%s' has an empty @FlagSpec description.",
                                                spec.toString()));
    }

    Map<String, FlagSpecInfo> flags = new HashMap<>();

    for (Field f : spec.getDeclaredFields()) {
      if (!f.isAnnotationPresent(Flag.class)) {
        continue;
      }

      f.setAccessible(true);

      if (!Modifier.isStatic(f.getModifiers())) {
        throw new FlagSpecException(String.format("Flag declared on non-static field '%s'.",
                                                  f.toString()));
      }

      Flag flag = f.getAnnotation(Flag.class);

      if (flag.names().length == 0) {
        throw new FlagSpecException(String.format("Flag declared for field '%s' has no names.",
                                                  f.toString()));
      }
      if (flag.description().length() == 0) {
        throw new FlagSpecException(String.format("Flag declared for field '%s' contains an empty description.",
                                                  f.toString()));
      }

      for (String name : flag.names()) {
        if (name.length() == 0) {
          throw new FlagSpecException(String.format("Flag declared for field '%s' contains an empty name.",
                                                    f.toString()));
        }
        if (HELP_FLAGS.contains(name)) {
          throw new FlagSpecException(String.format("Flag declared for field '%s' references reserved name '%s'.",
                                                    f.toString(), name));
        }
        if (flags.containsKey(name)) {
          throw new FlagSpecException(String.format("Flag name '%s' is duplicated (found on '%s', '%s').",
                                                    name, f.toString(), flags.get(name).field.toString()));
        }


        try {
          Parser<?> parser = getParser(f.getGenericType());
          flags.put(name, new FlagSpecInfo(flag, f, parser));
        } catch (FlagSpecException e) {
          throw new FlagSpecException(String.format("Flag declared for field '%s.%s' references unsupported type '%s'.",
                                                    f.getDeclaringClass().getSimpleName(), f.getName(), f.getGenericType().toString()), e);
        }
      }
    }

    return flags;
  }

  public static Parser<?> getParser(Type type) throws FlagSpecException {
    if (type instanceof GenericArrayType) {
      // Unsupported.
    }

    else if (type instanceof TypeVariable) {
      // Unsupported.
    }

    else if (type instanceof ParameterizedType) {
      ParameterizedType ptype = (ParameterizedType)type;

      if (ptype.getRawType() == List.class) {
        if (ptype.getActualTypeArguments().length == 1) {
          return new ListParser<Object>(ptype.getActualTypeArguments()[0]);
        }
      }
      else if (ptype.getRawType() == Set.class) {
        if (ptype.getActualTypeArguments().length == 1) {
          return new SetParser<Object>(ptype.getActualTypeArguments()[0]);
        }
      }
      else if (ptype.getRawType() == Map.class) {
        if (ptype.getActualTypeArguments().length == 2) {
          return new MapParser<Object, Object>(ptype.getActualTypeArguments()[0],
                                               ptype.getActualTypeArguments()[1]);
        }
      }
    }

    else if (type instanceof Class) {
      Class<?> ctype = (Class<?>)type;

      if (ctype.isArray()) {
        return new ArrayParser<Object>(ctype.getComponentType());
      }
      else if (ctype.isEnum()) {
        return EnumParser.create(ctype);
      }
      else if (PARSERS.containsKey(type)) {
        return PARSERS.get(type);
      }
    }

    throw new FlagSpecException(String.format("Unable to find parser for type '%s'.", type));
  }
}
