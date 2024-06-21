package se.kecon.kalbum.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
class FileAuthenticationManagerTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private FileAuthenticationManager fileAuthenticationManager;

    @Test
    void testAuthenticate_Success() {
        User user = new User();
        user.setEnabled(true);
        when(userDao.findByUsername("user1")).thenReturn(Optional.of(user));
        when(userDao.getPasswordHash("user1")).thenReturn(Optional.of("$2a$10$J0cOKWuvnRcfAXN61SZlJubeXlhBVT0z052iQiMEw6EObuenBZblK")); // bcrypt hash for "password"

        Authentication authentication = new UsernamePasswordAuthenticationToken("user1", "password");
        Authentication result = fileAuthenticationManager.authenticate(authentication);

        assertTrue(result.isAuthenticated());
    }

    @Test
    void testAuthenticate_Fail_WrongPassword() {
        User user = new User();
        user.setEnabled(true);
        when(userDao.findByUsername("user1")).thenReturn(Optional.of(user));
        when(userDao.getPasswordHash("user1")).thenReturn(Optional.of("$2a$10$J0cOKWuvnRcfAXN61SZlJubeXlhBVT0z052iQiMEw6EObuenBZblK")); // bcrypt hash for "password"

        Authentication authentication = new UsernamePasswordAuthenticationToken("user1", "wrongPassword");
        Authentication result = fileAuthenticationManager.authenticate(authentication);

        assertFalse(result.isAuthenticated());
    }

    @Test
    void testAuthenticate_Fail_UserDisabled() {
        User user = new User();
        user.setEnabled(false);
        when(userDao.findByUsername("user1")).thenReturn(Optional.of(user));
        when(userDao.getPasswordHash("user1")).thenReturn(Optional.of("$2a$10$J0cOKWuvnRcfAXN61SZlJubeXlhBVT0z052iQiMEw6EObuenBZblK")); // bcrypt hash for "password"

        Authentication authentication = new UsernamePasswordAuthenticationToken("user1", "password");
        Authentication result = fileAuthenticationManager.authenticate(authentication);

        assertFalse(result.isAuthenticated());
    }

    @Test
    void testAuthenticate_Fail_UserDoesNotExist() {
        when(userDao.findByUsername("nonexistentUser")).thenReturn(Optional.empty());

        Authentication authentication = new UsernamePasswordAuthenticationToken("nonexistentUser", "password");
        Authentication result = fileAuthenticationManager.authenticate(authentication);

        assertFalse(result.isAuthenticated());
    }
}