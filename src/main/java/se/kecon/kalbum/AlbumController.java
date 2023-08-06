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

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static se.kecon.kalbum.FileUtils.getFilename;
import static se.kecon.kalbum.FileUtils.removeSuffix;
import static se.kecon.kalbum.Validation.checkValidAlbumId;
import static se.kecon.kalbum.Validation.checkValidFilename;

/**
 * Controller for the album API.
 *
 * @author Kenny Colliander
 * @since 2023-07-31
 */
@RestController
@Slf4j
public class AlbumController {

    private static final String CONTENT_PATH = "contents";

    private static final String THUMBNAIL_PATH = ".thumbnails";

    @Setter
    @Value("${kalbum.path}")
    private Path albumBasePath;

    @Autowired
    private AlbumDao albumDao;

    /**
     * Get the path to the thumbnail for the given content
     *
     * @param id            album id
     * @param filename      content filename
     * @param contentFormat content format
     * @return path
     * @throws IllegalAlbumIdException  if the id is invalid
     * @throws IllegalFilenameException if the filename is invalid
     */
    protected Path getThumbnailPath(final String id, final String filename, final ContentFormat contentFormat) throws IllegalAlbumIdException, IllegalFilenameException {
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
     * @param id       album id
     * @param filename content filename
     * @return path
     * @throws IllegalAlbumIdException  if the id is invalid
     * @throws IllegalFilenameException if the filename is invalid
     */
    protected Path getContentPath(final String id, final String filename) throws IllegalAlbumIdException, IllegalFilenameException {
        checkValidAlbumId(id);
        checkValidFilename(filename);

        return albumBasePath.resolve(id).resolve(CONTENT_PATH).resolve(filename);
    }

    /**
     * List all albums
     *
     * @return list of albums
     */
    @GetMapping(path = "/albums/")
    public ResponseEntity<List<AlbumContent>> listAlbums() {
        log.info("List albums");

        final List<AlbumContent> albumContents = new ArrayList<>();

        this.albumDao.getAll().stream().map(album -> {
            final AlbumContent albumContent = new AlbumContent();
            albumContent.setId(album.getId());
            albumContent.setName(album.getName());
            return albumContent;
        }).forEach(albumContents::add);

        return new ResponseEntity<>(albumContents, HttpStatus.OK);
    }


    /**
     * Create a new album
     *
     * @param albumContent the name of the album, etc
     * @return the id of the created album
     */
    @PostMapping(path = "/albums/")
    public ResponseEntity<Void> createAlbum(@RequestBody AlbumContent albumContent) {
        log.info("Create album {}", albumContent);

        final Album album;
        try {
            album = this.albumDao.create(albumContent.getName());
        } catch (IOException e) {
            log.error("Failed to create album", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String resourceUrl = ServletUriComponentsBuilder.fromCurrentRequest().path("{id}").buildAndExpand(album.getId()).toUriString();

        // Create the HttpHeaders object and set the Location header
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(java.net.URI.create(resourceUrl));

        log.info("Created album {}", album);
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }


    /**
     * Get album
     *
     * @param id the id of the album
     * @return the album
     */
    @GetMapping(path = "/albums/{id}/contents/")
    public ResponseEntity<List<ContentData>> listAlbumContents(@PathVariable(name = "id") String id) {
        log.info("List resource directory for {}", id);

        try {
            final Optional<Album> album = this.albumDao.get(id);

            if (album.isPresent()) {
                return new ResponseEntity<>(album.get().getContents(), HttpStatus.OK);
            }

            log.warn("Album {} does not exist", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalAlbumIdException e) {
            log.error("Invalid album id", e);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    /**
     * Upload content to an album
     *
     * @param id   the id of the album
     * @param file the content to upload
     * @return the status
     */
    @PostMapping("/albums/{id}/contents/")
    public ResponseEntity<Void> uploadContent(@PathVariable(name = "id") String id, @RequestParam("file") MultipartFile file) {
        log.info("Upload content {} for album {}", file.getOriginalFilename(), id);

        try {
            // Check if album exists
            final Optional<Album> album = this.albumDao.get(id);

            if (album.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            final List<ContentData> contents = new ArrayList<>(album.get().getContents());
            Thumbnail.ImageInfo imageInfo;

            final String filename = getFilename(file);

            for (ContentData contentData : contents) {
                if (contentData.getSrc().equals(filename)) {
                    return new ResponseEntity<>(HttpStatus.CONFLICT);
                }
            }

            final ContentFormat contentFormat = ContentFormat.detectFileType(file);
            final Path contentPath = getContentPath(id, filename);
            final Path thumbnailPath = getThumbnailPath(id, filename, contentFormat);

            if (!Files.exists(thumbnailPath)) {
                Files.createDirectories(thumbnailPath.getParent());
            }

            if (Files.exists(contentPath)) {
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }

            final byte[] bs = file.getBytes();

            try (InputStream inputStream = new ByteArrayInputStream(bs)) {
                Files.copy(inputStream, contentPath);
            }

            if (contentFormat.isImage()) {
                try (InputStream inputStream = new ByteArrayInputStream(bs); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    imageInfo = Thumbnail.createThumbnail(inputStream, outputStream, 512, ContentFormat.getContentFormat(filename).name());

                    try (InputStream thumbnailInputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                        Files.copy(thumbnailInputStream, thumbnailPath);
                    }
                }
            } else {
                final BufferedImage bufferedImage = Thumbnail.generateVideoThumbnail(contentPath);
                try (InputStream inputStream = ClassLoader.getSystemResourceAsStream("static/play.png")) {
                    Objects.requireNonNull(inputStream, "Play icon not found");
                    final BufferedImage play = ImageIO.read(inputStream);

                    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                        imageInfo = Thumbnail.createThumbnail(bufferedImage, outputStream, 512, "png");

                        BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(outputStream.toByteArray()));
                        Graphics2D graphics = (Graphics2D) thumbnail.getGraphics();

                        try {
                            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                            final int width = thumbnail.getWidth();
                            final int height = thumbnail.getHeight();

                            graphics.drawImage(play, width / 2 - play.getWidth() / 2, height / 2 - play.getHeight() / 2, null);

                        } finally {
                            graphics.dispose();
                        }

                        try (ByteArrayOutputStream thumbnailOutputStream = new ByteArrayOutputStream()) {
                            ImageIO.write(thumbnail, "png", thumbnailOutputStream);

                            try (InputStream thumbnailInputStream = new ByteArrayInputStream(thumbnailOutputStream.toByteArray())) {
                                Files.copy(thumbnailInputStream, thumbnailPath);
                            }
                        }
                    }
                }
            }

            ContentData contentData = new ContentData();
            contentData.setSrc(filename);
            contentData.setContentType(contentFormat.getContentType());
            contentData.setWidth(imageInfo.getWidth());
            contentData.setHeight(imageInfo.getHeight());

            try (InputStream inputStream = new ByteArrayInputStream(bs)) {
                Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
                metadata.getDirectories().forEach(directory -> {
                    for (Tag tag : directory.getTags()) {
                        if (tag.getTagType() == 0x9003) {
                            DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                                    .appendPattern("yyyy:MM:dd HH:mm:ss")
                                    .toFormatter()
                                    .withZone(ZoneId.of("Europe/Stockholm")); // TODO: get from application properties
                            contentData.setTimestamp(dateTimeFormatter.parse(tag.getDescription(), Instant::from));
                        }
                    }
                });
            } catch (ImageProcessingException e) {
                log.warn("Failed to read metadata", e);
            }

            contents.add(contentData);
            album.get().setContents(contents);

            this.albumDao.update(album.get());

            String resourceUrl = ServletUriComponentsBuilder.fromCurrentRequest().path("{filename}").buildAndExpand(filename).toUriString();

            // Create the HttpHeaders object and set the Location header
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(java.net.URI.create(resourceUrl));

            return new ResponseEntity<>(headers, HttpStatus.CREATED);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalAlbumIdException | IllegalFilenameException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (UnsupportedContentFormatException e) {
            return new ResponseEntity<>(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
    }

    /**
     * Delete content from an album
     *
     * @param id       the id of the album
     * @param filename the name of the content to delete
     * @return the status
     */
    @DeleteMapping("/albums/{id}/contents/{filename}")
    public ResponseEntity<Void> deleteContent(@PathVariable(name = "id") String id, @PathVariable(name = "filename") String filename) {
        log.info("Delete content {} for album {}", filename, id);

        try {
            // Input is validated by albumDao.get and getContentPath
            final Optional<Album> album = this.albumDao.get(id);

            if (album.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            final Path contentPath = getContentPath(id, filename);
            final Path thumbnailPath = getThumbnailPath(id, filename, ContentFormat.getContentFormat(filename));

            if (Files.exists(contentPath)) {
                Files.delete(contentPath);
            }

            if (Files.exists(thumbnailPath)) {
                Files.delete(thumbnailPath);
            }

            final List<ContentData> contents = new ArrayList<>(album.get().getContents());
            if (contents.removeIf(contentData -> contentData.getSrc().equals(filename))) {
                album.get().setContents(contents);

                this.albumDao.update(album.get());

                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalAlbumIdException | IllegalFilenameException | UnsupportedContentFormatException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Patch alt and text of a photo or video in the album
     *
     * @param id          the id of the album
     * @param filename    the name of the content to patch
     * @param contentData the content data to patch, and in this case only alt and text are allowed
     * @return the status
     */
    @PatchMapping("/albums/{id}/contents/{filename}")
    public ResponseEntity<Void> patchContent(@PathVariable(name = "id") String id, @PathVariable(name = "filename") String filename, @RequestBody ContentData contentData) {
        log.info("Patch content {} for album {}", filename, id);

        try {
            // No need to check if filename is valid, since it needs to match existing content
            final Optional<Album> album = this.albumDao.get(id);

            if (album.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            final Optional<ContentData> storedContentData = album.get().getContents().stream().filter(data -> data.getSrc().equals(filename)).findFirst();

            if (storedContentData.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // Only alt and text can be patched
            storedContentData.get().setAlt(contentData.getAlt());
            storedContentData.get().setText(contentData.getText());

            this.albumDao.update(album.get());

            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalAlbumIdException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Get content from an album
     *
     * @param id       the id of the album
     * @param filename the name of the content to get
     * @return the content
     * @throws IOException if the content could not be read
     */
    @GetMapping(path = "/albums/{id}/contents/{filename}")
    public ResponseEntity<InputStreamResource> getContent(@PathVariable(name = "id") String id, @PathVariable(name = "filename") String filename) throws IOException {

        try {
            // Input is validated in getContentPath
            final Path path = getContentPath(id, filename);

            InputStream inputStream = Files.newInputStream(path);
            InputStreamResource inputStreamResource = new InputStreamResource(inputStream);
            String contentType = Files.probeContentType(path);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));

            return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
        } catch (IllegalAlbumIdException | IllegalFilenameException | NoSuchFileException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Get thumbnail from an album
     *
     * @param id       the id of the album
     * @param filename the name of the thumbnail to get
     * @return the thumbnail
     * @throws IOException if the thumbnail could not be read
     */
    @GetMapping(path = "/albums/{id}/contents/thumbnails/{filename}")
    public ResponseEntity<InputStreamResource> getThumbnail(@PathVariable(name = "id") String id, @PathVariable(name = "filename") String filename) throws IOException {
        try {
            // Input is validated by getThumbnailPath
            final Path path = getThumbnailPath(id, filename, ContentFormat.getContentFormat(filename));

            InputStream inputStream = Files.newInputStream(path);
            InputStreamResource inputStreamResource = new InputStreamResource(inputStream);
            String contentType = Files.probeContentType(path);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));

            return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
        } catch (IllegalAlbumIdException | IllegalFilenameException | UnsupportedContentFormatException |
                 NoSuchFileException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
