package se.kecon.kalbum.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Comparable<User> {

    private String username;

    private String firstName;

    private String lastName;

    private boolean enabled;

    private Role role;

    private String email;

    private ConcurrentMap<String, Role> albumRoles = new ConcurrentHashMap<>();

    private Instant lastPasswordResetDate;

    /**
     * Copy constructor
     */
    public User(User user) {
        this.username = user.username;
        this.firstName = user.firstName;
        this.lastName = user.lastName;
        this.enabled = user.enabled;
        this.role = user.role;
        this.email = user.email;
        this.lastPasswordResetDate = user.lastPasswordResetDate;
        this.albumRoles = user.albumRoles != null ? new ConcurrentHashMap<>(user.albumRoles) : null;
    }

    @Override
    public int compareTo(User o) {
        return this.username.compareTo(o.username);
    }
}
