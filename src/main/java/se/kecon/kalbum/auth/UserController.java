package se.kecon.kalbum.auth;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import se.kecon.kalbum.validation.*;

import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static se.kecon.kalbum.validation.Validation.*;

@RestController
@Slf4j
public class UserController {

    public static final String CSRF_TOKEN = "X-CSRF-Token";

    @Autowired
    private UserDao userDao;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private AlbumAuthorizationManager albumAuthorizationManager;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    /**
     * Create a new user.
     *
     * @param username the username of the user
     * @param email    the email of the user
     * @return 200 if the user was created, 400 if the username or email is invalid, 409 if the user already exists, 403 if the user is not allowed to create a user
     */
    @PreAuthorize("@albumAuthorizationManager.isAdmin(authentication)")
    @PostMapping("/users/")
    public ResponseEntity<Void> createUser(@RequestParam("username") String username, @RequestParam("email") String email) {
        try {
            checkValidUsername(username);
            checkValidEmail(email);

            log.info("createUser {} and {}", username, email);
            if (this.userDao.findByUsername(username).isPresent()) {
                log.info("createUser {} and {} rejected due to username already used", username, email);
                return ResponseEntity.badRequest().build();
            }

            if (this.userDao.findByEmail(email).isPresent()) {
                log.info("createUser {} and {} rejected due to e-mail already used", username, email);
                return ResponseEntity.badRequest().build();
            }

            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setEnabled(true);
            user.setRole(Role.USER);
            this.userDao.save(user);
            return ResponseEntity.created(URI.create(username)).header(CSRF_TOKEN, csrfTokenRepository.generateToken(null).getToken()).build();
        } catch (IllegalUsernameException | IllegalEmailException e) {
            log.info("createUser {} and {} rejected due to {}", username, email, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all users.
     *
     * @return a set of all users sorted by username or an empty set if no users exist. 403 if the user is not allowed to get all users
     */
    @PreAuthorize("@albumAuthorizationManager.isAdmin(authentication)")
    @GetMapping("/users/")
    public ResponseEntity<Set<User>> getUsers() {
        return ResponseEntity.ok().body(new TreeSet<>(this.userDao.findAll()));
    }

    /**
     * Get a user by username.
     *
     * @param username the username of the user
     * @return the user
     */
    @PreAuthorize("@albumAuthorizationManager.isAdmin(authentication)")
    @GetMapping("/users/{username}")
    public ResponseEntity<User> getUser(@PathVariable("username") String username) {
        try {
            checkValidUsername(username);
        } catch (IllegalUsernameException e) {
            log.info("getUser {} rejected due to {}", username, e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        return this.userDao.findByUsername(username).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Delete a user by username.
     *
     * @param username the username of the user
     * @return 200 if the user was deleted, 400 if the username is invalid, 403 if the user is not allowed to delete the user
     */
    @PreAuthorize("@albumAuthorizationManager.isAdmin(authentication)")
    @DeleteMapping("/users/{username}")
    public ResponseEntity<Void> deleteUser(@PathVariable("username") String username) {
        try {
            checkValidUsername(username);
        } catch (IllegalUsernameException e) {
            log.info("deleteUser {} rejected due to {}", username, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
        this.userDao.deleteByUsername(username);
        return ResponseEntity.noContent().header(CSRF_TOKEN, csrfTokenRepository.generateToken(null).getToken()).build();
    }

    /**
     * Update a user's email and set enabled.
     *
     * @param username the username of the user
     * @param email    the new email of the user or null if not changed
     * @param enabled  the new enabled status of the user or null if not changed
     * @param role     the new role of the user or null if not changed
     * @return 204 if the email was updated, 400 if the username or email is invalid, 403 if the user is not allowed to change the email
     */
    @PreAuthorize("@albumAuthorizationManager.isSelfOrAdmin(authentication, #username)")
    @PatchMapping("/users/{username}")
    public ResponseEntity<Void> updateUser(Authentication authentication, @PathVariable("username") String username, @RequestParam(value = "email", required = false) String email, @RequestParam(value = "enabled", required = false) Boolean enabled, @RequestParam(value = "role", required = false) String role) {

        final Role parsedRole;
        try {
            checkValidUsername(username);

            if (email != null && !email.isBlank()) {
                checkValidEmail(email);
            }

            if (role != null && !role.isBlank()) {
                if (albumAuthorizationManager.canAssignRole(authentication, username, role)) {
                    checkValidRole(role);
                    parsedRole = Role.valueOf(role);
                } else {
                    log.info("updateUser {} with {}, enabled {}, role {} rejected due to insufficient privileges", username, email, enabled, role);
                    return ResponseEntity.status(HttpServletResponse.SC_FORBIDDEN).build();
                }
            } else {
                parsedRole = null;
            }
        } catch (IllegalUsernameException | IllegalEmailException | IllegalRoleException e) {
            log.info("updateUser {} with {}, enabled {}, role {} rejected due to {}", username, email, enabled, role, e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        this.userDao.findByUsername(username).ifPresent(user -> {
            if (email != null && !email.isBlank()) {
                user.setEmail(email);
            }

            if (enabled != null) {
                user.setEnabled(enabled);
            }

            if (parsedRole != null) {
                user.setRole(parsedRole);
            }

            this.userDao.save(user);
        });

        log.info("updateUser {} with {} and enabled {}", username, email, enabled);
        return ResponseEntity.noContent().header(CSRF_TOKEN, csrfTokenRepository.generateToken(null).getToken()).build();
    }

    /**
     * Change password for a user. It may only be called by the user itself.
     *
     * @param username    the username of the user
     * @param password    the current password of the user
     * @param newPassword the new password of the user
     * @return 204 if the password was changed, 400 if the username or password is invalid, 403 if the user is not allowed to change the password
     */
    @PreAuthorize("@albumAuthorizationManager.isSelf(authentication, #username)")
    @PutMapping("/users/{username}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable("username") String username, @RequestParam("password") String password, @RequestParam("newPassword") String newPassword) {
        try {
            checkValidUsername(username);
            checkValidPassword(newPassword);
        } catch (IllegalUsernameException | IllegalPasswordException e) {
            log.info("updatePassword for {} rejected due to {}", username, e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        final Optional<String> passwordHash = this.userDao.getPasswordHash(username);

        if (passwordHash.isEmpty()) {
            log.info("updatePassword for {} rejected due to no previous password set", username);
            return ResponseEntity.badRequest().build();
        }

        if (!this.passwordEncoder.matches(password, passwordHash.get())) {
            log.info("updatePassword for {} rejected due old password didn't match", username);
            return ResponseEntity.badRequest().build();
        }

        final String newPasswordHash = this.passwordEncoder.encode(newPassword);
        this.userDao.setPasswordHash(username, newPasswordHash);

        return ResponseEntity.noContent().header(CSRF_TOKEN, csrfTokenRepository.generateToken(null).getToken()).build();
    }

    /**
     * Update a user's album role.
     *
     * @param username the username of the user
     * @param albumId  the id of the album
     * @param role     the new role of the user
     * @return 204 if the role was updated, 400 if the username, albumId or role is invalid
     */
    @PreAuthorize("@albumAuthorizationManager.canAssignRole(authentication, #username, #role)")
    @PutMapping("/users/{username}/albumRoles/{albumId}")
    public ResponseEntity<Void> updateAlbumRole(@PathVariable("username") String username, @PathVariable("albumId") String albumId, @RequestParam("role") String role) {
        try {
            checkValidUsername(username);
            checkValidAlbumId(albumId);
            checkValidAlbumRole(role);
        } catch (IllegalUsernameException | IllegalAlbumIdException | IllegalRoleException e) {
            log.info("updateAlbumRole for {} and {} with {} rejected due {}", username, albumId, role, e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        this.userDao.findByUsername(username).ifPresent(user -> {
            ConcurrentMap<String, Role> albumRoles = user.getAlbumRoles();
            if (albumRoles == null) {
                albumRoles = new ConcurrentHashMap<>();
            }

            if (role.equals(Role.NONE.name())) {
                albumRoles.remove(albumId);
            } else {
                albumRoles.put(albumId, Role.valueOf(role));
            }

            this.userDao.save(user);
        });

        log.info("updateAlbumRole for {} and {} with {}", username, albumId, role);
        return ResponseEntity.noContent().header(CSRF_TOKEN, csrfTokenRepository.generateToken(null).getToken()).build();
    }

    /**
     * Get logged in users roles
     */
    @GetMapping("/self")
    public ResponseEntity<User> getSelf(Authentication authentication) {
        return this.userDao.findByUsername(authentication.getName()).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
