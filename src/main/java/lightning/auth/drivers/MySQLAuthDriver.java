package lightning.auth.drivers;

import java.sql.ResultSet;
import java.sql.SQLException;

import lightning.auth.AuthAttempt;
import lightning.auth.AuthCountAndTimestamp;
import lightning.auth.AuthToken;
import lightning.auth.Auth.AuthDriver;
import lightning.db.MySQLDatabase;
import lightning.db.MySQLDatabaseProvider;
import lightning.db.NamedPreparedStatement;

import com.google.common.collect.ImmutableMap;

/**
 * An authentication driver that stores information in a MySQL database.
 * TODO: Automatic cleanup of expired data.
 */
public class MySQLAuthDriver implements AuthDriver {
  public final MySQLDatabaseProvider provider;
  
  public MySQLAuthDriver(MySQLDatabaseProvider provider) {
    this.provider = provider;
  }
    
  @Override
  public void terminateAllSessionsForUser(long userId) throws SQLException {
    try (MySQLDatabase db = provider.getDatabase()) {
      db.prepare("DELETE FROM auth_tokens WHERE user_id = :user_id;",
          ImmutableMap.of("user_id", userId)).executeUpdateAndClose();
      db.prepare("DELETE FROM auth_sessions WHERE user_id = :user_id;",
          ImmutableMap.of("user_id", userId)).executeUpdateAndClose();
    }
  }

  @Override
  public void terminateAllSessionsForUserExcepting(long userId, String hashedIdToIgnore)
      throws SQLException {
    try (MySQLDatabase db = provider.getDatabase()) {
      db.prepare("DELETE FROM auth_tokens WHERE user_id = :user_id AND token_hash != :token_hash;",
          ImmutableMap.of("user_id", userId, "token_hash", hashedIdToIgnore)).executeUpdateAndClose();
      db.prepare("DELETE FROM auth_sessions WHERE user_id = :user_id AND token_hash != :token_hash;",
          ImmutableMap.of("user_id", userId, "token_hash", hashedIdToIgnore)).executeUpdateAndClose();
    }
  }
  
  @Override
  public AuthCountAndTimestamp getFailedLoginCountSince(long userId, long time) throws SQLException {
    try (MySQLDatabase db = provider.getDatabase()) {
      NamedPreparedStatement query = db.prepare("SELECT COUNT(*), MAX(time) FROM auth_attempts WHERE user_id = :user_id AND time >= :time AND successful = 0;", 
          ImmutableMap.of("user_id", userId, "time", time));
      ResultSet result = query.executeQuery();
      if (!result.next())
        throw new SQLException("No rows returned for count query.");
      AuthCountAndTimestamp data = AuthCountAndTimestamp.create(result.getLong(1), result.getLong(2));
      result.close();
      query.close();
      return data;
    }
  }

  @Override
  public long getSuccessfulLoginCountSince(long userId, long time) throws SQLException {
    try (MySQLDatabase db = provider.getDatabase()) {
      return db.prepare("SELECT COUNT(*) FROM auth_attempts WHERE user_id = :user_id AND time >= :time AND successful = 1;", 
          ImmutableMap.of("user_id", userId, "time", time)).executeCount();
    }
  }

  @Override
  public long getFraudulentLoginCountSince(long userId, long time) throws SQLException {
    try (MySQLDatabase db = provider.getDatabase()) {
      return db.prepare("SELECT COUNT(*) FROM auth_attempts WHERE user_id = :user_id AND time >= :time AND fraudulent = 1;", 
          ImmutableMap.of("user_id", userId, "time", time)).executeCount();
    }
  }

  @Override
  public AuthCountAndTimestamp getFailedLoginCountSince(String ip, long time) throws SQLException {
    try (MySQLDatabase db = provider.getDatabase()) {
      NamedPreparedStatement query = db.prepare("SELECT COUNT(*), MAX(time) FROM auth_attempts WHERE ipaddress = :ip AND time >= :time AND successful = 0;", 
          ImmutableMap.of("ip", ip, "time", time));
      ResultSet result = query.executeQuery();
      if (!result.next())
        throw new SQLException("No rows returned for count query.");
      AuthCountAndTimestamp data = AuthCountAndTimestamp.create(result.getLong(1), result.getLong(2));
      result.close();
      query.close();
      return data;
    }
  }

