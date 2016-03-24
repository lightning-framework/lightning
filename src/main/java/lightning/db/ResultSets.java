package lightning.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.annotation.Nullable;

import com.google.common.base.Optional;

/**
 * Adds macros for dealing with SQL NULL values in java.sql.ResultSet.
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
  
  public static Optional<Long> getLongOption(ResultSet rs, String columnName) throws SQLException {
    return rs.getObject(columnName) != null ? Optional.of(rs.getLong(columnName)) : Optional.absent();
  }
  
  public static @Nullable Boolean getBoolean(ResultSet rs, String columnName) throws SQLException {
    if (rs.getObject(columnName) == null) {
      return null;
    }
    
    int value = rs.getInt(columnName);
    return value <= 0 ? false : true;
  }
  
  public static Optional<Boolean> getBooleanOption(ResultSet rs, String columnName) throws SQLException {
    return Optional.fromNullable(getBoolean(rs, columnName));
  }
  
  public static int encodeBoolean(boolean value) {
    return value ? 1 : 0;
  }
}
