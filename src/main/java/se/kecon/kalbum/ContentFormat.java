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

import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Enum for supported content formats.
 *
 * @author Kenny Colliander
 * @since 2023-07-31
 */
@Getter
public enum ContentFormat {
    /**
     * JPEG
     */
    JPEG("image/jpeg", true, "jpeg", "jpg"),

    /**
     * PNG
     */
    PNG("image/png", true, "png"),

    /**
     * MP4
     */
    MP4("video/mp4", false, "mp4");

    private final String contentType;

    @Getter
    private final Set<String> suffixes;

    @Getter
    private final boolean image;

    /**
     * Constructor
     *
     * @param contentType the associated content type
     * @param image       true if the content is an image
     * @param suffixes    the associated suffixes
     */
    ContentFormat(final String contentType, final boolean image, final String... suffixes) {
        this.contentType = contentType;
        this.suffixes = Set.of(suffixes);
        this.image = image;
    }

    /**
     * Get the content format from the suffix of the filename.
     *
     * @param filename The filename
     * @return The content format
     * @throws UnsupportedContentFormatException If the file type is not supported
     */
    public static ContentFormat getContentFormat(final String filename) throws UnsupportedContentFormatException {
        final String suffix = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        for (ContentFormat contentFormat : ContentFormat.values()) {
            if (contentFormat.getSuffixes().contains(suffix)) {
                return contentFormat;
            }
        }
        throw new UnsupportedContentFormatException("Unsupported file type.");
    }

    /**
     * Detect the file type from the content type of the file.
     *
     * @param file The file
     * @return The content format
     * @throws UnsupportedContentFormatException If the file is null or empty, or if the content type could not be determined
     */
    public static ContentFormat detectFileType(final MultipartFile file) throws UnsupportedContentFormatException {
        // Check if the file is empty or null
        if (file == null || file.isEmpty()) {
            throw new UnsupportedContentFormatException("The file is null or empty.");
        }

        // Get the content type of the file
        final String contentType = file.getContentType();

        // Content type may return null if it couldn't be determined
        if (contentType == null) {
            if (file.getOriginalFilename() != null) {
                return getContentFormat(file.getOriginalFilename());
            }
            throw new UnsupportedContentFormatException("Content type of the file could not be determined.");
        }

        // Check the content type to determine the file type
        return switch (contentType) {
            case "image/jpeg" -> ContentFormat.JPEG;
            case "image/png" -> ContentFormat.PNG;
            case "video/mp4" -> ContentFormat.MP4;
            default -> throw new UnsupportedContentFormatException("Unsupported file type.");
        };
    }
}
