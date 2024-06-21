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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.kecon.kalbum.validation.IllegalAlbumIdException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the album dao component
 * <p>
 * Created by Kenny Colliander on 2023-08-04
 * </p>
 */
@Slf4j
class AlbumDaoComponentTest {


    private FileSystem fileSystem;

    private Path albumBasePath;

    private AlbumDaoComponent albumDao;

    @BeforeEach
    void setUp() throws IOException {

        albumDao = new AlbumDaoComponent();

        // Create an in-memory file system
        fileSystem = Jimfs.newFileSystem(UUID.randomUUID().toString(), Configuration.unix());

        // Define the root directory for the file system
        Path rootDir = fileSystem.getPath("/");

        // Create a directory
        albumBasePath = rootDir.resolve("/var/lib/kalbum");
        Files.createDirectories(albumBasePath);

        albumDao.setAlbumBasePath(albumBasePath);

        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream("id1.json")) {
            requireNonNull(inputStream, "\"id1.json\" not found");
            Files.copy(inputStream, albumBasePath.resolve("id1.json"));
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        fileSystem.close();
    }

    @Test
    void create() throws IOException, IllegalAlbumIdException {

        albumDao.setIdGenerator(() -> "id1");
        Album album = albumDao.create("name1");

        assertEquals("id1", album.getId());
        assertEquals("name1", album.getName());

        List<String> ids = new ArrayList<>(Set.of("id1", "id2"));
        albumDao.setIdGenerator(() -> ids.remove(0)); // return id1 first, then id2 to test that id2 is used

        Album album2 = albumDao.create("name2");

        assertEquals("id2", album2.getId());
        assertEquals("name2", album2.getName());

        assertTrue(Files.exists(albumBasePath.resolve("id1.json")));
        assertTrue(Files.exists(albumBasePath.resolve("id2.json")));

        // Validate content of id1.json
        Album album1 = albumDao.get("id1").orElseGet(() -> {
            fail("Album id1 not found");
            return null;
        });

        assertEquals("id1", album1.getId());
        assertEquals("name1", album1.getName());

        // Validate content of id2.json
        Album album3 = albumDao.get("id2").orElseGet(() -> {
            fail("Album id2 not found");
            return null;
        });

        assertEquals("id2", album3.getId());
        assertEquals("name2", album3.getName());

        ObjectMapper objectMapper = albumDao.getObjectMapper();

        try (InputStream inputStream = Files.newInputStream(albumBasePath.resolve("id1.json"))) {
            requireNonNull(inputStream, "\"id1.json\" not found");
            Album fileAlbum1 = objectMapper.readValue(inputStream, Album.class);

            assertEquals("id1", fileAlbum1.getId());
            assertEquals("name1", fileAlbum1.getName());
        }

        try (InputStream inputStream = Files.newInputStream(albumBasePath.resolve("id2.json"))) {
            requireNonNull(inputStream, "\"id2.json\" not found");
            Album fileAlbum2 = objectMapper.readValue(inputStream, Album.class);

            assertEquals("id2", fileAlbum2.getId());
            assertEquals("name2", fileAlbum2.getName());
        }
    }

    @Test
    void get() throws IOException, IllegalAlbumIdException {

        albumDao.setIdGenerator(() -> "id1");
        Album album = albumDao.create("name1");

        assertEquals("id1", album.getId());
        assertEquals("name1", album.getName());

        Album cache = albumDao.get("id1").orElseGet(() -> {
            fail("Album id1 not found");
            return null;
        });

        assertEquals("id1", cache.getId());
        assertEquals("name1", cache.getName());
        assertNotSame(album, cache);
    }

    @Test
    void getNonExisting() throws IOException, IllegalAlbumIdException {

        albumDao.setIdGenerator(() -> "id1");
        Album album = albumDao.create("name1");

        assertEquals("id1", album.getId());
        assertEquals("name1", album.getName());

        Optional<Album> cache = albumDao.get("id2");

        assertFalse(cache.isPresent());
    }

    @Test
    void getInvalidId() throws IOException {
        albumDao.setIdGenerator(() -> "id1");
        Album album = albumDao.create("name1");

        assertEquals("id1", album.getId());
        assertEquals("name1", album.getName());

        assertThrows(IllegalAlbumIdException.class, () -> albumDao.get("id1.json"));
    }

    @Test
    void getAll() throws IOException {
        List<String> ids = new ArrayList<>(Set.of("id1", "id2"));

        albumDao.setIdGenerator(() -> ids.remove(0));
        Album album = albumDao.create("name1");
        Album album2 = albumDao.create("name2");

        List<Album> list = albumDao.getAll();

        // Check that the list contains the two albums
        assertEquals(2, list.size());
        assertTrue(list.contains(album));
        assertTrue(list.contains(album2));

        // Check that returned objects are copies
        assertNotSame(album, list.get(list.indexOf(album)));
        assertNotSame(album2, list.get(list.indexOf(album2)));
    }

    @Test
    void update() throws IOException, IllegalAlbumIdException {
        albumDao.setIdGenerator(() -> "id1");
        Album album = albumDao.create("name1");

        album.setName("name2");

        albumDao.update(album);

        Album cache = albumDao.get("id1").orElseGet(() -> {
            fail("Album id1 not found");
            return null;
        });

        assertEquals("id1", cache.getId());
        assertEquals("name2", cache.getName());
        assertNotSame(album, cache);

        try (InputStream inputStream = Files.newInputStream(albumBasePath.resolve("id1.json"))) {
            requireNonNull(inputStream, "\"id1.json\" not found");
            Album fileAlbum = albumDao.getObjectMapper().readValue(inputStream, Album.class);

            assertEquals("id1", fileAlbum.getId());
            assertEquals("name2", fileAlbum.getName());
        }
    }

