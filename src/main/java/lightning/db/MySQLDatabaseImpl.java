package lightning.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Encapsulates a connection to a MySQL database.
 * This object is not thread-safe; ideally should have one connection per thread.
 * @see http://dev.mysql.com/doc/connector-j/en/connector-j-usagenotes-basic.html
 * @see http://stackoverflow.com/questions/2839321/connect-java-to-a-mysql-database
 * @see http://zetcode.com/db/mysqljava/
 */
public class MySQLDatabaseImpl implements MySQLDatabase {
  private final Connection connection;
  private final MySQLDatabaseProvider provider;

  private MySQLDatabaseImpl(MySQLDatabaseProvider provider) throws SQLException {
    this.provider = provider;
    this.connection = provider.getConnection();
  }
  
  public MySQLDatabaseProvider getProvider() {
    return provider;
  }
   
  private final Stack<Transaction<?>> transactions = new Stack<>();
  private final ReentrantLock transactionLock = new ReentrantLock();
  
  /**
   * Executes a database transaction which returns a value. The queries within the given
   * closure will be executed as an all-or-nothing transaction. If the transaction fails,
   * the database is rolled back and the exception is re-thrown. If the transaction is
   * successful, the value returned by the closure is returned and changes are finalized.
   * 
   * The transaction implementation assumes at most one thread will have access to this
   * instance of MySQLDatabase at a time.
   * 
   * Transactions should terminate in finite time. Infinite loops or long-lived operations
   * may degrade database performance.
   * 
   * Transactions on the same MySQLDatabase instance from the same thread may be nested safely. 
   * 
   * Placing one transaction inside of another effectively expands the encompassing 
   * transaction to contain the queries in the nested transaction. 
   * 
   * Nested transactions are not finalized until the outermost transaction is finalized. 
   * The failure of any nested transaction causes the outermost transaction (and any 
   * transactions nested within it) to fail and be rolled back.
   * 
   * @param transaction A closure. All queries operating on this instance of MySQLDatabase
   *                    inside of the closure will be executed as a transaction.
   * @return The value returned by the given closure (if any).
   * @throws Exception If the transaction (or any enclosed transaction) fails.
   */
  public <T> T transaction(Transaction<T> transaction) throws Exception {
    try {
      transactionLock.lock();
      //logger.debug("Transaction Starts");
      
      if (transactions.isEmpty()) {
        //logger.debug("SET TRANSACTION = 1;");
        connection.setAutoCommit(false);
      }
      
      transactions.push(transaction);
      
      T result = transaction.execute();
      
      transactions.pop();
      
      if (transactions.isEmpty()) {
        //logger.debug("COMMIT, SET TRANSACTION = 0;");
        connection.commit();
        connection.setAutoCommit(true);
        transactionLock.unlock();
      }
      
      //logger.debug("Transaction Ends (Normal)");
      return result;
    } catch (Exception e) {
      transactions.pop();
      
      if (transactions.isEmpty()) {
        //logger.debug("ROLLBACK, SET TRANSACTION = 0;");
        connection.rollback();
        connection.setAutoCommit(true);
        transactionLock.unlock();
      }
      
      //logger.debug("Transaction Ends (Exception)");
      
      throw e;
    }
  }

  /**
   * Executes a database transaction which does not return a value.
   * @see {@link #transaction(Transaction)}
   */
  public void transaction(VoidTransaction transaction) throws Exception {
    transaction(() -> {
      transaction.execute();
      return null;
    });
  }

  /**
   * Closes the database connection.
   * 
   * @throws SQLException On failure.
   */
  public void close() throws SQLException {
    if (!connection.isClosed()) {
      connection.close();
    }
  }

  /**
   * @return The underlying database connection for direct usage.
   */
  public Connection getConnection() {
    return connection;
  }

  /**
   * @param query A prepared SQL statement with named parameters.
   * @return A prepared statement object.
   * @throws SQLException On failure.
   */
  public NamedPreparedStatement prepare(String query) throws SQLException {
    return NamedPreparedStatement.forQuery(connection, query);
  }
  
  /**
   * @param query A prepared SQL statement with named parameters.
   * @param data A map of named parameters to their values.
   * @return A prepared statement object initialized with data.
   * @throws SQLException On failure.
   */
  public NamedPreparedStatement prepare(String query, Map<String, ?> data) throws SQLException {
    NamedPreparedStatement statement = NamedPreparedStatement.forQuery(connection, query);
    statement.setFromMap(data);
    return statement;
  }

  /**
   * @param table Name of the table.
   * @param data Map of column names to desired values.
   * @return A prepared statement that inserts the given row into the database when executed.
   * @throws SQLException On failure.
   */
  public NamedPreparedStatement prepareInsert(String table, Map<String, ?> data)
      throws SQLException {
    return NamedPreparedStatement.forInsert(connection, table, data);
  }

  /**
   * @param table Name of the table.
   * @param data Map of column names to desired values. Must contain the primary key of the table.
   * @return A prepared statement that replaced the given row into the database when executed.
   * @throws SQLException On failure.
   */
  public NamedPreparedStatement prepareReplace(String table, Map<String, ?> data)
      throws SQLException {
    return NamedPreparedStatement.forReplace(connection, table, data);
  }

  /**
   * @param query A prepared SQL query with unnamed parameters (e.g. SELECT * FROM users WHERE name
   *        = ?;)
   * @param data To fill in the unnamed parameters (in order).
   * @return A prepared statement.
   * @throws SQLException On failure.
   */
  public PreparedStatement prepare(String query, List<Object> data) throws SQLException {
    PreparedStatement statement =
        connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

    for (int i = 0; i < data.size(); i++) {
      Object value = data.get(i);

      if (value instanceof Long) {
        statement.setLong(i + 1, ((Long) value).longValue());
      } else if (value instanceof Integer) {
        statement.setInt(i + 1, ((Integer) value).intValue());
      } else if (value instanceof String) {
        statement.setString(i + 1, (String) value);
      } else if (value instanceof Timestamp) {
        statement.setTimestamp(i + 1, (Timestamp) value);
      } else if (value instanceof Boolean) {
        statement.setInt(i + 1, ((Boolean) value).booleanValue() ? 1 : 0); // SQL has no native booleans.
      } else {
        statement.setObject(i + 1, value);
      }
    }

    return statement;
  }
  
  /**
   * @param provider Connection provider.
   * @return A connection to the database.
   * @throws SQLException On failure.
   */
  public static MySQLDatabase createConnection(MySQLDatabaseProvider provider) throws SQLException {
    return new MySQLDatabaseImpl(provider);
  }

  @Override
  public DatabasePaginator paginate(String query, Map<String, Object> data, long pageSize)
      throws SQLException {
    return new DatabasePaginator(this, query, data, pageSize);
  }

  @Override
  public void save(Saveable saveable) throws SQLException {
    saveable.saveTo(this);
  }
}
