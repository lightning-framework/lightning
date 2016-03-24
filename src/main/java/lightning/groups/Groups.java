package lightning.groups;

import java.util.List;
import java.util.Set;

/**
 * Provides an API for interacting with the set of all Groups.
 * Utilize to fetch, create, and delete groups in the database.
 * 
 * Example Usage:
 * - Group group = Groups.getByName("my group");
 * - Perform mutations on group
 */
public final class Groups {
  private static GroupsDriver sharedDriver;
  
  public static class GroupsException extends Exception {
    private static final long serialVersionUID = 1L;

    public GroupsException(Exception e) {
      super(e);
    }
    
    public GroupsException(String message) {
      super(message);
    }
  }
  
  public static void setDriver(GroupsDriver driver) {
    sharedDriver = driver;
  }
  
  /**
   * Implementations must be thread-safe.
   */
  public static interface GroupsDriver {
    public Group get(long id) throws Exception;
    public Group getByName(String name) throws Exception;
    public Iterable<Group> getAll() throws Exception;
    public Iterable<Group> getAllForUser(long userId) throws Exception;
    public Group create(String name) throws Exception;
    public void delete(long id) throws Exception;
    public void setName(long id, String name) throws Exception;
    public boolean hasPrivileges(long id, List<Long> privileges) throws Exception;
    public boolean grantPrivileges(long id, List<Long> privileges) throws Exception;
    public boolean revokePrivileges(long id, List<Long> privileges) throws Exception;
    public Set<Long> getPrivileges(long id) throws Exception;
    public Iterable<Long> getUsersInGroup(long id) throws Exception;
    public boolean groupHasUsers(long id, List<Long> userIds) throws Exception;
    public boolean addUsersToGroup(long id, List<Long> userIds) throws Exception;
    public boolean removeUsersFromGroup(long id, List<Long> userIds) throws Exception;
    public Set<Long> getGroupPrivilegesForUser(long userId) throws Exception;
    public void deleteDataForUser(long userId) throws Exception;
  }
  
  private static void checkDriver() {
    if (sharedDriver == null) {
      throw new RuntimeException("Error: Must call setDriver() before using Groups.");
    }
  }
  
  public static Group get(long id) throws GroupsException {
    checkDriver();
    
    try {
      return sharedDriver.get(id);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public static Group getByName(String name) throws GroupsException {
    checkDriver();
    
    try {
      return sharedDriver.getByName(name);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public static Iterable<Group> getAll() throws GroupsException {
    checkDriver();
    
    try {
      return sharedDriver.getAll();
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public static Iterable<Group> getAllForUser(long userId) throws GroupsException {
    checkDriver();
    
    try {
      return sharedDriver.getAllForUser(userId);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public static Group create(String name) throws GroupsException {
    checkDriver();
    
    try {
      return sharedDriver.create(name);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public static void delete(Group group) throws GroupsException {
    checkDriver();
    
    try {
      sharedDriver.delete(group.getId());
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public static Set<Long> getGroupPrivilegesForUser(long userId) throws GroupsException {
    checkDriver();
    
    try {
      return sharedDriver.getGroupPrivilegesForUser(userId);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public static void deleteDataForUser(long userId) throws GroupsException {
    checkDriver();
    
    try {
      sharedDriver.deleteDataForUser(userId);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
}