  @Override
  public long getSuccessfulLoginCountSince(String ip, long time) throws SQLException {
    try (MySQLDatabase db = provider.getDatabase()) {
      return db.prepare("SELECT COUNT(*) FROM auth_attempts WHERE ipaddress = :ip AND time >= :time AND successful = 1;", 
          ImmutableMap.of("ip", ip, "time", time)).executeCount();
    }
  }

  @Override
  public long getFraudulentLoginCountSince(String ip, long time) throws SQLException {
    try (MySQLDatabase db = provider.getDatabase()) {
      return db.prepare("SELECT COUNT(*) FROM auth_attempts WHERE ipaddress = :ip AND time >= :time AND fraudulent = 1;", 
          ImmutableMap.of("ip", ip, "time", time)).executeCount();
    }
  }

  @Override
  public void saveAuthAttempt(AuthAttempt attempt) throws Exception {
    try (MySQLDatabase db = provider.getDatabase()) {
      if (attempt.userId <= 0) {
        db.prepareInsert("auth_attempts", 
            new ImmutableMap.Builder<String, Object>()
              .put("ipaddress", attempt.ip)
              .put("time", attempt.timestamp)
              .put("successful", attempt.wasSuccessful)
              .put("fraudulent", attempt.wasFraudulent)
              .build()
            ).executeUpdateAndClose();
      } else {
        db.prepareInsert("auth_attempts", 
            new ImmutableMap.Builder<String, Object>()
              .put("ipaddress", attempt.ip)
              .put("user_id", attempt.userId)
              .put("time", attempt.timestamp)
              .put("successful", attempt.wasSuccessful)
              .put("fraudulent", attempt.wasFraudulent)
              .build()
            ).executeUpdateAndClose();
      }
    }
  }
  
  private void updateAuthToken(String table, AuthToken token) throws SQLException {
    try (MySQLDatabase db = provider.getDatabase()) {
      db.prepareReplace(table, 
          ImmutableMap.of(
              "user_id", token.userId, 
              "expires", token.expireTime, 
              "token_hash", token.hashedToken)
          ).executeUpdateAndClose();
    }
  }
  
  private void insertAuthToken(String table, AuthToken token) throws SQLException {
    try (MySQLDatabase db = provider.getDatabase()) {
      db.prepareInsert(table, 
          ImmutableMap.of(
              "user_id", token.userId, 
              "expires", token.expireTime, 
              "token_hash", token.hashedToken)
          ).executeUpdateAndClose();
    }
  }

  private void deleteAuthToken(String table, AuthToken token) throws SQLException {
    try (MySQLDatabase db = provider.getDatabase()) {
      db.prepare("DELETE FROM " + table + " WHERE token_hash = :token_hash",
          ImmutableMap.of("token_hash", token.hashedToken))
          .executeUpdateAndClose();
    }
  }
  
  private AuthToken getAuthToken(String table, String hashedId) throws SQLException {
    try (MySQLDatabase db = provider.getDatabase()) {
      NamedPreparedStatement query = db.prepare("SELECT * FROM " + table + " WHERE token_hash = :token_hash",
          ImmutableMap.of("token_hash", hashedId));
      ResultSet result = query.executeQuery();
      
      if (!result.next()) {
        return null;
      }
      
      AuthToken token = new AuthToken(
          result.getString("token_hash"),
          result.getLong("user_id"),
          result.getLong("expires"));
      
      result.close();
      query.close();
      return token;
    }
  }

  @Override
  public void updateSessionAuthToken(AuthToken token) throws SQLException {
    updateAuthToken("auth_sessions", token);
  }

  @Override
  public void insertSessionAuthToken(AuthToken token) throws SQLException {
    insertAuthToken("auth_sessions", token);
  }

  @Override
  public void deleteSessionAuthToken(AuthToken token) throws SQLException {
    deleteAuthToken("auth_sessions", token);
  }

  @Override
  public AuthToken getSessionAuthToken(String hashedId) throws SQLException {
    return getAuthToken("auth_sessions", hashedId);
  }

  @Override
  public void updatePersistentAuthToken(AuthToken token) throws SQLException {
    updateAuthToken("auth_tokens", token);
  }

  @Override
  public void insertPersistentAuthToken(AuthToken token) throws SQLException {
    insertAuthToken("auth_tokens", token);
  }

  @Override
  public void deletePersistentAuthToken(AuthToken token) throws SQLException {
    deleteAuthToken("auth_tokens", token);
  }

  @Override
  public AuthToken getPersistentAuthToken(String hashedId) throws SQLException {
    return getAuthToken("auth_tokens", hashedId);
  }
}
