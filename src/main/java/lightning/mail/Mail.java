package lightning.mail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.utils.IOUtils;

/**
 * Represents an Email Sending System.
 * Wraps the JavaMail API to provide a better interface.
 */
public class Mail {
  private static final Logger logger = LoggerFactory.getLogger(Mail.class);
  private static final String ENCODING = "UTF-8";
  private static ExecutorService pool = null;
  private static Session session = null;
  private static InternetAddress from = null;
  
  /**
   * Represents an Email Message.
   * Wraps the JavaMail API MimeMessage to provide a better interface.
   * 
   * An email consists of a set of recipients (TO, CC, BCC), a plain text
   * message AND/OR an HTML message (usually the plain text is fall back
   * for the HTML version on older mail clients), a subject, and a set of
   * file attachments.
   */
  public static final class Message {
    private final MimeMessage message;
    private final MimeMultipart multipart = new MimeMultipart("alternative");
    
    Message() throws MessagingException {
      message = new MimeMessage(session);
      message.setFrom(from);
    }
    
    /**
     * Adds a TO recipient.
     * @param email An email address.
     * @throws MessagingException On failure.
     */
    public void addRecipient(String email) throws MessagingException {
      message.addRecipient(RecipientType.TO, new InternetAddress(email));
    }
    
    /**
     * Adds a TO recipient.
     * @param email An email address.
     * @param name The display name for that email address.
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    public void addRecipient(String email, String name) throws MessagingException, UnsupportedEncodingException {
      message.addRecipient(RecipientType.TO, new InternetAddress(email, name));
    }
    
    /**
     * Adds a CC recipient.
     * @param email An email address.
     * @throws MessagingException
     */
    public void addCC(String email) throws MessagingException {
      message.addRecipient(RecipientType.CC, new InternetAddress(email));
    }
    
    /**
     * Adds a CC recipient.
     * @param email An email address.
     * @param name The display name for that email address.
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    public void addCC(String email, String name) throws MessagingException, UnsupportedEncodingException {
      message.addRecipient(RecipientType.CC, new InternetAddress(email, name));
    }
    
    /**
     * Adds a BCC recipient.
     * @param email An email address.
     * @throws MessagingException
     */
    public void addBCC(String email) throws MessagingException {
      message.addRecipient(RecipientType.BCC, new InternetAddress(email));
    }
    
    /**
     * Adds a BCC recipient.
     * @param email An email address.
     * @param name The display name for that email address.
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    public void addBCC(String email, String name) throws MessagingException, UnsupportedEncodingException {
      message.addRecipient(RecipientType.BCC, new InternetAddress(email, name));
    }
    
    /**
     * Sets the message's subject.
     * @param subject The subject.
     * @throws MessagingException
     */
    public void setSubject(String subject) throws MessagingException {
      message.setSubject(subject, ENCODING);
    }
    
    void send() throws MessagingException {
      message.setSentDate(new Date());
      message.setContent(multipart);
      Transport.send(message);
    }
    
    /**
     * Sets the text content of the message.
     * @param content The message content.
     * @throws MessagingException
     */
    public void setText(String content) throws MessagingException {
      MimeBodyPart textPart = new MimeBodyPart();
      textPart.setText(content, ENCODING);
      multipart.addBodyPart(textPart);
    }
    
    /**
     * Sets the HTML content of the message.
     * @param content The message content.
     * @throws MessagingException
     */
    public void setHTMLText(String content) throws MessagingException {
      MimeBodyPart htmlPart = new MimeBodyPart();
      htmlPart.setContent(content, "text/html; charset=UTF-8");
      multipart.addBodyPart(htmlPart);
    }
    
    /**
     * Sets the text content of the message from an InputStream.
     * @param stream To read exhaustively for message content.
     * @throws MessagingException
     * @throws IOException
     */
    public void setText(InputStream stream) throws MessagingException, IOException {
      MimeBodyPart textPart = new MimeBodyPart();
      textPart.setText(IOUtils.toString(stream), ENCODING);
      multipart.addBodyPart(textPart);
    }
    
    /**
     * Sets the HTML content of the message from an InputStream.
     * @param stream To read exhaustively for HTML message content.
     * @throws MessagingException
     */
    public void setHTMLText(InputStream stream) throws MessagingException {
      MimeBodyPart htmlPart = new MimeBodyPart();
      htmlPart.setContent(stream, "text/html; charset=UTF-8");
      multipart.addBodyPart(htmlPart);
    }
    
    /**
     * Adds an attached file.
     * @param fileName The name of the file as it will appear to the user.
     * @param mimeType The file's MIME type (see edu.rice.phyloweb.webserver.util.Mimes).
     * @param stream To read exhaustively for file contents.
     * @throws MessagingException
     */
    public void addAttachment(String fileName, String mimeType, InputStream stream) throws MessagingException {
      MimeBodyPart attachment = new MimeBodyPart();
      attachment.setFileName(fileName);
      attachment.setContent(stream, mimeType);
      multipart.addBodyPart(attachment);
    }
    
    /**
     * Adds an attached file.
     * @param path The path to the file on this machine.
     * @throws MessagingException
     * @throws IOException
     */
    public void addAttachment(String path) throws MessagingException, IOException {
      addAttachment(new File(path));
    }
    
    /**
     * Adds an attached file.
     * @param file A file reference.
     * @throws MessagingException
     * @throws IOException
     */
    public void addAttachment(File file) throws MessagingException, IOException {
      addAttachment(file.getName(), new FileDataSource(file.getCanonicalPath()));
    }
    
    /**
     * Adds an attachment.
     * @param fileName The name of the file.
     * @param source To retrieve the file contents from. 
     * @throws MessagingException
     */
    public void addAttachment(String fileName, DataSource source) throws MessagingException {
      MimeBodyPart attachment = new MimeBodyPart();
      attachment.setDataHandler(new DataHandler(source));
      attachment.setFileName(fileName);
      multipart.addBodyPart(attachment);    
    }
  }
  
  /**
   * Creates and returns a new email that can be send via Mail.send().
   * The message is sent from the default email address for the application.
   * The message will automatically have its send date and from fields filled out.
   * @return A new email that can be mutated.
   * @throws MessagingException On failure.
   */
  public static Message createMessage() throws MessagingException {
    if (from == null || session == null) {
      throw new MessagingException("Must call Mail.configure(...) before creating Mail.");
    }
    
    return new Message();
  }
  
  /**
   * Enqueues a message for delivery.
   * Delivery of a message is not guaranteed unless the returned future resolves to true.
   * @param message The message to be sent.
   * @return A future that resolves when the message is sent successfully or fails to send.
   *         Failures will be logged; it is not recommended that clients wait upon this future,
   *         particularly if they are servicing a web request.
   */
  public static Future<Boolean> send(Message message) {
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
  public static void sendNow(Message message) throws Exception {
    send(message).get();
  }
  
  public static interface MailConfig {
    public String getAddress();
    public int getPort();
    public boolean useSSL();
    public String getHost();
    public String getUsername();
    public String getPassword();
  }
  
  /**
   * Configures the mail system.
   * @param config
   * @throws AddressException
   * @throws UnsupportedEncodingException
   */
  public static void configure(final MailConfig config) throws AddressException, UnsupportedEncodingException {
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
