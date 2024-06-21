package se.kecon.kalbum.auth;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.kecon.kalbum.util.FileUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the user dao component.
 * Please note that tests of writeAllToPersistentStorage is tested by save and delete tests.
 */
class UserDaoComponentTest {

    private FileSystem fileSystem;

    private Path albumBasePath;

    private UserDaoComponent userDao;

    private static final String USER_1_USERNAME = "user1";
    private static final Boolean USER_1_ENABLED = true;
    private static final Role USER_1_ROLE = Role.USER;
    private static final String USER_1_EMAIL = "user1@example.com";
    private static final String USER_1_ALBUM_ROLES = "id1:USER,id2:ADMIN";
    private static final String USER_1_PASSWORD = "password";
    private static final String USER_1_PASSWORD_HASH = "$2a$10$J0cOKWuvnRcfAXN61SZlJubeXlhBVT0z052iQiMEw6EObuenBZblK";
    private static final String USER_1_FIRST_NAME = "user1";
    private static final String USER_1_LAST_NAME = "1user";

    /**
     * Values in this string are separated by semicolon (;)
     * username: user1
     * enabled: true
     * role: USER
     * email: user1@example.com
     * albumRoles: id1:USER,id2:ADMIN
     * password: password
     * first name: user1
     * last name: 1user
     * lastPasswordResetDate:
     */
    private static final String USER_1_STRING = String.join(";", USER_1_USERNAME, USER_1_ENABLED.toString(), USER_1_ROLE.toString(), USER_1_EMAIL, USER_1_ALBUM_ROLES, USER_1_PASSWORD_HASH, USER_1_FIRST_NAME, USER_1_LAST_NAME, "");

    private static final String USER_2_USERNAME = "user2";
    private static final Boolean USER_2_ENABLED = false;
    private static final Role USER_2_ROLE = Role.ADMIN;
    private static final String USER_2_EMAIL = "user2@example.com";
    private static final String USER_2_ALBUM_ROLES = "id1:USER,id2:ADMIN";
    private static final String USER_2_PASSWORD = "password";
    private static final String USER_2_PASSWORD_HASH = "$2a$10$7YtCpTzcM6BOhyrqSSYo9OoeIGfOV7nbnhzWvjRAzt1giaP8GtSgm";
    private static final String USER_2_FIRST_NAME = "user2";
    private static final String USER_2_LAST_NAME = "2user";

    /**
     * Values in this string are separated by semicolon (;)
     * username: user2
     * enabled: false
     * role: ADMIN
     * email: user2@example.com
     * albumRoles: id1:USER,id2:ADMIN
     * password: password
     * first name: user2
     * last name: 2user
     * lastPasswordResetDate:
     */
    private static final String USER_2_STRING = String.join(";", USER_2_USERNAME, USER_2_ENABLED.toString(), USER_2_ROLE.toString(), USER_2_EMAIL, USER_2_ALBUM_ROLES, USER_2_PASSWORD_HASH, USER_2_FIRST_NAME, USER_2_LAST_NAME, "");


    private static final String USER_3_USERNAME = "user3";
    private static final Boolean USER_3_ENABLED = true;
    private static final Role USER_3_ROLE = Role.USER;
    private static final String USER_3_EMAIL = "user3@example.com";
    private static final String USER_3_ALBUM_ROLES = "";
    private static final String USER_3_PASSWORD_HASH = "";
    private static final String USER_3_FIRST_NAME = "user3";
    private static final String USER_3_LAST_NAME = "3user";

    /**
     * Values in this string are separated by semicolon (;)
     * username: user3
     * enabled: true
     * role: USER
     * email:
     * albumRoles:
     * password:
     * first name: user3
     * last name: 3user
     * lastPasswordResetDate:
     */
    private static final String USER_3_STRING = String.join(";", USER_3_USERNAME, USER_3_ENABLED.toString(), USER_3_ROLE.toString(), USER_3_EMAIL, USER_3_ALBUM_ROLES, USER_3_PASSWORD_HASH, USER_3_FIRST_NAME, USER_3_LAST_NAME, "");


    @BeforeEach
    void setUp() throws IOException {

        userDao = new UserDaoComponent();

        // Create an in-memory file system
        fileSystem = Jimfs.newFileSystem(UUID.randomUUID().toString(), Configuration.unix());

        // Define the root directory for the file system
        Path rootDir = fileSystem.getPath("/");

        // Create a directory
        albumBasePath = rootDir.resolve("/var/lib/kalbum");
        Files.createDirectories(albumBasePath);

        userDao.setAlbumBasePath(albumBasePath);

    }

