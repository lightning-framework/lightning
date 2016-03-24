package lightning.db;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * Provides database connections using pooling.
 * 
 * With connection pooling, each thread (or even function) can safely call getDatabase() to acquire
 * an instance for its purpose and then close() that instance when finished with it. As an example,
 * each individual web request should acquire a connection, do its work, then close it.
 * 
 * In fact, you must remember to close connections as failing to do so will cause leaks. I recommend using
 * try-with-resources to automate this.
 * 
 * NOTE: BoneCP, Apache DBCP, C3P0 are all libraries that could be used for implementing this.
 * @see http://docs.oracle.com/javase/7/docs/api/javax/sql/DataSource.html
 * @see https://www.google.com/webhp?sourceid=chrome-instant&ion=1&espv=2&ie=UTF-8#q=sql%20java%20jetty
 * @see https://www.google.com/webhp?sourceid=chrome-instant&ion=1&espv=2&ie=UTF-8#q=java.sql.connection+web+app
 * @see https://www.google.com/webhp?sourceid=chrome-instant&ion=1&espv=2&ie=UTF-8#q=web%20app%20java.sql.connection%20thread%20safe
 * @see https://www.google.com/webhp?sourceid=chrome-instant&ion=1&espv=2&ie=UTF-8#q=java+sql+connection+pooling
 * @see http://www.javatips.net/blog/2013/12/dbcp-connection-pooling-example
 * @see http://balusc.omnifaces.org/2008/07/dao-tutorial-data-layer.html
 * @see http://www.javaranch.com/journal/200601/JDBCConnectionPooling.html
 */
public final class MySQLDatabaseProvider {
  private DataSource source;
  private final String hostName;
  private final String user;
  private final String password;
  private final String databaseName;
  private final int port;
  
  public MySQLDatabaseProvider(String hostName, int port, String user, String password, String databaseName) throws SQLException, PropertyVetoException {
    this.hostName = hostName;
    this.port = port;
    this.user = user;
    this.password = password;
    this.databaseName = databaseName;
    initializeSource();
  }
  
  /**
   * @return A MySQLDatabase instance created by connection pooling. Close after use.
   * @throws SQLException On failure.
   */
  public MySQLDatabase getDatabase() throws SQLException {
    return MySQLDatabase.createConnection(this);
  }
  
  /**
   * @return A raw Connection instance created by connection pooling. Close after use.
   * @throws SQLException On failure.
   */
  public Connection getConnection() throws SQLException {
    return source.getConnection();
  }

  private void initializeSource() throws SQLException, PropertyVetoException {
    // See http://www.mchange.com/projects/c3p0/
    ComboPooledDataSource source = new ComboPooledDataSource();
    source.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s", hostName, port, databaseName));
    source.setDriverClass("com.mysql.jdbc.Driver");
    source.setUser(user);
    source.setPassword(password);
    source.setMinPoolSize(5);
    source.setMaxPoolSize(40);
    source.setAcquireIncrement(5);
    source.setMaxStatements(500); // To be cached.
    source.setAcquireRetryAttempts(3);
    source.setAcquireRetryDelay(1000); // milliseconds
    source.setAutoCommitOnClose(true);
    source.setDebugUnreturnedConnectionStackTraces(true);
    source.setMaxConnectionAge(50000); // seconds
    source.setMaxIdleTime(50000); // seconds
    source.setMaxIdleTimeExcessConnections(50000); // seconds
    source.setUnreturnedConnectionTimeout(50000); // seconds
    source.setTestConnectionOnCheckin(false);
    source.setTestConnectionOnCheckout(false); // Note: expensive if true
    source.setIdleConnectionTestPeriod(600); // seconds
    this.source = source;
  }
  
  public String getHostName() {
    return hostName;
  }
  
  public String getPassword() {
    return password;
  }
  
  public String getUser() {
    return user;
  }
  
  public int getPort() {
    return port;
  }
  
  public String getDatabaseName() {
    return databaseName;
  }
}
