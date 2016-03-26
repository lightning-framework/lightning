package lightning.auth;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import lightning.crypt.SecureCookieManager.InsecureCookieException;
import lightning.sessions.Session;
import lightning.sessions.Session.SessionException;
import lightning.users.User;
import lightning.users.Users;
import lightning.users.Users.UsersException;
import lightning.util.Time;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;

/**
 * Provides an implementation of authentication for the Spark web framework.
 * The authentication implementation supports both temporary logins and persistent logins, as well
 * as a variety of advanced security features (e.g. throttling).
 * 
 * Usage:
 * - Create an Auth object for current request using Auth.forSession(session);
 * - Call various methods on Auth (e.g. Auth.isLoggedIn(), Auth.getUser());
 */
public final class Auth {
  // a[i] gives throttling time in seconds after i failed attempts; if > i failed attempts, then uses last value in list.
  private static final List<Integer> THROTTLING_SECONDS = ImmutableList.of(0, 0, 0, 2, 4, 8, 16, 30, 60, 120, 300);
  
  // Number of bytes to use in auth token; note that tokens stored in DB are BASE64 encoded.
  private static final int AUTH_TOKEN_BYTES = 128;
  
  // Reference to shared authentication driver instance.
  private static AuthDriver sharedDriver;
  
  /**
   * Sets the driver that will be used for authenticating users.
   * @param driver An authentication driver.
   */
  public static void setDriver(AuthDriver driver) {
    sharedDriver = driver;
  }
  
  /**
   * Checks whether or not an auth driver is installed; throws runtime exception if no driver.
   */
  private static void checkDriver() {
    if (sharedDriver == null) {
      throw new RuntimeException("Error: Must call Auth.setDriver() before using Auth.");
    }
  }
  
  /**
   * An interface that must be implemented by drivers.
   * A single instance will be used; drivers must be thread-safe.
   */
  public interface AuthDriver {
    /**
     * @param userId A user ID.
     * @param time A unix timestamp.
     * @return Number of failed login attempts occurring after time and unix time of most recent failed login attempt
     * occurring after time on the given user.
     * @throws Exception On I/O failure.
     */
    public AuthCountAndTimestamp getFailedLoginCountSince(long userId, long time) throws Exception;
    
    /**
     * @param ip An IP address.
     * @param time A unix timestamp.
     * @return Number of failed login attempts occurring after time and unix time of most recent failed login attempt
     * occurring after time on the given IP.
     * @throws Exception On I/O failure.
     */
    public AuthCountAndTimestamp getFailedLoginCountSince(String ip, long time) throws Exception;

    /**
     * Stores the provided login attempt in the database.
     * @param attempt To be written.
     * @throws Exception On I/O failure.
     */
    public void saveAuthAttempt(AuthAttempt attempt) throws Exception;
    
    public void terminateAllSessionsForUser(long userId) throws Exception;
    public void terminateAllSessionsForUserExcepting(long userId, String hashedIdToIgnore) throws Exception;
    public long getSuccessfulLoginCountSince(long userId, long time) throws Exception;
    public long getFraudulentLoginCountSince(long userId, long time) throws Exception;
    public long getSuccessfulLoginCountSince(String ip, long time) throws Exception;
    public long getFraudulentLoginCountSince(String ip, long time) throws Exception;
    public void updateSessionAuthToken(AuthToken token) throws Exception;
    public void insertSessionAuthToken(AuthToken token) throws Exception;
    public void deleteSessionAuthToken(AuthToken token) throws Exception;
    public void updatePersistentAuthToken(AuthToken token) throws Exception;
    public void insertPersistentAuthToken(AuthToken token) throws Exception;
    public void deletePersistentAuthToken(AuthToken token) throws Exception;
    public AuthToken getPersistentAuthToken(String hashedId) throws Exception;
    public AuthToken getSessionAuthToken(String hashedId) throws Exception;
  }
  
  /**
   * Hashes a token, returning an encoded string.
   * @param plaintextToken Raw authentication token
   * @return Hashed authentication token.
   */
  private static String hashToken(String plaintextToken) {
    return Hashing.sha256().hashString(plaintextToken, Charsets.UTF_8).toString();
  }
  
