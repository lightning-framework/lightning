package lightning.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Optional;

/**
 * Adds macros for dealing with SQL NULL values in java.sql.ResultSet (because wasNull() is horrible API design).
 */
public class ResultSets {
  public static @Nullable Long getLong(ResultSet rs, String columnName) throws SQLException {
    return rs.getObject(columnName) != null ? rs.getLong(columnName) : null;
  }
  
  public static @Nullable Long getLong(ResultSet rs, int columnIdx) throws SQLException {
    return rs.getObject(columnIdx) != null ? rs.getLong(columnIdx) : null;
  }
  
  public static @Nullable Integer getInteger(ResultSet rs, String columnName) throws SQLException {
    return rs.getObject(columnName) != null ? rs.getInt(columnName) : null;
  }
  
  public static @Nullable Integer getInteger(ResultSet rs, int columnIdx) throws SQLException {
    return rs.getObject(columnIdx) != null ? rs.getInt(columnIdx) : null;
  }
  
  public static @Nullable Double getDouble(ResultSet rs, String columnName) throws SQLException {
    return rs.getObject(columnName) != null ? rs.getDouble(columnName) : null;
  }
  
  public static @Nullable Double getDouble(ResultSet rs, int columnIdx) throws SQLException {
    return rs.getObject(columnIdx) != null ? rs.getDouble(columnIdx) : null;
  }
  
  public static @Nullable Float getFloat(ResultSet rs, String columnName) throws SQLException {
    return rs.getObject(columnName) != null ? rs.getFloat(columnName) : null;
  }
  
  public static @Nullable Float getFloat(ResultSet rs, int columnIdx) throws SQLException {
    return rs.getObject(columnIdx) != null ? rs.getFloat(columnIdx) : null;
  }
  
  public static @Nullable Boolean getBoolean(ResultSet rs, String columnName) throws SQLException {
    if (rs.getObject(columnName) == null) {
      return null;
    }
    
    int value = rs.getInt(columnName);
    return value <= 0 ? false : true;
  }
  
  public static Optional<Long> getLongOption(ResultSet rs, String columnName) throws SQLException {
    return rs.getObject(columnName) != null ? Optional.of(rs.getLong(columnName)) : Optional.absent();
  }
  
  public static Optional<Boolean> getBooleanOption(ResultSet rs, String columnName) throws SQLException {
    return Optional.fromNullable(getBoolean(rs, columnName));
  }
  
  public static int encodeBoolean(boolean value) {
    return value ? 1 : 0;
  }
  
  /**
   * Converts a result set into a list of maps. Each map represents a row and maps column
   * names to the associated value. Conversion may not necessarily be possible or correct
   * for all schemas; be careful.
   * @param result
   * @return
   * @throws SQLException
   */
  public static List<Map<String, Object>> toList(ResultSet result) throws SQLException {
    List<Map<String, Object>> data = new ArrayList<>();
    ResultSetMetaData md = result.getMetaData();
    int ncols = md.getColumnCount();
    
    while (result.next()) {
      Map<String, Object> record = new HashMap<>(ncols);
      
      for (int i = 1; i <= ncols; i++) {
        record.put(md.getColumnName(i), result.getObject(i));
      }
      
      data.add(record);
    }
    
    return data;
  }

  /**
   * Converts the row the cursor is currently pointing to into a map of column names to 
   * their associated values and reutnrs it.
   * @param result
   * @return
   * @throws SQLException
   */
  public static Map<String, Object> toMap(ResultSet result) throws SQLException {
    ResultSetMetaData md = result.getMetaData();
    int ncols = md.getColumnCount();
    Map<String, Object> record = new HashMap<>(ncols);
    
    for (int i = 1; i <= ncols; i++) {
      record.put(md.getColumnName(i), result.getObject(i));
    }
    
    return record;
  }
}
