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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static se.kecon.kalbum.FileUtils.removeSuffix;
import static se.kecon.kalbum.Validation.checkValidAlbumId;

/**
 * Data access objects for albums.
 *
 * @author Kenny Colliander
 * @since 2023-08-04
 */
@Component
@Slf4j
public class AlbumDaoComponent implements AlbumDao, InitializingBean {

    private final ConcurrentMap<String, Album> albums = new ConcurrentHashMap<>();

    private final Lock lock = new ReentrantLock();

    @Setter
    private Supplier<String> idGenerator = () -> UUID.randomUUID().toString();

    @Setter
    @Value("${kalbum.path}")
    private Path albumBasePath;

    /**
     * Create a new album with the given name and save it to persistent storage, then return the album
     *
     * @param name album name
     * @return album
     * @throws IOException if the album could not be saved to persistent storage
     */
    @Override
    public Album create(String name) throws IOException {

        int tries = 0;
        while (true) {
            String id;
            do {
                id = idGenerator.get();

                if (tries++ > 100) {
                    throw new IllegalStateException("Could not generate unique album id");
                }
            } while (this.albums.containsKey(id));

            final Album album = new Album();
            album.setId(id);
            album.setName(name);
            album.setContents(new ArrayList<>());

            try {
                saveToPersistentStorage(album);
                return album;
            } catch (IllegalAlbumIdException e) {
                log.error("Invalid album id was generated in create: " + id, e);
            }
        }
    }

    /**
     * Get the album with the given id
     *
     * @param id album id
     * @return album
     * @throws IllegalAlbumIdException if the id is invalid
     */
    @Override
    public Optional<Album> get(final String id) throws IllegalAlbumIdException {
        checkValidAlbumId(id);
        final Album album = this.albums.get(id);

        return album != null ? Optional.of(new Album(album)) : Optional.empty();
    }

    /**
     * Get all albums
     *
     * @return albums
     */
    @Override
    public List<Album> getAll() {
        return this.albums.values().stream().map(Album::new).toList();
    }

    /**
     * Update the album
     *
     * @param album the album to update
     * @throws IllegalAlbumIdException if the id is invalid
     * @throws IOException             if the album could not be stored
     */
    @Override
    public void update(Album album) throws IllegalAlbumIdException, IOException {
        checkValidAlbumId(album.getId());

        Album cachedAlbum = this.albums.get(album.getId());
        if (cachedAlbum == null) {
            throw new IllegalAlbumIdException("Album does not exist: " + album.getId());
        }
        saveToPersistentStorage(album);
    }

    /**
     * Delete the album with the given id. Please note that this is only the album configuration and all images will be left untouched.
     *
     * @param id album id
     * @throws IllegalAlbumIdException if the id is invalid
     * @throws IOException             if the album could not be deleted
     */
    @Override
    public void delete(String id) throws IllegalAlbumIdException, IOException {
        checkValidAlbumId(id);
        this.albums.remove(id);
        Files.delete(getAlbumPath(id));
    }

    /**
     * Get the object mapper with support for Java 8 time.
     *
     * @return object mapper
     */
    protected ObjectMapper getObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    /**
     * Get the path to the album with the given id.
     *
     * @param id album id
     * @return path
     * @throws IllegalAlbumIdException if the id is invalid
     */
    protected Path getAlbumPath(String id) throws IllegalAlbumIdException {
        checkValidAlbumId(id);
        return albumBasePath.resolve(id + ".json");
    }

    /**
     * Save the album to persistent storage.
     *
     * @param album the album to store
     * @throws IOException             if the album could not be stored
     * @throws IllegalAlbumIdException if the album id is invalid
     */
    protected void saveToPersistentStorage(final Album album) throws IOException, IllegalAlbumIdException {
        final Path path = getAlbumPath(album.getId());

        this.lock.lock();
        try {
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            final ObjectMapper objectMapper = getObjectMapper();
            Files.write(path, objectMapper.writeValueAsBytes(album));

            this.albums.put(album.getId(), new Album(album));
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Get the album with the given id from persistent storage. This method is not thread safe.
     *
     * @param id album id
     * @return album
     * @throws IllegalAlbumIdException if the id is invalid
     * @throws IOException             if the album could not be read
     */
    protected Optional<Album> getFromPersistentStorage(String id) throws IllegalAlbumIdException, IOException {
        final Path path = getAlbumPath(id);

        if (Files.exists(path)) {
            final ObjectMapper objectMapper = getObjectMapper();
            final byte[] bs = Files.readAllBytes(path);
            final Album album = objectMapper.readValue(bs, Album.class);

            this.albums.put(id, album);

            return Optional.of(new Album(album));
        }
        log.warn("Album {} does not exist", id);
        return Optional.empty();
    }

    /**
     * Read all albums from persistent storage. This will replace all albums in memory. This method is not thread safe.
     *
     * @throws IOException if the albums could not be read
     */
    protected void readAllFromPersistentStorage() throws IOException {
        final Set<String> added = new HashSet<>();

        try (Stream<Path> parent = Files.list(albumBasePath)) {
            parent.filter(Files::isRegularFile).filter(file -> file.getFileName().toString().endsWith(".json"))
                    .forEach(path -> {
                        final String id = removeSuffix(path.getFileName().toString());
                        try {
                            getFromPersistentStorage(id).ifPresent(album -> albums.put(id, album));
                            added.add(id);
                        } catch (IllegalAlbumIdException e) {
                            log.error("Invalid album id: {}", id, e);
                        } catch (IOException e) {
                            log.error("Failed to read album: {}", id, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to list albums", e);
            throw e;
        }

        albums.keySet().removeIf(id -> !added.contains(id));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        readAllFromPersistentStorage();
    }
}
