package se.kecon.kalbum.auth;

import java.util.Optional;
import java.util.Set;

public interface UserDao {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    void save(User user);

    void delete(User user);

    void deleteByUsername(String username);

    Set<User> findAll();

    void setPasswordHash(String username, String passwordHash);

    Optional<String> getPasswordHash(String username);

}
