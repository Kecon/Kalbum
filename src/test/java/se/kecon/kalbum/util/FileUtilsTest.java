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
package se.kecon.kalbum.util;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;
import se.kecon.kalbum.ContentFormat;
import se.kecon.kalbum.UnsupportedContentFormatException;
import se.kecon.kalbum.validation.IllegalAlbumIdException;
import se.kecon.kalbum.validation.IllegalFilenameException;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the file utils
 * <p>
 * Created by Kenny Colliander on 2023-08-04.
 * </p>
 */
class FileUtilsTest {

    @Test
    void testGetFilename() throws UnsupportedContentFormatException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("id1.png");

        assertEquals("id1.png", FileUtils.getFilename(file));
    }

    @Test
    void testRemoveSuffix() {
        assertEquals("id1", FileUtils.removeSuffix("id1.png"));
    }

    @Test
    void testRemoveSuffixDoubleDot() {
        assertEquals("id1.example", FileUtils.removeSuffix("id1.example.png"));
    }

    @Test
    void testRemoveSuffixWithNoSuffix() {
        assertThrows(IllegalArgumentException.class, () -> FileUtils.removeSuffix("id1"));
    }

    @Test
    void testRemoveSuffixNull() {
        assertThrows(IllegalArgumentException.class, () -> FileUtils.removeSuffix(null));
    }

    @Test
    void testCopy() throws IOException {
        try (final FileSystem fileSystem = Jimfs.newFileSystem(UUID.randomUUID().toString(), Configuration.unix())) {
            final Path path = fileSystem.getPath("/").resolve("test.tmp");
            final byte[] bytes = "Hello world".getBytes();

            FileUtils.copy(bytes, path);

            assertArrayEquals(bytes, Files.readAllBytes(path));
        }
    }

    @Test
    void testGetContentPath() throws IOException, IllegalAlbumIdException, IllegalFilenameException {
        try (final FileSystem fileSystem = Jimfs.newFileSystem(UUID.randomUUID().toString(), Configuration.unix())) {
            final Path path = fileSystem.getPath("/").resolve("var").resolve("lib").resolve("kalbum").resolve("id1").resolve("contents").resolve("test.png");

            assertEquals(path, FileUtils.getContentPath(fileSystem.getPath("/var/lib/kalbum"), "id1", "test.png"));
        }
    }

    @Test
    void testGetThumbnailPath() throws IOException, IllegalAlbumIdException, IllegalFilenameException {
        try (final FileSystem fileSystem = Jimfs.newFileSystem(UUID.randomUUID().toString(), Configuration.unix())) {
            final Path path = fileSystem.getPath("/").resolve("var").resolve("lib").resolve("kalbum").resolve("id1").resolve("contents").resolve(".thumbnails").resolve("test.png");

            assertEquals(path, FileUtils.getThumbnailPath(fileSystem.getPath("/var/lib/kalbum"), "id1", "test.png", ContentFormat.PNG));
        }
    }

    @Test
    void testEncodeSemicolonSeparatedListSimple() {
        assertEquals("a1;b2;c3", FileUtils.encodeSemicolonSeparatedList("a1", "b2", "c3"));
    }

    @Test
    void testEncodeSemicolonSeparatedListWithEscape() {
        assertEquals("a1\\n;b2\\r;c3\\t\\;boll;\\b\\f", FileUtils.encodeSemicolonSeparatedList("a1\n", "b2\r", "c3\t;boll", "\b\f"));
    }

    @Test
    void testEncodeSemicolonSeparatedListWithNull() {
        assertEquals("a1;b2;c3;", FileUtils.encodeSemicolonSeparatedList("a1", "b2", "c3", null));
    }

    @Test
    void testDecodeSemicolonSeparatedListSimple() {
        assertEquals(List.of("a1", "b2", "c3"), FileUtils.decodeSemicolonSeparatedList("a1;b2;c3"));
    }

    @Test
    void testDecodeSemicolonSeparatedListWithEscape() {
        assertEquals(List.of("a1\n", "b2\r", "c3\t;boll", "\b\f"), FileUtils.decodeSemicolonSeparatedList("a1\\n;b2\\r;c3\t\\;boll;\\b\\f"));
    }

    @Test
    void testDecodeSemicolonSeparatedListWithNull() {
        assertEquals(List.of("a1", "b2", "c3", ""), FileUtils.decodeSemicolonSeparatedList("a1;b2;c3;"));
    }
}