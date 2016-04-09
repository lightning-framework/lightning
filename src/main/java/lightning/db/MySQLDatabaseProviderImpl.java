package lightning.db;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import lightning.config.Config;

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
public final class MySQLDatabaseProviderImpl implements MySQLDatabaseProvider {
  private DataSource source;
  private final Config config;
  
  public MySQLDatabaseProviderImpl(Config config) throws SQLException, PropertyVetoException {
    this.config = config;
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
    String url = String.format("jdbc:mysql://%s:%d/%s", config.db.host, config.db.port, config.db.name);
    
    if (config.db.useSsl) {
      url += "?verifyServerCertificate=true&useSSL=true&requireSSL=true";
    }
    
    source.setJdbcUrl(url);
    source.setDriverClass("com.mysql.jdbc.Driver");
    source.setUser(config.db.username);
    source.setPassword(config.db.password);
    source.setMinPoolSize(config.db.minPoolSize);
    source.setMaxPoolSize(config.db.maxPoolSize);
    source.setAcquireIncrement(config.db.acquireIncrement);
    source.setMaxStatements(config.db.maxStatementsCached); // To be cached.
    source.setAcquireRetryAttempts(config.db.acquireRetryAttempts);
    source.setAcquireRetryDelay(config.db.acquireRetryDelayMs); // milliseconds
    source.setAutoCommitOnClose(config.db.autoCommitOnClose);
    source.setDebugUnreturnedConnectionStackTraces(true);
    source.setMaxConnectionAge(config.db.maxConnectionAgeS); // seconds
    source.setMaxIdleTime(config.db.maxIdleTimeS); // seconds
    source.setInitialPoolSize(config.db.minPoolSize);
    source.setMaxIdleTimeExcessConnections(config.db.maxIdleTimeExcessConnectionsS); // seconds
    source.setUnreturnedConnectionTimeout(config.db.unreturnedConnectionTimeoutS); // seconds
    source.setTestConnectionOnCheckin(false);
    source.setTestConnectionOnCheckout(false); // Note: expensive if true
    source.setIdleConnectionTestPeriod(config.db.idleConnectionTestPeriodS); // seconds
    this.source = source;
  }
  
  public String getHostName() {
    return config.db.host;
  }
  
  public String getPassword() {
    return config.db.password;
  }
  
  public String getUser() {
    return config.db.username;
  }
  
  public int getPort() {
    return config.db.port;
  }
  
  public String getDatabaseName() {
    return config.db.name;
  }
}
