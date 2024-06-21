package se.kecon.kalbum.auth;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import jakarta.mail.Transport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import se.kecon.kalbum.AlbumDaoComponent;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static se.kecon.kalbum.util.FileUtils.copy;

@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@SpringBootTest()
public class PasswordResetControllerTest {

    @Autowired
    private PasswordResetController passwordResetController;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserDao userDao;

    @MockBean
    private AlbumDaoComponent albumDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private User user1;

    private Clock clock;

    private FileSystem fileSystem;

    Path albumBasePath;

    private static final String USER_1_USERNAME = "user1";

    private static final String USER_1_EMAIL = "user1@example.org";
    private static final String USER_1_EMAIL_NEW = "user1.new@example.org";

    private static final String USER_1_PASSWORD = "password";
    private static final String USER_1_PASSWORD_HASH = "$2a$10$J0cOKWuvnRcfAXN61SZlJubeXlhBVT0z052iQiMEw6EObuenBZblK";

    private static UUID RESET_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");


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

        lenient().when(user1.getUsername()).thenReturn(USER_1_USERNAME);
        lenient().when(user1.getEmail()).thenReturn(USER_1_EMAIL);
        lenient().when(user1.getRole()).thenReturn(Role.SUPERADMIN);
        lenient().when(user1.isEnabled()).thenReturn(true);

        when(userDao.findByUsername(USER_1_USERNAME)).thenReturn(Optional.of(user1));
        when(userDao.findByEmail(USER_1_EMAIL)).thenReturn(Optional.of(user1));
        when(userDao.findAll()).thenReturn(Set.of(user1));
        when(userDao.getPasswordHash(USER_1_USERNAME)).thenReturn(Optional.of(USER_1_PASSWORD_HASH));

        clock = Clock.fixed(Instant.parse("2024-06-01T00:00:00Z"), Clock.systemUTC().getZone());
        passwordResetController.setClock(clock);

        passwordResetController.setPasswordEncoder(passwordEncoder);

        lenient().when(passwordEncoder.encode("password")).thenReturn(USER_1_PASSWORD_HASH);
    }

    @AfterEach
    void tearDown() throws IOException {
        fileSystem.close();
    }


    @Test
    void testResetPasswordSend() throws Exception {
        try (MockedStatic<Transport> mockedTransport = Mockito.mockStatic(Transport.class)) {
            mockMvc.perform(post("/resetpassword/send").contentType("application/json").content("""        
                            {
                                "email": "user1@example.org",
                                "passphrase": "passphrase11"
                            }
                            """))
                    .andExpect(status().isOk());

            mockedTransport.verify(() -> Transport.send(Mockito.any()), Mockito.times(1));
            verify(userDao).findByEmail(USER_1_EMAIL);
        }
    }

    @Test
    void testResetPasswordPut() throws Exception {

        ResetPasswordData resetPasswordData = new ResetPasswordData(USER_1_EMAIL, clock.instant(), "passphrase11");
        passwordResetController.addResetPasswordData(RESET_UUID, resetPasswordData);

        mockMvc.perform(put("/resetpassword/" + RESET_UUID).contentType("application/json").content("""
                {
                    "email": "user1@example.org",
                    "passphrase": "passphrase11",
                    "password": "password"
                }
                """)).andExpect(status().isNoContent());

        verify(userDao).setPasswordHash(USER_1_USERNAME, USER_1_PASSWORD_HASH);
    }
}
