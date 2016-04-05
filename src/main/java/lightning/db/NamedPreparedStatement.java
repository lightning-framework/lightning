package lightning.db;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * Replacement for java.sql.PreparedStatement that allows parameter naming (e.g. using :name as a placeholder
 * instead of ?).
 * @see http://www.javaworld.com/article/2077706/core-java/named-parameters-for-preparedstatement.html
 */
public final class NamedPreparedStatement implements AutoCloseable {
  /**
   * Returns a named prepared statement for the given connection and query.
   * @param connection
   * @param query A query using named parameters (e.g. SELECT * FROM users WHERE username = :username;)
   * @return
   * @throws SQLException
   */
  public static NamedPreparedStatement forQuery(Connection connection, String query) throws SQLException {
    return new NamedPreparedStatement(connection, query);
  }
  
  public static NamedPreparedStatement forInsert(Connection connection, String table, Map<String, ?> data) throws SQLException {
    if (data.isEmpty())
      throw new SQLException("No columns specified.");
    
    NamedPreparedStatement statement = new NamedPreparedStatement(connection, insertQueryFromMap("INSERT", table, data));
    statement.setFromMap(data);
    return statement;
  }
  
  public static NamedPreparedStatement forReplace(Connection connection, String table, Map<String, ?> data) throws SQLException {
    if (data.isEmpty())
      throw new SQLException("No columns specified.");
    
    NamedPreparedStatement statement = new NamedPreparedStatement(connection, insertQueryFromMap("REPLACE", table, data));
    statement.setFromMap(data);
    return statement;
  }
  
  /**
   * Builds a named parameterized insertion query using column names from keys in the map.
   * @param table
   * @param data
   * @return
   */
  private static String insertQueryFromMap(String verb, String table, Map<String, ?> data) {
    StringBuilder text = new StringBuilder();
    
    text.append(verb);
    text.append(" INTO ");
    text.append(table);
    text.append("(");
    
    for (String column : data.keySet()) {
      text.append(column);
      text.append(", ");
    }
    text.setLength(text.length() - 2);
    
    text.append(") VALUES (");
    
    for (String column : data.keySet()) {
      text.append(":");
      text.append(column);
      text.append(", ");
    }
    text.setLength(text.length() - 2);
    
    text.append(");");
    
    return text.toString();
  }
  
  /** The statement this object is wrapping. */
  private final PreparedStatement statement;

  /** Maps parameter names to arrays of ints which are the parameter indices. */
  private final Map<String, int[]> indexMap;

  /**
   * Creates a NamedParameterStatement.  Wraps a call to
   * c.{@link Connection#prepareStatement(java.lang.String) prepareStatement}.
   * @param connection the database connection
   * @param query      the parameterized query
   * @throws SQLException if the statement could not be created
   */
  private NamedPreparedStatement(Connection connection, String query) throws SQLException {
      indexMap = new HashMap<>();
      String parsedQuery = parse(query, indexMap);
      statement = connection.prepareStatement(parsedQuery, Statement.RETURN_GENERATED_KEYS);
  }
  
  /**
   * Sets parameters from a map whose keys are column names and values are data values.
   * @param data
   * @throws SQLException 
   */
  public void setFromMap(Map<String, ?> data) throws SQLException {
    if (data == null) {
      return;
    }
    
    for (Map.Entry<String, ?> entry : data.entrySet()) {
      Object value = entry.getValue();
      String column = entry.getKey();
      
      if (value instanceof Long) {
        this.setLong(column, ((Long) value).longValue());
      } else if (value instanceof Integer) {
        this.setInt(column, ((Integer) value).intValue());
      } else if (value instanceof String) {
        this.setString(column, (String) value);
      } else if (value instanceof Timestamp) {
        this.setTimestamp(column, (Timestamp) value);
      } else if (value instanceof Boolean) {
        this.setInt(column, ((Boolean) value).booleanValue() ? 1 : 0); // SQL has no native booleans.
      } else if (value instanceof SQLNull) {
        this.setNull(column, ((SQLNull) value).getSqlType());
      } else if (value instanceof InputStream) {
        this.setBinaryStream(column, (InputStream) value);
      } else if (value instanceof Double) {
        this.setDouble(column, (Double) value);
      } else if (value instanceof Float) {
        this.setFloat(column, (Float) value);
      } else if (value instanceof Date) {
        this.setDate(column, (Date) value);
      } else if (value instanceof Time) {
        this.setTime(column, (Time) value);
      } else {
        this.setObject(column, value);
      }
    }
  }

