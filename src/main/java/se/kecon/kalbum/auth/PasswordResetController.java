package se.kecon.kalbum.auth;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.websocket.server.PathParam;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.markenwerk.utils.mail.dkim.Canonicalization;
import net.markenwerk.utils.mail.dkim.DkimMessage;
import net.markenwerk.utils.mail.dkim.DkimSigner;
import net.markenwerk.utils.mail.dkim.SigningAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import se.kecon.kalbum.validation.IllegalEmailException;
import se.kecon.kalbum.validation.IllegalPasswordException;

import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static se.kecon.kalbum.validation.Validation.checkValidEmail;
import static se.kecon.kalbum.validation.Validation.checkValidPassword;

@RestController
@Slf4j
public class PasswordResetController {

    @Autowired
    private UserDao userDao;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Setter(AccessLevel.PACKAGE)
    private Clock clock = Clock.systemUTC();

    @Setter
    @Value("${kalbum.smtp.host}")
    private String host;

    @Setter
    @Value("${kalbum.smtp.port}")
    private String port = "25";

    @Setter
    @Value("${kalbum.smtp.auth}")
    private String auth = "false";

    @Setter
    @Value("${kalbum.smtp.starttls.enable}")
    private String starttlsEnable = "true";

    @Setter
    @Value("${kalbum.passwordreset.from-address}")
    private String fromAddress;

    @Setter
    @Value("${kalbum.passwordreset.password}")
    private String password;

    @Setter
    @Value("${kalbum.passwordreset.dkim.key}")
    private String dkimKey;

    @Setter
    @Value("${kalbum.passwordreset.dkim.selector}")
    private String dkimSelector;

    @Setter
    @Value("${kalbum.passwordreset.title}")
    private String title;

    @Setter
    @Value("${kalbum.baseurl}")
    private String baseUrl;

    private final ConcurrentMap<UUID, ResetPasswordData> resetPasswordDataMap = new ConcurrentHashMap<>();

    @Setter(AccessLevel.PACKAGE)
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    @Autowired
    private MessageSource messageSource;

    private final Lock lock = new ReentrantLock();


    @GetMapping("/resetpassword")
    public String resetPassword() {
        return templateEngine.process("resetpassword.html", new Context());
    }

    @PostMapping("/resetpassword/send")
    public void resetPassword(@RequestBody ResetPassword resetPassword) {
        lock.lock();
        try {
            Thread.sleep(1000);

            checkValidEmail(resetPassword.getEmail());
            log.info("resetPassword {}", resetPassword.getEmail());

            final Optional<User> user = userDao.findByEmail(resetPassword.getEmail());

            if (user.isEmpty()) {
                log.info("resetPassword for {} rejected due to no valid user", resetPassword.getEmail());
                return;
            }

            if(!user.get().isEnabled()) {
                log.info("resetPassword for {} rejected due to disabled user", resetPassword.getEmail());
                return;
            }

            if(user.get().getLastPasswordResetDate() != null && user.get().getLastPasswordResetDate().plusSeconds(60).isAfter(clock.instant())) {
                log.info("resetPassword for {} rejected due to too many requests", resetPassword.getEmail());
                return;
            }

            checkValidPassword(resetPassword.getPassphrase());

            Locale locale = LocaleContextHolder.getLocale();

            this.sendEmail(resetPassword.getEmail(), messageSource.getMessage("passwordreset.mail.subject", new Object[0], locale), this.renderResetPasswordTemplate(user.get(), locale, getResetPasswordLink(user.get(), resetPassword.getPassphrase())));

        } catch (IllegalEmailException e) {
            log.info("resetPassword for {} rejected due to invalid email", resetPassword.getEmail());
        } catch (IllegalPasswordException e) {
            log.info("resetPassword for {} rejected due to invalid passphrase", resetPassword.getEmail());
        } catch (Exception e) {
            log.error("resetPassword for {} failed", resetPassword.getEmail(), e);
        }
        finally {
            lock.unlock();
        }
    }

    @GetMapping("/resetpassword/{uuid}")
    public String resetPassword(@PathVariable("uuid") UUID uuid) {
        lock.lock();
        try {
            Thread.sleep(1000);

            log.info("resetPassword {}", uuid);

            final ResetPasswordData resetPasswordData = resetPasswordDataMap.get(uuid);

            if (resetPasswordData == null) {
                log.info("resetPassword for {} rejected due to no valid user", uuid);
                return getResetFailedResponse();
            }

            final Optional<User> user = userDao.findByEmail(resetPasswordData.getEmail());

            if (user.isEmpty()) {
                log.info("resetPassword for {} rejected due to no valid user", uuid);
                return getResetFailedResponse();
            }

            if(!user.get().isEnabled()) {
                log.info("resetPassword for {} rejected due to disabled user", uuid);
                return getResetFailedResponse();
            }

            Locale locale = LocaleContextHolder.getLocale();

            Context context = new Context();
            context.setLocale(locale);
            context.setVariable("token", uuid);
            return templateEngine.process("resetpassword-new.html", context);
        } catch (Exception e) {
            log.error("resetPassword for {} failed", uuid, e);
            return getResetFailedResponse();
        }
        finally {
            lock.unlock();
        }
    }

    private String getResetFailedResponse() {
        Context context = new Context();
        return templateEngine.process("resetpassword-failed.html", context);
    }

