package se.kecon.kalbum.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AlbumAuthorizationManagerTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private AlbumAuthorizationManager albumAuthorizationManager;

    @Test
    public void testIsAlbumUser() {
        User user = new User();
        user.setEnabled(true);
        user.setRole(Role.USER);
        user.setAlbumRoles(new ConcurrentHashMap<>(Map.of("album1", Role.USER)));
        when(userDao.findByUsername("user1")).thenReturn(Optional.of(user));

        Authentication authentication = new UsernamePasswordAuthenticationToken("user1", "password");
        boolean result = albumAuthorizationManager.isAlbumUser(authentication, "album1");

        assertTrue(result);
    }

    @Test
    public void testIsAlbumAdmin() {
        User user = new User();
        user.setEnabled(true);
        user.setRole(Role.ADMIN);
        user.setAlbumRoles(new ConcurrentHashMap<>(Map.of("album1", Role.ADMIN)));
        when(userDao.findByUsername("admin1")).thenReturn(Optional.of(user));

        Authentication authentication = new UsernamePasswordAuthenticationToken("admin1", "password");
        boolean result = albumAuthorizationManager.isAlbumAdmin(authentication, "album1");

        assertTrue(result);
    }

    @Test
    public void testIsAdmin() {
        User user = new User();
        user.setEnabled(true);
        user.setRole(Role.ADMIN);
        when(userDao.findByUsername("admin1")).thenReturn(Optional.of(user));

        Authentication authentication = new UsernamePasswordAuthenticationToken("admin1", "password");
        boolean result = albumAuthorizationManager.isAdmin(authentication);

        assertTrue(result);
    }

    @Test
    public void testCanAssignRole() {
        User user = new User();
        user.setEnabled(true);
        user.setRole(Role.ADMIN);
        when(userDao.findByUsername("admin1")).thenReturn(Optional.of(user));

        User user1 = new User();
        user1.setEnabled(true);
        user1.setRole(Role.USER);
        when(userDao.findByUsername("user1")).thenReturn(Optional.of(user1));

        Authentication authentication = new UsernamePasswordAuthenticationToken("admin1", "password");
        boolean result = albumAuthorizationManager.canAssignRole(authentication, "user1", "USER");

        assertTrue(result);
    }
}