package lightning.groups.drivers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import lightning.db.MySQLDatabase;
import lightning.db.MySQLDatabaseProvider;
import lightning.db.NamedPreparedStatement;
import lightning.groups.Group;
import lightning.groups.Groups.GroupsDriver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * A groups driver backed by MySQL.
 */
public class MySQLGroupDriver implements GroupsDriver {
  private final MySQLDatabaseProvider provider;
  
  public MySQLGroupDriver(MySQLDatabaseProvider provider) {
    this.provider = provider;
  }
  
  private Group nextGroup(PreparedStatement query, ResultSet result) throws SQLException {
    if (!result.next()) {
      result.close();
      query.close();
      return null;
    }
    
    Group group = new Group(this, result.getLong("id"), result.getString("name"));
    result.close();
    query.close();
    return group;
  }

  @Override
  public Group get(long id) throws SQLException {
    try (MySQLDatabase db = provider.getDatabase()) {
      PreparedStatement query = db.prepare("SELECT * FROM groups WHERE id = ?;", ImmutableList.of(id));
      ResultSet result = query.executeQuery();
      return nextGroup(query, result);
    }
  }

  @Override
  public Group getByName(String name) throws SQLException {
    try (MySQLDatabase db = provider.getDatabase()) {
      PreparedStatement query = db.prepare("SELECT * FROM groups WHERE name = ?;", ImmutableList.of(name));
      ResultSet result = query.executeQuery();
      return nextGroup(query, result);
    }
  }

  @Override
  public Group create(String name) throws SQLException {
    try (MySQLDatabase db = provider.getDatabase()) {
      NamedPreparedStatement query = db.prepareInsert("groups", ImmutableMap.of("name", name));
      query.executeUpdate();
      Group group = new Group(this, query.getInsertionId(), name);
      query.close();
      return group;
    }
  }

  @Override
  public void delete(long id) throws Exception {
    try (MySQLDatabase db = provider.getDatabase()) {
      NamedPreparedStatement query = db.prepare("DELETE FROM groups WHERE id = :id;");
      query.setLong("id", id);
      query.executeUpdate();
      query.close();
    }
  }

  @Override
  public void setName(long id, String name) throws Exception {
    throw new Exception("Unimplemented!"); // TODO: Implement
  }

  @Override
  public boolean hasPrivileges(long id, List<Long> privileges) throws Exception {
    throw new Exception("Unimplemented!"); // TODO: Implement
  }

  @Override
  public boolean grantPrivileges(long id, List<Long> privileges) throws Exception {
    throw new Exception("Unimplemented!"); // TODO: Implement
  }

  @Override
  public boolean revokePrivileges(long id, List<Long> privileges) throws Exception {
    throw new Exception("Unimplemented!"); // TODO: Implement
  }

  @Override
  public Set<Long> getPrivileges(long id) throws Exception {
    throw new Exception("Unimplemented!"); // TODO: Implement
  }

  @Override
  public Iterable<Long> getUsersInGroup(long id) throws Exception {
    throw new Exception("Unimplemented!"); // TODO: Implement
  }

  @Override
  public boolean groupHasUsers(long id, List<Long> userIds) throws Exception {
    throw new Exception("Unimplemented!"); // TODO: Implement
  }

  @Override
  public boolean addUsersToGroup(long id, List<Long> userIds) throws Exception {
    throw new Exception("Unimplemented!"); // TODO: Implement
  }

  @Override
  public boolean removeUsersFromGroup(long id, List<Long> userIds) throws Exception {
    throw new Exception("Unimplemented!"); // TODO: Implement
  }

  @Override
  public Iterable<Group> getAll() throws Exception {
    throw new Exception("Unimplemented!"); // TODO: Implement
  }

  @Override
  public Iterable<Group> getAllForUser(long id) throws Exception {
    throw new Exception("Unimplemented!"); // TODO: Implement
  }

  @Override
  public Set<Long> getGroupPrivilegesForUser(long userId) throws Exception {
    return new TreeSet<>(); // TODO: Implement
  }

  @Override
  public void deleteDataForUser(long userId) throws Exception {
    // TODO: Implement
  }
}