    public void sendEmail(String to, String subject, String htmlContent) throws Exception {

        try {
            // Get system properties
            final Properties properties = System.getProperties();

            // Setup mail server
            properties.setProperty("mail.smtp.host", this.host);
            properties.setProperty("mail.smtp.port", this.port);
            properties.setProperty("mail.smtp.auth", this.auth);
            properties.setProperty("mail.smtp.starttls.enable", this.starttlsEnable);

            final Session session;
            // Get the default Session object.
            if (this.password != null) {
                session = Session.getDefaultInstance(properties, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(fromAddress, password);
                    }
                });
            } else {
                session = Session.getDefaultInstance(properties);
            }

            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(fromAddress));

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

            // Set Subject: header field
            message.setSubject(subject);

            // Now set the actual message, but as HTML content
            message.setContent(htmlContent, "text/html");

            if (dkimKey == null || dkimKey.isEmpty() || dkimSelector == null || dkimSelector.isEmpty()) {
                log.info("Invalid DKIM configuration, sending message without DKIM signature to {}", to);
                Transport.send(message);
                log.info("Sent message successfully to {}", to);
                return;
            }

            // Create a DKIM signer
            byte[] dkimPrivateKeyBytes = Base64.getDecoder().decode(this.dkimKey);
            RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(dkimPrivateKeyBytes));
            DkimSigner dkimSigner = new DkimSigner(this.getDomain(fromAddress), this.dkimSelector, privateKey);
            dkimSigner.setIdentity(fromAddress);
            dkimSigner.setHeaderCanonicalization(Canonicalization.SIMPLE);
            dkimSigner.setBodyCanonicalization(Canonicalization.RELAXED);
            dkimSigner.setSigningAlgorithm(SigningAlgorithm.SHA256_WITH_RSA);
            dkimSigner.setLengthParam(true);
            dkimSigner.setCopyHeaderFields(false);

            // Create a DKIM message
            DkimMessage dkimMessage = new DkimMessage(message, dkimSigner);

            // Send message
            Transport.send(dkimMessage);
            log.info("Sent message successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send message to {}", to, e);
            throw e;
        }
    }

    /**
     * Get domain from e-mail
     *
     * @param email the e-mail address
     * @return the domain
     */
    private String getDomain(String email) {
        return email.substring(email.indexOf("@") + 1);
    }

    /**
     * Render the HTML template for the password reset e-mail
     *
     * @param user   the user to reset the password for
     * @param locale the locale to use for the template
     * @return the rendered HTML template
     */
    public String renderResetPasswordTemplate(final User user, final Locale locale, final String resetLink) {
        Context context = new Context();
        context.setLocale(locale);
        context.setVariable("title", title);
        context.setVariable("firstName", user.getFirstName());
        context.setVariable("resetLink", resetLink);

        return templateEngine.process("resetpassword-mail.html", context);
    }

    /**
     * Get the reset password link for the user
     *
     * @param user the user to reset the password for
     * @param passphrase the passphrase to use for the reset password
     * @return the reset password link
     */
    protected String getResetPasswordLink(final User user, final String passphrase) {

        ResetPasswordData resetPasswordData = new ResetPasswordData(user.getEmail(), clock.instant(), passphrase);
        user.setLastPasswordResetDate(resetPasswordData.getLastPasswordResetDate());

        this.userDao.save(user);

        UUID uuid;
        do {
            uuid = UUID.randomUUID();
        } while(resetPasswordDataMap.putIfAbsent(uuid, resetPasswordData) != null);

        return baseUrl + "/resetpassword/" + uuid;
    }

    @PutMapping("/resetpassword/{uuid}")
    public ResponseEntity<Void> resetPassword(@PathVariable("uuid") UUID uuid, @RequestBody ResetPassword resetPassword) {
        lock.lock();
        try {
            Thread.sleep(1000);

            log.info("resetPassword {}", uuid);

            final ResetPasswordData resetPasswordData = resetPasswordDataMap.get(uuid);

            if (resetPasswordData == null) {
                log.info("resetPassword for {} rejected due to no valid user", uuid);
                return ResponseEntity.status(403).build();
            }

            final Optional<User> user = userDao.findByEmail(resetPasswordData.getEmail());

            if (user.isEmpty()) {
                log.info("resetPassword for {} rejected due to no valid user for {}", uuid, resetPasswordData.getEmail());
                return ResponseEntity.status(403).build();
            }

            if(Duration.between(resetPasswordData.getLastPasswordResetDate(), clock.instant()).abs().toMinutes() > 15) {
                log.info("resetPassword for {} rejected due to expired link", uuid);
                return ResponseEntity.status(403).build();
            }

            if(!user.get().isEnabled()) {
                log.info("resetPassword for {} rejected due to disabled user", uuid);
                return ResponseEntity.status(403).build();
            }

            checkValidPassword(resetPassword.getPassphrase());

            try {
                checkValidPassword(resetPassword.getPassword());
            } catch (IllegalPasswordException e) {
                log.info("resetPassword for {} rejected due to invalid passphrase", uuid);
                return ResponseEntity.status(400).build();
            }

            if(!resetPasswordData.getPassphrase().equals(resetPassword.getPassphrase())) {
                log.info("resetPassword for {} rejected due to invalid passphrase", uuid);
                return ResponseEntity.status(401).build();
            }

            user.get().setLastPasswordResetDate(clock.instant());
            userDao.save(user.get());

            userDao.setPasswordHash(user.get().getUsername(), this.passwordEncoder.encode(resetPassword.getPassword()));

            resetPasswordDataMap.remove(uuid);
            return ResponseEntity.noContent().build();

        } catch (IllegalPasswordException e) {
            log.info("resetPassword for {} rejected due to invalid passphrase", uuid);
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            log.error("resetPassword for {} failed", uuid, e);
            return ResponseEntity.status(500).build();
        }
        finally {
            lock.unlock();
        }
    }

    // For testing purposes
    void addResetPasswordData(UUID uuid, ResetPasswordData resetPasswordData) {
        resetPasswordDataMap.put(uuid, resetPasswordData);
    }
}