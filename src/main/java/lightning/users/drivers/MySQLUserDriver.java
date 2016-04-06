package lightning.users.drivers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import lightning.auth.Auth;
import lightning.db.MySQLDatabase;
import lightning.db.MySQLDatabaseProvider;
import lightning.db.NamedPreparedStatement;
import lightning.db.ResultSets;
import lightning.groups.Groups;
import lightning.users.User;
import lightning.users.Users;
import lightning.users.Users.UsersDriver;

import org.eclipse.jetty.util.ClassLoadingObjectInputStream;

import com.google.common.collect.ImmutableList;

/**
 * An implementation of a users driver backed by MySQL.
 */
public class MySQLUserDriver implements UsersDriver {
  private final MySQLDatabaseProvider provider;
  private final Groups groups;
  
  public MySQLUserDriver(MySQLDatabaseProvider provider, Groups groups) {
    this.provider = provider;
    this.groups = groups;
  }
  
  public User nextUser(ResultSet result) throws ClassNotFoundException, SQLException, IOException {
    if (!result.next()) {
      return null;
    }
    
    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) getBlob(result, "properties");
    
    if (properties == null) {
      properties = new TreeMap<>();
    }
    
    return new User(this, 
        groups,
        result.getLong("id"), 
        result.getString("username"), 
        result.getString("email"), 
        result.getString("encrypted_password"), 
        result.getString("secret_key"),
        result.getLong("banned_until"), 
        ResultSets.getBoolean(result, "email_verified"),
        properties);
  }
  
  private static void setBlob(PreparedStatement statement, int index, Object object) throws SQLException, IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(object);
    oos.flush();
    byte[] bytes = baos.toByteArray();
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    statement.setBinaryStream(index, bais, bytes.length);
  }
  
  private static Object getBlob(ResultSet result, String column) throws SQLException, IOException, ClassNotFoundException {
    Blob blob = result.getBlob(column);
    if (result.wasNull())
      return null;
    InputStream is = blob.getBinaryStream();
    ClassLoadingObjectInputStream ois = new ClassLoadingObjectInputStream(is);
    Object o = ois.readObject();
    ois.close();
    return o;
  }

  @Override
  public User getUser(long userId) throws SQLException, ClassNotFoundException, IOException {
    try (MySQLDatabase db = provider.getDatabase()) {
      PreparedStatement query = db.prepare("SELECT * FROM users WHERE id = ?;",
          ImmutableList.of(userId));
      ResultSet result = query.executeQuery();
      User user = nextUser(result);
      result.close();
      query.close();
      return user;
    }
  }

  @Override
  public User getUserByName(String name) throws Exception {
    try (MySQLDatabase db = provider.getDatabase()) {
      PreparedStatement query = db.prepare("SELECT * FROM users WHERE username = ?;",
          ImmutableList.of(name));
      ResultSet result = query.executeQuery();
      User user = nextUser(result);
      result.close();
      query.close();
      return user;
    }
  }

  @Override
  public User getUserByEmail(String email) throws Exception {
    try (MySQLDatabase db = provider.getDatabase()) {
      PreparedStatement query = db.prepare("SELECT * FROM users WHERE email = ?;",
          ImmutableList.of(email));
      ResultSet result = query.executeQuery();
      User user = nextUser(result);
      result.close();
      query.close();
      return user;
    }
  }


  @Override
  public User getUserByToken(String token) throws Exception {
    try (MySQLDatabase db = provider.getDatabase()) {
      PreparedStatement query = db.prepare("SELECT * FROM users WHERE secret_key = ?;",
          ImmutableList.of(token));
      ResultSet result = query.executeQuery();
      User user = nextUser(result);
      result.close();
      query.close();
      return user;
    }
  }
  
  @Override
  public void save(User user, Set<Long> addPrivileges, Set<Long> removePrivileges) throws Exception {
    try (MySQLDatabase db = provider.getDatabase()) {
      // TODO Add support for privileges.
      /*NamedPreparedStatement query = db.prepare(""
          + "REPLACE INTO users (id, username, email, encrypted_password, secret_key, properties, banned_until) "
          + "VALUES (:id, :username, :email, :encrypted_password, :secret_key, :properties, :banned_until);");*/
      NamedPreparedStatement query = db.prepare(""
          + "UPDATE users "
          + "SET "
          + "  username = :username, "
          + "  email = :email, "
          + "  encrypted_password = :encrypted_password, "
          + "  secret_key = :secret_key, "
          + "  properties = :properties, "
          + "  banned_until = :banned_until, "
          + "  email_verified = :email_verified "
          + "WHERE id = :id;");
      query.setLong("id", user.getId());
      query.setString("username", user.getUserName());
      query.setString("email", user.getEmail());
      query.setString("encrypted_password", user.getEncryptedPassword());
      query.setString("secret_key", user.getToken());
      query.setInt("email_verified", ResultSets.encodeBoolean(user.emailIsVerified()));
      query.set("properties", (stmt, i) -> {
        try {
          setBlob(stmt, i, user.__getPropertyMap());
        } catch (IOException e) {
          throw new SQLException(e);
        }
      });
      query.setLong("banned_until", user.getBanExpiry());
      query.executeUpdate();
      query.close();
    }
  }

  @Override
  public Set<Long> getPrivileges(long userId) throws Exception {
    return new TreeSet<>(); // TODO: implement
  }

  @Override
  public Iterable<User> getAll() throws Exception {
    throw new Exception("Not implemented."); // TODO: implement
  }

  @Override
  public User create(String userName, String email, String plaintextPassword) throws SQLException {
    try (MySQLDatabase db = provider.getDatabase()) {
      NamedPreparedStatement query = db.prepare(""
          + "INSERT INTO users (username, email, encrypted_password, secret_key, properties, banned_until, email_verified) "
          + "VALUES (:username, :email, :encrypted_password, :secret_key, :properties, :banned_until, 0);");
      query.setString("username", userName);
      query.setString("email", email);
      String encryptedPassword = Users.hashPassword(plaintextPassword);
      query.setString("encrypted_password", encryptedPassword);
      String token = Auth.generateToken(); // TODO: Use something else.
      query.setString("secret_key", token);
      final Map<String, Object> properties = new TreeMap<>();
      query.set("properties", (stmt, i) -> {
        try {
          setBlob(stmt, i, properties);
        } catch (IOException e) {
          throw new SQLException(e);
        }
      });
      query.setLong("banned_until", 0);
      query.executeUpdate();
      long id = query.getInsertionId();
      query.close();
      
      return new User(this, groups, id, userName, email, encryptedPassword, token, 0, false, properties);
    }
  }

  @Override
  public void delete(long userId) throws SQLException {
    try (MySQLDatabase db = provider.getDatabase()) {
      PreparedStatement query;
      query = db.prepare("DELETE FROM user_privileges WHERE user_id = ?;", ImmutableList.of(userId));
      query.executeUpdate();
      query.close();
      
      query = db.prepare("DELETE FROM users WHERE id = ?;", ImmutableList.of(userId));
      query.executeUpdate();
      query.close();
    }
  }

  @Override
  public void recordLogin(User user) {}
}
