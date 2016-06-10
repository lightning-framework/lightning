package lightning.db;

import java.sql.ResultSet;

/**
 * Implements a distributed lock using a MySQL database.
 * Utilizes MySQL's GET_LOCK, RELEASE_LOCK functions.
 * Lock is bound to the specific database connection.
 * Lock is explicitly released on invoking release() or implicitly on connection closed/disconnect.
 * 
 * This lock is not safe to utilize for distributed coordination other than guaranteeing an
 * exclusive right to execute database queries on the connection from which the lock was
 * acquired between when the lock is acquired and released (implicitly or explicitly).
 * @see http://dev.mysql.com/doc/refman/5.7/en/miscellaneous-functions.html#function_get-lock
 * @see https://martin.kleppmann.com/2016/02/08/how-to-do-distributed-locking.html
 */
public final class DistributedLock {
  private final MySQLDatabase db;
  private final String lockName;
  
  
  public DistributedLock(MySQLDatabase db, String lockName) {
    this.db = db;
    this.lockName = lockName;
  }
  
  public boolean tryLock() throws Exception {
    try (NamedPreparedStatement stmt = db.prepare("SELECT GET_LOCK(:lock, 0);")) {
      stmt.setString("lock", lockName);
      try (ResultSet result = stmt.executeQuery()) {
        // 1 if success, 0 if timed out, NULL if error.
        if (!result.next()) {
          return false;
        }
        
        int code = result.getInt(1);
        
        if (result.wasNull()) {
          return false;
        }
        
        if (code == 1) {
          return true;
        }
        
        return false;
      }
    }
  }
  
  public void release() throws Exception {
    try (NamedPreparedStatement stmt = db.prepare("SELECT RELEASE_LOCK(:lock);")) {
      stmt.setString("lock", lockName);
      try (ResultSet result = stmt.executeQuery()) {
        // 1 if released, 0 if not established by thread, NULL if not exists.
      }
    }
  }
}
