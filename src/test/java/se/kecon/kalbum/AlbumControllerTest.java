/**
 * Kalbum
 * <p>
 * Copyright 2023 Kenny Colliander
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.kecon.kalbum;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import se.kecon.kalbum.auth.AlbumAuthorizationManager;
import se.kecon.kalbum.auth.UserController;
import se.kecon.kalbum.auth.UserDao;
import se.kecon.kalbum.util.FileUtils;
import se.kecon.kalbum.validation.IllegalAlbumIdException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static se.kecon.kalbum.util.FileUtils.copy;

/**
 * Test the album controller
 * <p>
 * Created by Kenny Colliander on 2023-07-31.
 * </p>
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class AlbumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlbumController albumController;

    private FileSystem fileSystem;

    @MockBean
    private AlbumDao albumDao;

    @MockBean
    private PreviewSupport previewSupport;

    @Mock
    private Album album;

    @Mock
    private ContentData contentData1;

    @Mock
    private ContentData contentData2;

    @MockBean
    private UserController userController;

    @MockBean
    private UserDao userDao;

    @MockBean
    private AlbumAuthorizationManager albumAuthorizationManager;

    private Path albumBasePath;


    @BeforeEach
    void setUp() throws IOException, IllegalAlbumIdException {

        // Create an in-memory file system
        fileSystem = Jimfs.newFileSystem(UUID.randomUUID().toString(), Configuration.unix());

        // Define the root directory for the file system
        Path rootDir = fileSystem.getPath("/");

        // Create a directory
        albumBasePath = rootDir.resolve("/var/lib/kalbum");
        Files.createDirectories(albumBasePath);

        albumController.setAlbumBasePath(albumBasePath);

        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream("id1.json")) {
            requireNonNull(inputStream, "Could not find file id1.json");
            Files.copy(inputStream, albumBasePath.resolve("id1.json"));
        }

        copy("".getBytes(), albumBasePath.resolve("users"));

        when(albumAuthorizationManager.isAlbumAdmin(any(), any())).thenReturn(true);
        when(albumAuthorizationManager.isAlbumUser(any(), any())).thenReturn(true);

        when(contentData1.getAlt()).thenReturn("Describes IMG_8653.jpg");
        when(contentData1.getSrc()).thenReturn("IMG_8653.jpg");
        when(contentData1.getContentType()).thenReturn("image/jpeg");
        when(contentData1.getWidth()).thenReturn(6720);
        when(contentData1.getHeight()).thenReturn(4480);

        when(contentData2.getAlt()).thenReturn("Describes IMG_8666.mp4");
        when(contentData2.getSrc()).thenReturn("IMG_8666.mp4");
        when(contentData2.getContentType()).thenReturn("video/mp4");
        when(contentData2.getWidth()).thenReturn(1280);
        when(contentData2.getHeight()).thenReturn(720);

        when(album.getId()).thenReturn("id1");
        when(album.getName()).thenReturn("Test album");
        when(album.getContents()).thenReturn(Collections.singletonList(contentData1));

        when(albumDao.get("id1")).thenReturn(Optional.of(album));

        when(albumDao.getAll()).thenReturn(Collections.singletonList(album));

        Path thumbnail = albumBasePath.resolve("id1").resolve("contents").resolve(".thumbnails").resolve("IMG_8653.jpg");
        Files.createDirectories(thumbnail.getParent());
        Files.write(thumbnail, new byte[]{1, 2, 3});

        Path thumbnail2 = albumBasePath.resolve("id1").resolve("contents").resolve(".thumbnails").resolve("IMG_8666.png");
        Files.write(thumbnail2, new byte[]{1, 2, 3});

    }

    @AfterEach
    void tearDown() throws IOException {
        fileSystem.close();
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testListAlbumContents() throws Exception {
        mockMvc.perform(get("/albums/id1/contents/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("[0].alt").value("Describes IMG_8653.jpg"))
                .andExpect(jsonPath("[0].src").value("IMG_8653.jpg"))
                .andExpect(jsonPath("[0].contentType").value("image/jpeg"))
                .andExpect(jsonPath("[0].width").value(6720))
                .andExpect(jsonPath("[0].height").value(4480));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testListAlbumContentsInvalidAlbumId() throws Exception {
        mockMvc.perform(get("/albums/id2/contents/"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user1", roles = "ADMIN")
    void testUploadImage() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream("IMG_8653.jpg")) {
            requireNonNull(inputStream, "Could not find file IMG_8653.jpg");

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "IMG_8654.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    inputStream.readAllBytes()
            );

            mockMvc.perform(multipart("/albums/id1/contents/")
                            .file(file).with(csrf()))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @WithMockUser(username = "user1", roles = "ADMIN")
    void testUploadImageWithInvalidAlbumId() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream("IMG_8653.jpg")) {
            requireNonNull(inputStream, "Could not find file IMG_8653.jpg");

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "IMG_8654.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    inputStream.readAllBytes()
            );

            mockMvc.perform(multipart("/albums/id2/contents/")
                            .file(file).with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @WithMockUser(username = "user1", roles = "ADMIN")
    void testUploadImageWithInvalidContentType() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream("IMG_8653.jpg")) {
            requireNonNull(inputStream, "Could not find file IMG_8653.jpg");

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "IMG_8654.jpg",
                    MediaType.APPLICATION_JSON_VALUE,
                    inputStream.readAllBytes()
            );

            mockMvc.perform(multipart("/albums/id1/contents/")
                            .file(file).with(csrf()))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testListAlbums() throws Exception {
        mockMvc.perform(get("/albums/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("[0].id").value("id1"))
                .andExpect(jsonPath("[0].name").value("Test album"));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testGetAlbumWithInvalidId() throws Exception {
        mockMvc.perform(get("/albums/id2/"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user1", roles = "SUPERADMIN")
    void testCreateAlbum() throws Exception {
        when(albumAuthorizationManager.isAdmin(any())).thenReturn(true);
        when(albumDao.create(any())).thenReturn(album);

        mockMvc.perform(post("/albums/").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Test album\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "user2", roles = "NONE")
    void testCreateAlbumAsUser() throws Exception {
        mockMvc.perform(post("/albums/").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Test album\"}"))
                .andExpect(status().isForbidden());
        verify(albumDao, never()).create(any());
    }

    @Test
    @WithMockUser(username = "user1", roles = "ADMIN")
    void testPatchContent() throws Exception {
        mockMvc.perform(patch("/albums/id1/contents/IMG_8653.jpg").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alt\": \"Describes IMG_8653.jpg\",\"text\": \"Some text\"}"))
                .andExpect(status().isNoContent());

        verify(albumDao).update(album);
        verify(contentData1).setText("Some text");
        verify(contentData1).setAlt("Describes IMG_8653.jpg");
    }

    @Test
    @WithMockUser(username = "user1", roles = "ADMIN")
    void testPatchContentWithInvalidAlbumId() throws Exception {
        mockMvc.perform(patch("/albums/id2/contents/IMG_8653.jpg").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alt\": \"Describes IMG_8653.jpg\",\"text\": \"Some text\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user1", roles = "ADMIN")
    void testPatchContentWithInvalidFilename() throws Exception {
        mockMvc.perform(patch("/albums/id1/contents/IMG_8654.jpg").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alt\": \"Describes IMG_8653.jpg\",\"text\": \"Some text\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user1", roles = "ADMIN")
    void testCreateContentAndGetContent()
            throws Exception {

        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream("IMG_8653.jpg")) {
            requireNonNull(inputStream, "Could not find file IMG_8653.jpg");
            byte[] bs = inputStream.readAllBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "IMG_8654.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    bs
            );

            mockMvc.perform(multipart("/albums/id1/contents/")
                            .file(file).with(csrf()))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/albums/id1/contents/IMG_8654.jpg"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_JPEG_VALUE)).andExpect(content().bytes(bs));
        }
    }

    @Test
    @WithMockUser(username = "user1", roles = "ADMIN")
    void testCreateContentWithInvalidAlbumId() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream("IMG_8653.jpg")) {
            requireNonNull(inputStream, "Could not find file IMG_8653.jpg");
            byte[] bs = inputStream.readAllBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "IMG_8654.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    bs
            );

            mockMvc.perform(multipart("/albums/id2/contents/") // <-- id2
                            .file(file).with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @WithMockUser(username = "user1", roles = "ADMIN")
    void testCreateContentWithInvalidContentType() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream("IMG_8653.jpg")) {
            requireNonNull(inputStream, "Could not find file IMG_8653.jpg");
            byte[] bs = inputStream.readAllBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "IMG_8654.jpg",
                    MediaType.APPLICATION_JSON_VALUE, // <--
                    bs
            );

            mockMvc.perform(multipart("/albums/id1/contents/")
                            .file(file).with(csrf()))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testGetContentWithInvalidAlbumId() throws Exception {
        mockMvc.perform(get("/albums/id2/contents/IMG_8653.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testGetContentWithInvalidFilename() throws Exception {
        mockMvc.perform(get("/albums/id1/contents/IMG_8654.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testGetThumbnailAsImage() throws Exception {

        mockMvc.perform(get("/albums/id1/contents/thumbnails/IMG_8653.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG_VALUE));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testGetThumbnailAsVideo() throws Exception {

        mockMvc.perform(get("/albums/id1/contents/thumbnails/IMG_8666.mp4"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testGetThumbnailWithInvalidAlbumId() throws Exception {
        mockMvc.perform(get("/albums/id2/contents/thumbnails/IMG_8653.jpg"))
                .andExpect(status().isNotFound());
    }


    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testGetThumbnailWithInvalidFilename() throws Exception {
        mockMvc.perform(get("/albums/id1/contents/thumbnails/IMG_8654.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user1", roles = "ADMIN")
    void testDeleteContent() throws Exception {
        mockMvc.perform(delete("/albums/id1/contents/IMG_8653.jpg").with(csrf()))
                .andExpect(status().isNoContent());

        verify(albumDao).update(album);
        verify(album).setContents(Collections.emptyList());
    }

    @Test
    @WithMockUser(username = "user1", roles = "ADMIN")
    void testDeleteContentWithInvalidAlbumId() throws Exception {
        mockMvc.perform(delete("/albums/id2/contents/IMG_8653.jpg").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user1", roles = "ADMIN")
    void testDeleteContentWithInvalidFilename() throws Exception {
        mockMvc.perform(delete("/albums/id1/contents/IMG_8654.jpg").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user1", roles = "ADMIN")
    void testDeleteAlbumWithInvalidId() throws Exception {
        mockMvc.perform(delete("/albums/id2/").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testGetPreview() throws Exception {

        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream("IMG_8653.jpg")) {
            requireNonNull(inputStream, "Could not find file IMG_8653.jpg");

            Files.copy(inputStream, FileUtils.getContentPath(albumBasePath, "id1", "preview.png"));

            mockMvc.perform(get("/albums/id1/preview.png"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE));
        }
    }
}