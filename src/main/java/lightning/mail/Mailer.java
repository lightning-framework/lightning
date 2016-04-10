package lightning.mail;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an Email Sending System.
 * Wraps the JavaMail API to provide a better interface.
 * 
 * Usage:
 * Message message = mailer().createMessage();
 * ... mutate the message ...
 * mailer().send(message);
 */
public class Mailer {
  private static final Logger logger = LoggerFactory.getLogger(Mailer.class);
  private ExecutorService pool = null;
  private Session session = null;
  private InternetAddress from = null;
  
  /**
   * Creates and returns a new email that can be send via Mail.send().
   * The message is sent from the default email address for the application.
   * The message will automatically have its send date and from fields filled out.
   * @return A new email that can be mutated.
   * @throws MessagingException On failure.
   */
  public Message createMessage() throws MessagingException {
    if (from == null || session == null) {
      throw new MessagingException("Must call Mail.configure(...) before creating Mail.");
    }
    
    return new Message(session, from);
  }
  
  /**
   * Enqueues a message for delivery.
   * Delivery of a message is not guaranteed unless the returned future resolves to true.
   * @param message The message to be sent.
   * @return A future that resolves when the message is sent successfully or fails to send.
   *         Failures will be logged; it is not recommended that clients wait upon this future,
   *         particularly if they are servicing a web request.
   */
  public Future<Boolean> sendAsync(Message message) {
    return pool.submit(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        try {
          message.send();
          return true;
        } catch (Exception e) {
          logger.error("Failed to send an email.", e);
          throw e;
        }
      }
    });
  }
  
  /**
   * Sends a message immediately. Blocks the calling thread until complete.
   * @param message The message to be sent.
   * @throws Exception If the message was not sent successfully.
   */
  public void send(Message message) throws Exception {
    sendAsync(message).get();
  }
  
  /**
   * Configures the mail system.
   * @param config
   * @throws AddressException
   * @throws UnsupportedEncodingException
   */
  public Mailer(final MailerConfig config) throws AddressException, UnsupportedEncodingException {
    Properties props = new Properties();
    from = new InternetAddress(config.getAddress(), config.getAddress());
    pool = Executors.newFixedThreadPool(10);
    props.put("mail.smtp.host", config.getHost());
    props.put("mail.smtp.port", Integer.toString(config.getPort()));
    // props.put("mail.smtp.starttls.enable", true);
    props.put("mail.smtp.ssl.enable", config.useSSL() ? "true" : "false");
    props.put("mail.smtp.socketFactory.port", Integer.toString(config.getPort()));
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    props.put("mail.smtp.socketFactory.fallback", "false");
    props.put("mail.smtp.auth", "true");
    
    // prop.put("mail.smtp.connectiontimeout", 1000);
    // prop.put("mail.smtp.timeout", 1000);
    
    session = javax.mail.Session.getDefaultInstance(props,
      new javax.mail.Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(config.getUsername(), config.getPassword());
        }
      }
    );
  }
}
