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

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test the thumbnail class.
 * <p>
 * Created by Kenny Colliander on 2023-08-04.
 * </p>
 */
class ThumbnailTest {

    @Test
    void testCreateThumbnail() throws IOException {

        try (InputStream inputStream = ThumbnailTest.class.getResourceAsStream("/IMG_8653.jpg");
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Thumbnail.createThumbnail(inputStream, outputStream, 512, "jpg");

            try (InputStream testInputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                BufferedImage img = ImageIO.read(testInputStream);

                assertEquals(512, img.getHeight());
                assertEquals(910, img.getWidth());
            }
        }
    }
}
