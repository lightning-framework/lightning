# Lightning

An experimental light-weight web framework for Java that expands on the Spark framework. The philosophy behind it is simple: to maximize developer productivity by making application development as convenient as possible.

# Features

  - Built-in distributed sessions
  - Built-in auth, users, groups, and permissions
  - Built-in templating with Freemarker
  - Built-in SQL support
  - Built-in email support
  - Built-in SSL support
  - Built-in support for CAS authentication
  - Built-in support for websockets
  - Built-in support for HTTP multipart
  - Built-in validator for forms
  - Built-in security features
    - Passwords are encrypted with BCrypt
    - Cookies are signed and verified with HMAC
    - Only hashes stored in the database, period
  - Powerful debug mode
    - Develop without ever restarting the server
    - Detailed in-browser stack traces on errors
  - Intelligent annotation-based routing and filters
  - Powerful type-safe HTTP abstractions

# Code Sample

```java
import lightning.mvc.*;
import static lightning.mvc.Context.*;
import static lightning.mvc.HTTPMethod.*;

// ... other imports omitted ...

@Controller
public final class TinyUrlController {
  @Route(path="/", methods={GET})
  public void home() throws Exception {
    redirectIfNotLoggedIn(url().to("/login"));
    redirect(url().to("/my"));
  }

  @Route(path="/my", methods={GET})
  @RequireAuth
  @Template("home.ftl")
  public Object handleListUrls() throws Exception {
    List<Map<String, Object>> urls = new ArrayList<>();

    try (NamedPreparedStatement query = db().prepare("SELECT * FROM tinyurls WHERE user_id = :user_id;")) {
      query.setLong("user_id", user().getId());
      try (ResultSet result = query.executeQuery()) {
        while (result.next()) {
          urls.add(new ImmutableMap.Builder<String, Object>()
              .put("code", result.getString("code"))
              .put("url", result.getString("url"))
              .put("last_updated", Time.format(result.getLong("last_updated")))
              .put("last_clicked", Time.format(result.getLong("last_clicked")))
              .put("click_count", result.getLong("click_count"))
              .put("share_url", url().to("/u/" + result.getString("code")))
              .build());
        }
      }
    }

    return ImmutableMap.of("urls", urls, "user", user().getUserName());
  }

  @Route(path="/delete", methods={POST})
  @RequireAuth
  public void handleDeleteUrl(@QParam("code") String code) throws Exception {
    validate("code").isNotEmpty().isShorterThan(50);
    badRequestIf(!passesValidation());

    try (NamedPreparedStatement query = db().prepare("DELETE FROM tinyurls WHERE code = :code AND user_id = :user_id;")) {
      query.setString("code", code);
      query.setLong("user_id", user().getId());
      accessViolationIf(query.executeUpdate() == 0);
    }

    redirect(url().to("/my"));
  }

  @Route(path="/add", methods={POST})
  @RequireAuth
  public void handleAddUrl(@QParam("code") String code, @QParam("url") String url) throws Exception {
    validate("code").isNotEmpty().isShorterThan(50).isAlphaNumeric();
    validate("url").isNotEmpty().isURL().isShorterThan(1000);
    badRequestIf(!passesValidation(), validator().getErrorsAsString());

    db().transaction(() -> {
      try (NamedPreparedStatement query = db().prepare("SELECT * FROM tinyurls WHERE code = :code;")) {
        query.setString("code", code);
        try (ResultSet result = query.executeQuery()) {
          badRequestIf(result.next(), "Provided code is already in use.");
        }
      }

      try (NamedPreparedStatement query = db().prepare("INSERT INTO tinyurls (url, code, last_updated, last_clicked, user_id) VALUES (:url, :code, :last_updated, :last_clicked, :user_id);")) {
        query.setString("url", url);
        query.setString("code", code);
        query.setLong("last_updated", Time.now());
        query.setLong("last_clicked", Time.now());
        query.setLong("user_id", user().getId());
        query.executeUpdate();
      }
    });

    redirect(url().to("/my"));
  }

  @Route(path="/modify", methods={POST})
  @RequireAuth
  public void handleModifyUrl(@QParam("code") String code, @QParam("url") String url) throws Exception {
    validate("code").isNotEmpty().isShorterThan(50);
    validate("url").isNotEmpty().isURL().isShorterThan(1000);
    badRequestIf(!passesValidation());

    try (NamedPreparedStatement query = db().prepare("UPDATE tinyurls SET url = :url, last_updated = :last_updated, last_clicked = :last_clicked WHERE code = :code AND user_id = :user_id;")) {
      query.setString("url", url);
      query.setString("code", code);
      query.setLong("last_updated", Time.now());
      query.setLong("last_clicked", Time.now());
      query.setLong("user_id", user().getId());
      badRequestIf(query.executeUpdate() == 0, "Attempted to update a non-existent or not owned code.");
    }

    redirect(url().to("/my"));
  }

  @Route(path="/u/:code", methods={GET})
  public void handleViewUrl(@RParam("code") String code) throws Exception {
    try (NamedPreparedStatement query = db().prepare("SELECT * FROM tinyurls WHERE code = :code;")) {
      query.setString("code", code);
      try (ResultSet result = query.executeQuery()) {
        notFoundIf(!result.next());

        // Update the click count.
        db().prepare("UPDATE tinyurls SET last_clicked = :last_clicked, click_count = click_count + 1 WHERE code = :code;",
            ImmutableMap.of("last_clicked", Time.now(), "code", code)).executeUpdateAndClose();

        // Redirect the user to the URL.
        redirect(result.getString("url"));
      }
    }
  }
}
```