  /**
   * Parses a query with named parameters. The parameter-index mappings are put into the map, and the
   * parsed query is returned.  DO NOT CALL FROM CLIENT CODE. This method is non-private so JUnit code can
   * test it.
   * @param query    query to parse
   * @param paramMap map to hold parameter-index mappings
   * @return the parsed query
   */
  private static final String parse(String query, Map<String, int[]> paramMap) {
    Map<String, LinkedList<Integer>> intermediateParams = new HashMap<>();
  
    // I was originally using regular expressions, but they didn't work well for ignoring
    // parameter-like strings inside quotes.
    int length = query.length();
    StringBuffer parsedQuery = new StringBuffer(length);
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;
    int index = 1;

    for(int i = 0; i < length; i++) {
      char c = query.charAt(i);
      
      if(inSingleQuote) {
        if(c == '\'') {
          inSingleQuote = false;
        }
      } else if(inDoubleQuote) {
        if(c == '"') {
          inDoubleQuote = false;
        }
      } else {
        if(c == '\'') {
          inSingleQuote = true;
        } else if(c == '"') {
          inDoubleQuote = true;
        } else if(c == ':' && i + 1 < length && Character.isJavaIdentifierStart(query.charAt(i + 1))) {
          int j = i+2;
          
          while(j < length && Character.isJavaIdentifierPart(query.charAt(j))) {
            j++;
          }
          
          String name = query.substring(i + 1,j);
          c = '?'; // replace the parameter with a question mark
          i += name.length(); // skip past the end if the parameter

          LinkedList<Integer> indexList = intermediateParams.get(name);
          
          if(indexList == null) {
            indexList = new LinkedList<Integer>();
            intermediateParams.put(name, indexList);
          }
          
          indexList.add(new Integer(index));

          index++;
        }
      }
      
      parsedQuery.append(c);
    }

    // replace the lists of Integer objects with arrays of ints
    for(Iterator<Map.Entry<String, LinkedList<Integer>>> itr = intermediateParams.entrySet().iterator(); itr.hasNext();) {
      Map.Entry<String, LinkedList<Integer>> entry = itr.next();
      LinkedList<Integer> list = entry.getValue();
      int[] indexes = new int[list.size()];
      int i = 0;
      
      for(Iterator<Integer> itr2 = list.iterator(); itr2.hasNext();) {
          Integer x = itr2.next();
          indexes[i++] = x.intValue();
      }
      
      paramMap.put(entry.getKey(), indexes);
    }

    return parsedQuery.toString();
  }

  /**
   * Returns the indexes for a parameter.
   * @param name parameter name
   * @return parameter indexes
   * @throws IllegalArgumentException if the parameter does not exist
   */
  private int[] getIndexes(String name) {
    int[] indexes = indexMap.get(name);
    
    if(indexes == null) {
      throw new IllegalArgumentException("Parameter not found: " + name);
    }
    
    return indexes;
  }

  /**
   * Sets a parameter.
   * @param name  parameter name
   * @param value parameter value
   * @throws SQLException if an error occurred
   * @throws IllegalArgumentException if the parameter does not exist
   * @see PreparedStatement#setObject(int, java.lang.Object)
   */
  public void setObject(String name, Object value) throws SQLException {
      int[] indexes = getIndexes(name);
      for(int i = 0; i < indexes.length; i++) {
          statement.setObject(indexes[i], value);
      }
  }

