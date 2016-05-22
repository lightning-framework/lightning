package lightning.sessions.drivers;

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

import lightning.db.MySQLDatabase;
import lightning.db.MySQLDatabaseProvider;
import lightning.db.NamedPreparedStatement;
import lightning.sessions.Session.SessionDriverException;
import lightning.sessions.Session.SessionStorageDriver;
import lightning.util.Time;

import org.eclipse.jetty.util.ClassLoadingObjectInputStream;

/**
 * A session driver implementation that uses a MySQL database.
 * TODO: Implement automatic cleanup of old sessions.
 */
public class MySQLSessionDriver implements SessionStorageDriver {
  private final MySQLDatabaseProvider provider;
  
  public MySQLSessionDriver(MySQLDatabaseProvider provider) {
    this.provider = provider;
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
  public Map<String, Object> get(String hashedId) throws SessionDriverException {
    try (MySQLDatabase db = provider.getDatabase()) {
      NamedPreparedStatement query = db.prepare("SELECT * FROM sessions WHERE session_id = :id;");
      query.setString("id", hashedId);      
      ResultSet result = query.executeQuery();
      
      if (!result.next()) {
        result.close();
        query.close();
        return null;
      }
      
      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) getBlob(result, "data");
      if (data == null)
        data = new TreeMap<>();
      result.close();
      query.close();
      return data;
    } catch (SQLException | IOException | ClassNotFoundException e) {
      throw new SessionDriverException(e);
    }
  }

  @Override
  public void put(String hashedId, Map<String, Object> data, Set<String> changedKeys)
      throws SessionDriverException {
    try (MySQLDatabase db = provider.getDatabase()) {
      NamedPreparedStatement query = db.prepare("REPLACE INTO sessions (session_id, last_save, data) VALUES (:id, :time, :data);");
      query.setString("id", hashedId);
      query.setLong("time", Time.now());
      query.set("data", (stmt, i) -> {
        try {
          setBlob(stmt, i, data);
        } catch (IOException e) {
          throw new SQLException(e);
        }
      });
      
      query.executeUpdate();
      query.close();
    } catch (SQLException e) {
      throw new SessionDriverException(e);
    }
  }

  @Override
  public boolean has(String hashedId) throws SessionDriverException {
    try (MySQLDatabase db = provider.getDatabase()) {
      NamedPreparedStatement query = db.prepare("SELECT COUNT(*) FROM sessions WHERE session_id = :id;");
      query.setString("id", hashedId);
      ResultSet result = query.executeQuery();
      
      if (result.next()) {
        int count = result.getInt(1);
        result.close();
        query.close();
        return count > 0;
      } else {
        result.close();
        query.close();
        throw new SessionDriverException("Count query returned no rows.");
      }      
    } catch (SQLException e) {
      throw new SessionDriverException(e);
    }
  }

  @Override
  public void invalidate(String hashedId) throws SessionDriverException {
    try (MySQLDatabase db = provider.getDatabase()) {
      NamedPreparedStatement query = db.prepare("DELETE FROM sessions WHERE session_id = :id;");
      query.setString("id", hashedId);
      query.executeUpdate();
      query.close();
    } catch (SQLException e) {
      throw new SessionDriverException(e);
    }
  }

  @Override
  public void keepAliveIfExists(String hashedId) throws SessionDriverException {
    try (MySQLDatabase db = provider.getDatabase()) {
      NamedPreparedStatement query = db.prepare("UPDATE sessions SET last_save = :time WHERE session_id = :id;");
      query.setString("id", hashedId);
      query.setLong("time", Time.now());
      query.executeUpdate();
      query.close();
    } catch (SQLException e) {
      throw new SessionDriverException(e);
    }
  }
}
