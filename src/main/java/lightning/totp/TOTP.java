package lightning.totp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base32;

import com.google.common.base.Optional;
import com.google.common.net.UrlEscapers;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * Implements the time-based one-time password algorithm specified in RFC 6238, 4226.
 * Provides access to the secret key in raw, base32, and QR code form for use with a TOTP client.
 *
 * If you are having trouble getting codes to match between this implementation and a
 * client authenticator, you should ensure that both your server and the client have
 * their system time synchronized to real time. A time skew on the clocks of an
 * authenticator and your server of >= 1.5 minutes will cause all codes to be rejected.
 *
 * Please note that each TOTP code is valid for only 3.5 minutes.
 *
 * This implementation does not protect against replay attacks that occur during the
 * time that a TOTP code is valid. You must use an external data store to verify that
 * you have not previously seen a code.
 *
 * This implementation does not protect against brute-force attacks. You should limit
 * the rate at which clients can attempt to check TOTP tokens using an external data
 * store.
 *
 * You should communicate TOTP secrets to clients in a secure manner (e.g. only over SSL).
 * We recommend picking keys (>=64 bytes, generated using a strong random number generator).
 *
 * You may elect to provide "scratch codes" to clients that can be accepted for one-time use in lieu
 * of a TOTP token for account recovery. Alternatively, you may recommend clients record the secret
 * key for recovery purposes.
 *
 * The server may store secret keys un-encrypted. If compromised, two-factor authentication will
 * simply provide no additional security until new keys are generated and installed by clients.
 *
 * You may send TOTP codes (fetched by getCurrentToken()) to clients via email, SMS, or other
 * third party system in lieu of having them use an authenticator app.
 *
 * An example usage (pseudo-code):
 *   let totp = new TOTP(user.secretKey);
 *   let response = totp.checkToken(httpRequest.authToken);
 *   let isValid = response.isValid;
 *   if (isValid) {
 *     atomic.transaction(user) {
 *       let counter <- user.lastUsedCounter OR 0;
 *       if (response.counter <= counter) {
 *          isValid = false; // Replay of a previously used token.
 *       }
 *       else {
 *          user.lastUsedCounter <- response.counter;
 *       }
 *     }
 *   }
 *   if (isValid) {
 *      // The user entered a valid, not-before-seen code.
 *      // Proceed with authentication.
 *   }
 */
public final class TOTP {
  private static final String URL_FORMAT = "otpauth://totp/%s%s?secret=%s&issuer=%s";
  private static final long TIME_STEP_MS = TimeUnit.SECONDS.toMillis(30);
  private static final int WINDOW_VARIANCE = 3;

  private static byte[] getLongAsBytesBigEndian(long value) {
    byte[] data = new byte[8];
    for (int i = 8; i-- > 0; value >>>= 8) {
      data[i] = (byte) value;
    }
    return data;
  }

  private static long getWindowFromTime(long time) {
    return time / TIME_STEP_MS;
  }

  private static long getCurrentTime() {
    return (new Date()).getTime();
  }

  private static long getCurrentWindow() {
    return getWindowFromTime(getCurrentTime());
  }

  public static final class TOTPCheck {
    public final long counter;
    public final boolean isValid;

    private TOTPCheck(long counter, boolean isValid) {
      this.counter = counter;
      this.isValid = isValid;
    }

    @Override
    public String toString() {
      return String.format("TOTPCheck[isValid=%b, counter=%d]", isValid, counter);
    }
  }

  private final byte[] key;

  /**
   * @param key A secret key (recommended 10 to 128 bytes in length). Client-specific. May be stored unencrypted.
   */
  public TOTP(byte[] key) {
    this.key = key;
  }

  /**
   * @param b32Key A secret key (base32 encoded as according to RFC 3548). Client-specific. May be stored unencrypted.
   * @throws DecoderException On invalid base32.
   */
  public TOTP(String b32Key) throws DecoderException {
    this((new Base32()).decode(b32Key));
  }

  private final TOTPCheck checkToken(int token, long window) {
    for (int i = -WINDOW_VARIANCE; i <= WINDOW_VARIANCE; i++) {
      if (window + i < 0) {
        continue;
      }

      if (getTokenForWindow(window + i) == token) {
        return new TOTPCheck(window + i, true);
      }
    }

    return new TOTPCheck(Math.max(window - WINDOW_VARIANCE - 1, 0), false);
  }

  /**
   * Checks whether or not a given TOTP token is currently valid.
   * This DOES NOT protect against replay attacks (must handle externally).
   * @param token A TOTP token.
   * @return Whether or not the given token is currently valid (isValid).
   *         If isValid, the caller should never again accept tokens with a counter <= the returned counter.
   */
  public final TOTPCheck checkToken(int token) {
    return checkToken(token, getCurrentWindow());
  }

