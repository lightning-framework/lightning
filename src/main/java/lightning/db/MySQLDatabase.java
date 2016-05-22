package lightning.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Note: See MySQLDatabaseImpl for documentation and comments.
 */
public interface MySQLDatabase extends AutoCloseable {
  @FunctionalInterface
  public interface Transaction<T> {
    public T execute() throws Exception;
  }

  @FunctionalInterface
  public interface VoidTransaction {
    public void execute() throws Exception;
  }
  
  public interface Saveable {
    public void saveTo(MySQLDatabase db) throws SQLException;
  }

  public <T> T transaction(Transaction<T> transaction) throws Exception;
  public void transaction(VoidTransaction transaction) throws Exception;
  public void close() throws SQLException;
  public Connection getConnection() throws SQLException;
  public NamedPreparedStatement prepare(String query) throws SQLException;
  public NamedPreparedStatement prepare(String query, Map<String, ?> data) throws SQLException;
  public NamedPreparedStatement prepareInsert(String table, Map<String, ?> data) throws SQLException;
  public NamedPreparedStatement prepareReplace(String table, Map<String, ?> data) throws SQLException;
  public DatabasePaginator paginate(String query, Map<String, Object> data, long pageSize) throws SQLException;
  public PreparedStatement prepare(String query, List<Object> data) throws SQLException;
  public void save(Saveable saveable) throws SQLException;  
  
  public static MySQLDatabase createConnection(MySQLDatabaseProvider provider) throws SQLException {
    return MySQLDatabaseImpl.createConnection(provider);
  }
}
