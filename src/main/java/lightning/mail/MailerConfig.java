package lightning.mail;

public interface MailerConfig {
  public String getAddress();
  public int getPort();
  public boolean useSSL();
  public String getHost();
  public String getUsername();
  public String getPassword();
}