  /**
   * Generates a random authentication token.
   * @return Raw (un-encrypted) authentication token.
   */
  public static String generateToken() {
    while (true) {
      SecureRandom generator = new SecureRandom();
      byte bytes[] = new byte[AUTH_TOKEN_BYTES];
      generator.nextBytes(bytes);
      String token = Base64.getEncoder().encodeToString(bytes);
      
      if (tokenIsInUse(token)) {
        continue; // Pick again.
      }
      
      return token;
    }
  }
  
  /**
   * Returns whether or not an authentication token is in use.
   * @param token Raw authentication token.
   * @return Whether or not the token is safe to use.
   */
  private static boolean tokenIsInUse(String token) {
    return false; // TODO(mschurr): Should check DB for duplicates.
  }
  
  /**
   * Returns a new instance of Auth for the given session.
   * @param session
   * @return
   */
  public static Auth forSession(Session session) {
    if (sharedDriver == null) {
      throw new RuntimeException("Must call Auth.setDriver() before calling Auth.forSession().");
    }
    
    return new Auth(session, sharedDriver);
  }
  
  /**
   * Terminates all sessions authenticated to the given user (including persistent tokens).
   * @param user To terminate sessions for.
   * @throws AuthException On driver failure.
   */
  public static void terminateAllSessionsForUser(User user) throws AuthException {
    checkDriver();
    
    try {
      sharedDriver.terminateAllSessionsForUser(user.getId());
    } catch (AuthException e) {
      throw e;
    } catch (Exception e) {
      throw new AuthException(AuthException.Type.DRIVER_ERROR, e);
    }
  }

  /**
   * Returns how many seconds the user should be throttled after attemptNumber failed attempts.
   * @param attemptNumber
   * @return Number of seconds to throttle the user for.
   */
  private static long getThrottlingSeconds(long attemptNumber) {
    if (attemptNumber <= 0) {
      return THROTTLING_SECONDS.get(0);
    } else if (attemptNumber >= THROTTLING_SECONDS.size()) {
      return THROTTLING_SECONDS.get(THROTTLING_SECONDS.size() - 1);
    } else {
      return THROTTLING_SECONDS.get(((int) attemptNumber) - 1);
    }
  }
  
  // Names of keys used in session to remember auth information.
  private static final String LAST_PASSWORD_TIME_KEY = "$$auth-last-entered-password";
  private static final String USER_ID_KEY = "$$auth-user-id";
  private static final String LAST_ACTIVE_KEY = "$$auth-last-active";
  private static final String AUTH_TOKEN_KEY = "$$auth-token-key";
  
  // Name of the cookie storing persistent auth tokens.
  private static final String TOKEN_COOKIE_NAME = "authd";
  
  // How long persistent tokens should last before requiring a re-login.
  private static final long PERSISTENT_TIMEOUT = 60 * 60 * 24 * 30; // in seconds (30 days)
  
  // How long one-time sessions should last before timing out.
  private static final long SESSION_TIMEOUT = 60 * 60; // in seconds (1 hr)
  
  // How far in the past we should look when deciding to throttle.
  private static final long THROTTLING_PERIOD = 60 * 15; // in seconds (15 min)
  
  private final AuthDriver driver;
  private final Session session;
  private User user;
  private boolean hasTriedAuth;
  
  private Auth(Session session, AuthDriver driver) {
    this.session = session;
    this.driver = driver;
    user = null;
    hasTriedAuth = false;
  }
  
  /**
   * @return Whether or not the session is currently authenticated to some user.
   * @throws SessionException 
   * @throws AuthException 
   */
  public boolean isLoggedIn() throws SessionException, AuthException {
    tryAuth();
    return user != null;
  }
  
  /**
   * @return The user the session is authenticated to (null if not logged in).
   * @throws SessionException 
   * @throws AuthException 
   */
  public User getUser() throws SessionException, AuthException {
    tryAuth();
    return user;
  }
  
  /**
   * Tries to authenticate the given session to some user.
   * Only tries the first time called.
   * @throws SessionException 
   * @throws AuthException 
   */
  private void tryAuth() throws SessionException, AuthException {
    if (hasTriedAuth) {
      return;
    }
    
    hasTriedAuth = true;
    
    // Try first to login using data stored in current session.
    tryFromSession();
    
    // If that didn't work, then try to login using persistent auth cookie.
    if (user == null)
      tryFromPersistentToken();
    
    // And make sure we can't log in as a banned user.
    if (user != null && user.isBanned())
      user = null;
  }
  
