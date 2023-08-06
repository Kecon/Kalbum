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

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.api.PictureWithMetadata;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Utility class for thumbnails.
 *
 * @author Kenny Colliander
 * @since 2023-07-31
 */
public class Thumbnail {

    private Thumbnail() {
    }

    @Data
    @AllArgsConstructor
    public static class ImageInfo {
        private int width;
        private int height;
    }

    public static ImageInfo createThumbnail(final InputStream inputStream, final OutputStream outputStream,
                                            final int maxHeight, final String formatName) throws IOException {
        final BufferedImage img = ImageIO.read(inputStream);
        return createThumbnail(img, outputStream, maxHeight, formatName);
    }

    public static ImageInfo createThumbnail(final BufferedImage img, final OutputStream outputStream,
                                            final int maxHeight, final String formatName) throws IOException {
        final float ratio = (float) img.getHeight() / maxHeight;
        final int width = Math.round(img.getWidth() / ratio);
        final BufferedImage thumb = new BufferedImage(width, maxHeight, BufferedImage.TYPE_INT_RGB);

        final Graphics2D g2d = (Graphics2D) thumb.getGraphics();
        g2d.drawImage(img, 0, 0, thumb.getWidth() - 1, thumb.getHeight() - 1, 0, 0, img.getWidth() - 1,
                img.getHeight() - 1, null);
        g2d.dispose();
        ImageIO.write(thumb, formatName, outputStream);

        return new ImageInfo(img.getWidth(), img.getHeight());
    }

    public static BufferedImage generateVideoThumbnail(Path videoFilePath) throws IOException {
        try {
            FileChannelWrapper fileChannelWrapper = NIOUtils.readableChannel(videoFilePath.toFile());
            FrameGrab grab = FrameGrab.createFrameGrab(fileChannelWrapper);
            grab.seekToSecondPrecise(0);

            PictureWithMetadata nativePicture = grab.getNativeFrameWithMetadata();
            Picture picture = nativePicture.getPicture();

            fileChannelWrapper.close();
            return rotateImage(nativePicture.getOrientation(), AWTUtil.toBufferedImage(picture));
        } catch (JCodecException e) {
            throw new IOException("Failed to generate video thumbnail", e);
        }
    }

    private static BufferedImage rotateImage(DemuxerTrackMeta.Orientation imageRotation, BufferedImage bufferedImage) {
        AffineTransform affineTransform = new AffineTransform();

        final boolean isOrientation90or270 = DemuxerTrackMeta.Orientation.D_90.equals(imageRotation) || DemuxerTrackMeta.Orientation.D_270.equals(imageRotation);

        if (isOrientation90or270) {
            affineTransform.translate((double) bufferedImage.getHeight() / 2, (double) bufferedImage.getWidth() / 2);
            affineTransform.rotate(DemuxerTrackMeta.Orientation.D_90.equals(imageRotation) ? Math.toRadians(90) : Math.toRadians(270));
            affineTransform.translate((double) -bufferedImage.getWidth() / 2, (double) -bufferedImage.getHeight() / 2);

        } else if (DemuxerTrackMeta.Orientation.D_180.equals(imageRotation)) {
            affineTransform.translate((double) bufferedImage.getWidth() / 2, (double) bufferedImage.getHeight() / 2);
            affineTransform.rotate(Math.toRadians(180));
            affineTransform.translate((double) -bufferedImage.getWidth() / 2, (double) -bufferedImage.getHeight() / 2);
        }

        AffineTransformOp affineTransformOp = new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BILINEAR);

        BufferedImage result;

        if (isOrientation90or270) {
            result = new BufferedImage(bufferedImage.getHeight(), bufferedImage.getWidth(), bufferedImage.getType());

        } else {
            result = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), bufferedImage.getType());
        }

        affineTransformOp.filter(bufferedImage, result);

        return result;
    }
}
