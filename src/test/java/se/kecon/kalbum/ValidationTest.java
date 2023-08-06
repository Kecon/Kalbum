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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test the validation class.
 * <p>
 * Created by Kenny Colliander on 2023-08-04.
 * </p>
 */
class ValidationTest {

    @ParameterizedTest
    @ValueSource(strings = {"id1", "511b811f-d41b-4e67-ac4a-6ccb2a06b949"})
    void checkValidAlbumIdAsValid(String value) throws IllegalAlbumIdException {
        Validation.checkValidAlbumId(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"id1/", "id1\\", "id1:", "id1*", "id1?", "id1\"", "id1<", "id1>", "id1|", "id1.", "id1 ", "id1\t", "id1\n", "id1\r", "id1\u000B", "id1\u000C", "id1\u0085", "id1\u00A0", "id1\u2007", "id1\u202F", "id1\uFEFF", "..", "../test", "..\\test", "*", "/", "\\"})
    void checkValidAlbumIdAsInvalid(String value) {
        assertThrows(IllegalAlbumIdException.class, () -> Validation.checkValidAlbumId(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"id1.png", "id1.jpg", "id1.jpeg", "id1.mp4", "id1.PNG", "id1.JPG", "id1.JPEG", "id1.MP4"})
    void checkValidFilename(String value) throws IllegalFilenameException {
        Validation.checkValidFilename(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"id1/", "id1\\", "id1:", "id1*", "id1?", "id1\"", "id1<", "id1>", "id1|", "id1.", "id1 ", "id1\t", "id1\n", "id1\r", "id1\u000B", "id1\u000C", "id1\u0085", "id1\u00A0", "id1\u2007", "id1\u202F", "id1\uFEFF", "..", "../test", "..\\test", "*", "/", "\\", "index.html"})
    void checkValidFilenameIdAsInvalid(String value) {
        assertThrows(IllegalFilenameException.class, () -> Validation.checkValidFilename(value));
    }
}