    @AfterEach
    void tearDown() throws IOException {
        fileSystem.close();
    }

    private void loadTestUsers() {
        try {
            FileUtils.copy(String.join("\n", USER_1_STRING, USER_2_STRING).getBytes(StandardCharsets.UTF_8), albumBasePath.resolve("users"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        userDao.readAllFromPersistentStorage();
    }


    @Test
    void testCheckInitializedNotInitialized() throws IOException {
        assertThrows(IllegalStateException.class, () -> userDao.checkInitialized());

        Files.writeString(albumBasePath.resolve("users"), "");

        userDao.readAllFromPersistentStorage();

        userDao.checkInitialized();
    }

    @Test
    void testGetUsersPath() {
        assertEquals(albumBasePath.resolve("users"), userDao.getUsersPath());
    }

    @Test
    void testReadAllFromPersistentStorage() throws IOException {
        assertThrows(UncheckedIOException.class, () -> userDao.readAllFromPersistentStorage());

        try {
            FileUtils.copy(String.join("\n", USER_1_STRING, USER_2_STRING, USER_3_STRING).getBytes(StandardCharsets.UTF_8), albumBasePath.resolve("users"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        userDao.readAllFromPersistentStorage();

        assertEquals(3, userDao.findAll().size());
    }

    @Test
    void testGetAlbumRolesAsString() {
        assertEquals(Map.of("id1", Role.USER, "id2", Role.ADMIN), userDao.getAlbumRoles(USER_1_ALBUM_ROLES));
    }

    @Test
    void testGetAlbumRolesAsList() {
        assertEquals(USER_1_ALBUM_ROLES, userDao.getAlbumRoles(Map.of("id1", Role.USER, "id2", Role.ADMIN)));
    }

    @Test
    void testFindByUsername() {
        loadTestUsers();

        Optional<User> user = userDao.findByUsername(USER_1_USERNAME);

        assertTrue(user.isPresent());

        assertEquals(USER_1_USERNAME, user.get().getUsername());
        assertEquals(USER_1_ENABLED, user.get().isEnabled());
        assertEquals(USER_1_ROLE, user.get().getRole());
        assertEquals(USER_1_EMAIL, user.get().getEmail());
        assertEquals(Map.of("id1", Role.USER, "id2", Role.ADMIN), user.get().getAlbumRoles());
    }

    @Test
    void testFindByEmail() {
        loadTestUsers();

        Optional<User> user = userDao.findByEmail(USER_1_EMAIL);

        assertTrue(user.isPresent());

        assertEquals(USER_1_USERNAME, user.get().getUsername());
        assertEquals(USER_1_ENABLED, user.get().isEnabled());
        assertEquals(USER_1_ROLE, user.get().getRole());
        assertEquals(USER_1_EMAIL, user.get().getEmail());
        assertEquals(Map.of("id1", Role.USER, "id2", Role.ADMIN), user.get().getAlbumRoles());
    }

    @Test
    void testSave() throws IOException {
        loadTestUsers();

        User user = new User();
        user.setUsername(USER_1_USERNAME);
        user.setEnabled(false);
        user.setRole(USER_1_ROLE);
        user.setEmail(USER_1_EMAIL);
        user.setFirstName(USER_1_FIRST_NAME);
        user.setLastName(USER_1_LAST_NAME);
        user.setAlbumRoles(new ConcurrentHashMap<>(Map.of("id1", Role.ADMIN, "id2", Role.ADMIN)));

        userDao.save(user);

        Set<User> users = userDao.findAll();
        assertEquals(2, users.size());

        User savedUser = users.stream().filter(u -> u.getUsername().equals(USER_1_USERNAME)).findFirst().orElseThrow();

        assertEquals(USER_1_USERNAME, savedUser.getUsername());
        assertFalse(savedUser.isEnabled());
        assertEquals(USER_1_ROLE, savedUser.getRole());
        assertEquals(USER_1_EMAIL, savedUser.getEmail());
        assertEquals(Map.of("id1", Role.ADMIN, "id2", Role.ADMIN), savedUser.getAlbumRoles());

        List<String> lines = Files.readAllLines(albumBasePath.resolve("users"));

        assertEquals(2, lines.size());


        String user1_1 = String.join(";", USER_1_USERNAME, "false", USER_1_ROLE.toString(), USER_1_EMAIL, "id1:ADMIN,id2:ADMIN", USER_1_PASSWORD_HASH, USER_1_FIRST_NAME, USER_1_LAST_NAME, "");
        assertTrue(lines.contains(user1_1));

        String user2_1 = String.join(";", USER_2_USERNAME, "false", USER_2_ROLE.toString(), USER_2_EMAIL, "id1:USER,id2:ADMIN", USER_2_PASSWORD_HASH, USER_2_FIRST_NAME, USER_2_LAST_NAME, "");
        assertTrue(lines.contains(user2_1));
    }

    @Test
    void testDelete() throws IOException {
        loadTestUsers();

        User user = this.userDao.findByUsername(USER_1_USERNAME).orElseThrow();

        this.userDao.delete(user);

        assertEquals(1, this.userDao.findAll().size());

        List<String> lines = Files.readAllLines(albumBasePath.resolve("users"));

        assertEquals(1, lines.size());

        String user2_1 = String.join(";", USER_2_USERNAME, "false", USER_2_ROLE.toString(), USER_2_EMAIL, "id1:USER,id2:ADMIN", USER_2_PASSWORD_HASH, USER_2_FIRST_NAME, USER_2_LAST_NAME, "");
        assertTrue(lines.contains(user2_1));
    }

    @Test
    void testDeleteByUsername() throws IOException {
        loadTestUsers();

        this.userDao.deleteByUsername(USER_1_USERNAME);

        assertEquals(1, this.userDao.findAll().size());

        List<String> lines = Files.readAllLines(albumBasePath.resolve("users"));

        assertEquals(1, lines.size());

        String user2_1 = String.join(";", USER_2_USERNAME, "false", USER_2_ROLE.toString(), USER_2_EMAIL, "id1:USER,id2:ADMIN", USER_2_PASSWORD_HASH, USER_2_FIRST_NAME, USER_2_LAST_NAME, "");
        assertTrue(lines.contains(user2_1));
    }

    @Test
    void testFindAll() {
        loadTestUsers();

        Set<User> users = userDao.findAll();

        assertEquals(2, users.size());

        User user1 = users.stream().filter(u -> u.getUsername().equals(USER_1_USERNAME)).findFirst().orElseThrow();
        assertEquals(USER_1_USERNAME, user1.getUsername());
        assertEquals(USER_1_ENABLED, user1.isEnabled());
        assertEquals(USER_1_ROLE, user1.getRole());
        assertEquals(USER_1_EMAIL, user1.getEmail());
        assertEquals(Map.of("id1", Role.USER, "id2", Role.ADMIN), user1.getAlbumRoles());

        User user2 = users.stream().filter(u -> u.getUsername().equals(USER_2_USERNAME)).findFirst().orElseThrow();
        assertEquals(USER_2_USERNAME, user2.getUsername());
        assertEquals(USER_2_ENABLED, user2.isEnabled());
        assertEquals(USER_2_ROLE, user2.getRole());
        assertEquals(USER_2_EMAIL, user2.getEmail());
        assertEquals(Map.of("id1", Role.USER, "id2", Role.ADMIN), user2.getAlbumRoles());
    }

    @Test
    void testSetPasswordHash() throws IOException {
        loadTestUsers();

        userDao.setPasswordHash(USER_1_USERNAME, "newHash");

        List<String> lines = Files.readAllLines(albumBasePath.resolve("users"));

        assertEquals(2, lines.size());

        String userString = String.join(";", USER_1_USERNAME, USER_1_ENABLED.toString(), USER_1_ROLE.toString(), USER_1_EMAIL, USER_1_ALBUM_ROLES, "newHash", USER_1_FIRST_NAME, USER_1_LAST_NAME, "");
        assertTrue(lines.contains(userString));

        userDao.getPasswordHash(USER_1_USERNAME).ifPresentOrElse(
                hash -> assertEquals("newHash", hash),
                () -> fail("Password hash not found"));
    }

    @Test
    void testGetPasswordHash() {
        loadTestUsers();

        userDao.getPasswordHash(USER_1_USERNAME).ifPresentOrElse(
                hash -> assertEquals(USER_1_PASSWORD_HASH, hash),
                () -> fail("Password hash not found"));
    }
}