  /**
   * Returns whether or not the user has entered their password during this session within the past 60 minutes.
   * Use this to restrict access to certain functions following re-authentication from a persistent token.
   * EXAMPLE USE CASE: If the user's session timed out and they were re-logged in from a persistent token,
   * then require them to re-enter their password (and check it with checkPassword()) before allowing them
   * to perform important actions (such as making a payment or changing profile information).
   * @throws SessionException 
   * @throws AuthException 
   */
  public boolean hasEnteredPasswordRecently() throws SessionException, AuthException {
    tryAuth();
    
    if (user == null) {
      return false;
    }
    
    if (!session.has(LAST_PASSWORD_TIME_KEY)) {
      return false;
    }
    
    if (Time.now() - session.getLong(LAST_PASSWORD_TIME_KEY) > SESSION_TIMEOUT) {
      return false;
    }
    
    return true;
  }
  
  /**
   * TODO(mschurr): Does this work correctly with load balancers and X-Forwarded-From?
   * @return The IP attached to current session.
   */
  private String getIP() {
    return session.getRequest().ip();
  }
  
  /**
   * Checks whether or not the provided password is correct for the account this session is currently logged into. 
   * Returns true on success.
   * If incorrect, returns false. 
   * An exception is always thrown if the current session is not logged in.
   * This function is subject to security measures on the current client/session and associated user account (e.g. throttling) and should be used instead
   * of the User service methods when such measures are required (i.e. only on user input such as change password forms). 
   * Will indicate that user has entered their password this session.
   * @param plaintextPassword
   * @return Whether or not the correct password was entered.
   * @throws AuthException
   * @throws SessionException 
   */
  public boolean checkPassword(String plaintextPassword) throws AuthException, SessionException {
    tryAuth();
    
    // Throw an error if session is not logged in.
    if (user == null) {
      throw new AuthException(AuthException.Type.NO_USER, "You are not logged in.");
    }
    
    // Throttle password attempts based on IP. This will prevent someone from doing something like using a change password
    // form to brute-force the password for an account they are already logged into (because maybe they stole an auth token
    // or physically walked up to someone's unlocked computer).
    // TODO: Throttling should extend to session, too, because sessions can change IP.
    throttleIP();
    
    // If the password is not correct:
    if (!user.checkPassword(plaintextPassword)) {
      try {
        // Log the attempt (for purposes of brute-force protection).
        driver.saveAuthAttempt(AuthAttempt.createFailed(user.getId(), getIP(), Time.now()));
      } catch (Exception e) {
        throw new AuthException(AuthException.Type.DRIVER_ERROR, "Failed to save auth attempt.");
      }
      
      return false;
    }
    
    // Indicate that the last time the password was entered as a result of user interaction is now.
    session.set(LAST_PASSWORD_TIME_KEY, Time.now());
    return true;
  }
  
  /**
   * Throws an exception if throttling should be performed on the given IP address.
   * @throws AuthException Iff IP should be throttled for the given ip.
   */
  private void throttleIP() throws AuthException {
    // Fetch the number of failed login attempts on that IP in the past 15 minutes AND the unix time of the 
    // most recent failed login attempt.
    AuthCountAndTimestamp info;
    try {
      info = driver.getFailedLoginCountSince(getIP(), Time.now() - THROTTLING_PERIOD);
    } catch (Exception e) {
      throw new AuthException(AuthException.Type.DRIVER_ERROR, e);
    }

    // Determine how many sections of throttling (if any) should occur based on the number of failed attempts.
    long ipThrottleDelay = getThrottlingSeconds(info.rowCount());

    // If the throttling delay is non-zero and the time we must wait until before trying again is after the current time:
    if (ipThrottleDelay > 0 && info.maxTimestamp() + ipThrottleDelay > Time.now()) {
      // Then perform throttling.
      throw new AuthException(AuthException.Type.IP_THROTTLED, "You must wait before trying again."); 
    }
  }
  