  /**
   * Sets a parameter.
   * @param name  parameter name
   * @param value parameter value
   * @throws SQLException if an error occurred
   * @throws IllegalArgumentException if the parameter does not exist
   * @see PreparedStatement#setString(int, java.lang.String)
   */
  public void setString(String name, String value) throws SQLException {
    int[] indexes = getIndexes(name);
    for(int i = 0; i < indexes.length; i++) {
      statement.setString(indexes[i], value);
    }
  }
  
  /**
   * @param name Of the parameter.
   * @param sqlType Use java.sql.Types
   * @throws SQLException
   */
  public void setNull(String name, int sqlType) throws SQLException {
    int[] indexes = getIndexes(name);
    for(int i = 0; i < indexes.length; i++) {
      statement.setNull(indexes[i], sqlType);
    }
  }
  
  public void setNull(String name, SQLNull type) throws SQLException {
    setNull(name, type.getSqlType());
  }

  /**
   * Sets a parameter.
   * @param name  parameter name
   * @param value parameter value
   * @throws SQLException if an error occurred
   * @throws IllegalArgumentException if the parameter does not exist
   * @see PreparedStatement#setInt(int, int)
   */
  public void setInt(String name, int value) throws SQLException {
    int[] indexes = getIndexes(name);
    for(int i = 0; i < indexes.length; i++) {
      statement.setInt(indexes[i], value);
    }
  }
  
  public void setDouble(String name, double value) throws SQLException {
    int[] indexes = getIndexes(name);
    for(int i = 0; i < indexes.length; i++) {
      statement.setDouble(indexes[i], value);
    }
  }
  
  public void setFloat(String name, float value) throws SQLException {
    int[] indexes = getIndexes(name);
    for(int i = 0; i < indexes.length; i++) {
      statement.setFloat(indexes[i], value);
    }
  }
  
  public void setBigDecimal(String name, BigDecimal value) throws SQLException {
    int[] indexes = getIndexes(name);
    for(int i = 0; i < indexes.length; i++) {
      statement.setBigDecimal(indexes[i], value);
    }
  }
  
  public void setTime(String name, Time value) throws SQLException {
    int[] indexes = getIndexes(name);
    for(int i = 0; i < indexes.length; i++) {
      statement.setTime(indexes[i], value);
    }
  }
  
  public void setDate(String name, Date value) throws SQLException {
    int[] indexes = getIndexes(name);
    for(int i = 0; i < indexes.length; i++) {
      statement.setDate(indexes[i], value);
    }
  }

  /**
   * Sets a parameter.
   * @param name  parameter name
   * @param value parameter value
   * @throws SQLException if an error occurred
   * @throws IllegalArgumentException if the parameter does not exist
   * @see PreparedStatement#setInt(int, int)
   */
  public void setLong(String name, long value) throws SQLException {
    int[] indexes = getIndexes(name);
    for(int i = 0; i < indexes.length; i++) {
      statement.setLong(indexes[i], value);
    }
  }
  
  @FunctionalInterface
  public interface SetterFunction {
    public void apply(PreparedStatement stmt, int index) throws SQLException;
  }
  
  /**
   * Allows to use raw statement for setting for a name using a setter function.
   * @param name
   * @param fn
   */
  public void set(String name, SetterFunction fn) throws SQLException {
    int[] indexes = getIndexes(name);
    for(int i = 0; i < indexes.length; i++) {
      fn.apply(statement, indexes[i]);
    }
  }
  
  public void setBinaryStream(String name, InputStream stream, long length) throws SQLException {
    int[] indexes = getIndexes(name);
    for(int i = 0; i < indexes.length; i++) {
      statement.setBinaryStream(indexes[i], stream, length);
    }
  }
  
  public void setBinaryStream(String name, InputStream stream) throws SQLException {
    int[] indexes = getIndexes(name);
    for(int i = 0; i < indexes.length; i++) {
      statement.setBinaryStream(indexes[i], stream);
    }
  }

