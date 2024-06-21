package se.kecon.kalbum.auth;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import se.kecon.kalbum.AlbumDaoComponent;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static se.kecon.kalbum.util.FileUtils.copy;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserDao userDao;

    @MockBean
    private AlbumDaoComponent albumDao;

    @Mock
    private User user1;

    private FileSystem fileSystem;

    Path albumBasePath;

    private static final String USER_1_USERNAME = "user1";

    private static final String USER_1_EMAIL = "user1@example.com";
    private static final String USER_1_EMAIL_NEW = "user1.new@example.com";

    private static final String USER_1_PASSWORD = "password";
    private static final String USER_1_PASSWORD_HASH = "$2a$10$J0cOKWuvnRcfAXN61SZlJubeXlhBVT0z052iQiMEw6EObuenBZblK";


    @Mock
    private User user2;

    private static final String USER_2_USERNAME = "user2";

    private static final String USER_2_EMAIL = "user2@example.com";

    private static final String USER_2_EMAIL_NEW = "user2.new@example.com";

    private static final String ALBUM_ID_1 = "album1";

    private static final String ALBUM_ID_2 = "album2";


    @BeforeEach
    void setUp() throws IOException {
        // Create an in-memory file system
        fileSystem = Jimfs.newFileSystem(UUID.randomUUID().toString(), Configuration.unix());

        // Define the root directory for the file system
        Path rootDir = fileSystem.getPath("/");

        // Create a directory
        albumBasePath = rootDir.resolve("/var/lib/kalbum");
        Files.createDirectories(albumBasePath);

        copy("".getBytes(), albumBasePath.resolve("users"));

        albumDao.setAlbumBasePath(albumBasePath);

        when(user1.getUsername()).thenReturn(USER_1_USERNAME);
        when(user1.getEmail()).thenReturn(USER_1_EMAIL);
        when(user1.getRole()).thenReturn(Role.SUPERADMIN);
        when(user1.isEnabled()).thenReturn(true);

        when(userDao.findByUsername(USER_1_USERNAME)).thenReturn(Optional.of(user1));
        when(userDao.findByEmail(USER_1_EMAIL)).thenReturn(Optional.of(user1));
        when(userDao.findAll()).thenReturn(Set.of(user1));
        when(userDao.getPasswordHash(USER_1_USERNAME)).thenReturn(Optional.of("$2a$10$J0cOKWuvnRcfAXN61SZlJubeXlhBVT0z052iQiMEw6EObuenBZblK"));
    }

    @AfterEach
    void tearDown() throws IOException {
        fileSystem.close();
    }

    @Test
    @WithMockUser(username = "user1")
    void createUser() throws Exception {
        when(userDao.findByUsername(USER_2_USERNAME)).thenReturn(Optional.empty());
        when(userDao.findByEmail(USER_2_EMAIL)).thenReturn(Optional.empty());

        mockMvc.perform(post("/users/").with(csrf())
                        .param("username", USER_2_USERNAME).param("email", USER_2_EMAIL))
                .andExpect(status().isCreated()).andExpect(header().string("Location", USER_2_USERNAME));

        verify(userDao, times(1)).findByUsername(USER_2_USERNAME);
        verify(userDao, times(1)).findByEmail(USER_2_EMAIL);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userDao, times(1)).save((User) userCaptor.capture());
        User user = (User) userCaptor.getValue();

        assertEquals(USER_2_USERNAME, user.getUsername());
        assertEquals(USER_2_EMAIL, user.getEmail());
        assertEquals(Role.USER, user.getRole());
        assertTrue(user.isEnabled());
    }

    @Test
    @WithMockUser(username = "user1")
    void getUsers() throws Exception {
        when(userDao.findAll()).thenReturn(Set.of(user1));

        mockMvc.perform(get("/users/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].username").value(USER_1_USERNAME))
                .andExpect(jsonPath("$[0].email").value(USER_1_EMAIL))
                .andExpect(jsonPath("$[0].role").value(Role.SUPERADMIN.toString()))
                .andExpect(jsonPath("$[0].enabled").value(true));

        verify(userDao, times(1)).findAll();
    }

    @Test
    @WithMockUser(username = "user2")
    void getUsersAsNonAdmin() throws Exception {
        when(user2.getUsername()).thenReturn(USER_2_USERNAME);
        when(user2.getEmail()).thenReturn(USER_2_EMAIL);
        when(user2.getRole()).thenReturn(Role.USER);
        when(user2.isEnabled()).thenReturn(true);

        when(userDao.findByUsername(USER_2_USERNAME)).thenReturn(Optional.of(user2));
        when(userDao.findAll()).thenReturn(Set.of(user1));

        mockMvc.perform(get("/users/"))
                .andExpect(status().isForbidden());

        verify(userDao, never()).findAll();
    }

    @Test
    @WithMockUser(username = "user1")
    void getUser() throws Exception {
        when(userDao.findByUsername(USER_1_USERNAME)).thenReturn(Optional.of(user1));

        mockMvc.perform(get("/users/user1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value(USER_1_USERNAME))
                .andExpect(jsonPath("$.email").value(USER_1_EMAIL))
                .andExpect(jsonPath("$.role").value(Role.SUPERADMIN.toString()))
                .andExpect(jsonPath("$.enabled").value(true));

        verify(userDao, times(2)).findByUsername(USER_1_USERNAME);
    }

    @Test
    @WithMockUser(username = "user1")
    void deleteUser() throws Exception {
        mockMvc.perform(delete("/users/{username}", USER_1_USERNAME).with(csrf()))
                .andExpect(status().isNoContent());

        verify(userDao, times(1)).deleteByUsername(USER_1_USERNAME);
    }

    @WithMockUser(username = "user2")
    @Test
    void deleteUserForbidden() throws Exception {
        mockMvc.perform(delete("/users/{username}", USER_1_USERNAME))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user1")
    void updateUserAsSelf() throws Exception {
        User user = new User();
        user.setUsername(USER_1_USERNAME);
        user.setEmail(USER_1_EMAIL);
        user.setRole(Role.USER);
        user.setEnabled(true);

        when(userDao.findByUsername(USER_1_USERNAME)).thenReturn(Optional.of(user));

        mockMvc.perform(patch("/users/{username}", USER_1_USERNAME).with(csrf())
                        .param("email", USER_1_EMAIL_NEW))
                .andExpect(status().isNoContent());

        User expectedUser = new User();
        expectedUser.setUsername(USER_1_USERNAME);
        expectedUser.setEmail(USER_1_EMAIL_NEW);
        expectedUser.setRole(Role.USER);
        expectedUser.setEnabled(true);

        verify(userDao, times(1)).save(expectedUser);
    }

    @Test
    @WithMockUser(username = "user1")
    void updateUserAsAdmin() throws Exception {
        User user = new User();
        user.setUsername(USER_2_USERNAME);
        user.setEmail(USER_2_EMAIL);
        user.setRole(Role.USER);
        user.setEnabled(true);

        when(userDao.findByUsername(USER_2_USERNAME)).thenReturn(Optional.of(user));

        mockMvc.perform(patch("/users/{username}", USER_2_USERNAME).with(csrf())
                        .param("email", USER_2_EMAIL_NEW))
                .andExpect(status().isNoContent());

        User expectedUser = new User();
        expectedUser.setUsername(USER_2_USERNAME);
        expectedUser.setEmail(USER_2_EMAIL_NEW);
        expectedUser.setRole(Role.USER);
        expectedUser.setEnabled(true);

        verify(userDao, times(1)).save(expectedUser);
    }

    @Test
    @WithMockUser(username = "user2")
    void updateUserOtherUserAsNonAdmin() throws Exception {
        when(user2.getUsername()).thenReturn(USER_2_USERNAME);
        when(user2.getEmail()).thenReturn(USER_2_EMAIL);
        when(user2.getRole()).thenReturn(Role.USER);
        when(user2.isEnabled()).thenReturn(true);

        when(userDao.findByUsername(USER_2_USERNAME)).thenReturn(Optional.of(user2));

        mockMvc.perform(patch("/users/{username}", USER_1_USERNAME).with(csrf())
                        .param("email", USER_1_EMAIL_NEW))
                .andExpect(status().isForbidden());

        verify(userDao, never()).save(any());
    }


    @Test
    @WithMockUser(username = "user1")
    void updatePassword() throws Exception {
        when(userDao.getPasswordHash(USER_1_USERNAME)).thenReturn(Optional.of(USER_1_PASSWORD_HASH));

        mockMvc.perform(put("/users/{username}/password", USER_1_USERNAME).with(csrf())
                        .param("newPassword", "lösenord").param("password", "password"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<String> passwordHashCaptor = ArgumentCaptor.forClass(String.class);
        verify(userDao, times(1)).setPasswordHash(eq(USER_1_USERNAME), passwordHashCaptor.capture());
        String passwordHash = passwordHashCaptor.getValue();

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        assertTrue(passwordEncoder.matches("lösenord", passwordHash));
    }

    @Test
    @WithMockUser(username = "user2")
    void updatePasswordForOtherUser() throws Exception {
        mockMvc.perform(put("/users/{username}/password", USER_1_USERNAME).with(csrf())
                        .param("newPassword", "lösenord").param("password", "password"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user1")
    void updateRole() throws Exception {
        User user = new User();
        user.setUsername(USER_2_USERNAME);
        user.setEmail(USER_2_EMAIL);
        user.setRole(Role.USER);
        user.setEnabled(true);

        when(userDao.findByUsername(USER_2_USERNAME)).thenReturn(Optional.of(user));

        mockMvc.perform(patch("/users/{username}", USER_2_USERNAME).with(csrf())
                        .param("role", Role.ADMIN.toString()))
                .andExpect(status().isNoContent());

        User expectedUser = new User();
        expectedUser.setUsername(USER_2_USERNAME);
        expectedUser.setEmail(USER_2_EMAIL);
        expectedUser.setRole(Role.ADMIN);
        expectedUser.setEnabled(true);

        verify(userDao, times(2)).findByUsername(USER_1_USERNAME);
        verify(userDao, times(3)).findByUsername(USER_2_USERNAME);
        verify(userDao, times(1)).save(expectedUser);
    }

    @Test
    @WithMockUser(username = "user2")
    void updateRoleAsNonAdmin() throws Exception {
        User user = new User();
        user.setUsername(USER_2_USERNAME);
        user.setEmail(USER_2_EMAIL);
        user.setRole(Role.USER);
        user.setEnabled(true);

        when(userDao.findByUsername(USER_2_USERNAME)).thenReturn(Optional.of(user));

        mockMvc.perform(patch("/users/{username}", USER_2_USERNAME).with(csrf())
                        .param("role", Role.ADMIN.toString()))
                .andExpect(status().isForbidden());

        verify(userDao, times(4)).findByUsername(USER_2_USERNAME);
        verifyNoMoreInteractions(userDao);
    }

    @Test
    @WithMockUser(username = "user1")
    void updateAlbumRoleAsAdmin() throws Exception {
        User user = new User();
        user.setUsername(USER_2_USERNAME);
        user.setEmail(USER_2_EMAIL);
        user.setRole(Role.USER);
        user.setEnabled(true);
        user.setAlbumRoles(new ConcurrentHashMap<>(Map.of(ALBUM_ID_2, Role.USER)));

        when(userDao.findByUsername(USER_2_USERNAME)).thenReturn(Optional.of(user));

        mockMvc.perform(put("/users/{username}/albumRoles/{albumId}", USER_2_USERNAME, ALBUM_ID_1).with(csrf())
                        .param("role", Role.USER.toString()))
                .andExpect(status().isNoContent());

        User expectedUser = new User();
        expectedUser.setUsername(USER_2_USERNAME);
        expectedUser.setEmail(USER_2_EMAIL);
        expectedUser.setRole(Role.USER);
        expectedUser.setEnabled(true);
        expectedUser.setAlbumRoles(new ConcurrentHashMap<>(Map.of(ALBUM_ID_1, Role.USER, ALBUM_ID_2, Role.USER)));

        verify(userDao, times(1)).findByUsername(USER_1_USERNAME);
        verify(userDao, times(2)).findByUsername(USER_2_USERNAME);
        verify(userDao, times(1)).save(expectedUser);
    }

    @Test
    @WithMockUser(username = "user2")
    void updateAlbumRoleAsNonAdmin() throws Exception {
        User user = new User();
        user.setUsername(USER_2_USERNAME);
        user.setEmail(USER_2_EMAIL);
        user.setRole(Role.USER);
        user.setEnabled(true);
        user.setAlbumRoles(new ConcurrentHashMap<>(Map.of(ALBUM_ID_2, Role.USER)));

        when(userDao.findByUsername(USER_2_USERNAME)).thenReturn(Optional.of(user));

        mockMvc.perform(put("/users/{username}/albumRoles/{albumId}", USER_2_USERNAME, ALBUM_ID_1).with(csrf())
                        .param("role", Role.USER.toString()))
                .andExpect(status().isForbidden());

        verify(userDao, times(2)).findByUsername(USER_2_USERNAME);
        verify(userDao, never()).save(any());
    }
}