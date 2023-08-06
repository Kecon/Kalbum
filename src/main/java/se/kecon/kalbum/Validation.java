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

import java.util.regex.Pattern;

/**
 * Validation methods for album id and content filename.
 *
 * @author Kenny Colliander
 * @since 2023-08-03
 */
public class Validation {

    protected static final Pattern VALID_ALBUM_ID = Pattern.compile("^[_a-zA-Z0-9\\-]+$");

    protected static final Pattern VALID_FILENAME = Pattern.compile("^[^\\\\/:*?\"<>|]+\\.(?i)(png|jpe?g|mp4)$");


    /**
     * Check if the id is valid and not contain any invalid characters
     *
     * @param id album id
     * @throws IllegalAlbumIdException if the id is invalid
     */
    public static void checkValidAlbumId(final String id) throws IllegalAlbumIdException {
        if (!VALID_ALBUM_ID.matcher(id).matches()) {
            throw new IllegalAlbumIdException("Invalid album id: " + id);
        }
    }

    /**
     * Check if the filename is valid and not contain any invalid characters
     *
     * @param filename content filename
     * @throws IllegalFilenameException if the filename is invalid
     */
    public static void checkValidFilename(final String filename) throws IllegalFilenameException {
        if (!VALID_FILENAME.matcher(filename).matches()) {
            throw new IllegalFilenameException("Invalid filename: " + filename);
        }
    }
}
