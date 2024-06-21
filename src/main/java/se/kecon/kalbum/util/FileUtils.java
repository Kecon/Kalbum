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

import org.springframework.web.multipart.MultipartFile;
import se.kecon.kalbum.ContentFormat;
import se.kecon.kalbum.UnsupportedContentFormatException;
import se.kecon.kalbum.validation.IllegalAlbumIdException;
import se.kecon.kalbum.validation.IllegalFilenameException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static se.kecon.kalbum.validation.Validation.checkValidAlbumId;
import static se.kecon.kalbum.validation.Validation.checkValidFilename;

/**
 * Utility class for files.
 *
 * @author Kenny Colliander
 * @since 2023-08-03
 */
public class FileUtils {

    public static final String CONTENT_PATH = "contents";

    public static final String THUMBNAIL_PATH = ".thumbnails";

    /**
     * Hide constructor
     */
    private FileUtils() {
    }

    /**
     * Get the filename of a file
     *
     * @param file the file
     * @return the filename
     */
    public static String getFilename(final MultipartFile file) throws UnsupportedContentFormatException {
        final String filename = file.getOriginalFilename();

        if (filename == null || filename.isEmpty()) {
            return UUID.randomUUID() + "." + ContentFormat.detectFileType(file).name().toLowerCase();
        } else {
            return filename;
        }
    }

    /**
     * Get filename without suffix
     *
     * @param filename the filename
     * @return the filename without suffix
     * @throws IllegalArgumentException if the filename doesn't contain any suffix
     */
    public static String removeSuffix(final String filename) {

        if (filename == null) {
            throw new IllegalArgumentException("Invalid filename");
        }

        final int index = filename.lastIndexOf('.');
        if (index == -1) {
            throw new IllegalArgumentException("Invalid filename");
        }
        return filename.substring(0, index);
    }

    /**
     * Copy a byte array to a path
     *
     * @param bs   the byte array
     * @param path the path
     * @throws IOException if an I/O error occurs
     */
    public static void copy(byte[] bs, Path path) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bs)) {
            Files.copy(inputStream, path);
        }
    }

    /**
     * Get the path to the thumbnail for the given content
     *
     * @param albumBasePath base path
     * @param id            album id
     * @param filename      content filename
     * @param contentFormat content format
     * @return path
     * @throws IllegalAlbumIdException  if the id is invalid
     * @throws IllegalFilenameException if the filename is invalid
     */
    public static Path getThumbnailPath(final Path albumBasePath, final String id, final String filename, final ContentFormat contentFormat) throws IllegalAlbumIdException, IllegalFilenameException {
        checkValidAlbumId(id);
        checkValidFilename(filename);

        if (contentFormat.isImage()) {
            return albumBasePath.resolve(id).resolve(CONTENT_PATH).resolve(THUMBNAIL_PATH).resolve(filename);
        } else {
            return albumBasePath.resolve(id).resolve(CONTENT_PATH).resolve(THUMBNAIL_PATH).resolve(removeSuffix(filename) + ".png");
        }
    }

    /**
     * Get the path to the content with the given id
     *
     * @param albumBasePath base path
     * @param id            album id
     * @param filename      content filename
     * @return path
     * @throws IllegalAlbumIdException  if the id is invalid
     * @throws IllegalFilenameException if the filename is invalid
     */
    public static Path getContentPath(final Path albumBasePath, final String id, final String filename) throws IllegalAlbumIdException, IllegalFilenameException {
        checkValidAlbumId(id);
        checkValidFilename(filename);

        return albumBasePath.resolve(id).resolve(CONTENT_PATH).resolve(filename);
    }

    /**
     * Encode a semicolon separated list to a string. The following characters are escaped: \n, \r, \t, \\, ;, \b, \f
     *
     * @param args the list to encode
     * @return the encoded string
     */
    public static String encodeSemicolonSeparatedList(final String... args) {
        if (args == null || args.length == 0) {
            return "";
        }

        final StringBuilder stringBuilder = new StringBuilder();

        for (final String arg : args) {
            if (!stringBuilder.isEmpty()) {
                stringBuilder.append(';');
            }

            if (arg == null) {
                continue;
            }

            for (final char c : arg.toCharArray()) {
                stringBuilder.append(switch (c) {
                    case '\n' -> "\\n"; // line feed
                    case '\r' -> "\\r"; // carriage return
                    case '\t' -> "\\t"; // tab
                    case '\\' -> "\\\\"; // backslash
                    case ';' -> "\\;"; // semicolon
                    case '\b' -> "\\b"; // backspace
                    case '\f' -> "\\f"; // form feed
                    default -> c;
                });
            }
        }

        return stringBuilder.toString();
    }

    /**
     * Decode a semicolon separated list from a string. The string is escaped with backslash for the
     * following characters: \n, \r, \t, \\, ;, \b, \f
     *
     * @param line the encoded string
     * @return the list
     */
    public static List<String> decodeSemicolonSeparatedList(String line) {
        if (line == null || line.isEmpty()) {
            return List.of();
        }

        final char[] chars = line.toCharArray();
        final StringBuilder stringBuilder = new StringBuilder();
        final List<String> list = new ArrayList<>();

        int index = 0;
        boolean escaped = false;

        while (index < chars.length) {
            final char c = chars[index];

            if (escaped) {
                escaped = false;

                stringBuilder.append(switch (c) {
                    case 'n' -> '\n'; // line feed
                    case 'r' -> '\r'; // carriage return
                    case 't' -> '\t'; // tab
                    case '\\' -> '\\'; // backslash
                    case ';' -> ';'; // semicolon
                    case 'b' -> '\b'; // backspace
                    case 'f' -> '\f'; // form feed

                    default -> "\\" + c;
                });

            } else if (c == '\\') {
                escaped = true;
            } else if (c == ';') {
                list.add(stringBuilder.toString());
                stringBuilder.setLength(0);
            } else {
                stringBuilder.append(c);
            }

            index++;
        }

        list.add(stringBuilder.toString());

        return list;
    }
}
