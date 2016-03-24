package lightning.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class MySQLDatabaseProxy implements MySQLDatabase {
  private final MySQLDatabaseProvider provider;
  private MySQLDatabase delegate;
  
  public MySQLDatabaseProxy(MySQLDatabaseProvider provider) {
    this.provider = provider;
    delegate = null;
  }
  
  public MySQLDatabaseProvider getProvider() {
    return this.provider;
  }
  
  private void lazyLoad() throws SQLException {
    if (delegate == null) {
      delegate = provider.getDatabase();
    }
  }

  @Override
  public MySQLDatabase createIdenticalConnection() throws SQLException {
    lazyLoad();
    return delegate.createIdenticalConnection();
  }

  @Override
  public <T> T transaction(Transaction<T> transaction) throws Exception {
    lazyLoad();
    return delegate.transaction(transaction);
  }

  @Override
  public void transaction(VoidTransaction transaction) throws Exception {
    lazyLoad();
    delegate.transaction(transaction);
  }

  @Override
  public void close() throws SQLException {
    // Do nothing, users shouldn't be able to close the proxy.
  }
  
  public void reallyClose() throws SQLException {
    if (delegate != null) {
      delegate.close();
    }
  }

  @Override
  public Connection getConnection() throws SQLException {
    lazyLoad();
    return delegate.getConnection();
  }

  @Override
  public NamedPreparedStatement prepare(String query) throws SQLException {
    lazyLoad();
    return delegate.prepare(query);
  }

  @Override
  public NamedPreparedStatement prepare(String query, Map<String, ?> data) throws SQLException {
    lazyLoad();
    return delegate.prepare(query, data);
  }

  @Override
  public NamedPreparedStatement prepareInsert(String table, Map<String, ?> data)
      throws SQLException {
    lazyLoad();
    return delegate.prepareInsert(table, data);
  }

  @Override
  public NamedPreparedStatement prepareReplace(String table, Map<String, ?> data)
      throws SQLException {
    lazyLoad();
    return delegate.prepareReplace(table, data);
  }

  @Override
  public PreparedStatement prepare(String query, List<Object> data) throws SQLException {
    lazyLoad();
    return delegate.prepare(query, data);
  }

  @Override
  public DatabasePaginator paginate(String query, Map<String, Object> data, long pageSize)
      throws SQLException {
    lazyLoad();
    return delegate.paginate(query, data, pageSize);
  }

  @Override
  public void save(Saveable saveable) throws SQLException {
    lazyLoad();
    delegate.save(saveable);
  }
}
