package lightning.mail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.IOUtils;

/**
 * Represents an Email Message.
 * Wraps the JavaMail API MimeMessage to provide a better interface.
 * 
 * An email consists of a set of recipients (TO, CC, BCC), a plain text
 * message AND/OR an HTML message (usually the plain text is fall back
 * for the HTML version on older mail clients), a subject, and a set of
 * file attachments.
 */
public final class Message {
  private static final String ENCODING = "UTF-8";
  private final MimeMessage message;
  private final MimeMultipart multipart = new MimeMultipart("alternative");
  
  Message(Session session, InternetAddress from) throws MessagingException {
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