package se.kecon.kalbum.auth;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import se.kecon.kalbum.util.FileUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Data access objects for users. It is thread safe and store the users in a file. This implementation is intended
 * to be used if you only have a few users, such as your family and close friends. If you have a lot of users,
 * you should use a database instead and a different implementation. The reason for this implementation is that it
 * doesn't require a database, and it is easy to use. Password hashes are not stored in the user object to not send them
 * around more than necessary. Instead, they are stored in a separate map.
 *
 * @author Kenny Colliander
 * @since 2023-08-08
 */
@Component
@Slf4j
public class UserDaoComponent implements UserDao, InitializingBean {

    public static final String USERS_FILE = "users";

    /**
     * The users with username as key and user as value
     */
    private final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();

    /**
     * The password hashes with username as key and password hash as value
     */
    private final ConcurrentMap<String, String> passwordHashes = new ConcurrentHashMap<>();

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final AtomicBoolean dirty = new AtomicBoolean(false);


    private final Lock lock = new ReentrantLock();

    @Setter
    @Value("${kalbum.path}")
    private Path albumBasePath;

    @Override
    public void afterPropertiesSet() throws Exception {
        readAllFromPersistentStorage();
    }

    /**
     * Check if the user database is initialized
     *
     * @throws IllegalStateException if the user database is not initialized
     */
    protected void checkInitialized() {
        if (!this.initialized.get()) {
            throw new IllegalStateException("UserDaoComponent is not initialized");
        }
    }

    /**
     * Get the path to the users file
     *
     * @return the path to the users file
     */
    protected Path getUsersPath() {
        return this.albumBasePath.resolve(USERS_FILE);
    }

    /**
     * Read all users from persistent storage and store them in memory. This method is thread safe and
     * is only called once.
     */
    protected void readAllFromPersistentStorage() {
        this.lock.lock();
        try {
            Files.readAllLines(this.getUsersPath()).forEach(line -> {
                final List<String> parts = FileUtils.decodeSemicolonSeparatedList(line);

                if (parts.size() != 9) {
                    log.warn("Invalid line in users file: " + line);
                } else {
                    final User user = new User();
                    user.setUsername(parts.get(0));
                    user.setEnabled(Boolean.parseBoolean(parts.get(1)));
                    user.setRole(Role.valueOf(parts.get(2)));
                    user.setEmail(parts.get(3));
                    ConcurrentMap<String, Role> albumRoles = getAlbumRoles(parts.get(4));
                    user.setFirstName(parts.get(6));
                    user.setLastName(parts.get(7));
                    try {
                        final String lastPasswordResetDate = parts.get(8);
                        if (lastPasswordResetDate != null && !lastPasswordResetDate.isBlank()) {
                            user.setLastPasswordResetDate(Instant.parse(lastPasswordResetDate));
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse last password reset date for user " + user.getUsername(), e);
                    }

                    user.setAlbumRoles(albumRoles);

                    this.users.put(user.getUsername(), user);
                    this.passwordHashes.put(user.getUsername(), parts.get(5));
                }
            });

            this.initialized.set(true);
        } catch (IOException e) {
            log.error("Failed to read users file", e);
            throw new UncheckedIOException("Failed to read users file", e);
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Get album roles from a string. The roles are separated by comma and the album id and role are separated by colon.
     *
     * @param rolesString the string to parse
     * @return the album roles
     */
    protected ConcurrentMap<String, Role> getAlbumRoles(String rolesString) {
        final ConcurrentMap<String, Role> albumRoles = new ConcurrentHashMap<>();

        for (final String roleTuple : rolesString.split(",")) {
            final String[] albumRoleParts = roleTuple.split(":");

            if (albumRoleParts.length != 2) {
                continue;
            }

            // albumRoleParts[0] is album id and albumRoleParts[1] is role
            albumRoles.put(albumRoleParts[0], Role.valueOf(albumRoleParts[1]));
        }
        return albumRoles;
    }

    @Async
    protected void writeAllToPersistentStorage() {
        this.lock.lock();
        try {
            if (!this.dirty.getAndSet(false)) {
                // Nothing to do, already saved
                return;
            }

            final List<String> lines = new ArrayList<>();

            this.users.values().forEach(user -> {
                final List<String> parts = new ArrayList<>();
                parts.add(user.getUsername());
                parts.add(String.valueOf(user.isEnabled()));
                parts.add(user.getRole().toString());
                parts.add(user.getEmail());
                parts.add(getAlbumRoles(user.getAlbumRoles()));
                parts.add(this.passwordHashes.get(user.getUsername()));
                parts.add(user.getFirstName());
                parts.add(user.getLastName());
                parts.add(user.getLastPasswordResetDate() != null ? user.getLastPasswordResetDate().toString() : null);

                lines.add(FileUtils.encodeSemicolonSeparatedList(parts.toArray(new String[0])));
            });

            Files.write(this.getUsersPath(), lines);
        } catch (IOException e) {
            log.error("Failed to write users file", e);
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Get album roles as a string. The roles are separated by comma and the album id and role are separated by colon.
     *
     * @param albumRoles the album roles
     * @return the string representation of the album roles
     */
    protected String getAlbumRoles(final Map<String, Role> albumRoles) {
        final StringBuilder stringBuilder = new StringBuilder();

        final Map<String, Role> sortedAlbumRoles = new TreeMap<>(albumRoles == null ? Collections.emptyMap() : albumRoles);

        sortedAlbumRoles.forEach((key, value) -> {
            if (!stringBuilder.isEmpty()) {
                stringBuilder.append(",");
            }

            stringBuilder.append(key);
            stringBuilder.append(":");
            stringBuilder.append(value);
        });

        return stringBuilder.toString();
    }

    @Override
    public Optional<User> findByUsername(final String username) {
        this.checkInitialized();

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }

        final User user = this.users.get(username);

        if (user == null) {
            return Optional.empty();
        }

        return Optional.of(new User(user));
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        this.checkInitialized();

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }

        return this.users.values().stream().filter(user -> email.equalsIgnoreCase(user.getEmail())).findFirst().map(User::new);
    }

    @Override
    public void save(User user) {
        this.checkInitialized();

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }

        this.users.put(user.getUsername(), new User(user));
        this.dirty.set(true);
        this.writeAllToPersistentStorage();
    }

    @Override
    public void delete(User user) {
        this.checkInitialized();

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }

        this.users.remove(user.getUsername());
        this.dirty.set(true);
        this.writeAllToPersistentStorage();
    }

    @Override
    public void deleteByUsername(final String username) {
        this.checkInitialized();

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }

        this.users.remove(username);
        this.dirty.set(true);
        this.writeAllToPersistentStorage();
    }

    @Override
    public Set<User> findAll() {
        this.checkInitialized();

        return this.users.values().stream().map(User::new).collect(Collectors.toSet());
    }

    @Override
    public void setPasswordHash(final String username, final String passwordHash) {
        this.checkInitialized();

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be null or blank");
        }

        this.passwordHashes.put(username, passwordHash);
        this.dirty.set(true);
        this.writeAllToPersistentStorage();
    }

    @Override
    public Optional<String> getPasswordHash(final String username) {
        this.checkInitialized();

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }

        final User user = this.users.get(username);

        if (user == null || !user.isEnabled()) {
            return Optional.empty();
        }

        final String passwordHash = this.passwordHashes.get(username);

        if (passwordHash == null) {
            return Optional.empty();
        }

        return Optional.of(passwordHash);
    }
}
