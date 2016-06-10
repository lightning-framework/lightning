package lightning.db;

import java.sql.Connection;

public enum TransactionLevel {
  SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE),
  REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
  READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
  READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
  NONE(Connection.TRANSACTION_NONE);
  
  private final int driverCode;
  
  private TransactionLevel(int driverCode) {
    this.driverCode = driverCode;
  }
  
  public int getDriverCode() {
    return driverCode;
  }
  
  public static TransactionLevel fromDriverCode(int code) {
    switch (code) {
      case Connection.TRANSACTION_SERIALIZABLE: return TransactionLevel.SERIALIZABLE;
      case Connection.TRANSACTION_REPEATABLE_READ: return TransactionLevel.REPEATABLE_READ;
      case Connection.TRANSACTION_READ_UNCOMMITTED: return TransactionLevel.READ_UNCOMMITTED;
      case Connection.TRANSACTION_READ_COMMITTED: return TransactionLevel.READ_COMMITTED;
      case Connection.TRANSACTION_NONE: return TransactionLevel.NONE;
      default: throw new IllegalArgumentException();
    }
  }
}
