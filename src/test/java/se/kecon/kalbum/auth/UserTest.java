package se.kecon.kalbum.auth;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    public void testGettersAndSetters() {
        User user = new User();
        user.setUsername("testUser");
        user.setEnabled(true);
        user.setRole(Role.USER);
        user.setEmail("testUser@example.com");
        user.setAlbumRoles(new ConcurrentHashMap<>());

        assertEquals("testUser", user.getUsername());
        assertTrue(user.isEnabled());
        assertEquals(Role.USER, user.getRole());
        assertEquals("testUser@example.com", user.getEmail());
        assertNotNull(user.getAlbumRoles());
    }

    @Test
    public void testCompareTo() {
        User user1 = new User();
        user1.setUsername("user1");

        User user2 = new User();
        user2.setUsername("user2");

        assertTrue(user1.compareTo(user2) < 0);
        assertTrue(user2.compareTo(user1) > 0);
        assertEquals(0, user1.compareTo(user1));
    }
}

