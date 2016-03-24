package lightning.plugins.cas;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lightning.mvc.Param;
import lightning.mvc.URLGenerator;
import lightning.util.HTTP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Request;
import spark.Response;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

/**
 * Central Authentication Service (CAS) Link Library
 *
 * Login Protocol Explanation:
 *   1) User visits login page our website.
 *      IF: the user's session has a CAS user attached, then skip to step (6).
 *   2) User is redirected to $CAS_BASE/login?service=%s
 *   3) User authenticates with CAS and is redirected back to our site with the following parameter:
 *        ticket = (string)
 *   4) Server verifies the login by contacting $CAS_BASE/serviceValidate?ticket=%s&service=%s
 *      CAS server returns XML including user's name <cas:user> and (optional) attributes.
 *      NOTE: Must be able to establish secure SSL connection to the CAS server for this to work.
 *      IF: ticket verification fails (CAS server returns nothing), then display an error page and abort.
 *   5) User's session is regenerated and has the CAS user attached.
 *   6) User is forwarded by our site to final location.
 *
 * Logout Protocol Explanation:
 *   1) User visits logout page on our website.
 *   2) User's session is destroyed.
 *   3) User is redirected to $CAS_BASE/logout?service=%s to terminate their session with CAS.
 *   4) User is redirected back to our site from CAS.
 *   5) User is redirected by our site to final location.
 *
 */
public final class CASAuthenticator {
  private final static Logger logger = LoggerFactory.getLogger(CASAuthenticator.class);
  private static final String CAS_TICKET_PARAM = "ticket";
  private static final String CAS_SERVICE_PARAM = "service";
  private static final String CAS_PATH_LOGIN = "/login";
  private static final String CAS_PATH_LOGOUT = "/logout";
  private static final String CAS_PATH_VERIFY = "/serviceValidate";
  private static final String REDIRECT_PARAM = "destination";
  
  public static CASAuthenticator newAuthenticator(CASConfig config) {
    return new CASAuthenticator(config);
  }
  
  private final CASConfig config;
  
  private CASAuthenticator(CASConfig config) {
    this.config = config;
  }
  
  
  /**
   * @return The base URL of the CAS server.
   */
  private String getBaseUrl() {
    return "https://" + config.host + config.path;
  }
  
  private String getLoginUrl(Request request, String destinationUrl) throws UnsupportedEncodingException {
    return String.format("%s%s?%s=%s", 
        getBaseUrl(), 
        CAS_PATH_LOGIN, 
        CAS_SERVICE_PARAM, 
        URLEncoder.encode(getServiceUrl(request, destinationUrl), "UTF-8"));
  }
  
  private String getServiceUrl(Request request, String destinationUrl) throws UnsupportedEncodingException {
    String currentUrl = request.url().contains("?") ?
        request.url().substring(0, request.url().indexOf("?")) :
        request.url();
        
    currentUrl = currentUrl.endsWith("/") ? currentUrl : currentUrl + "/";
    
    return String.format("%s?%s=%s", 
        currentUrl,
        REDIRECT_PARAM,
        URLEncoder.encode(destinationUrl, "UTF-8"));
  }
  
  private String getLogoutUrl(Request request, String destinationUrl) throws UnsupportedEncodingException {
    return String.format("%s%s?%s=%s", 
        getBaseUrl(), 
        CAS_PATH_LOGOUT, 
        CAS_SERVICE_PARAM, 
        URLEncoder.encode(getServiceUrl(request, destinationUrl), "UTF-8"));
  }
  
  private String getVerificationUrl(Request request, String ticket, String destinationUrl) throws UnsupportedEncodingException {
    return String.format("%s%s?%s=%s&%s=%s", 
        getBaseUrl(), 
        CAS_PATH_VERIFY, 
        CAS_TICKET_PARAM, 
        URLEncoder.encode(ticket, "UTF-8"),
        CAS_SERVICE_PARAM,
        URLEncoder.encode(getServiceUrl(request, destinationUrl), "UTF-8"));
  }
  
  public void endAuthentication(Request request, Response response, String destinationUrl) throws UnsupportedEncodingException {
    logger.info("Ending CAS Session -> {}", getLogoutUrl(request, destinationUrl));
    response.redirect(getLogoutUrl(request, destinationUrl), 302);
  }
  
  private boolean hasToken(Request request) {
    return queryParam(request, CAS_TICKET_PARAM).isNotEmpty();
  }
  
  private Param queryParam(Request request, String name) {
    return Param.wrap(name, request.queryParams(name));
  }
  
  public Optional<CASUser> startAuthentication(Request request, Response response, String destinationUrl) throws Exception {
    logger.info("Attempting CAS Authentication...");
    
    // Read the ticket (if present) and validate it.
    Optional<CASUser> user = verifyAuthentication(request);
    if (user.isPresent()) {
      // The ticket was valid and authentication is confirmed.
      return user;
    } else {
      logger.info("No CAS session found. -> {}", getLoginUrl(request, destinationUrl));
      response.redirect(getLoginUrl(request, destinationUrl), 302);
      return Optional.absent();
    }
  }
  
  private Optional<CASUser> verifyAuthentication(Request request) throws Exception {
    if (!hasToken(request)) {
      return Optional.absent();
    }
    
    String ticket = queryParam(request, CAS_TICKET_PARAM).stringValue();
    String destinationUrl = queryParam(request, REDIRECT_PARAM).stringOption().or(URLGenerator.forRequest(request).to("/"));
    String verificationUrl = getVerificationUrl(request, ticket, destinationUrl);
    logger.info("Found ticket. Verifying {} -> {}", ticket, verificationUrl);
    
    
    String response;
    
    try {
      response = HTTP.GET(verificationUrl);
    } catch (Exception e) {
      logger.info("Connection failed.", e);
      return Optional.absent();
    }
    
    logger.info("Got Response: {}", response);
    
    /*
     NOTE: The response body should be of the form:
    <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
    <cas:authenticationSuccess>
      <cas:user>XXXX</cas:user>
    </cas:authenticationSuccess>
    */
    
    if (!response.contains("<cas:authenticationSuccess>")) {
      logger.info("Authentication failed (no success response from server).");
      return Optional.absent();
    }
    
    final Pattern pattern = Pattern.compile("<cas:user>([A-Za-z0-9]+)</cas:user>");
    final Matcher matcher = pattern.matcher(response);
    
    if (!matcher.find()) {
      logger.info("Authentication failed (no cas user found)");
      return Optional.absent();
    }
    
    String username = matcher.group(1);

    // TODO(mschurr): The CAS protocol support transferring a property map... this isn't implemented here.
    CASUser user = new CASUser(username, ImmutableMap.of(), destinationUrl);
    logger.info("CAS session found for {} -> {}", user.username, user.destinationUrl);
    
    return Optional.of(user);
  }
}
