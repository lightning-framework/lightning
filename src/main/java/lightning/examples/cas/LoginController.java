package lightning.examples.cas;

import static lightning.server.Context.auth;
import static lightning.server.Context.redirect;
import static lightning.server.Context.redirectIfLoggedIn;
import static lightning.server.Context.redirectIfNotLoggedIn;
import static lightning.server.Context.request;
import static lightning.server.Context.response;
import static lightning.server.Context.url;
import lightning.ann.Controller;
import lightning.ann.Initializer;
import lightning.ann.Route;
import lightning.enums.HTTPMethod;
import lightning.plugins.cas.CASAuthenticator;
import lightning.plugins.cas.CASConfig;
import lightning.plugins.cas.CASUser;
import lightning.users.User;
import lightning.users.Users;

import com.google.common.base.Optional;

@Controller
public class LoginController {
  protected CASConfig casConfig;
  protected CASAuthenticator casAuth;
  protected final String DOMAIN = "@rice.edu";
  protected final String PASSWORD = "12kasf8u1"; // Doesn't matter, because never used.
  
  @Initializer
  public void initialize() {
    casConfig = CASConfig.newBuilder()
        .setHost("netid.rice.edu")
        .setPath("/cas")
        .build();
    
    casAuth = CASAuthenticator.newAuthenticator(casConfig);
  }
  
  @Route(path="/login", methods={HTTPMethod.GET})
  public void handleLogin() throws Exception {
    redirectIfLoggedIn(url().to("/"));
    
    // Attempt to log in.
    Optional<CASUser> casUserOpt = casAuth.startAuthentication(request(), response(), url().to("/"));
    
    if (casUserOpt.isPresent()) {
      // If we were successful in logging in, fetch the user data from CAS.
      CASUser casUser = casUserOpt.get();
      
      // Convert the CAS user to a native user.
      // Re-use the existing user record (if present), otherwise create a new user.
      // Note: We're just using a fixed constant password here, because the password will never be used
      // for anything since all authentication happens through CAS anyways but the native API requires it.
      Optional<User> userOpt = Optional.fromNullable(Users.getByName(casUser.username));
      User user = userOpt.isPresent() ? 
          userOpt.get() : 
          Users.create(casUser.username, casUser.username + DOMAIN, PASSWORD);
      
      // Log the user into the native user account that we created based on their CAS account.
      // Use attempt(...) to subject to security measures including throttling.
      auth().attempt(user.getUserName(), PASSWORD, true, null);
      
      // Redirect the user to their final destination.
      redirect(casUser.destinationUrl);
    }
  }
  
  @Route(path="/logout", methods={HTTPMethod.GET})
  public void handleLogout() throws Exception {
    redirectIfNotLoggedIn(url().to("/"));
    
    // Terminate the application session.
    auth().logout(true);
    
    // Terminate the CAS session.
    casAuth.endAuthentication(request(), response(), url().to("/"));
  }
}
