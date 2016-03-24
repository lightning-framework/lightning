package lightning.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class to help with paginating database queries.
 */
public class DatabasePaginator {
  final static Logger logger = LoggerFactory.getLogger(DatabasePaginator.class);
  
  private long rowCount = -1;
  private long pageCount = -1;
  private final long pageSize;
  private final MySQLDatabase db;
  private final String query;
  private final Map<String, Object> parameters;
  
  @FunctionalInterface
  public static interface DatabasePaginatorVisitor {
    /**
     * A function that will be called once for each row.
     * The ResultSet will be closed automatically.
     * @param row A result set for some row in your query.
     */
    public void apply(ResultSet row) throws Exception;
  }
  
  /**
   * @param db The database to use.
   * @param query The text of a query (as would be formatted in a NamedPreparedStatement) with no LIMIT clause.
   * @param parameters The values that fill in place-holders in the query text.
   * @param pageSize The size of each page.
   */
  public DatabasePaginator(MySQLDatabase db, String query, Map<String, Object> parameters, long pageSize) {
    this.pageSize = pageSize;
    this.db = db;
    this.query = query.trim();
    this.parameters = parameters;
  }
  
  /**
   * @return The total number of pages returned by your query.
   * @throws SQLException
   */
  public long getTotalPages() throws SQLException {
    calculatePages();    
    return pageCount;
  }
  
  /**
   * @return The total number of rows returned by your query.
   * @throws SQLException
   */
  public long getTotalRows() throws SQLException {
    calculatePages();
    return rowCount; 
  }
  
  /**
   * @return The size of each page.
   */
  public long getPageSize() {
    return pageSize;
  }

  /**
   * @param k A page number.
   * @return The size of the k-th page (in number of rows).
   * @throws SQLException
   */
  public long getSizeOfPage(long k) throws SQLException {
    calculatePages();
    if (k < 0 || k >= pageCount) {
      return 0;
    }
    
    if (k < pageCount - 1) {
      return pageSize;
    } else {
      return rowCount % pageSize;
    }
  }
  
  /**
   * @param k A page number.
   * @return Whether or not the k-th page exists.
   * @throws SQLException
   */
  public boolean hasPage(long k) throws SQLException {
    calculatePages();
    return k >= 0 && k < pageCount;
  }
  
  /**
   * Counts the number of records that would be returned by the query.
   * @throws SQLException
   */
  private void calculatePages() throws SQLException {
    if (rowCount != -1 && pageCount != -1) {
      return; // Nothing to do.
    }
    
    int selectStart = query.toLowerCase().indexOf("select ") + "select ".length();
    int selectEnd = query.toLowerCase().indexOf(" from ");
    String countQuery = query.substring(0, selectStart) + "COUNT(*) AS count" + query.substring(selectEnd);
    logger.debug("Count Query: " + countQuery);
    try (NamedPreparedStatement statement = db.prepare(countQuery, parameters);
         ResultSet result = statement.executeQuery()) {
      if (!result.next()) {
        throw new SQLException("The count query failed.");
      }
      
      rowCount = result.getLong("count");
      pageCount = (rowCount == 0) ? 1 : (rowCount / pageSize) + (rowCount % pageSize > 0 ? 1 : 0);
    }
    logger.debug("Found Count: rows=" + rowCount + " pages=" + pageCount);
  }
  
  /**
   * Executes a function once for every row in the resulting page.
   * @param pageNumber A page number (0 to getTotalPages() - 1).
   * @param fn A function to execute for every row in the resulting page.
   * @throws SQLException On error.
   */
  public void forRowsInPage(long pageNumber, DatabasePaginatorVisitor fn) throws Exception {
    if (!hasPage(pageNumber)) {
      throw new SQLException(String.format("Page %d does not exist.", pageNumber));
    }
    
    String pageQuery = (query.endsWith(";") ? query.substring(0, query.length() - 1) : query) 
        + String.format(" LIMIT %d, %d;", pageNumber * pageSize, pageSize);
    logger.debug("Fetching page=" + pageNumber + " query=" + pageQuery);
    try (NamedPreparedStatement statement = db.prepare(pageQuery, parameters);
         ResultSet result = statement.executeQuery()) {
      while (result.next()) {
        fn.apply(result);
      }
    }
  }
}
