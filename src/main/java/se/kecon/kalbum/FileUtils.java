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

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Utility class for files.
 *
 * @author Kenny Colliander
 * @since 2023-08-03
 */
public class FileUtils {

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
}
