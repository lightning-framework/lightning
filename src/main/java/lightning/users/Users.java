package lightning.users;

import java.util.Set;

import lightning.crypt.BCrypt;
import lightning.groups.Groups;

/**
 * Provides an API for interacting with the set of all users in the application.
 * Utilize this API to obtain references to User objects from the database,
 * to create new User objects, and to delete existing User objects.
 * 
 * Example Usage:
 * - User user = Users.getByName("myuser");
 * - Can now manipulate user (see methods on User).
 */
public final class Users {
  private UsersDriver sharedDriver;
  private Groups groups;
    
  public static interface UsersDriver {
    public void save(User user, Set<Long> addPrivileges, Set<Long> removePrivileges) throws Exception;
    public Set<Long> getPrivileges(long userId) throws Exception;
    public User getUser(long userId) throws Exception;
    public User getUserByName(String name) throws Exception;
    public User getUserByEmail(String email) throws Exception;
    public User getUserByToken(String token) throws Exception;
    public Iterable<User> getAll() throws Exception;
    public User create(String userName, String email, String plaintextPassword) throws Exception;
    public void delete(long userId) throws Exception;
    public void recordLogin(User user) throws Exception;
  }
  
  public static class UsersException extends Exception {
    private static final long serialVersionUID = 1L;

    public UsersException(Exception e) {
      super(e);
    }
    
    public UsersException(String message) {
      super(message);
    }
  }
  
  public Users(UsersDriver usersDriver, Groups groups) {
    if (usersDriver == null) {
      throw new IllegalArgumentException("Must provide a driver.");
    }
    sharedDriver = usersDriver;
    this.groups = groups;
  }
  
  public static String hashPassword(String plaintextPassword) {
    return BCrypt.hashpw(plaintextPassword, BCrypt.gensalt(10));
  }
  
  public static boolean checkPassword(String hash, String plaintextPassword) {
    return BCrypt.checkpw(plaintextPassword, hash);
  }
  
  public User getById(long id) throws UsersException {
    try {
      return sharedDriver.getUser(id);
    } catch (Exception e) {
      throw new UsersException(e);
    }
  }
  
  public User getByName(String userName) throws UsersException {
    try {
      return sharedDriver.getUserByName(userName);
    } catch (Exception e) {
      throw new UsersException(e);
    }
  }
  
  public User getByToken(String token) throws UsersException {
    try {
      return sharedDriver.getUserByToken(token);
    } catch (Exception e) {
      throw new UsersException(e);
    }
  }
  
  public User getByEmail(String email) throws UsersException {
    try {
      return sharedDriver.getUserByEmail(email);
    } catch (Exception e) {
      throw new UsersException(e);
    }
  }
  
  public Iterable<User> getAll() throws UsersException {
    try {
      return sharedDriver.getAll();
    } catch (Exception e) {
      throw new UsersException(e);
    }
  }
  
  public User create(String userName, String email, String plaintextPassword) throws UsersException {
    if (!isValidEmail(email)) {
      throw new UsersException("Email not valid.");
    }
    
    if (!isValidUserName(userName)) {
      throw new UsersException("Username not valid.");
    }
    
    if (!isValidPassword(userName, plaintextPassword)) {
      throw new UsersException("Password not valid.");
    }
    
    try {
      return sharedDriver.create(userName, email, plaintextPassword);
    } catch (Exception e) {
      throw new UsersException(e);
    }
  }
  
  public void delete(User user) throws UsersException {
    try {
      groups.deleteDataForUser(user.getId());
      sharedDriver.delete(user.getId());
    } catch (Exception e) {
      throw new UsersException(e);
    }
  }
  
  public void recordLogin(User user) throws UsersException {
    try {
      sharedDriver.recordLogin(user);
    } catch (Exception e) {
      throw new UsersException(e);
    }
  }
  
  public boolean isValidUserName(String userName) {
    return userName != null && userName.length() > 0; // TODO(mschurr): Additional validation;
  }
  
  public boolean isValidPassword(String userName, String password) {
    return password != null && password.length() > 0; // TODO(mschurr): Additional validation;
  }
  
  public boolean isValidEmail(String email) {
    return email != null && email.length() > 0; // TODO(mschurr): Additional validation;
  }
}