    @Test
    void updateNonExisting() throws IOException {
        albumDao.setIdGenerator(() -> "id1");
        Album album = albumDao.create("name1");

        album.setName("name2");
        album.setId("id2");

        assertThrows(IllegalAlbumIdException.class, () -> albumDao.update(album));
    }

    @Test
    void updateInvalidId() throws IOException {
        albumDao.setIdGenerator(() -> "id1");
        Album album = albumDao.create("name1");

        album.setName("name2");
        album.setId("id1.json");

        assertThrows(IllegalAlbumIdException.class, () -> albumDao.update(album));
    }

    @Test
    void delete() throws IOException, IllegalAlbumIdException {
        albumDao.setIdGenerator(() -> "id1");
        Album album = albumDao.create("name1");

        assertTrue(Files.exists(albumBasePath.resolve("id1.json")));

        albumDao.delete(album.getId());

        assertFalse(Files.exists(albumBasePath.resolve("id1.json")));
    }

    @Test
    void getAlbumPath() throws IllegalAlbumIdException {
        assertEquals(albumBasePath.resolve("id1.json"), albumDao.getAlbumPath("id1"));
    }

    @Test
    void saveToPersistentStorage() throws IllegalAlbumIdException, IOException {

        Album album = new Album();
        album.setId("id1");
        album.setName("name1");
        album.setContents(new ArrayList<>());

        albumDao.saveToPersistentStorage(album);

        try (InputStream inputStream = Files.newInputStream(albumBasePath.resolve("id1.json"))) {
            requireNonNull(inputStream, "\"id1.json\" not found");
            Album fileAlbum = albumDao.getObjectMapper().readValue(inputStream, Album.class);

            assertEquals("id1", fileAlbum.getId());
            assertEquals("name1", fileAlbum.getName());
            assertEquals(0, fileAlbum.getContents().size());
        }

        ContentData contentData = new ContentData();
        contentData.setSrc("src1.png");
        contentData.setContentType("image/png");

        Album album2 = new Album();
        album2.setId("id1");
        album2.setName("name2");
        album2.setContents(new ArrayList<>(Collections.singleton(contentData)));

        albumDao.saveToPersistentStorage(album2);

        try (InputStream inputStream = Files.newInputStream(albumBasePath.resolve("id1.json"))) {
            requireNonNull(inputStream, "\"id1.json\" not found");
            Album fileAlbum = albumDao.getObjectMapper().readValue(inputStream, Album.class);

            assertEquals("id1", fileAlbum.getId());
            assertEquals("name2", fileAlbum.getName());
            assertEquals(1, fileAlbum.getContents().size());
            assertEquals("src1.png", fileAlbum.getContents().get(0).getSrc());
            assertEquals("image/png", fileAlbum.getContents().get(0).getContentType());
        }
    }

    @Test
    void getFromPersistentStorage() throws IllegalAlbumIdException, IOException {

        ContentData contentData = new ContentData();
        contentData.setSrc("src1.png");
        contentData.setContentType("image/png");

        Album album = new Album();
        album.setId("id1");
        album.setName("name2");
        album.setContents(new ArrayList<>(Collections.singleton(contentData)));

        ObjectMapper objectMapper = albumDao.getObjectMapper();

        try (OutputStream outputStream = Files.newOutputStream(albumBasePath.resolve("id1.json"))) {
            objectMapper.writeValue(outputStream, album);
        }

        Album persistentAlbum = albumDao.getFromPersistentStorage("id1").orElseGet(() -> {
            fail("Album id1 not found");
            return null;
        });

        assertEquals("id1", persistentAlbum.getId());
        assertEquals("name2", persistentAlbum.getName());
        assertEquals(1, persistentAlbum.getContents().size());
        assertEquals("src1.png", persistentAlbum.getContents().get(0).getSrc());
        assertEquals("image/png", persistentAlbum.getContents().get(0).getContentType());
    }

    @Test
    void readAllFromPersistentStorage() throws IOException {

        ContentData contentData = new ContentData();
        contentData.setSrc("src1.png");
        contentData.setContentType("image/png");

        Album album = new Album();
        album.setId("id1");
        album.setName("name1");
        album.setContents(new ArrayList<>(Collections.singleton(contentData)));

        Album album2 = new Album();
        album2.setId("id2");
        album2.setName("name2");
        album2.setContents(new ArrayList<>());

        ObjectMapper objectMapper = albumDao.getObjectMapper();

        try (OutputStream outputStream = Files.newOutputStream(albumBasePath.resolve("id1.json"));
             OutputStream outputStream2 = Files.newOutputStream(albumBasePath.resolve("id2.json"))) {

            objectMapper.writeValue(outputStream, album);
            objectMapper.writeValue(outputStream2, album2);
        } catch (IOException e) {
            fail("Unexpected IOException");
        }

        albumDao.readAllFromPersistentStorage();

        List<Album> list = albumDao.getAll();
        assertEquals(2, list.size());

        int index1 = list.indexOf(album);

        assertEquals("id1", list.get(index1).getId());
        assertEquals("name1", list.get(index1).getName());
        assertEquals(1, list.get(index1).getContents().size());
        assertEquals("src1.png", list.get(index1).getContents().get(0).getSrc());
        assertEquals("image/png", list.get(index1).getContents().get(0).getContentType());

        int index2 = list.indexOf(album2);

        assertEquals("id2", list.get(index2).getId());
        assertEquals("name2", list.get(index2).getName());
        assertEquals(0, list.get(index2).getContents().size());
    }
}