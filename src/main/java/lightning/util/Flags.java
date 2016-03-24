package lightning.util;

import java.io.File;
import java.util.HashMap;

import com.google.common.base.Optional;

/**
 * A utility library for parsing command-line flags.
 * Accepts flags in the format "--name value --name2 value2".
 */
public class Flags {
  private static HashMap<String, String> flags = new HashMap<>();
  private static final String FLAG_PREFIX = "--";
  
  /**
   * Parses the provided arguments.
   * @param args Provided command-line arguments.
   */
  public static void parse(String[] args) {
    int i = 0;
    while (i < args.length) {
      if (args[i].startsWith(FLAG_PREFIX)) {
        if (i == args.length - 1) {
          flags.put(args[i].substring(FLAG_PREFIX.length(), args[i].length()), null);
        } else if (args[i+1].startsWith(FLAG_PREFIX)) {
          flags.put(args[i].substring(FLAG_PREFIX.length(), args[i].length()), null);
        } else {
          flags.put(args[i].substring(FLAG_PREFIX.length(), args[i].length()), args[i+1]);
        }
        
        i++;
      } else {
        i++;
      }
    }
  }
  
  /**
   * Returns whether or not a flag was present.
   * @param flagName A flag name.
   * @return Whether or not -flagName was present.
   */
  public static boolean has(String flagName) {
    return flags.containsKey(flagName);
  }
  
  /**
   * Returns whether or not a value was present for a flag.
   * @param flagName A flag name.
   * @return Whether or not a value was provided with -flagName.
   */
  public static boolean hasValue(String flagName) {
    return flags.containsKey(flagName) && flags.get(flagName) != null;
  }
  
  /**
   * @param flagName A flag name.
   * @return A file representing the path given in the flag flagName.
   * @throws FlagsException If file does not exist or is not readable.
   */
  public static File getFile(String flagName) throws FlagsException {
    if (flags.containsKey(flagName)) {
      File file = new File(flags.get(flagName));
      
      if (file.exists() && !file.isDirectory() && file.canRead()) {
        return file;
      }
      
      throw new FlagsException("Error: File " + flags.get(flagName) + " does not exist or is not readable.");
    }
    
    throw new FlagsException("Error: No flag exists named " + flagName);
  }
  
  /**
   * @param flagName A flag name.
   * @return The string value of the given flag.
   * @throws FlagsException The string value of the given flag (may be null).
   */
  public static String getString(String flagName) throws FlagsException {
    if (hasValue(flagName)) {
      return flags.get(flagName);
    }
    
    throw new FlagsException("Error: No flag exists named " + flagName);    
  }
  
  /**
   * @param flagName A flag name.
   * @param defaultValue A default value.
   * @return The user-provide value of flagName, or the default if not provided.
   */
  public static String getStringOrDefault(String flagName, String defaultValue) {
    return hasValue(flagName) ? flags.get(flagName) : defaultValue;
  }
  
  /**
   * @param flagName A flag name.
   * @return An option that is filled iff the given flag has a value.
   */
  public static Optional<String> getStringOption(String flagName) {
    return hasValue(flagName) ? Optional.of(flags.get(flagName)) : Optional.absent();
  }
  
  /**
   * @param flagName A flag name.
   * @return Returns the long value of a given flag.
   * @throws FlagsException If not provided or not a long.
   */
  public static long getLong(String flagName) throws FlagsException {
    if (hasValue(flagName)) {
      try {
        return Long.parseLong(flags.get(flagName));
     } catch (NumberFormatException nfe) {
       throw new FlagsException("Error: Flag " + flagName + " must be a number.");
     }
    }
    
    throw new FlagsException("Error: No flag exists named " + flagName);
  }
  
  /**
   * @param flagName A flag name.
   * @return Returns the long value of a given optional flag.
   * @throws FlagsException If not provided or not a long.
   */
  public static Optional<Long> getLongOption(String flagName) throws FlagsException {
    if (hasValue(flagName)) {
      try {
       return Optional.of(Long.parseLong(flags.get(flagName)));
     } catch (NumberFormatException nfe) {
       return Optional.absent();
     }
    }
    
    return Optional.absent();
  }
  
  /**
   * @param flagName A flag name.
   * @param defaultValue A value.
   * @return The long value of a given flag or defaultValue if not present/not valid.
   */
  public static long getLongOrDefault(String flagName, long defaultValue) {
    if (hasValue(flagName)) {
      try {
        return Long.parseLong(flags.get(flagName));
     } catch (NumberFormatException nfe) {
       return defaultValue;
     }
    }
    
    return defaultValue;
  }
  
  /**
   * @param flagName A flag name.
   * @return Returns the long value of a given flag.
   * @throws FlagsException If not provided or not a long.
   */
  public static int getInt(String flagName) throws FlagsException {
    if (flags.containsKey(flagName)) {
      try {
        return Integer.parseInt(flags.get(flagName));
     } catch (NumberFormatException nfe) {
       throw new FlagsException("Error: Flag " + flagName + " must be a number.");
     }
    }
    
    throw new FlagsException("Error: No flag exists named " + flagName);
  }
  
  /**
   * @param flagName A flag name.
   * @return Returns the long value of a given optional flag.
   * @throws FlagsException If not provided or not a long.
   */
  public static Optional<Integer> getIntOption(String flagName) throws FlagsException {
    if (hasValue(flagName)) {
      try {
       return Optional.of(Integer.parseInt(flags.get(flagName)));
     } catch (NumberFormatException nfe) {
       return Optional.absent();
     }
    }
    
    return Optional.absent();
  }
  
  /**
   * @param flagName A flag name.
   * @param defaultValue A value.
   * @return The int value of a given flag or defaultValue if not present/not valid.
   */
  public static int getIntOrDefault(String flagName, int defaultValue) {
    if (flags.containsKey(flagName)) {
      try {
        return Integer.parseInt(flags.get(flagName));
     } catch (NumberFormatException nfe) {
       return defaultValue;
     }
    }
    
    return defaultValue;
  }
  
  /**
   * @param flagName The name of the flag.
   * @param type The type of the enum.
   * @return An option representing the provided enum as given as a flag.
   * @throws FlagsException
   */
  public static <T extends Enum<T>> Optional<T> getEnumOption(String flagName, Class<T> type) throws FlagsException {
    if (!hasValue(flagName)) {
      return Optional.absent();
    }
    
    T[] constants = type.getEnumConstants();
    
    Optional<Integer> intValue = getIntOption(flagName);
    if (intValue.isPresent()) {
      int offset = intValue.get();
      
      if (offset >= 0 && offset < constants.length) {
        return Optional.of(constants[offset]);
      } else {
        return Optional.absent();
      }
    } else {
      String value = getString(flagName);
      
      for (T constant : constants) {
        if (value.equalsIgnoreCase(constant.toString())) {
          return Optional.of(constant);
        }
      }
      
      return Optional.absent();
    }
  }
  
  /**
   * An exception throwing when flag errors occur.
   */
  public static final class FlagsException extends Exception {
    private static final long serialVersionUID = 1L;

    public FlagsException() {
      super();
    }
    
    public FlagsException(String message) {
      super(message);
    }
    
    public FlagsException(Exception e) {
      super(e);
    }
  }
}