  /**
   * Throws an exception if throttling should be performed on the given user account.
   * @param userId To throttle for.
   * @throws AuthException Iff login attempts should be throttled for the given user.
   */
  private void throttleUser(long userId) throws AuthException {
    // Fetch the number of failed login attempts on that account in the past 15 minutes AND the unix time of the 
    // most recent failed login attempt.
    AuthCountAndTimestamp info;
    try {
      info = driver.getFailedLoginCountSince(userId, Time.now() - THROTTLING_PERIOD);
    } catch (Exception e) {
      throw new AuthException(AuthException.Type.DRIVER_ERROR, e);
    }
    
    // Determine how many sections of throttling (if any) should occur based on the number of failed attempts.
    long userThrottleDelay = getThrottlingSeconds(info.rowCount());
    
    // If the throttling delay is non-zero and the time we must wait until before trying again is after the current time:
    if (userThrottleDelay > 0 && info.maxTimestamp() + userThrottleDelay > Time.now()) {
      // Then perform throttling.
      throw new AuthException(AuthException.Type.USER_THROTTLED, "You must wait before trying again."); 
    }
  }
  
  /**
   * Terminates all active sessions and persistent tokens for the current user account (excluding this one).
   * Has no effect unless isLoggedIn() = true.
   * @throws SessionException 
   */
  public void terminateOtherSessions() throws AuthException, SessionException {
    tryAuth();
    
    // If we are not logged in, there is no work to be done.
    if (user == null) {
      return;
    }
    
    // TODO(mschurr) Implement.
    throw new AuthException(AuthException.Type.DRIVER_ERROR, "terminateOtherSessions not implemented.");
  }
  
  /**
   * Terminates the current user's session and (optionally) invalidates any persistent tokens associated with the current client.
   * @param shouldKillPersistentTokens Whether to invalidate persistent auth token cookies.
   * @throws SessionException 
   * @throws InsecureCookieException 
   */
  public void logout(boolean shouldKillPersistentTokens) throws SessionException, AuthException {
    tryAuth();
    
    // If we're not logged in, there's nothing to be done.
    if (user == null) {
      return;
    }
     
    try {
      // If we should invalidate persistent tokens AND the user has a persistent token set:
      if (shouldKillPersistentTokens && session.getCookieManager().has(TOKEN_COOKIE_NAME)) {
        // Delete the persistent token cookie.
        session.getCookieManager().delete(TOKEN_COOKIE_NAME);
        
        // And delete the token from the database.
        driver.deletePersistentAuthToken(new AuthToken(hashToken(session.getCookieManager().get(TOKEN_COOKIE_NAME))));
      }
    
      // Delete the session token from the database (if any).
      if (session.has(AUTH_TOKEN_KEY)) {
        driver.deleteSessionAuthToken(new AuthToken(session.getString(AUTH_TOKEN_KEY)));
      }
    } catch (Exception e) {
      throw new AuthException(AuthException.Type.DRIVER_ERROR, e);
    }
    
    // Reset all variables storing login info on local session.
    session.forget(LAST_ACTIVE_KEY);
    session.forget(AUTH_TOKEN_KEY);
    session.forget(LAST_PASSWORD_TIME_KEY);
    session.forget(USER_ID_KEY);
    session.save();
    
    // Reset local variables.
    user = null;
  }
  
  /**
   * Tries to perform a login using the information stored in session (if any).
   * @throws SessionException
   * @throws AuthException
   */
  private void tryFromSession() throws SessionException, AuthException {
    // Verify that the session has data about being logged in.
    if (session.has(AUTH_TOKEN_KEY)
        && session.has(LAST_ACTIVE_KEY)
        && session.has(USER_ID_KEY)) {
      // Check if the session has expired.
      if (Time.now() - session.getLong(LAST_ACTIVE_KEY) > SESSION_TIMEOUT) {
        this.logout(false); // Reset these session variables if so.
        return;
      }
      
      // Check that the token exists and is not revoked. 
      // Reset these session variables if so.
      
      // Fetch token from db.
      AuthToken token;
      try {
        token = driver.getSessionAuthToken(session.getString(AUTH_TOKEN_KEY));
      } catch (Exception e) {
        throw new AuthException(AuthException.Type.DRIVER_ERROR, e);
      }
      
      // Verify token exists.
      if (token == null) {
        this.logout(false);
        return;
      }
      
      // Verify userId matches.
      if (token.userId != session.getLong(USER_ID_KEY)) {
        this.logout(false);
        return;
      }
      
      // Verify not expired.
      if (Time.now() >= token.expireTime) {
        this.logout(false);
        return;
      }
      
      // Otherwise, we are logged in.
      session.set(LAST_ACTIVE_KEY, Time.now());
      
      try {
        // Note: if the user does not exist, we won't be logged in (user will be null). This is intended.
        user = Users.getById(session.getLong(USER_ID_KEY));
      } catch (Exception e) {
        throw new AuthException(AuthException.Type.DRIVER_ERROR, e);
      }
    }
  }

