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
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the content format
 * <p>
 * Created by Kenny Colliander on 2023-08-04.
 * </p>
 */
class ContentFormatTest {

    @Test
    void getContentFormatAsJpeg() throws UnsupportedContentFormatException {
        assertEquals(ContentFormat.JPEG, ContentFormat.getContentFormat("file1.jpeg"));
    }

    @Test
    void getContentFormatAsJpg() throws UnsupportedContentFormatException {
        assertEquals(ContentFormat.JPEG, ContentFormat.getContentFormat("file1.jpg"));
    }

    @Test
    void getContentFormatAsPng() throws UnsupportedContentFormatException {
        assertEquals(ContentFormat.PNG, ContentFormat.getContentFormat("file1.png"));
    }

    @Test
    void getContentFormatAsMp4() throws UnsupportedContentFormatException {
        assertEquals(ContentFormat.MP4, ContentFormat.getContentFormat("file1.mp4"));
    }

    @Test
    void getContentFormatAsInvalid() {
        assertThrows(UnsupportedContentFormatException.class, () -> ContentFormat.getContentFormat("file1.invalid"));
    }

    @Test
    void getContentFormatAsInvalidSuffix() {
        assertThrows(UnsupportedContentFormatException.class, () -> ContentFormat.getContentFormat("file1.invalid"));
    }

    @Test
    void getContentFormatAsNoSuffix() {
        assertThrows(UnsupportedContentFormatException.class, () -> ContentFormat.getContentFormat("file1"));
    }

    @Test
    void detectFileTypeJpegAsContentType() throws UnsupportedContentFormatException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/jpeg");
        assertEquals(ContentFormat.JPEG, ContentFormat.detectFileType(file));
    }

    @Test
    void detectFileTypePngAsContentType() throws UnsupportedContentFormatException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/png");
        assertEquals(ContentFormat.PNG, ContentFormat.detectFileType(file));
    }

    @Test
    void detectFileTypeMp4AsContentType() throws UnsupportedContentFormatException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("video/mp4");
        assertEquals(ContentFormat.MP4, ContentFormat.detectFileType(file));
    }

    @Test
    void detectFileTypeAsInvalidImage() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/invalid");
        assertThrows(UnsupportedContentFormatException.class, () -> ContentFormat.detectFileType(file));
    }

    @Test
    void detectFileTypeJpegAsFilename() throws UnsupportedContentFormatException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("id1.jpeg");
        assertEquals(ContentFormat.JPEG, ContentFormat.detectFileType(file));
    }

    @Test
    void detectFileTypeJpgAsFilename() throws UnsupportedContentFormatException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("id1.jpg");
        assertEquals(ContentFormat.JPEG, ContentFormat.detectFileType(file));
    }

    @Test
    void detectFileTypePngAsFilename() throws UnsupportedContentFormatException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("id1.png");
        assertEquals(ContentFormat.PNG, ContentFormat.detectFileType(file));
    }

    @Test
    void detectFileTypeMp4AsFilename() throws UnsupportedContentFormatException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("id1.mp4");
        assertEquals(ContentFormat.MP4, ContentFormat.detectFileType(file));
    }

    @Test
    void detectFileTypeAsInvalid()  {
        MultipartFile file = mock(MultipartFile.class);
        assertThrows(UnsupportedContentFormatException.class, () -> ContentFormat.detectFileType(file));
    }

    @Test
    void getContentTypeJpeg() {
        assertEquals("image/jpeg", ContentFormat.JPEG.getContentType());
    }

    @Test
    void getContentTypePng() {
        assertEquals("image/png", ContentFormat.PNG.getContentType());
    }

    @Test
    void getContentTypeMp4() {
        assertEquals("video/mp4", ContentFormat.MP4.getContentType());
    }

    @Test
    void getSuffixesJpeg() {
        assertTrue(ContentFormat.JPEG.getSuffixes().contains("jpeg"));
    }

    @Test
    void getSuffixesJpg() {
        assertTrue(ContentFormat.JPEG.getSuffixes().contains("jpg"));
    }

    @Test
    void getSuffixesPng() {
        assertTrue(ContentFormat.PNG.getSuffixes().contains("png"));
    }

    @Test
    void getSuffixesMp4() {
        assertTrue(ContentFormat.MP4.getSuffixes().contains("mp4"));
    }

    @Test
    void isImageJpeg() {
        assertTrue(ContentFormat.JPEG.isImage());
    }

    @Test
    void isImagePng() {
        assertTrue(ContentFormat.PNG.isImage());
    }

    @Test
    void isImageMp4() {
        assertFalse(ContentFormat.MP4.isImage());
    }
}