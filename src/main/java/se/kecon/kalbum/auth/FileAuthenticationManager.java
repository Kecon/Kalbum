package se.kecon.kalbum.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Slf4j
public class FileAuthenticationManager implements AuthenticationManager {

    @Autowired
    private UserDao userDao;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        if (authentication instanceof UsernamePasswordAuthenticationToken token) {
            String username = token.getName();
            String password = token.getCredentials().toString();

            final Optional<User> user = userDao.findByUsername(username);

            final Optional<String> passwordHash = userDao.getPasswordHash(username);

            if (user.isPresent() && user.get().isEnabled() && passwordHash.isPresent() && encoder.matches(password, passwordHash.get())) {
                UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(username, password, token.getAuthorities());
                log.info("User {} authenticated with {} and {}", username, token.getDetails(), token.getAuthorities());
                token.eraseCredentials();
                return result;
            } else {
                log.info("User {} not authenticated", username);
            }

            ((UsernamePasswordAuthenticationToken) authentication).eraseCredentials();
        } else {
            log.info("User not authenticated");
        }

        return authentication;
    }
}
