package lightning.http;

import static spark.Spark.exception;
import static spark.Spark.modelAndView;
import lightning.config.Config;
import lightning.debugscreen.DebugScreen;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.template.freemarker.FreeMarkerEngine;

import com.google.common.collect.ImmutableMap;

public final class Exceptions {
  private final static Logger logger = LoggerFactory.getLogger(Exceptions.class);
  
  public static void installExceptionHandlers(Config config, FreeMarkerEngine templateEngine) {
    // TODO(mschurr): If we could roll our own 404 page outside of debug mode, that'd be sweet, but I don't think it's possible w/ Spark.
    exception(BadRequestException.class, (e, req, res) -> {
      res.body(templateEngine.render(modelAndView(ImmutableMap.of(
          "status", "400",
          "status_text", "Bad Request",
          "message", e.getMessage(),
          "explanation",
          "Your request could not be serviced because it did not contain the required or "
            + "expected information. You should hit the back button on your browser and try again."),
          "error.ftl")));
      res.status(400);
    });

    exception(NotAuthorizedException.class, (e, req, res) -> {
      res.body(templateEngine.render(modelAndView(ImmutableMap.of(
          "status", "401",
          "status_text", "Unauthorized",
          "explanation", "You must authenticate to proceed. If you were previously authenticated, "
              + "your session may have expired."), "error.ftl")));
      res.status(401);
    });

    exception(AccessViolationException.class, (e, req, res) -> {
      res.body(templateEngine.render(modelAndView(ImmutableMap.of(
          "status", "403",
          "status_text", "Forbidden",
          "explanation", "You do not have permission to view this page."), "error.ftl")));
      res.status(403);
    });

    exception(NotFoundException.class, (e, req, res) -> {
      res.body(templateEngine.render(modelAndView(ImmutableMap.of(
          "status", "404",
          "status_text", "Not Found",
          "explanation", "We couldn't find the page you requested on our servers."), "error.ftl")));
      res.status(404);
    });

    exception(MethodNotAllowedException.class, (e, req, res) -> {
      res.body(templateEngine.render(modelAndView(ImmutableMap.of(
          "status", "405",
          "status_text", "Method Not Allowed",
          "explanation", "The method you have requested is not supported."), "error.ftl")));
      res.status(405);
    });

    exception(NotImplementedException.class, (e, req, res) -> {
      res.body(templateEngine.render(modelAndView(ImmutableMap.of(
          "status", "501",
          "status_text", "Not Implemented",
          "explanation", "The feature you are trying to access is currently under construction. "
              + "Please try again later."), "error.ftl")));
      res.status(501);
    });
        
    if (config.enableDebugMode) {
      exception(Exception.class, (e, req, res) -> {
        logger.error("Caught an unknown exception from a request handler: ", e);     
        DebugScreen screen = new DebugScreen();
        screen.handle(e, req, res);
      });
    } else {
      exception(Exception.class, (e, req, res) -> {
        logger.error("Caught an unknown exception from a request handler: ", e);
        res.body(templateEngine.render(modelAndView(ImmutableMap.of(
            "status", "500",
            "status_text", "Internal Server Error",
            "message", 
              config.enableDebugMode ? ExceptionUtils.getStackTrace(e) : "",
            "explanation", "Something went wrong on our servers while we were processing your "
                + "request. We're really sorry about this, and will work hard to get this resolved "
                + "as soon as possible."), "error.ftl")));
        res.status(500);
      });
    }
  }
}
