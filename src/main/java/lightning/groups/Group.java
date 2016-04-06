package lightning.groups;

import java.util.List;

import lightning.groups.Groups.GroupsDriver;
import lightning.groups.Groups.GroupsException;
import lightning.users.User;
import lightning.users.Users;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;


/**
 * A group is simply a named set of users.
 * 
 * A group may also have a set of permissions attached to it. These permissions are inherited
 * by all members.
 * 
 * Mutations to Group objects are persistent; they are immediately pushed to the database.
 * 
 * Example Usage:
 * Group group = groups.create("my fancy group");
 * User user = Users.getByName("myuser");
 * if (!group.hasUser(user))
 *   group.addUser(user);
 * group.grantPrivilege(Privilege.ADMIN);
 */
public final class Group {
  private final long id;
  private String name;
  private GroupsDriver driver;
  
  public Group(GroupsDriver driver, long id, String name) {
    this.driver = driver;
    this.id = id;
    this.name = name;
  }
  
  public long getId() {
    return id;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String newName) throws GroupsException {
    try {
      driver.setName(id, newName);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
    
    this.name = newName;
  }
  
  public Iterable<Long> getPrivileges() throws GroupsException {
    try {
      return driver.getPrivileges(id);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public boolean hasPrivilege(long pid) throws GroupsException {
    return hasPrivileges(ImmutableList.of(pid));
  }
  
  public void grantPrivilege(long pid) throws GroupsException {
    grantPrivileges(ImmutableList.of(pid));
  }
  
  public void revokePrivilege(long pid) throws GroupsException {
    revokePrivileges(ImmutableList.of(pid));
  }
  
  public boolean hasPrivileges(List<Long> pids) throws GroupsException {
    try {
      return driver.hasPrivileges(id, pids);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public void grantPrivileges(List<Long> pids) throws GroupsException {
    try {
      driver.grantPrivileges(id, pids);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public void revokePrivileges(List<Long> pids) throws GroupsException {
    try {
      driver.revokePrivileges(id, pids);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public void addUser(User user) throws GroupsException {
    addUsers(ImmutableList.of(user));
  }
  
  public void removeUser(User user) throws GroupsException {
    removeUsers(ImmutableList.of(user));
  }
  
  public boolean hasUser(User user) throws GroupsException {
    return hasUsers(ImmutableList.of(user));
  }
  
  public Iterable<Long> getUserIds() throws GroupsException {
    try {
      return driver.getUsersInGroup(id);
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  /**
   * @return An iterator over users which MAY throw runtime exceptions.
   * @throws GroupsException On I/O failure.
   */
  public Iterable<User> getUsers(Users users) throws GroupsException {
    try {
      return Iterables.transform(driver.getUsersInGroup(id), (gid) -> {
        try {
          return users.getById(gid);
        } catch (Exception e) {
          // Gross, but HTTP servlet will catch it. Not much else to do since Java iterators
          // do not allow exceptions to be thrown in next().
          throw new RuntimeException(e); 
        }
      });
    } catch (Exception e) {
      throw new GroupsException(e);
    } 
  }
  
  public boolean hasUsers(List<User> users) throws GroupsException {
    try {
      return driver.groupHasUsers(id, Lists.transform(users, user -> user.getId()));
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public void addUsers(List<User> users) throws GroupsException {
    try {
      driver.addUsersToGroup(id, Lists.transform(users, user -> user.getId()));
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
  
  public void removeUsers(List<User> users) throws GroupsException {
    try {
      driver.removeUsersFromGroup(id, Lists.transform(users, user -> user.getId()));
    } catch (Exception e) {
      throw new GroupsException(e);
    }
  }
}
