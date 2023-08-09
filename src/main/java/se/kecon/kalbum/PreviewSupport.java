package se.kecon.kalbum;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
@Slf4j
public class PreviewSupport {

    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    private static final String PREVIEW_FILENAME = "preview.png";

    private static final int PREVIEW_WIDTH = 512;
    private static final int PREVIEW_HEIGHT = 512;

    private static final int MAX_PREVIEW_IMAGE_WIDTH = 256;
    private static final int MAX_PREVIEW_IMAGE_HEIGHT = 256;

    private static final int MAX_IMAGES = 10;

    @Setter(AccessLevel.PACKAGE)
    private Random random = new SecureRandom();

    /**
     * Areas of where to place images in the preview
     */
    @Getter
    protected enum Area {
        CENTER(0.45f, 0.45f, 0.55f, 0.55f),
        TOP_LEFT(0f, 0f, 0.25f, 0.25f),
        BOTTOM_LEFT(0f, 0.75f, 0.25f, 1f),
        TOP_RIGHT(0.75f, 0f, 1f, 0.25f),
        BOTTOM_RIGHT(0.75f, 0.75f, 1f, 1f),
        LEFT(0f, 0.45f, 0.25f, 0.55f),
        RIGHT(0.75f, 0.45f, 1f, 0.55f),
        TOP(0.45f, 0f, 0.55f, 0.25f),
        BOTTOM(0.45f, 0.75f, 0.55f, 1f);

        private final float x1;
        private final float y1;
        private final float x2;
        private final float y2;

        /**
         * Constructor
         *
         * @param x1 x1
         * @param y1 y1
         * @param x2 x2
         * @param y2 y2
         */
        Area(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    @Setter
    @Value("${kalbum.path}")
    private Path albumBasePath;

    /**
     * Create a preview image for the given album
     *
     * @param album album
     * @throws IllegalAlbumIdException  if the album id is invalid
     * @throws IllegalFilenameException if the filename is invalid
     * @throws IOException              if the preview image could not be created
     */
    public void createPreview(Album album) throws IllegalAlbumIdException, IllegalFilenameException, IOException {
        final Path previewPath = FileUtils.getContentPath(albumBasePath, album.getId(), PREVIEW_FILENAME);
        final BufferedImage bufferedImage = new BufferedImage(PREVIEW_WIDTH, PREVIEW_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        final List<BufferedImage> imageList = new ArrayList<>();

        // Load a subset of random images from the album
        loadImages(album, imageList);

        Graphics2D graphics = bufferedImage.createGraphics();
        try {
            // Fill with transparent color
            graphics.setColor(TRANSPARENT);
            graphics.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());

            for (int index = 0; index < imageList.size(); index++) {
                // Place images in random areas
                final BufferedImage image = imageList.get(index);

                // Last image should be placed in the center
                final int area = (index + 1) >= imageList.size() ? 0 : (index % Area.values().length);
                randomPlaceImage(image, graphics, Area.values()[area]);
            }
        } finally {
            graphics.dispose();
        }

        try (final OutputStream outputStream = Files.newOutputStream(previewPath)) {
            // Write preview image to file using NIO API.
            ImageIO.write(bufferedImage, "png", outputStream);
        }
    }

    /**
     * Load images from the album. Random images are selected. Maximum number of images is {@link #MAX_IMAGES}. Please note that only images are loaded, and not videos.
     *
     * @param album     album
     * @param imageList list of images
     */
    protected void loadImages(final Album album, final List<BufferedImage> imageList) {
        final List<ContentData> contents = new ArrayList<>(album.getContents());
        Collections.shuffle(contents);
        contents.stream().filter((ContentData contentData) -> contentData.getContentType().startsWith("image/") && imageList.size() < MAX_IMAGES).forEach((ContentData contentData) -> {
            try (final InputStream inputStream = Files.newInputStream(FileUtils.getContentPath(albumBasePath, album.getId(), contentData.getSrc()))) {
                imageList.add(ImageIO.read(inputStream));
            } catch (IOException | IllegalAlbumIdException | IllegalFilenameException e) {
                log.error("Could not load image", e);
            }
        });
    }

    /**
     * Place image in the given area. The image will be scaled to fit in the area. It will be rotated randomly.
     *
     * @param source      source image
     * @param destination destination graphics
     * @param area        area
     */
    protected void randomPlaceImage(final BufferedImage source, final Graphics2D destination, final Area area) {

        final float angle = ((random.nextFloat() * 40) + 340f) % 360f;
        final int newWidth;
        final int newHeight;

        if (source.getWidth() > source.getHeight()) {
            // Landscape
            final float ratio = (float) source.getWidth() / MAX_PREVIEW_IMAGE_WIDTH;
            newWidth = MAX_PREVIEW_IMAGE_WIDTH;
            newHeight = (int) (source.getHeight() / ratio);
        } else {
            // Portrait
            final float ratio = (float) source.getHeight() / MAX_PREVIEW_IMAGE_HEIGHT;
            newWidth = (int) (source.getWidth() / ratio);
            newHeight = MAX_PREVIEW_IMAGE_HEIGHT;
        }

        // Scale image to fit in preview
        final BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = scaledImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);

            graphics.drawImage(source, 0, 0, newWidth, newHeight, null);
        } finally {
            graphics.dispose();
        }

        // Rotate image randomly
        final BufferedImage rotatedImage = rotateImage(scaledImage, angle);

        // Place image in random area
        int x = Math.round(PREVIEW_WIDTH * area.getX1() + random.nextInt(Math.round((area.getX2() - area.getX1()) * PREVIEW_WIDTH))) - Math.round(0.5f * rotatedImage.getWidth());
        int y = Math.round(PREVIEW_HEIGHT * area.getY1() + random.nextInt(Math.round((area.getY2() - area.getY1()) * PREVIEW_HEIGHT))) - Math.round(0.5f * rotatedImage.getHeight());

        // Make sure image is not placed outside preview area
        x = (x + rotatedImage.getWidth() > PREVIEW_WIDTH ? PREVIEW_WIDTH - rotatedImage.getWidth() : x);
        y = (y + rotatedImage.getHeight() > PREVIEW_HEIGHT ? PREVIEW_HEIGHT - rotatedImage.getHeight() : y);
        x = Math.max(x, 0);
        y = Math.max(y, 0);

        // Draw image
        destination.drawImage(rotatedImage, null, x, y);
    }

    /**
     * Rotate image
     *
     * @param bufferedImage image
     * @param angle         angle
     * @return rotated image
     */
    protected BufferedImage rotateImage(final BufferedImage bufferedImage, final float angle) {
        final double sin = Math.abs(Math.sin(Math.toRadians(angle)));
        final double cos = Math.abs(Math.cos(Math.toRadians(angle)));
        final int width = bufferedImage.getWidth();
        final int height = bufferedImage.getHeight();
        final int newWidth = (int) Math.floor(width * cos + height * sin);
        final int newHeight = (int) Math.floor(height * cos + width * sin);
        final BufferedImage rotated = new BufferedImage(newWidth, newHeight, bufferedImage.getType());
        final Graphics2D graphics2D = rotated.createGraphics();

        try {
            graphics2D.translate((newWidth - width) / 2, (newHeight - height) / 2);
            graphics2D.rotate(Math.toRadians(angle), width / 2d, height / 2d);
            graphics2D.drawRenderedImage(bufferedImage, null);
        } finally {
            graphics2D.dispose();
        }

        return rotated;
    }
}