  /**
   * Sets a parameter.
   * @param name  parameter name
   * @param value parameter value
   * @throws SQLException if an error occurred
   * @throws IllegalArgumentException if the parameter does not exist
   * @see PreparedStatement#setTimestamp(int, java.sql.Timestamp)
   */
  public void setTimestamp(String name, Timestamp value) throws SQLException {
    int[] indexes = getIndexes(name);
    for(int i = 0; i < indexes.length; i++) {
      statement.setTimestamp(indexes[i], value);
    }
  }

  /**
   * Returns the underlying statement.
   * @return the statement
   */
  public PreparedStatement getStatement() {
      return statement;
  }

  /**
   * Executes the statement.
   * @return true if the first result is a {@link ResultSet}
   * @throws SQLException if an error occurred
   * @see PreparedStatement#execute()
   */
  public boolean execute() throws SQLException {
    return statement.execute();
  }
  
  /**
   * Executes a query which returns a single row with the schema (Long), returning the value of that long.
   * Should be used to execute queries which SELECT COUNT(*).
   * Closes teh query.
   * @return The long value of the first column of the first row returned by this statement.
   * @throws SQLException
   */
  public long executeCount() throws SQLException {
    try (ResultSet rs = statement.executeQuery()) {
      
      if (!rs.next())
        throw new SQLException("Incorrect schema returned from count query.");
      
      long count = rs.getLong(1);
      return count;
    } finally {
      statement.close();
    }
  }
  
  /**
   * Executes the statement, which must be an SQL INSERT, UPDATE or DELETE statement;
   * or an SQL statement that returns nothing, such as a DDL statement.
   * Closes the statement.
   * @return number of rows affected
   * @throws SQLException if an error occurred
   * @see PreparedStatement#executeUpdate()
   */
  public int executeUpdateAndClose() throws SQLException {
    try {
      int count = statement.executeUpdate();
      return count;
    } finally {
      statement.close();
    }
  }
  
  /**
   * Executes the statement, which must be an SQL INSERT statement;
   * Closes the statement and returns the inserted id.
   * @return Inserted primary key.
   * @throws SQLException if an error occurred
   * @see PreparedStatement#executeUpdate()
   */
  public long executeInsertAndClose() throws SQLException {
    try {
      statement.executeUpdate();
      long id = this.getInsertionId();
      return id;
    } finally {
      statement.close();
    }
  }

  /**
   * Executes the statement, which must be a query.
   * @return the query results
   * @throws SQLException if an error occurred
   * @see PreparedStatement#executeQuery()
   */
  public ResultSet executeQuery() throws SQLException {
    return statement.executeQuery();
  }

  /**
   * Executes the statement, which must be an SQL INSERT, UPDATE or DELETE statement;
   * or an SQL statement that returns nothing, such as a DDL statement.
   * @return number of rows affected
   * @throws SQLException if an error occurred
   * @see PreparedStatement#executeUpdate()
   */
  public int executeUpdate() throws SQLException {
    return statement.executeUpdate();
  }

  /**
   * Closes the statement.
   * @throws SQLException if an error occurred
   * @see Statement#close()
   */
  public void close() throws SQLException {
    statement.close();
  }

  /**
   * Adds the current set of parameters as a batch entry.
   * @throws SQLException if something went wrong
   */
  public void addBatch() throws SQLException {
    statement.addBatch();
  }

  /**
   * Executes all of the batched statements.
   * 
   * See {@link Statement#executeBatch()} for details.
   * @return update counts for each statement
   * @throws SQLException if something went wrong
   */
  public int[] executeBatch() throws SQLException {
    return statement.executeBatch();
  }
  
  /**
   * @return
   * @throws SQLException
   */
  public ResultSet getGeneratedKeys() throws SQLException {
    return statement.getGeneratedKeys();
  }
  
  /**
   * @return Auto-generated ID of first row.
   * @throws SQLException
   */
  public long getInsertionId() throws SQLException {
    try (ResultSet result = getGeneratedKeys()) {
    
      if (!result.next())
        throw new SQLException("Failed to get autogenerated keys.");
      
      long value = result.getLong(1);
      return value;
    }
  }
}