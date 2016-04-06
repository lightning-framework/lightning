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
  public static class GroupsException extends Exception {
    private static final long serialVersionUID = 1L;

    public GroupsException(Exception e) {
      super(e);
    }
    
    public GroupsException(String message) {
      super(message);
    }
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
  
  private final GroupsDriver sharedDriver;
  
  public Groups(GroupsDriver driver) {
    if (driver == null) {
      throw new IllegalArgumentException("Must provide a driver.");
    }
    sharedDriver = driver;
  }
  
  public Group get(long id) throws GroupsException {
    try {
      return sharedDriver.get(id);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public Group getByName(String name) throws GroupsException {
    try {
      return sharedDriver.getByName(name);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public Iterable<Group> getAll() throws GroupsException {
    try {
      return sharedDriver.getAll();
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public Iterable<Group> getAllForUser(long userId) throws GroupsException {
    try {
      return sharedDriver.getAllForUser(userId);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public Group create(String name) throws GroupsException {
    try {
      return sharedDriver.create(name);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public void delete(Group group) throws GroupsException {
    try {
      sharedDriver.delete(group.getId());
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public Set<Long> getGroupPrivilegesForUser(long userId) throws GroupsException {
    try {
      return sharedDriver.getGroupPrivilegesForUser(userId);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public void deleteDataForUser(long userId) throws GroupsException {
    try {
      sharedDriver.deleteDataForUser(userId);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
}
