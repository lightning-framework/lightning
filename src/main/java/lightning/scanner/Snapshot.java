package lightning.scanner;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

/**
 * Captures a snapshot of the state of the byte code for all available Java classes
 * whose canonical names begin with the prefixes provided. 
 * 
 * The snapshot is captured as a map of canonical class names to (sha1, md5) hash pairs
 * of the byte code for that class.
 * 
 * Two snapshots may be compared via equals(...) to determine whether or not the byte
 * code for any classes of interest has changed between the points at which the snapshots
 * were captured.
 * 
 * This snapshotting system is used to determine whether or not hot swapping needs to 
 * occur when Lightning's debug mode is enabled.
 */
public final class Snapshot {  
  private static final class SnapshotHash {
    public final byte[] sha1_hash;
    public final byte[] md5_hash;
    
    public SnapshotHash(byte[] sha1_hash, byte[] md5_hash) {
      if (sha1_hash == null || md5_hash == null) {
        throw new IllegalArgumentException();
      }
      
      this.sha1_hash = sha1_hash;
      this.md5_hash = md5_hash;
    }
    
    @Override
    public boolean equals(Object other) {
      return (other instanceof SnapshotHash) &&
             Arrays.equals(((SnapshotHash)other).sha1_hash, sha1_hash) &&
             Arrays.equals(((SnapshotHash)other).md5_hash, md5_hash);
    }
    
    @Override
    public String toString() {
      return String.format("SnapShotHash<sha1=%s, md5=%s>",
                           Arrays.toString(sha1_hash),
                           Arrays.toString(md5_hash));
    }
  }
  
  private final ImmutableMap<String, SnapshotHash> state;
  
  private Snapshot(ImmutableMap<String, SnapshotHash> state) {
    this.state = state;
  }
  
  public boolean equals(Object o) {
    if (!(o instanceof Snapshot)) {
      return false;
    }
    
    Snapshot other = (Snapshot)o;
    
    for (String key : state.keySet()) {
      if (!other.state.containsKey(key)) {
        return false;
      }
      
      if (!state.get(key).equals(other.state.get(key))) {
        return false;
      }
    }
    
    for (String key : other.state.keySet()) {
      if (!state.containsKey(key)) {
        return false;
      }
    }
    
    return true;
  }
  
  public static Snapshot capture(List<String> prefixes) throws Exception {
    Set<ClassInfo> classes = ClassPath.from(ClassLoader.getSystemClassLoader()).getAllClasses();
    Map<String, SnapshotHash> state = new HashMap<>();
    
    for (ClassInfo clazz : classes) {
      boolean isReloadable = false;
      
      for (String prefix : prefixes) {
        if (clazz.getName().startsWith(prefix)) {
          isReloadable = true;
          break;
        }
      }
      
      if (!isReloadable) {
        continue;
      }
      
      InputStream bytecode = ClassLoader.getSystemClassLoader().getResourceAsStream(clazz.getResourceName());
      MessageDigest sha1 = MessageDigest.getInstance("SHA1");
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      
      byte[] buffer = new byte[1];
      int read;
      while ((read = bytecode.read(buffer)) != -1) {
        if (read > 0) {
          sha1.update(buffer);
          md5.update(buffer);
        }
      }
      
      state.put(clazz.getName(), new SnapshotHash(sha1.digest(), md5.digest()));      
    }
    
    return new Snapshot(ImmutableMap.copyOf(state));
  }
}