  /**
   * Tries to perform a login using the persistent authentication cookie (if any).
   * @throws AuthException
   * @throws SessionException
   */
  private void tryFromPersistentToken() throws AuthException, SessionException {
    // If an persistent auth token cookie does not exist, then we can't do anything.
    if (!session.getCookieManager().has(TOKEN_COOKIE_NAME)) {
      return;
    }
    
    // Throttle logins based on IP address; this will prevent users from attempting to
    // brute-force logins using a persistent auth token cookie.
    throttleIP();
    
    // Check that the token exists in the database and has not been revoked or expired.
    AuthToken token;
    try {
      token = driver.getPersistentAuthToken(hashToken(session.getCookieManager().get(TOKEN_COOKIE_NAME)));
    } catch (Exception e) {
      throw new AuthException(AuthException.Type.DRIVER_ERROR, e);
    }
    
    if (token == null) {
      try {
        // Log the failed attempt (important for preventing brute-force).
        driver.saveAuthAttempt(AuthAttempt.createFailed(getIP(), Time.now()));
      } catch (Exception e) {
        throw new AuthException(AuthException.Type.DRIVER_ERROR, e);
      }
      
      // Delete the cookie so that the user does not get stuck in an infinite loop.
      session.getCookieManager().delete(TOKEN_COOKIE_NAME);
      return;
    }
    
    // Verify that the user exists.
    User user;
    try {
      user = Users.getById(token.userId);
    } catch (UsersException e) {
      throw new AuthException(AuthException.Type.DRIVER_ERROR, e);
    }
    
    if (user == null) {
      try {
        // Log the failed attempt (important for preventing brute-force).
        driver.saveAuthAttempt(AuthAttempt.createFailed(getIP(), Time.now()));
      } catch (Exception e) {
        throw new AuthException(AuthException.Type.DRIVER_ERROR, e);
      }
      
      // Delete the cookie so that the user does not get stuck in an infinite loop.
      session.getCookieManager().delete(TOKEN_COOKIE_NAME);
      return;
    }
    
    // Update the current session to be logged in as the user indicated by the token.
    // Allocate and set a new persistent token for when/if the current session expires.
    loginAs(user, true, true);
  }
  
  /**
   * Logs the session in as the provided user. 
   * USE WITH CAUTION - NOT SUBJECT TO SECURITY MEASURES AND DOES NOT VERIFY PASSWORD. 
   * Will indicate that user has entered their password this session if not from token and call recordLogin on the user service.
   * @param user To log in as.
   * @param isPersistent Whether or not to set a persistent authentication token.
   * @param isFromToken Whether or not this login occurred from a persistent authentication token.
   * @throws SessionException 
   * @throws AuthException 
   */
  public void loginAs(User user, boolean isPersistent, boolean isFromToken) throws SessionException, AuthException {
    try {
      // Generate and store an authentication token for the current login session.
      String sessionTokenId = Auth.generateToken();
      AuthToken sessionToken = new AuthToken(hashToken(sessionTokenId), user.getId(), Time.now() + PERSISTENT_TIMEOUT);
      driver.insertSessionAuthToken(sessionToken);
      
      // If the login occurred from a persistent token:
      if (isFromToken && session.getCookieManager().has(TOKEN_COOKIE_NAME)) {
        // Invalidate the old token (it's good for a single use).
        driver.deletePersistentAuthToken(new AuthToken(hashToken(session.getCookieManager().get(TOKEN_COOKIE_NAME))));
      }
      
      // If this login should be persistent:
      if (isPersistent) {
        // Create and store a persistent authentication token for this account.
        String persistentTokenId = Auth.generateToken();
        AuthToken persistentToken = new AuthToken(hashToken(persistentTokenId), user.getId(), Time.now() + PERSISTENT_TIMEOUT);
        driver.insertPersistentAuthToken(persistentToken);
        
        // And store the persistent token as a cookie.
        session.getCookieManager().set(TOKEN_COOKIE_NAME, persistentTokenId);
      }
      
      // Update local variables.
      this.user = user;
      
      // Update the current session with the session token.
      session.set(AUTH_TOKEN_KEY, sessionToken.hashedToken);
      session.set(LAST_ACTIVE_KEY, Time.now());
      session.set(USER_ID_KEY, user.getId());
      
      if (!isFromToken) {
        // Indicate that the last time the password was entered as a reuslt of user interaction is now.
        // This only occurs from logins by calling loginAs directly or from calling attempt. Does not occur
        // when logging in from tokens (as the user DID NOT enter an actual password in that case).
        session.set(LAST_PASSWORD_TIME_KEY, Time.now());
      }
      
      session.regenerateId(); // To prevent session fixation attacks.
      session.save();
      
      // Log the successful attempt.
      driver.saveAuthAttempt(AuthAttempt.createSuccessful(user.getId(), getIP(), Time.now()));
      
      // Inform the driver to perform any record-keeping.
      Users.recordLogin(user);
    } catch (SessionException e) {
      throw e; // Re-throw.
    } catch (Exception e) {
      throw new AuthException(AuthException.Type.DRIVER_ERROR, e);
    }
  }
  