  private final int getTokenForWindow(long window) {
    try {
      SecretKeySpec skey = new SecretKeySpec(key, "HmacSHA1");
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(skey);
      byte[] hash = mac.doFinal(getLongAsBytesBigEndian(window));
      int offset = hash[hash.length - 1] & 0xF;
      long truncatedHash = 0;
      for (int i = 0; i < 4; ++i) {
        truncatedHash <<= 8;
        truncatedHash |= (hash[offset + i] & 0xFF);
      }
      truncatedHash &= 0x7FFFFFFF;
      truncatedHash %= (int)Math.pow(10, 6);
      return (int) truncatedHash;
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new RuntimeException(e); // Should not happen on any standard JVM.
    }
  }

  /**
   * @return The most recent TOTP token that will currently be accepted as valid.
   *         This does not guarantee the token has not already been used.
   */
  public final int getCurrentToken() {
    return getTokenForWindow(getCurrentWindow() + WINDOW_VARIANCE);
  }

  /**
   * @param counter A counter (returned by checkToken).
   * @return The next valid TOTP token (after the counter).
   *         NOTE: The returned token MAY NOT be accepted immediately by checkToken,
   *               but is guaranteed to be accepted in the near future (within 30 seconds).
   */
  public final int getCurrentToken(int counter) {
    return getTokenForWindow(Math.max(counter + 1, getCurrentWindow()));
  }

  /**
   * @return All TOTP tokens that will currently be accepted as valid.
   */
  public final Set<Integer> getAllValidTokens() {
    Set<Integer> result = new HashSet<>();
    long base = getWindowFromTime((new Date()).getTime());
    for (int i = -WINDOW_VARIANCE; i <= WINDOW_VARIANCE; i++) {
      if (base + i < 0) {
        continue;
      }
      result.add(getTokenForWindow(base + i));
    }
    return result;
  }


  /**
   * @return The secret key.
   */
  public final byte[] getSecretKey() {
    return key;
  }

  /**
   * @return The secret key (base-32 encoded as according to RFC 3548).
   */
  public final String getSecretKeyB32() {
    return (new Base32()).encodeToString(getSecretKey());
  }

  private final String getQRUrl(String issuer, Optional<String> account) {
    String accountStr = "";
    if (account.isPresent()) {
      accountStr = ":" + UrlEscapers.urlFragmentEscaper().escape(account.get());
    }

    return String.format(URL_FORMAT,
                         UrlEscapers.urlFragmentEscaper().escape(issuer),
                         accountStr,
                         UrlEscapers.urlFragmentEscaper().escape(getSecretKeyB32()),
                         UrlEscapers.urlFragmentEscaper().escape(issuer));
  }

  /**
   * Writes the given secret key to the provided output stream as a QR code in PNG format.
   * The generated QR code may be scanned by any client that implements the TOTP standard.
   * Two popular client implementations are Authy and Google Authenticator.
   * @param issuer Specifies an issuer name (may be displayed by client app).
   * @param account Specifies an account name (optional, may be displayed by client app).
   * @param size Specifies the width and height of the bar code (in pixels).
   * @param stream Specifies a stream to write the generated image to.
   * @throws IOException
   * @throws WriterException
   */
  public final void writeSecretQRCode(String issuer, Optional<String> account, int size, OutputStream stream) throws IOException, WriterException {
    QRCodeWriter writer = new QRCodeWriter();
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
    BitMatrix data = writer.encode(getQRUrl(issuer, account),
                                   BarcodeFormat.QR_CODE,
                                   size, size, hints);
    MatrixToImageWriter.writeToStream(data, "png", stream);
  }

  /**
   * Generates a QR code in PNG format containing the secret key.
   * The generated QR code may be scanned by any client that implements the TOTP standard.
   * Two popular client implementations are Authy and Google Authenticator.
   * @param issuer Specifies an issuer name (may be displayed by client app).
   * @param account Specifies an account name (optional, may be displayed by client app).
   * @param size Specifies the width and height of the bar code (in pixels).
   * @return The base64-encoded bytes of the QR code in PNG format (good for use in a data URI e.g. data:image/png;base64,#####)
   * @throws IOException
   * @throws WriterException
   */
  public final String getSecretQRCodeB64(String issuer, Optional<String> account, int size) throws IOException, WriterException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    writeSecretQRCode(issuer, account, size, stream);
    return Base64.getEncoder().encodeToString(stream.toByteArray());
  }
}
