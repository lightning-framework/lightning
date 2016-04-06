package lightning.db;

import java.sql.Connection;
import java.sql.SQLException;

public interface MySQLDatabaseProvider {
  /**
   * @return A MySQLDatabase instance created by connection pooling. Close after use.
   * @throws SQLException On failure.
   */
  public MySQLDatabase getDatabase() throws SQLException;
  
  /**
   * @return A raw Connection instance created by connection pooling. Close after use.
   * @throws SQLException On failure.
   */
  public Connection getConnection() throws SQLException;
}