 /**
  * Attempts to log the client in to the user with the provided user name and password. 
  * You may pass any extra information required by your driver into extraInfo.
  * On success, modifies the current session to be logged in and optionally sets a persistent token.
  * On failure, throws an AuthException containing information about what went wrong (e.g. incorrect password).
  * This function is subject to security measures on the current client/session and associated user account (e.g. throttling).
  * On success, this function will indicate that the user has entered their password this session and call recordLogin on the user service.
  * @param userName
  * @param plaintextPassword
  * @param isPersistent Whether or not to remember the user past session timeouts.
  * @param extraInfo For the authentication driver.
  * @throws AuthException
  * @throws UsersException 
  * @throws SessionException 
  */
  public void attempt(String userName, String plaintextPassword, boolean isPersistent, Map<String, ?> extraInfo) throws AuthException, UsersException, SessionException {
    // Require a user name.
    if (userName == null) {
      throw new AuthException(AuthException.Type.INVALID_USERNAME, "You must enter a username.");
    }
    
    // Require a password.
    if (plaintextPassword == null) {
      throw new AuthException(AuthException.Type.INVALID_PASSWORD, "You must enter a password.");
    }
    
    // Throttle login attempts based on attempts made by the user's IP address.
    throttleIP();
    
    // Fetch the user.
    User user = Users.getByName(userName);
    
    // Check that the user exists.
    if (user == null) {
      // If not, log the failed attempt and throw an error.
      try {
        driver.saveAuthAttempt(AuthAttempt.createFailed(getIP(), Time.now()));
      } catch (Exception e) {
        throw new AuthException(AuthException.Type.DRIVER_ERROR, e);
      }
      
      throw new AuthException(AuthException.Type.INVALID_USERNAME, "No user exists with that name.");
    }
    
    // Throttle login attempts based on the user account.
    // This prevents distributed brute force attacks on a single account. This also makes it possible to lock a
    // user out of their account by performing bogus login requests. TODO(mschurr): Provide email method to
    // bypass the throttling.
    throttleUser(user.getId());
    
    // Check that the password is correct.
    if (user.checkPassword(plaintextPassword) == false) {
      // If not, log the failed attempt and throw an error.
      try {
        driver.saveAuthAttempt(AuthAttempt.createFailed(user.getId(), getIP(), Time.now()));
      } catch (Exception e) {
        throw new AuthException(AuthException.Type.DRIVER_ERROR, e);
      }
      
      throw new AuthException(AuthException.Type.INVALID_PASSWORD, "You entered an incorrect password for user '" + user.getUserName() + "'.");
    }
    
    // Check that the user is not banned.
    if (user.isBanned()) {
      throw new AuthException(AuthException.Type.USER_BANNED, "That user is banned.");
    }
    
    // Update the user's session and (optionally) install a persistent token.
    this.loginAs(user, isPersistent, false);
  }
}
