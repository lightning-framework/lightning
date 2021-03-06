package lightning.users;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import lightning.groups.Group;
import lightning.groups.Groups;
import lightning.groups.Groups.GroupsException;
import lightning.mvc.ObjectParam;
import lightning.users.Users.UsersDriver;
import lightning.users.Users.UsersException;
import lightning.util.Time;

import com.google.common.collect.ImmutableList;

/**
 * Provides an API for interacting with a single user in the database.
 * Has setters and getters for well-defined fields.
 * Has setters and getters for arbitrary properties (can store any Serializable object).
 * 
 * Mutations to a User are in-memory; they become permanent only after invoking save() which pushes
 * the changes to the database in a batch.
 * 
 * Example:
 * User user = Users.getById(1);
 * System.out.println("Changing password for:" + user.getUserName());
 * user.setPassword("new_password");
 * user.save();
 */
public final class User {
  private UsersDriver driver;
  private Groups groups;
  private long id;
  private boolean isDirty;
  private String email;
  private String userName;
  private String encryptedPassword;
  private long banExpiry;
  private Map<String, Object> properties;
  private Set<Long> privileges;
  private Set<Long> effectivePrivileges; // Includes from groups.
  private Set<Long> privilegesToAdd;
  private Set<Long> privilegesToRemove;
  private boolean emailIsVerified;
  public String token;
  
  public User(UsersDriver driver, Groups groups, long id, String userName, String email, String encryptedPassword, String token, long banExpiry, boolean emailIsVerified, Map<String, Object> properties) {
    isDirty = false;
    this.driver = driver;
    this.id = id;
    this.userName = userName;
    this.email = email;
    this.encryptedPassword = encryptedPassword;
    this.banExpiry = banExpiry;
    this.properties = new HashMap<>(properties);
    this.privileges = null;
    this.token = token;
    this.emailIsVerified = emailIsVerified;
  }
  
  public boolean emailIsVerified() {
    return emailIsVerified;
  }
  
  public void setEmailIsVerified(boolean emailIsVerified) {
    this.emailIsVerified = emailIsVerified;
    isDirty = true;
  }
  
  public String getToken() {
    return token;
  }
  
  public void setToken(String token) {
    this.token = token;
    isDirty = true;
  }
  
  public long getId() {
    return id;
  }
  
  public String getEmail() {
    return email;
  }
  
  public String getUserName() {
    return userName;
  }
  
  public void setEmail(String email) {
    this.email = email;
    isDirty = true;
  }
  
  public void setUserName(String userName) {
    this.userName = userName;
    isDirty = true;
  }
  
  public String getEncryptedPassword() {
    return encryptedPassword;
  }
  
  public boolean isBanned() {
    return Time.now() <= banExpiry;
  }
  
  public long getBanExpiry() {
    return banExpiry;
  }
  
  public void banUntil(long timestamp) {
    this.banExpiry = timestamp;
    isDirty = true;
  }
  
  public void banForever() {
    this.banExpiry = Long.MAX_VALUE;
    isDirty = true;
  }
  
  public Map<String, Object> __getPropertyMap() {
    return Collections.unmodifiableMap(this.properties);
  }
  
  public void setPassword(String plaintextPassword) {
    encryptedPassword = Users.hashPassword(plaintextPassword);
    isDirty = true;
  }
  
  public boolean checkPassword(String plaintextPassword) {
    return Users.checkPassword(encryptedPassword, plaintextPassword);
  }
  
  public Set<String> getProperties() {
    return Collections.unmodifiableSet(properties.keySet());
  }
  
  public boolean hasProperty(String key) {
    return properties.containsKey(key);
  }
  
  public void deleteProperty(String key) {
    properties.remove(key);
    isDirty = true;
  }
  
  public void setProperty(String key, Object value) {
    if (!(value instanceof Serializable)) { // Note: also prevents nulls.
      throw new IllegalArgumentException("Value must implement Serializable.");
    }
    
    properties.put(key, value);
    isDirty = true;
  }
  
  public ObjectParam getProperty(String key) {
    if (!properties.containsKey(key)) {
      return new ObjectParam(null);
    }
    
    return new ObjectParam(properties.get(key));
  }
  
  public long getPropertyCount() {
    return properties.size();
  }
  
  
  public boolean hasPrivilege(long pid) throws GroupsException, UsersException {
    return hasPrivileges(ImmutableList.of(pid));
  }
  
  public boolean hasPrivilegeOnUser(long pid) throws GroupsException, UsersException {
    return hasPrivilegesOnUser(ImmutableList.of(pid));
  }
  
  public void grantPrivilege(long pid) throws GroupsException, UsersException {
    grantPrivileges(ImmutableList.of(pid));
  }
  
  public void revokePrivilege(long pid) throws GroupsException, UsersException {
    revokePrivileges(ImmutableList.of(pid));
  }
  
  public Set<Long> getPrivilegeSet() throws GroupsException, UsersException {
    HashSet<Long> set = new HashSet<>();
    
    for (Long i : getPrivileges()) {
      set.add(i);
    }
    
    return set;
  }
  
  public Map<Long, Boolean> getPrivilegeMap() throws GroupsException, UsersException {
    Map<Long, Boolean> set = new HashMap<>();
    
    for (Long i : getPrivileges()) {
      set.put(i, true);
    }
    
    return set;
  }
  
  public Iterable<Long> getPrivileges() throws GroupsException, UsersException {
    fetchPrivilegesIfNotExist();
    return this.effectivePrivileges;
  }
  
  public Iterable<Long> getPrivilegesOnUser() throws GroupsException, UsersException {
    fetchPrivilegesIfNotExist();
    return this.privileges;
  }
  
  public boolean hasPrivileges(List<Long> pids) throws GroupsException, UsersException {
    fetchPrivilegesIfNotExist();
    return this.effectivePrivileges.containsAll(pids);
  }
  
  public boolean hasPrivilegesOnUser(List<Long> pids) throws GroupsException, UsersException {
    fetchPrivilegesIfNotExist();
    return this.privileges.containsAll(pids);
  }
  
  public void grantPrivileges(List<Long> pids) throws GroupsException, UsersException {
    fetchPrivilegesIfNotExist();
    
    if (this.privilegesToAdd == null) {
      this.privilegesToAdd = new TreeSet<>();
    }
    
    this.privilegesToAdd.addAll(pids);
    isDirty = true;
  }
  
  public void revokePrivileges(List<Long> pids) throws GroupsException, UsersException {
    fetchPrivilegesIfNotExist();
    
    if (this.privilegesToRemove == null) {
      this.privilegesToRemove = new TreeSet<>();
    }
    
    this.privilegesToRemove.addAll(pids);
    isDirty = true;
  }
  
  public Iterable<Group> getGroups() throws GroupsException {
    return groups.getAllForUser(id);
  }
  
  private void fetchPrivilegesIfNotExist() throws GroupsException, UsersException {
    this.effectivePrivileges = groups.getGroupPrivilegesForUser(id);
    
    try {
      this.privileges = driver.getPrivileges(id);
    } catch (Exception e) {
      throw new UsersException(e);
    }
    
    this.effectivePrivileges.addAll(privileges);
  }
  
  public void save() throws UsersException {
    if (!isDirty) {
      return;
    }
    
    try {
      driver.save(this, privilegesToAdd, privilegesToRemove);
      isDirty = false;
    } catch (Exception e) {
      throw new UsersException(e);
    }
  }
}
